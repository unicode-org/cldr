package org.unicode.cldr.tool;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
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
    static final CLDRFile english = CLDR_CONFIG.getEnglish();

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
        LanguageTagCanonicalizer langCanoner = new LanguageTagCanonicalizer(null);
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
                                            if (lastFull.value == null) {
                                                break;
                                            }
                                            try {
                                                ltpFull.set(lastFull.value);
                                                ltpTag.set(value);
                                                if (isIllFormed(lastFull.value, ltpFull)
                                                        || isIllFormed(value, ltpTag)) {
                                                    processErrors.put(
                                                            Errors.Type.ill_formed_tags,
                                                            value,
                                                            lastFull.value,
                                                            "");
                                                } else {
                                                    final String fixedTag =
                                                            langCanoner.transform(value);
                                                    final String fixedFull =
                                                            langCanoner.transform(lastFull.value);
                                                    if (!fixedTag.equals(value)
                                                            || !fixedFull.equals(lastFull.value)) {
                                                        processErrors.put(
                                                                Errors.Type.canonicalizing,
                                                                value,
                                                                lastFull.value,
                                                                "mapped to: "
                                                                        + fixedTag
                                                                        + " ➡ "
                                                                        + fixedFull);
                                                        ltpTag.set(fixedTag);
                                                        ltpFull.set(fixedFull);
                                                    }
                                                    String fullLang = ltpFull.getLanguage();
                                                    final String fullScript = ltpFull.getScript();
                                                    String fullRegion = ltpFull.getRegion();

                                                    String reference = SIL;
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

                                                    final String tagLang = ltpTag.getLanguage();
                                                    final String tagScript = ltpTag.getScript();
                                                    final String tagRegion = ltpTag.getRegion();

                                                    if (!tagScript.isEmpty()
                                                            && !tagRegion.isEmpty()) {
                                                        processErrors.put(
                                                                Errors.Type.tag_is_full,
                                                                value,
                                                                lastFull.value,
                                                                "");
                                                    } else if (!tagLang.equals(fullLang)
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
                                                                    fixedTag,
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

            // check for items that need context

            Set<String> toRemove = new LinkedHashSet<>();
            for (Entry<String, LSRSource> entry : result.entrySet()) {
                // if we have lang_script or lang_region, we must have lang
                final String source = entry.getKey();
                if (source.equals("lfn_Cyrl")) {
                    int debug = 0;
                }
                if (source.contains("_")) {
                    // we have either aaa_Dddd or aaa_EEE (we know the source can't have 3 fields)
                    CLDRLocale clocale = CLDRLocale.getInstance(source);
                    final String language = clocale.getLanguage();
                    LSRSource fullForLanguage = result.get(language);
                    if (fullForLanguage == null) {
                        toRemove.add(source);
                        processErrors.put(
                                Errors.Type.language_of_tag_missing,
                                source,
                                entry.getValue().getLsrString(),
                                "but no mapping for " + language);
                    } else {
                        CLDRLocale targetForLanguage = fullForLanguage.getCldrLocale();
                        CLDRLocale target = entry.getValue().getCldrLocale();
                        // The missing value in LSRSource must not be the same as what would come in
                        // that is, if we have aaa => aaa_Bbbb_CC, then we cannot have:
                        // aaa_Dddd => aaa_Dddd_CC, nor
                        // aaa_EE => aaa_Bbbb_EE, nor
                        if (target.getLanguage().equals(targetForLanguage.getLanguage())
                                || target.getScript().equals(targetForLanguage.getScript())) {
                            toRemove.add(source);
                            processErrors.put(
                                    Errors.Type.redundant_mapping,
                                    source,
                                    entry.getValue().getLsrString(),
                                    "because: " + language + " ➡ " + targetForLanguage);
                        }
                    }
                }
            }
            for (String badKey : toRemove) {
                result.remove(badKey);
            }

            // protect the results

            processErrors.data = CldrUtility.protectCollection(processErrors.data);
            return CldrUtility.protectCollection(result);
        } catch (IOException ex) {
            throw new ICUUncheckedIOException(ex);
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
            throw new ICUUncheckedIOException(ex);
        }
        return ImmutableMultimap.copyOf(result);
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

    public static class Errors {
        public enum Type {
            ill_formed_tags("Ill-formed tags"),
            already_CLDR("Language already in CLDR"),
            tag_not_in_full("tag ⊄ full"),
            exception("exception"),
            skipping_scope("Skipping scope, SIL"),
            tag_is_full("Tag must not have both script and region"),
            language_of_tag_missing("Missing tag for just the language"),
            redundant_mapping(
                    "aaa => aaa_Bbbb_CC makes redundant aaa_Dddd => aaa_Dddd_CC & aaa_EE => aaa_Bbbb_EE"),
            canonicalizing("either the source or target are not canonical");

            private final String printable;

            private Type(String printable) {
                this.printable = printable;
            }
        }

        private Multimap<Type, String> data = TreeMultimap.create();

        public Multimap<Type, String> getData() {
            return data;
        }

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
}
