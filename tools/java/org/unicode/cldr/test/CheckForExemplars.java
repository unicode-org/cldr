package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.UnicodeSet;

public class CheckForExemplars extends CheckCLDR {
	private final UnicodeSet commonAndInherited = new UnicodeSet("[[:script=common:][:script=inherited:][:alphabetic=false:]]");
	static String[] EXEMPLAR_SKIPS = {"/hourFormat", "/exemplarCharacters", "/pattern", "/localizedPatternChars", "/segmentations"};

	UnicodeSet exemplars;
	boolean skip;
	
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrfile, List possibleErrors) {
		if (cldrfile == null) return this;
		skip = true;
		super.setCldrFileToCheck(cldrfile, possibleErrors);
		if (cldrfile.getLocaleID().equals("root")) {
			return this;
		}
		exemplars = cldrfile.getExemplarSet("");
		if (exemplars == null) {
			CheckStatus item = new CheckStatus().setType(CheckStatus.errorType)
			.setMessage("Failure to Initialize: {0} (need resolved locale)", new Object[]{this.getClass().getName()});
			possibleErrors.add(item);
			return this;
		}
		UnicodeSet temp = cldrfile.getExemplarSet("standard");
		if (temp != null) exemplars.addAll(temp);
		UnicodeSet auxiliary = cldrfile.getExemplarSet("auxiliary");
		if (auxiliary != null) exemplars.addAll(auxiliary);
		exemplars.addAll(commonAndInherited);
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
		CheckStatus item = new CheckStatus().setType(CheckStatus.errorType).setMessage("Not in exemplars: " + missing.toPattern(false));
		result.add(item);
		return this;
	}

}