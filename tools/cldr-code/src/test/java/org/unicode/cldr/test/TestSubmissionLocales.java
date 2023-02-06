package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.unicode.cldr.util.VettingViewer;

public class TestSubmissionLocales {
    @ParameterizedTest
    @CsvFileSource(resources = "/org/unicode/cldr/test/TestSubmissionLocales.csv", numLinesToSkip = 1)
    // Note: the header line is skipped, need to manually verify that the arguments
    // are in sync w/ the CSV
    void TestSubmissionDataDriven(String allowed, String locale,
        String error, String missing, String xpath) {
        assumeTrue(CheckCLDR.LIMITED_SUBMISSION, () -> "Skipping if not LIMITED_SUBMISSION");
        assumeFalse(allowed.charAt(0) == '#'); // skip 'comment' lines
        final boolean isAllowed = allowed.equals("allowed");
        final boolean isError = error.equals("error");
        final boolean isMissing = missing.equals("missing");
        assertEquals(isAllowed, SubmissionLocales.allowEvenIfLimited(locale, xpath, isError, isMissing, VettingViewer.VoteStatus.ok_novotes),
            () -> String.format("%s %s:%s", allowed, locale, xpath));
    }

    @Test
    void TestSubmissionLocale() {
        assumeTrue(CheckCLDR.LIMITED_SUBMISSION, () -> "Skipping if not LIMITED_SUBMISSION");
        assertAll("zh assertions",
            ()->assertTrue(SubmissionLocales.CLDR_LOCALES.contains("zh"), "Expected zh to be a CLDR_LOCALE"),
            ()->assertTrue(SubmissionLocales.CLDR_OR_HIGH_LEVEL_LOCALES.contains("zh"), "Expected zh to be a CLDR_OR_HIGH_LEVEL_LOCALE"),
            ()->assertTrue(SubmissionLocales.TC_ORG_LOCALES.contains("zh"), "Expected zh to be TC_ORG_LOCALES"),
            ()->assertFalse(SubmissionLocales.ALLOW_ALL_PATHS_BASIC.contains("zh"), "Expected zh to NOT be ALLOW_ALL_PATHS_BASIC"),
            ()->assertTrue(SubmissionLocales.LOCALES_FOR_LIMITED.contains("zh"), "Expected zh to be LOCALES_FOR_LIMITED"),
            ()->assertTrue(SubmissionLocales.LOCALES_ALLOWED_IN_LIMITED.contains("zh"), "Expected zh to be LOCALES_ALLOWED_IN_LIMITED"),
            ()->assertTrue(SubmissionLocales.LOCALES_FOR_LIMITED.contains("zh"), "Expected zh to be LOCALES_FOR_LIMITED")
        );
    }

    static public final boolean bool(final String s) {
        return Boolean.parseBoolean(s);
    }
}
