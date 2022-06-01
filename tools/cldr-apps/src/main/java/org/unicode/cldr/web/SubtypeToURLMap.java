// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package org.unicode.cldr.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRCacheDir;
import org.unicode.cldr.util.CLDRTool;

import com.ibm.icu.text.MessageFormat;

@CLDRTool(alias="subtype-to-url-map", description = "parse each of the params as a path or URL to a subtype map and check.")
public class SubtypeToURLMap {
    static final Logger logger = SurveyLog.forClass(SubtypeToURLMap.class);
    /**
     * Little tool for validating input data.
     * @param args list of files to validate, if empty runs against default data.
     * @throws IOException
     * @throws FileNotFoundException
     */
     public static void main(String args[]) throws FileNotFoundException, IOException {
        if(args.length == 0) {
            System.err.println("Usage: SubtypeToURLMap (url or file path). The default map is " + DEFAULT_URL);
            return;
        } else {
            int problems = 0;
            for(final String fn : args) {
                System.out.println("data: " + fn);
                SubtypeToURLMap map = getInstance(new File(fn));
                problems += map.dump();
            }
            if(problems > 0) {
                throw new IllegalArgumentException(MessageFormat.format("Total problem(s) found: {0} in {1} items(s)", problems, args.length));
            }
        }
    }

    /**
     * Map from Subtype to URL
     */
    private Map<Subtype, String> map;

    /**
     * The set of URLs is kept as a List so the original order is retained.
     */
    private List<String> urlList;

    public static class URLMapReader {
        final Map<Subtype, String> newMap = new HashMap<>();
        final List<String> newList = new ArrayList<>();
        int lineCount = 0;
        boolean started = false; // Are we in started state?
        boolean everStarted = false; // did we ever start?
        boolean hadSubtype = false; // did we have a subtype for this URL?
        int urlLast = 0; // line number of the last URL
        int subtypeLast = 0; // line number of the last subtype
        String url = null; // last URL seen
        int urlCount = 0; // number of urls

        void read(BufferedReader utf8Data) {
            try {
                for(String ln; (ln=utf8Data.readLine())!=null;) {
                    if(!handleLine(ln)) break;
                }
                handleComplete();
            } catch (IOException e) {
                throw new IllegalArgumentException("Line " + lineCount + ": Could not read subtypeMapping file", e);
            }
        }

        /**
         *
         * @param ln
         * @return true if handled, false if done
         */
        private boolean handleLine(String ln) {
            ++lineCount;
            ln = ln.trim();
            if(ln.isEmpty()) return true;
            if(!started) {
                if(ln.contains(BEGIN_MARKER)) {
                    handleBegin();
                }
                return true;
            }
            if(ln.contains(END_MARKER)) {
                return handleEnd();
            } else if(ln.isEmpty() || ln.startsWith(COMMENT)) {
                return true;
            } else if(isUrl(ln)) {
                handleUrl(ln);
            } else {
                handleSubtype(ln);
            }
            return true;
        }

        private void handleBegin() {
            started = everStarted = true;
        }

        private boolean handleEnd() {
            started = false;
            return false; // exit loop, got end
        }

        private void handleSubtype(String ln) {
            for(String str : ln.split("[, ]")) {
                str = str.trim();
                if(str.isEmpty()) continue;
                if(url == null) {
                    throw new IllegalArgumentException("Line " + lineCount + ": No page URL has been found yet for " + str);
                }
                try {
                    Subtype subtype = Subtype.valueOf(str);
                    newMap.put(subtype, url);
                    subtypeLast = lineCount;
                    hadSubtype = true;
                } catch(IllegalArgumentException iae) {
                    throw new IllegalArgumentException("Line " + lineCount + ": No subtype named '"+str+"'. See CheckCLDR.Subtype.");
                }
            }
        }

        private boolean isUrl(String ln) {
            return ln.startsWith("http:") || ln.startsWith("https:");
        }

        private void handleUrl(String ln) {
            urlLast = lineCount;
            url = ln;
            urlCount++;
            hadSubtype = false; // reset this
            try {
                new java.net.URL(ln);
            } catch(MalformedURLException mfe) {
                throw new IllegalArgumentException("Line " + lineCount + ": malformed URL: " + ln);
            }
            newList.add(ln);
        }

        private void handleComplete() {
            if(started) {
                throw new IllegalArgumentException("line " + lineCount + " - Error: No END line after line " + urlLast + ".");
            } else if (!everStarted) {
                throw new IllegalArgumentException("line " + lineCount + " - Error: No BEGIN line ");
            }
            if(urlCount>0 && !hadSubtype) { // had URLs, but did not end with a subtype
                throw new IllegalArgumentException("Error: Dangling URL " + url.toString() + " from line " + urlLast + " with no subtypes. Comment out that line.");
            } else if(subtypeLast == 0) {
                logger.warning("SubtypeToURLMap: Warning: no subtypes specified or no BEGIN line detected.");
            } else {
                logger.info("SubtypeToURLMap: read " + lineCount + " lines, " + urlCount + " urls and " + newMap.size() + " subtypes mapped.");
            }
        }

