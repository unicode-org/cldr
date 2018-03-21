package org.unicode.cldr.draft.keyboard.test;

import static org.unicode.cldr.draft.keyboard.IsoLayoutPosition.A03;
import static org.unicode.cldr.draft.keyboard.IsoLayoutPosition.B00;
import static org.unicode.cldr.draft.keyboard.IsoLayoutPosition.B01;
import static org.unicode.cldr.draft.keyboard.IsoLayoutPosition.B04;

import org.unicode.cldr.draft.keyboard.CharacterMap;
import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk;

public class CharacterMapTest extends TestFmwk {

    public void testCharacterMap() {
        ImmutableList<String> longPress = ImmutableList.of("a", "b", "c");
        CharacterMap character = CharacterMap.of(B04, "a", longPress);
        assertEquals("", B04, character.position());
        assertEquals("", "a", character.output());
        assertEquals("", ImmutableList.of("a", "b", "c"), character.longPressKeys());
    }

    public void testCompareTo() {
        CharacterMap character1 = CharacterMap.of(B00, "test1");
        CharacterMap character2 = CharacterMap.of(B01, "test2");
        CharacterMap character3 = CharacterMap.of(A03, "test3");
        CharacterMap character4 = CharacterMap.of(A03, "test4");

        // Between rows (B00 < A03)
        int result = character1.compareTo(character3);
        assertTrue("", result < 0);

        // Between columns (B00 < B01)
        result = character1.compareTo(character2);
        assertTrue("", result < 0);

        // Equal (A03 == A03)
        result = character3.compareTo(character4);
        assertTrue("", result == 0);
    }

    public void testEqualsTrue() {
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.A01, "Output",
            ImmutableList.of("A", "B"));
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.A01, "Output",
            ImmutableList.of("A", "B"));

        assertTrue("", characterMap1.equals(characterMap2));
        assertTrue("", characterMap1.hashCode() == characterMap2.hashCode());
    }

    public void testEqualsFalse() {
        CharacterMap characterMap1 = CharacterMap.of(IsoLayoutPosition.A01, "Output",
            ImmutableList.of("A", "B"));
        CharacterMap characterMap2 = CharacterMap.of(IsoLayoutPosition.A01, "Output");

        assertFalse("", characterMap1.equals(characterMap2));
    }
}
