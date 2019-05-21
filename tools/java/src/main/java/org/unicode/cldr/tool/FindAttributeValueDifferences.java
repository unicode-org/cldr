package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Objects;

public class FindAttributeValueDifferences {
    private static final String LAST_ARCHIVE_DIRECTORY = CLDRPaths.ARCHIVE_DIRECTORY;

    static public M4<String, String, String, Boolean> getActuals(
        CLDRFile english,
        M4<String, String, String, Boolean> result) {

        for (String path : english.fullIterable()) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            for (int i = 0; i < parts.size(); ++i) {
                String element = parts.getElement(i);
                for (Entry<String, String> av : parts.getAttributes(i).entrySet()) {
                    result.put(element, av.getKey(), av.getValue(), Boolean.TRUE);
                }
            }
        }
        return result;
    }

    public static void main(String[] args) {

        CLDRConfig config = CLDRConfig.getInstance();
        Factory current = config.getCldrFactory();
        Factory last = Factory.make(LAST_ARCHIVE_DIRECTORY + "cldr-" + ToolConstants.PREVIOUS_CHART_VERSION + "/common/main/", ".*");

        @SuppressWarnings({ "rawtypes", "unchecked" })
        M4<String, String, String, Boolean> newValues = ChainedMap.of(new TreeMap(), new TreeMap(), new TreeMap(), Boolean.class);
        @SuppressWarnings({ "rawtypes", "unchecked" })
        M4<String, String, String, Boolean> oldValues = ChainedMap.of(new TreeMap(), new TreeMap(), new TreeMap(), Boolean.class);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        M3<String, String, Boolean> emptyM3 = ChainedMap.of(new TreeMap(), new TreeMap(), Boolean.class);

        Set<String> modernCldr = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));

        for (String locale : Arrays.asList("de")) {
            getActuals(current.make(locale, false), newValues);
            getActuals(last.make(locale, false), oldValues);
        }

        Set<String> elements = new TreeSet<>(newValues.keySet());
        elements.addAll(oldValues.keySet());

        for (String element : elements) {
            M3<String, String, Boolean> newSubmap = CldrUtility.ifNull(newValues.get(element), emptyM3);
            M3<String, String, Boolean> oldSubmap = CldrUtility.ifNull(oldValues.get(element), emptyM3);
            Set<String> attributes = new TreeSet<>(newSubmap.keySet());
            attributes.addAll(oldSubmap.keySet());

            for (String attribute : attributes) {
                @SuppressWarnings({ "unchecked" })
                Set<String> newAttValues = CldrUtility.ifNull(newSubmap.get(attribute), Collections.EMPTY_MAP).keySet();
                @SuppressWarnings({ "unchecked" })
                Set<String> oldAttValues = CldrUtility.ifNull(oldSubmap.get(attribute), Collections.EMPTY_MAP).keySet();
                if (Objects.equal(newAttValues, oldAttValues)) {
                    continue;
                }
                showDiff(element, attribute, newAttValues, oldAttValues, "new");
                showDiff(element, attribute, oldAttValues, newAttValues, "old");
            }
        }
    }

    private static TreeSet<String> showDiff(String element, String attribute, Set<String> newAttValues, Set<String> oldAttValues, String title) {
        TreeSet<String> currentAttributeValues = new TreeSet<>(newAttValues);
        currentAttributeValues.removeAll(oldAttValues);
        for (String attributeValue : currentAttributeValues) {
            System.out.println(title + "\t" + element + "\t" + attribute + "\t" + attributeValue);
        }
        return currentAttributeValues;
    }
}
