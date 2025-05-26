package org.unicode.cldr.surveydriver;

import java.util.logging.*;

public class SurveyDriverLog {

    private static final Logger logger = Logger.getLogger(SurveyDriverLog.class.getName());

    static {
        logger.setUseParentHandlers(false);
        MyFormatter formatter = new MyFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
    }

    public static void println(Exception e) {
        logger.severe("Exception: " + e);
    }

    public static void println(String s) {
        logger.info(s);
    }

    static class MyFormatter extends Formatter {

        public String format(LogRecord record) {
            return formatMessage(record) + "\n";
        }
    }
}
