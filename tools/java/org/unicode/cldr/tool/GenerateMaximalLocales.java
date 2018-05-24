package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Problems:
 * "und_Hani", "zh_Hani"
 * "und_Sinh", "si_Sinh"
 *
 * @author markdavis
 *
 */
public class GenerateMaximalLocales {

    private static final String TEMP_UNKNOWN_REGION = "XZ";

    private static final String DEBUG_ADD_KEY = "und_Latn_ZA";

    private static final boolean SHOW_ADD = CldrUtility.getProperty("GenerateMaximalLocalesDebug", false);
    private static final boolean SUPPRESS_CHANGES = CldrUtility.getProperty("GenerateMaximalLocalesSuppress", false);
    private static final boolean SHOW_CONTAINERS = false;

    enum OutputStyle {
        PLAINTEXT, C, C_ALT, XML
    };

    private static OutputStyle OUTPUT_STYLE = OutputStyle.valueOf(CldrUtility.getProperty("OutputStyle", "XML", "XML")
        .toUpperCase());

    // set based on above
    private static final String SEPARATOR = OUTPUT_STYLE == OutputStyle.C || OUTPUT_STYLE == OutputStyle.C_ALT ? CldrUtility.LINE_SEPARATOR
        : "\t";
    private static final String TAG_SEPARATOR = OUTPUT_STYLE == OutputStyle.C_ALT ? "-" : "_";
    // private static final boolean FAVOR_REGION = true; // OUTPUT_STYLE == OutputStyle.C_ALT;

    private static final boolean tryDifferent = true;

    private static final File list[] = {
        new File(CLDRPaths.MAIN_DIRECTORY),
        new File(CLDRPaths.SEED_DIRECTORY),
        new File(CLDRPaths.EXEMPLARS_DIRECTORY) };

    private static Factory factory = SimpleFactory.make(list, ".*");
    private static SupplementalDataInfo supplementalData = SupplementalDataInfo
        .getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
    private static StandardCodes standardCodes = StandardCodes.make();
    private static CLDRFile english = factory.make("en", false);
    static Relation<String, String> cldrContainerToLanguages = Relation.of(new HashMap<String, Set<String>>(), HashSet.class);
    static {
        for (CLDRLocale locale : ToolConfig.getToolInstance().getCldrFactory().getAvailableCLDRLocales()) {
            String region = locale.getCountry();
            if (region == null || region.isEmpty() || Containment.isLeaf(region)) {
                continue;
            }
            cldrContainerToLanguages.put(region, locale.getLanguage());
        }
        cldrContainerToLanguages.freeze();
        System.out.println("Keep containers " + cldrContainerToLanguages);
    }

    private static final List<String> KEEP_TARGETS = Arrays.asList("und_Arab_PK", "und_Latn_ET");
    private static final ImmutableSet<String> deprecatedISONotInLST = ImmutableSet.of("scc", "scr");

    /**
     * This is the simplest way to override, by supplying the max value. 
     * It gets a very low weight, so doesn't override any stronger value.
     */
    private static final String[] MAX_ADDITIONS = new String[] {
        "bss_Latn_CM",
        "gez_Ethi_ET",
        "ken_Latn_CM",
        "und_Arab_PK",
        "wa_Latn_BE",

        "fub_Arab_CM",
        "fuf_Latn_GN",
        "kby_Arab_NE",
        "kdh_Arab_TG",
        "apd_Arab_TG",
        "zlm_Latn_TG",

        "cr_Cans_CA",
        "hif_Latn_FJ",
        "gon_Telu_IN",
        "lzz_Latn_TR",
        "lif_Deva_NP",
        "unx_Beng_IN",
        "unr_Beng_IN",
        "ttt_Latn_AZ",
        "pnt_Grek_GR",
        "tly_Latn_AZ",
        "tkr_Latn_AZ",
        "bsq_Bass_LR",
        "ccp_Cakm_BD",
        "blt_Tavt_VN",
        "rhg_Arab_MM",
        "rhg_Rohg_MM",
    };

    /**
     * The following overrides MASH the final values, so they may not result in consistent results. Safer is to add to MAX_ADDITIONS.
     * However, if you add, add both the language and language+script mappings.
     */
    // Many of the overrides below can be removed once the language/pop/country data is updated.
    private static final Map<String, String> LANGUAGE_OVERRIDES = CldrUtility.asMap(new String[][] {
        { "eo", "eo_Latn_001" },
        { "eo_Latn", "eo_Latn_001" },
        { "es", "es_Latn_ES" },
        { "es_Latn", "es_Latn_ES" },
        { "ff_Adlm", "ff_Adlm_GN" },
        { "io", "io_Latn_001" },
        { "io_Latn", "io_Latn_001" },
        { "jbo", "jbo_Latn_001" },
        { "jbo_Latn", "jbo_Latn_001" },
        { "ku_Arab", "ku_Arab_IQ" },
        { "lrc", "lrc_Arab_IR" },
        { "lrc_Arab", "lrc_Arab_IR" },
        { "man", "man_Latn_GM" },
        { "man_Latn", "man_Latn_GM" },
        { "mas", "mas_Latn_KE" },
        { "mas_Latn", "mas_Latn_KE" },
        { "mn", "mn_Cyrl_MN" },
        { "mn_Cyrl", "mn_Cyrl_MN" },
        { "mro", "mro_Mroo_BD" },
        { "mro_BD", "mro_Mroo_BD" },
        { "ms_Arab", "ms_Arab_MY" },
        { "pap", "pap_Latn_AW" },
        { "pap_Latn", "pap_Latn_AW" },
        { "prg", "prg_Latn_001" },
        { "prg_Latn", "prg_Latn_001" },
        { "rif", "rif_Tfng_MA" },
        { "rif_Latn", "rif_Latn_MA" },
        { "rif_Tfng", "rif_Tfng_MA" },
        { "rif_MA", "rif_Tfng_MA" },
        { "shi", "shi_Tfng_MA" },
        { "shi_Tfng", "shi_Tfng_MA" },
        { "shi_MA", "shi_Tfng_MA" },
        { "sr_Latn", "sr_Latn_RS" },
        { "ss", "ss_Latn_ZA" },
        { "ss_Latn", "ss_Latn_ZA" },
        { "swc", "swc_Latn_CD" },
        { "ti", "ti_Ethi_ET" },
        { "ti_Ethi", "ti_Ethi_ET" },
        { "und", "en_Latn_US" },
        { "und_Arab", "ar_Arab_EG" },
        { "und_Arab_PK", "ur_Arab_PK" },
        { "und_Bopo", "zh_Bopo_TW" },
        { "und_Deva_FJ", "hif_Deva_FJ" },
        { "und_EZ", "de_Latn_EZ" },
        { "und_Hani", "zh_Hani_CN" },
        { "und_Hani_CN", "zh_Hani_CN" },
        { "und_Kana", "ja_Kana_JP" },
        { "und_Kana_JP", "ja_Kana_JP" },
        { "und_Latn", "en_Latn_US" },
        { "und_Latn_ET", "en_Latn_ET" },
        { "und_Latn_NE", "ha_Latn_NE" },
        { "und_Latn_PH", "fil_Latn_PH" },
        { "und_ML", "bm_Latn_ML" },
        { "und_Latn_ML", "bm_Latn_ML" },
        { "und_MU", "mfe_Latn_MU" },
        { "und_NE", "ha_Latn_NE" },
        { "und_PH", "fil_Latn_PH" },
        { "und_PK", "ur_Arab_PK" },
        { "und_SO", "so_Latn_SO" },
        { "und_SS", "en_Latn_SS" },
        { "und_TK", "tkl_Latn_TK" },
        { "und_UN", "en_Latn_UN" },
        { "vo", "vo_Latn_001" },
        { "vo_Latn", "vo_Latn_001" },
        { "yi", "yi_Hebr_001" },
        { "yi_Hebr", "yi_Hebr_001" },
        { "yue", "yue_Hant_HK" },
        { "yue_Hant", "yue_Hant_HK" },
        { "yue_Hans", "yue_Hans_CN" },
        { "yue_CN", "yue_Hans_CN" },
        { "zh_Hani", "zh_Hani_CN" },

        { "zh_Bopo", "zh_Bopo_TW" },
        { "ccp", "ccp_Cakm_BD" },
        { "ccp_Cakm", "ccp_Cakm_BD" },
        { "und_Cakm", "ccp_Cakm_BD" },
        { "cu_Glag", "cu_Glag_BG" },
        { "sd_Khoj", "sd_Khoj_IN" },
        { "lif_Limb", "lif_Limb_IN" },
        { "grc_Linb", "grc_Linb_GR" },
        { "arc_Nbat", "arc_Nbat_JO" },
        { "arc_Palm", "arc_Palm_SY" },
        { "pal_Phlp", "pal_Phlp_CN" },
        { "en_Shaw", "en_Shaw_GB" },
        { "sd_Sind", "sd_Sind_IN" },
        { "und_Brai", "fr_Brai_FR" }, // hack
        { "und_Hanb", "zh_Hanb_TW" }, // Special script code
        { "zh_Hanb", "zh_Hanb_TW" }, // Special script code
        { "und_Jamo", "ko_Jamo_KR" }, // Special script code

        //{"und_Cyrl_PL", "be_Cyrl_PL"},

//        {"cr", "cr_Cans_CA"},
//        {"hif", "hif_Latn_FJ"},
//        {"gon", "gon_Telu_IN"},
//        {"lzz", "lzz_Latn_TR"},
//        {"lif", "lif_Deva_NP"},
//        {"unx", "unx_Beng_IN"},
//        {"unr", "unr_Beng_IN"},
//        {"ttt", "ttt_Latn_AZ"},
//        {"pnt", "pnt_Grek_GR"},
//        {"tly", "tly_Latn_AZ"},
//        {"tkr", "tkr_Latn_AZ"},
//        {"bsq", "bsq_Bass_LR"},
//        {"ccp", "ccp_Cakm_BD"},
//        {"blt", "blt_Tavt_VN"},
        { "mis_Medf", "mis_Medf_NG" },
    });

