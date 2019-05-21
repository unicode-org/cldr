package org.unicode.cldr.tool;

import java.util.LinkedHashMap;
import java.util.Map;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;

import com.google.common.base.Objects;
import com.ibm.icu.util.Output;

public class FindHardInheritance {
    static final class Info {
        final int regularCount;
        final int inheritedCount;
        final int redundantCount;
        final int constructedCount;
        final int coverageCount;
        
        final int regularCountCps;
        final int inheritedCountCps;
        final int redundantCountCps;
        final int constructedCountCps;
        final int coverageCountCps;
        
        public Info(int regularCount, int inheritedCount, int redundantCount, int constructedCount, int coverageCount, 
            int regularCountCps, int inheritedCountCps, int redundantCountCps, int constructedCountCps, int coverageCountCps) {
            this.regularCount = regularCount;
            this.inheritedCount = inheritedCount;
            this.redundantCount = redundantCount;
            this.constructedCount = constructedCount;
            this.coverageCount = coverageCount;
            this.regularCountCps = regularCountCps;
            this.inheritedCountCps = inheritedCountCps;
            this.redundantCountCps = redundantCountCps;
            this.constructedCountCps = constructedCountCps;
            this.coverageCountCps = coverageCountCps;
        }
        @Override
        public String toString() {
            return regularCount + "\t" + regularCountCps
                + "\t" + inheritedCount + "\t" + inheritedCountCps
                + "\t" + redundantCount + "\t" + redundantCountCps
                //+ "\t" + constructedCount + "\t" + constructedCountCps
                + "\t" + coverageCount + "\t" + coverageCountCps
                ;
        }
        static final String HEADER = 
                    "count\tcps"
                + "\tinher.\tcps"
                + "\tredund.\tcps"
                //+ "\tconstr.\tcps"
                + "\tcover.\tcps"
                ;
    }

    static Output<String> localeWhereFound = new Output<>();
    static Output<String> pathWhereFound = new Output<>();

    public static void main(String[] args) {
//        for (String localeId : CLDRConfig.getInstance().getCldrFactory().getAvailable()) {
//            String name = CLDRConfig.getInstance().getEnglish().getName(localeId);
//            System.out.println(localeId + "\t" + name);
//        }
//        if (true) return;
        
        Map<String, Info> data = new LinkedHashMap<>();
        System.out.println(
            "dir."
            + "\t" + "locale"
            + "\t" + Info.HEADER
            );
        Output<Info> output = new Output<>();

        for (String dir : DtdType.ldml.directories) {
            switch (dir) {
            default: break;
            case "casing": continue;
            }
            Factory factory = Factory.make(CLDRPaths.COMMON_DIRECTORY + dir, ".*");

            for (String localeId : factory.getAvailable()) {
                Info info = getCounts(dir, factory, localeId, output);
                System.out.println(dir
                    + "\t" + localeId
                    + "\t" + info
                    );
            }
        }
    }

    private static Info getCounts(String dir, Factory factory, String localeId, Output<Info> cpsInfo) {
        int redundantCount = 0;
        int regularCount = 0;
        int constructedCount = 0;
        int inheritedCount = 0;
        int coverageCount = 0;
        
        int redundantCountCps = 0;
        int regularCountCps = 0;
        int constructedCountCps = 0;
        int inheritedCountCps = 0;
        int coverageCountCps = 0;
        
        boolean allConstructed = "annotationsDerived".equals(dir);
        CoverageLevel2 coverage = CoverageLevel2.getInstance(localeId);
        CLDRFile cldrFile = factory.make(localeId, true, DraftStatus.contributed);
        CLDRFile unresolvedCldrFile = factory.make(localeId, false, DraftStatus.contributed);
        
        for (String path : cldrFile) {
            if (path.startsWith("//ldml/identity/")) {
                continue;
            }
            String value = cldrFile.getStringValue(path);
            int cps = value.codePointCount(0, value.length());
            Level level = coverage.getLevel(path);
            if (unresolvedCldrFile.getStringValue(path) == null) {
                ++inheritedCount;
                inheritedCountCps+=cps;
                continue;
            }
            if (!Level.CORE_TO_MODERN.contains(level)) {
                ++coverageCount;
                coverageCountCps+=cps;
                continue;
            }
            String bailey = cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
            if (Objects.equal(value, bailey)) {
                ++redundantCount;
                redundantCountCps+=cps;
            } else if (allConstructed) {
                ++constructedCount;
                constructedCountCps+=cps;
            } else {
                ++regularCount;
                regularCountCps+=cps;
            }
        }
        Info info = new Info(regularCount, inheritedCount, redundantCount, constructedCount, coverageCount, 
            regularCountCps, inheritedCountCps, redundantCountCps, constructedCountCps, coverageCountCps);
        return info;
    }
}
