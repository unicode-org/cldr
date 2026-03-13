package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.util.Output;
import org.unicode.cldr.util.XMLSource;

/**
 * A generator that produces the best localized date/time pattern for a given skeleton.
 * 
 * This implementation strictly follows the LDML TR35 specification for DateTimePatternGenerator,
 * utilizing CLDRFile for direct data access and implementing the Matching Skeletons algorithm.
 */
public class CldrDateTimePatternGenerator {
    private final CLDRFile file;
    private final String calendarID;
    private final boolean useStock;

    private char defaultHourFormatChar = 'H';
    private Map<String, String> availableFormats = new HashMap<>();
    private Map<String, String> appendItems = new HashMap<>();
    private Map<String, String> fieldNames = new HashMap<>();

    private String dateTimeFormatFull = "{1} {0}";
    private String dateTimeFormatLong = "{1} {0}";
    private String dateTimeFormatMedium = "{1} {0}";
    private String dateTimeFormatShort = "{1} {0}";

    private static final String[] STOCK = {"short", "medium", "long", "full"};
    
    /**
     * The canonical order of date/time fields as defined by the LDML specification (TR35).
     * Skeletons are normalized to this order to ensure consistent matching.
     */
    private static final String CANONICAL_ORDER = "GyYuUrQqMLwWEecdDFgabBhHKkmsSAzZOvVXx";

    /** Characters representing date fields. */
    private static final String DATE_FIELDS = "GyYruUQqMLwWEdDFg";

    /** Characters representing time fields. */
    private static final String TIME_FIELDS = "aBhHkKmmsSAzZOvVXx";

    /** Characters that are always numeric. */
    private static final String ALWAYS_NUMERIC_FIELDS = "yYruUwWdDFghHKkmsSA";

    /** Characters that are numeric if length is 1 or 2, and text otherwise. */
    private static final String NUMERIC_OR_TEXT_FIELDS = "MLQqec";

    /** Sets of related field characters that represent the same semantic field (e.g., M and L for Month). */
    private static final String[] RELATED_FIELD_SETS = {
        "yYruU", "ML", "wW", "dDFg", "Eec", "abB", "hHKk", "sSA", "zZOvVXx"
    };

    /**
     * Constructs a new generator for the given CLDRFile and calendar.
     * 
     * @param file the CLDRFile to read data from (should be a resolved file for proper inheritance)
     * @param calendarID the ID of the calendar (e.g., "gregorian", "japanese")
     * @param useStock if true, the generator will also include the standard stock date and time 
     *                 formats (short, medium, long, full) as defined in the dateFormats/timeFormats 
     *                 sections when matching skeletons.
     */
    public CldrDateTimePatternGenerator(CLDRFile file, String calendarID, boolean useStock) {
        this.file = file;
        this.calendarID = calendarID;
        this.useStock = useStock;
        init();
    }

