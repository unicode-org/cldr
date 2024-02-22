package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class TestPathDescription {
    @Test
    public void testParseGiantString() {
        RegexLookup<String> l = PathDescription.parseLookupString();
        assertNotNull(l);
    }
}
