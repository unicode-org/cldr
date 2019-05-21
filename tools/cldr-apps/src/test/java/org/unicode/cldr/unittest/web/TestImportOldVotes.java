package org.unicode.cldr.unittest.web;

import org.unicode.cldr.unittest.web.TestAll.WebTestInfo;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.UserRegistry;

import com.ibm.icu.dev.test.TestFmwk;

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
        logln("importOldVotes unit test is not implemented yet.\n");

       /* User user = null;
        SurveyMain sm = null;
        SurveyAjax sa = null;
        String val = null;
        String loc = null;
        boolean isSubmit = false;
        JSONWriter r = null;
        */
        /* Confirm we can create STFactory and SurveyMain. */
        /*
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
        */
        /* Confirm we can initialize and startup the SurveyMain.
         * 
         *  Currently we get this message in the console:
         *  "NOTE:  not inside of web process, using temporary CLDRHOME /Users/tbishop/Documents/WenlinDocs/Organizations/Unicode/CLDR_job/cldr/tools/cldr-apps/testing_cldr_home"
         *  Also we get NullPointerException for this line in SurveyMain.java doStartup:
         *      dbUtils.setupDBProperties(this, survprops);
         *      -- because dbUtils is null!!
         */
        /*
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
        */
        /* Confirm we can create a new SurveyAjax. */
        /*
        try {
            sa = new SurveyAjax();
        } catch (Exception e) {
            errln("new SurveyAjax threw unexpected exception: " + e.getMessage() + "\n");
            return;
        }
        */
        /* Confirm if user is null, importOldVotes returns E_NOT_LOGGED_IN in json but doesn't throw an exception. */
        /*
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
        */
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
        /*
        if (sm.reg == null) {
            errln("sm.reg == null\n");
            return;
        }
        */
        /*
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
        */

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

        // errln("importOldVotes test reached end, but test is still incomplete.\n");
    }
}
