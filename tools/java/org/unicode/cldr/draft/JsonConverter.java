package org.unicode.cldr.draft;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.ElementAttributeInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.ICUUncheckedIOException;

public class JsonConverter {

    private static final String FILES = "el.*";
    private static final String MAIN_DIRECTORY = CLDRPaths.MAIN_DIRECTORY;// CldrUtility.SUPPLEMENTAL_DIRECTORY;
                                                                          // //CldrUtility.MAIN_DIRECTORY;
    private static final String OUT_DIRECTORY = CLDRPaths.GEN_DIRECTORY + "/jason/"; // CldrUtility.MAIN_DIRECTORY;
    private static boolean COMPACT = false;
    static final Set<String> REPLACING_BASE = !COMPACT ? Collections.EMPTY_SET : new HashSet<String>(
        Arrays.asList("type id key count".split("\\s")));
    static final Set<String> EXTRA_DISTINGUISHING = new HashSet<String>(
        Arrays.asList("locales territory desired supported".split("\\s")));
    static final Relation<String, String> mainInfo = ElementAttributeInfo.getInstance(DtdType.ldml)
        .getElement2Attributes();
    static final Relation<String, String> suppInfo = ElementAttributeInfo.getInstance(DtdType.supplementalData)
        .getElement2Attributes();

    public static void main(String[] args) throws IOException {
        final String subdirectory = new File(MAIN_DIRECTORY).getName();
        final Factory cldrFactory = Factory.make(MAIN_DIRECTORY, FILES);
        final Set<String> locales = new TreeSet<String>(cldrFactory.getAvailable());
        final XPathParts oldParts = new XPathParts();
        final XPathParts parts = new XPathParts();
        // ElementName elementName = new ElementName();
        // LinkedHashMap<String, String> nonDistinguishing = new LinkedHashMap<String, String>();
        for (String locale : locales) {
            System.out.println("Converting:\t" + locale);
            final CLDRFile file = cldrFactory.make(locale, false);
            Relation<String, String> element2Attributes = file.isNonInheriting() ? suppInfo : mainInfo;
            final Item main = new TableItem(null);
            DtdType dtdType = null;
            for (Iterator<String> it = file.iterator("", file.getComparator()); it.hasNext();) {
                final String xpath = it.next();
                final String fullXpath = file.getFullXPath(xpath);
                String value = file.getStringValue(xpath);
                oldParts.set(fullXpath);
                if (dtdType == null) {
                    dtdType = DtdType.valueOf(parts.getElement(0));
                }
                rewrite(dtdType, oldParts, value, element2Attributes, parts);
                System.out.println(parts);
                Item current = main;
                int size = parts.size();

                for (int i = 0; i < size - 1; ++i) {
                    final String element = parts.getElement(i);
                    Map<String, String> actualAttributeKeys = parts.getAttributes(i);
                    Set<String> keySet = actualAttributeKeys.keySet();
                    if (keySet.size() != 0) {
                        Item temp = current.makeSubItem(element, Item.Type.unorderedItem);
                        for (String attribute : keySet) {
                            temp.put(attribute, actualAttributeKeys.get(attribute));
                        }
                    }
                    if (i < size - 2) {
                        current = current.makeSubItem(element,
                            actualAttributeKeys.containsKey("_q") ? Item.Type.orderedItem : Item.Type.unorderedItem);
                    } else {
                        current.put(element, parts.getElement(i + 1));
                    }
                }
            }
            PrintWriter out = FileUtilities.openUTF8Writer(OUT_DIRECTORY + subdirectory, locale + ".json");
            main.print(out, 0);
            out.close();
        }
    }

    static Relation<String, String> extraDistinguishing = Relation.of(new TreeMap<String, Set<String>>(), LinkedHashSet.class);
    static {
        putAll(extraDistinguishing, "dayPeriodRule", "earlyMorning", "before", "from");
    }

    static <K, V> void putAll(Relation r, K key, V... values) {
        r.putAll(key, Arrays.asList(values));
    }

    private static boolean isDistinguishing(DtdType dtdType, final String element, final String attribute) {
        // <mapZone other="Afghanistan" territory="001" type="Asia/Kabul"/> result is the type!
        // <deprecatedItems elements="variant" attributes="type" values="BOKMAL NYNORSK AALAND POLYTONI"/>
        // ugly: if there are values, then everything else is distinguishing, ow if there are attibutes, elements are
        if (element.equals("deprecatedItems")) {

        }
        Set<String> extras = extraDistinguishing.getAll(element);
        if (extras != null && extras.contains(attribute)) return true;
        if (EXTRA_DISTINGUISHING.contains(attribute)) return true;
        return CLDRFile.isDistinguishing(dtdType, element, attribute);
    }

