package org.unicode.cldr.util;

/**
 * Utilities for the TestShims
 * should go away when we move to all junit tests.
 * These are here so they can be shared between tools/java and tools/cldr-apps
 */
public class TestShimUtils {
    public static String[] getArgs(Class<?> clazz, String defaultArgs) {
        final String packageName = clazz.getPackage().getName();
        final String propKey = packageName+".testArgs";
        final String toSplit = System.getProperty(propKey, defaultArgs);
        System.err.println(propKey+"="+toSplit);
        final String s[] = toSplit.split(" "); // TODO: quoted strings, etc.
        return s;
    }
}
