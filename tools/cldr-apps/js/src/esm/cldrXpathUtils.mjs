// Extra utilities for XPath handling
// Split out for testing purposes

/**
 * Is the given path exceptional in the sense that null value is allowed?
 *
 * @param path the path
 * @return true if null value is allowed for path, else false
 *
 * This function is nearly identical to the Java function with the same name in TestPaths.java.
 * Keep it consistent with that function. It would be more ideal if this knowledge were encapsulated
 * on the server and the client didn't need to know about it. The server could send the client special
 * fallback values instead of null.
 *
 * Unlike the Java version on the server, here on the client we don't actually check that the path is an "extra" path.
 *
 * Example: http://localhost:8080/cldr-apps/v#/pa_Arab/Gregorian/35b886c9d25c9cb7
 * //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="midnight"]
 *
 * Reference: https://unicode-org.atlassian.net/browse/CLDR-11238
 */
function extraPathAllowsNullValue(path) {
  for (const prefix of [
    '//ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@',
    "//ldml/dates/timeZoneNames/metazone[@",
    "//ldml/dates/timeZoneNames/zone[@",
    "//ldml/numbers/currencies/currency[@",
    '//ldml/units/unitLength[@type="long"]/unit[@',
    '//ldml/units/unitLength[@type="long"]/compoundUnit[@',
    "//ldml/numbers/minimalPairs/caseMinimalPairs/",
  ]) {
    if (path.startsWith(prefix)) {
      return true;
    }
  }
  return false;
}

export { extraPathAllowsNullValue };
