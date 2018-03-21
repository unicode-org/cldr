package org.unicode.cldr.icu;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Formatter;
import java.util.Locale;

import org.unicode.cldr.util.InputStreamFactory;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.VersionInfo;

/**
 * @author George Rhoten
 */
public class ListNumbers {

    // For time testing
    /*
     * private static final double[][] RANGES = new double[][] {
     * new double[]{0, 0},
     * new double[]{0.001, 0.001},
     * new double[]{0.01, 0.01},
     * new double[]{0.011, 0.011},
     * new double[]{0.1, 0.1},
     * new double[]{0.2, 0.2},
     * new double[]{0.21, 0.21},
     * new double[]{0.3, 0.3},
     * new double[]{0.321, 0.321},
     * new double[]{0.4321, 0.4321},
     * new double[]{1.001, 1.001},
     * new double[]{1.01, 1.01},
     * new double[]{1.4321, 1.4321},
     * new double[]{59.4321, 61.4321},
     * new double[]{1, 21},
     * new double[]{59, 62},
     * new double[]{119, 121},
     * new double[]{3599, 3601},
     * new double[]{3659, 3661},
     * new double[]{86399, 86401},
     * new double[]{86459, 86461},
     * new double[]{89999, 90001},
     * new double[]{90059, 90061},
     * new double[]{172799, 172801},
     * new double[]{180119, 180122},
     * new double[]{180119.001, 180122.001},
     * new double[]{180119.002, 180122.002},
     * };
     */
    // For standard simple testing
    private static final double[][] RANGES = new double[][] {
        new double[] { -1, 0 },
        new double[] { 0.23, 0.23 },
        new double[] { 1, 31 },
        new double[] { 98, 102 },
        new double[] { 998, 1002 },
        new double[] { 1998, 2002 },
        new double[] { 9998, 10002 },
        new double[] { 100000, 100001 },
        new double[] { 1000000, 1000001 },
        new double[] { 10000000, 10000001 },
        new double[] { 100000000, 100000001 },
        new double[] { 1000000000, 1000000001 },
    };

    /*
     * private static final long[][] RANGES = new long[][] {
     * new long[]{1, 91},
     * new long[]{98, 102},
     * new long[]{200, 202},
     * new long[]{300, 302},
     * new long[]{400, 402},
     * new long[]{500, 502},
     * new long[]{600, 602},
     * new long[]{700, 702},
     * new long[]{800, 802},
     * new long[]{900, 902},
     * new long[]{998, 1002},
     * new long[]{1998, 2022},
     * new long[]{9998, 10002},
     * new long[]{20000, 20002},
     * new long[]{100000, 100001},
     * new long[]{200000, 200000},
     * new long[]{1000000, 1000002},
     * new long[]{2000000, 2000002},
     * new long[]{10000000, 10000001},
     * new long[]{20000000, 20000000},
     * new long[]{100000000, 100000002},
     * new long[]{200000000, 200000000},
     * new long[]{1000000000, 1000000001},
     * new long[]{2000000000, 2000000002},
     * new long[]{1000000000000L, 1000000000000L},
     * new long[]{2000000000000L, 2000000000000L},
     * new long[]{1000000000000000L, 1000000000000000L},
     * new long[]{2000000000000000L, 2000000000000000L},
     * };
     */

    private static String getRulePrefix(String currRuleName) {
        try {
            return currRuleName.substring(1, currRuleName.indexOf('-'));
        } catch (StringIndexOutOfBoundsException e) {
            return currRuleName;
        }
    }

    private static String getDisplayName(RuleBasedNumberFormat spellout, String currRuleName) {
        try {
            String prefix = currRuleName.substring(1, currRuleName.indexOf('-'));
            if (prefix.equals("spellout") || prefix.equals("digits") || prefix.equals("duration")) {
                String suffix = currRuleName.substring(currRuleName.indexOf('-') + 1);
                // return suffix + (spellout.getDefaultRuleSetName().equals(currRuleName) ? "<br/><em>Default</em>" :
                // "");
                return suffix;
            }
        } catch (StringIndexOutOfBoundsException e) {
        }
        return currRuleName;
    }

