package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.GenerateMaximalLocales.OutputStyle;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;

public class GenerateLikelySubtagTests {
    private static final String SEPARATOR = CldrUtility.LINE_SEPARATOR;
    private static final OutputStyle OUTPUT_STYLE = OutputStyle.XML;
    private static PrintWriter out;

    private static final String VERSION = CLDRFile.GEN_VERSION;

    public static void main(String[] args) throws IOException {
        out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY,
            "test/supplemental/likelySubtagTests" + (OUTPUT_STYLE == OutputStyle.XML ? ".xml" : ".txt"));
        if (OUTPUT_STYLE == OutputStyle.C) {
            out.println("// START");
        } else {
            out.println("<?xml version='1.0' encoding='UTF-8' ?>" + CldrUtility.LINE_SEPARATOR +
                "<!DOCTYPE cldrTest SYSTEM '../../common/dtd/cldrTest.dtd'>" + CldrUtility.LINE_SEPARATOR +
                "<!-- For information, see readme.html -->" + CldrUtility.LINE_SEPARATOR +
                "<cldrTest version='" + VERSION + "' base='aa'>" + CldrUtility.LINE_SEPARATOR +
                "  <likelySubtags>");
        }
        SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
        Map<String, String> likelySubtags = supplementalData.getLikelySubtags();

        if (OUTPUT_STYLE == OutputStyle.C) {
            writeTestLine2("FROM", "ADD-LIKELY", "REMOVE-LIKELY");
        }
        Set<String> testedAlready = new HashSet<String>();

        for (final String from : likelySubtags.keySet()) {
            final String to = likelySubtags.get(from);
            final String max = writeTestLine(from, likelySubtags);
            if (!to.equals(max)) {
                throw new IllegalArgumentException();
            }
            testedAlready.add(to);
        }
        LanguageTagParser ltp = new LanguageTagParser();
        for (String lang : new String[] { "und", "es", "zh", "art" }) { //
            ltp.setLanguage(lang);
            for (String script : new String[] { "", "Zzzz", "Latn", "Hans", "Hant", "Moon" }) {
                ltp.setScript(script);
                for (String region : new String[] { "", "ZZ", "CN", "TW", "HK", "AQ" }) {
                    ltp.setRegion(region);
                    String tag = ltp.toString();
                    if (testedAlready.contains(tag)) {
                        continue;
                    }
                    writeTestLine(tag, likelySubtags);
                    testedAlready.add(tag);
                }
            }
        }
        if (OUTPUT_STYLE == OutputStyle.C) {
            out.println(CldrUtility.LINE_SEPARATOR + "// END");
        } else {
            out.println("  </likelySubtags>" + CldrUtility.LINE_SEPARATOR + "</cldrTest>");
        }
        out.close();
    }

    private static String writeTestLine(final String from, Map<String, String> likelySubtags) {
        final String maxFrom = LikelySubtags.maximize(from, likelySubtags);
        final String minFrom = LikelySubtags.minimize(from, likelySubtags, true);
        writeTestLine2(from, maxFrom, minFrom);
        return maxFrom;
    }

    private static void writeTestLine2(final String from, final String maxFrom, final String minFrom) {
        if (OUTPUT_STYLE == OutputStyle.C) {
            out.print(
                "  {"
                    // + SEPARATOR + "    // " + comment
                    + SEPARATOR + "    " + getItem(from) + ","
                    + SEPARATOR + "    " + getItem(maxFrom) + ","
                    + SEPARATOR + "    " + getItem(minFrom) // +","
                    + CldrUtility.LINE_SEPARATOR + "  },");
        } else {
            out.println("    <!-- " + printNameOrError(from) + " \u2192 " +
                printNameOrError(maxFrom) + " \u2192 " +
                printNameOrError(minFrom) + " -->");
            out.println("    <result input='" + getNameOrError(from) +
                "' add='" + getNameOrError(maxFrom) +
                "' remove='" + getNameOrError(minFrom) + "'/>");
        }

    }

    private static String printNameOrError(final String maxFrom) {
        String result = GenerateMaximalLocales.printingName(maxFrom, "");
        if (result == null) {
            return "ERROR";
        }
        return result;
    }

    private static String getNameOrError(final String from) {
        String result = GenerateMaximalLocales.toAlt(from, true);
        if (result == null) {
            return "ERROR";
        }
        return result;
    }

    private static String getItem(String from) {
        final String toAlt = getNameOrError(from);
        if (toAlt == null) {
            return null;
        }
        return "\"" + toAlt + "\"";
    }

}
