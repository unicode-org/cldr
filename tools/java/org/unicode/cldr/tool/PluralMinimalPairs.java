package org.unicode.cldr.tool;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.PluralRules.PluralType;

public class PluralMinimalPairs {
    private Map<PluralType, Map<PluralInfo.Count, String>> typeToCountToSample = new EnumMap<>(PluralType.class);

    private PluralMinimalPairs() {
    }

    private static Map<String, PluralMinimalPairs> cache = new ConcurrentHashMap<>();
    public static PluralMinimalPairs EMPTY = new PluralMinimalPairs().freeze();

    // TODO should use builder pattern
    public static PluralMinimalPairs getInstance(String ulocale) {
        PluralMinimalPairs samplePatterns = cache.get(ulocale);
        if (samplePatterns != null) {
            return samplePatterns;
        }
        // don't care if we put it in twice, better than locking
        Factory factory = CLDRConfig.getInstance().getFullCldrFactory();
        try {
            samplePatterns = new PluralMinimalPairs();
            CLDRFile cldrFile = factory.make(ulocale, true);
            for (Iterator<String> it = cldrFile.iterator("//ldml/numbers/minimalPairs/"); it.hasNext();) {
                String path = it.next();
                String foundLocale = cldrFile.getSourceLocaleID(path, null);
                if (foundLocale.equals("root")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String sample = cldrFile.getStringValue(path);
                String element = parts.getElement(-1);
                PluralType type = "pluralMinimalPairs".equals(element) ? PluralType.CARDINAL
                    : "ordinalMinimalPairs".equals(element) ? PluralType.ORDINAL
                        : null;
                PluralInfo.Count category = PluralInfo.Count.valueOf(
                    parts.getAttributeValue(-1, type == PluralType.CARDINAL ? "count" : "ordinal"));
                if (category == null || type == null) {
                    throw new IllegalArgumentException("Bad plural info");
                }
                samplePatterns.put(ulocale, type, category, sample);
            }
            if (samplePatterns.typeToCountToSample.isEmpty()) {
                samplePatterns = EMPTY;
            } else {
                samplePatterns.freeze();
            }
        } catch (Exception e) {
            samplePatterns = EMPTY;
        }
        cache.put(ulocale, samplePatterns);
        return samplePatterns;
    }

    public void put(String locale, PluralType type, Count count, String sample) {
        Map<Count, String> countToSample = typeToCountToSample.get(type);
        if (countToSample == null) {
            typeToCountToSample.put(type, countToSample = new EnumMap<>(Count.class));
        }
        countToSample.put(count, sample);
    }

    public String get(PluralType type, Count count) {
        Map<Count, String> countToSample = typeToCountToSample.get(type);
        return countToSample == null ? null : countToSample.get(count);
    }

    public Set<Count> getCounts(PluralType type) {
        Map<Count, String> countToSample = typeToCountToSample.get(type);
        return countToSample == null ? null : typeToCountToSample.get(type).keySet();
    }

    public PluralMinimalPairs freeze() {
        typeToCountToSample = CldrUtility.protectCollection(typeToCountToSample);
        return this;
    }

    @Override
    public String toString() {
        return typeToCountToSample.toString();
    }

    public boolean isEmpty(PluralType type) {
        return !typeToCountToSample.containsKey(type);
    }
}