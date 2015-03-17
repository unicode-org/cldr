package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;

public class ChartDelta extends Chart {
    public static void main(String[] args) {
        new ChartDelta().writeChart(null);
    }

    private static final String SEP = "\u0001";
    private static final boolean DEBUG = false;
    private static final String DEBUG_FILE = "windowsZones.xml";
    static Pattern fileMatcher = Pattern.compile(".*");

    private static final String LAST_ARCHIVE_DIRECTORY = "/Users/markdavis/Google Drive/workspace/cldr-archive/cldr-26.0/";

    static final Set<String> LDML_DIRECTORIES = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "annotations",
        "casing", 
        "collation",
        "main", 
        "rbnf", 
        "segments"
        )));

    static String DIR =     CLDRPaths.CHART_DIRECTORY + "/delta/";
    static PathHeader.Factory phf = PathHeader.getFactory(ENGLISH);
    static final Set<String> DONT_CARE = new HashSet<>(Arrays.asList("draft", "standard", "reference"));

    @Override
    public String getDirectory() {
        return DIR;
    }
    @Override
    public String getTitle() {
        return "Delta Charts";
    }
    @Override
    public String getFileName() {
        return "index";
    }
    @Override
    public String getExplanation() {
        return "<p>Charts showing the differences from the last version. "
            + "Not all changed data is shown; currently differences in the annotations, segments, and keyboards are not charted.</p>";
    }
    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException{
        FormattedFileWriter.Anchors anchors = new FormattedFileWriter.Anchors();
        writeSubcharts(anchors);
        pw.setIndex("Main Chart Index", "../index.html");
        pw.write(anchors.toString());
    }

    static class PathHeaderSegment extends R3<PathHeader, Integer, String> {
        public PathHeaderSegment(PathHeader b, int elementIndex, String attribute) {
            super(b, elementIndex, attribute);
        }
    }

    static class PathDiff extends R4<PathHeaderSegment, String, String, String> {
        public PathDiff(String a, PathHeaderSegment b, String c, String d) {
            super(b, a, c, d);
        }
    }

