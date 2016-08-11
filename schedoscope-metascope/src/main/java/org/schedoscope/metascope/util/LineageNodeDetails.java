package org.schedoscope.metascope.util;

public class LineageNodeDetails {

  private String databaseName;
  private long lastTransformation;
  
  public void setDatabase(String databaseName) {
    this.databaseName = databaseName;
  }
  
  public String getDatabaseName() {
    return databaseName;
  }
  
  public long getLastTransformation() {
    return lastTransformation;
  }
  
  public void setLastTransformation(long lastTransformation) {
    this.lastTransformation = lastTransformation;
  }
  
}
