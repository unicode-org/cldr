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
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StringId;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;

public class GenerateBirth {
    private static boolean DEBUG = false;

    static CldrVersion[] VERSIONS;

    static Factory[] factories;

    final static Options myOptions = new Options()
        .add("target", ".*", CLDRPaths.BIRTH_DATA_DIR,
            "The target directory for building the text files that show the results.")
        .add("log", ".*", CLDRPaths.STAGING_DIRECTORY + "births/" + CldrVersion.baseline.getVersionInfo().getVersionString(2, 4),
            "The target directory for building the text files that show the results.")
        .add(
            "file",
            ".*",
            ".*",
            "Filter the information based on file name, using a regex argument. The '.xml' is removed from the file before filtering")
        .add("previous", "Stop after writing the English previous data.")
        .add("oldest",
            "\\d+(\\.\\d+)?",
            "36.0",
            "Oldest version to go back to, eg 36.1")
        .add("debug", "Debug");

    public static void main(String[] args) throws IOException {
        System.out.println("Run TestOutdatedPaths.java -v to see a listing of changes.");
        myOptions.parse(args, true);
        DEBUG = myOptions.get("debug").doesOccur();

        try {
            CldrVersion.checkVersions(); // verify versions up to date
        } catch (Exception e) {
            throw new ICUException("This tool can only be run if the archive of released versions matching CldrVersion is available.", e);
        }

        // generate the list for as far as we want to go back

        VersionInfo oldest = VersionInfo.getInstance(myOptions.get("oldest").getValue());
        List<CldrVersion> versions = new ArrayList<>();
        boolean foundStart = false;
        for (CldrVersion version : CldrVersion.CLDR_VERSIONS_DESCENDING) {
            versions.add(version);
           if (version.getVersionInfo() == oldest) {
               foundStart = true;
               break;
           }
        }
        if (!foundStart) {
            throw new IllegalArgumentException("The last version is " + myOptions.get("oldest").getValue() + "; it must be in: " + Joiner.on(", ").join(CldrVersion.CLDR_VERSIONS_DESCENDING));
        }
        VERSIONS = versions.toArray(new CldrVersion[versions.size()]);

        // set up the CLDR Factories for each version
        factories = new Factory[VERSIONS.length]; // hack for now; should change to list

        String filePattern = myOptions.get("file").getValue();

        ArrayList<Factory> list = new ArrayList<>();
        for (CldrVersion version : VERSIONS) {
            if (version == CldrVersion.unknown) {
                continue;
            }
            List<File> paths = version.getPathsForFactory();

            System.out.println(version + ", " + paths);
            Factory aFactory = SimpleFactory.make(paths.toArray(new File[paths.size()]), filePattern);
            list.add(aFactory);
        }
        list.toArray(factories);

        final String dataDirectory = myOptions.get("target").getValue();
        File dataDir = new File(dataDirectory);
        if (!dataDir.isDirectory()) {
            throw new IllegalArgumentException("-t value is not directory: " + dataDir);
        }

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
                    + "\tbirth: " + birth + "\tread: " + birthRead);
            }
        }

        // Set up the binary data files for all others

        File file = new File(dataDirectory + "/" + OutdatedPaths.OUTDATED_DATA);
        final String outputDataFile = PathUtilities.getNormalizedPathString(file);
        TreeMap<String, Set<String>> localeToNewer = new TreeMap<>();

        System.out.println("Writing data: " + outputDataFile);
        try (DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(file))) {
            dataOut.writeUTF(OutdatedPaths.FORMAT_KEY);

            // Load and process all the locales

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
        }

        // Doublecheck the data

        OutdatedPaths outdatedPaths = new OutdatedPaths(dataDirectory);
        Set<String> needPrevious = new TreeSet<>();
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
        private static final boolean USE_RESOLVED = false;
        final Relation<CldrVersion, String> birthToPaths;
        final Map<String, Row.R3<CldrVersion, String, String>> pathToBirthCurrentPrevious;
        final String locale;
        static final Pattern TYPE = PatternCache.get("\\[@type=\"([^\"]*)\"");
        final Matcher typeMatcher = TYPE.matcher("");
        Set<String> emptyPrevious = new HashSet<>();

        Births(String file) {
            locale = file;

            CLDRFile[] files = new CLDRFile[factories.length];
            DisplayAndInputProcessor[] processors = new DisplayAndInputProcessor[factories.length];

            for (int i = 0; i < factories.length; ++i) {
                try {
                    files[i] = factories[i].make(file, USE_RESOLVED);
                    processors[i] = new DisplayAndInputProcessor(files[i], false);
                } catch (Exception e) {
                    // stop when we fail to find
                    System.out.println("Stopped at " + file + ", " + CldrVersion.CLDR_VERSIONS_DESCENDING.get(i));
                    //e.printStackTrace();
                    break;
                }
            }
            birthToPaths = Relation.of(new TreeMap<CldrVersion, Set<String>>(), TreeSet.class);
            pathToBirthCurrentPrevious = new HashMap<>();
            for (String xpath : files[0]) {
                xpath = xpath.intern();
                if (xpath.contains("[@type=\"ar\"]")) {
                    int debug = 0;
                }
                String base = getProcessedStringValue(0, xpath, files, processors);

                String previousValue = null;
                int i;
                CLDRFile lastFile = files[0];
                for (i = 1; i < files.length && files[i] != null; ++i) {
                    String previous = getProcessedStringValue(i, xpath, files, processors);
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

        public String getProcessedStringValue(int fileNumber, String xpath, CLDRFile[] files, DisplayAndInputProcessor[] processors) {
            String base = files[fileNumber].getStringValue(xpath);
            if (base != null) {
                base = processors[fileNumber].processInput(xpath, base, null);
            }
            return base;
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
            try (DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(file))) {
                dataOut.writeUTF(OutdatedPaths.FORMAT_KEY);
                System.out.println("Writing data: " + PathUtilities.getNormalizedPathString(file));
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
                emptyPrevious = Collections.unmodifiableSet(emptyPrevious);
            }
        }

        Set<String> writeBirth(PrintWriter out, Births onlyNewer) {

            out.println("Loc\tVersion\tValue\tPrevValue\tEVersion\tEValue\tEPrevValue\tPath");

            Set<String> newer = new HashSet<>();
            HashMap<Long, String> sanityCheck = new HashMap<>();
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
            try (PrintWriter out = FileUtilities.openUTF8Writer(directory, filename + ".txt")) {
                Set<String> newer = writeBirth(out, onlyNewer);
                return newer;
            }
        }
    }
}
