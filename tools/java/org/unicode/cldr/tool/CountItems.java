/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.IsoCurrencyParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Pair;
import com.ibm.icu.dev.test.util.Relation;

import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.IsoRegionData;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.ZoneInflections;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.IsoCurrencyParser.Data;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeMapIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * Simple program to count the amount of data in CLDR. Internal Use.
 */
public class CountItems {

  static final String needsTranslationString = "America/Buenos_Aires " // America/Rio_Branco
      + " America/Manaus America/Belem "
      + " America/Campo_Grande America/Sao_Paulo "
      + " Australia/Perth Australia/Darwin Australia/Brisbane Australia/Adelaide Australia/Sydney Australia/Hobart "
      + " America/Vancouver America/Edmonton America/Regina America/Winnipeg America/Toronto America/Halifax America/St_Johns "
      + " Asia/Jakarta "
      + " America/Tijuana America/Hermosillo America/Chihuahua America/Mexico_City "
      + " Europe/Moscow Europe/Kaliningrad Europe/Moscow Asia/Yekaterinburg Asia/Novosibirsk Asia/Yakutsk Asia/Vladivostok"
      + " Pacific/Honolulu America/Indiana/Indianapolis America/Anchorage "
      + " America/Los_Angeles America/Phoenix America/Denver America/Chicago America/Indianapolis"
      + " America/New_York";

  static final Map country_map = CollectionUtilities.asMap(new String[][] {
      { "AQ", "http://www.worldtimezone.com/time-antarctica24.php" },
      { "AR", "http://www.worldtimezone.com/time-south-america24.php" },
      { "AU", "http://www.worldtimezone.com/time-australia24.php" },
      { "BR", "http://www.worldtimezone.com/time-south-america24.php" },
      { "CA", "http://www.worldtimezone.com/time-canada24.php" },
      { "CD", "http://www.worldtimezone.com/time-africa24.php" },
      { "CL", "http://www.worldtimezone.com/time-south-america24.php" },
      { "CN", "http://www.worldtimezone.com/time-cis24.php" },
      { "EC", "http://www.worldtimezone.com/time-south-america24.php" },
      { "ES", "http://www.worldtimezone.com/time-europe24.php" },
      { "FM", "http://www.worldtimezone.com/time-oceania24.php" },
      { "GL", "http://www.worldtimezone.com/index24.php" },
      { "ID", "http://www.worldtimezone.com/time-asia24.php" },
      { "KI", "http://www.worldtimezone.com/time-oceania24.php" },
      { "KZ", "http://www.worldtimezone.com/time-cis24.php" },
      { "MH", "http://www.worldtimezone.com/time-oceania24.php" },
      { "MN", "http://www.worldtimezone.com/time-cis24.php" },
      { "MX", "http://www.worldtimezone.com/index24.php" },
      { "MY", "http://www.worldtimezone.com/time-asia24.php" },
      { "NZ", "http://www.worldtimezone.com/time-oceania24.php" },
      { "PF", "http://www.worldtimezone.com/time-oceania24.php" },
      { "PT", "http://www.worldtimezone.com/time-europe24.php" },
      { "RU", "http://www.worldtimezone.com/time-russia24.php" },
      { "SJ", "http://www.worldtimezone.com/index24.php" },
      { "UA", "http://www.worldtimezone.com/time-cis24.php" },
      { "UM", "http://www.worldtimezone.com/time-oceania24.php" },
      { "US", "http://www.worldtimezone.com/time-usa24.php" },
      { "UZ", "http://www.worldtimezone.com/time-cis24.php" }, });

  /**
   * Count the data.
   * @throws IOException
   */
  public static void main(String[] args) throws Exception {
    double deltaTime = System.currentTimeMillis();
    try {
      String methodName = System.getProperty("method");
      CldrUtility.callMethod(methodName, CountItems.class);
      //countItems();

      //getZoneEquivalences();
      //if (true) return;

      //getPatternBlocks();
      //showExemplars();
      //genSupplementalZoneData(false);
      //showZoneInfo();
    } finally {
      deltaTime = System.currentTimeMillis() - deltaTime;
      System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
      System.out.println("Done");
    }
  }

