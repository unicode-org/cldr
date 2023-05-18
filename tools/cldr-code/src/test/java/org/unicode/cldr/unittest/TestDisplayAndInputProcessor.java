package org.unicode.cldr.unittest;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.util.Set;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.DisplayAndInputProcessor.PathSpaceType;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.Factory;

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

        for (String s :
                new UnicodeSet(
                        "[!-#%-\\\\_a-~¡§ª-¬±-³ ¶·¹-þ؉٠-٬۰-۹०-९০-৯੦-੯ ૦-૯୦-୯௦-௯౦-౯೦-೯൦-൯༠-༩ ၀-၉’‰−〇一七三九二五八六四]")) {
            if (s.contentEquals("-")) {
                continue; // special case because of non-breaking processing
            }
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
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getCLDRFile("twq", true));
        // time for data driven test
        final String input = "Z \u017E";
        final String expect = "[zž]"; // should only be lowercased if the exemplar class is set.
        String value = daip.processInput("//ldml/characters/exemplarCharacters", input, null);
        if (!value.equals(expect)) {
            errln(
                    "Tasawaq incorrectly normalized with output: '"
                            + value
                            + "', expected '"
                            + expect
                            + "'");
        }
    }

    public void TestMalayalam() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getCLDRFile("ml", false));
        String value =
                daip.processInput(
                        "//ldml/localeDisplayNames/languages/language[@type=\"alg\"]",
                        "അല്‍ഗോണ്‍ക്യന്‍ ഭാഷ",
                        null);
        if (!value.equals(
                "\u0D05\u0D7D\u0D17\u0D4B\u0D7A\u0D15\u0D4D\u0D2F\u0D7B \u0D2D\u0D3E\u0D37")) {
            errln("Malayalam incorrectly normalized with output: " + value);
        }
    }

    public void TestRomanian() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getCLDRFile("ro", false));
        String value =
                daip.processInput(
                        "//ldml/localeDisplayNames/types/type[@type=\"hant\"][@key=\"numbers\"]",
                        "Numerale chineze\u015Fti tradi\u0163ionale",
                        null);
        if (!value.equals("Numerale chineze\u0219ti tradi\u021Bionale")) {
            errln("Romanian incorrectly normalized: " + value);
        }
    }

    public void TestMyanmarZawgyi() {
        // Check that the Zawgyi detector and Zawgyi->Unicode converter perform
        // correctly.
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getCLDRFile("my", false));
        String z_mi = "ေမာင္းရီ (နယူးဇီလန္ကၽြန္းရွိ ပင္ရင္းတိုင္းရင္းသားလူမ်ိဳး)";
        String u_mi = "မောင်းရီ (နယူးဇီလန်ကျွန်းရှိ ပင်ရင်းတိုင်းရင်းသားလူမျိုး)";

        // Check that z_mi is detected as Zawgyi, and converted to u_mi.
        // Check that the converted version is detected as Unicode.
        String converted_z_mi = daip.processInput("", z_mi, null);
        if (!converted_z_mi.equals(u_mi)) {
            errln(
                    "Myanmar Zawgyi value incorrectly normalized: \n "
                            + z_mi
                            + " to \n"
                            + ">"
                            + converted_z_mi
                            + "<, expected\n"
                            + ">"
                            + u_mi
                            + "<");
        }
        String converted_u_mi = daip.processInput("", u_mi, null);
        if (!converted_u_mi.equals(u_mi)) {
            errln("Myanmar Unicode value incorrectly changed:\n" + u_mi + " to\n" + converted_u_mi);
        }
        // TODO(ccorn): test other strings with the converter.
        String mixed_latn_zawgyi = "ABCDE " + z_mi + "XYZ";
        String mixed_latn_unicode = "ABCDE " + u_mi + "XYZ";
        String converted_mixed = daip.processInput("", mixed_latn_zawgyi, null);
        if (!converted_mixed.equals(mixed_latn_unicode)) {
            errln(
                    "Myanmar mixed value incorrectly normalized:"
                            + converted_mixed.length()
                            + "\n"
                            + mixed_latn_zawgyi
                            + " to "
                            + mixed_latn_unicode.length()
                            + "\n"
                            + converted_mixed
                            + ", expected\n"
                            + mixed_latn_unicode);
        }

        // Test 1039 conversion - simple cases.
        String z1039 =
                "\u1031\u1019\u102c\u1004\u1039\u1038\u101b\u102e\u0020\u0028\u1014"
                        + "\u101A\u1030\u1038\u1007\u102E\u101C\u1014\u1039\u1000\u107D\u103C\u1014\u1039\u1038\u101B\u103D\u102D";
        String u103a =
                "\u1019\u1031\u102c\u1004\u103a\u1038\u101b\u102e\u0020\u0028\u1014"
                        + "\u101A\u1030\u1038\u1007\u102E\u101C\u1014\u103A\u1000\u103B\u103D\u1014\u103A\u1038\u101B\u103E\u102D";
        String converted_1039 = daip.processInput("", z1039, null);
        if (!converted_1039.equals(u103a)) {
            errln(
                    "Myanmar #1039 (Unicode) was changed: \n"
                            + z1039
                            + " to \n"
                            + converted_1039
                            + ", expected \n"
                            + u103a);
        }

        String z0 = "\u1000\u1005\u102C\u1038\u101E\u1019\u102C\u1038"; // Test
        // #0
        String converted_0 = daip.processInput("", z0, null);
        if (!converted_0.equals(z0)) {
            errln("Myanmar #0 (Unicode) was changed: " + z0 + " to " + converted_0);
        }

        String z5 = "\u1021\u101E\u1004\u1039\u1038\u1019\u103D"; // Test #5
        String u5 = "\u1021\u101E\u1004\u103A\u1038\u1019\u103E";
        String converted_5 = daip.processInput("", z5, null);
        if (!converted_5.equals(u5)) {
            errln("Myanmar #5 incorrectly normalized: " + z5 + " to " + converted_5);
        }

        String z_zero = "\u1031\u1040\u1037";
        String u_zero = "\u101d\u1031\u1037";
        String converted_zero = daip.processInput("", z_zero, null);
        if (!converted_zero.equals(u_zero)) {
            errln(
                    "Myanmar with diacritics and zero incorrectly normalized:\n"
                            + z_zero
                            + " to\n"
                            + converted_zero
                            + '\n'
                            + u_zero);
        }
        // Check that multiple digits are not converted.
        z_zero = "\u1041\u1040\u1037";
        u_zero = "\u1041\u1040\u1037";
        converted_zero = daip.processInput("", z_zero, null);
        if (!converted_zero.equals(u_zero)) {
            errln(
                    "Myanmar with two zeros incorrectly normalized:\n"
                            + z_zero
                            + " to\n"
                            + converted_zero
                            + '\n'
                            + u_zero);
        }

        // More checks that Unicode is not converted.
        String is_unicode = "\u1019\u101B\u103E\u102D\u101E\u1031\u102C";
        String check_is_unicode = daip.processInput("", is_unicode, null);
        if (!check_is_unicode.equals(is_unicode)) {
            errln("Myanmar should not have converted:\n" + is_unicode + " to\n" + check_is_unicode);
        }
        is_unicode = "\u1001\u103B\u103c";
        check_is_unicode = daip.processInput("", is_unicode, null);
        if (!check_is_unicode.equals(is_unicode)) {
            errln("Myanmar should not have converted:\n" + is_unicode + " to\n" + check_is_unicode);
        }
        is_unicode = "\u1001\u103E\u103A";
        check_is_unicode = daip.processInput("", is_unicode, null);
        if (!check_is_unicode.equals(is_unicode)) {
            errln("Myanmar should not have converted:\n" + is_unicode + " to\n" + check_is_unicode);
        }
    }

    public void TestCompactNumberFormats() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath =
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"] ";
        String value = daip.processInput(xpath, "0.00K.", null);
        assertEquals("Period not correctly quoted", "0K'.'", value);
        value = daip.processInput(xpath, "00.0K'.'", null);
        assertEquals("Quotes should not be double-quoted", "00K'.'", value);
        value = daip.processForDisplay(xpath, "0.0 K'.'");
        assertEquals("There should be no quotes left", "0.0 K.", value);
    }

    public void TestPatternCanonicalization() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath =
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String value = daip.processInput(xpath, "#,###,##0.###", null);
        assertEquals("Format not correctly canonicalized", "#,##0.###", value);
    }

    public void TestCurrencyFormatSpaces() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath =
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String value = daip.processInput(xpath, "¤ #,##0.00", null); // breaking
        // space
        assertEquals("Breaking space not replaced", "¤ #,##0.00", value); // non-breaking
        // space
    }

    private Boolean usesModifierApostrophe(CLDRFile testFile) {
        char MODIFIER_LETTER_APOSTROPHE = '\u02BC';
        String exemplarSet = testFile.getWinningValue("//ldml/characters/exemplarCharacters");
        UnicodeSet mainExemplarSet = new UnicodeSet(exemplarSet);
        UnicodeSetIterator usi = new UnicodeSetIterator(mainExemplarSet);
        while (usi.next()) {
            if (usi.codepoint == MODIFIER_LETTER_APOSTROPHE
                    || (usi.codepoint == UnicodeSetIterator.IS_STRING
                            && usi.getString().indexOf(MODIFIER_LETTER_APOSTROPHE) >= 0)) {
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
                    if (!DisplayAndInputProcessor.LANGUAGES_USING_MODIFIER_APOSTROPHE.contains(
                            thisLanguage)) {
                        errln(
                                "Language : "
                                        + thisLanguage
                                        + " uses MODIFIER_LETTER_APOSROPHE, but is not on the list in DAIP.LANGUAGES_USING_MODIFIER_APOSTROPHE");
                    }
                } else {
                    if (DisplayAndInputProcessor.LANGUAGES_USING_MODIFIER_APOSTROPHE.contains(
                            thisLanguage)) {
                        errln(
                                "Language : "
                                        + thisLanguage
                                        + "is on the list in DAIP.LANGUAGES_USING_MODIFIER_APOSTROPHE, but the main exemplars don't use this character.");
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                errln("Error in " + thisLanguage + " - " + t.getMessage());
            }
        }
    }

    public void TestQuoteNormalization() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath = "//ldml/units/unitLength[@type=\"narrow\"]/unitPattern[@count=\"one\"]";
        String value = daip.processInput(xpath, "{0}''", null); // breaking
        // space
        assertEquals("Quotes not normalized", "{0}″", value); // non-breaking
        // space
    }

    public void TestAdlamNasalization() {
        DisplayAndInputProcessor daip =
                new DisplayAndInputProcessor(info.getCLDRFile("ff_Adlm", false));
        final String xpath_a =
                "//ldml/localeDisplayNames/types/type[@type=\"hant\"][@key=\"numbers\"]";
        final String TEST_DATA[] = {
            xpath_a, // xpath
            "{0} 𞤸𞤭𞤼𞤢𞥄𞤲'𞤣𞤫", // src
            "{0} 𞤸𞤭𞤼𞤢𞥄𞤲" + DisplayAndInputProcessor.ADLAM_NASALIZATION + "𞤣𞤫", // dst
            xpath_a, // xpath
            "𞤐‘𞤄𞤵𞥅𞤯𞤭", // src
            "𞤐" + DisplayAndInputProcessor.ADLAM_NASALIZATION + "𞤄𞤵𞥅𞤯𞤭", // dst
            xpath_a,
            "𞤑𞤭𞤶𞤮𞥅𞤪𞤫 𞤖𞤢𞤱𞤪𞤭𞤼𞤵𞤲‘𞤣𞤫 𞤖𞤭𞥅𞤪𞤲𞤢𞥄𞤲‘𞤺𞤫 𞤘𞤪𞤭𞤲𞤤𞤢𞤲𞤣",
            "𞤑𞤭𞤶𞤮𞥅𞤪𞤫 𞤖𞤢𞤱𞤪𞤭𞤼𞤵𞤲"
                    + DisplayAndInputProcessor.ADLAM_NASALIZATION
                    + "𞤣𞤫 𞤖𞤭𞥅𞤪𞤲𞤢𞥄𞤲"
                    + DisplayAndInputProcessor.ADLAM_NASALIZATION
                    + "𞤺𞤫 𞤘𞤪𞤭𞤲𞤤𞤢𞤲𞤣",
            xpath_a, // no change
            "'Something' ‘Else’",
            "‘Something’ ‘Else’" // smart quotes
        };
        for (int i = 0; i < TEST_DATA.length; i += 3) {
            final String xpath = TEST_DATA[i + 0];
            final String src = TEST_DATA[i + 1];
            final String dst = TEST_DATA[i + 2];

            String value = daip.processInput(xpath, src, null);
            assertEquals("ff_Adlm: " + src, dst, value);
        }
    }

    private void showCldrFile(final CLDRFile cldrFile) {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(cldrFile, true);
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
                display = daip.processForDisplay(path, value);
                input = daip.processInput(path, display, internalException);
                diff(value, input, path);
                errln(
                        cldrFile.getLocaleID()
                                + "\tNo roundtrip in DAIP:"
                                + "\n\t  value<"
                                + value
                                + ">\n\tdisplay<"
                                + display
                                + ">\n\t  input<"
                                + input
                                + ">\n\t   diff<"
                                + diff
                                + (internalException[0] != null
                                        ? ">\n\texcep<" + internalException[0]
                                        : "")
                                + ">\n\tpath<"
                                + path
                                + ">");
                // for debugging
                daip.processForDisplay(path, value);
                daip.processInput(path, display, internalException);
            } else if (!CharSequences.equals(value, display)
                    || !CharSequences.equals(value, input)
                    || internalException[0] != null) {
                logln(
                        "DAIP Changes"
                                + "\n\tvalue<"
                                + value
                                + ">\n\tdisplay<"
                                + display
                                + ">\n\tinput<"
                                + input
                                + ">\n\tdiff<"
                                + diff
                                + (internalException[0] != null
                                        ? ">\n\texcep<" + internalException[0]
                                        : "")
                                + ">\n\tpath<"
                                + path
                                + ">");
            }
        }
    }
    /** DAIP can add characters to UnicodeSets, so remove them for a clean test. Could optimize */
    UnicodeSet suppressAdditions(UnicodeSet value, UnicodeSet input_value) {
        for (UnicodeSetIterator usi = new UnicodeSetIterator(value); usi.next(); ) {
            switch (usi.getString()) {
                case "\u2011":
                    input_value.remove('-');
                    break; // nobreak hyphen
                case "-":
                    input_value.remove('\u2011');
                    break; // nobreak hyphen
                case " ":
                    input_value.remove('\u00a0');
                    break; // nobreak space
                case "\u00a0":
                    input_value.remove(' ');
                    break; // nobreak space
                case "\u202F":
                    input_value.remove('\u2009');
                    break; // nobreak narrow space
                case "\u2009":
                    input_value.remove('\u202F');
                    break; // nobreak narrow space
            }
        }
        return input_value;
    }

    private String diff(String value, String input, String path) {
        if (value.equals(input)) {
            return null;
        }
        if (path.contains("/foreignSpaceReplacement") || path.contains("/nativeSpaceReplacement")) {
            return null; // CLDR-15384 typically inherited; no DAIP processing desired
        }
        if (path.contains("/exemplarCharacters") || path.contains("/parseLenient")) {
            try {
                UnicodeSet valueSet = new UnicodeSet(value);
                UnicodeSet inputSet;
                try {
                    inputSet = new UnicodeSet(input);
                } catch (Exception e) {
                    inputSet = UnicodeSet.EMPTY;
                }
                if (!valueSet.equals(inputSet)) {
                    // The test has problems, because DAIP can add characters.
                    // So check that it adds them right
                    UnicodeSet value_input = new UnicodeSet(valueSet).removeAll(inputSet);
                    UnicodeSet input_value =
                            suppressAdditions(
                                    valueSet, new UnicodeSet(inputSet).removeAll(valueSet));
                    if (!value_input.isEmpty() || !input_value.isEmpty()) {
                        return (value_input.isEmpty() ? "" : "V-I:" + value_input.toPattern(true))
                                + (input_value.isEmpty()
                                        ? ""
                                        : "I-V:" + input_value.toPattern(true));
                    }
                }
                return null;
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        String value2 =
                value.replace('[', '(').replace(']', ')').replace('［', '（').replace('］', '）');
        if (value2.equals(input)) {
            return null;
        }
        return "?";
    }

    /** Test whether DisplayAndInputProcessor.processInput removes backspaces */
    public void TestBackspaceFilter() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath = "//ldml/localeDisplayNames/languages/language[@type=\"fr\"]";
        String value = daip.processInput(xpath, "\btest\bTEST\b", null);
        assertEquals("Backspaces are filtered out", "testTEST", value);
    }

    /**
     * Test whether DisplayAndInputProcessor.processInput normalizes whitespace. This depends very
     * much on the xpath, since for most xpaths NBSP is normalized to ordinary space which, if
     * initial or final, is then removed by trim(). But for some xpaths, NBSP is retained and then
     * the standard trim() function doesn't apply. + * + * Each of the 3 types of paths, and for
     * each, samples with + * A0 20 + * 20 A0 + * 20 A0 20 + * 20 20
     */
    public void TestWhitespaceNormalization() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        PathSpaceData[] a = PathSpaceData.getArray();
        for (int i = 0; i < a.length; i++) {
            PathSpaceType pst = PathSpaceType.get(a[i].xpath);
            assertEquals("Path has expected type for i = " + i, a[i].pst, pst);
            String val = daip.processInput(a[i].xpath, a[i].rawValue, null);
            assertEquals("Whitespace is normalized for i = " + i, a[i].normValue, val);
        }
    }

    private static class PathSpaceData {
        private String xpath;
        private String rawValue, normValue;
        private PathSpaceType pst;

        public PathSpaceData(String xpath, String rawValue, String normValue, PathSpaceType pst) {
            this.xpath = xpath;
            this.rawValue = rawValue;
            this.normValue = normValue;
            this.pst = pst;
        }

        public static PathSpaceData[] getArray() {
            PathSpaceData[] a = {
                new PathSpaceData(
                        "//ldml/localeDisplayNames/types/type",
                        " \u00A0  TEST \u00A0 ",
                        "TEST",
                        PathSpaceType.allowSp),
                new PathSpaceData(
                        "//ldml/localeDisplayNames/languages/language[@type=\"ab\"]",
                        "\u00A0  FOO \u00A0\u00A0 BAR \u00A0",
                        "FOO BAR",
                        PathSpaceType.allowSp),
                new PathSpaceData(
                        "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"Bh\"]",
                        "\u00A0  DUCK \u00A0  GOOSE \u00A0",
                        "DUCK GOOSE",
                        PathSpaceType.allowSp),

                // removed temporarily per CLDR-16210
                // new
                // PathSpaceData("//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"Bh\"]",
                //     "\u202F  BOOK \u202F  HORSE \u202F", "BOOK HORSE", PathSpaceType.allowSp),

                new PathSpaceData(
                        "//ldml/numbers/currencies/currency/group",
                        " \u00A0  ፊደል \u00A0 ",
                        "ፊደል",
                        PathSpaceType.allowNbsp),
                new PathSpaceData(
                        "//ldml/numbers/currencyFormats/currencySpacing/beforeCurrency/insertBetween",
                        "\u00A0  ding \u00A0\u00A0 dong \u00A0",
                        "ding\u00A0dong",
                        PathSpaceType.allowNbsp),
                new PathSpaceData(
                        "//ldml/numbers/symbols/nan",
                        "\u00A0  HA   HU \u00A0",
                        "HA\u00A0HU",
                        PathSpaceType.allowNbsp),

                // removed temporarily per CLDR-16210
                // new PathSpaceData("//ldml/numbers/symbols/nan",
                //    "\u202F  BA \u202F  BU \u202F", "BA\u00A0BU", PathSpaceType.allowNbsp),

                new PathSpaceData(
                        "//ldml/numbers/symbols[@numberSystem=\"telu\"]/approximatelySign",
                        " \u00A0  试 \u00A0 ",
                        "试",
                        PathSpaceType.allowSpOrNbsp),
                new PathSpaceData(
                        "//ldml/dates/calendars/calendar[@type=\"hebrew\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"yMMMM\"]/greatestDifference[@id=\"M\"]",
                        "\u00A0  X \u00A0 Y \u00A0",
                        "X\u00A0Y",
                        PathSpaceType.allowSpOrNbsp),
                new PathSpaceData(
                        "//ldml/dates/calendars/calendar[@type=\"islamic-civil\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"MEd\"]/greatestDifference[@id=\"M\"]",
                        "\u00A0  Marvin   Gaye \u00A0",
                        "Marvin Gaye",
                        PathSpaceType.allowSpOrNbsp),
                new PathSpaceData(
                        "//ldml/dates/calendars/calendar[@type=\"islamic-civil\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"MEd\"]/greatestDifference[@id=\"M\"]",
                        "〖\u00A0\u00A0〗",
                        "〖\u00A0〗",
                        PathSpaceType.allowSpOrNbsp),

                // removed temporarily per CLDR-16210
                // new PathSpaceData(
                //
                // "//ldml/dates/calendars/calendar[@type=\"islamic-civil\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"MEd\"]/greatestDifference[@id=\"M\"]",
                //    "*〖\u202F\u00A0〗*", "*〖\u00A0〗*", PathSpaceType.allowSpOrNbsp),

                /*
                 * The following path is an exception in which regular space is changed to NBSP
                 * in spite of the path being allowSpOrNbsp; see comment "fix grouping separator if space"
                 */
                new PathSpaceData(
                        "//ldml/numbers/symbols[@numberSystem=\"telu\"]/approximatelySign",
                        "\u00A0  P   Q \u00A0",
                        "P\u00A0Q",
                        PathSpaceType.allowSpOrNbsp),
                /*
                 * The following path is an exception in which NBSP is changed to regular space
                 * in spite of the path being allowSpOrNbsp; see DisplayAndInputProcessor.annotationsForDisplay
                 */
                new PathSpaceData(
                        "//ldml/annotations/annotation[@cp=\"🍊\"]",
                        "\u00A0  fruit   |   orange   | \u00A0  tangerine \u00A0",
                        "fruit | orange | tangerine",
                        PathSpaceType.allowSpOrNbsp),
                /*
                 * The following path expects NNBSP (U+202F)
                 */
                // removed temporarily per CLDR-16210
                // new
                // PathSpaceData("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"narrow\"]/dayPeriod[@type=\"am\"]",
                //    " pizza \u00A0\u202F cardboard ", "pizza\u202Fcardboard",
                //    PathSpaceType.allowNNbsp),
            };
            return a;
        }
    }

    /** Test whether DisplayAndInputProcessor.processInput correctly makes whitespace adjustments */
    public void TestWhitespaceAdjustments() {
        class PathSpaceAdjustData {
            String locale;
            String xpath;
            String rawValue;
            String normValue;

            PathSpaceAdjustData(String loc, String path, String raw, String norm) {
                this.locale = loc;
                this.xpath = path;
                this.rawValue = raw;
                this.normValue = norm;
            }
        }

        PathSpaceAdjustData[] testItems = {
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                    "h:mm a",
                    "h:mm\u202Fa"),
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                    "h:mm aaaa",
                    "h:mm aaaa"), // no adjustment for aaaa
            new PathSpaceAdjustData(
                    "ja",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"hm\"]",
                    "a K:mm",
                    "a K:mm"),
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"hm\"]/greatestDifference[@id=\"a\"]",
                    "h:mm - h:mm a",
                    "h:mm\u2009–\u2009h:mm\u202Fa"),
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatFallback",
                    "{0} – {1}",
                    "{0}\u2009–\u2009{1}"),
            new PathSpaceAdjustData(
                    "uk",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                    "d MMM y 'р'.",
                    "d MMM y\u202F'р'."),
            new PathSpaceAdjustData(
                    "uk",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"yMMMd\"]",
                    "d MMM y 'р'.",
                    "d MMM y\u202F'р'."),
            new PathSpaceAdjustData(
                    "uk",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"yMMMd\"]/greatestDifference[@id=\"M\"]",
                    "d MMM - d MMM y 'р'.",
                    "d MMM – d MMM y\u202F'р'."),
            new PathSpaceAdjustData(
                    "es",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"narrow\"]/dayPeriod[@type=\"am\"]",
                    "a. m.",
                    "a.\u202Fm."),
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/units/unitLength[@type=\"narrow\"]unit[@type=\"mass-gram\"]/unitPattern[@count=\"other\"]",
                    "{0} g",
                    "{0}\u202Fg"),
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/units/unitLength[@type=\"narrow\"]unit[@type=\"mass-gram\"]/unitPattern[@count=\"other\"]",
                    "g {0}",
                    "g\u202F{0}"),
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/units/unitLength[@type=\"short\"]unit[@type=\"mass-gram\"]/unitPattern[@count=\"other\"]",
                    "{0} g",
                    "{0}\u00A0g"),
            new PathSpaceAdjustData(
                    "en",
                    "//ldml/units/unitLength[@type=\"short\"]unit[@type=\"mass-gram\"]/unitPattern[@count=\"other\"]",
                    "g {0}",
                    "g\u00A0{0}"),
        };

        for (PathSpaceAdjustData testItem : testItems) {
            DisplayAndInputProcessor daip =
                    new DisplayAndInputProcessor(info.getCLDRFile(testItem.locale, true), false);
            String normValue = daip.processInput(testItem.xpath, testItem.rawValue, null);
            assertEquals(
                    "Whitespace adjustment for " + testItem.xpath, testItem.normValue, normValue);
        }
    }

    /**
     * Test whether DisplayAndInputProcessor.processInput correctly normalizes annotations
     * containing “|” = U+007C VERTICAL LINE or its variations
     */
    public void TestSplitBar() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        String xpath = "//ldml/annotations/annotation[@cp=\"🆎\"]";
        String val = daip.processInput(xpath, "a|b", null);
        String normVal = "a | b";
        assertEquals("Pipe gets spaces", normVal, val);
        val = daip.processInput(xpath, "a   l   b", null);
        assertEquals("Lowercase L with spaces becomes pipe", normVal, val);
        val = daip.processInput(xpath, "alb", null);
        assertEquals("Lowercase L without spaces does not become pipe", "alb", val);
        val = daip.processInput(xpath, "a।b", null);
        assertEquals("U+0964 DEVANAGARI DANDA without spaces becomes pipe", normVal, val);
        val = daip.processInput(xpath, "a  ।  b", null);
        assertEquals("U+0964 DEVANAGARI DANDA with spaces becomes pipe", normVal, val);
    }

    public void TestFSR() {
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(info.getEnglish(), false);
        checkPathAllowsEmpty(daip, DisplayAndInputProcessor.FSR_START_PATH);
        checkPathAllowsEmpty(daip, DisplayAndInputProcessor.NOL_START_PATH);
    }

    public void checkPathAllowsEmpty(DisplayAndInputProcessor daip, String xpath) {
        String val = daip.processInput(xpath, DisplayAndInputProcessor.EMPTY_ELEMENT_VALUE, null);
        assertEquals(DisplayAndInputProcessor.EMPTY_ELEMENT_VALUE + " input for " + xpath, "", val);
        String roundTrip = daip.processForDisplay(xpath, "");
        assertEquals(
                "Empty FSR output for" + xpath,
                DisplayAndInputProcessor.EMPTY_ELEMENT_VALUE,
                roundTrip);
    }

    public void TestUnicodeSetExamples() {
        String[][] tests = {
            // unicodeSet, displayForm, roundtrip
            {
                "//ldml/characters/exemplarCharacters",
                "[a-c {def} å \\u200B \\- . ๎ ็]",
                "❰WNJ❱ ๎ ็ - . a b c def å",
                "[\\u200B ๎ ็ a b c {def} å]",
                // note: DAIP also adds break/nobreak alternates for
                // hyphen, and removes some characters if exemplars
            },
            {
                "//ldml/characters/parseLenients[@scope=\"date\"][@level=\"lenient\"]/parseLenient[@sample=\"-\"]",
                "[a-c {def} å \\u200B \\- . ๎ ็]",
                "❰WNJ❱ ๎ ็ - . a b c def å",
                "[\\u200B ๎ ็ \\- ‑ . a b c {def} å]",
                // note: DAIP also adds break/nobreak alternates
                // for hyphen, etc.
            },
        };
        // Note, since processInput does a fixup for examplars, we account for that in the input.
        // If we had just \u200B, then \u2011 gets added.
        Exception[] excp = new Exception[1];
        int count = 0;
        DisplayAndInputProcessor daip =
                new DisplayAndInputProcessor(info.getCLDRFile("da", true), true);
        for (String[] test : tests) {
            final String path = test[0];
            final String unicodeSet = new UnicodeSet(test[1]).toPattern(true); // normalize
            final String expectedDisplayForm = test[2];
            final String expectedXmlForm = test[3];
            // final String expectedRoundtrip = new UnicodeSet(test[3]).toPattern(true); //
            // normalize

            String actualDisplayForm = daip.processForDisplay(path, unicodeSet);
            assertEquals(
                    ++count + ") unicodeSet to display, " + path,
                    expectedDisplayForm,
                    actualDisplayForm);
            String actualXmlFormat = daip.processInput(path, expectedDisplayForm, excp);
            assertEquals(
                    count + ") display to unicodeSet, " + path, expectedXmlForm, actualXmlFormat);

            // Now we check that processInput can work on the display form.
            // Just in case the ST calls it twice.

            String doubleInputProcessing = daip.processInput(path, actualXmlFormat, excp);
            assertEquals(
                    count + ") processInput twice, " + path,
                    actualXmlFormat,
                    doubleInputProcessing);
        }
    }
}