//    static void CheckLocales() {
//        //File[] directories = new File(CLDRPaths.BASE_DIRECTORY + "common/").listFiles();
//        TreeSet<String> mainList = new TreeSet<>(Arrays.asList(new File(CLDRPaths.BASE_DIRECTORY + "common/" + "main/").list()));
//
//        for (String dir : LDML_DIRECTORIES) {
//            Set<String> dirFiles = new TreeSet(Arrays.asList(new File(CLDRPaths.BASE_DIRECTORY + "common/" + dir).list()));
//            if (!mainList.containsAll(dirFiles)) {
//                dirFiles.removeAll(mainList);
//                System.out.println(dir + " has extra files" + dirFiles);
//            }
//        }
//    }

    static final CLDRFile EMPTY_CLDR = new CLDRFile(new SimpleXMLSource("und").freeze());

    public void writeSubcharts(Anchors anchors) {
        writeNonLdmlPlain(anchors, getDirectory());
        writeLdml(anchors);  
    }
    
    private void writeLdml(Anchors anchors) {
        // set up factories
        List<Factory> factories = new ArrayList<>();
        List<Factory> oldFactories = new ArrayList<>();
        factories.add(Factory.make(CLDRPaths.BASE_DIRECTORY + "common/" + "main", ".*"));
        oldFactories.add(Factory.make(LAST_ARCHIVE_DIRECTORY + "common/" + "main", ".*"));

        for (String dir : LDML_DIRECTORIES) {
            if (!dir.equals("main")) {
                factories.add(Factory.make(CLDRPaths.BASE_DIRECTORY + "common/" + dir, ".*"));
                try {
                    oldFactories.add(Factory.make(LAST_ARCHIVE_DIRECTORY + "common/" + dir, ".*"));
                } catch (Exception e) {
                    oldFactories.add(null);
                }
            }
        }

        // get a list of all the locales to cycle over

        Relation<String,String> baseToLocales = Relation.of(new TreeMap(), HashSet.class);
        Matcher m = fileMatcher.matcher("");
        Set<String> defaultContents = SDI.getDefaultContentLocales();
        LanguageTagParser ltp = new LanguageTagParser();
        LikelySubtags ls = new LikelySubtags(SDI);
        for (String file : factories.get(0).getAvailable()) {
            if (defaultContents.contains(file)) {
                continue;
            }
            if (!m.reset(file).matches()) {
                continue;
            }
            String base = file.equals("root") ? "root" : ltp.set(ls.minimize(file)).getLanguageScript();
            baseToLocales.put(base, file);
        }

        // do keyboards later

        Status currentStatus = new Status();
        Status oldStatus = new Status();
        Set<PathDiff> diff = new TreeSet<>();
        Set<String> paths = new HashSet<>();

        Relation<PathHeader, String> diffAll = new Relation(new TreeMap(), TreeSet.class);
        XPathParts pathPlain = new XPathParts();

        for (Entry<String, Set<String>> baseNLocale : baseToLocales.keyValuesSet()) {
            String base = baseNLocale.getKey();
            int qCount = 0;
            for (int i = 0; i < factories.size(); ++i) {
                Factory factory = factories.get(i);
                Factory oldFactory = oldFactories.get(i);
                System.out.println(Arrays.asList(factory.getSourceDirectories()));

                for (String locale : baseNLocale.getValue()) {
                    System.out.println(locale);
                    CLDRFile current = makeWithFallback(factory, locale);
                    CLDRFile old = makeWithFallback(oldFactory, locale);
                    if (old == EMPTY_CLDR && current == EMPTY_CLDR) {
                        continue;
                    }
                    paths.clear();
                    for (String path : current.fullIterable()) {
                        paths.add(path);
                    }
                    for (String path : old.fullIterable()) {
                        paths.add(path);
                    }
                    for (String path : paths) {
                        if (path.startsWith("//ldml/identity")
                            || path.startsWith("//ldml/segmentations") // do later
                            || path.startsWith("//ldml/annotations") // do later
                            || path.startsWith("//ldml/rbnf") // do later
                            ) {
                            continue;
                        }
                        String sourceLocaleCurrent = current.getSourceLocaleID(path, currentStatus);
                        String sourceLocaleOld = old.getSourceLocaleID(path, oldStatus);

                        // filter out stuff that differs at a higher level
                        if (!sourceLocaleCurrent.equals(locale) 
                            && !sourceLocaleOld.equals(locale)) {
                            continue;
                        }
                        if (!path.equals(currentStatus.pathWhereFound)
                            || !path.equals(oldStatus.pathWhereFound)) {
                            continue;
                        }
                        // fix some incorrect cases

                        PathHeader ph;
                        try {
                            ph = phf.fromPath(path);
                        } catch (Exception e) {
                            System.err.println("Skipping path with bad PathHeader: " + path);
                            continue;
                        }

                        // handle non-distinguishing attributes
                        addPathDiff(old, current, locale, ph, diff);

                        addValueDiff(old.getStringValue(path), current.getStringValue(path), locale, ph, diff, diffAll);
                    }
                }
            }
            writeDiffs(anchors, base, diff);  
            diff.clear();
        }
        writeDiffs(anchors, diffAll);
    }

    private CLDRFile makeWithFallback(Factory oldFactory, String locale) {
        CLDRFile old;
        String oldLocale = locale;
        while (true) { // fall back for old, maybe to root
            try {
                old = oldFactory.make(oldLocale, true);
                break;
            } catch (Exception e) {
                oldLocale = LocaleIDParser.getParent(oldLocale);
                if (oldLocale == null) {
                    return EMPTY_CLDR;
                }
            }
        }
        return old;
    }

    private void addPathDiff(CLDRFile old, CLDRFile current, String locale, PathHeader ph, Set<PathDiff> diff2) {
        String path = ph.getOriginalPath();
        String fullPathCurrent = current.getFullXPath(path);
        String fullPathOld = old.getFullXPath(path);
        if (Objects.equals(fullPathCurrent, fullPathOld)) {
            return;
        }
        XPathParts pathPlain = new XPathParts().set(path);
        XPathParts pathCurrent = fullPathCurrent == null ? pathPlain : new XPathParts().set(fullPathCurrent);
        XPathParts pathOld = fullPathOld == null ? pathPlain : new XPathParts().set(fullPathOld);
        TreeSet<String> fullAttributes = null;
        int size = pathCurrent.size();
        for (int elementIndex = 0; elementIndex < size; ++elementIndex) { // will have same size
            Collection<String> distinguishing = pathPlain.getAttributeKeys(elementIndex);
            Collection<String> attributesCurrent = pathCurrent.getAttributeKeys(elementIndex);
            Collection<String> attributesOld = pathCurrent.getAttributeKeys(elementIndex);
            if (attributesCurrent.isEmpty() && attributesOld.isEmpty()) {
                continue;
            }
            if (fullAttributes == null) {
                fullAttributes = new TreeSet<String>();
            } else {
                fullAttributes.clear();
            }
            fullAttributes.addAll(attributesCurrent);
            fullAttributes.addAll(attributesOld);
            fullAttributes.removeAll(distinguishing);
            fullAttributes.removeAll(DONT_CARE);

            // at this point we only have non-distinguishing
            for (String attribute : fullAttributes) {
                String attributeValueOld = pathOld.getAttributeValue(elementIndex, attribute);
                String attributeValueCurrent = pathCurrent.getAttributeValue(elementIndex, attribute);
                if (Objects.equals(attributeValueOld, attributeValueCurrent)) {
                    continue;
                }
                PathDiff row = new PathDiff(locale, new PathHeaderSegment(ph, size-elementIndex-1, attribute), attributeValueOld, attributeValueCurrent);
                if (DEBUG) {
                    System.out.println(row);
                }
                diff2.add(row);
            }
        }
    }

    private void addValueDiff(String valueOld, String valueCurrent, String locale, PathHeader ph, Set<PathDiff> diff, Relation<PathHeader, String> diffAll) {
        String path = ph.getOriginalPath();

        if (!Objects.equals(valueCurrent, valueOld)) {
            PathDiff row = new PathDiff(locale, new PathHeaderSegment(ph, -1, ""), valueOld, valueCurrent);
            diff.add(row);
            diffAll.put(ph, locale);
        }
    }

    private void writeDiffs(Anchors anchors, String directory, String file, String title, Map<String, String> bcp) {
        TablePrinter tablePrinter = new TablePrinter()
        .addColumn("Path", "class='source'", null, "class='source'", true)
        .setBreakSpans(true)
        .addColumn("Old", "class='target'", null, "class='target'", true)
        .addColumn("New", "class='target'", null, "class='target'", true)
        ;
        for (Entry<String, String> entry : bcp.entrySet()) {
            String[] oldNew = entry.getValue().split(SEP);
            String shortKey = removeStart(entry.getKey(), "//supplementalData", "//ldmlBCP47");

            tablePrinter.addRow()
            .addCell(shortKey)
            .addCell(oldNew[0])
            .addCell(oldNew[1])
            .finishRow();
            //System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
        writeTable(anchors, file, tablePrinter, title);
    }

    private void writeDiffs(Anchors anchors, Relation<PathHeader, String> diffAll) {
        TablePrinter tablePrinter = new TablePrinter()
        .addColumn("Section", "class='source'", null, "class='source'", true)
        .addColumn("Page", "class='source'", null, "class='source'", true)
        .addColumn("Header", "class='source'", null, "class='source'", true)
        .addColumn("Code", "class='source'", null, "class='source'", true)
        .addColumn("Locales", "class='target'", null, "class='target'", true)
        ;
        for (Entry<PathHeader, Set<String>> row : diffAll.keyValuesSet()) {
            PathHeader ph = row.getKey();
            Set<String> locales = row.getValue();
            tablePrinter.addRow()
            .addCell(ph.getSectionId())
            .addCell(ph.getPageId())
            .addCell(ph.getHeader())
            .addCell(ph.getCode())
            .addCell(CollectionUtilities.join(locales, " "))
            .finishRow();
        }
        writeTable(anchors, "ldml-summary", tablePrinter, "Summary Delta"); 
    }


    private void writeDiffs(Anchors anchors, String file, Set<PathDiff> diff) {
        if (diff.isEmpty()) {
            return;
        }
        TablePrinter tablePrinter = new TablePrinter()
        .addColumn("Section", "class='source'", null, "class='source'", true)
        .addColumn("Page", "class='source'", null, "class='source'", true)
        .addColumn("Header", "class='source'", null, "class='source'", true)
        .addColumn("Code", "class='source'", null, "class='source'", true)
        .addColumn("Locale", "class='source'", null, "class='source'", true)
        .addColumn("Old", "class='target'", null, "class='target'", true)
        .addColumn("New", "class='target'", null, "class='target'", true)
        ;

        for (PathDiff row : diff) {
            PathHeaderSegment phs = row.get0();
            String locale = row.get1();
            String oldValue = row.get2();
            String currentValue = row.get3();

            PathHeader ph = phs.get0();
            Integer pathIndex = phs.get1();
            String attribute = phs.get2();
            String specialCode = ph.getCode();

            if (!attribute.isEmpty()) {
                specialCode += "_" + attribute;
                if (pathIndex != 0) {
                    specialCode += "|" + pathIndex;
                }
            }
            tablePrinter.addRow()
            .addCell(ph.getSectionId())
            .addCell(ph.getPageId())
            .addCell(ph.getHeader())
            .addCell(specialCode)
            .addCell(locale)
            .addCell(CldrUtility.ifNull(oldValue, "▷missing◁"))
            .addCell(CldrUtility.ifNull(currentValue, "▷removed◁"))
            .finishRow();
        }
        writeTable(anchors, file, tablePrinter, ENGLISH.getName(file) + " Delta");
        diff.clear();
    }

    private class ChartDeltaSub extends Chart {
        String title;
        String file;
        private TablePrinter tablePrinter;

        public ChartDeltaSub(String title, String file, TablePrinter tablePrinter) {
            super();
            this.title = title;
            this.file = file;
            this.tablePrinter = tablePrinter;
        }
        @Override
        public String getDirectory() {
            return DIR;
        }
        @Override
        public String getTitle() {
            return title;
        }
        @Override
        public String getFileName() {
            return file;
        }
        @Override
        public String getExplanation() {
            return "<p>Summary fields with changed values, listing locales where different."
                + " The collations, metadata, and rbnf still have a raw format.<p>";
        }
        @Override
        public void writeContents(FormattedFileWriter pw) throws IOException {
            pw.write(tablePrinter.toTable());
        }
    }
    private void writeTable(Anchors anchors, String file, TablePrinter tablePrinter, String title) {
        new ChartDeltaSub(title, file, tablePrinter).writeChart(anchors);
    }

    public void writeNonLdmlPlain(Anchors anchors, String directory) {
        Map<String,String> bcp = new TreeMap<>(CLDRFile.getComparator(DtdType.ldmlBCP47));
        Map<String,String> supplemental = new TreeMap<>(CLDRFile.getComparator(DtdType.supplementalData));

        for (String dir : new File(CLDRPaths.BASE_DIRECTORY + "common/").list()) {
            if (LDML_DIRECTORIES.contains(dir) 
                || dir.equals(".DS_Store") 
                || dir.equals("dtd")  // TODO as flat files
                || dir.equals("properties") // TODO as flat files
                || dir.equals("uca") // TODO as flat files
                ) {
                continue;
            }
            File dir1 = new File(LAST_ARCHIVE_DIRECTORY + "common/" + dir);
            File dir2 = new File(CLDRPaths.BASE_DIRECTORY + "common/" + dir);

            for (String file : dir2.list()) {
                if (!file.endsWith(".xml")) {
                    continue;
                }
                System.out.println(file);
                Map<String, String> contents1;
                try {
                    contents1 = fillData(dir1.toString() + "/", file);
                } catch (Exception e) {
                    contents1 = Collections.emptyMap();
                }
                Map<String, String> contents2;
                try {
                    contents2 = fillData(dir2.toString() + "/", file);
                } catch (Exception e) {
                    contents2 = Collections.emptyMap();
                }

                Set<String> keys = new TreeSet<String>(contents1.keySet());
                keys.addAll(contents2.keySet());
                for (String key : keys) {
                    String set1 = contents1.get(key);
                    String set2 = contents2.get(key);
                    if (Objects.equals(set1, set2)) {
                        if (file.equals(DEBUG_FILE)) { // for debugging
                            System.out.println("**Same: " + key + "\t" + set1);
                        }
                        continue;
                    }
                    String combinedValue = CldrUtility.ifNull(set1, "▷missing◁") + SEP + CldrUtility.ifNull(set2, "▷removed◁");
                    if (key.startsWith("//supplementalData")) {
                        supplemental.put(key,combinedValue);
                    } else {
                        bcp.put(key, combinedValue);
                    }
                }
            }
        }
        writeDiffs(anchors, directory, "bcp47", "CLDR BCP47 Delta", bcp);
        writeDiffs(anchors, directory, "supplemental-data", "CLDR Supplemental Data", supplemental);
    }

    private String removeStart(String key, String... string) {
        for (String start : string) {
            if (key.startsWith(start)) {
                return "…" + key.substring(start.length());
            }
        }
        return key;
    }

    private static Map<String, String> fillData(String directory, String file) {
        Map<String,String> contentsA = Collections.EMPTY_MAP;

        List<Pair<String, String>> contents1 = XMLFileReader.loadPathValues(directory + file, new ArrayList<Pair<String, String>>(), true);
        XPathParts parts = new XPathParts();
        DtdType dtdType = null;
        Map<String,String> nonDistinguishing = null;
        int qCount = 0;
        boolean debug = file.equals(DEBUG_FILE);
        for (Pair<String, String> s : contents1) {
            String first = s.getFirst();
            if (first.startsWith("//supplementalData/generation")
                || first.startsWith("//supplementalData/version")
                || first.startsWith("//supplementalData/references")
                || first.startsWith("//supplementalData/coverageLevels") // TODO enable once formatting is better
                || first.startsWith("//supplementalData/metadata/validity")
                || first.startsWith("//ldmlBCP47/generation")
                || first.startsWith("//ldmlBCP47/version")
                ) {
                continue;
            }
            if (debug) {
                int debugInt = 0;
            }
            parts.set(first);
            if (dtdType == null) {
                dtdType = DtdType.valueOf(parts.getElement(0));
                nonDistinguishing = new TreeMap<>();
                contentsA = new TreeMap<>(CLDRFile.getComparator(dtdType));
            }
            String value = s.getSecond().trim();
            for (int i = 0; i < parts.size(); ++i) {
                String element = parts.getElement(i);
                Collection<String> attributeKeys = parts.getAttributeKeys(i);
                if (element.equals("ruleset")) {
                    int x = 0;
                }
                if (shouldBeOrdered(dtdType, element)) {
                    if (!attributeKeys.contains("_q")) {
                        parts.putAttributeValue(i, "_q", String.valueOf(qCount++));
                    }
                } else {
                    if (attributeKeys.contains("_q")) {
                        parts.removeAttribute(i, "_q");
                    }
                }
                if (parts.getAttributeCount(i) == 0) {
                    continue;
                }
                for (String key : attributeKeys) {
                    if (!isFixedDistinguishing(dtdType, element, key)) {
                        nonDistinguishing.put(key, parts.getAttributeValue(i, key));
                    }
                }
                if (nonDistinguishing.isEmpty()) {
                    continue;
                }
                // remove from path
                for (String key : nonDistinguishing.keySet()) {
                    parts.removeAttribute(i, key);
                }
                // 
                for (String key : attributeKeys) {
                    if (SKIP_ATTRIBUTE.contains(key)) {
                        parts.removeAttribute(i, key);
                    }
                }

                String cleanedPartialPath = parts.toString(i+1);
                for (Entry<String, String> entry : nonDistinguishing.entrySet()) {
                    checkedPut(contentsA, cleanedPartialPath + "/_" + entry.getKey(), entry.getValue());
                }
                nonDistinguishing.clear();
            }
            if (!value.isEmpty()) {
                String path2 = parts.toString();
                checkedPut(contentsA, path2, value);
            }
        }
        return contentsA;
    }

    static final Set<String> SKIP_ATTRIBUTE = new HashSet<String>(Arrays.asList("references"));

    private static void checkedPut(Map<String, String> contentsA, String path2, String value) {
        String old = contentsA.get(path2);
        if (old != null) {
            if (old.equals(value)) {
                return;
            }
            throw new IllegalArgumentException("Already contains value for " + path2 + ": old:" + old + ", new: " + value);    
        }
        contentsA.put(path2, value);
    }

    static final ChainedMap.M4<DtdType, String, String, Boolean> FIXED_DISTINGUISHING = ChainedMap.of(
        new HashMap<DtdType, Object>(), 
        new HashMap<String, Object>(),
        new HashMap<String, Object>(),
        Boolean.class);

    static final ChainedMap.M3<DtdType, String, Boolean> FIXED_ORDERING = ChainedMap.of(
        new HashMap<DtdType, Object>(), 
        new HashMap<String, Object>(),
        Boolean.class);

    static {
        //<key name="ca" description="Calendar algorithm key" alias="calendar">
        //<type name="buddhist" description="Thai Buddhist calendar"/>
        FIXED_DISTINGUISHING.put(DtdType.ldmlBCP47, "key", "alias", false);

        FIXED_ORDERING.put(DtdType.supplementalData, "substitute", true);

        //<approvalRequirement votes="20" locales="Cldr:modern" paths="//ldml/numbers/symbols[^/]++/(decimal|group|(plus|minus)Sign)"/>
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "approvalRequirement", "locales", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "approvalRequirement", "paths", true);
        // approvalRequirement should be ordered, but for our comparison, simpler to override

        //<coverageVariable key="%acctPattern" value="[@type='accounting']/pattern[@type='standard']"/>
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "coverageVariable", "key", true);
        FIXED_ORDERING.put(DtdType.supplementalData, "coverageVariable", false);

        //<coverageLevel inLanguage="%phonebookCollationLanguages" value="minimal" match="localeDisplayNames/types/type[@type='phonebook'][@key='collation']"/>
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "coverageLevel", "inLanguage", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "coverageLevel", "inScript", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "coverageLevel", "inTerritory", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "coverageLevel", "match", true);
        FIXED_ORDERING.put(DtdType.supplementalData, "coverageLevel", false); // this should be true, but for our comparison, simpler to override

