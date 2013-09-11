package org.unicode.cldr.util;

import java.io.StringReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.DtdType;

import com.ibm.icu.dev.util.Relation;

/**
 * An immutable object that contains the structure of a DTD.
 * @author markdavis
 */
public class DtdData extends XMLFileReader.SimpleHandler {
    static final boolean SHOW_PROGRESS = CldrUtility.getProperty("verbose", false);
    static final boolean SHOW_ALL = CldrUtility.getProperty("show_all", false);
    static final boolean DEBUG = true;
    static final Pattern FILLER = Pattern.compile("[^-a-zA-Z0-9#]");

    final Map<String, Element> nameToElement = new HashMap<String, Element>();
    final Relation<String, Attribute> nameToAttributes = Relation.of(new TreeMap<String, Set<Attribute>>(), LinkedHashSet.class);
    final Set<Element> elements = new HashSet<Element>();
    final Set<Attribute> attributes = new HashSet<Attribute>();

    public final Element ROOT;
    public final Element PCDATA = elementFrom("PCDATA");
    public final Element ANY = elementFrom("ANY");
    public final DtdType dtdType;

    enum Mode {
        REQUIRED("#REQUIRED"),
        OPTIONAL("#IMPLIED"),
        FIXED("#FIXED"),
        NULL("null");
        final String source;

        Mode(String s) {
            source = s;
        }

        public static Mode forString(String mode) {
            for (Mode value : Mode.values()) {
                if (value.source.equals(mode)) {
                    return value;
                }
            }
            if (mode == null) {
                return NULL;
            }
            throw new IllegalArgumentException(mode);
        }
    }

    public enum AttributeType {
        CDATA, IDREF, IDREFS, ENTITY, ENTITIES, NMTOKEN, NMTOKENS, ENUMERATED_TYPE
    }

    public static class Attribute {
        public final String name;
        public final Element element;
        public final Mode mode;
        public final String value;
        public final AttributeType type;
        public final Map<String, Integer> values;

        private Attribute(Element element2, String aName, Mode mode2, String[] split, String value2) {
            element = element2;
            name = aName;
            mode = mode2;
            value = value2;
            AttributeType _type = AttributeType.ENUMERATED_TYPE;
            Map _values = Collections.EMPTY_MAP;
            if (split.length == 1) {
                try {
                    _type = AttributeType.valueOf(split[0]);
                } catch (Exception e) {
                }
            }
            if (_type == AttributeType.ENUMERATED_TYPE) {
                LinkedHashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
                for (String part : split) {
                    if (part.length() != 0) {
                        temp.put(part, temp.size());
                    }
                }
                _values = Collections.unmodifiableMap(temp);
            }
            type = _type;
            values = _values;
        }

        @Override
        public String toString() {
            return element.name + ":" + name;
        }

        public String features() {
            return (type == AttributeType.ENUMERATED_TYPE ? values.keySet().toString() : type.toString())
                + (mode == Mode.NULL ? "" : ", mode=" + mode)
                + (value == null ? "" : ", default=" + value);
        }
    }

    private DtdData(DtdType type) {
        this.dtdType = type;
        this.ROOT = elementFrom(type.toString());
    }

    private void addAttribute(String eName, String aName, String type, String mode, String value) {
        Attribute a = new Attribute(nameToElement.get(eName), aName, Mode.forString(mode), FILLER.split(type), value);
        nameToAttributes.put(aName, a);
        CldrUtility.putNew(a.element.attributes, a, a.element.attributes.size());
    }

    public enum ElementType {
        EMPTY, ANY, PCDATA, CHILDREN
    }

    public static class Element {
        public final String name;
        private ElementType type;
        private final Map<Element, Integer> children = new LinkedHashMap<Element, Integer>();
        private final Map<Attribute, Integer> attributes = new LinkedHashMap<Attribute, Integer>();

        private Element(String name2) {
            name = name2;
        }

