package org.unicode.cldr.tool;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringIterables;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ULocale;

public class LanguageCodeConverter {
    private static Map<String, String> languageNameToCode = new TreeMap<String, String>();
    private static Set<String> exceptionCodes = new TreeSet<String>();
    private static Set<String> parseErrors = new LinkedHashSet<String>();

    private static Map<String, R2<List<String>, String>> languageAliases = CLDRConfig.getInstance().getSupplementalDataInfo().getLocaleAliasInfo()
        .get("language");

    /**
     * Public only for testing.
     *
     * @internal
     */
    public static final Map<String, String> GOOGLE_CLDR = Builder.with(new LinkedHashMap<String, String>()) // preserve order
        .put("iw", "he")
        .put("jw", "jv")
        .put("no", "nb")
        .put("tl", "fil")
        .put("pt-BR", "pt")
        .put("xx-bork", "x_bork")
        .put("xx-elmer", "x_elmer")
        .put("xx-hacker", "x_hacker")
        .put("xx-pirate", "x_pirate")
        .put("xx-klingon", "tlh")
        .put("zh-CN", "zh")
        .put("zh-TW", "zh_Hant")
        .put("zh-HK", "zh_Hant_HK")
        .put("sit-NP", "lif")
        .put("ut", "und")
        .put("un", "und")
        .put("xx", "und")

        // .put("sh", "fil")
        .freeze();

    /**
     * Public only for testing.
     *
     * @internal
     */
    public static final Map<String, String> CLDR_GOOGLE = Builder.with(new HashMap<String, String>())
        .putAllTransposed(GOOGLE_CLDR)
        .freeze();

    /**
     * Public only for testing.
     *
     * @internal
     */
    public static final Map<String, String> EXTRA_SCRIPTS = Builder.with(new HashMap<String, String>())
        .on("crs", "pcm", "tlh").put("Latn")
        .freeze();

    static {
        // Reads the CLDR copy of
        // http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
        Map<String, Map<String, Map<String, String>>> lstreg = StandardCodes.getLStreg();
        Map<String, Map<String, String>> languages = lstreg.get("language");
        Set<String> validCodes = new HashSet<String>();

        for (Entry<String, Map<String, String>> codeInfo : languages.entrySet()) {
            String code = codeInfo.getKey();
            R2<List<String>, String> replacement = languageAliases.get(code);
            // Returns "sh" -> <{"sr_Latn"}, reason>
            if (replacement != null) {
                List<String> replacements = replacement.get0();
                if (replacements.size() != 1) {
                    continue;
                }
                code = replacements.get(0);
                if (code.contains("_")) {
                    continue;
                }
            }
            // if (languageAliases.containsKey(code)) {
            // continue;
            // }
            final Map<String, String> info = codeInfo.getValue();
            String deprecated = info.get("Deprecated");
            if (deprecated != null) {
                continue;
            }
            String name = info.get("Description");
            if (name.equals("Private use")) {
                continue;
            }
            validCodes.add(code);
            if (name.contains(StandardCodes.DESCRIPTION_SEPARATOR)) {
                for (String namePart : name.split(StandardCodes.DESCRIPTION_SEPARATOR)) {
                    addNameToCode("lstr", code, namePart);
                }
            } else {
                addNameToCode("lstr", code, name);
            }
        }

        // CLDRFile english; // = testInfo.getEnglish();
        for (String code : validCodes) {
            String icuName = ULocale.getDisplayName(code, "en");
            addNameToCode("cldr", code, icuName);
            // if (languageAliases.containsKey(code)) {
            // continue;
            // }
            // String cldrName = english.getName("language", code);
            // if (cldrName != null && !cldrName.equals("private-use")) {
            // addNameToCode("cldr", code, cldrName);
            // }
        }
        // add exceptions
        LanguageTagParser ltp = new LanguageTagParser();
        for (String line : StringIterables.in(CldrUtility.getUTF8Data("external/alternate_language_names.txt"))) {
            String[] parts = CldrUtility.cleanSemiFields(line);
            if (parts == null || parts.length == 0) continue;
            String code = parts[0];
            if (!validCodes.contains(code)) {
                if (code.equals("*OMIT")) {
                    parseErrors.add("Skipping " + line);
                    continue;
                }
                String base = ltp.set(code).getLanguage();
                if (!validCodes.contains(base)) {
                    R2<List<String>, String> alias = languageAliases.get(base);
                    if (alias != null) {
                        code = alias.get0().get(0);
                    } else {
                        parseErrors.add("Skipping " + line);
                        continue;
                    }
                }
            }
            exceptionCodes.add(toUnderbarLocale(code));
            if (parts.length < 2) {
                continue;
            }
            String name = parts[1];
            if (parts.length > 2) {
                name += ";" + parts[2]; // HACK
            }
            addNameToCode("exception", code, name);
        }
        for (String cldr : GOOGLE_CLDR.values()) {
            String goodCode = toUnderbarLocale(cldr);
            exceptionCodes.add(goodCode);
        }
        languageNameToCode = Collections.unmodifiableMap(languageNameToCode);
        exceptionCodes = Collections.unmodifiableSet(exceptionCodes);
        parseErrors = Collections.unmodifiableSet(parseErrors);
    }

    private static void addNameToCode(final String type, final String code, String name) {
        if (code.equals("mru") && name.equals("mru")) {
            // mru=Mono (Cameroon)
            // mro=Mru
            // Ignore the CLDR mapping of the code to itself,
            // to avoid clobbering the mapping of the real name Mru to the real code mro.
            return;
        }
        name = name.toLowerCase(Locale.ENGLISH);
        String oldCode = languageNameToCode.get(name);
        if (oldCode != null) {
            if (!oldCode.equals(code)) {
                parseErrors.add("Name Collision! " + type + ": " + name + " <" + oldCode + ", " + code + ">");
            } else {
                return;
            }
        }
        languageNameToCode.put(name, code);
    }

    public static String toGoogleLocaleId(String localeId) {
        // TODO fix to do languages, etc. field by field
        localeId = localeId.replace("-", "_");
        String result = CLDR_GOOGLE.get(localeId);
        result = result == null ? localeId : result;
        return result.replace("_", "-");
    }

    public static String fromGoogleLocaleId(String localeId) {
        localeId = localeId.replace("_", "-");
        // TODO fix to do languages, etc. field by field
        String result = GOOGLE_CLDR.get(localeId);
        result = result == null ? localeId : result;
        return result.replace("-", "_");
    }

    public static String toUnderbarLocale(String localeId) {
        return localeId.replace("-", "_");
    }

    public static String toHyphenLocale(String localeId) {
        return localeId.replace("_", "-");
    }

    public static String getCodeForName(String languageName) {
        return languageNameToCode.get(languageName.toLowerCase(Locale.ENGLISH));
    }

    public static Set<String> getExceptionCodes() {
        return exceptionCodes;
    }

    public static Set<String> getParseErrors() {
        return parseErrors;
    }

    public static Map<String, String> getLanguageNameToCode() {
        return languageNameToCode;
    }

}
