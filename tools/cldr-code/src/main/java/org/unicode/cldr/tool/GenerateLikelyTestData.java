package org.unicode.cldr.tool;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CalculatedCoverageLevels;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.TempPrintWriter;

public class GenerateLikelyTestData {
    private static final String DUMMY_SCRIPT = "Egyp";
    private static final String DUMMY_REGION = "AQ";
    static CLDRConfig config = CLDRConfig.getInstance();
    static Map<String, String> data = config.getSupplementalDataInfo().getLikelySubtags();
    static LikelySubtags likely = new LikelySubtags();

    public static void main(String[] args) {
        String test0 = "und-Adlm-BF";
        likely.maximize(test0);

        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(
                        CLDRPaths.TEST_DATA + "localeIdentifiers", "likelySubtags.txt")) {

            pw.println(
                    "# Test data for Likely Subtags\n"
                            + CldrUtility.getCopyrightString("#  ")
                            + "\n"
                            + "#\n"
                            + "# Test data for https://www.unicode.org/reports/tr35/tr35.html#Likely_Subtags\n"
                            + "#\n"
                            + "# Format:\n");
            showLine(pw, "# Source", "AddLikely", "RemoveFavorScript", "RemoveFavorRegion");
            pw.println(
                    "#   Source: a locale to which the following operations are applied.\n"
                            + "#   AddLikely: the result of the Add Likely Subtags.\n"
                            + "#   RemoveFavorScript: Remove Likely Subtags, when the script is favored.\n"
                            + "#                      Only included when different than AddLikely.\n"
                            + "#   RemoveFavorRegion: Remove Likely Subtags, when the region is favored.\n"
                            + "#                      Only included when different than RemoveFavorScript.\n"
                            + "#\n"
                            + "# Generation: GenerateLikelyTestData.java\n");

            // generate alternates
            // for now, simple case
            Set<String> testCases = getTestCases(data);

            for (String testRaw : testCases) {
                final String test = CLDRLocale.getInstance(testRaw).toLanguageTag();
                final String max = CLDRLocale.getInstance(likely.maximize(test)).toLanguageTag();
                if (test.equals(max)) {
                    continue;
                }
                final String minScript =
                        CLDRLocale.getInstance(likely.setFavorRegion(false).minimize(test))
                                .toLanguageTag();
                final String minRegion =
                        CLDRLocale.getInstance(likely.setFavorRegion(true).minimize(test))
                                .toLanguageTag();
                showLine(pw, test, max, minScript, minRegion);
            }
        }
    }

    // test data

    public static Set<String> getTestCases(Map<String, String> data) {
        CalculatedCoverageLevels coverage = CalculatedCoverageLevels.getInstance();
        TreeSet<String> testCases = new TreeSet<>();
        // for CLDR locales, add combinations
        // collect together the scripts&regions for each language. Will filter later
        Multimap<String, String> combinations = TreeMultimap.create();
        for (String localeString : config.getCldrFactory().getAvailable()) {
            Level effective = coverage.getEffectiveCoverageLevel(localeString);
            if (effective == null || effective.compareTo(Level.BASIC) < 0) {
                continue;
            }
            CLDRLocale locale = CLDRLocale.getInstance(localeString);
            String lang = locale.getLanguage();
            CLDRLocale max = CLDRLocale.getInstance(likely.maximize(localeString));
            combinations.put(lang, max.getScript());
            combinations.put(lang, max.getCountry());
            combinations.put(lang, DUMMY_REGION); // check odd conditions
            combinations.put(lang, DUMMY_SCRIPT); // check odd conditions
            combinations.put(lang, ""); // check odd conditions
        }
        Set<String> undCombinations = new TreeSet<>();
        for (Entry<String, Collection<String>> entry : combinations.asMap().entrySet()) {
            undCombinations.addAll(entry.getValue());
        }
        combinations.putAll("und", undCombinations);

        LanguageTagParser ltp = new LanguageTagParser();
        for (Entry<String, Collection<String>> entry : combinations.asMap().entrySet()) {
            final String lang = entry.getKey();
            System.out.println(lang);
            Set<String> items = new TreeSet<>(entry.getValue());
            Set<String> scripts = new LinkedHashSet<>();
            Set<String> regions = new LinkedHashSet<>();
            for (String scriptOrRegion : items) {
                ltp.set(lang); // clears script, region
                if (scriptOrRegion.length() == 4) {
                    ltp.setScript(scriptOrRegion);
                    scripts.add(scriptOrRegion);
                } else {
                    ltp.setRegion(scriptOrRegion);
                    if (!scriptOrRegion.isBlank()) {
                        regions.add(scriptOrRegion);
                    }
                }
                testCases.add(CLDRLocale.getInstance(ltp.toString()).toLanguageTag());
            }
            scripts.remove(DUMMY_REGION);
            scripts.remove(DUMMY_SCRIPT);

            if (!lang.equals("und")) { // record script/region combinations
                ltp.set("und");
                for (String script : scripts) {
                    ltp.setScript(script);
                    for (String region : regions) {
                        ltp.setRegion(region);
                        testCases.add(CLDRLocale.getInstance(ltp.toString()).toLanguageTag());
                    }
                }
            }
        }
        testCases.remove("und"); // TC accepted change to remove this, so don't test for it.
        return testCases;
    }

    public static void showLine(
            TempPrintWriter tempWriter,
            String test,
            final String max,
            final String minScript,
            final String minRegion) {
        tempWriter.println(
                test //
                        + " ;\t"
                        + (max.equals(test) ? "" : max) //
                        + " ;\t"
                        + (minScript.equals(max) ? "" : minScript) // script favored
                        + " ;\t"
                        + (minRegion.equals(minScript) ? "" : minRegion) // region favored
                );
    }
}
