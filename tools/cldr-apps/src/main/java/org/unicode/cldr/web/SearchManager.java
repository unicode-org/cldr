package org.unicode.cldr.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.util.AnnotationUtil;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StringId;

public class SearchManager implements Closeable {
    static final Logger logger = SurveyLog.forClass(SearchManager.class);
    private static final Pattern URL_PATTERN =
            PatternCache.get("^http.*\\/cldr-apps\\/v#\\/([^/]*)\\/([^/]*)\\/([a-f0-9A-F]{1,16})$");

    private static final int CONFIDENCE_EXACT_XPATH = 100;
    private static final int CONFIDENCE_EXACT_STRING = 90;
    private static final int CONFIDENCE_SUB_STRING = 50;
    private static final int CONFIDENCE_SUB_XPATH = 40;
    private static final int CONFIDENCE_OTHER = 10;

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

        @Schema(
                description =
                        "match confidence (e.g. 100 = exact XPath, 90 = exact String, 50 = partial String, 10 = others")
        public int confidence;

        protected SearchResult(String xpath, String context, String locale) {
            this.xpath = xpath;
            this.context = context;
            this.locale = locale;
            this.xpstrid = XPathTable.getStringIDString(xpath);
            this.confidence = CONFIDENCE_OTHER;
        }

        public SearchResult setConfidence(int confidence) {
            this.confidence = confidence;
            return this;
        }

        @Override
        public int compareTo(SearchResult o) {
            if (this == o) return 0;
            int rc = 0;
            if (rc == 0) rc = o.confidence - this.confidence;
            if (rc == 0) rc = compareXpathAndLocale(o);
            if (rc == 0) rc = this.context.compareTo(o.context);
            return rc;
        }

