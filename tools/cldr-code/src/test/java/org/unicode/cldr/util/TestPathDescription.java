package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class TestPathDescription {
    @Test
    public void testParseGiantString() {
        final PathDescriptionParser parser = new PathDescriptionParser();
        RegexLookup<Pair<String, String>> lookup =
                parser.parse(PathDescription.getPathDescriptionString());
        assertNotNull(lookup);
        assertFalse(lookup.size() == 0, "lookup is empty");
        String references = parser.getReferences();
        assertNotNull(references);
        assertFalse(parser.getReferences().trim().isBlank());
    }
}
