package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.TreeMultiset;
import com.ibm.icu.util.Output;
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.ChainedMap.M5;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.XPathParts;

public class ListCoverageLevels {

    static final Joiner JOIN_TAB = Joiner.on('\t').useForNull("null");

    enum Locales {
        all,
        modern_cldr,
        specific
    }

    enum Target {
        TC,
        DDL
    }

    private static final Set<String> VALID_REGULAR_UNITS =
            Validity.getInstance().getStatusToCodes(LstrType.unit).get(Validity.Status.regular);

    static final StandardCodes stdCodes = StandardCodes.make();

    static final Set<String> cldrCoverage =
            Sets.difference(
                    stdCodes.getLocaleCoverageLocales(Organization.cldr),
                    stdCodes.getLocaleCoverageLocales(Organization.special));

    private static String levelName(Level level) {
        return level == Level.COMPREHENSIVE ? "ꞏ" + level : level.toString();
    }

    public static void main(String[] args) {
        CLDRConfig config = CLDRConfig.getInstance();
        StandardCodes sc = StandardCodes.make();
        SupplementalDataInfo sdi = config.getSupplementalDataInfo();
        Factory mainAndAnnotationsFactory = config.getMainAndAnnotationsFactory();

        Locales localesToTest = Locales.all;

        Set<String> toTest;
        switch (localesToTest) {
            default:
                toTest = mainAndAnnotationsFactory.getAvailable();
                break;
            case modern_cldr:
                toTest = sc.getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));
                break;
            case specific:
                toTest = ImmutableSortedSet.of("it", "en", "ja", "root");
                break;
        }

        UnitConverter unitConverter = sdi.getUnitConverter();
        Map<String, TargetInfo> conversionData = unitConverter.getInternalConversionData();
        Set<String> shortUnits = new TreeSet<>(conversionData.keySet());

        // mainAndAnnotationsFactory.getAvailable();
        final Set<CLDRLocale> ALL;
        {
            Set<CLDRLocale> _ALL = new LinkedHashSet<>();
            toTest.forEach(locale -> _ALL.add(CLDRLocale.getInstance(locale)));
            ALL = ImmutableSet.copyOf(_ALL);
        }

        Organization foo;

        Map<Target, Map<Level, Multiset<String>>> TcToLevelToCounter = new TreeMap<>();

        for (Target target : Target.values()) {
            Map<Level, Multiset<String>> levelToCounter = new TreeMap<>();
            for (Level level : Level.values()) {
                levelToCounter.put(level, TreeMultiset.create());
            }
            TcToLevelToCounter.put(target, levelToCounter);
        }

        Map<Level, Multiset<String>> unitLevelToCounter = new TreeMap<>();
        Multimap<String, String> unitToLocales = TreeMultimap.create();
        for (Level level : Level.values()) {
            unitLevelToCounter.put(level, TreeMultiset.create());
        }

        for (String locale : toTest) {
            Optional<CLDRLocale> contained =
                    localeOrAncestorMatches(
                            locale, itOrParent -> cldrCoverage.contains(itOrParent.toString()));
            Target target = contained.isPresent() ? Target.TC : Target.DDL;

            CLDRFile file = mainAndAnnotationsFactory.make(locale, false);
            CoverageLevel2 coverageLeveler = null;
            try {
                coverageLeveler = CoverageLevel2.getInstance(locale);
            } catch (Exception e) {
            }
            System.out.println(
                    locale + "\t" + target + "\t" + (target == Target.TC ? contained.get() : ""));
            for (String path : file) {
                Level level =
                        coverageLeveler == null
                                ? Level.COMPREHENSIVE
                                : coverageLeveler.getLevel(path);
                String skeleton = PathStarrer.get(path);
                TcToLevelToCounter.get(target).get(level).add(skeleton);
                if (path.startsWith("//ldml/units/unitLength")
                        && !path.contains("coordinateUnit")
                        && !path.endsWith("/alias")) {
                    XPathParts parts = XPathParts.getFrozenInstance(path);
                    String longUnitId = parts.getAttributeValue(3, "type");
                    // unitLevelToCounter.get(level).add(longUnitId);
                    unitToLocales.put(longUnitId, locale);
                }
            }
        }

        System.out.println("\nSkeletons\n");

        for (Entry<Target, Map<Level, Multiset<String>>> entry0 : TcToLevelToCounter.entrySet()) {
            Target target = entry0.getKey();
            for (Entry<Level, Multiset<String>> entry : entry0.getValue().entrySet()) {
                Level level = entry.getKey();
                Multiset<String> counter = entry.getValue();
                for (Multiset.Entry<String> skeleton : counter.entrySet()) {
                    System.out.println(
                            target
                                    + "\t"
                                    + levelName(level)
                                    + "\t"
                                    + skeleton.getCount()
                                    + "\t"
                                    + skeleton.getElement());
                }
            }
        }

        if (true) return;

        System.out.println("\nUnits\n");
        System.out.println(
                JOIN_TAB.join(
                        "level",
                        "count",
                        "longId",
                        "unitId",
                        "quantity",
                        "base unit",
                        "factor",
                        "systems"));

        Set<String> validAndFound = new TreeSet<>();
        validAndFound.addAll(VALID_REGULAR_UNITS);
        validAndFound.addAll(unitToLocales.keySet());

        for (String longUnitId : validAndFound) {
            String unit = longUnitId;
            showUnit(unitConverter, unitToLocales.get(unit), unit);
            shortUnits.remove(unitConverter.getShortId(unit));
        }

        if (true) return;

        System.out.println("\nDetails\n");

        //        if (true) return;

        M4<Level, String, Attributes, Boolean> data =
                ChainedMap.of(
                        new TreeMap<Level, Object>(),
                        new TreeMap<String, Object>(),
                        new TreeMap<Attributes, Object>(),
                        Boolean.class);
        M5<String, Level, CLDRLocale, List<String>, Boolean> starredToLevels =
                ChainedMap.of(
                        new TreeMap<String, Object>(),
                        new TreeMap<Level, Object>(),
                        new TreeMap<CLDRLocale, Object>(),
                        new HashMap<List<String>, Object>(),
                        Boolean.class);

        // We don't care which items are present in the locale, just what the coverage level is.
        // so we just get the paths from root
        CLDRFile root = mainAndAnnotationsFactory.make("root", false);
        Set<String> testPaths = ImmutableSortedSet.copyOf(root.fullIterable());
        for (String path : testPaths) {
            if (path.endsWith("/alias")) {
                continue;
            }
            String starred = PathStarrer.get(path);
            List<String> plainAttrs = XPathParts.getFrozenInstance(path).getAttributeValues();
            for (String locale : toTest) {
                CLDRLocale cLoc = CLDRLocale.getInstance(locale);
                CoverageLevel2 coverageLeveler = CoverageLevel2.getInstance(locale);
                Level level = coverageLeveler.getLevel(path);
                Attributes attributes = new Attributes(cLoc, plainAttrs);
                data.put(level, starred, attributes, Boolean.TRUE);
                starredToLevels.put(starred, level, cLoc, plainAttrs, Boolean.TRUE);
            }
        }

        System.out.println("ALL=" + getLocaleName(null, ALL));

        for (Entry<String, Map<Level, Map<CLDRLocale, Map<List<String>, Boolean>>>> entry :
                starredToLevels) {
            String starred = entry.getKey();
            Set<String> items = new LinkedHashSet<>();
            for (Entry<Level, Map<CLDRLocale, Map<List<String>, Boolean>>> entry2 :
                    entry.getValue().entrySet()) {
                Level level = entry2.getKey();
                Set<CLDRLocale> locales = new LinkedHashSet<>();
                boolean mixed = false;
                Set<List<String>> lastAttrs = null;
                for (Entry<CLDRLocale, Map<List<String>, Boolean>> entry3 :
                        entry2.getValue().entrySet()) {
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
            System.out.println(starred + "\t" + items.size() + "\t" + Joiner.on(" ").join(items));
        }
        for (Level level : data.keySet()) {
            M3<String, Attributes, Boolean> data2 = data.get(level);
            for (String starred : data2.keySet()) {
                Set<Attributes> attributes = data2.get(starred).keySet();
                Multimap<String, List<String>> localesToAttrs =
                        Attributes.getLocaleNameToAttributeList(ALL, attributes);
                for (Entry<String, Collection<List<String>>> entry :
                        localesToAttrs.asMap().entrySet()) {
                    Collection<List<String>> attrs = entry.getValue();
                    System.out.println(
                            level
                                    + "\t"
                                    + starred
                                    + "\t"
                                    + entry.getKey()
                                    + "\t"
                                    + attrs.size()
                                    + "\t"
                                    + Attributes.compact(attrs, new StringBuilder()));
                }
            }
        }
    }

    private static Optional<CLDRLocale> localeOrAncestorMatches(
            String locale, Predicate<? super CLDRLocale> condition) {
        return Streams.stream(CLDRLocale.getInstance(locale).getParentIterator())
                .filter(condition)
                .findFirst();
    }

    private static void showUnit(
            UnitConverter unitConverter, Collection<String> locales, String longUnitId) {
        String unit = unitConverter.getShortId(longUnitId);
        String systems = "?";
        try {
            systems = unitConverter.getSystems(unit).toString();
        } catch (Exception e) {
        }
        Output<String> baseUnitOut = new Output<>();
        UnitConverter.ConversionInfo conversionInfo = null;
        try {
            conversionInfo = unitConverter.parseUnitId(unit, baseUnitOut, false);
        } catch (Exception e) {
        }

        double factor = conversionInfo == null ? Double.NaN : conversionInfo.factor.doubleValue();

        if (locales == null) {
            locales = Set.of();
        }

        System.out.println(
                JOIN_TAB.join(
                        locales,
                        longUnitId,
                        unit,
                        unitConverter.getQuantityFromUnit(unit, false),
                        baseUnitOut,
                        factor,
                        systems.toString()));
    }

    private static String getLocaleName(Set<CLDRLocale> all, Set<CLDRLocale> locales) {
        Function<Set<CLDRLocale>, String> remainderName =
                x -> {
                    Set<CLDRLocale> y = new LinkedHashSet<>(all);
                    y.removeAll(x);
                    return "AllLcs-(" + Joiner.on("|").join(y) + ")";
                };
        return all == null
                ? Joiner.on("|").join(locales)
                : locales.equals(all)
                        ? "AllLcs"
                        : locales.size() * 2 > all.size()
                                ? remainderName.apply(locales)
                                : Joiner.on("|").join(locales);
    }

    static class Attributes implements Comparable<Attributes> {
        private static final Comparator<Iterable<String>> COLLECTION_COMPARATOR =
                Comparators.lexicographical(Comparator.<String>naturalOrder());
        private final CLDRLocale cLoc;
        private final List<String> attributes;

        public Attributes(CLDRLocale cLoc, List<String> attributes2) {
            this.cLoc = cLoc;
            attributes = ImmutableList.copyOf(attributes2);
        }

        //        public static CharSequence compact(Set<CLDRLocale> all, Set<Attributes>
        // attributeSet) {
        //
        //            Multimap<String, List<String>> localeNameToAttributeList =
        // getLocaleNameToAttributeList(all, attributeSet);
        //
        //            StringBuilder result = new StringBuilder();
        //            // now abbreviate the attributes
        //            boolean first = true;
        //            for (Entry<String, Collection<List<String>>> entry :
        // localeNameToAttributeList.asMap().entrySet()) {
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
        //            return result;
        //        }

        public static StringBuilder compact(
                Collection<List<String>> attrList, StringBuilder result) {
            Map<String, Map> map = getMap(attrList);
            getName(map, result);
            return result;
        }

        public static Multimap<String, List<String>> getLocaleNameToAttributeList(
                Set<CLDRLocale> all, Set<Attributes> attributeSet) {
            Multimap<String, List<String>> localeNameToAttributeList =
                    TreeMultimap.create(Comparator.naturalOrder(), COLLECTION_COMPARATOR);
            {
                Multimap<List<String>, CLDRLocale> attributesToLocales =
                        TreeMultimap.create(COLLECTION_COMPARATOR, Comparator.naturalOrder());
                int count = 0;
                for (Attributes attributes : attributeSet) {
                    count = attributes.attributes.size();
                    attributesToLocales.put(attributes.attributes, attributes.cLoc);
                }
                if (count > 1) {
                    int debug = 0;
                }

                for (Entry<List<String>, Collection<CLDRLocale>> entry :
                        attributesToLocales.asMap().entrySet()) {
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

        private static <T, U extends Iterable<T>, V extends Iterable<U>> Map<T, Map> getMap(
                V source) {
            if (!source.iterator().hasNext()) {
                return Collections.emptyMap();
            }
            Map<T, Map> items = new LinkedHashMap<>();
            for (Iterable<T> list : source) {
                Map<T, Map> top = items;
                for (T item : list) {
                    Map<T, Map> value = top.get(item);
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
            return attributes.isEmpty()
                    ? cLoc.toString()
                    : cLoc + "|" + Joiner.on("|").join(attributes);
        }
    }
}
