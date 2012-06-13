package org.unicode.cldr.test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

public class CheckLogicalGroupings extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*LogicalGroupings.*
    
    private XPathParts parts = new XPathParts(); // used to parse out a path
    private Set<Count> pluralTypes;

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck,
            Map<String, String> options, List<CheckStatus> possibleErrors) {
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(
                cldrFileToCheck.getSupplementalDirectory()); 
        PluralInfo pluralInfo = supplementalData.getPlurals(PluralType.cardinal,
                cldrFileToCheck.getLocaleID());
        pluralTypes = pluralInfo.getCountToExamplesMap().keySet();
        return this;
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        // if (fullPath == null) return this; // skip paths that we don't have
        Set<String> paths;
        if (path.indexOf("/decimalFormats") > 0 && path.indexOf("[@count=") > 0) {
            // Check that all plural forms are present.
            paths = new HashSet<String>();
            parts.set(path);
            for (Count count : pluralTypes) {
                parts.setAttribute("pattern", "count", count.toString());
                paths.add(parts.toString());
            }
        } else {
            paths = LogicalGrouping.getPaths(path);
        }
        if (paths.size() < 2) return this; // skip if not part of a logical grouping
        boolean logicalGroupingIsEmpty = true;
        for (String apath: paths) {
            if (getCldrFileToCheck().isHere(apath)) {
                logicalGroupingIsEmpty = false;
                break;
            }
        }
        if (logicalGroupingIsEmpty) return this; // skip if the logical grouping is empty
        if (!getCldrFileToCheck().isHere(path)) {
            String showError;
            if ( this.getPhase() != null && ( this.getPhase().equals(Phase.SUBMISSION) || this.getPhase().equals(Phase.VETTING))) {
                showError = CheckStatus.warningType;
            } else {
                showError = CheckStatus.errorType;
            }
            result.add(new CheckStatus().setCause(this).setMainType(showError).setSubtype(Subtype.incompleteLogicalGroup) 
                    .setMessage("Incomplete logical group - must enter a value for all fields in the group")); 
        }

        if (this.getPhase() != null && this.getPhase().equals(Phase.FINAL_TESTING)) {
            Set<String> draftStatuses = new TreeSet<String>();
            for ( String apath : paths) {
                parts.set(getCldrFileToCheck().getFullXPath(apath));
                String draftStatus = parts.findFirstAttributeValue("draft");
                if ( draftStatus == null ) {
                    draftStatus = "approved";
                }
                draftStatuses.add(draftStatus);
            }
            if ( draftStatuses.size() != 1 ) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.inconsistentDraftStatus) // typically warningType or errorType
                        .setMessage("Inconsistent draft status within a logical group")); // the message; can be MessageFormat with arguments
            }
        }          
        return this;
    }    
}
