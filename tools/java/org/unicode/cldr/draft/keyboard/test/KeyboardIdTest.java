package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.KeyboardId;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

/**
 * Unit tests for {@link KeyboardId}.
 */
public class KeyboardIdTest extends TestFmwk {

    public void testKeyboardId() {
        KeyboardId id = KeyboardId.fromString("de-CH-t-k0-windows-extended-var");
        assertEquals("", ULocale.forLanguageTag("de-CH"), id.locale());
        assertEquals("", KeyboardId.Platform.WINDOWS, id.platform());
        ImmutableList<String> attributes = ImmutableList.of("extended", "var");
        assertEquals("", attributes, id.attributes());
    }

    public void testFromStringForSimple() {
        KeyboardId keyboardLocale = KeyboardId.fromString("bn-t-k0-windows");
        assertEquals("", "bn-t-k0-windows", keyboardLocale.toString());
    }

    public void testFromStringForComplex() {
        KeyboardId keyboardLocale = KeyboardId.fromString("es-US-t-k0-android-768dpi.xml");
        assertEquals("", "es-US-t-k0-android-768dpi.xml", keyboardLocale.toString());
    }

    public void testFromStringForInvalid() {
        try {
            KeyboardId.fromString("en-US-android");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected behavior.
        }
    }

    public void testEqualsTrue() {
        KeyboardId id1 = KeyboardId.fromString("de-CH-t-k0-windows-extended-var");
        KeyboardId id2 = KeyboardId.fromString("de-CH-t-k0-windows-extended-var");
        assertTrue("", id1.equals(id2));
        assertTrue("", id1.hashCode() == id2.hashCode());
    }

    public void testEqualsFalse() {
        KeyboardId id1 = KeyboardId.fromString("de-CH-t-k0-windows-extended-var");
        KeyboardId id2 = KeyboardId.fromString("es-US-t-k0-android-768dpi.xml");
        assertFalse("", id1.equals(id2));
    }
}
