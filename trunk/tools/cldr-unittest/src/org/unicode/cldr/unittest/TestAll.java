//##header J2SE15

package org.unicode.cldr.unittest;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;

import org.unicode.cldr.util.CLDRConfig;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {

    private static interface FormattableDate {
        String format(Date d);
    }

    /**
     * NullObject, to suppress Timestamp printing
     *
     * @author ribnitz
     *
     */
    private static class NullFormatableDate implements FormattableDate {

        @Override
        public String format(Date d) {
            return null;
        }
    }

    /**
     * Simplistic approach at formatting a Date (using Date and Time)
     *
     * @author ribnitz
     *
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
     *
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
     *
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

    public static void main(String[] args) {
        final boolean doTimeStamps = false;
        TimeStampingPrintWriter tspw = new TimeStampingPrintWriter(System.out);
        if (!doTimeStamps) {
            tspw.setFormatableDate(new NullFormatableDate());
        }
        long startTime = System.currentTimeMillis();
        int errCount = CLDRConfig.getInstance().setTestLog(new TestAll())
            .run(args, tspw);
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

    public TestAll() {
        super(new String[] {
            "org.unicode.cldr.unittest.LocaleMatcherTest",
            "org.unicode.cldr.unittest.GenerateTransformTest",
            "org.unicode.cldr.unittest.LanguageInfoTest",
            "org.unicode.cldr.unittest.LanguageTest",
            "org.unicode.cldr.unittest.LikelySubtagsTest",
            "org.unicode.cldr.unittest.NumberingSystemsTest",
            "org.unicode.cldr.unittest.StandardCodesTest",
            "org.unicode.cldr.unittest.TestAnnotations",
            "org.unicode.cldr.unittest.TestBasic",
            "org.unicode.cldr.unittest.TestCLDRFile",
            "org.unicode.cldr.unittest.TestCLDRUtils",
            "org.unicode.cldr.unittest.TestCanonicalIds",
            "org.unicode.cldr.unittest.TestCasingInfo",
            "org.unicode.cldr.unittest.TestCheckCLDR",
            "org.unicode.cldr.unittest.TestComparisonBuilder",
            "org.unicode.cldr.unittest.TestCoverageLevel",
            "org.unicode.cldr.unittest.TestDTDAttributes",
            "org.unicode.cldr.unittest.TestDisplayAndInputProcessor",
            "org.unicode.cldr.unittest.TestExampleGenerator",
            "org.unicode.cldr.unittest.TestExternalCodeAPIs",
            "org.unicode.cldr.unittest.TestFallbackIterator",
            "org.unicode.cldr.unittest.TestIdentifierInfo",
            "org.unicode.cldr.unittest.TestIdentity",
            "org.unicode.cldr.unittest.TestInheritance",
            "org.unicode.cldr.unittest.TestKeyboardModifierSet",
            "org.unicode.cldr.unittest.TestLdml2ICU",
            "org.unicode.cldr.unittest.TestLocalCurrency",
            "org.unicode.cldr.unittest.TestLocale",
            "org.unicode.cldr.unittest.TestLruMap",
            "org.unicode.cldr.unittest.TestMetadata",
            "org.unicode.cldr.unittest.TestOutdatedPaths",
            "org.unicode.cldr.unittest.TestPathHeader",
            "org.unicode.cldr.unittest.TestPaths",
            "org.unicode.cldr.unittest.TestPseudolocalization",
            "org.unicode.cldr.unittest.TestScriptMetadata",
            "org.unicode.cldr.unittest.TestSupplementalInfo",
            "org.unicode.cldr.unittest.TestTransforms",
            "org.unicode.cldr.unittest.TestUtilities",
            "org.unicode.cldr.unittest.TestCLDRLocaleCoverage",
            "org.unicode.cldr.unittest.TestDayPeriods",
            "org.unicode.cldr.unittest.TestSubdivisions",
            "org.unicode.cldr.unittest.TestAliases",
            "org.unicode.cldr.unittest.TestValidity",
            "org.unicode.cldr.unittest.TestDtdData",
            "org.unicode.cldr.unittest.TestCldrFactory",
            "org.unicode.cldr.unittest.TestUnContainment",
            //            "org.unicode.cldr.unittest.TestCollators" See Ticket #8288
        },
            "All tests in CLDR");
    }

    public static final String CLASS_TARGET_NAME = "CLDR";
}
