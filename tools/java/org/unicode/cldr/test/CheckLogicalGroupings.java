package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.XPathParts;

public class CheckLogicalGroupings extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*LogicalGroupings.*
    
    XPathParts parts = new XPathParts(); // used to parse out a path
        
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
      // if (fullPath == null) return this; // skip paths that we don't have

      Set<String> paths = LogicalGrouping.getPaths(path);
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
          if ( this.getPhase() != null && this.getPhase().equals(Phase.SUBMISSION)) {
              showError = CheckStatus.warningType;
          } else {
              showError = CheckStatus.errorType;
          }
          result.add(new CheckStatus().setCause(this).setMainType(showError).setSubtype(Subtype.incompleteLogicalGroup) 
                        .setMessage("Incomplete logical group - must enter a value for generic, standard, and daylight")); 
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
