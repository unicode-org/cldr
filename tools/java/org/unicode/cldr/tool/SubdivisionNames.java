package org.unicode.cldr.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSortedSet;

public class SubdivisionNames {

    public static final String SUBDIVISION_PATH_PREFIX = "//ldml/localeDisplayNames/subdivisions/subdivision";
    
    private final Map<String, String> subdivisionToName;

    public SubdivisionNames(String locale) {
        Builder<String, String> builder = ImmutableMap.builder();
        List<Pair<String, String>> data = new ArrayList<>();
        XMLFileReader.loadPathValues(CLDRPaths.COMMON_DIRECTORY + "subdivisions/"
            + locale
            + ".xml", data, true);
        for (Pair<String, String> pair : data) {
            // <subdivision type="AD-02">Canillo</subdivision>
            String rawPath = pair.getFirst();
            XPathParts path = XPathParts.getFrozenInstance(rawPath);
            if (!"subdivision".equals(path.getElement(-1))) {
                continue;
            }
            String type = path.getAttributeValue(-1, "type");
            String name = pair.getSecond();
            builder.put(type, name);
        }
        subdivisionToName = builder.build();
    }

    public static Set<String> getAvailableLocales() {
        TreeSet<String> result = new TreeSet<>();
        File baseDir = new File(CLDRPaths.COMMON_DIRECTORY + "subdivisions/");
        for (String file : baseDir.list()) {
            if (file.endsWith(".xml")) {
                result.add(file.substring(0, file.length() - 4));
            }
        }
        return ImmutableSortedSet.copyOf(result);
    }

    public Set<Entry<String, String>> entrySet() {
        return subdivisionToName.entrySet();
    }

    public String get(String subdivision) {
        return subdivisionToName.get(subdivision);
    }

    public Set<String> keySet() {
        return subdivisionToName.keySet();
    }
    
    public static String getPathFromCode(String code) {
        // <subdivision type="AD-02">Canillo</subdivision>
        return SUBDIVISION_PATH_PREFIX
            + "[@type=\"" + code + "\"]";
    }

    public static String getRegionFromSubdivision(String sdCode) {
        return sdCode.compareTo("A") < 0 ? sdCode.substring(0, 3) : sdCode.substring(0, 2).toUpperCase(Locale.ENGLISH);
    }

    public static String getSubregion(String sdCode) {
        return sdCode.compareTo("A") < 0 ? sdCode.substring(3) : sdCode.substring(2).toUpperCase(Locale.ENGLISH);
    }

    public static boolean isRegionCode(String regionOrSubdivision) {
        return regionOrSubdivision.length() == 2 
            || (regionOrSubdivision.length() == 3 && regionOrSubdivision.compareTo("A") < 0);
    }

    public static String toIsoFormat(String sdCode) {
        sdCode = sdCode.toUpperCase(Locale.ENGLISH);
        int insertion = sdCode.compareTo("A") < 0 ? 3 : 2;
        return sdCode.substring(0, insertion) + "-" + sdCode.substring(insertion);
    }

    static final Pattern OLD_SUBDIVISION = Pattern.compile("[a-zA-Z]{2}[-_][a-zA-Z0-9]{1,4}");
    
    public static boolean isOldSubdivisionCode(String item) {
        return item.length() > 4 && item.length() < 7 && OLD_SUBDIVISION.matcher(item).matches();
    }
}
