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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.util.ULocale;

/**
 * Test class for trying different approaches to flexible date/time.
 * Internal Use.
 * Once we figure out what approach to take, this should turn into the test file
 * for the data.
 */
public class FlexibleDateTime {
    static final boolean DEBUG = false;
    static final boolean SHOW_MATCHING = false;
    static final boolean SHOW2 = false;
    static final boolean SHOW_OO = false;
    static final String SEPARATOR = Utility.LINE_SEPARATOR + "\t";
    
    /**
     * Test different ways of doing flexible date/times.
     * Internal Use.
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
//        if (false) { // just for testing simple cases
//            DateTimePatternGenerator.DateTimeMatcher a = new DateTimePatternGenerator.DateTimeMatcher().set("HH:mm");
//            DateTimePatternGenerator.DateTimeMatcher b = new DateTimePatternGenerator.DateTimeMatcher().set("kkmm");
//            DistanceInfo missingFields = new DistanceInfo();
//            int distance = a.getDistance(b, -1, missingFields);
//        }
        //generate(args);
        //test(args);
    }

    public static PrintWriter log;
    
//	private static void generate(String[] args) throws IOException {
//		log = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY + "/flex/", "log.txt");
//        String filter = ".*";
//        if (args.length > 0)
//            filter = args[0];
//        
//        Factory cldrFactory = Factory.make(Utility.BASE_DIRECTORY
//                + "open_office/main/", filter);
//        Factory mainCLDRFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
//        FormatParser fp = new FormatParser();
//        // fix locale list
//        Collection ooLocales = new LinkedHashSet(cldrFactory.getAvailable());
//        ooLocales.remove("nb_NO"); // hack, since no_NO is the main one, and subsumes nb
//        Map localeMap = new LocaleIDFixer().fixLocales(ooLocales, new TreeMap());
//        //pw.println(localeMap);
//
//        for (Iterator it = localeMap.keySet().iterator(); it.hasNext();) {
//            String sourceLocale = (String) it.next();
//            String targetLocale = (String) localeMap.get(sourceLocale);
//            ULocale uSourceLocale = new ULocale(sourceLocale);
//            ULocale uTargetLocale = new ULocale(targetLocale);
//            log.println();
//            log.println(uTargetLocale.getDisplayName(ULocale.ENGLISH) + " (" + uTargetLocale + ")");
//            System.out.println(sourceLocale + "\t\u2192" + uTargetLocale.getDisplayName(ULocale.ENGLISH) + " (" + uTargetLocale + ")");
//            if (!sourceLocale.equals(targetLocale)) {
//            	log.println("[oo: " + uSourceLocale.getDisplayName(ULocale.ENGLISH) + " (" + sourceLocale + ")]");
//            }
//            Collection list = getOOData(cldrFactory, sourceLocale);
//            // get the current values
//            try {
//                Collection currentList = getDateFormats(mainCLDRFactory, targetLocale);
//                list.removeAll(currentList);
//            } catch (RuntimeException e) {
//                // ignore
//            }
//            
//            if (list.size() == 0) {
//            	log.println(sourceLocale + "\tEMPTY!"); // skip empty
//            	continue;
//            }
//            CLDRFile temp = CLDRFile.make(targetLocale);
//            String prefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@_q=\"";
//
//            int count = 0;
//            Map previousID = new HashMap();
//            for (Iterator it2 = list.iterator(); it2.hasNext();) {
//            	String pattern = (String) it2.next();
//            	new SimpleDateFormat(pattern); // check that compiles
//            	fp.set(pattern);
//            	String id = fp.getVariableFieldString();
//            	if (!allowedDateTimeCharacters.containsAll(id)) throw new IllegalArgumentException("Illegal characters in: " + pattern);
//            	if (id.length() == 0) {
//                    throw new IllegalArgumentException("Empty id for: " + pattern);
//                }
//                String previous = (String) previousID.get(id);
//                if (previous != null) {
//                    log.println("Skipping Duplicate pattern: " + pattern + " (already have " + previous + ")");
//                    continue;
//                } else {
//                    previousID.put(id, pattern);
//                }
//            	String path = prefix + (count++) + "\"]";
//            	temp.add(path, pattern);
//            }
//    		PrintWriter pw = BagFormatter.openUTF8Writer(Utility.GEN_DIRECTORY + "/flex/", targetLocale + ".xml");
//            temp.write(pw);
//            pw.close();
//            log.flush();
//        }
//        System.out.println("done");
//        log.close();
//	}

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
        	String value = currentFile.getWinningValue(path);
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
	    	for (Iterator it = supp.iterator("//supplementalData/metadata/alias/"); it.hasNext();) {
	    		String path = (String) it.next();
	    		//System.out.println(path);
	    		//if (!path.startsWith("//supplementalData/metadata/alias/")) continue;
	    		parts.set(supp.getFullXPath(path));
	    		//Map attributes = parts.getAttributes(3);
	    		String type = parts.getAttributeValue(3, "type");
	    		String replacement = parts.getAttributeValue(3, "replacement");
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
            DateTimePatternGenerator fdt = DateTimePatternGenerator.getEmptyInstance();
            add(fdt, list);
            Date now = new Date(99, 11, 23, 1, 2, 3);
            System.out.println("Sample Input: " + now);
            for (int i = 0; i < testData.length; ++i) {
                System.out.print("Input request: \t" + testData[i]);
                System.out.print(SEPARATOR + "Fields: \t" + fdt.getFields(testData[i]));
                String dfpattern;
                try {
                    dfpattern = fdt.getBestPattern(testData[i]);
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
    
    
    public static void add(DateTimePatternGenerator generator, Collection list) {
        for (Iterator it = list.iterator(); it.hasNext();) {
            generator.addPattern((String)it.next(), false, null);
        }
    }
    
   
    
    
    
    
    
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
                String value = item.getWinningValue(xpath);
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
