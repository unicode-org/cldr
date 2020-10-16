package org.unicode.cldr.api;

import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;
import static org.unicode.cldr.api.CldrData.PathOrder.DTD;

import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.api.CldrData.PrefixVisitor;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk;

/**
 * Tests for the code which converts a sequence of path/value pairs into a nested sequence of path
 * prefixes (this is necessary when using prefix visitors for data obtained via CLDRFile). 
 */
public final class PrefixVisitorTest extends TestFmwk {

    public void TestEmptyData() {
        CldrData testData = CldrDataSupplier.forValues(ImmutableList.of());
        PrefixVisitor v = new PrefixVisitor() {
            @Override
            public void visitPrefixStart(CldrPath prefix, Context ctx) {
                fail("no visitation should occur");
            }

            @Override
            public void visitPrefixEnd(CldrPath prefix) {
                fail("no visitation should occur");
            }
        };
        testData.accept(ARBITRARY, v);
    }

    public void TestSinglePathProcessing() {
        List<CldrValue> values = new ArrayList<>();
        addTo(values, "//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]", "Switzerland");

        CldrData testData = CldrDataSupplier.forValues(values);
        List<String> prefixes = new ArrayList<>();
        PrefixVisitor v = new PrefixVisitor() {
            @Override
            public void visitPrefixStart(CldrPath prefix, Context ctx) {
                prefixes.add("+" + prefix);
            }

            @Override
            public void visitPrefixEnd(CldrPath prefix) {
                prefixes.add("-" + prefix);
            }
        };
        testData.accept(ARBITRARY, v);
        List<String> expected = ImmutableList.of(
            "+//ldml",
            "+//ldml/localeDisplayNames",
            "+//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames",
            "-//ldml");
        assertEquals("cldr path prefixes", expected, prefixes);
    }

    public void TestSameParentPathProcessing() {
        List<CldrValue> values = new ArrayList<>();
        addTo(values, "//ldml/localeDisplayNames/territories/territory[@type=\"BE\"]", "Belgium");
        addTo(values, "//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]", "Switzerland");
        addTo(values, "//ldml/localeDisplayNames/territories/territory[@type=\"IN\"]", "India");

        CldrData testData = CldrDataSupplier.forValues(values);
        List<String> prefixes = new ArrayList<>();
        PrefixVisitor v = new PrefixVisitor() {
            @Override
            public void visitPrefixStart(CldrPath prefix, Context ctx) {
                prefixes.add("+" + prefix);
            }

            @Override
            public void visitPrefixEnd(CldrPath prefix) {
                prefixes.add("-" + prefix);
            }
        };
        testData.accept(ARBITRARY, v);
        // Same as above since the transition to sibling paths does not trigger start/end events.
        List<String> expected = ImmutableList.of(
            "+//ldml",
            "+//ldml/localeDisplayNames",
            "+//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames",
            "-//ldml");
        assertEquals("cldr path prefixes", expected, prefixes);
    }

    public void TestMultiplePathProcessing() {
        List<CldrValue> values = new ArrayList<>();
        // Note that the order is deliberately "bad" (since the prefixes are not grouped) but this
        // should be corrected for, even when path order is specified as "ARBITRARY".
        addTo(values, "//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]", "Switzerland");
        addTo(values,
            "//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras/eraAbbr/era[@type=\"0\"]",
            "BE");
        addTo(values, "//ldml/dates/fields/field[@type=\"era\"]/displayName", "era");
        addTo(values, "//ldml/localeDisplayNames/languages/language[@type=\"fr\"]", "French");

        CldrData testData = CldrDataSupplier.forValues(values);
        List<String> prefixes = new ArrayList<>();
        PrefixVisitor v = new PrefixVisitor() {
            @Override
            public void visitPrefixStart(CldrPath prefix, Context ctx) {
                prefixes.add("+" + prefix);
            }

            @Override
            public void visitPrefixEnd(CldrPath prefix) {
                prefixes.add("-" + prefix);
            }
        };
        testData.accept(ARBITRARY, v);
        List<String> expectedNested = ImmutableList.of(
            "+//ldml",
            "+//ldml/dates",
            "+//ldml/dates/calendars",
            "+//ldml/dates/calendars/calendar[@type=\"buddhist\"]",
            "+//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras",
            "+//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras/eraAbbr",
            "-//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras/eraAbbr",
            "-//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras",
            "-//ldml/dates/calendars/calendar[@type=\"buddhist\"]",
            "-//ldml/dates/calendars",
            "+//ldml/dates/fields",
            "+//ldml/dates/fields/field[@type=\"era\"]",
            "-//ldml/dates/fields/field[@type=\"era\"]",
            "-//ldml/dates/fields",
            "-//ldml/dates",
            "+//ldml/localeDisplayNames",
            "+//ldml/localeDisplayNames/languages",
            "-//ldml/localeDisplayNames/languages",
            "+//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames",
            "-//ldml");
        assertEquals("cldr path prefixes", expectedNested, prefixes);

        prefixes.clear();
        testData.accept(DTD, v);
        List<String> expectedDtd = ImmutableList.of(
            "+//ldml",
            "+//ldml/localeDisplayNames",
            "+//ldml/localeDisplayNames/languages",
            "-//ldml/localeDisplayNames/languages",
            "+//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames/territories",
            "-//ldml/localeDisplayNames",
            "+//ldml/dates",
            "+//ldml/dates/calendars",
            "+//ldml/dates/calendars/calendar[@type=\"buddhist\"]",
            "+//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras",
            "+//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras/eraAbbr",
            "-//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras/eraAbbr",
            "-//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras",
            "-//ldml/dates/calendars/calendar[@type=\"buddhist\"]",
            "-//ldml/dates/calendars",
            "+//ldml/dates/fields",
            "+//ldml/dates/fields/field[@type=\"era\"]",
            "-//ldml/dates/fields/field[@type=\"era\"]",
            "-//ldml/dates/fields",
            "-//ldml/dates",
            "-//ldml");
        assertEquals("cldr path prefixes", expectedDtd, prefixes);
    }

