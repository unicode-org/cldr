package org.unicode.cldr.icu;

public interface ICULog {
  public enum Level {
    DEBUG, INFO, LOG, WARNING, ERROR;
  }

  boolean willOutput(Level level);
  
  void setStatus(String status);

  void debug(String msg);
  void info(String msg);
  void log(String msg);
  void warning(String msg);
  void error(String msg);
  void error(String msg, Throwable t);
  
  /**
   * Outputs and flushes text with no status or level message.
   * Used for 'ticks' during long-running processes and
   * compact output.
   */
  public interface Emitter {
    /** Outputs and flushes text, no newline */
    void emit(String text);
    /** Emits a newline */
    void nl();
  }
  
  /**
   *  Returns an emitter.  If the log will output at this level,
   *  the emitter will forward the text, otherwise it will silently
   *  ignore it.  Use willOutput to see if the emitter will actually
   *  emit the text.
   */
  Emitter emitter(Level level);
}
