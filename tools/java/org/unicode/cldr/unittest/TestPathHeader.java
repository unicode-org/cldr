package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.Relation;

public class TestPathHeader extends TestFmwk {
    public static void main(String[] args) {
        new TestPathHeader().run(args);
    }

    static final TestInfo             info              = TestInfo.getInstance();
    static final Factory              factory           = info.getCldrFactory();
    static final CLDRFile             english           = info.getEnglish();
    static final SupplementalDataInfo supplemental      = info.getSupplementalDataInfo();
    static PathHeader.Factory         pathHeaderFactory = PathHeader.getFactory(english);

    public void TestAFile() {
        final String localeId = "en";
        CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(localeId);
        Counter<Level> counter = new Counter();
        Map<String, PathHeader> uniqueness = new HashMap();
        Set<String> alreadySeen = new HashSet();
        check(localeId, true, uniqueness, alreadySeen);
        // check paths
        for (Entry<SectionId, Set<PageId>> sectionAndPages : PathHeader.Factory
                .getSectionIdsToPageIds().keyValuesSet()) {
            final SectionId section = sectionAndPages.getKey();
            logln(section.toString());
            for (PageId page : sectionAndPages.getValue()) {
                final Set<String> cachedPaths = PathHeader.Factory.getCachedPaths(section, page);
                if (cachedPaths == null) {
                    errln("Null pages for: " + section + "\t" + page);
                } else {
                    int count2 = cachedPaths.size();
                    if (count2 == 0) {
                        errln("Missing pages for: " + section + "\t" + page);
                    } else {
                        counter.clear();
                        for (String s : cachedPaths) {
                            Level coverage = coverageLevel.getLevel(s);
                            counter.add(coverage, 1);
                        }
                        String countString = "";
                        int total = 0;
                        for (Level item : Level.values()) {
                            long count = counter.get(item);
                            if (count != 0) {
                                if (!countString.isEmpty()) {
                                    countString += ",\t+";
                                }
                                total += count;
                                countString += item + "=" + total;
                            }
                        }
                        logln("\t" + page + "\t" + countString);
                        if (page.toString().startsWith("Unknown")) {
                            logln("\t\t" + cachedPaths);
                        }
                    }
                }
            }
        }
    }
    public void TestMetazones() {

        CLDRFile nativeFile = factory.make("en", true);
        PathStarrer starrer = new PathStarrer();
        Set<PathHeader> pathHeaders = getPathHeaders(nativeFile);
        String oldPage = "";
        String oldHeader = "";
        for (PathHeader entry : pathHeaders) {
            final String page = entry.getPage();
            //            if (!oldPage.equals(page)) {
            //                logln(page);
            //                oldPage = page;
            //            }
            String header = entry.getHeader();
            if (!oldHeader.equals(header)) {
                logln(page + "\t" + header);
                oldHeader = header;
            }
        }
    }
    
    public Set<PathHeader> getPathHeaders(CLDRFile nativeFile) {
        Set<PathHeader> pathHeaders = new TreeSet<PathHeader>();
        for (String path : nativeFile.fullIterable()) {
            PathHeader p = pathHeaderFactory.fromPath(path);
            if (p.getSection().startsWith("Time")) {
                pathHeaders.add(p);
            }
        }
        return pathHeaders;
    }

