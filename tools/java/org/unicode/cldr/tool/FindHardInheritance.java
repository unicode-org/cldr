package org.unicode.cldr.tool;

import java.util.LinkedHashMap;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;

import com.google.common.base.Objects;
import com.ibm.icu.util.Output;

public class FindHardInheritance {
    static final class Info {
        final int regularCount;
        final int inheritedCount;
        final int redundantCount;
        final int constructedCount;
        public Info(int regularCount, int inheritedCount, int redundantCount, int constructedCount) {
            this.regularCount = regularCount;
            this.inheritedCount = inheritedCount;
            this.redundantCount = redundantCount;
            this.constructedCount = constructedCount;
        }
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
        System.out.println("dir."
            + "\t" + "locale"
            + "\t" + "regul."
            + "\t" + "inherit"
            + "\t" + "redund."
            + "\t" + "constr."
            );

        for (String dir : DtdType.ldml.directories) {
            switch (dir) {
            default: break;
            case "casing": continue;
            }
            Factory factory = Factory.make(CLDRPaths.COMMON_DIRECTORY + dir, ".*");

            for (String localeId : factory.getAvailable()) {
                Info info = getCounts(dir, factory, localeId);
                System.out.println(dir
                    + "\t" + localeId
                    + "\t" + info.regularCount
                    + "\t" + info.inheritedCount
                    + "\t" + info.redundantCount
                    + "\t" + info.constructedCount
                    );
            }
        }
    }

    private static Info getCounts(String dir, Factory factory, String localeId) {
        int redundantCount = 0;
        int regularCount = 0;
        int constructedCount = 0;
        int inheritedCount = 0;
        boolean allConstructed = "annotationsDerived".equals(dir);
        CLDRFile cldrFile = factory.make(localeId, true);
        CLDRFile unresolvedCldrFile = factory.make(localeId, false);
        for (String path : cldrFile) {
            if (path.startsWith("//ldml/identity/")) {
                continue;
            }
            if (unresolvedCldrFile.getStringValue(path) == null) {
                ++inheritedCount;
                continue;
            }
            String value = cldrFile.getStringValue(path);
            String bailey = cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
            if (Objects.equal(value, bailey)) {
                ++redundantCount;
            } else if (allConstructed) {
                ++constructedCount;
            } else {
                ++regularCount;
            }
        }
        Info info = new Info(regularCount, inheritedCount, redundantCount, constructedCount);
        return info;
    }
}
