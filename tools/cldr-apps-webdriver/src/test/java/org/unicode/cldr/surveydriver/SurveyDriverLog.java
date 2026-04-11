package org.unicode.cldr.surveydriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.*;

public class SurveyDriverLog {

    private static final Logger logger = Logger.getLogger(SurveyDriverLog.class.getName());

    static {
        logger.setUseParentHandlers(false);
        MyFormatter formatter = new MyFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
        // add a handler that appends to a flat log
        logger.addHandler(
                new Handler() {
                    @Override
                    public void close() {}

                    @Override
                    public void flush() throws SecurityException {
                        // auto flush
                    }

                    @Override
                    public void publish(LogRecord record) {
                        // check each time
                        if (SurveyDriverCredentials.haveOutputDir()) {
                            try {
                                printlnFile(
                                        SurveyDriverCredentials.getDriverLogFile(),
                                        record.getMessage());
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                                System.err.println(
                                        "Could not write to log: "
                                                + ioe.getMessage()
                                                + " - "
                                                + record.getMessage());
                            }
                        }
                    }
                });
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

    // helper function
    private static final void printlnFile(final File f, final String str) throws IOException {
        try (final Writer out = new FileWriter(f, true)) {
            out.append(str).append('\n');
        }
    }

    /** print to summary.md */
    public static void printlnSummary(final String s) {
        if (SurveyDriverCredentials.haveOutputDir()) {
            try {
                printlnFile(SurveyDriverCredentials.getDriverSummaryFile(), s);
            } catch (IOException ioe) {
                System.err.println("%% Could not write to summary file");
            }
        }
        System.out.println("%% " + s);
    }
}
