package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class TestLevel {
    @ParameterizedTest(name = "{index}: min {0},{1}")
    @CsvSource({
        // a, b, expected => min(a, b) == expected
        "UNDETERMINED,UNDETERMINED,UNDETERMINED",
        "BASIC,MODERN,BASIC",
        "BASIC,COMPREHENSIVE,BASIC",
        "MODERN,MODERN,MODERN",
        "MODERN,COMPREHENSIVE,MODERN",
        "COMPREHENSIVE,MODERN,MODERN",
    })
    void testMin(final String a, final String b, final String c) {
        final Level aa = Level.fromString(a);
        final Level bb = Level.fromString(b);
        final Level expect = Level.fromString(c);
        final Level actual = Level.min(aa, bb);
        assertEquals(
                expect,
                actual,
                () -> String.format("Expected Level.min(%s,%s) but was %s", aa, bb, actual));
    }

    @ParameterizedTest(name = "{index}: max {0},{1}")
    @CsvSource({
        // a, b, expected => min(a, b) == expected
        "UNDETERMINED,UNDETERMINED,UNDETERMINED",
        "BASIC,MODERN,MODERN",
        "BASIC,COMPREHENSIVE,COMPREHENSIVE",
        "MODERN,MODERN,MODERN",
        "MODERN,COMPREHENSIVE,COMPREHENSIVE",
        "COMPREHENSIVE,MODERN,COMPREHENSIVE",
    })
    void testMax(final String a, final String b, final String c) {
        final Level aa = Level.fromString(a);
        final Level bb = Level.fromString(b);
        final Level expect = Level.fromString(c);
        final Level actual = Level.max(aa, bb);
        assertEquals(
                expect,
                actual,
                () -> String.format("Expected Level.max(%s,%s) but was %s", aa, bb, actual));
    }

    static SupplementalDataInfo sdi = null;

    @BeforeAll
    public static void setUp() throws Exception {
        sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
    }

    /** walk through all currencies looking for modern ones */
    static final Stream<Arguments> modernCurrencies() {
        final Set<String> all = new TreeSet<String>();

        sdi.getCurrencyTerritories()
                .forEach(
                        (t) -> {
                            sdi.getCurrencyDateInfo(t).stream()
                                    // TODO: should we use RECENT_HISTORY? CLDR-16316
                                    .filter(
                                            di ->
                                                    (di.isLegalTender()
                                                            && (DateConstants.NOW.compareTo(
                                                                            di.getStart())
                                                                    >= 0)
                                                            && (DateConstants.NOW.compareTo(
                                                                            di.getEnd())
                                                                    <= 0)))
                                    .map(di -> di.getCurrency())
                                    .forEach(c -> all.add(c));
                        });
        return all.stream().map(c -> arguments(c));
    }

    @ParameterizedTest()
    @MethodSource("modernCurrencies")
    public void testModernCurrencies(final String code) {
        Level l =
                sdi.getCoverageLevel(
                        String.format(
                                "//ldml/numbers/currencies/currency[@type=\"%s\"]/symbol", code),
                        "und");
        final Level expect = Level.MODERN;
        assertTrue(
                expect.isAtLeast(l),
                () ->
                        String.format(
                                "Coverage for modern currency %s: %s, expected ≤ %s",
                                code, l, expect));
    }

    @Test
    public void TestMath() {
        assertTrue(Level.MODERN.isAbove(Level.MODERATE));
        assertFalse(Level.BASIC.isAtLeast(Level.MODERN));
        assertTrue(Level.MODERN.isAtLeast(Level.BASIC));
    }
}
