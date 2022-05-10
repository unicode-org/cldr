package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.util.ICUUncheckedIOException;

class LocalesTxtReader {
    Map<Organization, Map<String, Level>> platform_locale_level = null;
    Map<Organization, Relation<Level, String>> platform_level_locale = null;
    Map<String, Map<String, String>> platform_locale_levelString = null;
    Map<Organization, Map<String, Integer>> organization_locale_weight = null;
    Map<Organization, Map<String, Set<String>>> organization_locale_match = null;

    public static final String DEFAULT_NAME = "Locales.txt";

    public LocalesTxtReader() {
    }

    /**
     * Read from Locales.txt, from the default location
     * @param lstreg stream to read from
     */
    public LocalesTxtReader read(StandardCodes sc) {
        try (BufferedReader lstreg = CldrUtility.getUTF8Data(DEFAULT_NAME);) {
            return read(sc, lstreg);
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Internal Error reading Locales.txt", e);
        }
    }

    /**
     * Parse a Locales.txt file
     * @param sc StandardCodes used for validation
     * @param lstreg stream to read from
     */
    public LocalesTxtReader read(StandardCodes sc, BufferedReader lstreg) {
        LocaleIDParser parser = new LocaleIDParser();
        platform_locale_level = new EnumMap<>(Organization.class);
        organization_locale_weight = new EnumMap<>(Organization.class);
        organization_locale_match = new EnumMap<>(Organization.class);
        SupplementalDataInfo sd = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = sd.getDefaultContentLocales();
        String line;
        try {
            while (true) {
                Integer weight = null; // @weight
                String pathMatch = null; // @pathMatch
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
                String localePart = stuff.get(1).trim();
                List<String> localeStuff = CldrUtility.splitList(localePart, ' ', true);
                Set<String> locales = new TreeSet<>();

                for (final String entry : localeStuff) {
                    if (entry.startsWith("@")) {
                        List<String> kwStuff = CldrUtility.splitList(entry, '=', true);
                        if (kwStuff.size() > 2 || kwStuff.size() < 1) {
                            throw new IllegalArgumentException("Invalid @-command " + entry + " in Locales.txt: " + line);
                        }
                        final String atCommand = kwStuff.get(0);
                        switch(atCommand) {
                            case "@weight":
                                weight = Integer.parseInt(kwStuff.get(1));
                                break;

                            case "@pathMatch":
                                pathMatch = kwStuff.get(1);
                            break;
                            default:
                                throw new IllegalArgumentException("Unknown @-command " + atCommand + " in Locales.txt: " + line);
                        }
                    } else {
                        locales.add(entry);
                    }
                }

                if (locales.size() != 1) {
                    // require there to be exactly one locale.
                    // This would allow collapsing into fewer lines.
                    throw new IllegalArgumentException("Expected one locale entry in Locales.txt but got " + locales.size() + ": " + line);
                }

                // extract the single locale, process as before
                String locale = locales.iterator().next();

                if (!locale.equals(StandardCodes.ALL_LOCALES)) {
                    parser.set(locale);
                    String valid = sc.validate(parser);
                    if (valid.length() != 0) {
                        throw new IllegalArgumentException("Invalid locale in Locales.txt: " + line);
                    }
                    locale = parser.toString(); // normalize

                    // verify that the locale is not a default content locale
                    if (defaultContentLocales.contains(locale)) {
                        throw new IllegalArgumentException("Cannot have default content locale in Locales.txt: " + line);
                    }
                }

                Level status = Level.get(stuff.get(2));
                if (status == Level.UNDETERMINED) {
                    System.out.println("Warning: Level unknown on: " + line);
                }
                Map<String, Level> locale_status = platform_locale_level.get(organization);
                if (locale_status == null) {
                    platform_locale_level.put(organization, locale_status = new TreeMap<>());
                }
                locale_status.put(locale, status);
                if (!locale.equals(StandardCodes.ALL_LOCALES)) {
                    String scriptLoc = parser.getLanguageScript();
                    if (locale_status.get(scriptLoc) == null)
                        locale_status.put(scriptLoc, status);
                    String lang = parser.getLanguage();
                    if (locale_status.get(lang) == null)
                        locale_status.put(lang, status);
                }

                if (weight != null) {
                    organization_locale_weight
                        .computeIfAbsent(organization, ignored -> new TreeMap<String, Integer>())
                        .put(locale, weight);
                }
                if (pathMatch != null) {
                    organization_locale_match
                        .computeIfAbsent(organization, ignored -> new TreeMap<String, Set<String>>())
                        .put(locale, ImmutableSet.copyOf(pathMatch.split(",")));
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
                    Level languageLevel = locale_level.get(language);
                    if (languageLevel == null || languageLevel.compareTo(childLevel) < 0) {
                        locale_level.put(language, childLevel);
                    }
                }
                String oldLanguage = language;
                language = parser.getLanguageScript();
                if (!language.equals(oldLanguage)) {
                    Level languageLevel = locale_level.get(language);
                    if (languageLevel == null || languageLevel.compareTo(childLevel) < 0) {
                        locale_level.put(language, childLevel);
                    }
                }
            }
        }
        // backwards compat hack
        platform_locale_levelString = new TreeMap<>();
        platform_level_locale = new EnumMap<>(Organization.class);
        for (Organization platform : platform_locale_level.keySet()) {
            Map<String, String> locale_levelString = new TreeMap<>();
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
        return this;
    }
}
