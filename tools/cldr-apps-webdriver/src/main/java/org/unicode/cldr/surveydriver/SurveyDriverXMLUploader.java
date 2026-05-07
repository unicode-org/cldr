package org.unicode.cldr.surveydriver;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class SurveyDriverXMLUploader {

    SurveyDriver s;

    public SurveyDriverXMLUploader(SurveyDriver s) {
        this.s = s;
    }

    /** Test the XMLUploader interface ("Upload XML" in the gear menu). */
    public boolean testXMLUploader() {
        if (!s.login()) {
            return false;
        }
        String url = SurveyDriver.BASE_URL + "v#locales///";

        WebDriver driver = s.driver;
        driver.get(url);
        if (!s.waitUntilLoadingMessageDone(url)) {
            return false;
        }
        if (!s.waitUntilElementActive("left-sidebar", url)) {
            return false;
        }
        /*
         * Choose "Upload XML" from the gear menu
         */
        do {
            if (!clickOnMainMenu(url)) {
                return false;
            }
        } while (!clickOnUploadXMLElement(url));

        // The interface has changed.
        // There is still a separate tab for bulk upload, but now there's a new step, choosing
        // between these options:
        // 1. Convert XLSX to XML
        // 2. Upload XML as your vote (Bulk Upload)
        SurveyDriverLog.println(
                "❌ XML-Upload test needs revision for changed interface in Survey Tool!");

        switchToNewTabOrWindow();

        if (!specifyXmlFileToUpload(url)) {
            return false;
        }

        SurveyDriverLog.println("✅ XML-Upload test passed");
        return true;
    }

    /**
     * After new tab or window is created, switch WebDriver to it. Otherwise, our actions would
     * still operate on the old window.
     */
    private void switchToNewTabOrWindow() {
        WebDriver driver = s.driver;
        String winHandleBefore = driver.getWindowHandle();
        SurveyDriverLog.println("Before switch: window = " + winHandleBefore);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // driver.switchTo().defaultContent(); -- doesn't work with Chrome on macOS
        String currentTabHandle = driver.getWindowHandle();
        String newTabHandle =
                driver.getWindowHandles().stream()
                        .filter(handle -> !handle.equals(currentTabHandle))
                        .findFirst()
                        .get();
        driver.switchTo().window(newTabHandle);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String winHandleAfter = driver.getWindowHandle();
        SurveyDriverLog.println("After switch: window = " + winHandleAfter);
    }

    /**
     * Click on the main menu.
     *
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    private boolean clickOnMainMenu(String url) {
        String className = "main-menu-icon";
        WebElement clickEl = s.driver.findElement(By.className(className));
        if (clickEl == null) {
            SurveyDriverLog.println("❌ XML-Upload test failed for getting main menu");
            return false;
        }
        clickEl = clickEl.findElement(By.xpath("./..")); // parent
        new Actions(s.driver).moveToElement(clickEl, 0, 0).build().perform();
        if (!s.waitUntilElementClickable(clickEl, url)) {
            SurveyDriverLog.println(
                    "❌ XML-Upload test failed waiting for main menu to be clickable");
            return false;
        }
        int repeats = 0;
        for (; ; ) {
            try {
                clickEl.click();
                return true;
            } catch (StaleElementReferenceException e) {
                if (++repeats > 4) {
                    break;
                }
                SurveyDriverLog.println(
                        "clickOnMainMenu repeating for StaleElementReferenceException in " + url);
                clickEl = s.driver.findElement(By.className(className));
            } catch (Exception e) {
                SurveyDriverLog.println(e);
                break;
            }
        }
        SurveyDriverLog.println("❗ Test failed in clickOnMainMenu in " + url);
        return false;
    }

    /**
     * Click on the "Upload (Bulk Import)" item in the gear menu.
     *
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    private boolean clickOnUploadXMLElement(String url) {
        String linkText = "Upload (Bulk Import)";
        WebElement clickEl = null;
        try {
            clickEl = s.driver.findElement(By.partialLinkText(linkText));
        } catch (Exception e) {
            SurveyDriverLog.println(e);
        }
        if (clickEl == null) {
            SurveyDriverLog.println("❌ XML-Upload test failed for getting " + linkText + " menu");
            return false;
        }
        new Actions(s.driver).moveToElement(clickEl, 0, 0).build().perform();
        if (!s.waitUntilElementClickable(clickEl, url)) {
            SurveyDriverLog.println(
                    "❌ XML-Upload test failed waiting for " + linkText + " menu to be clickable");
            return false;
        }
        int repeats = 0;
        for (; ; ) {
            try {
                clickEl.click();
                return true;
            } catch (StaleElementReferenceException e) {
                if (++repeats > 4) {
                    break;
                }
                SurveyDriverLog.println(
                        "clickOnGearElement repeating for StaleElementReferenceException in "
                                + url);
                clickEl = s.driver.findElement(By.partialLinkText(linkText));
            } catch (Exception e) {
                SurveyDriverLog.println(e);
                break;
            }
        }
        SurveyDriverLog.println("❗ Test failed in clickOnUploadXMLElement in " + url);
        return false;
    }

    /**
     * Click on the Choose File button, and input the XML file pathname.
     *
     * @param url the url we're loading
     * @return true for success, false for failure
     */
    private boolean specifyXmlFileToUpload(String url) {
        final String id = "file";
        final String xmlPathname =
                "/Users/tbishop/Documents/WenlinDocs/Organizations/Unicode/CLDR_job/xml_upload_test.xml";
        int repeats = 0;
        for (; ; ) {
            if (!s.waitUntilIdExists(id, true, url)) {
                SurveyDriverLog.println("❌ XML-Upload test failed waiting for id to exist: " + id);
                return false;
            }
            WebElement clickEl = s.driver.findElement(By.id(id));
            if (clickEl == null) {
                SurveyDriverLog.println("❌ XML-Upload test failed for getting id: " + id);
                return false;
            }
            new Actions(s.driver).moveToElement(clickEl, 0, 0).build().perform();
            if (!s.waitUntilElementClickable(clickEl, url)) {
                SurveyDriverLog.println(
                        "❌ XML-Upload test failed waiting for element to be clickable: " + id);
                return false;
            }
            try {
                Actions action = new Actions(s.driver);
                action.moveToElement(clickEl).click().sendKeys(xmlPathname).perform();
                return true;
            } catch (StaleElementReferenceException e) {
                if (++repeats > 4) {
                    break;
                }
                SurveyDriverLog.println(
                        "specifyXmlFileToUpload repeating for StaleElementReferenceException in "
                                + url);
            } catch (Exception e) {
                SurveyDriverLog.println(e);
                break;
            }
        }
        SurveyDriverLog.println("❗ Test failed in specifyXmlFileToUpload in " + url);
        return false;
    }
}
