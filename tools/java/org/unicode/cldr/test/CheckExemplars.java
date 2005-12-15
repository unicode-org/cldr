package org.unicode.cldr.test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckExemplars extends CheckCLDR {
	Collator col;
	Collator spaceCol;
	boolean isRoot;
	static final UnicodeSet HangulSyllables = new UnicodeSet("[[:Hangul_Syllable_Type=LVT:][:Hangul_Syllable_Type=LV:]]");
	static final UnicodeSet AlwaysOK = new UnicodeSet("[[:script=common:][:script=inherited:]-[:Default_Ignorable_Code_Point:]]"); // [:script=common:][:script=inherited:]
	static final UnicodeSet AllowedInExemplars = new UnicodeSet(AlwaysOK).complement()
		.removeAll(new UnicodeSet("[[:Uppercase:]-[\u0130]]"))
		.addAll(new UnicodeSet("[[:Mn:][:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:]]"));
	//Allowed[:script=common:][:script=inherited:][:alphabetic=false:]
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		String locale = cldrFileToCheck.getLocaleID();
        col = Collator.getInstance(new ULocale(locale));
        spaceCol = Collator.getInstance(new ULocale(locale));
        spaceCol.setStrength(col.PRIMARY);
        isRoot = cldrFileToCheck.getLocaleID().equals("root");
		return this;
	}

	public CheckCLDR handleCheck(String path, String fullPath, String value, Map options, List result) {
		if (path.indexOf("/exemplarCharacters") < 0) return this;
        checkExemplar(value, result);
        // check relation to auxiliary set
       	if (path.indexOf("auxiliary") < 0) {
           	UnicodeSet auxiliarySet = getResolvedCldrFileToCheck().getExemplarSet("auxiliary");
       		if (auxiliarySet == null || auxiliarySet.size() == 0) {
       			result.add(new CheckStatus().setType(CheckStatus.errorType)
       					.setMessage("Missing Auxiliary Set")
       					.setHTMLMessage("Missing Auxiliary Set:" +
       					" see <a href='http://www.unicode.org/cldr/data_formats.html#Exemplar'>Exemplars</a>"));   			
       		}
       	} else { // auxiliary
   			UnicodeSet auxiliarySet = new UnicodeSet(value);
   			UnicodeSet mainSet = getResolvedCldrFileToCheck().getExemplarSet("");
   			if (auxiliarySet.containsSome(mainSet)) {
   				UnicodeSet overlap = new UnicodeSet(mainSet).retainAll(auxiliarySet).removeAll(HangulSyllables);
   				if (overlap.size() != 0) {
   					String fixedExemplar1 = CollectionUtilities.prettyPrint(overlap, true, null, null, col, col);
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
    	String fixedExemplar1 = CollectionUtilities.prettyPrint(exemplar1, true, null, null, col, col);
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
    		fixedExemplar1 = CollectionUtilities.prettyPrint(exemplar1, true, null, null, col, col);
	    	result.add(new CheckStatus().setType(CheckStatus.warningType)
	    	.setMessage("Should be limited to (specific-script - uppercase - invisibles + \u0130); thus not contain: {0}",
	    			new Object[]{fixedExemplar1}));
    		}
    	} else if (!isRoot && exemplar1.size() == 0) {
   			result.add(new CheckStatus().setType(CheckStatus.errorType)
   					.setMessage("Exemplar set must not be empty.")
   					.setHTMLMessage("Exemplar set must not be empty:" +
   					" see <a href='http://www.unicode.org/cldr/data_formats.html#Exemplar'>Exemplars</a>"));   			
    	}
	}
}