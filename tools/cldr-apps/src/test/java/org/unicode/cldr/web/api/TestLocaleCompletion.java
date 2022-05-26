package org.unicode.cldr.web.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.unittest.web.TestAll;
import org.unicode.cldr.unittest.web.TestSTFactory;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.SandboxLocales;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.api.LocaleCompletion.LocaleCompletionResponse;

public class TestLocaleCompletion {
    /**
     * Locales to use. We will start with a blank slate.
     */
    private static final String TEST_LOCALE_A = "es";
    private static final String TEST_LOCALE_B = "fr";

    /**
     * Baseline number of items missing in the above locales. This is sensitive to data changes.
     */
    final int STILL_MISSING_TEST_LOCALE_A = 42;
    final int STILL_MISSING_TEST_LOCALE_B = 30;

    /**
     * Baseline number of items provisional in the above locales. This is sensitive to data changes.
     */
    final int STILL_PROVISIONAL_LOCALE_A = 0;
    final int STILL_PROVISIONAL_LOCALE_B = 6;

    /**
     * Sanity check amount: Assume at least these many paths are present.
     */
    final private int LOCALE_SIZE = 5;

    @Test
    void testLocaleCompletion() throws SQLException {

        TestAll.setupTestDb();
        // initial setup
        final STFactory aFactory = TestSTFactory.createFactory();
        final SurveyMain sm = aFactory.sm;
        final TestCache tcA = new TestCache();
        final TestCache tcB = new TestCache();
        final CLDRConfig config = CLDRConfig.getInstance();
        final CLDRFile english = config.getEnglish();
        final CLDRFile sourceFileUnresolvedA = config.getCLDRFile(TEST_LOCALE_A, false);
        final CLDRFile sourceFileUnresolvedB = config.getCLDRFile(TEST_LOCALE_B, false);
        CheckCLDR.setDisplayInformation(english);

        final CLDRLocale localeA = CLDRLocale.getInstance(TEST_LOCALE_A);
        final CLDRLocale localeB = CLDRLocale.getInstance(TEST_LOCALE_B);
        final SandboxLocales.ScratchXMLSource localeSourceA = new SandboxLocales.ScratchXMLSource(localeA.getBaseName());
        final SandboxLocales.ScratchXMLSource localeSourceB = new SandboxLocales.ScratchXMLSource(localeB.getBaseName());
        final CLDRFile localeFileA = new CLDRFile(localeSourceA);
        final CLDRFile localeFileB = new CLDRFile(localeSourceB);
        final SandboxLocales.ScratchXMLSource rootSource = new SandboxLocales.ScratchXMLSource("root");
        final CLDRFile rootFile = new CLDRFile(rootSource);
        final Factory sandboxFactoryA = getFactory(english, localeSourceA, localeFileA, rootSource, rootFile);
        final Factory sandboxFactoryB = getFactory(english, localeSourceB, localeFileB, rootSource, rootFile);
        tcA.setFactory(sandboxFactoryA, "(?!.*(CheckCoverage).*).*");
        tcB.setFactory(sandboxFactoryB, "(?!.*(CheckCoverage).*).*");

        STFactory stfA = getSTFactory(sm, tcA, sandboxFactoryA);
        STFactory stfB = getSTFactory(sm, tcB, sandboxFactoryB);

        preloadLocaleFile(sourceFileUnresolvedA, localeFileA);
        preloadLocaleFile(sourceFileUnresolvedB, localeFileB);

        testCompletePaths(tcA, localeA, localeFileA, stfA);
        testIncompletePaths(tcB, localeB, localeFileB, stfB);
    }

    /**
     * Test with less than 100% completion
     * @param tc
     * @param locale
     * @param localeFile
     * @param stf
     * @throws MultipleFailuresError
     */
    private void testIncompletePaths(final TestCache tc, final CLDRLocale locale, final CLDRFile localeFile, STFactory stf) throws MultipleFailuresError {
        // ** Step two.
        setIncompletePaths(localeFile);
        tc.invalidateAllCached();
        final int INTENTIONAL_ERROR = 2;
        final int INTENTIONAL_MISSING = 2;
        final int INTENTIONAL_PROVISIONAL = 0;

        final int EXPECT_ERROR = INTENTIONAL_ERROR;
        final int EXPECT_MISSING = INTENTIONAL_MISSING + STILL_MISSING_TEST_LOCALE_B;
        final int EXPECT_PROVISIONAL = INTENTIONAL_PROVISIONAL + STILL_PROVISIONAL_LOCALE_B;

        assertFalse(CheckCLDR.LIMITED_SUBMISSION, "This test becomes invalid if LIMITED_SUBMISSION (or SubmissionLocales) changes.");

        {
            LocaleCompletionResponse completion = LocaleCompletion.handleGetLocaleCompletion(locale, stf);
            final int expected_votes = completion.total - (EXPECT_ERROR + EXPECT_MISSING + EXPECT_PROVISIONAL);
            assertNotNull(completion);
            assertAll("tests on completion - not quite 100%",
                () -> assertEquals(Level.MODERN.name(), completion.level, "completion level"),
                () -> assertTrue(completion.votes > LOCALE_SIZE, "completion votes should be bigger but was " + completion.votes),
                () -> assertTrue(completion.votes < completion.total,
                    "completion votes should be less than completion total but was  " + completion.votes + "/" + completion.total),
                () -> assertEquals(expected_votes, completion.votes,
                    "completion votes was not as expected"),
                () -> assertTrue(completion.total > LOCALE_SIZE, "completion total should be bigger but was " + completion.total),
                () -> assertEquals(EXPECT_ERROR, completion.error, "error count was " + completion.error),
                () -> assertEquals(EXPECT_MISSING, completion.missing, "missing count was " + completion.missing),
                () -> assertEquals(EXPECT_PROVISIONAL, completion.provisional, "provisional count was " + completion.provisional));
        }
    }

