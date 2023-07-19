package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class TestPathDescription {
    @Test
    public void testParseGiantString() {
        assertNotNull(PathDescription.parseLookupString());
    }
}
