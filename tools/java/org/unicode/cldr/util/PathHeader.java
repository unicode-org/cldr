package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.With.SimpleIterator;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ULocale;

public class PathHeader implements Comparable<PathHeader> {
    public enum SurveyToolStatus {
        DEPRECATED,
        HIDE,
        READ_ONLY,
        READ_WRITE
    }

    private final String              section;
    private final String              page;
    private final String              header;
    private final String              code;
    private final String              originalPath;
    private final SurveyToolStatus    status;

    // Used for ordering
    private final int                 sectionOrder;
    private final int                 pageOrder;
    private final int                 headerOrder;
    private final int                 codeOrder;

    static final Pattern              SEMI                 = Pattern.compile("\\s*;\\s*");
    static final Matcher              ALT_MATCHER          = Pattern.compile(
                                                                   "\\[@alt=\"([^\"]*+)\"]")
                                                                   .matcher("");

    static final RuleBasedCollator    alphabetic           = (RuleBasedCollator) Collator
                                                                   .getInstance(ULocale.ENGLISH);
    static {
        alphabetic.setNumericCollation(true);
    }
    static final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
    static final Map<String, String>  metazoneToContinent  = supplementalDataInfo
                                                                   .getMetazoneToContinentMap();
    static final StandardCodes        standardCode         = StandardCodes.make();

    /**
     * @param section
     * @param sectionOrder
     * @param page
     * @param pageOrder
     * @param header
     * @param headerOrder
     * @param code
     * @param codeOrder
     * @param status 
     */
    private PathHeader(String section, int sectionOrder, String page, int pageOrder, String header,
            int headerOrder, String code, int codeOrder, SurveyToolStatus status, String originalPath) {
        this.section = section;
        this.sectionOrder = sectionOrder;
        this.page = page;
        this.pageOrder = pageOrder;
        this.header = header;
        this.headerOrder = headerOrder;
        this.code = code;
        this.codeOrder = codeOrder;
        this.originalPath = originalPath;
        this.status = status;
    }

    /**
     * Return a factory for use in creating the headers. This should be cached.
     * The calls are thread-safe. The englishFile sets a static for now; after the first time, null can be passed.
     * 
     * @param englishFile
     */
    public static Factory getFactory(CLDRFile englishFile) {
        return new Factory(englishFile);
    }

    public String getSection() {
        return section;
    }

    public String getPage() {
        return page;
    }

