package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.ibm.icu.util.Output;

public class GenerateProductionData {
    static String SOURCE_COMMON_DIR = null;
    static String DEST_COMMON_DIR = null;
    static boolean VERBOSE = false;
    static boolean ADD_LOGICAL_GROUPS = false;
    static boolean ADD_DATETIME = false;
    static boolean SIDEWAYS = false;
    static boolean ROOT = false;
    static boolean ONLY_MODERN = false;

    static final Set<String> NON_XML = ImmutableSet.of("dtd", "properties", "testData", "uca");
    static final Set<String> COPY_ANYWAY = ImmutableSet.of("casing", "collation"); // don't want to "clean up", makes format difficult to use
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    enum MyOptions {
        sourceDirectory(new Params()
            .setHelp("source common directory")
            .setDefault(CLDRPaths.COMMON_DIRECTORY)
            .setMatch(".*")),
        destinationDirectory(new Params()
            .setHelp("destination common directory")
            .setDefault(CLDRPaths.AUX_DIRECTORY + "production/common")
            .setMatch(".*")),
        logicalGroups(new Params()
            .setHelp("flesh out logical groups")
            .setDefault("true")
            .setMatch(".*")),
        time(new Params()
            .setHelp("flesh out datetime")
            .setDefault("true")
            .setMatch(".*")),
        Sideways(new Params()
            .setHelp("minimize against sideways inheritance")
            .setDefault("false")
            .setMatch(".*")),
        root(new Params()
            .setHelp("minimize for root and code-fallback (false is only against explicit locales like fr)")
            .setDefault("false")
            .setMatch(".*")),
        onlyModern(new Params()
            .setHelp("exclude comprehensive paths")
            .setDefault("false")
            .setMatch(".*")),
        verbose(new Params()
            .setHelp("verbose debugging messages")), 
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
        VERBOSE = "true".equalsIgnoreCase(MyOptions.verbose.option.getValue());
        ADD_LOGICAL_GROUPS = "true".equalsIgnoreCase(MyOptions.logicalGroups.option.getValue());
        ADD_DATETIME = "true".equalsIgnoreCase(MyOptions.time.option.getValue());
        SIDEWAYS = "true".equalsIgnoreCase(MyOptions.Sideways.option.getValue());
        ROOT = "true".equalsIgnoreCase(MyOptions.root.option.getValue());
        ONLY_MODERN = "true".equalsIgnoreCase(MyOptions.onlyModern.option.getValue());

        // get directories

        for (DtdType type : DtdType.values()) {
            boolean isLdmlDtdType = type == DtdType.ldml;

            // bit of a hack, using the ldmlICU — otherwise unused! — to get the nonXML files.
            Set<String> directories = (type == DtdType.ldmlICU) ? NON_XML : type.directories;

            for (String dir : directories) {
                File sourceDir = new File(SOURCE_COMMON_DIR, dir);
                File destinationDir = new File(DEST_COMMON_DIR, dir);
                copyFilesAndReturnIsEmpty(sourceDir, destinationDir, null, isLdmlDtdType);
            }
        }
    }

