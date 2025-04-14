package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.unicode.cldr.icu.dev.util.ElapsedTimer;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.tool.ShowLocaleCoverage.StatusCounter;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CoreCoverageInfo.CoreItems;
import org.unicode.cldr.util.VettingViewer.MissingStatus;

public class CalculateLocaleCoverage {

    /** Output data */
    public static final class CoverageResult {
        public final Level staticLevel;
        public final String locale;
        public final String adjustedGoal;
        public final long found;
        public final long unconfirmedc;
        public final long missing;
        public final Level cldrLocaleLevelGoal;
        public String visibleLevelComputed;
        public boolean icu = false;
        public int sumFound;
        public int sumUnconfirmed;

        /** in order of the Level enum */
        public double proportions[] = new double[Level.values().length];

        public String shownMissingPaths[];

        public CoverageResult(
                CLDRLocale locale,
                Level adjustedGoal,
                long found,
                long unconfirmedc,
                long missing,
                Level cldrLocaleLevelGoal) {
            this.locale = locale.getBaseName();
            this.adjustedGoal = adjustedGoal.name();
            this.found = found;
            this.unconfirmedc = unconfirmedc;
            this.missing = missing;
            this.cldrLocaleLevelGoal = cldrLocaleLevelGoal;
            this.staticLevel =
                    CalculatedCoverageLevels.getInstance().getEffectiveCoverageLevel(locale);
        }

        public void setVisibleLevelComputed(String visibleLevelComputed) {
            this.visibleLevelComputed = visibleLevelComputed;
        }

        public void setICU(boolean b) {
            this.icu = b;
        }

        public void setSumFound(int sumFound) {
            this.sumFound = sumFound;
        }

        public void setSumUnconfirmed(int sumUnconfirmed) {
            this.sumUnconfirmed = sumUnconfirmed;
        }

        public void setProportion(Level level, Double double1) {
            this.proportions[level.ordinal()] = double1;
        }

        public void setMissingPaths(Set<CoreItems> shownMissingPaths) {
            this.shownMissingPaths =
                    shownMissingPaths.stream().map(m -> m.name()).toArray((n) -> new String[n]);
        }
    }
    ;

    /** Output map */
    Map<CLDRLocale, CoverageResult> data = new TreeMap<>();

    static final Logger logger = Logger.getLogger(CalculateLocaleCoverage.class.getName());

    // thresholds for measuring Level attainment
    private static final double BASIC_THRESHOLD = 1;
    private static final double MODERATE_THRESHOLD = 0.995;
    private static final double MODERN_THRESHOLD = 0.995;

    private final StandardCodes SC = StandardCodes.make();
    private final Factory factory;

    private final CLDRFile ENGLISH = CLDRConfig.getInstance().getEnglish();

    private static final RegexLookup<Boolean> SUPPRESS_PATHS_CAN_BE_EMPTY =
            new RegexLookup<Boolean>()
                    .add("\\[@alt=\"accounting\"]", true)
                    .add("\\[@alt=\"variant\"]", true)
                    .add("^//ldml/localeDisplayNames/territories/territory.*@alt=\"short", true)
                    .add("^//ldml/localeDisplayNames/languages/language.*_", true)
                    .add("^//ldml/numbers/currencies/currency.*/symbol", true)
                    .add("^//ldml/characters/exemplarCharacters", true);

    private static DraftStatus minimumDraftStatus = DraftStatus.unconfirmed;
    private PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(ENGLISH);

    final Set<String> COMMON_LOCALES;

    final StandardCodes STANDARD_CODES = StandardCodes.make();

    private final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            CLDRConfig.getInstance().getSupplementalDataInfo();

    public CalculateLocaleCoverage(Factory f) {
        this.factory = f;
        COMMON_LOCALES = factory.getAvailableLanguages();
    }

    public static Collection<CoverageResult> getCoverage(Factory f) {
        return getCoverage(f, null);
    }

    public static Collection<CoverageResult> getCoverage(Factory f, final String only) {

        CalculateLocaleCoverage c = new CalculateLocaleCoverage(f);
        c.calculateCoverage(only);
        return c.data.values();
    }

