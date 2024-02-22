package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.ibm.icu.dev.util.ElapsedTimer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.unittest.web.TestAll;
import org.unicode.cldr.unittest.web.TestAll.WebTestInfo;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.FileReaders;
import org.unicode.cldr.util.SpecialLocales;
import org.unicode.cldr.util.StackTracker;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.web.BallotBox.InvalidXPathException;
import org.unicode.cldr.web.BallotBox.VoteNotAcceptedException;
import org.unicode.cldr.web.UserRegistry.LogoutException;
import org.unicode.cldr.web.UserRegistry.User;

@NotThreadSafe
public class TestSTFactory {

    TestAll.WebTestInfo testInfo = WebTestInfo.getInstance();

    static STFactory gFac = null;
    UserRegistry.User gUser = null;

    @BeforeAll
    public static void setup() {
        TestAll.setupTestDb();
    }

    /** validate the phase and mode */
    @BeforeEach
    public void validatePhase() {
        CLDRConfig c = CLDRConfig.getInstance();
        assertNotNull(c);
        assertTrue(
                c.getPhase().isUnitTest(),
                () -> String.format("Phase %s returned false for isUnitTest()", c.getPhase()));
        assertEquals(
                Environment.UNITTEST, c.getEnvironment(), "Please set -DCLDR_ENVIRONMENT=UNITTEST");
        TestAll.assumeHaveDb();
    }

    @Test
    public void TestBasicFactory() throws SQLException {
        CLDRLocale locale = CLDRLocale.getInstance("aa");
        STFactory fac = getFactory();
        CLDRFile mt = fac.make(locale, false);
        BallotBox<User> box = fac.ballotBoxForLocale(locale);
        mt.iterator();
        final String somePath = "//ldml/localeDisplayNames/keys/key[@type=\"collation\"]";
        box.getValues(somePath);
    }

    @Test
    public void TestReadonlyLocales() throws SQLException {
        STFactory fac = getFactory();

        verifyReadOnly(fac.make("root", false));
        verifyReadOnly(fac.make("en", false));
    }

    private static final String ANY = "*";
    private static final String NULL = "<NULL>";

    private String expect(
            String path,
            String expectString,
            boolean expectVoted,
            CLDRFile file,
            BallotBox<User> box)
            throws LogoutException, SQLException {
        CLDRLocale locale = CLDRLocale.getInstance(file.getLocaleID());
        String currentWinner = file.getStringValue(path);
        boolean didVote = box.userDidVote(getMyUser(), path);
        StackTraceElement them = StackTracker.currentElement(0);
        String where = " (" + them.getFileName() + ":" + them.getLineNumber() + "): ";

        if (expectString == null) expectString = NULL;
        if (currentWinner == null) currentWinner = NULL;

        if (expectString != ANY && !expectString.equals(currentWinner)) {
            assertEquals(
                    expectString,
                    currentWinner,
                    "ERR:"
                            + where
                            + "Expected '"
                            + expectString
                            + "': "
                            + locale
                            + ":"
                            + path
                            + " ='"
                            + currentWinner
                            + "', "
                            + votedToString(didVote)
                            + box.getResolver(path));
        } else if (expectVoted != didVote) {
            assertEquals(
                    expectVoted,
                    didVote,
                    "ERR:"
                            + where
                            + "Expected VOTING="
                            + votedToString(expectVoted)
                            + ":  "
                            + locale
                            + ":"
                            + path
                            + " ='"
                            + currentWinner
                            + "', "
                            + votedToString(didVote)
                            + box.getResolver(path));
        } else {
            System.out.println(
                    where
                            + ":"
                            + locale
                            + ":"
                            + path
                            + " ='"
                            + currentWinner
                            + "', "
                            + votedToString(didVote)
                            + box.getResolver(path));
        }
        return currentWinner;
    }

    /**
     * @param didVote
     * @return
     */
    private String votedToString(boolean didVote) {
        return didVote ? "(I VOTED)" : "( did NOT VOTE) ";
    }