    private static String fileToString(String file) {
        String str = "";
        try (InputStream is = InputStreamFactory.createInputStream(new File(file));
            Reader reader = new InputStreamReader(is, "UTF-8");) {
//            InputStream is = new FileInputStream(file);

            char[] buffer = new char[is.available() + 1];
            int i = reader.read(buffer);
//            reader.close();
            if (i >= buffer.length) {
                // This should never happen. Summary of UTF-8 to UTF-16 conversion per codepoint:
                // 1 byte -> 1 char, 2 bytes -> 1 char, 3 bytes -> 1 char, 4 bytes -> 2 chars.
                throw new IOException("File encoding expanded when converting to UTF-16");
            } else if (i > 0) {
                str = new String(buffer, 0, i);
            }
        } catch (Exception e) {
            System.err.println(e);
        }
        return str;
    }

    private static String getNumberStyle(String ruleName, double val, boolean isRTL) {
        String bidiStyle = "";
        if (isRTL) {
            bidiStyle = " rtl";
        }
        if ((val < 1 || val != (long) val) && (ruleName.contains("ordinal") || ruleName.contains("year"))) {
            return " class=\"nonsense" + bidiStyle + "\"";
        }
        if (ruleName.contains("@noparse")) {
            return " class=\"noparse" + bidiStyle + "\"";
        }
        if (isRTL) {
            return " class=\"rtl\"";
        }
        return "";
    }

    private static void printLine(RuleBasedNumberFormat[] rbnfs, double num, boolean isRTL) {
        long longVal = (long) num;
        String numStr;
        if (longVal != num) {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);
            formatter.format("%.3f", num);
            numStr = sb.toString();
            formatter.close();
        } else {
            numStr = Long.toString(longVal);
        }

