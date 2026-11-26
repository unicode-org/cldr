package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.tool.GenerateCurrencyFormatTestData;
import org.unicode.cldr.tool.GenerateCurrencyFormatTestData.Dimensions;
import org.unicode.cldr.util.CLDRPaths;

public class TestCurrencyFormat {

    @Test
    public void TestCurrenciesTsv() throws IOException {
        runTsvTestFileName("currencies.tsv");
    }

    @Test
    public void TestCurrenciesModernCurrenciesTsv() throws IOException {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            if (cd == Dimensions.CurrencyDisplay.NO_CURRENCY) {
                continue; // Exclude NO_CURRENCY from extended suites
            }
            String displayLabel = cd.getLabel();
            if (displayLabel.equals("symbolNarrow")) {
                displayLabel = "narrow";
            }
            runTsvTestFileName("currencies_" + displayLabel + "_modern_currencies.tsv");
        }
    }

    @Test
    public void TestCurrenciesModernLocalesTsv() throws IOException {
        runTsvTestFileName("currencies_modern_locales.tsv");
    }

    @Test
    public void TestCurrenciesExtendedNumbersTsv() throws IOException {
        for (Dimensions.CurrencyDisplay cd : Dimensions.CurrencyDisplay.values()) {
            if (cd == Dimensions.CurrencyDisplay.NO_CURRENCY) {
                continue; // Exclude NO_CURRENCY from extended suites
            }
            String displayLabel = cd.getLabel();
            if (displayLabel.equals("symbolNarrow")) {
                displayLabel = "narrow";
            }
            runTsvTestFileName("currencies_" + displayLabel + "_extended_numbers.tsv");
        }
    }

    private void runTsvTestFileName(String filename) throws IOException {
        runTsvTest(Path.of(CLDRPaths.TEST_DATA + "currency", filename));
    }

    private void runTsvTest(Path filePath) throws IOException {
        String filename = filePath.getFileName().toString();
        if (!Files.exists(filePath)) {
            fail("Test data file not found: " + filePath);
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Header
            if (line == null) {
                fail("Empty test data file: " + filePath);
            }

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length < 7) {
                    fail(filename + ":" + lineNum + " - Invalid line: " + line);
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

                if (currencyFormatLength == null
                        || currencyFormatType == null
                        || currencyDisplay == null) {
                    fail(
                            filename
                                    + ":"
                                    + lineNum
                                    + " - Unrecognized dimension label in line: "
                                    + line);
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
                        expected,
                        actual,
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
                                + input);
            }
        }
    }
}
