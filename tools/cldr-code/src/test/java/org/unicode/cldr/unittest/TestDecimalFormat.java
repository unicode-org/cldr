package org.unicode.cldr.unittest;

import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.ULocale;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.unicode.cldr.util.CLDRPaths;

public class TestDecimalFormat extends TestFmwkPlus {

    public static void main(String[] args) {
        new TestDecimalFormat().run(args);
    }

    public void TestDecimaltsv() {
        try {
            runTsvTest("decimal.tsv");
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestDecimalAllLocales() {
        try {
            runTsvTest("decimal_all_locales.tsv");
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestDecimalAllNumbers() {
        try {
            runTsvTest("decimal_all_numbers.tsv");
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    public void TestDecimalRandom5Percent() {
        try {
            runTsvTest("decimal_random_5percent.tsv");
        } catch (IOException e) {
            errln("IOException: " + e.getMessage());
        }
    }

    private void runTsvTest(String filename) throws IOException {
        Path filePath = Path.of(CLDRPaths.TEST_DATA + "decimal", filename);
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
                if (parts.length < 4) {
                    errln(filename + ":" + lineNum + " - Invalid line: " + line);
                    continue;
                }

                String localeStr = parts[0];
                String styleStr = parts[1];
                double input = Double.parseDouble(parts[2]);
                String expected = parts[3];

                ULocale locale = new ULocale(localeStr);
                Style style = Style.fromLabel(styleStr);
                if (style == null) {
                    errln(filename + ":" + lineNum + " - Unknown style: " + styleStr);
                    continue;
                }

                String actual = format(locale, style, input);
                assertEquals(
                        filename
                                + ":"
                                + lineNum
                                + " - Failure for "
                                + localeStr
                                + " ("
                                + styleStr
                                + ") with input "
                                + input,
                        expected,
                        actual);
            }
        }
    }

    enum Style {
        DECIMAL("decimal"),
        PERCENT("percent"),
        SCIENTIFIC("scientific"),
        COMPACT_SHORT("compact-short"),
        COMPACT_LONG("compact-long");

        final String label;

        Style(String label) {
            this.label = label;
        }

        static Style fromLabel(String label) {
            for (Style s : values()) {
                if (s.label.equals(label)) {
                    return s;
                }
            }
            return null;
        }
    }

    private String format(ULocale locale, Style style, double number) {
        com.ibm.icu.number.LocalizedNumberFormatter lnf;
        switch (style) {
            case DECIMAL:
                lnf = NumberFormatter.withLocale(locale);
                break;
            case PERCENT:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .unit(com.ibm.icu.util.MeasureUnit.PERCENT)
                                .scale(com.ibm.icu.number.Scale.powerOfTen(2));
                break;
            case SCIENTIFIC:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .notation(com.ibm.icu.number.Notation.scientific());
                break;
            case COMPACT_SHORT:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .notation(com.ibm.icu.number.Notation.compactShort());
                break;
            case COMPACT_LONG:
                lnf =
                        NumberFormatter.withLocale(locale)
                                .notation(com.ibm.icu.number.Notation.compactLong());
                break;
            default:
                throw new IllegalArgumentException("Unknown style: " + style);
        }
        return lnf.format(number).toString();
    }
}
