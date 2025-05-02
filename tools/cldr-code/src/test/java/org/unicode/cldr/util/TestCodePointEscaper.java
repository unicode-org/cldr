package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * @see org.unicode.cldr.unittest.UnicodeSetPrettyPrinterTest
 */
public class TestCodePointEscaper {
    @Test
    void testNsms() {
        // System.out.println("Invisibles: " + CodePointEscaper.INVISIBLES.toPattern(true));
        // System.out.println("Named Invisibles " + CodePointEscaper.NAMED_INVISIBLES);
        // can uncomment this to check they are actually invisible!
        // System.out.print("Here they are: ->");
        // CodePointEscaper.NAMED_INVISIBLES.forEach(s -> System.out.print(s));
        // System.out.println("<--");

        // Make sure all of them are … named!
        CodePointEscaper.NAMED_INVISIBLES.forEach(
                s ->
                        assertNotNull(
                                CodePointEscaper.forCodePoint(s),
                                () ->
                                        "No named invisible for U+"
                                                + Integer.toString(s.codePointAt(0))));
    }
}
