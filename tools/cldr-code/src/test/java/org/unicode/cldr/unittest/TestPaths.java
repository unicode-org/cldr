package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.ChainedMap.M5;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdData.ElementType;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

public class TestPaths extends TestFmwkPlus {
    static final CLDRConfig testInfo = CLDRConfig.getInstance();
    static final SupplementalDataInfo SDI =
            testInfo.getSupplementalDataInfo(); // Load first, before XPartPaths is called

    public static void main(String[] args) {
        new TestPaths().run(args);
    }

    public void VerifyEnglishVsRoot() {
        HashSet<String> rootPaths = new HashSet<>();
        testInfo.getRoot().forEach(rootPaths::add);
        HashSet<String> englishPaths = new HashSet<>();
        testInfo.getEnglish().forEach(englishPaths::add);
        englishPaths.removeAll(rootPaths);
        if (englishPaths.size() == 0) {
            return;
        }
        Factory phf = PathHeader.getFactory(testInfo.getEnglish());
        Status status = new Status();
        Set<PathHeader> suspiciousPaths = new TreeSet<>();
        Set<PathHeader> errorPaths = new TreeSet<>();
        ImmutableSet<String> SKIP_VARIANT =
                ImmutableSet.of(
                        "ps-variant",
                        "ug-variant",
                        "ky-variant",
                        "az-short",
                        "Arab-variant",
                        "am-variant",
                        "pm-variant");
        for (String path : englishPaths) {
            // skip aliases, other counts
            if (!status.pathWhereFound.equals(path) || path.contains("[@count=\"one\"]")) {
                continue;
            }
            PathHeader ph = phf.fromPath(path);
            if (ph.getSectionId() == SectionId.Special || ph.getCode().endsWith("-name-other")) {
                continue;
            }
            if (path.contains("@alt")
                    && !SKIP_VARIANT.contains(ph.getCode())
                    && ph.getPageId() != PageId.Alphabetic_Information) {
                errorPaths.add(ph);
            } else {
                suspiciousPaths.add(ph);
            }
        }
        if (errorPaths.size() != 0) {
            errln("Error: paths in English but not root:" + getPaths(errorPaths));
        }
        logln("Suspicious: paths in English but not root:" + getPaths(suspiciousPaths));
    }

    private String getPaths(Set<PathHeader> altPaths) {
        StringBuilder b = new StringBuilder();
        for (PathHeader path : altPaths) {
            b.append("\n\t\t")
                    .append(path)
                    .append(":\t")
                    .append(testInfo.getEnglish().getStringValue(path.getOriginalPath()));
        }
        return b.toString();
    }

    /**
     * For each locale to test, loop through all the paths, including "extra" paths, checking for
     * each path: checkFullpathValue; checkPrettyPaths
     */
    public void TestPathHeadersAndValues() {
        /*
         * Use the pathsSeen hash to keep track of which paths have
         * already been seen. Since the test checkPrettyPaths isn't really
         * locale-dependent, run it only once for each path, for the first
         * locale in which the path occurs.
         */
        Set<String> pathsSeen = new HashSet<>();
        CLDRFile englishFile = testInfo.getCldrFactory().make("en", true);
        PathHeader.Factory phf = PathHeader.getFactory(englishFile);
        Status status = new Status();
        for (String locale : getLocalesToTest()) {
            if (!StandardCodes.isLocaleAtLeastBasic(locale)) {
                continue;
            }
            CLDRFile file = testInfo.getCLDRFile(locale, true);
            logln("Testing path headers and values for locale => " + locale);
            final Collection<String> extraPaths = file.getExtraPaths();
            for (Iterator<String> it = file.iterator(); it.hasNext(); ) {
                String path = it.next();
                if (extraPaths.contains(path)) {
                    continue;
                }
                checkFullpathValue(path, file, locale, status, false /* not extra path */);
                if (!pathsSeen.contains(path)) {
                    pathsSeen.add(path);
                    checkPrettyPaths(path, phf);
                }
            }
            for (String path : extraPaths) {
                checkFullpathValue(path, file, locale, status, true /* extra path */);
                if (!pathsSeen.contains(path)) {
                    pathsSeen.add(path);
                    checkPrettyPaths(path, phf);
                }
            }
        }
    }

