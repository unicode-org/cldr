package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CharacterFallbacks {
    private static CharacterFallbacks SINGLETON = new CharacterFallbacks();
    private HashMap<Integer, List<String>> data = new HashMap<Integer, List<String>>();

    static public CharacterFallbacks make() {
        return SINGLETON;
    }

    public List<String> getSubstitutes(int cp) {
        return data.get(cp);
    }

    private CharacterFallbacks() {
        Factory cldrFactory = Factory.make(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY, ".*");
        CLDRFile characterFallbacks = cldrFactory.make("characters", false);
        Comparator<String> comp = DtdData.getInstance(DtdType.supplementalData).getDtdComparator(null);

        for (Iterator<String> it = characterFallbacks.iterator("//supplementalData/characters/", comp); it.hasNext();) {
            String path = it.next();
            String fullPath = characterFallbacks.getFullXPath(path);
            XPathParts parts = XPathParts.getTestInstance(fullPath);
            /*
             * <character value = "―">
             * <substitute>—</substitute>
             * <substitute>-</substitute>
             */
            String value = parts.getAttributeValue(-2, "value");
            if (value.codePointCount(0, value.length()) != 1) {
                throw new IllegalArgumentException("Illegal value in " + fullPath);
            }
            int cp = value.codePointAt(0);
            String substitute = characterFallbacks.getStringValue(path);

            List<String> substitutes = data.get(cp);
            if (substitutes == null) {
                data.put(cp, substitutes = new ArrayList<String>());
            }
            substitutes.add(substitute);
        }
        CldrUtility.protectCollection(data);
    }
}
