package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Output;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.unicode.cldr.tool.LangTagsData.Errors;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.LocaleScriptInfo;
import org.unicode.cldr.util.LocaleValidator;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

/**
 * This class generates likelySubtags.xml, replacing GenerateMaximalSubtags and
 * GenerateLikelyAdditions.
 */
public class GenerateLikelySubtags {

    public enum OutputStyle {
        PLAINTEXT,
        C,
        C_ALT,
        XML
    }

    public enum LocaleOverride {
        KEEP_EXISTING,
        REPLACE_EXISTING
    }

    private static final Joiner JOIN_TAB = Joiner.on('\t').useForNull("∅");

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();

    private static final Map<String, Status> LANGUAGE_CODE_TO_STATUS =
            Validity.getInstance().getCodeToStatus(LstrType.language);
    private static final Map<String, Status> SCRIPT_CODE_TO_STATUS =
            Validity.getInstance().getCodeToStatus(LstrType.script);

    private static final String TEMP_UNKNOWN_REGION = "XZ";

    private static final String DEBUG_ADD_KEY = "und_Latn_ZA";

    private static final double MIN_UNOFFICIAL_LANGUAGE_SIZE = 10000000;
    private static final double MIN_UNOFFICIAL_LANGUAGE_PROPORTION = 0.20;
    private static final double MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE = 100000;

    /** When a language is not official, scale it down. */
    private static final double UNOFFICIAL_SCALE_DOWN = 0.2;

    private static final File list[] = {
        new File(CLDRPaths.MAIN_DIRECTORY), new File(CLDRPaths.EXEMPLARS_DIRECTORY)
    };

    private static Factory factory = SimpleFactory.make(list, ".*");
    private static Factory mainFactory = CLDR_CONFIG.getCldrFactory();
    private static SupplementalDataInfo supplementalData =
            SupplementalDataInfo.getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
    private static StandardCodes standardCodes = StandardCodes.make();
    private static CLDRFile english = factory.make("en", false);
    static Relation<String, String> cldrContainerToLanguages =
            Relation.of(new HashMap<String, Set<String>>(), HashSet.class);

    private static NumberFormat percent = NumberFormat.getPercentInstance();
    private static NumberFormat integer = NumberFormat.getIntegerInstance();

    private static boolean DROP_HARDCODED = false;

    private enum MyOptions {
        minimize(new Params().setHelp("Show minimization actions (")),
        add(new Params().setHelp("Show additions")),
        population(new Params().setHelp("Show population data used")),
        order(new Params().setHelp("Show the priority order for langauge data")),
        debug(new Params().setHelp("Show other debug info")),
        json(new Params().setHelp("Show json error data")),
        watch(
                new Params()
                        .setHelp(
                                "Only show info for locales with listed fields ('|' separated), eg -w419|Aghb|AU|bjt will show info for bjt_Latn or und_Laoo_AU")
                        .setMatch(".*")),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Options myOptions = new Options();

        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    private static boolean SHOW_ADD;
    private static boolean SHOW_MIN;
    private static boolean SHOW_POP;
    private static boolean SHOW_ORDER;
    private static boolean DEBUG;
    private static Map<String, LstrType> WATCH_PAIRS = null;

    private static final boolean SHOW_OVERRIDES = true;

    static final Map<String, LSRSource> silData = LangTagsData.getJsonData();

    public static void main(String[] args) throws IOException {
        System.out.println("Use options to track progress, -w to narrow to specific subtags.");
        MyOptions.parse(args);
        SHOW_ADD = MyOptions.add.option.doesOccur();
        SHOW_MIN = MyOptions.minimize.option.doesOccur();
        SHOW_POP = MyOptions.population.option.doesOccur();
        SHOW_ORDER = MyOptions.order.option.doesOccur();
        DEBUG = MyOptions.debug.option.doesOccur();
        String watchValues = MyOptions.watch.option.getValue();
        if (watchValues != null) {
            Map<String, LstrType> temp = new TreeMap<>();
            Splitter.on('|')
                    .split(watchValues)
                    .forEach(x -> temp.put(x, getTypeFromCasedSubtag(x)));
            WATCH_PAIRS = ImmutableMap.copyOf(temp);
        }
        boolean json = MyOptions.json.option.doesOccur();
        if (json) {
            Errors jsonErrors = LangTagsData.getProcessErrors();
            jsonErrors.printAll();
        }

        Map<String, String> oldOrigins = supplementalData.getLikelyOrigins();
        System.out.println("origins: " + new TreeSet<>(oldOrigins.values()));

        Map<String, String> baseMappings = generatePopulationData(new TreeMap<>(LOCALE_SOURCE));
        System.out.println(JOIN_TAB.join("\nBase data:", baseMappings.size()));

        Map<String, String> itemsRemoved = new TreeMap<>();

        Map<String, String> minimizedMappings = minimize(baseMappings, itemsRemoved);
        System.out.println(JOIN_TAB.join("\nMinimized:", minimizedMappings.size()));

        // Verify that the minimized version produces the same results

        LikelySubtags max = new LikelySubtags(baseMappings);
        LikelySubtags min = new LikelySubtags(minimizedMappings);

        Map<String, String> minFailures = new TreeMap<>(LOCALE_SOURCE);
        int failures = 0;
        System.out.println(
                "\nVerifying that Minimizing doesn't change function\n"
                        + JOIN_TAB.join("status, source, maxTarg, minTarg".split(", ")));
        for (String source : baseMappings.keySet()) {
            String orgTarg = max.maximize(source);
            String minTarg = min.maximize(source);
            if (!orgTarg.equals(minTarg)) {
                minFailures.put(source, orgTarg);
                System.out.println(JOIN_TAB.join("Fail", source, orgTarg, minTarg));
                failures++;
            } else {
                if (watching(SHOW_MIN, source, orgTarg, minTarg)) {
                    System.out.println(JOIN_TAB.join("Watch", source, orgTarg, minTarg));
                }
            }
        }
        if (failures != 0) {
            throw new IllegalArgumentException();
        }

        Set<String> newAdditions = new TreeSet<>();
        Set<String> newMissing = new TreeSet<>();

        // Check against last version

        System.out.println("\nReading old supplemental: may have unrelated errors.");
        final SupplementalDataInfo oldSupplementalInfo =
                SupplementalDataInfo.getInstance(
                        CldrUtility.getPath(CLDRPaths.LAST_COMMON_DIRECTORY, "supplemental/"));
        final Map<String, String> oldLikelyData = oldSupplementalInfo.getLikelySubtags();
        final Map<String, String> oldLikelyOrigins = oldSupplementalInfo.getLikelyOrigins();
        LikelySubtags oldLikely = new LikelySubtags(oldLikelyData);

        Set<String> sorted = new TreeSet<>(LOCALE_SOURCE);
        sorted.addAll(minimizedMappings.keySet());
        sorted.addAll(oldLikelyData.keySet());

        System.out.println(
                "\nCheck against last version\n"
                        + JOIN_TAB.join("Source", "Name", "oldValue", "Name", "newValue", "Name"));

        for (String source : sorted) {
            String oldValue = oldLikely.maximize(source);
            String oldOrigin = oldLikelyOrigins.get(source);
            if (oldOrigin != null && oldOrigin.contains("sil1")) {
                continue; // we don't control variations in sil data
            }
            String newValue = min.maximize(source);
            String removal = itemsRemoved.get(source);

            if (Objects.equal(oldValue, newValue)) {
                continue;
            }
            // skip new values, or oldValues that are specifically removed

            if (oldValue == null || oldValue.equals(removal)) {
                continue; // skip for now
            }

            // special cases

            if (getPart(source, LstrType.language).equals("und")
                    && oldValue.startsWith("en_Latn")) {
                continue; // skip for now
            }

            // show the remainder

            System.out.println(
                    JOIN_TAB.join(
                            source,
                            getNameSafe(source),
                            oldValue,
                            getNameSafe(oldValue),
                            newValue,
                            getNameSafe(newValue)));
        }
        System.out.println("new missing\t" + newMissing);

        printLikelySubtags(minimizedMappings);
    }

