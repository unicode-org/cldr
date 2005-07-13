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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;

/**
 * Provides access to various codes used by CLDR: RFC 3066, ISO 4217, Olson tzids
 */
public class StandardCodes {
	public static final String NO_COUNTRY = "001";

	private static StandardCodes singleton;
	private Map type_code_data = new TreeMap();
	private Map type_name_codes = new TreeMap();
	private Map type_code_preferred = new TreeMap();
	private Map country_modernCurrency = new TreeMap();
	private Map goodCodes = new TreeMap();
	private String date;
	
	/**
	 * Get the singleton copy of the standard codes.
	 */
	static public synchronized StandardCodes make() {
		if (singleton == null) singleton = new StandardCodes();
		return singleton;
	}
	
	/**
	 * The data is the name in the case of RFC3066 codes, and the country code in the case of TZIDs and ISO currency codes.
	 * If the country code is missing, uses ZZ.
	 */
	public String getData(String type, String code) {
		Map code_data = (Map) type_code_data.get(type);
		if (code_data == null) return null;
		List list = (List)code_data.get(code);
		if (list == null) return null;
		return (String)list.get(0);
	}

	/**
	 * @return the full data for the type and code
	 * For the data in lstreg, it is
	 * description | date | canonical_value | recommended_prefix # comments
	 */
	public List getFullData(String type, String code) {
		Map code_data = (Map) type_code_data.get(type);
		if (code_data == null) return null;
		return (List)code_data.get(code);
	}
	
	/**
	 * Return a replacement code, if available. If not, return null.
	 *
	 */
	public String getReplacement(String type, String code) {
		if (type.equals("currency")) return null; // no replacement codes for currencies
		List data = getFullData(type, code);
		if (data == null) return null;
		// if available, the replacement is a non-empty value other than --, in position 2.
		if (data.size() < 3) return null;
		String replacement = (String) data.get(2);
		if (!replacement.equals("") && !replacement.equals("--")) return replacement;
		return null;
	}

	
	/**
	 * Return the list of codes that have the same data. For example, returns all currency codes for a country
	 * If there is a preferred one, it is first.
	 * @param type
	 * @param data
	 * @return
	 */
	public List getCodes(String type, String data) {
		Map data_codes = (Map) type_name_codes.get(type);
		if (data_codes == null) return null;
		return Collections.unmodifiableList((List)data_codes.get(data));
	}

	/**
	 * Where there is a preferred code, return it.
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
	
	/**
	 * Get all the available "real" codes for a given type
	 * @param type
	 * @return
	 */
	public Set getGoodAvailableCodes(String type) {
		Set result = (Set) goodCodes.get(type);
		if (result == null) {
			Map code_name = (Map) type_code_data.get(type);
			if (code_name == null) return null;
			result = new TreeSet(code_name.keySet());
			if (type.equals("currency")) {
		        for (Iterator it = result.iterator(); it.hasNext();) {
		        	String code = (String) it.next();
		        	List data = getFullData(type, code);
		        	if ("X".equals(data.get(3))) {
		        		//System.out.println("Removing: " + code);
		        		it.remove();
		        	}
		        }
			} else if (!type.equals("tzid")) {
		        for (Iterator it = result.iterator(); it.hasNext();) {
		        	String code = (String) it.next();
		        	if (code.equals("root") || code.equals("QO")) continue;
		        	List data = getFullData(type, code);
		        	if (data.size() < 3) System.out.println(code + "\t" + data);
		        	if (data.get(0).equals("PRIVATE USE")
		        			|| (!data.get(2).equals("") && !data.get(2).equals("--"))) {
		        		//System.out.println("Removing: " + code);
		        		it.remove();
		        	}
		        }
			}
			result = Collections.unmodifiableSet(result);
			goodCodes.put(type, result);
		}
		return result;
	}
	
	/**
	 * Gets the modern currency.
	 */
	public Set getMainCurrencies(String countryCode) {
		return (Set) country_modernCurrency.get(countryCode);
	}
	
	private Map platform_locale_status = null;
	
	/**
	 * Returns locales according to status. It returns a Map of Maps,
	 * key 1 is either IBM or Java (perhaps more later),
	 * key 2 is the locale string
	 * value is the status. For IBM, it is G0..G4,
	 * while for Java it is Supported or Unsupported
	 */
	public Map getLocaleTypes() throws IOException {
		if (platform_locale_status == null) {
			platform_locale_status = new TreeMap();
			String line;
			BufferedReader lstreg = BagFormatter.openUTF8Reader(Utility.UTIL_DATA_DIR, "Locales.txt");
			while (true) {
				line = lstreg.readLine();
				if (line == null) break;
				int commentPos = line.indexOf('#');
				if (commentPos >= 0) line = line.substring(0, commentPos);
				if (line.length() == 0) continue;
				List stuff = Utility.splitList(line, ';', true);
				Map locale_status = (Map) platform_locale_status.get(stuff.get(0));
				if (locale_status == null)  platform_locale_status.put(stuff.get(0), locale_status = new TreeMap());
				locale_status.put(stuff.get(1), stuff.get(2));
			}
			Utility.protectCollection(platform_locale_status);
		}
		return platform_locale_status;
	}
	
