package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.web.SearchManager.SearchRequest;
import org.unicode.cldr.web.SearchManager.SearchResponse;
import org.unicode.cldr.web.SearchManager.SearchResult;

public class TestSearchManager {
    private static SearchManager mgr;

    @BeforeAll
    private static void setup() {
        mgr = SearchManager.forFactory(CLDRConfig.getInstance().getMainAndAnnotationsFactory());
    }

    @AfterAll
    private static void teardown() throws IOException {
        mgr.close();
    }

    @Test
    void TestMaltese() throws InterruptedException, ExecutionException {
        final String XPATH =
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"3\"]";
        final String searchText = "Marzu";
        final String locale = "mt";

        testOneSearchResult(XPATH, searchText, locale, locale);
    }

    @Test
    void TestEnglish() throws InterruptedException {
        final String XPATH =
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"3\"]";
        final String searchText = "March";
        final String locale = "en";

        // should be found in English
        assertAll(
                () -> testOneSearchResult(XPATH, searchText, "en", locale),
                // should be found even if we try in a different locale
                () -> testOneSearchResult(XPATH, searchText, "mt", locale));
    }

    @Test
    void TestCode() throws InterruptedException {
        final String XPATH = "//ldml/localeDisplayNames/scripts/script[@type=\"Gujr\"]";
        final String searchText = "Gujr";
        final String locale = "ar";
        assertAll(
                "looking for " + searchText + " in " + locale,
                () ->
                        testOneSearchResult(
                                XPATH,
                                searchText,
                                locale,
                                locale,
                                "code: " + searchText), // should match in the locale
                () ->
                        testOneSearchResult(
                                XPATH,
                                searchText.substring(0, 3),
                                locale,
                                locale,
                                "code: " + searchText) // should match with a partial string
                );
    }

    @Test
    void TestCodeBF() throws InterruptedException {
        final String XPATH = "//ldml/localeDisplayNames/territories/territory[@type=\"BF\"]";
        final String searchText = "BF";
        final String locale = "ff_Adlm_BF";
        assertAll(
                "looking for " + searchText + " in " + locale,
                () ->
                        testOneSearchResult(
                                XPATH,
                                searchText,
                                locale,
                                locale,
                                "code: " + searchText) // should match in the locale
                );
    }

    @Test
    void TestCodeXJ() throws InterruptedException {
        final String searchText = "XJ";
        final String locale = "root"; // won't find XJ
        assertAll(
                "looking for " + searchText + " in " + locale,
                () -> testNoResult(searchText, locale));
    }

    @Test
    void TestExceptionInSearch() throws InterruptedException {
        final String searchText = "XJ";
        final String locale = "mul"; // will throw
        assertThrows(ExecutionException.class, () -> testNoResult(searchText, locale));
    }

    @Test
    void TestKorean() throws InterruptedException {
        final String XPATH =
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"3\"]";
        final String searchText = "3월";
        final String locale = "ko";

        assertAll(
                () -> testOneSearchResult(XPATH, searchText, locale, locale),
                () -> testNoResult("3월3월", "ko"));
    }

    /** <annotation cp="🕗" draft="contributed"> //ldml/annotations/annotation[@cp="🕗"] */
    @Test
    void TestEmoji() throws InterruptedException {
        final String CP = Character.toString(0x1F557);
        final String CP2 =
                Character.toString(0x2764)
                        + Character.toString(0xFE0F)
                        + Character.toString(0x200D)
                        + Character.toString(0x1F525); // "❤️‍🔥"
        final String CP2_NOVS =
                Character.toString(0x2764)
                        + Character.toString(0x200D)
                        + Character.toString(0x1F525); // "❤️‍🔥"
        System.out.println(CP);
        final String XPATH = "//ldml/annotations/annotation[@cp=\"" + CP + "\"][@type=\"tts\"]";
        final String XPATH2 =
                "//ldml/annotations/annotation[@cp=\"" + CP2_NOVS + "\"][@type=\"tts\"]";
        final String searchText = CP;
        final String locale = "ja";
        assertAll(
                "looking for " + searchText + " in " + locale,
                () ->
                        testOneSearchResult(
                                XPATH,
                                searchText,
                                locale,
                                locale,
                                "tts: " + searchText), // should match in the locale
                () ->
                        testOneSearchResult(
                                XPATH,
                                searchText + Emoji.EMOJI_VARIANT, // should still match
                                locale,
                                locale,
                                "tts: " + searchText) // should match in the locale
                ,
                () -> testOneSearchResult(XPATH2, CP2, locale, locale, "tts: " + CP2_NOVS),
                () -> testOneSearchResult(XPATH2, CP2_NOVS, locale, locale, "tts: " + CP2_NOVS));
    }

