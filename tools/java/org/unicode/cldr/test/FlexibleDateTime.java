/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * Test class for trying different approaches to flexible date/time.
 * Internal Use.
 * Once we figure out what approach to take, this should turn into the test file
 * for the data.
 */
public class FlexibleDateTime {
    static final boolean DEBUG = false;
    static boolean SHOW_MATCHING = false;
    static boolean SHOW2 = false;
    static boolean SHOW_DISTANCE = false;
    static boolean SHOW_OO = false;
    static String SEPARATOR = "\r\n\t";
    
    /**
     * Test different ways of doing flexible date/times.
     * Internal Use.
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        if (false) { // just for testing simple cases
            DateTimeMatcher a = new DateTimeMatcher().set("HH:mm");
            DateTimeMatcher b = new DateTimeMatcher().set("kkmm");
            DistanceInfo missingFields = new DistanceInfo();
            int distance = a.getDistance(b, -1, missingFields);
        }
        generate(args);
        //test(args);
    }

    public static PrintWriter log;
    
	private static void generate(String[] args) throws IOException {
		log = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY + "/flex/", "log.txt");
        String filter = ".*";
        if (args.length > 0)
            filter = args[0];
        
        Factory cldrFactory = Factory.make(Utility.BASE_DIRECTORY
                + "open_office/main/", filter);
        Factory mainCLDRFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
        FormatParser fp = new FormatParser();
        // fix locale list
        Collection ooLocales = new LinkedHashSet(cldrFactory.getAvailable());
        ooLocales.remove("nb_NO"); // hack, since no_NO is the main one, and subsumes nb
        Map localeMap = new LocaleIDFixer().fixLocales(ooLocales, new TreeMap());
        //pw.println(localeMap);

        for (Iterator it = localeMap.keySet().iterator(); it.hasNext();) {
            String sourceLocale = (String) it.next();
            String targetLocale = (String) localeMap.get(sourceLocale);
            ULocale uSourceLocale = new ULocale(sourceLocale);
            ULocale uTargetLocale = new ULocale(targetLocale);
            log.println();
            log.println(uTargetLocale.getDisplayName(ULocale.ENGLISH) + " (" + uTargetLocale + ")");
            System.out.println(sourceLocale + "\t\u2192" + uTargetLocale.getDisplayName(ULocale.ENGLISH) + " (" + uTargetLocale + ")");
            if (!sourceLocale.equals(targetLocale)) {
            	log.println("[oo: " + uSourceLocale.getDisplayName(ULocale.ENGLISH) + " (" + sourceLocale + ")]");
            }
            Collection list = getOOData(cldrFactory, sourceLocale);
            // get the current values
            try {
                Collection currentList = getDateFormats(mainCLDRFactory, targetLocale);
                list.removeAll(currentList);
            } catch (RuntimeException e) {
                // ignore
            }
            
            if (list.size() == 0) {
            	log.println(sourceLocale + "\tEMPTY!"); // skip empty
            	continue;
            }
            CLDRFile temp = CLDRFile.make(targetLocale);
            String prefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@_q=\"";

            int count = 0;
            Map previousID = new HashMap();
            for (Iterator it2 = list.iterator(); it2.hasNext();) {
            	String pattern = (String) it2.next();
            	new SimpleDateFormat(pattern); // check that compiles
            	fp.set(pattern);
            	String id = fp.getFieldString();
            	if (!allowedDateTimeCharacters.containsAll(id)) throw new IllegalArgumentException("Illegal characters in: " + pattern);
            	if (id.length() == 0) {
                    throw new IllegalArgumentException("Empty id for: " + pattern);
                }
                String previous = (String) previousID.get(id);
                if (previous != null) {
                    log.println("Skipping Duplicate pattern: " + pattern + " (already have " + previous + ")");
                    continue;
                } else {
                    previousID.put(id, pattern);
                }
            	String path = prefix + (count++) + "\"]";
            	temp.add(path, pattern);
            }
    		PrintWriter pw = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY + "/flex/", targetLocale + ".xml");
            temp.write(pw);
            pw.close();
            log.flush();
        }
        System.out.println("done");
        log.close();
	}

	private static Collection getDateFormats(Factory mainCLDRFactory, String targetLocale) {
		List result = new ArrayList();
		XPathParts parts = new XPathParts(null, null);
        CLDRFile currentFile = null;
        String oldTargetLocale = targetLocale;
        // do fallback
        do {
			try {
				currentFile = mainCLDRFactory.make(targetLocale, true);
			} catch (RuntimeException e) {
				targetLocale = LocaleIDParser.getParent(targetLocale);
				if (targetLocale == null) {
					throw (IllegalArgumentException) new IllegalArgumentException("Couldn't open " + oldTargetLocale).initCause(e);
				}
				log.println("FALLING BACK TO " + targetLocale + " from " + oldTargetLocale);
			}
        } while (currentFile == null);
        for (Iterator it = currentFile.iterator(); it.hasNext(); ) {
        	String path = (String) it.next();
        	if (!isGregorianPattern(path, parts)) continue;
        	String value = currentFile.getStringValue(path);
        	result.add(value);
        	//log.println("adding " + path + "\t" + value);
        }
        return result;
	}
	
	public static boolean isGregorianPattern(String path, XPathParts parts) {
    	if (path.indexOf("Formats") < 0) return false; // quick exclude
    	parts.set(path);
    	if (parts.size() < 8 || !parts.getElement(7).equals("pattern")) return false;
    	if (!parts.containsAttributeValue("type","gregorian")) return false;
    	return true;
	}

	static class LocaleIDFixer {
		LocaleIDParser lip = new LocaleIDParser();
	    static final Set mainLocales = new HashSet(Arrays.asList(new String[]
	    {"ar_EG", "bn_IN", "de_DE", "en_US", "es_ES", "fr_FR", "it_IT", "nl_NL", "pt_BR", "sv_SE", "zh_TW"}));
	    DeprecatedCodeFixer dcf = new DeprecatedCodeFixer();
	
		Map fixLocales(Collection available, Map result) {
			// find the multi-country locales
			Map language_locales = new HashMap();
			for (Iterator it = available.iterator(); it.hasNext();) {
				String locale = (String) it.next();
				String fixedLocale = dcf.fixLocale(locale);
				result.put(locale, fixedLocale);
				String language = lip.set(fixedLocale).getLanguageScript();
				Set locales = (Set) language_locales.get(language);
				if (locales == null) language_locales.put(language, locales = new HashSet());
				locales.add(locale);
			}
			// if a language has a single locale, use it
			// otherwise use main
			for (Iterator it = language_locales.keySet().iterator(); it.hasNext();) {
				String language = (String) it.next();
				Set locales = (Set) language_locales.get(language);
				if (locales.size() == 1) {
					result.put(locales.iterator().next(), language);
					continue;
				}
				Set intersect = new HashSet(mainLocales);
				intersect.retainAll(locales);
				if (intersect.size() == 1) {
					// the intersection is the parent, so overwrite it
					result.put(intersect.iterator().next(), language);
					continue;
				}
				if (locales.contains("zh_CN")) { // special case, not worth extra code
					result.put("zh_CN", "zh");
					continue;
				}
				throw new IllegalArgumentException("Need parent locale: " + locales);
			}
			return result;
		}
	}
	
	static class DeprecatedCodeFixer {
	    Map languageAlias = new HashMap();
	    Map territoryAlias = new HashMap();
	    {
	    	Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
	    	CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
	    	XPathParts parts = new XPathParts(null, null);
	    	for (Iterator it = supp.iterator(); it.hasNext();) {
	    		String path = (String) it.next();
	    		//System.out.println(path);
	    		if (!path.startsWith("//supplementalData/metadata/alias/")) continue;
	    		parts.set(supp.getFullXPath(path));
	    		Map attributes = parts.getAttributes(3);
	    		String type = (String) attributes.get("type");
	    		String replacement = (String) attributes.get("replacement");
	    		if (parts.getElement(3).equals("languageAlias")) {
	    			languageAlias.put(type, replacement);
	    		} else if (parts.getElement(3).equals("territoryAlias")) {
	    			territoryAlias.put(type, replacement);
	    		} else throw new IllegalArgumentException("Unexpected type: " + path);
	    	}
	    	// special hack for OpenOffice
	    	territoryAlias.put("CB", "029");
	    	languageAlias.put("no", "nb");
	    }
	    LocaleIDParser lip = new LocaleIDParser();
	    
	    String fixLocale(String locale) {
	    	String oldLocale = locale;
	    	lip.set(locale);
	    	String territory = lip.getRegion();
	    	String replacement = (String) territoryAlias.get(territory);
	    	if (replacement != null) {
	    		lip.setRegion(replacement);
	    	}
	    	locale = lip.toString();
	    	for (Iterator it = languageAlias.keySet().iterator(); it.hasNext();) {
	    		String old = (String) it.next();
	    		if (!locale.startsWith(old)) continue;
	    		if (locale.length() == old.length()) {
	    			locale = (String) languageAlias.get(old);
	    			break;
	    		}
	    		else if (locale.charAt(old.length())=='_') {
	    			locale = (String) languageAlias.get(old) + locale.substring(old.length());
	    			break;
	    		}
	    	}
	    	//if (!oldLocale.equals(locale)) System.out.println(oldLocale + " \u2192 " + locale);
	    	return locale;
	    }
	}

	private static void test(String[] args) {
		// get the locale to use, with default
        String filter = "en_US";
        if (args.length > 0)
            filter = args[0];
        
        Factory cldrFactory = Factory.make(Utility.BASE_DIRECTORY
                + "open_office/main/", filter);
        for (Iterator it = cldrFactory.getAvailable().iterator(); it.hasNext();) {
            String locale = (String) it.next();
            ULocale ulocale = new ULocale(locale);
            System.out.println(ulocale.getDisplayName(ULocale.ENGLISH) + " (" + locale + ")");
            
            SimpleDateFormat df = (SimpleDateFormat) DateFormat
            .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                    ulocale);
            
            Collection list = getOOData(cldrFactory, locale);
            
            
            String[] testData = { "YwE", // year, week of year, weekday
                    "yD", // year, day of year
                    "yMFE", // year, month, nth day of week in month
                    "eG", "dMMy", "kh", "GHHmm", "yyyyHHmm", "Kmm", "kmm",
                    "MMdd", "ddHH", "yyyyMMMd", "yyyyMMddHHmmss",
                    "GEEEEyyyyMMddHHmmss",
                    "GuuuuMMMMwwWddDDDFEEEEaHHmmssSSSvvvv", // bizarre case just for testing
                    };
            DateTimePatternGenerator fdt = new DateTimePatternGenerator()
            .add(list);
            Date now = new Date(99, 11, 23, 1, 2, 3);
            System.out.println("Sample Input: " + now);
            for (int i = 0; i < testData.length; ++i) {
                System.out.print("Input request: \t" + testData[i]);
                System.out.print(SEPARATOR + "Fields: \t" + fdt.getFields(testData[i]));
                String dfpattern;
                try {
                    dfpattern = fdt.getBest(testData[i]);
                } catch (Exception e) {
                    System.out.println(SEPARATOR + e.getMessage());
                    continue;
                }
                System.out.print(SEPARATOR + "Localized Pattern: \t" + dfpattern);
                df.applyPattern(dfpattern);
                System.out.println(SEPARATOR + "Sample Results: \t?" + df.format(now) + "?");
            }
        }
	}
    
    
    
    
    static class VariableField {
        private String string;
        VariableField(String string) {
            this.string = string;
        }
        public String toString() {
            return string;
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
            return last <= 'z' && last >= '0' && (last <= '9' || last >= 'a' || (last >= 'A' && last <= 'Z'));
        }
        public List getItems() {
            return Collections.unmodifiableList(items);
        }
    }
    
    static class DateTimePatternGenerator {
        private ArrayList list = new ArrayList(1); // items are in priority order
        private transient DateTimeMatcher current = new DateTimeMatcher();
        private transient FormatParser fp = new FormatParser();
        private transient DistanceInfo _distanceInfo = new DistanceInfo();
        private transient boolean isComplete = false;
        
        public Collection getPatterns(Collection result) {
            for (Iterator it = list.iterator(); it.hasNext();) {
                DateTimeMatcher item = (DateTimeMatcher) it.next();
                result.add(item.toString());
            }
            return result;
        }
        
        DateTimePatternGenerator add(String pattern) {
            list.add(new DateTimeMatcher().set(pattern));
            return this;
        }
        
        DateTimePatternGenerator add(Collection list) {
            for (Iterator it = list.iterator(); it.hasNext();) {
                add((String)it.next());
            }
            return this;
        }
        
        String getBest(String inputRequest) {
            if (!isComplete) complete();
            current.set(inputRequest);
            String best = getBestRaw(current, -1, _distanceInfo);
            if (_distanceInfo.missingFieldMask == 0 && _distanceInfo.extraFieldMask == 0) {
                // we have a good item. Adjust the field types
                return adjustFieldTypes(best, current);
            }
            int neededFields = current.getFieldMask();
            // otherwise break up by date and time.
            String datePattern = getBestAppending(neededFields & DATE_MASK);
            String timePattern = getBestAppending(neededFields & TIME_MASK);
            
            
            if (datePattern == null) return timePattern == null ? "" : timePattern;
            if (timePattern == null) return datePattern;
            return MessageFormat.format(getDateTimeFormat(), new Object[]{datePattern, timePattern});
        }

        // TODO: get from resource
		private String getDateTimeFormat() {
			return "{0} {1}";
		}
        
        // TODO: get from resource
		private String getAppendFormat(int foundMask) {
			return "{0} [''" + showMask(foundMask) + "'' {1}]";
		}
        
        /**
         * 
         */
        private String getBestAppending(int missingFields) {
            String resultPattern = null;
            if (missingFields != 0) {
                resultPattern = getBestRaw(current, missingFields, _distanceInfo);
                resultPattern = adjustFieldTypes(resultPattern, current);
                while (_distanceInfo.missingFieldMask != 0) { // precondition: EVERY single field must work!
                	int startingMask = _distanceInfo.missingFieldMask;
                    String temp = getBestRaw(current, _distanceInfo.missingFieldMask, _distanceInfo);
                    temp = adjustFieldTypes(temp, current);
                    int foundMask = startingMask & ~_distanceInfo.missingFieldMask;
                    resultPattern = MessageFormat.format(getAppendFormat(foundMask), new Object[]{resultPattern, temp});
                }
            }
            return resultPattern;
        }
        
