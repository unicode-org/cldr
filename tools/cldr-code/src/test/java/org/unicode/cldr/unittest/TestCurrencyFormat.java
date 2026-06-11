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
        runTsvTestFileName("currencies.tsv");
    }

    @org.junit.jupiter.api.Test
    public void TestCurrenciesModernCurrenciesTsv() {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            for (Dimensions.CurrencyFormatLength fl : Dimensions.CurrencyFormatLength.values()) {
                runTsvTestFileName(getFileName(cd, fl, "modern_currencies"));
            }
        }
    }

    @org.junit.jupiter.api.Test
    public void TestCurrenciesModernLocalesTsv() {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            for (Dimensions.CurrencyFormatLength fl : Dimensions.CurrencyFormatLength.values()) {
                runTsvTestFileName(getFileName(cd, fl, "modern_locales"));
            }
        }
    }

    @org.junit.jupiter.api.Test
    public void TestCurrenciesExtendedNumbersTsv() {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            for (Dimensions.CurrencyFormatLength fl : Dimensions.CurrencyFormatLength.values()) {
                runTsvTestFileName(getFileName(cd, fl, "extended_numbers"));
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
                if (parts.length < 6) {
                    errln(filename + ":" + lineNum + " - Invalid line: " + line);
                    continue;
                }

                String localeStr = parts[0];
                String currencyStr = parts[1];
                String currencyFormatLengthStr = parts[2];
                String currencyDisplayStr = parts[3];
                double input = Double.parseDouble(parts[4]);
                String expected = parts[5];

                ULocale locale = new ULocale(localeStr);
                Dimensions.CurrencyFormatLength currencyFormatLength = null;
                for (Dimensions.CurrencyFormatLength fl :
                        Dimensions.CurrencyFormatLength.values()) {
                    if (fl.getLabel().equals(currencyFormatLengthStr)) {
                        currencyFormatLength = fl;
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
                                locale, currencyStr, currencyFormatLength, currencyDisplay, input);
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
                                + currencyDisplayStr
                                + ") with input "
                                + input,
                        expected,
                        actual);
            }
        }
    }

    private String getFileName(
            Dimensions.CurrencyDisplay cd, Dimensions.CurrencyFormatLength fl, String suite) {
        String lenSuffix =
                fl == Dimensions.CurrencyFormatLength.STANDARD ? "" : "_" + fl.getLabel();
        return "currencies_" + cd.getLabel() + lenSuffix + "_" + suite + ".tsv";
    }
}
