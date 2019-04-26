package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.VersionInfo;

public class GenerateItemCounts {
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CLDRConfig.getInstance().getSupplementalDataInfo();
    private static final boolean SKIP_ORDERING = true;
    private static final String OUT_DIRECTORY = CLDRPaths.GEN_DIRECTORY + "/itemcount/"; // CldrUtility.MAIN_DIRECTORY;
    private Map<String, List<StackTraceElement>> cantRead = new TreeMap<String, List<StackTraceElement>>();
    static {
        System.err.println("Probably obsolete tool");
    }
    private static String[] DIRECTORIES = {
        // MUST be oldest first!
        // "cldr-archive/cldr-21.0",
        // "cldr-24.0",
        "cldr-27.0",
        "trunk"
    };

    private static String TRUNK_VERSION = "26.0";

    static boolean doChanges = true;
    static Relation<String, String> path2value = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
    static final AttributeTypes ATTRIBUTE_TYPES = new AttributeTypes();

    final static Options myOptions = new Options();

    enum MyOptions {
        summary(null, null, "if present, summarizes data already collected. Run once with, once without."), directory(".*", ".*",
            "if summary, creates filtered version (eg -d main): does a find in the name, which is of the form dir/file"), verbose(null, null,
                "verbose debugging messages"), rawfilter(".*", ".*", "filter the raw files (non-summary, mostly for debugging)"),;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static Matcher DIR_FILE_MATCHER;
    static Matcher RAW_FILE_MATCHER;
    static boolean VERBOSE;

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.directory, args, true);

        DIR_FILE_MATCHER = PatternCache.get(MyOptions.directory.option.getValue()).matcher("");
        RAW_FILE_MATCHER = PatternCache.get(MyOptions.rawfilter.option.getValue()).matcher("");
        VERBOSE = MyOptions.verbose.option.doesOccur();