    /**
     * The following supplements the suppress-script. It overrides info from exemplars and the locale info.
     */
    private static String[][] SpecialScripts = {
        { "zh", "Hans" }, // Hans (not Hani)
        { "yue", "Hant" }, // Hans (not Hani)
        { "chk", "Latn" }, // Chuukese (Micronesia)
        { "fil", "Latn" }, // Filipino (Philippines)"
        { "ko", "Kore" }, // Korean (North Korea)
        { "ko_KR", "Kore" }, // Korean (North Korea)
        { "pap", "Latn" }, // Papiamento (Netherlands Antilles)
        { "pau", "Latn" }, // Palauan (Palau)
        { "su", "Latn" }, // Sundanese (Indonesia)
        { "tet", "Latn" }, // Tetum (East Timor)
        { "tk", "Latn" }, // Turkmen (Turkmenistan)
        { "ty", "Latn" }, // Tahitian (French Polynesia)
        { "ja", "Jpan" }, // Special script for japan
        { "und", "Latn" }, // Ultimate fallback
    };

    private static Map<String, String> localeToScriptCache = new TreeMap<String, String>();
    static {
        for (String language : standardCodes.getAvailableCodes("language")) {
            Map<String, String> info = standardCodes.getLangData("language", language);
            String script = info.get("Suppress-Script");
            if (script != null) {
                localeToScriptCache.put(language, script);
            }
        }
        for (String[] pair : SpecialScripts) {
            localeToScriptCache.put(pair[0], pair[1]);
        }
    }

    private static Map<String, String> FALLBACK_SCRIPTS;
    static {
        LanguageTagParser additionLtp = new LanguageTagParser();
        Map<String, String> _FALLBACK_SCRIPTS = new TreeMap<>();
        for (String addition : MAX_ADDITIONS) {
            additionLtp.set(addition);
            String lan = additionLtp.getLanguage();
            _FALLBACK_SCRIPTS.put(lan, additionLtp.getScript());
        }
        FALLBACK_SCRIPTS = ImmutableMap.copyOf(_FALLBACK_SCRIPTS);
    }

    private static int errorCount;

    public static void main(String[] args) throws IOException {

        printDefaultLanguagesAndScripts();

        Map<String, String> toMaximized = new TreeMap<String, String>();

        tryDifferentAlgorithm(toMaximized);

        minimize(toMaximized);

        // HACK TEMP_UNKNOWN_REGION
        // this is to get around the removal of items with ZZ in minimize.
        // probably cleaner way to do it, but this provides control over just those we want to retain.
        Set<String> toRemove = new TreeSet<>();
        Map<String, String> toFix = new TreeMap<>();
        for (Entry<String, String> entry : toMaximized.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.contains(TEMP_UNKNOWN_REGION)) {
                toRemove.add(key);
            } else if (value.contains(TEMP_UNKNOWN_REGION)) {
                toFix.put(key, value.replace(TEMP_UNKNOWN_REGION, UNKNOWN_REGION));
            }
        }
        for (String key : toRemove) {
            toMaximized.remove(key);
        }
        toMaximized.putAll(toFix);

        Map<String, String> oldLikely = SupplementalDataInfo.getInstance().getLikelySubtags();
        Set<String> changes = compareMapsAndFixNew("*WARNING* Likely Subtags: ", oldLikely, toMaximized, "ms_Arab",
            "ms_Arab_ID");
        System.out.println(CollectionUtilities.join(changes, "\n"));

        if (OUTPUT_STYLE == OutputStyle.C_ALT) {
            doAlt(toMaximized);
        }

        if (SHOW_ADD)
            System.out
                .println("/*"
                    + CldrUtility.LINE_SEPARATOR
                    + " To Maximize:"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " If using raw strings, make sure the input language/locale uses the right separator, and has the right casing."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Remove the script Zzzz and the region ZZ if they occur; change an empty language subtag to 'und'."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Get the language, region, and script from the cleaned-up tag, plus any variants/extensions"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Try each of the following in order (where the field exists)"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + "   Lookup language-script-region. If in the table, return the result + variants"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + "   Lookup language-script. If in the table, return the result (substituting the original region if it exists) + variants"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + "   Lookup language-region. If in the table, return the result (substituting the original script if it exists) + variants"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + "   Lookup language. If in the table, return the result (substituting the original region and script if either or both exist) + variants"
                    +
                    CldrUtility.LINE_SEPARATOR
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Example: Input is zh-ZZZZ-SG."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Normalize to zh-SG. Lookup in table. No match."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Remove SG, but remember it. Lookup zh, and get the match (zh-Hans-CN). Substitute SG, and return zh-Hans-SG."
                    +
                    CldrUtility.LINE_SEPARATOR
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " To Minimize:"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " First get max = maximize(input)."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Then for trial in {language, language-region, language-script}"
                    +
                    CldrUtility.LINE_SEPARATOR
                    + "     If maximize(trial) == max, then return trial."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " If you don't get a match, return max."
                    +
                    CldrUtility.LINE_SEPARATOR
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " Example: Input is zh-Hant. Maximize to get zh-Hant-TW."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " zh => zh-Hans-CN. No match, so continue."
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " zh-TW => zh-Hans-TW. Match, so return zh-TW."
                    +
                    CldrUtility.LINE_SEPARATOR
                    +
                    CldrUtility.LINE_SEPARATOR
                    + " (A variant of this uses {language, language-script, language-region}): that is, tries script before language."
                    +
                    CldrUtility.LINE_SEPARATOR + " toMaximal size:\t" + toMaximized.size() +
                    CldrUtility.LINE_SEPARATOR + "*/");

        printLikelySubtags(toMaximized);

        // if (OUTPUT_STYLE != OutputStyle.XML) {
        // printMap("const MapToMinimalSubtags default_subtags[]", toMinimized, null);
        // }

        printDefaultContent(toMaximized);

