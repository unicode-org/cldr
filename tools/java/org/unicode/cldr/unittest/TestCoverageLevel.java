package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CoverageLevel;
import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ULocale;

public class TestCoverageLevel extends TestFmwk {

    private static TestInfo testInfo = TestInfo.getInstance();

    private CoverageLevel coverageLevel = new CoverageLevel();


    private static int count = 0;

    public static void main(String[] args) throws IOException {
        new TestCoverageLevel().run(args);
    }

    public void TestNewVsOld() {
        checkNewVsOld("en");
    }

    private void checkNewVsOld(String locale) {
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        ULocale ulocale = new ULocale(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,true);
        coverageLevel.setFile(cldrFileToCheck, options, null, possibleErrors);

        Map<Row.R2<Level, Integer>, Relation<String, String>> failures = new TreeMap(); // Relation.of(new HashMap<Row.R2<Level, Integer>, Set<Relation<String,String>>>(), HashSet.class);

        int failureCount = 0;
        int successCount = 0;
        int count = 0;
        PathStarrer pathStarrer = new PathStarrer();

        for (String path : cldrFileToCheck) {
            ++count;
            if (0 == (count & 0xFF)) {
                logln(count + ", " + successCount + ", " + failureCount);
            }
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            Level level = coverageLevel.getCoverageLevel(fullPath);            
            int newLevel = testInfo.getSupplementalDataInfo().getCoverageValue(path, ulocale);
            if (newLevel != level.getLevel()) {
                ++failureCount;
            } else {
                ++successCount;
            }
            R2<Level, Integer> key = Row.of(level, newLevel);
            String starredPath = pathStarrer.set(path);
            Relation<String, String> starredToAttributes = failures.get(key);
            if (starredToAttributes == null) {
                failures.put(key, starredToAttributes = new Relation(new TreeMap<String, Set<String>>(), TreeSet.class));
            }
            starredToAttributes.put(starredPath, pathStarrer.getAttributesString("|"));
        }
        if (failureCount != 0) {
            errln("Differences between new and old: " + failureCount + ", same: " + successCount);
        } else {
            logln("Same value: " + successCount);            
        }
        if (params.verbose) {
            for (int i = 0; i < 2; ++i) {
                for (Entry<R2<Level, Integer>, Relation<String, String>> entry : failures.entrySet()) {
                    R2<Level, Integer> levels = entry.getKey();
                    Relation<String, String> starredToAttributes = entry.getValue();
                    Level oldLevel = levels.get0();
                    int newLevel = levels.get1();
                    byte oldLevelInt = oldLevel.getLevel();
                    if ((i == 0) == (oldLevelInt == newLevel)) {
                        continue;
                    }
                    //logln("\tCount:\t" + starredToAttributes.values().size() + ",\tOld level:\t" + oldLevel + " (=" + oldLevelInt + ")" + ",\tNew level:\t" + newLevel);
                    //int maxCount = 10;
                    for (Entry<String, Set<String>> s : starredToAttributes.keyValuesSet()) {
                        //                    if (--maxCount < 0) {
                        //                        logln("\t...");
                        //                        break;
                        //                    }
                        String valueSample = s.getValue().toString();
                        if (valueSample.length() > 100) {
                            valueSample = valueSample.substring(0,100) + "…";
                        }
                        logln(oldLevelInt + "\t" + newLevel + "\t" + s.getKey() + "\t" + valueSample);
                    }
                }
            }
        }
    }

    public void TestTime() { 
        checkTime("en");
    }

    private void checkTime(String locale) {
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        ULocale ulocale = new ULocale(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,true);

        Timer t = new Timer();
        coverageLevel.setFile(cldrFileToCheck, options, null, possibleErrors);
        for (String path : cldrFileToCheck) {
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            Level level = coverageLevel.getCoverageLevel(fullPath);            
        }
        long oldTime = t.getDuration();
        logln("Old time:\t" + t.toString());

        t.start();
        coverageLevel.setFile(cldrFileToCheck, options, null, possibleErrors);
        for (String path : cldrFileToCheck) {
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            int newLevel = testInfo.getSupplementalDataInfo().getCoverageValue(path, ulocale);
        }
        double newTime = t.getDuration();
        if (newTime > 2*oldTime) {
            errln("New Coverage Level time too big " + t.toString(1, oldTime));
        } else {
            logln("Old time:\t" + t.toString(1, oldTime));
        }
    }


    public void checkCounts() {

        //pathMatcher = Pattern.compile(getProperty("XMLPATH", ".*")).matcher("");

        double startTime = System.currentTimeMillis();
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        Relation<Level, String> values = new Relation(new TreeMap(), TreeSet.class);
        int oldSize = 0;

        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,true);
            coverageLevel.setFile(cldrFileToCheck, options, null, possibleErrors);
            for (String path : cldrFileToCheck) {
                String fullPath = cldrFileToCheck.getFullXPath(path);
                if (fullPath == null) {
                    continue;
                }
                try {
                    Level level = coverageLevel.getCoverageLevel(fullPath);
                    values.put(level, path);
                } catch (Exception e) {
                    String value = cldrFileToCheck.getStringValue(path);
                    errln("Can't create coverage level for path\t" 
                            + locale + ", " + path + ", " + fullPath + ", " + value);
                }
            }
            int size = keyValuePairCount(values);
            int deltaSize = size - oldSize;
            oldSize = size;
            logln(locale + "\tadditions: " + deltaSize + "\ttotal: " + size);
        }

        double deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
        System.out.println("Instances found: " + count);

        for (Level level : values.keySet()) {
            logln(level.toString());
            for (String path : values.getAll(level)) {
                logln("\t" + path);
            }
        }
    }

    private int keyValuePairCount(Relation<Level, String> values) {
        int total = 0;
        for (Entry<Level, String> entry : values.entrySet()) {
            total++;
        }
        return total;
    }
}