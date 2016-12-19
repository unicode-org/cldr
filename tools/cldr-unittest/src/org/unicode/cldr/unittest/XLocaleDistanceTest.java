package org.unicode.cldr.unittest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.XLikelySubtags.LSR;
import org.unicode.cldr.draft.XLocaleDistance;
import org.unicode.cldr.draft.XLocaleDistance.DistanceNode;
import org.unicode.cldr.draft.XLocaleDistance.DistanceTable;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.ULocale;

public class XLocaleDistanceTest extends TestFmwk {
    public static void main(String[] args) {
        new XLocaleDistanceTest().run(args);
    }
    
    public static final int FAIL = XLocaleDistance.ABOVE_THRESHOLD;
    XLocaleDistance localeMatcher = XLocaleDistance.getDefault();

    public void testMain() {
        
        logln("\n" + localeMatcher.toString(true));

//        XLocaleDistance intLocaleMatcher = XLocaleDistance.createDefaultInt();
//        logln(intLocaleMatcher.toString());
//        logln("closer: " + localeMatcher.getCloserLanguages());

        Object[][] tests = {
            {"iw", "he", 0}, // canonicals
            {"zh", "cmn", 0}, // canonicals

            {"to", "en", 14, FAIL}, // fallback languages get closer distances, between script (40) and region (4)
            {"no", "no_DE", 4},
            {"nn", "no", 10},
            {"no_DE", "nn", 14},
            {"no", "no", 0},
            {"no", "da", 12},
            {"da", "zh_Hant", FAIL},
            {"zh_Hant", "zh_Hans", 23, 19},
            {"zh_Hans", "en", FAIL},
            
            {"en-US", "en_AU", 5}, // across clusters
            {"en-VI", "en_GU", 4}, // within cluster
            {"en_AU", "en_CA", 4}, // within cluster
            
            {"en_CA", "en_Cyrl", FAIL},
            {"en_Cyrl", "es_MX", FAIL},
            
            {"hr", "sr", FAIL},
            {"hr", "sr-Latn", 8},
            {"sr", "sr-Latn", 5},
            
            // check 419 behavior. Should be as good as any in cluster
            {"es_MX", "es_AR", 4},
            {"es_MX", "es_419", 4},
            {"es_MX", "es_MX", 0},
            {"es_MX", "es_ES", 5},
            {"es_MX", "es_PT", 5},
            {"es_MX", "es_150", 5},
            
            {"es_419", "es_AR", 4},
            {"es_419", "es_419", 0},
            {"es_419", "es_MX", 4},
            {"es_419", "es_ES", 5},
            {"es_419", "es_PT", 5},
            {"es_419", "es_150", 5},
            
            {"es_ES", "es_AR", 5},
            {"es_ES", "es_419", 5},
            {"es_ES", "es_MX", 5},
            {"es_ES", "es_ES", 0},
            {"es_ES", "es_PT", 4},
            {"es_419", "es_150", 5},
        };
        // pre-process the data, so that it doesn't affect the timing below.
        for (int i = 0; i < tests.length; ++i) {
            Object[] row = tests[i];
            if (row.length < 4) {
                tests[i] = row = new Object[]{row[0], row[1], row[2], row[2]};
            }
            row[0] = new ULocale((String)row[0]);
            row[1] = new ULocale((String)row[1]);
        }

        final LocaleMatcher oldLocaleMatcher = new LocaleMatcher("");

        long likelyTime = 0;
        long newLikelyTime = 0;
        long newTime = 0;
        long intTime = 0;
        long oldTime = 0;
        final int maxIterations = 10000;

        for (int iterations = maxIterations; iterations > 0; --iterations) {
            int count=0;
            for (Object[] test : tests) {
                final ULocale desired = (ULocale) test[0];
                final ULocale supported = (ULocale) test[1];
                final int desiredToSupported = (Integer) test[2];
                final int supportedToDesired = (Integer) test[3];

                long temp = System.nanoTime();
                final ULocale desiredMax = ULocale.addLikelySubtags(desired);
                final ULocale supportedMax = ULocale.addLikelySubtags(supported);
                likelyTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                double distOld1 = oldLocaleMatcher.match(desired, desiredMax, supported, supportedMax);
                double distOld2 = oldLocaleMatcher.match(supported, supportedMax, desired, desiredMax);
                oldTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                final LSR desiredLSR = LSR.fromMaximalized(desired);
                final LSR supportedLSR = LSR.fromMaximalized(supported);
                newLikelyTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                int dist1 = localeMatcher.distance(desiredLSR, supportedLSR, 1000);
                int dist2 = localeMatcher.distance(supportedLSR, desiredLSR, 1000);
                newTime += System.nanoTime()-temp;

//                temp = System.nanoTime();
//                int distInt1 = intLocaleMatcher.distance(desiredLSR.language, supportedLSR.language, desiredLSR.script, supportedLSR.script, desiredLSR.region, supportedLSR.region);
//                int distInt2 = intLocaleMatcher.distance(supportedLSR.language, desiredLSR.language, supportedLSR.script, desiredLSR.script, supportedLSR.region, desiredLSR.region);
//                intTime += System.nanoTime()-temp;

                if (iterations == maxIterations) {
                    int ds = pinToDistance(distOld1);
                    int sd = pinToDistance(distOld2);
//                    assertEquals("old: " + desired + "\t ⇒ \t" + supported, desiredToSupported, pinToDistance(distOld1));
//                    assertEquals("old: " + desired + "\t ← \t" + supported, supportedToDesired, pinToDistance(distOld2));
                    
                    if (assertEquals("new: " + desired + "\t ⇒ \t" + supported, desiredToSupported, pin(dist1))) {
                        //assertEquals("int: " + desired + "\t ⇒ \t" + supported, dist1, distInt1);
                    }
                    if (assertEquals("new: " + desired + "\t ← \t" + supported, supportedToDesired, pin(dist2))) {
                        //assertEquals("int: " + desired + "\t ← \t" + supported, dist2, distInt2);
                    }
                }
            }
        }
        logln("\n");
        logln("\tlikelyTime:\t" + likelyTime/maxIterations);
        logln("\toldTime-likelyTime:\t" + oldTime/maxIterations);
        logln("totalOld:\t" + (oldTime+likelyTime)/maxIterations);
        logln("\tnewLikelyTime:\t" + newLikelyTime/maxIterations);
        logln("totalNew:\t" + newTime/maxIterations);
        //logln("\tnewIntTime-newLikelyTime-extractTime:\t" + intTime/maxIterations);
        //logln("totalInt:\t" + (intTime)/maxIterations);
    }