    static {
        for (CLDRLocale locale : mainFactory.getAvailableCLDRLocales()) {
            String region = locale.getCountry();
            if (region == null || region.isEmpty() || Containment.isLeaf(region)) {
                continue;
            }
            cldrContainerToLanguages.put(region, locale.getLanguage());
        }
        cldrContainerToLanguages.freeze();
        System.out.println("Keeping macroregions used in cldr " + cldrContainerToLanguages);
    }

    private static final List<String> KEEP_TARGETS =
            DROP_HARDCODED ? List.of() : List.of("und_Arab_PK", "und_Latn_ET");

    private static final ImmutableSet<String> deprecatedISONotInLST =
            DROP_HARDCODED ? ImmutableSet.of() : ImmutableSet.of("scc", "scr");

    /**
     * This is the simplest way to override, by supplying the max value. It gets a very low weight,
     * so doesn't override any stronger value.
     */
    private static final List<String> MAX_ADDITIONS =
            DROP_HARDCODED
                    ? List.of()
                    : List.of(
                            "bss_Latn_CM",
                            "gez_Ethi_ET",
                            "ken_Latn_CM",
                            "und_Arab_PK",
                            "wa_Latn_BE",
                            "fub_Arab_CM",
                            "fuf_Latn_GN",
                            "kby_Arab_NE",
                            "kdh_Latn_TG",
                            "zlm_Latn_MY",
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
                            "clc_Latn_CA",
                            "crg_Latn_CA",
                            "hur_Latn_CA",
                            "kwk_Latn_CA",
                            "lil_Latn_CA",
                            "ojs_Cans_CA",
                            "oka_Latn_CA",
                            "pqm_Latn_CA",
                            "no_Latn_NO",
                            "tok_Latn_001",
                            "prg_Latn_PL",
                            "ie_Latn_EE");

    /**
     * The following overrides do MASH the final values, so they may not result in consistent
     * results. Safer is to add to MAX_ADDITIONS. However, if you add, add both the language and
     * language+script mappings.
     */

    // Many of the overrides below can be removed once the language/pop/country data is updated.

    private static final Map<String, String> LANGUAGE_OVERRIDES =
            CldrUtility.asMap(
                    DROP_HARDCODED
                            ? new String[][] {
                                {LocaleNames.UND, "en_Latn_US"},
                            }
                            : new String[][] {
                                {"cic", "cic_Latn_US"},
                                {"cic_Latn", "cic_Latn_US"},
                                {"eo", "eo_Latn_001"},
                                {"eo_Latn", "eo_Latn_001"},
                                {"es", "es_Latn_ES"},
                                {"es_Latn", "es_Latn_ES"},
                                {"ff_BF", "ff_Latn_BF"},
                                {"ff_GM", "ff_Latn_GM"},
                                {"ff_GH", "ff_Latn_GH"},
                                {"ff_GW", "ff_Latn_GW"},
                                {"ff_LR", "ff_Latn_LR"},
                                {"ff_NE", "ff_Latn_NE"},
                                {"ff_NG", "ff_Latn_NG"},
                                {"ff_SL", "ff_Latn_SL"},
                                {"ff_Adlm", "ff_Adlm_GN"},
                                {"ia", "ia_Latn_001"},
                                {"ia_Latn", "ia_Latn_001"},
                                {"io", "io_Latn_001"},
                                {"io_Latn", "io_Latn_001"},
                                {"jbo", "jbo_Latn_001"},
                                {"jbo_Latn", "jbo_Latn_001"},
                                {"ku_Arab", "ku_Arab_IQ"},
                                {"lrc", "lrc_Arab_IR"},
                                {"lrc_Arab", "lrc_Arab_IR"},
                                {"man", "man_Latn_GM"},
                                {"man_Latn", "man_Latn_GM"},
                                {"mas", "mas_Latn_KE"},
                                {"mas_Latn", "mas_Latn_KE"},
                                {"mn", "mn_Cyrl_MN"},
                                {"mn_Cyrl", "mn_Cyrl_MN"},
                                {"mro", "mro_Mroo_BD"},
                                {"mro_BD", "mro_Mroo_BD"},
                                {"ms_Arab", "ms_Arab_MY"},
                                {"nan", "nan_Hans_CN"},
                                {"nan_Hant", "nan_Hant_TW"},
                                {"nan_Hans", "nan_Hans_CN"},
                                {"nan_TW", "nan_Hant_TW"},
                                {"pap", "pap_Latn_CW"},
                                {"pap_Latn", "pap_Latn_CW"},
                                {
                                    "rif", "rif_Latn_MA"
                                }, // https://unicode-org.atlassian.net/browse/CLDR-14962?focusedCommentId=165053
                                {"rif_Latn", "rif_Latn_MA"},
                                {"rif_Tfng", "rif_Tfng_MA"},
                                {"rif_MA", "rif_Latn_MA"}, // Ibid
                                {"shi", "shi_Tfng_MA"},
                                {"shi_Tfng", "shi_Tfng_MA"},
                                {"shi_MA", "shi_Tfng_MA"},
                                {"sr_Latn", "sr_Latn_RS"},
                                {"ss", "ss_Latn_ZA"},
                                {"ss_Latn", "ss_Latn_ZA"},
                                // {"swc", "swc_Latn_CD"},
                                {"ti", "ti_Ethi_ET"},
                                {"ti_Ethi", "ti_Ethi_ET"},
                                {LocaleNames.UND, "en_Latn_US"},
                                {"und_Adlm", "ff_Adlm_GN"},
                                {"und_Adlm_GN", "ff_Adlm_GN"},
                                {"und_Arab", "ar_Arab_EG"},
                                {"und_Arab_PK", "ur_Arab_PK"},
                                {"und_Bopo", "zh_Bopo_TW"},
                                {"und_Deva_FJ", "hif_Deva_FJ"},
                                {"und_Hani", "zh_Hani_CN"},
                                {"und_Hani_CN", "zh_Hani_CN"},
                                {"und_Kana", "ja_Kana_JP"},
                                {"und_Kana_JP", "ja_Kana_JP"},
                                {"und_Latn", "en_Latn_US"},
                                {"und_001", "en_Latn_001"}, // to not be overridden by tok_Latn_001
                                {
                                    "und_Latn_001", "en_Latn_001"
                                }, // to not be overridden by tok_Latn_001
                                {"und_Latn_ET", "en_Latn_ET"},
                                {"und_Latn_NE", "ha_Latn_NE"},
                                {"und_Latn_PH", "fil_Latn_PH"},
                                {"und_ML", "bm_Latn_ML"},
                                {"und_Latn_ML", "bm_Latn_ML"},
                                {"und_NE", "ha_Latn_NE"},
                                {"und_PH", "fil_Latn_PH"},
                                {"und_PK", "ur_Arab_PK"},
                                {"und_SO", "so_Latn_SO"},
                                {"und_SS", "en_Latn_SS"},
                                {"vo", "vo_Latn_001"},
                                {"vo_Latn", "vo_Latn_001"},
                                //                                {"yi", "yi_Hebr_001"},
                                //                                {"yi_Hebr", "yi_Hebr_001"},
                                {"yue", "yue_Hant_HK"},
                                {"yue_Hant", "yue_Hant_HK"},
                                {"yue_Hans", "yue_Hans_CN"},
                                {"yue_CN", "yue_Hans_CN"},
                                {"zh_Hani", "zh_Hani_CN"},
                                {"zh_Bopo", "zh_Bopo_TW"},
                                {"ccp", "ccp_Cakm_BD"},
                                {"ccp_Cakm", "ccp_Cakm_BD"},
                                {"und_Cakm", "ccp_Cakm_BD"},
                                {"cu_Glag", "cu_Glag_BG"},
                                {"sd_Khoj", "sd_Khoj_IN"},
                                {"lif_Limb", "lif_Limb_IN"},
                                {"arc_Nbat", "arc_Nbat_JO"},
                                {"arc_Palm", "arc_Palm_SY"},
                                {"pal_Phlp", "pal_Phlp_CN"},
                                {"en_Shaw", "en_Shaw_GB"},
                                {"sd_Sind", "sd_Sind_IN"},
                                {"und_Brai", "fr_Brai_FR"}, // hack
                                {"und_Hanb", "zh_Hanb_TW"}, // Special script code
                                {"zh_Hanb", "zh_Hanb_TW"}, // Special script code
                                {"und_Jamo", "ko_Jamo_KR"}, // Special script code

                                // {"und_Cyrl_PL", "be_Cyrl_PL"},

                                //        {"cr", "cr_Cans_CA"},
                                //        {"hif", "hif_Latn_FJ"},
                                //        {"gon", "gon_Telu_IN"},
                                //        {"lif", "lif_Deva_NP"},
                                //        {"unx", "unx_Beng_IN"},
                                //        {"unr", "unr_Beng_IN"},
                                //        {"bsq", "bsq_Bass_LR"},
                                //        {"ccp", "ccp_Cakm_BD"},
                                //        {"blt", "blt_Tavt_VN"},
                                //        { "mis_Medf", "mis_Medf_NG" },

                                {"ku_Yezi", "ku_Yezi_GE"},
                                {"hnj", "hnj_Hmnp_US"}, // preferred lang/script in CLDR
                                {"hnj_Hmnp", "hnj_Hmnp_US"},
                                {"und_Hmnp", "hnj_Hmnp_US"},
                                {"rhg", "rhg_Rohg_MM"}, // preferred lang/script in CLDR
                                {"rhg_Arab", "rhg_Arab_MM"},
                                {"und_Arab_MM", "rhg_Arab_MM"},
                                {"sd_IN", "sd_Deva_IN"}, // preferred in CLDR
                                // { "sd_Deva", "sd_Deva_IN"},
                                {"und_Cpmn", "und_Cpmn_CY"},
                                {"oc_ES", "oc_Latn_ES"},
                                {"os", "os_Cyrl_GE"},
                                {"os_Cyrl", "os_Cyrl_GE"},

                                // new additions for compatibility with old
                                {"und_419", "es_Latn_419"},
                                {"und_CC", "ms_Arab_CC"},
                                {"und_SS", "ar_Arab_SS"},

                                // additions for missing values from LikelySubtagsText
                                {"und_Arab_AF", "fa_Arab_AF"},
                                {"und_Arab_AZ", "az_Arab_AZ"},
                                {"und_Cyrl_BG", "bg_Cyrl_BG"},
                                {"und_Tibt_BT", "dz_Tibt_BT"},
                                {"und_Cyrl_BY", "be_Cyrl_BY"},
                                {"und_Arab_CC", "ms_Arab_CC"},
                                {"und_Ethi_ER", "ti_Ethi_ER"},
                                {"und_Arab_IR", "fa_Arab_IR"},
                                {"und_Cyrl_KG", "ky_Cyrl_KG"},
                                {"und_Cyrl_MK", "mk_Cyrl_MK"},
                                {"und_Cyrl_MN", "mn_Cyrl_MN"},
                                {"und_Deva_NP", "ne_Deva_NP"},
                                {"und_Cyrl_RS", "sr_Cyrl_RS"},
                                {"und_Cyrl_TJ", "tg_Cyrl_TJ"},
                                {"und_Cyrl_UA", "uk_Cyrl_UA"},
                                {"und_Hans_TW", "zh_Hans_TW"},
                                {"arc_Hatr", "arc_Hatr_IQ"},
                                {"hnj_Hmng", "hnj_Hmng_LA"},
                                {"bap_Krai", "bap_Krai_IN"},
                            });