    @Test
    public void TestBasicVote()
            throws SQLException, IOException, InvalidXPathException, VoteNotAcceptedException,
                    LogoutException {
        STFactory fac = getFactory();

        final String somePath = "//ldml/localeDisplayNames/keys/key[@type=\"collation\"]";
        String originalValue = null;
        String changedTo = null;

        CLDRLocale locale = CLDRLocale.getInstance("de");
        CLDRLocale localeSub = CLDRLocale.getInstance("de_CH");
        {
            CLDRFile mt = fac.make(locale, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale);

            originalValue = expect(somePath, ANY, false, mt, box);

            changedTo = "The main pump fixing screws with the correct strength class"; // as
            // per
            // ticket:2260

            if (originalValue.equals(changedTo)) {
                fail(
                        "for "
                                + locale
                                + " value "
                                + somePath
                                + " winner is already= "
                                + originalValue);
            }

            box.voteForValue(getMyUser(), somePath, changedTo); // vote
            expect(somePath, changedTo, true, mt, box);

            box.voteForValue(getMyUser(), somePath, null); // unvote
            expect(somePath, originalValue, false, mt, box);

            box.voteForValue(getMyUser(), somePath, changedTo); // vote again
            expect(somePath, changedTo, true, mt, box);

            Date modDate = mt.getLastModifiedDate(somePath);
            assertNotNull(modDate, "@1: mod date was null!");
            System.out.println("@1: mod date " + modDate);
        }

        // Restart STFactory.
        fac = resetFactory();
        {
            CLDRFile mt = fac.make(locale, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale);

            expect(somePath, changedTo, true, mt, box);

            {
                Date modDate = mt.getLastModifiedDate(somePath);
                assertNotNull(modDate, "@2: mod date was null!");
            }
            CLDRFile mt2 = fac.make(locale, true);
            {
                Date modDate = mt2.getLastModifiedDate(somePath);
                assertNotNull(modDate, "@2a: mod date was null!");
                System.out.println("@2a: mod date " + modDate);
            }
            CLDRFile mtMT = fac.make(localeSub, true);
            {
                Date modDate = mtMT.getLastModifiedDate(somePath);
                assertNotNull(modDate, "@2b: mod date was null!");
                System.out.println("@2b: mod date " + modDate);
            }
            CLDRFile mtMTb = fac.make(localeSub, false);
            {
                Date modDate = mtMTb.getLastModifiedDate(somePath);
                assertNull(modDate, "@2c: mod date should be null (unresolved source)!");
            }
            // unvote
            box.voteForValue(getMyUser(), somePath, null);

            expect(somePath, originalValue, false, mt, box);
            {
                Date modDate = mt.getLastModifiedDate(somePath);
                assertNull(modDate, "@3: mod date was null!");
            }
        }
        fac = resetFactory();
        {
            CLDRFile mt = fac.make(locale, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale);

            expect(somePath, originalValue, false, mt, box);

            // vote for ____2
            changedTo = changedTo + "2";

            System.out.println("VoteFor: " + changedTo);
            box.voteForValue(getMyUser(), somePath, changedTo);

            expect(somePath, changedTo, true, mt, box);

            System.out.println("Write out..");
            File targDir = TestAll.getEmptyDir(TestSTFactory.class.getName() + "_output");
            File outFile = new File(targDir, locale.getBaseName() + ".xml");
            PrintWriter pw =
                    FileUtilities.openUTF8Writer(
                            targDir.getAbsolutePath(), locale.getBaseName() + ".xml");
            mt.write(pw, noDtdPlease);
            pw.close();

            System.out.println("Read back..");
            CLDRFile readBack = null;
            readBack =
                    CLDRFile.loadFromFile(outFile, locale.getBaseName(), DraftStatus.unconfirmed);
            String reRead = readBack.getStringValue(somePath);

            System.out.println(
                    "reread:  "
                            + outFile.getAbsolutePath()
                            + " value "
                            + somePath
                            + " = "
                            + reRead);
            if (!changedTo.equals(reRead)) {
                System.out.println(
                        "reread:  "
                                + outFile.getAbsolutePath()
                                + " value "
                                + somePath
                                + " = "
                                + reRead
                                + ", should be "
                                + changedTo);
            }
        }
    }

