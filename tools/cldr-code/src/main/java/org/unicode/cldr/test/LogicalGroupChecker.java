package org.unicode.cldr.test;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.ibm.icu.util.Output;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.cldr.util.*;

public class LogicalGroupChecker {

    private static final int LIMIT_DISTANCE = 5;

    private final CheckLogicalGroupings checkLogicalGroupings;
    private final String path;
    private final String value;
    private final Output<LogicalGrouping.PathType> pathType;
    private final CLDRFile cldrFile;
    private final List<CheckCLDR.CheckStatus> result;
    private final Set<String> paths;

    private final boolean phaseCausesError;
    private final CoverageLevel2 coverageLevel;

    private boolean havePath = false;
    private String firstPath = null;
    private Set<String> missingPaths = null;
    private CLDRFile.DraftStatus myStatus;

    public LogicalGroupChecker(
        CheckLogicalGroupings checkLogicalGroupings,
        String path,
        String value,
        List<CheckCLDR.CheckStatus> result
    ) {
        this.checkLogicalGroupings = checkLogicalGroupings;
        this.path = path;
        this.value = value;
        this.result = result;
        pathType = new Output<>();
        cldrFile = checkLogicalGroupings.getCldrFileToCheck();
        paths = LogicalGrouping.getPaths(cldrFile, path, pathType);
        coverageLevel = CoverageLevel2.getInstance(SupplementalDataInfo.getInstance(), cldrFile.getLocaleID());
        phaseCausesError = CheckLogicalGroupings.PHASES_CAUSE_ERROR.contains(checkLogicalGroupings.getPhase());
    }

    public void run() {
        if (isTrivial()) {
            return; // skip if not part of a logical grouping
        }
        checkEditDistances();
        removeOptionalPaths();
        if (isTrivial()) {
            return; // skip if not part of a logical grouping
        }
        if (findMissingPaths()) {
            return; // skip other errors once we find missing paths
        }
        if (avoidWork()) {
            return; // bail if we are ok
        }
        loop();
    }

    private boolean isTrivial() {
        // skip if not part of a logical grouping
        return (paths == null || paths.size() < 2);
    }

    private void checkEditDistances() {
        switch (pathType.value) {
            case COUNT_CASE:
            case COUNT:
            case COUNT_CASE_GENDER:
                // only check the first path
                TreeSet<String> sorted = new TreeSet<>(paths);
                if (path.equals(sorted.iterator().next())) {
                    reallyCheckEditDistances(sorted);
                }
                break;
            default:
                break;
        }
    }

    private void reallyCheckEditDistances(TreeSet<String> sorted) {
        Multiset<String> values = TreeMultiset.create();
        int maxDistance = getMaxDistance(sorted, values);
        if (maxDistance >= LIMIT_DISTANCE) {
            maxDistance = getMaxDistance(paths, values);
            result.add(
                new CheckCLDR.CheckStatus()
                    .setCause(checkLogicalGroupings)
                    .setMainType(CheckCLDR.CheckStatus.warningType)
                    .setSubtype(CheckCLDR.CheckStatus.Subtype.largerDifferences) // typically warningType or errorType
                    .setMessage(
                        "{0} different characters within {1}; {2}",
                        maxDistance,
                        CheckLogicalGroupings.showInvisibles(values),
                        value
                    )
            );
        }
    }

    private int getMaxDistance(Set<String> paths, Multiset<String> values) {
        values.clear();
        Set<CheckLogicalGroupings.Fingerprint> fingerprints = new HashSet<>();
        for (String path1 : paths) {
            final String pathValue = CheckLogicalGroupings.cleanSpaces(
                path.contentEquals(path1) ? value : cldrFile.getWinningValue(path1)
            );
            values.add(pathValue);
            fingerprints.add(CheckLogicalGroupings.Fingerprint.make(pathValue));
        }
        return CheckLogicalGroupings.Fingerprint.maxDistanceBetween(fingerprints);
    }

    private void removeOptionalPaths() {
        LogicalGrouping.removeOptionalPaths(paths, cldrFile);
    }

    private boolean findMissingPaths() {
        missingPaths = getMissingPaths();
        if (havePath && !missingPaths.isEmpty()) {
            if (path.equals(firstPath)) {
                handleMissingPaths();
                return true;
            }
        }
        return false;
    }

