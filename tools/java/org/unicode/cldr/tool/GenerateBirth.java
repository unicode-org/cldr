package org.unicode.cldr.tool;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StringId;

import com.google.common.base.Objects;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.util.ICUException;

public class GenerateBirth {
    private static boolean DEBUG = false;

    private static final List<CldrVersion> VERSIONS_WITH_TRUNK_DESCENDING = CldrVersion.CLDR_VERSIONS_DESCENDING;

    static final CldrVersion[] VERSIONS = VERSIONS_WITH_TRUNK_DESCENDING.toArray(
        new CldrVersion[VERSIONS_WITH_TRUNK_DESCENDING.size()]); // hack for now; should change to list
    
    static final Factory[] factories = new Factory[VERSIONS.length-1]; // hack for now; should change to list

    final static Options myOptions = new Options()
        .add("target", ".*", CLDRPaths.BIRTH_DATA_DIR,
            "The target directory for building the text files that show the results.")
        .add("log", ".*", CLDRPaths.TMP_DIRECTORY + "dropbox/births/",
            "The target directory for building the text files that show the results.")
        .add(
            "file",
            ".*",
            ".*",
            "Filter the information based on file name, using a regex argument. The '.xml' is removed from the file before filtering")
        .add("previous", "Stop after writing the English previous data.")
        .add("debug", "Debug");

    public static void main(String[] args) throws IOException {
        myOptions.parse(args, true);
        try {
            CldrVersion.checkVersions(); // verify versions up to date
        } catch (Exception e) {
            throw new ICUException("This tool can only be run if the archive of released versions matching CldrVersion is available.", e);
        }
        
        // set up the CLDR Factories

        DEBUG = myOptions.get("debug").doesOccur();

        final CLDRConfig config = CLDRConfig.getInstance();

        String filePattern = myOptions.get("file").getValue();

        ArrayList<Factory> list = new ArrayList<Factory>();
        for (CldrVersion version : VERSIONS) {
            if (version == CldrVersion.unknown) {
                continue;
            }
            List<File> paths = version.getPathsForFactory();
//            String base = version.getBaseDirectory();
//            File[] paths = version.compareTo(CldrVersion.v27_0) > 0 ? // warning, order is reversed
//                new File[] { new File(base + "common/main/") } : 
//                    new File[] { new File(base + "common/main/"), new File(base + "common/annotations/") };

                System.out.println(version + ", " + paths);
                Factory aFactory = SimpleFactory.make(paths.toArray(new File[paths.size()]), filePattern);
                list.add(aFactory);
        }
        list.toArray(factories);

        final String dataDirectory = myOptions.get("target").getValue();

        // load and process English

        String logDirectory = myOptions.get("log").getValue();

        System.out.println("en");
        Births english = new Births("en");
        english.writeBirth(logDirectory, "en", null);
        english.writeBirthValues(dataDirectory + "/" + OutdatedPaths.OUTDATED_ENGLISH_DATA);
        
        Map<Long, Pair<CldrVersion, String>> pathToPrevious = new HashMap<>();
        
        // Verify that the write of English worked
        
        OutdatedPaths.readBirthValues(dataDirectory, null, pathToPrevious);
        for (Entry<String, R3<CldrVersion, String, String>> entry : english.pathToBirthCurrentPrevious.entrySet()) {
            String path = entry.getKey();
            String previous = entry.getValue().get2();
            CldrVersion birth = entry.getValue().get0();
            if (previous == null) {
                previous = OutdatedPaths.NO_VALUE;
            }
            long id = StringId.getId(path);
            Pair<CldrVersion, String> readValue = pathToPrevious.get(id);
            CldrVersion birthRead = readValue == null ? null : readValue.getFirst();
            String previousRead = readValue == null ? null : readValue.getSecond();
            if (!Objects.equal(previous, previousRead) || !Objects.equal(birth, birthRead)) {
                throw new IllegalArgumentException("path: " + path 
                    + "\tprevious: " + previous + "\tread: " + readValue
                    + "\tbirth: " + birth + "\tread: " + birthRead
                    );
            }
        }

        // Set up the binary data files for all others

        File file = new File(dataDirectory + "/" + OutdatedPaths.OUTDATED_DATA);
        final String outputDataFile = file.getCanonicalPath();
        System.out.println("Writing data: " + outputDataFile);
        DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(file));
        dataOut.writeUTF(OutdatedPaths.FORMAT_KEY);

