/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 *
 * in shell:  (such as .cldrrc)
 *   export CWDEBUG="-DCLDR_DTD_CACHE=/tmp/cldrdtd/"
 *   export CWDEFS="-DCLDR_DTD_CACHE_DEBUG=y ${CWDEBUG}"
 *
 *
 * in code:
 *   docBuilder.setEntityResolver(new CachingEntityResolver());
 *
 */

package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.unicode.cldr.draft.FileUtilities;

public class Log {
    static private PrintWriter log;

    public static void logln(int test, String message) {
        if (log != null && test != 0) log.println(message);
    }

    public static void logln(boolean test, String message) {
        if (log != null && test) log.println(message);
    }

    public static void logln(Object message) {
        if (log != null) log.println(message);
    }

    /**
     * @return Returns the log.
     */
    public static PrintWriter getLog() {
        return log;
    }

    /**
     * @param newlog
     *            The log to set.
     */
    public static void setLog(PrintWriter newlog) {
        log = newlog;
    }

    /**
     */
    public static void close() {
        if (log != null) log.close();
    }

    public static void setLog(String dir, String file) throws IOException {
        log = FileUtilities.openUTF8Writer(dir, file);
        log.print('\uFEFF');
    }

    public static void setLog(String file) throws IOException {
        log = FileUtilities.openUTF8Writer(null, file);
        log.print('\uFEFF');
    }

    public static void setLogNoBOM(String file) throws IOException {
        log = FileUtilities.openUTF8Writer(null, file);
    }

    public static void setLogNoBOM(String dir, String file) throws IOException {
        log = FileUtilities.openUTF8Writer(dir, file);
    }

    public static void println() {
        log.println();
    }

    public static void println(String string) {
        log.println(string);
    }

    public static void print(String string) {
        log.print(string);
    }

    /**
     * format a line and print, in 80 character pieces. A bit dumb right now: doesn't handle strings.
     *
     * @param format
     * @param args
     */
    public static void formatln(String format, Object... args) {
        String value = String.format(Locale.ENGLISH, format, args);
        if (value.length() <= 80) {
            log.println(value);
            return;
        }
        // if it is too long, see if there is a comment
        int commentLocation = value.lastIndexOf("//");
        String comment = "";
        if (commentLocation > 0) {
            comment = value.substring(commentLocation);
            value = value.substring(0, commentLocation);
        }
        while (value.length() > 80) {
            int lastSpace = value.lastIndexOf(' ', 80);
            if (lastSpace == -1) {
                log.println(value);
                break;
            }
            log.println(value.substring(0, lastSpace));
            value = value.substring(lastSpace);
        }
        if (value.length() + comment.length() < 79) {
            log.println(value + " " + comment);
            return;
        }
        log.println(value);
        log.println("    " + comment);
    }
}
