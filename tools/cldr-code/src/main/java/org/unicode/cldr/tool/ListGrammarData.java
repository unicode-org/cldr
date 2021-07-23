package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.BestMinimalPairSamples;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class ListGrammarData {
    static CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final CLDRFile ENGLISH = CONFIG.getEnglish();

    public static void main(String[] args) {
        Factory factory = CONFIG.getCldrFactory();
        PathHeader.Factory phf = PathHeader.getFactory();
        LinkedHashSet<String> errors = new LinkedHashSet<>();
        Multimap<String,String> lines = LinkedHashMultimap.create();
        LikelySubtags likelySubtags = new LikelySubtags();

        for (String localeRaw : factory.getAvailableLanguages()) {
            String locale = likelySubtags.minimize(localeRaw);
            if (!localeRaw.equals(locale)) {
                // System.out.println("Skipping " + locale);
                continue;
            }
            CLDRFile cldrFile = factory.make(locale, true);
            BestMinimalPairSamples bestMinimalPairSamples = new BestMinimalPairSamples(cldrFile);
            Map<PathHeader, String> pathHeaderToValue = new TreeMap<>();
            Multimap<String, String> sectionPageHeaderToCodes = TreeMultimap.create();
            for (String path : cldrFile.fullIterable()) {
                if (!path.startsWith("//ldml/numbers/minimalPairs")) {
                    continue;
                }
                PathHeader ph = phf.fromPath(path);
                String value = cldrFile.getStringValue(path);
                pathHeaderToValue.put(ph, value);
                sectionPageHeaderToCodes.put(sectionPageHeader(ph), ph.getCode());
            }
            if (pathHeaderToValue.isEmpty()) {
                continue;
            }
            // System.out.println(locale);
            final String names = locale + "\t" + ENGLISH.getName(locale);
            for (Entry<PathHeader, String> entry : pathHeaderToValue.entrySet()) {

                final PathHeader ph = entry.getKey();
                Collection<String> codes = sectionPageHeaderToCodes.get(sectionPageHeader(ph));
                if (codes.size() == 1) {
                    errors.add("*" + names + "\t" + ph + "\t" + "singlular!");
                    continue;
                }
                final String minimalPattern = entry.getValue();
                String sample = getBestValue(bestMinimalPairSamples, ph.getHeader(), ph.getCode());
                lines.put(ph.getHeader(), names + "\t" + ph + "\t" + minimalPattern + "\t" + sample);
            }
        }
        for (String key : new TreeSet<>(lines.keySet())) {
            String lastLocale = "";
            for (String line : lines.get(key)) {
                String locale = line.substring(0,line.indexOf('\t'));
                if (!locale.equals(lastLocale)) {
                    lastLocale = locale;
                    System.out.println();
                }
                System.out.println(line);
            }
        }

        if (false) for (String error : errors) {
            System.out.println(error);
        }
    }

    private static String getBestValue(BestMinimalPairSamples bestMinalPairSamples, String header, String code) {
        String result = null;
        switch(header) {
        case "Case":
            result = bestMinalPairSamples.getBestUnitWithCase(code);
            break;
        case "Gender":
            result = bestMinalPairSamples.getBestUnitWithGender(code);
            break;
        case "Ordinal":
            result = bestMinalPairSamples.getPluralOrOrdinalSample(PluralType.ordinal, code);
            break;
        case "Plural":
            result = bestMinalPairSamples.getPluralOrOrdinalSample(PluralType.cardinal, code);
            break;
        }
        return result == null ? "X" : result;
    }

    public static String sectionPageHeader(PathHeader ph) {
        return ph.getSectionId() + "|" + ph.getPageId() + "|" + ph.getHeader();
    }

}
