package org.unicode.cldr.draft.keyboard.test;

import java.util.EnumSet;

import org.unicode.cldr.draft.keyboard.CharacterMap;
import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;
import org.unicode.cldr.draft.keyboard.KeyMap;
import org.unicode.cldr.draft.keyboard.Keyboard;
import org.unicode.cldr.draft.keyboard.KeyboardId;
import org.unicode.cldr.draft.keyboard.KeyboardId.Platform;
import org.unicode.cldr.draft.keyboard.ModifierKey;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombination;
import org.unicode.cldr.draft.keyboard.ModifierKeyCombinationSet;
import org.unicode.cldr.draft.keyboard.Transform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class KeyboardTest extends TestFmwk {
    private Keyboard keyboard = createKeyboard();

    private static Keyboard createKeyboard() {
        // Base key map.
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.<ModifierKey> of());
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination1));
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.E02, "ě");
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.E03, "š",
            ImmutableList.of("ç %"));
        CharacterMap characterMap3 = CharacterMap.of(IsoLayoutPosition.D10, "¨");
        KeyMap keyMap1 = KeyMap.of(combinationSet1,
            ImmutableSet.of(characterMap1, characterMap2, characterMap3));

        // Option key map.
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.OPTION));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION, ModifierKey.SHIFT),
            ImmutableSet.of(ModifierKey.CAPSLOCK));
        ModifierKeyCombinationSet combinationSet2 = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination2, combination3));
        CharacterMap characterMap4 = CharacterMap.of(IsoLayoutPosition.C01, "ä");
        CharacterMap characterMap5 = CharacterMap.of(IsoLayoutPosition.C03, "ß", ImmutableList.of("Ð"));
        CharacterMap characterMap6 = CharacterMap.of(IsoLayoutPosition.C10, "\u0302");
        KeyMap keyMap2 = KeyMap.of(combinationSet2,
            ImmutableSet.of(characterMap4, characterMap5, characterMap6));

        // Transforms.
        ImmutableSortedSet<Transform> transforms = ImmutableSortedSet.of(Transform.of("¨ß", "\uD800\uDE80"),
            Transform.of("¨š", "s"), Transform.of("\u0302ß", "\u0305a"));

        return Keyboard.of(KeyboardId.fromString("cs-t-k0-osx-qwerty"),
            ImmutableList.of("Czech-QWERTY"), ImmutableSortedSet.of(keyMap1, keyMap2), transforms);
    }

    public void testKeyboardId() {
        KeyboardId id = keyboard.keyboardId();
        KeyboardId testId = KeyboardId.of(ULocale.forLanguageTag("cs"), Platform.OSX,
            ImmutableList.of("qwerty"));
        assertEquals("", testId, id);
    }

    public void testNames() {
        ImmutableList<String> names = keyboard.names();
        assertEquals("", ImmutableList.of("Czech-QWERTY"), names);
    }

    public void testKeyMaps() {
        // Base key map.
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.<ModifierKey> of());
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination1));
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.E02, "ě");
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.E03, "š",
            ImmutableList.of("ç %"));
        CharacterMap characterMap3 = CharacterMap.of(IsoLayoutPosition.D10, "¨");
        KeyMap keyMap1 = KeyMap.of(combinationSet1,
            ImmutableSet.of(characterMap1, characterMap2, characterMap3));

        // Option key map.
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.OPTION));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION, ModifierKey.SHIFT),
            ImmutableSet.of(ModifierKey.CAPSLOCK));
        ModifierKeyCombinationSet combinationSet2 = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination2, combination3));
        CharacterMap characterMap4 = CharacterMap.of(IsoLayoutPosition.C01, "ä");
        CharacterMap characterMap5 = CharacterMap.of(IsoLayoutPosition.C03, "ß", ImmutableList.of("Ð"));
        CharacterMap characterMap6 = CharacterMap.of(IsoLayoutPosition.C10, "\u0302");
        KeyMap keyMap2 = KeyMap.of(combinationSet2,
            ImmutableSet.of(characterMap4, characterMap5, characterMap6));

        assertEquals("", ImmutableSet.of(keyMap1, keyMap2), keyboard.keyMaps());
    }

    public void testTransforms() {
        ImmutableSet<Transform> transforms = ImmutableSet.of(Transform.of("¨ß", "\uD800\uDE80"),
            Transform.of("¨š", "s"), Transform.of("\u0302ß", "\u0305a"));
        assertEquals("", transforms, keyboard.transforms());
    }

    public void testEqualsTrue() {
        KeyboardId id = KeyboardId.of(ULocale.forLanguageTag("cs"), Platform.OSX,
            ImmutableList.of("qwerty"));
        // Base key map.
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(ImmutableSet.<ModifierKey> of());
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination1));
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.E02, "ě");
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.E03, "š",
            ImmutableList.of("ç %"));
        CharacterMap characterMap3 = CharacterMap.of(IsoLayoutPosition.D10, "¨");
        KeyMap keyMap1 = KeyMap.of(combinationSet1,
            ImmutableSet.of(characterMap1, characterMap2, characterMap3));
        // Option key map.
        ModifierKeyCombination combination2 = ModifierKeyCombination.ofOnKeys(ImmutableSet.of(ModifierKey.OPTION));
        ModifierKeyCombination combination3 = ModifierKeyCombination.ofOnAndDontCareKeys(
            ImmutableSet.of(ModifierKey.OPTION, ModifierKey.SHIFT),
            ImmutableSet.of(ModifierKey.CAPSLOCK));
        ModifierKeyCombinationSet combinationSet2 = ModifierKeyCombinationSet.of(ImmutableSet.of(
            combination2, combination3));
        CharacterMap characterMap4 = CharacterMap.of(IsoLayoutPosition.C01, "ä");
        CharacterMap characterMap5 = CharacterMap.of(IsoLayoutPosition.C03, "ß", ImmutableList.of("Ð"));
        CharacterMap characterMap6 = CharacterMap.of(IsoLayoutPosition.C10, "\u0302");
        KeyMap keyMap2 = KeyMap.of(combinationSet2,
            ImmutableSet.of(characterMap4, characterMap5, characterMap6));

        // Transforms.
        ImmutableSortedSet<Transform> transforms = ImmutableSortedSet.of(Transform.of("¨ß", "\uD800\uDE80"),
            Transform.of("¨š", "s"), Transform.of("\u0302ß", "\u0305a"));

        Keyboard keyboard2 = Keyboard.of(id, ImmutableList.of("Czech-QWERTY"),
            ImmutableSortedSet.of(keyMap1, keyMap2), transforms);

        assertTrue("", keyboard.equals(keyboard2));
        assertEquals("", keyboard2.hashCode(), keyboard.hashCode());
    }

    public void testEqualsFalse() {
        KeyboardId id = KeyboardId.of(ULocale.forLanguageTag("cs"), Platform.OSX,
            ImmutableList.of("qwerty"));

        // Base key map
        ModifierKeyCombination combination1 = ModifierKeyCombination.ofOnKeys(EnumSet.noneOf(ModifierKey.class));
        ModifierKeyCombinationSet combinationSet1 = ModifierKeyCombinationSet.of(ImmutableSet.of(combination1));
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.E02, "ě");
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.E03, "š",
            ImmutableList.of("ç %"));
        KeyMap keyMap1 = KeyMap.of(combinationSet1, ImmutableSet.of(characterMap1, characterMap2));

        // Transforms
        ImmutableSortedSet<Transform> emptyTransforms = ImmutableSortedSet.of();

        Keyboard keyboard2 = Keyboard.of(id, ImmutableList.of("Czech-QWERTY"),
            ImmutableSortedSet.of(keyMap1), emptyTransforms);

        assertFalse("", keyboard.equals(keyboard2));
        assertNotSame("", keyboard2.hashCode(), keyboard.hashCode());
    }
}
