package org.unicode.cldr.tool;

import com.ibm.icu.text.PluralRules.PluralType;
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

public class PluralMinimalPairs {
    private Map<PluralType, Map<PluralInfo.Count, String>> typeToCountToSample =
            new EnumMap<>(PluralType.class);

    private PluralMinimalPairs() {}

    private static final Map<String, PluralMinimalPairs> cache = new ConcurrentHashMap<>();
    public static final PluralMinimalPairs EMPTY = new PluralMinimalPairs().freeze();

    public static PluralMinimalPairs getInstance(String ulocale) {
        return cache.computeIfAbsent(ulocale, x -> getUncachedSamples(factory.make(ulocale, true)));
    }

    private static Factory factory = CLDRConfig.getInstance().getCldrFactory();

    public static PluralMinimalPairs getUncachedSamples(CLDRFile cldrFile) {
        PluralMinimalPairs samplePatterns;
        // don't care if we put it in twice, better than locking
        try {
            samplePatterns = new PluralMinimalPairs();
            for (Iterator<String> it = cldrFile.iterator("//ldml/numbers/minimalPairs/");
                    it.hasNext(); ) {
                String path = it.next();
                String foundLocale = cldrFile.getSourceLocaleID(path, null);
                if (foundLocale.equals("root")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String sample = cldrFile.getStringValue(path);
                String element = parts.getElement(-1);
                PluralType type;
                switch (element) {
                    case "pluralMinimalPairs":
                        type = PluralType.CARDINAL;
                        break;
                    case "ordinalMinimalPairs":
                        type = PluralType.ORDINAL;
                        break;
                    default:
                        continue; // skip grammar, case
                }
                PluralInfo.Count category =
                        PluralInfo.Count.valueOf(
                                parts.getAttributeValue(
                                        -1, type == PluralType.CARDINAL ? "count" : "ordinal"));
                if (category == null || type == null) {
                    throw new IllegalArgumentException("Bad plural info");
                }
                samplePatterns.put(cldrFile.getLocaleID(), type, category, sample);
            }
            if (samplePatterns.typeToCountToSample.isEmpty()) {
                samplePatterns = EMPTY;
            } else {
                samplePatterns.freeze();
            }
        } catch (Exception e) {
            samplePatterns = EMPTY;
        }
        return samplePatterns;
    }

    private void put(String locale, PluralType type, Count count, String sample) {
        Map<Count, String> countToSample = typeToCountToSample.get(type);
        if (countToSample == null) {
            typeToCountToSample.put(type, countToSample = new EnumMap<>(Count.class));
        }
        countToSample.put(count, sample);
    }

    public String getSample(PluralType type, Count count) {
        Map<Count, String> countToSample = typeToCountToSample.get(type);
        return countToSample == null ? null : countToSample.get(count);
    }

    public Set<Count> getCounts(PluralType type) {
        Map<Count, String> countToSample = typeToCountToSample.get(type);
        return countToSample == null ? null : typeToCountToSample.get(type).keySet();
    }

    private PluralMinimalPairs freeze() {
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
