package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.MatchValue.SemverMatchValue;
import org.unicode.cldr.util.MatchValue.ValidityMatchValue;

public class TestMatchValue {
    @Test
    void TestSemverMatchValue() {
        // assert that no keyword is allowed
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

    @Test
    void TestBcp47MatchValue() {
        // assert that no keyword is allowed
        final MatchValue m = ValidityMatchValue.of("bcp47-wellformed");

        ImmutableSet<String> is_bcp47 =
                ImmutableSet.of("und", "mt-Latn", "und-US-u-rg-ustx-tz-uschi");
        assertAll(
                "is=true",
                is_bcp47.stream().map(v -> () -> assertTrue(m.is(v), v + ": Should be bcp47")));

        ImmutableSet<String> not_bcp47 =
                ImmutableSet.of(
                        "de_DE@PREEURO", "en_US_POSIX", "a b c", "a", "aa-BB-CCC-DDDD-EEEEE-u-u-u");
        assertAll(
                "is=true",
                not_bcp47.stream()
                        .map(
                                v ->
                                        () ->
                                                assertFalse(
                                                        m.is(v),
                                                        v + ": Should NOT be a bcp47 version")));
    }
}
