package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Transform;

/**
 * Transforms a path by replacing attributes with .*
 *
 * @author markdavis
 */
public class PathStarrer implements Transform<String, String> {
    public static final String STAR_PATTERN = "([^\"]*+)";

    private String starredPathString;
    private final List<String> attributes = new ArrayList<String>();
    private final List<String> protectedAttributes = Collections.unmodifiableList(attributes);
    private String substitutionPattern = STAR_PATTERN;

    private static final Pattern ATTRIBUTE_PATTERN_OLD = PatternCache.get("=\"([^\"]*)\"");
    private final StringBuilder starredPathOld = new StringBuilder();

    public String set(String path) {
        XPathParts parts = XPathParts.getInstance(path);
        return set(parts, Collections.<String> emptySet());
    }

    /**
     * Sets the path starrer attributes, and returns the string.
     * @param parts
     * @return
     */
    public String set(XPathParts parts, Set<String> skipAttributes) {
        attributes.clear();
        for (int i = 0; i < parts.size(); ++i) {
            for (String key : parts.getAttributeKeys(i)) {
                if (!skipAttributes.contains(key)) {
                    attributes.add(parts.getAttributeValue(i, key));
                    parts.setAttribute(i, key, substitutionPattern);
                }
            }
        }
        starredPathString = parts.toString();
        return starredPathString;
    }

    public String setOld(String path) {
        Matcher starAttributeMatcher = ATTRIBUTE_PATTERN_OLD.matcher(path);
        starredPathOld.setLength(0);
        attributes.clear();
        int lastEnd = 0;
        while (starAttributeMatcher.find()) {
            int start = starAttributeMatcher.start(1);
            int end = starAttributeMatcher.end(1);
            starredPathOld.append(path.substring(lastEnd, start));
            starredPathOld.append(substitutionPattern);

            attributes.add(path.substring(start, end));
            lastEnd = end;
        }
        starredPathOld.append(path.substring(lastEnd));
        starredPathString = starredPathOld.toString();
        return starredPathString;
    }

    public List<String> getAttributes() {
        return protectedAttributes;
    }

    public String getAttributesString(String separator) {
        return CollectionUtilities.join(attributes, separator);
    }

    public String getResult() {
        return starredPathString;
    }

    public String getSubstitutionPattern() {
        return substitutionPattern;
    }

    public PathStarrer setSubstitutionPattern(String substitutionPattern) {
        this.substitutionPattern = substitutionPattern;
        return this;
    }

    @Override
    public String transform(String source) {
        return set(source);
    }

    // Used for coverage lookups - strips off the leading ^ and trailing $ from regexp pattern.
    public String transform2(String source) {
        String result = Utility.unescape(setOld(source));
        if (result.startsWith("^") && result.endsWith("$")) {
            result = result.substring(1, result.length() - 1);
        }
        //System.out.println("Path in  => "+source);
        //System.out.println("Path out => "+result);
        //System.out.println("-----------");

        return result;
    }
}
