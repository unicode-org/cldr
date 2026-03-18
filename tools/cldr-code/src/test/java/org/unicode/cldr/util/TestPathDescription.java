package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
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
    void testNoPlaceholders() {
        CLDRConfig config = CLDRConfig.getInstance();
        PathDescription pathDescriptionFactory =
                new PathDescription(
                        config.getSupplementalDataInfo(),
                        config.getEnglish(),
                        null,
                        null,
                        PathDescription.ErrorHandling.CONTINUE);

        final String locale = "fa";
        final CLDRFile file = config.getCLDRFile(locale, true); // Farsi has extra relative dates
        Map<String, String> xpathsWithPlaceholders =
                new TreeMap<>(); // map from value to xpath - so that we compact the results
        for (final String xpath : file.fullIterable()) {
            final String description =
                    pathDescriptionFactory.getDescription(xpath, file.getWinningValue(xpath), null);
            if (description == null) continue;
            if (description.contains("{")
                    && description
                            .replaceAll("\\{0\\} and \\{1\\} in the pattern", "")
                            .contains("{")) {
                // we exclude one particular case
                xpathsWithPlaceholders.put(description.split("\n")[0] + "…", xpath);
            }
        }
        CLDRURLS urls = CLDRConfig.getInstance().urls();
        assertTrue(
                xpathsWithPlaceholders.isEmpty(),
                () ->
                        "Remaining placeholders (sampling of xpaths) from PathDescriptions.md:\n"
                                + xpathsWithPlaceholders.entrySet().stream()
                                        .map(
                                                e ->
                                                        urls.forXpath(locale, e.getValue())
                                                                + " "
                                                                + e.getKey()
                                                                + " "
                                                                + e.getValue())
                                        .collect(Collectors.joining("\n")));
    }
}
