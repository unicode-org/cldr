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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.ibm.icu.text.UnicodeSet;

public class LanguageTagParser {
	/**
	 * @return Returns the language.
	 */
	public String getLanguage() {
		return language;
	}
	/**
	 * @return Returns the script.
	 */
	public String getScript() {
		return script;
	}
	/**
	 * @return Returns the region.
	 */
	public String getRegion() {
		return region;
	}
	/**
	 * @return Returns the variants.
	 */
	public List getVariants() {
		return frozenVariants;
	}
	/**
	 * @return Returns the grandfathered flag
	 */
	public boolean isGrandfathered() {
		return grandfathered;
	}
	/**
	 * @return Returns the extensions.
	 */
	public Map getExtensions() {
		return frozenExtensions;
	}

	/**
	 * @return Returns the original, preparsed language tag
	 */
	public String getOriginal() {
		return original;
	}
  /**
   * @return Returns the language-script (or language) part of a tag.
   */
  public String getLanguageScript() {
    if (script.length() != 0) return language + "_" + script;
    return language;
  } 

  /**
	 * @param in Collection of language tag strings
	 * @return Returns each of the language-script tags in the collection.
	 */
	public static Set getLanguageScript(Collection in) {
		return getLanguageAndScript(in, null);
	}
	/**
	 * @param in Collection of language tag strings
	 * @return Returns each of the language-script tags in the collection.
	 */
	public static Set getLanguageAndScript(Collection in, Set output) {
		if (output == null) output = new TreeSet();
		LanguageTagParser lparser = new LanguageTagParser();
		for (Iterator it = in.iterator(); it.hasNext();) {
			output.add(lparser.set((String)it.next()).getLanguageScript());
		}
		return output;
	}

	// private fields
	
	private String original;
	private boolean grandfathered = false;
	private String language;
	private String script;
	private String region;
	private List variants = new ArrayList();
	private Map<String,String> extensions = new LinkedHashMap<String,String>();
	private LinkedHashMap<String,String> localeExtensions = new LinkedHashMap<String,String>();
	
	private List frozenVariants = Collections.unmodifiableList(variants);
	private Map<String,String> frozenExtensions = Collections.unmodifiableMap(extensions);
	
	private static final UnicodeSet ALPHA = new UnicodeSet("[a-zA-Z]");
	private static final UnicodeSet DIGIT = new UnicodeSet("[0-9]");
	private static final UnicodeSet ALPHANUM = new UnicodeSet("[0-9a-zA-Z]");
	private static final UnicodeSet X = new UnicodeSet("[xX]");
	private static final UnicodeSet ALPHA_MINUS_X = new UnicodeSet(ALPHA).removeAll(X);
	private static StandardCodes standardCodes = StandardCodes.make();
	private static final Set grandfatheredCodes = standardCodes.getAvailableCodes("grandfathered");
	private static final String separator = "-_"; // '-' alone for 3066bis language tags
	
