/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.attribute.UnmodifiableSetException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;


/**
 * Provides access to various codes used by CLDR: RFC 3066, ISO 4217, Olson tzids
 */
public class StandardCodes {
	public static final String NO_COUNTRY = "001";

	private static String DATA_DIR = "C:/ICU4C/locale/tools/java/org/unicode/cldr/util/";
	private static StandardCodes singleton;
	private Map type_code_data = new TreeMap();
	private Map type_data_codes = new TreeMap();
	private Map type_code_preferred = new TreeMap();
	private String date;
	
	/**
	 * Get the singleton copy of the standard codes.
	 * @return
	 * @throws IOException
	 */
	static public synchronized StandardCodes make() {
		if (singleton == null) singleton = new StandardCodes();
		return singleton;
	}
	
	/**
	 * The data is the name in the case of RFC3066 codes, and the country code in the case of TZIDs and ISO currency codes.
	 * If the country code is missing, uses ZZ.
	 * @param type
	 * @param code
	 * @return
	 */
	public String getData(String type, String code) {
		Map code_data = (Map) type_code_data.get(type);
		if (code_data == null) return null;
		List list = (List)code_data.get(code);
		return (String)list.get(0);
	}
	
	/**
	 * @return the full data for the type and code
	 */
	public List getFullData(String type, String code) {
		Map code_data = (Map) type_code_data.get(type);
		if (code_data == null) return null;
		return (List)code_data.get(code);
	}

	
	/**
	 * Return the list of codes that have the same data. For example, returns all currency codes for a country
	 * If there is a preferred one, it is first.
	 * @param type
	 * @param data
	 * @return
	 */
	public List getCodes(String type, String data) {
		Map data_codes = (Map) type_data_codes.get(type);
		if (data_codes == null) return null;
		return Collections.unmodifiableList((List)data_codes.get(data));
	}

	/**
	 * Where there is a preferred code, return it.
	 * @param type
	 * @param code
	 * @return
	 */
	public String getPreferred(String type, String code) {
		Map code_preferred = (Map) type_code_preferred.get(type);
		if (code_preferred == null) return code;
		String newCode = (String) code_preferred.get(code);
		if (newCode == null) return code;
		return newCode;
	}

	/**
	 * Get all the available types
	 * @param type
	 * @return
	 */
	public Set getAvailableTypes() {
		return Collections.unmodifiableSet(type_code_data.keySet());
	}

	/**
	 * Get all the available codes for a given type
	 * @param type
	 * @return
	 */
	public Set getAvailableCodes(String type) {
		Map code_name = (Map) type_code_data.get(type);
		if (code_name == null) return null;
		return Collections.unmodifiableSet(code_name.keySet());
	}
	
	// ========== PRIVATES ==========

