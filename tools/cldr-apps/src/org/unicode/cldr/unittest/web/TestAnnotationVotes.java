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
        /* An older version of this test ("a=3, b|c=2, b|c|d=2 should make b|c, b|c|d, a") would now
         * fail due to rounding double (float) to long (integer) when calculating the geometric mean.
         * Most actual votes are multiples of 4, and this test passes with the new vote counts
         * simply being the old vote counts multiplied by 4.
         */
        String test = "adjustAnnotationVoteCounts for a=12, b|c=8, b|c|d=8 should make b|c, b|c|d, a";
        String[] valI = {"a", "b|c", "b|c|d"}; // input
        long[] votesI = {12  , 8,     8     }; // input vote counts
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
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV08() {
        // References:
        //  https://unicode.org/cldr/trac/ticket/11165
        //  https://unicode.org/cldr/trac/ticket/10973
        String test = "adjustAnnotationVoteCounts for hmyz | malárie | moskyt | štípnutí | virus, dengue | hmyz | malárie | moskyt | štípnutí | virus ...";
        String[] valI = {"hmyz | malárie | moskyt | štípnutí | virus", "dengue | hmyz | malárie | moskyt | štípnutí | virus"}; // input
        long[] votesI = {4,                                             8                                                   }; // input vote counts
        String[] valO = {"dengue | hmyz | malárie | moskyt | štípnutí | virus", "hmyz | malárie | moskyt | štípnutí | virus"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV09() {
        // same as TestAV08 except one-letter names
        String test = "adjustAnnotationVoteCounts for b|c|d|e|f=4, a|b|c|d|e|f=8 should make a|b|c|d|e|f, b|c|d|e|f";
        String[] valI = {"b|c|d|e|f", "a|b|c|d|e|f"}; // input
        long[] votesI = {4,            8           }; // input vote counts
        String[] valO = {"a|b|c|d|e|f", "b|c|d|e|f"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV10() {
        // same input and output as TestAV09, different vote counts
        String test = "adjustAnnotationVoteCounts for b|c|d|e|f=4, a|b|c|d|e|f=6 should make a|b|c|d|e|f, b|c|d|e|f";
        String[] valI = {"b|c|d|e|f", "a|b|c|d|e|f"}; // input
        long[] votesI = {4,            6           }; // input vote counts
        String[] valO = {"a|b|c|d|e|f", "b|c|d|e|f"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV11() {
        // same input as TestAV09, different vote counts and output
        String test = "adjustAnnotationVoteCounts for b|c|d|e|f=4, a|b|c|d|e|f=5 should make b|c|d|e|f, a|b|c|d|e|f";
        String[] valI = {"b|c|d|e|f", "a|b|c|d|e|f"}; // input
        long[] votesI = {4,            5           }; // input vote counts
        String[] valO = {"b|c|d|e|f", "a|b|c|d|e|f"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV12() {
        // test superior supersets with alphabetical fallback: a|b|c before a|b|d
        String test = "adjustAnnotationVoteCounts for a|b=4, a|b|d=8, a|b|c=8 should make a|b|c, a|b|d, a|b";
        String[] valI = {"a|b", "a|b|d", "a|b|c"}; // input
        long[] votesI = {4,      8,       8     }; // input vote counts
        String[] valO = {"a|b|c", "a|b|d", "a|b"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV13() {
        String test = "adjustAnnotationVoteCounts for a|b=4, a|b|d=8, a|b|c=7 should make a|b|d, a|b|c, a|b";
        String[] valI = {"a|b", "a|b|d", "a|b|c"}; // input
        long[] votesI = {4,      8,       7     }; // input vote counts
        String[] valO = {"a|b|d", "a|b|c", "a|b"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV14() {
        String test = "adjustAnnotationVoteCounts for a|b=8, a=8 should make a, a|b";
        String[] valI = {"a|b", "a"}; // input
        long[] votesI = {8,      8 }; // input vote counts
        String[] valO = {"a", "a|b"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV15() {
        String test = "adjustAnnotationVoteCounts for a|b=8, a=4 should make a|b, a";
        String[] valI = {"a|b", "a"}; // input
        long[] votesI = {8,      4 }; // input vote counts
        String[] valO = {"a|b", "a"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV16() {
        String test = "adjustAnnotationVoteCounts for a|b=4, a|b|d=8, a|b|c=7 should make a|b|d, a|b|c, a|b";
        String[] valI = {"a|b", "a|b|d", "a|b|c"}; // input
        long[] votesI = {4,      8,       7     }; // input vote counts
        String[] valO = {"a|b|d", "a|b|c", "a|b"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV17() {
        // test superior supersets with shorter-set fallback: a|b|e before a|b|c|d
        String test = "adjustAnnotationVoteCounts for a|b=4, a|b|c|d=8, a|b|e=8 should make a|b|e, a|b|c|d, a|b";
        String[] valI = {"a|b", "a|b|c|d", "a|b|e"}; // input
        long[] votesI = {4,      8,         8     }; // input vote counts
        String[] valO = {"a|b|e", "a|b|c|d", "a|b"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV18() {
        // test shorter-set fallback: a|b|e before a|b|c|d
        String test = "adjustAnnotationVoteCounts for a|b|c|d=8, a|b|e=8 should make a|b|e, a|b|c|d";
        String[] valI = {"a|b|c|d", "a|b|e"}; // input
        long[] votesI = {8,          8     }; // input vote counts
        String[] valO = {"a|b|e", "a|b|c|d"}; // expected/desired output
        runTest(test, valI, votesI, valO);
    }

    /**
     * Test features related to adjustAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of each function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV19() {
        // Superior supersets can't have more than 7 = CheckWidths.MAX_COMPONENTS_PER_ANNOTATION.
        String test = "adjustAnnotationVoteCounts for a|b=4, a|b|c|d|e|f|g|h=9, a|b|c|x|y=8 should make a|b|c|x|y, a|b, a|b|c|d|e|f|g|h";
        String[] valI = {"a|b", "a|b|c|d|e|f|g|h", "a|b|c|x|y"}; // input
        long[] votesI = {4,      9,                 8         }; // input vote counts
        String[] valO = {"a|b|c|d|e|f|g|h", "a|b|c|x|y", "a|b"}; // expected/desired output
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
