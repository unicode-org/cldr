package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Collections;

import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.Test;

public class LocaleSetTest {
    @Test
    public void testToString() {
        final String[] ALL_LOCALES = { "*" };
        assertArrayEquals(ALL_LOCALES, new LocaleSet(true).toStringArray(), "for all-locales set");

        final String[] NO_LOCALES = {};
        assertArrayEquals(NO_LOCALES, new LocaleSet().toStringArray(), "for no-locales set");
        assertArrayEquals(NO_LOCALES, new LocaleSet(Collections.emptySet()).toStringArray(), "for empty set");

        final String[] ONE_LOCALE = { "tlh" };
        assertArrayEquals(ONE_LOCALE, new LocaleSet(ImmutableSet.of("tlh")).toStringArray(), "for one-locale set");
    }
}
