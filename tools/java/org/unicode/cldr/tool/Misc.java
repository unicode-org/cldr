/*
**********************************************************************
* Copyright (c) 2004-2005, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.ZoneParser;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.ZoneParser.RuleLine;
import org.unicode.cldr.util.ZoneParser.ZoneLine;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
//import com.ibm.icu.impl.Utility;

/**
 * Grab-bag set of tools that needs to be rationalized.
 */
public class Misc {
	static Factory cldrFactory;
	static CLDRFile english;
	static CLDRFile resolvedRoot;
	// WARNING: this file needs a serious cleanup
	
    private static final int
	    HELP1 = 0,
	    HELP2 = 1,
	    SOURCEDIR = 2,
	    DESTDIR = 3,
	    MATCH = 4,
	    TO_LOCALIZE = 5,
	    CURRENT = 6,
		WINDOWS = 7,
		OBSOLETES = 8,
		ALIASES = 9,
		INFO = 10,
        ZONES = 11,
        LANGUAGE_TAGS = 12,
        FUNCTION = 13
		;
	
	private static final UOption[] options = {
	    UOption.HELP_H(),
	    UOption.HELP_QUESTION_MARK(),
	    UOption.SOURCEDIR().setDefault(CldrUtility.COMMON_DIRECTORY),
	    UOption.DESTDIR().setDefault(CldrUtility.GEN_DIRECTORY + "timezones/"),
	    UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
	    UOption.create("to_localize", 't', UOption.NO_ARG),
	    UOption.create("current", 'c', UOption.NO_ARG),
	    UOption.create("windows", 'w', UOption.NO_ARG),
	    UOption.create("obsoletes", 'o', UOption.NO_ARG),
	    UOption.create("aliases", 'a', UOption.NO_ARG),
        UOption.create("info", 'i', UOption.NO_ARG),
        UOption.create("zones", 'z', UOption.NO_ARG),
        UOption.create("langauge-tags", 'l', UOption.NO_ARG),
        UOption.create("function", 'f', UOption.REQUIRES_ARG),
	};
	
	private static final String HELP_TEXT = "Use the following options" + XPathParts.NEWLINE
	+ "-h or -?\tfor this message" + XPathParts.NEWLINE
	+ "-"+options[SOURCEDIR].shortName + "\tsource directory. Default = " 
	+ CldrUtility.getCanonicalName(CldrUtility.MAIN_DIRECTORY) + XPathParts.NEWLINE
	+ "-"+options[DESTDIR].shortName + "\tdestination directory. Default = "
	+ CldrUtility.getCanonicalName(CldrUtility.GEN_DIRECTORY + "main/") + XPathParts.NEWLINE
	+ "-m<regex>\tto restrict the locales to what matches <regex>" + XPathParts.NEWLINE
	+ "-t\tgenerates files that contain items missing localizations" + XPathParts.NEWLINE
	+ "-c\tgenerates missing timezone localizations" + XPathParts.NEWLINE
	+ "-w\tgenerates Windows timezone IDs" + XPathParts.NEWLINE
	+ "-o\tlist display codes that are obsolete" + XPathParts.NEWLINE
	+ "-o\tshows timezone aliases"
    + "-i\tgets element/attribute/value information"
    + "-z\tcollected timezone localizations"
	;

	/**
	 * Picks options and executes. Use -h to see options.
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
		try {
		  
      showLanguageTagCount();

//            Locale someLocale = Locale.FRENCH;
//            Date someDate = new Date();
//            ULocale uloc;
//            
//SimpleDateFormat dateTimeFormat = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT, someLocale);
//String pattern = dateTimeFormat.toPattern();
//// you now have a pattern, which you can copy and modify
//System.out.println(dateTimeFormat.format(someDate)); // unmodified
//pattern += "'some other stuff'";
//dateTimeFormat.applyPattern(pattern);
//System.out.println(dateTimeFormat.format(someDate)); // modified
//            
//            if (true) return;
	        UOption.parseArgs(args, options);
	        if (options[HELP1].doesOccur || options[HELP1].doesOccur) {
	        	System.out.println(HELP_TEXT);
            CldrUtility.showMethods(Misc.class);
	        	return;
	        }
			cldrFactory = Factory.make(options[SOURCEDIR].value + "/main/", options[MATCH].value);
			english = cldrFactory.make("en", false);
			resolvedRoot = cldrFactory.make("root", true);
			if (options[MATCH].value.equals("group1")) options[MATCH].value = "(en|fr|de|it|es|pt|ja|ko|zh)";
			Set languages = new TreeSet(cldrFactory.getAvailableLanguages());
			//new Utility.MatcherFilter(options[MATCH].value).retainAll(languages);
			//new Utility.MatcherFilter("(sh|zh_Hans|sr_Cyrl)").removeAll(languages);
			
			if (options[CURRENT].doesOccur) {
				printCurrentTimezoneLocalizations(languages);
			}
            
            if (options[ZONES].doesOccur) {
                printAllZoneLocalizations();
            }          
	
			if (options[TO_LOCALIZE].doesOccur) {
				for (Iterator it = languages.iterator(); it.hasNext();) {
					String language = (String)it.next();
					printSupplementalData(language);
				}
			}
			
			if (options[WINDOWS].doesOccur) {
				printWindowsZones();
			}
			
			if (options[INFO].doesOccur) {
				PrintWriter pw = BagFormatter.openUTF8Writer(CldrUtility.CHART_DIRECTORY, "attributesAndValues.html");
				new GenerateAttributeList(cldrFactory).show(pw);
				pw.close();
			}
			
			if (options[OBSOLETES].doesOccur) {
				listObsoletes();
			}

			if (options[ALIASES].doesOccur) {
				printZoneAliases();
			}
      

			// TODO add options for these later
			//getCities();
			//
      if (options[FUNCTION].doesOccur) {
        String function = options[FUNCTION].value;
        
        CldrUtility.callMethod(function, Misc.class);
      }

			//getZoneData();

		} finally {
			System.out.println("DONE");
		}
	}

//  public static void callMethod(String methodName, Class cls) {
//    try {
//      Method method;
//      try {
//        method = cls.getMethod(methodName, (Class[]) null);
//        try {
//          method.invoke(null, (Object[]) null);
//        } catch (Exception e) {
//          e.printStackTrace();              
//        }
//      } catch (Exception e) {
//        System.out.println("No such method: " + methodName);
//        showMethods(cls);
//      }
//    } catch (ClassNotFoundException e) {
//      e.printStackTrace();
//    }
//  }
//
//  public static void showMethods(Class cls) throws ClassNotFoundException {
//    System.out.println("Possible methods are: ");
//    Method[] methods = cls.getMethods();
//    Set<String> names = new TreeSet<String>();
//    for (int i = 0; i < methods.length; ++i) {
//      if (methods[i].getGenericParameterTypes().length != 0) continue;
//      int mods = methods[i].getModifiers();
//      if (!Modifier.isStatic(mods)) continue;
//      String name = methods[i].getName();
//      names.add(name);
//    }
//    for (Iterator it = names.iterator(); it.hasNext();) {
//      System.out.println("\t-f" + it.next());
//    }
//  }
	
	/**
	 * 
	 */
	private static void showLanguageTagCount() {
		StandardCodes sc = StandardCodes.make();
		int languageCount = sc.getGoodAvailableCodes("language").size();
    int scriptCount = sc.getGoodAvailableCodes("script").size();
    int countryCount = sc.getGoodAvailableCodes("territory").size();
    System.out.println("language subtags:\t" + languageCount);
    System.out.println("script subtags:\t" + scriptCount);
    System.out.println("region subtags:\t" + countryCount);

//		for (Iterator it = sc.getAvailableCodes("territory").iterator(); it.hasNext();) {
//			System.out.print("fr-" + it.next() + ", ");
//		}
		System.out.println();
	}

