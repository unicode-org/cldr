package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.GeneratedPluralSamples.Range.Status;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.NumberInfo;

public class GeneratedPluralSamples {
    static TestInfo testInfo = TestInfo.getInstance();
    private static final int SAMPLE_LIMIT = 16;
    private static final int UNBOUNDED_LIMIT = 20;
    private static final String RANGE_SEPARATOR = "~";

    static class Range implements Comparable<Range>{
        // invariant: visibleFractionDigitCount are the same
        private NumberInfo start;
        private NumberInfo end;
        final long offset;

        /**
         * Must only be called if visibleFractionDigitCount are the same.
         */
        public Range(NumberInfo start, NumberInfo end) {
            this.start = start;
            this.end = end;
            int temp = 1;
            for (long i = start.visibleFractionDigitCount; i != 0; i /= 10) {
                temp *= 10;
            }
            offset = temp;
        }
        public Range(Range other) {
            this.start = other.start;
            this.end = other.end;
            offset = other.offset;
        }
        @Override
        public int compareTo(Range o) {
            // TODO Auto-generated method stub
            int diff = start.compareTo(o.start);
            if (diff != 0) {
                return diff;
            }
            return end.compareTo(o.end);
        }
        enum Status {inside, rightBefore, other}
        /**
         * Must only be called if visibleFractionDigitCount are the same.
         */
        Status getStatus(NumberInfo ni) {
            long startValue = start.intValue * offset + start.fractionalDigits;
            long endValue = end.intValue * offset + end.fractionalDigits;
            long newValue = ni.intValue * offset + ni.fractionalDigits;
            return startValue <= newValue && newValue <= endValue ? Status.inside
                    : endValue + 1 == newValue ? Status.rightBefore 
                            : Status.other;
        }
        @Override
        public String toString() {
            return start + (start.equals(end) ? "" : RANGE_SEPARATOR + end);
        }
    }

