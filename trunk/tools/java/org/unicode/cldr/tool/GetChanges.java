package org.unicode.cldr.tool;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;

import com.ibm.icu.util.Output;

public class GetChanges {
    static class Data {
        final String valueLastRelease;
        final String valueSnapshot;
        final String valueTrunk;

        public Data(String valueLastRelease, String valueSnapshot, String valueTrunk) {
            super();
            this.valueLastRelease = valueLastRelease;
            this.valueSnapshot = valueSnapshot;
            this.valueTrunk = valueTrunk;
        }

        @Override
        public String toString() {
            return "«" + valueLastRelease + "»"
                + (!CldrUtility.equals(valueTrunk, valueLastRelease) ? "‡" : "")
                + "\t"
                + "«" + valueSnapshot + "»"
                + (CldrUtility.equals(valueTrunk, valueSnapshot) ? "†" : "");
        }
    }

    public static void main(String[] args) {
        CLDRConfig testInfo = ToolConfig.getToolInstance();
        CLDRFile english = testInfo.getEnglish();

        Factory lastReleaseFactory = Factory.make(CLDRPaths.LAST_DIRECTORY + "common/main/", ".*");
        Factory trunkFactory = testInfo.getCldrFactory();
        Factory snapshotFactory = Factory.make(CLDRPaths.TMP2_DIRECTORY + "vxml/common/main/", ".*");
        PathHeader.Factory phf = PathHeader.getFactory(english);

        int totalCount = 0;
        int localeCount = 0;

        Output<String> localeWhereFound = new Output<String>();
        Output<String> pathWhereFound = new Output<String>();

        for (String locale : lastReleaseFactory.getAvailable()) {
            Map<PathHeader, Data> results = new TreeMap<PathHeader, Data>();
            CLDRFile lastRelease = lastReleaseFactory.make(locale, false);
            CLDRFile trunk = trunkFactory.make(locale, false);
            CLDRFile snapshot = snapshotFactory.make(locale, true);
            for (String xpath : lastRelease) {
                if (xpath.contains("fallbackRegionFormat") || xpath.contains("exemplar")) {
                    continue;
                }
                String valueLastRelease = lastRelease.getStringValue(xpath);
                String newPath = fixOldPath(xpath);
                String valueSnapshot = snapshot.getStringValue(newPath);
                if (valueLastRelease.equals(valueSnapshot)) {
                    continue;
                }
                String valueTrunk = trunk.getStringValue(newPath);
                if (valueSnapshot == null && valueTrunk == null) { // committee deletion
                    continue;
                }
                // skip inherited
                String baileyValue = snapshot.getConstructedBaileyValue(xpath, pathWhereFound, localeWhereFound);
                if (!localeWhereFound.value.equals("root")
                    && !localeWhereFound.value.equals("code-fallback")
                    && CldrUtility.equals(valueSnapshot, baileyValue)) {
                    continue;
                }
                PathHeader ph = phf.fromPath(newPath);
                results.put(ph, new Data(valueLastRelease, valueSnapshot, valueTrunk));
            }
            if (results.isEmpty()) {
                continue;
            }
            int itemCount = 0;
            localeCount++;
            for (Entry<PathHeader, Data> entry : results.entrySet()) {
                System.out.println(localeCount + "\t" + ++itemCount + "\t" + locale + "\t" + english.getName(locale) + "\t«" + entry.getKey() + "\t"
                    + entry.getValue());
            }
            totalCount += itemCount;
        }
        System.out.println("Total:\t" + totalCount);
    }

    static Pattern OLD_PATH = PatternCache.get("//ldml/units/unit\\[@type=\"([^\"]*)\"]/unitPattern\\[@count=\"([^\"]*)\"](\\[@alt=\"([^\"]*)\"])?");
    static Matcher OLD_PATH_MATCHER = OLD_PATH.matcher("");

    private static String fixOldPath(String xpath) {
        // //ldml/units/unit[@type="day-future"]/unitPattern[@count="one"]
        // to
        // //ldml/units/unitLength[@type="long"]/unit[@type="duration-day-future"]/unitPattern[@count="one"]

        if (OLD_PATH_MATCHER.reset(xpath).matches()) {
            String type = OLD_PATH_MATCHER.group(4);
            return "//ldml/units/unitLength[@type=\"" + (type == null ? "long" : type)
                + "\"]/unit[@type=\"duration-" + OLD_PATH_MATCHER.group(1)
                + "\"]/unitPattern[@count=\"" + OLD_PATH_MATCHER.group(2)
                + "\"]";
        }
        return xpath;
    }
}
