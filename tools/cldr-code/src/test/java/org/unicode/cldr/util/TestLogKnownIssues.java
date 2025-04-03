package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.testutil.TestWithKnownIssues;

public class TestLogKnownIssues extends TestWithKnownIssues {
    @Test
    void sampleTestWithKnownIssues() {
        if(logKnownIssue("CLDR-18481", "Sample Log Known Issue")) {
            return; // don't test this case.
        }
        assertEquals(6, 2+2, "Expected to Fail");
    }

    @Test
    void sampleTestWithAssume() {
        assumeFalse(logKnownIssue("CLDR-18481", "Sample Log Known Issue"));

        assertEquals(6, 2+2, "Expected to Fail");
    }
}
