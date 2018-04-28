package org.unicode.cldr.unittest.web;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.unicode.cldr.unittest.web.TestAll.WebTestInfo;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyAjax;
import org.unicode.cldr.web.SurveyAjax.JSONWriter;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.XPathTable;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.ElapsedTimer;

public class TestImportOldVotes extends TestFmwk {

    TestAll.WebTestInfo testInfo = WebTestInfo.getInstance();

    STFactory gFac = null;
    UserRegistry.User gUser = null;

    public static void main(String[] args) {
        // new TestImportOldVotes().run(args);
        new TestImportOldVotes().run(TestAll.doResetDb(args));
    }

    /**
     * Test features related to the importOldVotes function.
     * Note: the name of this function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestImpOldVotes() {
        User user = null;
        SurveyMain sm = null;
        SurveyAjax sa = null;
        String val = null;
        String loc = null;
        boolean isSubmit = false;
        JSONWriter r = null;

        if (false) {
            /* Confirm we can create a new SurveyAjax. */
            try {
                sa = new SurveyAjax();
            } catch (Exception e) {
                errln("new SurveyAjax threw unexpected exception: " + e.getMessage() + "\n");
                return;
            }
    
            /* Confirm importOldVotes throws NullPointerException if JSONWriter and SurveyMain are null. */
            boolean gotNullPointerException = false;
            try {
                sa.importOldVotes(r, user, sm, isSubmit, val, loc);
            } catch (Exception e) {
                if (e instanceof NullPointerException) {
                    // This is expected, no errln
                    gotNullPointerException = true;
                }
                else {
                    errln("importOldVotes with sm null, expected NullPointerException, got exception: "
                        + e.toString() + " - " + e.getMessage() + "\n");
                    // e.printStackTrace();
                    return;
                }
            }
            if (!gotNullPointerException) {
                errln("importOldVotes with sm null, expected NullPointerException, got no exception\n");
                return;
            }
        }

        /* Confirm we can create STFactory and SurveyMain. */
        STFactory fac = null;
        try {
            fac = getFactory();
            sm = fac.sm;
            // SurveyMain.cldrHome = SurveyMain.getHome() + "/cldr";
            // SurveyMain.vap = "testingvap";
            // sm = new SurveyMain();
        } catch (Exception e) {
            errln("getFactory or fac.sm threw unexpected exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        if (fac == null || sm == null) {
            errln("null fac or sm");
            return;
        }

        /* Confirm we can initialize and startup the SurveyMain.
         * 
         *  Currently we get this message in the console:
         *  "NOTE:  not inside of web process, using temporary CLDRHOME /Users/tbishop/Documents/WenlinDocs/Organizations/Unicode/CLDR_job/cldr/tools/cldr-apps/testing_cldr_home"
         *  Also we get NullPointerException for this line in SurveyMain.java doStartup:
         *      dbUtils.setupDBProperties(this, survprops);
         *      -- because dbUtils is null!!
         */
        try {
            // CLDRConfig config = CLDRConfig.getInstance();
            // sm.init((ServletConfig) config);
            // ServletConfig config = getServletConfig();
            // sm.init(config);
            // SurveyMain.cldrHome = SurveyMain.getHome() + "/cldr";
            // SurveyMain.vap = "testingvap";
            // sm.init();
            sm.doStartup();
            // sm.doStartupDB();
        } catch (Exception e) {
            errln("doStartup threw unexpected exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }

        /* Confirm we can create a new SurveyAjax. */
        try {
            sa = new SurveyAjax();
        } catch (Exception e) {
            errln("new SurveyAjax threw unexpected exception: " + e.getMessage() + "\n");
            return;
        }

        /* Confirm if user is null, importOldVotes returns E_NOT_LOGGED_IN in json but doesn't throw an exception. */
        try {
           r = sa.newJSONStatus(sm);
           sa.importOldVotes(r, user, sm, isSubmit, val, loc);
        } catch (Exception e) {
            errln("importOldVotes threw exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        String json = r.toString(); // {..."isBusted":"0","err":"Must be logged in","SurveyOK":"1" ... "err_code":"E_NOT_LOGGED_IN",...}
        if (!json.contains("E_NOT_LOGGED_IN")) {
            errln("importOldVotes wotj user NULL, expected json to contain E_NOT_LOGGED_IN, but json = " + json + "\n");
            return;
        }

        /* Confirm we can get a User for an existing user based on email address.
         * TODO: The unit test should not depend on the state of the "real" DB.
         * org.unicode.cldr.unittest.web.TestAll.getDataSource() is supposed to startup the datasource.
         * See TestSTFactory.java for example of how to create an empty DB for testing, and create a new user.

            final STFactory fac = getFactory();

            fac.sm.reg = UserRegistry.createRegistry(SurveyLog.logger, sm);

            String email = name + "@" + org + ".example.com";
            UserRegistry.User u = fac.sm.reg.get(email);
            if (u == null) {
                UserRegistry.User proto = fac.sm.reg.getEmptyUser();
                proto.email = email;
                proto.name = name;
                proto.org = org;
                proto.password = UserRegistry.makePassword(proto.email);
                proto.userlevel = level.getSTLevel();
                proto.locales = UserRegistry.normalizeLocaleList(locales);
                u = fac.sm.reg.newUser(null, proto);
            }
            
            We'll also need to populate the db with old votes available for importing...
         */
        if (sm.reg == null) {
            errln("sm.reg == null\n");
            return;
        }
        try {
            String email = "hinawlinguist.eqmvzyfzv@knyx.cldr.example.com";
            user = sm.reg.get(email);
        } catch (Exception e) {
            errln("sm.reg.get threw exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        if (user == null) {
            errln("user == null\n");
            return;
        }

        /* TODO: test importOldVotesForValidatedUser
            Three ways this function is called:
            (1) loc == null, isSubmit == false: list locales to choose
            (2) loc == 'aa', isSubmit == false: show winning/losing votes available for import
            (3) loc == 'aa', isSubmit == true: update db based on vote

            Check r for expected output... */
        // importOldVotesForValidatedUser(r, user, sm, isSubmit, val, loc, oldVotesTable);

        // TODO: test viewOldVotes
        // Check JSONObject oldvotes for expected output...
        // viewOldVotes(user, sm, loc, locale, oldVotesTable, newVotesTable, oldvotes, fac, file);
        
        // TODO: test submitOldVotes
        // Check JSONObject oldvotes for expected output...
        // submitOldVotes(user, sm, locale, val, oldVotesTable, newVotesTable, oldvotes, fac);

        errln("importOldVotes test reached end, but test is still incomplete.\n");
    }
    
    /**
     * Get an STFactory for testing.
     * This function is based on one in TestTFactory.java.
     * @return the STFactory
     */
   private STFactory getFactory() throws SQLException {
        if (gFac == null) {
            long start = System.currentTimeMillis();
            TestAll.setupTestDb();
            logln("Set up test DB: " + ElapsedTimer.elapsedTime(start));

            ElapsedTimer et0 = new ElapsedTimer("clearing directory");
            //File cacheDir = TestAll.getEmptyDir(CACHETEST);
            logln(et0.toString());

            et0 = new ElapsedTimer("setup SurveyMain");
            SurveyMain sm = new SurveyMain();
            CookieSession.sm = sm; // hack - of course.
            logln(et0.toString());

            SurveyMain.fileBase = CLDRPaths.MAIN_DIRECTORY;
            SurveyMain.fileBaseSeed = new File(CLDRPaths.BASE_DIRECTORY, "seed/main/").getAbsolutePath();
            SurveyMain.fileBaseA = new File(CLDRPaths.BASE_DIRECTORY, "common/annotations/").getAbsolutePath();
            SurveyMain.fileBaseASeed = new File(CLDRPaths.BASE_DIRECTORY, "seed/annotations/").getAbsolutePath();

            SurveyMain.setFileBaseOld(CLDRPaths.BASE_DIRECTORY);
            // sm.twidPut(Vetting.TWID_VET_VERBOSE, true); // set verbose
            // vetting
            SurveyLog.logger = Logger.getAnonymousLogger();

            et0 = new ElapsedTimer("setup DB");
            Connection conn = DBUtils.getInstance().getDBConnection();
            logln(et0.toString());

            et0 = new ElapsedTimer("setup Registry");
            sm.reg = UserRegistry.createRegistry(SurveyLog.logger, sm);
            logln(et0.toString());

            et0 = new ElapsedTimer("setup XPT");
            sm.xpt = XPathTable.createTable(conn, sm);
            sm.xpt.getByXpath("//foo/bar[@type='baz']");
            logln(et0.toString());
            et0 = new ElapsedTimer("close connection");
            DBUtils.closeDBConnection(conn);
            logln(et0.toString());
            // sm.vet = Vetting.createTable(sm.logger, sm);

            // CLDRDBSourceFactory fac = new CLDRDBSourceFactory(sm,
            // sm.fileBase, Logger.getAnonymousLogger(), cacheDir);
            // logln("Setting up DB");
            // sm.setDBSourceFactory(fac);ignore
            // fac.setupDB(DBUtils.getInstance().getDBConnection());
            // logln("Vetter Ready (this will take a while..)");
            // fac.vetterReady(TestAll.getProgressIndicator(this));

            et0 = new ElapsedTimer("Set up STFactory");
            gFac = sm.getSTFactory();
            logln(et0.toString());
        }
        return gFac;
    }
}
