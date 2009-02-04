package org.unicode.cldr.util;

import com.ibm.icu.dev.test.util.XEquivalenceClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZoneParser {
  static final boolean DEBUG = false;

  private String version;
  
  private Map zone_to_country;

  private Map country_to_zoneSet;

  /**
   * @return mapping from zone id to country. If a zone has no country, then XX
   *         is used.
   */
  public Map getZoneToCounty() {
    if (zone_to_country == null)
      make_zone_to_country();
    return zone_to_country;
  }

  /**
   * @return mapping from country to zoneid. If a zone has no country, then XX
   *         is used.
   */
  public Map getCountryToZoneSet() {
    if (country_to_zoneSet == null)
      make_zone_to_country();
    return country_to_zoneSet;
  }

  /**
   * @return map from tzids to a list: latitude, longitude, country, comment?. + =
   *         N or E
   */
  public Map getZoneData() {
    if (zoneData == null)
      makeZoneData();
    return zoneData;
  }

  public List getDeprecatedZoneIDs() {
    return Arrays.asList(FIX_DEPRECATED_ZONE_DATA);
  }

  /**
   * 
   */
  private void make_zone_to_country() {
    zone_to_country = new TreeMap(TZIDComparator);
    country_to_zoneSet = new TreeMap();
    // Map aliasMap = getAliasMap();
    Map zoneData = getZoneData();
    for (Iterator it = zoneData.keySet().iterator(); it.hasNext();) {
      String zone = (String) it.next();
      String country = (String) ((List) zoneData.get(zone)).get(2);
      zone_to_country.put(zone, country);
      Set s = (Set) country_to_zoneSet.get(country);
      if (s == null)
        country_to_zoneSet.put(country, s = new TreeSet());
      s.add(zone);
    }
    /*
     * Set territories = getAvailableCodes("territory"); for (Iterator it =
     * territories.iterator(); it.hasNext();) { String code = (String)
     * it.next(); String[] zones = TimeZone.getAvailableIDs(code); for (int i =
     * 0; i < zones.length; ++i) { if (aliasMap.get(zones[i]) != null) continue;
     * zone_to_country.put(zones[i], code); } } String[] zones =
     * TimeZone.getAvailableIDs(); for (int i = 0; i < zones.length; ++i) { if
     * (aliasMap.get(zones[i]) != null) continue; if
     * (zone_to_country.get(zones[i]) == null) { zone_to_country.put(zones[i],
     * NO_COUNTRY); } } for (Iterator it = zone_to_country.keySet().iterator();
     * it.hasNext();) { String tzid = (String) it.next(); String country =
     * (String) zone_to_country.get(tzid); Set s = (Set)
     * country_to_zoneSet.get(country); if (s == null)
     * country_to_zoneSet.put(country, s = new TreeSet()); s.add(tzid); }
     */
    // protect
    zone_to_country = Collections.unmodifiableMap(zone_to_country);
    country_to_zoneSet = (Map) Utility.protectCollection(country_to_zoneSet);
  }

  /**
   * 
   * 
   * private Map bogusZones = null;
   * 
   * private Map getAliasMap() { if (bogusZones == null) { try { bogusZones =
   * new TreeMap(); BufferedReader in =
   * Utility.getUTF8Data"TimeZoneAliases.txt"); while (true) { String line =
   * in.readLine(); if (line == null) break; line = line.trim(); int pos =
   * line.indexOf('#'); if (pos >= 0) { skippedAliases.add(line); line =
   * line.substring(0,pos).trim(); } if (line.length() == 0) continue; List
   * pieces = Utility.splitList(line,';', true); bogusZones.put(pieces.get(0),
   * pieces.get(1)); } in.close(); } catch (IOException e) { throw new
   * IllegalArgumentException("Can't find timezone aliases"); } } return
   * bogusZones; }
   */

  Map zoneData;

  Set skippedAliases = new TreeSet();

  /*
   * # This file contains a table with the following columns: # 1. ISO 3166
   * 2-character country code. See the file `iso3166.tab'. # 2. Latitude and
   * longitude of the zone's principal location # in ISO 6709
   * sign-degrees-minutes-seconds format, # either +-DDMM+-DDDMM or
   * +-DDMMSS+-DDDMMSS, # first latitude (+ is north), then longitude (+ is
   * east). # 3. Zone name used in value of TZ environment variable. # 4.
   * Comments; present if and only if the country has multiple rows. # # Columns
   * are separated by a single tab.
   * 
   */
  static int parseYear(Object object, int defaultValue) {
    String year = (String) object;
    if ("only".startsWith(year))
      return defaultValue;
    if ("minimum".startsWith(year))
      return Integer.MIN_VALUE;
    if ("maximum".startsWith(year))
      return Integer.MAX_VALUE;
    return Integer.parseInt(year);
  }
  
  
  public static class Time {
    public int seconds;
    public byte type;
    static final byte WALL = 0, STANDARD = 1, UNIVERSAL = 2;
    
    Time(String in) {
      if (in.equals("-")) return; // zero/WALL is the default
      char suffix = in.charAt(in.length()-1);
      switch(suffix) {
        case 'w': in = in.substring(0,in.length()-1); break;
        case 's': in = in.substring(0,in.length()-1); type = STANDARD; break;
        case 'u': case 'g': case 'z': 
          in = in.substring(0,in.length()-1); type = UNIVERSAL; break;
      }
      seconds = parseSeconds(in, false);
    }
    
    public static  int parseSeconds(String in, boolean allowNegative) {
      boolean negative = false;
      if (in.startsWith("-")) {
        assert(allowNegative);
        negative = true;
        in = in.substring(1);
      }
      String[] pieces = in.split(":");
      int multiplier = 3600;
      int result = 0;
      for (int i = 0; i < pieces.length; ++i) {
        result += multiplier * Integer.parseInt(pieces[i]);
        multiplier /= 60;
        assert(multiplier >= 0);
      }
      if (negative) result = -result;
      return result;
    }
    public String toString() {
      return BoilerplateUtilities.toStringHelper(this);
    }
  }
  static final String[] months = {"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};
  static final String[] weekdays = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
  
  static int findStartsWith(String value, String[] array, boolean exact) {
    value = value.toLowerCase(Locale.ENGLISH);
    for (int i = 0; i < array.length; ++i) {
      if (array[i].startsWith(value)) return i;
    }
    throw new IllegalArgumentException("Can't find " + value + " in " + Arrays.asList(months));
  }
  
  static Pattern dayPattern = Pattern.compile("([0-9]+)|(last)([a-z]+)|([a-z]+)([<=>]+)([0-9]+)");
  static final String[] relations = {"<=", ">="};
  
  public static class Day implements Comparable {
    public int number;
    public byte relation;
    public int weekDay;
    static final byte NONE = 0, LEQ = 2, GEQ = 4;
    
    Day(String value) {
      value = value.toLowerCase();
      Matcher matcher = dayPattern.matcher(value);
      if (!matcher.matches()) {
        throw new IllegalArgumentException();
      }
      if (matcher.group(1) != null) {
        number = Integer.parseInt(matcher.group(1));
        return;
      }
      if (matcher.group(2) != null) {
        weekDay = findStartsWith(matcher.group(3), weekdays, false);
        number = 31;
        relation = LEQ;
        return;
      }
      if (matcher.group(4) != null) {
        weekDay = findStartsWith(matcher.group(4), weekdays, false);
        relation = (byte)findStartsWith(matcher.group(5), relations, false);
        number = Integer.parseInt(matcher.group(6));
        return;
      }
      throw new IllegalArgumentException();
    }
    public String toString() {
      return BoilerplateUtilities.toStringHelper(this);
    }
    public int compareTo(Object other) {
      return toString().compareTo(other.toString());
    }
  }


  /**
   * 
   A rule line has the form

   Rule  NAME  FROM  TO    TYPE  IN   ON       AT    SAVE  LETTER/S

   For example:

   Rule  US    1967  1973  -     Apr  lastSun  2:00  1:00  D

   The fields that make up a rule line are:

   NAME    Gives the (arbitrary) name of the set of rules this
   rule is part of.

   FROM    Gives the first year in which the rule applies.  Any
   integer year can be supplied; the Gregorian calendar
   is assumed.  The word minimum (or an abbreviation)
   means the minimum year representable as an integer.
   The word maximum (or an abbreviation) means the
   maximum year representable as an integer.  Rules can
   describe times that are not representable as time
   values, with the unrepresentable times ignored; this
   allows rules to be portable among hosts with
   differing time value types.

   TO      Gives the final year in which the rule applies.  In
   addition to minimum and maximum (as above), the word
   only (or an abbreviation) may be used to repeat the
   value of the FROM field.

   TYPE    Gives the type of year in which the rule applies.
   If TYPE is - then the rule applies in all years
   between FROM and TO inclusive.  If TYPE is something
   else, then zic executes the command
   yearistype year type
   to check the type of a year:  an exit status of zero
   is taken to mean that the year is of the given type;
   an exit status of one is taken to mean that the year
   is not of the given type.

   IN      Names the month in which the rule takes effect.
   Month names may be abbreviated.

   ON      Gives the day on which the rule takes effect.
   Recognized forms include:

   5        the fifth of the month
   lastSun  the last Sunday in the month
   lastMon  the last Monday in the month
   Sun>=8   first Sunday on or after the eighth
   Sun<=25  last Sunday on or before the 25th

   Names of days of the week may be abbreviated or
   spelled out in full.  Note that there must be no
   spaces within the ON field.

   AT      Gives the time of day at which the rule takes
   effect.  Recognized forms include:

   2        time in hours
   2:00     time in hours and minutes
   15:00    24-hour format time (for times after noon)
   1:28:14  time in hours, minutes, and seconds
   -        equivalent to 0

   where hour 0 is midnight at the start of the day,
   and hour 24 is midnight at the end of the day.  Any
   of these forms may be followed by the letter w if
   the given time is local "wall clock" time, s if the
   given time is local "standard" time, or u (or g or
   z) if the given time is universal time; in the
   absence of an indicator, wall clock time is assumed.
   *** cannot be negative

   SAVE    Gives the amount of time to be added to local
   standard time when the rule is in effect.  This
   field has the same format as the AT field (although,
   of course, the w and s suffixes are not used).
   *** can be positive or negative

   LETTER/S
   Gives the "variable part" (for example, the "S" or
   "D" in "EST" or "EDT") of time zone abbreviations to
   be used when this rule is in effect.  If this field
   is -, the variable part is null.


   *
   */

  
  public static class RuleLine {
    public static Set types = new TreeSet();
    public static Set days = new TreeSet();
    static Set saves = new TreeSet();
    RuleLine(List l) {
      fromYear = parseYear(l.get(0), 0);
      toYear = parseYear(l.get(1), fromYear);
      type = (String) l.get(2);
      if (type.equals("-")) type = null;
      month = 1 + findStartsWith((String) l.get(3), months, false);
      day = new Day((String)l.get(4));
      time = new Time((String)l.get(5));
      save = Time.parseSeconds((String)l.get(6), true);
      letter = (String) l.get(7);
      if (letter.equals("-")) letter = null;
      if (type != null) types.add(type);
      days.add(day);
    }

    public String toString() {
      return BoilerplateUtilities.toStringHelper(this);
    }

    public int fromYear;

    public int toYear;

    public String type;

    public int month;

    public Day day;

    public Time time;

    public int save;

    public String letter;

    public static final int FIELD_COUNT = 8; // excluding Rule, Name
  }

  /**
   A zone line has the form

   Zone  NAME                GMTOFF  RULES/SAVE  FORMAT  [UNTIL]

   For example:

   Zone  Australia/Adelaide  9:30    Aus         CST     1971 Oct 31 2:00

   The fields that make up a zone line are:

   NAME  The name of the time zone.  This is the name used in
   creating the time conversion information file for the
   zone.

   GMTOFF
   The amount of time to add to UTC to get standard time
   in this zone.  This field has the same format as the
   AT and SAVE fields of rule lines; begin the field with
   a minus sign if time must be subtracted from UTC.

   RULES/SAVE
   The name of the rule(s) that apply in the time zone
   or, alternately, an amount of time to add to local
   standard time.  If this field is - then standard time
   always applies in the time zone.

   FORMAT
   The format for time zone abbreviations in this time
   zone.  The pair of characters %s is used to show where
   the "variable part" of the time zone abbreviation
   goes.  Alternately, a slash (/) separates standard and
   daylight abbreviations.

   UNTIL The time at which the UTC offset or the rule(s) change
   for a location.  It is specified as a year, a month, a
   day, and a time of day.  If this is specified, the
   time zone information is generated from the given UTC
   offset and rule change until the time specified.  The
   month, day, and time of day have the same format as
   the IN, ON, and AT columns of a rule; trailing columns
   can be omitted, and default to the earliest possible
   value for the missing columns.

   The next line must be a "continuation" line; this has
   the same form as a zone line except that the string
   "Zone" and the name are omitted, as the continuation
   line will place information starting at the time
   specified as the UNTIL field in the previous line in
   the file used by the previous line.  Continuation
   lines may contain an UNTIL field, just as zone lines
   do, indicating that the next line is a further
   continuation.
   */
  public static class ZoneLine {
    public static Set untilDays = new TreeSet();
    public static Set rulesSaves = new TreeSet();
    ZoneLine(List l) {
      gmtOff = Time.parseSeconds((String) l.get(0), true);
      rulesSave = (String) l.get(1);
      if (rulesSave.equals("-")) rulesSave = "0";
      else if (rulesSave.charAt(0) < 'A') rulesSave = "" + Time.parseSeconds(rulesSave, false);

      format = (String) l.get(2);
      switch (l.size()) {
        case 7:
          untilTime = new Time((String)l.get(6)); // fall through
        case 6:
          untilDay = new Day((String)l.get(5)); // fall through
          untilDays.add(untilDay);
        case 5:
          untilMonth = 1+findStartsWith((String)l.get(4), months, false); // fall through
        case 4:
          untilYear = parseYear(l.get(3), Integer.MAX_VALUE); // fall through
        case 3:
          break; // ok
        default:
          throw new IllegalArgumentException("Wrong field count: " + l);
      }
      rulesSaves.add(rulesSave);
    }

    public String toString() {
      return BoilerplateUtilities.toStringHelper(this);
    }

    public int gmtOff;

    public String rulesSave;

    public String format;

    public int untilYear = Integer.MAX_VALUE; // indicating continuation

    public int untilMonth; 

    public Day untilDay;

    public Time untilTime;

    public String comment;

    public static final int FIELD_COUNT = 3; // excluding Zone, Name

    public static final int FIELD_COUNT_UNTIL = 7; // excluding Zone, Name
  }

  Map ruleID_rules = new TreeMap();

  Map zone_rules = new TreeMap();

  Map linkold_new = new TreeMap();

  Map linkNew_oldSet = new TreeMap();
  
  public class Transition {
    public long date;
    public long offset;
    public String abbreviation;
  }
  
  public class TransitionList {
    List transitions;
    void addTransitions(ZoneLine lastZoneLine, ZoneLine zoneLine, int startYear, int endYear) {
      // add everything between the zonelines
      if (lastZoneLine == null) return;
      startYear = Math.max(startYear, lastZoneLine.untilYear);
      endYear = Math.min(endYear, zoneLine.untilYear);
      int gmtOffset = lastZoneLine.gmtOff;
      for (int year = startYear; year <= endYear; ++year) {
        long startTime = resolveTime(gmtOffset, lastZoneLine.untilYear, lastZoneLine.untilMonth, lastZoneLine.untilDay, lastZoneLine.untilTime);
      }
    }
    private long resolveTime(int gmtOffset, int untilYear, int untilMonth, Day untilDay, Time untilTime) {
      // TODO Auto-generated method stub
      return 0;
    }
  }
  
  public TransitionList getTransitions(String zoneID, int startYear, int endYear) {
    TransitionList results = new TransitionList();
    List rules = (List)zone_rules.get(zoneID);
    ZoneLine lastZoneLine = null;
    for (Iterator it = rules.iterator(); it.hasNext();) {
      ZoneLine zoneLine = (ZoneLine) it.next();
      results.addTransitions(lastZoneLine, zoneLine, startYear, endYear);
      lastZoneLine = zoneLine;
    }
    return results;
  }

  public Comparator getTZIDComparator() {
    return TZIDComparator;
  }

  private static List errorData = Arrays.asList(new Object[] {
      new Double(Double.MIN_VALUE), new Double(Double.MIN_VALUE), "" });

  private Comparator TZIDComparator = new Comparator() {
    Map data = getZoneData();

    public int compare(Object o1, Object o2) {
      String s1 = (String) o1;
      String s2 = (String) o2;
      // String ss1 = s1.substring(0,s1.indexOf('/'));
      // String ss2 = s2.substring(0,s2.indexOf('/'));
      // if (!ss1.equals(ss2)) return regionalCompare.compare(ss1, ss2);
      List data1 = (List) data.get(s1);
      if (data1 == null)
        data1 = errorData;
      List data2 = (List) data.get(s2);
      if (data2 == null)
        data2 = errorData;

      int result;
      // country
      String country1 = (String) data1.get(2);
      String country2 = (String) data2.get(2);

      if ((result = country1.compareTo(country2)) != 0)
        return result;
      // longitude
      Double d1 = (Double) data1.get(1);
      Double d2 = (Double) data2.get(1);
      if ((result = d1.compareTo(d2)) != 0)
        return result;
      // latitude
      d1 = (Double) data1.get(0);
      d2 = (Double) data2.get(0);
      if ((result = d1.compareTo(d2)) != 0)
        return result;
      // name
      return s1.compareTo(s2);
    }
  };

  private static MapComparator regionalCompare = new MapComparator();
  static {
    regionalCompare.add("America");
    regionalCompare.add("Atlantic");
    regionalCompare.add("Europe");
    regionalCompare.add("Africa");
    regionalCompare.add("Asia");
    regionalCompare.add("Indian");
    regionalCompare.add("Australia");
    regionalCompare.add("Pacific");
    regionalCompare.add("Arctic");
    regionalCompare.add("Antarctica");
    regionalCompare.add("Etc");
  }

  private static String[] TZFiles = { "africa", "antarctica", "asia",
      "australasia", "backward", "etcetera", "europe", "northamerica",
      "pacificnew", "southamerica", "systemv" };

  private static Map FIX_UNSTABLE_TZIDS;

  private static Map RESTORE_UNSTABLE_TZIDS;

  private static Set SKIP_LINKS = new HashSet(Arrays.asList(new String[] {
      "Navajo", "America/Shiprock" }));
  
  private static Set PREFERRED_BASES = new HashSet(Arrays.asList(new String[] {
      "Europe/London"
  }));

  private static String[][] ADD_ZONE_ALIASES_DATA = { { "Etc/UTC", "Etc/GMT" },
      { "Etc/UCT", "Etc/GMT" }, 
      { "Navajo", "America/Shiprock" },
      // extras added in 2006g
      { "SystemV/AST4ADT", "America/Halifax" },
      { "SystemV/EST5EDT", "America/New_York" },
      { "EST5EDT", "America/New_York" },
      { "SystemV/CST6CDT", "America/Chicago" },
      { "CST6CDT", "America/Chicago" },
      { "SystemV/MST7MDT", "America/Denver" },
      { "MST7MDT", "America/Denver" },
      { "SystemV/PST8PDT", "America/Los_Angeles" },
      { "PST8PDT", "America/Los_Angeles" },
      { "SystemV/YST9YDT", "America/Anchorage" },
      
      { "SystemV/AST4", "Etc/GMT+4" },
      { "SystemV/EST5", "Etc/GMT+5" },
      { "EST", "Etc/GMT+5" },
      { "SystemV/CST6", "Etc/GMT+6" },
      { "SystemV/MST7", "Etc/GMT+7" },
      { "MST", "Etc/GMT+7" },
      { "SystemV/PST8", "Etc/GMT+8" },
      { "SystemV/YST9", "Etc/GMT+9" },
      { "SystemV/HST10", "Etc/GMT+10" },
      { "HST", "Etc/GMT+10" }, };
  
  /*
## Zone SystemV/AST4    -4:00   -       AST
## Zone SystemV/EST5    -5:00   -       EST
## Zone SystemV/CST6    -6:00   -       CST
## Zone SystemV/MST7    -7:00   -       MST
## Zone SystemV/PST8    -8:00   -       PST
## Zone SystemV/YST9    -9:00   -       YST
## Zone SystemV/HST10   -10:00  -       HST
   */

  static String[] FIX_DEPRECATED_ZONE_DATA = { 
      "Africa/Timbuktu",
      "America/Argentina/ComodRivadavia", 
      "Europe/Belfast", 
      "Pacific/Yap" };
  static {
    // The format is <new name>, <old name>
    String[][] FIX_UNSTABLE_TZID_DATA = new String[][] {
        { "America/Atikokan", "America/Coral_Harbour" },
        { "America/Argentina/Buenos_Aires", "America/Buenos_Aires" },
        { "America/Argentina/Catamarca", "America/Catamarca" },
        { "America/Argentina/Cordoba", "America/Cordoba" },
        { "America/Argentina/Jujuy", "America/Jujuy" },
        { "America/Argentina/Mendoza", "America/Mendoza" },
        { "America/Kentucky/Louisville", "America/Louisville" },
        { "America/Indiana/Indianapolis", "America/Indianapolis" },
        { "Africa/Asmara", "Africa/Asmera" },
        { "Atlantic/Faroe", "Atlantic/Faeroe" },
        { "Asia/Kolkata", "Asia/Calcutta" },
        { "Asia/Ho_Chi_Minh", "Asia/Saigon" },
        { "Asia/Kathmandu", "Asia/Katmandu" },
    };
    FIX_UNSTABLE_TZIDS = Utility.asMap(FIX_UNSTABLE_TZID_DATA);
    RESTORE_UNSTABLE_TZIDS = Utility.asMap(FIX_UNSTABLE_TZID_DATA,
        new HashMap(), true);
  }

  /**
   * 
   */
  private void makeZoneData() {
    try {
      // get version
      BufferedReader versionIn = Utility.getUTF8Data("tzdb-version.txt");
      version = versionIn.readLine();
      if (!version.matches("[0-9]{4}[a-z]")) {
        throw new IllegalArgumentException("Bad Version number: %s, should be of the form 2007x".format(version));
      }
      versionIn.close();
      
      // String deg = "([+-][0-9]+)";//
      String deg = "([+-])([0-9][0-9][0-9]?)([0-9][0-9])([0-9][0-9])?";//
      Matcher m = Pattern.compile(deg + deg).matcher("");
      zoneData = new TreeMap();
      BufferedReader in = Utility.getUTF8Data("zone.tab");
      while (true) {
        String line = in.readLine();
        if (line == null)
          break;
        line = line.trim();
        int pos = line.indexOf('#');
        if (pos >= 0) {
          skippedAliases.add(line);
          line = line.substring(0, pos).trim();
        }
        if (line.length() == 0)
          continue;
        List pieces = Utility.splitList(line, '\t', true);
        String country = (String) pieces.get(0);
        String latLong = (String) pieces.get(1);
        String tzid = (String) pieces.get(2);
        String ntzid = (String) FIX_UNSTABLE_TZIDS.get(tzid);
        if (ntzid != null)
          tzid = ntzid;
        String comment = pieces.size() < 4 ? null : (String) pieces.get(3);
        pieces.clear();
        if (!m.reset(latLong).matches())
          throw new IllegalArgumentException("Bad zone.tab, lat/long format: "
              + line);

        pieces.add(getDegrees(m, true));
        pieces.add(getDegrees(m, false));
        pieces.add(country);
        if (comment != null)
          pieces.add(comment);
        if (zoneData.containsKey(tzid))
          throw new IllegalArgumentException("Bad zone.tab, duplicate entry: "
              + line);
        zoneData.put(tzid, pieces);
      }
      in.close();
      // add Etcs
      for (int i = -14; i <= 12; ++i) {
        List pieces = new ArrayList();
        int latitude = 0;
        int longitude = i * 15;
        if (longitude <= -180) {
          longitude += 360;
        }
        pieces.add(new Double(latitude)); // lat
        // remember that the sign of the TZIDs is wrong
        pieces.add(new Double(-longitude)); // long
        pieces.add(StandardCodes.NO_COUNTRY); // country

        zoneData.put("Etc/GMT" + (i == 0 ? "" : i < 0 ? "" + i : "+" + i),
            pieces);
      }
      // add Unknown
      List pieces = new ArrayList();
      pieces.add(new Double(0)); // lat
      pieces.add(new Double(0)); // long
      pieces.add(StandardCodes.NO_COUNTRY); // country
      zoneData.put("Etc/Unknown", pieces);

      zoneData = (Map) Utility.protectCollection(zoneData); // protect for later

      // now get links
      Pattern whitespace = Pattern.compile("\\s+");
      XEquivalenceClass linkedItems = new XEquivalenceClass("None");
      for (int i = 0; i < TZFiles.length; ++i) {
        in = Utility.getUTF8Data(TZFiles[i]);
        String zoneID = null;
        while (true) {
          String line = in.readLine();
          if (line == null)
            break;
          String originalLine = line;
          int commentPos = line.indexOf("#");
          String comment = null;
          if (commentPos >= 0) {
            comment = line.substring(commentPos + 1).trim();
            line = line.substring(0, commentPos);
          }
          line = line.trim();
          if (line.length() == 0)
            continue;
          String[] items = whitespace.split(line);
          if (zoneID != null || items[0].equals("Zone")) {
            List l = new ArrayList();
            l.addAll(Arrays.asList(items));

            // Zone Africa/Algiers 0:12:12 - LMT 1891 Mar 15 0:01
            // 0:09:21 - PMT 1911 Mar 11 # Paris Mean Time
            if (zoneID == null) {
              l.remove(0); // "Zone"
              zoneID = (String) l.get(0);
              if (false) System.out.println("*Link:\t" + zoneID);
              String ntzid = (String) FIX_UNSTABLE_TZIDS.get(zoneID);
              if (ntzid != null)
                zoneID = ntzid;
              l.remove(0);
            }
            List zoneRules = (List) zone_rules.get(zoneID);
            if (zoneRules == null) {
              zoneRules = new ArrayList();
              zone_rules.put(zoneID, zoneRules);
            }
            if (l.size() < ZoneLine.FIELD_COUNT
                || l.size() > ZoneLine.FIELD_COUNT_UNTIL) {
              System.out.println("***Zone incorrect field count:");
              System.out.println(l);
              System.out.println(originalLine);
            }

            ZoneLine zoneLine = new ZoneLine(l);
            if (false && zoneLine.rulesSave.charAt(0) >= 'A' && ruleID_rules.get(zoneLine.rulesSave) == null) {
              System.out.println("*** Forward Reference to: " + zoneLine.rulesSave + " in " + zoneID);
            }
            zoneLine.comment = comment;
            if (false) {
              System.out
                  .println(zoneID + "\t" + zoneLine + "\t" + originalLine);
            }
            zoneRules.add(zoneLine);
            if (l.size() == ZoneLine.FIELD_COUNT) {
              zoneID = null; // no continuation line
            }
          } else if (items[0].equals("Rule")) {
            // # Rule NAME FROM TO TYPE IN ON AT SAVE LETTER/S
            // Rule Algeria 1916 only - Jun 14 23:00s 1:00 S

            String ruleID = items[1];
            List ruleList = (List) ruleID_rules.get(ruleID);
            if (ruleList == null) {
              ruleList = new ArrayList();
              ruleID_rules.put(ruleID, ruleList);
            }
            List l = new ArrayList();
            l.addAll(Arrays.asList(items));
            l.remove(0);
            l.remove(0);
            if (l.size() != RuleLine.FIELD_COUNT) {
              System.out.println("***Rule incorrect field count:");
              System.out.println(l);
            }
            if (comment != null)
              l.add(comment);
            RuleLine ruleLine = new RuleLine(l);
            ruleList.add(ruleLine);

          } else if (items[0].equals("Link")) {
            String old = items[2];
            String newOne = items[1];
            if (!SKIP_LINKS.contains(old) && !SKIP_LINKS.contains(newOne)) {
              // System.out.println("Original " + old + "\t=>\t" + newOne);
              linkedItems.add(old, newOne);
              if (false) System.out.println("*Link:\t" + old);
              if (false) System.out.println("*Link:\t" + newOne);
            }
            /*
             * String conflict = (String) linkold_new.get(old); if (conflict !=
             * null) { System.out.println("Conflict with old: " + old + " => " +
             * conflict + ", " + newOne); } System.out.println(old + "\t=>\t" +
             * newOne); linkold_new.put(old, newOne);
             */
          } else {
            if (DEBUG)
              System.out.println("Unknown zone line: " + line);
          }
        }
        in.close();
      }
      // add in stuff that should be links
      for (int i = 0; i < ADD_ZONE_ALIASES_DATA.length; ++i) {
        linkedItems.add(ADD_ZONE_ALIASES_DATA[i][0],
            ADD_ZONE_ALIASES_DATA[i][1]);
      }
      // linkedItems.add("Etc/UTC", "Etc/GMT");
      // linkedItems.add("Etc/UCT", "Etc/GMT");
      // linkedItems.add("Navajo", "America/Shiprock");

      Set isCanonical = zoneData.keySet();

      // walk through the sets, and
      // if any set contains two canonical items, split it.
      // if any contains one, make it the primary
      // if any contains zero, problem!
      for (Iterator it = linkedItems.getEquivalenceSets().iterator(); it
          .hasNext();) {
        Set equivalents = (Set) it.next();
        Set canonicals = new TreeSet(equivalents);
        canonicals.retainAll(isCanonical);
        if (canonicals.size() == 0)
          throw new IllegalArgumentException("No canonicals in: " + equivalents);
        if (false && canonicals.size() > 1) {
          if (DEBUG) {
            System.out.println("Too many canonicals in: " + equivalents);
            System.out
                .println("\t*Don't* put these into the same equivalence class: "
                    + canonicals);
          }
          Set remainder = new TreeSet(equivalents);
          remainder.removeAll(isCanonical);
          if (remainder.size() != 0) {
            if (DEBUG) {
              System.out
                  .println("\tThe following should be equivalent to others: "
                      + remainder);
            }
          }
        }
        {
          Object newOne;
          // get the item that we want to hang all the aliases off of.
          // normally this is the first (alphabetically) one, but
          // it may be overridden with PREFERRED_BASES
          Set preferredItems = new HashSet(PREFERRED_BASES);
          preferredItems.retainAll(canonicals);
          if (preferredItems.size() > 0) {
            newOne = preferredItems.iterator().next();
          } else {
            newOne = canonicals.iterator().next();
          }
          for (Iterator it2 = equivalents.iterator(); it2.hasNext();) {
            Object oldOne = it2.next();
            if (canonicals.contains(oldOne))
              continue;
            // System.out.println("Mapping " + oldOne + "\t=>\t" + newOne);
            linkold_new.put(oldOne, newOne);
          }
        }
      }

      /*
       * // fix the links from old to new, to remove chains for (Iterator it =
       * linkold_new.keySet().iterator(); it.hasNext();) { Object oldItem =
       * it.next(); Object newItem = linkold_new.get(oldItem); while (true) {
       * Object linkItem = linkold_new.get(newItem); if (linkItem == null)
       * break; if (true) System.out.println("Connecting link chain: " + oldItem +
       * "\t=> " + newItem + "\t=> " + linkItem); newItem = linkItem;
       * linkold_new.put(oldItem, newItem); } }
       *  // reverse the links *from* canonical names for (Iterator it =
       * linkold_new.keySet().iterator(); it.hasNext();) { Object oldItem =
       * it.next(); if (!isCanonical.contains(oldItem)) continue; Object newItem =
       * linkold_new.get(oldItem); }
       * 
       *  // fix unstable TZIDs Set itemsToRemove = new HashSet(); Map
       * itemsToAdd = new HashMap(); for (Iterator it =
       * linkold_new.keySet().iterator(); it.hasNext();) { Object oldItem =
       * it.next(); Object newItem = linkold_new.get(oldItem); Object modOldItem =
       * RESTORE_UNSTABLE_TZIDS.get(oldItem); Object modNewItem =
       * FIX_UNSTABLE_TZIDS.get(newItem); if (modOldItem == null && modNewItem ==
       * null) continue; if (modOldItem == null) { // just fix old entry
       * itemsToAdd.put(oldItem, modNewItem); continue; } // otherwise have to
       * nuke and redo itemsToRemove.add(oldItem); if (modNewItem == null)
       * modNewItem = newItem; itemsToAdd.put(modOldItem, modNewItem); } // now
       * make fixes (we couldn't earlier because we were iterating
       * Utility.removeAll(linkold_new, itemsToRemove);
       * linkold_new.putAll(itemsToAdd);
       *  // now remove all links that are from canonical zones
       * Utility.removeAll(linkold_new, zoneData.keySet());
       */

      // generate list of new to old
      for (Iterator it = linkold_new.keySet().iterator(); it.hasNext();) {
        String oldZone = (String) it.next();
        String newZone = (String) linkold_new.get(oldZone);
        Set s = (Set) linkNew_oldSet.get(newZone);
        if (s == null)
          linkNew_oldSet.put(newZone, s = new HashSet());
        s.add(oldZone);
      }

      // PROTECT EVERYTHING
      linkNew_oldSet = (Map) Utility.protectCollection(linkNew_oldSet);
      linkold_new = (Map) Utility.protectCollection(linkold_new);
      ruleID_rules = (Map) Utility.protectCollection(ruleID_rules);
      zone_rules = (Map) Utility.protectCollection(zone_rules);
      // TODO protect zone info later
    } catch (IOException e) {
      throw (IllegalArgumentException) new IllegalArgumentException(
          "Can't find timezone aliases: " + e.toString()).initCause(e);
    }
  }

  /**
   * @param m
   */
  private Double getDegrees(Matcher m, boolean lat) {
    int startIndex = lat ? 1 : 5;
    double amount = Integer.parseInt(m.group(startIndex + 1))
        + Integer.parseInt(m.group(startIndex + 2)) / 60.0;
    if (m.group(startIndex + 3) != null)
      amount += Integer.parseInt(m.group(startIndex + 3)) / 3600.0;
    if (m.group(startIndex).equals("-"))
      amount = -amount;
    return new Double(amount);
  }

  /**
   * @return Returns the linkold_new.
   */
  public Map getZoneLinkold_new() {
    getZoneData();
    return linkold_new;
  }

  /**
   * @return Returns the linkold_new.
   */
  public Map getZoneLinkNew_OldSet() {
    getZoneData();
    return linkNew_oldSet;
  }

  /**
   * @return Returns the ruleID_rules.
   */
  public Map getZoneRuleID_rules() {
    getZoneData();
    return ruleID_rules;
  }

  /**
   * @return Returns the zone_rules.
   */
  public Map getZone_rules() {
    getZoneData();
    return zone_rules;
  }

  public String getVersion() {
    return version;
  }

}