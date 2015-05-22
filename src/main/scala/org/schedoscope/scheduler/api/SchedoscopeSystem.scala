package org.schedoscope.scheduler.api

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

import com.ottogroup.bi.soda.bottler.ActionStatusListResponse
import com.ottogroup.bi.soda.bottler.Deploy
import com.ottogroup.bi.soda.bottler.Failed
import com.ottogroup.bi.soda.bottler.GetActions
import com.ottogroup.bi.soda.bottler.GetQueues
import com.ottogroup.bi.soda.bottler.GetViews
import com.ottogroup.bi.soda.bottler.Invalidate
import com.ottogroup.bi.soda.bottler.MaterializeView
import com.ottogroup.bi.soda.bottler.MaterializeViewMode
import com.ottogroup.bi.soda.bottler.NoDataAvailable
import com.ottogroup.bi.soda.bottler.QueueStatusListResponse
import com.ottogroup.bi.soda.bottler.SodaRootActor
import com.ottogroup.bi.soda.bottler.SodaRootActor.actionsManagerActor
import com.ottogroup.bi.soda.bottler.SodaRootActor.settings
import com.ottogroup.bi.soda.bottler.SodaRootActor.viewManagerActor
import com.ottogroup.bi.soda.bottler.ViewMaterialized
import com.ottogroup.bi.soda.bottler.ViewStatusListResponse
import com.ottogroup.bi.soda.bottler.ViewStatusResponse
import com.ottogroup.bi.soda.bottler.queryActor
import com.ottogroup.bi.soda.dsl.Named
import com.ottogroup.bi.soda.dsl.View

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.actorRef2Scala
import akka.event.Logging
import akka.pattern.Patterns
import org.schedoscope.scheduler.ViewStatusListResponse
import org.schedoscope.scheduler.ActionStatusListResponse
import org.schedoscope.dsl.views.ViewUrlParser.ParsedViewAugmentor
import org.schedoscope.dsl.Transformation
import org.schedoscope.scheduler.GetActions
import org.schedoscope.scheduler.GetViews
import org.schedoscope.scheduler.QueueStatusListResponse
import org.schedoscope.scheduler.GetQueues
import org.schedoscope.scheduler.MaterializeViewMode
import org.schedoscope.scheduler.api.SchedoscopeJsonProtocol.formatDate
import akka.util.Timeout

class SodaSystem extends SodaInterface {
  val log = Logging(settings.system, classOf[SodaRootActor])

  actionsManagerActor ! Deploy()

  val runningCommands = collection.mutable.HashMap[String, SchedoscopeCommand]()
  val doneCommands = collection.mutable.HashMap[String, SchedoscopeCommandStatus]()

  private def viewsFromUrl(viewUrlPath: String) = {
    View.viewsFromUrl(settings.env, viewUrlPath, settings.viewAugmentor)
  }

  private def getViews(viewUrlPath: Option[String], status: Option[String], filter: Option[String], dependencies: Boolean = false) = {
    val resolvedViews = if (viewUrlPath.isDefined && !viewUrlPath.get.isEmpty()) Some(viewsFromUrl(viewUrlPath.get)) else None
    queryActor[ViewStatusListResponse](viewManagerActor, GetViews(resolvedViews, status, filter, dependencies), settings.viewManagerResponseTimeout).viewStatusList
  }

  private def commandId(command: Any, args: Seq[Option[String]], start: Option[LocalDateTime] = None) = {
    val format = DateTimeFormat.forPattern("YYYYMMddHHmmss");
    val c = command match {
      case s: String => s
      case c: Any => Named.formatName(c.getClass.getSimpleName)
    }
    val a = if (args.size == 0) "_" else args.filter(_.isDefined).map(_.getOrElse("_")).mkString(":")
    if (start.isDefined) {
      val s = format.print(start.get)
      s"${c}::${a}::${s}"
    } else {
      s"${c}::${a}"
    }
  }

  private def submitCommandInternal(actors: List[ActorRef], command: Any, args: Option[String]*) = {
    val currentCommandId = runningCommandId(command.toString, args: _*)
    if (currentCommandId.isDefined) {
      commandStatus(currentCommandId.get)
    } else {
      val jobFutures = actors.map { actor => Patterns.ask(actor, command, Timeout(settings.completitionTimeout)) }
      val start = new LocalDateTime()
      val id = commandId(command, args, Some(start))
      runningCommands.put(id, SchedoscopeCommand(id, formatDate(start), jobFutures))
      SchedoscopeCommandStatus(id, formatDate(start), None, Map("submitted" -> jobFutures.size))
    }
  }

  private def finalizeInternal(commandId: String, start: String, status: Map[String, Int]) = {
    runningCommands.remove(commandId)
    val scs = SchedoscopeCommandStatus(commandId, start, Some(formatDate(new LocalDateTime())), status)
    doneCommands.put(commandId, scs)
    scs
  }

  private def runningCommandId(command: String, args: Option[String]*): Option[String] = {
    val cidPrefix = commandId(command, args)
    runningCommands.keys
      .foreach(k =>
        if (k.startsWith(cidPrefix))
          return Some(k))
    None
  }

