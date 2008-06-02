package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

public class CheckAlt extends CheckCLDR {
	
	XPathParts parts = new XPathParts();
	//CLDRFile.Status status = new CLDRFile.Status();
	Set seenSoFar = new HashSet();
	
	// determine if we have an alt=...proposed
	// if we have one, and there is not a non-proposed version -- in this same file, unaliased, there's a problem.
	public CheckCLDR handleCheck(String path, String fullPath, String value,
			Map<String, String> options, List<CheckStatus> result) {
    if (fullPath == null) return this; // skip paths that we don't have
    
     // quick checks
    if (path.indexOf("[@alt=") <= 0) {
      return this;
    }
    if (path.indexOf("proposed") <= 0) {
      return this;
    }

    String strippedPath = CLDRFile.getNondraftNonaltXPath(path);
    if (strippedPath.equals(path)) {
      return this; // paths equal, skip
    }
    
    
    String otherValue = getCldrFileToCheck().getStringValue(strippedPath);
    if (otherValue != null) {
      return this;
    }
//		String localeID = getCldrFileToCheck().getSourceLocaleID(path, null);
//		if (!localeID.equals(getCldrFileToCheck().getLocaleID())) {
//      return this; // must be same file
//    }
		//if (!status.pathWhereFound.equals(path)) return this; // must be unaliased
		
//    if (fullPath.contains("x999")) {
//      result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType)
//          .setMessage("There was a conflict introduced as a result of fixing default contents: please pick among the values or add a corrected value.", new Object[]{}));
//    }
    
//		String strippedPath = removeProposed(path);
//		if (strippedPath.equals(path)) return this; // happened to match "proposed" but wasn't in 'alt';
//		
//		localeID = getCldrFileToCheck().getSourceLocaleID(strippedPath, null);
//		// if localeID is null, or if it is CODE_FALLBACK_ID or root, we have a potential problem.
//		if (localeID == null || localeID.equals(XMLSource.CODE_FALLBACK_ID)) { //  || localeID.equals("root")
//			String message = strippedPath;
//      boolean checkOnSubmit = true;
    
//			if (seenSoFar.contains(strippedPath)) {
//        message += "MULTIPLE! ";
//        checkOnSubmit = false;
//      }
			result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.noUnproposedVariant)
          .setCheckOnSubmit(false)
					.setMessage("Proposed item but no unproposed variant", new Object[]{}));
			seenSoFar.add(strippedPath);


		return this;
	}

//	private String removeProposed(String path) {
//		parts.set(path);
//		for (int i = 0; i < parts.size(); ++i) {
//			Map attributes = parts.getAttributes(i);
//			for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
//				String attribute = (String) it.next();
//				if (!attribute.equals("alt")) continue;
//				String attributeValue = (String) attributes.get(attribute);
//				int pos = attributeValue.indexOf("proposed");
//				if (pos < 0) continue;
//				if (pos > 0 && attributeValue.charAt(pos-1) == '-') --pos; // backup for "...-proposed"
//				if (pos == 0) {
//					attributes.remove(attribute);
//					continue;
//				}			
//				attributeValue = attributeValue.substring(0,pos); // strip it off
//				attributes.put(attribute, attributeValue);
//			}
//		}
//		String strippedPath = parts.toString();
//		return strippedPath;
//	}

	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
		if (cldrFileToCheck == null) return this;
    // Skip if the phase is not final testing
    if (Phase.FINAL_TESTING == getPhase()) {
      setSkipTest(false); // ok
    } else {
      setSkipTest(true);
      return this;
    }

		super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		seenSoFar.clear();
		return this;
	}
//		Matcher myLocalePlus = Pattern.compile(cldrFileToCheck.getLocaleID() + "_[^_]*").matcher("");
//		Set children = cldrFileToCheck.getAvailableLocales();
//		List iChildren = new ArrayList();
//		for (Iterator it = children.iterator(); it.hasNext();) {
//			String locale = (String)it.next();
//			if (!myLocalePlus.reset(locale).matches()) continue;
//			CLDRFile child = cldrFileToCheck.make(locale, true);
//			if (child == null) {
//				CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
//				.setMessage("Null file from: {0}", new Object[]{locale});
//				possibleErrors.add(item);				
//			} else {
//				iChildren.add(child);
//			}
//		}
//		if (iChildren.size() == 0) immediateChildren = null;
//		else {
//			immediateChildren = new CLDRFile[iChildren.size()];
//			immediateChildren = (CLDRFile[]) iChildren.toArray(immediateChildren);
//		}
//		return this;
//	}

}
