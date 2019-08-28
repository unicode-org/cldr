package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

public class GenerateProductionData {
    static final String SOURCE_DIR = CLDRPaths.COMMON_DIRECTORY;
    static String DEST_DIR = CLDRPaths.AUX_DIRECTORY + "production/common";
    static final Set<String> NON_XML = ImmutableSet.of("dtd", "properties", "testData", "uca");
    static final Set<String> COPY_ANYWAY = ImmutableSet.of("casing", "collation"); // don't want to "clean up", makes format difficult to use
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    static boolean ONLY_MODERN = false;

    public static void main(String[] args) {
        // TODO rbnf and segments don't have modern coverage; fix there.
        ONLY_MODERN = Arrays.asList(args).contains("modern");
        if (ONLY_MODERN) {
            DEST_DIR = CLDRPaths.AUX_DIRECTORY + "production/modern/common";
        }
        // get directories
        // assume seed is already handled by CLDR-5169

        for (DtdType type : DtdType.values()) {
            boolean isLdmlDtdType = type == DtdType.ldml;

            // bit of a hack, using the ldmlICU — otherwise unused! — to get the nonXML files.
            Set<String> directories = (type == DtdType.ldmlICU) ? NON_XML : type.directories;

            for (String dir : directories) {
                File sourceDir = new File(SOURCE_DIR, dir);
                File destinationDir = new File(DEST_DIR, dir);
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
                factory = Factory.make(sourceFile.toString(), ".*");
            }

            Set<String> emptyLocales = new HashSet<>();
            for (String file : sorted) {
                File sourceFile2 = new File(sourceFile, file);
                File destinationFile2 = new File(destinationFile, file);
                System.out.println("\t" + file);
                boolean isEmpty = copyFilesAndReturnIsEmpty(sourceFile2, destinationFile2, factory, isLdmlDtdType);
                if (isEmpty) { // only happens for ldmln
                    emptyLocales.add(file.substring(0,file.length()-4)); // remove .xml for localeId
                }
            }
            // if there are empty ldml files, AND we aren't in /main/, 
            // then remove any without children
            if (!emptyLocales.isEmpty() && !sourceFile.getName().equals("main")) {
                Set<String> childless = getChildless(emptyLocales, factory.getAvailable());
                if (!childless.isEmpty()) {
                    System.out.println("\t" + destinationFile
                        + "\tRemoving empty locales:" + childless);
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
            CLDRFile cldrFile = factory.make(localeId, true);
            boolean gotOne = false;
            Set<String> toRemove = new HashSet<>();
            for (String xpath : cldrFileUnresolved) {
                if (xpath.startsWith("//ldml/identity")) {
                    continue;
                }

                String value = cldrFile.getStringValue(xpath);
                if (value == null) {
                    toRemove.add(xpath);
                    continue;
                }

                // special case root values that are only for Survey Tool use

                if (isRoot) {
                    if (xpath.startsWith("//ldml/annotations/annotation")) {
                        toRemove.add(xpath);
                        continue;
                    }
                }

                // remove items that are the same as their bailey values. This also catches Inheritance Marker

                String bailey = cldrFile.getConstructedBaileyValue(xpath, null, null);
                if (value.equals(bailey)) {
                    toRemove.add(xpath);
                    continue;
                }

                if (ONLY_MODERN) {
                    Level coverage = SDI.getCoverageLevel(xpath, localeId);
                    if (coverage == Level.COMPREHENSIVE) {
                        toRemove.add(xpath);
                        continue;
                    }
                }

                // if we got all the way to here, we have a non-empty result
                gotOne = true;
            }
            // TODO This loop keeps some unnecessary files, since we don't know until
            // after we've processed the children whether a parent has some non-empty children.
            // Solution is to keep a list of files that were empty, and process once we return
            // to the directory above.
            try (PrintWriter pw = new PrintWriter(destinationFile)) {
                CLDRFile outCldrFile = cldrFileUnresolved.cloneAsThawed();
                outCldrFile.removeAll(toRemove, false);
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
