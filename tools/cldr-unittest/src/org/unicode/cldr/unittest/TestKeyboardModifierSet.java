// Copyright 2012 Google Inc. All Rights Reserved.

package org.unicode.cldr.unittest;

import java.util.EnumSet;

import org.unicode.cldr.draft.KeyboardModifierSet;
import org.unicode.cldr.draft.KeyboardModifierSet.Modifier;

import com.ibm.icu.dev.test.TestFmwk;

/**
 * @author rwainman@google.com (Raymond Wainman)
 *
 */
public class TestKeyboardModifierSet extends TestFmwk {
    public static void main(String[] args) {
        new TestKeyboardModifierSet().run(args);
    }

    public void testParseSet_null() {
        try {
            KeyboardModifierSet.parseSet(null);
            errln("Should have error");
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    public void testParseSet_empty() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet.parseSet("");

        assertNotNull("x", modifierSet);
        assertEquals("x", 1, modifierSet.getVariants().size());
        EnumSet<KeyboardModifierSet.Modifier> emptySet = EnumSet
            .noneOf(KeyboardModifierSet.Modifier.class);
        assertTrue("x", modifierSet.contains(emptySet));
    }

    public void testParseSet_allOnKeys() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet
            .parseSet("cmd+ctrlL+altR");

        assertNotNull("x", modifierSet);
        assertEquals("x", 1, modifierSet.getVariants().size());
        EnumSet<Modifier> testSet = EnumSet.of(Modifier.cmd, Modifier.ctrlL,
            Modifier.altR);
        assertTrue("x", modifierSet.contains(testSet));
    }

    public void testParseSet_allDontCareKeys() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet
            .parseSet("cmd?+ctrlL?");

        assertNotNull("x", modifierSet);
        assertEquals("x", 4, modifierSet.getVariants().size());
        assertTrue("x", modifierSet.contains(EnumSet.noneOf(Modifier.class)));
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.cmd)));
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.ctrlL)));
        assertTrue("x",
            modifierSet.contains(EnumSet.of(Modifier.cmd, Modifier.ctrlL)));
    }

    public void testParseSet_mix() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet
            .parseSet("optR+shiftR+ctrlL?");

        assertNotNull("x", modifierSet);
        assertEquals("x", 2, modifierSet.getVariants().size());
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.optR,
            Modifier.shiftR)));
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.optR,
            Modifier.shiftR, Modifier.ctrlL)));
    }

    public void testParseSet_parentOn() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet.parseSet("alt");

        assertNotNull("x", modifierSet);
        assertEquals("x", 3, modifierSet.getVariants().size());
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.altL)));
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.altR)));
        assertTrue("x",
            modifierSet.contains(EnumSet.of(Modifier.altL, Modifier.altR)));
    }

    public void testParseSet_parentDontCare() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet.parseSet("alt?");

        assertNotNull("x", modifierSet);
        assertEquals("x", 4, modifierSet.getVariants().size());
        assertTrue("x", modifierSet.contains(EnumSet.noneOf(Modifier.class)));
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.altL)));
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.altR)));
        assertTrue("x",
            modifierSet.contains(EnumSet.of(Modifier.altL, Modifier.altR)));
    }

    public void testParseSet_parentMix() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet
            .parseSet("cmd+shift+alt?+caps?");

        assertNotNull("x", modifierSet);
        // 3 (shift) * 4 (alt?) * 2 (caps?) = 24
        assertEquals("x", 24, modifierSet.getVariants().size());
    }

    public void testParseSet_multiple() {
        KeyboardModifierSet modifierSet = KeyboardModifierSet
            .parseSet("optR+caps? cmd+shift");
        assertNotNull("x", modifierSet);
        assertEquals("x", 5, modifierSet.getVariants().size());
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.optR)));
        assertTrue("x",
            modifierSet.contains(EnumSet.of(Modifier.optR, Modifier.caps)));
        assertTrue("x",
            modifierSet.contains(EnumSet.of(Modifier.cmd, Modifier.shiftL)));
        assertTrue("x",
            modifierSet.contains(EnumSet.of(Modifier.cmd, Modifier.shiftR)));
        assertTrue("x", modifierSet.contains(EnumSet.of(Modifier.cmd,
            Modifier.shiftL, Modifier.shiftR)));
    }
}
