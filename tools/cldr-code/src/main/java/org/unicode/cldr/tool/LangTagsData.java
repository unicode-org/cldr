package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.util.Output;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

public class LangTagsData {
    private final Pattern fullTagMatch = Pattern.compile("\\s*\"(full|tag)\": \"([^\"]+)\",");
    private final String SIL = "sil1";

    private final Splitter TAB_SPLITTER = Splitter.on('\t');
    private final Set<String> LIKELY_SPECIALS = ImmutableSet.of("in", "iw", "ji", "jw", "mo");
    private final Set<String> FIX_VALIDITY = ImmutableSet.of("Zanb");
    private final Set<String> FIX_COUNTRY = ImmutableSet.of("yi");
    private final Validity validity = Validity.getInstance();

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final CLDRFile english = CLDR_CONFIG.getEnglish();

    private static final LangTagsData INSTANCE = new LangTagsData();

    private final Multimap<String, String> wikiData;
    private final Map<String, LSRSource> jsonData;
    private final Errors processErrors = new Errors();

    private LangTagsData() {
        wikiData = readWikidata();
        jsonData = readJson();
    }

    public static LangTagsData getInstance() {
        return INSTANCE;
    }

    public static Multimap<String, String> getWikiData() {
        return getInstance().wikiData;
    }

    public static Map<String, LSRSource> getJsonData() {
        return getInstance().jsonData;
    }

    public static Errors getProcessErrors() {
        return getInstance().processErrors;
    }

