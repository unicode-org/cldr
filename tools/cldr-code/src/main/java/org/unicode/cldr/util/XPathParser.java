package org.unicode.cldr.util;

/** This class implements the parsing for XPaths */
public abstract class XPathParser implements XPathValue {
    /**
     * Parse a string component. Will call handleXX() functions when parsed.
     *
     * @param xPath the path string
     * @param initial boolean, if true, will call clearElements() before adding, and make
     *     requiredPrefix // instead of /
     */
    protected void handleParse(String xPath, boolean initial) {
        String lastAttributeName = "";
        String requiredPrefix = "/";
        if (initial) {
            handleClearElements();
            requiredPrefix = "//";
        }
        if (!xPath.startsWith(requiredPrefix)) {
            parseError(xPath, 0);
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
                        parseError(xPath, i);
                    }
                    if (stringStart > 0) {
                        handleAddElement(xPath.substring(stringStart, i));
                    }
                    stringStart = i + 1;
                    break;
                case '[':
                    if (state != 'p' || stringStart >= i) {
                        parseError(xPath, i);
                    }
                    if (stringStart > 0) {
                        handleAddElement(xPath.substring(stringStart, i));
                    }
                    state = cp;
                    break;
                case '@':
                    if (state != '[') {
                        parseError(xPath, i);
                    }
                    stringStart = i + 1;
                    state = cp;
                    break;
                case '=':
                    if (state != '@' || stringStart >= i) {
                        parseError(xPath, i);
                    }
                    lastAttributeName = xPath.substring(stringStart, i);
                    state = cp;
                    break;
                case '\"':
                case '\'':
                    if (state == cp) { // finished
                        if (stringStart > i) {
                            parseError(xPath, i);
                        }
                        handleAddAttribute(lastAttributeName, xPath.substring(stringStart, i));
                        state = 'e';
                        break;
                    }
                    if (state != '=') {
                        parseError(xPath, i);
                    }
                    stringStart = i + 1;
                    state = cp;
                    break;
                case ']':
                    if (state != 'e') {
                        parseError(xPath, i);
                    }
                    state = 'p';
                    stringStart = -1;
                    break;
            }
        }
        // check to make sure terminated
        if (state != 'p' || stringStart >= xPath.length()) {
            parseError(xPath, xPath.length());
        }
        if (stringStart > 0) {
            handleAddElement(xPath.substring(stringStart, xPath.length()));
        }
    }

    /**
     * Standardized code for throwing a parse error
     *
     * @param s
     * @param i
     */
    protected void parseError(String s, int i) {
        throw new IllegalArgumentException("Malformed xPath '" + s + "' at " + i);
    }

    /** Subclass implementation */
    protected abstract void handleClearElements();

    /**
     * Subclass implementation
     *
     * @param element
     */
    protected abstract void handleAddElement(String element);

    /** Subclass implementation */
    protected abstract void handleAddAttribute(String attribute, String value);
}