        System.out.println(CldrUtility.LINE_SEPARATOR + "ERRORS:\t" + errorCount + CldrUtility.LINE_SEPARATOR);

    }

    static class RowData implements Comparable<RowData> {
        OfficialStatus os;
        String name;
        Long pop;

        public RowData(OfficialStatus os, String name, Long pop) {
            this.os = os;
            this.name = name;
            this.pop = pop;
        }

        public OfficialStatus getStatus() {
            // TODO Auto-generated method stub
            return os;
        }

        public CharSequence getName() {
            // TODO Auto-generated method stub
            return name;
        }

        public Long getLiteratePopulation() {
            // TODO Auto-generated method stub
            return pop;
        }

        public int compareTo(RowData o) {
            // TODO Auto-generated method stub
            int result = os.compareTo(o.os);
            if (result != 0) return -result;
            long result2 = pop - o.pop;
            if (result2 != 0) return result2 < 0 ? 1 : -1;
            return name.compareTo(o.name);
        }

        public boolean equals(Object o) {
            return 0 == compareTo((RowData) o);
        }

        public int hashCode() {
            throw new UnsupportedOperationException();
        }
    }

    private static void printDefaultLanguagesAndScripts() {

        final int minTotalPopulation = 10000000;
        final int minTerritoryPopulation = 1000000;
        final double minTerritoryPercent = 1.0 / 3;
        Map<String, Set<RowData>> languageToReason = new TreeMap<String, Set<RowData>>();
        Counter<String> languageToLiteratePopulation = new Counter<String>();
        NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
        nf.setGroupingUsed(true);
        LanguageTagParser ltp = new LanguageTagParser();
        LikelySubtags likelySubtags = new LikelySubtags();
        /*
         * A. X is a qualified language**, and at least one of the following is true:
         *
         * 1. X is has official status* in any country
         * 2. X exceeds a threshold population† of literate users worldwide: 1M
         * 3. X exceeds a threshold population† in some country Z: 100K and 20% of Z's population†.
         *
         * B. X is an exception explicitly approved by the committee or X has minimal
         * language coverage‡ in CLDR itself.
         */
        OfficialStatus minimalStatus = OfficialStatus.official_regional; // OfficialStatus.de_facto_official;
        Map<String, String> languages = new TreeMap<String, String>();
        for (String language : standardCodes.getAvailableCodes("language")) {
            String path = CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, language);
            String result = english.getStringValue(path);
            if (result != null) {
                languages.put(language, result);
            }
        }
        for (String language : languages.keySet()) {
            System.out.println(language + "\t" + languages.get(language));
        }

        for (String territory : supplementalData.getTerritoriesWithPopulationData()) {
            PopulationData territoryPop = supplementalData.getPopulationDataForTerritory(territory);
            double territoryPopulation = territoryPop.getLiteratePopulation();
            for (String languageScript : supplementalData.getLanguagesForTerritoryWithPopulationData(territory)) {
                PopulationData popData = supplementalData.getLanguageAndTerritoryPopulationData(languageScript,
                    territory);
                ltp.set(languageScript);
                String language = ltp.getLanguage();
//                if (ltp.getScript().isEmpty()) {
//                    String max = likelySubtags.maximize(languageScript);
//                    if (max != null) {
//                        ltp.set(max).setRegion("");
//                        languageScript = ltp.toString();
//                    }
//                }
                boolean add = false;
                // #1
                OfficialStatus status = popData.getOfficialStatus();
                if (status.compareTo(minimalStatus) >= 0) {
                    add = true;
                }
                long literatePopulation = getWritingPopulation(popData);
                // #2
                languageToLiteratePopulation.add(language, literatePopulation);
                // #3
                if (literatePopulation > minTerritoryPopulation
                    && literatePopulation > minTerritoryPercent * territoryPopulation) {
                    add = true;
                }
                if (add) {
                    add(languageToReason, language, territory, status, literatePopulation);
                    // Add the containing regions
                    for (String container : Containment.leafToContainer(territory)) {
                        add(languageToReason, language, container, OfficialStatus.unknown, literatePopulation);
                    }
                }
            }
        }
        // #2, now that we have the data
        for (String language : languageToLiteratePopulation.keySet()) {
            long totalPop = languageToLiteratePopulation.getCount(language);
            if (totalPop > minTotalPopulation) {
                add(languageToReason, language, "001", OfficialStatus.unknown, totalPop);
            }
        }

        // Specials
        add(languageToReason, "und", "001", OfficialStatus.unknown, 0);

        // for (String language : Iso639Data.getAvailable()) {
        // Scope scope = Iso639Data.getScope(language);
        // Type type = Iso639Data.getType(language);
        // if (scope == Scope.Special) {
        // add(languageToReason, language, "001", OfficialStatus.unknown, -1);
        // }
        // }
        // print them

        System.out.println("Detailed - Including:\t" + languageToReason.size());

        for (String language : languageToReason.keySet()) {
            Set<RowData> reasons = languageToReason.get(language);

            RowData lastReason = reasons.iterator().next();

            System.out.append(language)
                .append("\t")
                .append(english.getName(language))
                .append("\t")
                .append(lastReason.getStatus().toShortString())
                .append("\t")
                .append(nf.format(languageToLiteratePopulation.getCount(language)));
            for (RowData reason : reasons) {
                String status = reason.getStatus().toShortString();
                System.out.append("\t")
                    .append(status)
                    .append("-")
                    .append(reason.getName())
                    .append("-")
                    .append(nf.format(reason.getLiteratePopulation()));
            }
            System.out.append("\n");
        }

        // now list them

        Set<String> others = new TreeSet<String>();
        others.addAll(standardCodes.getGoodAvailableCodes("language"));
        others.removeAll(languageToReason.keySet());
        System.out.println("\nIncluded Languages:\t" + languageToReason.keySet().size());
        showLanguages(languageToReason.keySet(), languageToReason);
        System.out.println("\nExcluded Languages:\t" + others.size());
        showLanguages(others, languageToReason);
    }

    private static long getWritingPopulation(PopulationData popData) {
        final double writingPopulation = popData.getWritingPopulation();
        if (!Double.isNaN(writingPopulation)) {
            return (long) writingPopulation;
        }
        return (long) popData.getLiteratePopulation();
    }

    private static void showLanguages(Set<String> others, Map<String, Set<RowData>> languageToReason) {
        Set<String> sorted = new TreeSet<String>(Collator.getInstance(ULocale.ENGLISH));
        for (String language : others) {
            sorted.add(getLanguageName(language, languageToReason));
        }
        char last = 0;
        for (String language : sorted) {
            final char curr = language.charAt(0);
            if (last != curr) {
                System.out.println();
            } else if (last != '\u0000') {
                System.out.print(", ");
            }
            System.out.print(language);
            last = curr;
        }
        System.out.println();
    }

    private static String getLanguageName(String language,
        Map<String, Set<RowData>> languageToReason) {
        OfficialStatus best = OfficialStatus.unknown;
        Set<RowData> reasons = languageToReason.get(language);
        if (reasons != null) {
            for (RowData reason : reasons) {
                final OfficialStatus currentStatus = reason.getStatus();
                if (best.compareTo(currentStatus) < 0) {
                    best = currentStatus;
                }
            }
        }
        String status = best.toShortString();
        Scope scope = Iso639Data.getScope(language);
        if (scope == Scope.Special) {
            status = "S";
        }
        String languageFormatted = english.getName(language) + " [" + language + "]-" + status;
        return languageFormatted;
    }

    private static void add(Map<String, Set<RowData>> languageToReason, String language,
        String territoryRaw, OfficialStatus status, long population) {
        String territory = english.getName("territory", territoryRaw) + " [" + territoryRaw + "]";
        Set<RowData> set = languageToReason.get(language);
        if (set == null) {
            languageToReason.put(language, set = new TreeSet<RowData>());
        }
        set.add(new RowData(status, territory, population));
    }

    private static void printDefaultContent(Map<String, String> toMaximized) throws IOException {

        Set<String> defaultLocaleContent = new TreeSet<String>();

        // go through all the cldr locales, and add default contents
        // now computed from toMaximized
        Set<String> available = factory.getAvailable();
        Relation<String, String> toChildren = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        LanguageTagParser ltp = new LanguageTagParser();

        // System.out.println(maximize("az_Latn_AZ", toMaximized));
        Set<String> hasScript = new TreeSet<String>();

        // first get a mapping to children
        for (String locale : available) {
            if (locale.equals("root")) {
                continue;
            }
            if (ltp.set(locale).getVariants().size() != 0) {
                continue;
            }
            String parent = LocaleIDParser.getSimpleParent(locale);
            if (ltp.getScript().length() != 0) {
                hasScript.add(parent);
            }
            if (parent.equals("root")) {
                continue;
            }
            toChildren.put(parent, locale);
        }

        // Suppress script for locales for which we only have one locale in common/main. See ticket #7834.
        Set<String> suppressScriptLocales = new HashSet<String>(Arrays.asList(
            "bm_ML", "en_US", "ha_NG", "iu_CA", "ms_MY", "mn_MN",
            "byn_ER", "ff_SN", "dyo_SN", "kk_KZ", "ku_TR", "ky_KG", "ml_IN", "so_SO", "sw_TZ", "wo_SN", "yo_NG", "dje_NE",
            "blt_VN"));

        // if any have a script, then throw out any that don't have a script (unless they're specifically included.)
        Set<String> toRemove = new TreeSet<String>();
        for (String locale : hasScript) {
            toRemove.clear();
            Set<String> children = toChildren.getAll(locale);
            for (String child : children) {
                if (ltp.set(child).getScript().length() == 0 && !suppressScriptLocales.contains(child)) {
                    toRemove.add(child);
                }
            }
            if (toRemove.size() != 0) {
                System.out.println("Removing:\t" + locale + "\t" + toRemove + "\tfrom\t" + children);
                toChildren.removeAll(locale, toRemove);
            }
        }

        // we add a child as a default locale if it has the same maximization
        main: for (String locale : toChildren.keySet()) {
            String maximized = maximize(locale, toMaximized);
            if (maximized == null) {
                if (SHOW_ADD) System.out.println("Missing maximized:\t" + locale);
                continue;
            }
            Set<String> children = toChildren.getAll(locale);
            Map<String, String> debugStuff = new TreeMap<String, String>();
            for (String child : children) {
                String maximizedChild = maximize(child, toMaximized);
                if (maximized.equals(maximizedChild)) {
                    defaultLocaleContent.add(child);
                    continue main;
                }
                debugStuff.put(child, maximizedChild);
            }
            if (SHOW_ADD) System.out.println("Can't find maximized: " + locale + "=" + maximized
                + "\tin\t" + debugStuff);
        }

        defaultLocaleContent.remove("und_ZZ"); // und_ZZ isn't ever a real locale.

        showDefaultContentDifferencesAndFix(defaultLocaleContent);

        Log.setLogNoBOM(CLDRPaths.GEN_DIRECTORY + "/supplemental", "supplementalMetadata.xml");
        BufferedReader oldFile = FileUtilities.openUTF8Reader(CLDRPaths.SUPPLEMENTAL_DIRECTORY, "supplementalMetadata.xml");
        CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s*<defaultContent locales=\"\\s*"), Log.getLog(), false);

        String sep = CldrUtility.LINE_SEPARATOR + "\t\t\t";
        String broken = CldrUtility.breakLines(CldrUtility.join(defaultLocaleContent, " "), sep,
            PatternCache.get("(\\S)\\S*").matcher(""), 80);

        Log.println("\t\t<defaultContent locales=\"" + broken + "\"");
        Log.println("\t\t/>");

        // Log.println("</supplementalData>");
        CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s*/>\\s*(<!--.*)?"), null, true); // skip to matching >
        CldrUtility.copyUpTo(oldFile, null, Log.getLog(), true); // copy the rest

        Log.close();
        oldFile.close();
    }

    // private static void oldAlgorithm(Map<String,String> toMaximized) {
    // Set<String> defaultContentLocales = supplementalData.getDefaultContentLocales();
    // LanguageTagParser parser = new LanguageTagParser();
    // for (String locale : defaultContentLocales) {
    // String parent = parser.getParent(locale);
    // toMaximized.put(parent, locale);
    // if (SHOW_ADD) System.out.println("Adding:\t" + parent + "\t=>\t" + locale + "\t\tDefaultContent");
    // }
    //
    // for (String[] specialCase : SpecialCases) {
    // toMaximized.put(specialCase[0], specialCase[1]);
    // if (SHOW_ADD) System.out.println("Adding:\t" + specialCase[0] + "\t=>\t" + specialCase[1] + "\t\tSpecial");
    // }
    //
    // // recurse and close
    // closeMapping(toMaximized);
    //
    // addScript(toMaximized, parser);
    //
    // closeMapping(toMaximized);
    //
    // addLanguageScript(toMaximized, parser);
    //
    // closeMapping(toMaximized);
    //
    // addLanguageCountry(toMaximized, parser);
    //
    // closeMapping(toMaximized);
    //
    // addCountries(toMaximized);
    // addScript(toMaximized, parser);
    // closeMapping(toMaximized);
    // closeUnd(toMaximized);
    //
    // addDeprecated(toMaximized);
    //
    // closeMapping(toMaximized);
    //
    // checkConsistency(toMaximized);
    // }

    private static class MaxData {
        Relation<String, Row.R3<Double, String, String>> languages = Relation.of(new TreeMap<String, Set<Row.R3<Double, String, String>>>(), TreeSet.class);
        Map<String, Counter<String>> languagesToScripts = new TreeMap<String, Counter<String>>();
        Map<String, Counter<String>> languagesToRegions = new TreeMap<String, Counter<String>>();

        Relation<String, Row.R3<Double, String, String>> scripts = Relation.of(new TreeMap<String, Set<Row.R3<Double, String, String>>>(), TreeSet.class);
        Map<String, Counter<String>> scriptsToLanguages = new TreeMap<String, Counter<String>>();
        Map<String, Counter<String>> scriptsToRegions = new TreeMap<String, Counter<String>>();

        Relation<String, Row.R3<Double, String, String>> regions = Relation.of(new TreeMap<String, Set<Row.R3<Double, String, String>>>(), TreeSet.class);
        Map<String, Counter<String>> regionsToLanguages = new TreeMap<String, Counter<String>>();
        Map<String, Counter<String>> regionsToScripts = new TreeMap<String, Counter<String>>();

        Map<String, Counter<Row.R2<String, String>>> containersToLanguage = new TreeMap<String, Counter<Row.R2<String, String>>>();
        Relation<String, Row.R4<Double, String, String, String>> containersToLangRegion = Relation.of(
            new TreeMap<String, Set<Row.R4<Double, String, String, String>>>(), TreeSet.class);

        Relation<Row.R2<String, String>, Row.R2<Double, String>> languageScripts = Relation.of(
            new TreeMap<Row.R2<String, String>, Set<Row.R2<Double, String>>>(),
            TreeSet.class);
        Relation<Row.R2<String, String>, Row.R2<Double, String>> scriptRegions = Relation.of(
            new TreeMap<Row.R2<String, String>, Set<Row.R2<Double, String>>>(),
            TreeSet.class);
        Relation<Row.R2<String, String>, Row.R2<Double, String>> languageRegions = Relation.of(
            new TreeMap<Row.R2<String, String>, Set<Row.R2<Double, String>>>(),
            TreeSet.class);

        /**
         * Add population information. "order" is the negative of the population (makes the first be the highest).
         * @param language
         * @param script
         * @param region
         * @param order
         */
        void add(String language, String script, String region, Double order) {
            if (language.equals("cpp")) {
                System.out.println(language + "\t" + script + "\t" + region + "\t" + -order);
            }
            languages.put(language, Row.of(order, script, region));
            // addCounter(languagesToScripts, language, script, order);
            // addCounter(languagesToRegions, language, region, order);

            scripts.put(script, Row.of(order, language, region));
            // addCounter(scriptsToLanguages, script, language, order);
            // addCounter(scriptsToRegions, script, region, order);

            regions.put(region, Row.of(order, language, script));
            // addCounter(regionsToLanguages, region, language, order);
            // addCounter(regionsToScripts, region, script, order);

            languageScripts.put(Row.of(language, script), Row.of(order, region));
            scriptRegions.put(Row.of(script, region), Row.of(order, language));
            languageRegions.put(Row.of(language, region), Row.of(order, script));

            Set<String> containerSet = Containment.leafToContainer(region);
            if (containerSet != null) {
                for (String container : containerSet) {

                    containersToLangRegion.put(container, Row.of(order, language, script, region));
                    Counter<R2<String, String>> data = containersToLanguage.get(container);
                    if (data == null) {
                        containersToLanguage.put(container, data = new Counter<R2<String, String>>());
                    }
                    data.add(Row.of(language, script), (long) (double) order);

                }
            }

            if (SHOW_ADD) System.out.println("Data:\t" + language + "\t" + script + "\t" + region + "\t" + order);
        }
        // private void addCounter(Map<String, Counter<String>> map, String key, String key2, Double count) {
        // Counter<String> counter = map.get(key);
        // if (counter == null) {
        // map.put(key, counter = new Counter<String>());
        // }
        // counter.add(key2, count.longValue());
        // }
    }

    private static final double MIN_UNOFFICIAL_LANGUAGE_SIZE = 10000000;
    private static final double MIN_UNOFFICIAL_LANGUAGE_PROPORTION = 0.20;
    private static final double MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE = 100000;
    private static final double UNOFFICIAL_SCALE_DOWN = 0.2;

    private static NumberFormat percent = NumberFormat.getPercentInstance();
    private static NumberFormat number = NumberFormat.getIntegerInstance();

    private static void tryDifferentAlgorithm(Map<String, String> toMaximized) {
        // we are going to try a different approach.
        // first gather counts for maximized values
        // Set<Row.R3<String,String,String>,Double> rowsToCounts = new TreeMap();
        MaxData maxData = new MaxData();
        Set<String> cldrLocales = factory.getAvailable();
        Set<String> otherTerritories = new TreeSet<String>(standardCodes.getGoodAvailableCodes("territory"));

        // process all the information to get the top values for each triple.
        // each of the combinations of 1 or 2 components gets to be a key.
        for (String region : supplementalData.getTerritoriesWithPopulationData()) {
            otherTerritories.remove(region);
            PopulationData regionData = supplementalData.getPopulationDataForTerritory(region);
            final double literateTerritoryPopulation = regionData.getLiteratePopulation();
            // we need any unofficial language to meet a certain absolute size requirement and proportion size
            // requirement.
            // so the bar is x percent of the population, reset up to y absolute size.
            double minimalLiteratePopulation = literateTerritoryPopulation * MIN_UNOFFICIAL_LANGUAGE_PROPORTION;
            if (minimalLiteratePopulation < MIN_UNOFFICIAL_LANGUAGE_SIZE) {
                minimalLiteratePopulation = MIN_UNOFFICIAL_LANGUAGE_SIZE;
            }

            for (String writtenLanguage : supplementalData.getLanguagesForTerritoryWithPopulationData(region)) {
                PopulationData data = supplementalData.getLanguageAndTerritoryPopulationData(writtenLanguage, region);
                final double literatePopulation = getWritingPopulation(data); //data.getLiteratePopulation();
                double order = -literatePopulation; // negative so we get the inverse order

                if (data.getOfficialStatus() == OfficialStatus.unknown) {
                    final String locale = writtenLanguage + "_" + region;
                    if (literatePopulation >= minimalLiteratePopulation) {
                        // ok, skip
                    } else if (literatePopulation >= MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE && cldrLocales.contains(locale)) {
                        // ok, skip
                    } else {
                        // if (SHOW_ADD)
                        // System.out.println("Skipping:\t" + writtenLanguage + "\t" + region + "\t"
                        // + english.getName(locale)
                        // + "\t-- too small:\t" + number.format(literatePopulation));
                        // continue;
                    }
                    order *= UNOFFICIAL_SCALE_DOWN;
                    if (SHOW_ADD)
                        System.out.println("Retaining\t" + writtenLanguage + "\t" + region + "\t"
                            + english.getName(locale)
                            + "\t" + number.format(literatePopulation)
                            + "\t" + percent.format(literatePopulation / literateTerritoryPopulation)
                            + (cldrLocales.contains(locale) ? "\tin-CLDR" : ""));
                }
                String script;
                String language = writtenLanguage;
                final int pos = writtenLanguage.indexOf('_');
                if (pos > 0) {
                    language = writtenLanguage.substring(0, pos);
                    script = writtenLanguage.substring(pos + 1);
                } else {
                    script = getScriptForLocale2(language);
                }
                maxData.add(language, script, region, order);
            }
        }

        LanguageTagParser additionLtp = new LanguageTagParser();

        for (String addition : MAX_ADDITIONS) {
            additionLtp.set(addition);
            String lan = additionLtp.getLanguage();
            Set<R3<Double, String, String>> key = maxData.languages.get(lan);
            if (key == null) {
                maxData.add(lan, additionLtp.getScript(), additionLtp.getRegion(), 1.0);
            } else {
                int debug = 0;
            }
        }

        for (Entry<String, Collection<String>> entry : DeriveScripts.getLanguageToScript().asMap().entrySet()) {
            String language = entry.getKey();
            final Collection<String> values = entry.getValue();
            if (values.size() != 1) {
                continue; // skip, no either way 
            }
            Set<R3<Double, String, String>> old = maxData.languages.get(language);
            if (!maxData.languages.containsKey(language)) {
                maxData.add(language, values.iterator().next(), TEMP_UNKNOWN_REGION, 1.0);
            }
        }

        // add others, with English default
        for (String region : otherTerritories) {
            if (region.length() == 3) continue; // FIX ONCE WE ADD REGIONS
            maxData.add("en", "Latn", region, 1.0);
        }

        // get a reverse mapping, so that we can add the aliases

        Map<String, R2<List<String>, String>> languageAliases = SupplementalDataInfo.getInstance().getLocaleAliasInfo()
            .get("language");
        for (Entry<String, R2<List<String>, String>> str : languageAliases.entrySet()) {
            String reason = str.getValue().get1();
            if ("overlong".equals(reason) || "bibliographic".equals(reason) || "macrolanguage".equals(reason)) {
                continue;
            }
            List<String> replacements = str.getValue().get0();
            if (replacements == null) {
                continue;
            }
            String goodLanguage = replacements.get(0);

            String badLanguage = str.getKey();
            if (badLanguage.contains("_")) {
                continue;
            }
            if (deprecatedISONotInLST.contains(badLanguage)) {
                continue;
            }
            Set<R3<Double, String, String>> goodLanguageData = maxData.languages.getAll(goodLanguage);
            if (goodLanguageData == null) {
                continue;
            }
            R3<Double, String, String> value = goodLanguageData.iterator().next();
            final String script = value.get1();
            final String region = value.get2();
            maxData.add(badLanguage, script, region, 1.0);
            System.out.println("Adding aliases: " + badLanguage + ", " + script + ", " + region + ", " + reason);
        }

        // now, get the best for each one
        for (String language : maxData.languages.keySet()) {
            R3<Double, String, String> value = maxData.languages.getAll(language).iterator().next();
            final Comparable<String> script = value.get1();
            final Comparable<String> region = value.get2();
            add(language, language + "_" + script + "_" + region, toMaximized, "L->SR", Override.REPLACE_EXISTING,
                SHOW_ADD);
        }
        for (String language : maxData.languagesToScripts.keySet()) {
            String script = maxData.languagesToScripts.get(language).getKeysetSortedByCount(true).iterator().next();
            add(language, language + "_" + script, toMaximized, "L->S", Override.REPLACE_EXISTING, SHOW_ADD);
        }
        for (String language : maxData.languagesToRegions.keySet()) {
            String region = maxData.languagesToRegions.get(language).getKeysetSortedByCount(true).iterator().next();
            add(language, language + "_" + region, toMaximized, "L->R", Override.REPLACE_EXISTING, SHOW_ADD);
        }

        for (String script : maxData.scripts.keySet()) {
            R3<Double, String, String> value = maxData.scripts.getAll(script).iterator().next();
            final Comparable<String> language = value.get1();
            final Comparable<String> region = value.get2();
            add("und_" + script, language + "_" + script + "_" + region, toMaximized, "S->LR",
                Override.REPLACE_EXISTING, SHOW_ADD);
        }
        for (String script : maxData.scriptsToLanguages.keySet()) {
            String language = maxData.scriptsToLanguages.get(script).getKeysetSortedByCount(true).iterator().next();
            add("und_" + script, language + "_" + script, toMaximized, "S->L", Override.REPLACE_EXISTING, SHOW_ADD);
        }
        for (String script : maxData.scriptsToRegions.keySet()) {
            String region = maxData.scriptsToRegions.get(script).getKeysetSortedByCount(true).iterator().next();
            add("und_" + script, "und_" + script + "_" + region, toMaximized, "S->R", Override.REPLACE_EXISTING,
                SHOW_ADD);
        }

        for (String region : maxData.regions.keySet()) {
            R3<Double, String, String> value = maxData.regions.getAll(region).iterator().next();
            final Comparable<String> language = value.get1();
            final Comparable<String> script = value.get2();
            add("und_" + region, language + "_" + script + "_" + region, toMaximized, "R->LS",
                Override.REPLACE_EXISTING, SHOW_ADD);
        }
        for (String region : maxData.regionsToLanguages.keySet()) {
            String language = maxData.regionsToLanguages.get(region).getKeysetSortedByCount(true).iterator().next();
            add("und_" + region, language + "_" + region, toMaximized, "R->L", Override.REPLACE_EXISTING, SHOW_ADD);
        }
        for (String region : maxData.regionsToScripts.keySet()) {
            String script = maxData.regionsToScripts.get(region).getKeysetSortedByCount(true).iterator().next();
            add("und_" + region, "und_" + script + "_" + region, toMaximized, "R->S", Override.REPLACE_EXISTING,
                SHOW_ADD);
        }

        for (Entry<String, Counter<R2<String, String>>> containerAndInfo : maxData.containersToLanguage.entrySet()) {
            String region = containerAndInfo.getKey();
            if (region.equals("001")) {
                continue;
            }
            Counter<R2<String, String>> data = containerAndInfo.getValue();
            Set<R2<String, String>> keysetSortedByCount = data.getKeysetSortedByCount(true);
            if (SHOW_CONTAINERS) { // debug
                System.out.println("Container2L:\t" + region + "\t" + shorten(data.getEntrySetSortedByCount(true, null)));
                System.out.println("Container2LR:\t" + region + "\t" + maxData.containersToLangRegion.get(region));
            }
            R2<String, String> value = keysetSortedByCount.iterator().next(); // will get most negative
            final Comparable<String> language = value.get0();
            final Comparable<String> script = value.get1();

            // fix special cases like es-419, where a locale exists.
            // for those cases, what we add as output is the container. Otherwise the region.
            Set<String> skipLanguages = cldrContainerToLanguages.get(region);
            if (skipLanguages != null
                && skipLanguages.contains(language)) {
                add("und_" + region, language + "_" + script + "_" + region, toMaximized, "R*->LS",
                    Override.REPLACE_EXISTING, SHOW_ADD);
                continue;
            }

            // we now have the best language and script. Find the best region for that
            for (R4<Double, String, String, String> e : maxData.containersToLangRegion.get(region)) {
                final Comparable<String> language2 = e.get1();
                final Comparable<String> script2 = e.get2();
                if (language2.equals(language) && script2.equals(script)) {
                    add("und_" + region, language + "_" + script + "_" + e.get3(), toMaximized, "R*->LS",
                        Override.REPLACE_EXISTING, SHOW_ADD);
                    break;
                }
            }
        }

        for (R2<String, String> languageScript : maxData.languageScripts.keySet()) {
            R2<Double, String> value = maxData.languageScripts.getAll(languageScript).iterator().next();
            final Comparable<String> language = languageScript.get0();
            final Comparable<String> script = languageScript.get1();
            final Comparable<String> region = value.get1();
            add(language + "_" + script, language + "_" + script + "_" + region, toMaximized, "LS->R",
                Override.REPLACE_EXISTING, SHOW_ADD);
        }

        for (R2<String, String> scriptRegion : maxData.scriptRegions.keySet()) {
            R2<Double, String> value = maxData.scriptRegions.getAll(scriptRegion).iterator().next();
            final Comparable<String> script = scriptRegion.get0();
            final Comparable<String> region = scriptRegion.get1();
            final Comparable<String> language = value.get1();
            add("und_" + script + "_" + region, language + "_" + script + "_" + region, toMaximized, "SR->L",
                Override.REPLACE_EXISTING, SHOW_ADD);
        }

        for (R2<String, String> languageRegion : maxData.languageRegions.keySet()) {
            R2<Double, String> value = maxData.languageRegions.getAll(languageRegion).iterator().next();
            final Comparable<String> language = languageRegion.get0();
            final Comparable<String> region = languageRegion.get1();
            final Comparable<String> script = value.get1();
            add(language + "_" + region, language + "_" + script + "_" + region, toMaximized, "LR->S",
                Override.REPLACE_EXISTING, SHOW_ADD);
        }

        // get the script info from metadata as fallback

        TreeSet<String> sorted = new TreeSet<String>(ScriptMetadata.getScripts());
        for (String script : sorted) {
            Info i = ScriptMetadata.getInfo(script);
            String likelyLanguage = i.likelyLanguage;
            String originCountry = i.originCountry;
            final String result = likelyLanguage + "_" + script + "_" + originCountry;
            add("und_" + script, result, toMaximized, "S->LR•",
                Override.KEEP_EXISTING, SHOW_ADD);
            add(likelyLanguage, result, toMaximized, "L->SR•",
                Override.KEEP_EXISTING, SHOW_ADD);
        }

        // add overrides
        for (String key : LANGUAGE_OVERRIDES.keySet()) {
            add(key, LANGUAGE_OVERRIDES.get(key), toMaximized, "OVERRIDE", Override.REPLACE_EXISTING, true);
        }
    }

    public static String shorten(Object data) {
        String info = data.toString();
        if (info.length() > 255) {
            info = info.substring(0, 127) + "…";
        }
        return info;
    }

    private static void doAlt(Map<String, String> toMaximized) {
        // TODO Auto-generated method stub
        Map<String, String> temp = new TreeMap<String, String>();
        for (String locale : toMaximized.keySet()) {
            String target = toMaximized.get(locale);
            temp.put(toAlt(locale, true), toAlt(target, true));
        }
        toMaximized.clear();
        toMaximized.putAll(temp);
    }

    public static String maximize(String languageTag, Map<String, String> toMaximized) {
        LanguageTagParser ltp = new LanguageTagParser();

        // clean up the input by removing Zzzz, ZZ, and changing "" into und.
        ltp.set(languageTag);
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();
        boolean changed = false;
        if (language.equals("")) {
            ltp.setLanguage(language = "und");
            changed = true;
        }
        if (region.equals(UNKNOWN_SCRIPT)) {
            ltp.setScript(script = "");
            changed = true;
        }
        if (ltp.getRegion().equals(UNKNOWN_REGION)) {
            ltp.setRegion(region = "");
            changed = true;
        }
        if (changed) {
            languageTag = ltp.toString();
        }
        // check whole
        String result = toMaximized.get(languageTag);
        if (result != null) {
            return result;
        }
        // try empty region
        if (region.length() != 0) {
            result = toMaximized.get(ltp.setRegion("").toString());
            if (result != null) {
                return ltp.set(result).setRegion(region).toString();
            }
            ltp.setRegion(region); // restore
        }
        // try empty script
        if (script.length() != 0) {
            result = toMaximized.get(ltp.setScript("").toString());
            if (result != null) {
                return ltp.set(result).setScript(script).toString();
            }
            // try empty script and region
            if (region.length() != 0) {
                result = toMaximized.get(ltp.setRegion("").toString());
                if (result != null) {
                    return ltp.set(result).setScript(script).setRegion(region).toString();
                }
            }
        }
        if (!language.equals("und") && script.length() != 0 && region.length() != 0) {
            return languageTag; // it was ok, and we couldn't do anything with it
        }
        return null; // couldn't maximize
    }

    public static String minimize(String input, Map<String, String> toMaximized, boolean favorRegion) {
        if (input.equals("nb_Latn_SJ")) {
            System.out.print(""); // debug
        }
        String maximized = maximize(input, toMaximized);
        if (maximized == null) {
            return null; // failed
        }
        LanguageTagParser ltp = new LanguageTagParser().set(maximized);
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();
        // try building up from shorter to longer, and find the first that matches
        // could be more optimized, but for this code we want simplest
        String[] trials = { language,
            language + TAG_SEPARATOR + (favorRegion ? region : script),
            language + TAG_SEPARATOR + (!favorRegion ? region : script) };
        for (String trial : trials) {
            String newMaximized = maximize(trial, toMaximized);
            if (maximized.equals(newMaximized)) {
                return trial;
            }
        }
        return maximized;
    }

    // /**
    // * Verify that we can map from each language, script, and country to something.
    // * @param toMaximized
    // */
    // private static void checkConsistency(Map<String, String> toMaximized) {
    // Map<String,String> needMappings = new TreeMap();
    // LanguageTagParser parser = new LanguageTagParser();
    // for (String maximized : new TreeSet<String>(toMaximized.values())) {
    // parser.set(maximized);
    // final String language = parser.getLanguage();
    // final String script = parser.getScript();
    // final String region = parser.getRegion();
    // if (language.length() == 0 || script.length() == 0 || region.length() == 0) {
    // failure("   { \"" + maximized + "\", \"" + maximized + "\" },   //     " + english.getName(maximized) +
    // "\t\tFailed-Consistency");
    // continue;
    // }
    // addIfNotIn(language, maximized, needMappings, toMaximized, "Consistency");
    // addIfNotIn(language + "_" + script, maximized, needMappings, toMaximized, "Consistency");
    // addIfNotIn(language + "_" + region, maximized, needMappings, toMaximized, "Consistency");
    // addIfNotIn("und_" + script, maximized, needMappings, toMaximized, "Consistency");
    // addIfNotIn("und_" + script + "_" + region, maximized, needMappings, toMaximized, "Consistency");
    // addIfNotIn("und_" + region, maximized, needMappings, toMaximized, "Consistency");
    // }
    // toMaximized.putAll(needMappings);
    // }

    // private static void failure(String string) {
    // System.out.println(string);
    // errorCount++;
    // }

    // private static void addIfNotIn(String key, String value, Map<String, String> toAdd, Map<String, String>
    // otherToCheck, String kind) {
    // addIfNotIn(key, value, toAdd, otherToCheck == null ? null : otherToCheck.keySet(), null, kind);
    // }

    // private static void addIfNotIn(String key, String value, Map<String, String> toAdd, Set<String> skipKey,
    // Set<String> skipValue, String kind) {
    // if (!key.equals(value)
    // && !toAdd.containsKey(key)
    // && (skipKey == null || !skipKey.contains(key))
    // && (skipValue == null || !skipValue.contains(value))) {
    // add(key, value, toAdd, kind);
    // }
    // }

    enum Override {
        KEEP_EXISTING, REPLACE_EXISTING
    }

    private static void add(String key, String value, Map<String, String> toAdd, String kind, Override override,
        boolean showAction) {
        if (key.equals(DEBUG_ADD_KEY)) {
            System.out.println("*debug*");
        }
        String oldValue = toAdd.get(key);
        if (oldValue == null) {
            if (showAction) {
                System.out.println("Adding:\t\t" + getName(key) + "\t=>\t" + getName(value) + "\t\t\t\t" + kind);
            }
        } else if (override == Override.KEEP_EXISTING || value.equals(oldValue)) {
            // if (showAction) {
            // System.out.println("Skipping:\t" + key + "\t=>\t" + value + "\t\t\t\t" + kind);
            // }
            return;
        } else {
            if (showAction) {
                System.out.println("Replacing:\t" + getName(key) + "\t=>\t" + getName(value) + "\t, was\t" + getName(oldValue) + "\t\t" + kind);
            }
        }
        toAdd.put(key, value);
    }

    private static String getName(String value) {
        return ConvertLanguageData.getLanguageCodeAndName(value);
    }

    // private static void addCountries(Map<String, String> toMaximized) {
    // Map <String, Map<String, Double>> scriptToLanguageToSize = new TreeMap();
    //
    // for (String territory : supplementalData.getTerritoriesWithPopulationData()) {
    // Set<String> languages = supplementalData.getLanguagesForTerritoryWithPopulationData(territory);
    // String biggestOfficial = null;
    // double biggest = -1;
    // for (String language : languages) {
    // PopulationData info = supplementalData.getLanguageAndTerritoryPopulationData(language, territory);
    // // add to info about script
    //
    // String script = getScriptForLocale(language);
    // if (script != null) {
    // Map<String, Double> languageInfo = scriptToLanguageToSize.get(script);
    // if (languageInfo == null) scriptToLanguageToSize.put(script, languageInfo = new TreeMap());
    // String baseLanguage = language;
    // int pos = baseLanguage.indexOf('_');
    // if (pos >= 0) {
    // baseLanguage = baseLanguage.substring(0,pos);
    // }
    // Double size = languageInfo.get(baseLanguage);
    // languageInfo.put(baseLanguage, (size == null ? 0 : size) + info.getLiteratePopulation());
    // }
    //
    //
    // final OfficialStatus officialStatus = info.getOfficialStatus();
    // if (officialStatus == OfficialStatus.de_facto_official || officialStatus == OfficialStatus.official) {
    // double size2 = info.getLiteratePopulation();
    // if (biggest < size2) {
    // biggest = size2;
    // biggestOfficial = language;
    // }
    // }
    // }
    // if (biggestOfficial != null) {
    // final String replacementTag = "und_" + territory;
    // String maximized = biggestOfficial + "_" + territory;
    // toMaximized.put(replacementTag, maximized);
    // if (SHOW_ADD) System.out.println("Adding:\t" + replacementTag + "\t=>\t" + maximized + "\t\tLanguage-Territory");
    // }
    // }
    //
    // for (String script : scriptToLanguageToSize.keySet()) {
    // String biggestOfficial = null;
    // double biggest = -1;
    //
    // final Map<String, Double> languageToSize = scriptToLanguageToSize.get(script);
    // for (String language : languageToSize.keySet()) {
    // double size = languageToSize.get(language);
    // if (biggest < size) {
    // biggest = size;
    // biggestOfficial = language;
    // }
    // }
    // if (biggestOfficial != null) {
    // final String replacementTag = "und_" + script;
    // String maximized = biggestOfficial + "_" + script;
    // toMaximized.put(replacementTag, maximized);
    // if (SHOW_ADD) System.out.println("Adding:\t" + replacementTag + "\t=>\t" + maximized + "\t\tUnd-Script");
    // }
    // }
    // }

    // private static void closeUnd(Map<String, String> toMaximized) {
    // Map<String,String> toAdd = new TreeMap<String,String>();
    // for (String oldSource : toMaximized.keySet()) {
    // String maximized = toMaximized.get(oldSource);
    // if (!maximized.startsWith("und")) {
    // int pos = maximized.indexOf("_");
    // if (pos >= 0) {
    // addIfNotIn( "und" + maximized.substring(pos), maximized, toAdd, toMaximized, "CloseUnd");
    // }
    // }
    // }
    // toMaximized.putAll(toAdd);
    // }

    /**
     * Generate tags where the deprecated values map to the expanded values
     *
     * @param toMaximized
     */
    // private static void addDeprecated(Map<String, String> toMaximized) {
    // Map<String, Map<String, List<String>>> typeToTagToReplacement = supplementalData.getLocaleAliasInfo();
    // LanguageTagParser temp = new LanguageTagParser();
    // LanguageTagParser tagParsed = new LanguageTagParser();
    // LanguageTagParser replacementParsed = new LanguageTagParser();
    // Map<String,String> toAdd = new TreeMap<String,String>();
    // while (true) {
    // toAdd.clear();
    // for (String type : typeToTagToReplacement.keySet()) {
    // if (type.equals("variant") || type.equals("zone")) continue;
    // boolean addUnd = !type.equals("language");
    //
    // Map<String, List<String>> tagToReplacement = typeToTagToReplacement.get(type);
    // System.out.println("*" + type + " = " + tagToReplacement);
    //
    // for (String tag: tagToReplacement.keySet()) {
    //
    // final List<String> list = tagToReplacement.get(tag);
    // if (list == null) continue; // we don't have any information
    // String replacement = list.get(0);
    //
    // // only do multiples
    // if (tag.contains("_") || !replacement.contains("_")) {
    // continue;
    // }
    //
    // // we now have a tag and a replacement value
    // // make parsers that we can use
    // try {
    // tagParsed.set(addUnd ? "und-" + tag : tag);
    // replacementParsed.set(addUnd ? "und-" + replacement : replacement);
    // } catch (RuntimeException e) {
    // continue;
    // }
    // addIfNotIn(tag, replacement, toAdd, toMaximized,"Deprecated");
    //
    // for (String locale : toMaximized.keySet()) {
    // String maximized = toMaximized.get(locale);
    // addIfMatches(temp.set(locale), maximized, replacementParsed, tagParsed, toAdd, toMaximized);
    // addIfMatches(temp.set(maximized), maximized, replacementParsed, tagParsed, toAdd, toMaximized);
    // }
    // }
    // }
    // if (toAdd.size() == 0) {
    // break;
    // }
    // toMaximized.putAll(toAdd);
    // }
    // }

    // private static void addIfMatches(LanguageTagParser locale, String maximized, LanguageTagParser tagParsed,
    // LanguageTagParser replacementParsed, Map<String, String> toAdd, Map<String, String> toMaximized) {
    // if (!tagParsed.getLanguage().equals(locale.getLanguage()) && !tagParsed.getLanguage().equals("und")) {
    // return;
    // }
    // if (!tagParsed.getScript().equals(locale.getScript()) && !tagParsed.getScript().equals("")) {
    // return;
    // }
    // if (!tagParsed.getRegion().equals(locale.getRegion()) && !tagParsed.getRegion().equals("")) {
    // return;
    // }
    // if (!replacementParsed.getLanguage().equals("und")) {
    // locale.setLanguage(replacementParsed.getLanguage());
    // }
    // if (!replacementParsed.getScript().equals("")) {
    // locale.setScript(replacementParsed.getScript());
    // }
    // if (!replacementParsed.getRegion().equals("")) {
    // locale.setRegion(replacementParsed.getRegion());
    // }
    // addIfNotIn(locale.toString(), maximized, toAdd, toMaximized,"Deprecated");
    // }

    // private static int getSubtagPosition(String locale, String subtags) {
    // int pos = -1;
    // while (true) {
    // pos = locale.indexOf(subtags, pos + 1);
    // if (pos < 0) return -1;
    // // make sure boundaries are ok
    // if (pos != 0) {
    // char charBefore = locale.charAt(pos-1);
    // if (charBefore != '_' && charBefore != '_') return -1;
    // }
    // int limit = pos + subtags.length();
    // if (limit != locale.length()) {
    // char charAfter = locale.charAt(limit);
    // if (charAfter != '_' && charAfter != '_') return -1;
    // }
    // return pos;
    // }
    // }

    /*
     * Format
     * const DefaultSubtags default_subtags[] = {
     * {
     * // Afar => Afar (Latin, Ethiopia)
     * "aa",
     * "aa_Latn_ET"
     * },{
     * // Afrikaans => Afrikaans (Latin, South Africa)
     * "af",
     * "af_Latn_ZA"
     * },{
     */

    private static void printLikelySubtags(Map<String, String> fluffup) throws IOException {

        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY,
            "/supplemental/likelySubtags" + (OUTPUT_STYLE == OutputStyle.XML ? ".xml" : ".txt"));
        String spacing = OUTPUT_STYLE == OutputStyle.PLAINTEXT ? "\t" : " ";
        String header = OUTPUT_STYLE != OutputStyle.XML ? "const MapToMaximalSubtags default_subtags[] = {"
            : "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + CldrUtility.LINE_SEPARATOR
                + "<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">"
                + CldrUtility.LINE_SEPARATOR
                + "<!--"
                + CldrUtility.LINE_SEPARATOR
                + CldrUtility.getCopyrightString()
                + CldrUtility.LINE_SEPARATOR
                + "-->"
                + CldrUtility.LINE_SEPARATOR
                + "<!--"
                + CldrUtility.LINE_SEPARATOR
                + "Likely subtags data is generated programatically from CLDR's language/territory/population" + CldrUtility.LINE_SEPARATOR
                + "data using the GenerateMaximalLocales tool. Under normal circumstances, this file should" + CldrUtility.LINE_SEPARATOR
                + "not be patched by hand, as any changes made in that fashion may be lost."
                + CldrUtility.LINE_SEPARATOR
                + "-->"
                + CldrUtility.LINE_SEPARATOR
                + "<supplementalData>" + CldrUtility.LINE_SEPARATOR
                + "    <version number=\"$" +
                "Revision$\"/>" + CldrUtility.LINE_SEPARATOR
                + "    <likelySubtags>";
        String footer = OUTPUT_STYLE != OutputStyle.XML ? SEPARATOR + "};"
            : "    </likelySubtags>" + CldrUtility.LINE_SEPARATOR
                + "</supplementalData>";
        out.println(header);
        boolean first = true;
        Set<String> keys = new TreeSet<String>(new LocaleStringComparator());
        keys.addAll(fluffup.keySet());
        for (String printingLocale : keys) {
            String printingTarget = fluffup.get(printingLocale);
            String comment = printingName(printingLocale, spacing) + spacing + "=>" + spacing
                + printingName(printingTarget, spacing);

            if (OUTPUT_STYLE == OutputStyle.XML) {
                out.println("\t\t<likelySubtag from=\"" + printingLocale +
                    "\" to=\"" + printingTarget + "\"" +
                    "/>" + CldrUtility.LINE_SEPARATOR + "\t\t" + "<!--" + comment + "-->");
            } else {
                if (first) {
                    first = false;
                } else {
                    out.print(",");
                }
                if (comment.length() > 70 && SEPARATOR.equals(CldrUtility.LINE_SEPARATOR)) {
                    comment = printingName(printingLocale, spacing) + SEPARATOR + "    // " + spacing + "=>" + spacing
                        + printingName(printingTarget, spacing);
                }
                out.print(
                    "  {"
                        + SEPARATOR + "    // " + comment
                        + SEPARATOR + "    \"" + printingLocale + "\","
                        + SEPARATOR + "    \"" + printingTarget + "\""
                        + CldrUtility.LINE_SEPARATOR + "  }");
            }
        }
        out.println(footer);
        out.close();
    }

    public static String printingName(String locale, String spacing) {
        if (locale == null) {
            return null;
        }
        LanguageTagParser parser = new LanguageTagParser().set(locale);
        String lang = parser.getLanguage();
        String script = parser.getScript();
        String region = parser.getRegion();
        return "{" + spacing +
            (lang.equals("und") ? "?" : english.getName(CLDRFile.LANGUAGE_NAME, lang)) + ";" + spacing +
            (script == null || script.equals("") ? "?" : english.getName(CLDRFile.SCRIPT_NAME, script)) + ";" + spacing
            +
            (region == null || region.equals("") ? "?" : english.getName(CLDRFile.TERRITORY_NAME, region)) + spacing
            + "}";
    }

    private static final String[][] ALT_REVERSAL = {
        { "nb", "no" },
        { "no", "nb" },
        { "he", "iw" },
        { "iw", "he" },
    };

    public static String toAlt(String locale, boolean change) {
        if (!change || locale == null) {
            return locale;
        }
        String firstTag = getFirstTag(locale);
        for (String[] pair : ALT_REVERSAL) {
            if (firstTag.equals(pair[0])) {
                locale = pair[1] + locale.substring(pair[1].length());
                break;
            }
        }
        locale = locale.replace("_", "-");
        return locale;
    }

    private static String getFirstTag(String locale) {
        int pos = locale.indexOf('_');
        return pos < 0 ? locale : locale.substring(0, pos);
    }

    // private static Map<String, String> getBackMapping(Map<String, String> fluffup) {
    // Relation<String,String> backMap = new Relation(new TreeMap(), TreeSet.class, BEST_LANGUAGE_COMPARATOR);
    // for (String source : fluffup.keySet()) {
    // if (source.startsWith("und")) {
    // continue;
    // }
    // String maximized = fluffup.get(source);
    // backMap.put(maximized, source); // put in right order
    // }
    // Map<String,String> returnBackMap = new TreeMap();
    // for (String maximized : backMap.keySet()) {
    // final Set<String> all = backMap.getAll(maximized);
    // final String minimized = all.iterator().next();
    // returnBackMap.put(maximized, minimized);
    // }
    // return returnBackMap;
    // }

    /**
     * Language tags are presumed to share the first language, except possibly "und". Best is least
     */
    // private static Comparator BEST_LANGUAGE_COMPARATOR = new Comparator<String>() {
    // LanguageTagParser p1 = new LanguageTagParser();
    // LanguageTagParser p2 = new LanguageTagParser();
    // public int compare(String o1, String o2) {
    // if (o1.equals(o2)) return 0;
    // p1.set(o1);
    // p2.set(o2);
    // String lang1 = p1.getLanguage();
    // String lang2 = p2.getLanguage();
    //
    // // compare languages first
    // // put und at the end
    // int result = lang1.compareTo(lang2);
    // if (result != 0) {
    // if (lang1.equals("und")) return 1;
    // if (lang2.equals("und")) return -1;
    // return result;
    // }
    //
    // // now scripts and regions.
    // // if they have different numbers of fields, the shorter wins.
    // // If there are two fields, region is lowest.
    // // The simplest way is to just compare scripts first
    // // so zh-TW < zh-Hant, because we first compare "" to Hant
    // String script1 = p1.getScript();
    // String script2 = p2.getScript();
    // int scriptOrder = script1.compareTo(script2);
    // if (scriptOrder != 0) return scriptOrder;
    //
    // String region1 = p1.getRegion();
    // String region2 = p2.getRegion();
    // int regionOrder = region1.compareTo(region2);
    // if (regionOrder != 0) return regionOrder;
    //
    // return o1.compareTo(o2);
    // }
    //
    // };

    public static void minimize(Map<String, String> fluffup) {
        LanguageTagParser parser = new LanguageTagParser();
        LanguageTagParser targetParser = new LanguageTagParser();
        Set<String> removals = new TreeSet<String>();
        while (true) {
            removals.clear();
            for (String locale : fluffup.keySet()) {
                String target = fluffup.get(locale);
                if (targetParser.set(target).getRegion().equals(UNKNOWN_REGION)) {
                    removals.add(locale);
                    if (SHOW_ADD)
                        System.out.println("Removing:\t" + getName(locale) + "\t=>\t" + getName(target)
                            + "\t\t - Unknown Region in target");
                    continue;
                }
                if (targetParser.getScript().equals(UNKNOWN_SCRIPT)) {
                    removals.add(locale);
                    if (SHOW_ADD)
                        System.out.println("Removing:\t" + getName(locale) + "\t=>\t" + getName(target)
                            + "\t\t - Unknown Script in target");
                    continue;
                }

                String region = parser.set(locale).getRegion();
                if (region.length() != 0) {
                    if (region.equals(UNKNOWN_REGION)) {
                        removals.add(locale);
                        if (SHOW_ADD)
                            System.out.println("Removing:\t" + getName(locale) + "\t=>\t" + getName(target)
                                + "\t\t - Unknown Region in source");
                        continue;
                    }
                    parser.setRegion("");
                    String newLocale = parser.toString();
                    String newTarget = fluffup.get(newLocale);
                    if (newTarget != null) {
                        newTarget = targetParser.set(newTarget).setRegion(region).toString();
                        if (target.equals(newTarget) && !KEEP_TARGETS.contains(locale)) {
                            removals.add(locale);
                            if (SHOW_ADD)
                                System.out.println("Removing:\t" + locale + "\t=>\t" + target + "\t\tRedundant with "
                                    + newLocale);
                            continue;
                        }
                    }
                }
                String script = parser.set(locale).getScript();
                if (locale.equals(DEBUG_ADD_KEY)) {
                    System.out.println("*debug*");
                }
                if (script.length() != 0) {
                    if (script.equals(UNKNOWN_SCRIPT)) {
                        removals.add(locale);
                        if (SHOW_ADD)
                            System.out.println("Removing:\t" + locale + "\t=>\t" + target + "\t\t - Unknown Script");
                        continue;
                    }
                    parser.setScript("");
                    String newLocale = parser.toString();
                    String newTarget = fluffup.get(newLocale);
                    if (newTarget != null) {
                        newTarget = targetParser.set(newTarget).setScript(script).toString();
                        if (target.equals(newTarget) && !KEEP_TARGETS.contains(locale)) {
                            removals.add(locale);
                            if (SHOW_ADD)
                                System.out.println("Removing:\t" + locale + "\t=>\t" + target + "\t\tRedundant with "
                                    + newLocale);
                            continue;
                        }
                    }
                }
            }
            if (removals.size() == 0) {
                break;
            }
            for (String locale : removals) {
                fluffup.remove(locale);
            }
        }
    }

    // private static void addLanguageScript(Map<String, String> fluffup, LanguageTagParser parser) {
    // // add script
    // Map<String, String> temp = new TreeMap<String, String>();
    // while (true) {
    // temp.clear();
    // for (String target : new TreeSet<String>(fluffup.values())) {
    // parser.set(target);
    // final String territory = parser.getRegion();
    // if (territory.length() == 0) {
    // continue;
    // }
    // parser.setRegion("");
    // String possibleSource = parser.toString();
    // if (fluffup.containsKey(possibleSource)) {
    // continue;
    // }
    // String other = temp.get(possibleSource);
    // if (other != null) {
    // if (!target.equals(other)) {
    // System.out.println("**Failure with multiple sources in addLanguageScript: "
    // + possibleSource + "\t=>\t" + target + ", " + other);
    // }
    // continue;
    // }
    // temp.put(possibleSource, target);
    // if (SHOW_ADD) System.out.println("Adding:\t" + possibleSource + "\t=>\t" + target + "\t\tLanguage-Script");
    // }
    // if (temp.size() == 0) {
    // break;
    // }
    // fluffup.putAll(temp);
    // }
    //
    // }

    // private static void addLanguageCountry(Map<String, String> fluffup, LanguageTagParser parser) {
    // // add script
    // Map<String, String> temp = new TreeMap<String, String>();
    // while (true) {
    // temp.clear();
    // for (String target : new TreeSet<String>(fluffup.values())) {
    // parser.set(target);
    // String script = parser.getScript();
    // if (script.length() == 0) {
    // continue;
    // }
    // parser.setScript("");
    // String possibleSource = parser.toString();
    // if (fluffup.containsKey(possibleSource)) {
    // continue;
    // }
    // String other = temp.get(possibleSource);
    //
    // if (other != null) {
    // if (!target.equals(other)) {
    // script = getScriptForLocale(possibleSource);
    // if (script == null) {
    // System.out.println("**Failure with multiple sources in addLanguageCountry: "
    // + possibleSource + "\t=>\t" + target + ", " + other);
    // continue; // error message in routine
    // }
    // parser.setScript(script);
    // target = parser.toString();
    // }
    // }
    //
    // temp.put(possibleSource, target);
    // if (SHOW_ADD) System.out.println("Adding:\t" + possibleSource + "\t=>\t" + target + "\t\tLanguageCountry");
    // }
    // if (temp.size() == 0) {
    // break;
    // }
    // fluffup.putAll(temp);
    // }
    //
    // }

    // private static void addScript(Map<String, String> fluffup, LanguageTagParser parser) {
    // // add script
    // Map<String, String> temp = new TreeMap<String, String>();
    // while (true) {
    // temp.clear();
    // Set skipTarget = fluffup.keySet();
    // for (String locale : fluffup.keySet()) {
    // String target = fluffup.get(locale);
    // parser.set(target);
    // if (parser.getScript().length() != 0) {
    // continue;
    // }
    // String script = getScriptForLocale(target);
    //
    // if (script == null) {
    // continue; // error message in routine
    // }
    // parser.setScript(script);
    // String furtherTarget = parser.toString();
    // addIfNotIn(target, furtherTarget, temp, fluffup, "Script");
    // }
    // if (temp.size() == 0) {
    // break;
    // }
    // fluffup.putAll(temp);
    // }
    // }

    // private static String getScriptForLocale(String locale) {
    // String result = getScriptForLocale2(locale);
    // if (result != null) return result;
    // int pos = locale.indexOf('_');
    // if (pos >= 0) {
    // result = getScriptForLocale2(locale.substring(0,pos));
    // }
    // return result;
    // }

    private static String UNKNOWN_SCRIPT = "Zzzz";
    private static String UNKNOWN_REGION = "ZZ";

    private static String getScriptForLocale2(String locale) {
        String result = localeToScriptCache.get(locale);
        if (result != null) {
            return result;
        }
        if (locale.equals("ky")) {
            int debug = 0;
        }
        try {
            Map<Type, BasicLanguageData> data = supplementalData.getBasicLanguageDataMap(locale);
            if (data != null) {
                for (BasicLanguageData datum : data.values()) {
                    final Set<String> scripts = datum.getScripts();
                    boolean isPrimary = datum.getType() == BasicLanguageData.Type.primary;
                    if (scripts.size() != 1) {
                        if (scripts.size() > 1 && isPrimary) {
                            break;
                        }
                        continue;
                    }
                    String script = scripts.iterator().next();
                    if (isPrimary) {
                        return result = script;
                    } else if (result == null) {
                        result = script;
                    }
                }
                if (result != null) {
                    return result;
                }
            }
            CLDRFile cldrFile;
            try {
                cldrFile = factory.make(locale, true);
            } catch (RuntimeException e) {
                result = FALLBACK_SCRIPTS.get(locale);
                if (result == null) {
                    System.out.println("***Failed to find script for: " + locale + "\t" + english.getName(locale));
                    return result = UNKNOWN_SCRIPT;
                } else {
                    return result;
                }
            }
            UnicodeSet exemplars = getExemplarSet(cldrFile, "");
            Set<String> CLDRScripts = getScriptsFromUnicodeSet(exemplars);
            CLDRScripts.remove(UNKNOWN_SCRIPT);
            if (CLDRScripts.size() == 1) {
                return result = CLDRScripts.iterator().next();
            } else if (CLDRScripts.size() == 0) {
                System.out.println("**Failed to get script for:\t" + locale);
                return result = UNKNOWN_SCRIPT;
            } else {
                System.out.println("**Failed, too many scripts for:\t" + locale + ", " + CLDRScripts);
                return result = UNKNOWN_SCRIPT;
            }
        } finally {
            if (result.equals(UNKNOWN_SCRIPT)) {
                String temp = LANGUAGE_OVERRIDES.get(locale);
                if (temp != null) {
                    result = new LanguageTagParser().set(temp).getScript();
                    System.out.println("Getting script from LANGUAGE_OVERRIDES for " + locale + " => " + result);
                }
            }
            localeToScriptCache.put(locale, result);
            if (SHOW_ADD)
                System.out.println("Script:\t" + locale + "\t" + english.getName(locale) + "\t=>\t" + result + "\t"
                    + english.getName(CLDRFile.SCRIPT_NAME, result));
        }
    }

    // private static Map<String, String> closeMapping(Map<String, String> fluffup) {
    // if (SHOW_ADD) System.out.flush();
    // Map<String,String> temp = new TreeMap<String,String>();
    // while (true) {
    // temp.clear();
    // for (String locale : fluffup.keySet()) {
    // String target = fluffup.get(locale);
    // if (target.equals("si_Sinh") || target.equals("zh-Hani")) {
    // System.out.println("????");
    // }
    // String furtherTarget = fluffup.get(target);
    // if (furtherTarget == null) {
    // continue;
    // }
    // addIfNotIn(locale, furtherTarget, temp, null, "Close");
    // }
    // if (temp.size() == 0) {
    // break;
    // }
    // fluffup.putAll(temp);
    // }
    // if (SHOW_ADD) System.out.flush();
    // return temp;
    // }

    public static Set<String> getScriptsFromUnicodeSet(UnicodeSet exemplars) {
        // use bits first, since that's faster
        BitSet scriptBits = new BitSet();
        boolean show = false;
        for (UnicodeSetIterator it = new UnicodeSetIterator(exemplars); it.next();) {
            if (show)
                System.out.println(Integer.toHexString(it.codepoint));
            if (it.codepoint != UnicodeSetIterator.IS_STRING) {
                scriptBits.set(UScript.getScript(it.codepoint));
            } else {
                int cp;
                for (int i = 0; i < it.string.length(); i += UTF16.getCharCount(cp)) {
                    scriptBits.set(UScript.getScript(cp = UTF16.charAt(it.string, i)));
                }
            }
        }
        scriptBits.clear(UScript.COMMON);
        scriptBits.clear(UScript.INHERITED);
        Set<String> scripts = new TreeSet<String>();
        for (int j = 0; j < scriptBits.size(); ++j) {
            if (scriptBits.get(j)) {
                scripts.add(UScript.getShortName(j));
            }
        }
        return scripts;
    }

    public static UnicodeSet getExemplarSet(CLDRFile cldrfile, String type) {
        if (type.length() != 0)
            type = "[@type=\"" + type + "\"]";
        String v = cldrfile.getStringValue("//ldml/characters/exemplarCharacters"
            + type);
        if (v == null)
            return new UnicodeSet();
        return new UnicodeSet(v);
    }

    // private static String[][] SpecialCases = {
    // { "zh_Hani", "zh_Hans_CN"},
    // { "si_Sinh", "si_Sinh_LK"},
    // { "ii", "ii_CN"}, // Sichuan Yi (Yi)
    // { "iu", "iu_CA"}, // Inuktitut (Unified Canadian Aboriginal Syllabics)
    // { "und", "en"}, // English default
    // };

    static void showDefaultContentDifferencesAndFix(Set<String> defaultLocaleContent) {
        Set<String> errors = new LinkedHashSet<String>();
        Map<String, String> oldDefaultContent = SupplementalDataInfo.makeLocaleToDefaultContents(
            ConvertLanguageData.supplementalData.getDefaultContentLocales(), new TreeMap<String, String>(), errors);
        if (!errors.isEmpty()) {
            System.out.println(CollectionUtilities.join(errors, "\n"));
            errors.clear();
        }
        Map<String, String> newDefaultContent = SupplementalDataInfo.makeLocaleToDefaultContents(defaultLocaleContent,
            new TreeMap<String, String>(), errors);
        if (!errors.isEmpty()) {
            System.out.println("Default Content errors: " + CollectionUtilities.join(errors, "\n"));
            errors.clear();
        }
        Set<String> changes = compareMapsAndFixNew("*WARNING* Default Content: ", oldDefaultContent, newDefaultContent,
            "ar", "ar_001");
        System.out.println(CollectionUtilities.join(changes, "\n"));
        defaultLocaleContent.clear();
        defaultLocaleContent.addAll(newDefaultContent.values());
        newDefaultContent = SupplementalDataInfo.makeLocaleToDefaultContents(defaultLocaleContent,
            new TreeMap<String, String>(), errors);
        if (!errors.isEmpty()) {
            System.out.println("***New Errors: " + CollectionUtilities.join(errors, "\n"));
        }
    }

    private static Set<String> compareMapsAndFixNew(String title,
        Map<String, String> oldContent,
        Map<String, String> newContent, String... allowedOverrideValues) {
        Map<String, String> allowedOverrideValuesTest = new HashMap<String, String>();
        for (int i = 0; i < allowedOverrideValues.length; i += 2) {
            allowedOverrideValuesTest.put(allowedOverrideValues[i], allowedOverrideValues[i + 1]);
        }
        Set<String> changes = new TreeSet<String>();
        for (String parent : Builder.with(new TreeSet<String>()).addAll(newContent.keySet())
            .addAll(oldContent.keySet()).get()) {
            String oldValue = oldContent.get(parent);
            String newValue = newContent.get(parent);
            String overrideValue = allowedOverrideValuesTest.get(parent);
            if (overrideValue != null) {
                newContent.put(parent, overrideValue);
                newValue = overrideValue;
            }
            if (CldrUtility.equals(oldValue, newValue)) {
                continue;
            }
            String message;
            if (oldValue == null) {
                message = "Adding " + ConvertLanguageData.getLanguageCodeAndName(parent) + " => "
                    + ConvertLanguageData.getLanguageCodeAndName(newValue);
                newContent.put(parent, newValue);
            } else if (newValue == null) {
                if (SUPPRESS_CHANGES) {
                    message = "Suppressing removal of "
                        + ConvertLanguageData.getLanguageCodeAndName(parent) + " => "
                        + ConvertLanguageData.getLanguageCodeAndName(oldValue);
                    newContent.put(parent, oldValue);
                } else {
                    message = "Removing "
                        + ConvertLanguageData.getLanguageCodeAndName(parent) + " => "
                        + ConvertLanguageData.getLanguageCodeAndName(oldValue);
                    newContent.remove(oldValue);
                }
            } else {
                if (SUPPRESS_CHANGES) {
                    message = "Suppressing change of "
                        + ConvertLanguageData.getLanguageCodeAndName(parent) + " => "
                        + ConvertLanguageData.getLanguageCodeAndName(oldValue) + " to "
                        + ConvertLanguageData.getLanguageCodeAndName(newValue);
                    newContent.remove(newValue);
                    newContent.put(parent, oldValue);
                } else {
                    message = "Changing "
                        + ConvertLanguageData.getLanguageCodeAndName(parent) + " => "
                        + ConvertLanguageData.getLanguageCodeAndName(oldValue) + " to "
                        + ConvertLanguageData.getLanguageCodeAndName(newValue);
                    newContent.remove(oldValue);
                    newContent.put(parent, newValue);
                }
            }
            changes.add(title + message);
        }
        return changes;
    }

    public static class LocaleStringComparator implements Comparator<String> {
        LanguageTagParser ltp0 = new LanguageTagParser();
        LanguageTagParser ltp1 = new LanguageTagParser();

        public int compare(String arg0, String arg1) {
            ltp0.set(arg0);
            ltp1.set(arg1);
            String s0 = ltp0.getLanguage();
            String s1 = ltp1.getLanguage();
            int result = s0.compareTo(s1);
            if (result != 0) {
                return s0.equals("und") ? 1
                    : s1.equals("und") ? -1
                        : result;
            }
            s0 = ltp0.getScript();
            s1 = ltp1.getScript();
            result = s0.compareTo(s1);
            if (result != 0) {
                return result;
            }
            s0 = ltp0.getRegion();
            s1 = ltp1.getRegion();
            result = s0.compareTo(s1);
            if (result != 0) {
                return result;
            }
            return arg0.compareTo(arg1); // just in case
        }

    }
}
