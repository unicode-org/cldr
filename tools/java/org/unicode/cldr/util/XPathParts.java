/*
 ******************************************************************************
 * Copyright (C) 2004-2013, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Freezable;

/**
 * Parser for XPath
 *
 * Each XPathParts object describes a single path, with its xPath member, for example
 *     //ldml/characters/exemplarCharacters[@type="auxiliary"]
 * and a list of Element objects that depend on xPath.
 * Each Element object has an "element" string such as "ldml", "characters", or "exemplarCharacters",
 * plus attributes such as a Map from key "type" to value "auxiliary".
 */
public final class XPathParts implements Freezable<XPathParts> {
    private static final boolean DEBUGGING = false;

    private volatile boolean frozen = false;
    private List<Element> elements = new ArrayList<Element>();

    private DtdData dtdData;
    private final Map<String, Map<String, String>> suppressionMap;

    private static final Map<String, XPathParts> cache = new ConcurrentHashMap<String, XPathParts>();

    public XPathParts() {
        this(null, null);
    }

    public XPathParts(Map<String, Map<String, String>> suppressionMap) {
        this(null, suppressionMap);
    }

    /**
     * Construct an XPathParts with the given elements and suppressionMap
     *
     * @param elements the List of elements to be added, or null; always null except when called by cloneAsThawed
     * @param suppressionMap the suppression map (which is ...?), or null;
     *        when suppressionMap is not null, it is always CLDRFile.defaultSuppressionMap
     *
     * This private constructor is called only by cloneAsThawed (elements not null) and by the two public constructors above (elements null).
     */
    private XPathParts(List<Element> elements, Map<String, Map<String, String>> suppressionMap) {
        if (elements != null) {
            for (Element e : elements) {
                /*
                 * TODO: cloneAsThawed shouldn't always make dtdData null.
                 * cloneAsThawed makes dtdData null even when this.dtdData was non-null, which causes
                 * bugs if we try to use XPathParts.getInstance instead of XPathParts.set, specifically
                 * null pointer exception when called from DistinguishedXPath.getDistinguishingXPath.
                 * The (only?) way to get non-null dtdData is with addElement; call addElement here
                 * instead of calling elements.add directly?
                 */
                this.elements.add(e.cloneAsThawed());
                // addElement(e.element);
            }
        }        
        this.suppressionMap = suppressionMap;
    }

