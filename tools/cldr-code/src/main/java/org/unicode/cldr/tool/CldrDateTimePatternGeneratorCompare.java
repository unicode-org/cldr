package org.unicode.cldr.tool;

import com.ibm.icu.text.DateTimePatternGenerator;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrDateTimePatternGenerator;
import org.unicode.cldr.util.Factory;

/**
 * Compares the behavior of CldrDateTimePatternGenerator against ICU4J's DateTimePatternGenerator.
 */
@CLDRTool(
        alias = "compare-cldr-dtpg",
        description =
                "Compares CldrDateTimePatternGenerator against ICU4J DateTimePatternGenerator",
        url = CLDRURLS.TOOLSURL)
public class CldrDateTimePatternGeneratorCompare {
    private static final String[] SKELETONS = {
        // Era (G): 1..5
        "G",
        "GG",
        "GGG",
        "GGGG",
        "GGGGG",
        "GyMd",
        "GGyMd",
        "GGGyMd",
        "GGGGyMd",
        "GGGGGyMd",

        // Year (y): 1..4
        "y",
        "yy",
        "yyyy",
        "yMd",
        "yyMd",
        "yyyyMd",

        // Month (M): 1..5
        "M",
        "MM",
        "MMM",
        "MMMM",
        "MMMMM",
        "yM",
        "yMM",
        "yMMM",
        "yMMMM",
        "yMMMMM",

        // Day (d): 1..2
        "d",
        "dd",
        "yMd",
        "yMdd",

        // Day of week (E): 1..6
        // Skip EE and EEE since they canonicalize to E
        "E",
        "EEEE",
        "EEEEE",
        "EEEEEE",
        "yMdE",
        "yMdEEEE",
        "yMdEEEEE",
        "yMdEEEEEE",

        // Hour 1-12 (h): 1..2
        "h",
        "hh",
        "hm",
        "hhm",

        // Hour 0-23 (H): 1..2
        "H",
        "HH",
        "Hm",
        "HHm",

        // Hour preferred (j): 1..6
        "j",
        "jj",
        "jjj",
        "jjjj",
        "jjjjj",
        "jjjjjj",
        "jm",
        "jjm",
        "jjjm",
        "jjjjm",
        "jjjjjm",
        "jjjjjjm",

        // Hour preferred, no am/pm (J): 1..6
        "J",
        "JJ",
        "JJJ",
        "JJJJ",
        "JJJJJ",
        "JJJJJJ",
        "Jm",
        "JJm",
        "JJJm",
        "JJJJm",
        "JJJJJm",
        "JJJJJJm",

        // Hour preferred, context-dependent (C): 1..6
        "C",
        "CC",
        "CCC",
        "CCCC",
        "CCCCC",
        "CCCCCC",
        "Cm",
        "CCm",
        "CCCm",
        "CCCCm",
        "CCCCCm",
        "CCCCCCm",

        // Minute (m): 1..2
        "m",
        "mm",
        "hm",
        "hmm",

        // Second (s): 1..2
        "s",
        "ss",
        "hms",
        "hmss",

        // Timezone (z): 1..5
        "z",
        "zz",
        "zzz",
        "zzzz",
        "zzzzz",
        "hmsz",
        "hmszz",
        "hmszzz",
        "hmszzzz",
        "hmszzzzz",

        // --- Other fields not explicitly mentioned but previously included ---
        "U",
        "UUUU",
        "Q",
        "QQ",
        "QQQ",
        "QQQQ",
        "QQQQQ",
        "yQ",
        "yQQQ",
        "yQQQQ",
        "B",
        "BBBB",
        "BBBBB",
        "Bh",
        "Bhh",
        "Bhm",
        "BBBBhm",
        "BBBBBhm",
        "v",
        "vvvv",
        "yMv",
        "yMMMMdv",
        "yMMMMEEEEdvvvv",
        "yMdHmsv",
        "yMMMMdhmsvvvv"
    };

    private static final String[] CALENDARS = {
        "gregorian", "buddhist", "japanese", "roc", "islamic", "chinese"
    };

