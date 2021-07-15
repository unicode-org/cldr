package org.unicode.cldr.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.LogicalGrouping.PathType;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class CheckLogicalGroupings extends FactoryCheckCLDR {
    // Change MINIMUM_DRAFT_STATUS to DraftStatus.contributed if you only care about
    // contributed or higher. This can help to reduce the error count when you have a lot of new data.

    static final DraftStatus MIMIMUM_DRAFT_STATUS = DraftStatus.contributed;
    static final Set<Phase> PHASES_CAUSE_ERROR = ImmutableSet.of(Phase.FINAL_TESTING, Phase.VETTING);

    private boolean phaseCausesError;
    private CoverageLevel2 coverageLevel;

    public CheckLogicalGroupings(Factory factory) {
        super(factory);
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);

        // skip the test unless we are at the top level, eg
        //    test root, fr, sr_Latn, ...
        //    but skip fr_CA, sr_Latn_RS, etc.
        // TODO: could simplify some of the code later, since non-topLevel locales are skipped
        // NOTE: we could have a weaker test.
        // Skip if all of the items are either inherited, or aliased *including votes for inherited (3 up arrows)*

        String parent = LocaleIDParser.getParent(cldrFileToCheck.getLocaleID());
        boolean isTopLevel = parent == null || parent.equals("root");
        setSkipTest(!isTopLevel);
        phaseCausesError = PHASES_CAUSE_ERROR.contains(getPhase());

        coverageLevel = CoverageLevel2.getInstance(cldrFileToCheck.getLocaleID());

        return this;
    }


    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*LogicalGroupings.*

    /**
     * We are not as strict with sublocales (where the parent is neither root nor code_fallback).
     * @param path
     * @return
     */
    public boolean isHereOrNonRoot(String path) {
        if (getCldrFileToCheck().isHere(path)) {
            return true;
        }
        if (!getCldrFileToCheck().getLocaleID().contains("_")) { // quick check for top level
            return false;
        }
        String value = getResolvedCldrFileToCheck().getStringValue(path);
        if (value == null) {
            return false;
        }
        // the above items are just for fast checking.
        // check the origin of the value, and make sure it is not ≥ root
        String source = getResolvedCldrFileToCheck().getSourceLocaleID(path, null);
        return !source.equals(XMLSource.ROOT_ID) && !source.equals(XMLSource.CODE_FALLBACK_ID);
    }

    static final int LIMIT_DISTANCE = 5;

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {

        // if (fullPath == null) return this; // skip paths that we don't have
        if (LogicalGrouping.isOptional(getCldrFileToCheck(), path)) {
            return this;
        }

        Output<PathType> pathType = new Output<>();
        Set<String> paths = LogicalGrouping.getPaths(getCldrFileToCheck(), path, pathType);
        if (paths == null || paths.size() < 2) return this; // skip if not part of a logical grouping

        // check the edit distances for count, gender, case
        switch(pathType.value) {
        case COUNT_CASE:
        case COUNT:
        case COUNT_CASE_GENDER:
            // only check the first path
            TreeSet<String> sorted = new TreeSet<>(paths);
            if (path.equals(sorted.iterator().next())) {
                Multiset<String> values = TreeMultiset.create();
                int maxDistance = getMaxDistance(path, value, sorted, values);
                if (maxDistance >= LIMIT_DISTANCE) {
                    maxDistance = getMaxDistance(path, value, paths, values);
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
                        .setSubtype(Subtype.largerDifferences) // typically warningType or errorType
                        .setMessage("{0} different characters within {1}; {2}", maxDistance, showInvisibles(values), pathType.value));
                }
            }
            break;
        default: break;
        }


        // TODO make this more efficient
        Set<String> paths2 = new HashSet<>(paths);
        for (String p : paths2) {
            if (LogicalGrouping.isOptional(getCldrFileToCheck(), p)) {
                paths.remove(p);
            }
        }
        if (paths.size() < 2) return this; // skip if not part of a logical grouping


        Set<String> missingPaths = new HashSet<>();
        boolean havePath = false;
        String firstPath = null;
        for (String apath : paths) {
            if (isHereOrNonRoot(apath)) { // ok
                havePath = true;
            } else {
                if (missingPaths.isEmpty()) {
                    firstPath = apath; // pick the first one in sorted order (LogicalGrouping.getPaths is sorted)
                }
                missingPaths.add(apath);
            }
        }

        if (havePath && !missingPaths.isEmpty()) {
            if (path.equals(firstPath)) {
                Set<String> missingCodes = missingPaths
                    .stream()
                    .map(x -> getPathReferenceForMessage(x, true))
                    .collect(Collectors.toSet());
                Level cLevel = coverageLevel.getLevel(path);

                CheckStatus.Type showError = phaseCausesError ? CheckStatus.errorType : CheckStatus.warningType;
                result.add(new CheckStatus().setCause(this).setMainType(showError)
                    .setSubtype(Subtype.incompleteLogicalGroup)
                    .setMessage("Incomplete logical group - missing values for: {0}; level={1}", missingCodes.toString(), cLevel));
            }
            // skip other errors once we find missing paths
            return this;
        }

        // Special test during vetting phase to allow changes in a logical group when another item in the group
        // contains an error or warning.  See http://unicode.org/cldr/trac/ticket/4943.
        // I added the option lgWarningCheck so that we don't loop back on ourselves forever.
        // JCE: 2015-04-13: I don't think we need this any more, since we implemented
        // http://unicode.org/cldr/trac/ticket/6480, and it really slows things down.
        // I'll just comment it out until we get through a whole release without it.
        // TODO: Remove it completely if we really don't need it.
        //if (Phase.VETTING.equals(this.getPhase()) && options.get(Options.Option.lgWarningCheck) != "true") {
        //    Options checkOptions = options.clone();
        //    checkOptions.set("lgWarningCheck", "true");
        //    List<CheckStatus> statuses = new ArrayList<CheckStatus>();
        //    CompoundCheckCLDR secondaryChecker = CheckCLDR.getCheckAll(CLDRConfig.getInstance().getFullCldrFactory(), ".*");
        //    secondaryChecker.setCldrFileToCheck(getCldrFileToCheck(), checkOptions, statuses);

        //    for (String apath : paths) {
        //        if (apath == path) {
        //            continue;
        //        }
        //        String fPath = getCldrFileToCheck().getFullXPath(apath);
        //        if (fPath == null) {
        //            continue;
        //        }
        //        secondaryChecker.check(apath, fPath, getCldrFileToCheck().getWinningValue(apath), checkOptions, statuses);
        //        if (CheckStatus.hasType(statuses, Type.Error) || CheckStatus.hasType(statuses, Type.Warning)) {
        //            result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
        //                .setSubtype(Subtype.errorOrWarningInLogicalGroup)
        //                .setMessage("Error or warning within this logical group.  May require a change on this item to make the group correct."));
        //            break;
        //        }
        //    }
        //}

        //if (Phase.FINAL_TESTING.equals(this.getPhase())) {

        // Change the structure so we only check for draft status if this path fails;
        // avoids work for ones we are not going to alert on anyway.

        // If the draft status of something in the set is lower, then an implementation that filters out that draft status
        // will get the wrong value.

        String fPath = getCldrFileToCheck().getFullXPath(path);
        XPathParts parts = XPathParts.getFrozenInstance(fPath);
        DraftStatus myStatus = DraftStatus.forString(parts.findFirstAttributeValue("draft"));
        if (myStatus.compareTo(MIMIMUM_DRAFT_STATUS) >= 0) {
            return this; // bail if we are ok
        }

        // If some other path in the LG has a higher draft status, then cause error on this path.
        // NOTE: changed to show in Vetting, not just Resolution

        for (String apath : paths) {
            if (apath.equals(path) || missingPaths.contains(apath)) { // skip this path, skip others unless present
                continue;
            }
            fPath = getCldrFileToCheck().getFullXPath(apath);
            if (fPath == null) {
                continue;
            }
            parts = XPathParts.getFrozenInstance(fPath);
            DraftStatus draftStatus = DraftStatus.forString(parts.findFirstAttributeValue("draft"));

            // Cause error for anything above myStatus

            if (draftStatus.compareTo(myStatus) > 0) {
                CheckStatus.Type showError = phaseCausesError ? CheckStatus.errorType : CheckStatus.warningType;
                result.add(new CheckStatus().setCause(this).setMainType(showError)
                    .setSubtype(Subtype.inconsistentDraftStatus) // typically warningType or errorType
                    .setMessage("This item has draft status={0}, which is lower than the status={1} (for {2}).",
                        myStatus.name(),
                        draftStatus.name(),
                        getPathReferenceForMessage(apath, true))); // the
                break; // no need to continue
            }
        }
        return this;
    }

    static final Transliterator SHOW_INVISIBLES = Transliterator.createFromRules(
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
            b.append(element instanceof Integer ? showInvisibles((int) element) : showInvisibles(element.toString()));
            if (entry.getCount() > 1) {
                b.append('⨱').append(entry.getCount());
            }
        }
        return b.append('}').toString();
    }

    private int getMaxDistance(String path, String value, Set<String> paths, Multiset<String> values) {
        values.clear();
        final CLDRFile cldrFileToCheck = getCldrFileToCheck();
        Set<Fingerprint> fingerprints = new HashSet<>();
        for (String path1 : paths) {
            final String pathValue = cleanSpaces(path.contentEquals(path1) ? value : cldrFileToCheck.getWinningValue(path1));
            values.add(pathValue);
            fingerprints.add(Fingerprint.make(pathValue));
        }
        return Fingerprint.maxDistanceBetween(fingerprints);
    }

    private static final UnicodeMap<String> OTHER_SPACES = new UnicodeMap<String>().putAll(new UnicodeSet("[[:Z:][:S:][:whitespace:]]"), " ").freeze();
    public static String cleanSpaces(String pathValue) {
        return OTHER_SPACES.transform(pathValue);
    }

    // Later
    private static ConcurrentHashMap<String, Fingerprint> FINGERPRINT_CACHE = new ConcurrentHashMap<>();

    /**
     * Use cheap distance metric for testing differences; just the number of characters of each kind.
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
            List<Fingerprint> fingerprints = ImmutableList.copyOf(fingerprintsIn); // The set removes duplicates
            // get the n x n comparisons (but skipping inverses, so (n x (n-1))/2). Quadratic, but most sets are small.
            for (int i = fingerprints.size()-1; i > 0; --i) { // note the lower bound is different for i and j
                final Fingerprint fingerprint_i = fingerprints.get(i);
                for (int j = i-1; j >= 0; --j) {
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

            for (Integer element :allChars) {
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
            return FINGERPRINT_CACHE.computeIfAbsent(value, v -> new Fingerprint(v));
        }

        @Override
        public String toString() {
            return showInvisibles(codePointCounts);
        }
    }
}
