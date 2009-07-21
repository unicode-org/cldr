/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/

package org.unicode.cldr.util;

import java.text.FieldPosition;
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
import com.ibm.icu.text.UFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.TimeZone;

import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Factory;

/**
 * TimezoneFormatter. Class that uses CLDR data directly to parse / format timezone names
 * according to the specification in TR#35. Note: there are some areas where the spec needs
 * fixing.
 * @author davis
 */
public class TimezoneFormatter extends UFormat  {
	public static boolean SHOW_DRAFT = false;
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
	
  public TimezoneFormatter(Factory cldrFactory, String localeID, boolean includeDraft) {
    this(cldrFactory.make(localeID, true, includeDraft));
  }
  
  public TimezoneFormatter(Factory cldrFactory, String localeID, DraftStatus minimalDraftStatus) {
    this(cldrFactory.make(localeID, true, minimalDraftStatus));
  }
	/**
	 * Create from a cldrFactory and a locale id.
	 * @see CLDRFile
	 */
	public TimezoneFormatter(CLDRFile resolvedLocaleFile) {
		desiredLocaleFile = resolvedLocaleFile;
		inputLocaleID = desiredLocaleFile.getLocaleID();
		String hourFormatString = getStringValue("//ldml/dates/timeZoneNames/hourFormat");
		String[] hourFormatStrings = CldrUtility.splitArray(hourFormatString,';');
		ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder().setCldrFile(desiredLocaleFile);
		hourFormatPlus = icuServiceBuilder.getDateFormat("gregorian",0, 1);
		hourFormatPlus.applyPattern(hourFormatStrings[0]);
		hourFormatMinus = icuServiceBuilder.getDateFormat("gregorian",0, 1);
		hourFormatMinus.applyPattern(hourFormatStrings[1]);
		gmtFormat = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/gmtFormat"));
		regionFormat = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/regionFormat"));
		fallbackFormat = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/fallbackFormat"));
		checkForDraft("//ldml/dates/timeZoneNames/singleCountries");
        // default value if not in root. Only needed for CLDR 1.3
        String singleCountriesList = "Africa/Bamako America/Godthab America/Santiago America/Guayaquil" +
                " Asia/Shanghai Asia/Tashkent Asia/Kuala_Lumpur Europe/Madrid Europe/Lisbon" +
                " Europe/London Pacific/Auckland Pacific/Tahiti";
		String temp = desiredLocaleFile.getFullXPath("//ldml/dates/timeZoneNames/singleCountries");
		if (temp != null) {
			singleCountriesList = (String) new XPathParts(null, null).set(temp)
					.findAttributeValue("singleCountries", "list");
		}
        singleCountriesSet = new TreeSet(CldrUtility.splitList(singleCountriesList, ' '));

		/* not needed
		hoursFormat = new MessageFormat(desiredLocaleFile.getStringValue(
				"//ldml/dates/timeZoneNames/hoursFormat"));
		abbreviationFallback = (String) new XPathParts(null, null).set(
				desiredLocaleFile.getFullXPath("//ldml/dates/timeZoneNames/abbreviationFallback"))
				.findAttributes("abbreviationFallback").get("type");
		temp = desiredLocaleFile.getFullXPath("//ldml/dates/timeZoneNames/preferenceOrdering");
		preferenceOrdering = (String) new XPathParts(null, null).set(
				temp).findAttributes("preferenceOrdering").get("type");
		*/
	}
	
	/**
	 * 
	 */
	private String getStringValue(String cleanPath) {
		checkForDraft(cleanPath);
		return desiredLocaleFile.getWinningValue(cleanPath);
	}

	private String getName(int territory_name, String country, boolean skipDraft2) {
		checkForDraft(CLDRFile.getKey(territory_name,country));
		return desiredLocaleFile.getName(territory_name, country);
	}

	private void checkForDraft(String cleanPath) {
		String xpath = desiredLocaleFile.getFullXPath(cleanPath);
		
		if (SHOW_DRAFT && xpath != null && xpath.indexOf("[@draft=\"true\"]") >= 0) {
			System.out.println("Draft in " + inputLocaleID + ":\t" + cleanPath);
		}
	}
	
	/**
	 * Convenience routine for formatting based on a date
	 * @param skipExact TODO
	 */
	public String getFormattedZone(String inputZoneid, String pattern, long date, boolean skipExact) {
		String zoneid = (String) old_new.get(inputZoneid);
		if (zoneid == null) zoneid = inputZoneid;
		TimeZone tz = TimeZone.getTimeZone(zoneid);
		int gmtOffset1 = tz.getOffset(date);
		boolean daylight = tz.getRawOffset() != gmtOffset1;
		return getFormattedZone(zoneid, pattern, daylight, gmtOffset1, skipExact);
	}
	
