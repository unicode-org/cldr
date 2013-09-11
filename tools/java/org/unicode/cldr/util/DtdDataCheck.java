package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeValueComparatorFactory;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.ElementType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;

public class DtdDataCheck {
    private static class Walker {
        HashSet<Element> seen = new HashSet<Element>();
        Set<Element> elementsMissingDraft = new LinkedHashSet<Element>();
        Set<Element> elementsMissingAlt = new LinkedHashSet<Element>();
        Set<Element> elementsMissingAlias = new LinkedHashSet<Element>();
        Set<Element> elementsMissingSpecial = new LinkedHashSet<Element>();
        static final Set<String> SKIP_ATTRIBUTES = new HashSet<String>(Arrays.asList(
            "draft", "alt", "standard", "references"));
        static final Set<String> SKIP_ELEMENTS = new HashSet<String>(Arrays.asList(
            "alias", "special"));
        Set<Attribute> attributesWithValues = new LinkedHashSet<Attribute>();

        private DtdData dtdData;

        public Walker(DtdData dtdData) {
            this.dtdData = dtdData;
        }

        private void show(Element element) {
            show(element, "");
            System.out.println();
            if (dtdData.dtdType == DtdType.ldml && elementsMissingDraft.size() != 0) {
                System.out.println("*Elements missing draft:\t" + elementsMissingDraft);
                System.out.println();
            }
            if (dtdData.dtdType == DtdType.ldml && elementsMissingAlt.size() != 0) {
                System.out.println("*Elements missing alt:\t" + elementsMissingAlt);
                System.out.println();
            }
            if (attributesWithValues.size() != 0) {
                System.out.println("*Attributes with default values:");
                for (Attribute a : attributesWithValues) {
                    System.out.println("\t" + a + "\t" + a.features());
                }
                System.out.println();
            }
            StringBuilder diff = new StringBuilder();
            for (Entry<String, Set<Attribute>> entry : dtdData.nameToAttributes.keyValuesSet()) {
                Relation<String, String> featuresToElements = Relation.of(new TreeMap<String, Set<String>>(), LinkedHashSet.class);
                for (Attribute a : entry.getValue()) {
                    featuresToElements.put(a.features(), a.element.name);
                }
                if (featuresToElements.size() != 1) {
                    diff.append("\t" + entry.getKey() + "\n");
                    for (Entry<String, Set<String>> entry2 : featuresToElements.keyValuesSet()) {
                        diff.append("\t\t" + entry2.getKey() + "\n");
                        diff.append("\t\t\t on " + entry2.getValue() + "\n");
                    }
                }
            }
            if (diff.length() != 0) {
                System.out.println("*Attributes with different features by element:");
                System.out.println(diff);
                System.out.println();
            }
        }

