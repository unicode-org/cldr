package org.unicode.cldr.unittest;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.ant.CLDRBuild.CoverageLevel;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SimpleFactory;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.Output;

public class TestCLDRFile extends TestFmwk {
    public static void main(String[] args) {
        new TestCLDRFile().run(args);
    }

    public static Factory getAllFactory() {
        File mainDir = new File(CldrUtility.MAIN_DIRECTORY);
        if (!mainDir.isDirectory()) {
            throw new IllegalArgumentException("MAIN_DIRECTORY is not a directory: " + CldrUtility.MAIN_DIRECTORY);
        }
        File seedDir = new File(CldrUtility.SEED_DIRECTORY);
        if (!seedDir.isDirectory()) {
            throw new IllegalArgumentException("SEED_DIRECTORY is not a directory: " + CldrUtility.SEED_DIRECTORY);
        }
        File dirs[] = { mainDir, seedDir };
        return SimpleFactory.make(dirs, ".*", DraftStatus.approved);
    }

    public void testExtraPaths() {
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*", DraftStatus.approved);
        for (String locale : new String[] { "en", "ar", "ja" }) {
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            Set<String> s = (Set<String>) cldrFile.getExtraPaths(new TreeSet<String>());
            System.out.println("Extras for " + locale);
            for (String path : s) {
                System.out.println(path + " => " + cldrFile.getStringValue(path));
            }
            System.out.println("Already in " + locale);
            for (Iterator<String> it = cldrFile.iterator(Pattern.compile(".*\\[@count=.*").matcher("")); it.hasNext();) {
                String path = it.next();
                System.out.println(path + " => " + cldrFile.getStringValue(path));
            }
        }
    }

    public void testDraftFilter() {
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*", DraftStatus.approved);
        checkLocale(cldrFactory.make("root", true));
        checkLocale(cldrFactory.make("ee", true));
    }

    public void checkLocale(CLDRFile cldr) {
        Matcher m = Pattern.compile("gregorian.*eras").matcher("");
        for (Iterator<String> it = cldr.iterator("", new UTF16.StringComparator()); it.hasNext();) {
            String path = it.next();
            if (m.reset(path).find() && !path.contains("alias")) {
                System.out.println(cldr.getLocaleID() + "\t" + cldr.getStringValue(path) + "\t"
                    + cldr.getFullXPath(path));
            }
            if (path == null) {
                throw new IllegalArgumentException("Null path");
            }
            String fullPath = cldr.getFullXPath(path);
            if (fullPath.contains("@draft")) {
                throw new IllegalArgumentException("File can't contain draft elements");
            }
        }
    }

    public void testTimeZonePath() {
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        String tz = "Pacific/Midway";
        CLDRFile cldrFile = cldrFactory.make("lv", true);
        String retVal = cldrFile.getStringValue(
            "//ldml/dates/timeZoneNames/zone[@type=\"" + tz + "\"]/exemplarCity"
            , true).trim();
        System.out.println(retVal);
    }

    public void testSimple() {
        double deltaTime = System.currentTimeMillis();
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile english = cldrFactory.make("en", true);
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Creation: Elapsed: " + deltaTime / 1000.0 + " seconds");

        deltaTime = System.currentTimeMillis();
        english.getStringValue("//ldml");
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Creation: Elapsed: " + deltaTime / 1000.0 + " seconds");

        deltaTime = System.currentTimeMillis();
        english.getStringValue("//ldml");
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Caching: Elapsed: " + deltaTime / 1000.0 + " seconds");

        deltaTime = System.currentTimeMillis();
        for (int j = 0; j < 2; ++j) {
            for (Iterator<String> it = english.iterator(); it.hasNext();) {
                String dpath = it.next();
                String value = english.getStringValue(dpath);
                Set<String> paths = english.getPathsWithValue(value, null, null, null);
                if (!paths.contains(dpath)) {
                    throw new IllegalArgumentException(paths + " don't contain <" + value + ">.");
                }
                if (false && paths.size() > 1) {
                    System.out.println("Value: " + value + "\t\tPaths: " + paths);
                }
            }
        }
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
    }

