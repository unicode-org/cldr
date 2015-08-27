package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.TransliteratorUtilities;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.util.OutputInt;

public class ChartDelta extends Chart {
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CLDRConfig.getInstance().getSupplementalDataInfo();

    final static Options myOptions = new Options();

    enum MyOptions {
        fileFilter(".*", ".*", "filter by dir/locale, eg: ^main/en$ or .*/en"),
        verbose(null, null, "verbose debugging messages");
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    private Matcher fileFilter;
    private boolean verbose;

    public ChartDelta(Matcher fileFilter, boolean verbose) {
        this.fileFilter = fileFilter;
        this.verbose = verbose;
    }

    public static void main(String[] args) {
        myOptions.parse(MyOptions.fileFilter, args, true);
        Matcher fileFilter = !MyOptions.fileFilter.option.doesOccur() ? null : PatternCache.get(MyOptions.fileFilter.option.getValue()).matcher("");
        boolean verbose = MyOptions.verbose.option.doesOccur();
        new ChartDelta(fileFilter, verbose).writeChart(null);
    }

    private static final String SEP = "\u0001";
    private static final boolean DEBUG = false;
    private static final String DEBUG_FILE = null; // "windowsZones.xml";
    static Pattern fileMatcher = PatternCache.get(".*");

    private static final String LAST_ARCHIVE_DIRECTORY = "/Users/markdavis/Google Drive/workspace/cldr-archive/cldr-"
        + ToolConstants.PREVIOUS_CHART_VERSION + "/";

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
            + "Titles prefixed by ¤ are special: either the locale data summary or supplemental data. "
            + "Not all changed data is charted yet. For details see each chart.</p>";
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

    static final CLDRFile EMPTY_CLDR = new CLDRFile(new SimpleXMLSource("und").freeze());

    public void writeSubcharts(Anchors anchors) {
        writeLdml(anchors);  
        writeNonLdmlPlain(anchors, getDirectory());
    }

