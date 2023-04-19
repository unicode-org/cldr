package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}
