/*
 * Created on May 19, 2005
 * Copyright (C) 2004-2005, Unicode, Inc., International Business Machines Corporation, and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ArrayComparator;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.TransliteratorUtilities;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

/**
 * @throws IOException
 *
 */
class GenerateStatistics {
    static final boolean HACK = true;
    static CLDRFile english;
    static Factory factory;
    static LanguageTagParser ltp = new LanguageTagParser();
    static Collator col = Collator.getInstance(ULocale.ENGLISH);
    static boolean notitlecase = true;

    public static void generateSize(String sourceDir, String logDir, String match, boolean transliterate)
        throws IOException {
        factory = Factory.make(sourceDir, match);
        ToolUtilities.registerExtraTransliterators();

        PrintWriter logHtml = FileUtilities.openUTF8Writer(logDir, "test_generation_log.html");
        //String dir = logDir + "main" + File.separator;
        // DraftChecker dc = new DraftChecker(dir);
        english = factory.make("en", true);
        Set<String> languages = new TreeSet<String>(col), countries = new TreeSet<String>(col), draftLanguages = new TreeSet<String>(
            col), draftCountries = new TreeSet<String>(col);
        Set<Object> nativeLanguages = new TreeSet<Object>(), nativeCountries = new TreeSet<Object>(), draftNativeLanguages = new TreeSet<Object>(),
            draftNativeCountries = new TreeSet<Object>();
        int localeCount = 0;
        int draftLocaleCount = 0;

        Set<String> contents = removeSingleLanguagesWhereWeHaveScripts(factory.getAvailable());

        for (Iterator<String> it = contents.iterator(); it.hasNext();) {
            String localeID = it.next();
            if (CLDRFile.isSupplementalName(localeID)) continue;
            if (localeID.equals("root"))
                continue; // skip root
            System.out.println("Collecting info for:\t" + localeID.replace("_", "\t"));
            boolean draft = false; // dc.isDraft(localeName);
            if (draft) {
                draftLocaleCount++;
                addCounts(localeID, true, draftLanguages,
                    draftCountries, draftNativeLanguages,
                    draftNativeCountries);
            } else {
                localeCount++;
                addCounts(localeID, false, languages,
                    countries, nativeLanguages, nativeCountries);
            }
            if (false)
                Log.logln(draft + ", " + localeCount + ", "
                    + languages.size() + ", " + countries.size() + ", "
                    + draftLocaleCount + ", " + draftLanguages.size()
                    + ", " + draftCountries.size());
        }
        draftLanguages.removeAll(languages);
        for (Iterator<Object> it = nativeLanguages.iterator(); it.hasNext();) {
            draftNativeLanguages.remove(it.next());
        }
        logHtml.println("<html><head>");
        logHtml
            .println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        logHtml.println("</head><body>");
        logHtml.println("<p><b>Locales (" + localeCount + "):</b>");
        logHtml.println("<p><b>Languages (" + languages.size() + "):</b>");
        logHtml.println(showSet(nativeLanguages, transliterate, true));
        logHtml.println("<p><b>Territories (" + countries.size() + "):</b>");
        logHtml.println(showSet(nativeCountries, transliterate, false));
        logHtml.println("<p><b>Draft locales (" + draftLocaleCount + "):</b>");
        logHtml.println("<p><b>Draft languages (" + draftLanguages.size()
            + "):</b>");
        logHtml.println(showSet(draftNativeLanguages, transliterate, true));
        logHtml.println("<p><b>Draft countries (" + draftCountries.size()
            + "):</b>");
        logHtml.println(showSet(draftNativeCountries, transliterate, false));
        logHtml.println(CldrUtility.ANALYTICS);
        logHtml.println("</body></html>");
        logHtml.close();
    }

    /**
     *
     */
    private static Set<String> removeSingleLanguagesWhereWeHaveScripts(Set<String> contents) {
        StandardCodes sc = StandardCodes.make();
        contents = new TreeSet<String>(contents); // make writable
        if (false && HACK) {
            contents.add("bs_Latn");
            contents.add("bs_Cyrl");
            contents.add("bs_Latn_BA");
            contents.add("bs_Cyrl_BA");
        }
        // find the languages with scripts
        Set<String> toRemove = new HashSet<String>();
        if (HACK) toRemove.add("sh");

        for (Iterator<String> it = contents.iterator(); it.hasNext();) {
            String localeID = it.next();
            if (CLDRFile.isSupplementalName(localeID)) {
                continue;
            }
            // if there is a lang_script, then remove everything starting with lang that doesn't have "a" script
            String lang = ltp.set(localeID).getLanguage();
            String territory = ltp.set(localeID).getRegion();
            if (!sc.getGoodAvailableCodes("language").contains(lang)) {
                System.out.println("Odd language, removing: " + localeID);
                it.remove();
                continue;
            }
            if (territory.length() != 0 && !sc.getGoodAvailableCodes("territory").contains(territory)) {
                System.out.println("Odd territory, removing: " + localeID);
                it.remove();
                continue;
            }
            String langscript = ltp.set(localeID).getLanguageScript();
            if (!lang.equals(langscript)) toRemove.add(lang);
        }

        for (Iterator<String> it = contents.iterator(); it.hasNext();) {
            String localeID = it.next();
            if (CLDRFile.isSupplementalName(localeID)) {
                continue;
            }
            // if there is a lang_script, then remove everything starting with lang that doesn't have "a" script
            String lang = ltp.set(localeID).getLanguage();
            if (!toRemove.contains(lang)) continue;
            String langscript = ltp.set(localeID).getLanguageScript();
            if (lang.equals(langscript)) it.remove();
        }
        return contents;
    }

