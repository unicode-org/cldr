package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ibm.icu.dev.test.UnicodeKnownIssues;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.unicode.cldr.rdf.QueryClient;
import org.unicode.cldr.test.CheckQuotes;
import org.unicode.cldr.tool.Chart;
import org.unicode.cldr.web.HttpStatusCache;
import org.unicode.cldr.web.SubtypeToURLMap;

/**
 * Tests for web link vitality.
 * Disabled by default to avoid network traffic.
 *
 * To enable, set the CLDR_TEST_ENABLE_NET=true property
 * You can run this in this way:
 *  mvn --file=tools/pom.xml -pl cldr-apps -DCLDR_TEST_ENABLE_NET=true -Dtest=TestCLDRLinks test
 */
public class TestCLDRLinks {
    public static final boolean CLDR_TEST_ENABLE_NET = Boolean.parseBoolean(System.getProperty("CLDR_TEST_ENABLE_NET", "false"));

    /**
     * Call this to make sure it's OK to make network calls.
     */
    void assertNetOk() {
        assumeTrue(CLDR_TEST_ENABLE_NET, "CLDR_TEST_ENABLE_NET not true, not attempting network read");
    }

    private void assertURLOK(final String url, final String xpath) throws MalformedURLException {
        assertNetOk();
        final URL asURL = new URL(url); // make sure the URL parses OK
        assertTrue(HttpStatusCache.check(url), () -> String.format("Failed: %d <%s> from %s",
            HttpStatusCache.check(asURL), // this overload returns a status number, e.g. 404
            url,
            xpath));
    }

    @ParameterizedTest(name = "{0}—{1}")
    @MethodSource("pathDescriptionProvider")
    public void TestPathDescriptionLinks(final String url, final String xpath) throws MalformedURLException {
        assertURLOK(url, xpath);
    }

    /**
     * Provider for above test
     * @return
     */
    public static Stream<Arguments> pathDescriptionProvider() {
        CLDRConfig config = CLDRConfig.getInstance();
        CLDRFile english = config.getEnglish();
        PathDescription pathDescriptionFactory = new PathDescription(config.getSupplementalDataInfo(), english, null, null,
            PathDescription.ErrorHandling.CONTINUE);

        final Map<String, String> urls = new TreeMap<String, String>();

        for (final String xpath : english.fullIterable()) {
            final String description = pathDescriptionFactory.getRawDescription(xpath, "VALUE", null);
            // System.out.println(description);
            for (final String url : urlsFromString(description)) {
                urls.putIfAbsent(url, xpath);
            }
        }
        return urls.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    @ParameterizedTest(name = "{0}—{1}")
    @MethodSource("miscUrlProvider")
    /**
     * Test for other URLs
     * @param url
     * @param xpath
     * @throws MalformedURLException
     */
    public void TestMiscUrl(final String url, final String xpath) throws MalformedURLException {
        assertURLOK(url, xpath);
    }

    public static Stream<Arguments> miscUrlProvider() {
        final List<Arguments> l = new LinkedList<Arguments>();

        l.add(Arguments.of(CLDRURLS.CLDR_REPO_BASE, "CLDRURLS.CLDR_REPO_BASE"));
        l.add(Arguments.of(CLDRURLS.DEFAULT_BASE, "CLDRURLS.DEFAULT_BASE"));
        l.add(Arguments.of(CLDRURLS.CLDR_NEWTICKET_URL, "CLDRURLS.CLDR_NEWTICKET_URL"));
        l.add(Arguments.of(CLDRURLS.CLDR_REPO_ROOT, "CLDRURLS.CLDR_REPO_ROOT"));
        l.add(Arguments.of(CLDRURLS.CLDR_NEWTICKET_URL, "CLDRURLS.CLDR_NEWTICKET_URL"));
        l.add(Arguments.of(CLDRURLS.CLDR_HOMEPAGE, "CLDRURLS.CLDR_HOMEPAGE"));
        l.add(Arguments.of(CLDRURLS.CLDR_UPDATINGDTD_URL, "CLDRURLS.CLDR_UPDATINGDTD_URL"));
        l.add(Arguments.of(CLDRURLS.TOOLSURL, "CLDRURLS.TOOLSURL"));
        l.add(Arguments.of(CLDRURLS.PRIORITY_SUMMARY_HELP_URL, "CLDRURLS.PRIORITY_SUMMARY_HELP_URL"));
        l.add(Arguments.of(SubtypeToURLMap.getDefaultUrl(), "SubtypeToURLMap.getDefaultURL()"));
        l.add(Arguments.of(UnicodeKnownIssues.UNICODE_JIRA_BROWSE, "UnicodeKnownIssues.UNICODE_JIRA_BROWSE"));
        l.add(Arguments.of(CheckQuotes.VALID_DELIMITER_URL, "CheckQuotes.VALID_DELIMITER_URL"));
        l.add(Arguments.of(Chart.GITHUB_ROOT + "common/main/root.xml", "Chart.GITHUB_ROOT" + "common/main/root.xml"));
        l.add(Arguments.of(Chart.LDML_SPEC, "Chart.LDML_SPEC"));
        l.add(Arguments.of(QueryClient.DEFAULT_CLDR_DBPEDIA_SPARQL_SERVER, "QueryClient.DEFAULT_CLDR_DBPEDIA_SPARQL_SERVER"));
        l.add(Arguments.of(QueryClient.DEFAULT_CLDR_WIKIDATA_SPARQL_SERVER, "DEFAULT_CLDR_WIKIDATA_SPARQL_SERVER"));

        return l.stream();
    }

    private static final Pattern URL_PATTERN = Pattern.compile("(https?)://[^ -]*[^ .-]"); // very simplistic

    /**
     * Split a String into URLs. Skips trailing period.
     * @param str input string
     * @return iterator over URLs
     */
    static Iterable<String> urlsFromString(final String str) {
        if (str == null || str.isEmpty()) {
            return Collections.emptySet();
        }
        Matcher matcher = URL_PATTERN.matcher(str);
        return matcher.results().map(MatchResult::group).collect(Collectors.toSet());
    }

    @Test
    /**
     * Test for urlsFromString
     */
    void TestUrlsFromString() {
        final Set<String> set = new HashSet<>();
        for (final String s : urlsFromString("Foo bar baz http://bad_example.com https://this.that.")) {
            set.add(s);
        }
        assertTrue(set.contains("https://this.that")); // trailing dot is not included
        assertTrue(set.contains("http://bad_example.com"));
        assertEquals(2, set.size());
    }
}
