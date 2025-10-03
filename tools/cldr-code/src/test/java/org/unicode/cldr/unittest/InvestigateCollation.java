package org.unicode.cldr.unittest;

import com.google.common.collect.Comparators;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Joiners;

public class InvestigateCollation {
    static boolean showProgress = false;

    static RuleBasedCollator root = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
    static UnicodeMultimap<Primaries> primaryToCps = new UnicodeMultimap<>(TreeMap::new);
    static UnicodeMap<Primaries> cpsToPrimary = new UnicodeMap<>();

    static {
        root.setStrength(Collator.PRIMARY);
        int next = -1;
        for (int codePoint = 0; codePoint < 0x110000; ++codePoint) {
            int type = UCharacter.getType(codePoint);
            if (type == UCharacterCategory.UNASSIGNED
                    || type == UCharacterCategory.PRIVATE_USE
                    || type == UCharacterCategory.SURROGATE) {
                continue;
            }
            Primaries p = new Primaries(Character.toString(codePoint), 0);
            if (showProgress && codePoint >= next) {
                System.out.println(
                        Joiners.TAB.join(
                                "Processing:",
                                Integer.toHexString(codePoint),
                                Character.toString(codePoint),
                                p));
                next += 0x1000;
            }
            primaryToCps.put(p, codePoint);
            cpsToPrimary.put(codePoint, p);
        }
        /*
         *
         *
         UnicodeSet contractions = new UnicodeSet();
        UnicodeSet expansions = new UnicodeSet();

        root.getContractionsAndExpansions(contractions, expansions, true);
        contractions.freeze();
        expansions.freeze();

         */

    }

    public static void main(String[] args) {
        //        Primaries empty = new Primaries("\u0000", 0);
        //
        //        System.out.println("Primaries for null: " + empty);
        //        UnicodeSet setForEmpty = primaryToCps.get(empty);
        //        System.out.println(
        //                "Codepoints with empty primaries " + setForEmpty.size() + " " +
        // setForEmpty);
        Counter<Integer> counter = new Counter<>();
        Map<Integer, Primaries> samples = new TreeMap<>();
        Map<Integer, String> sampleString = new TreeMap<>();
        for (Entry<String, Primaries> keys : cpsToPrimary.entrySet()) {
            String sample = keys.getKey();
            Primaries primary = keys.getValue();
            int size = primary.primaries.size();
            counter.add(size, 1);
            if (!samples.containsKey(size)) {
                samples.put(size, primary);
                sampleString.put(size, sample);
            }
        }
        System.out.println("Size\tCount\tSample\tHex\tPrimaries");
        samples.entrySet().stream()
                .forEach(
                        x ->
                                System.out.println(
                                        Joiners.TAB.join(
                                                x.getKey(),
                                                counter.get(x.getKey()),
                                                sampleString.get(x.getKey()),
                                                Utility.hex(sampleString.get(x.getKey())),
                                                x.getValue())));

        StringRange rawQuery = new StringRange("a", "c");
        Query query = new Query(rawQuery);

        System.out.println("\nQuery\tLB\tUB\t#1stChars\t1stChars");
        System.out.println(rawQuery + "\t" + query);
        System.out.println();

        List<StringRange> tests =
                List.of(new StringRange("d", "f"), new StringRange("c"), new StringRange("𝕮"));
        for (StringRange cluster : tests) {
            boolean couldMatch = query.couldMatch(cluster);
            System.out.println(
                    "cluster " + cluster + " could match query " + rawQuery + ": " + couldMatch);
        }
    }

    static class Primaries implements Comparable<Primaries> {
        final List<Integer> primaries;

        public Primaries(String cps, int increment) {
            CollationElementIterator cei = root.getCollationElementIterator(cps);
            List<Integer> result =
                    new ArrayList<>(); // should be unsigned short, but simpler in prototype with
            // int
            while (true) {
                int collationElement = cei.next();
                if (collationElement == CollationElementIterator.NULLORDER) {
                    break;
                }
                int p = CollationElementIterator.primaryOrder(collationElement);
                if (p != 0) {
                    result.add(p);
                }
            }
            primaries = List.copyOf(result);
        }

        public Primaries(int singlePrimary) {
            primaries = List.of(singlePrimary);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (!(obj instanceof Primaries)) {
                return false;
            }
            return primaries.equals(((Primaries) obj).primaries);
        }

        @Override
        public int compareTo(Primaries other) {
            return iterableComparator.compare(primaries, other.primaries);
        }

        @Override
        public int hashCode() {
            return primaries.hashCode();
        }

        @Override
        public String toString() {
            return "["
                    + primaries.stream()
                            .map(x -> Utility.hex(x, 1))
                            .collect(Collectors.joining(","))
                    + "]";
        }

        static final Comparator<Iterable<Integer>> iterableComparator =
                Comparators.lexicographical(Comparator.<Integer>naturalOrder());
    }

    static class Query {
        final Primaries lowerBound;
        final Primaries upperBound;
        final UnicodeSet firstChars = new UnicodeSet();

        public Query(StringRange rawQuery) {
            lowerBound = new Primaries(rawQuery.lowerBound, 0);
            upperBound = new Primaries(rawQuery.upperBound, 1);
            for (int cp = lowerBound.primaries.get(0); cp <= upperBound.primaries.get(0); ++cp) {
                UnicodeSet uset = primaryToCps.get(new Primaries(cp));
                if (uset != null) {
                    firstChars.addAll(uset);
                }
            }
        }

        boolean couldMatch(StringRange stringRange) {
            return firstChars.containsSome(
                    stringRange.lowerBound.codePointAt(0), stringRange.upperBound.codePointAt(0));
        }

        @Override
        public String toString() {
            return Joiners.COMMA_SP.join(
                    lowerBound, upperBound, firstChars.size(), firstChars.toPattern(false));
        }
    }

    static class StringRange {
        public StringRange(String lowerBound, String upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        public StringRange(String string) {
            this(string, string);
        }

        final String lowerBound;
        final String upperBound;

        @Override
        public String toString() {
            return "«" + lowerBound + "," + upperBound + "»";
        }
    }

    // UnicodeMap is too slow

    static class UnicodeMultimap<T> {
        Map<T, UnicodeSet> data;

        UnicodeMultimap(Supplier<Map<T, UnicodeSet>> supplier) {
            data = supplier.get();
        }

        public UnicodeSet get(T primary) {
            return data.get(primary);
        }

        public Set<T> keySet() {
            return data.keySet();
        }

        public Set<Entry<T, UnicodeSet>> entrySet() {
            return data.entrySet();
        }

        UnicodeMultimap<T> put(T datum, int codePoint) {
            UnicodeSet uset = data.get(datum);
            if (uset == null) {
                data.put(datum, uset = new UnicodeSet());
            }
            uset.add(codePoint);
            return this;
        }

        public Multimap<Long, T> invertCodePointsInto(Multimap<Long, T> target) {
            data.entrySet().stream()
                    .forEach(
                            x ->
                                    x.getValue()
                                            .codePointStream()
                                            .forEach(y -> target.put((long) y, x.getKey())));
            return target;
        }
    }
}
