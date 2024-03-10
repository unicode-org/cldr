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
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

public class GenerateLikelyTestData {
    private static final String DUMMY_SCRIPT = "Egyp";
    private static final String DUMMY_REGION = "AQ";
    static CLDRConfig config = CLDRConfig.getInstance();
    static Map<String, String> data = config.getSupplementalDataInfo().getLikelySubtags();
    static LikelySubtags likely = new LikelySubtags();
    private static final Validity VALIDITY = Validity.getInstance();
    static Set<String> okRegions = VALIDITY.getStatusToCodes(LstrType.region).get(Status.regular);

    public static void main(String[] args) {
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
                            + "#                      If Add Likely Subtags fails, then “FAIL”.\n"
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
                if (testRaw.startsWith("qaa")) {
                    int debug = 0;
                }
                final CLDRLocale source = CLDRLocale.getInstance(testRaw);
                final String test = source.toLanguageTag();

                // if the maxLang is empty, we have no data for the language
                String lang = source.getLanguage();
                String maxLang = likely.maximize(lang);
                if (maxLang == null || maxLang.isEmpty()) {
                    showLine(pw, test, "FAIL", "FAIL", "FAIL");
                    continue;
                }

                final String maximize = likely.maximize(test);
                final String max = CLDRLocale.getInstance(maximize).toLanguageTag();
                final CLDRLocale minFavorScriptLocale =
                        CLDRLocale.getInstance(likely.setFavorRegion(false).minimize(test));
                final String favorScript = minFavorScriptLocale.toLanguageTag();
                final CLDRLocale minFavorRegionLocale =
                        CLDRLocale.getInstance(likely.setFavorRegion(true).minimize(test));
                final String minFavorRegion = minFavorRegionLocale.toLanguageTag();
                showLine(pw, test, max, favorScript, minFavorRegion);
            }
        }
    }

    public static void check(String test0) {
        String check = likely.maximize(test0);
        System.out.println(test0 + " → " + check);
    }

    // test data

    private static Set<String> ALLOWED_WITH_MACROREGION =
            Set.of("ar_001", "en_001", "en_150", "es_419"); // only intentional CLDR locales

    public static Set<String> getTestCases(Map<String, String> data) {
        CalculatedCoverageLevels coverage = CalculatedCoverageLevels.getInstance();
        Set<String> skipping = new TreeSet<>();
        TreeSet<String> testCases = new TreeSet<>();
        // for CLDR locales, add combinations
        // collect together the scripts&regions for each language. Will filter later
        Multimap<String, String> combinations = TreeMultimap.create();
        for (String localeString : config.getCldrFactory().getAvailable()) {
            Level effective = coverage.getEffectiveCoverageLevel(localeString);
            if (effective == null || effective.compareTo(Level.BASIC) < 0) {
                continue;
            }
            if (localeString.equals("root")) {
                continue;
            }
            CLDRLocale locale = CLDRLocale.getInstance(localeString);
            String lang = locale.getLanguage();
            CLDRLocale max = CLDRLocale.getInstance(likely.maximize(localeString));
            if (!okRegions.contains(max.getCountry())
                    && !ALLOWED_WITH_MACROREGION.contains(localeString)) {
                skipping.add(localeString);
                continue;
            }
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
        testCases.add("qaa");
        testCases.add("qaa_Cyrl");
        testCases.add("qaa_CH");
        testCases.add("qaa_Cyrl_CH");

        System.out.println("Skipping " + skipping);
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
