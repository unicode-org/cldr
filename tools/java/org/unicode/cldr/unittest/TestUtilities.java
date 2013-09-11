package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.ConvertLanguageData.InverseComparator;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DelegatingIterator;
import org.unicode.cldr.util.EscapingUtilities;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PluralSamples;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.VettingViewer.VoteStatus;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.CandidateInfo;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.Organization;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestUtilities extends TestFmwk {
    private static final UnicodeSet DIGITS = new UnicodeSet("[0-9]");
    static TestInfo testInfo = TestInfo.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = testInfo.getSupplementalDataInfo();
    private static final int STRING_ID_TEST_COUNT = 1024 * 16;

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
        Set<Count> counts = SUPPLEMENTAL_DATA_INFO.getPlurals(locale).getCounts();
        for (int i = 1; i < 5; ++i) {
            Map<Count, Double> samplesForDigits = pluralSamples.getSamples(i);
            if (!counts.containsAll(samplesForDigits.keySet())) {
                errln(locale + ": mismatch in samples, expected " + counts + ", got: " + samplesForDigits);
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

        public String toString() {
            return "StringIdThread " + id;
        }
    }

    public void TestStringId() {
        ArrayList<StringIdThread> threads = new ArrayList<StringIdThread>();

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
        Matcher byte1 = Pattern.compile("%[A-Za-z0-9]{2}").matcher("");
        Matcher byte2 = Pattern.compile("%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}").matcher("");
        Matcher byte3 = Pattern.compile("%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}").matcher("");
        Matcher byte4 = Pattern.compile("%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}%[A-Za-z0-9]{2}").matcher("");
        for (int i = 1; i <= 0x10FFFF; i = i * 3 / 2 + 1) {
            String escaped = EscapingUtilities.urlEscape(new StringBuilder().appendCodePoint(i).toString());
            logln(Integer.toHexString(i) + " => " + escaped);
            if (EscapingUtilities.OK_TO_NOT_QUOTE.contains(i)) {
                assertTrue("Should be unquoted", escaped.length() == 1);
            } else if (i < 0x80) {
                assertTrue("Should be %xx", byte1.reset(escaped).matches());
            } else if (i < 0x800) {
                assertTrue("Should be %xx%xx", byte2.reset(escaped).matches());
            } else if (i < 0x10000) {
                assertTrue("Should be %xx%xx%xx", byte3.reset(escaped).matches());
            } else {
                assertTrue("Should be %xx%xx%xx%xx", byte4.reset(escaped).matches());
            }
        }
    }

    public void TestDelegatingIterator() {
        Set<String> s = new TreeSet<String>(Arrays.asList(new String[] { "a", "b", "c" }));
        Set<String> t = new LinkedHashSet<String>(Arrays.asList(new String[] { "f", "d", "e" }));
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
        for (Object u : DelegatingIterator.array(1, "t", "u", new UnicodeSet("[a-z]"))) {
            result.append(u);
        }
        assertEquals("Iterator", "1tu[a-z]", result.toString());
    }

    public void TestCounter() {
        Counter<String> counter = new Counter<String>(true);
        Comparator<String> uca = new Comparator<String>() {
            Collator col = Collator.getInstance(ULocale.ENGLISH);

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

        assertEquals("getKeysetSortedByKey", Arrays.asList("a", "b", "c", "d"), new ArrayList<String>(counter
            .getKeysetSortedByKey()));

        assertEquals("getKeysetSortedByCount(true, ucaDown)", Arrays.asList("d", "c", "a", "b"), new ArrayList<String>(
            counter.getKeysetSortedByCount(true, ucaDown)));

        assertEquals("getKeysetSortedByCount(true, null), value", Arrays.asList("d", "a", "c", "b"),
            new ArrayList<String>(counter.getKeysetSortedByCount(true, uca)));

        assertEquals("getKeysetSortedByCount(false, ucaDown), descending", Arrays.asList("b", "c", "a", "d"),
            new ArrayList<String>(counter.getKeysetSortedByCount(false, ucaDown)));

        assertEquals("getKeysetSortedByCount(false, null), descending, value", Arrays.asList("b", "a", "c", "d"),
            new ArrayList<String>(counter.getKeysetSortedByCount(false, uca)));
    }

    public void TestOrganizationOrder() {
        Map<String, Organization> stringToOrg = new TreeMap<String, Organization>();
        for (Organization org : Organization.values()) {
            stringToOrg.put(org.toString(), org);
        }
        List<Organization> reordered = new ArrayList<Organization>(stringToOrg.values());
        List<Organization> plain = Arrays.asList(Organization.values());
        for (int i = 0; i < reordered.size(); ++i) {
            assertEquals("Items not in alphabetical order", reordered.get(i), plain.get(i));
        }
    }

    public void TestOrganizationNames() {
        UnicodeSet uppercase = new UnicodeSet("[:uppercase:]");
        for (Organization org : Organization.values()) {
            if (!uppercase.contains(org.getDisplayName().codePointAt(0))) {
                errln("Organization name isn't titlecased: " + org + ", " + org.getDisplayName());
            }
            assertEquals("Organization from enum name", org, Organization.fromString(org.toString()));
            assertEquals("Organization from display name", org, Organization.fromString(org.getDisplayName()));
        }
    }

    // public void TestVoteResolverData() {
    // final PrintWriter errorLogPrintWriter = this.getErrorLogPrintWriter();
    // final PrintWriter logPrintWriter = this.getLogPrintWriter();
    // String userFile = CldrUtility.getProperty("usersxml", CldrUtility.TMP_DIRECTORY +
    // "/incoming/vetted/usersa/usersa.xml");
    // String votesDirectory = CldrUtility.getProperty("votesxml", CldrUtility.TMP_DIRECTORY +
    // "/incoming/vetted/votes/");
    // String vettedDirectory = CldrUtility.getProperty("vetted", CldrUtility.TMP_DIRECTORY + "/incoming/vetted/main/");
    //
    // PathValueInfo.voteInfo = VoteResolver.getIdToPath(votesDirectory + "xpathTable.xml");
    // Factory factory = Factory.make(vettedDirectory, ".*");
    //
    // VoteResolver.setVoterToInfo(userFile);
    // Map<String, Map<Organization, Relation<Level, Integer>>> map = VoteResolver
    // .getLocaleToVetters();
    // for (String locale : map.keySet()) {
    // Map<Organization, Relation<Level, Integer>> orgToLevelToVoter = map.get(locale);
    // String localeName = null;
    // try {
    // localeName = testInfo.getEnglish().getName(locale);
    // } catch (RuntimeException e) {
    // errln("Invalid locale:\t" + locale);
    // localeName = "UNVALID(" + locale + ")";
    // }
    // if (DEBUG) {
    // for (Organization org : orgToLevelToVoter.keySet()) {
    // log(locale + "\t" + localeName + "\t" + org + ":");
    // final Relation<Level, Integer> levelToVoter = orgToLevelToVoter.get(org);
    // for (Level level : levelToVoter.keySet()) {
    // log("\t" + level + "=" + levelToVoter.getAll(level).size());
    // }
    // logln("");
    // }
    // }
    // }
    //
    // File votesDir = new File(votesDirectory);
    // for (String file : votesDir.list()) {
    // if (file.startsWith("xpathTable")) {
    // continue;
    // }
    // if (file.endsWith(".xml")) {
    // final String locale = file.substring(0,file.length()-4);
    // try {
    // checkLocaleVotes(factory, locale, votesDirectory, errorLogPrintWriter, logPrintWriter);
    // } catch (RuntimeException e) {
    // errln("Can't process " + locale + ": " + e.getMessage() + " " + Arrays.asList(e.getStackTrace()));
    // //throw (RuntimeException) new IllegalArgumentException("Can't process " + locale).initCause(e);
    // }
    // }
    // }
    // }

    static final boolean SHOW_DETAILS = CldrUtility.getProperty("showdetails", false);
    private static final CharSequence DEBUG_COMMENT = "set up a case of conflict within organization";

    // private void checkLocaleVotes(Factory factory, final String locale, String votesDirectory, PrintWriter errorLog,
    // PrintWriter warningLog) {
    // // logln("*** Locale " + locale + ": \t***");
    // PathValueInfo pathValueInfo = new PathValueInfo(factory, locale);

    // Map<Organization, Level> orgToMaxVote = VoteResolver.getOrganizationToMaxVote(locale);
    // if (orgToMaxVote.size() == 0) {
    // logln("");
    // logln(locale + ": \tNo organizations with translators");
    // } else if (!locale.contains("_")) {
    // logln("");
    // logln(locale + ": \tOrganizations with translators:\t" + orgToMaxVote);
    // }
    //
    // Map<Integer, Map<Integer, CandidateInfo>> info = VoteResolver.getBaseToAlternateToInfo(votesDirectory + locale
    // + ".xml");
    // Set<Organization> missingOrganizations = EnumSet.noneOf(Organization.class);
    // Counter<Organization> missingOrganizationCounter = new Counter<Organization>(true);
    // Counter<Organization> goodOrganizationCounter = new Counter<Organization>(true);
    // Counter<Status> winningStatusCounter = new Counter<Status>(true);
    // EnumSet<Organization> conflictedOrganizations = EnumSet.noneOf(Organization.class);
    // Set<Integer> missingOptimals = new TreeSet<Integer>();
    //
    // Set<Integer> surveyVsVoteResolverDifferences = new TreeSet<Integer>();
    //
    // Set<Integer> unknownVotersSoFar = new HashSet<Integer>();
    //
    // Counter<Status> oldStatusCounter = new Counter<Status>(true);
    // Counter<Status> surveyStatusCounter = new Counter<Status>(true);
    // Counter<Type> surveyTypeCounter = new Counter<Type>(true);
    // VoteResolver<String> voteResolver = new VoteResolver<String>();
    // Map<String, Integer> valueToItem = new HashMap<String, Integer>();
    //
    // for (int basePath : info.keySet()) {
    // final Map<Integer, CandidateInfo> itemInfo = info.get(basePath);
    // // if there is any approved value, then continue;
    // Status surveyWinningStatus = null;
    // String surveyWinningValue = null;
    //
    // // find the last release status and value
    // voteResolver.clear();
    // boolean haveOldStatus = false;
    //
    // valueToItem.clear();
    //
    // for (int item : itemInfo.keySet()) {
    // String itemValue = getValue(item);
    // String realPath = pathValueInfo.getRealPath(item);
    // if (realPath == null) {
    // logln(locale + ": \t!!! missing path for " + item);
    // continue;
    // }
    // String realValue = pathValueInfo.getRealValue(item);
    // if (realValue == null) {
    // logln(locale + ": \t!!! missing value for " + item);
    // continue;
    // }
    //
    // if (valueToItem.containsKey(itemValue)) {
    // errln(locale + ": \tTwo alternatives with same value:\t" + item + ", " + itemValue);
    // } else {
    // valueToItem.put(itemValue, item);
    // }
    //
    // CandidateInfo candidateInfo = itemInfo.get(item);
    // oldStatusCounter.add(candidateInfo.oldStatus, 1);
    // surveyStatusCounter.add(candidateInfo.surveyStatus, 1);
    // surveyTypeCounter.add(candidateInfo.surveyType, 1);
    // if (candidateInfo.surveyType == Type.optimal) {
    // if (surveyWinningValue != null) {
    // errln(locale + ": \tDuplicate optimal item:\t" + item);
    // }
    // surveyWinningStatus = candidateInfo.surveyStatus;
    // surveyWinningValue = String.valueOf(item);
    // }
    // if (candidateInfo.oldStatus != null) {
    // if (haveOldStatus) {
    // errln(locale + ": \tDuplicate optimal item:\t" + item);
    // }
    // haveOldStatus = true;
    // voteResolver.setLastRelease(itemValue, candidateInfo.oldStatus);
    // }
    // voteResolver.add(itemValue);
    // for (int voter : candidateInfo.voters) {
    // try {
    // voteResolver.add(itemValue, voter);
    // } catch (UnknownVoterException e) {
    // if (!unknownVotersSoFar.contains(e.getVoter())) {
    // errln(locale + ":\t" + e);
    // unknownVotersSoFar.add(e.getVoter());
    // }
    // }
    // }
    // }
    //
    // if (voteResolver.size() == 0) {
    // logln(locale + ": \t!!! no values for " + basePath);
    // continue;
    // }
    // if (surveyWinningValue == null) {
    // missingOptimals.add(basePath);
    // surveyWinningValue = BAD_VALUE;
    // }
    //
    // EnumSet<Organization> basePathConflictedOrganizations = voteResolver.getConflictedOrganizations();
    // conflictedOrganizations.addAll(basePathConflictedOrganizations);
    //
    // Status winningStatus = voteResolver.getWinningStatus();
    // String winningValue = voteResolver.getWinningValue();
    //
    // winningStatusCounter.add(winningStatus, 1);
    //
    // // we'll say the status is "good enough" if they have the same votes

    // final boolean sameResults = surveyWinningStatus == winningStatus
    // && voteResolver.getValuesWithSameVotes().contains(surveyWinningValue);
    // if (surveyWinningStatus == Status.approved && sameResults) {
    // continue;
    // }
    // if (!sameResults) {
    // surveyVsVoteResolverDifferences.add(basePath);
    // if (SHOW_DETAILS) {
    // showPaths(pathValueInfo, locale, basePath, itemInfo);
    // log("\t***Different results for:\t" + basePath);
    // if (surveyWinningStatus != winningStatus) {
    // log(", status ST:\t" + surveyWinningStatus);
    // log(", VR:\t" + winningStatus);
    // }
    // if (!voteResolver.getValuesWithSameVotes().contains(surveyWinningValue)) {
    // log(", value ST:\t" + surveyWinningValue);
    // log(", VR:\t" + winningValue);
    // }
    // logln("");
    // logln("\t\tVR:" + voteResolver);
    // }
    // }
    //
    // CandidateInfo candidateInfo = itemInfo.get(valueToItem.get(winningValue));
    // Map<Organization, Level> orgToMaxVoteHere = VoteResolver.getOrganizationToMaxVote(candidateInfo.voters);
    //
    // // if the winning item is less than contributed, record the organizations that haven't given their maximum
    // // vote to the winning item.
    // if (winningStatus.compareTo(Status.contributed) < 0) {
    // // showPaths(basePath, itemInfo);
    // missingOrganizations.clear();
    // for (Organization org : orgToMaxVote.keySet()) {
    // Level maxVote = orgToMaxVote.get(org);
    // Level maxVoteHere = orgToMaxVoteHere.get(org);
    // if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
    // missingOrganizations.add(org);
    // missingOrganizationCounter.add(org, 1);
    // }
    // }
    // // logln("&Missing organizations:\t" + missingOrganizations);
    // } else {
    // for (Organization org : orgToMaxVote.keySet()) {
    // Level maxVote = orgToMaxVote.get(org);
    // Level maxVoteHere = orgToMaxVoteHere.get(org);
    // if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
    // } else {
    // goodOrganizationCounter.add(org, 1);
    // }
    // }
    // }
    // }
    // if (missingOptimals.size() != 0) {
    // errln(locale + ": \tSurvey Tool missing optimal item for basePaths:\t" + missingOptimals);
    // }
    // if (surveyVsVoteResolverDifferences.size() > 0) {
    // errln(locale + ": \tSurvey Tool vs VoteResolver differences (approx):\t"
    // + surveyVsVoteResolverDifferences.size());
    // }
    // if (missingOrganizationCounter.size() > 0) {
    // if (SHOW_DETAILS) {
    // logln(locale + ": \toldStatus values:\t" + oldStatusCounter + ", TOTAL:\t"
    // + oldStatusCounter.getTotal());
    // logln(locale + ": \tsurveyType values:\t" + surveyTypeCounter + ", TOTAL:\t"
    // + surveyTypeCounter.getTotal());
    // logln(locale + ": \tsurveyStatus values:\t" + surveyStatusCounter + ", TOTAL:\t"
    // + surveyStatusCounter.getTotal());
    // }
    // logln(locale + ": \tMIA organizations:\t" + missingOrganizationCounter);
    // logln(locale + ": \tConflicted organizations:\t" + conflictedOrganizations);
    // logln(locale + ": \tCool organizations!:\t" + goodOrganizationCounter);
    // }
    // logln(locale + ": \tOptimal Status:\t" + winningStatusCounter);
    // }

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

    private void showPaths(PathValueInfo pathValueInfo, String locale, int basePath,
        final Map<Integer, CandidateInfo> itemInfo) {
        logln(locale + " basePath:\t" + basePath + "\t" + pathValueInfo.getRealPath(basePath));
        for (int item : itemInfo.keySet()) {
            CandidateInfo candidateInfo = itemInfo.get(item);
            logln("\tpath:\t" + item + ", " + candidateInfo);
            logln("\tvalue:\t" + pathValueInfo.getRealValue(item) + "\tpath:\t<" + pathValueInfo.getRealPath(item)
                + ">");
        }
    }

    Map<Integer, VoterInfo> testdata = CldrUtility.asMap(
        new Object[][] {
            { 801, new VoterInfo(Organization.guest, Level.street, "guestS") },
            { 701, new VoterInfo(Organization.gnome, Level.street, "gnomeS") },
            { 404, new VoterInfo(Organization.google, Level.vetter, "googleV") },
            { 411, new VoterInfo(Organization.google, Level.street, "googleS") },
            { 424, new VoterInfo(Organization.google, Level.vetter, "googleV2") },
            { 304, new VoterInfo(Organization.apple, Level.vetter, "appleV") },
            { 208, new VoterInfo(Organization.adobe, Level.expert, "adobeE") },
            { 101, new VoterInfo(Organization.ibm, Level.street, "ibmS") },
        });

    private int toVoterId(String s) {
        for (Entry<Integer, VoterInfo> entry : testdata.entrySet()) {
            if (s.equals(entry.getValue().getName())) {
                return entry.getKey();
            }
        }
        return Integer.MIN_VALUE;
    }

    public void TestTrunkStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<String>();
        resolver.setEstablishedFromLocale("de");

        resolver.setLastRelease("old-item", Status.approved);
        resolver.setTrunk("new-item", Status.approved);
        assertEquals("", "new-item", resolver.getWinningValue());

        resolver.clear();
        resolver.setLastRelease("old-item", Status.approved);
        resolver.setTrunk("new-item", Status.provisional);
        assertEquals("", "old-item", resolver.getWinningValue());
    }

    public void TestVoteResolverNgombaTrunkStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<String>();
        resolver.setEstablishedFromLocale("jgo");
        final String jgo21 = "\uA78B"; // "[ɑ ɑ́ ɑ̂ ɑ̌ b c d ɛ {ɛ́} {ɛ̂} {ɛ̌} {ɛ̀} {ɛ̄} f ɡ h i í î ǐ j k l m ḿ {m̀} {m̄} n ń ǹ {n̄} ŋ {ŋ́} {ŋ̀} {ŋ̄} ɔ {ɔ́} {ɔ̂} {ɔ̌} p pf s sh t ts u ú û ǔ ʉ {ʉ́} {ʉ̂} {ʉ̌} {ʉ̈} v w ẅ y z Ꞌ]";
        final String jgo22trunk = "\uA78C"; // "[a á â ǎ b c d ɛ {ɛ́} {ɛ̂} {ɛ̌} {ɛ̀} {ɛ̄} f ɡ h i í î ǐ j k l m ḿ {m̀} {m̄} n ń ǹ {n̄} ŋ {ŋ́} {ŋ̀} {ŋ̄} ɔ {ɔ́} {ɔ̂} {ɔ̌} p {pf} s {sh} t {ts} u ú û ǔ ʉ {ʉ́} {ʉ̂} {ʉ̌} {ʉ̈} v w ẅ y z ꞌ]";
        Status oldStatus = Status.approved;
        resolver.setLastRelease(jgo21, oldStatus); // seed/jgo.xml from 21
        logln("Last release: " + jgo21 + ", " + oldStatus);
        resolver.setTrunk(jgo22trunk, Status.approved); // seed/jgo.xml from 22 trunk
        logln("SVN: " + jgo22trunk);
        logln(resolver.toString());
        assertEquals("Winning Value", jgo22trunk, resolver.getWinningValue());
    }

    public void TestVoteStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<String>();

        resolver.setEstablishedFromLocale("de");
        resolver.setLastRelease("foo", Status.approved);
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
        resolver.setLastRelease(s1, Status.approved);
        resolver.add(s2, toVoterId("appleV"));
        voteStatus = resolver.getStatusForOrganization(Organization.apple);
        assertEquals("", VoteStatus.ok, voteStatus);
    }

    public void TestLosingStatus() {
        // af
        // losing? {lastRelease: {BQ, missing}, trunk: {null, null}, {orgToVotes: , totals: {}, conflicted: []},
        // sameVotes: [BQ], O: null, N: null, totals: {}, winning: {BQ, missing}}
        // XPath: //ldml/localeDisplayNames/territories/territory[@type="BQ"]
        // gcvs.openoffice_org.example.com
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<String>();

        resolver.setEstablishedFromLocale("af");
        resolver.setLastRelease("BQ", Status.missing);
        VoteStatus status = resolver.getStatusForOrganization(Organization.openoffice_org);
        assertEquals("", VoteStatus.provisionalOrWorse, status);

        // {lastRelease: {{0}: {1}, missing}, trunk: {null, null}, {orgToVotes: pakistan={{0}: {1}=8}, totals: {{0}:
        // {1}=8}, conflicted: []}, sameVotes: [{0}: {1}], O: {0}: {1}, N: null, totals: {{0}: {1}=8}, winning: {{0}:
        // {1}, approved}}
        resolver.clear();
        resolver.setLastRelease("{0}: {1}", Status.missing);
        resolver.add("{0}: {1}", toVoterId("adobeE"));
        status = resolver.getStatusForOrganization(Organization.openoffice_org);
        assertEquals("", VoteStatus.ok, status);

        // {lastRelease: {Arabisch, approved}, trunk: {Arabisch, approved}, {orgToVotes: , totals: {}, conflicted: []},
        // sameVotes: [Arabisch], O: null, N: null, totals: {}, winning: {Arabisch, approved}}
        resolver.clear();
        resolver.setLastRelease("Arabisch", Status.approved);
        resolver.setTrunk("Arabisch", Status.approved);
        status = resolver.getStatusForOrganization(Organization.openoffice_org);
        assertEquals("", VoteStatus.ok_novotes, status);
    }

    public void TestTotalVotesStatus() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<String>();

        Status oldStatus = Status.unconfirmed;

        resolver.setEstablishedFromLocale("de");
        resolver.setLastRelease("foo", oldStatus);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("apple", toVoterId("appleV"));

        // check that alphabetical wins when votes are equal
        String winner = resolver.getWinningValue();
        Status winningStatus = resolver.getWinningStatus();
        assertEquals("", "apple", winner);
        assertEquals("", Status.provisional, winningStatus);

        resolver.clear();
        resolver.setEstablishedFromLocale("de");
        resolver.setLastRelease("foo", oldStatus);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("zebra", toVoterId("googleS"));
        resolver.add("apple", toVoterId("appleV"));

        // check that total votes over alphabetical
        winner = resolver.getWinningValue();
        winningStatus = resolver.getWinningStatus();
        assertEquals("", "zebra", winner);
        assertEquals("", Status.provisional, winningStatus);
    }

    public void TestResolvedVoteCounts() {
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<String>();

        Status oldStatus = Status.unconfirmed;

        resolver.setEstablishedFromLocale("de");
        resolver.setLastRelease("foo", oldStatus);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("apple", toVoterId("appleV"));

        // check that alphabetical wins when votes are equal
        Map<String, Long> counts = resolver.getResolvedVoteCounts();
        logln(counts.toString());
        assertEquals("", "foo", new ArrayList<String>(counts.keySet()).get(2));

        resolver.clear();
        resolver.setEstablishedFromLocale("de");
        resolver.setLastRelease("foo", Status.approved);
        resolver.add("zebra", toVoterId("googleV"));
        resolver.add("apple", toVoterId("appleV"));
        counts = resolver.getResolvedVoteCounts();
        logln(counts.toString());
        assertEquals("", "foo", new ArrayList<String>(counts.keySet()).get(0));

        resolver.clear();
        resolver.setEstablishedFromLocale("de");
        resolver.setLastRelease("foo", Status.approved);
        resolver.add("zebra", toVoterId("googleS"));
        counts = resolver.getResolvedVoteCounts();
        logln(counts.toString());
        assertEquals("", "foo", new ArrayList<String>(counts.keySet()).get(0));
    }

    public void TestVoteResolver() {
        // to make it easier to debug failures, the first digit is an org, second is the individual in that org, and
        // third is the voting weight.
        VoteResolver.setVoterToInfo(testdata);
        VoteResolver<String> resolver = new VoteResolver<String>();
        String[] tests = {
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
            "sameVotes=best, next",
            "conflicts=[google]",
            "status=provisional",
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
            "208=primo",
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
            "208=primo",
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
            "411=best",
            "101=best",
            "oldStatus=contributed",
            // expected values
            "value=best",
            "sameVotes=best",
            "status=contributed",
            "conflicts=[]",
            "check",
        };
        String expectedValue = null;
        String expectedConflicts = null;
        Status expectedStatus = null;
        String oldValue = null;
        Status oldStatus = null;
        List<String> sameVotes = null;
        String locale = null;
        Map<Integer, String> values = new TreeMap<Integer, String>();
        int counter = -1;

        for (String test : tests) {
            String[] item = test.split("=");
            String name = item[0];
            String value = item.length < 2 ? null : item[1];
            if (name.equalsIgnoreCase("comment")) {
                logln("#\t" + value);
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
            } else if (name.equalsIgnoreCase("sameVotes")) {
                sameVotes = value == null ? new ArrayList<String>(0) : Arrays.asList(value.split(",\\s*"));
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
                resolver.setEstablishedFromLocale(locale);
                resolver.setLastRelease(oldValue, oldStatus);
                for (int voter : values.keySet()) {
                    resolver.add(values.get(voter), voter);
                }
                // print the contents
                logln(counter + "\t" + values);
                logln(resolver.toString());
                // now print the values
                assertEquals(counter + " value", expectedValue, resolver.getWinningValue());
                assertEquals(counter + " sameVotes", sameVotes.toString(), resolver.getValuesWithSameVotes().toString());
                assertEquals(counter + " status", expectedStatus, resolver.getWinningStatus());
                assertEquals(counter + " conflicts", expectedConflicts, resolver.getConflictedOrganizations()
                    .toString());
                resolver.clear();
                values.clear();
            } else {
                errln("unknown command:\t" + test);
            }
        }
    }
}