        /**
         * 
         */
        private void complete() {
            // make sure that every valid field occurs once, with a "default" length
            // for now, just 1. Optimized later
            boolean[] checked = new boolean[128];
            checked['a'] = true; // skip 'a'
            for (int i = 0; i < types.length; ++i) {
                char c = (char)types[i][0];
                if (checked[c]) continue;
                checked[c] = true;
                add(String.valueOf(c));
            }
            isComplete = true;
            // and add a complete time, complete date. This is a hack for now;
            // should be replaced by test on locale data.
            //add("HH:mm:ss.SSS v");
            //add("G yyyy-MM-dd"); // full normal day
            //add("eee, G yyyy-MM-dd"); // full normal day with dow
        }
        
        /**
         * 
         */
        private String getBestRaw(DateTimeMatcher source, int includeMask, DistanceInfo missingFields) {
            if (SHOW_DISTANCE) System.out.println("Searching for: " + source.pattern 
                    + ", mask: " + showMask(includeMask));
            DateTimeMatcher best = null;
            int bestDistance = Integer.MAX_VALUE;
            DistanceInfo tempInfo = new DistanceInfo();
            int limit = list.size();
            int missingCount = 0;
            for (int i = 0; i < limit; ++i) {
                DateTimeMatcher trial = (DateTimeMatcher) list.get(i);
                int distance = source.getDistance(trial, includeMask, tempInfo);
                if (SHOW_DISTANCE) System.out.println("\tDistance: " + trial.pattern + ":\t" 
                        + distance + ",\tmissing fields: " + tempInfo);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = trial;
                    missingFields.setTo(tempInfo);
                    if (distance == 0) break;
                }
            }
            return best.pattern;
        }
        
        public String replaceFieldTypes(String pattern, String inputRequest) {
            return adjustFieldTypes(pattern, current.set(inputRequest));
        }
        
        /**
         * 
         */
        private String adjustFieldTypes(String pattern, DateTimeMatcher inputRequest) {
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
                    if (inputRequest.type[type] != 0) {
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
            if (SHOW_DISTANCE) System.out.println("\tRaw: " + pattern);
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
    }
    
    private static class DateTimeMatcher {
        // just for testing; fix to make multi-threaded later
        static FormatParser fp = new FormatParser();
        String pattern = null;
        int[] type = new int[TYPE_LIMIT];
        String[] original = new String[TYPE_LIMIT];
        transient Set set = new TreeSet();
        
        public String toString() {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < TYPE_LIMIT; ++i) {
                if (original[i] != null) result.append(original[i]);
            }
            return result.toString();
        }
        
        DateTimeMatcher set(String pattern) {
            for (int i = 0; i < TYPE_LIMIT; ++i) {
                type[i] = NONE;
                original[i] = null;
            }
            this.pattern = pattern;
            fp.set(pattern);
            for (Iterator it = fp.getFields(new ArrayList()).iterator(); it.hasNext();) {
                String field = (String) it.next();
                if (field.charAt(0) == 'a') continue; // skip day period, special cass
                int canonicalIndex = getCanonicalIndex(field);
                if (canonicalIndex < 0) {
                    System.out.println("Bad field: " + field);
                    continue;
                }
                int[] row = types[canonicalIndex];
                int typeValue = row[1];
                if (original[typeValue] != null) {
                    throw new IllegalArgumentException("Conflicting fields: "
                            + original[typeValue] + ", " + field);
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
            pattern = source.pattern = null; // nuke the patterns, no longer valid
            for (int i = 0; i < type.length; ++i) {
                if ((fieldMask & (1<<i)) != 0) {
                    type[i] = source.type[i];
                    original[i] = source.original[i];
                } else {
                    type[i] = NONE;
                    original[i] = null;
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
    }
    
    static class DistanceInfo {
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
            return "missingFieldMask: " + showMask(missingFieldMask)
            + ", extraFieldMask: " + showMask(extraFieldMask);
        }
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
    
    static private int
    ERA = 0,
    YEAR = 1,
    QUARTER = 2,
    MONTH = 3,
    WEEK_OF_YEAR = 4,
    WEEK_OF_MONTH = 5,
    WEEKDAY = 6,
    DAY = 7,
    DAY_OF_YEAR = 8,
    DAY_OF_WEEK_IN_MONTH = 9,
    DAYPERIOD = 10,
    HOUR = 11,
    MINUTE = 12,
    SECOND = 13,
    FRACTIONAL_SECOND = 14,
    ZONE = 15,
    TYPE_LIMIT = 16;
    
    static private int 
    DATE_MASK = (1<<DAYPERIOD) - 1,
    TIME_MASK = (1<<TYPE_LIMIT) - 1 - DATE_MASK;
    
    static private int // numbers are chosen to express 'distance'
    DELTA = 0x10,
    NUMERIC = 0x100,
    NONE = 0,
    NARROW = -0x100,
    SHORT = -0x101,
    LONG = -0x102,
    EXTRA_FIELD =   0x10000,
    MISSING_FIELD = 0x1000;
    
    private static String getName(String s) {
        int i = getCanonicalIndex(s);
        String name = FIELD_NAME[types[i][1]];
        int subtype = types[i][2];
        boolean string = subtype < 0;
        if (string) subtype = -subtype;
        if (subtype < 0) name += ":S";
        else name += ":N";
        return name;
    }
    
    private static int getCanonicalIndex(String s) {
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
    
    // =================
    
    static class OOConverter {
        FormatParser fp = new FormatParser();
        
        public String convertOODate(String source, String locale) {
            if (source.length() == 0) return "";
            source = source.replace('"', '\''); // fix quoting convention
            StringBuffer buffer = new StringBuffer();
            fp.set(source);
            for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
                Object item = it.next();
                if (item instanceof VariableField) {
                    buffer.append(handleOODate(item.toString(), locale));
                } else {
                    buffer.append(item);
                }
            }
            return buffer.toString();
        }
        
        private String handleOODate(String string, String locale) {
            // preprocess hack for *localized* strings
            if (locale.startsWith("de")) {
                if (string.startsWith("T")) string = string.replace('T','D');
                if (string.startsWith("J")) string = string.replace('J','Y');
            } else if (locale.startsWith("nl")) {
                if (string.startsWith("J")) string = string.replace('J','Y');
            } else if (locale.startsWith("fi")) {
                if (string.startsWith("K")) string = string.replace('K','M');
                if (string.startsWith("V")) string = string.replace('V','Y');
                if (string.startsWith("P")) string = string.replace('P','D');
            } else if (locale.startsWith("fr")) {
                if (string.startsWith("J")) string = string.replace('J','D');
                if (string.startsWith("A")) string = string.replace('A','Y');
            } else if (locale.startsWith("es") || locale.startsWith("pt")) {
                if (string.startsWith("A")) string = string.replace('A','Y');
            } else if (locale.startsWith("it")) {
                if (string.startsWith("A")) string = string.replace('A','Y');
                if (string.startsWith("G")) string = string.replace('G','D');
            }
            //if (string.startsWith("M")) return string;
            if (string.startsWith("A")) string = string.replace('A','y'); // best we can do for now
            else if (string.startsWith("Y") || string.startsWith("W") || 
                    string.equals("D") || string.equals("DD")) string = string.toLowerCase();
            else if (string.equals("DDD") || string.equals("NN")) string = "EEE";
            else if (string.equals("DDDD") || string.equals("NNN")) string = "EEEE";
            else if (string.equals("NNNN")) return "EEEE, "; // RETURN WITHOUT TEST
            else if (string.equals("G")) string = "G"; // best we can do for now
            else if (string.equals("GG")) string = "G";
            else if (string.equals("GGG")) string = "G"; // best we can do for now
            else if (string.equals("E")) string = "y";
            else if (string.equals("EE") || string.equals("R")) string = "yy";
            else if (string.equals("RR")) string = "Gyy";
            //if (string.startsWith("Q")) string = string; // '\'' + string + '\'';
            //char c = string.charAt(0);
            //if (c < 0x80 && UCharacter.isLetter(c)else if rn string.replace(c,'x');
            if (!allowedDateTimeCharacters.containsAll(string)) {
            	throw new IllegalArgumentException("bad char in: " + string);
            }
            return string;
        }
        
        public String convertOOTime(String source, String locale) {
            if (source.length() == 0) return "";
            source = source.replace('"', '\''); // fix quoting convention
            int isAM = source.indexOf("AM/PM");
            if (isAM >= 0) {
                source = source.substring(0,isAM) + "a" + source.substring(isAM+5);
            }
            StringBuffer buffer = new StringBuffer();
            fp.set(source);
            for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
                Object item = it.next();
                if (item instanceof VariableField) {
                    buffer.append(handleOOTime(item.toString(), locale, isAM >= 0));
                } else {
                    buffer.append(item);
                }
            }
            return buffer.toString();
        }
        
        private String handleOOTime(String string, String locale, boolean isAM) {
            char c = string.charAt(0);
            switch (c) {
            case 'h': case 'H': case 't': case 'T': case 'u': case 'U':
                string = string.replace(c, isAM ? 'h' : 'H');
                break;
            case 'M': case 'S': string =  string.toLowerCase(); break;
            case '0': string = string.replace('0','S'); break; // ought to be more sophisticated, but this should work for normal stuff.
            //case 'a': case 's': case 'm': return string; // ok as is
            //default: return "x"; // cause error
            }
            if (!allowedDateTimeCharacters.containsAll(string)) {
            	throw new IllegalArgumentException("bad char in: " + string);
            }
            return string;
        }
        private String convertToRule(String string) {
            fp.set(string);
            StringBuffer buffer = new StringBuffer();
            Set additions = new HashSet();
            for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
                Object item = it.next();
                if (item instanceof VariableField) {
                    String s = item.toString();
                    if (s.startsWith("a")) {
                        buffer.append(s);
                    } else {
                        buffer.append('{' + s + '}');
                    }
                } else {
                    buffer.append(item);
                }
            }
            for (Iterator it = additions.iterator(); it.hasNext();) {
                buffer.insert(0,it.next());
            }
            return buffer.toString();
        }
    }
    static Date TEST_DATE = new Date(104,8,13,23,58,59);
    
    static Comparator VariableFieldComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            Collection a = (Collection)o1;
            Collection b = (Collection)o2;
            if (a.size() != b.size()) {
                if (a.size() < b.size()) return 1;
                return -1;
            }
            Iterator itb = b.iterator();
            for (Iterator ita = a.iterator(); ita.hasNext();) {
                String aa = (String) ita.next();
                String bb = (String) itb.next();
                int result = -aa.compareTo(bb);
                if (result != 0) return result;
            }
            return 0;
        }
    };
    
    public static UnicodeSet allowedDateTimeCharacters = new UnicodeSet("[A a c D d E e F G g h H K k L m M q Q s S u v W w Y y z Z]");
    
    static Collection getOOData(Factory cldrFactory, String locale) {
        List result = new ArrayList();
        XPathParts parts = new XPathParts(null, null);
        OOConverter ooConverter = new OOConverter();
        {
            if (SHOW_OO) System.out.println();
            CLDRFile item = cldrFactory.make(locale, false);
            for (Iterator it2 = item.iterator(); it2.hasNext();) {
                String xpath = (String) it2.next();
            	if (!isGregorianPattern(xpath, parts)) continue;
                boolean isDate = parts.getElement(4).equals("dateFormats");
                boolean isTime = parts.getElement(4).equals("timeFormats");
                String value = item.getStringValue(xpath);
                if (isDate || isTime) {
                    String pattern = value;
                    String oldPattern = pattern;
                    if (oldPattern.indexOf('[') >= 0) {
                    	log.println(locale + "\tSkipping [:\t" + xpath + "\t" + value);
                    	continue;
                    }
                    try {
						pattern = isDate ? ooConverter.convertOODate(pattern, locale) 
						        : ooConverter.convertOOTime(pattern, locale);
					} catch (RuntimeException e1) {
						log.println(locale + "\tSkipping unknown char:\t" + xpath + "\t" + value);
						continue;
					}
                    
                    //System.out.println(xpath + "\t" + pattern);
                    if (SHOW2) System.out.print("\t" + (isDate ? "Date" : "Time") + ": " + oldPattern + "\t" + pattern + "\t");
                    try {
                        SimpleDateFormat d = new SimpleDateFormat(pattern);
                        if (SHOW2) System.out.print(d.format(TEST_DATE));
                        result.add(d.toPattern());
                        if (SHOW_OO) System.out.println(d.toPattern());
                    } catch (Exception e) {
                        if (SHOW2) System.out.print(e.getLocalizedMessage());
                    }
                    if (SHOW2) System.out.println();
                } else {
                	log.println(locale + "\tSkipping datetime:\t" + xpath + "\t" + value);
                }
            }
            return result;
        }
    }
    
    private static Object putNoReplace(Map m, Object key, Object value) {
        Object current = m.get(key);
        if (current != null) return current;
        m.put(key, value);
        return null;
    }
}
