package org.unicode.cldr.draft.keyboard.test;

import org.unicode.cldr.draft.keyboard.IsoLayoutPosition;
import org.unicode.cldr.draft.keyboard.KeycodeMap;

import com.ibm.icu.dev.test.TestFmwk;

public class KeycodeMapTest extends TestFmwk {
    public void testGetIsoLayoutPosition() {
        String csv = "keycode,iso\n50,E03\n80,D10\n4,B00";
        KeycodeMap mapping = KeycodeMap.fromCsv(csv);
        assertEquals("", IsoLayoutPosition.E03, mapping.getIsoLayoutPosition(50));
    }

    public void testGetIsoLayoutPositionForMissingValue() {
        String csv = "keycode,iso\n50,E03\n80,D10\n4,B00";
        KeycodeMap mapping = KeycodeMap.fromCsv(csv);
        try {
            mapping.getIsoLayoutPosition(100);
            fail();
        } catch (NullPointerException e) {
            // Expected behavior
        }
    }

    public void testFromCsvMissingHeaders() {
        String csv = "50,E03\n4,B00";
        try {
            KeycodeMap.fromCsv(csv);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }
}
