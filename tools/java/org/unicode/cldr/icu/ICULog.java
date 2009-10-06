package org.unicode.cldr.icu;

public interface ICULog {
  public enum Level {
    DEBUG, INFO, LOG, WARNING, ERROR;
  }
  
  void setStatus(String status);
  
  void debug(String msg);
  void info(String msg);
  void log(String msg);
  void warning(String msg);
  void error(String msg);
  void error(String msg, Throwable t);
}