    private void calculateCoverage(final String only) {
        Set<String> locales = COMMON_LOCALES;
        Set<String> checkModernLocales =
                STANDARD_CODES.getLocaleCoverageLocales(
                        Organization.cldr, EnumSet.of(Level.MODERN));
        Set<String> availableLanguages = new TreeSet<>(factory.getAvailableLanguages());
        availableLanguages.addAll(checkModernLocales);

        Multimap<String, String> languageToRegion = TreeMultimap.create();
        LanguageTagParser ltp = new LanguageTagParser();
        LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer(true);
        for (String locale : factory.getAvailable()) {
            String country = ltp.set(locale).getRegion();
            if (!country.isEmpty()) {
                languageToRegion.put(ltc.transform(ltp.getLanguageScript()), country);
            }
        }
        languageToRegion = ImmutableMultimap.copyOf(languageToRegion);

        logger.info(Joiner.on("\n").join(languageToRegion.asMap().entrySet()));

        logger.info("# Calculating locale coverage: " + availableLanguages);

        NumberFormat percentFormat = NumberFormat.getPercentInstance(Locale.ENGLISH);
        percentFormat.setMaximumFractionDigits(1);

        Relation<MissingStatus, String> missingPaths =
                Relation.of(
                        new EnumMap<MissingStatus, Set<String>>(MissingStatus.class),
                        TreeSet.class,
                        CLDRFile.getComparator(DtdType.ldml));
        Set<String> unconfirmed = new TreeSet<>(CLDRFile.getComparator(DtdType.ldml));

        Set<String> defaultContents = SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales();

        Counter<Level> foundCounter = new Counter<>();
        Counter<Level> unconfirmedCounter = new Counter<>();
        Counter<Level> missingCounter = new Counter<>();

        List<Level> levelsToShow = new ArrayList<>(EnumSet.allOf(Level.class));
        levelsToShow.remove(Level.COMPREHENSIVE);
        levelsToShow.remove(Level.UNDETERMINED);
        levelsToShow = ImmutableList.copyOf(levelsToShow);
        List<Level> reversedLevels = new ArrayList<>(levelsToShow);
        Collections.reverse(reversedLevels);
        reversedLevels = ImmutableList.copyOf(reversedLevels);

        int localeCount = 0;

        NumberFormat tsvPercent = NumberFormat.getPercentInstance(Locale.ENGLISH);
        tsvPercent.setMaximumFractionDigits(2);

        long start = System.currentTimeMillis();
        LikelySubtags likelySubtags = new LikelySubtags();

        EnumMap<Level, Double> targetLevel = new EnumMap<>(Level.class);
        targetLevel.put(Level.CORE, 2 / 100d);
        targetLevel.put(Level.BASIC, 16 / 100d);
        targetLevel.put(Level.MODERATE, 33 / 100d);
        targetLevel.put(Level.MODERN, 100 / 100d);

        Multimap<String, String> pathToLocale = TreeMultimap.create();

        Counter<Level> computedLevels = new Counter<>();
        Counter<Level> computedSublocaleLevels = new Counter<>();

        for (String locale : availableLanguages) {
            if (only != null && !locale.equals((only))) {
                continue;
            }
            logger.info("Begin Calc Cov: " + locale);
            ElapsedTimer et = new ElapsedTimer("  End Calc Cov: " + locale + " in {0}");
            try {
                if (locale.contains("supplemental") // for old versionsl
                //                        || locale.startsWith("sr_Latn")
                ) {
                    continue;
                }
                if (locales != null && !locales.contains(locale)) {
                    String base = CLDRLocale.getInstance(locale).getLanguage();
                    if (!locales.contains(base)) {
                        continue;
                    }
                }
                // skip these
                if (defaultContents.contains(locale)
                        || LocaleNames.ROOT.equals(locale)
                        || LocaleNames.UND.equals(locale)) {
                    continue;
                }

                String region = ltp.set(locale).getRegion();
                if (!region.isEmpty()) continue; // skip region sub-locales.

                final Level cldrLocaleLevelGoal =
                        SC.getLocaleCoverageLevel(Organization.cldr, locale);
                final String specialFlag = getSpecialFlag(locale);

                final boolean cldrLevelGoalBasicToModern =
                        Level.CORE_TO_MODERN.contains(cldrLocaleLevelGoal);

                // String max = likelySubtags.maximize(locale);
                // if (max == null) {
                //     logger.info("could not maximize " + locale);
                //     continue;
                // }
                // ltp.set(max);
                // if (!ltp.isValid()) {
                //     logger.info("could not ltp set " + max);
                // }
                // final String script = ltp.getScript();
                // final String defRegion = ltp.getRegion();

                final String language = likelySubtags.minimize(locale);

                if (language == null) {
                    logger.info("skipping unminimizable " + locale);
                    continue;
                }

                missingPaths.clear();
                unconfirmed.clear();

                final CLDRFile file = factory.make(locale, true, minimumDraftStatus);

                Iterable<String> pathSource = new IterableFilter(file.fullIterable());

                VettingViewer.getStatus(
                        pathSource,
                        file,
                        pathHeaderFactory,
                        foundCounter,
                        unconfirmedCounter,
                        missingCounter,
                        missingPaths,
                        unconfirmed);

                CoverageResult cr;

                {
                    long found = 0;
                    long unconfirmedc = 0;
                    long missing = 0;
                    Level adjustedGoal =
                            cldrLocaleLevelGoal.compareTo(Level.BASIC) < 0
                                    ? Level.BASIC
                                    : cldrLocaleLevelGoal;
                    for (Level level : Level.values()) {
                        if (level.compareTo(adjustedGoal) <= 0) {
                            found += foundCounter.get(level);
                            unconfirmedc += unconfirmedCounter.get(level);
                            missing += missingCounter.get(level);
                        }
                    }

                    cr =
                            new CoverageResult(
                                    CLDRLocale.getInstance(locale),
                                    adjustedGoal,
                                    found,
                                    unconfirmedc,
                                    missing,
                                    cldrLocaleLevelGoal);
                    data.put(CLDRLocale.getInstance(locale), cr);
                }

                Collection<String> sublocales = languageToRegion.asMap().get(language);
                if (sublocales == null) {
                    sublocales = Collections.emptySet();
                }
                sublocales = ImmutableSet.copyOf(sublocales);

                // get the totals

                EnumMap<Level, Integer> totals = new EnumMap<>(Level.class);
                EnumMap<Level, Integer> confirmed = new EnumMap<>(Level.class);
                Set<CoreItems> specialMissingPaths = EnumSet.noneOf(CoreItems.class);

                StatusCounter starredCounter = new StatusCounter();

                {
                    Multimap<CoreItems, String> detailedErrors = TreeMultimap.create();
                    Set<CoreItems> coverage =
                            CoreCoverageInfo.getCoreCoverageInfo(file, detailedErrors);
                    for (CoreItems item : coverage) {
                        foundCounter.add(item.desiredLevel, 1);
                    }
                    for (Entry<CoreItems, String> entry : detailedErrors.entries()) {
                        CoreItems coreItem = entry.getKey();
                        String path = entry.getValue();
                        specialMissingPaths.add(coreItem);
                        // if goal (eg modern) >= itemLevel, indicate it is missing
                        if (coreItem.desiredLevel == Level.BASIC) {
                            starredCounter.gatherStarred(path, null);
                        }
                        missingCounter.add(coreItem.desiredLevel, 1);
                    }
                }

                int sumFound = 0;
                int sumMissing = 0;
                int sumUnconfirmed = 0;

                for (Level level : levelsToShow) {
                    long foundCount = foundCounter.get(level);
                    long unconfirmedCount = unconfirmedCounter.get(level);
                    long missingCount = missingCounter.get(level);

                    sumFound += foundCount;
                    sumUnconfirmed += unconfirmedCount;
                    sumMissing += missingCount;

                    confirmed.put(level, sumFound);
                    totals.put(level, sumFound + sumUnconfirmed + sumMissing);
                }

                // double modernTotal = totals.get(Level.MODERN);

                // first get the accumulated values
                EnumMap<Level, Integer> accumTotals = new EnumMap<>(Level.class);
                EnumMap<Level, Integer> accumConfirmed = new EnumMap<>(Level.class);
                int currTotals = 0;
                int currConfirmed = 0;
                for (Level level : levelsToShow) {
                    currTotals += totals.get(level);
                    currConfirmed += confirmed.get(level);
                    accumConfirmed.put(level, currConfirmed);
                    accumTotals.put(level, currTotals);
                }

                // print the totals

                Level computed = Level.UNDETERMINED;
                Map<Level, Double> levelToProportion = new EnumMap<>(Level.class);

                for (Level level : reversedLevels) {
                    int confirmedCoverage = accumConfirmed.get(level);
                    double total = accumTotals.get(level);

                    final double proportion = confirmedCoverage / total;
                    levelToProportion.put(level, proportion);

                    if (computed == Level.UNDETERMINED) {
                        switch (level) {
                            case MODERN:
                                if (proportion >= MODERN_THRESHOLD) {
                                    computed = level;
                                }
                                break;
                            case MODERATE:
                                if (proportion >= MODERATE_THRESHOLD) {
                                    computed = level;
                                }
                                break;
                            case BASIC:
                                if (proportion >= BASIC_THRESHOLD) {
                                    computed = level;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }

                Set<CoreItems> shownMissingPaths = EnumSet.noneOf(CoreItems.class);
                Level computedWithCore = computed == Level.UNDETERMINED ? Level.BASIC : computed;
                for (CoreItems item : specialMissingPaths) {
                    if (item.desiredLevel.compareTo(computedWithCore) <= 0) {
                        shownMissingPaths.add(item);
                    } else {
                        int debug = 0;
                    }
                }
                computedLevels.add(computed, 1);
                computedSublocaleLevels.add(computed, sublocales.size());

                // final String coreMissingString = Joiner.on(", ").join(shownMissingPaths);
                final String visibleLevelComputed = computed.toString();
                // final String goalComparedToComputed = computed == cldrLocaleLevelGoal
                //     ? " ≡"
                //     : cldrLocaleLevelGoal.compareTo(computed) < 0 ? " <" : " >";

                cr.setVisibleLevelComputed(visibleLevelComputed);

                try {
                    cr.setICU(!getIcuValue(language).isEmpty());
                } catch (Throwable t) {
                    logger.log(
                            java.util.logging.Level.SEVERE, "getting icu value for " + language, t);
                }
                cr.setSumFound(sumFound);
                cr.setSumUnconfirmed(sumUnconfirmed);

                // print the totals
                for (Level level : reversedLevels) {
                    cr.setProportion(level, (levelToProportion.get(level)));
                }

                cr.setMissingPaths(shownMissingPaths);

                localeCount++;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            logger.info(et.toString());
        }

        long end = System.currentTimeMillis();
        logger.info(
                (end - start) + " millis = " + ((end - start) / localeCount) + " millis/locale");
    }

    private static String getSpecialFlag(String locale) {
        return StandardCodes.make().getLocaleCoverageLevel(Organization.special, locale)
                        == Level.UNDETERMINED
                ? ""
                : "‡";
    }

    private static class IterableFilter implements Iterable<String> {
        private Iterable<String> source;

        IterableFilter(Iterable<String> source) {
            this.source = source;
        }

        /**
         * When some paths are defined after submission, we need to change them to COMPREHENSIVE in
         * computing the vetting status.
         */
        private static final Set<String> SUPPRESS_PATHS_AFTER_SUBMISSION = ImmutableSet.of();

        @Override
        public Iterator<String> iterator() {
            return new IteratorFilter(source.iterator());
        }

        private static class IteratorFilter implements Iterator<String> {
            Iterator<String> source;
            String peek;

            public IteratorFilter(Iterator<String> source) {
                this.source = source;
                fillPeek();
            }

            @Override
            public boolean hasNext() {
                return peek != null;
            }

            @Override
            public String next() {
                String result = peek;
                fillPeek();
                return result;
            }

            private void fillPeek() {
                peek = null;
                while (source.hasNext()) {
                    peek = source.next();
                    // if it is ok to assess, then break
                    if (!SUPPRESS_PATHS_AFTER_SUBMISSION.contains(peek)
                            && SUPPRESS_PATHS_CAN_BE_EMPTY.get(peek) != Boolean.TRUE) {
                        break;
                    }
                    peek = null;
                }
            }
        }
    }

    private final CoverageInfo coverageInfo = new CoverageInfo(SUPPLEMENTAL_DATA_INFO);

    private static String getIcuValue(String locale) {
        return ICU_Locales.contains(new ULocale(locale)) ? "ICU" : "";
    }

    private static final Set<ULocale> ICU_Locales =
            ImmutableSet.copyOf(ULocale.getAvailableLocales());
}
