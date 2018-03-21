package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Differ;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XMLFileReader;

import com.ibm.icu.text.Transform;

public class TestMetadata {
    public static void main(String[] args) {
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        CLDRFile metadata = cldrFactory.make("supplementalMetadata", false);
        // Set allKeys = new TreeSet();
        // CollectionUtilities.addAll(metadata.iterator(), allKeys);
        // System.out.println("Keys: " + allKeys);
        // attribute order

//        Set<String> elements = new TreeSet<String>();
//        Set<String> attributes = new TreeSet<String>();
//        Set<LinkedHashSet<String>> elementOrderingLists = new LinkedHashSet<LinkedHashSet<String>>();
//
//        getElementsAndAttributes(CldrUtility.MAIN_DIRECTORY + "root.xml", elements, attributes, elementOrderingLists);
//        Set<String> suppElements = new TreeSet<String>();
//        Set<String> suppAttributes = new TreeSet<String>();
//        Set<LinkedHashSet<String>> suppElementOrderingLists = new LinkedHashSet<LinkedHashSet<String>>();
//        getElementsAndAttributes(CldrUtility.COMMON_DIRECTORY + "supplemental/characters.xml", suppElements,
//            suppAttributes, suppElementOrderingLists);
//
//        Set<String> allElements = new TreeSet<String>();
//        allElements.addAll(elements);
//        allElements.addAll(suppElements);
//        Set<String> allAttributes = new TreeSet<String>();
//        allAttributes.addAll(attributes);
//        allAttributes.addAll(suppAttributes);
//
//        List<String> attributeOrder = Arrays.asList(metadata.getStringValue("//supplementalData/metadata/attributeOrder")
//            .split("\\s+"));
//        List<String> programAttributeOrder = CLDRFile.getAttributeOrder();
//
//        Set<String> allAttributeOrder = new TreeSet<String>();
//        allAttributeOrder.addAll(attributeOrder);
//        allAttributeOrder.addAll(programAttributeOrder);
//        allAttributeOrder.remove("_q");
//        if (showSetDifferences("dtd attributes", allAttributes, "attributeOrder+programAttributeOrder",
//            allAttributeOrder)) {
//            System.out.println("ERROR: differences in sets!");
//        }
//
//        if (!attributeOrder.equals(programAttributeOrder)) {
//            System.out.println("ElementOrderDifference: ");
//            System.out.println(showDifference(programAttributeOrder, attributeOrder, ", "));
//            System.out.println("metadata: " + attributeOrder);
//            System.out.println("program: " + programAttributeOrder);
//            System.out.println("ERROR: differences in sets!");
//        }
//
//        List<String> elementOrder = Arrays.asList(metadata.getStringValue("//supplementalData/metadata/elementOrder").split(
//            "\\s+"));
//        List<String> programElementOrder = (List<String>) CLDRFile.getElementOrder();
//
//        sublistCheck(elementOrderingLists, programElementOrder);
//        sublistCheck(suppElementOrderingLists, programElementOrder);
//
//        Set<String> allElementOrder = new TreeSet<String>();
//        allElementOrder.addAll(elementOrder);
//        allElementOrder.addAll(programElementOrder);
//        if (showSetDifferences("dtd elements", allElements, "elementOrder+programElementOrder", allElementOrder)) {
//            System.out.println("ERROR: differences in sets!");
//        }
//
//        if (!elementOrder.equals(programElementOrder)) {
//            System.out.println("ElementOrderDifference: ");
//            System.out.println(showDifference(programElementOrder, elementOrder, ", "));
//            System.out.println("metadata: " + elementOrder);
//            System.out.println("program: " + programElementOrder);
//            System.out.println("ERROR: differences in sets!");
//        }

        testZones(metadata);
        System.out.println("Done");
    }

    private static void sublistCheck(Set<LinkedHashSet<String>> elementOrderingLists, List<String> elementOrder) {
        for (Iterator<LinkedHashSet<String>> it = elementOrderingLists.iterator(); it.hasNext();) {
            LinkedHashSet<String> sublist = (LinkedHashSet<String>) it.next();
            // verify that the elements are in the list in the right order.
            int lastPosition = -1;
            for (Iterator<String> it2 = sublist.iterator(); it2.hasNext();) {
                String item = it2.next();
                int position = elementOrder.indexOf(item);
                if (position <= lastPosition) {
                    System.out.println("ERROR: elements out of order for: " + item + " in " + sublist);
                    return;
                }
            }
            System.out.println("Elements in order for: " + sublist);
        }
    }

    private static boolean showSetDifferences(String name1, Collection<String> set1, String name2, Collection<String> set2) {
        boolean hasDifference = false;
        TreeSet<String> temp = new TreeSet<String>();
        temp.addAll(set1);
        temp.removeAll(set2);
        if (temp.size() != 0) {
            System.out.println(name1 + " minus " + name2 + ":\t" + temp);
            hasDifference |= true;
        }
        temp.clear();
        temp.addAll(set2);
        temp.removeAll(set1);
        if (temp.size() != 0) {
            System.out.println(name2 + " minus " + name1 + ":\t" + temp);
            hasDifference |= true;
        }
        return hasDifference;
    }

