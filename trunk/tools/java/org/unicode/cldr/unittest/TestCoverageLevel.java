package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CoverageLevel;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ULocale;

public class TestCoverageLevel extends TestFmwk {

    private static TestInfo testInfo = TestInfo.getInstance();

    private CoverageLevel coverageLevel1 = new CoverageLevel(testInfo.getCldrFactory());


    public static void main(String[] args) throws IOException {
        new TestCoverageLevel().getStarred("fr");
        if (true) return;
        new TestCoverageLevel().getOrgs();
        new TestCoverageLevel().run(args);
    }

    private void getOrgs() {
        StandardCodes sc = StandardCodes.make();
        Set<R4<String, Level, String, String>> sorted = new TreeSet<R4<String, Level, String, String>>();
        Map<String, String> mapped = new TreeMap();
        for (String org : sc.getLocaleCoverageOrganizations()) {
            for (String locale : sc.getLocaleCoverageLocales(org)) {
                Level level = sc.getLocaleCoverageLevel(org, locale);
                final String name = testInfo.getEnglish().getName(locale);
                final R4<String, Level, String, String> row = Row.of(org, level, locale, name);
                mapped.put(name, locale);
                sorted.add(row);
            }
        }
        for (R4<String, Level, String, String> item : sorted) {
            System.out.println(item);
        }

        for (String org : sc.getLocaleCoverageOrganizations()) {
            System.out.print("\t" + org);
        }
        System.out.println();

        for (Entry<String, String> entry : mapped.entrySet()) {
            String name = entry.getKey();
            String locale = entry.getValue();
            System.out.print(name);
            for (String org : sc.getLocaleCoverageOrganizations()) {
                Level level = sc.getLocaleCoverageLevel(org, locale);
                System.out.print("\t" + (level == Level.UNDETERMINED ? "n/a" : level));
            }
            System.out.println();
        }
    }

    public void TestNewVsOld() {
        checkNewVsOld("en");
    }

