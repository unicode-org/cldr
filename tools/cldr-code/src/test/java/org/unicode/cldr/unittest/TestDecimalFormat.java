package org.unicode.cldr.unittest;

import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.unicode.cldr.tool.GenerateDecimalFormatTestData;
import org.unicode.cldr.tool.GenerateDecimalFormatTestData.Dimensions;
import org.unicode.cldr.util.CLDRPaths;

public class TestDecimalFormat extends TestFmwkPlus {

    public static void main(String[] args) {
        new TestDecimalFormat().run(args);
    }

    public void TestDecimalsTsv() {
        try {
            runTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "decimals.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestDecimalsModernLocalesTsv() {
        try {
            runTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "decimals_modern_locales.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestDecimalsAllNumbersTsv() {
        try {
            runTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "decimals_all_numbers.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCurrenciesTsv() {
        try {
            runCurrencyTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "currencies.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCurrenciesModernLocalesTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(CLDRPaths.TEST_DATA + "decimal", "currencies_modern_locales.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCurrenciesModernCurrenciesTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(CLDRPaths.TEST_DATA + "decimal", "currencies_modern_currencies.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCurrenciesAllNumbersTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(CLDRPaths.TEST_DATA + "decimal", "currencies_all_numbers.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCurrenciesAllNumbersShortTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(CLDRPaths.TEST_DATA + "decimal", "currencies_all_numbers_short.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCurrenciesAllNumbersLongTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(CLDRPaths.TEST_DATA + "decimal", "currencies_all_numbers_long.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCompactCurrenciesTsv() {
        try {
            runCurrencyTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "compact_currencies.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCompactCurrenciesModernLocalesTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(
                            CLDRPaths.TEST_DATA + "decimal",
                            "compact_currencies_modern_locales.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCompactCurrenciesModernCurrenciesShortTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(
                            CLDRPaths.TEST_DATA + "decimal",
                            "compact_currencies_modern_currencies_short.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCompactCurrenciesModernCurrenciesLongTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(
                            CLDRPaths.TEST_DATA + "decimal",
                            "compact_currencies_modern_currencies_long.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCompactCurrenciesAllNumbersShortTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(
                            CLDRPaths.TEST_DATA + "decimal",
                            "compact_currencies_all_numbers_short.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestCompactCurrenciesAllNumbersLongTsv() {
        try {
            runCurrencyTsvTest(
                    Path.of(
                            CLDRPaths.TEST_DATA + "decimal",
                            "compact_currencies_all_numbers_long.tsv"));
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
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
                if (parts.length < 5) {
                    errln(filename + ":" + lineNum + " - Invalid line: " + line);
                    continue;
                }

                String localeStr = parts[0];
                String numberFormatStr = parts[1];
                String formatLengthStr = parts[2];
                double input = Double.parseDouble(parts[3]);
                String expected = parts[4];

                ULocale locale = new ULocale(localeStr);
                Dimensions.NumberFormat format =
                        Dimensions.NumberFormat.valueOf(
                                numberFormatStr.toUpperCase().replace('-', '_'));
                Dimensions.FormatLength length =
                        formatLengthStr.isEmpty()
                                ? Dimensions.FormatLength.EMPTY
                                : Dimensions.FormatLength.valueOf(formatLengthStr.toUpperCase());

                String actual = GenerateDecimalFormatTestData.format(locale, format, length, input);
                assertEquals(
                        filename
                                + ":"
                                + lineNum
                                + " - Failure for "
                                + localeStr
                                + " ("
                                + numberFormatStr
                                + ", "
                                + formatLengthStr
                                + ") with input "
                                + input,
                        expected,
                        actual);
            }
        }
    }

    private void runCurrencyTsvTest(Path filePath) throws IOException {
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
                String numberFormatStr = parts[1];
                String formatLengthStr = parts[2];
                String currencyStr = parts[3];
                double input = Double.parseDouble(parts[4]);
                String expected = parts[5];

                ULocale locale = new ULocale(localeStr);
                Dimensions.NumberFormat format =
                        Dimensions.NumberFormat.valueOf(
                                numberFormatStr.toUpperCase().replace('-', '_'));
                Dimensions.FormatLength length =
                        formatLengthStr.isEmpty()
                                ? Dimensions.FormatLength.EMPTY
                                : Dimensions.FormatLength.valueOf(formatLengthStr.toUpperCase());

                String actual =
                        GenerateDecimalFormatTestData.format(
                                locale, format, length, currencyStr, input);
                assertEquals(
                        filename
                                + ":"
                                + lineNum
                                + " - Failure for "
                                + localeStr
                                + " ("
                                + numberFormatStr
                                + ", "
                                + formatLengthStr
                                + ", "
                                + currencyStr
                                + ") with input "
                                + input,
                        expected,
                        actual);
            }
        }
    }
}