    private static void rewrite(DtdType dtdType, XPathParts parts, String value,
        Relation<String, String> element2Attributes, XPathParts out) {
        out.clear();
        int size = parts.size();
        for (int i = 1; i < size; ++i) {
            final String element = parts.getElement(i);
            out.addElement(element);

            // turn a path into a revised path. All distinguished attributes (including those not currently on the
            // string)
            // get turned into extra element/element pairs, starting with _
            // all non-distinguishing attributes get turned into separate children
            // a/b[@non="y"][@dist="x"]/w : z =>
            // a/b/_dist/x/_non=y
            // a/b/_dist/x/w=z
            Collection<String> actualAttributeKeys = parts.getAttributeKeys(i);
            boolean isOrdered = actualAttributeKeys.contains("_q");
            Set<String> possibleAttributeKeys = element2Attributes.getAll(element);

            for (final String attribute : actualAttributeKeys) {
                String attributeValue = parts.getAttributeValue(i, attribute);
                if (!isDistinguishing(dtdType, element, attribute)) {
                    out.addAttribute(attribute, attributeValue);
                }
            }
            if (possibleAttributeKeys != null) {
                for (final String attribute : possibleAttributeKeys) {
                    if (isDistinguishing(dtdType, element, attribute)) {
                        if (attribute.equals("alt")) continue; // TODO fix
                        String attributeValue = parts.getAttributeValue(i, attribute);
                        out.addElement("_" + attribute);
                        if (attributeValue == null) {
                            attributeValue = "?";
                        }
                        out.addElement(attributeValue);
                    }
                }
            }
            if (isOrdered) {
                Map<String, String> lastAttributes = out.getAttributes(-2);
                lastAttributes.put("_q", "_q");
            }
        }
        if (value.length() > 0) {
            out.addElement(value);
        }

        if (!COMPACT) {
            return;
        }
        if (parts.getElement(-1).equals("type")) {
            String key = parts.getAttributeValue(-1, "key");
            if (key != null) {
                parts.setElement(-2, key + "Key");
                parts.putAttributeValue(-1, "key", null);
            }
            // fall thru
        }
        if (parts.getElement(1).equals("localeDisplayNames")) {
            String element2 = parts.getElement(2);
            if (!element2.endsWith("Pattern")) {
                if (element2.endsWith("s")) {
                    element2 = element2.substring(0, element2.length() - 1);
                }
                parts.setElement(2, element2 + "Names");
            }
            parts.removeElement(1);
        }
        if (parts.getElement(1).equals("dates")) {
            parts.removeElement(1);
            String element1 = parts.getElement(1);
            if (element1.equals("timeZoneNames")) {
                String main = parts.getElement(2);
                if (main.equals("zone") || main.equals("metazone")) {
                    parts.setElement(1, main + "Names");
                }
                return;
            }
        }
        if (parts.getElement(1).equals("numbers") && parts.getElement(2).equals("currencies")) {
            parts.removeElement(1);
            return;
        }
    }

    static class ElementName {
        String oldBase;
        String base;
        boolean replacedBase;
        StringBuilder suffix = new StringBuilder();

        public void reset(String element) {
            suffix.setLength(0);
            base = oldBase = element;
            replacedBase = false;
        }

        public void add(String attribute, String attributeValue) {
            if (REPLACING_BASE.contains(attribute)) {
                if (replacedBase) {
                    System.out.println("ERROR: Two replacement types on same element!!\t" + oldBase + "," + base + ","
                        + attribute + "," + attributeValue);
                } else {
                    replacedBase = true;
                    base = attributeValue;
                    return;
                }
            }
            suffix.append('$').append(attribute).append('=').append(attributeValue);
        }

        public String toString() {
            if (suffix == null) {
                return base;
            }
            return base + suffix;
        }
    }

    static abstract class Item {
        protected Item parent;

        public Item(Item parent) {
            this.parent = parent;
        }

        public abstract int size();

        enum Type {
            unorderedItem, orderedItem
        }

        public abstract Appendable print(Appendable result, int i);

