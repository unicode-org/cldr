package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.MinimizeRegex;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.VoteResolver;

public class TestCLDRLocaleCoverage extends TestFmwkPlus {
    private static StandardCodes sc = StandardCodes.make();
    private static final CLDRConfig CLDRCONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CLDRCONFIG.getSupplementalDataInfo();
    private static final CLDRFile ENGLISH = CLDRCONFIG.getEnglish();

    public static void main(String[] args) {
        new TestCLDRLocaleCoverage().run(args);
    }

    public void TestLanguageNameCoverage() {
        // mainLocales has the locales in common/main, which is basically the set in
        // attributeValueValidity.xml $language..
        // We add in additionsToTranslate below the set in attributeValueValidity.xml
        // $languageExceptions
        // (both sets are included in SDI.getCLDRLanguageCodes() but we do not use that until
        // later).
        Set<String> additionsToTranslate = ImmutableSortedSet.of(LocaleNames.ZXX, LocaleNames.MUL);

        Map<String, Status> validity = Validity.getInstance().getCodeToStatus(LstrType.language);
        Multimap<Status, String> statusToLang =
                Multimaps.invertFrom(Multimaps.forMap(validity), TreeMultimap.create());
        Set<String> regular = (Set<String>) statusToLang.get(Status.regular);
        Set<String> regularPlus =
                ImmutableSet.<String>builder()
                        .addAll(regular)
                        .add(LocaleNames.UND)
                        .add(LocaleNames.ZXX)
                        .add(LocaleNames.MUL)
                        .build();
        Set<String> valid = validity.keySet();

        Factory factory = CLDRCONFIG.getCldrFactory();
        Set<String> mainLocales = new LinkedHashSet<>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : factory.getAvailableLanguages()) {
            String language = ltp.set(locale).getLanguage();
            if (language.equals(LocaleNames.ROOT)) {
                language = LocaleNames.UND;
            } else if (!StandardCodes.isLocaleAtLeastBasic(language)) {
                continue;
            }
            mainLocales.add(language);
        }
        mainLocales = ImmutableSet.copyOf(mainLocales);
        Set<String> localesForNames = new TreeSet<>();
        localesForNames.addAll(mainLocales);
        localesForNames.addAll(additionsToTranslate);
        localesForNames = ImmutableSet.copyOf(localesForNames);

        assertContains("regularPlus.containsAll(mainLocales)", regularPlus, localesForNames);

