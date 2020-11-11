// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.cldr.api;

import static java.util.stream.Collectors.toList;
import static org.unicode.cldr.api.CldrPath.parseDistinguishingPath;

import java.util.Arrays;
import java.util.List;

import com.ibm.icu.dev.test.TestFmwk;

public class PathMatcherTest extends TestFmwk {
    public void TestMatcher() {
        CldrPath calEra = parseDistinguishingPath(
            "//ldml/dates/calendars/calendar[@type=\"buddhist\"]/eras/eraAbbr/era[@type=\"0\"]");
        CldrPath chineseMon1 = monthInfo("chinese", "format", "abbreviated", 1);
        CldrPath chineseMon2 = monthInfo("chinese", "format", "abbreviated", 2);
        CldrPath genericMon1 = monthInfo("generic", "stand-alone", "narrow", 1);
        CldrPath genericMon2 = monthInfo("generic", "stand-alone", "narrow", 2);
        List<CldrPath> calPaths =
            Arrays.asList(calEra, chineseMon1, chineseMon2, genericMon1, genericMon2);

        PathMatcher calendarPrefix = PathMatcher.of("//ldml/dates/calendars/calendar[@type=*]");
        assertTrue("is prefix match", calPaths.stream().allMatch(calendarPrefix::matchesPrefixOf));
        assertTrue("not full match", calPaths.stream().noneMatch(calendarPrefix::matches));

        PathMatcher chineseCalendars =
            PathMatcher.of("//ldml/dates/calendars/calendar[@type=\"chinese\"]");
        assertEquals("chinese data",
            calPaths.stream().filter(chineseCalendars::matchesPrefixOf).collect(toList()),
            Arrays.asList(chineseMon1, chineseMon2));

        PathMatcher anyMonth = calendarPrefix
            .withSuffix("months/monthContext[@type=*]/monthWidth[@type=*]/month[@type=*]");
        assertEquals("any month",
            calPaths.stream().filter(anyMonth::matches).collect(toList()),
            Arrays.asList(chineseMon1, chineseMon2, genericMon1, genericMon2));

        PathMatcher narrowMonth = calendarPrefix
            .withSuffix("months/monthContext[@type=*]/monthWidth[@type=\"narrow\"]/month[@type=*]");
        assertEquals("narrow month",
            calPaths.stream().filter(narrowMonth::matches).collect(toList()),
            Arrays.asList(genericMon1, genericMon2));

        PathMatcher firstMonth = calendarPrefix
            .withSuffix("months/monthContext[@type=*]/monthWidth[@type=*]/month[@type=\"1\"]");
        assertEquals("narrow month",
            calPaths.stream().filter(firstMonth::matches).collect(toList()),
            Arrays.asList(chineseMon1, genericMon1));

        PathMatcher fullMatch = PathMatcher.of("//ldml/dates"
            + "/calendars/calendar[@type=\"generic\"]"
            + "/months/monthContext[@type=\"stand-alone\"]"
            + "/monthWidth[@type=\"narrow\"]"
            + "/month[@type=\"2\"]");
        assertEquals("full match",
            calPaths.stream().filter(fullMatch::matches).collect(toList()),
            Arrays.asList(genericMon2));
    }

    public void TestWildcardSegment() {
        PathMatcher wildcard = PathMatcher.of("//ldml/dates"
            + "/calendars/calendar[@type=\"generic\"]"
            + "/*/*[@type=\"format\"]/*[@type=\"narrow\"]/*[@type=*]");

        assertTrue("", wildcard.matches(monthInfo("generic", "format", "narrow", 1)));
        assertTrue("", wildcard.matches(monthInfo("generic", "format", "narrow", 9)));
        assertTrue("", wildcard.matches(dayInfo("generic", "format", "narrow", "sun")));

        assertFalse("", wildcard.matches(monthInfo("chinese", "format", "narrow", 1)));
        assertFalse("", wildcard.matches(monthInfo("generic", "stand-alone", "narrow", 1)));
        assertFalse("", wildcard.matches(dayInfo("generic", "format", "wide", "mon")));
    }

    public void TestBadSpecifiers() {
        assertInvalidPathSpecification("");
        // Leading and trailing '/' are not permitted (they imply empty segments.
        assertInvalidPathSpecification("/foo/");
        assertInvalidPathSpecification("foo//bar");
        assertInvalidPathSpecification("foo/bad segment name");
        assertInvalidPathSpecification("foo/bar[type=*]");
        assertInvalidPathSpecification("foo/bar[@type=**]");
        assertInvalidPathSpecification("foo/bar[@type='double-quotes-only']");
    }

    private void assertInvalidPathSpecification(String spec) {
        try {
            PathMatcher.of(spec);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue("invalid spec: " + spec,
                e.getMessage().startsWith("invalid path pattern")
                    && e.getMessage().contains(spec));
        }
    }

    private static CldrPath monthInfo(String type, String context, String width, int number) {
        return CldrPath.parseDistinguishingPath(String.format(
            "//ldml/dates/calendars/calendar[@type=\"%s\"]"
                + "/months/monthContext[@type=\"%s\"]"
                + "/monthWidth[@type=\"%s\"]"
                + "/month[@type=\"%d\"]",
            type, context, width, number));
    }

    private static CldrPath dayInfo(String type, String context, String width, String id) {
        return CldrPath.parseDistinguishingPath(String.format(
            "//ldml/dates/calendars/calendar[@type=\"%s\"]"
                + "/days/dayContext[@type=\"%s\"]"
                + "/dayWidth[@type=\"%s\"]"
                + "/day[@type=\"%s\"]",
            type, context, width, id));
    }
}
