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
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.Normalizer;
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
		printLanguageData(cldrFactory, "index.html");
		//cldrFactory = Factory.make(Utility.COMMON_DIRECTORY + "../dropbox/extra2/", ".*");
		//printLanguageData(cldrFactory, "language_info2.txt");
		System.out.println("Done");
	}
	
	/**
	 * 
	 */
	private static List anchors = new ArrayList();
	
	private static void printLanguageData(Factory cldrFactory, String filename) throws IOException {
		LanguageInfo linfo = new LanguageInfo(cldrFactory);
        linfo.showTerritoryInfo();
        
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		//linfo.printDeprecatedItems(pw);
		linfo.printCurrency(pw);

        PrintWriter pw1 = new PrintWriter(new FormattedFileWriter(pw, "Languages and Territories"));
        pw1.println("<tr><th>Language \u2192 Territories");
        pw1.println("</th><th>Territory \u2192 Language");
        pw1.println("</th><th>Territories Not Represented");
        pw1.println("</th><th>Languages Not Represented");
        pw1.println("</th></tr>");
        
		pw1.println("<tr><td>");
		linfo.print(pw1, CLDRFile.LANGUAGE_NAME, CLDRFile.TERRITORY_NAME);
		pw1.println("</td><td>");
		linfo.print(pw1, CLDRFile.TERRITORY_NAME, CLDRFile.LANGUAGE_NAME);
        pw1.println("</td><td>");
        linfo.printMissing(pw1, CLDRFile.TERRITORY_NAME, CLDRFile.TERRITORY_NAME);
        pw1.println("</td><td>");
        linfo.printMissing(pw1, CLDRFile.LANGUAGE_NAME, CLDRFile.TERRITORY_NAME);
        pw1.println("</td></tr>");

        pw1.close();

        pw1 = new PrintWriter(new FormattedFileWriter(pw, "Languages and Scripts"));
        
        pw1.println("<tr><th>Language \u2192 Scripts");
        pw1.println("</th><th>Script  \u2192 Language");
        pw1.println("</th><th>Territories Not Represented");
        pw1.println("</th><th>Languages Not Represented");
        pw1.println("</th></tr>");

		pw1.println("<tr><td >");
		linfo.print(pw1, CLDRFile.LANGUAGE_NAME, CLDRFile.SCRIPT_NAME);
		pw1.println("</td><td>");
		linfo.print(pw1, CLDRFile.SCRIPT_NAME, CLDRFile.LANGUAGE_NAME);
        pw1.println("</td><td>");
		linfo.printMissing(pw1, CLDRFile.SCRIPT_NAME, CLDRFile.SCRIPT_NAME);
		pw1.println("</td><td>");
		linfo.printMissing(pw1, CLDRFile.LANGUAGE_NAME, CLDRFile.SCRIPT_NAME);
		pw1.println("</td></tr>");
        pw1.close();
		
		linfo.showCorrespondances();
        
        linfo.showCalendarData(pw);
		
		linfo.printContains(pw);
        
		linfo.printWindows_Tzid(pw);
		
        linfo.printCharacters(pw);

        linfo.printAliases(pw);
        
		pw.close();
        
		String contents = "<ul>";
		for (Iterator it = anchors.iterator(); it.hasNext();) {
			String item = (String) it.next();
			contents += "<li>" + item + "</li>";
		}
		contents += "</ul>";
		String[] replacements = {"%date%", df.format(new Date()), "%contents%", contents, "%data%", sw.toString()};
		PrintWriter pw2 = BagFormatter.openUTF8Writer(Utility.COMMON_DIRECTORY + "../diff/supplemental/", filename);
		FileUtilities.appendFile("org/unicode/cldr/tool/supplemental.html", "utf-8", pw2, replacements);
		pw2.close();
	}
    
    static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm 'GMT'");
    static {
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    static class FormattedFileWriter extends java.io.Writer {
        private StringWriter out = new StringWriter();
        private String title;
        private String filename;
        public FormattedFileWriter(PrintWriter indexFile, String title) throws IOException {
            String anchor = FileUtilities.anchorize(title);
            filename =  anchor + ".html";
            this.title = title;
            anchors.add("<a name='" + FileUtilities.anchorize(getTitle()) + "' href='" + getFilename() + "'>" + getTitle() + "</a></caption>");
            out.write("<div align='center'><table>");
        }
        public  String getFilename() {
            return filename;
        }
        public  String getTitle() {
            return title;
        }
        public void close() throws IOException {
            out.write("</table></div>");
            PrintWriter pw2 = BagFormatter.openUTF8Writer(Utility.COMMON_DIRECTORY + "../diff/supplemental/", filename);
            String[] replacements = {"%header%", "", "%title%", title, "%version%", "1.4", "%date%", df.format(new Date()), "%body%", out.toString()};
            FileUtilities.appendFile("org/unicode/cldr/tool/chart-template.html", "utf-8", pw2, replacements);
            pw2.close();
        }
        public void write(char[] cbuf, int off, int len) throws IOException {
            out.write(cbuf, off, len);
        }
        public void flush() throws IOException {
            out.flush();
        }
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
        Map territoryData = new TreeMap();
        Set territoryTypes = new TreeSet();
        Map charSubstitutions = new TreeMap(col);

		String defaultDigits = null;

		public LanguageInfo(Factory cldrFactory) {
			CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
			XPathParts parts = new XPathParts(new UTF16.StringComparator(), null);
			for (Iterator it = supp.iterator(); it.hasNext();) {
				String path = (String) it.next();
				parts.set(supp.getFullXPath(path));
				if (path.indexOf("/territoryContainment") >= 0) {
					Map attributes = parts.findAttributes("group");
					String type = (String) attributes.get("type");
					addTokens(type, (String) attributes.get("contains"), " ", group_contains);
					continue;
				}
				
                // <zoneItem type="America/Adak" territory="US" aliases="America/Atka US/Aleutian"/>
                if (path.indexOf("/zoneItem") >= 0) {
                    Map attributes = parts.getAttributes(parts.size() - 1);
                    String type = (String) attributes.get("type");
                    String territory = (String) attributes.get("territory");
                    String aliasAttributes = (String) attributes.get("aliases");
                    if (aliasAttributes != null) {
                        String[] aliasesList = aliasAttributes.split("\\s+");
                        
                        for (int i = 0; i < aliasesList.length; ++i) {
                            String alias = aliasesList[i];
                            aliases.add(new String[] {"timezone", alias, type});
                        }
                    }
                    // TODO territory, multizone
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
                if (path.indexOf("/calendarData") >= 0) {
                    Map attributes = parts.findAttributes("calendar");
                    String type = (String) attributes.get("type");
                    String territories = (String) attributes.get("territories");
                    addTerritoryInfo(territories, "calendar", type);
                }
                if (path.indexOf("/weekData") >= 0 || path.indexOf("measurementData") >= 0) {
                    String element = parts.getElement(parts.size() - 1);
                    Map attributes = parts.getAttributes(parts.size() - 1);
                    // later, make this a table
                    String key = "count";
                    String display = "days in week (min)";
                    if (element.equals("firstDay")) {
                        key = "day";
                        display = "first day of week";
                    } else if (element.equals("weekendStart")) {
                        key = "day";
                        display = "first day of weekend";
                    } else if (element.equals("weekendEnd")) {
                        key = "day";
                        display = "last day of weekend";
                    } else if (element.equals("measurementSystem")) {
                        key = "type";
                        display = "measurement system";
                    } else if (element.equals("paperSize")) {
                        key = "type";
                        display = "paper size";
                    }
                    String type = (String) attributes.get(key);
                    String territories = (String) attributes.get("territories");
                    addTerritoryInfo(territories, display, type);
                }
				if (path.indexOf("/generation") >= 0 || path.indexOf("/version") >= 0) continue;
				System.out.println("Skipped Element: " + path);
			}
			territory_languages = getInverse(language_territories);
			script_languages = getInverse(language_scripts);
            
            // now get some metadata
            
            CLDRFile supp2 = cldrFactory.make(CLDRFile.SUPPLEMENTAL_METADATA, false);
            for (Iterator it = supp2.iterator(); it.hasNext();) {
                String path = (String) it.next();
                parts.set(supp2.getFullXPath(path));
                if (path.indexOf("/alias") >= 0) {
                    String element = parts.getElement(parts.size() - 1);
                    Map attributes = parts.getAttributes(parts.size() - 1);
                    String type = (String) attributes.get("type");
                    if (!element.endsWith("Alias")) throw new IllegalArgumentException("Unexpected alias element: " + element);
                    element = element.substring(0,element.length() - 5);
                    String replacement = (String) attributes.get("replacement");
                    String name = "";
                    if (replacement == null) {
                        name = "(none)";
                    } else if (element.equals("language")) {
                        name = getName(replacement, false);
                    } else if (element.equals("zone")) {
                        element = "timezone";
                        name = replacement + "*";
                    } else {
                        int typeCode = CLDRFile.typeNameToCode(element);
                        if (typeCode >= 0) {
                            name = getName(typeCode, replacement, false);
                        } else {
                            name = "*" + replacement;
                        }
                    }
                    aliases.add(new String[] {element, type, name});
                    continue;
                }
                if (path.indexOf("/generation") >= 0 || path.indexOf("/version") >= 0) continue;
                System.out.println("Skipped Element: " + path);                
           }
            
            CLDRFile chars = cldrFactory.make("characters", false);
            int count = 0;
            for (Iterator it = chars.iterator(); it.hasNext();) {
                String path = (String) it.next();
                parts.set(chars.getFullXPath(path));
                if (parts.getElement(1).equals("version")) continue;
                if (parts.getElement(1).equals("generation")) continue;
                String value = parts.getAttributeValue(-2, "value");
                String substitute = chars.getStringValue(path, true);
                String nfc = Normalizer.normalize(value, Normalizer.NFC);
                String nfkc = Normalizer.normalize(value, Normalizer.NFKC);
                if (substitute.equals(nfc)) {
                    count++; continue;
                }
                if (substitute.equals(nfkc)) {
                    count++; continue;
                }
                Object already = charSubstitutions.get(value);
                if (already != null) System.out.println("Duplicate value:" + already);
                charSubstitutions.put(value, substitute);
            }
            if (count != 0) System.out.println("Skipped NFKC/NFC items: " + count);
		}

        /**
         * 
         */
        public void showTerritoryInfo() {
            Map territory_parent = new TreeMap();
            gather("001", territory_parent);
            for (Iterator it = territory_parent.keySet().iterator(); it.hasNext();) {
                String territory = (String) it.next();
                String parent = (String) territory_parent.get(territory);
                System.out.println(territory
                        + "\t" + english.getName(english.TERRITORY_NAME, territory, false)
                        + "\t" + parent
                        + "\t" + english.getName(english.TERRITORY_NAME, parent, false)
                        );
            }
        }
        
        private void gather(String item, Map territory_parent) {
            Collection containedByItem = (Collection) group_contains.get(item);
            if (containedByItem == null) return;
            for (Iterator it = containedByItem.iterator(); it.hasNext();) {
                String contained = (String) it.next();
                territory_parent.put(contained, item);
                gather(contained, territory_parent);
            }
        }

        private void addTerritoryInfo(String territoriesList, String type, String info) {
            String[] territories = territoriesList.split("\\s+");
            territoryTypes.add(type);
            for (int i = 0; i < territories.length; ++i) {
                String territory = getName(CLDRFile.TERRITORY_NAME, territories[i], false);
                Map s = (Map) territoryData.get(territory);
                if (s == null) territoryData.put(territory, s = new TreeMap());
                Set ss = (Set) s.get(type);
                if (ss == null) s.put(type, ss = new TreeSet());
                ss.add(info);
            }
        }
		
		public void showCalendarData(PrintWriter pw0) throws IOException {
            PrintWriter pw = new PrintWriter(new FormattedFileWriter(pw0, "Other Territory Data"));
            pw.println("<tr><th class='source'>Territory</th>");
            for (Iterator it = territoryTypes.iterator(); it.hasNext();) {
                pw.println("<th class='target'>" + it.next() + "</th>");
            }
            pw.println("</tr>");
            
            String worldName = getName(CLDRFile.TERRITORY_NAME, "001", false);
            Map worldData = (Map) territoryData.get(worldName);
            for (Iterator it = territoryData.keySet().iterator(); it.hasNext();) {
                String country = (String) it.next();
                if (country.equals(worldName)) continue;
                showCountry(pw, country, country, worldData);
            }
            showCountry(pw, worldName, "Other", worldData);
           pw.close();
        }

        private void showCountry(PrintWriter pw, String country, String countryTitle, Map worldData) {
            pw.println("<tr><td class='source'>" + countryTitle + "</td>");
            Map data = (Map) territoryData.get(country);
            for (Iterator it2 = territoryTypes.iterator(); it2.hasNext();) {
                String type = (String) it2.next();
                String target = "target";
                Set results = (Set) data.get(type);
                Set worldResults = (Set) worldData.get(type);
                if (results == null) {
                    results = worldResults;
                    target = "target2";
                } else if (results.equals(worldResults)) {
                    target = "target2";                    
                }
                String out = "";
                if (results != null) {
                    out = results+"";
                    out = out.substring(1,out.length()-1); // remove [ and ]
                }
                pw.println("<td class='"+ target + "'>" + out + "</td>");
            }
            pw.println("</tr>");
        }

        public void showCorrespondances() {
			// show correspondances between language and script
			Map name_script = new TreeMap();
			for (Iterator it = sc.getAvailableCodes("script").iterator(); it.hasNext();) {
				String script = (String) it.next();
				String name = (String) english.getName(CLDRFile.SCRIPT_NAME, script, false);
				if (name == null) name = script;
				name_script.put(name, script);
/*				source == CLDRFile.TERRITORY_NAME && target == CLDRFile.LANGUAGE_NAME ? territory_languages
						: source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.TERRITORY_NAME ? language_territories
						: source == CLDRFile.SCRIPT_NAME && target == CLDRFile.LANGUAGE_NAME ? script_languages
						: source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.SCRIPT_NAME ? language_scripts
*/			}
			String delimiter = "\\P{L}+";
			Map name_language = new TreeMap();
			for (Iterator it = sc.getAvailableCodes("language").iterator(); it.hasNext();) {
				String language = (String) it.next();
				String names = english.getName(CLDRFile.LANGUAGE_NAME, language, false);
				if (names == null) names = language;
				name_language.put(names, language);
			}
			for (Iterator it = sc.getAvailableCodes("language").iterator(); it.hasNext();) {
				String language = (String) it.next();
				String names = english.getName(CLDRFile.LANGUAGE_NAME, language, false);
				if (names == null) names = language;				
				String[] words = names.split(delimiter);
				if (words.length > 1) {
					//System.out.println(names);
				}
				for (int i = 0; i < words.length; ++i) {
					String name = words[i];
					String script = (String) name_script.get(name);
					if (script != null) {
						Set langSet = (Set) script_languages.get(script);
						if (langSet != null && langSet.contains(language)) System.out.print("*");
						System.out.println("\t" + name + " [" + language + "]\t=> " + name + " [" + script + "]");
					} else {
						String language2 = (String)name_language.get(name);
						if (language2 != null && !language.equals(language2)) {
							Set langSet = (Set) language_scripts.get(language);
							if (langSet != null) System.out.print("*");
							System.out.print("?\tSame script?\t + " +
								getName(CLDRFile.LANGUAGE_NAME, language, false) + "\t & " + 
								getName(CLDRFile.LANGUAGE_NAME, language2, false));
							langSet = (Set) language_scripts.get(language2);
							if (langSet != null) System.out.print("*");
							System.out.println();
						}
					}
				}
			}
		}


		/**
		 * @throws IOException 
		 * 
		 */
		public void printCurrency(PrintWriter index) throws IOException {
            PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, "Territory \u2192 Currency"));
			//doTitle(pw, "Territory \u2192 Currency");
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
            //doFooter(pw);
            pw.close();
            pw = new PrintWriter(new FormattedFileWriter(index, "Currency Format Info"));
            

			//doTitle(pw, "Currency Format Info");
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
               pw.close();
               //doFooter(pw);

			
//			if (false) {
//				doTitle(pw, "Territories Versus Currencies");
//				pw.println("<tr><th>Territories Without Currencies</th><th>Currencies Without Territories</th></tr>");
//				pw.println("<tr><td class='target'>");
//				Set territoriesWithoutCurrencies = new TreeSet();
//				territoriesWithoutCurrencies.addAll(sc.getGoodAvailableCodes("territory"));
//				territoriesWithoutCurrencies.removeAll(territoriesWithCurrencies);
//				territoriesWithoutCurrencies.removeAll(group_contains.keySet());
//				boolean first = true;
//				for (Iterator it = territoriesWithoutCurrencies.iterator(); it.hasNext();) {
//					if (first) first = false;
//					else pw.print(", ");
//					pw.print(english.getName(CLDRFile.TERRITORY_NAME, it.next().toString(), false));				
//				}
//				pw.println("</td><td class='target'>");
//				Set currenciesWithoutTerritories = new TreeSet();
//				currenciesWithoutTerritories.addAll(sc.getGoodAvailableCodes("currency"));
//				currenciesWithoutTerritories.removeAll(currenciesWithTerritories);
//				first = true;
//				for (Iterator it = currenciesWithoutTerritories.iterator(); it.hasNext();) {
//					if (first) first = false;
//					else pw.print(", ");
//					pw.print(english.getName(CLDRFile.CURRENCY_NAME, it.next().toString(), false));				
//				}
//				pw.println("</td></tr>");
//                   doFooter(pw);
//			}
		}

		/**
		 * @throws IOException 
		 * 
		 */
		public void printAliases(PrintWriter index) throws IOException {
            PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, "Aliases"));
            
			//doTitle(pw, "Aliases");
            pw.println("<tr><th class='source'>" + "Type" + "</th><th class='source'>" + "Code" + "</th><th class='target'>" + "Substitute (if avail)" + "</th></tr>");
			for (Iterator it = aliases.iterator(); it.hasNext();) {
				String[] items = (String[])it.next();
				pw.println("<tr><td class='source'>" + items[0] + "</td><td class='source'>" + items[1] + "</td><td class='target'>" + items[2] + "</td></tr>");
			}
               //doFooter(pw);
               pw.close();
		}
		
		//deprecatedItems
