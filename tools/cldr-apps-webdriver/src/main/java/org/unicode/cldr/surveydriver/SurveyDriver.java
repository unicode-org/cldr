package org.unicode.cldr.surveydriver;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Perform automated testing of the CLDR Survey Tool using Selenium WebDriver.
 *
 * <p>This test has been used with the cldr-apps-webdriver project running in IntelliJ. At the same
 * time, cldr-apps can be running either on localhost or on SmokeTest.
 *
 * <p>This code requires installing an implementation of WebDriver, such as chromedriver for Chrome.
 * On macOS, chromedriver can be installed from Terminal with brew as follows: brew install
 * chromedriver Then, right-click chromedriver, choose Open, and authorize to avoid the macOS error,
 * "“chromedriver” cannot be opened because the developer cannot be verified". Press Ctrl+C to stop
 * this instance of chromedriver.
 *
 * <p>(Testing with geckodriver for Firefox was unsuccessful, but has not been tried recently.)
 *
 * <p>Go to <a href="https://www.selenium.dev/downloads/">selenium.dev/downloads</a> and scroll down
 * to "Selenium Server (Grid)" and follow the link to download a file like
 * selenium-server-4.16.1.jar and save it in the parent directory of cldr-apps-webdriver.
 *
 * <p>Start selenium grid:
 *
 * <p>cd cldr-apps-webdriver/scripts; sh selenium-grid-start.sh
 *
 * <p>Open this file (SurveyDriver.java) in IntelliJ, right-click cldr-apps-webdriver in the Project
 * panel, and choose "Debug All Tests". You can do this repeatedly to start multiple browsers with
 * simulated vetters vetting at the same time.
 */
public class SurveyDriver {

    static void assertTrue(boolean t) {
        if (!t) throw new RuntimeException("Expected true but got false");
    }

    private static final String BASE_PATH = "/cldr-apps/";
    private static final String GLYPHICON_LOG_OUT = "glyphicon-log-out";
    private static final String ANT_NOTIFICATION_NOTICE_CLOSE = "ant-notification-notice-close";
    private static final String TD_CLASS_LOSING = "d-win othercell";
    private static final String TD_CLASS_WIN = "d-win proposedcell";
    private static final String TD_CLASS_ABSTAIN = "nocell";

    /*
     * Enable/disable specific tests using these booleans
     *
     * Most of these are the original (non sanity test) tests
     */
    static final boolean TEST_VETTING_TABLE = false;
    static final boolean TEST_FAST_VOTING = false;
    static final boolean TEST_LOCALES_AND_PAGES = false;
    static final boolean TEST_ANNOTATION_VOTING = false;
    static final boolean TEST_XML_UPLOADER = false;
    static final boolean TEST_DASHBOARD = false;
    // the sanity test
    static final boolean TEST_SANITY = true;

    /*
     * Configure for Survey Tool server, which can be localhost, cldr-smoke, cldr-staging, ...
     */
    static final String BASE_URL = SurveyDriverCredentials.getUrl() + BASE_PATH;
    // static final String BASE_URL = "https://cldr-smoke.unicode.org/cldr-apps/";
    // static final String BASE_URL = "https://cldr-staging.unicode.org/cldr-apps/";

    static final long TIME_OUT_SECONDS = SurveyDriverCredentials.getTimeOut();
    static final long TIME_OUT_QUICK_SECONDS = SurveyDriverCredentials.getQuickTimeOut();
    static final long SLEEP_MILLISECONDS = 100;

    /*
     * If USE_REMOTE_WEBDRIVER is true, then the driver will be a RemoteWebDriver (a class that implements
     * the WebDriver interface). Otherwise, the driver could be a ChromeDriver, or FirefoxDriver, EdgeDriver,
     * or SafariDriver (all subclasses of RemoteWebDriver) if we add options for those.
     * While much of the code in this class works either way, Selenium Grid needs the driver to be a
     * RemoteWebDriver and requires installation, see above comment about selenium-grid-start.sh
     */
    static final boolean USE_REMOTE_WEBDRIVER = true;
    static final String REMOTE_WEBDRIVER_URL = SurveyDriverCredentials.getWebdriverUrl();

    private static final String EMOJI_SKIP = ":arrow_lower_right:";
    private static final String EMOJI_FAIL = ":x:";
    private static final String EMOJI_PASS = ":white_check_mark:";

    public WebDriver driver;

    /** longer wait such as ST startup */
    public WebDriverWait wait;

    /** shorter wait such as page flipping */
    public WebDriverWait quickWait;

    private SessionId sessionId = null;

    /**
     * A nonnegative integer associated with a particular simulated user of Survey Tool, and with a
     * particular instance of a browser in the selenium grid. The user's email address will be like
     * "driver-123@cldr-apps-webdriver.org", where 123 would be the userIndex.
     */
    private int userIndex = 0; // possibly changed below, see getUserIndexFromGrid

    private boolean gotComprehensiveCoverage = false;

    public static void main(String args[]) {
        if(!runTests()) {
            throw new RuntimeException("Failed."); 
        }
    }

    public File takeSnapShot(final String imageComment) {
        if (!SurveyDriverCredentials.haveOutputDir()) {
            System.err.println("No screenshot (set WEBDRIVER_OUTPUT) - " + imageComment + ".jpg");
            return null;
        }
        int n = 0;
        try {
            File outputFile = SurveyDriverCredentials.getScreenshotFile(imageComment + ".jpg");
            while (outputFile.exists() && n < 20) {
                outputFile =
                        SurveyDriverCredentials.getScreenshotFile(imageComment + "-" + n + ".jpg");
                n++;
            }
            File tmpFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(tmpFile.toPath(), outputFile.toPath());
            outputFile.toPath();
            SurveyDriverLog.printlnSummary("- :tv: Screenshot: " + outputFile.toString());
            return outputFile;
        } catch (IOException ioe) {
            SurveyDriverLog.println(ioe);
            return null;
        }
    }

    public static void printSection(String s) {
        SurveyDriverLog.printlnSummary("");
        SurveyDriverLog.printlnSummary("## " + s);
        SurveyDriverLog.printlnSummary("");
    }

    public static void printSubSection(String s) {
        SurveyDriverLog.printlnSummary("");
        SurveyDriverLog.printlnSummary("### " + s);
        SurveyDriverLog.printlnSummary("");
    }

    public static void printSubSubSection(String s) {
        SurveyDriverLog.printlnSummary("");
        SurveyDriverLog.printlnSummary("#### " + s);
        SurveyDriverLog.printlnSummary("");
    }

    /** redirect this as it will be commonly used */
    public static final void printlnSummary(String s, Object... argsObjects) {
        if (argsObjects != null) {
            SurveyDriverLog.printlnSummary(String.format(s, argsObjects));
        } else {
            SurveyDriverLog.printlnSummary(s);
        }
    }

    public static String getResult(boolean pass) {
        return "Result: " + (pass ? EMOJI_PASS + " PASS" : EMOJI_FAIL + " FAIL");
    }

    public static boolean runTests() {
        boolean result = new SurveyDriver().runAllTests();
        SurveyDriverLog.printlnSummary("#### " + getResult(result));
        return result;
    }

    public boolean runAllTests() {
        SurveyDriverLog.printlnSummary("# Web Driver starting");

        setUp();
        takeSnapShot("setup-complete");

        try {
            if (TEST_VETTING_TABLE) {
                printSection("Test Vetting Table");
                assertTrue(SurveyDriverVettingTable.testVettingTable(this));
            }
            if (TEST_FAST_VOTING) {
                printSection("Test Fast Voting");
                assertTrue(testFastVoting());
            }
            if (TEST_LOCALES_AND_PAGES) {
                printSection("Test Locales and Pages");
                assertTrue(testAllLocalesAndPages());
            }
            if (TEST_ANNOTATION_VOTING) {
                printSection("Test Annotation Voting");
                assertTrue(testAnnotationVoting());
            }
            if (TEST_XML_UPLOADER) {
                printSection("Test XML Uploader");
                assertTrue(new SurveyDriverXMLUploader(this).testXMLUploader());
            }
            if (TEST_DASHBOARD) {
                printSection("Test Dashboard");
                assertTrue(new SurveyDriverDashboard(this).test());
            }
            if (TEST_SANITY) {
                return testSanity();
            }
        } catch (Throwable t) {
            SurveyDriverLog.println(t);
            return false;
        } finally {
            tearDown();
            printSection("Web Driver Stopping");
            SurveyDriverCredentials.finalizeOutputDir(); // so output is all world readable
        }
        return true;
    }

