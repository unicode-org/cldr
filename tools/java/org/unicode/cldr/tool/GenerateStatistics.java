/*
 * Created on May 19, 2005
 * Copyright (C) 2004-2005, Unicode, Inc., International Business Machines Corporation, and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Log;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;


/**
 * @throws IOException
 * 
 */
class GenerateStatistics {
	
	public static void generateSize(String sourceDir, String logDir, String tzadir, boolean transliterate) throws IOException {
		
		// HACK around lack of Armenian, Ethiopic				
		registerTransliteratorFromFile(tzadir, "Latin-Armenian");
		registerTransliteratorFromFile(tzadir, "Latin-Ethiopic");
		registerTransliteratorFromFile(tzadir, "Cyrillic-Latin");
		registerTransliteratorFromFile(tzadir, "Arabic-Latin");		


		PrintWriter logHtml = BagFormatter.openUTF8Writer(
				sourceDir,
				"log.html");
		String dir = logDir
				+ "main" + File.separator;
		DraftChecker dc = new DraftChecker(dir);
		Set filenames = GenerateCldrTests.getMatchingXMLFiles(dir, ".*");
		Collator col = Collator.getInstance(ULocale.ENGLISH);
		Set languages = new TreeSet(col), countries = new TreeSet(col), draftLanguages = new TreeSet(
				col), draftCountries = new TreeSet(col);
		Set nativeLanguages = new TreeSet(), nativeCountries = new TreeSet(), draftNativeLanguages = new TreeSet(), draftNativeCountries = new TreeSet();
		int localeCount = 0;
		int draftLocaleCount = 0;
		for (Iterator it = filenames.iterator(); it.hasNext();) {
			String localeName = (String) it.next();
			if (localeName.equals("root"))
				continue; // skip root
			boolean draft = dc.isDraft(localeName);
			if (draft) {
				draftLocaleCount++;
				addCounts(localeName, true, draftLanguages,
						draftCountries, draftNativeLanguages,
						draftNativeCountries);
			} else {
				localeCount++;
				addCounts(localeName, false, languages,
						countries, nativeLanguages, nativeCountries);
			}
			if (false)
				Log.logln(draft + ", " + localeCount + ", "
						+ languages.size() + ", " + countries.size() + ", "
						+ draftLocaleCount + ", " + draftLanguages.size()
						+ ", " + draftCountries.size());
		}
		draftLanguages.removeAll(languages);
		for (Iterator it = nativeLanguages.iterator(); it.hasNext();) {
			draftNativeLanguages.remove(it.next());
		}
		logHtml.println("<html><head>");
		logHtml
				.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
		logHtml.println("</head><body>");
		logHtml.println("<p><b>Locales (" + localeCount + "):</b>");
		logHtml.println("<p><b>Languages (" + languages.size() + "):</b>");
		logHtml.println(showSet(nativeLanguages, transliterate, true));
		logHtml.println("<p><b>Countries (" + countries.size() + "):</b>");
		logHtml.println(showSet(nativeCountries, transliterate, false));
		logHtml.println("<p><b>Draft locales (" + draftLocaleCount + "):</b>");
		logHtml.println("<p><b>Draft languages (" + draftLanguages.size()
				+ "):</b>");
		logHtml.println(showSet(draftNativeLanguages, transliterate, true));
		logHtml.println("<p><b>Draft countries (" + draftCountries.size()
				+ "):</b>");
		logHtml.println(showSet(draftNativeCountries, transliterate, false));
		logHtml.println("</body></html>");
		logHtml.close();
	}

    static final UnicodeSet NON_LATIN = new UnicodeSet("[^[:latin:][:common:][:inherited:]]");

