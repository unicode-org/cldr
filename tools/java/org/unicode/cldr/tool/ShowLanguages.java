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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.FileUtilities;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class ShowLanguages {
  static CLDRFile english;
  
  static Comparator col = new CollectionUtilities.MultiComparator(new Comparator[] { Collator.getInstance(new ULocale("en")), new UTF16.StringComparator(true, false, 0) });
  
  static StandardCodes sc = StandardCodes.make();
  
  static Factory cldrFactory;
  
  public static void main(String[] args) throws IOException {
    cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
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
    
    // linfo.printCountryData(pw);
    linfo.showCountryLanguageInfo(pw);
    
    linfo.showLanguageCountryInfo(pw);
    
    //linfo.printDeprecatedItems(pw);
    linfo.printCurrency(pw);
    
    PrintWriter pw1 = new PrintWriter(new FormattedFileWriter(pw, "Languages and Territories", null));
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
    
    pw1 = new PrintWriter(new FormattedFileWriter(pw, "Languages and Scripts", null));
    
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
    
    if (System.getProperty("Coverage") != null) {
      linfo.showCoverageGoals(pw);
    }
    
    linfo.showCorrespondances();
    
    //linfo.showCalendarData(pw);
    
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
    String[] replacements = { "%date%", df.format(new Date()), "%contents%", contents, "%data%", sw.toString() };
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
    
    public FormattedFileWriter(PrintWriter indexFile, String title, String explanation) throws IOException {
      String anchor = FileUtilities.anchorize(title);
      filename = anchor + ".html";
      this.title = title;
      anchors.add("<a name='" + FileUtilities.anchorize(getTitle()) + "' href='" + getFilename() + "'>" + getTitle() + "</a></caption>");
      if (explanation != null) {
        out.write(explanation);
      }
      out.write("<div align='center'><table>");
    }
    
    public String getFilename() {
      return filename;
    }
    
    public String getTitle() {
      return title;
    }
    
    public void close() throws IOException {
      out.write("</table></div>");
      PrintWriter pw2 = BagFormatter.openUTF8Writer(Utility.COMMON_DIRECTORY + "../diff/supplemental/", filename);
      String[] replacements = { "%header%", "", "%title%", title, "%version%", "1.5-draft", "%date%", df.format(new Date()), "%body%", out.toString() };
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
  
  public static Map<String,String> territoryAliases = new HashMap();
  
  static class LanguageInfo {
    private static final Map<String,Map<String,String>> localeAliasInfo = new TreeMap();
    
    Map language_scripts = new TreeMap();
    
    Map language_territories = new TreeMap();
    
    Map windows_tzid = new TreeMap();
    
    List deprecatedItems = new ArrayList();
    
    Map territory_languages;
    
    Map script_languages;
    
    Map group_contains = new TreeMap();
    
    Set aliases = new TreeSet(new ArrayComparator(new Comparator[] { new UTF16.StringComparator(), col }));
    
    Comparator col3 = new ArrayComparator(new Comparator[] { col, col, col });
    
    Map currency_fractions = new TreeMap(col);
    
    Map currency_territory = new TreeMap(col);
    
    Map territory_currency = new TreeMap(col);
    
    Set territoriesWithCurrencies = new TreeSet();
    
    Set currenciesWithTerritories = new TreeSet();
    
    Map territoryData = new TreeMap();
    
    Set territoryTypes = new TreeSet();
    
    Map charSubstitutions = new TreeMap(col);
    
    String defaultDigits = null;
    
    Map<String,Map<String,Object>> territoryLanguageData = new TreeMap<String,Map<String,Object>>();
    
    private Relation<String,String> territoriesToModernCurrencies = new Relation(new TreeMap(), TreeSet.class, null);
    
    public LanguageInfo(Factory cldrFactory) throws IOException {
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
              aliases.add(new String[] { "timezone", alias, type });
            }
          }
          // TODO territory, multizone
          continue;
        }
        
        if (path.indexOf("/currencyData") >= 0) {
          if (path.indexOf("/fractions") >= 0) {
            //<info iso4217="ADP" digits="0" rounding="0"/>
            String element = parts.getElement(parts.size() - 1);
            if (!element.equals("info"))
              throw new IllegalArgumentException("Unexpected fractions element: " + element);
            Map attributes = parts.getAttributes(parts.size() - 1);
            String iso4217 = (String) attributes.get("iso4217");
            String digits = (String) attributes.get("digits");
            String rounding = (String) attributes.get("rounding");
            digits = digits + (rounding.equals("0") ? "" : " (" + rounding + ")");
            if (iso4217.equals("DEFAULT"))
              defaultDigits = digits;
            else
              currency_fractions.put(getName(CLDRFile.CURRENCY_NAME, iso4217, false), digits);
            continue;
          }
          //<region iso3166="AR">
          //	<currency iso4217="ARS" from="1992-01-01"/>
          if (path.indexOf("/region") >= 0) {
            Map attributes = parts.getAttributes(parts.size() - 2);
            String iso3166 = (String) attributes.get("iso3166");
            attributes = parts.getAttributes(parts.size() - 1);
            String iso4217 = (String) attributes.get("iso4217");
            String to = (String) attributes.get("to");
            if (to == null)
              to = "\u221E";
            String from = (String) attributes.get("from");
            if (from == null)
              from = "-\u221E";
            String countryName = getName(CLDRFile.TERRITORY_NAME, iso3166, false);
            String currencyName = getName(CLDRFile.CURRENCY_NAME, iso4217, false);
            Set info = (Set) territory_currency.get(countryName);
            if (info == null)
              territory_currency.put(countryName, info = new TreeSet(col3));
            info.add(new String[] { from, to, currencyName });
            info = (Set) currency_territory.get(currencyName);
            if (info == null)
              currency_territory.put(currencyName, info = new TreeSet(col));
            territoriesWithCurrencies.add(iso3166);
            currenciesWithTerritories.add(iso4217);
            if (to.equals("\u221E") || to.compareTo("2006") > 0) {
              territoriesToModernCurrencies.put(iso3166, iso4217);
              info.add("<b>" + countryName + "</b>");

            } else {
              info.add("<i>" + countryName + "</i>");
              
            }
            continue;
          }
        }
        
        if (path.indexOf("/languageData") >= 0) {
          Map attributes = parts.findAttributes("language");
          String language = (String) attributes.get("type");
          String alt = (String) attributes.get("alt");
          addTokens(language, (String) attributes.get("scripts"), " ", language_scripts);
          // mark the territories
          if (alt == null)
            ; // nothing
          else if ("secondary".equals(alt))
            language += "*";
          else
            language += "*" + alt;
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
          String display = "Days in week (min)";
          if (element.equals("firstDay")) {
            key = "day";
            display = "First day of week";
          } else if (element.equals("weekendStart")) {
            key = "day";
            display = "First day of weekend";
          } else if (element.equals("weekendEnd")) {
            key = "day";
            display = "Last day of weekend";
          } else if (element.equals("measurementSystem")) {
            //<measurementSystem type="metric"  territories="001"/>
            key = "choice";
            display = "Meas. system";
          } else if (element.equals("paperSize")) {
            key = "type";
            display = "Paper Size";
          }
          String type = (String) attributes.get(key);
          String territories = (String) attributes.get("territories");
          addTerritoryInfo(territories, display, type);
        }
        if (path.indexOf("/territoryInfo") >= 0) {
          Map<String,String> attributes = parts.getAttributes(2);
          String type = (String) attributes.get("type");
          String name = english.getName(english.TERRITORY_NAME, type, false);
          Map languageData = (Map) territoryLanguageData.get(name);
          if (languageData == null) territoryLanguageData.put(name, languageData = new TreeMap());
          languageData.put("code", attributes.get("type"));
          languageData.put("gdp", attributes.get("gdp"));
          languageData.put("literacyPercent", attributes.get("literacyPercent"));
          languageData.put("population", attributes.get("population"));
          if (parts.size() > 3) {
            attributes = parts.getAttributes(3);
            Set languageData2 = (Set) languageData.get("language");
            if (languageData2 == null) languageData.put("language", languageData2 = new TreeSet(INVERSE_COMPARABLE));
            String literacy = attributes.get("literacyPercent");
            languageData2.add(
                new Pair(Double.parseDouble(attributes.get("populationPercent")),
                    new Pair(literacy == null ? Double.NaN : Double.parseDouble(literacy),
                        attributes.get("type"))));
          }
//        supplementalData/territoryTestData/territory[@type="AU"][@gdp="640100000000"][@literacy="0.99"][@population="20000000"]/languagePopulation[@type="zh_Hant"][@functionallyLiterate="420000"]
//        supplementalData/territoryTestData/territory[@type="GP"][@gdp="3513000000"][@literacy="0.99"][@population="450000"]
          continue;
        }
        if (path.indexOf("/generation") >= 0 || path.indexOf("/version") >= 0)
          continue;
        System.out.println("Skipped Element: " + path);
      }
      territory_languages = getInverse(language_territories);
      script_languages = getInverse(language_scripts);
      
      // now get some metadata
      localeAliasInfo.put("language", new TreeMap());
      localeAliasInfo.put("script", new TreeMap());
      localeAliasInfo.put("territory", new TreeMap());
      localeAliasInfo.put("variant", new TreeMap());
      localeAliasInfo.put("zone", new TreeMap());
      
      localeAliasInfo.get("language").put("no", "nb");
      localeAliasInfo.get("language").put("zh_CN", "zh_Hans_CN");
      localeAliasInfo.get("language").put("zh_SG", "zh_Hans_SG");
      localeAliasInfo.get("language").put("zh_TW", "zh_Hant_TW");
      localeAliasInfo.get("language").put("zh_MO", "zh_Hant_MO");
      localeAliasInfo.get("language").put("zh_HK", "zh_Hant_HK");
      
      CLDRFile supp2 = cldrFactory.make(CLDRFile.SUPPLEMENTAL_METADATA, false);
      for (Iterator it = supp2.iterator(); it.hasNext();) {
        String path = (String) it.next();
        parts.set(supp2.getFullXPath(path));
        if (path.indexOf("/alias") >= 0) {
          String element = parts.getElement(parts.size() - 1);
          Map attributes = parts.getAttributes(parts.size() - 1);
          String type = (String) attributes.get("type");
          if (!element.endsWith("Alias"))
            throw new IllegalArgumentException("Unexpected alias element: " + element);
          element = element.substring(0, element.length() - 5);
          String replacement = (String) attributes.get("replacement");
          localeAliasInfo.get(element).put(type, replacement);
          
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
          if (element.equals("territory")) {
            territoryAliases.put(type,name);
            aliases.add(new String[] { element, getName(CLDRFile.TERRITORY_NAME, type, false), name });
          } else {
            aliases.add(new String[] { element, type, name });
          }
          continue;
        }
        if (path.indexOf("/generation") >= 0 || path.indexOf("/version") >= 0)
          continue;
        System.out.println("Skipped Element: " + path);
      }
      Log.setLog("characterLog.txt");
      CLDRFile chars = cldrFactory.make("characters", false);
      int count = 0;
      for (Iterator it = chars.iterator("", CLDRFile.ldmlComparator); it.hasNext();) {
        String path = (String) it.next();
        parts.set(chars.getFullXPath(path));
        if (parts.getElement(1).equals("version"))
          continue;
        if (parts.getElement(1).equals("generation"))
          continue;
        String value = parts.getAttributeValue(-2, "value");
        String substitute = chars.getStringValue(path, true);
        addCharSubstitution(value, substitute);
      }
      if (count != 0)
        System.out.println("Skipped NFKC/NFC items: " + count);
      Log.close();
    }
    
    static final Comparator INVERSE_COMPARABLE = new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable)o2).compareTo(o1);
      }  
    };
    
