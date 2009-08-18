package org.unicode.cldr.test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.util.ULocale;

public class CheckExemplars extends CheckCLDR {
	Collator col;
	Collator spaceCol;
	boolean isRoot;
	static final UnicodeSet HangulSyllables = (UnicodeSet) new UnicodeSet("[[:Hangul_Syllable_Type=LVT:][:Hangul_Syllable_Type=LV:]]").freeze();
	static final UnicodeSet AlwaysOK = (UnicodeSet) new UnicodeSet("[[[:script=common:][:script=inherited:]-[:Default_Ignorable_Code_Point:]] [\u066A-\u066C]]").freeze(); //[\\u200c-\\u200f] [:script=common:][:script=inherited:]
	public static final UnicodeSet AllowedInExemplars = (UnicodeSet) new UnicodeSet(AlwaysOK).complement()
		.addAll(new UnicodeSet("[[:Mn:][:word_break=Katakana:][:word_break=ALetter:][:word_break=MidLetter:]]")
    .removeAll(new UnicodeSet("[[:Uppercase:]-[\u0130]]"))
    .add(0x200E,0x200F) // add RLM, LRM
		).freeze();
	public static final UnicodeSet TO_REMOVE_FROM_EXEMPLARS = (UnicodeSet) new UnicodeSet(AllowedInExemplars).complement().freeze();

	//Allowed[:script=common:][:script=inherited:][:alphabetic=false:]
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
		if (cldrFileToCheck == null) return this;
		super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
		String locale = cldrFileToCheck.getLocaleID();
        col = Collator.getInstance(new ULocale(locale));
        spaceCol = Collator.getInstance(new ULocale(locale));
        spaceCol.setStrength(col.PRIMARY);
        isRoot = cldrFileToCheck.getLocaleID().equals("root");
		return this;
	}

	public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
	  if (fullPath == null) return this; // skip paths that we don't have
	  if (path.indexOf("/exemplarCharacters") < 0) return this;
	  boolean isAuxiliary = path.indexOf("auxiliary") >= 0;
	  checkExemplar(value, result, isAuxiliary);

	  // check relation to auxiliary set
	  try {       	
	    UnicodeSet mainSet = getResolvedCldrFileToCheck().getExemplarSet("", CLDRFile.WinningChoice.WINNING);
	    if (path.indexOf("auxiliary") < 0) {
	      // check for auxiliary anyway

	      UnicodeSet auxiliarySet = getResolvedCldrFileToCheck().getExemplarSet("auxiliary", CLDRFile.WinningChoice.WINNING);

	      if (auxiliarySet == null) {
	        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.missingAuxiliaryExemplars)
	                .setMessage("Missing Auxiliary Set")
	                .setHTMLMessage("Most languages allow <i>some<i> auxiliary characters, so review this."));   			
	      }

	    } else { // auxiliary
	      UnicodeSet auxiliarySet = new UnicodeSet(value);
	      if (false && auxiliarySet.containsSome(mainSet)) {
	        UnicodeSet overlap = new UnicodeSet(mainSet).retainAll(auxiliarySet).removeAll(HangulSyllables);
	        if (overlap.size() != 0) {
	          String fixedExemplar1 = new PrettyPrinter()
            .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
            .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                    .setStrength2(Collator.PRIMARY))
                    .setCompressRanges(true)
                    .format(overlap);
	          result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.auxiliaryExemplarsOverlap)
	                  .setMessage("Auxiliary overlaps with main \u200E{0}\u200E", new Object[]{fixedExemplar1}));   			
	        }
	      }
	    }

	    Boolean localeIsRTL = false;
	    String opath = getResolvedCldrFileToCheck().getFullXPath("//ldml/layout/orientation");
	    XPathParts oparts = new XPathParts(null,null);
	    oparts.initialize(opath);
	    String lelement = oparts.getElement(-1);
	    String charOrientation = oparts.findAttributeValue(lelement,"characters");
	    if ( charOrientation.equals("right-to-left")) {
	      localeIsRTL = true;
	    }

	    UnicodeSetIterator mi = new UnicodeSetIterator(mainSet);
	    while (mi.next()) {
	      if ( mi.codepoint != UnicodeSetIterator.IS_STRING && 
	              ( UCharacter.getDirection(mi.codepoint) == UCharacterDirection.RIGHT_TO_LEFT ||
	                      UCharacter.getDirection(mi.codepoint) == UCharacterDirection.RIGHT_TO_LEFT_ARABIC ) &&
	                      ! localeIsRTL ) {
	        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.orientationDisagreesWithExemplars)
	                .setMessage("Main exemplar set contains RTL characters, but orientation of this locale is not RTL."));
	        break;
	      }
	    }
	  } catch (Exception e) {} // if these didn't parse, checkExemplar will be called anyway at some point
	  return this;
	}

	private void checkExemplar(String v, List<CheckStatus> result, boolean isAuxiliary) {
		if (v == null) return;
		UnicodeSet exemplar1;
    	try {
    		exemplar1 = new UnicodeSet(v);
    	} catch (Exception e) {
	    	result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.illegalExemplarSet)
	    	    	.setMessage("This field must be a set of the form [a b c-d ...]: ", new Object[]{e.getMessage()}));
    		return;
    	}
    	String fixedExemplar1 = new PrettyPrinter()
        .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
        .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                .setStrength2(Collator.PRIMARY))
                .setCompressRanges(true)
                .format(exemplar1);
    	UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar1);
    	if (!doubleCheck.equals(exemplar1)) {
	    	result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.internalUnicodeSetFormattingError)
	    	    	.setMessage("Internal Error: formatting not working for {0}", new Object[]{exemplar1}));

    	}
//    	else if (!v.equals(fixedExemplar1)) {
//	    	result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType)
//	    	.setMessage("Better formatting would be \u200E{0}\u200E", new Object[]{fixedExemplar1}));
//    	}
    	if (false && !AllowedInExemplars.containsAll(exemplar1)) {
    		exemplar1 = CollectionUtilities.flatten(exemplar1).removeAll(AllowedInExemplars);
    		if (exemplar1.size() != 0) {
    		fixedExemplar1 = new PrettyPrinter()
            .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
            .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                    .setStrength2(Collator.PRIMARY))
                    .setCompressRanges(true)
                    .format(exemplar1);
	    	result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.illegalCharactersInExemplars)
	    	.setMessage("Should be limited to (specific-script - uppercase - invisibles + \u0130); thus not contain: \u200E{0}\u200E",
	    			new Object[]{fixedExemplar1}));
    		}
    	} else if (!isRoot && exemplar1.size() == 0) {
        if (isAuxiliary) {
          result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.missingAuxiliaryExemplars)
              .setMessage("Empty Auxiliary Set.")
              .setHTMLMessage("Most languages allow <i>some<i> auxiliary characters, so review this."));   
        } else {
          result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.missingMainExemplars)
              .setMessage("Exemplar set must not be empty.")
              .setHTMLMessage("Exemplar set must not be empty -- that would imply that this language uses no letters!"));    
        }
    	}
	}
}