    private void checkNewVsOld(String locale) {
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        ULocale ulocale = new ULocale(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,true);
        coverageLevel1.setFile(cldrFileToCheck, options, null, possibleErrors);

        Map<Row.R2<Level, Level>, Relation<String, String>> failures = new TreeMap<Row.R2<Level, Level>, Relation<String, String>>(); // Relation.of(new HashMap<Row.R2<Level, Integer>, Set<Relation<String,String>>>(), HashSet.class);

        int failureCount = 0;
        int successCount = 0;
        int count = 0;
        PathStarrer pathStarrer = new PathStarrer();

        for (String path : cldrFileToCheck) {
            ++count;
            if (0 == (count & 0xFF)) {
                logln(count + ", " + successCount + ", " + failureCount);
            }
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
//            if (path.contains("ethiopic")) {
//                System.out.println("?");
//            }
            Level level = coverageLevel1.getCoverageLevel(fullPath);     
            
            Level newLevel = Level.fromLevel(testInfo.getSupplementalDataInfo().getCoverageValue(path, ulocale));
            if (newLevel != level) {
                Level exceptionLevel = exceptions.get(fullPath);
                if (newLevel == exceptionLevel) {
                    level = exceptionLevel;
                    ++successCount;
                } else {
                    ++failureCount;
                }
            } else {
                ++successCount;
            }
            R2<Level, Level> key = Row.of(level, newLevel);
            String starredPath = pathStarrer.set(path);
            Relation<String, String> starredToAttributes = failures.get(key);
            if (starredToAttributes == null) {
                failures.put(key, starredToAttributes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
            }
            starredToAttributes.put(starredPath, pathStarrer.getAttributesString("|"));
        }
        if (failureCount != 0) {
            errln("Differences between new and old: " + failureCount + ", same: " + successCount);
        } else {
            logln("Same value: " + successCount);            
        }
        if (params.verbose) {
            for (int i = 0; i < 2; ++i) {
                for (Entry<R2<Level, Level>, Relation<String, String>> entry : failures.entrySet()) {
                    R2<Level, Level> levels = entry.getKey();
                    Relation<String, String> starredToAttributes = entry.getValue();
                    Level oldLevel = levels.get0();
                    Level newLevel = levels.get1();
                    if ((i == 0) == (oldLevel == newLevel)) {
                        continue;
                    }
                    //logln("\tCount:\t" + starredToAttributes.values().size() + ",\tOld level:\t" + oldLevel + " (=" + oldLevelInt + ")" + ",\tNew level:\t" + newLevel);
                    //int maxCount = 10;
                    for (Entry<String, Set<String>> s : starredToAttributes.keyValuesSet()) {
                        //                    if (--maxCount < 0) {
                        //                        logln("\t...");
                        //                        break;
                        //                    }
                        String valueSample = s.getValue().toString();
                        if (valueSample.length() > 100) {
                            valueSample = valueSample.substring(0,100) + "…";
                        }
                        logln((oldLevel == null ? "?" : oldLevel.getLevel()) 
                                + "\t" + (newLevel == null ? "?" : newLevel.getLevel())
                                + "\t" + s.getKey() 
                                + "\t" + valueSample);
                    }
                }
            }
        }
    }
    
    private static void getStarred(String locale) {
        ULocale ulocale = new ULocale(locale);
        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,true);

        Map<Level, Relation<String, String>> data = new TreeMap<Level, Relation<String, String>>(); // Relation.of(new HashMap<Row.R2<Level, Integer>, Set<Relation<String,String>>>(), HashSet.class);

        int count = 0;
        PathStarrer pathStarrer = new PathStarrer();
        Status status = new Status();
        
        for (String path : cldrFileToCheck) {
            ++count;
            if (path.contains("/alias")) {
                continue;
            }
            String localeFound = cldrFileToCheck.getSourceLocaleID(path, status);
            if (status.pathWhereFound != path) {
                continue;
            }
            
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
//            if (path.contains("ethiopic")) {
//                System.out.println("?");
//            }
            Level level = coverageLevel2.getLevel(path);
            
            //R2<Level, Level> key = Row.of(level, newLevel);
            String starredPath = pathStarrer.set(path);
            Relation<String, String> starredToAttributes = data.get(level);
            if (starredToAttributes == null) {
                data.put(level, starredToAttributes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
            }
            starredToAttributes.put(starredPath, pathStarrer.getAttributesString("|"));
        }
        for (Entry<Level, Relation<String, String>> entry : data.entrySet()) {
            final Level level = entry.getKey();
            for (Entry<String, Set<String>> entry2 : entry.getValue().keyValuesSet()) {
                System.out.println(level.getLevel() + "\t" + level + "\t" + entry2.getKey() + "\t" + entry2.getValue());
            }
        }
    }

    public void TestTime() { 
        checkTime("en");
    }

    private void checkTime(String locale) {
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        ULocale ulocale = new ULocale(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,true);

        Timer t = new Timer();
        coverageLevel1.setFile(cldrFileToCheck, options, null, possibleErrors);
        for (String path : cldrFileToCheck) {
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            Level level = coverageLevel1.getCoverageLevel(fullPath);            
        }
        long oldTime = t.getDuration();
        logln("Old time:\t" + t.toString());

        t.start();
        coverageLevel1.setFile(cldrFileToCheck, options, null, possibleErrors);
        for (String path : cldrFileToCheck) {
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            int newLevel = testInfo.getSupplementalDataInfo().getCoverageValue(path, ulocale);
        }
        double newTime = t.getDuration();
        if (newTime > 2*oldTime) {
            errln("New Coverage Level time too big " + t.toString(1, oldTime));
        } else {
            logln("Old time:\t" + t.toString(1, oldTime));
        }
    }



    
    RegexLookup<Level> exceptions = RegexLookup.of(null, new Transform<String,Level>() {
        public Level transform(String source) {
            return Level.fromLevel(Integer.parseInt(source));
        }
    }, null)
    .loadFromFile(TestCoverageLevel.class, "TestCoverageLevel.txt");
    {
        for (R2<Finder, Level> x : exceptions) {
            System.out.println(x.get0().toString() + " => " + x.get1());
        }
    }
}