        System.out.print("<tr><td>" + numStr + "</td>");
        for (RuleBasedNumberFormat rbnf : rbnfs) {
            for (String name : rbnf.getRuleSetNames()) {
                System.out.print("<td" + getNumberStyle(name, num, isRTL) + ">" + rbnf.format(num, name) + "</td>");
            }
        }
        System.out.println("</tr>");
    }

    private static void printSkipLine(RuleBasedNumberFormat[] rbnfs) {
        // System.out.println("<tr><td colspan=\"" + tableColumns + "\">...</td></tr>");
        System.out.print("<tr><td>...</td>");
        for (RuleBasedNumberFormat rbnf : rbnfs) {
            for (String name : rbnf.getRuleSetNames()) {
                System.out.print("<th class=\"thead\"><b>" + getDisplayName(rbnf, name) + "</b></th>");
            }
        }
        System.out.println("</tr>");
    }

    private static void printTable(ULocale loc, RuleBasedNumberFormat[] rbnfs) {
        System.out.println("<h2 style=\"margin-top: 3em\"><a name=\"" + loc + "\"></a>" + loc + " - "
            + loc.getDisplayName() + "</h2>");
        System.out.println("<table border=\"1\" cellspacing=\"0\" cellpadding=\"2\">");
        System.out.println("<tr><th rowspan=\"2\" class=\"thead\"><b>Number</b></th>");
        for (RuleBasedNumberFormat rbnf : rbnfs) {
            int numColumns = rbnf.getRuleSetNames().length;
            System.out.print("<th colspan=\"" + numColumns + "\" style=\"text-align:center;\">"
                + getRulePrefix(rbnf.getDefaultRuleSetName()));
            System.out.print("<br/><span style=\"color: gray\">Default = "
                + getDisplayName(rbnf, rbnf.getDefaultRuleSetName()));
            System.out.println("</span></th>");
        }
        System.out.println("</tr>");
        System.out.print("<tr>");
        for (RuleBasedNumberFormat rbnf : rbnfs) {
            for (String name : rbnf.getRuleSetNames()) {
                System.out.print("<th class=\"thead\"><b>" + getDisplayName(rbnf, name) + "</b></th>");
            }
        }
        System.out.println("</tr>");
        byte direction = Character.getDirectionality(rbnfs[0].format(1).charAt(0));
        boolean isRTL = (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC);
        for (int rangeIdx = 0; rangeIdx < RANGES.length; rangeIdx++) {
            double end = RANGES[rangeIdx][1];
            if (end == (long) end && rangeIdx != 0) {
                printSkipLine(rbnfs);
            }
            for (double num = RANGES[rangeIdx][0]; num <= end; num++) {
                printLine(rbnfs, num, isRTL);
            }
        }
        System.out.println("</table>");
    }

    public static void main(String[] args) {
        System.out.println("<html>");
        System.out.println("<head>");
        System.out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        System.out.println("<title>RBNF output</title>");
        System.out.println("</head>");
        System.out.println("<body>");
        System.out.println("<p>Generated from: ICU4J " + VersionInfo.ICU_VERSION + "</p>");
        // Yeah. Yeah. It's invalid HTML, but that's how we get style into the wiki.
        System.out.println("<style type=\"text/css\">");
        System.out.println(".rtl {text-align:right}");
        System.out.println(".noparse {background-color:lightgray}");
        System.out.println(".nonsense {color:gray; background-color:lightgray}");
        System.out.println(".thead {text-align:center; font-weight:bold;}");
        System.out.println("</style>");
        if (args.length > 1) {
            RuleBasedNumberFormat[] rbnfs = new RuleBasedNumberFormat[] {
                new RuleBasedNumberFormat(fileToString(args[1]), new ULocale(args[0]))
            };
            printTable(new ULocale(args[0]), rbnfs);
        } else {
            for (ULocale loc : NumberFormat.getAvailableULocales()) {
                RuleBasedNumberFormat[] rbnfs = new RuleBasedNumberFormat[] {
                    new RuleBasedNumberFormat(loc, RuleBasedNumberFormat.SPELLOUT),
                    new RuleBasedNumberFormat(loc, RuleBasedNumberFormat.ORDINAL),
                    // new RuleBasedNumberFormat(loc, RuleBasedNumberFormat.DURATION)
                };
                if (!rbnfs[0].getLocale(ULocale.ACTUAL_LOCALE).equals(loc)) {
                    // Uninteresting duplicate data. Show only the minimal set of information.
                    continue;
                }
                try {
                    // Hack to minimize data displayed because ULocale.ACTUAL_LOCALE does not work as desired.
                    @SuppressWarnings("deprecation")
                    UResourceBundle bundle = UResourceBundle.getBundleInstance("com/ibm/icu/impl/data/icudt"
                        + VersionInfo.ICU_DATA_VERSION_PATH + "/rbnf", loc);
                    if (bundle.getSize() == 2) {
                        UResourceBundle parent = bundle.get("%%Parent");
                        System.out.println("<p>" + loc + " is an alias to " + parent.getString() + " - "
                            + new ULocale(parent.getString()).getDisplayName() + "</p>");
                        continue;
                    }
                } catch (Exception e) {
                    // normal case
                }
                printTable(loc, rbnfs);
            }
            RuleBasedNumberFormat[] rbnfNumbering = new RuleBasedNumberFormat[] {
                new RuleBasedNumberFormat(new ULocale(""), RuleBasedNumberFormat.NUMBERING_SYSTEM),
                new RuleBasedNumberFormat(new ULocale(""), RuleBasedNumberFormat.SPELLOUT),
                new RuleBasedNumberFormat(new ULocale(""), RuleBasedNumberFormat.ORDINAL)
            };
            printTable(new ULocale(""), rbnfNumbering);
        }
        System.out.println("</body>");
        System.out.println("</html>");
    }
}
