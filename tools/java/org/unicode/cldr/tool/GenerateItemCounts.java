package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DtdType;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLFileReader.SimpleHandler;
import org.unicode.cldr.util.XPathParts;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

public class GenerateItemCounts {
    private static final boolean SKIP_ORDERING = true;
    private static final String OUT_DIRECTORY = CldrUtility.GEN_DIRECTORY + "/itemcount/"; // CldrUtility.MAIN_DIRECTORY;
    private Map<String, List<StackTraceElement>> cantRead = new TreeMap<String, List<StackTraceElement>>();

    private static String[] DIRECTORIES = {
        // MUST be oldest first!
        // "cldr-archive/cldr-21.0",
        "cldr-archive/cldr-22.1",
        "cldr-archive/cldr-23.0",
    };

    static boolean doChanges = true;
    static Relation<String, String> path2value = new Relation(new TreeMap<String, String>(), TreeSet.class);
    static final AttributeTypes ATTRIBUTE_TYPES = new AttributeTypes();

    final static Options myOptions = new Options();

    enum MyOptions {
        summary(null, null, "if present, summarizes data already collected. Run once with, once without."),
        directory(".*", ".*",
            "if summary, creates filtered version (eg -d main): does a find in the name, which is of the form dir/file"),
        verbose(null, null, "verbose debugging messages"),
        rawfilter(".*", ".*", "filter the raw files (non-summary, mostly for debugging)"), ;
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

        DIR_FILE_MATCHER = Pattern.compile(MyOptions.directory.option.getValue()).matcher("");
        RAW_FILE_MATCHER = Pattern.compile(MyOptions.rawfilter.option.getValue()).matcher("");
        VERBOSE = MyOptions.verbose.option.doesOccur();

