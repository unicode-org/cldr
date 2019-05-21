package org.unicode.cldr.test;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.EmojiConstants;
import org.unicode.cldr.util.LanguageGroup;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.ibm.icu.util.ULocale;

public class EmojiSubdivisionNames {
    private static final String subdivisionPathPrefix = "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"";
    private static final String subdivisionPathSuffix = "\"]";
    private static final Map<String,Map<String,String>> localeToNameToSubdivisionId = new ConcurrentHashMap<>();
    private static final Map<String,Map<String,String>> localeToSubdivisionIdToName = new ConcurrentHashMap<>();

    private static final Pattern SUBDIVISION_PATTERN = Pattern.compile("\\s*<subdivision\\s+type=\"(gb(?:eng|sct|wls))\">([^<]+)</subdivision>");
    private static final SortedSet<String> SUBDIVISION_FILE_NAMES = ImmutableSortedSet.copyOf(
        new File(CLDRPaths.SUBDIVISIONS_DIRECTORY).list()
        );

    static {
        localeToNameToSubdivisionId.put("root", Collections.emptyMap());
    }

    public static Map<String, String> getSubdivisionIdToName(String localeID) {
        Map<String,String> result = localeToSubdivisionIdToName.get(localeID);
        if (result == null) {
            load(localeID);
            result = localeToSubdivisionIdToName.get(localeID);
            if (result == null) {
                result = Collections.emptyMap();
            }
        }
        return result;
    }

    public static Map<String, String> getNameToSubdivisionPath(String localeID) {
        Map<String,String> result = localeToNameToSubdivisionId.get(localeID);
        if (result == null) {
            load(localeID);
            result = localeToNameToSubdivisionId.get(localeID);
            if (result == null) {
                result = Collections.emptyMap();
            }
        }
        return result;
    }

    private static void load(String localeID) {
        try {
            Map<String,String> _subdivisionIdToName;
            Map<String, String> _nameToSubdivisionId;

            String fileName = localeID + ".xml";
            if (SUBDIVISION_FILE_NAMES.contains(fileName)) {
                _nameToSubdivisionId = new TreeMap<>();
                _subdivisionIdToName = new TreeMap<>();
                Matcher m = SUBDIVISION_PATTERN.matcher("");
                for (String line : FileUtilities.in(new File(CLDRPaths.SUBDIVISIONS_DIRECTORY, fileName))) {
                    if (m.reset(line).matches()) {
                        String xpath = subdivisionPathPrefix + EmojiConstants.toTagSeq(m.group(1)) + subdivisionPathSuffix;
                        _nameToSubdivisionId.put(m.group(2), xpath);
                        _subdivisionIdToName.put(m.group(1), m.group(2));
                    }
                }
                _nameToSubdivisionId = _nameToSubdivisionId.isEmpty() ? Collections.emptyMap() 
                    : ImmutableMap.copyOf(_nameToSubdivisionId);
                _subdivisionIdToName = _subdivisionIdToName.isEmpty() ? Collections.emptyMap() 
                    : ImmutableMap.copyOf(_subdivisionIdToName);
            } else {
                String parentLocaleId = LocaleIDParser.getParent(localeID);
                _nameToSubdivisionId = getNameToSubdivisionPath(parentLocaleId);
                _subdivisionIdToName = localeToSubdivisionIdToName.get(parentLocaleId);
            }
            localeToNameToSubdivisionId.put(localeID, _nameToSubdivisionId);
            localeToSubdivisionIdToName.put(localeID, _subdivisionIdToName);
        } catch (Exception e) {}
    }

    static Set<String> SUBDIVISIONS = ImmutableSet.of("gbeng", "gbsct", "gbwls");

    public static void main(String[] args) {
        System.out.print("Group\tOrd.\tLocale\tCode");
        for (String sd : SUBDIVISIONS) {
            System.out.print('\t');
            System.out.print(sd);

        }
        System.out.println();
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        Set<String> locales = new HashSet<>();
        for (String filename : SUBDIVISION_FILE_NAMES) {
            if (filename.endsWith(".xml")) {
                String locale = filename.substring(0, filename.length()-4);
                Map<String, String> map = getSubdivisionIdToName(locale);
                if (!map.isEmpty()) {
                    locales.add(locale);
                    ULocale ulocale = new ULocale(locale);
                    LanguageGroup group = LanguageGroup.get(ulocale);
                    int rank = LanguageGroup.rankInGroup(ulocale);
                    System.out.print(group + "\t" + rank + "\t" + english.getName(locale) + "\t" + locale);
                    for (String sd : SUBDIVISIONS) {
                        System.out.print('\t');
                        System.out.print(map.get(sd));
                    }
                    System.out.println();
                }
            }
        }
        for (String locale : StandardCodes.make().getLocaleCoverageLocales(Organization.cldr)) {
            if (locales.contains(locale)) {
                continue;
            }
            ULocale ulocale = new ULocale(locale);
            String region = ulocale.getCountry();
            if (!region.isEmpty() && !region.equals("PT") && !region.equals("GB")) {
                continue;
            }
            LanguageGroup group = LanguageGroup.get(ulocale);
            int rank = LanguageGroup.rankInGroup(ulocale);
            System.out.println(group + "\t" + rank + "\t" + english.getName(locale) + "\t" + locale);
        }

//        System.out.println(getSubdivisionIdToName("fr"));
//        System.out.println(getNameToSubdivisionPath("fr"));
    }
}