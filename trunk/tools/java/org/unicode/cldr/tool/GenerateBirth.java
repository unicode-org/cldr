package org.unicode.cldr.tool;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StringId;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.CharSequences;

public class GenerateBirth {
    private static boolean DEBUG = false;

    public enum Versions {
        trunk, v31_0, v30_0, v29_0, v28_0, v27_0, v26_0, v25_0, v24_0, v23_1, v22_1, v21_0, v2_0_1, v1_9_1, v1_8_1, v1_7_2, v1_6_1, v1_5_1, v1_4_1, v1_3_0, v1_2_0, v1_1_1;
        public String toString() {
            return this == Versions.trunk ? name() : name().substring(1).replace('_', '.');
        };
    }

    static final Versions[] VERSIONS = Versions.values();
    static final Factory[] factories = new Factory[VERSIONS.length];

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

        // set up the CLDR Factories

        DEBUG = myOptions.get("debug").doesOccur();

        final CLDRConfig config = CLDRConfig.getInstance();

        String filePattern = myOptions.get("file").getValue();

        ArrayList<Factory> list = new ArrayList<Factory>();
        for (Versions version : VERSIONS) {
            String base = version == Versions.trunk
                ? CLDRPaths.BASE_DIRECTORY
                : CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + version + "/";
            File[] paths = version.compareTo(Versions.v27_0) > 0 // warning, order is reversed
                ? new File[] { new File(base + "common/main/") }
                : new File[] { new File(base + "common/main/"), new File(base + "common/annotations/") };
            System.out.println(version + ", " + Arrays.asList(paths));
            Factory aFactory = SimpleFactory.make(paths, filePattern);
            list.add(aFactory);
        }
        list.toArray(factories);

        final String dataDirectory = myOptions.get("target").getValue();

        // load and process English

        String outputDirectory = myOptions.get("log").getValue();

        System.out.println("en");
        Births english = new Births("en");
        english.writeBirth(outputDirectory, "en", null);
        english.writeBirthValues(dataDirectory + "/" + OutdatedPaths.OUTDATED_ENGLISH_DATA);

        // if (!myOptions.get("file").doesOccur()) {
        // OutdatedPaths outdatedPaths = new OutdatedPaths(dataDirectory);
        //
        // return;
        // }
        // Set up the binary data file

        File file = new File(dataDirectory + "/" + OutdatedPaths.OUTDATED_DATA);
        final String outputDataFile = file.getCanonicalPath();
        System.out.println("Writing data: " + outputDataFile);
        DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(file));

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
            Set<String> newer = other.writeBirth(outputDirectory, fileName, english);

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
                    System.out.println("previous.isEmpty() != original" + locale + "\t" + StringId.getId(xpath) + "\t"
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
        final Relation<Versions, String> birthToPaths;
        final Map<String, Row.R3<Versions, String, String>> pathToBirthCurrentPrevious;
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
                    //e.printStackTrace();
                    break;
                }
            }
            birthToPaths = Relation.of(new TreeMap<Versions, Set<String>>(), TreeSet.class);
            pathToBirthCurrentPrevious = new HashMap<String, Row.R3<Versions, String, String>>();
            for (String xpath : files[0]) {

                xpath = xpath.intern();
                String base = files[0].getStringValue(xpath);
                String previousValue = null;
                int i;
                for (i = 1; i < files.length && files[i] != null; ++i) {
                    String previous = files[i].getStringValue(xpath);
                    if (previous == null) {
                        previous = fixNullPrevious(xpath);
                    }
                    if (!CharSequences.equals(base, previous)) {
                        if (previous != null) {
                            previousValue = previous;
                        }
                        break;
                    }
                }
                Versions version = VERSIONS[i - 1];
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
            System.out.println("Writing data: " + new File(file).getCanonicalPath());
            dataOut.writeInt(pathToBirthCurrentPrevious.size());

            // Load and process all the locales

            //TreeMap<String, Set<String>> localeToNewer = new TreeMap<String, Set<String>>();
            for (Entry<String, R3<Versions, String, String>> entry : pathToBirthCurrentPrevious.entrySet()) {
                String path = entry.getKey();
                R3<Versions, String, String> birthCurrentPrevious = entry.getValue();
                String previous = birthCurrentPrevious.get2();
                long id = StringId.getId(path);
                dataOut.writeLong(id);
                final String previousString = previous == null ? "" : previous;
                dataOut.writeUTF(previousString);
                if (previousString.isEmpty()) {
                    emptyPrevious.add(path);
                }
                if (DEBUG) {
                    System.out.println(id + "\t" + previous);
                }
            }
            dataOut.writeUTF("$END$");
            dataOut.close();
            emptyPrevious = Collections.unmodifiableSet(emptyPrevious);
        }

        Set<String> writeBirth(PrintWriter out, Births onlyNewer) {
            Set<String> newer = new HashSet<String>();
            HashMap<Long, String> sanityCheck = new HashMap<Long, String>();
            Versions onlyNewerVersion = Versions.trunk;
            String otherValue = "";
            String olderOtherValue = "";
            for (Entry<Versions, Set<String>> entry2 : birthToPaths.keyValuesSet()) {
                Versions version = entry2.getKey();
                for (String xpath : entry2.getValue()) {
                    long id = StringId.getId(xpath);
                    String old = sanityCheck.get(id);
                    if (old != null) {
                        throw new IllegalArgumentException("Path Collision " + xpath + ", old:" + old + ", id: " + id);
                    } else {
                        sanityCheck.put(id, xpath);
                    }
                    R3<Versions, String, String> info = pathToBirthCurrentPrevious.get(xpath);
                    if (onlyNewer != null) {

                        R3<Versions, String, String> otherInfo = onlyNewer.pathToBirthCurrentPrevious.get(xpath);
                        if (otherInfo == null) {
                            continue;
                        }
                        // skip if older or same
                        onlyNewerVersion = otherInfo.get0();
                        if (version.compareTo(onlyNewerVersion) <= 0) {
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
                        + "\t" + onlyNewerVersion
                        + "\t" + otherValue
                        + "\t" + olderOtherValue
                        + "\t" + xpath);

                }
            }
            return newer;
        }

        private String fixNull(String value) {
            if (value == null) {
                value = "∅";
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
