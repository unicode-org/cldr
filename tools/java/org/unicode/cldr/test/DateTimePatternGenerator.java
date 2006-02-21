/*

 *******************************************************************************

 * Copyright (C) 2002-2006, International Business Machines Corporation and    *

 * others. All Rights Reserved.                                                *

 *******************************************************************************

 *

 * $Source$

 * $Date$

 * $Revision$

 *

 *******************************************************************************

*/
package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.text.MessageFormat;

/**
 * This class provides flexible generation of date format patterns, like "yy-MM-dd". The user can build up the generator
 * by adding successive patterns. Once that is done, a query can be made using a "skeleton", which is a pattern which just
 * includes the desired fields and lengths. The generator will return the "best fit" pattern corresponding to that skeleton.
 * <p>Normally this class is built with data from a particular locale, but one can also be built from scratch.
 * <p>Note that single-field patterns are automatically added, and don't need to be added explicitly.
 * <p>Issue: may be useful to also have a function that returns the fields in a pattern, since we have that internally.
 * @author markdavis
 *
 */
class DateTimePatternGenerator {
    // debugging flags
    //static boolean SHOW_DISTANCE = false;
    
    /** Field numbers, used for AppendItem functions
     */
    static final public int ERA = 0, YEAR = 1, QUARTER = 2, MONTH = 3,
            WEEK_OF_YEAR = 4, WEEK_OF_MONTH = 5, WEEKDAY = 6, DAY = 7,
            DAY_OF_YEAR = 8, DAY_OF_WEEK_IN_MONTH = 9, DAYPERIOD = 10,
            HOUR = 11, MINUTE = 12, SECOND = 13, FRACTIONAL_SECOND = 14,
            ZONE = 15, TYPE_LIMIT = 16;

