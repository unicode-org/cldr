package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.ModifierKey;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk;

public class ModifierKeyTest extends TestFmwk {

    public void testFromString() {
        ModifierKey key = ModifierKey.fromString("shiftL");
        assertEquals("", ModifierKey.SHIFT_LEFT, key);
    }

    public void testFromStringInvalid() {
        try {
            ModifierKey.fromString("bla");
            fail();
        } catch (IllegalArgumentException e) {
            // Expected.
        }
    }

    public void testModifierKey() {
        ModifierKey key = ModifierKey.COMMAND;
        assertEquals("", ModifierKey.COMMAND, key.sibling());
        assertEquals("", ModifierKey.COMMAND, key.parent());
        assertEquals("", ImmutableList.<ModifierKey> of(), key.children());
    }

    public void testChildModifierKey() {
        ModifierKey key = ModifierKey.CONTROL_RIGHT;
        assertEquals("", ModifierKey.CONTROL_LEFT, key.sibling());
        assertEquals("", ModifierKey.CONTROL, key.parent());
        assertEquals("", ImmutableList.<ModifierKey> of(), key.children());
    }

    public void testParentModifierKey() {
        ModifierKey key = ModifierKey.ALT;
        assertEquals("", ModifierKey.ALT, key.sibling());
        assertEquals("", ModifierKey.ALT, key.parent());
        assertEquals("", ImmutableList.of(ModifierKey.ALT_LEFT, ModifierKey.ALT_RIGHT), key.children());
    }
}