    public static void main(String[] args) throws IOException {
        String filter = args.length > 0 ? args[0] : ".*";

        CLDRConfig config = CLDRConfig.getInstance();
        Factory factory = config.getCldrFactory();
        Set<String> locales = factory.getAvailableLanguages();

        String outputDir = CLDRPaths.CHART_DIRECTORY + "/verify/dates/";
        java.io.File dir = new java.io.File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filename = "dtpg_comparison.csv";

        System.out.println(
                "Generating comparison to " + outputDir + filename + " with filter " + filter);

        try (PrintWriter out = FileUtilities.openUTF8Writer(outputDir, filename)) {
            // Header
            out.println(
                    "Locale,Calendar,Skeleton,CldrDTPG Pattern,ICU4J DTPG Pattern,Match?,Trace");

            int count = 0;
            List<String> sortedLocales = new ArrayList<>(locales);
            Collections.sort(sortedLocales);

            for (String localeID : sortedLocales) {
                if (localeID.equals("root")) continue;
                if (!localeID.matches(filter)) continue;

                System.out.println("Processing locale: " + localeID);
                CLDRFile cldrFile = factory.make(localeID, true);

                boolean localeHasDiffs = false;
                List<String> rowsForThisLocale = new ArrayList<>();

                for (String calendar : CALENDARS) {
                    CldrDateTimePatternGenerator cldrGen =
                            new CldrDateTimePatternGenerator(cldrFile, calendar, false);

                    DateTimePatternGenerator icuGen = cldrGen.getIcu4jGenerator();

                    for (String skeleton : SKELETONS) {
                        List<String> trace = new ArrayList<>();
                        String cldrPattern = cldrGen.getBestPattern(skeleton, trace);
                        String icuPattern = normalizePattern(icuGen.getBestPattern(skeleton));

                        String matchResult = getDifferenceReason(cldrPattern, icuPattern);
                        if (!matchResult.equals("YES")) {
                            localeHasDiffs = true;
                            String traceStr = String.join(" | ", trace);
                            StringBuilder sb = new StringBuilder();
                            sb.append(escapeCsv(localeID)).append(",");
                            sb.append(escapeCsv(calendar)).append(",");
                            sb.append(escapeCsv(skeleton)).append(",");
                            sb.append(escapeCsv(cldrPattern)).append(",");
                            sb.append(escapeCsv(icuPattern)).append(",");
                            sb.append(escapeCsv(matchResult)).append(",");
                            sb.append(escapeCsv(traceStr));
                            rowsForThisLocale.add(sb.toString());
                        }
                    }
                }

                if (localeHasDiffs) {
                    for (String row : rowsForThisLocale) {
                        out.println(row);
                        count++;
                    }
                } else {
                    out.print(escapeCsv(localeID));
                    out.print(",");
                    out.print(escapeCsv("ALL"));
                    out.print(",");
                    out.print(escapeCsv("ALL"));
                    out.print(",");
                    out.print(escapeCsv(""));
                    out.print(",");
                    out.print(escapeCsv(""));
                    out.print(",");
                    out.print(escapeCsv("YES"));
                    out.print(",");
                    out.print(escapeCsv(""));
                    out.println();
                    count++;
                }
                out.flush();
            }
            System.out.println("Done! Generated " + count + " comparisons.");
        }
    }

    private static String getDifferenceReason(String cldr, String icu) {
        if (cldr.equals(icu)) return "YES";

        List<String> cldrFields = getFields(cldr);
        List<String> icuFields = getFields(icu);

        // 1. Check for Era mismatch
        boolean cldrHasG = cldrFields.stream().anyMatch(f -> f.startsWith("G"));
        boolean icuHasG = icuFields.stream().anyMatch(f -> f.startsWith("G"));
        if (cldrHasG != icuHasG) {
            return icuHasG ? "NO: Era added" : "NO: Era removed";
        }

        // 2. Check for Field Set mismatch (excluding Era)
        Set<Character> cldrFieldSet = new TreeSet<>();
        for (String f : cldrFields) if (!f.startsWith("G")) cldrFieldSet.add(f.charAt(0));
        Set<Character> icuFieldSet = new TreeSet<>();
        for (String f : icuFields) if (!f.startsWith("G")) icuFieldSet.add(f.charAt(0));

        if (!cldrFieldSet.equals(icuFieldSet)) {
            StringBuilder cldrOnly = new StringBuilder();
            for (char ch : cldrFieldSet) {
                if (!icuFieldSet.contains(ch)) cldrOnly.append(ch);
            }
            StringBuilder icuOnly = new StringBuilder();
            for (char ch : icuFieldSet) {
                if (!cldrFieldSet.contains(ch)) icuOnly.append(ch);
            }
            return "NO: Field set mismatch: " + cldrOnly + " -> " + icuOnly;
        }

        // 3. Check for Field Order mismatch
        List<Character> cldrOrder = new ArrayList<>();
        for (String f : cldrFields) cldrOrder.add(f.charAt(0));
        List<Character> icuOrder = new ArrayList<>();
        for (String f : icuFields) icuOrder.add(f.charAt(0));

        if (!cldrOrder.equals(icuOrder)) {
            return "NO: Field order mismatch";
        }

        // 4. Check for Field Length mismatch
        if (cldrFields.size() == icuFields.size()) {
            for (int i = 0; i < cldrFields.size(); i++) {
                String cF = cldrFields.get(i);
                String iF = icuFields.get(i);
                if (!cF.equals(iF)) {
                    return "NO: Field length mismatch: " + cF + " -> " + iF;
                }
            }
        }

        // 5. If everything else is the same, it must be literals or separators
        return "NO: Separator/Literal mismatch";
    }

    private static List<String> getFields(String pattern) {
        List<String> fields = new ArrayList<>();
        boolean inQuote = false;
        for (int i = 0; i < pattern.length(); ) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                inQuote = !inQuote;
                i++;
                continue;
            }
            if (inQuote) {
                i++;
                continue;
            }
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                int start = i;
                while (i < pattern.length() && pattern.charAt(i) == ch) {
                    i++;
                }
                fields.add(pattern.substring(start, i));
            } else {
                i++;
            }
        }
        return fields;
    }

    private static String normalizePattern(String pattern) {
        if (pattern == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < pattern.length(); ) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                sb.append(ch);
                inQuote = !inQuote;
                i++;
            } else if (inQuote) {
                sb.append(ch);
                i++;
            } else if (ch == 'E') {
                int count = 0;
                while (i < pattern.length() && pattern.charAt(i) == 'E') {
                    count++;
                    i++;
                }
                if (count <= 3) {
                    sb.append("E");
                } else {
                    for (int j = 0; j < count; j++) sb.append('E');
                }
            } else {
                sb.append(ch);
                i++;
            }
        }
        return sb.toString();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
