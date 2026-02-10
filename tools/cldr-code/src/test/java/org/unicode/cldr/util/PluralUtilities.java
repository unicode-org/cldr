package org.unicode.cldr.util;

import static java.util.Comparator.comparing;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.DecimalQuantitySamples;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.PluralRules.SampleType;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.PluralUtilities.KeySampleRanges;
import org.unicode.cldr.util.PluralUtilities.SampleRange.SameIntegerCount;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class PluralUtilities {
    public static final Pattern OBSOLETE_SYNTAX = Pattern.compile("(mod|in|is|within)");
    public static final Pattern RELATION = Pattern.compile("(!=|=|%)");
    public static final Pattern RELATION_PARTS =
            Pattern.compile(
                    "([a-z])"
                            + "\\s*"
                            + "(?:%\\s*(1[0]+))?"
                            + "\\s*"
                            + "(!=|=)"
                            + "\\s*"
                            + "([0-9.,]+)");

    private static final Splitter AND_SPLITTER =
            Splitter.on(Pattern.compile("\\band\\b")).trimResults();
    private static final Splitter OR_SPLITTER =
            Splitter.on(Pattern.compile("\\bor\\b")).trimResults();

    private static final boolean DEBUG = System.getProperty("PluralUtilities") != null;

    private static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();

    public static final MapComparator<String> PLURAL_OPERAND =
            new MapComparator<String>()
                    .add("n", "i", "v", "w", "f", "t", "c", "e")
                    .setErrorOnMissing(false)
                    .freeze();

    public static final Comparator<String> PLURAL_RELATION =
            comparing(
                            (String foo) ->
                                    foo == null || foo.isEmpty() ? " " : foo.substring(0, 1),
                            PLURAL_OPERAND)
                    .thenComparing(foo -> foo.contains("%"))
                    .thenComparing(foo -> foo.contains("!="))
                    .thenComparing(foo -> foo);

    public static final Splitter AND_OR =
            Splitter.on(Pattern.compile("\\b(or|and)\\b")).trimResults().omitEmptyStrings();

    // Utility methods and backing data

    /** Return mapping from representative locale to plural rules */
    public static String getRepresentativeLocaleForPluralRules(
            PluralType pluralType, String sourceLocale) {
        String result = pluralTypeToData.get(pluralType).localeToRepresentative.get(sourceLocale);
        return result == null ? "und" : result;
    }

    /**
     * Return a map from representative locale to those with the same rules as the sourceLocale. If
     * two locales have the same plural rules, they have the same representative.
     */
    public static ImmutableMap<String, String> getLocaleToRepresentative(PluralType pluralType) {
        return pluralTypeToData.get(pluralType).localeToRepresentative;
    }

    /**
     * Return a map from a set of Counts (plural categories) to the representatives with plural
     * rules with those counts. If two locales have the same plural rules, they have the same
     * representative.
     */
    public static ImmutableMultimap<Set<Count>, String> getCountSetToRepresentative(
            PluralType pluralType) {
        return pluralTypeToData.get(pluralType).countSetToRepresentative;
    }

    /**
     * The inverse of getRepresentativeLocaleForPluralRules; returns a mapping of representitive
     * locales.
     */
    public static ImmutableMultimap<String, String> getRepresentativeToLocales(
            PluralType pluralType) {
        return pluralTypeToData.get(pluralType).representativeToLocales;
    }

    /** Get map from representative To plural rules */
    public static ImmutableMap<String, PluralRules> getRepresentativeToPluralRules(
            PluralType pluralType) {
        return pluralTypeToData.get(pluralType).representativeToPluralRules;
    }

    /** Get map from representative To count set (aka keyword set, category set) */
    public static ImmutableMap<String, Set<Count>> getRepresentativeToCountSet(
            PluralType pluralType) {
        return pluralTypeToData.get(pluralType).representativeToCountSet;
    }

    public static Comparator<String> ORDER_LOCALES_BY_POP =
            new Comparator<>() {
                @Override
                public int compare(String o1, String o2) {
                    return Comparator.comparing(
                                    (String x) -> {
                                        PopulationData popData = supp.getLanguagePopulationData(x);
                                        return popData == null
                                                ? 0
                                                : popData.getLiteratePopulation();
                                    })
                            .reversed()
                            .compare(o1, o2);
                }
            };

    static final Map<PluralType, PluralTypeData> pluralTypeToData;

    static class PluralTypeData {
        private final ImmutableMap<String, String> localeToRepresentative;
        private final ImmutableMultimap<Set<Count>, String> countSetToRepresentative;

        private final ImmutableMultimap<String, String> representativeToLocales;
        private final ImmutableMap<String, Set<Count>> representativeToCountSet;
        private final ImmutableMap<String, PluralRules> representativeToPluralRules;

        private PluralTypeData(
                Map<String, String> _localeToRepresentative,
                Multimap<Set<Count>, String> _countSetToRepresentative,
                Multimap<String, String> _representativeToLocales,
                Map<String, Set<Count>> _representativeToCountSet,
                Map<String, PluralRules> _representativeToPluralRules) {

            localeToRepresentative = ImmutableMap.copyOf(_localeToRepresentative);
            countSetToRepresentative = ImmutableMultimap.copyOf(_countSetToRepresentative);

            representativeToLocales = ImmutableMultimap.copyOf(_representativeToLocales);
            representativeToPluralRules = ImmutableMap.copyOf(_representativeToPluralRules);
            representativeToCountSet = ImmutableMap.copyOf(_representativeToCountSet);
        }
    }

    static {
        Map<PluralType, PluralTypeData> _data = new TreeMap<>();
        for (PluralType pluralType : PluralType.values()) {

            Map<String, String> _localeToRepresentative = new TreeMap<>();
            Map<String, PluralRules> _representativeToPluralRules =
                    new TreeMap<>(ORDER_LOCALES_BY_POP);
            Multimap<String, String> _representativeToLocales =
                    TreeMultimap.create(ORDER_LOCALES_BY_POP, ORDER_LOCALES_BY_POP);
            Comparator<Set<Count>> reversedCountSetComparator =
                    new CldrUtility.CollectionComparator();
            Multimap<Set<Count>, String> _countSetToRepresentative =
                    TreeMultimap.create(
                            reversedCountSetComparator,
                            ORDER_LOCALES_BY_POP); // sort by count set, then locale population

            Multimap<String, String> formattedRulesToLocales = LinkedHashMultimap.create();

            supp.getPluralLocales(pluralType).stream()
                    .forEach(
                            locale -> {
                                PluralInfo pluralInfo = supp.getPlurals(pluralType, locale);
                                PluralRules rules = pluralInfo.getPluralRules();
                                formattedRulesToLocales.put(format(rules), locale);
                            });

            // now that we have the rules mapping to the same set of locales, make the other data

            formattedRulesToLocales.asMap().entrySet().stream()
                    .forEach(
                            x -> {
                                // sort the set to get the first
                                ImmutableSortedSet<String> set =
                                        ImmutableSortedSet.copyOf(x.getValue());
                                TreeSet<String> sortedByPop = new TreeSet<>(ORDER_LOCALES_BY_POP);
                                sortedByPop.addAll(x.getValue());
                                String representative =
                                        sortedByPop.iterator().next(); // pick first in population

                                set.stream()
                                        .forEach(
                                                y ->
                                                        _localeToRepresentative.put(
                                                                y, representative));
                                _representativeToLocales.putAll(representative, set);
                                PluralInfo pluralInfo = supp.getPlurals(pluralType, representative);
                                PluralRules rules = pluralInfo.getPluralRules();
                                Set<Count> categories =
                                        ImmutableSet.copyOf(
                                                rules.getKeywords().stream()
                                                        .map(y -> Count.valueOf(y))
                                                        .collect(Collectors.toList()));
                                _representativeToPluralRules.put(representative, rules);
                                _countSetToRepresentative.put(categories, representative);
                            });

            Map<String, Set<Count>> __representativeToCountSet = new LinkedHashMap<>();
            for (Map.Entry<? extends Set<Count>, ? extends String> entry :
                    _countSetToRepresentative.entries()) {
                if (__representativeToCountSet.put(entry.getValue(), entry.getKey()) != null) {
                    throw new IllegalArgumentException("Should never happen!");
                }
            }
            PluralTypeData pluralTypeData =
                    new PluralTypeData(
                            _localeToRepresentative,
                            _countSetToRepresentative,
                            _representativeToLocales,
                            __representativeToCountSet,
                            _representativeToPluralRules);
            _data.put(pluralType, pluralTypeData);
        }
        pluralTypeToData = ImmutableMap.copyOf(_data);
    }

    /**
     * Formats a long scaled by visibleDigits. Doesn't use standard double etc formatting, because
     * it is easier to control the formatting this way.
     *
     * @param value
     * @return
     */
    //    public static String format(long value, int visibleDigits, int exponent) {
    //        String result = String.valueOf(value);
    //        if (visibleDigits == 0) {
    //            //
    //        } else if (visibleDigits < result.length()) {
    //            result =
    //                    result.substring(0, result.length() - visibleDigits)
    //                            + "."
    //                            + result.substring(result.length() - visibleDigits);
    //        } else {
    //            result = "0." + "0".repeat(visibleDigits - result.length()) + result;
    //        }
    //        return result + (exponent == 0 ? "" : "e" + exponent);
    //    }

    /** A visible format of the key for sorting samples. */
    public static String showKey(Integer key) {
        int intDigitCount = key / 10000;
        int rem = key % 10000;
        int visibleDecimalCount = rem / 100;
        int exponent = rem % 100;
        return "x".repeat(intDigitCount)
                + (visibleDecimalCount == 0 ? "" : "." + "x".repeat(visibleDecimalCount))
                + (exponent == 0 ? "" : "e" + exponent);
    }

    /** Utility for getting 10^exponent, with focus on small powers */
    public static int pow10(int exponent) {
        // TODO see if the switch is necessary
        switch (exponent) {
            case 0:
                return 1;
            case 1:
                return 10;
            case 2:
                return 100;
            case 3:
                return 1000;
            case 4:
                return 10000;
            default:
                return (int) Math.pow(10, exponent);
        }
    }

    /** Get the number of digits in a number: 999 => 3, 1000 => 4, etc. */
    public static int digitCount(long number) {
        return number > 0
                ? (int) (Math.log10(number) + 1)
                : number == 0 ? 1 : (int) (Math.log10(-number) + 1);
    }

    /** A range of VisibleDecimals that have the same factionDigitCount */
    public static class SampleRange implements Comparable<SampleRange>, Iterable<CFixedDecimal> {
        private final CFixedDecimal first;
        private final CFixedDecimal last;

        public enum SameIntegerCount {
            yes,
            no
        }

        public static class Builder {
            CFixedDecimal first;
            CFixedDecimal last;

            /**
             * Add an item. If the visible decimal count is different than the others, fail.
             *
             * @param additional
             * @param sameIntegerCount TODO
             * @return
             */
            Status add(CFixedDecimal additional, SameIntegerCount sameIntegerCount) {
                if (first == null) {
                    first = additional;
                    last = additional;
                    return Status.added;
                } else {
                    if (additional.getVisibleDecimalCount() != first.getVisibleDecimalCount()
                            || sameIntegerCount == sameIntegerCount.yes
                                    && additional.getIntegerDigitCount()
                                            != first.getIntegerDigitCount()) {
                        return Status.makeNewRange;
                    }

                    long current = additional.getDigits();

                    if (current == last.getDigits() + 1) {
                        last = additional;
                        return Status.added;
                    }

                    // otherwise signal that we need a new range

                    return Status.makeNewRange;
                }
            }

            /** the builder is cleared and can be reused after building */
            SampleRange build() {
                SampleRange result = SampleRange.from(first, last);
                first = null;
                last = null;
                return result;
            }

            public boolean isEmpty() {
                return first == null;
            }
        }

        public enum Status {
            added,
            makeNewRange
        }

        public SampleRange(CFixedDecimal first_, CFixedDecimal last_) {
            if (first_.getVisibleDecimalCount() != last_.getVisibleDecimalCount()) {
                throw new IllegalArgumentException(
                        "Incompatible digits in " + first_ + ".." + last_);
            }
            first = first_;
            last = last_;
        }

        public static SampleRange from(CFixedDecimal first_, CFixedDecimal last_) {
            return new SampleRange(first_, last_);
        }

        @Override
        public String toString() {
            if (first.equals(last)) {
                return first.toString();
            } else {
                return first.toString() + "~" + last.toString();
            }
        }

        @Override
        public int compareTo(SampleRange o) {
            return Comparator.comparing(SampleRange::getFirst)
                    .thenComparing(SampleRange::getLast)
                    .compare(this, o);
        }

        public CFixedDecimal getFirst() {
            return first;
        }

        public CFixedDecimal getLast() {
            return last;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Integer getKey() {
            return first.getKey();
        }

        class PluralIterator implements Iterator<CFixedDecimal> {
            private CFixedDecimal current = first;

            @Override
            public boolean hasNext() {
                return current.compareTo(last) <= 0;
            }

            @Override
            public CFixedDecimal next() {
                CFixedDecimal result = current;
                current = result.next();
                return result;
            }
        }

        @Override
        public Iterator<CFixedDecimal> iterator() {
            return new PluralIterator();
        }

        @Override
        public boolean equals(Object obj) {
            SampleRange that = (SampleRange) obj;
            return first.equals(that.first) && last.equals(that.last);
        }

        @Override
        public int hashCode() {
            return first.hashCode() * 37 + last.hashCode();
        }
    }

    /**
     * Immutable class mapping keys to sample ranges. For the structure of a key, see
     * PluralSample::getKey().
     */
    public static class KeySampleRanges implements Iterable<Entry<Integer, SampleRange>> {
        private final Multimap<Integer, SampleRange> ranges;

        public static class Builder {
            private final Multimap<Integer, SampleRange> ranges = TreeMultimap.create();
            private final SampleRange.Builder inProcess = SampleRange.builder();

            public Builder add(CFixedDecimal additional, SameIntegerCount sameIntegerCount) {
                if (inProcess.add(additional, sameIntegerCount)
                        == SampleRange.Status.makeNewRange) {
                    SampleRange range = inProcess.build();
                    ranges.put(range.getKey(), range);
                    inProcess.add(additional, sameIntegerCount);
                }
                return this;
            }

            /** Only use if there can't be any overlap with other ranges */
            public Builder addRange(CFixedDecimal first_, CFixedDecimal last_) {
                SampleRange mr = SampleRange.from(first_, last_);
                ranges.put(mr.getKey(), mr);
                return this;
            }

            /**
             * Only use if there can't be any overlap with other ranges
             *
             * @param exponent TODO
             */
            public Builder addRange(int firstDigits, int secondDigts, int visible, int exponent) {
                return addRange(
                        new CFixedDecimal(firstDigits, visible, exponent),
                        new CFixedDecimal(secondDigts, visible, exponent));
            }

            /** Only use if there can't be any overlap with other ranges */
            public Builder addRange(SampleRange sampleRange) {
                return addRange(sampleRange.getFirst(), sampleRange.getLast());
            }

            public KeySampleRanges build() {
                if (!inProcess.isEmpty()) {
                    SampleRange range = inProcess.build();
                    ranges.put(range.getKey(), range);
                }
                return new KeySampleRanges(ranges);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return ranges.equals(((KeySampleRanges) obj).ranges);
        }

        @Override
        public int hashCode() {
            return ranges.hashCode();
        }

        public KeySampleRanges(Multimap<Integer, SampleRange> ranges) {
            this.ranges = ranges;
        }

        public int size() {
            return ranges.size();
        }

        @Override
        public String toString() {
            return ranges.asMap().entrySet().stream()
                    .map(x -> "\n\t" + showKey(x.getKey()) + " ↠ " + x.getValue())
                    .collect(Collectors.joining(" "));
        }

        //        public KeySampleRanges first(int max) {
        //            KeySampleRanges result = new KeySampleRanges();
        //            ranges.stream().limit(max).forEach(x -> result.ranges.add(x));
        //            return result;
        //        }

        @Override
        public Iterator<Entry<Integer, SampleRange>> iterator() {
            return ranges.entries().iterator();
        }

        public Iterable<Entry<Integer, Collection<SampleRange>>> setIterable() {
            return CldrUtility.getIterableFromIterator(ranges.asMap().entrySet().iterator());
        }

        public static Builder builder() {
            return new Builder();
        }

        public KeySampleRanges filter(BiFunction<Integer, SampleRange, Boolean> func) {
            Builder builder = builder();
            for (Entry<Integer, SampleRange> entry : ranges.entries()) {
                if (func.apply(entry.getKey(), entry.getValue())) {
                    builder.addRange(entry.getValue());
                }
            }
            return builder.build();
        }
    }

    public static boolean keyIsSampleType(int key, SampleType sampleType) {
        return ((key % 100) == 0) == (sampleType == SampleType.INTEGER);
    }

    public static final KeySampleRanges SAMPLE_RANGES_CARDINAL =
            KeySampleRanges.builder() // these use raw values!
                    .addRange(0, 9, 0, 0)
                    .addRange(0, 90, 1, 0)
                    .addRange(0, 900, 2, 0)
                    .addRange(10, 99, 0, 0)
                    .addRange(100, 990, 1, 0)
                    .addRange(100, 999, 0, 0)
                    .addRange(1000, 9999, 0, 0)
                    .addRange(10000, 10099, 0, 0)
                    .addRange(100000, 100099, 0, 0)
                    .addRange(1000000, 1000099, 0, 0)
                    .addRange(1000000, 1000000, 0, 6)
                    .addRange(1100000, 1100000, 0, 6)
                    .addRange(10000000, 10000099, 0, 0)
                    .addRange(11000000, 11000000, 0, 6)
                    .build();

    public static final KeySampleRanges SAMPLE_RANGES_ORDINAL =
            KeySampleRanges.builder() // these use raw values!
                    .addRange(0, 9, 0, 0)
                    .addRange(10, 99, 0, 0)
                    .addRange(100, 999, 0, 0)
                    .build();

    static {
        if (DEBUG) {
            for (Entry<Integer, Collection<SampleRange>> entry :
                    SAMPLE_RANGES_CARDINAL.setIterable()) {
                System.out.println(showKey(entry.getKey()) + "\t" + entry.getValue());
            }
        }
    }

    // TODO convert to nested maps

    private static Map<PluralRules, Map<Count, KeySampleRanges>> RANGE_CACHE_CARDINAL =
            new ConcurrentHashMap<>();

    private static Map<PluralRules, Map<Count, KeySampleRanges>> RANGE_CACHE_ORDINAL =
            new ConcurrentHashMap<>();

    /** Return samples from a plural type and plural rules */
    public static Map<Count, KeySampleRanges> getSamplesFromPluralRules(
            PluralType pluralType, PluralRules pluralRules) {
        Map<PluralRules, Map<Count, KeySampleRanges>> cache =
                pluralType == PluralType.cardinal ? RANGE_CACHE_CARDINAL : RANGE_CACHE_ORDINAL;
        KeySampleRanges sampleRanges0 = getSampleRanges(pluralType);

        return cache.computeIfAbsent(
                pluralRules,
                x -> {
                    return getInternalSamples(pluralRules, sampleRanges0);
                });
    }

    public static KeySampleRanges getSampleRanges(PluralType pluralType) {
        return pluralType == PluralType.cardinal ? SAMPLE_RANGES_CARDINAL : SAMPLE_RANGES_ORDINAL;
    }

    /** Only for tests */
    public static Map<Count, KeySampleRanges> getInternalSamples(
            PluralRules pluralRules, KeySampleRanges sampleRanges0) {
        Map<Count, KeySampleRanges.Builder> countToBuilder = new HashMap<>();

        for (Entry<Integer, Collection<SampleRange>> keySamples : sampleRanges0.setIterable()) {
            // Integer key = keySamples.getKey();
            Collection<SampleRange> ranges = keySamples.getValue();
            for (SampleRange range : ranges) {
                for (CFixedDecimal sample : range) {
                    Count count = sample.getPluralCategory(pluralRules);
                    KeySampleRanges.Builder builder = countToBuilder.get(count);
                    if (builder == null) {
                        countToBuilder.put(count, builder = KeySampleRanges.builder());
                    }
                    builder.add(sample, SameIntegerCount.yes);
                }
            }
        }
        Map<Count, KeySampleRanges> result = new TreeMap<>();
        countToBuilder.entrySet().stream()
                .forEach(y -> result.put(y.getKey(), y.getValue().build()));

        return CldrUtility.protectCollection(result);
    }

    /** Given a locale, eturn a map from count to samples */
    public static Map<Count, KeySampleRanges> getSamplesForLocale(
            PluralType pluralType, String locale) {
        String rep = getRepresentativeLocaleForPluralRules(pluralType, locale);
        PluralRules pluralRules1 = getRepresentativeToPluralRules(pluralType).get(rep);
        Map<Count, KeySampleRanges> pluralSamples1 =
                getSamplesFromPluralRules(pluralType, pluralRules1);
        return pluralSamples1;
    }

    /** Test utility */
    public static String format(
            PluralType pluralType, Map<Count, KeySampleRanges> countAndSamples) {
        return Joiners.N.join(PluralUtilities.getSamplesForLocale(pluralType, "en").entrySet());
    }

    /** Produce a normalized format of plural rules */
    public static String format(PluralRules rules) {
        return rules.getKeywords().stream()
                .filter(x -> !x.equals("other"))
                .map(x -> x + "\t" + normalizeRule(rules.getRules(x)))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Make sure spacing, etc is clean for a rule like e = 0 and i != 0 and i % 1000000 = 0 and v =
     * 0 or e != 0..5. Currently, don't normalize other features
     */
    public static String normalizeRule(String rule) {
        if (OBSOLETE_SYNTAX.matcher(rule).find()) {
            throw new IllegalArgumentException("Deprecated format: (mod|in|is|within) in " + rule);
        }
        StringBuilder result = new StringBuilder();
        boolean firstAnd = true;
        for (String andClause : AND_SPLITTER.split(rule)) {
            if (firstAnd) {
                firstAnd = false;
            } else {
                result.append(" and ");
            }
            boolean firstOr = true;
            for (String orClause : OR_SPLITTER.splitToList(andClause)) {
                if (firstOr) {
                    firstOr = false;
                } else {
                    result.append(" or ");
                }
                result.append(RELATION.matcher(orClause.replace(" ", "")).replaceAll(" $1 "));
            }
        }
        return result.toString();
    }

    public static Set<String> getRelations(String rule) {
        Set<String> result = new LinkedHashSet<>();
        for (String andClause : AND_SPLITTER.split(rule)) {
            for (String orClause : OR_SPLITTER.splitToList(andClause)) {
                result.add(orClause);
            }
        }
        return result;
    }

    static Map<String, String> OP_NAME =
            ImmutableMap.of(
                    "n",
                    "N",
                    "i",
                    "integer(N)",
                    "v",
                    "fraction digit count(N)",
                    "w",
                    "fraction digit count(N) [skipping trailing zeros]",
                    "f",
                    "fraction(N)",
                    "t",
                    "fraction(N) [skipping trailing zeros]",
                    "c",
                    "compact exponent(N)",
                    "e",
                    "compact exponent(N)");

    public static String englishVersion(String rule, boolean singleLine) {
        if (OBSOLETE_SYNTAX.matcher(rule).find()) {
            throw new IllegalArgumentException("Deprecated format: (mod|in|is|within) in " + rule);
        }
        StringBuilder result = new StringBuilder();
        boolean firstOr = true;
        for (String orClause : OR_SPLITTER.splitToList(rule)) {
            if (firstOr) {
                firstOr = false;
            } else if (singleLine) {
                result.append(" OR");
            } else {
                result.append("\n\tOR\n");
            }
            boolean firstAnd = true;
            for (String andClause : AND_SPLITTER.split(orClause)) {
                if (firstAnd) {
                    firstAnd = false;
                } else if (singleLine) {
                    result.append(" and");
                } else {
                    result.append(" and\n");
                }
                Matcher relationParts = RELATION_PARTS.matcher(andClause);
                if (!relationParts.matches()) {
                    return ("«"
                            + rule
                            + "» doesn't match "
                            + RELATION_PARTS
                            + " \t"
                            + RegexUtilities.showMismatch(relationParts, andClause));
                }
                if (singleLine) {
                    result.append(" ");
                } else {
                    result.append("\t");
                }
                String operand = relationParts.group(1);
                String modulus = relationParts.group(2);
                String relation = relationParts.group(3);
                String leftSide = relationParts.group(4);
                String operandName = OP_NAME.get(operand);
                boolean isEqual = relation.equals("=");
                String relName =
                        leftSide.contains(".") || leftSide.contains(",")
                                ? isEqual ? "∈" : "∉"
                                : isEqual ? "=" : "≠";
                if (modulus != null) {
                    int count = modulus.length() - 1;
                    if (count == 1) {
                        result.append(
                                Joiners.SP.join(
                                        "the last digit of", operandName, relName, leftSide));
                    } else {
                        result.append(
                                Joiners.SP.join(
                                        "the last",
                                        count,
                                        "digits of",
                                        operandName,
                                        relName,
                                        leftSide));
                    }
                } else {
                    result.append(Joiners.SP.join(operandName, relName, leftSide));
                }
            }
        }
        return result.toString();
    }

    /** Test utility */
    public static Pair<String, String> findDifference(
            PluralType pluralType, String repLocale1, String repLocale2) {
        Map<Count, KeySampleRanges> pluralSamples1 = getSamplesForLocale(pluralType, repLocale1);
        Map<Count, KeySampleRanges> pluralSamples2 = getSamplesForLocale(pluralType, repLocale2);
        if (!pluralSamples1.keySet().equals(pluralSamples2.keySet())) {
            return Pair.of(pluralSamples1.keySet().toString(), pluralSamples2.keySet().toString());
        }
        for (Count count : pluralSamples1.keySet()) {
            KeySampleRanges keySampleRanges1 = pluralSamples1.get(count);
            KeySampleRanges keySampleRanges2 = pluralSamples2.get(count);
            if (!keySampleRanges1.equals(keySampleRanges2)) {
                // find first difference
                Iterator<Entry<Integer, SampleRange>> it1 = keySampleRanges1.iterator();
                Iterator<Entry<Integer, SampleRange>> it2 = keySampleRanges2.iterator();
                SampleRange range1 = null;
                SampleRange range2 = null;
                // since we know there is a difference, we don't have to check as much
                while (true) {
                    range1 = it1.hasNext() ? it1.next().getValue() : null;
                    range2 = it2.hasNext() ? it2.next().getValue() : null;
                    if (!Objects.equals(range1, range2)) {
                        return Pair.of(
                                range1 == null ? "missing" : count + " " + range1,
                                range2 == null ? "missing" : count + " " + range2);
                    }
                }
            }
        }
        return null;
    }

    /** Immutable data for single rules */
    public static class LocalesAndSamples {
        public KeySampleRanges getKeySampleRanges() {
            return keySampleRanges;
        }

        public Set<String> getLocales() {
            return locales;
        }

        private LocalesAndSamples(KeySampleRanges value, Collection<String> collection) {
            keySampleRanges = value;
            locales = ImmutableSet.copyOf(collection);
        }

        final KeySampleRanges keySampleRanges;
        final Set<String> locales;
    }

    public static Map<String, LocalesAndSamples> getSingleRuleToLocalesAndSamples(PluralType type) {
        return ruleDataCache.computeIfAbsent(
                type, x -> getSingleRuleToLocalesAndSamplesInternal(type));
    }

    private static Map<PluralType, Map<String, LocalesAndSamples>> ruleDataCache =
            new ConcurrentHashMap<>();

    private static Map<String, LocalesAndSamples> getSingleRuleToLocalesAndSamplesInternal(
            PluralType type) {
        Map<String, KeySampleRanges> ruleToKeySampleRanges = new TreeMap<>();
        Multimap<String, String> ruleToLocales = TreeMultimap.create();

        for (String representative :
                PluralUtilities.getRepresentativeToPluralRules(type).keySet()) {
            PluralRules pluralRules =
                    PluralUtilities.getRepresentativeToPluralRules(type).get(representative);
            for (String keyword : pluralRules.getKeywords()) {
                if (keyword.equals("other")) {
                    continue;
                }
                String rule = pluralRules.getRules(keyword);
                boolean haveSamples = ruleToLocales.containsKey(rule);
                ruleToLocales.put(rule, representative);
                if (haveSamples) {
                    continue;
                }
                PluralRules singleton = PluralRules.createRules("many: " + rule);
                Map<Count, KeySampleRanges> samples =
                        PluralUtilities.getInternalSamples(
                                singleton, PluralUtilities.getSampleRanges(type));
                KeySampleRanges manySamples = samples.get(Count.many);
                ruleToKeySampleRanges.put(rule, manySamples);
            }
        }
        Map<String, LocalesAndSamples> singleRuleToLocalesAndSamples = new TreeMap();
        for (Entry<String, KeySampleRanges> entry : ruleToKeySampleRanges.entrySet()) {
            singleRuleToLocalesAndSamples.put(
                    entry.getKey(),
                    new LocalesAndSamples(entry.getValue(), ruleToLocales.get(entry.getKey())));
        }
        return ImmutableMap.copyOf(singleRuleToLocalesAndSamples);
    }

    /** Get old samples */
    public static String getOldSamples(
            String keyword, PluralRules pluralRules, SampleType sampleType) {
        DecimalQuantitySamples exampleList =
                pluralRules.getDecimalSamples(
                        keyword, sampleType); // plurals.getSamples9999(count);
        return exampleList == null ? "" : getOldExamples(exampleList);
    }

    private static String getOldExamples(DecimalQuantitySamples exampleList) {
        return Joiner.on(", ").join(exampleList.getSamples()) + (exampleList.bounded ? "" : ", …");
    }

    static final Pattern FIXED_DECIMAL_STRING =
            Pattern.compile(
                    "([0-9]+)" // integer part
                            + "(?:\\.([0-9]+))?" // fractional part
                            + "(?:[eEcC]([0-9]+))?"); // exponent part

    //    public static FixedDecimal parseToFixedDecimal(String num) {
    //        Matcher matcher = FIXED_DECIMAL_STRING.matcher(num);
    //        if (!matcher.matches()) {
    //            throw new IllegalArgumentException(
    //                    "Bad format: " + RegexUtilities.showMismatch(matcher, num));
    //        }
    //        String doubleStr = matcher.group(1);
    //        String fractionDigitStr = matcher.group(2);
    //        String exponentStr0 = matcher.group(3);
    //
    //        double n = Double.parseDouble(doubleStr);
    //        int exponent = exponentStr0 == null ? 0 : Integer.parseInt(exponentStr0);
    //        int v = fractionDigitStr == null ? 0 : fractionDigitStr.length();
    //        long f = fractionDigitStr == null ? 0L : Long.parseLong(fractionDigitStr);
    //        if (exponent != 0) {
    //            int pow10e = pow10(exponent);
    //            n *= pow10e;
    //            f /= pow10e;
    //            v = v > exponent ? v - exponent : 0;
    //        }
    //        return new FixedDecimal(n, v, f, exponent);
    //    }

    private static String padEnd(String string, int minLength, char c) {
        StringBuilder sb = new StringBuilder(minLength);
        sb.append(string);
        for (int i = string.length(); i < minLength; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    //    public static int getVisibleFractionCount(String value) {
    //        value = value.trim();
    //        int decimalPos = value.indexOf('.') + 1;
    //        if (decimalPos == 0) {
    //            return 0;
    //        } else {
    //            return value.length() - decimalPos;
    //        }
    //    }

    /** This is a copy of FixedDecimal.getFractionDigits, because the original is not public. */
    public static int getFractionalDigits(double n, int v) {
        if (v == 0) {
            return 0;
        } else {
            if (n < 0) {
                n = -n;
            }
            int baseFactor = (int) Math.pow(10, v);
            long scaled = Math.round(n * baseFactor);
            return (int) (scaled % baseFactor);
        }
    }

    public static String toSampleString(IFixedDecimal source) {
        final double n = source.getPluralOperand(Operand.n);
        final int exponent = (int) source.getPluralOperand(Operand.e);
        final int visibleDecimalDigitCount = (int) source.getPluralOperand(Operand.v);
        if (exponent == 0) {
            return String.format(Locale.ROOT, "%." + visibleDecimalDigitCount + "f", n);
        } else {
            // we need to slide the exponent back

            int fixedV = visibleDecimalDigitCount + exponent;
            String baseString =
                    String.format(Locale.ROOT, "%." + fixedV + "f", n / Math.pow(10, exponent));

            // HACK
            // However, we don't have enough information to round-trip if v == 0
            // So in that case we choose the shortest form,
            // so we have to have a hack to strip trailing fraction spaces.
            if (visibleDecimalDigitCount == 0) {
                for (int i = visibleDecimalDigitCount; i < fixedV; ++i) {
                    // TODO this code could and should be optimized, but for now...
                    if (baseString.endsWith("0")) {
                        baseString = baseString.substring(0, baseString.length() - 1);
                        continue;
                    }
                    break;
                }
                if (baseString.endsWith(".")) {
                    baseString = baseString.substring(0, baseString.length() - 1);
                }
            }
            return baseString + "c" + exponent;
        }
    }
}
