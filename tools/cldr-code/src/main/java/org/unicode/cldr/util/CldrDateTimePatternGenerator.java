package org.unicode.cldr.util;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A generator that produces the best localized date/time pattern for a given skeleton.
 *
 * <p>This implementation strictly follows the LDML TR35 specification for DateTimePatternGenerator,
 * utilizing CLDRFile for direct data access and implementing the Matching Skeletons algorithm.
 */
public class CldrDateTimePatternGenerator {
    private final CLDRFile file;
    private final String calendarID;
    private final boolean useStock;

    private char defaultHourFormatChar = 'H';
    private String[] allowedHourFormats = {"H"};
    private String decimal = "?";
    private final Map<String, String> availableFormats = new LinkedHashMap<>();
    private final Map<String, String> appendItems = new LinkedHashMap<>();
    private final Map<String, String> fieldNames = new LinkedHashMap<>();

    private String dateTimeFormatFull = "{1} {0}";
    private String dateTimeFormatLong = "{1} {0}";
    private String dateTimeFormatMedium = "{1} {0}";
    private String dateTimeFormatShort = "{1} {0}";

    private static final String[] STOCK = DateTimeFormats.STOCK;

    /**
     * The canonical order of date/time fields as defined by the LDML specification (TR35).
     * Skeletons are normalized to this order to ensure consistent matching.
     */
    private static final String CANONICAL_ORDER = DateTimeFormats.CANONICAL_ORDER;

    /** Characters representing date fields. */
    private static final String DATE_FIELDS = DateTimeFormats.DATE_FIELDS;

    /** Characters representing time fields. */
    private static final String TIME_FIELDS = DateTimeFormats.TIME_FIELDS;

    /** Characters that are always numeric. */
    private static final String ALWAYS_NUMERIC_FIELDS = DateTimeFormats.ALWAYS_NUMERIC_FIELDS;

    /** Characters that are numeric if length is 1 or 2, and text otherwise. */
    private static final String NUMERIC_OR_TEXT_FIELDS = DateTimeFormats.NUMERIC_OR_TEXT_FIELDS;

    /**
     * Sets of related field characters that represent the same semantic field (e.g., M and L for
     * Month).
     */
    private static final String[] RELATED_FIELD_SETS = DateTimeFormats.RELATED_FIELD_SETS;

    private static final Map<Character, String> RELATED_CHAR_MAP = new HashMap<>();

    static {
        for (String set : RELATED_FIELD_SETS) {
            for (int i = 0; i < set.length(); i++) {
                RELATED_CHAR_MAP.put(set.charAt(i), set);
            }
        }
    }

    /**
     * Constructs a new generator for the given CLDRFile and calendar.
     *
     * @param file the CLDRFile to read data from (should be a resolved file for proper inheritance)
     * @param calendarID the ID of the calendar (e.g., "gregorian", "japanese")
     * @param useStock if true, the generator will also include the standard stock date and time
     *     formats (short, medium, long, full) as defined in the dateFormats/timeFormats sections
     *     when matching skeletons.
     */
    public CldrDateTimePatternGenerator(CLDRFile file, String calendarID, boolean useStock) {
        this.file = file;
        this.calendarID = calendarID;
        this.useStock = useStock;
        init();
    }

    /**
     * Initializes the generator by loading preferred hour formats, stock patterns, field display
     * names, and available format patterns from the CLDR data.
     */
    private void init() {
        initHourFormat();
        initDecimal();
        initStockPatterns();

        Set<String> allIds = new LinkedHashSet<>();
        Set<String> allAppendRequests = new LinkedHashSet<>();

        collectFormatIdsAndAppendRequests(allIds, allAppendRequests);
        initFieldNames();
        initAvailableFormats(allIds);
        initAppendItems(allAppendRequests);
        initDateTimeFormats();

        // Sort available formats to eliminate tie-breaking at runtime
        sortAvailableFormats();
    }

    /**
     * Determines the preferred hour format (h, H, K, or k) for the locale from supplemental data.
     */
    private void initHourFormat() {
        // TR35, Section 3.8.3: "it ['j'] requests the preferred hour format for the locale
        // (h, H, K, or k), as determined by the preferred attribute of the hours element
        // in supplemental data."
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        String localeID = file.getLocaleID();
        PreferredAndAllowedHour pref = sdi.getTimeData().get(localeID);
        if (pref == null) {
            CLDRLocale loc = CLDRLocale.getInstance(localeID);
            String region = loc.getCountry();
            if (region == null || region.isEmpty()) {
                CLDRLocale max = loc.getMaximal();
                if (max != null) {
                    region = max.getCountry();
                }
            }
            pref = sdi.getTimeData().get(region);
        }
        if (pref == null) {
            pref = sdi.getTimeData().get("001");
        }
        if (pref != null) {
            defaultHourFormatChar = pref.preferred.toString().charAt(0);
            allowedHourFormats = pref.allowed.stream().map(Object::toString).toArray(String[]::new);
        }
    }

