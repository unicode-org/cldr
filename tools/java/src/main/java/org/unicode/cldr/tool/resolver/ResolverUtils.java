/*
 * Copyright (C) 2004-2011, Unicode, Inc., Google, Inc., and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */

package org.unicode.cldr.tool.resolver;

import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRFile;

/**
 * Utility methods for the CLDR Resolver tool
 *
 * @author ryanmentley@google.com (Ryan Mentley)
 */
public class ResolverUtils {
    /**
     * Output level from 0-5. 0 is nothing, 1 is errors, 2-3 is pretty sane, 5
     * will flood your terminal.
     */
    static int verbosity = 2;

    /**
     * This is a static class and should never be instantiated
     */
    private ResolverUtils() {
    }

    /**
     * Get the set of paths with non-null values from a CLDR file (including all
     * extra paths).
     *
     * @param file the CLDRFile from which to extract paths
     * @return a Set containing all the paths returned by
     *         {@link CLDRFile#iterator()}, plus those from
     *         {@link CLDRFile#getExtraPaths(java.util.Collection)}
     */
    public static Set<String> getAllPaths(CLDRFile file) {
        String locale = file.getLocaleID();
        Set<String> paths = new HashSet<String>();
        for (String path : file) {
            paths.add(path);
        }
        for (String path : file.getExtraPaths()) {
            if (file.getStringValue(path) != null) {
                paths.add(path);
            } else {
                debugPrintln(path + " is null in " + locale + ".", 3);
            }
        }
        return paths;
    }

    /**
     * Debugging method used to make null and empty strings more obvious in
     * printouts
     *
     * @param str the string
     * @return "[null]" if str==null, "[empty]" if str is the empty string, str
     *         otherwise
     */
    public static String strRep(String str) {
        if (str == null) {
            return "[null]";
        } else if (str.isEmpty()) {
            return "[empty]";
        } else {
            return str;
        }
    }

    /**
     * Debugging method to print things based on verbosity.
     *
     * @param str The string to print
     * @param msgVerbosity The minimum verbosity level at which to print this message
     */
    static void debugPrint(String str, int msgVerbosity) {
        if (verbosity >= msgVerbosity) {
            System.out.print(str);
        }
    }

    /**
     * Debugging method to print things based on verbosity.
     *
     * @param str The string to print
     * @param msgVerbosity The minimum verbosity level at which to print this message
     */
    static void debugPrintln(String str, int msgVerbosity) {
        debugPrint(str + "\n", msgVerbosity);
    }
}
