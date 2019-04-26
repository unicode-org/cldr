package org.unicode.cldr.draft;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Relation;

public class ExtractCountItems {
    CLDRConfig testInfo = ToolConfig.getToolInstance();
    Factory factory = testInfo.getCldrFactory();

    static class SampleData {
        EnumMap<Count, String> countToString = new EnumMap<Count, String>(Count.class);
        Relation<String, Count> stringToCount = Relation.of(new HashMap<String, Set<Count>>(), HashSet.class);
        String basePath;

        public SampleData(String basePath) {
            this.basePath = basePath;
        }

        public int put(Count count, String value) {
            countToString.put(count, value);
            stringToCount.put(value, count);
            return stringToCount.size();
        }

        public String toString() {
            return countToString.toString();
        }

        public Set<Entry<Count, String>> entrySet() {
            return countToString.entrySet();
        }
    }

    Map<String, SampleData> samples = new LinkedHashMap<String, SampleData>();

    public static void main(String[] args) {

        new ExtractCountItems().gatherData();
    }

    void gatherData() {
        Set<String> singletonLanguages = new LinkedHashSet<String>();
        Map<String, Map<String, SampleData>> defectiveLocales = new LinkedHashMap<String, Map<String, SampleData>>();

        for (String locale : factory.getAvailableLanguages()) {
            Map<String, Level> locale_status = StandardCodes.make().getLocaleToLevel(Organization.google);

            if (locale_status == null) continue;
            Level level = locale_status.get(locale);
            if (level == null) continue;
            if (level.compareTo(Level.BASIC) <= 0) continue;

            Set<String> pluralLocales = testInfo.getSupplementalDataInfo().getPluralLocales();
            if (!pluralLocales.contains(locale)) {
                continue;
            }
            PluralInfo pluralInfo = testInfo.getSupplementalDataInfo().getPlurals(locale);
            Set<String> keywords = pluralInfo.getCanonicalKeywords();
            int keywordCount = keywords.size();
            if (keywordCount == 1) {
                singletonLanguages.add(locale);
                continue;
            }

            CLDRFile cldr = factory.make(locale, true);
            Map<String, SampleData> data = new LinkedHashMap<String, SampleData>();
            SampleData sampleData = getSamples(cldr, keywordCount, "//ldml/units/unit", data);
            if (sampleData == null) {
                // try currencies
                sampleData = getSamples(cldr, keywordCount, "//ldml/numbers/currencies/currency", data);
            }
            if (sampleData == null) {
                defectiveLocales.put(locale, data);
                continue;
            }
            System.out.println("#" + locale + "\t" + sampleData.basePath);

            for (Entry<Count, String> entry : sampleData.entrySet()) {
                System.out.println(locale + "\t" + entry.getKey() + "\t<" + entry.getValue() + ">");
            }
        }
        System.out.println("");
        System.out.println("\tLanguages with one plural category: " + singletonLanguages);
        System.out.println("\tLanguages with only defective samples: ");
        for (Entry<String, Map<String, SampleData>> entry : defectiveLocales.entrySet()) {
            String locale = entry.getKey();
            System.out.println(locale);
            PluralInfo pluralInfo = testInfo.getSupplementalDataInfo().getPlurals(locale);
            Set<String> keywords = pluralInfo.getCanonicalKeywords();
            EnumSet<Count> realKeywords = EnumSet.noneOf(Count.class);
            for (String s : keywords) {
                realKeywords.add(Count.valueOf(s));
            }
            Set<Pair<Count, Count>> missingPairs = new HashSet<Pair<Count, Count>>();
            for (Count i : realKeywords) {
                for (Count j : realKeywords) {
                    if (i.compareTo(j) >= 0) {
                        continue;
                    }
                    missingPairs.add(new Pair<Count, Count>(i, j));
                }
            }
            showMinimalPairs(missingPairs, entry.getValue());
        }
    }

    private void showMinimalPairs(Set<Pair<Count, Count>> missingPairs, Map<String, SampleData> pathToSamples) {
        for (Entry<String, SampleData> entry : pathToSamples.entrySet()) {
            SampleData samples = entry.getValue();
            for (Iterator<Pair<Count, Count>> it = missingPairs.iterator(); it.hasNext();) {
                Pair<Count, Count> missing = it.next();
                Count first = missing.getFirst();
                Count second = missing.getSecond();
                String s1 = samples.countToString.get(first);
                String s2 = samples.countToString.get(second);
                if (s1 != null && s2 != null && !s1.equals(s2)) {
                    // have match, display and remove.
                    System.out.println(missing + "\t" + samples.basePath + "\t<" + s1 + ">\t<" + s2 + ">");
                    it.remove();
                }
            }
        }
        System.out.println("Missing minimal pairs: " + missingPairs);
    }

    SampleData getSamples(CLDRFile cldr, int keywordCount, String prefix, Map<String, SampleData> data) {
        for (String path : With.in(cldr.iterator(prefix, cldr.getComparator()))) {
            if (!path.contains("@count")) {
                continue;
            }
            if (path.contains("//ldml/numbers/decimalFormats") || path.contains("//ldml/numbers/currencyFormats")
                || path.contains("@alt=\"short\"")) {
                continue;
            }
            String value = cldr.getStringValue(path).toLowerCase(Locale.ENGLISH);
            // get the path without the count = basepath
            XPathParts parts = XPathParts.getTestInstance(path);
            Count count = PluralInfo.Count.valueOf(parts.getAttributeValue(-1, "count"));
            parts.setAttribute(-1, "count", null);
            String basePath = parts.toString();
            SampleData sampleData = data.get(basePath);
            if (sampleData == null) {
                data.put(basePath, sampleData = new SampleData(basePath));
            }
            int items = sampleData.put(count, value);
            if (items == keywordCount) {
                samples.put(cldr.getLocaleID(), sampleData);
                return sampleData;
            }
        }
        return null;
    }
}
