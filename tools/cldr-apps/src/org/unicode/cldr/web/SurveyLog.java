/**
 * Copyright (C) 2013 IBM Corporation and Others All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.StackTracker;

/**
 * @author srl
 * 
 */
public class SurveyLog {
    static boolean DEBUG = false;
    private static boolean checkDebug = false;
    // Logging
    public static Logger logger;

    static {
        logger = Logger.getLogger("org.unicode.cldr.SurveyMain");
        // if(DEBUG) {
        // logger.setLevel(Level.ALL);
        // // for(Handler h : logger.getHandlers()) {
        // // h.setLevel(logger.getLevel());
        // // }
        // ConsoleHandler ch = new ConsoleHandler();
        // ch.setLevel(logger.getLevel());
        // logger.addHandler(ch);
        // }
    }

    public static java.util.logging.Handler loggingHandler = null;

    static final void errln(String s) {
        logger.severe(s);
    }

    public static void logException(Throwable t) {
        logException(t, "", null);
    }

    public static void logException(Throwable t, String what) {
        logException(t, what, null);
    }

    public static void logException(String what) {
        logException(null, what, null);
    }

    public static final String RECORD_SEP = "!!!***!!! ";
    public static final String FIELD_SEP = "*** ";

    public enum LogField {
        SURVEY_EXCEPTION, SURVEY_UP, SURVEY_SHUTDOWN, SURVEY_BUSTED, DATE, UPTIME, CTX, LOGSITE, MESSAGE, STACK, SQL, REVISION
    };

    private static File gBaseDir = null;
    private static String gBaseDirStack = null;
    /**
     * Set the logging to happen inside the ST dir
     * 
     * @param homeFile
     */
    public static void setDir(File homeFile) {
        if (gBaseDir != null) {
            throw new InternalError("Error: setDir() was already called once, was at: ." + gBaseDirStack);
        }
        gBaseDirStack= StackTracker.currentStack();
        gBaseDir = homeFile;
    }
    
    private static final class LogEntry {
        StringBuilder sb = new StringBuilder();
        
        LogEntry() {
        }
        
        public LogEntry appendGeneral() {
            long nextTimePost = System.currentTimeMillis();
            appendLine(FIELD_SEP, LogField.DATE,nextTimePost,new Date());
            appendLine(FIELD_SEP, LogField.UPTIME, SurveyMain.uptime);
            return this;
        }
        
        public LogEntry appendLine(Object... args) {
            boolean first = true;
            for(Object o : args) {
                if(first) {
                    first=false;
                } else {
                    sb.append(' ');
                }
                sb.append(o);
            }
            sb.append('\n');
            return this;
        }
        
        public CharSequence write() {
            synchronized(SurveyLog.class) {
                File baseDir = gBaseDir;
                if (baseDir == null || !baseDir.isDirectory()) {
                    setDir(new File("."));
                    System.err.println(SurveyLog.class.getName() + " Warning: Storing exception.log in " + gBaseDir.getAbsolutePath()
                            + "/exception.log");
                }
                try {
                    File logFile = new File(baseDir, "exception.log");
                    OutputStream file = new FileOutputStream(logFile, true); // Append
                    PrintWriter pw = new PrintWriter(new OutputStreamWriter(file, "UTF-8"));
                    pw.append(sb);
                    pw.close();
                    file.close();
                } catch (IOException ioe) {
                    logger.severe("Err: " + ioe + " trying to write log entry!");
                    ioe.printStackTrace();
                }
            }
            return sb;
        }
    }

    public static synchronized void logException(final Throwable exception, String what, WebContext ctx) {
        LogEntry le = new LogEntry();
        le.appendLine(RECORD_SEP, LogField.SURVEY_EXCEPTION, what)
            .appendGeneral();
        if(SurveyMain.isSetup && !SurveyMain.isBusted()) {
            le.appendLine(FIELD_SEP, LogField.REVISION, SurveyMain.getCurrevStr(),SurveyMain.getNewVersion(),CLDRConfig.getInstance().getPhase(),CLDRConfig.getInstance().getEnvironment());
        }
        if (ctx != null) {
            le.appendLine(FIELD_SEP, LogField.CTX, ctx);
        }
        le.appendLine(FIELD_SEP, LogField.LOGSITE, StackTracker.currentStack());
        Throwable t = exception;
        while (t != null) {
            le.appendLine(FIELD_SEP, LogField.MESSAGE, t.toString(), t.getMessage());
            le.appendLine(FIELD_SEP, LogField.STACK, StackTracker.stackToString(t.getStackTrace(), 0));
                    ;
            if (t instanceof SQLException) {
                SQLException se = ((SQLException) t);
                le.appendLine(FIELD_SEP, LogField.SQL, "#"+se.getErrorCode(),se.getSQLState());
                t = se.getNextException();
            } else {
                t = t.getCause();
            }
        }

        logger.severe(le.write().toString());
        if(SurveyMain.isSetup) {
            getChunkyReader().nudge();
        }
        // then, to screen
        
        if(exception instanceof OutOfMemoryError) {
            SurveyMain.markBusted(exception); // would cause infinite recursion if busted() was called
        }
        
    }

    /**
     * @param event
     */
    public static void logEvent(LogField event) {
        new LogEntry().appendLine(RECORD_SEP, event).appendGeneral().write();
        if(SurveyMain.isSetup) 
             getChunkyReader().nudge();
    }
    
    private static ChunkyReader cr = null;

    public static synchronized ChunkyReader getChunkyReader() {
        if (cr == null) {
            cr = new ChunkyReader(new File(CLDRConfig.getInstance().getProperty("CLDRHOME"), "exception.log"), RECORD_SEP, FIELD_SEP, LogField.DATE.name());
        }
        return cr;
    }

    // private static long lastTimePost = -1;
    //
    // /**
    // * Note when the latest exception was recorded
    // * @param nextTimePost
    // */
    // private static synchronized void notifyLatestException(long nextTimePost)
    // {
    // if(lastTimePost < nextTimePost) {
    // lastTimePost = nextTimePost;
    // }
    // }
    //
    //
    //
    // public static synchronized long getLatestException() {
    // if(lastTimePost<0) {
    // File logFile = new File(SurveyMain.homeFile, "exception.log");
    // }
    // return lastTimePost;
    // }

    public static void logException(Throwable t, WebContext ctx) {
        logException(t, "(exception)", ctx);
    }

    public static final void debug(Object string) {
        if (isDebug()) {
            _doDebug(string != null ? string.toString() : null, Level.INFO);
        }
    }

    public static final void debug(String string) {
        if (isDebug()) {
            _doDebug(string, Level.INFO);
        }
    }

    private static void _doDebug(String string, Level l) {
        LogRecord lr = new LogRecord(l, string);
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        lr.setSourceClassName(st[4].getClassName());
        lr.setSourceMethodName(st[4].getMethodName());
        logger.log(lr);
    }

    private static void checkDebug() {
        if (CLDRConfig.getInstance().getProperty("DEBUG", false)) {
            DEBUG = true;
        }
        checkDebug = true;
    }

    public static final boolean isDebug() {
        if (!checkDebug) {
            checkDebug();
        }
        return DEBUG;
    }
    
    static Set<String> alreadyWarned = new HashSet<String>();
    
    /**
     * Warn one time, ignore after that
     * @param string
     */
    public static synchronized void warnOnce(String string) {
        if(!alreadyWarned.contains(string)) {
            logger.log(Level.WARNING, string);
            alreadyWarned.add(string);
        }
    }

}
