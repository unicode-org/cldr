package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.ChainedMap.M5;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;

public class ListCoverageLevels {
    public static void main(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        StandardCodes sc = config.getStandardCodes();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> defaultContents = sdi.getDefaultContentLocales();
        PathStarrer starrer = new PathStarrer().setSubstitutionPattern("*");
        Factory mainAndAnnotationsFactory = config.getMainAndAnnotationsFactory();

        Set<String> toTest = sc.getLocaleCoverageLocales(Organization.cldr, EnumSet.allOf(Level.class));
        // ImmutableSortedSet.of("it", "root", "ja"); 
        // mainAndAnnotationsFactory.getAvailable();
        final Set<CLDRLocale> ALL;
        {
            Set<CLDRLocale> _ALL = new LinkedHashSet<>();
            toTest.forEach(locale -> _ALL.add(CLDRLocale.getInstance(locale)));
            ALL = ImmutableSet.copyOf(_ALL);
        }

        M4<Level, String, Attributes, Boolean> data = ChainedMap.of(
            new TreeMap<Level,Object>(),
            new TreeMap<String,Object>(), 
            new TreeMap<Attributes,Object>(),
            Boolean.class);
         M5<String, Level, CLDRLocale, List<String>, Boolean> starredToLevels = ChainedMap.of(
            new TreeMap<String,Object>(),
            new TreeMap<Level,Object>(), 
            new TreeMap<CLDRLocale,Object>(),
            new HashMap<List<String>,Object>(),
            Boolean.class);

        // We don't care which items are present in the locale, just what the coverage level is.
        // so we just get the paths from root
        CLDRFile root = mainAndAnnotationsFactory.make("root", false);
        Set<String> testPaths = ImmutableSortedSet.copyOf(root.fullIterable());
        for (String path : testPaths) {
            if (path.endsWith("/alias")) {
                continue;
            }
            String starred = starrer.set(path);
            List<String> plainAttrs = starrer.getAttributes();
            for (String locale : toTest) {
                CLDRLocale cLoc = CLDRLocale.getInstance(locale);
                CoverageLevel2 coverageLeveler = CoverageLevel2.getInstance(locale);
                Level level = coverageLeveler.getLevel(path);
                Attributes attributes = new Attributes(cLoc, plainAttrs);
                data.put(level, starred, attributes, Boolean.TRUE);
                starredToLevels.put(starred, level, cLoc, plainAttrs, Boolean.TRUE);
            }
        }

//        for (String locale : toTest) {
//            if (!ltp.set(locale).getRegion().isEmpty()
//                //  || locale.equals("root")
//                || locale.equals("ceb")
//                || defaultContents.contains(locale)) {
//                continue;
//            }
//            CLDRLocale cLoc = CLDRLocale.getInstance(locale);
//            ALL.add(cLoc);
//            CoverageLevel2 coverageLeveler = CoverageLevel2.getInstance(locale);
//            //Level desiredLevel = sc.getLocaleCoverageLevel(Organization.cldr, locale);
//            CLDRFile testFile = mainAndAnnotationsFactory.make(locale, false);
//            for (String path : testFile.fullIterable()) {
//                Level level = coverageLeveler.getLevel(path);
//                String starred = starrer.set(path);
//                Attributes attributes = new Attributes(cLoc, starrer.getAttributes());
//                data.put(level, starred, attributes, Boolean.TRUE);
//                starredToLevels.put(starred, level, cLoc, Boolean.TRUE);
//            }
//        }
        
        System.out.println("ALL=" + getLocaleName(null, ALL));

        for (Entry<String, Map<Level, Map<CLDRLocale, Map<List<String>, Boolean>>>> entry : starredToLevels) {
            String starred = entry.getKey();
            Set<String> items = new LinkedHashSet<>();
            for (Entry<Level, Map<CLDRLocale, Map<List<String>, Boolean>>> entry2 : entry.getValue().entrySet()) {
                Level level = entry2.getKey();
                Set<CLDRLocale> locales = new LinkedHashSet<>();
                boolean mixed = false;
                Set<List<String>> lastAttrs = null;
                for (Entry<CLDRLocale, Map<List<String>, Boolean>> entry3 : entry2.getValue().entrySet()) {
                    CLDRLocale locale = entry3.getKey();
                    Set<List<String>> attrs = entry3.getValue().keySet();
                    if (lastAttrs != null && !attrs.equals(lastAttrs)) {
                        mixed = true;
                    }
                    lastAttrs = attrs;
                    locales.add(locale);
                }
                if (mixed == false) {
                    int debug = 0;
                }
                String localeName = getLocaleName(ALL, locales);
                items.add(level + ":" + (mixed ? "" : "°") + localeName);
            }
            System.out.println(starred + "\t" + items.size() + "\t" + CollectionUtilities.join(items, " "));
        }
        for (Level level : data.keySet()) {
            M3<String, Attributes, Boolean> data2 = data.get(level);
            for (String starred : data2.keySet()) {
                Set<Attributes> attributes = data2.get(starred).keySet();
                Multimap<String, List<String>> localesToAttrs = Attributes.getLocaleNameToAttributeList(ALL, attributes);
                for (Entry<String, Collection<List<String>>> entry : localesToAttrs.asMap().entrySet()) {
                    Collection<List<String>> attrs = entry.getValue();
                    System.out.println(level 
                        + "\t" + starred 
                        + "\t" + entry.getKey() 
                        + "\t" + attrs.size() 
                        + "\t" + Attributes.compact(attrs, new StringBuilder()));
                }
            }
        }
    }

