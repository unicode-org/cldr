package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Mode;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.XMLSource.Alias;
import org.unicode.cldr.util.XPathParts;

public class Inheritance {
    private Inheritance.AliasMapper aliasMapper;
    private Factory factory;

    public enum LateralAliasType {
        remove,
        removeWhenOptional,
        count
    }

    public static final Map<String, Inheritance.LateralAliasType> LATERAL_ATTRIBUTES =
            ImmutableMap.of(
                    "alt", LateralAliasType.remove,
                    "case", LateralAliasType.removeWhenOptional,
                    "gender", LateralAliasType.removeWhenOptional,
                    "count", LateralAliasType.count);

    /**
     * Is this a hard (explicit) value? (convenience method)
     *
     * @param value
     * @return
     */
    public static boolean isHardValue(String value) {
        return value != null && !value.equals(CldrUtility.INHERITANCE_MARKER);
    }

    /** Creat an Inheritance class from a factory. */
    public Inheritance(Factory factory) {
        CLDRFile root = factory.make("root", false);
        aliasMapper = new AliasMapper(root);
        this.factory = factory;
    }

    /** Get a String value with Bailey, as in current CLDRFile. */
    public String getStringValueWithBailey(
            String locale,
            String path,
            Output<String> pathWhereFound,
            Output<String> localeWhereFound) {
        List<CLDRFile> verticalChain = getVerticalLocaleChain(locale);
        String result = searchVertical(path, pathWhereFound, localeWhereFound, verticalChain);
        if (result != null) {
            return result; // the Outputs are set by searchVertical
        }
        return getBaileyValue(path, pathWhereFound, localeWhereFound, verticalChain);
    }

    /** Get a Bailey value, as in current CLDRFile. */
    public String getBaileyValue(
            String locale,
            String path,
            Output<String> pathWhereFound,
            Output<String> localeWhereFound) {
        return getBaileyValue(
                path, pathWhereFound, localeWhereFound, getVerticalLocaleChain(locale));
    }

    private List<CLDRFile> getVerticalLocaleChain(String locale) {
        List<CLDRFile> result = new ArrayList<>();
        while (locale != null) {
            result.add(factory.make(locale, false));
            locale = LocaleIDParser.getParent(locale);
        }
        return ImmutableList.copyOf(result);
    }

    private String getBaileyValue(
            String path,
            Output<String> pathWhereFound,
            Output<String> localeWhereFound,
            List<CLDRFile> verticalChain) {
        String result;
        Set<String> inheritanceChain =
                getInheritanceChain(verticalChain.get(0).getLocaleID(), path);
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

    private String searchVertical(
            String path,
            Output<String> pathWhereFound,
            Output<String> localeWhereFound,
            List<CLDRFile> verticalChain) {
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

    /** Find out if there is vertical inheritance */
    public Set<String> getInheritanceChain(String locale, String path) {
        Set<String> result = new LinkedHashSet<>(); // prevent dups
        addVerticals(path, result);
        XPathParts parts = XPathParts.getFrozenInstance(path);
        for (Entry<String, Inheritance.LateralAliasType> entry : LATERAL_ATTRIBUTES.entrySet()) {
            String attribute = entry.getKey();
            int elementNumber = getFirstElementForAttribute(parts, attribute);
            if (elementNumber < 0) {
                continue;
            }
            parts = parts.cloneAsThawed();
            switch (entry.getValue()) {
                case removeWhenOptional:
                    Attribute a =
                            InheritanceStats.dtdData.getAttribute(
                                    parts.getElement(elementNumber), attribute);
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
                throw new IllegalArgumentException(
                        "Multiple instances of " + attribute + " in " + parts);
            }
        }
        return result;
    }

    private void addPathReplacingAttribute(
            XPathParts parts,
            int elementNumber,
            String attribute,
            String attValue,
            Set<String> result) {
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
    public int getFirstElementForAttribute(XPathParts parts, String key) {
        for (int elementNumber = 0; elementNumber < parts.size(); ++elementNumber) {
            if (parts.getAttributeValue(elementNumber, key) != null) {
                return elementNumber;
            }
        }
        return -1;
    }

    /**
     * Class to gather all the aliases in root into a form useful for processing lateral alias
     * inheritance.
     */
    public static class AliasMapper {
        // this map is sorted in reverse, so that longer substrings always come before shorter
        // TODO make these immutable
        private final SortedMap<String, String> sorted;

        /**
         * Get all the prefixes
         *
         * @return
         */
        public Set<String> getInheritingPathPrefixes() {
            return sorted.keySet();
        }

        /** Get all the inherited paths for a given path */
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
         * Given a path in a resolving CLDRFile that inherits laterally with aliases, return the
         * path it inherits from. <br>
         * If the CLDRFile is not resolving, an exception is thrown.
         *
         * @param path
         * @return the path that the input path inherits from laterally, or null if there is no such
         *     path. <br>
         *     If the file is not resolving, an exception is thrown.
         */
        public String getInheritedPath(String path) {
            SortedMap<String, String> less = sorted.tailMap(path);
            String firstLess = less.firstKey();
            if (!path.startsWith(firstLess)) {
                return null;
            }
            String result = sorted.get(firstLess) + path.substring(firstLess.length());
            // System.out.println(path + " ==> " + result);
            return result;
        }

        /**
         * Given a path in a resolving CLDRFile, find all of the paths that inherit from it
         * laterally. That is, the result is the set of all paths P such that getInheritedPath(P) ==
         * path If there are no such paths, the empty set is returned. <br>
         * If the CLDRFile is not resolving, an exception is thrown.
         *
         * @param path
         * @return immutable set of laterally inheriting paths
         */
        public Set<String> getInheritingPaths(String path) {
            return null;
        }

        /**
         * Put together all the alias paths into the format: prefix => result.
         *
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
