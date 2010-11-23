package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.ArrayList;
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
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;

public class TestCoverageLevel extends TestFmwk {

    private static CoverageLevel coverageLevel = new CoverageLevel();

    private static String fileMatcher = CldrUtility.getProperty("FILE", ".*");
    
    private static Matcher pathMatcher = Pattern.compile(CldrUtility.getProperty("XPATH", ".*")).matcher("");

    private static int count = 0;

    public static void main(String[] args) throws IOException {
        new TestCoverageLevel().run(args);
    }

    public void TestCoverageLevels() {

        //pathMatcher = Pattern.compile(getProperty("XMLPATH", ".*")).matcher("");

        double startTime = System.currentTimeMillis();
        Factory factory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, fileMatcher);
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        Relation<Level, String> values = new Relation(new TreeMap(), TreeSet.class);
        int oldSize = 0;
        for (String locale : factory.getAvailable()) {
            CLDRFile cldrFileToCheck = factory.make(locale,true);
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
        for (Entry<Level, Set<String>> entry : values.entrySet()) {
            total += entry.getValue().size();
        }
        return total;
    }
}