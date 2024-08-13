package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.NumberFormat;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.GenerateMaximalLocales.LocaleOverride;
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
    private static final Joiner JOIN_TAB = Joiner.on('\t').useForNull("∅");

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();

    private static final Map<String, Status> LANGUAGE_CODE_TO_STATUS =
            Validity.getInstance().getCodeToStatus(LstrType.language);

    private static final String TEMP_UNKNOWN_REGION = "XZ";

    private static final String DEBUG_ADD_KEY = "und_Latn_ZA";

    private static final boolean SHOW_ADD =
            CldrUtility.getProperty("GenerateLikelySubtags_Debug", false);
    private static final boolean SUPPRESS_CHANGES =
            CldrUtility.getProperty("GenerateMaximalLocalesSuppress", false);
    private static final boolean SHOW_CONTAINERS = false;

    private static final boolean SHOW_ALL_LANGUAGE_CODES = false;
    private static final boolean SHOW_DETAILED = false;
    private static final boolean SHOW_INCLUDED_EXCLUDED = false;

    private static final double MIN_UNOFFICIAL_LANGUAGE_SIZE = 10000000;
    private static final double MIN_UNOFFICIAL_LANGUAGE_PROPORTION = 0.20;
    private static final double MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE = 100000;
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
    private static NumberFormat number = NumberFormat.getIntegerInstance();

    private static boolean DROP = false;

    static {
        for (CLDRLocale locale :
                ToolConfig.getToolInstance().getCldrFactory().getAvailableCLDRLocales()) {
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
            DROP ? List.of() : List.of("und_Arab_PK", "und_Latn_ET", "hi_Latn");

    private static final ImmutableSet<String> deprecatedISONotInLST =
            DROP ? ImmutableSet.of() : ImmutableSet.of("scc", "scr");

    /**
     * This is the simplest way to override, by supplying the max value. It gets a very low weight,
     * so doesn't override any stronger value.
     */
    private static final List<String> MAX_ADDITIONS =
            DROP
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
                            "clc_Latn_CA",
                            "crg_Latn_CA",
                            "hur_Latn_CA",
                            "kwk_Latn_CA",
                            "lil_Latn_CA",
                            "ojs_Cans_CA",
                            "oka_Latn_CA",
                            "pqm_Latn_CA",
                            "hi_Latn_IN",
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
                    DROP
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
                                {"swc", "swc_Latn_CD"},
                                {"ti", "ti_Ethi_ET"},
                                {"ti_Ethi", "ti_Ethi_ET"},
                                {LocaleNames.UND, "en_Latn_US"},
                                {"und_Adlm", "ff_Adlm_GN"},
                                {"und_Adlm_GN", "ff_Adlm_GN"},
                                {"und_Arab", "ar_Arab_EG"},
                                {"und_Arab_PK", "ur_Arab_PK"},
                                {"und_Bopo", "zh_Bopo_TW"},
                                {"und_Deva_FJ", "hif_Deva_FJ"},
                                {"und_EZ", "de_Latn_EZ"},
                                {"und_Hani", "zh_Hani_CN"},
                                {"und_Hani_CN", "zh_Hani_CN"},
                                {"und_Kana", "ja_Kana_JP"},
                                {"und_Kana_JP", "ja_Kana_JP"},
                                {"und_Latn", "en_Latn_US"},
                                {"und_001", "en_Latn_US"}, // to not be overridden by tok_Latn_001
                                {
                                    "und_Latn_001", "en_Latn_US"
                                }, // to not be overridden by tok_Latn_001
                                {"und_Latn_ET", "en_Latn_ET"},
                                {"und_Latn_NE", "ha_Latn_NE"},
                                {"und_Latn_PH", "fil_Latn_PH"},
                                {"und_ML", "bm_Latn_ML"},
                                {"und_Latn_ML", "bm_Latn_ML"},
                                {"und_MU", "mfe_Latn_MU"},
                                {"und_NE", "ha_Latn_NE"},
                                {"und_PH", "fil_Latn_PH"},
                                {"und_PK", "ur_Arab_PK"},
                                {"und_SO", "so_Latn_SO"},
                                {"und_SS", "en_Latn_SS"},
                                {"und_TK", "tkl_Latn_TK"},
                                {"und_UN", "en_Latn_UN"},
                                {"und_005", "pt_Latn_BR"},
                                {"vo", "vo_Latn_001"},
                                {"vo_Latn", "vo_Latn_001"},
                                {"yi", "yi_Hebr_001"},
                                {"yi_Hebr", "yi_Hebr_001"},
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
                                {"grc_Linb", "grc_Linb_GR"},
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
                                //        { "mis_Medf", "mis_Medf_NG" },

                                {"ku_Yezi", "ku_Yezi_GE"},
                                {"und_EU", "en_Latn_IE"},
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
        {LocaleNames.UND, "Latn"}, // Ultimate fallback
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

    public static void main(String[] args) {
        Map<String, String> old = supplementalData.getLikelySubtags();
        Map<String, String> oldOrigins = supplementalData.getLikelyOrigins();
        System.out.println("origins: " + new TreeSet<>(oldOrigins.values()));

        Map<String, String> toMaximized = generatePopulationData(new TreeMap<>(LOCALE_SOURCE));

        Map<String, String> result = minimize(toMaximized);

        Set<String> newAdditions = new TreeSet();
        Set<String> newMissing = new TreeSet();

        System.out.println(JOIN_TAB.join("Source", "Name", "oldValue", "Name", "newValue", "Name"));

        Set<String> sorted = new TreeSet<>(LOCALE_SOURCE);
        sorted.addAll(result.keySet());
        sorted.addAll(old.keySet());

        for (String source : sorted) {
            String oldValue = old.get(source);
            String newValue = result.get(source);
            if (Objects.equal(oldValue, newValue)) {
                continue;
            }
            final String origins = oldOrigins.get(source);
            if (origins != null && origins.contains("sil1")) {
                continue; // skip for now
            }
            if (oldValue == null) {
                continue; // skip for now
            }
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
    }

    /**
     * Compare locales, first by count of components (handling und), then by language, script, and
     * finally region
     */
    static Comparator<String> LOCALE_SOURCE =
            new Comparator<>() {

                @Override
                public int compare(String locale1, String locale2) {
                    CLDRLocale l1 = CLDRLocale.getInstance(locale1);
                    CLDRLocale l2 = CLDRLocale.getInstance(locale2);
                    // sort items with 0 components first, then 1, then 2 (there won't be 3)
                    int result =
                            ComparisonChain.start()
                                    // .compare(getCount(l1), getCount(l2))
                                    .compare(fixUnd(l1.getLanguage()), fixUnd(l2.getLanguage()))
                                    .compare(l1.getScript(), l2.getScript())
                                    .compare(l1.getCountry(), l2.getCountry())
                                    .result();
                    if (result == 0 && !locale1.equals(locale2)) {
                        throw new IllegalArgumentException();
                    }
                    return result;
                }

                private int getCount(CLDRLocale l1) {
                    int result =
                            ("und".equals(l1.getLanguage()) ? 0 : 1)
                                    + (l1.getScript().isEmpty() ? 0 : 1)
                                    + (l1.getCountry().isEmpty() ? 0 : 1);
                    return result;
                }

                private String fixUnd(String language) {
                    return "und".equals(language) ? "" : language;
                }
            };

    static {
        LOCALE_SOURCE.compare("hnj_MM", "hnj_Laoo");
    }

    public static String getNameSafe(String oldValue) {
        try {
            return english.getName(oldValue);
        } catch (Exception e) {
            return "n/a";
        }
    }

    private static Map<String, String> generatePopulationData(Map<String, String> toMaximized) {
        // we are going to try a different approach.
        // first gather counts for maximized values
        // Set<Row.R3<String,String,String>,Double> rowsToCounts = new TreeMap();
        MaxData maxData = new MaxData();
        Set<String> cldrLocales = factory.getAvailable();
        Set<String> otherTerritories =
                new TreeSet<>(standardCodes.getGoodAvailableCodes("territory"));

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
                    if (literatePopulation >= minimalLiteratePopulation) {
                        // ok, skip
                    } else if (literatePopulation >= MIN_UNOFFICIAL_CLDR_LANGUAGE_SIZE
                            && cldrLocales.contains(locale)) {
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
                        System.out.println(
                                "Retaining\t"
                                        + writtenLanguage
                                        + "\t"
                                        + region
                                        + "\t"
                                        + getNameSafe(locale)
                                        + "\t"
                                        + number.format(literatePopulation)
                                        + "\t"
                                        + percent.format(
                                                literatePopulation / literateTerritoryPopulation)
                                        + (cldrLocales.contains(locale) ? "\tin-CLDR" : ""));
                }

                String script = localeToScriptCache.get(writtenLanguage);
                if (script == null) {
                    script = LocaleScriptInfo.getScriptFromLocaleOrSupplemental(writtenLanguage);
                    if (script == null) {
                        noPopulationData.add(writtenLanguage);
                        continue;
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

        for (Entry<String, Collection<String>> entry :
                DeriveScripts.getLanguageToScript().asMap().entrySet()) {
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
            String goodLanguage = replacements.get(0);

            String badLanguage = str.getKey();
            if (badLanguage.contains("_")) {
                continue;
            }
            if (deprecatedISONotInLST.contains(badLanguage)) {
                continue;
            }
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
            final Comparable<String> script = value.get1();
            final Comparable<String> region = value.get2();
            add(
                    language,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "L->SR",
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
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
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
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
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
        }

        for (String script : maxData.scripts.keySet()) {
            R3<Double, String, String> value = maxData.scripts.getAll(script).iterator().next();
            final Comparable<String> language = value.get1();
            final Comparable<String> region = value.get2();
            add(
                    "und_" + script,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "S->LR",
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
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
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
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
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
        }

        for (String region : maxData.regions.keySet()) {
            R3<Double, String, String> value = maxData.regions.getAll(region).iterator().next();
            final Comparable<String> language = value.get1();
            final Comparable<String> script = value.get2();
            add(
                    "und_" + region,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "R->LS",
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
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
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
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
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
        }

        for (Entry<String, Counter<R2<String, String>>> containerAndInfo :
                maxData.containersToLanguage.entrySet()) {
            String region = containerAndInfo.getKey();
            if (region.equals("001")) {
                continue;
            }
            Counter<R2<String, String>> data = containerAndInfo.getValue();
            Set<R2<String, String>> keysetSortedByCount = data.getKeysetSortedByCount(true);
            if (SHOW_CONTAINERS) { // debug
                System.out.println(
                        "Container2L:\t"
                                + region
                                + "\t"
                                + truncateLongString(
                                        data.getEntrySetSortedByCount(true, null), 127));
                System.out.println(
                        "Container2LR:\t"
                                + region
                                + "\t"
                                + maxData.containersToLangRegion.get(region));
            }
            R2<String, String> value =
                    keysetSortedByCount.iterator().next(); // will get most negative
            final Comparable<String> language = value.get0();
            final Comparable<String> script = value.get1();

            // fix special cases like es-419, where a locale exists.
            // for those cases, what we add as output is the container. Otherwise the region.
            Set<String> skipLanguages = cldrContainerToLanguages.get(region);
            if (skipLanguages != null && skipLanguages.contains(language)) {
                add(
                        "und_" + region,
                        language + "_" + script + "_" + region,
                        toMaximized,
                        "R*->LS",
                        LocaleOverride.REPLACE_EXISTING,
                        SHOW_ADD);
                continue;
            }

            // we now have the best language and script. Find the best region for that
            for (R4<Double, String, String, String> e :
                    maxData.containersToLangRegion.get(region)) {
                final Comparable<String> language2 = e.get1();
                final Comparable<String> script2 = e.get2();
                if (language2.equals(language) && script2.equals(script)) {
                    add(
                            "und_" + region,
                            language + "_" + script + "_" + e.get3(),
                            toMaximized,
                            "R*->LS",
                            LocaleOverride.REPLACE_EXISTING,
                            SHOW_ADD);
                    break;
                }
            }
        }

        for (R2<String, String> languageScript : maxData.languageScripts.keySet()) {
            R2<Double, String> value =
                    maxData.languageScripts.getAll(languageScript).iterator().next();
            final Comparable<String> language = languageScript.get0();
            final Comparable<String> script = languageScript.get1();
            final Comparable<String> region = value.get1();
            add(
                    language + "_" + script,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "LS->R",
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
        }

        for (R2<String, String> scriptRegion : maxData.scriptRegions.keySet()) {
            R2<Double, String> value = maxData.scriptRegions.getAll(scriptRegion).iterator().next();
            final Comparable<String> script = scriptRegion.get0();
            final Comparable<String> region = scriptRegion.get1();
            final Comparable<String> language = value.get1();
            add(
                    "und_" + script + "_" + region,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "SR->L",
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
        }

        for (R2<String, String> languageRegion : maxData.languageRegions.keySet()) {
            R2<Double, String> value =
                    maxData.languageRegions.getAll(languageRegion).iterator().next();
            final Comparable<String> language = languageRegion.get0();
            final Comparable<String> region = languageRegion.get1();
            final Comparable<String> script = value.get1();
            add(
                    language + "_" + region,
                    language + "_" + script + "_" + region,
                    toMaximized,
                    "LR->S",
                    LocaleOverride.REPLACE_EXISTING,
                    SHOW_ADD);
        }

        // get the script info from metadata as fallback

        TreeSet<String> sorted = new TreeSet<>(ScriptMetadata.getScripts());
        for (String script : sorted) {
            Info i = ScriptMetadata.getInfo(script);
            String likelyLanguage = i.likelyLanguage;
            if (LANGUAGE_CODE_TO_STATUS.get(likelyLanguage) == Status.special) {
                likelyLanguage = LocaleNames.UND;
            }
            String originCountry = i.originCountry;
            final String result = likelyLanguage + "_" + script + "_" + originCountry;
            add(
                    "und_" + script,
                    result,
                    toMaximized,
                    "S->LR•",
                    LocaleOverride.KEEP_EXISTING,
                    SHOW_ADD);
            add(
                    likelyLanguage,
                    result,
                    toMaximized,
                    "L->SR•",
                    LocaleOverride.KEEP_EXISTING,
                    SHOW_ADD);
        }

        // add overrides
        for (String key : LANGUAGE_OVERRIDES.keySet()) {
            add(
                    key,
                    LANGUAGE_OVERRIDES.get(key),
                    toMaximized,
                    "OVERRIDE",
                    LocaleOverride.REPLACE_EXISTING,
                    true);
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
        return toMaximized;
    }

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

        Map<String, Counter<Row.R2<String, String>>> containersToLanguage = new TreeMap<>();
        Relation<String, Row.R4<Double, String, String, String>> containersToLangRegion =
                Relation.of(
                        new TreeMap<String, Set<Row.R4<Double, String, String, String>>>(),
                        TreeSet.class);

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
            if (SHOW_ADD && language.equals(LocaleNames.MIS)) {
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
                        containersToLanguage.put(container, data = new Counter<>());
                    }
                    data.add(Row.of(language, script), (long) (double) order);
                }
            }

            if (SHOW_ADD)
                System.out.println(
                        "Data:\t" + language + "\t" + script + "\t" + region + "\t" + order);
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

    private static String getName(String value) {
        return ConvertLanguageData.getLanguageCodeAndName(value);
    }

    private static void add(
            String key,
            String value,
            Map<String, String> toAdd,
            String kind,
            LocaleOverride override,
            boolean showAction) {
        if (SHOW_ADD && key.startsWith(LocaleNames.MIS)) {
            int debug = 1;
        }
        if (key.equals(DEBUG_ADD_KEY)) {
            System.out.println("*debug*");
        }
        String oldValue = toAdd.get(key);
        if (oldValue == null) {
            if (showAction) {
                System.out.println(
                        "\tAdding:\t\t"
                                + getName(key)
                                + "\t=>\t"
                                + getName(value)
                                + "\t\t\t\t"
                                + kind);
            }
        } else if (override == LocaleOverride.KEEP_EXISTING || value.equals(oldValue)) {
            // if (showAction) {
            // System.out.println("Skipping:\t" + key + "\t=>\t" + value + "\t\t\t\t" + kind);
            // }
            return;
        } else {
            if (showAction) {
                System.out.println(
                        "\tReplacing:\t"
                                + getName(key)
                                + "\t=>\t"
                                + getName(value)
                                + "\t, was\t"
                                + getName(oldValue)
                                + "\t\t"
                                + kind);
            }
        }
        toAdd.put(key, value);
    }

    public static String truncateLongString(Object data, int maxLen) {
        String info = data.toString();
        if (info.length() > maxLen) {
            info = info.substring(0, maxLen) + "…";
            // TODO, handle supplemental characters.
        }
        return info;
    }

    public static Map<String, String> minimize(Map<String, String> fluffup) {
        LanguageTagParser parser = new LanguageTagParser();
        LanguageTagParser targetParser = new LanguageTagParser();
        Set<String> removals = new TreeSet<>();
        while (true) {
            removals.clear();
            for (String locale : fluffup.keySet()) {
                String target = fluffup.get(locale);
                if (targetParser.set(target).getRegion().equals(LocaleScriptInfo.UNKNOWN_REGION)) {
                    removals.add(locale);
                    if (SHOW_ADD)
                        System.out.println(
                                "Removing:\t"
                                        + getName(locale)
                                        + "\t=>\t"
                                        + getName(target)
                                        + "\t\t - Unknown Region in target");
                    continue;
                }
                if (targetParser.getScript().equals(LocaleScriptInfo.UNKNOWN_SCRIPT)) {
                    removals.add(locale);
                    if (SHOW_ADD)
                        System.out.println(
                                "Removing:\t"
                                        + getName(locale)
                                        + "\t=>\t"
                                        + getName(target)
                                        + "\t\t - Unknown Script in target");
                    continue;
                }

                String region = parser.set(locale).getRegion();
                if (region.length() != 0) {
                    if (region.equals(LocaleScriptInfo.UNKNOWN_REGION)) {
                        removals.add(locale);
                        if (SHOW_ADD)
                            System.out.println(
                                    "Removing:\t"
                                            + getName(locale)
                                            + "\t=>\t"
                                            + getName(target)
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
                                System.out.println(
                                        "Removing:\t"
                                                + locale
                                                + "\t=>\t"
                                                + target
                                                + "\t\tRedundant with "
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
                    if (script.equals(LocaleScriptInfo.UNKNOWN_SCRIPT)) {
                        removals.add(locale);
                        if (SHOW_ADD)
                            System.out.println(
                                    "Removing:\t"
                                            + locale
                                            + "\t=>\t"
                                            + target
                                            + "\t\t - Unknown Script");
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
                                System.out.println(
                                        "Removing:\t"
                                                + locale
                                                + "\t=>\t"
                                                + target
                                                + "\t\tRedundant with "
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
        return fluffup;
    }
}
