package org.unicode.cldr.icu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.ant.CLDRConverterTool;
import org.unicode.cldr.icu.ResourceSplitter.SplitInfo;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileReaders;
import org.unicode.cldr.util.PatternCache;
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
 *
 * @author jchye
 */
public class NewLdml2IcuConverter extends CLDRConverterTool {
    private static final String ALIAS_PATH = "/\"%%ALIAS\"";

    static final boolean DEBUG = true;

    static final Pattern SEMI = PatternCache.get("\\s*+;\\s*+");

    /*
     * The type of file to be converted.
     */
    enum Type {
        locales, dayPeriods, genderList, likelySubtags, metadata, metaZones, numberingSystems, plurals, pluralRanges, postalCodeData, rgScope, supplementalData, windowsZones, keyTypeData, brkitr, collation, rbnf;
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
                .add("keeptogether", 'k', null, null,
                    "Write locale data to one file instead of splitting into separate directories. For debugging")
                .add("type", 't', "\\w+", null, "The type of file to be generated")
                .add("xpath", 'x', ".*", null, "An optional xpath to debug the regexes with")
                .add("filter", 'f', null, null, "Perform filtering on the locale data to be converted.")
                .add("organization", 'o', ".*", null, "The organization to filter the data for")
                .add("makefile", 'g', ".*", null, "If set, generates makefiles and alias files for the specified type. " +
                    "The value to set should be the name of the makefile.")
                .add("depgraphfile", 'e', ".*", null, "If set, generates a dependency graph file in JSON form summarizing parent and alias mappings between locale files. Only works when --type=locales.")
                .add("verbose", 'v', null, null, "Debugging aids");

    private static final String LOCALES_DIR = "locales";

