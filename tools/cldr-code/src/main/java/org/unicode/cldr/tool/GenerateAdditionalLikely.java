package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

/**
 * TODO: Merge into GenerateMaximalLocales, see CLDR-16380
 */
@CLDRTool(description = "Generate additional likely subtag data, see CLDR-16380", url="https://unicode-org.atlassian.net/browse/CLDR-16380", alias = "generate-additional-likely")
public class GenerateAdditionalLikely {

    private static final String SIL = "sil1";
    private static final boolean ADD_SEED_EXEMPLARS = false;

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Splitter UNDERBAR = Splitter.on('_');
    private static final Splitter TAB_SPLITTER = Splitter.on('\t');

    private static final Factory factory = CLDR_CONFIG.getExemplarsFactory();
    private static final CLDRFile english = CLDR_CONFIG.getEnglish();
    private static final LanguageTagParser ltpFull = new LanguageTagParser();
    private static final LanguageTagParser ltpTag = new LanguageTagParser();
    private static final Validity validity = Validity.getInstance();

    private static final Set<String> LANGUAGE_REGULAR = validity.getStatusToCodes(LstrType.language).get(Status.regular);
    private static final Set<String> SCRIPT_REGULAR = validity.getStatusToCodes(LstrType.script).get(Status.regular);
    private static final Set<String> REGION_REGULAR = validity.getStatusToCodes(LstrType.region).get(Status.regular);

    private static final Set<String> LIKELY_SPECIALS = ImmutableSet.of("in", "iw", "ji", "jw", "mo");
    private static final Set<String> FIX_VALIDITY = ImmutableSet.of("Zanb");
    private static final Set<String> FIX_COUNTRY = ImmutableSet.of("yi");

    static class LSRSource implements Comparable<LSRSource>{
        final Row.R4<String, String, String, String> data;

        LSRSource(String lang, String script, String region, String source) {
            if (script.contains("Soyo") || region.contains("Soyo")) {
                int debug = 0;
            }
            data = Row.of(lang, script, region, source);
            data.freeze();       }

        @Override
        public String toString() {
            return combineLSR(data.get0(), data.get1(), data.get2()) + " // " + data.get3();
        }
        @Override
        public int compareTo(LSRSource o) {
            return data.compareTo(o.data);
        }
        @Override
        public int hashCode() {
            return data.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            return data.equals(obj);
        }

        public String line(String source) {
            // TODO Auto-generated method stub
            //      <likelySubtag from="aa" to="aa_Latn_ET"/>
            // <!--{ Afar; ?; ? } => { Afar; Latin; Ethiopia }-->
            final String target = combineLSR(data.get0(), data.get1(), data.get2());
            final String origin = data.get3();
            final String result = "<likelySubtag from=\"" + source
                + "\" to=\"" + target
                + (origin.isBlank() ? "" : "\" origin=\"" + origin)
                + "\"/>"
                + "\t<!-- " + english.getName(source) + " ➡︎ " + english.getName(target) + " -->";
            return result;
        }

        public static String combineLSR(String lang, String script, String region) {
            return lang
                + (script.isEmpty() ? "" : "_" + script)
                + (region.isEmpty() ? "" : "_" + region);
        }
    }

    private static boolean isOk(String lang, String script, String region, Map<LstrType, Status> errors) {
        errors.clear();
        if (!LIKELY_SPECIALS.contains(lang)) {
            check(LstrType.language, lang, errors);
        }
        if (!FIX_VALIDITY.contains(script)) {
            check(LstrType.script, script, errors);
        }
        if (region.equals("001") && Iso639Data.getType(lang) == Type.Constructed){
            // ok
        } else {
            check(LstrType.region, region, errors);
        }
        return errors.isEmpty();
    }

    private static void check(LstrType lstrType, String lang, Map<LstrType, Status> errors) {
        final Status status = validity.getCodeToStatus(lstrType).get(lang);
        if (status != Status.regular) {
            errors.put(lstrType, status);
        }
    }

    private static class LikelySources {
        private static LikelySources SINGLETON = new LikelySources();
        public static Set<String> getSources() {
            return SINGLETON.alreadyLangs;
        }
        final ImmutableSet<String> alreadyLangs;
        private LikelySources() {
            Map<LstrType, Status> errors = new TreeMap<>();
            Map<String, String> likely = CLDR_CONFIG.getSupplementalDataInfo().getLikelySubtags();
            Set<String> _alreadyLangs = new TreeSet<>();
            _alreadyLangs.add("und");
            likely.forEach((key, value) -> {
                String lang = ltpFull.set(value).getLanguage();
                String script = ltpFull.set(value).getScript();
                String region = ltpFull.set(value).getRegion();
                _alreadyLangs.add(lang);
                if (!isOk(lang, script, region, errors)) {
                    showSkip("Skipping scope, CLDR", key, value, errors);
                }
            });
            System.out.println();
            alreadyLangs = ImmutableSet.copyOf(_alreadyLangs);
        }
    }

    static Multimap<String, String> langToRegion;

