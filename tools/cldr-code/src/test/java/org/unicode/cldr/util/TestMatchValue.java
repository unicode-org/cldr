package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.MatchValue.SemverMatchValue;

public class TestMatchValue {
    @Test
    void TestSemverMatchValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    SemverMatchValue.of("Not Allowed");
                });

        final SemverMatchValue m = SemverMatchValue.of(null);

        ImmutableSet<String> is_semver = ImmutableSet.of("0.0.0", "1.0.0", "42.0.0-BETA3");
        assertAll(
                "is=true",
                is_semver.stream().map(v -> () -> assertTrue(m.is(v), v + ": Should be a semver")));

        ImmutableSet<String> not_semver = ImmutableSet.of("0.0", "v1.0", "Some Version", "");
        assertAll(
                "is=true",
                not_semver.stream()
                        .map(v -> () -> assertFalse(m.is(v), v + ": Should NOT be a semver")));
    }
}
