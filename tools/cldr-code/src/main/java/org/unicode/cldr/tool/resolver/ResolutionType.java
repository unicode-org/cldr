// Copyright 2011 Google Inc. All Rights Reserved.

package org.unicode.cldr.tool.resolver;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Types of CLDR resolution
 *
 * @author ryanmentley@google.com (Ryan Mentley)
 */
public enum ResolutionType {
    SIMPLE, FULL, NO_CODE_FALLBACK;

    /* These are to allow multiple names for the same resolution types */

    /**
     * A list of resolve types that will result in the simple inheritance model
     */
    private static final List<String> SIMPLE_INHERITANCE = Arrays.asList(new String[] { "s", "simple",
        "simpleinheritance", "simple-inheritance", "p", "partial" });

    /**
     * A list of resolve types that will result in the fully-resolved inheritance
     * model
     */
    private static final List<String> FULLY_RESOLVED = Arrays.asList(new String[] { "f", "full",
        "fully", "fullyresolved", "fully-resolved" });

    /**
     * A list of resolve types that will result in the fully-resolved inheritance
     * model with code-fallback suppressed
     */
    private static final List<String> FULLY_RESOLVED_WITHOUT_CODE_FALLBACK = Arrays
        .asList(new String[] { "n", "nc", "nocode", "no-code", "nocodefallback", "no-code-fallback" });

    /**
     * Gets a ResolutionType corresponding to a given string
     *
     * @param str the string to resolve to a ResolutionType
     * @throws IllegalArgumentException if str does not correspond to any known
     *         resolution type
     * @return a ResolutionType
     */
    public static ResolutionType forString(String str) {
        str = str.toLowerCase(Locale.ENGLISH);
        if (SIMPLE_INHERITANCE.contains(str)) {
            return SIMPLE;
        } else if (FULLY_RESOLVED.contains(str)) {
            return FULL;
        } else if (FULLY_RESOLVED_WITHOUT_CODE_FALLBACK.contains(str)) {
            return NO_CODE_FALLBACK;
        } else {
            throw new IllegalArgumentException("\"" + str + "\" is not a known type of resolution.");
        }
    }
}
