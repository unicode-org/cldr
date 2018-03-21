package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.BaseUrl;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class SearchXml {

    // TODO Use options
    private static Matcher fileMatcher;

    private static Matcher pathMatcher;

    private static Matcher valueMatcher;
    private static Matcher levelMatcher;

    private static boolean showFiles;
    private static boolean showValues = true;
    private static boolean replaceValues;

    private static int total = 0;

    private static boolean countOnly = false;
    private static boolean verbose = false;

    private static boolean pathExclude = false;
    private static boolean levelExclude = false;
    private static boolean valueExclude = false;
    private static boolean fileExclude = false;
    private static boolean unique = false;
    private static boolean groups = false;
    private static Counter<String> uniqueData = new Counter<String>();

    private static String valuePattern;
    private static File comparisonDirectory;
    private static boolean recursive;

    private static Counter<String> kountRegexMatches;
    private static Counter<String> starCounter;
    private static final Set<String> ERRORS = new LinkedHashSet<String>();
    private static final PathStarrer pathStarrer = new PathStarrer();
    private static PathHeader.Factory PATH_HEADER_FACTORY = null;

    final static Options myOptions = new Options()
        .add("source", ".*", CLDRPaths.MAIN_DIRECTORY, "source directory (use also " + CLDRPaths.AUX_DIRECTORY + ")")
        .add("file", ".*", null, "regex to filter files. ! in front selects items that don't match.")
        .add("path", ".*", null, "regex to filter paths. ! in front selects items that don't match. example: -p relative.*@type=\\\"-?3\\\"")
        .add("value", ".*", null, "regex to filter values. ! in front selects items that don't match")
        .add("level", ".*", null, "regex to filter levels. ! in front selects items that don't match")
        .add("count", null, null, "only count items")
        .add("kount", null, null, "count regex group matches in pattern")
        .add("other", ".+", null, "compare against other directory")
        .add("unique", null, null, "only unique lines")
        .add("groups", null, null, "only retain capturing groups in path/value, eg in -p @modifiers=\\\"([^\\\"]*+)\\\", output the part in (...)")
        .add("Verbose", null, null, "verbose output")
        .add("recursive", null, null, "recurse directories")
        .add("Star", null, null, "get statistics on starred paths")
        .add("PathHeader", null, null, "show path header and string ID");

    public static void main(String[] args) throws IOException {
        double startTime = System.currentTimeMillis();
        myOptions.parse(args, true);

        verbose = myOptions.get("Verbose").doesOccur();

        String sourceDirectory = myOptions.get("source").getValue();
        if (sourceDirectory == null) {
            System.out.println("#" + "Need Source Directory! ");
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

        if (myOptions.get("Star").doesOccur()) {
            starCounter = new Counter<String>();
        }

        if (pathMatcher != null && valueMatcher != null) {
            valuePattern = valueMatcher.pattern().toString();
            if (PatternCache.get("\\$\\d.*").matcher(valuePattern).find()) {
                replaceValues = true;
            }
        }

        if (myOptions.get("PathHeader").doesOccur()) {
            PATH_HEADER_FACTORY = PathHeader.getFactory(ToolConfig.getToolInstance().getEnglish());
        }

        unique = myOptions.get("unique").doesOccur();
        groups = myOptions.get("groups").doesOccur();

        countOnly = myOptions.get("count").doesOccur();
        kountRegexMatches = myOptions.get("kount").doesOccur() ? new Counter<String>() : null;

        recursive = myOptions.get("recursive").doesOccur();

        // showFiles = myOptions.get("showFiles").doesOccur();
        // showValues = myOptions.get("showValues").doesOccur();

        File src = new File(sourceDirectory);
        if (!src.isDirectory()) {
            System.err.println("#" + sourceDirectory + " must be a directory");
            return;
        }

        String comparisonDirectoryString = myOptions.get("other").getValue();
        if (comparisonDirectoryString != null) {
            comparisonDirectory = new File(comparisonDirectoryString);
            if (!comparisonDirectory.isDirectory()) {
                System.err.println("#" + comparisonDirectoryString + " must be a directory");
                return;
            }
        }

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
                System.out.println("#" + kountRegexMatches.getCount(item) + "\t" + item);
            }
        }

        if (unique) {
            for (String item : uniqueData.getKeysetSortedByCount(false)) {
                System.out.println("#" + uniqueData.getCount(item) + item);
            }
        }

        if (starCounter != null) {
            for (String path : starCounter.getKeysetSortedByCount(false)) {
                System.out.println("#" + starCounter.get(path) + "\t" + path);
            }
        }
        double deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("#" + "Elapsed: " + deltaTime / 1000.0 + " seconds");
        System.out.println("#" + "Instances found: " + total);
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
        return UnicodeRegex.compile(property).matcher("");
    }

    private static void processDirectory(File src) throws IOException {
        if (comparisonDirectory != null) {
            System.out.println("#" + "Locale" +
                "\tFile" +
                "\tBase" +
                DiffInfo.DiffInfoHeader +
                "\n#\tValue\tOtherValue\tPath");
        }
        for (File file : src.listFiles()) {
            if (recursive && file.isDirectory()) {
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

            String coreName = fileName.substring(0, fileName.length() - 4); // remove .xml

            if (fileMatcher != null && fileExclude == fileMatcher.reset(coreName).find()) {
                if (verbose) {
                    System.out.println("#" + "* Skipping " + canonicalFile);
                }
                continue;
            }
            if (verbose) {
                System.out.println("#" + "Searching " + canonicalFile);
            }

            if (showFiles) {
                System.out.println("#" + "* " + canonicalFile);
            }

            Relation<String, String> source = getXmlFileAsRelation(src, fileName);
            Relation<String, String> other = null;
            if (comparisonDirectory != null) {
                other = getXmlFileAsRelation(comparisonDirectory, fileName);
            }

            checkFiles(recursive ? file.getParent() : null, fileName, coreName, source, other);
            System.out.flush();
        }
        System.out.println("#" + "\t" + DiffInfo.DiffInfoHeader);
        DIFF_INFO.showValues("TOTAL");

        for (String error : ERRORS) {
            System.err.println("#" + error);
        }
    }

    private static Relation<String, String> getXmlFileAsRelation(File directory, String fileName) {
        ListHandler listHandler = new ListHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(listHandler);
        try {
            String fileName2 = directory.getCanonicalPath() + "/" + fileName;
            xfr.read(fileName2, XMLFileReader.CONTENT_HANDLER
                | XMLFileReader.ERROR_HANDLER, false);
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter arg0 = new PrintWriter(stringWriter);
            e.printStackTrace(arg0);
            arg0.flush();
            ERRORS.add("Can't read " + directory + "/" + fileName + "\n" + stringWriter);
        }
        return listHandler.data;
    }

    static class ListHandler extends XMLFileReader.SimpleHandler {
        public Relation<String, String> data = Relation.of(new LinkedHashMap<String, Set<String>>(),
            LinkedHashSet.class);

        public void handlePathValue(String path, String value) {
            data.put(path, value);
        }
    }

    // static MyHandler myHandler = new MyHandler();

    static DiffInfo DIFF_INFO = new DiffInfo();

    static class DiffInfo {
        static final String DiffInfoHeader = "\tSame" +
            "\tDeletions" +
            "\tAdditions" +
            "\tChanges";

        int additionCount = 0;
        int deletionCount = 0;
        int changed2Values = 0;
        int sameCount = 0;

        public void showValues(String title) {
            System.out.println("#" + title +
                "\t" + sameCount +
                "\t" + deletionCount +
                "\t" + additionCount +
                "\t" + (changed2Values / 2));
            DIFF_INFO.additionCount += additionCount;
            DIFF_INFO.deletionCount += deletionCount;
            DIFF_INFO.changed2Values += changed2Values;
            DIFF_INFO.sameCount += sameCount;
        }
    }

    /**
     * @author markdavis
     * @param fileName
     * @param canonicalFile
     *
     */
    private static void checkFiles(
        String filePath,
        String fileName,
        String coreName,
        Relation<String, String> source,
        Relation<String, String> other) {
        CoverageLevel2 level = null;
        String firstMessage;
        String file;
        Counter<Level> levelCounter = new Counter<Level>();
        String canonicalFile = fileName;
        firstMessage = "* " + canonicalFile;
        file = canonicalFile;

        DiffInfo diffInfo = new DiffInfo();

        if (levelMatcher != null || countOnly) {
            try {
                level = CoverageLevel2.getInstance(canonicalFile);
            } catch (Exception e) {
            }
        }

        if (countOnly) {
            System.out.print(fileName);
            for (Level cLevel : Level.values()) {
                System.out.print("\t" + levelCounter.get(cLevel));
            }
            System.out.println();
        }

        Set<String> keys = new LinkedHashSet<String>(source.keySet());
        if (other != null) {
            keys.addAll(other.keySet());
        }
        for (String path : keys) {
            if (path.startsWith("//ldml/identity/")) {
                continue;
            }
            if (pathMatcher != null && pathExclude == pathMatcher.reset(path).find()) {
                continue;
            }

            Level pathLevel = null;

            pathLevel = level == null ? Level.COMPREHENSIVE : level.getLevel(path);
            levelCounter.add(pathLevel, 1);

            if (levelMatcher != null && levelExclude == levelMatcher.reset(pathLevel.toString()).find()) {
                continue;
            }

            Set<String> values = source.get(path);
            Set<String> otherValues = other == null ? null : other.get(path);

            // if (showValues) {
            // System.out.println("#"+values + "\t" + otherValues + "\t<=\t" + path);
            // }

            if (other != null) {
                if (values != otherValues) {
                    boolean diff = true;
                    if (values == null) {
                        diffInfo.additionCount += otherValues.size();
                    } else if (otherValues == null) {
                        diffInfo.deletionCount += values.size();
                    } else if (!values.equals(otherValues)) {
                        diffInfo.changed2Values += values.size() + otherValues.size();
                    } else {
                        diff = false;
                        diffInfo.sameCount += values.size();
                    }
                    if (diff && showValues) {
                        show(file, path, values, otherValues);
                    }
                }
            } else {
                for (String value : values) {
                    if (replaceValues) {
                        String pattern = valuePattern;
                        for (int i = 0; i <= pathMatcher.groupCount(); ++i) {
                            pattern = pattern.replace("$" + i, pathMatcher.group(i));
                        }
                        valueMatcher = PatternCache.get(pattern).matcher("");
                    }

                    if (valueMatcher != null && valueExclude == valueMatcher.reset(value).find()) {
                        continue;
                    }

                    if (kountRegexMatches != null && pathMatcher != null) {
                        kountRegexMatches.add(pathMatcher.group(1), 1);
                    }

                    if (starCounter != null) {
                        starCounter.add(pathStarrer.set(path), 1);
                    }
                    ++total;

                    if (firstMessage != null) {
                        // System.out.println("#"+firstMessage);
                        firstMessage = null;
                    }
                    if (!countOnly) {
                        String data = groups
                            ? group(value, valueMatcher) + "\t" + group(path, pathMatcher)
                            : value + "\t" + path;
                        if (!unique) {
                            String pathHeaderInfo = "";
                            if (PATH_HEADER_FACTORY != null) {
                                PathHeader pathHeader = PATH_HEADER_FACTORY.fromPath(path);
                                if (pathHeader != null) {
                                    pathHeaderInfo = "\n\t" + pathHeader
                                        + "\n\t" + pathHeader.getUrl(BaseUrl.PRODUCTION, coreName);
                                }
                            }
                            // http://st.unicode.org/cldr-apps/v#/en/Fields/59d8178ec2fe04ae
                            if (!groups && pathHeaderInfo.isEmpty()) {
                                show(file, path, Collections.singleton(value), null);
                            } else {
                                System.out.println("#?" +
                                    (recursive ? filePath + "\t" : "")
                                    + file + "\t" + data
                                    + pathHeaderInfo);
                            }
                        } else {
                            uniqueData.add(data, 1);
                        }
                    }
                }
            }
        }
        if (other != null) {
            ULocale locale = new ULocale(fileName.substring(0, fileName.length() - 4));
            String localeName = locale.getDisplayName(ULocale.ENGLISH);
            String title = localeName +
                "\t" + fileName +
                "\t" + getType(locale);
            diffInfo.showValues(title);
        }
    }

    private static void show(String fileName, String path, Set<String> values, Set<String> otherValues) {
        // locale=  af     ; action=add ; new_path=        //ldml/dates/fields/field[@type="second"]/relative[@type="0"]    ; new_value=    nou  
        String fileWithoutSuffix = fileName.substring(0, fileName.length() - 4);
        String values2 = values.size() != 1 ? values.toString() : values.iterator().next();
        System.out.println("locale=" + fileWithoutSuffix
            + ";\taction=add"
            + ";\tnew_path=" + path
            + ";\tnew_value=" + values2
            + (otherValues == null ? "" : ";\tother_value=" + otherValues));
    }

    static Set<String> defaultContent = SupplementalDataInfo.getInstance().getDefaultContentLocales();

    private static String getType(ULocale locale) {
        if (defaultContent.contains(locale.toString())) {
            return "DC";
        } else if (locale.getCountry().isEmpty()) {
            return "Base";
        } else {
            return "Region";
        }
    }

    private static String group(String item, Matcher matcher) {
        if (matcher == null) {
            return item;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 1; i <= matcher.groupCount(); ++i) {
            b.append(matcher.group(i));
        }
        return b.toString();
    }

    //    static class StarCounter {
    //        Map<String,Counter<String>> data = new HashMap();
    //    }
}