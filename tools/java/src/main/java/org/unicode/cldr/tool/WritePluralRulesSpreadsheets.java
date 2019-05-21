/*
 *******************************************************************************
 * Copyright (C) 2013, Google Inc. and International Business Machines Corporation and  *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.tool;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PluralRanges;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.text.PluralRules.SampleType;

/**
 * Generate a spreadsheet that translators can use to pick the right plural range results,
 * which we can then use to create the plural range rules.
 * @author markdavis
 */
public class WritePluralRulesSpreadsheets {

    // TODO rewrite to use Options, generate file instead of console.

    public static String[] tests = { "or", "tk", "ps", "as", "sd" };

    public static void main(String[] args) {
        //writePluralChecklist(tests);
        ranges();
    }

    static final SupplementalDataInfo supplemental = SupplementalDataInfo.getInstance();
    static final Factory factory = ToolConfig.getToolInstance().getFullCldrFactory();
    static final StandardCodes STD = StandardCodes.make();

    private static void ranges() {
        Multimap<Set<String>, String> missingMinimalPairs = HashMultimap.create();
        System.out.println("Type\tCode\tName\tRange\tResult\tResult Example\tStart-Range Example\tEnd-Range Example");
        Set<String> cldrLocales = // new TreeSet<>(Arrays.asList(tests));
            new TreeSet<>(STD.getLocaleCoverageLocales(Organization.cldr));
        cldrLocales.addAll(STD.getLocaleCoverageLocales(Organization.google));

        writeRanges("Core", cldrLocales, missingMinimalPairs);

        for (Entry<Set<String>, String> missing : missingMinimalPairs.entries()) {
            Set<String> keywords = missing.getKey();
            String locale = missing.getValue();
            System.out.println("Missing Core\t" + getName(locale) + "\t" + keywords);
        }
        System.out.println();

        missingMinimalPairs.clear();
        TreeSet<String> localesWithPlurals = new TreeSet<>(supplemental.getPluralLocales(SupplementalDataInfo.PluralType.cardinal));
        localesWithPlurals.removeAll(cldrLocales);

        writeRanges("Other", localesWithPlurals, missingMinimalPairs);

        for (Entry<Set<String>, String> missing : missingMinimalPairs.entries()) {
            Set<String> keywords = missing.getKey();
            String locale = missing.getValue();
            System.out.println("Missing Other\t" + getName(locale) + "\t" + keywords);
        }
    }