    private Map<String, LSRSource> readJson() {

        final LanguageTagParser ltpFull = new LanguageTagParser();
        final LanguageTagParser ltpTag = new LanguageTagParser();

        Path path = Paths.get(CLDRPaths.BIRTH_DATA_DIR, "/../external/langtags.json");
        if (!Files.exists(path)) {
            throw new IllegalArgumentException(path + " does not exist");
        }

        Matcher full = fullTagMatch.matcher("");
        Map<LstrType, Status> errors = new TreeMap<>();

        Output<String> lastFull = new Output<>();
        Map<String, LSRSource> result = new TreeMap<>();
        try {
            Files.lines(path)
                    .forEach(
                            x -> {
                                if (full.reset(x).matches()) {
                                    final String key = full.group(1);
                                    final String value = full.group(2).replace("-", "_");
                                    if (value.startsWith("aai")) {
                                        int debug = 0;
                                    }
                                    switch (key) {
                                        case "full":
                                            lastFull.value = value;
                                            break;
                                        case "tag":
                                            try {
                                                String fullLang =
                                                        ltpFull.set(lastFull.value).getLanguage();
                                                if (isIllFormed(lastFull.value, ltpFull)
                                                        || isIllFormed(value, ltpTag.set(value))) {
                                                    processErrors.put(
                                                            Errors.Type.ill_formed_tags,
                                                            value,
                                                            lastFull.value,
                                                            "");
                                                } else {
                                                    String reference = SIL;
                                                    final String fullScript = ltpFull.getScript();
                                                    String fullRegion = ltpFull.getRegion();
                                                    if (fullRegion.equals("ZZ")
                                                            || fullRegion.equals("001")) {
                                                        Collection<String> tempRegions =
                                                                wikiData.get(
                                                                        fullLang); // synthesize
                                                        if (!tempRegions.isEmpty()) {
                                                            fullRegion =
                                                                    tempRegions.iterator().next();
                                                            reference += " wikidata";
                                                        }
                                                    }

                                                    String tagLang = ltpTag.getLanguage();
                                                    String tagScript = ltpTag.getScript();
                                                    String tagRegion = ltpTag.getRegion();

                                                    if (!tagLang.equals(fullLang)
                                                            || (!tagScript.isEmpty()
                                                                    && !tagScript.equals(
                                                                            fullScript))
                                                            || (!tagRegion.isEmpty()
                                                                    && !tagRegion.equals(
                                                                            fullRegion))) {
                                                        processErrors.put(
                                                                Errors.Type.tag_not_in_full,
                                                                value,
                                                                lastFull.value,
                                                                "");
                                                    } else {
                                                        if (isOk(
                                                                fullLang,
                                                                fullScript,
                                                                fullRegion,
                                                                errors)) {
                                                            add(
                                                                    result,
                                                                    value,
                                                                    fullLang,
                                                                    fullScript,
                                                                    fullRegion,
                                                                    reference);
                                                        } else {
                                                            processErrors.put(
                                                                    Errors.Type.skipping_scope,
                                                                    value,
                                                                    ltpFull.toString(),
                                                                    errors.toString());
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {
                                                processErrors.put(
                                                        Errors.Type.exception,
                                                        value,
                                                        lastFull.value,
                                                        e.getMessage());
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

    private boolean isIllFormed(String source, LanguageTagParser languageTagParser) {
        return languageTagParser.getLanguage().isEmpty()
                || !languageTagParser.getVariants().isEmpty()
                || !languageTagParser.getExtensions().isEmpty()
                || !languageTagParser.getLocaleExtensions().isEmpty()
                || source.contains("@");
    }

    private boolean isOk(String lang, String script, String region, Map<LstrType, Status> errors) {
        errors.clear();
        if (!LIKELY_SPECIALS.contains(lang)) {
            check(LstrType.language, lang, errors);
        }
        if (!FIX_VALIDITY.contains(script)) {
            check(LstrType.script, script, errors);
        }
        if (region.equals("001") && Iso639Data.getType(lang) == Type.Constructed) {
            // ok
        } else {
            check(LstrType.region, region, errors);
        }
        return errors.isEmpty();
    }

    private void check(LstrType lstrType, String lang, Map<LstrType, Status> errors) {
        final Status status = validity.getCodeToStatus(lstrType).get(lang);
        if (status != Status.regular) {
            errors.put(lstrType, status);
        }
    }

    private Multimap<String, String> readWikidata() {
        Multimap<String, String> result = TreeMultimap.create();
        Path path =
                Paths.get(CLDRPaths.BIRTH_DATA_DIR, "/../external/wididata_lang_region.tsv")
                        .normalize();
        if (!Files.exists(path)) {
            throw new IllegalArgumentException(path + " does not exist");
        }
        try {
            Files.lines(path)
                    .forEach(
                            x -> {
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

    private void add(
            Map<String, LSRSource> result,
            String source,
            String lang,
            final String script,
            final String region,
            String reference) {
        LSRSource old = result.get(source);
        LSRSource newVersion = new LSRSource(lang, script, region, reference);
        if (old != null && !old.equals(newVersion)) {
            throw new IllegalArgumentException(
                    "Data already exists for " + source + ": old=" + old + ", new: " + newVersion);
        }
        result.put(source, newVersion);
    }

    private static class Errors {
        public enum Type {
            ill_formed_tags("Ill-formed tags"),
            already_CLDR("Language already in CLDR"),
            tag_not_in_full("tag ⊄ full"),
            exception("exception"),
            skipping_scope("Skipping scope, SIL");

            private final String printable;

            private Type(String printable) {
                this.printable = printable;
            }
        }

        public Multimap<Type, String> data = TreeMultimap.create();

        public void put(
                Type illFormedTags, String tagValue, String fullValue, String errorMessage) {
            data.put(
                    illFormedTags,
                    tagValue
                            + " ➡ "
                            + fullValue
                            + (errorMessage == null || errorMessage.isEmpty()
                                    ? ""
                                    : "\t—\t" + errorMessage));
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

    static class LSRSource implements Comparable<LSRSource> {
        final Row.R4<String, String, String, String> data;

        LSRSource(String lang, String script, String region, String source) {
            if (script.contains("Soyo") || region.contains("Soyo")) {
                int debug = 0;
            }
            data = Row.of(lang, script, region, source);
            data.freeze();
        }

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
            final String result =
                    "<likelySubtag from=\""
                            + source
                            + "\" to=\""
                            + target
                            + (origin.isBlank() ? "" : "\" origin=\"" + origin)
                            + "\"/>"
                            + "\t<!-- "
                            + english.getName(source)
                            + " ➡︎ "
                            + english.getName(target)
                            + " -->";
            return result;
        }

        public static String combineLSR(String lang, String script, String region) {
            return lang
                    + (script.isEmpty() ? "" : "_" + script)
                    + (region.isEmpty() ? "" : "_" + region);
        }
    }
}
