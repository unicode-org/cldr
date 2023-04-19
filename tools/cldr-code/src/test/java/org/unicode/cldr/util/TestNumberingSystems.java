package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.tool.GenerateNumberingSystemAliases;

/**
 * @see {@link org.unicode.cldr.unittest.NumberingSystemsTest}
 */
public class TestNumberingSystems {
    @Test
    @DisplayName(value = "Check that all numbering systems are properly in root.xml")
    public void TestRootNumberingSystems() {
        final Map<Pair<String, String>, String> missing =
                GenerateNumberingSystemAliases.getMissingRootNumberingSystems();
        assertTrue(
                missing.isEmpty(),
                () ->
                        "root.xml missing elements (run GenerateNumberingSystemAliases ): "
                                + missing.keySet().stream()
                                        .map(Pair::toString)
                                        .collect(Collectors.joining()));
    }
}
