/** */
package org.unicode.cldr.surveydriver;

/** Data for SurveyDriver: lists of locales and pages */
public class SurveyDriverData {

    /**
     * This list of page names was created by temporarily commenting out the toString function in
     * PathHeader.java, and inserting this line into the PageId initialization function:
     * System.out.println("PageId raw name: " + this.toString());
     *
     * <p>It would be better to have an API for requesting this list from the ST back end at
     * runtime!
     *
     * <p>There is a PageId enum defined in PathHeader.java. We could link with the cldr-apps code
     * and access that enum directly. However, there are difficulties with initiation, like
     * "java.lang.RuntimeException: CLDRConfigImpl used before SurveyMain.init() called!" "Set
     * -DCLDR_ENVIRONMENT=UNITTEST if you are in the test cases." Follow up on that possibility
     * later. In the meantime, we can copy and simplify the enum from PathHeader.java, since all we
     * need here is an array of strings.
     *
     * <p>PageId versus SectionId: PageId.Alphabetic_Information is in the section
     * SectionId.Core_Data
     *
     * <p>Alphabetic_Information(SectionId.Core_Data, "Alphabetic Information")
     *
     * <p>Each page is one web page; a section may encompass multiple pages, not all visible at
     * once. (There may also be multiple headers in one page. See PathHeader.txt which is a source
     * file.)
     */
    private static final String[] pages = {
        // spotless:off
        "Alphabetic_Information", "Numbering_Systems", "LinguisticElements", "Locale_Name_Patterns",
        "Languages_A_D", "Languages_E_J", "Languages_K_N", "Languages_O_S", "Languages_T_Z", "Scripts",
        "Territories", "T_NAmerica", "T_SAmerica", "T_Africa", "T_Europe", "T_Asia", "T_Oceania", "Locale_Variants",
        "Keys", "Fields", "Gregorian", "Generic", "Buddhist", "Chinese", "Coptic", "Dangi", "Ethiopic",
        "Ethiopic_Amete_Alem", "Hebrew", "Indian", "Islamic", "Japanese", "Persian", "Minguo",
        "Timezone_Display_Patterns", "NAmerica", "SAmerica", "Africa", "Europe", "Russia", "WAsia", "CAsia",
        "EAsia", "SAsia", "SEAsia", "Australasia", "Antarctica", "Oceania", "UnknownT", "Overrides", "Symbols",
        "MinimalPairs", "Number_Formatting_Patterns", "Compact_Decimal_Formatting",
        "Compact_Decimal_Formatting_Other", "Measurement_Systems", "Duration", "Length", "Area", "Volume",
        "SpeedAcceleration", "MassWeight", "EnergyPower", "ElectricalFrequency", "Weather", "Digital",
        "Coordinates", "OtherUnits", "CompoundUnits", "Displaying_Lists", "LinguisticElements", "Transforms",
        // "Identity", // what is "Identity"? Leads to error message.
        // "Version", // ditto
        // "Suppress",
        // "Deprecated",
        // "Unknown",
        "C_NAmerica", "C_SAmerica", "C_NWEurope", "C_SEEurope", "C_NAfrica", "C_WAfrica", "C_MAfrica", "C_EAfrica",
        "C_SAfrica", "C_WAsia", "C_CAsia", "C_EAsia", "C_SAsia", "C_SEAsia", "C_Oceania", "C_Unknown",
        "u_Extension", "t_Extension", "Alias", "IdValidity", "Locale", "RegionMapping", "WZoneMapping", "Transform",
        "Units", "Likely", "LanguageMatch", "TerritoryInfo", "LanguageInfo", "LanguageGroup", "Fallback",
        "Gender", "Metazone", "NumberSystem", "Plural", "PluralRange", "Containment", "Currency", "Calendar",
        "WeekData", "Measurement", "Language", "RBNF", "Segmentation", "DayPeriod", "Category", "Smileys", "People",
        "Animals_Nature", "Food_Drink", "Travel_Places", "Activities", "Objects", "Symbols2", "Flags", "Component",
        "Typography",
        // spotless:on
    };

