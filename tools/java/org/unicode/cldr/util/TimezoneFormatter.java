/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/

package org.unicode.cldr.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

import org.unicode.cldr.util.CLDRFile.Factory;

/**
 * TimezoneFormatter. Class that uses CLDR data directly to parse / format timezone names
 * according to the specification in TR#35. Note: there are some areas where the spec needs
 * fixing.
 * @author davis
 */
public class TimezoneFormatter {
	/**
	 * Length parameter for formatting
	 */
	public static final int SHORT = 0, LONG = 1, LENGTH_LIMIT = 2;
	/**
	 * Type parameter for formatting
	 */
	public static final int GMT = 0, GENERIC = 1, STANDARD = 2, DAYLIGHT = 3, TYPE_LIMIT = 4;
	
	/**
	 * Arrays of names, for testing. Should be const, but we can't do that in Java
	 */
	public static final List LENGTH = Arrays.asList(new String[] {"short", "long"});
	public static final List TYPE = Arrays.asList(new String[] {"gmt", "generic", "standard", "daylight"});
	
	// static fields built from Timezone Database for formatting and parsing

	private static final Map zone_countries = StandardCodes.make().getZoneToCounty();
	private static final Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
	private static final Map old_new = StandardCodes.make().getZoneLinkold_new();
	
	// instance fields built from CLDR data for formatting and parsing

	private transient SimpleDateFormat hourFormatPlus = new SimpleDateFormat();
	private transient SimpleDateFormat hourFormatMinus = new SimpleDateFormat();
	private transient MessageFormat hoursFormat, gmtFormat, regionFormat, fallbackFormat;
	private transient String abbreviationFallback, preferenceOrdering;
	private transient Set singleCountriesSet;
	
	// private for computation
	private transient Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	private transient SimpleDateFormat rfc822Plus = new SimpleDateFormat("+HHmm");
	private transient SimpleDateFormat rfc822Minus = new SimpleDateFormat("-HHmm");
	{
		TimeZone gmt = TimeZone.getTimeZone("GMT");
		rfc822Plus.setTimeZone(gmt);
		rfc822Minus.setTimeZone(gmt);
	}	

	// input parameters
	private CLDRFile desiredLocaleFile;
	private String inputLocaleID;
	private boolean skipDraft;
	
	/**
	 * Create from a cldrFactory and a locale id.
	 * @see CLDRFile
	 */
	public TimezoneFormatter(Factory cldrFactory, String localeID) {
		inputLocaleID = localeID;
		desiredLocaleFile = cldrFactory.make(localeID, true);
		String hourFormatString = getStringValue("/ldml/dates/timeZoneNames/hourFormat");
		String[] hourFormatStrings = Utility.splitArray(hourFormatString,';');
		ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder().setCLDRFactory(cldrFactory);
		hourFormatPlus = icuServiceBuilder.getDateFormat(localeID,0,1);
		hourFormatPlus.applyPattern(hourFormatStrings[0]);
		hourFormatMinus = icuServiceBuilder.getDateFormat(localeID,0,1);
		hourFormatMinus.applyPattern(hourFormatStrings[1]);
		gmtFormat = new MessageFormat(getStringValue("/ldml/dates/timeZoneNames/gmtFormat"));
		regionFormat = new MessageFormat(getStringValue("/ldml/dates/timeZoneNames/regionFormat"));
		fallbackFormat = new MessageFormat(getStringValue("/ldml/dates/timeZoneNames/fallbackFormat"));
		checkForDraft("/ldml/dates/timeZoneNames/singleCountries");
		String temp = desiredLocaleFile.getFullXPath("/ldml/dates/timeZoneNames/singleCountries");
		String singleCountriesList = (String) new XPathParts(null, null).set(
				temp).findAttributes("singleCountries").get("list");
		singleCountriesSet = new TreeSet(Utility.splitList(singleCountriesList, ' '));
		/* not needed
		hoursFormat = new MessageFormat(desiredLocaleFile.getStringValue(
				"/ldml/dates/timeZoneNames/hoursFormat"));
		abbreviationFallback = (String) new XPathParts(null, null).set(
				desiredLocaleFile.getFullXPath("/ldml/dates/timeZoneNames/abbreviationFallback"))
				.findAttributes("abbreviationFallback").get("type");
		temp = desiredLocaleFile.getFullXPath("/ldml/dates/timeZoneNames/preferenceOrdering");
		preferenceOrdering = (String) new XPathParts(null, null).set(
				temp).findAttributes("preferenceOrdering").get("type");
		*/
	}
	