    static final UnicodeSet NON_LATIN = new UnicodeSet("[^[:latin:][:common:][:inherited:]]");

    /**
     * @param nativeCountries
     * @param transliterate
     *            TODO
     * @param isLanguage
     *            TODO
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static String showSet(Set nativeCountries, boolean transliterate,
        boolean isLanguage) {
        UnicodeSet BIDI_R = new UnicodeSet(
            "[[:Bidi_Class=R:][:Bidi_Class=AL:]]");
        StringBuffer result = new StringBuffer();
        Map sb = new TreeMap(LanguageList.col);
        // collect multiples by English name
        for (Iterator it = nativeCountries.iterator(); it.hasNext();) {
            LanguageList llist = (LanguageList) it.next();
            Set s = (Set) sb.get(llist.getEnglishName());
            if (s == null)
                sb.put(llist.getEnglishName(), s = new TreeSet());
            s.add(llist);
        }

        Set<String> titleSet = new TreeSet<String>(col);
        Set<String> qualifierSet = new TreeSet<String>(col);

        for (Iterator<String> it = sb.keySet().iterator(); it.hasNext();) {
            String englishName = it.next();
            Set s = (Set) sb.get(englishName);
            if (result.length() != 0) {
                result.append("; ");
            }
            String code = "";
            boolean needQualifier = s.size() != 1;
            titleSet.clear();
            qualifierSet.clear();

            for (Iterator<LanguageList> it2 = s.iterator(); it2.hasNext();) {
                LanguageList llist = it2.next();
                String localName = llist.getLocalName();
                String locale = llist.getLocale();

                // see if we need qualifier
                String lang = locale, country = "";
                if (locale.length() > 3
                    && locale.charAt(locale.length() - 3) == '_') {
                    lang = locale.substring(0, locale.length() - 3);
                    country = locale.substring(locale.length() - 2);
                }

                // fix
                if (BIDI_R.containsSome(localName))
                    localName = '\u200E' + localName + '\u200E';

                // qualifiers += lang;

                if (isLanguage) {
                    code = lang;
                } else {
                    code = country;
                }

                if (!localName.equalsIgnoreCase(englishName)) {
                    needQualifier = true;
                    qualifierSet.add(localName);

                    if (transliterate && NON_LATIN.containsSome(localName)
                        && !lang.equals("ja")) {
                        String transName = localName;
                        try {
                            transName = fixedTitleCase("en",
                                toLatin.transliterate(localName));
                        } catch (RuntimeException e) {
                            System.out.println("\t" + e.getMessage());
                        }
                        if (NON_LATIN.containsSome(transName)) {
                            Log.logln("Can't transliterate " + localName
                                + ": " + transName);
                        } else {
                            titleSet.add(transName);
                        }
                    }
                }
            }
            String title = code + (titleSet.isEmpty() ? "" : ": " + titleSet.toString());
            String before = "", after = "";
            if (title.length() != 0) {
                before = "<span title=\'"
                    + TransliteratorUtilities.toHTML.transliterate(title) + "'>";
                after = "</span>";
            }
            String qualifiers = qualifierSet.toString();
            if (!needQualifier || qualifierSet.isEmpty())
                qualifiers = "";
            else
                qualifiers = " " + qualifiers; // qualifiers = " (" + qualifiers + ")";

            // fix
            if (englishName.endsWith(", China")) {
                englishName = englishName.substring(0, englishName.length()
                    - ", China".length())
                    + " China";
            }

            result.append(before)
                .append(
                    TransliteratorUtilities.toHTML.transliterate(englishName
                        + qualifiers))
                .append(after);
        }
        return result.toString();
    }

    /**
     * @param localeID
     * @param isDraft
     *            TODO
     * @param draftLanguages
     * @param draftCountries
     * @param draftNativeLanguages
     * @param draftNativeCountries
     */
    private static void addCounts(String localeID, boolean isDraft, Set<String> draftLanguages, Set<String> draftCountries,
        Set<Object> draftNativeLanguages, Set<Object> draftNativeCountries) {
        // ULocale uloc = new ULocale(localeName);
        ltp.set(localeID);
        String lang = ltp.getLanguage();
        String langScript = ltp.getLanguageScript();
        String country = ltp.getRegion();

        // dump aliases
        // if ((country.equals("TW") || country.equals("HK") || country.equals("MO")) && lang.equals("zh")) return;
        // if (lang.equals("zh_Hans") || lang.equals("sr_Cyrl") || lang.equals("sh")) return;

        String nativeName, englishName;
        draftLanguages.add(lang);
        nativeName = getFixedLanguageName(localeID, langScript);
        englishName = english.getName(langScript);
        if (!lang.equals("en") && nativeName.equals(englishName)) {
            Log.logln((isDraft ? "D" : "") + "\tWarning: in " + localeID + ", display name for " + lang
                + " equals English: " + nativeName);
        }

        draftNativeLanguages.add(new LanguageList(langScript, englishName, fixedTitleCase("en", nativeName)));

        if (!country.equals("")) {
            draftCountries.add(country);
            nativeName = getFixedDisplayCountry(localeID, country);
            englishName = getFixedDisplayCountry("en", country);
            if (!lang.equals("en") && nativeName.equals(englishName)) {
                Log.logln((isDraft ? "D" : "") + "\tWarning: in " + localeID + ", display name for " + country
                    + " equals English: " + nativeName);
            }
            draftNativeCountries.add(new LanguageList(localeID, englishName, fixedTitleCase("en", nativeName)));
        }
    }

