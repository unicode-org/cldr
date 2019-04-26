/**
 *
 */
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.TestTransforms;
import org.unicode.cldr.tool.LikelySubtags;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeFilter;
import com.ibm.icu.util.ICUUncheckedIOException;

public class CLDRTransforms {

    public static final String TRANSFORM_DIR = (CLDRPaths.COMMON_DIRECTORY + "transforms/");

    static final CLDRTransforms SINGLETON = new CLDRTransforms();

    public static CLDRTransforms getInstance() {
        return SINGLETON;
    }

    public Appendable getShowProgress() {
        return showProgress;
    }

    public CLDRTransforms setShowProgress(Appendable showProgress) {
        this.showProgress = showProgress;
        return this;
    }

    final Set<String> overridden = new HashSet<String>();
    final DependencyOrder dependencyOrder = new DependencyOrder();

    static public class RegexFindFilenameFilter implements FilenameFilter {
        Matcher matcher;

        public RegexFindFilenameFilter(Matcher filter) {
            matcher = filter;
        }

        @Override
        public boolean accept(File dir, String name) {
            return matcher.reset(name).find();
        }
    };

    /**
     *
     * @param dir
     *            TODO
     * @param namesMatchingRegex
     *            TODO
     * @param showProgress
     *            null if no progress needed
     * @param skipDashTIds TODO
     * @return
     */

    public static void registerCldrTransforms(String dir, String namesMatchingRegex, Appendable showProgress, boolean keepDashTIds) {
        CLDRTransforms r = getInstance();
        if (dir == null) {
            dir = TRANSFORM_DIR;
        }
        // reorder to preload some
        r.showProgress = showProgress;
        List<String> files;
        Set<String> ordered;

        if (namesMatchingRegex == null) {
            files = getAvailableIds();
            ordered = r.dependencyOrder.getOrderedItems(files, null, true);
        } else {
            Matcher filter = PatternCache.get(namesMatchingRegex).matcher("");
            r.deregisterIcuTransliterators(filter);
            files = Arrays.asList(new File(TRANSFORM_DIR).list(new RegexFindFilenameFilter(filter)));
            ordered = r.dependencyOrder.getOrderedItems(files, filter, true);
        }

        // System.out.println(ordered);
        for (String cldrFileName : ordered) {
            r.registerTransliteratorsFromXML(dir, cldrFileName, files, keepDashTIds);
        }
        Transliterator.registerAny(); // do this last!

    }

    public static List<String> getAvailableIds() {
        return Arrays.asList(new File(TRANSFORM_DIR).list());
    }

    public Set<String> getOverriddenTransliterators() {
        return Collections.unmodifiableSet(overridden);
    }

    static Transliterator fixup = Transliterator.getInstance("[:Mn:]any-hex/java");

    class DependencyOrder {
        // String[] doFirst = {"Latin-ConjoiningJamo"};
        // the following are file names, not IDs, so the dependencies have to go both directions
        // List<String> extras = new ArrayList<String>();

