package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.Mode;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource.Alias;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
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

    public static class Inheritance {
        private AliasMapper aliasMapper;
        private Factory factory;
        public enum LateralAliasType {remove, removeWhenOptional, count}
        public static final Map<String,LateralAliasType> LATERAL_ATTRIBUTES = ImmutableMap.of(
            "alt", LateralAliasType.remove,
            "case", LateralAliasType.removeWhenOptional,
            "gender", LateralAliasType.removeWhenOptional,
            "count", LateralAliasType.count
            );

        public static boolean isHardValue(String value) {
            return value != null && !value.equals(CldrUtility.INHERITANCE_MARKER);
        }

        Inheritance(Factory factory) {
            CLDRFile root = factory.make("root", false);
            aliasMapper = new AliasMapper(root);
            this.factory = factory;
        }

        public String getStringValueWithBailey(String locale, String path, Output<String> pathWhereFound, Output<String> localeWhereFound) {
            List<CLDRFile> verticalChain = getVerticalLocaleChain(locale);
            String result = searchVertical(path, pathWhereFound, localeWhereFound, verticalChain);
            if (result != null) {
                return result; // the Outputs are set by searchVertical
            }
            return getBaileyValue(path, pathWhereFound, localeWhereFound, verticalChain);
        }

        public String getBaileyValue(String locale, String path, Output<String> pathWhereFound, Output<String> localeWhereFound) {
            return getBaileyValue(path, pathWhereFound, localeWhereFound, getVerticalLocaleChain(locale));
        }

        private List<CLDRFile> getVerticalLocaleChain(String locale) {
            List<CLDRFile> result = new ArrayList<>();
            while (locale != null) {
                result.add(factory.make(locale, false));
                locale = LocaleIDParser.getParent(locale);
            }
            return ImmutableList.copyOf(result);
        }

        public String getBaileyValue(String path, Output<String> pathWhereFound, Output<String> localeWhereFound, List<CLDRFile> verticalChain) {
            String result;
            Set<String> inheritanceChain = getInheritanceChain(verticalChain.get(0).getLocaleID(), path);
            for (String path2 : inheritanceChain) {
                result = searchVertical(path2, pathWhereFound, localeWhereFound, verticalChain);
                if (result != null) {
                    return result; // the Outputs are set by searchVertical
                }
            }
            // we failed
            pathWhereFound.value = null;
            localeWhereFound.value = null;
            return null;
        }

        public String searchVertical(String path, Output<String> pathWhereFound, Output<String> localeWhereFound, List<CLDRFile> verticalChain) {
            for (CLDRFile file : verticalChain) {
                String value = file.getStringValue(path);
                if (isHardValue(value)) {
                    pathWhereFound.value = path;
                    localeWhereFound.value = file.getLocaleID();
                    return value;
                }
            }
            return null;
        }

        public Set<String> getInheritanceChain(String locale, String path) {
            Set<String> result = new LinkedHashSet<>(); // prevent dups
            addVerticals(path, result);
            XPathParts parts = XPathParts.getFrozenInstance(path);
            for (Entry<String, LateralAliasType> entry : LATERAL_ATTRIBUTES.entrySet()) {
                String attribute = entry.getKey();
                int elementNumber = getFirstElementForAttribute(parts, attribute);
                if (elementNumber < 0) {
                    continue;
                }
                parts = parts.cloneAsThawed();
                switch (entry.getValue()) {
                case removeWhenOptional:
                    Attribute a = dtdData.getAttribute(parts.getElement(elementNumber), attribute);
                    if (a.getMode() == Mode.OPTIONAL) {
                        addPathReplacingAttribute(parts, elementNumber, attribute, null, result);
                    }
                    break;
                case remove:
                    addPathReplacingAttribute(parts, elementNumber, attribute, null, result);
                    break;
                case count:
                    // TBD if count is decimal, use locale to get category
                    String attValue = parts.getAttributeValue(elementNumber, attribute);
                    if (!"other".equals(attValue)) {
                        addPathReplacingAttribute(parts, elementNumber, attribute, "other", result);
                    }
                    addPathReplacingAttribute(parts, elementNumber, attribute, null, result);
                    break;
                }
                int elementNumber2 = getFirstElementForAttribute(parts, attribute);
                if (elementNumber2 > 0) {
                    throw new IllegalArgumentException("Multiple instances of " + attribute + " in " + parts);
                }
            }
            return result;
        }

        public void addPathReplacingAttribute(XPathParts parts, int elementNumber, String attribute, String attValue, Set<String> result) {
            if (attValue == null) {
                parts.removeAttribute(elementNumber, attribute);
            } else {
                parts.setAttribute(elementNumber, attribute, attValue);
            }
            String path = parts.toString();
            addVerticals(path, result);
        }

        private void addVerticals(String path, Set<String> result) {
            result.add(path);
            aliasMapper.getInheritedPaths(path, result);
        }

        // TODO move to XPathParts
        private int getFirstElementForAttribute(XPathParts parts, String key) {
            for (int elementNumber = 0; elementNumber < parts.size(); ++elementNumber) {
                if (parts.getAttributeValue(elementNumber, key) != null) {
                    return elementNumber;
                }
            }
            return -1;
        }

        /**
         * Class to gather all the aliases in root into a form useful for
         * processing lateral alias inheritance.
         */
        public static class AliasMapper {
            // this map is sorted in reverse, so that longer substrings always come before shorter
            // TODO make these immutable
            private final SortedMap<String, String> sorted;

            /**
             * Get all the prefixes
             * @return
             */
            public Set<String> getInheritingPathPrefixes() {
                return sorted.keySet();
            }

            /**
             * Get all the inherited paths for a given path
             */
            public <T extends Collection<String>> T getInheritedPaths(String path, T result) {
                while (true) {
                    String trial = getInheritedPath(path);
                    if (trial == null) {
                        break;
                    }
                    if (!result.add(trial)) {
                        throw new IllegalArgumentException("Cycle in chain");
                    }
                    path = trial;
                }
                return result;
            }

            /**
             * Given a path in a resolving CLDRFile that inherits laterally with aliases,
             * return the path it inherits from.
             * <br>If the CLDRFile is not resolving, an exception is thrown.
             * @param path
             * @return the path that the input path inherits from laterally,
             * or null if there is no such path.
             * <br>If the file is not resolving, an exception is thrown.
             */

            public String getInheritedPath(String path) {
                SortedMap<String, String> less = sorted.tailMap(path);
                String firstLess = less.firstKey();
                if (!path.startsWith(firstLess)) {
                    return null;
                }
                String result = sorted.get(firstLess)
                    + path.substring(firstLess.length());
                // System.out.println(path + " ==> " + result);
                return result;
            }

            /**
             * Given a path in a resolving CLDRFile, find all of the paths that inherit from it laterally.
             * That is, the result is the set of all paths P such that getInheritedPath(P) == path
             * If there are no such paths, the empty set is returned.
             * <br>If the CLDRFile is not resolving, an exception is thrown.
             * @param path
             * @return immutable set of laterally inheriting paths
             */
            public Set<String> getInheritingPaths(String path) {
                return null;
            }

            /**
             * Put together all the alias paths into the format: prefix => result.
             * @param root
             */
            public AliasMapper(CLDRFile root) {
                if (!"root".equals(root.getLocaleID())) {
                    throw new IllegalArgumentException("Must use a root CLDRFile");
                }

                //  <alias source="locale" path="../listPattern[@type='or-short']"/>
                SortedMap<String, String> sorted = new TreeMap<>(Collections.reverseOrder());

                for (String path : root) {
                    if (!Alias.isAliasPath(path)) {
                        continue;
                    }
                    String fullPath = root.getFullXPath(path);
                    XPathParts parts = XPathParts.getFrozenInstance(fullPath);
                    String newParts = parts.getAttributeValue(-1, "path");
                    String prefix = Alias.stripLastElement(path);
                    String composed = Alias.addRelative(prefix, newParts);
                    sorted.put(prefix, composed);
                }
                this.sorted = ImmutableSortedMap.copyOf(sorted);
            }
        }
    }

    static void checkDtdData() {
        Set<String> lines = new TreeSet<>();

        for (Element e : dtdData.getElements()) {
            if (e.isDeprecated()) {
                continue;
            }
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
                        + "\t" + e.getChildren().keySet());
                }
            }
        }
        System.out.println("\nAttribute\tElement\tPossible Values");
        lines.forEach(x -> System.out.println(x));

        TreeMultimap<Pair<String,String>, String> dataToAlts = TreeMultimap.create();
        final Factory cldrFactory = CLDR_FACTORY;

        getAltPaths(dtdData, cldrFactory.make("root", true), dataToAlts);
        showAlts(dataToAlts);

        char lastChar = 0;
        for (String locale : cldrFactory.getAvailable()) {
            if (SDI.isDefaultContent(CLDRLocale.getInstance(locale))) {
                continue;
            }
            if (lastChar != locale.charAt(0)) {
                System.out.println(locale);
                lastChar = locale.charAt(0);
            }
            getAltPaths(dtdData, cldrFactory.make(locale, true), dataToAlts);
        }

        showAlts(dataToAlts);
    }

    public static void showAlts(TreeMultimap<Pair<String, String>, String> dataToAlts) {
        System.out.println("\nP. Element\tElement\tAlt value\tStarred Path");
        dataToAlts.asMap().entrySet().forEach(x -> {
            String element = x.getKey().getFirst();
            String path = x.getKey().getSecond();
            System.out.println(element + "\t" + COMMA_JOINER.join(x.getValue()) + "\t" + path);
        });
    }

    public static TreeMultimap<Pair<String, String>, String> getAltPaths(DtdData dtdData, CLDRFile file, TreeMultimap<Pair<String, String>, String> dataToAlts) {
        PathStarrer pathStarrer = new PathStarrer();
        pathStarrer.setSubstitutionPattern("*");
        Map<String, Element> elementNameMap = dtdData.getElementFromName();

        for (String path : file.fullIterable()) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            if (dtdData.isDeprecated(parts)) {
                continue;
            }
            for (int elementNumber = 0; elementNumber < parts.size(); ++elementNumber) {
                String altValue = parts.getAttributeValue(elementNumber, "alt");
                if (altValue != null) {
                    final String eName = parts.getElement(elementNumber);
                    String eName0 = parts.getElement(elementNumber-1);
                    dataToAlts.put(Pair.of(eName0 + "\t" + eName,
                        pathStarrer.set(path).replace("/", "/ ") + "\t" + COMMA_JOINER.join(elementNameMap.get(eName).getChildren().keySet())),
                        altValue);
                }
            }
        }
        return dataToAlts;
    }
}