	/**
	 * Convenience routine for formatting based on daylight or not, and the offset
	 * @param skipExact TODO
	 */
	public String getFormattedZone(String zoneid, String pattern, boolean daylight, int gmtOffset1, boolean skipExact) {
		int length = pattern.length() < 4 ? SHORT : LONG;
		int type = pattern.startsWith("z") ? (daylight ? DAYLIGHT : STANDARD)
				: pattern.startsWith("Z") ? GMT
				: GENERIC;
		return getFormattedZone(zoneid, length, type, gmtOffset1, skipExact);
	}
	
	/**
	 * Main routine for formatting based on a length (LONG or SHORT), 
	 * a type (GMT, GENERIC, STANDARD, DAYLIGHT), and an offset.
	 * @param skipExact TODO
	 */
	public String getFormattedZone(String inputZoneid, int length, int type, int gmtOffset1, boolean skipExact) {
		String result;
		
//		1.  Canonicalize the Olson ID according to the table in supplemental data.
//		Use that canonical ID in each of the following steps.
//		* America/Atka => America/Adak
//		* Australia/ACT => Australia/Sydney
		
		String zoneid = (String) old_new.get(inputZoneid);
		if (zoneid == null) zoneid = inputZoneid;
		
//		2. If there is an exact translation in the resolved locale, use it. (Note
//		that this translation may not at all be literal: it would be what is most
//		recognizable for people using the target language.)
//		* America/Los_Angeles => "Tampo de Pacifica"
		
		String prefix = "//ldml/dates/timeZoneNames/zone[@type=\"" + zoneid + "\"]/";
		if (type != GMT && !skipExact) {
			String formatValue = getStringValue(prefix + LENGTH.get(length) + "/" + TYPE.get(type));
			if (formatValue != null) return formatValue;
		}
		
//		2a. Else for RFC 822 format ("Z") follow the RFC.
//		*  America/Los_Angeles => "-0800"
		
		if (type == GMT && length == SHORT) {
			return gmtOffset1 < 0 ? rfc822Minus.format(new Date(-gmtOffset1)) : rfc822Plus.format(new Date(gmtOffset1));
		}

//		3. Else for non-wall-time (ie, GMT, daylight, or standard) or where there is
//		no country for the zone (eg, Etc/GMT+3), use the localized GMT format.
//		* America/Los_Angeles (standard time) => "GMT-08:00"
//		* America/Los_Angeles (daylight time) => "HMG-07:00" // French localization
//		* Etc/GMT+3 => "GMT-03:00" // note that Olson tzids have inverse polarity!
		
		String country = (String) zone_countries.get(zoneid);
		if (type != GENERIC || country.equals(StandardCodes.NO_COUNTRY)) {
			DateFormat format = gmtOffset1 < 0 ? hourFormatMinus : hourFormatPlus;
			calendar.setTimeInMillis(Math.abs(gmtOffset1));
			result = format.format(calendar);
			return gmtFormat.format(new Object[]{result});
		}

//		Thus the remaining steps are only applicable to the generic format. In these
//		steps, if the localized country is not available, use the country code. If
//		the localized exemplar city is not available, fall back to the last field of the
//		raw Olson ID (stripping off the prefix, and turning _ into space).
//
//		5. Else if the country for the zone only has one timezone or the zone id is
//		in the singleCountries list, format the localized country with the region
//		format.
//		* Africa/Monrovia => LR => "Tampo de Liberja"
//		* America/Havana => CU => "Tampo de CU" // if CU is not localized
		
		String countryTranslation = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country);
		if (countryTranslation == null) countryTranslation = country;
		
		Set s = (Set) countries_zoneSet.get(country);
		if (s != null && s.size() == 1 || singleCountriesSet.contains(zoneid)) {
			return regionFormat.format(new Object[]{countryTranslation});
		}

//		7. Else get the exemplar city and localized country, and format them with
//		the fallback format (as parameters 0 and 1, respectively).
//		* America/Buenos_Aires => "??????-????? (?????????)"
//		* America/Buenos_Aires => "??????-????? (AR)" // if Argentina isn't translated
//		* America/Buenos_Aires => "Buenos Aires (?????????)" // if Buenos Aires isn't
//		* America/Buenos_Aires => "Buenos Aires (AR)" // if both aren't

		String exemplarValue = getStringValue(prefix + "exemplarCity");
		if (exemplarValue == null) exemplarValue = getFallbackName(zoneid);
		
