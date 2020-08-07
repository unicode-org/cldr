package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;

public class ChartDelta extends Chart {
    /**
     * If true, check only high-level paths, i.e., paths for which any changes
     * have high potential to cause disruptive "churn"
     */
    private static final boolean DO_CHURN = false;

    private static final boolean verbose_skipping = false;

    private static final String DIR_NAME = DO_CHURN ? "churn" : "delta";

    private static final boolean SKIP_REFORMAT_ANNOTATIONS = ToolConstants.PREV_CHART_VERSION.compareTo("30") >= 0;

    private static final PageId DEBUG_PAGE_ID = PageId.DayPeriod;

    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CLDRConfig.getInstance().getSupplementalDataInfo();

    private enum MyOptions {
        fileFilter(new Params().setHelp("filter files by dir/locale, eg: ^main/en$ or .*/en").setMatch(".*")),
        orgFilter(new Params().setHelp("filter files by organization").setMatch(".*")),
        Vxml(new Params().setHelp("use cldr-aux for the base directory")),
        coverageFilter(new Params().setHelp("filter files by coverage").setMatch(".*")),
        directory(new Params().setHelp("Set the output directory name").setDefault(DIR_NAME).setMatch(".*")),
        verbose(new Params().setHelp("verbose debugging messages")),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    private final Matcher fileFilter;
    private final String DIR;
    private final Level minimumPathCoverage;
    private final boolean verbose;

    public static void main(String[] args) {
        System.out.println("use -DCHART_VERSION=36.0 -DPREV_CHART_VERSION=34.0 to generate the differences between v36 and v34.");
        MyOptions.parse(args);
        Matcher fileFilter = !MyOptions.fileFilter.option.doesOccur() ? null : PatternCache.get(MyOptions.fileFilter.option.getValue()).matcher("");
        if (MyOptions.orgFilter.option.doesOccur()) {
            if (MyOptions.fileFilter.option.doesOccur()) {
                throw new IllegalArgumentException("Can't have both fileFilter and orgFilter");
            }
            String rawOrg = MyOptions.orgFilter.option.getValue();
            Organization org = Organization.fromString(rawOrg);
            Set<String> locales = StandardCodes.make().getLocaleCoverageLocales(org);
            fileFilter = PatternCache.get("^(main|annotations)/(" + Joiner.on("|").join(locales) + ")$").matcher("");
        }
        Level coverage = !MyOptions.coverageFilter.option.doesOccur() ? null : Level.fromString(MyOptions.coverageFilter.option.getValue());
        boolean verbose = MyOptions.verbose.option.doesOccur();
        String DIR = CLDRPaths.CHART_DIRECTORY + MyOptions.directory.option.getValue();
        ChartDelta temp = new ChartDelta(fileFilter, coverage, DIR, verbose);
        temp.writeChart(null);
        temp.showTotals();
        System.out.println("Finished. Files may have been created in these directories:");
        System.out.println(DIR);
        System.out.println(getTsvDir(DIR, DIR_NAME));
    }

    private ChartDelta(Matcher fileFilter, Level coverage, String dir, boolean verbose) {
        this.fileFilter = fileFilter;
        this.verbose = verbose;
        this.DIR = dir;
        this.minimumPathCoverage = coverage;
    }

    private static final String SEP = "\u0001";
    private static final boolean DEBUG = false;
    private static final String DEBUG_FILE = null; // "windowsZones.xml";
    static Pattern fileMatcher = PatternCache.get(".*");

    static PathHeader.Factory phf = PathHeader.getFactory(ENGLISH);
    static final Set<String> DONT_CARE = new HashSet<>(Arrays.asList("draft", "standard", "reference"));

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return DO_CHURN ? "Churn Charts" : "Delta Charts";
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
    public void writeContents(FormattedFileWriter pw) throws IOException {
        FormattedFileWriter.Anchors anchors = new FormattedFileWriter.Anchors();
        FileUtilities.copyFile(ChartDelta.class, "index.css", getDirectory());
        FormattedFileWriter.copyIncludeHtmls(getDirectory(), true);
        counter.clear();
        fileCounters.clear();
        writeNonLdmlPlain(anchors);
        writeLdml(anchors);
        pw.setIndex("Main Chart Index", "../index.html");
        pw.write(anchors.toString());
    }

    private static class PathHeaderSegment extends R3<PathHeader, Integer, String> {
        public PathHeaderSegment(PathHeader b, int elementIndex, String attribute) {
            super(b, elementIndex, attribute);
        }
    }

    private static class PathDiff extends R4<PathHeaderSegment, String, String, String> {
        public PathDiff(String locale, PathHeaderSegment pathHeaderSegment, String oldValue, String newValue) {
            super(pathHeaderSegment, locale, oldValue, newValue);
        }
    }

    private static final CLDRFile EMPTY_CLDR = new CLDRFile(new SimpleXMLSource("und").freeze());

    private static final File CLDR_BASE_DIR = CLDRConfig.getInstance().getCldrBaseDirectory();