	/**
	 * 
	 */
	private String getStringValue(String cleanPath) {
		checkForDraft(cleanPath);
		return desiredLocaleFile.getStringValue(cleanPath);
	}

	private String getName(int territory_name, String country, boolean skipDraft2) {
		checkForDraft(CLDRFile.getKey(territory_name,country));
		return desiredLocaleFile.getName(territory_name, country, skipDraft2);
	}

	private void checkForDraft(String cleanPath) {
		String xpath = desiredLocaleFile.getFullXPath(cleanPath);
		if (xpath != null && xpath.indexOf("[@draft=\"true\"]") >= 0) {
			System.out.println("Draft in " + inputLocaleID + ":\t" + cleanPath);
		}
	}
	
	/**
	 * Convenience routine for formatting based on a date
	 */
	public String getFormattedZone(String inputZoneid, String pattern, long date) {
		String zoneid = (String) old_new.get(inputZoneid);
		if (zoneid == null) zoneid = inputZoneid;
		TimeZone tz = TimeZone.getTimeZone(zoneid);
		int gmtOffset1 = tz.getOffset(date);
		boolean daylight = tz.getRawOffset() != gmtOffset1;
		return getFormattedZone(zoneid, pattern, daylight, gmtOffset1);
	}
	
	/**
	 * Convenience routine for formatting based on daylight or not, and the offset
	 */
	public String getFormattedZone(String zoneid, String pattern, boolean daylight, int gmtOffset1) {
		int length = pattern.length() < 4 ? SHORT : LONG;
		int type = pattern.startsWith("z") ? (daylight ? DAYLIGHT : STANDARD)
				: pattern.startsWith("Z") ? GMT
				: GENERIC;
		return getFormattedZone(zoneid, length, type, gmtOffset1);
	}
	
	/**
	 * Main routine for formatting based on a length (LONG or SHORT), 
	 * a type (GMT, GENERIC, STANDARD, DAYLIGHT), and an offset.
	 */
	public String getFormattedZone(String inputZoneid, int length, int type, int gmtOffset1) {
		String result;
		String zoneid = (String) old_new.get(inputZoneid);
		if (zoneid == null) zoneid = inputZoneid;
		
		if (type == GMT && length == SHORT) { // just do the RFC, via Java; note that this will actually vary by date, but we hardcode for testing
			return gmtOffset1 < 0 ? rfc822Minus.format(new Date(-gmtOffset1)) : rfc822Plus.format(new Date(gmtOffset1));
		}
		String prefix = "/ldml/dates/timeZoneNames/zone[@type=\"" + zoneid + "\"]/";
		// 1. If non-GMT format, and we have an explicit translation, use it
		if (type != GMT) {
			String formatValue = getStringValue(prefix + LENGTH.get(length) + "/" + TYPE.get(type));
			if (formatValue != null) return formatValue;
		}
		
		String country = (String) zone_countries.get(zoneid);
		// 2. if GMT format or no country, use GMT format
		// 3. Else for non-wall-time, use GMT format
		//	America/Los_Angeles => "GMT-08:00"
		if (type != GENERIC || country.equals(StandardCodes.NO_COUNTRY)) {
			DateFormat format = gmtOffset1 < 0 ? hourFormatMinus : hourFormatPlus;
			calendar.setTimeInMillis(Math.abs(gmtOffset1));
			result = format.format(calendar);
			return gmtFormat.format(new Object[]{result});
		}

		// 4. *Else if there is an exemplar city, use it with the region format. The exemplar city may not be the same as the Olson ID city, if another city is much more recognizable for whatever reason. 
		// However, it is very strongly recommended that the same city be used.
		//	America/Los_Angeles => "Tampo de San Fransisko"

		// 5. *Else if there is a country for the time zone, and a translation in the locale for the country name, and the country only has one (modern) timezone, use it with the region format :
		//	Africa/Monrovia => LR => "Tampo de Liberja"
		Set s = (Set) countries_zoneSet.get(country);
		if (s != null && s.size() == 1 || singleCountriesSet.contains(zoneid)) {
			result = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, skipDraft);
			if (result != null) return regionFormat.format(new Object[]{result});
		}
		// 6. Else if it is a perpetual alias for a "real" ID, and if there is an exact translation for that, try #1..#4 with that alias.
		//	Europe/San_Marino => Europe/Rome => ... => "Tampo de Roma"
		
