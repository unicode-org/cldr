/*
 **********************************************************************
 * Copyright (c) 2002-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import org.unicode.cldr.test.CoverageLevel;
import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.util.Iso639Data.Type;

import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.XEquivalenceClass;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;

/**
 * Provides access to various codes used by CLDR: RFC 3066, ISO 4217, Olson
 * tzids
 */
public class StandardCodes {
    public static final String NO_COUNTRY = "001";

    private static StandardCodes singleton;

    private Map type_code_data = new TreeMap();

    private Map type_name_codes = new TreeMap();

    private Map type_code_preferred = new TreeMap();

    private Map country_modernCurrency = new TreeMap();

    private Map goodCodes = new TreeMap();

    private String date;

    private static final boolean DEBUG = false;

    /**
     * Get the singleton copy of the standard codes.
     */
    static public synchronized StandardCodes make() {
        if (singleton == null)
            singleton = new StandardCodes();
        return singleton;
    }

    /**
     * The data is the name in the case of RFC3066 codes, and the country code in
     * the case of TZIDs and ISO currency codes. If the country code is missing,
     * uses ZZ.
     */
    public String getData(String type, String code) {
        Map code_data = (Map) type_code_data.get(type);
        if (code_data == null)
            return null;
        List list = (List) code_data.get(code);
        if (list == null)
            return null;
        return (String) list.get(0);
    }

    /**
     * @return the full data for the type and code For the data in lstreg, it is
     *         description | date | canonical_value | recommended_prefix #
     *         comments
     */
    public List getFullData(String type, String code) {
        Map code_data = (Map) type_code_data.get(type);
        if (code_data == null)
            return null;
        return (List) code_data.get(code);
    }

