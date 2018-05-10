package org.unicode.cldr.unittest.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.unittest.web.TestAll.WebTestInfo;
import org.unicode.cldr.util.VoteResolver;

import com.ibm.icu.dev.test.TestFmwk;

public class TestAnnotationVotes extends TestFmwk {

    TestAll.WebTestInfo testInfo = WebTestInfo.getInstance();

    VoteResolver<String> r = new VoteResolver<String>();
    Set<String> sortedValuesI = null, sortedValuesO = null; // input and output
    HashMap<String, Long> voteCountI = null, voteCountO = null; // input and output

    public static void main(String[] args) {
        new TestAnnotationVotes().run(args);
    }

    /**
     * Test features related to adjustBarJoinedAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV00() {
        String test = "adjustBarJoinedAnnotationVoteCounts(null, null) should return quietly";
        try {
            r.adjustBarJoinedAnnotationVoteCounts(null, null);
        } catch (Exception e) {
            errln("❌ " + test + ". Unexpected exception: " + e.toString() + " - " + e.getMessage());
            return;
        }
        // TODO: logln instead of System.out.println -- but how to make it display in log? Set "level" or "verbose" somewhere?
        System.out.println("✅ " + test);
    }

    /**
     * Test features related to adjustBarJoinedAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV01() {
        String test = "adjustBarJoinedAnnotationVoteCounts for a=100, b=999, c=998 should return unchanged";
        String[] valI = {"a", "b", "c"};
        long[] votesI = {100, 999, 998};
        String[] valO = {"a", "b", "c"};
        long[] votesO = {100, 999, 998};
        runTest(test, valI, votesI, valO, votesO);
     }

    /**
     * Test features related to adjustBarJoinedAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV02() {
        String test = "adjustBarJoinedAnnotationVoteCounts for a|b=1, c|d=2, e|f=3 should reverse order";
        String[] valI = {"a|b", "c|d", "e|f"};
        long[] votesI = {1, 2, 3};
        String[] valO = {"e|f", "c|d", "a|b"};
        long[] votesO = {3, 2, 1};
        runTest(test, valI, votesI, valO, votesO);
    }

    /**
     * Test features related to adjustBarJoinedAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV99() {
        errln("❌ This set of tests is incomplete.");
    }

    private void runTest(String test, String[] valI, long[] votesI, String[] valO, long[] votesO) {
        setupTestIO(valI, votesI, valO, votesO);
        try {
            r.adjustBarJoinedAnnotationVoteCounts(voteCountI, sortedValuesI);
        } catch (Exception e) {
            errln("❌ " + test + ". Unexpected exception: " + e.toString() + " - " + e.getMessage());
            return;
        }
        // Convert sets to strings for comparison, otherwise the equals() function ignores differences in order.
        if (!sortedValuesI.toString().equals(sortedValuesO.toString()) || !voteCountI.equals(voteCountO)) {
            errln("❌ " + test);
            return;
        }
        System.out.println("✅ " + test);
    }
    
    /**
     * Setup test inputs and outputs.
     */
    private void setupTestIO(String[] inValues, long[] inVotes, String[] outValues, long[] outVotes) {
        sortedValuesI = new LinkedHashSet<String>(new ArrayList<String>(Arrays.asList(inValues)));
        sortedValuesO = new LinkedHashSet<String>(new ArrayList<String>(Arrays.asList(outValues)));
        voteCountI = new HashMap<String, Long>();
        voteCountO = new HashMap<String, Long>();
        int i = 0;
        for (String value : inValues) {
            voteCountI.put(value, inVotes[i++]);
        }
        i = 0;
        for (String value : outValues) {
            voteCountO.put(value, outVotes[i++]);
        }
    }
}
