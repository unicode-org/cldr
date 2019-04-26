package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map;

import org.unicode.cldr.icu.LDMLConstants;

/**
 * @deprecated
 */
public class PathUtilities {

    // ===== types of data and menu names
    public static final String LOCALEDISPLAYNAMES = "//ldml/localeDisplayNames/";
    /**
     * All of the data items under LOCALEDISPLAYNAMES (menu items)
     */
    public static final String LOCALEDISPLAYNAMES_ITEMS[] = { LDMLConstants.LANGUAGES,
        LDMLConstants.SCRIPTS, LDMLConstants.TERRITORIES, LDMLConstants.VARIANTS, LDMLConstants.KEYS,
        LDMLConstants.TYPES, PathUtilities.CURRENCIES, PathUtilities.TIMEZONES,
        PathUtilities.CODEPATTERNS, PathUtilities.MEASNAMES };
    public static final String OTHER_CALENDARS_XPATH = "//ldml/dates/calendars/calendar";

    public static final String LOCALEDISPLAYPATTERN_XPATH = LOCALEDISPLAYNAMES
        + LDMLConstants.LOCALEDISPLAYPATTERN;
    public static final String NUMBERSCURRENCIES = LDMLConstants.NUMBERS + "/"
        + PathUtilities.CURRENCIES;
    public static final String CURRENCIES = "currencies";
    public static final String TIMEZONES = "timezones";
    public static final String METAZONES = "metazones";
    public static String xOTHER = "misc";
    public static final String CODEPATTERNS = "codePatterns";
    public static final String MEASNAMES = "measurementSystemNames";

    public static String xpathToMenu(String path) {
        String theMenu = null;
        if (path.startsWith(LOCALEDISPLAYNAMES)) {
            for (int i = 0; i < LOCALEDISPLAYNAMES_ITEMS.length; i++) {
                if (path.startsWith(LOCALEDISPLAYNAMES
                    + LOCALEDISPLAYNAMES_ITEMS[i])) {
                    theMenu = LOCALEDISPLAYNAMES_ITEMS[i];
                }
            }
            if (path.startsWith(LOCALEDISPLAYPATTERN_XPATH)) {
                theMenu = LDMLConstants.LOCALEDISPLAYPATTERN;
            }
            // } else if(path.startsWith(GREGO_XPATH)) {
            // theMenu=GREGORIAN_CALENDAR;
        } else if (path.startsWith(OTHER_CALENDARS_XPATH)) {
            String items[] = getCalendarsItems();
            for (String which : items) {
                String CAL_XPATH = "//ldml/dates/calendars/calendar[@type=\"" + which + "\"]";
                if (path.startsWith(CAL_XPATH)) {
                    theMenu = which;
                    break;
                }
            }
        } else if (path.startsWith(LOCALEDISPLAYPATTERN_XPATH)) {
            theMenu = LDMLConstants.LOCALEDISPLAYPATTERN;
        } else if (path.startsWith("//ldml/" + NUMBERSCURRENCIES)) {
            theMenu = CURRENCIES;
        } else if (path.startsWith("//ldml/" + "dates/timeZoneNames/zone")) {
            theMenu = TIMEZONES;
        } else if (path.startsWith("//ldml/" + "units")) {
            theMenu = "units";
        } else if (path.startsWith("//ldml/" + "dates/timeZoneNames/metazone")) {
            theMenu = getMetazoneContinent(path);
            if (theMenu == null) {
                theMenu = METAZONES;
            }
        } else if (path.startsWith("//ldml/" + LDMLConstants.CHARACTERS + "/" + LDMLConstants.EXEMPLAR_CHARACTERS)) {
            theMenu = LDMLConstants.CHARACTERS;
        } else if (path.startsWith("//ldml/" + LDMLConstants.NUMBERS)) {
            theMenu = LDMLConstants.NUMBERS;
        } else if (path.startsWith("//ldml/" + LDMLConstants.REFERENCES)) {
            theMenu = LDMLConstants.REFERENCES;
        } else {
            theMenu = xOTHER;
            // other?
        }
        return theMenu;
    }

