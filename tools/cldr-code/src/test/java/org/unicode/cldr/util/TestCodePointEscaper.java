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
        final String forceEscapeRegex =
                CodePointEscaper.regexPattern(CodePointEscaper.ESCAPE_IN_SURVEYTOOL);

        final Matcher matcher = Pattern.compile(forceEscapeRegex).matcher("");

        int shouldFail = 'A';
        assertFalse(
                matcher.reset(stringToScan(shouldFail)).find(),
                () -> "For " + CodePointEscaper.hexEscape(shouldFail));

        int shouldSucceed = 0x202F;
        assertTrue(
                matcher.reset(stringToScan(shouldSucceed)).find(),
                () -> "For " + CodePointEscaper.hexEscape(shouldSucceed));

        // NOTE: A UnicodeSet pattern may look like regex, but the syntax is not the same.
        // So we can't use UnicodeSet.toPattern()

        for (final int e : CodePointEscaper.ESCAPE_IN_SURVEYTOOL.codePoints()) {
            assertTrue(
                    matcher.reset(stringToScan(e)).find(),
                    () -> "For " + CodePointEscaper.hexEscape(e));
        }
    }

    private String stringToScan(final int e) {
        final int[] codepoints = {'a', e, 'b'};
        return new String(codepoints, 0, codepoints.length);
    }
}
