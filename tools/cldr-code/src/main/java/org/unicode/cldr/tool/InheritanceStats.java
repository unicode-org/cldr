package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Output;

public class InheritanceStats {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();
    static final SupplementalDataInfo SDI = CLDR_CONFIG.getSupplementalDataInfo();
    static final DtdData dtdData = DtdData.getInstance(DtdType.ldml);

    private static final Joiner COMMA_JOINER = Joiner.on(", ");
    private static final Joiner SPACE_JOINER = Joiner.on(' ');

    static final char ZWSP = '\u200B';
    static final char TSP = '\u2009';



    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"dtd"};
        }
        for (String arg : args) {
            switch(arg) {
            case "stats": showLocaleStats(); break;
            case "inherit": testInheritance(); break;
            case "dtd": checkDtdData(); break;
            case "attr": showAttributeValues(); break;
            case "vxml": compareVxml(); break;
            default: throw new IllegalArgumentException("bad argument");
            }
        }
    }

    enum Status {
        diffBailey,
        eqBailey,
        isNull,
        isMarker,
        diffPath,
        diffLocale;

        static Set<Status> getSet(boolean isNullb, boolean isMarkerb, boolean bailey, boolean path, boolean locale) {
            Set<Status> s = EnumSet.noneOf(Status.class);
            if (isNullb) s.add(isNull);
            if (isMarkerb) s.add(isMarker);
            if (!bailey && !isNullb && !isMarkerb) s.add(diffBailey);
            if (isNullb || isMarkerb || bailey) {
                if (!isNullb && !isMarkerb) s.add(eqBailey);
                if (!path) s.add(diffPath);
                if (!locale) s.add(diffLocale);
            }
            Set<Status> result = intern.get(s);
            if (result == null) {
                throw new IllegalArgumentException();
            }
            return result;
        }
        static final Map<Set<Status>, Set<Status>> intern;
        static {
            Map<Set<Status>, Set<Status>> _intern = new LinkedHashMap<>();
            add(_intern, Status.diffBailey);
            add(_intern, Status.eqBailey, Status.diffLocale);
            add(_intern, Status.eqBailey, Status.diffPath, Status.diffLocale);
            add(_intern, Status.eqBailey, Status.diffPath);
            add(_intern, Status.isMarker, Status.diffLocale);
            add(_intern, Status.isMarker, Status.diffPath, Status.diffLocale);
            add(_intern, Status.isMarker, Status.diffPath);
            add(_intern, Status.isNull);
            add(_intern, Status.isNull, Status.diffLocale);
            add(_intern, Status.isNull, Status.diffPath, Status.diffLocale);
            add(_intern, Status.isNull, Status.diffPath);
            intern = ImmutableMap.copyOf(_intern);
        }
        static void add(Map<Set<Status>, Set<Status>> _intern, Status... set) {
            ImmutableSortedSet<Status> s = ImmutableSortedSet.copyOf(set);
            _intern.put(s, s);
        }
    }

    public static void showLocaleStats() {
        System.out.println();
        checkLocaleStats("es");
        checkLocaleStats("es_419");
        checkLocaleStats("es_AR");
    }

    public static void checkLocaleStats(String localeId) {

        CLDRFile english = CLDR_FACTORY.make(localeId, true);
        CLDRFile unresolved = english.getUnresolved();
        Output<String> pathWhereFoundB = new Output<>();
        Output<String> localeWhereFoundB = new Output<>();
        Counter<Set<Status>> counter = new Counter<>();

        for (String path : english) {
            String value = english.getStringValueWithBailey(path);
            if (value == null) {
                counter.add(Status.getSet(true, false, false, true, true), 1);
                continue;
            }
            String rawValue = unresolved.getStringValue(path);
            String bailey = english.getBaileyValue(path, pathWhereFoundB, localeWhereFoundB);

            boolean isNull = rawValue == null;
            boolean isMarker = Objects.equals(CldrUtility.INHERITANCE_MARKER, rawValue);
            boolean equalBailey = Objects.equals(rawValue, bailey);
            boolean pathEquals = Objects.equals(path, pathWhereFoundB.value);
            boolean localeEquals = Objects.equals(english.getLocaleID(), localeWhereFoundB.value);
            counter.add(Status.getSet(isNull, isMarker, equalBailey, pathEquals, localeEquals), 1);
        }
        for (Set<Status> set : Status.intern.keySet()) {
            System.out.println(localeId + "\t" + counter.getCount(set) + "\t" + set);
        }
        System.out.println(localeId + "\t" + counter.getTotal() + "\t" + "TOTAL");
    }

    public static void testInheritance() {
        Inheritance inheritance = new Inheritance(CLDR_FACTORY);

        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();
        Output<String> testPathWhereFound = new Output<>();
        Output<String> testLocaleWhereFound = new Output<>();

        // individual test cases
        String[][] tests = {
            {"en", "path", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power3\"]/compoundUnitPattern1[@count=\"other\"][@gender=\"feminine\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power3\"]/compoundUnitPattern1[@count=\"other\"]"
            },
            {"en", "path", "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot\"]/unitPattern[@count=\"one\"][@alt=\"variant\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot\"]/unitPattern[@count=\"one\"]"
            },
            {"de", "path", "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"][@gender=\"feminine\"][@case=\"accusative\"][@alt=\"variant\"]",
            "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1"},
        };
        boolean allOk = true;
        for (String[] row : tests) {
            String locale = row[0];
            String type = row[1];
            String path = row[2];
            String expected = row[3];
            String testValue = inheritance.getBaileyValue(locale, path, testPathWhereFound, testLocaleWhereFound);
            String actual = null;
            switch(type) {
            case "path": actual = testPathWhereFound.value; break;
            case "locale": actual = testLocaleWhereFound.value; break;
            }

            boolean success = assertEquals(path, expected, actual);
            if (!success) { // for debugging
                inheritance.getBaileyValue(locale, path, testPathWhereFound, testLocaleWhereFound);
                allOk = false;
            } else {
                System.out.println("\n" + locale
                    + "\t" + path
                    + "\nOK:      \t" + actual
                    + "\t" + testValue);
            }
        }

        // full locales. Only do if specific tests work
        if (allOk) {
            for (String locale : Arrays.asList("en", "fr", "oc")) {
                Set<String> paths = new TreeSet(Collections.reverseOrder());
                CLDRFile resolvedCldrFile = CLDR_FACTORY.make(locale, true);
                paths.addAll(ImmutableList.copyOf(resolvedCldrFile));
                for (String path : paths) {
                    if (resolvedCldrFile.getStringValue(path) == null) {
                        continue;
                    }
//                String unresolvedValue = verticalChain.get(0).getStringValue(path);
//                if (isHardValue(unresolvedValue)) {
//                    continue; // we have a value in the unresolved case, don't worry about it now
//                }
                    String value = resolvedCldrFile.getStringValueWithBailey(path, pathWhereFound, localeWhereFound);
                    if ("code-fallback".equals(localeWhereFound.value) || "constructed".equals(pathWhereFound.value)) {
                        continue;
                    }
                    if (path.equals(pathWhereFound.value)) {
                        continue; // ignore locale differences for now.
                    }
                    String testValue = inheritance.getBaileyValue(locale, path, testPathWhereFound, testLocaleWhereFound);
                    boolean success = true;
                    success &= assertEquals(path, pathWhereFound.value, testPathWhereFound.value);
                    // success &= assertEquals(path, localeWhereFound.value, testLocaleWhereFound.value);
                    if (!success) {
                        // for debugging
                        inheritance.getBaileyValue(locale, path, pathWhereFound, localeWhereFound);
                    } else {
                        //System.out.println("\nSource:  \t" + path + "\nOK:      \t" + pathWhereFound.value);
                    }
                }
            }
        }
//        AliasMapper aliasMapper = inheritance.aliasMapper;
//        System.out.println("nonInitials: " + aliasMapper.nonInitials.size());
//
//        Set<String> chain = inheritance.getInheritanceChain("en", "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot\"]/unitPattern[@count=\"one\"][@alt=\"variant\"]");
//        for (String s : chain) {
//            System.out.println(s);
//        }
    }

    public static boolean assertEquals(String message, String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            System.out.println("\nSource:  \t" + message);
            System.out.println("Expected:\t" + expected + "\nActual:  \t" + actual);
            return false;
        }
        return true;
    }

    static void checkDtdData() {
        Set<String> lines = new TreeSet<>();
        Multimap<Element, Element> childToParents = HashMultimap.create();
        // TODO, make Element comparable

        for (Element e : dtdData.getElements()) {
            if (e.isDeprecated()) {
                continue;
            }
            final Set<Element> children = e.getChildren().keySet();
            children.forEach(x -> childToParents.put(x, e));
            for (DtdData.Attribute a : e.getAttributes().keySet()) {
                if (a.isDeprecated()) {
                    continue;
                }
                String aName = a.name;
                if (Inheritance.LATERAL_ATTRIBUTES.containsKey(aName)) {
                    System.out.println(aName
                        + "\t" + e
                        + "\t" + a.getMatchString()
                        + "\t" + a.getMode()
                        + "\t" + children);
                }
            }
        }
        System.out.println("\nAttribute\tElement\tPossible Values");
        lines.forEach(x -> System.out.println(x));
        System.out.println("\nParents\tChild");
        childToParents.asMap().entrySet().forEach(x -> {
            Element child = x.getKey();
            Collection<Element> parents = x.getValue();
            final Set<Element> fromElement = new HashSet<>();
            child.getParents().forEach(
                y -> {if (!y.isDeprecated()) fromElement.add(y);}
                );
            if (!fromElement.equals(parents)) {
                throw new IllegalArgumentException("Bad parents in element");
            }
            if (x.getValue().size() > 1) {
                System.out.println(COMMA_JOINER.join(x.getValue()) + "\t" + x.getKey());
            }
        });
    }

    public static void showAttributeValues() {

        final Set<String> locales = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr, ImmutableSortedSet.of(Level.MODERN, Level.MODERATE));
        AttributeData attributeData = new AttributeData();

        Set<String> attributes = Inheritance.LATERAL_ATTRIBUTES.keySet();

        attributeData.getAttributePaths(CLDR_FACTORY.make("root", true), attributes);
        attributeData.showAlts();

        processLocales(locales, attributeData, attributes, CLDR_FACTORY);
        attributeData.showAlts();

        for (String version : Lists.reverse(ToolConstants.CLDR_VERSIONS)) {
            System.out.println(version);
            String dirBase = ToolConstants.getBaseDirectory(version);
            String current = dirBase + "common/" + "main";

            Factory factory;
            try {
                factory = Factory.make(current, ".*");
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            processLocales(locales, attributeData, attributes, factory);
            attributeData.showAlts();
        }
    }

    public static void processLocales(final Set<String> locales, AttributeData attributeData, Set<String> attributes, Factory factory) {
        char lastChar = 0;
        for (String locale : locales) {
            CLDRFile cldrFile;
            try {
                cldrFile = factory.make(locale, true);
            } catch (Exception e) {
                continue; // old versions might not have the locale
            }
            if (lastChar != locale.charAt(0)) {
                System.out.println("\t" + locale);
                lastChar = locale.charAt(0);
            }
            attributeData.getAttributePaths(cldrFile, attributes);
        }
    }

    private static final class AttributeData {
        private Set<String> seen = new HashSet<>();
        private TreeMultimap<String, String> dataToAlts = TreeMultimap.create();

        public void showAlts() {
            System.out.println("\nP. Element\tElement\tAlt value\tStarred Path");
            dataToAlts.asMap().entrySet().forEach(x -> {
                String element = x.getKey();
                System.out.println(element + "\t" + COMMA_JOINER.join(x.getValue()));
            });
        }

        public void getAttributePaths(CLDRFile file, Set<String> attributes) {
            PathStarrer pathStarrer = new PathStarrer();
            pathStarrer.setSubstitutionPattern("*");
            for (String path : file.fullIterable()) {
                if (seen.contains(path)) {
                    continue;
                }
                seen.add(path);
                XPathParts parts = XPathParts.getFrozenInstance(path);
                if (dtdData.isDeprecated(parts)) {
                    continue;
                }
                for (int elementNumber = 0; elementNumber < parts.size(); ++elementNumber) {
                    for (String attribute : parts.getAttributeKeys(elementNumber)) {
                        if (!attributes.contains(attribute)) {
                            continue;
                        }
                        String altValue = parts.getAttributeValue(elementNumber, attribute);
                        if (altValue.contains("proposed")) {
                            continue;
                        }
                        if (altValue != null) {
                            final String eName = parts.getElement(elementNumber);
                            String eName0 = parts.getElement(elementNumber-1);
                            dataToAlts.put(
                                eName0 + "\t" + eName + "\t" + attribute,
//                        pathStarrer.set(path) + "\t" + COMMA_JOINER.join(elementNameMap.get(eName).getChildren().keySet())),
                                altValue);
                        }
                    }
                }
            }
        }
    }

    public static void compareVxml() {
        final boolean skipAllowedChanges = true;
        final boolean checkFalseAgainstMain = true;

        for (String ldmlDirectory : DtdType.ldml.directories) {
            System.out.println("\n––––––––––\n" + ldmlDirectory + "\n––––––––––");

            final Factory mainFactory = SimpleFactory.make(CLDRPaths.COMMON_DIRECTORY + ldmlDirectory, ".*");
            final Factory dropFalse = SimpleFactory.make("/Users/markdavis/github/btangmu/common/" + ldmlDirectory, ".*");
            final Factory dropTrue = SimpleFactory.make("/Users/markdavis/github/common_true/common/" + ldmlDirectory, ".*");

            System.out.println("locale"
                + "\t" + "path"
                + "\t" + "valueT"
                + "\t" + "valueF"
                + "\t" + "valueM"
                + "\t" + "rawVT"
                + "\t" + "rawVF"
                + "\t" + "rawVM"
                );
            Set<String> seen = new HashSet<>();

            for (String locale : mainFactory.getAvailable()) {
                if (SubmissionLocales.ALLOW_ALL_PATHS_BASIC.contains(locale)) {
                    continue;
                }
                Level targetCoverageLevel = StandardCodes.make().getLocaleCoverageLevel(Organization.cldr, locale);

                CLDRFile cldrFileMain = mainFactory.make(locale, true);
                CLDRFile cldrFileFalse = dropFalse.make(locale, true);
                CLDRFile cldrFileTrue = dropTrue.make(locale, true);

                final CLDRFile unresolvedMain = cldrFileMain.getUnresolved();
                final CLDRFile unresolvedTrue = cldrFileTrue.getUnresolved();
                final CLDRFile unresolvedFalse = cldrFileFalse.getUnresolved();

                // just in case there are differences in the paths, include all
                Set<String> sortedPaths = ImmutableSortedSet.<String>naturalOrder()
                    .addAll(cldrFileMain)
                    .addAll(cldrFileFalse)
                    .addAll(cldrFileTrue)
                    .build();

                for (String path : sortedPaths) {
                    Level coverageLevel = SDI.getCoverageLevel(path, locale);
                    if (coverageLevel.compareTo(targetCoverageLevel) > 0) {
                        continue; // skip levels higher than the target
                    }
                    if (skipAllowedChanges
                        && SubmissionLocales.allowEvenIfLimited(locale, path, false, false)) {
                        continue;
                    }
                    // we care about resolved differences.

                    final String valueFalse = cldrFileFalse.getStringValueWithBailey(path);
                    final String valueTrue = cldrFileTrue.getStringValueWithBailey(path);
                    final String valueMain = cldrFileMain.getStringValueWithBailey(path);

                    if (checkFalseAgainstMain) {
                        if (valueMain == null || Objects.equals(valueFalse, valueMain)) {
                            continue;
                        }
                    } else {
                        if (Objects.equals(valueFalse, valueTrue)) {
                            continue;
                        }
                    }

                    String details = "\t" + path
                        + "\t" + valueTrue
                        + "\t" + valueFalse
                        + "\t" + valueMain

                        + "\t" + unresolvedTrue.getStringValue(path)
                        + "\t" + unresolvedFalse.getStringValue(path)
                        + "\t" + unresolvedMain.getStringValue(path);

                    // skip details that we have already seen

                    if (seen.contains(details)) {
                        continue;
                    }
                    System.out.println(locale + details);
                    seen.add(details);
                }
            }
        }

    }
}
