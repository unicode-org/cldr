package org.unicode.cldr.tool;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Output;

public class GetChanges {
    static CLDRConfig testInfo = ToolConfig.getToolInstance();
    static CLDRFile english = testInfo.getEnglish();
    static boolean verbose = true;

    public static void main(String[] args) {
        Matcher localeFilter = Pattern.compile(".*").matcher("");
        File vxml = new File(CLDRPaths.AUX_DIRECTORY, "voting/"
            + "35"
            + "/vxml");
        for (File vxmlDir : vxml.listFiles()) {
            if (!vxmlDir.isDirectory()) {
                continue;
            }
            System.out.println("#Dir: " + vxmlDir);
            String l1 = CLDRPaths.BASE_DIRECTORY + vxmlDir.getName() + "/";
            // common, ...
            File[] commonSeedFiles = vxmlDir.listFiles();
            if (commonSeedFiles == null) {
                System.out.println("#No files in: " + vxmlDir);
                continue;
            }
            for (File commonSeed : commonSeedFiles) {
                if (!commonSeed.isDirectory() 
                    || "dtd".equals(commonSeed.getName())) {
                    continue;
                }
                System.out.println("#Dir=\t" + commonSeed);
                String l2 = l1 + commonSeed.getName();
                // main, annotations
                Factory factoryVxml = Factory.make(commonSeed.toString(), ".*");
                Factory factoryTrunk = Factory.make(l2, ".*");

                for (String locale : factoryVxml.getAvailable()) {
                    if (!localeFilter.reset(locale).matches()) {
                        continue;
                    }
                    if (locale.equals("root")) {
                        continue;
                    }
                    CLDRFile vxmlFile;
                    try {
                        vxmlFile = factoryVxml.make(locale, false);
                    } catch (Exception e1) {
                        System.out.println("#Error: dir=\t" + commonSeed + "\t locale=\t" + locale + "\tmsg=\t" + e1.getMessage());
                        continue;
                    }
                    CLDRFile trunkFile = null;
                    try {
                        trunkFile = factoryTrunk.make(locale, true);
                    } catch (Exception e) {
                    }
                    compare(vxmlDir.getName() + "/" + commonSeed.getName(), vxmlFile, trunkFile);
                }
            }
        }
    }

    private static void compare(String dir, CLDRFile vxmlFile, CLDRFile trunkFile) {
        String localeID = vxmlFile.getLocaleID();
        //System.out.println("#Dir: " + dir + ";\tLocale: " + localeID);
//        Output<String> localeWhereFound = new Output<>();
//        Output<String> pathWhereFound = new Output<>();
        int countNew = 0;
        for (String path : vxmlFile) {
            if (path.contains("/identity")) {
                continue;
            }
            String vxmlValue = vxmlFile.getStringValue(path);
            String trunkValue = trunkFile == null ? null : trunkFile.getStringValue(path);
            if (Objects.equals(vxmlValue, trunkValue)) {
                continue;
            }
//            String baileyValue = vxmlFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
//            if (!localeID.equals(localeWhereFound.value)) {
//                continue;
//            }
//            if (!path.equals(pathWhereFound.value)) {
//                continue;
//            }
            if (trunkValue == null) {
                ++countNew;
                if (verbose && !dir.contains("seed")) {
                    System.out.println("new; locale=\t" + localeID + "\tvxml=\t" + vxmlValue + "\tpath=\t" + path);
                }
            } else {
                // System.out.println("diff; locale=" + localeID + ";\tvxml=" + vxmlValue + ";\ttrunk=" + trunkValue + ";\tpath=" + path);
            }
        }
        if (countNew != 0) {
            System.out.println("#Dir=\t" + dir + "\tLocale=\t" + localeID + "\tCountNew=\t" + countNew);
        }
    }

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


