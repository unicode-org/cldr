package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;

import com.ibm.icu.dev.test.TestFmwk;

public class IsoLayoutPositionTest extends TestFmwk {
    public void testForPosition() {
        IsoLayoutPosition position = IsoLayoutPosition.forPosition('C', 6);
        assertEquals("", IsoLayoutPosition.C06, position);
    }

    public void testForPositionWithInvalidRow() {
        try {
            IsoLayoutPosition.forPosition('F', 6);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    public void testForPositionWithInvalidColumn() {
        try {
            IsoLayoutPosition.forPosition('A', 14);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    public void testForPositionWithMissingEntry() {
        try {
            IsoLayoutPosition.forPosition('A', 9);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }

    public void testToStringWithLeadingZero() {
        String string = IsoLayoutPosition.A02.toString();
        assertEquals("", "A02", string);
    }

    public void testIsoLayoutPosition() {
        IsoLayoutPosition position = IsoLayoutPosition.D01;
        assertEquals("", "Q", position.englishKeyName());
        assertEquals("", 'D', position.row());
        assertEquals("", 1, position.column());
    }

    public void testIsoLayoutPositionForNonUsKey() {
        IsoLayoutPosition position = IsoLayoutPosition.B00;
        assertEquals("", "(key to left of Z)", position.englishKeyName());
        assertEquals("", 'B', position.row());
        assertEquals("", 0, position.column());
    }
}