	/**
	 * Parses out a language tag, setting a number of fields that can subsequently be retrieved.
	 * If a private-use field is found, it is returned as the last extension.<br>
	 * This only checks for well-formedness (syntax), not for validity (subtags in registry). For the latter, see isValid.
	 * @param languageTag
	 * @return
	 */
	public LanguageTagParser set(String languageTag) {
		if (languageTag.length() == 0) {
			throw new IllegalArgumentException("Language tag cannot be empty");
		}
		// clear everything out
		language = region = script = "";
		grandfathered = false;
		variants.clear();
		extensions.clear();
    localeExtensions.clear();
		original = languageTag;
		int localeExtensionsPosition = languageTag.indexOf('@');
		if (localeExtensionsPosition >= 0) {
			final String localeExtensionsString = languageTag.substring(localeExtensionsPosition + 1);
			for (String keyValue : localeExtensionsString.split(";")) {
			  final String[] keyValuePair = keyValue.split("\\=");
        final String key = keyValuePair[0];
        final String value = keyValuePair[1];
        if (keyValuePair.length != 2 || !ALPHANUM.containsAll(key) || !ALPHANUM.containsAll(value)) {
          throwError(keyValue, "Invalid key/value pair");
        }
			  localeExtensions.put(key, value);
			}
			
			languageTag = languageTag.substring(0, localeExtensionsPosition);
		}
		
		// first test for grandfathered
		if (grandfatheredCodes.contains(languageTag)) {
			language = languageTag;
			grandfathered = true;
			return this;
		}
		
		// each time we fetch a token, we check for length from 1..8, and all alphanum
		StringTokenizer st = new StringTokenizer(languageTag,"-_");
		String subtag = getSubtag(st);
		
		// check for private use (x-...) and return if so
		if (subtag.equalsIgnoreCase("x")) {
			getExtension(subtag, st, 1);
			return this;
		}
		
		// check that language subtag is valid
		if (!ALPHA.containsAll(subtag) || subtag.length() < 2) {
			throwError(subtag, "Invalid language subtag");
		}
		try { // The try block is to catch the out-of-tokens case. Easier than checking each time.
			language = subtag.toLowerCase(Locale.ENGLISH);
			subtag = getSubtag(st); // prepare for next
			
			// check for script, 4 letters
			if (subtag.length() == 4 && ALPHA.containsAll(subtag)) {
				script = subtag;
        script = script.substring(0,1).toUpperCase(Locale.ENGLISH) + script.substring(1).toLowerCase(Locale.ENGLISH);
				subtag = getSubtag(st); // prepare for next
			}
			
			// check for region, 2 letters or 3 digits
			if (subtag.length() == 2 && ALPHA.containsAll(subtag)
					|| subtag.length() == 3 && DIGIT.containsAll(subtag)) {
				region = subtag.toUpperCase(Locale.ENGLISH);
				subtag = getSubtag(st); // prepare for next
			}
			
			// get variants: length > 4 or len=4 & starts with digit
			while (subtag.length() > 4 || subtag.length() == 4 && DIGIT.contains(subtag.charAt(0))) {
				variants.add(subtag);
				subtag = getSubtag(st); // prepare for next
			}
			
			// get extensions: singleton '-' subtag (2-8 long)
			while (subtag.length() == 1 && ALPHA_MINUS_X.contains(subtag)) {
				subtag = getExtension(subtag, st, 2);
				if (subtag == null) return this; // done
			}
			
			if (subtag.equalsIgnoreCase("x")) {
				getExtension(subtag, st, 1);
				return this;
			}
			
			// if we make it to this point, then we have an error
			throwError(subtag, "Illegal subtag");
			
		} catch (NoSuchElementException e) {
			// this exception just means we ran out of tokens. That's ok, so we just return.
		}
		return this;
	}
	
	/**
	 * 
	 * @return true iff the language tag validates
	 */
	public boolean isValid() {
		if (grandfathered) return true; // don't need further checking, since we already did so when parsing
		if (!validates(language, "language")) return false;
		if (!validates(script, "script")) return false;
		if (!validates(region, "territory")) return false;
		for (Iterator it = variants.iterator(); it.hasNext();) {
			if (!validates((String)it.next(), "variant")) return false;
		}
//		for (Iterator it = extensions.iterator(); it.hasNext();) {
//			char ch = ((String)it.next()).charAt(0);
//			if (!X.contains(ch)) return false;
//		}
		return true; // passed the gauntlet
	}
	
