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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CLDRTest;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.ZoneInflections;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.ZoneInflections.OutputLong;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeMap.Composer;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * Simple program to count the amount of data in CLDR. Internal Use.
 */
public class CountItems {
	private static Set keys = new HashSet();
	/**
	 * Count the data.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
        try {
            getZoneEquivalences();
            if (true) return;
            getPatternBlocks();
            showExemplars();
            genSupplementalZoneData(true);
            showZoneInfo();
            countItems();
        } finally{
            System.out.println("Done");
        }
	}
    
    private static void getZoneEquivalences() {
        StandardCodes sc = StandardCodes.make();
        Collection codes = sc.getGoodAvailableCodes("tzid");
        ArrayComparator ac = new ArrayComparator(new Comparator[] {ArrayComparator.COMPARABLE,ArrayComparator.COMPARABLE,ArrayComparator.COMPARABLE});
        Map zone_countries = sc.getZoneToCounty();
        
        TreeSet country_inflection_names = new TreeSet(ac);
        for (Iterator it = codes.iterator(); it.hasNext();) {
            String zoneID = (String) it.next();
            String country = (String) zone_countries.get(zoneID);
            java.util.TimeZone zone = java.util.TimeZone.getTimeZone(zoneID);            
            ZoneInflections zip = new ZoneInflections(zone);
            country_inflection_names.add(new Object[]{country, zip, zoneID});
        }
        System.out.println("Modern Equivalents");
        String lastCountry = "";
        ZoneInflections lastZip = null;
        ZoneInflections.OutputLong diff = new ZoneInflections.OutputLong(0);
        for (Iterator it = country_inflection_names.iterator(); it.hasNext();) {
            Object[] row = (Object[]) it.next();
            String country = (String) row[0];
            ZoneInflections zip = (ZoneInflections) row[1];
            String zoneID = (String) row[2];
            int zipComp = zip.compareTo(lastZip, diff);
            
            if (!country.equals(lastCountry)) System.out.println();
            else if (diff.value != Long.MIN_VALUE) System.out.println("\t\t" + ICUServiceBuilder.isoDateFormat(diff.value));
            
            if (country.equals(lastCountry) && zipComp == 0) System.out.print("X");
            System.out.println("\t" + country + "\t" + zoneID
                    + "\t" + zip.formatHours(zip.getMinOffset())
                    + "\t" + zip.formatHours(zip.getMaxOffset())
                    );
            lastCountry = country;
            lastZip = zip;
        }
    }
    /**
     * 
     */
    private static void getPatternBlocks() {
        UnicodeSet patterns = new UnicodeSet("[:pattern_syntax:]");
        UnicodeSet unassigned = new UnicodeSet("[:unassigned:]");
        UnicodeSet punassigned = new UnicodeSet(patterns).retainAll(unassigned);
        UnicodeMap blocks = ICUPropertyFactory.make().getProperty("block").getUnicodeMap();
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
        for (UnicodeMap.MapIterator it = new UnicodeMap.MapIterator(blocks); it.nextRange();) {
            UnicodeSet range = new UnicodeSet(it.codepoint, it.codepointEnd);
            boolean hasPat = range.containsSome(patterns);
            String prefix = !hasPat ? "Not-Syntax"
                    : !range.containsSome(unassigned) ? "Closed"
                    : !range.containsSome(punassigned) ? "Closed2"
                    : "Open";
            
            boolean show = (prefix.equals("Open") || prefix.equals("Closed2")) ;

            if (show) System.out.println();
            System.out.println(prefix + "\t" + range + "\t" + it.value);
            if (show) {
                System.out.println(new UnicodeMap()
                        .putAll(unassigned,"<reserved>")
                        .putAll(punassigned,"<reserved-for-syntax>")
                        .setMissing("<assigned>")
                        .putAll(range.complement(),null));
            }
        }
    }

