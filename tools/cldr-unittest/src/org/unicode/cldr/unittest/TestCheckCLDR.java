package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.InputMethod;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckCLDR.StatusAction;
import org.unicode.cldr.test.CheckConsistentCasing;
import org.unicode.cldr.test.CheckDates;
import org.unicode.cldr.test.CheckForExemplars;
import org.unicode.cldr.test.CheckNames;
import org.unicode.cldr.test.CheckNew;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRInfo.CandidateInfo;
import org.unicode.cldr.util.CLDRInfo.PathValueInfo;
import org.unicode.cldr.util.CLDRInfo.UserInfo;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.text.UnicodeSet;

public class TestCheckCLDR extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();
    private final Set<String> eightPointLocales = new TreeSet<String>(
        Arrays.asList("ar ca cs da de el es fi fr he hi hr hu id it ja ko lt lv nb nl pl pt pt_PT ro ru sk sl sr sv th tr uk vi zh zh_Hant".split(" ")));

    public static void main(String[] args) {
        new TestCheckCLDR().run(args);
    }

    static class MyCheckCldr extends org.unicode.cldr.test.CheckCLDR {
        CheckStatus doTest() {
            try {
                throw new IllegalArgumentException("hi");
            } catch (Exception e) {
                return new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.abbreviatedDateFieldTooWide)
                    .setMessage("An exception {0}, and a number {1}", e,
                        1.5);
            }
        }

        @Override
        public CheckCLDR handleCheck(String path, String fullPath,
            String value, Options options, List<CheckStatus> result) {
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
        CheckConsistentCasing c = new CheckConsistentCasing(
            testInfo.getCldrFactory());
        Map<String, String> options = new LinkedHashMap<String, String>();
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        final CLDRFile english = testInfo.getEnglish();
        c.setCldrFileToCheck(english, options, possibleErrors);
        for (String path : english) {
            c.check(path, english.getFullXPath(path),
                english.getStringValue(path), options, possibleErrors);
        }
    }

    /**
     * Test the "collisionless" error/warning messages.
     */

    public static final String INDIVIDUAL_TESTS = ".*(CheckCasing|CheckCurrencies|CheckDates|CheckExemplars|CheckForCopy|CheckForExemplars|CheckMetazones|CheckNumbers)";
    static final Factory factory = testInfo.getCldrFactory();
    static final CLDRFile english = testInfo.getEnglish();

    private static final boolean DEBUG = true;

    public void TestPlaceholders() {
        // verify that every item with {0} has a pattern in pattern
        // placeholders,
        // and that every one generates an error in CheckCDLR for patterns when
        // given "?"
        // and that every non-pattern doesn't have an error in CheckCLDR for
        // patterns when given "?"
        Matcher messagePlaceholder = PatternCache.get("\\{\\d+\\}").matcher("");
        PatternPlaceholders patternPlaceholders = PatternPlaceholders
            .getInstance();

        CheckCLDR test = CheckCLDR.getCheckAll(factory, ".*");
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        Options options = new Options();
        test.setCldrFileToCheck(english, options, possibleErrors);
        List<CheckStatus> result = new ArrayList<CheckStatus>();

        PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(english);
        Set<PathHeader> sorted = new TreeSet<PathHeader>();
        for (String path : english.fullIterable()) {
            sorted.add(pathHeaderFactory.fromPath(path));
        }
        final String testPath = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day-future\"]/unitPattern[@count=\"0\"]";
        sorted.add(pathHeaderFactory.fromPath(testPath));

        for (PathHeader pathHeader : sorted) {
            String path = pathHeader.getOriginalPath();
            String value = english.getStringValue(path);
            if (value == null) {
                value = "?";
            }
            boolean containsMessagePattern = messagePlaceholder.reset(value)
                .find();
            final Map<String, PlaceholderInfo> placeholderInfo = patternPlaceholders
                .get(path);
            final PlaceholderStatus placeholderStatus = patternPlaceholders
                .getStatus(path);
            if (containsMessagePattern && placeholderStatus == PlaceholderStatus.DISALLOWED
                || !containsMessagePattern && placeholderStatus == PlaceholderStatus.REQUIRED) {
                errln("Value (" + value + ") looks like placeholder = "
                    + containsMessagePattern + ", but placeholder info = "
                    + placeholderStatus + "\t" + path);
                continue;
            } else if (placeholderStatus != PlaceholderStatus.DISALLOWED) {
                if (containsMessagePattern) {
                    Set<String> found = new HashSet<String>();
                    do {
                        found.add(messagePlaceholder.group());
                    } while (messagePlaceholder.find());
                    if (!found.equals(placeholderInfo.keySet())) {
                        // ^//ldml/characterLabels/characterLabelPattern[@type="category_list"] ; {0}=CATEGORY_TYPE family; {1}=REMAINING_ITEMS man, woman, girl
                        if (path.equals("//ldml/characterLabels/characterLabelPattern[@type=\"category-list\"]")) {
                            logKnownIssue("cldrbug:9534", "commenting out characterLabelPattern[@type=\"category-list\"] for now, pending real fix.");
                        } else {
                            errln("Value ("
                                + value
                                + ") has different placeholders than placeholder info ("
                                + placeholderInfo.keySet() + ")\t" + path);
                        }
                        continue;
                    } else {
                        logln("placeholder info = " + placeholderInfo + "\t"
                            + path);
                    }
                }

                // check that the error messages are right

                test.handleCheck(path, english.getFullXPath(path), "?",
                    options, result);
                CheckStatus gotIt = null;
                for (CheckStatus i : result) {
                    if (i.getSubtype() == Subtype.missingPlaceholders) {
                        gotIt = i;
                    }
                }
                if (placeholderStatus == PlaceholderStatus.REQUIRED
                    && gotIt == null) {
                    errln("CheckForExemplars SHOULD have detected "
                        + Subtype.missingPlaceholders + " for "
                        + placeholderStatus + " in " + path);
                    if (DEBUG) {
                        test.handleCheck(path, english.getFullXPath(path), "?",
                            options, result);
                    }
                } else if (placeholderStatus == PlaceholderStatus.OPTIONAL
                    && gotIt != null) {
                    errln("CheckForExemplars should NOT have detected "
                        + Subtype.missingPlaceholders + " for "
                        + placeholderStatus + " in " + path);
                    if (DEBUG) {
                        test.handleCheck(path, english.getFullXPath(path), "?",
                            options, result);
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
        Set<String> unique = new HashSet<String>();

        LanguageTagParser ltp = new LanguageTagParser();
        int count = 0;
        for (String locale : getInclusion() <= 5 ? eightPointLocales : factory.getAvailable()) {
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
        Set<String> unique = new HashSet<String>();

        checkLocale(test, "ko", null, unique);
    }

    public void checkLocale(CheckCLDR test, String localeID, String dummyValue,
        Set<String> unique) {
        checkLocale(test, testInfo.getCLDRFile(localeID, false), dummyValue, unique);
    }

    public void checkLocale(CheckCLDR test, CLDRFile nativeFile,
        String dummyValue, Set<String> unique) {
        String localeID = nativeFile.getLocaleID();
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        CheckCLDR.Options options = new CheckCLDR.Options();
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
            final String resolvedValue = dummyValue == null ? patched
                .getStringValue(path) : dummyValue;
                test.handleCheck(path, patched.getFullXPath(path), resolvedValue,
                    options, result);
                if (result.size() != 0) {
                    for (CheckStatus item : result) {
                        addExemplars(item, missingCurrencyExemplars,
                            missingExemplars);
                        final String mainMessage = StringId.getId(path) + "\t"
                            + pathHeader + "\t" + english.getStringValue(path)
                            + "\t" + item.getType() + "\t" + item.getSubtype();
                        if (unique != null) {
                            if (unique.contains(mainMessage)) {
                                continue;
                            } else {
                                unique.add(mainMessage);
                            }
                        }
                        logln(localeID + "\t" + mainMessage + "\t" + resolvedValue
                            + "\t" + item.getMessage() + "\t"
                            + pathHeader.getOriginalPath());
                    }
                }
        }
        if (missingCurrencyExemplars.size() != 0) {
            logln(localeID + "\tMissing Exemplars (Currency):\t"
                + missingCurrencyExemplars.toPattern(false));
        }
        if (missingExemplars.size() != 0) {
            logln(localeID + "\tMissing Exemplars:\t"
                + missingExemplars.toPattern(false));
        }
    }

    void addExemplars(CheckStatus status, UnicodeSet missingCurrencyExemplars,
        UnicodeSet missingExemplars) {
        Object[] parameters = status.getParameters();
        if (parameters != null) {
            if (parameters.length >= 1
                && status.getCause().getClass() == CheckForExemplars.class) {
                try {
                    UnicodeSet set = new UnicodeSet(parameters[0].toString());
                    if (status.getMessage().contains("currency")) {
                        missingCurrencyExemplars.addAll(set);
                    } else {
                        missingExemplars.addAll(set);
                    }
                } catch (RuntimeException e) {
                } // skip if not parseable as set
            }
        }
    }

    public void TestCheckNames() {
        CheckCLDR c = new CheckNames();
        Map<String, String> options = new LinkedHashMap<String, String>();
        List<CheckStatus> possibleErrors = new ArrayList<CheckStatus>();
        final CLDRFile english = testInfo.getEnglish();
        c.setCldrFileToCheck(english, options, possibleErrors);
        String xpath = "//ldml/localeDisplayNames/languages/language[@type=\"mga\"]";
        c.check(xpath, xpath, "Middle Irish (900-1200) ", options,
            possibleErrors);
        assertEquals("There should be an error", 1, possibleErrors.size());

        possibleErrors.clear();
        xpath = "//ldml/localeDisplayNames/currencies/currency[@type=\"afa\"]/name";
        c.check(xpath, xpath, "Afghan Afghani (1927-2002)", options,
            possibleErrors);
        assertEquals("Currencies are allowed to have dates", 0,
            possibleErrors.size());
    }

    public void TestCheckNew() {
        String path = "//ldml/dates/timeZoneNames/zone[@type=\"Europe/Dublin\"]/long/daylight";
        CheckCLDR c = new CheckNew(testInfo.getCldrFactory());
        List<CheckStatus> result = new ArrayList<CheckStatus>();
        Map<String, String> options = new HashMap<String, String>();
        c.setCldrFileToCheck(testInfo.getCLDRFile("fr", true),
            options, result);
        c.check(path, path, "foobar", options, result);
        for (CheckStatus status : result) {
            if (status.getSubtype() != Subtype.modifiedEnglishValue) {
                continue;
            }
            assertEquals(
                null,
                "The English value for this field changed from “Irish Summer Time” to “Irish Standard Time’, but the corresponding value for your locale didn't change.",
                status.getMessage());
            return;
        }
        errln("No failure message.");
    }

    public void TestCheckDates() {
        CheckCLDR.setDisplayInformation(testInfo.getEnglish()); // just in case
        String prefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"";
        String infix = "\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"";
        String suffix = "\"]";

        TestFactory testFactory = new TestFactory();

        List<CheckStatus> result = new ArrayList<CheckStatus>();
        Options options = new Options();
        final String collidingValue = "foobar";

        // Selection has stricter collision rules, because is is used to select different messages.
        // So two types with the same localization do collide unless they have exactly the same rules.

        Object[][] tests = {
            { "en" }, // set locale

            // nothing collides with itself
            { Type.format, DayPeriod.night1, Type.format, DayPeriod.night1, Subtype.none },
            { Type.format, DayPeriod.morning1, Type.format, DayPeriod.morning1, Subtype.none },
            { Type.format, DayPeriod.afternoon1, Type.format, DayPeriod.afternoon1, Subtype.none },
            { Type.format, DayPeriod.evening1, Type.format, DayPeriod.evening1, Subtype.none },

            { Type.format, DayPeriod.am, Type.format, DayPeriod.am, Subtype.none },
            { Type.format, DayPeriod.pm, Type.format, DayPeriod.pm, Subtype.none },
            { Type.format, DayPeriod.noon, Type.format, DayPeriod.noon, Subtype.none },
            { Type.format, DayPeriod.midnight, Type.format, DayPeriod.midnight, Subtype.none },

            { Type.selection, DayPeriod.night1, Type.selection, DayPeriod.night1, Subtype.none },
            { Type.selection, DayPeriod.morning1, Type.selection, DayPeriod.morning1, Subtype.none },
            { Type.selection, DayPeriod.afternoon1, Type.selection, DayPeriod.afternoon1, Subtype.none },
            { Type.selection, DayPeriod.evening1, Type.selection, DayPeriod.evening1, Subtype.none },

            { Type.selection, DayPeriod.am, Type.selection, DayPeriod.am, Subtype.none },
            { Type.selection, DayPeriod.pm, Type.selection, DayPeriod.pm, Subtype.none },
            { Type.selection, DayPeriod.noon, Type.selection, DayPeriod.noon, Subtype.none },
            { Type.selection, DayPeriod.midnight, Type.selection, DayPeriod.midnight, Subtype.none },

            // fixed classes always collide
            { Type.format, DayPeriod.am, Type.format, DayPeriod.pm, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.am, Type.format, DayPeriod.noon, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.am, Type.format, DayPeriod.midnight, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.pm, Type.format, DayPeriod.noon, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.pm, Type.format, DayPeriod.midnight, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.noon, Type.format, DayPeriod.midnight, Subtype.dateSymbolCollision },

            { Type.selection, DayPeriod.am, Type.selection, DayPeriod.pm, Subtype.dateSymbolCollision },
            { Type.selection, DayPeriod.am, Type.selection, DayPeriod.noon, Subtype.dateSymbolCollision },
            { Type.selection, DayPeriod.am, Type.selection, DayPeriod.midnight, Subtype.dateSymbolCollision },
            { Type.selection, DayPeriod.pm, Type.selection, DayPeriod.noon, Subtype.dateSymbolCollision },
            { Type.selection, DayPeriod.pm, Type.selection, DayPeriod.midnight, Subtype.dateSymbolCollision },
            { Type.selection, DayPeriod.noon, Type.selection, DayPeriod.midnight, Subtype.dateSymbolCollision },

            // 00-06 night1
            // 06-12 morning1
            // 12-18 afternoon1
            // 18-21 evening1
            // 21-24 night1
            //
            // So for a 12hour time, we have:
            //
            // 12  1  2  3  4  5  6  7  8  9 10 11
            //  n  n  n  n  n  n  m  m  m  m  m  m
            //  a  a  a  a  a  a  e  e  e  n  n  n

            // Formatting has looser collision rules, because it is always paired with a time.
            // That is, it is not a problem if two items collide,
            // if it doesn't cause a collision when paired with a time.
            // But if 11:00 has the same format (eg 11 X) as 23:00, there IS a collision.
            // So we see if there is an overlap mod 12.

            { Type.format, DayPeriod.night1, Type.format, DayPeriod.morning1, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.night1, Type.format, DayPeriod.afternoon1, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.night1, Type.format, DayPeriod.evening1, Subtype.none },

            { Type.format, DayPeriod.morning1, Type.format, DayPeriod.afternoon1, Subtype.none },
            { Type.format, DayPeriod.morning1, Type.format, DayPeriod.evening1, Subtype.dateSymbolCollision },

            { Type.format, DayPeriod.afternoon1, Type.format, DayPeriod.evening1, Subtype.none },

            // Selection has stricter collision rules, because is is used to select different messages.
            // So two types with the same localization do collide unless they have exactly the same rules.
            // We use chr to test the "unless they have exactly the same rules" below.

            { Type.selection, DayPeriod.morning1, Type.selection, DayPeriod.night1, Subtype.dateSymbolCollision },
            { Type.selection, DayPeriod.morning1, Type.selection, DayPeriod.afternoon1, Subtype.dateSymbolCollision },

            { Type.selection, DayPeriod.morning1, Type.selection, DayPeriod.am, Subtype.none }, // morning1 and am is allowable
            { Type.selection, DayPeriod.morning1, Type.selection, DayPeriod.pm, Subtype.dateSymbolCollision },

            { "fr" },

            // nothing collides with itself
            { Type.format, DayPeriod.night1, Type.format, DayPeriod.night1, Subtype.none },
            { Type.format, DayPeriod.morning1, Type.format, DayPeriod.morning1, Subtype.none },
            { Type.format, DayPeriod.afternoon1, Type.format, DayPeriod.afternoon1, Subtype.none },
            { Type.format, DayPeriod.evening1, Type.format, DayPeriod.evening1, Subtype.none },

            // French has different rules
            // 00-04   night1
            // 04-12   morning1
            // 12-18   afternoon1
            // 18-00   evening1
            //
            // So for a 12hour time, we have:
            //
            // 12  1  2  3  4  5  6  7  8  9 10 11
            //  n  n  n  n  m  m  m  m  m  m  m  m
            //  a  a  a  a  a  a  e  e  e  e  e  e

            { Type.format, DayPeriod.night1, Type.format, DayPeriod.morning1, Subtype.none },
            { Type.format, DayPeriod.night1, Type.format, DayPeriod.afternoon1, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.night1, Type.format, DayPeriod.evening1, Subtype.none },

            { Type.format, DayPeriod.morning1, Type.format, DayPeriod.afternoon1, Subtype.dateSymbolCollision },
            { Type.format, DayPeriod.morning1, Type.format, DayPeriod.evening1, Subtype.dateSymbolCollision },

            { Type.format, DayPeriod.afternoon1, Type.format, DayPeriod.evening1, Subtype.none },

            { "chr" },
            // Chr lets use test that same rules don't collide in selection
            // <dayPeriodRule type="morning1" from="0:00" before="12:00" />
            // <dayPeriodRule type="noon" at="12:00" />
            // <dayPeriodRule type="afternoon1" after="12:00" before="24:00" />
            { Type.selection, DayPeriod.morning1, Type.selection, DayPeriod.am, Subtype.none },
            { Type.selection, DayPeriod.afternoon1, Type.selection, DayPeriod.pm, Subtype.none },
        };
        CLDRFile testFile = null;
        for (Object[] test : tests) {
            // set locale
            if (test.length == 1) {
                if (testFile != null) {
                    logln("");
                }
                testFile = new CLDRFile(new SimpleXMLSource((String) test[0]));
                testFactory.addFile(testFile);
                continue;
            }
            final DayPeriodInfo.Type type1 = (Type) test[0];
            final DayPeriodInfo.DayPeriod period1 = (DayPeriod) test[1];
            final DayPeriodInfo.Type type2 = (Type) test[2];
            final DayPeriodInfo.DayPeriod period2 = (DayPeriod) test[3];
            final Subtype expectedSubtype = (Subtype) test[4];

            final String path1 = prefix + type1.pathValue + infix + period1 + suffix;
            final String path2 = prefix + type2.pathValue + infix + period2 + suffix;

            testFile.add(path1, collidingValue);
            testFile.add(path2, collidingValue);

            CheckCLDR c = new CheckDates(testFactory);
            c.setCldrFileToCheck(testFile, options, result);

            result.clear();
            c.check(path1, path1, collidingValue, options, result);
            Subtype actualSubtype = Subtype.none;
            String message = null;
            for (CheckStatus status : result) {
                actualSubtype = status.getSubtype();
                message = status.getMessage();
                break;
            }
            assertEquals(testFile.getLocaleID() + " " + type1 + "/" + period1 + " vs " + type2 + "/" + period2
                + (message == null ? "" : " [" + message + "]"), expectedSubtype, actualSubtype);

            testFile.remove(path1);
            testFile.remove(path2);
        }
    }

    public void TestShowRowAction() {
        Map<Key,Pair<Boolean,String>> actionToExamplePath = new TreeMap<>();
        Counter<Key> counter = new Counter<>();

        for (String locale : Arrays.asList("jv", "fr", "vo")) {
            DummyPathValueInfo dummyPathValueInfo = new DummyPathValueInfo();
            dummyPathValueInfo.locale = CLDRLocale.getInstance(locale);
            CLDRFile cldrFile = testInfo.getCldrFactory().make(locale, true);
            CLDRFile cldrFileUnresolved = testInfo.getCldrFactory().make(locale, false);

            Set<PathHeader> sorted = new TreeSet<>();
            for (String path : cldrFile) {
                PathHeader ph = pathHeaderFactory.fromPath(path);
                sorted.add(ph);
            }

            for (Phase phase : Arrays.asList(Phase.SUBMISSION, Phase.VETTING)) {
                for (CheckStatus.Type status : Arrays.asList(CheckStatus.warningType, CheckStatus.errorType)) {
                    dummyPathValueInfo.checkStatusType = status;

                    for (PathHeader ph : sorted) {
                        String path = ph.getOriginalPath();

                        //String phString = ph.toString();
                        SurveyToolStatus surveyToolStatus = ph.getSurveyToolStatus();
                        dummyPathValueInfo.xpath = path;
                        dummyPathValueInfo.lastReleaseValue = cldrFileUnresolved.getStringValue(path);
                        StatusAction action = phase.getShowRowAction(
                            dummyPathValueInfo, 
                            InputMethod.DIRECT, 
                            surveyToolStatus, 
                            dummyUserInfo);

                        if (surveyToolStatus == SurveyToolStatus.HIDE) {
                            assertEquals("HIDE ==> FORBID_READONLY", StatusAction.FORBID_READONLY, action);
                        } else if (CheckCLDR.LIMITED_SUBMISSION) {
                            if (status == CheckStatus.Type.Error) {
                                assertEquals("ERROR ==> ALLOW", StatusAction.ALLOW, action);
                            } else if (locale.equalsIgnoreCase("vo")) {
                                assertEquals("vo ==> FORBID_READONLY", StatusAction.FORBID_READONLY, action);
                            } else if (dummyPathValueInfo.lastReleaseValue == null) {
                                assertEquals("missing ==> ALLOW", StatusAction.ALLOW, action);
                            }
                        }

                        if (isVerbose()) {
                            Key key = new Key(locale, phase, status, surveyToolStatus, action);
                            counter.add(key,1);

                            if (!actionToExamplePath.containsKey(key)) {
                                // for debugging
                                if (locale.equals("vo") && action == StatusAction.ALLOW) {
                                    StatusAction action2 = phase.getShowRowAction(
                                        dummyPathValueInfo, 
                                        InputMethod.DIRECT, 
                                        surveyToolStatus, 
                                        dummyUserInfo);
                                }
                                actionToExamplePath.put(key, Pair.of(dummyPathValueInfo.lastReleaseValue != null, path));
                            }
                        }
                    }
                }
            }
        }
        if (isVerbose()) {
            for (Entry<Key, Pair<Boolean, String>> entry : actionToExamplePath.entrySet()) {
                System.out.print("\n" + entry.getKey() + "\t" + entry.getValue().getFirst() + "\t" + entry.getValue().getSecond());
            }
            System.out.println();
            for (R2<Long, Key> entry : counter.getEntrySetSortedByCount(false, null)) {
                System.out.println(entry.get0() + "\t" + entry.get1());
            }
        }
    }

    static class Key extends R5<Phase, CheckStatus.Type, SurveyToolStatus, StatusAction, String> {
        public Key(String locale, Phase phase, CheckStatus.Type status, SurveyToolStatus stStatus, StatusAction action) {
            super(phase, status, stStatus, action, locale);
        }
        @Override
        public String toString() {
            return  get0() + "\t" + get1() + "\t" + get2() + "\t" + get3() + "\t" + get4();
        }
    }

//    private static CLDRURLS URLS = testInfo.urls();

    private static final PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(testInfo.getEnglish());

//    private static final CoverageInfo coverageInfo = new CoverageInfo(testInfo.getSupplementalDataInfo());

    private static final VoterInfo dummyVoterInfo = new VoterInfo(Organization.cldr, 
        org.unicode.cldr.util.VoteResolver.Level.vetter, 
        "somename");

    private static final UserInfo dummyUserInfo = new UserInfo() {
        public VoterInfo getVoterInfo() {
            return dummyVoterInfo;
        }
    };

    private static class DummyPathValueInfo implements PathValueInfo {
        private CLDRLocale locale;
        private String xpath;
        private String lastReleaseValue;
        private CheckStatus.Type checkStatusType;

        private CandidateInfo candidateInfo = new CandidateInfo() {
            @Override
            public String getValue() {
                return null;
            }
            @Override
            public Collection<UserInfo> getUsersVotingOn() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<CheckStatus> getCheckStatusList() {
                return checkStatusType == null ? Collections.emptyList()
                    : Collections.singletonList(new CheckStatus().setMainType(checkStatusType));
            }

        };

        public Collection<? extends CandidateInfo> getValues() {
            throw new UnsupportedOperationException();
        }
        public CandidateInfo getCurrentItem() {
            return candidateInfo;
        }
        public String getLastReleaseValue() {
            return lastReleaseValue;
        }
        public Level getCoverageLevel() {
            return Level.MODERN;
        }
        public boolean hadVotesSometimeThisRelease() {
            throw new UnsupportedOperationException();
        }
        public CLDRLocale getLocale() {
            return locale;
        }
        public String getXpath() {
            return xpath;
        }
    }
}