        // Load and process all the locales

        TreeMap<String, Set<String>> localeToNewer = new TreeMap<String, Set<String>>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String fileName : factories[0].getAvailable()) {
            if (fileName.equals("en")) {
                continue;
            }
            if (!ltp.set(fileName).getRegion().isEmpty()) {
                continue; // skip region locales
            }
            // TODO skip default content locales
            System.out.println(fileName);
            Births other = new Births(fileName);
            Set<String> newer = other.writeBirth(logDirectory, fileName, english);

            dataOut.writeUTF(fileName);
            dataOut.writeInt(newer.size());
            for (String item : newer) {
                long id = StringId.getId(item);
                dataOut.writeLong(id);
                if (DEBUG) {
                    System.out.println(id + "\t" + item);
                }
            }
            localeToNewer.put(fileName, newer);
        }
        dataOut.writeUTF("$END$");
        dataOut.close();

        // Doublecheck the data

        OutdatedPaths outdatedPaths = new OutdatedPaths(dataDirectory);
        Set<String> needPrevious = new TreeSet<String>();
        int errorCount = 0;
        for (Entry<String, Set<String>> localeAndNewer : localeToNewer.entrySet()) {
            String locale = localeAndNewer.getKey();
            System.out.println("Checking " + locale);
            Set<String> newer = localeAndNewer.getValue();
            if (newer.size() != outdatedPaths.countOutdated(locale)) {
                throw new IllegalArgumentException("broken: " + locale);
            }
            for (String xpath : newer) {
                boolean isOutdated = outdatedPaths.isRawOutdated(locale, xpath);
                if (!isOutdated) {
                    System.out.println("Error, broken locale: " + locale + "\t" + StringId.getId(xpath) + "\t" + xpath);
                    ++errorCount;
                }
                if (outdatedPaths.isSkipped(xpath)) {
                    continue;
                }
                String previous = outdatedPaths.getPreviousEnglish(xpath);
                if (previous.isEmpty() != english.emptyPrevious.contains(xpath)) {
                    System.out.println("previous.isEmpty() != original " + locale + "\t" + StringId.getId(xpath) + "\t"
                        + xpath);
                    needPrevious.add(xpath);
                    ++errorCount;
                }
            }
        }
        if (errorCount != 0) {
            throw new IllegalArgumentException("Done, but " + errorCount + " errors");
        } else {
            System.out.println("Done, no errors");
        }
    }

    static class Births {
        final Relation<CldrVersion, String> birthToPaths;
        final Map<String, Row.R3<CldrVersion, String, String>> pathToBirthCurrentPrevious;
        final String locale;
        static final Pattern TYPE = PatternCache.get("\\[@type=\"([^\"]*)\"");
        final Matcher typeMatcher = TYPE.matcher("");
        Set<String> emptyPrevious = new HashSet<String>();

        Births(String file) {
            locale = file;
            CLDRFile[] files = new CLDRFile[factories.length];
            for (int i = 0; i < factories.length; ++i) {
                try {
                    files[i] = factories[i].make(file, false);
                } catch (Exception e) {
                    // stop when we fail to find
                    System.out.println("Stopped at " + file + ", " + CldrVersion.CLDR_VERSIONS_DESCENDING.get(i));
                    //e.printStackTrace();
                    break;
                }
            }
            birthToPaths = Relation.of(new TreeMap<CldrVersion, Set<String>>(), TreeSet.class);
            pathToBirthCurrentPrevious = new HashMap<String, Row.R3<CldrVersion, String, String>>();
            for (String xpath : files[0]) {
                xpath = xpath.intern();
                if (xpath.contains("[@type=\"ar\"]")) {
                    int debug = 0;
                }
                String base = files[0].getStringValue(xpath);
                String previousValue = null;
                int i;
                CLDRFile lastFile = files[0];
                for (i = 1; i < files.length && files[i] != null; ++i) {
                    String previous = files[i].getStringValue(xpath);
                    if (previous == null) {
                        previous = OutdatedPaths.NO_VALUE; // fixNullPrevious(xpath);
                    }
                    if (!CharSequences.equals(base, previous)) {
                        if (previous != null) {
                            previousValue = previous;
                        }
                        break;
                    }
                    lastFile = files[i];
                }
                CldrVersion version = CldrVersion.from(lastFile.getDtdVersionInfo());
                birthToPaths.put(version, xpath);
                pathToBirthCurrentPrevious.put(xpath, Row.of(version, base, previousValue));
            }
        }

        private String fixNullPrevious(String xpath) {
            if (typeMatcher.reset(xpath).find()) {
                String type = typeMatcher.group(1);
                if (xpath.contains("metazone")) {
                    return type.replace("_", " ");
                } else if (xpath.contains("zone")) {
                    String[] splits = type.split("/");
                    return splits[splits.length - 1].replace("_", " ");
                }
                return type;
            }
            return null;
        }

        public void writeBirthValues(String file) throws IOException {
            DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(file));
            dataOut.writeUTF(OutdatedPaths.FORMAT_KEY);
            System.out.println("Writing data: " + new File(file).getCanonicalPath());
            dataOut.writeInt(pathToBirthCurrentPrevious.size());

            // Load and process all the locales

            //TreeMap<String, Set<String>> localeToNewer = new TreeMap<String, Set<String>>();
            for (Entry<String, R3<CldrVersion, String, String>> entry : pathToBirthCurrentPrevious.entrySet()) {
                String path = entry.getKey();
                R3<CldrVersion, String, String> birthCurrentPrevious = entry.getValue();
                CldrVersion birth = birthCurrentPrevious.get0();
                String current = birthCurrentPrevious.get1();
                String previous = birthCurrentPrevious.get2();
                long id = StringId.getId(path);
                dataOut.writeLong(id);
                final String previousString = previous == null ? OutdatedPaths.NO_VALUE : previous;
                dataOut.writeUTF(previousString);
                if (previous == null) {
                    emptyPrevious.add(path);
                }
                dataOut.writeUTF(birth.toString());
                if (true) {
                    System.out.println(id + "\t" + birth + "\t«" + current + "⇐" + previous + "»");
                }
            }
            dataOut.writeUTF("$END$");
            dataOut.close();
            emptyPrevious = Collections.unmodifiableSet(emptyPrevious);
        }

        Set<String> writeBirth(PrintWriter out, Births onlyNewer) {
            
            out.println("Loc\tVersion\tValue\tPrevValue\tEVersion\tEValue\tEPrevValue\tPath");

            Set<String> newer = new HashSet<String>();
            HashMap<Long, String> sanityCheck = new HashMap<Long, String>();
            CldrVersion onlyNewerVersion = null;
            String otherValue = "n/a";
            String olderOtherValue = "n/a";
            for (Entry<CldrVersion, Set<String>> entry2 : birthToPaths.keyValuesSet()) {
                CldrVersion version = entry2.getKey();
                for (String xpath : entry2.getValue()) {
                    long id = StringId.getId(xpath);
                    String old = sanityCheck.get(id);
                    if (old != null) {
                        throw new IllegalArgumentException("Path Collision " + xpath + ", old:" + old + ", id: " + id);
                    } else {
                        sanityCheck.put(id, xpath);
                    }
                    R3<CldrVersion, String, String> info = pathToBirthCurrentPrevious.get(xpath);
                    if (onlyNewer != null) {

                        R3<CldrVersion, String, String> otherInfo = onlyNewer.pathToBirthCurrentPrevious.get(xpath);
                        if (otherInfo == null) {
                            continue;
                        }
                        // skip if not older than "comparison version"
                        onlyNewerVersion = otherInfo.get0();
                        if (!version.isOlderThan(onlyNewerVersion)) {
                            continue;
                        }
                        otherValue = fixNull(otherInfo.get1());
                        olderOtherValue = fixNull(otherInfo.get2());
                        newer.add(xpath);
                    }
                    String value = fixNull(info.get1());
                    String olderValue = fixNull(info.get2());

                    out.println(locale
                        + "\t" + version
                        + "\t" + value
                        + "\t" + olderValue
                        + "\t" + CldrUtility.ifNull(onlyNewerVersion, "n/a")
                        + "\t" + otherValue
                        + "\t" + olderOtherValue
                        + "\t" + xpath);

                }
            }
            return newer;
        }

        private String fixNull(String value) {
            if (value == null) {
                value = OutdatedPaths.NO_VALUE;
            }
            return value;
        }

        Set<String> writeBirth(String directory, String filename, Births onlyNewer) throws IOException {
            PrintWriter out = FileUtilities.openUTF8Writer(directory, filename + ".txt");
            Set<String> newer = writeBirth(out, onlyNewer);
            out.close();
            return newer;
        }
    }
}