	// ========== PRIVATES ==========

	private StandardCodes() {
		String[] files = {"lstreg.txt", "ISO4217.txt"}; // , "TZID.txt"
		type_code_preferred.put("tzid", new TreeMap());
		add("language", "root", "Root");
		String originalLine = null;
		for (int fileIndex = 0; fileIndex < files.length; ++fileIndex) {
			try {
				BufferedReader lstreg = BagFormatter.openUTF8Reader(Utility.UTIL_DATA_DIR, files[fileIndex]);
				while (true) {
					String line = originalLine = lstreg.readLine();
					if (line == null) break;
					int commentPos = line.indexOf('#');
					String comment = "";
					if (commentPos >= 0) {
						comment = line.substring(commentPos+1).trim();
						line = line.substring(0, commentPos);
					}
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
					
					List data = pieces;
					if (comment.indexOf("deprecated") >= 0) {
						//System.out.println(originalLine);
						if (data.get(2).toString().length() == 0) {
							data.set(2, "--");
						}
					}
					if (oldName.equalsIgnoreCase("PRIVATE USE")) {
						int separatorPos = code.indexOf("..");
						if (separatorPos < 0) {
							add(type, code, data);
						} else {
							String current = code.substring(0,separatorPos);
							String end = code.substring(separatorPos + 2);
							//System.out.println(">>" + code + "\t" + current + "\t" + end);
							for (; current.compareTo(end) <= 0; current = nextAlpha(current)) {
								//System.out.println(">" + current);
								add(type, current, data);
							}
						}
						continue;
					}
					if (!type.equals("tzid")) {
						add(type, code, data);
						if (type.equals("currency")) {
							// currency	|	TPE	|	Timor Escudo	|	TP	|	EAST TIMOR	|	O
							if (data.get(3).equals("C")) {
								String country = (String) data.get(1);
								Set codes = (Set) country_modernCurrency.get(country);
								if (codes == null) {
									country_modernCurrency.put(country, codes = new TreeSet());
								}
								codes.add(code);
							}
						}
						continue;
					}
					// type = tzid
					//List codes = (List) Utility.splitList(code, ',', true, new ArrayList());
					String preferred = null;
					for (int i = 0; i < pieces.size(); ++i) {
						code = (String) pieces.get(i);
						add(type, code, data);
						if (preferred == null) preferred = code;
						else {
							Map code_preferred = (Map) type_code_preferred.get(type);
							code_preferred.put(code, preferred);
						}
					}
				}
				lstreg.close();
			} catch (Exception e) {
				throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + files[fileIndex] + "\t" + originalLine).initCause(e);
			}
			Utility.protectCollection(country_modernCurrency);
		}
		Map m = getZoneData();
		for (Iterator it = m.keySet().iterator(); it.hasNext();) {
			String code = (String) it.next();
			add("tzid", code, m.get(code).toString());
		}
	}
	
	/**
	 * @param current
	 * @return
	 */
	private String nextAlpha(String current) {
		// Don't care that this is inefficient
		int value = 0;
		for (int i = 0; i < current.length(); ++i) {
			char c = current.charAt(i);
			c -= c < 'a' ? 'A' : 'a';
			value = value * 26 + c;			
		}
		value += 1;
		String result = "";
		for (int i = 0; i < current.length(); ++i) {
			result = (char)((value % 26) + 'A') + result;
			value = value / 26;
		}
		if (UCharacter.toLowerCase(current).equals(current)) {
			result = UCharacter.toLowerCase(result);
		} else if (UCharacter.toUpperCase(current).equals(current)) {
			// do nothing
		} else {
			result = UCharacter.toTitleCase(result,null);
		}
		return result;
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
		// hack
		if (type.equals("script")) {
			if (code.equals("Qaai")) {
				otherData = new ArrayList(otherData);
				otherData.set(0,"Inherited");
			} else if (code.equals("Zyyy")) {
				otherData = new ArrayList(otherData);
				otherData.set(0,"Common");
			}
		}

		// assume name is the first item
		
		String name = (String) otherData.get(0);

		// add to main list
		Map code_data = (Map) type_code_data.get(type);
		if (code_data == null) {
			code_data = new TreeMap();
			type_code_data.put(type, code_data);
		}
		List lastData = (List) code_data.get(code);
		if (lastData != null) {
			lastData.addAll(otherData);
		} else {
			code_data.put(code, otherData);
		}
		
		// now add mapping from name to codes
		Map name_codes = (Map) type_name_codes.get(type);
		if (name_codes == null) {
			name_codes = new TreeMap();
			type_name_codes.put(type, name_codes);
		}
		List codes = (List) name_codes.get(name);
		if (codes == null) {
			codes = new ArrayList();
			name_codes.put(name, codes);
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
		    BufferedReader in = BagFormatter.openUTF8Reader(Utility.UTIL_DATA_DIR, "zone.tab");
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
		    zoneData = (Map) Utility.protectCollection(zoneData); // protect for later
		    
		    // now get links
		    String lastZone = "";
		    Pattern whitespace = Pattern.compile("\\s+");
		    for (int i = 0; i < TZFiles.length; ++i) {
		    	in = BagFormatter.openUTF8Reader(Utility.UTIL_DATA_DIR, TZFiles[i]);
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
		    				String ntzid = (String) FIX_UNSTABLE_TZIDS.get(zoneID);
		    		        if (ntzid != null) zoneID = ntzid;
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
		    // add in stuff that should be links
		    linkold_new.put("Etc/UTC", "Etc/GMT");
		    linkold_new.put("Etc/UCT", "Etc/GMT");

		    // fix the links from old to new, to remove chains
		    for (Iterator it = linkold_new.keySet().iterator(); it.hasNext();) {
		    	Object oldItem = it.next();
		    	Object newItem = linkold_new.get(oldItem);
		    	while (true) {
		    		Object linkItem = linkold_new.get(newItem);
		    		if (linkItem == null) break;
		    		if (false) System.out.println("Connecting link chain: " + oldItem + "\t=> " + newItem + "\t=> " + linkItem);
		    		newItem = linkItem;
		    		linkold_new.put(oldItem, newItem);
		    	}
		    }
		    // fix unstable TZIDs
		    Set itemsToRemove = new HashSet();
		    Map itemsToAdd = new HashMap();
		    for (Iterator it = linkold_new.keySet().iterator(); it.hasNext();) {
		    	Object oldItem = it.next();
		    	Object newItem = linkold_new.get(oldItem);
		    	Object modOldItem = RESTORE_UNSTABLE_TZIDS.get(oldItem);
		    	Object modNewItem = FIX_UNSTABLE_TZIDS.get(newItem);
		    	if (modOldItem == null && modNewItem == null) continue;
		    	if (modOldItem == null) { // just fix old entry
		    		itemsToAdd.put(oldItem, modNewItem);
		    		continue; 
		    	}
		    	// otherwise have to nuke and redo
		    	itemsToRemove.add(oldItem);
		    	if (modNewItem == null) modNewItem = newItem;
		    	itemsToAdd.put(modOldItem, modNewItem);
		    }
		    // now make fixes (we couldn't earlier because we were iterating
		    Utility.removeAll(linkold_new, itemsToRemove);
		    linkold_new.putAll(itemsToAdd);
		    
		    // now remove all links that are from canonical zones
		    Utility.removeAll(linkold_new, zoneData.keySet());
		    
		    // generate list of new to old
		    for (Iterator it = linkold_new.keySet().iterator(); it.hasNext();) {
		    	String oldZone = (String) it.next();
		    	String newZone = (String) linkold_new.get(oldZone);
		    	Set s = (Set) linkNew_oldSet.get(newZone);
		    	if (s == null) linkNew_oldSet.put(newZone, s = new HashSet());
		    	s.add(oldZone);
		    }
		    
		    // PROTECT EVERYTHING
		    linkNew_oldSet = (Map) Utility.protectCollection(linkNew_oldSet);
	    	linkold_new = (Map) Utility.protectCollection(linkold_new);
	    	ruleID_rules = (Map) Utility.protectCollection(ruleID_rules);
	    	zone_rules = (Map) Utility.protectCollection(zone_rules);
		    // TODO protect zone info later
		} catch (IOException e) {
		    throw (IllegalArgumentException) new IllegalArgumentException("Can't find timezone aliases").initCause(e);
		}
	}

	Map ruleID_rules = new TreeMap();
    Map zone_rules = new TreeMap();
    Map linkold_new = new TreeMap();
    Map linkNew_oldSet = new TreeMap();
    
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
    
    private static Map FIX_UNSTABLE_TZIDS;
    private static Map RESTORE_UNSTABLE_TZIDS;
    static {
    	String[][] FIX_UNSTABLE_TZID_DATA = new String[][] {
    			{"America/Argentina/Buenos_Aires", "America/Buenos_Aires"},
    			{"America/Argentina/Catamarca", "America/Catamarca"},
    			{"America/Argentina/Cordoba", "America/Cordoba"},
    			{"America/Argentina/Jujuy", "America/Jujuy"},
    			{"America/Argentina/Mendoza", "America/Mendoza"}
    	};
    	FIX_UNSTABLE_TZIDS = Utility.asMap(FIX_UNSTABLE_TZID_DATA);
    	RESTORE_UNSTABLE_TZIDS = Utility.asMap(FIX_UNSTABLE_TZID_DATA, new HashMap(), true);
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
	 * @return Returns the linkold_new.
	 */
	public Map getZoneLinkNew_OldSet() {
		getZoneData();
		return linkNew_oldSet;
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