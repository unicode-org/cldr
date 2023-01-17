package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SearchManager {
    static final Logger logger = SurveyLog.forClass(SearchManager.class);

    /**
     * The request of a search
     */
    public static final class SearchRequest {
        public SearchRequest(String v) {
            value = v;
        }

        public SearchRequest() {
            value = null;
        }

        @Schema(description = "Value to search for")
        public String value;
    }

    /**
     * Struct for one single result
     */
    public static final class SearchResult {
        @Schema(description = "xpath to the resource")
        public String xpath;

        @Schema(description = "xpstrid to the resource")
        public String xpstrid;

        @Schema(description = "context of the match")
        public String context;

        @Schema(description = "locale of the match")
        public String locale;

        protected SearchResult(String xpath, String context, String locale) {
            this.xpath = xpath;
            this.context = context;
            this.locale = locale;
            this.xpstrid = XPathTable.getStringIDString(xpath);
        }
    }

    /**
     * Struct for the user visible, serialized response
     */
    public static final class SearchResponse {
        @Schema(description = "true if the search is now complete (all results in)")
        public boolean isComplete = false;

        @Schema(description = "true if the search is still ongoing (not cancelled or finished)")
        public boolean isOngoing = false;

        @Schema(description = "token for retrieving further data from the query")
        public String token = "";

        @Schema(description = "when the search was started")
        public Date searchStart;

        @Schema(description = "when the search was started")
        public Date lastUpdated;

        @Schema(description = "array of search results, in found order")
        public synchronized SearchResult[] getResults() {
            return results.toArray(new SearchResult[results.size()]);
        }

        List<SearchResult> results = new ArrayList<>();

        /**
         * Internal function for updating search status
         * @param r
         */
        synchronized void addResult(SearchResult r) {
            results.add(r);
            lastUpdated = new Date();
            logger.finer(() -> token + ": +1 result");
        }

        /**
         * Mark the search as complete.
         */
        synchronized void complete() {
            if (isComplete || !isOngoing)
                return;
            isComplete = true;
            isOngoing = false;
            lastUpdated = new Date();
            logger.fine(() -> token + ": complete");

            // TODO: could potentially cache the results here.
        }

        @Override
        public String toString() {
            return String.format("[SearchResponse#%s: %s, %s, #=%d]",
                token,
                isComplete ? "complete" : "incomplete",
                isOngoing ? "ongoing" : "stopped",
                results.size());
        }
    }

    /**
     * A search in progress
     */
    private class Search implements Callable<Search> {

        private final SearchRequest request;
        private final SearchResponse response = new SearchResponse();
        private final String locale;
        private Future<Search> future;

        Search(SearchRequest request, String locale) {
            this.locale = locale;
            this.request = request;
            this.response.lastUpdated = this.response.searchStart = new Date();
            this.response.isOngoing = true;
            this.response.isComplete = false;
            this.response.token = CookieSession.newId();
            logger.fine(() -> String.format("%s: l=%s, q='%s'",
                this.response,
                this.locale,
                this.request.value));
        }

        public void begin() {
            // TODO: could look up cached results here.

            this.future = SurveyThreadManager
                .getExecutorService()
                .submit(this);
        }

        @Override
        public Search call() throws Exception {
            // first, simple search of exact values
            CLDRFile file = factory.make(locale, false);
            Set<String> xresult = new TreeSet<>();
            file.getPathsWithValue(request.value, "", null, xresult);
            for (final String xpath : xresult) {
                // Skip if this isn’t found "here"
                if (!file.isHere(xpath)) {
                    continue;
                }
                // Add incrementally. A user may get a partial result if they request before we are done.
                response.addResult(new SearchResult(
                    xpath, request.value, locale));
            }

            // All done (for now!)
            response.complete();

            return this;
        }

        public void stop() {
            future.cancel(true);
            response.complete();
        }
    }

    private Factory factory;

    final Cache<Object, Object> searches = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();

    private SearchManager(Factory f) {
        this.factory = f;
    }

    /**
     * Factory for the SearchManager
     * @param f
     * @return
     */
    public static SearchManager forFactory(Factory f) {
        return new SearchManager(f);
    }

    /**
     * Start up a new search
     * @param request
     * @return
     */
    public SearchResponse newSearch(final SearchRequest request, final String locale) {
        final Search s = new Search(request, locale);
        searches.put(s.response.token, s);

        s.begin();

        return s.response;
    }

    /**
     * Get any updated search result using a prior token
     * @param token
     * @return
     */
    public SearchResponse getSearch(final String token) {
        Search s = (Search) searches.getIfPresent(token);
        if (s == null) return null;
        return s.response;
    }

    /**
     * Remove the specified search, stopping any operation in progress.
     * This token is no longer valid.
     * @param token
     * @return true if the search was found before it was deleted
     */
    public synchronized boolean deleteSearch(final String token) {
        Search s = (Search) searches.getIfPresent(token);
        if (s == null) return false;
        searches.invalidate(token);
        s.stop();
        return true;
    }
}
