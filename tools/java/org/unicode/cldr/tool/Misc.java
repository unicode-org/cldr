package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Value;
import org.xml.sax.SAXParseException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.TimeZone;
//import com.ibm.icu.impl.Utility;

public class Misc {
	public static void main(String[] args) throws IOException {
			//printSupplementalData();
			getCities();
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
		missing2.removeAll(sc.getZoneToCountries().keySet());
		System.out.println(missing2);
		missing2.clear();
		missing2.addAll(sc.getZoneToCountries().keySet());
		missing2.removeAll(sc.getZoneData().keySet());
		System.out.println(missing2);
		if (true) return;
		
		Map country_city_data = new TreeMap();
		Map territoryName_code = new HashMap();
		Map zone_to_country = sc.getZoneToCountries();
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
			List list = Utility.split(line, '\t', true);
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
			
			List pieces = Utility.split(zone, '/', true);
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

	private static void printSupplementalData() {
		Factory cldrFactory = Factory.make("C:\\ICU4C\\locale\\common\\main\\", ".*");
		CLDRFile english = cldrFactory.make("en", false);
		CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
		XPathParts parts = new XPathParts(null, null);
		for (Iterator it = supp.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			Value v = supp.getValue(path);
			parts.set(v.getFullXPath());
			Map m = parts.findAttributes("language");
			if (m == null) continue;
			System.out.println("Type: " + m.get("type") 
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
			Collection contents = Utility.split((String)attributes.get("contains"), ' ', true, new ArrayList());
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
				System.out.println("\"> <!--" + english.getName(CLDRFile.TERRITORY_NAME, type, false) + " -->");
			}
		}
		Set seen = new TreeSet();
		show(english, groups, seen);
		StandardCodes sc = StandardCodes.make();
		Set codes = sc.getAvailableCodes("territory");
		Set missing = new TreeSet(codes);
		missing.removeAll(seen);
		if (missing.size() != 0) System.out.println("Missing: ");
		for (Iterator it = missing.iterator(); it.hasNext();) {
			String key = (String) it.next();
			//String name = english.getName(CLDRFile.TERRITORY_NAME, key, false);
			System.out.println("\t" + key + "\t" + sc.getFullData("territory", key));
		}		
	}

	private static void show(CLDRFile localization, Map groups, Set seen) {
		show2(localization, groups, "001", 0, seen);
	}
	private static void show2(CLDRFile localization, Map groups, String key, int indent, Set seen) {
		//String fixedKey = fixNumericKey(key);
		seen.add(key);
		String name = localization.getName(CLDRFile.TERRITORY_NAME, key, false);
		System.out.println(Utility.repeat("\t", indent) + name + " (" + key + ")");
		Collection s = (Collection) groups.get(key);
		if (s == null) return;
		Map reorder = new TreeMap();
		for (Iterator it = s.iterator(); it.hasNext();) {
			key = (String) it.next();
			reorder.put(localization.getName(CLDRFile.TERRITORY_NAME, key, false), key);
		}		
		for (Iterator it = reorder.keySet().iterator(); it.hasNext();) {
			key = (String) it.next();
			String value = (String) reorder.get(key);
			show2(localization, groups, value, indent + 1, seen);
		}
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
		LocaleIDParser parser = new LocaleIDParser();
		for (Iterator it = locales.iterator(); it.hasNext();) {
			// if doesn't have exactly one _, skip
			String locale = (String)it.next();
			parser.set(locale);
			if (parser.getScript().length() == 0 && parser.getRegion().length() == 0) continue;
			if (parser.getVariants().length > 0) continue;
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