    private void testOneSearchResult(
            final String XPATH,
            final String searchText,
            final String locale,
            final String resultLocale)
            throws InterruptedException, ExecutionException {
        testOneSearchResult(XPATH, searchText, locale, resultLocale, searchText);
    }

    private void testOneSearchResult(
            final String XPATH,
            final String searchText,
            final String locale,
            final String resultLocale,
            final String expectContext)
            throws InterruptedException, ExecutionException {
        final Set<SearchResult> r =
                ImmutableSet.of(new SearchResult(XPATH, expectContext, resultLocale));

        testSearchResult(searchText, locale, r);
    }

    private void testNoResult(final String searchText, final String locale)
            throws InterruptedException, ExecutionException {
        testSearchResult(searchText, locale, Collections.emptySet());
    }

    private void testSearchResult(
            final String searchText, final String locale, Set<SearchResult> expected)
            throws InterruptedException, ExecutionException {
        // Create a SearchManager over disk files
        assertNotNull(mgr);
        final String searchContext = searchText + " in " + locale;

        SearchRequest request = new SearchRequest(searchText);
        final SearchResponse r0 = mgr.newSearch(request, locale);
        assertNotNull(r0);
        assertNotNull(r0.token);
        // can't assume it has not already completed.

        final int PATIENCE = 6000; // x .1 seconds
        for (int n = 0; n < PATIENCE; n++) {
            final SearchResponse r1 = mgr.getSearch(r0.token);
            if (r1.isOngoing) {
                assertFalse(r1.isComplete);
                Thread.sleep(100);
                continue;
            }
            // OK, no longer ongoing
            assertTrue(r1.isComplete);
            assertFalse(r1.isOngoing);
            assertNotNull(r1.getResults());
            final Set<SearchResult> results = new TreeSet<SearchResult>();
            for (final SearchResult result : r1.getResults()) {
                results.add(result);
                assertFalse(
                        result.xpath.contains("/identity"),
                        () ->
                                String.format(
                                        "Did not expect /identity xpaths present in %s - %s",
                                        result.xpath, searchContext));
            }
            if (expected.isEmpty()) {
                assertEquals(
                        0,
                        results.size(),
                        () -> "Expected no results looking for " + searchContext);
            } else {
                assertTrue(
                        results.size() >= 1,
                        () ->
                                "Expected >=1 result, got "
                                        + results.size()
                                        + " looking for "
                                        + searchContext);
                for (final SearchResult result : expected) {
                    assertTrue(
                            results.contains(result),
                            () ->
                                    "Did not find result "
                                            + result
                                            + " looking for "
                                            + searchContext
                                            + " ("
                                            + CodePointEscaper.toEscaped(searchText)
                                            + ")"
                                            + " - matches= "
                                            + results);
                }
            }

            // if there's only one, do an exact comparison
            if (results.size() == 1 && expected.size() == 1) {
                final SearchResult expectedResult = expected.iterator().next();
                final SearchResult foundResult = results.iterator().next();
                assertEquals(expectedResult.xpath, foundResult.xpath);
                assertEquals(expectedResult.xpstrid, foundResult.xpstrid);
                assertEquals(expectedResult.locale, foundResult.locale);
                assertEquals(expectedResult.context, foundResult.context);
            }

            // delete the query
            assertTrue(mgr.deleteSearch(r1.token));
            assertNull(mgr.getSearch(r1.token)); // no longer there
            assertFalse(mgr.deleteSearch(r1.token)); // can't delete it further
            assertNull(mgr.getSearch(r1.token)); // nope, it's really gone!
            return;
        }
        throw new RuntimeException(
                "Patience exceeded for query "
                        + r0.token
                        + " - "
                        + searchContext
                        + " - did not complete in time.");
    }

    @Test
    void TestApi() throws ExecutionException {
        // Create a SearchManager over disk files
        SearchManager mgr = SearchManager.forFactory(CLDRConfig.getInstance().getCldrFactory());
        assertNotNull(mgr);
        assertFalse(mgr.deleteSearch("some token"));
        assertNull(mgr.getSearch("some token"));
    }
}
