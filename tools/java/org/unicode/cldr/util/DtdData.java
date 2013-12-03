package org.unicode.cldr.util;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.DtdType;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.Transform;

/**
 * An immutable object that contains the structure of a DTD.
 * @author markdavis
 */
public class DtdData extends XMLFileReader.SimpleHandler {
    private static final boolean SHOW_ALL = CldrUtility.getProperty("show_all", false);
    private static final boolean DEBUG = false;
    private static final Pattern FILLER = Pattern.compile("[^-a-zA-Z0-9#_]");

    private final Relation<String, Attribute> nameToAttributes = Relation.of(new TreeMap<String, Set<Attribute>>(), LinkedHashSet.class);
    private Map<String, Element> nameToElement = new HashMap<String, Element>();
    private MapComparator<String> attributeComparator;
    private MapComparator<String> elementComparator;

    public final Element ROOT;
    public final Element PCDATA = elementFrom("#PCDATA");
    public final Element ANY = elementFrom("ANY");
    public final DtdType dtdType;
    public final String version;
    private Element lastElement;
    private Attribute lastAttribute;
    private String firstComment;

    public enum Mode {
        REQUIRED("#REQUIRED"),
        OPTIONAL("#IMPLIED"),
        FIXED("#FIXED"),
        NULL("null");

        public final String source;

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
        CDATA, ID, IDREF, IDREFS, ENTITY, ENTITIES, NMTOKEN, NMTOKENS, ENUMERATED_TYPE
    }

    public static class Attribute implements Named {
        public final String name;
        public final Element element;
        public final Mode mode;
        public final String defaultValue;
        public final AttributeType type;
        public final Map<String, Integer> values;
        private String comment;

        private Attribute(Element element2, String aName, Mode mode2, String[] split, String value2) {
            element = element2;
            name = aName.intern();
            mode = mode2;
            defaultValue = value2 == null ? null 
                : value2.intern();
            AttributeType _type = AttributeType.ENUMERATED_TYPE;
            Map<String, Integer> _values = Collections.emptyMap();
            if (split.length == 1) {
                try {
                    _type = AttributeType.valueOf(split[0]);
                } catch (Exception e) {
                }
            }
            type = _type;

            if (_type == AttributeType.ENUMERATED_TYPE) {
                LinkedHashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
                for (String part : split) {
                    if (part.length() != 0) {
                        temp.put(part.intern(), temp.size());
                    }
                }
                _values = Collections.unmodifiableMap(temp);
            }
            values = _values;
        }

        @Override
        public String toString() {
            return element.name + ":" + name;
        }

        public String features() {
            return (type == AttributeType.ENUMERATED_TYPE ? values.keySet().toString() : type.toString())
                + (mode == Mode.NULL ? "" : ", mode=" + mode)
                + (defaultValue == null ? "" : ", default=" + defaultValue);
        }

        @Override
        public String getName() {
            return name;
        }

        public void addComment(String commentIn) {
            comment = comment == null ? commentIn : comment + "\n" + commentIn;
        }
    }

    private DtdData(DtdType type, String version) {
        this.dtdType = type;
        this.ROOT = elementFrom(type.toString());
        this.version = version;
    }

    private void addAttribute(String eName, String aName, String type, String mode, String value) {
        Attribute a = new Attribute(nameToElement.get(eName), aName, Mode.forString(mode), FILLER.split(type), value);
        getAttributesFromName().put(aName, a);
        CldrUtility.putNew(a.element.attributes, a, a.element.attributes.size());
        lastElement = null;
        lastAttribute = a;
    }

    public enum ElementType {
        EMPTY, ANY, PCDATA("(#PCDATA)"), CHILDREN;
        public final String source;

        private ElementType(String s) {
            source = s;
        }
        private ElementType() {
            source = name();
        }
    }

    interface Named {
        String getName();
    }

