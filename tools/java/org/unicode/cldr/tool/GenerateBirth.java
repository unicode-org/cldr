package org.unicode.cldr.tool;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StringId;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.CharSequences;

public class GenerateBirth {
    private static final boolean DEBUG = false;
    enum Versions {
        trunk, v1_9_0, v1_8_1, v1_7_2, v1_6_1, v1_5_1, v1_4_1, v1_3_0, v1_2_0, v1_1_1;
        public String toString() {
            return this == Versions.trunk ? name() : name().substring(1).replace('_', '.');
        };
    }
    static final Versions[] VERSIONS = Versions.values();
    static final Factory[] factories = new Factory[VERSIONS.length];
    
    final static Options myOptions = new Options()
    .add("target", ".*", CldrUtility.UTIL_CODE_DIR + "test/", "The target directory for building the text files that show the results.")
    .add("log", ".*", CldrUtility.TMP_DIRECTORY + "dropbox/births/", "The target directory for building the text files that show the results.")
    .add("file", ".*", ".*", "Filter the information based on file name, using a regex argument. The '.xml' is removed from the file before filtering")
    ;

    public static void main(String[] args) throws IOException {
        myOptions.parse(args, true);
        
        // set up the CLDR Factories
        
        String filePattern = myOptions.get("file").getValue();

        ArrayList<Factory> list = new ArrayList<Factory>();
        for (Versions version : VERSIONS) {
            Factory aFactory = Factory.make(CldrUtility.BASE_DIRECTORY 
                    + (version == Versions.trunk ? "" : "../cldr-" + version) 
                    + "/common/main/", filePattern);
            list.add(aFactory);
        }
        list.toArray(factories);

        // load and process English
        
        String outputDirectory = myOptions.get("log").getValue();

        System.out.println("en");
        Births english = new Births("en");
        english.writeBirth(outputDirectory, "en", null);
        
        // Set up the binary data file

        File file = new File(myOptions.get("target").getValue() + "outdated.data");
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
        
        OutdatedPaths outdatedPaths = new OutdatedPaths(outputDataFile);
        for (Entry<String, Set<String>> localeAndNewer : localeToNewer.entrySet()) {
            String locale = localeAndNewer.getKey();
            System.out.println("Checking " + locale);
            Set<String> newer = localeAndNewer.getValue();
            if (newer.size() != outdatedPaths.countOutdated(locale)) {
                throw new IllegalArgumentException("broken: " + locale);
            }
            for (String xpath : newer) {
                boolean isOutdated = outdatedPaths.isOutdated(locale, xpath);
                if (!isOutdated) {
                    throw new IllegalArgumentException("broken: " + locale + "\t" + xpath);
                }
            }
        }
    }


    static class Births {
        final Relation<Versions, String> birthToPaths;
        final Map<String, Row.R3<Versions,String,String>> pathToBirthCurrentPrevious;
        final String locale;

        Births(String file) {
            locale = file;
            CLDRFile[] files = new CLDRFile[factories.length];
            for (int i = 0; i < factories.length; ++i) {
                try {
                    files[i] = factories[i].make(file, false);
                } catch (Exception e) {
                    break;
                }
            }
            birthToPaths = Relation.of(new TreeMap<Versions, Set<String>>(), TreeSet.class);
            pathToBirthCurrentPrevious = new HashMap();
            for (String xpath : files[0]) {
                xpath = xpath.intern();
                String base = files[0].getStringValue(xpath);
                String previousValue = null;
                int i;
                for (i = 1; i < files.length && files[i] != null; ++i) {
                    String previous = files[i].getStringValue(xpath);
                    if (!CharSequences.equals(base, previous)) {
                        if (previous != null) {
                            previousValue = previous;
                        }
                        break;
                    }
                }
                Versions version = VERSIONS[i-1];
                birthToPaths.put(version, xpath);
                pathToBirthCurrentPrevious.put(xpath, Row.of(version, base, previousValue));
            }
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
                        if (xpath.contains("/exemplarCharacters") || xpath.contains("/references")) {
                            continue;
                        }
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

                    out.println(locale + "\t" + version + "\t" + onlyNewerVersion 
                            + "\t" + xpath 
                            + "\t" + value
                            + "\t" + olderValue
                            + "\t" + otherValue 
                            + "\t" + olderOtherValue 
                    );

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
            PrintWriter out = BagFormatter.openUTF8Writer(directory, filename + ".txt");
            Set<String> newer = writeBirth(out, onlyNewer);
            out.close();
            return newer;
        }
    }
}
