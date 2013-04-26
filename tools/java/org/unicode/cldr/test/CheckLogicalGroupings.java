package org.unicode.cldr.test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.XPathParts;

public class CheckLogicalGroupings extends CheckCLDR {

    // Change MINIMUM_DRAFT_STATUS to DraftStatus.contributed if you only care about
    // contributed or higher. This can help to reduce the error count when you have a lot of new data.

    static final DraftStatus MIMIMUM_DRAFT_STATUS = DraftStatus.approved;

    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*LogicalGroupings.*

    private XPathParts parts = new XPathParts(); // used to parse out a path

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
        List<CheckStatus> result) {
        // if (fullPath == null) return this; // skip paths that we don't have
        if (LogicalGrouping.isOptional(getCldrFileToCheck(), path)) return this;
        Set<String> paths = LogicalGrouping.getPaths(getCldrFileToCheck(), path);
        if (paths.size() < 2) return this; // skip if not part of a logical grouping
        boolean logicalGroupingIsEmpty = true;
        for (String apath : paths) {
            if (getCldrFileToCheck().isHere(apath)) {
                logicalGroupingIsEmpty = false;
                break;
            }
        }
        if (logicalGroupingIsEmpty) return this; // skip if the logical grouping is empty
        if (!getCldrFileToCheck().isHere(path)) {
            CheckStatus.Type showError = CheckStatus.warningType;
            if (this.getPhase() != null && !this.getPhase().equals(Phase.FINAL_TESTING)) {
                showError = CheckStatus.warningType;
            } else {
                showError = CheckStatus.errorType;
            }
            result.add(new CheckStatus().setCause(this).setMainType(showError)
                .setSubtype(Subtype.incompleteLogicalGroup)
                .setMessage("Incomplete logical group - must enter a value for all fields in the group"));
        }

        if (this.getPhase() != null && this.getPhase().equals(Phase.FINAL_TESTING)) {
            Factory factory = PathHeader.getFactory(this.getDisplayInformation());
            DraftStatus myStatus = null;
            EnumMap<DraftStatus, PathHeader> draftStatuses = new EnumMap<DraftStatus, PathHeader>(DraftStatus.class);
            for (String apath : paths) {
                String fPath = getCldrFileToCheck().getFullXPath(apath);
                if (fPath == null) {
                    continue;
                }
                parts.set(fPath);
                DraftStatus draftStatus = DraftStatus.forString(parts.findFirstAttributeValue("draft"));

                // anything at or above the minimum is ok.

                if (draftStatus.compareTo(MIMIMUM_DRAFT_STATUS) >= 0) {
                    draftStatus = DraftStatus.approved;
                }
                if (apath.equals(path)) { // record what this path has, for later.
                    myStatus = draftStatus;
                }
                PathHeader old = draftStatuses.get(draftStatus);
                if (old == null) { // take first or path itself
                    draftStatuses.put(draftStatus, factory.fromPath(apath));
                }
            }
            if (draftStatuses.size() > 1 && myStatus != DraftStatus.approved) { // only show errors for the items that
                                                                                // have insufficient status
                if (myStatus != null) { // remove my status from the list
                    draftStatuses.remove(myStatus);
                }
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.inconsistentDraftStatus) // typically warningType or errorType
                    .setMessage("Inconsistent draft status within a logical group: {0}", draftStatuses.values())); // the
                                                                                                                   // message;
                                                                                                                   // can
                                                                                                                   // be
                                                                                                                   // MessageFormat
                                                                                                                   // with
                                                                                                                   // arguments
            }
        }
        return this;
    }
}