        CoverageLevel2 coverageLeveler = CoverageLevel2.getInstance(LocaleNames.UND);
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
            // assertContains("mainLocales.containsAll(coverage:" + level + ")", localesForNames,
            // levelToLanguage.get(level));
            coverageLocales.addAll(levelToLanguage.get(level));
        }

        // If this fails, it is because of a mismatch between coverage and the getCLDRLanguageCodes.
        // Usually a problem with coverage.
        boolean showRegex =
                !assertContains(
                        "localesForNames.containsAll(coverageLocales)",
                        localesForNames,
                        coverageLocales);
        showRegex |=
                !assertContains(
                        "coverageLocales.containsAll(localesForNames) - add to %language80 or lower under coverageLevels.xml?",
                        coverageLocales, localesForNames);
        if (showRegex) {
            String simplePattern = MinimizeRegex.simplePattern(localesForNames);
            warnln("Plain Regex for coverage:\n" + simplePattern);
        }

        coverageLocales.addAll(SDI.getCLDRLanguageCodes());

        Map<String, Integer> official1M = getOfficial1M();
        Map<Integer, String> official1MSetNames = new TreeMap<>(Comparator.reverseOrder());
        for (Entry<String, Integer> localeAndSize : official1M.entrySet()) {
            final String locale = localeAndSize.getKey();
            if (!localesForNames.contains(locale)
                    && CLDRLocale.getInstance(locale).getParent().equals(CLDRLocale.ROOT)) {
                official1MSetNames.put(
                        localeAndSize.getValue(), "\t" + locale + "\t" + ENGLISH.getName(locale));
            }
        }
        if (!official1MSetNames.isEmpty()) {
            logln(
                    "Official with 1M+ speakers, need investigation of literacy:\n\t"
                            + Joiner.on("\n\t").join(official1MSetNames.entrySet()));
        }

        //        assertContains("sdiLocales contains oldModernLocales", sdiLocales,
        // oldModernLocales);
        //        assertContains("oldModernLocales contains sdiLocales", oldModernLocales,
        // sdiLocales);

        coverageLocales.removeAll(mainLocales);
        coverageLocales.removeAll(additionsToTranslate);

        for (String locale : localesForNames) {
            logln("\n" + locale + "\t" + ENGLISH.getName(locale));
        }

        logln("\nmainLocales:" + composeList(mainLocales, "\n\t", new StringBuilder()));
        logln(
                "\nadditionsToTranslate:"
                        + composeList(additionsToTranslate, "\n\t", new StringBuilder()));
        logln("\noldModernLocales:" + composeList(coverageLocales, "\n\t", new StringBuilder()));
    }

    private Map<String, Integer> getOfficial1M() {
        Counter<String> counter = new Counter<>();
        for (String region : SDI.getTerritoriesWithPopulationData()) {
            for (String language : SDI.getLanguagesForTerritoryWithPopulationData(region)) {
                PopulationData popData =
                        SDI.getLanguageAndTerritoryPopulationData(language, region);
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
        Map<String, Integer> result = new TreeMap<>();
        for (String language : counter.keySet()) {
            long litPop = counter.get(language);
            if (litPop >= 1_000_000) {
                result.put(language, (int) litPop);
            }
        }
        return ImmutableMap.copyOf(result);
    }

    static final StringBuilder composeList(
            Iterable<String> source, String separator, StringBuilder result) {
        String prefix = null;
        for (String item : source) {
            if (prefix == null || !item.startsWith(prefix)) {
                result.append(separator);
                prefix = item.substring(0, 1); // only ascii
            } else {
                result.append(' ');
            }
            result.append(item);
        }
        return result;
    }

    private boolean assertContains(
            String title, Collection<String> set, Collection<String> subset) {
        set = removeBelowBasic(set);
        subset = removeBelowBasic(subset);
        boolean result = set.containsAll(subset);
        if (!result) {
            Set<String> temp = new LinkedHashSet<>(subset);
            temp.removeAll(set);
            Set<String> temp2 = new TreeSet<>();
            for (String locale : temp) {
                temp2.add(locale + "\t" + ENGLISH.getName(locale));
            }
            errln(title + ": Missing:\t" + temp.size() + "\n\t" + Joiner.on("\n\t").join(temp2));
        }
        return result;
    }

    private Collection<String> removeBelowBasic(Collection<String> set) {
        Collection<String> set2 = new TreeSet<>();
        for (String locale : set) {
            if (StandardCodes.isLocaleAtLeastBasic(locale)) {
                set2.add(locale);
            }
        }
        return set2;
    }

    /** Test whether there are any locales for the organization CLDR */
    public void TestCLDROrganizationPresence() {
        Set<String> cldrLocales =
                sc.getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));
        assertNotNull("Expected CLDR modern locales not to be null", cldrLocales);
        assertTrue(
                "Expected locales for CLDR, but found none.",
                cldrLocales != null && !cldrLocales.isEmpty());
    }

    /** Tests that cldr+special is a superset of the TC locales, with the right levels */
    public void TestCldrSuperset() {
        final Set<Organization> orgs = Organization.getTCOrgs();

        Map<Organization, Map<String, Level>> orgToLevels = new TreeMap<>();
        orgs.forEach(org -> orgToLevels.put(org, sc.getLocalesToLevelsFor(org)));

        Map<String, Level> special = sc.getLocalesToLevelsFor(Organization.special);

        Map<String, Level> cldr = sc.getLocalesToLevelsFor(Organization.cldr);

        // check that the cldr locales (+ special) have the max level of the TC locales

        for (Entry<String, Level> entry : cldr.entrySet()) {
            final String locale = entry.getKey();

            final Map<Organization, Level> orgToLevel =
                    orgToLevels.entrySet().stream()
                            .collect(
                                    Collectors.toMap(
                                            Entry::getKey,
                                            v -> {
                                                final Level l = v.getValue().get(locale);
                                                if (l == null) return Level.UNDETERMINED;
                                                return l;
                                            }));

            Level cldrLevel = entry.getValue();
            Level specialLevel = special.get(locale);
            boolean cldrLevelIsModern = cldrLevel.compareTo(Level.MODERN) >= 0;

            // check the vote count

            final int count =
                    (int)
                            orgToLevel.values().stream()
                                    .filter(TestCLDRLocaleCoverage::isPresentAndAtLeastModern)
                                    .count();
            final int countMin = 2;
            final boolean countAtLeast = count > countMin;
            int defaultVotes =
                    SupplementalDataInfo.getInstance()
                            .getRequiredVotes(CLDRLocale.getInstance(locale), null);

            if (countAtLeast && cldrLevelIsModern) {
                assertEquals(
                        "orgCount="
                                + count
                                + ", and cldrLevel="
                                + cldrLevel
                                + ", expected LOWER_BAR but it wasn't for "
                                + locale,
                        VoteResolver.LOWER_BAR,
                        defaultVotes);
            } else {
                assertNotEquals(
                        "orgCount="
                                + count
                                + ", and cldrLevel="
                                + cldrLevel
                                + ", expected "
                                + locale
                                + " to NOT have LOWER_BAR",
                        VoteResolver.LOWER_BAR,
                        defaultVotes);
            }

            // check the max level
            Level maxLevel =
                    Level.max(specialLevel, Level.max(orgToLevel.values().toArray(new Level[0])));
            assertEquals(
                    "cldr level = max for " + locale + " (" + ENGLISH.getName(locale) + ")",
                    cldrLevel,
                    maxLevel);
        }

        // check that the cldr locales include all of the other locale's
        orgToLevels
                .entrySet()
                .forEach(
                        e -> {
                            final Organization org = e.getKey();
                            final Map<String, Level> l = e.getValue();
                            checkCldrContains("cldr", cldr, org.name(), l);
                            checkCldrContains("cldr", cldr, "special", l);
                            // check that special doesn't overlap with TC, except for locales in
                            // LOCALE_CONTAINMENT_EXCEPTIONS
                            checkDisjoint("special", special, org.name(), l);
                        });
    }

    private static boolean isPresentAndAtLeastModern(Level orgLevel) {
        return orgLevel == Level.UNDETERMINED
                ? false
                : orgLevel.compareTo(Level.MODERN) >= 0 ? true : false;
    }

    private static final Set<String> ANY_LOCALE_SET = ImmutableSet.of("*");
    private static final Set<String> LOCALE_CONTAINMENT_EXCEPTIONS =
            ImmutableSet.of(
                    "sr_Latn", // auto-generated
                    "hi",
                    "sr",
                    "yue", // these are inserted by Locales.txt processing TODO don't add to special
                    "to",
                    "qu" // optional locales
                    );

    private void checkCldrContains(
            String firstName,
            Map<String, Level> first,
            String otherName,
            Map<String, Level> other) {
        assertEquals(
                firstName + " ⊇ " + otherName,
                Collections.emptySet(),
                Sets.difference(Sets.difference(other.keySet(), ANY_LOCALE_SET), first.keySet()));
    }

    private void checkDisjoint(
            String firstName,
            Map<String, Level> first,
            String otherName,
            Map<String, Level> other) {
        assertEquals(
                firstName + " ⩃ " + otherName,
                Collections.emptySet(),
                Sets.difference(
                        Sets.intersection(other.keySet(), first.keySet()),
                        LOCALE_CONTAINMENT_EXCEPTIONS));
    }

    public void TestParentCoverage() {
        final SupplementalDataInfo sd = SupplementalDataInfo.getInstance();
        final Set<String> defaultContentLocales = sd.getDefaultContentLocales();

        for (Organization organization : sc.getLocaleCoverageOrganizations()) {
            if (organization == Organization.special) {
                continue;
            }
            final Map<String, Level> localesToLevels = sc.getLocalesToLevelsFor(organization);
            for (Entry<String, Level> localeAndLevel : localesToLevels.entrySet()) {
                String originalLocale = localeAndLevel.getKey();
                Level level = localeAndLevel.getValue();
                String locale = originalLocale;
                while (true) {
                    String parent = LocaleIDParser.getParent(locale);
                    if (parent == null || parent.equals(LocaleNames.ROOT)) {
                        break;
                    }
                    if (!defaultContentLocales.contains(parent)
                            && !parent.equals("en_001")) { // en_001 is generated later from en_GB
                        Level parentLevel = localesToLevels.get(parent);
                        if (assertNotNull(
                                String.format(
                                        "Locales.txt: Entry '%s ; %s ; ...' is missing parent '%s ; %s ; ...'",
                                        organization, originalLocale, organization, parent),
                                parentLevel)) {
                            assertTrue(
                                    String.format(
                                            "Locales.txt: Entry '%s ; %s ; %s' should not be higher than parent '%s ; %s ; %s'",
                                            organization,
                                            originalLocale,
                                            level,
                                            organization,
                                            parent,
                                            parentLevel),
                                    parentLevel.compareTo(level) >= 0);
                        }
                    }
                    locale = parent;
                }
            }
        }
    }
}
