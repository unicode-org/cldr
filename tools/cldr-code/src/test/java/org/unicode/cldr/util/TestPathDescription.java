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
                                                e -> {
                                                    return urls.forXpath(locale, e.getValue())
                                                            + " "
                                                            + e.getKey()
                                                            + " "
                                                            + e.getValue();
                                                })
                                        .collect(Collectors.joining("\n")));
    }

    @Test
    void testHints() {
        CLDRConfig config = CLDRConfig.getInstance();
        CLDRFile eng = config.getEnglish();
        PathDescription pathDescriptionFactory =
                new PathDescription(
                        config.getSupplementalDataInfo(),
                        eng,
                        null,
                        null,
                        PathDescription.ErrorHandling.CONTINUE);

        Map<String, String> xpathToHint = TranslationHints.getMap();
        xpathToHint.forEach((path, hint) -> confirmInPD(pathDescriptionFactory, path, hint));
        for (String path : eng) {
            String description = pathDescriptionFactory.getHintRawDescription(path, null);
            String hint = TranslationHints.get(path);
            if (hint != null || description != null) {
                assertEquals(
                        hint,
                        description,
                        "Cycling through English paths, description should match hint for path "
                                + path
                                + "; description "
                                + description
                                + "; hint "
                                + hint);
            }
        }
    }

    private void confirmInPD(PathDescription pathDescriptionFactory, String path, String hint) {
        final String description = pathDescriptionFactory.getHintRawDescription(path, null);
        assertNotNull(description, "Null description for path " + path + "; expected: " + hint);
        assertEquals(
                hint,
                description,
                "Cycling through TranslationHints, description should match hint for path "
                        + path
                        + "; description "
                        + description
                        + "; hint "
                        + hint);
    }
}
