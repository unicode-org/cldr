package org.unicode.cldr.test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitConverter.UnitSystem;
import org.unicode.cldr.util.UnitPathType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.util.Output;

/**
 * Return the best samples for illustrating minimal pairs
 * @author markdavis
 *
 */
public class BestMinimalPairSamples {
    public static final String EQUALS_NOMINATIVE = "＝nominative";
    private static final Joiner PLUS_JOINER = Joiner.on("+");
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();

    final private CLDRFile cldrFile;
    final private GrammarInfo grammarInfo;
    final private PluralRules pluralInfo;
    final private PluralRules ordinalInfo;
    final private ICUServiceBuilder icuServiceBuilder;
    private CaseAndGenderSamples caseAndGenderSamples = null; // lazy evaluated
    private Multimap<String, String> genderToUnits;
    private Multimap<Integer, String> uniqueCaseAndCountToUnits;
    private Multimap<String, String> distinctNominativeCaseToUnit;
    private final boolean gatherStats;

    public BestMinimalPairSamples(CLDRFile cldrFile, ICUServiceBuilder icuServiceBuilder, boolean gatherStats) {
        this.cldrFile = cldrFile;
        grammarInfo = supplementalDataInfo.getGrammarInfo(cldrFile.getLocaleID());
        pluralInfo = supplementalDataInfo.getPlurals(PluralType.cardinal, cldrFile.getLocaleID()).getPluralRules();
        ordinalInfo = supplementalDataInfo.getPlurals(PluralType.ordinal, cldrFile.getLocaleID()).getPluralRules();
        this.icuServiceBuilder = icuServiceBuilder;
        genderToUnits = TreeMultimap.create();
        uniqueCaseAndCountToUnits = TreeMultimap.create();
        this.gatherStats = gatherStats;
    }


    static final class CaseAndGenderSamples {
        private final Map<String, Pair<String, String>> genderCache;
        private final Map<String, String> caseCache;
        private final String caseUnitId;

        public CaseAndGenderSamples(Map<String, String> caseCache2, String bestCaseUnitId,  Map<String, Pair<String, String>> genderCache2) {
            genderCache  = genderCache2;
            caseCache = caseCache2;
            caseUnitId = bestCaseUnitId;
        }

        public String getGender(String gender, Output<String> shortUnitId) {
            Pair<String, String> result = genderCache.get(gender);
            if (result == null) {
                return null;
            }
            shortUnitId.value = result.getFirst();
            return result.getSecond();
        }

        public String getCase(String unitCase, Output<String> shortUnitId) {
            shortUnitId.value = caseUnitId;
            return caseCache.get(unitCase);
        }
    }

    /**
     * Returns a "good" value for a unit. Favors metric units, and simple units
     * @param shortUnitId
     */
    public synchronized String getBestUnitWithGender(String gender, Output<String> shortUnitId) {
        if (grammarInfo == null) {
            return null;
        }
        if (caseAndGenderSamples == null) {
            caseAndGenderSamples = loadCaches();
        }
        return caseAndGenderSamples.getGender(gender, shortUnitId);
    }

    /**
     * Returns a "good" value for a unit. Favors metric units, and simple units
     * @param shortUnitId
     */
    public synchronized String getBestUnitWithCase(String unitCase, Output<String> shortUnitId) {
        if (grammarInfo == null) {
            return null;
        }
        if (caseAndGenderSamples == null) {
            caseAndGenderSamples = loadCaches();
        }
        return caseAndGenderSamples.getCase(unitCase, shortUnitId);
    }

    static final Set<String> SKIP_CASE = ImmutableSet.of(
        "concentr-ofglucose",
        "concentr-portion",
        "length-100-kilometer",
        "pressure-ofhg");

    public CaseAndGenderSamples loadCaches() {
        Collection<String> unitCases = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units);
        Map<String,String> genderResults = Maps.newHashMap();
        Multimap<String, Pair<String,String>> unitPatternToCaseAndCounts = TreeMultimap.create();
        distinctNominativeCaseToUnit = TreeMultimap.create();

