package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;

import doc.org.unicode.cldr.tool.MinimizeRegex;

public class TestCLDRLocaleCoverage extends TestFmwkPlus {
    private static StandardCodes sc = StandardCodes.make();
    private static final CLDRConfig CLDRCONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CLDRCONFIG.getSupplementalDataInfo();
    private static final CLDRFile ENGLISH = CLDRCONFIG.getEnglish();


    public static void main(String[] args) {
        new TestCLDRLocaleCoverage().run(args);
    }

    public void TestLanguageNameCoverage() {
        
        Set<String> additionsToTranslate = new TreeSet<>(Arrays.asList("zxx", "ceb", "ny", "co", "ht", "hmn", "la", "sm", "st", "su", "sa", "mul"));
        
        Map<String, Status> validity = Validity.getInstance().getCodeToStatus(LstrType.language);
        Multimap<Status, String> statusToLang = Multimaps.invertFrom(Multimaps.forMap(validity), TreeMultimap.create());
        Set<String> regular = (Set<String>) statusToLang.get(Status.regular);
        Set<String> regularPlus = ImmutableSet.<String>builder().addAll(regular).add("und").add("zxx").add("mul").build();
        Set<String> valid = validity.keySet();
        
        Factory factory = CLDRCONFIG.getCldrFactory();
        Set<String> mainLocales = new LinkedHashSet<>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : factory.getAvailableLanguages()) {
            String language = ltp.set(locale).getLanguage();
            if (language.equals("root")) language = "und";
            mainLocales.add(language);
        }
        mainLocales = ImmutableSet.copyOf(mainLocales);
        Set<String> localesForNames = new TreeSet<>();
        localesForNames.addAll(mainLocales);
        localesForNames.addAll(additionsToTranslate);
        localesForNames = ImmutableSet.copyOf(localesForNames);

        assertContains("regularPlus.containsAll(mainLocales)", regularPlus, localesForNames);
        
        CoverageLevel2 coverageLeveler = CoverageLevel2.getInstance("und");
        Multimap<Level, String> levelToLanguage = TreeMultimap.create();
        for (String locale : valid) {
            String path = CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, locale);
            Level level = coverageLeveler.getLevel(path);
            levelToLanguage.put(level, locale);
        }
        
        Set<String> coverageLocales = new TreeSet<>();
        for (Level level : Level.values()) {
            if (level == Level.COMPREHENSIVE) {
                continue;
            }
            //assertContains("mainLocales.containsAll(coverage:" + level + ")", localesForNames, levelToLanguage.get(level));
            coverageLocales.addAll(levelToLanguage.get(level));
        }
        
        // If this fails, it is because of a mismatch between coverage and the getCLDRLanguageCodes.
        // Usually a problem with coverage.
        boolean showRegex = !assertContains("localesForNames.containsAll(coverageLocales)", localesForNames, coverageLocales);
        showRegex |= !assertContains("coverageLocales.containsAll(localesForNames)", coverageLocales, localesForNames);
        if (showRegex || true) {
            String simplePattern = MinimizeRegex.simplePattern(localesForNames);
            warnln("Plain Regex for coverage:\n" + simplePattern);
            warnln("Compact Regex for coverage:\n" + MinimizeRegex.compressWith(localesForNames, new UnicodeSet("[a-z]")));
        }

        coverageLocales.addAll(SDI.getCLDRLanguageCodes());
        
        Map<String,Integer> official1M = getOfficial1M();
        Set<String> official1MSet = new TreeSet<>();
        for (String locale : official1M.keySet()) {
            if (!localesForNames.contains(locale)) {
                official1MSet.add(locale);
            }
        }
        warnln("Official with 1M+ speakers, need investigation of literacy: " + official1MSet);


