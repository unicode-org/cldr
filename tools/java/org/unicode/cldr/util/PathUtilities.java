package org.unicode.cldr.util;

import org.unicode.cldr.icu.LDMLConstants;

public class PathUtilities {
  static final String LOCALEDISPLAYNAMES = "//ldml/localeDisplayNames/";
  static final String CURRENCIES = "currencies";
  static final String TIMEZONES = "timezones";
  static final String METAZONES = "metazones";
  static final String MEASNAMES = "measurementSystemNames";
  static final String MEASNAME = "measurementSystemName";
  static final String CODEPATTERNS = "codePatterns";
  static final String CODEPATTERN = "codePattern";
  public static final String NUMBERSCURRENCIES = LDMLConstants.NUMBERS + "/"+CURRENCIES;
  public static final String CURRENCYTYPE = "//ldml/"+NUMBERSCURRENCIES+"/currency[@type='";
  /**
   *  All of the data items under LOCALEDISPLAYNAMES (menu items)
   */
  static final String LOCALEDISPLAYNAMES_ITEMS[] = { 
    LDMLConstants.LANGUAGES, LDMLConstants.SCRIPTS, LDMLConstants.TERRITORIES,
    LDMLConstants.VARIANTS, LDMLConstants.KEYS, LDMLConstants.TYPES,
    CURRENCIES,
    TIMEZONES,
    METAZONES,
    CODEPATTERNS,
    MEASNAMES
  };
  public static String xMAIN = "general";
  public static String xOTHER = "misc";
  public static String xREMOVE = "REMOVE";
  
  public static final String GREGORIAN_CALENDAR = "gregorian calendar";
  public static final String OTHER_CALENDARS = "other calendars";
  // 
  public static final String OTHERROOTS_ITEMS[] = {
    LDMLConstants.CHARACTERS,
    LDMLConstants.NUMBERS,
    GREGORIAN_CALENDAR,
    OTHER_CALENDARS,
    "references",
    xOTHER
  };
  public static final String GREGO_XPATH = "//ldml/dates/"+LDMLConstants.CALENDARS+"/"+LDMLConstants.CALENDAR+"[@type=\"gregorian\"]";
  public static final String OTHER_CALENDARS_XPATH = "//ldml/dates/calendars/calendar";
  
  public static String xpathToMenu(String path) {                    
    String theMenu=null;
    if(path.startsWith(LOCALEDISPLAYNAMES)) {
      for(int i=0;i<LOCALEDISPLAYNAMES_ITEMS.length;i++) {
        if(path.startsWith(LOCALEDISPLAYNAMES+LOCALEDISPLAYNAMES_ITEMS[i])) {
          theMenu=LOCALEDISPLAYNAMES_ITEMS[i];
        }
      }
    } else if(path.startsWith(GREGO_XPATH)) {
      theMenu=GREGORIAN_CALENDAR;
    } else if(path.startsWith(OTHER_CALENDARS_XPATH)) {
      theMenu=OTHER_CALENDARS;
    } else if(path.startsWith("//ldml/"+NUMBERSCURRENCIES)) {
      theMenu=CURRENCIES;
    } else if(path.startsWith( "//ldml/"+"dates/timeZoneNames/zone")){
      theMenu=TIMEZONES;
    } else if(path.startsWith( "//ldml/"+"dates/timeZoneNames/metazone")){
      theMenu=METAZONES;
    } else if(path.startsWith( "//ldml/"+LDMLConstants.CHARACTERS)) {
      theMenu = LDMLConstants.CHARACTERS;
    } else if(path.startsWith( "//ldml/"+LDMLConstants.NUMBERS)) {
      theMenu = LDMLConstants.NUMBERS;
    } else if(path.startsWith( "//ldml/"+LDMLConstants.REFERENCES)) {
      theMenu = LDMLConstants.REFERENCES;
    } else {
      theMenu=xOTHER;
      // other?
    }
    return theMenu;
  }
}