    private static String getLocaleName(Set<CLDRLocale> all, Set<CLDRLocale> locales) {
        Function<Set<CLDRLocale>,String> remainderName = x -> {
            Set<CLDRLocale> y = new LinkedHashSet<>(all);
            y.removeAll(x);
            return "AllLcs-(" + CollectionUtilities.join(y, "|") + ")";
        };
        return all == null ? CollectionUtilities.join(locales, "|")
            : locales.equals(all) ? "AllLcs" 
                : locales.size()*2 > all.size() ? remainderName.apply(locales)
                    : CollectionUtilities.join(locales, "|");
    }

    static class Attributes implements Comparable<Attributes>{
        private static final Comparator<Iterable<String>> COLLECTION_COMPARATOR = Comparators.lexicographical(Comparator.<String>naturalOrder());
        private final CLDRLocale cLoc;
        private final List<String> attributes;

        public Attributes(CLDRLocale cLoc, List<String> attributes2) {
            this.cLoc = cLoc;
            attributes = ImmutableList.copyOf(attributes2);
        }

//        public static CharSequence compact(Set<CLDRLocale> all, Set<Attributes> attributeSet) {
//
//            Multimap<String, List<String>> localeNameToAttributeList = getLocaleNameToAttributeList(all, attributeSet);
//
//            StringBuilder result = new StringBuilder();
//            // now abbreviate the attributes
//            boolean first = true;
//            for (Entry<String, Collection<List<String>>> entry : localeNameToAttributeList.asMap().entrySet()) {
//                if (!first) {
//                    result.append(' ');
//                } else {
//                    first = false;
//                }
//                result.append(entry.getKey());
//                Collection<List<String>> attrList = entry.getValue();
//                Map<String, Map> map = getMap(attrList);
//                getName(map, result);
//            }            
//
//
////            localeNameToAttributeList.forEach((name,list) -> {
////                if (result.length() != 0) {
////                    result.append(' ');
////                }
////                result.append(name);
////                if (!list.isEmpty()) {
////                    result.append(':').append(CollectionUtilities.join(list, "|"));
////                }
////                });
//            return result;
//        }

        public static StringBuilder compact(Collection<List<String>> attrList, StringBuilder result) {
            Map<String, Map> map = getMap(attrList);
            getName(map, result);
            return result;
        }


        public static Multimap<String, List<String>> getLocaleNameToAttributeList(Set<CLDRLocale> all, Set<Attributes> attributeSet) {
            Multimap<String,List<String>> localeNameToAttributeList = TreeMultimap.create(Comparator.naturalOrder(), COLLECTION_COMPARATOR);
            {
                Multimap<List<String>,CLDRLocale> attributesToLocales = TreeMultimap.create(COLLECTION_COMPARATOR, Comparator.naturalOrder());
                int count = 0;
                for (Attributes attributes : attributeSet) {
                    count = attributes.attributes.size();
                    attributesToLocales.put(attributes.attributes, attributes.cLoc);
                }
                if (count > 1) {
                    int debug = 0;
                }

                for (Entry<List<String>, Collection<CLDRLocale>> entry : attributesToLocales.asMap().entrySet()) {
                    List<String> attributeList = entry.getKey();
                    Set<CLDRLocale> locales = (Set<CLDRLocale>) entry.getValue();
                    String localeName = getLocaleName(all, locales);
                    localeNameToAttributeList.put(localeName, attributeList);
                }
            }
            return localeNameToAttributeList;
        }

        private static void getName(Map<String, Map> map, StringBuilder result) {
            if (map.isEmpty()) {
                return;
            }
            result.append("(");
            boolean first = true;
            for (Entry<String, Map> entry : map.entrySet()) {
                if (!first) {
                    result.append('|');
                } else {
                    first = false;
                }
                result.append(entry.getKey());
                getName(entry.getValue(), result);
            }
            result.append(")");
        }

        private static <T, U extends Iterable<T>, V extends Iterable<U>> Map<T, Map> getMap(V source) {
            if (!source.iterator().hasNext()) {
                return Collections.emptyMap();
            }
            Map<T,Map> items = new LinkedHashMap<>();
            for (Iterable<T> list : source) {
                Map<T, Map> top = items;
                for (T item : list) {
                    Map<T, Map> value = (Map<T, Map>) top.get(item);
                    if (value == null) {
                        top.put(item, value = new LinkedHashMap<>());
                    }
                    top = value;
                }
            }
            return items;
        }

        @Override
        public int compareTo(Attributes o) {
            return ComparisonChain.start()
                .compare(cLoc, o.cLoc)
                .compare(attributes, o.attributes, COLLECTION_COMPARATOR)
                .result();
        }
        @Override
        public String toString() {
            return attributes.isEmpty() ? cLoc.toString() : cLoc + "|" + CollectionUtilities.join(attributes, "|");
        }
    }
}