    private void old() {
        boolean onlyMissing = true;
        String release = "33.1";

        String subdir = "annotations"; // "main";
        Factory lastReleaseFactory = Factory.make(CLDRPaths.LAST_DIRECTORY + "common/"
            + subdir, ".*");
        Factory trunkFactory = testInfo.getAnnotationsFactory(); // CldrFactory();
        Factory snapshotFactory = Factory.make(CLDRPaths.AUX_DIRECTORY + "voting/"
            + release
            + "/vxml/common/"
            + subdir, ".*");
        PathHeader.Factory phf = PathHeader.getFactory(english);

        int totalCount = 0;
        int localeCount = 0;

        Output<String> localeWhereFound = new Output<String>();
        Output<String> pathWhereFound = new Output<String>();

        CLDRFile englishCldrFile = trunkFactory.make("en", false);
        final Set<String> paths = ImmutableSet.copyOf(englishCldrFile.iterator());
        System.out.println("english paths: " + paths.size());

        Multimap<String,PathHeader> missing = TreeMultimap.create();

        Set<String> locales = testInfo.getStandardCodes().getLocaleCoverageLocales(Organization.cldr, Collections.singleton(Level.MODERN));

        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : locales) {
            if (!ltp.set(locale).getRegion().isEmpty() || locale.equals("sr_Latn")) {
                continue; // skip region locales, they'll inherit
            }
//            if (!locale.equals("sr_Latn")) {
//                continue;
//            }
            System.out.println(locale);

            CLDRFile snapshot;
            try {
                snapshot = snapshotFactory.make(locale, false);
            } catch (Exception e) {
                System.out.println("Skipping " + locale + ", no data in annotations/");
                continue;
            }

            CLDRFile lastRelease = null;
            try {
                lastRelease = lastReleaseFactory.make(locale, false);
            } catch (Exception e) {
            }

            CLDRFile trunk = null;
            try {
                trunk = trunkFactory.make(locale, false);
            } catch (Exception e) {
            }

            Map<PathHeader, Data> results = new TreeMap<PathHeader, Data>();

            for (String xpath : paths) {
                if (xpath.contains("fallbackRegionFormat") 
                    || xpath.contains("exemplar") 
                    || xpath.contains("/identity")) {
                    continue;
                }
                String newPath = fixOldPath(xpath);
                String valueSnapshot = snapshot.getStringValue(newPath);
                PathHeader ph = null;

                if (valueSnapshot == null) {
                    ph = phf.fromPath(newPath);
                    missing.put(locale, ph);
                }
                String valueLastRelease = lastRelease == null ? null : lastRelease.getStringValue(xpath);
                if (valueSnapshot == null) {
                    ph = phf.fromPath(newPath);
                    missing.put(locale, ph);
                }

                if (onlyMissing) {
                    continue;
                }
                if (valueSnapshot != null && Objects.equals(valueLastRelease, valueSnapshot)) {
                    continue;
                }
                String valueTrunk = trunk == null ? null : trunk.getStringValue(newPath);
//                if (valueSnapshot == null && valueTrunk == null) { // committee deletion
//                    continue;
//                }
                // skip inherited
                String baileyValue = snapshot.getConstructedBaileyValue(xpath, pathWhereFound, localeWhereFound);
                if (!"root".equals(localeWhereFound.value)
                    && !"code-fallback".equals(localeWhereFound.value)
                    && CldrUtility.equals(valueSnapshot, baileyValue)) {
                    continue;
                }
                ph = ph != null ? ph : phf.fromPath(newPath);
                results.put(ph, new Data(valueLastRelease, valueSnapshot, valueTrunk));
            }
            if (results.isEmpty()) {
                continue;
            }
            int itemCount = 0;
            localeCount++;
            for (Entry<PathHeader, Data> entry : results.entrySet()) {
                PathHeader ph = entry.getKey();
                String englishValue = englishCldrFile.getStringValue(ph.getOriginalPath());
                System.out.println(localeCount 
                    + "\t" + ++itemCount 
                    + "\t" + locale 
                    + "\t" + english.getName(locale) 
                    + "\t" + ph 
                    + "\t«" + englishValue 
                    + "»\t" + entry.getValue());
            }
            totalCount += itemCount;
        }
        System.out.println("Total:\t" + totalCount);
        for ( Entry<String, PathHeader> entry : missing.entries()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue()); 
        }
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
