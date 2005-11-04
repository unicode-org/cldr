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
 		return this;
	}

	private void checkExemplar(String v, List result) {
		if (v == null) return;
    	UnicodeSet exemplar1 = new UnicodeSet(v);
    	String fixedExemplar1 = CollectionUtilities.prettyPrint(exemplar1, col, col, true);
    	if (exemplar1.equals(fixedExemplar1)) return;
    	CheckStatus item = new CheckStatus().setType(CheckStatus.warningType).setMessage("Should be formatted as {0}", new Object[]{fixedExemplar1});
    	result.add(item);
	}
}