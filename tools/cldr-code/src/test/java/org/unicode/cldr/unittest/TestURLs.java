package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRPaths;

public class TestURLs extends TestFmwk {

    private static final boolean DISABLE_BROKEN = true; // test for broken URL not working yet

    private static final Joiner JOIN_TAB = Joiner.on('\t');
    private static final Joiner JOIN_SP = Joiner.on(' ');
    private static final Joiner JOIN_LF = Joiner.on('\n');

    enum SiteType {
        SITE,
        SPEC,
        CHART,
        GITHUB
    }

    public static void main(String[] args) {
        new TestURLs().run(args);
    }

    public void testDetection() {
        String[][] tests = {
            {"## <a name=\"Parts\" href='#Parts'>Parts</a>", "#Parts"},
            {"## <a name=\"Parts\" href=\"#Parts\">Parts</a>", "#Parts"},
            {
                "*   Part 1: [Core](tr35.md#Contents) (languages, locales, basic structure)",
                "tr35.md#Contents"
            },
        };
        int lineCount = 0;
        for (String[] test : tests) {
            String line = test[0];
            String expected = test[1];
            Multimap<Integer, String> lineToUrls = TreeMultimap.create();
            getUrls(lineCount++ + line, 0, lineToUrls);
            Collection<String> result = lineToUrls.asMap().get(0);
            assertEquals(line, expected, result == null ? null : Joiner.on('◎').join(result));
        }
    }

    static final Path BASE = Path.of(CLDRPaths.BASE_DIRECTORY);

    public void testSiteFiles() {
        checkSiteFiles(SiteType.SITE, Path.of(CLDRPaths.BASE_DIRECTORY, "docs/site"));
    }

    public void testSpecFiles() {
        checkSiteFiles(SiteType.SPEC, Path.of(CLDRPaths.BASE_DIRECTORY, "docs/ldml"));
    }

