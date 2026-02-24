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
     * The canonical order of date/time fields as defined by the LDML specification.
     * Skeletons are normalized to this order to ensure consistent matching.
     */
    private static final String CANONICAL_ORDER = "GyYruUQqMLwWEecdDFgabBhHKkmsSAzZOvVXx";

    public CldrDateTimePatternGenerator(CLDRFile file, String calendarID, boolean useStock) {
        this.file = file;
        this.calendarID = calendarID;
        this.useStock = useStock;
        init();
    }

    private void init() {
        // TR35 says the default hour cycle is derived from the short time pattern.
        // We look it up and check the first hour character we find.
        String shortTimePath = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String shortTimePattern = getStringValueWithFallback(shortTimePath);
        if (shortTimePattern != null) {
            for (int i = 0; i < shortTimePattern.length(); i++) {
                char c = shortTimePattern.charAt(i);
                if (c == 'h' || c == 'H' || c == 'k' || c == 'K') {
                    defaultHourFormatChar = c;
                    break;
                }
            }
        }

        if (useStock) {
            for (String stock : STOCK) {
                String dPath = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateFormats/dateFormatLength[@type=\"" + stock + "\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String tPath = "//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/timeFormats/timeFormatLength[@type=\"" + stock + "\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                
                String dp = getStringValueWithFallback(dPath);
                if (dp != null) availableFormats.put(getSkeletonFromPattern(dp), dp);
                
                String tp = getStringValueWithFallback(tPath);
                if (tp != null) availableFormats.put(getSkeletonFromPattern(tp), tp);
            }
        }

        Set<String> allIds = new LinkedHashSet<>();
        Set<String> allAppendRequests = new LinkedHashSet<>();
        
        for (String path : file.fullIterable()) {
            if (path.contains("/availableFormats/dateFormatItem")) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String id = parts.getAttributeValue(-1, "id");
                if (id != null) allIds.add(id);
            } else if (path.contains("/appendItems/appendItem")) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String request = parts.getAttributeValue(-1, "request");
                if (request != null) allAppendRequests.add(request);
            } else if (path.contains("/dates/fields/field") && path.contains("displayName")) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String type = parts.getAttributeValue(-2, "type");
                String value = file.getStringValueWithBailey(path);
                if (type != null && value != null) {
                    fieldNames.put(type, value);
                }
            }
        }

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

        dateTimeFormatFull = getStringValueWithFallbackOrDefault("//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/dateTimeFormatLength[@type=\"full\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]", dateTimeFormatFull);
        dateTimeFormatLong = getStringValueWithFallbackOrDefault("//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/dateTimeFormatLength[@type=\"long\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]", dateTimeFormatLong);
        dateTimeFormatMedium = getStringValueWithFallbackOrDefault("//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/dateTimeFormatLength[@type=\"medium\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]", dateTimeFormatMedium);
        dateTimeFormatShort = getStringValueWithFallbackOrDefault("//ldml/dates/calendars/calendar[@type=\"" + calendarID + "\"]/dateTimeFormats/dateTimeFormatLength[@type=\"short\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]", dateTimeFormatShort);
    }

    /**
     * Resolves values from the CLDRFile, strictly handling the TR35 rule that 
     * non-Gregorian calendars fallback to Gregorian values if missing locally 
     * or at the root aliasing levels.
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

    private String getStringValueWithFallbackOrDefault(String path, String defaultValue) {
        String val = getStringValueWithFallback(path);
        return val != null ? val : defaultValue;
    }

    public char getDefaultHourFormatChar() {
        return defaultHourFormatChar;
    }

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
        
        if (calendarID.equals("buddhist") && file.getLocaleID().startsWith("th") && canonicalSkeleton.equals("yyMEEEd")) {
            System.err.println("DEBUG: th buddhist avail formats: " + availableFormats.keySet());
        }
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

    private String getBasicPattern(String field) {
        String p = availableFormats.get(field);
        if (p != null) return p;
        for (String avail : availableFormats.keySet()) {
             if (avail.length() == field.length() && areFieldsRelated(avail.charAt(0), field.charAt(0))) {
                 return expandPattern(field, avail, availableFormats.get(avail));
             }
        }
        return field;
    }

    private String getDateSkeleton(String skeleton) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skeleton.length(); i++) {
            char c = skeleton.charAt(i);
            if ("GyYruUQqMLwWEdDFg".indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String getTimeSkeleton(String skeleton) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skeleton.length(); i++) {
            char c = skeleton.charAt(i);
            if ("aBhHkKmmsSAzZOvVXx".indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Determines which dateTimeFormat glue pattern to use based on the TR35 "Missing Skeleton Fields" algorithm.
     * The selection is strictly based on the width of the Month and Weekday fields in the date half.
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
     * TR35 matching penalty metric:
     * - Same field type and length = 0 penalty.
     * - Same field type, different length = penalty of |req - avail|.
     * - Related field type (e.g. M vs L) = penalty of 10 + |req - avail|.
     * - Missing field = massive penalty (20 per field).
     * - Extra fields in available = rejected entirely (returns 10000).
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
                if (rc == af.charAt(0)) {
                    dist += Math.abs(rf.length() - af.length());
                } else {
                    dist += 10 + Math.abs(rf.length() - af.length());
                }
            } else {
                dist += 20; 
            }
        }
        return dist;
    }
    
    /**
     * Checks if two field characters represent the same semantic field (e.g., M and L for Month).
     */
    private boolean areFieldsRelated(char a, char b) {
        if (a == b) return true;
        String[] sets = {
            "yYruU", "ML", "wW", "dDFg", "Eec", "abB", "hHKk", "sSA", "zZOvVXx"
        };
        for (String set : sets) {
            if (set.indexOf(a) >= 0 && set.indexOf(b) >= 0) return true;
        }
        return false;
    }

    /**
     * Splits a skeleton into its constituent fields (e.g., "yMMMd" -> ["y", "MMM", "d"]).
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
     * Adjusts the field lengths in the base pattern to match the requested skeleton.
     * 
     * For example, if the request is "yyyy" and the best match is "y" (pattern "y"),
     * it expands the pattern to "yyyy". It also handles related fields like M vs L.
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
                    char newChar = reqF.charAt(0);
                    for (int k = 0; k < reqF.length(); k++) res.append(newChar);
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

    /**
     * Extracts a canonical skeleton from a date/time pattern string.
     */
    private String getSkeletonFromPattern(String pattern) {
        StringBuilder skel = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && Character.isLetter(c)) {
                skel.append(c);
            }
        }
        return canonicalizeSkeleton(skel.toString());
    }
}
