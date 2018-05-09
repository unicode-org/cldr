package org.unicode.cldr.unittest.web;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.unittest.web.TestAll.WebTestInfo;
import org.unicode.cldr.util.VoteResolver;

import com.ibm.icu.dev.test.TestFmwk;

public class TestAnnotationVotes extends TestFmwk {

    TestAll.WebTestInfo testInfo = WebTestInfo.getInstance();

    public static void main(String[] args) {
        new TestAnnotationVotes().run(args);
        // new TestAnnotationVotes().run(TestAll.doResetDb(args));
    }

    /**
     * Test features related to adjustBarJoinedAnnotationVoteCounts in VoteResolver.java.
     * Note: the name of this function must begin with "Test", or it will be ignored! See TestFmwk.java.
     */
    public void TestAV() {
        VoteResolver<String> r = new VoteResolver<String>();
        HashMap<String, Long> voteCount = null;
        Set<String> sortedValues = null;
        String test = "adjustBarJoinedAnnotationVoteCounts(null, null) should return quietly";
        try {
            r.adjustBarJoinedAnnotationVoteCounts(voteCount, sortedValues);
        } catch (Exception e) {
            errln("❌ " + test + ". Unexpected exception: "
                    + e.toString() + " - " + e.getMessage());
            return;
        }
        // TODO: logln instead of System.out.println -- but how to make it display in log? Set "level" or "verbose" somewhere?
        System.out.println("✅ " + test);
  
        test = "adjustBarJoinedAnnotationVoteCounts for a=100, b=999, c=998 should return unchanged";
        sortedValues = new LinkedHashSet<String>();
        sortedValues.add("a");
        sortedValues.add("b");
        sortedValues.add("c");
        voteCount = new HashMap<String, Long>();
        long count = 100;
        for (String value : sortedValues) {
            voteCount.put(value, count--);
        }
        HashMap<String, Long> voteCount2 = new HashMap<String, Long>(voteCount);
        Set<String> sortedValues2 =  new LinkedHashSet<String>(sortedValues);
        try {
            r.adjustBarJoinedAnnotationVoteCounts(voteCount, sortedValues);
        } catch (Exception e) {
            errln("❌ " + test + ". Unexpected exception: "
                + e.toString() + " - " + e.getMessage());
            return;
        }
        if (!sortedValues.equals(sortedValues2) || !voteCount.equals(voteCount2)) {
            errln("❌ " + test);
            return;
        }
        System.out.println("✅ " + test);
        
        test = "adjustBarJoinedAnnotationVoteCounts for a|b=1, c|d=2, e|f=3 should reverse order";
        sortedValues.clear();
        sortedValues.add("a|b");
        sortedValues.add("c|d");
        sortedValues.add("e|f");
        sortedValues2.clear();
        sortedValues.add("e|f");
        sortedValues.add("c|d");
        sortedValues.add("a|b");
        voteCount.clear();
        count = 1;
        for (String value : sortedValues) {
            voteCount.put(value, count++);
        }
        voteCount2.clear();
        count = 1;
        for (String value : sortedValues2) {
            voteCount2.put(value, count++);
        }
        try {
            r.adjustBarJoinedAnnotationVoteCounts(voteCount, sortedValues);
        } catch (Exception e) {
            errln("❌ " + test + ". Unexpected exception: "
                + e.toString() + " - " + e.getMessage());
            return;
        }
        if (!sortedValues.equals(sortedValues2) || !voteCount.equals(voteCount2)) {
            errln("❌ " + test);
            return;
        }
        System.out.println("✅ " + test);

        errln("❌ This test is not yet fully implemented.");
    }
}
