package org.unicode.cldr.tool;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;

public class ShowDtdDiffs {
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();
    
    static final List<String> CLDR_VERSIONS = Arrays.asList(
        "1.1.1",
        "1.2.0",
        "1.3.0",
        "1.4.1",
        "1.5.1",
        "1.6.1",
        "1.7.2",
        "1.8.1",
        "1.9.1",
        "2.0.1",
        "21.0",
        "22.1",
        "23.1",
        "24.0",
        "25.0",
        "26.0",
        "27.0"
        );
    static Set<DtdType> TYPES = EnumSet.allOf(DtdType.class);
    static {
        TYPES.remove(DtdType.ldmlICU);
    }

    public static void main(String[] args) {
        String last = null;
        for (String current : CLDR_VERSIONS) {
            System.out.println(current);
            for (DtdType type : TYPES) {
                DtdData dtdCurrent = null;
                try {
                    dtdCurrent = DtdData.getInstance(type, current);
                } catch (Exception e) {
                    if (!(e.getCause() instanceof FileNotFoundException)) {
                        throw e;
                    }
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
                System.out.println("\t" + type);
                diff(dtdLast, dtdCurrent);
            }
            last = current;
        }
    }
    
    private static void diff(DtdData dtdLast, DtdData dtdCurrent) {
        Map<String, Element> oldNameToElement = dtdLast == null ? Collections.EMPTY_MAP : dtdLast.getElementFromName();
        checkNames(dtdCurrent, oldNameToElement, "/", dtdCurrent.ROOT, new HashSet<Element>());
    }

    private static void checkNames(DtdData dtdCurrent, Map<String, Element> oldNameToElement, String path, Element element, HashSet<Element> seen) {
        if (seen.contains(element)) {
            return;
        }
        seen.add(element);
        String name = element.getName();
        if (SKIP_ELEMENTS.contains(name)) {
            return;
        }
        if (SDI.isDeprecated(dtdCurrent.dtdType, name, "*", "*")) {
            return;
        }
        String newPath = path + "/" + element.name;
        if (!oldNameToElement.containsKey(name)) {
            String attributeNames = getAttributeNames(dtdCurrent.dtdType, name, Collections.EMPTY_MAP, element.getAttributes());
            System.out.println("\t\tAdded element «" + newPath + "»"
                + attributeNames);
        } else {
            Element oldElement = oldNameToElement.get(name);
            String attributeNames = getAttributeNames(dtdCurrent.dtdType, name, oldElement.getAttributes(), element.getAttributes());
            if (!attributeNames.isEmpty()) {
                System.out.println("\t\tTo element «" + newPath + "», added: "
                    + attributeNames);
            }
        }
        for (Element child : element.getChildren().keySet()) {
            checkNames(dtdCurrent, oldNameToElement, newPath, child, seen);
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
    
    private static String getAttributeNames(DtdType dtdType, String elementName, Map<Attribute, Integer> attributesOld, Map<Attribute, Integer> attributes) {
        Set<String> names = new LinkedHashSet<>();
        main:
            for (Attribute attribute : attributes.keySet()) {
                String name = attribute.getName();
                if (SKIP_ATTRIBUTES.contains(name)) {
                    continue;
                }
                if (SDI.isDeprecated(dtdType, elementName, name, "*")) {
                    continue;
                }
                for (Attribute attributeOld : attributesOld.keySet()) {
                    if (attributeOld.name.equals(name)) {
                        continue main;
                    }
                }
                names.add(name);
            }
        return names.isEmpty() ? "" : "\tattributes: ‹" + CollectionUtilities.join(names, "›, ‹") + "›";
    }
}