/** */
package org.unicode.cldr.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.RuleBasedTransliterator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeFilter;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.DiscreteComparator.Builder;

public class CLDRTransforms {

    public static final String TRANSFORM_DIR = (CLDRPaths.COMMON_DIRECTORY + "transforms/");

    static final CLDRTransforms SINGLETON = new CLDRTransforms();

    private static final boolean PARANOID = true;

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

    final Set<String> overridden = new HashSet<>();

    // final DependencyOrder dependencyOrder = new DependencyOrder();

    //    static public class RegexFindFilenameFilter implements FilenameFilter {
    //        Matcher matcher;
    //
    //        public RegexFindFilenameFilter(Matcher filter) {
    //            matcher = filter;
    //        }
    //
    //        @Override
    //        public boolean accept(File dir, String name) {
    //            return matcher.reset(name).find();
    //        }
    //    }

    /**
     * @param dir TODO
     * @param namesMatchingRegex TODO
     * @param showProgress null if no progress needed
     * @param skipDashTIds TODO
     * @return
     */
    public static void registerCldrTransforms(
            String dir, String namesMatchingRegex, Appendable showProgress, boolean keepDashTIds) {
        CLDRTransforms r = getInstance();
        if (dir == null) {
            dir = TRANSFORM_DIR;
        }
        // reorder to preload some
        r.showProgress = showProgress;
        Set<String> ordered = getFileRegistrationOrder(dir);

        if (namesMatchingRegex != null) {
            Matcher filter = PatternCache.get(namesMatchingRegex).matcher("");
            ordered =
                    ordered.stream()
                            .filter(x -> filter.reset(x).matches())
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            //            r.deregisterIcuTransliterators(filter);
            //            files = Arrays.asList(new File(TRANSFORM_DIR).list(new
            // RegexFindFilenameFilter(filter)));
            //            ordered = r.dependencyOrder.getOrderedItems(files, filter, true);
        }

        // System.out.println(ordered);
        for (String cldrFileName : ordered) {
            r.registerTransliteratorsFromXML(
                    dir, cldrFileName, Collections.emptySet(), keepDashTIds);
        }
        Transliterator.registerAny(); // do this last!
    }

