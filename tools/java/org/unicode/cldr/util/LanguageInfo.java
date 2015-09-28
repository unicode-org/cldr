package org.unicode.cldr.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.impl.Relation;

public class LanguageInfo {
    static final StandardCodes sc = StandardCodes.make();
    static final CLDRConfig config = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI = config.getSupplementalDataInfo();

    public enum CldrDir {
        base, main, seed
    };

    private int literatePopulation;
    private Relation<OfficialStatus, String> statusToRegions = Relation.of(new EnumMap<OfficialStatus, Set<String>>(OfficialStatus.class), TreeSet.class);
    private Level level;
    private LanguageInfo.CldrDir cldrDir;

    /**
     * @return the statusToRegions
     */
    public Relation<OfficialStatus, String> getStatusToRegions() {
        return statusToRegions;
    }

    /**
     * @return the literatePopulation
     */
    public int getLiteratePopulation() {
        return literatePopulation;
    }

    public Level getCldrLevel() {
        return level;
    }

    public LanguageInfo.CldrDir getCldrDir() {
        return cldrDir;
    }

    //private M3<OfficialStatus, String, Boolean> status = ChainedMap.of(new EnumMap<OfficialStatus, Object>(OfficialStatus.class), new TreeMap<String,Object>(), Boolean.class);
    public static LanguageInfo get(String languageCode) {
        return languageCodeToInfo.get(languageCode);
    }

    public static Set<String> getAvailable() {
        return languageCodeToInfo.keySet();
    }

    @Override
    public String toString() {
        return literatePopulation
            + "\t" + CldrUtility.ifNull(cldrDir, "")
            + "\t" + CldrUtility.ifSame(level, Level.UNDETERMINED, "")
            + "\t" + (statusToRegions.isEmpty() ? "" : statusToRegions.toString());
    }

    static final Map<String, LanguageInfo> languageCodeToInfo;
    static {
        TreeMap<String, LanguageInfo> temp = new TreeMap<String, LanguageInfo>();
        // get population/official status
        LanguageTagParser ltp = new LanguageTagParser();
        for (String territory : SDI.getTerritoriesWithPopulationData()) {
            for (String language0 : SDI.getLanguagesForTerritoryWithPopulationData(territory)) {
                PopulationData data = SDI.getLanguageAndTerritoryPopulationData(language0, territory);
                String language = ltp.set(language0).getLanguage();
                LanguageInfo foo = getRaw(temp, language);
                OfficialStatus ostatus = data.getOfficialStatus();
                if (ostatus != OfficialStatus.unknown) {
                    foo.statusToRegions.put(ostatus, territory);
                }
                foo.literatePopulation += data.getLiteratePopulation();
            }
        }
        // set cldr directory status
        final Set<String> languages = config.getCldrFactory().getAvailableLanguages();
        final Set<String> full_languages = config.getFullCldrFactory().getAvailableLanguages();
        for (String language : full_languages) {
            LanguageInfo foo = getRaw(temp, language);
            foo.cldrDir = languages.contains(language) ? CldrDir.main
                : CldrDir.seed;
        }
        getRaw(temp, "und").cldrDir = CldrDir.base;
        getRaw(temp, "zxx").cldrDir = CldrDir.base;

        // finalize and protect
        for (Entry<String, LanguageInfo> entry : temp.entrySet()) {
            final LanguageInfo value = entry.getValue();
            value.statusToRegions.freeze();
            value.level = sc.getLocaleCoverageLevel(Organization.cldr, entry.getKey());

        }
        languageCodeToInfo = Collections.unmodifiableMap(temp);
    }

    private static LanguageInfo getRaw(TreeMap<String, LanguageInfo> temp, String language) {
        LanguageInfo foo = temp.get(language);
        if (foo == null) {
            temp.put(language, foo = new LanguageInfo());
        }
        return foo;
    }
}