    public static class Element implements Named {
        public final String name;
        private ElementType type;
        private final Map<Element, Integer> children = new LinkedHashMap<Element, Integer>();
        private final Map<Attribute, Integer> attributes = new LinkedHashMap<Attribute, Integer>();
        private String comment;

        private Element(String name2) {
            name = name2.intern();
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
            if ((type == ElementType.CHILDREN) == (children.size() == 0) 
                && !model.startsWith("(#PCDATA|cp")) {
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

        @Override
        public String getName() {
            return name;
        }

        public Element getChildNamed(String string) {
            for (Element e : children.keySet()) {
                if (e.name.equals(string)) {
                    return e;
                }
            }
            return null;
        }

        public Attribute getAttributeNamed(String string) {
            for (Attribute a : attributes.keySet()) {
                if (a.name.equals(string)) {
                    return a;
                }
            }
            return null;
        }

        public void addComment(String comment) {
            this.comment = this.comment == null ? comment : this.comment + "\n" + comment;
        }
    }

    private Element elementFrom(String name) {
        Element result = nameToElement.get(name);
        if (result == null) {
            nameToElement.put(name, result = new Element(name));
        }
        return result;
    }

    private void addElement(String name2, String model) {
        Element element = elementFrom(name2);
        element.setChildren(this, model);
        lastElement = element;
        lastAttribute = null;
    }

    private void addComment(String comment) {
        if (comment.contains("##########")) {
            return;
        }
        if (lastElement != null) {
            lastElement.addComment(comment);
        } else if (lastAttribute != null) {
            lastAttribute.addComment(comment);
        } else {
            firstComment = firstComment == null ? comment : firstComment + "\n" + comment;
        }
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
    
    @Override
    public void handleComment(String path, String comment) {
        addComment(comment);
    }

    // TODO hide this
    /**
     * @deprecated
     */
    @Override
    public void handleEndDtd() {
        throw new XMLFileReader.AbortException();
    }

    //    static final Map<CLDRFile.DtdType, String> DTD_TYPE_TO_FILE;
    //    static {
    //        EnumMap<CLDRFile.DtdType, String> temp = new EnumMap<CLDRFile.DtdType, String>(CLDRFile.DtdType.class);
    //        temp.put(CLDRFile.DtdType.ldml, CldrUtility.BASE_DIRECTORY + "common/dtd/ldml.dtd");
    //        temp.put(CLDRFile.DtdType.supplementalData, CldrUtility.BASE_DIRECTORY + "common/dtd/ldmlSupplemental.dtd");
    //        temp.put(CLDRFile.DtdType.ldmlBCP47, CldrUtility.BASE_DIRECTORY + "common/dtd/ldmlBCP47.dtd");
    //        temp.put(CLDRFile.DtdType.keyboard, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlKeyboard.dtd");
    //        temp.put(CLDRFile.DtdType.platform, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlPlatform.dtd");
    //        DTD_TYPE_TO_FILE = Collections.unmodifiableMap(temp);
    //    }

    static final EnumMap<CLDRFile.DtdType, DtdData> CACHE = new EnumMap<CLDRFile.DtdType, DtdData>(CLDRFile.DtdType.class);

    public static synchronized DtdData getInstance(CLDRFile.DtdType type) {
        return getInstance(type, null);
    }

    public static synchronized DtdData getInstance(CLDRFile.DtdType type, String version) {
        DtdData simpleHandler = version == null ? CACHE.get(type) : null; // don't bother caching old versions
        if (simpleHandler == null) {
            simpleHandler = new DtdData(type, version);
            XMLFileReader xfr = new XMLFileReader().setHandler(simpleHandler);
            File directory = version == null ? CLDRConfig.getInstance().getCldrBaseDirectory()
                : new File(CLDRPaths.ARCHIVE_DIRECTORY + "/cldr-" + version);
            File file = new File(directory,type.dtdPath);
            StringReader s = new StringReader("<?xml version='1.0' encoding='UTF-8' ?>"
                + "<!DOCTYPE ldml SYSTEM '" + file.getAbsolutePath() + "'>");
            xfr.read(type.toString(), s, -1, true); //  DTD_TYPE_TO_FILE.get(type)
            if (simpleHandler.ROOT.children.size() == 0) {
                throw new IllegalArgumentException(); // should never happen
            }
            simpleHandler.freeze();
            if (version == null) {
                CACHE.put(type, simpleHandler);
            }
        }
        return simpleHandler;
    }

    private void freeze() {
        if (version == null) { // only generate for new versions
            MergeLists<String> elementMergeList = new MergeLists<String>();
            elementMergeList.add(dtdType.toString());
            MergeLists<String> attributeMergeList = new MergeLists<String>();
            attributeMergeList.add("_q");

            for (Element element : nameToElement.values()) {
                if (element.children.size() > 0) {
                    Collection<String> names = getNames(element.children.keySet());
                    elementMergeList.add(names);
                    if (DEBUG) {
                        System.out.println(element.getName() + "\t→\t" + names);
                    }
                }
                if (element.attributes.size() > 0) {
                    Collection<String> names = getNames(element.attributes.keySet());
                    attributeMergeList.add(names);
                    if (DEBUG) {
                        System.out.println(element.getName() + "\t→\t@" + names);
                    }
                }
            }
            List<String> elementList = elementMergeList.merge();
            List<String> attributeList = attributeMergeList.merge();
            if (DEBUG) {
                System.out.println("Element Ordering:\t" + elementList);
                System.out.println("Attribute Ordering:\t" + attributeList);
            }
            // double-check
            //        for (Element element : elements) {
            //            if (!MergeLists.hasConsistentOrder(elementList, element.children.keySet())) {
            //                throw new IllegalArgumentException("Failed to find good element order: " + element.children.keySet());
            //            }
            //            if (!MergeLists.hasConsistentOrder(attributeList, element.attributes.keySet())) {
            //                throw new IllegalArgumentException("Failed to find good attribute order: " + element.attributes.keySet());
            //            }
            //        }
            elementComparator = new MapComparator<String>(elementList).setErrorOnMissing(true).freeze();
            attributeComparator = new MapComparator<String>(attributeList).setErrorOnMissing(true).freeze();
        }
        nameToAttributes.freeze();
        nameToElement = Collections.unmodifiableMap(nameToElement);
    }

    private Collection<String> getNames(Collection<? extends Named> keySet) {
        List<String> result = new ArrayList<String>();
        for (Named e : keySet) {
            result.add(e.getName());
        }
        return result;
    }

    public enum DtdItem {
        ELEMENT, ATTRIBUTE, ATTRIBUTE_VALUE
    }

    public interface AttributeValueComparator {
        public int compare(String element, String attribute, String value1, String value2);
    }

    public Comparator<String> getDtdComparator(AttributeValueComparator avc) {
        return new DtdComparator(avc);
    }

    private class DtdComparator implements Comparator<String> {
        private final AttributeValueComparator avc;

        public DtdComparator(AttributeValueComparator avc) {
            this.avc = avc;
        }

        @Override
        public int compare(String path1, String path2) {
            XPathParts a = XPathParts.getFrozenInstance(path1);
            XPathParts b = XPathParts.getFrozenInstance(path2);
            int max = Math.max(a.size(), b.size());
            String baseA = a.getElement(0);
            String baseB = b.getElement(0);
            if (!ROOT.name.equals(baseA) || !ROOT.name.equals(baseB)) {
                throw new IllegalArgumentException("Comparing two different DTDs: " + baseA + ", " + baseB);
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

                // at this point the elements are the same and correspond to elementA
                // in the dtd

                // Handle the special added elements
                String aqValue = a.getAttributeValue(i, "_q");
                if (aqValue != null) {
                    String bqValue = b.getAttributeValue(i, "_q");
                    if (!aqValue.equals(bqValue)) {
                        int aValue = Integer.parseInt(aqValue);
                        int bValue = Integer.parseInt(bqValue);
                        return aValue - bValue;
                    }
                    --countA;
                    --countB;
                }

                attributes: 
                    for (Entry<Attribute, Integer> attr : elementA.attributes.entrySet()) {
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
                        } else if (avc != null) {
                            return avc.compare(elementA.name, main.name, valueA, valueB);
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

    public MapComparator<String> getAttributeComparator() {
        return attributeComparator;
    }

    public MapComparator<String> getElementComparator() {
        return elementComparator;
    }

    public Relation<String, Attribute> getAttributesFromName() {
        return nameToAttributes;
    }

    public Map<String, Element> getElementFromName() {
        return nameToElement;
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

    public String toString() {
        StringBuilder b = new StringBuilder();
        // <!ELEMENT ldml (identity, (alias | (fallback*, localeDisplayNames?, layout?, contextTransforms?, characters?, delimiters?, measurement?, dates?, numbers?, units?, listPatterns?, collations?, posix?, segmentations?, rbnf?, metadata?, references?, special*))) >
        // <!ATTLIST ldml draft ( approved | contributed | provisional | unconfirmed | true | false ) #IMPLIED > <!-- true and false are deprecated. -->
        if (firstComment != null) {
            b.append("\n<!--").append(firstComment).append("-->");
        }
        toString(ROOT, b, new HashSet<Element>());
        return b.toString();
    }

    private void toString(Element current, StringBuilder b, Set<Element> seen) {
        if (seen.contains(current)) {
            return;
        }
        seen.add(current);

        b.append("\n\n<!ELEMENT " + current.name + " ");
        boolean first = true;
        Element aliasElement = getElementFromName().get("alias");
        //b.append(current.rawChildren);
        if (current.type == ElementType.CHILDREN) {
            LinkedHashSet<Element> elements = new LinkedHashSet<Element>(current.children.keySet());
            boolean hasAlias = aliasElement != null && elements.remove(aliasElement);
            //boolean hasSpecial = specialElement != null && elements.remove(specialElement);
            if (hasAlias) {
                b.append("(alias |");
            }
            b.append("(");
            // <!ELEMENT transformNames ( alias | (transformName | special)* ) >
            // <!ELEMENT layout ( alias | (orientation*, inList*, inText*, special*) ) >

            for (Element e : elements) {
                if (first) {
                    first = false;
                } else {
                    b.append(", ");
                }
                b.append(e.name);
                if (e.type != ElementType.PCDATA) {
                    b.append("*");
                }
            }
            if (hasAlias) {
                b.append(")");
            }
            b.append(")");
        } else {
            b.append(current.type.source);
        }
        b.append(">");
        if (current.comment != null) {
            b.append(" <!--").append(current.comment).append("-->");
        }
        for (Attribute a : current.attributes.keySet()) {
            b.append("\n<!ATTLIST " + current.name + " " + a.name);
            if (a.type == AttributeType.ENUMERATED_TYPE) {
                b.append(" (");
                first = true;
                for (String s : a.values.keySet()) {
                    if (first) {
                        first = false;
                    } else {
                        b.append(" | ");
                    }
                    b.append(s);
                }
                b.append(")");
            } else {
                b.append(' ').append(a.type);
            }
            if (a.mode != Mode.NULL){ 
                b.append(" ").append(a.mode.source);
            }
            if (a.defaultValue != null) {
                b.append(" \"").append(a.defaultValue).append('"');
            }
            b.append(">");
            if (a.comment != null) {
                b.append(" <!--").append(a.comment).append("-->");
            }
        }
        if (current.children.size() > 0) {
            for (Element e : current.children.keySet()) {
                toString(e, b, seen);
            }
        }
    }

    public static <T> T removeFirst(Collection<T> elements, Transform<T,Boolean> matcher) {
        for (Iterator<T> it = elements.iterator(); it.hasNext();) {
            T item = it.next();
            if (matcher.transform(item) == Boolean.TRUE) {
                it.remove();
                return item;
            }
        }
        return null;
    }
}