        /** for identity - ignore confidence and context. */
        private int compareXpathAndLocale(SearchResult o) {
            int rc = 0;
            if (rc == 0) rc = this.xpath.compareTo(o.xpath);
            if (rc == 0) rc = this.locale.compareTo(o.locale);
            return rc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SearchResult)) return false;
            return (compareXpathAndLocale((SearchResult) o) == 0);
        }

        @Override
        public String toString() {
            return locale + ":" + xpath + "=" + context + " %"+confidence;
        }
    }

    /** Struct for the user visible, serialized response */
    public static final class SearchResponse {
        /** max items to search */
        private static final int MAX_SEARCH = 250;

        @Schema(description = "true if too many items came in")
        public boolean isTruncated = false;

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
            final Set<SearchResult> resultSet = new TreeSet<>(results.values());
            return resultSet.toArray(new SearchResult[results.size()]);
        }

        Map<String, SearchResult> results = new ConcurrentHashMap<>();

        /**
         * Internal function for updating search status
         * we can have multiple searches return the same xpath,
         * for example "Guj" might be a substring xpath and a substring value at the same time
         *
         * @param r
         */
        synchronized void addResult(SearchResult r) {
            final SearchResult oldValue = results.putIfAbsent(r.xpath, r);
            if (oldValue != null) {
                int rc = r.compareTo(oldValue);
                if (rc >= 0) return; // later = lower confidence
                results.put(r.xpath, r);
            }
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

        @Schema(hidden = true)
        public int size() {
            return results.size();
        }

        @Schema(hidden = true)
        public synchronized boolean truncateIfFull() {
            if (isTruncated == true) return true;
            if (size() > MAX_SEARCH) {
                isTruncated = true;
                return true;
            }
            return false;
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
            addExactMatches(locale, 0);
            CLDRLocale cLoc = CLDRLocale.getInstance(locale);
            CLDRLocale parLoc = cLoc.getParent();

            addPastedUrl();

            while (parLoc != null && !response.truncateIfFull()) {
                final String parLocName = parLoc.getBaseName();
                logger.finest(() -> " trying " + parLocName);
                addExactMatches(parLocName, 1);
                parLoc = parLoc.getParent();
            }

            // add codes (in the locale)
            addCodes(locale);

            // Add English (if we didn't already search it)
            if (!cLoc.childOf(EN)) {
                addExactMatches(EN.getBaseName(), 2);
            }

            logger.finest(() -> "completing");
            // All done (for now!)
            response.complete();

            return this;
        }

        /** match if user pastes in a URL from production, smoketest, local */
        private void addPastedUrl() {
            try {
                if (request.value.startsWith("http")) {
                    final Matcher m = URL_PATTERN.matcher(request.value);
                    if (m.matches()) {
                        String loc = m.group(1);
                        // final String page = m.group(2);  // don't care about this one
                        final String hex = m.group(3);
                        if (!CookieSession.sm.getDiskFactory().getAvailable().contains(loc)) {
                            loc = locale; // revert to current locale
                        }
                        final String xpath = StringId.getStringFromHexId(hex);
                        if (xpath != null) {
                            response.addResult(
                                    new SearchResult(xpath, "SurveyTool URL", loc)
                                            .setConfidence(CONFIDENCE_EXACT_XPATH));
                        }
                    }
                }
            } catch (Throwable t) {
                logger.log(
                        Level.WARNING,
                        t,
                        () -> String.format("Whilst parsing URL %s", request.value));
            }
        }

        private void addCodes(final String locale) {
            logger.finest(() -> "AddCodes " + locale);
            final CLDRFile resolvedFile = factory.make(locale, true);
            logger.finest(() -> "AddCodes made resolved " + locale);

            for (final String x : resolvedFile.fullIterable()) {
                if (x.startsWith("//ldml/annotations/annotation")) {
                    final String cp = AnnotationUtil.removeEmojiVariationSelector(request.value);
                    if (x.contains("[@cp=\"" + cp + "\"]") && x.contains("[@type=\"tts\"]")) {
                        response.addResult(
                                new SearchResult(x, "tts: " + cp, locale)
                                        .setConfidence(CONFIDENCE_SUB_STRING));
                    }
                    continue; // Do not try to match otherwise code for annotation
                }

                final PathHeader ph = phf.fromPath(x);
                if (!ph.getSurveyToolStatus().visible())
                    continue; // skip invisible paths (but match comprehensive ones)

                // match exact xpath
                if (x.equals(request.value)) {
                    response.addResult(
                            new SearchResult(x, "Exact XPath", locale)
                                    .setConfidence(CONFIDENCE_EXACT_XPATH));
                    return; // Don't try to match others if XPath matches.
                }

                // match xpath hex
                if (StringId.getHexId(x).equals(request.value)) {
                    response.addResult(
                            new SearchResult(x, "Exact Hex XPath", locale)
                                    .setConfidence(CONFIDENCE_EXACT_XPATH));
                    // return; // It might be possible for hex to be "cafe" !
                }

                // match partial xpath
                if (x.startsWith(request.value)) {
                    response.addResult(
                            new SearchResult(x, "Partial XPath", locale)
                                    .setConfidence(CONFIDENCE_SUB_XPATH));
                    // return;
                }

                if (ph.getCode().equalsIgnoreCase(request.value)) {
                    response.addResult(
                            new SearchResult(x, "code: " + ph.getCode(), locale)
                                    .setConfidence(CONFIDENCE_EXACT_STRING));
                    // return;
                }

                // partial code
                if (ph.getCode().contains(request.value)) {
                    response.addResult(
                            new SearchResult(x, "code: " + ph.getCode(), locale)
                                    .setConfidence(CONFIDENCE_SUB_XPATH));
                    // return;
                }
                if (response.truncateIfFull()) return;
            }
        }

        private CLDRFile addExactMatches(final String locale, int deconfidence) {
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

                response.addResult(
                        new SearchResult(xpath, v, locale)
                                .setConfidence(CONFIDENCE_EXACT_STRING - deconfidence));
                if (response.truncateIfFull()) return file;
            }

            // add inexact matches
            final String lowerv = request.value.toLowerCase();
            for (final String xpath : file.fullIterable()) {
                if (response.truncateIfFull()) return file;

                if (!file.isHere(xpath)) continue;

                final String s = file.getStringValue(xpath);
                if(s.contains(request.value)) {
                    response.addResult(
                        new SearchResult(xpath, "…"+request.value+"…", locale)
                        .setConfidence(CONFIDENCE_SUB_STRING - deconfidence));
                } else if(s.toLowerCase().contains(lowerv)) {
                        response.addResult(
                        new SearchResult(xpath, "…"+request.value+"…", locale)
                        .setConfidence(CONFIDENCE_SUB_STRING - deconfidence-5)); // lowercase
                }
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
