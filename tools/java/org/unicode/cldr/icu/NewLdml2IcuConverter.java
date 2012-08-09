package org.unicode.cldr.icu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.ant.CLDRConverterTool;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.icu.ResourceSplitter.SplitInfo;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;

/**
 * Simpler mechanism for converting CLDR data to ICU Resource Bundles, intended
 * to replace LDML2ICUConverter. The format is almost entirely data-driven
 * instead of having lots of special-case code.
 *
 * The flags used to specify the data to be generated are copied directly from
 * LDML2ICUConverter.
 *
 * Unlike the instructions in CLDRConverterTool, this converter does not invoke
 * computeConvertibleXPaths to check if each xpath is convertible because the
 * xpaths that are convertible have already been filtered out by the regex lookups.
 * It may make more sense down the road to refactor CLDRConverterTool such that
 * this class doesn't inherit unnecessary functionality.
 *
 * A rough overview of the new converter is available at
 * https://sites.google.com/site/cldr/development/coding-cldr-tools/newldml2icuconverter
 * @author jchye
 */
public class NewLdml2IcuConverter extends CLDRConverterTool {
    static final boolean DEBUG = true;

    static final Pattern SEMI = Pattern.compile("\\s*+;\\s*+");

    /*
     * The type of file to be converted.
     */
    enum Type {
        locales, 
        dayPeriods,
        genderList, likelySubtags,
        metadata, metaZones,
        numberingSystems,
        plurals,
        postalCodeData,
        supplementalData, 
        windowsZones,
        keyTypeData;
    }

    private static final Options options = new Options(
            "Usage: LDML2ICUConverter [OPTIONS] [FILES]\n" +
            "This program is used to convert LDML files to ICU data text files.\n" +
            "Please refer to the following options. Options are not case sensitive.\n" +
            "\texample: org.unicode.cldr.icu.Ldml2IcuConverter -s xxx -d yyy en")
        .add("sourcedir", ".*", "Source directory for CLDR files")
        .add("destdir", ".*", ".", "Destination directory for output files, defaults to the current directory")
        .add("specialsdir", 'p', ".*", null, "Source directory for files containing special data, if any")
        .add("supplementaldir", 'm', ".*", null, "The supplemental data directory")
        .add("keeptogether", 'k', null, null, "Write locale data to one file instead of splitting into separate directories. For debugging")
        .add("type", 't', "\\w+", null, "The type of file to be generated")
        .add("cldrVersion", 'c', ".*", "21.0", "The version of the CLDR data, used purely for supplementalData output.");

    private static final String LOCALES_DIR = "locales";

    private boolean keepTogether = false;
    private Map<String, String> dirMapping;
    private Set<String> allDirs;
    private String sourceDir;
    private String destinationDir;
    private IcuDataSplitter splitter;

    /**
     * Maps ICU paths to the directories they should end up in.
     */
    private Map<String, String> getDirMapping() {
        if (dirMapping == null) {
            dirMapping = loadMapFromFile("ldml2icu_dir_mapping.txt");
            allDirs = new HashSet<String>(dirMapping.values());
            allDirs.remove("*");
            allDirs.add(LOCALES_DIR);
        }
        return dirMapping;
    }

    private static Map<String, String> loadMapFromFile(String filename) {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader reader = FileUtilities.openFile(NewLdml2IcuConverter.class, filename);
        String line;
        try {
            int lineNum = 1;
            while((line = reader.readLine()) != null) {
                if (line.length() == 0 || line.startsWith("#")) continue;
                String[] content = line.split(SEMI.toString());
                if (content.length != 2) {
                    throw new IllegalArgumentException("Invalid syntax of " + filename + " at line " + lineNum);
                }
                map.put(content[0], content[1]);
                lineNum++;
            }
        } catch(IOException e) {
            System.err.println("Failed to read fallback file.");
            e.printStackTrace();
        }
        return map;
    }

    private List<SplitInfo> loadSplitInfoFromFile() {
        Map<String, String> dirMapping = getDirMapping();
        List<SplitInfo> splitInfos = new ArrayList<SplitInfo>();
        for (Entry<String, String> entry : dirMapping.entrySet()) {
            SplitInfo splitInfo = new SplitInfo(entry.getKey(), entry.getValue());
            splitInfos.add(splitInfo);
        }
        return splitInfos;
    }