	private static void listObsoletes() {
		java.util.TimeZone t;
		StandardCodes sc = StandardCodes.make();
		for (Iterator typeIt = sc.getAvailableTypes().iterator(); typeIt.hasNext();) {
			String type = (String) typeIt.next();
			System.out.println(type);
			for (Iterator codeIt = sc.getAvailableCodes(type).iterator(); codeIt.hasNext();) {
				String code = (String) codeIt.next();
				List list = sc.getFullData(type, code);
				if (list.size() < 3) continue;
				String replacementCode = (String) list.get(2);
				if (replacementCode.length() == 0) continue;
				System.out.println(code + " => " + replacementCode + "; " 
						+ english.getName(type, replacementCode));
			}
		}
	}
	
    // Windows info: http://msdn.microsoft.com/library/default.asp?url=/library/en-us/e2k3/e2k3/_cdoex_time_zone_to_cdotimezoneid_map.asp
    // ICU info: http://oss.software.ibm.com/cvs/icu/~checkout~/icu/source/common/putil.c
    // search for "Mapping between Windows zone IDs"
	
    static Set priorities = new TreeSet(Arrays.asList(new String[] { "en", "zh_Hans",
            "zh_Hant", "da", "nl", "fi", "fr", "de", "it",
            "ja", "ko", "nb", "pt_BR", "ru", "es", "sv", "ar", "bg", "ca",
            "hr", "cs", "et", "el", "he", "hi", "hu", "is", "id", "lv", "lt",
            "pl", "ro", "sr", "sk", "sl", "tl", "th", "tr", "uk", "ur", "vi"
//            // "en_GB",  
            }));
 
