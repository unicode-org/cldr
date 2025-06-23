package org.unicode.cldr.util;

import static java.util.Comparator.comparing;

import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.PluralRules;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.text.FixedDecimal;
import org.unicode.cldr.util.PluralUtilities.KeySampleRanges;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class PluralUtilities {
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

    /**
     * Return a representative locale that has the same rules as the sourceLocale. If two locales
     * have the same plural rules, they have the same representative. All locales without plural
     * rules OR that inherit, return "und"
     *
     * @param sourceLocale
     * @return
     */
    public static String getRepresentativeLocaleForPluralRules(String sourceLocale) {
        String result = localeToRepresentative.get(sourceLocale);
        return result == null ? "und" : result;
    }

    /**
     * Return a map from representative locale to those with the same rules as the sourceLocale. If
     * two locales have the same plural rules, they have the same representative.
     *
     * @param sourceLocale
     * @return
     */
    public static ImmutableMap<String, String> getLocaleToRepresentative() {
        return localeToRepresentative;
    }

    /**
     * Return a map from a set of Counts (plural categories) to the representatives with plural
     * rules with those counts. If two locales have the same plural rules, they have the same
     * representative.
     *
     * @param sourceLocale
     * @return
     */
    public static ImmutableMultimap<Set<Count>, String> getCountSetToRepresentative() {
        return countSetToRepresentative;
    }

    /**
     * The inverse of getRepresentativeLocaleForPluralRules; returns a mapping of representitive
     * locales.
     */
    public static ImmutableMultimap<String, String> getRepresentativeToLocales() {
        return representativeToLocales;
    }

    /**
     * Get map from representative To
     *
     * @return
     */
    public static ImmutableMap<String, PluralRules> getRepresentativeToPluralRules() {
        return representativeToPluralRules;
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

    public static ImmutableMap<String, Set<Count>> getRepresentativeToCountSet() {
        return representativeToCountSet;
    }

    public static <T, S extends Comparable<T>> Comparator<Iterable<S>> iterableComparator() {
        return Comparators.lexicographical(Comparator.<Comparable>naturalOrder().reversed());
    }

    private static final ImmutableMap<String, String> localeToRepresentative;
    private static final ImmutableMultimap<Set<Count>, String> countSetToRepresentative;

    private static final ImmutableMap<String, PluralRules> representativeToPluralRules;
    private static final ImmutableMultimap<String, String> representativeToLocales;
    private static final ImmutableMap<String, Set<Count>> representativeToCountSet;

    static {
        Map<String, String> _localeToRepresentative = new TreeMap<>();
        Map<String, PluralRules> _representativeToPluralRules = new TreeMap<>(ORDER_LOCALES_BY_POP);
        Multimap<String, String> _representativeToLocales =
                TreeMultimap.create(ORDER_LOCALES_BY_POP, ORDER_LOCALES_BY_POP);
        Comparator<Iterable<Count>> reversedCountSetComparator =
                Comparators.lexicographical(Comparator.<Count>naturalOrder().reversed());
        Multimap<Set<Count>, String> _countSetToRepresentative =
                TreeMultimap.create(
                        reversedCountSetComparator,
                        ORDER_LOCALES_BY_POP); // sort by count set, then locale population

        Multimap<PluralRules, String> rulesToLocales = LinkedHashMultimap.create();

        supp.getPluralLocales().stream()
                .forEach(
                        locale -> {
                            PluralInfo pluralInfo = supp.getPlurals(locale);
                            PluralRules rules = pluralInfo.getPluralRules();
                            rulesToLocales.put(rules, locale);
                        });

        // now that we have the rules mapping to the same set of locales, make the other data

        rulesToLocales.asMap().entrySet().stream()
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
                                    .forEach(y -> _localeToRepresentative.put(y, representative));
                            _representativeToLocales.putAll(representative, set);
                            PluralInfo pluralInfo = supp.getPlurals(representative);
                            PluralRules rules = pluralInfo.getPluralRules();
                            Set<Count> categories =
                                    ImmutableSet.copyOf(
                                            rules.getKeywords().stream()
                                                    .map(y -> Count.valueOf(y))
                                                    .collect(Collectors.toList()));
                            _representativeToPluralRules.put(representative, rules);
                            _countSetToRepresentative.put(categories, representative);
                        });

        Map<String, Set<Count>> temp = new LinkedHashMap<>();
        for (Map.Entry<? extends Set<Count>, ? extends String> entry :
                _countSetToRepresentative.entries()) {
            if (temp.put(entry.getValue(), entry.getKey()) != null) {
                throw new IllegalArgumentException("Should never happen!");
            }
        }

        localeToRepresentative = ImmutableMap.copyOf(_localeToRepresentative);
        representativeToCountSet = ImmutableMap.copyOf(temp);
        representativeToPluralRules = ImmutableMap.copyOf(_representativeToPluralRules);
        representativeToLocales = ImmutableMultimap.copyOf(_representativeToLocales);
        countSetToRepresentative = ImmutableMultimap.copyOf(_countSetToRepresentative);
    }

    /**
     * An immutable value used for decimal numbers, targeted at use with plurals. These are similar
     * to FixedDecimals, but have additional capabilities, useful for CLDR. The number of digits is
     * limited to 19, whereby some of those can be fractional digits (including trailing zeros). Eg,
     * 1.0 ≠ 1
     */
    public static class PluralSample implements Comparable<PluralSample> {
        private final long digits;
        private final int visibleDecimalCount;
        private final int factor;

        public long getDigits() {
            return digits;
        }

        public int getIntegerDigitCount() {
            return digitCount(digits / factor);
        }

        public int getVisibleDecimalCount() {
            return visibleDecimalCount;
        }

        public PluralSample(long digits, int fractionCount) {
            this.digits = digits;
            this.factor = pow10(fractionCount);
            this.visibleDecimalCount = fractionCount;
        }

        public PluralSample(FixedDecimal fixedDecimal) {
            visibleDecimalCount = fixedDecimal.getVisibleDecimalDigitCount();
            factor = pow10(visibleDecimalCount);
            digits = fixedDecimal.getIntegerValue() * factor + fixedDecimal.getDecimalDigits();
        }

        public FixedDecimal toFixedDecimal() {
            return new FixedDecimal(digits, visibleDecimalCount);
        }

        @Override
        public boolean equals(Object obj) {
            PluralSample that = (PluralSample) obj;
            return digits == that.digits && visibleDecimalCount == that.visibleDecimalCount;
        }

        @Override
        public int hashCode() {
            return (int) ((digits << 16) ^ visibleDecimalCount);
        }

        @Override
        public int compareTo(PluralSample other) {
            return Comparator.comparingInt(PluralSample::getVisibleDecimalCount)
                    .thenComparing(PluralSample::getDigits)
                    .compare(this, other);
        }

        public int compareTo(long digits, int fractionCount) {
            return Comparator.comparingInt(PluralSample::getVisibleDecimalCount)
                    .thenComparing(PluralSample::getDigits)
                    .compare(this, new PluralSample(digits, fractionCount));
        }

        /**
         * Get the value immediately after `this`, according to the visible decimal count: vdc=0 =>
         * 1, vdc=1 => 0.1, ...
         */
        public PluralSample next() {
            return new PluralSample(digits + 1, visibleDecimalCount);
        }

        /**
         * Return a key composed of the integer digit count and visible decimal count. Used for
         * buckets with compact decimals
         */
        public int compactKey() {
            return 100 * getIntegerDigitCount() + visibleDecimalCount;
        }

        @Override
        public String toString() {
            return format(digits, visibleDecimalCount);
        }

        public Integer getKey() {
            return compactKey();
        }

        public Count getPluralCategory(PluralRules pluralRules) {
            FixedDecimal fd0 = new FixedDecimal(digits / factor, visibleDecimalCount);
            String keyword = pluralRules.select(fd0);
            return Count.valueOf(keyword);
        }
    }

    public static int pow10(int exponent) {
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

    public static int digitCount(long number) {
        return number > 0
                ? (int) (Math.log10(number) + 1)
                : number == 0 ? 1 : (int) (Math.log10(-number) + 1);
    }

    /** A range of VisibleDecimals that have the same factionDigitCount */
    public static class SampleRange implements Comparable<SampleRange>, Iterable<PluralSample> {
        private final PluralSample first;
        private final PluralSample last;

        public static class Builder {
            PluralSample first;
            PluralSample last;

            /**
             * Add an item. If the visible decimal count is different than the others, fail.
             *
             * @param additional
             * @return
             */
            Status add(PluralSample additional) {
                if (first == null) {
                    first = additional;
                    last = additional;
                    return Status.added;
                } else {
                    if (additional.visibleDecimalCount != first.visibleDecimalCount) {
                        return Status.makeNewRange;
                    }

                    long current = additional.digits;

                    if (current == last.digits + 1) {
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

        public SampleRange(PluralSample first_, PluralSample last_) {
            if (first_.visibleDecimalCount != last_.visibleDecimalCount) {
                throw new IllegalArgumentException(
                        "Incompatible digits in " + first_ + ".." + last_);
            }
            first = first_;
            last = last_;
        }

        public static SampleRange from(PluralSample first_, PluralSample last_) {
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

        public PluralSample getFirst() {
            return first;
        }

        public PluralSample getLast() {
            return last;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Integer getKey() {
            return first.getKey();
        }

        class PluralIterator implements Iterator<PluralSample> {
            private PluralSample current = first;

            @Override
            public boolean hasNext() {
                return current.compareTo(last) <= 0;
            }

            @Override
            public PluralSample next() {
                PluralSample result = current;
                current = result.next();
                return result;
            }
        }

        @Override
        public Iterator<PluralSample> iterator() {
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
     * Formats a long scaled by visibleDigits. Doesn't use standard double etc formatting, because
     * it is easier to control the formatting this way.
     *
     * @param value
     * @return
     */
    public static String format(long value, int visibleDigits) {
        String result = String.valueOf(value);
        if (visibleDigits == 0) {
            return result;
        } else if (visibleDigits < result.length()) {
            return result.substring(0, result.length() - visibleDigits)
                    + "."
                    + result.substring(result.length() - visibleDigits);
        } else {
            return "0." + "0".repeat(visibleDigits - result.length()) + result;
        }
    }

    /**
     * Immutable class mapping keys to sample ranges. A key contains the number of integer digits
     * and the number of decimal digits.
     */
    public static class KeySampleRanges implements Iterable<Entry<Integer, SampleRange>> {
        private final Multimap<Integer, SampleRange> ranges;

        public static class Builder {
            private final Multimap<Integer, SampleRange> ranges = TreeMultimap.create();
            private final SampleRange.Builder inProcess = SampleRange.builder();

            public Builder add(PluralSample additional) {
                if (inProcess.add(additional) == SampleRange.Status.makeNewRange) {
                    SampleRange range = inProcess.build();
                    ranges.put(range.getKey(), range);
                    inProcess.add(additional);
                }
                return this;
            }

            /** Only use if there can't be any overlap with other ranges */
            public Builder addRange(PluralSample first_, PluralSample last_) {
                SampleRange mr = SampleRange.from(first_, last_);
                ranges.put(mr.getKey(), mr);
                return this;
            }

            /** Only use if there can't be any overlap with other ranges */
            public Builder addRange(int firstDigits, int secondDigts, int visible) {
                return addRange(
                        new PluralSample(firstDigits, visible),
                        new PluralSample(secondDigts, visible));
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
            return getIterableFromIterator(ranges.asMap().entrySet().iterator());
        }

        public static Builder builder() {
            return new Builder();
        }

        KeySampleRanges filter(BiFunction<Integer, SampleRange, Boolean> func) {
            Builder builder = builder();
            for (Entry<Integer, SampleRange> entry : ranges.entries()) {
                if (func.apply(entry.getKey(), entry.getValue())) {
                    builder.addRange(entry.getValue());
                }
            }
            return builder.build();
        }
    }

    public static <T> Iterable<T> getIterableFromIterator(Iterator<T> iterator) {
        return () -> iterator;
    }

    public static final KeySampleRanges SAMPLE_RANGES =
            KeySampleRanges.builder() // these use raw values!
                    .addRange(0, 9, 0)
                    .addRange(0, 90, 1)
                    .addRange(0, 900, 2)
                    .addRange(10, 99, 0)
                    .addRange(100, 990, 1)
                    .addRange(100, 999, 0)
                    .addRange(1000, 2099, 0)
                    .addRange(10000, 10099, 0)
                    .addRange(100000, 100099, 0)
                    .addRange(1000000, 1000099, 0)
                    .addRange(10000000, 10000099, 0)
                    .build();

    public static String showKey(Integer key) {
        int intDigitCount = key / 100;
        int visibleDecimalCount = key % 100;
        return "x".repeat(intDigitCount)
                + (visibleDecimalCount == 0 ? "" : "." + "x".repeat(visibleDecimalCount));
    }

    static {
        if (DEBUG) {
            for (Entry<Integer, Collection<SampleRange>> entry : SAMPLE_RANGES.setIterable()) {
                System.out.println(showKey(entry.getKey()) + "\t" + entry.getValue());
            }
        }
    }

    private static Map<PluralRules, Map<Count, KeySampleRanges>> RANGE_CACHE =
            new ConcurrentHashMap<>();

    public static Map<Count, KeySampleRanges> getSamplesFromPluralRules(PluralRules pluralRules) {
        return RANGE_CACHE.computeIfAbsent(
                pluralRules,
                x -> {
                    Map<Count, KeySampleRanges.Builder> countToBuilder = new HashMap<>();

                    for (Entry<Integer, Collection<SampleRange>> keySamples :
                            SAMPLE_RANGES.setIterable()) {
                        Integer key = keySamples.getKey();
                        Collection<SampleRange> ranges = keySamples.getValue();
                        for (SampleRange range : ranges) {
                            for (PluralSample sample : range) {
                                Count count = sample.getPluralCategory(pluralRules);
                                KeySampleRanges.Builder builder = countToBuilder.get(count);
                                if (builder == null) {
                                    countToBuilder.put(count, builder = KeySampleRanges.builder());
                                }
                                builder.add(sample);
                            }
                        }
                    }
                    Map<Count, KeySampleRanges> result = new TreeMap<>();
                    countToBuilder.entrySet().stream()
                            .forEach(y -> result.put(y.getKey(), y.getValue().build()));

                    return CldrUtility.protectCollection(result);
                });
    }

    public static Map<Count, KeySampleRanges> getSamplesForLocale(String locale) {
        String rep = getRepresentativeLocaleForPluralRules(locale);
        PluralRules pluralRules1 = PluralUtilities.getRepresentativeToPluralRules().get(rep);
        Map<Count, KeySampleRanges> pluralSamples1 =
                PluralUtilities.getSamplesFromPluralRules(pluralRules1);
        return pluralSamples1;
    }

    public static String format(Map<Count, KeySampleRanges> countAndSamples) {
        return Joiners.N.join(PluralUtilities.getSamplesForLocale("en").entrySet());
    }

    public static String format(PluralRules rules) {
        return rules.getKeywords().stream()
                .filter(x -> !x.equals("other"))
                .map(x -> x + "\t" + rules.getRules(x))
                .collect(Collectors.joining("\n"));
    }

    public static Pair<String, String> findDifference(String repLocale1, String repLocale2) {
        Map<Count, KeySampleRanges> pluralSamples1 = getSamplesForLocale(repLocale1);
        Map<Count, KeySampleRanges> pluralSamples2 = getSamplesForLocale(repLocale2);
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
}