        int bestCaseFormCount = 0;
        String bestCaseUnitId = null;
        Multimap<String, Pair<String,String>> bestUnitPatternToCases = null;
        Multimap<String, String> unitToDistinctNominativeCase = TreeMultimap.create();

        for (String longUnitId : GrammarInfo.getUnitsToAddGrammar()) {
            String possibleGender = cldrFile.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + longUnitId + "\"]/gender");
            String shortUnitId = ExampleGenerator.UNIT_CONVERTER.getShortId(longUnitId);
            if (shortUnitId.equals("hour") && cldrFile.getLocaleID().equals("ta")) {
                int debug = 0;
            }
            if (possibleGender != null) {
                if (gatherStats) {
                    genderToUnits.put(possibleGender, shortUnitId);
                }
                String formerLongUnitId = genderResults.get(possibleGender);
                if (formerLongUnitId == null || isBetterUnit(longUnitId, formerLongUnitId)) {
                    genderResults.put(possibleGender, longUnitId);
                }
            }
            if (!unitCases.isEmpty()) {
                unitPatternToCaseAndCounts.clear();
                for (String count : pluralInfo.getKeywords()) {
                    for (String unitCase : unitCases) {
                        String grammarAttributes = GrammarInfo.getGrammaticalInfoAttributes(grammarInfo, UnitPathType.unit, count, null, unitCase);
                        String unitPattern = cldrFile.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + longUnitId + "\"]/unitPattern" + grammarAttributes);
                        if (unitPattern == null) {
                            continue;
                        }
                        unitPattern = unitPattern.replace("\u00A0", "").trim();
                        final Pair<String, String> caseAndCount = Pair.of(unitCase, count);
                        unitPatternToCaseAndCounts.put(unitPattern, caseAndCount);
                    }
                }
                int caseFormCount = unitPatternToCaseAndCounts.keySet().size();

                boolean alwaysSameAsNominative = true;
                TreeMultimap<Pair<String, String>, String> caseAndCountToPattern = Multimaps.invertFrom(unitPatternToCaseAndCounts, TreeMultimap.create());
                for (Entry<Pair<String, String>, String> entry : caseAndCountToPattern.entries()) {
                    Pair<String, String> caseAndCount = entry.getKey();
                    String pattern = entry.getValue();
                    String gCase = caseAndCount.getFirst();
                    if (!gCase.equals("nominative")) {
                        Pair<String, String> nomPair = Pair.of("nominative", caseAndCount.getSecond());
                        NavigableSet<String> nomPatterns = caseAndCountToPattern.get(nomPair);
                        if (!nomPatterns.contains(pattern)) {
                            unitToDistinctNominativeCase.put(shortUnitId, gCase);
                            alwaysSameAsNominative = false;
                        }
                    }
                }
                for (Entry<String, Collection<String>> entry : unitToDistinctNominativeCase.asMap().entrySet()) {
                    distinctNominativeCaseToUnit.put(PLUS_JOINER.join(entry.getValue()), entry.getKey());
                }
                if (alwaysSameAsNominative) {
                    distinctNominativeCaseToUnit.put(EQUALS_NOMINATIVE, shortUnitId);
                }

                if (gatherStats
                    && !SKIP_CASE.contains(longUnitId)) {
                    uniqueCaseAndCountToUnits.put(caseFormCount, shortUnitId);
                }