    public void TestNestedPrefixVisitors() {
        List<CldrValue> values = new ArrayList<>();
        addTo(values, "//ldml/dates/fields/field[@type=\"era\"]/displayName", "era");
        addTo(values, "//ldml/localeDisplayNames/languages/language[@type=\"fr\"]", "French");
        addTo(values, "//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]", "Switzerland");

        CldrData testData = CldrDataSupplier.forValues(values);

        List<String> prefixes = new ArrayList<>();
        class SubVisitor implements PrefixVisitor {
            private final PathMatcher matcher;
            private final SubVisitor subVisitor;
            private final String name;

            public SubVisitor(
                String name, PathMatcher matcher, SubVisitor subVisitor) {
                this.matcher = matcher;
                this.subVisitor = subVisitor;
                this.name = name;
            }

            @Override
            public void visitPrefixStart(CldrPath prefix, Context ctx) {
                prefixes.add("+" + name + ": " + prefix);
                if (subVisitor != null && matcher.matches(prefix)) {
                    // Bound sub-visitors with markers (just to test the "done function" really).
                    prefixes.add("---->");
                    ctx.install(subVisitor, v -> prefixes.add("<----"));
                }
            }

            @Override
            public void visitPrefixEnd(CldrPath prefix) {
                prefixes.add("-" + name + ": " + prefix);
            }
        }

        // These matchers are visited by the parent doing the matching and the child visitor sees
        // everything below this point (but not the path itself).
        PathMatcher ldml = PathMatcher.of("//ldml");
        PathMatcher names = ldml.withSuffix("localeDisplayNames");
        // Visitors constructed in reverse (since the child is passed into the parent).
        SubVisitor baz = new SubVisitor("baz", null, null);
        SubVisitor bar = new SubVisitor("bar", names, baz);
        SubVisitor foo = new SubVisitor("foo", ldml, bar);

        testData.accept(DTD, foo);
        List<String> expectedNested = ImmutableList.of(
            "+foo: //ldml",
            "---->",  // Everything below //ldml is visited by "bar".
            "+bar: //ldml/localeDisplayNames",
            "---->",  // Everything below //ldml/localeDisplayNames is visited by "baz".
            "+baz: //ldml/localeDisplayNames/languages",
            "-baz: //ldml/localeDisplayNames/languages",
            "+baz: //ldml/localeDisplayNames/territories",
            "-baz: //ldml/localeDisplayNames/territories",
            "<----",  // The "done function" is called at the upwards transition of visitors.
            "-bar: //ldml/localeDisplayNames",
            "+bar: //ldml/dates",
            "+bar: //ldml/dates/fields",
            "+bar: //ldml/dates/fields/field[@type=\"era\"]",
            "-bar: //ldml/dates/fields/field[@type=\"era\"]",
            "-bar: //ldml/dates/fields",
            "-bar: //ldml/dates",
            "<----",
            "-foo: //ldml");
        assertEquals("cldr path prefixes", expectedNested, prefixes);
    }

    // Note: Just testing the test supplier, not the real suppliers (their tests are elsewhere).
    public void TestParentPathsNotAllowed() {
        List<CldrValue> values = new ArrayList<>();
        addTo(values, "//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]", "Switzerland");
        addTo(values, "//ldml/localeDisplayNames/territories", "Invalid Parent Path");

        try {
            CldrDataSupplier.forValues(values);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue("exception message contains",
                e.getMessage().contains("//ldml/localeDisplayNames/territories"));
        }
    }

    private static void addTo(List<CldrValue> values, String fullPath, String value) {
        values.add(CldrValue.parseValue(fullPath, value));
    }
}