	/**
	 * @param subtag
	 * @param type
	 * @return true if the subtag is empty, or if it is in the registry
	 */
	private boolean validates(String subtag, String type) {
		return subtag.length() == 0 || standardCodes.getAvailableCodes(type).contains(subtag);
	}
	/**
	 * Internal method
	 * @param minLength TODO
	 */
	private String getExtension(String subtag, StringTokenizer st, int minLength) {
    final String key = subtag;
    if (extensions.containsKey(key)) {
      throwError(subtag, "Can't have two extensions with the same key");
    }
		if (!st.hasMoreElements()) {
		  throwError(subtag, "Private Use / Extension requires subsequent subtag");
		}
		StringBuffer result = new StringBuffer();
		try {
			while (st.hasMoreElements()) {
				subtag = getSubtag(st);
				if (subtag.length() < minLength) {
				  return subtag;
				}
				if (result.length() != 0) {
				    result.append('-');
				}
				result.append(subtag);
			}
			return null;
		} finally {
			extensions.put(key, result.toString());
		}
	}
	
	/**
	 * Internal method
	 */
	private String getSubtag(StringTokenizer st) {
		String result = st.nextToken();
        if (result.length() < 1 || result.length() > 8) {
            throwError(result, "Illegal length (must be 1..8)");
        }
        if (!ALPHANUM.containsAll(result)) {
            throwError(result, "Illegal characters (" + new UnicodeSet().addAll(result).removeAll(ALPHANUM) + ")");
        }
		return result;
	}
	
	/**
	 * Internal method
	 */
	private void throwError(String subtag, String errorText) {
		throw new IllegalArgumentException(errorText + ": " + subtag + " in " + original);
	}

	/**
	 * @return Returns the localeExtensions.
	 */
	public Map<String,String> getLocaleExtensions() {
		return localeExtensions;
	}
  
  public LanguageTagParser setRegion(String region) {
    this.region = region;
    return this;
  }
  public LanguageTagParser setScript(String script) {
    this.script = script;
    return this;
  }
  
  public String toString() {
    String result = language;
    if (this.script.length() != 0) result += "_" + script;
    if (this.region.length() != 0) result += "_" + region;
    if (this.variants.size() != 0) {
      for (String variant : (Collection<String>) variants) {
        result += "_" + variant;
      }
    }
    return result;
  }
  
  public enum Fields {LANGUAGE, SCRIPT, REGION, VARIANTS};
  public static Set<Fields> LANGUAGE_SCRIPT = Collections.unmodifiableSet(EnumSet.of(Fields.LANGUAGE, Fields.SCRIPT));
  public static Set<Fields> LANGUAGE_REGION = Collections.unmodifiableSet(EnumSet.of(Fields.LANGUAGE, Fields.REGION));
  public static Set<Fields> LANGUAGE_SCRIPT_REGION = Collections.unmodifiableSet(EnumSet.of(Fields.LANGUAGE, Fields.SCRIPT, Fields.REGION));
  
  public String toString(Set<Fields> selection) {
    String result = language;
    if (selection.contains(Fields.SCRIPT) && script.length() != 0) result += "_" + script;
    if (selection.contains(Fields.REGION) && region.length() != 0) result += "_" + region;
    if (selection.contains(Fields.VARIANTS) && variants.size() != 0) {
      for (String variant : (Collection<String>) variants) {
        result += "_" + variant;
      }
    }
    return result;
  }

  /**
   * From en-US, returns en, then root, then null.
   * @param localeCode
   * @return
   */
  public static String getParent(String localeCode) {
    if (localeCode == null || localeCode.equals("root")) return null;
    int pos = localeCode.lastIndexOf('_');
    if (pos < 0) return "root";
    return localeCode.substring(0,pos);
  }
  public void setLanguage(String language) {
    if (language.contains("_")) {
      String oldScript = script;
      String oldRegion = region;
      List oldVariants = variants;
      set(language);
      if (script.length() == 0) {
        script = oldScript;
      }
      if (region.length() == 0) {
        region = oldRegion;
      }
      if (oldVariants.size() != 0) {
        variants.addAll(oldVariants);
      }

    } else {
      this.language = language;
    }
  }
  public LanguageTagParser setLocaleExtensions(LinkedHashMap<String, String> localeExtensions) {
    this.localeExtensions = localeExtensions;
    return this;
  }
}