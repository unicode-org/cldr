package org.unicode.cldr.test;

import java.util.Comparator;
import java.util.List;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckExemplars extends CheckCLDR {
	Collator col;
	Collator spaceCol;
	static final UnicodeSet HangulSyllables = new UnicodeSet("[[:Hangul_Syllable_Type=LVT:][:Hangul_Syllable_Type=LV:]]");
	static final UnicodeSet AlwaysOK = new UnicodeSet("[[:script=common:][:script=inherited:]-[:Default_Ignorable_Code_Point:]]"); // [:script=common:][:script=inherited:]
	static final UnicodeSet AllowedInExemplars = new UnicodeSet(AlwaysOK).complement()
		.removeAll(new UnicodeSet("[[:Uppercase:]-[\u0130]]"))
		.addAll(new UnicodeSet("[:Mn:]"));
	//Allowed[:script=common:][:script=inherited:][:alphabetic=false:]
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, possibleErrors);
		String locale = cldrFileToCheck.getLocaleID();
        col = Collator.getInstance(new ULocale(locale));
        spaceCol = Collator.getInstance(new ULocale(locale));
        spaceCol.setStrength(col.PRIMARY);

		return this;
	}

	public CheckCLDR _check(String path, String fullPath, String value, XPathParts pathParts, XPathParts fullPathParts, List result) {
		if (path.indexOf("/exemplarCharacters") < 0) return this;
        checkExemplar(value, result);
        if (path.indexOf("auxiliary") >= 0) {
        	UnicodeSet exemplar1 = new UnicodeSet(value);
        	UnicodeSet exemplar2 = getResolvedCldrFileToCheck().getExemplarSet("auxiliary");
        	if (exemplar2.containsSome(exemplar1)) {
        		UnicodeSet overlap = new UnicodeSet(exemplar1).retainAll(exemplar2).removeAll(HangulSyllables);
        		if (overlap.size() != 0) {
        			String fixedExemplar1 = CollectionUtilities.prettyPrint(overlap, col, col, true);
        	    	result.add(new CheckStatus().setType(CheckStatus.warningType)
        	    	.setMessage("Auxilliary overlaps with main {0}", new Object[]{fixedExemplar1}));   			
        		}
        	}
        }
 		return this;
	}

	private void checkExemplar(String v, List result) {
		if (v == null) return;
    	UnicodeSet exemplar1 = new UnicodeSet(v);
    	String fixedExemplar1 = CollectionUtilities.prettyPrint(exemplar1, col, col, true);
    	UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar1);
    	if (!doubleCheck.equals(exemplar1)) {
	    	result.add(new CheckStatus().setType(CheckStatus.errorType)
	    	    	.setMessage("Internal Error: formatting not working for {0}", new Object[]{exemplar1}));

    	} else if (!v.equals(fixedExemplar1)) {
	    	result.add(new CheckStatus().setType(CheckStatus.warningType)
	    	.setMessage("Better formatting would be {0}", new Object[]{fixedExemplar1}));
    	}
    	if (!AllowedInExemplars.containsAll(exemplar1)) {
    		exemplar1 = CollectionUtilities.flatten(exemplar1).removeAll(AllowedInExemplars);
    		if (exemplar1.size() != 0) {
    		fixedExemplar1 = CollectionUtilities.prettyPrint(exemplar1, col, col, true);
	    	result.add(new CheckStatus().setType(CheckStatus.warningType)
	    	.setMessage("Should be limited to (specific-script - uppercase - invisibles + \u0130); thus not contain: {0}",
	    			new Object[]{fixedExemplar1}));
    		}
    	}
	}
}