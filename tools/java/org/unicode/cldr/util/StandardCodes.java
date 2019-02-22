/*
 **********************************************************************
 * Copyright (c) 2002-2011, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.ZoneParser.ZoneLine;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;

/**
 * Provides access to various codes used by CLDR: RFC 3066, ISO 4217, Olson
 * tzids
 */
public class StandardCodes {

    public enum CodeType {
        language, script, territory, extlang, grandfathered, redundant, variant, currency, tzid;
        public static CodeType from(String name) {
            if ("region".equals(name)) {
                return territory;
            }
            return CodeType.valueOf(name);
        }
    }

    private static final Set<CodeType> TypeSet = Collections.unmodifiableSet(EnumSet.allOf(CodeType.class));

    private static final Set<String> TypeStringSet;
    static {
        LinkedHashSet<String> foo = new LinkedHashSet<String>();
        for (CodeType x : CodeType.values()) {
            foo.add(x.toString());
        }
        TypeStringSet = Collections.unmodifiableSet(foo);
    }

    public static final String DESCRIPTION_SEPARATOR = "\u25AA";

    public static final String NO_COUNTRY = "001";

    private static StandardCodes singleton;

    private EnumMap<CodeType, Map<String, List<String>>> type_code_data = new EnumMap<CodeType, Map<String, List<String>>>(
        CodeType.class);

    private EnumMap<CodeType, Map<String, List<String>>> type_name_codes = new EnumMap<CodeType, Map<String, List<String>>>(
        CodeType.class);

    private EnumMap<CodeType, Map<String, String>> type_code_preferred = new EnumMap<CodeType, Map<String, String>>(
        CodeType.class);

    private Map<String, Set<String>> country_modernCurrency = new TreeMap<String, Set<String>>();

