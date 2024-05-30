package org.unicode.cldr.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.ibm.icu.impl.UnicodeMap;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.With;

public class CheckLogicalGroupings extends FactoryCheckCLDR {
    // Change MINIMUM_DRAFT_STATUS to DraftStatus.contributed if you only care about
    // contributed or higher. This can help to reduce the error count when you have a lot of new
    // data.
    static final DraftStatus MIMIMUM_DRAFT_STATUS = DraftStatus.contributed;
    static final Set<Phase> PHASES_CAUSE_ERROR =
            ImmutableSet.of(Phase.FINAL_TESTING, Phase.VETTING);

    public CheckLogicalGroupings(Factory factory) {
        super(factory);
    }

    @Override
    public CheckCLDR handleSetCldrFileToCheck(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        super.handleSetCldrFileToCheck(cldrFileToCheck, options, possibleErrors);

        // skip the test unless we are at the top level, eg
        //    test root, fr, sr_Latn, ...
        //    but skip fr_CA, sr_Latn_RS, etc.
        // TODO: could simplify some of the code later, since non-topLevel locales are skipped
        // NOTE: we could have a weaker test.
        // Skip if all of the items are either inherited, or aliased *including votes for inherited
        // (3 up arrows)*

        String parent = LocaleIDParser.getParent(cldrFileToCheck.getLocaleID());
        boolean isTopLevel = parent == null || parent.equals("root");
        setSkipTest(!isTopLevel);
        return this;
    }

    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.*
    // -t.*LogicalGroupings.*

    @Override
    public CheckCLDR handleCheck(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (LogicalGrouping.isOptional(getCldrFileToCheck(), path)) {
            return this;
        }
        if (!accept(result)) return this;
        new LogicalGroupChecker(this, path, value, result).run();
        return this;
    }

    static final Transliterator SHOW_INVISIBLES =
            Transliterator.createFromRules(
                    "show",
                    "([[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]-[\\u0020]]) > &hex/perl($1);",
                    Transliterator.FORWARD);

    public static String showInvisibles(String value) {
        return SHOW_INVISIBLES.transform(value);
    }

    public static String showInvisibles(int codePoint) {
        return showInvisibles(With.fromCodePoint(codePoint));
    }

    public static String showInvisibles(Multiset<?> codePointCounts) {
        StringBuilder b = new StringBuilder().append('{');
        for (Entry<?> entry : codePointCounts.entrySet()) {
            if (b.length() > 1) {
                b.append(", ");
            }
            Object element = entry.getElement();
            b.append(
                    element instanceof Integer
                            ? showInvisibles((int) element)
                            : showInvisibles(element.toString()));
            if (entry.getCount() > 1) {
                b.append('⨱').append(entry.getCount());
            }
        }
        return b.append('}').toString();
    }

    private static final UnicodeMap<String> OTHER_SPACES =
            new UnicodeMap<String>()
                    .putAll(new UnicodeSet("[[:Z:][:S:][:whitespace:]]"), " ")
                    .freeze();

    public static String cleanSpaces(String pathValue) {
        return OTHER_SPACES.transform(pathValue);
    }

    // Later
    private static final ConcurrentHashMap<String, Fingerprint> FINGERPRINT_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Use cheap distance metric for testing differences; just the number of characters of each
     * kind.
     */
    public static class Fingerprint {
        private final Multiset<Integer> codePointCounts;

        private Fingerprint(String value) {
            Multiset<Integer> result = TreeMultiset.create();
            for (int cp : With.codePointArray(value)) {
                result.add(cp);
            }
            codePointCounts = ImmutableMultiset.copyOf(result);
        }

        public static int maxDistanceBetween(Set<Fingerprint> fingerprintsIn) {
            int distance = 0;
            List<Fingerprint> fingerprints =
                    ImmutableList.copyOf(fingerprintsIn); // The set removes duplicates
            // get the n x n comparisons (but skipping inverses, so (n x (n-1))/2). Quadratic, but
            // most sets are small.
            for (int i = fingerprints.size() - 1;
                    i > 0;
                    --i) { // note the lower bound is different for i and j
                final Fingerprint fingerprint_i = fingerprints.get(i);
                for (int j = i - 1; j >= 0; --j) {
                    final Fingerprint fingerprints_j = fingerprints.get(j);
                    final int currentDistance = fingerprint_i.getDistanceTo(fingerprints_j);
                    distance = Math.max(distance, currentDistance);
                }
            }
            return distance;
        }

        public int getDistanceTo(Fingerprint that) {
            int distance = 0;
            Set<Integer> allChars = new HashSet<>(that.codePointCounts.elementSet());
            allChars.addAll(codePointCounts.elementSet());

            for (Integer element : allChars) {
                final int count = codePointCounts.count(element);
                final int otherCount = that.codePointCounts.count(element);
                distance += Math.abs(count - otherCount);
            }
            return distance;
        }

        public int size() {
            return codePointCounts.size();
        }

        public static Fingerprint make(String value) {
            return FINGERPRINT_CACHE.computeIfAbsent(value, Fingerprint::new);
        }

        @Override
        public String toString() {
            return showInvisibles(codePointCounts);
        }
    }
}
