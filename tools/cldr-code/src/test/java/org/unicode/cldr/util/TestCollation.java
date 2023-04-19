package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestCollation {
    static final CLDRConfig config = CLDRConfig.getInstance();

    public static Stream<Arguments> collationLocales() {
        return config.getAllCollationFactory().getAvailable().stream().map(str -> arguments(str));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collationLocales")
    /** Assert no PUA in Collator rules */
    public void TestDefaultCollator(String locale) {
        CLDRFile cldrFile = config.getAllCollationFactory().make(locale, true);
        final String path = "//ldml/collations/defaultCollation";
        final String defaultCollation = cldrFile.getStringValue(path);
        assumeTrue(defaultCollation != null, "null default collation");
        final String collationPath =
                "//ldml/collations/collation[@type=\"" + defaultCollation + "\"]";
        final Set<String> rule = cldrFile.getPaths(collationPath, null, null);
        assertFalse(rule.isEmpty(), locale + ": Could not load defaultCollation " + collationPath);
    }
}
