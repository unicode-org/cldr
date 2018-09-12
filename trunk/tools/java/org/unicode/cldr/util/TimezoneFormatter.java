/*
 * *********************************************************************
 * Copyright (c) 2002-2004, International Business Machines Corporation and others. All Rights Reserved.
 * *********************************************************************
 * Author: Mark Davis
 * *********************************************************************
 */

package org.unicode.cldr.util;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.SupplementalDataInfo.MetaZoneRange;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UFormat;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;

/**
 * TimezoneFormatter. Class that uses CLDR data directly to parse / format timezone names according to the specification
 * in TR#35. Note: there are some areas where the spec needs fixing.
 *
 *
 * @author davis
 */

public class TimezoneFormatter extends UFormat {

    /**
     *
     */
    private static final long serialVersionUID = -506645087792499122L;
    private static final long TIME = new Date().getTime();
    public static boolean SHOW_DRAFT = false;

    public enum Location {
        GMT, LOCATION, NON_LOCATION;
        public String toString() {
            return this == GMT ? "gmt" : this == LOCATION ? "location" : "non-location";
        }
    }

    public enum Type {
        GENERIC, SPECIFIC;
        public String toString(boolean daylight) {
            return this == GENERIC ? "generic" : daylight ? "daylight" : "standard";
        }

        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public enum Length {
        SHORT, LONG, OTHER;
        public String toString() {
            return this == SHORT ? "short" : this == LONG ? "long" : "other";
        }
    }

    public enum Format {
        VVVV(Type.GENERIC, Location.LOCATION, Length.OTHER), vvvv(Type.GENERIC, Location.NON_LOCATION, Length.LONG), v(Type.GENERIC, Location.NON_LOCATION,
            Length.SHORT), zzzz(Type.SPECIFIC, Location.NON_LOCATION, Length.LONG), z(Type.SPECIFIC, Location.NON_LOCATION, Length.SHORT), ZZZZ(Type.GENERIC,
                Location.GMT, Length.LONG), Z(Type.GENERIC, Location.GMT, Length.SHORT), ZZZZZ(Type.GENERIC, Location.GMT, Length.OTHER);
        final Type type;
        final Location location;
        final Length length;

        private Format(Type type, Location location, Length length) {
            this.type = type;
            this.location = location;
            this.length = length;
        }
    };

    // /**
    // * Type parameter for formatting
    // */
    // public static final int GMT = 0, GENERIC = 1, STANDARD = 2, DAYLIGHT = 3, TYPE_LIMIT = 4;
    //
    // /**
    // * Arrays of names, for testing. Should be const, but we can't do that in Java
    // */
    // public static final List LENGTH = Arrays.asList(new String[] {"short", "long"});
    // public static final List TYPE = Arrays.asList(new String[] {"gmt", "generic", "standard", "daylight"});

    // static fields built from Timezone Database for formatting and parsing

    // private static final Map zone_countries = StandardCodes.make().getZoneToCounty();
    // private static final Map countries_zoneSet = StandardCodes.make().getCountryToZoneSet();
    // private static final Map old_new = StandardCodes.make().getZoneLinkold_new();

    private static SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();

    // instance fields built from CLDR data for formatting and parsing

