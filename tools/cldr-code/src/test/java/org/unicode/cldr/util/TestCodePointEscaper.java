package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @see org.unicode.cldr.unittest.UnicodeSetPrettyPrinterTest
 */
public class TestCodePointEscaper {
    @Test
    void testForEach() {
        for (final CodePointEscaper e : CodePointEscaper.values()) {
            assertEquals(e, CodePointEscaper.forCodePoint(e.getString()));
            assertTrue(
                    CodePointEscaper.FORCE_ESCAPE.contains(e.getCodePoint()),
                    () -> "For " + e.name());
        }
    }

    @Test
    void testRegex() {
        // NOTE: A UnicodeSet pattern may look like regex, but the syntax is not the same.
        // So we can't use UnicodeSet.toPattern()

        final String forceEscapeRegex =
                CodePointEscaper.regexPattern(CodePointEscaper.ESCAPE_IN_SURVEYTOOL);

        final Matcher matcher = Pattern.compile(forceEscapeRegex).matcher("");

        for (final int e : CodePointEscaper.ESCAPE_IN_SURVEYTOOL.codePoints()) {
            assertTrue(
                    matcher.reset(stringToScan(e)).find(),
                    () -> "For " + CodePointEscaper.hexEscape(e));
        }

        // example where should fail
        int shouldFail = 'A';
        assertFalse(
                matcher.reset(stringToScan(shouldFail)).find(),
                () -> "For " + CodePointEscaper.hexEscape(shouldFail));
    }

    private String stringToScan(final int e) {
        final int[] codepoints = {'a', e, 'b'};
        return new String(codepoints, 0, codepoints.length);
    }
}
