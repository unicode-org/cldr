package org.unicode.cldr.tool;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.ibm.icu.util.Output;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.AnnotationUtil;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GlossonymConstructor;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.ParentLocaleComponent;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

public class GenerateProductionData {
    private static boolean DEBUG = false;
    private static boolean VERBOSE = false;
    private static Matcher FILE_MATCH = null;

    private static String SOURCE_COMMON_DIR = null;
    private static String DEST_COMMON_DIR = null;

    private static boolean ADD_LOGICAL_GROUPS = false;
    private static boolean ADD_DATETIME = false;
    private static boolean ADD_SIDEWAYS = false;
    private static boolean ADD_ROOT = false;
    private static boolean INCLUDE_COMPREHENSIVE = false;
    private static boolean CONSTRAINED_RESTORATION = false;

    private static final Set<String> NON_XML =
            ImmutableSet.of("dtd", "properties", "testData", "uca");
    private static final Set<String> COPY_ANYWAY =
            ImmutableSet.of(
                    "casing",
                    "collation"); // don't want to "clean up", makes format difficult to use
    private static final SupplementalDataInfo SDI =
            CLDRConfig.getInstance().getSupplementalDataInfo();

    enum MyOptions {
        sourceDirectory(
                new Params()
                        .setHelp("source common directory")
                        .setDefault(CLDRPaths.COMMON_DIRECTORY)
                        .setMatch(".*")),
        destinationDirectory(
                new Params()
                        .setHelp("destination common directory")
                        .setDefault(CLDRPaths.STAGING_DIRECTORY + "production/common")
                        .setMatch(".*")),
        logicalGroups(
                new Params()
                        .setHelp("add path/values for logical groups")
                        .setDefault("true")
                        .setMatch("true|false")),
        time(
                new Params()
                        .setHelp("add path/values for stock date/time/datetime")
                        .setDefault("true")
                        .setMatch("true|false")),
        Sideways(
                new Params()
                        .setHelp("add path/values for sideways inheritance")
                        .setDefault("true")
                        .setMatch("true|false")),
        root(
                new Params()
                        .setHelp("add path/values for root and code-fallback")
                        .setDefault("true")
                        .setMatch("true|false")),
        constrainedRestoration(
                new Params()
                        .setHelp("only add inherited paths that were in original file")
                        .setDefault("true")
                        .setMatch("true|false")),
        includeComprehensive(
                new Params()
                        .setHelp("exclude comprehensive paths — otherwise just to modern level")
                        .setDefault("true")
                        .setMatch("true|false")),
        verbose(new Params().setHelp("verbose debugging messages")),
        Debug(new Params().setHelp("debug")),
        fileMatch(new Params().setHelp("regex to match patterns").setMatch(".*")),
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

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        // TODO rbnf and segments don't have modern coverage; fix there.

        MyOptions.parse(args, true);
        SOURCE_COMMON_DIR = MyOptions.sourceDirectory.option.getValue();
        DEST_COMMON_DIR = MyOptions.destinationDirectory.option.getValue();

        // debugging
        VERBOSE = MyOptions.verbose.option.doesOccur();
        DEBUG = MyOptions.Debug.option.doesOccur();
        String fileMatch = MyOptions.fileMatch.option.getValue();
        if (fileMatch != null) {
            FILE_MATCH = Pattern.compile(fileMatch).matcher("");
        }

        // controls for minimization
        ADD_LOGICAL_GROUPS = "true".equalsIgnoreCase(MyOptions.logicalGroups.option.getValue());
        ADD_DATETIME = "true".equalsIgnoreCase(MyOptions.time.option.getValue());
        ADD_SIDEWAYS = "true".equalsIgnoreCase(MyOptions.Sideways.option.getValue());
        ADD_ROOT = "true".equalsIgnoreCase(MyOptions.root.option.getValue());

        // constraints
        INCLUDE_COMPREHENSIVE =
                "true".equalsIgnoreCase(MyOptions.includeComprehensive.option.getValue());
        CONSTRAINED_RESTORATION =
                "true".equalsIgnoreCase(MyOptions.constrainedRestoration.option.getValue());

