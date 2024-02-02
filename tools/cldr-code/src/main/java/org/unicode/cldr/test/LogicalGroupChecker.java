package org.unicode.cldr.test;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.ibm.icu.util.Output;
import java.util.*;
import java.util.stream.Collectors;
import org.unicode.cldr.util.*;

public class LogicalGroupChecker {

    private static final int LIMIT_DISTANCE = 5;

    private final CheckLogicalGroupings checkLogicalGroupings;

    /**
     * The path to check. Any error/warning will be attached to this path. This path is not
     * optional, since CheckLogicalGroupings.handleCheck skips optional paths.
     */
    private final String pathToCheck;

    private final String value;
    private final Output<LogicalGrouping.PathType> pathType;
    private final CLDRFile cldrFile;
    private final List<CheckCLDR.CheckStatus> result;
    private final Set<String> paths;

    private final boolean phaseCausesError;
    private final CoverageLevel2 coverageLevel;

    /** Have we found at least one non-missing non-optional path in the group? */
    private boolean groupHasOneOrMorePresentRequiredPaths = false;

    /**
     * The first (in sorted order) required path determined to be missing
     *
     * <p>If multiple required paths are missing, only the first one gets a notification attached to
     * it, and that notification includes a list of all the missing required items; for example,
     * "Incomplete logical group - missing values for: [1, 0]"
     */
    private String firstMissingRequiredPath = null;

    private Set<String> presentPaths = null;
    private Set<String> optionalPaths = null;
    private Set<String> missingRequiredPaths = null;

    private CLDRFile.DraftStatus myStatus;

    public LogicalGroupChecker(
            CheckLogicalGroupings checkLogicalGroupings,
            String path,
            String value,
            List<CheckCLDR.CheckStatus> result) {
        this.checkLogicalGroupings = checkLogicalGroupings;
        this.pathToCheck = path;
        this.value = value;
        this.result = result;
        pathType = new Output<>();
        cldrFile = checkLogicalGroupings.getCldrFileToCheck();
        paths = LogicalGrouping.getPaths(cldrFile, path, pathType);
        coverageLevel =
                CoverageLevel2.getInstance(
                        SupplementalDataInfo.getInstance(), cldrFile.getLocaleID());
        phaseCausesError =
                CheckLogicalGroupings.PHASES_CAUSE_ERROR.contains(checkLogicalGroupings.getPhase());
    }

    public void run() {
        if (paths == null || paths.size() < 2) {
            return; // skip if not part of a logical grouping
        }
        checkEditDistances();
        if (checkMissingRequiredPaths()) {
            return; // skip other errors once we find missing paths
        }
        if (avoidDraftStatusWork()) {
            return; // bail if we are ok
        }
        paths.removeAll(optionalPaths);
        checkHigherDraftStatus();
    }

    private void checkEditDistances() {
        switch (pathType.value) {
            case COUNT_CASE:
            case COUNT:
            case COUNT_CASE_GENDER:
                // only check the first path
                TreeSet<String> sorted = new TreeSet<>(paths);
                if (pathToCheck.equals(sorted.iterator().next())) {
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
                            .setSubtype(
                                    CheckCLDR.CheckStatus.Subtype
                                            .largerDifferences) // typically warningType or
                            // errorType
                            .setMessage(
                                    "{0} different characters within {1}; {2}",
                                    maxDistance,
                                    CheckLogicalGroupings.showInvisibles(values),
                                    value));
        }
    }

    private int getMaxDistance(Set<String> paths, Multiset<String> values) {
        values.clear();
        Set<CheckLogicalGroupings.Fingerprint> fingerprints = new HashSet<>();
        for (String path1 : paths) {
            final String pathValue =
                    CheckLogicalGroupings.cleanSpaces(
                            pathToCheck.contentEquals(path1)
                                    ? value
                                    : cldrFile.getWinningValue(path1));
            values.add(pathValue);
            fingerprints.add(CheckLogicalGroupings.Fingerprint.make(pathValue));
        }
        return CheckLogicalGroupings.Fingerprint.maxDistanceBetween(fingerprints);
    }

    private boolean checkMissingRequiredPaths() {
        getPresentAndOptionalPaths();
        getMissingRequiredPaths();
        if (groupIsIncomplete()) {
            handleMissingRequiredPaths();
            return true;
        }
        return false;
    }

    private void getPresentAndOptionalPaths() {
        presentPaths = new HashSet<>();
        optionalPaths = new HashSet<>();
        for (String apath : paths) {
            if (isHereOrNonRoot(apath)) {
                presentPaths.add(apath);
            }
            if (LogicalGrouping.isOptional(cldrFile, apath)) {
                optionalPaths.add(apath);
            }
        }
    }