    private void handleMissingPaths() {
        Set<String> missingCodes = missingPaths
            .stream()
            .map(x -> checkLogicalGroupings.getPathReferenceForMessage(x, true))
            .collect(Collectors.toSet());
        Level cLevel = coverageLevel.getLevel(path);

        CheckCLDR.CheckStatus.Type showError = phaseCausesError
            ? CheckCLDR.CheckStatus.errorType
            : CheckCLDR.CheckStatus.warningType;
        result.add(
            new CheckCLDR.CheckStatus()
                .setCause(checkLogicalGroupings)
                .setMainType(showError)
                .setSubtype(CheckCLDR.CheckStatus.Subtype.incompleteLogicalGroup)
                .setMessage(
                    "Incomplete logical group - missing values for: {0}; level={1}",
                    missingCodes.toString(),
                    cLevel
                )
        );
    }

    private Set<String> getMissingPaths() {
        Set<String> missingPaths = new HashSet<>();
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
        return missingPaths;
    }

    /**
     * We are not as strict with sublocales (where the parent is neither root nor code_fallback).
     *
     * @param path the path
     * @return true or false
     */
    private boolean isHereOrNonRoot(String path) {
        if (cldrFile.isHere(path)) {
            return true;
        }
        if (!cldrFile.getLocaleID().contains("_")) { // quick check for top level
            return false;
        }
        final CLDRFile resolvedCldrFile = checkLogicalGroupings.getResolvedCldrFileToCheck();
        if (resolvedCldrFile.getStringValue(path) == null) {
            return false;
        }
        // the above items are just for fast checking.
        // check the origin of the value, and make sure it is not ≥ root
        String source = resolvedCldrFile.getSourceLocaleID(path, null);
        return !source.equals(XMLSource.ROOT_ID) && !source.equals(XMLSource.CODE_FALLBACK_ID);
    }

    /**
     * Return true if we're finished
     *
     * @return true or false
     */
    private boolean avoidWork() {
        // only check for draft status if this path fails;
        // avoids work for ones we are not going to alert on anyway.

        // If the draft status of something in the set is lower, then an implementation that filters out that draft status
        // will get the wrong value.
        final String fPath = cldrFile.getFullXPath(path);
        final XPathParts parts = XPathParts.getFrozenInstance(fPath);
        myStatus = CLDRFile.DraftStatus.forString(parts.findFirstAttributeValue("draft"));
        return myStatus.compareTo(CheckLogicalGroupings.MIMIMUM_DRAFT_STATUS) >= 0;
    }

    private void loop() {
        // If some other path in the LG has a higher draft status, then cause error on this path.
        // NOTE: changed to show in Vetting, not just Resolution
        for (String apath : paths) {
            if (apath.equals(path) || missingPaths.contains(apath)) { // skip this path, skip others unless present
                continue;
            }
            final String fPath = cldrFile.getFullXPath(apath);
            if (fPath == null) {
                continue;
            }
            final XPathParts parts = XPathParts.getFrozenInstance(fPath);
            final CLDRFile.DraftStatus draftStatus = CLDRFile.DraftStatus.forString(
                parts.findFirstAttributeValue("draft")
            );

            // Cause error for anything above myStatus
            if (draftStatus.compareTo(myStatus) > 0) {
                addOneHigherStatus(apath, myStatus, draftStatus);
                break; // no need to continue
            }
        }
    }

    private void addOneHigherStatus(String apath, CLDRFile.DraftStatus myStatus, CLDRFile.DraftStatus draftStatus) {
        CheckCLDR.CheckStatus.Type showError = phaseCausesError
            ? CheckCLDR.CheckStatus.errorType
            : CheckCLDR.CheckStatus.warningType;
        result.add(
            new CheckCLDR.CheckStatus()
                .setCause(checkLogicalGroupings)
                .setMainType(showError)
                .setSubtype(CheckCLDR.CheckStatus.Subtype.inconsistentDraftStatus) // typically warningType or errorType
                .setMessage(
                    "This item has draft status={0}, which is lower than the status={1} (for {2}).",
                    myStatus.name(),
                    draftStatus.name(),
                    checkLogicalGroupings.getPathReferenceForMessage(apath, true)
                )
        );
    }
}
