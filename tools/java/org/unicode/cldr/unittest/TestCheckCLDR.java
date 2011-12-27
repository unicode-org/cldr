package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckConsistentCasing;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.dev.test.TestFmwk;

public class TestCheckCLDR extends TestFmwk {
    public static void main(String[] args) {
        new TestCheckCLDR().run(args);
    }
    static class MyCheckCldr extends org.unicode.cldr.test.CheckCLDR {
        CheckStatus doTest() {
            try {
                throw new IllegalArgumentException("hi");
            } catch (Exception e) {
                return new CheckStatus().setMainType(CheckStatus.warningType).setSubtype(Subtype.abbreviatedDateFieldTooWide)
                .setMessage("An exception {0}, and a number {1}", e, 1.5);      
            }
        }
        @Override
        public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
            return null;
        }
    }
    public void TestExceptions() {
        CheckStatus status = new MyCheckCldr().doTest();
        Exception[] exceptions = status.getExceptionParameters();
        assertEquals("Number of exceptions:", exceptions.length, 1);
        assertEquals("Exception message:", "hi", exceptions[0].getMessage());
        logln(Arrays.asList(exceptions[0].getStackTrace()).toString());
        logln(status.getMessage());
    }
    public static void TestCheckConsistentCasing() {
        TestInfo info = TestInfo.getInstance();
        CheckConsistentCasing c = new CheckConsistentCasing(info.getCldrFactory());
        Map<String, String> options = new LinkedHashMap();
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        final CLDRFile english = info.getEnglish();
        c.setCldrFileToCheck(english, options, possibleErrors);
        for (String path : english) {
            c.check(path, english.getFullXPath(path), english.getStringValue(path), options, possibleErrors);
        }
    }
}