	private StandardCodes() {
		String[] files = {"lstreg.txt", "ISO4217.txt", "TZID.txt"};
		type_code_preferred.put("tzid", new TreeMap());
		add("script", "Qaai", "Inherited");
		add("script", "Zyyy", "Common");
		add("language", "root", "Root");
		String line = null;
		for (int j = 0; j < files.length; ++j) {
			try {
				BufferedReader lstreg = BagFormatter.openUTF8Reader(DATA_DIR, files[j]);
				while (true) {
					line = lstreg.readLine();
					if (line == null) break;
					int commentPos = line.indexOf('#');
					if (commentPos >= 0) line = line.substring(0, commentPos);
					if (line.length() == 0) continue;
					List pieces = (List) Utility.splitList(line, '|', true, new ArrayList());
					String type = (String) pieces.get(0);
					pieces.remove(0);
					if (type.equals("region")) type ="territory";
					String code = (String) pieces.get(0);
					pieces.remove(0);
					if (type.equals("date")) {
						date = code;
						continue;
					}
					String oldName = (String) pieces.get(0);
					int pos = oldName.indexOf(';');
					if (pos >= 0) {
						oldName = oldName.substring(0,pos).trim();
						pieces.set(0,oldName);
					}
					List name = pieces;
					if (oldName.equalsIgnoreCase("PRIVATE USE")) continue;
					if (!type.equals("tzid")) {
						add(type, code, name);			
						continue;
					}
					List codes = (List) Utility.splitList(code, ',', true, new ArrayList());
					String preferred = null;
					for (int i = 0; i < pieces.size(); ++i) {
						code = (String) pieces.get(i);
						add(type, code, name);
						if (preferred == null) preferred = code;
						else {
							Map code_preferred = (Map) type_code_preferred.get(type);
							code_preferred.put(code, preferred);
						}
					}
				}
				lstreg.close();
			} catch (Exception e) {
				throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + files[j] + "\t" + line).initCause(e);
			}
		}
	}
	
	/**
	 * @param string
	 * @param string2
	 * @param string3
	 */
	private void add(String string, String string2, String string3) {
		List l = new ArrayList();
		l.add(string3);
		add(string, string2, l);	
	}

	private void add(String type, String code, List otherData) {
		Map code_data = (Map) type_code_data.get(type);
		if (code_data == null) {
			code_data = new TreeMap();
			type_code_data.put(type, code_data);
		}
		code_data.put(code, otherData);
		Map data_codes = (Map) type_data_codes.get(type);
		if (data_codes == null) {
			data_codes = new TreeMap();
			type_data_codes.put(type, data_codes);
		}
		String name = (String) otherData.get(0);
		List codes = (List) data_codes.get(name);
		if (codes == null) {
			codes = new ArrayList();
			data_codes.put(name, codes);
		}
		codes.add(code);
	}
	
	private Map zone_to_country;
	private Map country_to_zoneSet;

	/**
	 * @return mapping from zone id to country. If a zone has no country, then XX is used.
	 */
	public Map getZoneToCounty() {
		if (zone_to_country == null) make_zone_to_country();
		return zone_to_country;
	}
		
	/**
	 * @return mapping from country to zoneid. If a zone has no country, then XX is used.
	 */
	public Map getCountryToZoneSet() {
		if (country_to_zoneSet == null) make_zone_to_country();
		return country_to_zoneSet;
	}
		
    /**
	 * 
	 */
	private void make_zone_to_country() {
		zone_to_country = new TreeMap(TZIDComparator);
		country_to_zoneSet = new TreeMap();
		//Map aliasMap = getAliasMap();
		Map zoneData = getZoneData();
		for (Iterator it = zoneData.keySet().iterator(); it.hasNext(); ){
			String zone = (String) it.next();
			String country = (String)((List)zoneData.get(zone)).get(2);
			zone_to_country.put(zone, country);
			Set s = (Set) country_to_zoneSet.get(country);
			if (s == null) country_to_zoneSet.put(country, s = new TreeSet());
			s.add(zone);			
		}
		/*
		Set territories = getAvailableCodes("territory");
		for (Iterator it = territories.iterator(); it.hasNext();) {
			String code = (String) it.next();
			String[] zones = TimeZone.getAvailableIDs(code);
			for (int i = 0; i < zones.length; ++i) {
				if (aliasMap.get(zones[i]) != null) continue;
				zone_to_country.put(zones[i], code);
			}
		}
		String[] zones = TimeZone.getAvailableIDs();
		for (int i = 0; i < zones.length; ++i) {
			if (aliasMap.get(zones[i]) != null) continue;
			if (zone_to_country.get(zones[i]) == null) {
				zone_to_country.put(zones[i], NO_COUNTRY);
			}
		}
		for (Iterator it = zone_to_country.keySet().iterator(); it.hasNext();) {
			String tzid = (String) it.next();
			String country = (String) zone_to_country.get(tzid);
			Set s = (Set) country_to_zoneSet.get(country);
			if (s == null) country_to_zoneSet.put(country, s = new TreeSet());
			s.add(tzid);
		}
		*/
		// protect
		zone_to_country = Collections.unmodifiableMap(zone_to_country);
		country_to_zoneSet = (Map) Utility.protectCollection(country_to_zoneSet);
	}
	/**


	private Map bogusZones = null;    
    
    private Map getAliasMap() {
        if (bogusZones == null) {
            try {
                bogusZones = new TreeMap();
                BufferedReader in = BagFormatter.openUTF8Reader(DATA_DIR, "TimeZoneAliases.txt");
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    line = line.trim();
                    int pos = line.indexOf('#');
                    if (pos >= 0) {
                        skippedAliases.add(line);
                        line = line.substring(0,pos).trim();
                    } 
                    if (line.length() == 0) continue;
                    List pieces = Utility.splitList(line,';', true);
                    bogusZones.put(pieces.get(0), pieces.get(1));
                }
                in.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't find timezone aliases");
            }
        }
        return bogusZones;
    }
    */
	
    Map zoneData;
    Set skippedAliases = new TreeSet();
    /*
     * # This file contains a table with the following columns:
# 1.  ISO 3166 2-character country code.  See the file `iso3166.tab'.
# 2.  Latitude and longitude of the zone's principal location
#     in ISO 6709 sign-degrees-minutes-seconds format,
#     either +-DDMM+-DDDMM or +-DDMMSS+-DDDMMSS,
#     first latitude (+ is north), then longitude (+ is east).
# 3.  Zone name used in value of TZ environment variable.
# 4.  Comments; present if and only if the country has multiple rows.
#
# Columns are separated by a single tab.

     */
    /**
     * @return map from tzids to a list: latitude, longitude, country, comment?. + = N or E
     */
    public Map getZoneData() {
        if (zoneData == null) makeZoneData();
        return zoneData;
    }
    
    /**
	 * 
	 */
	private void makeZoneData() {
		try {
			//String deg = "([+-][0-9]+)";//
			String deg = "([+-])([0-9][0-9][0-9]?)([0-9][0-9])([0-9][0-9])?";//
			Matcher m = Pattern.compile(deg+deg).matcher("");
			zoneData = new TreeMap();
		    BufferedReader in = BagFormatter.openUTF8Reader(DATA_DIR, "zone.tab");
		    while (true) {
		        String line = in.readLine();
		        if (line == null) break;
		        line = line.trim();
		        int pos = line.indexOf('#');
		        if (pos >= 0) {
		            skippedAliases.add(line);
		            line = line.substring(0,pos).trim();
		        } 
		        if (line.length() == 0) continue;
		        List pieces = Utility.splitList(line,'\t', true);
		        String country = (String) pieces.get(0);
		        String latLong = (String) pieces.get(1);
		        String tzid = (String) pieces.get(2);
		        String ntzid = (String) FIX_UNSTABLE_TZIDS.get(tzid);
		        if (ntzid != null) tzid = ntzid;
		        String comment = pieces.size() < 4 ? null : (String) pieces.get(3);
		        pieces.clear();
		        if (!m.reset(latLong).matches()) throw new IllegalArgumentException("Bad zone.tab, lat/long format: " + line);
		    
		    	pieces.add(getDegrees(m, true));
		    	pieces.add(getDegrees(m, false));
		        pieces.add(country);
		        if (comment != null) pieces.add(comment);
		        if (zoneData.containsKey(tzid)) throw new IllegalArgumentException("Bad zone.tab, duplicate entry: " + line);
		        zoneData.put(tzid, pieces);
		    }
		    in.close();
		    // add Etcs
		    for (int i = -14; i <= 12; ++i) {
		    	List pieces = new ArrayList();
		    	int latitude = 0;
		    	int longitude = i*15;
		    	if (longitude <= -180) {
		    		longitude += 360;
		    	}
		    	pieces.add(new Double(latitude)); // lat
		    		//  remember that the sign of the TZIDs is wrong
		    	pieces.add(new Double(-longitude)); // long
		    	pieces.add(NO_COUNTRY); // country
		    	
		    	zoneData.put("Etc/GMT" 
		    			+ (i == 0 ? "" : i < 0 ? "" + i : "+" + i),
		    			pieces);
		    }
		    zoneData = Collections.unmodifiableMap(zoneData); // protect for later
		    
		    // now get links
		    String lastZone = "";
		    Pattern whitespace = Pattern.compile("\\s+");
		    for (int i = 0; i < TZFiles.length; ++i) {
		    	in = BagFormatter.openUTF8Reader(DATA_DIR, TZFiles[i]);
		    	while (true) {
		    		String line = in.readLine();
		    		if (line == null) break;
		    		if (line.startsWith("#") || line.trim().length() == 0) continue;
					String[] items = whitespace.split(line);
		    		if (items[0].equals("Rule")) {
		    			//# Rule	NAME	FROM	TO	TYPE	IN	ON	AT	SAVE	LETTER/S
						//Rule	Algeria	1916	only	-	Jun	14	23:00s	1:00	S
		    			
		    			String ruleID = items[1];
		    			List ruleList = (List) ruleID_rules.get(ruleID);
		    			if (ruleList == null) {
		    				ruleList = new ArrayList();
		    				ruleID_rules.put(ruleID, ruleList);
		    			}
		    			List l = new ArrayList();
		    			l.addAll(Arrays.asList(items));
		    			l.remove(0);
		    			l.remove(0);
		    			ruleList.add(l);
		    		} else if (items[0].equals("Zone") || line.startsWith("\t")) {

		    			// Zone	Africa/Algiers	0:12:12 -	LMT	1891 Mar 15 0:01
						//				0:09:21	-	PMT	1911 Mar 11    # Paris Mean Time
		    			String zoneID;
		    			if (line.startsWith("\t")) {
		    				zoneID = lastZone;
		    			} else {
		    				zoneID = items[1];
		    			}
		    			List zoneRules = (List) zone_rules.get(zoneID);
		    			if (zoneRules == null) {
		    				zoneRules = new ArrayList();
		    				zone_rules.put(zoneID, zoneRules);
		    			}
		    			List l = new ArrayList();
		    			l.addAll(Arrays.asList(items));
		    			l.remove(0);
		    			l.remove(0);
		    			zoneRules.add(l);
		    			lastZone = zoneID;
		    		} else if (items[0].equals("Link")) {
		     			String old = items[2];
		    			String newOne = items[1];
		    			String conflict = (String) linkold_new.get(old);
		    			if (conflict != null) {
		    				System.out.println("Conflict with old: " + old + " => " + conflict + ", " + newOne);
		    			}
		    			linkold_new.put(old, newOne);
		    		} else {
		    			System.out.println("Unknown zone line: " + line);
		    		}
		    	}
		    	in.close();
		    }
		    // TODO protect zone info later
		} catch (IOException e) {
		    throw (IllegalArgumentException) new IllegalArgumentException("Can't find timezone aliases").initCause(e);
		}
	}

	Map ruleID_rules = new TreeMap();
    Map zone_rules = new TreeMap();
    Map linkold_new = new TreeMap();
    
    public Comparator getTZIDComparator() {
    	return TZIDComparator;
    }
    
	private Comparator TZIDComparator = new Comparator() {
		Map data = getZoneData();
		public int compare(Object o1, Object o2) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			//String ss1 = s1.substring(0,s1.indexOf('/'));
			//String ss2 = s2.substring(0,s2.indexOf('/'));
			//if (!ss1.equals(ss2)) return regionalCompare.compare(ss1, ss2);
			List data1 = (List) data.get(s1);
			List data2 = (List) data.get(s2);
			if (data1 != null && data2 != null) {
				int result;
				//country
				String country1 = (String) data1.get(2);
				String country2 = (String) data2.get(2);
				if ((result = country1.compareTo(country2)) != 0) return result;
				//longitude
				Double d1 = (Double) data1.get(1);
				Double d2 = (Double) data2.get(1);
				if ((result = d1.compareTo(d2)) != 0) return result;
				//latitude
				d1 = (Double) data1.get(0);
				d2 = (Double) data2.get(0);
				if ((result = d1.compareTo(d2)) != 0) return result;
				// name
				return s1.compareTo(s2); 
			}
			throw new IllegalArgumentException("Can't compare " + s1 + " and " + s2);
		}		
	};

	private static MapComparator regionalCompare = new MapComparator();
	static {
		regionalCompare.add("America");
		regionalCompare.add("Atlantic");		
		regionalCompare.add("Europe");
		regionalCompare.add("Africa");
		regionalCompare.add("Asia");
		regionalCompare.add("Indian");		
		regionalCompare.add("Australia");		
		regionalCompare.add("Pacific");
		regionalCompare.add("Arctic");
		regionalCompare.add("Antarctica");		
		regionalCompare.add("Etc");	
	}
	
	private static String[] TZFiles = {
			"africa", "antarctica", "asia", "australasia", "backward", "etcetera", "europe", 
			"northamerica", "pacificnew", "southamerica", "systemv"
	};
    
    private static Map FIX_UNSTABLE_TZIDS = new HashMap();
    static {
    	String[][] FIX_UNSTABLE_TZID_DATA = new String[][] {
    			{"America/Argentina/Buenos_Aires", "America/Buenos_Aires"},
    			{"America/Argentina/Catamarca", "America/Catamarca"},
    			{"America/Argentina/Cordoba", "America/Cordoba"},
    			{"America/Argentina/Jujuy", "America/Jujuy"},
    			{"America/Argentina/Mendoza", "America/Mendoza"}
    	};
    	for (int i = 0; i < FIX_UNSTABLE_TZID_DATA.length; ++i) {
    		FIX_UNSTABLE_TZIDS.put(FIX_UNSTABLE_TZID_DATA[i][0], FIX_UNSTABLE_TZID_DATA[i][1]);
    	}
    }
    
    private List DELETED3166 = Collections.unmodifiableList(Arrays.asList(new String[] {
    	"BQ", "BU", "CT", "DD", "DY", "FQ", "FX", "HV", "JT", "MI", "NH", "NQ", "NT",
		"PC", "PU", "PZ", "RH", "SU", "TP", "VD", "WK", "YD", "YU", "ZR"
    }));
    
    public List getOld3166() {
    	return DELETED3166;
    }
 
	/**
	 * @param m
	 */
	private Double getDegrees(Matcher m, boolean lat) {
		int startIndex = lat ? 1 : 5;
		double amount = Integer.parseInt(m.group(startIndex+1)) + Integer.parseInt(m.group(startIndex+2))/60.0;
		if (m.group(startIndex+3) != null) amount += Integer.parseInt(m.group(startIndex+3))/3600.0;
		if (m.group(startIndex).equals("-")) amount = -amount;
		return new Double(amount);
	}
	
	/**
	 * @return Returns the linkold_new.
	 */
	public Map getZoneLinkold_new() {
		getZoneData();
		return linkold_new;
	}
	/**
	 * @return Returns the ruleID_rules.
	 */
	public Map getZoneRuleID_rules() {
		getZoneData();
		return ruleID_rules;
	}
	/**
	 * @return Returns the zone_rules.
	 */
	public Map getZone_rules() {
		getZoneData();
		return zone_rules;
	}
}