    private int pinToDistance(double original) {
        long distance = Math.round((100*(1-original)));
        return distance >= 40 ? FAIL : (int)distance;
    }

    private int pin(int original) {
        return original >= 40 ? FAIL : original;
    }
    
    @SuppressWarnings("deprecation")
    public void testInternalTable() {
        checkTables(localeMatcher.internalGetDistanceTable(), "", 1);
    }

    @SuppressWarnings("deprecation")
    private void checkTables(DistanceTable internalGetDistanceTable, String title, int depth) {
        // Check that ANY, ANY is always present, and that the table has a depth of exactly 3 everyplace.
        Map<String, Set<String>> matches = internalGetDistanceTable.getInternalMatches();
        
        // must have ANY,ANY
        boolean haveANYANY = false;
        for (Entry<String, Set<String>> entry : matches.entrySet()) {
            String first = entry.getKey();
            boolean haveANYfirst = first.equals(XLocaleDistance.ANY);
            for (String second : entry.getValue()) {
                haveANYANY |= haveANYfirst && second.equals(XLocaleDistance.ANY);
                DistanceNode distanceNode = internalGetDistanceTable.getInternalNode(first, second);
                DistanceTable subDistanceTable = distanceNode.getDistanceTable();
                if (subDistanceTable == null || subDistanceTable.isEmpty()) {
                    if (depth != 3) {
                        logln("depth should be 3");
                    }
                    if (distanceNode.getClass() != DistanceNode.class) {
                        logln("should be plain DistanceNode");
                    }
                } else {
                    if (depth >= 3) {
                        logln("depth should be ≤ 3");
                    }
                    if (distanceNode.getClass() == DistanceNode.class) {
                        logln("should NOT be plain DistanceNode");
                    }
                    checkTables(subDistanceTable, first + "," + second + ",", depth+1);
                }
            }
        }
        if (!haveANYANY) {
            logln("ANY-ANY not in" + matches);
        }
    }
}