        private void show(Element element, String indent) {
            if (seen.contains(element)) {
                System.out.println(indent + element.name + "*");
            } else {
                seen.add(element);
                if (!element.containsAttribute("draft")) {
                    elementsMissingDraft.add(element);
                }
                if (!element.containsAttribute("alt")) {
                    elementsMissingAlt.add(element);
                }
                ElementType type = element.getType();
                System.out.println(indent + element.name + (type == ElementType.CHILDREN ? "" : "\t" + type));
                indent += "\t";
                for (Attribute a : element.getAttributes().keySet()) {
                    if (a.value != null) {
                        attributesWithValues.add(a);
                    }
                    if (SKIP_ATTRIBUTES.contains(a.name)) {
                        continue;
                    }
                    System.out.println(indent + "@" + a.name + "\t" + a.features());
                }
                for (Element e : element.getChildren().keySet()) {
                    if (SKIP_ELEMENTS.contains(e.name)) {
                        continue;
                    }
                    show(e, indent);
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Set<DtdType> args2 = DtdData.DTD_TYPE_TO_FILE.keySet();
            args = new String[args2.size()];
            int i = 0;
            for (DtdType arg : args2) {
                args[i++] = arg.name();
            }
        }
        for (String arg : args) {
            long start = System.currentTimeMillis();
            DtdType type = CLDRFile.DtdType.valueOf(arg);
            DtdData dtdData = DtdData.getInstance(type);
            long end = System.currentTimeMillis();
            System.out.println("Millis: " + (end - start));
            new Walker(dtdData).show(dtdData.ROOT);
            if (type == DtdType.ldml) {
                Set<String> errors = new LinkedHashSet<String>();
                //                checkOrder(dtdData.ROOT, errors);
                //                for (String error : errors) {
                //                    System.out.println("ERROR:\t" + error);
                //                }
                //                errors.clear();
                AttributeValueComparatorFactory avcf = new AttributeValueComparatorFactory() {
                    @Override
                    public Comparator<String> getAttributeValueComparator(String element, String attribute) {
                        return CLDRFile.getAttributeValueComparator(element, attribute);
                    }
                };
                Comparator<String> comp = dtdData.getDtdComparator(avcf);
                CLDRFile test = TestInfo.getInstance().getEnglish();
                Set<String> sorted = new TreeSet(CLDRFile.ldmlComparator);
                CollectionUtilities.addAll(test.iterator(), sorted);
                String[] sortedArray = sorted.toArray(new String[sorted.size()]);

                // compare for identity
                String lastPath = null;
                for (String currentPath : sortedArray) {
                    if (lastPath != null) {
                        int compValue = comp.compare(lastPath, currentPath);
                        if (compValue >= 0) {
                            comp.compare(lastPath, currentPath);
                            errors.add(lastPath + " ≥ " + currentPath);
                        }
                    }
                    lastPath = currentPath;
                }
                // check cost
                //                checkCost("DtdComparator", sortedArray, comp);
                //                checkCost("DtdComparator(null)", sortedArray, dtdData.getDtdComparator(null));
                //                checkCost("CLDRFile.ldmlComparator", sortedArray, CLDRFile.ldmlComparator);
                //                checkCost("XPathParts", sortedArray);

                for (String error : errors) {
                    System.out.println("ERROR:\t" + error);
                }
            }
        }
    }

    static final int LOOP = 100;

    private static void checkCost(String title, String[] sortedArray, Comparator<String> comp) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < LOOP; ++i) {
            String lastPath = null;
            for (String currentPath : sortedArray) {
                if (lastPath != null) {
                    int compValue = comp.compare(lastPath, currentPath);
                }
                lastPath = currentPath;
            }
        }
        long end = System.currentTimeMillis();
        System.out.println(title + "\tmillis:\t" + (end - start) / (double) LOOP);
    }

    private static void checkCost(String title, String[] sortedArray) {
        XPathParts parts = new XPathParts();
        long start = System.currentTimeMillis();
        for (int i = 0; i < LOOP; ++i) {
            for (String currentPath : sortedArray) {
                parts.set(currentPath);
            }
        }
        long end = System.currentTimeMillis();
        System.out.println(title + "\tmillis:\t" + (end - start) / (double) LOOP);
    }

    //    private static void checkOrder(Element element, Set<String> errors) {
    //        // compare attributes
    //        Attribute lastAttribute = null;
    //        for (Attribute attribute : element.attributes.keySet()) {
    //            Comparator<String> comp = CLDRFile.getAttributeValueComparator(element.name, attribute.name);
    //            if (attribute.values.size() != 0) {
    //                String lastAttributeValue = null;
    //                for (String value : attribute.values.keySet()) {
    //                    if (lastAttributeValue != null) {
    //                        int stockCompare = comp.compare(lastAttributeValue, value);
    //                        if (stockCompare >= 0) {
    //                            errors.add("Failure with "
    //                                    + element.name 
    //                                    + ":" + attribute.name
    //                                    + " values:\t" + lastAttributeValue + " ≥ " + value);
    //                        }
    //                    }
    //                    lastAttributeValue = value;
    //                }
    //            }
    //            if (lastAttribute != null) {
    //                int stockCompare = CLDRFile.getAttributeComparator().compare(lastAttribute.name, attribute.name);
    //                if (stockCompare >= 0) {
    //                    errors.add("Failure with attributes:\t" + lastAttribute.name + " ≥ " + attribute.name);
    //                }
    //            }
    //            lastAttribute = attribute;
    //        }
    //        // compare child elements
    //        Element lastElement = null;
    //        for (Element child : element.children.keySet()) {
    //            if (lastElement != null) {
    //                int stockCompare = CLDRFile.getElementOrderComparator().compare(lastElement.name, child.name);
    //                if (stockCompare >= 0) {
    //                    errors.add("Failure with elements:\t" + lastElement.name + " ≥ " + child.name);
    //                }
    //            }
    //            checkOrder(child, errors);
    //            lastElement = child;
    //        }
    //    }

}