    /**
     * Add-only set of ranges.
     */
    static class Ranges {
        Set<Range>[] data = new Set[4];
        int size = 0;
        {
            for (int i = 0; i < data.length; ++i) {
                data[i] = new TreeSet<Range>();
            }
        }
        public Ranges(Ranges other) {
            for (int i = 0; i < data.length; ++i) {
                for (Range range : other.data[i]) {
                    data[i].add(new Range(range));
                }
            }
        }
        public Ranges() {
            // TODO Auto-generated constructor stub
        }
        void add(NumberInfo ni) {
            Set<Range> set = data[ni.visibleFractionDigitCount];
            for (Range item : set) {
                switch (item.getStatus(ni)) {
                case inside: 
                    return;
                case rightBefore: 
                    item.end = ni; // just extend it
                    ++size;
                    return;
                }
            }
            set.add(new Range(ni,ni));
            ++size;
        }
        public int size() {
            return size;
        }
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (Set<Range> datum : data) {
                for (Range range : datum) {
                    if (b.length() != 0) {
                        b.append(", ");
                    }
                    b.append(range.start);
                    if (!range.start.equals(range.end)){
                        b.append(RANGE_SEPARATOR).append(range.end);
                    }
                }
            }
            return b.toString();
        }
        public void trim(int sampleLimit) {
            // limit to a total of sampleLimit ranges, *but* also include have at least one of each fraction length
            for (int i = 0; i < data.length; ++i) {
                if (sampleLimit <= 0) {
                    sampleLimit = 1;
                }
                for (Iterator it = data[i].iterator(); it.hasNext();) {
                    it.next();
                    --sampleLimit;
                    if (sampleLimit < 0) {
                        it.remove();
                    }
                }
            }

        }
    }

    static class DataSample {
        int count;
        int countNoTrailing = -1;
        final Set<Double> noTrailing = new HashSet();
        final Ranges samples = new Ranges();
        final NumberInfo[] digitToSample = new NumberInfo[10];

        public String toString(boolean isKnownBounded) {
            if (countNoTrailing < 0) {
                countNoTrailing = noTrailing.size();
                noTrailing.clear(); // to avoid running out of memory.
            }
            boolean isBounded = isKnownBounded || countNoTrailing < UNBOUNDED_LIMIT;
            samples.trim(SAMPLE_LIMIT);  // to avoid running out of memory.
            Ranges samples2 = new Ranges(samples);
            for (NumberInfo ni : digitToSample) {
                if (ni != null) {
                    samples2.add(ni);
                }
            }
            return samples2 + (isBounded ? "" : ", …");
        }
        private void add(NumberInfo ni) {
            ++count;
            samples.add(ni);
            noTrailing.add(ni.source);
            int digit = getDigit(ni);
            if (digitToSample[digit] == null) {
                digitToSample[digit] = ni;
            }
        }
        @Override
        public boolean equals(Object obj) {
            DataSample other = (DataSample)obj;
            return count == other.count
                    && samples.equals(other.samples)
                    && digitToSample.equals(other.digitToSample);
        }
        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return count ^ samples.hashCode() ^ Arrays.asList(digitToSample).hashCode();
        }
    }

    static class Data {
        private final String keyword; // for debugging
        private final DataSample integers = new DataSample();
        private final DataSample decimals = new DataSample();
        private final boolean isKnownIntegerBounded;
        private final boolean isKnownDecimalBounded;

        Data(String keyword, String rule) {
            this.keyword = keyword;
            isKnownIntegerBounded = rule != null 
                    && rule.startsWith("n")
                    && !rule.contains("or") 
                    && !rule.contains("and") 
                    && !rule.contains("mod") 
                    && !rule.contains("not") 
                    && !rule.contains("!");
            isKnownDecimalBounded = isKnownIntegerBounded
                    && !rule.contains("within");
            if (isKnownIntegerBounded) {
                System.out.println("# known integer bounded: " + rule);
            }
            if (isKnownDecimalBounded) {
                System.out.println("# known decimal bounded: " + rule);
            }
        }
        
        private void add(NumberInfo ni) {
            if (ni.visibleFractionDigitCount == 0) {
                integers.add(ni);
            } else {
                decimals.add(ni);
            }
        }
        @Override
        public String toString() {
            String integersString = integers.toString(isKnownIntegerBounded);
            String decimalsString = decimals.toString(isKnownDecimalBounded);
            return (integersString.isEmpty() ? "\t\t" : "\t@integers\t" + integersString)
                    + (decimalsString.isEmpty() ? "" : "\t@decimals\t" + decimalsString);
        }
        @Override
        public boolean equals(Object obj) {
            Data other = (Data)obj;
            return integers.equals(other.integers) && decimals.equals(other.decimals);
        }
    }

    static private int getDigit(NumberInfo ni) {
        int result = 0;
        long value = ni.intValue;
        do {
            ++result;
            value /= 10;
        } while (value != 0);
        return result;
    }

    TreeMap<String,Data> keywordToData = new TreeMap();

    GeneratedPluralSamples(PluralInfo pluralInfo, PluralType type) {
        PluralInfo pluralRule = pluralInfo;
        // 9999, powers; no decimals
        collect(pluralRule, 10000, 0);
        collect10s(pluralRule, 100000, 0);

        if (type != PluralType.cardinal) {
            return;
        }
        
        // 9999.9, powers .0
        collect(pluralRule, 10000, 1);
        collect10s(pluralRule, 1000000, 1);
        
        // 999.99, powers .00
        collect(pluralRule, 1000, 2);
        collect10s(pluralRule, 1000000, 2);

        // 99.999, powers .000
        collect(pluralRule, 100, 3);
        collect10s(pluralRule, 1000000, 3);
    }

    private void collect10s(PluralInfo pluralInfo, int limit, int decimals) {
        double power = Math.pow(10, decimals);
        for (int i = 1; i <= limit*(int)power; i *= 10) {
            add(pluralInfo, i/power, decimals);
        }
    }

    private void collect(PluralInfo pluralInfo, int limit, int decimals) {
        double power = Math.pow(10, decimals);
        for (int i = 0; i <= limit*(int)power; ++i) {
            add(pluralInfo, i/power, decimals);
        }
    }
    
    private void add(PluralInfo pluralInfo, double d, int visibleDecimals) {
        NumberInfo ni = new NumberInfo(d, visibleDecimals);
        String keyword = pluralInfo.getPluralRules().select(ni);
        Data data = keywordToData.get(keyword);
        if (data == null) {
            keywordToData.put(keyword, data = new Data(keyword, pluralInfo.getRule(Count.valueOf(keyword))));
        }
        data.add(ni);
    }

    private Data getData(String keyword) {
        return keywordToData.get(keyword);
    }

    @Override
    public boolean equals(Object obj) {
        return keywordToData.equals(((GeneratedPluralSamples)obj).keywordToData);
    }

    @Override
    public int hashCode() {
        return keywordToData.hashCode();
    }

    public static void main(String[] args) {
        Matcher localeMatcher = null;
        if (args.length > 0) {
            localeMatcher = Pattern.compile(args[0]).matcher("");
        }
        int failureCount = 0;

        for (PluralType type : PluralType.values()) {
            Set<String> locales = testInfo.getSupplementalDataInfo().getPluralLocales(type);
            Relation<PluralInfo, String> seenAlready = Relation.of(new TreeMap(), TreeSet.class);

            //System.out.println(type + ": " + locales);
            for (String locale : locales) {
                if (localeMatcher != null && !localeMatcher.reset(locale).find()) {
                    continue;
                }
                PluralInfo pluralInfo = testInfo.getSupplementalDataInfo().getPlurals(type, locale);
                seenAlready.put(pluralInfo, locale);
            }

            Relation<GeneratedPluralSamples, PluralInfo> samplesToPlurals = Relation.of(new LinkedHashMap(), LinkedHashSet.class);
            for (Entry<PluralInfo, Set<String>> entry : seenAlready.keyValuesSet()) {
                PluralInfo pluralInfo = entry.getKey();
                Set<String> equivalentLocales = entry.getValue();
                PluralRules pluralRules = pluralInfo.getPluralRules();
                GeneratedPluralSamples samples = new GeneratedPluralSamples(pluralInfo, type);
                samplesToPlurals.put(samples, pluralInfo);
                for (String keyword : pluralRules.getKeywords()) {
                    Count count = Count.valueOf(keyword);
                    String rule = pluralInfo.getRule(count);
                    if (rule == null && count != Count.other) {
                        pluralInfo.getRule(count);
                        throw new IllegalArgumentException("No rule for " + count);
                    }
                    String representative = equivalentLocales.iterator().next();
                    System.out.print(type + "\t" + representative + "\t" + keyword + "\t" + (rule == null ? "" : rule));
                    Data data = samples.getData(keyword);
                    if (data == null) {
                        System.err.println("***Failure");
                        failureCount++;
                        continue;
                    }
                    System.out.println(data.toString());
                }
                System.out.println();
            }
            for (Entry<PluralInfo, Set<String>> entry : seenAlready.keyValuesSet()) {
                if (entry.getValue().size() == 1) {
                    continue;
                }
                Set<String> remainder = new LinkedHashSet(entry.getValue());
                String first = remainder.iterator().next();
                remainder.remove(first);
                System.out.println(type + "\tEQUIV:\t\t" + first + "\t≣\t" + CollectionUtilities.join(remainder, ", "));
            }
            System.out.println();
            for (Entry<GeneratedPluralSamples, Set<PluralInfo>> entry : samplesToPlurals.keyValuesSet()) {
                Set<PluralInfo> set = entry.getValue();
                if (set.size() != 1) {
                    System.err.println("***Failure: Duplicate results " + set);
                    failureCount++;
                }
            }
        }
        System.err.println("***Failures: " + failureCount);
    }
}