    /**
     * @throws IOException
     * 
     */
    private static void showExemplars() throws IOException {
        PrintWriter out = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY, "fixed_exemplars.txt");
        Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
        Set locales = cldrFactory.getAvailable();
        for (Iterator it = locales.iterator(); it.hasNext();) {
            System.out.print('.');
            String locale = (String)it.next();
            CLDRFile cldrfile = cldrFactory.make(locale, false);
            String v = cldrfile.getStringValue("/ldml/characters/exemplarCharacters");
            if (v == null) continue;
            UnicodeSet exemplars = new UnicodeSet(v);
            if (exemplars.size() != 0 && exemplars.size() < 500) {
                Collator col = Collator.getInstance(new ULocale(locale));
                Collator spaceCol = Collator.getInstance(new ULocale(locale));
                spaceCol.setStrength(col.PRIMARY);
                out.println(locale + ":\t\u200E" + v + '\u200E');
//                String fixedFull = CollectionUtilities.prettyPrint(exemplars, col, false);
//                System.out.println(" =>\t" + fixedFull);
//                verifyEquality(exemplars, new UnicodeSet(fixedFull));
                String fixed = CollectionUtilities.prettyPrint(exemplars, col, spaceCol, true);
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
        if (others.equals(exemplars)) return;
        System.out.println("FAIL\ta-b\t" + new UnicodeSet(exemplars).removeAll(others));
        System.out.println("\tb-a\t" + new UnicodeSet(others).removeAll(exemplars));
    }

    /**
     * 
     */
    private static void countItems() {
        //CLDRKey.main(new String[]{"-mde.*"});
        Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
        int count = countItems(cldrFactory, false);
		System.out.println("Count (core): " + count);
		count = countItems(cldrFactory, true);
		System.out.println("Count (resolved): " + count);
		System.out.println("Unique XPaths: " + keys.size());
    }
    
    public static void genSupplementalZoneData(boolean skipUnaliased) {
        RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance();
        col.setNumericCollation(true);
        StandardCodes sc = StandardCodes.make();

        Map zone_country = sc.getZoneToCounty();
        Map country_zone = sc.getCountryToZoneSet();
        Map old_new = sc.getZoneLinkold_new();
        Map new_old = new TreeMap();

        for (Iterator it = old_new.keySet().iterator(); it.hasNext();) {
            String old = (String) it.next();
            String newOne = (String) old_new.get(old);
            Set oldSet = (Set) new_old.get(newOne);
            if (oldSet == null) new_old.put(newOne, oldSet = new TreeSet());
            oldSet.add(old);
        }
        Map fullMap = new TreeMap(col);
        for (Iterator it = zone_country.keySet().iterator(); it.hasNext();) {
            String zone = (String) it.next();
            String defaultName = TimezoneFormatter.getFallbackName(zone);
            Object already = fullMap.get(defaultName);
            if (already != null) System.out.println("CONFLICT: " + already + ", " + zone);
            fullMap.put(defaultName, zone);
        }
        //fullSet.addAll(zone_country.keySet());
        //fullSet.addAll(new_old.keySet());

        System.out.println("<!-- Generated by org.unicode.cldr.tool.CountItems -->");
        System.out.println("<supplementalData>");
        System.out.println("\t<timezoneData>");
        
        Set multizone = new TreeSet();
        for (Iterator it = country_zone.keySet().iterator(); it.hasNext();) {
            String country = (String)it.next();
            Set zones = (Set) country_zone.get(country); 
            if (zones != null && zones.size() != 1) multizone.add(country);
        }

        System.out.println("\t\t<zoneFormatting multizone=\"" + toString(multizone, " ") + "\">");
       
        for (Iterator it = fullMap.keySet().iterator(); it.hasNext();) {
            String lastField = (String) it.next(); 
            String zone = (String) fullMap.get(lastField);            
            String country = (String) zone_country.get(zone);

            Set aliases = (Set) new_old.get(zone);
            if (aliases != null) {
                aliases = new TreeSet(aliases);
                aliases.remove(zone);
            }
            if (skipUnaliased) if (aliases == null || aliases.size() == 0) continue;
            
            System.out.println("\t\t\t<zoneItem"
                    + " type=\"" + zone + "\""
                    + " territory=\"" + country + "\""
                    + (aliases != null && aliases.size() > 0 ? " aliases=\"" + toString(aliases, " ") + "\"": "")
                    + "/>"
                    );
        }
        System.out.println("\t\t</zoneFormatting>");
        System.out.println("\t</timezoneData>");
        System.out.println("<supplementalData>");
    }

    /**
     * 
     */
    private static String toString(Collection aliases, String separator) {
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Iterator it = aliases.iterator(); it.hasNext();) {
            Object item = it.next();
            if (first) first = false;
            else result.append(separator);
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

        Factory mainCldrFactory = Factory.make(Utility.COMMON_DIRECTORY + "main" + File.separator, ".*");
        CLDRFile desiredLocaleFile = mainCldrFactory.make("root", true);
        String temp = desiredLocaleFile.getFullXPath("/ldml/dates/timeZoneNames/singleCountries");
        String singleCountriesList = (String) new XPathParts(null, null).set(
                temp).findAttributes("singleCountries").get("list");
        Set singleCountriesSet = new TreeSet(Utility.splitList(singleCountriesList, ' '));

        Map zone_countries = StandardCodes.make().getZoneToCounty();
        Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
        System.out.println();
        i = 0;
        System.out.println("/* zoneID, country, isSingle */");
        for (Iterator it = zone_countries.keySet().iterator(); it.hasNext();) {
            String old = (String) it.next();
            String newOne = (String) zone_countries.get(old);
            Set s = (Set) countries_zoneSet.get(newOne);
            String isSingle = (s != null && s.size() == 1 || singleCountriesSet.contains(old)) ? "T" : "F";
            System.out.println("{\"" + old + "\", \"" + newOne+ "\", \"" + isSingle + "\"},");
            ++i;
        }
        System.out.println("/* Total: " + i + " */");
                
        if (true) return;
        
        Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
        Map platform_locale_status = StandardCodes.make().getLocaleTypes();
        Map onlyLocales = (Map) platform_locale_status.get("IBM");
        Set locales = onlyLocales.keySet();
        CLDRFile english = cldrFactory.make("en", true);
        for (Iterator it = locales.iterator(); it.hasNext();) {
            String locale = (String) it.next();
            System.out.println(locale + "\t" + english.getName(locale,false) + "\t" + onlyLocales.get(locale));         
        }
    }

	/**
	 * @param cldrFactory
	 * @param resolved
	 */
	private static int countItems(Factory cldrFactory, boolean resolved) {
		int count = 0;
		Set locales = cldrFactory.getAvailable();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String locale = (String)it.next();
			CLDRFile item = cldrFactory.make(locale, resolved);
			keys.addAll(item.keySet());
			int current = item.keySet().size();
			System.out.println(locale + "\t" + current);
			count += current;
		}
		return count;
	}

}