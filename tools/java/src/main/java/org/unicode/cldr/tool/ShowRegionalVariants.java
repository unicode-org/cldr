package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Objects;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.UnicodeSet;

public class ShowRegionalVariants {
    private static String MY_DIR;

    private static final boolean SKIP_SUPPRESSED_PATHS = true;

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO = CONFIG.getSupplementalDataInfo();
    private static final Factory FACTORY = CONFIG.getCldrFactory();
    private static final CLDRFile ENGLISH = CONFIG.getEnglish();
    private static final CLDRLocale ROOT = CLDRLocale.getInstance("root");
    //private static final CLDRLocale en_US_POSIX = CLDRLocale.getInstance("en_US_POSIX");
    private static final CLDRLocale SWISS_HIGH_GERMAN = CLDRLocale.getInstance("de_CH");

    final static Options myOptions = new Options();

    enum MyOptions {
        targetDir(".*", CLDRPaths.GEN_DIRECTORY + "/regional/", "target output file."),;
        // boilderplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.targetDir, args, true);

        MY_DIR = MyOptions.targetDir.option.getValue();

        Set<String> coverageLocales = CONFIG.getStandardCodes().getLocaleCoverageLocales("cldr");
        Set<String> dc = new HashSet<>(SUPPLEMENTAL_DATA_INFO.getDefaultContentLocales());
        Set<String> skipLocales = new HashSet<>(Arrays.asList("root", "en_US_POSIX", "sr_Latn"));

        Relation<CLDRLocale, CLDRLocale> parentToChildren = Relation.of(new TreeMap<CLDRLocale, Set<CLDRLocale>>(), TreeSet.class);
        // first, collect all locales for lookup by parents.

        for (String locale : FACTORY.getAvailable()) {
            if (skipLocales.contains(locale.toString())
                || dc.contains(locale.toString())) {
                continue;
            }
            CLDRLocale loc = CLDRLocale.getInstance(locale);

            if (!coverageLocales.contains(loc.getLanguage())) {
                continue;
            }
            CLDRLocale parent = null;
            for (CLDRLocale current = loc;; current = parent) {
                parent = current.getParent();
                if (!dc.contains(parent.toString())) { // skip over default content
                    break;
                }
            }
            if (ROOT.equals(parent)) {
                continue;
            } else if ("root".equals(parent.toString())) {
                throw new IllegalArgumentException("CLDRLocale failure");
            }
            parentToChildren.put(parent, loc);
        }

        // show inheritance
        System.out.println("Locale Name\tCode\tRegion\tInherits from\tCode");
        showInheritance(parentToChildren);

        // next find out the unique items in children
        Relation<String, String> valueToAncestors = Relation.of(new LinkedHashMap<String, Set<String>>(), LinkedHashSet.class);

        int count = 0;

