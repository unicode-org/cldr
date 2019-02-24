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
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.XMLSource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Output;

public class GetChanges {
    static CLDRConfig testInfo = ToolConfig.getToolInstance();
    static CLDRFile english = testInfo.getEnglish();
    static boolean verbose = true;

    public static void main(String[] args) {
        System.out.println("#Dir\tState\tTrunk Value\tmodify_config.txt Locale\tAction\tlabel\tPath Value\tlabel\tNew Value");
        Matcher localeFilter = Pattern.compile(".*").matcher("");
        File vxml = new File(CLDRPaths.AUX_DIRECTORY, "voting/"
            + "35"
            + "/vxml2");
        for (File vxmlDir : vxml.listFiles()) {
            if (!vxmlDir.isDirectory()) {
                continue;
            }
            //System.out.println("#Dir=\t" + vxmlDir);
            // common, ...
            File[] commonSeedFiles = vxmlDir.listFiles();
            if (commonSeedFiles == null) {
                System.out.println("##No files in: " + vxmlDir);
                continue;
            }
            for (File commonSeed : commonSeedFiles) {
                if (!commonSeed.isDirectory() 
                    || "dtd".equals(commonSeed.getName())) {
                    continue;
                }
                //System.out.println("#Dir=\t" + commonSeed);
                String subDir = vxmlDir.getName() + "/" + commonSeed.getName();
                String l2 = CLDRPaths.BASE_DIRECTORY + subDir;
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
                        vxmlFile = factoryVxml.make(locale, true);
                    } catch (Exception e1) {
                        System.out.println("##Error: dir=\t" + commonSeed + "\t locale=\t" + locale + "\tmsg=\t" + e1.getMessage());
                        continue;
                    }
                    CLDRFile trunkFile = null;
                    try {
                        trunkFile = factoryTrunk.make(locale, true);
                    } catch (Exception e) {
                    }
                    compare(subDir, vxmlFile, trunkFile);
                }
            }
        }
    }

    public static final Set<String> ROOT_OR_CODE_FALLBACK = ImmutableSet.of(XMLSource.CODE_FALLBACK_ID, XMLSource.ROOT_ID);

    private static void compare(String dir, CLDRFile vxmlFileResolved, CLDRFile trunkFileResolved) {
        String localeID = vxmlFileResolved.getLocaleID();
        if (localeID.equals("ccp")) {
            int debug = 0;
        }
        CLDRFile vxmlFileUnresolved = vxmlFileResolved.getUnresolved();
        //System.out.println("#Dir: " + dir + ";\tLocale: " + localeID);
        Output<String> localeWhereFound = new Output<>();
        Output<String> pathWhereFound = new Output<>();
        Status status = new Status();

        int countNew = 0;
        int countChanged = 0;
        for (String path : vxmlFileUnresolved) {
            if (path.contains("/identity")) {
                continue;
            }
            if (path.contains("ccp")) {
                int debug = 0;
            }

            // get trunk value. If not accessible or ROOT_OR_CODE_FALLBACK, then set to null

            String trunkValue = null;
            if (trunkFileResolved != null) {
                trunkValue = trunkFileResolved.getStringValue(path);
                String foundId = trunkFileResolved.getSourceLocaleID(path, status);
                if (ROOT_OR_CODE_FALLBACK.contains(foundId)) {
                    trunkValue = null;
                }
            }
            String vxmlValue = vxmlFileResolved.getStringValue(path);

            // quick test; will repeat

            if (Objects.equals(vxmlValue, trunkValue)) {
                continue;
            }

            // If vxmlValue is INHERITANCE_MARKER, then get bailey
            // if ROOT_OR_CODE_FALLBACK location, then we skip

            if (vxmlValue.equals(CldrUtility.INHERITANCE_MARKER)) {
                vxmlValue = vxmlFileResolved.getBaileyValue(path, pathWhereFound, localeWhereFound);
                if (ROOT_OR_CODE_FALLBACK.contains(localeWhereFound.value)) {
                    continue;
                }
//              if (!path.equals(pathWhereFound.value)) {
//                  continue;
//              }
            }

            if (Objects.equals(vxmlValue, trunkValue)) {
                continue;
            }
            if (trunkValue == null) {
                ++countNew;
                if (verbose) {
                    System.out.println(
                        dir
                        +";\tnew\t" 
                        + "\tlocale=" + localeID + ";"
                        + "\taction=add;"
                        + "\tpath=\t" + path
                        + "\tnew_value=\t" + vxmlValue
                        );
                }
            } else {
                // we do a lot of processing on the annotations, so most likely are just changes introduced by that. 
                // So ignore for now
                if (path.startsWith("//ldml/annotations")) {
                    continue;
                }
                ++countChanged;
                if (verbose) {
                    System.out.println(
                        dir
                        + ";\ttrunk=\t" + trunkValue
                        + "\tlocale=" + localeID + ";"
                        + "\taction=add;"
                        + "\tpath=\t" + path
                        + "\tnew_value=\t" + vxmlValue
                        );
                }
            }
//            if (countNew != 0 || countChanged != 0) {
//                System.out.println("#Dir=\t" + dir + "\tLocale=\t" + localeID + "\tCountNew=\t" + countNew + "\tCountChanged=\t" + countChanged);
//            }
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
                System.out.println("##Skipping " + locale + ", no data in annotations/");
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
        System.out.println("##Total:\t" + totalCount);
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