  def materialize(viewUrlPath: Option[String], status: Option[String], filter: Option[String], mode: Option[String]) = {
    val viewActors = getViews(viewUrlPath, status, filter).map(v => v.actor)
    submitCommandInternal(viewActors, MaterializeView(mode.getOrElse(MaterializeViewMode.default)), viewUrlPath, status, filter)
  }

  def invalidate(viewUrlPath: Option[String], status: Option[String], filter: Option[String], dependencies: Option[Boolean]) = {
    val viewActors = getViews(viewUrlPath, status, filter, dependencies.getOrElse(false)).map(v => v.actor)
    submitCommandInternal(viewActors, Invalidate(), viewUrlPath, status, filter)
  }

  def newdata(viewUrlPath: Option[String], status: Option[String], filter: Option[String]) = {
    val viewActors = getViews(viewUrlPath, status, filter).map(v => v.actor)
    submitCommandInternal(viewActors, "newdata", viewUrlPath, status, filter)
  }

  def views(viewUrlPath: Option[String], status: Option[String], filter: Option[String], dependencies: Option[Boolean], overview: Option[Boolean]) = {
    val views = getViews(viewUrlPath, status, filter, dependencies.getOrElse(false))
    val statusMap = views.map(v => (v.view.urlPath, v.status)).toMap // needed for status of dependencies in output
    val viewStatusList = views
      .map(v => ViewStatus(v.view.urlPath, v.status, None, if (!dependencies.getOrElse(false)) None else Some(v.view.dependencies.map(d => ViewStatus(d.urlPath, statusMap.getOrElse(d.urlPath, ""), None, None)).toList)))
    val ov = viewStatusList.groupBy(_.status).mapValues(_.size)
    ViewStatusList(ov, if (overview.getOrElse(false)) List() else viewStatusList)
  }

  def actions(status: Option[String], filter: Option[String]) = {
    val result = queryActor[ActionStatusListResponse](actionsManagerActor, GetActions(), settings.statusListAggregationTimeout)
    val actions = result.actionStatusList
      .map(a => SchedoscopeJsonProtocol.parseActionStatus(a))
      .filter(a => !status.isDefined || status.get.equals(a.status))
      .filter(a => !filter.isDefined || a.actor.matches(filter.get)) // FIXME: is the actor name a good filter criterion?
    val overview = actions
      .groupBy(_.status)
      .map(el => (el._1, el._2.size))
    ActionStatusList(overview, actions)
  }

  def queues(typ: Option[String], filter: Option[String]): QueueStatusList = {
    val result = queryActor[QueueStatusListResponse](actionsManagerActor, GetQueues(), settings.statusListAggregationTimeout)
    val queues = result.actionQueues
      .filterKeys(t => !typ.isDefined || t.startsWith(typ.get))
      .map { case (t, queue) => (t, SchedoscopeJsonProtocol.parseQueueElements(queue)) }
      .map { case (t, queue) => (t, queue.filter(el => !filter.isDefined || el.targetView.matches(filter.get))) }
    val overview = queues
      .map(el => (el._1, el._2.size))
    QueueStatusList(overview, queues)
  }

  def commandStatus(commandId: String) = {
    val cmd = runningCommands.get(commandId)
    if (!cmd.isDefined) {
      SchedoscopeCommandStatus(commandId, "", None, Map("non-existent" -> 1))
    } else {
      val statusCounts = cmd.get.parts
        .map(f => {
          if (f.isCompleted)
            Await.result(f, 0 seconds) match {
              // FIXME: Here we have to map all possible return messages to status codes
              case ViewMaterialized(view, incomplete, changed, errors) => "materialized"
              case NoDataAvailable(view) => "no-data"
              case Failed(view) => "failed"
              case ViewStatusResponse(status, view, actor) => status
              case _ => "other"
            }
          else
            "running"
        })
        .groupBy(_.toString)
        .mapValues(_.size)

      if (statusCounts.get("running").getOrElse(0) == 0) {
        finalizeInternal(commandId, cmd.get.start, Map("submitted" -> cmd.get.parts.size) ++ statusCounts)
      } else {
        SchedoscopeCommandStatus(commandId, cmd.get.start, None, Map("submitted" -> cmd.get.parts.size) ++ statusCounts)
      }
    }
  }

  def commands(status: Option[String], filter: Option[String]) = {
    val running = runningCommands.keys.map(id => {
      commandStatus(id)
    }).toList
    (running ++ doneCommands.values)
      .groupBy(_.id)
      .map(p => p._2.head)
      .toSeq
      .sortBy(_.start.toString)
      .filter(c => if (!status.isDefined || status.get.isEmpty) true else (c.status.get(status.get).isDefined && c.status(status.get) > 0))
      .filter(c => !filter.isDefined || c.id.matches(filter.get))
      .toList
  }

  def shutdown(): Boolean = {
    settings.system.shutdown()
    settings.system.awaitTermination(5 seconds)
    settings.system.actorSelection("/user/*").tell(PoisonPill, Actor.noSender)
    settings.system.awaitTermination(5 seconds)
    settings.system.isTerminated
  }

}
