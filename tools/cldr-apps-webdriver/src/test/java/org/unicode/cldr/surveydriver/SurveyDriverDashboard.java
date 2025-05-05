package org.unicode.cldr.surveydriver;

import org.openqa.selenium.WebDriver;

public class SurveyDriverDashboard {

    private final SurveyDriver s;
    private final WebDriver driver;

    public SurveyDriverDashboard(SurveyDriver s) {
        this.s = s;
        this.driver = s.driver;
    }

    private static final String[] locales = {
        // Czech, German, Spanish, French, Hindi, Japanese, Russian, Chinese
        "cs", "de", "es", "fr", "hi", "ja", "ru", "zh"
    };

    /** Test the Dashboard interface */
    public boolean test() {
        if (!s.login()) {
            return false;
        }
        final int REPETITION_COUNT = 10000;
        for (int i = 0; i < REPETITION_COUNT; i++) {
            SurveyDriverLog.println("SurveyDriverDashboard.test i = " + i);
            if (!testOne(i)) {
                return false;
            }
        }
        SurveyDriverLog.println("✅ Dashboard test passed");
        return true;
    }

    private boolean testOne(int i) {
        String loc = locales[i % locales.length];
        String url = SurveyDriver.BASE_URL + "v#/" + loc + "//";
        driver.get(url);
        if (!s.hideLeftSidebar(url)) {
            return false;
        }
        if (!s.waitUntilElementInactive("left-sidebar", url)) {
            return false;
        }
        if (!s.waitUntilElementInactive("overlay", url)) {
            return false;
        }
        // If we're on a locale's "General Info" page (rather than a specific
        // page such as "Alphabetic_Information"), then the "Open Dashboard"
        // button is from GeneralInfo.vue, and its class includes "general-open-dash".
        // There are other "Open Dashboard" buttons produced in cldrGui.mjs, and a
        // "Dashboard" item in the left-sidebar.
        // For unknown reasons, clickButtonByXpath with "//button[contains(., 'Open Dashboard')]"
        // fails here.
        if (!s.clickButtonByClassName("general-open-dash", url)) {
            return false;
        }
        if (!s.waitUntilIdExists("DashboardScroller", true, url)) {
            return false;
        }
        SurveyDriverLog.println("✅ Dashboard: tested locale " + loc);
        return true;
    }
}
