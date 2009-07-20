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
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.PatternInfo;

/**
 * Temporary class while refactoring.
 * @author markdavis
 *
 */
class FlexibleDateFromCLDR {
    DateTimePatternGenerator gen = DateTimePatternGenerator.getEmptyInstance();
    transient XPathParts parts = new XPathParts(null, null);
    private transient ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
            
    static List tests = Arrays.asList(new String[]{
            
            "HHmmssSSSvvvv", // 'complete' time
            "HHmm",
            "HHmmvvvv",
            "HHmmss",
            "HHmmssSSSSS",
            "HHmmssvvvv",

            "MMMd",
            "Md",

            "YYYYD", // (maybe?)

            "yyyyww",
            "yyyywwEEE",

            "yyyyQQQQ",
            "yyyyMM",

            "yyyyMd",
            "yyyyMMMd",
            "yyyyMMMEEEd",

            "GyyyyMMMd",
            "GyyyyMMMEEEd", // 'complete' date 
            
            "YYYYwEEE", // year, week of year, weekday
            "yyyyDD", // year, day of year
            "yyyyMMFE", // year, month, nth day of week in month
            // misc
            "eG", "dMMy", "GHHmm", "yyyyHHmm", "Kmm", "kmm",
            "MMdd", "ddHH", "yyyyMMMd", "yyyyMMddHHmmss",
            "GEEEEyyyyMMddHHmmss",
            "GuuuuQMMMMwwWddDDDFEEEEaHHmmssSSSvvvv", // bizarre case just for testing
            });

    public void set(CLDRFile cldrFile) {
        icuServiceBuilder.setCldrFile(cldrFile);
        gen = DateTimePatternGenerator.getEmptyInstance(); // for now
        failureMap.clear();
    }
    /**
     * 
     */
    public void showFlexibles() {
        Map items = (Map)gen.getSkeletons(new LinkedHashMap());
        System.out.println("ERRORS");
        for (Iterator it = failureMap.keySet().iterator(); it.hasNext();) {
            Object item = it.next();
            Object value = failureMap.get(item);
            System.out.println("\t" + value);
        }
        for (int i = 0; i < DateTimePatternGenerator.TYPE_LIMIT; ++i) {
            String format = gen.getAppendItemFormat(i);
            if (format.indexOf('\u251C') >= 0) {
                System.out.println("\tMissing AppendItem format:\t" + DISPLAY_NAME_MAP[i]);
            }
            if (i == DateTimePatternGenerator.FRACTIONAL_SECOND) continue; // don't need this field
            String name = gen.getAppendItemName(i);
            if (name.matches("F[0-9]+")) {
                System.out.println("\tMissing Field Name:\t" + DISPLAY_NAME_MAP[i]);
            }
        }
        System.out.println("SKELETON\t=> PATTERN LIST");
        for (Iterator it = items.keySet().iterator(); it.hasNext();) {
            Object skeleton = it.next();
            System.out.println("\t\"" + skeleton + "\"\t=>\t\"" + items.get(skeleton) + "\"");
        }
        System.out.println("REDUNDANTS");
        Collection redundants = gen.getRedundants(new ArrayList());
        for (Iterator it = redundants.iterator(); it.hasNext();) {
            Object item = it.next();
            System.out.println("\t" + item);
        }
        System.out.println("TESTS");
        for (Iterator it = tests.iterator(); it.hasNext();) {
            String item = (String) it.next();
            try {
                String pat = gen.getBestPattern(item);
                String sample = "<can't format>";
                try {
                    DateFormat df = icuServiceBuilder.getDateFormat("gregorian", pat);
                    sample = df.format(new Date());
                } catch (RuntimeException e) {}
                System.out.println("\t\"" + item + "\"\t=>\t\"" + pat +  "\"\t=>\t\"" + sample + "\"");
            } catch (RuntimeException e) {
                System.out.println(e.getMessage()); // e.printStackTrace();
            }
        }
        System.out.println("END");
    }

    Map failureMap = new TreeMap();
    
    /**
     * @param path
     * @param value
     * @param fullPath
     */
    public void checkFlexibles(String path, String value, String fullPath) {
        if (path.indexOf("numbers/symbols/decimal") >= 0) {
            gen.setDecimal(value);
            return;
        }
        if (path.indexOf("gregorian") < 0) return;
        if (path.indexOf("/appendItem") >= 0) {
            String key = (String) parts.set(path).getAttributeValue(-1, "request");
            try {
                gen.setAppendItemFormat(getIndex(key, APPEND_ITEM_NAME_MAP), value);
            } catch (RuntimeException e) {
                failureMap.put(path, "\tWarning: can't set AppendItemFormat:\t" + key + ":\t" + value);
            }
            return;
        }
        if (path.indexOf("/fields") >= 0) {
            String key = (String) parts.set(path).getAttributeValue(-2, "type");
            try {
                gen.setAppendItemName(getIndex(key, DISPLAY_NAME_MAP), value);
            } catch (RuntimeException e) {
                failureMap.put(path, "\tWarning: can't set AppendItemName:\t" + key + ":\t" + value);
            }
            return;
        }

        if (path.indexOf("pattern") < 0 && path.indexOf("dateFormatItem") < 0) return;
        // set the am/pm preference
        if (path.indexOf("timeFormatLength[@type=\"short\"]") >= 0) {
        	fp.set(value);
        	for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
        		Object item = it.next();
        		if (item instanceof DateTimePatternGenerator.VariableField) {
        			if (item.toString().charAt(0) == 'h') {
        				isPreferred12Hour = true;
        			}
        		}
        	}
        }
        if (path.indexOf("dateTimeFormatLength") > 0) return; // exclude {1} {0}
        // add to generator
        try {
            gen.addPattern(value, false, patternInfo);
            switch (patternInfo.status) {
            case PatternInfo.CONFLICT:
                    failureMap.put(path, "Conflicting Patterns: \"" + value + "\"\t&\t\"" + patternInfo.conflictingPattern + "\"");
                    break;
            }
        } catch (RuntimeException e) {
            failureMap.put(path, e.getMessage());
        }
    }
	DateTimePatternGenerator.FormatParser fp = new DateTimePatternGenerator.FormatParser();

    boolean isPreferred12Hour = false;

    static private String[] DISPLAY_NAME_MAP = {
      "era", "year", "quarter", "month", "week", "week_in_month", "weekday", 
      "day", "day_of_year", "day_of_week_in_month", "dayperiod", 
      "hour", "minute", "second", "fractional_second", "zone", "-"
    };

    static private String[] APPEND_ITEM_NAME_MAP_OLD = {
      "G", "y", "Q", "M", "w", "W", "e", 
      "d", "D", "F", "a", 
      "h", "m", "s", "S", "v", "-"
    };
    
    static private String[] APPEND_ITEM_NAME_MAP = {
      "Era", "Year", "Quarter", "Month", "Week", "Week", "Day-Of-Week", 
      "Day", "Day", "Day-Of-Week", "-", 
      "Hour", "Minute", "Second", "-", "Timezone", "-"
    };

    int getIndex(String s, String[] strings) {
      for (int i = 0; i < strings.length; ++i) {
        if (s.equals(strings[i])) return i;
      }
      return -1;
    }
    
    PatternInfo patternInfo = new PatternInfo();

    public Collection getRedundants(Collection output) {
        return gen.getRedundants(output);
    }
    public Object getFailurePath(Object path) {
        return failureMap.get(path);
    }
	public boolean preferred12Hour() {
		// TODO Auto-generated method stub
		return isPreferred12Hour;
	}
}