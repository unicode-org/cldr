/*
 ******************************************************************************
 * Copyright (C) 2004-2013, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.util;

import java.io.File;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
public final class XPathParts implements Freezable<XPathParts>, Comparable<XPathParts> {
    private static final boolean DEBUGGING = false;

    private volatile boolean frozen = false;

    private List<Element> elements = new ArrayList<>();

    private DtdData dtdData = null;

    private static final Map<String, XPathParts> cache = new ConcurrentHashMap<>();

    /**
     * Construct a new empty XPathParts object.
     *
     * Note: for faster performance, call getFrozenInstance or getInstance instead of this constructor.
     * This constructor remains public for special cases in which individual elements are added with
     * addElement rather than using a complete path string.
     */
    public XPathParts() {

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
     * Empty the xpath
     *
     * Called by JsonConverter.rewrite() and CLDRFile.write()
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
     * @param pw the PrintWriter to receive output
     * @param filteredXPath used for calling filteredXPath.writeComment; may or may not be same as "this";
     *        "filtered" is from xpath, while "this" may be from getFullXPath(xpath)
     * @param lastFullXPath the last XPathParts (not filtered), or null (to be treated same as empty)
     * @param v getStringValue(xpath); or empty string
     * @param xpath_comments the Comments object; or null
     * @return this XPathParts
     *
     * Note: this method gets THREE XPathParts objects: this, filteredXPath, and lastFullXPath.
     *
     * TODO: create a unit test that calls this function directly.
     *
     * Called only by XMLModify.main and CLDRFile.write, as follows:
     *
     * CLDRFile.write:
     *    current.writeDifference(pw, current, last, "", tempComments);
     *    current.writeDifference(pw, currentFiltered, last, v, tempComments);
     *
     * XMLModify.main:
     *    parts.writeDifference(out, parts, lastParts, value, null);
     */
    public XPathParts writeDifference(PrintWriter pw, XPathParts filteredXPath, XPathParts lastFullXPath,
        String v, Comments xpath_comments) {
        int limit = (lastFullXPath == null) ? 0 : findFirstDifference(lastFullXPath);
        if (lastFullXPath != null) {
            // write the end of the last one
            for (int i = lastFullXPath.size() - 2; i >= limit; --i) {
                pw.print(Utility.repeat("\t", i));
                pw.println(lastFullXPath.elements.get(i).toString(XML_CLOSE));
            }
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

    /**
     * Write the last xpath.
     *
     * last.writeLast(pw) is equivalent to current.clear().writeDifference(pw, null, last, null, tempComments).
     *
     * @param pw the PrintWriter to receive output
     */
    public void writeLast(PrintWriter pw) {
        for (int i = this.size() - 2; i >= 0; --i) {
            pw.print(Utility.repeat("\t", i));
            pw.println(elements.get(i).toString(XML_CLOSE));
        }
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

        private EnumMap<CommentType, Map<String, String>> comments = new EnumMap<>(
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
            List<String> result = new ArrayList<>();
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

        @Override
        public Object clone() {
            try {
                Comments result = (Comments) super.clone();
                for (CommentType c : CommentType.values()) {
                    result.comments.put(c, new HashMap<>(comments.get(c)));
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
        if (elementIndex < 0) {
            elementIndex += size();
        }
        return elements.get(elementIndex).getAttributeValue(attribute);
    }

    public void putAttributeValue(int elementIndex, String attribute, String value) {
        elementIndex = elementIndex >= 0 ? elementIndex : elementIndex + size();
        Map<String, String> ea = elements.get(elementIndex).attributes;
        if (value == null && (ea == null || !ea.containsKey(attribute))) {
            return;
        }
        if (value != null && ea != null && value.equals(ea.get(attribute))) {
            return;
        }
        makeElementsMutable();
        makeElementMutable(elementIndex);
        // make mutable may change elements.get(elementIndex), so we have to use elements.get(elementIndex) after calling
        elements.get(elementIndex).putAttribute(attribute, value);
    }

    /**
     * Get the attributes for the nth element. Returns null or an empty map if there's nothing.
     * PROBLEM: exposes internal map
     */
    public Map<String, String> findAttributes(String elementName) {
        int index = findElement(elementName);
        if (index == -1) {
            return null;
        }
        return getAttributes(index);
    }

    /**
     * Find the attribute value
     */
    public String findAttributeValue(String elementName, String attributeName) {
        Map<String, String> attributes = findAttributes(elementName);
        if (attributes == null) {
            return null;
        }
        return attributes.get(attributeName);
    }

    /**
     * Add an Element object to this XPathParts, using the given element name.
     * If this is the first Element in this XPathParts, also set dtdData.
     * Do not set any attributes.
     *
     * @param element the string describing the element, such as "ldml",
     *                "supplementalData", etc.
     * @return this XPathParts
     */
    public XPathParts addElement(String element) {
        if (elements.size() == 0) {
            try {
                /*
                 * The first element should match one of the DtdType enum values.
                 * Use it to set dtdData.
                 */
                File dir = CLDRConfig.getInstance().getCldrBaseDirectory();
                dtdData = DtdData.getInstance(DtdType.valueOf(element), dir);
            } catch (Exception e) {
                dtdData = null;
            }
        }
        makeElementsMutable();
        elements.add(new Element(element));
        return this;
    }

    public void makeElementsMutable() {
        if (frozen) {
            throw new UnsupportedOperationException("Can't modify frozen object.");
        }

        if (elements instanceof ImmutableList) {
            elements = new ArrayList<>(elements);
        }
    }

    public void makeElementMutable(int elementIndex) {
        if (frozen) {
            throw new UnsupportedOperationException("Can't modify frozen object.");
        }

        Element e = elements.get(elementIndex);
        Map<String, String> ea = e.attributes;
        if (ea == null || ea instanceof ImmutableMap) {
            elements.set(elementIndex, e.cloneAsThawed());
        }
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
        putAttributeValue(elements.size() - 1, attribute, value);
        return this;
    }

    public XPathParts removeAttribute(String elementName, String attributeName) {
        return removeAttribute(findElement(elementName), attributeName);
    }

    public XPathParts removeAttribute(int elementIndex, String attributeName) {
        putAttributeValue(elementIndex, attributeName, null);
        return this;
    }

    public XPathParts removeAttributes(String elementName, Collection<String> attributeNames) {
        return removeAttributes(findElement(elementName), attributeNames);
    }

    public XPathParts removeAttributes(int elementIndex, Collection<String> attributeNames) {
        elementIndex = elementIndex >= 0 ? elementIndex : elementIndex + size();
        Map<String, String> ea = elements.get(elementIndex).attributes;
        if (ea == null || attributeNames == null || attributeNames.isEmpty() || Collections.disjoint(attributeNames, ea.keySet())) {
            return this;
        }
        makeElementsMutable();
        makeElementMutable(elementIndex);
        // make mutable may change elements.get(elementIndex), so we have to use elements.get(elementIndex) after calling
        elements.get(elementIndex).removeAttributes(attributeNames);
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
     * Called by set (initial = true), and addRelative (initial = false)
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
                if (state != 'p' || stringStart >= i) {
                    return parseError(xPath, i);
                }
                if (stringStart > 0) {
                    addElement(xPath.substring(stringStart, i));
                }
                stringStart = i + 1;
                break;
            case '[':
                if (state != 'p' || stringStart >= i) {
                    return parseError(xPath, i);
                }
                if (stringStart > 0) {
                    addElement(xPath.substring(stringStart, i));
                }
                state = cp;
                break;
            case '@':
                if (state != '[') {
                    return parseError(xPath, i);
                }
                stringStart = i + 1;
                state = cp;
                break;
            case '=':
                if (state != '@' || stringStart >= i) {
                    return parseError(xPath, i);
                }
                lastAttributeName = xPath.substring(stringStart, i);
                state = cp;
                break;
            case '\"':
            case '\'':
                if (state == cp) { // finished
                    if (stringStart > i) {
                        return parseError(xPath, i);
                    }
                    addAttribute(lastAttributeName, xPath.substring(stringStart, i));
                    state = 'e';
                    break;
                }
                if (state != '=') {
                    return parseError(xPath, i);
                }
                stringStart = i + 1;
                state = cp;
                break;
            case ']':
                if (state != 'e') {
                    return parseError(xPath, i);
                }
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
    @Override
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
    @Override
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

    @Override
    public int compareTo(XPathParts that) {
        return dtdData.getDtdComparator().xpathComparator(this, that);
    }


    /**
     * boilerplate
     */
    @Override
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

    private final class Element {
        private final String element;
        private Map<String, String> attributes; // = new TreeMap(AttributeComparator);

        public Element(String element) {
            this(element, null);
        }

        public Element(Element other, String element) {
            this(element, other.attributes);
        }

        public Element(String element, Map<String, String> attributes) {
            this.element = element.intern();  // allow fast comparison
            if (attributes == null) {
                this.attributes = null;
            } else {
                this.attributes = new TreeMap<>(getAttributeComparator());
                this.attributes.putAll(attributes);
            }
        }

        /**
         * Add the given attribute, value pair to this Element object; or,
         * if value is null, remove the attribute.
         *
         * @param attribute, the string such as "number" or "cldrVersion"
         * @param value, the string such as "$Revision$" or "35", or null for removal
         */
        public void putAttribute(String attribute, String value) {
            attribute = attribute.intern(); // allow fast comparison
            if (value == null) {
                if (attributes != null) {
                    attributes.remove(attribute);
                    if (attributes.size() == 0) {
                        attributes = null;
                    }
                }
            } else {
                if (attributes == null) {
                    attributes = new TreeMap<>(getAttributeComparator());
                }
                attributes.put(attribute, value);
            }
        }

        /**
         * Remove the given attributes from this Element object.
         *
         * @param attributeNames
         */
        private void removeAttributes(Collection<String> attributeNames) {
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

        @Override
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
                if (style == XML_NO_VALUE) {
                    result.append('/');
                }
                if (CLDRFile.HACK_ORDER && element.equals("ldml")) {
                    result.append(' ');
                }
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
            Map<String, Map<String, String>> suppressionMap = null;
            if (removeLDMLExtras) {
                suppressionMap = CLDRFile.getDefaultSuppressionMap();
            }
            for (Entry<String, String> attributesAndValues : attributes.entrySet()) {
                String attribute = attributesAndValues.getKey();
                String value = attributesAndValues.getValue();
                if (removeLDMLExtras && suppressionMap != null) {
                    if (skipAttribute(element, attribute, value, suppressionMap)) {
                        continue;
                    }
                    if (skipAttribute("*", attribute, value, suppressionMap)) {
                        continue;
                    }
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

        /**
         * Should writeAttributes skip the given element, attribute, and value?
         *
         * @param element
         * @param attribute
         * @param value
         * @return true to skip, else false
         *
         * Called only by writeAttributes
         *
         * Assume suppressionMap isn't null.
         */
        private boolean skipAttribute(String element, String attribute, String value,
            Map<String, Map<String, String>> suppressionMap) {
            Map<String, String> attribute_value = suppressionMap.get(element);
            boolean skip = false;
            if (attribute_value != null) {
                Object suppressValue = attribute_value.get(attribute);
                if (suppressValue == null) {
                    suppressValue = attribute_value.get("*");
                }
                if (suppressValue != null) {
                    if (value.equals(suppressValue) || suppressValue.equals("*")) {
                        skip = true;
                    }
                }
            }
            return skip;
        }

        @Override
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

        @Override
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
                return ImmutableMap.of();
            }
            return ImmutableMap.copyOf(attributes);
        }

        private String getAttributeValue(String attribute) {
            if (attributes == null) {
                return null;
            }
            return attributes.get(attribute);
        }

        public Element makeImmutable() {
            if (attributes != null && !(attributes instanceof ImmutableMap)) {
                attributes = ImmutableMap.copyOf(attributes);
            }

            return this;
        }

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
            if (!e.getElement().equals(elementName)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /**
     * Get the MapComparator for this XPathParts.
     *
     * @return the MapComparator, or null
     *
     * Called by the Element constructor, and by putAttribute
     */
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
        makeElementsMutable();
        elements.remove(elements.size() - 1);
        return this;
    }

    /**
     * Replace the elements of this XPathParts with clones of the elements of the given other XPathParts
     *
     * @param parts the given other XPathParts (not modified)
     * @return this XPathParts (modified)
     *
     * Called by XPathParts.replace and CldrItem.split.
     */
//   If this is restored, it will need to be modified.
//    public XPathParts set(XPathParts parts) {
//        if (frozen) {
//            throw new UnsupportedOperationException("Can't modify frozen Element");
//        }
//        try {
//            dtdData = parts.dtdData;
//            elements.clear();
//            for (Element element : parts.elements) {
//                elements.add((Element) element.clone());
//            }
//            return this;
//        } catch (CloneNotSupportedException e) {
//            throw (InternalError) new InternalError().initCause(e);
//        }
//    }

    /**
     * Replace up to i with parts
     *
     * @param i
     * @param parts
     */
//    If this is restored, it will need to be modified.
//    public XPathParts replace(int i, XPathParts parts) {
//        if (frozen) {
//            throw new UnsupportedOperationException("Can't modify frozen Element");
//        }
//        List<Element> temp = elements;
//        elements = new ArrayList<>();
//        set(parts);
//        for (; i < temp.size(); ++i) {
//            elements.add(temp.get(i));
//        }
//        return this;
//    }

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
        putAttributeValue(index, attributeName, attributeValue);
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
                    putAttributeValue(i, attribute, null);
                    break;
                }
                attributeValue = attributeValue.substring(0, pos); // strip it off
                putAttributeValue(i, attribute, attributeValue);
                break; // there is only one alt!
            }
        }
        return this;
    }

    public XPathParts setElement(int elementIndex, String newElement) {
        makeElementsMutable();
        if (elementIndex < 0) {
            elementIndex += size();
        }
        Element element = elements.get(elementIndex);
        elements.set(elementIndex, new Element(element, newElement));
        return this;
    }

    public XPathParts removeElement(int elementIndex) {
        makeElementsMutable();
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

    public XPathParts setAttribute(int elementIndex, String attributeName, String attributeValue) {
        putAttributeValue(elementIndex, attributeName, attributeValue);
        return this;
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
                temp.add(element.makeImmutable());
            }
            elements = ImmutableList.copyOf(temp);
            frozen = true;
        }
        return this;
    }

    @Override
    public XPathParts cloneAsThawed() {
        XPathParts xppClone = new XPathParts();
        /*
         * Remember to copy dtdData.
         * Reference: https://unicode.org/cldr/trac/ticket/12007
         */
        xppClone.dtdData = this.dtdData;
        if (!frozen) {
            for (Element e : this.elements) {
                xppClone.elements.add(e.cloneAsThawed());
            }
        } else {
            xppClone.elements = this.elements;
        }
        return xppClone;
    }

    public static XPathParts getFrozenInstance(String path) {
        XPathParts result = cache.computeIfAbsent(path,
            (String forPath) -> new XPathParts().addInternal(forPath, true).freeze());
        return result;
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
                    ueMap = new TreeMap<>();
                }
                ueMap.put(k, entry.getValue());
            }
        }
        return ueMap;
    }

    public static String getPathWithoutAlt(String xpath) {
        XPathParts xpp = getFrozenInstance(xpath).cloneAsThawed();
        xpp.removeAttribute("alt");
        return xpp.toString();
    }

    private XPathParts removeAttribute(String attribute) {
        for (int i = 0; i < elements.size(); ++i) {
            putAttributeValue(i, attribute, null);
        }
        return this;
    }
}