//  public void printCountryData(PrintWriter pw) throws IOException {
//  NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
//  nf.setGroupingUsed(true);
//  NumberFormat pf = NumberFormat.getPercentInstance(ULocale.ENGLISH);
//  pf.setMinimumFractionDigits(1);
//  pf.setMaximumFractionDigits(1);
//  PrintWriter pw2 = showCountryDataHeader(pw, "Territory-Language Information");
//  pw2.println("<tr>" +
//  "<th class='source'>Territory</th>" +
//  "<th class='source'>Code</th>" +
//  "<th class='target'>Population</th>" +
//  "<th class='target'>Literacy</th>" +
//  "<th class='target'>GDP (PPP)</th>" +
//  
//  "<th class='target'>Language</th>" +
//  "<th class='target'>Code</th>" +
//  "<th class='target'>Population</th>" +
//  "<th class='target'>Literacy</th>" +
//  "</tr>");
//  for (String territoryName : territoryLanguageData.keySet()) {
//  Map<String,Object>results = territoryLanguageData.get(territoryName);
//  Set<Pair<Double,Pair<Double,String>>> language = (Set<Pair<Double,Pair<Double,String>>>)results.get("language");
//  int span = language == null ? 0 : language.size();
//  String spanString = span == 0 ? "" : " rowSpan='"+span+"'";
//  double population = Double.parseDouble((String)results.get("population"));
//  double gdp = Double.parseDouble((String)results.get("gdp"));
//  pw2.println("<tr>" +
//  "<td class='source'" + spanString + ">" + territoryName + "</td>" +
//  "<td class='source'" + spanString + ">" + results.get("code") + "</td>" +
//  "<td class='targetRight'" + spanString + ">" + (population <= 1 ? "<i>na</i>" : nf.format(population)) + "</td>" +
//  "<td class='targetRight'" + spanString + ">" + pf.format(Double.parseDouble((String)results.get("literacyPercent"))/100) + "</td>" +
//  "<td class='targetRight'" + spanString + ">" + (gdp <= 1 ? "<i>na</i>" : nf.format(gdp)) + "</td>");
//  if (span == 0) {
//  pw2.println("<td class='source'><i>na</i></td>" +
//  "<td class='source'><i>na</i></td>"
//  + "<td class='targetRight'><i>na</i></td>"
//  + "<td class='targetRight'><i>na</i></td>"
//  + "</tr>");
//  } else {
//  boolean first = true;
//  for (Pair<Double,Pair<Double,String>> languageCodePair : language) {
//  double languagePopulation = languageCodePair.first;
//  double languageliteracy = languageCodePair.second.first;
//  String languageCode = languageCodePair.second.second;
//  if (first) {
//  first = false;
//  } else {
//  pw2.println("<tr>");
//  }
//  double proportion = languagePopulation/population;
//  if (proportion > 1) {
//  System.out.println("Warning - proportion > 100:" + territoryName + ", " + english.getName(languageCode, false));
//  proportion = 1;
//  }
//  pw2.println(
//  "<td class='source'>" + english.getName(languageCode, false) + "</td>" +
//  "<td class='source'>" + languageCode + "</td>"
//  + "<td class='targetRight'>" + pf.format(languagePopulation/100) + "</td>"
//  + "<td class='targetRight'>" + (Double.isNaN(languageliteracy) ? "<i>na</i>" : pf.format(languageliteracy/100)) + "</td>"
//  + "</tr>");
//  }
//  }
//  }
//  pw2.close();
//  /*
//  *           Map languageData = (Map) territoryLanguageData.get(type);
//  if (languageData == null) territoryLanguageData.put(type, languageData = new TreeMap());
//  languageData.put("gdp", attributes.get("gdp"));
//  languageData.put("literacy", attributes.get("literacy"));
//  languageData.put("population", attributes.get("population"));
//  attributes = parts.getAttributes(3);
//  if (attributes != null) {
//  Map languageData2 = (Map) languageData.get("language");
//  if (languageData2 == null) territoryLanguageData.put(type, languageData2 = new LinkedHashMap());
//  languageData.put(attributes.get("type"), attributes.get("functionallyLiterate"));
//  }
//  
//  */
////pw.println("<tr><th class='source'>Territory</th><th class='target'>From</th><th class='target'>To</th><th class='target'>Currency</th></tr>");
////for (Iterator it = territory_currency.keySet().iterator(); it.hasNext();) {
////String territory = (String) it.next();
////Set info = (Set) territory_currency.get(territory);
////pw.println("<tr><td class='source' rowSpan='" + info.size() + "'>" + territory + "</td>");
////boolean first = true;
////for (Iterator it2 = info.iterator(); it2.hasNext();) {
////String[] items = (String[]) it2.next();
////if (first)
////first = false;
////else
////pw.println("<tr>");
////pw.println("<td class='target'>" + items[0] + "</td><td class='target'>" + items[1] + "</td><td class='target'>" + items[2] + "</td></tr>");
////}
////}
//////doFooter(pw);
////pw.close();
////pw = new PrintWriter(new FormattedFileWriter(index, "Currency Format Info"));
////
//////doTitle(pw, "Currency Format Info");
////pw.println("<tr><th class='source'>Currency</th><th class='target'>Digits</th><th class='target'>Countries</th></tr>");
////Set currencyList = new TreeSet(col);
////currencyList.addAll(currency_fractions.keySet());
////currencyList.addAll(currency_territory.keySet());
////
////for (Iterator it = currencyList.iterator(); it.hasNext();) {
////String currency = (String) it.next();
////String fractions = (String) currency_fractions.get(currency);
////if (fractions == null)
////fractions = defaultDigits;
////Set territories = (Set) currency_territory.get(currency);
////pw.print("<tr><td class='source'>" + currency + "</td><td class='target'>" + fractions + "</td><td class='target'>");
////if (territories != null) {
////boolean first = true;
////for (Iterator it2 = territories.iterator(); it2.hasNext();) {
////if (first)
////first = false;
////else
////pw.print(", ");
////pw.print(it2.next());
////}
////}
////pw.println("</td></tr>");
////}
////pw.close();
//////doFooter(pw);
////
//  }
    
    // http://www.faqs.org/rfcs/rfc2396.html
    //    delims      = "<" | ">" | "#" | "%" | <">
    //"{" | "}" | "|" | "\" | "^" | "[" | "]" | "`"
    // Within a query component, the characters ";", "/", "?", ":", "@",
    // "&", "=", "+", ",", and "$" are reserved.
    static final UnicodeSet ESCAPED_URI_QUERY = (UnicodeSet) new UnicodeSet("[\\u0000-\\u0020\\u007F <>#%\"\\{}|\\\\\\^\\[\\]`;/?:@\\&=+,$\\u0080-\\U0001FFFF]").freeze();
    static {
      System.out.println(new UnicodeSet(ESCAPED_URI_QUERY).complement());
    }
    
    private String urlEncode(String input) {
      try {
        byte[] utf8 = input.getBytes("utf-8");
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < utf8.length; ++i) {
          int b = utf8[i]&0xFF;
          if (ESCAPED_URI_QUERY.contains(b)) {
            output.append('%');
            if (b < 0x10) output.append('0');
            output.append(Integer.toString(b,16));
          } else {
            output.append((char)b);
          }
        }
        return output.toString();
      } catch (UnsupportedEncodingException e) {
        throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
      }
    }
    private String addBug(int bugNumber, String text, String from, String subject, String body) {
      if (from.length() != 0) {
        from = "&from=" + urlEncode(from);
      }
      if (body.length() != 0) {
        body = "&body=" + urlEncode(body);
      }
      if (subject.length() != 0) {
        subject = "&subject=" + urlEncode(subject);
      }
      return "<a target='_blank' href='http://unicode.org/cldr/bugs/locale-bugs/data?compose=" + bugNumber + from + subject + body + "'>" + text + "</a>";
    }
    
    private void showLanguageCountryInfo(PrintWriter pw) throws IOException {
      PrintWriter pw21 = new PrintWriter(new FormattedFileWriter(pw, "Language-Territory Information", 
          "<div  style='margin:1em'><p>The language data is provided for localization testing, and is under development for CLDR 1.5. " +
          "To add a new territory for a language, see the <i>add new</i> links below. " +
          "For more information, see <a href=\"territory_language_information.html\">Territory-Language Information.</a>" +
      "<p></div>"));
      PrintWriter pw2 = pw21;
      NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
      nf.setGroupingUsed(true);
      NumberFormat percent = new DecimalFormat("000.0%");
      TablePrinter tablePrinter = new TablePrinter()
      //tablePrinter.setSortPriorities(0,5)
      .addColumn("Language", "class='source'", null, "class='source'", true).setSortPriority(0).setBreakSpans(true)
      .addColumn("Code", "class='source'", "<a name=\"{0}\">{0}</a>", "class='source'", true)
      //.addColumn("Report Bug", "class='target'", null, "class='target'", false)
      .addColumn("Territory", "class='target'", null, "class='target'", true)
      .addColumn("Code", "class='target'", "<a href=\"territory_language_information.html#{0}\">{0}</a>", "class='target'", true)
      .addColumn("Language Population", "class='target'", "{0,number,#,##0}", "class='targetRight'", true).setSortPriority(1).setSortAscending(false)
//    .addColumn("Territory Population", "class='target'", "{0,number,#,##0}", "class='targetRight'", true)
//    .addColumn("Language Literacy", "class='target'", "{0,number,00.0}%", "class='targetRight'", true)
//    .addColumn("Territory Literacy", "class='target'", "{0,number,00.0}%", "class='targetRight'", true)
      //.addColumn("Territory GDP (PPP)", "class='target'", "{0,number,#,##0}", "class='targetRight'", true) 
      ;
      TreeSet<String> languages = new TreeSet();
      Collection<Comparable[]> data = new ArrayList<Comparable[]>();
      String msg = "<br><i>click on each country code</i>";
      for (String territoryName : territoryLanguageData.keySet()) {
        Map<String,Object>results = territoryLanguageData.get(territoryName);
        Set<Pair<Double,Pair<Double,String>>> language = (Set<Pair<Double,Pair<Double,String>>>)results.get("language");
        double population = Double.parseDouble((String)results.get("population"));
        double gdp = Double.parseDouble((String)results.get("gdp"));
        if (language == null) {
          Comparable[] items = new Comparable[]{
              english.getName("und", false) + msg,
              "und",
              //bug,
              territoryName,
              (String)results.get("code"),
              Double.NaN,
//            population,
//            Double.NaN,
//            Double.parseDouble((String)results.get("literacyPercent")),
//            gdp
          };
          data.add(items);
        } else {
          for (Pair<Double,Pair<Double,String>> languageCodePair : language) {
            double languagePopulation = languageCodePair.first;
            double languageliteracy = languageCodePair.second.first;
            String languageCode = languageCodePair.second.second;
            languages.add(languageCode);
            double territoryLiteracy = Double.parseDouble((String)results.get("literacyPercent"));
            if (Double.isNaN(languageliteracy)) {
              languageliteracy = territoryLiteracy;
            }
            Comparable[] items = new Comparable[]{
                english.getName(languageCode, false) + msg,
                languageCode,
                //bug,
                territoryName,
                (String)results.get("code"),
                languagePopulation/100 * population,
//              population,
//              languageliteracy,
//              territoryLiteracy,
//              gdp
            };
            data.add(items);
          }
        }
      }
      for (String languageCode : languages) {
        Comparable[] items = new Comparable[]{
            english.getName(languageCode, false) + msg,
            languageCode,
            //bug,
            addBug(1217, "<i>add new</i>", "<email>", "add territory to " + english.getName(languageCode, false) + " (" + languageCode + ")", "<territory, speaker population in territory, and references>"),
            "",
            0.0d,
//          0.0d,
//          0.0d,
//          0.0d,
//          gdp
        };
        data.add(items);
        
      }
      Comparable[][] flattened = data.toArray(new Comparable[data.size()][]);
      String value = tablePrinter.addRows(flattened).toTable();
      pw2.println(value);
      pw2.close();
    }
    
    private void showCoverageGoals(PrintWriter pw) throws IOException {
      PrintWriter pw2 = new PrintWriter(new FormattedFileWriter(pw, "Coverage Goals", "<p>" +
          "The following show default coverage goals for member data submitters. " +
          "<i>[basic]</i> shows where there is no specific value for a given vendor, " +
          "while <i>(...)</i> indicates that the goal is inherited from the parent. " +
          "A * is added if the goal differs from the parent locale's goal. " +
          "For information on what these goals mean (comprehensive, modern, moderate,...), see the LDML specification " +
          "<a href='http://www.unicode.org/reports/tr35/#Coverage_Levels'>Appendix M: Coverage Levels</a>. " +
          "See also the coverageAdditions in <a href='http://www.unicode.org/cldr/data/common/supplemental/supplementalMetadata.xml'>supplemental metadata</a>." +
      "</p>"));
      
      
      TablePrinter tablePrinter = new TablePrinter()
      //tablePrinter.setSortPriorities(0,4)
      .addColumn("Language", "class='source'", null, "class='source'", true).setSortPriority(0)
      .addColumn("Locale", "class='source'", null, "class='source'", false)
      .addColumn("Code", "class='source'", "<a href=\"http://www.unicode.org/cldr/data/common/main/{0}.xml\">{0}</a>", "class='source'", false)
      .addColumn("CLDR", "class='source'", null, "class='source'", false).setSortAscending(false).setSortPriority(1)
      ;
      Map<String, Map<String, String>> vendordata = sc.getLocaleTypes();
      Set<String> locales = new TreeSet();
      for (String vendor : vendordata.keySet()) {
        if (vendor.equals("Java")) continue;
        tablePrinter.addColumn(vendor, "class='target'", null, "class='target'", false);
        locales.addAll(vendordata.get(vendor).keySet());
      }
      Collection<Comparable[]> data = new ArrayList<Comparable[]>();
      List<String> list = new ArrayList<String>();
      LanguageTagParser ltp = new LanguageTagParser();
      String alias2 = getAlias("sh_YU");
      
      for (String locale : locales) {
        String alias = getAlias(locale);
        if (!alias.equals(locale)) {
          System.out.println("Should use canonical form: " + locale + " => " + alias);
        }
        list.clear();
        String baseLang = ltp.set(locale).getLanguage();
        String baseLangName = english.getName(baseLang, false);
        list.add(baseLangName);
        list.add(baseLang.equals(locale) ? "" : english.getName(locale, false));
        list.add(locale);
        list.add(cldrFactory.getAvailable().contains(locale) ? "\u00A0" : baseLang.equals(locale) ? "No" : "<i>(No)</i>");
        for (String vendor : vendordata.keySet()) {
          if (vendor.equals("Java")) continue;
          String status = getVendorStatus(locale, vendor, vendordata);
          if (!baseLang.equals(locale) && !status.startsWith("<")) {
            String langStatus = getVendorStatus(baseLang, vendor, vendordata);
            if (!langStatus.equals(status)) {
              status += "*";
            }
          }
          list.add(status);
        }
        data.add(list.toArray(new String[list.size()]));
      }
      Comparable[][] flattened = data.toArray(new Comparable[data.size()][]);
      String value = tablePrinter.addRows(flattened).toTable();
      pw2.println(value);
      pw2.close();
    }
    
    LanguageTagParser lpt2 = new LanguageTagParser();
    private String getAlias(String locale) {
      lpt2.set(locale);
      locale = lpt2.toString(); // normalize
      String language = lpt2.getLanguage();
      String script = lpt2.getScript();
      String region = lpt2.getRegion();
      //List variants = lpt2.getVariants();
      String temp;
      for (String old : localeAliasInfo.get("language").keySet()) {
        if (locale.startsWith(old)) {
          temp = localeAliasInfo.get("language").get(old);
          lpt2.setLanguage(temp.split("\\s+")[0] + locale.substring(old.length()));
          break;
        }
      }
      temp = localeAliasInfo.get("script").get(script);
      if (temp != null) {
        lpt2.setScript(temp.split("\\s+")[0]);
      }
      temp = localeAliasInfo.get("territory").get(region);
      if (temp != null) {
        lpt2.setRegion(temp.split("\\s+")[0]);
      }
      return lpt2.toString();
    }
    
    private String getVendorStatus(String locale, String vendor, Map<String, Map<String, String>> vendordata) {
      String status = vendordata.get(vendor).get(locale);
      String curLocale = locale;
      while (status == null) {
        curLocale = LanguageTagParser.getParent(curLocale);
        if (curLocale.equals("root")) {
          status = "<i>[basic]</i>";
          break;
        }
        status = vendordata.get(vendor).get(curLocale);
        if (status != null) {
          status = "<i>(" + status + ")</i>";
        }
      }
      return status;
    }
    
    private void showCountryLanguageInfo(PrintWriter pw) throws IOException {
      PrintWriter pw21 = new PrintWriter(new FormattedFileWriter(pw, "Territory-Language Information", 
          "<div  style='margin:1em'><p>The language data is provided for localization testing, and is under development for CLDR 1.5. " +
          "The main goal is to provide approximate figures for the literate, functional population for each language in each territory: " +
          "that is, the population that is able to read and write each language, and is comfortable enough to use it with computers. " +
          "</p><p>The GDP and Literacy figures are taken from the World Bank where available, otherwise supplemented by FactBook data and other sources. " +
          "Much of the per-language data is taken from the Ethnologue, but is supplemented and processed using many other sources, including per-country census data. " +
          "(The focus of the Ethnologue is native speakers, which includes people who are not literate, and excludes people who are functional second-langauge users.) " +
          "</p><p>The percentages may add up to more than 100% due to multilingual populations, " +
          "or may be less than 100% due to illiteracy or because the data has not yet been gathered or processed. " +
          "Languages with a population of less than 100,000 or 1% of the territory population are currently omitted. " +
          "<p><b>Defects:</b> If you find errors or omissions in this data, please report the information with the <i>bug</i> or <i>add new</i> link below." +
          "</p></div>"));
      PrintWriter pw2 = pw21;
      NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
      nf.setGroupingUsed(true);
      NumberFormat percent = new DecimalFormat("000.0%");
      TablePrinter tablePrinter = new TablePrinter()
      //tablePrinter.setSortPriorities(0,4)
      .addColumn("Territory", "class='source'", null, "class='source'", true).setSortPriority(0).setBreakSpans(true)
      .addColumn("Code", "class='source'", "<a name=\"{0}\">{0}</a>", "class='source'", true)
      .addColumn("Terr. Pop (M)", "class='target'", "{0,number,#,###.0}", "class='targetRight'", true)
      .addColumn("Terr. Literacy", "class='target'", "{0,number,#.0}%", "class='targetRight'", true)
      .addColumn("Terr. GDP ($M PPP)", "class='target'", "{0,number,#,##0}", "class='targetRight'", true) 
      .addColumn("Currencies (2006...)", "class='target'", null, "class='target'", true);
      for (Iterator<String> it = territoryTypes.iterator(); it.hasNext();) {
        String header = it.next();
        if (header.equals("calendar")) header = "calendar (+gregorian)";
        tablePrinter.addColumn(header).setHeaderAttributes("class='target'").setCellAttributes("class='target'").setSpanRows(true);
      }
      
      tablePrinter.addColumn("Language", "class='target'", null, "class='target'", false)
      .addColumn("Code", "class='target'", "<a href=\"language_territory_information.html#{0}\">{0}</a>", "class='target'", false)
      .addColumn("Lang. Pop.%", "class='target'", "{0,number,#.0}%", "class='targetRight'", true).setSortAscending(false).setSortPriority(1)
      .addColumn("Lang. Liter\u00ADacy", "class='target'", "{0,number,#.0}%", "class='targetRight'", true)
      .addColumn("Report Bug", "class='target'", null, "class='target'", false)
      ;
      
      for (String territoryName : territoryLanguageData.keySet()) {
        Map<String,Object>results = territoryLanguageData.get(territoryName);
        Set<Pair<Double,Pair<Double,String>>> language = (Set<Pair<Double,Pair<Double,String>>>)results.get("language");
        double population = Double.parseDouble((String)results.get("population"))/1000000;
        double gdp = Double.parseDouble((String)results.get("gdp"))/1000000;
        double territoryLiteracy = Double.parseDouble((String)results.get("literacyPercent"));
        String territoryCode = (String)results.get("code");
        
        Map worldData = (Map) territoryData.get(getName(CLDRFile.TERRITORY_NAME, "001", false));
        Map countryData = (Map) territoryData.get(getName(CLDRFile.TERRITORY_NAME, territoryCode, false)); 
        
        if (language != null) for (Pair<Double,Pair<Double,String>> languageCodePair : language) {
          double languagePopulation = languageCodePair.first;
          double languageliteracy = languageCodePair.second.first;
          String languageCode = languageCodePair.second.second;
          
          if (Double.isNaN(languageliteracy)) {
            languageliteracy = territoryLiteracy;
          }
//        Comparable[] items = new Comparable[]{
//        territoryName,
//        territoryCode,
//        population,
//        territoryLiteracy,
//        gdp,
//        getCurrencyNames(territoryCode),
//        english.getName(languageCode, false),
//        languageCode,
//        languagePopulation,
//        languageliteracy,
//        addBug(1217, "<i>bug</i>", "<email>", "fix info for " + english.getName(languageCode, false) + " (" + languageCode + ")"
//        + " in " + territoryName + " (" + territoryCode + ")",
//        "<fixed data for territory, plus references>"),
//        };
//        tablePrinter.addRow(items);
          tablePrinter.addRow()
          .addCell(territoryName)
          .addCell(territoryCode)
          .addCell(population)
          .addCell(territoryLiteracy)
          .addCell(gdp)
          .addCell(getCurrencyNames(territoryCode));
           
          addOtherCountryData(tablePrinter, worldData, countryData);
          
          tablePrinter
          .addCell(english.getName(languageCode, false))
          .addCell(languageCode)
          .addCell(languagePopulation)
          .addCell(languageliteracy)
          .addCell(addBug(1217, "<i>bug</i>", "<email>", "fix info for " + english.getName(languageCode, false) + " (" + languageCode + ")"
              + " in " + territoryName + " (" + territoryCode + ")",
          "<fixed data for territory, plus references>"))
          .finishRow();
        }
        
//      Comparable[] items = new Comparable[]{
//      territoryName,
//      territoryCode,
//      population,
//      Double.parseDouble((String)results.get("literacyPercent")),
//      gdp,
//      getCurrencyNames(territoryCode),
//      addBug(1217, "<i>add new</i>", "<email>", "add language to " + territoryName + "(" + territoryCode + ")", "<language, speaker pop. and literacy in territory, plus references>"),
//      "",
//      0.0d,
//      0.0d,
//      ""
//      };
//      tablePrinter.addRow(items);
        tablePrinter.addRow()
        .addCell(territoryName)
        .addCell(territoryCode)
        .addCell(population)
        .addCell(territoryLiteracy)
        .addCell(gdp)
        .addCell(getCurrencyNames(territoryCode));
        
        addOtherCountryData(tablePrinter, worldData, countryData);

        tablePrinter
        .addCell(addBug(1217, "<i>add new</i>", "<email>", "add language to " + territoryName + "(" + territoryCode + ")", "<language, speaker pop. and literacy in territory, plus references>"))
        .addCell("")
        .addCell(0.0d)
        .addCell(0.0d)
        .addCell("")
        .finishRow();
        
        
      }
      String value = tablePrinter.toTable();
      pw2.println(value);
      pw2.close();
    }
    
    private void addOtherCountryData(TablePrinter tablePrinter, Map worldData, Map countryData) {
      for (Iterator<String> it2 = territoryTypes.iterator(); it2.hasNext();) {
        String type = it2.next();
        Set worldResults = (Set) worldData.get(type);
        Set territoryResults = null;
        if (countryData != null) {
          territoryResults = (Set) countryData.get(type);
        }
        if (territoryResults == null) {
          territoryResults = worldResults;
        }
        String out = "";
        if (territoryResults != null) {
          out = territoryResults + "";
          out = out.substring(1, out.length() - 1); // remove [ and ]
        }
        tablePrinter.addCell(out);
      }
    }
    
    private String getCurrencyNames(String territoryCode) {
      StringBuilder buffer = new StringBuilder();
      for (String code : territoriesToModernCurrencies.getAll(territoryCode)) {
        if (buffer.length() != 0) buffer.append("<br>");
        buffer.append(getName(CLDRFile.CURRENCY_NAME, code, false));
      }
      return buffer.toString();
    }
    
    
    private void addCharSubstitution(String value, String substitute) {
      if (substitute.equals(value))
        return;
      LinkedHashSet already = (LinkedHashSet) charSubstitutions.get(value);
      if (already == null)
        charSubstitutions.put(value, already = new LinkedHashSet(0));
      already.add(substitute);
      Log.logln(hex(value, " ") + "; " + hex(substitute, " "));
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
        System.out.println(territory + "\t" + english.getName(english.TERRITORY_NAME, territory, false) + "\t" + parent + "\t" + english.getName(english.TERRITORY_NAME, parent, false));
      }
    }
    
    private void gather(String item, Map territory_parent) {
      Collection containedByItem = (Collection) group_contains.get(item);
      if (containedByItem == null)
        return;
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
        if (s == null)
          territoryData.put(territory, s = new TreeMap());
        Set ss = (Set) s.get(type);
        if (ss == null)
          s.put(type, ss = new TreeSet());
        ss.add(info);
      }
    }
    
    public void showCalendarData(PrintWriter pw0) throws IOException {
      PrintWriter pw = new PrintWriter(new FormattedFileWriter(pw0, "Other Territory Data", null));
      pw.println("<tr><th class='source'>Territory</th>");
      for (Iterator<String> it = territoryTypes.iterator(); it.hasNext();) {
        String header = (String) it.next();
        if (header.equals("calendar")) header = "calendar (+gregorian)";
        pw.println("<th class='target'>" + header + "</th>");
      }
      pw.println("</tr>");
      
      String worldName = getName(CLDRFile.TERRITORY_NAME, "001", false);
      Map worldData = (Map) territoryData.get(worldName);
      for (Iterator it = territoryData.keySet().iterator(); it.hasNext();) {
        String country = (String) it.next();
        if (country.equals(worldName))
          continue;
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
          out = results + "";
          out = out.substring(1, out.length() - 1); // remove [ and ]
        }
        pw.println("<td class='" + target + "'>" + out + "</td>");
      }
      pw.println("</tr>");
    }
    
    public void showCorrespondances() {
      // show correspondances between language and script
      Map name_script = new TreeMap();
      for (Iterator it = sc.getAvailableCodes("script").iterator(); it.hasNext();) {
        String script = (String) it.next();
        String name = (String) english.getName(CLDRFile.SCRIPT_NAME, script, false);
        if (name == null)
          name = script;
        name_script.put(name, script);
        /*				source == CLDRFile.TERRITORY_NAME && target == CLDRFile.LANGUAGE_NAME ? territory_languages
         : source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.TERRITORY_NAME ? language_territories
         : source == CLDRFile.SCRIPT_NAME && target == CLDRFile.LANGUAGE_NAME ? script_languages
         : source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.SCRIPT_NAME ? language_scripts
         */}
      String delimiter = "\\P{L}+";
      Map name_language = new TreeMap();
      for (Iterator it = sc.getAvailableCodes("language").iterator(); it.hasNext();) {
        String language = (String) it.next();
        String names = english.getName(CLDRFile.LANGUAGE_NAME, language, false);
        if (names == null)
          names = language;
        name_language.put(names, language);
      }
      for (Iterator it = sc.getAvailableCodes("language").iterator(); it.hasNext();) {
        String language = (String) it.next();
        String names = english.getName(CLDRFile.LANGUAGE_NAME, language, false);
        if (names == null)
          names = language;
        String[] words = names.split(delimiter);
        if (words.length > 1) {
          //System.out.println(names);
        }
        for (int i = 0; i < words.length; ++i) {
          String name = words[i];
          String script = (String) name_script.get(name);
          if (script != null) {
            Set langSet = (Set) script_languages.get(script);
            if (langSet != null && langSet.contains(language))
              System.out.print("*");
            System.out.println("\t" + name + " [" + language + "]\t=> " + name + " [" + script + "]");
          } else {
            String language2 = (String) name_language.get(name);
            if (language2 != null && !language.equals(language2)) {
              Set langSet = (Set) language_scripts.get(language);
              if (langSet != null)
                System.out.print("*");
              System.out.print("?\tSame script?\t + " + getName(CLDRFile.LANGUAGE_NAME, language, false) + "\t & " + getName(CLDRFile.LANGUAGE_NAME, language2, false));
              langSet = (Set) language_scripts.get(language2);
              if (langSet != null)
                System.out.print("*");
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
      PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, "Detailed Territory-Currency Information", 
          "<p>The following table shows when currencies were in use in different countries. " +
          "See also <a href='#format_info'>Decimal Digits and Rounding</a>. " +
          "To correct any information here, please file a " +
          addBug(1274, "bug", "<email>", "Currency Bug", "<currency, country, and references supporting change>") +
      ".</p>"));
      //doTitle(pw, "Territory \u2192 Currency");
      pw.println("<tr><th class='source'>Territory</th><th class='target'>From</th><th class='target'>To</th><th class='target'>Currency</th></tr>");
      for (Iterator it = territory_currency.keySet().iterator(); it.hasNext();) {
        String territory = (String) it.next();
        Set info = (Set) territory_currency.get(territory);
        pw.println("<tr><td class='source' rowSpan='" + info.size() + "'>" + territory + "</td>");
        boolean first = true;
        for (Iterator it2 = info.iterator(); it2.hasNext();) {
          String[] items = (String[]) it2.next();
          if (first)
            first = false;
          else
            pw.println("<tr>");
          pw.println("<td class='target'>" + items[0] + "</td><td class='target'>" + items[1] + "</td><td class='target'>" + items[2] + "</td></tr>");
        }
      }
      //doFooter(pw);
      //pw.close();
      //pw = new PrintWriter(new FormattedFileWriter(index, "Currency Format Info", null));
      pw.write("</table></div>");
      pw.write("<h2><a name='format_info'>Decimal Digits and Rounding</a></h2>");
      pw.write("<p>This table shows the digit rounding for the currency, plus the countries where it is or was in use. " +
          "Countries where the currency is not in current use are marked by italics. " +
          "If the currency uses 'nickel rounding' in retail formatting, the digits are followed by '(5)'." +
          "</p>");
      pw.write("<div align='center'><table>");
      
      //doTitle(pw, "Currency Format Info");
      pw.println("<tr><th class='source'>Currency</th><th class='target'>Digits</th><th class='target'>Countries</th></tr>");
      Set currencyList = new TreeSet(col);
      currencyList.addAll(currency_fractions.keySet());
      currencyList.addAll(currency_territory.keySet());
      
      for (Iterator it = currencyList.iterator(); it.hasNext();) {
        String currency = (String) it.next();
        String fractions = (String) currency_fractions.get(currency);
        if (fractions == null)
          fractions = defaultDigits;
        Set territories = (Set) currency_territory.get(currency);
        pw.print("<tr><td class='source'>" + currency + "</td><td class='target'>" + fractions + "</td><td class='target'>");
        if (territories != null) {
          boolean first = true;
          for (Iterator it2 = territories.iterator(); it2.hasNext();) {
            if (first)
              first = false;
            else
              pw.print(", ");
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
      PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, "Aliases", null));
      
      //doTitle(pw, "Aliases");
      pw.println("<tr><th class='source'>" + "Type" + "</th><th class='source'>" + "Code" + "</th><th class='target'>" + "Substitute (if avail)" + "</th></tr>");
      for (Iterator it = aliases.iterator(); it.hasNext();) {
        String[] items = (String[]) it.next();
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
      PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, "Windows \u2192 Tzid", null));
      //doTitle(pw, "Windows \u2192 Tzid");
      for (Iterator it = windows_tzid.keySet().iterator(); it.hasNext();) {
        String source = (String) it.next();
        String target = (String) windows_tzid.get(source);
        pw.println("<tr><td class='source'>" + source + "</td><td class='target'>" + target + "</td></tr>");
      }
      //doFooter(pw);
      pw.close();
    }
    
    //<info iso4217="ADP" digits="0" rounding="0"/>
    
    public void printContains(PrintWriter index) throws IOException {
      String title = "Territory Containment (UN M.49)";
      
      PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, title, null));
      //doTitle(pw, title);
      List<String[]> rows = new ArrayList<String[]>();
      printContains3("001", rows, new ArrayList());
      TablePrinter tablePrinter = new TablePrinter()
      .addColumn("World","class='source'",null,"class='z0'",true).setSortPriority(0)
      .addColumn("Continent","class='source'",null,"class='z1'",true).setSortPriority(1)
      .addColumn("Subcontinent","class='source'",null,"class='z2'",true).setSortPriority(2)
      .addColumn("Country (Territory)","class='source'",null,"class='z3'",true).setSortPriority(3)
      .addColumn("Time Zone","class='source'",null,"class='z4'",true).setSortPriority(4);
      String[][] flatData = rows.toArray(string2ArrayPattern);
      pw.println(tablePrinter.addRows(flatData).toTable());
      pw.close();
    }
    
    static String[] stringArrayPattern = new String[0];
    static String[][] string2ArrayPattern = new String[0][];
    
    private void printContains3(String start, List<String[]> rows, ArrayList<String> currentRow) {
      int len = currentRow.size();
      if (len > 3) {
        return; // skip long items
      }
      currentRow.add(getName(CLDRFile.TERRITORY_NAME, start, false));
      Collection<String> contains = (Collection<String>) group_contains.get(start);
      if (contains == null) {
        contains = (Collection<String>) sc.getCountryToZoneSet().get(start);
        currentRow.add("");
        if (contains == null) {
          currentRow.set(len+1,"???");
          rows.add(currentRow.toArray(stringArrayPattern));          
        } else {
          for (String item : contains) {
            currentRow.set(len+1,item);
            rows.add(currentRow.toArray(stringArrayPattern));
          }
        }
        currentRow.remove(len+1);
      } else {
        for (String item : contains) {
          if (territoryAliases.keySet().contains(item)) {
            continue;
          }
          printContains3(item, rows, currentRow);
        }        
      }
      currentRow.remove(len);
    }
    
    public void printCharacters(PrintWriter index) throws IOException {
      String title = "Character Fallback Substitutions";
      
      PrintWriter pw = new PrintWriter(new FormattedFileWriter(index, title, null));
      //doTitle(pw, title);
      pw.println("<tr><th colSpan='3'>Substitute for character (if not in repertoire)</th><th colSpan='4'>The following (in priority order, first string that <i>is</i> in repertoire)</th></tr>");
      UnicodeSet chars = new UnicodeSet("[:NFKC_QuickCheck=N:]");
      for (com.ibm.icu.text.UnicodeSetIterator it = new com.ibm.icu.text.UnicodeSetIterator(chars); it.next();) {
        String value = it.getString();
        addCharSubstitution(value, Normalizer.normalize(value, Normalizer.NFC));
        addCharSubstitution(value, Normalizer.normalize(value, Normalizer.NFKC));
      }
      int[] counts = new int[4];
      for (Iterator it = charSubstitutions.keySet().iterator(); it.hasNext();) {
        String value = (String) it.next();
        LinkedHashSet substitutes = (LinkedHashSet) charSubstitutions.get(value);
        String nfc = Normalizer.normalize(value, Normalizer.NFC);
        String nfkc = Normalizer.normalize(value, Normalizer.NFKC);
        
        String sourceTag = "<td class='source'>";
        if (substitutes.size() > 1) {
          sourceTag = "<td class='source' rowSpan='" + substitutes.size() + "'>";
        }
        boolean first = true;
        for (Iterator it2 = substitutes.iterator(); it2.hasNext();) {
          String substitute = (String) it2.next();
          String type = "Explicit";
          String targetTag = "<td class='target3'>";
          if (substitute.equals(nfc)) {
            type = "NFC";
            targetTag = "<td class='target'>";
            counts[2]++;
          } else if (substitute.equals(nfkc)) {
            type = "NFKC";
            targetTag = "<td class='target4'>";
            counts[3]++;
          } else {
            counts[0]++;
          }
          pw.println("<tr>"
              + (!first ? "" : sourceTag + hex(value, ", ") + "</td>" + sourceTag + TransliteratorUtilities.toHTML.transliterate(value) + "</td>" + sourceTag + UCharacter.getName(value, ", ")
                  + "</td>") + targetTag + type + "</td>" + targetTag + hex(substitute, ", ") + "</td>" + targetTag + TransliteratorUtilities.toHTML.transliterate(substitute) + "</td>" + targetTag
                  + UCharacter.getName(substitute, ", ") + "</td></tr>");
          first = false;
        }
      }
      //doFooter(pw);
      pw.close();
      for (int i = 0; i < counts.length; ++i) {
        System.out.println("Count\t" + i + "\t" + counts[i]);
      }
    }
    
    public static String hex(String s, String separator) {
      StringBuffer result = new StringBuffer();
      int cp;
      for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
        cp = UTF16.charAt(s, i);
        if (i != 0)
          result.append(separator);
        result.append(com.ibm.icu.impl.Utility.hex(cp));
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
      if (!isFirst)
        pw.print(lead);
      int count = getTotalContainedItems(start, depth);
      pw.print("<td class='z" + depth + "' rowSpan='" + count + "'>" + name + "</td>"); // colSpan='" + (5 - depth) + "' 
      if (depth == 4)
        pw.println("</tr>");
      Collection contains = getContainedCollection(start, depth);
      if (contains != null) {
        Collection contains2 = new TreeSet(territoryNameComparator);
        contains2.addAll(contains);
        boolean first = true;
        for (Iterator it = contains2.iterator(); it.hasNext();) {
          String item = (String) it.next();
          printContains2(pw, lead, item, depth + 1, first); //  + "<td>&nbsp;</td>"
          first = false;
        }
      }
    }
    
    private int getTotalContainedItems(String start, int depth) {
      Collection c = getContainedCollection(start, depth);
      if (c == null)
        return 1;
      int sum = 0;
      for (Iterator it = c.iterator(); it.hasNext();) {
        sum += getTotalContainedItems((String) it.next(), depth + 1);
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
        if (table == CLDRFile.SCRIPT_NAME)
          missingItems.removeAll(language_scripts.keySet());
        if (table == CLDRFile.TERRITORY_NAME)
          missingItems.removeAll(language_territories.keySet());
      } else {
        throw new IllegalArgumentException("Illegal code");
      }
      Set missingItemsNamed = new TreeSet(col);
      for (Iterator it = missingItems.iterator(); it.hasNext();) {
        String item = (String) it.next();
        List data = sc.getFullData(type, item);
        if (data.get(0).equals("PRIVATE USE"))
          continue;
        if (data.size() < 3)
          continue;
        if (!"".equals(data.get(2)))
          continue;
        
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
      Map data = source == CLDRFile.TERRITORY_NAME && target == CLDRFile.LANGUAGE_NAME ? territory_languages
          : source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.TERRITORY_NAME ? language_territories : source == CLDRFile.SCRIPT_NAME && target == CLDRFile.LANGUAGE_NAME ? script_languages
              : source == CLDRFile.LANGUAGE_NAME && target == CLDRFile.SCRIPT_NAME ? language_scripts : null;
      // transform into names, and sort
      Map territory_languageNames = new TreeMap(col);
      for (Iterator it = data.keySet().iterator(); it.hasNext();) {
        String territory = (String) it.next();
        String territoryName = getName(source, territory, true);
        Set s = (Set) territory_languageNames.get(territoryName);
        if (s == null)
          territory_languageNames.put(territoryName, s = new TreeSet(col));
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
      if (oldcode.contains(" ")) {
        String[] result = oldcode.split("\\s+");
        for (int i = 0; i < result.length; ++i) {
          result[i] = getName(type, result[i], codeFirst);
        }
        return Utility.join(Arrays.asList(result), ", ");
      } else {
        int pos = oldcode.indexOf('*');
        String code = pos < 0 ? oldcode : oldcode.substring(0, pos);
        String ename = english.getName(type, code, false);
        return codeFirst ? "[" + oldcode + "]\t" + (ename == null ? code : ename) : (ename == null ? code : ename) + "\t[" + oldcode + "]";
      }
    }
    
    private String getName(String locale, boolean codeFirst) {
      String ename = english.getName(locale, false);
      return codeFirst ? "[" + locale + "]\t" + (ename == null ? locale : ename) : (ename == null ? locale : ename) + "\t[" + locale + "]";
    }
    
    Comparator territoryNameComparator = new Comparator() {
      public int compare(Object o1, Object o2) {
        return col.compare(getName(CLDRFile.TERRITORY_NAME, (String) o1, false), getName(CLDRFile.TERRITORY_NAME, (String) o2, false));
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
      Set territories = (Set) language_territories.get(language);
      for (Iterator it2 = territories.iterator(); it2.hasNext();) {
        Object territory = it2.next();
        Set languages = (Set) territory_languages.get(territory);
        if (languages == null)
          territory_languages.put(territory, languages = new TreeSet(col));
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
      if (s == null)
        key_value.put(key, s = new TreeSet(col));
      s.addAll(Arrays.asList(values.split(value_delimiter)));
    }
  }
}