    private void writeLdml(Anchors anchors) {
        // set up factories
        List<Factory> factories = new ArrayList<>();
        List<Factory> oldFactories = new ArrayList<>();
//        factories.add(Factory.make(CLDRPaths.BASE_DIRECTORY + "common/" + "main", ".*"));
//        oldFactories.add(Factory.make(LAST_ARCHIVE_DIRECTORY + "common/" + "main", ".*"));

        for (String dir : CLDRPaths.LDML_DIRECTORIES) {
            factories.add(Factory.make(CLDRPaths.BASE_DIRECTORY + "common/" + dir, ".*"));
            try {
                oldFactories.add(Factory.make(LAST_ARCHIVE_DIRECTORY + "common/" + dir, ".*"));
            } catch (Exception e) {
                oldFactories.add(null);
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
                List<File> sourceDirs = Arrays.asList(factory.getSourceDirectories());
                if (sourceDirs.size() != 1) {
                    throw new IllegalArgumentException("Internal error: expect single source dir");
                }
                File sourceDir = sourceDirs.get(0);
                String sourceDirLeaf = sourceDir.getName();
                //System.out.println(sourceDirLeaf);

                for (String locale : baseNLocale.getValue()) {
                    String nameAndLocale = sourceDirLeaf + "/" + locale;
                    if (fileFilter != null && !fileFilter.reset(nameAndLocale).find()) {
                        if (verbose) {
                            System.out.println("SKIPPING: " + nameAndLocale);
                        }
                        continue;
                    }
                    boolean isBase = locale.equals(base);
                    if (verbose) {
                        System.out.println(nameAndLocale);
                    }
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
                            || path.endsWith("/alias")
                            || path.startsWith("//ldml/segmentations") // do later
                            || path.startsWith("//ldml/annotations") // do later
                            || path.startsWith("//ldml/rbnf") // do later
                            ) {
                            continue;
                        }

//                        if (path.startsWith("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"stand-alone\"]")) {
//                            System.out.println(path);
//                        }
                        String sourceLocaleCurrent = current.getSourceLocaleID(path, currentStatus);
                        String sourceLocaleOld = old.getSourceLocaleID(path, oldStatus);

                        // filter out stuff that differs at a higher level, except allow root when we are in base
                        if (!sourceLocaleCurrent.equals(locale)
                            && !sourceLocaleOld.equals(locale)) {
                            continue;
//                            if (!isBase) {
//                                continue;
//                            } else if (!sourceLocaleCurrent.equals("root") && !sourceLocaleOld.equals("root")) {
//                                continue;
//                            } else if (sourceLocaleCurrent.equals(sourceLocaleOld)) {
//                                continue;
//                            }
                        }
                        if (!path.equals(currentStatus.pathWhereFound)
                            && !path.equals(oldStatus.pathWhereFound)) {
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
        if (oldFactory == null) {
            return EMPTY_CLDR;
        }
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
        .addColumn("Locales where different", "class='target'", null, "class='target'", true)
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
        .addColumn("Old", "class='target' width='20%'", null, "class='target'", true)
        .addColumn("New", "class='target' width='20%'", null, "class='target'", true)
        .addColumn("Level", "class='target'", null, "class='target'", true)
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
            Level coverageLevel = SUPPLEMENTAL_DATA_INFO.getCoverageLevel(ph.getOriginalPath(), locale);
            String fixedOldValue = oldValue == null ? "▷missing◁" : TransliteratorUtilities.toHTML.transform(oldValue);
            String fixedNewValue = currentValue == null ? "▷removed◁" : TransliteratorUtilities.toHTML.transform(currentValue);

            tablePrinter.addRow()
            .addCell(ph.getSectionId())
            .addCell(ph.getPageId())
            .addCell(ph.getHeader())
            .addCell(specialCode)
            .addCell(locale)
            .addCell(fixedOldValue)
            .addCell(fixedNewValue)
            .addCell(coverageLevel)
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
        public boolean getShowDate() {
            return false;
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
            return "<p>Lists data fields that differ from the last version."
                + " Inherited differences in locales are suppressed, except where the source locales are different. "
                + " The collations and metadata still have a raw format."
                + " The rbnf, segmentations, and annotations are not yet included.<p>";
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
        Map<String,Map<String,String>> transform = new TreeMap<>();

        for (String dir : new File(CLDRPaths.BASE_DIRECTORY + "common/").list()) {
            if (CLDRPaths.LDML_DIRECTORIES.contains(dir) 
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
                String base = file.substring(0,file.length()-4);
                if (fileFilter != null && !fileFilter.reset(dir + "/" + base).find()) {
                    continue;
                }

                if (verbose) {
                    System.out.println(file);
                }
                Map<String, String> contents1;
                contents1 = fillData(dir1.toString() + "/", file);

//                try {
//                } catch (Exception e) {
//                    contents1 = Collections.emptyMap();
//                }
                Map<String, String> contents2;
                contents2 = fillData(dir2.toString() + "/", file);

//                try {
//                } catch (Exception e) {
//                    contents2 = Collections.emptyMap();
//                }

                Set<String> keys = new TreeSet<String>(CldrUtility.ifNull(contents1.keySet(), Collections.<String>emptySet()));
                keys.addAll(CldrUtility.ifNull(contents2.keySet(), Collections.<String>emptySet()));
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
                        if (key.contains("/transforms/")) {
                            Map<String, String> baseMap = transform.get(base);
                            if (baseMap == null) {
                                transform.put(base, baseMap = new TreeMap<>(CLDRFile.getComparator(DtdType.supplementalData)));
                            }
                            baseMap.put(key,combinedValue);
                        }
                        supplemental.put(key,combinedValue);
                    } else {
                        bcp.put(key, combinedValue);
                    }
                }
            }
        }
        writeDiffs(anchors, directory, "bcp47", "¤¤BCP47 Delta", bcp);
        writeDiffs(anchors, directory, "supplemental-data", "¤¤Supplemental Delta", supplemental);
        for (Entry<String, Map<String, String>> entry : transform.entrySet()) {
            writeDiffs(anchors, directory, "transform-" + entry.getKey(), "¤" + name(entry.getKey()), entry.getValue());
        }
    }

    static final Splitter ONHYPHEN = Splitter.on('-');
    final LanguageTagParser lparser = new LanguageTagParser();
    final Map<LstrType, Map<Validity.Status, Set<String>>> validity = Validity.getInstance().getData();
    final Set<String> regularLanguage = validity.get(LstrType.language).get(Validity.Status.regular);

    private String name(String key) {
        // eo-eo_FONIPA
        // Latin-ASCII
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (String part : ONHYPHEN.split(key)) {
            lparser.set(part);
            String base = lparser.getLanguage();
            int script = UScript.getCodeFromName(base);
            if (script != UScript.INVALID_CODE) {
                part = UScript.getName(script);
            } else if (regularLanguage.contains(base)) {
                part = ENGLISH.getName(part);
            }
            if (i != 0) {
                sb.append('-');
            }
            sb.append(part);
            ++i;
        }
        return sb.toString();
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
        Map<String,String> contentsA = Collections.emptyMap();

        List<Pair<String, String>> contents1;
        try {
            contents1 = XMLFileReader.loadPathValues(directory + file, new ArrayList<Pair<String, String>>(), true);
        } catch (Exception e) {
            return contentsA;
        }
        XPathParts parts = new XPathParts();
        DtdType dtdType = null;
        Map<String,String> nonDistinguishing = null;
        int qCount = 0;
        boolean debug = file.equals(DEBUG_FILE);
        OutputInt q = new OutputInt();
        for (Pair<String, String> s : contents1) {
            String first = s.getFirst();
            if (first.startsWith("//supplementalData/generation")
                || first.startsWith("//supplementalData/version")
                || first.startsWith("//supplementalData/references")
                || first.startsWith("//supplementalData/coverageLevels") // TODO enable once formatting is better
                || first.startsWith("//supplementalData/metadata/validity")
                || first.startsWith("//ldmlBCP47/generation")
                || first.startsWith("//ldmlBCP47/version")
                || first.startsWith("//supplementalData/transforms/") && first.endsWith("/comment")
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
            value = old + "\n" + value;
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
        FIXED_ORDERING.put(DtdType.supplementalData, "tRule", false);

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
