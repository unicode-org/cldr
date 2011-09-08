package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.XMLFileReader;

public class SearchXml {
    //TODO Use options
    private static Matcher fileMatcher;

    private static Matcher pathMatcher;

    private static Matcher valueMatcher;
    private static Matcher levelMatcher;

    private static boolean showFiles;
    private static boolean showValues;
    private static boolean replaceValues;

    private static int total = 0;

    private static boolean countOnly = false;
    private static boolean verbose = false;

    private static boolean pathExclude = false;
    private static boolean levelExclude = false;
    private static boolean valueExclude = false;
    private static boolean fileExclude = false;

    private static String valuePattern;

    private static Counter<String> kountRegexMatches;

    final static Options myOptions = new Options()
    .add("source", ".*", CldrUtility.MAIN_DIRECTORY, "source directory")
    .add("file", ".*", null, "regex to filter files. ! in front selects items that don't match.")
    .add("path", ".*", null, "regex to filter paths. ! in front selects items that don't match. example: -p relative.*@type=\\\"-?3\\\"")
    .add("value", ".*", null, "regex to filter values. ! in front selects items that don't match")
    .add("level", ".*", null, "regex to filter levels. ! in front selects items that don't match")
    .add("count", null, null, "only count items")
    .add("kount", null, null, "count regex group matches in pattern")
    .add("Verbose", null, null, "verbose output")
    ;

    public static void main(String[] args) throws IOException {
        myOptions.parse(args, true);

        String sourceDirectory = myOptions.get("source").getValue();
        if (sourceDirectory == null) {
            System.out.println("Need Source Directory! ");
            return;
        }
        Output<Boolean> exclude = new Output<Boolean>();
        fileMatcher = getMatcher(myOptions.get("file").getValue(), exclude);
        fileExclude = exclude.value;

        pathMatcher = getMatcher(myOptions.get("path").getValue(), exclude);
        pathExclude = exclude.value;

        levelMatcher = getMatcher(myOptions.get("level").getValue(), exclude);
        levelExclude = exclude.value;

        valueMatcher = getMatcher(myOptions.get("value").getValue(), exclude);
        valueExclude = exclude.value;

        if (pathMatcher != null && valueMatcher != null) {
            valuePattern = valueMatcher.pattern().toString();
            if (Pattern.compile("\\$\\d.*").matcher(valuePattern).find()) {
                replaceValues = true;
            }
        }

        countOnly = myOptions.get("count").doesOccur();
        kountRegexMatches = myOptions.get("kount").doesOccur() ? new Counter<String>() : null;

        //        showFiles = myOptions.get("showFiles").doesOccur();
        //        showValues = myOptions.get("showValues").doesOccur();

        double startTime = System.currentTimeMillis();
        File src = new File(sourceDirectory);

        if (countOnly) {
            System.out.print("file");
            for (Level cLevel : Level.values()) {
                System.out.print("\t" + cLevel);
            }
            System.out.println();
        }

        processDirectory(src);

        if (kountRegexMatches != null) {
            for (String item : kountRegexMatches.getKeysetSortedByCount(false)) {
                System.out.println(kountRegexMatches.getCount(item) + "\t" + item);
            }
        }

        double deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
        System.out.println("Instances found: " + total);
    }

    private static Matcher getMatcher(String property, Output<Boolean> exclude) {
        exclude.value = false;
        if (property == null) {
            return null;
        }
        if (property.startsWith("!")) {
            exclude.value = true;
            property = property.substring(1);
        }
        return Pattern.compile(property).matcher("");
    }

    private static void processDirectory(File src) throws IOException {
        for (File file : src.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file);
                continue;
            }
            if (file.length() == 0) {
                continue;
            }

            String fileName = file.getName();
            String canonicalFile = file.getCanonicalPath();

            if (!fileName.endsWith(".xml")) {
                continue;
            }

            fileName = fileName.substring(0,fileName.length()-4); // remove .xml

            if (fileMatcher != null && fileExclude == fileMatcher.reset(fileName).find()) {
                if (verbose) {
                    System.out.println("Skipping " + canonicalFile);
                }
                continue;
            }


            if (showFiles) {
                System.out.println("* " + canonicalFile);
            }

            myHandler.levelCounter.clear();
            myHandler.firstMessage = "* " + canonicalFile;
            myHandler.file = fileName;
            if (levelMatcher != null || countOnly) {
                try {
                    myHandler.level = CoverageLevel2.getInstance(fileName);
                } catch (Exception e) {
                    myHandler.level = null;
                }
            }

            XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
            try {
                xfr.read(canonicalFile, XMLFileReader.CONTENT_HANDLER
                        | XMLFileReader.ERROR_HANDLER, false);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            if (countOnly) {
                System.out.print(fileName);
                for (Level cLevel : Level.values()) {
                    System.out.print("\t" + myHandler.levelCounter.get(cLevel));
                }
                System.out.println();
            }
            System.out.flush();
        }
    }

    static MyHandler myHandler = new MyHandler();

    /**
     * @author markdavis
     *
     */
    static class MyHandler extends XMLFileReader.SimpleHandler {
        CoverageLevel2 level;
        String firstMessage;
        String file;
        Counter<Level> levelCounter = new Counter<Level>();

        public void handlePathValue(String path, String value) {

            if (pathMatcher != null && pathExclude == pathMatcher.reset(path).find()) {
                return;
            }


            Level pathLevel = null;

            pathLevel = level == null ? Level.COMPREHENSIVE : level.getLevel(path);
            levelCounter.add(pathLevel, 1);

            if (levelMatcher != null && levelExclude == levelMatcher.reset(pathLevel.toString()).find()) {
                return;
            }

            if (showValues) {
                System.out.println(value + "\t<=\t" + path);
            }

            if (replaceValues) {
                String pattern = valuePattern;
                for (int i = 0; i <= pathMatcher.groupCount(); ++i) {
                    pattern = pattern.replace("$" + i, pathMatcher.group(i));
                }
                valueMatcher = Pattern.compile(pattern).matcher("");
            }

            if (valueMatcher != null && valueExclude == valueMatcher.reset(value).find()) {
                return;
            }

            if (kountRegexMatches != null && pathMatcher != null) {
                kountRegexMatches.add(pathMatcher.group(1), 1);
            }

            ++total;

            if (firstMessage != null) {
                //System.out.println(firstMessage);
                firstMessage = null;
            }
            if (!countOnly) {
                System.out.println(file + "\t" + value + "\t" + path);
            }
        }
    }
}