    String cityformat = fallbackFormat.format(new Object[]{exemplarValue, countryTranslation});
		return regionFormat.format(new Object[]{cityformat});
	}
	
	/**
	 * Used for computation in parsing
	 */
	private static final int WALL_LIMIT = 2, STANDARD_LIMIT = 4;
	private static final String[] zoneTypes = {"\"]/long/generic", "\"]/short/generic",
			"\"]/long/standard", "\"]/short/standard", 
			"\"]/long/daylight", "\"]/short/daylight"};

	private transient Matcher m = Pattern.compile("([-+])([0-9][0-9])([0-9][0-9])").matcher("");

	private transient boolean parseInfoBuilt;
	private transient final Map localizedCountry_countryCode= new HashMap();
	private transient final Map exemplar_zone= new HashMap();
	private transient final Map localizedExplicit_zone = new HashMap();
	private transient final Map country_zone = new HashMap();
	
	/**
	 * Returns zoneid. In case of an offset, returns "Etc/GMT+/-HH" or "Etc/GMT+/-HHmm".
	 * Remember that Olson IDs have reversed signs!
	 *
	 */
	public String parse(String inputText, ParsePosition parsePosition) {
		long[] offsetMillisOutput = new long[1];
		String result = parse(inputText, parsePosition, offsetMillisOutput);
		if (result == null || result.length() != 0) return result;
		long offsetMillis = offsetMillisOutput[0];
		String sign = "Etc/GMT-";
		if (offsetMillis < 0) {
			offsetMillis = -offsetMillis;
			sign = "Etc/GMT+";
		}
		long minutes = (offsetMillis + 30*1000) / (60*1000);
		long hours = minutes / 60;
		minutes = minutes % 60;
		result = sign + String.valueOf(hours);
		if (minutes != 0) result += ":" + String.valueOf(100 + minutes).substring(1,3);
		return result;	
	}
	/**
	 * Returns zoneid, or if a gmt offset, returns "" and a millis value in offsetMillis[0].
	 * If we can't parse, return null
	 */
	public String parse(String inputText, ParsePosition parsePosition, long[] offsetMillis) {
		// if we haven't parsed before, build parsing info
		if (!parseInfoBuilt) buildParsingInfo();
		int startOffset = parsePosition.getIndex();
		// there are the following possible formats
		
		// Explicit strings
		// If the result is a Long it is millis, otherwise it is the zoneID
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

		Object[] results = gmtFormat.parse(inputText, parsePosition);
		if (results != null) {
      if (results.length == 0) {
        // for debugging
        results = gmtFormat.parse(inputText, parsePosition);
      }
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
    
    // first remove the region format if possible
    
    parsePosition.setIndex(startOffset);
    Object[] x = regionFormat.parse(inputText, parsePosition);
    if (x != null) {
      inputText = (String) x[0];
    }

		String city = null, country = null;
		parsePosition.setIndex(startOffset);
		x = fallbackFormat.parse(inputText, parsePosition);
		if (x != null) {
			city = (String) x[0];
			country = (String) x[1];
			// at this point, we don't really need the country, so ignore it
			// the city could be the last field of a zone, or could be an exemplar city
			// we have built the map so that both work
			return (String) exemplar_zone.get(city);
		}
		
		// see if the string is a localized country
		String countryCode = (String) localizedCountry_countryCode.get(inputText);
		if (countryCode == null) countryCode = country; // if not, try raw code
		return (String) country_zone.get(countryCode);
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
		String prefix = "//ldml/dates/timeZoneNames/zone[@type=\"";
		String countryPrefix = "//ldml/localeDisplayNames/territories/territory[@type=\"";
		Map localizedNonWall = new HashMap();
		Set skipDuplicates = new HashSet();
		for (Iterator it = desiredLocaleFile.iterator(); it.hasNext();) {
			String path = (String) it.next();
			// dumb, simple implementation
			if (path.startsWith(prefix)) {
				String zoneId = matchesPart(path, prefix, "\"]/exemplarCity");
				if (zoneId != null) {
					String name = desiredLocaleFile.getWinningValue(path);
					if (name != null) exemplar_zone.put(name, zoneId);
				}
				for (int i = 0; i < zoneTypes.length; ++i) {
					zoneId = matchesPart(path, prefix, zoneTypes[i]);
					if (zoneId != null) {
						String name = desiredLocaleFile.getWinningValue(path);
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

    public Object parseObject(String source, ParsePosition pos) {
        TimeZone foo;
        CurrencyAmount fii;
        com.ibm.icu.text.UnicodeSet fuu;
        return null;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        // TODO Auto-generated method stub
        return null;
    }
}
