package org.unicode.cldr.unittest;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
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

public class TestRBNF extends TestFmwkPlus {

    private static final String TEST_DATA_DIR = CLDRPaths.TEST_DATA + "rbnf/";
    private static final File RBNF_DIR = new File(CLDRPaths.RBNF_DIRECTORY);

    private static final Map<String, String> TYPE_TO_GROUPING = new HashMap<>();

    static {
        TYPE_TO_GROUPING.put("spell", "SpelloutRules");
        TYPE_TO_GROUPING.put("digits", "OrdinalRules");
        TYPE_TO_GROUPING.put("number", "NumberingSystemRules");
    }

    public void testConformance() {
        File testDir = new File(TEST_DATA_DIR);
        if (!testDir.exists() || !testDir.isDirectory()) {
            errln("Test data directory not found: " + TEST_DATA_DIR);
            return;
        }
        String[] ssvFiles = testDir.list((dir, name) -> name.endsWith(".ssv"));
        if (ssvFiles == null || ssvFiles.length == 0) {
            errln("No .ssv test files found in " + TEST_DATA_DIR);
            return;
        }
        for (String ssvFile : ssvFiles) {
            checkLocaleFile(ssvFile);
        }
    }

    private void checkLocaleFile(String ssvFile) {
        String localeId = ssvFile.substring(0, ssvFile.length() - 4);
        File rbnfXml = new File(RBNF_DIR, localeId + ".xml");
        if (!rbnfXml.exists()) {
            errln("RBNF XML file not found for locale " + localeId + ": " + rbnfXml);
            return;
        }

        Map<String, String> groupingRules = extractRules(rbnfXml);
        if (groupingRules == null) {
            return;
        }

        ULocale locale = new ULocale(localeId);
        Map<String, RuleBasedNumberFormat> formatters = new HashMap<>();
        for (Map.Entry<String, String> entry : groupingRules.entrySet()) {
            String rules = entry.getValue().trim();
            if (!rules.isEmpty()) {
                try {
                    formatters.put(entry.getKey(), new RuleBasedNumberFormat(rules, locale));
                } catch (Exception e) {
                    errln(
                            localeId
                                    + ": Failed to create RuleBasedNumberFormat for "
                                    + entry.getKey()
                                    + ": "
                                    + e.getMessage());
                }
            }
        }

        File ssvPath = new File(TEST_DATA_DIR + ssvFile);
        int lineNum = 0;
        try (BufferedReader reader = Files.newBufferedReader(ssvPath.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split(";", 4);
                if (fields.length < 4) {
                    errln(ssvFile + ":" + lineNum + ": Expected 4 fields, got " + fields.length);
                    continue;
                }
                String type = fields[0].trim();
                String ruleName = fields[1].trim();
                String numberStr = fields[2].trim();
                String expected = fields[3].trim();

                String grouping = TYPE_TO_GROUPING.get(type);
                if (grouping == null) {
                    if (type.isEmpty()) {
                        warnln(ssvFile + ":" + lineNum + ": Disabled test: " + line);
                    } else {
                        errln(ssvFile + ":" + lineNum + ": Unknown type: " + type);
                    }
                    continue;
                }

                RuleBasedNumberFormat rbnf = formatters.get(grouping);
                if (rbnf == null) {
                    errln(
                            ssvFile
                                    + ":"
                                    + lineNum
                                    + ": No "
                                    + grouping
                                    + " rules found in "
                                    + localeId
                                    + ".xml");
                    continue;
                }

                Number number;
                if (numberStr.isEmpty()) {
                    errln(ssvFile + ":" + lineNum + ": can not parse number: " + numberStr);
                    continue;
                }
                if (numberStr.contains(".") || numberStr.equals("Inf") || numberStr.equals("NaN")) {
                    number = Double.parseDouble(numberStr);
                } else {
                    number = Long.parseLong(numberStr);
                }
                String actual;
                try {
                    if (ruleName.isEmpty()) {
                        if (number instanceof Double) {
                            actual = rbnf.format(number.doubleValue());
                        } else {
                            actual = rbnf.format(number.longValue());
                        }
                    } else {
                        if (number instanceof Double) {
                            actual = rbnf.format(number.doubleValue(), ruleName);
                        } else {
                            actual = rbnf.format(number.longValue(), ruleName);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    errln(
                            ssvFile
                                    + ":"
                                    + lineNum
                                    + " format "
                                    + numberStr
                                    + " ["
                                    + ruleName
                                    + "]: "
                                    + e.getMessage());
                    continue;
                }

                assertEquals(
                        ssvFile + ":" + lineNum + " format " + numberStr + " [" + ruleName + "]",
                        expected,
                        actual);

                try {
                    if (!ruleName.isEmpty()) {
                        rbnf.setDefaultRuleSet(ruleName);
                    }
                    Number parsed = rbnf.parse(expected);
                    assertEquals(
                            ssvFile
                                    + ":"
                                    + lineNum
                                    + " parse \""
                                    + expected
                                    + "\" ["
                                    + ruleName
                                    + "]",
                            number,
                            parsed);
                } catch (ParseException e) {
                    errln(
                            ssvFile
                                    + ":"
                                    + lineNum
                                    + " parse failed for \""
                                    + expected
                                    + "\" ["
                                    + ruleName
                                    + "]: "
                                    + e.getMessage());
                }
            }
        } catch (IOException e) {
            errln("Error reading " + ssvFile + ": " + e.getMessage());
        }
    }

    public void testRoundtrip() {
        if (!RBNF_DIR.exists() || !RBNF_DIR.isDirectory()) {
            errln("RBNF directory not found: " + RBNF_DIR);
            return;
        }
        String[] xmlFiles = RBNF_DIR.list((dir, name) -> name.endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) {
            errln("No .xml files found in " + RBNF_DIR);
            return;
        }
        for (String xmlFile : xmlFiles) {
            checkRoundtrip(xmlFile);
        }
    }

    private static final Set<String> KNOWN_BROKEN_LOCALES =
            new TreeSet<>(Arrays.asList("ga", "lt"));
    private static final Set<String> ALIASES = new TreeSet<>(Arrays.asList("nb", "en_001"));

    private static final Number[] ROUNDTRIP_LONG_VALUES = {
        -1L, 0L, 1L, 2L, 3L, 10L, 99L, 100L, 101L, 999L, 1000L, 1001L, 1999L, 2000L, 2001L, 2100L,
        2200L, 10000L, 20000L, 100000L, 200000L, 1000000L, 2000000L,
    };

    private static final Map<String, Number[]> ROUNDTRIP_VALUES = new HashMap<>();

    static {
        ROUNDTRIP_VALUES.put(
                "SpelloutRules",
                new Number[] {
                    -1L,
                    0L,
                    0.2,
                    1L,
                    1.1,
                    2L,
                    3L,
                    10L,
                    99L,
                    100L,
                    101L,
                    999L,
                    1000L,
                    1001L,
                    1999L,
                    2000L,
                    2001L,
                    2100L,
                    2200L,
                    10000L,
                    20000L,
                    100000L,
                    200000L,
                    1000000L,
                    2000000L,
                    Double.POSITIVE_INFINITY,
                    Double.NaN
                });
        ROUNDTRIP_VALUES.put("OrdinalRules", ROUNDTRIP_LONG_VALUES);
        ROUNDTRIP_VALUES.put("NumberingSystemRules", ROUNDTRIP_LONG_VALUES);
    }

    private void checkRoundtrip(String xmlFile) {
        String localeId = xmlFile.substring(0, xmlFile.length() - 4);
        if (KNOWN_BROKEN_LOCALES.contains(localeId)) {
            warnln("Skipping known broken locale: " + localeId);
            return;
        }
        ULocale locale = new ULocale(localeId);
        File rbnfXml = new File(RBNF_DIR, xmlFile);
        String filePath = rbnfXml.getAbsolutePath();

        // Validate identity elements using XMLFileReader
        List<Pair<String, String>> data = new ArrayList<>();
        XMLFileReader.loadPathValues(filePath, data, false);
        for (Pair<String, String> pathValue : data) {
            String path = pathValue.getFirst();
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (parts.size() >= 3 && "identity".equals(parts.getElement(1))) {
                String element = parts.getElement(2);
                String typeAttr = parts.getAttributeValue(2, "type");
                if ("language".equals(element)) {
                    if (locale.getLanguage().isEmpty()) {
                        // Special case for the root locale.
                        typeAttr = "";
                    }
                    assertEquals(xmlFile + " language", locale.getLanguage(), typeAttr);
                } else if ("script".equals(element)) {
                    assertEquals(xmlFile + " script", locale.getScript(), typeAttr);
                } else if ("territory".equals(element)) {
                    assertEquals(xmlFile + " territory", locale.getCountry(), typeAttr);
                }
            }
        }

        // Extract rules and create formatters
        Map<String, String> groupingRules = extractRules(rbnfXml);
        if (groupingRules == null) {
            return;
        }

        for (Map.Entry<String, String> entry : groupingRules.entrySet()) {
            String grouping = entry.getKey();
            String rules = entry.getValue().trim();
            if (rules.isEmpty()) {
                continue;
            }
            RuleBasedNumberFormat rbnf;
            try {
                rbnf = new RuleBasedNumberFormat(rules, locale);
            } catch (Exception e) {
                errln(
                        localeId
                                + " "
                                + grouping
                                + ": Failed to create RuleBasedNumberFormat: "
                                + e.getMessage());
                continue;
            }

            Number[] values = ROUNDTRIP_VALUES.get(grouping);
            if (values == null) {
                continue;
            }

            for (String ruleSetName : rbnf.getRuleSetNames()) {
                rbnf.setDefaultRuleSet(ruleSetName);
                for (Number value : values) {
                    String formatted;
                    if (value instanceof Long) {
                        formatted = rbnf.format(value.longValue(), ruleSetName);
                    } else {
                        formatted = rbnf.format(value.doubleValue(), ruleSetName);
                    }
                    try {
                        Number parsed = rbnf.parse(formatted);
                        assertEquals(
                                localeId
                                        + " "
                                        + ruleSetName
                                        + " roundtrip "
                                        + value
                                        + " → \""
                                        + formatted
                                        + "\"",
                                value.doubleValue(),
                                parsed.doubleValue());
                    } catch (ParseException e) {
                        errln(
                                localeId
                                        + " "
                                        + ruleSetName
                                        + " parse failed for "
                                        + value
                                        + " → \""
                                        + formatted
                                        + "\": "
                                        + e.getMessage());
                    }
                }
            }
        }
    }

    private Map<String, String> extractRules(File xmlFile) {
        Map<String, String> result = extractRulesWithoutWarning(xmlFile);
        if (result == null) {
            String filename = xmlFile.getName();
            String localeId = filename.substring(0, filename.length() - 4);
            if (!ALIASES.contains(localeId)) {
                warnln("No rulesetGrouping found in " + xmlFile);
            }
        }
        return result;
    }

    private static Map<String, String> extractRulesWithoutWarning(File xmlFile) {
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

    private static final Map<String, String> GROUPING_TO_TYPE = new HashMap<>();

    static {
        for (Map.Entry<String, String> entry : TYPE_TO_GROUPING.entrySet()) {
            GROUPING_TO_TYPE.put(entry.getValue(), entry.getKey());
        }
    }

    // Candidate numbers for cardinal/numbering rulesets
    private static final Number[] CARDINAL_CANDIDATES = {
        -1L, 0L, 0.5, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L,
        18L, 19L, 20L, 21L, 25L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 99L, 100L, 101L, 200L, 300L,
        500L, 1000L, 1001L, 2000L, 5000L, 10000L, 100000L, 1000000L, 2000000L
    };

    private static final Number[] ORDINAL_CANDIDATES = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};

    private static final Number[] YEAR_CANDIDATES = {1999L, 2000L, 2001L};

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
            Map<String, String> groupingRules = extractRulesWithoutWarning(rbnfXml);
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

    private static Number[] getCandidates(String ruleSetName) {
        if (ruleSetName.contains("ordinal")) {
            return ORDINAL_CANDIDATES;
        }
        if (ruleSetName.contains("year")) {
            return YEAR_CANDIDATES;
        }
        return CARDINAL_CANDIDATES;
    }

    private static List<String> generateTestLines(
            RuleBasedNumberFormat rbnf, String ruleSetName, Number[] candidates, String type) {
        List<String> result = new ArrayList<>();
        for (Number n : candidates) {
            String formatted = formatNumber(rbnf, ruleSetName, n);
            result.add(toSsvLine(type, ruleSetName, n, formatted));
        }

        return result;
    }

    private static String formatNumber(RuleBasedNumberFormat rbnf, String ruleSetName, Number n) {
        if (n instanceof Double) {
            return rbnf.format(n.doubleValue(), ruleSetName);
        } else {
            return rbnf.format(n.longValue(), ruleSetName);
        }
    }

    private static String toSsvLine(
            String type, String ruleSetName, Number number, String formatted) {
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

    public static void main(String[] args) {
        generateData();
        var test = new TestRBNF();
        test.params = TestParams.create("", new PrintWriter(System.out));
        test.testConformance();
    }
}
