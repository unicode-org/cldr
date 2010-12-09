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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	
	public static final List<String> CROSS_SCRIPT_LOCALES = Arrays.asList(
	        new String[] {
	                "az_Arab","az_Cyrl","en_Dsrt","en_Shaw","ha_Arab","ku_Latn","mn_Mong","pa_Arab","sh","shi_Tfng","sr_Latn","uz_Arab","uz_Latn","zh_Hant"
	        }
	);
	// TODO, Make this data driven instead of a hard-coded list.
	
    public static final Map<String,String> TOP_LEVEL_ALIAS_LOCALES = Builder.with(new HashMap<String,String>())
    .put("az_AZ", "az_Latn")
    .put("az_IR", "az_Arab")
    .put("ha_GH", "ha_Latn")
    .put("ha_NE", "ha_Latn")
    .put("ha_NG", "ha_Latn")
    .put("ha_SD", "ha_Arab")
    .put("kk_KZ", "kk_Cyrl")
    .put("ku_IQ", "ku_Arab")
    .put("ku_IR", "ku_Arab")
    .put("ku_SY", "ku_Latn")
    .put("ku_TR", "ku_Latn")
    .put("mn_CN", "mn_Mong")
    .put("mn_MN", "mn_Cyrl")
    .put("mo", "ro")
    .put("pa_IN", "pa_Guru")
    .put("pa_PK", "pa_Arab")
    .put("sh_BA", "sr_Latn")
    .put("sh_CS", "sr_Latn")
    .put("sh_YU", "sr_Latn")
    .put("shi_MA", "shi_Latn")
    .put("sr_BA", "sr_Cyrl")
    .put("sr_CS", "sr_Cyrl")
    .put("sr_Cyrl_CS", "sr_Cyrl")
    .put("sr_Cyrl_YU", "sr_Cyrl")
    .put("sr_Latn_CS", "sr_Latn")
    .put("sr_Latn_YU", "sr_Latn")
    .put("sr_ME", "sr_Latn")
    .put("sr_RS", "sr_Cyrl")
    .put("sr_YU", "sr_Cyrl")
    .put("tg_TJ", "tg_Cyrl")
    .put("tl_PH", "fil")
    .put("tzm_MA", "tzm_Latn")
    .put("ug_CN", "ug_Arab")
    .put("uz_AF", "uz_Arab")
    .put("uz_UZ", "uz_Cyrl")
    .put("wo_SN", "wo_Latn")
    .put("zh_CN", "zh_Hans")
    .put("zh_HK", "zh_Hant")
    .put("zh_MO", "zh_Hant")
    .put("zh_SG", "zh_Hans")
    .put("zh_TW", "zh_Hant")
    .freeze();
	
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
	        if (CROSS_SCRIPT_LOCALES.contains(localeName)) {
	          return "root";
	        }
	        String other = TOP_LEVEL_ALIAS_LOCALES.get(localeName);
	        if (other != null) {
	            return other;
	        }
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
      if (CROSS_SCRIPT_LOCALES.contains(localeName)) {
          return "root";
      }
      String other = TOP_LEVEL_ALIAS_LOCALES.get(localeName);
      if (other != null) {
          return other;
      }
      return localeName.substring(0,pos);
    }
    if (localeName.equals("root") || localeName.equals("supplementalData")) return null;
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