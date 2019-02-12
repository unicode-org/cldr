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
import org.unicode.cldr.util.CldrUtility.SimpleLineComparator;
import org.unicode.cldr.util.DateTimeCanonicalizer;
import org.unicode.cldr.util.DateTimeCanonicalizer.DateTimePatternType;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
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
        + XPathParts.NEWLINE
        + "-r\t to minimize the results (removing items that inherit from parent)."
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

        String sourceInput = options[SOURCEDIR].value;
        String destInput = options[DESTDIR].value;
        if (recurseOnDirectories != null) {
            sourceInput = removeSuffix(sourceInput, "main/", "main");
            destInput = removeSuffix(destInput, "main/", "main");
        }
        String sourceDirBase = CldrUtility.checkValidDirectory(sourceInput); // Utility.COMMON_DIRECTORY + "main/";
        String targetDirBase = CldrUtility.checkValidDirectory(destInput); // Utility.GEN_DIRECTORY + "main/";
        System.out.format("Source:\t%s\n", sourceDirBase);
        System.out.format("Target:\t%s\n", targetDirBase);

        Set<String> dirSet = new TreeSet<String>();
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
            Log.setLog(targetDir + "/diff", "log.txt");
            try { // String[] failureLines = new String[2];
                SimpleLineComparator lineComparer = new SimpleLineComparator(
                    // SimpleLineComparator.SKIP_SPACES +
                    SimpleLineComparator.TRIM +
                    SimpleLineComparator.SKIP_EMPTY +
                    SimpleLineComparator.SKIP_CVS_TAGS);

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
                Set<String> locales = new TreeSet<String>(cldrFactory.getAvailable());
                if (mergeFactory != null) {
                    Set<String> temp = new TreeSet<String>(mergeFactory.getAvailable());
                    Set<String> locales3 = new TreeSet<String>();
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
                    if (options[MINIMIZE].doesOccur) {
                        if (pathHeaderFactory == null) {
                            pathHeaderFactory = PathHeader.getFactory(cldrFactory.make("en", true));
                        }
                        // TODO, fix identity
                        String parent = LocaleIDParser.getParent(test);
                        if (parent != null) {
                            CLDRFile toRemove = cldrFactory.make(parent, true);
                            // remove the items that are language codes, script codes, or region codes
                            // since they may be real translations.
                            if (parent.equals("root")) {
                                if (k.getFullXPath("//ldml/alias", true) != null) {
                                    System.out.println("Skipping completely aliased file: " + test);
                                } else {
                                    // k.putRoot(toRemove);
                                }
                            }
                            if (retainIfTrue == null) {
                                retainIfTrue = new RetainWhenMinimizing();
                            }
                            retainIfTrue.setParentFile(toRemove);
                            List<String> removed = DEBUG ? null : new ArrayList<String>();
                            k.removeDuplicates(toRemove, COMMENT_REMOVALS, retainIfTrue, removed);
                            if (removed != null && removed.size() != 0) {
                                totalRemoved += removed.size();
                                Set<PathHeader> sorted = new TreeSet<PathHeader>();
                                for (String path : removed) {
                                    sorted.add(pathHeaderFactory.fromPath(path));
                                }
                                for (PathHeader pathHeader : sorted) {
                                    System.out.println("\t# " + test + "\t" + pathHeader + "\t" + pathHeader.getOriginalPath());
                                }
                                System.out.println("\t# " + test + "\t# Removed:\t" + removed.size());
                            }
                        }
                    }
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
                        Iterator it4 = k.iterator();
                        Set s = CollectionUtilities.addAll(it4, new TreeSet());

                        System.out.println(k.getStringValue(testPath));
                        // if (true) return;
                        Set orderedSet = new TreeSet(k.getComparator());
                        CollectionUtilities.addAll(k.iterator(), orderedSet);
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
                Log.close();
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
    };

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
        Set<String> toRemove = new HashSet<String>();
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

    abstract static class CLDRFilter {
        protected CLDRFile cldrFileToFilter;
        protected CLDRFile cldrFileToFilterResolved;
        private String localeID;
        protected Set<String> availableChildren;
        private Set<String> toBeRemoved;
        private CLDRFile toBeReplaced;
        protected XPathParts parts = new XPathParts(null, null);
        protected XPathParts fullparts = new XPathParts(null, null);
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

    static Set<String> totalSkeletons = new HashSet<String>();

    static Map<String, String> rootUnitMap = new HashMap<String, String>();

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

                XPathParts parts = XPathParts.getInstance(path);
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
                XPathParts parts = XPathParts.getInstance(fullPath);
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
            public void handlePath(String xpath) {
                if (xpath.indexOf("=\"InterIndic\"") < 0) return;
                String v = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                fullparts.set(fullXPath);
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
                parts.set(xpath);
                if (!parts.containsAttributeValue("alt", "accounting")) return;
                String oldFullXPath = cldrFileToFilter.getFullXPath(xpath);
                String value = cldrFileToFilter.getStringValue(xpath);
                fullparts.set(oldFullXPath);
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

        fixList.add('_', "remove superfluous compound language translations", new CLDRFilter() {
            private CLDRFile resolved;

            public void handleStart() {
                resolved = factory.make(cldrFileToFilter.getLocaleID(), true);
            }

            public void handlePath(String xpath) {
                if (!xpath.contains("_")) return;
                if (!xpath.contains("/language")) return;
                String languageCode = parts.set(xpath).findAttributeValue("language", "type");
                String v = resolved.getStringValue(xpath);
                if (v.equals(languageCode)) {
                    remove(xpath, "same as language code");
                    return;
                }
                String generatedTranslation = resolved.getName(languageCode, true);
                if (v.equals(generatedTranslation)) {
                    remove(xpath, "superfluous compound language");
                }
                String spacelessGeneratedTranslation = generatedTranslation.replace(" ", "");
                if (v.equals(spacelessGeneratedTranslation)) {
                    remove(xpath, "superfluous compound language (after removing space)");
                }
            }
        });

        fixList.add('l', "change language code", new CLDRFilter() {
            private CLDRFile resolved;

            public void handleStart() {
                resolved = factory.make(cldrFileToFilter.getLocaleID(), true);
            }

            public void handlePath(String xpath) {
                if (!xpath.contains("/language")) return;
                String languageCode = parts.set(xpath).findAttributeValue("language", "type");
                String v = resolved.getStringValue(xpath);
                if (!languageCode.equals("swc")) return;
                parts.setAttribute("language", "type", "sw_CD");
                replace(xpath, parts.toString(), v);
            }
        });

        if (false) fixList.add('s', "fix stand-alone narrows", new CLDRFilter() {
            public void handlePath(String xpath) {
                if (xpath.indexOf("[@type=\"narrow\"]") < 0) return;
                parts.set(xpath);
                String element = "";
                if (parts.findElement("dayContext") >= 0) {
                    element = "dayContext";
                } else if (parts.findElement("monthContext") >= 0) {
                    element = "monthContext";
                } else
                    return;

                // change the element type UNLESS it conflicts
                parts.setAttribute(element, "type", "stand-alone");
                if (cldrFileToFilter.getStringValue(parts.toString()) != null) return;

                String v = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                fullparts.set(fullXPath);
                fullparts.setAttribute(element, "type", "stand-alone");
                replace(xpath, fullparts.toString(), v);
            }
        });

        fixList.add('m', "remove multiple alt-variants", new CLDRFilter() {

            public void handleStart() {
            }

            public void handlePath(String xpath) {
                parts.set(xpath);
                if (!parts.containsAttributeValue("alt", "variant")) return;
                String variantValue = cldrFileToFilter.getStringValue(xpath);
                String nonVariantXpath = xpath.replaceAll("\\[\\@alt=\"variant\"\\]", "");
                String nonVariantValue = cldrFileToFilter.getStringValue(nonVariantXpath);
                if (variantValue.equals(nonVariantValue)) {
                    remove(xpath, "removing superfluous alt-variant value");
                }
            }
        });

        fixList.add('g', "Swap alt/non-alt values for Czechia", new CLDRFilter() {

            public void handleStart() {
            }

            public void handlePath(String xpath) {
                parts.set(xpath);
                if (!parts.containsAttributeValue("alt", "variant") || !parts.containsAttributeValue("type", "CZ")) return;
                String variantValue = cldrFileToFilter.getStringValue(xpath);
                String nonVariantXpath = xpath.replaceAll("\\[\\@alt=\"variant\"\\]", "");
                String nonVariantValue = cldrFileToFilter.getStringValue(nonVariantXpath);
                replace(xpath, xpath, nonVariantValue);
                replace(nonVariantXpath, nonVariantXpath, variantValue);
            }
        });

        fixList.add('u', "fix duration unit patterns", new CLDRFilter() {

            public void handlePath(String xpath) {
                if (!xpath.contains("/units")) {
                    return;
                }
                if (!xpath.contains("/durationUnitPattern")) {
                    return;
                }

                String value = cldrFileToFilter.getStringValue(xpath);
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);

                parts.set(fullXPath);

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
                parts.set(fullpath);
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

            public void handlePath(String xpath) {

                if (!options[USER].doesOccur) return;
                String userID = options[USER].value;
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                String value = cldrFileToFilter.getStringValue(xpath);
                parts.set(fullpath);
                parts.addAttribute("draft", "unconfirmed");
                parts.addAttribute("alt", "proposed-u" + userID + "-implicit1.8");
                String newPath = parts.toString();
                replace(fullpath, newPath, value);
            }
        });

        fixList.add('c', "Fix transiton from an old currency code to a new one", new CLDRFilter() {
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

            public void handleStart() {
                inputProcessor = new DisplayAndInputProcessor(cldrFileToFilter, true);
            }

            public void handleEnd() {
                inputProcessor = null; // clean up, just in case
            }

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

            public void handlePath(String xpath) {
                if (xpath.indexOf("/currency") < 0
                    && xpath.indexOf("/timeZoneNames") < 0
                    && xpath.indexOf("/localeDisplayNames") < 0) return;
                parts.set(xpath);
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

        if (false) fixList.add('q', "fix exemplars", new CLDRFilter() {
            Collator col;
            Collator spaceCol;
            UnicodeSet uppercase = new UnicodeSet("[[:Uppercase:]-[\u0130]]");
            UnicodeSetIterator usi = new UnicodeSetIterator();

            public void handleStart() {
                String locale = cldrFileToFilter.getLocaleID();
                col = Collator.getInstance(new ULocale(locale));
                spaceCol = Collator.getInstance(new ULocale(locale));
                spaceCol.setStrength(col.PRIMARY);
            }

            public void handlePath(String xpath) {
                if (xpath.indexOf("/exemplarCharacters") < 0) return;
                String value = cldrFileToFilter.getStringValue(xpath);
                try {
                    String fixedValue = value.replaceAll("- ", "-"); // TODO fix hack
                    if (!fixedValue.equals(value)) {
                        System.out.println("Changing: " + value);
                    }
                    fixedValue = "[" + fixedValue + "]"; // add parens in case forgotten
                    UnicodeSet s1 = new UnicodeSet(fixedValue).removeAll(uppercase);
                    UnicodeSet s = new UnicodeSet();
                    for (usi.reset(s1); usi.next();) {
                        s.add(Normalizer.compose(usi.getString(), false));
                    }

                    String fixedExemplar1 = new UnicodeSetPrettyPrinter()
                        .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
                        .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                            .setStrength2(Collator.PRIMARY))
                        .setCompressRanges(true)
                        .format(s);

                    if (!value.equals(fixedExemplar1)) {
                        String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                        replace(fullXPath, fullXPath, fixedExemplar1);
                    }
                } catch (RuntimeException e) {
                    System.out.println("Illegal UnicodeSet: " + cldrFileToFilter.getLocaleID() + "\t" + value);
                }
            }
        });

        fixList.add('w', "fix alt='...proposed' when there is no alternative", new CLDRFilter() {
            private XPathParts parts = new XPathParts();
            private Set<String> newFullXPathSoFar = new HashSet<String>();

            public void handlePath(String xpath) {
                if (xpath.indexOf("proposed") < 0) return;
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                String newFullXPath = parts.set(fullXPath).removeProposed().toString();
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

//        fixList.add('l', "Remove losing items", new CLDRFilter() {
//            public void handlePath(String xpath) {
//                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
//                if (fullXPath.indexOf("proposed-x10") < 0) return;
//                if (fullXPath.indexOf("unconfirmed") < 0) return;
//                remove(fullXPath, "Losing item");
//            }
//        });

        if (false) fixList.add('z', "fix ZZ", new CLDRFilter() {
            public void handlePath(String xpath) {
                if (xpath.indexOf("/exemplarCharacters") < 0) return;
                String value = cldrFileToFilter.getStringValue(xpath);
                if (value.indexOf("[:") < 0) return;
                UnicodeSet s = new UnicodeSet(value);
                s.add(0xFFFF);
                s.remove(0xFFFF); // force flattening
                // at this point, we only have currency formats
                String fullXPath = cldrFileToFilter.getFullXPath(xpath);
                replace(fullXPath, fullXPath, s.toPattern(false));
            }
        });

        // fixList.add('z', "GenerateIndex", new CLDRFilter() {
        // @Override
        // public void handleStart() {
        // // TODO Auto-generated method stub
        // super.handleStart();
        // if (cldrFileToFilter.getExemplarSet("", WinningChoice.WINNING) != null) {
        // String indexPattern = GenerateIndexCharacters.getConstructedIndexSet(cldrFileToFilter.getLocaleID(),
        // cldrFileToFilter);
        // replace("//ldml/characters/exemplarCharacters[@type=\"index\"][@draft=\"unconfirmed\"]",
        // "//ldml/characters/exemplarCharacters[@type=\"index\"][@draft=\"unconfirmed\"]", indexPattern);
        // }
        // }
        // public void handlePath(String xpath) {
        // return;
        // }
        // });

        // fixList.add('k', "fix kk/KK", new CLDRFilter() {
        // DateTimePatternGenerator dtpg;
        // DateTimePatternGenerator.PatternInfo patternInfo = new DateTimePatternGenerator.PatternInfo();
        // DateTimePatternGenerator.FormatParser fp = new DateTimePatternGenerator.FormatParser();
        // Set dateFormatItems = new TreeSet();
        // Set standardFormats = new TreeSet();
        //
        // public void handleStart() {
        // dtpg = DateTimePatternGenerator.getEmptyInstance(); // should add clear()
        // dateFormatItems.clear();
        // standardFormats.clear();
        // }
        //
        // // <dateFormatItem id="KKmm" alt="proposed-u133-2" draft="provisional">hh:mm a</dateFormatItem>
        // public void handlePath(String xpath) {
        // if (xpath.indexOf("/dateFormatItem") >= 0) {
        // System.out.println(cldrFileToFilter.getStringValue(xpath) + "\t" + xpath);
        // dateFormatItems.add(xpath);
        // }
        // if (xpath.indexOf("gregorian") >= 0 && xpath.indexOf("pattern") >= 0) {
        // if (xpath.indexOf("dateFormat") >= 0 || xpath.indexOf("timeFormat") >= 0) {
        // standardFormats.add(xpath);
        // }
        // }
        // }
        // public void handleEnd() {
        // //if (dateFormatItems.size() == 0) return; // nothing to do
        //
        // // now add all the standard patterns
        // // algorithmically construct items from the standard formats
        //
        // Set standardSkeletons = new HashSet();
        // List items = new ArrayList();
        // for (Iterator it = standardFormats.iterator(); it.hasNext();) {
        // String xpath = (String) it.next();
        // String value = cldrFileToFilter.getStringValue(xpath);
        // dtpg.addPattern(value, false, patternInfo);
        // standardSkeletons.add(dtpg.getSkeleton(value));
        // if (false) { // code for adding guesses
        // fp.set(value);
        // items.clear();
        // fp.getAutoPatterns(value, items);
        // for (int i = 0; i < items.size(); ++i) {
        // String autoItem = (String)items.get(i);
        // dtpg.addPattern(autoItem, false, patternInfo);
        // if (patternInfo.status == patternInfo.OK) show("generate", value + " ==> " + autoItem);
        // }
        // }
        // retain(xpath, "-(std)");
        // }
        //
        // for (Iterator it = dateFormatItems.iterator(); it.hasNext();) {
        // String xpath = (String) it.next();
        // String value = cldrFileToFilter.getStringValue(xpath);
        // String oldValue = value;
        //
        // String skeleton = dtpg.getSkeleton(value);
        // // remove if single field
        // if (dtpg.isSingleField(skeleton)) {
        // remove(xpath, "Single Field");
        // continue;
        // }
        // // remove if date + time
        // fp.set(value);
        // // the following use fp, so make sure it is set
        //
        // if (fp.hasDateAndTimeFields()) {
        // remove(xpath, "Date + Time");
        // continue;
        // }
        //
        // if (containsSS()) {
        // remove(xpath, "SS");
        // continue;
        // }
        //
        // // see if we have a k or K & fix
        // value = fixKk(xpath, value);
        //
        // dtpg.addPattern(value, false, patternInfo);
        //
        // // // in case we changed value
        // // skeleton = dtpg.getSkeleton(value);
        // // String fullPath = cldrFileToFilter.getFullXPath(xpath);
        // // String oldFullPath = fullPath;
        // // parts.set(fullPath);
        // // Map attributes = parts.getAttributes(-1);
        // // String id = (String)attributes.get("id");
        // //
        // // // fix the ID
        // // if (!id.equals(skeleton)) {
        // // attributes.put("id", skeleton);
        // // fullPath = parts.toString();
        // // }
        // //
        // // // make the change
        // // boolean differentPath = !fullPath.equals(oldFullPath);
        // // if (differentPath || !value.equals(oldValue)) {
        // // String reason = "Fixed value";
        // // if (differentPath) {
        // // reason = "Fixed id";
        // // String collisionValue = cldrFileToFilter.getStringValue(fullPath);
        // // if (collisionValue != null) {
        // // if (!value.equals(collisionValue)) {
        // // System.out.println("Collision: not changing " + fullPath
        // // + " =\t " + value + ", old: " + collisionValue);
        // // }
        // // //skip if there was an old item with a different id
        // // remove(oldFullPath, "ID collision");
        // // return;
        // // }
        // // }
        // // replace(oldFullPath, fullPath, value, reason);
        // // }
        // }
        //
        // // make a minimal set
        // Map skeleton_patterns = dtpg.getSkeletons(null);
        //
        // Collection redundants = dtpg.getRedundants(null);
        // for (Iterator it = redundants.iterator(); it.hasNext();) {
        // String skeleton = dtpg.getSkeleton((String) it.next());
        // skeleton_patterns.remove(skeleton);
        // }
        // // remove all the standard IDs
        // for (Iterator it = standardSkeletons.iterator(); it.hasNext();) {
        // String standardSkeleton = (String) it.next();
        // skeleton_patterns.remove(standardSkeleton);
        // }
        // // Now add them all back in. Preserve old paths if possible
        // for (Iterator it = dateFormatItems.iterator(); it.hasNext();) {
        // String xpath = (String) it.next();
        // String oldValue = cldrFileToFilter.getStringValue(xpath);
        // String oldFullPath = cldrFileToFilter.getFullXPath(xpath);
        // String newFullPath = oldFullPath;
        // parts.set(newFullPath);
        // Map attributes = parts.getAttributes(-1);
        // String id = (String)attributes.get("id");
        // String newValue = (String) skeleton_patterns.get(id);
        // if (newValue == null) {
        // remove(xpath, "redundant");
        // continue;
        // }
        // String draft = (String)attributes.get("draft");
        // if (draft == null || draft.equals("approved")) {
        // attributes.put("draft", "provisional");
        // newFullPath = parts.toString();
        // }
        // if (oldValue.equals(newValue) && newFullPath.equals(oldFullPath)) {
        // retain(xpath, "-");
        // skeleton_patterns.remove(id);
        // continue; // skip, they are the same
        // }
        // // not redundant, but altered
        // replace(oldFullPath, newFullPath, newValue, "fixed");
        // skeleton_patterns.remove(id);
        // }
        // parts.set("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/" +
        // "dateFormatItem");
        // Map attributes = parts.getAttributes(-1);
        // //attributes.put("alt", "proposed-666");
        // attributes.put("draft", "provisional");
        // for (Iterator it = skeleton_patterns.keySet().iterator(); it.hasNext();) {
        // String skeleton = (String)it.next();
        // String pattern = (String)skeleton_patterns.get(skeleton);
        // attributes.put("id", skeleton);
        // String fullPath = parts.toString();
        // replace(fullPath, fullPath, pattern);
        // }
        // }
        //
        // private String fixKk(String xpath, String value) {
        // List fields = fp.getItems();
        // for (int i = 0; i < fields.size(); ++i) {
        // Object field = fields.get(i);
        // if (field instanceof DateTimePatternGenerator.VariableField) {
        // char first = field.toString().charAt(0);
        // String replacement = null;
        // if (first == 'k') replacement = "H";
        // else if (first == 'K') replacement = "h";
        // if (replacement != null) {
        // field = new DateTimePatternGenerator.VariableField(Utility.repeat(replacement, field.toString().length()));
        // fields.set(i, field);
        // }
        // }
        // }
        // String newValue = fp.toString();
        // if (!value.equals(newValue)) {
        // remove(xpath, value + " => " + newValue);
        // }
        // return newValue;
        // }
        //
        // private boolean containsSS() {
        // List fields = fp.getItems();
        // for (int i = 0; i < fields.size(); ++i) {
        // Object field = fields.get(i);
        // if (field instanceof DateTimePatternGenerator.VariableField) {
        // char first = field.toString().charAt(0);
        // if (first == 'S') return true;
        // }
        // }
        // return false;
        // }
        // });
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
            Map<String, Set<String>> seenSoFar = new HashMap<String, Set<String>>();

            public void handleStart() {
                seenSoFar.clear();
            }

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
                if (xpath.indexOf("/availableFormats") < 0) return;
                String value = cldrFileToFilter.getStringValue(xpath);
                if (value == null) return; // not in current file

                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                fullparts.set(fullpath);

                Map<String, String> attributes = fullparts.findAttributes("dateFormatItem");
                String id = attributes.get("id");
                String oldID = id;
                try {
                    id = dateTimePatternGenerator.getBaseSkeleton(id);
                    if (id.equals(oldID)) return;
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
            Map<String, Set<String>> seenSoFar = new HashMap<String, Set<String>>();

            public void handleStart() {
                seenSoFar.clear();
            }

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

            public void handleStart() {
                rootFile = factory.make("root", false);
            }

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
            Map<String, TreeMap<String, String>> locale_oldref_newref = new TreeMap<String, TreeMap<String, String>>();
            TreeMap<String, String> oldref_newref;

            //LanguageTagParser ltp = new LanguageTagParser();

            // References standards = new References(true);
            // References references = new References(false);

            public void handleStart() {
                String locale = cldrFileToFilter.getLocaleID();
                oldref_newref = locale_oldref_newref.get(locale);
                if (oldref_newref == null) {
                    oldref_newref = new TreeMap<String, String>();
                    locale_oldref_newref.put(locale, oldref_newref);
                }
            }

            // // Samples:
            // // <language type="ain" references="RP1">阿伊努文</language>
            // // <reference type="R1" uri="http://www.info.gov.hk/info/holiday_c.htm">二零零五年公眾假期刊登憲報</reference>
            public void handlePath(String xpath) {
                // must be minimised for this to work.
                String fullpath = cldrFileToFilter.getFullXPath(xpath);
                if (!fullpath.contains("reference")) return;
                String value = cldrFileToFilter.getStringValue(xpath);
                fullparts.set(fullpath);
                if ("reference".equals(fullparts.getElement(-1))) {
                    fixType(value, "type", fullpath);
                } else if (fullparts.getAttributeValue(-1, "references") != null) {
                    fixType(value, "references", fullpath);
                } else {
                    System.out.println("CLDRModify: Skipping: " + xpath);
                }
            }

            private void fixType(String value, String type, String oldFullPath) {
                String ref = fullparts.getAttributeValue(-1, type);
                if (whitespace.containsSome(ref)) throw new IllegalArgumentException("Whitespace in references");
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
                XPathParts parts = XPathParts.getInstance(fullpath);
                String cp = parts.getAttributeValue(2, "cp");
                String tts = parts.getAttributeValue(2, "tts");
                String type = parts.getAttributeValue(2, "type");
                if ("tts".equals(type)) {
                    return; // ok, skip
                }
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
                ;
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
                String name = cldrFileToFilter.getStringValue(xpath);
                XPathParts keywordParts = parts.cloneAsThawed().removeAttribute(2, "type");
                String keywordPath = keywordParts.toString();
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
                String newKeywordValue = CollectionUtilities.join(sorted, " | ");
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
                parts = XPathParts.getFrozenInstance(xpath);
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
            private LinkedHashSet<Map<ConfigKeys, ConfigMatch>> keyValues = new LinkedHashSet<Map<ConfigKeys, ConfigMatch>>();
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
                System.out.println("# Checking entries & adding:\t" + keyValues.size());
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
                                "Bad arguments, must have " +
                                    "path==null, value=null, new_path!=null, new_value!=null:\n\t"
                                    + entry);
                        }
                        String newPathString = newPath.getPath(cldrFileToFilter);
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
                locale2keyValues = new LinkedHashMap<ConfigMatch, LinkedHashSet<Map<ConfigKeys, ConfigMatch>>>();
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
                        Map<ConfigKeys, ConfigMatch> keyValue = new EnumMap<ConfigKeys, ConfigMatch>(
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

                        LinkedHashSet<Map<ConfigKeys, ConfigMatch>> keyValues = locale2keyValues
                            .get(locale);
                        if (keyValues == null) {
                            locale2keyValues.put(locale,
                                keyValues = new LinkedHashSet<Map<ConfigKeys, ConfigMatch>>());
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

        // fixList.add('q', "fix numbering system", new CLDRFilter() {
        // private final UnicodeSet dotEquivalents =(UnicodeSet) new UnicodeSet("[.．․﹒ 。｡︒۔٬]").freeze();
        // private final UnicodeSet commaEquivalents = (UnicodeSet) new UnicodeSet("[,，﹐ ، ٫ 、﹑､،]").freeze();
        // private final UnicodeSet apostropheEquivalent = (UnicodeSet) new UnicodeSet("[︐︑ '＇ ‘ ’ ]").freeze();
        // private final UnicodeSet spaces = (UnicodeSet) new UnicodeSet("[:whitespace:]").freeze();
        //
        // private final UnicodeSet ALLOWED_IN_NUMBER_SYMBOLS = (UnicodeSet) new
        // UnicodeSet("[\\u0000-\\u00FF ’ ‰ ∞ −]").freeze();
        //
        // private final UnicodeMap map = new UnicodeMap();
        // {
        // map.putAll(dotEquivalents, ".");
        // map.putAll(commaEquivalents, ",");
        // map.putAll(apostropheEquivalent, "’");
        // map.putAll(spaces, "\u00a0");
        // map.put('٪', "%");
        // map.put('؛', ";");
        // map.put('؉', "‰");
        // map.putAll(new UnicodeSet("\\p{dash}"), "-");
        // }
        //
        // private String system;
        // private CLDRFile resolved;
        //
        // /*
        // <decimal>.</decimal>
        // <group>,</group>
        // <list>;</list>
        // <percentSign>%</percentSign>
        // <nativeZeroDigit>0</nativeZeroDigit>
        // <patternDigit>#</patternDigit>
        // <plusSign>+</plusSign>
        // <minusSign>-</minusSign>
        // <exponential>E</exponential>
        // <perMille>‰</perMille>
        // <infinity>∞</infinity>
        // <nan>NaN</nan>
        // */
        // public void handleStart() {
        // resolved = cldrFileToFilter.make(cldrFileToFilter.getLocaleID(), true);
        // system = "????";
        // String zero = resolved.getStringValue("//ldml/numbers/symbols/nativeZeroDigit");
        // int firstChar = zero.codePointAt(0);
        // switch(firstChar) {
        // case '0': system = "????"; break;
        // case '٠': system = "arab"; break;
        // case '۰': system = "arabext"; break;
        // default:
        // int script = UScript.getScript(zero.codePointAt(0));
        // if (script != UScript.UNKNOWN) {
        // system = UScript.getShortName(script).toLowerCase(Locale.ENGLISH);
        // }
        // break;
        // }
        // }
        // public void handlePath(String xpath) {
        // String fullpath = cldrFileToFilter.getFullXPath(xpath);
        // if (!fullpath.startsWith("//ldml/numbers/symbols/")) return;
        // String value = cldrFileToFilter.getStringValue(xpath);
        // if (ALLOWED_IN_NUMBER_SYMBOLS.contains(value)) return;
        // parts.set(xpath);
        // String alt = parts.getAttributeValue(-1, "alt");
        // if (alt != null) {
        // show("*** Non-empty alt on " + xpath + "\t\t" + value,"???");
        // return;
        // }
        // String last = parts.getElement(-1);
        // String newValue = getLatinSeparator(value, last);
        // if (newValue == null) {
        // throw new IllegalArgumentException("Can't handle " + xpath + "\t\t" + value);
        // }
        // if (newValue.equals(value)) {
        // return;
        // }
        // replace(fullpath, fullpath, newValue);
        // parts.set(fullpath);
        // parts.addAttribute("alt", system);
        // String newPath = parts.toString();
        // replace(newPath, newPath, value);
        // }
        //
        // String getLatinSeparator(String value, String last) {
        // String newValue = map.transform(value);
        // if (ALLOWED_IN_NUMBER_SYMBOLS.containsAll(newValue)) {
        // return newValue;
        // }
        // if (last.equals("nativeZeroDigit")) {
        // return "0";
        // }
        // if (last.equals("exponential")) {
        // return "E";
        // }
        // if (last.equals("nan")) {
        // return "NaN";
        // }
        // if (last.equals("infinity")) {
        // return "∞";
        // }
        // if (last.equals("list")) {
        // return ";";
        // }
        // if (last.equals("percentSign")) {
        // return "%";
        // }
        // if (last.equals("group")) {
        // return "’";
        // }
        // return null;
        // }
        // });

        fixList.add('i', "fix Identical Children");
        fixList.add('o', "check attribute validity");

        fixList.add('^', "add inheritance-marked items from vxml to trunk", new CLDRFilter() {
            Map<String, Factory> factories;

            @Override
            public void handleSetup() {
                Map<String, Factory> _factories = new LinkedHashMap<>();
                for (String top : Arrays.asList("common/", "seed/")) {
                    for (String leaf : Arrays.asList("main/", "annotations/")) {
                        String key = top + leaf;
                        _factories.put(key, Factory.make(CLDRPaths.AUX_DIRECTORY + "voting/" + CLDRFile.GEN_VERSION + "/vxml/" + key, ".*"));
                    }
                }
                factories = ImmutableMap.copyOf(_factories);
            }

            @Override
            public void handleStart() {
                String localeID = cldrFileToFilter.getLocaleID();
                LanguageTagParser ltp = new LanguageTagParser().set(localeID);
                if (!ltp.getRegion().isEmpty() || !ltp.getVariants().isEmpty()) {
                    return;
                }
                File[] sourceDir = factory.getSourceDirectories();
                if (sourceDir.length != 1) {
                    throw new IllegalArgumentException("Can only handle single directory factory: " + Arrays.asList(sourceDir));
                }
                Factory vxml = factories.get(getLast2Dirs(sourceDir[0]));
                CLDRFile vxmlCommonMainFile = vxml.make(localeID, false);
                CLDRFile resolved = cldrFileToFilter;
                if (!cldrFileToFilter.isResolved()) {
                    resolved = factory.make(cldrFileToFilter.getLocaleID(), true);
                }

                // add all path,values in resolved IFF they are not in the unresolved AND there was a vote for uparrow in vxml
                for (String xpath : resolved) {
                    if (cldrFileToFilter.isHere(xpath)) {
                        continue;
                    }
                    String vxmlValue = vxmlCommonMainFile.getStringValue(xpath);
                    if (vxmlValue == null) {
                        continue;
                    }
                    if (!CldrUtility.INHERITANCE_MARKER.equals(vxmlValue)) {
                        continue;
                    }
                    // special hack to avoid combined locale names like //ldml/localeDisplayNames/languages/language[@type="en_AU"][@draft="contributed"]

                    if (xpath.startsWith("//ldml/localeDisplayNames/languages/language[@type=") && xpath.contains("_")) {
                        continue;
                    }

                    String value = resolved.getStringValue(xpath);
                    String fullPath = vxmlCommonMainFile.getFullXPath(xpath); // get the draft status, etc.
                    add(fullPath, value, "Vote for inherited");

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
                if (paths.size() < 2) {
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

                    XPathParts fullparts = XPathParts.getInstance(fullPath);
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
        Set<String> skipPaths = new HashSet<String>();
        Map<String, ValuePair> haveSameValues = new TreeMap<String, ValuePair>();
        CLDRFile resolvedFile = cldrFactory.make(key, true);
        // get only those paths that are not in "root"
        CollectionUtilities.addAll(resolvedFile.iterator(), skipPaths);

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

        Set<String> removal = new TreeSet<String>(k.getComparator());
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
        Set<String> setWithCaseInsensitive = new TreeSet<String>(caseInsensitive);
        setWithCaseInsensitive.addAll(Arrays.asList(new String[] { "a", "b", "c" }));
        Set<String> plainSet = new TreeSet<String>();
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