        public Map<Subtype, String> getMap() {
            return Collections.unmodifiableMap(newMap);
        }

        public List<String> getList() {
            return Collections.unmodifiableList(newList);
        }
    }

    /**
     * Internal constructor for
     * @param utf8Data for cleaned input data (i.e. not HTML)
     * @throws IllegalArgumentException
     */
    protected SubtypeToURLMap(BufferedReader utf8Data) throws IllegalArgumentException{
        URLMapReader mapReader = new URLMapReader();
        mapReader.read(utf8Data);

        this.map = mapReader.getMap();
        this.urlList = mapReader.getList();
    }
    public static final String COMMENT = "#";
    public static final String END_MARKER = "-*- END CheckCLDR.Subtype Mapping -*-";
    public static final String BEGIN_MARKER = "-*- BEGIN CheckCLDR.Subtype Mapping -*-";

    /**
     * Dump a subtype map to stderr.
     * @return unhandled type count, or zero
     */
    public int dump() {
        int count = 0;
        for(final Subtype s : getHandledTypes()) {
            System.err.println(s + " => " + get(s));
        }

        System.err.println("Not Handled:");
        for(final Subtype s : getUnhandledTypes()) {
            count++;
            System.err.println("  " + s.name()+",");
        }
        return count;
    }

    /**
     * Write the URLMap to a file
     * @param pw
     * @throws IOException
     */
    public void write(PrintWriter pw) throws IOException {
        pw.println(COMMENT + " " + "dumped by " + getClass().getSimpleName());
        pw.println(COMMENT + " " + BEGIN_MARKER);
        for (final String url : getUrls()) {
            pw.println(url);
            for(final Subtype type : getSubtypesForUrl(url)) {
                pw.println(type.name()+",");
            }
            pw.println();
        }
        pw.println(COMMENT + " " + END_MARKER);
    }

    /**
     * Get a list of the URLs, in original order
     * @return
     */
    public List<String> getUrls() {
        return urlList;
    }
    /**
     * get the subtypes that match a certain URL
     */
    public Set<Subtype> getSubtypesForUrl(final String url) {
        Set<Subtype> set = new TreeSet<>();
        map.forEach((Subtype t, String s) -> {
            if(s.equals(url)) set.add(t);
        });
        return set;
    }

    /**
     * Get the URL with more information about a subtype.
     * @param subtype the subtype to fetch
     * @return url or null
     */
    public String get(Subtype subtype) {
        return map.get(subtype);
    }

    /**
     * Get the subtypes that ARE handled by this map.
     * @return
     */
    public Collection<Subtype> getHandledTypes() {
        return Collections.unmodifiableSet(map.keySet());
    }

    /**
     * Get the subtypes that ARE NOT handled by this map.
     * @return
     */
    public Collection<Subtype> getUnhandledTypes() {
        Set<Subtype> ts = createExpectedSubtypes();
        ts.removeAll(map.keySet());
        return Collections.unmodifiableSet(ts);
    }

    /**
     * Create a set with the expected subtypes (minus 'none') in name order.
     * The set is mutable, with the expectation that the caller may use the set
     * for verification.
     * @return
     */
    static Set<Subtype> createExpectedSubtypes() {
        return Arrays.stream(Subtype.values())
        .filter(s -> !(s == Subtype.none)) // do not expect "none"
        .collect(Collectors.toCollection(
            () -> new TreeSet<>((Subtype s1, Subtype s2)->s1.name().compareTo(s2.name()))));
    }

    /**
     * Get the subtypes that are expected to be handled by this map.
     * (i.e. minus Subtype.none)
     * @return
     */
    public static Collection<Subtype> getExpectedSubtypes() {
        return Collections.unmodifiableCollection(createExpectedSubtypes());
    }

    public static SubtypeToURLMap getInstance(final BufferedReader bufferedReader) {
        return new SubtypeToURLMap(bufferedReader);
    }

