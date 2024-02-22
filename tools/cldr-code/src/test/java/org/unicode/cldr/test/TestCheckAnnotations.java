package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestCheckAnnotations {

    @ParameterizedTest
    @ValueSource(
            // all of these should fail
            strings = {
                // Soures as noted.

                // zu
                "E13.1\u2013128",
                "E13.1\u2013127 | iphesenti",
                "ampersand | E13.1\u2013125",
                "E13.1\u2013125",
                "E13.1\u2013136 | Vote E13.1\u2013136",
                "E11:005",
                // sr
                "E13.1\u2013305",
                // ku
                "E416",
            })
    public void TestIllegalAnnotationName(final String value) {
        assertNotNull(CheckAnnotations.hasAnnotationECode(value));
    }
}
