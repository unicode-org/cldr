package org.unicode.cldr.tool;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.util.CollectionUtilities;

public class ShowDtdDiffs {
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    static Set<DtdType> TYPES = EnumSet.allOf(DtdType.class);
    static {
        TYPES.remove(DtdType.ldmlICU);
    }

    static final Map<DtdType, String> FIRST_VERSION = new EnumMap<>(DtdType.class);
    static {
        FIRST_VERSION.put(DtdType.ldmlBCP47, "1.7.2");
        FIRST_VERSION.put(DtdType.keyboard, "22.1");
        FIRST_VERSION.put(DtdType.platform, "22.1");
    }

    public static void main(String[] args) {
        String last = null;
        for (String current : ToolConstants.CLDR_VERSIONS) {
            String currentName = current == null ? "trunk" : current;
            for (DtdType type : TYPES) {
                String firstVersion = FIRST_VERSION.get(type);
                if (firstVersion != null && current != null && current.compareTo(firstVersion) < 0) {
                    continue;
                }
                DtdData dtdCurrent = null;
                try {
                    dtdCurrent = DtdData.getInstance(type, current);
                } catch (Exception e) {
                    if (!(e.getCause() instanceof FileNotFoundException)) {
                        throw e;
                    }
                    System.out.println(e.getMessage() + ", " + e.getCause().getMessage());
                    continue;
                }
                DtdData dtdLast = null;
                if (last != null) {
                    try {
                        dtdLast = DtdData.getInstance(type, last);
                    } catch (Exception e) {
                        if (!(e.getCause() instanceof FileNotFoundException)) {
                            throw e;
                        }
                    }
                }
                diff(currentName + "\t" + type, dtdLast, dtdCurrent);
            }
            last = current;
        }
    }

    private static void diff(String prefix, DtdData dtdLast, DtdData dtdCurrent) {
        Map<String, Element> oldNameToElement = dtdLast == null ? Collections.EMPTY_MAP : dtdLast.getElementFromName();
        checkNames(prefix, dtdCurrent, oldNameToElement, "/", dtdCurrent.ROOT, new HashSet<Element>());
    }

    private static void checkNames(String prefix, DtdData dtdCurrent, Map<String, Element> oldNameToElement, String path, Element element,
        HashSet<Element> seen) {
        if (seen.contains(element)) {
            return;
        }
        seen.add(element);
        String name = element.getName();
        if (SKIP_ELEMENTS.contains(name)) {
            return;
        }

        if (isDeprecated(dtdCurrent.dtdType, name, "*")) { // SDI.isDeprecated(dtdCurrent.dtdType, name, "*", "*")) {
            return;
        }
        String newPath = path + "/" + element.name;
        if (!oldNameToElement.containsKey(name)) {
            String attributeNames = getAttributeNames(dtdCurrent, name, Collections.EMPTY_MAP, element.getAttributes());
            System.out.println(prefix + "\tElement\t" + newPath + "\t" + attributeNames);
        } else {
            Element oldElement = oldNameToElement.get(name);
            String attributeNames = getAttributeNames(dtdCurrent, name, oldElement.getAttributes(), element.getAttributes());
            if (!attributeNames.isEmpty()) {
                System.out.println(prefix + "\tAttribute\t" + newPath + "\t" + attributeNames);
            }
        }
        for (Element child : element.getChildren().keySet()) {
            checkNames(prefix, dtdCurrent, oldNameToElement, newPath, child, seen);
        }
    }

//    static class Parents {
//        final DtdData dtd;
//        final Relation<String,String> childToParents = Relation.of(new HashMap(), HashSet.class);
//        static Map<DtdData,Parents> cache = new ConcurrentHashMap<>();
//
//        public Parents(DtdData dtd) {
//           this.dtd = dtd;
//        }
//
//        static Parents getInstance(DtdData dtd) {
//            Parents result = cache.get(dtd);
//            if (result == null) {
//                result = new Parents(dtd);
//                result.addParents(dtd.ROOT, new HashSet<Element>());
//                result.childToParents.freeze();
//            }
//            return result;
//        }
//
//        private void addParents(Element element, HashSet<Element> seen) {
//            if (!seen.contains(element)) {
//                for (Element child : element.getChildren().keySet()) {
//                    childToParents.put(child.name, element.name);
//                    addParents(child, seen);
//                }
//            }
//        }
//    }

    static final Set<String> SKIP_ELEMENTS = new HashSet<>(Arrays.asList("generation", "identity", "alias", "special", "telephoneCodeData"));
    static final Set<String> SKIP_ATTRIBUTES = new HashSet<>(Arrays.asList("references", "standard", "draft", "alt"));

    private static String getAttributeNames(DtdData dtdCurrent, String elementName, Map<Attribute, Integer> attributesOld, Map<Attribute, Integer> attributes) {
        Set<String> names = new LinkedHashSet<>();
        main: for (Attribute attribute : attributes.keySet()) {
            String name = attribute.getName();
            if (SKIP_ATTRIBUTES.contains(name)) {
                continue;
            }
            if (isDeprecated(dtdCurrent.dtdType, elementName, name)) { // SDI.isDeprecated(dtdCurrent, elementName, name, "*")) {
                continue;
            }
            for (Attribute attributeOld : attributesOld.keySet()) {
                if (attributeOld.name.equals(name)) {
                    continue main;
                }
            }
            names.add(name);
        }
        return names.isEmpty() ? "" : CollectionUtilities.join(names, ", ");
    }

    private static boolean isDeprecated(DtdType dtdType, String elementName, String attributeName) {
        try {
            return DtdData.getInstance(dtdType).isDeprecated(elementName, attributeName, "*");
        } catch (DtdData.IllegalByDtdException e) {
            return true;
        }
    }
}