//        assertContains("sdiLocales contains oldModernLocales", sdiLocales, oldModernLocales);
//        assertContains("oldModernLocales contains sdiLocales", oldModernLocales, sdiLocales);
        
        coverageLocales.removeAll(mainLocales);
        coverageLocales.removeAll(additionsToTranslate);
        
        for (String locale : localesForNames) {
            logln("\n" + locale + "\t" + ENGLISH.getName(locale));
        }

        logln("\nmainLocales:" + composeList(mainLocales, "\n\t", new StringBuilder()));
        logln("\nadditionsToTranslate:" + composeList(additionsToTranslate, "\n\t", new StringBuilder()));
        logln("\noldModernLocales:" + composeList(coverageLocales, "\n\t", new StringBuilder()));
    }
    
    private Map<String,Integer> getOfficial1M() {
        Counter<String> counter = new Counter<>();
        for (String region : SDI.getTerritoriesWithPopulationData()) {
            for (String language : SDI.getLanguagesForTerritoryWithPopulationData(region)) {
                PopulationData popData = SDI.getLanguageAndTerritoryPopulationData(language, region);
                OfficialStatus status = popData.getOfficialStatus();
                if (status == OfficialStatus.unknown) {
                    continue;
                }
                // we only care about names, so drop scripts
                int underbar = language.indexOf('_');
                if (underbar >= 0) {
                    language = language.substring(0, underbar);
                }
                counter.add(language, (int) popData.getLiteratePopulation());
            }
        }
        Map<String,Integer> result = new TreeMap<>();
        for (String language : counter.keySet()) {
            long litPop = counter.get(language);
            if (litPop >= 1_000_000) {
                result.put(language, (int)litPop);
            }
            
        }
        return ImmutableMap.copyOf(result);
    }

    static final StringBuilder composeList(Iterable<String> source, String separator, StringBuilder result) {
        String prefix = null;
        for (String item : source) {
            if (prefix == null || !item.startsWith(prefix)) {
                result.append(separator);
                prefix = item.substring(0,1); // only ascii
            } else {
                result.append(' ');
            }
            result.append(item);
        }
        return result;
    }

    private boolean assertContains(String title, Collection<String> set, Collection<String> subset) {
        boolean result = assertTrue(title, set.containsAll(subset));
        if (!result) {
            Set<String> temp = new LinkedHashSet<>(subset);
            temp.removeAll(set);
            Set<String> temp2 = new TreeSet<>();
            for (String locale : temp) {
                temp2.add(locale + "\t" + ENGLISH.getName(locale));
            }
            warnln("Missing:\t" + temp.size() + "\n\t" + CollectionUtilities.join(temp2, "\n\t"));
        }
        return result;
    }
    
    /**
     * Test whether there are any locales for the organization CLDR
     */
    public void TestCLDROrganizationPresence() {
        Set<String> cldrLocales = sc.getLocaleCoverageLocales(
            Organization.cldr, EnumSet.of(Level.MODERN));
        assertNotNull("Expected CLDR modern locales not to be null",
            cldrLocales);
        assertTrue("Expected locales for CLDR, but found none.",
            cldrLocales != null && !cldrLocales.isEmpty());
    }

    /**
     * Tests that cldr is a superset.
     */
    public void TestCldrSuperset() {
        checkCldrLocales(Organization.apple, ERR);
        checkCldrLocales(Organization.google, ERR);
        checkCldrLocales(Organization.microsoft, WARN);
    }

    static Set<String> SKIP_SUPERSET = ImmutableSet.of("to", "fo");
    
    private void checkCldrLocales(Organization organization, int warningLevel) {
        // use a union, so that items can be higher
        EnumSet<Level> modernModerate = EnumSet.of(Level.MODERATE, Level.MODERN);
        
        Set<String> orgLocalesModerate = sc.getLocaleCoverageLocales(organization, modernModerate);
        Set<String> cldrLocalesModerate = sc.getLocaleCoverageLocales(Organization.cldr, modernModerate);
        Set<String> failures = checkCldrLocalesSuperset(modernModerate, cldrLocalesModerate, organization, orgLocalesModerate, warningLevel,
            SKIP_SUPERSET);

        EnumSet<Level> modernSet = EnumSet.of(Level.MODERN);
        Set<String> orgLocalesModern = sc.getLocaleCoverageLocales(organization, modernSet);
        Set<String> cldrLocalesModern = sc.getLocaleCoverageLocales(Organization.cldr, modernSet);
        failures = new HashSet<>(failures);
        failures.addAll(SKIP_SUPERSET);
        checkCldrLocalesSuperset(modernSet, cldrLocalesModern, organization, orgLocalesModern, warningLevel, failures);
    }

    private Set<String> checkCldrLocalesSuperset(Set<Level> level, Set<String> cldrLocales, Organization organization, Set<String> orgLocales, int warningLevel,
        Set<String> skip) {
        if (!cldrLocales.containsAll(orgLocales)) {
            Set<String> diff2 = new LinkedHashSet<>(Sets.difference(orgLocales, cldrLocales));
            diff2.removeAll(skip);
            if (!diff2.isEmpty()) {
                String diffString = diff2.toString();
                String levelString = CollectionUtilities.join(level, "+");
                for (String localeId : diff2) {
                    diffString += "\n\t" + localeId + "\t" + CLDRConfig.getInstance().getEnglish().getName(localeId);
                }
                msg("The following " + organization.displayName + " " + levelString + " locales were absent from the "
                    + Organization.cldr.displayName + " " + levelString + " locales:" + diffString,
                    warningLevel, true, true);
            }
            return diff2;
        }
        return Collections.EMPTY_SET;
    }
}
