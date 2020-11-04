package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.ModifierKey;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombination;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombinationSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.ibm.icu.dev.test.TestFmwk;

public class ModifierKeyCombinationSetTest extends TestFmwk {

    public void testOfWithSingleCombination() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.<ModifierKey> of());
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(combination));
        assertEquals("", ImmutableSortedSet.of(combination), combinationSet.combinations());
        assertEquals("", "", combinationSet.toString());
    }

    public void testOfWithMultipleCombinations() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.CONTROL));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION_RIGHT, ModifierKey.CONTROL),
            ImmutableSet.of(ModifierKey.COMMAND, ModifierKey.SHIFT_LEFT));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL_LEFT, ModifierKey.CONTROL_RIGHT, ModifierKey.CAPSLOCK),
            ImmutableSet.of(ModifierKey.ALT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2, combination3));
        assertEquals("", ImmutableSortedSet.of(combination1, combination2, combination3),
            combinationSet.combinations());
        assertEquals("", "ctrlL+ctrlR+caps+alt? ctrl+optR+cmd?+shiftL? cmd+shift+ctrl?",
            combinationSet.toString());
    }

    public void testOfWithSimplification() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT, ModifierKey.COMMAND, ModifierKey.SHIFT_LEFT),
            ImmutableSet.of(ModifierKey.SHIFT_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        ModifierKeyCombination simplifiedCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.SHIFT));
        assertEquals("", ImmutableSortedSet.of(simplifiedCombination), combinationSet.combinations());
        assertEquals("", "cmd+altR+shift?", combinationSet.toString());
    }

    public void testOfWithMultipleSimplifications() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT, ModifierKey.COMMAND, ModifierKey.SHIFT_LEFT),
            ImmutableSet.of(ModifierKey.SHIFT_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.CONTROL, ModifierKey.ALT_LEFT));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombination combination4 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.CONTROL, ModifierKey.ALT_LEFT, ModifierKey.ALT_RIGHT));
        ModifierKeyCombination combination5 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.CAPSLOCK));
        ImmutableSet<ModifierKeyCombination> combinations = ImmutableSet.of(combination1, combination2,
            combination3, combination4, combination5);
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(combinations);
        ModifierKeyCombination simplifiedCombination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.ALT_RIGHT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombination simplifiedCombination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL, ModifierKey.ALT_LEFT),
            ImmutableSet.of(ModifierKey.ALT_RIGHT));
        assertEquals("",
            ImmutableSortedSet.of(simplifiedCombination1, simplifiedCombination2, combination5),
            combinationSet.combinations());
        assertEquals("", "caps ctrl+altL+altR? cmd+altR+shift?", combinationSet.toString());
    }

    public void testOfWithCascadingSimplifications() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.OPTION_LEFT, ModifierKey.OPTION_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.OPTION_RIGHT));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.OPTION_LEFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2, combination3));
        ModifierKeyCombination simplifiedCombination = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.OPTION));
        assertEquals("", ImmutableSortedSet.of(simplifiedCombination), combinationSet.combinations());
        assertEquals("", "shift+opt?", combinationSet.toString());
    }

    public void testCombine() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.CONTROL));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION_RIGHT, ModifierKey.CONTROL),
            ImmutableSet.of(ModifierKey.COMMAND, ModifierKey.SHIFT_LEFT));
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CAPSLOCK, ModifierKey.OPTION),
            ImmutableSet.of(ModifierKey.CONTROL));
        ModifierKeyCombinationSet combinationSet2 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination3));
        ModifierKeyCombinationSet finalSet = ModifierKeyCombinationSet.combine(Lists.newArrayList(
            combinationSet1, combinationSet2));
        assertEquals("", ImmutableSortedSet.of(combination1, combination2, combination3),
            finalSet.combinations());
        assertEquals("", "opt+caps+ctrl? ctrl+optR+cmd?+shiftL? cmd+shift+ctrl?", finalSet.toString());
    }

    public void testCompareToWithSimpleCombinations() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination1));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.COMMAND));
        ModifierKeyCombinationSet combinationSet2 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination2));
        assertTrue("", combinationSet1.compareTo(combinationSet2) < 0);
    }

    public void testCompareToWithComplexCombinations() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.CONTROL));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CAPSLOCK, ModifierKey.SHIFT_RIGHT),
            ImmutableSet.of(ModifierKey.ALT));
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION_RIGHT, ModifierKey.CONTROL),
            ImmutableSet.of(ModifierKey.COMMAND, ModifierKey.SHIFT_LEFT));
        ModifierKeyCombinationSet combinationSet2 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination3));
        assertTrue("", combinationSet1.compareTo(combinationSet2) < 0);
    }

    public void testCompareToWithDifferentSizeCombinations() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.CONTROL));
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination1));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.COMMAND, ModifierKey.SHIFT_RIGHT),
            ImmutableSet.of(ModifierKey.ALT));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT), ImmutableSet.of(ModifierKey.CONTROL));
        ModifierKeyCombinationSet combinationSet2 = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination2, combination3));
        assertTrue("", combinationSet1.compareTo(combinationSet2) < 0);
    }

    public void testIsBaseForSimpleCombinations() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(ImmutableSet.<ModifierKey> of());
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(combination));
        assertTrue("", combinationSet.isBase());
    }

    public void testIsBaseForComplexCombinations() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.<ModifierKey> of(),
            ImmutableSet.of(ModifierKey.COMMAND, ModifierKey.ALT_LEFT, ModifierKey.CONTROL));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.ALT_LEFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        assertTrue("", combinationSet.isBase());
    }

    public void testIsBaseFalse() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.ALT_LEFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        assertFalse("", combinationSet.isBase());
    }

    public void testEqualsTrue() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.ALT_LEFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        ModifierKeyCombination combinationTest1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombination combinationTest2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.ALT_LEFT));
        ModifierKeyCombinationSet combinationSetTest = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combinationTest1, combinationTest2));
        assertTrue("", combinationSet.equals(combinationSetTest));
        assertEquals("", combinationSet.hashCode(), combinationSetTest.hashCode());
    }

    public void testEqualsFalse() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(
            ModifierKey.SHIFT, ModifierKey.CAPSLOCK));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.ALT_LEFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        ModifierKeyCombination combinationTest1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombination combinationTest2 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.SHIFT, ModifierKey.COMMAND),
            ImmutableSet.of(ModifierKey.ALT_LEFT));
        ModifierKeyCombinationSet combinationSetTest = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combinationTest1, combinationTest2));
        assertFalse("", combinationSet.equals(combinationSetTest));
        assertNotSame("", combinationSet.hashCode(), combinationSetTest.hashCode());
    }
}
