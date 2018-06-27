package org.unicode.cldr.unittest;

import java.util.Set;

import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.Factory;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestDisplayAndInputProcessor extends TestFmwk {

    CLDRConfig info = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestDisplayAndInputProcessor().run(args);
    }

    public void TestAll() {
        showCldrFile(info.getEnglish());
        showCldrFile(info.getCLDRFile("ar", true));
        showCldrFile(info.getCLDRFile("ja", true));
        showCldrFile(info.getCLDRFile("hi", true));
        showCldrFile(info.getCLDRFile("wae", true));
    }

    public void TestAExemplars() {
        UnicodeSet test = new UnicodeSet();
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), true);
        Exception[] internalException = new Exception[1];

        for (String s : new UnicodeSet("[!-#%-\\]_a-~¡§ª-¬±-³ µ-·¹-þ؉٠-٬۰-۹०-९০-৯੦-੯ ૦-૯୦-୯௦-௯౦-౯೦-೯൦-൯༠-༩ ၀-၉\\‎\\‏’‰−〇一七三九二五八六四]")) {
            test.clear().add(s);
            String value = test.toPattern(false);
            String path = CLDRFile.getExemplarPath(ExemplarType.numbers);

            String display = daip.processForDisplay(path, value);
            internalException[0] = null;
            String input = daip.processInput(path, display, internalException);

            try {
                UnicodeSet roundTrip = new UnicodeSet(input);
                if (!assertEquals(test.toString() + "=>" + display, test, roundTrip)) {
                    input = daip.processInput(path, display, internalException); // for debugging
                }
            } catch (Exception e) {
                errln(test.toString() + "=>" + display + ": Failed to parse " + input);
            }
        }
    }

    public void TestTasawaq() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info
            .getCLDRFile("twq", true));
        // time for data driven test
        final String input = "[Z \u017E ]";
        final String expect = "[z \u017E]"; // lower case
        String value = daip.processInput(
            "//ldml/characters/exemplarCharacters", input, null);
        if (!value.equals(expect)) {
            errln("Tasawaq incorrectly normalized with output: '" + value
                + "', expected '" + expect + "'");
        }
    }

    public void TestMalayalam() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info
            .getCLDRFile("ml", false));
        String value = daip.processInput(
            "//ldml/localeDisplayNames/languages/language[@type=\"alg\"]",
            "അല്‍ഗോണ്‍ക്യന്‍ ഭാഷ", null);
        if (!value
            .equals("\u0D05\u0D7D\u0D17\u0D4B\u0D7A\u0D15\u0D4D\u0D2F\u0D7B \u0D2D\u0D3E\u0D37")) {
            errln("Malayalam incorrectly normalized with output: " + value);
        }
    }

    public void TestRomanian() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info
            .getCLDRFile("ro", false));
        String value = daip
            .processInput(
                "//ldml/localeDisplayNames/types/type[@type=\"hant\"][@key=\"numbers\"]",
                "Numerale chineze\u015Fti tradi\u0163ionale", null);
        if (!value.equals("Numerale chineze\u0219ti tradi\u021Bionale")) {
            errln("Romanian incorrectly normalized: " + value);
        }
    }

    public void TestMyanmarZawgyi() {
        // Check that the Zawgyi detector and Zawgyi->Unicode converter perform
        // correctly.
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info
            .getCLDRFile("my", false));
        String z_mi = "ေမာင္းရီ (နယူးဇီလန္ကၽြန္းရွိ ပင္ရင္းတိုင္းရင္းသားလူမ်ိဳး)"; // language
        // mi
        // in
        // CLDR
        // 24
        String u_mi = "မောင်းရီ (နယူးဇီလန်ကျွန်းရှိ ပင်ရင်းတိုင်းရင်းသားလူမျိုး)"; // mi
        // converted
        // to
        // Unicode

        // Check that z_mi is detected as Zawgyi, and converted to u_mi.
        // Check that the converted version is detected as Unicode.
        String converted_z_mi = daip.processInput("", z_mi, null);
        if (!converted_z_mi.equals(u_mi)) {
            errln("Myanmar Zawgyi value incorrectly normalized: \n " + z_mi
                + " to \n" + ">" + converted_z_mi + "<, expected\n" + ">"
                + u_mi + "<");
        }
        String converted_u_mi = daip.processInput("", u_mi, null);
        if (!converted_u_mi.equals(u_mi)) {
            errln("Myanmar Unicode value incorrectly changed:\n" + u_mi
                + " to\n" + converted_u_mi);
        }
        // TODO(ccorn): test other strings with the converter.
        String mixed_latn_zawgyi = "ABCDE " + z_mi + "XYZ";
        String mixed_latn_unicode = "ABCDE " + u_mi + "XYZ";
        String converted_mixed = daip.processInput("", mixed_latn_zawgyi, null);
        if (!converted_mixed.equals(mixed_latn_unicode)) {
            errln("Myanmar mixed value incorrectly normalized:"
                + converted_mixed.length() + "\n" + mixed_latn_zawgyi
                + " to " + mixed_latn_unicode.length() + "\n"
                + converted_mixed + ", expected\n" + mixed_latn_unicode);
        }

        // Test 1039 conversion - simple cases.
        String z1039 = "\u1031\u1019\u102c\u1004\u1039\u1038\u101b\u102e\u0020\u0028\u1014"
            + "\u101A\u1030\u1038\u1007\u102E\u101C\u1014\u1039\u1000\u107D\u103C\u1014\u1039\u1038\u101B\u103D\u102D";
        String u103a = "\u1019\u1031\u102c\u1004\u103a\u1038\u101b\u102e\u0020\u0028\u1014"
            + "\u101A\u1030\u1038\u1007\u102E\u101C\u1014\u103A\u1000\u103B\u103D\u1014\u103A\u1038\u101B\u103E\u102D";
        String converted_1039 = daip.processInput("", z1039, null);
        if (!converted_1039.equals(u103a)) {
            errln("Myanmar #1039 (Unicode) was changed: \n" + z1039 + " to \n"
                + converted_1039 + ", expected \n" + u103a);
        }

        String z0 = "\u1000\u1005\u102C\u1038\u101E\u1019\u102C\u1038"; // Test
        // #0
        String converted_0 = daip.processInput("", z0, null);
        if (!converted_0.equals(z0)) {
            errln("Myanmar #0 (Unicode) was changed: " + z0 + " to "
                + converted_0);
        }

        String z5 = "\u1021\u101E\u1004\u1039\u1038\u1019\u103D"; // Test #5
        String u5 = "\u1021\u101E\u1004\u103A\u1038\u1019\u103E";
        String converted_5 = daip.processInput("", z5, null);
        if (!converted_5.equals(u5)) {
            errln("Myanmar #5 incorrectly normalized: " + z5 + " to "
                + converted_5);
        }

        String z_with_space = "\u0020\u102e\u0020\u1037\u0020\u1039"; // Test #5
        String u_with_space = "\u00a0\u102e\u00a0\u1037\u00a0\u103a";
        String converted_space = daip.processInput("", z_with_space, null);
        if (!converted_space.equals(u_with_space)) {
            errln("Myanmar with space incorrectly normalized:\n" + z_with_space
                + " to\n" + converted_space + '\n' + u_with_space);
        }

        String z_zero = "\u1031\u1040\u1037";
        String u_zero = "\u101d\u1031\u1037";
        String converted_zero = daip.processInput("", z_zero, null);
        if (!converted_zero.equals(u_zero)) {
            errln("Myanmar with diacritics and zero incorrectly normalized:\n"
                + z_zero + " to\n" + converted_zero + '\n' + u_zero);
        }
        // Check that multiple digits are not converted.
        z_zero = "\u1041\u1040\u1037";
        u_zero = "\u1041\u1040\u1037";
        converted_zero = daip.processInput("", z_zero, null);
        if (!converted_zero.equals(u_zero)) {
            errln("Myanmar with two zeros incorrectly normalized:\n" + z_zero
                + " to\n" + converted_zero + '\n' + u_zero);
        }

        // More checks that Unicode is not converted.
        String is_unicode = "\u1019\u101B\u103E\u102D\u101E\u1031\u102C";
        String check_is_unicode = daip.processInput("", is_unicode, null);
        if (!check_is_unicode.equals(is_unicode)) {
            errln("Myanmar should not have converted:\n" + is_unicode + " to\n"
                + check_is_unicode);
        }
        is_unicode = "\u1001\u103A\u103B";
        check_is_unicode = daip.processInput("", is_unicode, null);
        if (!check_is_unicode.equals(is_unicode)) {
            errln("Myanmar should not have converted:\n" + is_unicode + " to\n"
                + check_is_unicode);
        }
        is_unicode = "\u1001\u103E\u103A";
        check_is_unicode = daip.processInput("", is_unicode, null);
        if (!check_is_unicode.equals(is_unicode)) {
            errln("Myanmar should not have converted:\n" + is_unicode + " to\n"
                + check_is_unicode);
        }
    }

    public void TestCompactNumberFormats() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(
            info.getEnglish(), false);
        String xpath = "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"] ";
        String value = daip.processInput(xpath, "0.00K.", null);
        assertEquals("Period not correctly quoted", "0K'.'", value);
        value = daip.processInput(xpath, "00.0K'.'", null);
        assertEquals("Quotes should not be double-quoted", "00K'.'", value);
        value = daip.processForDisplay(xpath, "0.0 K'.'");
        assertEquals("There should be no quotes left", "0.0 K.", value);
    }

    public void TestPatternCanonicalization() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(
            info.getEnglish(), false);
        String xpath = "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String value = daip.processInput(xpath, "#,###,##0.###", null);
        assertEquals("Format not correctly canonicalized", "#,##0.###", value);
    }

    public void TestCurrencyFormatSpaces() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(
            info.getEnglish(), false);
        String xpath = "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String value = daip.processInput(xpath, "¤ #,##0.00", null); // breaking
        // space
        assertEquals("Breaking space not replaced", "¤ #,##0.00", value); // non-breaking
        // space
    }

    private Boolean usesModifierApostrophe(CLDRFile testFile) {
        char MODIFIER_LETTER_APOSTROPHE = '\u02BC';
        String exemplarSet = testFile
            .getWinningValue("//ldml/characters/exemplarCharacters");
        UnicodeSet mainExemplarSet = new UnicodeSet(exemplarSet);
        UnicodeSetIterator usi = new UnicodeSetIterator(mainExemplarSet);
        while (usi.next()) {
            if (usi.codepoint == MODIFIER_LETTER_APOSTROPHE
                || (usi.codepoint == UnicodeSetIterator.IS_STRING && usi
                    .getString().indexOf(MODIFIER_LETTER_APOSTROPHE) >= 0)) {
                return true;
            }
        }
        return false;
    }

    public void TestModifierApostropheLocales() {
        Factory f = info.getFullCldrFactory();
        Set<String> allLanguages = f.getAvailableLanguages();
        for (String thisLanguage : allLanguages) {
            CLDRFile thisLanguageFile = f.make(thisLanguage, true);
            try {
                if (usesModifierApostrophe(thisLanguageFile)) {
                    if (!DisplayAndInputProcessor.LANGUAGES_USING_MODIFIER_APOSTROPHE
                        .contains(thisLanguage)) {
                        errln("Language : "
                            + thisLanguage
                            + " uses MODIFIER_LETTER_APOSROPHE, but is not on the list in DAIP.LANGUAGES_USING_MODIFIER_APOSTROPHE");
                    }
                } else {
                    if (DisplayAndInputProcessor.LANGUAGES_USING_MODIFIER_APOSTROPHE
                        .contains(thisLanguage)) {
                        errln("Language : "
                            + thisLanguage
                            + "is on the list in DAIP.LANGUAGES_USING_MODIFIER_APOSTROPHE, but the main exemplars don't use this character.");
                    }
                }
            } catch(Throwable t) {
                t.printStackTrace();
                errln("Error in " + thisLanguage + " - " + t.getMessage());
            }
        }
    }

    public void TestQuoteNormalization() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(
            info.getEnglish(), false);
        String xpath = "//ldml/units/unitLength[@type=\"narrow\"]/unitPattern[@count=\"one\"]";
        String value = daip.processInput(xpath, "{0}''", null); // breaking
        // space
        assertEquals("Quotes not normalized", "{0}″", value); // non-breaking
        // space
    }

    private void showCldrFile(final CLDRFile cldrFile) {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(cldrFile,
            true);
        Exception[] internalException = new Exception[1];
        for (String path : cldrFile) {
            String value = cldrFile.getStringValue(path);
            if (value.equals("[\\- , . % ‰ + 0-9]")) {
                int debug = 0;
            }
            String display = daip.processForDisplay(path, value);
            internalException[0] = null;
            String input = daip.processInput(path, display, internalException);
            String diff = diff(value, input, path);
            if (diff != null) {
                errln(cldrFile.getLocaleID() + "\tNo roundtrip in DAIP:"
                    + "\n\t  value<"
                    + value
                    + ">\n\tdisplay<"
                    + display
                    + ">\n\t  input<"
                    + input
                    + ">\n\t   diff<"
                    + diff
                    + (internalException[0] != null ? ">\n\texcep<"
                        + internalException[0] : "")
                    + ">\n\tpath<"
                    + path + ">");
                daip.processInput(path, value, internalException); // for
                // debugging
            } else if (!CharSequences.equals(value, display)
                || !CharSequences.equals(value, input)
                || internalException[0] != null) {
                logln("DAIP Changes"
                    + "\n\tvalue<"
                    + value
                    + ">\n\tdisplay<"
                    + display
                    + ">\n\tinput<"
                    + input
                    + ">\n\tdiff<"
                    + diff
                    + (internalException[0] != null ? ">\n\texcep<"
                        + internalException[0] : "")
                    + ">\n\tpath<"
                    + path + ">");
            }
        }
    }

    private String diff(String value, String input, String path) {
        if (value.equals(input)) {
            return null;
        }
        if (path.contains("/exemplarCharacters") || path.contains("/parseLenient")) {
            try {
                UnicodeSet s1 = new UnicodeSet(value);
                UnicodeSet s2 = new UnicodeSet(input);
                if (!s1.equals(s2)) {
                    UnicodeSet temp = new UnicodeSet(s1).removeAll(s2);
                    UnicodeSet temp2 = new UnicodeSet(s2).removeAll(s1);
                    temp.addAll(temp2);
                    return temp.toPattern(true);
                }
                return null;
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        String value2 = value.replace('[', '(').replace(']', ')')
            .replace('［', '（').replace('］', '）');
        if (value2.equals(input)) {
            return null;
        }
        return "?";
    }
}