	/**
	 * @param nativeCountries
	 * @param transliterate
	 *            TODO
	 * @param isLanguage
	 *            TODO
	 */
	private static String showSet(Set nativeCountries, boolean transliterate,
			boolean isLanguage) {
		UnicodeSet BIDI_R = new UnicodeSet(
				"[[:Bidi_Class=R:][:Bidi_Class=AL:]]");
		StringBuffer result = new StringBuffer();
		Map sb = new TreeMap(LanguageList.col);
		// collect multiples by English name
		for (Iterator it = nativeCountries.iterator(); it.hasNext();) {
			LanguageList llist = (LanguageList) it.next();
			Set s = (Set) sb.get(llist.getEnglishName());
			if (s == null)
				sb.put(llist.getEnglishName(), s = new TreeSet());
			s.add(llist);
		}
		for (Iterator it = sb.keySet().iterator(); it.hasNext();) {
			String englishName = (String) it.next();
			Set s = (Set) sb.get(englishName);
			if (result.length() != 0) {
				result.append(", ");
			}
			String qualifiers = "", title = "";
			int count = 0;
			boolean needQualifier = s.size() != 1;

			for (Iterator it2 = s.iterator(); it2.hasNext();) {
				count += 1;
				LanguageList llist = (LanguageList) it2.next();
				String localName = llist.getLocalName();
				String locale = llist.getLocale();

				// see if we need qualifier
				String lang = locale, country = "";
				if (locale.length() > 3
						&& locale.charAt(locale.length() - 3) == '_') {
					lang = locale.substring(0, locale.length() - 3);
					country = locale.substring(locale.length() - 2);
				}

				// fix
				if (BIDI_R.containsSome(localName))
					localName = '\u200E' + localName + '\u200E';

				if (qualifiers.length() != 0)
					qualifiers += "; ";
				qualifiers += lang;

				if (title.length() != 0)
					title += "; ";
/*				if (isLanguage) {
					title += lang;
				} else {
					title += country;
				}
*/				if (!localName.equalsIgnoreCase(englishName)) {
					needQualifier = true;
					qualifiers += ", " + localName;

					if (transliterate && NON_LATIN.containsSome(localName)
							&& !lang.equals("ja")) {
						String transName = fixedTitleCase(ULocale.ENGLISH,
								toLatin.transliterate(localName));
						if (NON_LATIN.containsSome(transName)) {
							Log.logln("Can't transliterate " + localName
									+ ": " + transName);
						} else {
							title += ", " + transName;
						}
					}
				}
			}
			String before = "", after = "";
			if (title.length() != 0) {
				before = "<span title=\'"
						+ BagFormatter.toHTML.transliterate(title) + "'>";
				after = "</span>";
			}
			if (!needQualifier)
				qualifiers = "";
			else
				qualifiers = " (" + qualifiers + ")";

			// fix
			if (englishName.endsWith(", China")) {
				englishName = englishName.substring(0, englishName.length()
						- ", China".length())
						+ " China";
			}

			result.append(before)
					.append(
							BagFormatter.toHTML.transliterate(englishName
									+ qualifiers)).append(after);
		}
		return result.toString();
	}
	/**
	 * @param localeName
	 * @param isDraft TODO
	 * @param draftLanguages
	 * @param draftCountries
	 * @param draftNativeLanguages
	 * @param draftNativeCountries
	 */
	private static void addCounts(String localeName, boolean isDraft, Set draftLanguages, Set draftCountries,
			Set draftNativeLanguages, Set draftNativeCountries) {
		ULocale uloc = new ULocale(localeName);
		String lang = localeName, country = "";
		if (localeName.length() > 3 && localeName.charAt(localeName.length() - 3) == '_') {
			lang = localeName.substring(0, localeName.length() - 3);
			country = localeName.substring(localeName.length() - 2);
		}
		
		// dump aliases
		if ((country.equals("TW") || country.equals("HK") || country.equals("MO")) && lang.equals("zh")) return;
		if (lang.equals("zh_Hans") || lang.equals("sr_Cyrl") || lang.equals("sh")) return;
		
		String nativeName, englishName;
		draftLanguages.add(lang);
		nativeName = uloc.getDisplayLanguage(uloc);
		englishName = uloc.getDisplayLanguage(ULocale.ENGLISH);
		if (!lang.equals("en") && nativeName.equals(englishName)) {
			Log.logln((isDraft ? "D" : "") +"\tWarning: in " + localeName + ", display name for " + lang + " equals English: "  + nativeName);
		}
		
			draftNativeLanguages.add(new LanguageList(lang, englishName, fixedTitleCase(uloc, nativeName)));
		
		if (!country.equals("")) {
			draftCountries.add(country);
			nativeName = getFixedDisplayCountry(uloc, uloc);
			englishName = getFixedDisplayCountry(uloc, ULocale.ENGLISH);
			if (!lang.equals("en") && nativeName.equals(englishName)) {
				Log.logln((isDraft ? "D" : "") + "\tWarning: in " + localeName + ", display name for " + country + " equals English: "  + nativeName);
			}
			draftNativeCountries.add(new LanguageList(uloc.toString(), englishName, fixedTitleCase(uloc, nativeName)));
		}
	}
	