    /**
     * For the given path and CLDRFile, check that fullPath, value, and source are all non-null.
     *
     * <p>Allow null value for some exceptional extra paths.
     *
     * @param path the path, such as '//ldml/dates/fields/field[@type="tue"]/relative[@type="1"]'
     * @param file the CLDRFile
     * @param locale the locale string
     * @param status the Status to be used/set by getSourceLocaleID
     * @param isExtraPath true if the path is an "extra" path, else false
     */
    private void checkFullpathValue(
            String path, CLDRFile file, String locale, Status status, boolean isExtraPath) {
        String fullPath = file.getFullXPath(path);
        String value = file.getStringValue(path);
        String source = file.getSourceLocaleID(path, status);

        assertEquals("CanonicalOrder", XPathParts.getFrozenInstance(path).toString(), path);

        if (fullPath == null) {
            errln("Locale: " + locale + ",\t Null FullPath: " + path);
        } else if (!path.equals(fullPath)) {
            assertEquals(
                    "CanonicalOrder (FP)",
                    XPathParts.getFrozenInstance(fullPath).toString(),
                    fullPath);
        }

        if (value == null) {
            if (allowsExtraPath(path, isExtraPath)) {
                return;
            }
            if (CldrUtility.INHERITANCE_MARKER.equals(
                    file.getUnresolved().getStringValue(fullPath))) {
                logKnownIssue(
                        "cldrbug:16209", "Remove this clause (and any paths that still fail)");
                // When that ticket is resolved, then comment this clause out.
                // But leave that comment for future guidance where someone wants to move paths from
                // root to extraPaths.
                return;
            }
            errln(
                    "Locale: "
                            + locale
                            + ",\t Value=null, \tPath: "
                            + path
                            + ",\t IsExtraPath: "
                            + isExtraPath);
        }

        if (source == null) {
            errln("Locale: " + locale + ",\t Source=null, \tPath: " + path);
        }

        if (status.pathWhereFound == null) {
            errln("Locale: " + locale + ",\t Path=null, \tPath: " + path);
        }
    }

    final ImmutableSet<String> ALLOWED_NULL =
            ImmutableSet.of(
                    "//ldml/dates/timeZoneNames/zone[@type=\"Pacific/Enderbury\"]/exemplarCity");

    /** Is the path allowed to have a null value? */
    public boolean allowsExtraPath(String path, boolean isExtraPath) {
        return (isExtraPath && extraPathAllowsNullValue(path)) || ALLOWED_NULL.contains(path);
    }

