/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.FileUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.ULocale;


public class ShowLanguages {
	static CLDRFile english;
	static Collator col = Collator.getInstance(new ULocale("en"));
	static StandardCodes sc = StandardCodes.make();
	
	public static void main(String[] args) throws IOException {
		Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
		english = cldrFactory.make("en", false);
		printLanguageData(cldrFactory, "supplemental.html");
		//cldrFactory = Factory.make(Utility.COMMON_DIRECTORY + "../dropbox/extra2/", ".*");
		//printLanguageData(cldrFactory, "language_info2.txt");
		System.out.println("Done");
	}
	
	/**
	 * 
	 */
	private static Map anchors = new LinkedHashMap();
	
	private static void printLanguageData(Factory cldrFactory, String filename) throws IOException {
		LanguageInfo linfo = new LanguageInfo(cldrFactory);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("<div align='center'><table><tr><td>");
		linfo.print(pw, "Language \u2192 Territories", CLDRFile.LANGUAGE_NAME, CLDRFile.TERRITORY_NAME);
		pw.println("</td><td>");
		linfo.print(pw, "Territory \u2192 Language", CLDRFile.TERRITORY_NAME, CLDRFile.LANGUAGE_NAME);
		pw.println("</td></tr></table></div>");

		pw.println("<div align='center'><table><tr><td>");
		linfo.print(pw, "Language \u2192 Scripts", CLDRFile.LANGUAGE_NAME, CLDRFile.SCRIPT_NAME);
		pw.println("</td><td>");
		linfo.print(pw, "Script \u2192 Language", CLDRFile.SCRIPT_NAME, CLDRFile.LANGUAGE_NAME);
		pw.println("</td></tr></table></div>");

		pw.println("<div align='center'><table><tr><td>");
		linfo.printMissing(pw, "Territories Not Represented", CLDRFile.TERRITORY_NAME);
		pw.println("</td><td>");
		linfo.printMissing(pw, "Scripts Not Represented", CLDRFile.SCRIPT_NAME);
		pw.println("</td><td>");
		linfo.printMissing(pw, "Languages Not Represented", CLDRFile.LANGUAGE_NAME);
		pw.println("</td></tr></table></div>");
		
		linfo.printContains(pw);
		
		pw.close();
		String contents = "<ul>";
		for (Iterator it = anchors.keySet().iterator(); it.hasNext();) {
			String title = (String) it.next();
			String anchor = (String) anchors.get(title);
			contents += "<li><a href='#" + anchor + "'>" + title + "</a></li>";
		}
		contents += "</ul>";
		String[] replacements = {"%contents%", contents, "%data%", sw.toString()};
		PrintWriter pw2 = BagFormatter.openUTF8Writer(Utility.COMMON_DIRECTORY + "../diff/supplemental/", filename);
		FileUtilities.appendFile("org/unicode/cldr/tool/supplemental.html", "utf-8", pw2, replacements);
		pw2.close();
	}

	static class LanguageInfo {
		Map language_scripts = new TreeMap();
		Map language_territories = new TreeMap();
		Map territory_languages;
		Map script_languages;
		Map group_contains = new TreeMap();

		public LanguageInfo(Factory cldrFactory) {
			CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
			XPathParts parts = new XPathParts(new UTF16.StringComparator(), null);
			for (Iterator it = supp.keySet().iterator(); it.hasNext();) {
				String path = (String) it.next();
				if (path.indexOf("/territoryContainment") >= 0) {
					parts.set(supp.getFullXPath(path));
					Map attributes = parts.findAttributes("group");
					String type = (String) attributes.get("type");
					addTokens(type, (String) attributes.get("contains"), " ", group_contains);
					continue;
				}
				
				if (path.indexOf("/languageData") < 0) continue;
				parts.set(supp.getFullXPath(path));
				Map attributes = parts.findAttributes("language");
				String language = (String) attributes.get("type");
				String alt = (String) attributes.get("alt");
				addTokens(language, (String) attributes.get("scripts"), " ", language_scripts);
				// mark the territories
				if (alt == null) ; // nothing
				else if ("secondary".equals(alt)) language += "*";
				else language += "*" + alt;
				//<language type="af" scripts="Latn" territories="ZA"/>
				addTokens(language, (String) attributes.get("territories"), " ", language_territories);
			}
			territory_languages = getInverse(language_territories);
			script_languages = getInverse(language_scripts);
		}
		
		public void printContains(PrintWriter pw) {
			String title = "Territory Containment (UN M.49)";
			doTitle(pw, title);
			printContains2(pw, "<tr>", "001", 0);
			pw.println("</table></div>");
		}
		/**
		 * 
		 */
		private void doTitle(PrintWriter pw, String title) {
			String anchor = FileUtilities.anchorize(title);
			pw.println("<div align='center'><table><caption><a name='" + anchor + "'>" + title + "</a></caption>");
			anchors.put(title, anchor);
		}

		public void printContains2(PrintWriter pw, String lead, String start, int depth) {
			pw.println(lead + (depth == 3 ? "<td class='target'>" : "<td class='source'>")
					+ getName(CLDRFile.TERRITORY_NAME, start, false) + "</td></tr>");
			Collection contains = (Collection) group_contains.get(start);
			if (contains != null) {
				Collection contains2 = new TreeSet(territoryNameComparator);
				contains2.addAll(contains);
				boolean first = true;
				for (Iterator it = contains2.iterator(); it.hasNext();) {
					String item = (String)it.next();
					printContains2(pw, lead + "<td>&nbsp;</td>", item, depth+1);
				}
			}
		}
		
