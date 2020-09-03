/*
 ******************************************************************************
 * Copyright (C) 2004-2013, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CLDRTest;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.QuickCheck;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.NumberingSystem;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DateTimeCanonicalizer;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
// import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.PathChecker;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Tool for applying modifications to the CLDR files. Use -h to see the options.
 * <p>
 * There are some environment variables that can be used with the program <br>
 * -DSHOW_FILES=<anything> shows all create/open of files.
 */
@CLDRTool(alias = "modify",
description = "Tool for applying modifications to the CLDR files. Use -h to see the options.")
public class CLDRModify {
    private static final boolean DEBUG = false;
    static final String DEBUG_PATHS = null; // ".*currency.*";
    static final boolean COMMENT_REMOVALS = false; // append removals as comments
    static final UnicodeSet whitespace = new UnicodeSet("[:whitespace:]").freeze();
    static final UnicodeSet HEX = new UnicodeSet("[a-fA-F0-9]").freeze();
    private static final DtdData dtdData = DtdData.getInstance(DtdType.ldml);

    // TODO make this into input option.

    enum ConfigKeys {
        action, locale, path, value, new_path, new_value
    }

    enum ConfigAction {
        /**
         * Remove a path
         */
        delete,
        /**
         * Add a path/value
         */
        add,
        /**
         * Replace a path/value. Equals 'add' but tests selected paths
         */
        replace,
        /**
         * Add a a path/value. Equals 'add' but tests that path did NOT exist
         */
        addNew,
    }

    static final class ConfigMatch {
        final String exactMatch;
        final Matcher regexMatch; // doesn't have to be thread safe
        final ConfigAction action;
        final boolean hexPath;

        public ConfigMatch(ConfigKeys key, String match) {
            if (key == ConfigKeys.action) {
                exactMatch = null;
                regexMatch = null;
                action = ConfigAction.valueOf(match);
                hexPath = false;
            } else if (match.startsWith("/") && match.endsWith("/")) {
                if (key != ConfigKeys.locale && key != ConfigKeys.path && key != ConfigKeys.value) {
                    throw new IllegalArgumentException("Regex only allowed for old path/value.");
                }
                exactMatch = null;
                regexMatch = PatternCache.get(match.substring(1, match.length() - 1)
                    .replace("[@", "\\[@")).matcher("");
                action = null;
                hexPath = false;
            } else {
                exactMatch = match;
                regexMatch = null;
                action = null;
                hexPath = (key == ConfigKeys.new_path || key == ConfigKeys.path)
                    && HEX.containsAll(match);
            }

        }

        public boolean matches(String other) {
            if (exactMatch == null) {
                return regexMatch.reset(other).find();
            } else if (hexPath) {
                // convert path to id for comparison
                return exactMatch.equals(StringId.getHexId(other));
            } else {
                return exactMatch.equals(other);
            }
        }

        @Override
        public String toString() {
            return action != null ? action.toString()
                : exactMatch == null ? regexMatch.toString()
                    : hexPath ? "*" + exactMatch + "*"
                        : exactMatch;
        }

        public String getPath(CLDRFile cldrFileToFilter) {
            if (!hexPath) {
                return exactMatch;
            }
            // ensure that we have all the possible paths cached
            String path = StringId.getStringFromHexId(exactMatch);
            if (path == null) {
                for (String eachPath : cldrFileToFilter.fullIterable()) {
                    StringId.getHexId(eachPath);
                }
                path = StringId.getStringFromHexId(exactMatch);
                if (path == null) {
                    throw new IllegalArgumentException("No path for hex id: " + exactMatch);
                }
            }
            return path;
        }

        public static String getModified(ConfigMatch valueMatch, String value, ConfigMatch newValue) {
            if (valueMatch == null) { // match anything
                if (newValue != null && newValue.exactMatch != null) {
                    return newValue.exactMatch;
                }
                if (value != null) {
                    return value;
                }
                throw new IllegalArgumentException("Can't have both old and new be null.");
            } else if (valueMatch.exactMatch == null) { // regex
                if (newValue == null || newValue.exactMatch == null) {
                    throw new IllegalArgumentException("Can't have regex without replacement.");
                }
                StringBuffer buffer = new StringBuffer();
                valueMatch.regexMatch.appendReplacement(buffer, newValue.exactMatch);
                return buffer.toString();
            } else {
                return newValue.exactMatch != null ? newValue.exactMatch : value;
            }
        }
    }

    static FixList fixList = new FixList();

    private static final int HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        MATCH = 4,
        JOIN = 5,
        MINIMIZE = 6,
        FIX = 7,
        JOIN_ARGS = 8,
        VET_ADD = 9,
        RESOLVE = 10,
        PATH = 11,
        USER = 12,
        ALL_DIRS = 13,
        CHECK = 14,
        KONFIG = 15;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR().setDefault(CLDRPaths.MAIN_DIRECTORY),
        UOption.DESTDIR().setDefault(CLDRPaths.GEN_DIRECTORY + "cldrModify/"),
        UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("join", 'j', UOption.OPTIONAL_ARG),
        UOption.create("minimize", 'r', UOption.NO_ARG),
        UOption.create("fix", 'f', UOption.OPTIONAL_ARG),
        UOption.create("join-args", 'i', UOption.OPTIONAL_ARG),
        UOption.create("vet", 'v', UOption.OPTIONAL_ARG),
        UOption.create("resolve", 'z', UOption.OPTIONAL_ARG),
        UOption.create("path", 'p', UOption.REQUIRES_ARG),
        UOption.create("user", 'u', UOption.REQUIRES_ARG),
        UOption.create("all", 'a', UOption.REQUIRES_ARG),
        UOption.create("check", 'c', UOption.NO_ARG),
        UOption.create("konfig", 'k', UOption.OPTIONAL_ARG).setDefault("modify_config.txt"),
    };

    private static final UnicodeSet allMergeOptions = new UnicodeSet("[rcd]");

    static final String HELP_TEXT1 = "Use the following options"
        + XPathParts.NEWLINE
        + "-h or -?\t for this message"
        + XPathParts.NEWLINE
        + "-"
        + options[SOURCEDIR].shortName
        + "\t source directory. Default = -s"
        + CldrUtility.getCanonicalName(CLDRPaths.MAIN_DIRECTORY)
        + XPathParts.NEWLINE
        + "\tExample:-sC:\\Unicode-CVS2\\cldr\\common\\gen\\source\\"
        + XPathParts.NEWLINE
        + "-"
        + options[DESTDIR].shortName
        + "\t destination directory. Default = -d"
        + CldrUtility.getCanonicalName(CLDRPaths.GEN_DIRECTORY + "main/")
        + XPathParts.NEWLINE
        + "-m<regex>\t to restrict the locales to what matches <regex>"
        + XPathParts.NEWLINE
        + "-j<merge_dir>/X'\t to merge two sets of files together (from <source_dir>/X and <merge_dir>/X', "
        + XPathParts.NEWLINE
        + "\twhere * in X' is replaced by X)."
        + XPathParts.NEWLINE
        + "\tExample:-jC:\\Unicode-CVS2\\cldr\\dropbox\\to_be_merged\\missing\\missing_*"
        + XPathParts.NEWLINE
        + "-i\t merge arguments:"
        + XPathParts.NEWLINE
        + "\tr\t replace contents (otherwise new data will be draft=\"unconfirmed\")"
        + XPathParts.NEWLINE
        + "\tc\t ignore comments in <merge_dir> files"
