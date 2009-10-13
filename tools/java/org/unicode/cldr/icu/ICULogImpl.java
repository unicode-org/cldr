package org.unicode.cldr.icu;

import java.io.PrintWriter;

public class ICULogImpl implements ICULog {
  private final Level level;
  private final PrintWriter out;
  private String status;

  public ICULogImpl(Level level) {
    this.level = level;
    this.out= new PrintWriter(System.out);
  }

  @Override
  public void debug(String msg) {
    out(Level.DEBUG, msg);
  }

  @Override
  public void error(String msg) {
    out(Level.ERROR, msg);
  }

  @Override
  public void error(String msg, Throwable t) {
    if (t == null) {
      error(msg);
    } else {
      out(Level.ERROR, msg + " '" + t.toString() + "'");
      t.printStackTrace(out);
    }
  }

  @Override
  public void info(String msg) {
    out(Level.INFO, msg);
  }

  @Override
  public void log(String msg) {
    out(Level.LOG, msg);
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public void warning(String msg) {
    out(Level.WARNING, msg);
  }

  protected void out(Level level, String msg) {
    if (willOutput(level)) {
      if (status != null) {
        out.format("%s (%s): %s%n", level, status, msg);
      } else {
        out.format("%s: %s%n", level, msg);
      }
      out.flush();
    }
  }

  @Override
  public boolean willOutput(Level level) {
    return level.ordinal() >= this.level.ordinal();
  }
}