        if (MyOptions.summary.option.doesOccur()) {
            doSummary();
            System.out.println("DONE");
            return;
            // } else if (arg.equals("changes")) {
            // doChanges = true;
        } else {
        }
        // Pattern dirPattern = dirPattern = Pattern.compile(arg);
        GenerateItemCounts main = new GenerateItemCounts();
        try {
            Relation<String, String> oldPath2value = null;
            for (String dir : DIRECTORIES) {
                // if (dirPattern != null && !dirPattern.matcher(dir).find()) continue;
                String fulldir = new File(CldrUtility.ARCHIVE_DIRECTORY + "/" + dir).getCanonicalPath();
                String prefix = (MyOptions.rawfilter.option.doesOccur() ? "filtered_" : "");
                String fileKey = dir.replace("/", "_");
                PrintWriter summary = BagFormatter.openUTF8Writer(OUT_DIRECTORY, prefix + "count_" + fileKey + ".txt");
                PrintWriter changes = BagFormatter
                    .openUTF8Writer(OUT_DIRECTORY, prefix + "changes_" + fileKey + ".txt");
                main.summarizeCoverage(summary, fulldir);
                if (doChanges) {
                    if (oldPath2value != null) {
                        compare(summary, changes, oldPath2value, path2value);
                    }
                    oldPath2value = path2value;
                    path2value = new Relation(new TreeMap<String, String>(), TreeSet.class);
                }
                summary.close();
                changes.close();
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

    static class AttributeTypes {
        Relation<String, String> elementPathToAttributes = Relation.of(new TreeMap<String, Set<String>>(),
            TreeSet.class);
        final PathStarrer PATH_STARRER = new PathStarrer().setSubstitutionPattern("*");
        final Set<String> STARRED_PATHS = new TreeSet<String>();
        XPathParts parts = new XPathParts();
        StringBuilder elementPath = new StringBuilder();

        public void add(String path) {
            parts.set(path);
            elementPath.setLength(0);
            DtdType type = CLDRFile.DtdType.valueOf(parts.getElement(0));
            for (int i = 0; i < parts.size(); ++i) {
                String element = parts.getElement(i);
                elementPath.append('/').append(element);
                elementPathToAttributes.putAll(elementPath.toString().intern(), parts.getAttributeKeys(i));
            }
        }

        public void showStarred() throws IOException {
            PrintWriter starred = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "starred" + ".txt");

            for (Entry<String, Set<String>> entry : elementPathToAttributes.keyValuesSet()) {
                Set<String> attributes = entry.getValue();
                if (attributes.size() == 0) {
                    continue;
                }
                String path = entry.getKey();
                String[] elements = path.split("/");
                DtdType type = CLDRFile.DtdType.valueOf(elements[1]);
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

    static Pattern prefix = Pattern.compile("([^/]+/[^/]+)(.*)");

    private static void compare(PrintWriter summary, PrintWriter changes2, Relation<String, String> oldPath2value,
        Relation<String, String> path2value2) {
        Set<String> union = Builder.with(new TreeSet<String>()).addAll(oldPath2value.keySet())
            .addAll(path2value2.keySet()).get();
        long changes = 0;
        long total = 0;
        Matcher prefixMatcher = prefix.matcher("");
        Counter<String> newCount = new Counter<String>();
        Counter<String> deletedCount = new Counter<String>();
        for (String path : union) {
            if (!prefixMatcher.reset(path).find()) {
                throw new IllegalArgumentException();
            }
            String prefix = prefixMatcher.group(1);
            String localPath = prefixMatcher.group(2);
            Set<String> set1 = oldPath2value.getAll(path);
            Set<String> set2 = path2value2.getAll(path);
            if (set2 != null) {
                total += set2.size();
            }
            if (set1 == null) {
                changes2.println(prefix + "\tNew:\t" + "\t" + set2 + "\t" + localPath);
                changes += set2.size();
                newCount.add(prefix, set2.size());
            } else if (set2 == null) {
                changes2.println(prefix + "\tDeleted:\t" + set1 + "\t\t" + localPath);
                changes += set1.size();
                deletedCount.add(prefix, set1.size());
            } else if (!set1.equals(set2)) {
                TreeSet<String> set1minus2 = Builder.with(new TreeSet<String>()).addAll(set1).removeAll(set2).get();
                TreeSet<String> set2minus1 = Builder.with(new TreeSet<String>()).addAll(set2).removeAll(set1).get();
                int diffCount = set1minus2.size() + set2minus1.size();
                changes += diffCount;
                newCount.add(prefix, set2minus1.size());
                deletedCount.add(prefix, set1minus2.size());
                changes2.println(prefix + "\tChanged:\t" + set1minus2
                    + "\t"
                    + set2minus1
                    + "\t" + localPath);
            }
        }
        union = Builder.with(new TreeSet<String>()).addAll(newCount.keySet()).addAll(deletedCount.keySet()).get();
        for (String prefix : union) {
            changes2.println(prefix + "\tRemoved:\t" + deletedCount.get(prefix) + "\tAdded:\t" + newCount.get(prefix));
        }
        changes2.println("#Total" + "\tRemoved:\t" + deletedCount.getTotal() + "\tAdded:\t" + newCount.getTotal());
        summary.println("#Total:\t" + total);
    }

    final static Pattern LOCALE_PATTERN = Pattern.compile(
        "([a-z]{2,3})(?:[_-]([A-Z][a-z]{3}))?(?:[_-]([a-zA-Z0-9]{2,3}))?([_-][a-zA-Z0-9]{1,8})*");

    public static void doSummary() throws IOException {
        Map<String, R4<Counter<String>, Counter<String>, Counter<String>, Counter<String>>> key_release_count = new TreeMap();
        Matcher countryLocale = LOCALE_PATTERN.matcher("");
        List<String> releases = new ArrayList<String>();
        Pattern releaseNumber = Pattern.compile("count_.*-(\\d+(\\.\\d+)*)\\.txt");
        // int releaseCount = 1;
        Relation<String, String> release_keys = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Relation<String, String> localesToPaths = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Set<String> writtenLanguages = new TreeSet();
        Set<String> countries = new TreeSet();

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
            VersionInfo vi = VersionInfo.getInstance(releaseNum);
            boolean captureData = vi.equals(mostRecentVersion);
            releases.add(releaseNum);
            BufferedReader in = BagFormatter.openUTF8Reader("", subdir.getCanonicalPath());
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
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
                        key = writtenLang + "—" + ULocale.getDisplayName(writtenLang, "en");
                    }
                    if (valueCount + attrCount == 0) continue;
                    release_keys.put(releaseNum, key);
                    R4<Counter<String>, Counter<String>, Counter<String>, Counter<String>> release_count = key_release_count
                        .get(key);
                    if (release_count == null) {
                        release_count = Row.of(new Counter<String>(), new Counter<String>(), new Counter<String>(),
                            new Counter<String>());
                        key_release_count.put(key, release_count);
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
        PrintWriter summary = BagFormatter.openUTF8Writer(OUT_DIRECTORY,
            (MyOptions.directory.option.doesOccur() ? "filtered-" : "") + "summary" +
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
        PrintWriter summary2 = BagFormatter.openUTF8Writer(OUT_DIRECTORY,
            (MyOptions.directory.option.doesOccur() ? "filtered-" : "") + "locales" +
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
    static final Pattern skipPath = Pattern.compile("" +
        "\\[\\@alt=\"[^\"]*proposed" +
        "|^//" +
        "(ldml(\\[[^/]*)?/identity" +
        "|(ldmlBCP47|supplementalData)(\\[[^/]*)?/(generation|version)" +
        ")"
        );

    static class MyHandler extends SimpleHandler {
        XPathParts parts = new XPathParts();
        long valueCount;
        long valueLen;
        long attributeCount;
        long attributeLen;
        Matcher skipPathMatcher = skipPath.matcher("");
        String prefix;
        int orderedCount;
        DtdType type;

        MyHandler(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void handlePathValue(String path, String value) {
            if (type == null) {
                parts.set(path);
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
            if (value.length() != 0) {
                valueCount++;
                valueLen += value.length();
                if (doChanges) {
                    path2value.put(pathKey, value);
                }
            }
            if (path.contains("[@")) {
                parts.set(path);
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
                            path2value.put(pathKey, valuePart);
                            // }
                        }
                    }
                }
            }
        }

        private String fixKeyPath(String path) {
            parts.set(path);
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

    private MyHandler check(String systemID, String name) {
        MyHandler myHandler = new MyHandler(name);
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
            System.out.println("\r\nerror: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            System.out.println("\r\nfatalError: " + XMLFileReader.showSAX(exception));
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            System.out.println("\r\nwarning: " + XMLFileReader.showSAX(exception));
            throw exception;
        }
    }

    private void summarizeCoverage(PrintWriter summary, String commonDir) {
        System.out.println(commonDir);
        summary.println("#name" + "\t" + "value-count" + "\t" + "value-len" + "\t" + "attr-count" + "\t" + "attr-len");
        File commonDirectory = new File(commonDir);
        if (!commonDirectory.exists()) {
            System.out.println("Doesn't exist:\t" + commonDirectory);
        }
        summarizeFiles(summary, commonDirectory, 1);
    }

    public void summarizeFiles(PrintWriter summary, File directory, int level) {
        System.out.println("\t\t\t\t\t\t\t".substring(0, level) + directory);
        int count = 0;
        for (File file : directory.listFiles()) {
            String filename = file.getName();
            if (file.isDirectory()) {
                summarizeFiles(summary, file, level + 1);
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
                MyHandler handler = check(file.toString(), name);
                summary.println(name + "\t" + handler.valueCount + "\t" + handler.valueLen + "\t"
                    + handler.attributeCount + "\t" + handler.attributeLen);
            }
        }
        System.out.println();
    }
}
