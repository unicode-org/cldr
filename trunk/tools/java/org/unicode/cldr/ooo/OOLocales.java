/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;


public class OOLocales
{
    static private String m_OOLocales [] = {
        "af_ZA", "ar_EG", "ar_LB", "ar_SA", "ar_TN", "az_AZ",
        "bg_BG", "bs_BA", "ca_ES", "cs_CZ", "cy_GB", "da_DK",
        "de_AT", "de_CH", "de_DE", "de_LI", "de_LU", "el_GR",
        "en_AU", "en_BZ", "en_CA", "en_CB", "en_GB", "en_IE",
        "en_JM", "en_NZ", "en_PH", "en_TT", "en_US", "en_ZA",
        "en_ZW", "es_AR", "es_BO", "es_CL", "es_CO", "es_CR",
        "es_DO", "es_EC", "es_ES", "es_GT", "es_HN", "es_MX",
        "es_NI", "es_PA", "es_PE", "es_PR", "es_PY", "es_SV",
        "es_UY", "es_VE", "et_EE", "eu", "fi_FI", "fr_BE",
        "fr_CA", "fr_CH", "fr_FR", "fr_LU", "fr_MC", "gl_ES",
        "gu_IN", "he_IL", "hi_IN", "hr_HR", "hu_HU", "ia",
        "id_ID", "is_IS", "it_CH", "it_IT", "it_IT", "ja_JP", 
        "km_KH", "kn_IN", "ko_KR", "lo_LA", "lt_LT", "lv_LV",
        "mn_MN", "mr_IN", "ms_MY", "nb_NO", "nl_BE", "nl_NL",
        "nn_NO", "no_NO", "pa_IN", "pl_PL", "pt_BR", "pt_PT",
        "ro_RO", "ru_RU", "sh_YU", "sk_SK", "sl_SI", "sr_YU",
        "sv_FI", "sv_SE", "sw_TZ", "ta_IN", "te_IN", "th_TH",
        "tr_TR", "uk_UA", "zh_CN", "zh_HK", "zh_MO", "zh_SG",
        "zh_TW", "ga_IE", "fo_FO", "rw_RW"};
       
    
    //all locales have gregorian calendar as default , some have a second calendar which must be geenrated
    static private String extra_calendars [][] = 
    { 
        {"ar_EG",OOConstants.HIJRI},  
        {"ar_LB",OOConstants.HIJRI}, 
        {"ar_SA",OOConstants.HIJRI}, 
        {"ar_TN",OOConstants.HIJRI},
        {"he_IL",OOConstants.JEWISH},
        {"ja_JP",OOConstants.GENGOU},
        {"ko_KR",OOConstants.HANJA},
        {"lo_LA",OOConstants.BUDDHIST},
        {"th_TH",OOConstants.BUDDHIST},
        {"zh_TW",OOConstants.ROC}};
    
    /** Creates a new instance of OOLocales */
    public OOLocales()
    {

    }
    
    static public boolean isOOLocale (String locale)
    {
        boolean bFound = false;
        
        for (int i=0; i < m_OOLocales.length; i++)
        {
            if (m_OOLocales[i].compareTo (locale) ==0)
            {
                bFound = true;
                break;
            }
        }
        return bFound;
    }
    
    //returns the name (if any) of a second calendar needed by this locale
    static public String getExtraCalendar (String localeStr)
    {
        String calendar = null;
        for (int i=0; i < extra_calendars.length; i++)
        {
            if (extra_calendars[i][0].compareTo(localeStr)==0)
            {
                calendar = extra_calendars[i][1];
                break;
            }
        }
        return calendar;
    }
     
    //determines if we need to write the input calendar to an OO file for the input locale
    static public boolean needCalendar (String calendar, String localeStr)
    {
        boolean bNeeded = false;
        if ((calendar.compareTo(OOConstants.GREGORIAN)==0)
            || ( (getExtraCalendar (localeStr)!=null) && (getExtraCalendar (localeStr).compareTo(calendar)==0)))
            bNeeded = true;
        
        return bNeeded;
    }
        
        
        
}
