package org.unicode.cldr.icu;

import java.io.PrintWriter;

public class ICULogImpl implements ICULog {
    private final Level level;
    private final PrintWriter out;
    private String status;

    public ICULogImpl(Level level) {
        this.level = level;
        this.out = new PrintWriter(System.out);
    }

    public void debug(String msg) {
        out(Level.DEBUG, msg);
    }

    public void error(String msg) {
        out(Level.ERROR, msg);
    }

    public void error(String msg, Throwable t) {
        if (t == null) {
            error(msg);
        } else {
            out(Level.ERROR, msg + " '" + t.toString() + "'");
            t.printStackTrace(out);
        }
        out.flush();
    }

    public void info(String msg) {
        out(Level.INFO, msg);
    }

    public void log(String msg) {
        out(Level.LOG, msg);
    }

    public void setStatus(String status) {
        this.status = status;
    }

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

    public boolean willOutput(Level level) {
        return level.ordinal() >= this.level.ordinal();
    }

    public Emitter emitter(Level level) {
        return willOutput(level) ? logEmitter : nullEmitter;
    }

    // This buffers because ant replaces System.out with an implementation
    // that prepends the name of the running task and appends a newline
    // whenever we flush.
    private final Emitter logEmitter = new Emitter() {
        private final StringBuilder buf = new StringBuilder();

        public void emit(String text) {
            buf.append(text);
        }

        public void nl() {
            out.println(buf.toString());
            out.flush();
            buf.setLength(0);
        }
    };

    private static final Emitter nullEmitter = new Emitter() {
        public void emit(String text) {
        }

        public void nl() {
        }
    };
}