    public void TestStatus() {
        CLDRFile nativeFile = factory.make("en", true);
        PathStarrer starrer = new PathStarrer();
        EnumMap<SurveyToolStatus, Relation<String, String>> info2 = new EnumMap<SurveyToolStatus, Relation<String, String>>(
                SurveyToolStatus.class);
        Counter<SurveyToolStatus> counter = new Counter<SurveyToolStatus>();
        Set<String> nuked = new HashSet<String>();
        PrettyPath pp = new PrettyPath();
        XPathParts parts = new XPathParts();
        Set<String> deprecatedStar = new HashSet<String>();
        Set<String> differentStar = new HashSet<String>();

        for (String path : nativeFile.fullIterable()) {

            PathHeader p = pathHeaderFactory.fromPath(path);
            final SurveyToolStatus surveyToolStatus = p.getSurveyToolStatus();
            final SurveyToolStatus tempSTS = surveyToolStatus == SurveyToolStatus.DEPRECATED ? SurveyToolStatus.HIDE
                    : surveyToolStatus;
            String starred = starrer.set(path);
            List<String> attr = starrer.getAttributes();
            if (surveyToolStatus != SurveyToolStatus.READ_WRITE) {
                nuked.add(starred);
            }

            // check against old
            SurveyToolStatus oldStatus = SurveyToolStatus.READ_WRITE;
            String prettyPath = pp.getPrettyPath(path);

            if (prettyPath.contains("numberingSystems") ||
                    prettyPath.contains("exemplarCharacters") ||
                    prettyPath.contains("indexCharacters")) {
                oldStatus = SurveyToolStatus.READ_ONLY;
            } else if (CheckCLDR.skipShowingInSurvey.matcher(path).matches()) {
                oldStatus = SurveyToolStatus.HIDE;
            }

            if (tempSTS != oldStatus && oldStatus != SurveyToolStatus.READ_WRITE) {
                if (!differentStar.contains(starred)) {
                    errln("Different from old:\t" + oldStatus + "\t" + surveyToolStatus + "\t"
                            + path);
                    differentStar.add(starred);
                }
            }

            // check against deprecated
            boolean isDeprecated = supplemental.hasDeprecatedItem("ldml", parts.set(path));
            if (isDeprecated != (surveyToolStatus == SurveyToolStatus.DEPRECATED)) {
                if (!deprecatedStar.contains(starred)) {
                    errln("Different from supplementalMetadata deprecated:\t" + isDeprecated + "\t"
                            + surveyToolStatus + "\t" + path);
                    deprecatedStar.add(starred);
                }
            }

            Relation<String, String> data = info2.get(surveyToolStatus);
            if (data == null) {
                info2.put(surveyToolStatus,
                        data = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
            }
            data.put(starred, CollectionUtilities.join(attr, "|"));
        }
        for (Entry<SurveyToolStatus, Relation<String, String>> entry : info2.entrySet()) {
            final SurveyToolStatus status = entry.getKey();
            for (Entry<String, Set<String>> item : entry.getValue().keyValuesSet()) {
                final String starred = item.getKey();
                if (status == SurveyToolStatus.READ_WRITE && !nuked.contains(starred)) {
                    continue;
                }
                logln(status + "\t" + starred + "\t" + item.getValue());
            }
        }
    }
    
    public void TestPathDescriptionCompleteness() {
        PathDescription pathDescription = new PathDescription(supplemental, english, null, null, PathDescription.ErrorHandling.CONTINUE);
        for (PathHeader pathHeader : getPathHeaders(english)) {
            String path = pathHeader.getOriginalPath();
            String value = english.getStringValue(path);
            String description = pathDescription.getDescription(path, value, null, null);
            if (description == null) {
                errln("Path has no description:\t" + value + "\t" + path);
            } else if (!description.contains("http://")) {
                errln("Description has no URL:\t" + description + "\t" + value + "\t" + path);
            } else if (description == PathDescription.MISSING_DESCRIPTION) {
                logln("Fallback Description:\t" + value + "\t" + path);
            }
        }
    }

    public void TestZCompleteness() {
        Map<String, PathHeader> uniqueness = new HashMap();
        Set<String> alreadySeen = new HashSet();
        LanguageTagParser ltp = new LanguageTagParser();
        int count = 0;
        for (String locale : factory.getAvailable()) {
            if (!ltp.set(locale).getRegion().isEmpty()) {
                continue;
            }
            check(locale, false, uniqueness, alreadySeen);
            ++count;
        }
        logln("Count:\t" + count);
    }

    public void check(String localeID, boolean resolved, Map<String, PathHeader> uniqueness,
            Set<String> alreadySeen) {
        CLDRFile nativeFile = factory.make(localeID, resolved);
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
                            errln("PathHeader has fallback: " + visible + "\t"
                                    + pathHeader.getOriginalPath());
                            // } else {
                            // logln("Special:\t" + visible + "\t" +
                            // pathHeader.getOriginalPath());
                        }
                    }
                    uniqueness.put(visible, pathHeader);
                } else if (!old.equals(pathHeader)) {
                    errln("PathHeader not unique: " + visible + "\t" + pathHeader.getOriginalPath()
                            + "\t" + old.getOriginalPath());
                }
            }
        }
        logln(localeID + "\t" + count);
    }

    public void TestZ() {
        PathStarrer pathStarrer = new PathStarrer();
        pathStarrer.setSubstitutionPattern("%A");

        Set<PathHeader> sorted = new TreeSet<PathHeader>();
        Map<String, String> missing = new TreeMap<String, String>();
        Map<String, String> skipped = new TreeMap<String, String>();
        Map<String, String> collide = new TreeMap<String, String>();

        logln("Traversing Paths");
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
        logln("\nConverted:\t" + sorted.size());
        String lastHeader = "";
        String lastPage = "";
        String lastSection = "";
        List<String> threeLevel = new ArrayList<String>();
        Status status = new Status();
        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance("en");

        for (PathHeader pathHeader : sorted) {
            String original = pathHeader.getOriginalPath();
            String sourceLocale = english.getSourceLocaleID(original, status);
            if (!original.equals(status.pathWhereFound)) {
                continue;
            }
            if (!lastSection.equals(pathHeader.getSection())) {
                logln("");
                threeLevel.add(pathHeader.getSection());
                threeLevel.add("\t" + pathHeader.getPage());
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastSection = pathHeader.getSection();
                lastPage = pathHeader.getPage();
                lastHeader = pathHeader.getHeader();
            } else if (!lastPage.equals(pathHeader.getPage())) {
                logln("");
                threeLevel.add("\t" + pathHeader.getPage());
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastPage = pathHeader.getPage();
                lastHeader = pathHeader.getHeader();
            } else if (!lastHeader.equals(pathHeader.getHeader())) {
                logln("");
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastHeader = pathHeader.getHeader();
            }
            logln(pathHeader
                    + "\t" + coverageLevel2.getLevel(original)
                    + "\t" + english.getStringValue(pathHeader.getOriginalPath())
                    + "\t" + pathHeader.getOriginalPath());
        }
        if (collide.size() != 0) {
            errln("\nCollide:\t" + collide.size());
            for (Entry<String, String> item : collide.entrySet()) {
                errln("\t" + item);
            }
        }
        if (missing.size() != 0) {
            errln("\nMissing:\t" + missing.size());
            for (Entry<String, String> item : missing.entrySet()) {
                errln("\t" + item.getKey() + "\tvalue:\t" + item.getValue());
            }
        }
        if (skipped.size() != 0) {
            errln("\nSkipped:\t" + skipped.size());
            for (Entry<String, String> item : skipped.entrySet()) {
                errln("\t" + item);
            }
        }
        Counter<PathHeader.Factory.CounterData> counterData = pathHeaderFactory
        .getInternalCounter();
        logln("\nInternal Counter:\t" + counterData.size());
        for (PathHeader.Factory.CounterData item : counterData.keySet()) {
            logln("\t" + counterData.getCount(item)
                    + "\t" + item.get2() // externals
                    + "\t" + item.get3()
                    + "\t" + item.get0() // internals
                    + "\t" + item.get1()
            );
        }
        logln("\nMenus/Headers:\t" + threeLevel.size());
        for (String item : threeLevel) {
            logln(item);
        }
        LinkedHashMap<String, Set<String>> sectionsToPages = pathHeaderFactory.getSectionsToPages();
        logln("\nMenus:\t" + sectionsToPages.size());
        for (Entry<String, Set<String>> item : sectionsToPages.entrySet()) {
            final String section = item.getKey();
            for (String page : item.getValue()) {
                logln("\t" + section + "\t" + page);
                int count = 0;
                for (String path : pathHeaderFactory.filterCldr(section, page, english)) {
                    count += 1; // just count them.
                }
                logln("\t" + count);
            }
        }
    }

}