    public void testResolution() {
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile german = cldrFactory.make("de", true);
        // Test direct lookup.
        String xpath = "//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator";
        String id = german.getSourceLocaleID(xpath, null);
        if (!id.equals("de")) {
            throw new RuntimeException("Expected de but was " + id + " for " + xpath);
        }

        // Test aliasing.
        xpath = "//ldml/dates/calendars/calendar[@type=\"islamic-civil\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"yyyyMEd\"]";
        id = german.getSourceLocaleID(xpath, null);
        if (!id.equals("de")) {
            throw new RuntimeException("Expected de but was " + id + " for " + xpath);
        }

        // Test lookup that falls to root.
        xpath = "//ldml/dates/calendars/calendar[@type=\"coptic\"]/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"narrow\"]/month[@type=\"5\"]";
        id = german.getSourceLocaleID(xpath, null);
        if (!id.equals("root")) {
            throw new RuntimeException("Expected root but was " + id + " for " + xpath);
        }
    }

    static final NumberFormat percent = NumberFormat.getPercentInstance();
    static final class Size {
        int items;
        int chars;
        public void add(String topValue) {
            items++;
            chars += topValue.length();
        }
        public String over(Size base) {
            return "items: " + items + "(" + percent.format(items/(0.0+base.items)) + "); " +
            		"chars: " + chars + "(" + percent.format(chars/(0.0+base.chars)) + ")";
        }
    }
    
    public void testGeorgeBailey() {
        Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        PathHeader.Factory phf = PathHeader.getFactory(cldrFactory.make("en", true));
        for (String locale : Arrays.asList("de", "de_AT", "en", "nl")) {
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(locale);

            //CLDRFile parentFile = cldrFactory.make(LocaleIDParser.getParent(locale), true);
            CLDRFile cldrFileUnresolved = cldrFactory.make(locale, false);
            Status status = new Status();
            Output<String> localeWhereFound = new Output<String>();
            Output<String> pathWhereFound = new Output<String>();

            Map<String,String> diff = new TreeMap<String,String>(CLDRFile.ldmlComparator);
            
            Size countSuperfluous = new Size();
            Size countExtraLevel = new Size();
            Size countOrdinary = new Size();
            
            for (String path : cldrFile.fullIterable()) {
                String baileyValue = cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound );
                String topValue = cldrFileUnresolved.getStringValue(path);
                String resolvedValue = cldrFile.getStringValue(path);

                // if there is a value, then either it is at the top level or it is the bailey value.

                if (resolvedValue != null) {
                    if (topValue != null) {
                        assertEquals("top≠resolved\t" + locale + "\t" + phf.fromPath(path), topValue, resolvedValue);
                    } else {
                        String locale2 = cldrFile.getSourceLocaleID(path, status);
                        assertEquals("bailey value≠\t" + locale + "\t" + phf.fromPath(path), resolvedValue, baileyValue);
                        assertEquals("bailey locale≠\t" + locale + "\t" + phf.fromPath(path), locale2, localeWhereFound.value);
                        assertEquals("bailey path≠\t" + locale + "\t" + phf.fromPath(path), status.pathWhereFound, pathWhereFound.value);
                    }
                }

                if (topValue != null) {
                    if (CldrUtility.equals(topValue, baileyValue)) {
                        countSuperfluous.add(topValue);
                    } else if (coverageLevel.getLevel(path).compareTo(Level.MODERN) > 0) {
                        countExtraLevel.add(topValue);
                    }                        
                    countOrdinary.add(topValue);

                    
//                    String parentValue = parentFile.getStringValue(path);
//                    if (!CldrUtility.equals(parentValue, baileyValue)) {
//                        diff.put(path, "parent=" + parentValue + ";\tbailey=" + baileyValue);
//                    }
                }
            }
            warnln("Superfluous (" + locale + "):\t" + countSuperfluous.over(countOrdinary));
            warnln(">Modern (" + locale + "):\t" + countExtraLevel.over(countOrdinary));
            for (Entry<String, String> entry : diff.entrySet()) {
                System.out.println(locale + "\t" + phf.fromPath(entry.getKey()) + ";\t" + entry.getValue());
            }
        }
    }
}