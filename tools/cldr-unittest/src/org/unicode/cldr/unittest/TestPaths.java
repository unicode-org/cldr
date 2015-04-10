package org.unicode.cldr.unittest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;

public class TestPaths extends TestFmwkPlus {
    static TestInfo testInfo = TestInfo.getInstance();

    public static void main(String[] args) {
        new TestPaths().run(args);
    }

    public void VerifyEnglishVsRoot() {
        Set<String> rootPaths = CollectionUtilities.addAll(testInfo
            .getCldrFactory().make("root", true).iterator(),
            new HashSet<String>());
        Set<String> englishPaths = CollectionUtilities.addAll(testInfo
            .getEnglish().iterator(), new HashSet<String>());
        englishPaths.removeAll(rootPaths);
        if (englishPaths.size() == 0) {
            return;
        }
        Factory phf = PathHeader.getFactory(testInfo.getEnglish());
        Status status = new Status();
        Set<PathHeader> suspiciousPaths = new TreeSet<PathHeader>();
        Set<PathHeader> errorPaths = new TreeSet<PathHeader>();
        Set<String> SKIP_VARIANT = new HashSet<String>(Arrays.asList(
            "ps-variant", "ug-variant", "ky-variant", "az-short",
            "Arab-variant", "am-variant", "pm-variant"));
        for (String path : englishPaths) {
            // skip aliases, other counts
            if (!status.pathWhereFound.equals(path)
                || path.contains("[@count=\"one\"]")) {
                continue;
            }
            PathHeader ph = phf.fromPath(path);
            if (ph.getSectionId() == SectionId.Special
                || ph.getCode().endsWith("-name-other")) {
                continue;
            }
            if (path.contains("@alt") && !SKIP_VARIANT.contains(ph.getCode())
                && ph.getPageId() != PageId.Alphabetic_Information) {
                errorPaths.add(ph);
            } else {
                suspiciousPaths.add(ph);
            }
        }
        if (errorPaths.size() != 0) {
            errln("Error: paths in English but not root:"
                + getPaths(errorPaths));
        }
        logln("Suspicious: paths in English but not root:"
            + getPaths(suspiciousPaths));
    }

    private String getPaths(Set<PathHeader> altPaths) {
        StringBuilder b = new StringBuilder();
        for (PathHeader path : altPaths) {
            b.append("\n\t\t")
            .append(path)
            .append(":\t")
            .append(testInfo.getEnglish().getStringValue(
                path.getOriginalPath()));
        }
        return b.toString();
    }

    public void TestGetFullPath() {
        Status status = new Status();

        for (String locale : getLocalesToTest()) {
            CLDRFile file = testInfo.getCldrFactory().make(locale, true);
            logln(locale);

            for (Iterator<String> it = file.iterator(); it.hasNext();) {
                String path = it.next();
                String fullPath = file.getFullXPath(path);
                String value = file.getStringValue(path);
                String source = file.getSourceLocaleID(path, status);
                if (fullPath == null) {
                    errln("Locale: " + locale + ",\t FullPath: " + path);
                }
                if (value == null) {
                    errln("Locale: " + locale + ",\t Value: " + path);
                }
                if (source == null) {
                    errln("Locale: " + locale + ",\t Source: " + path);
                }
                if (status.pathWhereFound == null) {
                    errln("Locale: " + locale + ",\t Found Path: " + path);
                }
            }
        }
    }

    public void TestPathHeaders() {

        Set<String> pathsSeen = new HashSet<String>();
        CLDRFile englishFile = testInfo.getCldrFactory().make("en", true);
        PathHeader.Factory phf = PathHeader.getFactory(englishFile);

        for (String locale : getLocalesToTest()) {
            CLDRFile file = testInfo.getCldrFactory().make(locale, true);
            logln("Testing path headers for locale => " + locale);

            for (Iterator<String> it = file.iterator(); it.hasNext();) {
                checkPaths(it.next(), pathsSeen, phf, locale);
            }
            for (String path : file.getExtraPaths()) {
                checkPaths(path, pathsSeen, phf, locale);
            }
        }
    }

    private void checkPaths(String path, Set<String> pathsSeen,
        PathHeader.Factory phf, String locale) {
        if (path.endsWith("/alias")) {
            return;
        }
        if (pathsSeen.contains(path)) {
            return;
        }
        pathsSeen.add(path);
        logln("Testing ==> " + path);
        String prettied = phf.fromPath(path).toString();
        String unprettied = phf.fromPath(path).getOriginalPath();
        if (!path.equals(unprettied)) {
            errln("Path Header doesn't roundtrip:\t" + path + "\t" + prettied
                + "\t" + unprettied);
        } else {
            logln(prettied + "\t" + path);
        }
    }

