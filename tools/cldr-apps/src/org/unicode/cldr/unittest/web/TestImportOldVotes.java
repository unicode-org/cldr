package org.unicode.cldr.unittest.web;

import org.unicode.cldr.web.SurveyAjax;
import org.unicode.cldr.web.SurveyException;

import com.ibm.icu.dev.test.TestFmwk;

public class TestImportOldVotes extends TestFmwk {
  
    public static void main(String[] args) {
        new TestImportOldVotes().run(args);
    }

    public static final int TEST_COUNT = 10000;

    public void TestImp() {
        SurveyAjax sa = new SurveyAjax();
        String dummy = "dummy-before-import";
        try {
            dummy = sa.importOldVotesDummy("test");
        } catch (SurveyException e) {
            e.printStackTrace();
        }
        errln("Attention: TestImp errln: importOldVotesDummy returned " + dummy + "\n");
    }

}
