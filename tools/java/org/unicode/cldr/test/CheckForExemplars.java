/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckForExemplars extends CheckCLDR {
	//private final UnicodeSet commonAndInherited = new UnicodeSet(CheckExemplars.Allowed).complement(); 
	// "[[:script=common:][:script=inherited:][:alphabetic=false:]]");
	static String[] EXEMPLAR_SKIPS = {"/currencySpacing", "/hourFormat", "/exemplarCharacters", "/pattern",
        "/localizedPatternChars", "/segmentations", "/dateFormatItem", "/references"};

	UnicodeSet exemplars;
	boolean skip;
	Collator col;
	Collator spaceCol;
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFile, Map options, List possibleErrors) {
		if (cldrFile == null) return this;
		skip = true;
		super.setCldrFileToCheck(cldrFile, options, possibleErrors);
		if (cldrFile.getLocaleID().equals("root")) {
			return this;
		}
		String locale = cldrFile.getLocaleID();
        col = Collator.getInstance(new ULocale(locale));
        spaceCol = Collator.getInstance(new ULocale(locale));
        spaceCol.setStrength(col.PRIMARY);

		CLDRFile resolvedFile = cldrFile.getResolved();
		exemplars = resolvedFile.getExemplarSet("");
		if (exemplars == null) {
		  CheckStatus item = new CheckStatus().setCause(this).setType(CheckStatus.errorType)
		  .setMessage("No Exemplar Characters: {0}", new Object[]{this.getClass().getName()});
		  possibleErrors.add(item);
		  return this;
		}
		//UnicodeSet temp = resolvedFile.getExemplarSet("standard");
		//if (temp != null) exemplars.addAll(temp);
		UnicodeSet auxiliary = resolvedFile.getExemplarSet("auxiliary");
		if (auxiliary != null) exemplars.addAll(auxiliary);
		exemplars.addAll(CheckExemplars.AlwaysOK);
		skip = false;
		return this;
	}
	public CheckCLDR handleCheck(String path, String fullPath, String value,
			Map options, List result) {
		if (skip) return this;
/*srl*/ if(path == null) { 
            throw new InternalError("Empty path!");
        } else if(getCldrFileToCheck() == null) {
            throw new InternalError("no file to check!");
        }
        String sourceLocale = getCldrFileToCheck().getSourceLocaleID(path, null);
        if (XMLSource.CODE_FALLBACK_ID.equals(sourceLocale)) {
            return this;
        } else if ("root".equals(sourceLocale)) {
            // skip eras for non-gregorian
            if (path.indexOf("/calendar") >= 0 && path.indexOf("gregorian") <= 0) return this;
        }
		for (int i = 0; i < EXEMPLAR_SKIPS.length; ++i) {
			if (path.indexOf(EXEMPLAR_SKIPS[i]) > 0 ) return this; // skip some items.
		}
		if (path.startsWith("//ldml/posix/messages")) return this;

		if (exemplars.containsAll(value)) return this;
		UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(exemplars);
		String fixedMissing = CollectionUtilities.prettyPrint(missing, true, null, null, col, col);
		CheckStatus item = new CheckStatus().setCause(this).setType(CheckCLDR.finalErrorType)
			.setMessage("Not in exemplars: \u200E{0}\u200E", new Object[]{fixedMissing});
		result.add(item);
		return this;
	}

}