//        <!ATTLIST dayPeriodRule type NMTOKEN #REQUIRED >
//        <!ATTLIST dayPeriodRule at NMTOKEN #IMPLIED >
//        <!ATTLIST dayPeriodRule after NMTOKEN #IMPLIED >
//        <!ATTLIST dayPeriodRule before NMTOKEN #IMPLIED >
//        <!ATTLIST dayPeriodRule from NMTOKEN #IMPLIED >
//        <!ATTLIST dayPeriodRule to NMTOKEN #IMPLIED >
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "dayPeriodRule", "type", false);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "dayPeriodRule", "at", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "dayPeriodRule", "after", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "dayPeriodRule", "before", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "dayPeriodRule", "from", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "dayPeriodRule", "to", true);

// <personList type="neutral" locales="af bn bg da de en et eu fa fil fi gu hu id ja kn ko ml ms no sv sw ta te th tr vi zu" />
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "personList", "type", false);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "personList", "locales", true);

        //          <languageMatch desired="am_*_*" supported="en_GB" percent="90" oneway="true" /> <!-- fix ICU for en_GB -->
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "languageMatch", "desired", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "languageMatch", "supported", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "languageMatch", "oneway", true);
        FIXED_ORDERING.put(DtdType.supplementalData, "languageMatch", false); // this should be true, but for our comparison, simpler to override

        //              <usesMetazone to="1979-10-25 23:00" from="1977-10-20 23:00" mzone="Europe_Central"/>
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "usesMetazone", "from", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "usesMetazone", "to", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "usesMetazone", "mzone", false);


