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
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;

public class GenerateAdditionalLikely {

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Splitter UNDERBAR = Splitter.on('_');
    private static final Splitter TAB_SPLITTER = Splitter.on('\t');

    static class ScriptRegion {
        Multimap<Pair<String, String>, String> data = TreeMultimap.create();

        public void put(String script, String region, String source) {
            data.put(Pair.of(script, region), source);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    static Factory factory = CLDR_CONFIG.getExemplarsFactory();
    static CLDRFile english = CLDR_CONFIG.getEnglish();
    static LanguageTagParser ltp = new LanguageTagParser();
    static Validity validity = Validity.getInstance();

    static final Set<String> LANGUAGE_REGULAR = validity.getStatusToCodes(LstrType.language).get(Status.regular);
    static final Set<String> SCRIPT_REGULAR = validity.getStatusToCodes(LstrType.script).get(Status.regular);
    static final Set<String> REGION_REGULAR = validity.getStatusToCodes(LstrType.region).get(Status.regular);

    static final Set<String> LIKELY_SPECIALS = ImmutableSet.of("in", "iw", "ji", "jw", "mo");
    static final Set<String> FIX_VALIDITY = ImmutableSet.of("Zanb");
    static final Set<String> FIX_COUNTRY = ImmutableSet.of("yi");

    public static boolean isOk(String lang, String script, String region, Map<LstrType, Status> errors) {
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

    public static void check(LstrType lstrType, String lang, Map<LstrType, Status> errors) {
        final Status status = validity.getCodeToStatus(lstrType).get(lang);
        if (status != Status.regular) {
            errors.put(lstrType, status);
        }
    }

    public static void main(String[] args) {
        // list("de en es es-419 fr hr it nl pl pt-BR pt-PT vi tr ru ar th ko zh-CN zh-TW ja ach af ak az ban su xx-bork bs br ca ceb cs sn co ht cy da yo et xx-elmer eo eu ee tl fil fo fy gaa ga gd gl gn xx-hacker ha haw bem ig rn id ia xh zu is jw rw sw tlh kg mfe kri la lv to lt ln loz lua lg hu mg mt mi ms pcm no nn nso ny uz oc om xx-pirate ro rm qu nyn crs sq sk sl so st sr-ME sr-Latn fi sv tn tum tk tw wo el be bg ky kk mk mn sr tt tg uk ka hy yi iw ug ur ps sd fa ckb ti am ne mr hi bn pa gu or ta te kn ml si lo my km chr");
        Map<String, String> likely = CLDR_CONFIG.getSupplementalDataInfo().getLikelySubtags();

        Map<LstrType, Status> errors = new TreeMap<>();

        ImmutableSet<String> alreadyLangs = loadLikely(likely, errors);

        Map<String, ScriptRegion> result = new TreeMap<>();

        readJson(alreadyLangs, result);

        if (false) {
            Multimap<String, String> langToRegion = TreeMultimap.create();
            readWikidata(alreadyLangs, langToRegion);

            for (String locale : factory.getAvailable()) {
                CLDRFile file = factory.make(locale, false);
                UnicodeSet exemplars = file.getExemplarSet(ExemplarType.main, null);
                String lang = ltp.set(locale).getLanguage();
                if (!alreadyLangs.contains(lang)) {
                    String script = getScript(exemplars);
                    Collection<String> regions = langToRegion.get(lang);
                    for (String region : regions) {
                        addIfOk(result, lang, script, region, "wiki+exemplars", errors);
                    }
                }
            }
        }
        System.out.println();
        Multimap<String, String> likelyAdditions = TreeMultimap.create();

        for (Entry<String, ScriptRegion> entry : result.entrySet()) {
            handle(entry, likelyAdditions);
        }
        System.out.println("\nAdd");
        likelyAdditions.asMap().entrySet().forEach(x -> {
            String key = x.getKey();
            if (x.getValue().size() == 1) {
                for (String value : x.getValue()) {
                    System.out.println(key + "\t" + value + "\t" + infoFields(value));
                }
            }
        }
            );

        System.out.println("\nFix & Add");

        likelyAdditions.asMap().entrySet().forEach(x -> {
            String key = x.getKey();
            if (x.getValue().size() != 1) {
                for (String value : x.getValue()) {
                    System.out.println(key + "\t" + value + "\t" + infoFields(value));
                }
                System.out.println();
            }
        }
            );

    }

    static ImmutableMap<String, String> remap = ImmutableMap.of("iw", "he", "jw", "jv");
    private static void list(String string) {
        for (String code : string.split(" ")) {
            ltp.set(code.replace("-", "_"));
            String lang = ltp.getLanguage();
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

    public static ImmutableSet<String> loadLikely(Map<String, String> likely, Map<LstrType, Status> errors) {
        Set<String> _alreadyLangs = new TreeSet<>();
        _alreadyLangs.add("und");
        likely.forEach((key, value) -> {
            String lang = ltp.set(value).getLanguage();
            String script = ltp.set(value).getScript();
            String region = ltp.set(value).getRegion();
            if (isOk(lang, script, region, errors)) {
                _alreadyLangs.add(lang);
            } else if (!region.equals("ZZ")){
                System.out.println("Current Likely\tSkipping:\t" + key + "\t" + value + "\t" + infoFields(value) + "\t" + errors);
            }
        });
        System.out.println();
        ImmutableSet<String> alreadyLangs = ImmutableSet.copyOf(_alreadyLangs);
        return alreadyLangs;
    }

    public static String infoFields(String value) {
        int under = value.indexOf('_');
        String lang = under < 0 ? value : value.substring(0,under);
        return english.getName(value)
            + "\t" + Iso639Data.getScope(lang)
            + "\t" + Iso639Data.getType(lang)
            ;
    }

    // add  <likelySubtag from="aa" to="aa_Latn_ET"/>, status

    private static void handle(Entry<String, ScriptRegion> entry, Multimap<String, String> likelyAdditions) {
        String lang = entry.getKey();
        ScriptRegion scriptRegion = entry.getValue();
        // it is ok if there is a single LSR, eg
        // eg aaa   Ghotuo  {Latn={NG=[sil]}}
        // eg aak   Ankave  {Latn={PG=[sil, wiki+exemplars]}}

        String comment = "";
        for (Entry<Pair<String, String>, String> key : scriptRegion.data.entries()) {
            addKeys(lang, key.getKey(), comment,  likelyAdditions);
        }
    }

    public static void addKeys(String lang, Pair<String, String> key, String comment, Multimap<String, String> likelyAdditions) {
        likelyAdditions.put(lang, lang + "_" + key.getFirst() + "_" + key.getSecond() + comment);
    }

    static final Pattern fullTagMatch = Pattern.compile("\\s*\"(full|tag)\": \"([^\"]+)\",");

    public static Map<String, ScriptRegion> readJson(ImmutableSet<String> alreadyLangs, Map<String, ScriptRegion> result) {
        Path path = Paths.get(CLDRPaths.BIRTH_DATA_DIR, "/../external/langtags.json");
        Matcher full = fullTagMatch.matcher("");
        Map<LstrType, Status> errors = new TreeMap<>();

        Output<String> lastFull = new Output<>(null);
        try {
            Files.lines(path)
            .forEach(x -> {
                if (full.reset(x).matches()) {
                    final String key = full.group(1).replace("-", "_");
                    final String value = full.group(2).replace("-", "_");
                    switch(key) {
                    case "full": lastFull.value = value; break;
                    case "tag":
                        try {
                            String lang = ltp.set(lastFull.value).getLanguage();
                            if (!alreadyLangs.contains(lang)) {
                                if (lang.isEmpty()
                                    || !ltp.getVariants().isEmpty()
                                    || !ltp.getExtensions().isEmpty()
                                    || !ltp.getLocaleExtensions().isEmpty()
                                    || lastFull.value.contains("@")
                                    ) {
                                    System.out.println("SIL\tParse Error\t" + lastFull.value + " from " + value);
                                } else {
                                    final String script = ltp.getScript();
                                    final String region = ltp.getRegion();
                                    String source = "SIL";
                                    if (!value.equals(lang)) {
                                        System.out.println(value + "=>" + lastFull.value);
                                    }
                                    addIfOk(result, lang, script, region, source, errors);
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("SIL\tParse Error\t" + lastFull.value + " from " + value + "\terror\t" + e.getMessage());
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

    public static void addIfOk(Map<String, ScriptRegion> result, String lang, final String script, final String region, String source,
        Map<LstrType, Status> errors) {
        if (isOk(lang, script, region, errors)) {
            add(result, lang, script, region, source);
        } else {
            System.out.println(source + "\tSkipping:\t" + ltp.toString() + "\t" + infoFields(ltp.toString()) + "\t" + errors);
        }
    }

    public static void readWikidata(ImmutableSet<String> alreadyLangs, Multimap<String, String> result) {
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
    }

    public static void add(Map<String, ScriptRegion> result, String lang, final String script, final String region, String source) {
        ScriptRegion old = result.get(lang);
        if (old == null) {
            result.put(lang, old = new ScriptRegion());
        }
        old.put(script, region, source);
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