    /**
     * The following supplements the suppress-script. It overrides info from exemplars and the
     * locale info.
     */
    private static String[][] SpecialScripts = {
        {"zh", "Hans"}, // Hans (not Hani)
        {"yue", "Hant"}, // Hant (not Hani)
        {"ko", "Kore"}, // Korean (North Korea)
        {"ko_KR", "Kore"}, // Korean (North Korea)
        {"ja", "Jpan"}, // Special script for japan

        //        {"chk", "Latn"}, // Chuukese (Micronesia)
        //        {"fil", "Latn"}, // Filipino (Philippines)"
        //        {"pap", "Latn"}, // Papiamento (Netherlands Antilles)
        //        {"pau", "Latn"}, // Palauan (Palau)
        //        {"su", "Latn"}, // Sundanese (Indonesia)
        //        {"tet", "Latn"}, // Tetum (East Timor)
        //        {"tk", "Latn"}, // Turkmen (Turkmenistan)
        //        {"ty", "Latn"}, // Tahitian (French Polynesia)
        // {LocaleNames.UND, "Latn"}, // Ultimate fallback
    };

    private static Map<String, String> localeToScriptCache = new TreeMap<>();

    static {
        for (String language : standardCodes.getAvailableCodes("language")) {
            Map<String, String> info = standardCodes.getLangData("language", language);
            String script = info.get("Suppress-Script");
            if (script != null) {
                localeToScriptCache.put(language, script);
            }
        }
        for (String[] pair : SpecialScripts) { // overriding other elements
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

    /**
     * Debugging function that returns false if the flag is false, otherwise returns true if the
     * WATCH is null or the locales don't match the WATCH.
     *
     * @param flag
     * @param locales
     * @return
     */
    static boolean watching(boolean flag, String... locales) {
        if (!flag) {
            return false;
        }
        if (WATCH_PAIRS == null) {
            return true;
        }
        for (String locale : locales) {
            for (Entry<String, LstrType> entry : WATCH_PAIRS.entrySet()) {
                if (entry.getKey().equals(getPart(locale, entry.getValue()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the LstrType from well-formed, properly cased LSTR subtag. Otherwise, returns null from
     * null, otherwise garbage.
     */
    public static LstrType getTypeFromCasedSubtag(String casedSubtag) {
        if (casedSubtag == null) {
            return null;
        }
        final char cp0 = casedSubtag.charAt(0);
        final char cp1 = casedSubtag.charAt(1);
        return cp0 > 'Z'
                ? LstrType.language // de
                : cp1 > 'Z'
                        ? LstrType.script // Latn
                        : LstrType.region; // US, 001
    }

    /** Get the part of a locale according to the LstrType */
    public static String getPart(String locale, LstrType lstrType) {
        return getPart(CLDRLocale.getInstance(locale), lstrType);
    }

    /** Get the part of a locale according to the LstrType */
    public static String getPart(CLDRLocale loc, LstrType type) {
        switch (type) {
            case language:
                return loc.getLanguage();
            case script:
                return loc.getScript();
            case region:
                return loc.getCountry();
            default:
                throw new IllegalArgumentException(type.toString());
        }
    }

    /** Compare locales, putting und.* last. */
    public static Comparator<String> LOCALE_SOURCE =
            new Comparator<>() {

                @Override
                public int compare(String locale1, String locale2) {
                    CLDRLocale l1 = CLDRLocale.getInstance(locale1);
                    CLDRLocale l2 = CLDRLocale.getInstance(locale2);
                    // sort items with 0 components first, then 1, then 2 (there won't be 3)
                    int result =
                            ComparisonChain.start()
                                    .compare(getLanguage(l1), getLanguage(l2))
                                    .compare(getScript(l1), getScript(l2))
                                    .compare(getRegion(l1), getRegion(l2))
                                    .result();
                    if (result == 0 && !locale1.equals(locale2)) {
                        throw new IllegalArgumentException();
                    }
                    return result;
                }

                private String getLanguage(CLDRLocale loc) {
                    return replaceMissing(loc.getLanguage(), "und", "Ω");
                }

                private String getScript(CLDRLocale loc) {
                    return loc.getScript();
                }

                private String getRegion(CLDRLocale loc) {
                    return loc.getCountry();
                }

                private String replaceMissing(String field, String ifEqual, String replacement) {
                    return ifEqual.equals(field) ? replacement : field;
                }
            };

    static {
        LOCALE_SOURCE.compare("hnj_MM", "hnj_Laoo");
    }

    public static String getNameSafe(String oldValue) {
        try {
            if (oldValue != null) {
                String result = english.getName(oldValue);
                if (result.startsWith("Unknown language ")) {
                    result = result.substring("Unknown language ".length());
                }
                return result;
            }
        } catch (Exception e) {
        }
        return "n/a";
    }

    private static OutputStyle OUTPUT_STYLE =
            OutputStyle.valueOf(CldrUtility.getProperty("OutputStyle", "XML", "XML").toUpperCase());

    private static final String TAG_SEPARATOR = OUTPUT_STYLE == OutputStyle.C_ALT ? "-" : "_";

    private static final Joiner JOIN_SPACE = Joiner.on(' ');
    private static final Joiner JOIN_UBAR = Joiner.on('_');

    private static final Joiner JOIN_LS = Joiner.on(CldrUtility.LINE_SEPARATOR);

    private static Map<String, String> generatePopulationData(Map<String, String> toMaximized) {
        // we are going to try a different approach.
        // first gather counts for maximized values
        // Set<Row.R3<String,String,String>,Double> rowsToCounts = new TreeMap();
        MaxData maxData = new MaxData();
        Set<String> cldrLocales = factory.getAvailable();
        // skip ZZ
        Set<String> otherTerritories =
                new TreeSet<>(
                        Sets.difference(
                                standardCodes.getGoodAvailableCodes("territory"), Set.of("ZZ")));

        // process all the information to get the top values for each triple.
        // each of the combinations of 1 or 2 components gets to be a key.

        Set<String> noPopulationData = new TreeSet<>();

        for (String region : supplementalData.getTerritoriesWithPopulationData()) {
            otherTerritories.remove(region);
            PopulationData regionData = supplementalData.getPopulationDataForTerritory(region);
            final double literateTerritoryPopulation = regionData.getLiteratePopulation();
            // we need any unofficial language to meet a certain absolute size requirement and
            // proportion size
            // requirement.
            // so the bar is x percent of the population, reset up to y absolute size.
            double minimalLiteratePopulation =
                    literateTerritoryPopulation * MIN_UNOFFICIAL_LANGUAGE_PROPORTION;
            if (minimalLiteratePopulation < MIN_UNOFFICIAL_LANGUAGE_SIZE) {
                minimalLiteratePopulation = MIN_UNOFFICIAL_LANGUAGE_SIZE;
            }

            for (String writtenLanguage :
                    supplementalData.getLanguagesForTerritoryWithPopulationData(region)) {
                PopulationData data =
                        supplementalData.getLanguageAndTerritoryPopulationData(
                                writtenLanguage, region);
                final double literatePopulation =
                        getWritingPopulation(data); // data.getLiteratePopulation();
                double order = -literatePopulation; // negative so we get the inverse order

                if (data.getOfficialStatus() == OfficialStatus.unknown) {
                    final String locale = writtenLanguage + "_" + region;
                    //                    if (literatePopulation >= minimalLiteratePopulation) {
                    //                        // ok, skip
                    //                    } else if (literatePopulation >=
                    // MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE
                    //                            && cldrLocales.contains(locale)) {
                    //                        // ok, skip
                    //                    } else {
                    //                        // if (SHOW_ADD)
                    //                        // System.out.println("Skipping:\t" + writtenLanguage
                    // + "\t" + region + "\t"
                    //                        // + english.getName(locale)
                    //                        // + "\t-- too small:\t" +
                    // number.format(literatePopulation));
                    //                        // continue;
                    //                    }
                    order *= UNOFFICIAL_SCALE_DOWN;
                    if (watching(SHOW_POP, writtenLanguage))
                        System.out.println(
                                JOIN_TAB.join(
                                        "Scaling unofficial: ",
                                        writtenLanguage,
                                        region,
                                        getNameSafe(locale),
                                        integer.format(literatePopulation),
                                        percent.format(
                                                literatePopulation / literateTerritoryPopulation),
                                        cldrLocales.contains(locale) ? "CLDR Loc" : ""));
                }

                String script = localeToScriptCache.get(writtenLanguage);
                if (script == null) {
                    script = LocaleScriptInfo.getScriptFromLocaleOrSupplemental(writtenLanguage);
                    if (script == null) {
                        LSRSource silLSR = silData.get(writtenLanguage);
                        if (silLSR != null) {
                            script = silLSR.getScript();
                        } else {
                            noPopulationData.add(writtenLanguage);
                            continue;
                        }
                    }
                    localeToScriptCache.put(writtenLanguage, script);
                }

                String language = writtenLanguage;
                final int pos = writtenLanguage.indexOf('_');
                if (pos > 0) {
                    language = writtenLanguage.substring(0, pos);
                }
                maxData.add(language, script, region, order);
            }
        }
        if (!noPopulationData.isEmpty()) {
            for (String lang : noPopulationData) {
                System.out.println(
                        JOIN_TAB.join("No script in pop. data for", lang, getNameSafe(lang)));
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

        // Old code for getting language to script, adding XZ, which converts to ZZ. Replaced by use
        // of SIL data

        //        for (Entry<String, Collection<String>> entry :
        //                DeriveScripts.getLanguageToScript().asMap().entrySet()) {
        //            String language = entry.getKey();
        //            final Collection<String> values = entry.getValue();
        //            if (values.size() != 1) {
        //                continue; // skip, no either way
        //            }
        //            Set<R3<Double, String, String>> old = maxData.languages.get(language);
        //            if (!maxData.languages.containsKey(language)) {
        //                maxData.add(language, values.iterator().next(), TEMP_UNKNOWN_REGION, 1.0);
        //            }
        //        }

        // add others, with English default
        for (String region : otherTerritories) {
            if (!LocaleValidator.ALLOW_IN_LIKELY.isAllowed(LstrType.region, region, null, null)) {
                continue;
            }
            if (region.length() == 3) continue; // handled with exceptions
            maxData.add("en", "Latn", region, 1.0);
        }

        // get a reverse mapping, so that we can add the aliases

        Map<String, R2<List<String>, String>> languageAliases =
                SupplementalDataInfo.getInstance().getLocaleAliasInfo().get("language");
        for (Entry<String, R2<List<String>, String>> str : languageAliases.entrySet()) {
            String reason = str.getValue().get1();
            if ("overlong".equals(reason)
                    || "bibliographic".equals(reason)
                    || "macrolanguage".equals(reason)) {
                continue;
            }
            List<String> replacements = str.getValue().get0();
            if (replacements == null) {
                continue;
            }

            String badLanguage = str.getKey();
            if (badLanguage.contains("_")) { // only single subtag
                continue;
            }

            if (deprecatedISONotInLST.contains(badLanguage)) {
                continue;
            }

            if (LANGUAGE_CODE_TO_STATUS.get(badLanguage) != Validity.Status.regular) {
                if (!LocaleValidator.ALLOW_IN_LIKELY.isAllowed(
                        LstrType.language, badLanguage, null, null)) {
                    continue;
                }
            }

            // see what the values are for the replacements

            String goodLanguage = replacements.get(0);
            Set<R3<Double, String, String>> goodLanguageData =
                    maxData.languages.getAll(goodLanguage);
            if (goodLanguageData == null) {
                continue;
            }

            R3<Double, String, String> value = goodLanguageData.iterator().next();
            final String script = value.get1();
            final String region = value.get2();

            maxData.add(badLanguage, script, region, 1.0);
            System.out.println(
                    "Adding aliases: "
                            + badLanguage
                            + ", "
                            + script
                            + ", "
                            + region
                            + ", "
                            + reason);
        }

        // now, get the best for each one
        for (String language : maxData.languages.keySet()) {
            R3<Double, String, String> value = maxData.languages.getAll(language).iterator().next();
            final String script = value.get1();
            final String region = value.get2();
            add(
                    language,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "L->SR",
                    LocaleOverride.REPLACE_EXISTING);
        }
        for (String language : maxData.languagesToScripts.keySet()) {
            String script =
                    maxData.languagesToScripts
                            .get(language)
                            .getKeysetSortedByCount(true)
                            .iterator()
                            .next();
            add(
                    language,
                    language + "_" + script,
                    toMaximized,
                    "L->S",
                    LocaleOverride.REPLACE_EXISTING);
        }
        for (String language : maxData.languagesToRegions.keySet()) {
            String region =
                    maxData.languagesToRegions
                            .get(language)
                            .getKeysetSortedByCount(true)
                            .iterator()
                            .next();
            add(
                    language,
                    language + "_" + region,
                    toMaximized,
                    "L->R",
                    LocaleOverride.REPLACE_EXISTING);
        }

        for (String script : maxData.scripts.keySet()) {
            R3<Double, String, String> value = maxData.scripts.getAll(script).iterator().next();
            final String language = value.get1();
            final String region = value.get2();
            add(
                    "und_" + script,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "S->LR",
                    LocaleOverride.REPLACE_EXISTING);
        }
        for (String script : maxData.scriptsToLanguages.keySet()) {
            String language =
                    maxData.scriptsToLanguages
                            .get(script)
                            .getKeysetSortedByCount(true)
                            .iterator()
                            .next();
            add(
                    "und_" + script,
                    language + "_" + script,
                    toMaximized,
                    "S->L",
                    LocaleOverride.REPLACE_EXISTING);
        }
        for (String script : maxData.scriptsToRegions.keySet()) {
            String region =
                    maxData.scriptsToRegions
                            .get(script)
                            .getKeysetSortedByCount(true)
                            .iterator()
                            .next();
            add(
                    "und_" + script,
                    "und_" + script + "_" + region,
                    toMaximized,
                    "S->R",
                    LocaleOverride.REPLACE_EXISTING);
        }

        for (String region : maxData.regions.keySet()) {
            R3<Double, String, String> value = maxData.regions.getAll(region).iterator().next();
            final String language = value.get1();
            final String script = value.get2();
            add(
                    "und_" + region,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "R->LS",
                    LocaleOverride.REPLACE_EXISTING);
        }
        for (String region : maxData.regionsToLanguages.keySet()) {
            String language =
                    maxData.regionsToLanguages
                            .get(region)
                            .getKeysetSortedByCount(true)
                            .iterator()
                            .next();
            add(
                    "und_" + region,
                    language + "_" + region,
                    toMaximized,
                    "R->L",
                    LocaleOverride.REPLACE_EXISTING);
        }
        for (String region : maxData.regionsToScripts.keySet()) {
            String script =
                    maxData.regionsToScripts
                            .get(region)
                            .getKeysetSortedByCount(true)
                            .iterator()
                            .next();
            add(
                    "und_" + region,
                    "und_" + script + "_" + region,
                    toMaximized,
                    "R->S",
                    LocaleOverride.REPLACE_EXISTING);
        }

        for (R2<String, String> languageScript : maxData.languageScripts.keySet()) {
            R2<Double, String> value =
                    maxData.languageScripts.getAll(languageScript).iterator().next();
            final String language = languageScript.get0();
            final String script = languageScript.get1();
            final String region = value.get1();
            add(
                    language + "_" + script,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "LS->R",
                    LocaleOverride.REPLACE_EXISTING);
        }

        for (R2<String, String> scriptRegion : maxData.scriptRegions.keySet()) {
            R2<Double, String> value = maxData.scriptRegions.getAll(scriptRegion).iterator().next();
            final String script = scriptRegion.get0();
            final String region = scriptRegion.get1();
            final String language = value.get1();
            add(
                    "und_" + script + "_" + region,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "SR->L",
                    LocaleOverride.REPLACE_EXISTING);
        }

        for (R2<String, String> languageRegion : maxData.languageRegions.keySet()) {
            R2<Double, String> value =
                    maxData.languageRegions.getAll(languageRegion).iterator().next();
            final String language = languageRegion.get0();
            final String region = languageRegion.get1();
            final String script = value.get1();
            add(
                    language + "_" + region,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "LR->S",
                    LocaleOverride.REPLACE_EXISTING);
        }

        // get the script info from metadata as fallback

        TreeSet<String> sorted = new TreeSet<>(ScriptMetadata.getScripts());
        for (String script : sorted) {
            switch (SCRIPT_CODE_TO_STATUS.get(script)) {
                case special:
                case unknown:
                    continue;
                default:
                    break;
            }
            Info i = ScriptMetadata.getInfo(script);
            String likelyLanguage = i.likelyLanguage;
            String originCountry = i.originCountry;
            if (LANGUAGE_CODE_TO_STATUS.get(likelyLanguage) == Status.special) {
                likelyLanguage = LocaleNames.UND;
            }
            LanguageTagParser ltp =
                    new LanguageTagParser()
                            .setLanguage(likelyLanguage)
                            .setScript(script)
                            .setRegion(originCountry);
            Set<String> errors = new LinkedHashSet<>();
            if (!LocaleValidator.isValid(ltp, LocaleValidator.ALLOW_IN_LIKELY, errors)) {
                System.out.println(JOIN_LS.join("Failure in ScriptMetaData: " + ltp, errors));
                continue;
            }
            final String result = likelyLanguage + "_" + script + "_" + originCountry;
            add("und_" + script, result, toMaximized, "S->LR•", LocaleOverride.KEEP_EXISTING);
            add(likelyLanguage, result, toMaximized, "L->SR•", LocaleOverride.KEEP_EXISTING);
        }

        // add overrides
        for (Entry<String, String> entry : LANGUAGE_OVERRIDES.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            add(source, target, toMaximized, "OVERRIDE", LocaleOverride.REPLACE_EXISTING);
        }

        // Make sure mapping is additive
        // That is, adding a field doesn't disturb results
        // if a__ => a_b_c, then a_b_ => a_b_c, a__c => a_b_c
        // if _b_ => a_b_c, then a_b_ => a_b_c, _b_c => a_b_c
        // if __c => a_b_c, then a__c => a_b_c, _b_c => a_b_c
        //
        int failures = 0;
        for (Entry<String, String> entry : toMaximized.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();
            CLDRLocale cSource = CLDRLocale.getInstance(source);
            String sLang = cSource.getLanguage();
            if (sLang.equals("und")) { // normalize to make computation easier
                sLang = "";
            }
            String sScript = cSource.getScript();
            String sRegion = cSource.getRegion();

            int fieldCount = countNonEmpty(sLang, sScript, sRegion);
            switch (fieldCount) {
                case 1:
                    break; // the case we care about
                case 0:
                case 2:
                    continue;
                default:
                    throw new IllegalArgumentException("Bad field count: " + cSource);
            }

            CLDRLocale cTarget = CLDRLocale.getInstance(target);
            String tLang = cTarget.getLanguage();
            String tScript = cTarget.getScript();
            String tRegion = cTarget.getRegion();
            if (!sLang.isBlank()) {
                failures +=
                        getErrorCount(toMaximized, source, target, JOIN_UBAR.join(sLang, tScript));
                failures +=
                        getErrorCount(toMaximized, source, target, JOIN_UBAR.join(sLang, tRegion));
            } else if (!sScript.isBlank()) {
                failures +=
                        getErrorCount(toMaximized, source, target, JOIN_UBAR.join(tLang, sScript));
                failures +=
                        getErrorCount(
                                toMaximized,
                                source,
                                target,
                                JOIN_UBAR.join("und", sScript, tRegion));
            } else { // region
                failures +=
                        getErrorCount(toMaximized, source, target, JOIN_UBAR.join(tLang, sRegion));
                failures +=
                        getErrorCount(
                                toMaximized,
                                source,
                                target,
                                JOIN_UBAR.join("und", tScript, sRegion));
            }
        }
        if (failures != 0) {
            throw new IllegalArgumentException("Non-additive failure count: " + failures);
        }

        // Make sure that the mapping is Idempotent. If we have A ==> B, we must never have B ==> C
        // We run this check until we get no problems.
        Set<List<String>> problems = new HashSet<>();

        while (true) {
            problems.clear();
            for (Entry<String, String> entry : toMaximized.entrySet()) {
                String source = entry.getKey();
                String target = entry.getValue();
                if (target.contains("_Zzzz") || target.contains("_ZZ")) { // these are special cases
                    continue;
                }
                String idempotentCandidate = LikelySubtags.maximize(target, toMaximized);

                if (idempotentCandidate == null) {
                    System.out.println("Can't maximize " + target);
                } else if (!idempotentCandidate.equals(target)) {
                    problems.add(ImmutableList.of(source, target, idempotentCandidate));
                }
            }
            if (problems.isEmpty()) {
                break;
            }
            for (List<String> row : problems) {
                System.out.println(
                        "Idempotence: dropping mapping "
                                + row.get(0)
                                + " to "
                                + row.get(1)
                                + " since the target maps further to "
                                + row.get(2));
                toMaximized.remove(row.get(0));
            }
        }
        return CldrUtility.protectCollection(toMaximized);
    }

    private static int getErrorCount(
            Map<String, String> toMaximized, String source, String target, String modSource) {
        String modTarget = toMaximized.get(modSource);
        if (modTarget != null && !target.equals(modTarget)) {
            System.out.println(
                    JOIN_SPACE.join("Non-additive: ", source, target, modSource, modTarget));
            return 1;
        }
        return 0;
    }

    public static int countNonEmpty(String... items) {
        int count = 0;
        for (String item : items) {
            if (!item.isEmpty()) {
                ++count;
            }
        }
        return count;
    }

    /** Class for maximizing data sources */
    public static class MaxData {
        Relation<String, Row.R3<Double, String, String>> languages =
                Relation.of(
                        new TreeMap<String, Set<Row.R3<Double, String, String>>>(), TreeSet.class);
        Map<String, Counter<String>> languagesToScripts = new TreeMap<>();
        Map<String, Counter<String>> languagesToRegions = new TreeMap<>();

        Relation<String, Row.R3<Double, String, String>> scripts =
                Relation.of(
                        new TreeMap<String, Set<Row.R3<Double, String, String>>>(), TreeSet.class);
        Map<String, Counter<String>> scriptsToLanguages = new TreeMap<>();
        Map<String, Counter<String>> scriptsToRegions = new TreeMap<>();

        Relation<String, Row.R3<Double, String, String>> regions =
                Relation.of(
                        new TreeMap<String, Set<Row.R3<Double, String, String>>>(), TreeSet.class);
        Map<String, Counter<String>> regionsToLanguages = new TreeMap<>();
        Map<String, Counter<String>> regionsToScripts = new TreeMap<>();

        Relation<Row.R2<String, String>, Row.R2<Double, String>> languageScripts =
                Relation.of(
                        new TreeMap<Row.R2<String, String>, Set<Row.R2<Double, String>>>(),
                        TreeSet.class);
        Relation<Row.R2<String, String>, Row.R2<Double, String>> scriptRegions =
                Relation.of(
                        new TreeMap<Row.R2<String, String>, Set<Row.R2<Double, String>>>(),
                        TreeSet.class);
        Relation<Row.R2<String, String>, Row.R2<Double, String>> languageRegions =
                Relation.of(
                        new TreeMap<Row.R2<String, String>, Set<Row.R2<Double, String>>>(),
                        TreeSet.class);

        /**
         * Add population information. "order" is the negative of the population (makes the first be
         * the highest).
         *
         * @param language
         * @param script
         * @param region
         * @param order
         */
        void add(String language, String script, String region, Double order) {
            // check for bad codes sneaking in
            LanguageTagParser ltp =
                    new LanguageTagParser()
                            .setLanguage(language)
                            .setScript(script)
                            .setRegion(region);
            Set<String> errors = new LinkedHashSet<>();
            if (!LocaleValidator.isValid(ltp, LocaleValidator.ALLOW_IN_LIKELY, errors)) {
                System.out.println(JOIN_LS.join("Bad Add of " + ltp, errors));
            }

            if (watching(SHOW_ORDER, language))
                System.out.println(
                        JOIN_TAB.join(
                                "Add Data:", language, script, region, integer.format(order)));

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
        }
        // private void addCounter(Map<String, Counter<String>> map, String key, String key2, Double
        // count) {
        // Counter<String> counter = map.get(key);
        // if (counter == null) {
        // map.put(key, counter = new Counter<String>());
        // }
        // counter.add(key2, count.longValue());
        // }
    }

    private static long getWritingPopulation(PopulationData popData) {
        final double writingPopulation = popData.getWritingPopulation();
        if (!Double.isNaN(writingPopulation)) {
            return (long) writingPopulation;
        }
        return (long) popData.getLiteratePopulation();
    }

    private static void add(
            String key,
            String value,
            Map<String, String> toAdd,
            String kind,
            LocaleOverride override) {
        add(key, value, toAdd, kind, override, SHOW_ADD);
    }

    private static void add(
            String key,
            String value,
            Map<String, String> toAdd,
            String kind,
            LocaleOverride override,
            boolean showAction) {
        String oldValue = toAdd.get(key);
        if (oldValue == null) {
            if (watching(showAction, key, value)) {
                System.out.println(
                        JOIN_TAB.join(
                                "",
                                "Adding:",
                                key,
                                getNameSafe(key),
                                "→",
                                value,
                                getNameSafe(value),
                                "",
                                "",
                                kind));
            }
            toAdd.put(key, value);
        } else if (override != LocaleOverride.KEEP_EXISTING && !value.equals(oldValue)) {
            if (watching(showAction, key, value)) {
                System.out.println(
                        JOIN_TAB.join(
                                "",
                                "Replacing:",
                                key,
                                getNameSafe(key),
                                "→",
                                value,
                                getNameSafe(value),
                                ", was",
                                oldValue,
                                getNameSafe(oldValue),
                                kind));
            }
            toAdd.put(key, value);
        }
    }

    public static String truncateLongString(Object data, int maxLen) {
        String info = data.toString();
        if (info.length() > maxLen) {
            if (UCharacter.codePointAt(info, maxLen - 1) > 0xFFFF) {
                maxLen--;
            }
            info = info.substring(0, maxLen) + "…";
        }
        return info;
    }

    enum LsrType {
        LSR,
        LS,
        LR,
        SR,
        L,
        S,
        R
    }

    /**
     * Minimize<br>
     * We know that the following algorithm will be used in the lookup, so we remove mappings that
     * are redundant. https://cldr-smoke.unicode.org/spec/main/ldml/tr35.html#likely-subtags<br>
     * A subtag is called empty if it is a missing script or region subtag, or it is a base language
     * subtag with the value "und". In the description below, a subscript on a subtag x indicates
     * which tag it is from: xs is in the source, xm is in a match, and xr is in the final result.
     *
     * <p>Lookup. Look up each of the following in order, and stop on the first match:
     *
     * <ol>
     *   <li>languages_scripts_regions
     *   <li>languages_scripts
     *   <li>languages_regions
     *   <li>languages
     * </ol>
     *
     * <p>Return
     *
     * <p>
     *
     * <ol>
     *   <li>If there is no match, signal an error and stop.
     *   <li>Otherwise there is a match = languagem_scriptm_regionm
     *   <li>Let xr = xs if xs is neither empty nor 'und', and xm otherwise.
     *   <li>Return the language tag composed of languager_scriptr_regionr + variants + extensions.
     * </ol>
     */
    public static Map<String, String> minimize(
            Map<String, String> max, Map<String, String> itemsRemoved) {

        final LanguageTagParser sourceParser = new LanguageTagParser();
        final Map<String, String> removals = new TreeMap<>();
        final Map<String, String> toMinimize = new TreeMap<>(LOCALE_SOURCE);
        final Output<String> intermediate = new Output<>();

        toMinimize.putAll(max);

        // Remove redundant mappings.
        // For example, suppose we have the following mappings:
        // {aa=aa_Latn_ET, aa_DJ=aa_Latn_DJ, aa_ER=aa_Latn_ER}
        // Using the algorithm above if aa_DJ=aa_Latn_DJ were not there we would
        // 1. check for aa_DJ, fail
        // 2. check for aa, get aa_Latn_ET, and substitute DJ for ET, getting the right answer.
        // So aa_DJ is redundant

        // Dependencies
        // We should never have an LocaleScriptInfo.UNKNOWN_REGION, or
        // LocaleScriptInfo.UNKNOWN_SCRIPT
        // The unit tests will guarantee this if somehow we slip up
        // Similarly, we should never have the target have language="und", or be missing script or
        // region
        // We also know that the source never has 3 full fields (ie, never L≠und && S≠"" && R≠"")

        // Make multiple passes if necessary
        for (int pass = 0; ; ++pass) {
            removals.clear();
            for (Entry<String, String> entry : toMinimize.entrySet()) {
                String source = entry.getKey();
                if (source.equals("und")) {
                    continue; // never remove
                }
                String target = entry.getValue();
                if (source.equals("aa_DJ") || source.equals("und_Arab_AF")) {
                    int debug = 0;
                }
                sourceParser.set(source);

                if (!sourceParser.getLanguage().equals("und")
                        && !sourceParser.getScript().isEmpty()
                        && !sourceParser.getRegion().isEmpty()) {
                    throw new IllegalArgumentException("Bogus source: " + source);
                }

                // The following has some redundant checks, but it makes the
                // code more convoluted to catch them, and perf is not an issue.

                String trial;

                // und_Cyrl_RU => ru_Cyrl_RU, but und_Cyrl => ru_Cyrl_RU
                // und_Latn_DE => de_Latn_DE, but und_DE => de_Latn_DE
                // und_Latn_US => en_Latn_US, but und => en_Latn_US

                if (!sourceParser.getScript().isEmpty() && !sourceParser.getRegion().isEmpty()) {
                    trial =
                            compose(
                                    sourceParser.getLanguage(),
                                    sourceParser.getScript(),
                                    sourceParser.getRegion());
                    if (!trial.equals(source)) {
                        String result =
                                matchAndFill(
                                        sourceParser, trial, removals, toMinimize, intermediate);
                        if (target.equals(result)) {
                            removals.put(source, target);
                            showRemoving(LsrType.LSR, source, target, trial, intermediate.value);
                            continue;
                        }
                    }
                }

                // de_Latn => de_Latn_DE, but de => de_Latn_DE
                // und_Cyrl => ru_Cyrl_RU, but ru_Cyrl => ru_Cyrl_RU

                if (!sourceParser.getScript().isEmpty()) {
                    trial = compose(sourceParser.getLanguage(), sourceParser.getScript(), "");
                    if (!trial.equals(source)) {
                        String result =
                                matchAndFill(
                                        sourceParser, trial, removals, toMinimize, intermediate);
                        if (target.equals(result)) {
                            removals.put(source, target);
                            showRemoving(LsrType.LS, source, target, trial, intermediate.value);
                            continue;
                        }
                    }
                }

                // de_DE => de_Latn_DE, but de => de_Latn_DE
                // und_RU => ru_Cyrl_RU, but ru_RU => ru_Cyrl_RU

                if (!sourceParser.getRegion().isEmpty()) {
                    trial = compose(sourceParser.getLanguage(), "", sourceParser.getRegion());
                    if (!trial.equals(source)) {

                        String result =
                                matchAndFill(
                                        sourceParser, trial, removals, toMinimize, intermediate);
                        if (target.equals(result)
                                && !fieldChangesLanguage(
                                        LsrType.S, sourceParser, removals, toMinimize)) {
                            removals.put(source, target);
                            showRemoving(LsrType.LR, source, target, trial, intermediate.value);
                            continue;
                        }
                    }
                }

                // ultimate fallback

                if (true) {
                    trial = sourceParser.getLanguage();
                    if (!trial.equals(source)) {
                        String result =
                                matchAndFill(
                                        sourceParser, trial, removals, toMinimize, intermediate);
                        if (target.equals(result)
                                && (sourceParser.getScript().isEmpty()
                                        || sourceParser.getRegion().isEmpty()
                                        || (!fieldChangesLanguage(
                                                        LsrType.S,
                                                        sourceParser,
                                                        removals,
                                                        toMinimize)
                                                && !fieldChangesLanguage(
                                                        LsrType.R,
                                                        sourceParser,
                                                        removals,
                                                        toMinimize)))) {
                            removals.put(source, target);
                            showRemoving(LsrType.L, source, target, trial, intermediate.value);
                            continue;
                        }
                    }
                }
            }
            if (removals.size() == 0) {
                break;
            }
            itemsRemoved.putAll(removals);
            for (String locale : removals.keySet()) {
                toMinimize.remove(locale);
            }
        }
        return CldrUtility.protectCollection(toMinimize);
    }

    public static boolean fieldChangesLanguage(
            LsrType lsrType,
            final LanguageTagParser sourceParser,
            final Map<String, String> removals,
            final Map<String, String> toMinimize) {
        if (!isEmpty(sourceParser, lsrType)) {
            final LanguageTagParser tempParser = new LanguageTagParser();
            copyFrom(tempParser, LsrType.L, sourceParser);
            copyFrom(tempParser, lsrType, sourceParser);

            // Special Check!
            // Suppose we have
            // (A) und_Arab_AF => fa_Arab_AF
            // It appears we can remove (A) because we have
            // (B) und_AF ==> fa_Arab_AF
            // However, because script is checked before region
            // We will have a first have a hit on
            // (C) und_Arab => ar_Arab_xx
            // Which will result in the wrong answer (ar_Arab_AF).

            String trial2 =
                    compose(sourceParser.getLanguage(), getField(sourceParser, lsrType), "");
            String result2 = matchAndFill(sourceParser, trial2, removals, toMinimize, null);
            if (result2 != null) {
                final LanguageTagParser tempParser2 = new LanguageTagParser();
                tempParser2.set(result2);
                String lang2 = tempParser2.getLanguage();
                String tempLang = tempParser.getLanguage();
                if (tempLang != lang2) {
                    return true;
                }
            }
        }
        return false;
    }

    // Some of these would be useful on LanguageTagParser

    public static String getField(LanguageTagParser fromParser, LsrType lsr) {
        switch (lsr) {
            case L:
                return fromParser.getLanguage();
            case S:
                return fromParser.getScript();
            case R:
                return fromParser.getRegion();
            default:
                throw new IllegalArgumentException();
        }
    }

    public static LanguageTagParser copyFrom(
            LanguageTagParser intoParser, LsrType lsr, LanguageTagParser fromParser) {
        switch (lsr) {
            case L:
                intoParser.setLanguage(fromParser.getLanguage());
                break;
            case S:
                intoParser.setScript(fromParser.getScript());
                break;
            case R:
                intoParser.setRegion(fromParser.getRegion());
                break;
            default:
                throw new IllegalArgumentException();
        }
        return intoParser;
    }

    public static LanguageTagParser ifEmptyCopyFrom(
            LanguageTagParser intoParser, LsrType lsr, LanguageTagParser fromParser) {
        return isEmpty(intoParser, lsr) ? intoParser : copyFrom(intoParser, lsr, fromParser);
    }

    public static boolean isEmpty(LanguageTagParser intoParser, LsrType lsr) {
        return getField(intoParser, lsr).equals(lsr == LsrType.L ? "und" : "");
    }

    public static String matchAndFill(
            LanguageTagParser sourceParser,
            String trial,
            Map<String, String> removals,
            Map<String, String> toMinimize,
            Output<String> intermediate) {
        String possibleSuper;
        String result;
        possibleSuper = removals.containsKey(trial) ? null : toMinimize.get(trial);
        result = null;
        if (possibleSuper != null) {
            LanguageTagParser tempParser3 = new LanguageTagParser();
            tempParser3.set(possibleSuper);
            if (!sourceParser.getLanguage().equals("und")) {
                tempParser3.setLanguage(sourceParser.getLanguage());
            }
            if (!getField(sourceParser, LsrType.S).isEmpty()) {
                copyFrom(tempParser3, LsrType.S, sourceParser);
            }
            if (!sourceParser.getRegion().isEmpty()) {
                tempParser3.setRegion(sourceParser.getRegion());
            }
            result = tempParser3.toString();
        }
        if (intermediate != null) {
            intermediate.value = possibleSuper;
        }
        return result;
    }

    private static String compose(String lang, String script, String region) {
        String result = lang;
        if (!script.isEmpty()) {
            result += "_" + script;
        }
        if (!region.isEmpty()) {
            result += "_" + region;
        }
        return result;
    }

    static class MapView<K, V> {
        K skip;
    }

    public static void showRemoving(
            Object pass, String locale, String target, String fallback, String fallbackTarget) {
        if (watching(SHOW_MIN, locale, target, fallback, fallbackTarget)) {
            System.out.println(
                    JOIN_TAB.join(
                            pass, "Removing: ", locale, "→", target, fallback, fallbackTarget));
        }
    }

    public static String printingName(String locale, Joiner spacing) {
        if (locale == null) {
            return null;
        }
        CLDRLocale cLocale = CLDRLocale.getInstance(locale);
        String lang = cLocale.getLanguage();
        String script = cLocale.getScript();
        String region = cLocale.getCountry();
        return spacing.join(
                (lang.equals(LocaleNames.UND)
                        ? "?"
                        : english.getName(CLDRFile.LANGUAGE_NAME, lang)),
                (script == null || script.equals("")
                        ? "?"
                        : english.getName(CLDRFile.SCRIPT_NAME, script)),
                (region == null || region.equals("")
                        ? "?"
                        : english.getName(CLDRFile.TERRITORY_NAME, region)));
    }

    static final String SEPARATOR =
            OUTPUT_STYLE == OutputStyle.C || OUTPUT_STYLE == OutputStyle.C_ALT
                    ? CldrUtility.LINE_SEPARATOR
                    : "\t";
    static final Joiner spacing =
            Joiner.on(OUTPUT_STYLE == OutputStyle.PLAINTEXT ? "\t" : "‧").useForNull("∅");

    static final String arrow = OUTPUT_STYLE == OutputStyle.PLAINTEXT ? "\t⇒\t" : "\t➡ ";

    private static File printLikelySubtags(Map<String, String> fluffup) throws IOException {
        final File genDir = new File(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        final File genFile =
                new File(
                        genDir,
                        "likelySubtags" + (OUTPUT_STYLE == OutputStyle.XML ? ".xml" : ".txt"));
        System.out.println("Writing to " + genFile);

        // set based on above
        try (PrintWriter out = FileUtilities.openUTF8Writer(genFile)) {
            String header =
                    OUTPUT_STYLE != OutputStyle.XML
                            ? "const MapToMaximalSubtags default_subtags[] = {"
                            : JOIN_LS.join(
                                    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>",
                                    "<!DOCTYPE supplementalData SYSTEM \"../../common/dtd/ldmlSupplemental.dtd\">",
                                    "<!--",
                                    CldrUtility.getCopyrightString(),
                                    "-->",
                                    "<!--",
                                    "Likely subtags data is generated programatically from CLDR's language/territory/population",
                                    "data using the GenerateMaximalLocales tool. Under normal circumstances this file should",
                                    "not be patched by hand, as any changes made in that fashion may be lost.",
                                    "-->",
                                    "<supplementalData>",
                                    "    <version number=\"$" + "Revision$\"/>",
                                    "    <likelySubtags>");
            String footer =
                    OUTPUT_STYLE != OutputStyle.XML
                            ? SEPARATOR + "};"
                            : "    </likelySubtags>"
                                    + CldrUtility.LINE_SEPARATOR
                                    + "</supplementalData>";
            out.println(header);
            boolean first = true;
            printLine(fluffup, Map.of(), first, out);

            if (OUTPUT_STYLE == OutputStyle.XML) {
                out.println("       <!-- Data donated by SIL -->");
            }

            // Now add from silData
            // filter to only languages that are not already in
            Map<String, String> silMap = new HashMap<>();
            Map<String, String> silOrigins = new HashMap<>();

            for (Entry<String, LSRSource> entry : silData.entrySet()) {
                CLDRLocale source = CLDRLocale.getInstance(entry.getKey());
                String lang = source.getLanguage();
                if (!fluffup.containsKey(lang)) {
                    silMap.put(entry.getKey(), entry.getValue().getLsrString());
                    if (!entry.getValue().getSources().isEmpty()) {
                        silOrigins.put(entry.getKey(), entry.getValue().getSourceString());
                    }
                }
            }
            printLine(silMap, silOrigins, first, out);

            out.println(footer);
            out.close();
        }
        return genFile;
    }

    public static void printLine(
            Map<String, String> toPrint,
            Map<String, String> origins,
            boolean first,
            PrintWriter out) {
        Set<String> keys = new TreeSet<>(LOCALE_SOURCE);
        keys.addAll(toPrint.keySet());
        boolean noUndYet = true;
        for (String printingLocale : keys) {
            String printingTarget = toPrint.get(printingLocale);
            String origin = origins.get(printingLocale);
            String comment =
                    printingName(printingLocale, spacing)
                            + arrow
                            + printingName(printingTarget, spacing);

            if (OUTPUT_STYLE == OutputStyle.XML) {
                if (noUndYet) {
                    if (printingLocale.startsWith("und")) {
                        noUndYet = false;
                        out.println(
                                "       <!-- Data to find likely language; some implementations may omit -->");
                    }
                }
                out.println(
                        "\t\t<likelySubtag from=\""
                                + printingLocale
                                + "\" to=\""
                                + printingTarget
                                + "\""
                                + (origin == null ? "" : " origin=\"" + origin + "\"")
                                + "/>"
                                + "\t\t"
                                + "<!--"
                                + comment
                                + "-->");
            } else {
                if (first) {
                    first = false;
                } else {
                    out.print(",");
                }
                if (comment.length() > 70 && SEPARATOR.equals(CldrUtility.LINE_SEPARATOR)) {
                    comment =
                            printingName(printingLocale, spacing)
                                    + SEPARATOR
                                    + "    // "
                                    + arrow
                                    + printingName(printingTarget, spacing);
                }
                out.print(
                        "  {"
                                + SEPARATOR
                                + "    // "
                                + comment
                                + SEPARATOR
                                + "    \""
                                + printingLocale
                                + "\","
                                + SEPARATOR
                                + "    \""
                                + printingTarget
                                + "\""
                                + CldrUtility.LINE_SEPARATOR
                                + "  }");
            }
        }
    }
}