    /**
     * Initializes the generator by loading preferred hour formats, stock patterns,
     * field display names, and available format patterns from the CLDR data.
     */
    private void init() {
        // TR35, Section 3.8.3: "it ['j'] requests the preferred hour format for the locale 
        // (h, H, K, or k), as determined by the preferred attribute of the hours element 
        // in supplemental data."
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        CLDRLocale loc = CLDRLocale.getInstance(file.getLocaleID());
        String region = loc.getCountry();
        if (region == null || region.isEmpty()) {
            CLDRLocale max = loc.getMaximal();
            if (max != null) {
                region = max.getCountry();
            }
        }
        PreferredAndAllowedHour pref = sdi.getTimeData().get(region);
        if (pref == null) {
            pref = sdi.getTimeData().get("001");
        }
        if (pref != null) {
            defaultHourFormatChar = pref.preferred.toString().charAt(0);
        }

        if (useStock) {
            for (String stock : STOCK) {
                String dPath = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateFormats/dateFormatLength[@type=\"" + stock + "\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String dsPath = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateFormats/dateFormatLength[@type=\"" + stock + "\"]/dateFormat[@type=\"standard\"]/datetimeSkeleton";
                String tPath = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/timeFormats/timeFormatLength[@type=\"" + stock + "\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String tsPath = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/timeFormats/timeFormatLength[@type=\"" + stock + "\"]/timeFormat[@type=\"standard\"]/datetimeSkeleton";
                
                String dp = getStringValueWithFallback(dPath);
                String ds = getStringValueWithFallback(dsPath);
                if (dp != null && ds != null) {
                    availableFormats.put(canonicalizeSkeleton(ds), dp);
                }
                
                String tp = getStringValueWithFallback(tPath);
                String ts = getStringValueWithFallback(tsPath);
                if (tp != null && ts != null) {
                    availableFormats.put(canonicalizeSkeleton(ts), tp);
                }
            }
        }

        Set<String> allIds = new LinkedHashSet<>();
        Set<String> allAppendRequests = new LinkedHashSet<>();
        
        // Efficiently collect all possible format IDs and append requests from the calendar subtrees.
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
        
        // Collect field display names from the fields subtree.
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

        // Resolve the actual patterns for OUR specific calendar (handling TR35 cross-calendar fallback).
        for (String id : allIds) {
            String path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"" + id + "\"]";
            String value = getStringValueWithFallback(path);
            if (value != null) {
                availableFormats.put(canonicalizeSkeleton(id), value);
            }
        }

        for (String request : allAppendRequests) {
            String path = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/appendItems/appendItem[@request=\"" + request + "\"]";
            String value = getStringValueWithFallback(path);
            if (value != null) {
                appendItems.put(request, value);
            }
        }

        // TODO: Consider using atTime pattern
        String dateTimeFormatLengthPattern = "//ldml/dates/calendars/calendar[@type=\"%s\"]/dateTimeFormats/dateTimeFormatLength[@type=\"%s\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        dateTimeFormatFull = getStringValueWithFallbackOrDefault(
            String.format(dateTimeFormatLengthPattern, calendarID, "full"),
            dateTimeFormatFull
        );
        dateTimeFormatLong = getStringValueWithFallbackOrDefault(
            String.format(dateTimeFormatLengthPattern, calendarID, "long"),
            dateTimeFormatLong
        );
        dateTimeFormatMedium = getStringValueWithFallbackOrDefault(
            String.format(dateTimeFormatLengthPattern, calendarID, "medium"),
            dateTimeFormatMedium
        );
        dateTimeFormatShort = getStringValueWithFallbackOrDefault(
            String.format(dateTimeFormatLengthPattern, calendarID, "short"),
            dateTimeFormatShort
        );
    }

    /**
     * Resolves values from the CLDRFile, strictly handling the TR35 rule that 
     * non-Gregorian calendars fallback to Gregorian values if missing locally 
     * or at the root aliasing levels.
     * 
     * @param path the XPath to resolve
     * @return the resolved string value, or null if not found
     */
    private String getStringValueWithFallback(String path) {
        Output<String> localeWhereFound = new Output<>();
        String val = file.getStringValueWithBailey(path, null, localeWhereFound);

        if (!calendarID.equals("gregorian")) {
            // If value is null, or it was only found at root/code-fallback, we must 
            // attempt to inherit from the specific locale's Gregorian calendar first.
            if (val == null || (localeWhereFound.value != null && (localeWhereFound.value.equals("root") || localeWhereFound.value.equals(XMLSource.CODE_FALLBACK_ID)))) {
                String gregPath = path.replaceFirst("\\[@type=\"[^\"]+\"\\]", "[@type=\"gregorian\"]");
                String gregVal = file.getStringValueWithBailey(gregPath, null, localeWhereFound);
                if (gregVal != null && localeWhereFound.value != null && !localeWhereFound.value.equals("root") && !localeWhereFound.value.equals(XMLSource.CODE_FALLBACK_ID)) {
                    return gregVal;
                }
            }
        }
        return val;
    }

    /**
     * Resolves a value from the CLDRFile with fallback, returning a default value if not found.
     * 
     * @param path the XPath to resolve
     * @param defaultValue the value to return if resolution fails
     * @return the resolved value or the default
     */
    private String getStringValueWithFallbackOrDefault(String path, String defaultValue) {
        String val = getStringValueWithFallback(path);
        return val != null ? val : defaultValue;
    }

    /**
     * Returns the default hour format character (h, H, K, or k) for the locale.
     */
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
     * The process follows these steps:
     * 1. Canonicalize the requested skeleton.
     * 2. Check for an exact match in available formats.
     * 3. If it contains both date and time fields, split them, match independently, and combine.
     * 4. Search for the closest available skeleton using the TR35 distance metric.
     * 5. Expand the matched pattern to match requested field lengths.
     * 6. Use appendItems to add any requested fields missing from the best match.
     * 
     * @param skeleton the requested skeleton
     * @return the best localized pattern
     */
    public String getBestPattern(String skeleton) {
        if (skeleton == null || skeleton.isEmpty()) return "";
        
        skeleton = skeleton.replace('j', defaultHourFormatChar);
        skeleton = skeleton.replace('C', 'a');
        
        String canonicalSkeleton = canonicalizeSkeleton(skeleton);
        
        if (availableFormats.containsKey(canonicalSkeleton)) {
            return availableFormats.get(canonicalSkeleton);
        }

        String dateSkeleton = getDateSkeleton(canonicalSkeleton);
        String timeSkeleton = getTimeSkeleton(canonicalSkeleton);
        
        if (dateSkeleton.length() > 0 && timeSkeleton.length() > 0) {
            String datePattern = getBestPattern(dateSkeleton);
            String timePattern = getBestPattern(timeSkeleton);
            String dateTimePattern = getDateTimePattern(dateSkeleton);
            return dateTimePattern.replace("{1}", datePattern).replace("{0}", timePattern);
        }

        int bestDistance = Integer.MAX_VALUE;
        String bestMatchSkeleton = null;
        
        for (String availSkeleton : availableFormats.keySet()) {
            int dist = getDistance(canonicalSkeleton, availSkeleton);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestMatchSkeleton = availSkeleton;
            }
        }

        if (bestMatchSkeleton != null && bestDistance < 1000) {
            String pattern = expandPattern(canonicalSkeleton, bestMatchSkeleton, availableFormats.get(bestMatchSkeleton));
            
            // Handle missing fields
            List<String> reqFields = splitSkeleton(canonicalSkeleton);
            List<String> availFields = splitSkeleton(bestMatchSkeleton);
            Set<Character> availChars = new LinkedHashSet<>();
            for (String f : availFields) {
                availChars.add(f.charAt(0));
                for (char c : CANONICAL_ORDER.toCharArray()) {
                    if (areFieldsRelated(f.charAt(0), c)) availChars.add(c);
                }
            }
            
            for (String rf : reqFields) {
                if (!availChars.contains(rf.charAt(0))) {
                    pattern = appendField(pattern, rf);
                }
            }
            return pattern;
        }
        
        // Final fallback
        List<String> fields = splitSkeleton(canonicalSkeleton);
        if (fields.isEmpty()) return "";
        String res = "";
        for (String f : fields) {
            if (res.isEmpty()) res = getBasicPattern(f);
            else res = appendField(res, f);
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
        return appendFormat.replace("{0}", pattern).replace("{1}", firstFieldPattern).replace("{2}", fieldDisplayName);
    }

    /**
     * Returns the TR35 appendItem request name for a given field character.
     * 
     * @param fieldChar the field character
     * @return the request name (e.g., "Year", "Month")
     */
    private String getAppendRequestName(char fieldChar) {
        switch(fieldChar) {
            case 'G': return "Era";
            case 'y': case 'Y': case 'u': case 'U': return "Year";
            case 'Q': case 'q': return "Quarter";
            case 'M': case 'L': return "Month";
            case 'w': return "Week";
            case 'W': return "Week";
            case 'd': case 'D': case 'F': case 'g': return "Day";
            case 'E': case 'e': case 'c': return "Day-Of-Week";
            case 'h': case 'H': case 'K': case 'k': return "Hour";
            case 'm': return "Minute";
            case 's': case 'S': case 'A': return "Second";
            case 'z': case 'Z': case 'O': case 'v': case 'V': case 'X': case 'x': return "Timezone";
            default: return String.valueOf(fieldChar);
        }
    }

    /**
     * Returns the CLDR field name key for display name lookup.
     * 
     * @param fieldChar the field character
     * @return the field name key (e.g., "year", "month")
     */
    private String getFieldDisplayNameKey(char fieldChar) {
        switch(fieldChar) {
            case 'G': return "era";
            case 'y': case 'Y': case 'u': case 'U': return "year";
            case 'Q': case 'q': return "quarter";
            case 'M': case 'L': return "month";
            case 'w': return "week";
            case 'W': return "week_of_month";
            case 'd': return "day";
            case 'D': return "day_of_year";
            case 'F': return "day_of_week_in_month";
            case 'g': return "day";
            case 'E': case 'e': case 'c': return "weekday";
            case 'a': case 'b': case 'B': return "dayperiod";
            case 'h': case 'H': case 'K': case 'k': return "hour";
            case 'm': return "minute";
            case 's': case 'S': case 'A': return "second";
            case 'z': case 'Z': case 'O': case 'v': case 'V': case 'X': case 'x': return "zone";
            default: return String.valueOf(fieldChar);
        }
    }

    /**
     * Produces a basic pattern for a single field by searching available formats
     * for a matching or related field of the same length.
     * 
     * @param field the field to generate a pattern for
     * @return a localized pattern string
     */
    private String getBasicPattern(String field) {
        String p = availableFormats.get(field);
        if (p != null) return p;
        for (String avail : availableFormats.keySet()) {
             if (avail.length() == field.length() && areFieldsRelated(avail.charAt(0), field.charAt(0))) {
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skeleton.length(); i++) {
            char c = skeleton.charAt(i);
            if (DATE_FIELDS.indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Extracts time-related fields from a skeleton.
     * 
     * @param skeleton the full skeleton
     * @return a skeleton containing only time fields
     */
    private String getTimeSkeleton(String skeleton) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skeleton.length(); i++) {
            char c = skeleton.charAt(i);
            if (TIME_FIELDS.indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Determines which dateTimeFormat glue pattern to use based on the TR35 "Missing Skeleton Fields" algorithm.
     * The selection is strictly based on the width of the Month and Weekday fields in the date half.
     * 
     * @param dateSkeleton the date portion of the skeleton
     * @return the combined dateTimeFormat pattern
     */
    private String getDateTimePattern(String dateSkeleton) {
        boolean wideMonth = dateSkeleton.contains("MMMM") || dateSkeleton.contains("LLLL");
        boolean weekday = dateSkeleton.contains("EEEE") || dateSkeleton.contains("cccc") || dateSkeleton.contains("E") || dateSkeleton.contains("c");
        boolean abbrMonth = dateSkeleton.contains("MMM") || dateSkeleton.contains("LLL");
        
        if (wideMonth && weekday && (dateSkeleton.contains("EEEE") || dateSkeleton.contains("cccc"))) return dateTimeFormatFull;
        if (wideMonth) return dateTimeFormatLong;
        if (abbrMonth) return dateTimeFormatMedium;
        return dateTimeFormatShort;
    }

    /**
     * Calculates the TR35 distance between a requested skeleton and an available one.
     * 
     * Penalties:
     * - Same field type and length = 0.
     * - Same field type, different length = |req - avail|.
     * - Related field type (e.g. M vs L) = 10 + |req - avail|.
     * - Numeric/Text mismatch (e.g. M vs MMM) = 100 + |req - avail|.
     * - Missing field = 50.
     * - Extra fields in available = rejected (10000).
     * 
     * @param req the requested skeleton
     * @param avail the available skeleton to compare
     * @return the calculated distance
     */
    private int getDistance(String req, String avail) {
        List<String> reqFields = splitSkeleton(req);
        List<String> availFields = splitSkeleton(avail);
        
        Map<Character, String> reqMap = new HashMap<>();
        for (String f : reqFields) reqMap.put(f.charAt(0), f);
        
        Map<Character, String> availMap = new HashMap<>();
        for (String f : availFields) availMap.put(f.charAt(0), f);
        
        int dist = 0;
        for (char c : availMap.keySet()) {
            boolean found = false;
            if (reqMap.containsKey(c)) found = true;
            else {
                for (char rc : reqMap.keySet()) {
                    if (areFieldsRelated(c, rc)) { found = true; break; }
                }
            }
            if (!found) return 10000;
        }
        
        for (String rf : reqFields) {
            char rc = rf.charAt(0);
            String af = availMap.get(rc);
            if (af == null) {
                for (char ac : availMap.keySet()) {
                    if (areFieldsRelated(rc, ac)) { af = availMap.get(ac); break; }
                }
            }
            
            if (af != null) {
                dist += getSingleFieldDistance(af, rf);
            } else {
                dist += 50; 
            }
        }
        return dist;
    }

    /**
     * Calculates the TR35 distance between two individual fields.
     */
    private static int getSingleFieldDistance(String availField, String requestedField) {
        int diff = 0;
        if (availField.charAt(0) != requestedField.charAt(0)) {
            diff += 1;
        }
        boolean isNumeric = isNumeric(availField);
        if (isNumeric != isNumeric(requestedField)) {
            diff += 100;
            return diff;
        }
        if (isNumeric) {
            diff += 2;
            return diff;
        }
        // Normalize E to EEE
        int l1 = Math.max(3, availField.length());
        int l2 = Math.max(3, requestedField.length());
        diff += 3 + Math.abs(l1 - l2);
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
        for (String set : RELATED_FIELD_SETS) {
            if (set.indexOf(a) >= 0 && set.indexOf(b) >= 0) return true;
        }
        return false;
    }

    /**
     * Splits a skeleton into its constituent field strings (e.g., "yMMMd" -> ["y", "MMM", "d"]).
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
     * Adjusts the field lengths and types in a base pattern to match the requested skeleton.
     * 
     * @param reqSkeleton the requested skeleton
     * @param availSkeleton the available skeleton the pattern came from
     * @param pattern the localized pattern to expand
     * @return the expanded pattern
     */
    private String expandPattern(String reqSkeleton, String availSkeleton, String pattern) {
        List<String> reqFields = splitSkeleton(reqSkeleton);
        List<String> availFields = splitSkeleton(availSkeleton);
        
        Map<Character, String> reqMap = new HashMap<>();
        for (String f : reqFields) reqMap.put(f.charAt(0), f);
        
        Map<Character, String> availMap = new HashMap<>();
        for (String f : availFields) availMap.put(f.charAt(0), f);

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
            
            int start = i;
            while (i < pattern.length() && pattern.charAt(i) == c) i++;
            String patField = pattern.substring(start, i);
            
            String reqF = reqMap.get(c);
            if (reqF == null) {
                for (char r : reqMap.keySet()) {
                    if (areFieldsRelated(c, r)) {
                        reqF = reqMap.get(r);
                        break;
                    }
                }
            }
            
            if (reqF != null) {
                String availF = availMap.get(c);
                if (availF == null) {
                    for (char r : availMap.keySet()) {
                        if (areFieldsRelated(c, r)) {
                            availF = availMap.get(r);
                            break;
                        }
                    }
                }
                
                if (availF != null && patField.length() == availF.length()) {
                    if (isNumeric(reqF) == isNumeric(availF)) {
                        char newChar = reqF.charAt(0);
                        for (int k = 0; k < reqF.length(); k++) res.append(newChar);
                    } else {
                        // Prevent substitution between numeric and non-numeric fields
                        res.append(patField);
                    }
                } else {
                    res.append(patField);
                }
            } else {
                res.append(patField);
            }
        }
        return res.toString();
    }

    /**
     * Normalizes a skeleton to the canonical field order.
     * 
     * @param skel the skeleton to normalize
     * @return the canonicalized skeleton
     */
    private String canonicalizeSkeleton(String skel) {
        int[] counts = new int[128];
        for (int i = 0; i < skel.length(); i++) {
            char c = skel.charAt(i);
            if (c < 128) counts[c]++;
        }
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < CANONICAL_ORDER.length(); i++) {
            char c = CANONICAL_ORDER.charAt(i);
            for (int j = 0; j < counts[c]; j++) res.append(c);
        }
        return res.toString();
    }
}
