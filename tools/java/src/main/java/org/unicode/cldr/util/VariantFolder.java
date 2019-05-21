package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.XEquivalenceClass.SetMaker;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

public class VariantFolder {
    private AlternateFetcher alternateFetcher;
    public static SetMaker mySetMaker = new SetMaker() {
        Comparator c = new UTF16.StringComparator(true, false, 0);
        Comparator bestIsLowest = new Comparator() {
            public int compare(Object o1, Object o2) {
                String s1 = o1.toString();
                String s2 = o2.toString();
                final boolean casefold1 = UCharacter.foldCase(s1, true).equals(s1);
                final boolean casefold2 = UCharacter.foldCase(s2, true).equals(s2);
                if (casefold1 != casefold2) {
                    return casefold1 ? -1 : 1;
                }
                final boolean canonical1 = Normalizer.isNormalized(s1, Normalizer.COMPOSE, 0);
                final boolean canonical2 = Normalizer.isNormalized(s2, Normalizer.COMPOSE, 0);
                if (canonical1 != canonical2) {
                    return canonical1 ? -1 : 1;
                }
                int len1 = s1.codePointCount(0, s1.length());
                int len2 = s2.codePointCount(0, s2.length());
                if (len1 != len2) {
                    return len1 - len2;
                }
                return c.compare(s1, s2);
            }

        };

        public Set make() {
            return new TreeSet(bestIsLowest);
        }
    };
    public static final UnicodeSet NORMAL_CHARS = new UnicodeSet("[^[:c:]]");

    //private String source;

    //private Set<String> result;

    public interface AlternateFetcher {
        /**
         * The input string MUST be in the output set. Note that the results must be
         * valid even if input string might not be on even code point boundaries.
         * For example, if the input is "XabY" where X and Y are have surrogates,
         * and the alternates are by case, then the results have to be {"XabY",
         * "XAbY", "XaBY", "XABY"}.
         * <p>
         * The caller must never modify the set.
         *
         * @param item
         * @return
         */
        Set<String> getAlternates(String item, Set<String> output);
    }

    public static class CompatibilityFolder implements VariantFolder.AlternateFetcher {
        private static final UnicodeSet NORMAL_CHARS = new UnicodeSet("[^[:c:]]");
        static XEquivalenceClass equivalents = new XEquivalenceClass("none", mySetMaker);
        static {
            for (UnicodeSetIterator it = new UnicodeSetIterator(NORMAL_CHARS); it.next();) {
                String item = it.getString();
                equivalents.add(item, Normalizer.decompose(item, true));
                equivalents.add(item, Normalizer.compose(item, true));
            }
        }

        public Set<String> getAlternates(String item, Set<String> output) {
            output.add(item);
            return equivalents.getEquivalences(item);
        }

    }

    public static class CanonicalFolder implements VariantFolder.AlternateFetcher {
        static XEquivalenceClass equivalents = new XEquivalenceClass("none", mySetMaker);
        static {
            for (UnicodeSetIterator it = new UnicodeSetIterator(NORMAL_CHARS); it.next();) {
                String item = it.getString();
                equivalents.add(item, Normalizer.decompose(item, false));
                equivalents.add(item, Normalizer.compose(item, false));
            }
        }

        public Set<String> getAlternates(String item, Set<String> output) {
            output.add(item);
            return equivalents.getEquivalences(item);
        }

    }

    public static class CaseVariantFolder implements VariantFolder.AlternateFetcher {
        private static final UnicodeSet NORMAL_CHARS = new UnicodeSet("[^[:c:]]");
        static XEquivalenceClass equivalents = new XEquivalenceClass("none", mySetMaker);
        static {
            for (UnicodeSetIterator it = new UnicodeSetIterator(NORMAL_CHARS); it.next();) {
                String item = it.getString();
                equivalents.add(item, UCharacter.toLowerCase(item));
                equivalents.add(item, UCharacter.toUpperCase(item));
                equivalents.add(item, UCharacter.foldCase(item, true));
                equivalents.add(item, UCharacter.toTitleCase(ULocale.ROOT, item, null));
            }
        }

        public Set<String> getAlternates(String item, Set<String> output) {
            output.add(item);
            return equivalents.getEquivalences(item);
        }
    }

    /**
     * The class is designed to be immutable, at least as far as Java allows. That is, if the alternateFetcher is, then
     * it will be.
     *
     * @param alternateFetcher
     */
    public VariantFolder(AlternateFetcher alternateFetcher) {
        this.alternateFetcher = alternateFetcher;
    }

    // We keep track of the alternates for each combination of start,len
    // so with a length of 3 we have the following structure
    // {{0,1}, {1,2}, {2,3} -- length of 1
    // {0,2}, {1,3}
    // {0,3}}

    @SuppressWarnings("unchecked")
    public Set<String> getClosure(String source) {
        int stringLength = source.length();
        if (stringLength == 0) {
            Set<String> result = new HashSet<String>();
            result.add(source);
            return result;
        }
        Set<String>[][] combos = new Set[stringLength][];
        for (int i = 0; i < stringLength; ++i) {
            combos[i] = new Set[stringLength - i];
        }
        for (int i = 0; i < stringLength; ++i) {
            combos[0][i] = alternateFetcher.getAlternates(source.substring(i, i + 1),
                new HashSet<String>());
        }
        for (int level = 1; level < stringLength; ++level) {
            // at each level, we add strings of that length (plus 1)
            for (int start = 0; start < stringLength - level; ++start) {
                int limit = start + level + 1;
                // System.out.println(start + ", " + limit);
                // we first add any longer alternates
                Collection<String> current = combos[level][start] = new HashSet<String>();
                current.addAll(alternateFetcher.getAlternates(source.substring(start,
                    limit), new HashSet<String>()));
                // then we add the cross product of shorter strings
                for (int breakPoint = start + 1; breakPoint < limit; ++breakPoint) {
                    addCrossProduct(combos[breakPoint - start - 1][start], combos[limit
                        - breakPoint - 1][breakPoint], current);
                }
            }
        }
        return combos[combos.length - 1][0];
    }

    private void addCrossProduct(Collection<String> source1,
        Collection<String> source2, Collection<String> output) {
        for (String x : source1) {
            for (String y : source2) {
                output.add(x + y);
            }
        }
    }

    public UnicodeSet getClosure(UnicodeSet input) {
        UnicodeSet result = new UnicodeSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(input); it.next();) {
            Set<String> temp = getClosure(it.getString());
            for (String s : temp) {
                result.add(s);
            }
        }
        return result;
    }

    public String reduce(String s) {
        Set<String> temp = getClosure(s);
        return temp.iterator().next();
    }

    public UnicodeSet reduce(UnicodeSet input) {
        UnicodeSet result = new UnicodeSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(input); it.next();) {
            final String reduce = reduce(it.getString());
            result.add(reduce);
        }
        return result;
    }
}