    public String getHeader() {
        return header;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public SurveyToolStatus getSurveyToolStatus() {
        return status;
    }

    @Override
    public String toString() {
        return section + "\t" + sectionOrder + "\t"
                + page + "\t" + pageOrder + "\t"
                + header + "\t" + headerOrder + "\t"
                + code + "\t" + codeOrder + "";
    }

    @Override
    public int compareTo(PathHeader other) {
        // Within each section, order alphabetically if the integer orders are
        // not different.
        int result;
        if (0 != (result = sectionOrder - other.sectionOrder)) {
            return result;
        }
        if (0 != (result = alphabetic.compare(section, other.section))) {
            return result;
        }
        if (0 != (result = pageOrder - other.pageOrder)) {
            return result;
        }
        if (0 != (result = alphabetic.compare(page, other.page))) {
            return result;
        }
        if (0 != (result = headerOrder - other.headerOrder)) {
            return result;
        }
        if (0 != (result = alphabetic.compare(header, other.header))) {
            return result;
        }
        if (0 != (result = codeOrder - other.codeOrder)) {
            return result;
        }
        if (0 != (result = alphabetic.compare(code, other.code))) {
            return result;
        }
        if (0 != (result = alphabetic.compare(originalPath, other.originalPath))) {
            return result;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        PathHeader other;
        try {
            other = (PathHeader) obj;
        } catch (Exception e) {
            return false;
        }
        return section.equals(other.section) && page.equals(other.page)
                && header.equals(other.header) && code.equals(other.code)
                && originalPath.equals(other.originalPath);
    }

    @Override
    public int hashCode() {
        return originalPath.hashCode();
    }

    public static class Factory {
        static final RegexLookup<RawData>                  lookup                     = RegexLookup
                                                                                              .of(new PathHeaderTransform())
                                                                                              .setPatternTransform(
                                                                                                      RegexLookup.RegexFinderTransformPath)
                                                                                              .loadFromFile(
                                                                                                      PathHeader.class,
                                                                                                      "data/PathHeader.txt");
        // synchronized with lookup
        static final Output<String[]>                      args                       = new Output<String[]>();
        // synchronized with lookup
        static final Counter<RawData>                      counter                    = new Counter<RawData>();
        // synchronized with lookup
        static final Map<RawData, String>                  samples                    = new HashMap<RawData, String>();
        // synchronized with lookup
        static int                                         order;

        static final Map<String, PathHeader>               cache                      = new HashMap<String, PathHeader>();
        // synchronized with cache
        static final Map<String, Map<String, SectionPage>> sectionToPageToSectionPage = new HashMap<String, Map<String, SectionPage>>();
        static final Relation<SectionPage, String>         sectionPageToPaths         = Relation
                                                                                              .of(new TreeMap<SectionPage, Set<String>>(),
                                                                                                      HashSet.class);

        private static CLDRFile                            englishFile;

        /**
         * Create a factory for creating PathHeaders.
         * @param englishFile
         *            - only sets the file (statically!) if not already set.
         */
        private Factory(CLDRFile englishFile) {
            setEnglishCLDRFileIfNotSet(englishFile); // temporary
        }

        /**
         * Returns true if we set it, false if set before.
         * 
         * @param englishFile2
         * @return
         */
        private static boolean setEnglishCLDRFileIfNotSet(CLDRFile englishFile2) {
            synchronized (Factory.class) {
                if (englishFile != null) {
                    return false;
                }
                englishFile = englishFile2;
                return true;
            }
        }

        /**
         * Return the PathHeader for a given path. Thread-safe.
         */
        public PathHeader fromPath(String distinguishingPath) {
            if (distinguishingPath == null) {
                throw new NullPointerException("Path cannot be null");
            }
            synchronized (cache) {
                PathHeader old = cache.get(distinguishingPath);
                if (old != null) {
                    return old;
                }
            }
            synchronized (lookup) {
                // special handling for alt
                String alt = null;
                int altPos = distinguishingPath.indexOf("[@alt=");
                if (altPos >= 0) {
                    if (ALT_MATCHER.reset(distinguishingPath).find()) {
                        distinguishingPath = distinguishingPath.substring(0, ALT_MATCHER.start())
                                + distinguishingPath.substring(ALT_MATCHER.end());
                        alt = ALT_MATCHER.group(1);
                        int pos = alt.indexOf("proposed");
                        if (pos >= 0) {
                            alt = pos == 0 ? null : alt.substring(0, pos - 1); 
                            // drop "proposed",
                            // change "xxx-proposed" to xxx.
                        }
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
                RawData data = lookup.get(distinguishingPath, null, args);
                if (data == null) {
                    return null;
                }
                counter.add(data, 1);
                if (!samples.containsKey(data)) {
                    samples.put(data, distinguishingPath);
                }
                try {
                    PathHeader result = new PathHeader(
                            fix(data.section, data.sectionOrder), order,
                            fix(data.page, data.pageOrder), order,
                            fix(data.header, data.headerOrder), order,
                            fix(data.code + (alt == null ? "" : "-" + alt), data.codeOrder), order,
                            data.status,
                            distinguishingPath);
                    synchronized (cache) {
                        PathHeader old = cache.get(distinguishingPath);
                        if (old == null) {
                            cache.put(distinguishingPath, result);
                        } else {
                            result = old;
                        }
                        Map<String, SectionPage> pageToPathHeaders = sectionToPageToSectionPage
                                .get(result.section);
                        if (pageToPathHeaders == null) {
                            sectionToPageToSectionPage.put(result.section, pageToPathHeaders
                                    = new HashMap<String, SectionPage>());
                        }
                        SectionPage sectionPage = pageToPathHeaders.get(result.page);
                        if (sectionPage == null) {
                            pageToPathHeaders.put(result.page, sectionPage
                                    = new SectionPage(result.section, result.sectionOrder,
                                            result.page, result.pageOrder));
                        }
                        sectionPageToPaths.put(sectionPage, distinguishingPath);
                    }
                    return result;
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Probably too few capturing groups in regex for " + distinguishingPath,
                            e);
                }
            }
        }

        private static class SectionPage implements Comparable<SectionPage> {
            private final String section;
            private final String page;
            // Used for ordering
            private final int    sectionOrder;
            private final int    pageOrder;

            public SectionPage(String section, int sectionOrder, String page, int pageOrder) {
                this.section = section;
                this.sectionOrder = sectionOrder;
                this.page = page;
                this.pageOrder = pageOrder;
            }

            @Override
            public int compareTo(SectionPage other) {
                // Within each section, order alphabetically if the integer
                // orders are
                // not different.
                int result;
                if (0 != (result = sectionOrder - other.sectionOrder)) {
                    return result;
                }
                if (0 != (result = alphabetic.compare(section, other.section))) {
                    return result;
                }
                if (0 != (result = pageOrder - other.pageOrder)) {
                    return result;
                }
                if (0 != (result = alphabetic.compare(page, other.page))) {
                    return result;
                }
                return 0;
            }

            @Override
            public boolean equals(Object obj) {
                PathHeader other;
                try {
                    other = (PathHeader) obj;
                } catch (Exception e) {
                    return false;
                }
                return section.equals(other.section) && page.equals(other.page);
            }

            @Override
            public int hashCode() {
                return section.hashCode() ^ page.hashCode();
            }
        }

        /**
         * Returns a set of paths currently associated with the given section
         * and page.
         * <p>
         * <b>Warning:</b>
         * <ol>
         * <li>The set may not be complete for a cldrFile unless all of paths in
         * the file have had fromPath called. And this includes getExtraPaths().
         * </li>
         * <li>The set may include paths that have no value in the current
         * cldrFile.</li>
         * <li>The set may be empty, if the section/page aren't valid.</li>
         * </ol>
         * Thread-safe.
         * 
         * @target a collection where the paths are to be returned.
         */
        public static Set<String> getCachedPaths(String section, String page) {
            Set<String> target = new HashSet<String>();
            synchronized (cache) {
                Map<String, SectionPage> pageToSectionPage = sectionToPageToSectionPage
                        .get(section);
                if (pageToSectionPage == null) {
                    return null;
                }
                SectionPage sectionPage = pageToSectionPage.get(page);
                if (sectionPage == null) {
                    return null;
                }
                Set<String> set = sectionPageToPaths.getAll(sectionPage);
                target.addAll(set);
            }
            return target;
        }

        /**
         * Returns a set of section/pages currently cached.
         * <p>
         * <b>Warning:</b>
         * <ol>
         * <li>The set may not be complete for a cldrFile unless all of paths in
         * the file have had fromPath called. And this includes getExtraPaths().
         * </li>
         * </ol>
         * Thread-safe.
         * 
         * @target a collection where the paths are to be returned.
         */
        public static Relation<String, String> getCachedSectionToPages() {
            Relation<String, String> target = Relation.of(new LinkedHashMap<String, Set<String>>(),
                    LinkedHashSet.class);
            synchronized (cache) {
                for (SectionPage sectionPage : sectionPageToPaths.keySet()) {
                    target.put(sectionPage.section, sectionPage.page);
                }
            }
            return target;
        }

        /**
         * Return the Sections and Pages that are in use, for display in menus.
         * Both are ordered.
         * 
         * @deprecated Use getCachedSectionToPages instead. Make sure that you
         *             process the paths first.
         */
        public static LinkedHashMap<String, Set<String>> getSectionsToPages() {
            LinkedHashMap<String, Set<String>> sectionsToPages = new LinkedHashMap<String, Set<String>>();
            synchronized (lookup) {
                for (R2<Finder, RawData> foo : lookup) {
                    RawData data = foo.get1();
                    Set<String> pages = sectionsToPages.get(data.section);
                    if (pages == null) {
                        sectionsToPages.put(data.section, pages = new LinkedHashSet<String>());
                    }
                    // Special Hack for Metazones and Calendar
                    // We could make this more general by having a
                    // TransformWithRange.getValues() for each function. Not
                    // worth
                    // doing right now.
                    if (!data.page.contains("&")) {
                        pages.add(data.page);
                    } else if (data.page.contains("metazone")) {
                        pages.addAll(new TreeSet<String>(metazoneToContinent.values()));
                    } else if (data.page.contains("calendar")) {
                        Set<String> calendars = supplementalDataInfo.getBcp47Keys().get("ca");
                        Transform<String, String> calendarFunction = functionMap.get("calendar");
                        for (String calendar : calendars) {
                            pages.add(calendarFunction.transform(calendar));
                        }
                    }
                }
            }
            return sectionsToPages;
        }

        public Iterable<String> filterCldr(String section, String page, CLDRFile file) {
            return new FilteredIterable(section, page, file);
        }

        private class FilteredIterable implements Iterable<String>, SimpleIterator<String> {
            private final String           section;
            private final String           page;
            private final CLDRFile         file;
            private final Iterator<String> fileIterator;
            private Iterator<String>       extraPaths;

            FilteredIterable(String section, String page, CLDRFile file) {
                this.section = section;
                this.page = page;
                this.file = file;
                this.fileIterator = file.iterator();
            }

            @Override
            public Iterator<String> iterator() {
                return With.toIterator(this);
            }

            @Override
            public String next() {
                while (fileIterator.hasNext()) {
                    String path = fileIterator.next();
                    PathHeader pathHeader = fromPath(path);
                    if (section.equals(pathHeader.section) && page.equals(pathHeader.page)) {
                        return path;
                    }
                }
                if (extraPaths == null) {
                    extraPaths = file.getExtraPaths().iterator();
                }
                while (extraPaths.hasNext()) {
                    String path = extraPaths.next();
                    PathHeader pathHeader = fromPath(path);
                    if (section.equals(pathHeader.section) && page.equals(pathHeader.page)) {
                        return path;
                    }
                }
                return null;
            }
        }

        private static class ChronologicalOrder {
            private Map<String, Integer> map = new HashMap<String, Integer>();
            private String               item;
            private int                  order;
            private ChronologicalOrder   toClear;

            ChronologicalOrder(ChronologicalOrder toClear) {
                this.toClear = toClear;
            }

            int getOrder() {
                return order;
            }

            public String set(String itemToOrder) {
                if (itemToOrder.startsWith("*")) {
                    item = itemToOrder.substring(1, itemToOrder.length());
                    return item; // keep old order
                }
                item = itemToOrder;
                Integer old = map.get(item);
                if (old != null) {
                    order = old.intValue();
                } else {
                    order = map.size();
                    map.put(item, order);
                    clearLower();
                }
                return item;
            }

            private void clearLower() {
                if (toClear != null) {
                    toClear.map.clear();
                    toClear.order = 0;
                    toClear.clearLower();
                }
            }
        }

        static class RawData {
            static ChronologicalOrder codeOrdering    = new ChronologicalOrder(null);
            static ChronologicalOrder headerOrdering  = new ChronologicalOrder(codeOrdering);
            static ChronologicalOrder pageOrdering    = new ChronologicalOrder(headerOrdering);
            static ChronologicalOrder sectionOrdering = new ChronologicalOrder(pageOrdering);

            public RawData(String source) {
                String[] split = SEMI.split(source);
                section = sectionOrdering.set(split[0]);
                sectionOrder = sectionOrdering.getOrder();

                page = pageOrdering.set(split[1]);
                pageOrder = pageOrdering.getOrder();

                header = headerOrdering.set(split[2]);
                headerOrder = headerOrdering.getOrder();

                code = codeOrdering.set(split[3]);
                codeOrder = codeOrdering.getOrder();
                
                status = split.length < 5 ? SurveyToolStatus.READ_WRITE : SurveyToolStatus.valueOf(split[4]);
            }

            public final String section;
            public final int    sectionOrder;
            public final String page;
            public final int    pageOrder;
            public final String header;
            public final int    headerOrder;
            public final String code;
            public final int    codeOrder;
            public final SurveyToolStatus status;

            @Override
            public String toString() {
                return section + "\t" + sectionOrder + "\t"
                        + page + "\t" + pageOrder + "\t"
                        + header + "\t" + headerOrder + "\t"
                        + code + "\t" + codeOrder + "\t"
                        + status;
            }
        }

        static class PathHeaderTransform implements Transform<String, RawData> {
            @Override
            public RawData transform(String source) {
                return new RawData(source);
            }
        }

        /**
         * Internal data, for testing and debugging.
         */
        public class CounterData extends Row.R4<String, RawData, String, String> {
            public CounterData(String a, RawData b, String c) {
                super(a, b, c == null ? "no sample" : c, c == null ? "no sample" : fromPath(c)
                        .toString());
            }
        }

        /**
         * Get the internal data, for testing and debugging.
         */
        public Counter<CounterData> getInternalCounter() {
            synchronized (lookup) {
                Counter<CounterData> result = new Counter<CounterData>();
                for (R2<Finder, RawData> foo : lookup) {
                    Finder finder = foo.get0();
                    RawData data = foo.get1();
                    long count = counter.get(data);
                    result.add(new CounterData(finder.toString(), data, samples.get(data)), count);
                }
                return result;
            }
        }

        static Map<String, Transform<String, String>> functionMap    = new HashMap<String, Transform<String, String>>();
        static String[]                               months         = { "Jan", "Feb", "Mar",
                                                                             "Apr", "May", "Jun",
                                                                             "Jul", "Aug", "Sep",
                                                                             "Oct", "Nov", "Dec",
                                                                             "Und" };
        static List<String>                           days           = Arrays.asList("sun", "mon",
                                                                             "tue", "wed", "thu",
                                                                             "fri", "sat");
        // static Map<String, String> likelySubtags =
        // supplementalDataInfo.getLikelySubtags();
        static LikelySubtags                          likelySubtags  = new LikelySubtags();
        static HyphenSplitter                         hyphenSplitter = new HyphenSplitter();
        static Transform<String, String>              catFromTerritory;
        static {
            functionMap.put("month", new Transform<String, String>() {
                public String transform(String source) {
                    int m = Integer.parseInt(source);
                    order = m;
                    return months[m - 1];
                }
            });
            functionMap.put("day", new Transform<String, String>() {
                public String transform(String source) {
                    int m = days.indexOf(source);
                    order = m;
                    return source;
                }
            });
            functionMap.put("metazone", new Transform<String, String>() {
                public String transform(String source) {
                    String continent = metazoneToContinent.get(source);
                    return continent;
                }
            });
            functionMap.put("calendar", new Transform<String, String>() {
                Map<String, String> fixNames = Builder.with(new HashMap<String, String>())
                                                     .put("islamicc", "Islamic Civil")
                                                     .put("roc", "ROC")
                                                     .put("Ethioaa", "Ethiopic Amete Alem")
                                                     .put("Gregory", "Gregorian")
                                                     .put("iso8601", "ISO 8601")
                                                     .freeze();

                public String transform(String source) {
                    String result = fixNames.get(source);
                    return result != null ? result : UCharacter.toTitleCase(source, null);
                }
            });
            functionMap.put("titlecase", new Transform<String, String>() {
                public String transform(String source) {
                    return UCharacter.toTitleCase(source, null);
                }
            });
            functionMap.put("categoryFromScript", new Transform<String, String>() {
                public String transform(String source) {
                    String script = hyphenSplitter.split(source);
                    Info info = ScriptMetadata.getInfo(script);
                    if (info == null) {
                        info = ScriptMetadata.getInfo("Zzzz");
                    }
                    order = 100 - info.idUsage.ordinal();
                    return info.idUsage.name;
                }
            });
            functionMap.put("scriptFromLanguage", new Transform<String, String>() {
                public String transform(String source0) {
                    String language = hyphenSplitter.split(source0);
                    String script = likelySubtags.getLikelyScript(language);
                    if (script == null) {
                        script = likelySubtags.getLikelyScript(language);
                    }
                    String scriptName = englishFile.getName(CLDRFile.SCRIPT_NAME, script);
                    return script.equals("Hans") || script.equals("Hant") ? "Han Script"
                            : scriptName.endsWith(" Script") ? scriptName
                                    : scriptName + " Script";
                }
            });
            functionMap.put("categoryFromTerritory",
                    catFromTerritory = new Transform<String, String>() {
                        Relation<String, String> containmentCore          = supplementalDataInfo
                                                                                  .getContainmentCore();
                        Relation<String, String> containmentFull          = supplementalDataInfo
                                                                                  .getTerritoryToContained();
                        Relation<String, String> containedToContainer     = Relation
                                                                                  .of(new HashMap<String, Set<String>>(),
                                                                                          HashSet.class)
                                                                                  .addAllInverted(
                                                                                          containmentFull);
                        Relation<String, String> containedToContainerCore = Relation
                                                                                  .of(new HashMap<String, Set<String>>(),
                                                                                          HashSet.class)
                                                                                  .addAllInverted(
                                                                                          containmentCore);
                        Map<String, Integer>     toOrder                  = new LinkedHashMap<String, Integer>();
                        int                      level                    = 0;
                        {
                            getOrder("001");
                            System.out.println(order);
                        }

                        public String transform(String source) {
                            String territory = hyphenSplitter.split(source);
                            Set<String> containers = containedToContainerCore.get(territory);
                            if (containers == null) {
                                containers = containedToContainer.get(territory);
                            }
                            String container = containers != null ? containers.iterator().next()
                                    : territory.equals("001") ? "001" : "ZZ";
                            Integer temp = toOrder.get(container);
                            order = temp != null ? temp.intValue() : level;
                            return TestInfo.getInstance().getEnglish()
                                    .getName(CLDRFile.TERRITORY_NAME, container);
                        }

                        private void getOrder(String source) {
                            if (toOrder.containsKey(source)) {
                                return;
                            }
                            toOrder.put(source, ++level);
                            Set<String> contained = containmentCore.get(source);
                            if (contained == null) {
                                return;
                            }
                            for (String subitem : contained) {
                                getOrder(subitem);
                            }
                        }
                    });
            functionMap.put("categoryFromTimezone", new Transform<String, String>() {
                Map<String, String> zone2country = StandardCodes.make().getZoneToCounty();

                public String transform(String source0) {
                    String territory = zone2country.get(source0);
                    if (territory == null) {
                        territory = "ZZ";
                    }
                    return catFromTerritory.transform(territory);
                }
            });
            functionMap.put("categoryFromCurrency", new Transform<String, String>() {
                public String transform(String source0) {
                    String territory = likelySubtags.getLikelyTerritoryFromCurrency(source0);
                    if (territory == null || territory.equals("ZZ")) {
                        order = 999;
                        return "Not Current Tender";
                    }
                    return catFromTerritory.transform(territory)
                            + ": "
                            + TestInfo.getInstance().getEnglish()
                                    .getName(CLDRFile.TERRITORY_NAME, territory);
                }
            });
        }

        static class HyphenSplitter {
            String main;
            String extras;

            String split(String source) {
                int hyphenPos = source.indexOf('-');
                if (hyphenPos < 0) {
                    main = source;
                    extras = "";
                } else {
                    main = source.substring(0, hyphenPos);
                    extras = source.substring(hyphenPos);
                }
                return main;
            }
        }

        /**
         * This converts "functions", like &month, and sets the order.
         * 
         * @param input
         * @param order
         * @return
         */
        private static String fix(String input, int orderIn) {
            input = RegexLookup.replace(input, args.value);
            order = orderIn;
            int functionStart = input.indexOf("&");
            if (functionStart >= 0) {
                int functionEnd = input.indexOf('(', functionStart);
                int argEnd = input.indexOf(')', functionEnd);
                Transform<String, String> func = functionMap.get(input.substring(functionStart + 1,
                        functionEnd));
                String temp = func.transform(input.substring(functionEnd + 1, argEnd));
                input = input.substring(0, functionStart) + temp + input.substring(argEnd + 1);
            }
            return input;
        }
    }

}