    private static class XmlFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.endsWith(".xml");
        }
    }

    public static List<String> getAvailableIds() {
        return Arrays.asList(new File(TRANSFORM_DIR).list(new XmlFilter()));
    }

    public Set<String> getOverriddenTransliterators() {
        return Collections.unmodifiableSet(overridden);
    }

    static Transliterator fixup = Transliterator.getInstance("[:Mn:]any-hex/java");

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
        return getInstance(
                matcher.group(2)
                        + "-"
                        + matcher.group(1)
                        + (matcher.group(4) == null ? "" : "/" + matcher.group(4)));
    }

    private BiMap<String, String> displayNameToId = HashBiMap.create();

    public BiMap<String, String> getDisplayNameToId() {
        return displayNameToId;
    }

    private void addDisplayNameToId(Map<String, String> ids2, ParsedTransformID directionInfo) {
        displayNameToId.put(directionInfo.getDisplayId(), directionInfo.toString());
    }

    public String registerTransliteratorsFromXML(
            String dir, String cldrFileName, Set<String> cantSkip, boolean keepDashTIds) {
        ParsedTransformID directionInfo = new ParsedTransformID();
        String ruleString = getIcuRulesFromXmlFile(dir, cldrFileName, directionInfo);

        String id = directionInfo.getId();
        addDisplayNameToId(displayNameToId, directionInfo);

        if (directionInfo.getDirection() == Direction.both
                || directionInfo.getDirection() == Direction.forward) {
            for (String alias : directionInfo.getAliases()) {
                if (!keepDashTIds && alias.contains("-t-")) {
                    continue;
                }
                Transliterator.unregister(alias);
                Transliterator.registerAlias(alias, id);
            }
            internalRegister(id, ruleString, Transliterator.FORWARD);
        }
        if (directionInfo.getDirection() == Direction.both
                || directionInfo.getDirection() == Direction.backward) {
            for (String alias : directionInfo.getBackwardAliases()) {
                if (!keepDashTIds && alias.contains("-t-")) {
                    continue;
                }
                Transliterator.unregister(alias);
                Transliterator.registerAlias(alias, directionInfo.getBackwardId());
            }
            internalRegister(id, ruleString, Transliterator.REVERSE);
        }
        return id;
    }

    /**
     * Return Icu rules, and the direction info
     *
     * @param dir TODO
     * @param cldrFileName
     * @param directionInfo
     * @return
     */
    public static String getIcuRulesFromXmlFile(
            String dir, String cldrFileName, ParsedTransformID directionInfo) {
        final MyHandler myHandler = new MyHandler(cldrFileName, directionInfo);
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(
                dir + cldrFileName,
                XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER,
                true);
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

            if (PARANOID) { // for paranoid testing
                String r1 =
                        CLDRTransforms.showTransliterator("", t, 9999, new StringBuilder())
                                .toString();
                Transliterator t2 = Transliterator.getInstance(id);
                String r2 =
                        CLDRTransforms.showTransliterator("", t2, 9999, new StringBuilder())
                                .toString();
                if (!r1.equals(r2)) {
                    throw new IllegalArgumentException(
                            "Rules unequal\n" + ruleString + "$$$\n$$$" + r2);
                }
            }
            // verifyNullFilter("halfwidth-fullwidth");
            if (showProgress != null) {
                append(
                        "Registered new Transliterator: "
                                + id
                                + (oldTranslit == null ? "" : "\told:\t" + oldTranslit.getID())
                                + '\n');
                if (id.startsWith("el-")) {
                    CLDRTransforms.showTransliterator("", t, 999);
                    Transliterator t2 = Transliterator.getInstance(id);
                    CLDRTransforms.showTransliterator("", t2, 999);
                }
            }
        } catch (RuntimeException e) {
            if (showProgress != null) {
                e.printStackTrace();
                append(
                        "Couldn't register new Transliterator: "
                                + id
                                + "\t"
                                + e.getMessage()
                                + '\n');
            } else {
                throw (IllegalArgumentException)
                        new IllegalArgumentException("Couldn't register new Transliterator: " + id)
                                .initCause(e);
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

    //    @SuppressWarnings("deprecation")
    //    public void registerFromIcuFormatFiles(String directory) throws IOException {
    //
    ////        deregisterIcuTransliterators((Matcher) null);
    //
    //        Matcher getId = PatternCache.get("\\s*(\\S*)\\s*\\{\\s*").matcher("");
    //        Matcher getSource =
    // PatternCache.get("\\s*(\\S*)\\s*\\{\\s*\\\"(.*)\\\".*").matcher("");
    //        Matcher translitID = PatternCache.get("([^-]+)-([^/]+)+(?:[/](.+))?").matcher("");
    //
    //        Map<String, String> fixedIDs = new TreeMap<>();
    //        Set<String> oddIDs = new TreeSet<>();
    //
    //        File dir = new File(directory);
    //        // get the list of files to take, and their directions
    //        BufferedReader input = FileUtilities.openUTF8Reader(directory, "root.txt");
    //        String id = null;
    //        String filename = null;
    //        Map<String, String> aliasMap = new LinkedHashMap<>();
    //
    //        // deregisterIcuTransliterators();
    //
    //        // do first, since others depend on theseregisterFromIcuFile
    //        /**
    //         * Special aliases.
    //         * Tone-Digit {
    //         * alias {"Pinyin-NumericPinyin"}
    //         * }
    //         * Digit-Tone {
    //         * alias {"NumericPinyin-Pinyin"}
    //         * }
    //         */
    //        // registerFromIcuFile("Latin-ConjoiningJamo", directory, null);
    //        // registerFromIcuFile("Pinyin-NumericPinyin", directory, null);
    //        // Transliterator.registerAlias("Tone-Digit", "Pinyin-NumericPinyin");
    //        // Transliterator.registerAlias("Digit-Tone", "NumericPinyin-Pinyin");
    //        // registerFromIcuFile("Fullwidth-Halfwidth", directory, null);
    //        // registerFromIcuFile("Hiragana-Katakana", directory, null);
    //        // registerFromIcuFile("Latin-Katakana", directory, null);
    //        // registerFromIcuFile("Hiragana-Latin", directory, null);
    //
    //        while (true) {
    //            String line = input.readLine();
    //            if (line == null) break;
    //            line = line.trim();
    //            if (line.startsWith("\uFEFF")) {
    //                line = line.substring(1);
    //            }
    //            if (line.startsWith("TransliteratorNamePattern")) break; // done
    //            // if (line.indexOf("Ethiopic") >= 0) {
    //            // appendln("Skipping Ethiopic");
    //            // continue;
    //            // }
    //            if (getId.reset(line).matches()) {
    //                String temp = getId.group(1);
    //                if (!temp.equals("file") && !temp.equals("internal")) id = temp;
    //                continue;
    //            }
    //            if (getSource.reset(line).matches()) {
    //                String operation = getSource.group(1);
    //                String source = getSource.group(2);
    //                if (operation.equals("alias")) {
    //                    aliasMap.put(id, source);
    //                    checkIdFix(id, fixedIDs, oddIDs, translitID);
    //                    id = null;
    //                } else if (operation.equals("resource:process(transliterator)")) {
    //                    filename = source;
    //                } else if (operation.equals("direction")) {
    //                    try {
    //                        if (id == null || filename == null) {
    //                            // appendln("skipping: " + line);
    //                            continue;
    //                        }
    //                        if (filename.indexOf("InterIndic") >= 0 && filename.indexOf("Latin")
    // >= 0) {
    //                            // append("**" + id);
    //                        }
    //                        checkIdFix(id, fixedIDs, oddIDs, translitID);
    //
    //                        final int direction = source.equals("FORWARD") ?
    // Transliterator.FORWARD
    //                            : Transliterator.REVERSE;
    //                        registerFromIcuFile(id, directory, filename, direction);
    //
    //                        verifyNullFilter("halfwidth-fullwidth");
    //
    //                        id = null;
    //                        filename = null;
    //                    } catch (RuntimeException e) {
    //                        throw (RuntimeException) new IllegalArgumentException("Failed with " +
    // filename + ", " + source)
    //                        .initCause(e);
    //                    }
    //                } else {
    //                    append(dir + "root.txt unhandled line:" + line);
    //                }
    //                continue;
    //            }
    //            String trimmed = line.trim();
    //            if (trimmed.equals("")) continue;
    //            if (trimmed.equals("}")) continue;
    //            if (trimmed.startsWith("//")) continue;
    //            throw new IllegalArgumentException("Unhandled:" + line);
    //        }
    //
    //        final Set<String> rawIds = idToRules.keySet();
    //        Set<String> ordered = dependencyOrder.getOrderedItems(rawIds, null, false);
    //        ordered.retainAll(rawIds); // since we are in ID space, kick out anything that isn't
    //
    //        for (String id2 : ordered) {
    //            RuleDirection stuff = idToRules.get(id2);
    //            internalRegisterNoReverseId(id2, stuff.ruleString, stuff.direction);
    //            verifyNullFilter("halfwidth-fullwidth"); // TESTING
    //        }
    //
    //        for (Iterator<String> it = aliasMap.keySet().iterator(); it.hasNext();) {
    //            id = it.next();
    //            String source = aliasMap.get(id);
    //            Transliterator.unregister(id);
    //            Transliterator t = Transliterator.createFromRules(id, "::" + source + ";",
    // Transliterator.FORWARD);
    //            Transliterator.registerInstance(t);
    //            // verifyNullFilter("halfwidth-fullwidth");
    //            appendln("Registered new Transliterator Alias: " + id);
    //
    //        }
    //        appendln("Fixed IDs");
    //        for (Iterator<String> it = fixedIDs.keySet().iterator(); it.hasNext();) {
    //            String id2 = it.next();
    //            appendln("\t" + id2 + "\t" + fixedIDs.get(id2));
    //        }
    //        appendln("Odd IDs");
    //        for (Iterator<String> it = oddIDs.iterator(); it.hasNext();) {
    //            String id2 = it.next();
    //            appendln("\t" + id2);
    //        }
    //        Transliterator.registerAny(); // do this last!
    //    }

    Map<String, RuleDirection> idToRules = new TreeMap<>();

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

    public void checkIdFix(
            String id, Map<String, String> fixedIDs, Set<String> oddIDs, Matcher translitID) {
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

    //    public void deregisterIcuTransliterators(Matcher filter) {
    //        // Remove all of the current registrations
    //        // first load into array, so we don't get sync problems.
    //        List<String> rawAvailable = new ArrayList<>();
    //        for (Enumeration<String> en = Transliterator.getAvailableIDs(); en.hasMoreElements();)
    // {
    //            final String id = en.nextElement();
    //            if (filter != null && !filter.reset(id).matches()) {
    //                continue;
    //            }
    //            rawAvailable.add(id);
    //        }
    //
    //        // deregisterIcuTransliterators(rawAvailable);
    //
    //        Set<String> available = dependencyOrder.getOrderedItems(rawAvailable, filter, false);
    //        List<String> reversed = new LinkedList<>();
    //        for (String item : available) {
    //            reversed.add(0, item);
    //        }
    //        // available.retainAll(rawAvailable); // remove the items we won't touch anyway
    //        // rawAvailable.removeAll(available); // now the ones whose order doesn't matter
    //        // deregisterIcuTransliterators(rawAvailable);
    //        deregisterIcuTransliterators(reversed);
    //
    //        for (Enumeration<String> en = Transliterator.getAvailableIDs(); en.hasMoreElements();)
    // {
    //            String oldId = en.nextElement();
    //            append("Retaining: " + oldId + "\n");
    //        }
    //    }
    //
    //    public void deregisterIcuTransliterators(Collection<String> available) {
    //        for (String oldId : available) {
    //            Transliterator t;
    //            try {
    //                t = Transliterator.getInstance(oldId);
    //            } catch (IllegalArgumentException e) {
    //                if (e.getMessage().startsWith("Illegal ID")) {
    //                    continue;
    //                }
    //                append("Failure with: " + oldId);
    //                t = Transliterator.getInstance(oldId);
    //                throw e;
    //            } catch (RuntimeException e) {
    //                append("Failure with: " + oldId);
    //                t = Transliterator.getInstance(oldId);
    //                throw e;
    //            }
    //            String className = t.getClass().getName();
    //            if (className.endsWith(".CompoundTransliterator")
    //                || className.endsWith(".RuleBasedTransliterator")
    //                || className.endsWith(".AnyTransliterator")) {
    //                appendln("REMOVING: " + oldId);
    //                Transliterator.unregister(oldId);
    //            } else {
    //                appendln("Retaining: " + oldId + "\t\t" + className);
    //            }
    //        }
    //    }

    public enum Direction {
        backward,
        both,
        forward
    }

    public enum Visibility {
        external,
        internal
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
            return getSource()
                    + "-"
                    + getTarget()
                    + (getVariant() == null ? "" : "/" + getVariant());
        }

        public String getDisplayId() {
            return getDisplaySource()
                    + "-"
                    + getDisplayTarget()
                    + (getVariant() == null ? "" : "/" + getDisplayVariant());
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
                String name =
                        CLDRConfig.getInstance()
                                .getEnglish()
                                .nameGetter()
                                .getNameFromIdentifier(sourceOrTarget);
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
            return getTarget()
                    + "-"
                    + getSource()
                    + (getVariant() == null ? "" : "/" + getVariant());
        }

        public ParsedTransformID() {}

        public ParsedTransformID set(
                String source, String target, String variant, Direction direction) {
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

        @Override
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

        @Override
        public void handlePathValue(String path, String value) {
            if (first) {
                if (path.startsWith("//supplementalData/version")) {
                    return;
                } else if (path.startsWith("//supplementalData/generation")) {
                    return;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                Map<String, String> attributes = parts.findAttributes("transform");
                if (attributes == null) {
                    throw new IllegalArgumentException(
                            "Not an XML transform file: " + cldrFileName + "\t" + path);
                }
                directionInfo.setSource(attributes.get("source"));
                directionInfo.setTarget(attributes.get("target"));
                directionInfo.setVariant(attributes.get("variant"));
                directionInfo.setDirection(
                        Direction.valueOf(attributes.get("direction").toLowerCase(Locale.ENGLISH)));

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

    static boolean ALREADY_REGISTERED = false;

    /**
     * Register just those transliterators that are different than ICU. TODO: check against the file
     * system to make sure the list is accurate.
     */
    public void registerModified() {
        synchronized (CLDRTransforms.class) {
            if (ALREADY_REGISTERED) {
                return;
            }
            // NEW
            registerTranslit("Lao-Latin", "ບ", "b");
            registerTranslit("Khmer-Latin", "ឥ", "ĕ");
            registerTranslit("Sinhala-Latin", "ක", "ka");
            registerTranslit("Japn-Latn", "譆", "aa");

            // MODIFIED
            registerTranslit("Han-SpacedHan", "《", "«");
            registerTranslit("Greek-Latin", "΄", "´");
            registerTranslit("Hebrew-Latin", "־", "-");
            registerTranslit("Cyrillic-Latin", "ө", "ö");
            registerTranslit("Myanmar-Latin", "ဿ", "s");
            registerTranslit("Latin-Armenian", "’", "՚");

            registerTranslit("Interindic-Latin", "\uE070", ".", "\uE03C", "\u0323", "\uE04D", "");

            registerTranslit("Malayalam-Interindic", "ൺ", "");
            registerTranslit("Interindic-Malayalam", "", "ണ്");
            registerTranslit("Malayalam-Latin", "ൺ", "ṇ");

            registerTranslit("Devanagari-Interindic", "ॲ", "\uE084");
            registerTranslit("Devanagari-Latin", "ॲ", "æ");

            registerTranslit("Arabic-Latin", "؉", "‰");
            ALREADY_REGISTERED = true;
        }
    }

    private static final ImmutableSet<String> noSkip = ImmutableSet.of();

    private static final boolean SHOW = false;
    private static final boolean SHOW_FAILED_MATCHES = false;

    /** Register a transliterator and verify that a sample changed value is accurate */
    public void registerTranslit(String ID, String... sourcePairs) {
        String internalId = registerTransliteratorsFromXML(TRANSFORM_DIR, ID, noSkip, true);
        Transliterator.registerAny(); // do this last!
        Transliterator t = null;
        try {
            t = Transliterator.getInstance(internalId);
        } catch (Exception e) {
            System.out.println("For " + ID + " (" + internalId + ")");
            e.printStackTrace();
            return;
        }
        testSourceTarget(t, sourcePairs);
    }

    public static void showTransliterator(String prefix, Transliterator t, int limit) {
        showTransliterator(prefix, t, limit, System.out);
        System.out.flush();
    }

    public static <T extends Appendable> T showTransliterator(
            String prefix, Transliterator t, int limit, T output) {
        if (!prefix.isEmpty()) {
            prefix += " ";
        }
        try {
            output.append(prefix + "ID:\t" + t.getID() + "\n");
            output.append(prefix + "Class:\t" + t.getClass().getName() + "\n");
            if (t.getFilter() != null) {
                output.append(prefix + "Filter:\t" + t.getFilter().toPattern(false) + "\n");
            }
            if (t instanceof RuleBasedTransliterator) {
                RuleBasedTransliterator rbt = (RuleBasedTransliterator) t;
                String[] rules = rbt.toRules(true).split("\n");
                int length = rules.length;
                if (limit >= 0 && limit < length) length = limit;
                output.append(prefix + "Rules:\n");
                prefix += "\t";
                for (int i = 0; i < length; ++i) {
                    output.append(prefix + rules[i] + "\n");
                }
            } else {
                Transliterator[] elements = t.getElements();
                if (elements[0] == t) {
                    output.append(prefix + "\tNonRuleBased\n");
                    return output;
                } else {
                    prefix += "\t";
                    for (int i = 0; i < elements.length; ++i) {
                        showTransliterator(prefix, elements[i], limit, output);
                    }
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        return output;
    }

    public static void testSourceTarget(Transliterator t, String... sourcePairs) {
        for (int i = 0; i < sourcePairs.length; i += 2) {
            String sourceTest = sourcePairs[i];
            String targetTest = sourcePairs[i + 1];
            String target = t.transform(sourceTest);
            if (!target.equals(targetTest)) {
                throw new IllegalArgumentException(
                        t.getID()
                                + " For "
                                + sourceTest
                                + ", expected "
                                + targetTest
                                + ", got "
                                + target);
            }
        }
    }

    /**
     * Gets a transform from a script to Latin. for testing For a locale, use
     * ExemplarUtilities.getLikelyScript(locale) to get the script
     */
    public static Transliterator getTestingLatinScriptTransform(final String script) {
        String id;

        switch (script) {
            case "Latn":
                return null;
            case "Khmr":
                id = "Khmr-Latn/UNGEGN";
                break;
            case "Laoo":
                id = "Laoo-Latn/UNGEGN";
                break;
            case "Sinh":
                id = "Sinh-Latn/UNGEGN";
                break;
            case "Japn":
                id = "Jpan-Latn";
                break;
            case "Kore":
                id = "Hangul-Latn";
                break;
            case "Hant":
            case "Hans":
                id = "Han-Latn";
                break;
            case "Olck":
                id = "sat_Olck-sat_FONIPA"; // Latin IPA
                break;
            case "Cher":
                id = "chr-chr_FONIPA";
                break;
            default:
                id = script + "-Latn";
        }
        return Transliterator.getInstance(id);
    }

    /**
     * Returns the set of all files that can be registered, in an order that makes sure that all
     * dependencies are handled. That is, if X uses Y in its rules, then Y has to come before X.
     *
     * <p>The problem is that when you build a transliterator from rules, and one of those rules is
     * to call another transliterator X, it inserts the <b>currently</b> registered transliterator
     * into the transliterator being built. So whenever a transliterator X is changed, you have to
     * reregister every transliterator that calls X. Otherwise the old version of X sticks around in
     * those calling transliterators. So the order that you register transliterators is important!
     */
    public static Set<String> getFileRegistrationOrder(String dir) {
        if (dir == null) {
            dir = TRANSFORM_DIR;
        }
        List<String> files = getAvailableIds();
        Multimap<String, String> fileToAliases = HashMultimap.create();
        Multimap<String, String> fileToDependencies = TreeMultimap.create();
        for (String file : files) {
            // Very simple test that depends on standard format
            // eg
            //            ::[॑ ॒ ॔ ॓ ़ ँ-ः । ॥ ॰ ०-९ ॐ ॲ ऄ-ऋ ॠ ऌ ॡ ऍ-कक़ खख़ गग़ घ-जज़ झ-डड़ ढढ़ ण-फफ़ ब-यय़
            // र-ह ऽ ॽ ा-ॄ ॢ ॣ ॅ-्];
            //            ::NFD;
            //            ::Devanagari-InterIndic;
            //            ::InterIndic-Latin;
            //            ::NFC;
            ParsedTransformID directionInfo = new ParsedTransformID();
            String ruleString = getIcuRulesFromXmlFile(dir, file, directionInfo);
            Set<String> others = new LinkedHashSet<>();
            Set<String> order =
                    ruleString
                            .lines()
                            .map(x -> x.trim())
                            .filter(x -> x.contains("::") && !x.trim().startsWith("#"))
                            .map(x -> parseDoubleColon(x, others))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            order.addAll(others);
            if (SHOW) {
                System.out.println(file + "=>" + order);
            }
            if (!order.isEmpty()) {
                fileToDependencies.putAll(file, order);
            }
            if (directionInfo.direction != Direction.backward) { // that is, forward or both
                fileToAliases.put(file, directionInfo.getId());
                fileToAliases.putAll(file, Arrays.asList(directionInfo.getAliases()));
                if (SHOW) {
                    System.out.println(
                            "\t"
                                    + directionInfo.getId()
                                    + "\t"
                                    + Arrays.asList(directionInfo.getAliases()));
                }
            }
            if (directionInfo.direction != Direction.forward) { // that is, backward or both
                fileToAliases.put(file, directionInfo.getBackwardId());
                fileToAliases.putAll(file, Arrays.asList(directionInfo.getBackwardAliases()));
                if (SHOW) {
                    System.out.println(
                            "\t"
                                    + directionInfo.getBackwardId()
                                    + "\t"
                                    + Arrays.asList(directionInfo.getBackwardAliases()));
                }
            }
        }
        TreeMultimap<String, String> aliasesToFile =
                Multimaps.invertFrom(fileToAliases, TreeMultimap.create());
        Multimap<String, String> fileToDependentFiles = TreeMultimap.create();

        for (Entry<String, Collection<String>> entry : fileToDependencies.asMap().entrySet()) {
            Set<String> v =
                    entry.getValue().stream()
                            .filter(x -> aliasesToFile.containsKey(x))
                            .map(y -> aliasesToFile.get(y).first())
                            .collect(Collectors.toSet());
            fileToDependentFiles.putAll(entry.getKey(), v);
        }
        Builder<String> comp = new DiscreteComparator.Builder<>(null);
        fileToDependentFiles.forEach(
                (x, y) -> {
                    if (SHOW) {
                        System.out.println(x + "=" + y);
                    }
                    comp.add(y, x); // put dependent earlier
                });
        // .add("c", "d", "b", "a").add("m", "n", "d").get();

        DiscreteComparator<String> comp2 = comp.get();
        Set<String> orderedDependents = new LinkedHashSet<>(comp2.getOrdering());
        orderedDependents.retainAll(
                fileToDependentFiles.values()); // remove files that are not dependents
        Set<String> remainingFiles = new TreeSet<>(files);
        remainingFiles.removeAll(orderedDependents);
        orderedDependents.addAll(remainingFiles);
        if (SHOW_FAILED_MATCHES) {
            System.out.println(orderedDependents);
        }
        return ImmutableSet.copyOf(orderedDependents);
    }

    // fails match: :: [:Latin:] fullwidth-halfwidth ();

    static final Pattern TRANSLIT_FINDER =
            Pattern.compile(
                    "\\s*::\\s*"
                            + "(?:\\[[^\\]]+\\]\\s*)?"
                            + "([A-Za-z0-9////_//-]*)?"
                            + "(?:"
                            + "\\s*\\("
                            + "(?:\\[[^\\]]+\\]\\s*)?"
                            + "([A-Za-z0-9////_//-]*)?"
                            + "\\s*\\)"
                            + ")?"
                            + "\\s*;\\s*(#.*)?");

    //    static {
    //        Matcher matcher = TRANSLIT_FINDER.matcher("::[:Latin:] fullwidth-halfwidth();");
    //        System.out.println(matcher.matches());
    //    }

    static String parseDoubleColon(String x, Set<String> others) {
        Matcher matcher = TRANSLIT_FINDER.matcher(x);
        if (matcher.matches()) {
            String first = matcher.group(1);
            String second = matcher.group(2);
            if (SHOW) {
                System.out.println("1: " + first + "\t2:" + second);
            }
            if (second != null && !second.isBlank()) {
                others.add(second);
            }
            return first == null || first.isBlank() ? "" : first;
        } else {
            if (SHOW_FAILED_MATCHES) {
                System.out.println("fails match: " + x);
            }
        }
        return "";
    }

    public class CLDRTransformsJsonIndex {
        /** raw list of available IDs */
        public String[] available =
                getAvailableIds().stream()
                        .map((String id) -> id.replace(".xml", ""))
                        .sorted()
                        .collect(Collectors.toList())
                        .toArray(new String[0]);
    }

    /** This gets the metadata (index file) exposed as cldr-json/cldr-transforms/transforms.json */
    public CLDRTransformsJsonIndex getJsonIndex() {
        final CLDRTransformsJsonIndex index = new CLDRTransformsJsonIndex();
        return index;
    }
}