//		public void printDeprecatedItems(PrintWriter pw) {
//			doTitle(pw, "Deprecated Items");
//			pw.print("<tr><td class='z0'><b>Type</b></td><td class='z1'><b>Elements</b></td><td class='z2'><b>Attributes</b></td><td class='z4'><b>Values</b></td>");
//			for (Iterator it = deprecatedItems.iterator(); it.hasNext();) {
//				Map source = (Map)it.next();
//				Object item;
//				pw.print("<tr>");
//				pw.print("<td class='z0'>" + ((item = source.get("type")) != null ? item : "<i>any</i>")  + "</td>");
//				pw.print("<td class='z1'>" + ((item = source.get("elements")) != null ? item : "<i>any</i>")  + "</td>");
//				pw.print("<td class='z2'>" + ((item = source.get("attributes")) != null ? item : "<i>any</i>") + "</td>");
//				pw.print("<td class='z4'>" + ((item = source.get("values")) != null ? item : "<i>any</i>") + "</td>");
//				pw.print("</tr>");
//			}
//               doFooter(pw);
//		}
		
		public void printWindows_Tzid(PrintWriter index) throws IOException {
            PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, "Windows \u2192 Tzid"));
			//doTitle(pw, "Windows \u2192 Tzid");
			for (Iterator it = windows_tzid.keySet().iterator(); it.hasNext();) {
				String source = (String)it.next();
				String target = (String)windows_tzid.get(source);
				pw.println("<tr><td class='source'>" + source + "</td><td class='target'>" + target + "</td></tr>");
			}
               //doFooter(pw);
               pw.close();
		}
		
		//<info iso4217="ADP" digits="0" rounding="0"/>

		public void printContains(PrintWriter index) throws IOException {
			String title = "Territory Containment (UN M.49)";
            
            PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, title));
			//doTitle(pw, title);
			printContains2(pw, "<tr>", "001", 0, true);
            //doFooter(pw);
            pw.close();
		}
        
        public void printCharacters(PrintWriter index) throws IOException {
            String title = "Character Fallback Substitutions";
            
            PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, title));
            //doTitle(pw, title);
            pw.println("<tr><th colSpan='3'>Character</th><th colSpan='3'>Substitution (if not in target charset)</th></tr>");
            for (Iterator it = charSubstitutions.keySet().iterator(); it.hasNext();) {
                String value = (String)it.next();
                String substitute = (String)charSubstitutions.get(value);
                pw.println("<tr><td class='source'>"
                        + hex(value, ", ") + "</td><td class='source'>"
                        + value + "</td><td class='source'>"
                        + UCharacter.getName(value, ", ") + "</td><td class='target'>"
                        + hex(substitute, ", ") + "</td><td class='target'>"
                        + substitute + "</td><td class='target'>"
                        + UCharacter.getName(substitute, ", ") 
                        + "</td></tr>" );
                
            }
            //doFooter(pw);
            pw.close();
        }
        

        public static String hex(String s, String separator) {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < s.length(); ++i) {
                if (i != 0) result.append(separator);
                com.ibm.icu.impl.Utility.hex(s.charAt(i), result);
            }
            return result.toString();
        }

        
		/**
		 * 
		 */