    public static void main(String[] args) {

        Map<String, LSRSource> result = new TreeMap<>();
        Map<LstrType, Status> errors = new TreeMap<>();

        Errors processErrors = new Errors();

        langToRegion = readWikidata(LikelySources.getSources());
        readJson(LikelySources.getSources(), result, processErrors );

        processErrors.printAll();

        if (ADD_SEED_EXEMPLARS) {

            for (String locale : factory.getAvailable()) {
                CLDRFile file = factory.make(locale, false);
                UnicodeSet exemplars = file.getExemplarSet(ExemplarType.main, null);
                String lang = ltpFull.set(locale).getLanguage();
                if (!LikelySources.getSources().contains(lang)) {
                    String script = getScript(exemplars);
                    Collection<String> regions = langToRegion.get(lang);
                    for (String region : regions) {
                        addIfOk(result, lang, lang, script, region, "wiki+exemplars", errors);
                    }
                }
            }
        }
        System.out.println();

        Multimap<String, String> defects = LinkedHashMultimap.create();

        for (Entry<String, LSRSource> entry : result.entrySet()) {
            String source = entry.getKey();
            LSRSource lsrs = entry.getValue();
            String tagLang = ltpTag.set(source).getLanguage();
            if (!result.containsKey(tagLang)) {
                defects.put(source, tagLang);
                showError("Missing lang record", source, lsrs.toString(), "Needs\t"  + tagLang);
            }
        }

        System.out.println("\nData to add: " + (result.entrySet().size() - defects.size()) + "\n");

        for (Entry<String, LSRSource> entry : result.entrySet()) {
            String source = entry.getKey();
            if (defects.containsKey(source)) {
                continue;
            }
            LSRSource lsrs = entry.getValue();
            System.out.println("\t\t" + lsrs.line(source));
        }

//        Multimap<String, String> likelyAdditions = TreeMultimap.create();
//        System.out.println("\nAdd");
//        likelyAdditions.asMap().entrySet().forEach(x -> {
//            String key = x.getKey();
//            if (x.getValue().size() == 1) {
//                for (String value : x.getValue()) {
//                    System.out.println(key + "\t" + value + "\t" + infoFields(value));
//                }
//            }
//        }
//            );
//
//        System.out.println("\nFix & Add");
//
//        likelyAdditions.asMap().entrySet().forEach(x -> {
//            String key = x.getKey();
//            if (x.getValue().size() != 1) {
//                for (String value : x.getValue()) {
//                    System.out.println(key + "\t" + value + "\t" + infoFields(value));
//                }
//                System.out.println();
//            }
//        }
//            );

    }

    static ImmutableMap<String, String> remap = ImmutableMap.of("iw", "he", "jw", "jv");

    private static void list(String string) {
        for (String code : string.split(" ")) {
            ltpFull.set(code.replace("-", "_"));
            String lang = ltpFull.getLanguage();
            String cldrLang = remap.get(lang);
            if (cldrLang != null) {
                lang = cldrLang;
            }

            System.out.println(code
                + "\t" + english.getName(code)
                + "\t" + Iso639Data.getType(lang)
                + "\t" + Iso639Data.getScope(lang));
        }
        System.out.println();
    }

    public static void showSkip(String message, String source, String target, Map<LstrType, Status> errors) {
        showError(message, source, target, infoFields(target) + "\t" + errors);
    }

    public static void showError(String message, String source, String target, String errors) {
        System.out.println(message + "\t" + source + " ➡ " + target + (errors.isEmpty() ? "" : "\t" + errors));
    }


    private static String infoFields(String value) {
        int under = value.indexOf('_');
        String lang = under < 0 ? value : value.substring(0,under);
        return english.getName(value)
            + "\t" + Iso639Data.getScope(lang)
            + "\t" + Iso639Data.getType(lang)
            ;
    }

    // add  <likelySubtag from="aa" to="aa_Latn_ET"/>, status

//    private static void handle(Entry<String, LSRSource> original, Multimap<String, String> likelyAdditions) {
//        String source = original.getKey();
//        LSRSource lsr = original.getValue();
//        if (source.contains("_")) {
//            int debug = 0;
//        }
//        // it is ok if there is a single LSR, eg
//        // eg aaa   Ghotuo  {Latn={NG=[sil]}}
//        // eg aak   Ankave  {Latn={PG=[sil, wiki+exemplars]}}
//
//        for (Entry<R3<String, String, String>, String> entry : lsr.data) {
//            addKeys(source, entry.getKey(), entry.getValue(),  likelyAdditions);
//        }
//    }

//    private static void addKeys(String source, R3<String, String, String> r3, String comment, Multimap<String, String> likelyAdditions) {
//        likelyAdditions.put(source, r3.get0() + "_" + r3.get1() + "_" + r3.get2() + comment);
//    }

    static final Pattern fullTagMatch = Pattern.compile("\\s*\"(full|tag)\": \"([^\"]+)\",");