    private static void getElementsAndAttributes(String fileWithDTD, Collection<String> elements, Collection<String> attributes,
        Collection<LinkedHashSet<String>> elementOrderingLists) {
        XMLFileReader xfr = new XMLFileReader().setHandler(new MyHandler(elements, attributes, elementOrderingLists));
        xfr.read(fileWithDTD, -1, true);
    }

    private static class ToString<T> implements Transform<T, String> {
        @Override
        public String transform(T source) {
            return source == null ? "<null>" : source.toString();
        }
    }

    public static <T> String showDifference(Iterable<T> a, Iterable<T> b, String separator) {
        return showDifference(a, b, separator, new ToString<T>(), new ToString<T>());
    }

    public static <T> String showDifference(Iterable<T> a, Iterable<T> b, String separator,
        Transform<T, String> aDisplay,
        Transform<T, String> bDisplay) {
        Differ<T> differ = new Differ<T>(300, 10);
        StringBuilder out = new StringBuilder();
        Iterator<T> ai = a.iterator();
        Iterator<T> bi = b.iterator();
        boolean first = true;
        while (true) {
            boolean done = true;
            if (ai.hasNext()) {
                differ.addA(ai.next());
                done = false;
            }
            if (bi.hasNext()) {
                differ.addB(bi.next());
                done = false;
            }
            differ.checkMatch(done);

            if (differ.getACount() != 0 || differ.getBCount() != 0) {
                if (first)
                    first = false;
                else
                    out.append(separator);
                out.append("...");
                out.append(separator).append(aDisplay.transform(differ.getA(-1)));

                if (differ.getACount() != 0) {
                    for (int i = 0; i < differ.getACount(); ++i) {
                        out.append(separator).append('-');
                        out.append(aDisplay.transform(differ.getA(i)));
                    }
                }
                if (differ.getBCount() != 0) {
                    for (int i = 0; i < differ.getBCount(); ++i) {
                        out.append(separator).append('+');
                        out.append(bDisplay.transform(differ.getB(i)));
                    }
                }
                out.append(separator).append(aDisplay.transform(differ.getA(differ.getACount())));
            }
            if (done) break;
        }
        return out.toString();
    }

    private static void testZones(CLDRFile metadata) {
        String zoneList = null;
        for (Iterator<String> it = metadata.iterator(); it.hasNext();) {
            String key = it.next();
            if (key.indexOf("\"$tzid\"") >= 0) {
                zoneList = metadata.getStringValue(key);
                break;
            }
        }

        String[] zones = zoneList.split("\\s+");
        Set<String> metaZoneSet = new TreeSet<String>();
        metaZoneSet.addAll(Arrays.asList(zones));

        StandardCodes sc = StandardCodes.make();
        Map<String, List<String>> new_oldZones = sc.getZoneData();
        Set<String> stdZoneSet = new TreeSet<String>();
        stdZoneSet.addAll(new_oldZones.keySet());

        if (metaZoneSet.equals(stdZoneSet)) {
            System.out.println("Zone Set is up-to-date");
        } else {
            Set<String> diff = new TreeSet<String>();
            diff.addAll(metaZoneSet);
            diff.removeAll(stdZoneSet);
            System.out.println("Meta Zones - Std Zones: " + diff);
            diff.clear();
            diff.addAll(stdZoneSet);
            diff.removeAll(metaZoneSet);
            System.out.println("Std Zones - Meta Zones: " + diff);

            System.out.println("Meta Zones: " + metaZoneSet);
            System.out.println("Std Zones: " + stdZoneSet);

        }
    }

    static class MyHandler extends XMLFileReader.SimpleHandler {
        Collection<String> elements;
        Collection<String> attributes;
        Collection<LinkedHashSet<String>> elementOrderingLists;

        public MyHandler(Collection<String> elements, Collection<String> attributes, Collection<LinkedHashSet<String>> elementOrderingLists) {
            this.elements = elements;
            this.attributes = attributes;
            this.elementOrderingLists = elementOrderingLists;
        }

        public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
            attributes.add(aName);
            // System.out.println(
            // "eName: " + eName
            // + ",\t aName: " + aName
            // + ",\t type: " + type
            // + ",\t mode: " + mode
            // + ",\t value: " + value
            // );
        }

        public void handleElementDecl(String name, String model) {
            elements.add(name);
            LinkedHashSet<String> ordering = new LinkedHashSet<String>(Arrays.asList(model.split("[^-_a-zA-Z0-9]+")));
            ordering.remove("");
            ordering.remove("PCDATA");
            ordering.remove("EMPTY");
            if (ordering.size() > 1) {
                if (elementOrderingLists.add(ordering)) {
                    // System.out.println(model + " =>\t" + ordering);
                }
            }
            // System.out.println(
            // "name: " + name
            // + ",\t model: " + model);
        }

        // public void handlePathValue(String path, String value) {
        // System.out.println(
        // "path: " + path
        // + ",\t value: " + value);
        // }
        //
        // public void handleComment(String path, String comment) {
        // System.out.println(
        // "path: " + path
        // + ",\t comment: " + comment);
        // }

    }
}