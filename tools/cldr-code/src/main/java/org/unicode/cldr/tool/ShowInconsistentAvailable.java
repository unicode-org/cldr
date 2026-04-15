package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.UnmodifiableIterator;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.text.DateIntervalInfo;
import com.ibm.icu.text.DateIntervalInfo.PatternInfo;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.DecimalQuantitySamples;
import com.ibm.icu.text.PluralRules.DecimalQuantitySamplesRange;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.PluralRules.PluralType;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.DatetimeUtilities;
import org.unicode.cldr.util.DatetimeUtilities.DatePatternInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.NestedMap;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PluralRanges;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.XPathParts;

public class ShowInconsistentAvailable {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final Factory MAIN_FACTORY = CONFIG.getCldrFactory();
    static boolean INCLUDE_ERA = true;
    static boolean SHOW_PROGRESS_RAW = false;
    static boolean SHOW_PROGRESS = false;
    static String DEBUG_ONLY_CALENDAR = null; // "chinese"; // null == all
    static SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();

    static FormatParser fp = new DateTimePatternGenerator.FormatParser();
    static PathHeader.Factory phf = PathHeader.getFactory();
    static int counter = 0;
    static Set<String> nullErrors = new LinkedHashSet<>();

    private enum MyOptions {
        checkPluralRanges(new Params()),
        ordering(
                new Params()
                        .setHelp("find the ordering of fields in availableFormats and intervals")),
        inconsistencies(new Params().setHelp("find inconsistancies in available formatts")),
        badSkeletons(new Params().setHelp("bad skeletons")),
        root(new Params().setHelp("find root paths")),
        seperatorsCheck(new Params());

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Options myOptions = new Options();

        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        MyOptions.parse(args);
        Set<String> cldrLocales = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr);
        if (MyOptions.ordering.option.doesOccur()) {
            showOrdering(cldrLocales);
        }
        if (MyOptions.root.option.doesOccur()) {
            getRootPaths();
        }
        if (MyOptions.inconsistencies.option.doesOccur()) {
            showInconsistencies(cldrLocales);
        }
        if (MyOptions.badSkeletons.option.doesOccur()) {
            badSkeletons();
        }
        if (MyOptions.checkPluralRanges.option.doesOccur()) {
            checkPluralRanges();
        }
        if (MyOptions.seperatorsCheck.option.doesOccur()) {
            checkSeparators();
        }
    }

    static class SeparatorData {
        public SeparatorData(Collection<String> patterns) {
            for (String pattern : patterns) {
                // We capture the literals from the following triples (in either order)
                // YEAR literal MONTH-numeric
                // YEAR literal DAY
                // MONTH-numeric literal DAY
                // HOUR literal MINUTE
                // HOUR literal SECOND // probably never occurs!
                // MINUTE literal SECOND
                String lastLiteral = "";
                int lastType = -1; // ignore last type
                for (Object item : fp.set(pattern).getItems()) {
                    if (item instanceof DateTimePatternGenerator.VariableField) {
                        VariableField2 v = new VariableField2(item, true);
                        if (!v.isNumeric()) {
                            lastType = -1; // ignore last type
                        } else { // non-numeric
                            int currentType = v.getType();
                            switch (currentType) {
                                case DateTimePatternGenerator.YEAR:
                                    if (lastType == DateTimePatternGenerator.MONTH) {
                                        yMSeparatorToPatterns.put(lastLiteral, pattern);
                                    } else if (lastType == DateTimePatternGenerator.DAY) {
                                        ydSeparatorToPatterns.put(lastLiteral, pattern);
                                    }
                                    lastType = currentType;
                                    break;
                                case DateTimePatternGenerator.MONTH:
                                    if (lastType == DateTimePatternGenerator.YEAR) {
                                        yMSeparatorToPatterns.put(lastLiteral, pattern);
                                    } else if (lastType == DateTimePatternGenerator.DAY) {
                                        MdSeparatorToPatterns.put(lastLiteral, pattern);
                                    }
                                    lastType = currentType;
                                    break;
                                case DateTimePatternGenerator.DAY:
                                    if (lastType == DateTimePatternGenerator.YEAR) {
                                        ydSeparatorToPatterns.put(lastLiteral, pattern);
                                    } else if (lastType == DateTimePatternGenerator.MONTH) {
                                        MdSeparatorToPatterns.put(lastLiteral, pattern);
                                    }
                                    lastType = currentType;
                                    break;
                                case DateTimePatternGenerator.HOUR:
                                    if (lastType == DateTimePatternGenerator.MINUTE) {
                                        hmSeparatorToPatterns.put(lastLiteral, pattern);
                                    } else if (lastType == DateTimePatternGenerator.SECOND) {
                                        hsSeparatorToPatterns.put(lastLiteral, pattern);
                                    }
                                    lastType = currentType;
                                    break;
                                case DateTimePatternGenerator.MINUTE:
                                    if (lastType == DateTimePatternGenerator.HOUR) {
                                        hmSeparatorToPatterns.put(lastLiteral, pattern);
                                    } else if (lastType == DateTimePatternGenerator.SECOND) {
                                        msSeparatorToPatterns.put(lastLiteral, pattern);
                                    }
                                    lastType = currentType;
                                    break;
                                case DateTimePatternGenerator.SECOND:
                                    if (lastType == DateTimePatternGenerator.HOUR) {
                                        hsSeparatorToPatterns.put(lastLiteral, pattern);
                                    } else if (lastType == DateTimePatternGenerator.MINUTE) {
                                        msSeparatorToPatterns.put(lastLiteral, pattern);
                                    }
                                    lastType = currentType;
                                    break;
                                default:
                                    lastType = -1;
                                    break;
                            }
                        }
                        lastLiteral = "";
                    } else {
                        lastLiteral = item.toString();
                    }
                }
            }
        }

        final Multimap<String, String> yMSeparatorToPatterns = TreeMultimap.create();
        final Multimap<String, String> ydSeparatorToPatterns = TreeMultimap.create();
        final Multimap<String, String> MdSeparatorToPatterns = TreeMultimap.create();

        final Multimap<String, String> hmSeparatorToPatterns = TreeMultimap.create();
        final Multimap<String, String> hsSeparatorToPatterns =
                TreeMultimap.create(); // probably empty!!
        final Multimap<String, String> msSeparatorToPatterns = TreeMultimap.create();

        @Override
        public String toString() {
            return Joiners.TAB.join(
                    "yM:", yMSeparatorToPatterns, //  
                    "yd:", ydSeparatorToPatterns, //  
                    "Md:", MdSeparatorToPatterns, //  
                    "hm:", hmSeparatorToPatterns, //  
                    "hs:", hsSeparatorToPatterns, //  
                    "ms:", msSeparatorToPatterns //  
                    );
        }

        public String summary() {
            return Joiners.TAB.join(
                    "yM:", yMSeparatorToPatterns.keySet(), //  
                    "yd:", ydSeparatorToPatterns.keySet(), //  
                    "Md:", MdSeparatorToPatterns.keySet(), //  
                    "hm:", hmSeparatorToPatterns.keySet(), //  
                    "hs:", hsSeparatorToPatterns.keySet(), //  
                    "ms:", msSeparatorToPatterns.keySet() //  
                    );
        }
    }

    private static void checkSeparators() {
        // quick check
        check("h:m:s", "y/M/d");
        check("k:m?s", "M-dy");

        Set<String> locales =
                StandardCodes.make()
                        .getLocaleCoverageLocales(
                                Organization.cldr,
                                Set.of(Level.MODERN, Level.MODERATE, Level.BASIC));
        List<String> details = new ArrayList<>();
        for (String locale : locales) {
            CLDRFile unresolvedCfile = MAIN_FACTORY.make(locale, false);
            Map<String, DatePatternInfo> unresolvedmap =
                    DatetimeUtilities.calendarToDatePatternInfo(unresolvedCfile);
            if (unresolvedmap.isEmpty()) {
                continue;
            }
            CLDRFile cfile = MAIN_FACTORY.make(locale, true);
            Map<String, DatePatternInfo> map = DatetimeUtilities.calendarToDatePatternInfo(cfile);
            Map<String, SeparatorData> calendarToSeparatorData = new LinkedHashMap<>();
            for (String calendar :
                    unresolvedmap
                            .keySet()) { // only look at calendars that this file has explicitly
                DatePatternInfo info = map.get(calendar);
                Map<String, String> skeletonToPattern = info.getAvailableSkeletonToPattern();
                Map<String, String> stockToPattern = info.getStockSkeletonToPattern();
                Set<String> patterns = new TreeSet<>();
                patterns.addAll(skeletonToPattern.values());
                patterns.addAll(stockToPattern.values());
                calendarToSeparatorData.put(calendar, new SeparatorData(patterns));
            }
            String localeName = ng.getNameFromIdentifier(locale);
            for (Entry<String, SeparatorData> entry : calendarToSeparatorData.entrySet()) {
                System.out.println(
                        Joiners.TAB.join(
                                localeName, locale, entry.getKey(), entry.getValue().summary()));
                details.add(Joiners.TAB.join(localeName, locale, entry.getKey(), entry.getValue()));
            }
        }
        System.out.println("\nDETAILS\n");
        details.stream().forEach(System.out::println);
    }

    private static void check(String... patterns) {
        List<String> list = Arrays.asList(patterns);
        System.out.println(list + "\n" + new SeparatorData(list));
    }

    static final NameGetter ng = new NameGetter(CONFIG.getEnglish());

    private static void checkPluralRanges() {
        Set<String> locales =
                StandardCodes.make()
                        .getLocaleCoverageLocales(Organization.cldr, Set.of(Level.MODERN));

        for (String locale : locales) {
            String parent = CLDRLocale.getInstance(locale).getParent().toString();
            if (locales.contains(parent)) {
                continue;
            }
            final MinimalExample minimalSample = new MinimalExample(locale);
            final PluralRules rules = SDI.getPluralRules(locale, PluralType.CARDINAL);
            final PluralRanges ranges = SDI.getPluralRanges(locale);
            final Set<String> keywords = rules.getKeywords();
            boolean onlyOther = keywords.size() == 1;
            String localeName = ng.getNameFromIdentifier(locale);
            for (String keyword1 : keywords) {
                final Count keyCount1 = Count.valueOf(keyword1);
                boolean hasSingletonValue = minimalSample.countToSamples.get(keyCount1).size() == 1;
                String sample1 =
                        minimalSample.formatSample(keyCount1, MinimalExample.SampleType.first);
                for (String keyword2 : keywords) {
                    final Count keyCount2 = Count.valueOf(keyword2);
                    System.out.print(Joiners.TAB.join(localeName, locale, keyword1, keyword2));
                    if (keyCount2 == keyCount1 && (hasSingletonValue || onlyOther)) {
                        System.out.println(Joiners.TAB.join("", "UNNECESSARY"));
                        continue;
                    }
                    String sample2 =
                            minimalSample.formatSample(keyCount2, MinimalExample.SampleType.second);
                    final Count explicit =
                            ranges == null ? null : ranges.getExplicit(keyCount1, keyCount2);
                    String sample3 = minimalSample.formatSamples(keyCount1, keyCount2);
                    if (minimalSample.minimalPairs.isEmpty()) {
                        System.out.println(
                                Joiners.TAB.join(
                                        "",
                                        explicit == null ? "N/A" : explicit,
                                        "NO MINIMAL PAIRS"));

                    } else {
                        System.out.println(
                                Joiners.TAB.join(
                                        "",
                                        explicit == null ? "N/A" : explicit,
                                        minimalSample.withPattern(sample1, keyCount1),
                                        minimalSample.withPattern(sample2, keyCount2),
                                        minimalSample.withPattern(sample3, explicit)));
                    }
                }
            }
        }
    }

    public static class MinimalExample {
        public enum SampleType {
            first,
            second
        }

        private final Map<Count, String> minimalPairs;
        private final PluralRules rules;
        private final ImmutableMultimap<Count, DecimalQuantity> countToSamples;
        private final ULocale locale;
        private final String defaultNumberingSystem;
        private String rangePattern;

        public MinimalExample(String locale) {
            this.locale = new ULocale(locale);
            CLDRFile cldrFile = CONFIG.getCLDRFile(locale, true);
            Map<Count, String> countToPattern = new TreeMap<>();
            String _defaultNumberingSystem = "latn";
            Map<String, String> numberSystemToRangePattern = new HashMap<>();
            for (String path : cldrFile) {
                if (path.contains("/pluralMinimalPairs")) {
                    // <pluralMinimalPairs count="one">{0} day</pluralMinimalPairs>
                    XPathParts parts = XPathParts.getFrozenInstance(path);
                    String count = parts.getAttributeValue(-1, "count");
                    String value = cldrFile.getStringValue(path);
                    countToPattern.put(Count.valueOf(count), value);
                } else if (path.contains("/miscPatterns")) {
                    XPathParts parts = XPathParts.getFrozenInstance(path);
                    String numberSystem = parts.getAttributeValue(-2, "numberSystem");
                    String type = parts.getAttributeValue(-1, "type");
                    // <miscPatterns numberSystem="latn"><pattern type="range">↑↑↑</pattern>
                    if ("range".equals(type)) {
                        numberSystemToRangePattern.put(numberSystem, cldrFile.getStringValue(path));
                    }
                } else if (path.contains("/defaultNumberingSystem")) {
                    _defaultNumberingSystem = cldrFile.getStringValue(path);
                    // <defaultNumberingSystem>beng</defaultNumberingSystem>
                }
            }
            defaultNumberingSystem = _defaultNumberingSystem;
            rangePattern = numberSystemToRangePattern.get(defaultNumberingSystem);
            minimalPairs = ImmutableMap.copyOf(countToPattern);
            rules = SDI.getPluralRules(locale, PluralType.CARDINAL);
            Multimap<Count, DecimalQuantity> _countToSamples = LinkedHashMultimap.create();
            for (String key : rules.getKeywords()) {
                Count keyCount = Count.valueOf(key);
                Map<Double, DecimalQuantity> samples = getInternalSamples(keyCount);
                if (samples == null) {
                    throw new IllegalArgumentException();
                }
                _countToSamples.putAll(keyCount, samples.values());
            }
            countToSamples = ImmutableMultimap.copyOf(_countToSamples);
        }

        public String withPattern(String numeric, Count keyCount) {
            String pattern = keyCount == null ? null : minimalPairs.get(keyCount);
            if (pattern == null) {
                pattern = "{0} 𝐍/𝐀";
            }
            return pattern.replace("{0}", numeric);
        }

        public String formatSample(Count keyCount, SampleType sampleType) {
            ImmutableCollection<DecimalQuantity> samples = countToSamples.get(keyCount);
            UnmodifiableIterator<DecimalQuantity> it1 = samples.iterator();
            DecimalQuantity quant = it1.next();
            if (sampleType == SampleType.second && it1.hasNext()) {
                quant = it1.next();
            }
            return format(quant);
        }

        public String formatSamples(Count keyCount1, Count keyCount2) {
            ImmutableCollection<DecimalQuantity> samples1 = countToSamples.get(keyCount1);
            ImmutableCollection<DecimalQuantity> samples2 = countToSamples.get(keyCount2);
            UnmodifiableIterator<DecimalQuantity> it1 = samples1.iterator();
            String s1 = format(it1.next());
            String s2;
            // if they are the same keycount, we want to get the second one from the iterator, if
            // there is one
            if (keyCount1 == keyCount2 && it1.hasNext()) {
                s2 = format(it1.next());
            } else {
                s2 = format(samples2.iterator().next());
            }
            return rangePattern.replace("{0}", s1).replace("{1}", s2); // {0}–{1}
        }

        private Map<Double, DecimalQuantity> getInternalSamples(Count keyCount1) {
            Map<Double, DecimalQuantity> samples = new LinkedHashMap<>();
            fromSamples(keyCount1, PluralRules.SampleType.INTEGER, samples);
            fromSamples(keyCount1, PluralRules.SampleType.DECIMAL, samples);
            return samples;
        }

        public void fromSamples(
                Count keyCount1,
                PluralRules.SampleType sampleType,
                Map<Double, DecimalQuantity> result) {
            if (result.size() >= 2) {
                return;
            }
            DecimalQuantitySamples samples =
                    rules.getDecimalSamples(keyCount1.toString(), sampleType);
            if (samples != null) {
                Iterator<DecimalQuantitySamplesRange> integerSamples =
                        samples == null ? null : samples.getSamples().iterator();
                while (integerSamples.hasNext()) {
                    DecimalQuantitySamplesRange range = integerSamples.next();
                    add(result, range.start);
                    if (result.size() >= 2) {
                        return;
                    }
                    add(result, range.end);
                    if (result.size() >= 2) {
                        return;
                    }
                }
            }
        }

        // We don't add an item if there is already one that has the same double value
        private void add(Map<Double, DecimalQuantity> result, DecimalQuantity item) {
            double temp = item.toDouble();
            if (!result.containsKey(temp)) {
                result.put(temp, item);
            }
        }

        private String format(DecimalQuantity decimalQuantity) {
            java.math.BigDecimal value = decimalQuantity.toBigDecimal();
            int visibleDecimals = (int) decimalQuantity.getPluralOperand(Operand.v);
            String result =
                    NumberFormatter.withLocale(locale)
                            .precision(Precision.minMaxFraction(visibleDecimals, visibleDecimals))
                            .format(value)
                            .toString();
            // System.out.println(value + "\t" + result);
            return result;
        }
    }

    private static void badSkeletons() {
        Set<String> locales = new LinkedHashSet<>();
        locales.add("root");
        locales.addAll(MAIN_FACTORY.getAvailable());
        Multimap<String, String> idToCalendar = TreeMultimap.create();
        NestedMap.Multimap2<String, String, String> idToCalendarToLocales =
                NestedMap.Multimap2.create(TreeMap::new);

        Multimap<String, String> typeToCalendar = TreeMultimap.create();
        NestedMap.Multimap2<String, String, String> typeToCalendarToLocales =
                NestedMap.Multimap2.create(TreeMap::new);

        // locales = Set.of("en", "ja", "fi"); // testing
        String lastLang = "";
        for (String locale : locales) {
            CLDRFile cldrFile = MAIN_FACTORY.make(locale, false);
            Map<String, DatePatternInfo> map =
                    DatetimeUtilities.calendarToDatePatternInfo(cldrFile); // unresolved
            if (map.isEmpty()) continue;

            CLDRLocale clocale = CLDRLocale.getInstance(locale);
            String lang = clocale.getLanguage();
            if (!lang.equals(lastLang)) {
                System.out.println(locale);
                lastLang = lang;
            }

            for (Entry<String, DatePatternInfo> entry : map.entrySet()) {
                String calendar = entry.getKey();
                DatePatternInfo datePatternInfo = entry.getValue();
                Map<String, String> available = datePatternInfo.getAvailableSkeletonToPattern();
                available.keySet().stream()
                        .forEach(
                                id -> {
                                    idToCalendar.put(id, calendar);
                                    idToCalendarToLocales.put(id, calendar, locale);
                                });
                Map<String, String> stockToPattern = datePatternInfo.getStockSkeletonToPattern();
                stockToPattern.keySet().stream()
                        .forEach(
                                type -> {
                                    typeToCalendar.put(type, calendar);
                                    typeToCalendarToLocales.put(type, calendar, locale);
                                });
            }
        }
        Map<String, Multimap<String, String>> reformatted = extracted(idToCalendarToLocales);
        reformatted.entrySet().stream()
                .forEach(
                        x -> {
                            String pattern = x.getKey();
                            Map<String, Collection<String>> mmm = x.getValue().asMap();
                            List<String> causes = new ArrayList<>();
                            System.out.println(
                                    Joiners.TAB.join(
                                            "A",
                                            pattern,
                                            DatetimeUtilities.normalizePattern(pattern, causes),
                                            causes,
                                            format(mmm)));
                        });

        Map<String, Multimap<String, String>> reformatted2 = extracted(typeToCalendarToLocales);
        reformatted2.entrySet().stream()
                .forEach(
                        x -> {
                            String pattern = x.getKey();
                            Map<String, Collection<String>> mmm = x.getValue().asMap();
                            List<String> causes = new ArrayList<>();

                            System.out.println(
                                    Joiners.TAB.join(
                                            "S",
                                            pattern,
                                            DatetimeUtilities.normalizePattern(pattern, causes),
                                            causes,
                                            format(mmm)));
                        });

        idToCalendar.asMap().entrySet().stream()
                .forEach(x -> System.out.println(Joiners.TAB.join("A", x.getKey(), x.getValue())));

        typeToCalendar.asMap().entrySet().stream()
                .forEach(x -> System.out.println(Joiners.TAB.join("S", x.getKey(), x.getValue())));
        // DatetimeUtilities.missingSkeletonsForLengths.
    }

    private static String format(Map<String, Collection<String>> mmm) {
        return mmm.entrySet().stream()
                .map(y -> y.getKey() + " 🠶 " + Joiners.SP.join(y.getValue()))
                .collect(Collectors.joining(" ⏎ "));
    }

    private static Map<String, Multimap<String, String>> extracted(
            NestedMap.Multimap2<String, String, String> toMapOfMulti) {
        Map<String, Multimap<String, String>> reformatted = new LinkedHashMap<>();
        toMapOfMulti.stream()
                .filter(x -> DatetimeUtilities.idStatus(x.getKey1()) != null)
                .forEach(
                        x -> {
                            Multimap<String, String> mm =
                                    reformatted.computeIfAbsent(
                                            x.getKey1(), y -> TreeMultimap.create());
                            mm.put(x.getKey2(), x.getValue());
                        });
        return reformatted;
    }

    public static void showInconsistencies(final Set<String> cldrLocalesWithoutSpecial) {
        System.out.println(
                "counter, locale, fLocale, calendar, skeleton, alt, coverage, value, sSimple, vSimple, error"
                        .replace(", ", "\t"));
        showInconsistenciesInLocale("root");
        for (String locale : cldrLocalesWithoutSpecial) {
            showInconsistenciesInLocale(locale);
        }
        nullErrors =
                ImmutableSet.copyOf(
                        nullErrors.stream()
                                .map(x -> ++counter + "\t" + x)
                                .collect(Collectors.toList()));
        System.out.println(Joiner.on('\n').join(nullErrors));
    }

    public static void showOrdering(final Set<String> cldrLocalesWithoutSpecial) {
        Multimap<String, SimplePattern> calendarToSPatterns = TreeMultimap.create();
        for (String locale : cldrLocalesWithoutSpecial) {
            getTCPaths(locale, calendarToSPatterns);
        }
        Set<SimplePattern> ts = new TreeSet<>();
        ts.addAll(calendarToSPatterns.values());
        ts = ImmutableSet.copyOf(ts);
        Set<VariableField2> fields = new TreeSet<>();
        for (SimplePattern xx : ts) {
            fields.addAll(xx.internal);
        }
        fields.remove(VARIABLE_FIELD_U);

        for (VariableField2 field : fields) {

            System.out.print("\n∋Var\tCal");
            for (SimplePattern p : ts) {
                if (p.internal.contains(field)) {
                    System.out.print("\t" + p);
                }
            }
            System.out.println();
            for (String cal : calendarToSPatterns.keySet()) {
                System.out.print(SimplePattern.pretty(field) + "\t" + cal);
                for (SimplePattern p : ts) {
                    if (p.internal.contains(field)) {
                        System.out.print(
                                "\t" + (calendarToSPatterns.containsEntry(cal, p) ? "Y" : "-"));
                    }
                }
                System.out.println();
            }
        }
    }

    private static void getTCPaths(
            String locale, Multimap<String, SimplePattern> calendarToSPatterns) {
        CLDRFile cldrFile = MAIN_FACTORY.make(locale, false);
        DateIntervalInfo fInfo = new DateIntervalInfo(new ULocale(locale)).freeze();

        for (String path : cldrFile) {
            String value = cldrFile.getStringValue(path);
            if (value == null || value.isBlank() || value.equals("↑↑↑")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            final String lastElement = parts.getElement(-1);
            switch (lastElement) {
                case "dateFormatItem":
                    String calendar = parts.getAttributeValue(3, "type");
                    SimplePattern valueSimplePattern =
                            new SimplePattern(value, PatternType.pattern);
                    calendarToSPatterns.put(calendar, valueSimplePattern);
                    break;
                case "greatestDifference":
                    calendar = parts.getAttributeValue(3, "type");
                    SimplePattern first = null;
                    SimplePattern second = null;
                    PatternInfo pattern = DateIntervalInfo.genPatternInfo(value, false);
                    try {
                        first = new SimplePattern(pattern.getFirstPart(), PatternType.pattern);
                        second = new SimplePattern(pattern.getSecondPart(), PatternType.pattern);
                    } catch (Exception e) {
                    }
                    if (first == null || second == null) {
                        String skeleton = parts.getAttributeValue(-2, "id");
                        String greatest = parts.getAttributeValue(-1, "id");
                        System.out.println(
                                Joiners.TAB.join(
                                        "Error:",
                                        locale,
                                        calendar,
                                        lastElement,
                                        skeleton,
                                        greatest,
                                        value));
                        continue;
                    }
                    calendarToSPatterns.put(calendar, first);
                    calendarToSPatterns.put(calendar, second);
                    break;
            }
        }
    }

    private static void getRootPaths() {
        Multimap<String, String> skelToCals = TreeMultimap.create();
        Map<Pair<String, String>, String> skelCalToSource = new HashMap<>();
        Set<String> calendars = new TreeSet<>();
        final CLDRFile root = CONFIG.getRoot();

        for (String path : root) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (!parts.getElement(-1).equals("dateFormatItem")) {
                continue;
            }
            String calendar = parts.getAttributeValue(3, "type");
            calendars.add(calendar);
            String skeleton = parts.getAttributeValue(-1, "id");
            String alt = parts.getAttributeValue(-1, "alt");
            if (alt != null) {
                throw new IllegalArgumentException("unexpected");
            }
            skelToCals.put(skeleton, calendar);
            Status out = new Status();
            root.getSourceLocaleID(path, out);
            String source = "none";
            if (out.pathWhereFound != null) {
                XPathParts parts2 = XPathParts.getFrozenInstance(out.pathWhereFound);
                source = parts2.getAttributeValue(3, "type");
            }
            skelCalToSource.put(Pair.of(skeleton, calendar), source);
        }
        System.out.println("skeleton\t" + Joiner.on('\t').join(calendars));
        for (Entry<String, Collection<String>> entry : skelToCals.asMap().entrySet()) {
            final String skeleton = entry.getKey();
            System.out.print(skeleton);
            Collection<String> currentCalendars = entry.getValue();
            for (String calendar : calendars) {
                String source = skelCalToSource.get(Pair.of(skeleton, calendar));
                System.out.print("\t" + (currentCalendars.contains(calendar) ? source : "n/a"));
            }
            System.out.println();
        }
        System.out.println();
    }

    static class PatternData2 {
        List<String> data;

        PatternData2(String... strings) {
            data = Arrays.asList(strings);
        }

        @Override
        public String toString() {
            return Joiners.TAB.join(data);
        }

        @Override
        public boolean equals(Object obj) {
            return data.equals(((PatternData2) obj).data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }
    }

    static void showInconsistenciesInLocale(String locale) {
        CLDRFile cldrFile = MAIN_FACTORY.make(locale, true);
        Status out = new Status();

        Multimap<String, PathHeader> sorted = TreeMultimap.create();

        for (String path : cldrFile) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (!parts.getElement(-1).equals("dateFormatItem")) {
                continue;
            }
            String calendar = parts.getAttributeValue(3, "type");
            if (DEBUG_ONLY_CALENDAR != null && !calendar.equals(DEBUG_ONLY_CALENDAR)) {
                continue;
            }
            if (SHOW_PROGRESS_RAW) {
                String value = cldrFile.getStringValue(path);
                String skeleton = parts.getAttributeValue(-1, "id");
                String alt = parts.getAttributeValue(-1, "alt");
                if (alt == null) {
                    alt = "";
                }
                SimplePattern skeletonSimplePattern =
                        new SimplePattern(skeleton, PatternType.skeleton);
                SimplePattern valueSimplePattern = new SimplePattern(value, PatternType.pattern);
                String fLocale = cldrFile.getSourceLocaleID(path, out);
                String fPath = out.pathWhereFound;

                System.out.println(
                        Joiners.TAB.join(
                                List.of(
                                        "" + ++counter,
                                        locale,
                                        fLocale,
                                        calendar,
                                        skeleton,
                                        alt,
                                        value,
                                        skeletonSimplePattern,
                                        valueSimplePattern)));
            }

            sorted.put(calendar, phf.fromPath(path));
        }

        for (Entry<String, Collection<PathHeader>> calAndPh : sorted.asMap().entrySet()) {
            String calendar = calAndPh.getKey();
            Collection<PathHeader> phset = calAndPh.getValue();

            Map<SimplePattern, Multimap<SimplePattern, PatternData2>> skelSP2valSP2Data =
                    new TreeMap<>();

            for (PathHeader ph : phset) {
                String path = ph.getOriginalPath();
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String value = cldrFile.getStringValue(path);
                if (value == null) {
                    nullErrors.add(locale + "\t" + path);
                    continue;
                }
                String fLocale = cldrFile.getSourceLocaleID(path, out);
                String pathWhereFound = out.pathWhereFound;
                String skeleton = parts.getAttributeValue(-1, "id");
                String alt = parts.getAttributeValue(-1, "alt");
                if (alt == null) {
                    alt = "";
                }
                if (alt.equals("variant")) {
                    continue;
                }

                SimplePattern skeletonSimplePattern =
                        new SimplePattern(skeleton, PatternType.skeleton);
                SimplePattern valueSimplePattern = new SimplePattern(value, PatternType.pattern);

                // we verify that for the same (calendar, skeletonSimplePattern), we only have one
                // valueSimplePattern
                // that is, we don't have yMd => {dMy, yMd}

                Multimap<SimplePattern, PatternData2> valueMM =
                        skelSP2valSP2Data.get(skeletonSimplePattern);

                if (valueMM == null) {
                    skelSP2valSP2Data.put(
                            skeletonSimplePattern, valueMM = LinkedHashMultimap.create());
                }
                valueMM.put(
                        valueSimplePattern,
                        new PatternData2(
                                locale,
                                fLocale,
                                calendar,
                                skeleton,
                                alt,
                                SDI.getCoverageLevel(path, locale).toString(),
                                value));
            }

            for (Entry<SimplePattern, Multimap<SimplePattern, PatternData2>> entry :
                    skelSP2valSP2Data.entrySet()) {
                final SimplePattern skeletonSP = entry.getKey();
                // if the multimap has multiple keys, then we have a problem
                // that means that similar skeletons map to dissimilar values
                final Set<SimplePattern> valueSet = entry.getValue().keySet();
                boolean inconsistentValues = valueSet.size() > 1;

                // allow Ms to match Mn, if all else equal
                // common with Slavics
                if (inconsistentValues) {
                    Set<String> neuteredMonths = new TreeSet<>();
                    for (SimplePattern p : valueSet) {
                        neuteredMonths.add(p.toString().replace("Mⁿ", "Mˢ"));
                    }
                    if (neuteredMonths.size() == 1) {
                        inconsistentValues = false;
                    }
                }

                // show the errors
                for (Entry<SimplePattern, Collection<PatternData2>> entry2 :
                        entry.getValue().asMap().entrySet()) {
                    final SimplePattern valueSP = entry2.getKey();
                    if (SHOW_PROGRESS || inconsistentValues) {
                        for (PatternData2 patternData : entry2.getValue()) {
                            System.out.println(
                                    ++counter
                                            + "\t"
                                            + patternData
                                            + "\t"
                                            + skeletonSP
                                            + "\t"
                                            + valueSP
                                            + (inconsistentValues ? "\t❌" : ""));
                        }
                    }
                }
                if (inconsistentValues) {
                    System.out.println();
                }
                //                if (valueSimplePatternToData.keySet().size() > 1) {
                //                    System.out.println();
                //                }
            }
        }
    }

    static class VariableField2 extends VariableField implements Comparable<VariableField2> {

        public VariableField2(Object vf, boolean strict) {
            super(vf.toString(), strict);
        }

        @Override
        public int compareTo(VariableField2 o) {
            return ComparisonChain.start()
                    .compare(getType(), o.getType())
                    .compare(isNumeric(), o.isNumeric())
                    .result();
        }

        @Override
        public boolean equals(Object obj) {
            return compareTo((VariableField2) obj) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getType(), isNumeric());
        }
    }

    public enum PatternType {
        skeleton,
        pattern
    }

    private static final VariableField2 VARIABLE_FIELD_U = new VariableField2("U", true);

    static class SimplePattern implements Comparable<SimplePattern> {
        static Comparator<Iterable<VariableField2>> comp =
                Comparators.lexicographical(Comparator.<VariableField2>naturalOrder());
        Collection<VariableField2> internal;

        SimplePattern(String id, PatternType patternType) {
            internal =
                    patternType == PatternType.skeleton ? new TreeSet<>() : new LinkedHashSet<>();
            for (Object item : fp.set(id).getItems()) {
                if (item instanceof DateTimePatternGenerator.VariableField) {
                    VariableField2 v = new VariableField2(item, true);
                    switch (v.getType()) {
                        case DateTimePatternGenerator.ERA:
                            if (!INCLUDE_ERA && patternType == PatternType.pattern) {
                                continue;
                            }
                            break;
                        case DateTimePatternGenerator.DAYPERIOD:
                            continue;
                        case DateTimePatternGenerator.YEAR: // handle r(U) by mapping U to r
                            v = VARIABLE_FIELD_U;
                            break;
                    }
                    internal.add(v);
                }
            }
            if (patternType == PatternType.pattern) {
                internal = List.copyOf(internal);
            }
        }

        @Override
        public String toString() {
            return internal.stream().map(v -> pretty(v)).collect(Collectors.joining(""));
        }

        public static String pretty(VariableField2 v) {
            return VariableField.getCanonicalCode(v.getType()) + (v.isNumeric() ? "ⁿ" : "ˢ");
        }

        @Override
        public int compareTo(SimplePattern o) {
            return comp.compare(internal, o.internal);
        }

        @Override
        public boolean equals(Object obj) {
            return internal.equals(((SimplePattern) obj).internal);
        }

        @Override
        public int hashCode() {
            return internal.hashCode();
        }
    }

    void checkSupersets() {
        Set<String> locales = Set.of("en", "ja", "fi"); // testing
        String lastLang = "";
        for (String locale : locales) {
            CLDRFile cldrFile = MAIN_FACTORY.make(locale, true);
            Map<String, DatePatternInfo> map =
                    DatetimeUtilities.calendarToDatePatternInfo(cldrFile); // unresolved
            if (map.isEmpty()) continue;
        }
        //    static class PatternData {
        //        final String skeleton;
        //        final String value;
        //        final String foundInfo;
        //
        //        public PatternData(
        //                String skeleton, String value, String foundLocale, String pathWhereFound)
        // {
        //            this.skeleton = skeleton;
        //            this.value = value;
        //            if (foundLocale.equals("≡")) {
        //                if (pathWhereFound.equals("≡")) {
        //                    foundInfo = null;
        //                } else {
        //                    foundInfo = pathWhereFound;
        //                }
        //            } else {
        //                if (pathWhereFound.equals("≡")) {
        //                    foundInfo = foundLocale;
        //                } else {
        //                    foundInfo = foundLocale + "/" + pathWhereFound;
        //                }
        //            }
        //        }
        //
        //        @Override
        //        public String toString() {
        //            return skeleton + " => " + value + (foundInfo == null ? "" : " (" + foundInfo
        // +
        // ")");
        //        }
        //    }
    }
}