    /** These are pages for which special annotation vote resolution is applicable */
    private static final String[] annotationPages = {
        // spotless:off
        "Category", "Smileys", "People", "Animals_Nature", "Food_Drink",
        "Travel_Places", "Activities", "Objects", "Symbols2", "Flags",
        // spotless:on
    };

    /**
     * This list of locales was obtained by putting a breakpoint on SurveyAjax.getLocalesSet,
     * getting its return value, and adding quotation marks by search/replace.
     */
    private static final String[] locales = {
        // spotless:off
        "aa", "aa_DJ", "aa_ER", "aa_ET", "af", "af_NA", "af_ZA", "agq", "agq_CM",
        "ak",
        "ak_GH", "am", "am_ET", "ar", "ar_001", "ar_AE", "ar_BH", "ar_DJ", "ar_DZ", "ar_EG", "ar_EH", "ar_ER",
        "ar_IL", "ar_IQ", "ar_JO", "ar_KM", "ar_KW", "ar_LB", "ar_LY", "ar_MA", "ar_MR", "ar_OM", "ar_PS", "ar_QA",
        "ar_SA", "ar_SD", "ar_SO", "ar_SS", "ar_SY", "ar_TD", "ar_TN", "ar_YE", "arn", "arn_CL", "as", "as_IN",
        "asa", "asa_TZ", "ast", "ast_ES", "az", "az_Arab", "az_Arab_IQ", "az_Arab_IR", "az_Arab_TR", "az_Cyrl",
        "az_Cyrl_AZ", "az_Latn", "az_Latn_AZ", "ba", "ba_RU", "bas", "bas_CM", "be", "be_BY", "bem", "bem_ZM",
        "bez", "bez_TZ", "bg", "bg_BG", "bgn", "bgn_AE", "bgn_AF", "bgn_IR", "bgn_OM", "bgn_PK", "blt", "blt_VN",
        "bm", "bm_ML", "bm_Nkoo", "bm_Nkoo_ML", "bn", "bn_BD", "bn_IN", "bo", "bo_CN", "bo_IN", "br", "br_FR",
        "brx", "brx_IN", "bs", "bs_Cyrl", "bs_Cyrl_BA", "bs_Latn", "bs_Latn_BA", "bss", "bss_CM", "byn", "byn_ER",
        "ca", "ca_AD", "ca_ES", "ca_ES_VALENCIA", "ca_FR", "ca_IT", "cch", "cch_NG", "ccp", "ccp_BD", "ccp_IN",
        "ce", "ce_RU", "ceb", "cgg", "cgg_UG", "chr", "chr_US", "ckb", "ckb_IQ", "ckb_IR", "co", "co_FR", "cs",
        "cs_CZ", "cu", "cu_RU", "cv", "cv_RU", "cy", "cy_GB", "da", "da_DK", "da_GL", "dav", "dav_KE", "de",
        "de_AT", "de_BE", "de_CH", "de_DE", "de_IT", "de_LI", "de_LU", "dje", "dje_NE", "dsb", "dsb_DE", "dua",
        "dua_CM", "dv", "dv_MV", "dyo", "dyo_SN", "dz", "dz_BT", "ebu", "ebu_KE", "ee", "ee_GH", "ee_TG", "el",
        "el_CY", "el_GR", "el_POLYTON", "en", "en_001", "en_150", "en_AG", "en_AI", "en_AS", "en_AT", "en_AU",
        "en_BB", "en_BE", "en_BI", "en_BM", "en_BS", "en_BW", "en_BZ", "en_CA", "en_CC", "en_CH", "en_CK", "en_CM",
        "en_CX", "en_CY", "en_DE", "en_DG", "en_DK", "en_DM", "en_Dsrt", "en_Dsrt_US", "en_ER", "en_FI", "en_FJ",
        "en_FK", "en_FM", "en_GB", "en_GD", "en_GG", "en_GH", "en_GI", "en_GM", "en_GU", "en_GY", "en_HK", "en_IE",
        "en_IL", "en_IM", "en_IN", "en_IO", "en_JE", "en_JM", "en_KE", "en_KI", "en_KN", "en_KY", "en_LC", "en_LR",
        "en_LS", "en_MG", "en_MH", "en_MO", "en_MP", "en_MS", "en_MT", "en_MU", "en_MW", "en_MY", "en_NA", "en_NF",
        "en_NG", "en_NL", "en_NR", "en_NU", "en_NZ", "en_PG", "en_PH", "en_PK", "en_PN", "en_PR", "en_PW", "en_RW",
        "en_SB", "en_SC", "en_SD", "en_SE", "en_SG", "en_SH", "en_SI", "en_SL", "en_SS", "en_SX", "en_SZ",
        "en_Shaw", "en_TC", "en_TK", "en_TO", "en_TT", "en_TV", "en_TZ", "en_UG", "en_UM", "en_US", "en_US_POSIX",
        "en_VC", "en_VG", "en_VI", "en_VU", "en_WS", "en_ZA", "en_ZM", "en_ZW", "en_ZZ", "eo", "eo_001", "es",
        "es_419", "es_AR", "es_BO", "es_BR", "es_BZ", "es_CL", "es_CO", "es_CR", "es_CU", "es_DO", "es_EA", "es_EC",
        "es_ES", "es_GQ", "es_GT", "es_HN", "es_IC", "es_MX", "es_NI", "es_PA", "es_PE", "es_PH", "es_PR", "es_PY",
        "es_SV", "es_US", "es_UY", "es_VE", "et", "et_EE", "eu", "eu_ES", "ewo", "ewo_CM", "fa", "fa_AF", "fa_IR",
        "ff", "ff_Adlm", "ff_Adlm_BF", "ff_Adlm_CM", "ff_Adlm_GH", "ff_Adlm_GM", "ff_Adlm_GN", "ff_Adlm_GW",
        "ff_Adlm_LR", "ff_Adlm_MR", "ff_Adlm_NE", "ff_Adlm_NG", "ff_Adlm_SL", "ff_Adlm_SN", "ff_Latn", "ff_Latn_BF",
        "ff_Latn_CM", "ff_Latn_GH", "ff_Latn_GM", "ff_Latn_GN", "ff_Latn_GW", "ff_Latn_LR", "ff_Latn_MR",
        "ff_Latn_NE", "ff_Latn_NG", "ff_Latn_SL", "ff_Latn_SN", "fi", "fi_FI", "fil", "fil_PH", "fo", "fo_DK",
        "fo_FO", "fr", "fr_BE", "fr_BF", "fr_BI", "fr_BJ", "fr_BL", "fr_CA", "fr_CD", "fr_CF", "fr_CG", "fr_CH",
        "fr_CI", "fr_CM", "fr_DJ", "fr_DZ", "fr_FR", "fr_GA", "fr_GF", "fr_GN", "fr_GP", "fr_GQ", "fr_HT", "fr_KM",
        "fr_LU", "fr_MA", "fr_MC", "fr_MF", "fr_MG", "fr_ML", "fr_MQ", "fr_MR", "fr_MU", "fr_NC", "fr_NE", "fr_PF",
        "fr_PM", "fr_RE", "fr_RW", "fr_SC", "fr_SN", "fr_SY", "fr_TD", "fr_TG", "fr_TN", "fr_VU", "fr_WF", "fr_YT",
        "fur", "fur_IT", "fy", "fy_NL", "ga", "ga_IE", "gaa", "gaa_GH", "gd", "gd_GB", "gez", "gez_ER", "gez_ET",
        "gl", "gl_ES", "gn", "gn_PY", "gsw", "gsw_CH", "gsw_FR", "gsw_LI", "gu", "gu_IN", "guz", "guz_KE", "gv",
        "gv_IM", "ha", "ha_Arab", "ha_Arab_NG", "ha_Arab_SD", "ha_GH", "ha_NE", "ha_NG", "haw", "haw_US", "he",
        "he_IL", "hi", "hi_IN", "hr", "hr_BA", "hr_HR", "hsb", "hsb_DE", "hu", "hu_HU", "hy", "hy_AM", "ia",
        "ia_001", "id", "id_ID", "ig", "ig_NG", "ii", "ii_CN", "io", "io_001", "is", "is_IS", "it", "it_CH",
        "it_IT", "it_SM", "it_VA", "iu", "iu_CA", "iu_Latn", "iu_Latn_CA", "ja", "ja_JP", "jbo", "jbo_001", "jgo",
        "jgo_CM", "jmc", "jmc_TZ", "jv", "jv_ID", "ka", "ka_GE", "kab", "kab_DZ", "kaj", "kaj_NG", "kam", "kam_KE",
        "kcg", "kcg_NG", "kde", "kde_TZ", "kea", "kea_CV", "ken", "ken_CM", "khq", "khq_ML", "ki", "ki_KE", "kk",
        "kk_KZ", "kkj", "kkj_CM", "kl", "kl_GL", "kln", "kln_KE", "km", "km_KH", "kn", "kn_IN", "ko", "ko_KP",
        "ko_KR", "kok", "kok_IN", "kpe", "kpe_GN", "kpe_LR", "ks", "ks_IN", "ksb", "ksb_TZ", "ksf", "ksf_CM", "ksh",
        "ksh_DE", "ku", "ku_TR", "kw", "kw_GB", "ky", "ky_KG", "lag", "lag_TZ", "lb", "lb_LU", "lg", "lg_UG", "lkt",
        "lkt_US", "ln", "ln_AO", "ln_CD", "ln_CF", "ln_CG", "lo", "lo_LA", "lrc", "lrc_IQ", "lrc_IR", "lt", "lt_LT",
        "lu", "lu_CD", "luo", "luo_KE", "luy", "luy_KE", "lv", "lv_LV", "mas", "mas_KE", "mas_TZ", "mer", "mer_KE",
        "mfe", "mfe_MU", "mg", "mg_MG", "mgh", "mgh_MZ", "mgo", "mgo_CM", "mi", "mi_NZ", "mk", "mk_MK", "ml",
        "ml_IN", "mn", "mn_MN", "mn_Mong", "mn_Mong_CN", "mn_Mong_MN", "mni", "mni_IN", "moh", "moh_CA", "mr",
        "mr_IN", "ms", "ms_Arab", "ms_Arab_BN", "ms_Arab_MY", "ms_BN", "ms_MY", "ms_SG", "mt", "mt_MT", "mua",
        "mua_CM", "my", "my_MM", "myv", "myv_RU", "mzn", "mzn_IR", "naq", "naq_NA", "nb", "nb_NO", "nb_SJ", "nd",
        "nd_ZW", "nds", "nds_DE", "nds_NL", "ne", "ne_IN", "ne_NP", "nl", "nl_AW", "nl_BE", "nl_BQ", "nl_CW",
        "nl_NL", "nl_SR", "nl_SX", "nmg", "nmg_CM", "nn", "nn_NO", "nnh", "nnh_CM", "nqo", "nqo_GN", "nr", "nr_ZA",
        "nso", "nso_ZA", "nus", "nus_SS", "ny", "ny_MW", "nyn", "nyn_UG", "oc", "oc_FR", "om", "om_ET", "om_KE",
        "or", "or_IN", "os", "os_GE", "os_RU", "pa", "pa_Arab", "pa_Arab_PK", "pa_Guru", "pa_Guru_IN", "pl",
        "pl_PL", "prg", "prg_001", "ps", "ps_AF", "ps_PK", "pt", "pt_AO", "pt_BR", "pt_CH", "pt_CV", "pt_GQ",
        "pt_GW", "pt_LU", "pt_MO", "pt_MZ", "pt_PT", "pt_ST", "pt_TL", "qu", "qu_BO", "qu_EC", "qu_PE", "quc",
        "quc_GT", "rm", "rm_CH", "rn", "rn_BI", "ro", "ro_MD", "ro_RO", "rof", "rof_TZ", "root", "ru", "ru_BY",
        "ru_KG", "ru_KZ", "ru_MD", "ru_RU", "ru_UA", "rw", "rw_RW", "rwk", "rwk_TZ", "sa", "sa_IN", "sah", "sah_RU",
        "saq", "saq_KE", "sbp", "sbp_TZ", "sc", "sc_IT", "scn", "scn_IT", "sd", "sd_PK", "sdh", "sdh_IQ", "sdh_IR",
        "se", "se_FI", "se_NO", "se_SE", "seh", "seh_MZ", "ses", "ses_ML", "sg", "sg_CF", "shi", "shi_Latn",
        "shi_Latn_MA", "shi_Tfng", "shi_Tfng_MA", "si", "si_LK", "sid", "sid_ET", "sk", "sk_SK", "sl", "sl_SI",
        "sma", "sma_NO", "sma_SE", "smj", "smj_NO", "smj_SE", "smn", "smn_FI", "sms", "sms_FI", "sn", "sn_ZW", "so",
        "so_DJ", "so_ET", "so_KE", "so_SO", "sq", "sq_AL", "sq_MK", "sq_XK", "sr", "sr_Cyrl", "sr_Cyrl_BA",
        "sr_Cyrl_ME", "sr_Cyrl_RS", "sr_Cyrl_XK", "sr_Latn", "sr_Latn_BA", "sr_Latn_ME", "sr_Latn_RS", "sr_Latn_XK",
        "ss", "ss_SZ", "ss_ZA", "ssy", "ssy_ER", "st", "st_LS", "st_ZA", "sv", "sv_AX", "sv_FI", "sv_SE", "sw",
        "sw_CD", "sw_KE", "sw_TZ", "sw_UG", "syr", "syr_IQ", "syr_SY", "ta", "ta_IN", "ta_LK", "ta_MY", "ta_SG",
        "te", "te_IN", "teo", "teo_KE", "teo_UG", "tg", "tg_TJ", "th", "th_TH", "ti", "ti_ER", "ti_ET", "tig",
        "tig_ER", "tk", "tk_TM", "tn", "tn_BW", "tn_ZA", "to", "to_TO", "tr", "tr_CY", "tr_TR", "trv", "trv_TW",
        "ts", "ts_ZA", "tt", "tt_RU", "twq", "twq_NE", "tzm", "tzm_MA", "ug", "ug_CN", "uk", "uk_UA", "und",
        "und_ZZ", "ur", "ur_IN", "ur_PK", "uz", "uz_Arab", "uz_Arab_AF", "uz_Cyrl", "uz_Cyrl_UZ", "uz_Latn",
        "uz_Latn_UZ", "vai", "vai_Latn", "vai_Latn_LR", "vai_Vaii", "vai_Vaii_LR", "ve", "ve_ZA", "vi", "vi_VN",
        "vo", "vo_001", "vun", "vun_TZ", "wa", "wa_BE", "wae", "wae_CH", "wal", "wal_ET", "wbp", "wbp_AU", "wo",
        "wo_SN", "xh", "xh_ZA", "xog", "xog_UG", "yav", "yav_CM", "yi", "yi_001", "yo", "yo_BJ", "yo_NG", "yue",
        "yue_Hans", "yue_Hans_CN", "yue_Hant", "yue_Hant_HK", "zgh", "zgh_MA", "zh", "zh_Hans", "zh_Hans_CN",
        "zh_Hans_HK", "zh_Hans_MO", "zh_Hans_SG", "zh_Hant", "zh_Hant_HK", "zh_Hant_MO", "zh_Hant_TW", "zu",
        "zu_ZA",
        // spotless:on
    };

    public static String[] getPages() {
        return pages;
    }

    public static String[] getAnnotationPages() {
        return annotationPages;
    }

    public static String[] getLocales() {
        return locales;
    }
}
