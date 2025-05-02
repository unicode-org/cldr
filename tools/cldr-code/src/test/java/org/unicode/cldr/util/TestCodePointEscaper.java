package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