        Relation<Matcher, String> dependsOn = Relation.of(new LinkedHashMap<Matcher, Set<String>>(), LinkedHashSet.class);
        {
            addDependency("Latin-(Jamo|Hangul)(/.*)?", "Latin-ConjoiningJamo", "ConjoiningJamo-Latin");
            addDependency("(Jamo|Hangul)-Latin(/.*)?", "Latin-ConjoiningJamo", "ConjoiningJamo-Latin");
            addDependency("Latin-Han(/.*)", "Han-Spacedhan");
            addDependency(".*(Hiragana|Katakana|Han|han).*", "Fullwidth-Halfwidth", "Halfwidth-Fullwidth");
            addDependency(".*(Hiragana).*", "Latin-Katakana", "Katakana-Latin");

            addInterIndicDependency("Arabic");
            addInterIndicDependency("Bengali");
            addInterIndicDependency("Devanagari");
            addInterIndicDependency("Gujarati");
            addInterIndicDependency("Gurmukhi");
            addInterIndicDependency("Kannada");
            addInterIndicDependency("Malayalam");
            addInterIndicDependency("Oriya");
            addInterIndicDependency("Tamil");
            addInterIndicDependency("Telugu");
            addInterIndicDependency("ur");

            addDependency(".*Digit.*", "NumericPinyin-Pinyin", "Pinyin-NumericPinyin");
            addDependency("Latin-NumericPinyin(/.*)?", "Tone-Digit", "Digit-Tone");
            addDependency("NumericPinyin-Latin(/.*)?", "Tone-Digit", "Digit-Tone");
            addDependency("am-ar", "am-am_FONIPA", "und_FONIPA-ar");
            addDependency("am-chr", "am-am_FONIPA", "und_FONIPA-chr");
            addDependency("am-fa", "am-am_FONIPA", "und_FONIPA-fa");
            addDependency("ch-am", "ch-ch_FONIPA", "am-am_FONIPA");
            addDependency("ch-ar", "ch-ch_FONIPA", "und_FONIPA-ar");
            addDependency("ch-chr", "ch-ch_FONIPA", "und_FONIPA-chr");
            addDependency("ch-fa", "ch-ch_FONIPA", "und_FONIPA-fa");
            addDependency("cs-am", "cs-cs_FONIPA", "am-am_FONIPA");
            addDependency("cs-ar", "cs-cs_FONIPA", "und_FONIPA-ar");
            addDependency("cs-chr", "cs-cs_FONIPA", "und_FONIPA-chr");
            addDependency("cs-fa", "cs-cs_FONIPA", "und_FONIPA-fa");
            addDependency("cs-ja", "cs-cs_FONIPA", "cs_FONIPA-ja");
            addDependency("cs_FONIPA-ko", "Latin-Hangul");
            addDependency("cs-ko", "cs-cs_FONIPA", "cs_FONIPA-ko");
            addDependency("de-ASCII", "Any-ASCII");
            addDependency("eo-am", "eo-eo_FONIPA", "am-am_FONIPA");
            addDependency("eo-ar", "eo-eo_FONIPA", "und_FONIPA-ar");
            addDependency("eo-chr", "eo-eo_FONIPA", "und_FONIPA-chr");
            addDependency("eo-fa", "eo-eo_FONIPA", "und_FONIPA-fa");
            addDependency("es-am", "es-es_FONIPA", "am-am_FONIPA");
            addDependency("es-ar", "es-es_FONIPA", "und_FONIPA-ar");
            addDependency("es-chr", "es-es_FONIPA", "und_FONIPA-chr");
            addDependency("es-fa", "es-es_FONIPA", "und_FONIPA-fa");
            addDependency("es_419-am", "es-es_FONIPA", "es_FONIPA-es_419_FONIPA", "am-am_FONIPA");
            addDependency("es_419-ar", "es-es_FONIPA", "es_FONIPA-es_419_FONIPA", "und_FONIPA-ar");
            addDependency("es_419-chr", "es-es_FONIPA", "es_FONIPA-es_419_FONIPA", "und_FONIPA-chr");
            addDependency("es_419-fa", "es-es_FONIPA", "es_FONIPA-es_419_FONIPA", "und_FONIPA-fa");
            addDependency("es_419-ja", "es-es_FONIPA", "es_FONIPA-es_419_FONIPA", "es_FONIPA-ja");
            addDependency("es-am", "es-es_FONIPA", "es_FONIPA-am");
            addDependency("es-ja", "es-es_FONIPA", "es_FONIPA-ja");
            addDependency("es-zh", "es-es_FONIPA", "es_FONIPA-zh");

            addDependency("Han-Latin-Names", "Han-Latin");

            addDependency("hy-am", "hy-hy_FONIPA", "am-am_FONIPA");
            addDependency("hy-ar", "hy-hy_FONIPA", "und_FONIPA-ar");
            addDependency("hy-chr", "hy-hy_FONIPA", "und_FONIPA-chr");
            addDependency("hy-fa", "hy-hy_FONIPA", "und_FONIPA-fa");
            addDependency("hy_AREVMDA-am", "hy_AREVMDA-hy_AREVMDA_FONIPA", "am-am_FONIPA");
            addDependency("hy_AREVMDA-ar", "hy_AREVMDA-hy_AREVMDA_FONIPA", "und_FONIPA-ar");
            addDependency("hy_AREVMDA-chr", "hy_AREVMDA-hy_AREVMDA_FONIPA", "und_FONIPA-chr");
            addDependency("hy_AREVMDA-fa", "hy_AREVMDA-hy_AREVMDA_FONIPA", "und_FONIPA-fa");
            addDependency("ia-am", "ia-ia_FONIPA", "am-am_FONIPA");
            addDependency("ia-ar", "ia-ia_FONIPA", "und_FONIPA-ar");
            addDependency("ia-chr", "ia-ia_FONIPA", "und_FONIPA-chr");
            addDependency("ia-fa", "ia-ia_FONIPA", "und_FONIPA-fa");
            addDependency("kk-am", "kk-kk_FONIPA", "am-am_FONIPA");
            addDependency("kk-ar", "kk-kk_FONIPA", "und_FONIPA-ar");
            addDependency("kk-chr", "kk-kk_FONIPA", "und_FONIPA-chr");
            addDependency("kk-fa", "kk-kk_FONIPA", "und_FONIPA-fa");
            addDependency("ky-am", "ky-ky_FONIPA", "am-am_FONIPA");
            addDependency("ky-ar", "ky-ky_FONIPA", "und_FONIPA-ar");
            addDependency("ky-chr", "ky-ky_FONIPA", "und_FONIPA-chr");
            addDependency("ky-fa", "ky-ky_FONIPA", "und_FONIPA-fa");
            addDependency("my-am", "my-my_FONIPA", "am-am_FONIPA");
            addDependency("my-ar", "my-my_FONIPA", "und_FONIPA-ar");
            addDependency("my-chr", "my-my_FONIPA", "und_FONIPA-chr");
            addDependency("my-fa", "my-my_FONIPA", "und_FONIPA-fa");
            addDependency("pl-am", "pl-pl_FONIPA", "am-am_FONIPA");
            addDependency("pl-ar", "pl-pl_FONIPA", "und_FONIPA-ar");
            addDependency("pl-chr", "pl-pl_FONIPA", "und_FONIPA-chr");
            addDependency("pl-fa", "pl-pl_FONIPA", "und_FONIPA-fa");
            addDependency("pl-ja", "pl-pl_FONIPA", "pl_FONIPA-ja");
            addDependency("rm_SURSILV-am", "rm_SURSILV-rm_FONIPA_SURSILV", "am-am_FONIPA");
            addDependency("rm_SURSILV-ar", "rm_SURSILV-rm_FONIPA_SURSILV", "und_FONIPA-ar");
            addDependency("rm_SURSILV-chr", "rm_SURSILV-rm_FONIPA_SURSILV", "und_FONIPA-chr");
            addDependency("rm_SURSILV-fa", "rm_SURSILV-rm_FONIPA_SURSILV", "und_FONIPA-fa");
            addDependency("ro-am", "ro-ro_FONIPA", "am-am_FONIPA");
            addDependency("ro-ar", "ro-ro_FONIPA", "und_FONIPA-ar");
            addDependency("ro-chr", "ro-ro_FONIPA", "und_FONIPA-chr");
            addDependency("ro-fa", "ro-ro_FONIPA", "und_FONIPA-fa");
            addDependency("ro-ja", "ro-ro_FONIPA", "ro_FONIPA-ja");
            addDependency("sat-am", "sat_Olck-sat_FONIPA", "am-am_FONIPA");
            addDependency("sat-ar", "sat_Olck-sat_FONIPA", "und_FONIPA-ar");
            addDependency("sat-chr", "sat_Olck-sat_FONIPA", "und_FONIPA-chr");
            addDependency("sat-fa", "sat_Olck-sat_FONIPA", "und_FONIPA-fa");
            addDependency("si-am", "si-si_FONIPA", "am-am_FONIPA");
            addDependency("si-ar", "si-si_FONIPA", "und_FONIPA-ar");
            addDependency("si-chr", "si-si_FONIPA", "und_FONIPA-chr");
            addDependency("si-fa", "si-si_FONIPA", "und_FONIPA-fa");
            addDependency("sk-am", "sk-sk_FONIPA", "am-am_FONIPA");
            addDependency("sk-ar", "sk-sk_FONIPA", "und_FONIPA-ar");
            addDependency("sk-chr", "sk-sk_FONIPA", "und_FONIPA-chr");
            addDependency("sk-fa", "sk-sk_FONIPA", "und_FONIPA-fa");
            addDependency("sk-ja", "sk-sk_FONIPA", "sk_FONIPA-ja");
            addDependency("tlh-am", "tlh-tlh_FONIPA", "am-am_FONIPA");
            addDependency("tlh-ar", "tlh-tlh_FONIPA", "und_FONIPA-ar");
            addDependency("tlh-chr", "tlh-tlh_FONIPA", "und_FONIPA-chr");
            addDependency("tlh-fa", "tlh-tlh_FONIPA", "und_FONIPA-fa");
            addDependency("xh-am", "xh-xh_FONIPA", "am-am_FONIPA");
            addDependency("xh-ar", "xh-xh_FONIPA", "und_FONIPA-ar");
            addDependency("xh-chr", "xh-xh_FONIPA", "und_FONIPA-chr");
            addDependency("xh-fa", "xh-xh_FONIPA", "und_FONIPA-fa");
            addDependency("zu-am", "zu-zu_FONIPA", "am-am_FONIPA");
            addDependency("zu-ar", "zu-zu_FONIPA", "und_FONIPA-ar");
            addDependency("zu-chr", "zu-zu_FONIPA", "und_FONIPA-chr");
            addDependency("zu-fa", "zu-zu_FONIPA", "und_FONIPA-fa");
            addDependency("Latin-Bopomofo", "Latin-NumericPinyin");

            // addExtras("cs-ja", "cs-ja", "es-am", "es-ja", "es-zh", "Han-Latin/Names");
            // Pinyin-NumericPinyin.xml
        }