    @Override
    public void processArgs(String[] args) {
        Set<String> extraArgs = options.parse(args, true);
        // For supplemental output files, the supplemental directory is specified
        // as the source directory and the supplemental directory argument is
        // not required.
        if (!options.get("sourcedir").doesOccur()) {
            throw new IllegalArgumentException("Source directory must be specified.");
        }
        sourceDir = options.get("sourcedir").getValue();

        destinationDir = options.get("destdir").getValue();
        if (!options.get("type").doesOccur()) {
            throw new IllegalArgumentException("Type not specified");
        }
        Type type = Type.valueOf(options.get("type").getValue());
        keepTogether = options.get("keeptogether").doesOccur();
        if (!keepTogether && type == Type.supplementalData || type == Type.locales) {
            if (splitInfos == null) {
                splitInfos = loadSplitInfoFromFile();
            }
            splitter = IcuDataSplitter.make(destinationDir, splitInfos);
        }

        // Process files.
        switch (type) {
        case locales:
            // Generate locale data.
            SupplementalDataInfo supplementalDataInfo = null;
            Option option = options.get("supplementaldir");
            if (option.doesOccur()) {
                supplementalDataInfo = SupplementalDataInfo.getInstance(options.get("supplementaldir").getValue());
            } else {
                throw new IllegalArgumentException("Supplemental directory must be specified.");
            }

            Factory specialFactory = null;
            option = options.get("specialsdir");
            if (option.doesOccur()) {
                specialFactory = Factory.make(option.getValue(), ".*");
            }

            // LocalesMap passed in from ant
            List<String> locales = new ArrayList<String>();
            Factory factory = null;
            if (getLocalesMap() != null && getLocalesMap().size() > 0) {
                for (String filename : getLocalesMap().keySet()) {
                    // Remove ".xml" from the end.
                    locales.add(filename.substring(0, filename.length() - 4));
                }
                factory = Factory.make(sourceDir, ".*", DraftStatus.contributed);
                Collections.sort(locales);
            } else if (extraArgs.size() > 0) {
                factory = Factory.make(sourceDir, extraArgs.iterator().next(), DraftStatus.contributed);
                locales.addAll(factory.getAvailable());
            } else {
                throw new IllegalArgumentException("No files specified!");
            }

            LocaleMapper mapper = new LocaleMapper(factory, specialFactory, supplementalDataInfo);
            processLocales(mapper, locales);
            break;
        case keyTypeData:
            processBcp47Data(type);
            break;
        default: // supplemental data
            processSupplemental(type, options.get("cldrVersion").getValue());
        }
    }

    private void processBcp47Data(Type type) {
        Bcp47Mapper mapper = new Bcp47Mapper(sourceDir);
        IcuData[] icuData = mapper.fillFromCldr();
        for (IcuData data : icuData) {
            writeIcuData(data, destinationDir);
        }
    }

    private void processSupplemental(Type type, String cldrVersion) {
        IcuData icuData;
        if (type == Type.plurals) {
            PluralsMapper mapper = new PluralsMapper(sourceDir);
            icuData = mapper.fillFromCldr();
        } else if (type == Type.dayPeriods) {
            DayPeriodsMapper mapper = new DayPeriodsMapper(sourceDir);
            icuData = mapper.fillFromCldr();
        } else {
            SupplementalMapper mapper = new SupplementalMapper(sourceDir, cldrVersion);
            icuData = mapper.fillFromCldr(type.toString());
        }
        writeIcuData(icuData, destinationDir);
    }

    /**
     * Writes the given IcuData object to file.
     * @param icuData the IcuData object to be written
     * @param outputDir the destination directory of the output file
     */
    private void writeIcuData(IcuData icuData, String outputDir) {
        if (icuData.keySet().size() == 0) {
            throw new RuntimeException(icuData.getName() + " was not written because no data was generated.");
        }
        try {
            // Split data into different directories if necessary.
            // splitInfos is filled from the <remap> element in ICU's build.xml.
            if (splitter == null) {
                IcuTextWriter.writeToFile(icuData, outputDir);
            } else {
                String fallbackDir = new File(outputDir).getName();
                Map<String, IcuData> splitData = splitter.split(icuData, fallbackDir);
                for (String dir : splitData.keySet()) {
                    IcuTextWriter.writeToFile(splitData.get(dir), outputDir + "/../" + dir);
                }
            }
        } catch (IOException e) {
            System.err.println("Error while converting " + icuData.getSourceFile());
            e.printStackTrace();
        }
    }

    private void processLocales(LocaleMapper mapper, List<String> locales) {
        for (String locale : locales) {
            long time = System.currentTimeMillis();
            IcuData icuData = mapper.fillFromCLDR(locale);
            writeIcuData(icuData, destinationDir);
            System.out.println("Converted " + locale + ".xml in " +
                    (System.currentTimeMillis() - time) + "ms");
        }
    }

    /**
     * TODO: call this method when we switch over to writing aliased files from
     * the LDML2ICUConverter. aliasList = aliasDeprecates.aliasList.
     * @param mapper
     * @param aliasList
     */
    private void writeAliasedFiles(LocaleMapper mapper, List<Alias> aliasList) {
        for (Alias alias: aliasList) {
            IcuData icuData = mapper.fillFromCldr(alias);
            if (icuData != null) {
                writeIcuData(icuData, destinationDir);
            }
        }
    }

    /**
     * In this prototype, just convert one file.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        long totalTime = System.currentTimeMillis();
        NewLdml2IcuConverter converter = new NewLdml2IcuConverter();
        converter.processArgs(args);
        System.out.println("Total time taken: " + (System.currentTimeMillis() - totalTime));
    }

}