        protected Appendable indent(Appendable result, int i) throws IOException {
            return result.append(getIndent(i));
        }

        protected String getIndent(int i) {
            return Utility.repeat("    ", i);
        }

        public Appendable appendString(Appendable result, String string, int indent) throws IOException {
            result.append('"');
            for (int i = 0; i < string.length(); ++i) {
                // http://www.json.org/
                // any-Unicode-character-except-"-or-\-or-control-character
                // uses UTF16
                char ch = string.charAt(i);
                switch (ch) {
                case '\"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '/':
                    result.append("\\/");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    if (indent < 0) {
                        result.append("\\n");
                    } else {
                        result.append('\n').append(getIndent(indent));
                    }
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (ch <= 0x1F || 0x7F <= ch && ch <= 0x9F) {
                        result.append("\\u").append(Utility.hex(ch, 4));
                    } else {
                        result.append(ch);
                    }
                    break;
                }
            }
            return result.append('"');
        }

        public String toString() {
            return print(new StringBuilder(), 0).toString();
        }

        protected Item create(Type ordered) {
            switch (ordered) {
            case unorderedItem:
                return new TableItem(this);
            case orderedItem:
                return new ArrayItem(this);
            default:
                throw new UnsupportedOperationException();
            }
        }

        public abstract Item makeSubItem(String element, Type ordered);

        public abstract void put(String element, String value);

        public Item getRoot() {
            if (parent == null) {
                return this;
            } else {
                return parent.getRoot();
            }
        }
    }

    static class TableItem extends Item {
        public TableItem(Item parent) {
            super(parent);
        }

        private Map<String, Item> map = new LinkedHashMap<String, Item>();

        public Item get(String element) {
            return map.get(element);
        }

        public void put(String element, String value) {
            Item old = map.get(element);
            if (old != null) {
                if (old instanceof StringItem) {
                    if (value.equals(((StringItem) old).value)) {
                        return;
                    }
                }
                throw new IllegalArgumentException("ERROR: Table already has object: " + element + ", " + old + ", "
                    + value + ", " + getRoot().toString());
            }
            map.put(element, new StringItem(value));
        }

        public Item makeSubItem(String element, Type ordered) {
            Item result = map.get(element);
            if (result != null) {
                return result;
            }
            result = create(ordered);
            result.parent = this;

            map.put(element, result);
            return result;
        }

        public Appendable print(Appendable result, int i) {
            try {
                if (map.size() == 0) {
                    result.append("{}");
                    return result;
                }
                result.append("{\n");
                boolean first = true;
                for (String key : map.keySet()) {
                    Item value = map.get(key);
                    if (first) {
                        first = false;
                    } else {
                        result.append(",\n");
                    }
                    indent(result, i + 1);
                    appendString(result, key, -1).append(" : ");
                    value.print(result, i + 1);
                }
                result.append("\n");
                indent(result, i).append("}");
                return result;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        @Override
        public int size() {
            return map.size();
        }
    }

    static class ArrayItem extends Item {
        public ArrayItem(Item parent) {
            super(parent);
        }

        private List<Row.R2<String, Item>> list = new ArrayList<Row.R2<String, Item>>();

        @Override
        public Appendable print(Appendable result, int i) {
            try {
                if (list.size() == 0) {
                    result.append("[]");
                    return result;
                }

                result.append("[\n");
                for (int j = 0; j < list.size(); ++j) {
                    if (j != 0) {
                        result.append(",\n");
                    }
                    indent(result, i + 1);
                    R2<String, Item> row = list.get(j);
                    result.append("{");
                    appendString(result, row.get0(), i + 1);
                    result.append(" : ");
                    row.get1().print(result, i + 1);
                    result.append("}");
                }
                result.append("\n");
                indent(result, i).append("]");
                return result;
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public Item makeSubItem(String element, Type ordered) {
            Item result = create(ordered);
            list.add(Row.of(element, result));
            return result;
        }

        public void put(String element, String value) {
            list.add(Row.of(element, (Item) new StringItem(value)));
        }

        @Override
        public int size() {
            return list.size();
        }
    }

    static class StringItem extends Item {
        private String value;

        public StringItem(String value2) {
            super(null);
            value = value2;
        }

        @Override
        public Appendable print(Appendable result, int i) {
            try {
                return appendString(result, value, i + 1);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public Item makeSubItem(String element, Type ordered) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String element, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }
    }
}
