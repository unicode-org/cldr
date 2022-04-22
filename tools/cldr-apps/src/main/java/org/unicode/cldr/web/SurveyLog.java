/**
 * Copyright (C) 2013 IBM Corporation and Others All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;

/**
 * @author srl
 */
public class SurveyLog {
    /**
     * Get a Logger class for the specified calling class.
     * @param clazz
     * @return
     */
    public static final Logger forClass(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }


    // Logging. Using a static reference for pedagogical reasons.
    private static final Logger logger = SurveyLog.forClass(SurveyLog.class);


    static boolean DEBUG = false;
    private static boolean checkDebug = false;
    @Deprecated
    static final void errln(String s) {
        logger.severe(s);
    }

    @Deprecated
    public static void logException(Throwable t) {
        logException(t, "", null);
    }

    @Deprecated
    public static void logException(Throwable t, String what) {
        logException(t, what, null);
    }

    @Deprecated
    public static void logException(String what) {
        logException(null, what, null);
    }

    public static final String RECORD_SEP = "!!!***!!! ";
    public static final String FIELD_SEP = "*** ";

    enum LogField {
        SURVEY_EXCEPTION, DATE, UPTIME, CTX, LOGSITE, MESSAGE, STACK, SQL, REVISION, SURVEYEXCEPTION
    }

    private static File gBaseDir = null;

    /**
     * Set the logging to happen inside the ST dir
     *
     * @param homeFile
     */
    public static void setDir(File homeFile) {
        if (gBaseDir != null) {
            throw new InternalError("Error: setDir() was already called once.");
        }
        gBaseDir = homeFile;
    }

    public static void logException(Logger logger, final Throwable exception, String what, WebContext ctx) {
        logger.log(Level.SEVERE, what, exception);
        countException(exception);
    }

    /**
     * Count an exception in the SurveyMetrics
     */
    public static void countException(final Throwable exception) {
        if (CookieSession.sm != null && CookieSession.sm.surveyMetrics != null) {
            CookieSession.sm.surveyMetrics.countException(exception);
        }
    }

    /**
     *
     * @param exception
     * @param what
     * @param ctx
     */
    public static synchronized void logException(final Throwable exception, String what, WebContext ctx) {
        logException(logger, exception, what, ctx);
    }

    private static ChunkyReader cr = null;

    public static synchronized ChunkyReader getChunkyReader() {
        if (cr == null) {
            cr = new ChunkyReader(new File(CLDRConfig.getInstance().getProperty(CldrUtility.HOME_KEY), "exception.log"), RECORD_SEP
                + LogField.SURVEY_EXCEPTION.name(), FIELD_SEP, LogField.DATE.name());
        }
        return cr;
    }

    @Deprecated
    public static void logException(Throwable t, WebContext ctx) {
        logException(logger, t, ctx);
    }


    public static void logException(Logger logger, Throwable t, String string) {
        logger.log(Level.SEVERE, string, t);
        countException(t);
    }

    public static void logException(Logger logger, Throwable t, WebContext ctx) {
        logException(logger, t, "(exception)", ctx);
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

    static ConcurrentHashMap<String, Boolean> alreadyWarned = new ConcurrentHashMap<>();

    /**
     * Warn one time, ignore after that
     * @param string
     * @deprecated use warnOnce with your own logger
     */
    @Deprecated
    public static void warnOnce(String string) {
        warnOnce(logger, string);
    }

    public static void warnOnce(Logger logger, String string) {
        final String key = logger.getName()+":"+string;
        if (alreadyWarned.putIfAbsent(key, true) == null) {
            logger.log(Level.WARNING, string);
        }
    }

    static void shutdown() {
        debug("SurveyLog: Number of items in warnOnce() list: " + alreadyWarned.size());
    }
}