    /**
     * Add some paths that would lead to less than 100% completion
     * @param localeFile
     */
    private void setIncompletePaths(final CLDRFile localeFile) {
        String path, value;

        // INTENTIONAL_ERROR = 2;
        path = "//ldml/dates/timeZoneNames/metazone[@type=\"Arabian\"]/long/generic";
        value = " ";
        localeFile.add(path, value);
        assertEquals(value, localeFile.getWinningValue(path), "setIncompletePaths error 1");

        path = "//ldml/characters/exemplarCharacters";
        value = "[]";
        localeFile.add(path, value);
        assertEquals(value, localeFile.getWinningValue(path), "setIncompletePaths error 2");

        // INTENTIONAL_MISSING = 2;
        path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]";
        localeFile.remove(path);
        assertEquals(null, localeFile.getWinningValue(path), "setIncompletePaths missing 1");

        path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"2\"]";
        localeFile.remove(path);
        assertEquals(null, localeFile.getWinningValue(path), "setIncompletePaths missing 2");

        // INTENTIONAL_PROVISIONAL = 0
        // none of the following works for making items provisional; instead, getStatusForOrganization
        // sees winningStatus = VoteResolver.Status.approved, and returns VoteStatus.ok_novotes rather than VoteStatus.provisionalOrWorse
        /*
        localeFile.remove("//ldml/dates/timeZoneNames/zone[@type=\"Asia/Qyzylorda\"]/exemplarCity");
        localeFile.remove("//ldml/dates/timeZoneNames/zone[@type=\"Pacific/Kanton\"]/exemplarCity");

        path = "//ldml/dates/timeZoneNames/zone[@type=\"Asia/Qyzylorda\"]/exemplarCity[@draft=\"provisional\"]";
        // path = "//ldml/dates/timeZoneNames/zone[@type=\"Asia/Qyzylorda\"]/exemplarCity";
        value = "cabaca";
        localeFile.add(path, value);
        assertEquals(value, localeFile.getWinningValue(path), "setIncompletePaths provisional 1");

        path = "//ldml/dates/timeZoneNames/zone[@type=\"Pacific/Kanton\"]/exemplarCity[@draft=\"provisional\"]";
        // path = "//ldml/dates/timeZoneNames/zone[@type=\"Pacific/Kanton\"]/exemplarCity";
        value = "bacaba";
        localeFile.add(path, value);
        assertEquals(value, localeFile.getWinningValue(path), "setIncompletePaths provisional 2");
        */
    }

    /**
     * Test with 100% completion
     * @param tc
     * @param locale
     * @param localeFile
     * @param stf
     * @throws MultipleFailuresError
     */
    private void testCompletePaths(final TestCache tc, final CLDRLocale locale, final CLDRFile localeFile, STFactory stf) throws MultipleFailuresError {
        // ** Step one.  Set up the locale
        // Expected 100%
        setCompletePaths(localeFile);

        tc.invalidateAllCached();

        {
            LocaleCompletionResponse completion = LocaleCompletion.handleGetLocaleCompletion(locale, stf);

            assertNotNull(completion);
            assertAll("tests on completion - 100%",
                () -> assertEquals(Level.MODERN.name(), completion.level, "completion level"),
                () -> assertTrue(completion.votes > LOCALE_SIZE, "completion votes should be bigger but was " + completion.votes),
                () -> assertEquals(completion.total - STILL_MISSING_TEST_LOCALE_A, completion.votes,
                    "completion votes should be equal completion total but was  " + completion.votes + "/" + completion.total),
                () -> assertTrue(completion.total > LOCALE_SIZE, "completion total should be bigger but was " + completion.total),
                () -> assertEquals(0, completion.error, "error"),
                () -> assertEquals(STILL_MISSING_TEST_LOCALE_A, completion.missing, "missing"),
                () -> assertEquals(STILL_PROVISIONAL_LOCALE_A, completion.provisional, "provisional"));
        }
    }

    /**
     * Add some paths that would give 100% locale completion
     * @param localeFile
     */
    private void setCompletePaths(final CLDRFile localeFile) {
        localeFile.add("//ldml/characters/exemplarCharacters", "[abc]");
        localeFile.add("//ldml/numbers/symbols[@numberSystem=\"latn\"]/group",
            ","); // ok
        localeFile.add("//ldml/localeDisplayNames/languages/language[@type=\"" + localeFile.getLocaleID() + "\"]",
            "baccacab"); // for fallback (need to use only 'a,b,c')

        localeFile.add("//ldml/numbers/currencies/currency[@type=\"XTS\"]/displayName[@draft=\"provisional\"]",
            "Test Currency (comprehensive level, should not have an effect on completion)");
        localeFile.add("//ldml/numbers/currencies/currency[@type=\"XUA\"]/displayName[@draft=\"unconfirmed\"]",
            "Other Test Currency (comprehensive level, should not have an effect on completion)");
    }

    /**
     * Load some initial paths (with exclusions) from a disk file
     * @param sourceFileUnresolved the file from which to get the data to preload
     * @param localeFile the file to receive the data
     */
    private void preloadLocaleFile(final CLDRFile sourceFileUnresolved, final CLDRFile localeFile) {
        // Preload some stuff
        for (final String xpath : sourceFileUnresolved) {
            // Skip some problematic paths.
            if (xpath.startsWith("//ldml/characters/parseLenients") ||
                xpath.startsWith("//ldml/numbers/currency") ||
                xpath.startsWith("//ldml/numbers/scientific") ||
                xpath.startsWith("//ldml/numbers/decimal") ||
                xpath.startsWith("//ldml/numbers/percent")) {
                continue;
            }
            String value = sourceFileUnresolved.getWinningValue(xpath);
            localeFile.add(xpath, value);
        }
    }

    /**
     * New up a STFactory
     * @param sm
     * @param tc
     * @param sandboxFactory
     * @return
     */
    private STFactory getSTFactory(SurveyMain sm, final TestCache tc, final Factory sandboxFactory) {
        return new STFactory(sm) {
            @Override
            public TestResultBundle getTestResult(CLDRLocale loc, CheckCLDR.Options options) {
                return tc.getBundle(options);
            }

            @Override
            public CLDRFile make(String locale, boolean resolved) {
                return sandboxFactory.make(locale, resolved); // pass through
            }
        };
    }

    /**
     * New up a Factory
     * @param english
     * @param localeSource
     * @param localeFile
     * @param rootSource
     * @param rootFile
     * @return
     */
    private Factory getFactory(final CLDRFile english, final SandboxLocales.ScratchXMLSource localeSource, final CLDRFile localeFile,
        final SandboxLocales.ScratchXMLSource rootSource, final CLDRFile rootFile) {
        return new Factory() {

            @Override
            public File getSupplementalDirectory() {
                return english.getSupplementalDirectory();
            }

            @Override
            public File[] getSourceDirectories() {
                return new File[]{ new File(CLDRPaths.MAIN_DIRECTORY) };
            }

            @Override
            protected CLDRFile handleMake(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
                if (resolved) {
                    if (localeID.equals(localeSource.getLocaleID())) {
                        List<XMLSource> l = new LinkedList<>();
                        l.add(localeSource);
                        l.add(rootSource);
                        XMLSource.ResolvingSource rs = new XMLSource.ResolvingSource(l);
                        return new CLDRFile(rs);
                    } else if (localeID.equals("root")) {
                        List<XMLSource> l = new LinkedList<>();
                        l.add(rootSource);
                        XMLSource.ResolvingSource rs = new XMLSource.ResolvingSource(l);
                        return new CLDRFile(rs);
                    } else if (localeID.equals("en")) {
                        return english;
                    }
                    throw new RuntimeException("Don't know how to make " + localeID + " resolved");
                } else if (localeID.equals(localeSource.getLocaleID())) {
                    return localeFile;
                } else if (localeID.equals("root")) {
                    return rootFile;
                } else {
                    throw new RuntimeException("Don't know how to make " + localeID + " unresolved");
                }
            }

            @Override
            public DraftStatus getMinimalDraftStatus() {
                return DraftStatus.unconfirmed;
            }

            @Override
            protected Set<String> handleGetAvailable() {
                return Collections.singleton(localeSource.getLocaleID());
            }

            @Override
            public List<File> getSourceDirectoriesForLocale(String localeName) {
                return Collections.singletonList(new File(CLDRPaths.MAIN_DIRECTORY));
            }
        };
    }
}
