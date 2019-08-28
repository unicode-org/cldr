package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

public class GenerateProductionData {
    static final String SOURCE_DIR = CLDRPaths.COMMON_DIRECTORY;
    static final String DEST_DIR = CLDRPaths.AUX_DIRECTORY + "production/common";
    static final Set<String> NON_XML = ImmutableSet.of("dtd", "properties", "testData", "uca");
    static final Set<String> COPY_ANYWAY = ImmutableSet.of("casing", "collation"); // don't want to "clean up", makes format difficult to use

    public static void main(String[] args) {
        // get directories
        // assume seed is already handled by CLDR-5169

        for (DtdType type : DtdType.values()) {
            boolean isLdmlDtdType = type == DtdType.ldml;

            // bit of a hack, using the ldmlICU — otherwise unused! — to get the nonXML files.
            Set<String> directories = (type == DtdType.ldmlICU) ? NON_XML : type.directories;

            for (String dir : directories) {
                File sourceDir = new File(SOURCE_DIR, dir);
                File destinationDir = new File(DEST_DIR, dir);
                copyFiles(sourceDir, destinationDir, null, isLdmlDtdType);
            }
        }
    }

    private static void copyFiles(File sourceFile, File destinationFile, Factory factory, boolean isLdmlDtdType) {
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
            factory = isLdmlDtdType ? Factory.make(sourceFile.toString(), ".*") : null;

            for (String file : sorted) {
                File sourceFile2 = new File(sourceFile, file);
                File destinationFile2 = new File(destinationFile, file);
                System.out.println("\t" + file);
                copyFiles(sourceFile2, destinationFile2, factory, isLdmlDtdType);
            }
        } else if (factory != null) {
            String file = sourceFile.getName();
            if (!file.endsWith(".xml")) {
                return;
            }
            String localeId = file.substring(0, file.length()-4);
            boolean isRoot = localeId.equals("root");
            CLDRFile cldrFileUnresolved = factory.make(localeId, false);
            CLDRFile cldrFile = factory.make(localeId, true);
            boolean gotOne = false;
            Set<String> toRemove = new HashSet<>();
            for (String xpath : cldrFileUnresolved) {
                String value = cldrFile.getStringValue(xpath);
                if (value == null) {
                    continue;
                }
                if (isRoot) {
                    if (xpath.startsWith("//ldml/annotations/annotation")) {
                        continue;
                    }
                }
                String bailey = cldrFile.getConstructedBaileyValue(xpath, null, null);
                if (value.equals(bailey)) {
                    toRemove.add(xpath);
                }
            }
            // TODO if file is empty AND not in main, skip
            try (PrintWriter pw = new PrintWriter(destinationFile)) {
                CLDRFile outCldrFile = cldrFileUnresolved.cloneAsThawed();
                outCldrFile.removeAll(toRemove, false);
                outCldrFile.write(pw);
            } catch (FileNotFoundException e) {
                System.err.println("Can't copy " + sourceFile + " to " + destinationFile + " — " + e);
            }
        } else {
            // for now, just copy
            copyFiles(sourceFile, destinationFile);
        }
    }

    private static void copyFiles(File sourceFile, File destinationFile) {
        try {
            Files.copy(sourceFile, destinationFile);
        } catch (IOException e) {
            System.err.println("Can't copy " + sourceFile + " to " + destinationFile + " — " + e);
        }
    }
}
