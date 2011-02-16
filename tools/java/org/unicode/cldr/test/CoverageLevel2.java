package org.unicode.cldr.test;

import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CoverageLevelInfo;

public class CoverageLevel2 {
    private RegexLookup<Level> lookup;
    static {
        for (CoverageLevelInfo coverageInfo : SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY).coverageLevels) {
            String pattern = coverageInfo.match.replace('\'','"');
        }
    }
    
    public static CoverageLevel2 getInstance(String locale) {
        return null;
    }

    public Level getLevel(String path) {
        return lookup.get(path);
    }

    public int getIntLevel(String path) {
        return getLevel(path).getLevel();
    }
}