    private enum ChangeType {
        added, deleted, changed, same;
        public static ChangeType get(String oldValue, String currentValue) {
            return oldValue == null ? added
                : currentValue == null ? deleted
                    : oldValue.equals(currentValue) ? same
                        : changed;
        }
    }

    private Counter<ChangeType> counter = new Counter<>();
    private Map<String, Counter<ChangeType>> fileCounters = new TreeMap<>();
    private Set<String> badHeaders = new TreeSet<>();

    private void addChange(String file, ChangeType changeType, int count) {
        counter.add(changeType, count); // unified add
        Counter<ChangeType> fileCounter = fileCounters.get(file);
        if (fileCounter == null) {
            fileCounters.put(file, fileCounter = new Counter<>());
        }
        fileCounter.add(changeType, count);
    }

    private void showTotals() {
        try (PrintWriter pw = FileUtilities.openUTF8Writer(getTsvDir(DIR, DIR_NAME), DIR_NAME + "_summary.tsv")) {
            pw.println("# percentages are of *new* total");
            pw.print("# dir\tfile");
            for (ChangeType item : ChangeType.values()) {
                pw.print("\t" + (item == ChangeType.same ? "total" : item.toString()));
            }
            pw.println();
            showTotal(pw, "TOTAL/", counter);

            for (Entry<String, Counter<ChangeType>> entry : fileCounters.entrySet()) {
                showTotal(pw, entry.getKey(), entry.getValue());
            }
            for (String s : badHeaders) {
                pw.println(s);
            }
            pw.println("# EOF");
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private void showTotal(PrintWriter pw, String title2, Counter<ChangeType> counter2) {
        long total = counter2.getTotal();
        NumberFormat pf = NumberFormat.getPercentInstance();
        pf.setMinimumFractionDigits(2);
        NumberFormat nf = NumberFormat.getIntegerInstance();
        pw.print(title2.replace("/", "\t"));
        for (ChangeType item : ChangeType.values()) {
            if (item == ChangeType.same) {
                pw.print("\t" + nf.format(total));
            } else {
                final long current = counter2.getCount(item);
                pw.print("\t" + nf.format(current));
            }
        }
        pw.println();
    }

    /**
     *
     * @param anchors
     * @throws IOException
     *
     * TODO: shorten the function using subroutines
     */
    private void writeLdml(Anchors anchors)  throws IOException {

        try (PrintWriter tsvFile = FileUtilities.openUTF8Writer(getTsvDir(DIR, DIR_NAME), DIR_NAME + ".tsv");
            PrintWriter tsvCountFile = FileUtilities.openUTF8Writer(getTsvDir(DIR, DIR_NAME), DIR_NAME + "_count.tsv");
            ) {
            tsvFile.println("# Section\tPage\tHeader\tCode\tLocale\tOld\tNew\tLevel");

            // set up factories
            List<Factory> factories = new ArrayList<>();
            List<Factory> oldFactories = new ArrayList<>();

            Counter<PathHeader> counts = new Counter<>();

            String dirBase = ToolConstants.getBaseDirectory(ToolConstants.CHART_VERSION);
            String prevDirBase = ToolConstants.getBaseDirectory(ToolConstants.PREV_CHART_VERSION);

            for (String dir : DtdType.ldml.directories) {
                if (dir.equals("annotationsDerived") || dir.equals("casing")) {
                    continue;
                }
                String current = dirBase + "common/" + dir;
                String past = prevDirBase + "common/" + dir;
                try {
                    factories.add(Factory.make(current, ".*"));
                } catch (Exception e1) {
                    System.out.println("Skipping: " + dir + "\t" + e1.getMessage());
                    continue; // skip where the directories don't exist in old versions
                }
                try {
                    oldFactories.add(Factory.make(past, ".*"));
                } catch (Exception e) {
                    System.out.println("Couldn't open factory: " + past);
                    past = null;
                    oldFactories.add(null);
                }
                System.out.println("Will compare: " + dir + "\t\t" + current + "\t\t" + past);
            }
            if (factories.isEmpty()) {
                throw new IllegalArgumentException("No factories found for "
                    + dirBase + ": " + DtdType.ldml.directories);
            }
            // get a list of all the locales to cycle over

            Relation<String, String> baseToLocales = Relation.of(new TreeMap<String, Set<String>>(), HashSet.class);
            Matcher m = fileMatcher.matcher("");
            Set<String> defaultContents = SDI.getDefaultContentLocales();
            LanguageTagParser ltp = new LanguageTagParser();
            LikelySubtags ls = new LikelySubtags();
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

            Relation<PathHeader, String> diffAll = Relation.of(new TreeMap<PathHeader, Set<String>>(), TreeSet.class);
            for (Entry<String, Set<String>> baseNLocale : baseToLocales.keyValuesSet()) {
                String base = baseNLocale.getKey();
                for (int i = 0; i < factories.size(); ++i) {
                    Factory factory = factories.get(i);
                    Factory oldFactory = oldFactories.get(i);
                    List<File> sourceDirs = Arrays.asList(factory.getSourceDirectories());
                    if (sourceDirs.size() != 1) {
                        throw new IllegalArgumentException("Internal error: expect single source dir");
                    }
                    File sourceDir = sourceDirs.get(0);
                    String sourceDirLeaf = sourceDir.getName();
                    boolean resolving = !sourceDirLeaf.contains("subdivisions")
                        && !sourceDirLeaf.contains("transforms");

                    for (String locale : baseNLocale.getValue()) {
                        String nameAndLocale = sourceDirLeaf + "/" + locale;
                        if (fileFilter != null && !fileFilter.reset(nameAndLocale).find()) {
                            if (verbose && verbose_skipping) {
                                System.out.println("SKIPPING: " + nameAndLocale);
                            }
                            continue;
                        }
                        if (verbose) {
                            System.out.println(nameAndLocale);
                        }
                        CLDRFile current = makeWithFallback(factory, locale, resolving);
                        CLDRFile old = makeWithFallback(oldFactory, locale, resolving);
                        if (!locale.equals("root") && current.getLocaleID().equals("root") && old.getLocaleID().equals("root")) {
                            continue;
                        }
                        if (old == EMPTY_CLDR && current == EMPTY_CLDR) {
                            continue;
                        }
                        paths.clear();
                        for (String path : current.fullIterable()) {
                            if (allowPath(locale, path)) {
                                paths.add(path);
                            }
                        }
                        for (String path : old.fullIterable()) {
                            if (!paths.contains(path) && allowPath(locale, path)) {
                                paths.add(path);
                            }
                        }

                        Output<String> reformattedValue = new Output<>();
                        Output<Boolean> hasReformattedValue = new Output<>();

                        for (String path : paths) {
                            if (DO_CHURN && !pathIsHighLevel(path)) {
                                continue;
                            }
                            if (path.startsWith("//ldml/identity")
                                || path.endsWith("/alias")
                                || path.startsWith("//ldml/segmentations") // do later
                                || path.startsWith("//ldml/rbnf") // do later
                                ) {
                                continue;
                            }
                            PathHeader ph = getPathHeader(path);
                            if (ph == null) {
                                continue;
                            }

                            String oldValue = null;
                            String currentValue = null;

                            {
                                String sourceLocaleCurrent = current.getSourceLocaleID(path, currentStatus);
                                String sourceLocaleOld = getReformattedPath(oldStatus, old, path, reformattedValue, hasReformattedValue);

                                // filter out stuff that differs at a higher level
                                if (!sourceLocaleCurrent.equals(locale)
                                    && !sourceLocaleOld.equals(locale)) {
                                    continue;
                                }
                                if (!path.equals(currentStatus.pathWhereFound)
                                    && !path.equals(oldStatus.pathWhereFound)) {
                                    continue;
                                }
                                // fix some incorrect cases?

                                currentValue = current.getStringValue(path);
                                if (CldrUtility.INHERITANCE_MARKER.equals(currentValue)) {
                                    currentValue = current.getConstructedBaileyValue(path, null, null);
                                }
                                oldValue = hasReformattedValue.value ? reformattedValue.value : old.getStringValue(path);
                                if (CldrUtility.INHERITANCE_MARKER.equals(oldValue)) {
                                    oldValue = old.getConstructedBaileyValue(path, null, null);
                                }
                            }
                            // handle non-distinguishing attributes
                            addPathDiff(sourceDir, old, current, locale, ph, diff);

                            addValueDiff(sourceDir, oldValue, currentValue, locale, ph, diff, diffAll);
                        }
                    }
                }
                writeDiffs(anchors, base, diff, tsvFile, counts);
                diff.clear();
            }
            writeDiffs(diffAll);

            writeCounter(tsvCountFile, "Count", counts);
            tsvFile.println("# EOF");
            tsvCountFile.println("# EOF");
        }

    }

    /**
     * Should the given path be taken into account for generating "churn" reports?
     *
     * @param path
     * @return true if it counts, else false to ignore
     */
    private boolean pathIsHighLevel(String path) {
        // TODO: more paths, use RegexLookup, read from file
        final Set<String> churnPaths = new HashSet<>(Arrays.asList(
            "//ldml/characters/exemplarCharacters",
            "//ldml/numbers/defaultNumberingSystem",
            "//ldml/numbers/otherNumberingSystems/native",
            "//ldml/localeDisplayNames/territories/territory",
            "//ldml/localeDisplayNames/languages/language",
            "//ldml/dates/fields/field[@type=\"year\"]/displayName",
            "//ldml/dates/fields/field[@type=\"month\"]/displayName",
            "//ldml/dates/fields/field[@type=\"week\"]/displayName",
            "//ldml/dates/fields/field[@type=\"day\"]/displayName",
            "//ldml/dates/fields/field[@type=\"hour\"]/displayName",
            "//ldml/dates/fields/field[@type=\"era\"]/displayName",
            "//ldml/dates/fields/field[@type=\"minute\"]/displayName",
            "//ldml/dates/fields/field[@type=\"second\"]/displayName",
            "//supplementalData/weekData/firstDay",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"full\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"long\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateFormats/dateFormatLength[@type=\"short\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"MMMEd\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"MEd\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"full\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"long\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"medium\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/timeFormats/timeFormatLength[@type=\"short\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"am\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"am\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"pm\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=\"pm\"]",
            "//ldml/numbers/currencies/currency[@type=\"KRW\"]/displayName",
            "//ldml/numbers/currencies/currency[@type=\"KRW\"]/symbol",
            "//ldml/numbers/currencies/currency[@type=\"KRW\"]/symbol[@alt=\"narrow\"]",
            "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/numbers/currencyFormats[@numberSystem=\"arab\"]/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/numbers/currencies/currency[@type=\"CNY\"]/symbol",
            "//ldml/numbers/currencies/currency[@type=\"CNY\"]/symbol[@alt=\"narrow\"]",
            "//ldml/numbers/minimumGroupingDigits",
            "//ldml/numbers/symbols[@numberSystem=\"latn\"]/decimal",
            "//ldml/numbers/symbols[@numberSystem=\"latn\"]/group",
            "//ldml/numbers/symbols[@numberSystem=\"arab\"]/decimal",
            "//ldml/numbers/symbols[@numberSystem=\"arab\"]/group",
            "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/numbers/percentFormats[@numberSystem=\"latn\"]/percentFormatLength/percentFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength/currencyFormat[@type=\"accounting\"]/pattern[@type=\"standard\"]",
            "//ldml/numbers/decimalFormats[@numberSystem=\"arab\"]/decimalFormatLength/decimalFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//ldml/numbers/percentFormats[@numberSystem=\"arab\"]/percentFormatLength/percentFormat[@type=\"standard\"]/pattern[@type=\"standard\"]",
            "//supplementalData/metadata/alias",
            "//supplementalData/territoryContainment"
        ));

        if (churnPaths.contains(path)) {
            return true;
        }
        return false;
    }

    private boolean allowPath(String locale, String path) {
        if (minimumPathCoverage != null) {
            Level pathLevel = SUPPLEMENTAL_DATA_INFO.getCoverageLevel(path, locale);
            if (minimumPathCoverage.compareTo(pathLevel) < 0) {
                return false;
            }
        }
        return true;
    }

    private String getReformattedPath(Status oldStatus, CLDRFile old, String path, Output<String> value, Output<Boolean> hasReformattedValue) {
        if (SKIP_REFORMAT_ANNOTATIONS || !path.startsWith("//ldml/annotations/")) {
            hasReformattedValue.value = Boolean.FALSE;
            return old.getSourceLocaleID(path, oldStatus);
        }
        // OLD:     <annotation cp='[😀]' tts='grinning face'>face; grin</annotation>
        // NEW:     <annotation cp="😀">face | grin</annotation>
        //          <annotation cp="😀" type="tts">grinning face</annotation>
        // from the NEW paths, get the OLD values
        XPathParts parts = XPathParts.getFrozenInstance(path).cloneAsThawed(); // not frozen, for removeAttribute
        boolean isTts = parts.getAttributeValue(-1, "type") != null;
        if (isTts) {
            parts.removeAttribute(-1, "type");
        }
        String cp = parts.getAttributeValue(-1, "cp");
        parts.setAttribute(-1, "cp", "[" + cp + "]");

        String oldStylePath = parts.toString();
        String temp = old.getStringValue(oldStylePath);
        if (temp == null) {
            hasReformattedValue.value = Boolean.FALSE;
        } else if (isTts) {
            String temp2 = old.getFullXPath(oldStylePath);
            value.value = XPathParts.getFrozenInstance(temp2).getAttributeValue(-1, "tts");
            hasReformattedValue.value = Boolean.TRUE;
        } else {
            value.value = temp.replaceAll("\\s*;\\s*", " | ");
            hasReformattedValue.value = Boolean.TRUE;
        }
        return old.getSourceLocaleID(oldStylePath, oldStatus);
    }

    PathStarrer starrer = new PathStarrer().setSubstitutionPattern("%A");

    private PathHeader getPathHeader(String path) {
        try {
            PathHeader ph = phf.fromPath(path);
            if (ph.getPageId() == PageId.Unknown) {
                String star = starrer.set(path);
                badHeaders.add(star);
                return null;
            }
            return ph;
        } catch (Exception e) {
            String star = starrer.set(path);
            badHeaders.add(star);
            // System.err.println("Skipping path with bad PathHeader: " + path);
            return null;
        }
    }

    private CLDRFile makeWithFallback(Factory oldFactory, String locale, boolean resolving) {
        if (oldFactory == null) {
            return EMPTY_CLDR;
        }
        CLDRFile old;
        String oldLocale = locale;
        while (true) { // fall back for old, maybe to root
            try {
                old = oldFactory.make(oldLocale, resolving);
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

    private void addPathDiff(File sourceDir, CLDRFile old, CLDRFile current, String locale, PathHeader ph, Set<PathDiff> diff2) {
        String path = ph.getOriginalPath();
        String fullPathCurrent = current.getFullXPath(path);
        String fullPathOld = old.getFullXPath(path);
        if (Objects.equals(fullPathCurrent, fullPathOld)) {
            return;
        }
        XPathParts pathPlain = XPathParts.getFrozenInstance(path);
        XPathParts pathCurrent = fullPathCurrent == null ? pathPlain : XPathParts.getFrozenInstance(fullPathCurrent);
        XPathParts pathOld = fullPathOld == null ? pathPlain : XPathParts.getFrozenInstance(fullPathOld);
        TreeSet<String> fullAttributes = null;
        int size = pathCurrent.size();
        String parentAndName = parentAndName(sourceDir, locale);
        for (int elementIndex = 0; elementIndex < size; ++elementIndex) { // will have same size
            Collection<String> distinguishing = pathPlain.getAttributeKeys(elementIndex);
            Collection<String> attributesCurrent = pathCurrent.getAttributeKeys(elementIndex);
            Collection<String> attributesOld = pathCurrent.getAttributeKeys(elementIndex);
            if (attributesCurrent.isEmpty() && attributesOld.isEmpty()) {
                continue;
            }
            if (fullAttributes == null) {
                fullAttributes = new TreeSet<>();
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
                    addChange(parentAndName, ChangeType.same, 1);
                    continue;
                }
                addChange(parentAndName, ChangeType.get(attributeValueOld, attributeValueCurrent), 1);

                PathDiff row = new PathDiff(
                    locale,
                    new PathHeaderSegment(ph, size - elementIndex - 1, attribute),
                    attributeValueOld,
                    attributeValueCurrent);
                if (DEBUG) {
                    System.out.println(row);
                }
                diff2.add(row);
            }
        }
    }

    private String parentAndName(File sourceDir, String locale) {
        return sourceDir.getName() + "/" + locale + ".xml";
    }

    private void addValueDiff(File sourceDir, String valueOld, String valueCurrent, String locale, PathHeader ph, Set<PathDiff> diff,
        Relation<PathHeader, String> diffAll) {
        // handle stuff that can be split specially
        Splitter splitter = getSplitter(ph.getOriginalPath(), valueOld, valueCurrent);
        int count = 1;
        String parentAndName = parentAndName(sourceDir, locale);
        if (Objects.equals(valueCurrent, valueOld)) {
            if (splitter != null && valueCurrent != null) {
                count = splitHandlingNull(splitter, valueCurrent).size();
            }
            addChange(parentAndName, ChangeType.same, count);
        } else {
            if (splitter != null) {
                List<String> setOld = splitHandlingNull(splitter, valueOld);
                List<String> setNew = splitHandlingNull(splitter, valueCurrent);
                int[] sameAndNotInSecond = new int[2];
                valueOld = getFilteredValue(setOld, setNew, sameAndNotInSecond);
                addChange(parentAndName, ChangeType.same, sameAndNotInSecond[0]);
                addChange(parentAndName, ChangeType.deleted, sameAndNotInSecond[1]);
                sameAndNotInSecond[0] = sameAndNotInSecond[1] = 0;
                valueCurrent = getFilteredValue(setNew, setOld, sameAndNotInSecond);
                addChange(parentAndName, ChangeType.added, sameAndNotInSecond[1]);
            } else {
                addChange(parentAndName, ChangeType.get(valueOld, valueCurrent), count);
            }
            PathDiff row = new PathDiff(locale, new PathHeaderSegment(ph, -1, ""), valueOld, valueCurrent);
            diff.add(row);
            diffAll.put(ph, locale);
        }
    }

    private List<String> splitHandlingNull(Splitter splitter, String value) {
        return value == null ? null : splitter.splitToList(value);
    }

    private Splitter getSplitter(String path, String valueOld, String valueCurrent) {
        if (path.contains("/annotation") && !path.contains("tts")) {
            return DtdData.BAR_SPLITTER;
        } else if (valueOld != null && valueOld.contains("\n") || valueCurrent != null && valueCurrent.contains("\n")) {
            return DtdData.CR_SPLITTER;
        } else {
            return null;
        }
    }

    /**
     * Return string with all lines from linesToRemove removed
     * @param toGetStringFor
     * @param linesToRemove
     * @return
     */
    private String getFilteredValue(Collection<String> toGetStringFor, Collection<String> linesToRemove,
        int[] sameAndDiff) {
        if (toGetStringFor == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        Set<String> toRemove = linesToRemove == null ? Collections.emptySet() : new HashSet<>(linesToRemove);
        boolean removed = false;
        for (String old : toGetStringFor) {
            if (toRemove.contains(old)) {
                removed = true;
                sameAndDiff[0]++;
            } else {
                sameAndDiff[1]++;
                if (removed) {
                    buf.append("…\n");
                    removed = false;
                }
                buf.append(old).append('\n');
            }
        }
        if (removed) {
            buf.append("…");
        } else if (buf.length() > 0) {
            buf.setLength(buf.length() - 1); // remove final \n
        }
        return buf.toString();
    }

    private void writeDiffs(Anchors anchors, String file, String title, Multimap<PathHeader, String> bcp, PrintWriter tsvFile) {
        if (bcp.isEmpty()) {
            System.out.println("\tDeleting: " + DIR + "/" + file);
            new File(DIR + file).delete();
            return;
        }
        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Section", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Page", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)//.setRepeatDivider(true)
            .addColumn("Header", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Code", "class='source'", null, "class='source'", false)
            .addColumn("Old", "class='target'", null, "class='target'", false) //  width='20%'
            .addColumn("New", "class='target'", null, "class='target'", false); //  width='20%'
        PathHeader ph1 = phf.fromPath("//supplementalData/metadata/alias/subdivisionAlias[@type=\"TW-TXQ\"]/_reason");
        PathHeader ph2 = phf.fromPath("//supplementalData/metadata/alias/subdivisionAlias[@type=\"LA-XN\"]/_replacement");
        ph1.compareTo(ph2);
        for (Entry<PathHeader, Collection<String>> entry : bcp.asMap().entrySet()) {
            PathHeader ph = entry.getKey();
            if (ph.getPageId() == DEBUG_PAGE_ID) {
                System.out.println(ph + "\t" + ph.getOriginalPath());
            }
            for (String value : entry.getValue()) {
                String[] oldNew = value.split(SEP);
                tablePrinter.addRow()
                .addCell(ph.getSectionId())
                .addCell(ph.getPageId())
                .addCell(ph.getHeader())
                .addCell(ph.getCode())
                .addCell(oldNew[0])
                .addCell(oldNew[1])
                .finishRow();
            }
        }
        writeTable(anchors, file, tablePrinter, title, tsvFile);
    }

    private void writeDiffs(Relation<PathHeader, String> diffAll) {
        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Section", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Page", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Header", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Code", "class='source'", null, "class='source'", true)
            .addColumn("Locales where different", "class='target'", null, "class='target'", true);
        for (Entry<PathHeader, Set<String>> row : diffAll.keyValuesSet()) {
            PathHeader ph = row.getKey();
            Set<String> locales = row.getValue();
            tablePrinter.addRow()
            .addCell(ph.getSectionId())
            .addCell(ph.getPageId())
            .addCell(ph.getHeader())
            .addCell(ph.getCode())
            .addCell(Joiner.on(" ").join(locales))
            .finishRow();
        }
    }

    private void writeDiffs(Anchors anchors, String file, Set<PathDiff> diff, PrintWriter tsvFile, Counter<PathHeader> counts) {
        if (diff.isEmpty()) {
            return;
        }
        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Section", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Page", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Header", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Code", "class='source'", null, "class='source'", true)
            .addColumn("Locale", "class='source'", null, "class='source'", true)
            .addColumn("Old", "class='target'", null, "class='target'", true) //  width='20%'
            .addColumn("New", "class='target'", null, "class='target'", true) //  width='20%'
            .addColumn("Level", "class='target'", null, "class='target'", true);

        for (PathDiff row : diff) {
            PathHeaderSegment phs = row.get0();
            counts.add(phs.get0(), 1);
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
        String title = ENGLISH.getName(file) + (DO_CHURN ? " Churn" : " Delta");
        writeTable(anchors, file, tablePrinter, title, tsvFile);

        diff.clear();
    }

    private class ChartDeltaSub extends Chart {
        private String title;
        private String file;
        private TablePrinter tablePrinter;
        private PrintWriter tsvFile;

        private ChartDeltaSub(String title, String file, TablePrinter tablePrinter, PrintWriter tsvFile) {
            super();
            this.title = title;
            this.file = file;
            this.tablePrinter = tablePrinter;
            this.tsvFile = tsvFile;
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
            return "<p>Lists data fields that differ from the last major version (see versions above)."
                + " Inherited differences in locales are suppressed, except where the source locales are different. "
                + "<p>";
        }

        @Override
        public void writeContents(FormattedFileWriter pw) throws IOException {
            pw.write(tablePrinter.toTable());
            tablePrinter.toTsv(tsvFile);
        }
    }

    private void writeTable(Anchors anchors, String file, TablePrinter tablePrinter, String title, PrintWriter tsvFile) {
        ChartDeltaSub chartDeltaSub = new ChartDeltaSub(title, file, tablePrinter, tsvFile);
        chartDeltaSub.writeChart(anchors);
    }

    private void writeNonLdmlPlain(Anchors anchors) throws IOException {
        try (PrintWriter tsvFile = FileUtilities.openUTF8Writer(getTsvDir(DIR, DIR_NAME), DIR_NAME + "_supp.tsv");
            PrintWriter tsvCountFile = FileUtilities.openUTF8Writer(getTsvDir(DIR, DIR_NAME), DIR_NAME + "_supp_count.tsv");
            ) {
            tsvFile.println("# Section\tPage\tHeader\tCode\tOld\tNew");

            Multimap<PathHeader, String> bcp = TreeMultimap.create();
            Multimap<PathHeader, String> supplemental = TreeMultimap.create();
            Multimap<PathHeader, String> transforms = TreeMultimap.create();

            Counter<PathHeader> countSame = new Counter<>();
            Counter<PathHeader> countAdded = new Counter<>();
            Counter<PathHeader> countDeleted = new Counter<>();

            for (String dir : new File(CLDRPaths.BASE_DIRECTORY + "common/").list()) {
                if (DtdType.ldml.directories.contains(dir)
                    || dir.equals(".DS_Store")
                    || dir.equals("dtd") // TODO as flat files
                    || dir.equals("properties") // TODO as flat files
                    || dir.equals("uca") // TODO as flat files
                    ) {
                    continue;
                }
                File dirOld = new File(PREV_CHART_VERSION_DIRECTORY + "common/" + dir);
                System.out.println("\tLast dir: " + dirOld);
                File dir2 = new File(CHART_VERSION_DIRECTORY + "common/" + dir);
                System.out.println("\tCurr dir: " + dir2);

                for (String file : dir2.list()) {
                    if (!file.endsWith(".xml")) {
                        continue;
                    }
                    String parentAndFile = dir + "/" + file;
                    String base = file.substring(0, file.length() - 4);
                    if (fileFilter != null && !fileFilter.reset(dir + "/" + base).find()) {
                        if (verbose) { //  && verbose_skipping
                            System.out.println("SKIPPING: " + dir + "/" + base);
                        }
                        continue;
                    }

                    if (verbose) {
                        System.out.println(file);
                    }
                    Relation<PathHeader, String> contentsOld = fillData(dirOld.toString() + "/", file);
                    Relation<PathHeader, String> contents2 = fillData(dir2.toString() + "/", file);

                    Set<PathHeader> keys = new TreeSet<>(CldrUtility.ifNull(contentsOld.keySet(), Collections.<PathHeader> emptySet()));
                    keys.addAll(CldrUtility.ifNull(contents2.keySet(), Collections.<PathHeader> emptySet()));
                    DtdType dtdType = null;
                    for (PathHeader key : keys) {
                        String originalPath = key.getOriginalPath();
                        if (DO_CHURN && !pathIsHighLevel(originalPath)) {
                            continue;
                        }
                        boolean isTransform = originalPath.contains("/tRule");
                        if (dtdType == null) {
                            dtdType = DtdType.fromPath(originalPath);
                        }
                        Multimap<PathHeader, String> target = dtdType == DtdType.ldmlBCP47 ? bcp
                            : isTransform ? transforms
                                : supplemental;
                        Set<String> setOld = contentsOld.get(key);
                        Set<String> set2 = contents2.get(key);

                        if (Objects.equals(setOld, set2)) {
                            if (file.equals(DEBUG_FILE)) { // for debugging
                                System.out.println("**Same: " + key + "\t" + setOld);
                            }
                            addChange(parentAndFile, ChangeType.same, setOld.size());
                            countSame.add(key, 1);
                            continue;
                        }
                        if (setOld == null) {
                            addChange(parentAndFile, ChangeType.added, set2.size());
                            for (String s : set2) {
                                addRow(target, key, "▷missing◁", s);
                                countAdded.add(key, 1);
                            }
                        } else if (set2 == null) {
                            addChange(parentAndFile, ChangeType.deleted, setOld.size());
                            for (String s : setOld) {
                                addRow(target, key, s, "▷removed◁");
                                countDeleted.add(key, 1);
                            }
                        } else {
                            Set<String> s1MOld = setOld;
                            Set<String> s2M1 = set2;
                            if (s1MOld.isEmpty()) {
                                addRow(target, key, "▷missing◁", Joiner.on(", ").join(s2M1));
                                addChange(parentAndFile, ChangeType.added, s2M1.size());
                                countAdded.add(key, 1);
                            } else if (s2M1.isEmpty()) {
                                addRow(target, key, Joiner.on(", ").join(s1MOld), "▷removed◁");
                                addChange(parentAndFile, ChangeType.deleted, s1MOld.size());
                                countDeleted.add(key, 1);
                            } else {
                                String valueOld;
                                String valueCurrent;

                                int[] sameAndNotInSecond = new int[2];
                                valueOld = getFilteredValue(s1MOld, s1MOld, sameAndNotInSecond);
                                addChange(parentAndFile, ChangeType.same, sameAndNotInSecond[0]);
                                countSame.add(key, 1);
                                addChange(parentAndFile, ChangeType.deleted, sameAndNotInSecond[1]);
                                sameAndNotInSecond[1] = 0;
                                countDeleted.add(key, 1);
                                valueCurrent = getFilteredValue(s2M1, s1MOld, sameAndNotInSecond);
                                addChange(parentAndFile, ChangeType.added, sameAndNotInSecond[1]);
                                addRow(target, key, valueOld, valueCurrent);
                                countAdded.add(key, 1);
                            }
                        }
                    }
                }
            }
            String deltaChurn = DO_CHURN ? "Churn" : "Delta";
            writeDiffs(anchors, "bcp47", "¤¤BCP47 " + deltaChurn, bcp, tsvFile);
            writeDiffs(anchors, "supplemental-data", "¤¤Supplemental " + deltaChurn, supplemental, tsvFile);
            writeDiffs(anchors, "transforms", "¤¤Transforms " + deltaChurn, transforms, tsvFile);

            writeCounter(tsvCountFile, "CountSame", countSame);
            tsvCountFile.println();
            writeCounter(tsvCountFile, "CountAdded", countAdded);
            tsvCountFile.println();
            writeCounter(tsvCountFile, "CountDeleted", countDeleted);

            tsvFile.println("# EOF");
            tsvCountFile.println("# EOF");
        }
    }

    private void writeCounter(PrintWriter tsvFile, String title, Counter<PathHeader> countDeleted) {
        tsvFile.append("# "
            + title
            + "\tPathHeader\n\n");
        for (R2<Long, PathHeader> entry : countDeleted.getEntrySetSortedByCount(false, null)) {
            tsvFile.println(entry.get0() + "\t" + entry.get1());
        }
    }

    private void addRow(Multimap<PathHeader, String> target, PathHeader key, String oldItem, String newItem) {
        if (oldItem.isEmpty() || newItem.isEmpty()) {
            throw new IllegalArgumentException();
        }
        target.put(key, oldItem + SEP + newItem);
    }

    private Relation<PathHeader, String> fillData(String directory, String file) {
        Relation<PathHeader, String> results = Relation.of(new TreeMap<PathHeader, Set<String>>(), TreeSet.class);

        List<Pair<String, String>> contents1;
        try {
            contents1 = XMLFileReader.loadPathValues(directory + file, new ArrayList<Pair<String, String>>(), true);
        } catch (Exception e) {
            return results;
        }
        DtdType dtdType = null;
        DtdData dtdData = null;
        Multimap<String, String> extras = TreeMultimap.create();

        for (Pair<String, String> s : contents1) {
            String path = s.getFirst();
            if (DO_CHURN && !pathIsHighLevel(path)) {
                continue;
            }
            String value = s.getSecond();
            if (dtdType == null) {
                /*
                 * TODO: fix or explain: if dtdType and dtdData depend on path, why is this done
                 * only the first time through the loop? Do all the paths in this loop have the
                 * same dtdType? Also, when we're looking at paths from an old archived version,
                 * should we use the old archived DTD instead of the current one in CLDR_BASE_DIR?
                 */
                dtdType = DtdType.fromPath(path);
                dtdData = DtdData.getInstance(dtdType, CLDR_BASE_DIR);
            }
            XPathParts pathPlain = XPathParts.getFrozenInstance(path);
            try {
                if (dtdData.isMetadata(pathPlain)) {
                    continue;
                }
            } catch (NullPointerException e) {
                /*
                 * TODO: this happens for "grammaticalState" in this path from version 37:
                 * //supplementalData/grammaticalData/grammaticalFeatures[@targets="nominal"][@locales="he"]/grammaticalState[@values="definite indefinite construct"]
                 * Referencw: https://unicode-org.atlassian.net/browse/CLDR-13306
                 */
                System.out.println("Caught NullPointerException in fillData calling isMetadata, path = " + path);
                continue;
            }
            Set<String> pathForValues = dtdData.getRegularizedPaths(pathPlain, extras);
            if (pathForValues != null) {
                for (String pathForValue : pathForValues) {
                    PathHeader pathHeader = phf.fromPath(pathForValue);
                    Splitter splitter = DtdData.getValueSplitter(pathPlain);
                    for (String line : splitter.split(value)) {
                        // special case # in transforms
                        if (isComment(pathPlain, line)) {
                            continue;
                        }
                        results.put(pathHeader, line);
                    }
                }
            }
            for (Entry<String, Collection<String>> entry : extras.asMap().entrySet()) {
                final String extraPath = entry.getKey();
                final PathHeader pathHeaderExtra = phf.fromPath(extraPath);
                final Collection<String> extraValue = entry.getValue();
                if (isExtraSplit(extraPath)) {
                    for (String items : extraValue) {
                        results.putAll(pathHeaderExtra, DtdData.SPACE_SPLITTER.splitToList(items));
                    }
                } else {
                    results.putAll(pathHeaderExtra, extraValue);
                }
            }
            if (pathForValues == null && !value.isEmpty()) {
                System.err.println("Shouldn't happen");
            }
        }
        return results;
    }

    private boolean isExtraSplit(String extraPath) {
        if (extraPath.endsWith("/_type") && extraPath.startsWith("//supplementalData/metaZones/mapTimezones")) {
            return true;
        }
        return false;
    }

    private static boolean isComment(XPathParts pathPlain, String line) {
        if (pathPlain.contains("transform")) {
            if (line.startsWith("#")) {
                return true;
            }
        }
        return false;
    }
}
