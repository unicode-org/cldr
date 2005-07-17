/*
 * Created on Jan 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.unicode.cldr.util;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
 * @author davis
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TimezoneFormatter {
	public static final int SHORT = 0, LONG = 1, LENGTH_LIMIT = 2;
	public static final List LENGTH = Arrays.asList(new String[] {"short", "long"});
	public static final int GMT = 0, GENERIC = 1, STANDARD = 2, DAYLIGHT = 3, TYPE_LIMIT = 4;
	public static final List TYPE = Arrays.asList(new String[] {"gmt", "generic", "standard", "daylight"});
	
	//private static final long D2005_01_02 = new Date(2005-1900,0,1).getTime();
	//private static final long D2005_06_02 = new Date(2005-1900,7,1).getTime();
	
	private static final Map zone_countries = StandardCodes.make().getZoneToCounty();
	private static final Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
	private static final Map old_new = StandardCodes.make().getZoneLinkold_new();

	private SimpleDateFormat[] hourFormat = new SimpleDateFormat[2];
	private MessageFormat hoursFormat, gmtFormat, regionFormat,
			fallbackFormat;
	private String abbreviationFallback, preferenceOrdering;
	private CLDRFile desiredLocaleFile;
	private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	private Set singleCountriesSet;
	private boolean skipDraft;
	
	private static int PLUS_FORMAT = 0, MINUS_FORMAT = 1;

	public TimezoneFormatter(Factory cldrFactory, String localeID) {
		CLDRFile desiredLocaleFile = cldrFactory.make(localeID, true);
		this.desiredLocaleFile = desiredLocaleFile;
		String hourFormatString = desiredLocaleFile.getStringValue("/ldml/dates/timeZoneNames/hourFormat");
		String[] hourFormatStrings = Utility.splitArray(hourFormatString,';');
		ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder().setCLDRFactory(cldrFactory);
		hourFormat[PLUS_FORMAT] = icuServiceBuilder.getDateFormat(localeID,0,1);
		hourFormat[PLUS_FORMAT].applyPattern(hourFormatStrings[0]);
		hourFormat[MINUS_FORMAT] = icuServiceBuilder.getDateFormat(localeID,0,1);
		hourFormat[MINUS_FORMAT].applyPattern(hourFormatStrings[1]);
		hoursFormat = new MessageFormat(desiredLocaleFile.getStringValue(
				"/ldml/dates/timeZoneNames/hoursFormat"));
		gmtFormat = new MessageFormat(desiredLocaleFile.getStringValue(
				"/ldml/dates/timeZoneNames/gmtFormat"));
		regionFormat = new MessageFormat(desiredLocaleFile.getStringValue(
				"/ldml/dates/timeZoneNames/regionFormat"));
		fallbackFormat = new MessageFormat(desiredLocaleFile.getStringValue(
				"/ldml/dates/timeZoneNames/fallbackFormat"));
		abbreviationFallback = (String) new XPathParts(null, null).set(
				desiredLocaleFile.getFullXPath("/ldml/dates/timeZoneNames/abbreviationFallback"))
				.findAttributes("abbreviationFallback").get("type");
		String temp = desiredLocaleFile.getFullXPath("/ldml/dates/timeZoneNames/preferenceOrdering");
		preferenceOrdering = (String) new XPathParts(null, null).set(
				temp).findAttributes("preferenceOrdering").get("type");
		temp = desiredLocaleFile.getFullXPath("/ldml/dates/timeZoneNames/singleCountries");
		String singleCountriesList = (String) new XPathParts(null, null).set(
				temp).findAttributes("singleCountries").get("list");
		singleCountriesSet = new TreeSet(Utility.splitList(singleCountriesList, ' '));
	}
	
	public String getFormattedZone(String inputZoneid, String pattern, long date) {
		String zoneid = (String) old_new.get(inputZoneid);
		if (zoneid == null) zoneid = inputZoneid;
		TimeZone tz = TimeZone.getTimeZone(zoneid);
		int gmtOffset1 = tz.getOffset(date);
		boolean daylight = tz.getRawOffset() != gmtOffset1;
		return getFormattedZone(zoneid, pattern, daylight, gmtOffset1);
	}
	
	public String getFormattedZone(String zoneid, String pattern, boolean daylight, int gmtOffset1) {
		int length = pattern.length() < 4 ? SHORT : LONG;
		int type = pattern.startsWith("z") ? (daylight ? DAYLIGHT : STANDARD)
				: pattern.startsWith("Z") ? GMT
				: GENERIC;
		return getFormattedZone(zoneid, length, type, gmtOffset1);
	}
	
	SimpleDateFormat rfc822Plus = new SimpleDateFormat("+HHmm");
	SimpleDateFormat rfc822Minus = new SimpleDateFormat("-HHmm");
	TimeZone gmt = TimeZone.getTimeZone("GMT");
	{
		rfc822Plus.setTimeZone(gmt);
		rfc822Minus.setTimeZone(gmt);
	}
	
	public String getFormattedZone(String inputZoneid, int length, int type, int gmtOffset1) {
		String result;
		String zoneid = (String) old_new.get(inputZoneid);
		if (zoneid == null) zoneid = inputZoneid;
		
		if (type == GMT && length == SHORT) { // just do the RFC, via Java; note that this will actually vary by date, but we hardcode for testing
			return gmtOffset1 < 0 ? rfc822Minus.format(new Date(-gmtOffset1)) : rfc822Plus.format(new Date(gmtOffset1));
		}
/*
<zone type="Europe/Paris">
	<long>
		<generic>Central European Time</generic>
		<standard>Central European Standard Time</standard>
		<daylight>Central European Daylight Time</daylight>
    </long>
    <short>
    	<generic>CET</generic>
    	<standard>CEST</standard>
    	<daylight>CEDT</daylight>
    </short>
    <exemplarCity>Paris</exemplarCity>
</zone>
*/
		String prefix = "/ldml/dates/timeZoneNames/zone[@type=\"" + zoneid + "\"]/";
		// 1. If non-GMT format, and we have an explicit translation, use it
		if (type != GMT) {
			String formatValue = desiredLocaleFile.getStringValue(prefix + LENGTH.get(length) + "/" + TYPE.get(type));
			if (formatValue != null) return formatValue;
		}
		
		String country = (String) zone_countries.get(zoneid);
		// 2. if GMT format or no country, use it
		// 3. Else for non-wall-time, use GMT format
		//	America/Los_Angeles => "GMT-08:00"
		if (type != GENERIC || country.equals(StandardCodes.NO_COUNTRY)) {
			DateFormat format = hourFormat[gmtOffset1 < 0 ? MINUS_FORMAT : PLUS_FORMAT];
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
		String exemplarValue = desiredLocaleFile.getStringValue(prefix + "exemplarCity");
		if (exemplarValue != null) {
			result = exemplarValue;
		} else {
			result = getFallbackName(zoneid);
		}
		
		String countryTranslation = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, skipDraft);
		if (countryTranslation == null) countryTranslation = country;
		return fallbackFormat.format(new Object[]{result, countryTranslation});
		
		// 8. Else use the (possibly multi-offset) GMT format
		//	America/Nome => "GMT-0900/-0800"
		// NOP -- never get there.	
	}
	
	Matcher m = Pattern.compile("(\\+ | \\-) ([0-9][0-9])([0-9][0-9])").matcher("");
	
	/**
	 * Returns zoneid, or if a gmt offset, returns in offset.
	 * @throws ParseException
	 */
	public String parse(String inputText, long[] offsetMillis) {
		// if we haven't parsed before, build parsing info
		if (!parseInfoBuilt) buildParsingInfo();
		// there are the following possible formats
		// explicit strings
		String result = (String) localizedExplicit_zone.get(inputText);
		if (result != null) return result;
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
		try {
			Object[] results = gmtFormat.parse(inputText);
			if (results != null) {
				String hours = (String) results[0];
				Date date = hourFormat[0].parse(hours);
				if (date != null) {
					offsetMillis[0] = date.getTime();
					return "";					
				}
				date = hourFormat[1].parse(hours); // negative format
				if (date != null) {
					offsetMillis[0] = -date.getTime();
					return "";					
				}
			}
		} catch (ParseException e) {}
		
		//	Generic fallback, example: city or city (country)	
		try {
			String city = null, country = null;
			Object[] x = fallbackFormat.parse(inputText);
			if (x != null) {
				city = (String) x[0];
				country = (String) x[1];
				// at this point, we don't really need the country, so ignore it
				// the city could be the last field of a zone, or could be an exemplar city
				// we have built the map so that both work
				return (String) exemplar_zone.get(city);
			} else {
				x = regionFormat.parse(inputText);
				if (x == null) return null; // can't find anything
				country = (String) x[0];
				// see if the string is a localized country
				String countryCode = (String) localizedCountry_countryCode.get(country);
				if (countryCode == null) countryCode = country; // if not, see if it is a raw code
				Set zones = (Set) countries_zoneSet.get(countryCode);
				return (String) zones.iterator().next(); // return first one, best we can do
			}
		} catch (ParseException e1) {}
		
		// last field of zone string
		return null; // not found
	}

	/**
	 * 
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
		for (Iterator it = desiredLocaleFile.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			// dumb, simple implementation
			if (path.startsWith(prefix)) {
				String zoneId = matchesPart(path, prefix, "\"]/exemplarCity");
				if (zoneId != null) {
					String name = desiredLocaleFile.getStringValue(path);
					exemplar_zone.put(name, zoneId);
				}
				for (int i = 0; i < zoneTypes.length; ++i) {
					zoneId = matchesPart(path, prefix, zoneTypes[0]);
					if (zoneId != null) {
						String name = desiredLocaleFile.getStringValue(path);
						localizedExplicit_zone.put(name, zoneId);
					}
				}
			} else {		
				// now do localizedCountry_countryCode
				String countryCode = matchesPart(path, countryPrefix, "\"]");
				if (countryCode != null) {
					String name = desiredLocaleFile.getStringValue(path);
					localizedExplicit_zone.put(name, countryCode);
				}
			}
		}
		parseInfoBuilt = true;
	}
	
	String matchesPart(String input, String prefix, String suffix) {
		if (!input.startsWith(prefix)) return null;
		if (!input.endsWith(suffix)) return null;
		return input.substring(prefix.length(), input.length() - suffix.length());
	}

	static final String[] zoneTypes = {"\"]/long/generic", "\"]/long/standard", "\"]/long/daylight", 
			"\"]/long/short", "\"]/long/short", "\"]/long/short"};
	transient boolean parseInfoBuilt;
	transient static final Map localizedCountry_countryCode= new HashMap();
	transient static final Map exemplar_zone= new HashMap();
	transient static final Map localizedExplicit_zone = new HashMap();
	
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
	public boolean isSkipDraft() {
		return skipDraft;
	}
	public TimezoneFormatter setSkipDraft(boolean skipDraft) {
		this.skipDraft = skipDraft;
		return this;
	}
}
