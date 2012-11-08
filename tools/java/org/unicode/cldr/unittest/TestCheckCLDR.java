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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckConsistentCasing;
import org.unicode.cldr.test.CheckForExemplars;
import org.unicode.cldr.test.CheckNames;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
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
                return new CheckStatus().setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.abbreviatedDateFieldTooWide)
                    .setMessage("An exception {0}, and a number {1}", e, 1.5);
            }
        }

        @Override
        public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
            List<CheckStatus> result) {
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

    private static final boolean DEBUG = true;

    public void TestPlaceholders() {
        // verify that every item with {0} has a pattern in pattern placeholders,
        // and that every one generates an error in CheckCDLR for patterns when given "?"
        // and that every non-pattern doesn't have an error in CheckCLDR for patterns when given "?"
        Matcher messagePlaceholder = Pattern.compile("\\{\\d+\\}").matcher("");
        PatternPlaceholders patternPlaceholders = PatternPlaceholders.getInstance();

        CheckCLDR test = CheckCLDR.getCheckAll(factory, ".*");
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        Map<String, String> options = new HashMap<String, String>();
        test.setCldrFileToCheck(english, options, possibleErrors);
        List<CheckStatus> result = new ArrayList<CheckStatus>();

        PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(english);
        Set<PathHeader> sorted = new TreeSet<PathHeader>();
        for (String path : english.fullIterable()) {
            sorted.add(pathHeaderFactory.fromPath(path));
        }
        final String testPath = "//ldml/units/unit[@type=\"day-future\"]/unitPattern[@count=\"0\"]";
        sorted.add(pathHeaderFactory.fromPath(testPath));

        for (PathHeader pathHeader : sorted) {
            String path = pathHeader.getOriginalPath();
            if (path.equals(testPath)) {
                int x = 0;
            }
            String value = english.getStringValue(path);
            if (value == null) {
                value = "?";
            }
            boolean containsMessagePattern = messagePlaceholder.reset(value).find();
            final Map<String, PlaceholderInfo> placeholderInfo = patternPlaceholders.get(path);
            final PlaceholderStatus placeholderStatus = patternPlaceholders.getStatus(path);
            if (containsMessagePattern && placeholderStatus == PlaceholderStatus.DISALLOWED || !containsMessagePattern
                && placeholderStatus == PlaceholderStatus.REQUIRED) {
                errln("Value (" + value + ") looks like placeholder = " + containsMessagePattern
                    + ", but placeholder info = " + placeholderStatus + "\t" + path);
                continue;
            } else if (placeholderStatus != PlaceholderStatus.DISALLOWED) {
                if (containsMessagePattern) {
                    Set<String> found = new HashSet<String>();
                    do {
                        found.add(messagePlaceholder.group());
                    } while (messagePlaceholder.find());
                    if (!found.equals(placeholderInfo.keySet())) {
                        errln("Value (" + value + ") has different placeholders than placeholder info ("
                            + placeholderInfo.keySet() + ")\t" + path);
                        continue;
                    } else {
                        logln("placeholder info = " + placeholderInfo + "\t" + path);
                    }
                }

                // check that the error messages are right

                test.handleCheck(path, english.getFullXPath(path), "?", options, result);
                CheckStatus gotIt = null;
                for (CheckStatus i : result) {
                    if (i.getSubtype() == Subtype.missingPlaceholders) {
                        gotIt = i;
                    }
                }
                if (placeholderStatus == PlaceholderStatus.REQUIRED && gotIt == null) {
                    errln("CheckForExemplars SHOULD have detected " + Subtype.missingPlaceholders + " for "
                        + placeholderStatus + " in " + path);
                    if (DEBUG) {
                        test.handleCheck(path, english.getFullXPath(path), "?", options, result);
                    }
                } else if (placeholderStatus == PlaceholderStatus.OPTIONAL && gotIt != null) {
                    errln("CheckForExemplars should NOT have detected " + Subtype.missingPlaceholders + " for "
                        + placeholderStatus + " in " + path);
                    if (DEBUG) {
                        test.handleCheck(path, english.getFullXPath(path), "?", options, result);
                    }
                } else {
                    logln("CheckForExemplars found " + result);
                }
            }
        }
    }

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
        checkLocale(test, factory.make(localeID, false), dummyValue, unique);
    }

    public void checkLocale(CheckCLDR test, CLDRFile nativeFile, String dummyValue, Set<String> unique) {
        String localeID = nativeFile.getLocaleID();
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        Map<String, String> options = new HashMap<String, String>();
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

        logln("Checking: " + localeID);
        UnicodeSet missingCurrencyExemplars = new UnicodeSet();
        UnicodeSet missingExemplars = new UnicodeSet();

        for (PathHeader pathHeader : sorted) {
            String path = pathHeader.getOriginalPath();
            // override.overridePath = path;
            final String resolvedValue = dummyValue == null ? patched.getStringValue(path) : dummyValue;
            test.handleCheck(path, patched.getFullXPath(path), resolvedValue, options, result);
            if (result.size() != 0) {
                for (CheckStatus item : result) {
                    addExemplars(item, missingCurrencyExemplars, missingExemplars);
                    final String mainMessage = StringId.getId(path)
                        + "\t" + pathHeader
                        + "\t" + english.getStringValue(path)
                        + "\t" + item.getType()
                        + "\t" + item.getSubtype();
                    if (unique != null) {
                        if (unique.contains(mainMessage)) {
                            continue;
                        } else {
                            unique.add(mainMessage);
                        }
                    }
                    logln(localeID + "\t" + mainMessage + "\t" + resolvedValue + "\t" + item.getMessage() + "\t"
                        + pathHeader.getOriginalPath());
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
                } catch (RuntimeException e) {
                } // skip if not parseable as set
            }
        }
    }

    public void TestCheckNames() {
        TestInfo info = TestInfo.getInstance();
        CheckCLDR c = new CheckNames();
        Map<String, String> options = new LinkedHashMap();
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        final CLDRFile english = info.getEnglish();
        c.setCldrFileToCheck(english, options, possibleErrors);
        String xpath = "//ldml/localeDisplayNames/languages/language[@type=\"mga\"]";
        c.check(xpath, xpath, "Middle Irish (900-1200) ", options, possibleErrors);
        assertEquals("There should be an error", 1, possibleErrors.size());

        possibleErrors.clear();
        xpath = "//ldml/localeDisplayNames/currencies/currency[@type=\"afa\"]/name";
        c.check(xpath, xpath, "Afghan Afghani (1927-2002)", options, possibleErrors);
        assertEquals("Currencies are allowed to have dates", 0, possibleErrors.size());
    }
}
