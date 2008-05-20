package org.unicode.cldr.util;

import org.unicode.cldr.icu.LDMLConstants;

public class PathUtilities {

  // ===== types of data and menu names
  public static final String LOCALEDISPLAYNAMES         = "//ldml/localeDisplayNames/";
  /**
   * All of the data items under LOCALEDISPLAYNAMES (menu items)
   */
  public static final String LOCALEDISPLAYNAMES_ITEMS[] = { LDMLConstants.LANGUAGES,
      LDMLConstants.SCRIPTS, LDMLConstants.TERRITORIES, LDMLConstants.VARIANTS, LDMLConstants.KEYS,
      LDMLConstants.TYPES, PathUtilities.CURRENCIES, PathUtilities.TIMEZONES,
      PathUtilities.CODEPATTERNS, PathUtilities.MEASNAMES };
  public static final String OTHER_CALENDARS_XPATH      = "//ldml/dates/calendars/calendar";

  public static final String LOCALEDISPLAYPATTERN_XPATH = LOCALEDISPLAYNAMES
                                                                + LDMLConstants.LOCALEDISPLAYPATTERN;
  public static final String NUMBERSCURRENCIES          = LDMLConstants.NUMBERS + "/"
                                                                + PathUtilities.CURRENCIES;
  public static final String CURRENCIES                 = "currencies";
  public static final String TIMEZONES                  = "timezones";
  public static final String METAZONES                  = "metazones";
  public static String       xOTHER                     = "misc";
  public static final String CODEPATTERNS               = "codePatterns";
  public static final String MEASNAMES                  = "measurementSystemNames";
  
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
      theMenu = METAZONES;
    } else if (path.startsWith("//ldml/" + LDMLConstants.CHARACTERS)) {
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

    String defaultCalendarsItems = "gregorian buddhist coptic ethiopic chinese hebrew indian islamic islamic-civil japanese persian roc";
    return (defaultCalendarsItems.split(" "));

    // }
  }


}
