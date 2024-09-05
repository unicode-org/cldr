package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckDisplayCollisions;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

public class TestCheckDisplayCollisions extends TestFmwkPlus {
    private static final String ukRegion =
            "//ldml/localeDisplayNames/territories/territory[@type=\"GB\"]";
    private static final String englandSubdivision =
            "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"gbeng\"]";

    private static final String scorpioEmoji =
            "//ldml/annotations/annotation[@cp=\"♏\"][@type=\"tts\"]";
    private static final String scorpionEmoji =
            "//ldml/annotations/annotation[@cp=\"🦂\"][@type=\"tts\"]";

    private static final String japanRegion =
            "//ldml/localeDisplayNames/territories/territory[@type=\"JP\"]";
    private static final String japanMap =
            "//ldml/annotations/annotation[@cp=\"🗾\"][@type=\"tts\"]";

    private static final String milli =
            "//ldml/units/unitLength[@type=\"short\"]/compoundUnit[@type=\"10p-3\"]/unitPrefixPattern";
    private static final String mega =
            "//ldml/units/unitLength[@type=\"short\"]/compoundUnit[@type=\"10p6\"]/unitPrefixPattern";

    private static final String deciLong =
            "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"10p-1\"]/unitPrefixPattern";
    private static final String deciNarrow =
            "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"10p-1\"]/unitPrefixPattern";
    private static final String deciShort =
            "//ldml/units/unitLength[@type=\"short\"]/compoundUnit[@type=\"10p-1\"]/unitPrefixPattern";

    public static void main(String[] args) {
        new TestCheckDisplayCollisions().run(args);
    }

    public void testInheritance() {
        XMLSource rootSource = new SimpleXMLSource("root");
        CLDRFile root = new CLDRFile(rootSource);

        XMLSource enSource = new SimpleXMLSource("en");
        CLDRFile en = new CLDRFile(enSource);

        XMLSource frSource = new SimpleXMLSource("fr");
        frSource.putValueAtPath(scorpionEmoji, "scorpion");
        frSource.putValueAtPath(scorpioEmoji, "scorpion zodiac");
        frSource.putValueAtPath(englandSubdivision, "Angleterre");
        frSource.putValueAtPath(ukRegion, "Royaume-Uni");
        frSource.putValueAtDPath(japanRegion, "Japon");
        frSource.putValueAtDPath(japanMap, "carte du Japon");
        frSource.putValueAtDPath(milli, "m{0}");
        frSource.putValueAtDPath(mega, "M{0}");

        frSource.putValueAtDPath(deciLong, "d{0}");
        frSource.putValueAtDPath(deciNarrow, "d{0}");
        frSource.putValueAtDPath(deciShort, "d{0}");

        CLDRFile fr = new CLDRFile(frSource);

        XMLSource frCaSource = new SimpleXMLSource("fr_CA");
        frCaSource.putValueAtPath(scorpioEmoji, "scorpion");
        frCaSource.putValueAtPath(ukRegion, "Angleterre");
        frCaSource.putValueAtDPath(japanMap, "Japon");
        CLDRFile frCA = new CLDRFile(frCaSource);

        TestFactory factory = new TestFactory();
        factory.addFile(root);
        factory.addFile(en);
        factory.addFile(fr);
        factory.addFile(frCA);

        CheckDisplayCollisions cdc = new CheckDisplayCollisions(factory);
        cdc.setEnglishFile(CLDRConfig.getInstance().getEnglish());

        CLDRFile frResolved = factory.make("fr", true);
        checkFile(cdc, fr, frResolved);

        CLDRFile frCaResolved = factory.make("fr_CA", true);
        checkFile(cdc, frCA, frCaResolved, scorpioEmoji, ukRegion);
    }

    public void testUnitPatternCollisions() {
        final String unitPattern1 =
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot\"]/unitPattern[@count=\"one\"]";
        /** different count as # 1. MUST NOT COLLIDE WITH #1 */
        final String unitPattern2 =
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot\"]/unitPattern[@count=\"other\"]";
        /** different unit as # 1. MUST COLLIDE WITH #1 */
        final String unitPattern3 =
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-point\"]/unitPattern[@count=\"one\"]";
        /**
         * #4 and #5 must NOT collide; case="nominative" and case="accusative" when paths are
         * otherwise identical and have the same value
         */
        final String unitPattern4 =
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"volume-dram\"]/unitPattern[@count=\"other\"][@case=\"nominative\"]";
        final String unitPattern5 =
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"volume-dram\"]/unitPattern[@count=\"other\"][@case=\"accusative\"]";

        mustCollide(false, unitPattern1, unitPattern2);
        mustCollide(true, unitPattern1, unitPattern3);
        mustCollide(false, unitPattern4, unitPattern5);
    }