    private transient SimpleDateFormat hourFormatPlus = new SimpleDateFormat();
    private transient SimpleDateFormat hourFormatMinus = new SimpleDateFormat();
    private transient MessageFormat gmtFormat, regionFormat,
        regionFormatStandard, regionFormatDaylight, fallbackFormat;
    //private transient String abbreviationFallback, preferenceOrdering;
    private transient Set<String> singleCountriesSet;

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
     *
     * @see CLDRFile
     */
    public TimezoneFormatter(CLDRFile resolvedLocaleFile) {
        desiredLocaleFile = resolvedLocaleFile;
        inputLocaleID = desiredLocaleFile.getLocaleID();
        String hourFormatString = getStringValue("//ldml/dates/timeZoneNames/hourFormat");
        String[] hourFormatStrings = CldrUtility.splitArray(hourFormatString, ';');
        ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder().setCldrFile(desiredLocaleFile);
        hourFormatPlus = icuServiceBuilder.getDateFormat("gregorian", 0, 1);
        hourFormatPlus.applyPattern(hourFormatStrings[0]);
        hourFormatMinus = icuServiceBuilder.getDateFormat("gregorian", 0, 1);
        hourFormatMinus.applyPattern(hourFormatStrings[1]);
        gmtFormat = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/gmtFormat"));
        regionFormat = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/regionFormat"));
        regionFormatStandard = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/regionFormat[@type=\"standard\"]"));
        regionFormatDaylight = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/regionFormat[@type=\"daylight\"]"));
        fallbackFormat = new MessageFormat(getStringValue("//ldml/dates/timeZoneNames/fallbackFormat"));
        checkForDraft("//ldml/dates/timeZoneNames/singleCountries");
        // default value if not in root. Only needed for CLDR 1.3
        String singleCountriesList = "Africa/Bamako America/Godthab America/Santiago America/Guayaquil"
            + " Asia/Shanghai Asia/Tashkent Asia/Kuala_Lumpur Europe/Madrid Europe/Lisbon"
            + " Europe/London Pacific/Auckland Pacific/Tahiti";
        String temp = desiredLocaleFile.getFullXPath("//ldml/dates/timeZoneNames/singleCountries");
        if (temp != null) {
            singleCountriesList = (String) new XPathParts(null, null).set(temp).findAttributeValue("singleCountries",
                "list");
        }
        singleCountriesSet = new TreeSet<String>(CldrUtility.splitList(singleCountriesList, ' '));
    }

    /**
     *
     */
    private String getStringValue(String cleanPath) {
        checkForDraft(cleanPath);
        return desiredLocaleFile.getWinningValue(cleanPath);
    }

    private String getName(int territory_name, String country, boolean skipDraft2) {
        checkForDraft(CLDRFile.getKey(territory_name, country));
        return desiredLocaleFile.getName(territory_name, country);
    }

    private void checkForDraft(String cleanPath) {
        String xpath = desiredLocaleFile.getFullXPath(cleanPath);

        if (SHOW_DRAFT && xpath != null && xpath.indexOf("[@draft=\"true\"]") >= 0) {
            System.out.println("Draft in " + inputLocaleID + ":\t" + cleanPath);
        }
    }

    /**
     * Formatting based on pattern and date.
     */
    public String getFormattedZone(String zoneid, String pattern, long date) {
        Format format = Format.valueOf(pattern);
        return getFormattedZone(zoneid, format.location, format.type, format.length, date);
    }

    /**
     * Formatting based on broken out features and date.
     */
    public String getFormattedZone(String inputZoneid, Location location, Type type, Length length, long date) {
        String zoneid = TimeZone.getCanonicalID(inputZoneid);
        BasicTimeZone timeZone = (BasicTimeZone) TimeZone.getTimeZone(zoneid);
        int gmtOffset1 = timeZone.getOffset(date);
        MetaZoneRange metaZoneRange = sdi.getMetaZoneRange(zoneid, date);
        String metazone = metaZoneRange == null ? "?" : metaZoneRange.metazone;
        boolean noTimezoneChangeWithin184Days = noTimezoneChangeWithin184Days(timeZone, date);
        boolean daylight = gmtOffset1 != timeZone.getRawOffset();
        return getFormattedZone(inputZoneid, location, type, length, daylight, gmtOffset1, metazone,
            noTimezoneChangeWithin184Days);
    }

    /**
     * Low-level routine for formatting based on zone, broken-out features, plus special settings (which are usually
     * computed from the date, but are here for specific access.)
     *
     * @param inputZoneid
     * @param location
     * @param type
     * @param length
     * @param daylight
     * @param gmtOffset1
     * @param metazone
     * @param noTimezoneChangeWithin184Days
     * @return
     */
    public String getFormattedZone(String inputZoneid, Location location, Type type, Length length, boolean daylight,
        int gmtOffset1, String metazone, boolean noTimezoneChangeWithin184Days) {
        String formatted = getFormattedZoneInternal(inputZoneid, location, type, length, daylight, gmtOffset1,
            metazone, noTimezoneChangeWithin184Days);
        if (formatted != null) {
            return formatted;
        }
        if (type == Type.GENERIC && location == Location.NON_LOCATION) {
            formatted = getFormattedZone(inputZoneid, Location.LOCATION, type, length, daylight, gmtOffset1, metazone,
                noTimezoneChangeWithin184Days);
            if (formatted != null) {
                return formatted;
            }
        }
        return getFormattedZone(inputZoneid, Location.GMT, null, Length.LONG, daylight, gmtOffset1, metazone,
            noTimezoneChangeWithin184Days);
    }

    private String getFormattedZoneInternal(String inputZoneid, Location location, Type type, Length length,
        boolean daylight, int gmtOffset1, String metazone, boolean noTimezoneChangeWithin184Days) {

        String result;
        // 1. Canonicalize the Olson ID according to the table in supplemental data.
        // Use that canonical ID in each of the following steps.
        // * America/Atka => America/Adak
        // * Australia/ACT => Australia/Sydney

        String zoneid = TimeZone.getCanonicalID(inputZoneid);
        // BasicTimeZone timeZone = (BasicTimeZone) TimeZone.getTimeZone(zoneid);
        // if (zoneid == null) zoneid = inputZoneid;

        switch (location) {
        default:
            throw new IllegalArgumentException("Bad enum value for location: " + location);

        case GMT:
            // 2. For RFC 822 GMT format ("Z") return the results according to the RFC.
            // America/Los_Angeles → "-0800"
            // Note: The digits in this case are always from the western digits, 0..9.
            if (length == Length.SHORT) {
                return gmtOffset1 < 0 ? rfc822Minus.format(new Date(-gmtOffset1)) : rfc822Plus.format(new Date(
                    gmtOffset1));
            }

            // 3. For the localized GMT format, use the gmtFormat (such as "GMT{0}" or "HMG{0}") with the hourFormat
            // (such as "+HH:mm;-HH:mm" or "+HH.mm;-HH.mm").
            // America/Los_Angeles → "GMT-08:00" // standard time
            // America/Los_Angeles → "HMG-07:00" // daylight time
            // Etc/GMT+3 → "GMT-03.00" // note that TZ tzids have inverse polarity!
            // Note: The digits should be whatever are appropriate for the locale used to format the time zone, not
            // necessarily from the western digits, 0..9. For example, they might be from ०..९.

            DateFormat format = gmtOffset1 < 0 ? hourFormatMinus : hourFormatPlus;
            calendar.setTimeInMillis(Math.abs(gmtOffset1));
            result = format.format(calendar);
            return gmtFormat.format(new Object[] { result });
        // 4. For ISO 8601 time zone format ("ZZZZZ") return the results according to the ISO 8601.
        // America/Los_Angeles → "-08:00"
        // Etc/GMT → Z // special case of UTC
        // Note: The digits in this case are always from the western digits, 0..9.

        // TODO
        case NON_LOCATION:
            // 5. For the non-location formats (generic or specific),
            // 5.1 if there is an explicit translation for the TZID in timeZoneNames according to type (generic,
            // standard, or daylight) in the resolved locale, return it.
            // America/Los_Angeles → "Heure du Pacifique (ÉUA)" // generic
            // America/Los_Angeles → 太平洋標準時 // standard
            // America/Los_Angeles → Yhdysvaltain Tyynenmeren kesäaika // daylight
            // Europe/Dublin → Am Samhraidh na hÉireann // daylight
            // Note: This translation may not at all be literal: it would be what is most recognizable for people using
            // the target language.

            String formatValue = getLocalizedExplicitTzid(zoneid, type, length, daylight);
            if (formatValue != null) {
                return formatValue;
            }

            // 5.2 Otherwise, if there is a metazone standard format,
            // and the offset and daylight offset do not change within 184 day +/- interval
            // around the exact formatted time, use the metazone standard format ("Mountain Standard Time" for Phoenix).
            // (184 is the smallest number that is at least 6 months AND the smallest number that is more than 1/2 year
            // (Gregorian)).
            if (metazone == null) {
                metazone = sdi.getMetaZoneRange(zoneid, TIME).metazone;
            }
            String metaZoneName = getLocalizedMetazone(metazone, type, length, daylight);
            if (metaZoneName == null && noTimezoneChangeWithin184Days) {
                metaZoneName = getLocalizedMetazone(metazone, Type.SPECIFIC, length, false);
            }

            // 5.3 Otherwise, if there is a metazone generic format, then do the following:
            // *** CHANGE to
            // 5.2 Get the appropriate metazone format (generic, standard, daylight).
            // if there is none, (do old 5.2).
            // if there is either one, then do the following

            if (metaZoneName != null) {

                // 5.3.1 Compare offset at the requested time with the preferred zone for the current locale; if same,
                // we use the metazone generic format.
                // "Pacific Time" for Vancouver if the locale is en-CA, or for Los Angeles if locale is en-US. Note that
                // the fallback is the golden zone.
                // The metazone data actually supplies the preferred zone for a country.
                String localeId = desiredLocaleFile.getLocaleID();
                LanguageTagParser languageTagParser = new LanguageTagParser();
                String defaultRegion = languageTagParser.set(localeId).getRegion();
                // If the locale does not have a country the likelySubtags supplemental data is used to get the most
                // likely country.
                if (defaultRegion.isEmpty()) {
                    String localeMax = LikelySubtags.maximize(localeId, sdi.getLikelySubtags());
                    defaultRegion = languageTagParser.set(localeMax).getRegion();
                    if (defaultRegion.isEmpty()) {
                        return "001"; // CLARIFY
                    }
                }
                Map<String, String> regionToZone = sdi.getMetazoneToRegionToZone().get(metazone);
                String preferredLocalesZone = regionToZone.get(defaultRegion);
                if (preferredLocalesZone == null) {
                    preferredLocalesZone = regionToZone.get("001");
                }
                // TimeZone preferredTimeZone = TimeZone.getTimeZone(preferredZone);
                // CLARIFY: do we mean that the offset is the same at the current time, or that the zone is the same???
                // the following code does the latter.
                if (zoneid.equals(preferredLocalesZone)) {
                    return metaZoneName;
                }

                // 5.3.2 If the zone is the preferred zone for its country but not for the country of the locale, use
                // the metazone generic format + (country)
                // [Generic partial location] "Pacific Time (Canada)" for the zone Vancouver in the locale en_MX.

                String zoneIdsCountry = TimeZone.getRegion(zoneid);
                String preferredZonesCountrysZone = regionToZone.get(zoneIdsCountry);
                if (preferredZonesCountrysZone == null) {
                    preferredZonesCountrysZone = regionToZone.get("001");
                }
                if (zoneid.equals(preferredZonesCountrysZone)) {
                    String countryName = getLocalizedCountryName(zoneIdsCountry);
                    return fallbackFormat.format(new Object[] { countryName, metaZoneName }); // UGLY, should be able to
                    // just list
                }

                // If all else fails, use metazone generic format + (city).
                // [Generic partial location]: "Mountain Time (Phoenix)", "Pacific Time (Whitehorse)"
                String cityName = getLocalizedExemplarCity(zoneid);
                return fallbackFormat.format(new Object[] { cityName, metaZoneName });
            }
            //
            // Otherwise, fall back.
            // Note: In composing the metazone + city or country: use the fallbackFormat
            //
            // {1} will be the metazone
            // {0} will be a qualifier (city or country)
            // Example: Pacific Time (Phoenix)

            if (length == Length.LONG) {
                return getRegionFallback(zoneid,
                    type == Type.GENERIC || noTimezoneChangeWithin184Days ? regionFormat
                        : daylight ? regionFormatDaylight : regionFormatStandard);
            }
            return null;

        case LOCATION:

            // 6.1 For the generic location format:
            return getRegionFallback(zoneid, regionFormat);

        // FIX examples
        // Otherwise, get both the exemplar city and country name. Format them with the fallbackRegionFormat (for
        // example, "{1} Time ({0})". For example:
        // America/Buenos_Aires → "Argentina Time (Buenos Aires)"
        // // if the fallbackRegionFormat is "{1} Time ({0})".
        // America/Buenos_Aires → "Аргентина (Буэнос-Айрес)"
        // // if both are translated, and the fallbackRegionFormat is "{1} ({0})".
        // America/Buenos_Aires → "AR (Буэнос-Айрес)"
        // // if Argentina is not translated.
        // America/Buenos_Aires → "Аргентина (Buenos Aires)"
        // // if Buenos Aires is not translated.
        // America/Buenos_Aires → "AR (Buenos Aires)"
        // // if both are not translated.
        // Note: As with the regionFormat, exceptional cases need to be explicitly translated.
        }
    }

    private String getRegionFallback(String zoneid, MessageFormat regionFallbackFormat) {
        // Use as the country name, the explicitly localized country if available, otherwise the raw country code.
        // If the localized exemplar city is not available, use as the exemplar city the last field of the raw TZID,
        // stripping off the prefix and turning _ into space.
        // CU → "CU" // no localized country name for Cuba

        // CLARIFY that above applies to 5.3.2 also!

        // America/Los_Angeles → "Los Angeles" // no localized exemplar city
        // From <timezoneData> get the country code for the zone, and determine whether there is only one timezone
        // in the country.
        // If there is only one timezone or the zone id is in the singleCountries list,
        // format the country name with the regionFormat (for example, "{0} Time"), and return it.
        // Europe/Rome → IT → Italy Time // for English
        // Africa/Monrovia → LR → "Hora de Liberja"
        // America/Havana → CU → "Hora de CU" // if CU is not localized
        // Note: If a language does require grammatical changes when composing strings, then it should either use a
        // neutral format such as what is in root, or put all exceptional cases in explicitly translated strings.
        //

        // Note: <timezoneData> may not have data for new TZIDs.
        //
        // If the country for the zone cannot be resolved, format the exemplar city
        // (it is unlikely that the localized exemplar city is available in this case,
        // so the exemplar city might be composed by the last field of the raw TZID as described above)
        // with the regionFormat (for example, "{0} Time"), and return it.
        // ***FIX by changing to: if the country can't be resolved, or the zonesInRegion are not unique

        String zoneIdsCountry = TimeZone.getRegion(zoneid);
        if (zoneIdsCountry != null) {
            String[] zonesInRegion = TimeZone.getAvailableIDs(zoneIdsCountry);
            if (zonesInRegion != null && zonesInRegion.length == 1 || singleCountriesSet.contains(zoneid)) {
                String countryName = getLocalizedCountryName(zoneIdsCountry);
                return regionFallbackFormat.format(new Object[] { countryName });
            }
        }
        String cityName = getLocalizedExemplarCity(zoneid);
        return regionFallbackFormat.format(new Object[] { cityName });
    }

    public boolean noTimezoneChangeWithin184Days(BasicTimeZone timeZone, long date) {
        // TODO Fix this to look at the real times
        TimeZoneTransition startTransition = timeZone.getPreviousTransition(date, true);
        if (startTransition == null) {
            //System.out.println("No transition for " + timeZone.getID() + " on " + new Date(date));
            return true;
        }
        if (!atLeast184Days(startTransition.getTime(), date)) {
            return false;
        } else {
            TimeZoneTransition nextTransition = timeZone.getNextTransition(date, false);
            if (nextTransition != null && !atLeast184Days(date, nextTransition.getTime())) {
                return false;
            }
        }
        return true;
    }

    private boolean atLeast184Days(long start, long end) {
        long transitionDays = (end - start) / (24 * 60 * 60 * 1000);
        return transitionDays >= 184;
    }

    private String getLocalizedExplicitTzid(String zoneid, Type type, Length length, boolean daylight) {
        String formatValue = desiredLocaleFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\"" + zoneid
            + "\"]/" + length.toString() + "/" + type.toString(daylight));
        return formatValue;
    }

    public String getLocalizedMetazone(String metazone, Type type, Length length, boolean daylight) {
        if (metazone == null) {
            return null;
        }
        String name = desiredLocaleFile.getWinningValue("//ldml/dates/timeZoneNames/metazone[@type=\"" + metazone
            + "\"]/" + length.toString() + "/" + type.toString(daylight));
        return name;
    }

    private String getLocalizedCountryName(String zoneIdsCountry) {
        String countryName = desiredLocaleFile.getName(CLDRFile.TERRITORY_NAME, zoneIdsCountry);
        if (countryName == null) {
            countryName = zoneIdsCountry;
        }
        return countryName;
    }

    public String getLocalizedExemplarCity(String timezoneString) {
        String exemplarCity = desiredLocaleFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\""
            + timezoneString + "\"]/exemplarCity");
        if (exemplarCity == null) {
            exemplarCity = timezoneString.substring(timezoneString.lastIndexOf('/') + 1).replace('_', ' ');
        }
        return exemplarCity;
    }

    /**
     * Used for computation in parsing
     */
    private static final int WALL_LIMIT = 2, STANDARD_LIMIT = 4;
    private static final String[] zoneTypes = { "\"]/long/generic", "\"]/short/generic", "\"]/long/standard",
        "\"]/short/standard", "\"]/long/daylight", "\"]/short/daylight" };

    private transient Matcher m = PatternCache.get("([-+])([0-9][0-9])([0-9][0-9])").matcher("");

    private transient boolean parseInfoBuilt;
    private transient final Map<String, String> localizedCountry_countryCode = new HashMap<String, String>();
    private transient final Map<String, String> exemplar_zone = new HashMap<String, String>();
    private transient final Map<Object, Object> localizedExplicit_zone = new HashMap<Object, Object>();
    private transient final Map<String, String> country_zone = new HashMap<String, String>();

    /**
     * Returns zoneid. In case of an offset, returns "Etc/GMT+/-HH" or "Etc/GMT+/-HHmm".
     * Remember that Olson IDs have reversed signs!
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
        long minutes = (offsetMillis + 30 * 1000) / (60 * 1000);
        long hours = minutes / 60;
        minutes = minutes % 60;
        result = sign + String.valueOf(hours);
        if (minutes != 0) result += ":" + String.valueOf(100 + minutes).substring(1, 3);
        return result;
    }

    /**
     * Returns zoneid, or if a gmt offset, returns "" and a millis value in offsetMillis[0]. If we can't parse, return
     * null
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
            if (result instanceof String) return (String) result;
            offsetMillis[0] = ((Long) result).longValue();
            return "";
        }

        // RFC 822
        if (m.reset(inputText).matches()) {
            int hours = Integer.parseInt(m.group(2));
            int minutes = Integer.parseInt(m.group(3));
            int millis = hours * 60 * 60 * 1000 + minutes * 60 * 1000;
            if (m.group(1).equals("-")) millis = -millis; // check sign!
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

        // Generic fallback, example: city or city (country)

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

        // // do old ones first, we don't care if they are overriden
        // for (Iterator it = old_new.keySet().iterator(); it.hasNext();) {
        // String zoneid = (String) it.next();
        // exemplar_zone.put(getFallbackName(zoneid), zoneid);
        // }

        // then canonical ones
        for (String zoneid : TimeZone.getAvailableIDs()) {
            exemplar_zone.put(getFallbackName(zoneid), zoneid);
        }

        // now add exemplar cities, AND pick up explicit strings, AND localized countries
        String prefix = "//ldml/dates/timeZoneNames/zone[@type=\"";
        String countryPrefix = "//ldml/localeDisplayNames/territories/territory[@type=\"";
        Map<String, Comparable> localizedNonWall = new HashMap<String, Comparable>();
        Set<String> skipDuplicates = new HashSet<String>();
        for (Iterator<String> it = desiredLocaleFile.iterator(); it.hasNext();) {
            String path = it.next();
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
                                // TODO: use Etc/GMT... localizedNonWall.remove(name);
                                TimeZone tz = TimeZone.getTimeZone(zoneId);
                                int offset = tz.getRawOffset();
                                if (i >= STANDARD_LIMIT) {
                                    offset += tz.getDSTSavings();
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
        for (Iterator<String> it = localizedNonWall.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            Object value = localizedNonWall.get(key);
            localizedExplicit_zone.put(key, value);
        }
        // now build country_zone. Could check each time for the singleCountries list, but this is simpler
        for (String key : StandardCodes.make().getGoodAvailableCodes("territory")) {
            String[] tzids = TimeZone.getAvailableIDs(key);
            if (tzids == null || tzids.length == 0) continue;
            // only use if there is a single element OR there is a singleCountrySet element
            if (tzids.length == 1) {
                country_zone.put(key, tzids[0]);
            } else {
                Set<String> set = new LinkedHashSet<String>(Arrays.asList(tzids)); // make modifyable
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
        result = pos < 0 ? zoneid : zoneid.substring(pos + 1);
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

    // The following are just for compatibility, until some fixes are made.

    public static final List<String> LENGTH = Arrays.asList(Length.SHORT.toString(), Length.LONG.toString());
    public static final int LENGTH_LIMIT = LENGTH.size();
    public static final int TYPE_LIMIT = Type.values().length;

    public String getFormattedZone(String zoneId, String pattern, boolean daylight, int offset, boolean b) {
        Format format = Format.valueOf(pattern);
        return getFormattedZone(zoneId, format.location, format.type, format.length, daylight, offset, null, false);
    }

    public String getFormattedZone(String zoneId, int length, int type, int offset, boolean b) {
        return getFormattedZone(zoneId, Location.LOCATION, Type.values()[type], Length.values()[length], false, offset,
            null, true);
    }

    public String getFormattedZone(String zoneId, String pattern, long time, boolean b) {
        return getFormattedZone(zoneId, pattern, time);
    }

    // end compat

}
