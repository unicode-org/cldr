package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.CollectionUtilities;

public class PathStarrer {
    static final String STAR_PATTERN = "([^\"]*)";
    static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("=\"([^\"]*)\"");
    
    private String starredPathString;
    private final List<String> attributes = new ArrayList<String>();
    private final List<String> protectedAttributes = Collections.unmodifiableList(attributes);
    private final StringBuilder starredPath = new StringBuilder();
    private String substitutionPattern = STAR_PATTERN;

    public String set(String path) {
        Matcher starAttributeMatcher = ATTRIBUTE_PATTERN.matcher(path);
        starredPath.setLength(0);
        attributes.clear();
        int lastEnd = 0;
        while (starAttributeMatcher.find()) {
            int start = starAttributeMatcher.start(1);
            int end = starAttributeMatcher.end(1);
            starredPath.append(path.substring(lastEnd, start));
            starredPath.append(".*");

            attributes.add(path.substring(start, end));
            lastEnd = end;
        }
        starredPath.append(path.substring(lastEnd));
        starredPathString = starredPath.toString();
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

}
