/*
 * Created on Jan 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
	
	private static final long D2005_01_02 = new Date(2005-1900,0,1).getTime();
	private static final long D2005_06_02 = new Date(2005-1900,7,1).getTime();
	
	private SimpleDateFormat[] hourFormat = new SimpleDateFormat[2];
	private MessageFormat hoursFormat, gmtFormat, regionFormat,
			fallbackFormat;
	private String abbreviationFallback, preferenceOrdering;
	private CLDRFile desiredLocaleFile;
	private Map zone_countries = StandardCodes.make().getZoneToCounty();
	private Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
	private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	private Set singleCountriesSet;
	private Map old_new = StandardCodes.make().getZoneLinkold_new();

	public TimezoneFormatter(Factory cldrFactory, String localeID) {
		CLDRFile desiredLocaleFile = cldrFactory.make(localeID, true);
		this.desiredLocaleFile = desiredLocaleFile;
		String hourFormatString = desiredLocaleFile.getStringValue("/ldml/dates/timeZoneNames/hourFormat");
		String[] hourFormatStrings = Utility.splitArray(hourFormatString,';');
		ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder().setCLDRFactory(cldrFactory);
		hourFormat[0] = icuServiceBuilder.getDateFormat(localeID,0,1);
		hourFormat[0].applyPattern(hourFormatStrings[0]);
		hourFormat[1] = icuServiceBuilder.getDateFormat(localeID,0,1);
		hourFormat[1].applyPattern(hourFormatStrings[1]);
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
	
	public String getFormattedZone(String zoneid, String pattern, Date date) {
		int length = pattern.length() < 4 ? SHORT : LONG;
		int type = pattern.startsWith("z") ? (daylight ? DAYLIGHT : STANDARD)
				: pattern.startsWith("Z") ? GMT
				: GENERIC;
		return getFormattedZone(zoneid, length, type, date);
	}
	
	public String getFormattedZone(String zoneid, String pattern, boolean daylight) {
		int length = pattern.length() < 4 ? SHORT : LONG;
		int type = pattern.startsWith("z") ? (daylight ? DAYLIGHT : STANDARD)
				: pattern.startsWith("Z") ? GMT
				: GENERIC;
		return getFormattedZone(zoneid, length, type);
	}
	
	SimpleDateFormat rfc822Plus = new SimpleDateFormat("+HHmm");
	SimpleDateFormat rfc822Minus = new SimpleDateFormat("-HHmm");
	TimeZone gmt = TimeZone.getTimeZone("GMT");
	{
		rfc822Plus.setTimeZone(gmt);
		rfc822Minus.setTimeZone(gmt);
	}
	
	public String getFormattedZone(String inputZoneid, int length, int type) {
		String result;
		String zoneid = (String) old_new.get(inputZoneid);
		if (zoneid == null) zoneid = inputZoneid;
		
		if (type == GMT && length == SHORT) { // just do the RFC, via Java; note that this will actually vary by date, but we hardcode for testing
			TimeZone tz = TimeZone.getTimeZone(zoneid);
			int offset = tz.getRawOffset();
			return offset < 0 ? rfc822Minus.format(new Date(-offset)) : rfc822Plus.format(new Date(offset));
		}
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
		if (type != GMT) {
			String formatValue = desiredLocaleFile.getStringValue(prefix + LENGTH.get(length) + "/" + TYPE.get(type));
			if (formatValue != null) return formatValue;
		}
		
		String country = (String) zone_countries.get(zoneid);
		// 2. if GMT format or no country, use it
		if (type != GENERIC || country.equals(StandardCodes.NO_COUNTRY)) {
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

		// 5. *Else if there is a country for the time zone, and a translation in the locale for the country name, and the country only has one (modern) timezone, use it with the region format :
		//	Africa/Monrovia => LR => "Tampo de Liberja"
		Set s = (Set) countries_zoneSet.get(country);
		if (s != null && s.size() == 1 || singleCountriesSet.contains(zoneid)) {
			result = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, false);
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
		String countryTranslation = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, false);
		if (countryTranslation == null) countryTranslation = country;
		return fallbackFormat.format(new Object[]{result, countryTranslation});
		// 8. Else use the (possibly multi-offset) GMT format
		//	America/Nome => "GMT-0900/-0800"

		// TODO 
		
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
}
