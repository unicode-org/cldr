package org.unicode.cldr.unittest;

import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public class TestDisplayAndInputProcessor extends TestFmwk {

    TestInfo info = TestAll.TestInfo.getInstance();

    public static void main(String[] args) {
        new TestDisplayAndInputProcessor().run(args);
    }

    public void TestAll() {
        showCldrFile(info.getEnglish());
        showCldrFile(info.getCldrFactory().make("wae", true));
    }

    public void TestTasawaq() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getCldrFactory().make("twq", true));
        // time for data driven test
        final String input = "[Z \u017E ]";
        final String expect = "[z \u017E]"; // lower case
        String value = daip.processInput(
            "//ldml/characters/exemplarCharacters",
            input, null);
        if (!value.equals(expect)) {
            errln("Tasawaq incorrectly normalized with output: '" + value + "', expected '" + expect + "'");
        }
    }

    public void TestMalayalam() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getCldrFactory().make("ml", false));
        String value = daip.processInput(
            "//ldml/localeDisplayNames/languages/language[@type=\"alg\"]",
            "അല്‍ഗോണ്‍ക്യന്‍ ഭാഷ", null);
        if (!value.equals("\u0D05\u0D7D\u0D17\u0D4B\u0D7A\u0D15\u0D4D\u0D2F\u0D7B \u0D2D\u0D3E\u0D37")) {
            errln("Malayalam incorrectly normalized with output: " + value);
        }
    }

    public void TestRomanian() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getCldrFactory().make("ro", false));
        String value = daip.processInput(
            "//ldml/localeDisplayNames/types/type[@type=\"hant\"][@key=\"numbers\"]",
            "Numerale chineze\u015Fti tradi\u0163ionale", null);
        if (!value.equals("Numerale chineze\u0219ti tradi\u021Bionale")) {
            errln("Romanian incorrectly normalized: " + value);
        }
    }

    public void TestCompactNumberFormats() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath = "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"] ";
        String value = daip.processInput(xpath, "0.00K.", null);
        assertEquals("Period not correctly quoted", "0K'.'", value);
        value = daip.processInput(xpath, "00.0K'.'", null);
        assertEquals("Quotes should not be double-quoted", "00K'.'", value);
        value = daip.processForDisplay(xpath, "0.0 K'.'");
        assertEquals("There should be no quotes left", "0.0 K.", value);
    }

    public void TestPatternCanonicalization() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath = "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String value = daip.processInput(xpath, "#,###,##0.###", null);
        assertEquals("Format not correctly canonicalized", "#,##0.###", value);
    }

    public void TestCurrencyFormatSpaces() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath = "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String value = daip.processInput(xpath, "¤ #,##0.00", null); // breaking space
        assertEquals("Breaking space not replaced", "¤ #,##0.00", value); // non-breaking space
    }

    public void TestQuoteNormalization() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath = "//ldml/units/unitLength[@type=\"narrow\"]/unitPattern[@count=\"one\"]";
        String value = daip.processInput(xpath, "{0}''", null); // breaking space
        assertEquals("Quotes not normalized", "{0}″", value); // non-breaking space
    }

    private void showCldrFile(final CLDRFile cldrFile) {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(cldrFile, true);
        Exception[] internalException = new Exception[1];
        for (String path : cldrFile) {
            String value = cldrFile.getStringValue(path);
            String display = daip.processForDisplay(path, value);
            internalException[0] = null;
            String input = daip.processInput(path, display, internalException);
            String diff = diff(value, input, path);
            if (diff != null) {
                errln("No roundtrip in DAIP:"
                    + "\n\tvalue<" + value
                    + ">\n\tdisplay<" + display
                    + ">\n\tinput<" + input
                    + ">\n\tdiff<" + diff
                    + (internalException[0] != null ? ">\n\texcep<" + internalException[0] : "")
                    + ">\n\tpath<" + path + ">");
                daip.processInput(path, value, internalException); // for debugging
            } else if (!CharSequences.equals(value, display)
                || !CharSequences.equals(value, input)
                || internalException[0] != null) {
                logln("DAIP Changes"
                    + "\n\tvalue<" + value
                    + ">\n\tdisplay<" + display
                    + ">\n\tinput<" + input
                    + ">\n\tdiff<" + diff
                    + (internalException[0] != null ? ">\n\texcep<" + internalException[0] : "")
                    + ">\n\tpath<" + path + ">");
            }
        }
    }

    private String diff(String value, String input, String path) {
        if (value.equals(input)) {
            return null;
        }
        if (path.contains("/exemplarCharacters")) {
            try {
                UnicodeSet s1 = new UnicodeSet(value);
                UnicodeSet s2 = new UnicodeSet(input);
                if (!s1.equals(s2)) {
                    UnicodeSet temp = new UnicodeSet(s1).removeAll(s2);
                    UnicodeSet temp2 = new UnicodeSet(s2).removeAll(s1);
                    temp.addAll(temp2);
                    return temp.toPattern(false);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        String value2 = value.replace('[', '(').replace(']', ')').replace('［', '（').replace('］', '）');
        if (value2.equals(input)) {
            return null;
        }
        return "?";
    }
}