    private static class Errors {
        public enum Type {
            ill_formed_tags("Ill-formed tags"),
            already_CLDR("Language already in CLDR"),
            tag_not_in_full("tag ⊄ full"),
            exception("exception");
            private final String printable;
            private Type(String printable) {
                this.printable = printable;
            }
        }
        public Multimap<Type, String> data = TreeMultimap.create();
        public void put(Type illFormedTags, String tagValue, String fullValue, String errorMessage) {
            data.put(illFormedTags, tagValue + " ➡ " + fullValue + (errorMessage == null || errorMessage.isEmpty() ? "" : "\t—\t" + errorMessage));
        }
        public void printAll() {
            for (Entry<Type, Collection<String>> entry : data.asMap().entrySet()) {
                Type type = entry.getKey();
                System.out.println();
                for (String message : entry.getValue()) {
                    System.out.println(type + "\t" + message);
                }
            }
        }
    }

    private static Map<String, LSRSource> readJson(Set<String> alreadyLangs, Map<String, LSRSource> result, Errors processErrors) {
        Path path = Paths.get(CLDRPaths.BIRTH_DATA_DIR, "/../external/langtags.json");
        Matcher full = fullTagMatch.matcher("");
        Map<LstrType, Status> errors = new TreeMap<>();

        Output<String> lastFull = new Output<>();
        try {
            Files.lines(path)
            .forEach(x -> {
                if (full.reset(x).matches()) {
                    final String key = full.group(1);
                    final String value = full.group(2).replace("-", "_");
                    if (value.startsWith("aai")) {
                        int debug = 0;
                    }
                    switch(key) {
                    case "full":
                        lastFull.value = value;
                        break;
                    case "tag":
                        try {
                            String fullLang = ltpFull.set(lastFull.value).getLanguage();
                            if (alreadyLangs.contains(fullLang)) {
                                processErrors.put(Errors.Type.already_CLDR, value, lastFull.value, "");
                                break;
                            } else if (isIllFormed(lastFull.value, ltpFull) || isIllFormed(value, ltpTag.set(value))) {
                                processErrors.put(Errors.Type.ill_formed_tags, value, lastFull.value, "");
                            } else {
                                String reference = SIL;
                                final String fullScript = ltpFull.getScript();
                                String fullRegion = ltpFull.getRegion();
                                if (fullRegion.equals("ZZ") || fullRegion.equals("001")) {
                                    Collection<String> tempRegions = langToRegion.get(fullLang); // synthesize
                                    if (!tempRegions.isEmpty()) {
                                        fullRegion = tempRegions.iterator().next();
                                        reference += " wikidata";
                                    }
                                }

                                String tagLang = ltpTag.getLanguage();
                                String tagScript = ltpTag.getScript();
                                String tagRegion = ltpTag.getRegion();

                                if (!tagLang.equals(fullLang)
                                    || (!tagScript.isEmpty() && !tagScript.equals(fullScript))
                                    || (!tagRegion.isEmpty() && !tagRegion.equals(fullRegion))
                                    ) {
                                    processErrors.put(Errors.Type.tag_not_in_full, value, lastFull.value, "");
                                } else {
                                    addIfOk(result, value, fullLang, fullScript, fullRegion, reference, errors);
                                }
                            }
                        } catch (Exception e) {
                            processErrors.put(Errors.Type.exception, value, lastFull.value, e.getMessage());
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(); // never happens
                    }
                }
            });
            return result;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static boolean isIllFormed(String source, LanguageTagParser languageTagParser) {
        return languageTagParser.getLanguage().isEmpty()
            || !languageTagParser.getVariants().isEmpty()
            || !languageTagParser.getExtensions().isEmpty()
            || !languageTagParser.getLocaleExtensions().isEmpty()
            || source.contains("@");
    }

    private static void addIfOk(Map<String, LSRSource> result, String source, String lang, final String script, final String region, String reference,
        Map<LstrType, Status> errors) {
        if (isOk(lang, script, region, errors)) {
            add(result, source, lang, script, region, reference);
        } else {
            showSkip("Skipping scope, SIL", source, ltpFull.toString(), errors);
        }
    }

    private static Multimap<String, String> readWikidata(Set<String> alreadyLangs) {
        Multimap<String, String> result = TreeMultimap.create();
        Path path = Paths.get(CLDRPaths.BIRTH_DATA_DIR, "/../external/wididata_lang_region.tsv");
        try {
            Files.lines(path)
            .forEach(x -> {
                if (!x.startsWith("#")) {
                    List<String> list = TAB_SPLITTER.splitToList(x);
                    String lang = list.get(1);
                    String region = list.get(3);
                    result.put(lang, region);
                }
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return result;
    }

    private static void add(Map<String, LSRSource> result, String source, String lang, final String script, final String region, String reference) {
        LSRSource old = result.get(source);
        LSRSource newVersion = new LSRSource(lang, script, region, reference);
        if (old != null && !old.equals(newVersion)) {
            throw new IllegalArgumentException("Data already exists for " + source + ": old=" + old + ", new: " + newVersion);
        }
        result.put(source, newVersion);
    }

    private static String getScript(UnicodeSet exemplars) {
        for (String s : exemplars) {
            int scriptNum = UScript.getScript(s.codePointAt(0));
            if (scriptNum != UScript.COMMON && scriptNum != UScript.INHERITED) {
                return UScript.getShortName(scriptNum);
            }
        }
        return "Zxxx";
    }
}