        if (MyOptions.summary.option.doesOccur()) {
            doSummary();
            System.out.println("DONE");
            return;
            // } else if (arg.equals("changes")) {
            // doChanges = true;
        } else {
        }
        // Pattern dirPattern = dirPattern = PatternCache.get(arg);
        GenerateItemCounts main = new GenerateItemCounts();
        try {
            Relation<String, String> oldPath2value = null;
            for (String dir : DIRECTORIES) {
                // if (dirPattern != null && !dirPattern.matcher(dir).find()) continue;
                final String pathname = dir.equals("trunk") ? CLDRPaths.BASE_DIRECTORY
                    : CLDRPaths.ARCHIVE_DIRECTORY + "/" + dir;
                boolean isFinal = dir == DIRECTORIES[DIRECTORIES.length - 1];

                String fulldir = new File(pathname).getCanonicalPath();
                String prefix = (MyOptions.rawfilter.option.doesOccur() ? "filtered_" : "");
                String fileKey = dir.replace("/", "_");
                try (
                    PrintWriter summary = FileUtilities.openUTF8Writer(OUT_DIRECTORY, prefix + fileKey + "_count.txt");
                    PrintWriter changes = FileUtilities.openUTF8Writer(OUT_DIRECTORY, prefix + fileKey + "_changes.txt");
                    PrintWriter changesNew = FileUtilities.openUTF8Writer(OUT_DIRECTORY, prefix + fileKey + "_news.txt");
                    PrintWriter changesDeletes = FileUtilities.openUTF8Writer(OUT_DIRECTORY, prefix + fileKey + "_deletes.txt");
                    PrintWriter changesSummary = FileUtilities.openUTF8Writer(OUT_DIRECTORY, prefix + fileKey + "_changes_summary.txt");) {
                    main.summarizeCoverage(summary, fulldir, isFinal);
                    if (doChanges) {
                        if (oldPath2value != null) {
                            compare(summary, changes, changesNew, changesDeletes, changesSummary, oldPath2value, path2value);
                            checkBadAttributes(path2value, prefix + fileKey + "_dtd_check.txt");
                        }
                        oldPath2value = path2value;
                        path2value = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
                    }
                }
            }
            ATTRIBUTE_TYPES.showStarred();
        } finally {
            if (main.cantRead.size() != 0) {
                System.out.println("Couldn't read:\t");
                for (String file : main.cantRead.keySet()) {
                    System.out.println(file + "\t" + main.cantRead.get(file));
                }
            }
            System.out.println("DONE");
        }
    }

    static final Set<String> SKIP_ATTRIBUTES = new HashSet<>(Arrays.asList("draft", "references", "validSubLocales"));

    static final Relation<String, DtdType> ELEMENTS_OCCURRING = Relation.of(new TreeMap(), TreeSet.class);
    static final Relation<String, DtdType> ELEMENTS_POSSIBLE = Relation.of(new TreeMap(), TreeSet.class);
    static final Relation<String, Row.R2<DtdType, String>> ATTRIBUTES_OCCURRING = Relation.of(new TreeMap(), TreeSet.class);
    static final Relation<String, Row.R2<DtdType, String>> ATTRIBUTES_POSSIBLE = Relation.of(new TreeMap(), TreeSet.class);

    private static void checkBadAttributes(Relation<String, String> path2value2, String outputFile)
        throws IOException {
        // an attribute is misplaced if it is not distinguishing, but is on a non-final node.

        Set<String> errors = new LinkedHashSet<>();

        SupplementalDataInfo supp = SUPPLEMENTAL_DATA_INFO;
        for (DtdType dtdType : DtdType.values()) {
            if (dtdType == DtdType.ldmlICU) {
                continue;
            }
            DtdData data = DtdData.getInstance(dtdType);
            for (Element element : data.getElements()) {
                String elementName = element.name;
                ELEMENTS_POSSIBLE.put(elementName, dtdType);
                final Set<Element> children = element.getChildren().keySet();

                boolean skipFinal = children.isEmpty()
                    || children.size() == 1
                        && children.iterator().next().name.equals("special");

                for (Entry<Attribute, Integer> attributeInt : element.getAttributes().entrySet()) {
                    Attribute attribute = attributeInt.getKey();
                    String attributeName = attribute.name;
                    if (attribute.defaultValue != null) {
                        errors.add("Warning, default value «" + attribute.defaultValue
                            + "» for: " + dtdType + "\t" + elementName + "\t" + attributeName);
                    }
                    final R2<DtdType, String> attributeRow = Row.of(dtdType, elementName);
                    ATTRIBUTES_POSSIBLE.put(attributeName, attributeRow);
                    if (skipFinal || SKIP_ATTRIBUTES.contains(attributeName)) { // don't worry about non-final, references, draft, standard
                        continue;
                    }
                    if (supp.isDeprecated(dtdType, elementName, attributeName, null)) {
                        continue;
                    }
                    if (!CLDRFile.isDistinguishing(dtdType, elementName, attributeName)) {
                        String doesOccur = "";
                        final Set<R2<DtdType, String>> attributeRows = ATTRIBUTES_OCCURRING.get(attributeName);
                        if (attributeRows == null || !attributeRows.contains(attributeRow)) {
                            doesOccur = "\tNEVER";
                        }
                        errors.add("Warning, !disting, !leaf: " + dtdType + "\t" + elementName + "\t" + attributeName + "\t" + children + doesOccur);
                    }
                }
            }
        }
        try (
            PrintWriter out = FileUtilities.openUTF8Writer(OUT_DIRECTORY, outputFile)) {
            out.println("\nElements\tDeprecated\tOccurring\tPossible in DTD, but never occurs");

            for (Entry<String, Set<DtdType>> x : ELEMENTS_POSSIBLE.keyValuesSet()) {
                final String element = x.getKey();
                if (element.equals("#PCDATA") || element.equals("ANY") || element.equals("generation")) {
                    continue;
                }
                final Set<DtdType> possible = x.getValue();
                Set<DtdType> deprecated = new TreeSet();
                for (DtdType dtdType : possible) {
                    if (SUPPLEMENTAL_DATA_INFO.isDeprecated(dtdType, element, "*", "*")) {
                        deprecated.add(dtdType);
                    }
                }
                Set<DtdType> notDeprecated = new TreeSet(possible);
                notDeprecated.removeAll(deprecated);

                Set<DtdType> occurs = CldrUtility.ifNull(ELEMENTS_OCCURRING.get(element), Collections.EMPTY_SET);
                Set<DtdType> noOccur = new TreeSet(possible);
                noOccur.removeAll(occurs);

                if (!Collections.disjoint(deprecated, occurs)) { // deprecated must not occur
                    final Set<DtdType> intersection = CldrUtility.intersect(deprecated, occurs);
                    errors.add("Error: element «" + element
                        + "» is deprecated in " + (deprecated.equals(possible) ? "EVERYWHERE" : intersection) +
                        " but occurs in live data: " + intersection);
                }
                if (!Collections.disjoint(notDeprecated, noOccur)) { // if !deprecated & !occur, warning
                    errors.add("Warning: element «" + element
                        + "» doesn't occur in and is not deprecated in " + CldrUtility.intersect(notDeprecated, noOccur));
                }

                out.println(element
                    + "\t" + deprecated
                    + "\t" + occurs
                    + "\t" + noOccur);
            }

            out.println("\nAttributes\tDeprecated\tOccurring\tPossible in DTD, but never occurs");

            for (Entry<String, Set<R2<DtdType, String>>> x : ATTRIBUTES_POSSIBLE.keyValuesSet()) {
                final String attribute = x.getKey();
                if (attribute.equals("alt") || attribute.equals("draft") || attribute.equals("references")) {
                    continue;
                }
                final Set<R2<DtdType, String>> possible = x.getValue();
                Set<R2<DtdType, String>> deprecated = new TreeSet();
                for (R2<DtdType, String> s : possible) {
                    final DtdType dtdType = s.get0();
                    final String element = s.get1();
                    if (SUPPLEMENTAL_DATA_INFO.isDeprecated(dtdType, element, attribute, "*")) {
                        deprecated.add(s);
                    }
                }
                Set<R2<DtdType, String>> notDeprecated = new TreeSet(possible);
                notDeprecated.removeAll(deprecated);

                Set<R2<DtdType, String>> occurs = CldrUtility.ifNull(ATTRIBUTES_OCCURRING.get(attribute), Collections.EMPTY_SET);
                Set<R2<DtdType, String>> noOccur = new TreeSet(possible);
                noOccur.removeAll(occurs);

                if (!Collections.disjoint(deprecated, occurs)) { // deprecated must not occur
                    final Set<R2<DtdType, String>> intersection = CldrUtility.intersect(deprecated, occurs);
                    errors.add("Error: attribute «" + attribute
                        + "» is deprecated in " + (deprecated.equals(possible) ? "EVERYWHERE" : intersection) +
                        " but occurs in live data: " + intersection);
                }
                if (!Collections.disjoint(notDeprecated, noOccur)) { // if !deprecated & !occur, warning
                    errors.add("Warning: attribute «" + attribute
                        + "» doesn't occur in and is not deprecated in " + CldrUtility.intersect(notDeprecated, noOccur));
                }
                out.println(attribute
                    + "\t" + deprecated
                    + "\t" + occurs
                    + "\t" + noOccur);
            }
            out.println("\nERRORS/WARNINGS");
            out.println(CollectionUtilities.join(errors, "\n"));
        }
    }

    static class AttributeTypes {
        Relation<String, String> elementPathToAttributes = Relation.of(new TreeMap<String, Set<String>>(),
            TreeSet.class);
        final PathStarrer PATH_STARRER = new PathStarrer().setSubstitutionPattern("*");
        final Set<String> STARRED_PATHS = new TreeSet<String>();
        XPathParts parts = new XPathParts();
        StringBuilder elementPath = new StringBuilder();

        public void add(String path) {
            XPathParts parts = XPathParts.getTestInstance(path);
            elementPath.setLength(0);
            for (int i = 0; i < parts.size(); ++i) {
                String element = parts.getElement(i);
                elementPath.append('/').append(element);
                elementPathToAttributes.putAll(elementPath.toString().intern(), parts.getAttributeKeys(i));
            }
        }

        public void showStarred() throws IOException {
            PrintWriter starred = FileUtilities.openUTF8Writer(OUT_DIRECTORY, "starred" + ".txt");

            for (Entry<String, Set<String>> entry : elementPathToAttributes.keyValuesSet()) {
                Set<String> attributes = entry.getValue();
                if (attributes.size() == 0) {
                    continue;
                }
                String path = entry.getKey();
                String[] elements = path.split("/");
                DtdType type = DtdType.valueOf(elements[1]);
                String finalElement = elements[elements.length - 1];
                starred.print(path);
                for (String attribute : attributes) {
                    if (CLDRFile.isDistinguishing(type, finalElement, attribute)) {
                        starred.print("[@" + attribute + "='disting.']");
                    } else {
                        starred.print("[@" + attribute + "='DATA']");
                    }
                }
                starred.println();
            }
            starred.close();
        }
    }

    static Pattern prefix = PatternCache.get("([^/]+/[^/]+)(.*)");

    static class Delta {
        Counter<String> newCount = new Counter<String>();
        Counter<String> deletedCount = new Counter<String>();
        Counter<String> changedCount = new Counter<String>();
        Counter<String> unchangedCount = new Counter<String>();

        void print(PrintWriter changesSummary, Set<String> prefixes) {
            changesSummary.println("Total"
                + "\t" + unchangedCount.getTotal()
                + "\t" + deletedCount.getTotal()
                + "\t" + changedCount.getTotal()
                + "\t" + newCount.getTotal());
            changesSummary.println("Directory\tSame\tRemoved\tChanged\tAdded");
            for (String prefix : prefixes) {
                changesSummary.println(prefix
                    + "\t" + unchangedCount.get(prefix)
                    + "\t" + deletedCount.get(prefix)
                    + "\t" + changedCount.get(prefix)
                    + "\t" + newCount.get(prefix));
            }
        }
    }

    private static void compare(PrintWriter summary, PrintWriter changes, PrintWriter changesNew,
        PrintWriter changesDeletes, PrintWriter changesSummary, Relation<String, String> oldPath2value,
        Relation<String, String> path2value2) {
        Set<String> union = Builder.with(new TreeSet<String>()).addAll(oldPath2value.keySet())
            .addAll(path2value2.keySet()).get();
        long total = 0;
        Matcher prefixMatcher = prefix.matcher("");
        Delta charCount = new Delta();
        Delta itemCount = new Delta();
        Set<String> prefixes = new TreeSet();
        for (String path : union) {
            if (!prefixMatcher.reset(path).find()) {
                throw new IllegalArgumentException();
            }
            String prefix = prefixMatcher.group(1);
            prefixes.add(prefix);
            String localPath = prefixMatcher.group(2);
            Set<String> set1 = oldPath2value.getAll(path);
            Set<String> set2 = path2value2.getAll(path);
            if (set2 != null) {
                total += set2.size();
            }
            if (set1 == null) {
                changesNew.println(prefix + "\t" + "\t" + set2 + "\t" + localPath);
                itemCount.newCount.add(prefix, set2.size());
                charCount.newCount.add(prefix, totalLength(set2));
            } else if (set2 == null) {
                changesDeletes.println(prefix + "\t" + set1 + "\t\t" + localPath);
                itemCount.deletedCount.add(prefix, -set1.size());
                charCount.deletedCount.add(prefix, -totalLength(set1));
            } else if (!set1.equals(set2)) {
                TreeSet<String> set1minus2 = Builder.with(new TreeSet<String>()).addAll(set1).removeAll(set2).get();
                TreeSet<String> set2minus1 = Builder.with(new TreeSet<String>()).addAll(set2).removeAll(set1).get();
                TreeSet<String> set2and1 = Builder.with(new TreeSet<String>()).addAll(set2).retainAll(set1).get();
                itemCount.changedCount.add(prefix, (set2minus1.size() + set1minus2.size() + 1) / 2);
                itemCount.unchangedCount.add(prefix, set2and1.size());
                charCount.changedCount.add(prefix, (totalLength(set2minus1) + totalLength(set1minus2) + 1) / 2);
                charCount.unchangedCount.add(prefix, totalLength(set2and1));
                changes.println(prefix + "\t" + set1minus2
                    + "\t"
                    + set2minus1
                    + "\t" + localPath);
            } else {
                itemCount.unchangedCount.add(prefix, set2.size());
                charCount.unchangedCount.add(prefix, totalLength(set2));
            }
        }
        itemCount.print(changesSummary, prefixes);
        changesSummary.println();
        charCount.print(changesSummary, prefixes);
//        union = Builder.with(new TreeSet<String>())
//            .addAll(newCount.keySet())
//            .addAll(deletedCount.keySet())
//            .addAll(changedCount.keySet())
//            .addAll(unchangedCount.keySet())
//            .get();
        summary.println("#Total:\t" + total);
    }

    private static long totalLength(Set<String> set2) {
        int result = 0;
        for (String s : set2) {
            result += s.length();
        }
        return result;
    }

    final static Pattern LOCALE_PATTERN = PatternCache.get(
        "([a-z]{2,3})(?:[_-]([A-Z][a-z]{3}))?(?:[_-]([a-zA-Z0-9]{2,3}))?([_-][a-zA-Z0-9]{1,8})*");

    public static void doSummary() throws IOException {
        Map<String, R4<Counter<String>, Counter<String>, Counter<String>, Counter<String>>> key_release_count = new TreeMap<String, R4<Counter<String>, Counter<String>, Counter<String>, Counter<String>>>();
        Matcher countryLocale = LOCALE_PATTERN.matcher("");
        List<String> releases = new ArrayList<String>();
        Pattern releaseNumber = PatternCache.get("count_(?:.*-(\\d+(\\.\\d+)*)|trunk)\\.txt");
        // int releaseCount = 1;
        Relation<String, String> release_keys = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Relation<String, String> localesToPaths = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Set<String> writtenLanguages = new TreeSet<String>();
        Set<String> countries = new TreeSet<String>();

        File[] listFiles = new File(OUT_DIRECTORY).listFiles();
        // find the most recent version
        VersionInfo mostRecentVersion = VersionInfo.getInstance(0);
        for (File subdir : listFiles) {
            final String name = subdir.getName();
            final Matcher releaseMatcher = releaseNumber.matcher(name);
            if (!releaseMatcher.matches()) {
                if (name.startsWith("count_")) {
                    throw new IllegalArgumentException("Bad match " + RegexUtilities.showMismatch(releaseMatcher, name));
                }
                continue;
            }
            String releaseNum = releaseMatcher.group(1); // "1." + releaseCount++;
            if (releaseNum == null) {
                releaseNum = TRUNK_VERSION;
            }
            VersionInfo vi = VersionInfo.getInstance(releaseNum);
            if (vi.compareTo(mostRecentVersion) > 0) {
                mostRecentVersion = vi;
            }
        }

        for (File subdir : listFiles) {
            final String name = subdir.getName();
            final Matcher releaseMatcher = releaseNumber.matcher(name);
            if (!releaseMatcher.matches()) {
                if (name.startsWith("count_")) {
                    throw new IllegalArgumentException("Bad match " + RegexUtilities.showMismatch(releaseMatcher, name));
                }
                continue;
            }
            String releaseNum = releaseMatcher.group(1); // "1." + releaseCount++;
            if (releaseNum == null) {
                releaseNum = TRUNK_VERSION;
            }
            VersionInfo vi = VersionInfo.getInstance(releaseNum);
            boolean captureData = vi.equals(mostRecentVersion);
            releases.add(releaseNum);
            BufferedReader in = FileUtilities.openUTF8Reader("", subdir.getCanonicalPath());
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                // common/main  New:        [Yellowknife]   /gl//ldml/dates/timeZoneNames/zone[@type="America/Yellowknife"]/exemplarCity

                String[] parts = line.split("\t");
                try {
                    String file = parts[0];
                    if (file.startsWith("seed/") || !DIR_FILE_MATCHER.reset(file).find()) {
                        if (VERBOSE) {
                            System.out.println("Skipping: " + RegexUtilities.showMismatch(DIR_FILE_MATCHER, file));
                        }
                        continue;
                    } else if (VERBOSE) {
                        System.out.println("Including: " + file);
                    }

                    long valueCount = Long.parseLong(parts[1]);
                    long valueLen = Long.parseLong(parts[2]);
                    long attrCount = Long.parseLong(parts[3]);
                    long attrLen = Long.parseLong(parts[4]);
                    int lastSlash = file.lastIndexOf("/");
                    String key2 = file;
                    String path = file.substring(0, lastSlash);
                    String key = file.substring(lastSlash + 1);
                    if (countryLocale.reset(key).matches()) {
                        String lang = countryLocale.group(1);
                        String script = countryLocale.group(2);
                        String country = countryLocale.group(3);
                        String writtenLang = lang + (script == null ? "" : "_" + script);
                        String locale = writtenLang + (country == null ? "" : "_" + country);
                        if (captureData) {
                            localesToPaths.put(locale, path);
                            writtenLanguages.add(writtenLang);
                            if (country != null) {
                                countries.add(country);
                            }
                        }
                        // System.out.println(key + " => " + newKey);
                        //key = writtenLang + "—" + ULocale.getDisplayName(writtenLang, "en");
                    }
                    if (valueCount + attrCount == 0) continue;
                    release_keys.put(releaseNum, key2);
                    R4<Counter<String>, Counter<String>, Counter<String>, Counter<String>> release_count = key_release_count
                        .get(key2);
                    if (release_count == null) {
                        release_count = Row.of(new Counter<String>(), new Counter<String>(), new Counter<String>(),
                            new Counter<String>());
                        key_release_count.put(key2, release_count);
                    }
                    release_count.get0().add(releaseNum, valueCount);
                    release_count.get1().add(releaseNum, valueLen);
                    release_count.get2().add(releaseNum, attrCount);
                    release_count.get3().add(releaseNum, attrLen);
                } catch (Exception e) {
                    throw new IllegalArgumentException(line, e);
                }
            }
            in.close();
        }
        PrintWriter summary = FileUtilities.openUTF8Writer(OUT_DIRECTORY, (MyOptions.directory.option.doesOccur() ? "filtered-" : "") + "summary" +
            ".txt");
        for (String file : releases) {
            summary.print("\t" + file + "\tlen");
        }
        summary.println();
        for (String key : key_release_count.keySet()) {
            summary.print(key);
            R4<Counter<String>, Counter<String>, Counter<String>, Counter<String>> release_count = key_release_count
                .get(key);
            for (String release2 : releases) {
                long count = release_count.get0().get(release2) + release_count.get2().get(release2);
                long len = release_count.get1().get(release2) + release_count.get3().get(release2);
                summary.print("\t" + count + "\t" + len);
            }
            summary.println();
        }
        for (String release : release_keys.keySet()) {
            System.out.println("Release:\t" + release + "\t" + release_keys.getAll(release).size());
        }
        summary.close();
        PrintWriter summary2 = FileUtilities.openUTF8Writer(OUT_DIRECTORY, (MyOptions.directory.option.doesOccur() ? "filtered-" : "") + "locales" +
            ".txt");
        summary2.println("#Languages (inc. script):\t" + writtenLanguages.size());
        summary2.println("#Countries:\t" + countries.size());
        summary2.println("#Locales:\t" + localesToPaths.size());
        for (Entry<String, Set<String>> entry : localesToPaths.keyValuesSet()) {
            summary2.println(entry.getKey() + "\t" + CollectionUtilities.join(entry.getValue(), "\t"));
        }
        summary2.close();
    }

    static final Set<String> ATTRIBUTES_TO_SKIP = Builder.with(new HashSet<String>())
        .addAll("version", "references", "standard", "draft").freeze();
    static final Pattern skipPath = PatternCache.get("" +
        "\\[\\@alt=\"[^\"]*proposed" +
        "|^//" +
        "(ldml(\\[[^/]*)?/identity" +
        "|(ldmlBCP47|supplementalData|keyboard)(\\[[^/]*)?/(generation|version)" +
        ")");

    static void capture(DtdType type2, XPathParts parts) {
        for (int i = 0; i < parts.size(); ++i) {
            String element = parts.getElement(i);
            ELEMENTS_OCCURRING.put(element, type2);
            for (String attribute : parts.getAttributes(i).keySet()) {
                ATTRIBUTES_OCCURRING.put(attribute, Row.of(type2, element));
            }
        }
    }

    static class MyHandler extends SimpleHandler {
        long valueCount;
        long valueLen;
        long attributeCount;
        long attributeLen;
        Matcher skipPathMatcher = skipPath.matcher("");
        Splitter lines = Splitter.onPattern("\n+").omitEmptyStrings().trimResults();
        String prefix;
        int orderedCount;
        DtdType type;
        private final boolean isFinal;

        MyHandler(String prefix, boolean isFinal) {
            this.prefix = prefix;
            this.isFinal = isFinal;
        }

        @Override
        public void handlePathValue(String path, String value) {
            if (type == null) {
                XPathParts parts = XPathParts.getTestInstance(path);
                type = DtdType.valueOf(parts.getElement(0));
            }

            ATTRIBUTE_TYPES.add(path);

            if (skipPathMatcher.reset(path).find()) {
                return;
            }
            String pathKey = null;
            if (doChanges) {
                // if (path.contains("/collations")) {
                // System.out.println("whoops");
                // }
                pathKey = fixKeyPath(path);
            }
            int len = value.length();
            value = value.trim();
            if (value.isEmpty() && len > 0) {
                value = " ";
            }
            if (value.length() != 0) {
                List<String> valueLines = lines.splitToList(value);
                if (valueLines.size() == 1) {
                    valueCount++;
                    valueLen += value.length();
                    if (doChanges) {
                        path2value.put(pathKey, value);
                    }
                } else {
                    int count = 0;
                    for (String v : valueLines) {
                        valueCount++;
                        valueLen += v.length();
                        if (doChanges) {
                            path2value.put(pathKey + "/_q" + count++, v);
                        }
                    }
                }
            }
            XPathParts parts = XPathParts.getTestInstance(path);
            if (isFinal) {
                capture(type, parts);
            }
            if (path.contains("[@")) {
                int i = parts.size() - 1; // only look at last item
                Collection<String> attributes = parts.getAttributeKeys(i);
                if (attributes.size() != 0) {
                    String element = parts.getElement(i);
                    for (String attribute : attributes) {
                        if (ATTRIBUTES_TO_SKIP.contains(attribute)
                            || CLDRFile.isDistinguishing(type, element, attribute)) {
                            continue;
                        }
                        String valuePart = parts.getAttributeValue(i, attribute);
                        // String[] valueParts = attrValue.split("\\s");
                        // for (String valuePart : valueParts) {
                        attributeCount++;
                        attributeLen += valuePart.length();
                        if (doChanges) {
                            path2value.put(pathKey + "/_" + attribute, valuePart);
                            // }
                        }
                    }
                }
            }
        }

        private String fixKeyPath(String path) {
            XPathParts parts = XPathParts.getTestInstance(path);
            for (int i = 0; i < parts.size(); ++i) {
                String element = parts.getElement(i);
                if (!SKIP_ORDERING) {
                    if (CLDRFile.isOrdered(element, type)) {
                        parts.addAttribute("_q", String.valueOf(orderedCount++));
                    }
                }
            }
            return prefix + CLDRFile.getDistinguishingXPath(parts.toString(), null, false);
        }
    }

    private MyHandler check(String systemID, String name, boolean isFinal) {
        MyHandler myHandler = new MyHandler(name, isFinal);
        try {
            XMLFileReader reader = new XMLFileReader().setHandler(myHandler);
            reader.read(systemID, XMLFileReader.CONTENT_HANDLER, true);
        } catch (Exception e) {
            cantRead.put(name, Arrays.asList(e.getStackTrace()));
        }
        return myHandler;

        // try {
        // FileInputStream fis = new FileInputStream(systemID);
        // XMLFileReader xmlReader = XMLFileReader.createXMLReader(true);
        // xmlReader.setErrorHandler(new MyErrorHandler());
        // MyHandler myHandler = new MyHandler();
        // smlReader
        // xmlReader.setHandler(myHandler);
        // InputSource is = new InputSource(fis);
        // is.setSystemId(systemID.toString());
        // xmlReader.parse(is);
        // fis.close();
        // return myHandler;
        // } catch (SAXParseException e) {
        // System.out.println("\t" + "Can't read " + systemID);
        // System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        // } catch (SAXException e) {
        // System.out.println("\t" + "Can't read " + systemID);
        // System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        // } catch (IOException e) {
        // System.out.println("\t" + "Can't read " + systemID);
        // System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
        // }
    }

    static class MyErrorHandler implements ErrorHandler {
        public void error(SAXParseException exception) throws SAXException {
            System.out.println("\nerror: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            System.out.println("\nfatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            System.out.println("\nwarning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    private void summarizeCoverage(PrintWriter summary, String commonDir, boolean isFinal) {
        System.out.println(commonDir);
        summary.println("#name" + "\t" + "value-count" + "\t" + "value-len" + "\t" + "attr-count" + "\t" + "attr-len");
        File commonDirectory = new File(commonDir);
        if (!commonDirectory.exists()) {
            System.out.println("Doesn't exist:\t" + commonDirectory);
        }
        summarizeFiles(summary, commonDirectory, isFinal, 1);
    }

    static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList("specs", "tools", "seed", "exemplars"));

    public void summarizeFiles(PrintWriter summary, File directory, boolean isFinal, int level) {
        System.out.println("\t\t\t\t\t\t\t".substring(0, level) + directory);
        int count = 0;
        for (File file : directory.listFiles()) {
            String filename = file.getName();
            if (filename.startsWith(".")) {
                // do nothing
            } else if (file.isDirectory()) {
                if (!SKIP_DIRS.contains(filename)) {
                    summarizeFiles(summary, file, isFinal, level + 1);
                }
            } else if (!filename.startsWith("#") && filename.endsWith(".xml")) {
                String name = new File(directory.getParent()).getName() + "/" + directory.getName() + "/"
                    + file.getName();
                name = name.substring(0, name.length() - 4); // strip .xml
                if (!RAW_FILE_MATCHER.reset(name).find()) {
                    continue;
                }
                if (VERBOSE) {
                    System.out.println(name);
                } else {
                    System.out.print(".");
                    if (++count > 100) {
                        count = 0;
                        System.out.println();
                    }
                    System.out.flush();
                }
                MyHandler handler = check(file.toString(), name, isFinal);
                summary.println(name + "\t" + handler.valueCount + "\t" + handler.valueLen + "\t"
                    + handler.attributeCount + "\t" + handler.attributeLen);
            }
        }
        System.out.println();
    }
}
