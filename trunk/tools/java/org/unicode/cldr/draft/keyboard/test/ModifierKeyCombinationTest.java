package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.ModifierKey;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombination;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;

public class ModifierKeyCombinationTest extends TestFmwk {

    public void testOfOnKeysForSimpleCombination() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.SHIFT);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.OPTION,
            ModifierKey.ALT, ModifierKey.COMMAND, ModifierKey.CAPSLOCK);
        assertEquals("", offKeys, combination.offKeys());
        assertEquals("", "shift", combination.toString());
    }

    public void testOfOnKeysForComplexCombination() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.CONTROL, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT,
            ModifierKey.CAPSLOCK, ModifierKey.CONTROL);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.ALT_LEFT, ModifierKey.OPTION,
            ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
        assertEquals("", "ctrl+altR+caps+shift", combination.toString());
    }

    public void testOfOnKeysForComplexCombinationWithVariant() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.SHIFT_RIGHT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.SHIFT_RIGHT,
            ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.ALT_LEFT,
            ModifierKey.OPTION, ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
        assertEquals("", "altR+caps+shiftR+shiftL?", combination.toString());
    }

    public void testOfOnAndDontCareKeysForSimpleCombination() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.COMMAND), ImmutableSet.of(ModifierKey.OPTION_LEFT));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.COMMAND);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.ALT,
            ModifierKey.CONTROL, ModifierKey.OPTION_RIGHT, ModifierKey.SHIFT, ModifierKey.CAPSLOCK);
        assertEquals("", offKeys, combination.offKeys());
        assertEquals("", "cmd+optL?", combination.toString());
    }

    public void testOfOnAndDontCareKeys_complex() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.SHIFT, ModifierKey.CAPSLOCK),
            ImmutableSet.of(ModifierKey.ALT, ModifierKey.COMMAND));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.SHIFT,
            ModifierKey.CAPSLOCK);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.OPTION);
        assertEquals("", offKeys, combination.offKeys());
        assertEquals("", "ctrl+caps+shift+cmd?+alt?", combination.toString());
    }

    public void testOfOnAndDontCareKeysForComplexCombinationWithVariant() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.SHIFT, ModifierKey.CAPSLOCK),
            ImmutableSet.of(ModifierKey.ALT, ModifierKey.ALT_RIGHT, ModifierKey.COMMAND));
        ImmutableSet<ModifierKey> onKeys = ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.SHIFT,
            ModifierKey.CAPSLOCK);
        assertEquals("", onKeys, combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.OPTION, ModifierKey.ALT_LEFT);
        assertEquals("", offKeys, combination.offKeys());
        assertEquals("", "ctrl+caps+shift+cmd?+altR?", combination.toString());
    }

    public void testOfOnAndDontCareKeys_empty() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.<ModifierKey> of(), ImmutableSet.<ModifierKey> of());
        assertEquals("", ImmutableSet.<ModifierKey> of(), combination.onKeys());
        ImmutableSet<ModifierKey> offKeys = ImmutableSet.of(ModifierKey.OPTION, ModifierKey.SHIFT,
            ModifierKey.CONTROL, ModifierKey.ALT, ModifierKey.CAPSLOCK, ModifierKey.COMMAND);
        assertEquals("", offKeys, combination.offKeys());
        assertEquals("", "", combination.toString());
    }

    public void testEqualsTrue() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        assertTrue("", combination1.equals(combination2));
        assertTrue("", combination1.hashCode() == combination2.hashCode());
    }

    public void testEqualsWithDontCareTrue() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK),
            ImmutableSet.of(ModifierKey.COMMAND));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK),
            ImmutableSet.of(ModifierKey.COMMAND));
        assertTrue("", combination1.equals(combination2));
        assertTrue("", combination1.hashCode() == combination2.hashCode());
    }

    public void testEqualsFalse() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.ALT_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.COMMAND));
        assertFalse("", combination1.equals(combination2));
        assertTrue("", combination1.hashCode() != combination2.hashCode());
    }

    public void testEqualsWithDontCareFalse() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.ALT_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.COMMAND));
        assertFalse("", combination1.equals(combination2));
        assertTrue("", combination1.hashCode() != combination2.hashCode());
    }

    public void testCompareToForOnKeys() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.CAPSLOCK));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.OPTION));
        // Expected: [caps, opt]
        assertTrue("", combination1.compareTo(combination2) < 0);
    }

    public void testCompareToForCloseOnKeys() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        // Expected: [altR+caps, altR+caps+shift]
        assertTrue("", combination1.compareTo(combination2) < 0);
    }

    public void testCompareToForComplexOnKeys() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.CONTROL_LEFT, ModifierKey.OPTION));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.CONTROL, ModifierKey.SHIFT, ModifierKey.OPTION));
        // Expected: [ctrlL+opt, ctrl+opt+shift]
        assertTrue("", combination1.compareTo(combination2) < 0);
    }

    public void testCompareToForLeftAndRightVariants() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.CONTROL_LEFT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.CONTROL_RIGHT));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.CONTROL));
        // Expected: [ctrlR, ctrlL, ctrl]
        assertTrue("", combination1.compareTo(combination2) > 0);
        assertTrue("", combination1.compareTo(combination3) < 0);
        assertTrue("", combination2.compareTo(combination3) < 0);
    }

    public void testCompareToForEqualCombinations() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        assertTrue("", combination1.compareTo(combination2) == 0);
    }

    public void testCompareToWithDontCares() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION), ImmutableSet.of(ModifierKey.CONTROL));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION), ImmutableSet.of(ModifierKey.COMMAND));
        // Expected: [opt+ctrl?, opt+cmd?]
        assertTrue("", combination1.compareTo(combination2) < 0);
    }

    public void testCompareToWithCloseDontCares() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT), ImmutableSet.of(ModifierKey.CAPSLOCK));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.ALT_RIGHT));
        // Expected: [altR altR+caps?]
        assertTrue("", combination1.compareTo(combination2) > 0);
    }

    public void testCompareToWithComplexDontCares() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT),
            ImmutableSet.of(ModifierKey.CONTROL_LEFT, ModifierKey.OPTION));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT),
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.SHIFT, ModifierKey.OPTION));
        // Expected: [altR+ctrlL?+opt?, altR+ctrl?+opt?+shift?]
        assertTrue("", combination1.compareTo(combination2) < 0);
    }

    public void testCompareToWithLeftRightVariantDontCares() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.CONTROL_LEFT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.CONTROL_RIGHT));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.CONTROL));
        // Expected: [shift+ctrlR?, shift+ctrlL?, shift+ctrl?]
        assertTrue("", combination1.compareTo(combination2) > 0);
        assertTrue("", combination1.compareTo(combination3) < 0);
        assertTrue("", combination2.compareTo(combination3) < 0);
    }

    public void testCompareToWithEqualDontCares() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.ALT_RIGHT, ModifierKey.CAPSLOCK));
        assertTrue("", combination1.compareTo(combination2) == 0);
    }

    public void testIsBaseCombinationTrue() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.<ModifierKey> of(),
            ImmutableSet.of(ModifierKey.COMMAND, ModifierKey.ALT_LEFT, ModifierKey.CONTROL));
        assertTrue("", combination.isBase());
    }

    public void testIsBaseCombinationFalse() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        assertFalse("", combination.isBase());
    }
}
