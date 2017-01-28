package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class SubdivisionNames {
    
    private final Map<String, String> subdivisionToName;
    
    public SubdivisionNames(String locale) {
        Builder<String, String> builder = ImmutableMap.builder();
        List<Pair<String, String>> data = new ArrayList<>();
        XMLFileReader.loadPathValues(CLDRPaths.COMMON_DIRECTORY + "subdivisions/"
            + locale
            + ".xml", data, true);
        for (Pair<String, String> pair : data) {
            // <subdivision type="AD-02">Canillo</subdivision>
            XPathParts path = XPathParts.getFrozenInstance(pair.getFirst());
            if (!"subdivision".equals(path.getElement(-1))) {
                continue;
            }
            String name = pair.getSecond();
            builder.put(path.getAttributeValue(-1, "type"), name);
        }
        subdivisionToName = builder.build();
    }

    public String get(String subdivision) {
        return subdivisionToName.get(subdivision);
    }

    public Set<String> keySet() {
        return subdivisionToName.keySet();
    }

    public static String getRegionFromSubdivision(String sdCode) {
        return sdCode.compareTo("A") < 0 ? sdCode.substring(0,3) : sdCode.substring(0,2).toUpperCase();
    }

    public static boolean isRegionCode(String regionOrSubdivision) {
        return regionOrSubdivision.length() == 2 || (regionOrSubdivision.length() == 3 && regionOrSubdivision.compareTo("A") < 0);
    }
}