        try (
            PrintWriter grandSummary = FileUtilities.openUTF8Writer(MY_DIR, "GrandSummary.txt");
            PrintWriter summary = FileUtilities.openUTF8Writer(MY_DIR, "Summary.txt");
            PrintWriter detailFile = FileUtilities.openUTF8Writer(MY_DIR, "details.txt");) {
            grandSummary.println("Parent\tName\tTotal Diff Count\tChildren");
            summary.println("Parent\tName\tDiff Count\tChild\tChild Name");
            detailFile
                .println(
                    "№\tBase\tParent Locales I\tParent Locales II\tChild Locales\tEnglish value\tParent value I\tParent value II\tChild value\tCorrected Child value\tComments\tFix Parent value?\tSection\tPage\tHeader\tCode");
            PathHeader.Factory phf = PathHeader.getFactory(ENGLISH);
            String lastBase = "";
            for (Entry<CLDRLocale, Set<CLDRLocale>> item : parentToChildren.keyValuesSet()) {
                CLDRLocale parent = item.getKey();
                String base = parent.getLanguage();

                CLDRFile parentFile = FACTORY.make(parent.toString(), true, DraftStatus.contributed);
                M4<PathHeader, String, CLDRLocale, Boolean> pathToValuesToLocales = ChainedMap.of(
                    new TreeMap<PathHeader, Object>(),
                    new TreeMap<String, Object>(),
                    new TreeMap<CLDRLocale, Object>(),
                    Boolean.class);

                Counter<CLDRLocale> childDiffs = new Counter<>();

                for (CLDRLocale child : item.getValue()) {
                    //childDiffs.add(child, 0); // make sure it shows up
                    String childString = child.toString();
                    CLDRFile childFile = FACTORY.make(childString, false, DraftStatus.contributed);
                    for (String path : childFile) {
                        if (SKIP_SUPPRESSED_PATHS) {
                            if (path.contains("/currency") && path.contains("/symbol")) {
                                continue;
                            }
                        }
                        String childValue = childFile.getStringValue(path);
                        if (childValue == null) {
                            continue;
                        }
                        String parentValue = parentFile.getStringValue(path);
                        if (parentValue == null) {
                            parentValue = "∅∅∅";
                        }
                        if (!Objects.equal(childValue, parentValue)) {
                            if (SKIP_SUPPRESSED_PATHS) {
                                if ("∅∅∅".equals(childValue) || "∅∅∅".equals(parentValue)) {
                                    continue; // skip suppressed paths
                                }
                            }
                            if (parentValue != null) {
                                if (child.equals(SWISS_HIGH_GERMAN)) {
                                    String norm = parentValue.replace("ß", "ss");
                                    if (childValue.equals(norm)) {
                                        continue;
                                    }
                                } else if (base.equals("en")) {
                                    if (sameExceptEnd(childValue, "re", parentValue, "er")
                                        || sameExceptEnd(childValue, "res", parentValue, "ers")) {
                                        continue;
                                    }
                                }
                            }
                            PathHeader pheader = phf.fromPath(path);
                            if (SectionId.Special == pheader.getSectionId()) {
                                continue;
                            }
                            pathToValuesToLocales.put(pheader, childValue, child, Boolean.TRUE);
                            childDiffs.add(child, 1);
                        }
                    }
                }

                long totalChildDiffs = childDiffs.getTotal();
                if (totalChildDiffs == 0) {
                    continue;
                }

                if (!base.equals(lastBase)) {
                    detailFile.println();
//                    if (detailFile != null) {
//                        detailFile.close();
//                    }
//                    detailFile = FileUtilities.openUTF8Writer(MY_DIR, "detail-" + base + ".txt");
//                    detailFile.println("Section\tPage\tHeader\tCode\tLocales\tvalue\tParent Locales\tvalue\tParent Locales\tvalue");
//                    lastBase = base;
                }

                grandSummary.println(parent + "\t" + ENGLISH.getName(parent.toString()) + "\t" + totalChildDiffs + "\t" + item.getValue());
                for (CLDRLocale s : childDiffs.getKeysetSortedByKey()) {
                    long childDiffValue = childDiffs.get(s);
                    if (childDiffValue == 0) {
                        continue;
                    }
                    summary.println(parent + "\t" + ENGLISH.getName(parent.toString()) + "\t" + childDiffValue + "\t" + s + "\t"
                        + ENGLISH.getName(s.toString()));
                }

                ArrayList<CLDRFile> parentChain = new ArrayList<CLDRFile>();
                for (CLDRLocale current = parent;;) {
                    parentChain.add(FACTORY.make(current.toString(), true));
                    CLDRLocale grand = current.getParent();
                    if (ROOT.equals(grand)) {
                        break;
                    }
                    current = grand;
                }

                for (PathHeader ph : pathToValuesToLocales.keySet()) {
                    M3<String, CLDRLocale, Boolean> values = pathToValuesToLocales.get(ph);
                    valueToAncestors.clear();
                    for (String value : values.keySet()) {
                        Set<CLDRLocale> childLocales = values.get(value).keySet();
                        String englishValue = ENGLISH.getStringValue(ph.getOriginalPath());
                        String originalPath = ph.getOriginalPath();
                        for (CLDRFile grand : parentChain) {
                            valueToAncestors.put(quote(grand.getStringValue(originalPath)), grand.getLocaleID());
                        }
                        Set<Entry<String, Set<String>>> keyValuesSet = valueToAncestors.keyValuesSet();
                        final int countParents = keyValuesSet.size();
                        if (countParents < 1 || countParents > 2) {
                            throw new IllegalArgumentException("Too few/many parents");
                        }

                        // // №  Base    Parent Locales I    Parent Locales II   Child Locales   English value   Parent value I  Parent value II Child value
                        // Corrected Child value   Comments    Fix Parent value?   Section Page    Header  Code

                        detailFile.print(
                            ++count
                                + "\t" + base);

                        for (Entry<String, Set<String>> entry : keyValuesSet) {
                            detailFile.print("\t" + entry.getValue());
                        }
                        if (countParents == 1) {
                            detailFile.print("\t");
                        }
                        detailFile.print(""
                            + "\t" + childLocales
                            + "\t" + quote(englishValue));
                        for (Entry<String, Set<String>> entry : keyValuesSet) {
                            detailFile.print("\t" + entry.getKey());
                        }
                        if (countParents == 1) {
                            detailFile.print("\t");
                        }
                        detailFile.print(""
                            + "\t" + quote(value)
                            + "\t" + ""
                            + "\t" + ""
                            + "\t" + ""
                            + "\t" + ph);
                        detailFile.println();
                    }
                }

            }
        }
        System.out.println("DONE");
//        if (detailFile != null) {
//            detailFile.close();
//        }
    }