  public static void getZoneEquivalences() throws IOException, ParseException {
    //    	String tzid = "America/Argentina/ComodRivadavia";
    //    	TimeZone tz = TimeZone.getTimeZone(tzid);
    //    	int offset = tz.getOffset(new Date().getTime());
    //    	System.out.println(tzid + ":\t" + offset);
    //    	System.out.println("in available? " + Arrays.asList(TimeZone.getAvailableIDs()).contains(tzid));
    //    	System.out.println(new TreeSet(Arrays.asList(TimeZone.getAvailableIDs())));

    Set needsTranslation = new TreeSet(Arrays.asList(needsTranslationString
        .split("[,]?\\s+")));
    Set singleCountries = new TreeSet(
        Arrays
            .asList("Africa/Bamako America/Godthab America/Santiago America/Guayaquil     Asia/Shanghai Asia/Tashkent Asia/Kuala_Lumpur Europe/Madrid Europe/Lisbon Europe/London Pacific/Auckland Pacific/Tahiti"
                .split("\\s")));
    Set defaultItems = new TreeSet(
        Arrays
            .asList("Antarctica/McMurdo America/Buenos_Aires Australia/Sydney America/Sao_Paulo America/Toronto Africa/Kinshasa America/Santiago Asia/Shanghai America/Guayaquil Europe/Madrid Europe/London America/Godthab Asia/Jakarta Africa/Bamako America/Mexico_City Asia/Kuala_Lumpur Pacific/Auckland Europe/Lisbon Europe/Moscow Europe/Kiev America/New_York Asia/Tashkent Pacific/Tahiti Pacific/Kosrae Pacific/Tarawa Asia/Almaty Pacific/Majuro Asia/Ulaanbaatar Arctic/Longyearbyen Pacific/Midway"
                .split("\\s")));

    StandardCodes sc = StandardCodes.make();
    Collection codes = sc.getGoodAvailableCodes("tzid");
    TreeSet extras = new TreeSet();
    Map m = sc.getZoneLinkNew_OldSet();
    for (String code : (Collection<String>) codes) {
      Collection s = (Collection) m.get(code);
      if (s == null)
        continue;
      extras.addAll(s);
    }
    extras.addAll(codes);
    Set icu4jTZIDs = new TreeSet(Arrays.asList(TimeZone.getAvailableIDs()));
    Set diff2 = new TreeSet(icu4jTZIDs);
    diff2.removeAll(extras);
    System.out.println("icu4jTZIDs - StandardCodes: " + diff2);
    diff2 = new TreeSet(extras);
    diff2.removeAll(icu4jTZIDs);
    System.out.println("StandardCodes - icu4jTZIDs: " + diff2);
    ArrayComparator ac = new ArrayComparator(new Comparator[] {
        ArrayComparator.COMPARABLE, ArrayComparator.COMPARABLE,
        ArrayComparator.COMPARABLE });
    Map zone_countries = sc.getZoneToCounty();

    TreeSet country_inflection_names = new TreeSet(ac);
    PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY,
        "inflections.txt");

    TreeMap<Integer, TreeSet<String>> minOffsetMap = new TreeMap<Integer, TreeSet<String>>();
    TreeMap<Integer, TreeSet<String>> maxOffsetMap = new TreeMap<Integer, TreeSet<String>>();

    for (Iterator it = codes.iterator(); it.hasNext();) {
      String zoneID = (String) it.next();
      String country = (String) zone_countries.get(zoneID);
      TimeZone zone = TimeZone.getTimeZone(zoneID);
      ZoneInflections zip = new ZoneInflections(zone);
      out.println(zoneID + "\t" + zip);
      country_inflection_names.add(new Object[] { country, zip, zoneID });

      TreeSet<String> s = minOffsetMap.get(zip.getMinOffset());
      if (s == null)
        minOffsetMap.put(zip.getMinOffset(), s = new TreeSet<String>());
      s.add(zone.getID());

      s = maxOffsetMap.get(zip.getMaxOffset());
      if (s == null)
        maxOffsetMap.put(zip.getMaxOffset(), s = new TreeSet<String>());
      s.add(zone.getID());

    }
    System.out.println("Minimum Offset: " + minOffsetMap);
    System.out.println("Maximum Offset: " + maxOffsetMap);
    out.close();