		/**
		 * 
		 */
		public void printMissing(PrintWriter pw, String title, int source) {
			Set missingItems = new HashSet();
			String type = null;
			if (source == CLDRFile.TERRITORY_NAME) {
				type = "territory";
				missingItems.addAll(sc.getAvailableCodes(type));
				missingItems.removeAll(territory_languages.keySet());
				missingItems.removeAll(group_contains.keySet());
				missingItems.remove("200"); // czechoslovakia
			} else if (source == CLDRFile.SCRIPT_NAME) {
				type = "script";
				missingItems.addAll(sc.getAvailableCodes(type));
				missingItems.removeAll(script_languages.keySet());
			} else if (source == CLDRFile.LANGUAGE_NAME) {
				type = "language";
				missingItems.addAll(sc.getAvailableCodes(type));
				missingItems.removeAll(language_scripts.keySet());
				missingItems.removeAll(language_territories.keySet());
			} else  {
				throw new IllegalArgumentException("Illegal code");
			}
			Set missingItemsNamed = new TreeSet(col);
			for (Iterator it = missingItems.iterator(); it.hasNext();) {
				String item = (String) it.next();
				List data = sc.getFullData(type, item);
				if (data.get(0).equals("PRIVATE USE")) continue;
				if (data.size() < 3) continue;
				if (!data.get(2).equals("")) continue;

				String itemName = getName(source, item, true);
				missingItemsNamed.add(itemName);
			}
			doTitle(pw, title);
			for (Iterator it = missingItemsNamed.iterator(); it.hasNext();) {
				pw.println("<tr><td class='target'>" + it.next() + "</td></tr>");
			}
			pw.println("</table>");
		}

		// source, eg english.TERRITORY_NAME
		// target, eg english.LANGUAGE_NAME
		public void print(PrintWriter pw, String title, int source, int target) {
			Map data = 
				source == CLDRFile.TERRITORY_NAME && target == CLDRFile.LANGUAGE_NAME ? territory_languages
				: source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.TERRITORY_NAME ? language_territories
				: source == CLDRFile.SCRIPT_NAME && target == CLDRFile.LANGUAGE_NAME ? script_languages
				: source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.SCRIPT_NAME ? language_scripts
				: null;
			// transform into names, and sort
			Map territory_languageNames = new TreeMap(col);
			for (Iterator it = data.keySet().iterator(); it.hasNext();) {
				String territory = (String) it.next();
				String territoryName = getName(source, territory, true);
				Set s = (Set) territory_languageNames.get(territoryName);
				if (s == null) territory_languageNames.put(territoryName, s = new TreeSet(col));
				for (Iterator it2 = ((Set) data.get(territory)).iterator(); it2.hasNext();) {
					String language = (String) it2.next();
					String languageName = getName(target, language, true);
					s.add(languageName);
				}
			}
			doTitle(pw, title);
			for (Iterator it = territory_languageNames.keySet().iterator(); it.hasNext();) {
				String territoryName = (String) it.next();
				pw.println("<tr><td class='source' colspan='2'>" + territoryName + "</td></tr>");
				Set s = (Set) territory_languageNames.get(territoryName);
				for (Iterator it2 = s.iterator(); it2.hasNext();) {
					String languageName = (String) it2.next();
					pw.println("<tr><td>&nbsp;</td><td class='target'>" + languageName + "</td></tr>");
				}
			}
			pw.println("</table><br>");
		}

		/**
		 * @param codeFirst TODO
		 * 
		 */
		private String getName(int type, String oldcode, boolean codeFirst) {
			int pos = oldcode.indexOf('*');
			String code = pos < 0 ? oldcode : oldcode.substring(0,pos);
			String ename = english.getName(type, code, false);
			return codeFirst ? "[" + oldcode +"]\t" + (ename == null ? code : ename)
					: (ename == null ? code : ename) + "\t[" + oldcode +"]";
		}
		
		Comparator territoryNameComparator = new Comparator () {
			public int compare(Object o1, Object o2) {
				return col.compare(getName(CLDRFile.TERRITORY_NAME, (String)o1, false), getName(CLDRFile.TERRITORY_NAME, (String)o2, false));
			}			
		};
	}

	/**
	 * 
	 */
	private static Map getInverse(Map language_territories) {
		// get inverse relation
		Map territory_languages = new TreeMap();
		for (Iterator it = language_territories.keySet().iterator(); it.hasNext();) {
			Object language = it.next();
			Set territories = (Set)language_territories.get(language);
			for (Iterator it2 = territories.iterator(); it2.hasNext();) {
				Object territory = it2.next();
				Set languages = (Set) territory_languages.get(territory);
				if (languages == null) territory_languages.put(territory, languages = new TreeSet(col));
				languages.add(language);
			}
		}
		return territory_languages;
		
	}

	/**
	 * @param value_delimiter TODO
	 * 
	 */
	private static void addTokens(String key, String values, String value_delimiter, Map key_value) {
		if (values != null) {
			Set s = (Set) key_value.get(key);
			if (s == null) key_value.put(key, s = new TreeSet(col));
			s.addAll(Arrays.asList(values.split(value_delimiter)));
		}
	}
}