    private static void writePluralChecklist(String... locales) {
        List<String> sampleStrings = Arrays.asList("0",
            "0.1",
            "0.2",
            "0.9",
            "1.9",
            "1",
            "1.0",
            "1.2",
            "2.0",
            "2.1",
            "0.00",
            "0.01",
            "0.10",
            "0.11",
            "0.02",
            "1.00",
            "1.10",
            "1.11",
            "1.02",
            "2.00",
            "2.01",
            "2.9");
        for (String locale : locales) {
            if ("root".equals(locale) || locale.contains("_")) {
                continue;
            }
            PluralRules rules = supplemental.getPlurals(locale).getPluralRules();
            PluralMinimalPairs samplePatterns = PluralMinimalPairs.getInstance(locale);
            if (samplePatterns.isEmpty(PluralType.CARDINAL)) {
                continue;
            }
            Set<FixedDecimal> samples = new TreeSet<>();

            Set<String> keywords = rules.getKeywords();
            // header
            System.out.print(
                locale
                    + "\t" + "Number"
                    + "\t" + "Cat."
                    + "\t" + "Sample"
                    + "\t" + "Replacement for Sample");
            for (String keyword : keywords) {
                System.out.print("\t" + Count.valueOf(keyword));
                HashSet<Double> items = new HashSet<>(rules.getSamples(keyword, SampleType.INTEGER));
                for (int i = 0; i < 5; ++i) {
                    items.add(i + 0d);
                    items.add(i + 10d);
                    items.add(i + 20d);
                    items.add(i + 100d);
                    items.add(i + 110d);
                }
                for (Double sample : items) {
                    FixedDecimal fd = new FixedDecimal(sample);
                    samples.add(fd);
                }
                for (String sample : sampleStrings) {
                    FixedDecimal fd = new FixedDecimal(sample);
                    samples.add(fd);
                }
            }
            System.out.println();

            for (FixedDecimal number : samples) {
                String cat = rules.select(number);
                String sample = samplePatterns.get(PluralType.CARDINAL, Count.valueOf(cat));
                System.out.print(
                    locale
                        + "\t" + " " + number
                        + "\t" + cat
                        + "\t" + sample.replace("{0}", number.toString())
                        + "\t" + "«replace if Sample wrong»");
                for (String keyword : keywords) {
                    String sample2 = samplePatterns.get(PluralType.CARDINAL, Count.valueOf(keyword));
                    System.out.print("\t" + sample2.replace("{0}", number.toString()));
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    private static void writeRanges(String title, Set<String> locales, Multimap<Set<String>, String> missingMinimalPairs) {
        for (String locale : locales) {
            if ("root".equals(locale) || locale.contains("_")) {
                continue;
            }
            PluralRules rules = supplemental.getPlurals(locale).getPluralRules();
            String rangePattern;
            try {
                rangePattern = factory.make(locale, true).getStringValue("//ldml/numbers/miscPatterns[@numberSystem=\"latn\"]/pattern[@type=\"range\"]");
            } catch (Exception e) {
                missingMinimalPairs.put(rules.getKeywords(), locale);
                continue;
            }
            PluralMinimalPairs samplePatterns = PluralMinimalPairs.getInstance(locale);
            if (samplePatterns.isEmpty(PluralType.CARDINAL)) {
                missingMinimalPairs.put(rules.getKeywords(), locale);
                continue;
            }
            Set<String> keywords = rules.getKeywords();
            PluralRanges pluralRanges = supplemental.getPluralRanges(locale);
            for (String start : keywords) {
                FixedDecimal small = getSample(rules, start, null); // smallest
                String startPattern = getSamplePattern(samplePatterns, start);
                for (String end : keywords) {
                    FixedDecimal large = getSample(rules, end, small); // smallest
                    if (large == null) {
                        continue;
                    }
                    String endPattern = getSamplePattern(samplePatterns, end);
                    String range = MessageFormat.format(rangePattern, small.toString(), large.toString());
                    Count rangeCount = pluralRanges == null ? null : pluralRanges.get(Count.valueOf(start), Count.valueOf(end));
                    String rangeCountPattern = rangeCount == null ? "<copy correct pattern>" : getSamplePattern(samplePatterns, rangeCount.toString());
                    System.out.println(title
                        + "\t" + getName(locale)
                        + "\t" + start + "—" + end
                        + "\t" + (rangeCount == null ? "?" : rangeCount.toString())
                        + "\t" + (rangeCountPattern.contains("{0}") ? rangeCountPattern.replace("{0}", range) : rangeCountPattern)
                        + "\t" + (startPattern.contains("{0}") ? startPattern.replace("{0}", range) : "?")
                        + "\t" + (endPattern.contains("{0}") ? endPattern.replace("{0}", range) : "?"));
                }
            }
            System.out.println();
        }
    }

    private static String getName(String missing) {
        return missing + "\t" + CLDRConfig.getInstance().getEnglish().getName(missing);
    }

    private static String getSamplePattern(PluralMinimalPairs samplePatterns, String start) {
        return samplePatterns.get(PluralType.CARDINAL, Count.valueOf(start));
    }

    private static FixedDecimal getSample(PluralRules rules, String start, FixedDecimal minimum) {
        FixedDecimal result = getSample(rules, start, SampleType.INTEGER, minimum);
        FixedDecimal result2 = getSample(rules, start, SampleType.DECIMAL, minimum);
        if (result == null) {
            return result2;
        }
        return result;
    }

    private static FixedDecimal getSample(PluralRules rules, String start, SampleType sampleType, FixedDecimal minimum) {
        FixedDecimalSamples samples = rules.getDecimalSamples(start, sampleType);
        if (samples == null) {
            return null;
        }
        Set<FixedDecimalRange> samples2 = samples.getSamples();
        if (samples2 == null) {
            return null;
        }
        for (FixedDecimalRange sample : samples2) {
            if (minimum == null) {
                return sample.start;
            } else if (minimum.getSource() < sample.start.getSource()) {
                return sample.start;
            } else if (minimum.getSource() < sample.end.getSource()) {
                return sample.end;
            }
        }
        return null;
    }
}
