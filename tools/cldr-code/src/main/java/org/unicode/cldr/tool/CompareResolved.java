package org.unicode.cldr.tool;

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

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.ibm.icu.util.Output;

public class CompareResolved {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CLDR_CONFIG.getSupplementalDataInfo();

    private enum MyOptions {
        source(new Params().setHelp("Set the source directory name. Only these files will be compared").setDefault(CLDRPaths.MAIN_DIRECTORY).setMatch(".*")),
        compare(new Params().setHelp("Set the comparison directory name.").setMatch(".*").setDefault(CLDRPaths.ARCHIVE_DIRECTORY + "cldr-42.0/common/main")),
        fileFilter(new Params().setHelp("Filter files in source dir.").setMatch(".*")),
        pathFilter(new Params().setHelp("Filter paths in each source file.").setMatch(".*")),
        verbose(new Params().setMatch(null)),
        Vertical(new Params().setHelp("True to only check values with vertical inheritance").setMatch("true|false")),
        Horizontal(new Params().setHelp("True to only check values with horizontal inheritance").setMatch("true|false")),
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

        // TODO
        Boolean onlyVertical = null; // !MyOptions.Vertical.option.doesOccur() ? null : Boolean.valueOf(MyOptions.Horizontal.option.getValue());
        Boolean onlyHorizontal = null; // !MyOptions.Horizontal.option.doesOccur() ? null : Boolean.valueOf(MyOptions.Horizontal.option.getValue());

        // set up factories

        Factory sourceFactory = Factory.make(sourceDir, ".*");
        Factory compareFactory;
        try {
            compareFactory = Factory.make(compareDir, ".*");
        } catch (Exception e1) {
            System.out.println(e1);
            return;
        }

        System.out.println("Comparing " + sourceDir + ", to " + compareDir);

        // don't currently use these, but might filter on these in the future
        Output<String> sourcePathFound = new Output<>();
        Output<String> sourceLocaleFound = new Output<>();
        Output<String> comparePathFound = new Output<>();
        Output<String> compareLocaleFound = new Output<>();

        int filterCountAllLocales = 0;
        int diffCountAllLocales = 0;

        // cycle over locales
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
            if (verbose) {
                System.out.println("Comparing " + localeID);
            }

            // cycle over the union of paths
            int filterCount = 0;
            int diffCount = 0;

            for (String path : sortedPaths) {
                if (pathMatcher != null && !pathMatcher.reset(path).find()) {
                    continue;
                }
                ++filterCount;
                String sourceValue = sourceFile.getStringValueWithBailey(path, sourcePathFound, sourceLocaleFound);
                String compareValue = compareFile.getStringValueWithBailey(path, comparePathFound, compareLocaleFound);

                if (Objects.equal(sourceValue, compareValue)) {
                    continue;
                }

                // inheritance filters, if not null, follow it.
                if (onlyVertical != null) {
                    final boolean isVertical = !Objects.equal(sourceLocaleFound.value, compareLocaleFound.value);
                    if (isVertical != (onlyVertical == true)) {
                        continue;
                    }
                }
                if (onlyHorizontal != null) {
                    final boolean isHorizontal = !Objects.equal(sourcePathFound.value, comparePathFound.value);
                    if (isHorizontal != (onlyHorizontal == true)) {
                        continue;
                    }
                }

                // fell through, so record diff

                ++diffCount;
                System.out.println(path + "\t" + sourceValue + "\t" + compareValue);
            }
            if (verbose) {
                System.out.println("\tfilteredCount: " + filterCount + ", diffCount: " + diffCount);
            }
            filterCountAllLocales += filterCount;
            diffCountAllLocales += diffCount;
        }
        if (verbose) {
            System.out.println("ALL LOCALES: filteredCountAllLocales: " + filterCountAllLocales + ", diffCountAllLocales: " + diffCountAllLocales);
        }
    }
}