//        + XPathParts.NEWLINE
//        + "-r\t to minimize the results (removing items that inherit from parent)."
+ XPathParts.NEWLINE
+ "-v\t incorporate vetting information, and generate diff files."
+ XPathParts.NEWLINE
+ "-z\t generate resolved files"
+ XPathParts.NEWLINE
+ "-p\t set path for -fx"
+ XPathParts.NEWLINE
+ "-u\t set user for -fb"
+ XPathParts.NEWLINE
+ "-a\t pattern: recurse over all subdirectories that match pattern"
+ XPathParts.NEWLINE
+ "-c\t check that resulting xml files are valid. Requires that a dtd directory be copied to the output directory, in the appropriate location."
+ XPathParts.NEWLINE
+ "-k\t config_file\twith -fk perform modifications according to what is in the config file. For format details, see:"
+ XPathParts.NEWLINE
+ "\t\thttp://cldr.unicode.org/development/cldr-big-red-switch/cldrmodify-passes/cldrmodify-config."
+ XPathParts.NEWLINE
+ "-f\t to perform various fixes on the files (add following arguments to specify which ones, eg -fxi)"
+ XPathParts.NEWLINE;

    static final String HELP_TEXT2 = "Note: A set of bat files are also generated in <dest_dir>/diff. They will invoke a comparison program on the results."
        + XPathParts.NEWLINE;
    private static final boolean SHOW_DETAILS = false;
    private static boolean SHOW_PROCESSING = false;

    static String sourceInput;

    /**
     * Picks options and executes. Use -h to see options.
     */
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        UOption.parseArgs(args, options);
        if (options[HELP1].doesOccur || options[HELP2].doesOccur) {
            System.out.println(HELP_TEXT1 + fixList.showHelp() + HELP_TEXT2);
            return;
        }
        checkSuboptions(options[FIX], fixList.getOptions());
        checkSuboptions(options[JOIN_ARGS], allMergeOptions);
        String recurseOnDirectories = options[ALL_DIRS].value;
        boolean makeResolved = options[RESOLVE].doesOccur; // Utility.COMMON_DIRECTORY + "main/";

        // String sourceDir = "C:\\ICU4C\\locale\\common\\main\\";

        sourceInput = options[SOURCEDIR].value;
        String destInput = options[DESTDIR].value;
        if (recurseOnDirectories != null) {
            sourceInput = removeSuffix(sourceInput, "main/", "main");
            destInput = removeSuffix(destInput, "main/", "main");
        }
        String sourceDirBase = CldrUtility.checkValidDirectory(sourceInput); // Utility.COMMON_DIRECTORY + "main/";
        String targetDirBase = CldrUtility.checkValidDirectory(destInput); // Utility.GEN_DIRECTORY + "main/";
        System.out.format("Source:\t%s\n", sourceDirBase);
        System.out.format("Target:\t%s\n", targetDirBase);

        Set<String> dirSet = new TreeSet<>();
        if (recurseOnDirectories == null) {
            dirSet.add("");
        } else {
            String[] subdirs = new File(sourceDirBase).list();
            Matcher subdirMatch = PatternCache.get(recurseOnDirectories).matcher("");
            for (String subdir : subdirs) {
                if (!subdirMatch.reset(subdir).find()) continue;
                dirSet.add(subdir + "/");
            }
        }
        for (String dir : dirSet) {
            String sourceDir = sourceDirBase + dir;
            if (!new File(sourceDir).isDirectory()) continue;
            String targetDir = targetDirBase + dir;
            // Log.setLog(targetDir + "/diff", "log.txt");
            try {
                Factory cldrFactory = Factory.make(sourceDir, ".*");

                if (options[VET_ADD].doesOccur) {
                    VettingAdder va = new VettingAdder(options[VET_ADD].value);
                    va.showFiles(cldrFactory, targetDir);
                    return;
                }

                Factory mergeFactory = null;

                String join_prefix = "", join_postfix = "";
                if (options[JOIN].doesOccur) {
                    String mergeDir = options[JOIN].value;
                    File temp = new File(mergeDir);
                    mergeDir = CldrUtility.checkValidDirectory(temp.getParent() + File.separator); // Utility.COMMON_DIRECTORY
                    // + "main/";
                    String filename = temp.getName();
                    join_prefix = join_postfix = "";
                    int pos = filename.indexOf("*");
                    if (pos >= 0) {
                        join_prefix = filename.substring(0, pos);
                        join_postfix = filename.substring(pos + 1);
                    }
                    mergeFactory = Factory.make(mergeDir, ".*");
                }
                /*
                 * Factory cldrFactory = Factory.make(sourceDir, ".*");
                 * Set testSet = cldrFactory.getAvailable();
                 * String[] quicktest = new String[] {
                 * "de"
                 * //"ar", "dz_BT",
                 * // "sv", "en", "de"
                 * };
                 * if (quicktest.length > 0) {
                 * testSet = new TreeSet(Arrays.asList(quicktest));
                 * }
                 */
                Set<String> locales = new TreeSet<>(cldrFactory.getAvailable());
                if (mergeFactory != null) {
                    Set<String> temp = new TreeSet<>(mergeFactory.getAvailable());
                    Set<String> locales3 = new TreeSet<>();
                    for (String locale : temp) {
                        if (!locale.startsWith(join_prefix) || !locale.endsWith(join_postfix)) continue;
                        locales3.add(locale.substring(join_prefix.length(), locale.length() - join_postfix.length()));
                    }
                    locales.retainAll(locales3);
                    System.out.println("Merging: " + locales3);
                }
                new CldrUtility.MatcherFilter(options[MATCH].value).retainAll(locales);

                RetainWhenMinimizing retainIfTrue = null;
                PathHeader.Factory pathHeaderFactory = null;

                fixList.handleSetup();

                long lastTime = System.currentTimeMillis();
                int spin = 0;
                System.out.format(locales.size() + " Locales:\t%s\n", locales.toString());
                int totalRemoved = 0;
                for (String test : locales) {
                    spin++;
                    if (SHOW_PROCESSING) {
                        long now = System.currentTimeMillis();
                        if (now - lastTime > 5000) {
                            System.out.println(" .. still processing " + test + " [" + spin + "/" + locales.size()
                            + "]");
                            lastTime = now;
                        }
                    }
                    // testJavaSemantics();

                    // TODO parameterize the directory and filter
                    // System.out.println("C:\\ICU4C\\locale\\common\\main\\fr.xml");

                    CLDRFile k = cldrFactory.make(test, makeResolved).cloneAsThawed();
                    // HashSet<String> set = Builder.with(new HashSet<String>()).addAll(k).get();
                    // System.out.format("Locale\t%s, Size\t%s\n", test, set.size());
                    // if (k.isNonInheriting()) continue; // for now, skip supplementals
                    if (DEBUG_PATHS != null) {
                        System.out.println("Debug1 (" + test + "):\t" + k.toString(DEBUG_PATHS));
                    }
                    // System.out.println(k);
                    // String s1 =
                    // "//ldml/segmentations/segmentation[@type=\"LineBreak\"]/variables/variable[@_q=\"0061\"][@id=\"$CB\"] ";
                    // String s2 =
                    // "//ldml/segmentations/segmentation[@type=\"LineBreak\"]/variables/variable[@_q=\"003A\"][@id=\"$CB\"]";
                    // System.out.println(k.ldmlComparator.compare(s1, s2));
                    if (mergeFactory != null) {
                        int mergeOption = CLDRFile.MERGE_ADD_ALTERNATE;
                        CLDRFile toMergeIn = mergeFactory.make(join_prefix + test + join_postfix, false)
                            .cloneAsThawed();
                        if (toMergeIn != null) {
                            if (options[JOIN_ARGS].doesOccur) {
                                if (options[JOIN_ARGS].value.indexOf("r") >= 0)
                                    mergeOption = CLDRFile.MERGE_REPLACE_MY_DRAFT;
                                if (options[JOIN_ARGS].value.indexOf("d") >= 0)
                                    mergeOption = CLDRFile.MERGE_REPLACE_MINE;
                                if (options[JOIN_ARGS].value.indexOf("c") >= 0) toMergeIn.clearComments();
                                if (options[JOIN_ARGS].value.indexOf("x") >= 0) removePosix(toMergeIn);
                            }
                            toMergeIn.makeDraft(DraftStatus.contributed);
                            k.putAll(toMergeIn, mergeOption);
                        }
                        // special fix
                        k.removeComment(
                            " The following are strings that are not found in the locale (currently), but need valid translations for localizing timezones. ");
                    }
                    if (DEBUG_PATHS != null) {
                        System.out.println("Debug2 (" + test + "):\t" + k.toString(DEBUG_PATHS));
                    }
                    if (options[FIX].doesOccur) {
                        fix(k, options[FIX].value, options[KONFIG].value, cldrFactory);
                    }
                    if (DEBUG_PATHS != null) {
                        System.out.println("Debug3 (" + test + "):\t" + k.toString(DEBUG_PATHS));
                    }
//                    if (options[MINIMIZE].doesOccur) {
//                        if (pathHeaderFactory == null) {
//                            pathHeaderFactory = PathHeader.getFactory(cldrFactory.make("en", true));
//                        }
//                        // TODO, fix identity
//                        String parent = LocaleIDParser.getParent(test);
//                        if (parent != null) {
//                            CLDRFile toRemove = cldrFactory.make(parent, true);
//                            // remove the items that are language codes, script codes, or region codes
//                            // since they may be real translations.
//                            if (retainIfTrue == null) {
//                                retainIfTrue = new RetainWhenMinimizing();
//                            }
//                            retainIfTrue.setParentFile(toRemove);
//                            List<String> removed = DEBUG ? null : new ArrayList<String>();
//                            k.removeDuplicates(toRemove, COMMENT_REMOVALS, retainIfTrue, removed);
//                            if (removed != null && removed.size() != 0) {
//                                totalRemoved += removed.size();
//                                Set<PathHeader> sorted = new TreeSet<PathHeader>();
//                                for (String path : removed) {
//                                    sorted.add(pathHeaderFactory.fromPath(path));
//                                }
//                                for (PathHeader pathHeader : sorted) {
//                                    System.out.println("\t# " + test + "\t" + pathHeader + "\t" + pathHeader.getOriginalPath());
//                                }
//                                System.out.println("\t# " + test + "\t# Removed:\t" + removed.size());
//                            }
//                        }
//                    }
                    // System.out.println(CLDRFile.getAttributeOrder());

                    /*
                     * if (false) {
                     * Map tempComments = k.getXpath_comments();
                     *
                     * for (Iterator it2 = tempComments.keySet().iterator(); it2.hasNext();) {
                     * String key = (String) it2.next();
                     * String comment = (String) tempComments.get(key);
                     * Log.logln("Writing extra comment: " + key);
                     * System.out.println(key + "\t comment: " + comment);
                     * }
                     * }
                     */

                    if (DEBUG_PATHS != null) {
                        System.out.println("Debug4 (" + test + "):\t" + k.toString(DEBUG_PATHS));
                    }

                    PrintWriter pw = FileUtilities.openUTF8Writer(targetDir, test + ".xml");
                    String testPath = "//ldml/dates/calendars/calendar[@type=\"persian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]";
                    if (false) {
                        System.out.println("Printing Raw File:");
                        testPath = "//ldml/dates/calendars/calendar[@type=\"persian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/alias";
                        System.out.println(k.getStringValue(testPath));
                        // System.out.println(k.getFullXPath(testPath));
                        TreeSet s = new TreeSet();
                        k.forEach(s::add);

                        System.out.println(k.getStringValue(testPath));
                        // if (true) return;
                        Set orderedSet = new TreeSet(k.getComparator());
                        k.forEach(orderedSet::add);
                        for (Iterator it3 = orderedSet.iterator(); it3.hasNext();) {
                            String path = (String) it3.next();
                            // System.out.println(path);
                            if (path.equals(testPath)) {
                                System.out.println("huh?");
                            }
                            String value = k.getStringValue(path);
                            String fullpath = k.getFullXPath(path);
                            System.out.println("\t=\t" + fullpath);
                            System.out.println("\t=\t" + value);
                        }
                        System.out.println("Done Printing Raw File:");
                    }

                    k.write(pw);
                    // pw.println();
                    pw.close();
                    if (options[CHECK].doesOccur) {
                        QuickCheck.check(new File(targetDir, test + ".xml"));
                    }

                    // JCE: I don't think anyone really uses the .bat files from CLDRModify any more, since
                    // Eclipse provides a decent file comparison program.  You can comment this back in if
                    // you need it, but I found that sometimes having this here clobbers the real output
                    // file, which we definitely don't want.
                    // ToolUtilities.generateBat(sourceDir, test + ".xml", targetDir, test + ".xml", lineComparer);

                    /*
                     * boolean ok = Utility.areFileIdentical(sourceDir + test + ".xml",
                     * targetDir + test + ".xml", failureLines, Utility.TRIM + Utility.SKIP_SPACES);
                     * if (!ok) {
                     * System.out.println("Found differences at: ");
                     * System.out.println("\t" + failureLines[0]);
                     * System.out.println("\t" + failureLines[1]);
                     * }
                     */
                }
                if (totalSkeletons.size() != 0) {
                    System.out.println("Total Skeletons" + totalSkeletons);
                }
                if (totalRemoved > 0) {
                    System.out.println("# Removed:\t" + totalRemoved);
                }
            } finally {
                fixList.handleCleanup();
                // Log.close();
                System.out.println("Done -- Elapsed time: " + ((System.currentTimeMillis() - startTime) / 60000.0)
                    + " minutes");
            }
        }
    }

    private static String removeSuffix(String value, String... suffices) {
        for (String suffix : suffices) {
            if (value.endsWith(suffix)) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    /*
     * Use the coverage to determine what we should keep in the case of a locale just below root.
     */

    static class RetainWhenMinimizing implements CLDRFile.RetentionTest {
        private CLDRFile file;
        private CLDRLocale c;
        private boolean isArabicSublocale;
        // Status status = new Status(); // no need to have, was unused

        public RetainWhenMinimizing setParentFile(CLDRFile file) {
            this.file = file;
            this.c = CLDRLocale.getInstance(file.getLocaleIDFromIdentity());
            isArabicSublocale = "ar".equals(c.getLanguage()) && !"001".equals(c.getCountry());
            return this;
        }

        @Override
        public Retention getRetention(String path) {
            if (path.startsWith("//ldml/identity/")) {
                return Retention.RETAIN;
            }
            // special case for Arabic
            if (isArabicSublocale && path.startsWith("//ldml/numbers/defaultNumberingSystem")) {
                return Retention.RETAIN;
            }
            String localeId = file.getSourceLocaleID(path, null);
            if ((c.isLanguageLocale() || c.equals(CLDRLocale.getInstance("pt_PT")))
                && (XMLSource.ROOT_ID.equals(localeId) || XMLSource.CODE_FALLBACK_ID.equals(localeId))) {
                return Retention.RETAIN;
            }
            return Retention.RETAIN_IF_DIFFERENT;
        }
    }

    static final Splitter COMMA_SEMI = Splitter.on(Pattern.compile("[,;|]")).trimResults().omitEmptyStrings();
    protected static final boolean NUMBER_SYSTEM_HACK = true;

    /**
     *
     */
    private static void checkSuboptions(UOption givenOptions, UnicodeSet allowedOptions) {
        if (givenOptions.doesOccur && !allowedOptions.containsAll(givenOptions.value)) {
            throw new IllegalArgumentException("Illegal sub-options for "
                + givenOptions.shortName
                + ": "
                + new UnicodeSet().addAll(givenOptions.value).removeAll(allowedOptions)
                + CldrUtility.LINE_SEPARATOR + "Use -? for help.");
        }
    }

    /**
     *
     */
    private static void removePosix(CLDRFile toMergeIn) {
        Set<String> toRemove = new HashSet<>();
        for (String xpath : toMergeIn) {
            if (xpath.startsWith("//ldml/posix")) toRemove.add(xpath);
        }
        toMergeIn.removeAll(toRemove, false);
    }

    // private static class References {
    // static Map<String,Map<String,String>> locale_oldref_newref = new TreeMap<String,Map<String,String>>();
    //
    // static String[][] keys = {{"standard", "S", "[@standard=\"true\"]"}, {"references", "R", ""}};
    // UnicodeSet digits = new UnicodeSet("[0-9]");
    // int referenceCounter = 0;
    // Map references_token = new TreeMap();
    // Set tokenSet = new HashSet();
    // String[] keys2;
    // boolean isStandard;
    // References(boolean standard) {
    // isStandard = standard;
    // keys2 = standard ? keys[0] : keys[1];
    // }
    // /**
    // *
    // */
    // public void reset(CLDRFile k) {
    // }
    // /**
    // *
    // */
    // // Samples:
    // // <language type="ain" references="RP1">阿伊努文</language>
    // // <reference type="R1" uri="http://www.info.gov.hk/info/holiday_c.htm">二零零五年公眾假期刊登憲報</reference>
    // private int fix(Map attributes, CLDRFile replacements) {
    // // we have to have either a references element or attributes.
    // String references = (String) attributes.get(keys2[0]);
    // int result = 0;
    // if (references != null) {
    // references = references.trim();
    // if (references.startsWith("S") || references.startsWith("R")) {
    // if (digits.containsAll(references.substring(1))) return 0;
    // }
    // String token = (String) references_token.get(references);
    // if (token == null) {
    // while (true) {
    // token = keys2[1] + (++referenceCounter);
    // if (!tokenSet.contains(token)) break;
    // }
    // references_token.put(references, token);
    // System.out.println("Adding: " + token + "\t" + references);
    // replacements.add("//ldml/references/reference[@type=\"" + token + "\"]" + keys2[2], references);
    // result = 1;
    // }
    // attributes.put(keys2[0], token);
    // }
    // return result;
    // }
    // }

    static PathChecker pathChecker = new PathChecker();


    abstract static class CLDRFilter {
        protected CLDRFile cldrFileToFilter;
        protected CLDRFile cldrFileToFilterResolved;
        private String localeID;
        protected Set<String> availableChildren;
        private Set<String> toBeRemoved;
        private CLDRFile toBeReplaced;
        protected Factory factory;

        public final void setFile(CLDRFile k, Factory factory, Set<String> removal, CLDRFile replacements) {
            this.cldrFileToFilter = k;
            cldrFileToFilterResolved = null;
            this.factory = factory;
            localeID = k.getLocaleID();
            this.toBeRemoved = removal;
            this.toBeReplaced = replacements;
            handleStart();
        }

        public void handleStart() {
        }

        public abstract void handlePath(String xpath);

        public void handleEnd() {
        }

        public CLDRFile getResolved() {
            if (cldrFileToFilterResolved == null) {
                if (cldrFileToFilter.isResolved()) {
                    cldrFileToFilterResolved = cldrFileToFilter;
                } else {
                    cldrFileToFilterResolved = factory.make(cldrFileToFilter.getLocaleID(), true);
                }
            }
            return cldrFileToFilterResolved;

        }
        public void show(String reason, String detail) {
            System.out.println("%" + localeID + "\t" + reason + "\tConsidering " + detail);
        }

        public void retain(String path, String reason) {
            System.out.println("%" + localeID + "\t" + reason + "\tRetaining: " + cldrFileToFilter.getStringValue(path)
            + "\t at: " + path);
        }

        public void remove(String path) {
            remove(path, "-");
        }

        public void remove(String path, String reason) {
            if (toBeRemoved.contains(path)) return;
            toBeRemoved.add(path);
//            System.out.println("%" + localeID + "\t" + reason + "\tRemoving:\t«"
//                + cldrFileToFilter.getStringValue(path) + "»\t at:\t" + path);
            String oldValueOldPath = cldrFileToFilter.getStringValue(path);
            showAction(reason, "Removing", oldValueOldPath, null, null, path, path);
        }

        public void replace(String oldFullPath, String newFullPath, String newValue) {
            replace(oldFullPath, newFullPath, newValue, "-");
        }

        public void showAction(String reason, String action, String oldValueOldPath, String oldValueNewPath,
            String newValue, String oldFullPath, String newFullPath) {
            System.out.println("%"
                + localeID
                + "\t"
                + action
                + "\t"
                + reason
                + "\t«"
                + oldValueOldPath
                + "»"
                + (newFullPath.equals(oldFullPath) || oldValueNewPath == null ? "" : oldValueNewPath
                    .equals(oldValueOldPath) ? "/=" : "/«" + oldValueNewPath + "»")
                + "\t→\t" + (newValue == null ? "∅" : newValue.equals(oldValueOldPath) ? "≡" : "«" + newValue + "»")
                + "\t" + oldFullPath
                + (newFullPath.equals(oldFullPath) ? "" : "\t→\t" + newFullPath));
        }

        /**
         * There are the following cases, where:
         *
         * <pre>
         * pathSame,    new value null:         Removing    v       p
         * pathSame,    new value not null:     Replacing   v   v'  p
         * pathChanges, nothing at new path:    Moving      v       p   p'
         * pathChanges, same value at new path: Replacing   v   v'  p   p'
         * pathChanges, value changes:          Overriding  v   v'  p   p'
         *
         * <pre>
         * @param oldFullPath
         * @param newFullPath
         * @param newValue
         * @param reason
         */
        public void replace(String oldFullPath, String newFullPath, String newValue, String reason) {
            String oldValueOldPath = cldrFileToFilter.getStringValue(oldFullPath);
            String temp = cldrFileToFilter.getFullXPath(oldFullPath);
            if (temp != null) {
                oldFullPath = temp;
            }
            boolean pathSame = oldFullPath.equals(newFullPath);

            if (!pathChecker.checkPath(newFullPath)) {
                throw new IllegalArgumentException("Bad path: " + newFullPath);
            }

            if (pathSame) {
                if (newValue == null) {
                    remove(oldFullPath, reason);
                } else if (oldValueOldPath == null) {
                    toBeReplaced.add(oldFullPath, newValue);
                    showAction(reason, "Adding", oldValueOldPath, null, newValue, oldFullPath, newFullPath);
                } else {
                    toBeReplaced.add(oldFullPath, newValue);
                    showAction(reason, "Replacing", oldValueOldPath, null, newValue, oldFullPath, newFullPath);
                }
                return;
            }
            String oldValueNewPath = cldrFileToFilter.getStringValue(newFullPath);
            toBeRemoved.add(oldFullPath);
            toBeReplaced.add(newFullPath, newValue);

            if (oldValueNewPath == null) {
                showAction(reason, "Moving", oldValueOldPath, oldValueNewPath, newValue, oldFullPath, newFullPath);
            } else if (oldValueNewPath.equals(newValue)) {
                showAction(reason, "Redundant Value", oldValueOldPath, oldValueNewPath, newValue, oldFullPath, newFullPath);
            } else {
                showAction(reason, "Overriding", oldValueOldPath, oldValueNewPath, newValue, oldFullPath, newFullPath);
            }
        }

        /**
         * Adds a new path-value pair to the CLDRFile.
         * @param path the new path
         * @param value the value
         * @param reason Reason for adding the path and value.
         */
        public void add(String path, String value, String reason) {
            String oldValueOldPath = cldrFileToFilter.getStringValue(path);
            if (oldValueOldPath == null) {
                toBeRemoved.remove(path);
                toBeReplaced.add(path, value);
                showAction(reason, "Adding", oldValueOldPath, null,
                    value, path, path);
            } else {
                replace(path, path, value);
            }
        }

        public CLDRFile getReplacementFile() {
            return toBeReplaced;
        }

        public void handleCleanup() {
        }

        public void handleSetup() {
        }

        public String getLocaleID() {
            return localeID;
        }
    }

    static class FixList {
        // simple class, so we use quick list
        CLDRFilter[] filters = new CLDRFilter[128]; // only ascii
        String[] helps = new String[128]; // only ascii
        UnicodeSet options = new UnicodeSet();
        String inputOptions = null;

        void add(char letter, String help) {
            add(letter, help, null);
        }

        public void handleSetup() {
            for (int i = 0; i < filters.length; ++i) {
                if (filters[i] != null) {
                    filters[i].handleSetup();
                }
            }
        }

        public void handleCleanup() {
            for (int i = 0; i < filters.length; ++i) {
                if (filters[i] != null) {
                    filters[i].handleCleanup();
                }
            }
        }

        public UnicodeSet getOptions() {
            return options;
        }

        void add(char letter, String help, CLDRFilter filter) {
            if (helps[letter] != null) throw new IllegalArgumentException("Duplicate letter: " + letter);
            filters[letter] = filter;
            helps[letter] = help;
            options.add(letter);
        }

        void setFile(CLDRFile file, String inputOptions, Factory factory, Set<String> removal, CLDRFile replacements) {
            this.inputOptions = inputOptions;
            for (int i = 0; i < inputOptions.length(); ++i) {
                char c = inputOptions.charAt(i);
                if (filters[c] != null) {
                    try {
                        filters[c].setFile(file, factory, removal, replacements);
                    } catch (RuntimeException e) {
                        System.err.println("Failure in " + filters[c].localeID + "\t START");
                        throw e;
                    }
                }
            }
        }

        void handleStart() {
            for (int i = 0; i < inputOptions.length(); ++i) {
                char c = inputOptions.charAt(i);
                if (filters[c] != null) {
                    try {
                        filters[c].handleStart();
                    } catch (RuntimeException e) {
                        System.err.println("Failure in " + filters[c].localeID + "\t START");
                        throw e;
                    }
                }
            }
        }

        void handlePath(String xpath) {
            //options = options.toLowerCase();
            for (int i = 0; i < inputOptions.length(); ++i) {
                char c = inputOptions.charAt(i);
                if (filters[c] != null) {
                    try {
                        filters[c].handlePath(xpath);
                    } catch (RuntimeException e) {
                        System.err.println("Failure in " + filters[c].localeID + "\t " + xpath);
                        throw e;
                    }
                }
            }
        }

        void handleEnd() {
            for (int i = 0; i < inputOptions.length(); ++i) {
                char c = inputOptions.charAt(i);
                if (filters[c] != null) {
                    try {
                        filters[c].handleEnd();
                    } catch (RuntimeException e) {
                        System.err.println("Failure in " + filters[c].localeID + "\t START");
                        throw e;
                    }
                }
            }
        }

        String showHelp() {
            String result = "";
            for (int i = 0; i < filters.length; ++i) {
                if (helps[i] != null) {
                    result += "\t" + (char) i + "\t " + helps[i] + XPathParts.NEWLINE;
                }
            }
            return result;
        }
    }

    static Set<String> totalSkeletons = new HashSet<>();

    static Map<String, String> rootUnitMap = new HashMap<>();

    static {
        rootUnitMap.put("second", "s");
        rootUnitMap.put("minute", "min");
        rootUnitMap.put("hour", "h");
        rootUnitMap.put("day", "d");
        rootUnitMap.put("week", "w");
        rootUnitMap.put("month", "m");
        rootUnitMap.put("year", "y");

        fixList.add('z', "Remove deprecated elements", new CLDRFilter() {

            public boolean isDeprecated(DtdType type, String element, String attribute, String value) {
                return DtdData.getInstance(type).isDeprecated(element, attribute, value);
            }

            public boolean isDeprecated(DtdType type, String path) {

                XPathParts parts = XPathParts.getFrozenInstance(path);
                for (int i = 0; i < parts.size(); ++i) {
                    String element = parts.getElement(i);
                    if (isDeprecated(type, element, "*", "*")) {
                        return true;
                    }
                    for (Entry<String, String> entry : parts.getAttributes(i).entrySet()) {
                        String attribute = entry.getKey();
                        String value = entry.getValue();
                        if (isDeprecated(type, element, attribute, value)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public void handlePath(String xpath) {
                String fullPath = cldrFileToFilter.getFullXPath(xpath);
                XPathParts parts = XPathParts.getFrozenInstance(fullPath);
                for (int i = 0; i < parts.size(); ++i) {
                    String element = parts.getElement(i);
                    if (dtdData.isDeprecated(element, "*", "*")) {
                        remove(fullPath, "Deprecated element");
                        return;
                    }
                    for (Entry<String, String> entry : parts.getAttributes(i).entrySet()) {
                        String attribute = entry.getKey();
                        String value = entry.getValue();
                        if (dtdData.isDeprecated(element, attribute, value)) {
                            remove(fullPath, "Element with deprecated attribute(s)");
                        }
                    }
                }
            }
        });

        fixList.add('e', "fix Interindic", new CLDRFilter() {
            @Override
            public void handlePath(String xpath) {
                if (xpath.indexOf("=\"InterIndic\"") < 0) return;
                String v = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                XPathParts fullparts = XPathParts.getFrozenInstance(fullXPath);
                Map<String, String> attributes = fullparts.findAttributes("transform");
                String oldValue = attributes.get("direction");
                if ("both".equals(oldValue)) {
                    attributes.put("direction", "forward");
                    replace(xpath, fullparts.toString(), v);
                }
            }
        });

        fixList.add('B', "fix bogus values", new CLDRFilter() {
            RegexLookup<Integer> paths = RegexLookup.<Integer> of()
                .setPatternTransform(RegexLookup.RegexFinderTransformPath2)
                .add("//ldml/localeDisplayNames/languages/language[@type='([^']*)']", 0)
                .add("//ldml/localeDisplayNames/scripts/script[@type='([^']*)']", 0)
                .add("//ldml/localeDisplayNames/territories/territory[@type='([^']*)']", 0)
                .add("//ldml/dates/timeZoneNames/metazone[@type='([^']*)']", 0)
                .add("//ldml/dates/timeZoneNames/zone[@type='([^']*)']/exemplarCity", 0)
                .add("//ldml/numbers/currencies/currency[@type='([^']*)']/displayName", 0);
            Output<String[]> arguments = new Output<>();
            CLDRFile english = CLDRConfig.getInstance().getEnglish();
            boolean skip;

            @Override
            public void handleStart() {
                CLDRFile resolved = factory.make(cldrFileToFilter.getLocaleID(), true);
                UnicodeSet exemplars = resolved.getExemplarSet(ExemplarType.main, WinningChoice.WINNING);
                skip = exemplars.containsSome('a', 'z');
                // TODO add simpler way to skip file entirely
            }

            @Override
            public void handlePath(String xpath) {
                if (skip) {
                    return;
                }
                Integer lookupValue = paths.get(xpath, null, arguments);
                if (lookupValue == null) {
                    return;
                }
                String type = arguments.value[1];
                String value = cldrFileToFilter.getStringValue(xpath);
                if (value.equals(type)) {
                    remove(xpath, "Matches code");
                    return;
                }
                String evalue = english.getStringValue(xpath);
                if (value.equals(evalue)) {
                    remove(xpath, "Matches English");
                    return;
                }
            }
        });

        fixList.add('s', "fix alt accounting", new CLDRFilter() {
            @Override
            public void handlePath(String xpath) {
                XPathParts parts = XPathParts.getFrozenInstance(xpath);
                if (!parts.containsAttributeValue("alt", "accounting")) {
                    return;
                }
                String oldFullXPath = cldrFileToFilter.getFullXPath(xpath);
                String value = cldrFileToFilter.getStringValue(xpath);
                XPathParts fullparts = XPathParts.getFrozenInstance(oldFullXPath).cloneAsThawed(); // not frozen, for removeAttribute
                fullparts.removeAttribute("pattern", "alt");
                fullparts.setAttribute("currencyFormat", "type", "accounting");
                String newFullXPath = fullparts.toString();
                replace(oldFullXPath, newFullXPath, value, "Move alt=accounting value to new path");
            }
        });

        fixList.add('n', "add unit displayName", new CLDRFilter() {
            @Override
            public void handlePath(String xpath) {
                if (xpath.indexOf("/units/unitLength[@type=\"long\"]") < 0 || xpath.indexOf("/unitPattern[@count=\"other\"]") < 0 ||
                    xpath.indexOf("[@draft=\"unconfirmed\"]") >= 0) {
                    return;
                }
                String value = cldrFileToFilter.getStringValue(xpath);
                String newValue = null;
                if (value.startsWith("{0}")) {
                    newValue = value.substring(3).trim();
                } else if (value.endsWith("{0}")) {
                    newValue = value.substring(0, value.length() - 3).trim();
                } else {
                    System.out.println("unitPattern-other does not start or end with \"{0}\": \"" + value + "\"");
                    return;
                }

                String oldFullXPath = cldrFileToFilter.getFullXPath(xpath);
                String newFullXPath = oldFullXPath.substring(0, oldFullXPath.indexOf("unitPattern")).concat("displayName[@draft=\"provisional\"]");
                add(newFullXPath, newValue, "create unit displayName-long from unitPattern-long-other");
                String newFullXPathShort = newFullXPath.replace("[@type=\"long\"]", "[@type=\"short\"]");
                add(newFullXPathShort, newValue, "create unit displayName-short from unitPattern-long-other");
            }
        });

        fixList.add('x', "retain paths", new CLDRFilter() {
            Matcher m = null;

            @Override
            public void handlePath(String xpath) {
                if (m == null) {
                    m = PatternCache.get(options[PATH].value).matcher("");
                }
                //String v = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                if (!m.reset(fullXPath).matches()) {
                    remove(xpath);
                }
            }
        });

//        fixList.add('_', "remove superfluous compound language translations", new CLDRFilter() {
//            private CLDRFile resolved;
//
//            public void handleStart() {
//                resolved = factory.make(cldrFileToFilter.getLocaleID(), true);
//            }
//
//            public void handlePath(String xpath) {
//                if (!xpath.contains("_")) return;
//                if (!xpath.contains("/language")) return;
//                XPathParts parts = XPathParts.getFrozenInstance(xpath);
//                String languageCode = parts.findAttributeValue("language", "type");
//                String v = resolved.getStringValue(xpath);
//                if (v.equals(languageCode)) {
//                    remove(xpath, "same as language code");
//                    return;
//                }
//                String generatedTranslation = resolved.getName(languageCode, true);
//                if (v.equals(generatedTranslation)) {
//                    remove(xpath, "superfluous compound language");
//                }
//                String spacelessGeneratedTranslation = generatedTranslation.replace(" ", "");
//                if (v.equals(spacelessGeneratedTranslation)) {
//                    remove(xpath, "superfluous compound language (after removing space)");
//                }
//            }
//        });

        fixList.add('l', "change language code", new CLDRFilter() {
            private CLDRFile resolved;

            @Override
            public void handleStart() {
                resolved = factory.make(cldrFileToFilter.getLocaleID(), true);
            }

            @Override
            public void handlePath(String xpath) {
                if (!xpath.contains("/language")) {
                    return;
                }
                XPathParts parts = XPathParts.getFrozenInstance(xpath);
                String languageCode = parts.findAttributeValue("language", "type");
                String v = resolved.getStringValue(xpath);
                if (!languageCode.equals("swc")) {
                    return;
                }
                parts = parts.cloneAsThawed();
                parts.setAttribute("language", "type", "sw_CD");
                replace(xpath, parts.toString(), v);
            }
        });

//        fixList.add('m', "remove multiple alt-variants", new CLDRFilter() {
//
//            public void handleStart() {
//            }
//
//            public void handlePath(String xpath) {
//                XPathParts parts = XPathParts.getFrozenInstance(xpath);
//                if (!parts.containsAttributeValue("alt", "variant")) {
//                    return;
//                }
//                String variantValue = cldrFileToFilter.getStringValue(xpath);
//                String nonVariantXpath = xpath.replaceAll("\\[\\@alt=\"variant\"\\]", "");
//                String nonVariantValue = cldrFileToFilter.getStringValue(nonVariantXpath);
//                if (variantValue.equals(nonVariantValue)) {
//                    remove(xpath, "removing superfluous alt-variant value");
//                }
//            }
//        });

        fixList.add('g', "Swap alt/non-alt values for Czechia", new CLDRFilter() {

            @Override
            public void handleStart() {
            }

            @Override
            public void handlePath(String xpath) {
                XPathParts parts = XPathParts.getFrozenInstance(xpath);
                if (!parts.containsAttributeValue("alt", "variant") || !parts.containsAttributeValue("type", "CZ")) {
                    return;
                }
                String variantValue = cldrFileToFilter.getStringValue(xpath);
                String nonVariantXpath = xpath.replaceAll("\\[\\@alt=\"variant\"\\]", "");
                String nonVariantValue = cldrFileToFilter.getStringValue(nonVariantXpath);
                replace(xpath, xpath, nonVariantValue);
                replace(nonVariantXpath, nonVariantXpath, variantValue);
            }
        });

        fixList.add('u', "fix duration unit patterns", new CLDRFilter() {

            @Override
            public void handlePath(String xpath) {
                if (!xpath.contains("/units")) {
                    return;
                }
                if (!xpath.contains("/durationUnitPattern")) {
                    return;
                }

                String value = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);

                XPathParts parts = XPathParts.getFrozenInstance(fullXPath);
                String unittype = parts.findAttributeValue("durationUnit", "type");

                String newFullXpath = "//ldml/units/durationUnit[@type=\"" + unittype + "\"]/durationUnitPattern";
                replace(fullXPath, newFullXpath, value, "converting to new duration unit structure");
            }
        });

        fixList.add('a', "Fix 0/1", new CLDRFilter() {
            final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();
            PluralInfo info;

            @Override
            public void handleStart() {
                info = SupplementalDataInfo.getInstance().getPlurals(super.localeID);
            }

            @Override
            public void handlePath(String xpath) {

                if (xpath.indexOf("count") < 0) {
                    return;
                }
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                XPathParts parts = XPathParts.getFrozenInstance(fullpath).cloneAsThawed(); // not frozen, for setAttribute
                String countValue = parts.getAttributeValue(-1, "count");
                if (!DIGITS.containsAll(countValue)) {
                    return;
                }
                int intValue = Integer.parseInt(countValue);
                Count count = info.getCount(intValue);
                parts.setAttribute(-1, "count", count.toString());
                String newPath = parts.toString();
                String oldValue = cldrFileToFilter.getStringValue(newPath);
                String value = cldrFileToFilter.getStringValue(xpath);
                if (oldValue != null) {
                    String fixed = oldValue.replace("{0}", countValue);
                    if (value.equals(oldValue)
                        || value.equals(fixed)) {
                        remove(fullpath, "Superfluous given: "
                            + count + "→«" + oldValue + "»");
                    } else {
                        remove(fullpath, "Can’t replace: "
                            + count + "→«" + oldValue + "»");
                    }
                    return;
                }
                replace(fullpath, newPath, value, "Moving 0/1");
            }
        });

        fixList.add('b', "Prep for bulk import", new CLDRFilter() {

            @Override
            public void handlePath(String xpath) {
                if (!options[USER].doesOccur) {
                    return;
                }
                String userID = options[USER].value;
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                String value = cldrFileToFilter.getStringValue(xpath);
                XPathParts parts = XPathParts.getFrozenInstance(fullpath).cloneAsThawed(); // not frozen, for addAttribute
                parts.addAttribute("draft", "unconfirmed");
                parts.addAttribute("alt", "proposed-u" + userID + "-implicit1.8");
                String newPath = parts.toString();
                replace(fullpath, newPath, value);
            }
        });

        fixList.add('c', "Fix transiton from an old currency code to a new one", new CLDRFilter() {
            @Override
            public void handlePath(String xpath) {
                String oldCurrencyCode = "VEF";
                String newCurrencyCode = "VES";
                int fromDate = 2008;
                int toDate = 2018;
                String leadingParenString = " (";
                String trailingParenString = ")";
                String separator = "\u2013";
                String languageTag = "root";

                if (xpath.indexOf("/currency[@type=\"" + oldCurrencyCode + "\"]/displayName") < 0) {
                    return;
                }
                String value = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                String newFullXPath = fullXPath.replace(oldCurrencyCode, newCurrencyCode);
                cldrFileToFilter.add(newFullXPath, value);

                // Exceptions for locales that use an alternate numbering system or a different format for the dates at
                // the end.
                // Add additional ones as necessary
                String localeID = cldrFileToFilter.getLocaleID();
                if (localeID.equals("ne")) {
                    languageTag = "root-u-nu-deva";
                } else if (localeID.equals("bn")) {
                    languageTag = "root-u-nu-beng";
                } else if (localeID.equals("ar")) {
                    leadingParenString = " - ";
                    trailingParenString = "";
                } else if (localeID.equals("fa")) {
                    languageTag = "root-u-nu-arabext";
                    separator = Utility.unescape(" \\u062A\\u0627 ");
                }

                NumberFormat nf = NumberFormat.getInstance(ULocale.forLanguageTag(languageTag));
                nf.setGroupingUsed(false);

                String tagString = leadingParenString + nf.format(fromDate) + separator + nf.format(toDate)
                + trailingParenString;

                replace(fullXPath, fullXPath, value + tagString);

            }
        });

        fixList.add('p', "input-processor", new CLDRFilter() {
            private DisplayAndInputProcessor inputProcessor;

            @Override
            public void handleStart() {
                inputProcessor = new DisplayAndInputProcessor(cldrFileToFilter, true);
            }

            @Override
            public void handleEnd() {
                inputProcessor = null; // clean up, just in case
            }

            @Override
            public void handlePath(String xpath) {
                String value = cldrFileToFilter.getStringValue(xpath);
                if (!value.equals(value.trim())) {
                    value = value; // for debugging
                }
                String newValue = inputProcessor.processInput(xpath, value, null);
                if (value.equals(newValue)) {
                    return;
                }
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                replace(fullXPath, fullXPath, newValue);
            }
        });

        fixList.add('t', "Fix missing count values groups", new CLDRFilter() {

            @Override
            public void handlePath(String xpath) {
                if (xpath.indexOf("@count=\"other\"") < 0) {
                    return;
                }

                String value = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                String[] missingCounts = { "one" };
                for (String count : missingCounts) {
                    String newFullXPath = fullXPath.replace("other", count);
                    if (cldrFileToFilter.getWinningValue(newFullXPath) == null) {
                        add(newFullXPath, value, "Adding missing plural form");
                    }
                }

            }
        });

        fixList.add('f', "NFC (all but transforms, exemplarCharacters, pc, sc, tc, qc, ic)", new CLDRFilter() {
            @Override
            public void handlePath(String xpath) {
                if (xpath.indexOf("/segmentation") >= 0
                    || xpath.indexOf("/transforms") >= 0
                    || xpath.indexOf("/exemplarCharacters") >= 0
                    || xpath.indexOf("/pc") >= 0
                    || xpath.indexOf("/sc") >= 0
                    || xpath.indexOf("/tc") >= 0
                    || xpath.indexOf("/qc") >= 0
                    || xpath.indexOf("/ic") >= 0) return;
                String value = cldrFileToFilter.getStringValue(xpath);
                String nfcValue = Normalizer.compose(value, false);
                if (value.equals(nfcValue)) return;
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                replace(fullXPath, fullXPath, nfcValue);
            }
        });

        fixList.add('v', "remove illegal codes", new CLDRFilter() {

            /*
             * Set legalCurrencies;
             * }
             * {
             * StandardCodes sc = StandardCodes.make();
             * legalCurrencies = new TreeSet(sc.getAvailableCodes("currency"));
             * // first remove non-ISO
             * for (Iterator it = legalCurrencies.iterator(); it.hasNext();) {
             * String code = (String) it.next();
             * List data = sc.getFullData("currency", code);
             * if ("X".equals(data.get(3))) it.remove();
             * }
             * }
             */
            StandardCodes sc = StandardCodes.make();
            String[] codeTypes = { "language", "script", "territory", "currency" };

            @Override
            public void handlePath(String xpath) {
                if (xpath.indexOf("/currency") < 0
                    && xpath.indexOf("/timeZoneNames") < 0
                    && xpath.indexOf("/localeDisplayNames") < 0) return;
                XPathParts parts = XPathParts.getFrozenInstance(xpath);
                String code;
                for (int i = 0; i < codeTypes.length; ++i) {
                    code = parts.findAttributeValue(codeTypes[i], "type");
                    if (code != null) {
                        if (!sc.getGoodAvailableCodes(codeTypes[i]).contains(code)) remove(xpath);
                        return;
                    }
                }
                code = parts.findAttributeValue("zone", "type");
                if (code != null) {
                    if (code.indexOf("/GMT") >= 0) remove(xpath);
                }

            }
        });

        fixList.add('w', "fix alt='...proposed' when there is no alternative", new CLDRFilter() {
            private Set<String> newFullXPathSoFar = new HashSet<>();

            @Override
            public void handlePath(String xpath) {
                if (xpath.indexOf("proposed") < 0) return;
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                XPathParts parts = XPathParts.getFrozenInstance(fullXPath).cloneAsThawed(); // not frozen, for removeProposed
                String newFullXPath = parts.removeProposed().toString();
                // now see if there is an uninherited value
                String value = cldrFileToFilter.getStringValue(xpath);
                String baseValue = cldrFileToFilter.getStringValue(newFullXPath);
                if (baseValue != null) {
                    // if the value AND the fullxpath are the same as what we have, then delete
                    if (value.equals(baseValue)) {
                        String baseFullXPath = cldrFileToFilter.getFullXPath(newFullXPath);
                        if (baseFullXPath.equals(newFullXPath)) {
                            remove(xpath, "alt=base");
                        }
                    }
                    return; // there is, so skip
                }
                // there isn't, so modif if we haven't done so already
                if (!newFullXPathSoFar.contains(newFullXPath)) {
                    replace(fullXPath, newFullXPath, value);
                    newFullXPathSoFar.add(newFullXPath);
                }
            }
        });
        /*
         * Fix id to be identical to skeleton
         * Eliminate any single-field ids
         * Add "L" (stand-alone month), "?" (other stand-alones)
         * Remove any fields with both a date and a time
         * Test that datetime format is valid format (will have to fix by hand)
         * Map k, K to H, h
         *
         * In Survey Tool: don't show id; compute when item added or changed
         * test validity
         */

        fixList.add('d', "fix dates", new CLDRFilter() {
            DateTimePatternGenerator dateTimePatternGenerator = DateTimePatternGenerator.getEmptyInstance();
            DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
            Map<String, Set<String>> seenSoFar = new HashMap<>();

            @Override
            public void handleStart() {
                seenSoFar.clear();
            }

            @Override
            public void handlePath(String xpath) {
                // timeFormatLength type="full"
                if (xpath.contains("timeFormatLength") && xpath.contains("full")) {
                    String fullpath = cldrFileToFilter.getFullXPath(xpath);
                    String value = cldrFileToFilter.getStringValue(xpath);
                    boolean gotChange = false;
                    List<Object> list = formatParser.set(value).getItems();
                    for (int i = 0; i < list.size(); ++i) {
                        Object item = list.get(i);
                        if (item instanceof DateTimePatternGenerator.VariableField) {
                            String itemString = item.toString();
                            if (itemString.charAt(0) == 'z') {
                                list.set(i, new VariableField(Utility.repeat("v", itemString.length())));
                                gotChange = true;
                            }
                        }
                    }
                    if (gotChange) {
                        String newValue = toStringWorkaround();
                        if (value != newValue) {
                            replace(xpath, fullpath, newValue);
                        }
                    }
                }
                if (xpath.indexOf("/availableFormats") < 0) {
                    return;
                }
                String value = cldrFileToFilter.getStringValue(xpath);
                if (value == null) {
                    return; // not in current file
                }

                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                XPathParts fullparts = XPathParts.getFrozenInstance(fullpath);
                Map<String, String> attributes = fullparts.findAttributes("dateFormatItem");
                String id = attributes.get("id");
                String oldID = id;
                try {
                    id = dateTimePatternGenerator.getBaseSkeleton(id);
                    if (id.equals(oldID)) {
                        return;
                    }
                    System.out.println(oldID + " => " + id);
                } catch (RuntimeException e) {
                    id = "[error]";
                    return;
                }

                attributes.put("id", id);
                totalSkeletons.add(id);

                replace(xpath, fullparts.toString(), value);
            }

            private String toStringWorkaround() {
                StringBuffer result = new StringBuffer();
                List<Object> items = formatParser.getItems();
                for (int i = 0; i < items.size(); ++i) {
                    Object item = items.get(i);
                    if (item instanceof String) {
                        result.append(formatParser.quoteLiteral((String) items.get(i)));
                    } else {
                        result.append(items.get(i).toString());
                    }
                }
                return result.toString();
            }

        });

        fixList.add('y', "fix years to be y (with exceptions)", new CLDRFilter() {
            DateTimeCanonicalizer dtc = new DateTimeCanonicalizer(true);

            DateTimePatternGenerator dateTimePatternGenerator = DateTimePatternGenerator.getEmptyInstance();
            DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
            Map<String, Set<String>> seenSoFar = new HashMap<>();

            @Override
            public void handleStart() {
                seenSoFar.clear();
            }

            @Override
            public void handlePath(String xpath) {
                DateTimePatternType datetimePatternType = DateTimePatternType.fromPath(xpath);

                // check to see if we need to change the value

                if (!DateTimePatternType.STOCK_AVAILABLE_INTERVAL_PATTERNS.contains(datetimePatternType)) {
                    return;
                }
                String oldValue = cldrFileToFilter.getStringValue(xpath);
                String value = dtc.getCanonicalDatePattern(xpath, oldValue, datetimePatternType);

                String fullPath = cldrFileToFilter.getFullXPath(xpath);
                // Deleted code to canonicalize id for availableFormats items (cldrbug 5760)

                if (value.equals(oldValue)) {
                    return;
                }

                // made it through the gauntlet, so replace

                replace(xpath, fullPath, value);
            }
        });

        // This should only be applied to specific locales, and the results checked manually afterward.
        // It will only create ranges using the same digits as in root, not script-specific digits.
        // Any pre-existing year ranges should use the range marker from the intervalFormats "y" item.
        // This make several assumptions and is somewhat *FRAGILE*.
        fixList.add('j', "add year ranges from root to Japanese calendar eras", new CLDRFilter() {
            private CLDRFile rootFile;

            @Override
            public void handleStart() {
                rootFile = factory.make("root", false);
            }

            @Override
            public void handlePath(String xpath) {
                // Skip paths we don't care about
                if (xpath.indexOf("/calendar[@type=\"japanese\"]/eras/era") < 0) return;
                // Get root name for the era, check it
                String rootEraValue = rootFile.getStringValue(xpath);
                int rootEraIndex = rootEraValue.indexOf(" (");
                if (rootEraIndex < 0) return; // this era does not have a year range in root, no need to add one in this
                // locale
                // Get range marker from intervalFormat range for y
                String yearIntervalFormat = cldrFileToFilter
                    .getStringValue(
                        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"y\"]/greatestDifference[@id=\"y\"]");
                if (yearIntervalFormat == null) return; // oops, no intervalFormat data for y
                String rangeMarker = yearIntervalFormat.replaceAll("[.y\u5E74\uB144]", ""); // *FRAGILE* strip out
                // everything except the
                // range-indicating part
                // Get current locale name for this era, check it
                String eraValue = cldrFileToFilter.getStringValue(xpath);
                if (eraValue.indexOf('(') >= 0 && eraValue.indexOf(rangeMarker) >= 0) return; // this eraValue already
                // has a year range that
                // uses the appropriate
                // rangeMarker
                // Now update the root year range it with the rangeMarker for this locale, and append it to this
                // locale's name
                String rootYearRange = rootEraValue.substring(rootEraIndex);
                String appendYearRange = rootYearRange.replaceAll("[\u002D\u2013]", rangeMarker);
                String newEraValue = eraValue.concat(appendYearRange);
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                replace(xpath, fullpath, newEraValue);
                // System.out.println("CLDRModify fj: rootEraValue: \"" + rootEraValue + "\", eraValue: \"" + eraValue +
                // "\", rangeMarker: \"" + rangeMarker + "\"");
            }
        });

        fixList.add('r', "fix references and standards", new CLDRFilter() {
            int currentRef = 500;
            Map<String, TreeMap<String, String>> locale_oldref_newref = new TreeMap<>();
            TreeMap<String, String> oldref_newref;

            //LanguageTagParser ltp = new LanguageTagParser();

            // References standards = new References(true);
            // References references = new References(false);

            @Override
            public void handleStart() {
                String locale = cldrFileToFilter.getLocaleID();
                oldref_newref = locale_oldref_newref.get(locale);
                if (oldref_newref == null) {
                    oldref_newref = new TreeMap<>();
                    locale_oldref_newref.put(locale, oldref_newref);
                }
            }

            // // Samples:
            // // <language type="ain" references="RP1">阿伊努文</language>
            // // <reference type="R1" uri="http://www.info.gov.hk/info/holiday_c.htm">二零零五年公眾假期刊登憲報</reference>
            @Override
            public void handlePath(String xpath) {
                // must be minimised for this to work.
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                if (!fullpath.contains("reference")) return;
                String value = cldrFileToFilter.getStringValue(xpath);
                XPathParts fullparts = XPathParts.getFrozenInstance(fullpath).cloneAsThawed(); // can't be frozen
                if ("reference".equals(fullparts.getElement(-1))) {
                    fixType(value, "type", fullpath, fullparts);
                } else if (fullparts.getAttributeValue(-1, "references") != null) {
                    fixType(value, "references", fullpath, fullparts);
                } else {
                    System.out.println("CLDRModify: Skipping: " + xpath);
                }
            }

            /**
             *
             * @param value
             * @param type
             * @param oldFullPath
             * @param fullparts the XPathParts -- must not be frozen, for addAttribute
             */
            private void fixType(String value, String type, String oldFullPath, XPathParts fullparts) {
                String ref = fullparts.getAttributeValue(-1, type);
                if (whitespace.containsSome(ref)) {
                    throw new IllegalArgumentException("Whitespace in references");
                }
                String newRef = getNewRef(ref);
                fullparts.addAttribute(type, newRef);
                replace(oldFullPath, fullparts.toString(), value);
            }

            private String getNewRef(String ref) {
                String newRef = oldref_newref.get(ref);
                if (newRef == null) {
                    newRef = String.valueOf(currentRef++);
                    newRef = "R" + Utility.repeat("0", (3 - newRef.length())) + newRef;
                    oldref_newref.put(ref, newRef);
                }
                return newRef;
            }
        });

        fixList.add('q', "fix annotation punctuation", new CLDRFilter() {
            @Override
            public void handlePath(String xpath) {
                if (!xpath.contains("/annotation")) {
                    return;
                }
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                XPathParts parts = XPathParts.getFrozenInstance(fullpath);
                String cp = parts.getAttributeValue(2, "cp");
                String tts = parts.getAttributeValue(2, "tts");
                String type = parts.getAttributeValue(2, "type");
                if ("tts".equals(type)) {
                    return; // ok, skip
                }
                parts = parts.cloneAsThawed();
                String hex = "1F600";
                if (cp.startsWith("[")) {
                    UnicodeSet us = new UnicodeSet(cp);
                    if (us.size() == 1) {
                        cp = us.iterator().next();
                        hex = Utility.hex(cp);
                    } else {
                        hex = us.toString();
                    }
                    parts.putAttributeValue(2, "cp", cp);
                }
                parts.removeAttribute(2, "tts");
                if (tts != null) {
                    String newTts = CldrUtility.join(COMMA_SEMI.splitToList(tts), ", ");
                    XPathParts parts2 = parts.cloneAsThawed();
                    parts2.putAttributeValue(2, "type", "tts");
                    add(parts2.toString(), newTts, "separate tts");
                }
                String value = cldrFileToFilter.getStringValue(xpath);
                String newValue = CldrUtility.join(COMMA_SEMI.splitToList(value), " | ");
                final String newFullPath = parts.toString();
                Comments comments = cldrFileToFilter.getXpath_comments();
                String comment = comments.removeComment(CommentType.PREBLOCK, xpath);
                comment = hex + (comment == null ? "" : " " + comment);
                comments.addComment(CommentType.PREBLOCK, newFullPath, comment);
                if (!fullpath.equals(newFullPath) || !value.equals(newValue)) {
                    replace(fullpath, newFullPath, newValue);
                }
            }
        });

        fixList.add('Q', "add annotation names to keywords", new CLDRFilter() {
            Set<String> available = Annotations.getAvailable();
            TreeSet<String> sorted = new TreeSet<>(Collator.getInstance(ULocale.ROOT));
            CLDRFile resolved;

            @Override
            public void handleStart() {
                String localeID = cldrFileToFilter.getLocaleID();
                if (!available.contains(localeID)) {
                    throw new IllegalArgumentException("no annotations available, probably wrong directory");
                }
                resolved = factory.make(localeID, true);
            }

            @Override
            public void handlePath(String xpath) {
                if (!xpath.contains("/annotation")) {
                    return;
                }
                //      <annotation cp="💯">100 | honderd | persent | telling | vol</annotation>
                //      <annotation cp="💯" type="tts">honderd punte</annotation>
                //      we will copy honderd punte into the list of keywords.
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                XPathParts parts = XPathParts.getFrozenInstance(fullpath);
                String type = parts.getAttributeValue(2, "type");
                if (type == null) {
                    return; // no TTS, so keywords, skip
                }

                XPathParts keywordParts = parts.cloneAsThawed().removeAttribute(2, "type");
                String keywordPath = keywordParts.toString();
                String rawKeywordValue = cldrFileToFilter.getStringValue(keywordPath);

                // skip if keywords AND name are inherited
                if (rawKeywordValue == null || rawKeywordValue.equals(CldrUtility.INHERITANCE_MARKER)) {
                    String rawName = cldrFileToFilter.getStringValue(xpath);
                    if (rawName == null || rawName.equals(CldrUtility.INHERITANCE_MARKER)) {
                        return;
                    }
                }

                // skip if the name is not above root
                String nameSourceLocale = resolved.getSourceLocaleID(xpath, null);
                if ("root".equals(nameSourceLocale) || XMLSource.CODE_FALLBACK_ID.equals(nameSourceLocale)) {
                    return;
                }

                String name = resolved.getStringValue(xpath);
                String keywordValue = resolved.getStringValue(keywordPath);
                String sourceLocaleId = resolved.getSourceLocaleID(keywordPath, null);
                sorted.clear();
                sorted.add(name);
                List<String> items;
                if (!sourceLocaleId.equals(XMLSource.ROOT_ID) && !sourceLocaleId.equals(XMLSource.CODE_FALLBACK_ID)) {
                    items = Annotations.splitter.splitToList(keywordValue);
                    sorted.addAll(items);
                } else {
                    int debug = 0;
                }
                DisplayAndInputProcessor.filterCoveredKeywords(sorted);
                String newKeywordValue = Joiner.on(" | ").join(sorted);
                if (!newKeywordValue.equals(keywordValue)) {
                    replace(keywordPath, keywordPath, newKeywordValue);
                }
            }
        });

        fixList.add('N', "add number symbols to exemplars", new CLDRFilter() {
            CLDRFile resolved;
            UnicodeSet numberStuff = new UnicodeSet();
            Set<String> seen = new HashSet<>();
            Set<String> hackAllowOnly = new HashSet<>();
            boolean skip = false;

            @Override
            public void handleStart() {
                String localeID = cldrFileToFilter.getLocaleID();
                resolved = factory.make(localeID, true);
                numberStuff.clear();
                seen.clear();
                skip = localeID.equals("root");
                // TODO add return value to handleStart to skip calling handlePath

                if (NUMBER_SYSTEM_HACK) {
                    hackAllowOnly.clear();
                    for (NumberingSystem system : NumberingSystem.values()) {
                        String numberingSystem = system.path == null ? "latn" : cldrFileToFilter.getStringValue(system.path);
                        if (numberingSystem != null) {
                            hackAllowOnly.add(numberingSystem);
                        }
                    }
                    int debug = 0;
                }
            }

            @Override
            public void handlePath(String xpath) {
                // the following doesn't work without NUMBER_SYSTEM_HACK, because there are spurious numbersystems in the data.
                // http://unicode.org/cldr/trac/ticket/10648
                // so using a hack for now in handleEnd
                if (skip || !xpath.startsWith("//ldml/numbers/symbols")) {
                    return;
                }

                // //ldml/numbers/symbols[@numberSystem="latn"]/exponential
                XPathParts parts = XPathParts.getFrozenInstance(xpath);
                String system = parts.getAttributeValue(2, "numberSystem");
                if (system == null) {
                    System.err.println("Bogus numberSystem:\t" + cldrFileToFilter.getLocaleID() + " \t" + xpath);
                    return;
                } else if (seen.contains(system) || !hackAllowOnly.contains(system)) {
                    return;
                }
                seen.add(system);
                UnicodeSet exemplars = resolved.getExemplarsNumeric(system);
                System.out.println("# " + system + " ==> " + exemplars.toPattern(false));
                for (String s : exemplars) {
                    numberStuff.addAll(s); // add individual characters
                }
            }

            @Override
            public void handleEnd() {
                if (!numberStuff.isEmpty()) {
                    UnicodeSet current = cldrFileToFilter.getExemplarSet(ExemplarType.numbers, WinningChoice.WINNING);
                    if (!numberStuff.equals(current)) {
                        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(cldrFileToFilter);
                        if (current != null && !current.isEmpty()) {
                            numberStuff.addAll(current);
                        }
                        String path = CLDRFile.getExemplarPath(ExemplarType.numbers);
                        String value = daip.getPrettyPrinter().format(numberStuff);
                        replace(path, path, value);
                    }
                }
            }
        });

        fixList.add('k',
            "fix according to -k config file. Details on http://cldr.unicode.org/development/cldr-big-red-switch/cldrmodify-passes/cldrmodify-config",
            new CLDRFilter() {
            private Map<ConfigMatch, LinkedHashSet<Map<ConfigKeys, ConfigMatch>>> locale2keyValues;
            private LinkedHashSet<Map<ConfigKeys, ConfigMatch>> keyValues = new LinkedHashSet<>();
            private Matcher draftMatcher = Pattern.compile("\\[@draft=\"[^\"]+\"]").matcher("");

            @Override
            public void handleStart() {
                super.handleStart();
                if (!options[FIX].doesOccur || !options[FIX].value.equals("k")) {
                    return;
                }
                if (locale2keyValues == null) {
                    fillCache();
                }
                // set up for the specific locale we are dealing with.
                // a small optimization
                String localeId = getLocaleID();
                keyValues.clear();
                for (Entry<ConfigMatch, LinkedHashSet<Map<ConfigKeys, ConfigMatch>>> localeMatcher : locale2keyValues
                    .entrySet()) {
                    if (localeMatcher.getKey().matches(localeId)) {
                        keyValues.addAll(localeMatcher.getValue());
                    }
                }
                System.out.println("# Checking entries & changing:\t" + keyValues.size());
                for (Map<ConfigKeys, ConfigMatch> entry : keyValues) {
                    ConfigMatch action = entry.get(ConfigKeys.action);
                    //ConfigMatch locale = entry.get(ConfigKeys.locale);
                    ConfigMatch pathMatch = entry.get(ConfigKeys.path);
                    ConfigMatch valueMatch = entry.get(ConfigKeys.value);
                    ConfigMatch newPath = entry.get(ConfigKeys.new_path);
                    ConfigMatch newValue = entry.get(ConfigKeys.new_value);
                    switch (action.action) {
                    // we add all the values up front
                    case addNew:
                    case add:
                        if (pathMatch != null || valueMatch != null || newPath == null || newValue == null) {
                            throw new IllegalArgumentException(
                                "Bad arguments, must have non-null for one of:" +
                                    "path, value, new_path, new_value "
                                    + ":\n\t"
                                    + entry);
                        }
                        String newPathString = newPath.getPath(getResolved());
                        if (action.action == ConfigAction.add
                            || cldrFileToFilter.getStringValue(newPathString) == null) {
                            replace(newPathString, newPathString, newValue.exactMatch, "config");
                        }
                        break;
                        // we just check
                    case replace:
                        if ((pathMatch == null && valueMatch == null) || (newPath == null && newValue == null)) {
                            throw new IllegalArgumentException(
                                "Bad arguments, must have " +
                                    "(path!=null OR value=null) AND (new_path!=null OR new_value!=null):\n\t"
                                    + entry);
                        }
                        break;
                        // For delete, we just check; we'll remove later
                    case delete:
                        if (newPath != null || newValue != null) {
                            throw new IllegalArgumentException("Bad arguments, must have " +
                                "newPath=null, newValue=null"
                                + entry);
                        }
                        break;
                    default: // fall through
                        throw new IllegalArgumentException("Internal Error");
                    }
                }
            }

            private void fillCache() {
                locale2keyValues = new LinkedHashMap<>();
                String configFileName = options[KONFIG].value;
                FileProcessor myReader = new FileProcessor() {
                    {
                        doHash = false;
                    }

                    @Override
                    protected boolean handleLine(int lineCount, String line) {
                        line = line.trim();
//                            if (line.isEmpty()) {
//                                return true;
//                            }
                        String[] lineParts = line.split("\\s*;\\s*");
                        Map<ConfigKeys, ConfigMatch> keyValue = new EnumMap<>(
                            ConfigKeys.class);
                        for (String linePart : lineParts) {
                            int pos = linePart.indexOf('=');
                            if (pos < 0) {
                                throw new IllegalArgumentException(lineCount + ":\t No = in command: «" + linePart + "» in " + line);
                            }
                            ConfigKeys key = ConfigKeys.valueOf(linePart.substring(0, pos).trim());
                            if (keyValue.containsKey(key)) {
                                throw new IllegalArgumentException("Must not have multiple keys: " + key);
                            }
                            String match = linePart.substring(pos + 1).trim();
                            keyValue.put(key, new ConfigMatch(key, match));
                        }
                        final ConfigMatch locale = keyValue.get(ConfigKeys.locale);
                        if (locale == null || keyValue.get(ConfigKeys.action) == null) {
                            throw new IllegalArgumentException();
                        }

                        // validate new path
                        LinkedHashSet<Map<ConfigKeys, ConfigMatch>> keyValues = locale2keyValues
                            .get(locale);
                        if (keyValues == null) {
                            locale2keyValues.put(locale,
                                keyValues = new LinkedHashSet<>());
                        }
                        keyValues.add(keyValue);
                        return true;
                    }
                };
                myReader.process(CLDRModify.class, configFileName);
            }

            @Override
            public void handlePath(String xpath) {
                // slow method; could optimize
                for (Map<ConfigKeys, ConfigMatch> entry : keyValues) {
                    if (xpath.equals("AKUAPEM")) {
                        int debug = 0;
                    }
                    ConfigMatch pathMatch = entry.get(ConfigKeys.path);
                    if (pathMatch != null && !pathMatch.matches(xpath)) {
                        continue;
                    }
                    ConfigMatch valueMatch = entry.get(ConfigKeys.value);
                    String value = cldrFileToFilter.getStringValue(xpath);
                    if (valueMatch != null && !valueMatch.matches(value)) {
                        continue;
                    }
                    ConfigMatch action = entry.get(ConfigKeys.action);
                    switch (action.action) {
                    case delete:
                        remove(xpath, "config");
                        break;
                    case replace:
                        ConfigMatch newPath = entry.get(ConfigKeys.new_path);
                        ConfigMatch newValue = entry.get(ConfigKeys.new_value);

                        String fullpath = cldrFileToFilter.getFullXPath(xpath);
                        String draft = "";
                        int loc = fullpath.indexOf("[@draft=");
                        if (loc >= 0) {
                            int loc2 = fullpath.indexOf(']', loc + 7);
                            draft = fullpath.substring(loc, loc2 + 1);
                        }

                        String modPath = ConfigMatch.getModified(pathMatch, xpath, newPath) + draft;
                        String modValue = ConfigMatch.getModified(valueMatch, value, newValue);
                        replace(xpath, modPath, modValue, "config");
                    }
                }
            }
        });
        fixList.add('i', "fix Identical Children");
        fixList.add('o', "check attribute validity");

        /**
        Goal is: if value in vxml is ^^^, then add ^^^ to trunk IFF
        (a) if there is no value in trunk
        (b) the value in trunk = bailey.
         */
        fixList.add('^', "add inheritance-marked items from vxml to trunk", new CLDRFilter() {
            Factory VxmlFactory;
            final ArrayList<File> fileList = new ArrayList<>();

            @Override
            public void handleStart() {
                if (fileList.isEmpty()) {
                    for (String top : Arrays.asList("common/", "seed/")) {
                        //for (String leaf : Arrays.asList("main/", "annotations/")) {
                        String leaf = sourceInput.contains("annotations") ? "annotations/" : "main/";
                        String key = top + leaf;
                        fileList.add(new File(CLDRPaths.AUX_DIRECTORY + "voting/" + CLDRFile.GEN_VERSION + "/vxml/" + key));
                    }
                    VxmlFactory = SimpleFactory.make(fileList.toArray(new File[fileList.size()]), ".*");
                }

                String localeID = cldrFileToFilter.getLocaleID();

                CLDRFile vxmlCommonMainFile;
                try {
                    vxmlCommonMainFile = VxmlFactory.make(localeID, false);
                } catch (Exception e) {
                    System.out.println("#ERROR: VXML file not found for " + localeID + " in " + fileList);
                    return;
                }
                CLDRFile resolved = cldrFileToFilter;

                if (!cldrFileToFilter.isResolved()) {
                    resolved = factory.make(cldrFileToFilter.getLocaleID(), true);
                }

                for (String xpath : vxmlCommonMainFile) {
                    if (xpath.contains("/language[@type=\"aa\"")) {
                        int debug = 0;
                    }
                    String vxmlValue = vxmlCommonMainFile.getStringValue(xpath);
                    if (vxmlValue == null) {
                        continue;
                    }
                    if (!CldrUtility.INHERITANCE_MARKER.equals(vxmlValue)) {
                        continue;
                    }

                    String trunkValue = resolved.getStringValue(xpath);
                    if (trunkValue != null) {
                        String baileyValue = resolved.getBaileyValue(xpath, null, null);
                        if (!trunkValue.equals(baileyValue)) {
                            continue;
                        }
                    }
                    // at this point, the vxmlValue is ^^^ and the trunk value is either null or == baileyValue


                    // special hack to avoid combined locale names like //ldml/localeDisplayNames/languages/language[@type="en_AU"][@draft="contributed"]

//                    if (xpath.startsWith("//ldml/localeDisplayNames/languages/language[@type=") && xpath.contains("_")) {
//                        continue;
//                    }

                    String fullPath = resolved.getFullXPath(xpath); // get the draft status, etc.
                    if (fullPath == null) { // debugging
                        fullPath = vxmlCommonMainFile.getFullXPath(xpath);
                        if (fullPath == null) {
                            throw new ICUException("getFullXPath not working for " + localeID + ", " + xpath);
                        }
                    }
                    add(fullPath, vxmlValue, "Add or replace by " + CldrUtility.INHERITANCE_MARKER);
                }
            }
            @Override
            public void handlePath(String xpath) {
                // Everything done in handleStart
            }
        });

        fixList.add('L', "fix logical groups by adding all the bailey values", new CLDRFilter() {
            Set<String> seen = new HashSet<>();
            CLDRFile resolved;
            boolean skip;
            CoverageLevel2 coverageLeveler;

            @Override
            public void handleStart() {
                seen.clear();
                resolved = getResolved();
                skip = false;
                coverageLeveler = null;

                String localeID = cldrFileToFilter.getLocaleID();
                LanguageTagParser ltp = new LanguageTagParser().set(localeID);
                if (!ltp.getRegion().isEmpty() || !ltp.getVariants().isEmpty()) {
                    skip = true;
                } else {
                    coverageLeveler = CoverageLevel2.getInstance(localeID);
                }
            }
            @Override
            public void handlePath(String xpath) {
                if (skip
                    || seen.contains(xpath)
                    || coverageLeveler.getLevel(xpath) == Level.COMPREHENSIVE) {
                    return;
                }
                Set<String> paths = LogicalGrouping.getPaths(cldrFileToFilter, xpath);
                if (paths == null || paths.size() < 2) {
                    return;
                }
                Set<String> needed = new LinkedHashSet<>();
                for (String path2 : paths) {
                    if (path2.equals(xpath)) {
                        continue;
                    }
                    if (cldrFileToFilter.isHere(path2)) {
                        continue;
                    }
                    if (LogicalGrouping.isOptional(cldrFileToFilter, path2)) {
                        continue;
                    }
                    // ok, we have a path missing a value
                    needed.add(path2);
                }
                if (needed.isEmpty()) {
                    return;
                }
                // we need at least one value

                // flesh out by adding a bailey value
                // TODO resolve the draft status in a better way
                // For now, get the lowest draft status, and we'll reset everything to that.

                DraftStatus worstStatus = DraftStatus.contributed; // don't ever add an approved.
                for (String path2 : paths) {
                    XPathParts parts = XPathParts.getFrozenInstance(path2);
                    String rawStatus = parts.getAttributeValue(-1, "draft");
                    if (rawStatus == null) {
                        continue;
                    }
                    DraftStatus df = DraftStatus.forString(rawStatus);
                    if (df.compareTo(worstStatus) < 0) {
                        worstStatus = df;
                    }
                }

                for (String path2 : paths) {
                    String fullPath = resolved.getFullXPath(path2);
                    String value = resolved.getStringValue(path2);
                    if (LogicalGrouping.isOptional(cldrFileToFilter, path2)
                        && !cldrFileToFilter.isHere(path2)) {
                        continue;
                    }

                    XPathParts fullparts = XPathParts.getFrozenInstance(fullPath).cloneAsThawed(); // not frozen, for setAttribute
                    fullparts.setAttribute(-1, "draft", worstStatus.toString());
                    replace(fullPath, fullparts.toString(), value, "Fleshing out bailey to " + worstStatus);
                }
                seen.addAll(paths);
            }
        });
    }

    public static String getLast2Dirs(File sourceDir1) {
        String[] pathElements = sourceDir1.toString().split("/");
        return pathElements[pathElements.length-2] + "/" + pathElements[pathElements.length-1] + "/";
    }


    // references="http://www.stat.fi/tk/tt/luokitukset/lk/kieli_02.html"

    private static class ValuePair {
        String value;
        String fullxpath;
    }

    /**
     * Find the set of xpaths that
     * (a) have all the same values (if present) in the children
     * (b) are absent in the parent,
     * (c) are different than what is in the fully resolved parent
     * and add them.
     */
    static void fixIdenticalChildren(Factory cldrFactory, CLDRFile k, CLDRFile replacements) {
        String key = k.getLocaleID();
        if (key.equals("root")) return;
        Set<String> availableChildren = cldrFactory.getAvailableWithParent(key, true);
        if (availableChildren.size() == 0) return;
        Set<String> skipPaths = new HashSet<>();
        Map<String, ValuePair> haveSameValues = new TreeMap<>();
        CLDRFile resolvedFile = cldrFactory.make(key, true);
        // get only those paths that are not in "root"
        resolvedFile.forEach(skipPaths::add);

        // first, collect all the paths
        for (String locale : availableChildren) {
            if (locale.indexOf("POSIX") >= 0) continue;
            CLDRFile item = cldrFactory.make(locale, false);
            for (String xpath : item) {
                if (skipPaths.contains(xpath)) continue;
                // skip certain elements
                if (xpath.indexOf("/identity") >= 0) continue;
                if (xpath.startsWith("//ldml/numbers/currencies/currency")) continue;
                if (xpath.startsWith("//ldml/dates/timeZoneNames/metazone[")) continue;
                if (xpath.indexOf("[@alt") >= 0) continue;
                if (xpath.indexOf("/alias") >= 0) continue;

                // must be string vale
                ValuePair v1 = new ValuePair();
                v1.value = item.getStringValue(xpath);
                v1.fullxpath = item.getFullXPath(xpath);

                ValuePair vAlready = haveSameValues.get(xpath);
                if (vAlready == null) {
                    haveSameValues.put(xpath, v1);
                } else if (!v1.value.equals(vAlready.value) || !v1.fullxpath.equals(vAlready.fullxpath)) {
                    skipPaths.add(xpath);
                    haveSameValues.remove(xpath);
                }
            }
        }
        // at this point, haveSameValues is all kosher, so add items
        for (String xpath : haveSameValues.keySet()) {
            ValuePair v = haveSameValues.get(xpath);
            // if (v.value.equals(resolvedFile.getStringValue(xpath))
            // && v.fullxpath.equals(resolvedFile.getFullXPath(xpath))) continue;
            replacements.add(v.fullxpath, v.value);
        }
    }

    static void fixAltProposed() {
        throw new IllegalArgumentException();
        // throw out any alt=proposed values that are the same as the main
        // HashSet toRemove = new HashSet();
        // for (Iterator it = dataSource.iterator(); it.hasNext();) {
        // String cpath = (String) it.next();
        // if (cpath.indexOf("[@alt=") < 0) continue;
        // String cpath2 = getNondraftNonaltXPath(cpath);
        // String value = getStringValue(cpath);
        // String value2 = getStringValue(cpath2);
        // if (!value.equals(value2)) continue;
        // // have to worry about cases where the info is not in the value!!
        // //fix this; values are the same!!
        // String fullpath = getNondraftNonaltXPath(getFullXPath(cpath));
        // String fullpath2 = getNondraftNonaltXPath(getFullXPath(cpath2));
        // if (!fullpath.equals(fullpath2)) continue;
        // Log.logln(getLocaleID() + "\tRemoving redundant alternate: " + getFullXPath(cpath) + " ;\t" + value);
        // Log.logln("\t\tBecause of: " + getFullXPath(cpath2) + " ;\t" + value2);
        // if (getFullXPath(cpath2).indexOf("[@references=") >= 0) {
        // System.out.println("Warning: removing references: " + getFullXPath(cpath2));
        // }
        // toRemove.add(cpath);
        // }
        // dataSource.removeAll(toRemove);

    }

    /**
     * Perform various fixes
     * TODO add options to pick which one.
     *
     * @param options
     * @param config
     * @param cldrFactory
     */
    private static void fix(CLDRFile k, String inputOptions, String config, Factory cldrFactory) {

        // TODO before modifying, make sure that it is fully resolved.
        // then minimize against the NEW parents

        Set<String> removal = new TreeSet<>(k.getComparator());
        CLDRFile replacements = SimpleFactory.makeFile("temp");
        fixList.setFile(k, inputOptions, cldrFactory, removal, replacements);

        for (String xpath : k) {
            fixList.handlePath(xpath);
        }
        fixList.handleEnd();

        // remove bad attributes

        if (inputOptions.indexOf('v') >= 0) {
            CLDRTest.checkAttributeValidity(k, null, removal);
        }

        // raise identical elements

        if (inputOptions.indexOf('i') >= 0) {
            fixIdenticalChildren(cldrFactory, k, replacements);
        }

        // now do the actions we collected

        if (SHOW_DETAILS) {
            if (removal.size() != 0 || !replacements.isEmpty()) {
                if (!removal.isEmpty()) {
                    System.out.println("Removals:");
                    for (String path : removal) {
                        System.out.println(path + " =\t " + k.getStringValue(path));
                    }
                }
                if (!replacements.isEmpty()) {
                    System.out.println("Additions/Replacements:");
                    System.out.println(replacements.toString().replaceAll("\u00A0", "<NBSP>"));
                }
            }
        }
        if (removal.size() != 0) {
            k.removeAll(removal, COMMENT_REMOVALS);
        }
        k.putAll(replacements, CLDRFile.MERGE_REPLACE_MINE);
    }

    /**
     * Internal
     */
    public static void testJavaSemantics() {
        Collator caseInsensitive = Collator.getInstance(ULocale.ROOT);
        caseInsensitive.setStrength(Collator.SECONDARY);
        Set<String> setWithCaseInsensitive = new TreeSet<>(caseInsensitive);
        setWithCaseInsensitive.addAll(Arrays.asList(new String[] { "a", "b", "c" }));
        Set<String> plainSet = new TreeSet<>();
        plainSet.addAll(Arrays.asList(new String[] { "a", "b", "B" }));
        System.out.println("S1 equals S2?\t" + setWithCaseInsensitive.equals(plainSet));
        System.out.println("S2 equals S1?\t" + plainSet.equals(setWithCaseInsensitive));
        setWithCaseInsensitive.removeAll(plainSet);
        System.out.println("S1 removeAll S2 is empty?\t" + setWithCaseInsensitive.isEmpty());
    }

    // <localizedPatternChars>GyMdkHmsSEDFwWahKzYeugAZ</localizedPatternChars>
    /*
     * <localizedPattern>
     * <map type="era">G</map>
     * <map type="year">y</map>
     * <map type="year_iso">Y</map>
     * <map type="year_uniform">u</map>
     * <map type="month">M</map>
     * <map type="week_in_year">w</map>
     * <map type="week_in_month">W</map>
     * <map type="day">d</map>
     * <map type="day_of_year">D</map>
     * <map type="day_of_week_in_month">F</map>
     * <map type="day_julian">g</map>
     * <map type="day_of_week">E</map>
     * <map type="day_of_week_local">e</map>
     * <map type="period_in_day">a</map>
     * <map type="hour_1_12">h</map>
     * <map type="hour_0_23">H</map>
     * <map type="hour_0_11">K</map>
     * <map type="hour_1_24">k</map>
     * <map type="minute">m</map>
     * <map type="second">s</map>
     * <map type="fractions_of_second">S</map>
     * <map type="milliseconds_in_day">A</map>
     * <map type="timezone">z</map>
     * <map type="timezone_gmt">Z</map>
     * </localizedPattern>
     */

}
