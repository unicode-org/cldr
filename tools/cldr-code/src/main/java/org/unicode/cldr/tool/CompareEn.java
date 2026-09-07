package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;

@CLDRTool(
        alias = "compare-en",
        description = "BRS: compare en_GB and en_001",
        url = "https://cldr.unicode.org/development/brs-copy-en_gb-to-en_001")
public class CompareEn {

    enum MyOptions {
        uplevel(new Params().setHelp("move elements from en_GB into en_001")),
        verbose(new Params().setHelp("verbose output")),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static final Options myOptions = new Options();

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

    /** In addition to rule-based skipping of paths, skip any paths that are found in this file */
    private static final String skipFileName = "CompareEnSkipPaths.txt";

    private static final List<String> skipPaths = readSkipPaths(skipFileName);

    /**
     * For debugging and revising this code, in verbose mode, report any paths that are specified in
     * skipFileName but not actually skipped on that basis (they might not have been found, or they
     * might have been skipped according to a rule)
     */
    private static List<String> verboseConfirmSkipped = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        MyOptions.parse(args, true);
        VERBOSE = MyOptions.verbose.option.doesOccur();
        if (MyOptions.uplevel.option.doesOccur()) {
            uplevelEnGB();
        } else {
            writeComparison();
        }
        if (VERBOSE) {
            for (String path : skipPaths) {
                if (!verboseConfirmSkipped.contains(path)) {
                    System.out.println("Path not skipped: " + path);
                }
            }
        }
    }

    private static void uplevelEnGB() throws IOException {
        System.out.println("Copying values from en_GB to en_001");
        Values values = new Values();
        int copiedCount = 0;

        for (Factory factory : Arrays.asList(mainFactory, annotationsFactory)) {
            String outDir =
                    factory.getSourceDirectory(); // CLDRPaths.GEN_DIRECTORY + "uplevel/" + new
            // File(factory.getSourceDirectory()).getName();
            CLDRFile en_001 = factory.make("en_001", false);
            CLDRFile en_001R = factory.make("en_001", true);
            CLDRFile en_001Out = en_001.cloneAsThawed();

            CLDRFile en_GB = factory.make("en_GB", false);
            CLDRFile en_GBR = factory.make("en_GB", true);

            // walk through all the new paths and values to check them.

            TreeSet<String> paths = new TreeSet<>();
            en_GB.forEach(paths::add);

            for (String path : paths) {
                // skip certain paths
                if (failsFilter(path, en_GBR, en_001R, values) != FilterStatus.accept) {
                    continue;
                }

                // replace the en_001 value
                String fullPath = en_GBR.getFullXPath(path);
                if (VERBOSE) {
                    System.out.println(
                            "Changing «"
                                    + values.value001R
                                    + "» to «"
                                    + values.valueGBR
                                    + "» in\t"
                                    + path);
                }
                en_001Out.add(fullPath, values.valueGBR);
                ++copiedCount;
            }

            try (PrintWriter out = FileUtilities.openUTF8Writer(outDir, "en_001.xml")) {
                en_001Out.write(out);
            }
        }
        System.out.println("Copied " + copiedCount + " values from en_GB to en_001");
    }

    private static List<String> readSkipPaths(String fileName) {
        ArrayList<String> list = new ArrayList<>();
        int count = 0;
        try (BufferedReader br = CldrUtility.getUTF8Data(fileName)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    list.add(line);
                    ++count;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Read " + count + " paths from " + fileName);
        return list;
    }

    static final class Values {
        String valueGBR;
        String value001R;
    }

    enum FilterStatus {
        /** don't make the change */
        skip,
        /** don't make the change, but note in the comparison file */
        skipButNote,
        /** make the change */
        accept
    }

    /**
     * Check whether we should skip or not. If we accept the change, then values contains the
     * values.
     *
     * @param path
     * @param en_GBR
     * @param en_001R
     * @param values
     * @return
     */
    private static FilterStatus failsFilter(
            String path, CLDRFile en_GBR, CLDRFile en_001R, Values values) {
        if (path.startsWith("//ldml/identity")) {
            return FilterStatus.skip;
        }
        if (path.startsWith("//ldml/dates/timeZoneNames/") && path.contains("/short/")
                || path.contains("/datetimeSkeleton")
                || path.contains("/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]")) {
            if (VERBOSE) {
                System.out.println("Skipping (per filter)\t" + path);
            }
            return FilterStatus.skipButNote;
        }
        values.valueGBR = en_GBR.getStringValue(path);
        if (values.valueGBR == null || CldrUtility.INHERITANCE_MARKER.equals(values.valueGBR)) {
            return FilterStatus.skip;
        }

        values.value001R = en_001R.getStringValue(path);
        if (CldrUtility.INHERITANCE_MARKER.equals(values.value001R)) {
            values.value001R = null;
        }

        if (Objects.equals(values.valueGBR, values.value001R)) {
            return FilterStatus.skip;
        }
        if (skipPaths.contains(path)) {
            if (VERBOSE) {
                System.out.println("Skipping (per " + skipFileName + ")\t" + path);
                verboseConfirmSkipped.add(path);
            }
            return FilterStatus.skipButNote;
        }
        return FilterStatus.accept;
    }

    private static void writeComparison() throws IOException {
        System.out.println("Writing to: " + CLDRPaths.GEN_DIRECTORY + "comparison/CompareEn.tsv");
        PathHeader.Factory phf = PathHeader.getFactory();
        Values values = new Values();

        try (PrintWriter out =
                FileUtilities.openUTF8Writer(
                        CLDRPaths.GEN_DIRECTORY + "comparison", "CompareEn.tsv")) {
            out.println(
                    "Proposed Disposition\tSection\tPage\tHeader\tCode\ten\ten_001\ten_GB\tPath");
            for (Factory factory : Arrays.asList(mainFactory, annotationsFactory)) {
                CLDRFile en_001 = factory.make("en_001", false);
                CLDRFile en_GB = factory.make("en_GB", false);

                CLDRFile enR = factory.make("en", true);
                CLDRFile en_001R = factory.make("en_001", true);
                CLDRFile en_GBR = factory.make("en_GB", true);

                // walk through all the new paths and values to check them.

                TreeSet<PathHeader> paths = new TreeSet<>();
                en_GB.forEach(x -> paths.add(phf.fromPath(x)));
                en_001.forEach(x -> paths.add(phf.fromPath(x)));

                for (PathHeader pathHeader : paths) {
                    String path = pathHeader.getOriginalPath();
                    String note = "";
                    // skip certain paths
                    switch (failsFilter(path, en_GBR, en_001R, values)) {
                        case skipButNote:
                            note = "AUTOSKIP";
                            break;
                        case accept:
                            break;
                        case skip:
                            continue;
                    }

                    String valueR = enR.getStringValue(path);

                    // drop the cases that will disappear with minimization

                    out.println(
                            note
                                    + "\t"
                                    + pathHeader
                                    + "\t"
                                    + valueR
                                    + "\t"
                                    + en_001.getStringValue(path)
                                    + "\t"
                                    + en_GB.getStringValue(path)
                                    + "\t"
                                    + path);
                }
            }
        }
    }
}
