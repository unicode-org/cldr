package org.unicode.cldr.tool;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

public class GenerateRBNFTestData {

    private static final String TEST_DATA_DIR = CLDRPaths.TEST_DATA + "rbnf/";
    private static final File RBNF_DIR = new File(CLDRPaths.RBNF_DIRECTORY);

    private static final Map<String, String> TYPE_TO_GROUPING = new HashMap<>();

    static {
        TYPE_TO_GROUPING.put("spell", "SpelloutRules");
        TYPE_TO_GROUPING.put("digits", "OrdinalRules");
        TYPE_TO_GROUPING.put("number", "NumberingSystemRules");
    }

    static final Map<String, String> GROUPING_TO_TYPE = new HashMap<>();

    static {
        for (Map.Entry<String, String> entry : TYPE_TO_GROUPING.entrySet()) {
            GROUPING_TO_TYPE.put(entry.getValue(), entry.getKey());
        }
    }

    private static final Set<String> KNOWN_BROKEN_LOCALES =
            new TreeSet<>(Arrays.asList("ga", "lt"));
    private static final Set<String> ALIASES = new TreeSet<>(Arrays.asList("nb", "en_001"));

    // Candidate numbers for cardinal/numbering rulesets
    static final Number[] CARDINAL_CANDIDATES = {
        -1L, 0L, 0.5, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L,
        17L, 18L, 19L, 20L, 21L, 25L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 99L, 100L, 101L, 200L,
        300L, 500L, 1000L, 1001L, 2000L, 5000L, 10000L, 100000L, 1000000L, 2000000L
    };

    static final Number[] ORDINAL_CANDIDATES = {
        1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L
    };

    static final Number[] YEAR_CANDIDATES = {1999L, 2000L, 2001L};

    static String toSsvLine(String type, String ruleSetName, Number number, String formatted) {
        String numStr;
        if (number instanceof Double) {
            double d = number.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                numStr = String.valueOf((long) d);
            } else {
                numStr = String.valueOf(d);
            }
        } else {
            numStr = String.valueOf(number.longValue());
        }
        return type + ";" + ruleSetName + ";" + numStr + ";" + formatted;
    }

    static String formatNumber(RuleBasedNumberFormat rbnf, String ruleSetName, Number n) {
        if (n instanceof Double) {
            return rbnf.format(n.doubleValue(), ruleSetName);
        } else {
            return rbnf.format(n.longValue(), ruleSetName);
        }
    }

    static List<String> generateTestLines(
            RuleBasedNumberFormat rbnf, String ruleSetName, Number[] candidates, String type) {
        List<String> result = new ArrayList<>();
        boolean omitName = ruleSetName.equals(rbnf.getDefaultRuleSetName());
        for (Number n : candidates) {
            String formatted = formatNumber(rbnf, ruleSetName, n);
            result.add(toSsvLine(type, omitName ? "" : ruleSetName, n, formatted));
        }
        return result;
    }

    static Number[] getCandidates(String ruleSetName) {
        if (ruleSetName.contains("ordinal")) {
            return ORDINAL_CANDIDATES;
        }
        if (ruleSetName.contains("year")) {
            return YEAR_CANDIDATES;
        }
        return CARDINAL_CANDIDATES;
    }

    /**
     * Generates .ssv test data files for RBNF locales that don't already have one. Run this method
     * after adding new RBNF rulesets.
     */
    static void generateData() {
        String[] xmlFiles = RBNF_DIR.list((dir, name) -> name.endsWith(".xml"));
        if (xmlFiles == null) {
            return;
        }
        Arrays.sort(xmlFiles);

        for (String xmlFile : xmlFiles) {
            String localeId = xmlFile.substring(0, xmlFile.length() - 4);
            if (KNOWN_BROKEN_LOCALES.contains(localeId)) {
                continue;
            }
            if (ALIASES.contains(localeId)) {
                continue;
            }

            File outputFile = new File(TEST_DATA_DIR + localeId + ".ssv");
            if (outputFile.exists()) {
                continue;
            }

            File rbnfXml = new File(RBNF_DIR, xmlFile);
            Map<String, String> groupingRules = extractRules(rbnfXml);
            if (groupingRules == null || groupingRules.isEmpty()) {
                continue;
            }

            ULocale locale = new ULocale(localeId);
            List<String> lines = new ArrayList<>();

            for (Map.Entry<String, String> groupEntry : groupingRules.entrySet()) {
                String grouping = groupEntry.getKey();
                String rules = groupEntry.getValue().trim();
                if (rules.isEmpty()) continue;

                String type = GROUPING_TO_TYPE.get(grouping);
                if (type == null) {
                    continue;
                }

                RuleBasedNumberFormat rbnf;
                try {
                    rbnf = new RuleBasedNumberFormat(rules, locale);
                } catch (Exception e) {
                    System.err.println(
                            localeId
                                    + " "
                                    + grouping
                                    + ": Failed to create RBNF: "
                                    + e.getMessage());
                    continue;
                }

                for (String ruleSetName : rbnf.getRuleSetNames()) {
                    if (ruleSetName.startsWith("%%")) {
                        continue;
                    }

                    Number[] candidates = getCandidates(ruleSetName);
                    List<String> testLines =
                            generateTestLines(rbnf, ruleSetName, candidates, type);
                    lines.addAll(testLines);
                }
            }

            if (lines.isEmpty()) {
                continue;
            }

            try (PrintWriter pw = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
                pw.println("#");
                pw.println("# Copyright © 2026-2026 Unicode, Inc.");
                pw.println(
                        "# For terms of use, see https://www.unicode.org/terms_of_use.html");
                pw.println("#");
                pw.println("# Format: type;rule name;number;expected result");
                pw.println(
                        "# type: spell=SpelloutRules, digits=OrdinalRules, number=NumberingSystemRules");
                pw.println("# rule name: empty for the default ruleset");
                pw.println("#");
                for (String line : lines) {
                    pw.println(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println(
                    "Generated " + outputFile.getName() + " (" + lines.size() + " tests)");
        }
    }

    public static Map<String, String> extractRules(File xmlFile) {
        String filePath = xmlFile.getAbsolutePath();
        List<Pair<String, String>> data = new ArrayList<>();
        XMLFileReader.loadPathValues(filePath, data, false);
        Map<String, String> rules = new HashMap<>();
        for (Pair<String, String> pathValue : data) {
            String path = pathValue.getFirst();
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (parts.size() >= 3 && "rulesetGrouping".equals(parts.getElement(2))) {
                String groupingType = parts.getAttributeValue(2, "type");
                String value = pathValue.getSecond();
                if (groupingType != null && value != null) {
                    rules.put(groupingType, value);
                }
            }
        }
        if (rules.isEmpty()) {
            return null;
        }
        return rules;
    }

    public static void main(String[] args) {
        generateData();
    }
}