        // get directories

        Map<File, File> specialDirectories = new TreeMap<>();

        Arrays.asList(DtdType.values())
                // .parallelStream()
                // .unordered()
                .forEach(
                        type -> {
                            boolean isLdmlDtdType = type == DtdType.ldml;

                            // bit of a hack, using the ldmlICU — otherwise unused! — to get the
                            // nonXML files.
                            Set<String> directories =
                                    (type == DtdType.ldmlICU) ? NON_XML : type.directories;

                            for (String dir : directories) {
                                File sourceDir = new File(SOURCE_COMMON_DIR, dir);
                                File destinationDir = new File(DEST_COMMON_DIR, dir);
                                Stats stats = new Stats();
                                copyFilesAndReturnIsEmpty(
                                        sourceDir, destinationDir, null, isLdmlDtdType, stats);
                                if (directoryIsSpecial(sourceDir.getAbsolutePath())) {
                                    specialDirectories.put(sourceDir, destinationDir);
                                }
                            }
                        });

        for (File source : specialDirectories.keySet()) {
            File dest = specialDirectories.get(source);
            doubleCheckSpecialPaths(source, dest);
        }
    }

    private static class Stats {
        long files;
        long removed;
        long retained;
        long remaining;

        Stats clear() {
            files = removed = retained = remaining = 0;
            return this;
        }

        @Override
        public String toString() {
            return "files="
                    + files
                    + (removed + retained + remaining == 0
                            ? ""
                            : "; removed="
                                    + removed
                                    + "; retained="
                                    + retained
                                    + "; remaining="
                                    + remaining);
        }

        public void showNonZero(String label) {
            if (removed + retained + remaining != 0) {
                System.out.println(label + toString());
            }
        }
    }

