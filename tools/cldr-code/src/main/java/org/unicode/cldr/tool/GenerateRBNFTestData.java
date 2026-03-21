package org.unicode.cldr.tool;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

@CLDRTool(alias = "generate-rbnf-ssv", description = "Generate RBNF semicolon test files")
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

    private static final Set<String> ALIASES = new TreeSet<>(Arrays.asList("nb", "en_001"));

    static final TreeSet<Number> YEAR_CANDIDATES =
            new TreeSet<>(Arrays.asList(1999L, 2000L, 2001L));
    static final TreeSet<Number> ORDINAL_CANDIDATES =
            new TreeSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
    static final TreeSet<Number> CARDINAL_CANDIDATES =
            new TreeSet<>(Comparator.comparingDouble(Number::doubleValue));

    static {
        CARDINAL_CANDIDATES.addAll(
                Arrays.asList(
                        -1L, 0L, 0.5, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L,
                        15L, 16L, 17L, 18L, 19L, 20L, 21L, 25L, 30L, 40L, 50L, 60L, 70L, 80L, 90L,
                        99L, 100L, 101L, 200L, 300L, 500L, 1000L, 1001L, 2000L, 5000L, 10000L,
                        100000L, 1000000L, 2000000L));
    }

    /*
    This is 2^53 - 1, which is the highest value that can roundtrip between a double and a long.
    While this could go to LONG.MAX_VALUE, accurate roundtrip parsing is not currently possible with ICU at this time in 2026.
    */
    static final long MAXIMUM_TEST_VALUE = 9007199254740991L;

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
            RuleBasedNumberFormat rbnf,
            String ruleSetName,
            TreeSet<Number> candidates,
            String type) {
        List<String> result = new ArrayList<>();
        boolean omitName =
                ruleSetName.equals(rbnf.getDefaultRuleSetName())
                        && "NumberingSystemRules".equals(type);
        for (Number n : candidates) {
            String formatted = formatNumber(rbnf, ruleSetName, n);
            result.add(toSsvLine(type, omitName ? "" : ruleSetName, n, formatted));
        }
        return result;
    }

    static TreeSet<Number> getCandidates(String rules, String ruleSetName) {
        TreeSet<Number> candidates = new TreeSet<>(Comparator.comparingDouble(Number::doubleValue));
        boolean isCardinal = false;

        if (ruleSetName.contains("ordinal")) {
            candidates.addAll(ORDINAL_CANDIDATES);
        } else if (ruleSetName.contains("year")) {
            candidates.addAll(YEAR_CANDIDATES);
        } else {
            candidates.addAll(CARDINAL_CANDIDATES);
            isCardinal = true;
        }

        // Find the start of this ruleset
        String header = ruleSetName + ":";
        int start = rules.indexOf(header);
        if (start < 0) {
            return candidates;
        }
        start += header.length();

        // Find the end (next ruleset or end of string)
        int end = rules.length();
        int nextRuleset = rules.indexOf("\n%", start);
        if (nextRuleset >= 0) {
            end = nextRuleset;
        }

        String rulesetText = rules.substring(start, end);

        // Split by ';' to get individual rules
        for (String rule : rulesetText.split(";")) {
            rule = rule.trim();
            if (rule.isEmpty()) {
                continue;
            }

            int colonIdx = rule.indexOf(':');
            if (colonIdx < 0) {
                continue;
            }

            String baseStr = rule.substring(0, colonIdx).trim();

            // Handle special rules
            if (baseStr.equals("-x")) {
                if (isCardinal) {
                    candidates.add(-1L);
                }
                // else not interesting from a conformance perspective.
                continue;
            }
            if (baseStr.endsWith("x")) {
                if (isCardinal) {
                    candidates.add(0.5);
                    candidates.add(1.5);
                }
                // else not interesting from a conformance perspective.
                continue;
            }
            if (baseStr.equals("Inf")) {
                candidates.add(Double.POSITIVE_INFINITY);
                continue;
            }
            if (baseStr.equals("NaN")) {
                candidates.add(Double.NaN);
                continue;
            }

            // Strip /divisor suffix (e.g. "1010/100" -> "1010")
            int slashIdx = baseStr.indexOf('/');
            if (slashIdx >= 0) {
                baseStr = baseStr.substring(0, slashIdx);
            }

            long baseValue = Long.parseLong(baseStr);
            if (baseValue < MAXIMUM_TEST_VALUE) {
                if (isCardinal || baseValue >= 2) {
                    candidates.add(baseValue - 1);
                }
                // else this type typically doesn't work less than 1.
                if (isCardinal || baseValue >= 1) {
                    candidates.add(baseValue);
                }
                // else this type typically doesn't work less than 1.
                candidates.add(baseValue + 1);
            } else {
                // This base value is not feasible to roundtrip the formatting and parsing with ICU
                // at this time.
                // It can be formatted, but not parsed correctly.
                // Let's try the largest maximum value that can be roundtripped instead.
                candidates.add(MAXIMUM_TEST_VALUE);
            }
        }

        return candidates;
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

                    TreeSet<Number> candidates = getCandidates(rules, ruleSetName);
                    List<String> testLines = generateTestLines(rbnf, ruleSetName, candidates, type);
                    lines.addAll(testLines);
                }
            }

            if (lines.isEmpty()) {
                continue;
            }

            try (PrintWriter pw = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
                pw.println("#");
                pw.println("# Copyright © 2026-2026 Unicode, Inc.");
                pw.println("# For terms of use, see https://www.unicode.org/terms_of_use.html");
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
