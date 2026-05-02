package org.unicode.cldr.surveydriver;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppTest {
    @Test
    public void shouldDrive() {
        assertTrue(SurveyDriver.runTests());
    }
}
