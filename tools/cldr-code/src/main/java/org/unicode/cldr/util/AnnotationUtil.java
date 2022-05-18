package org.unicode.cldr.util;

import java.util.regex.Pattern;

public class AnnotationUtil {
    private static final String PATH_PREFIX = "//ldml/annotations/annotation";

    private static final Pattern BAD_EMOJI = Pattern.compile("E\\d+(\\.\\d+)?-\\d+");

    /**
     * Does the given value match an annotation code like "E10-840"?
     *
     * @param value like "E10-836" (bad) or "grinning face" (good)
     * @return true if value matches a code (bad), else false (good)
     */
    public static boolean matchesCode(String value) {
        return BAD_EMOJI.matcher(value).matches();
    }

    public static boolean pathIsAnnotation(String path) {
        return path.startsWith(PATH_PREFIX);
    }
}