    private void initDecimal() {
        String ns = file.getStringValueWithBailey("//ldml/numbers/defaultNumberingSystem");
        if (ns == null) {
            ns = "latn";
        }
        String decimalPath = "//ldml/numbers/symbols[@numberSystem='" + ns + "']/decimal";
        String decimalValue = file.getStringValueWithBailey(decimalPath);
        if (decimalValue != null) {
            this.decimal = decimalValue;
        }
    }

    /**
     * If useStock is true, includes standard stock date and time patterns (short, medium, long,
     * full) in the available formats.
     */
    private void initStockPatterns() {
        if (!useStock) return;

        for (String stock : STOCK) {
            String dBase =
                    String.format(
                            "//ldml/dates/calendars/calendar[@type=\"%s\"]/dateFormats/dateFormatLength[@type=\"%s\"]/dateFormat[@type=\"standard\"]",
                            calendarID, stock);
            String tBase =
                    String.format(
                            "//ldml/dates/calendars/calendar[@type=\"%s\"]/timeFormats/timeFormatLength[@type=\"%s\"]/timeFormat[@type=\"standard\"]",
                            calendarID, stock);

            String dp = file.getStringValueWithBailey(dBase + "/pattern[@type=\"standard\"]");
            String ds = file.getStringValueWithBailey(dBase + "/datetimeSkeleton");
            if (dp != null && ds != null) {
                availableFormats.put(canonicalizeSkeleton(ds), dp);
            }

            String tp = file.getStringValueWithBailey(tBase + "/pattern[@type=\"standard\"]");
            String ts = file.getStringValueWithBailey(tBase + "/datetimeSkeleton");
            if (tp != null && ts != null) {
                availableFormats.put(canonicalizeSkeleton(ts), tp);
            }
        }
    }

