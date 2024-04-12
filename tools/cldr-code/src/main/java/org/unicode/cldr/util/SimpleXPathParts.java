package org.unicode.cldr.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Similar to {@link XPathParts} but does not depend on DTDs, etc. Doesn't cache, so most
 * appropriate for one-time data loading where paths will be unique. Safe for use during static
 * init. API conforms to {@link XPathValue}
 */
public class SimpleXPathParts extends XPathParser {

    private final class Element {
        public final String name;
        public final Map<String, String> attributes = new TreeMap<>();

        public Element(String name) {
            this.name = name;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public boolean containsAttribute(String attribute) {
            return attributes.containsKey(attribute);
        }
    }

    final List<Element> elements = new LinkedList<>();
    private final String xpath;

    @Override
    public String getElement(int i) {
        if (i < 0) {
            i = elements.size() + i;
        }
        return elements.get(i).name;
    }

    /**
     * Search for an element within the path.
     *
     * @param elementName the element to look for
     * @return element number if found, else -1 if not found
     */
    public int findElement(String elementName) {
        for (int i = 0; i < elements.size(); ++i) {
            Element e = elements.get(i);
            if (!e.name.equals(elementName)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    @Override
    public boolean containsElement(String elementName) {
        return findElement(elementName) >= 0;
    }

    @Override
    public String getAttributeValue(int i, String attribute) {
        if (i < 0) {
            i = elements.size() + i;
        }
        return elements.get(i).attributes.get(attribute);
    }

    @Override
    public Map<String, String> getAttributes(int elementIndex) {
        return elements.get(elementIndex >= 0 ? elementIndex : elementIndex + size())
                .getAttributes();
    }

    @Override
    public boolean containsAttribute(String attribute) {
        for (final Element e : elements) {
            if (e.containsAttribute(attribute)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void handleClearElements() {
        elements.clear();
    }

    @Override
    protected void handleAddElement(String element) {
        elements.add(new Element(element));
    }

    @Override
    protected void handleAddAttribute(String attribute, String value) {
        elements.get(elements.size() - 1).attributes.put(attribute, value);
    }

    SimpleXPathParts(String xpath) {
        this.xpath = xpath;
        handleParse(xpath, true);
    }

    @Override
    public String toString() {
        return xpath;
    }

    /**
     * Parse an XPath string into a read-only structure.
     *
     * @param xpath
     * @return the parsed XPath
     */
    static XPathValue getFrozenInstance(String xpath) {
        // return the abstract class
        return new SimpleXPathParts(xpath);
    }

    @Override
    public int size() {
        return elements.size();
    }
}
