package org.unicode.cldr.unittest;

import org.unicode.cldr.tool.CLDRFilePseudolocalizer;
import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.dev.test.TestFmwk;

/**
 * Unit tests for pseudo locale generation tool.
 *
 * @author viarheichyk@google.com (Igor Viarheichyk)
 */
public class TestPseudolocalization extends TestFmwk {

    public static void main(String[] args) {
        new TestPseudolocalization().run(args);
    }

    public void testConverter() {
        CLDRFilePseudolocalizer ps = CLDRFilePseudolocalizer.createInstanceXA();

        CLDRFile result = ps.generate();

        assertEquals("Language name is not pseudolocalized",
            "[Ɓéļåŕûšîåñ one two]",
            result.getStringValue("//ldml/localeDisplayNames/languages/language[@type=\"be\"]"));

        assertEquals("Numeric placeholders should not be pseudolocalized",
            "[Ļåñĝûåĝé∶ {0} one two]",
            result.getStringValue(
                "//ldml/localeDisplayNames/codePatterns/codePattern[@type=\"language\"]"));

        assertEquals("Exemplar characters are not properly merged",
            "[a b c d e f g h i j k l m n o p q r s t u v w x y z "
                + "\\u2045 å \\u2003 ƀ ç ð é ƒ ĝ ĥ î ĵ ķ ļ ɱ ñ ö þ ǫ ŕ š ţ û ṽ ŵ ẋ ý ž \\u2046]",
            result.getStringValue("//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]"));

        assertEquals("Date and time placeholders should only be bracketed",
            "[h:mm:ss a]",
            result.getStringValue(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/"
                    + "availableFormats/dateFormatItem[@id=\"hms\"]"));

        assertEquals("Only literal part of a placeholder should be accented",
            "[{1} 'åţ' {0} 'one']",
            result.getStringValue(
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/"
                    + "dateTimeFormatLength[@type=\"long\"]/dateTimeFormat[@type=\"standard\"]/"
                    + "pattern[@type=\"standard\"]"));
    }
}