        private void addInterIndicDependency(String script) {
            addPivotDependency(script, "InterIndic");
            if (!script.equals("Arabic")) {
                addDependency(script + "-Arabic",
                    script + "-InterIndic", "InterIndic-Arabic");
            }
        }

        private void addPivotDependency(String script, String pivot) {
            addDependency(script + "-.*", "Bengali" + "-" + pivot, pivot + "-" + "Bengali");
            addDependency(".*-" + "Bengali" + "(/.*)?", pivot + "-" + "Bengali", pivot + "-" + "Bengali");
        }

        // private void addExtras(String... strings) {
        // for (String item : strings) {
        // extras.add(item);
        // }
        // }

        private void addDependency(String pattern, String... whatItDependsOn) {
            dependsOn.putAll(PatternCache.get(pattern).matcher(""), Arrays.asList(whatItDependsOn));
        }

        public Set<String> getOrderedItems(Collection<String> rawInput, Matcher filter, boolean hasXmlSuffix) {
            Set<String> input = new LinkedHashSet<String>(rawInput);
            // input.addAll(extras);

            Set<String> ordered = new LinkedHashSet<String>();

            // for (String other : doFirst) {
            // ordered.add(hasXmlSuffix ? other + ".xml" : other);
            // }

            for (String cldrFileName : input) {
                if (hasXmlSuffix && !cldrFileName.endsWith(".xml")) {
                    continue;
                }

                if (filter != null && !filter.reset(cldrFileName).find()) {
                    append("Skipping " + cldrFileName + "\n");
                    continue;
                }
                // add dependencies first
                addDependenciesRecursively(cldrFileName, ordered, hasXmlSuffix);
            }
            append("Adding: " + ordered + "\n");
            return ordered;
        }

