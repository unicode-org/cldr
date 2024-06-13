package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.unicode.cldr.util.CLDRLocale;

public class TestSubmissionLocales {
    @ParameterizedTest
    @CsvFileSource(
            resources = "/org/unicode/cldr/test/TestSubmissionLocales.csv",
            numLinesToSkip = 1)
    // Note: the header line is skipped, need to manually verify that the arguments
    // are in sync w/ the CSV
    void TestSubmissionDataDriven(
            String allowed, String locale, String error, String missing, String xpath) {
        assumeTrue(CheckCLDR.LIMITED_SUBMISSION, () -> "Skipping if not LIMITED_SUBMISSION");
        assumeFalse(allowed.charAt(0) == '#'); // skip 'comment' lines
        final boolean isAllowed = allowed.equals("allowed");
        final boolean isError = error.equals("error");
        final boolean isMissing = missing.equals("missing");
        assertEquals(
                isAllowed,
                SubmissionLocales.allowEvenIfLimited(locale, xpath, isError, isMissing),
                () -> String.format("%s %s:%s", allowed, locale, xpath));
    }

    @Test
    void TestSubmissionLocale() {
        assumeTrue(CheckCLDR.LIMITED_SUBMISSION, () -> "Skipping if not LIMITED_SUBMISSION");
        assertAll(
                "zh assertions",
                () ->
                        assertTrue(
                                SubmissionLocales.CLDR_LOCALES.contains("zh"),
                                "Expected zh to be a CLDR_LOCALE"),
                () ->
                        assertTrue(
                                SubmissionLocales.CLDR_OR_HIGH_LEVEL_LOCALES.contains("zh"),
                                "Expected zh to be a CLDR_OR_HIGH_LEVEL_LOCALE"),
                () ->
                        assertTrue(
                                SubmissionLocales.TC_ORG_LOCALES.contains("zh"),
                                "Expected zh to be TC_ORG_LOCALES"),
                () ->
                        assertFalse(
                                SubmissionLocales.ALLOW_ALL_PATHS_BASIC.contains("zh"),
                                "Expected zh to NOT be ALLOW_ALL_PATHS_BASIC"),
                () ->
                        assertTrue(
                                SubmissionLocales.LOCALES_FOR_LIMITED.contains("zh"),
                                "Expected zh to be LOCALES_FOR_LIMITED"),
                () ->
                        assertTrue(
                                SubmissionLocales.LOCALES_ALLOWED_IN_LIMITED.contains("zh"),
                                "Expected zh to be LOCALES_ALLOWED_IN_LIMITED"),
                () ->
                        assertTrue(
                                SubmissionLocales.LOCALES_FOR_LIMITED.contains("zh"),
                                "Expected zh to be LOCALES_FOR_LIMITED"));
    }

    public static final boolean bool(final String s) {
        return Boolean.parseBoolean(s);
    }

    @ParameterizedTest
    @CsvSource({
        // loc, isTc
        "root, true",
        "de, true",
        "de_IT, true",
        "csw, false",
        "csw_CA, false",
        "cho_US, false",
        "zh, true",
        "zh_Hant_MO, true",
        "smj, false",
        "smj_NO, false",
        "smj_SE, false",
    })
    public void testIsTcLocale(final String loc, final String tf) {
        final CLDRLocale l = CLDRLocale.getInstance(loc);
        assertNotNull(l, loc);
        final Boolean isCldr = Boolean.parseBoolean(tf);
        assertEquals(isCldr, SubmissionLocales.isTcLocale(l));
    }

    @ParameterizedTest
    @CsvSource({
        // loc, isExtended
        "root, false",
        "de, false",
        "de_IT, false",
        "csw, true",
        "csw_CA, true",
        "cho_US, true",
        "zh, false",
        "zh_Hant_MO, false",
        "smj, true",
        "smj_NO, true",
        "smj_SE, true",
    })
    public void testIsExtendedSubmissionLocale(final String loc, final String tf) {
        final CLDRLocale l = CLDRLocale.getInstance(loc);
        assertNotNull(l, loc);
        final Boolean isExtendedSubmission = Boolean.parseBoolean(tf);
        assertEquals(isExtendedSubmission, SubmissionLocales.isOpenForExtendedSubmission(l));
    }

    @Test
    public void textAdditionalVsTcLocale() {
        final List<CLDRLocale> extendedSubmissionButNotTcLocales =
                SubmissionLocales.ADDITIONAL_EXTENDED_SUBMISSION.stream()
                        .map(l -> CLDRLocale.getInstance(l))
                        .filter(l -> !SubmissionLocales.isTcLocale(l))
                        .collect(Collectors.toList());
        assertTrue(
                extendedSubmissionButNotTcLocales.isEmpty(),
                () ->
                        "Locales in SubmissionLocales.ADDITIONAL_EXTENDED_SUBMISSION that should be removed as they are not TC locales: "
                                + String.join(
                                        " ",
                                        extendedSubmissionButNotTcLocales.stream()
                                                .map(l -> l.getBaseName())
                                                .collect(Collectors.toList())));
    }
}
