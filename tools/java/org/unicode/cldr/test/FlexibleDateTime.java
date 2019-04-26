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
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;

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
    static final String SEPARATOR = CldrUtility.LINE_SEPARATOR + "\t";

    /**
     * Test different ways of doing flexible date/times.
     * Internal Use.
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // if (false) { // just for testing simple cases
        // DateTimePatternGenerator.DateTimeMatcher a = new DateTimePatternGenerator.DateTimeMatcher().set("HH:mm");
        // DateTimePatternGenerator.DateTimeMatcher b = new DateTimePatternGenerator.DateTimeMatcher().set("kkmm");
        // DistanceInfo missingFields = new DistanceInfo();
        // int distance = a.getDistance(b, -1, missingFields);
        // }
        // generate(args);
        // test(args);
    }

    public static PrintWriter log;

    public static boolean isGregorianPattern(String path) {
        if (path.indexOf("Formats") < 0) {
            return false; // quick exclude
        }
        XPathParts parts = XPathParts.getTestInstance(path);
        if (parts.size() < 8 || !parts.getElement(7).equals("pattern")) {
            return false;
        }
        if (!parts.containsAttributeValue("type", "gregorian")) {
            return false;
        }
        return true;
    }

    static class LocaleIDFixer {
        LocaleIDParser lip = new LocaleIDParser();
        static final Set<String> mainLocales = new HashSet<String>(
            Arrays.asList(new String[] { "ar_EG", "bn_IN", "de_DE", "en_US", "es_ES", "fr_FR", "it_IT", "nl_NL", "pt_BR", "sv_SE", "zh_TW" }));
        DeprecatedCodeFixer dcf = new DeprecatedCodeFixer();

        Map<String, String> fixLocales(Collection<String> available, Map<String, String> result) {
            // find the multi-country locales
            Map<String, Set<String>> language_locales = new HashMap<String, Set<String>>();
            for (String locale : available) {
                String fixedLocale = dcf.fixLocale(locale);
                result.put(locale, fixedLocale);
                String language = lip.set(fixedLocale).getLanguageScript();
                Set<String> locales = language_locales.get(language);
                if (locales == null) {
                    language_locales.put(language, locales = new HashSet<String>());
                }
                locales.add(locale);
            }
            // if a language has a single locale, use it
            // otherwise use main
            for (String language : language_locales.keySet()) {
                Set<String> locales = language_locales.get(language);
                if (locales.size() == 1) {
                    result.put(locales.iterator().next(), language);
                    continue;
                }
                Set<String> intersect = new HashSet<String>(mainLocales);
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
        Map<String, String> languageAlias = new HashMap<String, String>();
        Map<String, String> territoryAlias = new HashMap<String, String>();
        {
            Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
            CLDRFile supp = cldrFactory.make(CLDRFile.SUPPLEMENTAL_NAME, false);
            for (Iterator<String> it = supp.iterator("//supplementalData/metadata/alias/"); it.hasNext();) {
                String path = it.next();
                XPathParts parts = XPathParts.getTestInstance(supp.getFullXPath(path));
                String type = parts.getAttributeValue(3, "type");
                String replacement = parts.getAttributeValue(3, "replacement");
                if (parts.getElement(3).equals("languageAlias")) {
                    languageAlias.put(type, replacement);
                } else if (parts.getElement(3).equals("territoryAlias")) {
                    territoryAlias.put(type, replacement);
                } else {
                    throw new IllegalArgumentException("Unexpected type: " + path);                    
                }
            }
            // special hack for OpenOffice
            territoryAlias.put("CB", "029");
            languageAlias.put("no", "nb");
        }
        LocaleIDParser lip = new LocaleIDParser();

        String fixLocale(String locale) {
            lip.set(locale);
            String territory = lip.getRegion();
            String replacement = (String) territoryAlias.get(territory);
            if (replacement != null) {
                lip.setRegion(replacement);
            }
            locale = lip.toString();
            for (String old : languageAlias.keySet()) {
                if (!locale.startsWith(old)) continue;
                if (locale.length() == old.length()) {
                    locale = languageAlias.get(old);
                    break;
                } else if (locale.charAt(old.length()) == '_') {
                    locale = languageAlias.get(old) + locale.substring(old.length());
                    break;
                }
            }
            // if (!oldLocale.equals(locale)) System.out.println(oldLocale + " \u2192 " + locale);
            return locale;
        }
    }

    // private static void test(String[] args) {
    // // get the locale to use, with default
    // String filter = "en_US";
    // if (args.length > 0)
    // filter = args[0];
    //
    // Factory cldrFactory = Factory.make(CldrUtility.BASE_DIRECTORY
    // + "open_office/main/", filter);
    // for (String locale : cldrFactory.getAvailable()) {
    // ULocale ulocale = new ULocale(locale);
    // System.out.println(ulocale.getDisplayName(ULocale.ENGLISH) + " (" + locale + ")");
    //
    // SimpleDateFormat df = (SimpleDateFormat) DateFormat
    // .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
    // ulocale);
    //
    // Collection list = getOOData(cldrFactory, locale);
    //
    //
    // String[] testData = { "YwE", // year, week of year, weekday
    // "yD", // year, day of year
    // "yMFE", // year, month, nth day of week in month
    // "eG", "dMMy", "kh", "GHHmm", "yyyyHHmm", "Kmm", "kmm",
    // "MMdd", "ddHH", "yyyyMMMd", "yyyyMMddHHmmss",
    // "GEEEEyyyyMMddHHmmss",
    // "GuuuuMMMMwwWddDDDFEEEEaHHmmssSSSvvvv", // bizarre case just for testing
    // };
    // DateTimePatternGenerator fdt = DateTimePatternGenerator.getEmptyInstance();
    // add(fdt, list);
    // Date now = new Date(99, 11, 23, 1, 2, 3);
    // System.out.println("Sample Input: " + now);
    // for (int i = 0; i < testData.length; ++i) {
    // System.out.print("Input request: \t" + testData[i]);
    // System.out.print(SEPARATOR + "Fields: \t" + fdt.getFields(testData[i]));
    // String dfpattern;
    // try {
    // dfpattern = fdt.getBestPattern(testData[i]);
    // } catch (Exception e) {
    // System.out.println(SEPARATOR + e.getMessage());
    // continue;
    // }
    // System.out.print(SEPARATOR + "Localized Pattern: \t" + dfpattern);
    // df.applyPattern(dfpattern);
    // System.out.println(SEPARATOR + "Sample Results: \t?" + df.format(now) + "?");
    // }
    // }
    // }

    public static void add(DateTimePatternGenerator generator, Collection<String> list) {
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            generator.addPattern(it.next(), false, null);
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
            for (Iterator<Object> it = fp.getItems().iterator(); it.hasNext();) {
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
                if (string.startsWith("T")) string = string.replace('T', 'D');
                if (string.startsWith("J")) string = string.replace('J', 'Y');
            } else if (locale.startsWith("nl")) {
                if (string.startsWith("J")) string = string.replace('J', 'Y');
            } else if (locale.startsWith("fi")) {
                if (string.startsWith("K")) string = string.replace('K', 'M');
                if (string.startsWith("V")) string = string.replace('V', 'Y');
                if (string.startsWith("P")) string = string.replace('P', 'D');
            } else if (locale.startsWith("fr")) {
                if (string.startsWith("J")) string = string.replace('J', 'D');
                if (string.startsWith("A")) string = string.replace('A', 'Y');
            } else if (locale.startsWith("es") || locale.startsWith("pt")) {
                if (string.startsWith("A")) string = string.replace('A', 'Y');
            } else if (locale.startsWith("it")) {
                if (string.startsWith("A")) string = string.replace('A', 'Y');
                if (string.startsWith("G")) string = string.replace('G', 'D');
            }
            // if (string.startsWith("M")) return string;
            if (string.startsWith("A"))
                string = string.replace('A', 'y'); // best we can do for now
            else if (string.startsWith("Y") || string.startsWith("W") ||
                string.equals("D") || string.equals("DD"))
                string = string.toLowerCase();
            else if (string.equals("DDD") || string.equals("NN"))
                string = "EEE";
            else if (string.equals("DDDD") || string.equals("NNN"))
                string = "EEEE";
            else if (string.equals("NNNN"))
                return "EEEE, "; // RETURN WITHOUT TEST
            else if (string.equals("G"))
                string = "G"; // best we can do for now
            else if (string.equals("GG"))
                string = "G";
            else if (string.equals("GGG"))
                string = "G"; // best we can do for now
            else if (string.equals("E"))
                string = "y";
            else if (string.equals("EE") || string.equals("R"))
                string = "yy";
            else if (string.equals("RR")) string = "Gyy";
            // if (string.startsWith("Q")) string = string; // '\'' + string + '\'';
            // char c = string.charAt(0);
            // if (c < 0x80 && UCharacter.isLetter(c)else if rn string.replace(c,'x');
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
                source = source.substring(0, isAM) + "a" + source.substring(isAM + 5);
            }
            StringBuffer buffer = new StringBuffer();
            fp.set(source);
            for (Iterator<Object> it = fp.getItems().iterator(); it.hasNext();) {
                Object item = it.next();
                if (item instanceof VariableField) {
                    buffer.append(handleOOTime(item.toString(), isAM >= 0));
                } else {
                    buffer.append(item);
                }
            }
            return buffer.toString();
        }

        private String handleOOTime(String string, boolean isAM) {
            char c = string.charAt(0);
            switch (c) {
            case 'h':
            case 'H':
            case 't':
            case 'T':
            case 'u':
            case 'U':
                string = string.replace(c, isAM ? 'h' : 'H');
                break;
            case 'M':
            case 'S':
                string = string.toLowerCase();
                break;
            case '0':
                string = string.replace('0', 'S');
                break; // ought to be more sophisticated, but this should work for normal stuff.
            // case 'a': case 's': case 'm': return string; // ok as is
            // default: return "x"; // cause error
            }
            if (!allowedDateTimeCharacters.containsAll(string)) {
                throw new IllegalArgumentException("bad char in: " + string);
            }
            return string;
        }
    }

    static Date TEST_DATE = new Date(104, 8, 13, 23, 58, 59);

    static Comparator<Collection<String>> VariableFieldComparator = new Comparator<Collection<String>>() {
        public int compare(Collection<String> a, Collection<String> b) {
            if (a.size() != b.size()) {
                if (a.size() < b.size()) return 1;
                return -1;
            }
            Iterator<String> itb = b.iterator();
            for (Iterator<String> ita = a.iterator(); ita.hasNext();) {
                String aa = ita.next();
                String bb = itb.next();
                int result = -aa.compareTo(bb);
                if (result != 0) return result;
            }
            return 0;
        }
    };

    public static UnicodeSet allowedDateTimeCharacters = new UnicodeSet(
        "[A a c D d E e F G g h H K k L m M q Q s S u v W w Y y z Z]");

    static Collection<String> getOOData(Factory cldrFactory, String locale) {
        List<String> result = new ArrayList<String>();
        OOConverter ooConverter = new OOConverter();
        {
            if (SHOW_OO) {
                System.out.println();
            }
            CLDRFile item = cldrFactory.make(locale, false);
            for (String xpath : item) {
                if (!isGregorianPattern(xpath)) {
                    continue;
                }
                XPathParts parts = XPathParts.getTestInstance(xpath);
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

                    // System.out.println(xpath + "\t" + pattern);
                    if (SHOW2)
                        System.out.print("\t" + (isDate ? "Date" : "Time") + ": " + oldPattern + "\t" + pattern + "\t");
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
}
