package org.unicode.cldr.unittest;

import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.unicode.cldr.tool.GenerateCurrencyFormatTestData;
import org.unicode.cldr.tool.GenerateCurrencyFormatTestData.Dimensions;
import org.unicode.cldr.util.CLDRPaths;

public class TestCurrencyFormat extends TestFmwkPlus {

    public static void main(String[] args) {
        new TestCurrencyFormat().run(args);
    }

    @org.junit.jupiter.api.BeforeEach
    public void initTestParams() {
        params = TestParams.create(new String[0], new java.io.PrintWriter(System.out));
    }

    @org.junit.jupiter.api.Test
    public void TestCurrenciesTsv() {
        runTsvTestFileName("core.tsv");
    }

    @org.junit.jupiter.api.Test
    public void TestCurrenciesModernCurrenciesTsv() {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            for (Dimensions.CurrencyFormatLength fl : Dimensions.CurrencyFormatLength.values()) {
                for (Dimensions.CurrencyFormatType ft : Dimensions.CurrencyFormatType.values()) {
                    if (fl == Dimensions.CurrencyFormatLength.SHORT
                            && ft == Dimensions.CurrencyFormatType.ACCOUNTING) {
                        continue;
                    }
                    runTsvTestFileName(getFileName(cd, fl, ft, "modern_currencies"));
                }
            }
        }
    }

    @org.junit.jupiter.api.Test
    public void TestCurrenciesModernLocalesTsv() {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            for (Dimensions.CurrencyFormatLength fl : Dimensions.CurrencyFormatLength.values()) {
                for (Dimensions.CurrencyFormatType ft : Dimensions.CurrencyFormatType.values()) {
                    if (fl == Dimensions.CurrencyFormatLength.SHORT
                            && ft == Dimensions.CurrencyFormatType.ACCOUNTING) {
                        continue;
                    }
                    runTsvTestFileName(getFileName(cd, fl, ft, "modern_locales"));
                }
            }
        }
    }

    @org.junit.jupiter.api.Test
    public void TestCurrenciesExtendedNumbersTsv() {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            for (Dimensions.CurrencyFormatLength fl : Dimensions.CurrencyFormatLength.values()) {
                for (Dimensions.CurrencyFormatType ft : Dimensions.CurrencyFormatType.values()) {
                    if (fl == Dimensions.CurrencyFormatLength.SHORT
                            && ft == Dimensions.CurrencyFormatType.ACCOUNTING) {
                        continue;
                    }
                    runTsvTestFileName(getFileName(cd, fl, ft, "extended_numbers"));
                }
            }
        }
    }

    private void runTsvTestFileName(String filename) {
        Path filePath = Path.of(CLDRPaths.TEST_DATA + "currency", filename);
        try {
            runTsvTest(filePath);
        } catch (IOException e) {
            errln("IOException running test for file " + filename + ": " + e.getMessage());
        }
    }

    private void runTsvTest(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString();
        if (!Files.exists(filePath)) {
            errln("Test data file not found: " + filePath);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Header
            if (line == null) {
                errln("Empty test data file: " + filePath);
                return;
            }

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length < 7) {
                    errln(filename + ":" + lineNum + " - Invalid line: " + line);
                    continue;
                }

                String localeStr = parts[0];
                String currencyStr = parts[1];
                String currencyFormatLengthStr = parts[2];
                String currencyFormatTypeStr = parts[3];
                String currencyDisplayStr = parts[4];
                double input = Double.parseDouble(parts[5]);
                String expected = parts[6];

                ULocale locale = new ULocale(localeStr);
                Dimensions.CurrencyFormatLength currencyFormatLength = null;
                for (Dimensions.CurrencyFormatLength fl :
                        Dimensions.CurrencyFormatLength.values()) {
                    if (fl.getLabel().equals(currencyFormatLengthStr)) {
                        currencyFormatLength = fl;
                        break;
                    }
                }
                Dimensions.CurrencyFormatType currencyFormatType = null;
                for (Dimensions.CurrencyFormatType ft : Dimensions.CurrencyFormatType.values()) {
                    if (ft.getLabel().equals(currencyFormatTypeStr)) {
                        currencyFormatType = ft;
                        break;
                    }
                }
                Dimensions.CurrencyDisplay currencyDisplay = null;
                for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
                    if (cd.getLabel().equals(currencyDisplayStr)) {
                        currencyDisplay = cd;
                        break;
                    }
                }

                String actual =
                        GenerateCurrencyFormatTestData.format(
                                locale,
                                currencyStr,
                                currencyFormatLength,
                                currencyFormatType,
                                currencyDisplay,
                                input);
                assertEquals(
                        filename
                                + ":"
                                + lineNum
                                + " - Failure for "
                                + localeStr
                                + " ("
                                + currencyStr
                                + ", "
                                + currencyFormatLengthStr
                                + ", "
                                + currencyFormatTypeStr
                                + ", "
                                + currencyDisplayStr
                                + ") with input "
                                + input,
                        expected,
                        actual);
            }
        }
    }

    private String getFileName(
            Dimensions.CurrencyDisplay cd,
            Dimensions.CurrencyFormatLength fl,
            Dimensions.CurrencyFormatType ft,
            String suite) {
        String displayLabel = cd.getLabel();
        if (displayLabel.equals("narrowSymbol")) {
            displayLabel = "narrow";
        }
        String lenSuffix =
                fl == Dimensions.CurrencyFormatLength.STANDARD ? "" : "_" + fl.getLabel();
        String typeSuffix = ft == Dimensions.CurrencyFormatType.STANDARD ? "" : "_acc";

        String suiteAbbr = suite;
        if (suite.equals("modern_currencies")) {
            suiteAbbr = "mod_cur";
        } else if (suite.equals("modern_locales")) {
            suiteAbbr = "mod_loc";
        } else if (suite.equals("extended_numbers")) {
            suiteAbbr = "ext_num";
        }

        return displayLabel + typeSuffix + lenSuffix + "_" + suiteAbbr + ".tsv";
    }
}