    /**
     * Get at the language registry values, as a Map from label to value.
     * @param type
     * @param code
     * @return
     */
    public Map<String,String> getLangData(String type, String code) {
        try {
            if (type.equals("territory")) type = "region";
            else if (type.equals("variant")) code = code.toLowerCase(Locale.ENGLISH);
            return (Map) ((Map)languageRegistry.get(type)).get(code);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Return a replacement code, if available. If not, return null.
     * 
     */
    public String getReplacement(String type, String code) {
        if (type.equals("currency"))
            return null; // no replacement codes for currencies
        List data = getFullData(type, code);
        if (data == null)
            return null;
        // if available, the replacement is a non-empty value other than --, in
        // position 2.
        if (data.size() < 3)
            return null;
        String replacement = (String) data.get(2);
        if (!replacement.equals("") && !replacement.equals("--"))
            return replacement;
        return null;
    }

    /**
     * Return the list of codes that have the same data. For example, returns all
     * currency codes for a country If there is a preferred one, it is first.
     * 
     * @param type
     * @param data
     * @return
     */
    public List getCodes(String type, String data) {
        Map data_codes = (Map) type_name_codes.get(type);
        if (data_codes == null)
            return null;
        return Collections.unmodifiableList((List) data_codes.get(data));
    }

    /**
     * Where there is a preferred code, return it.
     */
    public String getPreferred(String type, String code) {
        Map code_preferred = (Map) type_code_preferred.get(type);
        if (code_preferred == null)
            return code;
        String newCode = (String) code_preferred.get(code);
        if (newCode == null)
            return code;
        return newCode;
    }

    /**
     * Get all the available types
     */
    public Set<String> getAvailableTypes() {
        return Collections.unmodifiableSet(type_code_data.keySet());
    }

    /**
     * Get all the available codes for a given type
     * 
     * @param type
     * @return
     */
    public Set<String> getAvailableCodes(String type) {
      if ("region".equals(type)) type = "territory";
        Map code_name = (Map) type_code_data.get(type);
        if (code_name == null)
            return null;
        return Collections.unmodifiableSet(code_name.keySet());
    }

    /**
     * Get all the available "real" codes for a given type
     * 
     * @param type
     * @return
     */
    public Set<String> getGoodAvailableCodes(String type) {
        Set result = (Set) goodCodes.get(type);
        if (result == null) {
            Map code_name = (Map) type_code_data.get(type);
            if (code_name == null)
                return null;
            result = new TreeSet(code_name.keySet());
            if (type.equals("currency")) {
                for (Iterator it = result.iterator(); it.hasNext();) {
                    String code = (String) it.next();
                    List data = getFullData(type, code);
                    if ("X".equals(data.get(3))) {
                        // System.out.println("Removing: " + code);
                        it.remove();
                    }
                }
            } else if (!type.equals("tzid")) {
                for (Iterator it = result.iterator(); it.hasNext();) {
                    String code = (String) it.next();
                    if (code.equals("root") || code.equals("QO"))
                        continue;
                    List data = getFullData(type, code);
                    if (data.size() < 3) {
                        if (DEBUG)
                            System.out.println(code + "\t" + data);
                    }
                    if ("PRIVATE USE".equalsIgnoreCase(data.get(0).toString())
                            || (!data.get(2).equals("") && !data.get(2).equals("--"))) {
                        // System.out.println("Removing: " + code);
                        it.remove();
                    }
                }
            }
            result = Collections.unmodifiableSet(result);
            goodCodes.put(type, result);
        }
        return result;
    }

    /**
     * Gets the modern currency.
     */
    public Set getMainCurrencies(String countryCode) {
        return (Set) country_modernCurrency.get(countryCode);
    }

    private Map<String,Map<String,Level>> platform_locale_level = null;
    private Map<String,Map<String,String>> platform_locale_levelString = null;

    /**
     * Get rid of this
     * @param type
     * @return
     * @throws IOException
     * @deprecated
     */
    public String getEffectiveLocaleType(String type) throws IOException {
        if ((type != null) && (getLocaleCoverageOrganizations().contains(type))) {
            return type;
        } else {
            return "IBM"; // the default.. for now..
        }
    }

    static Comparator caseless = new Comparator() {

        public int compare(Object arg0, Object arg1) {
            String s1 = (String) arg0;
            String s2 = (String) arg1;
            return s1.compareToIgnoreCase(s2);
        }

    };

    /**
     * Returns locales according to status. It returns a Map of Maps, key 1 is
     * either IBM or Java (perhaps more later), key 2 is the Level.
     * @deprecated
     */
    public Map<String, Map<String, Level>> getLocaleTypes() {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_locale_level;
    }

    /**
     * Returns coverage level of locale according to organization. Returns Level.BASIC if information is missing.
     */
    public Level getLocaleCoverageLevel(String organization, String desiredLocale) {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        if (organization == null) return Level.BASIC;
        Map<String, Level> locale_status = platform_locale_level.get(organization);
        if (locale_status == null) return Level.BASIC;
        // see if there is a parent
        while (desiredLocale != null) {
            Level status = locale_status.get(desiredLocale);
            if (status != null && status != Level.UNDETERMINED) return status;
            desiredLocale = LocaleIDParser.getParent(desiredLocale);
        }
        return Level.BASIC;
    }

    public Set<String> getLocaleCoverageOrganizations() {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_locale_level.keySet();
    }

    public Set<String> getLocaleCoverageLocales(String organization) {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_locale_level.get(organization).keySet();
    }

    private void loadPlatformLocaleStatus() {
        LocaleIDParser parser = new LocaleIDParser();
        platform_locale_level = new TreeMap(caseless);
        String line;
        try {
            BufferedReader lstreg = CldrUtility.getUTF8Data("Locales.txt");
            while (true) {
                line = lstreg.readLine();
                if (line == null)
                    break;
                int commentPos = line.indexOf('#');
                if (commentPos >= 0)
                    line = line.substring(0, commentPos);
                if (line.length() == 0)
                    continue;
                List stuff = CldrUtility.splitList(line, ';', true);
                String organization = (String) stuff.get(0);
                String locale = (String) stuff.get(1);
                Level status = Level.get((String) stuff.get(2));
                if (status == Level.UNDETERMINED) {
                    System.out.println("Warning: Level unknown on: " + line);
                }
                Map locale_status = (Map) platform_locale_level.get(organization);
                if (locale_status == null) {
                    platform_locale_level.put(organization, locale_status = new TreeMap());
                }
                parser.set(locale);
                String valid = validate(parser);
                if (valid.length() != 0) {
                    System.out.println("Warning: " + valid + "; " + line);
                }
                locale = parser.toString(); // normalize
                locale_status.put(locale, status);
                String scriptLoc = parser.getLanguageScript();
                if (locale_status.get(scriptLoc) == null)
                    locale_status.put(scriptLoc, status);
                String lang = parser.getLanguage();
                if (locale_status.get(lang) == null)
                    locale_status.put(lang, status);
            }
        } catch (IOException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Internal Error").initCause(e);
        }

        // now reset the parent to be the max of the children
        for (String platform : platform_locale_level.keySet()) {
            Map<String, Level> locale_level = platform_locale_level.get(platform);
            for (String locale : locale_level.keySet()) {
                parser.set(locale);
                Level childLevel = locale_level.get(locale);

                String language = parser.getLanguage();
                if (!language.equals(locale)) {
                    CoverageLevel.Level languageLevel = (CoverageLevel.Level) locale_level.get(language);
                    if (languageLevel == null || languageLevel.compareTo(childLevel) < 0) {
                        locale_level.put(language, childLevel);
                    }
                }
                String oldLanguage = language;
                language = parser.getLanguageScript();
                if (!language.equals(oldLanguage)) {
                    CoverageLevel.Level languageLevel = (CoverageLevel.Level) locale_level.get(language);
                    if (languageLevel == null || languageLevel.compareTo(childLevel) < 0) {
                        locale_level.put(language, childLevel);
                    }
                }
            }
        }
        // backwards compat hack
        platform_locale_levelString = new TreeMap(caseless);
        for (String platform : platform_locale_level.keySet()) {
            Map<String,String> locale_levelString = new TreeMap();
            platform_locale_levelString.put(platform, locale_levelString);
            Map<String, Level> locale_level = platform_locale_level.get(platform);
            for (String locale : locale_level.keySet()) {
                locale_levelString.put(locale, locale_level.get(locale).toString());
            }
        }
        platform_locale_level = CldrUtility.protectCollection(platform_locale_level);
        platform_locale_levelString = CldrUtility.protectCollection(platform_locale_levelString);
    }

    private String validate(LocaleIDParser parser) {
        String message = "";
        String lang = parser.getLanguage();
        if (lang.length() == 0) {
            message += ", Missing language";
        } else if (!getAvailableCodes("language").contains(lang)) {
            message += ", Invalid language code: " + lang;
        }
        String script = parser.getScript();
        if (script.length() != 0 && !getAvailableCodes("script").contains(script)) {
            message += ", Invalid script code: " + script;
        }
        String territory = parser.getRegion();
        if (territory.length() != 0 && !getAvailableCodes("territory").contains(territory)) {
            message += ", Invalid territory code: " + lang;
        }
        return message.length() == 0 ? message : message.substring(2);
    }

    /**
     * Ascertain that the given locale in in the given group specified by the
     * organization
     * 
     * @param locale
     * @param group
     * @param org
     * @return boolean
     */
    public boolean isLocaleInGroup(String locale, String group, String org) {
        return group.equals(getGroup(locale,org));
    }

    /**
     * Gets the coverage group given a locale and org
     * 
     * @param locale
     * @param org
     * @return group if availble, null if not
     */
    public String getGroup(String locale, String org) {
        return getLocaleCoverageLevel(org,locale).toString();
    }

    // ========== PRIVATES ==========
    static Map languageRegistry;

    private StandardCodes() {
        String[] files = {/* "lstreg.txt", */"ISO4217.txt" }; // , "TZID.txt"
        type_code_preferred.put("tzid", new TreeMap());
        add("language", "root", "Root");
        String originalLine = null;
        for (int fileIndex = 0; fileIndex < files.length; ++fileIndex) {
            try {
                BufferedReader lstreg = CldrUtility.getUTF8Data(files[fileIndex]);
                while (true) {
                    String line = originalLine = lstreg.readLine();
                    if (line == null)
                        break;
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    line = line.trim();
                    int commentPos = line.indexOf('#');
                    String comment = "";
                    if (commentPos >= 0) {
                        comment = line.substring(commentPos + 1).trim();
                        line = line.substring(0, commentPos);
                    }
                    if (line.length() == 0)
                        continue;
                    List pieces = (List) CldrUtility.splitList(line, '|', true,
                            new ArrayList());
                    String type = (String) pieces.get(0);
                    pieces.remove(0);
                    if (type.equals("region"))
                        type = "territory";

                    String code = (String) pieces.get(0);
                    pieces.remove(0);
                    if (type.equals("date")) {
                        date = code;
                        continue;
                    }

                    String oldName = (String) pieces.get(0);
                    int pos = oldName.indexOf(';');
                    if (pos >= 0) {
                        oldName = oldName.substring(0, pos).trim();
                        pieces.set(0, oldName);
                    }

                    List data = pieces;
                    if (comment.indexOf("deprecated") >= 0) {
                        // System.out.println(originalLine);
                        if (data.get(2).toString().length() == 0) {
                            data.set(2, "--");
                        }
                    }
                    if (oldName.equalsIgnoreCase("PRIVATE USE")) {
                        int separatorPos = code.indexOf("..");
                        if (separatorPos < 0) {
                            add(type, code, data);
                        } else {
                            String current = code.substring(0, separatorPos);
                            String end = code.substring(separatorPos + 2);
                            // System.out.println(">>" + code + "\t" + current + "\t" + end);
                            for (; current.compareTo(end) <= 0; current = nextAlpha(current)) {
                                // System.out.println(">" + current);
                                add(type, current, data);
                            }
                        }
                        continue;
                    }
                    if (!type.equals("tzid")) {
                        add(type, code, data);
                        if (type.equals("currency")) {
                            // currency | TPE | Timor Escudo | TP | EAST TIMOR | O
                            if (data.get(3).equals("C")) {
                                String country = (String) data.get(1);
                                Set codes = (Set) country_modernCurrency.get(country);
                                if (codes == null) {
                                    country_modernCurrency.put(country, codes = new TreeSet());
                                }
                                codes.add(code);
                            }
                        }
                        continue;
                    }
                    // type = tzid
                    // List codes = (List) Utility.splitList(code, ',', true, new
                    // ArrayList());
                    String preferred = null;
                    for (int i = 0; i < pieces.size(); ++i) {
                        code = (String) pieces.get(i);
                        add(type, code, data);
                        if (preferred == null)
                            preferred = code;
                        else {
                            Map code_preferred = (Map) type_code_preferred.get(type);
                            code_preferred.put(code, preferred);
                        }
                    }
                }
                lstreg.close();
            } catch (Exception e) {
                System.err.println("WARNING: " + files[fileIndex]
                                                       + " may be a corrupted UTF-8 file. Please check.");
                throw (IllegalArgumentException) new IllegalArgumentException(
                        "Can't read " + files[fileIndex] + "\t" + originalLine)
                .initCause(e);
            }
            country_modernCurrency = CldrUtility.protectCollection(country_modernCurrency);
        }

        // data is: description | date | canonical_value | recommended_prefix #
        // comments
        // HACK, just rework

        languageRegistry = getLStreg();
        languageRegistry = CldrUtility.protectCollection(languageRegistry);

        for (Iterator it = languageRegistry.keySet().iterator(); it.hasNext();) {
            String type = (String) it.next();
            String type2 = type.equals("region") ? "territory" : type;
            Map m = (Map) languageRegistry.get(type);
            for (Iterator it2 = m.keySet().iterator(); it2.hasNext();) {
                String code = (String) it2.next();
                Map mm = (Map) m.get(code);
                List data = new ArrayList(0);
                data.add(mm.get("Description"));
                data.add(mm.get("Added"));
                String pref = (String) mm.get("Preferred-Value");
                if (pref == null) {
                    pref = (String) mm.get("Deprecated");
                    if (pref == null)
                        pref = "";
                    else
                        pref = "deprecated";
                }
                data.add(pref);
                if (type.equals("variant")) {
                    code = code.toUpperCase();
                }
                // data.add(mm.get("Recommended_Prefix"));
                // {"region", "BQ", "Description", "British Antarctic Territory",
                // "Preferred-Value", "AQ", "CLDR", "True", "Deprecated", "True"},
                add(type2, code, data);
            }
        }

        Map m = getZoneData();
        for (Iterator it = m.keySet().iterator(); it.hasNext();) {
            String code = (String) it.next();
            add("tzid", code, m.get(code).toString());
        }
    }

    /**
     * @param current
     * @return
     */
    private static String nextAlpha(String current) {
        // Don't care that this is inefficient
        int value = 0;
        for (int i = 0; i < current.length(); ++i) {
            char c = current.charAt(i);
            c -= c < 'a' ? 'A' : 'a';
            value = value * 26 + c;
        }
        value += 1;
        String result = "";
        for (int i = 0; i < current.length(); ++i) {
            result = (char) ((value % 26) + 'A') + result;
            value = value / 26;
        }
        if (UCharacter.toLowerCase(current).equals(current)) {
            result = UCharacter.toLowerCase(result);
        } else if (UCharacter.toUpperCase(current).equals(current)) {
            // do nothing
        } else {
            result = UCharacter.toTitleCase(result, null);
        }
        return result;
    }

    /**
     * @param string
     * @param string2
     * @param string3
     */
    private void add(String string, String string2, String string3) {
        List l = new ArrayList();
        l.add(string3);
        add(string, string2, l);
    }

    private void add(String type, String code, List otherData) {
        // hack
        if (type.equals("script")) {
            if (code.equals("Qaai")) {
                otherData = new ArrayList(otherData);
                otherData.set(0, "Inherited");
            } else if (code.equals("Zyyy")) {
                otherData = new ArrayList(otherData);
                otherData.set(0, "Common");
            }
        }

        // assume name is the first item

        String name = (String) otherData.get(0);

        // add to main list
        Map code_data = (Map) type_code_data.get(type);
        if (code_data == null) {
            code_data = new TreeMap();
            type_code_data.put(type, code_data);
        }
        List lastData = (List) code_data.get(code);
        if (lastData != null) {
            lastData.addAll(otherData);
        } else {
            code_data.put(code, otherData);
        }

        // now add mapping from name to codes
        Map name_codes = (Map) type_name_codes.get(type);
        if (name_codes == null) {
            name_codes = new TreeMap();
            type_name_codes.put(type, name_codes);
        }
        List codes = (List) name_codes.get(name);
        if (codes == null) {
            codes = new ArrayList();
            name_codes.put(name, codes);
        }
        codes.add(code);
    }

    private List DELETED3166 = Collections.unmodifiableList(Arrays
            .asList(new String[] { "BQ", "BU", "CT", "DD", "DY", "FQ", "FX", "HV",
                    "JT", "MI", "NH", "NQ", "NT", "PC", "PU", "PZ", "RH", "SU", "TP",
                    "VD", "WK", "YD", "YU", "ZR" }));

    public List getOld3166() {
        return DELETED3166;
    }

    private Map WorldBankInfo;

    public Map getWorldBankInfo() {
        if (WorldBankInfo == null) {
            List temp = fillFromCommaFile(CldrUtility.UTIL_DATA_DIR, "WorldBankInfo.txt", false);
            WorldBankInfo = new HashMap();
            for (Iterator it = temp.iterator(); it.hasNext();) {
                String line = (String) it.next();
                List row = CldrUtility.splitList(line, ';', true);
                String key = (String) row.get(0);
                row.remove(0);
                WorldBankInfo.put(key, row);
            }
            WorldBankInfo = CldrUtility.protectCollection(WorldBankInfo);
        }
        return WorldBankInfo;
    }

    Set MainTimeZones;

    public Set getMainTimeZones() {
        if (MainTimeZones == null) {
            List temp = fillFromCommaFile(CldrUtility.UTIL_DATA_DIR, "MainTimeZones.txt", false);
            MainTimeZones = new TreeSet();
            MainTimeZones.addAll(temp);
            MainTimeZones = CldrUtility.protectCollection(MainTimeZones);
        }
        return MainTimeZones;
    }

    Set moribundLanguages;

    public Set<String> getMoribundLanguages() {
        if (moribundLanguages == null) {
            List temp = fillFromCommaFile(CldrUtility.UTIL_DATA_DIR, "moribund_languages.txt", true);
            moribundLanguages = new TreeSet<String>();
            moribundLanguages.addAll(temp);
            moribundLanguages = CldrUtility.protectCollection(moribundLanguages);
        }
        return moribundLanguages;
    }

    // produces a list of the 'clean' lines
    private List fillFromCommaFile(String dir, String filename, boolean trim) {
        try {
            List result = new ArrayList();
            String line;
            BufferedReader lstreg = BagFormatter.openUTF8Reader(dir, filename);
            while (true) {
                line = lstreg.readLine();
                if (line == null)
                    break;
                int commentPos = line.indexOf('#');
                if (commentPos >= 0) {
                    line = line.substring(0, commentPos);
                }
                if (trim) {
                    line = line.trim();
                }    
                if (line.length() == 0)
                    continue;
                result.add(line);
            }
            return result;
        } catch (Exception e) {
            throw (RuntimeException) new IllegalArgumentException(
                    "Can't process file: " + dir + filename).initCause(e);
        }
    }

    // return a complex map. language -> arn -> {"Comments" -> "x",
    // "Description->y,...}
    static String[][] extras = {
        { "language", "root", "Description", "Root", "CLDR", "True" },
        { "language", "cch", "Description", "Atsam", "CLDR", "True" },
        { "language", "kaj", "Description", "Jju", "CLDR", "True" },
        { "language", "kcg", "Description", "Tyap", "CLDR", "True" },
        { "language", "kfo", "Description", "Koro", "CLDR", "True" },
        { "language", "mfe", "Description", "Morisyen", "CLDR", "True" },
        { "region", "172", "Description", "Commonwealth of Independent States", "CLDR", "True" },
        { "region", "062", "Description", "South-Central Asia", "CLDR", "True" },
        { "region", "003", "Description", "North America", "CLDR", "True" },
        { "variant", "POLYTONI", "Description", "Polytonic Greek", "CLDR", "True", "Preferred-Value", "POLYTON" },
        { "variant", "REVISED", "Description", "Revised Orthography", "CLDR",
        "True" },
        { "variant", "SAAHO", "Description", "Dialect", "CLDR", "True" },
        { "variant", "POSIX", "Description", "Computer-Style", "CLDR", "True" },
        // {"region", "172", "Description", "Commonwealth of Independent States",
        // "CLDR", "True"},
        { "region", "QU", "Description", "European Union", "CLDR", "True" },
        { "region", "ZZ", "Description", "Unknown or Invalid Region", "CLDR", "True" },
        { "region", "QO", "Description", "Outlying Oceania", "CLDR", "True" },
        { "script", "Qaai", "Description", "Inherited", "CLDR", "True" },
        // {"region", "003", "Description", "North America", "CLDR", "True"},
        // {"region", "062", "Description", "South-central Asia", "CLDR", "True"},
        // {"region", "200", "Description", "Czechoslovakia", "CLDR", "True"},
        // {"region", "830", "Description", "Channel Islands", "CLDR", "True"},
        // {"region", "833", "Description", "Isle of Man", "CLDR", "True"},

        // {"region", "NT", "Description", "Neutral Zone (formerly between Saudi
        // Arabia & Iraq)", "CLDR", "True", "Deprecated", "True"},
        // {"region", "SU", "Description", "Union of Soviet Socialist Republics",
        // "CLDR", "True", "Deprecated", "True"},
        // {"region", "BQ", "Description", "British Antarctic Territory",
        // "Preferred-Value", "AQ", "CLDR", "True", "Deprecated", "True"},
        // {"region", "CT", "Description", "Canton and Enderbury Islands",
        // "Preferred-Value", "KI", "CLDR", "True", "Deprecated", "True"},
        // {"region", "FQ", "Description", "French Southern and Antarctic Territories
        // (now split between AQ and TF)", "CLDR", "True", "Deprecated", "True"},
        // {"region", "JT", "Description", "Johnston Island", "Preferred-Value", "UM",
        // "CLDR", "True", "Deprecated", "True"},
        // {"region", "MI", "Description", "Midway Islands", "Preferred-Value", "UM",
        // "CLDR", "True", "Deprecated", "True"},
        // {"region", "NQ", "Description", "Dronning Maud Land", "Preferred-Value",
        // "AQ", "CLDR", "True", "Deprecated", "True"},
        // {"region", "PC", "Description", "Pacific Islands Trust Territory (divided
        // into FM, MH, MP, and PW)", "Preferred-Value", "AQ", "CLDR", "True",
        // "Deprecated", "True"},
        // {"region", "PU", "Description", "U.S. Miscellaneous Pacific Islands",
        // "Preferred-Value", "UM", "CLDR", "True", "Deprecated", "True"},
        // {"region", "PZ", "Description", "Panama Canal Zone", "Preferred-Value",
        // "PA", "CLDR", "True", "Deprecated", "True"},
        // {"region", "VD", "Description", "North Vietnam", "Preferred-Value", "VN",
        // "CLDR", "True", "Deprecated", "True"},
        // {"region", "WK", "Description", "Wake Island", "Preferred-Value", "UM",
        // "CLDR", "True", "Deprecated", "True"},
    };

    public static Map<String,Map<String,Map<String,String>>> getLStreg() {

      Map<String,Map<String,Map<String,String>>> result = new TreeMap();

        int lineNumber = 1;

        Set funnyTags = new TreeSet();
        String line;
        String registryName = CldrUtility.getProperty("registry", "language-subtag-registry");
        try {
            BufferedReader lstreg = CldrUtility.getUTF8Data(registryName);
            boolean started = false;
            String lastType = null;
            String lastTag = null;
            Map<String,Map<String,String>> subtagData = null;
            Map<String,String> currentData = null;
            String lastLabel = null;
            String lastRest = null;
            boolean inRealContent = false;
            for (; ; ++lineNumber) {
                line = lstreg.readLine();
                if (line == null)
                    break;
                if (line.length() == 0)
                    continue; // skip blanks
                if (line.startsWith("File-Date: ")) {
                    if (DEBUG) System.out.println("Language Subtag Registry: " + line);
                    inRealContent = true;
                    continue;
                }
                if (!inRealContent) {
                    // skip until we get to real content
                    continue;
                }
                // skip cruft
                if (line.startsWith("Internet-Draft")) {
                    continue;
                }
                if (line.startsWith("Ewell")) {
                    continue;
                }
                if (line.startsWith("\f")) {
                    continue;
                }
                if (line.startsWith("4.  Security Considerations")) {
                    break;
                }

                if (line.startsWith("%%"))
                    continue; // skip separators (ok, since data starts with Type:
                if (line.startsWith(" ")) {
                    currentData.put(lastLabel, lastRest + " " + line.trim());
                    continue;
                }

                /*
                 * Type: language Subtag: aa Description: Afar Added: 2005-10-16
                 * Suppress-Script: Latn
                 */
                int pos2 = line.indexOf(':');
                String label = line.substring(0, pos2).trim();
                String rest = line.substring(pos2 + 1).trim();
                if (label.equalsIgnoreCase("Type")) {
                    subtagData = (Map) result.get(lastType = rest);
                    if (subtagData == null)
                        result.put(rest, subtagData = new TreeMap());
                } else if (label.equalsIgnoreCase("Subtag")
                        || label.equalsIgnoreCase("Tag")) {
                    lastTag = rest;
                    String endTag = null;
                    int pos = lastTag.indexOf("..");
                    if (pos >= 0) {
                        endTag = lastTag.substring(pos + 2);
                        lastTag = lastTag.substring(0, pos);
                    }
                    currentData = new TreeMap();
                    if (endTag == null) {
                        putSubtagData(lastTag, subtagData, currentData);
                        languageCount.add(lastType, 1);
                        //System.out.println(languageCount.getCount(lastType) + "\t" + lastType + "\t" + lastTag);
                    } else {
                        for (; lastTag.compareTo(endTag) <= 0; lastTag = nextAlpha(lastTag)) {
                            //System.out.println(">" + current);
                            putSubtagData(lastTag, subtagData, currentData);
                            languageCount.add(lastType, 1);
                            //System.out.println(languageCount.getCount(lastType) + "\t" + lastType + "\t" + lastTag);
                        }

                    }
                    //label.equalsIgnoreCase("Added") || label.equalsIgnoreCase("Suppress-Script")) {
                    // skip
                    //} else if (pieces.length < 2) {
                    //	System.out.println("Odd Line: " + lastType + "\t" + lastTag + "\t" + line);
                } else {
                    lastLabel = label.intern();
                    lastRest = TransliteratorUtilities.fromXML.transliterate(rest);
                    String oldValue = (String) currentData.get(lastLabel);
                    if (oldValue != null) {
                        lastRest = oldValue + "\u25AA" + lastRest;
                    }
                    currentData.put(lastLabel, lastRest);
                }
            }
        } catch (Exception e) {
            throw (RuntimeException) new IllegalArgumentException(
                    "Can't process file: " + CldrUtility.UTIL_DATA_DIR
                    + registryName + ";\t at line " + lineNumber).initCause(e);
        } finally {
            if (!funnyTags.isEmpty()) {
                if (DEBUG)
                    System.out.println("Funny tags: " + funnyTags);
            }
        }
        // add extras
        for (int i = 0; i < extras.length; ++i) {
            Map subtagData = (Map) result.get(extras[i][0]);
            if (subtagData == null)
                result.put(extras[i][0], subtagData = new TreeMap());
            Map labelData = new TreeMap();
            for (int j = 2; j < extras[i].length; j += 2) {
                labelData.put(extras[i][j], extras[i][j + 1]);
            }
            subtagData.put(extras[i][1], labelData);
        }
        return result;
    }

    private static Object putSubtagData(String lastTag, Map subtagData, Map currentData) {
        Map oldData = (Map) subtagData.get(lastTag);
        if (oldData != null) {
            if (oldData.get("CLDR") != null) {
                System.out.println("overriding: " + lastTag + ", " + oldData);
            } else {
                throw new IllegalArgumentException("Duplicate tag: " + lastTag);
            }
        }
        return subtagData.put(lastTag, currentData);
    }

    static Counter languageCount = new Counter();

    public static Counter getLanguageCount() {
        return languageCount;
    }

    ZoneParser zoneParser = new ZoneParser();

    static public final Set<String> MODERN_SCRIPTS = Collections
    .unmodifiableSet(new TreeSet(
            //"Bali " +
            // "Bugi " +
            //"Copt " +
            //"Hano " +
            //"Osma " +
            // "Qaai " +
            // "Sylo " +
            // "Syrc " +
            // "Tagb " +
            //"Tglg " +
            Arrays
            .asList("Hans Hant Jpan Hrkt Kore Arab Armn Bali Beng Bopo Cans Cham Cher Cyrl Deva Ethi Geor Grek Gujr Guru Hani Hang Hebr Hira Knda Kana Kali Khmr Laoo Latn Lepc Limb Mlym Mong Mymr Talu Nkoo Olck Orya Saur Sinh Tale Taml Telu Thaa Thai Tibt Tfng Vaii Yiii".split("\\s+"))));
    // updated to http://www.unicode.org/reports/tr31/tr31-9.html#Specific_Character_Adjustments

    public Map getZone_rules() {
        return zoneParser.getZone_rules();
    }

    public Map getZoneData() {
        return zoneParser.getZoneData();
    }

    public Map getCountryToZoneSet() {
        return zoneParser.getCountryToZoneSet();
    }

    public List getDeprecatedZoneIDs() {
        return zoneParser.getDeprecatedZoneIDs();
    }

    public Comparator getTZIDComparator() {
        return zoneParser.getTZIDComparator();
    }

    public Map getZoneLinkNew_OldSet() {
        return zoneParser.getZoneLinkNew_OldSet();
    }

    public Map getZoneLinkold_new() {
        return zoneParser.getZoneLinkold_new();
    }

    public Map getZoneRuleID_rules() {
        return zoneParser.getZoneRuleID_rules();
    }

    public Map getZoneToCounty() {
        return zoneParser.getZoneToCounty();
    }

    public String getZoneVersion() {
        return zoneParser.getVersion();
    }

    public static String fixLanguageTag(String languageSubtag) {
        if (languageSubtag.equals("mo")) { // fix special cases
            return "ro";
        } else if (languageSubtag.equals("no")) {
            return "nb";
        }
        return languageSubtag;
    }

    public boolean isModernLanguage(String languageCode) {
        if (getMoribundLanguages().contains(languageCode)) return false;
        Type type = Iso639Data.getType(languageCode);
        if (type == Type.Living) return true;
        if (languageCode.equals("eo")) return true; // exception for Esperanto
        //    Scope scope = Iso639Data.getScope(languageCode);
        //    if (scope == Scope.Collection) return false;
        return false;
    }

    public static boolean isScriptModern(String script) {
        return StandardCodes.MODERN_SCRIPTS.contains(script);
    }

    static final Pattern whitespace = Pattern.compile("\\s+");
    
    static final Set<String> filteredLanguages = Collections
            .unmodifiableSet(new TreeSet(
            Arrays
            .asList(whitespace.split("ab af am ar as asa ay az be bem bez bg bm bn bo bs ca " +
            		"cs cy da de dv dz ebu efi el en eo es et eu fa fi fil fj fr " +
            		"ga gl gn gsw gu ha haw he hi hr ht hu hy id ig is it ja jv " +
            		"ka kea kk km kn ko ks ku ky la lah lb ln lo lt luy lv " +
            		"mg mi mk ml mn mr ms mt mul my nb nd ne nl nn no nso ny or os " +
            		"pa pl ps pt qu rm rn ro rof ru rw rwk sa sd se sg sh si sk sl sm sn so sq sr ss st su sv sw swb " +
            		"ta te tet tg th ti tk tl tn to tpi tr ts ty ug uk und ur uz ve vi wo xh yo yue zh zu zxx"))));
    static final Set<String> filteredScripts = Collections
            .unmodifiableSet(new TreeSet(
            Arrays
            .asList(whitespace.split("Arab Armn Beng Cyrl Deva Ethi Geor Grek Gujr Guru Hebr Hani Hans Hant Bopo " +
            "Jpan Hira Kana Knda Khmr Kore Hang Laoo Latn Mlym Mong Mymr Orya Sinh Taml " +
            "Telu Thaa Thai Tibt Zyyy Zsym Zxxx Zzzz Brai"))));
    static Set<String> filteredCurrencies = null;
    
    public Set<String> getSurveyToolDisplayCodes(String type) {
        // TODO return filtered list of codes
        // TODO fix QU => EU
        if (type.equals("language")) return filteredLanguages;
        if (type.equals("script")) return filteredScripts;
        if (type.equals("currency")) {
            synchronized (StandardCodes.class) {
                if (filteredCurrencies == null) {
                    filteredCurrencies = new TreeSet();
                    for (String country : make().getGoodAvailableCodes("territory")) {
                        Set mainCurrencies = getMainCurrencies(country);
                        if (mainCurrencies != null) {
                            filteredCurrencies.addAll(mainCurrencies);
                        }
                    }
                    filteredCurrencies = Collections.unmodifiableSet(filteredCurrencies);
                }
            }
            return filteredCurrencies;
        }
        return getGoodAvailableCodes(type);
    }
}
