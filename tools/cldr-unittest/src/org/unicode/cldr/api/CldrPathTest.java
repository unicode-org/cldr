package org.unicode.cldr.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.test.TestFmwk;

import java.util.Comparator;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Tests for the core CLDR path representation. Since this is an immutable value type, the tests
 * largely focus on things like parsing and validity checking. 
 */
// TODO(dbeaumont): Add more tests for the details of DTD ordering.
public final class CldrPathTest extends TestFmwk {
    // An arbitrary set of full path strings (must have value elements) and their corresponding
    // distinguishing paths.
    private static final ImmutableMap<String, String> FULL_PATHS =
        ImmutableMap.<String, String>builder()
            .put(
                "//ldmlBCP47/keyword"
                    + "/key[@name=\"tz\"][@description=\"Time zone key\"][@alias=\"timezone\"]"
                    + "/type[@name=\"adalv\"][@description=\"Andorra\"][@alias=\"Europe/Andorra\"]",
                "//ldmlBCP47/keyword/key[@name=\"tz\"]/type[@name=\"adalv\"]")
            .put("//supplementalData/info[@iso4217=\"AMD\"][@digits=\"2\"]"
                    + "[@rounding=\"0\"][@cashDigits=\"0\"][@cashRounding=\"0\"]",
                "//supplementalData/info[@iso4217=\"AMD\"]")
            .build();

    // An arbitrary set of distinguishing path strings (no value elements).
    private static final ImmutableList<String> DISTINGUISHING_PATHS =
        ImmutableList.<String>builder()
            .addAll(FULL_PATHS.values())
            .add("//ldml/localeDisplayNames/territories/territory[@type=\"US\"]")
            .add("//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]")
            .add("//ldml/dates/fields/field[@type=\"era\"]/displayName")
            .add("//ldml/rbnf/rulesetGrouping[@type=\"NumberingSystemRules\"]"
                + "/ruleset#0[@type=\"armenian-lower\"]/rbnfrule#10")
            .build();

    public void TestSimple() {
        CldrPath p = CldrPath.parseDistinguishingPath(
            "//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]");
        assertEquals("path length", 4, p.getLength());
        assertEquals("element name", "territory", p.getName());
        assertEquals("sort index", -1, p.getSortIndex());
        assertEquals("attribute count", 1, p.getAttributeCount());
        assertEquals("parent name", "territories", p.getParent().getName());
        assertEquals("parent attribute count", 0, p.getParent().getAttributeCount());
    }

    public void TestSortIndex() {
        CldrPath p = CldrPath.parseDistinguishingPath(
            "//ldml/rbnf/rulesetGrouping[@type=\"NumberingSystemRules\"]"
                + "/ruleset#0[@type=\"armenian-lower\"]/rbnfrule#10");
        assertEquals("path length", 5, p.getLength());

        assertEquals("element name", "rbnfrule", p.getName());
        assertEquals("sort index", 10, p.getSortIndex());
        assertEquals("element name", "ruleset", p.getParent().getName());
        assertEquals("sort index", 0, p.getParent().getSortIndex());
    }

    public void TestInvalid() {
        try {
            CldrPath.parseDistinguishingPath("");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error message", "path must not be empty", e.getMessage());
        }
        try {
            CldrPath.parseDistinguishingPath("//");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error message", "invalid path: //", e.getMessage());
        }
        try {
            CldrPath.parseDistinguishingPath("Hello World");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error message", "invalid path: Hello World", e.getMessage());
        }
    }

    public void TestInvalidDtd() {
        try {
            CldrPath.parseDistinguishingPath("//foo");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error message", "unknown DTD type: //foo", e.getMessage());
        }
    }

    public void TestInvalidElementName() {
        try {
            CldrPath.parseDistinguishingPath("//ldml/foo");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error message", "invalid path: //ldml/foo", e.getMessage());
        }
    }

    public void TestInvalidAtributeName() {
        try {
            CldrPath.parseDistinguishingPath(
                "//ldml/localeDisplayNames/territories/territory[@foo=\"CH\"]");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("error message",
                "invalid path: //ldml/localeDisplayNames/territories/territory[@foo=\"CH\"]",
                e.getMessage());
        }
    }

    public void TestDistinguishingPathParsing() {
        for (String s : DISTINGUISHING_PATHS) {
            CldrPath.parseDistinguishingPath(s);
        }
        // Paths with value attributes should not parse. 
        for (String s : FULL_PATHS.keySet()) {
            try {
                CldrPath.parseDistinguishingPath(s);
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                assertTrue("error message matches",
                    e.getMessage().matches(
                        "unexpected value attribute '.*' in distinguishing path: .*"));
            }
        }
    }

    public void TestFullPathParsing() {
        for (String s : FULL_PATHS.keySet()) {
            // The parsed path should be only the distinguishing path.
            CldrPath p = CldrValue.parseValue(s, "").getPath();
            assertEquals("distinguishing paths", FULL_PATHS.get(s), p.toString());
        }
    }

    public void TestDistinguishingPathRoundTrip() {
        for (String s1 : DISTINGUISHING_PATHS) {
            CldrPath p1 = CldrPath.parseDistinguishingPath(s1);
            String s2 = p1.toString();
            assertEquals("path string", s1, s2);
            CldrPath p2 = CldrPath.parseDistinguishingPath(p1.toString());
            assertEquals("paths", p1, p2);
        }
    }

    public void TestDtdOrder() {
        ImmutableList<String> sorted =
            DISTINGUISHING_PATHS.stream()
                .map(CldrPath::parseDistinguishingPath)
                .sorted(Comparator.naturalOrder())
                .map(Object::toString)
                .collect(toImmutableList());
        assertEquals("sorted paths", ImmutableList.of(
            "//ldmlBCP47/keyword/key[@name=\"tz\"]/type[@name=\"adalv\"]",
            "//supplementalData/info[@iso4217=\"AMD\"]",
            "//ldml/localeDisplayNames/territories/territory[@type=\"CH\"]",
            "//ldml/localeDisplayNames/territories/territory[@type=\"US\"]",
            "//ldml/dates/fields/field[@type=\"era\"]/displayName",
            "//ldml/rbnf/rulesetGrouping[@type=\"NumberingSystemRules\"]"
                + "/ruleset#0[@type=\"armenian-lower\"]/rbnfrule#10"),
            sorted);
    }

    public void TestDraftStatus() {
        CldrPath p = CldrPath.parseDistinguishingPath(
            "//ldml/numbers/currencies/currency[@type=\"MGA\"]"
                + "/displayName[@count=\"one\"][@draft=\"contributed\"]");
        assertEquals("path string",
            "//ldml/numbers/currencies/currency[@type=\"MGA\"]/displayName[@count=\"one\"]",
            p.toString());
        assertEquals("draft status", CldrDraftStatus.CONTRIBUTED, p.getDraftStatus().get());
    }
}