    private void mustCollide(
            boolean expectCollisionErrors, String unitPatternA, String unitPatternB) {
        final String testLocale = "pt";
        final String duplicatedValue = "{0}pontos";

        final XMLSource rootSource = new SimpleXMLSource("root");
        final CLDRFile root = new CLDRFile(rootSource);

        final XMLSource enSource = new SimpleXMLSource("en");
        final CLDRFile en = new CLDRFile(enSource);

        final XMLSource source = new SimpleXMLSource(testLocale);

        source.putValueAtPath(unitPatternA, duplicatedValue);
        source.putValueAtPath(unitPatternB, duplicatedValue);

        CLDRFile file = new CLDRFile(source);

        TestFactory factory = new TestFactory();
        factory.addFile(root);
        factory.addFile(en);
        factory.addFile(file);

        CheckDisplayCollisions cdc = new CheckDisplayCollisions(factory);
        cdc.setEnglishFile(CLDRConfig.getInstance().getEnglish());

        CLDRFile ptResolved = factory.make(testLocale, true);
        if (expectCollisionErrors) {
            checkFile(cdc, file, ptResolved, unitPatternA, unitPatternB);
        } else {
            checkFile(cdc, file, ptResolved);
        }
    }

    private void checkFile(
            CheckDisplayCollisions cdc,
            CLDRFile cldrFile,
            CLDRFile cldrFileResolved,
            String... expectedErrors) {
        List<CheckStatus> possibleErrors = new ArrayList<>();
        Options options = new Options();
        cdc.setCldrFileToCheck(cldrFile, options, possibleErrors);
        if (!possibleErrors.isEmpty()) {
            errln("init: " + possibleErrors);
            possibleErrors.clear();
        }
        Map<String, List<CheckStatus>> found = new HashMap<>();
        for (String path : cldrFileResolved) {
            String value = cldrFileResolved.getStringValue(path);
            // System.out.println(path + "\t" + value);
            if (path.equals(deciLong)) {
                int debug = 0;
            }
            cdc.check(path, path, value, options, possibleErrors);
            if (!possibleErrors.isEmpty()) {
                found.put(path, ImmutableList.copyOf(possibleErrors));
                possibleErrors.clear();
                logln("Found error " + cldrFile.getLocaleID() + ":\t" + path + ", " + value);
            } else {
                logln("No error " + cldrFile.getLocaleID() + ":\t" + path + ", " + value);
            }
        }
        Set<String> expected = new TreeSet<>(Arrays.asList(expectedErrors));
        for (Entry<String, List<CheckStatus>> entry : found.entrySet()) {
            String path = entry.getKey();
            if (expected.contains(path)) {
                expected.remove(path);
            } else {
                errln(
                        cldrFile.getLocaleID()
                                + " unexpected error: "
                                + path
                                + " : "
                                + entry.getValue());
            }
            checkUnknown(path, entry);
        }
        assertEquals(
                cldrFile.getLocaleID() + " expected to be errors: ",
                Collections.emptySet(),
                expected);
    }

    /**
     * Report an error if the CheckStatus parameters contain the word "Unknown", which can be a
     * symptom of errors such as removal of required "count" attribute. "Unknown" may not always be
     * an error, but for the data used in this test we don't expect it.
     *
     * @param path
     * @param entry
     */
    private void checkUnknown(String path, Entry<String, List<CheckStatus>> entry) {
        CheckStatus cs = entry.getValue().get(0);
        String s = cs.getParameters()[0].toString();
        if (s.contains("Unknown")) {
            errln("Found Unknown in : " + path + ":\n" + s);
        }
    }

    public void TestDotPixel14031() {
        Map<String, String> pathValuePairs =
                ImmutableMap.of(
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot\"]/displayName",
                                "Punkt",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-pixel\"]/displayName",
                                "Punkt",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-pixel-per-centimeter\"]/displayName",
                                "Punkt pro Zentimeter",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot-per-centimeter\"]/displayName",
                                "Punkt pro Zentimeter");
        TestFactory factory = makeFakeCldrFile("de", pathValuePairs);
        checkDisplayCollisions("de", pathValuePairs, factory);
    }

