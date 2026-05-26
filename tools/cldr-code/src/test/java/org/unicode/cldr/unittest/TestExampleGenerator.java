package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.TimeZone;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.test.CheckCLDR.InputMethod;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckCLDR.StatusAction;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.UnitLength;
import org.unicode.cldr.test.RelatedDatePathValues;
import org.unicode.cldr.unittest.TestCheckCLDR.DummyPathValueInfo;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFileOverride;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrIntervalFormat;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.CaseValues;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.BaseUrl;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitPathType;
import org.unicode.cldr.util.VoteResolver.VoterInfo;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

public class TestExampleGenerator extends TestFmwk {

    private static final String SKIP = "SKIP";

    private static final Joiner CR_TAB2_JOINER = Joiner.on("\n\t\t");

    private static final boolean CHECK_ROW_ACTION = false;

    private static final Splitter SLASH2_SPLITTER =
            Splitter.on("//").omitEmptyStrings().trimResults();

    private static final Joiner TAB_JOINER = Joiner.on('\t');

    boolean showTranslationPaths =
            CldrUtility.getProperty("TestExampleGenerator:showTranslationPaths", false);

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
                "〖❬1,23❭_❬dollar des États-Unis❭〗〖❬1,23❭_❬euro❭〗〖❬0,00❭_❬dollar des États-Unis❭〗〖❬0,00❭_❬euro❭〗"
            },
            {
                "fr",
                "other",
                "〖❬2,34 ❭value-other〗〖❬3,45 ❭value-other〗",
                "〖❬2,34❭_❬dollars des États-Unis❭〗〖❬2,34❭_❬euros❭〗〖❬3,45❭_❬dollars des États-Unis❭〗〖❬3,45❭_❬euros❭〗"
            },
            {"en", "one", "〖❬1 ❭Bermudan dollar〗", "〖❬1❭ ❬US dollar❭〗〖❬1❭ ❬euro❭〗"},
            {
                "en",
                "other",
                "〖❬1.23 ❭Bermudan dollars〗〖❬0.00 ❭Bermudan dollars〗",
                "〖❬1.23❭ ❬US dollars❭〗〖❬1.23❭ ❬euros❭〗〖❬0.00❭ ❬US dollars❭〗〖❬0.00❭ ❬euros❭〗"
            },
        };
        String sampleCurrencyPatternPrefix =
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/unitPattern[@count=\"";
        String sampleCurrencyPrefix =
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"";
        String sampleTemplateSuffix = "\"]";

        for (String[] row : tests) {
            ExampleGenerator exampleGenerator = getExampleGenerator(row[0]);
            String value = "value-" + row[1];

            String path = sampleCurrencyPrefix + row[1] + sampleTemplateSuffix;
            String result =
                    ExampleGenerator.simplify(exampleGenerator.getExampleHtml(path, value), false);
            assertEquals(row[0] + "-" + row[1] + "-BMD", row[2], result);

            value = "{0}_{1}";
            path = sampleCurrencyPatternPrefix + row[1] + sampleTemplateSuffix;
            result = ExampleGenerator.simplify(exampleGenerator.getExampleHtml(path, value), false);
            assertEquals(row[0] + "-" + row[1] + "-pat", row[3], result);
        }
    }

    /**
     * Only add to this if the example should NEVER appear. <br>
     * WARNING - do not disable the test by putting in too broad a match. Make sure the paths are
     * reasonably granular.
     */
    static final Set<String> DELIBERATE_EXCLUDED_EXAMPLES =
            ImmutableSet.of(
                    "//ldml/layout/orientation/characterOrder",
                    "//ldml/layout/orientation/lineOrder",
                    "//ldml/characters/moreInformation",
                    "//ldml/numbers/symbols[@numberSystem=\"([^\"]*+)\"]/infinity",
                    "//ldml/numbers/symbols[@numberSystem=\"([^\"]*+)\"]/list",
                    "//ldml/numbers/symbols[@numberSystem=\"([^\"]*+)\"]/nan",
                    "//ldml/numbers/currencies/currency[@type=\"([^\"]*+)\"]/displayName",
                    "//ldml/localeDisplayNames/measurementSystemNames/measurementSystemName[@type=\"([^\"]*+)\"]",
                    // old format
                    "//ldml/posix/messages/nostr",
                    "//ldml/posix/messages/yesstr",
                    "//ldml/contextTransforms/contextTransformUsage[@type=\"([^\"]*+)\"]/contextTransform[@type=\"([^\"]*+)\"]",
                    "//ldml/characters/exemplarCharacters",
                    "//ldml/characters/exemplarCharacters[@type=\"([^\"]*+)\"]",
                    "//ldml/characters/parseLenients.*",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/months/monthContext[@type=\"([^\"]*+)\"]/monthWidth[@type=\"([^\"]*+)\"]/month[@type=\"([^\"]*+)\"]",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/days/dayContext[@type=\"([^\"]*+)\"]/dayWidth[@type=\"([^\"]*+)\"]/day[@type=\"([^\"]*+)\"]",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/quarters/quarterContext[@type=\"([^\"]*+)\"]/quarterWidth[@type=\"([^\"]*+)\"]/quarter[@type=\"([^\"]*+)\"]", // examples only for gregorian
                    "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/displayName",
                    "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/relative[@type=\"([^\"]*+)\"]",
                    "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/relativeTime[@type=\"([^\"]*+)\"]/relativeTimePattern[@count=\"([^\"]*+)\"]",
                    "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/relativePeriod",
                    "//ldml/dates/fields/field[@type=\"([^\"]*+)\"]/displayName[@alt=\"([^\"]*+)\"]",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/numericSeparators/numericDateSeparator",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/numericSeparators/numericTimeSeparator",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/cyclicNameSets/cyclicNameSet[@type=\"([^\"]*+)\"]/cyclicNameContext[@type=\"([^\"]*+)\"]/cyclicNameWidth[@type=\"([^\"]*+)\"]/cyclicName[@type=\"([^\"]*+)\"]",
                    "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"([^\"]*+)\"]",
                    "//ldml/numbers/minimalPairs/ordinalMinimalPairs[@ordinal=\"([^\"]*+)\"]",
                    "//ldml/characters/parseLenients[@scope=\"([^\"]*+)\"][@level=\"([^\"]*+)\"]/parseLenient[@sample=\"([^\"]*+)\"]",
                    "//ldml/numbers/rationalFormats/rationalUsage",
                    "//ldml/numbers/rationalFormats[@numberSystem=\"([^\"]*+)\"]/rationalUsage");

    // Only add to above if the example should NEVER appear.

    /**
     * Add to this if the example SHOULD appear, but we don't have it yet. <br>
     * TODO Add later
     */
    static final Set<String> TEMPORARY_EXCLUDED_EXAMPLES =
            ImmutableSet.of(
                    // CLDR-19227
                    "//ldml/characters/placeholderBoundarySpacing[@type=\"([^\"]*+)\"][@scopes=\"([^\"]*+)\"]",
                    // CLDR-14831
                    "//ldml/characters/nestedBracketReplacement[@bracket=\"([^\"]*+)\"]",
                    "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/beforeCurrency/currencyMatch",
                    "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/beforeCurrency/surroundingMatch",
                    "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/beforeCurrency/insertBetween",
                    "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/afterCurrency/currencyMatch",
                    "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/afterCurrency/surroundingMatch",
                    "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencySpacing/afterCurrency/insertBetween",
                    "//ldml/numbers/currencyFormats[@numberSystem=\"([^\"]*+)\"]/currencyPatternAppendISO", // TODO see CLDR-14831
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
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraNames/era[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]", // examples only for two closest eras to 2025
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraAbbr/era[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraNarrow/era[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/months/monthContext[@type=\"([^\"]*+)\"]/monthWidth[@type=\"([^\"]*+)\"]/month[@type=\"([^\"]*+)\"][@yeartype=\"([^\"]*+)\"]",
                    "//ldml/dates/timeZoneNames/gmtUnknownFormat", // TODO CLDR-14121
                    "//ldml/dates/timeZoneNames/gmtUnknownFormat[@alt=\"([^\"]*+)\"]", // TODO
                    // CLDR-14121
                    "//ldml/numbers/minimumGroupingDigits",
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
                    "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/long/standard", // Error:
                    // (TestExampleGenerator.java:245) No background:   <Coordinated Universal Time>
                    //    〖Coordinated Universal Time〗
                    "//ldml/personNames/nameOrderLocales[@order=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/foreignSpaceReplacement[@xml:space=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/foreignSpaceReplacement[@xml:space=\"([^\"]*+)\"]", // TODO
                    "//ldml/personNames/foreignSpaceReplacement[@alt=\"([^\"]*+)\"]",
                    "//ldml/personNames/foreignSpaceReplacement", // TODO CLDR-15384
                    "//ldml/personNames/nativeSpaceReplacement[@xml:space=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/nativeSpaceReplacement[@xml:space=\"([^\"]*+)\"]", // TODO
                    "//ldml/personNames/nativeSpaceReplacement[@alt=\"([^\"]*+)\"]",
                    "//ldml/personNames/nativeSpaceReplacement", // TODO CLDR-15384
                    "//ldml/personNames/initialPattern[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/initialPattern[@type=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/personName[@order=\"([^\"]*+)\"][@length=\"([^\"]*+)\"][@usage=\"([^\"]*+)\"][@formality=\"([^\"]*+)\"]/namePattern[@alt=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/sampleName[@item=\"([^\"]*+)\"]/nameField[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/sampleName[@item=\"([^\"]*+)\"]/nameField[@type=\"([^\"]*+)\"]", // TODO CLDR-15384
                    "//ldml/personNames/parameterDefault[@parameter=\"([^\"]*+)\"]" // TODO
                    // CLDR-15384
                    );

    // Add to above if the example SHOULD appear, but we don't have it yet. TODO Add later

    /**
     * Only add to this if the background should NEVER appear. <br>
     * The background is used when the element is used as part of another format. <br>
     * WARNING - do not disable the test by putting in too broad a match. Make sure the paths are
     * reasonably granular.
     */
    static final Set<String> DELIBERATE_OK_TO_MISS_BACKGROUND =
            ImmutableSet.of(
                    "//ldml/numbers/defaultNumberingSystem",
                    "//ldml/numbers/otherNumberingSystems/native",
                    // TODO fix formatting
                    "//ldml/characters/exemplarCharacters",
                    "//ldml/characters/exemplarCharacters[@type=\"([^\"]*+)\"]",
                    // TODO Add background
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/timeFormats/timeFormatLength[@type=\"([^\"]*+)\"]/timeFormat[@type=\"([^\"]*+)\"]/pattern[@type=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]", // CLDR-16606
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"([^\"]*+)\"][@alt=\"([^\"]*+)\"]", // CLDR-16606
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
     * Add to this if the background SHOULD appear, but we don't have them yet. <br>
     * The background is used when the element is used as part of another format. <br>
     * TODO Add later
     */
    static final Set<String> TEMPORARY_OK_TO_MISS_BACKGROUND =
            ImmutableSet.of(
                    "//ldml/numbers/defaultNumberingSystem",
                    "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"([^\"]*+)\"][@count=\"([^\"]*+)\"]",
                    "//ldml/dates/timeZoneNames/zone[@type=\"([^\"]*+)\"]/long/standard",
                    "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/short/generic",
                    "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/short/standard",
                    "//ldml/dates/timeZoneNames/metazone[@type=\"([^\"]*+)\"]/short/daylight",
                    "//ldml/personNames/personName[@order=\"([^\"]*+)\"][@length=\"([^\"]*+)\"][@formality=\"([^\"]*+)\"]/namePattern",
                    "//ldml/personNames/personName[@order=\"([^\"]*+)\"][@length=\"([^\"]*+)\"][@usage=\"([^\"]*+)\"][@formality=\"([^\"]*+)\"]/namePattern"); // CLDR-15384

    // Add to above if the background SHOULD appear, but we don't have them yet. TODO Add later

    public void TestAllPaths() {
        ExampleGenerator exampleGenerator = getExampleGenerator("en");
        Set<String> seen = new HashSet<>();
        CLDRFile cldrFile = exampleGenerator.getCldrFile();
        TreeSet<String> target = new TreeSet<>(cldrFile.getComparator());
        cldrFile.fullIterable().forEach(target::add);
        for (String path : target) {
            String plainStarred = PathStarrer.get(path);
            String value = cldrFile.getStringValue(path);
            if (value == null
                    || path.endsWith("/alias")
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
            String javaEscapedStarred = "\"" + plainStarred.replace("\"", "\\\"") + "\",";
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

                        errln(
                                "'null' in message:\t<"
                                        + value
                                        + ">\t"
                                        + simplified
                                        + "\t"
                                        + javaEscapedStarred);
                        // String example2 =
                        // exampleGenerator.getExampleHtml(path, value); // for
                        // debugging
                    }
                } else if (!simplified.startsWith("〖")) {
                    if (!seen.contains(javaEscapedStarred)) {
                        errln(
                                "Funny HTML:\t<"
                                        + value
                                        + ">\t"
                                        + simplified
                                        + "\t"
                                        + javaEscapedStarred);
                    }
                } else if (!simplified.contains("❬")
                        && !DELIBERATE_OK_TO_MISS_BACKGROUND.contains(plainStarred)) {
                    if (!seen.contains(javaEscapedStarred)) {

                        if (TEMPORARY_OK_TO_MISS_BACKGROUND.contains(plainStarred)
                                && logKnownIssue(
                                        "Cldrbug:6342",
                                        "Make sure that background appears: "
                                                + simplified
                                                + "; "
                                                + plainStarred)) {
                            continue;
                        }

                        errln(
                                "No background:\t<"
                                        + value
                                        + ">\t"
                                        + simplified
                                        + "\t"
                                        + javaEscapedStarred);
                    }
                }
            }
            seen.add(javaEscapedStarred);
        }
    }

    public void TestUnits() {
        ExampleGenerator exampleGenerator = getExampleGenerator("en");
        String staticMeterExample =
                "〖1 meter ≡ 1,000 millimeter〗〖1 meter ≈ 1.0936 yard (US/UK)〗〖1 meter ≡ 1/1000 kilometer〗〖1 meter ≈ 621.37×10ˆ-6 mile (US/UK)〗";
        String staticMeterExampleJp =
                "〖1 meter ≡ 1,000 millimeter〗〖1 meter ≡ 3.025 jo-jp (JP)〗〖1 meter ≈ 1.0936 yard (US/UK)〗〖1 meter ≈ 0.0023341 ri-jp (JP)〗〖1 meter ≡ 1/1000 kilometer〗〖1 meter ≈ 621.37×10ˆ-6 mile (US/UK)〗";
        String staticMeterExampleRi =
                "〖1 ri-jp (JP) ≡ 1,296 jo-jp (JP)〗〖1 ri-jp (JP) ≈ 468.54 yard (US/UK)〗〖1 ri-jp (JP) ≈ 428.43 meter〗〖1 ri-jp (JP) ≈ 0.42843 kilometer〗〖1 ri-jp (JP) ≈ 0.26621 mile (US/UK)〗";

        checkValue(
                "Length m",
                staticMeterExample,
                exampleGenerator,
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/displayName");
        checkValue(
                "Duration hm",
                "〖5:37〗",
                exampleGenerator,
                "//ldml/units/durationUnit[@type=\"hm\"]/durationUnitPattern");
        checkValue(
                "Length m",
                "〖❬1❭ meter〗〖〗" + staticMeterExample,
                exampleGenerator,
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"one\"]");
        checkValue(
                "Length m",
                "〖❬1.5❭ meters〗〖〗" + staticMeterExample,
                exampleGenerator,
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]");
        checkValue(
                "Length m",
                "〖❬1.5❭ m〗〖〗" + staticMeterExample,
                exampleGenerator,
                "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]");
        checkValue(
                "Length m",
                "〖❬1.5❭m〗〖〗" + staticMeterExample,
                exampleGenerator,
                "//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]");

        // The following are to ensure that we properly generate an example when we have a
        // non-winning value
        checkValue(
                "Length m",
                "〖❬1.5❭ badmeter〗〖〗" + staticMeterExample,
                exampleGenerator,
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]",
                "{0} badmeter");

        ExampleGenerator exampleGeneratorDe = getExampleGenerator("de");
        checkValue(
                "Length m",
                "〖❬1,5❭ badmeter〗〖❬Anstatt 1,5❭ badmeter❬ …❭〗〖❌  ❬… für 1,5❭ badmeter❬ …❭〗〖〗"
                        + staticMeterExample,
                exampleGeneratorDe,
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"][@case=\"genitive\"]",
                "{0} badmeter");

        ExampleGenerator exampleGeneratorJa = getExampleGenerator("ja");
        checkValue(
                "Length m",
                "〖❬1.5❭m〗〖〗" + staticMeterExampleJp,
                exampleGeneratorJa,
                "//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"other\"]");
        checkValue(
                "Length ri",
                "〖❬1.5❭ 里〗〖〗" + staticMeterExampleRi,
                exampleGeneratorJa,
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-ri-jp\"]/unitPattern[@count=\"other\"]");
    }

    /**
     * Check that the expected exampleGenerator example is produced for the parameters, with the
     * value coming from the file.
     */
    private void checkValue(
            String message, String expected, ExampleGenerator exampleGenerator, String path) {
        checkValue(message, expected, exampleGenerator, path, null);
    }

    /** Check that the expected exampleGenerator example is produced for the parameters */
    private void checkValue(
            String message,
            String expected,
            ExampleGenerator exampleGenerator,
            String path,
            String value) {
        final CLDRFile cldrFile = exampleGenerator.getCldrFile();
        value = value != null ? value : cldrFile.getStringValue(path);
        String actual = exampleGenerator.getExampleHtml(path, value);
        assertEquals(
                cldrFile.getLocaleID() + ": " + message,
                expected,
                ExampleGenerator.simplify(actual, false));
    }

    public void TestCompoundUnit() {
        String[][] tests = {
            {"per", "LONG", "one", "〖❬1 meter❭ per ❬second❭〗"},
            {"per", "SHORT", "one", "〖❬1 m❭/❬sec❭〗"},
            {"per", "NARROW", "one", "〖❬1m❭/❬s❭〗"},
            {"per", "LONG", "other", "〖❬1.5 meters❭ per ❬second❭〗"},
            {"per", "SHORT", "other", "〖❬1.5 m❭/❬sec❭〗"},
            {"per", "NARROW", "other", "〖❬1.5m❭/❬s❭〗"},
            {"times", "LONG", "one", "〖❬1 newton❭-❬meter❭〗"},
            {"times", "SHORT", "one", "〖❬1 N❭⋅❬m❭〗"},
            {"times", "NARROW", "one", "〖❬1N❭⋅❬m❭〗"},
            {"times", "LONG", "other", "〖❬1.5 newton❭-❬meters❭〗"},
            {"times", "SHORT", "other", "〖❬1.5 N❭⋅❬m❭〗"},
            {"times", "NARROW", "other", "〖❬1.5N❭⋅❬m❭〗"},
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
            List<String> examples = new ArrayList<>();
            exampleGenerator.handleCompoundUnit(
                    UnitLength.valueOf(test[1]), test[0], Count.valueOf(test[2]), examples);
            String actual = exampleGenerator.formatExampleList(examples);
            assertEquals("CompoundUnit", test[3], ExampleGenerator.simplify(actual, true));
        }
    }

    public void TestTranslationPaths() {
        for (String locale : Arrays.asList("en", "el", "ru")) {
            CLDRFile cldrFile = CLDRConfig.getInstance().getCldrFactory().make(locale, true);
            ExampleGenerator exampleGenerator = getExampleGenerator(locale);

            for (UnitPathType pathType : UnitPathType.values()) {
                for (String width : Arrays.asList("long", "short", "narrow")) {
                    if (pathType == UnitPathType.gender && !width.equals("long")) {
                        continue;
                    }
                    for (String unit : pathType.sampleShortUnitType) {
                        String path =
                                pathType.getTranslationPath(
                                        cldrFile, width, unit, "one", "nominative", null);
                        String value = cldrFile.getStringValue(path);
                        if (value != null) {
                            String example = exampleGenerator.getExampleHtml(path, value);
                            if (assertNotNull(locale + "/" + path, example)) {
                                String simplified = ExampleGenerator.simplify(example, false);
                                if (showTranslationPaths) {
                                    warnln(
                                            locale
                                                    + ", "
                                                    + width
                                                    + ", "
                                                    + pathType.toString()
                                                    + " ==>"
                                                    + simplified);
                                }
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
            {"de", "LONG", "other", "Quadrat{0}", "〖❬1,5 ❭Quadrat❬meter❭〗"},
            {"en", "SHORT", "one", "z{0}", "〖❬1 ❭z❬m❭〗"},
            {"en", "LONG", "other", "zetta{0}", "〖❬1.5 ❭zetta❬meters❭〗"},
            {"en", "SHORT", "one", "{0}²", "〖❬1 m❭²〗"},
            {"en", "LONG", "other", "square {0}", "〖❬1.5 ❭square ❬meters❭〗"},
            {"de", "SHORT", "one", "z{0}", "〖❬1 ❭z❬m❭〗"},
            {"de", "LONG", "other", "Zetta{0}", "〖❬1,5 ❭Zetta❬meter❭〗"},
            {"de", "SHORT", "one", "{0}²", "〖❬1 m❭²〗"},
            {"de", "LONG", "other", "Quadrat{0}", "〖❬1,5 ❭Quadrat❬meter❭〗"},
        };
        for (String[] test : tests) {

            ExampleGenerator exampleGenerator = getExampleGenerator(test[0]);
            List<String> examples = new ArrayList<>();
            exampleGenerator.handleCompoundUnit1(
                    UnitLength.valueOf(test[1]), Count.valueOf(test[2]), test[3], examples);
            String actual = exampleGenerator.formatExampleList(examples);
            assertEquals("CompoundUnit", test[4], ExampleGenerator.simplify(actual, true));
        }
    }

    public void TestCompoundUnit3() {
        final Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        String[][] tests = {
            // locale, path, value, expected-example
            {
                "en",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                "LOCALE",
                "〖square ❬meters❭〗"
            }, //
            {
                "en",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                "LOCALE",
                "〖❬1 ❭square ❬meter❭〗"
            }, //
            {
                "en",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                "LOCALE",
                "〖❬1.5 ❭square ❬meters❭〗"
            }, //
            {
                "en",
                "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                "LOCALE",
                "〖❬m❭²〗"
            },
            {
                "en",
                "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                "LOCALE",
                "〖❬1m❭²〗"
            },
            {
                "en",
                "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                "LOCALE",
                "〖❬1.5m❭²〗"
            },

            // warning, french patterns has U+00A0 in them
            {
                "fr",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                "Square {0}",
                "〖Square ❬mètres❭〗"
            },
            {
                "fr",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                "square {0}",
                "〖❬1,5 ❭square ❬mètre❭〗"
            },
            {
                "fr",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                "squares {0}",
                "〖❬3,5 ❭squares ❬mètres❭〗"
            },
            {
                "fr",
                "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1",
                "LOCALE",
                "〖❬m❭²〗"
            },
            {
                "fr",
                "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                "LOCALE",
                "〖❬1,5m❭²〗"
            },
            {
                "fr",
                "//ldml/units/unitLength[@type=\"narrow\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                "LOCALE",
                "〖❬3,5m❭²〗"
            },
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
            assertEquals(
                    ++lineCount + ") " + localeID + ", CompoundUnit3",
                    expected,
                    ExampleGenerator.simplify(actual, false));
        }
    }

    HashMap<String, ExampleGenerator> ExampleGeneratorCache = new HashMap<>();

    private ExampleGenerator getExampleGenerator(String locale) {
        ExampleGenerator result = ExampleGeneratorCache.get(locale);
        if (result == null) {
            final CLDRFile nativeCldrFile = info.getCLDRFile(locale, true);
            result = new ExampleGenerator(nativeCldrFile, info.getEnglish());
            ExampleGeneratorCache.put(locale, result);
        }
        return result;
    }

    public void TestEllipsis() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        String[][] tests = {
            {"initial", "〖…❬iappone❭〗"},
            {"medial", "〖❬Svizzer❭…❬iappone❭〗"},
            {"final", "〖❬Svizzer❭…〗"},
            {"word-initial", "〖… ❬Giappone❭〗"},
            {"word-medial", "〖❬Svizzera❭ … ❬Giappone❭〗"},
            {"word-final", "〖❬Svizzera❭ …〗"},
        };
        for (String[] pair : tests) {
            checkValue(
                    exampleGenerator,
                    "//ldml/characters/ellipsis[@type=\"" + pair[0] + "\"]",
                    pair[1]);
        }
    }

    private void checkValue(ExampleGenerator exampleGenerator, String path, String expected) {
        String value = exampleGenerator.getCldrFile().getStringValue(path);
        String result =
                ExampleGenerator.simplify(exampleGenerator.getExampleHtml(path, value), false);
        assertEquals("Ellipsis", expected, result);
    }

    public static String simplify(String exampleHtml) {
        return ExampleGenerator.simplify(exampleHtml, false);
    }

    public void TestClip() {
        assertEquals("Clipping", "bc", ExampleGenerator.clip("abc", 1, 0));
        assertEquals("Clipping", "ab", ExampleGenerator.clip("abc", 0, 1));
        assertEquals(
                "Clipping", "b\u0308c\u0308", ExampleGenerator.clip("a\u0308b\u0308c\u0308", 1, 0));
        assertEquals(
                "Clipping", "a\u0308b\u0308", ExampleGenerator.clip("a\u0308b\u0308c\u0308", 0, 1));
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
        checkValue(
                "Range",
                "〖❬99❭-❬144❭〗",
                exampleGenerator,
                "//ldml/numbers/miscPatterns[@numberSystem=\"latn\"]/pattern[@type=\"range\"]");
        // String actual = exampleGenerator.getExampleHtml(
        // "//ldml/numbers/miscPatterns[@type=\"arab\"]/pattern[@type=\"atLeast\"]",
        // "at least {0}", Zoomed.IN);
        // assertEquals("Invalid format",
        // "<div class='cldr_example'>at least 99</div>", actual);
    }

    public void TestPluralSamples() {
        ExampleGenerator exampleGenerator = getExampleGenerator("sv");
        String[][] tests = {
            {
                "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"length-centimeter\"]/unitPattern[@count=\"one\"]",
                "Number should be one",
                "〖❬1❭ cm〗〖❬Jag tror att 1❭ cm❬ är tillräckligt.❭〗"
            },
            {
                "//ldml/numbers/minimalPairs/ordinalMinimalPairs[@ordinal=\"one\"]",
                "Ordinal one",
                "〖Ta ❬1❭:a svängen till höger〗〖❌  Ta ❬3❭:a svängen till höger〗"
            },
            {
                "//ldml/numbers/minimalPairs/ordinalMinimalPairs[@ordinal=\"other\"]",
                "Ordinal other",
                "〖Ta ❬3❭:e svängen till höger〗〖❌  Ta ❬1❭:e svängen till höger〗"
            },
        };
        for (String[] row : tests) {
            String path = row[0];
            String message = row[1];
            String expected = row[2];
            checkValue(message, expected, exampleGenerator, path);
        }
    }

    public void TestLocaleDisplayPatterns() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        String actual =
                exampleGenerator.getExampleHtml(
                        "//ldml/localeDisplayNames/localeDisplayPattern/localePattern",
                        "{0} [{1}]");
        assertEquals(
                "localePattern example faulty",
                "<div class='cldr_example'><span class='cldr_substituted'>uzbeco</span> [<span class='cldr_substituted'>Afghanistan</span>]</div>"
                        + "<div class='cldr_example'><span class='cldr_substituted'>uzbeco</span> [<span class='cldr_substituted'>arabo, Afghanistan</span>]</div>"
                        + "<div class='cldr_example'><span class='cldr_substituted'>uzbeco</span> [<span class='cldr_substituted'>arabo, Afghanistan, Cifre indo-arabe, Fuso orario: Ora Etiopia</span>]</div>",
                actual);
        actual =
                exampleGenerator.getExampleHtml(
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
        String actual =
                simplify(
                        exampleGenerator.getExampleHtml(
                                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                                "¤ #0.00"));
        assertEquals("Currency format example faulty", "〖€ ❬1295,00❭〗〖-€ ❬1295,00❭〗", actual);
    }

    public void TestCurrencyFormatsWithContext() {
        ExampleGenerator exampleGenerator = getExampleGenerator("he");
        String actual =
                simplify(
                        exampleGenerator.getExampleHtml(
                                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                                "‏#,##0.00 ¤;‏-#,##0.00 ¤"));
        assertEquals(
                "Currency format example faulty",
                "【‏❬1,295❭.❬00❭ ₪〗【⃪‏❬1,295❭.❬00❭ ₪〗【‏‎-❬1,295❭.❬00❭ ₪〗【⃪‏‎-❬1,295❭.❬00❭ ₪〗【‏❬1,295❭.❬00❭ ILS〗【⃪‏❬1,295❭.❬00❭ ILS〗【‏‎-❬1,295❭.❬00❭ ILS〗【⃪‏‎-❬1,295❭.❬00❭ ILS〗",
                actual);
    }

    public void TestDateFormatsWithContext() {
        ExampleGenerator exampleGenerator = getExampleGenerator("ar");
        String actual =
                simplify(
                        exampleGenerator.getExampleHtml(
                                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"short\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                                "d‏/M‏/y"));
        assertEquals("Currency format example faulty", "【5‏/9‏/1999〗【⃪5‏/9‏/1999〗", actual);
    }

    public void TestDateTimeComboFormats() {
        ExampleGenerator exampleGenerator = getExampleGenerator("en");
        checkValue(
                "DateTimeCombo long std",
                "〖❬September 5, 1999❭, ❬1:25:59 PM Eastern Standard Time❭〗〖❬September 5, 1999❭, ❬1:25 PM❭〗〖❬September 5, 1999❭, ❬7:00 AM – 1:25 PM❭〗〖❬today❭, ❬7:00 AM – 1:25 PM❭〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/dateTimeFormatLength[@type=\"long\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
        checkValue(
                "DateTimeCombo short std",
                "〖❬9/5/99❭, ❬1:25:59 PM EST❭〗〖❬9/5/99❭, ❬1:25 PM❭〗〖❬9/5/99❭, ❬7:00 AM – 1:25 PM❭〗〖❬today❭, ❬7:00 AM – 1:25 PM❭〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/dateTimeFormatLength[@type=\"short\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
        checkValue(
                "DateTimeCombo long atTime",
                "〖❬September 5, 1999❭ at ❬1:25:59 PM Eastern Standard Time❭〗〖❬September 5, 1999❭ at ❬1:25 PM❭〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/dateTimeFormatLength[@type=\"long\"]/dateTimeFormat[@type=\"atTime\"]/pattern[@type=\"standard\"]");
        checkValue(
                "DateTimeCombo long relative",
                "〖❬today❭ at ❬1:25 PM❭〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/dateTimeFormatLength[@type=\"long\"]/dateTimeFormat[@type=\"relative\"]/pattern[@type=\"standard\"]");
    }

    public void TestDateSymbols() {
        ExampleGenerator exampleGenerator = getExampleGenerator("cs");
        checkValue(
                "cs format wide",
                "〖5. června 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"6\"]");
        checkValue(
                "cs format abbreviated",
                "〖5. čvn 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"6\"]");
        checkValue(
                "cs stand-alone wide",
                "〖červen 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"wide\"]/month[@type=\"6\"]");
        checkValue(
                "cs stand-alone abbreviated",
                "〖čvn 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"6\"]");
    }

    public void TestMinimumGroupingExamples() {
        ExampleGenerator exampleGeneratorEn = getExampleGenerator("en"); // min grouping 1
        ExampleGenerator exampleGeneratorEs = getExampleGenerator("es"); // min grouping 2
        checkValue(
                "MinimumGrouping en: 1",
                "〖❬543.21❭〗〖❬6,543❭.❬21❭〗〖❬76,543❭.❬21❭〗",
                exampleGeneratorEn,
                "//ldml/numbers/minimumGroupingDigits");
        checkValue(
                "MinimumGrouping es: 2",
                "〖❬543,21❭〗〖❬6543,21❭〗〖❬76.543❭,❬21❭〗",
                exampleGeneratorEs,
                "//ldml/numbers/minimumGroupingDigits");
    }

    public void TestSymbols() {
        CLDRFile english = info.getEnglish();
        ExampleGenerator exampleGenerator = new ExampleGenerator(english, english);
        String actual =
                exampleGenerator.getExampleHtml(
                        "//ldml/numbers/symbols[@numberSystem=\"latn\"]/superscriptingExponent",
                        "x");

        assertEquals(
                "superscriptingExponent faulty",
                "<div class='cldr_example'><span class='cldr_substituted'>1.23456789</span>x10<span class='cldr_substituted'><sup>5</sup></span></div>",
                actual);
    }

    public void TestFallbackFormat() {
        ExampleGenerator exampleGenerator =
                new ExampleGenerator(info.getEnglish(), info.getEnglish());
        String actual =
                exampleGenerator.getExampleHtml(
                        "//ldml/dates/timeZoneNames/fallbackFormat", "{1} [{0}]");
        assertEquals(
                "fallbackFormat faulty",
                "〖❬Central Time❭ [❬Cancún❭]〗",
                ExampleGenerator.simplify(actual, false));
    }

    public void Test4897() {
        ExampleGenerator exampleGenerator = getExampleGenerator("it");
        final CLDRFile cldrFile = exampleGenerator.getCldrFile();
        for (String xpath :
                With.in(
                        cldrFile.iterator(
                                "//ldml/dates/timeZoneNames", cldrFile.getComparator()))) {
            String value = cldrFile.getStringValue(xpath);
            String actual = exampleGenerator.getExampleHtml(xpath, value);
            if (actual == null) {
                if (!xpath.contains("singleCountries") && !xpath.contains("gmtUnknownFormat")) {
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
                "〖❬1,23 ❭dollari delle Bermuda〗〖❬0,00 ❭dollari delle Bermuda〗"
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/unitPattern[@count=\"other\"]",
                "〖❬1,23❭ ❬dollari statunitensi❭〗〖❬1,23❭ ❬euro❭〗〖❬0,00❭ ❬dollari statunitensi❭〗〖❬0,00❭ ❬euro❭〗"
            },
            {"//ldml/numbers/currencies/currency[@type=\"BMD\"]/symbol", "〖❬123.456,79 ❭BMD〗"},
        };

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
                "<div class='cldr_example'><span class='cldr_substituted'>1</span> thousand</div>"
            },
            {
                "//ldml/numbers/percentFormats[@numberSystem=\"latn\"]/percentFormatLength/percentFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
                "<div class='cldr_example'><span class='cldr_substituted'>5</span>%</div>"
                        + "<div class='cldr_example'><span class='cldr_substituted'>12,345</span>,<span class='cldr_substituted'>679</span>%</div>"
                        + "<div class='cldr_example'>-<span class='cldr_substituted'>12,345</span>,<span class='cldr_substituted'>679</span>%</div>"
            }
        };
        final CLDRFile nativeCldrFile = info.getEnglish();
        ExampleGenerator exampleGenerator =
                new ExampleGenerator(info.getEnglish(), info.getEnglish());
        for (String[] testPair : testPairs) {
            String xpath = testPair[0];
            String expected = testPair[1];
            String value = nativeCldrFile.getStringValue(xpath);
            String actual = exampleGenerator.getExampleHtml(xpath, value);
            assertEquals("specifics", expected, actual);
        }
    }

    private void showCldrFile(final CLDRFile cldrFile) {
        ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile, info.getEnglish());
        checkPathValue(
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"full\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@draft=\"unconfirmed\"]",
                "EEEE d MMMMl y'x'G",
                null);

        for (String xpath : cldrFile.fullIterable()) {
            if (xpath.endsWith("/alias")) {
                continue;
            }
            String value = cldrFile.getStringValue(xpath);
            checkPathValue(exampleGenerator, xpath, value, null);
        }
    }

    private void checkPathValue(
            ExampleGenerator exampleGenerator, String xpath, String value, String expected) {
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
                        errln("Text not quoted correctly:" + "\t" + text + "\t" + xpath);
                    }
                }
                boolean skipLog = false;
                if (expected != null) {
                    String simplified = ExampleGenerator.simplify(text, false);
                    // redo for debugging
                    text = exampleGenerator.getExampleHtml(xpath, value);
                    XPathParts parts = XPathParts.getFrozenInstance(xpath);
                    // expecting something like:
                    // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="1000"][@count="one"]
                    skipLog =
                            // match the order and operands in checkCompact to make
                            // debugging easier.
                            // ("de", CChoice.decimal, Count.other, "short", "1000", "〖❬2❭ Mio.〗");
                            !assertEquals(
                                    "Example text for "
                                            + Joiners.COMMA_SP.join(
                                                    parts.getElement(4), // decimal vs currency
                                                    parts.getAttributeValue(
                                                            -1, "count"), // plural form
                                                    parts.getAttributeValue(
                                                            3, "type"), // short vs long
                                                    parts.getAttributeValue(
                                                            -1,
                                                            "type"), // 1000..00 compact category
                                                    "«" + value + "»"),
                                    expected,
                                    simplified);
                }
                if (!skipLog) {
                    logln("getExampleHtml\t" + text + "\t" + xpath);
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

    enum CChoice {
        decimal,
        currency
    }

    public void TestCompactPlurals() {

        // German is an example of a language that doesn't translate short values below a million

        // Currency (has no long form)
        // For German there is no number that has plural category 'one' until we hit a million. But
        // from 10-99 million, also none
        checkCompact("de", CChoice.currency, Count.one, "short", "1000", "〖❬0.000❭ €〗");
        checkCompact("de", CChoice.currency, Count.one, "short", "10000", "〖❬00.000❭ €〗");
        checkCompact("de", CChoice.currency, Count.one, "short", "100000", "〖❬000.000❭ €〗");
        checkCompact("de", CChoice.currency, Count.one, "short", "1000000", "〖❬1❭ Mio. €〗");
        checkCompact("de", CChoice.currency, Count.one, "short", "10000000", "〖❬00❭ Mio. €〗");

        checkCompact("de", CChoice.currency, Count.other, "short", "1000", "〖❬1.000❭ €〗");
        checkCompact("de", CChoice.currency, Count.other, "short", "10000", "〖❬10.000❭ €〗");
        checkCompact("de", CChoice.currency, Count.other, "short", "100000", "〖❬100.000❭ €〗");
        checkCompact("de", CChoice.currency, Count.other, "short", "1000000", "〖❬2❭ Mio. €〗");
        checkCompact("de", CChoice.currency, Count.other, "short", "10000000", "〖❬10❭ Mio. €〗");

        // Decimal short
        checkCompact("de", CChoice.decimal, Count.one, "short", "1000", "〖❬0.000❭〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "10000", "〖❬00.000❭〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "100000", "〖❬000.000❭〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "1000000", "〖❬1❭ Mio.〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "10000000", "〖❬00❭ Mio.〗");

        checkCompact("de", CChoice.decimal, Count.other, "short", "1000", "〖❬1.000❭〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "10000", "〖❬10.000❭〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "100000", "〖❬100.000❭〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "1000000", "〖❬2❭ Mio.〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "10000000", "〖❬10❭ Mio.〗");

        // Decimal long
        // But the long forms can have plural category one

        checkCompact("de", CChoice.decimal, Count.one, "short", "1000", "〖❬0.000❭〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "10000", "〖❬00.000❭〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "100000", "〖❬000.000❭〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "1000000", "〖❬1❭ Mio.〗");
        checkCompact("de", CChoice.decimal, Count.one, "short", "10000000", "〖❬00❭ Mio.〗");

        checkCompact("de", CChoice.decimal, Count.other, "short", "1000", "〖❬1.000❭〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "10000", "〖❬10.000❭〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "100000", "〖❬100.000❭〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "1000000", "〖❬2❭ Mio.〗");
        checkCompact("de", CChoice.decimal, Count.other, "short", "10000000", "〖❬10❭ Mio.〗");

        // Also check some languages with more complicated plurals

        checkCompact("cs", CChoice.decimal, Count.many, "long", "1000000", "〖❬1,1❭ milionu〗");
        checkCompact("pl", CChoice.decimal, Count.other, "long", "1000000", "〖❬1,1❭ miliona〗");
    }

    private void checkCompact(
            String localeID,
            CChoice decimalVsCurrency,
            Count many,
            String longVsShort,
            String zeros,
            String expected) {
        CLDRFile cldrFile = info.getCLDRFile(localeID, true);
        ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile, info.getEnglish());
        String path =
                "//ldml/numbers/"
                        + decimalVsCurrency
                        + "Formats[@numberSystem=\"latn\"]"
                        + "/"
                        + decimalVsCurrency
                        + "FormatLength[@type=\""
                        + longVsShort
                        + "\"]"
                        + "/"
                        + decimalVsCurrency
                        + "Format[@type=\"standard\"]"
                        + "/pattern[@type=\""
                        + zeros
                        + "\"][@count=\""
                        + many
                        + "\"]";
        checkPathValue(exampleGenerator, path, cldrFile.getStringValue(path), expected);
    }

    // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="1000"][@count="one"]

    public void TestDayPeriods() {
        // checkDayPeriod("da", "format", "morning1", "〖05:00 – 10:00〗〖❬7:30❭ morgens〗");
        checkDayPeriod("zh", "format", "morning1", "〖05:00 – 08:00⁻〗〖清晨❬6:30❭〗");

        checkDayPeriod("de", "format", "morning1", "〖05:00 – 10:00⁻〗〖❬7:30 ❭morgens〗");
        checkDayPeriod("de", "stand-alone", "morning1", "〖05:00 – 10:00⁻〗");
        checkDayPeriod("de", "format", "morning2", "〖10:00 – 12:00⁻〗〖❬11:00 ❭vormittags〗");
        checkDayPeriod("de", "stand-alone", "morning2", "〖10:00 – 12:00⁻〗");
        checkDayPeriod("pl", "format", "morning1", "〖06:00 – 10:00⁻〗〖❬8:00 ❭rano〗");
        checkDayPeriod("pl", "stand-alone", "morning1", "〖06:00 – 10:00⁻〗");

        checkDayPeriod("en", "format", "night1", "〖21:00 – 24:00⁻〗〖❬10:30 ❭at night〗");
        checkDayPeriod("en", "stand-alone", "night1", "〖21:00 – 24:00⁻〗");

        checkDayPeriod("en", "format", "noon", "〖12:00〗〖❬12:00 ❭noon〗");
        checkDayPeriod("en", "format", "midnight", "〖00:00〗〖❬12:00 ❭midnight〗");
        checkDayPeriod("en", "format", "am", "〖00:00 – 12:00⁻〗〖❬6:00 ❭AM〗");
        checkDayPeriod("en", "format", "pm", "〖12:00 – 24:00⁻〗〖❬6:00 ❭PM〗");
    }

    private void checkDayPeriod(
            String localeId, String type, String dayPeriodCode, String expected) {
        CLDRFile cldrFile = info.getCLDRFile(localeId, true);
        ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile, info.getEnglish());
        String prefix =
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"";
        String suffix =
                "\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"" + dayPeriodCode + "\"]";
        String path = prefix + type + suffix;
        checkPathValue(exampleGenerator, path, cldrFile.getStringValue(path), expected);
    }

    public void TestAllDayPeriods() { // excludes midnight, see ICU-12278
        checkDayPeriodsForLocale(
                "en",
                "Bhm",
                "〖6:00 in the morning〗〖12:00 noon〗〖3:00 in the afternoon〗〖7:30 in the evening〗〖10:30 at night〗");
        checkDayPeriodsForLocale(
                "it",
                "Bhm",
                "〖3:00 di notte〗〖9:00 di mattina〗〖12:00 mezzogiorno〗〖3:00 di pomeriggio〗〖9:00 di sera〗");
        checkDayPeriodsForLocale(
                "de",
                "Bhm",
                "〖2:30 nachts〗〖7:30 morgens〗〖11:00 vorm.〗〖12:30 mittags〗〖3:30 nachm.〗〖9:00 abends〗");
        checkDayPeriodsForLocale("zh", "Bhm", "〖凌晨2:30〗〖早上6:30〗〖上午10:00〗〖中午12:30〗〖下午4:00〗〖晚上9:30〗");
        checkDayPeriodsForLocale(
                "am",
                "EBhm",
                "〖ሐሙስ በሌሊት 3:00〗〖ሐሙስ ጥዋት 9:00〗〖ሐሙስ ቀትር 12:00〗〖ሐሙስ ከሰዓት 3:00〗〖ሐሙስ በምሽት 9:00〗");
        checkDayPeriodsForLocale(
                "hi",
                "EBhms",
                "〖गुरु रात 2:00:00〗〖गुरु सुबह 8:00:00〗〖गुरु दोपहर 2:00:00〗〖गुरु शाम 6:00:00〗");
    }

    public void checkDayPeriodsForLocale(String localeId, String pattern, String expected) {
        ExampleGenerator exampleGenerator = getExampleGenerator(localeId);
        String path =
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]"
                        + "/dateTimeFormats/availableFormats/dateFormatItem"
                        + "[@id=\""
                        + pattern
                        + "\"]";
        String message = "Day periods with pattern \"" + pattern + "\"";
        checkValue(message, expected, exampleGenerator, path);
    }

    /**
     * Test that getExampleHtml returns same output for same input regardless of order in which it
     * is called with different inputs.
     *
     * <p>Calling getExampleHtml with a particular path and value presumably should NOT depend on
     * the history of paths and/or values it has been called with previously.
     *
     * <p>We formerly got different examples for SPECIAL_PATH depending on whether an example was
     * first gotten for USE_EVIL_PATH.
     *
     * <p>Without EVIL_PATH, got right value for SPECIAL_PATH: <div class='cldr_example'><span
     * class='cldr_substituted'>123 456,79 </span>€</div>
     *
     * <p>With EVIL_PATH, got wrong value for SPECIAL_PATH: <div class='cldr_example'><span
     * class='cldr_substituted'>123457 k </span>€</div>
     *
     * <p>This was fixed by doing clone() before returning a DecimalFormat in ICUServiceBuilder.
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-13375.
     *
     * <p>Subsequently, DAIP changed to normalize NNBSP to NBSP for some paths, so this test was
     * revised not to depend on that distinction, only to expect an example containing "456,79" as a
     * substring (DAIP has its own unit tests). Ideally this test might be improved so as not to
     * depend on actual values at all, but would call getExampleHtml repeatedly for the same set of
     * paths but in different orders and confirm the example is the same regardless of the order;
     * that would require disabling the cache.
     *
     * @throws IOException
     */
    public void TestExampleGeneratorConsistency() throws IOException {
        final String EVIL_PATH =
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"one\"]";
        final String SPECIAL_PATH = "//ldml/numbers/currencies/currency[@type=\"EUR\"]/symbol";
        final String EXPECTED_TO_CONTAIN = "456,79";

        final CLDRFile cldrFile = info.getCLDRFile("fr", true);
        final ExampleGenerator eg = new ExampleGenerator(cldrFile, info.getEnglish());

        final String evilValue = cldrFile.getStringValue(EVIL_PATH);
        final String specialValue = cldrFile.getStringValue(SPECIAL_PATH);

        eg.getExampleHtml(EVIL_PATH, evilValue);
        final String specialExample = eg.getExampleHtml(SPECIAL_PATH, specialValue);
        if (!specialExample.contains(EXPECTED_TO_CONTAIN)) {
            errln("Expected example to contain " + EXPECTED_TO_CONTAIN + "; got " + specialExample);
        }
    }

    public void TestInflectedUnitExamples() {
        String[][] deTests = {
            {
                "one",
                "accusative",
                "〖❬1❭ Tag〗〖❬… für 1❭ Tag❬ …❭〗〖❌  ❬Anstatt 1❭ Tag❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "one",
                "dative",
                "〖❬1❭ Tag〗〖❬… mit 1❭ Tag❬ …❭〗〖❌  ❬Anstatt 1❭ Tag❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "one",
                "genitive",
                "〖❬1❭ Tages〗〖❬Anstatt 1❭ Tages❬ …❭〗〖❌  ❬… für 1❭ Tages❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "one",
                "nominative",
                "〖❬1❭ Tag〗〖❬1❭ Tag❬ kostet (kosten) € 3,50.❭〗〖❌  ❬Anstatt 1❭ Tag❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "other",
                "accusative",
                "〖❬1,5❭ Tage〗〖❬… für 1,5❭ Tage❬ …❭〗〖❌  ❬… mit 1,5❭ Tage❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "other",
                "dative",
                "〖❬1,5❭ Tagen〗〖❬… mit 1,5❭ Tagen❬ …❭〗〖❌  ❬… für 1,5❭ Tagen❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "other",
                "genitive",
                "〖❬1,5❭ Tage〗〖❬Anstatt 1,5❭ Tage❬ …❭〗〖❌  ❬… mit 1,5❭ Tage❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "other",
                "nominative",
                "〖❬1,5❭ Tage〗〖❬1,5❭ Tage❬ kostet (kosten) € 3,50.❭〗〖❌  ❬… mit 1,5❭ Tage❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
        };
        checkInflectedUnitExamples("de", deTests);
        String[][] elTests = {
            {
                "one",
                "accusative",
                "〖❬1❭ ημέρα〗〖❬… ανά 1❭ ημέρα❬ …❭〗〖❌  ❬… αξίας 1❭ ημέρα❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "one",
                "genitive",
                "〖❬1❭ ημέρας〗〖❬… αξίας 1❭ ημέρας❬ …❭〗〖❌  ❬… ανά 1❭ ημέρας❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "one",
                "nominative",
                "〖❬1❭ ημέρα〗〖❬Η απόσταση είναι 1❭ ημέρα❬ …❭〗〖❌  ❬… αξίας 1❭ ημέρα❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "other",
                "accusative",
                "〖❬0,9❭ ημέρες〗〖❬… ανά 0,9❭ ημέρες❬ …❭〗〖❌  ❬… αξίας 0,9❭ ημέρες❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "other",
                "genitive",
                "〖❬0,9❭ ημερών〗〖❬… αξίας 0,9❭ ημερών❬ …❭〗〖❌  ❬… ανά 0,9❭ ημερών❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
            {
                "other",
                "nominative",
                "〖❬0,9❭ ημέρες〗〖❬Η απόσταση είναι 0,9❭ ημέρες❬ …❭〗〖❌  ❬… αξίας 0,9❭ ημέρες❬ …❭〗〖〗〖1 day ≡ 24 hour〗〖1 day ≡ 1/7 week〗"
            },
        };
        checkInflectedUnitExamples("el", elTests);
    }

    private void checkInflectedUnitExamples(final String locale, String[][] tests) {
        final CLDRFile cldrFile = info.getCLDRFile(locale, true);
        ExampleGenerator exampleGenerator = getExampleGenerator(locale);
        String pattern =
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day\"]/unitPattern[@count=\"COUNT\"][@case=\"CASE\"]";
        boolean showWorkingExamples = false;
        for (String[] row : tests) {
            String path = pattern.replace("COUNT", row[0]).replace("CASE", row[1]);
            String expected = row[2];
            String value = cldrFile.getStringValue(path);
            String actualRaw = exampleGenerator.getExampleHtml(path, value);
            String actual = ExampleGenerator.simplify(actualRaw, false);
            showWorkingExamples |= !assertEquals(row[0] + ", " + row[1], expected, actual);
        }

        // If a test fails, verbose will regenerate what the code thinks they should be.
        // Review for correctness, and then replace the test cases

        if (showWorkingExamples) {
            System.out.println(
                    "## The following would satisfy the test, but check to make sure the expected values are all correct!");
            PluralInfo pluralInfo = SDI.getPlurals(PluralType.cardinal, locale);
            GrammarInfo grammarInfo = SDI.getGrammarInfo(locale);
            final Collection<String> grammaticalValues2 =
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalCase,
                            GrammaticalScope.units);

            for (Count plural : pluralInfo.getCounts()) {
                for (String grammaticalCase : grammaticalValues2) {
                    String path =
                            pattern.replace("COUNT", plural.toString())
                                    .replace("CASE", grammaticalCase);
                    String value = cldrFile.getStringValue(path);
                    String actualRaw = exampleGenerator.getExampleHtml(path, value);
                    String actual = ExampleGenerator.simplify(actualRaw, false);
                    System.out.println(
                            "{\""
                                    + plural
                                    + "\", "
                                    + "\""
                                    + grammaticalCase
                                    + "\", "
                                    + "\""
                                    + actual
                                    + "\"},");
                }
            }
        }
    }

    public void TestMinimalPairExamples() {
        String[][] tests = {
            {
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"one\"]",
                "〖❬1❭ Tag〗〖❌  ❬2❭ Tag〗"
            },
            {
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"other\"]",
                "〖❬2❭ Tage〗〖❌  ❬1❭ Tage〗"
            },
            {
                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"accusative\"]",
                "〖… für ❬1 metrische Pint❭ …〗〖❌  … für ❬1 metrischen Pint❭ …〗"
            },
            {
                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"dative\"]",
                "〖… mit ❬1 metrischen Pint❭ …〗〖❌  … mit ❬1 metrische Pint❭ …〗"
            },
            {
                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"genitive\"]",
                "〖Anstatt ❬1 metrischen Pints❭ …〗〖❌  Anstatt ❬1 metrische Pint❭ …〗"
            },
            {
                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"nominative\"]",
                "〖❬2 metrische Pints❭ kostet (kosten) € 3,50.〗〖❌  ❬1 metrische Pint❭ kostet (kosten) € 3,50.〗"
            },
            {
                "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"feminine\"]",
                "〖Die ❬Stunde❭ ist …〗〖❌  Die ❬Tag❭ ist …〗"
            },
            {
                "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"masculine\"]",
                "〖Der ❬Tag❭ ist …〗〖❌  Der ❬Stunde❭ ist …〗"
            },
            {
                "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"neuter\"]",
                "〖Das ❬Jahr❭ ist …〗〖❌  Das ❬Stunde❭ ist …〗"
            },
        };
        checkMinimalPairExamples("de", tests);

        String[][] elTests = {
            {
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"one\"]",
                "〖❬1❭ ημέρα〗〖❌  ❬2❭ ημέρα〗"
            },
            {
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"other\"]",
                "〖❬2❭ ημέρες〗〖❌  ❬1❭ ημέρες〗"
            },
            {
                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"accusative\"]",
                "〖… ανά ❬1 τόνο❭ …〗〖❌  … ανά ❬1 τόνου❭ …〗"
            },
            {
                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"genitive\"]",
                "〖… αξίας ❬1 τόνου❭ …〗〖❌  … αξίας ❬1 τόνο❭ …〗"
            },
            {
                "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"nominative\"]",
                "〖Η απόσταση είναι ❬2 τόνοι❭ …〗〖❌  Η απόσταση είναι ❬1 τόνο❭ …〗"
            },
            {
                "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"feminine\"]",
                "〖Η ❬ημέρα❭ είναι〗〖❌  Η ❬μήνας❭ είναι〗"
            },
            {
                "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"masculine\"]",
                "〖Ο ❬μήνας❭ θα είναι〗〖❌  Ο ❬ημέρα❭ θα είναι〗"
            },
            {
                "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"neuter\"]",
                "〖Το ❬λεπτό❭ ήταν〗〖❌  Το ❬ημέρα❭ ήταν〗"
            },
        };
        checkMinimalPairExamples("el", elTests);
    }

    private void checkMinimalPairExamples(final String locale, String[][] tests) {
        final CLDRFile cldrFile = info.getCLDRFile(locale, true);
        ExampleGenerator exampleGenerator = getExampleGenerator(locale);
        boolean showWorkingExamples = false;
        for (String[] row : tests) {
            String path = row[0];
            String expected = row[1];
            String value = cldrFile.getStringValue(path);
            String actualRaw = exampleGenerator.getExampleHtml(path, value);
            String actual = ExampleGenerator.simplify(actualRaw, false);
            showWorkingExamples |= !assertEquals(row[0] + ", " + row[1], expected, actual);
        }

        // If a test fails, verbose will regenerate what the code thinks they should be.
        // Review for correctness, and then replace the test cases

        if (showWorkingExamples) {
            System.out.println(
                    "## The following would satisfy the test, but check to make sure the expected values are all correct!");
            PluralInfo pluralInfo = SDI.getPlurals(PluralType.cardinal, locale);
            ArrayList<String> paths = new ArrayList<>();

            for (Count plural : pluralInfo.getCounts()) {
                paths.add(
                        "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\""
                                + plural
                                + "\"]");
            }
            GrammarInfo grammarInfo = SDI.getGrammarInfo(locale);
            for (String grammaticalValues :
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalCase,
                            GrammaticalScope.units)) {
                paths.add(
                        "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\""
                                + grammaticalValues
                                + "\"]");
            }
            for (String grammaticalValues :
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalGender,
                            GrammaticalScope.units)) {
                paths.add(
                        "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\""
                                + grammaticalValues
                                + "\"]");
            }
            for (String path : paths) {
                String value = cldrFile.getStringValue(path);
                String actualRaw = exampleGenerator.getExampleHtml(path, value);
                String actual = ExampleGenerator.simplify(actualRaw, false);
                System.out.println("{\"" + path.replace("\"", "\\\"") + "\", \"" + actual + "\"},");
            }
        }
    }

    /**
     * Test the production of minimal pair examples, to make sure we get no exceptions. If -v, then
     * generates lines for spreadsheet survey
     */
    public void TestListMinimalPairExamples() {
        Set<String> localesWithGrammar = SDI.hasGrammarInfo();
        if (isVerbose()) {
            System.out.println(
                    "\nLC\tLocale\tType\tCode\tCurrent Pattern\tVerify this is correct!\tVerify this is wrong!");
        }
        final String unused = "∅";
        List<String> pluralSheet = new ArrayList();
        for (String locale : localesWithGrammar) {
            final CLDRFile cldrFile = info.getCLDRFile(locale, true);
            ExampleGenerator exampleGenerator = getExampleGenerator(locale);

            PluralInfo pluralInfo = SDI.getPlurals(PluralType.cardinal, cldrFile.getLocaleID());
            Map<String, Pair<String, String>> paths = new LinkedHashMap<>();

            Set<Count> counts = pluralInfo.getCounts();
            if (counts.size() > 1) {
                for (Count plural : counts) {
                    paths.put(
                            "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\""
                                    + plural
                                    + "\"]",
                            Pair.of("plural", plural.toString()));
                }
            }
            GrammarInfo grammarInfo = SDI.getGrammarInfo(locale);
            Collection<String> unitCases =
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalCase,
                            GrammaticalScope.units);
            Collection<String> generalCasesRaw =
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalCase,
                            GrammaticalScope.general);
            Collection<CaseValues> generalCases =
                    generalCasesRaw.stream()
                            .map(x -> CaseValues.valueOf(x))
                            .collect(Collectors.toCollection(TreeSet::new));
            for (CaseValues unitCase0 : generalCases) {
                String unitCase = unitCase0.toString();
                paths.put(
                        (unitCases.contains(unitCase) ? "" : unused)
                                + "//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\""
                                + unitCase
                                + "\"]",
                        Pair.of("case", unitCase));
            }
            Collection<String> unitGenders =
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalGender,
                            GrammaticalScope.units);
            Collection<String> generalGenders =
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalGender,
                            GrammaticalScope.general);
            for (String unitGender : generalGenders) {
                paths.put(
                        (unitGenders.contains(unitGender) ? "" : unused)
                                + "//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\""
                                + unitGender
                                + "\"]",
                        Pair.of("gender", unitGender));
            }
            String localeName =
                    CLDRConfig.getInstance()
                            .getEnglish()
                            .nameGetter()
                            .getNameFromIdentifier(locale);
            boolean pluralOnly = true;
            if (paths.isEmpty()) {
                pluralSheet.add(
                        locale + "\t" + localeName + "\t" + "N/A" + "\t" + "N/A" + "\t" + "N/A");
            } else {
                for (Entry<String, Pair<String, String>> pathAndLabel : paths.entrySet()) {
                    String path = pathAndLabel.getKey();
                    String label = pathAndLabel.getValue().getFirst();
                    String code = pathAndLabel.getValue().getSecond();
                    if (!label.equals("plural")) {
                        pluralOnly = false;
                    }
                }
                String lastLabel = "";
                for (Entry<String, Pair<String, String>> pathAndLabel : paths.entrySet()) {
                    String path = pathAndLabel.getKey();
                    String label = pathAndLabel.getValue().getFirst();
                    String code = pathAndLabel.getValue().getSecond();
                    String pattern = "";
                    String examples = "";
                    if (!label.equals(lastLabel)) {
                        lastLabel = label;
                        if (!pluralOnly) {
                            if (isVerbose()) {
                                System.out.println();
                            }
                        }
                    }
                    if (path.startsWith(unused)) {
                        pattern = "🚫  Not used with formatted units";
                    } else {
                        pattern = cldrFile.getStringValue(path);
                        if (pattern == null) {
                            warnln(
                                    "Missing ExampleGenerator html example for "
                                            + locale
                                            + "("
                                            + localeName
                                            + "): "
                                            + path);
                            continue;
                        }
                        String actualRaw = exampleGenerator.getExampleHtml(path, pattern);
                        String actualSimplified = ExampleGenerator.simplify(actualRaw, false);
                        examples =
                                actualSimplified
                                        .replace("〗〖", "\t")
                                        .replace("〗", "")
                                        .replace("〖", "");
                        List<String> exampleList =
                                com.google.common.base.Splitter.on('\t')
                                        .trimResults()
                                        .splitToList(examples);
                        final int exampleListSize = exampleList.size();
                        switch (exampleListSize) {
                            case 2: // ok
                                break;
                            case 1:
                                warnln(
                                        "Expecting exactly 2 examples: "
                                                + exampleList
                                                + ", but got "
                                                + exampleListSize);
                                break;
                            default:
                                errln(
                                        "Expecting exactly 2 examples: "
                                                + exampleList
                                                + ", but got "
                                                + exampleListSize);
                                break;
                        }
                        StringBuilder exampleBuffer = new StringBuilder();
                        for (String exampleItem : exampleList) {
                            if (exampleItem.contains("❬null❭") || exampleItem.contains("❬n/a❭")) {
                                boolean bad = (exampleItem.contains("❌"));
                                exampleItem = "🆖  No unit available";
                                if (bad) {
                                    exampleItem = "❌  " + exampleItem;
                                }
                            }
                            if (exampleBuffer.length() != 0) {
                                exampleBuffer.append('\t');
                            }
                            exampleBuffer.append(exampleItem);
                        }
                        examples = exampleBuffer.toString();
                    }
                    String line =
                            (locale
                                    + "\t"
                                    + localeName
                                    + "\t"
                                    + label
                                    + "\t"
                                    + code
                                    + "\t"
                                    + pattern
                                    + "\t"
                                    + examples);
                    if (pluralOnly) {
                        pluralSheet.add(line);
                    } else {
                        if (isVerbose()) {
                            System.out.println(line);
                        }
                    }
                }
            }
            if (pluralOnly) {
                pluralSheet.add("");
            } else if (isVerbose()) {
                System.out.println();
            }
        }
        if (isVerbose()) {
            System.out.println("#################### Plural Only ###################");
            for (String line : pluralSheet) {
                System.out.println(line);
            }
        }
    }

    public void TestUnicodeSetExamples() {
        String[][] tests = {
            {
                "hi",
                "//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]",
                "[ॄ‌‍]",
                "〖‎🗝️ ॑ ॒ ॠ ॡ ॻ ॼ ॾ ॿ ॢ ॣ〗〖❰ZWNJ❱ ≡ Cursive non-joiner〗〖❰ZWJ❱ ≡ Cursive joiner〗〖❬internal: ❭[ॄ‌‍]〗"
            },
            // TODO: This test is too fragile. Commented out for discussion in CLDR-17608
            // {
            //     "hu",
            //     "//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]",
            //     "[qw-yàâ-èê-ìîïñòôøùûÿāăēĕīĭōŏœūŭ]",
            //     "〖‎🗝️ · ắ ằ ẵ ẳ ấ ầ ẫ ẩ ǎ a̧ ą ą́ a᷆ a᷇ ả ạ ặ ậ a̱ aː áː àː ɓ ć ĉ č ċ ď ḑ đ ḍ ḓ
            // ð ɖ ɗ ế ề ễ ể ě ẽ ė ę ę́ e᷆ e᷇ ẻ ẹ ẹ́ ẹ̀ ệ e̱ eː éː èː ǝ ǝ́ ǝ̀ ǝ̂ ǝ̌ ǝ̄ ə ə́ ə̀ ə̂ ə̌
            // ə̄ ɛ ɛ́ ɛ̀ ɛ̂ ɛ̌ ɛ̈ ɛ̃ ɛ̧ ɛ̄ ɛ᷆ ɛ᷇ ɛ̱ ɛ̱̈ ƒ ğ ĝ ǧ g̃ ġ ģ g̱ gʷ ǥ ɣ ĥ ȟ ħ ḥ ʻ ǐ ĩ İ i̧
            // į į́ i᷆ i᷇ ỉ ị i̱ iː íː ìː íj́ ı ɨ ɨ́ ɨ̀ ɨ̂ ɨ̌ ɨ̄ ɩ ɩ́ ɩ̀ ɩ̂ ĵ ǩ ķ ḵ kʷ ƙ ĺ ľ ļ ł ḷ ḽ
            // ḻ ḿ m̀ m̄ ń ǹ ň ṅ ņ n̄ ṇ ṋ ṉ ɲ ŋ ŋ́ ŋ̀ ŋ̄ ố ồ ỗ ổ ǒ õ ǫ ǫ́ o᷆ o᷇ ỏ ơ ớ ờ ỡ ở ợ ọ ọ́
            // ọ̀ ộ o̱ oː óː òː ɔ ɔ́ ɔ̀ ɔ̂ ɔ̌ ɔ̈ ɔ̃ ɔ̧ ɔ̄ ɔ᷆ ɔ᷇ ɔ̱ ŕ ř ŗ ṛ ś ŝ š ş ṣ ș ß ť ṭ ț ṱ ṯ ŧ
            // ǔ ů ũ u̧ ų u᷆ u᷇ ủ ư ứ ừ ữ ử ự ụ uː úː ùː ʉ ʉ́ ʉ̀ ʉ̂ ʉ̌ ʉ̈ ʉ̄ ʊ ʊ́ ʊ̀ ʊ̂ ṽ ʋ ẃ ẁ ŵ ẅ
            // ý ỳ ŷ ỹ ỷ ỵ y̱ ƴ ź ž ż ẓ ʒ ǯ þ ʔ ˀ ʼ ꞌ ǀ ǁ ǂ ǃ〗〖❬internal:
            // ❭[qw-yàâ-èê-ìîïñòôøùûÿāăēĕīĭōŏœūŭ]〗"
            // },
            {
                "de",
                "//ldml/characters/parseLenients[@scope=\"date\"][@level=\"lenient\"]/parseLenient[@sample=\"-\"]",
                "[\\u200B \\- . ๎ ็]",
                "〖‎➕ ❰ALB❱ ๎ ็〗〖‎➖ ❰NBHY❱ /〗〖❰ALB❱ ≡ Allow line break〗〖❬internal: ❭[\\-.็๎​]〗"
            },
            {
                "de",
                "//ldml/characters/exemplarCharacters",
                "[\\u200B a-z ๎ ็]",
                "〖‎➕ ❰ALB❱ ๎ ็〗〖‎➖ ä ö ß ü〗〖❰ALB❱ ≡ Allow line break〗〖❬internal: ❭[a-z็๎​]〗"
            },
            {"de", "//ldml/characters/exemplarCharacters", "a-z ❰ZWSP❱", null},
        };

        for (String[] test : tests) {
            final String locale = test[0];
            final String path = test[1];
            final String value = test[2];
            final String expected = test[3];
            ExampleGenerator exampleGenerator = getExampleGenerator(locale);
            String actual =
                    ExampleGenerator.simplify( //
                            exampleGenerator.getExampleHtml(path, value));
            assertEquals(locale + path + "=" + value, expected, actual);
        }
    }

    public void TestEraFormats() {
        ExampleGenerator exampleGeneratorJa = getExampleGenerator("ja");
        ExampleGenerator exampleGeneratorEs = getExampleGenerator("es");
        ExampleGenerator exampleGeneratorZh = getExampleGenerator("zh");
        checkValue(
                "japanese type=235 abbreviated",
                "〖平成31年〗",
                exampleGeneratorJa,
                "//ldml/dates/calendars/calendar[@type=\"japanese\"]/eras/eraAbbr/era[@type=\"235\"]");
        checkValue(
                "gregorian type=0 wide",
                "〖1 antes de Cristo〗",
                exampleGeneratorEs,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"0\"]");
        checkValue(
                "gregorian type=0-variant wide",
                "〖1 antes de la era común〗",
                exampleGeneratorEs,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/eras/eraNames/era[@type=\"0\"][@alt=\"variant\"]");
        checkValue(
                "roc type=1 abbreviated",
                "〖民国91年〗",
                exampleGeneratorZh,
                "//ldml/dates/calendars/calendar[@type=\"roc\"]/eras/eraAbbr/era[@type=\"1\"]");
    }

    public void TestQuarterFormats() {
        ExampleGenerator exampleGenerator = getExampleGenerator("ti");
        checkValue(
                "ti Q2 format wide",
                "〖2ይ ርብዒ 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"2\"]");
        checkValue(
                "ti Q2 format abbreviated",
                "〖ር2 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"abbreviated\"]/quarter[@type=\"2\"]");
        checkValue(
                "ti Q4 stand-alone wide",
                "〖4ይ ርብዒ 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"stand-alone\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"4\"]");
        checkValue(
                "ti Q4 stand-alone abbreviated",
                "〖ር4 1999〗",
                exampleGenerator,
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"stand-alone\"]/quarterWidth[@type=\"abbreviated\"]/quarter[@type=\"4\"]");
    }

    public void TestRelative() {
        ExampleGenerator exampleGeneratorIt = getExampleGenerator("it");
        ExampleGenerator exampleGeneratorAm = getExampleGenerator("am");
        ExampleGenerator exampleGeneratorCs = getExampleGenerator("cs");
        ExampleGenerator exampleGeneratorLv = getExampleGenerator("lv");
        checkValue(
                "it relative day type 2",
                "〖Set letter case for top example:〗〖5 settembre (dopodomani)〗〖dopodomani (5 settembre)〗〖See letter case instructions at right.〗",
                exampleGeneratorIt,
                "//ldml/dates/fields/field[@type=\"day\"]/relative[@type=\"2\"]");
        checkValue(
                "it relative hour future-other",
                "〖Set letter case for top example:〗〖18:25 (tra ❬100❭ ore)〗〖tra ❬100❭ ore (18:25)〗〖See letter case instructions at right.〗",
                exampleGeneratorIt,
                "//ldml/dates/fields/field[@type=\"hour\"]/relativeTime[@type=\"future\"]/relativeTimePattern[@count=\"other\"]");
        checkValue(
                "it relative year past-one",
                "〖Set letter case for top example:〗〖settembre 1999 (❬1❭ anno fa)〗〖❬1❭ anno fa (settembre 1999)〗〖See letter case instructions at right.〗",
                exampleGeneratorIt,
                "//ldml/dates/fields/field[@type=\"year\"]/relativeTime[@type=\"past\"]/relativeTimePattern[@count=\"one\"]");
        checkValue(
                "am relative month future-one",
                "〖Set letter case for top example:〗〖ሴፕቴምበር 1999 (በ❬1❭ ወር ውስጥ)〗〖በ❬1❭ ወር ውስጥ (ሴፕቴምበር 1999)〗〖See letter case instructions at right.〗",
                exampleGeneratorAm,
                "//ldml/dates/fields/field[@type=\"month\"]/relativeTime[@type=\"future\"]/relativeTimePattern[@count=\"one\"]");
        checkValue(
                "am relative month future-other",
                "〖Set letter case for top example:〗〖ሴፕቴምበር 1999 (በ❬100❭ ወራት ውስጥ)〗〖በ❬100❭ ወራት ውስጥ (ሴፕቴምበር 1999)〗〖See letter case instructions at right.〗",
                exampleGeneratorAm,
                "//ldml/dates/fields/field[@type=\"month\"]/relativeTime[@type=\"future\"]/relativeTimePattern[@count=\"other\"]");
        checkValue(
                "cs relative hour past-many",
                "〖Set letter case for top example:〗〖18:25 (před ❬0,5❭ hodiny)〗〖Před ❬0,5❭ hodiny (18:25)〗〖See letter case instructions at right.〗",
                exampleGeneratorCs,
                "//ldml/dates/fields/field[@type=\"hour\"]/relativeTime[@type=\"past\"]/relativeTimePattern[@count=\"many\"]");
        checkValue(
                "lv relative month future-other",
                "〖Set letter case for top example:〗〖1999. g. septembris (pēc ❬22❭ mēnešiem)〗〖pēc ❬22❭ mēnešiem (1999. g. septembris)〗〖See letter case instructions at right.〗",
                exampleGeneratorLv,
                "//ldml/dates/fields/field[@type=\"month\"]/relativeTime[@type=\"future\"]/relativeTimePattern[@count=\"other\"]");
    }

    static final class MissingKey implements Comparable<MissingKey> {
        final SectionId sectionId;
        final PageId pageId;
        final String starred;

        public MissingKey(SectionId sectionId, PageId pageId, String starred) {
            this.sectionId = sectionId;
            this.pageId = pageId;
            this.starred = starred;
        }

        @Override
        public int compareTo(MissingKey o) {
            return ComparisonChain.start()
                    .compare(sectionId, o.sectionId)
                    .compare(pageId, o.pageId)
                    .compare(starred, o.starred)
                    .result();
        }

        @Override
        public boolean equals(Object obj) {
            return compareTo((MissingKey) obj) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(sectionId, pageId, starred);
        }
    }

    public void TestForMissing() {

        // IF this fails for items that don't need examples, look at HANDLE_MISSING

        Factory factory = info.getCldrFactory(); // don't worry about examples for annotations
        DtdData dtdData = DtdData.getInstance(DtdType.ldml);
        PathHeader.Factory phf = PathHeader.getFactory();
        Set<String> seenPaths =
                new HashSet<>(); // assume whether there is an example is independent of locale, to
        // speed up the test.
        final String separator = "•";

        // Setup for calling phase.getShowRowAction
        DummyPathValueInfo dummyPathValueInfo = null;
        VoterInfo dummyVoterInfo = null;
        UserInfo dummyUserInfo = null;

        // disabled, since it doesn't eliminate anything. However, left under a flag just in case it
        // is useful later
        if (CHECK_ROW_ACTION) {
            dummyPathValueInfo = new DummyPathValueInfo();
            dummyVoterInfo =
                    new VoterInfo(
                            Organization.cldr,
                            org.unicode.cldr.util.VoteResolver.Level.vetter,
                            "somename");
            dummyUserInfo =
                    new UserInfo() {
                        @Override
                        public VoterInfo getVoterInfo() {
                            return dummyVoterInfo;
                        }
                    };
        }

        // Use representative locales:
        // 'en' for the most coverage,
        // 'de' and 'cs' for more complex inflections,
        // 'ja' for CJK issues

        for (String localeId : List.of("de", "en", "cs", "ja")) {
            CLDRFile cldrFile = factory.make(localeId, true);
            CLDRFile cldrFileUnresolved = cldrFile.getUnresolved();

            ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile, info.getEnglish());
            if (CHECK_ROW_ACTION) {
                dummyPathValueInfo.setLocale(CLDRLocale.getInstance(localeId));
            }
            // for collecting data

            Counter<MissingKey> countWithExamples = new Counter<>();
            Map<MissingKey, String> samplesForWith = new HashMap<>();
            Counter<MissingKey> countWithoutExamples = new Counter<>();
            Multimap<MissingKey, String> samplesForWithout = TreeMultimap.create();
            Map<MissingKey, String> sampleUrlForWithout = new TreeMap<>();
            TreeMultimap<String, String> skipped = TreeMultimap.create();

            // for each path in the file, check that there is an example
            // or we know why not

            for (String xpath : cldrFile.fullIterable()) {
                if (seenPaths.contains(xpath)) {
                    continue;
                }
                seenPaths.add(xpath);
                if (xpath.endsWith("/alias")) {
                    continue;
                }
                String value = cldrFile.getStringValue(xpath);
                if (value == null) {
                    continue;
                }

                final XPathParts parts = XPathParts.getFrozenInstance(xpath);
                if (dtdData.isDeprecated(parts)) {
                    continue;
                }
                Level level = SDI.getCoverageLevel(xpath, "en");
                if (level.isAtLeast(Level.COMPREHENSIVE)) {
                    continue;
                }
                String starred = PathStarrer.getWithPattern(xpath, PathStarrer.SIMPLE_STAR_PATTERN);
                String attrs =
                        Joiner.on(separator)
                                .join(XPathParts.getFrozenInstance(xpath).getAttributeValues());
                PathHeader ph = phf.fromPath(xpath);
                if (CHECK_ROW_ACTION) {
                    dummyPathValueInfo.setXpath(xpath);
                    dummyPathValueInfo.setBaselineValue(cldrFileUnresolved.getStringValue(xpath));
                    StatusAction action =
                            Phase.SUBMISSION.getShowRowAction(
                                    dummyPathValueInfo, InputMethod.DIRECT, ph, dummyUserInfo);
                    if (action.isForbidden()) {
                        System.out.println(xpath + " is forbidden");
                        continue;
                    }
                }

                MissingKey key = new MissingKey(ph.getSectionId(), ph.getPageId(), starred);
                String example = null;
                try {
                    example = exampleGenerator.getExampleHtml(xpath, value);
                } catch (Exception e) {
                }
                if (example == null) {
                    String missingResult = getResult(starred, attrs);
                    if (missingResult != null) {
                        skipped.put(missingResult, starred);
                        continue;
                    }

                    samplesForWithout.put(key, sampleAttrAndValue(attrs, value));
                    sampleUrlForWithout.put(key, ph.getUrl(BaseUrl.PRODUCTION, localeId));
                    countWithoutExamples.add(key, 1);
                } else {
                    if (!samplesForWith.containsKey(key)) {
                        samplesForWith.put(key, sampleAttrAndValue(attrs, value));
                    }
                    countWithExamples.add(key, 1);
                }
            }
            Set<MissingKey> keys = new TreeSet<>();
            keys.addAll(countWithoutExamples.keySet());
            keys.addAll(countWithExamples.keySet());
            List<String> missingItems = new ArrayList<>();

            // we use the missing keys, which sort by section, page, path

            for (MissingKey key : keys) {
                final long countWithout = countWithoutExamples.get(key);
                if (countWithout == 0) { // ok, no missing
                    continue;
                }
                final Collection<String> sampleForWithoutItem = samplesForWithout.get(key);
                final String sampleForWithItem = samplesForWith.get(key);
                final long countWith = countWithExamples.get(key);
                final double doneRatio = countWith / (double) (countWith + countWithout);
                missingItems.add(
                        TAB_JOINER.join(
                                doneRatio,
                                countWithout,
                                (sampleForWithItem == null
                                        ? sampleForWithoutItem.iterator().next()
                                        : Joiner.on("; ")
                                                .join(Iterables.limit(sampleForWithoutItem, 5))),
                                sampleUrlForWithout.get(key),
                                countWith,
                                (sampleForWithItem == null ? "n/a" : sampleForWithItem),
                                key.sectionId,
                                key.pageId,
                                key.starred));
            }

            // show all the skipped items, and logKnownIssue items

            for (Entry<String, Collection<String>> entry : skipped.asMap().entrySet()) {
                final String entryComment = entry.getKey();
                final String paths = CR_TAB2_JOINER.join(entry.getValue());
                if (entryComment.equals(SKIP)) {
                    logln(entryComment + ";\n\t\t" + paths);
                } else {
                    int spacePos = entryComment.indexOf(' ');
                    String ticketId = entryComment.substring(0, spacePos);
                    String ticketMessage = entryComment.substring(spacePos + 1);
                    String message =
                            ticketMessage + ")\n\t\t(For the following paths:\n\t\t" + paths;
                    if (!logKnownIssue(ticketId, message)) {
                        errln(message + " (known issue " + ticketId + ")");
                    }
                }
            }

            if (!missingItems.isEmpty()) {
                // Here is where missing examples will show up.
                // If it is ok to skip them (ONLY WHEN THERE IS NO REASONABLE EXAMPLE),
                // add to HANDLE_MISSING data
                // Otherwise add an example.
                errln(
                        TAB_JOINER.join(localeId, "missing examples:", missingItems.size())
                                + "\n"
                                + "\nDone?\tWithout\tSample Attrs\tURL\tWith\tSample Attrs\tSection\tPage\tStarred Pattern\n"
                                + Joiner.on("\n").join(missingItems));
            }
        }
    }

    public void testLightSpeed() {
        String[][] tests = {
            {
                "cs",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-light-speed\"]/unitPattern[@count=\"one\"]",
                "{0} světlo",
                "〖Used as a fallback in the following:〗〖❬1❭ světlo⋅sekunda〗〖❬1❭ světlo⋅minuta〗〖❬1❭ světlo⋅hodina〗〖❬1❭ světlo⋅den〗〖❬1❭ světlo⋅týden〗〖❬1❭ světlo⋅měsíc〗〖Compare with:〗〖❬1❭ světelný rok〗"
            },
            {
                "fr",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-light-speed\"]/unitPattern[@count=\"one\"]",
                "lumière {0}",
                "〖Used as a fallback in the following:〗〖❬1,5❭ lumière-seconde〗〖❬1,5❭ lumière-minute〗〖❬1,5❭ lumière-heure〗〖❬1,5❭ lumière-jour〗〖❬1,5❭ lumière-semaine〗〖❬1,5❭ lumière-mois〗〖Compare with:〗〖❬1,5❭ année-lumière〗"
            },
            {
                "en",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-light-speed\"]/unitPattern[@count=\"one\"]",
                "{0} LIGHT",
                "〖Used as a fallback in the following:〗〖❬1❭ LIGHT-second〗〖❬1❭ LIGHT-minute〗〖❬1❭ LIGHT-hour〗〖❬1❭ LIGHT-day〗〖❬1❭ LIGHT-week〗〖❬1❭ LIGHT-month〗〖Compare with:〗〖❬1❭ light year〗"
            },
            {
                "nl",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-light-speed\"]/unitPattern[@count=\"one\"]",
                "{0} licht",
                "〖Used as a fallback in the following:〗〖❬1❭ licht⋅seconde〗〖❬1❭ licht⋅minuut〗〖❬1❭ licht⋅uur〗〖❬1❭ licht⋅dag〗〖❬1❭ licht⋅week〗〖❬1❭ licht⋅maand〗〖Compare with:〗〖❬1❭ lichtjaar〗"
            },
            {
                "am",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-light-speed\"]/unitPattern[@count=\"other\"]",
                "{0} ብርሃን",
                "〖Used as a fallback in the following:〗〖❬2.6❭ ብርሃን⋅ሰከንዶች〗〖❬2.6❭ ብርሃን⋅ደቂቃዎች〗〖❬2.6❭ ብርሃን⋅ሰዓቶች〗〖❬2.6❭ ብርሃን⋅ቀናት〗〖❬2.6❭ ብርሃን⋅ሳምንታት〗〖❬2.6❭ ብርሃን⋅ወራት〗〖Compare with:〗〖❬2.6❭ የብርሃን ዓመት〗"
            },
        };
        String lastLocale = "";
        CLDRFile baseCldrFile = null;
        Map<String, String> map;
        ExampleGenerator exampleGenerator;

        for (String[] test : tests) {
            String locale = test[0];
            String path = test[1];
            String value = test[2];
            String expected = test[3];
            if (!locale.equals(lastLocale)) {
                baseCldrFile = info.getCldrFactory().make(locale, true);
                lastLocale = locale;
            }
            // reset the locale
            if (value
                    == null) { // Note that we can start with a null value, then replace it with the
                // current actual value, for stability in the future.
                value = baseCldrFile.getStringValue(path);
                exampleGenerator = new ExampleGenerator(baseCldrFile);
            } else {
                map = ImmutableMap.of(path, value);
                exampleGenerator = new ExampleGenerator(new CLDRFileOverride(baseCldrFile, map));
            }
            String actual = ExampleGenerator.simplify(exampleGenerator.getExampleHtml(path, value));
            assertEquals(locale + " " + path + " " + value, expected, actual);
        }
    }

    /**
     * This is a mechanism for TestMissing exceptions: a) skipping the items where there are no
     * reasonable examples b) logging known issues where we know what to do, and have filed tickets
     *
     * <p>Then only new missing examples will trigger errors.
     *
     * <p>If new structure is added, an example should be added at the same time if there is a
     * reasonable example, otherwise it should be added with "OK".
     */
    static final Map<String, Map<String, String>> HANDLE_MISSING;

    static {
        // The format is 3 items
        // a) a list of paths (separated by space or just concatenated)
        // b) a return value. OK to just skip, otherwise <ticket><space><comment>
        // c) a list of 1 or more attributes (like "mul", "zxx") or a wildcard "*"

        String[][] data = {
            // mul➔«Multiple languages»; zxx➔«No linguistic content»
            {SKIP, "//ldml/localeDisplayNames/languages/language[@type=\"*\"]", "mul", "zxx"},
            {SKIP, "//ldml/numbers/rationalFormats[@numberSystem=\"*\"]/rationalUsage", "*"},
            {
                SKIP,
                "//ldml/characters/moreInformation"
                        + "//ldml/characters/nestedBracketReplacement[@bracket=\"*\"]"
                        + "//ldml/dates/fields/field[@type=\"*\"]/relative[@type=\"*\"]"
                        + "//ldml/dates/timeZoneNames/gmtUnknownFormat"
                        + "//ldml/dates/timeZoneNames/gmtUnknownFormat[@alt=\"*\"]" // TODO
                        // CLDR-14121
                        + "//ldml/dates/timeZoneNames/metazone[@type=\"*\"]/short/standard"
                        + "//ldml/numbers/symbols[@numberSystem=\"*\"]/infinity"
                        + "//ldml/numbers/symbols[@numberSystem=\"*\"]/nan"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/eras/eraAbbr/era[@type=\"*\"][@alt=\"*\"]"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/eras/eraNames/era[@type=\"*\"][@alt=\"*\"]"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/eras/eraNames/era[@type=\"*\"][@alt=\"*\"]"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/dateTimeFormats/numericSeparators/numericDateSeparator"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/dateTimeFormats/numericSeparators/numericTimeSeparator"
                        + "//ldml/typographicNames/styleName[@type=\"*\"][@subtype=\"*\"][@alt=\"*\"]",
                "*"
            },
            //            {
            //                "CLDR-17945 Add examples of date intervals",
            //
            // "//ldml/dates/calendars/calendar[@type=\"*\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"*\"]/greatestDifference[@id=\"*\"]",
            //                "*"
            //            },
            {
                "CLDR-17945 Show \"{0} ¤¤\" with formatted number and ISO code, eg {0} ¤¤ becomes 3,5 EUR",
                "//ldml/numbers/currencyFormats[@numberSystem=\"*\"]/currencyPatternAppendISO",
                "*"
            },
            {
                "CLDR-17945 Show 2 currencies with pattern, eg EUR ➔ USD",
                "//ldml/numbers/currencies/currency[@type=\"*\"]/displayName",
                "*"
            },
            {
                "CLDR-17945 Show as part of a locale name",
                "//ldml/localeDisplayNames/keys/key[@type=\"*\"]"
                        + "//ldml/localeDisplayNames/measurementSystemNames/measurementSystemName[@type=\"*\"]"
                        + "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"*\"]"
                        + "//ldml/localeDisplayNames/types/type[@key=\"*\"][@type=\"*\"]"
                        + "//ldml/localeDisplayNames/types/type[@key=\"*\"][@type=\"*\"][@alt=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show using two months, eg Januar - Juni",
                "//ldml/dates/calendars/calendar[@type=\"*\"]/dateTimeFormats/intervalFormats/intervalFormatFallback",
                "*"
            },
            {
                "CLDR-15078 Enable compound unit formatting",
                "//ldml/units/unitLength[@type=\"*\"]/unit[@type=\"*\"]/unitPattern[@count=\"*\"]"
                        + "//ldml/units/unitLength[@type=\"*\"]/unit[@type=\"*\"]/unitPattern[@count=\"*\"][@case=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show font with field, eg: Helvetica (kursiv), Helvetica (Kursivstellung), Helvetica (vertikale Brüch)",
                "//ldml/typographicNames/styleName[@type=\"*\"][@subtype=\"*\"]"
                        + "//ldml/typographicNames/axisName[@type=\"*\"]"
                        + "//ldml/typographicNames/featureName[@type=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show in date with both variants: formatting and standalone. That way people can see what difference it makes, eg between MMMM and LLLL",
                "//ldml/dates/calendars/calendar[@type=\"*\"]/days/dayContext[@type=\"*\"]/dayWidth[@type=\"*\"]/day[@type=\"*\"]"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/months/monthContext[@type=\"*\"]/monthWidth[@type=\"*\"]/month[@type=\"*\"]"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/months/monthContext[@type=\"*\"]/monthWidth[@type=\"*\"]/month[@type=\"*\"][@yeartype=\"*\"]"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/quarters/quarterContext[@type=\"*\"]/quarterWidth[@type=\"*\"]/quarter[@type=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show pattern with example",
                "//ldml/dates/fields/field[@type=\"*\"]/relativePeriod",
                "*"
            },
            {
                "CLDR-17945 Show sample name with 2 different values",
                "//ldml/personNames/foreignSpaceReplacement"
                        + "//ldml/personNames/initialPattern[@type=\"*\"]"
                        + "//ldml/personNames/nativeSpaceReplacement"
                        + "//ldml/personNames/parameterDefault[@parameter=\"*\"]"
                        + "//ldml/personNames/sampleName[@item=\"*\"]/nameField[@type=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show two units with pattern, eg 'Meter ➔ Fuß'",
                "//ldml/units/unitLength[@type=\"*\"]/unit[@type=\"*\"]/displayName",
                "*"
            },
            {
                "CLDR-17945 Show with {0}: {0}, eg Monat: Januar",
                "//ldml/dates/fields/field[@type=\"*\"]/displayName",
                "*"
            },
            {
                "CLDR-5854 Show with appropriate amount, eg 'in 3 Jahren', and for all relatives > 1 day, add a time",
                "//ldml/dates/fields/field[@type=\"*\"]/relativeTime[@type=\"*\"]/relativeTimePattern[@count=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show with formattted date, including era",
                "//ldml/dates/calendars/calendar[@type=\"*\"]/eras/eraAbbr/era[@type=\"*\"]\n"
                        + "//ldml/dates/calendars/calendar[@type=\"*\"]/eras/eraNames/era[@type=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show with pattern, eg '30° Süd'",
                "//ldml/units/unitLength[@type=\"*\"]/coordinateUnit/coordinateUnitPattern[@type=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Show with pattern, eg Richtung: 30° Süd",
                "//ldml/units/unitLength[@type=\"*\"]/coordinateUnit/displayName",
                "*"
            },
            {
                "CLDR-17945 Show with sample characters (where possible, emoji)",
                "//ldml/characterLabels/characterLabelPattern[@type=\"*\"][@count=\"*\"]\n"
                        + "//ldml/characterLabels/characterLabel[@type=\"*\"]\n"
                        + "//ldml/characterLabels/characterLabelPattern[@type=\"*\"]",
                "*"
            },
            {
                "CLDR-17945 Use gender minimal pair patterns to show in context — look at the minimal pair examples, reversing the background",
                "//ldml/units/unitLength[@type=\"*\"]/unit[@type=\"*\"]/gender",
                "*"
            }
        };
        Map<String, Map<String, String>> _HANDLE_MISSING = new TreeMap<>();
        for (String[] row : data) {
            if (row.length < 3) {
                throw new IllegalArgumentException(
                        "Need 3+ values; see comments below HANDLE_MISSING");
            }
            String result = row[0];
            String paths = row[1];
            for (String path : SLASH2_SPLITTER.split(paths)) {
                path = "//" + path;
                // note, the resulting attributeToResult may be empty
                Map<String, String> attributeToResult = _HANDLE_MISSING.get(path);
                if (attributeToResult == null) {
                    _HANDLE_MISSING.put(path, attributeToResult = new TreeMap<>());
                }
                for (int i = 2; i < row.length; ++i) {
                    String attribute = row[i];
                    attributeToResult.put(attribute, result);
                }
            }
        }
        HANDLE_MISSING = CldrUtility.protectCollection(_HANDLE_MISSING);
    }

    private String getResult(String starredPath, String attr) {
        Map<String, String> attributeToResult = HANDLE_MISSING.get(starredPath);
        if (attributeToResult == null) {
            return null;
        }
        String result = attributeToResult.get(attr);
        if (result == null) {
            result = attributeToResult.get("*"); // wildcard
        }
        return result;
    }

    private String sampleAttrAndValue(String attrs, String value) {
        return attrs + "➔«" + value + "»";
    }

    public void testRationals() {

        //        <rationalPattern>{0}⁄{1}</rationalPattern>
        //        <integerAndRationalPattern>{0} {1}</integerAndRationalPattern>
        //        <integerAndRationalPattern alt="superSub">{0}​{1}</integerAndRationalPattern>
        //        <rationalUsage>used</rationalUsage> <!-- unknown vs unused vs used -->

        String[][] tests = {
            {
                "en",
                "//ldml/numbers/rationalFormats[@numberSystem=\"latn\"]/rationalPattern",
                "〖❬1❭❰ZWNJ❱⁄❰ZWNJ❱❬2❭〗〖❬1❭⁄❬2❭〗〖❬<sup>1</sup>❭⁄❬<sub>2</sub>❭〗〖❬¹❭⁄❬₂❭〗"
            },
            {
                "en",
                "//ldml/numbers/rationalFormats[@numberSystem=\"latn\"]/integerAndRationalPattern",
                "〖❬3❭❰NBTSP❱❬1❰ZWNJ❱⁄❰ZWNJ❱2❭〗〖❬3❭❰NBTSP❱❬½❭〗〖❬3❭❰NBTSP❱❬<sup>1</sup>⁄<sub>2</sub>❭〗"
            },
            {
                "en",
                "//ldml/numbers/rationalFormats[@numberSystem=\"latn\"]/integerAndRationalPattern[@alt=\"superSub\"]",
                "〖❬3❭❰NB❱❬½❭〗〖❬3❭❰NB❱❬<sup>1</sup>⁄<sub>2</sub>❭〗"
            },
            {"en", "//ldml/numbers/rationalFormats[@numberSystem=\"latn\"]/rationalUsage", null},
            {
                "hi",
                "//ldml/numbers/rationalFormats[@numberSystem=\"deva\"]/rationalPattern",
                "〖❬१❭❰ZWNJ❱⁄❰ZWNJ❱❬२❭〗〖❬१❭⁄❬२❭〗〖❬<sup>१</sup>❭⁄❬<sub>२</sub>❭〗"
            },
            {
                "hi",
                "//ldml/numbers/rationalFormats[@numberSystem=\"deva\"]/integerAndRationalPattern",
                "〖❬३❭❰NBTSP❱❬१❰ZWNJ❱⁄❰ZWNJ❱२❭〗〖❬३❭❰NBTSP❱❬<sup>१</sup>⁄<sub>२</sub>❭〗"
            },
            {
                "hi",
                "//ldml/numbers/rationalFormats[@numberSystem=\"deva\"]/integerAndRationalPattern[@alt=\"superSub\"]",
                "〖❬३❭❰NB❱❬<sup>१</sup>⁄<sub>२</sub>❭〗"
            },
            {"hi", "//ldml/numbers/rationalFormats[@numberSystem=\"deva\"]/rationalUsage", null},
        };
        CLDRFile cldrFile = null;
        ExampleGenerator eg = null;
        String oldLocale = "";
        for (String[] test : tests) {
            String locale = test[0];
            if (!Objects.equal(oldLocale, locale)) {
                cldrFile = CLDRConfig.getInstance().getCldrFactory().make(locale, true);
                eg = getExampleGenerator("en");
            }
            String path = test[1];
            String expected = test[2];
            String stringValue = cldrFile.getStringValue(path);
            String exampleHtml = eg.getExampleHtml(path, stringValue);
            String actual =
                    exampleHtml == null
                            ? null
                            : CodePointEscaper.toEscaped(ExampleGenerator.simplify(exampleHtml));
            assertEquals(
                    locale + path + " " + CodePointEscaper.toEscaped(stringValue),
                    expected,
                    actual);
        }
    }

    public void TestKeyTypeScope() {
        // <keys><key type="collation">Calendar</key>
        // <types><type key="collation" type="dictionary">Dictionary Sort Order</type>
        // <types><type key="collation" type="dictionary" scope="core">Dictionary</type>
        String kpath = "//ldml/localeDisplayNames/keys/key[@type=\"collation\"]";
        String path =
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"dictionary\"]";
        String spath =
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"dictionary\"][@scope=\"core\"]";
        CLDRFile cldrFile = CLDRConfig.getInstance().getCldrFactory().make("en", true);
        ExampleGenerator eg = getExampleGenerator("en");

        cldrFile.iterator("//ldml/localeDisplayNames/types/type");
        String value = cldrFile.getStringValue(path);
        String svalue = cldrFile.getStringValue(spath);
        String actual = ExampleGenerator.simplify(eg.getExampleHtml(path, value));
        String sactual = ExampleGenerator.simplify(eg.getExampleHtml(spath, svalue));
        assertEquals("plain", null, actual);
        assertEquals(
                "scope=core",
                "〖❬Sort Order❭〗〖❬   others…❭〗〖❬   ❭Dictionary〗〖❬   …others❭〗〖❬Sort Order: ❭Dictionary〗",
                sactual);
    }

    public void testLanguageMenuAttributes() {
        String[][] tests = {
            {
                "//ldml/localeDisplayNames/languages/language[@type=\"ku\"][@menu=\"core\"]",
                "〖Kurdish❬ (Kurmanji)❭〗"
            },
            {
                "//ldml/localeDisplayNames/languages/language[@type=\"ku\"][@menu=\"extension\"]",
                "〖❬Kurdish (❭Kurmanji❬)❭〗"
            }
        };
        ExampleGenerator eg = getExampleGenerator("en");

        CLDRFile cldrFile = CLDRConfig.getInstance().getCldrFactory().make("en", true);

        for (String[] test : tests) {
            String path = test[0];
            String expected = test[1];
            String value = cldrFile.getStringValue(path);
            String exampleHtml = eg.getExampleHtml(path, value);
            String actual = ExampleGenerator.simplify(exampleHtml);
            assertEquals(path, expected, actual);
        }
    }

    public void testRelatedPathValues() {
        CLDRFile cldrFile = CLDRConfig.getInstance().getCldrFactory().make("en", true);
        Multimap<String, String> skeletons = TreeMultimap.create();
        PathHeader.Factory phf = PathHeader.getFactory();
        Map<PathHeader, String> data = new TreeMap<>();
        for (String path : cldrFile) {
            if (path.startsWith(
                            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/")
                    && !path.endsWith("/alias")
                    && !path.endsWith("/intervalFormatFallback")) {
                // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/intervalFormats/intervalFormatFallback
                XPathParts parts = XPathParts.getFrozenInstance(path);
                Set<String> values = RelatedDatePathValues.getRelatedPathValues(cldrFile, parts);
                data.put(
                        phf.fromPath(path),
                        Joiners.TAB.join(cldrFile.getStringValue(path), values, path));
                String skeleton = parts.getAttributeValue(RelatedDatePathValues.idElement, "id");
                if (skeleton == null) {
                    continue;
                }
                String element = parts.getElement(RelatedDatePathValues.dateTypeElement);
                switch (element) {
                    case "availableFormats":
                    case "intervalFormats":
                        skeletons.put(skeleton, element);
                    default:
                        break;
                }
            }
        }
        if (isVerbose()) {
            System.out.println();
            for (Entry<PathHeader, String> entry : data.entrySet()) {
                System.out.println(entry.getValue());
            }
            System.out.println();
            for (Entry<String, Collection<String>> entry : skeletons.asMap().entrySet()) {
                System.out.println(
                        Joiners.TAB.join(
                                entry.getKey(),
                                entry.getValue().contains("availableFormats"),
                                entry.getValue().contains("intervalFormats")));
            }
        }
    }

    public void testIntervalFormats() {
        String[][] tests = {
            {"h – h B", "h|[h]| – |h B|[h,  , B]", "12 – 1 in the afternoon"},
            {"E H – H v", "E H|[E,  , H]| – |H v|[H,  , v]", "Wed 12 – 13 GMT"},
            {"MdM", "Missing literal between first and second formats in «MdM»"},
            {"Md", "Interval patterns must have two parts, with a separator between: «Md»"}
        };
        final CLDRLocale loc = CLDRLocale.getInstance("en");
        final ICUServiceBuilder isb = ICUServiceBuilder.forLocale(loc);
        Date DATE1 = Date.from(Instant.parse("2025-01-01T12:00:00Z"));
        Date DATE2 = Date.from(Instant.parse("2025-01-01T13:00:00Z"));

        for (String[] test : tests) {
            String source = test[0];
            String expected = test[1];
            String expected2 = test.length <= 2 ? null : test[2];
            CldrIntervalFormat intf = null;
            String actual;
            try {
                intf = CldrIntervalFormat.getInstance("gregorian", source);
                actual =
                        Joiners.VBAR.join(
                                intf.firstPattern,
                                intf.firstFields,
                                intf.separator,
                                intf.secondPattern,
                                intf.secondFields);
                String actual2 = intf.format(DATE1, DATE2, isb, TimeZone.GMT_ZONE);
                assertEquals(Joiners.COMMA_SP.join(source, DATE1, DATE2), expected2, actual2);
            } catch (Exception e) {
                actual = e.getMessage();
            }
            assertEquals(source, expected, actual);
        }
    }

    public void testAvailableAndIntervalExamples() {
        // for now, just gregorian, just English
        String[][] tests = {

            // Available dates

            {
                "GyMMMEd",
                "〖Sun, Sep 5, 1999 AD〗〖Related formats:〗〖Sun, Sep 5, 1999〗〖Sep 5, 1999 AD〗"
            },
            {"GyMMMd", "〖Sep 5, 1999 AD〗〖Related formats:〗〖Sep 5, 1999〗"},
            {"GyMEd", "〖Sun, 9/5/1999 AD〗〖Related formats:〗〖Sun, 9/5/1999〗〖9/5/1999 AD〗"},
            {"GyMd", "〖9/5/1999 AD〗〖Related formats:〗〖9/5/1999〗"},
            {"yMMMEd", "〖Sun, Sep 5, 1999〗〖Related formats:〗〖Sep 5, 1999〗"},
            {"yMMMd", "〖Sep 5, 1999〗〖Related formats:〗〖Sep 5〗"},
            {"yMEd", "〖Sun, 9/5/1999〗〖Related formats:〗〖9/5/1999〗"},
            {"yMd", "〖9/5/1999〗〖Related formats:〗〖9/5〗"},
            {"MMMEd", "〖Sun, Sep 5〗〖Related formats:〗〖Sep 5〗"},
            {"MMMd", "〖Sep 5〗"},
            {"MEd", "〖Sun, 9/5〗〖Related formats:〗〖9/5〗"},
            {"Md", "〖9/5〗"},

            // Available times

            {"Hmv", "〖13:25 EST〗〖03:25 EST〗〖Related formats:〗〖13:25〗"},
            {"Hv", "〖13 EST〗〖03 EST〗"},
            {"Eh", "〖Sun 1 PM〗〖Sun 3 AM〗"},
            {"Ehm", "〖Sun 1:25 PM〗〖Sun 3:25 AM〗〖Related formats:〗〖1:25 PM〗"},
            {"EHm", "〖Sun 13:25〗〖Sun 03:25〗〖Related formats:〗〖13:25〗"},
            {"Ehms", "〖Sun 1:25:59 PM〗〖Sun 3:25:59 AM〗〖Related formats:〗〖1:25:59 PM〗"},
            {"EHms", "〖Sun 13:25:59〗〖Sun 03:25:59〗〖Related formats:〗〖13:25:59〗"},

            // Intervals
            {
                "GyMMMd/y",
                "〖Nov 13, 2008 – Dec 14, 2009 AD〗〖Feb 3, 2008 – Mar 4, 2009 AD〗〖Related Flexible Dates:〗〖Nov 13, 2008 AD〗〖Feb 3, 2008 AD〗〖Nov 13, 2008〗〖Feb 3, 2008〗"
            },
            {
                "GyMMMd/M",
                "〖Nov 13 – Dec 14, 2008 AD〗〖Feb 3 – Mar 4, 2008 AD〗〖Related Flexible Dates:〗〖Nov 13, 2008 AD〗〖Feb 3, 2008 AD〗〖Nov 13〗〖Feb 3〗"
            },
            {
                "GyMMMd/d",
                "〖Nov 13 – 14, 2008 AD〗〖Feb 3 – 4, 2008 AD〗〖Related Flexible Dates:〗〖Nov 13, 2008 AD〗〖Feb 3, 2008 AD〗〖Nov 13〗〖Feb 3〗"
            },
            {
                "GyMd/y",
                "〖11/13/2008 – 12/14/2009 AD〗〖2/3/2008 – 3/4/2009 AD〗〖Related Flexible Dates:〗〖11/13/2008 AD〗〖2/3/2008 AD〗〖11/13/2008〗〖2/3/2008〗"
            },
            {
                "GyMd/M",
                "〖11/13/2008 – 12/14/2008 AD〗〖2/3/2008 – 3/4/2008 AD〗〖Related Flexible Dates:〗〖11/13/2008 AD〗〖2/3/2008 AD〗〖11/13/2008〗〖2/3/2008〗"
            },
            {
                "GyMd/d",
                "〖11/13/2008 – 11/14/2008 AD〗〖2/3/2008 – 2/4/2008 AD〗〖Related Flexible Dates:〗〖11/13/2008 AD〗〖2/3/2008 AD〗〖11/13/2008〗〖2/3/2008〗"
            },
            {
                "Hmv/H",
                "〖05:07 – 06:26 GMT〗〖05:07 – 06:07 GMT〗〖Related Flexible Dates:〗〖05:07 GMT〗〖05:07〗"
            },
            {
                "Hmv/m",
                "〖05:07 – 05:26 GMT〗〖05:07 – 05:07 GMT〗〖Related Flexible Dates:〗〖05:07 GMT〗〖05:07〗"
            },
        };
        ExampleGenerator eg = getExampleGenerator("en");
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        XPathParts intervalParts =
                XPathParts.getFrozenInstance(
                                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"Bhm\"]/greatestDifference[@id=\"B\"]")
                        .cloneAsThawed();
        XPathParts availableParts =
                XPathParts.getFrozenInstance(
                                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"MMMd\"]")
                        .cloneAsThawed();
        for (String[] test : tests) {
            String source = test[0];
            String expected = test[1];
            String path = null;
            int slashPos = test[0].indexOf('/');
            if (slashPos < 0) {
                availableParts.setAttribute(RelatedDatePathValues.idElement, "id", source);
                path = availableParts.toString();
            } else {
                intervalParts.setAttribute(
                        RelatedDatePathValues.idElement, "id", test[0].substring(0, slashPos));
                intervalParts.setAttribute(
                        RelatedDatePathValues.idElement + 1, "id", test[0].substring(slashPos + 1));
                path = intervalParts.toString();
            }
            String value = english.getStringValue(path);
            String actual = ExampleGenerator.simplify(eg.getExampleHtml(path, value));
            assertEquals(source, expected, actual);
        }
    }
}
