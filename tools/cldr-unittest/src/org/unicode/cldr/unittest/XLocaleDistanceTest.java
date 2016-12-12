package org.unicode.cldr.unittest;

import org.unicode.cldr.draft.XLikelySubtags;
import org.unicode.cldr.draft.XLikelySubtags.LSR;
import org.unicode.cldr.draft.XLocaleDistance;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.ULocale;

public class XLocaleDistanceTest extends TestFmwk {
    public static void main(String[] args) {
        new XLocaleDistanceTest().run(args);
    }
    
    public void TestMain() {
        
        XLocaleDistance localeMatcher = XLocaleDistance.getDefault();
        logln(localeMatcher.toString());
        logln("closer: " + localeMatcher.getCloserLanguages());

        XLocaleDistance intLocaleMatcher = XLocaleDistance.createDefaultInt();
//        logln(intLocaleMatcher.toString());
//        logln("closer: " + localeMatcher.getCloserLanguages());

        Object[][] tests = {
            {"no", "no_DE", 4},

            
            {"to", "en", 14, 666}, // fallback languages get closer distances, between script (40) and region (4)
            {"no", "no_DE", 4},
            {"no_DE", "nb", 5},
            {"nb", "no", 1},
            {"no", "no", 0},
            {"no", "da", 12},
            {"da", "zh_Hant", 666},
            {"zh_Hant", "zh_Hans", 23, 19},
            {"zh_Hans", "en", 666},
            {"en", "en_GB", 5},
            {"en", "en_GU", 3},
            {"en_GB", "en_CA", 3},
            {"en_CA", "en_Cyrl", 666},
            {"en_Cyrl", "es_MX", 666},
            
            {"es_MX", "es_AR", 3},
            {"es_MX", "es_419", 3},
            {"es_MX", "es_MX", 0},
            {"es_MX", "es_ES", 5},
            {"es_MX", "es_PT", 5},
            {"es_MX", "es_150", 5},
            
            {"es_419", "es_AR", 3},
            {"es_419", "es_419", 0},
            {"es_419", "es_MX", 3},
            {"es_419", "es_ES", 5},
            {"es_419", "es_PT", 5},
            {"es_419", "es_150", 5},
            
            {"es_ES", "es_AR", 5},
            {"es_ES", "es_419", 5},
            {"es_ES", "es_MX", 5},
            {"es_ES", "es_ES", 0},
            {"es_ES", "es_PT", 3},
            {"es_419", "es_150", 5},
        };
        // fix, so that it doesn't affect the timing below.
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
        long extractTime = 0;
        long newTime = 0;
        long intTime = 0;
        long oldTime = 0;
        final int maxIterations = 10000;
        final XLikelySubtags newLikely = XLikelySubtags.getDefault();

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
                String desiredLang = desired.getLanguage();
                String desiredScript = desired.getScript();
                String desiredRegion = desired.getCountry();

                String supportedLang = supported.getLanguage();
                String supportedScript = supported.getScript();
                String supportedRegion = supported.getCountry();
                extractTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                final LSR desiredLSR = newLikely.addLikelySubtags(desiredLang, desiredScript, desiredRegion);
                final LSR supportedLSR = newLikely.addLikelySubtags(supportedLang, supportedScript, supportedRegion);
                newLikelyTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                int dist1 = localeMatcher.distance(desiredLSR.language, supportedLSR.language, desiredLSR.script, supportedLSR.script, desiredLSR.region, supportedLSR.region);
                int dist2 = localeMatcher.distance(supportedLSR.language, desiredLSR.language, supportedLSR.script, desiredLSR.script, supportedLSR.region, desiredLSR.region);
                newTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                int distInt1 = intLocaleMatcher.distance(desiredLSR.language, supportedLSR.language, desiredLSR.script, supportedLSR.script, desiredLSR.region, supportedLSR.region);
                int distInt2 = intLocaleMatcher.distance(supportedLSR.language, desiredLSR.language, supportedLSR.script, desiredLSR.script, supportedLSR.region, desiredLSR.region);
                intTime += System.nanoTime()-temp;

                if (iterations == 1) {
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
        logln("\textractTime:\t" + extractTime/maxIterations);
        logln("\tnewTime-newLikelyTime-extractTime:\t" + newTime/maxIterations);
        logln("totalNew:\t" + (newLikelyTime+extractTime+newTime)/maxIterations);
        logln("\tnewIntTime-newLikelyTime-extractTime:\t" + intTime/maxIterations);
        logln("totalInt:\t" + (newLikelyTime+extractTime+intTime)/maxIterations);
    }

    private int pinToDistance(double original) {
        long distance = Math.round((100*(1-original)));
        return distance >= 40 ? 666 : (int)distance;
    }

    private int pin(int original) {
        return original >= 40 ? 666 : original;
    }

}
