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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.FileUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.TimeZone;
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
		
		linfo.printAliases(pw);
		linfo.printDeprecatedItems(pw);
		linfo.printCurrency(pw);

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
		
		linfo.printWindows_Tzid(pw);
		
		
		pw.close();
		String contents = "<ul>";
		for (Iterator it = anchors.keySet().iterator(); it.hasNext();) {
			String title = (String) it.next();
			String anchor = (String) anchors.get(title);
			contents += "<li><a href='#" + anchor + "'>" + title + "</a></li>";
		}
		contents += "</ul>";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm 'GMT'");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		String[] replacements = {"%date%", df.format(new Date()), "%contents%", contents, "%data%", sw.toString()};
		PrintWriter pw2 = BagFormatter.openUTF8Writer(Utility.COMMON_DIRECTORY + "../diff/supplemental/", filename);
		FileUtilities.appendFile("org/unicode/cldr/tool/supplemental.html", "utf-8", pw2, replacements);
		pw2.close();
	}

	static class LanguageInfo {
		Map language_scripts = new TreeMap();
		Map language_territories = new TreeMap();
		Map windows_tzid = new TreeMap();
		List deprecatedItems = new ArrayList();
		Map territory_languages;
		Map script_languages;
		Map group_contains = new TreeMap();
		Set aliases = new TreeSet(new ArrayComparator(new Comparator[]{new UTF16.StringComparator(), col}));
		Comparator col3 = new ArrayComparator(new Comparator[]{col, col, col});
		Map currency_fractions = new TreeMap(col);
		Map currency_territory = new TreeMap(col);
		Map territory_currency = new TreeMap(col);
		Set territoriesWithCurrencies = new TreeSet();
		Set currenciesWithTerritories = new TreeSet();

		String defaultDigits = null;

		public LanguageInfo(Factory cldrFactory) {
			CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
			XPathParts parts = new XPathParts(new UTF16.StringComparator(), null);
			for (Iterator it = supp.keySet().iterator(); it.hasNext();) {
				String path = (String) it.next();
				parts.set(supp.getFullXPath(path));
				if (path.indexOf("/territoryContainment") >= 0) {
					Map attributes = parts.findAttributes("group");
					String type = (String) attributes.get("type");
					addTokens(type, (String) attributes.get("contains"), " ", group_contains);
					continue;
				}
				
				if (path.indexOf("/alias") >= 0) {
					String element = parts.getElement(parts.size() - 1);
					Map attributes = parts.getAttributes(parts.size() - 1);
					String type = (String) attributes.get("type");
					if (!element.endsWith("Alias")) throw new IllegalArgumentException("Unexpected alias element: " + element);
					element = element.substring(0,element.length() - 5);
					String replacement = (String) attributes.get("replacement");
					if (element.equals("language")) {
						aliases.add(new String[] {type, getName(replacement, false)});
					} else {
						int typeCode = CLDRFile.typeNameToCode(element);
						aliases.add(new String[] {type, getName(typeCode, replacement, false)});
					}
					continue;
				}

				if (path.indexOf("/currencyData") >= 0) {
					if (path.indexOf("/fractions") >= 0) {
						//<info iso4217="ADP" digits="0" rounding="0"/>
						String element = parts.getElement(parts.size() - 1);
						if (!element.equals("info")) throw new IllegalArgumentException("Unexpected fractions element: " + element);
						Map attributes = parts.getAttributes(parts.size() - 1);
						String iso4217 = (String) attributes.get("iso4217");
						String digits = (String) attributes.get("digits");
						String rounding = (String) attributes.get("rounding");
						digits = digits + (rounding.equals("0") ? "" : " (" + rounding + ")");
						if (iso4217.equals("DEFAULT")) defaultDigits = digits;
						else currency_fractions.put(getName(CLDRFile.CURRENCY_NAME, iso4217, false), digits);
						continue;
					}
					//<region iso3166="AR">
					//	<currency iso4217="ARS" from="1992-01-01"/>
					if (path.indexOf("/region") >= 0) {
						Map attributes = parts.getAttributes(parts.size() - 2);
						String iso3166 = (String)attributes.get("iso3166");
						attributes = parts.getAttributes(parts.size() - 1);						
						String iso4217 = (String) attributes.get("iso4217");
						String to = (String) attributes.get("to");
						if (to == null) to = "\u221E";
						String from = (String) attributes.get("from");
						if (from == null) from = "-\u221E";
						String countryName = getName(CLDRFile.TERRITORY_NAME, iso3166, false);
						String currencyName = getName(CLDRFile.CURRENCY_NAME, iso4217, false);
						Set info = (Set) territory_currency.get(countryName);
						if (info == null) territory_currency.put(countryName, info = new TreeSet(col3));
						info.add(new String[] {from, to, currencyName});
						info = (Set) currency_territory.get(currencyName);
						if (info == null) currency_territory.put(currencyName, info = new TreeSet(col));
						info.add(countryName);
						territoriesWithCurrencies.add(iso3166);
						currenciesWithTerritories.add(iso4217);
						continue;
					}
				}				
				
				if (path.indexOf("/languageData") >= 0) {
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
					continue;
				}
				
				if (path.indexOf("/mapTimezones") >= 0) {
					Map attributes = parts.findAttributes("mapZone");
					String tzid = (String) attributes.get("type");
					String other = (String) attributes.get("other");
					windows_tzid.put(other, tzid);
					continue;
				}
				if (path.indexOf("/deprecatedItems") >= 0) {
					deprecatedItems.add(parts.findAttributes("deprecatedItems"));
					continue;
				}
				if (path.indexOf("/generation") >= 0 || path.indexOf("/version") >= 0) continue;
				System.out.println("Unknown Element: " + path);
			}
			territory_languages = getInverse(language_territories);
			script_languages = getInverse(language_scripts);
		}
		
		/**
		 * 
		 */
		public void printCurrency(PrintWriter pw) {
			doTitle(pw, "Territory \u2192 Currency");
			pw.println("<tr><th class='source'>Territory</th><th class='target'>From</th><th class='target'>To</th><th class='target'>Currency</th></tr>");
			for (Iterator it = territory_currency.keySet().iterator(); it.hasNext();) {
				String territory = (String)it.next();
				Set info = (Set)territory_currency.get(territory);
				pw.println("<tr><td class='source' rowSpan='" + info.size() + "'>" + territory + "</td>");
				boolean first = true;
				for (Iterator it2 = info.iterator(); it2.hasNext();) {
					String[] items = (String[]) it2.next();
					if (first) first = false;
					else pw.println("<tr>");
					pw.println("<td class='target'>" + items[0]
							+ "</td><td class='target'>" + items[1]
							+ "</td><td class='target'>" + items[2]
							+ "</td></tr>");
				}
			}
			pw.println("</table></div>");

			doTitle(pw, "Currency Format Info");
			pw.println("<tr><th class='source'>Currency</th><th class='target'>Digits</th><th class='target'>Countries</th></tr>");
			Set currencyList = new TreeSet(col);
			currencyList.addAll(currency_fractions.keySet());
			currencyList.addAll(currency_territory.keySet());

			for (Iterator it = currencyList.iterator(); it.hasNext();) {
				String currency = (String)it.next();
				String fractions = (String)currency_fractions.get(currency);
				if (fractions == null) fractions = defaultDigits;
				Set territories = (Set)currency_territory.get(currency);
				pw.print("<tr><td class='source'>" + currency + "</td><td class='target'>" + fractions + "</td><td class='target'>");
				if (territories != null) {
					boolean first = true;
					for (Iterator it2 = territories.iterator(); it2.hasNext();) {
						if (first) first = false;
						else pw.print(", ");
						pw.print(it2.next());
					}
				}
				pw.println("</td></tr>");
			}
			pw.println("</table></div>");
			
			if (false) {
				doTitle(pw, "Territories Versus Currencies");
				pw.println("<tr><th>Territories Without Currencies</th><th>Currencies Without Territories</th></tr>");
				pw.println("<tr><td class='target'>");
				Set territoriesWithoutCurrencies = new TreeSet();
				territoriesWithoutCurrencies.addAll(sc.getGoodAvailableCodes("territory"));
				territoriesWithoutCurrencies.removeAll(territoriesWithCurrencies);
				territoriesWithoutCurrencies.removeAll(group_contains.keySet());
				boolean first = true;
				for (Iterator it = territoriesWithoutCurrencies.iterator(); it.hasNext();) {
					if (first) first = false;
					else pw.print(", ");
					pw.print(english.getName(CLDRFile.TERRITORY_NAME, it.next().toString(), false));				
				}
				pw.println("</td><td class='target'>");
				Set currenciesWithoutTerritories = new TreeSet();
				currenciesWithoutTerritories.addAll(sc.getGoodAvailableCodes("currency"));
				currenciesWithoutTerritories.removeAll(currenciesWithTerritories);
				first = true;
				for (Iterator it = currenciesWithoutTerritories.iterator(); it.hasNext();) {
					if (first) first = false;
					else pw.print(", ");
					pw.print(english.getName(CLDRFile.CURRENCY_NAME, it.next().toString(), false));				
				}
				pw.println("</td></tr>");
				pw.println("</table></div>");
			}
		}

		/**
		 * 
		 */
		public void printAliases(PrintWriter pw) {
			doTitle(pw, "Aliases");
			for (Iterator it = aliases.iterator(); it.hasNext();) {
				String[] items = (String[])it.next();
				pw.println("<tr><td class='source'>" + items[0] + "</td><td class='target'>" + items[1] + "</td></tr>");
			}
			pw.println("</table></div>");
		}
		
		//deprecatedItems
		public void printDeprecatedItems(PrintWriter pw) {
			doTitle(pw, "Deprecated Items");
			pw.print("<tr><td class='z0'><b>Type</b></td><td class='z1'><b>Elements</b></td><td class='z2'><b>Attributes</b></td><td class='z4'><b>Values</b></td>");
			for (Iterator it = deprecatedItems.iterator(); it.hasNext();) {
				Map source = (Map)it.next();
				Object item;
				pw.print("<tr>");
				pw.print("<td class='z0'>" + ((item = source.get("type")) != null ? item : "<i>any</i>")  + "</td>");
				pw.print("<td class='z1'>" + ((item = source.get("elements")) != null ? item : "<i>any</i>")  + "</td>");
				pw.print("<td class='z2'>" + ((item = source.get("attributes")) != null ? item : "<i>any</i>") + "</td>");
				pw.print("<td class='z4'>" + ((item = source.get("values")) != null ? item : "<i>any</i>") + "</td>");
				pw.print("</tr>");
			}
			pw.println("</table></div>");
		}
		
		public void printWindows_Tzid(PrintWriter pw) {
			doTitle(pw, "Windows \u2192 Tzid");
			for (Iterator it = windows_tzid.keySet().iterator(); it.hasNext();) {
				String source = (String)it.next();
				String target = (String)windows_tzid.get(source);
				pw.println("<tr><td class='source'>" + source + "</td><td class='target'>" + target + "</td></tr>");
			}
			pw.println("</table></div>");
		}
		
		//<info iso4217="ADP" digits="0" rounding="0"/>

		public void printContains(PrintWriter pw) {
			String title = "Territory Containment (UN M.49)";
			doTitle(pw, title);
			printContains2(pw, "<tr>", "001", 0, true);
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

		public void printContains2(PrintWriter pw, String lead, String start, int depth, boolean isFirst) {
			String name = depth == 4 ? start : getName(CLDRFile.TERRITORY_NAME, start, false);
			if (!isFirst) pw.print(lead);
			pw.print("<td class='z" + depth + "'>" + name + "</td>"); // colSpan='" + (5 - depth) + "' 
			if (depth == 4) pw.println("</tr>");
			Collection contains = (Collection) group_contains.get(start);
			if (contains == null) {
				contains = (Collection) sc.getCountryToZoneSet().get(start);
				if (contains == null && depth == 3 && start.compareTo("A") >= 0) {
					contains = new TreeSet();
					contains.add("<font color='red'>MISSING TZID</font>");
				}
			}
			if (contains != null) {
				Collection contains2 = new TreeSet(territoryNameComparator);
				contains2.addAll(contains);
				boolean first = true;
				for (Iterator it = contains2.iterator(); it.hasNext();) {
					String item = (String)it.next();
					printContains2(pw, lead + "<td>&nbsp;</td>", item, depth+1, first);
					first = false;
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
		
		private String getName(String locale, boolean codeFirst) {
			String ename = english.getName(locale, false);
			return codeFirst ? "[" + locale +"]\t" + (ename == null ? locale : ename)
					: (ename == null ? locale : ename) + "\t[" + locale +"]";
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