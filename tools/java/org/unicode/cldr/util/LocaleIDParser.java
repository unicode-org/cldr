/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;

public class LocaleIDParser {
	/**
	 * @return Returns the language.
	 */
	public String getLanguage() {
		return language;
	}
	/**
	 * @return Returns the language.
	 */
	public String getLanguageScript() {
		if (script.length() != 0) return language + "_" + script;
		return language;
	}
	
	public static Set getLanguageScript(Collection in) {
		return getLanguageScript(in, null);
	}

	public static Set getLanguageScript(Collection in, Set output) {
		if (output == null) output = new TreeSet();
		LocaleIDParser lparser = new LocaleIDParser();
		for (Iterator it = in.iterator(); it.hasNext();) {
			output.add(lparser.set((String)it.next()).getLanguageScript());
		}
		return output;
	}
	/**
	 * @return Returns the region.
	 */
	public String getRegion() {
		return region;
	}
	/**
	 * @return Returns the script.
	 */
	public String getScript() {
		return script;
	}
	/**
	 * @return Returns the variants.
	 */
	public String[] getVariants() {
		return (String[]) variants.clone();
	}
	// TODO, update to RFC3066
	// http://www.inter-locale.com/ID/draft-phillips-langtags-08.html
	private String language;
	private String script;
	private String region;
	private String[] variants;
	
	static final UnicodeSet letters = new UnicodeSet("[a-zA-Z]");
	static final UnicodeSet digits = new UnicodeSet("[0-9]");
	
	public LocaleIDParser set(String localeID) {
		region = script = "";
		variants = new String[0];

		String[] pieces = new String[100]; // fix limitation later
		Utility.split(localeID, '_', pieces);
		int i = 0;
		language = pieces[i++];
		if (i >= pieces.length) return this;
		if (pieces[i].length() == 4) {
			script = pieces[i++];
			if (i >= pieces.length) return this;
		}
		if (pieces[i].length() == 2 && letters.containsAll(pieces[i])
				|| pieces[i].length() == 3 && digits.containsAll(pieces[i])) {
			region = pieces[i++];
			if (i >= pieces.length) return this;
		}
		List al = new ArrayList();
		while (i < pieces.length && pieces[i].length() > 0) {
			al.add(pieces[i++]);
		}
		variants = new String[al.size()];
		al.toArray(variants);
		return this;
	}
	/**
	 * Utility to get the parent of a locale. If the input is "root", then the output is null.
	 * @param localeName
	 * @return
	 */
	public static String getParent(String localeName) {
	    int pos = localeName.lastIndexOf('_');
	    if (pos >= 0) {
	        return localeName.substring(0,pos);
	    }
	    if (localeName.equals("root") || localeName.equals(CLDRFile.SUPPLEMENTAL_NAME)) return null;
	    return "root";
	}
}