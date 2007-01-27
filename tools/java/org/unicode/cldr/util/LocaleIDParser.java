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
import java.util.EnumSet;
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
	 */
	public static String getParent(String localeName) {
	    int pos = localeName.lastIndexOf('_');
	    if (pos >= 0) {
	        return localeName.substring(0,pos);
	    }
	    if (localeName.equals("root") || localeName.equals(CLDRFile.SUPPLEMENTAL_NAME)) return null;
	    return "root";
	}
	public LocaleIDParser setLanguage(String language) {
		this.language = language;
		return this;
	}
	public LocaleIDParser setRegion(String region) {
		this.region = region;
		return this;
	}
	public LocaleIDParser setScript(String script) {
		this.script = script;
		return this;
	}
	public LocaleIDParser setVariants(String[] variants) {
		this.variants = (String[]) variants.clone();
		return this;
	}
  
  public enum Level {
    Language, Script, Region, Variants, Other
  }
  /**
   * Returns an int mask indicating the level
   * @return (2 if script is present) + (4 if region is present) + (8 if region is present)
   */
  public Set<Level> getLevels() {
    EnumSet<Level> result = EnumSet.of(Level.Language);
    if (getScript().length() != 0) result.add(Level.Script);
    if (getRegion().length() != 0) result.add(Level.Region);
    if (getVariants().length != 0) result.add(Level.Variants);
    return result;
  }
  
  public String getParent() {
    String localeName = toString();
    int pos = localeName.lastIndexOf('_');
    if (pos >= 0) {
      return localeName.substring(0,pos);
    }
    if (localeName.equals("root")) return null;
    return "root";
  }
  
  public Set<String> getSiblings(Set<String> set) {
    Set<Level> myLevel = getLevels();
    String localeID = toString();
    String parentID = getParent(localeID);
    
    String prefix = parentID.equals("root") ? "" : parentID + "_";
    Set<String> siblings = new TreeSet<String>();
    for (String id : set) {
      if (id.startsWith(prefix) && set(id).getLevels().equals(myLevel)) {
        siblings.add(id);
      }
    }
    set(localeID); // leave in known state
    return siblings;
  }

	public String toString() {
		StringBuffer result = new StringBuffer(language);
		if (script.length() != 0) result.append('_').append(script);
		if (region.length() != 0) result.append('_').append(region);
		if (variants != null) {
			for (int i = 0; i < variants.length; ++i) {
				result.append('_').append(variants[i]);
			}
		}
		return result.toString();
	}
}