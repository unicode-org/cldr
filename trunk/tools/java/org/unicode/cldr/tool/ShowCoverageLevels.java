package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CoverageLevel;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Level;

import com.ibm.icu.dev.test.util.Relation;

public class ShowCoverageLevels {
    private static TestInfo testInfo = TestInfo.getInstance();

    private static CoverageLevel coverageLevel1 = new CoverageLevel(testInfo.getCldrFactory());

    private static int count = 0;

    public static void main(String[] args) {

        //pathMatcher = Pattern.compile(getProperty("XMLPATH", ".*")).matcher("");

        double startTime = System.currentTimeMillis();
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        Relation<Level, String> values = new Relation(new EnumMap<Level, String>(Level.class), TreeSet.class);
        int oldSize = 0;

        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,true);
            coverageLevel1.setFile(cldrFileToCheck, options, null, possibleErrors);
            for (String path : cldrFileToCheck) {
                String fullPath = cldrFileToCheck.getFullXPath(path);
                if (fullPath == null) {
                    continue;
                }
                try {
                    Level level = coverageLevel1.getCoverageLevel(fullPath);
                    values.put(level, path);
                } catch (Exception e) {
                    String value = cldrFileToCheck.getStringValue(path);
                    System.out.println("Can't create coverage level for path\t" 
                            + locale + ", " + path + ", " + fullPath + ", " + value);
                }
            }
            int size = keyValuePairCount(values);
            int deltaSize = size - oldSize;
            oldSize = size;
            System.out.println(locale + "\tadditions: " + deltaSize + "\ttotal: " + size);
        }

        double deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
        System.out.println("Instances found: " + count);

        for (Level level : values.keySet()) {
            System.out.println(level.toString());
            for (String path : values.getAll(level)) {
                System.out.println("\t" + path);
            }
        }
    }

    private static int keyValuePairCount(Relation<Level, String> values) {
        int total = 0;
        for (Entry<Level, String> entry : values.entrySet()) {
            total++;
        }
        return total;
    }
}