        private void setChildren(DtdData dtdData, String model) {
            if (model.equals("EMPTY")) {
                type = ElementType.EMPTY;
                return;
            }
            type = ElementType.CHILDREN;
            for (String part : FILLER.split(model)) {
                if (part.length() != 0) {
                    if (part.equals("#PCDATA")) {
                        type = ElementType.PCDATA;
                    } else if (part.equals("ANY")) {
                        type = ElementType.ANY;
                    } else {
                        CldrUtility.putNew(children, dtdData.elementFrom(part), children.size());
                    }
                }
            }
            if ((type == ElementType.CHILDREN) == (children.size() == 0) && !model.startsWith("(#PCDATA|cp")) {
                throw new IllegalArgumentException("CLDR does not permit Mixed content. " + name + ":" + model);
            }
        }

        public boolean containsAttribute(String string) {
            for (Attribute a : attributes.keySet()) {
                if (a.name.equals(string)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return name;
        }

        public ElementType getType() {
            return type;
        }

        public Map<Element, Integer> getChildren() {
            return Collections.unmodifiableMap(children);
        }

        public Map<Attribute, Integer> getAttributes() {
            return Collections.unmodifiableMap(attributes);
        }
    }

    private Element elementFrom(String name) {
        Element result = nameToElement.get(name);
        if (result == null) {
            nameToElement.put(name, result = new Element(name));
            elements.add(result);
        }
        return result;
    }

    private void addElement(String name2, String model) {
        Element element = elementFrom(name2);
        element.setChildren(this, model);
    }

    // TODO hide this
    /**
     * @deprecated
     */
    @Override
    public void handleElementDecl(String name, String model) {
        if (SHOW_ALL) {
            System.out.println("element: " + name + ", model: " + model);
        }
        addElement(name, model);
    }

    // TODO hide this
    /**
     * @deprecated
     */
    public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
        if (SHOW_ALL) {
            System.out.println("eName: " + eName
                + ", attribute: " + aName
                + ", type: " + type
                + ", mode: " + mode
                + ", value: " + value
                );
        }
        addAttribute(eName, aName, type, mode, value);
    }

    // TODO hide this
    /**
     * @deprecated
     */
    @Override
    public void handleEndDtd() {
        throw new XMLFileReader.AbortException();
    }