    private static void printAllZoneLocalizations() throws IOException {
        StandardCodes sc = StandardCodes.make();
        Set zones = sc.getAvailableCodes("tzid");
        Map offset_zone_locale_name = new TreeMap();
        for (Iterator it2 = priorities.iterator(); it2.hasNext();) {
            String locale = (String) it2.next();
            System.out.println(locale);
            try {
                TimezoneFormatter tzf = new TimezoneFormatter(cldrFactory, locale, true);
                for (Iterator it = zones.iterator(); it.hasNext();) {
                    String zone = (String) it.next();
                    TimeZone tzone = TimeZone.getTimeZone(zone);
                    int stdOffset = tzone.getRawOffset();
                    Integer standardOffset = new Integer(-stdOffset);
                    String name = tzf.getFormattedZone(zone, "vvvv", false, stdOffset, false);
                    String gmt = tzf.getFormattedZone(zone, "ZZZZ", false, stdOffset, false);
                    String fullName = "(" + gmt + ") " 
                        + (zone.startsWith("Etc") ? "" : name);
                    
                    Map zone_locale_name = (Map) offset_zone_locale_name.get(standardOffset);
                    if (zone_locale_name == null) offset_zone_locale_name.put(standardOffset, zone_locale_name = new TreeMap());
                    
                    Map locale_name = (Map) zone_locale_name.get(zone);
                    if (locale_name == null) zone_locale_name.put(zone, locale_name = new TreeMap());
                    
                    locale_name.put(locale, fullName);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        PrintWriter out = BagFormatter.openUTF8Writer("c:/", "zone_localizations.html");
        out.println("<html><head>");
        out.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        out.println("<title>Zone Localizations</title>");
        out.println("<style>");
        out.println("th,td { text-align: left; vertical-align: top }");
        out.println("th { background-color: gray }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<table cellspacing='0' cellpadding='2' border='1'>");
        out.println("<tr><th></th><th>No</th><th>Country</th><th>Offset(s)</th>");
        
        // do the header
        for (Iterator it2 = priorities.iterator(); it2.hasNext();) {
            String locale = (String) it2.next();
            String englishLocaleName = english.getName(locale);
            out.println("<th>" + locale + " (" + englishLocaleName + ")" + "</th>");
        }
        
        // now the rows
        out.println("</tr>");
        Map zone_country = sc.getZoneToCounty();
        int count = 0;
        for (Iterator it = offset_zone_locale_name.keySet().iterator(); it.hasNext();) {
            Integer offset = (Integer) it.next();
            //out.println(offset);
            Map zone_locale_name = (Map) offset_zone_locale_name.get(offset);
            for (Iterator it2 = zone_locale_name.keySet().iterator(); it2.hasNext();) {
                String zone = (String) it2.next();
                out.println("<tr>");
                out.println("<th>" + (++count) + "</th>");
                out.println("<th>" + zone + "</th>");
                String country = (String) zone_country.get(zone);
                String countryName = english.getName(english.TERRITORY_NAME,country);
                out.println("<td>" + country + " (" + countryName + ")" + "</td>");
                TimeZone tzone = TimeZone.getTimeZone(zone);
                out.println("<td>" + offsetString(tzone) + "</td>");
                Map locale_name = (Map) zone_locale_name.get(zone);
                for (Iterator it3 = priorities.iterator(); it3.hasNext();) {
                    String locale = (String) it3.next();
                    String name = (String) locale_name.get(locale);
                    out.println("<td>");
                    if (name == null) {
                        out.println("&nbsp;");    
                    } else {
                        out.println(TransliteratorUtilities.toHTML.transliterate(name));    
                    }
                    out.println("</td>");
                }
                out.println("</tr>");
            }
        }
        out.println("</table>");
        out.println(ShowData.ANALYTICS);
        out.println("</body></html>");
        out.close();
    }
	/**
     * @param tzone
     * @return
     */
    private static String offsetString(TimeZone tzone) {
        // TODO Auto-generated method stub
        int janOffset = tzone.getOffset(JAN152006);
        int juneOffset = tzone.getOffset(JUNE152006);
        String result = hours.format(janOffset/3600000.0);
        if (juneOffset != janOffset) result += " / " + hours.format(juneOffset/3600000.0);
        return result;
    }
    static long JAN152006 = new Date(2006-1900,1-1,15,0,0,0).getTime();
    static long JUNE152006 = new Date(2006-1900,6-1,15,0,0,0).getTime();
    static NumberFormat hours = new DecimalFormat("0.##");
    /**
	 * @param languages
	 * @throws IOException
	 */
	private static void printCurrentTimezoneLocalizations(Set languages) throws IOException {
		Set rtlLanguages = new TreeSet();
		for (Iterator it = languages.iterator(); it.hasNext();) {
			String language = (String)it.next();
			CLDRFile desiredLocaleFile = cldrFactory.make(language, true);
			String orientation = desiredLocaleFile.getFullXPath("//ldml/layout/orientation");
			boolean rtl = orientation == null ? false
					: orientation.indexOf("[@characters=\"right-to-left\"]") >= 0;
			// <orientation characters="right-to-left"/>
			PrintWriter log = BagFormatter.openUTF8Writer(options[DESTDIR].value + "", language + "_timezones.html");
			log.println("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
			log.println("<style type=\"text/css\"><!--");
			log.println("td { text-align: center; vertical-align:top }");
			log.println("th { vertical-align:top }");
			if (rtl) {
				rtlLanguages.add(language);
				log.println("body { direction:rtl }");
				log.println(".ID {background-color: silver; text-align:right;}");
				log.println(".T {text-align:right; color: green}");
			} else {
				log.println(".ID {background-color: silver; text-align:left;}");
				log.println(".T {text-align:left; color: green}");
			}
			log.println(".I {color: blue}");
			log.println(".A {color: red}");
			log.println("--></style>");
			log.println("<title>Time Zone Localizations for " + language + "</title><head><body>");
			log.println("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse\">");
			printCurrentTimezoneLocalizations(log, language);
			//printSupplementalData(group1[i]);
      log.println("</table>");
      log.println(ShowData.ANALYTICS);
			log.println("</body></html>");
			log.close();
		}
		System.out.println("RTL languages: " + rtlLanguages);
	}
	
	static void printZoneAliases() {
		RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
		col.setNumericCollation(true);
		StandardCodes sc = StandardCodes.make();
		Map zone_countries = sc.getZoneToCounty();
		Map old_new = sc.getZoneLinkold_new();
		Map new_old = new TreeMap(col);
		Map country_zones = new TreeMap(col);
		for (Iterator it = zone_countries.keySet().iterator(); it.hasNext();) {
			String zone = (String)it.next();
			new_old.put(zone, new TreeSet(col));
			String country = (String) zone_countries.get(zone);
			String name = english.getName("territory", country) + " (" + country + ")";
			Set oldSet = (Set) country_zones.get(name);
			if (oldSet == null) country_zones.put(name, oldSet = new TreeSet(col));
			oldSet.add(zone);
		}
		for (Iterator it = old_new.keySet().iterator(); it.hasNext();) {
			String oldOne = (String) it.next();
			String newOne = (String) old_new.get(oldOne);
			Set oldSet = (Set) new_old.get(newOne);
			if (false && oldSet == null) {
				System.out.println("Warning: missing zone: " + newOne);
				new_old.put(newOne, oldSet = new TreeSet(col));
			}
			oldSet.add(oldOne);
		}
		for (Iterator it3 = country_zones.keySet().iterator(); it3.hasNext();) {
			String country = (String) it3.next();
			System.out.println(country);
			Set zones = (Set)country_zones.get(country);
			for (Iterator it = zones.iterator(); it.hasNext();) {
				String newOne = (String) it.next();
				System.out.println("    tzid:\t" + newOne);
				Set oldSet = (Set) new_old.get(newOne);
				for (Iterator it2 = oldSet.iterator(); it2.hasNext();) {
					String oldOne = (String) it2.next();
					System.out.println("        alias:\t" + oldOne);
				}
			}
		}
	}

	static void printWindowsZones() {
		System.out.println("\t<timezoneData>");
		System.out.println("\t\t<mapTimezones type=\"windows\">");
		for (int i = 0; i < ZONE_MAP.length; i += 3) {
			System.out.println("\t\t\t<mapZone other=\"" + ZONE_MAP[i+1] 
				+ "\" type=\"" + ZONE_MAP[i] 
				+ "\"/> <!-- " + ZONE_MAP[i+2] + "-->");
		}
		System.out.println("\t\t</mapTimezones>");
		System.out.println("\t</timezoneData>");
		
		for (int i = 0; i < ZONE_MAP.length; i += 3) {
			int p1 = ZONE_MAP[i+2].indexOf('(');
			int p2 = ZONE_MAP[i+2].indexOf(')');
			System.out.println(
					ZONE_MAP[i] 
							 + "\t" + 	ZONE_MAP[i+1] 
													+ "\t" + ZONE_MAP[i+2].substring(0,p1)
													+ "\t" + ZONE_MAP[i+2].substring(p1+1,p2)
													+ "\t" + ZONE_MAP[i+2].substring(p2+1)
				);
		}

	}
    
    static String[] ZONE_MAP = {
        "Etc/GMT+12",           "Dateline", "S (GMT-12:00) International Date Line West",

        "Pacific/Apia",         "Samoa", "S (GMT-11:00) Midway Island, Samoa",

        "Pacific/Honolulu",     "Hawaiian", "S (GMT-10:00) Hawaii",

        "America/Anchorage",    "Alaskan", "D (GMT-09:00) Alaska",

        "America/Los_Angeles",  "Pacific", "D (GMT-08:00) Pacific Time (US & Canada); Tijuana",

        "America/Phoenix",      "US Mountain", "S (GMT-07:00) Arizona",
        "America/Denver",       "Mountain", "D (GMT-07:00) Mountain Time (US & Canada)",
        "America/Chihuahua",    "Mexico Standard Time 2", "D (GMT-07:00) Chihuahua, La Paz, Mazatlan",

        "America/Managua",      "Central America", "S (GMT-06:00) Central America",
        "America/Regina",       "Canada Central", "S (GMT-06:00) Saskatchewan",
        "America/Mexico_City",  "Mexico", "D (GMT-06:00) Guadalajara, Mexico City, Monterrey",
        "America/Chicago",      "Central", "D (GMT-06:00) Central Time (US & Canada)",

        "America/Indianapolis", "US Eastern", "S (GMT-05:00) Indiana (East)",
        "America/Bogota",       "SA Pacific", "S (GMT-05:00) Bogota, Lima, Quito",
        "America/New_York",     "Eastern", "D (GMT-05:00) Eastern Time (US & Canada)",

        "America/Caracas",      "SA Western", "S (GMT-04:00) Caracas, La Paz",
        "America/Santiago",     "Pacific SA", "D (GMT-04:00) Santiago",
        "America/Halifax",      "Atlantic", "D (GMT-04:00) Atlantic Time (Canada)",

        "America/St_Johns",     "Newfoundland", "D (GMT-03:30) Newfoundland",

        "America/Buenos_Aires", "SA Eastern", "S (GMT-03:00) Buenos Aires, Georgetown",
        "America/Godthab",      "Greenland", "D (GMT-03:00) Greenland",
        "America/Sao_Paulo",    "E. South America", "D (GMT-03:00) Brasilia",

        "America/Noronha",      "Mid-Atlantic", "D (GMT-02:00) Mid-Atlantic",

        "Atlantic/Cape_Verde",  "Cape Verde", "S (GMT-01:00) Cape Verde Is.",
        "Atlantic/Azores",      "Azores", "D (GMT-01:00) Azores",

        "Africa/Casablanca",    "Greenwich", "S (GMT) Casablanca, Monrovia",
        "Europe/London",        "GMT", "D (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London",

        "Africa/Lagos",         "W. Central Africa", "S (GMT+01:00) West Central Africa",
        "Europe/Berlin",        "W. Europe", "D (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna",
        "Europe/Paris",         "Romance", "D (GMT+01:00) Brussels, Copenhagen, Madrid, Paris",
        "Europe/Sarajevo",      "Central European", "D (GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb",
        "Europe/Belgrade",      "Central Europe", "D (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague",

        "Africa/Johannesburg",  "South Africa", "S (GMT+02:00) Harare, Pretoria",
        "Asia/Jerusalem",       "Israel", "S (GMT+02:00) Jerusalem",
        "Europe/Istanbul",      "GTB", "D (GMT+02:00) Athens, Istanbul, Minsk",
        "Europe/Helsinki",      "FLE", "D (GMT+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius",
        "Africa/Cairo",         "Egypt", "D (GMT+02:00) Cairo",
        "Europe/Bucharest",     "E. Europe", "D (GMT+02:00) Bucharest",

        "Africa/Nairobi",       "E. Africa", "S (GMT+03:00) Nairobi",
        "Asia/Riyadh",          "Arab", "S (GMT+03:00) Kuwait, Riyadh",
        "Europe/Moscow",        "Russian", "D (GMT+03:00) Moscow, St. Petersburg, Volgograd",
        "Asia/Baghdad",         "Arabic", "D (GMT+03:00) Baghdad",

        "Asia/Tehran",          "Iran", "D (GMT+03:30) Tehran",

        "Asia/Muscat",          "Arabian", "S (GMT+04:00) Abu Dhabi, Muscat",
        "Asia/Tbilisi",         "Caucasus", "D (GMT+04:00) Baku, Tbilisi, Yerevan",

        "Asia/Kabul",           "Afghanistan", "S (GMT+04:30) Kabul",

        "Asia/Karachi",         "West Asia", "S (GMT+05:00) Islamabad, Karachi, Tashkent",
        "Asia/Yekaterinburg",   "Ekaterinburg", "D (GMT+05:00) Ekaterinburg",

        "Asia/Calcutta",        "India", "S (GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi",

        "Asia/Katmandu",        "Nepal", "S (GMT+05:45) Kathmandu",

        "Asia/Colombo",         "Sri Lanka", "S (GMT+06:00) Sri Jayawardenepura",
        "Asia/Dhaka",           "Central Asia", "S (GMT+06:00) Astana, Dhaka",
        "Asia/Novosibirsk",     "N. Central Asia", "D (GMT+06:00) Almaty, Novosibirsk",

        "Asia/Rangoon",         "Myanmar", "S (GMT+06:30) Rangoon",

        "Asia/Bangkok",         "SE Asia", "S (GMT+07:00) Bangkok, Hanoi, Jakarta",
        "Asia/Krasnoyarsk",     "North Asia", "D (GMT+07:00) Krasnoyarsk",

        "Australia/Perth",      "W. Australia", "S (GMT+08:00) Perth",
        "Asia/Taipei",          "Taipei", "S (GMT+08:00) Taipei",
        "Asia/Singapore",       "Singapore", "S (GMT+08:00) Kuala Lumpur, Singapore",
        "Asia/Hong_Kong",       "China", "S (GMT+08:00) Beijing, Chongqing, Hong Kong, Urumqi",
        "Asia/Irkutsk",         "North Asia East", "D (GMT+08:00) Irkutsk, Ulaan Bataar",

        "Asia/Tokyo",           "Tokyo", "S (GMT+09:00) Osaka, Sapporo, Tokyo",
        "Asia/Seoul",           "Korea", "S (GMT+09:00) Seoul",
        "Asia/Yakutsk",         "Yakutsk", "D (GMT+09:00) Yakutsk",

        "Australia/Darwin",     "AUS Central", "S (GMT+09:30) Darwin",
        "Australia/Adelaide",   "Cen. Australia", "D (GMT+09:30) Adelaide",

        "Pacific/Guam",         "West Pacific", "S (GMT+10:00) Guam, Port Moresby",
        "Australia/Brisbane",   "E. Australia", "S (GMT+10:00) Brisbane",
        "Asia/Vladivostok",     "Vladivostok", "D (GMT+10:00) Vladivostok",
        "Australia/Hobart",     "Tasmania", "D (GMT+10:00) Hobart",
        "Australia/Sydney",     "AUS Eastern", "D (GMT+10:00) Canberra, Melbourne, Sydney",

        "Asia/Magadan",         "Central Pacific", "S (GMT+11:00) Magadan, Solomon Is., New Caledonia",

        "Pacific/Fiji",         "Fiji", "S (GMT+12:00) Fiji, Kamchatka, Marshall Is.",
        "Pacific/Auckland",     "New Zealand", "D (GMT+12:00) Auckland, Wellington",

        "Pacific/Tongatapu",    "Tonga", "S (GMT+13:00) Nuku'alofa",
    };
	
	/**
	 * @throws IOException
	 * 
	 */
	private static void printCurrentTimezoneLocalizations(PrintWriter log, String locale) throws IOException {
		StandardCodes sc = StandardCodes.make();
		
		Map linkNew_Old = sc.getZoneLinkNew_OldSet();
		TimezoneFormatter tzf = new TimezoneFormatter(cldrFactory, locale, true);
		/*
		<hourFormat>+HHmm;-HHmm</hourFormat>
		<hoursFormat>{0}/{1}</hoursFormat>
		<gmtFormat>GMT{0}</gmtFormat>
		<regionFormat>{0}</regionFormat>
		<fallbackFormat>{0} ({1})</fallbackFormat>
		<abbreviationFallback type="standard"/>
		<preferenceOrdering type="America/Mexico_City America/Chihuahua America/New_York">
		*/
		RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(new ULocale(locale));
		col.setNumericCollation(true);
		Set orderedAliases = new TreeSet(col);
		
		Map zone_countries = StandardCodes.make().getZoneToCounty();
		Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();

		Map reordered = new TreeMap(col);
		CLDRFile desiredLocaleFile = cldrFactory.make(locale, true);

		for (Iterator it = zone_countries.keySet().iterator(); it.hasNext();) {
			String zoneID = (String) it.next();
			String country = (String) zone_countries.get(zoneID);
			String countryName = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country);
			if (countryName == null) countryName = UTF16.valueOf(0x10FFFD) + country;
			reordered.put(countryName + "0" + zoneID, zoneID);
		}
		
		String[] field = new String[TimezoneFormatter.TYPE_LIMIT];
		boolean first = true;
		int count = 0;
		for (Iterator it = reordered.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			String zoneID = (String) reordered.get(key);
			String country = (String) zone_countries.get(zoneID);
			String countryName = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country);
			if (countryName == null) countryName = country;
			log.println("<tr><th class='ID' colspan=\"4\"><table><tr><th class='I'>"
					+ (++count) + "</th><th class='T'>" + TransliteratorUtilities.toHTML.transliterate(countryName)
					+ "</th><th class='I'>\u200E" +TransliteratorUtilities.toHTML.transliterate(zoneID));
			Set s = (Set) linkNew_Old.get(zoneID);
			if (s != null) {
				log.println("\u200E</th><td class='A'>\u200E");
				orderedAliases.clear();
				orderedAliases.addAll(s);
				boolean first2 = true;
				for (Iterator it9 = s.iterator(); it9.hasNext();) {
					String alias = (String)it9.next();
					if (first2) first2 = false;
					else log.println("; ");
					log.print(TransliteratorUtilities.toHTML.transliterate(alias));
				}
			}
			log.print("\u200E</td></tr></table></th></tr>");
			if (first) {
				first = false;
				log.println("<tr><th width=\"25%\">&nbsp;</th><th width=\"25%\">generic</th><th width=\"25%\">standard</th><th width=\"25%\">daylight</th></tr>");				
			} else {
				log.println("<tr><th>&nbsp;</th><th>generic</th><th>standard</th><th>daylight</th></tr>");
			}
			for (int i = 0; i < TimezoneFormatter.LENGTH_LIMIT; ++i) {
				log.println("<tr><th>" + TimezoneFormatter.LENGTH.get(i) + "</th>");
				for (int j = 0; j < TimezoneFormatter.TYPE_LIMIT; ++j) {
					field[j] = TransliteratorUtilities.toHTML.transliterate(tzf.getFormattedZone(zoneID, i, j, 0, false));
				}
				if (field[0].equals(field[1]) && field[1].equals(field[2])) {
					log.println("<td colspan=\"3\">" + field[0] + "</td>");
				} else {
					for (int j = 0; j < TimezoneFormatter.TYPE_LIMIT; ++j) {
						log.println("<td>" + field[j] + "</td>");			
					}
				}
				log.println("</tr>");
			}
		}
	}

	void showOrderedTimezones() {
		StandardCodes sc = StandardCodes.make();
		String world = sc.getData("territory", "001");
	}
	
	static CldrUtility.VariableReplacer langTag = new CldrUtility.VariableReplacer()
		.add("$alpha", "[a-zA-Z]")
		.add("$digit", "[0-9]")
		.add("$alphanum", "[a-zA-Z0-9]")
		.add("$x", "[xX]")
		.add("$grandfathered", "en-GB-oed" +
				"|i-(?:ami|bnn|default|enochian|hak|klingon|lux|mingo|navajo|pwn|tao|tay|tsu)" +
				"|no-(?:bok|nyn)" +
				"|sgn-(?:BE-(?:fr|nl)|CH-de)" +
				"|zh-(?:gan|min(?:-nan)?|wuu|yue)")
		.add("$lang", "$alpha{2,8}")
		.add("$extlang", "(?:-$alpha{3})") // *3("-" 3ALPHA)
		.add("$script", "(?:-$alpha{4})") // ["-" script],  4ALPHA 
		.add("$region", "(?:-$alpha{2}|-$digit{3})") // ["-" region],  2ALPHA   / 3DIGIT
		.add("$variant", "(?:-$digit$alphanum{3}|-$alphanum{5,8})") // *("-" variant), 5*8alphanum  / (DIGIT 3alphanum)
		.add("$extension", "(?:-[$alphanum&&[^xX]](?:-$alphanum{2,8})+)")
		.add("$privateuse", "(?:$x(?:-$alphanum{1,8})+)")
		.add("$privateuse2", "(?:-$privateuse)");
	static String langTagPattern = langTag.replace(			
			"($lang)"
				+ CldrUtility.LINE_SEPARATOR + "\t($extlang{0,3})"
				+ CldrUtility.LINE_SEPARATOR + "\t($script?)"
				+ CldrUtility.LINE_SEPARATOR + "\t($region?)"
				+ CldrUtility.LINE_SEPARATOR + "\t($variant*)"
				+ CldrUtility.LINE_SEPARATOR + "\t($extension*)"
				+ CldrUtility.LINE_SEPARATOR + "\t($privateuse2?)"
			+ CldrUtility.LINE_SEPARATOR + "|($grandfathered)"
			+ CldrUtility.LINE_SEPARATOR + "|($privateuse)"
			);
	static String cleanedLangTagPattern = langTagPattern.replaceAll("[\\r\\t\\n\\s]","");
	static Matcher regexLanguageTagOld = Pattern.compile(cleanedLangTagPattern).matcher("");
	
	public static void getZoneData() {
		StandardCodes sc = StandardCodes.make();
		System.out.println("Links: Old->New");
		Map m = sc.getZoneLinkold_new();
		int count = 0;
		for (Iterator it = m.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			String newOne = (String) m.get(key);
			System.out.println(++count + "\t" + key + " => " + newOne);
		}
		count = 0;
		System.out.println();
		System.out.println("Links: Old->New, not final");
		Set oldIDs = m.keySet();
		for (Iterator it = oldIDs.iterator(); it.hasNext();) {
			++count;
			String key = (String) it.next();
			String newOne = (String) m.get(key);
			String further = (String) m.get(newOne);
			if (further == null) continue;
			while (true) {
				String temp = (String) m.get(further);
				if (temp == null) break;
				further = temp;
			}
			System.out.println(count + "\t" + key + " => " + newOne + " # NOT FINAL => " + further);			
		}
		
		m = sc.getZone_rules();
		System.out.println();
		System.out.println("Zones with old IDs");
		for (Iterator it = m.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			if (oldIDs.contains(key)) System.out.println(key);
		}

		Set modernIDs = sc.getZoneData().keySet();
		System.out.println();
		System.out.println("Zones without countries");
		TreeSet temp = new TreeSet(m.keySet());
		temp.removeAll(modernIDs);
		System.out.println(temp);

		Set countries = sc.getAvailableCodes("territory");
		System.out.println();
		System.out.println("Countries without zones");
		temp.clear();
		temp.addAll(countries);
		temp.removeAll(sc.getOld3166());
		for (Iterator it = sc.getZoneData().values().iterator(); it.hasNext();) {
			Object x = it.next();
			List list  = (List) x;
			temp.remove(list.get(2));
		}
		for (Iterator it = temp.iterator(); it.hasNext();) {
			String item = (String) it.next();
			if (UCharacter.isDigit(item.charAt(0))) it.remove();
		}
		System.out.println(temp);

		System.out.println();
		System.out.println("Zone->RulesIDs");
		m = sc.getZone_rules();
		for (Iterator it = m.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			System.out.println(key + " => " + XPathParts.NEWLINE + "\t" 
					+ getSeparated((Collection) m.get(key), XPathParts.NEWLINE + "\t"));
		}
    
		System.out.println();
		System.out.println("RulesID->Rules");
		m = sc.getZoneRuleID_rules();
		for (Iterator it = m.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			System.out.println(key + " => " + XPathParts.NEWLINE + "\t" 
					+ getSeparated((Collection) m.get(key), XPathParts.NEWLINE + "\t"));
		}
    
    System.out.println();
    System.out.println("ZoneIDs->Abbreviations");

    // now get all the abbreviations
    //Map rule_abbreviations = getAbbreviations(m);
    
    Map ruleID_Rules = sc.getZoneRuleID_rules();
    Map abb_zones = new TreeMap();
    m = sc.getZone_rules();
    for (Iterator it = m.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      Set abbreviations = new TreeSet();
      //rule_abbreviations.put(key, abbreviations);
      ZoneLine lastZoneLine = null;
      
      for (Iterator it2 = ((Collection) m.get(key)).iterator(); it2.hasNext();) {
        ZoneLine zoneLine = (ZoneLine) it2.next();
        int thisYear = zoneLine.untilYear;
        String format = zoneLine.format;
        if (format.indexOf('/') >= 0) {
          Collection abb = Arrays.asList(format.split("/"));
          for (Iterator it3 = abb.iterator(); it3.hasNext();) {
            add(abbreviations, format.replaceAll("%s", it3.next().toString()), key, lastZoneLine, zoneLine);
          }
        } else if (format.indexOf('%') >= 0) {
          Set abb = getAbbreviations(ruleID_Rules, lastZoneLine, zoneLine);
          if (abb.size() == 0) {
            System.out.println("??? Didn't find %s values for " + format + " under " + key 
                + ";" + CldrUtility.LINE_SEPARATOR + "\tLast:" + lastZoneLine + ";" + CldrUtility.LINE_SEPARATOR + "\tCurrent: " + zoneLine);
            abb = getAbbreviations(ruleID_Rules, lastZoneLine, zoneLine);
          }

          if (abb == null) {
            System.out.println("??? " + zoneLine.rulesSave);
            add(abbreviations, format, key, lastZoneLine, zoneLine);
          } else {
            for (Iterator it3 = abb.iterator(); it3.hasNext();) {
              add(abbreviations, format.replaceAll("%s", it3.next().toString()), key, lastZoneLine, zoneLine);
            }
          }
        } else {
          add(abbreviations, format, key, lastZoneLine, zoneLine);
        }
        lastZoneLine = zoneLine;
      }
      for (Iterator it3 = abbreviations.iterator(); it3.hasNext();) {
        String abb = (String) it3.next();
        if (abb.equals("")) {
          it3.remove();
          continue;
        }
        Set zones = (Set) abb_zones.get(abb);
        if (zones == null) abb_zones.put(abb, zones = new TreeSet());
        zones.add(key);
      }
      System.out.println(key + " => " + XPathParts.NEWLINE + "\t" 
          + getSeparated(abbreviations, XPathParts.NEWLINE + "\t"));
    }
    
    System.out.println();
    System.out.println("Abbreviations->ZoneIDs");
    for (Iterator it = abb_zones.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      System.out.println(key + " => " + XPathParts.NEWLINE + "\t" 
          + getSeparated((Collection) abb_zones.get(key), XPathParts.NEWLINE + "\t"));
    }
    
    System.out.println("Types: " + ZoneParser.RuleLine.types);
    System.out.println("Saves: " + ZoneParser.RuleLine.days);
    System.out.println("untilDays: " + ZoneParser.ZoneLine.untilDays);
    System.out.println("rulesSaves: " + ZoneParser.ZoneLine.rulesSaves);

	}

  private static void add(Set abbreviations, String format, String zone, ZoneLine lastZoneLine, ZoneLine zoneLine) {
    if (format.length() < 3) {
    System.out.println("??? Format too short: '" + format + "' under " + zone 
        + ";" + CldrUtility.LINE_SEPARATOR + "\tLast:" + lastZoneLine + ";" + CldrUtility.LINE_SEPARATOR + "\tCurrent: " + zoneLine);
    return;
    }
    abbreviations.add(format);
  }

  private static Set getAbbreviations(Map rules, ZoneLine lastZoneLine, ZoneLine zoneLine) {
    Set result = new TreeSet();
    List ruleList = (List) rules.get(zoneLine.rulesSave);
    for (Iterator it2 = ruleList.iterator(); it2.hasNext();) {
      RuleLine ruleLine = (RuleLine) it2.next();
      int from = ruleLine.fromYear;
      int to = ruleLine.toYear;
      // they overlap?
      if (zoneLine.untilYear >= from && (lastZoneLine == null || lastZoneLine.untilYear <= to)) {
        result.add(ruleLine.letter == null ? "?!?" : ruleLine.letter);
      }
    }
    return result;
  }
	
	private static String getSeparated(Collection c, String separator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (Iterator it = c.iterator(); it.hasNext();) {
			if (first) first = false;
			else result.append(separator);
			result.append(it.next());
		}
		return result.toString();
	}

	private static void getCities() throws IOException {
		StandardCodes sc = StandardCodes.make();
		Set territories = sc.getAvailableCodes("territory");
		Map zoneData = sc.getZoneData();
		
		Set s = new TreeSet(sc.getTZIDComparator());
		s.addAll(sc.getZoneData().keySet());
		int counter = 0;
		for (Iterator it = s.iterator(); it.hasNext();) {
			String key = (String) it.next();
			System.out.println(++counter + "\t" + key + "\t" + zoneData.get(key));
		}
		Set missing2 = new TreeSet(sc.getZoneData().keySet());
		missing2.removeAll(sc.getZoneToCounty().keySet());
		System.out.println(missing2);
		missing2.clear();
		missing2.addAll(sc.getZoneToCounty().keySet());
		missing2.removeAll(sc.getZoneData().keySet());
		System.out.println(missing2);
		if (true) return;
		
		Map country_city_data = new TreeMap();
		Map territoryName_code = new HashMap();
		Map zone_to_country = sc.getZoneToCounty();
		for (Iterator it = territories.iterator(); it.hasNext();) {
			String code = (String) it.next();
			territoryName_code.put(sc.getData("territory", code), code);
		}
		Transliterator t = Transliterator.getInstance(
				"hex-any/html; [\\u0022] remove");
		Transliterator t2 = Transliterator.getInstance(
		"NFD; [:m:]Remove; NFC");
		BufferedReader br = BagFormatter.openUTF8Reader("c:/data/","cities.txt");
		counter = 0;
		Set missing = new TreeSet();
		while (true) {
			String line = br.readLine();
			if (line == null) break;
			if (line.startsWith("place name")) continue;
			List list = CldrUtility.splitList(line, '\t', true);
			String place = (String)list.get(0);
			place = t.transliterate(place);
			String place2 = t2.transliterate(place);
			String country = (String)list.get(1);
			String population = (String)list.get(2);
			String latitude = (String)list.get(3);
			String longitude = (String)list.get(4);
			String country2 = (String) corrections.get(country);
			String code = (String) territoryName_code.get(country2 == null ? country : country2);
			if (code == null) missing.add(country);
			Map city_data = (Map) country_city_data.get(code);
			if (city_data == null) {
				city_data = new TreeMap();
				country_city_data.put(code,city_data);
			}
			city_data.put(place2, 
					place + "_" + population + "_" + latitude + "_" + longitude);
		}
		if (false) for (Iterator it = missing.iterator(); it.hasNext();) {
			System.out.println("\"" + it.next() + "\", \"XXX\",");
		}
		
		for (Iterator it = country_city_data.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			Map city_data = (Map) country_city_data.get(key);
			for (Iterator it2 = city_data.keySet().iterator(); it2.hasNext();) {
				String key2 = (String) it2.next();
				String value = (String) city_data.get(key2);
				System.out.println(++counter + "\t" + key + "\t"
						+ key2 + "\t" + value );
			}
		}
		for (Iterator it = zone_to_country.keySet().iterator(); it.hasNext();) {
			String zone = (String) it.next();
			if (zone.startsWith("Etc")) continue;
			String country = (String) zone_to_country.get(zone);
			Map city_data = (Map) country_city_data.get(country);
			if (city_data == null) {
				System.out.println("Missing country: " + zone + "\t" + country);
				continue;
			}
			
			List pieces = CldrUtility.splitList(zone, '/', true);
			String city = (String) pieces.get(pieces.size() - 1);
			city = city.replace('_', ' ');
			String data = (String) city_data.get(city);
			if (data != null) continue;
			System.out.println();
			System.out.println("\"" + city + "\", \"XXX\" // "
					+ zone + ",\t" + sc.getData("territory", country));
			System.out.println(city_data);
		}
	}
	
	static final String[] COUNTRY_CORRECTIONS = {
			"Antigua & Barbuda", "Antigua and Barbuda",
			"Bosnia-Herzegovina", "Bosnia and Herzegovina",
			"British Virgin Islands", "Virgin Islands, British",
			"Brunei", "Brunei Darussalam",
			"Central Africa", "Central African Republic",
			"Congo (Dem. Rep.)", "Congo, The Democratic Republic of the",
			"East Timor", "Timor-Leste",
			"External Territories of Australia", "Australia",
			"Falkland Islands and dependencies", "Falkland Islands (Malvinas)",
			"Guernsey and Alderney", "United Kingdom",
			"Guinea Bissau", "Guinea-Bissau",
			"Iran", "Iran, Islamic Republic of",
			"Ivory Coast", "Cote d'Ivoire",
			"Jersey", "United Kingdom",
			"Korea (North)", "Korea, Democratic People's Republic of",
			"Korea (South)", "Korea, Republic of",
			"Laos", "Lao People's Democratic Republic",
			"Libya", "Libyan Arab Jamahiriya",
			"Macedonia", "Macedonia, The Former Yugoslav Republic of",
			"Man (Isle of)", "United Kingdom",
			"Moldova", "Moldova, Republic of",
			"Norfolk", "Norfolk Island",
			"Palestine", "Palestinian Territory, Occupied",
			"Russia", "Russian Federation",
			"R\u9D6Eion", "Reunion",
			"Sahara", "Western Sahara",
			"Saint Pierre & Miquelon", "Saint Pierre and Miquelon",
			"Smaller Territories of the UK", "United Kingdom",
			"Syria", "Syrian Arab Republic",
			"S\u3BE0Tom\u9821nd Pr\uDBA3ipe", "Sao Tome and Principe",
			"Taiwan", "Taiwan, Province of China",
			"Tanzania", "Tanzania, United Republic of",
			"Terres Australes", "French Polynesia",
			"United States of America", "United States",
			"Vatican", "Holy See (Vatican City State)",
			"Vietnam", "Viet Nam",
			"Virgin Islands of the United States", "Virgin Islands, U.S.",
			"Wallis & Futuna", "Wallis and Futuna",
	};
	
	

	static final Map corrections = new HashMap();
	static {
		for (int i = 0; i < COUNTRY_CORRECTIONS.length; i+=2) {
			corrections.put(COUNTRY_CORRECTIONS[i], COUNTRY_CORRECTIONS[i+1]);
		}
	}

	//static PrintWriter log;
	
	private static void printSupplementalData(String locale) throws IOException {
		
		PrintWriter log = null; // BagFormatter.openUTF8Writer(options[DESTDIR].value + "", locale + "_timezonelist.xml");
		CLDRFile desiredLocaleFile = (CLDRFile) cldrFactory.make(locale, true).cloneAsThawed();
		desiredLocaleFile.removeDuplicates(resolvedRoot, false, false, null);
		
		CLDRFile english = cldrFactory.make("en", true);
		Collator col = Collator.getInstance(new ULocale(locale));
		CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
		XPathParts parts = new XPathParts(null, null);
		for (Iterator it = supp.iterator(); it.hasNext();) {
			String path = (String) it.next();
			parts.set(supp.getFullXPath(path));
			Map m = parts.findAttributes("language");
			if (m == null) continue;
			if (false) System.out.println("Type: " + m.get("type") 
					+ "\tscripts: " + m.get("scripts")
					+ "\tterritories: " + m.get("territories")
					);
		}
		
		// territories
		Map groups = new TreeMap();
		for (Iterator it = supp.iterator(); it.hasNext();) {
			String path = (String) it.next();
			parts.set(supp.getFullXPath(path));
			Map m = parts.findAttributes("territoryContainment");
			if (m == null) continue;
			Map attributes = parts.getAttributes(2);
			String type = (String) attributes.get("type");
			Collection contents = CldrUtility.splitList((String)attributes.get("contains"), ' ', true, new ArrayList());
			groups.put(type, contents);
			if (false) {
				System.out.print("\t\t<group type=\"" + fixNumericKey(type)
						+ "\" contains=\"");
				boolean first = true;
				for (Iterator it2 = contents.iterator(); it2.hasNext();) {
					if (first) first = false;
					else System.out.print(" ");
					System.out.print(fixNumericKey((String)it2.next()));
				}
				System.out.println("\"> <!--" + desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, type) + " -->");
			}
		}
		Set seen = new TreeSet();
		printTimezonesToLocalize(log, desiredLocaleFile, groups, seen, col, false, english);
		StandardCodes sc = StandardCodes.make();
		Set codes = sc.getAvailableCodes("territory");
		Set missing = new TreeSet(codes);
		missing.removeAll(seen);
		if (false) {
			if (missing.size() != 0) System.out.println("Missing: ");
			for (Iterator it = missing.iterator(); it.hasNext();) {
				String key = (String) it.next();
				//String name = english.getName(CLDRFile.TERRITORY_NAME, key, false);
				System.out.println("\t" + key + "\t" + sc.getFullData("territory", key));
			}		
		}
		if (log != null) log.close();
	}

	// <ldml><localeDisplayNames><territories>
	//		<territory type="001" draft="true">World</territory>
	// <ldml><dates><timeZoneNames>
	//		<zone type="America/Anchorage" draft="true"><exemplarCity draft="true">Anchorage</exemplarCity></zone>
	
	private static void printTimezonesToLocalize(PrintWriter log, CLDRFile localization, Map groups, Set seen, Collator col, boolean showCode, 
			CLDRFile english) throws IOException {
		Set[] missing = new Set[2];
		missing[0] = new TreeSet();
		missing[1] = new TreeSet(StandardCodes.make().getTZIDComparator());
		printWorldTimezoneCategorization(log, localization, groups, "001", 0, seen, col, showCode, zones_countrySet(), missing);
		if (missing[0].size() == 0 && missing[1].size() == 0) return;
		PrintWriter log2 = BagFormatter.openUTF8Writer(options[DESTDIR].value + "", 
				localization.getLocaleID() + "_to_localize.xml");
		log2.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		log2.println("<!DOCTYPE ldml SYSTEM \"../../common/dtd/ldml.dtd\">");
		log2.println("<ldml><identity><version number=\"" + CLDRFile.GEN_VERSION + "\"/><generation date=\"2005-01-01\"/><language type=\""
				+ TransliteratorUtilities.toXML.transliterate(localization.getLocaleID())+"\"/></identity>");
		log2.println("<!-- The following are strings that are not found in the locale (currently), " +
				"but need valid translations for localizing timezones. -->");
		if (missing[0].size() != 0) {
			log2.println("<localeDisplayNames><territories>");
			for (Iterator it = missing[0].iterator(); it.hasNext();) {
				String key = (String)it.next();
				log2.println("\t<territory type=\"" + key + "\" draft=\"unconfirmed\">"+ 
						TransliteratorUtilities.toXML.transliterate("TODO " + english.getName(CLDRFile.TERRITORY_NAME, key))
						+ "</territory>");
			}
			log2.println("</territories></localeDisplayNames>");
		}
		if (true) {
			String lastCountry = "";
			log2.println("<dates><timeZoneNames>");
			log2.println("\t<hourFormat>TODO +HHmm;-HHmm</hourFormat>");
			log2.println("\t<hoursFormat>TODO {0}/{1}</hoursFormat>");
			log2.println("\t<gmtFormat>TODO GMT{0}</gmtFormat>");
			log2.println("\t<regionFormat>TODO {0}</regionFormat>");
			log2.println("\t<fallbackFormat>TODO {0} ({1})</fallbackFormat>");
			for (Iterator it = missing[1].iterator(); it.hasNext();) {
				String key = (String)it.next();
				List data = (List) StandardCodes.make().getZoneData().get(key);
				String countryCode = (String)data.get(2);
				String country = english.getName(CLDRFile.TERRITORY_NAME, countryCode);
				if (!country.equals(lastCountry)) {
					lastCountry = country;
					log2.println("\t<!-- " + country + "-->");
				}
				log2.println("\t<zone type=\"" + key + "\"><exemplarCity draft=\"unconfirmed\">"
						+ TransliteratorUtilities.toXML.transliterate("TODO " + getName(english,key,null))
						+ "</exemplarCity></zone>");
			}
			log2.println("</timeZoneNames></dates>");
		}
		log2.println("</ldml>");
		log2.close();
	}
	
	static String[] levelNames = {"world", "continent", "subcontinent", "country", "subzone"};
	
	private static void printWorldTimezoneCategorization(PrintWriter log, CLDRFile localization, 
			Map groups, String key, int indent, Set seen, Collator col, boolean showCode, 
			Map zone_countrySet, Set[] missing) {
		//String fixedKey = fixNumericKey(key);
		seen.add(key);
		String name = getName(localization, key, missing);
		Collection s = (Collection) groups.get(key);		
		String element = levelNames[indent];
		
		if (log != null) log.print(Utility.repeat("\t", indent) + "<" + element + " n=\"" + name + (showCode ? " (" + key + ")" : "") + "\"");
		boolean gotZones = true;
		if (s == null) {
			s = (Collection) zone_countrySet.get(key);
			if (s == null || s.size() == 1) s = null; // skip singletons
			else gotZones = true;			
		}
		if (s == null) {
			if (log != null) log.println("/>");
			return;
		}
		
		if (log != null) log.println(">");
		Map reorder = new TreeMap(col);
		for (Iterator it = s.iterator(); it.hasNext();) {
			key = (String) it.next();
			String value = getName(localization, key, missing);
			if (value == null) {
				System.out.println("Missing value for: " + key);
				value = key;
			}
			reorder.put(value, key);
		}		
		for (Iterator it = reorder.keySet().iterator(); it.hasNext();) {
			key = (String) it.next();
			String value = (String) reorder.get(key);
			printWorldTimezoneCategorization(log, localization, groups, value, indent + 1, seen, col, showCode, zone_countrySet, missing);
		}
		if (log != null) log.println(Utility.repeat("\t", indent) + "</" + element + ">");
	}
	
	/**
	 * @param localization
	 * @param key
	 * @param missing TODO
	 * @return
	 */
	private static String getName(CLDRFile localization, String key, Set[] missing) {
		String name;
		int pos = key.lastIndexOf('/');
		if (pos >= 0) {
			String v = localization.getStringValue("//ldml/dates/timeZoneNames/zone[@type=\"" + key + "\"]/exemplarCity");
			if (v != null) name = v;
			else {
			
	// <ldml><dates><timezoneNames>
	//		<zone type="America/Anchorage">
	//			<exemplarCity draft="true">Anchorage</exemplarCity>
				if (missing != null) missing[1].add(key);
				name = key.substring(pos+1);
				name = name.replace('_', ' ');
			}
		} else {
			name = localization.getName(CLDRFile.TERRITORY_NAME, key);
			if (name == null) {
				if (missing != null) missing[0].add(key);
				name = key;
			}
		}
		return name;
	}

	static Map zones_countrySet() {
		Map m = StandardCodes.make().getZoneData();
		Map result = new TreeMap();
		for (Iterator it = m.keySet().iterator(); it.hasNext(); ) {
			String tzid = (String) it.next();
			List list = (List) m.get(tzid);
			String country = (String) list.get(2);
			Set zones = (Set) result.get(country);
			if (zones == null) {
				zones = new TreeSet();
				result.put(country, zones);
			}
			zones.add(tzid);
		}
		return result;
	}
	/**
	 * @param key
	 * @return
	 */
	private static String fixNumericKey(String key) {
		//String key = (String) it.next();
		char c = key.charAt(0);
		if (c > '9') return key;
		String fixedKey = key.length() == 3 ? key : key.length() == 2 ? "0" + key : "00" + key;
		return fixedKey;
	}

	private static void compareLists() throws IOException {
		BufferedReader in = BagFormatter.openUTF8Reader("", "language_list.txt");
		String[] pieces = new String[4];
		Factory cldrFactory = Factory.make(options[SOURCEDIR].value + "main\\", ".*");
		//CLDRKey.main(new String[]{"-mde.*"});
		Set locales = cldrFactory.getAvailable();
		Set cldr = new TreeSet();
		LanguageTagParser parser = new LanguageTagParser();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			// if doesn't have exactly one _, skip
			String locale = (String)it.next();
			parser.set(locale);
			if (parser.getScript().length() == 0 && parser.getRegion().length() == 0) continue;
			if (parser.getVariants().size() > 0) continue;
			cldr.add(locale.replace('_', '-'));
		}
		
		Set tex = new TreeSet();
		while (true) {
			String line = in.readLine();
			if (line == null) break;
			line = line.trim();
			if (line.length() == 0) continue;
			int p = line.indexOf(' ');
			tex.add(line.substring(0,p));
		}
		Set inCldrButNotTex = new TreeSet(cldr);
		inCldrButNotTex.removeAll(tex);
		System.out.println(" inCldrButNotTex " + inCldrButNotTex);
		Set inTexButNotCLDR = new TreeSet(tex);
		inTexButNotCLDR.removeAll(cldr);
		System.out.println(" inTexButNotCLDR " + inTexButNotCLDR);
	}
	
	void generateTransliterators() throws IOException {
		File translitSource = new File("C:\\ICU\\icu\\source\\data\\translit");
		Matcher m = Pattern.compile(".*Hebrew.*").matcher("");
		File[] list = translitSource.listFiles();
		for (int i = 0; i < list.length; ++i) {
			File file = list[i];
			String name = file.getName();
			if (!m.reset(name).matches()) continue;
			if (!name.endsWith(".txt")) continue;
			String fixedName = name.substring(name.length()-4);
			BufferedReader input = BagFormatter.openUTF8Reader(file.getParent() + File.pathSeparator, name);
			CLDRFile outFile = new CLDRFile(null, false);
			int count = 0;
			while (true) {
				String line = input.readLine();
				String contents = line;
				if (line == null) break;
				if (line.length() == 0) continue;
				count++;
				outFile.add("//supplementalData/transforms/transform/line[@_q=\"" + count + "\"]", line);
			}
			PrintWriter pw = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY + "/translit/", fixedName + ".xml");
			outFile.write(pw);
			pw.close();
		}
	}
}