    /** Efficiently scans the calendar subtrees for all available format IDs and append requests. */
    private void collectFormatIdsAndAppendRequests(
            Set<String> allIds, Set<String> allAppendRequests) {
        for (String path : With.in(file.iterator("//ldml/dates/calendars/calendar"))) {
            if (path.contains("/availableFormats/dateFormatItem")) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String id = parts.getAttributeValue(-1, "id");
                if (id != null) allIds.add(id);
            } else if (path.contains("/appendItems/appendItem")) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String request = parts.getAttributeValue(-1, "request");
                if (request != null) allAppendRequests.add(request);
            }
        }
    }

    /**
     * Resolves the available format patterns for our specific calendar, following TR35
     * cross-calendar fallback rules.
     */
    private void initAvailableFormats(Set<String> allIds) {
        for (String id : allIds) {
            String path =
                    "//ldml/dates/calendars/calendar[@type=\""
                            + calendarID
                            + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                            + id
                            + "\"]";
            String value = file.getStringValueWithBailey(path);
            if (value != null) {
                availableFormats.put(canonicalizeSkeleton(maybeAddDayPeriod(id)), value);
            }
        }
    }

    private static CharSequence maybeAddDayPeriod(CharSequence skeleton) {
        // TR35: If skeletons for 12-hour time do not contain a day period, the skeleton will be
        // treated as implicitly containing 'a'.
        if (skeleton.chars().anyMatch(c -> c == 'h')
                && !skeleton.chars().anyMatch(c -> c == 'a' || c == 'b' || c == 'B')) {
            StringBuilder builder = new StringBuilder(skeleton);
            builder.append('a');
            return builder;
        }
        return skeleton;
    }

    /** Resolves the appendItem patterns for our specific calendar. */
    private void initAppendItems(Set<String> allAppendRequests) {
        for (String request : allAppendRequests) {
            String path =
                    "//ldml/dates/calendars/calendar[@type=\""
                            + calendarID
                            + "\"]/dateTimeFormats/appendItems/appendItem[@request=\""
                            + request
                            + "\"]";
            String value = file.getStringValueWithBailey(path);
            if (value != null) {
                appendItems.put(request, value);
            }
        }
    }

    /** Collects localized field display names from the fields subtree. */
    private void initFieldNames() {
        for (String path : With.in(file.iterator("//ldml/dates/fields/field"))) {
            if (path.contains("displayName")) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String type = parts.getAttributeValue(-2, "type");
                String value = file.getStringValueWithBailey(path);
                if (type != null && value != null) {
                    fieldNames.put(type, value);
                }
            }
        }
    }

    /** Loads the four standard date-time glue patterns (full, long, medium, short). */
    private void initDateTimeFormats() {
        String base =
                "//ldml/dates/calendars/calendar[@type=\"%s\"]/dateTimeFormats/dateTimeFormatLength[@type=\"%s\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        dateTimeFormatFull =
                file.getStringValueWithBailey(
                        String.format(base, calendarID, "full"), dateTimeFormatFull);
        dateTimeFormatLong =
                file.getStringValueWithBailey(
                        String.format(base, calendarID, "long"), dateTimeFormatLong);
        dateTimeFormatMedium =
                file.getStringValueWithBailey(
                        String.format(base, calendarID, "medium"), dateTimeFormatMedium);
        dateTimeFormatShort =
                file.getStringValueWithBailey(
                        String.format(base, calendarID, "short"), dateTimeFormatShort);
    }

    /** Returns the default hour format character (h, H, K, or k) for the locale. */
    public char getDefaultHourFormatChar() {
        return defaultHourFormatChar;
    }

    /**
     * Adds all base skeletons defined in the availableFormats to the provided set.
     *
     * @param result the set to add skeletons to
     * @return the provided set with added skeletons
     */
    public Set<String> getBaseSkeletons(Set<String> result) {
        if (result == null) {
            result = new LinkedHashSet<>();
        }
        result.addAll(availableFormats.keySet());
        return result;
    }

    /**
     * Returns the best matching localized pattern for the requested skeleton.
     *
     * <p>The process follows these steps: 1. Canonicalize the requested skeleton. 2. Check for an
     * exact match in available formats. 3. If it contains both date and time fields, split them,
     * match independently, and combine. 4. Search for the closest available skeleton using the TR35
     * distance metric. 5. Expand the matched pattern to match requested field lengths. 6. Use
     * appendItems to add any requested fields missing from the best match.
     *
     * <p>For details of how the DTPG arrived at the pattern, pass a List as the second argument.
     *
     * @param skeleton the requested skeleton
     * @return the best localized pattern
     */
    public String getBestPattern(String skeleton) {
        return getBestPattern(skeleton, null);
    }

    /**
     * Returns the best matching localized pattern for the requested skeleton, and records the steps
     * taken in the provided log list.
     *
     * @param skeleton the requested skeleton
     * @param log a list to collect the trace messages, or null
     * @return the best localized pattern
     */
    public String getBestPattern(String skeleton, List<String> log) {
        if (skeleton == null || skeleton.isEmpty()) return "";
        boolean skeletonHasCapJ = skeleton.contains("J");
        String result = getBestPatternInternal(skeleton, log);
        if (skeletonHasCapJ) {
            // result = removeDayPeriods(result);
            // After removing day periods, we MUST ensure the hour field matches
            // defaultHourFormatChar
            result = replaceHourCharacter(result, defaultHourFormatChar);
        }
        return result;
    }

    private String getBestPatternInternal(String skeleton, List<String> log) {
        StringBuilder skeletonCopy = new StringBuilder();
        for (int i = 0; i < skeleton.length(); i++) {
            char reqChar = skeleton.charAt(i);
            if (reqChar == 'j' || reqChar == 'C' || reqChar == 'J') {
                int extraLen = 0;
                while (i + 1 < skeleton.length() && skeleton.charAt(i + 1) == reqChar) {
                    extraLen++;
                    i++;
                }

                // This matches TR35 logic for j, J, and C skeleton field length handling.
                int hourLen;
                if (extraLen % 1 == 0) {
                    hourLen = 1;
                } else {
                    hourLen = 2;
                }
                int dayPeriodLen;
                if (extraLen < 2) {
                    // j, jj, C, CC → a, b, B (abbreviated)
                    dayPeriodLen = 1;
                } else if (extraLen < 4) {
                    // jjj, jjjj, CCC, CCCC → aaaa, bbbb, BBBB (wide)
                    dayPeriodLen = 4;
                } else {
                    // jjjjj, jjjjjj, CCCCC, CCCCCC → aaaaa, bbbbb, BBBBB (narrow)
                    dayPeriodLen = 5;
                }

                char hourChar = 'H';
                char dayPeriodChar = 0;
                switch (reqChar) {
                    case 'J':
                        // Find the H pattern to avoid the day period
                        // (no-op)
                        break;
                    case 'j':
                        switch (defaultHourFormatChar) {
                            case 'h':
                            case 'K':
                                hourChar = 'h';
                                dayPeriodChar = 'a';
                                break;
                            default:
                                // (no-op)
                                break;
                        }
                        break;
                    case 'C':
                        switch (allowedHourFormats[0]) {
                            case "H":
                                // (no-op)
                                break;
                            case "h":
                                hourChar = 'h';
                                dayPeriodChar = 'a';
                                break;
                            case "hb":
                                hourChar = 'h';
                                dayPeriodChar = 'b';
                                break;
                            case "hB":
                                hourChar = 'h';
                                dayPeriodChar = 'B';
                                break;
                            default:
                                assert false;
                        }
                    default:
                        assert false;
                }

                for (int x = 0; x < hourLen; x++) skeletonCopy.append(hourChar);
                if (dayPeriodChar != 0) {
                    for (int x = 0; x < dayPeriodLen; x++) skeletonCopy.append(dayPeriodChar);
                }
            } else {
                skeletonCopy.append(reqChar);
            }
        }

        String canonicalSkeleton = canonicalizeSkeleton(maybeAddDayPeriod(skeletonCopy));

        if (log != null && !skeleton.equals(canonicalSkeleton)) {
            log.add("Canonical skeleton: " + skeleton + " → " + canonicalSkeleton);
        }

        if (availableFormats.containsKey(canonicalSkeleton)) {
            String result = availableFormats.get(canonicalSkeleton);
            if (log != null) log.add("Exact match: " + result);
            return result;
        }

        String dateSkeleton = getDateSkeleton(canonicalSkeleton);
        String timeSkeleton = getTimeSkeleton(canonicalSkeleton);

        if (dateSkeleton.length() > 0 && timeSkeleton.length() > 0) {
            return combineDateAndTime(dateSkeleton, timeSkeleton, log);
        }

        String bestMatchSkeleton = findBestMatch(canonicalSkeleton);

        if (bestMatchSkeleton != null) {
            String origPattern = availableFormats.get(bestMatchSkeleton);
            String expandedPattern =
                    expandPattern(canonicalSkeleton, bestMatchSkeleton, origPattern);
            if (log != null)
                log.add(
                        "Closest match: "
                                + bestMatchSkeleton
                                + " → "
                                + origPattern
                                + " → "
                                + expandedPattern);

            return appendMissingFields(expandedPattern, canonicalSkeleton, bestMatchSkeleton, log);
        }

        return getFallbackPattern(canonicalSkeleton, log);
    }

    private String replaceHourCharacter(String pattern, char newHourChar) {
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
                sb.append(c);
                continue;
            }
            if (!inQuotes && (c == 'h' || c == 'H' || c == 'k' || c == 'K')) {
                sb.append(newHourChar);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Combines date and time patterns using the appropriate dateTimeFormat glue pattern. */
    private String combineDateAndTime(String dateSkeleton, String timeSkeleton, List<String> log) {
        if (log != null)
            log.add(
                    "Splitting skeleton into date='"
                            + dateSkeleton
                            + "' and time='"
                            + timeSkeleton
                            + "'");
        String datePattern = getBestPattern(dateSkeleton, log);
        String timePattern = getBestPattern(timeSkeleton, log);
        String dateTimePattern = getDateTimePattern(dateSkeleton);
        String combined = dateTimePattern.replace("{1}", datePattern).replace("{0}", timePattern);
        if (log != null)
            log.add("Combined components using '" + dateTimePattern + "' → " + combined);
        return combined;
    }

    /** Finds the available skeleton with the minimum distance to the requested skeleton. */
    private String findBestMatch(String canonicalSkeleton) {
        int bestDistance = Integer.MAX_VALUE;
        String bestMatchSkeleton = null;

        for (String availSkeleton : availableFormats.keySet()) {
            int dist = getDistance(canonicalSkeleton, availSkeleton);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestMatchSkeleton = availSkeleton;
            }
        }
        // Distance of 10000 or more means no reasonable match was found (e.g. extra fields).
        return bestDistance < 10000 ? bestMatchSkeleton : null;
    }

    /** Appends fields from the requested skeleton that are missing in the matched skeleton. */
    private String appendMissingFields(
            String pattern, String reqSkeleton, String matchSkeleton, List<String> log) {
        List<String> reqFields = splitSkeleton(reqSkeleton);
        List<String> availFields = splitSkeleton(matchSkeleton);
        Set<Character> availChars = new LinkedHashSet<>();

        for (String f : availFields) {
            char c = f.charAt(0);
            availChars.add(c);
            for (char co : CANONICAL_ORDER.toCharArray()) {
                if (areFieldsRelated(c, co)) availChars.add(co);
            }
        }

        for (String rf : reqFields) {
            if (!availChars.contains(rf.charAt(0))) {
                pattern = appendField(pattern, rf);
                if (log != null) log.add("Appended missing field '" + rf + "' → " + pattern);
            }
        }
        return pattern;
    }

    /**
     * Generates a basic pattern as a last resort by concatenating basic patterns for each field.
     */
    private String getFallbackPattern(String canonicalSkeleton, List<String> log) {
        if (log != null) log.add("No close match found, falling back to basic patterns");
        List<String> fields = splitSkeleton(canonicalSkeleton);
        if (fields.isEmpty()) return "";

        String res = "";
        for (String f : fields) {
            if (res.isEmpty()) {
                res = getBasicPattern(f);
                if (log != null) log.add("Started fallback with '" + f + "' → " + res);
            } else {
                res = appendField(res, f);
                if (log != null) log.add("Appended fallback field '" + f + "' → " + res);
            }
        }
        return res;
    }

    /**
     * Appends a missing field to an existing pattern using the appendItems template.
     *
     * @param pattern the existing pattern
     * @param field the missing field to append
     * @return the updated pattern
     */
    private String appendField(String pattern, String field) {
        char firstChar = field.charAt(0);
        String requestName = getAppendRequestName(firstChar);
        String appendFormat = appendItems.get(requestName);
        if (appendFormat == null) {
            appendFormat = "{0} \u251c{2}: {1}\u2524";
        }

        String fieldNameKey = getFieldDisplayNameKey(firstChar);
        String fieldDisplayName = fieldNames.getOrDefault(fieldNameKey, fieldNameKey);

        String firstFieldPattern = getBasicPattern(field);
        return appendFormat
                .replace("{0}", pattern)
                .replace("{1}", firstFieldPattern)
                .replace("{2}", quote(fieldDisplayName));
    }

    private String quote(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                sb.append("''");
            } else {
                sb.append(c);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    /**
     * Returns the TR35 appendItem request name for a given field character.
     *
     * @param fieldChar the field character
     * @return the request name (e.g., "Year", "Month")
     */
    private String getAppendRequestName(char fieldChar) {
        String result = DateTimeFormats.getAppendRequestName(fieldChar);
        return result != null ? result : String.valueOf(fieldChar);
    }

    /**
     * Returns the CLDR field name key for display name lookup.
     *
     * @param fieldChar the field character
     * @return the field name key (e.g., "year", "month")
     */
    private String getFieldDisplayNameKey(char fieldChar) {
        String result = DateTimeFormats.getFieldDisplayNameKey(fieldChar);
        return result != null ? result : String.valueOf(fieldChar);
    }

    /**
     * Produces a basic pattern for a single field by searching available formats for a matching or
     * related field of the same length.
     *
     * @param field the field to generate a pattern for
     * @return a localized pattern string
     */
    private String getBasicPattern(String field) {
        String p = availableFormats.get(field);
        if (p != null) return p;
        // Weekday in skeleton is E but it is EEE in patterns
        if (field.equals("E")) {
            field = "EEE";
        }
        for (String avail : availableFormats.keySet()) {
            if (splitSkeleton(avail).size() == 1
                    && avail.length() == field.length()
                    && areFieldsRelated(avail.charAt(0), field.charAt(0))) {
                if (isNumeric(avail) == isNumeric(field)) {
                    return expandPattern(field, avail, availableFormats.get(avail));
                }
            }
        }
        return field;
    }

    /**
     * Extracts date-related fields from a skeleton.
     *
     * @param skeleton the full skeleton
     * @return a skeleton containing only date fields
     */
    private String getDateSkeleton(String skeleton) {
        return filterSkeleton(skeleton, DATE_FIELDS);
    }

    /**
     * Extracts time-related fields from a skeleton.
     *
     * @param skeleton the full skeleton
     * @return a skeleton containing only time fields
     */
    private String getTimeSkeleton(String skeleton) {
        return filterSkeleton(skeleton, TIME_FIELDS);
    }

    private String filterSkeleton(String skeleton, String allowedFields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skeleton.length(); i++) {
            char c = skeleton.charAt(i);
            if (allowedFields.indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Determines which dateTimeFormat glue pattern to use based on the TR35 "Missing Skeleton
     * Fields" algorithm. The selection is strictly based on the width of the Month and Weekday
     * fields in the date half.
     *
     * @param dateSkeleton the date portion of the skeleton
     * @return the combined dateTimeFormat pattern
     */
    private String getDateTimePattern(String dateSkeleton) {
        int maxMonthLen = 0;
        int maxWeekdayLen = 0;
        for (String field : splitSkeleton(dateSkeleton)) {
            char c = field.charAt(0);
            if (c == 'M' || c == 'L') {
                maxMonthLen = Math.max(maxMonthLen, field.length());
            } else if (c == 'E' || c == 'c') {
                maxWeekdayLen = Math.max(maxWeekdayLen, field.length());
            }
        }

        if (maxMonthLen >= 4 && maxWeekdayLen >= 4) return dateTimeFormatFull;
        if (maxMonthLen >= 4) return dateTimeFormatLong;
        if (maxMonthLen >= 3) return dateTimeFormatMedium;
        return dateTimeFormatShort;
    }

    /**
     * Calculates the TR35 distance between a requested skeleton and an available one.
     *
     * <p>Penalties: - Same field type and length = 0. - Same field type, different length = |req -
     * avail|. - Related field type (e.g. M vs L) = 16 + |req - avail|. - Numeric/Text mismatch
     * (e.g. M vs MMM) = 100 + |req - avail|. - Missing field = 1000. - Extra fields in available =
     * rejected (10000).
     *
     * @param req the requested skeleton
     * @param avail the available skeleton to compare
     * @return the calculated distance
     */
    private int getDistance(String req, String avail) {
        Map<Character, String> reqMap = getSkeletonMap(req);
        Map<Character, String> availMap = getSkeletonMap(avail);

        int dist = 0;
        for (char c : availMap.keySet()) {
            if (getMatchingField(reqMap, c) == null) {
                return 10000; // Extra fields in available are not allowed.
            }
        }

        for (String rf : splitSkeleton(req)) {
            char rc = rf.charAt(0);
            String af = getMatchingField(availMap, rc);

            if (af != null) {
                dist += getSingleFieldDistance(af, rf);
            } else {
                dist += 1000; // Missing field in available.
            }
        }
        return dist;
    }

    /**
     * Converts a skeleton string into a map from field character to field string (e.g. 'y' →
     * "yyyy").
     */
    private Map<Character, String> getSkeletonMap(String skeleton) {
        Map<Character, String> map = new HashMap<>();
        for (String f : splitSkeleton(skeleton)) {
            map.put(f.charAt(0), f);
        }
        return map;
    }

    /** Finds a field in the map that matches the given character, or is related to it. */
    private String getMatchingField(Map<Character, String> map, char fieldChar) {
        String match = map.get(fieldChar);
        if (match != null) return match;
        for (Map.Entry<Character, String> entry : map.entrySet()) {
            if (areFieldsRelated(fieldChar, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Calculates the TR35 distance between two individual fields. */
    private static int getSingleFieldDistance(String availField, String requestedField) {
        if (availField.equals(requestedField)) {
            return 0;
        }
        int diff = 0;
        if (availField.charAt(0) != requestedField.charAt(0)) {
            // Examples: h <=> H, b <=> a, y <=> U, v <=> z
            diff += 8;
        }
        boolean isNumeric = isNumeric(availField);
        if (isNumeric != isNumeric(requestedField)) {
            diff += 128;
        }
        if (isNumeric) {
            diff += Math.abs(availField.length() - requestedField.length());
        } else {
            // Normalize E to EEE
            int l1 = Math.max(3, availField.length());
            int l2 = Math.max(3, requestedField.length());
            // From Mark: If they are a different length, then:
            // - Between short and abbreviated is ~20
            // - Between abbreviated and wide is ~20
            // - Between short and wide is ~40
            // - Narrow should be closest to short
            //
            // 3 = abbr
            // 4 = wide
            // 5 = narrow
            // 6 = short
            //
            // 5' = narrow
            // 6' = short
            // 7' = abbr
            // 8' = wide
            if (l1 <= 4) {
                l1 += 4;
            }
            if (l2 <= 4) {
                l2 += 4;
            }
            diff += 16 * Math.abs(l1 - l2);
        }
        return diff;
    }

    /**
     * Checks if a field with a given character and length is numeric or text.
     *
     * @param field the field string, like "MMM"
     * @return true if numeric, false if text
     */
    private static boolean isNumeric(CharSequence fieldString) {
        char field = fieldString.charAt(0);
        if (ALWAYS_NUMERIC_FIELDS.indexOf(field) >= 0) {
            return true;
        }
        if (NUMERIC_OR_TEXT_FIELDS.indexOf(field) >= 0) {
            return fieldString.length() <= 2;
        }
        return false;
    }

    /**
     * Checks if two field characters represent the same semantic field (e.g., M and L for Month).
     *
     * @param a first field character
     * @param b second field character
     * @return true if the fields are related
     */
    private boolean areFieldsRelated(char a, char b) {
        if (a == b) return true;
        String set = RELATED_CHAR_MAP.get(a);
        return set != null && set.indexOf(b) >= 0;
    }

    /**
     * Splits a skeleton into its constituent field strings (e.g., "yMMMd" → ["y", "MMM", "d"]).
     *
     * @param skel the skeleton to split
     * @return a list of field strings
     */
    private List<String> splitSkeleton(String skel) {
        List<String> res = new ArrayList<>();
        if (skel == null || skel.isEmpty()) return res;
        int i = 0;
        while (i < skel.length()) {
            char c = skel.charAt(i);
            int start = i;
            while (i < skel.length() && skel.charAt(i) == c) i++;
            res.add(skel.substring(start, i));
        }
        return res;
    }

    /**
     * Sorts the available formats based on TR35 tie-breaking rules. This ensures that findBestMatch
     * can simply take the first matching distance, eliminating the need for runtime tie-breaking.
     */
    private void sortAvailableFormats() {
        Map<String, char[]> charsMap = new HashMap<>();
        Map<String, int[]> lengthsMap = new HashMap<>();
        for (String skel : availableFormats.keySet()) {
            int limit = DateTimePatternGenerator.TYPE_LIMIT;
            char[] chars = new char[limit];
            int[] lengths = new int[limit];
            populateFields(skel, chars, lengths);
            charsMap.put(skel, chars);
            lengthsMap.put(skel, lengths);
        }

        List<Map.Entry<String, String>> entries = new ArrayList<>(availableFormats.entrySet());
        entries.sort(
                (e1, e2) -> {
                    char[] chars1 = charsMap.get(e1.getKey());
                    char[] chars2 = charsMap.get(e2.getKey());
                    int[] lengths1 = lengthsMap.get(e1.getKey());
                    int[] lengths2 = lengthsMap.get(e2.getKey());

                    int limit = DateTimePatternGenerator.TYPE_LIMIT;
                    for (int i = 0; i < limit; i++) {
                        if (chars1[i] != chars2[i]) {
                            return chars2[i] - chars1[i];
                        }
                        if (lengths1[i] != lengths2[i]) {
                            return lengths2[i] - lengths1[i];
                        }
                    }
                    return e1.getKey().compareTo(e2.getKey());
                });

        availableFormats.clear();
        for (Map.Entry<String, String> entry : entries) {
            availableFormats.put(entry.getKey(), entry.getValue());
        }
    }

    /** Populates arrays with field characters and lengths, indexed by the ICU4J field type. */
    private void populateFields(String skeleton, char[] chars, int[] lengths) {
        for (String f : splitSkeleton(skeleton)) {
            try {
                int type = new com.ibm.icu.text.DateTimePatternGenerator.VariableField(f).getType();
                if (type >= 0 && type < DateTimePatternGenerator.TYPE_LIMIT) {
                    chars[type] = f.charAt(0);
                    lengths[type] = f.length();
                }
            } catch (Exception e) {
                // Ignore invalid fields in the available skeleton.
            }
        }
    }

    /**
     * Adjusts the field lengths and types in a base pattern to match the requested skeleton.
     *
     * @param reqSkeleton the requested skeleton
     * @param availSkeleton the available skeleton the pattern came from
     * @param pattern the localized pattern to expand
     * @return the expanded pattern
     */
    private String expandPattern(String reqSkeleton, String availSkeleton, String pattern) {
        Map<Character, String> reqMap = getSkeletonMap(reqSkeleton);
        Map<Character, String> availMap = getSkeletonMap(availSkeleton);

        StringBuilder res = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < pattern.length(); ) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
                res.append(c);
                i++;
                continue;
            }
            if (inQuotes || !Character.isLetter(c)) {
                res.append(c);
                i++;
                continue;
            }

            // The rest of this function calculates the desired output char and length.
            // We default to the field in the pattern and override only in specific cases.
            char outChar = c;
            int start = i;
            while (i < pattern.length() && pattern.charAt(i) == c) i++;
            int outLen = i - start;
            String patF = pattern.substring(start, i);

            // Special handling for seconds and fractional seconds
            if (c == 's' && !availSkeleton.contains("S")) {
                String reqS = getMatchingField(reqMap, 'S');
                if (reqS != null && reqSkeleton.contains("S")) {
                    res.append(patF);
                    res.append(decimal);
                    res.append(reqS);
                    continue;
                }
            }

            // reqF is the field from the requested skeleton.
            // availF is the field from the available skeleton.
            //
            // For example:
            // - Requested skeleton: "yMMdE"
            // - Available skeleton (closest match): "yMdE"
            // - Pattern: "EEE, M/d/y"
            String reqF = getMatchingField(reqMap, c);
            String availF = getMatchingField(availMap, c);
            reqF = skeletonFieldToPatternField(reqF);
            availF = skeletonFieldToPatternField(availF);

            // Check if we need to change the length or character of the field.
            // This only happens if the corresponding field is in the skeleton!
            if (availF != null && reqF != null) {
                char reqChar = reqF.charAt(0);
                if (availF.charAt(0) != reqChar) {
                    // The available skeleton has a different field character than the requested
                    // skeleton.
                    // Adjust the field character if it is in an appropriate category.
                    if (reqChar != 'j' && reqChar != 'J' && reqChar != 'C' && reqChar != 'U') {
                        outChar = reqChar;
                    }
                }
                int reqLen = reqF.length();
                if (availF.length() != reqLen) {
                    // The available skeleton has a different field length than the requested
                    // skeleton.
                    // Adjust the field length so long as we don't cross between numeric and
                    // alphabetic.
                    if (isNumeric(patF) == isNumeric(reqF)
                            && isNumeric(patF) == isNumeric(availF)) {
                        outLen = reqLen;
                    }
                }
            }

            // Print out the resolved character and length.
            for (int k = 0; k < outLen; k++) {
                res.append(outChar);
            }
        }
        return res.toString();
    }

    /**
     * Converts a skeleton field like "E" to a pattern field like "EEE".
     *
     * <p>For most fields, this is a no-op.
     */
    private static String skeletonFieldToPatternField(String skeletonField) {
        if ("E".equals(skeletonField)) {
            return "EEE";
        }
        return skeletonField;
    }

    /**
     * Normalizes a skeleton to the canonical field order.
     *
     * @param skel the skeleton to normalize
     * @return the canonicalized skeleton
     */
    private String canonicalizeSkeleton(CharSequence skel) {
        int[] counts = new int[128];
        for (int i = 0; i < skel.length(); i++) {
            char c = skel.charAt(i);
            assert c < 128;
            counts[c]++;
        }
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < CANONICAL_ORDER.length(); i++) {
            char c = CANONICAL_ORDER.charAt(i);
            for (int j = 0; j < counts[c]; j++) res.append(c);
        }
        return res.toString();
    }

    /**
     * Creates an ICU4J DateTimePatternGenerator populated with the exact same data loaded by this
     * CldrDateTimePatternGenerator. This is useful for algorithmic comparisons.
     */
    public DateTimePatternGenerator getIcu4jGenerator() {
        DateTimePatternGenerator icuGen = DateTimePatternGenerator.getEmptyInstance();
        icuGen.setDefaultHourFormatChar(defaultHourFormatChar);

        // TODO: ICU4J's DateTimePatternGenerator.getEmptyInstance() doesn't initialize
        // allowedHourFormats, causing an NPE in getBestPattern for skeletons containing 'C'.
        // Use reflection as a last resort until a public API is added.
        try {
            Field field = DateTimePatternGenerator.class.getDeclaredField("allowedHourFormats");
            field.setAccessible(true);
            field.set(icuGen, allowedHourFormats);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If reflection fails, some skeletons (e.g., 'C') will still crash in ICU4J.
        }

        icuGen.setDateTimeFormat(DateFormat.FULL, dateTimeFormatFull);
        icuGen.setDateTimeFormat(DateFormat.LONG, dateTimeFormatLong);
        icuGen.setDateTimeFormat(DateFormat.MEDIUM, dateTimeFormatMedium);
        icuGen.setDateTimeFormat(DateFormat.SHORT, dateTimeFormatShort);
        icuGen.setDecimal(decimal);

        /**
         * We MUST use override=true here to ensure that all patterns defined in CLDR's
         * availableFormats are added to the ICU4J generator's internal skeleton2pattern map.
         *
         * <p>Furthermore, we MUST use addPatternWithSkeleton instead of addPattern because some
         * CLDR patterns map a specific skeleton to a pattern that doesn't have the exact same field
         * widths (e.g., cs: yMMMd → d. M. y). If we only pass the pattern, ICU4J derives the
         * skeleton (yMd) and forgets the original intent (yMMMd), leading to incorrect fallback
         * adjustments during getBestPattern.
         */
        for (Map.Entry<String, String> entry : availableFormats.entrySet()) {
            icuGen.addPatternWithSkeleton(
                    entry.getValue(),
                    entry.getKey(),
                    true,
                    new DateTimePatternGenerator.PatternInfo());
        }

        for (Map.Entry<String, String> entry : appendItems.entrySet()) {
            int field = mapAppendItem(entry.getKey());
            if (field != -1) {
                icuGen.setAppendItemFormat(field, entry.getValue());
            }
        }

        for (Map.Entry<String, String> entry : fieldNames.entrySet()) {
            int field = mapFieldName(entry.getKey());
            if (field != -1) {
                icuGen.setAppendItemName(field, entry.getValue());
            }
        }

        return icuGen;
    }

    private static int mapAppendItem(String request) {
        return DateTimeFormats.mapAppendItem(request);
    }

    private static int mapFieldName(String key) {
        return DateTimeFormats.mapFieldName(key);
    }
}
