package org.unicode.cldr.draft.keyboard.test;

import java.util.EnumSet;

import org.unicode.cldr.draft.keyboard.CharacterMap;
import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;
import org.unicode.cldr.draft.keyboard.KeyMap;
import org.unicode.cldr.draft.keyboard.ModifierKey;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombination;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombinationSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;

public class KeyMapTest extends TestFmwk {
    private KeyMap keyMap = createKeyMap();

    private static KeyMap createKeyMap() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL), ImmutableSet.of(ModifierKey.OPTION_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.SHIFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.C03, "A",
            ImmutableList.of("$", "%", "\uD800\uDE80"));
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.C02, "S");
        return KeyMap.of(combinationSet, ImmutableSet.of(characterMap1, characterMap2));
    }

    public void testModifierKeyCombinationSet() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.CONTROL), EnumSet.of(ModifierKey.OPTION_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(EnumSet.of(ModifierKey.SHIFT));
        ModifierKeyCombinationSet combinationSetTest = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        assertEquals("", combinationSetTest, keyMap.modifierKeyCombinationSet());
    }

    public void testIsoLayoutToCharacterMap() {
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.C03, "A",
            ImmutableList.of("$", "%", "\uD800\uDE80"));
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.C02, "S");
        ImmutableMap<IsoLayoutPosition, CharacterMap> isoLayoutToCharacterMap = ImmutableMap.of(
            IsoLayoutPosition.C03, characterMap1, IsoLayoutPosition.C02, characterMap2);
        assertEquals("", isoLayoutToCharacterMap, keyMap.isoLayoutToCharacterMap());
    }

    public void testEqualsFalse() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL), ImmutableSet.of(ModifierKey.OPTION_RIGHT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(combination1));
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.C03, "B",
            ImmutableList.of("$", "%"));
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.C02, "W");
        KeyMap keyMapTest = KeyMap.of(combinationSet, ImmutableSet.of(characterMap1, characterMap2));
        assertFalse("", keyMap.equals(keyMapTest));
        assertNotSame("", keyMapTest.hashCode(), keyMap.hashCode());
    }

    public void testEqualsTrue() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.CONTROL), ImmutableSet.of(ModifierKey.OPTION_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(EnumSet.of(ModifierKey.SHIFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.C03, "A",
            ImmutableList.of("$", "%", "\uD800\uDE80"));
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.C02, "S");
        KeyMap keyMapTest = KeyMap.of(combinationSet, ImmutableSet.of(characterMap1, characterMap2));
        assertTrue("", keyMap.equals(keyMapTest));
        assertEquals("", keyMapTest.hashCode(), keyMap.hashCode());
    }

    public void testCompareToGreater() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(EnumSet.noneOf(ModifierKey.class));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(combination));
        KeyMap keyMapTest = KeyMap.of(combinationSet, ImmutableSet.<CharacterMap> of());
        assertTrue("", keyMap.compareTo(keyMapTest) > 0);
    }

    public void testCompareToLess() {
        ModifierKeyCombination combination = ModifierKeyCombination.ofOnKeys(EnumSet.of(ModifierKey.COMMAND));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(combination));
        KeyMap keyMapTest = KeyMap.of(combinationSet, ImmutableSet.<CharacterMap> of());
        assertTrue("", keyMap.compareTo(keyMapTest) < 0);
    }

    public void testCompareTo_equals() {
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnAndDontCareKeys(
            EnumSet.of(ModifierKey.CONTROL), EnumSet.of(ModifierKey.OPTION_RIGHT));
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(EnumSet.of(ModifierKey.SHIFT));
        ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination1, combination2));
        KeyMap keyMapTest = KeyMap.of(combinationSet, ImmutableSet.<CharacterMap> of());
        assertEquals("", 0, keyMap.compareTo(keyMapTest));
    }
}