    public void checkDayPeriodCollisions() {
        // CLDR-16933 (uk, ...)
        // Let midnight match a non-fixed period that starts at, ends at, or contains midnight (both
        // versions);
        // Let noon match a non-fixed period that starts at, ends at, or contains noon (or just
        // before noon);
        Map<String, String> ukPathValuePairs =
                ImmutableMap.of(
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"midnight\"]",
                                "ночі",
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"night1\"]",
                                "ночі",
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"noon\"]",
                                "дня",
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"afternoon1\"]",
                                "дня");
        TestFactory ukFactory = makeFakeCldrFile("uk", ukPathValuePairs);
        checkDisplayCollisions("uk", ukPathValuePairs, ukFactory);

        // CLDR-17132 (fr, ...)
        // Let night1 have the same name as morning1/am if night1 starts at 00:00
        Map<String, String> frPathValuePairs =
                ImmutableMap.of(
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"morning1\"]",
                                "matin",
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"night1\"]",
                                "matin");
        TestFactory frFactory = makeFakeCldrFile("fr", frPathValuePairs);
        checkDisplayCollisions("fr", frPathValuePairs, frFactory);

        // CLDR-17139 (fil, ...)
        // Let night1 have the same name as evening1 if night1 ends at 24:00
        Map<String, String> filPathValuePairs =
                ImmutableMap.of(
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"evening1\"]",
                                "ng gabi",
                        "ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"night1\"]",
                                "ng gabi");
        TestFactory filFactory = makeFakeCldrFile("fil", filPathValuePairs);
        checkDisplayCollisions("fil", filPathValuePairs, filFactory);
    }

    public void checkCurrencySymbolCollisions() {
        // CLDR-17792
        Map<String, String> pt_PTPathValuePairs =
                ImmutableMap.of(
                        "ldml/numbers/currencies/currency[@type=\"EUR\"]/symbol", "€",
                        "ldml/numbers/currencies/currency[@type=\"GBP\"]/symbol", "£",
                        "ldml/numbers/currencies/currency[@type=\"PTE\"]/symbol", "\u200B",
                        "ldml/numbers/currencies/currency[@type=\"PTE\"]/decimal", "$",
                        "ldml/numbers/currencies/currency[@type=\"USD\"]/symbol", "US$");
        TestFactory pt_PTFactory = makeFakeCldrFile("pt_PT", pt_PTPathValuePairs);
        checkDisplayCollisions("pt_PT", pt_PTPathValuePairs, pt_PTFactory);
    }

    public void checkDisplayCollisions(
            String locale, Map<String, String> pathValuePairs, TestFactory factory) {
        CheckDisplayCollisions cdc = new CheckDisplayCollisions(factory);
        cdc.setEnglishFile(CLDRConfig.getInstance().getEnglish());

        List<CheckStatus> possibleErrors = new ArrayList<>();
        cdc.setCldrFileToCheck(
                factory.make(locale, true), new Options(ImmutableMap.of()), possibleErrors);
        for (Entry<String, String> entry : pathValuePairs.entrySet()) {
            cdc.check(
                    entry.getKey(),
                    entry.getKey(),
                    entry.getValue(),
                    new Options(ImmutableMap.of()),
                    possibleErrors);
            assertEquals(entry.toString(), Collections.emptyList(), possibleErrors);
        }
    }

    public TestFactory makeFakeCldrFile(String locale, Map<String, String> pathValuePairs) {
        TestFactory factory = new TestFactory();
        XMLSource rootSource = new SimpleXMLSource("root");
        factory.addFile(new CLDRFile(rootSource));

        XMLSource localeSource = new SimpleXMLSource(locale);
        for (Entry<String, String> entry : pathValuePairs.entrySet()) {
            localeSource.putValueAtPath(entry.getKey(), entry.getValue());
        }
        factory.addFile(new CLDRFile(localeSource));
        return factory;
    }

    public void TestDurationPersonVariants() {
        Map<String, String> pathValuePairs =
                ImmutableMap.of(
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day-person\"]/unitPattern[@count=\"other\"]",
                                "Punkt",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day\"]/unitPattern[@count=\"other\"]",
                                "Punkt",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-pixel\"]/displayName",
                                "Punkt",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-pixel-per-centimeter\"]/displayName",
                                "Punkt pro Zentimeter",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot-per-centimeter\"]/displayName",
                                "Punkt pro Zentimeter");
        TestFactory factory = makeFakeCldrFile("de", pathValuePairs);
        checkDisplayCollisions("de", pathValuePairs, factory);
    }
}
