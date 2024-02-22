package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @see {@link CheckMetazones}
 */
public class TestCheckMetazones {
    @Test
    public void TestMetazoneUsesDST() {
        assertFalse(CheckMetazones.metazoneUsesDST("Yukon"), "Yukon doesn't use DST");
        assertTrue(CheckMetazones.metazoneUsesDST("America_Pacific"), "America_Pacific uses DST");
    }
}
