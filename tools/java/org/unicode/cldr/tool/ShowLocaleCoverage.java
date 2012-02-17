package org.unicode.cldr.tool;

import java.util.Map;
import java.util.Set;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;

public class ShowLocaleCoverage {
    private static TestInfo testInfo = TestInfo.getInstance();
    public static void main(String[] args) {
        LanguageTagParser ltp = new LanguageTagParser();
        Map<String, String> likely = testInfo.getSupplementalDataInfo().getLikelySubtags();
        Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();

        //Map<String,Counter<Level>> counts = new HashMap();
        System.out.print("Script\tNative\tEnglish\tCode\tCode*");
        for (Level level : Level.values()) {
            System.out.print("\t≤" + level);
        }
        System.out.println();
        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            if (defaultContents.contains(locale)) {
                continue;
            }
            String region = ltp.set(locale).getRegion();
            if (!region.isEmpty()) continue; // skip regions
            String language = ltp.getLanguage();
            String script = ltp.getScript();
            if (script.isEmpty()) {
                String likelySubtags = likely.get(language);
                if (likelySubtags != null) {
                    script = ltp.set(likelySubtags).getScript();
                }
            }

            CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance(locale);
            Counter<Level> counter = new Counter();
            final CLDRFile file = testInfo.getCldrFactory().make(locale, false);
            for (String path: file) {
                if (path.contains("unconfirmed") || path.contains("provisional")) {
                    continue;
                }
                Level level = coverageLevel2.getLevel(path);
                counter.add(level, 1);
            }
            System.out.print(
                    script
                    + "\t" + file.getName(language) 
                    + "\t" + testInfo.getEnglish().getName(language) 
                    + "\t" + language
                    + "\t" + locale);
            int sum = 0;
            for (Level level : Level.values()) {
                sum += counter.get(level);
                System.out.print("\t" + sum);
            }
            System.out.println();
        }
    }
}