    private void getMissingRequiredPaths() {
        missingRequiredPaths = new HashSet<>();
        for (String apath : paths) {
            if (optionalPaths.contains(apath)) {
                continue;
            }
            if (apath.contains("beaufort")) {
                // TODO CLDR-17352 Missing grammatical inflections for unit Beaufort in many locales
                continue;
            }
            if (presentPaths.contains(apath)) {
                groupHasOneOrMorePresentRequiredPaths = true;
            } else {
                if (missingRequiredPaths.isEmpty()) {
                    firstMissingRequiredPath =
                            apath; // pick the first one in sorted order (LogicalGrouping.getPaths
                    // is sorted)
                }
                missingRequiredPaths.add(apath);
            }
        }
    }

    /**
     * Is the group incomplete, such that it should cause an "incomplete logical group" notification
     * for the path to be checked?
     *
     * @return true if the group is incomplete
     *     <p>If pathToCheck.equals(firstMissingRequiredPath), then there is a missing required
     *     path. That doesn't necessarily imply we have an "incomplete logical group" as defined by
     *     this code, for the purpose of making a notification. For example, if all paths in a group
     *     are missing, it's not "incomplete" (all missing paths within the coverage level are
     *     notified as missing, but there is no "incomplete logical group" notification). For
     *     another example, if a group has only one required path, and that path is missing, the
     *     group is incomplete if and only if it also has a present optional path.
     */
    private boolean groupIsIncomplete() {
        return (pathToCheck.equals(firstMissingRequiredPath)
                && ((groupHasOneOrMorePresentRequiredPaths && groupHasTwoOrMoreRequiredPaths())
                        || groupHasOneOrMorePresentOptionalPaths()));
    }

    private boolean groupHasTwoOrMoreRequiredPaths() {
        return paths.size() >= optionalPaths.size() + 2;
    }

    private boolean groupHasOneOrMorePresentOptionalPaths() {
        Set<String> intersect = new HashSet<>(presentPaths);
        intersect.retainAll(optionalPaths);
        return intersect.size() >= 1;
    }

    private void handleMissingRequiredPaths() {
        Set<String> missingCodes =
                missingRequiredPaths.stream()
                        .map(x -> checkLogicalGroupings.getPathReferenceForMessage(x, true))
                        .collect(Collectors.toSet());
        Level cLevel = coverageLevel.getLevel(pathToCheck);

        CheckCLDR.CheckStatus.Type showError =
                phaseCausesError
                        ? CheckCLDR.CheckStatus.errorType
                        : CheckCLDR.CheckStatus.warningType;
        result.add(
                new CheckCLDR.CheckStatus()
                        .setCause(checkLogicalGroupings)
                        .setMainType(showError)
                        .setSubtype(CheckCLDR.CheckStatus.Subtype.incompleteLogicalGroup)
                        .setMessage(
                                "Incomplete logical group - missing values for: {0}; level={1}",
                                missingCodes.toString(), cLevel));
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
    private boolean avoidDraftStatusWork() {
        // only check for draft status if this path fails;
        // avoids work for ones we are not going to alert on anyway.

        // If the draft status of something in the set is lower, then an implementation that filters
        // out that draft status
        // will get the wrong value.
        final String fPath = cldrFile.getFullXPath(pathToCheck);
        final XPathParts parts = XPathParts.getFrozenInstance(fPath);
        myStatus = CLDRFile.DraftStatus.forString(parts.findFirstAttributeValue("draft"));
        return myStatus.compareTo(CheckLogicalGroupings.MIMIMUM_DRAFT_STATUS) >= 0;
    }

    private void checkHigherDraftStatus() {
        // If some other path in the LG has a higher draft status, then cause error on this path.
        // NOTE: changed to show in Vetting, not just Resolution
        for (String apath : paths) {
            if (apath.equals(pathToCheck)
                    || missingRequiredPaths.contains(
                            apath)) { // skip this path, skip others unless present
                continue;
            }
            final String fPath = cldrFile.getFullXPath(apath);
            if (fPath == null) {
                continue;
            }
            final XPathParts parts = XPathParts.getFrozenInstance(fPath);
            final CLDRFile.DraftStatus draftStatus =
                    CLDRFile.DraftStatus.forString(parts.findFirstAttributeValue("draft"));

            // Cause error for anything above myStatus
            if (draftStatus.compareTo(myStatus) > 0) {
                addOneHigherStatus(apath, myStatus, draftStatus);
                break; // no need to continue
            }
        }
    }

    private void addOneHigherStatus(
            String apath, CLDRFile.DraftStatus myStatus, CLDRFile.DraftStatus draftStatus) {
        CheckCLDR.CheckStatus.Type showError =
                phaseCausesError
                        ? CheckCLDR.CheckStatus.errorType
                        : CheckCLDR.CheckStatus.warningType;
        result.add(
                new CheckCLDR.CheckStatus()
                        .setCause(checkLogicalGroupings)
                        .setMainType(showError)
                        .setSubtype(
                                CheckCLDR.CheckStatus.Subtype
                                        .inconsistentDraftStatus) // typically warningType or
                        // errorType
                        .setMessage(
                                "This item has draft status={0}, which is lower than the status={1} (for {2}).",
                                myStatus.name(),
                                draftStatus.name(),
                                checkLogicalGroupings.getPathReferenceForMessage(apath, true)));
    }
}
