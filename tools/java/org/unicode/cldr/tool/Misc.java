package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Value;
import org.xml.sax.SAXParseException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
//import com.ibm.icu.impl.Utility;

public class Misc {
	static Factory cldrFactory;
	// WARNING: this file needs a serious cleanup
	
	public static void main(String[] args) throws IOException {
		cldrFactory = Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*");
		String[] group1 = {
				//"en", "zh", "ja", "ko",
				//"fr", 
				"de", 
				//"it", "es", "pt"
				};
		
		//group1 = (String[]) cldrFactory.getAvailableLanguages().toArray(group1);
		if (false) for (int i = 0; i < group1.length; ++i) {
			if (!XPathParts.isLanguage(group1[i])) continue;
			if (group1[i].compareTo("sh") == 0) continue;
			PrintWriter log = BagFormatter.openUTF8Writer("C:\\DATA\\GEN\\cldr\\timezones\\", group1[i] + "_current.html");
			log.println("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
			log.println("<style><!--");
			log.println("td { text-align: center }");
			log.println("--></style>");
			log.println("<title>Time Zone Localizations for " + group1[i] + "</title><head><body>");
			log.println("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse\">");
			printTimeZones(log, group1[i]);
			//printSupplementalData(group1[i]);
			log.println("</table></body></html>");
			log.close();
		}

		for (int i = 0; i < group1.length; ++i) {
			if (!XPathParts.isLanguage(group1[i])) continue;
			if (group1[i].compareTo("sh") == 0) continue;
			printSupplementalData(group1[i]);
		}
		System.out.println("DONE");
//getCities();
		//testLanguageTags();
		//getZoneData();
	}
	
	/**
	 * @throws IOException
	 * 
	 */
	private static void printTimeZones(PrintWriter log, String locale) throws IOException {

		CLDRFile desiredLocaleFile = cldrFactory.make(locale, true);
		CLDRFile english = cldrFactory.make("en", false);
		TimezoneFormatter tzf = new TimezoneFormatter(desiredLocaleFile);
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
		Map reordered = new TreeMap(col);
		for (Iterator it = tzf.zone_countries.keySet().iterator(); it.hasNext();) {
			String zoneID = (String) it.next();
			String country = (String) tzf.zone_countries.get(zoneID);
			String countryName = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, false);
			if (countryName == null) countryName = UTF16.valueOf(0x10FFFD) + country;
			reordered.put(countryName + "0" + zoneID, zoneID);
		}
		
		String[] field = new String[tzf.TYPE_LIMIT];
		boolean first = true;
		int count = 0;
		for (Iterator it = reordered.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			String zoneID = (String) reordered.get(key);
			String country = (String) tzf.zone_countries.get(zoneID);
			String countryName = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, false);
			if (countryName == null) countryName = country;
			log.println("<tr><th bgcolor=\"silver\" colspan=\"4\" align=\"left\"><font color=\"#0000FF\">"
					+ (++count) + ". " + countryName + ": " + zoneID 
					+ "</font></th></tr>");
			if (first) {
				first = false;
				log.println("<tr><th width=\"25%\">&nbsp;</th><th width=\"25%\">generic</th><th width=\"25%\">standard</th><th width=\"25%\">daylight</th></tr>");				
			} else {
				log.println("<tr><th>&nbsp;</th><th>generic</th><th>standard</th><th>daylight</th></tr>");
			}
			for (int i = 0; i < tzf.LENGTH_LIMIT; ++i) {
				log.println("<tr><th>" + tzf.LENGTH[i] + "</th>");
				for (int j = 0; j < tzf.TYPE_LIMIT; ++j) {
					field[j] = tzf.getFormattedZone(zoneID, i, j);
				}
				if (field[0].equals(field[1]) && field[1].equals(field[2])) {
					log.println("<td colspan=\"3\">" + field[0] + "</td>");
				} else {
					for (int j = 0; j < tzf.TYPE_LIMIT; ++j) {
						log.println("<td>" + field[j] + "</td>");			
					}
				}
				log.println("</tr>");
			}
		}
	}
	
	private static class TimezoneFormatter {
		DateFormat[] hourFormat = new DateFormat[2];
		MessageFormat hoursFormat, gmtFormat, regionFormat,
				fallbackFormat;
		String abbreviationFallback, preferenceOrdering;
		CLDRFile desiredLocaleFile;
		Map zone_countries = StandardCodes.make().getZoneToCounty();
		Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

		TimezoneFormatter(CLDRFile desiredLocaleFile) {
			this.desiredLocaleFile = desiredLocaleFile;
			String hourFormatString = desiredLocaleFile.getValue("/ldml/dates/timeZoneNames/hourFormat").getStringValue();
			String[] hourFormatStrings = Utility.splitArray(hourFormatString,';');
			hourFormat[0] = new SimpleDateFormat(hourFormatStrings[0]);
			hourFormat[1] = new SimpleDateFormat(hourFormatStrings[1]);
			hoursFormat = new MessageFormat(desiredLocaleFile.getValue(
					"/ldml/dates/timeZoneNames/hoursFormat").getStringValue());
			gmtFormat = new MessageFormat(desiredLocaleFile.getValue(
					"/ldml/dates/timeZoneNames/gmtFormat").getStringValue());
			regionFormat = new MessageFormat(desiredLocaleFile.getValue(
					"/ldml/dates/timeZoneNames/regionFormat").getStringValue());
			fallbackFormat = new MessageFormat(desiredLocaleFile.getValue(
					"/ldml/dates/timeZoneNames/fallbackFormat")
					.getStringValue());
			abbreviationFallback = (String) new XPathParts(null, null).set(
					desiredLocaleFile.getValue(
							"/ldml/dates/timeZoneNames/abbreviationFallback")
							.getFullXPath()).findAttributes(
					"abbreviationFallback").get("type");
			Value temp = desiredLocaleFile.getValue("/ldml/dates/timeZoneNames/preferenceOrdering");
			preferenceOrdering = (String) new XPathParts(null, null).set(
					temp.getFullXPath()).findAttributes(
					"preferenceOrdering").get("type");
		}
		static final int GMT = 0, SHORT = 1, LONG = 2, LENGTH_LIMIT = 3;
		static final String[] LENGTH = {"gmt", "short", "long"};
		static final int GENERIC = 0, STANDARD = 1, DAYLIGHT = 2, TYPE_LIMIT = 3;
		static final String[] TYPE = {"generic", "standard", "daylight"};
		static final long D2005_01_02 = new Date(2005-1900,0,1).getTime();
		static final long D2005_06_02 = new Date(2005-1900,7,1).getTime();
		
		String getFormattedZone(String zoneid, int length, int type) {
			String result;
/*
<long>
	<generic>Alaska Time</generic>
	<standard>Alaska Standard Time</standard>
	<daylight>Alaska Daylight Time</daylight>
</long>
<short>
	<generic>AKT</generic>
	<standard>AKST</standard>
	<daylight>AKDT</daylight>
</short>
*/
			String prefix = "/ldml/dates/timeZoneNames/zone[@type=\"" + zoneid + "\"]/";
			// 1. If non-GMT format, and we have an explicit translation, use it
			if (length != GMT) {
				Value formatValue = desiredLocaleFile.getValue(prefix + LENGTH[length] + "/" + TYPE[type]);
				if (formatValue != null) return formatValue.getStringValue();
			}
			
			String country = (String) zone_countries.get(zoneid);
			// 2. if GMT format or no country, use it
			if (length == GMT || country.equals(StandardCodes.NO_COUNTRY)) {
				TimeZone tz = TimeZone.getTimeZone(zoneid);
				long gmtOffset1 = tz.getOffset(D2005_01_02);
				tz = TimeZone.getTimeZone(zoneid);
				long gmtOffset2 = tz.getOffset(D2005_06_02);
				DateFormat format = hourFormat[gmtOffset1 >= 0 ? 0 : 1];
				if (type == GENERIC && gmtOffset1 != gmtOffset2) {
					calendar.setTimeInMillis(Math.abs(gmtOffset1));
					String first = format.format(calendar);
					calendar.setTimeInMillis(Math.abs(gmtOffset2));
					String second = format.format(calendar);
					result = hoursFormat.format(new Object[]{first, second});
				} else {
					gmtOffset1 = type == STANDARD ? Math.min(gmtOffset1, gmtOffset2) : Math.max(gmtOffset1, gmtOffset2);
					calendar.setTimeInMillis(Math.abs(gmtOffset1));
					result = format.format(calendar);
				}
				return gmtFormat.format(new Object[]{result});
			}
			// 3. Else for non-wall-time, use GMT format
			//	America/Los_Angeles => "GMT-08:00"

			// 4. *Else if there is an exemplar city, use it with the region format. The exemplar city may not be the same as the Olson ID city, if another city is much more recognizable for whatever reason. 
			// However, it is very strongly recommended that the same city be used.
			//	America/Los_Angeles => "Tampo de San Fransisko"
			Value exemplarValue = desiredLocaleFile.getValue(prefix + "exemplarCity");
			if (exemplarValue != null) {
				return regionFormat.format(new Object[]{exemplarValue.getStringValue()});
			}
			// 5. *Else if there is a country for the time zone, and a translation in the locale for the country name, and the country only has one (modern) timezone, use it with the region format :
			//	Africa/Monrovia => LR => "Tampo de Liberja"
			Set s = (Set) countries_zoneSet.get(country);
			if (s != null && s.size() == 1) {
				result = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, false);
				if (result != null) return result;
			}
			// 6. Else if it is a perpetual alias for a "real" ID, and if there is an exact translation for that, try #1..#4 with that alias.
			//	Europe/San_Marino => Europe/Rome => ... => "Tampo de Roma"
			
			// TODO
			
			// 7. Else fall back to the raw Olson ID (stripping off the prefix, and turning _ into space), using the fallback format. 
			//	America/Goose_Bay => "Tampo de  «Goose Bay»"
			int pos = zoneid.lastIndexOf('/');
			result = pos < 0 ? zoneid : zoneid.substring(pos+1);
			result = result.replace('_', ' ');
			String countryTranslation = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, false);
			if (countryTranslation == null) countryTranslation = country;
			return fallbackFormat.format(new Object[]{result, countryTranslation});
			// 8. Else use the (possibly multi-offset) GMT format
			//	America/Nome => "GMT-0900/-0800"

			// TODO 
			
		}
	}

	void showOrderedTimezones() {
		StandardCodes sc = StandardCodes.make();
		String world = sc.getData("territory", "001");
	}
	
	public static class VariableReplacer {
		// simple implementation for now
		Comparator c;
		private Map m = new TreeMap(Collections.reverseOrder());
		public VariableReplacer add(String variable, String value) {
			m.put(variable, value);
			return this;
		}
		public String replace(String source) {
			String oldSource;
			do {
				oldSource = source;
				for (Iterator it = m.keySet().iterator(); it.hasNext();) {
					String variable = (String) it.next();
					String value = (String) m.get(variable);
					source = replaceAll(source, variable, value);
				}
			} while (!source.equals(oldSource));
			return source;
		}
		public String replaceAll(String source, String key, String value) {
			while (true) {
				int pos = source.indexOf(key);
				if (pos < 0) return source;
				source = source.substring(0,pos) + value + source.substring(pos+key.length());
			}
		}
	}
	
	static VariableReplacer langTag = new VariableReplacer()
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
		.add("$extlang", "(?:-$alpha{3})")
		.add("$script", "(?:-$alpha{4})")
		.add("$region", "(?:-$alpha{2}|-$digit{3})")
		.add("$variant", "(?:-$digit$alphanum{3}|-$alphanum{5,8})")
		.add("$extension", "(?:-[$alphanum&&[^xX]](?:-$alphanum{2,8})+)")
		.add("$privateuse", "(?:$x(?:-$alphanum{1,8})+)")
		.add("$privateuse2", "(?:-$privateuse)");
	static String langTagPattern = langTag.replace(			
			"($lang)"
				+ "\r\n\t($extlang*)"
				+ "\r\n\t($script?)"
				+ "\r\n\t($region?)"
				+ "\r\n\t($variant*)"
				+ "\r\n\t($extension*)"
				+ "\r\n\t($privateuse2?)"
			+ "\r\n|($grandfathered)"
			+ "\r\n|($privateuse)"
			);
	static String cleanedLangTagPattern = langTagPattern.replaceAll("[\\r\\t\\n\\s]","");
	static Matcher regexLanguageTag = Pattern.compile(cleanedLangTagPattern).matcher("");
	
	static String[] groupNames = {"whole", "lang", "extlangs", "script", "region", "variants", "extensions", 
			"privateuse", "grandfathered", "privateuse"
	};
	
	private static void testLanguageTags() {
		System.out.println(langTagPattern);
		System.out.println(cleanedLangTagPattern);
		StandardCodes sc = StandardCodes.make();
		Set grandfathered = sc.getAvailableCodes("grandfathered");
		for (Iterator it = grandfathered.iterator(); it.hasNext();) {
			System.out.print(it.next() + " | ");
		}
		LanguageTagParser ltp = new LanguageTagParser();
		String[] tests = {
				"en", 
				"en-US", 
				"en-Latn", 
				"en-Latn-US", 
				"en-enx-eny-US", 
				"x-12345678-a", 
				"en-Latn-US-lojban-gaulish",
				"en-Latn-US-lojban-gaulish-a-12345678-ABCD-b-ABCDEFGH-x-a-b-c-12345678",
				"en-Latn-001",
				"en-GB-oed", // grandfathered
				"badtagsfromhere",
				"b-fish",
				"en-UK-oed",
				"en-US-Latn", 
		};
		for (int i = 0; i < tests.length; ++i) {
			try {
				System.out.println("Parsing " + tests[i]);
				ltp.set(tests[i]);
				if (ltp.getLanguage().length() != 0) System.out.println("\tlang:    \t" + ltp.getLanguage() + (ltp.isGrandfathered() ? " (grandfathered)" : ""));
				if (ltp.getExtlangs().size() != 0) System.out.println("\textlangs:\t" + ltp.getExtlangs());
				if (ltp.getScript().length() != 0) System.out.println("\tscript:\t" + ltp.getScript());
				if (ltp.getRegion().length() != 0) System.out.println("\tregion:\t" + ltp.getRegion());
				if (ltp.getVariants().size() != 0) System.out.println("\tvariants:\t" + ltp.getVariants());
				if (ltp.getExtensions().size() != 0) System.out.println("\textensions:\t" + ltp.getExtensions());
				System.out.println("\tisValid?\t" + ltp.isValid());
			} catch (Exception e) {
				System.out.println("\t" + e.getMessage());
				System.out.println("\tisValid?\tfalse");
			}
			boolean matches = regexLanguageTag.reset(tests[i]).matches();
			System.out.println("\tregex?\t" + matches);
			if (matches) {
				for (int j = 0; j <= regexLanguageTag.groupCount(); ++j) {
					String g = regexLanguageTag.group(j);
					if (g == null || g.length() == 0) continue;
					System.out.println("\t" + j + "\t" + groupNames[j] + ":\t" + g);
				}
			}
		}
	}
	
	private static void getZoneData() {
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
	}
	
	public static String getSeparated(Collection c, String separator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (Iterator it = c.iterator(); it.hasNext();) {
			if (first) first = false;
			else result.append(separator);
			result.append(it.next());
		}
		return result.toString();
	}

	public static void getCities() throws IOException {
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
			List list = Utility.splitList(line, '\t', true);
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
			
			List pieces = Utility.splitList(zone, '/', true);
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
		
		PrintWriter log = null; // BagFormatter.openUTF8Writer("C:\\DATA\\GEN\\cldr\\timezones\\", locale + "_timezonelist.xml");
		CLDRFile desiredLocaleFile = cldrFactory.make(locale, true);
		CLDRFile english = cldrFactory.make("en", true);
		Collator col = Collator.getInstance(new ULocale(locale));
		CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
		XPathParts parts = new XPathParts(null, null);
		for (Iterator it = supp.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			Value v = supp.getValue(path);
			parts.set(v.getFullXPath());
			Map m = parts.findAttributes("language");
			if (m == null) continue;
			if (false) System.out.println("Type: " + m.get("type") 
					+ "\tscripts: " + m.get("scripts")
					+ "\tterritories: " + m.get("territories")
					);
		}
		
		// territories
		Map groups = new TreeMap();
		for (Iterator it = supp.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			Value v = supp.getValue(path);
			parts.set(v.getFullXPath());
			Map m = parts.findAttributes("territoryContainment");
			if (m == null) continue;
			Map attributes = parts.getAttributes(2);
			String type = (String) attributes.get("type");
			Collection contents = Utility.splitList((String)attributes.get("contains"), ' ', true, new ArrayList());
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
				System.out.println("\"> <!--" + desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, type, false) + " -->");
			}
		}
		Set seen = new TreeSet();
		show(log, desiredLocaleFile, groups, seen, col, false, english);
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
	
	private static void show(PrintWriter log, CLDRFile localization, Map groups, Set seen, Collator col, boolean showCode, 
			CLDRFile english) throws IOException {
		Set[] missing = new Set[2];
		missing[0] = new TreeSet();
		missing[1] = new TreeSet(StandardCodes.make().getTZIDComparator());
		show2(log, localization, groups, "001", 0, seen, col, showCode, zones_countrySet(), missing);
		if (missing[0].size() == 0 && missing[1].size() == 0) return;
		PrintWriter log2 = BagFormatter.openUTF8Writer("C:\\DATA\\GEN\\cldr\\timezones\\", 
				localization.getKey() + "_to_localize.xml");
		log2.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		log2.println("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.3/ldml.dtd\">");
		log2.println("<ldml><identity><version number=\"1.3\"/><generation date=\"2005-01-01\"/><language type=\""
				+ localization.getKey()+"\"/></identity>");
		log2.println("<!-- The following are strings that are not found in the locale (currently), " +
				"but need valid translations for localizing timezones. -->");
		if (missing[0].size() != 0) {
			log2.println("<localeDisplayNames><territories>");
			for (Iterator it = missing[0].iterator(); it.hasNext();) {
				String key = (String)it.next();
				log2.println("\t<territory type=\"" + key + "\" draft=\"true\">"+ 
						english.getName(CLDRFile.TERRITORY_NAME, key, false)
						+ "</territory>");
			}
			log2.println("</territories></localeDisplayNames>");
		}
		if (true) {
			String lastCountry = "";
			log2.println("<dates><timeZoneNames>");
			log2.println("\t<hourFormat>+HHmm;-HHmm</hourFormat>");
			log2.println("\t<hoursFormat>{0}/{1}</hoursFormat>");
			log2.println("\t<gmtFormat>GMT{0}</gmtFormat>");
			log2.println("\t<regionFormat>{0}</regionFormat>");
			log2.println("\t<fallbackFormat>{0} ({1})</fallbackFormat>");
			for (Iterator it = missing[1].iterator(); it.hasNext();) {
				String key = (String)it.next();
				List data = (List) StandardCodes.make().getZoneData().get(key);
				String countryCode = (String)data.get(2);
				String country = english.getName(CLDRFile.TERRITORY_NAME, countryCode, false);
				if (!country.equals(lastCountry)) {
					lastCountry = country;
					log2.println("\t<!-- " + country + "-->");
				}
				log2.println("\t<zone type=\"" + key + "\"><exemplarCity draft=\"true\">"
						+ getName(english,key,null)
						+ "</exemplarCity></zone>");
			}
			log2.println("</timeZoneNames></dates>");
		}
		log2.println("</ldml>");
		log2.close();
	}
	
	static String[] levelNames = {"world", "continent", "subcontinent", "country", "subzone"};
	
	private static void show2(PrintWriter log, CLDRFile localization, Map groups, String key, int indent, Set seen, Collator col, boolean showCode, 
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
			show2(log, localization, groups, value, indent + 1, seen, col, showCode, zone_countrySet, missing);
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
			Value v = localization.getValue("/ldml/dates/timeZoneNames/zone[@type=\"" + key + "\"]/exemplarCity");
			if (v != null) name = v.getStringValue();
			else {
			
	// <ldml><dates><timezoneNames>
	//		<zone type="America/Anchorage">
	//			<exemplarCity draft="true">Anchorage</exemplarCity>
				if (missing != null) missing[1].add(key);
				name = key.substring(pos+1);
				name = name.replace('_', ' ');
			}
		} else {
			name = localization.getName(CLDRFile.TERRITORY_NAME, key, false);
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
		Factory cldrFactory = Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*");
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
}