        private void addDependenciesRecursively(String cldrFileName, Set<String> ordered, boolean hasXmlSuffix) {
            String item = hasXmlSuffix && cldrFileName.endsWith(".xml") ? cldrFileName.substring(0,
                cldrFileName.length() - 4) : cldrFileName;
            for (Matcher m : dependsOn.keySet()) {
                if (m.reset(item).matches()) {
                    for (String other : dependsOn.getAll(m)) {
                        final String toAdd = hasXmlSuffix ? other + ".xml" : other;
                        if (other.equals(item) || ordered.contains(toAdd)) {
                            continue;
                        }
                        addDependenciesRecursively(toAdd, ordered, hasXmlSuffix);
                        append("Dependency: Adding: " + toAdd + " before " + item + "\n");
                    }
                }
            }
            ordered.add(item);
        }

    }

    public Transliterator getInstance(String id) {
        if (!overridden.contains(id)) {
            throw new IllegalArgumentException("No overriden transform for " + id);
        }
        return Transliterator.getInstance(id);
    }

    public static Pattern TRANSFORM_ID_PATTERN = PatternCache.get("(.+)-([^/]+)(/(.*))?");

    public Transliterator getReverseInstance(String id) {
        Matcher matcher = TRANSFORM_ID_PATTERN.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("**No transform for " + id);
        }
        return getInstance(matcher.group(2) + "-" + matcher.group(1)
            + (matcher.group(4) == null ? "" : "/" + matcher.group(4)));
    }

    private BiMap<String,String> displayNameToId = HashBiMap.create();
    
    public BiMap<String, String> getDisplayNameToId() {
        return displayNameToId;
    }
    
    private void addDisplayNameToId(Map<String, String> ids2, ParsedTransformID directionInfo) {
        displayNameToId.put(directionInfo.getDisplayId(), directionInfo.toString());
    }

    public void registerTransliteratorsFromXML(String dir, String cldrFileName, List<String> cantSkip, boolean keepDashTIds) {
        ParsedTransformID directionInfo = new ParsedTransformID();
        String ruleString;
        final String cldrFileName2 = cldrFileName + ".xml";
        try {
            ruleString = getIcuRulesFromXmlFile(dir, cldrFileName2, directionInfo);
        } catch (RuntimeException e) {
            if (!cantSkip.contains(cldrFileName2)) {
                return;
            }
            throw e;
        }
        
        String id = directionInfo.getId();
        addDisplayNameToId(displayNameToId, directionInfo);
        
        if (directionInfo.getDirection() == Direction.both || directionInfo.getDirection() == Direction.forward) {
            internalRegister(id, ruleString, Transliterator.FORWARD);
            for (String alias : directionInfo.getAliases()) {
                if (!keepDashTIds && alias.contains("-t-")) {
                    continue;
                }
                Transliterator.registerAlias(alias, id);
            }
        }
        if (directionInfo.getDirection() == Direction.both || directionInfo.getDirection() == Direction.backward) {
            internalRegister(id, ruleString, Transliterator.REVERSE);
            for (String alias : directionInfo.getBackwardAliases()) {
                if (!keepDashTIds && alias.contains("-t-")) {
                    continue;
                }
                Transliterator.registerAlias(alias, directionInfo.getBackwardId());
            }
        }
    }

    /**
     * Return Icu rules, and the direction info
     *
     * @param dir
     *            TODO
     * @param cldrFileName
     * @param directionInfo
     * @return
     */
    public static String getIcuRulesFromXmlFile(String dir, String cldrFileName, ParsedTransformID directionInfo) {
        final MyHandler myHandler = new MyHandler(cldrFileName, directionInfo);
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(dir + cldrFileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, true);
        return myHandler.getRules();
    }

    private void internalRegister(String id, String ruleString, int direction) {
        if (direction == Transliterator.REVERSE) {
            id = ParsedTransformID.reverse(id);
        }
        internalRegisterNoReverseId(id, ruleString, direction);
    }

    private void internalRegisterNoReverseId(String id, String ruleString, int direction) {
        try {
            Transliterator t = Transliterator.createFromRules(id, ruleString, direction);
            overridden.add(id);
            Transliterator oldTranslit = null;
            if (showProgress != null) {
                try {
                    oldTranslit = Transliterator.getInstance(id);
                } catch (Exception e) {
                }
            }
            Transliterator.unregister(id);
            Transliterator.registerInstance(t);
            // if (false) { // for paranoid testing
            // Transliterator t1 = Transliterator.createFromRules(id, ruleString, direction);
            // String r1 = t1.toRules(false);
            // Transliterator t2 = Transliterator.getInstance(id);
            // String r2 = t2.toRules(false);
            // if (!r1.equals(r2)) {
            // throw (IllegalArgumentException) new IllegalArgumentException("Rules unequal" + ruleString + "$$$\n$$$" +
            // r2);
            // }
            // }
            // verifyNullFilter("halfwidth-fullwidth");
            if (showProgress != null) {
                append("Registered new Transliterator: " + id
                    + (oldTranslit == null ? "" : "\told:\t" + oldTranslit.getID())
                    + '\n');
                if (id.startsWith("el-")) {
                    TestTransforms.showTransliterator("", t, 999);
                    Transliterator t2 = Transliterator.getInstance(id);
                    TestTransforms.showTransliterator("", t2, 999);
                }
            }
        } catch (RuntimeException e) {
            if (showProgress != null) {
                e.printStackTrace();
                append("Couldn't register new Transliterator: " + id + "\t" + e.getMessage() + '\n');
            } else {
                throw (IllegalArgumentException) new IllegalArgumentException("Couldn't register new Transliterator: "
                    + id).initCause(e);
            }
        }
    }

    Appendable showProgress;

    private void append(String string) {
        try {
            if (showProgress == null) {
                return;
            }
            showProgress.append(string);
            if (showProgress instanceof Writer) {
                ((Writer) showProgress).flush();
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private void appendln(String s) {
        append(s + "\n");
    }

    // ===================================

    @SuppressWarnings("deprecation")
    public void registerFromIcuFormatFiles(String directory) throws IOException {

        deregisterIcuTransliterators((Matcher) null);

        Matcher getId = PatternCache.get("\\s*(\\S*)\\s*\\{\\s*").matcher("");
        Matcher getSource = PatternCache.get("\\s*(\\S*)\\s*\\{\\s*\\\"(.*)\\\".*").matcher("");
        Matcher translitID = PatternCache.get("([^-]+)-([^/]+)+(?:[/](.+))?").matcher("");

        Map<String, String> fixedIDs = new TreeMap<String, String>();
        Set<String> oddIDs = new TreeSet<String>();

        File dir = new File(directory);
        // get the list of files to take, and their directions
        BufferedReader input = FileUtilities.openUTF8Reader(directory, "root.txt");
        String id = null;
        String filename = null;
        Map<String, String> aliasMap = new LinkedHashMap<String, String>();

        // deregisterIcuTransliterators();

        // do first, since others depend on theseregisterFromIcuFile
        /**
         * Special aliases.
         * Tone-Digit {
         * alias {"Pinyin-NumericPinyin"}
         * }
         * Digit-Tone {
         * alias {"NumericPinyin-Pinyin"}
         * }
         */
        // registerFromIcuFile("Latin-ConjoiningJamo", directory, null);
        // registerFromIcuFile("Pinyin-NumericPinyin", directory, null);
        // Transliterator.registerAlias("Tone-Digit", "Pinyin-NumericPinyin");
        // Transliterator.registerAlias("Digit-Tone", "NumericPinyin-Pinyin");
        // registerFromIcuFile("Fullwidth-Halfwidth", directory, null);
        // registerFromIcuFile("Hiragana-Katakana", directory, null);
        // registerFromIcuFile("Latin-Katakana", directory, null);
        // registerFromIcuFile("Hiragana-Latin", directory, null);

        while (true) {
            String line = input.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            if (line.startsWith("TransliteratorNamePattern")) break; // done
            // if (line.indexOf("Ethiopic") >= 0) {
            // appendln("Skipping Ethiopic");
            // continue;
            // }
            if (getId.reset(line).matches()) {
                String temp = getId.group(1);
                if (!temp.equals("file") && !temp.equals("internal")) id = temp;
                continue;
            }
            if (getSource.reset(line).matches()) {
                String operation = getSource.group(1);
                String source = getSource.group(2);
                if (operation.equals("alias")) {
                    aliasMap.put(id, source);
                    checkIdFix(id, fixedIDs, oddIDs, translitID);
                    id = null;
                } else if (operation.equals("resource:process(transliterator)")) {
                    filename = source;
                } else if (operation.equals("direction")) {
                    try {
                        if (id == null || filename == null) {
                            // appendln("skipping: " + line);
                            continue;
                        }
                        if (filename.indexOf("InterIndic") >= 0 && filename.indexOf("Latin") >= 0) {
                            // append("**" + id);
                        }
                        checkIdFix(id, fixedIDs, oddIDs, translitID);

                        final int direction = source.equals("FORWARD") ? Transliterator.FORWARD
                            : Transliterator.REVERSE;
                        registerFromIcuFile(id, directory, filename, direction);

                        verifyNullFilter("halfwidth-fullwidth");

                        id = null;
                        filename = null;
                    } catch (RuntimeException e) {
                        throw (RuntimeException) new IllegalArgumentException("Failed with " + filename + ", " + source)
                            .initCause(e);
                    }
                } else {
                    append(dir + "root.txt unhandled line:" + line);
                }
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.equals("")) continue;
            if (trimmed.equals("}")) continue;
            if (trimmed.startsWith("//")) continue;
            throw new IllegalArgumentException("Unhandled:" + line);
        }

        final Set<String> rawIds = idToRules.keySet();
        Set<String> ordered = dependencyOrder.getOrderedItems(rawIds, null, false);
        ordered.retainAll(rawIds); // since we are in ID space, kick out anything that isn't

        for (String id2 : ordered) {
            RuleDirection stuff = idToRules.get(id2);
            internalRegisterNoReverseId(id2, stuff.ruleString, stuff.direction);
            verifyNullFilter("halfwidth-fullwidth"); // TESTING
        }

        for (Iterator<String> it = aliasMap.keySet().iterator(); it.hasNext();) {
            id = it.next();
            String source = aliasMap.get(id);
            Transliterator.unregister(id);
            Transliterator t = Transliterator.createFromRules(id, "::" + source + ";", Transliterator.FORWARD);
            Transliterator.registerInstance(t);
            // verifyNullFilter("halfwidth-fullwidth");
            appendln("Registered new Transliterator Alias: " + id);

        }
        appendln("Fixed IDs");
        for (Iterator<String> it = fixedIDs.keySet().iterator(); it.hasNext();) {
            String id2 = it.next();
            appendln("\t" + id2 + "\t" + fixedIDs.get(id2));
        }
        appendln("Odd IDs");
        for (Iterator<String> it = oddIDs.iterator(); it.hasNext();) {
            String id2 = it.next();
            appendln("\t" + id2);
        }
        Transliterator.registerAny(); // do this last!
    }

    Map<String, RuleDirection> idToRules = new TreeMap<String, RuleDirection>();

    private class RuleDirection {
        String ruleString;
        int direction;

        public RuleDirection(String ruleString, int direction) {
            super();
            this.ruleString = ruleString;
            this.direction = direction;
        }
    }

    private void registerFromIcuFile(String id, String directory, String filename, int direction) {
        if (filename == null) {
            filename = id.replace("-", "_").replace("/", "_") + ".txt";
        }
        String ruleString = CldrUtility.getText(directory, filename);
        idToRules.put(id, new RuleDirection(ruleString, direction));
    }

    // private void registerFromIcuFile(String id, String dir, String filename) {
    // registerFromIcuFile(id, dir, filename, Transliterator.FORWARD);
    // registerFromIcuFile(id, dir, filename, Transliterator.REVERSE);
    // }

    public void checkIdFix(String id, Map<String, String> fixedIDs, Set<String> oddIDs, Matcher translitID) {
        if (fixedIDs.containsKey(id)) return;
        if (!translitID.reset(id).matches()) {
            appendln("Can't fix: " + id);
            fixedIDs.put(id, "?" + id);
            return;
        }
        String source1 = translitID.group(1);
        String target1 = translitID.group(2);
        String variant = translitID.group(3);
        String source = fixID(source1);
        String target = fixID(target1);
        if (!source1.equals(source)) {
            fixedIDs.put(source1, source);
        }
        if (!target1.equals(target)) {
            fixedIDs.put(target1, target);
        }
        if (variant != null) {
            oddIDs.add("variant: " + variant);
        }
    }

    static String fixID(String source) {
        return source; // for now
    }

    public void deregisterIcuTransliterators(Matcher filter) {
        // Remove all of the current registrations
        // first load into array, so we don't get sync problems.
        List<String> rawAvailable = new ArrayList<String>();
        for (Enumeration<String> en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
            final String id = en.nextElement();
            if (filter != null && !filter.reset(id).matches()) {
                continue;
            }
            rawAvailable.add(id);
        }

        // deregisterIcuTransliterators(rawAvailable);

        Set<String> available = dependencyOrder.getOrderedItems(rawAvailable, filter, false);
        List<String> reversed = new LinkedList<String>();
        for (String item : available) {
            reversed.add(0, item);
        }
        // available.retainAll(rawAvailable); // remove the items we won't touch anyway
        // rawAvailable.removeAll(available); // now the ones whose order doesn't matter
        // deregisterIcuTransliterators(rawAvailable);
        deregisterIcuTransliterators(reversed);

        for (Enumeration<String> en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
            String oldId = en.nextElement();
            append("Retaining: " + oldId + "\n");
        }
    }

    public void deregisterIcuTransliterators(Collection<String> available) {
        for (String oldId : available) {
            Transliterator t;
            try {
                t = Transliterator.getInstance(oldId);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().startsWith("Illegal ID")) {
                    continue;
                }
                append("Failure with: " + oldId);
                t = Transliterator.getInstance(oldId);
                throw e;
            } catch (RuntimeException e) {
                append("Failure with: " + oldId);
                t = Transliterator.getInstance(oldId);
                throw e;
            }
            String className = t.getClass().getName();
            if (className.endsWith(".CompoundTransliterator")
                || className.endsWith(".RuleBasedTransliterator")
                || className.endsWith(".AnyTransliterator")) {
                appendln("REMOVING: " + oldId);
                Transliterator.unregister(oldId);
            } else {
                appendln("Retaining: " + oldId + "\t\t" + className);
            }
        }
    }

    public enum Direction {
        backward, both, forward
    }

    public enum Visibility {
        external, internal
    }

    public static class ParsedTransformID {
        public String source = "Any";
        public String target = "Any";
        public String variant;
        protected String[] aliases = {};
        protected String[] backwardAliases = {};
        protected Direction direction = null;
        protected Visibility visibility;

        public String getId() {
            return getSource() + "-" + getTarget() + (getVariant() == null ? "" : "/" + getVariant());
        }

        public String getDisplayId() {
            return getDisplaySource() + "-" + getDisplayTarget() + (getVariant() == null ? "" : "/" + getDisplayVariant());
        }

        private String getDisplayVariant() {
            return getVariant();
        }

        private String getDisplayTarget() {
            return getDisplaySourceOrTarget(getTarget());
        }

        private String getDisplaySource() {
            return getDisplaySourceOrTarget(getSource());
        }

        private String getDisplaySourceOrTarget(String sourceOrTarget) {
            int uscript = UScript.getCodeFromName(sourceOrTarget);
            if (uscript >= 0) {
                return UScript.getName(uscript);
            }
            if (sourceOrTarget.contains("FONIPA")) {
                return "IPA";
            }
            if (sourceOrTarget.equals("InterIndic")) {
                return "Indic";
            }
            try {
                String name = CLDRConfig.getInstance().getEnglish().getName(sourceOrTarget);
                return name;
            } catch (Exception e) {
                return sourceOrTarget;
            }
        }
        
        static final LikelySubtags likely = new LikelySubtags();
        
        public static String getScriptCode(String sourceOrTarget) {
            int uscript = UScript.getCodeFromName(sourceOrTarget);
            if (uscript >= 0) {
                return UScript.getShortName(uscript);
            }
            if (sourceOrTarget.contains("FONIPA")) {
                return "Ipa0";
            }
            if (sourceOrTarget.equals("InterIndic")) {
                return "Ind0";
            }
            try {
                String max = likely.maximize(sourceOrTarget);
                return max == null ? null : new LanguageTagParser().set(max).getScript();
            } catch (Exception e) {
                return null;
            }
        }

        public String getBackwardId() {
            return getTarget() + "-" + getSource() + (getVariant() == null ? "" : "/" + getVariant());
        }

        public ParsedTransformID() {
        }

        public ParsedTransformID set(String source, String target, String variant, Direction direction) {
            this.source = source;
            this.target = target;
            this.variant = variant;
            this.direction = direction;
            return this;
        }

        public ParsedTransformID set(String id) {
            variant = null;
            int pos = id.indexOf('-');
            if (pos < 0) {
                source = "Any";
                target = id;
                return this;
            }
            source = id.substring(0, pos);
            int pos2 = id.indexOf('/', pos);
            if (pos2 < 0) {
                target = id.substring(pos + 1);
                return this;
            }
            target = id.substring(pos + 1, pos2);
            variant = id.substring(pos2 + 1);
            return this;
        }

        public ParsedTransformID reverse() {
            String temp = source;
            source = target;
            target = temp;
            return this;
        }

        public String getTargetVariant() {
            return target + (variant == null ? "" : "/" + variant);
        }

        public String getSourceVariant() {
            return source + (variant == null ? "" : "/" + variant);
        }

        protected void setDirection(Direction direction) {
            this.direction = direction;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setVariant(String variant) {
            this.variant = variant;
        }

        protected String getVariant() {
            return variant;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getTarget() {
            return target;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSource() {
            return source;
        }

        public String toString() {
            return source + "-" + getTargetVariant();
        }

        public static String getId(String source, String target, String variant) {
            String id = source + '-' + target;
            if (variant != null) id += "/" + variant;
            return id;
        }

        public static String reverse(String id) {
            return new ParsedTransformID().set(id).getBackwardId();
        }

        public void setAliases(String[] aliases) {
            this.aliases = aliases;
        }

        public String[] getAliases() {
            return aliases;
        }

        public void setBackwardAliases(String[] backwardAliases) {
            this.backwardAliases = backwardAliases;
        }

        public String[] getBackwardAliases() {
            return backwardAliases;
        }

        protected void setVisibility(String string) {
            visibility = Visibility.valueOf(string);
        }

        public Visibility getVisibility() {
            return visibility;
        }
    }

    /**
     * Verify that if the transliterator exists, it has a null filter
     *
     * @param id
     */
    public static void verifyNullFilter(String id) {
        Transliterator widen;
        try {
            widen = Transliterator.getInstance(id);
        } catch (Exception e) {
            return;
        }
        UnicodeFilter filter = widen.getFilter();
        if (filter != null) {
            throw new IllegalArgumentException(id + " has non-empty filter: " + filter);
        }
    }

    public static class MyHandler extends XMLFileReader.SimpleHandler {
        boolean first = true;
        ParsedTransformID directionInfo;
        String cldrFileName;
        StringBuilder rules = new StringBuilder();

        public String getRules() {
            return rules.toString();
        }

        public MyHandler(String cldrFileName, ParsedTransformID directionInfo) {
            super();
            this.cldrFileName = cldrFileName;
            this.directionInfo = directionInfo;
        }

        public void handlePathValue(String path, String value) {
             if (first) {
                if (path.startsWith("//supplementalData/version")) {
                    return;
                } else if (path.startsWith("//supplementalData/generation")) {
                    return;
                }
                XPathParts parts = XPathParts.getTestInstance(path);
                Map<String, String> attributes = parts.findAttributes("transform");
                if (attributes == null) {
                    throw new IllegalArgumentException("Not an XML transform file: " + cldrFileName + "\t" + path);
                }
                directionInfo.setSource(attributes.get("source"));
                directionInfo.setTarget(attributes.get("target"));
                directionInfo.setVariant(attributes.get("variant"));
                directionInfo.setDirection(Direction.valueOf(attributes.get("direction").toLowerCase(Locale.ENGLISH)));

                String alias = attributes.get("alias");
                if (alias != null) {
                    directionInfo.setAliases(alias.trim().split("\\s+"));
                }

                String backwardAlias = attributes.get("backwardAlias");
                if (backwardAlias != null) {
                    directionInfo.setBackwardAliases(backwardAlias.trim().split("\\s+"));
                }

                directionInfo.setVisibility(attributes.get("visibility"));
                first = false;
            }
            if (path.indexOf("/comment") >= 0) {
                // skip
            } else if (path.indexOf("/tRule") >= 0) {
                value = fixup.transliterate(value);
                rules.append(value).append(CldrUtility.LINE_SEPARATOR);
            } else {
                throw new IllegalArgumentException("Unknown element: " + path + "\t " + value);
            }
        }
    }
}
