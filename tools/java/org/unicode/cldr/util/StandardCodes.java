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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;

/**
 * Provides access to various codes used by CLDR: RFC 3066, ISO 4217, Olson tzids
 */
public class StandardCodes {
	private static StandardCodes singleton;
	private Map type_code_data = new TreeMap();
	private Map type_data_codes = new TreeMap();
	private Map type_code_preferred = new TreeMap();
	
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
		return (String)code_data.get(code);
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
		for (int j = 0; j < files.length; ++j) {
			try {
				BufferedReader lstreg = BagFormatter.openUTF8Reader("", files[j]);
				String[] pieces = new String[10];
				while (true) {
					String line = lstreg.readLine();
					if (line == null) break;
					int commentPos = line.indexOf('#');
					if (commentPos >= 0) line = line.substring(0, commentPos);
					if (line.length() == 0) continue;
					Utility.split(line, '|', pieces);
					String type = pieces[0].trim();
					if (type.equals("region")) type ="territory";
					String code = pieces[1].trim();
					String name = pieces[2].trim();
					if (name.equalsIgnoreCase("PRIVATE USE")) continue;
					if (!type.equals("tzid")) {
						add(type, code, name);			
						continue;
					}
					Utility.split(code, ',', pieces);
					String preferred = null;
					for (int i = 0; i < pieces.length; ++i) {
						code = pieces[i].trim();
						if (code.length() == 0) break;
						add(type, code, name);
						if (preferred == null) preferred = code;
						else {
							Map code_preferred = (Map) type_code_preferred.get(type);
							code_preferred.put(code, preferred);
						}
					}
				}
				lstreg.close();
			} catch (IOException e) {
				throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + files[j]).initCause(e);
			}
		}
	}
	
	private void add(String type, String code, String name) {
		int pos = name.indexOf(';');
		if (pos >= 0) name = name.substring(0,pos).trim();
		Map code_data = (Map) type_code_data.get(type);
		if (code_data == null) {
			code_data = new TreeMap();
			type_code_data.put(type, code_data);
		}
		code_data.put(code, name);
		Map data_codes = (Map) type_data_codes.get(type);
		if (data_codes == null) {
			data_codes = new TreeMap();
			type_data_codes.put(type, data_codes);
		}
		List codes = (List) data_codes.get(name);
		if (codes == null) {
			codes = new ArrayList();
			data_codes.put(name, codes);
		}
		codes.add(code);
	}
}