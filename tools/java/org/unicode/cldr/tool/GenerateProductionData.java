package org.unicode.cldr.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;

import com.google.common.io.Files;

public class GenerateProductionData {
    static final String SOURCE_DIR = CLDRPaths.COMMON_DIRECTORY;
    static final String DEST_DIR = CLDRPaths.AUX_DIRECTORY + "production/";

    public static void main(String[] args) {
        // get directories
        // assume seed is already handled by CLDR-5169
        for (DtdType type : DtdType.values()) {
            for (String dir : type.directories) {
                File sourceDir = new File(SOURCE_DIR, dir);
                Factory factory = type == DtdType.ldml ? Factory.make(sourceDir.toString(), ".*") : null;

                File destinationDir = new File(DEST_DIR, dir);
                destinationDir.mkdirs();
                for (String file : sourceDir.list()) {
                    if (!file.endsWith(".xml")) {
                        continue;
                    }
                    File sourceFile = new File(sourceDir, file);
                    File destinationFile = new File(destinationDir, file);

                    if (type == DtdType.ldml) {
                        String localeId = file.substring(0, file.length()-4);
                        CLDRFile cldrFile = factory.make(localeId, true);
                        CLDRFile outCldrFile = new CLDRFile(new SimpleXMLSource(localeId));
                        for (String xpath : cldrFile) {
                            String value = cldrFile.getStringValue(xpath);
                            if (value == null) {
                                continue;
                            }
                            String bailey = cldrFile.getConstructedBaileyValue(xpath, null, null);
                            if (value.equals(bailey)) {
                                continue;
                            }
                            String fullXpath = cldrFile.getFullXPath(xpath);
                            outCldrFile.add(fullXpath, value);
                        }
                        try (PrintWriter pw = new PrintWriter(destinationFile)) {
                            outCldrFile.write(pw);
                        } catch (FileNotFoundException e) {
                            System.err.println("Can't copy " + sourceFile + " to " + destinationFile + " — " + e);
                        }
                    } else {
                        // for now, just copy
                        try {
                            Files.copy(sourceFile, destinationFile);
                        } catch (IOException e) {
                            System.err.println("Can't copy " + sourceFile + " to " + destinationFile + " — " + e);
                        }
                    }
                }
            }
        }
        // TODO copy other files
    }
}
