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

import org.unicode.cldr.util.CLDRFile.Value;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

/**
 * @author davis
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TimezoneFormatter {
	public static final int GMT = 0, SHORT = 1, LONG = 2, LENGTH_LIMIT = 3;
	public static final List LENGTH = Arrays.asList(new String[] {"gmt", "short", "long"});
	public static final int GENERIC = 0, STANDARD = 1, DAYLIGHT = 2, TYPE_LIMIT = 3;
	public static final List TYPE = Arrays.asList(new String[] {"generic", "standard", "daylight"});
	
	private static final long D2005_01_02 = new Date(2005-1900,0,1).getTime();
	private static final long D2005_06_02 = new Date(2005-1900,7,1).getTime();
	
	private DateFormat[] hourFormat = new DateFormat[2];
	private MessageFormat hoursFormat, gmtFormat, regionFormat,
			fallbackFormat;
	private String abbreviationFallback, preferenceOrdering;
	private CLDRFile desiredLocaleFile;
	private Map zone_countries = StandardCodes.make().getZoneToCounty();
	private Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
	private Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

	public TimezoneFormatter(CLDRFile desiredLocaleFile) {
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
	
	public String getFormattedZone(String zoneid, int length, int type) {
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
			Value formatValue = desiredLocaleFile.getValue(prefix + LENGTH.get(length) + "/" + TYPE.get(type));
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

		// 5. *Else if there is a country for the time zone, and a translation in the locale for the country name, and the country only has one (modern) timezone, use it with the region format :
		//	Africa/Monrovia => LR => "Tampo de Liberja"
		Set s = (Set) countries_zoneSet.get(country);
		if (s != null && s.size() == 1) {
			result = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, country, false);
			if (result != null) return regionFormat.format(new Object[]{result});
		}
		// 6. Else if it is a perpetual alias for a "real" ID, and if there is an exact translation for that, try #1..#4 with that alias.
		//	Europe/San_Marino => Europe/Rome => ... => "Tampo de Roma"
		
		// TODO
		
		// 7. Else fall back to the raw Olson ID (stripping off the prefix, and turning _ into space), using the fallback format. 
		//	America/Goose_Bay => "Tampo de  «Goose Bay»"
		Value exemplarValue = desiredLocaleFile.getValue(prefix + "exemplarCity");
		if (exemplarValue != null) {
			result = exemplarValue.getStringValue();
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
	 * @param zoneid
	 * @return
	 */
	public static String getFallbackName(String zoneid) {
		String result;
		int pos = zoneid.lastIndexOf('/');
		result = pos < 0 ? zoneid : zoneid.substring(pos+1);
		result = result.replace('_', ' ');
		return result;
	}
}