	private static class LanguageList implements Comparable {
		Object [] contents;
		static Collator col = Collator.getInstance(ULocale.ENGLISH);
		static Comparator comp = new ArrayComparator(new Collator[]{col, col, null});
		LanguageList(String locale, String englishName, String localName) {
			contents = new Object[] {englishName, locale, localName};
		}
		public int compareTo(Object o) {
			return comp.compare(contents,((LanguageList)o).contents);
		}
		String getLocale() { return (String)contents[1]; }
		String getEnglishName() { return (String)contents[0]; }
		String getLocalName() { return (String)contents[2]; }
	}
	
	static String fixedTitleCase(ULocale uloc, String in) {
		String result = UCharacter.toTitleCase(uloc, in, null);
		result = GenerateCldrTests.replace(result, "U.s.", "U.S.");
		result = GenerateCldrTests.replace(result, "S.a.r.", "S.A.R.");
		return result;
	}
	/*
	static void addMapSet(Map m, Object key, Object value, Comparator com) {
		Set valueSet = (Set) m.get(key);
		if (valueSet == null) {
			valueSet = new TreeSet(com);
			m.put(key, valueSet);
		}
		valueSet.add(value);
	}
	*/
	
	

	/**
	 * @param uloc
	 * @return
	 */
	private static String getFixedDisplayCountry(ULocale uloc, ULocale forLanguage) {
		String name = uloc.getDisplayCountry(forLanguage);
		Object trial = fixCountryNames.get(name);
		if (trial != null) {
			return (String)trial;
		}
		return name;
	}
	
	static Map fixCountryNames = new HashMap(); 
	static {
		fixCountryNames.put("\u0408\u0443\u0433\u043E\u0441\u043B\u0430\u0432\u0438\u0458\u0430", "\u0421\u0440\u0431\u0438\u0458\u0430 \u0438 \u0426\u0440\u043D\u0430 \u0413\u043E\u0440\u0430");
		fixCountryNames.put("Jugoslavija", "Srbija i Crna Gora");
		fixCountryNames.put("Yugoslavia", "Serbia and Montenegro");
	}
	public static final Transliterator toLatin = Transliterator.getInstance("any-latin");
	
	static void registerTransliteratorFromFile(String dir, String id) {
		try {
			String filename = id.replace('-', '_');
			BufferedReader br = BagFormatter.openUTF8Reader(dir, filename + ".txt");
			StringBuffer buffer = new StringBuffer();
			while (true) {
				String line = br.readLine();
				if (line == null) break;
				if (line.length() > 0 && line.charAt(0) == '\uFEFF') line = line.substring(1);
				buffer.append(line).append("\r\n");
			}
			br.close();
			String rules = buffer.toString();
			Transliterator t;
			int pos = id.indexOf('-');
			String rid;
			if (pos < 0) {
				rid = id + "-Any";
				id = "Any-" + id;
			} else {
				rid = id.substring(pos+1) + "-" + id.substring(0, pos);
			}
			Transliterator.unregister(id);
			t = Transliterator.createFromRules(id, rules, Transliterator.FORWARD);
			Transliterator.registerInstance(t);

			/*String test = "\u049A\u0430\u0437\u0430\u049B";
			System.out.println(t.transliterate(test));
			t = Transliterator.getInstance(id);
			System.out.println(t.transliterate(test));
			*/

			Transliterator.unregister(rid);
			t = Transliterator.createFromRules(rid, rules, Transliterator.REVERSE);
			Transliterator.registerInstance(t);
			System.out.println("Registered new Transliterator: " + id + ", " + rid);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Can't open " + dir + ", " + id);
		}
	}


	public static class DraftChecker {
		String dir;
		Map cache = new HashMap();
		Object TRUE = new Object();
		Object FALSE = new Object();
		public DraftChecker(String dir) {
			this.dir = dir;
		}
		
		public boolean isDraft(String localeName) {
			Object check = cache.get(localeName);
			if (check != null) {
				return check == TRUE;
			}
			BufferedReader pw = null;
			boolean result = true;
			try {
				pw = BagFormatter.openUTF8Reader(dir, localeName + ".xml");
				while (true) {
					String line = pw.readLine();
					assert (line != null); // should never get here
					if (line.indexOf("<ldml") >= 0) {
						if (line.indexOf("draft") >= 0) {
							check = TRUE;
						} else {
							check = FALSE;
						}
						break;
					}
				}
				pw.close();
			} catch (IOException e) {				
				e.printStackTrace();
				throw new IllegalArgumentException("Failure on " + localeName + ": " + dir + localeName + ".xml");
			}
			cache.put(localeName, check);
			return check == TRUE;
		}
	}

}