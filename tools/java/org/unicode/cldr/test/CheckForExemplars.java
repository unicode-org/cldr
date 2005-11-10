/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckForExemplars extends CheckCLDR {
	//private final UnicodeSet commonAndInherited = new UnicodeSet(CheckExemplars.Allowed).complement(); 
	// "[[:script=common:][:script=inherited:][:alphabetic=false:]]");
	static String[] EXEMPLAR_SKIPS = {"/hourFormat", "/exemplarCharacters", "/pattern", "/localizedPatternChars", "/segmentations"};

	UnicodeSet exemplars;
	boolean skip;
	Collator col;
	Collator spaceCol;
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFile, List possibleErrors) {
		if (cldrFile == null) return this;
		skip = true;
		super.setCldrFileToCheck(cldrFile, possibleErrors);
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
			CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
			.setMessage("Failure to Initialize: {0} (need resolved locale)", new Object[]{this.getClass().getName()});
			possibleErrors.add(item);
			return this;
		}
		UnicodeSet temp = resolvedFile.getExemplarSet("standard");
		if (temp != null) exemplars.addAll(temp);
		UnicodeSet auxiliary = resolvedFile.getExemplarSet("auxiliary");
		if (auxiliary != null) exemplars.addAll(auxiliary);
		exemplars.addAll(CheckExemplars.AlwaysOK);
		skip = false;
		return this;
	}
	public CheckCLDR _check(String path, String fullPath, String value,
			XPathParts pathParts, XPathParts fullPathParts, List result) {
		if (skip) return this;
		for (int i = 0; i < EXEMPLAR_SKIPS.length; ++i) {
			if (path.indexOf(EXEMPLAR_SKIPS[i]) > 0 ) return this; // skip some items.
		}
		if (path.startsWith("//ldml/posix/messages")) return this;

		if (exemplars.containsAll(value)) return this;
		UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(exemplars);
		String fixedMissing = CollectionUtilities.prettyPrint(missing, col, col, true);
		CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
			.setMessage("Not in exemplars: {0}", new Object[]{fixedMissing});
		result.add(item);
		return this;
	}

}