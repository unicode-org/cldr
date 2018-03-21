package org.unicode.cldr.test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

public class CheckLogicalGroupings extends FactoryCheckCLDR {

    public CheckLogicalGroupings(Factory factory) {
        super(factory);
    }

    // Change MINIMUM_DRAFT_STATUS to DraftStatus.contributed if you only care about
    // contributed or higher. This can help to reduce the error count when you have a lot of new data.

    static final DraftStatus MIMIMUM_DRAFT_STATUS = DraftStatus.approved;

    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*LogicalGroupings.*

    //private XPathParts parts = new XPathParts(); // used to parse out a path

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

    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        // if (fullPath == null) return this; // skip paths that we don't have
        if (LogicalGrouping.isOptional(getCldrFileToCheck(), path)) return this;
        Set<String> paths = LogicalGrouping.getPaths(getCldrFileToCheck(), path);
        Set<String> paths2 = new HashSet<String>(paths);
        for (String p : paths2) {
            if (LogicalGrouping.isOptional(getCldrFileToCheck(), p)) {
                paths.remove(p);
            }
        }
        if (paths.size() < 2) return this; // skip if not part of a logical grouping
        int logicalGroupingCount = 0;
        for (String apath : paths) {
            if (isHereOrNonRoot(apath)) {
                logicalGroupingCount++;
            }
        }
        if (logicalGroupingCount == 0) return this; // skip if the logical grouping is empty
        if (!isHereOrNonRoot(path) ||
            (this.getPhase().equals(Phase.FINAL_TESTING) && logicalGroupingCount != paths.size())) {
            CheckStatus.Type showError = CheckStatus.errorType;
            if (this.getPhase().equals(Phase.BUILD)) {
                showError = CheckStatus.warningType;
            }
            result.add(new CheckStatus().setCause(this).setMainType(showError)
                .setSubtype(Subtype.incompleteLogicalGroup)
                .setMessage("Incomplete logical group - must enter a value for all fields in the group"));
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
        DraftStatus myStatus = null;
        EnumMap<DraftStatus, PathHeader> draftStatuses = new EnumMap<DraftStatus, PathHeader>(DraftStatus.class);
        for (String apath : paths) {
            String fPath = getCldrFileToCheck().getFullXPath(apath);
            if (fPath == null) {
                continue;
            }
            if (apath.startsWith("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day")) {
                int debug = 0;
            }
            XPathParts parts = XPathParts.getFrozenInstance(fPath);
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
                draftStatuses.put(draftStatus, getPathHeaderFactory().fromPath(apath));
            }
        }
        if (draftStatuses.size() > 1 && myStatus != DraftStatus.approved) { // only show errors for the items that
            // have insufficient status
            if (myStatus != null) { // remove my status from the list
                draftStatuses.remove(myStatus);
            }
            CheckStatus.Type showError = CheckStatus.warningType;
            if (this.getPhase().equals(Phase.FINAL_TESTING)) {
                showError = CheckStatus.errorType;
            }
            result.add(new CheckStatus().setCause(this).setMainType(showError)
                .setSubtype(Subtype.inconsistentDraftStatus) // typically warningType or errorType
                .setMessage("Logical group problem.  All members of a logical group need to be confirmed together. "
                    + "For how to do this, see <a target='CLDR-ST-DOCS' href='http://cldr.org/translation/logical-groups'>Logical Groups</a>"
                    + "​ in the CLDR translation guidelines.")); // the
            // message;
            // can
            // be
            // MessageFormat
            // with
            // arguments
        }
        // }
        return this;
    }
}