    public static SubtypeToURLMap getInstance(final File fn) throws IOException, FileNotFoundException {
        try (InputStream fis = new FileInputStream(fn);
            InputStreamReader inputStreamReader = new InputStreamReader(fis, Charset.forName("UTF-8"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            ) {
            return SubtypeToURLMap.getInstance(bufferedReader);
        }
    }

    /**
     * Fetch from a URL.
     *
     * @param resource
     * @return
     * @throws IOException
     */
    public static SubtypeToURLMap getInstance(URL resource) throws IOException, URISyntaxException {
        if(resource.toString().endsWith(".txt")) {
            // plain text
            Class<?> classes[] = {InputStream.class};
            try (InputStream is  = (InputStream)resource.getContent(classes);
                Reader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                ) {
                return new SubtypeToURLMap(br);
            }
        } else if(resource.getProtocol().equals("http") || resource.getProtocol().equals("https")) {

            Document doc = Jsoup.connect(resource.toString()).get();
            SubtypeToURLMap newMap = getInstance(resource, doc);
            return newMap;
        } else {
            // assume HTML to parse
            Class<?> classes[] = {InputStream.class};
            try (InputStream is  = (InputStream)resource.getContent(classes);
                ) {
                Document doc = Jsoup.parse(is, "UTF-8", resource.toString());
                return getInstance(resource, doc);
            }
        }
    }

    private static SubtypeToURLMap getInstance(URL resource, Document doc) throws IOException {
        StringBuffer sb = new StringBuffer();
        doc.select("div code").forEach(n ->
            n.textNodes()
                .forEach(tn -> sb.append(tn.text()).append('\n')));
        logger.info("Read " + sb.length() + " chars from " + resource.toString());
        try (Reader sr = new StringReader(sb.toString());
            BufferedReader br = new BufferedReader(sr);) {
            return new SubtypeToURLMap(br);
        }
    }
    static final String DEFAULT_URL = "https://cldr.unicode.org/development/subtypes";

    private static String CACHE_SUBTYPE_FILE = "urlmap-cache.txt";

    private final static class SubtypeToURLMapHelper {
        private static final int EXPIRE_DAYS = 1;
        static CLDRCacheDir cacheDir = CLDRCacheDir.getInstance(CLDRCacheDir.CacheType.urlmap);
        static File cacheFile = getCacheFile();
        private static File getCacheFile() {
            return new File(cacheDir.getEmptyDir(), CACHE_SUBTYPE_FILE);
        }
        static SubtypeToURLMap INSTANCE = make(); // not final, may be reloaded.
        static SubtypeToURLMap make() {
            SubtypeToURLMap map = null;
            if(cacheFile.canRead()) {
                Instant fileDate = Instant.ofEpochMilli(cacheFile.lastModified());
                Instant staleAfter = getExpirationDate();
                // after 1 day, try to reload the file.

                try {
                    map = getInstance(cacheFile);
                    logger.info(" Read " + cacheFile.getAbsolutePath() + " - date " + fileDate);
                    if(fileDate.isAfter(staleAfter)) {
                        System.err.println("Cache file stale, will try to reload");
                    } else {
                        return map;
                    }
                } catch (IOException ioe) {
                    System.err.println("Could not initialize SubtypeToURLMap from file " + cacheFile.getAbsolutePath());
                }
            }
            try {
                map = SubtypeToURLMap.getInstance(new URL(getDefaultUrl()));
                logger.info("Read new map from " + getDefaultUrl());
                // now, write out the cache
                writeToCache(map);
            } catch (IOException | URISyntaxException e) {
                logger.warning("Could not initialize SubtypeToURLMap: " + e + " for URL " + getDefaultUrl());
                e.printStackTrace();
                // If we loaded the cache file, we will still use it.
            }
            return map;
        }
        /**
         * The date before which a cache's contents are invalid.
         * @return
         */
        private static Instant getExpirationDate() {
            return cacheDir.getType().getLatestGoodInstant(EXPIRE_DAYS);
        }
        private static void writeToCache(SubtypeToURLMap map) {
            try (PrintWriter pw = new PrintWriter(cacheFile, StandardCharsets.UTF_8.name())) {
                map.write(pw);
                System.out.println("Updated cachefile " + cacheFile.getAbsolutePath());
            } catch ( IOException ioe ) {
                ioe.printStackTrace();
                System.err.println("Error trying to update cachefile: " + ioe);
            }
        }
    }

    /**
     * Fetch the URL used for the default map
     * @return
     */
    public static String getDefaultUrl() {
        return DEFAULT_URL;
    }
    /**
     * Get the default instance.
     * @return
     */
    public static final SubtypeToURLMap getInstance() {
        return SubtypeToURLMapHelper.INSTANCE;
    }

    public static String forSubtype(Subtype subtype) {
        if(SubtypeToURLMapHelper.INSTANCE == null) return null;
        return SubtypeToURLMapHelper.INSTANCE.get(subtype);
    }

    public static SubtypeToURLMap reload() {
        return (SubtypeToURLMapHelper.INSTANCE = SubtypeToURLMapHelper.make());
    }
}