    private static class LanguageList implements Comparable<Object> {
        Object[] contents;
        static Collator col = Collator.getInstance(ULocale.ENGLISH);
        static Comparator<Object[]> comp = new ArrayComparator(new Collator[] { col, col, null });

        LanguageList(String locale, String englishName, String localName) {
            contents = new Object[] { englishName, locale, localName };
        }

        public int compareTo(Object o) {
            return comp.compare(contents, ((LanguageList) o).contents);
        }

        String getLocale() {
            return (String) contents[1];
        }

        String getEnglishName() {
            return (String) contents[0];
        }

        String getLocalName() {
            return (String) contents[2];
        }
    }

    static String fixedTitleCase(String localeID, String in) {
        if (notitlecase) return in;
        String result = UCharacter.toTitleCase(new ULocale(localeID), in, null);
        if (HACK) {
            result = GenerateCldrTests.replace(result, "U.s.", "U.S.");
            result = GenerateCldrTests.replace(result, "S.a.r.", "S.A.R.");
        }
        return result;
    }

    /*
     * static void addMapSet(Map m, Object key, Object value, Comparator com) {
     * Set valueSet = (Set) m.get(key);
     * if (valueSet == null) {
     * valueSet = new TreeSet(com);
     * m.put(key, valueSet);
     * }
     * valueSet.add(value);
     * }
     */

    /**
     *
     */
    private static String getFixedLanguageName(String localeID, String lang) {
        if (HACK) {
            if (localeID.equals("bs") || localeID.startsWith("bs_")) {
                if (lang.equals("bs") || lang.startsWith("bs_")) return "Bosanski";
            }
        }
        CLDRFile cldr = factory.make(localeID, true);
        return cldr.getName(lang);
    }

    /**
     * @param uloc
     * @return
     */
    private static String getFixedDisplayCountry(String localeID, String country) {
        if (HACK) {
            if (localeID.equals("bs") || localeID.startsWith("bs_")) {
                if (country.equals("BA"))
                    return "\u0411\u043E\u0441\u043D\u0430 \u0438 \u0425\u0435\u0440\u0446\u0435\u0433\u043E\u0432\u0438\u043D\u0430";
            }
        }
        CLDRFile cldr = factory.make(localeID, true);
        String name = cldr.getName("territory", country);
        if (false && HACK) {
            Object trial = fixCountryNames.get(name);
            if (trial != null) {
                return (String) trial;
            }
        }
        return name;
    }

    static Map<String, String> fixCountryNames = new HashMap<String, String>();
    static {
        fixCountryNames.put("\u0408\u0443\u0433\u043E\u0441\u043B\u0430\u0432\u0438\u0458\u0430",
            "\u0421\u0440\u0431\u0438\u0458\u0430 \u0438 \u0426\u0440\u043D\u0430 \u0413\u043E\u0440\u0430");
        fixCountryNames.put("Jugoslavija", "Srbija i Crna Gora");
        fixCountryNames.put("Yugoslavia", "Serbia and Montenegro");
    }
    public static final Transliterator toLatin = Transliterator.getInstance("any-latin");

    public static class DraftChecker {
        String dir;
        Map<String, Object> cache = new HashMap<String, Object>();
        Object TRUE = new Object();
        Object FALSE = new Object();

        public DraftChecker(String dir) {
            this.dir = dir;
        }

        public boolean isDraft(String localeName) {
            Object check = cache.get(localeName);
            if (check != null) {
                return check == TRUE;
            }
            BufferedReader pw = null;
            //boolean result = true;
            try {
                pw = FileUtilities.openUTF8Reader(dir, localeName + ".xml");
                while (true) {
                    String line = pw.readLine();
                    if (line == null) {
                        throw new IllegalArgumentException("Internal Error: should never get here.");
                    }
                    if (line.indexOf("<ldml") >= 0) {
                        if (line.indexOf("draft") >= 0) {
                            check = TRUE;
                        } else {
                            check = FALSE;
                        }
                        break;
                    }
                }
                pw.close();
            } catch (IOException e) {
                throw new ICUUncheckedIOException("Failure on " + localeName + ": " + dir + localeName + ".xml", e);
            }
            cache.put(localeName, check);
            return check == TRUE;
        }
    }

}