    private Map<CodeType, Set<String>> goodCodes = new TreeMap<CodeType, Set<String>>();

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
        Map<String, List<String>> code_data = getCodeData(type);
        if (code_data == null)
            return null;
        List<String> list = code_data.get(code);
        if (list == null)
            return null;
        return list.get(0);
    }

    /**
     * @return the full data for the type and code For the data in lstreg, it is
     *         description | date | canonical_value | recommended_prefix #
     *         comments
     */
    public List<String> getFullData(String type, String code) {
        Map<String, List<String>> code_data = getCodeData(type);
        if (code_data == null)
            return null;
        return code_data.get(code);
    }

    /**
     * @return the full data for the type and code For the data in lstreg, it is
     *         description | date | canonical_value | recommended_prefix #
     *         comments
     */
    public List<String> getFullData(CodeType type, String code) {
        Map<String, List<String>> code_data = type_code_data.get(type);
        if (code_data == null)
            return null;
        return code_data.get(code);
    }

    private Map<String, List<String>> getCodeData(String type) {
        return getCodeData(CodeType.from(type));
    }

    private Map<String, List<String>> getCodeData(CodeType type) {
        return type_code_data.get(type);
    }

    /**
     * Get at the language registry values, as a Map from label to value.
     *
     * @param type
     * @param code
     * @return
     */
    public Map<String, String> getLangData(String type, String code) {
        try {
            if (type.equals("territory"))
                type = "region";
            else if (type.equals("variant")) code = code.toLowerCase(Locale.ENGLISH);
            return (Map) ((Map) getLStreg().get(type)).get(code);
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
        List<String> data = getFullData(type, code);
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
     * currency codes for a country. If there is a preferred one, it is first.
     *
     * @param type
     * @param data
     * @return
     */
    @Deprecated
    public List<String> getCodes(String type, String data) {
        return getCodes(CodeType.valueOf(type), data);
    }

    /**
     * Return the list of codes that have the same data. For example, returns all
     * currency codes for a country. If there is a preferred one, it is first.
     */
    public List<String> getCodes(CodeType type, String data) {
        Map<String, List<String>> data_codes = type_name_codes.get(type);
        if (data_codes == null)
            return null;
        return Collections.unmodifiableList(data_codes.get(data));
    }

    /**
     * Where there is a preferred code, return it.
     */
    @Deprecated
    public String getPreferred(String type, String code) {
        return getPreferred(CodeType.valueOf(type), code);
    }

    /**
     * Where there is a preferred code, return it.
     */

    public String getPreferred(CodeType type, String code) {
        Map<String, String> code_preferred = type_code_preferred.get(type);
        if (code_preferred == null)
            return code;
        String newCode = code_preferred.get(code);
        if (newCode == null)
            return code;
        return newCode;
    }

    /**
     * Get all the available types
     */
    public Set<String> getAvailableTypes() {
        return TypeStringSet;
    }

    /**
     * Get all the available types
     */
    public Set<CodeType> getAvailableTypesEnum() {
        return TypeSet;
    }

    /**
     * Get all the available codes for a given type
     *
     * @param type
     * @return
     */
    public Set<String> getAvailableCodes(String type) {
        return getAvailableCodes(CodeType.from(type));
    }

    /**
     * Get all the available codes for a given type
     *
     * @param type
     * @return
     */
    public Set<String> getAvailableCodes(CodeType type) {
        Map<String, List<String>> code_name = type_code_data.get(type);
        return Collections.unmodifiableSet(code_name.keySet());
    }

    public Set<String> getGoodAvailableCodes(String stringType) {
        return getGoodAvailableCodes(CodeType.from(stringType));
    }

    /**
     * Get all the available "real" codes for a given type, excluding private use,
     * but including some deprecated codes. Use SupplementalDataInfo getLocaleAliases to
     * exclude others.
     *
     * @param type
     * @return
     */
    public Set<String> getGoodAvailableCodes(CodeType type) {
        Set<String> result = goodCodes.get(type);
        if (result == null) {
            synchronized (goodCodes) {
                Map<String, List<String>> code_name = getCodeData(type);
                SupplementalDataInfo sd = SupplementalDataInfo.getInstance();
                if (code_name == null)
                    return null;
                result = new TreeSet<String>(code_name.keySet());
                switch (type) {
                case currency:
                    break; // nothing special                    
                case language:
                    return sd.getCLDRLanguageCodes();
                case script:
                    return sd.getCLDRScriptCodes();
                case tzid:
                    break; // nothing special
                default:
                    for (Iterator<String> it = result.iterator(); it.hasNext();) {
                        String code = (String) it.next();
                        if (code.equals("root") || code.equals("QO"))
                            continue;
                        List<String> data = getFullData(type, code);
                        if (data.size() < 3) {
                            if (DEBUG)
                                System.out.println(code + "\t" + data);
                        }
                        if ("PRIVATE USE".equalsIgnoreCase(data.get(0))
                            || (!data.get(2).equals("") && !data.get(2).equals("--"))) {
                            // System.out.println("Removing: " + code);
                            it.remove();
                        }
                    }
                }
                result = Collections.unmodifiableSet(result);
                goodCodes.put(type, result);
            }
        }
        return result;
    }

    private static Set<String> GOOD_COUNTRIES;

    public Set<String> getGoodCountries() {
        synchronized (goodCodes) {
            if (GOOD_COUNTRIES == null) {
                Set<String> temp = new LinkedHashSet<String>();
                for (String s : getGoodAvailableCodes(CodeType.territory)) {
                    if (isCountry(s)) {
                        temp.add(s);
                    }
                }
                GOOD_COUNTRIES = Collections.unmodifiableSet(temp);
            }
        }
        return GOOD_COUNTRIES;
    }

    /**
     * Gets the modern currency.
     */
    public Set<String> getMainCurrencies(String countryCode) {
        return country_modernCurrency.get(countryCode);
    }

    private EnumMap<Organization, Map<String, Level>> platform_locale_level = null;
    private EnumMap<Organization, Relation<Level, String>> platform_level_locale = null;
    private Map<String, Map<String, String>> platform_locale_levelString = null;

//    /**
//     * Get rid of this
//     *
//     * @param type
//     * @return
//     * @throws IOException
//     * @deprecated
//     */
//    public String getEffectiveLocaleType(String type) throws IOException {
//        if ((type != null) && (getLocaleCoverageOrganizations().contains(Organization.valueOf(type)))) {
//            return type;
//        } else {
//            return null; // the default.. for now..
//        }
//    }

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
     *
     * @deprecated
     */
    public Map<Organization, Map<String, Level>> getLocaleTypes() {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_locale_level;
    }

    /**
     * Return map of locales to levels
     * @param org
     * @return
     */
    public Map<String, Level> getLocaleToLevel(Organization org) {
        return getLocaleTypes().get(org);
    }

    public Level getLocaleCoverageLevel(String organization, String desiredLocale) {
        return getLocaleCoverageLevel(Organization.fromString(organization), desiredLocale);
    }

    public Level getLocaleCoverageLevel(Organization organization, String desiredLocale) {
        return getLocaleCoverageLevel(organization, desiredLocale, new Output<LocaleCoverageType>());
    }

    public enum LocaleCoverageType {
        explicit, parent, star, undetermined
    }

    /**
     * Returns coverage level of locale according to organization. Returns Level.UNDETERMINED if information is missing.
     * A locale of "*" in the data means "everything else".
     */
    public Level getLocaleCoverageLevel(Organization organization, String desiredLocale, Output<LocaleCoverageType> coverageType) {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        coverageType.value = LocaleCoverageType.undetermined;
        if (organization == null) {
            return Level.UNDETERMINED;
        }
        Map<String, Level> locale_status = platform_locale_level.get(organization);
        if (locale_status == null) {
            return Level.UNDETERMINED;
        }
        // see if there is a parent
        String originalLocale = desiredLocale;
        while (desiredLocale != null) {
            Level status = locale_status.get(desiredLocale);
            if (status != null && status != Level.UNDETERMINED) {
                coverageType.value = originalLocale == desiredLocale ? LocaleCoverageType.explicit : LocaleCoverageType.parent;
                return status;
            }
            desiredLocale = LocaleIDParser.getParent(desiredLocale);
        }
        Level status = locale_status.get("*");
        if (status != null && status != Level.UNDETERMINED) {
            coverageType.value = LocaleCoverageType.star;
            return status;
        }
        return Level.UNDETERMINED;
    }

    /**
     * Returns coverage level of locale according to organization. Returns Level.UNDETERMINED if information is missing.
     */
    public Level getDefaultLocaleCoverageLevel(Organization organization) {
        return getLocaleCoverageLevel(organization, "*");
    }

    public Set<Organization> getLocaleCoverageOrganizations() {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_locale_level.keySet();
    }

    public Set<String> getLocaleCoverageOrganizationStrings() {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_locale_levelString.keySet();
    }

    public Set<String> getLocaleCoverageLocales(String organization) {
        return getLocaleCoverageLocales(Organization.fromString(organization));
    }

    public Set<String> getLocaleCoverageLocales(Organization organization) {
        synchronized (StandardCodes.class) {
            if (platform_locale_level == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_locale_level.get(organization).keySet();
    }

    public Relation<Level, String> getLevelsToLocalesFor(Organization organization) {
        synchronized (StandardCodes.class) {
            if (platform_level_locale == null) {
                loadPlatformLocaleStatus();
            }
        }
        return platform_level_locale.get(organization);
    }

    public Set<String> getLocaleCoverageLocales(Organization organization, Set<Level> choice) {
        Set<String> result = new LinkedHashSet<String>();
        for (String locale : getLocaleCoverageLocales(organization)) {
            if (choice.contains(getLocaleCoverageLevel(organization, locale))) {
                result.add(locale);
            }
        }
        return result;
    }

    private void loadPlatformLocaleStatus() {
        LocaleIDParser parser = new LocaleIDParser();
        platform_locale_level = new EnumMap<Organization, Map<String, Level>>(Organization.class);
        SupplementalDataInfo sd = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = sd.getDefaultContentLocales();
        String line;
        try {
            BufferedReader lstreg = CldrUtility.getUTF8Data("Locales.txt");
            while (true) {
                line = lstreg.readLine();
                if (line == null)
                    break;
                int commentPos = line.indexOf('#');
                if (commentPos >= 0) {
                    line = line.substring(0, commentPos);
                }
                line = line.trim();
                if (line.length() == 0)
                    continue;
                List<String> stuff = CldrUtility.splitList(line, ';', true);
                Organization organization;

                // verify that the organization is valid
                try {
                    organization = Organization.fromString(stuff.get(0));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid organization in Locales.txt: " + line);
                }

                // verify that the locale is valid BCP47
                String locale = (String) stuff.get(1);
                if (!locale.equals("*")) {
                    parser.set(locale);
                    String valid = validate(parser);
                    if (valid.length() != 0) {
                        throw new IllegalArgumentException("Invalid locale in Locales.txt: " + line);
                    }
                    locale = parser.toString(); // normalize

                    // verify that the locale is not a default content locale
                    if (defaultContentLocales.contains(locale)) {
                        throw new IllegalArgumentException("Cannot have default content locale in Locales.txt: " + line);
                    }
                }

                Level status = Level.get((String) stuff.get(2));
                if (status == Level.UNDETERMINED) {
                    System.out.println("Warning: Level unknown on: " + line);
                }
                Map<String, Level> locale_status = platform_locale_level.get(organization);
                if (locale_status == null) {
                    platform_locale_level.put(organization, locale_status = new TreeMap<String, Level>());
                }
                locale_status.put(locale, status);
                if (!locale.equals("*")) {
                    String scriptLoc = parser.getLanguageScript();
                    if (locale_status.get(scriptLoc) == null)
                        locale_status.put(scriptLoc, status);
                    String lang = parser.getLanguage();
                    if (locale_status.get(lang) == null)
                        locale_status.put(lang, status);
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error", e);
        }

        // now reset the parent to be the max of the children
        for (Organization platform : platform_locale_level.keySet()) {
            Map<String, Level> locale_level = platform_locale_level.get(platform);
            for (String locale : locale_level.keySet()) {
                parser.set(locale);
                Level childLevel = locale_level.get(locale);

                String language = parser.getLanguage();
                if (!language.equals(locale)) {
                    Level languageLevel = (Level) locale_level.get(language);
                    if (languageLevel == null || languageLevel.compareTo(childLevel) < 0) {
                        locale_level.put(language, childLevel);
                    }
                }
                String oldLanguage = language;
                language = parser.getLanguageScript();
                if (!language.equals(oldLanguage)) {
                    Level languageLevel = (Level) locale_level.get(language);
                    if (languageLevel == null || languageLevel.compareTo(childLevel) < 0) {
                        locale_level.put(language, childLevel);
                    }
                }
            }
        }
        // backwards compat hack
        platform_locale_levelString = new TreeMap<String, Map<String, String>>();
        platform_level_locale = new EnumMap<>(Organization.class);
        for (Organization platform : platform_locale_level.keySet()) {
            Map<String, String> locale_levelString = new TreeMap<String, String>();
            platform_locale_levelString.put(platform.toString(), locale_levelString);
            Map<String, Level> locale_level = platform_locale_level.get(platform);
            for (String locale : locale_level.keySet()) {
                locale_levelString.put(locale, locale_level.get(locale).toString());
            }
            Relation level_locale = Relation.of(new EnumMap(Level.class), HashSet.class);
            level_locale.addAllInverted(locale_level).freeze();
            platform_level_locale.put(platform, level_locale);
        }
        CldrUtility.protectCollection(platform_level_locale);
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
    public boolean isLocaleInGroup(String locale, String group, Organization org) {
        return group.equals(getGroup(locale, org));
    }

    public boolean isLocaleInGroup(String locale, String group, String org) {
        return isLocaleInGroup(locale, group, Organization.fromString(org));
    }

    public String getGroup(String locale, String org) {
        return getGroup(locale, Organization.fromString(org));
    }

    /**
     * Gets the coverage group given a locale and org
     *
     * @param locale
     * @param org
     * @return group if availble, null if not
     */
    public String getGroup(String locale, Organization org) {
        Level l = getLocaleCoverageLevel(org, locale);
        if (l.equals(Level.UNDETERMINED)) {
            return null;
        } else {
            return l.toString();
        }
    }

    // ========== PRIVATES ==========

    private StandardCodes() {
        String[] files = { /* "lstreg.txt", */"ISO4217.txt" }; // , "TZID.txt"
        type_code_preferred.put(CodeType.tzid, new TreeMap<String, String>());
        add(CodeType.language, "root", "Root");
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
                    List<String> pieces = CldrUtility.splitList(line, '|', true,
                        new ArrayList<String>());
                    CodeType type = CodeType.from(pieces.get(0));
                    pieces.remove(0);

                    String code = pieces.get(0);
                    pieces.remove(0);
                    if (type.equals("date")) {
                        continue;
                    }

                    String oldName = pieces.get(0);
                    int pos = oldName.indexOf(';');
                    if (pos >= 0) {
                        oldName = oldName.substring(0, pos).trim();
                        pieces.set(0, oldName);
                    }

                    List<String> data = pieces;
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
                                Set<String> codes = country_modernCurrency.get(country);
                                if (codes == null) {
                                    country_modernCurrency.put(country, codes = new TreeSet<String>());
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
                            Map<String, String> code_preferred = type_code_preferred.get(type);
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

        Map<String, Map<String, Map<String, String>>> languageRegistry = getLStreg();
        // languageRegistry = CldrUtility.protectCollection(languageRegistry);

        for (String type : languageRegistry.keySet()) {
            CodeType type2 = CodeType.from(type);
            Map<String, Map<String, String>> m = languageRegistry.get(type);
            for (String code : m.keySet()) {
                Map<String, String> mm = m.get(code);
                List<String> data = new ArrayList<String>(0);
                data.add(mm.get("Description"));
                data.add(mm.get("Added"));
                String pref = mm.get("Preferred-Value");
                if (pref == null) {
                    pref = mm.get("Deprecated");
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

        Map<String, List<String>> m = getZoneData();
        for (Iterator<String> it = m.keySet().iterator(); it.hasNext();) {
            String code = it.next();
            add(CodeType.tzid, code, m.get(code).toString());
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
    private void add(CodeType type, String string2, String string3) {
        List<String> l = new ArrayList<String>();
        l.add(string3);
        add(type, string2, l);
    }

    private void add(CodeType type, String code, List<String> otherData) {
        // hack
        if (type == CodeType.script) {
            if (code.equals("Qaai")) {
                otherData = new ArrayList<String>(otherData);
                otherData.set(0, "Inherited");
            } else if (code.equals("Zyyy")) {
                otherData = new ArrayList<String>(otherData);
                otherData.set(0, "Common");
            }
        }

        // assume name is the first item

        String name = otherData.get(0);

        // add to main list
        Map<String, List<String>> code_data = getCodeData(type);
        if (code_data == null) {
            code_data = new TreeMap<String, List<String>>();
            type_code_data.put(type, code_data);
        }
        List<String> lastData = code_data.get(code);
        if (lastData != null) {
            lastData.addAll(otherData);
        } else {
            code_data.put(code, otherData);
        }

        // now add mapping from name to codes
        Map<String, List<String>> name_codes = type_name_codes.get(type);
        if (name_codes == null) {
            name_codes = new TreeMap<String, List<String>>();
            type_name_codes.put(type, name_codes);
        }
        List<String> codes = name_codes.get(name);
        if (codes == null) {
            codes = new ArrayList<String>();
            name_codes.put(name, codes);
        }
        codes.add(code);
    }

    private List<String> DELETED3166 = Collections.unmodifiableList(Arrays
        .asList(new String[] { "BQ", "BU", "CT", "DD", "DY", "FQ", "FX", "HV",
            "JT", "MI", "NH", "NQ", "NT", "PC", "PU", "PZ", "RH", "SU", "TP",
            "VD", "WK", "YD", "YU", "ZR" }));

    public List<String> getOld3166() {
        return DELETED3166;
    }

    private Map<String, List<String>> WorldBankInfo;

    public Map<String, List<String>> getWorldBankInfo() {
        if (WorldBankInfo == null) {
            List<String> temp = fillFromCommaFile("WorldBankInfo.txt", false);
            WorldBankInfo = new HashMap<String, List<String>>();
            for (String line : temp) {
                List<String> row = CldrUtility.splitList(line, ';', true);
                String key = row.get(0);
                row.remove(0);
                WorldBankInfo.put(key, row);
            }
            WorldBankInfo = CldrUtility.protectCollection(WorldBankInfo);
        }
        return WorldBankInfo;
    }

    Set<String> moribundLanguages;

    public Set<String> getMoribundLanguages() {
        if (moribundLanguages == null) {
            List<String> temp = fillFromCommaFile("moribund_languages.txt", true);
            moribundLanguages = new TreeSet<String>();
            moribundLanguages.addAll(temp);
            moribundLanguages = CldrUtility.protectCollection(moribundLanguages);
        }
        return moribundLanguages;
    }

    // produces a list of the 'clean' lines
    private List<String> fillFromCommaFile(String filename, boolean trim) {
        try {
            List<String> result = new ArrayList<String>();
            String line;
            BufferedReader lstreg = CldrUtility.getUTF8Data(filename);
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
                "Can't process file: data/" + filename).initCause(e);
        }
    }

    // return a complex map. language -> arn -> {"Comments" -> "x",
    // "Description->y,...}
    static String[][] extras = {
        { "language", "root", "Description", "Root", "CLDR", "True" },
        // { "language", "cch", "Description", "Atsam", "CLDR", "True" },
        // { "language", "kaj", "Description", "Jju", "CLDR", "True" },
        // { "language", "kcg", "Description", "Tyap", "CLDR", "True" },
        // { "language", "kfo", "Description", "Koro", "CLDR", "True" },
        // { "language", "mfe", "Description", "Morisyen", "CLDR", "True" },
        // { "region", "172", "Description", "Commonwealth of Independent States", "CLDR", "True" },
        // { "region", "062", "Description", "South-Central Asia", "CLDR", "True" },
        // { "region", "003", "Description", "North America", "CLDR", "True" },
        //        { "variant", "POLYTONI", "Description", "Polytonic Greek", "CLDR", "True", "Preferred-Value", "POLYTON" },
        { "variant", "REVISED", "Description", "Revised Orthography", "CLDR", "True" },
        { "variant", "SAAHO", "Description", "Dialect", "CLDR", "True" },
        { "variant", "POSIX", "Description", "Computer-Style", "CLDR", "True" },
        // {"region", "172", "Description", "Commonwealth of Independent States",
        // "CLDR", "True"},
        // { "region", "", "Description", "European Union", "CLDR", "True" },
        { "region", "ZZ", "Description", "Unknown or Invalid Region", "CLDR", "True" },
        { "region", "QO", "Description", "Outlying Oceania", "CLDR", "True" },
        { "region", "XK", "Description", "Kosovo", "CLDR", "True" },
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

    static final String registryName = CldrUtility.getProperty("registry", "language-subtag-registry");

    public enum LstrType {
        language("und", "zxx", "mul", "mis", "root"), 
        script("Zzzz", "Zsym", "Zxxx", "Zmth"), 
        region("ZZ"), 
        variant(), 
        extlang(true, false), 
        grandfathered(true, false),
        redundant(true, false),
        /** specialized codes for validity; TODO: rename LstrType **/
        currency(false, true, "XXX"),
        subdivision(false, true),
        unit(false, true);

        public final Set<String> specials;
        public final String unknown;
        public final boolean isLstr;
        public final boolean isUnicode;

        private LstrType(String... unknownValue) {
            this(true, true, unknownValue);
        }

        private LstrType(boolean lstr, boolean unicode, String... unknownValue) {
            unknown = unknownValue.length == 0 ? null : unknownValue[0];
            LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList(unknownValue));
            if (unknown != null) {
                set.remove(unknown);
            }
            specials = Collections.unmodifiableSet(set);
            isLstr = lstr;
            isUnicode = unicode;
        }

        // 
        static final Pattern WELLFORMED = Pattern.compile("([0-9]{3}|[a-zA-Z]{2})[a-zA-Z0-9]{1,4}");

        boolean isWellFormed(String candidate) {
            switch (this) {
            case subdivision:
                return WELLFORMED.matcher(candidate).matches();
            default:
                throw new UnsupportedOperationException();
            }
        }

        public String toCompatString() {
            return this == region ? "territory" : toString();
        }
    }

    public enum LstrField {
        Type, Subtag, Description, Added, Scope, Tag, Suppress_Script, Macrolanguage, Deprecated, Preferred_Value, Comments, Prefix, CLDR;
        public static LstrField from(String s) {
            return LstrField.valueOf(s.trim().replace("-", "_"));
        }
    }

    static Map<String, Map<String, Map<String, String>>> LSTREG;
    static Map<LstrType, Map<String, Map<LstrField, String>>> LSTREG_ENUM;
    static Map<LstrType, Map<String, Map<LstrField, String>>> LSTREG_RAW;

    /**
     * Returns a map like {extlang={aao={Added=2009-07-29, Description=Algerian Saharan Arabic, ...<br>
     * That is, type => subtype => map<tag,value>. Descriptions are concatenated together, separated by
     * DESCRIPTION_SEPARATOR.
     *
     * @return
     */
    public static Map<String, Map<String, Map<String, String>>> getLStreg() {
        if (LSTREG == null) {
            initLstr();
        }
        return LSTREG;
    }

    /**
     * Returns a map like {extlang={aao={Added=2009-07-29, Description=Algerian Saharan Arabic, ...<br>
     * That is, type => subtype => map<tag,value>. Descriptions are concatenated together, separated by
     * DESCRIPTION_SEPARATOR.
     *
     * @return
     */
    public static Map<LstrType, Map<String, Map<LstrField, String>>> getEnumLstreg() {
        if (LSTREG_ENUM == null) {
            initLstr();
        }
        return LSTREG_ENUM;
    }

    public static Map<LstrType, Map<String, Map<LstrField, String>>> getLstregEnumRaw() {
        if (LSTREG_ENUM == null) {
            initLstr();
        }
        return LSTREG_RAW;
    }

    private static void initLstr() {
        Map<LstrType, Map<String, Map<LstrField, String>>> result2 = new TreeMap<LstrType, Map<String, Map<LstrField, String>>>();

        int lineNumber = 1;

        Set<String> funnyTags = new TreeSet<String>();
        String line;
        try {
            BufferedReader lstreg = CldrUtility.getUTF8Data(registryName);
            LstrType lastType = null;
            String lastTag = null;
            Map<String, Map<LstrField, String>> subtagData = null;
            Map<LstrField, String> currentData = null;
            LstrField lastLabel = null;
            String lastRest = null;
            boolean inRealContent = false;
//            Map<String, String> translitCache = new HashMap<String, String>();
            for (;; ++lineNumber) {
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
                LstrField label = LstrField.from(line.substring(0, pos2));
                String rest = line.substring(pos2 + 1).trim();
                if (label == LstrField.Type) {
                    subtagData = CldrUtility.get(result2, lastType = LstrType.valueOf(rest));
                    if (subtagData == null) {
                        result2.put(LstrType.valueOf(rest), subtagData = new TreeMap<String, Map<LstrField, String>>());
                    }
                } else if (label == LstrField.Subtag
                    || label == LstrField.Tag) {
                    lastTag = rest;
                    String endTag = null;
                    // Subtag: qaa..qtz
                    int pos = lastTag.indexOf("..");
                    if (pos >= 0) {
                        endTag = lastTag.substring(pos + 2);
                        lastTag = lastTag.substring(0, pos);
                    }
                    currentData = new TreeMap<LstrField, String>();
                    if (endTag == null) {
                        putSubtagData(lastTag, subtagData, currentData);
                        languageCount.add(lastType, 1);
                        // System.out.println(languageCount.getCount(lastType) + "\t" + lastType + "\t" + lastTag);
                    } else {
                        for (; lastTag.compareTo(endTag) <= 0; lastTag = nextAlpha(lastTag)) {
                            // System.out.println(">" + current);
                            putSubtagData(lastTag, subtagData, currentData);
                            languageCount.add(lastType, 1);
                            // System.out.println(languageCount.getCount(lastType) + "\t" + lastType + "\t" + lastTag);
                        }

                    }
                    // label.equalsIgnoreCase("Added") || label.equalsIgnoreCase("Suppress-Script")) {
                    // skip
                    // } else if (pieces.length < 2) {
                    // System.out.println("Odd Line: " + lastType + "\t" + lastTag + "\t" + line);
                } else {
                    lastLabel = label;
                    // The following code was removed because in the standard tests (TestAll) both lastRest and rest were always equal.
                    //                    if(!translitCache.containsKey(rest)) {
                    //                        lastRest = TransliteratorUtilities.fromXML.transliterate(rest);
                    //                        translitCache.put(rest, lastRest);
                    //                        if (!lastRest.equals(rest)) {
                    //                            System.out.println(System.currentTimeMillis()+" initLStr: LastRest: '"+lastRest+"' Rest: '"+rest+"'");
                    //                        }
                    //                    } else {
                    //                        lastRest = translitCache.get(rest);
                    //                    }
                    lastRest = rest;
                    String oldValue = (String) CldrUtility.get(currentData, lastLabel);
                    if (oldValue != null) {
                        lastRest = oldValue + DESCRIPTION_SEPARATOR + lastRest;
                    }
                    currentData.put(lastLabel, lastRest);
                }
            }
        } catch (Exception e) {
            throw (RuntimeException) new IllegalArgumentException(
                "Can't process file: data/"
                    + registryName + ";\t at line " + lineNumber).initCause(e);
        } finally {
            if (!funnyTags.isEmpty()) {
                if (DEBUG)
                    System.out.println("Funny tags: " + funnyTags);
            }
        }
        // copy raw
        Map<LstrType, Map<String, Map<LstrField, String>>> rawLstreg = new TreeMap<LstrType, Map<String, Map<LstrField, String>>>();
        for (Entry<LstrType, Map<String, Map<LstrField, String>>> entry1 : result2.entrySet()) {
            LstrType key1 = entry1.getKey();
            TreeMap<String, Map<LstrField, String>> raw1 = new TreeMap<String, Map<LstrField, String>>();
            rawLstreg.put(key1, raw1);
            for (Entry<String, Map<LstrField, String>> entry2 : entry1.getValue().entrySet()) {
                String key2 = entry2.getKey();
                final Map<LstrField, String> value2 = entry2.getValue();
                TreeMap<LstrField, String> raw2 = new TreeMap<LstrField, String>();
                raw2.putAll(value2);
                raw1.put(key2, raw2);
            }
        }
        LSTREG_RAW = CldrUtility.protectCollection(rawLstreg);

        // add extras
        for (int i = 0; i < extras.length; ++i) {
            Map<String, Map<LstrField, String>> subtagData = CldrUtility.get(result2, LstrType.valueOf(extras[i][0]));
            if (subtagData == null) {
                result2.put(LstrType.valueOf(extras[i][0]), subtagData = new TreeMap<String, Map<LstrField, String>>());
            }
            Map<LstrField, String> labelData = new TreeMap<LstrField, String>();
            for (int j = 2; j < extras[i].length; j += 2) {
                labelData.put(LstrField.from(extras[i][j]), extras[i][j + 1]);
            }
            Map<LstrField, String> old = CldrUtility.get(subtagData, extras[i][1]);
            if (old != null) {
                if (!"Private use".equals(CldrUtility.get(old, LstrField.Description))) {
                    throw new IllegalArgumentException("REPLACING data for " + extras[i][1] + "\t" + old + "\twith"
                        + labelData);
                }
            }
            if (false) {
                System.out.println((old != null ? "REPLACING" + "\t" + old : "ADDING") +
                    " data for " + extras[i][1] + "\twith" + labelData);
            }
            subtagData.put(extras[i][1], labelData);
        }
        // build compatibility map
        Map<String, Map<String, Map<String, String>>> result = new LinkedHashMap<String, Map<String, Map<String, String>>>();
        for (Entry<LstrType, Map<String, Map<LstrField, String>>> entry : result2.entrySet()) {
            Map<String, Map<String, String>> copy2 = new LinkedHashMap<String, Map<String, String>>();
            result.put(entry.getKey().toString(), copy2);
            for (Entry<String, Map<LstrField, String>> entry2 : entry.getValue().entrySet()) {
                Map<String, String> copy3 = new LinkedHashMap<String, String>();
                copy2.put(entry2.getKey(), copy3);
                for (Entry<LstrField, String> entry3 : entry2.getValue().entrySet()) {
                    copy3.put(entry3.getKey().toString(), entry3.getValue());
                }
            }
        }
        LSTREG = CldrUtility.protectCollection(result);
        LSTREG_ENUM = CldrUtility.protectCollection(result2);
    }

    private static <K, K2, V> Map<K2, V> putSubtagData(K lastTag, Map<K, Map<K2, V>> subtagData, Map<K2, V> currentData) {
        Map<K2, V> oldData = subtagData.get(lastTag);
        if (oldData != null) {
            if (oldData.get("CLDR") != null) {
                System.out.println("overriding: " + lastTag + ", " + oldData);
            } else {
                throw new IllegalArgumentException("Duplicate tag: " + lastTag);
            }
        }
        return subtagData.put(lastTag, currentData);
    }

    static Counter<LstrType> languageCount = new Counter<LstrType>();

    public static Counter<LstrType> getLanguageCount() {
        return languageCount;
    }

    ZoneParser zoneParser = new ZoneParser();

    // static public final Set<String> MODERN_SCRIPTS = Collections
    // .unmodifiableSet(new TreeSet(
    // // "Bali " +
    // // "Bugi " +
    // // "Copt " +
    // // "Hano " +
    // // "Osma " +
    // // "Qaai " +
    // // "Sylo " +
    // // "Syrc " +
    // // "Tagb " +
    // // "Tglg " +
    // Arrays
    // .asList("Hans Hant Jpan Hrkt Kore Arab Armn Bali Beng Bopo Cans Cham Cher Cyrl Deva Ethi Geor Grek Gujr Guru Hani Hang Hebr Hira Knda Kana Kali Khmr Laoo Latn Lepc Limb Mlym Mong Mymr Talu Nkoo Olck Orya Saur Sinh Tale Taml Telu Thaa Thai Tibt Tfng Vaii Yiii"
    // .split("\\s+"))));

    // updated to http://www.unicode.org/reports/tr31/tr31-9.html#Specific_Character_Adjustments

    /**
     * @deprecated
     */
    public Map<String, List<ZoneLine>> getZone_rules() {
        return zoneParser.getZone_rules();
    }

    /**
     * @deprecated
     */
    public Map<String, List<String>> getZoneData() {
        return zoneParser.getZoneData();
    }

    /**
     * @deprecated
     */
    public Set<String> getCanonicalTimeZones() {
        return zoneParser.getZoneData().keySet();
    }

    /**
     * @deprecated
     */
    public Map<String, Set<String>> getCountryToZoneSet() {
        return zoneParser.getCountryToZoneSet();
    }

    /**
     * @deprecated
     */
    public List<String> getDeprecatedZoneIDs() {
        return zoneParser.getDeprecatedZoneIDs();
    }

    /**
     * @deprecated
     */
    public Comparator<String> getTZIDComparator() {
        return zoneParser.getTZIDComparator();
    }

    /**
     * @deprecated
     */
    public Map<String, Set<String>> getZoneLinkNew_OldSet() {
        return zoneParser.getZoneLinkNew_OldSet();
    }

    /**
     * @deprecated
     */
    public Map<String, String> getZoneLinkold_new() {
        return zoneParser.getZoneLinkold_new();
    }

    /**
     * @deprecated
     */
    public Map getZoneRuleID_rules() {
        return zoneParser.getZoneRuleID_rules();
    }

    /**
     * @deprecated
     */
    public Map<String, String> getZoneToCounty() {
        return zoneParser.getZoneToCounty();
    }

    /**
     * @deprecated
     */
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
        // Scope scope = Iso639Data.getScope(languageCode);
        // if (scope == Scope.Collection) return false;
        return false;
    }

    public static boolean isScriptModern(String script) {
        ScriptMetadata.Info info = ScriptMetadata.getInfo(script);
        if (info == null) {
            if (false) throw new IllegalArgumentException("No script metadata for: " + script);
            return false;
        }
        IdUsage idUsage = info.idUsage;
        return idUsage != IdUsage.EXCLUSION && idUsage != IdUsage.UNKNOWN;
    }

    static final Pattern whitespace = PatternCache.get("\\s+");
    static Set<String> filteredCurrencies = null;

    public Set<String> getSurveyToolDisplayCodes(String type) {
        return getGoodAvailableCodes(type);
    }

    static UnicodeSet COUNTRY = new UnicodeSet("[a-zA-Z]").freeze();

    /**
     * Quick check for whether valid country. Not complete: should use Validity
     * @param territory
     * @return
     */
    public static boolean isCountry(String territory) {
        switch (territory) {
        case "ZZ":
        case "QO":
        case "EU":
        case "UN":
        case "EZ":
            return false;
        default:
            return territory.length() == 2 && COUNTRY.containsAll(territory);
        }
    }

    public boolean isLstregPrivateUse(String type, String code) {
        Map<String, String> lStregData = getLStreg().get(type).get(code);
        return lStregData.get("Description").equalsIgnoreCase("private use");
    }

    public boolean isLstregDeprecated(String type, String code) {
        Map<String, String> lStregData = getLStreg().get(type).get(code);
        return lStregData.get("Deprecated") != null;
    }
}
