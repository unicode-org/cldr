package org.unicode.cldr.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.VersionInfo;

public class SimpleXMLSource extends XMLSource {
    private Map<String, String> xpath_value = CldrUtility.newConcurrentHashMap();
    private Map<String, String> xpath_fullXPath = CldrUtility.newConcurrentHashMap();
    private Comments xpath_comments = new Comments(); // map from paths to comments.
    private Relation<String, String> VALUE_TO_PATH = null;
    private Object VALUE_TO_PATH_MUTEX = new Object();
    private VersionInfo dtdVersionInfo;

    public SimpleXMLSource(String localeID) {
        this.setLocaleID(localeID);
    }

    /**
     * Create a shallow, locked copy of another XMLSource.
     *
     * @param copyAsLockedFrom
     */
    protected SimpleXMLSource(SimpleXMLSource copyAsLockedFrom) {
        this.xpath_value = copyAsLockedFrom.xpath_value;
        this.xpath_fullXPath = copyAsLockedFrom.xpath_fullXPath;
        this.xpath_comments = copyAsLockedFrom.xpath_comments;
        this.setLocaleID(copyAsLockedFrom.getLocaleID());
        locked = true;
    }

    public String getValueAtDPath(String xpath) {
        return (String) xpath_value.get(xpath);
    }

    public String getFullPathAtDPath(String xpath) {
        String result = (String) xpath_fullXPath.get(xpath);
        if (result != null) return result;
        if (xpath_value.get(xpath) != null) return xpath; // we don't store duplicates
        // System.err.println("WARNING: "+getLocaleID()+": path not present in data: " + xpath);
        // return xpath;
        return null; // throw new IllegalArgumentException("Path not present in data: " + xpath);
    }

    public Comments getXpathComments() {
        return xpath_comments;
    }

    public void setXpathComments(Comments xpath_comments) {
        this.xpath_comments = xpath_comments;
    }

    // public void putPathValue(String xpath, String value) {
    // if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    // String distinguishingXPath = CLDRFile.getDistinguishingXPath(xpath, fixedPath);
    // xpath_value.put(distinguishingXPath, value);
    // if (!fixedPath[0].equals(distinguishingXPath)) {
    // xpath_fullXPath.put(distinguishingXPath, fixedPath[0]);
    // }
    // }
    public void removeValueAtDPath(String distinguishingXPath) {
        String oldValue = xpath_value.get(distinguishingXPath);
        xpath_value.remove(distinguishingXPath);
        xpath_fullXPath.remove(distinguishingXPath);
        updateValuePathMapping(distinguishingXPath, oldValue, null);
    }

    public Iterator<String> iterator() { // must be unmodifiable or locked
        return Collections.unmodifiableSet(xpath_value.keySet()).iterator();
    }

    public XMLSource freeze() {
        locked = true;
        return this;
    }

    public XMLSource cloneAsThawed() {
        SimpleXMLSource result = (SimpleXMLSource) super.cloneAsThawed();
        result.xpath_comments = (Comments) result.xpath_comments.clone();
        result.xpath_fullXPath = CldrUtility.newConcurrentHashMap(result.xpath_fullXPath);
        result.xpath_value = CldrUtility.newConcurrentHashMap(result.xpath_value);
        return result;
    }

    public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
        xpath_fullXPath.put(distinguishingXPath, fullxpath);
    }

    public void putValueAtDPath(String distinguishingXPath, String value) {
        String oldValue = xpath_value.get(distinguishingXPath);
        xpath_value.put(distinguishingXPath, value);
        updateValuePathMapping(distinguishingXPath, oldValue, value);
    }

    private void updateValuePathMapping(String distinguishingXPath, String oldValue, String newValue) {
        synchronized (VALUE_TO_PATH_MUTEX) {
            if (VALUE_TO_PATH != null) {
                if (oldValue != null) {
                    VALUE_TO_PATH.remove(normalize(oldValue), distinguishingXPath);
                }
                if (newValue != null) {
                    VALUE_TO_PATH.put(normalize(newValue), distinguishingXPath);
                }
            }
        }
    }

    @Override
    public void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result) {
        // build a Relation mapping value to paths, if needed
        synchronized (VALUE_TO_PATH_MUTEX) {
            if (VALUE_TO_PATH == null) {
                VALUE_TO_PATH = Relation.of(new HashMap<String, Set<String>>(), HashSet.class);
                for (Iterator<String> it = iterator(); it.hasNext();) {
                    String path = it.next();
                    String value = normalize(getValueAtDPath(path));
                    VALUE_TO_PATH.put(value, path);
                }
            }
            Set<String> paths = VALUE_TO_PATH.getAll(normalize(valueToMatch));
            if (paths == null) {
                return;
            }
            if (pathPrefix == null || pathPrefix.length() == 0) {
                result.addAll(paths);
                return;
            }
            for (String path : paths) {
                if (path.startsWith(pathPrefix)) {
                    // if (altPath.originalPath.startsWith(altPrefix.originalPath)) {
                    result.add(path);
                }
            }
        }
    }

    static final Normalizer2 NFKCCF = Normalizer2.getNFKCCasefoldInstance();
    static final Normalizer2 NFKC = Normalizer2.getNFKCInstance();

    // The following includes letters, marks, numbers, currencies, and *selected* symbols/punctuation
    static final UnicodeSet NON_ALPHANUM = new UnicodeSet("[^[:L:][:M:][:N:][:Sc:]/+\\-°′″%‰٪؉−⍰()⊕☉]").freeze();

    public static String normalize(String valueToMatch) {
        return replace(NON_ALPHANUM, NFKCCF.normalize(valueToMatch), "");
    }

    public static String normalizeCaseSensitive(String valueToMatch) {
        return replace(NON_ALPHANUM, NFKC.normalize(valueToMatch), "");
    }

    public static String replace(UnicodeSet unicodeSet, String valueToMatch, String substitute) {
        // handle patterns
        if (valueToMatch.contains("{")) {
            valueToMatch = PLACEHOLDER.matcher(valueToMatch).replaceAll("⍰").trim();
        }
        StringBuilder b = null; // delay creating until needed
        for (int i = 0; i < valueToMatch.length(); ++i) {
            int cp = valueToMatch.codePointAt(i);
            if (unicodeSet.contains(cp)) {
                if (b == null) {
                    b = new StringBuilder();
                    b.append(valueToMatch.substring(0, i)); // copy the start
                }
                if (substitute.length() != 0) {
                    b.append(substitute);
                }
            } else if (b != null) {
                b.appendCodePoint(cp);
            }
            if (cp > 0xFFFF) { // skip end of supplemental character
                ++i;
            }
        }
        if (b != null) {
            valueToMatch = b.toString();
        }
        return valueToMatch;
    }

    static final Pattern PLACEHOLDER = PatternCache.get("\\{\\d\\}");

    public void setDtdVersionInfo(VersionInfo dtdVersionInfo) {
        this.dtdVersionInfo = dtdVersionInfo;
    }

    public VersionInfo getDtdVersionInfo() {
        return dtdVersionInfo;
    }
}