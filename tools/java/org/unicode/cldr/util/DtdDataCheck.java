package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.AttributeType;
import org.unicode.cldr.util.DtdData.AttributeValueComparator;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.ElementType;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;

public class DtdDataCheck {

    static SupplementalDataInfo SUPPLEMENTAL = SupplementalDataInfo.getInstance();

    static final Set<Row.R4<DtdType,String,String,String>> DEPRECATED = new LinkedHashSet<Row.R4<DtdType,String,String,String>>();
    static final Set<Row.R3<DtdType,String,String>> DISTINGUISHING = new LinkedHashSet<Row.R3<DtdType,String,String>>();
    static final Set<Row.R3<DtdType,String,String>> NONDISTINGUISHING = new LinkedHashSet<Row.R3<DtdType,String,String>>();

    private static final boolean CHECK_CORRECTNESS = false;

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
        Set<Attribute> attributesWithDefaultValues = new LinkedHashSet<Attribute>();


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
            if (attributesWithDefaultValues.size() != 0) {
                System.out.println("*Attributes with default values:");
                for (Attribute a : attributesWithDefaultValues) {
                    System.out.println("\t" + a + "\t" + a.features());
                }
                System.out.println();
            }
            StringBuilder diff = new StringBuilder();
            for (Entry<String, Set<Attribute>> entry : dtdData.getAttributesFromName().keyValuesSet()) {
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
                    if (a.defaultValue != null) {
                        attributesWithDefaultValues.add(a);
                    }
                    if (SKIP_ATTRIBUTES.contains(a.name)) {
                        continue;
                    }
                    String special = "";
                    boolean allDeprecated = false;
                    if (SUPPLEMENTAL.isDeprecated(dtdData.dtdType, element.name, a.name, null)) {
                        special += "\t#DEPRECATED#";
                        allDeprecated = true;
                        DEPRECATED.add(Row.of(dtdData.dtdType, element.name, a.name, "*"));
                    } else if (a.type == AttributeType.ENUMERATED_TYPE) {
                        for (String value : a.values.keySet()) {
                            if (SUPPLEMENTAL.isDeprecated(dtdData.dtdType, element.name, a.name, value)) {
                                special += "\t#DEPRECATED:" + value + "#";
                                DEPRECATED.add(Row.of(dtdData.dtdType, element.name, a.name, value));
                            }
                        }
                    }
                    if (!allDeprecated) {
                        if (CLDRFile.isDistinguishing(dtdData.dtdType, element.name, a.name)) {
                            special += "\t#DISTINGUISHING#";
                            DISTINGUISHING.add(Row.of(dtdData.dtdType, element.name, a.name));
                        } else {
                            NONDISTINGUISHING.add(Row.of(dtdData.dtdType, element.name, a.name));
                        }
                    }
                    System.out.println(indent + "@" + a.name + "\t" + a.features() + special);
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
            DtdType[] args2 = DtdType.values();
            args = new String[args2.length];
            int i = 0;
            for (DtdType arg : args2) {
                args[i++] = arg.name();
            }
        }
        Timer timer = new Timer();
        for (String arg : args) {
            timer.start();
            DtdType type = CLDRFile.DtdType.valueOf(arg);
            DtdData dtdData = DtdData.getInstance(type);
            long duration = timer.stop();
            System.out.println("Time: " + timer);
            new Walker(dtdData).show(dtdData.ROOT);
            if (CHECK_CORRECTNESS && type == DtdType.ldml) {
                Set<String> errors = new LinkedHashSet<String>();
                //                checkOrder(dtdData.ROOT, errors);
                //                for (String error : errors) {
                //                    System.out.println("ERROR:\t" + error);
                //                }
                //                errors.clear();
                dtdData = DtdData.getInstance(DtdType.ldml);
                AttributeValueComparator avc = new AttributeValueComparator() {
                    @Override
                    public int compare(String element, String attribute, String value1, String value2) {
                        Comparator<String> comp = CLDRFile.getAttributeValueComparator(element, attribute);
                        return comp.compare(value1, value2);
                    }
                };
                Comparator<String> comp = dtdData.getDtdComparator(avc);
                CLDRFile test = TestInfo.getInstance().getEnglish();
                Set<String> sorted = new TreeSet(CLDRFile.getLdmlComparator());
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
                for (String error : errors) {
                    System.err.println("ERROR:\t" + error);
                }
                if (errors.size() != 0) {
                    throw new IllegalArgumentException();
                }
                // check cost
                checkCost("DtdComparator", sortedArray, comp);
                checkCost("DtdComparator(null)", sortedArray, dtdData.getDtdComparator(null));
                checkCost("CLDRFile.ldmlComparator", sortedArray, CLDRFile.getLdmlComparator());
                //checkCost("XPathParts", sortedArray);

            }
        }

        for (String arg : args) {
            timer.start();
            DtdType type = CLDRFile.DtdType.valueOf(arg);
            DtdData dtdData = DtdData.getInstance(type);
            long duration = timer.stop();
            System.out.println("Time: " + timer);
        }
        int i = 0;
        for (R3<DtdType, String, String> x : DISTINGUISHING) {
            System.out.println(++i + "\tDISTINGUISHING\t" + x);
        }
        i = 0;
        for (R3<DtdType, String, String> x : NONDISTINGUISHING) {
            System.out.println(++i + "\tNONDISTINGUISHING\t" + x);
        }
        i = 0;
        for (R4<DtdType, String, String, String> x : DEPRECATED) {
            System.out.println(++i + "\tDEPRECATED\t" + x);
        }
    }

    static final int LOOP = 100;

    private static void checkCost(String title, String[] sortedArray, Comparator<String> comp) {
        Timer timer = new Timer();
        for (int i = 0; i < LOOP; ++i) {
            String lastPath = null;
            for (String currentPath : sortedArray) {
                if (lastPath != null) {
                    int compValue = comp.compare(lastPath, currentPath);
                }
                lastPath = currentPath;
            }
        }
        timer.stop();
        System.out.println(title + "\tTime:\t" + timer.toString(LOOP));
    }

    private static void checkCost(String title, String[] sortedArray) {
        XPathParts parts = new XPathParts();
        Timer timer = new Timer();
        for (int i = 0; i < LOOP; ++i) {
            for (String currentPath : sortedArray) {
                parts.set(currentPath);
            }
        }
        long end = System.currentTimeMillis();
        System.out.println(title + "\tTime:\t" + timer.toString(LOOP));
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