    /** Set up the driver and its "wait" object. */
    private void setUp() {
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);

        ChromeOptions options = new ChromeOptions();
        options.setCapability("goog:loggingPrefs", logPrefs);
        // options.addArguments("start-maximized"); // this works, but inconvenient especially if
        // more than one
        // options.addArguments("auto-open-devtools-for-tabs"); // this works, but makes window too
        // narrow
        if (USE_REMOTE_WEBDRIVER) {
            printlnSummary("- getting remote session");
            System.out.println("Getting remote session from " + REMOTE_WEBDRIVER_URL);
            try {
                driver = new RemoteWebDriver(new URL(REMOTE_WEBDRIVER_URL + "/wd/hub"), options);
            } catch (MalformedURLException e) {
                SurveyDriverLog.println(e);
            }
            sessionId = ((RemoteWebDriver) driver).getSessionId();
            SurveyDriverLog.println(
                    "Session id = " + sessionId); // e.g., 9c0d7d317d64cb53b6eaefc70427d4d8
        } else {
            driver = new ChromeDriver(options);
        }
        wait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(TIME_OUT_SECONDS),
                        Duration.ofMillis(SLEEP_MILLISECONDS));
        quickWait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(TIME_OUT_QUICK_SECONDS),
                        Duration.ofMillis(SLEEP_MILLISECONDS));
        if (USE_REMOTE_WEBDRIVER) {
            userIndex = getUserIndexFromGrid(sessionId);
        }
        printlnSummary("- Got user. userIndex = " + userIndex + ", " + driver.toString());
    }

    /** Clean up when finished testing. */
    private void tearDown() {
        SurveyDriverLog.println(
                "cldr-apps-webdriver is quitting, goodbye from sessionId " + sessionId);
        if (driver != null) {
            /*
             * This five-second sleep may not always be appropriate. It can help to see the browser for a few seconds
             * before it closes. Alternatively a breakpoint can be set on driver.quit() for the same purpose.
             */
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                SurveyDriverLog.println("Sleep interrupted before driver.quit; " + e);
            }
            driver.quit();
            driver = null; // so we only quit once
        }
    }

    /**
     * Test "fast" voting, that is, voting for several items on a page, and measuring the time of
     * response.
     *
     * <p>Purposes: (1) study the sequence of events, especially client-server traffic, when
     * multiple voting events (maybe by a single user) are being handled; (2) simulate simultaneous
     * input from multiple vetters, for integration and performance testing under high load.
     */
    private boolean testFastVoting() {
        if (!login()) {
            return false;
        }
        /*
         * TODO: configure the locale and page on a per-slot basis, to enable multiple simulated
         * users to be voting in multiple locales and/or pages.
         */
        String loc = "sr";
        String page = "Languages_A_D";
        String url = BASE_URL + "v#/" + loc + "/" + page;

        /*
         * Repeat the test a large number of times.
         * Eventually we'll have more sophisticated criteria for when to stop.
         */
        final int REPETITION_COUNT = 10000;
        for (int i = 0; i < REPETITION_COUNT; i++) {
            SurveyDriverLog.println("testFastVoting i = " + i);
            try {
                if (!testFastVotingInner(page, url)) {
                    return false;
                }
            } catch (StaleElementReferenceException e) {
                /*
                 * Formerly sometimes we got "org.openqa.selenium.StaleElementReferenceException:
                 * stale element reference: element is not attached to the page document".
                 * It may have been due to ajax response causing the table to be rebuilt.
                 * Survey Tool has since been revised so that the table is rebuilt less often.
                 * If this exception becomes problematic, we should investigate ways to catch it
                 * and continue wherever it occurs.
                 */
                SurveyDriverLog.println(
                        "Continuing main loop after StaleElementReferenceException, i = " + i);
            }
        }
        SurveyDriverLog.println("✅ Fast vote test passed for " + loc + ", " + page);
        return true;
    }

    private boolean testFastVotingInner(String page, String url) {
        driver.get(url);
        /*
         * Wait for the correct title, and then wait for the div
         * whose id is "LoadingMessageSection" to get the style "display: none".
         */
        if (!waitForTitle(page, url)) {
            return false;
        }
        if (!waitUntilLoadingMessageDone(url)) {
            return false;
        }
        if (!hideLeftSidebar(url)) {
            return false;
        }
        if (!waitUntilElementInactive("left-sidebar", url)) {
            return false;
        }
        /*
         * TODO: handle "overlay" element more robustly. While this mostly works, the overlay can
         * pop up again when you least expect it, causing, for example:
         *
         * org.openqa.selenium.WebDriverException: unknown error: Element <input ...
         * title="click to vote" value="الأدانجمية"> is not clickable at point (746, 429).
         * Other element would receive the click: <div id="overlay" class="" style="z-index: 1000;"></div>
         */
        if (!waitUntilElementInactive("overlay", url)) {
            return false;
        }
        if (!gotComprehensiveCoverage && !chooseComprehensiveCoverage(url)) {
            return false;
        }
        gotComprehensiveCoverage = true;

        double firstClickTime = 0;

        /*
         * For the first four rows, click on the Abstain (nocell) button.
         * Then, for the first three rows, click on the Winning (proposedcell) button.
         * Then, for the fourth row, click on the Add (addcell) button and enter a new value.
         * TODO: instead of hard-coding these hexadecimal row ids, specify the first four
         * "tr" elements of the main table using the appropriate findElement(By...).
         * Displayed rows depend on coverage level, which in turn depends on the user; if we're logged
         * in as Admin, then we see Comprehensive; if not logged in, we see Modern (and we can't vote).
         * Something seems to have changed between versions 34 and 35; now first four rows are:
         *     Abkhazian ► ab	row_f3d4397b739b287
         * 	   Achinese ► ace	row_6899b21f19eef8cc
         * 	   Acoli ► ach		row_1660459cc74c9aec
         * 	   Adangme ► ada	row_7d1d3cbd260601a4
         * Acoli appears to be a new addition.
         */
        String[] rowIds = {
            "f3d4397b739b287", "6899b21f19eef8cc", "1660459cc74c9aec", "7d1d3cbd260601a4"
        };
        String[] cellClasses = {"nocell", "proposedcell"};
        final boolean verbose = true;
        for (String cell : cellClasses) {
            for (int i = 0; i < rowIds.length; i++) {
                String rowId = "row_" + rowIds[i];
                boolean doAdd = (i == rowIds.length - 1) && cell.equals("proposedcell");
                String tagName = doAdd ? "button" : "input";
                String cellClass = doAdd ? "addcell" : cell;
                WebElement rowEl = null, columnEl, clickEl = null;
                int repeats = 0;
                if (verbose) {
                    String op = cell.equals("nocell") ? "Abstain" : "Vote";
                    SurveyDriverLog.println(op + " row " + (i + 1) + " (" + rowId + ")");
                }
                for (; ; ) {
                    try {
                        rowEl = driver.findElement(By.id(rowId));
                    } catch (StaleElementReferenceException e) {
                        if (++repeats > 4) {
                            break;
                        }
                        SurveyDriverLog.println(
                                "Continuing after StaleElementReferenceException for findElement by id rowId "
                                        + rowId
                                        + " for "
                                        + url);
                        continue;
                    } catch (Exception e) {
                        SurveyDriverLog.println(e);
                        break;
                    }
                    if (rowEl == null) {
                        SurveyDriverLog.println(
                                "❌ Fast vote test failed, missing row id " + rowId + " for " + url);
                        return false;
                    }
                    try {
                        columnEl = rowEl.findElement(By.className(cellClass));
                    } catch (StaleElementReferenceException e) {
                        if (++repeats > 4) {
                            break;
                        }
                        SurveyDriverLog.println(
                                "Continuing after StaleElementReferenceException for findElement by class cellClass "
                                        + cellClass
                                        + " for "
                                        + url);
                        continue;
                    } catch (Exception e) {
                        SurveyDriverLog.println(e);
                        break;
                    }
                    if (columnEl == null) {
                        SurveyDriverLog.println(
                                "❌ Fast vote test failed, no column "
                                        + cellClass
                                        + " for row "
                                        + rowId
                                        + " for "
                                        + url);
                        return false;
                    }
                    try {
                        clickEl = columnEl.findElement(By.tagName(tagName));
                    } catch (StaleElementReferenceException e) {
                        if (++repeats > 4) {
                            break;
                        }
                        SurveyDriverLog.println(
                                "Continuing after StaleElementReferenceException for findElement by tagName "
                                        + rowId
                                        + " for "
                                        + url);
                        continue;
                    } catch (Exception e) {
                        SurveyDriverLog.println(e);
                        break;
                    }
                    break;
                }
                if (clickEl == null) {
                    SurveyDriverLog.println(
                            "❌ Fast vote test failed, no tag "
                                    + tagName
                                    + " for row "
                                    + rowId
                                    + " for "
                                    + url);
                    return false;
                }
                clickEl =
                        waitUntilRowCellTagElementClickable(
                                clickEl, rowId, cellClass, tagName, url);
                if (clickEl == null) {
                    return false;
                }
                try {
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("overlay")));
                } catch (Exception e) {
                    SurveyDriverLog.println(e);
                    SurveyDriverLog.println(
                            "❌ Fast vote test failed, invisibilityOfElementLocated overlay for row "
                                    + rowId
                                    + " for "
                                    + url);
                    return false;
                }
                if (firstClickTime == 0.0) {
                    firstClickTime = System.currentTimeMillis();
                }
                try {
                    clickOnRowCellTagElement(clickEl, rowId, cellClass, tagName, url);
                } catch (StaleElementReferenceException e) {
                    if (++repeats > 4) {
                        break;
                    }
                    SurveyDriverLog.println(
                            "Continuing after StaleElementReferenceException for clickEl.click for row "
                                    + rowId
                                    + " for "
                                    + url);
                    continue;
                } catch (Exception e) {
                    SurveyDriverLog.println(e);
                    SurveyDriverLog.println(
                            "❌ Fast vote test failed, clickEl.click for row "
                                    + rowId
                                    + " for "
                                    + url);
                    return false;
                }
                if (doAdd) {
                    try {
                        /*
                         * Problem here: waitInputBoxAppears can get StaleElementReferenceException five times,
                         * must be for rowEl which isn't re-gotten. For now at least, just continue loop if
                         * waitInputBoxAppears returns null.
                         */
                        WebElement inputEl = waitInputBoxAppears(rowEl, url);
                        if (inputEl == null) {
                            SurveyDriverLog.println(
                                    "Warning: continuing, didn't see input box for " + url);
                            continue;
                        }
                        inputEl =
                                waitUntilRowCellTagElementClickable(
                                        inputEl, rowId, cellClass, "input", url);
                        if (inputEl == null) {
                            SurveyDriverLog.println(
                                    "Warning: continuing, input box not clickable for " + url);
                            continue;
                        }
                        inputEl.clear();
                        inputEl.click();
                        inputEl.sendKeys("Testxyz");
                        inputEl.sendKeys(Keys.RETURN);
                    } catch (WebDriverException e) {
                        SurveyDriverLog.println(
                                "Continuing after WebDriverException for doAdd for row "
                                        + rowId
                                        + " for "
                                        + url);
                    }
                }
            }
        }
        /*
         * Wait for tr_checking2 element (temporary green background) NOT to exist.
         * The temporary green background indicates that the client is waiting for a response
         * from the server before it can update the display to show the results of a
         * completed voting operation.
         */
        if (!waitUntilClassExists("tr_checking2", false, url)) {
            return false;
        }
        double deltaTime = System.currentTimeMillis() - firstClickTime;
        SurveyDriverLog.println(
                "Total time elapsed since first click = " + deltaTime / 1000.0 + " sec");
        return true;
    }

    /** Log into Survey Tool. */
    public boolean login() {
        final String url = BASE_URL;
        SurveyDriverLog.println("Logging in to " + url + " as #" + userIndex);
        driver.get(url);
        /*
         * Wait for the correct title, and then wait for the div
         * whose id is "LoadingMessageSection" to get the style "display: none".
         */
        String page = "Locale List";
        if (!waitForTitle(page, url)) {
            return false;
        }
        if (!waitUntilLoadingMessageDone(url)) {
            return false;
        }
        if (haveLogoutButton()) {
            if (!doLogout()) {
                return false;
            }
        }
        if (!loginWithButton(url)) {
            return false;
        }
        /*
         * To make sure we're really logged in, find an element with class "glyphicon-user".
         */
        if (!waitUntilClassExists("glyphicon-user", true, url)) {
            SurveyDriverLog.println("❌ Login failed, glyphicon-user icon never appeared in " + url);
            return false;
        }
        return true;
    }

    private boolean haveLogoutButton() {
        return haveElementWithClass(GLYPHICON_LOG_OUT);
    }

    private boolean doLogout() {
        printlnSummary("- logging out…");
        if (!clickButtonByXpath(
                "//span[@class='glyphicon " + GLYPHICON_LOG_OUT + "']", driver.getCurrentUrl())) {
            return false;
        }
        // wait until logout button is no longer there
        return waitUntilClassExists(GLYPHICON_LOG_OUT, false, driver.getCurrentUrl());
    }

    private boolean loginWithButton(String url) {
        final String loginXpath = "//span[text()='Log In']";
        final String usernameXpath = "//input[@placeholder='Username']";
        final String passwordXpath = "//input[@placeholder='Password']";
        final SurveyDriverCredentials cred = SurveyDriverCredentials.getForUser(userIndex);
        if (!clickButtonByXpath(loginXpath, url)) {
            return false;
        }
        if (!inputTextByXpath(usernameXpath, cred.getEmail(), url)) {
            return false;
        }
        if (!inputTextByXpath(passwordXpath, cred.getPassword(), url)) {
            return false;
        }
        return clickButtonByXpath(loginXpath, url);
    }

    public boolean clickButtonByXpath(String xpath, String url) {
        WebElement el = getClickableElementByXpath(xpath, url);
        if (el == null) {
            return false;
        }
        try {
            el.click();
        } catch (Exception e) {
            SurveyDriverLog.println("Exception caught while trying to click " + xpath + " element");
            SurveyDriverLog.println(e);
            return false;
        }
        return true;
    }

    private boolean inputTextByXpath(String xpath, String text, String url) {
        WebElement el = getClickableElementByXpath(xpath, url);
        if (el == null) {
            return false;
        }
        try {
            el.clear();
            el.click();
            el.sendKeys(text);
            el.sendKeys(Keys.RETURN);
        } catch (Exception e) {
            SurveyDriverLog.println("Exception caught while trying to input text " + text);
            SurveyDriverLog.println(e);
            return false;
        }
        return true;
    }

    private WebElement getClickableElementByXpath(String xpath, String url) {
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver -> driver.findElement(By.xpath(xpath)) != null);
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, timed out waiting for element to be found by xpath "
                            + xpath
                            + " in url "
                            + url);
            return null;
        }
        WebElement el;
        try {
            el = driver.findElement(By.xpath(xpath));
            wait.until(ExpectedConditions.elementToBeClickable(el));
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, timed out waiting for "
                            + xpath
                            + " button to be clickable in "
                            + url);
            return null;
        }
        return el;
    }

    /**
     * Choose "Comprehensive" from the "Coverage" menu
     *
     * @param url the current URL, only for error message
     * @return true for success, false for failure
     */
    private boolean chooseComprehensiveCoverage(String url) {
        String id = "coverageLevel";
        WebElement menu = driver.findElement(By.id(id));
        if (!waitUntilElementClickable(menu, url)) {
            return false;
        }
        try {
            menu.click();
        } catch (Exception e) {
            SurveyDriverLog.println("Exception caught while trying to open Coverage menu");
            SurveyDriverLog.println(e);
            return false;
        }
        /*
         * <option value="comprehensive" data-v-47405740="">Comprehensive</option>
         */
        WebElement item = menu.findElement(By.cssSelector("option[value='comprehensive']"));
        if (!waitUntilElementClickable(item, url)) {
            return false;
        }
        try {
            item.click();
        } catch (Exception e) {
            SurveyDriverLog.println(
                    "Exception caught while trying to choose Comprehensive from Coverage menu");
            SurveyDriverLog.println(e);
            return false;
        }
        return true;
    }

    /** Test all the locales and pages we're interested in. */
    private boolean testAllLocalesAndPages() {
        String[] locales = SurveyDriverData.getLocales();
        String[] pages = SurveyDriverData.getPages();
        /*
         * Reference: https://unicode.org/cldr/trac/ticket/11238 "browser console shows error message,
         * there is INHERITANCE_MARKER without inheritedValue"
         */
        String searchString =
                "INHERITANCE_MARKER without inheritedValue"; // formerly, "there is no Bailey Target
        // item"

        for (String loc : locales) {
            // for (PathHeader.PageId page : PathHeader.PageId.values()) {
            for (String page : pages) {
                if (!testOneLocationAndPage(loc, page, searchString)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Test the given locale and page.
     *
     * @param loc the locale string, like "pt_PT"
     * @param page the page name, like "Alphabetic_Information"
     * @return true if all parts of the test pass, else false
     */
    private boolean testOneLocationAndPage(String loc, String page, String searchString) {
        String url = BASE_URL + "v#/" + loc + "/" + page;
        driver.get(url);

        /*
         * Wait for the correct title, and then wait for the div
         * whose id is "LoadingMessageSection" to get the style "display: none".
         */
        if (!waitForTitle(page, url)) {
            return false;
        }
        if (!waitUntilLoadingMessageDone(url)) {
            return false;
        }
        int searchStringCount = countLogEntriesContainingString(searchString);
        if (searchStringCount > 0) {
            SurveyDriverLog.println(
                    "❌ Test failed: "
                            + searchStringCount
                            + " occurrences in log of '"
                            + searchString
                            + "' for "
                            + url);
            return false;
        }
        SurveyDriverLog.println(
                "✅ Test passed: zero occurrences in log of '"
                        + searchString
                        + "' for "
                        + loc
                        + ", "
                        + page);

        WebElement el = null;
        try {
            el = driver.findElement(By.id("row_44fca52aa81abcb2"));
        } catch (Exception ignored) {
        }
        if (el != null) {
            SurveyDriverLog.println("✅✅✅ Got it in " + url);
        }
        return true;
    }

    private boolean testAnnotationVoting() {
        /*
         * This list of locales was obtained by putting a breakpoint on SurveyAjax.getLocalesSet, getting its
         * return value, and adding quotation marks by search/replace.
         */
        String[] locales = SurveyDriverData.getLocales();
        String[] pages = SurveyDriverData.getAnnotationPages();

        /*
         * Reference: https://unicode.org/cldr/trac/ticket/11270
         * "Use floating point instead of integers for vote counts"
         * See VoteResolver.java
         *
         * Note: This searchString didn't actually occur in the browser console, only in the
         * Eclipse console, which is NOT detected by WebDriver. It didn't matter since I had
         * a breakpoint in Eclipse for the statement in question.
         */
        String searchString = "Rounding matters for useKeywordAnnotationVoting";
        long errorCount = 0;
        for (String loc : locales) {
            // for (PathHeader.PageId page : PathHeader.PageId.values()) {
            for (String page : pages) {
                if (!testOneLocationAndPage(loc, page, searchString)) {
                    /*
                     * We could return when we encounter an error.
                     * Or, we could keep going, to find more errors.
                     * Maybe there should be a global setting for that...
                     */
                    // return;
                    ++errorCount;
                }
            }
        }
        if (errorCount > 0) {
            SurveyDriverLog.println("❌ Test failed, total " + errorCount + " errors");
            return false;
        }
        return true;
    }

    /**
     * Count how many log entries contain the given string.
     *
     * @param searchString the string for which to search
     * @return the number of occurrences
     */
    private int countLogEntriesContainingString(String searchString) {
        int searchStringCount = 0;
        Logs logs = driver.manage().logs();
        for (String type : logs.getAvailableLogTypes()) {
            List<LogEntry> logEntries = logs.get(type).getAll();
            for (LogEntry entry : logEntries) {
                String message = entry.getMessage();
                if (message.contains(searchString)) {
                    SurveyDriverLog.println(entry.toString());
                    ++searchStringCount;
                }
            }
        }
        return searchStringCount;
    }

    /**
     * Wait for the title to contain the given string
     *
     * @param s the string expected to occur in the title
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitForTitle(String s, String url) {
        printlnSummary("- Waiting for title: %s", s);
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver ->
                                    (Objects.requireNonNull(webDriver).getTitle().contains(s)));
        } catch (TimeoutException e) {
            SurveyDriverLog.println(e);
            printlnSummary(
                    "- ❌ Test failed, timed out, waiting for title to contain %s in %s but was %s",
                    s, url, driver.getTitle());
            return false;
        }
        printlnSummary("  - OK: url = " + driver.getCurrentUrl());
        return true;
    }

    /**
     * Wait for the title to contain the given string
     *
     * @param s the string expected to occur in the title
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitForTitleToNotBe(String s, String url) {
        printlnSummary("- Waiting for title to not be: %s", s);
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver ->
                                    (!Objects.requireNonNull(webDriver).getTitle().contains(s)));
        } catch (TimeoutException e) {
            SurveyDriverLog.println(e);
            printlnSummary(
                    "- ❌ Test failed, timed out, waiting for title to not contain %s in %s but was %s",
                    s, url, driver.getTitle());
            return false;
        }
        printlnSummary("  - OK: url = " + driver.getCurrentUrl());
        return true;
    }

    /**
     * Wait for the div whose id is "LoadingMessageSection" to get the style "display: none".
     *
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitUntilLoadingMessageDone(String url) {
        String loadingId = "LoadingMessageSection";
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver ->
                                    Objects.requireNonNull(webDriver)
                                            .findElement(By.id(loadingId))
                                            .getCssValue("display")
                                            .contains("none"));
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "- ❌ Test failed, maybe timed out, waiting for " + loadingId + " in " + url);
            return false;
        }
        return true;
    }

    /**
     * Hide the element whose id is "left-sidebar", by simulating the appropriate mouse action if
     * it's visible.
     *
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean hideLeftSidebar(String url) {
        String id = "left-sidebar";
        WebElement bar = driver.findElement(By.id(id));
        if (bar.getAttribute("class").contains("active")) {
            /*
             * Move the mouse away from the left sidebar.
             * Mouse movements don't seem able to hide the left sidebar with WebDriver.
             * Even clicks on other elements don't work.
             * The only solution I've found is to add this line to redesign.js:
             *     $('#dragger').click(hideOverlayAndSidebar);
             * Then clicking on the edge of the sidebar closes it, and simulated click here works.
             */
            // SurveyDriverLog.println("Moving mouse, so to squeak...");
            String otherId = "dragger";
            WebElement otherEl = driver.findElement(By.id(otherId));
            if (!waitUntilElementClickable(otherEl, url)) {
                return false;
            }
            try {
                otherEl.click();
            } catch (Exception e) {
                SurveyDriverLog.println("Exception caught while moving mouse, so to squeak...");
                SurveyDriverLog.println(e);
                return false;
            }
        }
        return true;
    }

    /**
     * Wait for the element with given id to have class "active".
     *
     * @param id the id of the element
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitUntilElementActive(String id, String url) {
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver -> {
                                WebElement el =
                                        Objects.requireNonNull(webDriver).findElement(By.id(id));
                                return el != null && el.getAttribute("class").contains("active");
                            });
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, maybe timed out, waiting for " + id + " in " + url);
            return false;
        }
        return true;
    }

    /**
     * Wait for the element with given id NOT to have class "active".
     *
     * @param id the id of the element
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitUntilElementInactive(String id, String url) {
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver -> {
                                WebElement el =
                                        Objects.requireNonNull(webDriver).findElement(By.id(id));
                                return el == null || !el.getAttribute("class").contains("active");
                            });
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, maybe timed out, waiting for inactive id " + id + " in " + url);
            return false;
        }
        return true;
    }

    /**
     * Wait for an input element to appear inside the addcell in the given row element
     *
     * @param rowEl the row element that will contain the input element
     * @param url the url we're loading
     * @return the input element, or null for failure
     */
    public WebElement waitInputBoxAppears(WebElement rowEl, String url) {
        /*
         * Caution: a row may have more than one input element -- for example, the "radio" buttons are input elements.
         * First we need to find the addcell for this row, then find the input tag inside the addcell.
         * Note that the add button does NOT contain the input tag, but the addcell contains both the add button
         * and the input tag.
         */
        WebElement inputEl = null;
        int repeats = 0;
        for (; ; ) {
            try {
                WebElement addCell = rowEl.findElement(By.className("addcell"));
                inputEl =
                        wait.until(
                                ExpectedConditions.presenceOfNestedElementLocatedBy(
                                        addCell, By.tagName("input")));
                /*
                 * TODO: don't wait here for 30 seconds, as sometimes happens...
                 */
            } catch (StaleElementReferenceException e) {
                if (++repeats > 4) {
                    break;
                }
                SurveyDriverLog.println(
                        "waitInputBoxAppears repeating for StaleElementReferenceException");
                continue;
            } catch (Exception e) {
                /*
                 * org.openqa.selenium.TimeoutException: Expected condition failed: waiting for visibility of element located by By.tagName:
                 * input (tried for 30 second(s) with 100 milliseconds interval)
                 */
                SurveyDriverLog.println(e);
                SurveyDriverLog.println(
                        "❌ Test failed, maybe timed out, waiting for input in addcell in " + url);
            }
            break;
        }
        return inputEl;
    }

    /**
     * Wait until the element with the given class name is clickable, then click it.
     *
     * @param className the class name
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean clickButtonByClassName(String className, String url) {
        WebElement button = driver.findElement(By.className(className));
        if (!waitUntilElementClickable(button, url)) {
            return false;
        }
        try {
            button.click();
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, maybe timed out, waiting button with class "
                            + className
                            + " to be clickable in "
                            + url);
            return false;
        }
        return true;
    }

    /**
     * Wait until the element is clickable.
     *
     * @param el the element
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitUntilElementClickable(WebElement el, String url) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(el));
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, maybe timed out, waiting for "
                            + el
                            + " to be clickable in "
                            + url);
            return false;
        }
        return true;
    }

    public boolean waitUntilElementDisplayed(WebElement el, String url) {
        try {
            quickWait.until((ExpectedCondition<Boolean>) webDriver -> el.isDisplayed());
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, maybe timed out, waiting for "
                            + el
                            + " to be displayed in "
                            + url);
            return false;
        }
        return true;
    }

    /**
     * Wait until the element specified by rowId, cellClass, tagName is clickable.
     *
     * @param clickEl the element to wait for, if not stale
     * @param rowId the id for the row element
     * @param cellClass the class of the cell, typically "nocell", "proposedcell", or "addcell"
     * @param tagName typically "button" or "input"
     * @param url the url we're loading
     * @return the (possibly updated) clickEl for success, null for failure
     */
    public WebElement waitUntilRowCellTagElementClickable(
            WebElement clickEl, String rowId, String cellClass, String tagName, String url) {
        int repeats = 0;
        for (; ; ) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(clickEl));
                return clickEl;
            } catch (StaleElementReferenceException e) {
                if (++repeats > 4) {
                    break;
                }
                SurveyDriverLog.println(
                        "waitUntilRowCellTagElementClickable repeating for StaleElementReferenceException");
                WebElement rowEl = driver.findElement(By.id(rowId));
                WebElement columnEl = rowEl.findElement(By.className(cellClass));
                clickEl = columnEl.findElement(By.tagName(tagName));
            } catch (NoSuchElementException e) {
                if (++repeats > 4) {
                    break;
                }
                SurveyDriverLog.println(
                        "waitUntilRowCellTagElementClickable repeating for NoSuchElementException");
                WebElement rowEl = driver.findElement(By.id(rowId));
                WebElement columnEl = rowEl.findElement(By.className(cellClass));
                clickEl = columnEl.findElement(By.tagName(tagName));
            } catch (Exception e) {
                /*
                 * TODO: sometimes get here with org.openqa.selenium.NoSuchElementException
                 *       no such element: Unable to locate element: {"method":"tag name","selector":"input"}
                 * Called by testFastVotingInner with tagName = "input"
                 * Repeat in that case, similar to StaleElementReferenceException? Or do we need to
                 * repeat at a higher level, and/or re-get clickEl?
                 */
                SurveyDriverLog.println(e);
                break;
            }
        }
        SurveyDriverLog.println(
                "❌ Test failed in waitUntilRowCellTagElementClickable for "
                        + rowId
                        + ","
                        + cellClass
                        + ","
                        + tagName
                        + " in "
                        + url);
        return null;
    }

    /**
     * Click on the element specified by clickEl, rowId, cellClass, tagName.
     *
     * @param clickEl the element to click on, if not stale
     * @param rowId the id for the row element
     * @param cellClass the class of the cell, typically "nocell", "proposedcell", or "addcell"
     * @param tagName typically "button" or "input"
     * @param url the url we're loading
     */
    public void clickOnRowCellTagElement(
            WebElement clickEl, String rowId, String cellClass, String tagName, String url) {
        int repeats = 0;
        for (; ; ) {
            try {
                clickEl.click();
                return;
            } catch (StaleElementReferenceException e) {
                if (++repeats > 4) {
                    break;
                }
                SurveyDriverLog.println(
                        "clickOnRowCellTagElement repeating for StaleElementReferenceException for "
                                + rowId
                                + ","
                                + cellClass
                                + ","
                                + tagName
                                + " in "
                                + url);
                int recreateStringCount =
                        countLogEntriesContainingString(
                                "insertRows: recreating table from scratch");
                SurveyDriverLog.println(
                        "clickOnRowCellTagElement: log has "
                                + recreateStringCount
                                + " scratch messages");
                WebElement rowEl = driver.findElement(By.id(rowId));
                WebElement columnEl = rowEl.findElement(By.className(cellClass));
                clickEl = columnEl.findElement(By.tagName(tagName));
            } catch (Exception e) {
                SurveyDriverLog.println(e);
                break;
            }
        }
        SurveyDriverLog.println(
                "❗ Test failed in clickOnRowCellTagElement for "
                        + rowId
                        + ","
                        + cellClass
                        + ","
                        + tagName
                        + " in "
                        + url);
    }

    /**
     * Wait until an element with class with the given name exists, or wait until one doesn't.
     *
     * @param className the class name
     * @param checking true to wait until such an element exists, or false to wait until no such
     *     element exists
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitUntilClassExists(String className, boolean checking, String url) {
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver -> {
                                int elCount =
                                        Objects.requireNonNull(webDriver)
                                                .findElements(By.className(className))
                                                .size();
                                return checking ? (elCount > 0) : (elCount == 0);
                            });
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, maybe timed out, waiting for class "
                            + className
                            + (checking ? "" : " not")
                            + " to exist for "
                            + url);
            return false;
        }
        return true;
    }

    /**
     * Wait until an element with id with the given name exists, or wait until one doesn't.
     *
     * @param idName the id name
     * @param checking true to wait until such an element exists, or false to wait until no such
     *     element exists
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    public boolean waitUntilIdExists(String idName, boolean checking, String url) {
        try {
            wait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver -> {
                                int elCount =
                                        Objects.requireNonNull(webDriver)
                                                .findElements(By.id(idName))
                                                .size();
                                return checking ? (elCount > 0) : (elCount == 0);
                            });
        } catch (Exception e) {
            SurveyDriverLog.println(e);
            SurveyDriverLog.println(
                    "❌ Test failed, maybe timed out, waiting for id "
                            + idName
                            + (checking ? "" : " not")
                            + " to exist for "
                            + url);
            return false;
        }
        return true;
    }

    /**
     * Supposing there are n slots in the selenium grid, get a number in the range [0, ..., n - 1],
     * representing the particular slot we are using. This number will be used as the "user index"
     * identifying a unique Survey Tool simulated user, with a fictitious email address like
     * "driver-123@cldr-apps-webdriver.org", where 123 would be the user index.
     *
     * @param sessionId the session id associated with our slot
     * @return the user index, a nonnegative integer
     */
    private int getUserIndexFromGrid(SessionId sessionId) {
        String url = REMOTE_WEBDRIVER_URL + "/status"; // http://localhost:4444/status
        driver.get(url);
        String jsonString = driver.findElement(By.tagName("body")).getText();
        // SurveyDriverLog.println("jsonString = " + jsonString);

        // Ideally, at this point we could convert the json into a NodeStatus object
        // NodeStatus nodeStatus = NodeStatus.fromJson(jsonString);
        //
        // https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/grid/data/NodeStatus.html
        // Unfortunately it's not clear how to do that...
        // Another way would be with gson:
        //  SurveyDriverTestSession obj = new Gson().fromJson(jsonString,
        // SurveyDriverNodeStatus.class);
        // -- with our own imitation of NodeStatus, but that seems too difficult and error-prone.
        // Another way would be to forget about NodeStatus, and simply assign a different user index
        // each time we launch SurveyDriver, using our own mechanism independent of the grid...

        // Crude work-around: each slot has "lastStarted" (1970 if never), sometimes followed by
        // sessionId.
        // For example:
        // "lastStarted": "2023-12-21T03:24:37.017561Z",
        //   "sessionId": "19049a348716f4dd825e330c47787573",
        int uIndex = -1;
        String sessionIdString = sessionId.toString();
        int slotCount = 0;
        for (String line : jsonString.split("\\n")) {
            if (line.contains("lastStarted")) {
                ++slotCount;
            } else if (line.contains("sessionId") && line.contains(sessionIdString)) {
                uIndex = slotCount - 1;
                System.out.println("Setting user index to " + uIndex + "; based on " + url);
                break;
            }
        }
        if (uIndex < 0) {
            uIndex = new Random().nextInt(9);
            System.out.println(
                    "Setting user index randomly to "
                            + uIndex
                            + "; unable to determine from "
                            + url);
        }
        return uIndex;
    }

    boolean sanityLogin(int userToUse) {
        boolean result = true;
        printSection("Sanity Login: " + userToUse);
        userIndex = userToUse;
        if (!login()) {
            printSubSection("Sanity Login failed.");
            takeSnapShot("fail-sanity-login-" + userToUse);
            result = false;
        }
        closeAntNotification();
        takeSnapShot("sanity-login-" + userToUse);
        result = waitForTitleToNotBe("Import Old Winning Votes", driver.getCurrentUrl()) && result;
        // Force auto import because it may not have happened yet
        {
            String url = BASE_URL + "v#auto_import///";
            driver.get(url);
        }
        result = waitForTitleToNotBe("Import Old Winning Votes", driver.getCurrentUrl()) && result;
        return result;
    }

    /**
     * @returns true if sanity test passes
     */
    boolean testSanity() {
        boolean result = true;

        result = sanityLogin(SurveyDriverCredentials.SPECIAL_USER_TC) && result;

        /// ----- OK. now start the steps
        /// 10. CLA
        result = testCla() && result;

        /// 11.- Voting
        result = testVoting() && result;

        /// 24.- Forum
        result = testForum() && result;

        /// 28.- Reports
        result = testReports() && result;

        /// 29 BRS - n/a

        printSection("Overall Sanity Test: " + getResult(result));
        return result;
    }

    boolean testVoting() {
        boolean result = true;

        final String VOTING_LOCALE = "fr";
        final String VOTING_LOCALE_NAME = "French";

        // ---
        printSection(String.format("Voting %s - %s", VOTING_LOCALE, VOTING_LOCALE_NAME));

        //  11..20 voting for a new value and existing
        result = testVotingNewAndExisting(result, VOTING_LOCALE, VOTING_LOCALE_NAME);

        result = sanityLogin(SurveyDriverCredentials.SPECIAL_USER_DDL) && result;

        /// 21.  Voted for an abbreviated month or day that conflicts with another entry, verified
        // warning displayed in the info panel
        result =
                testVotingAbbreviatedMonth(
                        result, "ady", "Adyghe"); // Should be mul, see CLDR-19409

        result = sanityLogin(SurveyDriverCredentials.SPECIAL_USER_TC) && result;

        /// 22.  Voted for a Winning value with an error, and see error displayed in the Info Panel
        // (requires a 4-vote locale)
        result = testVotingWinningValueWithError(result, VOTING_LOCALE, VOTING_LOCALE_NAME);

        /// 23.  Navigate to Next/Previous page

        printSubSection("Overall Voting " + VOTING_LOCALE + ": " + getResult(result));
        takeSnapShot("voting-" + VOTING_LOCALE + "-done");
        return result;
    }

    private boolean testVotingAbbreviatedMonth(boolean result, String locale, String localeName) {
        final String VOTING_HASH = "9aa3b64aaf1346b"; // Jan
        final String VOTING_OTHER_HASH = "4f14011064918b2"; // Feb
        final String VOTING_SAME_VALUE = "Ф";
        final String VOTING_PAGE = "Gregorian";
        printSubSection(String.format("Abbr conflicting month - TODO CLDR-16859 %s", EMOJI_SKIP));

        if (false) { // TODO
            printSubSection(
                    String.format("Abbr conflicting month, verify warning: %s", VOTING_HASH));
            // don't care about the page

            // First, reset! Abstain on Jan
            {
                result = navigateToHash(locale, localeName, VOTING_HASH, VOTING_PAGE) && result;
                result = goAwaySidebar(result);
                final WebElement votingRow = getVotingRow(VOTING_HASH);

                result = abstainIfNotAbstained(votingRow) && result;
            }

            // Meanwhile, over on February...
            {
                result =
                        navigateToHash(locale, localeName, VOTING_OTHER_HASH, VOTING_PAGE)
                                && result;
                result = goAwaySidebar(result);
                final WebElement votingRow = getVotingRow(VOTING_OTHER_HASH);

                // .. abstain on Feb..
                result = abstainIfNotAbstained(votingRow) && result;
                // Vote for Feb
                result = submitValue(VOTING_OTHER_HASH, VOTING_SAME_VALUE, votingRow) && result;
                result = waitTillMyVoteHasClass(votingRow, TD_CLASS_WIN) && result;
            }

            // Back to January.
            {
                result = navigateToHash(locale, localeName, VOTING_HASH, VOTING_PAGE) && result;
                result = goAwaySidebar(result);
                final WebElement votingRow = getVotingRow(VOTING_HASH);

                // Vote for the same in Jan
                result = submitValue(VOTING_HASH, VOTING_SAME_VALUE, votingRow) && result;
                result = waitTillMyVoteHasClass(votingRow, TD_CLASS_WIN) && result;

                // TODO:
            }
        }

        takeSnapShot("voting-" + locale + "-abbr");
        return result;
    }

    private boolean goAwaySidebar(boolean result) {
        // the overlay is as annoying to the webdriver as it is to vetters.
        hideLeftSidebar(driver.getCurrentUrl());
        if (!waitUntilElementInactive("overlay", driver.getCurrentUrl())) {
            result = false;
        }
        return result;
    }

    private boolean testVotingWinningValueWithError(
            boolean result, String locale, String localeName) {
        return result;
    }

    private boolean testVotingNewAndExisting(
            boolean result, final String locale, final String localeName) {
        /// 11.
        final String VOTING_HASH = "77e7af2481778f25";
        final String VOTING_PAGE = "Fields";
        final String VALUE_NIM = "nim";
        final String VOTE_ALT_NIM = "ci_77e7af2481778f25_bmlt"; // "vbmlt__xymjmb";
        final String EXISTING_VALUE = "min";
        result = testOpenLocale(locale, localeName) && result;

        /// 12.
        result = testNavigateDashboard() && result;

        /// 13.
        result = testWarningClick() && result;

        /// 15. Open Date & Time fields
        printSubSection("Open Date & Time fields");
        result = navigateToPage(locale, localeName, VOTING_HASH, VOTING_PAGE) && result;
        takeSnapShot("voting-" + locale + "-" + VOTING_PAGE);

        /// 16.  Opened Dashboard
        result = confirmDashboardOpen(result);

        /// 17.  Opened Info Panel
        result = confirmInfoPanelOpen(result);
        /// 18.  Submitted a new value for an item

        // find our row (throws on failure)
        final WebElement votingRow = getVotingRow(VOTING_HASH);

        result = abstainIfNotAbstained(votingRow) && result;

        result = confirmAbstention(result, votingRow);

        submitValue(VOTING_HASH, VALUE_NIM, votingRow);

        printlnSummary(
                "- waiting for '%s' to appear",
                VALUE_NIM); // OK, now wait for the button to appear.
        waitUntilIdExists(VOTE_ALT_NIM, true, driver.getCurrentUrl());
        WebElement myLosingVote = driver.findElement(By.id(VOTE_ALT_NIM)); // using an id here
        printlnSummary("- Got cell with » %s", myLosingVote.getText());
        result = confirmExpectedVote(result, VALUE_NIM, myLosingVote);

        result = confirmWeCastLosingVote(result, votingRow);

        takeSnapShot("voting-" + "addedValue");

        /// 19.  Voted for an existing item
        result = voteForExistingValue(result, votingRow, EXISTING_VALUE);

        takeSnapShot("voting-Existing");

        /// 20.  Abstain the same existing item to undo
        {
            voteAbstain(votingRow);
            result = waitTillMyVoteHasClass(votingRow, TD_CLASS_ABSTAIN) && result;
        }

        takeSnapShot("voting-" + locale + "-abstain");
        return result;
    }

    private WebElement getVotingRow(final String VOTING_HASH) {
        String rowId = "row_" + VOTING_HASH;
        printlnSummary("- Waiting for our row %s to exist", rowId);
        if (!waitUntilIdExists(rowId, true, driver.getCurrentUrl())) return null;
        final WebElement votingRow = driver.findElement(By.id(rowId));
        if (!waitUntilElementDisplayed(votingRow, driver.getCurrentUrl())) return null;
        return votingRow;
    }

    private boolean navigateToPage(
            final String locale,
            final String localeName,
            final String votingHash,
            final String votingPage) {
        String url = BASE_URL + "v#/" + locale + "/" + votingPage + "/" + votingHash;
        driver.get(url);
        return waitForTitle(localeName + ": " + votingPage, url);
    }

    private boolean navigateToHash(
            final String locale,
            final String localeName,
            final String votingHash,
            final String votingPage) {
        String url = BASE_PATH + "#/" + locale + "/" + votingPage + "/" + votingHash;
        driver.get(url);
        return waitForTitle(localeName + ": " + votingPage, url);
    }

    private boolean voteForExistingValue(boolean result, final WebElement votingRow, String value) {
        WebElement existingValue = findCldrValue(votingRow, value); // the original value.
        if (existingValue == null) {
            result = false;
            printlnSummary(" :x: Could not find cldrValue %s", value);
        }
        // That's nice that we found existingValue. That means the next line will work.
        WebElement existingTd = findValueCell(value, votingRow);
        if (existingTd == null) {
            result = false;
            printlnSummary(" :x: Could not find td for cldrValue %s", value);
        }
        // now, find the clicker
        WebElement existingClicker = existingTd.findElement(By.className("ichoice-o"));
        if (existingClicker == null) {
            result = false;
            printlnSummary(" :x: Could not find clicker for cldrValue %s", value);
        }

        printlnSummary("- clicking %s", value);
        existingClicker.click();
        printlnSummary("- Waiting until our vote is the winner");
        result = waitTillMyVoteHasClass(votingRow, TD_CLASS_WIN) && result;
        return result;
    }

    private boolean confirmWeCastLosingVote(boolean result, final WebElement votingRow) {
        WebElement ourVote = findCellWithMyVote(votingRow);
        @Nullable String ourClassName = getClassName(ourVote);
        printlnSummary("- Our vote should be losing = %s ", ourClassName);
        result = waitTillMyVoteHasClass(votingRow, TD_CLASS_LOSING) && result;
        return result;
    }

    private boolean confirmExpectedVote(
            boolean result, final String value, WebElement myVoteCldrValue) {
        if (!myVoteCldrValue.getText().trim().equals(value)) {
            result = false;
            printlnSummary(" :x: Wrong value!");
        }
        return result;
    }

    private boolean submitValue(
            final String xpathHash, final String value, final WebElement votingRow) {
        try {
            clickPlusButton(xpathHash, votingRow);

            printlnSummary("- waiting for input box to exist");
            assertTrue(waitUntilClassExists("ant-modal", true, driver.getCurrentUrl()));
            printlnSummary("- getting the input box");
            WebElement dialog = driver.findElement(By.className("ant-modal"));
            WebElement inputBox = dialog.findElement(By.cssSelector("input.ant-input"));
            inputBox.click();

            printlnSummary(String.format("- Type `%s`", value));
            inputBox.sendKeys(value);
            takeSnapShot("voting-" + "inputbox");
            final String SUBMIT_BTN = "Submit";
            WebElement submitBtn = findButtonByText(dialog, SUBMIT_BTN);
            printlnSummary("- Clicking: %s", submitBtn.getText());
            submitBtn.click();
        } catch (Throwable t) {
            SurveyDriverLog.println(t);
            printlnSummary("- :x: Failed to set %s=%s: %s", xpathHash, value, t.getMessage());
            return false;
        } finally {
            takeSnapShot("Voting-" + xpathHash + "-" + value);
        }
        return true;
    }

    private void clickPlusButton(final String VOTING_HASH, final WebElement votingRow) {
        WebElement plusButton = votingRow.findElement(By.className("plus"));

        printlnSummary("- clicked on +: " + VOTING_HASH);
        plusButton.click();
    }

    private boolean confirmAbstention(boolean result, final WebElement votingRow) {
        WebElement ourVote = findCellWithMyVote(votingRow);
        printlnSummary("- Our vote = %s ", getClassName(ourVote));
        if (!getClassName(ourVote).contains(TD_CLASS_ABSTAIN)) {
            result = false;
            printlnSummary(" :x: Expected abstension");
        }
        return result;
    }

    private boolean abstainIfNotAbstained(final WebElement votingRow) {
        WebElement ourVote = findCellWithMyVote(votingRow);
        if (!getClassName(ourVote).contains(TD_CLASS_ABSTAIN)) {
            printlnSummary(
                    "- :do_not_litter: Oops, we've been here before.. abstaining on this item");
            voteAbstain(votingRow);
            return waitTillMyVoteHasClass(votingRow, TD_CLASS_ABSTAIN);
        }
        return true;
    }

    private void voteAbstain(final WebElement votingRow) {
        WebElement noCell = votingRow.findElement(By.className(TD_CLASS_ABSTAIN)); // abstention
        noCell.click();
    }

    private boolean confirmInfoPanelOpen(boolean result) {
        printlnSummary("- Confirming the Info Panel is open");
        result = waitForElementWithClassAndSubtext("i-am-info-panel", "Info Panel") && result;
        return result;
    }

    private boolean confirmDashboardOpen(boolean result) {
        printlnSummary("- Confirming the Dashboard is open");
        result = waitForElementWithClassAndSubtext("i-am-dashboard", "Dashboard") && result;
        return result;
    }

    private boolean waitTillMyVoteHasClass(final WebElement votingRow, final String className) {
        try {
            wait.until(
                    webDriver -> {
                        // we should be in the winning row
                        WebElement ourVote = findCellWithMyVote(votingRow);
                        if (ourVote == null) return false;
                        return (getClassName(ourVote).contains(className));
                    });
            return true;
        } catch (org.openqa.selenium.TimeoutException timeout) {
            WebElement ourVote = findCellWithMyVote(votingRow);
            printlnSummary(
                    "- :x: timed out waiting for our vote to have %s — got %s",
                    className, getClassName(ourVote));
            return false;
        }
    }

    private @Nullable String getClassName(WebElement ourVote) {
        return ourVote.getAttribute("className");
    }

    private WebElement findCldrValue(WebElement parent, String text) {
        return findByText(parent.findElements(By.className("cldrValue")), text);
    }

    /**
     * find button by text
     *
     * @param parent
     * @param text
     * @return
     */
    private WebElement findButtonByText(WebElement parent, final String text) {
        List<WebElement> btns = parent.findElements(By.tagName("button"));
        return findByText(btns, text);
    }

    /**
     * @returns element from list that matches, or null
     */
    private WebElement findByText(List<WebElement> parent, final String text) {
        for (final WebElement e : parent) {
            if (e.getText().trim().equals(text.trim())) {
                return e;
            }
            // debugging
            // printlnSummary("- Button: %s", e.getText());
        }
        return null;
    }

    /**
     * @param by matcher (e.g. class=cldrValue)
     * @param textMatch optional match for internal string
     * @param tagName type of return value, e.g. "td"
     * @param root parent to look in
     * @returns parent element of type tagName, but stopping at root
     */
    private WebElement getParentElement(
            By by, final String textMatch, final String tagName, WebElement root) {
        for (final WebElement child : root.findElements(By.tagName(tagName))) {
            for (final WebElement grandChild : child.findElements(by)) {
                if (textMatch != null) {
                    if (grandChild.getText().trim().equals(textMatch.trim())) {
                        return child;
                    }
                } else {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * @returns which TD has this specific value in it
     */
    private WebElement findValueCell(final String cldrValue, final WebElement votingRow) {
        return getParentElement(By.className("cldrValue"), cldrValue, "td", votingRow);
    }

    /**
     * @returns which TD has my vote in it
     */
    private WebElement findCellWithMyVote(final WebElement votingRow) {
        return getParentElement(By.className("ichoice-x"), null, "td", votingRow);
    }

    boolean testOpenLocale(final String locale, final String locName) {
        boolean result = true;
        printSubSection("Open Locale: " + locale);

        try {
            // the locale list
            {
                String url = BASE_URL + "v#locales///";
                driver.get(url);
                waitForTitle("Locale List", url);
            }

            // pop open left side
            {
                // TODO CLDR-16859 sidebar click wasn't working...
                final WebElement leftSidebar = driver.findElement(By.id("left-sidebar"));
                // For now just confirm that leftSidebar is preent.
                // // final Point  p = leftSidebar.getLocation();
                // leftSidebar.click();
                // final String linkToLocale = "#/" + locale + "//";

                // now, look for a visible link
                // final WebElement theLocale = findLocaleLink(locale, locName);
                // if (theLocale == null) {
                //     SurveyDriverLog.printlnSummary("- :x: could not find locale link");
                //     takeSnapShot("open-" + locale);
                //     return false;
                // }
                // theLocale.click();

                // Go over to the locale's page
                {
                    String url = BASE_URL + "v#/" + locale + "//";
                    printlnSummary("- go to locale page " + locale);
                    driver.get(url);
                }

                waitForTitle(locName, driver.getCurrentUrl());
            }

            takeSnapShot("open-" + locale);
        } catch (Throwable t) {
            takeSnapShot("open-" + locale + "-exception");
            SurveyDriverLog.println(t);
            result = false;
        }

        printSubSubSection("Overall Open Locale: " + getResult(result));
        return result;
    }

    boolean testDashboardCategory(String category, String catchar, boolean okToSkip) {
        printSubSubSection("Navigate to: " + category + " - " + catchar);
        final WebElement btn = findDashboardCategoryButton(category, okToSkip);
        if (btn == null) {
            takeSnapShot("navigate-dashboard-nobutton-" + category);
            return okToSkip;
        } else {
            btn.click();
        }
        takeSnapShot("navigate-dashboard-" + category);

        final WebElement visibleItemInCat = findVisibleItemInCategory(catchar);

        if (visibleItemInCat == null) {
            printlnSummary("- " + getResult(false) + " - found no visible items for " + catchar);
            return false;
        }

        printlnSummary("- " + getResult(true) + " - found visible item " + catchar);

        return true;
    }

    final WebElement findVisibleItemInCategory(String catchar) {
        final WebElement scroller = driver.findElement(By.id("DashboardScroller"));

        for (final WebElement e : scroller.findElements(By.className("category"))) {
            if (e.getText().startsWith(catchar) && e.isDisplayed()) {
                return e;
            }
        }
        return null;
    }

    boolean testNavigateDashboard() {
        boolean result = true;
        printSubSection("Navigate Dashboard");
        try {
            if (!waitUntilIdExists("DashboardScroller", true, driver.getCurrentUrl())) {
                result = false;
                printlnSummary("- :x: could not find DashboardScroller");
            }

            result = testDashboardCategory("Missing", "M", true) && result;
            result = testDashboardCategory("Provisional", "P", true) && result;
            result = testDashboardCategory("Warning", "W", false) && result;
            result = testDashboardCategory("Reports", "R", false) && result;
            // leave Warning as clicked for the next test.
        } catch (Throwable t) {
            takeSnapShot("navigate-dashboard-exception");
            SurveyDriverLog.println(t);
            result = false;
        }
        printSubSubSection("Navigate Dashboard: " + getResult(result));
        return result;
    }

    private WebElement findDashboardCategoryButton(String category, boolean okToSkip) {
        for (final WebElement e : driver.findElements(By.tagName("BUTTON"))) {
            if (e.getText().startsWith(category)) {
                if (!e.isDisplayed()) {
                    SurveyDriverLog.printlnSummary(
                            "- warning: button " + category + " is found but not displayed");
                } else {
                    return e;
                }
            }
        }
        if (okToSkip) {
            printlnSummary("- " + EMOJI_SKIP + " skip " + category + " - not found");
        } else {
            printlnSummary("- :x: could not find category button " + category);
        }
        return null;
    }

    // private WebElement findLocaleLink(String locale, String locName) {
    //     for (final WebElement e : driver.findElements(By.className("locName"))) {
    //         if (e.getText().equals(locName)) {
    //             if (!e.isDisplayed()) {
    //                 SurveyDriverLog.printlnSummary("- :x: locale name is found but not
    // displayed");
    //                 return null;
    //             }
    //             return e;
    //         }
    //     }
    //     return null;
    // }

    boolean testWarningClick() {
        boolean result = true;
        printSubSection("Click on Warning");
        try {
            // make sure we're still OK
            if (!waitUntilIdExists("DashboardScroller", true, driver.getCurrentUrl())) {
                result = false;
                printlnSummary("- :x: could not find DashboardScroller");
                return result;
            }

            // rerun this test
            testDashboardCategory("Warning", "W", true);

            final WebElement clickItem = findVisibleItemInCategory("W");
            if (clickItem == null) {
                printlnSummary("- :x: could not find item in W");
                result = false;
                return result;
            }

            printlnSummary("- Clicking the dashboard item");
            clickItem.click();

            printlnSummary("- Waiting until the dashboard item shows up");
            // wait until the clicked item shows up
            if (!waitUntilUrlMatches("^.*v#\\/[a-z_A-Z]+\\/[A-Za-z0-9_]+\\/[a-f0-9]+$")) {
                return false;
            }
            printlnSummary("- ended up at " + driver.getTitle() + " - " + driver.getCurrentUrl());

            /// 14. make sure infoPanel is there
            if (!waitUntilIdExists("info-panel-help", true, driver.getCurrentUrl())) {
                printlnSummary("- :x: Info panel didn't showup");
                return false;
            }
            printlnSummary("- got InfoPanel OK");

            takeSnapShot("click-warning");
        } catch (Throwable t) {
            takeSnapShot("click-warning-exception");
            SurveyDriverLog.println(t);
            result = false;
        }
        printSubSubSection("Click on Warning: " + getResult(result));
        return result;
    }

    private boolean waitUntilUrlMatches(final String regex) {
        final Pattern pat = Pattern.compile(regex);
        try {
            quickWait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver -> pat.matcher(driver.getCurrentUrl()).matches());
            return true;
        } catch (TimeoutException e) {
            printlnSummary(
                    "- :x: failed, timed out waiting for URL to match "
                            + regex
                            + " - final url "
                            + driver.getCurrentUrl());
            return false;
        }
    }

    boolean testCla() {
        boolean result = true;
        printSection("CLA");

        String url = BASE_URL + "v#cla///";
        printlnSummary("- before CLA: " + driver.getTitle() + " @ " + driver.getCurrentUrl());
        driver.get(url);
        printlnSummary("- after CLA: " + driver.getTitle() + " @ " + driver.getCurrentUrl());
        waitForTitle("Contributor License Agreement", url);
        boolean hasSignedCorpCla =
                waitForElementWithClassAndSubtext(
                        "ant-alert-message",
                        "Your organization has signed a Unicode Corporate CLA");
        printlnSummary("- Has signed corp CLA: " + getResult(hasSignedCorpCla));
        result = hasSignedCorpCla && result;
        if (!result) {
            printlnSummary(
                    "  :clock: waiting - " + driver.getTitle() + " @ " + driver.getCurrentUrl());
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
        takeSnapShot("sanity-cla");
        if (!result) {
            printlnSummary(
                    "  :clock: waiting - " + driver.getTitle() + " @ " + driver.getCurrentUrl());
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }

        printSubSection("CLA: " + getResult(result));
        return result;
    }

    boolean haveElementWithClassAndSubtext(final String className, final String subText) {
        for (final WebElement e : driver.findElements(By.className(className))) {
            if (e.getText().contains(subText)) return true;
        }
        return false;
    }

    boolean haveElementWithClass(final String className) {
        return !(driver.findElements(By.className(className)).isEmpty());
    }

    boolean waitForElementWithClassAndSubtext(final String className, final String subText) {
        try {
            quickWait.until(
                    (ExpectedCondition<Boolean>)
                            webDriver -> haveElementWithClassAndSubtext(className, subText));
            return true;
        } catch (TimeoutException t) {
            printlnSummary("- :x: did not find .%s with '%s'", className, subText);
            return false;
        }
    }

    void closeAntNotification() {
        printlnSummary("- waiting to close " + ANT_NOTIFICATION_NOTICE_CLOSE);
        try {
            final WebElement el = driver.findElement(By.className(ANT_NOTIFICATION_NOTICE_CLOSE));
            if (el != null) {
                printlnSummary("  …clicked");
                el.click();
            }
        } catch (Throwable t) {
            printlnSummary("  …not found (thats ok) ");
        }
    }

    boolean testForum() {
        boolean result = true;
        printSection("Forum");

        printlnSummary("- TODO CLDR-16859");

        /*

        24……27

            Clicked Forum navigation to show all posts that are Needing action
            Go to Item from Forum
            See forum conversation within Info Panel
            Comment on conversation from within Info Panel


        */

        printSubSection("Overall Forum: " + getResult(result));
        return result;
    }

    boolean testReports() {
        boolean result = true;
        printSection("Reports");

        /// 28 Opened Reports / Datetime

        printlnSummary("- TODO CLDR-16859");

        printSubSection("Overall Reports: " + getResult(result));
        return result;
    }
}
