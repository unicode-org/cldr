package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.web.SearchManager.SearchRequest;
import org.unicode.cldr.web.SearchManager.SearchResponse;
import org.unicode.cldr.web.SearchManager.SearchResult;

public class TestSearchManager {
    @Test
    void TestMaltese() throws InterruptedException {
        // Create a SearchManager over disk files
        SearchManager mgr = SearchManager.forFactory(CLDRConfig.getInstance().getCldrFactory());
        assertNotNull(mgr);

        SearchRequest request = new SearchRequest("Marzu");
        final SearchResponse r0 = mgr.newSearch(request, "mt");
        assertNotNull(r0);
        assertNotNull(r0.token);
        // can't assume it has not already completed.

        final int PATIENCE = 40; // x .1 seconds = 4 seconds
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
            SearchResult[] results = r1.getResults();
            assertNotNull(results);
            assertTrue(results.length >= 1, () -> "Expected >=1 result, got " + results.length);
            int foundFormat = 0;
            for (final SearchResult result : results) {
                if (result.xpstrid.equals("1c7bd76a22b7472f")) {
                    foundFormat++;
                    assertEquals(
                            result.xpath,
                            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"3\"]");
                    assertEquals(result.xpstrid, "1c7bd76a22b7472f");
                    assertEquals(result.locale, "mt");
                    assertEquals(result.context, request.value);
                } else {
                    assertNotEquals(
                            result.xpath,
                            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"3\"]");
                }
            }
            assertEquals(1, foundFormat, "Expect to find exactly 1 xpath 1c7bd76a22b7472f");

            // delete the query
            assertTrue(mgr.deleteSearch(r1.token));
            assertNull(mgr.getSearch(r1.token)); // no longer there
            assertFalse(mgr.deleteSearch(r1.token)); // can't delete it further
            assertNull(mgr.getSearch(r1.token)); // nope, it's really gone!
            return;
        }
        throw new RuntimeException(
                "Patience exceeded for query " + r0.token + " - did not complete in time.");
    }

    @Test
    void TestApi() {
        // Create a SearchManager over disk files
        SearchManager mgr = SearchManager.forFactory(CLDRConfig.getInstance().getCldrFactory());
        assertNotNull(mgr);
        assertFalse(mgr.deleteSearch("some token"));
        assertNull(mgr.getSearch("some token"));
    }
}
