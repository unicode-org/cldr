package org.unicode.cldr.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableSet;

public class CheckLogicalGroupings extends FactoryCheckCLDR {
    // Change MINIMUM_DRAFT_STATUS to DraftStatus.contributed if you only care about
    // contributed or higher. This can help to reduce the error count when you have a lot of new data.

    static final DraftStatus MIMIMUM_DRAFT_STATUS = DraftStatus.approved;
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
        if (LogicalGrouping.isOptional(getCldrFileToCheck(), path)) {
            return this;
        }

        Set<String> paths = LogicalGrouping.getPaths(getCldrFileToCheck(), path);
        if (paths.size() < 2) return this; // skip if not part of a logical grouping

        // TODO 
        Set<String> paths2 = new HashSet<String>(paths);
        for (String p : paths2) {
            if (LogicalGrouping.isOptional(getCldrFileToCheck(), p)) {
                paths.remove(p);
            }
        }
        if (paths.size() < 2) return this; // skip if not part of a logical grouping

        Set<String> missingPaths = new HashSet<String>();
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
                    .setMessage("This item has a lower draft status (in its logical group) than {0}.", 
                        getPathReferenceForMessage(apath, true))); // the
                break; // no need to continue
            }
        }
        return this;
    }
}
