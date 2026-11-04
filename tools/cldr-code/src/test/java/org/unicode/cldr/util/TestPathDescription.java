package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestPathDescription {
    @Test
    public void testParseGiantString() {
        parseOneString(PathDescription.pathDescriptionFileName);
        parseOneString(PathDescription.pathDescriptionHintsFileName);
    }

    private void parseOneString(String fileName) {
        final String bigString = PathDescription.getBigString(fileName);
        final PathDescriptionParser parser = new PathDescriptionParser();
        final RegexLookup<Pair<String, String>> lookup = parser.parse(fileName);

        assertNotNull(lookup);
        assertNotEquals(0, lookup.size(), "lookup is empty");

        String references = parser.getReferences();
        assertNotNull(references);
        assertFalse(references.trim().isEmpty());
        // To print out the regex tree:
        // System.out.println(lookup.toString());
        assertFalse(
                references.contains("#h."),
                fileName + " refers to broken anchor #h.… (old Google Sites)");

        String badLine = containsHttpWithoutLeftBracket(bigString);
        assertNull(
                badLine,
                fileName
                        + " contains http in line that does not start with left bracket: "
                        + badLine);
    }

    private String containsHttpWithoutLeftBracket(String s) {
        for (String line : s.split("\\R")) {
            if (!line.isBlank() && line.charAt(0) != '[' && line.contains("http")) {
                return line;
            }
        }
        return null;
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // xpath
                "//ldml/localeDisplayNames/languages/language[@type=\"ml\"]",
                "//ldml/dates/timeZoneNames/zone[@type=\"Asia/Kuching\"]/exemplarCity"
            })
    public void testPresent(final String xpath) {
        assertNotNull(PathDescription.getPathHandling().get(xpath), xpath);
    }

    @Test
    public void testDollarFormat() {
        assertEquals("The same.", PathDescription.formatWithDollarSub("The same."));
        assertEquals(
                "For furloughs per fortnight, use {0}",
                PathDescription.formatWithDollarSub(
                        "For $0 per $1, use {0}", "furloughs", "fortnight"));
    }
}
