package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PluralRanges;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class GeneratePluralRanges {
    public GeneratePluralRanges(SupplementalDataInfo supplementalDataInfo) {
        SUPPLEMENTAL = supplementalDataInfo;
        prf = PluralRulesFactory.getInstance(SUPPLEMENTAL);
    }

    private static final boolean MINIMAL = true;

    public static void main(String[] args) {
        CLDRConfig testInfo = ToolConfig.getToolInstance();
        GeneratePluralRanges me = new GeneratePluralRanges(testInfo.getSupplementalDataInfo());
        me.reformatPluralRanges();
        //me.generateSamples(testInfo.getEnglish(), testInfo.getCldrFactory());
    }

    private void generateSamples(CLDRFile english, Factory factory) {
        //Map<ULocale, PluralRulesFactory.SamplePatterns> samples = PluralRulesFactory.getLocaleToSamplePatterns();
        // add all the items with plural ranges
        Set<String> sorted = new TreeSet<String>(SUPPLEMENTAL.getPluralRangesLocales());
        // add the core locales
//        sorted.addAll(StandardCodes.make().getLocaleCoverageLocales("google", EnumSet.of(Level.MODERN)));
        sorted.addAll(StandardCodes.make().getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN)));
        // add any variant plural forms
        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : SUPPLEMENTAL.getPluralLocales()) {
            if (locale.contains("_")) {
                if (sorted.contains(ltp.set(locale).getLanguage())) {
                    sorted.add(locale);
                }
            }
        }
        //sorted.add("fil");
        System.out.println("Co.\tLocale Name\tStart\tEnd\tResult\tStart Sample\tEnd Sample\tStart Example\tEnd Example\tCombined Example");
        for (String locale : sorted) {
            PluralInfo pluralInfo3 = SUPPLEMENTAL.getPlurals(locale);
            if (locale.contains("_")) {
                PluralInfo pluralInfo2 = SUPPLEMENTAL.getPlurals(ltp.set(locale).getLanguage());
                if (pluralInfo2.equals(pluralInfo3)) {
                    continue;
                }
            }

            Set<Count> counts3 = pluralInfo3.getCounts();
            if (counts3.size() == 1) {
                continue; // skip japanese, etc.
            }

            List<RangeSample> list = getRangeInfo(factory.make(locale, true));
            if (list == null) {
                System.out.println("Failure with " + locale);
                continue;
            }
            for (RangeSample rangeSample : list) {
                System.out.println(locale + "\t" + english.getName(locale)
                    + "\t" + rangeSample.start
                    + "\t" + rangeSample.end
                    + "\t" + (rangeSample.result == null ? "missing" : rangeSample.result)
                    + "\t" + rangeSample.min
                    + "\t" + rangeSample.max
                    + "\t" + rangeSample.startExample
                    + "\t" + rangeSample.endExample
                    + "\t" + rangeSample.resultExample);
            }
        }
    }

    public List<RangeSample> getRangeInfo(CLDRFile cldrFile) {
        String locale = cldrFile.getLocaleID();
        if (locale.equals("iw")) {
            locale = "he";
        }
        //Map<ULocale, PluralRulesFactory.SamplePatterns> samples = PluralRulesFactory.getLocaleToSamplePatterns();
        List<RangeSample> list = new ArrayList<RangeSample>();
        PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(locale);
        Set<Count> counts = pluralInfo.getCounts();
        PluralRanges pluralRanges = SUPPLEMENTAL.getPluralRanges(locale);
        if (pluralRanges == null && locale.contains("_")) {
            String locale2 = new ULocale(locale).getLanguage();
            pluralRanges = SUPPLEMENTAL.getPluralRanges(locale2);
        }
        if (pluralRanges == null) {
            return null;
        }
        ULocale ulocale = new ULocale(locale);
        PluralMinimalPairs samplePatterns = PluralMinimalPairs.getInstance(ulocale.toString()); // CldrUtility.get(samples, ulocale);
//        if (samplePatterns == null && locale.contains("_")) {
//            ulocale = new ULocale(ulocale.getLanguage());
//            samplePatterns = CldrUtility.get(samples, ulocale);
//            if (samplePatterns == null) {
//                return null;
//            }
//        }

        Output<FixedDecimal> maxSample = new Output<FixedDecimal>();
        Output<FixedDecimal> minSample = new Output<FixedDecimal>();

        ICUServiceBuilder icusb = new ICUServiceBuilder();
        icusb.setCldrFile(cldrFile);
        DecimalFormat nf = icusb.getNumberFormat(1);
        //String decimal = cldrFile.getWinningValue("//ldml/numbers/symbols[@numberSystem=\"latn\"]/decimal");
        String defaultNumberingSystem = cldrFile.getWinningValue("//ldml/numbers/defaultNumberingSystem");
        String range = cldrFile.getWinningValue("//ldml/numbers/miscPatterns[@numberSystem=\""
            + defaultNumberingSystem
            + "\"]/pattern[@type=\"range\"]");

        //            if (decimal == null) {
        //                throw new IllegalArgumentException();
        //            }
        for (Count s : counts) {
            for (Count e : counts) {
                if (!pluralInfo.rangeExists(s, e, minSample, maxSample)) {
                    continue;
                }
                Count r = pluralRanges.getExplicit(s, e);
                String minFormatted = format(nf, minSample.value);
                String maxFormatted = format(nf, maxSample.value);
                String rangeFormatted = MessageFormat.format(range, minFormatted, maxFormatted);

                list.add(new RangeSample(
                    s, e, r,
                    minSample.value,
                    maxSample.value,
                    getExample(locale, samplePatterns, s, minFormatted), getExample(locale, samplePatterns, e, maxFormatted),
                    getExample(locale, samplePatterns, r, rangeFormatted)));
            }
        }
        return list;
    }

    public static class RangeSample {
        // Category Examples    Minimal Pairs   Rules
        public RangeSample(Count start, Count end, Count result,
            FixedDecimal min, FixedDecimal max,
            String startExample, String endExample, String resultExample) {
            this.start = start;
            this.end = end;
            this.result = result;
            this.min = min;
            this.max = max;
            this.startExample = startExample;
            this.endExample = endExample;
            this.resultExample = resultExample;
        }

        final Count start;
        final Count end;
        final Count result;
        final FixedDecimal min;
        final FixedDecimal max;
        final String startExample;
        final String endExample;
        final String resultExample;
    }

    public static String format(DecimalFormat nf, FixedDecimal minSample) {
        nf.setMinimumFractionDigits(minSample.getVisibleDecimalDigitCount());
        nf.setMaximumFractionDigits(minSample.getVisibleDecimalDigitCount());
        return nf.format(minSample);
    }

    //    private String format(String decimal, Output<FixedDecimal> minSample) {
    //        return minSample.toString().replace(".", decimal);
    //    }

    public static String getExample(String locale, PluralMinimalPairs samplePatterns, Count r, String numString) {
        if (r == null) {
            return "«missing»";
        }
        String samplePattern;
        try {
            samplePattern = samplePatterns.get(PluralRules.PluralType.CARDINAL, r); // CldrUtility.get(samplePatterns.keywordToPattern, r);
        } catch (Exception e) {
            throw new IllegalArgumentException("Locale: " + locale + "; Count: " + r, e);
        }
        return samplePattern
            .replace('\u00A0', '\u0020')
            .replace("{0}", numString);
    }

    private final SupplementalDataInfo SUPPLEMENTAL;
    private final PluralRulesFactory prf;

    public static final Comparator<Set<String>> STRING_SET_COMPARATOR = new SetComparator<String, Set<String>>();
    public static final Comparator<Set<Count>> COUNT_SET_COMPARATOR = new SetComparator<Count, Set<Count>>();

    static final class SetComparator<T extends Comparable<T>, U extends Set<T>> implements Comparator<U> {
        public int compare(U o1, U o2) {
            return CollectionUtilities.compare((Collection<T>) o1, (Collection<T>) o2);
        }
    };

    public void reformatPluralRanges() {
        Map<Set<Count>, Relation<Set<String>, String>> seen = new TreeMap<Set<Count>, Relation<Set<String>, String>>(COUNT_SET_COMPARATOR);

        for (String locale : SUPPLEMENTAL.getPluralRangesLocales()) {

            PluralRanges pluralRanges = SUPPLEMENTAL.getPluralRanges(locale);
            PluralInfo pluralInfo = SUPPLEMENTAL.getPlurals(locale);
            Set<Count> counts = pluralInfo.getCounts();

            Set<String> s;
            if (false) {
                System.out.println("Minimized, but not ready for prime-time");
                s = minimize(pluralRanges, pluralInfo);
            } else {
                s = reformat(pluralRanges, counts);
            }
            Relation<Set<String>, String> item = seen.get(counts);
            if (item == null) {
                seen.put(counts,
                    item = Relation.of(new TreeMap<Set<String>, Set<String>>(STRING_SET_COMPARATOR), TreeSet.class));
            }
            item.put(s, locale);
        }

        for (Entry<Set<Count>, Relation<Set<String>, String>> entry0 : seen.entrySet()) {
            System.out.println("\n<!-- " + CollectionUtilities.join(entry0.getKey(), ", ") + " -->");
            for (Entry<Set<String>, Set<String>> entry : entry0.getValue().keyValuesSet()) {
                System.out.println("\t\t<pluralRanges locales=\"" + CollectionUtilities.join(entry.getValue(), " ") + "\">");
                for (String line : entry.getKey()) {
                    System.out.println("\t\t\t" + line);
                }
                System.out.println("\t\t</pluralRanges>");
            }
        }
    }

    enum RangeStrategy {
        other, end, start, mixed
    }

    public Set<String> reformat(PluralRanges pluralRanges, Set<Count> counts) {
        Set<String> s;
        s = new LinkedHashSet<String>();
        // first determine the general principle

        //        EnumSet<RangeStrategy> strategy = EnumSet.allOf(RangeStrategy.class);
        //        Count firstResult = null;
        //        for (Count start : counts) {
        //            for (Count end : counts) {
        //                Count result = pluralRanges.getExplicit(start, end);
        //                if (result == null) {
        //                    continue;
        //                } else if (firstResult == null) {
        //                    firstResult = result;
        //                }
        //                if (result != start) {
        //                    strategy.remove(RangeStrategy.start);
        //                }
        //                if (result != end) {
        //                    strategy.remove(RangeStrategy.end);
        //                }
        //                if (result != Count.other) {
        //                    strategy.remove(RangeStrategy.other);
        //                }
        //           }
        //        }
        //        s.add("<!-- Range Principle: " + strategy.iterator().next() + " -->");
        for (Count start : counts) {
            for (Count end : counts) {
                Count result = pluralRanges.getExplicit(start, end);
                if (result == null) {
                    continue;
                }
                String line = PluralRanges.showRange(start, end, result);
                s.add(line);
            }
        }
        return s;
    }

    Set<String> minimize(PluralRanges pluralRanges, PluralInfo pluralInfo) {
        Set<String> result = new LinkedHashSet<String>();
        // make it easier to manage
        PluralRanges.Matrix matrix = new PluralRanges.Matrix();
        Output<FixedDecimal> maxSample = new Output<FixedDecimal>();
        Output<FixedDecimal> minSample = new Output<FixedDecimal>();
        for (Count s : Count.VALUES) {
            for (Count e : Count.VALUES) {
                if (!pluralInfo.rangeExists(s, e, minSample, maxSample)) {
                    continue;
                }
                Count r = pluralRanges.getExplicit(s, e);
                matrix.set(s, e, r);
            }
        }
        // if everything is 'other', we are done
        //        if (allOther == true) {
        //            return result;
        //        }
        EnumSet<Count> endDone = EnumSet.noneOf(Count.class);
        EnumSet<Count> startDone = EnumSet.noneOf(Count.class);
        if (MINIMAL) {
            for (Count end : pluralInfo.getCounts()) {
                Count r = matrix.endSame(end);
                if (r != null
                //&& r != Count.other
                ) {
                    result.add("<pluralRange" +
                        "              \t\tend=\"" + end
                        + "\"\tresult=\"" + r + "\"/>");
                    endDone.add(end);
                }
            }
            Output<Boolean> emit = new Output<Boolean>();
            for (Count start : pluralInfo.getCounts()) {
                Count r = matrix.startSame(start, endDone, emit);
                if (r != null
                // && r != Count.other
                ) {
                    if (emit.value) {
                        result.add("<pluralRange" +
                            "\tstart=\"" + start
                            + "\"          \t\tresult=\"" + r + "\"/>");
                    }
                    startDone.add(start);
                }
            }
        }
        //Set<String> skip = new LinkedHashSet<String>();
        for (Count end : pluralInfo.getCounts()) {
            if (endDone.contains(end)) {
                continue;
            }
            for (Count start : pluralInfo.getCounts()) {
                if (startDone.contains(start)) {
                    continue;
                }
                Count r = matrix.get(start, end);
                if (r != null
                //&& !(MINIMAL && r == Count.other)
                ) {
                    result.add(PluralRanges.showRange(start, end, r));
                } else {
                    result.add("<!-- <pluralRange" +
                        "\tstart=\"" + start
                        + "\" \tend=\"" + end
                        + "\" \tresult=\"" + r + "\"/> -->");

                }

            }
        }
        return result;
    }

}
