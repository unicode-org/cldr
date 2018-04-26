package org.unicode.cldr.unittest.web;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SurveyAjax;
import org.unicode.cldr.web.SurveyMain;

import com.ibm.icu.dev.test.TestFmwk;

public class TestImportOldVotes extends TestFmwk {
  
    public static void main(String[] args) {
        new TestImportOldVotes().run(args);
    }

    public void TestImp() {
        SurveyAjax sa = null;
        try {
            sa = new SurveyAjax();
        } catch (Exception e) {
            errln("TestImp: new SurveyAjax threw exception: " + e.getMessage() + "\n");
        }
        try {
            CookieSession mySession = null; // = new CookieSession(true, "0.0.0.0");
            // SurveyMain.cldrHome = SurveyMain.getHome() + "/cldr";
            // SurveyMain.vap = "testingvap";
            // SurveyMain sm = new SurveyMain();
            SurveyMain sm = null;
            HttpServletRequest request = null;
            String val = null;
            PrintWriter out = null;
            String what = null;
            String loc = null;
            String xpath = null;
            sa.importOldVotes(mySession, sm, request, val,  out,  what, loc, xpath);
        } catch (Exception e) {
            errln("TestImp: importOldVotes threw exception: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
        errln("TestImp: importOldVotes test is still incomplete.\n");
    }
}
