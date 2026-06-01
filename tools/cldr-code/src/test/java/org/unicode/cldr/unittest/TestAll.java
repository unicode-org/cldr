// ##header J2SE15

package org.unicode.cldr.unittest;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.icu.dev.test.TestFmwk.TestGroup;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.ShimmedMain;

/** Top level test used to run all other tests as a batch. */
public class TestAll extends TestGroup {
    public static String[] getAllTests() {
        return ShimmedMain.findAllTests(TestAll.class.getPackage());
    }

    private static interface FormattableDate {
        String format(Date d);
    }

    /**
     * NullObject, to suppress Timestamp printing
     *
     * @author ribnitz
     */
    private static class NullFormatableDate implements FormattableDate {

        @Override
        public String format(@SuppressWarnings("unused") Date d) {
            return null;
        }
    }

    /**
     * Simplistic approach at formatting a Date (using Date and Time)
     *
     * @author ribnitz
     */
    private static class SimpleFormattableDate implements FormattableDate {
        private final DateFormat df;

        public SimpleFormattableDate() {
            df = new SimpleDateFormat("y-MM-d HH:mm:ss");
        }

        @Override
        public String format(Date d) {
            return " << " + df.format(d) + " >>";
        }
    }

    /**
     * Class putting a timestamp at the end of each line output
     *
     * @author ribnitz
     */
    private static class TimeStampingPrintWriter extends PrintWriter {
        protected FormattableDate df = new SimpleFormattableDate();

        public TimeStampingPrintWriter(Writer out, boolean autoFlush) {
            super(out, autoFlush);
        }

        public TimeStampingPrintWriter(Writer out) {
            super(out);
            // TODO Auto-generated constructor stub
        }

        public TimeStampingPrintWriter(OutputStream out, boolean autoFlush) {
            super(out, autoFlush);
            // TODO Auto-generated constructor stub
        }

        public TimeStampingPrintWriter(OutputStream out) {
            super(out);
            // TODO Auto-generated constructor stub
        }

        public void setFormatableDate(FormattableDate aDate) {
            df = aDate;
        }

        private String getFormattedDateString() {
            return df.format(new Date());
        }

        @Override
        public void write(String s) {
            if (s.equals("\n") || s.equals("\r\n")) {
                String ss = getFormattedDateString();
                if (ss != null) {
                    super.write(" " + ss + s);
                } else {
                    super.write(s);
                }
            } else {
                super.write(s);
            }
        }
    }

    /**
     * Helper class to convert milliseconds into hours/minuy
     *
     * @author ribnitz
     */
    private static class DateDisplayBean {
        public final int hours;
        public final int minutes;
        public final int seconds;
        public final int millis;

        public DateDisplayBean(long ms) {
            long m = ms;
            hours = (int) (m / (60 * 60 * 1000));
            if (hours > 0) {
                m -= (hours * 60 * 60 * 1000);
            }
            minutes = (int) (m / (60 * 1000));
            if (minutes > 0) {
                m -= minutes * 60 * 1000;
            }
            seconds = (int) (m / 1000);
            millis = (int) (m - (seconds * 1000));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (hours > 0) {
                sb.append(hours);
                sb.append(hours > 1 ? " hours " : " hour ");
            }
            if (minutes > 0) {
                sb.append(minutes);
                sb.append(minutes > 1 ? " minutes " : " minute ");
            }
            if (seconds > 0) {
                sb.append(seconds);
                sb.append(seconds > 1 ? " seconds " : " second ");
            }
            if (millis > 0) {
                sb.append(millis);
                sb.append(millis > 1 ? " milliseconds" : " millisecond");
            }
            return sb.toString();
        }
    }

    /** This is the entrypoint from the command line */
    public static void main(String[] args) {
        // Special cldr-code setup and options
        final boolean doTimeStamps = false;
        TimeStampingPrintWriter tspw = new TimeStampingPrintWriter(System.out);
        if (!doTimeStamps) {
            tspw.setFormatableDate(new NullFormatableDate());
        }
        long startTime = System.currentTimeMillis();
        int errCount = main(args, tspw);
        long endTime = System.currentTimeMillis();
        DateDisplayBean dispBean = new DateDisplayBean(endTime - startTime);
        StringBuffer sb = new StringBuffer();
        sb.append("Tests took ");
        sb.append(dispBean.toString());
        System.out.println(sb.toString());

        if (errCount != 0) {
            System.exit(1);
        }
    }

    /** This is the entrypoint from JUnit */
    public static int main(String[] args, PrintWriter logs) {
        /** Setup stuff */
        // No setup stuff for cldr-code currently.

        /** boilerplate */
        TestFmwk test = CLDRConfig.getInstance().setTestLog(new TestAll());
        return test.run(args, logs);
    }

    public TestAll() {
        super(getAllTests(), "All tests in CLDR");
    }

    public static final String CLASS_TARGET_NAME = "CLDR";
}