    /**
     * Copy files in directories, recursively.
     *
     * @param sourceFile
     * @param destinationFile
     * @param factory
     * @param isLdmlDtdType
     * @param stats
     * @return true if the file is an ldml file with empty content.
     */
    private static boolean copyFilesAndReturnIsEmpty(
            File sourceFile,
            File destinationFile,
            Factory factory,
            boolean isLdmlDtdType,
            final Stats stats) {
        if (sourceFile.isDirectory()) {

            System.out.println(sourceFile + " => " + destinationFile);
            if (!destinationFile.mkdirs()) {
                // if created, remove old contents
                Arrays.stream(destinationFile.listFiles()).forEach(File::delete);
            }

            Set<String> sorted = new TreeSet<>();
            sorted.addAll(Arrays.asList(sourceFile.list()));

            if (COPY_ANYWAY.contains(sourceFile.getName())) { // special cases
                isLdmlDtdType = false;
            }
            // reset factory for directory
            factory = null;
            if (isLdmlDtdType) {
                // if the factory is empty, then we just copy files
                factory = Factory.make(sourceFile.toString(), ".*");
            }
            boolean isMainDir = factory != null && sourceFile.getName().contentEquals("main");
            boolean isRbnfDir = factory != null && sourceFile.getName().contentEquals("rbnf");
            boolean isAnnotationsDir =
                    factory != null && sourceFile.getName().startsWith("annotations");

            Set<String> emptyLocales = new HashSet<>();
            final Stats stats2 = new Stats();
            final Factory theFactory = factory;
            final boolean isLdmlDtdType2 = isLdmlDtdType;
            sorted
                    // .parallelStream()
                    .forEach(
                    file -> {
                        File sourceFile2 = new File(sourceFile, file);
                        File destinationFile2 = new File(destinationFile, file);
                        if (VERBOSE) System.out.println("\t" + file);

                        // special step to just copy certain files like main/root.xml file
                        Factory currFactory = theFactory;
                        if (isMainDir) {
                            if (file.equals("root.xml")) {
                                currFactory = null;
                            }
                        } else if (isRbnfDir) {
                            currFactory = null;
                        }

                        // when the currFactory is null, we just copy files as-is
                        boolean isEmpty =
                                copyFilesAndReturnIsEmpty(
                                        sourceFile2,
                                        destinationFile2,
                                        currFactory,
                                        isLdmlDtdType2,
                                        stats2);
                        if (isEmpty) { // only happens for ldml
                            emptyLocales.add(getLocaleIdFromFileName(file));
                        }
                    });
            stats2.showNonZero("\tTOTAL:\t");
            // if there are empty ldml files, AND we aren't in /main/,
            // then remove any without children
            if (!emptyLocales.isEmpty() && !isMainDir) {
                Set<String> childless =
                        getChildless(emptyLocales, factory.getAvailable(), isAnnotationsDir);
                if (!childless.isEmpty()) {
                    if (VERBOSE)
                        System.out.println(
                                "\t" + destinationFile + "\tRemoving empty locales:" + childless);
                    childless.stream()
                            .forEach(locale -> new File(destinationFile, locale + ".xml").delete());
                }
            }
            return false;
        } else if (factory != null) {
            String file = sourceFile.getName();
            if (!file.endsWith(".xml")) {
                return false;
            }
            String localeId = getLocaleIdFromFileName(file);
            if (FILE_MATCH != null) {
                if (!FILE_MATCH.reset(localeId).matches()) {
                    return false;
                }
            }
            boolean isRoot = localeId.equals(LocaleNames.ROOT);

            CLDRFile cldrFileUnresolved = factory.make(localeId, false);
            CLDRFile cldrFileResolved = factory.make(localeId, true);
            boolean gotOne = false;
            Set<String> toRemove = new TreeSet<>(); // TreeSet just makes debugging easier
            Set<String> toRetain = new TreeSet<>();
            Set<String> toRetainSpecial = new TreeSet<>();
            Output<String> pathWhereFound = new Output<>();
            Output<String> localeWhereFound = new Output<>();

            final boolean specialPathsAreRequired =
                    areSpecialPathsRequired(localeId, sourceFile.toString());

            String debugPath =
                    "//ldml/localeDisplayNames/languages/language[@type=\"en_US\"]"; // "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"power-kilowatt\"]/displayName";
            boolean debugLocale = localeId.equals("pt");

            ImmutableSet<String> sortedPaths =
                    ImmutableSortedSet.copyOf(cldrFileUnresolved); // sort for debugging

            for (String xpath : sortedPaths) {
                if (xpath.startsWith("//ldml/identity")) {
                    continue;
                }
                if (debugPath != null && debugLocale && xpath.startsWith(debugPath)) {
                    int debug = 0;
                }

                String value = cldrFileUnresolved.getStringValue(xpath);
                if (value == null || CldrUtility.INHERITANCE_MARKER.equals(value)) {
                    toRemove.add(xpath);
                    continue;
                }

                // special-case the root values that are only for Survey Tool use

                if (isRoot) {
                    if (AnnotationUtil.pathIsAnnotation(xpath)) {
                        toRemove.add(xpath);
                        continue;
                    }
                }

                if (specialPathsAreRequired && pathIsSpecial(xpath)) {
                    toRetainSpecial.add(xpath);
                }

                // Remove items that are the same as their bailey values.
                // However, two optional parameters change what happens
                // if ADD_SIDEWAYS is true, then we check for paths equal (condidtionally, see the
                // method doc)
                // if ADD_ROOT is true, then we check for the found locale being root

                String bailey =
                        cldrFileResolved.getBaileyValue(xpath, pathWhereFound, localeWhereFound);
                if (value.equals(bailey)
                        && (!ADD_SIDEWAYS
                                || pathEqualsOrIsOkAltVariantOf(
                                        cldrFileResolved,
                                        xpath,
                                        pathWhereFound.value,
                                        localeId,
                                        localeWhereFound.value))
                        && (!ADD_ROOT
                                || (!Objects.equals(XMLSource.ROOT_ID, localeWhereFound.value)
                                        && !Objects.equals(
                                                XMLSource.CODE_FALLBACK_ID,
                                                localeWhereFound.value)))) {
                    toRemove.add(xpath);
                    continue;
                }

                // remove level=comprehensive (under setting)

                if (!INCLUDE_COMPREHENSIVE) {
                    Level coverage = SDI.getCoverageLevel(xpath, localeId);
                    if (coverage == Level.COMPREHENSIVE) {
                        toRemove.add(xpath);
                        continue;
                    }
                }

                // if we got all the way to here, we have a non-empty result

                // check to see if we might need to flesh out logical groups
                // TODO Should be done in the converter tool!!
                if (ADD_LOGICAL_GROUPS && !LogicalGrouping.isOptional(cldrFileResolved, xpath)) {
                    Set<String> paths = LogicalGrouping.getPaths(cldrFileResolved, xpath);
                    if (paths != null && paths.size() > 1) {
                        for (String possiblePath : paths) {
                            // Unclear from API whether we need to do this filtering
                            if (!LogicalGrouping.isOptional(cldrFileResolved, possiblePath)) {
                                toRetain.add(possiblePath);
                            }
                        }
                    }
                }

                // check to see if we might need to flesh out datetime.
                // TODO Should be done in the converter tool!!
                if (ADD_DATETIME && isDateTimePath(xpath)) {
                    toRetain.addAll(dateTimePaths(xpath));
                }

                // past the gauntlet
                gotOne = true;
            }
            if (specialPathsAreRequired) {
                addSpecialPathsIfMissing(toRetainSpecial);
            }

            // we even add empty files, but can delete them back on the directory level.
            try (PrintWriter pw = new PrintWriter(destinationFile)) {
                CLDRFile outCldrFile = cldrFileUnresolved.cloneAsThawed();

                // Remove paths, but pull out the ones to retain
                // example:
                // toRemove == {a b c} // c may have ^^^ value
                // toRetain == {b c d} // d may have ^^^ value

                if (DEBUG) {
                    showIfNonZero(localeId, "removing", toRemove);
                    showIfNonZero(localeId, "retaining", toRetain);
                    showIfNonZero(localeId, "retaining for special paths", toRetainSpecial);
                }
                if (CONSTRAINED_RESTORATION) {
                    toRetain.retainAll(toRemove); // only add paths that were there already
                    // toRetain == {b c}
                    if (DEBUG) {
                        showIfNonZero(localeId, "constrained retaining", toRetain);
                    }
                }
                // add "special" paths even if CONSTRAINED_RESTORATION
                toRetain.addAll(toRetainSpecial);

                boolean changed0 = toRemove.removeAll(toRetain);
                // toRemove == {a}
                if (DEBUG && changed0) {
                    showIfNonZero(localeId, "final removing", toRemove);
                }

                boolean changed = toRetain.removeAll(toRemove);
                // toRetain = {b c d} or if constrained, {b c}
                if (DEBUG && changed) {
                    showIfNonZero(localeId, "final retaining", toRetain);
                }

                outCldrFile.removeAll(toRemove, false);
                if (DEBUG) {
                    for (String xpath : toRemove) {
                        System.out.println(
                                localeId
                                        + ": removing: «"
                                        + cldrFileUnresolved.getStringValue(xpath)
                                        + "», "
                                        + xpath);
                    }
                }

                // now set any null values to bailey values if not present
                for (String xpath : toRetain) {
                    if (debugPath != null
                            && localeId.equals(debugLocale)
                            && xpath.equals(debugPath)) {
                        int debug = 0;
                    }
                    String value = cldrFileResolved.getStringValue(xpath);
                    if (value == null || value.equals(CldrUtility.INHERITANCE_MARKER)) {
                        throw new IllegalArgumentException(
                                localeId + ": " + value + " in value for " + xpath);
                    } else {
                        if (DEBUG) {
                            String oldValue = cldrFileUnresolved.getStringValue(xpath);
                            System.out.println(
                                    "Restoring: «" + oldValue + "» ⇒ «" + value + "»\t" + xpath);
                        }
                        outCldrFile.add(xpath, value);
                    }
                }

                // double-check results
                int count = 0;
                for (String xpath : outCldrFile) {
                    if (debugPath != null
                            && localeId.equals(debugLocale)
                            && xpath.equals(debugPath)) {
                        int debug = 0;
                    }
                    String value = outCldrFile.getStringValue(xpath);
                    if (value == null || value.equals(CldrUtility.INHERITANCE_MARKER)) {
                        throw new IllegalArgumentException(
                                localeId + ": " + value + " in value for " + xpath);
                    }
                }

                outCldrFile.write(pw);
                ++stats.files;
                stats.removed += toRemove.size();
                stats.retained += toRetain.size();
                stats.remaining += count;
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(
                        "Can't copy " + sourceFile + " to " + destinationFile + " — ", e);
            }
            return !gotOne;
        } else {
            if (FILE_MATCH != null) {
                String file = sourceFile.getName();
                int dotPos = file.lastIndexOf('.');
                String baseName = dotPos >= 0 ? file.substring(0, file.length() - dotPos) : file;
                if (!FILE_MATCH.reset(baseName).matches()) {
                    return false;
                }
            }
            // for now, just copy
            ++stats.files;
            copyFiles(sourceFile, destinationFile);
            return false;
        }
    }

