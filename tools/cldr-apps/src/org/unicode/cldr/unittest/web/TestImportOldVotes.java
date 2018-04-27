package org.unicode.cldr.unittest.web;

import org.unicode.cldr.web.SurveyAjax;
import org.unicode.cldr.web.SurveyAjax.JSONWriter;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.dev.test.TestFmwk;

public class TestImportOldVotes extends TestFmwk {
  
    public static void main(String[] args) {
        new TestImportOldVotes().run(args);
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
        String xpath = null;
        boolean isSubmit = false;
        JSONWriter r = null;
        /* Confirm we can create a new SurveyAjax. */
        try {
            sa = new SurveyAjax();
        } catch (Exception e) {
            errln("new SurveyAjax threw unexpected exception: " + e.getMessage() + "\n");
            return;
        }
        /* Confirm importOldVotes throws NullPointerException if SurveyMain is null. */
        boolean gotNullPointerException = false;
        try {
            r = sa.importOldVotes(user, sm, isSubmit, val, loc, xpath);
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
        /* Confirm we can create a new SurveyMain. */
        try {
            // SurveyMain.cldrHome = SurveyMain.getHome() + "/cldr";
            // SurveyMain.vap = "testingvap";
            sm = new SurveyMain();
        } catch (Exception e) {
            errln("new SurveyMain threw unexpected exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        /* Confirm we can initialize and startup the SurveyMain. */
        try {
            // CLDRConfig config = CLDRConfig.getInstance();
            // sm.init((ServletConfig) config);
            // ServletConfig config = getServletConfig();
            // sm.init(config);
            // SurveyMain.cldrHome = SurveyMain.getHome() + "/cldr";
            // SurveyMain.vap = "testingvap";
            sm.init();
            sm.doStartup();
            // sm.doStartupDB();
        } catch (Exception e) {
            errln("sm.init or doStartup threw unexpected exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }

        /* Confirm if user is null, importOldVotes returns E_NOT_LOGGED_IN in json but doesn't throw an exception. */
        try {
            r = sa.importOldVotes(user, sm, isSubmit, val, loc, xpath);
        } catch (Exception e) {
            errln("importOldVotes threw exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        String json = r.toString(); // {..."isBusted":"0","err":"Must be logged in","SurveyOK":"1" ... "err_code":"E_NOT_LOGGED_IN",...}
        if (!json.contains("E_NOT_LOGGED_IN")) {
            errln("importOldVotes wotj user NULL, expected json to contain E_NOT_LOGGED_IN, but json = " + json + "\n");
            return;
        }
        /* Confirm we can get a User for an existing user based on email address. */
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

        errln("importOldVotes test reached end, but test is still incomplete.\n");
    }
}
