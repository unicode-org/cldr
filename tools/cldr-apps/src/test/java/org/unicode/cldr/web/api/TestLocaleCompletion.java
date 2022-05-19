package org.unicode.cldr.web.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleSet;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.SandboxLocales;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.VoterInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.web.MemoryUserRegistry;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.XPathTable;
import org.unicode.cldr.web.api.LocaleCompletion.LocaleCompletionResponse;

public class TestLocaleCompletion {
    /**
     * Locale to use. We will start with a blank slate.
     */
    private static final String TEST_LOCALE = "es";
    /**
     * Sanity check amount: Assume at least these many paths are present.
     */
    private int lOCALE_SIZE = 5;

    @Test
    void testLocaleCompletion() throws SQLException, IOException {
        SurveyMain sm = getSurveyMain();
        final TestCache tc = new TestCache();
        final CLDRConfig config = CLDRConfig.getInstance();
        final CLDRFile english = config.getEnglish();
        final CLDRFile spanish = config.getCLDRFile("es", false);
        CheckCLDR.setDisplayInformation(english);

        final CLDRLocale locale = CLDRLocale.getInstance(TEST_LOCALE);
        // final CLDRFile mulFile = sandFactory.make(mul.getBaseName(), false);
        final SandboxLocales.ScratchXMLSource localeSource = new SandboxLocales.ScratchXMLSource(locale.getBaseName());
        final CLDRFile localeFile = new CLDRFile(localeSource);
        final SandboxLocales.ScratchXMLSource rootSource = new SandboxLocales.ScratchXMLSource("root");
        final CLDRFile rootFile = new CLDRFile(rootSource);
        final Factory sandboxFactory = getFactory(english, localeSource, localeFile, rootSource, rootFile);
        tc.setFactory(sandboxFactory, "(?!.*(CheckCoverage).*).*");

        STFactory stf = getSTFactory(sm, tc, sandboxFactory);

        preloadLocaleFile(spanish, localeFile);

        /* TODO Temporarily disabled for CLDR-15585, restore when data for es is fleshed out
        testCompletePaths(tc, locale, localeFile, stf);

        testIncompletePaths(tc, locale, localeFile, stf);
        */
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
        final int EXPECT_ERROR = 2;
        final int EXPECT_MISSING = 1;
        final int EXPECT_PROVISIONAL = 2;

        assertFalse(CheckCLDR.LIMITED_SUBMISSION, "This test becomes invalid if LIMITED_SUBMISSION (or SubmissionLocales) changes.");

        {
            LocaleCompletionResponse completion = LocaleCompletion.handleGetLocaleCompletion(locale, stf);

            assertNotNull(completion);
            assertAll("tests on completion - not quite 100%",
                () -> assertEquals(Level.MODERN.name(), completion.level, "completion level"),
                () -> assertTrue(completion.votes > lOCALE_SIZE, "completion votes should be bigger but was " + completion.votes),
                () -> assertTrue(completion.votes < completion.total,
                    "completion votes should be less than completion total but was  " + completion.votes + "/" + completion.total),
                () -> assertEquals(completion.total - (EXPECT_ERROR + EXPECT_MISSING + EXPECT_PROVISIONAL), completion.votes,
                    "completion votes should be less than completion total but was  " + completion.votes + "/" + completion.total),
                () -> assertTrue(completion.total > lOCALE_SIZE, "completion total should be bigger but was " + completion.total),
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
        // Make some changes, and we should have a couple of errors.
        // "Expected <100%"
        // Mutate
        localeFile.add("//ldml/characters/exemplarCharacters", "[]");
        localeFile.add("//ldml/numbers/symbols[@numberSystem=\"latn\"]/group",
            ",,,"); // err; expected 1, not 3
        // rootFile.add("//ldml/localeDisplayNames/languages/language[@type=\""+TEST_LOCALE+"\"]",
        //     TEST_LOCALE); // for fallback
        localeFile.remove("//ldml/localeDisplayNames/languages/language[@type=\"" + TEST_LOCALE + "\"]");
        localeFile.add("//ldml/dates/timeZoneNames/zone[@type=\"Pacific/Kanton\"]/exemplarCity[@draft=\"provisional\"]", "cabaca");
        localeFile.add("//ldml/dates/timeZoneNames/zone[@type=\"Asia/Qyzylorda\"]/exemplarCity[@draft=\"unconfirmed\"]", "bacaba");
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
        // ** Step one.  Setup the locale
        // Expected 100%
        setCompletePaths(localeFile);

        tc.invalidateAllCached();

        {
            LocaleCompletionResponse completion = LocaleCompletion.handleGetLocaleCompletion(locale, stf);

            assertNotNull(completion);
            assertAll("tests on completio - 100%",
                () -> assertEquals(Level.MODERN.name(), completion.level, "completion level"),
                () -> assertTrue(completion.votes > lOCALE_SIZE, "completion votes should be bigger but was " + completion.votes),
                () -> assertEquals(completion.total, completion.votes,
                    "completion votes should be equal completion total but was  " + completion.votes + "/" + completion.total),
                () -> assertTrue(completion.total > lOCALE_SIZE, "completion total should be bigger but was " + completion.total),
                () -> assertEquals(0, completion.error),
                () -> assertEquals(0, completion.missing),
                () -> assertEquals(0, completion.provisional));
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
        localeFile.add("//ldml/localeDisplayNames/languages/language[@type=\"" + TEST_LOCALE + "\"]",
            "baccacab"); // for fallback (need to use only 'a,b,c')

        localeFile.add("//ldml/numbers/currencies/currency[@type=\"XTS\"]/displayName[@draft=\"provisional\"]",
            "Test Currency (comprehensive level, should not have an effect on completion)");
        localeFile.add("//ldml/numbers/currencies/currency[@type=\"XUA\"]/displayName[@draft=\"unconfirmed\"]",
            "Other Test Currency (comprehensive level, should not have an effect on completion)");
    }

    /**
     * Load some initial paths (with exclusions) from a disk file
     * @param spanish
     * @param localeFile
     */
    private void preloadLocaleFile(final CLDRFile spanish, final CLDRFile localeFile) {
        // Preload some stuff
        for (final String xpath : spanish) {
            // Skip some problematic paths.
            if (xpath.startsWith("//ldml/characters/parseLenients") ||
                xpath.startsWith("//ldml/numbers/currency") ||
                xpath.startsWith("//ldml/numbers/scientific") ||
                xpath.startsWith("//ldml/numbers/decimal") ||
                xpath.startsWith("//ldml/numbers/percent")) {
                continue;
            }
            localeFile.add(xpath, spanish.getStringValue(xpath));
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
        STFactory stf = new STFactory(sm) {
            @Override
            public TestResultBundle getTestResult(CLDRLocale loc, CheckCLDR.Options options) {
                return tc.getBundle(options);
            }

            @Override
            public CLDRFile make(String locale, boolean resolved) {
                return sandboxFactory.make(locale, resolved); // pass through
            }
        };
        return stf;
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
        final Factory sandFactory = new Factory() {

            @Override
            public File getSupplementalDirectory() {
                return english.getSupplementalDirectory();
            }

            @Override
            public File[] getSourceDirectories() {
                File[] f = { new File(CLDRPaths.MAIN_DIRECTORY) };
                return f;
            }

            @Override
            protected CLDRFile handleMake(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
                if (resolved) {
                    if (localeID.equals(TEST_LOCALE)) {
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
                    }
                    throw new RuntimeException("dont know how to make " + localeID + " r=" + resolved);
                } else if (localeID.equals(TEST_LOCALE)) {
                    return localeFile;
                } else if (localeID.equals("root")) {
                    return rootFile;
                } else {
                    throw new RuntimeException("dont know how to make " + localeID + " r=" + resolved);
                }
            }

            @Override
            public DraftStatus getMinimalDraftStatus() {
                return DraftStatus.unconfirmed;
            }

            @Override
            protected Set<String> handleGetAvailable() {
                return Collections.singleton(TEST_LOCALE);
            }

            @Override
            public List<File> getSourceDirectoriesForLocale(String localeName) {
                return Collections.singletonList(new File(CLDRPaths.MAIN_DIRECTORY));
            }
        };
        return sandFactory;
    }

    /**
     * New up a SurveyMain
     * @return
     */
    private SurveyMain getSurveyMain() {
        SurveyMain sm = new SurveyMain();
        sm.xpt = new XPathTable();
        sm.reg = new MemoryUserRegistry() {
            final VoterInfo user0 = new VoterInfo(Organization.guest, VoteResolver.Level.tc, "Test User", new LocaleSet(true));

            @Override
            public synchronized Map<Integer, VoterInfo> getVoterToInfo() {
                Map<Integer, VoterInfo> m = new TreeMap<Integer, VoterInfo>();
                m.put(0, user0);
                return m;
            }

            @Override
            public VoterInfo getVoterToInfo(int userid) {
                if (userid == 0) {
                    return user0;
                } else {
                    return null;
                }
            }
        };
        return sm;
    }
}
