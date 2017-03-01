package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.CountryCodeConverter;
import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * To update, copy the list from http://en.wikipedia.org/wiki/List_of_official_languages_by_state into WikipediaOfficialLanguages.txt
 * May take some code updates, since they are not consistent, so a number of tests below are hacks based on current behavior.
 */
public class WikipediaOfficialLanguages {
    public static class Info implements Comparable<Info> {
        final String language;
        final OfficialStatus status;
        final String comments;

        public Info(String language, OfficialStatus status, String comments) {
            this.language = language;
            this.status = status;
            this.comments = comments;
        }

        @Override
        public boolean equals(Object arg0) {
            return compareTo((Info) arg0) == 0;
        }

        @Override
        public int hashCode() {
            return language.hashCode() ^ status.hashCode();
        }

        @Override
        public int compareTo(Info other) {
            int s = status.compareTo(other.status);
            return s != 0 ? -s : language.compareTo(other.language);
        }
    }

    private static Relation<String, Info> regionToLanguageStatus = Relation.of(new TreeMap<String, Set<Info>>(), TreeSet.class);
    static {
        Relation<String, String> REPLACE_REGIONS = Relation.of(new HashMap<String, Set<String>>(), HashSet.class);
        for (String s : Arrays.asList("Abkhazia", "Nagorno-Karabakh", "Northern Cyprus",
            "Sahrawi Arab Democratic Republic", "Tamazight", "Somaliland", "Somalian", "South Ossetia", "Transnistria")) {
            REPLACE_REGIONS.put(s, "SKIP");
        }
        for (String s : Arrays.asList(
            //"Akrotiri and Dhekelia",
            "Anguilla", "Bermuda",
            //"British Antarctic Territory",
            "British Indian Ocean Territory", "British Virgin Islands", "Cayman Islands", "Falkland Islands", "Gibraltar",
            "Montserrat", "Pitcairn Islands", "Saint Helena", "Ascension Island", "Tristan da Cunha")) {
            String region = CountryCodeConverter.getCodeFromName(s);
            if (region == null) {
                System.out.println("Couldn't parse region: <" + s + ">");
            } else {
                REPLACE_REGIONS.put("United Kingdom and overseas territories", region);
            }
        }
        for (String s : Arrays.asList("French Guiana", "French Polynesia", "Guadeloupe", "Martinique",
            "Mayotte", "New Caledonia", "Réunion", "Saint Barthélemy", "Saint Martin", "Saint Pierre and Miquelon",
            "Wallis and Futuna")) {
            String region = CountryCodeConverter.getCodeFromName(s);
            if (region == null) {
                System.out.println("Couldn't parse region: <" + s + ">");
            } else {
                REPLACE_REGIONS.put("France and overseas departments and territories", region);
            }
        }

        Matcher pagenote = PatternCache.get("\\[\\d+\\]").matcher("");
        Pattern commentBreak = PatternCache.get("\\)\\s*\\(?|\\s*\\(");
        int count = 0;
        try {
            BufferedReader input = FileUtilities.openUTF8Reader(CLDRPaths.UTIL_DATA_DIR, "WikipediaOfficialLanguages.txt");
            Set<String> regionSet = null;
            while (true) {
                String line = input.readLine();
                if (line == null) break;
                if (line.startsWith(" ")) {
                    // region
                    //  Afghanistan[1]
                    line = pagenote.reset(line.trim()).replaceAll("");
                    String[] items = commentBreak.split(line);
                    Set<String> replacement = REPLACE_REGIONS.get(items[0]);
                    if (replacement != null) {
                        if (replacement.contains("SKIP")) {
                            regionSet = Collections.emptySet();
                        } else {
                            regionSet = replacement;
                        }
                        continue;
                    }

                    String region = CountryCodeConverter.getCodeFromName(items[0]);
                    if (region == null) {
                        System.out.println(++count + " Couldn't parse region: <" + items[0] + "> in line: " + line);
                        regionSet = Collections.emptySet();
                    } else {
                        regionSet = new HashSet<String>();
                        regionSet.add(region);
                    }
                } else if (line.contains("[edit]") || line.trim().isEmpty()) {
                    continue;
                } else if (!regionSet.isEmpty()) {
                    if (line.contains("Sign Language")) {
                        continue;
                    }
                    // Language / Status
                    // Pashto (statewide) (official)
                    line = pagenote.reset(line.trim()).replaceAll("");
                    String[] items = commentBreak.split(line);
                    String language = LanguageCodeConverter.getCodeForName(items[0]);
                    if (language == null) {
                        System.out.println(++count + " Couldn't parse language:\txxx ; " + items[0] + "\tfor <" + regionSet +
                            "> in line: " + line);
                    } else if ("sgn".equals(language)) {
                        continue;
                    } else {
                        StringBuffer s = new StringBuffer();
                        for (int i = 1; i < items.length; ++i) {
                            if (s.length() != 0) {
                                s.append("; ");
                            }
                            s.append(items[i]);
                        }
                        String comments = s.toString();
                        Set<String> narrowRegionSet = getRegionSet(regionSet, comments);

                        for (String region : narrowRegionSet) {
                            regionToLanguageStatus.put(region,
                                new Info(language, guessStatus(comments),
                                    comments));
                        }
                    }
                }
            }
            regionToLanguageStatus.freeze();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static OfficialStatus guessStatus(String line) {
        if (line.contains("minority")) {
            return OfficialStatus.official_minority;
        } else if (line.contains("regional") || line.startsWith("in ")) {
            return OfficialStatus.official_regional;
        } else if (line.contains("recognized")) {
            return OfficialStatus.recognized;
        } else if (line.contains("de facto")) {
            return OfficialStatus.de_facto_official;
        } else if (line.contains("official") || line.contains("national") || line.isEmpty()) {
            return OfficialStatus.official;
        }
        return OfficialStatus.official;
    }

    private static Set<String> getRegionSet(Set<String> regionSet, String comments) {
        if (regionSet.size() < 2) {
            return regionSet;
        }
        int inLen = getStartLen(comments, "in ");
        if (inLen == 0) {
            inLen = getStartLen(comments, "minority language in ");
        }
        if (inLen == 0) {
            return regionSet;
        }
        comments = comments.substring(inLen);

        Set<String> result = new HashSet<String>();
        String[] parts = comments.split("(,?\\s+and|,|;)\\s+");
        for (String part : parts) {
            if (part.isEmpty() || part.equals("de facto")) {
                continue;
            }
            String region = CountryCodeConverter.getCodeFromName(part);
            if (region == null) {
                System.err.println("* Can't convert " + region + " in " + part);
            } else {
                result.add(region);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException(regionSet + ", " + comments);
        }
        return result;
    }

    private static int getStartLen(String comments, String string) {
        return comments.startsWith(string) ? string.length() : 0;
    }

    public static Set<String> getRegions() {
        return regionToLanguageStatus.keySet();
    }

    public static Set<Info> getInfo(String region) {
        return regionToLanguageStatus.get(region);
    }

    public static void main(String[] args) {
        CLDRConfig testInfo = ToolConfig.getToolInstance();
        CLDRFile english = testInfo.getEnglish();
        SupplementalDataInfo supplementalDataInfo = testInfo.getSupplementalDataInfo();
        StandardCodes sc = testInfo.getStandardCodes();
        Set<String> locales = sc.getLocaleCoverageLocales("google"); // for now, restrict this

        System.out.println("Cc\tCountry\tLc\tLanguage Name\tWiki status (heuristic)\tCLDR status\t\tWiki notes");
        Set<String> seen = new HashSet<String>();
        for (String region : getRegions()) {
            //boolean regionShown = false;
            Set<String> cldrLanguagesRaw = supplementalDataInfo.getLanguagesForTerritoryWithPopulationData(region);
            Map<String, PopulationData> cldrLanguageInfo = new HashMap<String, PopulationData>();
            for (String s : cldrLanguagesRaw) {
                if (s.contains("_")) {
                    PopulationData sInfo = supplementalDataInfo.getLanguageAndTerritoryPopulationData(s, region);
                    s = s.substring(0, s.indexOf('_'));
                    cldrLanguageInfo.put(s, sInfo);
                }
            }
            for (Info info : getInfo(region)) {
                if (!locales.contains(info.language)) {
                    continue;
                }
                PopulationData sInfo = supplementalDataInfo.getLanguageAndTerritoryPopulationData(info.language, region);
                if (sInfo == null) {
                    sInfo = cldrLanguageInfo.get(info.language);
                }
                OfficialStatus cldrStatus = sInfo == null ? OfficialStatus.unknown : sInfo.getOfficialStatus();
                if (!areCompatible(info.status, cldrStatus)) {
                    System.out.print(region + "\t" + english.getName(CLDRFile.TERRITORY_NAME, region));
                    ;
                    System.out.println("\t" + info.language
                        + "\t" + english.getName(info.language)
                        + "\t" + info.status
                        + "\t" + (cldrStatus == null ? "NOT-IN-CLDR" : cldrStatus)
                        + "\t-\t" + info.comments);
                }
                seen.add(info.language);
            }
            for (String r2 : cldrLanguagesRaw) {
                if (!seen.contains(r2) && !r2.contains("_") && locales.contains(r2)) {
                    PopulationData sInfo = supplementalDataInfo.getLanguageAndTerritoryPopulationData(r2, region);
                    OfficialStatus officialStatus = sInfo.getOfficialStatus();
                    if (OfficialStatus.unknown != officialStatus) {
                        System.out.print(region + "\t" + english.getName(CLDRFile.TERRITORY_NAME, region));
                        ;
                        System.out.println("\t" + r2
                            + "\t" + english.getName(r2)
                            + "\t" + "CLDR-ONLY"
                            + "\t" + (sInfo == null ? "NOT-IN-CLDR" : officialStatus));
                    }
                }
            }
        }
        Set<String> errors = LanguageCodeConverter.getParseErrors();
        for (String error : errors) {
            if (!error.startsWith("Name Collision!")
                && !error.startsWith("Skipping *OMIT")) {
                System.err.println(error);
            }
        }
        // Verify codes
        //        errors = CountryCodeConverter.getParseErrors();
        //        for (String error : errors) {
        //            System.err.println(error);
        //        }
    }

    private static boolean areCompatible(OfficialStatus infoStatus, OfficialStatus cldrStatus) {
        return infoStatus == cldrStatus
            || infoStatus == OfficialStatus.official_regional && cldrStatus == OfficialStatus.official_minority
            || infoStatus == OfficialStatus.official_minority && cldrStatus == OfficialStatus.official_regional;
    }
}