    /**
     * Is the given extra path exceptional in the sense that null value is allowed?
     *
     * @param path the extra path
     * @return true if null value is allowed for path, else false
     *     <p>As of 2019-08-09, null values are found for many "metazone" paths like:
     *     //ldml/dates/timeZoneNames/metazone[@type="Galapagos"]/long/standard for many locales.
     *     Also for some "zone" paths like:
     *     //ldml/dates/timeZoneNames/zone[@type="Pacific/Honolulu"]/short/generic for locales
     *     including root, ja, and ar. Also for some "dayPeriods" paths like
     *     //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="midnight"]
     *     only for these six locales: bs_Cyrl, bs_Cyrl_BA, pa_Arab, pa_Arab_PK, uz_Arab,
     *     uz_Arab_AF.
     *     <p>This function is nearly identical to the JavaScript function with the same name. Keep
     *     the two functions consistent with each other. It would be more ideal if this knowledge
     *     were encapsulated on the server and the client didn't need to know about it. The server
     *     could send the client special fallback values instead of null.
     *     <p>Extra paths are generated by CLDRFile.getRawExtraPathsPrivate; this function may need
     *     updating (to allow null for other paths) if that function changes.
     *     <p>Reference: https://unicode-org.atlassian.net/browse/CLDR-11238
     */
    private boolean extraPathAllowsNullValue(String path) {
        if (path.contains("/timeZoneNames/metazone")
                || path.contains("/timeZoneNames/zone")
                || path.contains("/dayPeriods/dayPeriodContext")
                || path.contains("/unitPattern")
                || path.contains("/gender")
                || path.contains("/caseMinimalPairs")
                || path.contains("/genderMinimalPairs")
                || path.contains("/sampleName")
        //            ||
        // path.equals("//ldml/dates/timeZoneNames/zone[@type=\"Australia/Currie\"]/exemplarCity")
        //            ||
        // path.equals("//ldml/dates/timeZoneNames/zone[@type=\"Pacific/Enderbury\"]/exemplarCity")
        // +
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check that the given path and PathHeader.Factory undergo correct roundtrip conversion between
     * original and pretty paths.
     *
     * @param path the path string
     * @param phf the PathHeader.Factory
     */
    private void checkPrettyPaths(String path, PathHeader.Factory phf) {
        if (path.endsWith("/alias")) {
            return;
        }
        logln("Testing ==> " + path);
        String prettied = phf.fromPath(path).toString();
        String unprettied = phf.fromPath(path).getOriginalPath();
        if (!path.equals(unprettied)) {
            errln("Path Header doesn't roundtrip:\t" + path + "\t" + prettied + "\t" + unprettied);
        } else {
            logln(prettied + "\t" + path);
        }
    }

    private Collection<String> getLocalesToTest() {
        return params.inclusion <= 5
                ? Arrays.asList("root", "en", "ja", "ar", "de", "ru")
                : params.inclusion < 10
                        ? testInfo.getCldrFactory().getAvailableLanguages()
                        : testInfo.getCldrFactory().getAvailable();
    }

    /**
     * find all the items that are deprecated, but appear in paths and the items that aren't
     * deprecated, but don't appear in paths
     */
    static final class CheckDeprecated {
        M5<DtdType, String, String, String, Boolean> data =
                ChainedMap.of(
                        new HashMap<DtdType, Object>(),
                        new HashMap<String, Object>(),
                        new HashMap<String, Object>(),
                        new HashMap<String, Object>(),
                        Boolean.class);
        private TestPaths testPaths;

        public CheckDeprecated(TestPaths testPaths) {
            this.testPaths = testPaths;
        }

        static final Set<String> ALLOWED =
                new HashSet<>(Arrays.asList("postalCodeData", "postCodeRegex"));
        static final Set<String> OK_IF_MISSING =
                new HashSet<>(Arrays.asList("alt", "draft", "references"));

        public boolean check(XPathParts parts, String fullName) {
            DtdData dtdData = parts.getDtdData();
            for (int i = 0; i < parts.size(); ++i) {
                String elementName = parts.getElement(i);
                if (dtdData.isDeprecated(elementName, "*", "*")) {
                    if (ALLOWED.contains(elementName)) {
                        return false;
                    }
                    testPaths.errln(
                            "Deprecated element in data: "
                                    + dtdData.dtdType
                                    + ":"
                                    + elementName
                                    + " \t;"
                                    + fullName);
                    return true;
                }
                data.put(dtdData.dtdType, elementName, "*", "*", true);
                for (Entry<String, String> attributeNValue : parts.getAttributes(i).entrySet()) {
                    String attributeName = attributeNValue.getKey();
                    if (dtdData.isDeprecated(elementName, attributeName, "*")) {
                        if (attributeName.equals("draft")) {
                            testPaths.errln(
                                    "Deprecated attribute in data: "
                                            + dtdData.dtdType
                                            + ":"
                                            + elementName
                                            + ":"
                                            + attributeName
                                            + " \t;"
                                            + fullName
                                            + " - consider adding to DtdData.DRAFT_ON_NON_LEAF_ALLOWED if you are sure this is ok.");
                        } else {
                            testPaths.errln(
                                    "Deprecated attribute in data: "
                                            + dtdData.dtdType
                                            + ":"
                                            + elementName
                                            + ":"
                                            + attributeName
                                            + " \t;"
                                            + fullName);
                        }
                        return true;
                    }
                    String attributeValue = attributeNValue.getValue();
                    if (dtdData.isDeprecated(elementName, attributeName, attributeValue)) {
                        testPaths.errln(
                                "Deprecated attribute value in data: "
                                        + dtdData.dtdType
                                        + ":"
                                        + elementName
                                        + ":"
                                        + attributeName
                                        + ":"
                                        + attributeValue
                                        + " \t;"
                                        + fullName);
                        return true;
                    }
                    data.put(dtdData.dtdType, elementName, attributeName, "*", true);
                    data.put(dtdData.dtdType, elementName, attributeName, attributeValue, true);
                }
            }
            return false;
        }

        public void show(int inclusion) {
            for (DtdType dtdType : DtdType.values()) {
                if (dtdType.getStatus() != DtdType.DtdStatus.active) {
                    continue;
                }
                if (dtdType == DtdType.ldmlICU) {
                    continue;
                }
                M4<String, String, String, Boolean> infoEAV = data.get(dtdType);
                if (infoEAV == null) {
                    testPaths.warnln("Data doesn't contain: " + dtdType);
                    continue;
                }
                DtdData dtdData = DtdData.getInstance(dtdType);
                for (Element element : dtdData.getElements()) {
                    if (element.isDeprecated()
                            || element == dtdData.ANY
                            || element == dtdData.PCDATA) {
                        continue;
                    }
                    M3<String, String, Boolean> infoAV = infoEAV.get(element.name);
                    if (infoAV == null) {
                        testPaths.logln("Data doesn't contain: " + dtdType + ":" + element.name);
                        continue;
                    }

                    for (Attribute attribute : element.getAttributes().keySet()) {
                        if (attribute.isDeprecated() || OK_IF_MISSING.contains(attribute.name)) {
                            continue;
                        }
                        Map<String, Boolean> infoV = infoAV.get(attribute.name);
                        if (infoV == null) {
                            testPaths.logln(
                                    "Data doesn't contain: "
                                            + dtdType
                                            + ":"
                                            + element.name
                                            + ":"
                                            + attribute.name);
                            continue;
                        }
                        for (String value : attribute.values.keySet()) {
                            if (attribute.isDeprecatedValue(value)) {
                                continue;
                            }
                            if (!infoV.containsKey(value)) {
                                testPaths.logln(
                                        "Data doesn't contain: "
                                                + dtdType
                                                + ":"
                                                + element.name
                                                + ":"
                                                + attribute.name
                                                + ":"
                                                + value);
                            }
                        }
                    }
                }
            }
        }
    }

    public void TestNonLdml() {
        int maxPerDirectory = getInclusion() <= 5 ? 20 : Integer.MAX_VALUE;
        CheckDeprecated checkDeprecated = new CheckDeprecated(this);
        PathStarrer starrer = new PathStarrer();
        StringBuilder removed = new StringBuilder();
        Set<String> nonFinalValues = new LinkedHashSet<>();
        Set<String> skipLast = new HashSet(Arrays.asList("version", "generation"));
        String[] normalizedPath = {""};

        int counter = 0;
        for (String directory : Arrays.asList("keyboards/", "common/", "seed/", "exemplars/")) {
            String dirPath = CLDRPaths.BASE_DIRECTORY + directory;
            for (String fileName : new File(dirPath).list()) {
                File dir2 = new File(dirPath + fileName);
                if (!dir2.isDirectory()
                        || (dir2.getName().equals("import")
                                && directory.equals("keyboards/")) // has a different root element
                        || fileName.equals("properties") // TODO as flat files
                //                    || fileName.equals(".DS_Store")
                //                    || ChartDelta.LDML_DIRECTORIES.contains(dir)
                //                    || fileName.equals("dtd")  // TODO as flat files
                //                    || fileName.equals(".project")  // TODO as flat files
                //                    //|| dir.equals("uca") // TODO as flat files
                ) {
                    continue;
                }

                Set<Pair<String, String>> seen = new HashSet<>();
                Set<String> seenStarred = new HashSet<>();
                int count = 0;
                Set<Element> haveErrorsAlready = new HashSet<>();
                for (String file : dir2.list()) {
                    if (!file.endsWith(".xml")) {
                        continue;
                    }
                    if (++count > maxPerDirectory) {
                        break;
                    }
                    String fullName = dir2 + "/" + file;
                    for (Pair<String, String> pathValue :
                            XMLFileReader.loadPathValues(
                                    fullName, new ArrayList<Pair<String, String>>(), true)) {
                        String path = pathValue.getFirst();
                        final String value = pathValue.getSecond();
                        XPathParts parts = XPathParts.getFrozenInstance(path);
                        DtdData dtdData = parts.getDtdData();
                        DtdType type = dtdData.dtdType;

                        String finalElementString = parts.getElement(-1);
                        Element finalElement = dtdData.getElementFromName().get(finalElementString);
                        if (!haveErrorsAlready.contains(finalElement)) {
                            ElementType elementType = finalElement.getType();
                            Element.ValueConstraint requirement = finalElement.getValueConstraint();
                            if (requirement == Element.ValueConstraint.empty && !value.isEmpty()
                                    || requirement == Element.ValueConstraint.nonempty
                                            && value.isEmpty()) {
                                finalElement.getValueConstraint(); // for debugging
                                errln(
                                        "PCDATA ≠ emptyValue inconsistency:"
                                                + "\tfile="
                                                + fileName
                                                + "/"
                                                + file
                                                + "\telementType="
                                                + elementType
                                                + "\tvalue=«"
                                                + value
                                                + "»"
                                                + "\tpath="
                                                + path);
                                haveErrorsAlready.add(finalElement); // suppress all but first error
                            }
                        }

                        if (checkDeprecated.check(parts, fullName)) {
                            break;
                        }

                        String last = parts.getElement(-1);
                        if (skipLast.contains(last)) {
                            continue;
                        }

                        checkParts(fileName + "/" + file, parts);

                        String dpath = CLDRFile.getDistinguishingXPath(path, normalizedPath);
                        if (!dpath.equals(path)) {
                            checkParts(fileName + "/" + file, dpath);
                        }
                        if (!normalizedPath.equals(path) && !normalizedPath[0].equals(dpath)) {
                            checkParts(fileName + "/" + file, normalizedPath[0]);
                        }
                        XPathParts mutableParts = parts.cloneAsThawed();
                        counter =
                                removeNonDistinguishing(
                                        mutableParts, dtdData, counter, removed, nonFinalValues);
                        String cleaned = mutableParts.toString();
                        Pair<String, String> pair =
                                Pair.of(type == DtdType.ldml ? file : type.toString(), cleaned);
                        if (seen.contains(pair)) {
                            //                        parts.set(path);
                            //                        removeNonDistinguishing(parts, dtdData,
                            // counter, removed, nonFinalValues);
                            if (type != DtdType.keyboardTest3
                                    || !logKnownIssue(
                                            "CLDR-15034",
                                            "keyboardTest data appears as duplicate xpaths")) {
                                errln(
                                        "Duplicate "
                                                + type.toString()
                                                + ": "
                                                + file
                                                + ", "
                                                + path
                                                + ", "
                                                + cleaned
                                                + ", "
                                                + value);
                            }
                        } else {
                            seen.add(pair);
                            if (!nonFinalValues.isEmpty()) {
                                String starredPath = starrer.set(path);
                                if (!seenStarred.contains(starredPath)) {
                                    seenStarred.add(starredPath);
                                    logln("Non-node values: " + nonFinalValues + "\t" + path);
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
        checkDeprecated.show(getInclusion());
    }

    private void checkParts(String file, String path) {
        checkParts(file, XPathParts.getFrozenInstance(path));
    }

    public void checkParts(String file, XPathParts parts) {
        DtdData dtdData = parts.getDtdData();
        Element current = dtdData.ROOT;
        for (int i = 0; i < parts.size(); ++i) {
            String elementName = parts.getElement(i);
            if (i == 0) {
                assertEquals("root", current.name, elementName);
            } else {
                current = current.getChildNamed(elementName);
                if (!assertNotNull("element", current)) {
                    return; // failed
                }
                assertFalse(file + "/" + elementName + " deprecated", current.isDeprecated());
            }
            for (String attributeName : parts.getAttributeKeys(i)) {
                Attribute attribute = current.getAttributeNamed(attributeName);
                if (!assertNotNull("attribute", attribute)) {
                    return; // failed
                }
                assertFalse(
                        file + "/" + elementName + "@" + attributeName + " deprecated",
                        attribute.isDeprecated());

                String value = parts.getAttributeValue(i, attributeName);
                switch (attribute.getValueStatus(value)) {
                    case valid:
                        break;
                    default:
                        errln(
                                file
                                        + "/"
                                        + elementName
                                        + "@"
                                        + attributeName
                                        + ", expected match to: "
                                        + attribute.getMatchString()
                                        + " actual: «"
                                        + value
                                        + "»");
                        attribute.getValueStatus(value);
                        break;
                }
            }
        }
    }

    static final Set<String> SKIP_NON_NODE =
            new HashSet<>(Arrays.asList("references", "visibility", "access"));

    /**
     * @param parts the thawed XPathParts (can't be frozen, for putAttributeValue)
     * @param data
     * @param counter
     * @param removed
     * @param nonFinalValues
     * @return
     */
    private int removeNonDistinguishing(
            XPathParts parts,
            DtdData data,
            int counter,
            StringBuilder removed,
            Set<String> nonFinalValues) {
        removed.setLength(0);
        nonFinalValues.clear();
        HashSet<String> toRemove = new HashSet<>();
        nonFinalValues.clear();
        int size = parts.size();
        int last = size - 1;
        for (int i = 0; i < size; ++i) {
            removed.append("/");
            String element = parts.getElement(i);
            if (data.isOrdered(element)) {
                parts.putAttributeValue(i, "_q", String.valueOf(counter));
                counter++;
            }
            for (String attribute : parts.getAttributeKeys(i)) {
                if (!data.isDistinguishing(element, attribute)) {
                    toRemove.add(attribute);
                    if (i != last && !SKIP_NON_NODE.contains(attribute)) {
                        if (attribute.equals("draft")
                                && (parts.getElement(1).equals("transforms")
                                        || parts.getElement(1).equals("collations"))) {
                            // do nothing
                        } else {
                            nonFinalValues.add(attribute);
                        }
                    }
                }
            }
            if (!toRemove.isEmpty()) {
                for (String attribute : toRemove) {
                    removed.append(
                            "[@"
                                    + attribute
                                    + "=\""
                                    + parts.getAttributeValue(i, attribute)
                                    + "\"]");
                    parts.removeAttribute(i, attribute);
                }
                toRemove.clear();
            }
        }
        return counter;
    }
}
