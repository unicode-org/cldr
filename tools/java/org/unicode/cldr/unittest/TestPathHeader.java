package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathStarrer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.Relation;

public class TestPathHeader extends TestFmwk {
    public static void main(String[] args) {
        new TestPathHeader().run(args);
    }
    static final TestInfo info = TestInfo.getInstance();
    static final Factory factory = info.getCldrFactory();
    static final CLDRFile english = info.getEnglish();
    static PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(english);

    public void TestAFile() {
        Map<String,PathHeader> uniqueness = new HashMap();
        Set<String> alreadySeen = new HashSet();
        check("en", uniqueness, alreadySeen);
        // check paths
        for (Entry<String, Set<String>> sectionAndPages : PathHeader.Factory.getCachedSectionToPages().keyValuesSet()) {
            final String section = sectionAndPages.getKey();
            logln(section); 
            for (String page : sectionAndPages.getValue()) {
                final Set<String> cachedPaths = PathHeader.Factory.getCachedPaths(section, page);
                if (cachedPaths == null) {
                    errln("Null pages for: " + section + "\t" + page);
                } else {
                    int count2 = cachedPaths.size();
                    if (count2 == 0) {
                        errln("Missing pages for: " + section + "\t" + page);
                    } else {
                        logln("\t" + page + "\t" + count2); 
                    }
                }
            }
        }
    }

    public void TestCompleteness() {
        Map<String,PathHeader> uniqueness = new HashMap();
        Set<String> alreadySeen = new HashSet();
        LanguageTagParser ltp = new LanguageTagParser();
        int count = 0;
        for (String locale : factory.getAvailable()) {
            if (!ltp.set(locale).getRegion().isEmpty()) {
                continue;
            }
            check(locale, uniqueness, alreadySeen);
            ++count;
        }
        logln("Count:\t" + count);
    }

    public void check(String localeID, Map<String,PathHeader> uniqueness, Set<String> alreadySeen) {
        CLDRFile nativeFile = factory.make(localeID, false);
        int count = 0;
        for (String path : nativeFile) {
            if (alreadySeen.contains(path)) {
                continue;
            }
            alreadySeen.add(path);
            final PathHeader pathHeader = pathHeaderFactory.fromPath(path);
            ++count;
            if (pathHeader == null) {
                errln("Null pathheader for " + path);
            } else {
                String visible = pathHeader.toString();
                PathHeader old = uniqueness.get(visible);
                if (old == null) {
                    if (pathHeader.getSection().equals("Special")) {
                        if (pathHeader.getSection().equals("Unknown")) {
                            errln("PathHeader has fallback: " + visible + "\t" + pathHeader.getOriginalPath());
                        } else {
                            logln("Special:\t" + visible + "\t" + pathHeader.getOriginalPath());
                        }
                    }
                    uniqueness.put(visible, pathHeader);
                } else if (!old.equals(pathHeader)) {
                    errln("PathHeader not unique: " + visible + "\t" + pathHeader.getOriginalPath() + "\t" + old.getOriginalPath());
                }
            }
        }
        logln(localeID + "\t" + count);
    }

    public void check() {
        PathStarrer pathStarrer = new PathStarrer();
        pathStarrer.setSubstitutionPattern("%A");

        Set<PathHeader> sorted = new TreeSet<PathHeader>();
        Map<String, String> missing = new TreeMap<String, String>();
        Map<String, String> skipped = new TreeMap<String, String>();
        Map<String, String> collide = new TreeMap<String, String>();

        System.out.println("Traversing Paths");
        for (String path : english) {
            PathHeader pathHeader = pathHeaderFactory.fromPath(path);
            String value = english.getStringValue(path);
            if (pathHeader == null) {
                final String starred = pathStarrer.set(path);
                missing.put(starred, value + "\t" + path);
                continue;
            }
            if (pathHeader.getSection().equalsIgnoreCase("skip")) {
                final String starred = pathStarrer.set(path);
                skipped.put(starred, value + "\t" + path);
                continue;
            }
            sorted.add(pathHeader);
        }
        System.out.println("\nConverted:\t" + sorted.size());
        String lastHeader = "";
        String lastPage = "";
        String lastSection = "";
        List<String> threeLevel = new ArrayList<String>();
        for (PathHeader pathHeader : sorted) {
            if (!lastSection.equals(pathHeader.getSection())) {
                System.out.println();
                threeLevel.add(pathHeader.getSection());
                threeLevel.add("\t" + pathHeader.getPage());
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastSection = pathHeader.getSection();
                lastPage = pathHeader.getPage();
                lastHeader = pathHeader.getHeader();
            } else if (!lastPage.equals(pathHeader.getPage())) {
                System.out.println();
                threeLevel.add("\t" + pathHeader.getPage());
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastPage = pathHeader.getPage();
                lastHeader = pathHeader.getHeader();
            } else if (!lastHeader.equals(pathHeader.getHeader())) {
                System.out.println();
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastHeader = pathHeader.getHeader();
            }
            System.out.println(pathHeader + ";\t"
                    + english.getStringValue(pathHeader.getOriginalPath()) + "\t"
                    + pathHeader.getOriginalPath());
        }
        System.out.println("\nCollide:\t" + collide.size());
        for (Entry<String, String> item : collide.entrySet()) {
            System.out.println("\t" + item);
        }
        System.out.println("\nMissing:\t" + missing.size());
        for (Entry<String, String> item : missing.entrySet()) {
            System.out.println("\t" + item.getKey() + "\tvalue:\t" + item.getValue());
        }
        System.out.println("\nSkipped:\t" + skipped.size());
        for (Entry<String, String> item : skipped.entrySet()) {
            System.out.println("\t" + item);
        }
        Counter<PathHeader.Factory.CounterData> counterData = pathHeaderFactory.getInternalCounter();
        System.out.println("\nInternal Counter:\t" + counterData.size());
        for (PathHeader.Factory.CounterData item : counterData.keySet()) {
            System.out.println("\t" + counterData.getCount(item)
                    + "\t" + item.get2() // externals
                    + "\t" + item.get3()
                    + "\t" + item.get0() // internals
                    + "\t" + item.get1()
            );
        }
        System.out.println("\nMenus/Headers:\t" + threeLevel.size());
        for (String item : threeLevel) {
            System.out.println(item);
        }
        LinkedHashMap<String, Set<String>> sectionsToPages = pathHeaderFactory.getSectionsToPages();
        System.out.println("\nMenus:\t" + sectionsToPages.size());
        for (Entry<String, Set<String>> item : sectionsToPages.entrySet()) {
            final String section = item.getKey();
            for (String page : item.getValue()) {
                System.out.print("\t" + section + "\t" + page);
                int count = 0;
                for (String path : pathHeaderFactory.filterCldr(section, page, english)) {
                    count += 1; // just count them.
                }
                System.out.println("\t" + count);
            }
        }
    }

}