    @Test
    public void TestDenyVote() throws SQLException, IOException {
        STFactory fac = getFactory();
        final String somePath2 = "//ldml/localeDisplayNames/keys/key[@type=\"numbers\"]";
        // String originalValue2 = null;
        String changedTo2 = null;
        // test votring for a bad locale
        {
            CLDRLocale locale2 = CLDRLocale.getInstance("mt_MT");
            // CLDRFile mt_MT = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            try {
                box.voteForValue(getMyUser(), somePath2, changedTo2);
                fail("Error! should have failed to vote for " + locale2);
            } catch (Throwable t) {
                System.out.println(
                        "Good - caught " + t.toString() + " as this locale is a default content.");
            }
        }
        {
            CLDRLocale locale2 = CLDRLocale.getInstance("en");
            // CLDRFile mt_MT = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            try {
                box.voteForValue(getMyUser(), somePath2, changedTo2);
                fail("Error! should have failed to vote for " + locale2);
            } catch (Throwable t) {
                System.out.println(
                        "Good - caught " + t.toString() + " as this locale is readonly english.");
            }
        }
        {
            CLDRLocale locale2 = CLDRLocale.getInstance("no");
            // CLDRFile no = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);
            final String bad_xpath =
                    "//ldml/units/unitLength[@type=\"format\"]/unit[@type=\"murray\"]/unitPattern[@count=\"many\"]";

            try {
                box.voteForValue(getMyUser(), bad_xpath, "{0} Murrays"); // bogus
                fail("Error! should have failed to vote for " + locale2 + " xpath " + bad_xpath);
            } catch (Throwable t) {
                System.out.println(
                        "Good - caught "
                                + t.toString()
                                + " voting for "
                                + bad_xpath
                                + " as this is a bad xpath.");
            }
        }
    }

    @Test
    public void TestSparseVote()
            throws SQLException, IOException, InvalidXPathException, SurveyException,
                    LogoutException {
        STFactory fac = getFactory();

        final String somePath2 = "//ldml/localeDisplayNames/keys/key[@type=\"calendar\"]";
        String originalValue2 = null;
        String changedTo2 = null;
        CLDRLocale locale2 = CLDRLocale.getInstance("fr_BE");
        // Can't (and shouldn't) try to do this test if the locale is configured as read-only
        // (including algorithmic).
        if (SpecialLocales.Type.isReadOnly(SpecialLocales.getType(locale2))) {
            return;
        }

        // test sparsity
        {
            CLDRFile cldrFile = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            originalValue2 = expect(somePath2, null, false, cldrFile, box);

            changedTo2 = "The alternate pump fixing screws with the incorrect strength class";

            if (originalValue2.equals(changedTo2)) {
                fail(
                        "for "
                                + locale2
                                + " value "
                                + somePath2
                                + " winner is already= "
                                + originalValue2);
            }

            box.voteForValue(getMyUser(), somePath2, changedTo2);

            expect(somePath2, changedTo2, true, cldrFile, box);
        }
        // Restart STFactory.
        fac = resetFactory();
        {
            CLDRFile cldrFile = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            expect(somePath2, changedTo2, true, cldrFile, box);

            // unvote
            box.voteForValue(getMyUser(), somePath2, null);

            /*
             * No one has voted; expect inheritance to win
             */
            expect(somePath2, CldrUtility.INHERITANCE_MARKER, false, cldrFile, box);
        }
        fac = resetFactory();
        {
            CLDRFile cldrFile = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            expect(somePath2, null, false, cldrFile, box);

            // vote for ____2
            changedTo2 = changedTo2 + "2";

            System.out.println("VoteFor: " + changedTo2);
            box.voteForValue(getMyUser(), somePath2, changedTo2);

            expect(somePath2, changedTo2, true, cldrFile, box);

            System.out.println("Write out..");
            File targDir = TestAll.getEmptyDir(TestSTFactory.class.getName() + "_output");
            File outFile = new File(targDir, locale2.getBaseName() + ".xml");
            PrintWriter pw =
                    FileUtilities.openUTF8Writer(
                            targDir.getAbsolutePath(), locale2.getBaseName() + ".xml");
            cldrFile.write(pw, noDtdPlease);
            pw.close();

            System.out.println("Read back..");
            CLDRFile readBack =
                    CLDRFile.loadFromFile(outFile, locale2.getBaseName(), DraftStatus.unconfirmed);

            String reRead = readBack.getStringValue(somePath2);

            System.out.println(
                    "reread:  "
                            + outFile.getAbsolutePath()
                            + " value "
                            + somePath2
                            + " = "
                            + reRead);
            if (!changedTo2.equals(reRead)) {
                System.out.println(
                        "reread:  "
                                + outFile.getAbsolutePath()
                                + " value "
                                + somePath2
                                + " = "
                                + reRead
                                + ", should be "
                                + changedTo2);
            }
        }
    }

    @Test
    public void TestVettingDataDriven() throws SQLException, IOException {
        runDataDrivenTest(TestSTFactory.class.getSimpleName()); // TestSTFactory.xml
    }

    @Test
    public void TestVotePerf() throws SQLException, IOException {
        final CheckCLDR.Phase p = CLDRConfig.getInstance().getPhase();
        assertTrue(p.isUnitTest(), "phase " + p + ".isUnitTest()");
        runDataDrivenTest("TestVotePerf");
    }

    public void TestUserRegistry() throws SQLException, IOException {
        runDataDrivenTest("TestUserRegistry");
    }

    private void runDataDrivenTest(final String fileBasename) throws SQLException, IOException {
        final STFactory fac = getFactory();
        final File targDir = TestAll.getEmptyDir(TestSTFactory.class.getName() + "_output");

        XMLFileReader myReader = new XMLFileReader();
        final Map<String, String> attrs = new TreeMap<>();
        final Map<String, String> vars = new TreeMap<>();
        myReader.setHandler(new DataDrivenSTTestHandler(vars, fac, targDir, attrs));
        final String fileName = fileBasename + ".xml";
        myReader.read(
                TestSTFactory.class
                        .getResource("data/" + fileName)
                        .toString(), // for DTD resolution
                getUTF8Data(fileName),
                -1,
                true);
    }

    /**
     * Fetch data from jar
     *
     * @param name name of thing to load (org.unicode.cldr.web.data.name)
     */
    public static BufferedReader getUTF8Data(String name) throws java.io.IOException {
        return FileReaders.openFile(STFactory.class, "data/" + name);
    }

    @Test
    public void TestVettingWithNonDistinguishing()
            throws SQLException, IOException, InvalidXPathException, SurveyException,
                    LogoutException {
        if (TestAll.skipIfNoDb()) return;
        STFactory fac = getFactory();

        final String somePath2 =
                "//ldml/dates/calendars/calendar[@type=\"hebrew\"]/dateFormats/dateFormatLength[@type=\"full\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
        String originalValue2 = null;
        String changedTo2 = null;
        CLDRLocale locale2 = CLDRLocale.getInstance("he");
        String fullPath = null;
        {
            CLDRFile mt_MT = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            originalValue2 = expect(somePath2, ANY, false, mt_MT, box);

            fullPath = mt_MT.getFullXPath(somePath2);
            System.out.println("locale " + locale2 + " path " + somePath2 + " full = " + fullPath);
            if (!fullPath.contains("numbers=")) {
                System.out.println(
                        "Warning: "
                                + locale2
                                + ":"
                                + somePath2
                                + " fullpath doesn't contain numbers= - test skipped, got path "
                                + fullPath);
                return;
            }

            changedTo2 = "EEEE, d _MMMM y";

            if (originalValue2.equals(changedTo2)) {
                fail(
                        "for "
                                + locale2
                                + " value "
                                + somePath2
                                + " winner is already= "
                                + originalValue2);
            }

            box.voteForValue(getMyUser(), somePath2, changedTo2);

            expect(somePath2, changedTo2, true, mt_MT, box);
        }
        // Restart STFactory.
        fac = resetFactory();
        {
            CLDRFile mt_MT = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            expect(somePath2, changedTo2, true, mt_MT, box);

            // unvote
            box.voteForValue(getMyUser(), somePath2, null);

            expect(somePath2, originalValue2, false, mt_MT, box); // Expect
            // original
            // value - no
            // one has
            // voted.

            String fullPath2 = mt_MT.getFullXPath(somePath2);
            if (!fullPath2.contains("numbers=")) {
                fail("Error - voted, but full path lost numbers= - " + fullPath2);
            }
        }
        fac = resetFactory();
        {
            CLDRFile mt_MT = fac.make(locale2, false);
            BallotBox<User> box = fac.ballotBoxForLocale(locale2);

            expect(somePath2, originalValue2, false, mt_MT, box); // still
            // original
            // value

            // vote for ____2
            changedTo2 = changedTo2 + "__";

            System.out.println("VoteFor: " + changedTo2);
            box.voteForValue(getMyUser(), somePath2, changedTo2);

            expect(somePath2, changedTo2, true, mt_MT, box);

            System.out.println("Write out..");
            File targDir = TestAll.getEmptyDir(TestSTFactory.class.getName() + "_output");
            File outFile = new File(targDir, locale2.getBaseName() + ".xml");
            PrintWriter pw =
                    FileUtilities.openUTF8Writer(
                            targDir.getAbsolutePath(), locale2.getBaseName() + ".xml");
            mt_MT.write(pw, noDtdPlease);
            pw.close();

            System.out.println("Read back..");
            CLDRFile readBack =
                    CLDRFile.loadFromFile(outFile, locale2.getBaseName(), DraftStatus.unconfirmed);

            String reRead = readBack.getStringValue(somePath2);

            System.out.println(
                    "reread:  "
                            + outFile.getAbsolutePath()
                            + " value "
                            + somePath2
                            + " = "
                            + reRead);
            if (!changedTo2.equals(reRead)) {
                System.out.println(
                        "reread:  "
                                + outFile.getAbsolutePath()
                                + " value "
                                + somePath2
                                + " = "
                                + reRead
                                + ", should be "
                                + changedTo2);
            }
            String fullPath2 = readBack.getFullXPath(somePath2);
            if (!fullPath2.contains("numbers=")) {
                fail("Error - readBack's full path lost numbers= - " + fullPath2);
            }
        }
    }

    private void verifyReadOnly(CLDRFile f) {
        String loc = f.getLocaleID();
        try {
            f.add("//ldml/foo", "bar");
            fail("Error: " + loc + " is supposed to be readonly.");
        } catch (Throwable t) {
            System.out.println("Pass: " + loc + " is readonly, caught " + t.toString());
        }
    }

    public UserRegistry.User getMyUser() throws LogoutException, SQLException {
        if (gUser == null) {
            gUser = getFactory().sm.reg.get(null, UserRegistry.ADMIN_EMAIL, "[::1]", true);
        }
        return gUser;
    }

    public static synchronized STFactory getFactory() throws SQLException {
        if (gFac == null) {
            gFac = createFactory();
        }
        return gFac;
    }

    private STFactory resetFactory() throws SQLException {
        if (gFac == null) {
            System.out.println("STFactory wasn't loaded - not resetting.");
            return getFactory();
        } else {
            System.out.println("--- resetting STFactory() ----- [simulate reload] ------------");
            return gFac = getFactory().TESTING_shutdownAndRestart();
        }
    }

    public static STFactory createFactory() throws SQLException {
        long start = System.currentTimeMillis();
        TestAll.setupTestDb();
        System.err.println("Set up test DB: " + ElapsedTimer.elapsedTime(start));

        ElapsedTimer et0 = new ElapsedTimer("clearing directory");
        // File cacheDir = TestAll.getEmptyDir(CACHETEST);
        System.err.println(et0.toString());

        et0 = new ElapsedTimer("setup SurveyMain");
        SurveyMain sm = new SurveyMain();
        CookieSession.sm = sm; // hack - of course.
        System.err.println(et0.toString());

        SurveyMain.fileBase = CLDRPaths.MAIN_DIRECTORY;
        SurveyMain.fileBaseSeed =
                new File(CLDRPaths.BASE_DIRECTORY, "seed/main/").getAbsolutePath();
        SurveyMain.fileBaseA =
                new File(CLDRPaths.BASE_DIRECTORY, "common/annotations/").getAbsolutePath();
        SurveyMain.fileBaseASeed =
                new File(CLDRPaths.BASE_DIRECTORY, "seed/annotations/").getAbsolutePath();

        et0 = new ElapsedTimer("setup DB");
        Connection conn = DBUtils.getInstance().getAConnection();
        System.err.println(et0.toString());

        et0 = new ElapsedTimer("setup Registry");
        sm.reg = UserRegistry.createRegistry(sm);
        System.err.println(et0.toString());

        et0 = new ElapsedTimer("setup XPT");
        sm.xpt = XPathTable.createTable(conn);
        sm.xpt.getByXpath("//foo/bar[@type='baz']");
        System.err.println(et0.toString());
        et0 = new ElapsedTimer("close connection");
        DBUtils.closeDBConnection(conn);
        System.err.println(et0.toString());
        et0 = new ElapsedTimer("Set up STFactory");
        STFactory fac = sm.getSTFactory();
        System.err.println(et0.toString());

        org.junit.jupiter.api.Assertions.assertFalse(
                SurveyMain.isBusted(), "SurveyTool shouldn’t be busted!");
        return fac;
    }

    static final Map<String, Object> noDtdPlease = new TreeMap<>();

    static {
        noDtdPlease.put(
                "DTD_DIR", CLDRPaths.COMMON_DIRECTORY + File.separator + "dtd" + File.separator);
    }
}
