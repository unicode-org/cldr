package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.tool.PluralMinimalPairs;
import org.unicode.cldr.tool.PluralRulesFactory;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.IsoCurrencyParser;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PluralRanges;
import org.unicode.cldr.util.PreferredAndAllowedHour;
import org.unicode.cldr.util.PreferredAndAllowedHour.HourStyle;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.CodeType;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.ContainmentStyle;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyNumberInfo;
import org.unicode.cldr.util.SupplementalDataInfo.DateRange;
import org.unicode.cldr.util.SupplementalDataInfo.MetaZoneRange;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.SupplementalDataInfo.SampleList;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterEnums;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class TestSupplementalInfo extends TestFmwkPlus {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    private static final StandardCodes STANDARD_CODES = testInfo
        .getStandardCodes();

    private static final SupplementalDataInfo SUPPLEMENTAL = testInfo
        .getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestSupplementalInfo().run(args);
    }

    public void TestPluralSampleOrder() {
        HashSet<PluralInfo> seen = new HashSet<PluralInfo>();
        for (String locale : SUPPLEMENTAL.getPluralLocales()) {
            if (locale.equals("root")) {
                continue;
            }
            PluralInfo pi = SUPPLEMENTAL.getPlurals(locale);
            if (seen.contains(pi)) {
                continue;
            }
            seen.add(pi);
            for (SampleType s : SampleType.values()) {
                for (Count c : pi.getCounts(s)) {
                    FixedDecimalSamples sSamples = pi.getPluralRules()
                        .getDecimalSamples(c.toString(), s);
                    if (sSamples == null) {
                        errln(locale + " no sample for " + c);
                        continue;
                    }
                    if (s == SampleType.DECIMAL) {
                        continue; // skip
                    }
                    FixedDecimalRange lastSample = null;
                    for (FixedDecimalRange sample : sSamples.samples) {
                        if (lastSample != null) {
                            if (lastSample.start.compareTo(sample.start) > 0) {
                                errln(locale + ":" + c + ": out of order with "
                                    + lastSample + " > " + sample);
                            } else if (false) {
                                logln(locale + ":" + c + ": in order with "
                                    + lastSample + " < " + sample);
                            }
                        }
                        lastSample = sample;
                    }
                }
            }
        }
    }

    public void TestPluralRanges() {
        PluralRulesFactory prf = PluralRulesFactory.getInstance(SUPPLEMENTAL);
        Set<String> localesToTest = new TreeSet<String>(
            SUPPLEMENTAL.getPluralRangesLocales());
        for (String locale : StandardCodes.make().getLocaleCoverageLocales(
            "google")) { // superset
            if (locale.equals("*") || locale.contains("_")) {
                continue;
            }
            localesToTest.add(locale);
        }
        Set<String> modernLocales = testInfo.getStandardCodes()
            .getLocaleCoverageLocales(Organization.cldr,
                EnumSet.of(Level.MODERN));

        Output<FixedDecimal> maxSample = new Output<FixedDecimal>();
        Output<FixedDecimal> minSample = new Output<FixedDecimal>();

        for (String locale : localesToTest) {
            final String templateLine = "Template for " + ULocale.getDisplayName(locale, "en") + " (" + locale + ") translators to fix:";
            PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(locale);
            Set<Count> counts = pluralInfo.getCounts();

            final PluralMinimalPairs samplePatterns = PluralMinimalPairs.getInstance(new ULocale(locale).toString());

            // check that there are no null values
            PluralRanges pluralRanges = SUPPLEMENTAL.getPluralRanges(locale);
            if (pluralRanges == null) {
                if (!modernLocales.contains(locale)) {
                    logln("Missing plural ranges for " + locale);
                } else {
                    errOrLog(CoverageIssue.error, locale + "\tMissing plural ranges", "Cldrbug:7839", "Missing plural data for modern locales");
                    StringBuilder failureCases = new StringBuilder(templateLine);
                    for (Count start : counts) {
                        for (Count end : counts) {
                            pluralInfo.rangeExists(start, end, minSample, maxSample);
                            final String rangeLine = getRangeLine(start, end, null, maxSample, minSample, samplePatterns);
                            failureCases.append("\n" + locale + "\t" + rangeLine);
                        }
                    }
                    errOrLog(CoverageIssue.warn, failureCases.toString());
                }
                continue;
            }
            EnumSet<Count> found = EnumSet.noneOf(Count.class);
            for (Count count : Count.values()) {
                if (pluralRanges.isExplicitlySet(count)
                    && !counts.contains(count)) {
                    assertTrue(
                        locale
                            + "\t pluralRanges categories must be valid for locale:\t"
                            + count + " must be in " + counts,
                        !pluralRanges.isExplicitlySet(count));
                }
                for (Count end : Count.values()) {
                    Count result = pluralRanges.getExplicit(count, end);
                    if (result != null) {
                        found.add(result);
                    }
                }
            }

            // check empty range results
            if (found.isEmpty()) {
                errOrLog(CoverageIssue.error, "Empty range results for " + locale, "Cldrbug:7839", "Missing plural data for modern locales");
            } else {
                if (samplePatterns == null) {
                    errOrLog(CoverageIssue.error, locale + "\tMissing sample patterns", "Cldrbug:7839", "Missing plural data for modern locales");
                } else {
                    for (Count result : found) {
                        String samplePattern = samplePatterns.get(
                            PluralRules.PluralType.CARDINAL, result);
                        if (samplePattern != null && !samplePattern.contains("{0}")) {
                            errln("Plural Ranges cannot have results that don't use {0} in samples: "
                                + locale
                                + ", "
                                + result
                                + "\t«"
                                + samplePattern + "»");
                        }
                    }
                }
                if (isVerbose()) {
                    logln("Range results for " + locale + ":\t" + found);
                }
            }

            // check for missing values
            boolean failure = false;
            StringBuilder failureCases = new StringBuilder(templateLine);
            for (Count start : counts) {
                for (Count end : counts) {
                    boolean needsValue = pluralInfo.rangeExists(start, end,
                        minSample, maxSample);
                    Count explicitValue = pluralRanges.getExplicit(start, end);
                    final String rangeLine = getRangeLine(start, end, explicitValue, maxSample, minSample, samplePatterns);
                    failureCases.append("\n" + locale + "\t" + rangeLine);
                    if (needsValue && explicitValue == null) {
                        errOrLog(CoverageIssue.error, locale + "\tNo explicit value for range: "
                            + rangeLine,
                            "Cldrbug:7839", "Missing plural data for modern locales");
                        failure = true;
                        failureCases.append("\tError — need explicit result");
                    } else if (!needsValue && explicitValue != null) {
                        errOrLog(CoverageIssue.error, locale + "\tDoesn't need explicit value, but has one: "
                            + PluralRanges.showRange(start, end, explicitValue),
                            "Cldrbug:7839", "Missing plural data for modern locales");
                        failureCases.append("\tUnnecessary");
                        failure = true;
                    } else {
                        failureCases.append("\tOK");
                    }
                }
            }
            if (failure) {
                errOrLog(CoverageIssue.warn, failureCases.toString());
            }
        }
    }

    private String getRangeLine(Count start, Count end, Count result,
        Output<FixedDecimal> maxSample, Output<FixedDecimal> minSample,
        PluralMinimalPairs samplePatterns) {
        final String range = minSample + "–" + maxSample;
        String example = range;
        if (samplePatterns != null) {
            example = "";
            if (result != null) {
                String pat = samplePatterns.get(PluralRules.PluralType.CARDINAL, result);
                example += "«" + (pat == null ? "MISSING-PATTERN" : pat.replace("{0}", range)) + "»";
            } else {
                for (Count c : new TreeSet<>(Arrays.asList(start, end, Count.other))) {
                    String pat = samplePatterns.get(PluralRules.PluralType.CARDINAL, c);
                    example += c + ":«" + (pat == null ? "MISSING-PATTERN" : pat.replace("{0}", range)) + "»" + "?\tOR ";
                }
                example += " …";
            }
        }
        return start + "\t" + end + "\t" + (result == null ? "?" : result.toString()) + "\t" + example;
    }

    private String getRangeLine(Count count, PluralRules pluralRules, String pattern) {
        String sample = "?";
        FixedDecimalSamples exampleList = pluralRules.getDecimalSamples(count.toString(), PluralRules.SampleType.INTEGER);
        if (exampleList == null) {
            exampleList = pluralRules.getDecimalSamples(count.toString(), PluralRules.SampleType.DECIMAL);
        }
        FixedDecimal sampleDecimal = PluralInfo.getNonZeroSampleIfPossible(exampleList);
        sample = sampleDecimal.toString();

        String example = pattern == null ? "NO-SAMPLE!" : "«" + pattern.replace("{0}", sample) + "»";
        return count + "\t" + example;
    }

    public void TestPluralSamples() {
        String[][] test = { { "en", "ordinal", "1", "one" },
            { "en", "ordinal", "2", "two" },
            { "en", "ordinal", "3", "few" },
            { "en", "ordinal", "4", "other" },
            { "sl", "cardinal", "2", "two" }, };
        for (String[] row : test) {
            checkPluralSamples(row);
        }
    }

    public void TestPluralSamples2() {
        PluralRulesFactory prf = PluralRulesFactory.getInstance(SUPPLEMENTAL);
        for (String locale : prf.getLocales()) {
            if (locale.equals("und")) {
                continue;
            }
            if (locale.equals("pl")) {
                int debug = 0;
            }
            final PluralMinimalPairs samplePatterns = PluralMinimalPairs.getInstance(locale);
            for (PluralRules.PluralType type : PluralRules.PluralType.values()) {
                PluralInfo rules = SUPPLEMENTAL.getPlurals(
                    SupplementalDataInfo.PluralType.fromStandardType(type),
                    locale.toString());
                if (rules.getCounts().size() == 1) {
                    continue; // don't require rules for unary cases
                }
                Multimap<String, Count> sampleToCount = TreeMultimap.create();

                for (Count count : rules.getCounts()) {
                    String sample = samplePatterns.get(type, count);
                    if (sample == null) {
                        errOrLog(CoverageIssue.error, locale + "\t" + type + " \tmissing samples for " + count, "cldrbug:7075",
                            "Missing ordinal minimal pairs");
                    } else {
                        sampleToCount.put(sample, count);
                        PluralRules pRules = rules.getPluralRules();
                        double unique = pRules.getUniqueKeywordValue(count
                            .toString());
                        if (unique == PluralRules.NO_UNIQUE_VALUE
                            && !sample.contains("{0}")) {
                            errln("Missing {0} in sample: " + locale + ", " + type + ", " + count + " «" + sample + "»");
                        }
                    }
                }
                for (Entry<String, Collection<Count>> entry : sampleToCount.asMap().entrySet()) {
                    if (entry.getValue().size() > 1) {
                        errln("Colliding minimal pair samples: " + locale + ", " + type + ", " + entry.getValue() + " «" + entry.getKey() + "»");
                    }
                }
            }
        }
    }

    public void TestCldrScriptCodes() {
        Set<String> codes = SUPPLEMENTAL.getCLDRScriptCodes();

        Set<String> unicodeScripts = ScriptMetadata.getScripts();
        assertRelation("getCLDRScriptCodes contains Unicode Scripts", true, codes, CONTAINS_ALL, unicodeScripts);

        ImmutableSet<String> allSpecials = ImmutableSet.of("Zinh", "Zmth", "Zsye", "Zsym", "Zxxx", "Zyyy", "Zzzz");
        assertRelation("getCLDRScriptCodes contains allSpecials", true, codes, CONTAINS_ALL, allSpecials);

        ImmutableSet<String> allCompos = ImmutableSet.of("Hanb", "Hrkt", "Jamo", "Jpan", "Kore");
        assertRelation("getCLDRScriptCodes contains allCompos", true, codes, CONTAINS_ALL, allCompos);

        Map<Status, Set<String>> scripts = Validity.getInstance().getStatusToCodes(LstrType.script);
        for (Entry<Status, Set<String>> e : scripts.entrySet()) {
            switch (e.getKey()) {
            case regular:
            case special:
            case unknown:
                assertRelation("getCLDRScriptCodes contains " + e.getKey(), true, codes, CONTAINS_ALL, e.getValue());
                break;
            default:
                break; // do nothin
            }
        }

        ImmutableSet<String> variants = ImmutableSet.of("Aran", "Cyrs", "Geok", "Latf", "Latg", "Syre", "Syrj", "Syrn");
        assertRelation("getCLDRScriptCodes contains variants", false, codes, CONTAINS_SOME, variants);
    }

    public void checkPluralSamples(String... row) {
        PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(
            PluralType.valueOf(row[1]), row[0]);
        Count count = pluralInfo.getCount(new FixedDecimal(row[2]));
        assertEquals(CollectionUtilities.join(row, ", "),
            Count.valueOf(row[3]), count);
    }

    public void TestPluralLocales() {
        // get the unique rules
        for (PluralType type : PluralType.values()) {
            Relation<PluralInfo, String> pluralsToLocale = Relation.of(
                new HashMap<PluralInfo, Set<String>>(), TreeSet.class);
            for (String locale : new TreeSet<String>(
                SUPPLEMENTAL.getPluralLocales(type))) {
                PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(type, locale);
                pluralsToLocale.put(pluralInfo, locale);
            }

            String[][] equivalents = { { "mo", "ro" }, { "tl", "fil" },
                { "he", "iw" }, { "in", "id" }, { "jw", "jv" },
                { "ji", "yi" }, { "sh", "sr" }, };
            for (Entry<PluralInfo, Set<String>> pluralInfoEntry : pluralsToLocale
                .keyValuesSet()) {
                PluralInfo pluralInfo2 = pluralInfoEntry.getKey();
                Set<String> locales = pluralInfoEntry.getValue();
                // check that equivalent locales are either both in or both out
                for (String[] row : equivalents) {
                    assertEquals(
                        type + " must be equivalent: " + Arrays.asList(row),
                        locales.contains(row[0]), locales.contains(row[1]));
                }
                // check that no rules contain 'within'
                for (Count count : pluralInfo2.getCounts()) {
                    String rule = pluralInfo2.getRule(count);
                    if (rule == null) {
                        continue;
                    }
                    assertFalse(
                        "Rule '" + rule + "' for " + Arrays.asList(locales)
                            + " doesn't contain 'within'",
                        rule.contains("within"));
                }
            }
        }
    }

    public void TestDigitPluralCases() {
        String[][] tests = {
            { "en", "one", "1", "1" },
            { "en", "one", "2", "" },
            { "en", "one", "3", "" },
            { "en", "one", "4", "" },
            { "en", "other", "1", "0, 2-9, 0.0, 0.1, 0.2, …" },
            { "en", "other", "2", "10-99, 10.0, 10.1, 10.2, …" },
            { "en", "other", "3", "100-999, 100.0, 100.1, 100.2, …" },
            { "en", "other", "4", "1000-9999, 1000.0, 1000.1, 1000.2, …" },
            { "hr", "one", "1", "1, 0.1, 2.10, 1.1, …" },
            { "hr", "one", "2",
                "21, 31, 41, 51, 61, 71, …, 10.1, 12.10, 11.1, …" },
            { "hr", "one", "3",
                "101, 121, 131, 141, 151, 161, …, 100.1, 102.10, 101.1, …" },
            { "hr", "one", "4",
                "1001, 1021, 1031, 1041, 1051, 1061, …, 1000.1, 1002.10, 1001.1, …" },
            { "hr", "few", "1", "2-4, 0.2, 0.3, 0.4, …" },
            { "hr", "few", "2",
                "22-24, 32-34, 42-44, …, 10.2, 10.3, 10.4, …" },
            { "hr", "few", "3",
                "102-104, 122-124, 132-134, …, 100.2, 100.3, 100.4, …" },
            { "hr", "few", "4",
                "1002-1004, 1022-1024, 1032-1034, …, 1000.2, 1000.3, 1000.4, …" },
            { "hr", "other", "1", "0, 5-9, 0.0, 0.5, 0.6, …" },
            { "hr", "other", "2",
                "10-20, 25-30, 35-40, …, 10.0, 10.5, 10.6, …" },
            { "hr", "other", "3",
                "100, 105-120, 125-130, 135-140, …, 100.0, 100.5, 100.6, …" },
            { "hr", "other", "4",
                "1000, 1005-1020, 1025-1030, 1035-1040, …, 1000.0, 1000.5, 1000.6, …" }, };
        for (String[] row : tests) {
            PluralInfo plurals = SUPPLEMENTAL.getPlurals(row[0]);
            SampleList uset = plurals.getSamples9999(Count.valueOf(row[1]),
                Integer.parseInt(row[2]));
            assertEquals(row[0] + ", " + row[1] + ", " + row[2], row[3],
                uset.toString());
        }
    }

    public void TestDigitPluralCompleteness() {
        String[][] exceptionStrings = {
            // defaults
            { "*", "zero", "0,00,000,0000" }, { "*", "one", "0" },
            { "*", "two", "0,00,000,0000" },
            { "*", "few", "0,00,000,0000" },
            { "*", "many", "0,00,000,0000" },
            { "*", "other", "0,00,000,0000" },
            // others
            { "mo", "other", "00,000,0000" }, //
            { "ro", "other", "00,000,0000" }, //
            { "cs", "few", "0" }, // j in 2..4
            { "sk", "few", "0" }, // j in 2..4
            { "da", "one", "0" }, // j is 1 or t is not 0 and n within 0..2
            { "is", "one", "0,00,000,0000" }, // j is 1 or f is 1
            { "sv", "one", "0" }, // j is 1
            { "he", "two", "0" }, // j is 2
            { "ru", "one", "0,00,000,0000" }, // j mod 10 is 1 and j mod 100
            // is not 11
            { "uk", "one", "0,00,000,0000" }, // j mod 10 is 1 and j mod 100
            // is not 11
            { "bs", "one", "0,00,000,0000" }, // j mod 10 is 1 and j mod 100
            // is not 11 or f mod 10 is
            // 1 and f mod 100 is not 11
            { "hr", "one", "0,00,000,0000" }, // j mod 10 is 1 and j mod 100
            // is not 11 or f mod 10 is
            // 1 and f mod 100 is not 11
            { "sh", "one", "0,00,000,0000" }, // j mod 10 is 1 and j mod 100
            // is not 11 or f mod 10 is
            // 1 and f mod 100 is not 11
            { "sr", "one", "0,00,000,0000" }, // j mod 10 is 1 and j mod 100
            // is not 11 or f mod 10 is
            // 1 and f mod 100 is not 11
            { "mk", "one", "0,00,000,0000" }, // j mod 10 is 1 or f mod 10
            // is 1
            { "sl", "one", "0,000,0000" }, // j mod 100 is 1
            { "sl", "two", "0,000,0000" }, // j mod 100 is 2
            { "he", "many", "00,000,0000" }, // j not in 0..10 and j mod 10
            // is 0
            { "tzm", "one", "0,00" }, // n in 0..1 or n in 11..99
            { "gd", "one", "0,00" }, // n in 1,11
            { "gd", "two", "0,00" }, // n in 2,12
            { "shi", "few", "0,00" }, // n in 2..10
            { "gd", "few", "0,00" }, // n in 3..10,13..19
            { "ga", "few", "0" }, // n in 3..6
            { "ga", "many", "0,00" }, // n in 7..10
            { "ar", "zero", "0" }, // n is 0
            { "cy", "zero", "0" }, // n is 0
            { "ksh", "zero", "0" }, // n is 0
            { "lag", "zero", "0" }, // n is 0
            { "pt", "one", "0" }, // i = 1 and v = 0 or i = 0 and t = 1
            { "pt_PT", "one", "0" }, // n = 1 and v = 0
            { "ar", "two", "0" }, // n is 2
            { "cy", "two", "0" }, // n is 2
            { "ga", "two", "0" }, // n is 2
            { "iu", "two", "0" }, // n is 2
            { "kw", "two", "0" }, // n is 2
            { "naq", "two", "0" }, // n is 2
            { "se", "two", "0" }, // n is 2
            { "sma", "two", "0" }, // n is 2
            { "smi", "two", "0" }, // n is 2
            { "smj", "two", "0" }, // n is 2
            { "smn", "two", "0" }, // n is 2
            { "sms", "two", "0" }, // n is 2
            { "cy", "few", "0" }, // n is 3
            { "cy", "many", "0" }, // n is 6
            { "br", "many", "" }, // n is not 0 and n mod 1000000 is 0
            { "gv", "one", "0,00,000,0000" }, // n mod 10 is 1
            { "be", "one", "0,00,000,0000" }, // n mod 10 is 1 and n mod 100
            // is not 11
            { "lv", "one", "0,00,000,0000" }, // n mod 10 is 1 and n mod 100
            // is not 11 or v is 2 and f
            // mod 10 is 1 and f mod 100
            // is not 11 or v is not 2
            // and f mod 10 is 1
            { "br", "one", "0,00,000,0000" }, // n mod 10 is 1 and n mod 100
            // not in 11,71,91
            { "lt", "one", "0,00,000,0000" }, // n mod 10 is 1 and n mod 100
            // not in 11..19
            { "fil", "one", "0,00,000,0000" }, // v = 0 and i = 1,2,3 or v =
            // 0 and i % 10 != 4,6,9 or
            // v != 0 and f % 10 !=
            // 4,6,9
            { "tl", "one", "0,00,000,0000" }, // v = 0 and i = 1,2,3 or v =
            // 0 and i % 10 != 4,6,9 or
            // v != 0 and f % 10 !=
            // 4,6,9
            { "dsb", "one", "0,00,000,0000" }, // v = 0 and i % 100 = 1 or f
            // % 100 = 1
        };
        // parse out the exceptions
        Map<PluralInfo, Relation<Count, Integer>> exceptions = new HashMap<PluralInfo, Relation<Count, Integer>>();
        Relation<Count, Integer> fallback = Relation.of(
            new EnumMap<Count, Set<Integer>>(Count.class), TreeSet.class);
        for (String[] row : exceptionStrings) {
            Relation<Count, Integer> countToDigits;
            if (row[0].equals("*")) {
                countToDigits = fallback;
            } else {
                PluralInfo plurals = SUPPLEMENTAL.getPlurals(row[0]);
                countToDigits = exceptions.get(plurals);
                if (countToDigits == null) {
                    exceptions.put(
                        plurals,
                        countToDigits = Relation.of(
                            new EnumMap<Count, Set<Integer>>(
                                Count.class),
                            TreeSet.class));
                }
            }
            Count c = Count.valueOf(row[1]);
            for (String digit : row[2].split(",")) {
                // "99" is special, just to have the result be non-empty
                countToDigits.put(c, digit.length());
            }
        }
        Set<PluralInfo> seen = new HashSet<PluralInfo>();
        Set<String> sorted = new TreeSet<String>(
            SUPPLEMENTAL.getPluralLocales(PluralType.cardinal));
        Relation<String, String> ruleToExceptions = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);

        for (String locale : sorted) {
            PluralInfo plurals = SUPPLEMENTAL.getPlurals(locale);
            if (seen.contains(plurals)) { // skip identicals
                continue;
            }
            Relation<Count, Integer> countToDigits = exceptions.get(plurals);
            if (countToDigits == null) {
                countToDigits = fallback;
            }
            for (Count c : plurals.getCounts()) {
                List<String> compose = new ArrayList<String>();
                boolean needLine = false;
                Set<Integer> digitSet = countToDigits.get(c);
                if (digitSet == null) {
                    digitSet = fallback.get(c);
                }
                for (int digits = 1; digits < 5; ++digits) {
                    boolean expected = digitSet.contains(digits);
                    boolean hasSamples = plurals.hasSamples(c, digits);
                    if (hasSamples) {
                        compose.add(Utility.repeat("0", digits));
                    }
                    if (!assertEquals(locale + ", " + digits + ", " + c,
                        expected, hasSamples)) {
                        needLine = true;
                    }
                }
                if (needLine) {
                    String countRules = plurals.getPluralRules().getRules(
                        c.toString());
                    ruleToExceptions.put(countRules == null ? "" : countRules,
                        "{\"" + locale + "\", \"" + c + "\", \""
                            + CollectionUtilities.join(compose, ",")
                            + "\"},");
                }
            }
        }
        if (!ruleToExceptions.isEmpty()) {
            System.out
                .println("To fix the above, review the following, then replace in TestDigitPluralCompleteness");
            for (Entry<String, String> entry : ruleToExceptions.entrySet()) {
                System.out.println(entry.getValue() + "\t// " + entry.getKey());
            }
        }
    }

    public void TestLikelyCode() {
        Map<String, String> likely = SUPPLEMENTAL.getLikelySubtags();
        String[][] tests = { { "it_AQ", "it_Latn_AQ" },
            { "it_Arab", "it_Arab_IT" }, { "az_Cyrl", "az_Cyrl_AZ" }, };
        for (String[] pair : tests) {
            String newMax = LikelySubtags.maximize(pair[0], likely);
            assertEquals("Likely", pair[1], newMax);
        }

    }

    public void TestLikelySubtagCompleteness() {
        Map<String, String> likely = SUPPLEMENTAL.getLikelySubtags();

        for (String language : SUPPLEMENTAL.getCLDRLanguageCodes()) {
            if (!likely.containsKey(language)) {
                logln("WARNING: No likely subtag for CLDR language code ("
                    + language + ")");
            }
        }
        for (String script : SUPPLEMENTAL.getCLDRScriptCodes()) {
            if (!likely.containsKey("und_" + script)
                && !script.equals("Latn")
                && !script.equals("Zinh")
                && !script.equals("Zyyy")
                && ScriptMetadata.getInfo(script) != null
                && ScriptMetadata.getInfo(script).idUsage != ScriptMetadata.IdUsage.EXCLUSION
                && ScriptMetadata.getInfo(script).idUsage != ScriptMetadata.IdUsage.UNKNOWN) {
                errln("No likely subtag for CLDR script code (und_" + script
                    + ")");
            }
        }

    }

    public void TestEquivalentLocales() {
        Set<Set<String>> seen = new HashSet<Set<String>>();
        Set<String> toTest = new TreeSet<String>(testInfo.getCldrFactory()
            .getAvailable());
        toTest.addAll(SUPPLEMENTAL.getLikelySubtags().keySet());
        toTest.addAll(SUPPLEMENTAL.getLikelySubtags().values());
        toTest.addAll(SUPPLEMENTAL.getDefaultContentLocales());
        LanguageTagParser ltp = new LanguageTagParser();
        main: for (String locale : toTest) {
            if (locale.startsWith("und") || locale.equals("root")) {
                continue;
            }
            Set<String> s = SUPPLEMENTAL.getEquivalentsForLocale(locale);
            if (seen.contains(s)) {
                continue;
            }
            // System.out.println(s + " => " + VettingViewer.gatherCodes(s));

            List<String> ss = new ArrayList<String>(s);
            String last = ss.get(ss.size() - 1);
            ltp.set(last);
            if (!ltp.getVariants().isEmpty() || !ltp.getExtensions().isEmpty()) {
                continue; // skip variants for now.
            }
            String language = ltp.getLanguage();
            String script = ltp.getScript();
            String region = ltp.getRegion();
            if (!script.isEmpty() && !region.isEmpty()) {
                String noScript = ltp.setScript("").toString();
                String noRegion = ltp.setScript(script).setRegion("")
                    .toString();
                switch (s.size()) {
                case 1: // ok if already maximized and strange script/country,
                    // eg it_Arab_JA
                    continue main;
                case 2: // ok if adds default country/script, eg {en_Cyrl,
                    // en_Cyrl_US} or {en_GB, en_Latn_GB}
                    String first = ss.get(0);
                    if (first.equals(noScript) || first.equals(noRegion)) {
                        continue main;
                    }
                    break;
                case 3: // ok if different script in different country, eg
                    // {az_IR, az_Arab, az_Arab_IR}
                    if (noScript.equals(ss.get(0))
                        && noRegion.equals(ss.get(1))) {
                        continue main;
                    }
                    break;
                case 4: // ok if all combinations, eg {en, en_US, en_Latn,
                    // en_Latn_US}
                    if (language.equals(ss.get(0))
                        && noScript.equals(ss.get(1))
                        && noRegion.equals(ss.get(2))) {
                        continue main;
                    }
                    break;
                }
            }
            errln("Strange size or composition:\t" + s + " \t"
                + showLocaleParts(s));
            seen.add(s);
        }
    }

    private String showLocaleParts(Set<String> s) {
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> b = new LinkedHashSet<String>();
        for (String ss : s) {
            ltp.set(ss);
            addName(CLDRFile.LANGUAGE_NAME, ltp.getLanguage(), b);
            addName(CLDRFile.SCRIPT_NAME, ltp.getScript(), b);
            addName(CLDRFile.TERRITORY_NAME, ltp.getRegion(), b);
        }
        return CollectionUtilities.join(b, "; ");
    }

    private void addName(int languageName, String code, Set<String> b) {
        if (code.isEmpty()) {
            return;
        }
        String name = testInfo.getEnglish().getName(languageName, code);
        if (!code.equals(name)) {
            b.add(code + "=" + name);
        }
    }

    public void TestDefaultScriptCompleteness() {
        Relation<String, String> scriptToBase = Relation.of(
            new LinkedHashMap<String, Set<String>>(), TreeSet.class);
        main: for (String locale : testInfo.getCldrFactory()
            .getAvailableLanguages()) {
            if (!locale.contains("_") && !"root".equals(locale)) {
                String defaultScript = SUPPLEMENTAL.getDefaultScript(locale);
                if (defaultScript != null) {
                    continue;
                }
                CLDRFile cldrFile = testInfo.getCLDRFile(locale,
                    false);
                UnicodeSet set = cldrFile.getExemplarSet("",
                    WinningChoice.NORMAL);
                for (String s : set) {
                    int script = UScript.getScript(s.codePointAt(0));
                    if (script != UScript.UNKNOWN && script != UScript.COMMON
                        && script != UScript.INHERITED) {
                        scriptToBase.put(UScript.getShortName(script), locale);
                        continue main;
                    }
                }
                scriptToBase.put(UScript.getShortName(UScript.UNKNOWN), locale);
            }
        }
        if (scriptToBase.size() != 0) {
            for (Entry<String, Set<String>> entry : scriptToBase.keyValuesSet()) {
                errln("Default Scripts missing:\t" + entry.getKey() + "\t"
                    + entry.getValue());
            }
        }
    }

    public void TestTimeData() {
        Map<String, PreferredAndAllowedHour> timeData = SUPPLEMENTAL
            .getTimeData();
        Set<String> regionsSoFar = new HashSet<String>();
        Set<String> current24only = new HashSet<String>();
        Set<String> current12preferred = new HashSet<String>();

        boolean haveWorld = false;
        for (Entry<String, PreferredAndAllowedHour> e : timeData.entrySet()) {
            String region = e.getKey();
            if (region.equals("001")) {
                haveWorld = true;
            }
            regionsSoFar.add(region);
            PreferredAndAllowedHour preferredAndAllowedHour = e.getValue();
            final HourStyle firstAllowed = preferredAndAllowedHour.allowed.iterator().next();
            if (preferredAndAllowedHour.preferred == HourStyle.H && firstAllowed == HourStyle.h
                || preferredAndAllowedHour.preferred == HourStyle.H && firstAllowed == HourStyle.hb
                || preferredAndAllowedHour.preferred == HourStyle.h && firstAllowed == HourStyle.H) {
                errln(region + ": allowed " + preferredAndAllowedHour.allowed
                    + " starts with preferred " + preferredAndAllowedHour.preferred);
            } else if (isVerbose()) {
                logln(region + ": allowed " + preferredAndAllowedHour.allowed
                    + " starts with preferred " + preferredAndAllowedHour.preferred);
            }
            // for (HourStyle c : preferredAndAllowedHour.allowed) {
            // if (!PreferredAndAllowedHour.HOURS.contains(c)) {
            // errln(region + ": illegal character in " +
            // preferredAndAllowedHour.allowed + ". It contains " + c
            // + " which is not in " + PreferredAndAllowedHour.HOURS);
            // }
            // }
            if (!preferredAndAllowedHour.allowed.contains(HourStyle.h)
                && !preferredAndAllowedHour.allowed.contains(HourStyle.hb)) {
                current24only.add(region);
            }
            if (preferredAndAllowedHour.preferred == HourStyle.h) {
                current12preferred.add(region);
            }
        }
        Set<String> missing = new TreeSet<String>(
            STANDARD_CODES.getGoodAvailableCodes(CodeType.territory));
        missing.removeAll(regionsSoFar);
        for (Iterator<String> it = missing.iterator(); it.hasNext();) {
            if (!StandardCodes.isCountry(it.next())) {
                it.remove();
            }
        }

        // if we don't have 001, then we can't miss any regions
        if (!missing.isEmpty()) {
            if (haveWorld) {
                logln("Implicit regions: " + missing);
            } else {
                errln("Missing regions: " + missing);
            }
        }

        // The feedback gathered from our translators is that the following use
        // 24 hour time ONLY:
        Set<String> only24lang = new TreeSet<String>(
            Arrays.asList(("sq, br, bu, ca, hr, cs, da, de, nl, et, eu, fi, "
                + "fr, gl, he, is, id, it, nb, pt, ro, ru, sr, sk, sl, sv, tr, hy")
                    .split(",\\s*")));
        // With the new preferences, this is changed 
        Set<String> only24region = new TreeSet<String>();
        Set<String> either24or12region = new TreeSet<String>();

        // get all countries where official or de-facto official
        // add them two one of two lists, based on the above list of languages
        for (String language : SUPPLEMENTAL
            .getLanguagesForTerritoriesPopulationData()) {
            boolean a24lang = only24lang.contains(language);
            for (String region : SUPPLEMENTAL
                .getTerritoriesForPopulationData(language)) {
                PopulationData pop = SUPPLEMENTAL
                    .getLanguageAndTerritoryPopulationData(language, region);
                if (pop.getOfficialStatus().compareTo(
                    OfficialStatus.de_facto_official) < 0) {
                    continue;
                }
                if (a24lang) {
                    only24region.add(region);
                } else {
                    either24or12region.add(region);
                }
            }
        }
        // if we have a case like CA, where en uses 12/24 but fr uses 24, remove
        // it for safety
        only24region.removeAll(either24or12region);
        // There are always exceptions... Remove VA (Vatican), since it allows 12/24
        // but the de facto langauge is Italian.
        only24region.remove("VA");
        // also remove all the regions where 'h' is preferred
        only24region.removeAll(current12preferred);
        // now verify
        if (!current24only.containsAll(only24region)) {
            Set<String> missing24only = new TreeSet<String>(only24region);
            missing24only.removeAll(current24only);

            errln("24-hour-only doesn't include needed items:\n"
                + " add "
                + CldrUtility.join(missing24only, " ")
                + "\n\t\t"
                + CldrUtility.join(missing24only, "\n\t\t",
                    new NameCodeTransform(testInfo.getEnglish(),
                        CLDRFile.TERRITORY_NAME)));
        }
    }

    public static class NameCodeTransform implements StringTransform {
        private final CLDRFile file;
        private final int codeType;

        public NameCodeTransform(CLDRFile file, int code) {
            this.file = file;
            this.codeType = code;
        }

        @Override
        public String transform(String code) {
            return file.getName(codeType, code) + " [" + code + "]";
        }
    }

    public void TestAliases() {
        testInfo.getStandardCodes();
        Map<String, Map<String, Map<String, String>>> bcp47Data = StandardCodes
            .getLStreg();
        Map<String, Map<String, R2<List<String>, String>>> aliases = SUPPLEMENTAL
            .getLocaleAliasInfo();

        for (Entry<String, Map<String, R2<List<String>, String>>> typeMap : aliases
            .entrySet()) {
            String type = typeMap.getKey();
            Map<String, R2<List<String>, String>> codeReplacement = typeMap
                .getValue();

            Map<String, Map<String, String>> bcp47DataTypeData = bcp47Data
                .get(type.equals("territory") ? "region" : type);
            if (bcp47DataTypeData == null) {
                logln("skipping BCP47 test for " + type);
            } else {
                for (Entry<String, Map<String, String>> codeData : bcp47DataTypeData
                    .entrySet()) {
                    String code = codeData.getKey();
                    if (codeReplacement.containsKey(code)
                        || codeReplacement.containsKey(code
                            .toUpperCase(Locale.ENGLISH))) {
                        continue;
                        // TODO, check the value
                    }
                    Map<String, String> data = codeData.getValue();
                    if (data.containsKey("Deprecated")
                        && SUPPLEMENTAL.getCLDRLanguageCodes().contains(
                            code)) {
                        errln("supplementalMetadata.xml: alias is missing <languageAlias type=\""
                            + code + "\" ... /> " + "\t" + data);
                    }
                }
            }

            Set<R3<String, List<String>, List<String>>> failures = new TreeSet<R3<String, List<String>, List<String>>>();
            Set<String> nullReplacements = new TreeSet<String>();
            for (Entry<String, R2<List<String>, String>> codeRep : codeReplacement
                .entrySet()) {
                String code = codeRep.getKey();
                List<String> replacements = codeRep.getValue().get0();
                if (replacements == null) {
                    nullReplacements.add(code);
                    continue;
                }
                Set<String> fixedReplacements = new LinkedHashSet<String>();
                for (String replacement : replacements) {
                    R2<List<String>, String> newReplacement = codeReplacement
                        .get(replacement);
                    if (newReplacement != null) {
                        List<String> list = newReplacement.get0();
                        if (list != null) {
                            fixedReplacements.addAll(list);
                        }
                    } else {
                        fixedReplacements.add(replacement);
                    }
                }
                List<String> fixedList = new ArrayList<String>(
                    fixedReplacements);
                if (!replacements.equals(fixedList)) {
                    R3<String, List<String>, List<String>> row = Row.of(code,
                        replacements, fixedList);
                    System.out.println(row.toString());
                    failures.add(row);
                }
            }

            if (failures.size() != 0) {
                for (R3<String, List<String>, List<String>> item : failures) {
                    String code = item.get0();
                    List<String> oldReplacement = item.get1();
                    List<String> newReplacement = item.get2();

                    errln(code + "\t=>\t" + oldReplacement + "\tshould be:\n\t"
                        + "<" + type + "Alias type=\"" + code
                        + "\" replacement=\""
                        + CollectionUtilities.join(newReplacement, " ")
                        + "\" reason=\"XXX\"/> <!-- YYY -->\n");
                }
            }
            if (nullReplacements.size() != 0) {
                logln("No Replacements\t" + type + "\t" + nullReplacements);
            }
        }
    }

    static final List<String> oldRegions = Arrays
        .asList("NT, YD, QU, SU, DD, FX, ZR, AN, BU, TP, CS, YU"
            .split(", "));

    public void TestTerritoryContainment() {
        Relation<String, String> map = SUPPLEMENTAL
            .getTerritoryToContained(ContainmentStyle.all);
        Relation<String, String> mapCore = SUPPLEMENTAL.getContainmentCore();
        Set<String> mapItems = new LinkedHashSet<String>();
        // get all the items
        for (String item : map.keySet()) {
            mapItems.add(item);
            mapItems.addAll(map.getAll(item));
        }
        Map<String, Map<String, String>> bcp47RegionData = StandardCodes
            .getLStreg().get("region");

        // verify that all regions are covered
        Set<String> bcp47Regions = new LinkedHashSet<String>(
            bcp47RegionData.keySet());
        bcp47Regions.remove("ZZ"); // We don't care about ZZ since it is the
        // unknown region...
        for (Iterator<String> it = bcp47Regions.iterator(); it.hasNext();) {
            String region = it.next();
            Map<String, String> data = bcp47RegionData.get(region);
            if (data.containsKey("Deprecated")) {
                logln("Removing deprecated " + region);
                it.remove();
            }
            if ("Private use".equals(data.get("Description"))) {
                it.remove();
            }
        }

        if (!mapItems.equals(bcp47Regions)) {
            mapItems.removeAll(oldRegions);
            errlnDiff("containment items not in bcp47 regions: ", mapItems,
                bcp47Regions);
            errlnDiff("bcp47 regions not in containment items: ", bcp47Regions,
                mapItems);
        }

        // verify that everything in the containment core can be reached
        // downwards from 001.

        Map<String, Integer> from001 = getRecursiveContainment("001", map,
            new LinkedHashMap<String, Integer>(), 1);
        from001.put("001", 0);
        Set<String> keySet = from001.keySet();
        for (String region : keySet) {
            logln(Utility.repeat("\t", from001.get(region)) + "\t" + region
                + "\t" + getRegionName(region));
        }

        // Populate mapItems with the core containment
        mapItems.clear();
        for (String item : mapCore.keySet()) {
            mapItems.add(item);
            mapItems.addAll(mapCore.getAll(item));
        }

        if (!mapItems.equals(keySet)) {
            errlnDiff(
                "containment core items that can't be reached from 001: ",
                mapItems, keySet);
        }
    }

    private void errlnDiff(String title, Set<String> mapItems,
        Set<String> keySet) {
        Set<String> diff = new LinkedHashSet<String>(mapItems);
        diff.removeAll(keySet);
        if (diff.size() != 0) {
            errln(title + diff);
        }
    }

    private String getRegionName(String region) {
        return testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME, region);
    }

    private Map<String, Integer> getRecursiveContainment(String region,
        Relation<String, String> map, Map<String, Integer> result, int depth) {
        Set<String> contained = map.getAll(region);
        if (contained == null) {
            return result;
        }
        for (String item : contained) {
            if (result.containsKey(item)) {
                logln("Duplicate containment " + item + "\t"
                    + getRegionName(item));
                continue;
            }
            result.put(item, depth);
            getRecursiveContainment(item, map, result, depth + 1);
        }
        return result;
    }

    public void TestMacrolanguages() {
        Set<String> languageCodes = STANDARD_CODES
            .getAvailableCodes("language");
        Map<String, Map<String, R2<List<String>, String>>> typeToTagToReplacement = SUPPLEMENTAL
            .getLocaleAliasInfo();
        Map<String, R2<List<String>, String>> tagToReplacement = typeToTagToReplacement
            .get("language");

        Relation<String, String> replacementToReplaced = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);
        for (String language : tagToReplacement.keySet()) {
            List<String> replacements = tagToReplacement.get(language).get0();
            if (replacements != null) {
                replacementToReplaced.putAll(replacements, language);
            }
        }
        replacementToReplaced.freeze();

        Map<String, Map<String, Map<String, String>>> lstreg = StandardCodes
            .getLStreg();
        Map<String, Map<String, String>> lstregLanguageInfo = lstreg
            .get("language");

        Relation<Scope, String> scopeToCodes = Relation.of(
            new TreeMap<Scope, Set<String>>(), TreeSet.class);
        // the invariant is that every macrolanguage has exactly 1 encompassed
        // language that maps to it

        main: for (String language : Builder.with(new TreeSet<String>())
            .addAll(languageCodes).addAll(Iso639Data.getAvailable()).get()) {
            if (language.equals("no") || language.equals("sh"))
                continue; // special cases
            Scope languageScope = getScope(language, lstregLanguageInfo);
            if (languageScope == Scope.Macrolanguage) {
                if (Iso639Data.getHeirarchy(language) != null) {
                    continue main; // is real family
                }
                Set<String> replacements = replacementToReplaced
                    .getAll(language);
                if (replacements == null || replacements.size() == 0) {
                    scopeToCodes.put(languageScope, language);
                } else {
                    // it still might be bad, if we don't have a mapping to a
                    // regular language
                    for (String replacement : replacements) {
                        Scope replacementScope = getScope(replacement,
                            lstregLanguageInfo);
                        if (replacementScope == Scope.Individual) {
                            continue main;
                        }
                    }
                    scopeToCodes.put(languageScope, language);
                }
            }
        }
        // now show the items we found
        for (Scope scope : scopeToCodes.keySet()) {
            for (String language : scopeToCodes.getAll(scope)) {
                String name = testInfo.getEnglish().getName(language);
                if (name == null || name.equals(language)) {
                    Set<String> set = Iso639Data.getNames(language);
                    if (set != null) {
                        name = set.iterator().next();
                    } else {
                        Map<String, String> languageInfo = lstregLanguageInfo
                            .get(language);
                        if (languageInfo != null) {
                            name = languageInfo.get("Description");
                        }
                    }
                }
                errln(scope + "\t" + language + "\t" + name + "\t"
                    + Iso639Data.getType(language));
            }
        }
    }

    private Scope getScope(String language,
        Map<String, Map<String, String>> lstregLanguageInfo) {
        Scope languageScope = Iso639Data.getScope(language);
        Map<String, String> languageInfo = lstregLanguageInfo.get(language);
        if (languageInfo == null) {
            // System.out.println("Couldn't get lstreg info for " + language);
        } else {
            String lstregScope = languageInfo.get("Scope");
            if (lstregScope != null) {
                Scope scope2 = Scope.fromString(lstregScope);
                if (languageScope != scope2) {
                    // System.out.println("Mismatch in scope between LSTR and ISO 639:\t"
                    // + scope2 + "\t" +
                    // languageScope);
                    languageScope = scope2;
                }
            }
        }
        return languageScope;
    }

    static final boolean LOCALES_FIXED = true;

    public void TestPopulation() {
        Set<String> languages = SUPPLEMENTAL
            .getLanguagesForTerritoriesPopulationData();
        Relation<String, String> baseToLanguages = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);
        LanguageTagParser ltp = new LanguageTagParser();
        LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer(false);

        for (String language : languages) {
            if (LOCALES_FIXED) {
                String canonicalForm = ltc.transform(language);
                if (!assertEquals("Canonical form", canonicalForm, language)) {
                    int debug = 0;
                }
            }

            String base = ltp.set(language).getLanguage();
            String script = ltp.getScript();
            baseToLanguages.put(base, language);

            // add basic data, basically just for wo!
            // if there are primary scripts, they must include script (if not
            // empty)
            Set<String> primaryScripts = Collections.emptySet();
            Map<Type, BasicLanguageData> basicData = SUPPLEMENTAL
                .getBasicLanguageDataMap(base);
            if (basicData != null) {
                BasicLanguageData s = basicData
                    .get(BasicLanguageData.Type.primary);
                if (s != null) {
                    primaryScripts = s.getScripts();
                }
            }

            // do some consistency tests; if there is a script, it must be in
            // primaryScripts
            if (!script.isEmpty() && !primaryScripts.contains(script)) {
                errln(base + ": Script found in territory data (" + script
                    + ") is not in primary scripts :\t" + primaryScripts);
            }

            // if there are multiple primary scripts, they will be in
            // baseToLanguages
            if (primaryScripts.size() > 1) {
                for (String script2 : primaryScripts) {
                    baseToLanguages.put(base, base + "_" + script2);
                }
            }
        }

        if (!LOCALES_FIXED) {
            // the invariants are that if we have a base, we must not have a script.
            // and if we don't have a base, we must have two items
            for (String base : baseToLanguages.keySet()) {
                Set<String> languagesForBase = baseToLanguages.getAll(base);
                if (languagesForBase.contains(base)) {
                    if (languagesForBase.size() > 1) {
                        errln("Cannot have base alone with other scripts:\t"
                            + languagesForBase);
                    }
                } else {
                    if (languagesForBase.size() == 1) {
                        errln("Cannot have only one script for language:\t"
                            + languagesForBase);
                    }
                }
            }
        }
    }

    public void TestCompleteness() {
        if (SUPPLEMENTAL.getSkippedElements().size() > 0) {
            logln("SupplementalDataInfo API doesn't support: "
                + SUPPLEMENTAL.getSkippedElements().toString());
        }
    }

    // these are settings for exceptional cases we want to allow
    private static final Set<String> EXCEPTION_CURRENCIES_WITH_NEW = new TreeSet<String>(
        Arrays.asList("ILS", "NZD", "PGK", "TWD"));

    // ok since there is no problem with confusion
    private static final Set<String> OK_TO_NOT_HAVE_OLD = new TreeSet<String>(
        Arrays.asList("ADP", "ATS", "BEF", "CYP", "DEM", "ESP", "FIM",
            "FRF", "GRD", "IEP", "ITL", "LUF", "MTL", "MTP", "NLG",
            "PTE", "YUM", "ARA", "BAD", "BGL", "BOP", "BRC", "BRN",
            "BRR", "BUK", "CSK", "ECS", "GEK", "GNS", "GQE", "HRD",
            "ILP", "LTT", "LVR", "MGF", "MLF", "MZE", "NIC", "PEI",
            "PES", "SIT", "SRG", "SUR", "TJR", "TPE", "UAK", "YUD",
            "YUN", "ZRZ", "GWE"));

    private static final Date LIMIT_FOR_NEW_CURRENCY = new Date(
        new Date().getYear() - 5, 1, 1);
    private static final Date NOW = new Date();
    private Matcher oldMatcher = Pattern.compile(
        "\\bold\\b|\\([0-9]{4}-[0-9]{4}\\)", Pattern.CASE_INSENSITIVE)
        .matcher("");
    private Matcher newMatcher = Pattern.compile("\\bnew\\b",
        Pattern.CASE_INSENSITIVE).matcher("");

    /**
     * Test that access to currency info in supplemental data is ok. At this
     * point just a simple test.
     *
     * @param args
     */
    public void TestCurrency() {
        IsoCurrencyParser isoCodes = IsoCurrencyParser.getInstance();
        Set<String> currencyCodes = STANDARD_CODES
            .getGoodAvailableCodes("currency");
        Relation<String, Pair<String, CurrencyDateInfo>> nonModernCurrencyCodes = Relation
            .of(new TreeMap<String, Set<Pair<String, CurrencyDateInfo>>>(),
                TreeSet.class);
        Relation<String, Pair<String, CurrencyDateInfo>> modernCurrencyCodes = Relation
            .of(new TreeMap<String, Set<Pair<String, CurrencyDateInfo>>>(),
                TreeSet.class);
        Set<String> territoriesWithoutModernCurrencies = new TreeSet<String>(
            STANDARD_CODES.getGoodAvailableCodes("territory"));
        Map<String, Date> currencyFirstValid = new TreeMap<String, Date>();
        Map<String, Date> currencyLastValid = new TreeMap<String, Date>();
        territoriesWithoutModernCurrencies.remove("ZZ");

        for (String territory : STANDARD_CODES
            .getGoodAvailableCodes("territory")) {
            /* "EU" behaves like a country for purposes of this test */
            if ((SUPPLEMENTAL.getContained(territory) != null)
                && !territory.equals("EU")) {
                territoriesWithoutModernCurrencies.remove(territory);
                continue;
            }
            Set<CurrencyDateInfo> currencyInfo = SUPPLEMENTAL
                .getCurrencyDateInfo(territory);
            if (currencyInfo == null) {
                continue; // error, but will pick up below.
            }
            for (CurrencyDateInfo dateInfo : currencyInfo) {
                final String currency = dateInfo.getCurrency();
                final Date start = dateInfo.getStart();
                final Date end = dateInfo.getEnd();
                if (dateInfo.getErrors().length() != 0) {
                    logln("parsing " + territory + "\t" + dateInfo.toString()
                        + "\t" + dateInfo.getErrors());
                }
                Date firstValue = currencyFirstValid.get(currency);
                if (firstValue == null || firstValue.compareTo(start) < 0) {
                    currencyFirstValid.put(currency, start);
                }
                Date lastValue = currencyLastValid.get(currency);
                if (lastValue == null || lastValue.compareTo(end) > 0) {
                    currencyLastValid.put(currency, end);
                }
                if (start.compareTo(NOW) < 0 && end.compareTo(NOW) >= 0) { // Non-tender
                    // is
                    // OK...
                    modernCurrencyCodes.put(currency,
                        new Pair<String, CurrencyDateInfo>(territory,
                            dateInfo));
                    territoriesWithoutModernCurrencies.remove(territory);
                } else {
                    nonModernCurrencyCodes.put(currency,
                        new Pair<String, CurrencyDateInfo>(territory,
                            dateInfo));
                }
                logln(territory
                    + "\t"
                    + dateInfo.toString()
                    + "\t"
                    + testInfo.getEnglish().getName(CLDRFile.CURRENCY_NAME,
                        currency));
            }
        }
        // fix up
        nonModernCurrencyCodes.removeAll(modernCurrencyCodes.keySet());
        Relation<String, String> isoCurrenciesToCountries = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class)
            .addAllInverted(isoCodes.getCountryToCodes());
        // now print error messages
        logln("Modern Codes: " + modernCurrencyCodes.size() + "\t"
            + modernCurrencyCodes);
        Set<String> missing = new TreeSet<String>(
            isoCurrenciesToCountries.keySet());
        missing.removeAll(modernCurrencyCodes.keySet());
        if (missing.size() != 0) {
            errln("Missing codes compared to ISO: " + missing.toString());
        }

        for (String currency : modernCurrencyCodes.keySet()) {
            Set<Pair<String, CurrencyDateInfo>> data = modernCurrencyCodes
                .getAll(currency);
            final String name = testInfo.getEnglish().getName(
                CLDRFile.CURRENCY_NAME, currency);

            Set<String> isoCountries = isoCurrenciesToCountries
                .getAll(currency);
            if (isoCountries == null) {
                isoCountries = new TreeSet<String>();
            }

            TreeSet<String> cldrCountries = new TreeSet<String>();
            for (Pair<String, CurrencyDateInfo> x : data) {
                cldrCountries.add(x.getFirst());
            }
            if (!isoCountries.equals(cldrCountries)) {
                if (!logKnownIssue("cldrbug:10765", "Missing codes compared to ISO: " + missing.toString())) {

                    errln("Mismatch between ISO and Cldr modern currencies for "
                        + currency + "\tISO:" + isoCountries + "\tCLDR:"
                        + cldrCountries);
                    showCountries("iso-cldr", isoCountries, cldrCountries, missing);
                    showCountries("cldr-iso", cldrCountries, isoCountries, missing);
                }
            }

            if (oldMatcher.reset(name).find()) {
                errln("Has 'old' in name but still used " + "\t" + currency
                    + "\t" + name + "\t" + data);
            }
            if (newMatcher.reset(name).find()
                && !EXCEPTION_CURRENCIES_WITH_NEW.contains(currency)) {
                // find the first use. If older than 5 years, flag as error
                if (currencyFirstValid.get(currency).compareTo(
                    LIMIT_FOR_NEW_CURRENCY) < 0) {
                    errln("Has 'new' in name but used since "
                        + CurrencyDateInfo.formatDate(currencyFirstValid
                            .get(currency))
                        + "\t" + currency + "\t"
                        + name + "\t" + data);
                } else {
                    logln("Has 'new' in name but used since "
                        + CurrencyDateInfo.formatDate(currencyFirstValid
                            .get(currency))
                        + "\t" + currency + "\t"
                        + name + "\t" + data);
                }
            }
        }
        logln("Non-Modern Codes (with dates): " + nonModernCurrencyCodes.size()
            + "\t" + nonModernCurrencyCodes);
        for (String currency : nonModernCurrencyCodes.keySet()) {
            final String name = testInfo.getEnglish().getName(
                CLDRFile.CURRENCY_NAME, currency);
            if (newMatcher.reset(name).find()
                && !EXCEPTION_CURRENCIES_WITH_NEW.contains(currency)) {
                logln("Has 'new' in name but NOT used since "
                    + CurrencyDateInfo.formatDate(currencyLastValid
                        .get(currency))
                    + "\t" + currency + "\t" + name
                    + "\t" + nonModernCurrencyCodes.getAll(currency));
            } else if (!oldMatcher.reset(name).find()
                && !OK_TO_NOT_HAVE_OLD.contains(currency)) {
                logln("Doesn't have 'old' or date range in name but NOT used since "
                    + CurrencyDateInfo.formatDate(currencyLastValid
                        .get(currency))
                    + "\t"
                    + currency
                    + "\t"
                    + name
                    + "\t" + nonModernCurrencyCodes.getAll(currency));
                for (Pair<String, CurrencyDateInfo> pair : nonModernCurrencyCodes
                    .getAll(currency)) {
                    final String territory = pair.getFirst();
                    Set<CurrencyDateInfo> currencyInfo = SUPPLEMENTAL
                        .getCurrencyDateInfo(territory);
                    for (CurrencyDateInfo dateInfo : currencyInfo) {
                        if (dateInfo.getEnd().compareTo(NOW) < 0) {
                            continue;
                        }
                        logln("\tCurrencies used instead: "
                            + territory
                            + "\t"
                            + dateInfo
                            + "\t"
                            + testInfo.getEnglish().getName(
                                CLDRFile.CURRENCY_NAME,
                                dateInfo.getCurrency()));

                    }
                }

            }
        }
        Set<String> remainder = new TreeSet<String>();
        remainder.addAll(currencyCodes);
        remainder.removeAll(nonModernCurrencyCodes.keySet());
        // TODO make this an error, except for allowed exceptions.
        logln("Currencies without Territories: " + remainder);
        if (territoriesWithoutModernCurrencies.size() != 0) {
            errln("Modern territory missing currency: "
                + territoriesWithoutModernCurrencies);
        }
    }

    private void showCountries(final String title, Set<String> isoCountries,
        Set<String> cldrCountries, Set<String> missing) {
        missing.clear();
        missing.addAll(isoCountries);
        missing.removeAll(cldrCountries);
        for (String country : missing) {
            logln("\t\tExtra in " + title + "\t" + country + " - "
                + getRegionName(country));
        }
    }

    public void TestCurrencyDecimalPlaces() {
        IsoCurrencyParser isoCodes = IsoCurrencyParser.getInstance();
        Relation<String, IsoCurrencyParser.Data> codeList = isoCodes
            .getCodeList();
        Set<String> currencyCodes = STANDARD_CODES
            .getGoodAvailableCodes("currency");
        for (String cc : currencyCodes) {
            Set<IsoCurrencyParser.Data> d = codeList.get(cc);
            if (d != null) {
                for (IsoCurrencyParser.Data x : d) {
                    CurrencyNumberInfo cni = SUPPLEMENTAL.getCurrencyNumberInfo(cc);
                    if (cni.digits != x.getMinorUnit()) {
                        logln("Mismatch between ISO/CLDR for decimal places for currency => " + cc +
                            ". ISO = " + x.getMinorUnit() + " CLDR = " + cni.digits);
                    }
                }
            }
        }
    }

    /**
     * Verify that we have a default script for every CLDR base language
     */
    public void TestDefaultScripts() {
        SupplementalDataInfo supp = SUPPLEMENTAL;
        Map<String, String> likelyData = supp.getLikelySubtags();
        Map<String, String> baseToDefaultContentScript = new HashMap<String, String>();
        for (CLDRLocale locale : supp.getDefaultContentCLDRLocales()) {
            String script = locale.getScript();
            if (!script.isEmpty() && locale.getCountry().isEmpty()) {
                baseToDefaultContentScript.put(locale.getLanguage(), script);
            }
        }
        for (String locale : testInfo.getCldrFactory().getAvailableLanguages()) {
            if ("root".equals(locale)) {
                continue;
            }
            CLDRLocale loc = CLDRLocale.getInstance(locale);
            String baseLanguage = loc.getLanguage();
            String defaultScript = supp.getDefaultScript(baseLanguage);

            String defaultContentScript = baseToDefaultContentScript
                .get(baseLanguage);
            if (defaultContentScript != null) {
                assertEquals(loc + " defaultContentScript = default",
                    defaultScript, defaultContentScript);
            }
            String likely = likelyData.get(baseLanguage);
            String likelyScript = likely == null ? null : CLDRLocale
                .getInstance(likely).getScript();
            Map<Type, BasicLanguageData> scriptInfo = supp
                .getBasicLanguageDataMap(baseLanguage);
            if (scriptInfo == null) {
                errln(loc + ": has no BasicLanguageData");
            } else {
                BasicLanguageData data = scriptInfo.get(Type.primary);
                if (data == null) {
                    data = scriptInfo.get(Type.secondary);
                }
                if (data == null) {
                    errln(loc + ": has no scripts in BasicLanguageData");
                } else if (!data.getScripts().contains(defaultScript)) {
                    errln(loc + ": " + defaultScript
                        + " not in BasicLanguageData " + data.getScripts());
                }
            }

            assertEquals(loc + " likely = default", defaultScript, likelyScript);

            assertNotNull(loc + ": needs default script", defaultScript);

            if (!loc.getScript().isEmpty()) {
                if (!loc.getScript().equals(defaultScript)) {
                    assertNotEquals(locale
                        + ": only include script if not default",
                        loc.getScript(), defaultScript);
                }
            }

        }
    }

    enum CoverageIssue {
        log, warn, error
    }

    public void TestPluralCompleteness() {
        // Set<String> cardinalLocales = new
        // TreeSet<String>(SUPPLEMENTAL.getPluralLocales(PluralType.cardinal));
        // Set<String> ordinalLocales = new
        // TreeSet<String>(SUPPLEMENTAL.getPluralLocales(PluralType.ordinal));
        // Map<ULocale, PluralRulesFactory.SamplePatterns> sampleCardinals =
        // PluralRulesFactory.getLocaleToSamplePatterns();
        // Set<ULocale> sampleCardinalLocales = PluralRulesFactory.getLocales();
        // // new HashSet(PluralRulesFactory.getSampleCounts(uLocale,
        // type).keySet());
        // Map<ULocale, PluralRules> overrideCardinals =
        // PluralRulesFactory.getPluralOverrides();
        // Set<ULocale> overrideCardinalLocales = new
        // HashSet<ULocale>(overrideCardinals.keySet());

        Set<String> testLocales = STANDARD_CODES.getLocaleCoverageLocales(
            Organization.google, EnumSet.of(Level.MODERN));
        Set<String> allLocales = testInfo.getCldrFactory().getAvailable();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : allLocales) {
            // the only known case where plural rules depend on region or script
            // is pt_PT
            if (locale.equals("root")) {
                continue;
            }
            ltp.set(locale);
            if (!ltp.getRegion().isEmpty() || !ltp.getScript().isEmpty()) {
                continue;
            }
            CoverageIssue needsCoverage = testLocales.contains(locale)
                ? CoverageIssue.error
                : CoverageIssue.log;
            CoverageIssue needsCoverage2 = needsCoverage == CoverageIssue.error ? CoverageIssue.warn : needsCoverage;

            //            if (logKnownIssue("Cldrbug:8809", "Missing plural rules/samples be and ga locales")) {
            //                if (locale.equals("be") || locale.equals("ga")) {
            //                    needsCoverage = CoverageIssue.warn;
            //                }
            //            }
            PluralRulesFactory prf = PluralRulesFactory
                .getInstance(CLDRConfig.getInstance()
                    .getSupplementalDataInfo());

            for (PluralType type : PluralType.values()) {
                PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(type, locale,
                    false);
                if (pluralInfo == null) {
                    errOrLog(needsCoverage, locale + "\t" + type + " \tmissing plural rules", "Cldrbug:7839", "Missing plural data for modern locales");
                    continue;
                }
                Set<Count> counts = pluralInfo.getCounts();
                // if (counts.size() == 1) {
                // continue; // skip checking samples
                // }
                HashSet<String> samples = new HashSet<String>();
                EnumSet<Count> countsWithNoSamples = EnumSet
                    .noneOf(Count.class);
                Relation<String, Count> samplesToCounts = Relation.of(
                    new HashMap(), LinkedHashSet.class);
                Set<Count> countsFound = prf.getSampleCounts(locale,
                    type.standardType);
                StringBuilder failureCases = new StringBuilder();
                for (Count count : counts) {
                    String pattern = prf.getSamplePattern(locale, type.standardType, count);
                    final String rangeLine = getRangeLine(count, pluralInfo.getPluralRules(), pattern);
                    failureCases.append('\n').append(locale).append('\t').append(type).append('\t').append(rangeLine);
                    if (countsFound == null || !countsFound.contains(count)) {
                        countsWithNoSamples.add(count);
                    } else {
                        samplesToCounts.put(pattern, count);
                        logln(locale + "\t" + type + "\t" + count + "\t"
                            + pattern);
                    }
                }
                if (!countsWithNoSamples.isEmpty()) {
                    errOrLog(needsCoverage, locale + "\t" + type + "\t missing samples:\t" + countsWithNoSamples,
                        "cldrbug:7075", "Missing ordinal minimal pairs");
                    errOrLog(needsCoverage2, failureCases.toString());
                }
                for (Entry<String, Set<Count>> entry : samplesToCounts
                    .keyValuesSet()) {
                    if (entry.getValue().size() != 1) {
                        errOrLog(needsCoverage, locale + "\t" + type + "\t duplicate samples: " + entry.getValue()
                            + " => «" + entry.getKey() + "»", "cldrbug:7119", "Some duplicate minimal pairs");
                        errOrLog(needsCoverage2, failureCases.toString());
                    }
                }
            }
        }
    }

    public void errOrLog(CoverageIssue causeError, String message, String logTicket, String logComment) {
        switch (causeError) {
        case error:
            if (logTicket == null) {
                errln(message);
                break;
            }
            logKnownIssue(logTicket, logComment);
            // fall through
        case warn:
            warnln(message);
            break;
        case log:
            logln(message);
            break;
        }
    }

    public void errOrLog(CoverageIssue causeError, String message) {
        errOrLog(causeError, message, null, null);
    }

    public void TestNumberingSystemDigits() {

        // Don't worry about digits from supplemental planes yet ( ICU can't
        // handle them anyways )
        // hanidec is the only known non codepoint order numbering system
        // TODO: Fix so that it works properly on non-BMP digit strings.
        String[] knownExceptions = { "brah", "cakm", "hanidec", "osma", "shrd",
            "sora", "takr" };
        List<String> knownExceptionList = Arrays.asList(knownExceptions);
        for (String ns : SUPPLEMENTAL.getNumericNumberingSystems()) {
            if (knownExceptionList.contains(ns)) {
                continue;
            }
            String digits = SUPPLEMENTAL.getDigits(ns);
            int previousChar = 0;
            int ch;

            for (int i = 0; i < digits.length(); i += Character.charCount(ch)) {
                ch = digits.codePointAt(i);
                if (i > 0 && ch != previousChar + 1) {
                    errln("Digits for numbering system "
                        + ns
                        + " are not in code point order. Previous char = U+"
                        + Utility.hex(previousChar, 4)
                        + " Current char = U+" + Utility.hex(ch, 4));
                    break;
                }
                previousChar = ch;
            }
        }
    }

    public void TestNumberingSystemDigitCompleteness() {
        List<Integer> unicodeDigits = new ArrayList<Integer>();
        for (int cp = UCharacter.MIN_CODE_POINT; cp <= UCharacter.MAX_CODE_POINT; cp++) {
            if (UCharacter.getType(cp) == UCharacterEnums.ECharacterCategory.DECIMAL_DIGIT_NUMBER) {
                unicodeDigits.add(Integer.valueOf(cp));
            }
        }

        for (String ns : SUPPLEMENTAL.getNumericNumberingSystems()) {
            String digits = SUPPLEMENTAL.getDigits(ns);
            int ch;

            for (int i = 0; i < digits.length(); i += Character.charCount(ch)) {
                ch = digits.codePointAt(i);
                unicodeDigits.remove(Integer.valueOf(ch));
            }
        }

        if (unicodeDigits.size() > 0) {
            for (Integer i : unicodeDigits) {
                errln("Unicode digit: " + UCharacter.getName(i) + " is not in any numbering system. Script = "
                    + UScript.getShortName(UScript.getScript(i)));
            }
        }
    }

    public void TestMetazones() {
        Date goalMin = new Date(70, 0, 1);
        Date goalMax = new Date(300, 0, 2);
        ImmutableSet<String> knownTZWithoutMetazone = ImmutableSet.of("America/Montreal", "Asia/Barnaul", "Asia/Tomsk", "Europe/Kirov");
        for (String timezoneRaw : TimeZone.getAvailableIDs()) {
            String timezone = TimeZone.getCanonicalID(timezoneRaw);
            String region = TimeZone.getRegion(timezone);
            if (!timezone.equals(timezoneRaw) || "001".equals(region)) {
                continue;
            }
            if (knownTZWithoutMetazone.contains(timezone)) {
                continue;
            }
            final Set<MetaZoneRange> ranges = SUPPLEMENTAL
                .getMetaZoneRanges(timezone);

            if (assertNotNull("metazones for " + timezone, ranges)) {
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                for (MetaZoneRange range : ranges) {
                    if (range.dateRange.from != DateRange.START_OF_TIME) {
                        min = Math.min(min, range.dateRange.from);
                    }
                    if (range.dateRange.to != DateRange.END_OF_TIME) {
                        max = Math.max(max, range.dateRange.to);
                    }
                }
                assertRelation(timezone + " has metazone before 1970?", true,
                    goalMin, LEQ, new Date(min));
                assertRelation(timezone
                    + " has metazone until way in the future?", true,
                    goalMax, GEQ, new Date(max));
            }
        }
        com.google.common.collect.Interners i;
    }

    public void Test9924() {
        PopulationData zhCNData = SUPPLEMENTAL.getLanguageAndTerritoryPopulationData(LOCALES_FIXED ? "zh" : "zh_Hans", "CN");
        PopulationData yueCNData = SUPPLEMENTAL.getLanguageAndTerritoryPopulationData("yue_Hans", "CN");
        assertTrue("yue*10 < zh", yueCNData.getPopulation() < zhCNData.getPopulation());
    }

    public void Test10765() { // 
        Set<String> surveyToolLanguages = SUPPLEMENTAL.getCLDRLanguageCodes(); // codes that show up in Survey Tool
        Set<String> mainLanguages = new TreeSet<>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : testInfo.getCldrFactory().getAvailableLanguages()) {
            mainLanguages.add(ltp.set(locale).getLanguage());
        }
        // add special codes we want to see anyway
        mainLanguages.add("und");
        mainLanguages.add("mul");
        mainLanguages.add("zxx");

        if (!mainLanguages.containsAll(surveyToolLanguages)) {
            CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(SUPPLEMENTAL, "ja"); // pick "neutral" locale
            Set<String> temp = new TreeSet<>(surveyToolLanguages);
            temp.removeAll(mainLanguages);
            Set<String> modern = new TreeSet<>();
            Set<String> comprehensive = new TreeSet<>();
            for (String lang : temp) {
                Level level = coverageLevel.getLevel(CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, lang));
                if (level.compareTo(Level.MODERN) <= 0) {
                    modern.add(lang);
                } else {
                    comprehensive.add(lang);
                }
            }
            warnln("«Modern» Languages in <variable id='$language' type='choice'> that aren't in main/* : " + getNames(modern));
            logln("«Comprehensive» Languages in <variable id='$language' type='choice'> that aren't in main/* : " + getNames(comprehensive));
        }
        if (!surveyToolLanguages.containsAll(mainLanguages)) {
            mainLanguages.removeAll(surveyToolLanguages);
            assertEquals("No main/* languages are missing from Survey Tool:language names (eg <variable id='$language' type='choice'>) ",
                Collections.EMPTY_SET, mainLanguages);
        }
    }

    private Set<String> getNames(Set<String> temp) {
        Set<String> tempNames = new TreeSet<>();
        for (String langCode : temp) {
            tempNames.add(testInfo.getEnglish().getName(CLDRFile.LANGUAGE_NAME, langCode) + " (" + langCode + ")");
        }
        return tempNames;
    }
}
