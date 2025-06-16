package org.unicode.cldr.unittest;

import com.google.common.collect.ComparisonChain;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.unicode.cldr.util.Joiners;

public class InvestigateCollation {
    public static void main(String[] args) throws Exception {
        boolean bounded = false;
        boolean showProgress = false;

        RuleBasedCollator root = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);

        // Map based on collation elements, and based on their primaries

        UnicodeMultimap<CE> ceMap = new UnicodeMultimap<>(TreeMap::new);
        UnicodeMultimap<Long> pMap = new UnicodeMultimap<>(TreeMap::new);

        // just for tracking

        int next = 0x1000;
        long bottom = 0;
        long top = Long.MIN_VALUE; // largest unsigned

        for (int codePoint = 'a'; codePoint < 0x110000; ++codePoint) {
            int type = UCharacter.getType(codePoint);
            if (type == UCharacterCategory.UNASSIGNED
                    || type == UCharacterCategory.PRIVATE_USE
                    || type == UCharacterCategory.SURROGATE) {
                continue;
            }
            CE ce = CE.from(root, codePoint);
            if (ce.bogus) {
                // This is just an artifact of the current implementation; just uses longs for the
                // fields
                System.out.println(
                        "Too many collation elements: "
                                + ce
                                + "\t"
                                + Long.toHexString(codePoint)
                                + "\t"
                                + Character.toString(codePoint));
                continue;
            }

            if (showProgress && codePoint >= next) {
                System.out.println(
                        Joiners.TAB.join(
                                "Processing:",
                                Integer.toHexString(codePoint),
                                Character.toString(codePoint),
                                ce));
                next += 0x1000;
            }
            ceMap.put(ce, codePoint);
            pMap.put(ce.primary, codePoint);
            if (codePoint == 'a') {
                bottom = ce.primary;
            } else if (codePoint == 'Ω') {
                top = ce.primary;
            }
        }

        UnicodeSet contractions = new UnicodeSet();
        UnicodeSet expansions = new UnicodeSet();

        root.getContractionsAndExpansions(contractions, expansions, true);
        contractions.freeze();
        expansions.freeze();

        System.out.println(
                "\nContractions:\t" + contractions.size() + "\t" + contractions.toPattern(false));

        System.out.println(
                "\nExpansions:\t" + expansions.size() + "\t" + expansions.toPattern(false));

        //        UnicodeSet contractAndExpansions = new UnicodeSet().addAll(expansions.strings());
        //        expansions = new UnicodeSet(expansions).removeAll(expansions.strings());
        //
        //        System.out.println("\nExpansions & Contractions:\t"  +
        // contractAndExpansions.size() + "\t" +  contractAndExpansions.toPattern(false));

        System.out.println("\nShowing primaries with more than one character");

        final long bottom2 = bottom;
        final long top2 = top;
        System.out.println("bottom: " + Long.toHexString(bottom2));
        System.out.println("top: " + Long.toHexString(top2));

        pMap.entrySet().stream()
                .filter(
                        x ->
                                !bounded
                                        || (0 <= Long.compareUnsigned(bottom2, x.getKey())
                                                && 0 <= Long.compareUnsigned(x.getKey(), top2)))
                .filter(x -> x.getValue().size() > 1)
                .forEach(
                        x ->
                                System.out.println(
                                        Long.toHexString(x.getKey())
                                                + "\t"
                                                + x.getValue().size()
                                                + "\t"
                                                + x.getValue().toPattern(false)));
    }

    // Puts together a compound CE. Currently uses longs, so 20-30 characters aren't handled

    static class CE implements Comparable<CE> {
        static final long OVERFLOW16 = 0xFFFF000000000000l;
        static final long OVERFLOW8 = 0xFF00000000000000l;

        long primary;
        long secondary;
        long tertiary;
        boolean bogus;

        // later, fix to make final fields

        public static CE from(RuleBasedCollator root, int codePoint) {
            CE ce = new CE();
            CollationElementIterator cei =
                    root.getCollationElementIterator(Character.toString(codePoint));
            while (true) {
                int collationElement = cei.next();
                if (collationElement == CollationElementIterator.NULLORDER) {
                    break;
                }
                ce.add(collationElement);
            }
            return ce;
        }

        void add(int collationElement) {
            int p = CollationElementIterator.primaryOrder(collationElement);
            int s = CollationElementIterator.secondaryOrder(collationElement);
            int t = CollationElementIterator.tertiaryOrder(collationElement);

            if (p != 0) {
                if ((primary & OVERFLOW16) != 0) {
                    bogus = true;
                }
                primary = (primary << 16) | p;
            }
            if (s != 0) {
                if ((secondary & OVERFLOW8) != 0) {
                    bogus = true;
                }
                secondary = (secondary << 8) | s;
            }
            if (t != 0) {
                if ((tertiary & OVERFLOW8) != 0) {
                    bogus = true;
                }
                tertiary = (tertiary << 8) | t;
            }
        }

        @Override
        public int compareTo(CE o) {
            return ComparisonChain.start()
                    .compare(primary, o.primary)
                    .compare(secondary, o.secondary)
                    .compare(tertiary, o.tertiary)
                    .result();
        }

        @Override
        public boolean equals(Object obj) {
            return compareTo((CE) obj) == 0;
        }

        @Override
        public int hashCode() {
            return (int) (primary ^ secondary ^ tertiary);
        }

        @Override
        public String toString() {
            return Joiners.SP.join(
                    Long.toHexString(primary),
                    Long.toHexString(secondary),
                    Long.toHexString(tertiary));
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
    }
}
