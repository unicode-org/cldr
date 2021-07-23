package org.unicode.cldr.test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitConverter.UnitSystem;
import org.unicode.cldr.util.UnitPathType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.text.PluralRules.SampleType;

/**
 * Return the best samples for illustrating minimal pairs
 * @author markdavis
 *
 */
public class BestMinimalPairSamples {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();

    final private CLDRFile cldrFile;
    final private GrammarInfo grammarInfo;
    final private PluralRules pluralInfo;
    final private PluralRules ordinalInfo;
    private CaseAndGenderSamples caseAndGenderSamples = null;

    public BestMinimalPairSamples(CLDRFile cldrFile) {
        this.cldrFile = cldrFile;
        grammarInfo = supplementalDataInfo.getGrammarInfo(cldrFile.getLocaleID());
        pluralInfo = supplementalDataInfo.getPlurals(PluralType.cardinal, cldrFile.getLocaleID()).getPluralRules();
        ordinalInfo = supplementalDataInfo.getPlurals(PluralType.ordinal, cldrFile.getLocaleID()).getPluralRules();
    }

    static final class CaseAndGenderSamples {
        private final Map<String, String> genderCache;
        private final Map<String, String> caseCache;

        public CaseAndGenderSamples(Map<String, String> caseCache2, Map<String, String> genderCache2) {
            genderCache  = genderCache2;
            caseCache = caseCache2;
        }
    }

    /**
     * Returns a "good" value for a unit. Favors metric units, and simple units
     */
    public synchronized String getBestUnitWithGender(String gender) {
        if (caseAndGenderSamples == null) {
            caseAndGenderSamples = loadCaches();
        }
        return caseAndGenderSamples.genderCache.get(gender);
    }

    /**
     * Returns a "good" value for a unit. Favors metric units, and simple units
     */
    public synchronized String getBestUnitWithCase(String unitCase) {
        if (caseAndGenderSamples == null) {
            caseAndGenderSamples = loadCaches();
        }
        return caseAndGenderSamples.caseCache.get(unitCase);
    }


    public CaseAndGenderSamples loadCaches() {
        Collection<String> unitCases = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units);
        Map<String,String> genderResults = Maps.newHashMap();
        Multimap<String, Pair<String,String>> unitPatternToCases = TreeMultimap.create();

        int bestCaseFormCount = 0;
        String bestCaseUnitId = null;
        Multimap<String, Pair<String,String>> bestUnitPatternToCases = null;

        for (String longUnitId : ExampleGenerator.UNITS) {
            String possibleGender = cldrFile.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + longUnitId + "\"]/gender");
            if (possibleGender != null) {
                String formerLongUnitId = genderResults.get(possibleGender);
                if (formerLongUnitId == null || isBetterUnit(longUnitId, formerLongUnitId)) {
                    genderResults.put(possibleGender, longUnitId);
                }
            }
            if (!unitCases.isEmpty()) {
                unitPatternToCases.clear();
                for (String count : pluralInfo.getKeywords()) {
                    for (String unitCase : unitCases) {
                        String grammarAttributes = GrammarInfo.getGrammaticalInfoAttributes(grammarInfo, UnitPathType.unit, count, null, unitCase);
                        String unitPattern = cldrFile.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + longUnitId + "\"]/unitPattern" + grammarAttributes);
                        if (unitPattern == null) {
                            continue;
                        }
                        unitPattern = unitPattern.replace("\u00A0", "").trim();
                        unitPatternToCases.put(unitPattern, Pair.of(unitCase, count));
                    }
                }
                // For case, we should do something fancier, but for now we pick the units with the largest number of distinct forms.
                int caseFormCount = unitPatternToCases.keySet().size();
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
                    bestUnitPatternToCases = TreeMultimap.create(unitPatternToCases);
                }
            }
        }
        // Fill the case cache with the most distinctive forms.
        Map<String, String> caseCache = getBestCasePatterns(bestUnitPatternToCases);

        // Make the gender cache be translated units instead of unit IDs
        Count count = pluralInfo.getKeywords().contains("one") ? Count.one : Count.other;
        Map<String,String> result2 = Maps.newHashMap();

        for (Entry<String, String> entry : genderResults.entrySet()) {
            String shortUnitId = entry.getValue();
            String unitPattern = cldrFile.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + shortUnitId + "\"]/unitPattern[@count=\"" + count + "\"]");
            unitPattern = unitPattern.replace("{0}", "").replace("\u00A0", "").trim();
            result2.put(entry.getKey(), unitPattern);
        }
        // it doesn't matter if we reset this due to multiple threads
        Map<String, String> genderCache = ImmutableMap.copyOf(result2);
        CaseAndGenderSamples result = new CaseAndGenderSamples(caseCache, genderCache);

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
            if (sample == null) {
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
        String result;
        PluralRules rules = pluralType == PluralType.cardinal ? pluralInfo : ordinalInfo;
        FixedDecimalSamples samples = rules.getDecimalSamples(code, SampleType.INTEGER);
        if (samples == null) {
            samples = rules.getDecimalSamples(code, SampleType.DECIMAL);
        }
        if (samples == null) {
            return null;
        }
        return samples.getSamples().iterator().next().start.toString();
    }
}