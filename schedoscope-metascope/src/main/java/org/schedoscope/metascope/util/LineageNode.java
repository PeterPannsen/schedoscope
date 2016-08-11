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
package org.schedoscope.metascope.util;

import java.util.ArrayList;
import java.util.List;

public class LineageNode {

  private String label;
  private int id;
  private List<LineageNode> wiredTo;
  private LineageNodeDetails details;
  
  public LineageNode() {
    this.wiredTo = new ArrayList<LineageNode>();
  }

  public LineageNode(String label) {
    this();
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public List<LineageNode> getWiredTo() {
    return wiredTo;
  }

  public void setWiredTo(List<LineageNode> wiredTo) {
    this.wiredTo = wiredTo;
  }

  public void isWiredTo(LineageNode node) {
    this.wiredTo.add(node);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LineageNode)) {
      return false;
    } else {
      LineageNode node = (LineageNode) obj;
      return label.equals(node.getLabel());
    }
  }
  
  public LineageNodeDetails getDetails() {
    if (details == null) {
      this.details = new LineageNodeDetails();
    }
    return details;
  }

}