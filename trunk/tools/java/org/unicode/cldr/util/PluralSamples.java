package org.unicode.cldr.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

public class PluralSamples {
    private static final Map<String, PluralSamples> cache = new ConcurrentHashMap<String, PluralSamples>();
    private final Map<Count, Double>[] samples = new Map[4]; // we do 1, 2, 3, and 4 decimals

    public PluralSamples(String locale) {
        SupplementalDataInfo info = SupplementalDataInfo.getInstance();
        PluralInfo pluralInfo = info.getPlurals(locale);
        int total = pluralInfo.getCounts().size();

        samples[0] = Collections.unmodifiableMap(getValuesForDigits(pluralInfo, total, 0, 9));
        samples[1] = Collections.unmodifiableMap(getValuesForDigits(pluralInfo, total, 10, 99));
        samples[2] = Collections.unmodifiableMap(getValuesForDigits(pluralInfo, total, 100, 999));
        samples[3] = Collections.unmodifiableMap(getValuesForDigits(pluralInfo, total, 1000, 9999));
    }

    private Map<Count, Double> getValuesForDigits(PluralInfo pluralInfo, int total, int start, int end) {
        Map<Count, Double> set = new EnumMap<Count, Double>(Count.class);
        // Cycle through digits
        boolean favorPositive = start == 0;
        if (favorPositive) {
            ++start;
        }
        for (int item = start; item < end; ++item) {
            double dItem = (double) item;
            Count count = pluralInfo.getCount(dItem);
            Double old = set.get(count);
            if (old == null) {
                set.put(count, dItem);
                if (set.size() == total) {
                    return set;
                }
            }
        }
        if (favorPositive) { // try zero
            double dItem = 0.0d;
            Count count = pluralInfo.getCount(dItem);
            Double old = set.get(count);
            if (old == null) {
                set.put(count, dItem);
                if (set.size() == total) {
                    return set;
                }
            }
        }
        // need fractions
        start *= 10;
        end *= 10;
        for (int item = start; item < end; ++item) {
            double dItem = item / 10d;
            Count count = pluralInfo.getCount(dItem);
            Double old = set.get(count);
            if (old == null) {
                set.put(count, dItem);
                if (set.size() == total) {
                    return set;
                }
            }
        }
        return set;
    }

    /**
     * Get a set of samples for the locale.
     * @param locale
     * @return
     */
    public static PluralSamples getInstance(String locale) {
        PluralSamples result = cache.get(locale);
        if (result == null) {
            result = new PluralSamples(locale);
            cache.put(locale, result); // don't care if done twice
        }
        return result;
    }

    /**
     * Return a mapping from plural category to doubles
     * @param i
     * @return
     */
    public Map<PluralInfo.Count, Double> getSamples(int digits) {
        return samples[digits - 1];
    }
}
