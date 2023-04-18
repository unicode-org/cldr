package org.unicode.cldr.tool;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.ibm.icu.util.Output;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

public class CompareResolved {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            CLDR_CONFIG.getSupplementalDataInfo();

    private enum MyOptions {
        source(
                new Params()
                        .setHelp("Set the source directory name. Only these files will be compared")
                        .setDefault(CLDRPaths.ARCHIVE_DIRECTORY + "cldr-42.0/common/main")
                        .setMatch(".*")),
        compare(
                new Params()
                        .setHelp("Set the comparison directory name.")
                        .setMatch(".*")
                        .setDefault(CLDRPaths.MAIN_DIRECTORY)),
        fileFilter(new Params().setHelp("Filter files in source dir.").setMatch(".*")),
        pathFilter(new Params().setHelp("Filter paths in each source file.").setMatch(".*")),
        verbose(new Params().setMatch(null)),
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

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {

        // get options

        MyOptions.parse(args);
        String sourceDir = MyOptions.source.option.getValue();
        String compareDir = MyOptions.compare.option.getValue();

        Matcher fileMatcher = null;
        String pattern = MyOptions.fileFilter.option.getValue();
        if (pattern != null) {
            fileMatcher = Pattern.compile(pattern).matcher("");
        }
        Matcher pathMatcher = null;
        pattern = MyOptions.pathFilter.option.getValue();
        if (pattern != null) {
            pathMatcher = Pattern.compile(pattern).matcher("");
        }

        boolean verbose = MyOptions.verbose.option.doesOccur();

        // set up factories

        Factory sourceFactory = Factory.make(sourceDir, ".*");
        Factory compareFactory;
        try {
            compareFactory = Factory.make(compareDir, ".*");
        } catch (Exception e1) {
            System.out.println(e1);
            return;
        }

        System.out.println("## Comparing\t\tSource (S) dir\tCompare (C) dir");
        System.out.println("## Comparing\t\t" + sourceDir + "\t" + compareDir);

        // don't currently use these, but might filter on these in the future
        Output<String> sourcePathFound = new Output<>();
        Output<String> sourceLocaleFound = new Output<>();
        Output<String> comparePathFound = new Output<>();
        Output<String> compareLocaleFound = new Output<>();

        int filterCountAllLocales = 0;
        int diffCountAllLocales = 0;

        // cycle over locales
        System.out.println(
                "## Locale\tRequested Path\tResolved Value (S)\tResolved Value (C)\tFound Locale (S)\tFound Locale (C)\tFound Path (S)\tFound Path (C)");
        for (String localeID : sourceFactory.getAvailable()) {
            if (fileMatcher != null && !fileMatcher.reset(localeID).find()) {
                continue;
            }

            // create the CLDRFiles

            CLDRFile sourceFile = sourceFactory.make(localeID, true); // resolved
            CLDRFile compareFile;
            try {
                compareFile = compareFactory.make(localeID, true); // resolved
            } catch (Exception e) {
                System.out.println(localeID + " not available in " + compareDir);
                continue;
            }

            // get the union of paths

            Set<String> sortedPaths = new TreeSet<>(); // could sort by PathHeader also
            sortedPaths.addAll(Sets.newTreeSet(sourceFile));
            sortedPaths.addAll(Sets.newTreeSet(compareFile));

            // cycle over the union of paths
            int filterCount = 0;
            int diffCount = 0;

            for (String path : sortedPaths) {
                if (pathMatcher != null && !pathMatcher.reset(path).find()) {
                    continue;
                }
                ++filterCount;
                String sourceValue =
                        sourceFile.getStringValueWithBailey(
                                path, sourcePathFound, sourceLocaleFound);
                String compareValue =
                        compareFile.getStringValueWithBailey(
                                path, comparePathFound, compareLocaleFound);

                if (Objects.equal(sourceValue, compareValue)) {
                    continue;
                }

                final boolean verticalDiff =
                        !Objects.equal(sourceLocaleFound.value, compareLocaleFound.value);
                final boolean horizontalDiff =
                        !Objects.equal(sourcePathFound.value, comparePathFound.value);

                //                // inheritance filters, if not null, follow it.
                //                if (onlyVertical != null) {
                //                    if (verticalDiff != (onlyVertical == true)) {
                //                        continue;
                //                    }
                //                }
                //                if (onlyHorizontal != null) {
                //                    if (horizontalDiff != (onlyHorizontal == true)) {
                //                        continue;
                //                    }
                //                }

                // fell through, so record diff

                ++diffCount;
                System.out.println(
                        localeID
                                + "\t"
                                + path //
                                + "\t"
                                + sourceValue
                                + "\t"
                                + compareValue //
                                + "\t"
                                + sourceLocaleFound.value
                                + "\t"
                                + compareLocaleFound.value
                                + "\t"
                                + abbreviate(sourcePathFound.value, path)
                                + "\t"
                                + abbreviate(comparePathFound.value, path));
            }
            if (verbose || diffCount != 0) {
                System.out.println(
                        "# "
                                + localeID
                                + "\tfilteredCount:\t"
                                + filterCount
                                + "\tdiffCount:\t"
                                + diffCount);
            }
            filterCountAllLocales += filterCount;
            diffCountAllLocales += diffCount;
        }
        if (verbose || diffCountAllLocales != 0) {
            System.out.println(
                    "# "
                            + "ALL LOCALES"
                            + "\t#filteredCount:\t"
                            + filterCountAllLocales
                            + ", diffCountAllLocales: "
                            + diffCountAllLocales);
        }
        System.out.println("DONE");
    }

    /*
     * Abbreviate the pathToAbbreviate, replacing leading and trailing identical elements/attributes by …
     */
    private static String abbreviate(String pathToAbbreviate, String referencePath) {
        if (pathToAbbreviate.equals(referencePath)) {
            return "…";
        }
        XPathParts compare = XPathParts.getFrozenInstance(pathToAbbreviate);
        XPathParts source = XPathParts.getFrozenInstance(referencePath);
        final int cSize = compare.size();
        final int sSize = source.size();
        int min = Math.min(cSize, sSize);

        int initialSame = 0;
        for (; initialSame < min; ++initialSame) {
            String cElement = compare.getElement(initialSame);
            String sElement = source.getElement(initialSame);
            Map<String, String> cAttributes = compare.getAttributes(initialSame);
            Map<String, String> sAttributes = source.getAttributes(initialSame);
            if (!cElement.equals(sElement) || !cAttributes.equals(sAttributes)) {
                break;
            }
        }
        // at this point elements less than initialSame are identical

        int trailingSame = cSize;
        int cDelta = -1;
        int sDelta = sSize - cSize - 1;
        for (; trailingSame > initialSame; --trailingSame) {
            String cElement = compare.getElement(trailingSame + cDelta);
            String sElement = source.getElement(trailingSame + sDelta);
            Map<String, String> cAttributes = compare.getAttributes(trailingSame + cDelta);
            Map<String, String> sAttributes = source.getAttributes(trailingSame + sDelta);
            if (!cElement.equals(sElement) || !cAttributes.equals(sAttributes)) {
                break;
            }
        }
        // at this point elements at or after trailingSame are identical
        return "…"
                + compare.toString(initialSame, trailingSame)
                + (trailingSame == cSize ? "" : "…");
    }
}