//        <!ATTLIST numberingSystem type ( numeric | algorithmic ) #REQUIRED >
//        <!ATTLIST numberingSystem id NMTOKEN #REQUIRED >
//        <!ATTLIST numberingSystem radix NMTOKEN #IMPLIED >
//        <!ATTLIST numberingSystem digits CDATA #IMPLIED >
//        <!ATTLIST numberingSystem rules CDATA #IMPLIED >
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "numberingSystem", "type", false);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "numberingSystem", "radix", false);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "numberingSystem", "digits", false);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "numberingSystem", "rules", false);

        FIXED_ORDERING.put(DtdType.supplementalData, "pluralRule", false); // this should be true, but for our comparison, simpler to override

        //pluralRanges locales
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "pluralRanges", "locales", true);

        //          <pluralRange start="one"   end="one"   result="one"/>
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "pluralRange", "start", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "pluralRange", "end", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "pluralRange", "result", false);

//        <territory type="AC" gdp="35200000" literacyPercent="99" population="940">  <!--Ascension Island-->
//        <languagePopulation type="en" populationPercent="99" references="R1020"/>   <!--English-->

        FIXED_DISTINGUISHING.put(DtdType.ldml, "rbnfrule", "value", false);
        FIXED_ORDERING.put(DtdType.ldml, "ruleset", false); // this should be true, but for our comparison, simpler to override
        FIXED_ORDERING.put(DtdType.ldml, "rbnfrule", false); // this should be true, but for our comparison, simpler to override

        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "transform", "source", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "transform", "target", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "transform", "direction", true);
        FIXED_DISTINGUISHING.put(DtdType.supplementalData, "transform", "variant", true);
        FIXED_ORDERING.put(DtdType.supplementalData, "tRule", false); // this should be true, but for our comparison, simpler to override

        // FIXED_ORDERING.freeze(); Add to ChainedMap?
    }
    private static boolean isFixedDistinguishing(DtdType dtdType, String element, String key) {
        Boolean override = FIXED_DISTINGUISHING.get(dtdType, element, key);
        if (override != null) {
            return override;
        }
        return CLDRFile.isDistinguishing(dtdType, element, key);
    }

    private static boolean shouldBeOrdered(DtdType dtdType, String element) {
        Boolean override = FIXED_ORDERING.get(dtdType, element);
        if (override != null) {
            return override;
        }
        return CLDRFile.isOrdered(element, dtdType);
    }

}