		// 7. Else fall back to the raw Olson ID (stripping off the prefix, and turning _ into space), using the fallback format. 
		//	America/Goose_Bay => "Tampo de  «Goose Bay»"
		String exemplarValue = getStringValue(prefix + "exemplarCity");
		if (exemplarValue != null) {
			result = exemplarValue;
		} else {
			result = getFallbackName(zoneid);
		}
		
		String countryTranslation = getName(CLDRFile.TERRITORY_NAME, country, skipDraft);
		if (countryTranslation == null) countryTranslation = country;
		return fallbackFormat.format(new Object[]{result, countryTranslation});
		
		// 8. Else use the (possibly multi-offset) GMT format
		//	America/Nome => "GMT-0900/-0800"
		// NOP -- never get there.	
	}
	
	/**
	 * Used for computation in parsing
	 */
	private static final int WALL_LIMIT = 2, STANDARD_LIMIT = 4;
	private static final String[] zoneTypes = {"\"]/long/generic", "\"]/short/generic",
			"\"]/long/standard", "\"]/short/standard", 
			"\"]/long/daylight", "\"]/short/daylight"};

	private transient Matcher m = Pattern.compile("([-+])([0-9][0-9])([0-9][0-9])").matcher("");
	private transient ParsePosition parsePosition = new ParsePosition(0);

	private transient boolean parseInfoBuilt;
	private transient final Map localizedCountry_countryCode= new HashMap();
	private transient final Map exemplar_zone= new HashMap();
	private transient final Map localizedExplicit_zone = new HashMap();
	private transient final Map country_zone = new HashMap();
	
	/**
	 * Returns zoneid, or if a gmt offset, returns "" and a millis value in offsetMillis[0].
	 * If we can't parse, return null
	 */
	public String parse(String inputText, long[] offsetMillis) {
		// if we haven't parsed before, build parsing info
		if (!parseInfoBuilt) buildParsingInfo();
		// there are the following possible formats
		// explicit strings
		Object result = localizedExplicit_zone.get(inputText);
		if (result != null) {
			if (result instanceof String) return (String)result;
			offsetMillis[0] = ((Long)result).longValue();
			return "";
		}
		
		// RFC 822
		if (m.reset(inputText).matches()) {
			int hours = Integer.parseInt(m.group(2));
			int minutes = Integer.parseInt(m.group(3));
			int millis = hours *60*60*1000 + minutes * 60*1000;
			if (m.group(1).equals("-")) millis = - millis;		// check sign!
			offsetMillis[0] = millis;
			return "";
		}
		
		// GMT-style (also fallback for daylight/standard)

		parsePosition.setIndex(0);
		Object[] results = gmtFormat.parse(inputText, parsePosition);
		if (results != null) {
			String hours = (String) results[0];
			parsePosition.setIndex(0);
			Date date = hourFormatPlus.parse(hours, parsePosition);
			if (date != null) {
				offsetMillis[0] = date.getTime();
				return "";					
			}
			parsePosition.setIndex(0);
			date = hourFormatMinus.parse(hours, parsePosition); // negative format
			if (date != null) {
				offsetMillis[0] = -date.getTime();
				return "";					
			}
		}
		
		//	Generic fallback, example: city or city (country)	

		String city = null, country = null;
		parsePosition.setIndex(0);
		Object[] x = fallbackFormat.parse(inputText, parsePosition);
		if (x != null) {
			city = (String) x[0];
			country = (String) x[1];
			// at this point, we don't really need the country, so ignore it
			// the city could be the last field of a zone, or could be an exemplar city
			// we have built the map so that both work
			return (String) exemplar_zone.get(city);
		} else {
			parsePosition.setIndex(0);
			x = regionFormat.parse(inputText, parsePosition);
			if (x == null) return null; // can't find anything
			country = (String) x[0];
			// see if the string is a localized country
			String countryCode = (String) localizedCountry_countryCode.get(country);
			if (countryCode == null) countryCode = country; // if not, try raw code
			return (String) country_zone.get(countryCode);
		}
	}

	/**
	 * Internal method. Builds parsing tables.
	 */
	private void buildParsingInfo() {
		// TODO Auto-generated method stub
		
		// Exemplar cities (plus constructed ones)
		// and add all the last fields.
		
		// do old ones first, we don't care if they are overriden
		for (Iterator it = old_new.keySet().iterator(); it.hasNext();) {
			String zoneid = (String) it.next();
			exemplar_zone.put(getFallbackName(zoneid), zoneid);
		}
		
		// then canonical ones
		for (Iterator it = zone_countries.keySet().iterator(); it.hasNext();) {
			String zoneid = (String) it.next();
			exemplar_zone.put(getFallbackName(zoneid), zoneid);
		}
		
		// now add exemplar cities, AND pick up explicit strings, AND localized countries
		String prefix = "/ldml/dates/timeZoneNames/zone[@type=\"";
		String countryPrefix = "/ldml/localeDisplayNames/territories/territory[@type=\"";
		Map localizedNonWall = new HashMap();
		Set skipDuplicates = new HashSet();
		for (Iterator it = desiredLocaleFile.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			// dumb, simple implementation
			if (path.startsWith(prefix)) {
				String zoneId = matchesPart(path, prefix, "\"]/exemplarCity");
				if (zoneId != null) {
					String name = desiredLocaleFile.getStringValue(path);
					if (name != null) exemplar_zone.put(name, zoneId);
				}
				for (int i = 0; i < zoneTypes.length; ++i) {
					zoneId = matchesPart(path, prefix, zoneTypes[i]);
					if (zoneId != null) {
						String name = desiredLocaleFile.getStringValue(path);
						if (name == null) continue;
						if (i < WALL_LIMIT) { // wall time
							localizedExplicit_zone.put(name, zoneId);
						} else {
							// TODO: if a daylight or standard string is ambiguous, return GMT!!							
							Object dup = localizedNonWall.get(name);
							if (dup != null) {
								skipDuplicates.add(name);
// TODO: use Etc/GMT...			localizedNonWall.remove(name);
								TimeZone tz = TimeZone.getTimeZone(zoneId);
								int offset = tz.getRawOffset();
								if (i >= STANDARD_LIMIT) {
									offset +=  tz.getDSTSavings();
								}
								localizedNonWall.put(name, new Long(offset));
							} else {
								localizedNonWall.put(name, zoneId);
							}
						}
					}
				}
			} else {		
				// now do localizedCountry_countryCode
				String countryCode = matchesPart(path, countryPrefix, "\"]");
				if (countryCode != null) {
					String name = desiredLocaleFile.getStringValue(path);
					if (name != null) localizedCountry_countryCode.put(name, countryCode);
				}
			}
		}
		// add to main set
		for (Iterator it = localizedNonWall.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			Object value = localizedNonWall.get(key);
			localizedExplicit_zone.put(key, value);
		}
		// now build country_zone. Could check each time for the singleCountries list, but this is simpler
		for (Iterator it = countries_zoneSet.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			Set set = (Set) countries_zoneSet.get(key);
			if (set == null) continue;
			// only use if there is a single element OR there is a singleCountrySet element
			if (set.size() == 1) {
				country_zone.put(key, set.iterator().next());
			} else {
				set = new HashSet(set); // make modifyable
				set.retainAll(singleCountriesSet);
				if (set.size() == 1) {
					country_zone.put(key, set.iterator().next());
				}
			}
		}
		parseInfoBuilt = true;
	}
	
	/**
	 * Internal method for simple building tables
	 */
	private String matchesPart(String input, String prefix, String suffix) {
		if (!input.startsWith(prefix)) return null;
		if (!input.endsWith(suffix)) return null;
		return input.substring(prefix.length(), input.length() - suffix.length());
	}

	/**
	 * Returns the name for a timezone id that will be returned as a fallback.
	 */
	public static String getFallbackName(String zoneid) {
		String result;
		int pos = zoneid.lastIndexOf('/');
		result = pos < 0 ? zoneid : zoneid.substring(pos+1);
		result = result.replace('_', ' ');
		return result;
	}
	/**
	 * Getter
	 */
	public boolean isSkipDraft() {
		return skipDraft;
	}
	/**
	 * Setter
	 */
	public TimezoneFormatter setSkipDraft(boolean skipDraft) {
		this.skipDraft = skipDraft;
		return this;
	}
}
