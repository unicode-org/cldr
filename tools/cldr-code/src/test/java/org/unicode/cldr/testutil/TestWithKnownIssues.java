package org.unicode.cldr.testutil;

import java.io.PrintWriter;
import org.junit.jupiter.api.AfterAll;
import org.unicode.cldr.icu.dev.test.TestFmwk.TestParams;
import org.unicode.cldr.icu.dev.test.UnicodeKnownIssues;
import org.unicode.cldr.unittest.TestAll;
import org.unicode.cldr.util.ShimmedMain;
import org.unicode.cldr.util.StackTracker;

// Example of use:

// class MyTest extends TestWithKnownIssues {
//
//   @Test
//   void sampleTestWithKnownIssues() {
//       if (logKnownIssue("CLDR-18481", "Sample Log Known Issue")) {
//           return; // don't test this case.
//       }
//       assertEquals(6, 2 + 2, "Expected to Fail");
//   }
//
//   @Test
//   void sampleTestWithAssume() {
//       assumeFalse(logKnownIssue("CLDR-18481", "Sample Log Known Issue"));
//
//       assertEquals(6, 2 + 2, "Expected to Fail");
//   }
//
// }

/** JUnit classes that want to have known issues must extend this class */
public class TestWithKnownIssues {
    private static final Class<TestAll> OUR_CLASS = TestAll.class;
    // use the same args as TestAll
    private static final String[] args = ShimmedMain.getArgs(OUR_CLASS);
    private static final TestParams params =
            TestParams.create(args, new PrintWriter(System.err)).init();
    private static final UnicodeKnownIssues uki = params.getKnownIssues();

    /** Make sure that log known issues get printed */
    @AfterAll
    public static void afterAllKnown() {
        if (uki.printKnownIssues(System.err::println)) {
            System.err.println(
                    String.format(
                            "Use -D%s%s=-allKnownIssues to show all known issue sites",
                            OUR_CLASS.getPackageName(), ShimmedMain.TEST_ARGS));
        }
        uki.clear();
    }

    /**
     * Log the known issue. This method returns true unless -prop:logKnownIssue=no is specified in
     * the argument list.
     *
     * @param ticket A ticket number string. For an ICU ticket, use "ICU-10245". For a CLDR ticket,
     *     use "CLDR-12345". For compatibility, "1234" -> ICU-1234 and "cldrbug:456" -> CLDR-456
     * @param comment Additional comment, or null
     * @return true unless -prop:logKnownIssue=no is specified in the test command line argument.
     */
    public boolean logKnownIssue(String ticket, String comment) {
        if (params.hasLogKnownIssue()) {
            StackTraceElement e = StackTracker.currentElement(0);
            final String path =
                    String.format(
                            "%s:%d: (%s)", e.getFileName(), e.getLineNumber(), e.getClassName());
            uki.logKnownIssue(path, ticket, comment);
            return true;
        } else {
            return false;
        }
    }
}