                // For case, we should do something fancier, but for now we pick the units with the largest number of distinct forms.
                int diff = caseFormCount - bestCaseFormCount;
                if (diff > 0
                    || diff == 0
                    && isBetterUnit(longUnitId, bestCaseUnitId)) {
//                    System.out.println(cldrFile.getLocaleID() + "\t" + longUnitId + " better than " + bestCaseUnitId);
//                 if (WORSE.contains(longUnitId)) {
//                        isBetterUnit(longUnitId, bestCaseUnitId);
//                    }
                    bestCaseFormCount = caseFormCount;
                    bestCaseUnitId = longUnitId;
                    bestUnitPatternToCases = TreeMultimap.create(unitPatternToCaseAndCounts);
                }
            }
        }
        // Fill the case cache with the most distinctive forms.
        Map<String, String> caseCache = getBestCasePatterns(bestUnitPatternToCases);

        // Make the gender cache be translated units as well as unit IDs
        Count count = pluralInfo.getKeywords().contains("one") ? Count.one : Count.other;
        Map<String,Pair<String,String>> result2 = Maps.newHashMap();

        for (Entry<String, String> entry : genderResults.entrySet()) {
            String longUnitId = entry.getValue();
            String unitPattern = cldrFile.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + longUnitId + "\"]/unitPattern[@count=\"" + count + "\"]");
            unitPattern = unitPattern.replace("{0}", "").replace("\u00A0", "").trim();
            result2.put(entry.getKey(), Pair.of(ExampleGenerator.UNIT_CONVERTER.getShortId(longUnitId), unitPattern));
        }
        // it doesn't matter if we reset this due to multiple threads
        Map<String, Pair<String, String>> genderCache = ImmutableMap.copyOf(result2);
        CaseAndGenderSamples result = new CaseAndGenderSamples(caseCache, ExampleGenerator.UNIT_CONVERTER.getShortId(bestCaseUnitId), genderCache);

        genderToUnits = ImmutableMultimap.copyOf(genderToUnits);
        uniqueCaseAndCountToUnits = ImmutableMultimap.copyOf(uniqueCaseAndCountToUnits);
        distinctNominativeCaseToUnit = ImmutableMultimap.copyOf(distinctNominativeCaseToUnit);
        return result;
    }

    /**
     * Get the a pattern that is most unique for each case.
     * @param bestUnitPatternToCases
     * @return
     */
    private Map<String, String> getBestCasePatterns(Multimap<String, Pair<String, String>> bestUnitPatternToCases) {
        if (bestUnitPatternToCases == null || bestUnitPatternToCases.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String,String> result = new TreeMap<>();
        while (true) {
            String bestPattern = getBestPattern(bestUnitPatternToCases);
            Pair<String, String> bestCaseCount = bestUnitPatternToCases.get(bestPattern).iterator().next();
            String bestCase = bestCaseCount.getFirst();
            String bestCount = bestCaseCount.getSecond();
            String sample = getPluralOrOrdinalSample(PluralType.cardinal, bestCount);
            if (sample == null) { // debugging
                getPluralOrOrdinalSample(PluralType.cardinal, bestCount);
            }
            result.put(bestCaseCount.getFirst(), bestPattern.replace("{0}", sample));
            TreeMultimap<Pair<String, String>, String> caseToPatterns = Multimaps.invertFrom(bestUnitPatternToCases, TreeMultimap.create());
            for (String count : pluralInfo.getKeywords()) {
                caseToPatterns.removeAll(Pair.of(bestCase, count));
            }
            if (caseToPatterns.keySet().isEmpty()) {
                return result;
            }
            bestUnitPatternToCases = Multimaps.invertFrom(caseToPatterns, TreeMultimap.create());
        }
    }

    private String getBestPattern(Multimap<String, Pair<String, String>> bestUnitPatternToCases) {
        int bestCaseSize = 1000;
        String bestPattern = null;
        Collection<Pair<String, String>> bestCase = null;
        for (Entry<String, Collection<Pair<String, String>>> entry : bestUnitPatternToCases.asMap().entrySet()) {
            final Collection<Pair<String, String>> setOfCases = entry.getValue();
            if (setOfCases.size() < bestCaseSize) {
                bestCaseSize = setOfCases.size();
                bestPattern = entry.getKey();
                bestCase = setOfCases;
            }
        }
        return bestPattern;
    }

    public boolean isBetterUnit(String longUnitId, String formerLongUnitId) {
        // replace if as good or better (where better is smaller). Metric is better. If both metric, choose alphabetical
        boolean isBetter = false;
        int diff = systemWeight(longUnitId) - systemWeight(formerLongUnitId);
        if (diff < 0) {
            isBetter = true;
        } else if (diff == 0) {
            diff = categoryWeight(longUnitId) - categoryWeight(formerLongUnitId);
            if (diff < 0) {
                isBetter = true;
            } else if (diff == 0 && longUnitId.compareTo(formerLongUnitId) < 0) {
                isBetter = true;
            }
        }
        return isBetter;
    }

    static final Set<String> WORSE = ImmutableSet.of("length-100-kilometer", "length-mile-scandinavian");
    /**
     * better result is smaller
     * @param longUnitId
     * @return
     */
    public int systemWeight(String longUnitId) {
        if (WORSE.contains(longUnitId)) {
            return 1;
        }
        Set<UnitSystem> systems = ExampleGenerator.UNIT_CONVERTER.getSystemsEnum(ExampleGenerator.UNIT_CONVERTER.getShortId(longUnitId));
        if (systems.contains(UnitSystem.metric)) {
            return 0;
        }
        return 1;
    }

    private int categoryWeight(String longUnitId) {
        if (longUnitId.startsWith("length")) {
            return 0;
        } else if (longUnitId.startsWith("weight")) {
            return 1;
        } else if (longUnitId.startsWith("duration")) {
            return 2;
        }
        return 999;
    }

    public String getPluralOrOrdinalSample(PluralType pluralType, String code) {
        PluralRules rules = pluralType == PluralType.cardinal ? pluralInfo : ordinalInfo;
        FixedDecimalSamples samples = rules.getDecimalSamples(code, SampleType.INTEGER);
        if (samples == null) {
            samples = rules.getDecimalSamples(code, SampleType.DECIMAL);
        }
        if (samples == null) {
            return null;
        }

        // get good sample. Avoid zero if possible
        FixedDecimal sample = null;
        for (FixedDecimalRange sampleRange : samples.getSamples()) {
            sample = sampleRange.start;
            if (sample.doubleValue() != 0d) {
                break;
            }
        }

        if (icuServiceBuilder != null) {
            int visibleDigits = sample.getVisibleDecimalDigitCount();
            DecimalFormat nf;
            if (visibleDigits == 0) {
                nf = icuServiceBuilder.getNumberFormat(0); // 0 is integer, 1 is decimal
            } else {
                nf = icuServiceBuilder.getNumberFormat(1); // 0 is integer, 1 is decimal
                int minFracDigits = nf.getMinimumFractionDigits();
                int maxFracDigits = nf.getMaximumFractionDigits();
                if (minFracDigits != visibleDigits || maxFracDigits != visibleDigits) {
                    nf = (DecimalFormat) nf.clone();
                    nf.setMaximumFractionDigits(visibleDigits);
                    nf.setMinimumFractionDigits(visibleDigits);
                }
            }
            return nf.format(sample);
        }
        return sample.toString();
    }

    /**
     * Get the best value to show, plus the shortUnitId if relevant (case/gender)
     */
    public String getBestValue(String header, String code, Output<String> shortUnitId) {
        String result = null;
        switch(header) {
        case "Case":
            result = getBestUnitWithCase(code, shortUnitId);
            break;
        case "Gender":
            result = getBestUnitWithGender(code, shortUnitId);
            break;
        case "Ordinal":
            result = getPluralOrOrdinalSample(PluralType.ordinal, code);
            shortUnitId.value = "n/a";
            break;
        case "Plural":
            result = getPluralOrOrdinalSample(PluralType.cardinal, code);
            shortUnitId.value = "n/a";
            break;
        }
        return result == null ? "X" : result;
    }

    public Multimap<String, String> getGenderToUnits() {
        return genderToUnits;
    }

    public Multimap<Integer, String> getUniqueCaseAndCountToUnits() {
        return uniqueCaseAndCountToUnits;
    }
    public Multimap<String, String> getDistinctNominativeCaseToUnit() {
        return distinctNominativeCaseToUnit;
    }
}