    public static String[] getCalendarsItems() {
        // TODO : Make this data driven from supplementalMetaData ;
        // I couldn't get the xpath right....
        // CLDRFile mySupp = getFactory().make("supplementalMetaData",false);
        // String xpath =
        // "//supplementalData/metadata/validity/variable[@id=\"$calendar\"][@type=\"choice\"]";
        // String items = mySupp.getStringValue(xpath);
        // if ( items != null ) {
        // return (items.split(" "));
        // }
        // else {

        String defaultCalendarsItems = "gregorian buddhist coptic ethiopic chinese hebrew indian islamic japanese persian roc";
        return (defaultCalendarsItems.split(" "));

        // }
    }

    public static String[] getMetazonesItems() {
        String defaultMetazonesItems = "Africa America Antarctica Asia Australia Europe Atlantic Indian Pacific";
        return (defaultMetazonesItems.split(" "));
    }

    private static Map<String, String> mzXpathToContinent = new HashMap<String, String>();

    private synchronized static String getMetazoneContinent(String xpath) {
        String continent = mzXpathToContinent.get(xpath);
        if (continent == null) {
            XPathParts parts = XPathParts.getTestInstance(xpath);
            String thisMetazone = parts.getAttributeValue(3, "type");
            continent = getMetazoneToContinentMap().get(thisMetazone);
        }
        return continent;
    }

    static Map<String, String> mzToContinentMap = null;

    private static Map<String, String> getMetazoneToContinentMap() {
        if (mzToContinentMap == null) {
            System.err
                .println(
                    "PathUtilities.java getMetazoneToContinentMap(): TODO: Get this data from supplemental data! http://unicode.org/cldr/trac/ticket/3761");
            HashMap<String, String> newMap = new HashMap<String, String>();
            for (int i = 0; i < mzToContinentStatic.length; i += 2) {
                newMap.put(mzToContinentStatic[i + 0], mzToContinentStatic[i + 1]);
            }
            mzToContinentMap = newMap;
        }
        return mzToContinentMap;
    }