    private boolean keepTogether = false;
    private Map<String, String> dirMapping;
    private Set<String> allDirs;
    private String sourceDir;
    private String destinationDir;
    private String supplementalDir;
    private IcuDataSplitter splitter;
    private Filter filter;
    private boolean verbose = false;

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
        BufferedReader reader = FileReaders.openFile(NewLdml2IcuConverter.class, filename);
        String line;
        try {
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0 || line.startsWith("#")) continue;
                String[] content = line.split(SEMI.toString());
                if (content.length != 2) {
                    throw new IllegalArgumentException("Invalid syntax of " + filename + " at line " + lineNum);
                }
                map.put(content[0], content[1]);
                lineNum++;
            }
        } catch (IOException e) {
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
        supplementalDir = options.get("supplementaldir").getValue();

        destinationDir = options.get("destdir").getValue();
        if (!options.get("type").doesOccur()) {
            throw new IllegalArgumentException("Type not specified: " + Arrays.asList(Type.values()));
        }
        Type type = Type.valueOf(options.get("type").getValue());
        keepTogether = options.get("keeptogether").doesOccur();
        if (!keepTogether && type == Type.supplementalData || type == Type.locales) {
            if (splitInfos == null) {
                splitInfos = loadSplitInfoFromFile();
            }
            splitter = IcuDataSplitter.make(destinationDir, splitInfos);
        }

        verbose = options.get("verbose").doesOccur();

        String debugXPath = options.get("xpath").getValue();
        // Quotes are stripped out at the command line so add them back in.
        if (debugXPath != null) {
            debugXPath = debugXPath.replaceAll("=([^\\]\"]++)\\]", "=\"$1\"\\]");
        }

        Factory specialFactory = null;
        File specialsDir = null;
        Option option = options.get("specialsdir");
        if (option.doesOccur()) {
            if (type == Type.rbnf) {
                specialsDir = new File(option.getValue());
            } else {
                specialFactory = Factory.make(option.getValue(), ".*");
            }
        } else if (type == Type.brkitr) {
            specialFactory = Factory.make(options.get("specialsdir").getValue(), ".*");
        }

        // Get list of locales if defined.
        Set<String> includedLocales = getIncludedLocales();
        Map<String, String> localesMap = getLocalesMap();
        if (includedLocales != null && includedLocales.size() > 0) {
            final Set<String> locales = new HashSet<String>();
            for (String locale : includedLocales) {
                if (localesMap.containsKey(locale + ".xml")) {
                    locales.add(locale);
                }
            }

            filter = new Filter() {
                @Override
                public boolean includes(String value) {
                    return locales.contains(value);
                }
            };
        } else if (extraArgs.size() > 0) {
            final String regex = extraArgs.iterator().next();
            filter = new Filter() {
                @Override
                public boolean includes(String value) {
                    return value.matches(regex);
                }
            };
        } else if (type == Type.locales || type == Type.collation) {
            throw new IllegalArgumentException(
                "Missing locale list. Please provide a list of locales or a regex.");
        } else {
            filter = new Filter() {
                @Override
                public boolean includes(String value) {
                    return true;
                }
            };
        }

        // Process files.
        Mapper mapper = null;
        switch (type) {
        case locales:
            // Generate locale data.
            SupplementalDataInfo supplementalDataInfo = null;
            option = options.get("supplementaldir");
            if (option.doesOccur()) {
                supplementalDataInfo = SupplementalDataInfo.getInstance(supplementalDir);
            } else {
                throw new IllegalArgumentException("Supplemental directory must be specified with -s");
            }

            option = options.get("depgraphfile");
            if (option.doesOccur()) {
                DependencyGraphData dependencyGraphData = new DependencyGraphData(
                    supplementalDataInfo, aliasDeprecates.aliasList);
                generateDependencyGraphFile(dependencyGraphData, option.getValue());
            }

            Factory factory = Factory.make(sourceDir, ".*", DraftStatus.contributed);
            String organization = options.get("organization").getValue();
            LocaleMapper localeMapper = new LocaleMapper(factory, specialFactory,
                supplementalDataInfo, options.get("filter").doesOccur(), organization);
            localeMapper.setDebugXPath(debugXPath);
            mapper = localeMapper;
            break;
        case keyTypeData:
            processBcp47Data();
            break;
        case brkitr:
            mapper = new BreakIteratorMapper(sourceDir, specialFactory);
            break;
        case collation:
            mapper = new CollationMapper(sourceDir, specialFactory);
            break;
        case rbnf:
            mapper = new RbnfMapper(new File(sourceDir), specialsDir);
            break;
        default: // supplemental data
            processSupplemental(type, debugXPath);
        }

        if (mapper != null) {
            convert(mapper);
            option = options.get("makefile");
            if (option.doesOccur()) {
                generateMakefile(mapper, option.getValue());
            }
        }
    }

    private void processBcp47Data() {
        Bcp47Mapper mapper = new Bcp47Mapper(sourceDir);
        IcuData[] icuData = mapper.fillFromCldr();
        for (IcuData data : icuData) {
            writeIcuData(data, destinationDir);
        }
    }

    private void processSupplemental(Type type, String debugXPath) {
        IcuData icuData;
        // Use the supplementaldir if explicitly specified , otherwise the source dir.
        String dir = options.get("supplementaldir").doesOccur() ? supplementalDir : sourceDir;
        switch (type) {
        case plurals: {
            PluralsMapper mapper = new PluralsMapper(dir);
            icuData = mapper.fillFromCldr();
            break;
        }
        case pluralRanges: {
            PluralRangesMapper mapper = new PluralRangesMapper(dir);
            icuData = mapper.fillFromCldr();
            break;
        }
        case dayPeriods: {
            DayPeriodsMapper mapper = new DayPeriodsMapper(dir);
            icuData = mapper.fillFromCldr();
            break;
        }
        default: {
            SupplementalMapper mapper = SupplementalMapper.create(dir);
            if (debugXPath != null) {
                mapper.setDebugXPath(debugXPath);
            }
            icuData = mapper.fillFromCldr(type.toString());
        }
        }
        writeIcuData(icuData, destinationDir);
    }

    /**
     * Writes the given IcuData object to file.
     *
     * @param icuData
     *            the IcuData object to be written
     * @param outputDir
     *            the destination directory of the output file
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

    /**
     * Converts CLDR XML files using the specified mapper.
     */
    private void convert(Mapper mapper) {
        IcuData icuData;
        Iterator<IcuData> iterator = mapper.iterator(filter);
        final Type type = Type.valueOf(options.get("type").getValue());
        while (iterator.hasNext()) {
            long time = System.currentTimeMillis();
            icuData = iterator.next();
            writeIcuData(icuData, destinationDir);
            System.out.println("Converted " + type + ": " + icuData.getName() + ".xml in " +
                (System.currentTimeMillis() - time) + "ms");
        }
    }

    /**
     * Generates makefiles for files generated from the specified mapper.
     * @param mapper
     * @param makefileName
     */
    private void generateMakefile(Mapper mapper, String makefileName) {
        // Generate aliases and makefiles for main directory.
        Set<String> aliases = writeSyntheticFiles(mapper.getGenerated(), destinationDir);
        Makefile makefile = mapper.generateMakefile(aliases);
        writeMakefile(makefile, destinationDir, makefileName);
        if (splitter == null) return;

        // Generate aliases and locales for remaining directories if a splitter was used.
        for (String dir : splitter.getTargetDirs()) {
            File outputDir = new File(destinationDir, "../" + dir);
            aliases = writeSyntheticFiles(splitter.getDirSources(dir), outputDir.getAbsolutePath());
            makefile = splitter.generateMakefile(aliases, outputDir.getName());
            writeMakefile(makefile, outputDir.getAbsolutePath(), makefileName);
        }
    }

    /**
     * Generates dependency graph files (usually named _dependencies.py).
     */
    private void generateDependencyGraphFile(DependencyGraphData dependencyGraphData, String filename) {
        try {
            dependencyGraphData.print(destinationDir, filename);
        } catch (IOException e) {
            System.err.println("Unable to write " + filename + ": " + e);
            System.exit(-1);
        }
    }

    /**
     * Creates all synthetic files needed by the makefile in the specified output directory.
     * @param sources the set of source files that have already been generated
     * @param outputDir
     * @return
     */
    private Set<String> writeSyntheticFiles(Set<String> sources, String outputDir) {
        Set<String> targets = new HashSet<String>();
        if (aliasDeprecates != null) {
            if (aliasDeprecates.emptyLocaleList != null) {
                for (String locale : aliasDeprecates.emptyLocaleList) {
                    IcuData icuData = createEmptyFile(locale);
                    System.out.println("Empty locale created: " + locale);
                    targets.add(locale);
                    writeIcuData(icuData, outputDir);
                }
            }
            if (aliasDeprecates.aliasList != null) {
                for (Alias alias : aliasDeprecates.aliasList) {
                    try {
                        writeAlias(alias, outputDir, sources, targets);
                    } catch (IOException e) {
                        System.err.println("Error writing alias " + alias.from + "-" + alias.to);
                        System.exit(-1);
                    }
                }
            }
        }
        return targets;
    }

    /**
     * Writes a makefile to the specified directory and filename.
     */
    private void writeMakefile(Makefile makefile, String outputDir, String makefileName) {
        try {
            new File(outputDir + File.separator + makefileName).createNewFile();
            makefile.print(outputDir, makefileName);
        } catch (IOException e) {
            System.err.println("Error while writing makefile for " + outputDir + "/" + makefileName);
        }
    }

    /**
     * Creates an empty IcuData object to act as a placeholder for the specified alias target locale.
     */
    public IcuData createEmptyFile(String locale) {
        IcuData icuData = new IcuData("icu-locale-deprecates.xml & build.xml", locale, true);
        icuData.setFileComment("generated alias target");
        icuData.add("/___", "");
        return icuData;
    }

    /**
     * Creates any synthetic files required for the specified alias.
     * @param alias
     * @param outputDir
     * @param sources the set of sources in the output directory
     * @param aliasTargets the alias targets already created in the output directory
     * @throws IOException
     */
    private void writeAlias(Alias alias, String outputDir,
        Set<String> sources, Set<String> aliasTargets) throws IOException {
        String from = alias.from;
        String to = alias.to;
        // Add synthetic destination file for alias if necessary.
        if (!sources.contains(to) && !aliasTargets.contains(to) && new File(outputDir + File.separator + alias.to + ".txt").createNewFile()) {
            System.out.println(to + " not found, creating empty file in " + outputDir);
            IcuTextWriter.writeToFile(createEmptyFile(alias.to), outputDir);
            aliasTargets.add(to);
        }

        if (from == null || to == null) {
            throw new IllegalArgumentException("Malformed alias - no 'from' or 'to': from=\"" +
                from + "\" to=\"" + to + "\"");
        }

        if (sources.contains(from)) {
            throw new IllegalArgumentException(
                "Can't be both a synthetic alias locale and a real xml file - "
                    + "consider using <aliasLocale locale=\"" + from + "\"/> instead. ");
        }

        String rbPath = alias.rbPath;
        String value = alias.value;
        if ((rbPath == null) != (value == null)) {
            throw new IllegalArgumentException("Incomplete alias specification for " +
                from + "-" + to + ": both rbPath (" +
                rbPath + ") and value (" + value + ") must be specified");
        }

        IcuData icuData = new IcuData("icu-locale-deprecates.xml & build.xml", from, true);
        if (rbPath == null) {
            icuData.add(ALIAS_PATH, to);
        } else {
            icuData.add(rbPath, value);
        }

        if (new File(outputDir + File.separator + from + ".txt").createNewFile()) {
            IcuTextWriter.writeToFile(icuData, outputDir);
            aliasTargets.add(alias.from);
            System.out.println("Created alias from " + from + " to " + to + " in " + outputDir + ".");
        }
    }

    public static void main(String[] args) throws IOException {
        long totalTime = System.currentTimeMillis();
        NewLdml2IcuConverter converter = new NewLdml2IcuConverter();
        converter.processArgs(args);
        System.out.println("Total time taken: " + (System.currentTimeMillis() - totalTime) + "ms");
    }
}
