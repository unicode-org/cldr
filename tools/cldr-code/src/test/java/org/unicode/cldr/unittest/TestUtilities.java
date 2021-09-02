package org.unicode.cldr.unittest;
/*
 * TODO: rename this file and class to avoid confusion with org.unicode.cldr.util TestUtilities.java
 * When Eclipse console shows an error such as
 *    Error: (TestUtilities.java:1154) : 8 value: expected "old-value", got null
 * the link wrongly opens the wrong file named TestUtilities.java. The two files are:
 * cldr/tools/cldr-code/src/main/java/org/unicode/cldr/util/TestUtilities.java
 * cldr/tools/cldr-code/src/test/java/org/unicode/cldr/unittest/TestUtilities.java
 */

import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.tool.ConvertLanguageData.InverseComparator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DelegatingIterator;
import org.unicode.cldr.util.EscapingUtilities;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PluralSamples;
import org.unicode.cldr.util.SpecialLocales;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.Choice;
import org.unicode.cldr.util.VettingViewer.MissingStatus;
import org.unicode.cldr.util.VettingViewer.VoteStatus;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.VoteResolver.VoterInfo;
import org.unicode.cldr.util.XMLUploader;
import org.unicode.cldr.util.props.ICUPropertyFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestUtilities extends TestFmwkPlus {
    public static boolean DEBUG = true;

    private static final UnicodeSet DIGITS = new UnicodeSet("[0-9]");
    static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo
        .getSupplementalDataInfo();
    private static final int STRING_ID_TEST_COUNT = 1024 * 16;

    final int ONE_VETTER_BAR = Level.vetter.getVotes();
    final int TWO_VETTER_BAR = 2 * ONE_VETTER_BAR;

    public static void main(String[] args) {
        new TestUtilities().run(args);
    }

    public void TestPluralSamples() {
        checkPluralSamples("en");
        checkPluralSamples("cs");
        checkPluralSamples("ar");
    }

    private void checkPluralSamples(String locale) {
        PluralSamples pluralSamples = PluralSamples.getInstance(locale);
        Set<Count> counts = SUPPLEMENTAL_DATA_INFO.getPlurals(locale)
            .getCounts();
        for (int i = 1; i < 5; ++i) {
            Map<Count, Double> samplesForDigits = pluralSamples.getSamples(i);
            if (!counts.containsAll(samplesForDigits.keySet())) {
                errln(locale + ": mismatch in samples, expected " + counts
                    + ", got: " + samplesForDigits);
            } else if (samplesForDigits.size() == 0) {
                errln(locale + ": no sample for digit " + i);
            } else {
                logln(locale + " plural samples: " + samplesForDigits);
            }
        }
    }

    public static class StringIdException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public class StringIdThread extends Thread {
        final private Random r = new Random();
        final private int id;

        StringIdThread(int i) {
            super("Demo Thread");
            id = i;
        }

        @Override
        public void run() {
            logln("Starting thread: " + this);
            for (int i = 0; i < STRING_ID_TEST_COUNT; ++i) {
                String s = String.valueOf(r.nextInt());
                long l = StringId.getId(s);
                String s2 = StringId.getStringFromId(l);
                if (!s.equals(s2)) {
                    throw new StringIdException();
                }
            }
            logln("Ending thread: " + this);
        }

        @Override
        public String toString() {
            return "StringIdThread " + id;
        }
    }

    public void TestStringId() {
        ArrayList<StringIdThread> threads = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            StringIdThread thread = new StringIdThread(i);
            threads.add(thread);
            thread.start();
        }
        for (StringIdThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                errln(e.toString());
            }
        }
    }

    public void TestUrlEscape() {
        Matcher byte1 = PatternCache.get("%[A-Za-z0-9]{2}").matcher("");
        Matcher byte2 = PatternCache.get("%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}")
            .matcher("");
        Matcher byte3 = PatternCache.get(
            "%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}").matcher("");
        Matcher byte4 = PatternCache.get(
            "%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}")
            .matcher("");
        for (int i = 1; i <= 0x10FFFF; i = i * 3 / 2 + 1) {
            String escaped = EscapingUtilities.urlEscape(new StringBuilder()
                .appendCodePoint(i).toString());
            logln(Integer.toHexString(i) + " => " + escaped);
            if (EscapingUtilities.OK_TO_NOT_QUOTE.contains(i)) {
                assertTrue("Should be unquoted", escaped.length() == 1);
            } else if (i < 0x80) {
                assertTrue("Should be %xx", byte1.reset(escaped).matches());
            } else if (i < 0x800) {
                assertTrue("Should be %xx%xx", byte2.reset(escaped).matches());
            } else if (i < 0x10000) {
                assertTrue("Should be %xx%xx%xx", byte3.reset(escaped)
                    .matches());
            } else {
                assertTrue("Should be %xx%xx%xx%xx", byte4.reset(escaped)
                    .matches());
            }
        }
    }

    public void TestDelegatingIterator() {
        Set<String> s = new TreeSet<>(Arrays.asList(new String[] { "a",
            "b", "c" }));
        Set<String> t = new LinkedHashSet<>(Arrays.asList(new String[] {
            "f", "d", "e" }));
        StringBuilder result = new StringBuilder();

        for (String u : DelegatingIterator.iterable(s, t)) {
            result.append(u);
        }
        assertEquals("Iterator", "abcfde", result.toString());

        result.setLength(0);
        for (String u : DelegatingIterator.array("s", "t", "u")) {
            result.append(u);
        }
        assertEquals("Iterator", "stu", result.toString());

        int count = 0;
        result.setLength(0);
        for (int u : DelegatingIterator.array(1, 3, 5)) {
            count += u;
        }
        assertEquals("Iterator", 9, count);

        result.setLength(0);
        for (Object u : DelegatingIterator.array(1, "t", "u", new UnicodeSet(
            "[a-z]"))) {
            result.append(u);
        }
        assertEquals("Iterator", "1tu[a-z]", result.toString());
    }

    public void TestUntimedCounter() {
        // simulates how Counter is used in VettingViewer
        Counter<Choice> problemCounter = new Counter<>();
        problemCounter.increment(Choice.error);
        problemCounter.increment(Choice.error);
        problemCounter.increment(Choice.warning);

        assertEquals("problemCounter error", 2, problemCounter.get(Choice.error));
        assertEquals("problemCounter warning", 1, problemCounter.get(Choice.warning));
        assertEquals("problemCounter weLost", 0, problemCounter.get(Choice.weLost));

        Counter<Choice> otherCounter = new Counter<>();
        otherCounter.addAll(problemCounter);
        otherCounter.increment(Choice.error);

        assertEquals("otherCounter error", 3, otherCounter.get(Choice.error));
        assertEquals("otherCounter warning", 1, otherCounter.get(Choice.warning));
        assertEquals("otherCounter weLost", 0, otherCounter.get(Choice.weLost));
    }

    public void TestCounter() {
        Counter<String> counter = new Counter<>(true);
        Comparator<String> uca = new Comparator<String>() {
            Collator col = Collator.getInstance(ULocale.ENGLISH);

            @Override
            public int compare(String o1, String o2) {
                return col.compare(o1, o2);
            }
        };
        InverseComparator ucaDown = new InverseComparator(uca);

        counter.add("c", 95);
        counter.add("b", 50);
        counter.add("b", 101);
        counter.add("a", 100);
        counter.add("a", -5);
        counter.add("d", -3);
        assertEquals("getCount(b)", counter.getCount("b"), 151);
        assertEquals("getCount(a)", counter.getCount("a"), 95);
        assertEquals("getCount(a)", counter.getTotal(), 338);
        assertEquals("getItemCount", counter.getItemCount(), 4);

        assertEquals("getMap", "{a=95, b=151, c=95, d=-3}", counter.toString());

        assertEquals("getKeysetSortedByKey", Arrays.asList("a", "b", "c", "d"),
            new ArrayList<>(counter.getKeysetSortedByKey()));

        assertEquals(
            "getKeysetSortedByCount(true, ucaDown)",
            Arrays.asList("d", "c", "a", "b"),
            new ArrayList<String>(counter.getKeysetSortedByCount(true,
                ucaDown)));

        assertEquals("getKeysetSortedByCount(true, null), value",
            Arrays.asList("d", "a", "c", "b"), new ArrayList<>(
                counter.getKeysetSortedByCount(true, uca)));

        assertEquals("getKeysetSortedByCount(false, ucaDown), descending",
            Arrays.asList("b", "c", "a", "d"), new ArrayList<String>(
                counter.getKeysetSortedByCount(false, ucaDown)));

        assertEquals("getKeysetSortedByCount(false, null), descending, value",
            Arrays.asList("b", "a", "c", "d"), new ArrayList<>(
                counter.getKeysetSortedByCount(false, uca)));
    }

    public void TestOrganizationOrder() {
        Map<String, Organization> stringToOrg = new TreeMap<>();
        for (Organization org : Organization.values()) {
            stringToOrg.put(org.toString(), org);
        }
        List<Organization> reordered = new ArrayList<>(
            stringToOrg.values());
        List<Organization> plain = Arrays.asList(Organization.values());
        for (int i = 0; i < reordered.size(); ++i) {
            assertEquals("Items not in alphabetical order", reordered.get(i),
                plain.get(i));
        }
    }

    public void TestOrganizationNames() {
        UnicodeSet uppercase = new UnicodeSet("[:uppercase:]");
        for (Organization org : Organization.values()) {
            if (!uppercase.contains(org.getDisplayName().codePointAt(0))) {
                errln("Organization name isn't titlecased: " + org + ", "
                    + org.getDisplayName());
            }
            assertEquals("Organization from enum name", org,
                Organization.fromString(org.toString()));
            assertEquals("Organization from display name", org,
                Organization.fromString(org.getDisplayName()));
        }
    }

    static final boolean SHOW_DETAILS = CldrUtility.getProperty("showdetails",
        false);
    private static final CharSequence DEBUG_COMMENT = "set up a case of conflict within organization";

    static class PathValueInfo {
        private static Map<Integer, String> voteInfo;
        private CLDRFile file;

        public PathValueInfo(Factory factory, String locale) {
            this.file = factory.make(locale, false);
        }

        public String getRealValue(int id) {
            return file.getStringValue(getRealPath(id));
        }

        public String getRealPath(int id) {
            return voteInfo.get(id);
        }
    }

    /** Test user data. Restructured to be easier to read, more typesafe */
    enum TestUser {
        guestS(801, Organization.guest, Level.street),
        gnomeS(701, Organization.gnome, Level.street),
        gnomeV(702, Organization.gnome, Level.vetter),
        googleV(404, Organization.google, Level.vetter),
        googleS(411, Organization.google, Level.street),
        googleV2(424, Organization.google, Level.vetter),
        appleV(304, Organization.apple, Level.vetter),
        adobeE(204, Organization.adobe, Level.manager),
        adobeV(209, Organization.adobe, Level.vetter),
        ibmS(101, Organization.ibm, Level.street),
        ibmV(134, Organization.ibm, Level.vetter),
        ibmE(114, Organization.ibm, Level.manager),
        ibmT(129, Organization.ibm, Level.tc),
        guestS2(802, Organization.guest, Level.street);

        public static final Map<Integer, VoterInfo> TEST_USERS;
        public final Integer voterId;
        public final VoterInfo voterInfo;

        TestUser(int intVoterId, Organization organization, Level level) {
            voterId = intVoterId;
            voterInfo = new VoterInfo(organization, level, name());
        }

        static {
            ImmutableMap.Builder<Integer, VoterInfo> temp = ImmutableMap.builder();
            for (TestUser testUser : values()) {
                temp.put(testUser.voterId, testUser.voterInfo);
            }
            TEST_USERS = temp.build();
        }
    }

    public static final Map<Integer, VoterInfo> testdata = TestUser.TEST_USERS;

    private int toVoterId(String s) {
        return TestUser.valueOf(s).voterId;
    }

    public void TestTrunkStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();
        resolver.setLocale(CLDRLocale.getInstance("de"), null);

        resolver.setBaseline("new-item", Status.approved);
        assertEquals("", "new-item", resolver.getWinningValue());

        /*
         * Formerly last-release would win over trunk in a 2nd scenario here, due to
         * the difference in status. Now last-release plays no role, that test is obsolete.
         * Reference: https://unicode.org/cldr/trac/ticket/11916
         */
    }

    public void TestVoteResolverNgombaTrunkStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();
        resolver.setLocale(CLDRLocale.getInstance("jgo"), null);
        final String jgo22trunk = "\uA78C"; // "[a á â ǎ b c d ɛ {ɛ́} {ɛ̂} {ɛ̌} {ɛ̀} {ɛ̄} f ɡ h i í î ǐ j k l m ḿ {m̀} {m̄} n ń ǹ {n̄} ŋ {ŋ́} {ŋ̀} {ŋ̄} ɔ {ɔ́} {ɔ̂} {ɔ̌} p {pf} s {sh} t {ts} u ú û ǔ ʉ {ʉ́} {ʉ̂} {ʉ̌} {ʉ̈} v w ẅ y z ꞌ]";
        resolver.setBaseline(jgo22trunk, Status.approved); // seed/jgo.xml from 22
        // trunk
        logln("SVN: " + jgo22trunk);
        logln(resolver.toString());
        assertEquals("Winning Value", jgo22trunk, resolver.getWinningValue());
    }

    public void TestVoteStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();

        resolver.setLocale(CLDRLocale.getInstance("de"), null);
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", Status.approved);
        resolver.add("fii", toVoterId("adobeE"));
        resolver.add("fii", toVoterId("appleV"));
        VoteStatus voteStatus;
        voteStatus = resolver.getStatusForOrganization(Organization.google);
        assertEquals("", VoteStatus.ok, voteStatus);
        voteStatus = resolver.getStatusForOrganization(Organization.apple);
        assertEquals("", VoteStatus.ok, voteStatus);

        // make non-equal foo
        String s1 = "foo";
        String s2 = new StringBuilder("fo").append("o").toString();
        if (s1 == s2) {
            errln("Test problem");
        }
        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setBaseline(s1, Status.approved);
        resolver.add(s2, toVoterId("appleV"));
        voteStatus = resolver.getStatusForOrganization(Organization.apple);
        assertEquals("", VoteStatus.ok, voteStatus);
    }

    public void TestLosingStatus() {
        // af
        // losing? {baseline: {BQ, missing}, trunk: {null, null},
        // {orgToVotes: , totals: {}, conflicted: []},
        // sameVotes: [BQ], O: null, N: null, totals: {}, winning: {BQ,
        // missing}}
        // XPath: //ldml/localeDisplayNames/territories/territory[@type="BQ"]
        // gcvs.openoffice_org.example.com
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();

        resolver.setLocale(CLDRLocale.getInstance("af"), null);
        resolver.setBaseline("BQ", Status.missing);
        VoteStatus status = resolver
            .getStatusForOrganization(Organization.openoffice_org);
        assertEquals("", VoteStatus.provisionalOrWorse, status);

        // {lastRelease: {{0}: {1}, missing}, trunk: {null, null}, {orgToVotes:
        // pakistan={{0}: {1}=8}, totals: {{0}:
        // {1}=8}, conflicted: []}, sameVotes: [{0}: {1}], O: {0}: {1}, N: null,
        // totals: {{0}: {1}=8}, winning: {{0}:
        // {1}, approved}}
        resolver.clear();
        resolver.setBaileyValue("bailey");
        // resolver.setLastRelease("{0}: {1}", Status.missing);
        resolver.add("{0}: {1}", toVoterId("adobeE"));
        status = resolver.getStatusForOrganization(Organization.openoffice_org);
        assertEquals("", VoteStatus.ok, status);

        // {lastRelease: {Arabisch, approved}, trunk: {Arabisch, approved},
        // {orgToVotes: , totals: {}, conflicted: []},
        // sameVotes: [Arabisch], O: null, N: null, totals: {}, winning:
        // {Arabisch, approved}}
        resolver.clear();
        resolver.setBaileyValue("bailey");
        // resolver.setLastRelease("Arabisch", Status.approved);
        resolver.setBaseline("Arabisch", Status.approved);
        status = resolver.getStatusForOrganization(Organization.openoffice_org);
        assertEquals("", VoteStatus.ok_novotes, status);
    }

    public void TestTotalVotesStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();

        Status oldStatus = Status.unconfirmed;

        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("de"), null);
        resolver.setBaseline("foo", oldStatus);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("apple", toVoterId("appleV"));

        // check that alphabetical wins when votes are equal
        String winner = resolver.getWinningValue();
        Status winningStatus = resolver.getWinningStatus();
        assertEquals("", "apple", winner);
        assertEquals("", Status.provisional, winningStatus);

        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("de"), null);
        resolver.setBaseline("foo", oldStatus);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("zebra", toVoterId("googleS"));
        resolver.add("apple", toVoterId("appleV"));

        // check that total votes over alphabetical
        winner = resolver.getWinningValue();
        winningStatus = resolver.getWinningStatus();
        assertEquals("", "zebra", winner);
        assertEquals("", Status.provisional, winningStatus);
    }

    public void TestVoteDowngrade() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();

        Status oldStatus = Status.unconfirmed;

        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("mt"), null);
        resolver.setBaseline("foo", oldStatus);
        resolver.add("aardvark", toVoterId("adobeE"));
        resolver.add("zebra", toVoterId("ibmT"));
        assertEquals("", "zebra", resolver.getWinningValue()); // TC vote of 20
        // beats
        // expert's 8
        assertEquals("", Status.approved, resolver.getWinningStatus());

        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("mt"), null);
        resolver.setBaseline("foo", oldStatus);
        resolver.add("aardvark", toVoterId("adobeE"));
        resolver.add("zebra", toVoterId("ibmT"));
        resolver.add("aardvark", toVoterId("ibmE"));
        assertEquals("", "zebra", resolver.getWinningValue()); // TC vote of 20
        // beats
        // manager's 4
        // and its own
        // manager's 4
        assertEquals("", Status.approved, resolver.getWinningStatus());

        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("mt"), null);
        resolver.setBaseline("foo", oldStatus);
        resolver.add("aardvark", toVoterId("adobeE"));
        resolver.add("zebra", toVoterId("ibmT"), Level.vetter.getVotes()); // NOTE:
        // reduced
        // votes:
        // as
        // vetter.
        resolver.add("aardvark", toVoterId("ibmE"));
        assertEquals("", "aardvark", resolver.getWinningValue()); // Now
        // aardvark
        // wins -
        // managers
        // win out as provisional
        assertEquals("", Status.provisional, resolver.getWinningStatus());

        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("mt"), null);
        resolver.setBaseline("foo", oldStatus);
        resolver.add("aardvark", toVoterId("adobeE"));
        resolver.add("zebra", toVoterId("ibmT"), Level.vetter.getVotes()); // NOTE:
        // reduced
        // votes:
        // as
        // vetter.
        assertEquals("", "aardvark", resolver.getWinningValue()); // Now
        // aardvark
        // wins -
        // managers
        // win out.
        assertEquals("", Status.provisional, resolver.getWinningStatus());
    }

    public void TestResolvedVoteCounts() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();

        Status oldStatus = Status.unconfirmed;

        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("de"), null);
        resolver.setBaseline("foo", oldStatus);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("apple", toVoterId("appleV"));

        // check that alphabetical wins when votes are equal
        Map<String, Long> counts = resolver.getResolvedVoteCounts();
        logln(counts.toString());
        assertEquals("", "foo", new ArrayList<>(counts.keySet()).get(2));

        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("de"), null);
        resolver.setBaseline("foo", Status.approved);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("apple", toVoterId("appleV"));
        counts = resolver.getResolvedVoteCounts();
        logln(counts.toString());
        assertEquals("", "foo", new ArrayList<>(counts.keySet()).get(0));

        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setLocale(CLDRLocale.getInstance("de"), null);
        resolver.setBaseline("foo", Status.approved);
        resolver.add("zebra", toVoterId("googleS"));
        counts = resolver.getResolvedVoteCounts();
        logln(counts.toString());
        assertEquals("", "foo", new ArrayList<>(counts.keySet()).get(0));
    }

    private void verifyRequiredVotes(VoteResolver<String> resolver, String locale,
        String xpath, Status baselineStatus, int required) {
        StringBuilder sb = new StringBuilder();
        sb.append("Locale: " + locale);
        resolver.clear();
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", baselineStatus);
        PathHeader ph = null;
        if (xpath != null) {
            sb.append(" XPath: " + xpath);
            ph = PathHeader.getFactory(testInfo.getEnglish())
                .fromPath(xpath);
        }
        resolver.setLocale(CLDRLocale.getInstance(locale), ph);
        assertEquals(ph.toString(), required, resolver.getRequiredVotes());
    }

    public void TestRequiredVotes() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();
        verifyRequiredVotes(resolver, "mt",
            "//ldml/localeDisplayNames/languages/language[@type=\"fr_CA\"]",
            Status.missing, ONE_VETTER_BAR);
        verifyRequiredVotes(resolver, "fr",
            "//ldml/localeDisplayNames/languages/language[@type=\"fr_CA\"]",
            Status.provisional, TWO_VETTER_BAR);
        verifyRequiredVotes(resolver, "es",
            "//ldml/numbers/symbols[@numberSystem=\"latn\"]/group",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "es",
            "//ldml/numbers/symbols[@numberSystem=\"latn\"]/decimal",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "hi",
            "//ldml/numbers/symbols[@numberSystem=\"deva\"]/decimal",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "hi",
            "//ldml/numbers/symbols[@numberSystem=\"deva\"]/group",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "ast",
            "//ldml/numbers/symbols[@numberSystem=\"latn\"]/decimal",
            Status.approved, ONE_VETTER_BAR);
        verifyRequiredVotes(resolver, "mt",
            "//ldml/characters/exemplarCharacters",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "mt",
            "//ldml/characters/exemplarCharacters",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "mt",
            "//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "mt",
            "//ldml/characters/exemplarCharacters[@type=\"numbers\"]",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "mt",
            "//ldml/characters/exemplarCharacters[@type=\"punctuation\"]",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "mt",
            "//ldml/characters/exemplarCharacters[@type=\"index\"]",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "es",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sun\"]",
            Status.approved, VoteResolver.HIGH_BAR);
        verifyRequiredVotes(resolver, "ast",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sun\"]",
            Status.approved, ONE_VETTER_BAR);
        verifyRequiredVotes(resolver, "es",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]",
            Status.provisional, VoteResolver.LOWER_BAR);
        verifyRequiredVotes(resolver, "ast",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]",
            Status.approved, ONE_VETTER_BAR);
    }

    /**
     * In sublocales, for a typical path, the required votes should be 4, except for
     * the two locales pt_PT and zh_Hant
     */
    public void TestSublocaleRequiredVotes() {
        final Set<String> eightVoteSublocales = new HashSet<>(Arrays.asList("pt_PT", "zh_Hant"));
        final VoteResolver<String> resolver = new VoteResolver<>();
        final String path = "//ldml/annotations/annotation[@cp=\"🌏\"][@type=\"tts\"]";
        for (String locale : SubmissionLocales.CLDR_LOCALES) {
            if (locale.contains("_")) {
                int expectedRequiredVotes = eightVoteSublocales.contains(locale) ? TWO_VETTER_BAR : ONE_VETTER_BAR;
                verifyRequiredVotes(resolver, locale, path, Status.approved, expectedRequiredVotes);
            }
        }
    }

    public void TestVoteResolver() {
        // to make it easier to debug failures, the first digit is an org,
        // second is the individual in that org, and
        // third is the voting weight.
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();
        String[] tests = {
            "bailey=BAILEY",
            "comment=regression case from John Emmons",
            "locale=wae",
            "oldValue=2802",
            "oldStatus=approved",
            "304=208027", // Apple vetter
            // expected values
            "value=208027",
            "status=approved",
            "sameVotes=208027",
            "conflicts=[]",
            "check",
            // first test
            "oldValue=old-value",
            "oldStatus=provisional",
            "comment=Check that identical values get the top overall vote, and that org is maxed (eg vetter + street = vetter)",
            "404=next",
            "411=next",
            "304=best",
            // expected values
            "value=next",
            "sameVotes=next,best",
            "conflicts=[]",
            "status=provisional",
            "check",

            "comment=now give next a slight edge (5 to 4) with a different organization",
            "404=next",
            "304=best",
            "801=next",
            // expected values
            "value=next",
            "sameVotes=next",
            "status=approved",
            "check",

            "comment=set up a case of conflict within organization",
            "404=next",
            "424=best",
            // expected values
            "value=best", // alphabetical
            "sameVotes=best",
            "conflicts=[google]",
            "status=approved",
            "check",

            "comment=now cross-organizational conflict, also check for max value in same organization (4, 1) => 4 not 5",
            "404=next",
            "424=best",
            "411=best",
            "304=best",
            // expected values
            "conflicts=[google]",
            "value=best",
            "sameVotes=best",
            "status=approved",
            "check",

            "comment=now clear winner 8 over 4",
            "404=next",
            // "424=best",
            "411=best",
            "304=best",
            "204=primo",
            "114=primo",
            // expected values
            "conflicts=[]",
            "value=primo",
            "sameVotes=primo",
            "status=approved",
            "check",

            "comment=now not so clear, throw in a street value. So it is 8 to 5. (used to be provisional)",
            "404=next",
            // "424=best",
            "411=best",
            "304=best",
            "204=primo",
            "114=primo",
            "101=best",
            // expected values
            "conflicts=[]",
            "value=primo",
            "status=approved",
            "check",

            "comment=set up vote of 4 in established locale, with old provisional value",
            "locale=fr",
            "404=best",
            "oldStatus=provisional",
            // expected values
            "value=best",
            "sameVotes=best",
            "status=contributed",
            "conflicts=[]",
            "check",

            "comment=now set up vote of 4 in established locale, but with old contributed value",
            "oldStatus=contributed",
            // expected values
            "value=old-value",
            "sameVotes=old-value",
            "status=contributed",
            "conflicts=[]",
            "check",

            "comment=now set up vote of 1 + 1 in established locale, and with old contributed value",
            "411=best", "101=best", "oldStatus=contributed",
            // expected values
            "value=best", "sameVotes=best", "status=contributed",
            "conflicts=[]", "check", };
        String expectedValue = null;
        String expectedConflicts = null;
        Status expectedStatus = null;
        String oldValue = null;
        Status oldStatus = null;
        String baileyValue = null;
        List<String> sameVotes = null;
        String locale = null;
        Map<Integer, String> values = new TreeMap<>();
        int counter = -1;

        for (String test : tests) {
            String[] item = test.split("=");
            String name = item[0];
            String value = item.length < 2 ? null : item[1];
            if (name.equalsIgnoreCase("comment")) {
                logln("#\t" + value);
                //System.out.println("#\t" + value);
                if (DEBUG_COMMENT != null && value.contains(DEBUG_COMMENT)) {
                    int x = 0;
                }
            } else if (name.equalsIgnoreCase("locale")) {
                locale = value;
            } else if (name.equalsIgnoreCase("bailey")) {
                baileyValue = value;
            } else if (name.equalsIgnoreCase("oldValue")) {
                oldValue = value;
            } else if (name.equalsIgnoreCase("oldStatus")) {
                oldStatus = Status.valueOf(value);
            } else if (name.equalsIgnoreCase("value")) {
                expectedValue = value;
            } else if (name.equalsIgnoreCase("sameVotes")) {
                sameVotes = value == null ? new ArrayList<>(0) : Arrays
                    .asList(value.split(",\\s*"));
            } else if (name.equalsIgnoreCase("status")) {
                expectedStatus = Status.valueOf(value);
            } else if (name.equalsIgnoreCase("conflicts")) {
                expectedConflicts = value;
            } else if (DIGITS.containsAll(name)) {
                final int voter = Integer.parseInt(name);
                if (value == null || value.equals("null")) {
                    values.remove(voter);
                } else {
                    values.put(voter, value);
                }
            } else if (name.equalsIgnoreCase("check")) {
                counter++;
                // load the resolver
                resolver.setBaileyValue(baileyValue);
                resolver.setLocale(CLDRLocale.getInstance(locale), null);
                resolver.setBaseline(oldValue, oldStatus);
                for (int voter : values.keySet()) {
                    resolver.add(values.get(voter), voter);
                }
                // print the contents
                logln(counter + "\t" + values);
                logln(resolver.toString());
                // now print the values
                assertEquals(counter + " value", expectedValue,
                    resolver.getWinningValue());
                assertEquals(counter + " sameVotes", sameVotes.toString(),
                    resolver.getValuesWithSameVotes().toString());
                assertEquals(counter + " status", expectedStatus,
                    resolver.getWinningStatus());
                assertEquals(counter + " conflicts", expectedConflicts,
                    resolver.getConflictedOrganizations().toString());
                resolver.clear();
                resolver.setBaileyValue("bailey");
                values.clear();
            } else {
                errln("unknown command:\t" + test);
            }
        }
    }

    void assertSpecialLocale(String loc, SpecialLocales.Type type) {
        assertEquals("SpecialLocales.txt for " + loc, type,
            SpecialLocales.getType(CLDRLocale.getInstance(loc)));
    }

    public void TestSpecialLocales() {
        assertSpecialLocale("sr", null);
        assertSpecialLocale("ha_NE", SpecialLocales.Type.readonly);
        assertSpecialLocale("sr_Latn", SpecialLocales.Type.readonly);
        assertSpecialLocale("sr_Latn_BA", SpecialLocales.Type.readonly);
        assertSpecialLocale("yue_Hans", null); // not readonly, because it is not policy DISCARD
        assertSpecialLocale("en", SpecialLocales.Type.readonly);
        assertSpecialLocale("en_ZZ", SpecialLocales.Type.readonly);
        assertSpecialLocale("en_ZZ_PROGRAMMERESE", null); // not defined
        assertSpecialLocale("und", null);
        assertSpecialLocale("mul", SpecialLocales.Type.scratch);
        assertSpecialLocale("mul_ZZ", SpecialLocales.Type.scratch);
        assertSpecialLocale("und_001", null); // not defined

        CLDRLocale sr_Latn = CLDRLocale.getInstance("sr_Latn");
        CLDRLocale sr_Latn_BA = CLDRLocale.getInstance("sr_Latn_BA");
        logln("sr_Latn raw comment = " + SpecialLocales.getCommentRaw(sr_Latn));
        assertTrue("sr_Latn raw contains @ sign",
            SpecialLocales.getCommentRaw(sr_Latn).contains("@"));

        logln("sr_Latn comment = " + SpecialLocales.getComment(sr_Latn));
        assertTrue("sr_Latn comment does NOT contain @ sign", !SpecialLocales
            .getComment(sr_Latn).contains("@"));
        logln("sr_Latn_BA raw comment = "
            + SpecialLocales.getCommentRaw(sr_Latn_BA));
        assertTrue("sr_Latn_BA raw contains '@sr_Latn_BA'", SpecialLocales
            .getCommentRaw(sr_Latn_BA).contains("@sr_Latn_BA"));

    }

    public void TestCLDRURLS() {
        final String KOREAN_LANGUAGE = "//ldml/localeDisplayNames/languages/language[@type=\"ko\"]";
        final String KOREAN_LANGUAGE_STRID = "821c2a2fc5c206d";
        final CLDRLocale maltese = CLDRLocale.getInstance("mt");
        assertEquals("base", "https://st.unicode.org/cldr-apps", CLDRConfig
            .getInstance().urls().base());
        assertEquals(
            "locales list",
            "https://st.unicode.org/cldr-apps/v#locales///",
            CLDRConfig.getInstance().urls()
            .forSpecial(CLDRURLS.Special.Locales));
        assertEquals("maltese", "https://st.unicode.org/cldr-apps/v#/mt//",
            CLDRConfig.getInstance().urls().forLocale(maltese));
        assertEquals("korean in maltese",
            "https://st.unicode.org/cldr-apps/v#/mt//"
                + KOREAN_LANGUAGE_STRID,
                CLDRConfig.getInstance()
                .urls().forXpath(maltese, KOREAN_LANGUAGE));
        assertEquals("korean in maltese via stringid",
            "https://st.unicode.org/cldr-apps/v#/mt//"
                + KOREAN_LANGUAGE_STRID,
                CLDRConfig.getInstance()
                .urls().forXpathHexId(maltese, KOREAN_LANGUAGE_STRID));
        assertEquals("south east asia in maltese",
            "https://st.unicode.org/cldr-apps/v#/mt/C_SEAsia/", CLDRConfig
            .getInstance().urls().forPage(maltese, PageId.C_SEAsia));
        try {
            String ret = CLDRConfig.getInstance().urls()
                .forXpathHexId(maltese, KOREAN_LANGUAGE);
            errln("Error- expected forXpathHexId to choke on an xpath but got "
                + ret);
        } catch (IllegalArgumentException iae) {
            logln("GOOD: forXpathHexId Caught expected " + iae);
        }
        try {
            String ret = CLDRConfig.getInstance().urls()
                .forXpath(maltese, KOREAN_LANGUAGE_STRID);
            errln("Error- expected forXpath to choke on a hexid but got "
                + ret);
        } catch (IllegalArgumentException iae) {
            logln("GOOD: forXpath Caught expected " + iae);
        }

        assertEquals("korean in maltese - absoluteUrl",
            "https://st.unicode.org/cldr-apps/v#/mt//"
                + KOREAN_LANGUAGE_STRID,
                CLDRConfig.getInstance()
                .absoluteUrls().forXpath(maltese, KOREAN_LANGUAGE));

    }

    static final UnicodeMap<String> SCRIPTS = ICUPropertyFactory.make().getProperty("script").getUnicodeMap_internal();
    static final UnicodeMap<String> GC = ICUPropertyFactory.make().getProperty("general_category").getUnicodeMap_internal();

    public void TestUnicodeMapCompose() {
        logln("Getting Scripts");

        UnicodeMap.Composer<String> composer = new UnicodeMap.Composer<String>() {
            @Override
            public String compose(int codepoint, String string, String a, String b) {
                return a.toString() + "_" + b.toString();
            }
        };

        logln("Trying Compose");

        UnicodeMap<String> composed = ((UnicodeMap) SCRIPTS.cloneAsThawed()).composeWith(GC, composer);
        String last = "";
        for (int i = 0; i < 0x10FFFF; ++i) {
            String comp = composed.getValue(i);
            String gc = GC.getValue(i);
            String sc = SCRIPTS.getValue(i);
            if (!comp.equals(composer.compose(i, null, sc, gc))) {
                errln("Failed compose at: " + i);
                break;
            }
            if (!last.equals(comp)) {
                logln(Utility.hex(i) + "\t" + comp);
                last = comp;
            }
        }
    }

    private static final int SET_LIMIT = 0x10FFFF;
    private static final int CHECK_LIMIT = 0xFFFF;
    private static final NumberFormat pf = NumberFormat.getPercentInstance();
    private static final NumberFormat nf = NumberFormat.getInstance();

    public void TestUnicodeMapTime() {
        boolean shortTest = getInclusion() < 10;
        double hashTime, umTime, icuTime, treeTime;
        int warmup = shortTest ? 1 : 20;
        umTime = checkUnicodeMapSetTime(warmup, 0);
        hashTime = checkUnicodeMapSetTime(warmup, 1);
        logln("Percentage: " + pf.format(hashTime / umTime));
        treeTime = checkUnicodeMapSetTime(warmup, 3);
        logln("Percentage: " + pf.format(treeTime / umTime));

        if (shortTest) {
            return;
        }

        umTime = checkUnicodeMapGetTime(1000, 0);
        hashTime = checkUnicodeMapGetTime(1000, 1);
        logln("Percentage: " + pf.format(hashTime / umTime));
        icuTime = checkUnicodeMapGetTime(1000, 2);
        logln("Percentage: " + pf.format(icuTime / umTime));
        treeTime = checkUnicodeMapGetTime(1000, 3);
        logln("Percentage: " + pf.format(treeTime / umTime));
    }

    private static final int propEnum = UProperty.GENERAL_CATEGORY;

    private double checkUnicodeMapSetTime(int iterations, int type) {
        _checkUnicodeMapSetTime(1, type);
        double result = _checkUnicodeMapSetTime(iterations, type);
        logln((type == 0 ? "UnicodeMap" : type == 1 ? "HashMap" : type == 2 ? "ICU" : "TreeMap") + "\t" + nf.format(result));
        return result;
    }

    private double _checkUnicodeMapSetTime(int iterations, int type) {
        UnicodeMap<String> map1 = SCRIPTS;
        Map<Integer, String> map2 = map1.putAllCodepointsInto(new HashMap<Integer, String>());
        Map<Integer, String> map3 = new TreeMap<>(map2);
        System.gc();
        double start = System.currentTimeMillis();
        for (int j = 0; j < iterations; ++j)
            for (int cp = 0; cp <= SET_LIMIT; ++cp) {
                int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
                if (enumValue <= 0) continue; // for smaller set
                String value = UCharacter.getPropertyValueName(propEnum, enumValue, UProperty.NameChoice.LONG);
                switch (type) {
                case 0:
                    map1.put(cp, value);
                    break;
                case 1:
                    map2.put(cp, value);
                    break;
                case 3:
                    map3.put(cp, value);
                    break;
                }
            }
        double end = System.currentTimeMillis();
        return (end - start) / 1000 / iterations;
    }

    private double checkUnicodeMapGetTime(int iterations, int type) {
        UnicodeMap<String> map1 = new UnicodeMap<>();
        Map<Integer, String> map2 = map1.putAllCodepointsInto(new HashMap<Integer, String>());
        Map<Integer, String> map3 = new TreeMap<>();
        _checkUnicodeMapGetTime(map1, map2, map3, 1, type); // warmup
        double result = _checkUnicodeMapGetTime(map1, map2, map3, iterations, type);
        logln((type == 0 ? "UnicodeMap" : type == 1 ? "HashMap" : type == 2 ? "ICU" : "TreeMap") + "\t" + nf.format(result));
        return result;
    }

    private double _checkUnicodeMapGetTime(UnicodeMap<String> map1, Map<Integer, String> map2, Map<Integer, String> map3, int iterations, int type) {
        System.gc();
        double start = System.currentTimeMillis();
        for (int j = 0; j < iterations; ++j)
            for (int cp = 0; cp < CHECK_LIMIT; ++cp) {
                switch (type) {
                case 0:
                    map1.getValue(cp);
                    break;
                case 1:
                    map2.get(cp);
                    break;
                case 2:
                    int enumValue = UCharacter.getIntPropertyValue(cp, propEnum);
                    UCharacter.getPropertyValueName(propEnum, enumValue, UProperty.NameChoice.LONG);
                    break;
                case 3:
                    map3.get(cp);
                    break;
                }
            }
        double end = System.currentTimeMillis();
        return (end - start) / 1000 / iterations;
    }

    public void TestStevenTest() {

        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<>();

        String tests[] = {
            "bailey=BAILEY",
            "comment=Steven Loomis test case tweaked by Parthinator",
            "locale=wae",
            "oldValue=_",
            "oldStatus=approved",
            "304=test", // Apple vetter
            // expected values
            "value=test",
            "status=approved",
            "sameVotes=test",
            "conflicts=[]",
            "check",

            //test1
            "comment=timestamp case1",
            "locale=de",
            "oldValue=old-value",
            "oldStatus=provisional",
            "404=Foo",
            "424=Bar",
            //expected
            "value=Bar",
            "status=provisional",
            "sameVotes=Bar, test",
            "conflicts=[google]",
            "check",

            //test2
            "comment=timestamp case2",
            "locale=de",
            "oldValue=Bar",
            "oldStatus=provisional",
            "424=Foo",
            "404=Bar",
            // expected values
            "value=Bar",
            "status=provisional",
            "sameVotes=Bar, test",
            "conflicts=[google]",
            "check",

            //test 3
            "comment=timestamp guest case",
            "locale=de",
            "oldValue=_",
            "oldStatus=unconfirmed",
            //# // G vetter A
            //timestamp=1
            "801=Foo",
            //timestamp=2
            "802=Bar",
            // expected values
            "value=Bar",
            "status=contributed",
            "sameVotes=Bar",
            "conflicts=[google, guest]",
            "check",
        };

        String expectedValue = null;
        String expectedConflicts = null;
        Status expectedStatus = null;
        String oldValue = null;
        Status oldStatus = null;
        String baileyValue = null;
        List<String> sameVotes = null;
        String locale = null;
        int voteEntries = 0;
        Map<Integer, String> values = new TreeMap<>();
        Map<Integer, VoteEntries> valuesMap = new TreeMap<>();

        int counter = -1;

        for (String test : tests) {
            String[] item = test.split("=");
            String name = item[0];
            String value = item.length < 2 ? null : item[1];
            if (name.equalsIgnoreCase("comment")) {
                logln("#\t" + value);
                //System.out.println("#\t" + value);
                if (DEBUG_COMMENT != null && value.contains(DEBUG_COMMENT)) {
                    int x = 0;
                }
            } else if (name.equalsIgnoreCase("locale")) {
                locale = value;
            } else if (name.equalsIgnoreCase("oldValue")) {
                oldValue = value;
            } else if (name.equalsIgnoreCase("oldStatus")) {
                oldStatus = Status.valueOf(value);
            } else if (name.equalsIgnoreCase("value")) {
                expectedValue = value;
            } else if (name.equalsIgnoreCase("bailey")) {
                baileyValue = value;
            } else if (name.equalsIgnoreCase("sameVotes")) {
                sameVotes = value == null ? new ArrayList<>(0) : Arrays
                    .asList(value.split(",\\s*"));
            } else if (name.equalsIgnoreCase("status")) {
                expectedStatus = Status.valueOf(value);
            } else if (name.equalsIgnoreCase("conflicts")) {
                expectedConflicts = value;
            } else if (DIGITS.containsAll(name)) {
                final int voter = Integer.parseInt(name);
                if (value == null || value.equals("null")) {
                    values.remove(voter);
                    for (Map.Entry<Integer, VoteEntries> entry : valuesMap.entrySet()) {
                        if (entry.getValue().getVoter() == voter) {
                            valuesMap.remove(entry.getKey());
                        }
                    }
                } else {
                    values.put(voter, value);
                    valuesMap.put(++voteEntries, new VoteEntries(voter, value));
                }
            } else if (name.equalsIgnoreCase("check")) {
                counter++;
                // load the resolver
                resolver.setBaileyValue(baileyValue);
                resolver.setLocale(CLDRLocale.getInstance(locale), null);
                resolver.setBaseline(oldValue, oldStatus);
                for (int voteEntry : valuesMap.keySet()) {

                    resolver.add(valuesMap.get(voteEntry).getValue(), valuesMap.get(voteEntry).getVoter());
                }
                // print the contents
                logln(counter + "\t" + values);
                logln(resolver.toString());
                // now print the values
                assertEquals(counter + " value", expectedValue,
                    resolver.getWinningValue());
                assertEquals(counter + " sameVotes", sameVotes.toString(),
                    resolver.getValuesWithSameVotes().toString());
                assertEquals(counter + " status", expectedStatus,
                    resolver.getWinningStatus());
                assertEquals(counter + " conflicts", expectedConflicts,
                    resolver.getConflictedOrganizations().toString());
                resolver.clear();
                values.clear();
            } else {
                errln("unknown command:\t" + test);
            }
        }
    }

    public void testBaileyVotes() {
        VoteResolver.setVoterToInfo(TestUser.TEST_USERS);
        VoteResolver<String> resolver = new VoteResolver<>();
        CLDRLocale locale = CLDRLocale.getInstance("de");
        PathHeader path = null;

        /*
         * Simple case, all = bailey
         */
        resolver.setLocale(locale, path);
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", Status.approved);

        resolver.add("bailey", TestUser.appleV.voterId);
        resolver.add("bailey", TestUser.ibmV.voterId);
        resolver.add("bailey", TestUser.googleV.voterId);
        assertEquals("Simple case, all = bailey", "bailey", resolver.getWinningValue());

        /*
         * Another simple case, all = INHERITANCE_MARKER
         * Added per https://unicode.org/cldr/trac/ticket/11299
         */
        resolver.clear();
        resolver.setLocale(locale, path);
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", Status.approved);

        resolver.add(CldrUtility.INHERITANCE_MARKER, TestUser.appleV.voterId);
        resolver.add(CldrUtility.INHERITANCE_MARKER, TestUser.ibmV.voterId);
        resolver.add(CldrUtility.INHERITANCE_MARKER, TestUser.googleV.voterId);
        assertEquals("Another simple case, all = INHERITANCE_MARKER", CldrUtility.INHERITANCE_MARKER, resolver.getWinningValue());

        /*
         * INHERITANCE_MARKER should win here, having more votes than bailey.
         * Changed per https://unicode.org/cldr/trac/ticket/11299
         */
        resolver.clear();
        resolver.setLocale(locale, path);
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", Status.approved);

        resolver.add("bailey", TestUser.appleV.voterId);
        resolver.add(CldrUtility.INHERITANCE_MARKER, TestUser.ibmV.voterId);
        resolver.add(CldrUtility.INHERITANCE_MARKER, TestUser.googleV.voterId);
        assertEquals("The bailey value and explicit value combine to win", CldrUtility.INHERITANCE_MARKER, resolver.getWinningValue());

        /*
         * INHERITANCE_MARKER should win here, having equal number of votes with bailey;
         * first they combine to win over other-vote.
         * Changed per https://unicode.org/cldr/trac/ticket/11299
         */
        resolver.clear();
        resolver.setLocale(locale, path);
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", Status.approved);

        resolver.add("bailey", TestUser.appleV.voterId);
        resolver.add(CldrUtility.INHERITANCE_MARKER, TestUser.ibmV.voterId);
        resolver.add("other-vote", TestUser.googleV.voterId);
        assertEquals("The bailey value and explicit value combine to win again", CldrUtility.INHERITANCE_MARKER, resolver.getWinningValue());

        /*
         * Split vote, no action
         */
        resolver.clear();
        resolver.setLocale(locale, path);
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", Status.approved);

        resolver.add("bailey", TestUser.appleV.voterId);
        resolver.add("not-bailey", TestUser.ibmV.voterId);
        resolver.add("other-vote", TestUser.googleV.voterId);
        assertEquals("Split vote, no action", "foo", resolver.getWinningValue());

        /*
         * Bailey should win if it has MORE votes than INHERITANCE_MARKER, helped
         * by the presence of INHERITANCE_MARKER to win over other-vote.
         * Changed per https://unicode.org/cldr/trac/ticket/11299
         * Previously, "the only case where CldrUtility.INHERITANCE_MARKER wins is where they all are";
         * now we already have several tests above where INHERITANCE_MARKER wins.
         */
        resolver.clear();
        resolver.setLocale(locale, path);
        resolver.setBaileyValue("bailey");
        resolver.setBaseline("foo", Status.approved);

        resolver.add("bailey", TestUser.googleV.voterId);
        resolver.add("bailey", TestUser.appleV.voterId);
        resolver.add(CldrUtility.INHERITANCE_MARKER, TestUser.ibmV.voterId);
        resolver.add("other-vote", TestUser.adobeV.voterId);
        resolver.add("other-vote", TestUser.gnomeV.voterId);
        assertEquals("Bailey wins with help of INHERITANCE_MARKER", "bailey", resolver.getWinningValue());
    }

    /**
     * Test XMLUploader.writeBulkInfoHtml
     */
    public void TestBulkUploadHtml() {
        StringWriter out = new StringWriter();
        final String bulkStage = "submit";
        try {
            XMLUploader.writeBulkInfoHtml(bulkStage, out);
        } catch (Exception e) {
            errln("Exception for writeBulkInfoHtml in TestBulkUploadHtml: " + e);
        }
        final String expected = "<div class='bulkNextInfo'>\n<ul>\n<li class='header'>Bulk Upload:</li>\n" +
            "<li class='inactive'>\n<h1>1. upload</h1>\n<h2>Upload XML file</h2>\n</li>\n" +
            "<li class='inactive'>\n<h1>2. check</h1>\n<h2>Verify valid XML</h2>\n</li>\n" +
            "<li class='inactive'>\n<h1>3. test</h1>\n<h2>Test for CLDR errors</h2>\n</li>\n" +
            "<li class='active'>\n<h1>4. submit</h1>\n<h2>Data submitted into SurveyTool</h2>\n</li>\n" +
            "</ul>\n</div>\n";
        assertEquals("writeBulkInfoHtml", expected, out.toString());
    }

    /**
     * Verify that VettingViewer.getMissingStatus returns MissingStatus.PRESENT
     * for a typical path in a well-populated locale
     *
     * Ideally we should also test for MissingStatus.DISPUTED, etc.; that's more difficult
     */
    public void TestMissingStatus() {
        final String path = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"volume-cup\"]/displayName";
        final String locale = "fr";
        final CLDRFile cldrFile = testInfo.getCLDRFile(locale, true);
        final MissingStatus expected = MissingStatus.PRESENT;
        // Note: VettingViewer.getMissingStatus reports PRESENT for items with ↑↑↑ and absent if the item
        // is removed to inherit from root, even though the value obtained is the same in either case;
        // so for path pick an item that does not have ↑↑↑, otherwise when that item is stripped for
        // production data the test will fail.
        final MissingStatus status = VettingViewer.getMissingStatus(cldrFile, path, true /* latin */);
        if (status != expected) {
            errln("Got getMissingStatus = " + status.toString() + "; expected " + expected.toString());
        }
    }

    /**
     * Check that expected paths are Aliased, and have debugging code
     */
    public void TestMissingGrammar() {
        // https://cldr-smoke.unicode.org/cldr-apps/v#/hu/Length/a4915bf505ffb49
        final String path = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"one\"][@case=\"accusative\"]";
        checkGrammarCoverage("hr", path,    MissingStatus.PRESENT, DEBUG, 1, 0, 0, 0, 0); // this isn't a very good test, since we have to adjust each time. Should create fake cldr data instead
        checkGrammarCoverage("kw", path, MissingStatus.ABSENT, false, 0, 0, 1, 1, 0);
        checkGrammarCoverage("en_NZ", path, MissingStatus.ALIASED, DEBUG, 1, 0, 0, 0, 0);
    }

    /**
     * Check the getMissingStatus and getStatus. Note that the values may need to be adjusted in successive versions. The sizes are expected sizes.
     * @param locale
     * @param path
     * @param statusExpected
     * @param debug TODO
     */
    public void checkGrammarCoverage(final String locale, final String path, MissingStatus statusExpected, boolean debug, int... sizes) {
        final CLDRFile cldrFile = testInfo.getCLDRFile(locale, true);
        final MissingStatus expected = statusExpected;
        final MissingStatus status = VettingViewer.getMissingStatus(cldrFile, path, true /* latin */);
        if (status != expected) {
            errln(locale + " got getMissingStatus = " + status.toString() + "; expected " + expected.toString());
        }
        Iterable<String> pathsToTest = Collections.singleton(path);
        Counter<org.unicode.cldr.util.Level> foundCounter = new Counter<>();
        Counter<org.unicode.cldr.util.Level> unconfirmedCounter = new Counter<>();
        Counter<org.unicode.cldr.util.Level> missingCounter = new Counter<>();
        Relation<MissingStatus, String> missingPaths = new Relation(new TreeMap<MissingStatus,String>(), TreeSet.class, Ordering.natural());
        Set<String> unconfirmedPaths = new TreeSet<>();
        VettingViewer.getStatus(pathsToTest, cldrFile, PathHeader.getFactory(),
            foundCounter, unconfirmedCounter, missingCounter, missingPaths, unconfirmedPaths);
        assertEquals(locale + " foundCounter (0)", sizes[0], foundCounter.getTotal());
        assertEquals(locale + " unconfirmedCounter (1)", sizes[1], unconfirmedCounter.getTotal());
        assertEquals(locale + " missingCounter (2)", sizes[2], missingCounter.getTotal());
        assertEquals(locale + " missingPaths (3)", sizes[3], missingPaths.size());
        assertEquals(locale + " unconfirmedPaths (4)", sizes[4], unconfirmedPaths.size());
        showStatusResults(locale, foundCounter, unconfirmedCounter, missingCounter, missingPaths, unconfirmedPaths);
        if (debug) {
            foundCounter.clear();
            unconfirmedCounter.clear();
            missingCounter.clear();
            missingPaths.clear();
            unconfirmedPaths.clear();
            pathsToTest = cldrFile.fullIterable();
            VettingViewer.getStatus(pathsToTest, cldrFile, PathHeader.getFactory(),
                foundCounter, unconfirmedCounter, missingCounter, missingPaths, unconfirmedPaths);
            showStatusResults(locale, foundCounter, unconfirmedCounter, missingCounter, missingPaths, unconfirmedPaths);
        }
    }

    public void showStatusResults(final String locale, Counter<org.unicode.cldr.util.Level> foundCounter,
        Counter<org.unicode.cldr.util.Level> unconfirmedCounter, Counter<org.unicode.cldr.util.Level> missingCounter,
        Relation<MissingStatus, String> missingPaths, Set<String> unconfirmedPaths) {
        warnln("\n" + locale + " foundCounter:\t" + foundCounter
            + "\n" + locale + " unconfirmedCounter:\t" + unconfirmedCounter
            + "\n" + locale + " missingCounter:\t" + missingCounter
            + "\n" + locale + " unconfirmedPaths:\t" + unconfirmedPaths
            + "\n" + locale + " missing paths (modern):"
            );
        int count = 0;
        for (Entry<MissingStatus, String> entry : missingPaths.entrySet()) {
            final MissingStatus missingStatus = entry.getKey();
            final String missingPath = entry.getValue();
            warnln(++count
                + "\t" + locale
                + "\t" + missingStatus
                + "\t" + missingPath
                + "\t" + SUPPLEMENTAL_DATA_INFO.getCoverageLevel(missingPath, locale));
        }
    }

    /**
     * Test the function VoteResolver.Level.canCreateOrSetLevelTo()
     *
     * Compare org.unicode.cldr.unittest.web.TestUserRegistry.TestCanSetUserLevel()
     */
    public void TestCanCreateOrSetLevelTo() {
        if (Level.vetter.canCreateOrSetLevelTo(Level.street)
            || Level.anonymous.canCreateOrSetLevelTo(Level.street)
            || Level.street.canCreateOrSetLevelTo(Level.locked)
            || Level.locked.canCreateOrSetLevelTo(Level.locked)
            ) {
            errln("Only managers and above can change levels at all");
        }
        if (Level.manager.canCreateOrSetLevelTo(Level.tc)
            || Level.manager.canCreateOrSetLevelTo(Level.admin)
            || Level.tc.canCreateOrSetLevelTo(Level.admin)
            ) {
            errln("Can’t change anyone to a more privileged level than you");
        }
    }
}