    static final Map<CLDRFile.DtdType, String> DTD_TYPE_TO_FILE;
    static {
        EnumMap<CLDRFile.DtdType, String> temp = new EnumMap<CLDRFile.DtdType, String>(CLDRFile.DtdType.class);
        temp.put(CLDRFile.DtdType.ldml, CldrUtility.COMMON_DIRECTORY + "dtd/ldml.dtd");
        temp.put(CLDRFile.DtdType.supplementalData, CldrUtility.COMMON_DIRECTORY + "dtd/ldmlSupplemental.dtd");
        temp.put(CLDRFile.DtdType.ldmlBCP47, CldrUtility.COMMON_DIRECTORY + "dtd/ldmlBCP47.dtd");
        temp.put(CLDRFile.DtdType.keyboard, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlKeyboard.dtd");
        temp.put(CLDRFile.DtdType.platform, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlPlatform.dtd");
        DTD_TYPE_TO_FILE = Collections.unmodifiableMap(temp);
    }

    static final EnumMap<CLDRFile.DtdType, DtdData> CACHE = new EnumMap<CLDRFile.DtdType, DtdData>(CLDRFile.DtdType.class);

    public static synchronized DtdData getInstance(CLDRFile.DtdType type) {
        DtdData simpleHandler = CACHE.get(type);
        if (simpleHandler == null) {
            simpleHandler = new DtdData(type);
            XMLFileReader xfr = new XMLFileReader().setHandler(simpleHandler);
            StringReader s = new StringReader("<?xml version='1.0' encoding='UTF-8' ?>"
                + "<!DOCTYPE ldml SYSTEM '" + DTD_TYPE_TO_FILE.get(type) + "'>");
            xfr.read(type.toString(), s, -1, true); //  DTD_TYPE_TO_FILE.get(type)
            if (simpleHandler.ROOT.children.size() == 0) {
                throw new IllegalArgumentException(); // should never happen
            }
            CACHE.put(type, simpleHandler);
        }
        return simpleHandler;
    }

    public enum DtdItem {
        ELEMENT, ATTRIBUTE, ATTRIBUTE_VALUE
    }

    public interface AttributeValueComparatorFactory {
        public Comparator<String> getAttributeValueComparator(String element, String attribute);
    }

    public Comparator<String> getDtdComparator(AttributeValueComparatorFactory avcf) {
        return new DtdComparator(avcf);
    }

    private class DtdComparator implements Comparator<String> {
        private final AttributeValueComparatorFactory avcf;
        private transient XPathParts a = new XPathParts();
        private transient XPathParts b = new XPathParts();

        public DtdComparator(AttributeValueComparatorFactory avcf) {
            this.avcf = avcf;
        }

        @Override
        public synchronized int compare(String arg0, String arg1) {
            a.set(arg0);
            b.set(arg1);
            int max = Math.max(a.size(), b.size());
            String baseA = a.getElement(0);
            String baseB = b.getElement(0);
            if (!ROOT.name.equals(baseA) || !ROOT.name.equals(baseB)) {
                throw new IllegalArgumentException();
            }
            Element parent = ROOT;
            Element elementA;
            for (int i = 1; i < max; ++i, parent = elementA) {
                elementA = nameToElement.get(a.getElement(i));
                Element elementB = nameToElement.get(b.getElement(i));
                if (elementA != elementB) {
                    int aa = parent.children.get(elementA);
                    int bb = parent.children.get(elementB);
                    return aa - bb;
                }
                int countA = a.getAttributeCount(i);
                int countB = b.getAttributeCount(i);
                if (countA == 0 && countB == 0) {
                    continue;
                }
                // we have two ways to compare the attributes. One based on the dtd,
                // and one based on explicit comparators

                attributes: for (Entry<Attribute, Integer> attr : elementA.attributes.entrySet()) {
                    Attribute main = attr.getKey();
                    String valueA = a.getAttributeValue(i, main.name);
                    String valueB = b.getAttributeValue(i, main.name);
                    if (valueA == null) {
                        if (valueB != null) {
                            return -1;
                        }
                    } else if (valueB == null) {
                        return 1;
                    } else if (valueA.equals(valueB)) {
                        --countA;
                        --countB;
                        if (countA == 0 && countB == 0) {
                            break attributes;
                        }
                        continue; // TODO
                    } else if (avcf != null) {
                        Comparator<String> comp = avcf.getAttributeValueComparator(elementA.name, main.name);
                        return comp.compare(valueA, valueB);
                    } else if (main.values.size() != 0) {
                        int aa = main.values.get(valueA);
                        int bb = main.values.get(valueB);
                        return aa - bb;
                    } else {
                        return valueA.compareTo(valueB);
                    }
                }
                if (countA != 0 || countB != 0) {
                    throw new IllegalArgumentException();
                }
            }
            return a.size() - b.size();
        }
    }

    //    private static class XPathIterator implements SimpleIterator<Node> {
    //        private String path;
    //        private int position; // at the start of the next element, or at the end of the string
    //        private Node node = new Node();
    //        
    //        public void set(String path) {
    //            if (!path.startsWith("//")) {
    //                throw new IllegalArgumentException();
    //            }
    //            this.path = path;
    //            this.position = 2;
    //        }
    //
    //        @Override
    //        public Node next() {
    //            // starts with /...[@...="...."]...
    //            if (position >= path.length()) {
    //                return null;
    //            }
    //            node.elementName = "";
    //            node.attributes.clear();
    //            int start = position;
    //            // collect the element
    //            while (true) {
    //                if (position >= path.length()) {
    //                    return node;
    //                }
    //                char ch = path.charAt(position++);
    //                switch (ch) {
    //                case '/':
    //                    return node;
    //                case '[':
    //                    node.elementName = path.substring(start, position);
    //                    break;
    //                }
    //            }
    //            // done with element, we hit a [, collect the attributes
    //
    //            if (path.charAt(position++) != '@') {
    //                throw new IllegalArgumentException();
    //            }
    //            while (true) {
    //                if (position >= path.length()) {
    //                    return node;
    //                }
    //                char ch = path.charAt(position++);
    //                switch (ch) {
    //                case '/':
    //                    return node;
    //                case '[':
    //                    node.elementName = path.substring(start, position);
    //                    break;
    //                }
    //            }
    //        }
    //    }

}