    /**
     * Copy files in directories, recursively.
     * @param sourceFile
     * @param destinationFile
     * @param factory
     * @param isLdmlDtdType
     * @param hasChildren
     * @return true if the file is an ldml file with empty content.
     */
    private static boolean copyFilesAndReturnIsEmpty(File sourceFile, File destinationFile, Factory factory, boolean isLdmlDtdType) {
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

            Set<String> emptyLocales = new HashSet<>();
            for (String file : sorted) {
                File sourceFile2 = new File(sourceFile, file);
                File destinationFile2 = new File(destinationFile, file);
                if (VERBOSE) System.out.println("\t" + file);

                // special step to just copy certain files like main/root.xml file
                Factory currFactory = factory;
                if (isMainDir) {
                    if (file.equals("root.xml")) {
                        currFactory = null;
                    }
                } else if (isRbnfDir) {
                    currFactory = null;
                }

                // when the currFactory is null, we just copy files as-is
                boolean isEmpty = copyFilesAndReturnIsEmpty(sourceFile2, destinationFile2, currFactory, isLdmlDtdType);
                if (isEmpty) { // only happens for ldml
                    emptyLocales.add(file.substring(0,file.length()-4)); // remove .xml for localeId
                }
            }
            // if there are empty ldml files, AND we aren't in /main/, 
            // then remove any without children
            if (!emptyLocales.isEmpty() && !sourceFile.getName().equals("main")) {
                Set<String> childless = getChildless(emptyLocales, factory.getAvailable());
                if (!childless.isEmpty()) {
                    if (VERBOSE) System.out.println("\t" + destinationFile + "\tRemoving empty locales:" + childless);
                    childless.stream().forEach(locale -> new File(destinationFile, locale + ".xml").delete());
                }
            }
            return false;
        } else if (factory != null) {
            String file = sourceFile.getName();
            if (!file.endsWith(".xml")) {
                return false;
            }
            String localeId = file.substring(0, file.length()-4);
            boolean isRoot = localeId.equals("root");
            CLDRFile cldrFileUnresolved = factory.make(localeId, false);
            CLDRFile cldrFileResolved = factory.make(localeId, true);
            boolean gotOne = false;
            Set<String> toRemove = new HashSet<>();
            Set<String> toRetain = new HashSet<>();
            Output<String> pathWhereFound = new Output<>();
            Output<String> localeWhereFound = new Output<>();

            String debugPath = null; // "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"power-kilowatt\"]/displayName";
            String debugLocale = "af";

            for (String xpath : cldrFileUnresolved) {
                if (xpath.startsWith("//ldml/identity")) {
                    continue;
                }
                if (debugPath != null && localeId.equals(debugLocale) && xpath.equals(debugPath)) {
                    int debug = 0;
                }

                String value = cldrFileUnresolved.getStringValue(xpath);
                if (value == null || CldrUtility.INHERITANCE_MARKER.equals(value)) {
                    toRemove.add(xpath);
                    continue;
                }

                // special-case the root values that are only for Survey Tool use

                if (isRoot) {
                    if (xpath.startsWith("//ldml/annotations/annotation")) {
                        toRemove.add(xpath);
                        continue;
                    }
                }

                // remove items that are the same as their bailey values. This also catches Inheritance Marker

                String bailey = cldrFileResolved.getConstructedBaileyValue(xpath, pathWhereFound, localeWhereFound);
                if (value.equals(bailey) 
                    && (SIDEWAYS 
                        || pathEqualsOrIsAltVariantOf(xpath, pathWhereFound.value))
                    && (ROOT 
                        || (!Objects.equals(XMLSource.ROOT_ID, localeWhereFound.value) 
                            && !Objects.equals(XMLSource.CODE_FALLBACK_ID, localeWhereFound.value)))) {
                    toRemove.add(xpath);
                    continue;
                }

                // remove level=comprehensive (under setting)

                if (ONLY_MODERN) {
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
                    if (paths.size() > 1) {
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


            // we even add empty files, but can delete them back on the directory level.
            try (PrintWriter pw = new PrintWriter(destinationFile)) {
                CLDRFile outCldrFile = cldrFileUnresolved.cloneAsThawed();

                // pull out the ones to retain

                toRemove.removeAll(toRetain);
                toRetain.removeAll(toRemove);
                outCldrFile.removeAll(toRemove, false);


                // now set any null values to bailey values if not present
                for (String xpath : toRetain) {
                    if (debugPath != null && localeId.equals(debugLocale) && xpath.equals(debugPath)) {
                        int debug = 0;
                    }
                    String value = cldrFileResolved.getStringValue(xpath);
                    if (value == null || value.equals(CldrUtility.INHERITANCE_MARKER)) {
                        throw new IllegalArgumentException(localeId + ": " + value + " in value for " + xpath);
                    } else {
                        outCldrFile.add(xpath, value);
                    }
                }

                // double-check results
                for (String xpath : outCldrFile) {
                    if (debugPath != null && localeId.equals(debugLocale) && xpath.equals(debugPath)) {
                        int debug = 0;
                    }
                    String value = outCldrFile.getStringValue(xpath);
                    if (value == null || value.equals(CldrUtility.INHERITANCE_MARKER)) {
                        throw new IllegalArgumentException(localeId + ": " + value + " in value for " + xpath);
                    }
                }

                outCldrFile.write(pw);
            } catch (FileNotFoundException e) {
                System.err.println("Can't copy " + sourceFile + " to " + destinationFile + " — " + e);
            }
            return !gotOne;
        } else {
            // for now, just copy
            copyFiles(sourceFile, destinationFile);
            return false;
        }
    }

    private static boolean pathEqualsOrIsAltVariantOf(String desiredPath, String foundPath) {
        if (desiredPath.equals(foundPath)) {
            return true;
        }
        if (desiredPath.contains("type=\"en_GB\"") && desiredPath.contains("alt=")) {
            int debug = 0;
        }
        if (foundPath == null) { 
            // We can do this, because the bailey value has already been checked
            // Since it isn't null, a null indicates a constructed alt value
            return true;
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

    /** generate full dateTimePaths from any element
    //ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type=".*"]/dateFormat[@type="standard"]/pattern[@type="standard"]
    //ldml/dates/calendars/calendar[@type="gregorian"]/timeFormats/timeFormatLength[@type=".*"]/timeFormat[@type="standard"]/pattern[@type="standard"]
    //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/dateTimeFormatLength[@type=".*"]/dateTimeFormat[@type="standard"]/pattern[@type="standard"]
     */
    private static Set<String> dateTimePaths(String xpath) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String prefix = xpath.substring(0,xpath.indexOf(']') + 2); // get after ]/
        for (String type : Arrays.asList("date", "time", "dateTime")) {
            String pattern = prefix + "$XFormats/$XFormatLength[@type=\"$Y\"]/$XFormat[@type=\"standard\"]/pattern[@type=\"standard\"]".replace("$X", type);
            for (String width : Arrays.asList("full", "long", "medium", "short")) {
                result.add(pattern.replace("$Y", width));
            }
        }
        return result;
    }

    private static Set<String> getChildless(Set<String> emptyLocales, Set<String> available) {
        // first build the parent2child map
        Multimap<String,String> parent2child = HashMultimap.create();
        for (String locale : available) {
            String parent = LocaleIDParser.getParent(locale);
            if (parent != null) {
                parent2child.put(parent, locale); 
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
     * @param name
     * @param emptyLocales 
     * @param parent2child
     * @return
     */
    private static boolean allChildrenAreEmpty(
        String locale, 
        Set<String> emptyLocales, 
        Multimap<String, String> parent2child) {

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