    /**
     * See if the xpath contains an element
     */
    public boolean containsElement(String element) {
        for (int i = 0; i < elements.size(); ++i) {
            if (elements.get(i).getElement().equals(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Empty the xpath (pretty much the same as set(""))
     */
    public XPathParts clear() {
        elements.clear();
        dtdData = null;
        return this;
    }

    /**
     * Write out the difference from this xpath and the last, putting the value in the right place. Closes up the
     * elements that were not closed, and opens up the new.
     *
     * @param pw
     * @param filteredXPath
     *            TODO
     * @param lastFullXPath
     * @param filteredLastXPath
     *            TODO
     */
    public XPathParts writeDifference(PrintWriter pw, XPathParts filteredXPath, XPathParts lastFullXPath,
        XPathParts filteredLastXPath, String v, Comments xpath_comments) {
        int limit = findFirstDifference(lastFullXPath);
        // write the end of the last one
        for (int i = lastFullXPath.size() - 2; i >= limit; --i) {
            pw.print(Utility.repeat("\t", i));
            pw.println(lastFullXPath.elements.get(i).toString(XML_CLOSE));
        }
        if (v == null) {
            return this; // end
        }
        // now write the start of the current
        for (int i = limit; i < size() - 1; ++i) {
            if (xpath_comments != null) {
                filteredXPath.writeComment(pw, xpath_comments, i + 1, Comments.CommentType.PREBLOCK);
            }
            pw.print(Utility.repeat("\t", i));
            pw.println(elements.get(i).toString(XML_OPEN));
        }
        if (xpath_comments != null) {
            filteredXPath.writeComment(pw, xpath_comments, size(), Comments.CommentType.PREBLOCK);
        }

        // now write element itself
        pw.print(Utility.repeat("\t", (size() - 1)));
        Element e = elements.get(size() - 1);
        String eValue = v;
        if (eValue.length() == 0) {
            pw.print(e.toString(XML_NO_VALUE));
        } else {
            pw.print(e.toString(XML_OPEN));
            pw.print(untrim(eValue, size()));
            pw.print(e.toString(XML_CLOSE));
        }
        if (xpath_comments != null) {
            filteredXPath.writeComment(pw, xpath_comments, size(), Comments.CommentType.LINE);
        }
        pw.println();
        if (xpath_comments != null) {
            filteredXPath.writeComment(pw, xpath_comments, size(), Comments.CommentType.POSTBLOCK);
        }
        pw.flush();
        return this;
    }

    private String untrim(String eValue, int count) {
        String result = TransliteratorUtilities.toHTML.transliterate(eValue);
        if (!result.contains("\n")) {
            return result;
        }
        String spacer = "\n" + Utility.repeat("\t", count);
        result = result.replace("\n", spacer);
        return result;
    }

    public static class Comments implements Cloneable {
        public enum CommentType {
            LINE, PREBLOCK, POSTBLOCK
        }

        private EnumMap<CommentType, Map<String, String>> comments = new EnumMap<CommentType, Map<String, String>>(
            CommentType.class);

        public Comments() {
            for (CommentType c : CommentType.values()) {
                comments.put(c, new HashMap<String, String>());
            }
        }

        public String getComment(CommentType style, String xpath) {
            return comments.get(style).get(xpath);
        }

        public Comments addComment(CommentType style, String xpath, String comment) {
            String existing = comments.get(style).get(xpath);
            if (existing != null) {
                comment = existing + XPathParts.NEWLINE + comment;
            }
            comments.get(style).put(xpath, comment);
            return this;
        }

        public String removeComment(CommentType style, String xPath) {
            String result = comments.get(style).get(xPath);
            if (result != null) comments.get(style).remove(xPath);
            return result;
        }

        public List<String> extractCommentsWithoutBase() {
            List<String> result = new ArrayList<String>();
            for (CommentType style : CommentType.values()) {
                for (Iterator<String> it = comments.get(style).keySet().iterator(); it.hasNext();) {
                    String key = it.next();
                    String value = comments.get(style).get(key);
                    result.add(value + "\t - was on: " + key);
                    it.remove();
                }
            }
            return result;
        }

        public Object clone() {
            try {
                Comments result = (Comments) super.clone();
                for (CommentType c : CommentType.values()) {
                    result.comments.put(c, new HashMap<String, String>(comments.get(c)));
                }
                return result;
            } catch (CloneNotSupportedException e) {
                throw new InternalError("should never happen");
            }
        }

        /**
         * @param other
         */
        public Comments joinAll(Comments other) {
            for (CommentType c : CommentType.values()) {
                CldrUtility.joinWithSeparation(comments.get(c), XPathParts.NEWLINE, other.comments.get(c));
            }
            return this;
        }

        /**
         * @param string
         */
        public Comments removeComment(String string) {
            if (initialComment.equals(string)) initialComment = "";
            if (finalComment.equals(string)) finalComment = "";
            for (CommentType c : CommentType.values()) {
                for (Iterator<String> it = comments.get(c).keySet().iterator(); it.hasNext();) {
                    String key = it.next();
                    String value = comments.get(c).get(key);
                    if (!value.equals(string)) continue;
                    it.remove();
                }
            }
            return this;
        }

        private String initialComment = "";
        private String finalComment = "";

        /**
         * @return Returns the finalComment.
         */
        public String getFinalComment() {
            return finalComment;
        }

        /**
         * @param finalComment
         *            The finalComment to set.
         */
        public Comments setFinalComment(String finalComment) {
            this.finalComment = finalComment;
            return this;
        }

        /**
         * @return Returns the initialComment.
         */
        public String getInitialComment() {
            return initialComment;
        }

        /**
         * @param initialComment
         *            The initialComment to set.
         */
        public Comments setInitialComment(String initialComment) {
            this.initialComment = initialComment;
            return this;
        }
    }

    /**
     * @param pw
     * @param xpath_comments
     * @param index
     *            TODO
     */
    private XPathParts writeComment(PrintWriter pw, Comments xpath_comments, int index, Comments.CommentType style) {
        if (index == 0) return this;
        String xpath = toString(index);
        Log.logln(DEBUGGING, "Checking for: " + xpath);
        String comment = xpath_comments.removeComment(style, xpath);
        if (comment != null) {
            boolean blockComment = style != Comments.CommentType.LINE;
            XPathParts.writeComment(pw, index - 1, comment, blockComment);
        }
        return this;
    }

    /**
     * Finds the first place where the xpaths differ.
     */
    public int findFirstDifference(XPathParts last) {
        int min = elements.size();
        if (last.elements.size() < min) min = last.elements.size();
        for (int i = 0; i < min; ++i) {
            Element e1 = elements.get(i);
            Element e2 = last.elements.get(i);
            if (!e1.equals(e2)) return i;
        }
        return min;
    }

    /**
     * Checks if the new xpath given is like the this one.
     * The only diffrence may be extra alt and draft attributes but the
     * value of type attribute is the same
     *
     * @param last
     * @return
     */
    public boolean isLike(XPathParts last) {
        int min = elements.size();
        if (last.elements.size() < min) min = last.elements.size();
        for (int i = 0; i < min; ++i) {
            Element e1 = elements.get(i);
            Element e2 = last.elements.get(i);
            if (!e1.equals(e2)) {
                /* is the current element the last one */
                if (i == min - 1) {
                    String et1 = e1.getAttributeValue("type");
                    String et2 = e2.getAttributeValue("type");
                    if (et1 == null && et2 == null) {
                        et1 = e1.getAttributeValue("id");
                        et2 = e2.getAttributeValue("id");
                    }
                    if (et1 != null && et2 != null && et1.equals(et2)) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Does this xpath contain the attribute at all?
     */
    public boolean containsAttribute(String attribute) {
        for (int i = 0; i < elements.size(); ++i) {
            Element element = elements.get(i);
            if (element.getAttributeValue(attribute) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Does it contain the attribute/value pair?
     */
    public boolean containsAttributeValue(String attribute, String value) {
        for (int i = 0; i < elements.size(); ++i) {
            String otherValue = elements.get(i).getAttributeValue(attribute);
            if (otherValue != null && value.equals(otherValue)) return true;
        }
        return false;
    }

    /**
     * How many elements are in this xpath?
     */
    public int size() {
        return elements.size();
    }

    /**
     * Get the nth element. Negative values are from end
     */
    public String getElement(int elementIndex) {
        return elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size()).getElement();
    }

    public int getAttributeCount(int elementIndex) {
        return elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size()).getAttributeCount();
    }

    /**
     * Get the attributes for the nth element (negative index is from end). Returns null or an empty map if there's
     * nothing.
     * PROBLEM: exposes internal map
     */
    public Map<String, String> getAttributes(int elementIndex) {
        return elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size()).getAttributes();
    }

    /**
     * return non-modifiable collection
     *
     * @param elementIndex
     * @return
     */
    public Collection<String> getAttributeKeys(int elementIndex) {
        return elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size())
            .getAttributes()
            .keySet();
    }

    /**
     * Get the attributeValue for the attrbute at the nth element (negative index is from end). Returns null if there's
     * nothing.
     */
    public String getAttributeValue(int elementIndex, String attribute) {
        if (elementIndex < 0) elementIndex += size();
        return elements.get(elementIndex).getAttributeValue(attribute);
    }

    public void putAttributeValue(int elementIndex, String attribute, String value) {
        elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size()).putAttribute(attribute, value);
    }

    /**
     * Get the attributes for the nth element. Returns null or an empty map if there's nothing.
     * PROBLEM: exposes internal map
     */
    public Map<String, String> findAttributes(String elementName) {
        int index = findElement(elementName);
        if (index == -1) return null;
        return getAttributes(index);
    }

    /**
     * Find the attribute value
     */
    public String findAttributeValue(String elementName, String attributeName) {
        Map<String, String> attributes = findAttributes(elementName);
        if (attributes == null) return null;
        return (String) attributes.get(attributeName);
    }

    /**
     * Add an element
     */
    public XPathParts addElement(String element) {
        if (elements.size() == 0) {
            try {
                dtdData = DtdData.getInstance(DtdType.valueOf(element));
            } catch (Exception e) {
                dtdData = null;
            }
        }
        elements.add(new Element(element));
        return this;
    }

    /**
     * Varargs version of addElement.
     *  Usage:  xpp.addElements("ldml","localeDisplayNames")
     * @param element
     * @return this for chaining
     */
    public XPathParts addElements(String... element) {
        for (String e : element) {
            addElement(e);
        }
        return this;
    }

    /**
     * Add an attribute/value pair to the current last element.
     */
    public XPathParts addAttribute(String attribute, String value) {
        Element e = elements.get(elements.size() - 1);
        e.putAttribute(attribute, value);
        return this;
    }

    public XPathParts removeAttribute(String elementName, String attributeName) {
        return removeAttribute(findElement(elementName), attributeName);
    }

    public XPathParts removeAttribute(int elementIndex, String attributeName) {
        elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size()).putAttribute(attributeName, null);
        return this;
    }

    public XPathParts removeAttributes(String elementName, Collection<String> attributeNames) {
        return removeAttributes(findElement(elementName), attributeNames);
    }

    public XPathParts removeAttributes(int elementIndex, Collection<String> attributeNames) {
        elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size()).removeAttributes(attributeNames);
        return this;
    }

    /**
     * Parse out an xpath, and pull in the elements and attributes.
     *
     * @param xPath
     * @return
     */
    public XPathParts set(String xPath) {
        if (frozen) {
            throw new UnsupportedOperationException("Can't modify frozen Element");
        }
        return addInternal(xPath, true);
    }

    /**
     * Set an xpath, but ONLY if 'this' is clear (size = 0)
     *
     * @param xPath
     * @return
     */
    public XPathParts initialize(String xPath) {
        if (size() == 0) {
            set(xPath);
        }
        return this;
    }

    /**
     * Add the given path to this XPathParts.
     *
     * @param xPath the path string
     * @param initial boolean, if true, call elements.clear() and set dtdData = null before adding,
     *                and make requiredPrefix // instead of /
     * @return the XPathParts, or parseError
     *
     * Called by initialize (initial = true), set (initial = true), and addRelative (initial = false)
     */
    private XPathParts addInternal(String xPath, boolean initial) {
        String lastAttributeName = "";
        String requiredPrefix = "/";
        if (initial) {
            elements.clear();
            dtdData = null;
            requiredPrefix = "//";
        }
        if (!xPath.startsWith(requiredPrefix)) {
            return parseError(xPath, 0);
        }
        int stringStart = requiredPrefix.length(); // skip prefix
        char state = 'p';
        // since only ascii chars are relevant, use char
        int len = xPath.length();
        for (int i = 2; i < len; ++i) {
            char cp = xPath.charAt(i);
            if (cp != state && (state == '\"' || state == '\'')) {
                continue; // stay in quotation
            }
            switch (cp) {
            case '/':
                if (state != 'p' || stringStart >= i) return parseError(xPath, i);
                if (stringStart > 0) addElement(xPath.substring(stringStart, i));
                stringStart = i + 1;
                break;
            case '[':
                if (state != 'p' || stringStart >= i) return parseError(xPath, i);
                if (stringStart > 0) addElement(xPath.substring(stringStart, i));
                state = cp;
                break;
            case '@':
                if (state != '[') return parseError(xPath, i);
                stringStart = i + 1;
                state = cp;
                break;
            case '=':
                if (state != '@' || stringStart >= i) return parseError(xPath, i);
                lastAttributeName = xPath.substring(stringStart, i);
                state = cp;
                break;
            case '\"':
            case '\'':
                if (state == cp) { // finished
                    if (stringStart > i) return parseError(xPath, i);
                    addAttribute(lastAttributeName, xPath.substring(stringStart, i));
                    state = 'e';
                    break;
                }
                if (state != '=') return parseError(xPath, i);
                stringStart = i + 1;
                state = cp;
                break;
            case ']':
                if (state != 'e') return parseError(xPath, i);
                state = 'p';
                stringStart = -1;
                break;
            }
        }
        // check to make sure terminated
        if (state != 'p' || stringStart >= xPath.length()) {
            return parseError(xPath, xPath.length());
        }
        if (stringStart > 0) {
            addElement(xPath.substring(stringStart, xPath.length()));
        }
        return this;
    }

    /**
     * boilerplate
     */
    public String toString() {
        return toString(elements.size());
    }

    public String toString(int limit) {
        if (limit < 0) {
            limit += size();
        }
        String result = "/";
        try {
            for (int i = 0; i < limit; ++i) {
                result += elements.get(i).toString(XPATH_STYLE);
            }
        } catch (RuntimeException e) {
            throw e;
        }
        return result;
    }

    public String toString(int start, int limit) {
        if (start < 0) {
            start += size();
        }
        if (limit < 0) {
            limit += size();
        }
        String result = "";
        for (int i = start; i < limit; ++i) {
            result += elements.get(i).toString(XPATH_STYLE);
        }
        return result;
    }

    /**
     * boilerplate
     */
    public boolean equals(Object other) {
        try {
            XPathParts that = (XPathParts) other;
            if (elements.size() != that.elements.size()) return false;
            for (int i = 0; i < elements.size(); ++i) {
                if (!elements.get(i).equals(that.elements.get(i))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * boilerplate
     */
    public int hashCode() {
        int result = elements.size();
        for (int i = 0; i < elements.size(); ++i) {
            result = result * 37 + elements.get(i).hashCode();
        }
        return result;
    }

    // ========== Privates ==========

    private XPathParts parseError(String s, int i) {
        throw new IllegalArgumentException("Malformed xPath '" + s + "' at " + i);
    }

    public static final int XPATH_STYLE = 0, XML_OPEN = 1, XML_CLOSE = 2, XML_NO_VALUE = 3;
    public static final String NEWLINE = "\n";

    private final class Element implements Cloneable, Freezable<Element> {
        private volatile boolean frozen;
        private final String element;
        private Map<String, String> attributes; // = new TreeMap(AttributeComparator);

        public Element(String element) {
            this(element, null);
        }

        public Element(Element other, String element) {
            this(element, other.attributes);
        }

        public Element(String element, Map<String, String> attributes) {
            this.frozen = false;
            this.element = element.intern();  // allow fast comparison
            if (attributes == null) {
                this.attributes = null;
            } else {
                this.attributes = new TreeMap<String, String>(getAttributeComparator());
                this.attributes.putAll(attributes);
            }
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return frozen ? this
                : new Element(element, attributes);
        }

        public void putAttribute(String attribute, String value) {
            attribute = attribute.intern(); // allow fast comparison
            if (frozen) {
                throw new UnsupportedOperationException("Can't modify frozen object.");
            }
            if (value == null) {
                if (attributes != null) {
                    attributes.remove(attribute);
                    if (attributes.size() == 0) {
                        attributes = null;
                    }
                }
            } else {
                if (attributes == null) {
                    attributes = new TreeMap<String, String>(getAttributeComparator());
                }
                attributes.put(attribute, value);
            }
        }

        public void removeAttributes(Collection<String> attributeNames) {
            if (frozen) {
                throw new UnsupportedOperationException("Can't modify frozen object.");
            }
            if (attributeNames == null) {
                return;
            }
            for (String attribute : attributeNames) {
                attributes.remove(attribute);
            }
            if (attributes.size() == 0) {
                attributes = null;
            }
        }

        public String toString() {
            throw new IllegalArgumentException("Don't use");
        }

        /**
         * @param style
         *            from XPATH_STYLE
         * @return
         */
        public String toString(int style) {
            StringBuilder result = new StringBuilder();
            // Set keys;
            switch (style) {
            case XPathParts.XPATH_STYLE:
                result.append('/').append(element);
                writeAttributes("[@", "\"]", false, result);
                break;
            case XPathParts.XML_OPEN:
            case XPathParts.XML_NO_VALUE:
                result.append('<').append(element);
                writeAttributes(" ", "\"", true, result);
                if (style == XML_NO_VALUE) result.append('/');
                if (CLDRFile.HACK_ORDER && element.equals("ldml")) result.append(' ');
                result.append('>');
                break;
            case XML_CLOSE:
                result.append("</").append(element).append('>');
                break;
            }
            return result.toString();
        }

        /**
         * @param element
         *            TODO
         * @param prefix
         *            TODO
         * @param postfix
         *            TODO
         * @param removeLDMLExtras
         *            TODO
         * @param result
         */
        private Element writeAttributes(String prefix, String postfix,
            boolean removeLDMLExtras, StringBuilder result) {
            if (getAttributeCount() == 0) {
                return this;
            }
            for (Entry<String, String> attributesAndValues : attributes.entrySet()) {
                String attribute = attributesAndValues.getKey();
                String value = attributesAndValues.getValue();
                if (removeLDMLExtras && suppressionMap != null) {
                    if (skipAttribute(element, attribute, value)) continue;
                    if (skipAttribute("*", attribute, value)) continue;
                }
                try {
                    result.append(prefix).append(attribute).append("=\"")
                        .append(removeLDMLExtras ? TransliteratorUtilities.toHTML.transliterate(value) : value)
                        .append(postfix);
                } catch (RuntimeException e) {
                    throw e; // for debugging
                }
            }
            return this;
        }

        private boolean skipAttribute(String element, String attribute, String value) {
            Map<String, String> attribute_value = suppressionMap.get(element);
            boolean skip = false;
            if (attribute_value != null) {
                Object suppressValue = attribute_value.get(attribute);
                if (suppressValue == null) suppressValue = attribute_value.get("*");
                if (suppressValue != null) {
                    if (value.equals(suppressValue) || suppressValue.equals("*")) skip = true;
                }
            }
            return skip;
        }

        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            try {
                Element that = (Element) other;
                // == check is ok since we intern elements
                return element == that.element
                    && (attributes == null ? that.attributes == null
                        : that.attributes == null ? attributes == null
                            : attributes.equals(that.attributes));
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return element.hashCode() * 37 + (attributes == null ? 0 : attributes.hashCode());
        }

        public String getElement() {
            return element;
        }

        private int getAttributeCount() {
            if (attributes == null) {
                return 0;
            }
            return attributes.size();
        }

        private Map<String, String> getAttributes() {
            if (attributes == null) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(attributes);
        }

        private String getAttributeValue(String attribute) {
            if (attributes == null) {
                return null;
            }
            return attributes.get(attribute);
        }

        @Override
        public boolean isFrozen() {
            return frozen;
        }

        @Override
        public Element freeze() {
            if (!frozen) {
                attributes = attributes == null ? null
                    : Collections.unmodifiableMap(attributes);
                frozen = true;
            }
            return this;
        }

        @Override
        public Element cloneAsThawed() {
            return new Element(element, attributes);
        }
    }

    /**
     * Search for an element within the path.
     *
     * @param elementName
     *            the element to look for
     * @return element number if found, else -1 if not found
     */
    public int findElement(String elementName) {
        for (int i = 0; i < elements.size(); ++i) {
            Element e = elements.get(i);
            if (!e.getElement().equals(elementName)) continue;
            return i;
        }
        return -1;
    }

    private MapComparator<String> getAttributeComparator() {
        return dtdData == null ? null
            : dtdData.dtdType == DtdType.ldml ? CLDRFile.getAttributeOrdering()
                : dtdData.getAttributeComparator();
    }

    /**
     * Determines if an elementName is contained in the path.
     *
     * @param elementName
     * @return
     */
    public boolean contains(String elementName) {
        return findElement(elementName) >= 0;
    }

    /**
     * add a relative path to this XPathParts.
     */
    public XPathParts addRelative(String path) {
        if (frozen) {
            throw new UnsupportedOperationException("Can't modify frozen Element");
        }
        if (path.startsWith("//")) {
            elements.clear();
            path = path.substring(1); // strip one
        } else {
            while (path.startsWith("../")) {
                path = path.substring(3);
                trimLast();
            }
            if (!path.startsWith("/")) path = "/" + path;
        }
        return addInternal(path, false);
    }

    /**
     */
    public XPathParts trimLast() {
        if (frozen) {
            throw new UnsupportedOperationException("Can't modify frozen Element");
        }
        elements.remove(elements.size() - 1);
        return this;
    }

    /**
     * Replace the elements of this XPathParts with clones of the elements of the given other XPathParts
     *  
     * @param parts
     * @return this XPathParts
     *
     * This is NOT the same function as set(String xPath).
     *
     * Called by XPathParts.replace and CldrItem.split.
     */
    public XPathParts set(XPathParts parts) {
        if (frozen) {
            throw new UnsupportedOperationException("Can't modify frozen Element");
        }
        try {
            dtdData = parts.dtdData;
            elements.clear();
            for (Element element : parts.elements) {
                elements.add((Element) element.clone());
            }
            return this;
        } catch (CloneNotSupportedException e) {
            throw (InternalError) new InternalError().initCause(e);
        }
    }

    /**
     * Replace up to i with parts
     *
     * @param i
     * @param parts
     */
    public XPathParts replace(int i, XPathParts parts) {
        if (frozen) {
            throw new UnsupportedOperationException("Can't modify frozen Element");
        }
        List<Element> temp = elements;
        elements = new ArrayList<Element>();
        set(parts);
        for (; i < temp.size(); ++i) {
            elements.add(temp.get(i));
        }
        return this;
    }

    /**
     * Utility to write a comment.
     *
     * @param pw
     * @param blockComment
     *            TODO
     * @param indent
     */
    static void writeComment(PrintWriter pw, int indent, String comment, boolean blockComment) {
        // now write the comment
        if (comment.length() == 0) return;
        if (blockComment) {
            pw.print(Utility.repeat("\t", indent));
        } else {
            pw.print(" ");
        }
        pw.print("<!--");
        if (comment.indexOf(NEWLINE) > 0) {
            boolean first = true;
            int countEmptyLines = 0;
            // trim the line iff the indent != 0.
            for (Iterator<String> it = CldrUtility.splitList(comment, NEWLINE, indent != 0, null).iterator(); it.hasNext();) {
                String line = it.next();
                if (line.length() == 0) {
                    ++countEmptyLines;
                    continue;
                }
                if (countEmptyLines != 0) {
                    for (int i = 0; i < countEmptyLines; ++i)
                        pw.println();
                    countEmptyLines = 0;
                }
                if (first) {
                    first = false;
                    line = line.trim();
                    pw.print(" ");
                } else if (indent != 0) {
                    pw.print(Utility.repeat("\t", (indent + 1)));
                    pw.print(" ");
                }
                pw.println(line);
            }
            pw.print(Utility.repeat("\t", indent));
        } else {
            pw.print(" ");
            pw.print(comment.trim());
            pw.print(" ");
        }
        pw.print("-->");
        if (blockComment) {
            pw.println();
        }
    }

    /**
     * Utility to determine if this a language locale?
     * Note: a script is included with the language, if there is one.
     *
     * @param in
     * @return
     */
    public static boolean isLanguage(String in) {
        int pos = in.indexOf('_');
        if (pos < 0) return true;
        if (in.indexOf('_', pos + 1) >= 0) return false; // no more than 2 subtags
        if (in.length() != pos + 5) return false; // second must be 4 in length
        return true;
    }

    /**
     * Returns -1 if parent isn't really a parent, 0 if they are identical, and 1 if parent is a proper parent
     */
    public static int isSubLocale(String parent, String possibleSublocale) {
        if (parent.equals("root")) {
            if (parent.equals(possibleSublocale)) return 0;
            return 1;
        }
        if (parent.length() > possibleSublocale.length()) return -1;
        if (!possibleSublocale.startsWith(parent)) return -1;
        if (parent.length() == possibleSublocale.length()) return 0;
        if (possibleSublocale.charAt(parent.length()) != '_') return -1; // last subtag too long
        return 1;
    }

    /**
     * Sets an attribute/value on the first matching element.
     */
    public XPathParts setAttribute(String elementName, String attributeName, String attributeValue) {
        int index = findElement(elementName);
        elements.get(index).putAttribute(attributeName, attributeValue);
        return this;
    }

    public XPathParts removeProposed() {
        for (int i = 0; i < elements.size(); ++i) {
            Element element = elements.get(i);
            if (element.getAttributeCount() == 0) {
                continue;
            }
            for (Entry<String, String> attributesAndValues : element.getAttributes().entrySet()) {
                String attribute = attributesAndValues.getKey();
                if (!attribute.equals("alt")) {
                    continue;
                }
                String attributeValue = attributesAndValues.getValue();
                int pos = attributeValue.indexOf("proposed");
                if (pos < 0) break;
                if (pos > 0 && attributeValue.charAt(pos - 1) == '-') --pos; // backup for "...-proposed"
                if (pos == 0) {
                    element.putAttribute(attribute, null);
                    break;
                }
                attributeValue = attributeValue.substring(0, pos); // strip it off
                element.putAttribute(attribute, attributeValue);
                break; // there is only one alt!
            }
        }
        return this;
    }

    public XPathParts setElement(int elementIndex, String newElement) {
        if (elementIndex < 0) {
            elementIndex += size();
        }
        Element element = elements.get(elementIndex);
        elements.set(elementIndex, new Element(element, newElement));
        return this;
    }

    public XPathParts removeElement(int elementIndex) {
        elements.remove(elementIndex >= 0 ? elementIndex : elementIndex + size());
        return this;
    }

    public String findFirstAttributeValue(String attribute) {
        for (int i = 0; i < elements.size(); ++i) {
            String value = getAttributeValue(i, attribute);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public void setAttribute(int elementIndex, String attributeName, String attributeValue) {
        Element element = elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size());
        element.putAttribute(attributeName, attributeValue);
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public XPathParts freeze() {
        if (!frozen) {
            // ensure that it can't be modified. Later we can fix all the call sites to check frozen.
            List<Element> temp = new ArrayList<>(elements.size());
            for (Element element : elements) {
                temp.add(element.freeze());
            }
            elements = Collections.unmodifiableList(temp);
            frozen = true;
        }
        return this;
    }

    @Override
    public XPathParts cloneAsThawed() {
        return new XPathParts(elements, suppressionMap);
    }

    public static synchronized XPathParts getFrozenInstance(String path) {
        XPathParts result = cache.get(path);
        if (result == null) {
            cache.put(path, result = new XPathParts().set(path).freeze());
        }
        return result;
    }

    public static XPathParts getInstance(String path) {
        return getFrozenInstance(path).cloneAsThawed();
    }

    /**
     * Same as getInstance, but temporarily distinguished for testing; for some callers getFrozenInstance might work as well and be faster
     * TODO: replace with getInstance or getFrozenInstance, when complete https://unicode.org/cldr/trac/ticket/12007
     * @param path
     * @return the XPathParts
     */
    public static XPathParts getTestInstance(String path) {
        return getInstance(path);
        // return getFrozenInstance(path);
    }

    public DtdData getDtdData() {
        return dtdData;
    }

    public Set<String> getElements() {
        Builder<String> builder = ImmutableSet.builder();
        for (int i = 0; i < elements.size(); ++i) {
            builder.add(elements.get(i).getElement());
        }
        return builder.build();
    }

    public Map<String, String> getSpecialNondistinguishingAttributes() {
        Map<String, String> ueMap = null; // common case, none found.
        for (int i = 0; i < this.size(); i++) {
            // taken from XPathTable.getUndistinguishingElementsFor, with some cleanup
            // from XPathTable.getUndistinguishingElements, we include alt, draft
            for (Entry<String, String> entry : this.getAttributes(i).entrySet()) {
                String k = entry.getKey();
                if (getDtdData().isDistinguishing(getElement(i), k)
                    || k.equals("alt") // is always distinguishing, so we don't really need this.
                    || k.equals("draft")) {
                    continue;
                }
                if (ueMap == null) {
                    ueMap = new TreeMap<String, String>();
                }
                ueMap.put(k, entry.getValue());
            }
        }
        return ueMap;
    }
}