    out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY,
        "modernTimezoneEquivalents.html");
    out.println("<html>" + "<head>"
        + "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>"
        + "<title>Modern Equivalent Timezones</title><style>");
    out.println("td.top,td.topr { background-color: #EEEEFF }");
    out.println("td.r,td.topr { text-align:right }");
    out
        .println("td.gap { font-weight:bold; border-top: 3px solid #0000FF; border-bottom: 3px solid #0000FF; background-color: #CCCCCC }");
    out
        .println("</style>"
            + "</head>"
            + "<body>"
            + "<table border='1' cellspacing='0' cellpadding='2' style='border-collapse: collapse'>");
    Tabber.HTMLTabber tabber1 = new Tabber.HTMLTabber();
    tabber1.setParameters(4, "class='r'");
    tabber1.setParameters(5, "class='r'");
    Tabber.HTMLTabber tabber2 = new Tabber.HTMLTabber();
    tabber2.setParameters(0, "class='top'");
    tabber2.setParameters(1, "class='top'");
    tabber2.setParameters(2, "class='top'");
    tabber2.setParameters(3, "class='top'");
    tabber2.setParameters(4, "class='topr'");
    tabber2.setParameters(5, "class='topr'");
    Tabber.HTMLTabber tabber3 = new Tabber.HTMLTabber();
    tabber3.setParameters(0, "class='gap'");
    tabber3.setParameters(1, "class='gap'");
    tabber3.setParameters(2, "class='gap'");
    tabber3.setParameters(3, "class='gap'");
    tabber3.setParameters(4, "class='gap'");
    tabber3.setParameters(5, "class='gap'");

    long minimumDate = ICUServiceBuilder.isoDateParse("2000-1-1T00:00:00Z")
        .getTime();
    out
        .println("<h1>Modern Equivalent Timezones: <a target='_blank' href='instructions.html'>Instructions</a></h1>");
    out.println(ShowData.dateFooter());
    out.println("<p>Zones identical after: "
        + ICUServiceBuilder.isoDateFormat(minimumDate) + "</p>");
    String lastCountry = "";
    ZoneInflections lastZip = null;
    ZoneInflections.OutputLong diff = new ZoneInflections.OutputLong(0);
    Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    TimezoneFormatter tzf = new TimezoneFormatter(cldrFactory, "en", true);
    Map country_zoneSet = sc.getCountryToZoneSet();
    boolean shortList = true;
    boolean first = true;
    int category = 1;
    Tabber tabber = tabber1;
    for (Iterator it = country_inflection_names.iterator(); it.hasNext();) {
      Object[] row = (Object[]) it.next();
      String country = (String) row[0];
      if (country.equals("001"))
        continue;
      if (shortList && ((Set) country_zoneSet.get(country)).size() < 2)
        continue;
      ZoneInflections zip = (ZoneInflections) row[1];
      String zoneID = (String) row[2];
      int zipComp = zip.compareTo(lastZip, diff);

      if (!country.equals(lastCountry)) {
        if (first)
          first = false;
        category = 1;
        subheader(out, tabber3);
      } else if (diff.value >= minimumDate) {
        //out.println(tabber.process("\tDiffers at:\t" + ICUServiceBuilder.isoDateFormat(diff.value)));
        tabber = tabber == tabber1 ? tabber2 : tabber1;
        ++category;
      }
      String zoneIDShown = zoneID;
      if (needsTranslation.contains(zoneID)) {
        zoneIDShown = "<b>" + zoneIDShown + "\u00B9</b>";
      }
      if (singleCountries.contains(zoneID)) {
        zoneIDShown = "<i>" + zoneIDShown + "</i> \u00B2";
      }
      if (defaultItems.contains(zoneID)) {
        zoneIDShown = "<span style='background-color: #FFFF00'>" + zoneIDShown
            + "</span> ?";
      }
      //if (country.equals(lastCountry) && diff.value >= minimumDate) System.out.print("X");
      String newCountry = country;
      String mapLink = (String) country_map.get(country);
      if (mapLink != null) {
        newCountry = "<a target='map' href='" + mapLink + "'>" + country
            + "</a>";
      }
      String minOffset = zip.formatHours(zip.getMinOffset(minimumDate));
      String maxOffset = zip.formatHours(zip.getMaxOffset(minimumDate));
      if (!icu4jTZIDs.contains(zoneID)) {
        minOffset = maxOffset = "??";
      }

      out.println(tabber.process(newCountry + "\t" + "<b>" + category + "</b>"
          + "\t" + zoneIDShown + "\t"
          + tzf.getFormattedZone(zoneID, "vvvv", minimumDate, false) + "\t"
          + minOffset + "\t" + maxOffset));
      lastCountry = country;
      lastZip = zip;
    }
    subheader(out, tabber3);
    out.println("</table>");
    out.println(ShowData.ANALYTICS);
    out.println("</body></html>");
    out.close();
  }

  private static void subheader(PrintWriter out, Tabber tabber) {
    //out.println("<tr><td colspan='6' class='gap'>&nbsp;</td></tr>");
    out.println(tabber.process("Cnty" + "\t" + "Grp" + "\t" + "ZoneID" + "\t"
        + "Formatted ID" + "\t" + "MaxOffset" + "\t" + "MinOffset"));
  }

  /**
   * 
   */
  private static void getPatternBlocks() {
    UnicodeSet patterns = new UnicodeSet("[:pattern_syntax:]");
    UnicodeSet unassigned = new UnicodeSet("[:unassigned:]");
    UnicodeSet punassigned = new UnicodeSet(patterns).retainAll(unassigned);
    UnicodeMap blocks = ICUPropertyFactory.make().getProperty("block")
        .getUnicodeMap();
    blocks.setMissing("<Reserved-Block>");
    //            blocks.composeWith(new UnicodeMap().putAll(new UnicodeSet(patterns).retainAll(unassigned),"<reserved>"), 
    //                    new UnicodeMap.Composer() {
    //                public Object compose(int codePoint, Object a, Object b) {
    //                    if (a == null) {
    //                        return b;
    //                    }
    //                    if (b == null) {
    //                        return a;
    //                    }
    //                    return a.toString() + " " + b.toString();
    //                }});
    for (UnicodeMapIterator it = new UnicodeMapIterator(blocks); it
        .nextRange();) {
      UnicodeSet range = new UnicodeSet(it.codepoint, it.codepointEnd);
      boolean hasPat = range.containsSome(patterns);
      String prefix = !hasPat ? "Not-Syntax"
          : !range.containsSome(unassigned) ? "Closed" : !range
              .containsSome(punassigned) ? "Closed2" : "Open";

      boolean show = (prefix.equals("Open") || prefix.equals("Closed2"));

      if (show)
        System.out.println();
      System.out.println(prefix + "\t" + range + "\t" + it.value);
      if (show) {
        System.out.println(new UnicodeMap().putAll(unassigned, "<reserved>")
            .putAll(punassigned, "<reserved-for-syntax>").setMissing(
                "<assigned>").putAll(range.complement(), null));
      }
    }
  }

  /**
   * @throws IOException
   * 
   */
  private static void showExemplars() throws IOException {
    PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY,
        "fixed_exemplars.txt");
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    Set locales = cldrFactory.getAvailable();
    for (Iterator it = locales.iterator(); it.hasNext();) {
      System.out.print('.');
      String locale = (String) it.next();
      CLDRFile cldrfile = cldrFactory.make(locale, false);
      String v = cldrfile
          .getStringValue("//ldml/characters/exemplarCharacters");
      if (v == null)
        continue;
      UnicodeSet exemplars = new UnicodeSet(v);
      if (exemplars.size() != 0 && exemplars.size() < 500) {
        Collator col = Collator.getInstance(new ULocale(locale));
        Collator spaceCol = Collator.getInstance(new ULocale(locale));
        spaceCol.setStrength(col.PRIMARY);
        out.println(locale + ":\t\u200E" + v + '\u200E');
        //                String fixedFull = CollectionUtilities.prettyPrint(exemplars, col, false);
        //                System.out.println(" =>\t" + fixedFull);
        //                verifyEquality(exemplars, new UnicodeSet(fixedFull));
        String fixed = new PrettyPrinter()
        .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
        .setSpaceComparator(spaceCol != null ? spaceCol : Collator.getInstance(ULocale.ROOT)
                .setStrength2(Collator.PRIMARY))
                .setCompressRanges(true)
                .format(exemplars);
        out.println(" =>\t\u200E" + fixed + '\u200E');

        verifyEquality(exemplars, new UnicodeSet(fixed));
        out.flush();
      }
    }
    out.close();
  }

  /**
   * 
   */
  private static void verifyEquality(UnicodeSet exemplars, UnicodeSet others) {
    if (others.equals(exemplars))
      return;
    System.out.println("FAIL\ta-b\t"
        + new UnicodeSet(exemplars).removeAll(others));
    System.out.println("\tb-a\t" + new UnicodeSet(others).removeAll(exemplars));
  }

  /**
   * 
   */
  public static void generateCurrencyItems() {
    IsoCurrencyParser isoCurrencyParser = IsoCurrencyParser.getInstance();
    Relation<String, Data> codeList = isoCurrencyParser.getCodeList();
    StringBuffer list = new StringBuffer();
    for (Iterator it = codeList.keySet().iterator(); it.hasNext();) {
      //String lastField = (String) it.next(); 
      //String zone = (String) fullMap.get(lastField);    
      String currencyCode = (String) it.next();
      Set<Data> dataSet = codeList.getAll(currencyCode);
      boolean first = true;
      for (Data data : dataSet) {
        if (first) {
          System.out.print(currencyCode);
          first = false;
        }
        System.out.println("\t" + data);
      }

      if (list.length() != 0)
        list.append(" ");
      list.append(currencyCode);

    }
    System.out.println();
    String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t\t";
    // "((?:[-+_A-Za-z0-9]+[/])+[A-Za-z0-9])[-+_A-Za-z0-9]*"
    String broken = CldrUtility.breakLines(list.toString(), sep, Pattern.compile(
        "([A-Z])[A-Z][A-Z]").matcher(""), 80);
    assert (list.toString().equals(broken.replace(sep, " ")));
    System.out.println("\t\t\t<!-- currency version "
        + isoCurrencyParser.getVersion() + " -->");
    System.out.println("\t\t\t<variable id=\"$currency\" type=\"choice\">"
        + broken + CldrUtility.LINE_SEPARATOR + "\t\t\t</variable>");
    Set<String> isoTextFileCodes = StandardCodes.make().getAvailableCodes(
        "currency");
    Set temp = new TreeSet(codeList.keySet());
    temp.removeAll(isoTextFileCodes);
    if (temp.size() != 0) {
      throw new IllegalArgumentException("Missing from ISO4217.txt file: " + temp);
    }
  }

  public static void genSupplementalZoneData() throws IOException {
    genSupplementalZoneData(false);
  }

  public static void genSupplementalZoneData(boolean skipUnaliased)
      throws IOException {
    RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance();
    col.setNumericCollation(true);
    StandardCodes sc = StandardCodes.make();
    Map<String, String> zone_country = sc.getZoneToCounty();
    Map country_zone = sc.getCountryToZoneSet();
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    CLDRFile english = cldrFactory.make("en", true);

    writeZonePrettyPath(col, zone_country, english);
    writeMetazonePrettyPath();

    Map old_new = sc.getZoneLinkold_new();
    Map new_old = new TreeMap();

    for (Iterator it = old_new.keySet().iterator(); it.hasNext();) {
      String old = (String) it.next();
      String newOne = (String) old_new.get(old);
      Set oldSet = (Set) new_old.get(newOne);
      if (oldSet == null)
        new_old.put(newOne, oldSet = new TreeSet());
      oldSet.add(old);
    }
    Map fullMap = new TreeMap(col);
    for (Iterator it = zone_country.keySet().iterator(); it.hasNext();) {
      String zone = (String) it.next();
      String defaultName = TimezoneFormatter.getFallbackName(zone);
      Object already = fullMap.get(defaultName);
      if (already != null)
        System.out.println("CONFLICT: " + already + ", " + zone);
      fullMap.put(defaultName, zone);
    }
    //fullSet.addAll(zone_country.keySet());
    //fullSet.addAll(new_old.keySet());

    System.out
        .println("<!-- Generated by org.unicode.cldr.tool.CountItems -->");
    System.out.println("<supplementalData>");
    System.out.println("\t<timezoneData>");
    System.out.println();

    Set multizone = new TreeSet();
    for (Iterator it = country_zone.keySet().iterator(); it.hasNext();) {
      String country = (String) it.next();
      Set zones = (Set) country_zone.get(country);
      if (zones != null && zones.size() != 1)
        multizone.add(country);
    }

    System.out.println("\t\t<zoneFormatting multizone=\""
        + toString(multizone, " ") + "\"" + " tzidVersion=\""
        + sc.getZoneVersion() + "\"" + ">");

    Set orderedSet = new TreeSet(col);
    orderedSet.addAll(zone_country.keySet());
    orderedSet.addAll(sc.getDeprecatedZoneIDs());
    StringBuffer tzid = new StringBuffer();

    for (Iterator it = orderedSet.iterator(); it.hasNext();) {
      //String lastField = (String) it.next(); 
      //String zone = (String) fullMap.get(lastField);    
      String zone = (String) it.next();
      if (tzid.length() != 0)
        tzid.append(' ');
      tzid.append(zone);

      String country = (String) zone_country.get(zone);
      if (country == null)
        continue; // skip deprecated

      Set aliases = (Set) new_old.get(zone);
      if (aliases != null) {
        aliases = new TreeSet(aliases);
        aliases.remove(zone);
      }
      if (skipUnaliased)
        if (aliases == null || aliases.size() == 0)
          continue;

      System.out.println("\t\t\t<zoneItem"
          + " type=\""
          + zone
          + "\""
          + " territory=\""
          + country
          + "\""
          + (aliases != null && aliases.size() > 0 ? " aliases=\""
              + toString(aliases, " ") + "\"" : "") + "/>");
    }

    System.out.println("\t\t</zoneFormatting>");
    System.out.println();
    System.out.println("\t</timezoneData>");
    System.out.println("</supplementalData>");
    System.out.println();
    String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t\t";
    // "((?:[-+_A-Za-z0-9]+[/])+[A-Za-z0-9])[-+_A-Za-z0-9]*"
    String broken = CldrUtility.breakLines(tzid, sep, Pattern.compile(
        "((?:[-+_A-Za-z0-9]+[/])+[-+_A-Za-z0-9])[-+_A-Za-z0-9]*").matcher(""),
        80);
    assert (tzid.toString().equals(broken.replace(sep, " ")));
    System.out.println("\t\t\t<variable id=\"$tzid\" type=\"choice\">" + broken
        + CldrUtility.LINE_SEPARATOR + "\t\t\t</variable>");
  }

  public static void writeMetazonePrettyPath() {
    TestInfo testInfo = TestInfo.getInstance();
    Map<String, Map<String, String>> map = testInfo.getSupplementalDataInfo().getMetazoneToRegionToZone();
    Map zoneToCountry = testInfo.getStandardCodes().getZoneToCounty();
    Set<Pair<String,String>> results = new TreeSet();
    Map<String,String> countryToContinent = getCountryToContinent(testInfo.getSupplementalDataInfo(), testInfo.getEnglish());
    
    for (String metazone : map.keySet()) {
      Map<String, String> regionToZone = map.get(metazone);
      String zone = regionToZone.get("001");
      if (zone == null) {
        throw new IllegalArgumentException("Missing 001 for metazone " + metazone);
      }
      String continent = zone.split("/")[0];
      
      final Object country = zoneToCountry.get(zone);
      results.add(new Pair<String,String>(continent + "\t" + country + "\t" + countryToContinent.get(country) + "\t" + metazone, metazone));
    }
    for (Pair<String,String> line : results) {
      System.out.println("'" + line.getSecond() + "'\t>\t'\t" + line.getFirst() + "\t'");
    }
  }

  private static Map<String, String> getCountryToContinent(SupplementalDataInfo supplementalDataInfo, CLDRFile english) {
    Relation<String,String> countryToContinent = new Relation(new TreeMap(), TreeSet.class);
    Set<String> continents = new HashSet<String>(Arrays.asList("002", "019", "142", "150", "009"));
    // note: we don't need more than 3 levels
    for (String continent : continents) {
      final Set<String> subcontinents = supplementalDataInfo.getContained(continent);
      countryToContinent.putAll(subcontinents, continent);
      for (String subcontinent : subcontinents) {
        if (subcontinent.equals("QU")) continue;
        final Set<String> countries = supplementalDataInfo.getContained(subcontinent);
        countryToContinent.putAll(countries, continent);
      }
    }
    // convert to map
    Map<String, String> results = new TreeMap<String, String>();
    for (String item : countryToContinent.keySet()) {
      final Set<String> containees = countryToContinent.getAll(item);
      if (containees.size() != 1) {
        throw new IllegalArgumentException(item + "\t" + containees);
      }
      results.put(item, english.getName(CLDRFile.TERRITORY_NAME, containees.iterator().next()));
    }
    return results;
  }

  private static void writeZonePrettyPath(RuleBasedCollator col, Map<String, String> zone_country,
          CLDRFile english) throws IOException {
    System.out.println("Writing zonePrettyPath");
    Set<String> masked = new HashSet();
    Map<String, String> zoneNew_Old = new TreeMap(col);
    String lastZone = "XXX";
    for (String zone : new TreeSet<String>(zone_country.keySet())) {
      String[] parts = zone.split("/");
      String newPrefix = zone_country.get(zone); // english.getName("tzid", zone_country.get(zone), false).replace(' ', '_');
      if (newPrefix.equals("001")) {
        newPrefix = "ZZ";
      }
      parts[0] = newPrefix;
      String newName;
      if (parts.length > 2) {
        System.out.println("\tMultifield: " + zone);
        if (parts.length == 3 && parts[1].equals("Argentina")) {
          newName = parts[0] + "/" + parts[1];
        } else {
          newName = CldrUtility.join(parts, "/");
        }
      } else {
        newName = CldrUtility.join(parts, "/");
      }
      zoneNew_Old.put(newName, zone);
      if (zone.startsWith(lastZone)) {
        masked.add(zone); // find "masked items" and do them first.
      } else {
        lastZone = zone;
      }
    }

    Log.setLog(CldrUtility.GEN_DIRECTORY + "/supplemental/prettyPathZone.txt");
    String lastCountry = "";
    for (int i = 0; i < 2; ++i) {
      Set<String> orderedList = zoneNew_Old.keySet();
      if (i == 0) {
        Log
            .println("# Short IDs for zone names: country code + last part of TZID");
        Log
            .println("# First are items that would be masked, and are moved forwards and sorted in reverse order");
        Log.println();
        Comparator c;
        Set<String> temp = new TreeSet(new ReverseComparator(col));
        temp.addAll(orderedList);
        orderedList = temp;
      } else {
        Log.println();
        Log.println("# Normal items, sorted by country code");
        Log.println();
      }

      // do masked items first

      for (String newName : orderedList) {
        String oldName = zoneNew_Old.get(newName);
        if (masked.contains(oldName) != (i == 0)) {
          continue;
        }
        String newCountry = newName.split("/")[0];
        if (!newCountry.equals(lastCountry)) {
          Log.println("# " + newCountry + "\t"
              + english.getName("territory", newCountry));
          lastCountry = newCountry;
        }
        Log.println("\t'" + oldName + "'\t>\t'" + newName + "';");
      }
    }
    Log.close();
    System.out.println("Done Writing zonePrettyPath");
  }

  public static class ReverseComparator<T> implements Comparator<T> {
    Comparator<T> other;

    public ReverseComparator(Comparator<T> other) {
      this.other = other;
    }

    public int compare(T o1, T o2) {
      return other.compare(o2, o1);
    }
  }

  public static void getSubtagVariables() {
    System.out.println("</supplementalData>");
    System.out.println();
    String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t\t";
    StandardCodes sc = StandardCodes.make();
    for (String type : new String[] { "grandfathered", "language", "territory",
        "script", "variant", "currency", "currency2" }) {
      Set i;
      if (type.equals("currency2")) {
        i = getSupplementalCurrency();
      } else {
        i = sc.getAvailableCodes(type);
      }
      TreeSet<String> s = new TreeSet<String>(i);
      StringBuffer b = new StringBuffer();
      for (String code : s) {
        if (b.length() != 0)
          b.append(' ');
        b.append(code);
      }
      // "((?:[-+_A-Za-z0-9]+[/])+[A-Za-z0-9])[-+_A-Za-z0-9]*"
      String broken = CldrUtility.breakLines(b, sep, Pattern.compile(
          "([-_A-Za-z0-9])[-_A-Za-z0-9]*").matcher(""), 80);
      assert (b.toString().equals(broken.replace(sep, " ")));
      System.out.println("\t\t\t<variable id=\"$" + type
          + "\" type=\"choice\">" + broken + CldrUtility.LINE_SEPARATOR + "\t\t\t</variable>");
    }
    Set<String> available = Iso639Data.getAvailable();
    //      <languageAlias type="aju" replacement="jrb"/> <!-- Moroccan Judeo-Arabic ⇒ Judeo-Arabic -->
    Set<String> bad3letter = new HashSet<String>();
    for (String lang : available) {
      if (lang.length() != 2) continue;
      String alpha3 = Iso639Data.toAlpha3(lang);
      bad3letter.add(alpha3);
      System.out.println("\t\t\t<languageAlias type=\"" + alpha3 + "\" replacement=\"" + lang + "\"/> <!-- " +
              Iso639Data.getNames(lang) + " -->");
    }
    SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
    Map<String, Map<String, List<String>>> localeAliasInfo = supplementalData.getLocaleAliasInfo();
    Map<String, List<String>> languageAliasInfo = localeAliasInfo.get("language");
    Set<String> encompassed = Iso639Data.getEncompassed();
    Set<String> macros = Iso639Data.getMacros();
    Map<String,String> encompassed_macro = new HashMap();
    for (String type : languageAliasInfo.keySet()) {
      List<String> replacements = languageAliasInfo.get(type);
      if (!encompassed.contains(type)) continue;
      if (replacements == null && replacements.size() != 1) continue;
      String replacement = replacements.get(0);
      if (macros.contains(replacement)) {
        // we have a match, encompassed => replacement
        encompassed_macro.put(type, replacement);
      }
    }
    Set<String> missing = new TreeSet();
    missing.addAll(macros);
    missing.remove("no");
    missing.remove("sh");
    
    missing.removeAll(encompassed_macro.values());
    if (missing.size() != 0) {
      for (String missingMacro : missing) {
        System.out.println("ERROR Missing <languageAlias type=\"" + "???" + "\" replacement=\"" + missingMacro + "\"/> <!-- ??? => " +
                Iso639Data.getNames(missingMacro) + " -->");
        System.out.println("\tOptions for ???:");
        for (String enc : Iso639Data.getEncompassedForMacro(missingMacro)) {
          System.out.println("\t" + enc + "\t// " + Iso639Data.getNames(enc));
        }
      }
    }
    // verify that every macro language has a encompassed mapping to it
    // and remember those codes
    

    // verify that nobody contains a bad code
    
    for(String type : languageAliasInfo.keySet()) {
      List<String> replacements = languageAliasInfo.get(type);
      if (replacements == null) continue;
      for (String replacement : replacements) {
        if (bad3letter.contains(replacement)) {
          System.out.println("ERROR: Replacement(s) for type=\"" + type +
          		"\" contains " + replacement + ", which should be: " + Iso639Data.fromAlpha3(replacement));
        }
      }
    }
    
    // get the bad ISO codes
    
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    CLDRFile english = cldrFactory.make("en", true);

    Set<String> territories = new TreeSet();
    Relation<String, String> containers = supplementalData.getTerritoryToContained();
    for (String region : sc.getAvailableCodes("territory")) {
      if (containers.containsKey(region)) continue;
      territories.add(region);
    }
    addRegions(english, territories, "EA,EU,IC".split(","), new Transform<String,String>() {
      public String transform(String region) {
        return IsoRegionData.get_alpha3(region);
      }
    });
    addRegions(english, territories, "AC,CP,DG,EA,EU,IC,TA".split(","), new Transform<String,String>() {
      public String transform(String region) {
        return IsoRegionData.getNumeric(region);
      }
    });
    
    // check that all deprecated codes are in fact deprecated
    Map<String, Map<String, Map<String, String>>> fullData = sc.getLStreg();

    checkCodes("language", sc, localeAliasInfo, fullData);
    checkCodes("script", sc, localeAliasInfo, fullData);
    checkCodes("territory", sc, localeAliasInfo, fullData);
  }

  private static void checkCodes(String type, StandardCodes sc,
          Map<String, Map<String, List<String>>> localeAliasInfo, Map<String, Map<String, Map<String, String>>> fullData) {
    Map<String, Map<String, String>> typeData = fullData.get("territory".equals(type) ? "region" : type);
    Map<String, List<String>> aliasInfo = localeAliasInfo.get(type);
    for (String code : sc.getAvailableCodes(type)) {
      Map<String, String> subdata = typeData.get(code);
      String deprecated = subdata.get("Deprecated");
      if (deprecated == null) continue;
      String replacement = subdata.get("Preferred-Value");
      List<String> supplementalReplacements = aliasInfo.get(code);
      if (supplementalReplacements == null) {
        System.out.println("Deprecated in LSTR, but not in supplementalData: " + type + "\t" + code + "\t" + replacement);
      }
    }
  }
  

  private static void addRegions(CLDRFile english, Set<String> availableCodes, String[] exceptions, Transform<String,String> trans) {
    Set<String> missingRegions = new TreeSet<String>();
    Set<String> exceptionSet = new HashSet(Arrays.asList(exceptions));
    for (String region : availableCodes) {
      if (exceptionSet.contains(region)) continue;
      String alpha3 = trans.transform(region);
      String name = english.getName(CLDRFile.TERRITORY_NAME, region);
      if (alpha3 == null) {
        missingRegions.add(region);
        continue;
      }
      System.out.println("\t\t\t<territoryAlias type=\"" + alpha3 + "\" replacement=\"" + region + "\"/> <!-- " + name + " -->");
    }
    for (String region : missingRegions) {
      String name = english.getName(CLDRFile.TERRITORY_NAME, region);
      System.out.println("ERROR: Missing code for " + region + "\t" + name);
    }
  }

  private static Set getSupplementalCurrency() {
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.SUPPLEMENTAL_DIRECTORY,
        ".*");
    CLDRFile supp = cldrFactory.make("supplementalData", false);
    XPathParts p = new XPathParts();
    Set currencies = new TreeSet();
    Set decimals = new TreeSet();
    for (Iterator it = supp.iterator(); it.hasNext();) {
      String path = (String) it.next();
      String currency = p.set(path).findAttributeValue("currency", "iso4217");
      if (currency != null) {
        currencies.add(currency);
      }
      currency = p.set(path).findAttributeValue("info", "iso4217");
      if (currency != null) {
        decimals.add(currency);
      }
    }
    decimals.remove("DEFAULT");
    if (!currencies.containsAll(decimals)) {
      decimals.removeAll(currencies);
      System.out.println("Value with decimal but no country: " + decimals);
    }
    Set std = StandardCodes.make().getAvailableCodes("currency");
    TreeSet stdMinusCountry = new TreeSet(std);
    stdMinusCountry.removeAll(currencies);
    if (std.size() != 0) {
      System.out.println("In standard, but no country:" + stdMinusCountry);
    }
    TreeSet countriesMinusStd = new TreeSet(currencies);
    countriesMinusStd.removeAll(std);
    if (countriesMinusStd.size() != 0) {
      System.out.println("Have country, but not in std:" + countriesMinusStd);
    }

    return currencies;
  }

  /**
   * 
   */
  private static String toString(Collection aliases, String separator) {
    StringBuffer result = new StringBuffer();
    boolean first = true;
    for (Iterator it = aliases.iterator(); it.hasNext();) {
      Object item = it.next();
      if (first)
        first = false;
      else
        result.append(separator);
      result.append(item);
    }
    return result.toString();
  }

  public static void showZoneInfo() throws IOException {
    StandardCodes sc = StandardCodes.make();
    Map m = sc.getZoneLinkold_new();
    int i = 0;
    System.out.println("/* Generated by org.unicode.cldr.tool.CountItems */");
    System.out.println();
    i = 0;
    System.out.println("/* zoneID, canonical zoneID */");
    for (Iterator it = m.keySet().iterator(); it.hasNext();) {
      String old = (String) it.next();
      String newOne = (String) m.get(old);
      System.out.println("{\"" + old + "\", \"" + newOne + "\"},");
      ++i;
    }
    System.out.println("/* Total: " + i + " */");

    System.out.println();
    i = 0;
    System.out.println("/* All canonical zoneIDs */");
    for (Iterator it = sc.getZoneData().keySet().iterator(); it.hasNext();) {
      String old = (String) it.next();
      System.out.println("\"" + old + "\",");
      ++i;
    }
    System.out.println("/* Total: " + i + " */");

    Factory mainCldrFactory = Factory.make(CldrUtility.COMMON_DIRECTORY + "main"
        + File.separator, ".*");
    CLDRFile desiredLocaleFile = mainCldrFactory.make("root", true);
    String temp = desiredLocaleFile
        .getFullXPath("//ldml/dates/timeZoneNames/singleCountries");
    String singleCountriesList = (String) new XPathParts(null, null).set(temp)
        .findAttributes("singleCountries").get("list");
    Set singleCountriesSet = new TreeSet(CldrUtility.splitList(singleCountriesList,
        ' '));

    Map zone_countries = StandardCodes.make().getZoneToCounty();
    Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
    System.out.println();
    i = 0;
    System.out.println("/* zoneID, country, isSingle */");
    for (Iterator it = zone_countries.keySet().iterator(); it.hasNext();) {
      String old = (String) it.next();
      String newOne = (String) zone_countries.get(old);
      Set s = (Set) countries_zoneSet.get(newOne);
      String isSingle = (s != null && s.size() == 1 || singleCountriesSet
          .contains(old)) ? "T" : "F";
      System.out.println("{\"" + old + "\", \"" + newOne + "\", \"" + isSingle
          + "\"},");
      ++i;
    }
    System.out.println("/* Total: " + i + " */");

    if (true)
      return;

    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    Map platform_locale_status = StandardCodes.make().getLocaleTypes();
    Map onlyLocales = (Map) platform_locale_status.get("IBM");
    Set locales = onlyLocales.keySet();
    CLDRFile english = cldrFactory.make("en", true);
    for (Iterator it = locales.iterator(); it.hasNext();) {
      String locale = (String) it.next();
      System.out.println(locale + "\t" + english.getName(locale) + "\t"
          + onlyLocales.get(locale));
    }
  }

  static final NumberFormat decimal = NumberFormat.getNumberInstance();
  static {
    decimal.setGroupingUsed(true);
  }

  public static void countItems() {
    //CLDRKey.main(new String[]{"-mde.*"});
    String dir = CldrUtility.getProperty("source", CldrUtility.MAIN_DIRECTORY);
    Factory cldrFactory = CLDRFile.Factory.make(dir, ".*");
    countItems(cldrFactory, false);
  }

  /**
   * @param cldrFactory
   * @param resolved
   */
  private static int countItems(Factory cldrFactory, boolean resolved) {
    int count = 0;
    int resolvedCount = 0;
    Set locales = cldrFactory.getAvailable();
    Set keys = new HashSet();
    Set values = new HashSet();
    Set fullpaths = new HashSet();
    Matcher alt = CLDRFile.ALT_PROPOSED_PATTERN.matcher("");

    Set temp = new HashSet();
    for (Iterator it = locales.iterator(); it.hasNext();) {
      String locale = (String) it.next();
      if (CLDRFile.isSupplementalName(locale))
        continue;
      CLDRFile item = cldrFactory.make(locale, false);

      temp.clear();
      for (Iterator it2 = item.iterator(); it2.hasNext();) {
        String path = (String) it2.next();
        if (alt.reset(path).matches()) {
          continue;
        }
        temp.add(path);
        keys.add(path);
        values.add(item.getStringValue(path));
        fullpaths.add(item.getFullXPath(path));
      }
      int current = temp.size();

      CLDRFile itemResolved = cldrFactory.make(locale, true);
      temp.clear();
      CollectionUtilities.addAll(itemResolved.iterator(), temp);
      int resolvedCurrent = temp.size();

      System.out.println(locale + "\tPlain:\t" + current + "\tResolved:\t"
          + resolvedCurrent + "\tUnique Paths:\t" + keys.size()
          + "\tUnique Values:\t" + values.size() + "\tUnique Full Paths:\t"
          + fullpaths.size());
      count += current;
      resolvedCount += resolvedCurrent;
    }
    System.out.println("Total Items\t" + decimal.format(count));
    System.out
        .println("Total Resolved Items\t" + decimal.format(resolvedCount));
    System.out.println("Unique Paths\t" + decimal.format(keys.size()));
    System.out.println("Unique Values\t" + decimal.format(values.size()));
    System.out
        .println("Unique Full Paths\t" + decimal.format(fullpaths.size()));
    return count;
  }

}
