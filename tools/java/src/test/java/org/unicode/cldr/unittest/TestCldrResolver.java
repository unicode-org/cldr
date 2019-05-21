/*
 * Copyright (C) 2004-2011, Unicode, Inc., Google, Inc., and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.tool.resolver.CldrResolver;
import org.unicode.cldr.tool.resolver.ResolutionType;
import org.unicode.cldr.tool.resolver.ResolverUtils;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;

/**
 * Runs all the CLDR Resolver Tool tests
 *
 * @author jchye@google.com (Jennifer Chye), ryanmentley@google.com (Ryan
 *         Mentley)
 */
public class TestCldrResolver extends TestFmwkPlus {

    static CLDRConfig testInfo = CLDRConfig.getInstance();

    private static final String LOCALES_TO_TEST = ".*";

    public void TestSimpleResolution() {
        try {
            new SimpleResolverTest().testResolution();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void TestFullResolution() {
        new ResolverTest(ResolutionType.FULL).testResolution();
    }

    public void TestNoCodeFallback() {
        new ResolverTest(ResolutionType.NO_CODE_FALLBACK) {
            @Override
            protected boolean shouldIgnorePath(String distinguishedPath,
                CLDRFile file) {
                return super.shouldIgnorePath(distinguishedPath, file)
                    || file.getSourceLocaleID(distinguishedPath, null)
                        .equals(CldrResolver.CODE_FALLBACK);
            }
        }.testResolution();
    }

    /**
     * Main method that runs all CLDR Resolver tests
     *
     * @param args
     *            Command-line arguments
     */
    public static void main(String[] args) {
        new TestCldrResolver().run(args);
    }

    /**
     * A superclass for all Resolver tests to encompass common functionality
     *
     * @author ryanmentley@google.com (Ryan Mentley)
     */
    private class ResolverTest {
        /**
         * Gets the fully-resolved data for the locale.
         *
         * @param locale
         *            the locale for which to get the map
         * @return an immutable Map from distinguished path to string value
         */
        protected Map<String, String> loadToolDataFromResolver(String locale) {
            // Resolve with the tool
            CLDRFile file = resolver.resolveLocale(locale);
            Map<String, String> values = new HashMap<String, String>();
            for (String path : file) {
                values.put(path, file.getStringValue(path));
            }
            return values;
        }

        /**
         * Determines whether an XPath should be ignored for testing purposes
         *
         * @param distinguishedPath
         *            a distinguished XPath
         * @param file
         *            the CLDRFile from which the path was retrieved
         * @return {@code true} if the path should be ignored; {@code false}
         *         otherwise
         */
        protected boolean shouldIgnorePath(String distinguishedPath,
            CLDRFile file) {
            return distinguishedPath.endsWith("/alias")
                || distinguishedPath.startsWith("//ldml/identity/");
        }

        private Factory factory;
        private CldrResolver resolver;

        public ResolverTest(ResolutionType resolutionType) {
            factory = testInfo.getCldrFactory();
            resolver = new CldrResolver(factory, resolutionType);
        }

        /**
         * Template method to test any type of CLDR resolution
         */
        public final void testResolution() {
            Set<String> locales = resolver.getLocaleNames(LOCALES_TO_TEST);

            // Resolve with CLDR and check against CldrResolver output.
            for (String locale : locales) {
                CLDRFile cldrResolved = testInfo.getCLDRFile(locale, true);
                Set<String> cldrPaths = new HashSet<String>();
                Map<String, String> toolResolved = loadToolDataFromResolver(locale);
                // Check to make sure no paths from the CLDR-resolved version
                // that aren't
                // explicitly excluded get left out
                for (String distinguishedPath : ResolverUtils
                    .getAllPaths(cldrResolved)) {
                    // Check if path should be ignored
                    if (!shouldIgnorePath(distinguishedPath, cldrResolved)) {
                        String cldrValue = cldrResolved
                            .getStringValue(distinguishedPath);
                        String toolValue = toolResolved.get(distinguishedPath);
                        assertNotNull(
                            "Path "
                                + distinguishedPath
                                + " is present in CLDR resolved file for locale "
                                + locale
                                + " but not in tool resolved file (CLDR value: '"
                                + cldrValue + "').",
                            toolValue);
                        assertEquals("Tool resolved value for "
                            + distinguishedPath + " in locale " + locale
                            + " should match CLDRFile resolved value",
                            cldrValue, toolValue);
                        // Add the path to the Set for the next batch of checks
                        cldrPaths.add(distinguishedPath);
                    }
                }
                // Check to make sure that all paths from the tool-resolved
                // version are
                // also in the CLDR-resolved version
                for (String canonicalPath : toolResolved.keySet()) {
                    // Check if path should be ignored
                    if (!shouldIgnorePath(canonicalPath, cldrResolved)) {
                        assertTrue(
                            "Path "
                                + canonicalPath
                                + " is present in tool resolved file for locale "
                                + locale
                                + " but not in CLDR resolved file.",
                            cldrPaths.contains(canonicalPath)
                                || toolResolved
                                    .get(canonicalPath)
                                    .equals(CldrUtility.NO_INHERITANCE_MARKER));
                    }
                }
            }
        }
    }

    private class SimpleResolverTest extends ResolverTest {
        public SimpleResolverTest() {
            super(ResolutionType.SIMPLE);
        }

        @Override
        protected Map<String, String> loadToolDataFromResolver(String locale) {
            String parent = LocaleIDParser.getSimpleParent(locale);
            if (parent == null) {
                // locale is root, just grab it straight out of the unresolved
                // data
                return super.loadToolDataFromResolver(locale);
            } else {
                Map<String, String> resolvedParent = loadToolDataFromResolver(parent);
                Map<String, String> resolvedChild = new HashMap<String, String>(
                    resolvedParent);
                Map<String, String> unresolvedChild = super.loadToolDataFromResolver(locale);
                for (String distinguishedPath : unresolvedChild.keySet()) {

                    String childValue = unresolvedChild.get(distinguishedPath);
                    if (!distinguishedPath.startsWith("//ldml/identity/")) {
                        // Ignore the //ldml/identity/ elements
                        String parentValue = resolvedParent
                            .get(distinguishedPath);
                        assertNotEquals(
                            "Child ("
                                + locale
                                + ") should not contain values that are the same in the truncation parent locale ("
                                + parent + ") at path '"
                                + distinguishedPath + "'.",
                            childValue,
                            parentValue);
                    }
                    // Overwrite the parent value
                    resolvedChild.put(distinguishedPath, childValue);
                }
                return resolvedChild;
            }
        }
    }
}
