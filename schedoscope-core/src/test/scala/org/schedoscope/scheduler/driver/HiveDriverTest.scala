/**
 * Copyright 2015 Otto (GmbH & Co KG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.schedoscope.scheduler.driver

import org.scalatest.{ FlatSpec, Matchers }
import org.schedoscope.DriverTests
import org.schedoscope.dsl.transformations.HiveTransformation
import org.schedoscope.test.resources.LocalTestResources
import org.schedoscope.test.resources.TestDriverRunCompletionHandlerCallCounter._

class HiveDriverTest extends FlatSpec with Matchers {
  lazy val driver: HiveDriver = new LocalTestResources().hiveDriver

  "HiveDriver" should "have transformation name hive" taggedAs (DriverTests) in {
    driver.transformationName shouldBe "hive"
  }

  it should "execute hive transformations synchronously" taggedAs (DriverTests) in {

    val driverRunState = driver.runAndWait(HiveTransformation("SHOW TABLES"))

    driverRunState shouldBe a[DriverRunSucceeded[_]]
  }

  it should "execute another hive transformations synchronously" taggedAs (DriverTests) in {
    val driverRunState = driver.runAndWait(HiveTransformation("SHOW TABLES"))

    driverRunState shouldBe a[DriverRunSucceeded[_]]
  }

  it should "execute hive transformations and return errors when running synchronously" taggedAs (DriverTests) in {
    val driverRunState = driver.runAndWait(HiveTransformation("FAIL ME"))

    driverRunState shouldBe a[DriverRunFailed[_]]
  }

  it should "execute hive transformations asynchronously" taggedAs (DriverTests) in {
    val driverRunHandle = driver.run(HiveTransformation("SHOW TABLES"))

    var runWasAsynchronous = false

    while (driver.getDriverRunState(driverRunHandle).isInstanceOf[DriverRunOngoing[_]])
      runWasAsynchronous = true

    runWasAsynchronous shouldBe true
    driver.getDriverRunState(driverRunHandle) shouldBe a[DriverRunSucceeded[_]]
  }

  it should "execute hive transformations and return errors when running asynchronously" taggedAs (DriverTests) in {
    val driverRunHandle = driver.run(HiveTransformation("FAIL ME"))

    var runWasAsynchronous = false

    while (driver.getDriverRunState(driverRunHandle).isInstanceOf[DriverRunOngoing[_]])
      runWasAsynchronous = true

    runWasAsynchronous shouldBe true
    driver.getDriverRunState(driverRunHandle) shouldBe a[DriverRunFailed[_]]
  }

  it should "call its DriverRunCompletitionHandlers' driverRunCompleted upon request" taggedAs (DriverTests) in {
    val runHandle = driver.run(HiveTransformation("SHOW TABLES"))

    while (driver.getDriverRunState(runHandle).isInstanceOf[DriverRunOngoing[_]]) {}

    driver.driverRunCompleted(runHandle)

    driverRunCompletedCalled(runHandle, driver.getDriverRunState(runHandle)) shouldBe true
  }

  it should "call its DriverRunCompletitionHandlers' driverRunStarted upon request" taggedAs (DriverTests) in {
    val runHandle = driver.run(HiveTransformation("SHOW TABLES"))

    driver.driverRunStarted(runHandle)

    driverRunStartedCalled(runHandle) shouldBe true
  }
}