    private Collection<String> getLocalesToTest() {
        return params.inclusion < 5 ? Arrays.asList("root", "en", "ja", "ar")
            : params.inclusion < 10 ? testInfo.getCldrFactory()
                .getAvailableLanguages() : testInfo.getCldrFactory()
                .getAvailable();
    }

    public void TestNonLdml () {
        XPathParts parts = new XPathParts();
        PathStarrer starrer = new PathStarrer();
        StringBuilder removed = new StringBuilder();
        Set<String> nonFinalValues = new LinkedHashSet<>();
        Set<String> skipLast = new HashSet(Arrays.asList("version", "generation"));
        int counter = 0;
        for (String dir : new File(CLDRPaths.BASE_DIRECTORY + "common/").list()) {
            if (dir.equals(".DS_Store") 
                //|| ChartDelta.LDML_DIRECTORIES.contains(dir) 
                || dir.equals("dtd")  // TODO as flat files
                || dir.equals("properties") // TODO as flat files
                //|| dir.equals("uca") // TODO as flat files
                ) {
                continue;
            }
            File dir2 = new File(CLDRPaths.BASE_DIRECTORY + "common/" + dir);

            Set<Pair<String,String>> seen = new HashSet<>();
            Set<String> seenStarred = new HashSet<>();
            DtdData dtdData = null;
            DtdType type = null;
            for (String file : dir2.list()) {
                if (!file.endsWith(".xml")) {
                    continue;
                }
                String fullName = dir2 + "/" + file;
                //logln(fullName);
                for (Pair<String, String> pathValue : XMLFileReader.loadPathValues(fullName, new ArrayList<Pair<String, String>>(), true)) {
                    String path = pathValue.getFirst();
                    parts.set(path);
                    if (dtdData == null) {
                        type = DtdType.valueOf(parts.getElement(0));
                        dtdData = DtdData.getInstance(type);
                    }
                    String last = parts.getElement(-1);
                    if (skipLast.contains(last)) {
                        continue;
                    }
                    counter = removeNonDistinguishing(parts, dtdData, counter, removed, nonFinalValues);
                    String cleaned = parts.toString();
                    Pair<String, String> pair = Pair.of(type == DtdType.ldml ? file : type.toString(), cleaned);
                    if (seen.contains(pair)) {
                        errln("Duplicate: " + file + ", " + path + ", " + cleaned + ", " + pathValue.getSecond());
                    } else {
                        seen.add(pair);
                        if (!nonFinalValues.isEmpty()) {
                            String starredPath = starrer.set(path);
                            if (!seenStarred.contains(starredPath)) {
                                seenStarred.add(starredPath);
                                warnln("Non-node values: " + nonFinalValues + "\t" + path);
                            }
                        }
                        if (isVerbose()) {
                            String starredPath = starrer.set(path);
                            if (!seenStarred.contains(starredPath)) {
                                seenStarred.add(starredPath);
                                logln("@" + "\t" + cleaned + "\t" + removed);
                            }
                        }
                    }
                }
            }
        }
    }

    static final Set<String> SKIP_NON_NODE = new HashSet<>(Arrays.asList("references", "visibility", "access"));
    
    private int removeNonDistinguishing(XPathParts parts, DtdData data, int counter, StringBuilder removed, Set<String> nonFinalValues) {
        removed.setLength(0);
        nonFinalValues.clear();
        HashSet<String> toRemove = new HashSet<>();
        nonFinalValues.clear();
        int size = parts.size();
        int last = size-1;
        for (int i = 0; i < size; ++i) {
            removed.append("/");
            String element = parts.getElement(i);
            if (DtdData.isOrdered(element, null)) {
                parts.addAttribute("_q", String.valueOf(counter));
                counter++;
            }
            for (String attribute : parts.getAttributeKeys(i)) {
                if (!data.isDistinguishing(element, attribute)) {
                    toRemove.add(attribute);
                    if (i != last && !SKIP_NON_NODE.contains(attribute)) {
                        if (attribute.equals("draft")
                            && (parts.getElement(1).equals("transforms") || parts.getElement(1).equals("collations"))) {
                            // do nothing
                        } else {
                            nonFinalValues.add(attribute);
                        }
                    }
                }
            }
            if (!toRemove.isEmpty()) {
                for (String attribute : toRemove) {
                    removed.append("[@" + attribute + "=\"" + parts.getAttributeValue(i, attribute) + "\"]");
                    parts.removeAttribute(i, attribute);
                }
                toRemove.clear();
            }
        }
        return counter;
    }
}