//		private PrintWriter doTitle(PrintWriter pw, String title) {
//			//String anchor = FileUtilities.anchorize(title);
//			pw.println("<div align='center'><table>");
//			//anchors.put(title, anchor);
//            //PrintWriter result = null;
//            //return result;
//		}
        
        

//        private void doFooter(PrintWriter pw) {
//            pw.println("</table></div>");
//        }

		public void printContains2(PrintWriter pw, String lead, String start, int depth, boolean isFirst) {
			String name = depth == 4 ? start : getName(CLDRFile.TERRITORY_NAME, start, false);
			if (!isFirst) pw.print(lead);
			int count = getTotalContainedItems(start, depth);
			pw.print("<td class='z" + depth + "' rowSpan='" + count + "'>" + name + "</td>"); // colSpan='" + (5 - depth) + "' 
			if (depth == 4) pw.println("</tr>");
			Collection contains = getContainedCollection(start, depth);
			if (contains != null) {
				Collection contains2 = new TreeSet(territoryNameComparator);
				contains2.addAll(contains);
				boolean first = true;
				for (Iterator it = contains2.iterator(); it.hasNext();) {
					String item = (String)it.next();
					printContains2(pw, lead, item, depth+1, first); //  + "<td>&nbsp;</td>"
					first = false;
				}
			}
		}
		
		private int getTotalContainedItems(String start, int depth) {
			Collection c = getContainedCollection(start, depth);
			if (c == null) return 1;
			int sum = 0;
			for (Iterator it = c.iterator(); it.hasNext();) {
				sum += getTotalContainedItems((String)it.next(), depth + 1);
			}
			return sum;
		}
		
		/**
		 * 
		 */
		private Collection getContainedCollection(String start, int depth) {
			Collection contains = (Collection) group_contains.get(start);
			if (contains == null) {
				contains = (Collection) sc.getCountryToZoneSet().get(start);
				if (contains == null && depth == 3) {
					contains = new TreeSet();
					if (start.compareTo("A") >= 0) {
						contains.add("<font color='red'>MISSING TZID</font>");
					} else {
						contains.add("<font color='red'>Not yet ISO code</font>");
					}
				}
			}
			return contains;
		}

		/**
		 * @param table TODO
		 * 
		 */
		public void printMissing(PrintWriter pw, int source, int table) {
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
				if (table == CLDRFile.SCRIPT_NAME) missingItems.removeAll(language_scripts.keySet());
                if (table == CLDRFile.TERRITORY_NAME) missingItems.removeAll(language_territories.keySet());
			} else  {
				throw new IllegalArgumentException("Illegal code");
			}
			Set missingItemsNamed = new TreeSet(col);
			for (Iterator it = missingItems.iterator(); it.hasNext();) {
				String item = (String) it.next();
				List data = sc.getFullData(type, item);
				if (data.get(0).equals("PRIVATE USE")) continue;
				if (data.size() < 3) continue;
				if (!"".equals(data.get(2))) continue;

				String itemName = getName(source, item, true);
				missingItemsNamed.add(itemName);
			}
            pw.println("<div align='center'><table>");
			for (Iterator it = missingItemsNamed.iterator(); it.hasNext();) {
				pw.println("<tr><td class='target'>" + it.next() + "</td></tr>");
			}
            pw.println("</table></div>");
		}

		// source, eg english.TERRITORY_NAME
		// target, eg english.LANGUAGE_NAME
		public void print(PrintWriter pw, int source, int target) {
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
            
            pw.println("<div align='center'><table>");

			for (Iterator it = territory_languageNames.keySet().iterator(); it.hasNext();) {
				String territoryName = (String) it.next();
				pw.println("<tr><td class='source' colspan='2'>" + territoryName + "</td></tr>");
				Set s = (Set) territory_languageNames.get(territoryName);
				for (Iterator it2 = s.iterator(); it2.hasNext();) {
					String languageName = (String) it2.next();
					pw.println("<tr><td>&nbsp;</td><td class='target'>" + languageName + "</td></tr>");
				}
			}
            pw.println("</table></div>");

		}

		/**
		 * @param codeFirst TODO
		 * 
		 */
		private String getName(int type, String oldcode, boolean codeFirst) {
			int pos = oldcode.indexOf('*');
			String code = pos < 0 ? oldcode : oldcode.substring(0,pos);
			String ename = english.getName(type, code, false);
			return codeFirst
                    ? "[" + oldcode +"]\t" + (ename == null ? code : ename)
					: (ename == null ? code : ename) + "\t[" + oldcode +"]";
		}
		
		private String getName(String locale, boolean codeFirst) {
			String ename = english.getName(locale, false);
			return codeFirst 
                    ? "[" + locale +"]\t" + (ename == null ? locale : ename)
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