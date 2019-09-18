package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;

import com.ibm.icu.dev.util.CollectionUtilities;

public class CompareEn {

    enum MyOptions {
        uplevel(new Params().setHelp("move elements from en_GB into en_oo1")),
        verbose(new Params().setHelp("verbose output")),
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

    static Factory mainFactory = CLDRConfig.getInstance().getCldrFactory();
    static Factory annotationsFactory = CLDRConfig.getInstance().getAnnotationsFactory();
    private static boolean VERBOSE = false;

    public static void main(String[] args) throws IOException {
        MyOptions.parse(args, true);
        VERBOSE = MyOptions.verbose.option.doesOccur();
        if (MyOptions.uplevel.option.doesOccur()) {
            uplevelEnGB();
        } else {
            writeComparison();
        }
    }

    private static void uplevelEnGB() throws IOException {
        System.out.println("Copying values from en_GB to en_001");

        for (Factory factory : Arrays.asList(mainFactory, annotationsFactory)) {
            String outDir = factory.getSourceDirectory(); // CLDRPaths.GEN_DIRECTORY + "uplevel/" + new File(factory.getSourceDirectory()).getName();
            CLDRFile en_001 = factory.make("en_001", false);
            CLDRFile en_001R = factory.make("en_001", true);
            CLDRFile en_001Out = en_001.cloneAsThawed();

            CLDRFile en_GB = factory.make("en_GB", false);
            CLDRFile en_GBR = factory.make("en_GB", true);

            // walk through all the new paths and values to check them.

            Set<String> paths = CollectionUtilities.addAll(en_GB.iterator(), new TreeSet<>());

            for (String path : paths) {
                if (path.startsWith("//ldml/identity")) {
                    continue; 
                }
             // skip certain paths
                if (path.startsWith("//ldml/dates/timeZoneNames/") && path.contains("/short/")) {
                    if (VERBOSE) {
                        System.out.println("Skipping\t" + path);
                    }
                    continue;
                }
                String valueGBR = en_GBR.getStringValue(path);
                if (valueGBR == null || CldrUtility.INHERITANCE_MARKER.equals(valueGBR)) {
                    continue;
                }

                String value001R = en_001R.getStringValue(path);
                if (CldrUtility.INHERITANCE_MARKER.equals(value001R)) {
                    value001R = null;
                }

                if (Objects.equals(valueGBR, value001R)) {
                    continue;
                }

                // replace the en_001 value
                String fullPath = en_GBR.getFullXPath(path);
                if (VERBOSE) {
                    System.out.println("Changing «" + value001R + "» to «" + valueGBR + "» in\t" + path);
                }
                en_001Out.add(fullPath, valueGBR);
            }

            try (PrintWriter out = FileUtilities.openUTF8Writer(outDir, "en_001.xml")) {
                en_001Out.write(out);
            }
        }
    }

    private static void writeComparison() throws IOException {
        System.out.println("Writing to: " + CLDRPaths.GEN_DIRECTORY + "comparison" + "en.txt");

        try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "comparison", "en.txt")) {
            out.println("From CompareEn.java");
            out.println("Proposed Disposition\ten\ten_001\ten_GB\tPath");
            for (Factory factory : Arrays.asList(mainFactory, annotationsFactory)) {
                CLDRFile en = factory.make("en", false);
                CLDRFile en_001 = factory.make("en_001", false);
                CLDRFile en_GB = factory.make("en_GB", false);

                CLDRFile enR = factory.make("en", true);
                CLDRFile en_001R = factory.make("en_001", true);
                CLDRFile en_GBR = factory.make("en_GB", true);

                // walk through all the new paths and values to check them.

                Set<String> paths = CollectionUtilities.addAll(en_GB.iterator(), new TreeSet<>());
                paths = CollectionUtilities.addAll(en_001.iterator(), paths);

                for (String path : paths) {
                    if (path.startsWith("//ldml/identity")) {
                        continue;
                    }
                    String value001R = en_001R.getStringValue(path);
                    if (CldrUtility.INHERITANCE_MARKER.equals(value001R)) {
                        value001R = null;
                    }
                    String valueGBR = en_GBR.getStringValue(path);
                    if (CldrUtility.INHERITANCE_MARKER.equals(valueGBR)) {
                        valueGBR = null;
                    }
                    String valueR = enR.getStringValue(path);

                    // drop the cases that will disappear with minimization

                    if (Objects.equals(value001R, valueGBR)) {
                        continue;
                    }
                    out.println(
                        "\t" + valueR
                        + "\t" + en_001.getStringValue(path)
                        + "\t" + en_GB.getStringValue(path)
                        + "\t" + path);
                }
            }
        }
    }
}