    /**
     * Return a list of all the skeletons (in canonical form) from this class.
     * 
     * @param an
     *            output Map. If you want to see the intern order being used,
     *            supply a LinkedHashMap.
     * @return the input Map.
     */
    public Map getSkeletons(Map result) {
        for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
            DateTimeMatcher item = (DateTimeMatcher) it.next();
            String pattern = (String) skeleton2pattern.get(item);
            if (CANONICAL_SET.contains(pattern)) continue;
            result.put(item.toString(), pattern);
        }
        return result;
    }
    
    /**
     * A struct used because Java doesn't have output parameters. Used with add(...)
     */
    public static final class PatternInfo { // struct for return information
        public static final int OK = 0, CONFLICT = 1; // status values
        public int status;
        public String conflictingPattern;
    }
    
    /**
     * Adds a pattern to the generator. If the pattern has the same skeleton as an existing pattern,
     * and the override parameter is set, then the previous value is overriden. Otherwise, the previous
     * value is retained. In either case, the conflicting information is returned in PatternInfo.
     * @param override TODO
     */
    public DateTimePatternGenerator add(String pattern, boolean override, PatternInfo returnInfo) {
        DateTimeMatcher matcher = new DateTimeMatcher().set(pattern, fp);
        String previousValue = (String)skeleton2pattern.get(matcher);
        if (previousValue != null) {
            returnInfo.status = PatternInfo.CONFLICT;
            returnInfo.conflictingPattern = previousValue;
            if (!override) return this;
        }
        returnInfo.status = PatternInfo.OK;
        returnInfo.conflictingPattern = "";
        skeleton2pattern.put(matcher, pattern);
        return this;
    }
    
    /**
     * Return the best pattern matching the input skeleton. It is guaranteed to have all of the fields in the skeleton.
     */
    public String getBestPattern(String skeleton) {
        //if (!isComplete) complete();
        current.set(skeleton, fp);
        String best = getBestRaw(current, -1, _distanceInfo);
        if (_distanceInfo.missingFieldMask == 0 && _distanceInfo.extraFieldMask == 0) {
            // we have a good item. Adjust the field types
            return adjustFieldTypes(best, current, false);
        }
        int neededFields = current.getFieldMask();
        // otherwise break up by date and time.
        String datePattern = getBestAppending(neededFields & DATE_MASK);
        String timePattern = getBestAppending(neededFields & TIME_MASK);
        
        
        if (datePattern == null) return timePattern == null ? "" : timePattern;
        if (timePattern == null) return datePattern;
        return MessageFormat.format(getDateTimeFormat(), new Object[]{datePattern, timePattern});
    }

	private String getDateTimeFormat() {
		return dateTimeFormat;
	}
    
    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

	public String getDecimal() {
        return decimal;
    }

    public void setDecimal(String decimal) {
        this.decimal = decimal;
    }

    private String getAppendFormat(int foundMask) {
        return appendItemFormats[foundMask];
	}
    
    private String getAppendName(int foundMask) {
        return "'" + appendItemNames[foundMask] + "'";
    }
    
    /**
     * Return Redundant patterns are those which if removed, make no difference in the resulting getBestPattern values.
     * @param output if not null, stores the redundant patterns that are removed. To get these in internal order,
     * supply a LinkedHashSet.
     * @return a new generator
     */
    public Collection getRedundants(Collection output) {
        for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
            DateTimeMatcher current = (DateTimeMatcher) it.next();
            String pattern = (String) skeleton2pattern.get(current);
            if (CANONICAL_SET.contains(pattern)) continue;
            skipMatcher = current;
            String trial = getBestPattern(current.toString());
            if (trial.equals(pattern)) {
                output.add(pattern);
            }
        }
        if (false) { // ordered
        DateTimePatternGenerator results = new DateTimePatternGenerator();
        PatternInfo pinfo = new PatternInfo();
        for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
            DateTimeMatcher current = (DateTimeMatcher) it.next();
            String pattern = (String) skeleton2pattern.get(current);
            if (CANONICAL_SET.contains(pattern)) continue;
            //skipMatcher = current;
            String trial = results.getBestPattern(current.toString());
            if (trial.equals(pattern)) {
                output.add(pattern);
            } else {
                results.add(pattern, false, pinfo);
            }
        }
        }
        return output;
    }
    
    /**
     * Adjusts the field types (width and subtype) according to the skeleton string. That is, if you supply a pattern
     * like "d-M H:m", and a skeleton of "MMMMddhhmm", then the input pattern is adjusted to be "dd-MMMM hh:mm".
     * @param pattern
     * @param skeleton
     * @return
     */
    public String replaceFieldTypes(String pattern, String skeleton) {
        return adjustFieldTypes(pattern, current.set(skeleton, fp), false);
    }
    
    public String getAppendItemFormats(int field) {
        return appendItemFormats[field];
    }

    public void setAppendItemFormats(int field, String value) {
       appendItemFormats[field] = value;
    }

    public String getAppendItemNames(int field) {
        return appendItemNames[field];
    }

    public void setAppendItemNames(int field, String value) {
        appendItemNames[field] = value;
    }
    // ========= PRIVATES ============
    
    private Map skeleton2pattern = new TreeMap(); // items are in priority order
    private String decimal = "?";
    private String dateTimeFormat = "{0} {1}";
    private String[] appendItemFormats = new String[TYPE_LIMIT];
    private String[] appendItemNames = new String[TYPE_LIMIT];
    {
        for (int i = 0; i < TYPE_LIMIT; ++i) {
            appendItemFormats[i] = "{0} \u251C{2}: {1}\u2524";
            appendItemNames[i] = "F" + i;
        }
    }

    private transient DateTimeMatcher current = new DateTimeMatcher();
    private transient FormatParser fp = new FormatParser();
    private transient DistanceInfo _distanceInfo = new DistanceInfo();
    private transient boolean isComplete = false;
    private transient DateTimeMatcher skipMatcher = null;
    
    private static final int FRACTIONAL_MASK = 1<<FRACTIONAL_SECOND;
    private static final int SECOND_AND_FRACTIONAL_MASK = (1<<SECOND) | (1<<FRACTIONAL_SECOND);
    /**
     * We only get called here if we failed to find an exact skeleton. We have broken it into date + time, and look for the pieces.
     * If we fail to find a complete skeleton, we compose in a loop until we have all the fields.
     */
    private String getBestAppending(int missingFields) {
        String resultPattern = null;
        if (missingFields != 0) {
            resultPattern = getBestRaw(current, missingFields, _distanceInfo);
            resultPattern = adjustFieldTypes(resultPattern, current, false);
           
            while (_distanceInfo.missingFieldMask != 0) { // precondition: EVERY single field must work!
                
                // special hack for SSS. If we are missing SSS, and we had ss but found it, replace the s field according to the 
                // number separator
                if ((_distanceInfo.missingFieldMask & SECOND_AND_FRACTIONAL_MASK) == FRACTIONAL_MASK
                        && (missingFields & SECOND_AND_FRACTIONAL_MASK) == SECOND_AND_FRACTIONAL_MASK) {
                    resultPattern = adjustFieldTypes(resultPattern, current, true);
                    _distanceInfo.missingFieldMask &= ~FRACTIONAL_MASK; // remove bit
                    continue;
                }

                int startingMask = _distanceInfo.missingFieldMask;
                String temp = getBestRaw(current, _distanceInfo.missingFieldMask, _distanceInfo);
                temp = adjustFieldTypes(temp, current, false);
                int foundMask = startingMask & ~_distanceInfo.missingFieldMask;
                int topField = getTopBitNumber(foundMask);
                resultPattern = MessageFormat.format(getAppendFormat(topField), new Object[]{resultPattern, temp, getAppendName(topField)});
            }
        }
        return resultPattern;
    }
    
    /**
     * @param current2
     * @return
     */
    private String adjustSeconds(DateTimeMatcher current2) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param foundMask
     * @return
     */
    private int getTopBitNumber(int foundMask) {
        int i = 0;
        while (foundMask != 0) {
            foundMask >>>= 1;
            ++i;
        }
        return i-1;
    }

    /**
     * 
     */
    private void complete() {
        PatternInfo patternInfo = new PatternInfo();
        // make sure that every valid field occurs once, with a "default" length
        for (int i = 0; i < CANONICAL_ITEMS.length; ++i) {
            char c = (char)types[i][0];
            add(String.valueOf(CANONICAL_ITEMS[i]), false, patternInfo);
        }
        isComplete = true;
    }
    {
        complete();
    }
    
    /**
     * 
     */
    private String getBestRaw(DateTimeMatcher source, int includeMask, DistanceInfo missingFields) {
//        if (SHOW_DISTANCE) System.out.println("Searching for: " + source.pattern 
//                + ", mask: " + showMask(includeMask));
        int bestDistance = Integer.MAX_VALUE;
        String bestPattern = "";
        DistanceInfo tempInfo = new DistanceInfo();
        for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
            DateTimeMatcher trial = (DateTimeMatcher) it.next();
            if (trial.equals(skipMatcher)) continue;
            int distance = source.getDistance(trial, includeMask, tempInfo);
//            if (SHOW_DISTANCE) System.out.println("\tDistance: " + trial.pattern + ":\t" 
//                    + distance + ",\tmissing fields: " + tempInfo);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPattern = (String) skeleton2pattern.get(trial);
                missingFields.setTo(tempInfo);
                if (distance == 0) break;
            }
        }
        return bestPattern;
    }
    
    /**
     * @param fixFractionalSeconds TODO
     * 
     */
    private String adjustFieldTypes(String pattern, DateTimeMatcher inputRequest, boolean fixFractionalSeconds) {
        fp.set(pattern);
        StringBuffer newPattern = new StringBuffer();
        for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
            Object item = it.next();
            if (item instanceof String) {
                newPattern.append((String)item);
            } else {
                String field = ((VariableField) item).string;
                int canonicalIndex = getCanonicalIndex(field);
                int type = types[canonicalIndex][1];
                if (fixFractionalSeconds && type == SECOND) {
                    String newField = inputRequest.original[FRACTIONAL_SECOND];
                    field = field + decimal + newField;
                } else if (inputRequest.type[type] != 0) {
                    String newField = inputRequest.original[type];
                    // normally we just replace the field. However HOUR is special; we only change the length
                    if (type != HOUR) {
                        field = newField;
                    } else if (field.length() != newField.length()){
                        char c = field.charAt(0);
                        field = "";
                        for (int i = newField.length(); i > 0; --i) field += c;
                    }
                }
                newPattern.append(field);
            }
        }
        //if (SHOW_DISTANCE) System.out.println("\tRaw: " + pattern);
        return newPattern.toString();
    }
    
    String getFields(String pattern) {
        fp.set(pattern);
        StringBuffer newPattern = new StringBuffer();
        for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
            Object item = it.next();
            if (item instanceof String) {
                newPattern.append((String)item);
            } else {
                newPattern.append("{" + getName(item.toString()) + "}");
            }
        }
        return newPattern.toString();
    }
    
    static String showMask(int mask) {
        String result = "";
        for (int i = 0; i < TYPE_LIMIT; ++i) {
            if ((mask & (1<<i)) == 0) continue;
            if (result.length() != 0) result += " | ";
            result += FIELD_NAME[i] + " ";
        }
        return result;
    }
    
    static private String[] FIELD_NAME = {
            "Era", "Year", "Quarter", "Month", "Week_in_Year", "Week_in_Month", "Weekday", 
            "Day", "Day_Of_Year", "Day_of_Week_in_Month", "Dayperiod", 
            "Hour", "Minute", "Second", "Fractional_Second", "Zone"
    };
    
    static private String[] CANONICAL_ITEMS = {
        "G", "y", "Q", "M", "w", "W", "e", 
        "d", "D", "F", 
        "H", "m", "s", "S", "v"
    };
    
    static private Set CANONICAL_SET = new HashSet(Arrays.asList(CANONICAL_ITEMS));
    
    static final private int 
    DATE_MASK = (1<<DAYPERIOD) - 1,
    TIME_MASK = (1<<TYPE_LIMIT) - 1 - DATE_MASK;
    
    static final private int // numbers are chosen to express 'distance'
    DELTA = 0x10,
    NUMERIC = 0x100,
    NONE = 0,
    NARROW = -0x100,
    SHORT = -0x101,
    LONG = -0x102,
    EXTRA_FIELD =   0x10000,
    MISSING_FIELD = 0x1000;

    
    static private String getName(String s) {
        int i = getCanonicalIndex(s);
        String name = FIELD_NAME[types[i][1]];
        int subtype = types[i][2];
        boolean string = subtype < 0;
        if (string) subtype = -subtype;
        if (subtype < 0) name += ":S";
        else name += ":N";
        return name;
    }
    
    static private int getCanonicalIndex(String s) {
        int len = s.length();
        int ch = s.charAt(0);
        for (int i = 0; i < types.length; ++i) {
            int[] row = types[i];
            if (row[0] != ch) continue;
            if (row[3] > len) continue;
            if (row[row.length-1] < len) continue;
            return i;
        }
        return -1;
    }
    
    static private int[][] types = {
            // the order here makes a difference only when searching for single field.
        // format is:
        // pattern character, main type, weight, min length, weight
            {'G', ERA, SHORT, 1, 3},
            {'G', ERA, LONG, 4},
            
            {'y', YEAR, NUMERIC, 1, 20},
            {'Y', YEAR, NUMERIC + DELTA, 1, 20},
            {'u', YEAR, NUMERIC + 2*DELTA, 1, 20},

            {'Q', QUARTER, NUMERIC, 1, 2},
            {'Q', QUARTER, SHORT, 3},
            {'Q', QUARTER, LONG, 4},

            {'M', MONTH, NUMERIC, 1, 2},
            {'M', MONTH, SHORT, 3},
            {'M', MONTH, LONG, 4},
            {'M', MONTH, NARROW, 5},
            {'L', MONTH, NUMERIC + DELTA, 1, 2},
            {'L', MONTH, SHORT - DELTA, 3},
            {'L', MONTH, LONG - DELTA, 4},
            {'L', MONTH, NARROW - DELTA, 5},
            
            {'w', WEEK_OF_YEAR, NUMERIC, 1, 2},
            {'W', WEEK_OF_MONTH, NUMERIC + DELTA, 1},
            
            {'e', WEEKDAY, NUMERIC + DELTA, 1, 2},
            {'e', WEEKDAY, SHORT - DELTA, 3},
            {'e', WEEKDAY, LONG - DELTA, 4},
            {'e', WEEKDAY, NARROW - DELTA, 5},
            {'E', WEEKDAY, SHORT, 1, 3},
            {'E', WEEKDAY, LONG, 4},
            {'E', WEEKDAY, NARROW, 5},
            {'c', WEEKDAY, NUMERIC + 2*DELTA, 1, 2},
            {'c', WEEKDAY, SHORT - 2*DELTA, 3},
            {'c', WEEKDAY, LONG - 2*DELTA, 4},
            {'c', WEEKDAY, NARROW - 2*DELTA, 5},
            
            {'d', DAY, NUMERIC, 1, 2},
            {'D', DAY_OF_YEAR, NUMERIC + DELTA, 1, 3},
            {'F', DAY_OF_WEEK_IN_MONTH, NUMERIC + 2*DELTA, 1},
            {'g', DAY, NUMERIC + 3*DELTA, 1, 20}, // really internal use, so we don't care
            
            {'a', DAYPERIOD, SHORT, 1},
            
            {'H', HOUR, NUMERIC + 10*DELTA, 1, 2}, // 24 hour
            {'k', HOUR, NUMERIC + 11*DELTA, 1, 2},
            {'h', HOUR, NUMERIC, 1, 2}, // 12 hour
            {'K', HOUR, NUMERIC + DELTA, 1, 2},
            
            {'m', MINUTE, NUMERIC, 1, 2},
            
            {'s', SECOND, NUMERIC, 1, 2},
            {'S', FRACTIONAL_SECOND, NUMERIC + DELTA, 1, 1000},
            {'A', SECOND, NUMERIC + 2*DELTA, 1, 1000},
            
            {'v', ZONE, SHORT - 2*DELTA, 1},
            {'v', ZONE, LONG - 2*DELTA, 4},
            {'z', ZONE, SHORT, 1, 3},
            {'z', ZONE, LONG, 4},
            {'Z', ZONE, SHORT - DELTA, 1, 3},
            {'Z', ZONE, LONG - DELTA, 4},
    };
    
    private static class DateTimeMatcher implements Comparable {
        //private String pattern = null;
        private int[] type = new int[TYPE_LIMIT];
        private String[] original = new String[TYPE_LIMIT];

        // just for testing; fix to make multi-threaded later
        // private static FormatParser fp = new FormatParser();
        
        public String toString() {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < TYPE_LIMIT; ++i) {
                if (original[i].length() != 0) result.append(original[i]);
            }
            return result.toString();
        }
        
        DateTimeMatcher set(String pattern, FormatParser fp) {
            for (int i = 0; i < TYPE_LIMIT; ++i) {
                type[i] = NONE;
                original[i] = "";
            }
            fp.set(pattern);
            for (Iterator it = fp.getFields(new ArrayList()).iterator(); it.hasNext();) {
                String field = (String) it.next();
                if (field.charAt(0) == 'a') continue; // skip day period, special cass
                int canonicalIndex = getCanonicalIndex(field);
                if (canonicalIndex < 0) {
                    throw new IllegalArgumentException("Illegal field:\t"
                            + field + "\t in " + pattern);
                }
                int[] row = types[canonicalIndex];
                int typeValue = row[1];
                if (original[typeValue].length() != 0) {
                    throw new IllegalArgumentException("Conflicting fields:\t"
                            + original[typeValue] + ", " + field + "\t in " + pattern);
                }
                original[typeValue] = field;
                int subTypeValue = row[2];
                if (subTypeValue > 0) subTypeValue += field.length();
                type[typeValue] = (byte) subTypeValue;
            }
            return this;
        }
        
        /**
         * 
         */
        public int getFieldMask() {
            int result = 0;
            for (int i = 0; i < type.length; ++i) {
                if (type[i] != 0) result |= (1<<i);
            }
            return result;
        }
        
        /**
         * 
         */
        public void extractFrom(DateTimeMatcher source, int fieldMask) {
            for (int i = 0; i < type.length; ++i) {
                if ((fieldMask & (1<<i)) != 0) {
                    type[i] = source.type[i];
                    original[i] = source.original[i];
                } else {
                    type[i] = NONE;
                    original[i] = "";
                }
            }
        }
        
        int getDistance(DateTimeMatcher other, int includeMask, DistanceInfo distanceInfo) {
            int result = 0;
            distanceInfo.clear();
            for (int i = 0; i < type.length; ++i) {
                int myType = (includeMask & (1<<i)) == 0 ? 0 : type[i];
                int otherType = other.type[i];
                if (myType == otherType) continue; // identical (maybe both zero) add 0
                if (myType == 0) { // and other is not
                    result += EXTRA_FIELD;
                    distanceInfo.addExtra(i);
                } else if (otherType == 0) { // and mine is not
                    result += MISSING_FIELD;
                    distanceInfo.addMissing(i);
                } else {
                    result += Math.abs(myType - otherType); // square of mismatch
                }
            }
            return result;
        }

        public int compareTo(Object o) {
            DateTimeMatcher that = (DateTimeMatcher) o;
            for (int i = 0; i < original.length; ++i) {
                int comp = original[i].compareTo(that.original[i]);
                if (comp != 0) return -comp;
            }
            return 0;
        }       

        public boolean equals(Object other) {
            if (other == null) return false;
            DateTimeMatcher that = (DateTimeMatcher) other;
            for (int i = 0; i < original.length; ++i) {
                if (!original[i].equals(that.original[i])) return false;
            }
            return true;
        }       
        public int hashCode() {
            int result = 0;
            for (int i = 0; i < original.length; ++i) {
                result ^= original[i].hashCode();
            }
            return result;
        }       
    }
    
    private static class DistanceInfo {
        int missingFieldMask;
        int extraFieldMask;
        void clear() {
            missingFieldMask = extraFieldMask = 0;
        }
        /**
         * 
         */
        public void setTo(DistanceInfo other) {
            missingFieldMask = other.missingFieldMask;
            extraFieldMask = other.extraFieldMask;
        }
        void addMissing(int field) {
            missingFieldMask |= (1<<field);
        }
        void addExtra(int field) {
            extraFieldMask |= (1<<field);
        }
        public String toString() {
            return "missingFieldMask: " + DateTimePatternGenerator.showMask(missingFieldMask)
            + ", extraFieldMask: " + DateTimePatternGenerator.showMask(extraFieldMask);
        }
    }

    static class FormatParser {
        private List items = new ArrayList();
        private char quoteChar = '\'';
        
        FormatParser set(String string) {
            items.clear();
            if (string.length() == 0) return this;
            //int start = 1;
            int lastPos = 0;
            char last = string.charAt(lastPos);
            boolean lastIsVar = isVariableField(last);
            boolean inQuote = last == quoteChar;
            // accumulate any sequence of unquoted ASCII letters as a variable
            // anything else as a string (with quotes retained)
            for (int i = 1; i < string.length(); ++i) {
                char ch = string.charAt(i);
                if (ch == quoteChar) {
                    inQuote = !inQuote;
                }
                boolean chIsVar = !inQuote && isVariableField(ch);
                // break between ASCII letter and any non-equal letter
                if (ch == last && lastIsVar == chIsVar) continue;
                String part = string.substring(lastPos, i);
                if (lastIsVar) {
                    items.add(new VariableField(part));
                } else {
                    items.add(part);
                }
                lastPos = i;
                last = ch;
                lastIsVar = chIsVar;
            }
            String part = string.substring(lastPos, string.length());
            if (lastIsVar) {
                items.add(new VariableField(part));
            } else {
                items.add(part);
            }
            return this;
        }
        /**
         * @param output
         * @return
         */
        public Collection getFields(Collection output) {
            if (output == null) output = new TreeSet();
            main:
                for (Iterator it = items.iterator(); it.hasNext();) {
                    Object item = it.next();
                    if (item instanceof VariableField) {
                        String s = item.toString();
                        switch(s.charAt(0)) {
                        //case 'Q': continue main; // HACK
                        case 'a': continue main; // remove
                        }
                        output.add(s);
                    }
                }
            //System.out.println(output);
            return output;
        }
        /**
         * @return
         */
        public String getFieldString() {
            Set set = (Set)getFields(null);
            StringBuffer result = new StringBuffer();
            for (Iterator it = set.iterator(); it.hasNext();) {
                String item = (String) it.next();
                result.append(item);
            }
            return result.toString();
        }
        /**
         * @param last
         * @return
         */
        private boolean isVariableField(char last) {
            return last <= 'z' && last >= 'A' && (last >= 'a' || last <= 'Z');
        }
        public List getItems() {
            return Collections.unmodifiableList(items);
        }
    }
    
    static class VariableField {
        String string;
        VariableField(String string) {
            this.string = string;
        }
        public String toString() {
            return string;
        }
    }

}