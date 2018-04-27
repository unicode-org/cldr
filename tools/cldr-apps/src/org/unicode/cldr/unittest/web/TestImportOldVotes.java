package org.unicode.cldr.unittest.web;

import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SurveyAjax;
import org.unicode.cldr.web.SurveyAjax.JSONWriter;
import org.unicode.cldr.web.SurveyMain;

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
        SurveyMain sm = null;
        SurveyAjax sa = null;
        CookieSession mySession = null;
        String val = null;
        String what = null;
        String loc = null;
        String xpath = null;
        boolean isSubmit = false;
        JSONWriter r = null;
        try {
            sa = new SurveyAjax();
        } catch (Exception e) {
            errln("new SurveyAjax threw unexpected exception: " + e.getMessage() + "\n");
            return;
        }
        boolean gotNullPointerException = false;
        try {
            r = sa.importOldVotes(mySession, sm, isSubmit, val, what, loc, xpath);
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
        try {
            // SurveyMain.cldrHome = SurveyMain.getHome() + "/cldr";
            // SurveyMain.vap = "testingvap";
            sm = new SurveyMain();
        } catch (Exception e) {
            errln("new SurveyMain threw unexpected exception: " + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        try {
            mySession = CookieSession.newSession(true /* isGuest */, "0.0.0.0" /* ip */, "?" /* fromID */);
        } catch (Exception e) {
            errln("CookieSession.newSession threw unexpected exception: "
                + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        if (mySession == null) {
            errln("CookieSession.newSession returned null\n");
            return;
        }
        try {
            // problem: mySession.user == null, leads to E_NOT_LOGGED_IN in json string
            r = sa.importOldVotes(mySession, sm, isSubmit, val, what, loc, xpath);
        } catch (Exception e) {
            errln("importOldVotes with sm not null, got exception: "
                    + e.toString() + " - " + e.getMessage() + "\n");
            return;
        }
        String json = r.toString();
        errln("json = " + json + "\n");
        /* Now we have the output of importOldVotes as a long json string, like:
         * {"visitors":"","isBusted":"0","err":"Must be logged in","SurveyOK":"1","progress":"(obsolete-progress)","err_code":"E_NOT_LOGGED_IN",...}
         * We can fix the parameters to importOldVotes so that, for example, it avoids E_NOT_LOGGED_IN; that may be difficult.
         * Alternatively, start refactoring importOldVotes so that this unit testing is easier, with mock inputs;
         * but that risks breaking importOldVotes before we have a unit test to reveal we've broken it...
         */

        errln("importOldVotes test reached end, but test is still incomplete.\n");
    }
}
