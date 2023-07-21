package org.unicode.cldr.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Like {@link XPathParts} but does not depend on DTDs, etc. Safe for use at startup. */
public class SimpleXPathParts extends XPathParser {

    private final class Element {
        public final String name;
        public final Map<String, String> attributes = new TreeMap<>();

        public Element(String name) {
            this.name = name;
        }
    }

    final List<Element> elements = new LinkedList<>();

    @Override
    public String getElement(int i) {
        if (i < 0) {
            i = elements.size() + i;
        }
        return elements.get(i).name;
    }

    @Override
    public String getAttributeValue(int i, String attribute) {
        if (i < 0) {
            i = elements.size() + i;
        }
        return elements.get(i).attributes.get(attribute);
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
        handleParse(xpath, true);
    }

    /**
     * for API compatibility
     *
     * @param xpath
     * @return
     */
    static SimpleXPathParts getFrozenInstance(String xpath) {
        return new SimpleXPathParts(xpath);
    }
}
