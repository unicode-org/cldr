package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.UnitLength;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitPathType;
import org.unicode.cldr.util.With;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;

public class TestExampleGenerator extends TestFmwk {
    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    CLDRConfig info = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestExampleGenerator().run(args);
    }

    public void testCurrency() {
        String[][] tests = {
            {
                "fr",
                "one",
                "〖❬1,23 ❭value-one〗〖❬0,00 ❭value-one〗",
            "〖❬1,23❭_❬dollar des États-Unis❭〗〖❬1,23❭_❬euro❭〗〖❬0,00❭_❬dollar des États-Unis❭〗〖❬0,00❭_❬euro❭〗" },
            {
                "fr",
                "other",
                "〖❬2,34 ❭value-other〗〖❬3,45 ❭value-other〗",
            "〖❬2,34❭_❬dollars des États-Unis❭〗〖❬2,34❭_❬euros❭〗〖❬3,45❭_❬dollars des États-Unis❭〗〖❬3,45❭_❬euros❭〗" },
            { "en", "one", "〖❬1 ❭Bermudan dollar〗",
            "〖❬1❭ ❬US dollar❭〗〖❬1❭ ❬euro❭〗" },
            { "en", "other",
                "〖❬1.23 ❭Bermudan dollars〗〖❬0.00 ❭Bermudan dollars〗",
            "〖❬1.23❭ ❬US dollars❭〗〖❬1.23❭ ❬euros❭〗〖❬0.00❭ ❬US dollars❭〗〖❬0.00❭ ❬euros❭〗" }, };
        String sampleCurrencyPatternPrefix = "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/unitPattern[@count=\"";
        String sampleCurrencyPrefix = "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"";
        String sampleTemplateSuffix = "\"]";

        for (String[] row : tests) {
            ExampleGenerator exampleGenerator = getExampleGenerator(row[0]);
            String value = "value-" + row[1];

            String path = sampleCurrencyPrefix + row[1] + sampleTemplateSuffix;
            String result = ExampleGenerator
                .simplify(exampleGenerator.getExampleHtml(path, value), false);
            assertEquals(row[0] + "-" + row[1] + "-BMD", row[2], result);

            value = "{0}_{1}";
            path = sampleCurrencyPatternPrefix + row[1] + sampleTemplateSuffix;
            result = ExampleGenerator
                .simplify(exampleGenerator.getExampleHtml(path, value), false);
            assertEquals(row[0] + "-" + row[1] + "-pat", row[3], result);
        }
    }

    /**
     * Only add to this if the example should NEVER appear.
     * <br>WARNING - do not disable the test by putting in too broad a match. Make sure the paths are reasonably granular.
     */
    static final Set<String> DELIBERATE_EXCLUDED_EXAMPLES = ImmutableSet.of(
        "//ldml/layout/orientation/characterOrder",
        "//ldml/layout/orientation/lineOrder",
        "//ldml/characters/moreInformation",
        "//ldml/numbers/symbols[@numberSystem=\"([^\"]*+)\"]/infinity",
        "//ldml/numbers/symbols[@numberSystem=\"([^\"]*+)\"]/list",
        "//ldml/numbers/symbols[@numberSystem=\"([^\"]*+)\"]/nan",
        "//ldml/numbers/currencies/currency[@type=\"([^\"]*+)\"]/displayName",
        "//ldml/localeDisplayNames/measurementSystemNames/measurementSystemName[@type=\"([^\"]*+)\"]",
        // old format
        "//ldml/numbers/symbols/infinity",
        "//ldml/numbers/symbols/list",
        "//ldml/numbers/symbols/nan",
        "//ldml/posix/messages/nostr",
        "//ldml/posix/messages/yesstr",
        "//ldml/contextTransforms/contextTransformUsage[@type=\"([^\"]*+)\"]/contextTransform[@type=\"([^\"]*+)\"]",
        "//ldml/characters/exemplarCharacters",
        "//ldml/characters/exemplarCharacters[@type=\"([^\"]*+)\"]",
        "//ldml/characters/parseLenients.*",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/months/monthContext[@type=\"([^\"]*+)\"]/monthWidth[@type=\"([^\"]*+)\"]/month[@type=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/days/dayContext[@type=\"([^\"]*+)\"]/dayWidth[@type=\"([^\"]*+)\"]/day[@type=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/quarters/quarterContext[@type=\"([^\"]*+)\"]/quarterWidth[@type=\"([^\"]*+)\"]/quarter[@type=\"([^\"]*+)\"]",
        "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/displayName",
        "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/relative[@type=\"([^\"]*+)\"]",
        "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/relativeTime[@type=\"([^\"]*+)\"]/relativeTimePattern[@count=\"([^\"]*+)\"]",
        "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/relativePeriod",
        "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/displayName[@alt=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/cyclicNameSets/cyclicNameSet[@type=\"([^\"]*+)\"]/cyclicNameContext[@type=\"([^\"]*+)\"]/cyclicNameWidth[@type=\"([^\"]*+)\"]/cyclicName[@type=\"([^\"]*+)\"]",

        "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"([^\"]*+)\"]",
        "//ldml/numbers/minimalPairs/ordinalMinimalPairs[@ordinal=\"([^\"]*+)\"]",
        "//ldml/characters/parseLenients[@scope=\"([^\"]*+)\"][@level=\"([^\"]*+)\"]/parseLenient[@sample=\"([^\"]*+)\"]"
        );
    // Only add to above if the example should NEVER appear.

    /**
     * Add to this if the example SHOULD appear, but we don't have it yet.
     * <br>TODO Add later
     */
    static final Set<String> TEMPORARY_EXCLUDED_EXAMPLES = ImmutableSet.of(

        "//ldml/numbers/currencyFormats/currencySpacing/beforeCurrency/currencyMatch",
        "//ldml/numbers/currencyFormats/currencySpacing/beforeCurrency/surroundingMatch",
        "//ldml/numbers/currencyFormats/currencySpacing/beforeCurrency/insertBetween",
        "//ldml/numbers/currencyFormats/currencySpacing/afterCurrency/currencyMatch",
        "//ldml/numbers/currencyFormats/currencySpacing/afterCurrency/surroundingMatch",
        "//ldml/numbers/currencyFormats/currencySpacing/afterCurrency/insertBetween",
        "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/beforeCurrency/currencyMatch",
        "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/beforeCurrency/surroundingMatch",
        "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/beforeCurrency/insertBetween",
        "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/afterCurrency/currencyMatch",
        "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/afterCurrency/surroundingMatch",
        "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/afterCurrency/insertBetween",

        "//ldml/localeDisplayNames/variants/variant[@type=\"([^\"]*+)\"]",
        "//ldml/localeDisplayNames/keys/key[@type=\"([^\"]*+)\"]",
        "//ldml/localeDisplayNames/types/type[@key=\"([^\"]*+)\"][@type=\"([^\"]*+)\"]",
        "//ldml/localeDisplayNames/types/type[@key=\"([^\"]*+)\"][@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]",

        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraNames/era[@type=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraAbbr/era[@type=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraNarrow/era[@type=\"([^\"]*+)\"]",

        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateFormats/dateFormatLength[@type=\"([^\"]*+)\"]/dateFormat[@type=\"([^\"]*+)\"]/datetimeSkeleton",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/timeFormats/timeFormatLength[@type=\"([^\"]*+)\"]/timeFormat[@type=\"([^\"]*+)\"]/datetimeSkeleton",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateFormats/dateFormatLength[@type=\"([^\"]*+)\"]/datetimeSkeleton",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/timeFormats/timeFormatLength[@type=\"([^\"]*+)\"]/datetimeSkeleton",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/appendItems/appendItem[@request=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/intervalFormats/intervalFormatFallback",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"([^\"]*+)\"]/greatestDifference[@id=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraNames/era[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraAbbr/era[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraNarrow/era[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/months/monthContext[@type=\"([^\"]*+)\"]/monthWidth[@type=\"([^\"]*+)\"]/month[@type=\"([^\"]*+)\"][@yeartype=\"([^\"]*+)\"]",
        "//ldml/dates/timeZoneNames/gmtZeroFormat",

        "//ldml/numbers/minimumGroupingDigits",
        "//ldml/numbers/symbols/timeSeparator",
        "//ldml/numbers/symbols[@numberSystem=\"([^\"]*+)\"]/timeSeparator",

        "//ldml/units/unitLength[@type=\"([^\"]*+)\"]/unit[@type=\"([^\"]*+)\"]/displayName",
        "//ldml/units/unitLength[@type=\"([^\"]*+)\"]/unit[@type=\"([^\"]*+)\"]/perUnitPattern",
        "//ldml/units/unitLength[@type=\"([^\"]*+)\"]/coordinateUnit/coordinateUnitPattern[@type=\"([^\"]*+)\"]",
        "//ldml/units/unitLength[@type=\"([^\"]*+)\"]/coordinateUnit/displayName",

        "//ldml/characterLabels/characterLabelPattern[@type=\"([^\"]*+)\"]",
        "//ldml/characterLabels/characterLabelPattern[@type=\"([^\"]*+)\"][@count=\"([^\"]*+)\"]",
        "//ldml/characterLabels/characterLabel[@type=\"([^\"]*+)\"]",
        "//ldml/typographicNames/axisName[@type=\"([^\"]*+)\"]",
        "//ldml/typographicNames/styleName[@type=\"([^\"]*+)\"][@subtype=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]",
        "//ldml/typographicNames/styleName[@type=\"([^\"]*+)\"][@subtype=\"([^\"]*+)\"]",
        "//ldml/typographicNames/featureName[@type=\"([^\"]*+)\"]",

        "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"([^\"]*+)\"]",

        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/long/standard" // Error: (TestExampleGenerator.java:245) No background:   <Coordinated Universal Time>    〖Coordinated Universal Time〗
        );
    // Add to above if the example SHOULD appear, but we don't have it yet. TODO Add later


    /**
     * Only add to this if the background should NEVER appear.
     * <br>The background is used when the element is used as part of another format.
     * <br>WARNING - do not disable the test by putting in too broad a match. Make sure the paths are reasonably granular.
     */
    static final Set<String> DELIBERATE_OK_TO_MISS_BACKGROUND = ImmutableSet.of(
        "//ldml/numbers/defaultNumberingSystem",
        "//ldml/numbers/otherNumberingSystems/native",
        // TODO fix formatting
        "//ldml/characters/exemplarCharacters",
        "//ldml/characters/exemplarCharacters[@type=\"([^\"]*+)\"]",
        // TODO Add background
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateFormats/dateFormatLength[@type=\"([^\"]*+)\"]/dateFormat[@type=\"([^\"]*+)\"]/pattern[@type=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/timeFormats/timeFormatLength[@type=\"([^\"]*+)\"]/timeFormat[@type=\"([^\"]*+)\"]/pattern[@type=\"([^\"]*+)\"]",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"([^\"]*+)\"]",
        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/exemplarCity",
        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/exemplarCity[@alt=\"([^\"]*+)\"]",
        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/long/daylight",
        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/short/generic",
        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/short/standard",
        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/short/daylight",
        "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/long/generic",
        "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/long/standard",
        "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/long/daylight",
        "//ldml/units/durationUnit[@type=\"([^\"]*+)\"]/durationUnitPattern");
    // Only add to above if the background should NEVER appear.


    /**
     * Add to this if the background SHOULD appear, but we don't have them yet.
     * <br> The background is used when the element is used as part of another format.
     * <br> TODO Add later
     */
    static final Set<String> TEMPORARY_OK_TO_MISS_BACKGROUND = ImmutableSet.of(
        "//ldml/numbers/defaultNumberingSystem",
        "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"([^\"]*+)\"][@count=\"([^\"]*+)\"]",
        "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/long/standard",
        "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/short/generic",
        "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/short/standard",
        "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/short/daylight");
    // Add to above if the background SHOULD appear, but we don't have them yet. TODO Add later

    public void TestAllPaths() {
        ExampleGenerator exampleGenerator = getExampleGenerator("en");
        PathStarrer ps = new PathStarrer();
        Set<String> seen = new HashSet<>();
        CLDRFile cldrFile = exampleGenerator.getCldrFile();
        TreeSet<String> target = new TreeSet<>(cldrFile.getComparator());
        cldrFile.fullIterable().forEach(target::add);
        for (String path : target) {
            String plainStarred = ps.set(path);
            String value = cldrFile.getStringValue(path);
            if (value == null || path.endsWith("/alias")
                || path.startsWith("//ldml/identity")
                || DELIBERATE_EXCLUDED_EXAMPLES.contains(plainStarred)) {
                continue;
            }
            if (TEMPORARY_EXCLUDED_EXAMPLES.contains(plainStarred)) {
                if (logKnownIssue(
                    "Cldrbug:6342",
                    "Need an example for each path used in context: " + plainStarred)) {
                    continue;
                }
                continue;
            }
            String example = exampleGenerator.getExampleHtml(path, value);
            String javaEscapedStarred = "\""
                + plainStarred.replace("\"", "\\\"") + "\",";
            if (example == null) {
                if (!seen.contains(javaEscapedStarred)) {
                    errln("No example:\t<" + value + ">\t" + javaEscapedStarred);
                }
            } else {
                String simplified = ExampleGenerator.simplify(example, false);

                if (simplified.contains("null")) {
                    if (true || !seen.contains(javaEscapedStarred)) {
                        // debug
                        exampleGenerator.getExampleHtml(path, value);
                        ExampleGenerator.simplify(example, false);

                        errln("'null' in message:\t<" + value + ">\t"
                            + simplified + "\t" + javaEscapedStarred);
                        // String example2 =
                        // exampleGenerator.getExampleHtml(path, value); // for
                        // debugging
                    }
                } else if (!simplified.startsWith("〖")) {
                    if (!seen.contains(javaEscapedStarred)) {
                        errln("Funny HTML:\t<" + value + ">\t" + simplified
                            + "\t" + javaEscapedStarred);
                    }
                } else if (!simplified.contains("❬")
                    && !DELIBERATE_OK_TO_MISS_BACKGROUND.contains(plainStarred)) {
                    if (!seen.contains(javaEscapedStarred)) {

                        if (TEMPORARY_OK_TO_MISS_BACKGROUND.contains(plainStarred)
                            && logKnownIssue(
                                "Cldrbug:6342",
                                "Make sure that background appears: " + simplified + "; " + plainStarred)) {
                            continue;
                        }

                        errln("No background:\t<" + value + ">\t" + simplified
                            + "\t" + javaEscapedStarred);
                    }
                }
            }
            seen.add(javaEscapedStarred);
        }
    }

    public void TestUnits() {
        ExampleGenerator exampleGenerator = getExampleGenerator("en");
        checkValue("Duration hm", "〖5:37〗", exampleGenerator,
            "//ldml/units/durationUnit[@type=\"hm\"]/durationUnitPattern");
        checkValue(
            "Length m",
            "〖❬1❭ meter〗",
            exampleGenerator,
            "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"one\"]");
        checkValue(
            "Length m",
            "〖❬1.5❭ meters〗",
            exampleGenerator,
            "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]");
        checkValue(
            "Length m",
            "〖❬1.5❭ m〗",
            exampleGenerator,
            "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]");
        checkValue(
            "Length m",
            "〖❬1.5❭m〗",
            exampleGenerator,
            "//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]");

        // The following are to ensure that we properly generate an example when we have a non-winning value
        checkValue(
            "Length m",
            "〖❬1.5❭ badmeter〗",
            exampleGenerator,
            "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]",
            "{0} badmeter");

        ExampleGenerator exampleGeneratorDe = getExampleGenerator("de");
        checkValue(
            "Length m",
            "〖❬1,5❭ badmeter〗〖❬Anstatt 1,5❭ badmeter❬ …❭〗",
            exampleGeneratorDe,
            "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"][@case=\"genitive\"]",
            "{0} badmeter");
    }

    private void checkValue(String message, String expected,
        ExampleGenerator exampleGenerator, String path) {
        checkValue(message, expected, exampleGenerator, path, null);
    }

    private void checkValue(String message, String expected,
        ExampleGenerator exampleGenerator, String path, String value) {
        value = value != null ? value : exampleGenerator.getCldrFile().getStringValue(path);
        String actual = exampleGenerator.getExampleHtml(path, value);
        assertEquals(message, expected,
            ExampleGenerator.simplify(actual, false));
    }

    public void TestCompoundUnit() {
        String[][] tests = {
            { "per", "LONG", "one", "〖❬1 meter❭ per ❬second❭〗" },
            { "per", "SHORT", "one", "〖❬1 m❭/❬sec❭〗" },
            { "per", "NARROW", "one", "〖❬1m❭/❬s❭〗" },
            { "per", "LONG", "other", "〖❬1.5 meters❭ per ❬second❭〗" },
            { "per", "SHORT", "other", "〖❬1.5 m❭/❬sec❭〗" },
            { "per", "NARROW", "other", "〖❬1.5m❭/❬s❭〗" },
            { "times", "LONG", "one", "〖❬1 newton❭-❬meter❭〗" },
            { "times", "SHORT", "one", "〖❬1 N❭⋅❬m❭〗" },
            { "times", "NARROW", "one", "〖❬1N❭⋅❬m❭〗" },
            { "times", "LONG", "other", "〖❬1.5 newton❭-❬meters❭〗" },
            { "times", "SHORT", "other", "〖❬1.5 N❭⋅❬m❭〗" },
            { "times", "NARROW", "other", "〖❬1.5N❭⋅❬m❭〗" },
        };
        checkCompoundUnits("en", tests);
        // reenable these after Arabic has meter translated
        // String[][] tests2 = {
        // {"LONG", "few", "〖❬1 meter❭ per ❬second❭〗"},
        // };
        // checkCompoundUnits("ar", tests2);
    }

    private void checkCompoundUnits(String locale, String[][] tests) {
        ExampleGenerator exampleGenerator = getExampleGenerator(locale);
        for (String[] test : tests) {
            String actual = exampleGenerator.handleCompoundUnit(
                UnitLength.valueOf(test[1]),
                test[0],
                Count.valueOf(test[2]));
            assertEquals("CompoundUnit", test[3],
                ExampleGenerator.simplify(actual, true));
        }
    }

    public void TestTranslationPaths() {
        for (String locale : Arrays.asList("en", "el")) {
            CLDRFile cldrFile = CLDRConfig.getInstance().getCldrFactory().make(locale, true);
            ExampleGenerator exampleGenerator = getExampleGenerator(locale);

            for (UnitPathType pathType : UnitPathType.values()) {
                for (String width : Arrays.asList("long", "short", "narrow")) {
                    if (pathType == UnitPathType.gender && !width.equals("long")) {
                        continue;
                    }
                    for (String unit : pathType.sampleShortUnitType) {
                        String path = pathType.getTranslationPath(cldrFile, width, unit, "one", "nominative", null);
                        String value = cldrFile.getStringValue(path);
                        if (value != null) {
                            String example = exampleGenerator.getExampleHtml(path, value);
                            if (assertNotNull(locale + "/" + path, example)) {
                                String simplified = ExampleGenerator.simplify(example, false);
                                warnln(locale + ", " + width + ", " + pathType.toString() + " ==>" + simplified);
                            } else {
                                // for debugging
                                example = exampleGenerator.getExampleHtml(path, value);
                            }
                        }
                    }
                }
            }
        }
    }

    public void TestCompoundUnit2() {
        String[][] tests = {
            { "de", "LONG", "other", "Quadrat{0}", "〖❬1,5 ❭Quadrat❬meter❭〗" },

            { "en", "SHORT", "one", "z{0}", "〖❬1 ❭z❬m❭〗" },
            { "en", "LONG", "other", "zetta{0}", "〖❬1.5 ❭zetta❬meters❭〗" },

            { "en", "SHORT", "one", "{0}²", "〖❬1 m❭²〗" },
            { "en", "LONG", "other", "square {0}", "〖❬1.5 ❭square ❬meters❭〗" },

            { "de", "SHORT", "one", "z{0}", "〖❬1 ❭z❬m❭〗" },
            { "de", "LONG", "other", "Zetta{0}", "〖❬1,5 ❭Zetta❬meter❭〗" },

            { "de", "SHORT", "one", "{0}²", "〖❬1 m❭²〗" },
            { "de", "LONG", "other", "Quadrat{0}", "〖❬1,5 ❭Quadrat❬meter❭〗" },
        };
        for (String[] test : tests) {

            ExampleGenerator exampleGenerator = getExampleGenerator(test[0]);

            String actual = exampleGenerator.handleCompoundUnit1(
                UnitLength.valueOf(test[1]),
                Count.valueOf(test[2]),
                test[3]);
            assertEquals("CompoundUnit", test[4],
                ExampleGenerator.simplify(actual, true));
        }
    }

    public void TestCompoundUnit3() {
        final Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        String[][] tests = {
            // locale, path, value, expected-example
            { "en", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                "LOCALE", "〖square ❬meters❭〗" }, //
            { "en", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                    "LOCALE", "〖❬1 ❭square ❬meter❭〗" }, //
            { "en", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                        "LOCALE", "〖❬1.5 ❭square ❬meters❭〗" }, //

            { "en", "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                            "LOCALE", "〖❬m❭²〗" },
            { "en", "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                                "LOCALE", "〖❬1m❭²〗" },
            { "en", "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                                    "LOCALE", "〖❬1.5m❭²〗" },

            // warning, french patterns has U+00A0 in them
            { "fr", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                                        "Square {0}", "〖Square ❬mètres❭〗" },
            { "fr", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                                            "square {0}", "〖❬1,5 ❭square ❬mètre❭〗" },
            { "fr", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                                                "squares {0}", "〖❬3,5 ❭squares ❬mètres❭〗" },

            { "fr", "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                                                    "LOCALE", "〖❬m❭²〗" },
            { "fr", "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                                                        "LOCALE", "〖❬1,5m❭²〗" },
            { "fr", "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                                                            "LOCALE", "〖❬3,5m❭²〗" },
        };

        int lineCount = 0;
        for (String[] test : tests) {

            final String localeID = test[0];
            final String xpath = test[1];
            String value = test[2];
            String expected = test[3];

            ExampleGenerator exampleGenerator = getExampleGenerator(localeID);

            if (value.equals("LOCALE")) {
                value = cldrFactory.make(localeID, true).getStringValue(xpath);
            }
            String actual = exampleGenerator.getExampleHtml(xpath, value);
            assertEquals(++lineCount + ") "
                + localeID
                + ", CompoundUnit3", expected,
                ExampleGenerator.simplify(actual, false));
        }

    }

    HashMap<String, ExampleGenerator> ExampleGeneratorCache = new HashMap<>();

    private ExampleGenerator getExampleGenerator(String locale) {
        ExampleGenerator result = ExampleGeneratorCache.get(locale);
        if (result == null) {
            final CLDRFile nativeCldrFile = info.getCLDRFile(locale,
                true);
            result = new ExampleGenerator(nativeCldrFile, info.getEnglish(),
                CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
            ExampleGeneratorCache.put(locale, result);
        }
        return result;
    }

    public void TestEllipsis() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        String[][] tests = { { "initial", "〖…❬iappone❭〗" },
            { "medial", "〖❬Svizzer❭…❬iappone❭〗" },
            { "final", "〖❬Svizzer❭…〗" },
            { "word-initial", "〖… ❬Giappone❭〗" },
            { "word-medial", "〖❬Svizzera❭ … ❬Giappone❭〗" },
            { "word-final", "〖❬Svizzera❭ …〗" }, };
        for (String[] pair : tests) {
            checkValue(exampleGenerator, "//ldml/characters/ellipsis[@type=\""
                + pair[0] + "\"]", pair[1]);
        }
    }

    private void checkValue(ExampleGenerator exampleGenerator, String path,
        String expected) {
        String value = exampleGenerator.getCldrFile().getStringValue(path);
        String result = ExampleGenerator.simplify(
            exampleGenerator.getExampleHtml(path, value), false);
        assertEquals("Ellipsis", expected, result);
    }

    public static String simplify(String exampleHtml) {
        return ExampleGenerator.simplify(exampleHtml, false);
    }

    public void TestClip() {
        assertEquals("Clipping", "bc", ExampleGenerator.clip("abc", 1, 0));
        assertEquals("Clipping", "ab", ExampleGenerator.clip("abc", 0, 1));
        assertEquals("Clipping", "b\u0308c\u0308",
            ExampleGenerator.clip("a\u0308b\u0308c\u0308", 1, 0));
        assertEquals("Clipping", "a\u0308b\u0308",
            ExampleGenerator.clip("a\u0308b\u0308c\u0308", 0, 1));
    }

    public void TestPaths() {
        showCldrFile(info.getEnglish());
        showCldrFile(info.getCLDRFile("fr", true));
    }

    public void TestMiscPatterns() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        checkValue(
            "At least",
            "〖≥❬99❭〗",
            exampleGenerator,
            "//ldml/numbers/miscPatterns[@numberSystem=\"latn\"]/pattern[@type=\"atLeast\"]");
        checkValue("Range", "〖❬99❭-❬144❭〗", exampleGenerator,
            "//ldml/numbers/miscPatterns[@numberSystem=\"latn\"]/pattern[@type=\"range\"]");
        // String actual = exampleGenerator.getExampleHtml(
        // "//ldml/numbers/miscPatterns[@type=\"arab\"]/pattern[@type=\"atLeast\"]",
        // "at least {0}", Zoomed.IN);
        // assertEquals("Invalid format",
        // "<div class='cldr_example'>at least 99</div>", actual);
    }

    public void TestPluralSamples() {
        ExampleGenerator exampleGenerator = getExampleGenerator("sv");
        String path = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"length-centimeter\"]/unitPattern[@count=\"one\"]";
        checkValue("Number should be one", "〖❬1❭ cm〗", exampleGenerator, path);
    }

    public void TestLocaleDisplayPatterns() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        String actual = exampleGenerator.getExampleHtml(
            "//ldml/localeDisplayNames/localeDisplayPattern/localePattern",
            "{0} [{1}]");
        assertEquals(
            "localePattern example faulty",
            "<div class='cldr_example'><span class='cldr_substituted'>uzbeco</span> [<span class='cldr_substituted'>Afghanistan</span>]</div>"
                + "<div class='cldr_example'><span class='cldr_substituted'>uzbeco</span> [<span class='cldr_substituted'>arabo, Afghanistan</span>]</div>"
                + "<div class='cldr_example'><span class='cldr_substituted'>uzbeco</span> [<span class='cldr_substituted'>arabo, Afghanistan, Cifre indo-arabe, Fuso orario: Ora Etiopia</span>]</div>",
                actual);
        actual = exampleGenerator
            .getExampleHtml(
                "//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator",
                "{0}. {1}");
        assertEquals(
            "localeSeparator example faulty",
            "<div class='cldr_example'><span class='cldr_substituted'>uzbeco (arabo</span>. <span class='cldr_substituted'>Afghanistan)</span></div>"
                + "<div class='cldr_example'><span class='cldr_substituted'>uzbeco (arabo</span>. <span class='cldr_substituted'>Afghanistan</span>. <span class='cldr_substituted'>Cifre indo-arabe</span>. <span class='cldr_substituted'>Fuso orario: Ora Etiopia)</span></div>",
                actual);
    }

    public void TestCurrencyFormats() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        String actual = simplify(exampleGenerator
            .getExampleHtml(
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                "¤ #0.00"));
        assertEquals("Currency format example faulty",
            "〖€ ❬1295,00❭〗〖-€ ❬1295,00❭〗", actual);
    }

    public void TestSymbols() {
        CLDRFile english = info.getEnglish();
        ExampleGenerator exampleGenerator = new ExampleGenerator(english,
            english, CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        String actual = exampleGenerator
            .getExampleHtml(
                "//ldml/numbers/symbols[@numberSystem=\"latn\"]/superscriptingExponent",
                "x");

        assertEquals(
            "superscriptingExponent faulty",
            "<div class='cldr_example'><span class='cldr_substituted'>1.23456789</span>x10<span class='cldr_substituted'><sup>5</sup></span></div>",
            actual);
    }

    public void TestFallbackFormat() {
        ExampleGenerator exampleGenerator = new ExampleGenerator(
            info.getEnglish(), info.getEnglish(),
            CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        String actual = exampleGenerator.getExampleHtml(
            "//ldml/dates/timeZoneNames/fallbackFormat", "{1} [{0}]");
        assertEquals("fallbackFormat faulty", "〖❬Central Time❭ [❬Cancun❭]〗",
            ExampleGenerator.simplify(actual, false));
    }

    public void Test4897() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        for (String xpath : With.in(exampleGenerator.getCldrFile().iterator(
            "//ldml/dates/timeZoneNames",
            exampleGenerator.getCldrFile().getComparator()))) {
            String value = exampleGenerator.getCldrFile().getStringValue(xpath);
            String actual = exampleGenerator.getExampleHtml(xpath, value);
            if (actual == null) {
                if (!xpath.contains("singleCountries")
                    && !xpath.contains("gmtZeroFormat")) {
                    errln("Null value for " + value + "\t" + xpath);
                    // for debugging
                    exampleGenerator.getExampleHtml(xpath, value);
                }
            } else {
                logln(actual + "\t" + value + "\t" + xpath);
            }
        }
    }

    public void Test4528() {
        String[][] testPairs = {
            {
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"other\"]",
            "〖❬1,23 ❭dollari delle Bermuda〗〖❬0,00 ❭dollari delle Bermuda〗" },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/unitPattern[@count=\"other\"]",
            "〖❬1,23❭ ❬dollari statunitensi❭〗〖❬1,23❭ ❬euro❭〗〖❬0,00❭ ❬dollari statunitensi❭〗〖❬0,00❭ ❬euro❭〗" },
            { "//ldml/numbers/currencies/currency[@type=\"BMD\"]/symbol",
            "〖❬123.456,79 ❭BMD〗" }, };

        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        for (String[] testPair : testPairs) {
            String xpath = testPair[0];
            String expected = testPair[1];
            String value = exampleGenerator.getCldrFile().getStringValue(xpath);
            String actual = simplify(exampleGenerator.getExampleHtml(xpath, value));
            assertEquals("specifics", expected, actual);
        }
    }

    public void Test4607() {
        String[][] testPairs = {
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]",
            "<div class='cldr_example'><span class='cldr_substituted'>1</span> thousand</div>" },
            {
                "//ldml/numbers/percentFormats[@numberSystem=\"latn\"]/percentFormatLength/percentFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                "<div class='cldr_example'><span class='cldr_substituted'>5</span>%</div>"
                    + "<div class='cldr_example'><span class='cldr_substituted'>12,345</span>,<span class='cldr_substituted'>679</span>%</div>"
                    + "<div class='cldr_example'>-<span class='cldr_substituted'>12,345</span>,<span class='cldr_substituted'>679</span>%</div>" } };
        final CLDRFile nativeCldrFile = info.getEnglish();
        ExampleGenerator exampleGenerator = new ExampleGenerator(
            info.getEnglish(), info.getEnglish(),
            CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        for (String[] testPair : testPairs) {
            String xpath = testPair[0];
            String expected = testPair[1];
            String value = nativeCldrFile.getStringValue(xpath);
            String actual = exampleGenerator.getExampleHtml(xpath, value);
            assertEquals("specifics", expected, actual);
        }
    }

    private void showCldrFile(final CLDRFile cldrFile) {
        ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile,
            info.getEnglish(), CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        checkPathValue(
            exampleGenerator,
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"full\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@draft=\"unconfirmed\"]",
            "EEEE d MMMMl y'x'G", null);

        for (String xpath : cldrFile.fullIterable()) {
            if (xpath.endsWith("/alias")) {
                continue;
            }
            String value = cldrFile.getStringValue(xpath);
            checkPathValue(exampleGenerator, xpath, value, null);
            // remove this, no longer used
//            if (xpath.contains("count=\"one\"")) {
//                String xpath2 = xpath.replace("count=\"one\"", "count=\"1\"");
//                checkPathValue(exampleGenerator, xpath2, value, null);
//            }
        }
    }

    private void checkPathValue(ExampleGenerator exampleGenerator,
        String xpath, String value, String expected) {
        Set<String> alreadySeen = new HashSet<>();
        try {
            String text = exampleGenerator.getExampleHtml(xpath, value);
            if (text == null) {
                // skip
            } else if (text.contains("Exception")) {
                errln("getExampleHtml\t" + text);
            } else if (!alreadySeen.contains(text)) {
                if (text.contains("n/a")) {
                    if (text.contains("&lt;")) {
                        errln("Text not quoted correctly:" + "\t" + text
                            + "\t" + xpath);
                    }
                }
                boolean skipLog = false;
                if (expected != null) {
                    String simplified = ExampleGenerator.simplify(text, false);
                    // redo for debugging
                    text = exampleGenerator.getExampleHtml(xpath, value);
                    skipLog = !assertEquals("Example text for «" + value + "»", expected, simplified);
                }
                if (!skipLog) {
                    logln("getExampleHtml\t" + text + "\t"
                        + xpath);
                }
                alreadySeen.add(text);
            }
        } catch (Exception e) {
            errln("getExampleHtml\t" + e.getMessage());
        }

        try {
            String text = exampleGenerator.getHelpHtml(xpath, value);
            if (text == null) {
                // skip
            } else if (text.contains("Exception")) {
                errln("getHelpHtml\t" + text);
            } else {
                logln("getExampleHtml(help)\t" + "\t" + text + "\t" + xpath);
            }
        } catch (Exception e) {
            if (false) {
                e.printStackTrace();
            }
            errln("getHelpHtml\t" + e.getMessage());
        }
    }

    public void TestCompactPlurals() {
        checkCompactExampleFor("de", Count.one, "〖❬1❭ Mio. €〗", "short", "currency", "000000");
        checkCompactExampleFor("de", Count.other, "〖❬2❭ Mio. €〗", "short", "currency", "000000");
        checkCompactExampleFor("de", Count.one, "〖❬12❭ Mio. €〗", "short", "currency", "0000000");
        checkCompactExampleFor("de", Count.other, "〖❬10❭ Mio. €〗", "short", "currency", "0000000");

        checkCompactExampleFor("cs", Count.many, "〖❬1,1❭ milionu〗", "long", "decimal", "000000");
        checkCompactExampleFor("pl", Count.other, "〖❬1,1❭ miliona〗", "long", "decimal", "000000");
    }

    private void checkCompactExampleFor(String localeID, Count many,
        String expected, String longVsShort, String decimalVsCurrency, String zeros) {
        CLDRFile cldrFile = info.getCLDRFile(localeID, true);
        ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile,
            info.getEnglish(), CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        String path = "//ldml/numbers/"
            + decimalVsCurrency + "Formats[@numberSystem=\"latn\"]"
            + "/" + decimalVsCurrency + "FormatLength[@type=\"" + longVsShort + "\"]"
            + "/" + decimalVsCurrency + "Format[@type=\"standard\"]"
            + "/pattern[@type=\"1" + zeros + "\"][@count=\"" + many + "\"]";
        checkPathValue(exampleGenerator, path, cldrFile.getStringValue(path),
            expected);
    }

//ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="1000"][@count="one"]

    public void TestDayPeriods() {
        //checkDayPeriod("da", "format", "morning1", "〖05:00 – 10:00〗〖❬7:30❭ morgens〗");
        checkDayPeriod("zh", "format", "morning1", "〖05:00 – 08:00⁻〗〖清晨❬6:30❭〗");

        checkDayPeriod("de", "format", "morning1", "〖05:00 – 10:00⁻〗〖❬7:30 ❭morgens〗");
        checkDayPeriod("de", "stand-alone", "morning1", "〖05:00 – 10:00⁻〗");
        checkDayPeriod("de", "format", "morning2", "〖10:00 – 12:00⁻〗〖❬11:00 ❭vormittags〗");
        checkDayPeriod("de", "stand-alone", "morning2", "〖10:00 – 12:00⁻〗");
        checkDayPeriod("pl", "format", "morning1", "〖06:00 – 10:00⁻〗〖❬8:00 ❭rano〗");
        checkDayPeriod("pl", "stand-alone", "morning1", "〖06:00 – 10:00⁻〗");

        checkDayPeriod("en", "format", "night1", "〖00:00 – 06:00⁻; 21:00 – 24:00⁻〗〖❬3:00 ❭at night〗");
        checkDayPeriod("en", "stand-alone", "night1", "〖00:00 – 06:00⁻; 21:00 – 24:00⁻〗");

        checkDayPeriod("en", "format", "noon", "〖12:00〗〖❬12:00 ❭noon〗");
        checkDayPeriod("en", "format", "midnight", "〖00:00〗〖❬12:00 ❭midnight〗");
        checkDayPeriod("en", "format", "am", "〖00:00 – 12:00⁻〗〖❬6:00 ❭AM〗");
        checkDayPeriod("en", "format", "pm", "〖12:00 – 24:00⁻〗〖❬6:00 ❭PM〗");
    }

    private void checkDayPeriod(String localeId, String type, String dayPeriodCode, String expected) {
        CLDRFile cldrFile = info.getCLDRFile(localeId, true);
        ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile, info.getEnglish(), CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        String prefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"";
        String suffix = "\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\""
            + dayPeriodCode
            + "\"]";
        String path = prefix + type + suffix;
        checkPathValue(exampleGenerator, path, cldrFile.getStringValue(path), expected);
    }

    /**
     * Test that getExampleHtml returns same output for same input regardless of
     * order in which it is called with different inputs.
     *
     * Calling getExampleHtml with a particular path and value presumably should NOT depend on the
     * history of paths and/or values it has been called with previously.
     *
     * We formerly got different examples for SPECIAL_PATH depending on whether an example was
     * first gotten for USE_EVIL_PATH.
     *
     * Without EVIL_PATH, got right value for SPECIAL_PATH:
     * <div class='cldr_example'><span class='cldr_substituted'>123 456,79 </span>€</div>
     *
     * With EVIL_PATH, got wrong value for SPECIAL_PATH:
     * <div class='cldr_example'><span class='cldr_substituted'>123457 k </span>€</div>
     *
     * This was fixed by doing clone() before returning a DecimalFormat in ICUServiceBuilder.
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-13375.
     *
     * @throws IOException
     */
    public void TestExampleGeneratorConsistency() throws IOException {
        final String EVIL_PATH = "//ldml/numbers/currencyFormats/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"one\"]";
        final String SPECIAL_PATH = "//ldml/numbers/currencies/currency[@type=\"EUR\"]/symbol";
        final String EXPECTED = "123 456,79";

        final CLDRFile cldrFile = info.getCLDRFile("fr", true);
        final ExampleGenerator eg = new ExampleGenerator(cldrFile, info.getEnglish(), CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

        final String evilValue = cldrFile.getStringValue(EVIL_PATH);
        final String specialValue = cldrFile.getStringValue(SPECIAL_PATH);

        eg.getExampleHtml(EVIL_PATH, evilValue);
        final String specialExample = eg.getExampleHtml(SPECIAL_PATH, specialValue);

        if (!specialExample.contains(EXPECTED)) {
            errln("Expected example to contain " + EXPECTED + "; got " + specialExample);
        }
    }

    public void TestInflectedUnitExamples() {
        final CLDRFile cldrFile = info.getCLDRFile("de", true);
        ExampleGenerator exampleGenerator = getExampleGenerator("de");
        String pattern = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day\"]/unitPattern[@count=\"COUNT\"][@case=\"CASE\"]";
        String[][] tests = {
            {"one", "nominative",  "〖❬1❭ Tag〗〖❬1❭ Tag❬ kostet (kosten) € 3,50.❭〗"},
            {"one", "accusative",  "〖❬1❭ Tag〗〖❬… für 1❭ Tag❬ …❭〗"},
            {"one", "genitive",  "〖❬1❭ Tages〗〖❬Anstatt 1❭ Tages❬ …❭〗"},
            {"one", "dative",  "〖❬1❭ Tag〗〖❬… mit 1❭ Tag❬ …❭〗"},
            {"other", "nominative",  "〖❬1,5❭ Tage〗〖❬1,5❭ Tage❬ kostet (kosten) € 3,50.❭〗"},
            {"other", "accusative",  "〖❬1,5❭ Tage〗〖❬… für 1,5❭ Tage❬ …❭〗"},
            {"other", "genitive",  "〖❬1,5❭ Tage〗〖❬Anstatt 1,5❭ Tage❬ …❭〗"},
            {"other", "dative",  "〖❬1,5❭ Tagen〗〖❬… mit 1,5❭ Tagen❬ …❭〗"}
        };
        for (String[] row : tests) {
            String path = pattern.replace("COUNT", row[0]).replace("CASE", row[1]);
            String expected = row[2];
            String value = cldrFile.getStringValue(path);
            String actualRaw = exampleGenerator.getExampleHtml(path, value);
            String actual = ExampleGenerator.simplify(actualRaw, false);
            assertEquals(row[0] + ", " + row[1], expected, actual);
        }
    }

    public void TestMinimalPairExamples() {
        final CLDRFile cldrFile = info.getCLDRFile("de", true);
        ExampleGenerator exampleGenerator = getExampleGenerator("de");
        String[][] tests = {
            {"//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"one\"]", "〖❬1❭ Tag〗"},
            {"//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"other\"]", "〖❬2❭ Tage〗"},
            {"//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"accusative\"]", "〖… für ❬1 metrische Pint❭ …〗"},
            {"//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"dative\"]", "〖… mit ❬1 metrischen Pint❭ …〗"},
            {"//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"genitive\"]", "〖Anstatt ❬1 metrischen Pints❭ …〗"},
            {"//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"nominative\"]", "〖❬2 metrische Pints❭ kostet (kosten) € 3,50.〗"},
            {"//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"feminine\"]", "〖Die ❬Stunde❭ ist …〗"},
            {"//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"masculine\"]", "〖Der ❬Meter❭ ist …〗"},
            {"//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"neuter\"]", "〖Das ❬mol❭ ist …〗"},
        };
        for (String[] row : tests) {
            String path = row[0];
            String expected = row[1];
            String value = cldrFile.getStringValue(path);
            String actualRaw = exampleGenerator.getExampleHtml(path, value);
            String actual = ExampleGenerator.simplify(actualRaw, false);
            assertEquals(row[0] + ", " + row[1], expected, actual);
        }
        if (isVerbose()) { // generate examples
            PluralInfo pluralInfo = SDI.getPlurals(PluralType.cardinal, cldrFile.getLocaleID());
            ArrayList<String> paths = new ArrayList<>();

            for (Count plural : pluralInfo.getCounts()) {
                paths.add("//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"" + plural +  "\"]");
            }
            GrammarInfo grammarInfo = SDI.getGrammarInfo("de");
            for (String grammaticalValues : grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units)) {
                paths.add("//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"" + grammaticalValues +  "\"]");
            }
            for (String grammaticalValues : grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalGender, GrammaticalScope.units)) {
                paths.add("//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"" + grammaticalValues +  "\"]");
            }
            for (String path : paths) {
                String value = cldrFile.getStringValue(path);
                String actualRaw = exampleGenerator.getExampleHtml(path, value);
                String actual = ExampleGenerator.simplify(actualRaw, false);
                System.out.println("{\"" + path.replace("\"", "\\\"") + "\", \"" + actual + "\"},");
            }
        }
    }

}
