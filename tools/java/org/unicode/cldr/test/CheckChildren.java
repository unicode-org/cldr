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
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;

public class CheckChildren extends CheckCLDR {
	CLDRFile[] immediateChildren;
	Map tempSet = new HashMap();
	
	public CheckCLDR handleCheck(String path, String fullPath, String value,
			Map<String, String> options, List<CheckStatus> result) {
    if (isSkipTest()) return this; // disabled
    if (fullPath == null) return this; // skip paths that we don't have
    if (immediateChildren == null) return this; // skip
		

		//String current = getResolvedCldrFileToCheck().getStringValue(path);
		tempSet.clear();
		for (int i = 0; i < immediateChildren.length; ++i) {
			String otherValue;
			try {
				otherValue = immediateChildren[i].getWinningValue(path);
			} catch (RuntimeException e) {
				throw e;
			}
			tempSet.put(immediateChildren[i].getLocaleID(), otherValue);
		}
		if (tempSet.values().contains(value)) return this;
    
		CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.valueAlwaysOverridden)
    .setCheckOnSubmit(false)
		.setMessage("Value always overridden in children: {0}", new Object[]{tempSet.keySet().toString()});
		result.add(item);
		tempSet.clear(); // free for gc
		return this;
	}

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
		Matcher myLocalePlus = Pattern.compile(cldrFileToCheck.getLocaleID() + "_[^_]*").matcher("");
		Set children = cldrFileToCheck.getAvailableLocales();
		List iChildren = new ArrayList();
		for (Iterator it = children.iterator(); it.hasNext();) {
			String locale = (String)it.next();
			if (!myLocalePlus.reset(locale).matches()) continue;
			CLDRFile child = cldrFileToCheck.make(locale, true);
			if (child == null) {
				CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.nullChildFile)
				.setMessage("Null file from: {0}", new Object[]{locale});
				possibleErrors.add(item);				
			} else {
				iChildren.add(child);
			}
		}
		if (iChildren.size() == 0) immediateChildren = null;
		else {
			immediateChildren = new CLDRFile[iChildren.size()];
			immediateChildren = (CLDRFile[]) iChildren.toArray(immediateChildren);
		}
		return this;
	}

}
