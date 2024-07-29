package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.DateIntervalInfo;
import com.ibm.icu.text.DateIntervalInfo.PatternInfo;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

public class ShowInconsistentAvailable {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    static boolean INCLUDE_ERA = true;
    static boolean SHOW_PROGRESS_RAW = false;
    static boolean SHOW_PROGRESS = false;
    static String DEBUG_ONLY_CALENDAR = null; // "chinese"; // null == all
    static SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();

    private static final Joiner TAB_JOINER = Joiner.on('\t');
    private static final Joiner LF_JOINER = Joiner.on('\n');
    static FormatParser fp = new DateTimePatternGenerator.FormatParser();
    static Factory f = CONFIG.getCldrFactory();
    static PathHeader.Factory phf = PathHeader.getFactory();
    static PathStarrer ps = new PathStarrer();
    static int counter = 0;
    static Set<String> nullErrors = new LinkedHashSet<>();

    private enum MyOptions {
        ordering(
                new Params()
                        .setHelp("find the ordering of fields in availableFormats and intervals")),
        inconsistencies(new Params().setHelp("find inconsistancies in available formatts")),
        root(new Params().setHelp("find root paths"));

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
        Set<String> specialLocales =
                StandardCodes.make().getLocaleCoverageLocales(Organization.special);
        final Set<String> cldrLocalesWithoutSpecial = Sets.difference(cldrLocales, specialLocales);

        if (MyOptions.ordering.option.doesOccur()) {
            showOrdering(cldrLocalesWithoutSpecial);
        }
        if (MyOptions.root.option.doesOccur()) {
            getRootPaths();
        }
        if (MyOptions.inconsistencies.option.doesOccur()) {
            showInconsistencies(cldrLocalesWithoutSpecial);
        }
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
        CLDRFile cldrFile = f.make(locale, false);
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
                                TAB_JOINER.join(
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
            return TAB_JOINER.join(data);
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
        CLDRFile cldrFile = f.make(locale, true);
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
                ps.set(path);
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
                        TAB_JOINER.join(
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

    //    static class PatternData {
    //        final String skeleton;
    //        final String value;
    //        final String foundInfo;
    //
    //        public PatternData(
    //                String skeleton, String value, String foundLocale, String pathWhereFound) {
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
    //            return skeleton + " => " + value + (foundInfo == null ? "" : " (" + foundInfo +
    // ")");
    //        }
    //    }
}
