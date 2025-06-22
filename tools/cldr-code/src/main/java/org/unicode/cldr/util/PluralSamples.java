package org.unicode.cldr.util;

import com.ibm.icu.text.PluralRules;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

public class PluralSamples {
    private static SupplementalDataInfo info = SupplementalDataInfo.getInstance();

    private static final Map<String, PluralRules> localeToLocaleWithRules =
            new ConcurrentHashMap<>();
    private static final Map<PluralRules, PluralSamples> rulesToSamples = new ConcurrentHashMap<>();

    private static final int SAMPLE_SIZE = 6;
    private final Map<Count, Double>[] samples =
            new Map[SAMPLE_SIZE]; // we do up to fairly large powers of 10 — needed for Compact

    // decimal, with locales that don't have 'thousand'

    private PluralSamples(PluralRules rules) {
        List<Count> keywords =
                rules.getKeywords().stream().map(Count::valueOf).collect(Collectors.toList());

        samples[0] = Collections.unmodifiableMap(getValuesForDigits(rules, keywords, 0, 9));
        samples[1] = Collections.unmodifiableMap(getValuesForDigits(rules, keywords, 10, 99));
        samples[2] = Collections.unmodifiableMap(getValuesForDigits(rules, keywords, 100, 999));
        samples[3] = Collections.unmodifiableMap(getValuesForDigits(rules, keywords, 1000, 9999));
        samples[4] = Collections.unmodifiableMap(getValuesForDigits(rules, keywords, 10000, 99999));
        samples[5] =
                Collections.unmodifiableMap(getValuesForDigits(rules, keywords, 100000, 999999));
    }

    private Map<Count, Double> getValuesForDigits(
            PluralRules rules, List<Count> keywords, int start, int end) {
        int total = keywords.size();
        Map<Count, Double> mapCountToDouble = new EnumMap<>(Count.class);

        // Cycle through digits
        boolean favorPositive = start == 0;
        if (favorPositive) {
            ++start;
        }
        for (int item = start; item <= end; ++item) {
            double dItem = item;
            Count count = Count.valueOf(rules.select(dItem));
            Double old = mapCountToDouble.get(count);
            if (old == null) {
                mapCountToDouble.put(count, dItem);
                if (mapCountToDouble.size() == total) {
                    return mapCountToDouble;
                }
            }
        }
        if (favorPositive) { // try zero
            double dItem = 0.0d;
            Count count = Count.valueOf(rules.select(dItem));
            Double old = mapCountToDouble.get(count);
            if (old == null) {
                mapCountToDouble.put(count, dItem);
                if (mapCountToDouble.size() == total) {
                    return mapCountToDouble;
                }
            }
        }
        // need fractions
        start *= 10;
        end *= 10;
        for (int item = start; item < end; ++item) {
            double dItem = item / 10d;
            Count count = Count.valueOf(rules.select(dItem));
            Double old = mapCountToDouble.get(count);
            if (old == null) {
                mapCountToDouble.put(count, dItem);
                if (mapCountToDouble.size() == total) {
                    return mapCountToDouble;
                }
            }
        }
        return mapCountToDouble;
    }

    /** Get a set of samples for the locale. */
    public static PluralSamples getInstance(String locale) {

        // We cache by plural rules, so that we share the info among locale with the same rules
        // info.getPlurals uses inheritance to get the first locale in the chain with rules

        PluralRules rules = getCachedRules(locale);
        return rulesToSamples.computeIfAbsent(rules, x -> new PluralSamples(x));
    }

    public static PluralRules getCachedRules(String locale) {
        return localeToLocaleWithRules.computeIfAbsent(
                locale, x -> info.getPlurals(x).getPluralRules());
    }

    /** Return a mapping from plural category to doubles */
    public Map<PluralInfo.Count, Double> getSamples(int digits) {
        if (digits > 0 && digits <= SAMPLE_SIZE) {
            return samples[digits - 1];
        }
        return null;
    }
}