    private static String getLocaleIdFromFileName(String file) {
        return file.substring(0, file.length() - 4); // drop ".xml"
    }

    /**
     * Are any "special paths" required to be explicitly included for this locale, in this
     * directory?
     *
     * <p>Currently this requirement applies only to Arabic defaultNumberingSystem, only in
     * common/main
     *
     * @param localeId the locale ID such as "ar_KM"
     * @param directory the string describing the source directory; currently only "common/main" has
     *     special paths
     * @return true if required, else false
     */
    private static boolean areSpecialPathsRequired(String localeId, String directory) {

        return localeIsSpecial(localeId) && directoryIsSpecial(directory);
    }

    private static boolean directoryIsSpecial(String directory) {
        return directory.contains("common/main");
    }

    private static boolean localeIsSpecial(String localeId) {
        return localeId.equals("ar") || (localeId.startsWith("ar_") && !"ar_001".equals(localeId));
    }

    private static final String[] SPECIAL_PATHS =
            new String[] {
                "//ldml/numbers/defaultNumberingSystem",
                "//ldml/numbers/defaultNumberingSystem[@alt=\"latn\"]"
            };
    private static final Set<String> SPECIAL_PATH_SET = new TreeSet<>(Arrays.asList(SPECIAL_PATHS));

    /**
     * Is the given path a "special path" required to be explicitly included?
     *
     * @param xpath the path
     * @return true if this particular path is required, else false
     */
    private static boolean pathIsSpecial(String xpath) {
        return SPECIAL_PATH_SET.contains(xpath);
    }

