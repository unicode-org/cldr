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
import org.unicode.cldr.tool.GenerateDecimalFormatTestData;
import org.unicode.cldr.tool.GenerateDecimalFormatTestData.Dimensions;
import org.unicode.cldr.util.CLDRPaths;

public class TestDecimalFormat {

    @Test
    public void TestDecimalsTsv() throws IOException {
        runTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "decimals.tsv"));
    }

    @Test
    public void TestDecimalsModernLocalesTsv() throws IOException {
        runTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "decimals_modern_locales.tsv"));
    }

    @Test
    public void TestDecimalsExtendedNumbersTsv() throws IOException {
        runTsvTest(Path.of(CLDRPaths.TEST_DATA + "decimal", "decimals_extended_numbers.tsv"));
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
                if (parts.length < 5) {
                    fail(filename + ":" + lineNum + " - Invalid line: " + line);
                }

                String localeStr = parts[0];
                String numberFormatStr = parts[1];
                String formatLengthStr = parts[2];
                double input = Double.parseDouble(parts[3]);
                String expected = parts[4];

                ULocale locale = new ULocale(localeStr);
                Dimensions.NumberFormat format =
                        Dimensions.NumberFormat.valueOf(numberFormatStr.toUpperCase());
                Dimensions.FormatLength length =
                        formatLengthStr.isEmpty()
                                ? Dimensions.FormatLength.EMPTY
                                : Dimensions.FormatLength.valueOf(formatLengthStr.toUpperCase());

                String actual = GenerateDecimalFormatTestData.format(locale, format, length, input);
                assertEquals(
                        expected,
                        actual,
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
                                + input);
            }
        }
    }
}
