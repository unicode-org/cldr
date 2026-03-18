package org.unicode.cldr.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.AnnotationUtil;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;

public class SearchManager implements Closeable {
    static final Logger logger = SurveyLog.forClass(SearchManager.class);

    /** The request of a search */
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

    /** Struct for one single result */
    public static final class SearchResult implements Comparable<SearchResult> {
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

        @Override
        public int compareTo(SearchResult o) {
            if (this == o) return 0;
            int rc = 0;
            if (rc == 0) rc = this.xpath.compareTo(o.xpath);
            if (rc == 0) rc = this.locale.compareTo(o.locale);
            if (rc == 0) rc = this.context.compareTo(o.context);
            return rc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SearchResult)) return false;
            return (compareTo((SearchResult) o) == 0);
        }

        @Override
        public String toString() {
            return locale + ":" + xpath + "=" + context;
        }
    }

    /** Struct for the user visible, serialized response */
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
         *
         * @param r
         */
        synchronized void addResult(SearchResult r) {
            results.add(r);
            lastUpdated = new Date();
            logger.finer(() -> token + ": +1 result");
        }

        /** Mark the search as complete. */
        synchronized void complete() {
            logger.finest(() -> "completing..");
            if (isComplete || !isOngoing) return;
            isComplete = true;
            isOngoing = false;
            lastUpdated = new Date();
            logger.fine(() -> token + ": complete");

            // TODO: could potentially cache the results here.
        }

        @Override
        public String toString() {
            return String.format(
                    "[SearchResponse#%s: %s, %s, #=%d]",
                    token,
                    isComplete ? "complete" : "incomplete",
                    isOngoing ? "ongoing" : "stopped",
                    results.size());
        }

        @Schema(hidden = true)
        public boolean isEmpty() {
            return (results.isEmpty());
        }
    }

    /** A search in progress */
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
            logger.fine(
                    () ->
                            String.format(
                                    "%s: l=%s, q='%s'",
                                    this.response, this.locale, this.request.value));
        }

        public void begin() {
            // TODO: could look up cached results here.

            future = SurveyThreadManager.getExecutorService().submit(this);
        }

        final CLDRLocale EN = CLDRLocale.getInstance("en");

        @Override
        public Search call() throws Exception {
            logger.finest("begin call()");
            // first, simple search of exact values
            addExactMatches(locale);
            CLDRLocale cLoc = CLDRLocale.getInstance(locale);
            CLDRLocale parLoc = cLoc.getParent();
            while (response.isEmpty() && parLoc != null) {
                final String parLocName = parLoc.getBaseName();
                logger.finest(() -> " trying " + parLocName);
                addExactMatches(parLocName);
                parLoc = parLoc.getParent();
            }

            // add codes (in the locale)
            addCodes(locale);

            // Add English (if we didn't already search it)
            if (response.isEmpty() && !cLoc.childOf(EN)) {
                addExactMatches(EN.getBaseName());
            }

            logger.finest(() -> "completing");
            // All done (for now!)
            response.complete();

            return this;
        }

        private void addCodes(final String locale) {
            logger.finest(() -> "AddCodes " + locale);
            final CLDRFile resolvedFile = factory.make(locale, true);
            logger.finest(() -> "AddCodes made resolved " + locale);

            for (final String x : resolvedFile.fullIterable()) {
                if (x.startsWith("//ldml/annotations/annotation")) {
                    final String cp = AnnotationUtil.removeEmojiVariationSelector(request.value);
                    if (x.contains("[@cp=\"" + cp + "\"]") && x.contains("[@type=\"tts\"]")) {
                        response.addResult(new SearchResult(x, "tts: " + cp, locale));
                    }
                    continue; // Do not try to match otherwise code for annotation
                }

                final PathHeader ph = phf.fromPath(x);
                if (!ph.getSurveyToolStatus().visible()) continue; // skip invisible paths

                // match exact xpath
                if (x.equals(request.value)) {
                    response.addResult(new SearchResult(x, "Exact XPath", locale));
                    return;
                }

                // match partial xpath
                if (x.startsWith(request.value)) {
                    response.addResult(new SearchResult(x, "Partial XPath", locale));
                    return;
                }

                if (ph.getCode().equalsIgnoreCase(request.value)) {
                    response.addResult(new SearchResult(x, "code: " + ph.getCode(), locale));
                    return;
                }

                // partial code
                if (ph.getCode().contains(request.value)) {
                    response.addResult(new SearchResult(x, "code: " + ph.getCode(), locale));
                    return;
                }
            }
        }

        private CLDRFile addExactMatches(final String locale) {
            logger.finest(() -> "AEM " + locale + " on " + factory);
            CLDRFile file = factory.make(locale, false);
            logger.finest(() -> "AEM called make on " + locale);
            Set<String> xresult = new TreeSet<>();
            file.getPathsWithValue(request.value, "", null, xresult);
            for (final String xpath : xresult) {
                // Skip if this isn’t found "here"
                if (!file.isHere(xpath)) {
                    continue;
                }

                final PathHeader ph = phf.fromPath(xpath);
                if (!ph.getSurveyToolStatus().visible()) continue; // skip invisible paths

                // Add incrementally. A user may get a partial result if they request before we are
                // done.
                final String v = file.getStringValue(xpath);
                if (!v.equals(request.value)) continue; // TODO: CLDR-18700

                response.addResult(new SearchResult(xpath, v, locale));
            }
            return file;
        }

        public void stop() {
            future.cancel(true);
            response.complete();
        }
    }

    private Factory factory;

    final Cache<Object, Object> searches =
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

    private org.unicode.cldr.util.PathHeader.Factory phf;

    private SearchManager(Factory f) {
        this.factory = f;
        this.phf = PathHeader.getFactory(f.make("en", true));
    }

    /**
     * Factory for the SearchManager
     *
     * @param f
     * @return
     */
    public static SearchManager forFactory(Factory f) {
        return new SearchManager(f);
    }

    /**
     * Start up a new search
     *
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
     *
     * @param token
     * @return
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public SearchResponse getSearch(final String token) throws ExecutionException {
        final Search s = (Search) searches.getIfPresent(token);
        if (s == null) return null; // not present
        try {
            return s.future.get(1, TimeUnit.SECONDS).response;
        } catch (InterruptedException | TimeoutException e) {
            // the exception here isn't interesting
            logger.log(
                    Level.FINEST,
                    () ->
                            String.format(
                                    "Search %s was interrupted or timeout, but will return best available result: %s",
                                    token, e.toString()));
            return s.response;
        } catch (ExecutionException e) {
            logger.log(
                    Level.WARNING,
                    e,
                    () ->
                            String.format(
                                    "Search %s had exception %s",
                                    token, e.getCause().getMessage()));
            s.response.complete(); // mark as complete
            throw e;
        }
    }

    /**
     * Remove the specified search, stopping any operation in progress. This token is no longer
     * valid.
     *
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

    @Override
    public void close() throws IOException {
        // attempt to shut down all running searches
        for (final Object k : searches.asMap().keySet()) {
            deleteSearch((String) k);
        }
        searches.invalidateAll();
    }
}