    private static void addSpecialPathsIfMissing(Set<String> toRetainSpecial) {
        for (String xpath : SPECIAL_PATH_SET) {
            if (!toRetainSpecial.contains(xpath)) {
                toRetainSpecial.add(xpath);
            }
        }
    }

    /**
     * Confirm that a file (in the destination) is present for each "special" locale (in the
     * source(), and that each such destination file contains all the required "special" paths
     *
     * @param sourceDir a directory like ".../common/main"
     * @param destDir a directory
     */
    private static void doubleCheckSpecialPaths(File sourceDir, File destDir) {
        Set<String> sorted = new TreeSet<>();
        sorted.addAll(Arrays.asList(sourceDir.list()));
        Factory factory = Factory.make(destDir.toString(), ".*");
        sorted.forEach(
                file -> {
                    doubleCheckLocale(sourceDir, destDir, file, factory);
                });
    }

    private static void doubleCheckLocale(
            File sourceDir, File destDir, String file, Factory factory) {
        if (!file.endsWith(".xml")) {
            return;
        }
        String localeId = getLocaleIdFromFileName(file);
        if (!localeIsSpecial(localeId)) {
            return;
        }
        File destFile = new File(destDir, file);
        if (!destFile.exists()) {
            throw new InternalCldrException("doubleCheckLocale FILE NOT FOUND: " + destFile);
        }
        // Note: factory.make will fail here unless ../common/dtd/ldml.dtd exists in relation to the
        // destination folder
        CLDRFile cldrFileUnresolved = factory.make(localeId, false);
        for (String xpath : SPECIAL_PATH_SET) {
            String value = cldrFileUnresolved.getStringValue(xpath);
            if (value == null) {
                throw new InternalCldrException(
                        "Locale " + localeId + " missing required special path " + xpath);
            }
            if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                throw new InternalCldrException(
                        "Locale "
                                + localeId
                                + " has INHERITANCE_MARKER for required special path "
                                + xpath);
            }
        }
    }

    private static void showIfNonZero(String localeId, String title, Set<String> toRemove) {
        if (toRemove.size() != 0) {
            System.out.println(localeId + ": " + title + ": " + toRemove.size());
        }
    }

    /**
     * Exceptions for generating production data, because the results would not pass
     * CompareResolved.
     */
    static final Multimap<String, String> LOCALE_TO_PATH_EXCEPTIONS =
            ImmutableListMultimap.<String, String>builder()
                    .put(
                            "oc_ES",
                            "//ldml/localeDisplayNames/territories/territory[@type=\"HK\"][@alt=\"short\"]")
                    .put(
                            "zh_Hant_MO",
                            "//ldml/localeDisplayNames/languages/language[@type=\"yue\"][@alt=\"menu\"]")
                    .put(
                            "zh_Hant_MO",
                            "//ldml/localeDisplayNames/territories/territory[@type=\"CI\"][@alt=\"variant\"]")
                    .put(
                            "zh_Hant_HK",
                            "//ldml/localeDisplayNames/languages/language[@type=\"yue\"][@alt=\"menu\"]")
                    .put(
                            "zh_Hant_HK",
                            "//ldml/localeDisplayNames/territories/territory[@type=\"CI\"][@alt=\"variant\"]")
                    .put(
                            "ru_BY",
                            "//ldml/numbers/currencies/currency[@type=\"RUR\"]/symbol[@alt=\"narrow\"]")
                    .put(
                            "oc_ES",
                            "//ldml/localeDisplayNames/territories/territory[@type=\"HK\"][@alt=\"short\"]")
                    .put(
                            "el_POLYTON",
                            "//ldml/localeDisplayNames/territories/territory[@type=\"CI\"][@alt=\"variant\"]")
                    .put(
                            "be_TARASK",
                            "//ldml/localeDisplayNames/languages/language[@type=\"az\"][@alt=\"short\"]")
                    .build();

    /**
     * Check if a path is equal, or if it is a suitable alt variant If it returns true, the value
     * will be removed; false will retain it.
     */
    private static boolean pathEqualsOrIsOkAltVariantOf(
            CLDRFile cldrFileResolved,
            String desiredPath,
            String foundPath,
            String localeId,
            String foundLocaleId) {
        if (LOCALE_TO_PATH_EXCEPTIONS.containsEntry(localeId, desiredPath)) {
            return false;
        }
        /*
         * Protect against bad case, such as:
         *
         * pt      //ldml/localeDisplayNames/languages/language[@type="en_US"]                  ↑↑↑ (= inglês americano)
         * pt      //ldml/localeDisplayNames/languages/language[@type="en_US"][@alt="short"]    inglês (EUA)
         *
         * pt_AO   //ldml/localeDisplayNames/languages/language[@type="en_US"][@alt="short"]    inglês (EUA)
         *
         * When processing pt, its short value disappears, because its value = lateral inherited (constructed) value from pt
         * When processing pt_AO, its short value is also removed, because it is the same as the pt
         * But then when it is constructed, its value =
         *
         */
        if (desiredPath.equals(foundPath)) {
            // TODO for a full fix, we need to check that the foundLocaleId/foundPath will not
            // disappear when it is processed.
            // For now, we are using the LOCALE_TO_PATH_EXCEPTIONS.
            return true;
        }
        if (!foundLocaleId.equals(
                localeId)) { // extra condition on alt values; has to be found in the same locale
            return false;
        }
        if (desiredPath.contains("type=\"en_GB\"") && desiredPath.contains("alt=")) {
            int debug = 0;
        }
        if (foundPath == null || foundPath.equals(GlossonymConstructor.PSEUDO_PATH)) {
            if (!LocaleIDParser.isL1(localeId)) {
                return true;
            }
            // We can do this, because the bailey value has already been checked.
            // Since it isn't null, a null or PSEUDO_PATH indicates a constructed alt value.
            return false;
        }
        XPathParts desiredPathParts = XPathParts.getFrozenInstance(desiredPath);
        XPathParts foundPathParts = XPathParts.getFrozenInstance(foundPath);
        if (desiredPathParts.size() != foundPathParts.size()) {
            return false;
        }
        for (int e = 0; e < desiredPathParts.size(); ++e) {
            String element1 = desiredPathParts.getElement(e);
            String element2 = foundPathParts.getElement(e);
            if (!element1.equals(element2)) {
                return false;
            }
            Map<String, String> attr1 = desiredPathParts.getAttributes(e);
            Map<String, String> attr2 = foundPathParts.getAttributes(e);
            if (attr1.equals(attr2)) {
                continue;
            }
            Set<String> keys1 = attr1.keySet();
            Set<String> keys2 = attr2.keySet();
            for (String attr : Sets.union(keys1, keys2)) {
                if (attr.equals("alt")) {
                    continue;
                }
                if (!Objects.equals(attr1.get(attr), attr2.get(attr))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isDateTimePath(String xpath) {
        return xpath.startsWith("//ldml/dates/calendars/calendar")
                && xpath.contains("FormatLength[@type=");
    }

    /**
     * generate full dateTimePaths from any element
     * //ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type=".*"]/dateFormat[@type="standard"]/pattern[@type="standard"]
     * //ldml/dates/calendars/calendar[@type="gregorian"]/timeFormats/timeFormatLength[@type=".*"]/timeFormat[@type="standard"]/pattern[@type="standard"]
     * //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/dateTimeFormatLength[@type=".*"]/dateTimeFormat[@type="standard"]/pattern[@type="standard"]
     */
    private static Set<String> dateTimePaths(String xpath) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String prefix = xpath.substring(0, xpath.indexOf(']') + 2); // get after ]/
        for (String type : Arrays.asList("date", "time", "dateTime")) {
            String pattern =
                    prefix
                            + "$XFormats/$XFormatLength[@type=\"$Y\"]/$XFormat[@type=\"standard\"]/pattern[@type=\"standard\"]"
                                    .replace("$X", type);
            for (String width : Arrays.asList("full", "long", "medium", "short")) {
                result.add(pattern.replace("$Y", width));
            }
        }
        return result;
    }

    private static Set<String> getChildless(
            Set<String> emptyLocales, Set<String> available, boolean isAnnotationsDir) {
        // first build the parent2child map
        Multimap<String, String> parent2child = HashMultimap.create();
        for (String locale : available) {
            String parent = LocaleIDParser.getParent(locale);
            if (parent != null) {
                parent2child.put(parent, locale);
            }
            if (isAnnotationsDir) {
                String simpleParent =
                        LocaleIDParser.getParent(locale, ParentLocaleComponent.collations);
                if (simpleParent != null && (parent == null || simpleParent != parent)) {
                    parent2child.put(simpleParent, locale);
                }
            }
        }

        // now cycle through the empties
        Set<String> result = new HashSet<>();
        for (String empty : emptyLocales) {
            if (allChildrenAreEmpty(empty, emptyLocales, parent2child)) {
                result.add(empty);
            }
        }
        return result;
    }

    /**
     * Recursively checks that all children are empty (including that there are no children)
     *
     * @param locale
     * @param emptyLocales
     * @param parent2child
     * @return
     */
    private static boolean allChildrenAreEmpty(
            String locale, Set<String> emptyLocales, Multimap<String, String> parent2child) {

        Collection<String> children = parent2child.get(locale);
        for (String child : children) {
            if (!emptyLocales.contains(child)) {
                return false;
            }
            if (!allChildrenAreEmpty(child, emptyLocales, parent2child)) {
                return false;
            }
        }
        return true;
    }

    private static void copyFiles(File sourceFile, File destinationFile) {
        try {
            Files.copy(sourceFile, destinationFile);
        } catch (IOException e) {
            System.err.println("Can't copy " + sourceFile + " to " + destinationFile + " — " + e);
        }
    }
}