    private void checkSiteFiles(SiteType siteType, Path directoryToScan) {
        Path directoryPath = directoryToScan;
        Set<LineChecker> results = new LinkedHashSet<>();
        try (Stream<Path> filepath = Files.walk(directoryPath)) {
            filepath.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .forEach(x -> checkFile(siteType, x, results));
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        System.out.println();
        assertResults(results);
        System.out.println("\nDomains\n" + JOIN_LF.join(domains));
    }

    public void testFile() {
        Path p = Path.of(CLDRPaths.BASE_DIRECTORY, "docs/site/development/adding-locales.md");
        Set<LineChecker> results = new LinkedHashSet<>();
        checkFile(SiteType.SITE, p, results);
        assertResults(results);
    }

    public void assertResults(Set<LineChecker> results) {
        results.stream()
                .forEach(
                        x -> {
                            for (Issue issue : Issue.values()) {
                                Collection<String> errorLines = x.getProblems(issue);
                                if (errorLines.isEmpty()) {
                                    continue;
                                }
                                if (Issue.ERRORS.contains(issue)) {
                                    if (!assertEquals("File errors", 0, errorLines.size())) {
                                        errorLines.stream().forEach(System.out::println);
                                    }
                                } else {
                                    errorLines.stream().forEach(System.out::println);
                                }
                            }
                        });
    }

    public void checkFile(SiteType siteType, Path p, Set<LineChecker> results) {
        // logln(p.toString());
        LineChecker lineChecker = new LineChecker(siteType, p);
        results.add(lineChecker);
        try {
            Files.lines(p)
                    .forEach(
                            line -> {
                                lineChecker.checkLine(line);
                            });
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public void testLineChecker() {
        String[][] tests = {
            {"SITE", "../index/downloads.md#cldr-releasesdownloads", "BAD_LOCAL HAS_MD"},
            {"SITE", "../index/downloads#cldr-releasesdownloads", "BAD_LOCAL"},
        };
        for (String[] test : tests) {
            SiteType siteType = SiteType.valueOf(test[0]);
            String line = test[1];
            String expected = test[2];
            LineChecker lineChecker = new LineChecker(siteType, Path.of("../foo"));
            Set<Issue> actual = lineChecker.getURLIssues(0, line);
            assertEquals(JOIN_TAB.join(siteType, line), expected, JOIN_SP.join(actual));
        }
    }

    public void testIssueExamples() {
        for (Issue issue : Issue.values()) {
            if (DISABLE_BROKEN & issue == Issue.BROKEN) {
                continue;
            }
            final SiteType siteType = SiteType.SITE; // expand later for other types
            LineChecker lineChecker = new LineChecker(siteType, Path.of("../foo"));
            Set<Issue> actual = lineChecker.getURLIssues(0, issue.example);
            assertEquals(
                    JOIN_TAB.join(siteType, issue.example),
                    issue.toString(),
                    JOIN_SP.join(Sets.intersection(Set.of(issue), actual)).toString());
        }
    }

    enum Issue {
        ΩK("", "https://unicode-org.atlassian.net/browse/CLDR-14927"),
        OLD_ANCHOR("Old anchor, needs replacement", "downloads/cldr-44#h.nvqx283jwsx"),
        BAD_LOCAL("Local can't refer outside of docs/site", "../ldml/tr35.md"),
        MD_FORBID(
                "Links within Site must not end with .md; but those within Spec must",
                "../index/downloads.md#cldr-releasesdownloads"),
        MD_REQUIRE(
                "Links within Site must not end with .md; but those within Spec must",
                "../index/downloads.md#cldr-releasesdownloads"),
        BAD_GITHUB(
                "Don't link to github for site or spec",
                "https://unicode-org.github.io/cldr-staging/charts/42/delta/bcp47.html"),
        BAD_GITHUB_DOCS(
                "Don't link into github for docs",
                "https://github.com/unicode-org/cldr/blob/main/docs/requesting_changes.md"),
        MAKE_RELATIVE("Change to relative", "https://cldr.unicode.org/index/cldr-spec"),
        OLD_SITE(
                "Don't link to old site",
                "https://sites.google.com/unicode.org/cldr/index/downloads/cldr-42#h.xtb1v8tpviuc"),
        OLDER_SITE(
                "Don't link to ancient site",
                "http://www.unicode.org/repos/cldr/trunk/common/bcp47/number.xml"),
        SEARCH(
                "Don't vector through google search",
                "http://www.google.com/search?q=Congo+site%3Alemonde.fr"),
        SMOKE(
                "Don't link to smoketest",
                "https://cldr-smoke.unicode.org/smoketest/v#/fr/Symbols2/47925556fd2904b5"),
        MALFORMED_URL("MalformedURLException", "http://example.com:-80/"),
        BROKEN("Can't access URL", "https://qreioqhfiorufpehbquhe.com");

        static final Set<Issue> ERRORS =
                Sets.difference(
                        ImmutableSet.copyOf(Issue.values()), Set.of(Issue.ΩK, Issue.OLD_ANCHOR));
        final String message;
        final String example;

        //        private Issue(String message) {
        //            this(message, "…");
        //        }
        private Issue(String message, String example) {
            this.message = message;
            this.example = example;
        }
    }

    static final Set<String> domains = new TreeSet<>();

    static final class LineChecker {
        int count = 0;
        private final Multimap<Integer, String> lineToUrls = TreeMultimap.create();
        private final SiteType siteType;
        private final String disallowedLocal;
        private final Path path;

        public LineChecker(SiteType siteType, Path path) {
            this.siteType = siteType;
            disallowedLocal = siteType == SiteType.SPEC ? "/cldr/docs/ldml/" : "/cldr/docs/site/";
            this.path = path;
        }

        private void checkLine(String line) {
            getUrls(line, ++count, lineToUrls);
        }

        public List<String> getProblems(Issue issue) {
            List<String> errorLines = new ArrayList<>();
            lineToUrls.forEach(
                    (x, y) -> {
                        Set<Issue> issues = getURLIssues(x, y);
                        issues.stream()
                                .filter(q -> q == issue)
                                .forEach(
                                        r -> {
                                            Path pathRelative = BASE.relativize(path);
                                            errorLines.add(
                                                    JOIN_TAB.join(issue, pathRelative, x, y));
                                        });
                    });
            return errorLines;
        }

        public Set<Issue> getURLIssues(Integer line, String urlString) {
            Set<Issue> issues = new LinkedHashSet<>();
            try {
                URL url;
                boolean isLocal;
                if (PROTOCOL_PAT.matcher(urlString).lookingAt()) {
                    url = new URL(urlString);
                    isLocal = false;
                } else {
                    url = new URL("file:" + Path.of(path.toString(), urlString).normalize());
                    isLocal = true;
                }
                String file = url.getFile();
                String domain = url.getAuthority();
                if (domain != null) {
                    domains.add(domain);
                }

                checkSite(urlString, url, file, domain, isLocal, issues);

                if (issues.isEmpty() && !isLocal & !doesURLExist(url)) { // not working yet
                    issues.add(Issue.BROKEN);
                }
            } catch (MalformedURLException e) {
                issues.add(Issue.MALFORMED_URL);
            }
            if (issues.isEmpty()) {
                issues.add(Issue.ΩK);
            }
            return issues;
        }

        public void checkSite(
                String urlString,
                URL url,
                String file,
                String domain,
                boolean isLocal,
                Set<Issue> issues) {
            if (isLocal) {
                if (!url.toString().contains(disallowedLocal)) {
                    issues.add(Issue.BAD_LOCAL);
                }
                final boolean endsWithMd = file.endsWith(".md") && urlString.contains(".md");
                // second check is because relativizing 'fills in' a file with .md

                if (siteType == SiteType.SPEC
                        && !endsWithMd
                        && !urlString.startsWith("#")
                        && !urlString.endsWith(".png")
                        && !urlString.endsWith(".abnf")) {
                    issues.add(Issue.MD_REQUIRE);
                }
                if (siteType == SiteType.SITE && endsWithMd) {
                    issues.add(Issue.MD_FORBID);
                    // relative links cannot end with .md
                }
            }
            if (url.getRef() != null //
                    && url.getRef().startsWith("h.")) {
                issues.add(Issue.OLD_ANCHOR);
            }
            if (file.contains("github.com/") && file.contains("/docs/")) {
                issues.add(Issue.BAD_GITHUB);
            }
            checkDomain(issues, file, domain);
        }

        public void checkDomain(Set<Issue> error, String file, String domain) {
            if (domain != null) {
                switch (domain) {
                    case "cldr-smoke.unicode.org":
                        error.add(Issue.SMOKE);
                        break;
                    case "sites.google.com":
                        error.add(Issue.OLD_SITE);
                        break;
                    case "cldr.unicode.org":
                        if (siteType == SiteType.SITE) {
                            error.add(Issue.MAKE_RELATIVE);
                        }
                    case "unicode.org":
                        if (file.startsWith("/cldr/data/")
                                || file.startsWith("unicode.org/cldr/repository_access")
                                || file.startsWith("/cldr/trac/")) {
                            error.add(Issue.OLDER_SITE);
                        }
                        break;
                    case "www.unicode.org":
                        if (file.startsWith("/reports/tr35/")) {
                            if (siteType == SiteType.SPEC) {
                                error.add(Issue.MAKE_RELATIVE);
                            }
                        } else if (file.startsWith("/repos")) {
                            error.add(Issue.OLDER_SITE);
                        }
                        break;
                    case "github.com":
                        if (file.contains("/docs/")) {
                            error.add(Issue.BAD_GITHUB_DOCS);
                        }
                        break;
                    case "unicode-org.github.io":
                        if (file.startsWith("/cldr-staging/")) {
                            error.add(Issue.BAD_GITHUB);
                        }
                        break;
                    case "www.google.com":
                        if (file.startsWith("/url?") || file.startsWith("/search?")) {
                            error.add(Issue.SEARCH);
                        }
                        break;
                }
            }
        }
    }

    public static Pattern PROTOCOL_PAT = Pattern.compile("[a-z0-9+.\\-]+:"); //

    public static Pattern OLD_ANCHOR = Pattern.compile(".*#h\\..*"); //

    public static Pattern URL_PAT =
            Pattern.compile(
                    "(?:\\bhref=(?:" //
                            + "[\"]([^\"]*)" //
                            + "|[']([^']*)))"
                            + "|\\]\\(([^)]*)"); //

    public static void getUrls(String line, int lineNumber, Multimap<Integer, String> lineToUrls) {
        Matcher m = URL_PAT.matcher(line);
        int count = 0;
        while (m.find()) {
            for (int i = 1; i <= m.groupCount(); ++i) {
                String group = m.group(i);
                if (group != null) {
                    lineToUrls.put(lineNumber, group);
                    ++count;
                    break;
                }
            }
        }
        // For debugging
        //        if (count == 0) {
        //            int start = line.indexOf("href");
        //            System.out.println(RegexUtilities.showMismatch(url, line.subSequence(start,
        // line.length())));
        //            int d=0;
        //        }
    }

    public static boolean doesURLExist(URL url) {
        if (DISABLE_BROKEN) return true; // the following doesn't work.
        try {
            // We want to check the current URL
            HttpURLConnection.setFollowRedirects(false);

            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            // We don't need to get data
            httpURLConnection.setRequestMethod("HEAD");

            // Some websites don't like programmatic access so pretend to be a browser
            httpURLConnection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
            int responseCode = httpURLConnection.getResponseCode();

            // We only accept response code 200
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }
}
