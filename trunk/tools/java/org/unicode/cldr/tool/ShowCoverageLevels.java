package org.unicode.cldr.tool;

import java.util.EnumMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Level;

import com.ibm.icu.impl.Relation;

public class ShowCoverageLevels {
    private static CLDRConfig testInfo = ToolConfig.getToolInstance();

    private static int count = 0;

    public static void main(String[] args) {
        if (true) {
            //ShowLocaleCoverage foo;
            throw new IllegalArgumentException("See ShowLocaleCoverage (TODO: merge these).");
        }

        double startTime = System.currentTimeMillis();
        Relation<Level, String> values = new Relation(new EnumMap<Level, String>(Level.class), TreeSet.class);
        int oldSize = 0;

        CoverageInfo coverageInfo = CLDRConfig.getInstance().getCoverageInfo();
        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale, true);
            for (String path : cldrFileToCheck) {
                String fullPath = cldrFileToCheck.getFullXPath(path);
                if (fullPath == null) {
                    continue;
                }
                try {
//                    Level level = testInfo.getSupplementalDataInfo().getCoverageLevel(fullPath, locale);
                    Level level = coverageInfo.getCoverageLevel(fullPath, locale);
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
        return values.entrySet().size();
    }
}
