package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckConsistentCasing;
import org.unicode.cldr.test.CheckForExemplars;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StringId;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

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
    /**
     * Test the "collisionless" error/warning messages.
     */

    public static final String INDIVIDUAL_TESTS = ".*(CheckCasing|CheckCurrencies|CheckDates|CheckExemplars|CheckForCopy|CheckForExemplars|CheckMetazones|CheckNumbers)";
    static final TestInfo info = TestInfo.getInstance();
    static final Factory factory = info.getCldrFactory();
    static final CLDRFile english = info.getEnglish();

    public void TestFullErrors() {

        CheckCLDR test = CheckCLDR.getCheckAll(factory, INDIVIDUAL_TESTS);
        CheckCLDR.setDisplayInformation(english);

        final String localeID = "fr";
        checkLocale(test, localeID, "?", null);
    }
    
    public void TestAllLocales() {

        CheckCLDR test = CheckCLDR.getCheckAll(factory, INDIVIDUAL_TESTS);
        CheckCLDR.setDisplayInformation(english);
        Set<String> unique = new HashSet();

        LanguageTagParser ltp = new LanguageTagParser();
        int count = 0;
        for (String locale : factory.getAvailable()) {
            if (!ltp.set(locale).getRegion().isEmpty()) {
                continue;
            }
            checkLocale(test, locale, null, unique);
            ++count;
        }
        logln("Count:\t" + count);
    }
    
    public void TestA() {

        CheckCLDR test = CheckCLDR.getCheckAll(factory, INDIVIDUAL_TESTS);
        CheckCLDR.setDisplayInformation(english);
        Set<String> unique = new HashSet();

        checkLocale(test, "ko", null, unique);
    }


    
    public void checkLocale(CheckCLDR test, String localeID, String dummyValue, Set<String> unique) {
        CLDRFile nativeFile = factory.make(localeID, false);
        List<CheckStatus> possibleErrors = new ArrayList();
        Map<String, String> options = new HashMap();
        test.setCldrFileToCheck(nativeFile, options, possibleErrors);
        List<CheckStatus> result = new ArrayList<CheckStatus>();

        CLDRFile patched = nativeFile; // new CLDRFile(override);
        PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(english);
        Set<PathHeader> sorted = new TreeSet<PathHeader>();
        for (String path : patched) {
            final PathHeader pathHeader = pathHeaderFactory.fromPath(path);
            if (pathHeader != null) {
                sorted.add(pathHeader);
            }
        }
        
        System.out.println(localeID);
        UnicodeSet missingCurrencyExemplars = new UnicodeSet();
        UnicodeSet missingExemplars = new UnicodeSet();

        for (PathHeader pathHeader : sorted) {
            String path = pathHeader.getOriginalPath();
            //override.overridePath = path;
            final String resolvedValue = dummyValue == null ? patched.getStringValue(path) : dummyValue;
            test.handleCheck(path, patched.getFullXPath(path), resolvedValue, options, result);
            if (result.size() != 0) {
                for (CheckStatus item : result) {
                    addExemplars(item, missingCurrencyExemplars, missingExemplars);
                    final String mainMessage = StringId.getId(path)
                                                + "\t" + pathHeader 
                                                + "\t" + english.getStringValue(path) 
                                                + "\t" + item.getType() 
                                                + "\t" + item.getSubtype()
                                                ;
                    if (unique != null) {
                        if (unique.contains(mainMessage)) {
                            continue;
                        } else {
                            unique.add(mainMessage);
                        }
                    }
                    logln(localeID + "\t" + mainMessage + "\t" + resolvedValue + "\t" + item.getMessage() + "\t" + pathHeader.getOriginalPath());
                }
            }
        }
        if (missingCurrencyExemplars.size() != 0) {
            logln(localeID + "\tMissing Exemplars (Currency):\t" + missingCurrencyExemplars.toPattern(false));
        }
        if (missingExemplars.size() != 0) {
            logln(localeID + "\tMissing Exemplars:\t" + missingExemplars.toPattern(false));
        }
    }
    
    void addExemplars(CheckStatus status, UnicodeSet missingCurrencyExemplars, UnicodeSet missingExemplars) {
        Object[] parameters = status.getParameters();
        if (parameters != null) {
            if (parameters.length >= 1 && status.getCause().getClass() == CheckForExemplars.class) {
                try {
                    UnicodeSet set = new UnicodeSet(parameters[0].toString());
                    if (status.getMessage().contains("currency")) {
                        missingCurrencyExemplars.addAll(set);
                    } else if (status.getSubtype() != Subtype.discouragedCharactersInTranslation) {
                        missingExemplars.addAll(set);
                    }
                } catch (RuntimeException e) {} // skip if not parseable as set
            }
        }
    }
}
