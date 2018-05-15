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
    HashMap<String, Long> voteCountI = null; // input

    public static void main(String[] args) {
        new TestAnnotationVotes().run(args);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV00() {
        String test = "adjustAnnotationVoteCounts(null, null) should return quietly";
        try {
            r.adjustAnnotationVoteCounts(null, null);
        } catch (Exception e) {
            errln("❌ " + test + ". Unexpected exception: " + e.toString() + " - " + e.getMessage());
            return;
        }
        // TODO: logln instead of System.out.println -- but how to make it display in log? Set "level" or "verbose" somewhere?
        System.out.println("✅ " + test);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV01() {
        String test = "adjustAnnotationVoteCounts for a=100, b=99, c=98 should return unchanged";
        String[] valI = {"a", "b", "c"}; // input
        long[] votesI = {100,  99,  98}; // input vote counts
        String[] valO = {"a", "b", "c"}; // expected/desired output
        runTest(test, valI, votesI, valO);
     }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV02() {
        String test = "adjustAnnotationVoteCounts for a|b=1, c|d=2, e|f=3 should reverse order";
        String[] valI = {"a|b", "c|d", "e|f"}; // input
        long[] votesI = {1,      2,     3   }; // input vote counts
        String[] valO = {"e|f", "c|d", "a|b"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV03() {
        String test = "adjustAnnotationVoteCounts for a=2, b=2, b|c=1 should make b, a, b|c";
        String[] valI = {"a", "b", "b|c"}; // input
        long[] votesI = { 2,   2,   1   }; // input vote counts
        String[] valO = {"b", "a", "b|c"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV04() {
        String test = "adjustAnnotationVoteCounts for a|b|c|f=8, a|b|e=6, a|e=4 should make a|b|e, a|e, a|b|c|f";
        String[] valI = {"a|b|c|f", "a|b|e", "a|e"}; // input
        long[] votesI = {8,          6,       4   }; // input vote counts
        String[] valO = {"a|b|e", "a|e", "a|b|c|f"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV05() {
        String test = "adjustAnnotationVoteCounts for a=3, b|c=2, b|c|d=2 should make b|c, b|c|d, a";
        String[] valI = {"a", "b|c", "b|c|d"}; // input
        long[] votesI = {3   , 2,     2     }; // input vote counts
        String[] valO = {"b|c", "b|c|d", "a"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV06() {
        String test = "adjustAnnotationVoteCounts for a|b|c=8, a|b|d=6, a|d=4 should make a|b|d, a|d, a|b|c";
        String[] valI = {"a|b|c", "a|b|d", "a|d"}; // input
        long[] votesI = {8,        6,       4   }; // input vote counts
        String[] valO = {"a|b|d", "a|d", "a|b|c"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV07() {
        String test = "adjustAnnotationVoteCounts for a=24, b|c=20, b|c|d=20 should make b|c, b|c|d, a";
        String[] valI = {"a", "b|c", "b|c|d"}; // input
        long[] votesI = {24,   20,    20    }; // input vote counts
        String[] valO = {"b|c", "b|c|d", "a"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Run adjustAnnotationVoteCounts with the given input and expected output.
     * Test whether the actual output matches the expected output.
     * 
     * @param test the string describing what should happen
     * @param valI the array of input values
     * @param votesI the array of vote counts corresponding to valI
     * @param valO the array of expected output values
     * 
     * Note: there is no array of expected output vote counts. Treat the test as passing
     * or failing based only on the order of output values, not on the exact output vote counts.
     */
   private void runTest(String test, String[] valI, long[] votesI, String[] valO) {
        setupTestIO(valI, votesI, valO);
        String input = sortedValuesI.toString() + " " + voteCountI.toString(); // before adjusting
        try {
            r.adjustAnnotationVoteCounts(sortedValuesI, voteCountI);
        } catch (Exception e) {
            errln("❌ " + test + ". Unexpected exception: " + e.toString() + " - " + e.getMessage());
            return;
        }
        // Convert sets to strings for comparison, otherwise the set.equals() function ignores differences in order.
        // Also, strings can be displayed more conveniently with errln.
        String expected = sortedValuesO.toString();
        String actually = sortedValuesI.toString();
        if (!actually.equals(expected)) {
            String msg = test + "."
                + "\n\tInput:   \t" + input
                + "\n\tExpected:\t" + expected
                + "\n\tActually:\t" + actually + " " + voteCountI.toString();
            errln("❌ " + msg);
            return;
        }
        System.out.println("✅ " + test);
    }

    /**
     * Set up test inputs and outputs.
     * 
     * @param valI the array of input values
     * @param votesI the array of vote counts corresponding to valI
     * @param valO the array of expected output values
     */
    private void setupTestIO(String[] valI, long[] votesI, String[] valO) {
        sortedValuesI = new LinkedHashSet<String>(new ArrayList<String>(Arrays.asList(valI)));
        sortedValuesO = new LinkedHashSet<String>(new ArrayList<String>(Arrays.asList(valO)));
        voteCountI = new HashMap<String, Long>();
        int i = 0;
        for (String value : valI) {
            voteCountI.put(value, votesI[i++]);
        }
    }
}
