package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertFalse(parser.getReferences().trim().isEmpty());

        // To print out the regex tree:
        // System.out.println(lookup.toString());
        assertFalse(
                parser.getReferences().contains("#h."),
                "PathDescriptions.md refers to broken anchor #h.… (old Google Sites)");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // xpath
                "//ldml/localeDisplayNames/languages/language[@type=\"ml\"]",
                "//ldml/dates/timeZoneNames/zone[@type=\"Asia/Kuching\"]/exemplarCity"
            })
    public void testPresent(final String xpath) {
        assertNotNull(PathDescription.getPathHandling().get(xpath), () -> xpath);
    }
}