    private static final String mzToContinentStatic[] = {

        "Philippines", "Asia",
        "Gambier", "Pacific",
        "Ecuador", "America",
        "Kuybyshev", "Europe",
        "Europe_Western", "Atlantic",
        "Chile", "America",
        "Afghanistan", "Asia",
        "Pierre_Miquelon", "America",
        "Solomon", "Pacific",
        "Arabian", "Asia",
        "Krasnoyarsk", "Asia",
        "Vladivostok", "Asia",
        "Fiji", "Pacific",
        "Niue", "Pacific",
        "Marquesas", "Pacific",
        "Karachi", "Asia",
        "Aqtobe", "Asia",
        "Irish", "Europe",
        "Yakutsk", "Asia",
        "Galapagos", "Pacific",
        "Bangladesh", "Asia",
        "America_Pacific", "America",
        "Urumqi", "Asia",
        "Tahiti", "Pacific",
        "Samoa", "Pacific",
        "Uzbekistan", "Asia",
        "Turkey", "Europe",
        "Kyrgystan", "Asia",
        "Europe_Eastern", "Europe",
        "Casey", "Antarctica",
        "Lord_Howe", "Australia",
        "Kizilorda", "Asia",
        "Kashgar", "Asia",
        "Africa_Western", "Africa",
        "Macquarie", "Antarctica",
        "Wake", "Pacific",
        "Australia_Eastern", "Australia",
        "Guyana", "America",
        "Taipei", "Asia",
        "Samarkand", "Asia",
        "Mawson", "Antarctica",
        "Africa_Eastern", "Africa",
        "Guam", "Pacific",
        "Kazakhstan_Western", "Asia",
        "Aqtau", "Asia",
        "Cook", "Pacific",
        "Wallis", "Pacific",
        "Irkutsk", "Asia",
        "Africa_Southern", "Africa",
        "French_Guiana", "America",
        "Chatham", "Pacific",
        "Oral", "Asia",
        "Noronha", "America",
        "Paraguay", "America",
        "Moscow", "Europe",
        "Hong_Kong", "Asia",
        "Yerevan", "Asia",
        "Vostok", "Antarctica",
        "Rothera", "Antarctica",
        "Colombia", "America",
        "Newfoundland", "America",
        "Hawaii_Aleutian", "Pacific",
        "East_Timor", "Asia",
        "GMT", "Atlantic",
        "Indian_Ocean", "Indian",
        "Reunion", "Indian",
        "Vanuatu", "Pacific",
        "Malaysia", "Asia",
        "Kwajalein", "Pacific",
        "Line_Islands", "Pacific",
        "Shevchenko", "Asia",
        "Azores", "Atlantic",
        "Frunze", "Asia",
        "Greenland_Eastern", "America",
        "Hovd", "Asia",
        "Lanka", "Asia",
        "Almaty", "Asia",
        "Macau", "Asia",
        "Mongolia", "Asia",
        "Easter", "Pacific",
        "British", "Europe",
        "Korea", "Asia",
        "Papua_New_Guinea", "Pacific",
        "Bering", "America",
        "Cocos", "Indian",
        "Mauritius", "Indian",
        "Argentina", "America",
        "Tokelau", "Pacific",
        "America_Central", "America",
        "Alaska", "America",
        "Georgia", "Asia",
        "Choibalsan", "Asia",
        "Sakhalin", "Asia",
        "Anadyr", "Asia",
        "Dushanbe", "Asia",
        "Indonesia_Eastern", "Asia",
        "Japan", "Asia",
        "Omsk", "Asia",
        "Nauru", "Pacific",
        "Cuba", "America",
        "Iran", "Asia",
        "Sverdlovsk", "Asia",
        "Maldives", "Indian",
        "Europe_Central", "Europe",
        "Kamchatka", "Asia",
        "Tajikistan", "Asia",
        "Pitcairn", "Pacific",
        "Gilbert_Islands", "Pacific",
        "Novosibirsk", "Asia",
        "Brunei", "Asia",
        "Tonga", "Pacific",
        "Changbai", "Asia",
        "India", "Asia",
        "Indonesia_Western", "Asia",
        "Malaya", "Asia",
        "Dacca", "Asia",
        "Tashkent", "Asia",
        "New_Zealand", "Pacific",
        "Indonesia_Central", "Asia",
        "Myanmar", "Asia",
        "South_Georgia", "Atlantic",
        "Truk", "Pacific",
        "Pakistan", "Asia",
        "Borneo", "Asia",
        "DumontDUrville", "Antarctica",
        "Argentina_Western", "America",
        "Uruguay", "America",
        "Dutch_Guiana", "America",
        "Ponape", "Pacific",
        "Gulf", "Asia",
        "Aktyubinsk", "Asia",
        "America_Mountain", "America",
        "Dominican", "America",
        "North_Mariana", "Pacific",
        "Yukon", "America",
        "Armenia", "Asia",
        "Falkland", "Atlantic",
        "Tbilisi", "Asia",
        "Baku", "Asia",
        "Venezuela", "America",
        "Ashkhabad", "Asia",
        "Cape_Verde", "Atlantic",
        "Phoenix_Islands", "Pacific",
        "Brasilia", "America",
        "Marshall_Islands", "Pacific",
        "Volgograd", "Europe",
        "Yekaterinburg", "Asia",
        "Kosrae", "Pacific",
        "Tuvalu", "Pacific",
        "Africa_Central", "Africa",
        "Palau", "Pacific",
        "Alaska_Hawaii", "America",
        "Qyzylorda", "Asia",
        "Bhutan", "Asia",
        "Israel", "Asia",
        "America_Eastern", "America",
        "Nepal", "Asia",
        "Azerbaijan", "Asia",
        "Uralsk", "Asia",
        "Bolivia", "America",
        "Liberia", "Africa",
        "Turkmenistan", "Asia",
        "Davis", "Antarctica",
        "Norfolk", "Pacific",
        "Indochina", "Asia",
        "Peru", "America",
        "Acre", "America",
        "China", "Asia",
        "Chamorro", "Pacific",
        "Atlantic", "America",
        "Syowa", "Antarctica",
        "Africa_FarWestern", "Africa",
        "New_Caledonia", "Pacific",
        "Greenland_Western", "America",
        "Suriname", "America",
        "Seychelles", "Indian",
        "Christmas", "Indian",
        "Australia_CentralWestern", "Australia",
        "Greenland_Central", "America",
        "French_Southern", "Indian",
        "Australia_Central", "Australia",
        "Australia_Western", "Australia",
        "Magadan", "Asia",
        "Kazakhstan_Eastern", "Asia",
        "Goose_Bay", "America",
        "Singapore", "Asia",
        "Amazon", "America",
        "Long_Shu", "Asia",
        "Samara", "Europe",
    };

}