    private static void showInheritance(Relation<CLDRLocale, CLDRLocale> parentToChildren) {
        Set<CLDRLocale> values = parentToChildren.values();
        Set<CLDRLocale> topParents = new TreeSet<>(parentToChildren.keySet());
        topParents.removeAll(values);
        showInheritance(topParents, "", parentToChildren);
    }

    private static void showInheritance(Set<CLDRLocale> topParents, String prefix, Relation<CLDRLocale, CLDRLocale> parentToChildren) {
        for (CLDRLocale locale : topParents) {
            String current = nameForLocale(locale) + "\t" + prefix;
            System.out.println(current);
            Set<CLDRLocale> newChildren = parentToChildren.get(locale);
            if (newChildren == null) {
                continue;
            }
            showInheritance(newChildren, current, parentToChildren);
        }
    }

    static final LikelySubtags LS = new LikelySubtags();

    private static String nameForLocale(CLDRLocale key) {
        String country = key.getCountry();
        if (country.isEmpty()) {
            String max = LS.maximize(key.toString());
            LanguageTagParser ltp = new LanguageTagParser().set(max);
            country = "(" + ltp.getRegion() + ")";
        }
        return ENGLISH.getName(key.toString(), false, CLDRFile.SHORT_ALTS) + "\t" + key + "\t" + country;
    }

    private static boolean sameExceptEnd(String childValue, String childEnding, String parentValue, String parentEnding) {
        if (childValue.endsWith(childEnding)
            && parentValue.endsWith(parentEnding)
            && childValue.substring(0, childValue.length() - childEnding.length()).equals(
                parentValue.substring(0, parentValue.length() - parentEnding.length()))) {
            return true;
        }
        return false;
    }

    static final UnicodeSet SPREAD_SHEET_SENSITIVE = new UnicodeSet().add('=').add('+').add('0', '9');

    private static String quote(String value) {
        if (value == null || value.isEmpty()) {
            return "∅∅∅";
        }
        int first = value.codePointAt(0);
        return SPREAD_SHEET_SENSITIVE.contains(first) ? "'" + value : value;
    }
}
