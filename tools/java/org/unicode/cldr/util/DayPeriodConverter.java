package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.util.ULocale;

public class DayPeriodConverter {
    private static final boolean TO_CODE = true;

    // HACK TO SET UP DATA
    // Will be replaced by real data table in the future

    static final String[][] RAW_DATA = {
        {"germanic", "en", "English", "night", "night", "night", "night", "night", "night", "morning", "morning", "morning", "morning", "morning", "morning", "afternoon", "afternoon", "afternoon", "afternoon", "afternoon", "afternoon", "evening", "evening", "evening", "night", "night", "night"},
        {"germanic", "en-GB", "English (UK)", "night", "night", "night", "night", "night", "night", "morning", "morning", "morning", "morning", "morning", "morning", "afternoon", "afternoon", "afternoon", "afternoon", "afternoon", "evening", "evening", "evening", "evening", "night", "night", "night"},
        {"germanic", "af", "Afrikaans", "nag", "nag", "nag", "nag", "nag", "oggend", "oggend", "oggend", "oggend", "oggend", "oggend", "oggend", "middag", "middag", "middag", "middag", "middag", "middag", "aand", "aand", "aand", "aand", "aand", "aand"},
        {"germanic", "nl", "Dutch", "nacht", "nacht", "nacht", "nacht", "nacht", "nacht", "ochtend", "ochtend", "ochtend", "ochtend", "ochtend", "ochtend", "middag", "middag", "middag", "middag", "middag", "middag", "avond", "avond", "avond", "avond", "avond", "avond"},
        {"germanic", "de", "German", "Nacht", "Nacht", "Nacht", "Nacht", "Nacht", "Morgen", "Morgen", "Morgen", "Morgen", "Morgen", "Vormittag", "Vormittag", "Mittag", "Nachmittag", "Nachmittag", "Nachmittag", "Nachmittag", "Nachmittag", "Abend", "Abend", "Abend", "Abend", "Abend", "Abend"},
        {"germanic", "da", "Danish", "nat", "nat", "nat", "nat", "nat", "morgen", "morgen", "morgen", "morgen", "morgen", "formiddag", "formiddag", "eftermiddag", "eftermiddag", "eftermiddag", "eftermiddag", "eftermiddag", "eftermiddag", "aften", "aften", "aften", "aften", "aften", "aften"},
        {"germanic", "nb", "Norwegian Bokmål", "natt", "natt", "natt", "natt", "natt", "natt", "morgen", "morgen", "morgen", "morgen", "formiddag", "formiddag", "ettermiddag", "ettermiddag", "ettermiddag", "ettermiddag", "ettermiddag", "ettermiddag", "kveld", "kveld", "kveld", "kveld", "kveld", "kveld"},
        {"germanic", "sv", "Swedish", "natt", "natt", "natt", "natt", "natt", "morgon", "morgon", "morgon", "morgon", "morgon", "förmiddag", "förmiddag", "eftermiddag", "eftermiddag", "eftermiddag", "eftermiddag", "eftermiddag", "eftermiddag", "kväll", "kväll", "kväll", "kväll", "kväll", "kväll"},
        {"germanic", "is", "Icelandic", "nótt", "nótt", "nótt", "nótt", "nótt", "nótt", "morgunn", "morgunn", "morgunn", "morgunn", "morgunn", "morgunn", "eftir hádegi", "eftir hádegi", "eftir hádegi", "eftir hádegi", "eftir hádegi", "eftir hádegi", "kvöld", "kvöld", "kvöld", "kvöld", "kvöld", "kvöld"},
        {"romance", "pt", "Portuguese", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "manhã", "manhã", "manhã", "manhã", "manhã", "manhã", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "noite", "noite", "noite", "noite", "noite"},
        {"romance", "pt_PT", "European Portuguese", "noite", "noite", "noite", "noite", "noite", "noite", "manhã", "manhã", "manhã", "manhã", "manhã", "manhã", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "noite", "noite", "noite", "noite"},
        {"romance", "gl", "Galician", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "mañá", "mañá", "mañá", "mañá", "mañá", "mañá", "mediodía", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "noite", "noite", "noite"},
        {"romance", "es", "Spanish", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "mañana", "mañana", "mañana", "mañana", "mañana", "mañana", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "noche", "noche", "noche", "noche"},
        {"romance", "es_419", "Latin American Spanish", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "madrugada", "mañana", "mañana", "mañana", "mañana", "mañana", "mañana", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "tarde", "noche", "noche", "noche", "noche"},
        {"romance", "ca", "Catalan", "matinada", "matinada", "matinada", "matinada", "matinada", "matinada", "matí", "matí", "matí", "matí", "matí", "matí", "migdia", "tarda", "tarda", "tarda", "tarda", "tarda", "tarda", "vespre", "vespre", "nit", "nit", "nit"},
        {"romance", "it", "Italian", "notte", "notte", "notte", "notte", "notte", "notte", "mattina", "mattina", "mattina", "mattina", "mattina", "mattina", "pomeriggio", "pomeriggio", "pomeriggio", "pomeriggio", "pomeriggio", "pomeriggio", "sera", "sera", "sera", "sera", "sera", "sera"},
        {"romance", "ro", "Romanian", "noapte", "noapte", "noapte", "noapte", "noapte", "dimineață", "dimineață", "dimineață", "dimineață", "dimineață", "dimineață", "dimineață", "după-amiază", "după-amiază", "după-amiază", "după-amiază", "după-amiază", "după-amiază", "seară", "seară", "seară", "seară", "noapte", "noapte"},
        {"romance", "fr", "French", "nuit", "nuit", "nuit", "nuit", "matin", "matin", "matin", "matin", "matin", "matin", "matin", "matin", "après-midi", "après-midi", "après-midi", "après-midi", "après-midi", "après-midi", "soir", "soir", "soir", "soir", "soir", "soir"},
        {"romance", "fr-CA", "French (Canada)", "nuit", "nuit", "nuit", "nuit", "matin", "matin", "matin", "matin", "matin", "matin", "matin", "matin", "après-midi", "après-midi", "après-midi", "après-midi", "après-midi", "après-midi", "soir", "soir", "soir", "soir", "soir", "soir"},
        {"slavic", "hr", "Croatian", "noć", "noć", "noć", "noć", "jutro", "jutro", "jutro", "jutro", "jutro", "jutro", "jutro", "jutro", "popodne", "popodne", "popodne", "popodne", "popodne", "popodne", "večer", "večer", "večer", "noć", "noć", "noć"},
        {"slavic", "bs", "Bosnian", "noć", "noć", "noć", "noć", "jutro", "jutro", "jutro", "jutro", "jutro", "jutro", "jutro", "jutro", "popodne", "popodne", "popodne", "popodne", "popodne", "popodne", "veče", "veče", "veče", "noć", "noć", "noć"},
        {"slavic", "sr", "Serbian", "ноћ", "ноћ", "ноћ", "ноћ", "ноћ", "ноћ", "јутро", "јутро", "јутро", "јутро", "јутро", "јутро", "поподне", "поподне", "поподне", "поподне", "поподне", "поподне", "вече", "вече", "вече", "ноћ", "ноћ", "ноћ"},
        {"slavic", "sl", "Slovenian", "noč", "noč", "noč", "noč", "noč", "noč", "jutro", "jutro", "jutro", "jutro", "dopoldne", "dopoldne", "popoldne", "popoldne", "popoldne", "popoldne", "popoldne", "popoldne", "večer", "večer", "večer", "večer", "noč", "noč"},
        {"slavic", "cs", "Czech", "noc", "noc", "noc", "noc", "ráno", "ráno", "ráno", "ráno", "ráno", "dopoledne", "dopoledne", "dopoledne", "odpoledne", "odpoledne", "odpoledne", "odpoledne", "odpoledne", "odpoledne", "večer", "večer", "večer", "večer", "noc", "noc"},
        {"slavic", "sk", "Slovak", "noc", "noc", "noc", "noc", "ráno", "ráno", "ráno", "ráno", "ráno", "dopoludnie", "dopoludnie", "dopoludnie", "popoludnie", "popoludnie", "popoludnie", "popoludnie", "popoludnie", "popoludnie", "večer", "večer", "večer", "večer", "noc", "noc"},
        {"slavic", "pl", "Polish", "noc", "noc", "noc", "noc", "noc", "noc", "rano", "rano", "rano", "rano", "przedpołudnie", "przedpołudnie", "popołudnie", "popołudnie", "popołudnie", "popołudnie", "popołudnie", "popołudnie", "wieczór", "wieczór", "wieczór", "noc", "noc", "noc"},
        {"slavic", "bg", "Bulgarian", "нощ", "нощ", "нощ", "нощ", "сутринта", "сутринта", "сутринта", "сутринта", "сутринта", "сутринта", "сутринта", "на обяд", "на обяд", "на обяд", "следобяд", "следобяд", "следобяд", "следобяд", "вечерта", "вечерта", "вечерта", "вечерта", "нощ", "нощ"},
        {"slavic", "mk", "Macedonian", "по полноќ", "по полноќ", "по полноќ", "по полноќ", "наутро", "наутро", "наутро", "наутро", "наутро", "наутро", "претпладне", "претпладне", "попладне", "попладне", "попладне", "попладне", "попладне", "попладне", "навечер", "навечер", "навечер", "навечер", "навечер", "навечер"},
        {"slavic", "ru", "Russian", "ночь", "ночь", "ночь", "ночь", "утро", "утро", "утро", "утро", "утро", "утро", "утро", "утро", "день", "день", "день", "день", "день", "день", "вечер", "вечер", "вечер", "вечер", "вечер", "вечер"},
        {"slavic", "uk", "Ukrainian", "ніч", "ніч", "ніч", "ніч", "ранок", "ранок", "ранок", "ранок", "ранок", "ранок", "ранок", "ранок", "день", "день", "день", "день", "день", "день", "вечір", "вечір", "вечір", "вечір", "вечір", "вечір"},
        {"baltic", "lt", "Lithuanian", "naktis", "naktis", "naktis", "naktis", "naktis", "naktis", "rytas", "rytas", "rytas", "rytas", "rytas", "rytas", "diena", "diena", "diena", "diena", "diena", "diena", "vakaras", "vakaras", "vakaras", "vakaras", "vakaras", "vakaras"},
        {"baltic", "lv", "Latvian", "nakts", "nakts", "nakts", "nakts", "nakts", "nakts", "rīts", "rīts", "rīts", "rīts", "rīts", "rīts", "pēcpusdiena", "pēcpusdiena", "pēcpusdiena", "pēcpusdiena", "pēcpusdiena", "pēcpusdiena", "vakars", "vakars", "vakars", "vakars", "vakars", "nakts"},
        {"other-indo", "el", "Greek", "μεσάνυχτα", "βράδυ", "βράδυ", "βράδυ", "πρωί", "πρωί", "πρωί", "πρωί", "πρωί", "πρωί", "πρωί", "πρωί", "μεσημέρι", "μεσημέρι", "μεσημέρι", "μεσημέρι", "μεσημέρι", "απόγευμα", "απόγευμα", "απόγευμα", "βράδυ", "βράδυ", "βράδυ", "βράδυ"},
        {"other-indo", "fa", "Persian", "شب", "شب", "شب", "شب", "صبح", "صبح", "صبح", "صبح", "صبح", "صبح", "صبح", "صبح", "بعد از ظهر", "بعد از ظهر", "بعد از ظهر", "بعد از ظهر", "بعد از ظهر", "عصر", "عصر", "شب", "شب", "شب", "شب", "شب"},
        {"other-indo", "hy", "Armenian", "գիշեր", "գիշեր", "գիշեր", "գիշեր", "գիշեր", "գիշեր", "առավոտ", "առավոտ", "առավոտ", "առավոտ", "առավոտ", "առավոտ", "ցերեկ", "ցերեկ", "ցերեկ", "ցերեկ", "ցերեկ", "ցերեկ", "երեկո", "երեկո", "երեկո", "երեկո", "երեկո", "երեկո"},
        {"other-indo", "ka", "Georgian", "ღამე", "ღამე", "ღამე", "ღამე", "ღამე", "დილა", "დილა", "დილა", "დილა", "დილა", "დილა", "დილა", "მეორე ნახევარი", "მეორე ნახევარი", "მეორე ნახევარი", "მეორე ნახევარი", "მეორე ნახევარი", "მეირე ნახევარი", "საღამო", "საღამო", "საღამო", "ღამე", "ღამე", "ღამე"},
        {"other-indo", "sq", "Albanian", "natë", "natë", "natë", "natë", "mëngjes", "mëngjes", "mëngjes", "mëngjes", "mëngjes", "paradite", "paradite", "paradite", "pasdite", "pasdite", "pasdite", "pasdite", "pasdite", "pasdite", "mbrëmje", "mbrëmje", "mbrëmje", "mbrëmje", "mbrëmje", "mbrëmje"},
        {"indic", "ur", "Urdu", "رات", "رات", "رات", "رات", "صبح", "صبح", "صبح", "صبح", "صبح", "صبح", "صبح", "صبح", "دوپہر", "دوپہر", "دوپہر", "دوپہر", "سہ پہر", "سہ پہر", "شام", "شام", "رات", "رات", "رات", "رات"},
        {"indic", "hi", "Hindi", "रात", "रात", "रात", "रात", "सुबह", "सुबह", "सुबह", "सुबह", "सुबह", "सुबह", "सुबह", "सुबह", "दोपहर", "दोपहर", "दोपहर", "दोपहर", "शाम", "शाम", "शाम", "शाम", "रात", "रात", "रात", "रात"},
        {"indic", "bn", "Bengali", "রাত্রি", "রাত্রি", "রাত্রি", "রাত্রি", "ভোর", "ভোর", "সকাল", "সকাল", "সকাল", "সকাল", "সকাল", "সকাল", "দুপুর", "দুপুর", "দুপুর", "দুপুর", "বিকাল", "বিকাল", "সন্ধ্যা", "সন্ধ্যা", "রাত্রি", "রাত্রি", "রাত্রি", "রাত্রি"},
        {"indic", "gu", "Gujarati", "રાત", "રાત", "રાત", "રાત", "સવાર", "સવાર", "સવાર", "સવાર", "સવાર", "સવાર", "સવાર", "સવાર", "બપોર", "બપોર", "બપોર", "બપોર", "સાંજ", "સાંજ", "સાંજ", "સાંજ", "રાત", "રાત", "રાત", "રાત"},
        {"indic", "mr", "Marathi", "रात्री", "रात्री", "रात्री", "रात्र", "पहाटे", "पहाटे", "सकाळी", "सकाळी", "सकाळी", "सकाळी", "सकाळी", "सकाळी", "दुपारी", "दुपारी", "दुपारी", "दुपारी", "संध्याकाळी", "संध्याकाळी", "संध्याकाळी", "संध्याकाळी", "रात्री", "रात्री", "रात्री", "रात्री"},
        {"indic", "ne", "Nepali", "रात", "रात", "रात", "रात", "विहान", "विहान", "विहान", "विहान", "विहान", "विहान", "विहान", "विहान", "अपरान्ह", "अपरान्ह", "अपरान्ह", "अपरान्ह", "साँझ", "साँझ", "साँझ", "बेलुका", "बेलुका", "बेलुका", "रात", "रात"},
        {"indic", "pa", "Punjabi", "ਰਾਤ", "ਰਾਤ", "ਰਾਤ", "ਰਾਤ", "ਸਵੇਰ", "ਸਵੇਰ", "ਸਵੇਰ", "ਸਵੇਰ", "ਸਵੇਰ", "ਸਵੇਰ", "ਸਵੇਰ", "ਸਵੇਰ", "ਦੁਪਹਿਰ", "ਦੁਪਹਿਰ", "ਦੁਪਹਿਰ", "ਦੁਪਹਿਰ", "ਸ਼ਾਮ", "ਸ਼ਾਮ", "ਸ਼ਾਮ", "ਸ਼ਾਮ", "ਸ਼ਾਮ", "ਰਾਤ", "ਰਾਤ", "ਰਾਤ"},
        {"indic", "si", "Sinhala", "මැදියමට පසු", "පාන්දර", "පාන්දර", "පාන්දර", "පාන්දර", "පාන්දර", "උදේ", "උදේ", "උදේ", "උදේ", "උදේ", "උදේ", "දවල්", "දවල්", "හවස", "හවස", "හවස", "හවස", "රෑ", "රෑ", "රෑ", "රෑ", "රෑ", "රෑ"},
        {"dravidian", "ta", "Tamil", "இரவு", "இரவு", "இரவு", "அதிகாலை", "அதிகாலை", "காலை", "காலை", "காலை", "காலை", "காலை", "காலை", "காலை", "பிற்பகல்", "பிற்பகல்", "மாலை", "மாலை", "பிற்பகல்", "பிற்பகல்", "மாலை", "மாலை", "மாலை", "இரவு", "இரவு", "இரவு"},
        {"dravidian", "te", "Telugu", "రాత్రి", "రాత్రి", "రాత్రి", "రాత్రి", "రాత్రి", "రాత్రి", "ఉదయం", "ఉదయం", "ఉదయం", "ఉదయం", "ఉదయం", "ఉదయం", "మధ్యాహ్నం", "మధ్యాహ్నం", "మధ్యాహ్నం", "మధ్యాహ్నం", "మధ్యాహ్నం", "మధ్యాహ్నం", "సాయంత్రం", "సాయంత్రం", "సాయంత్రం", "రాత్రి", "రాత్రి", "రాత్రి"},
        {"dravidian", "ml", "Malayalam", "രാത്രി", "രാത്രി", "രാത്രി", "പുലർച്ചെ", "പുലർച്ചെ", "പുലർച്ചെ", "രാവിലെ", "രാവിലെ", "രാവിലെ", "രാവിലെ", "രാവിലെ", "രാവിലെ", "ഉച്ചയ്ക്ക്", "ഉച്ചയ്ക്ക്", "ഉച്ചതിരിഞ്ഞ്", "വൈകുന്നേരം", "വൈകുന്നേരം", "വൈകുന്നേരം", "സന്ധ്യയ്ക്ക്", "രാത്രി", "രാത്രി", "രാത്രി", "രാത്രി", "രാത്രി"},
        {"dravidian", "kn", "Kannada", "ರಾತ್ರಿ", "ರಾತ್ರಿ", "ರಾತ್ರಿ", "ರಾತ್ರಿ", "ರಾತ್ರಿ", "ರಾತ್ರಿ", "ಬೆಳಗ್ಗೆ", "ಬೆಳಗ್ಗೆ", "ಬೆಳಗ್ಗೆ", "ಬೆಳಗ್ಗೆ", "ಬೆಳಗ್ಗೆ", "ಬೆಳಗ್ಗೆ", "ಮಧ್ಯಾಹ್ನ", "ಮಧ್ಯಾಹ್ನ", "ಮಧ್ಯಾಹ್ನ", "ಮಧ್ಯಾಹ್ನ", "ಮಧ್ಯಾಹ್ನ", "ಮಧ್ಯಾಹ್ನ", "ಸಂಜೆ", "ಸಂಜೆ", "ಸಂಜೆ", "ರಾತ್ರಿ", "ರಾತ್ರಿ", "ರಾತ್ರಿ"},
        {"cjk", "zh", "Chinese", "凌晨", "凌晨", "凌晨", "凌晨", "凌晨", "早上", "早上", "早上", "上午", "上午", "上午", "上午", "中午", "下午", "下午", "下午", "下午", "下午", "下午", "晚上", "晚上", "晚上", "晚上", "晚上"},
        {"cjk", "zh_Hant", "Traditional Chinese", "凌晨", "凌晨", "凌晨", "凌晨", "凌晨", "早上", "早上", "早上", "上午", "上午", "上午", "上午", "中午", "下午", "下午", "下午", "下午", "下午", "下午", "晚上", "晚上", "晚上", "晚上", "晚上"},
        {"cjk", "zh-HK", "Chinese (Hong Kong)", "凌晨", "凌晨", "凌晨", "凌晨", "凌晨", "早上", "早上", "早上", "上午", "上午", "上午", "上午", "中午", "下午", "下午", "下午", "下午", "下午", "下午", "晚上", "晚上", "晚上", "晚上", "晚上"},
        {"cjk", "ja", "Japanese", "夜中", "夜中", "夜中", "夜中", "明け方", "明け方", "朝", "朝", "朝", "午前", "午前", "午前", "午後", "午後", "午後", "午後", "夕方", "夕方", "夕方", "夜", "夜", "夜", "夜", "夜中"},
        {"cjk", "ko", "Korean", "밤", "밤", "밤", "새벽", "새벽", "새벽", "오전", "오전", "오전", "오전", "오전", "오전", "오후", "오후", "오후", "오후", "오후", "오후", "저녁", "저녁", "저녁", "밤", "밤", "밤"},
        {"turkic", "tr", "Turkish", "gece", "gece", "gece", "gece", "gece", "gece", "sabah", "sabah", "sabah", "sabah", "sabah", "öğleden önce", "öğleden sonra", "öğleden sonra", "öğleden sonra", "öğleden sonra", "öğleden sonra", "öğleden sonra", "akşamüstü", "akşam", "akşam", "gece", "gece", "gece"},
        {"turkic", "az", "Azerbaijani", "gecə", "gecə", "gecə", "gecə", "sübh", "sübh", "səhər", "səhər", "səhər", "səhər", "səhər", "səhər", "gündüz", "gündüz", "gündüz", "gündüz", "gündüz", "axşamüstü", "axşamüstü", "axşam", "axşam", "axşam", "axşam", "axşam"},
        {"turkic", "kk", "Kazakh", "түн", "түн", "түн", "түн", "түн", "түн", "таң", "таң", "таң", "таң", "таң", "таң", "түс", "түс", "түс", "түс", "түс", "түс", "кеш", "кеш", "кеш", "түн", "түн", "түн"},
        {"turkic", "ky", "Kyrgyz", "түн", "түн", "түн", "түн", "түн", "түн", "эртең менен", "эртең менен", "эртең менен", "эртең менен", "эртең менен", "эртең менен", "түштөн кийин", "түштөн кийин", "түштөн кийин", "түштөн кийин", "түштөн кийин", "түштөн кийин", "кечкурун", "кечкурун", "кечкурун", "түн", "түн", "түн"},
        {"turkic", "uz", "Uzbek", "tun", "tun", "tun", "tun", "tun", "tun", "ertalab", "ertalab", "ertalab", "ertalab", "ertalab", "kunduz", "kunduz", "kunduz", "kunduz", "kunduz", "kunduz", "kunduz", "kechqurun", "kechqurun", "kechqurun", "kechqurun", "tun", "tun"},
        {"uralic", "et", "Estonian", "öö", "öö", "öö", "öö", "öö", "hommik", "hommik", "hommik", "hommik", "hommik", "hommik", "hommik", "pärastlõuna", "pärastlõuna", "pärastlõuna", "pärastlõuna", "pärastlõuna", "pärastlõuna", "õhtu", "õhtu", "õhtu", "õhtu", "õhtu", "öö"},
        {"uralic", "fi", "b", "yö", "yö", "yö", "yö", "yö", "aamu", "aamu", "aamu", "aamu", "aamu", "aamupäivä", "aamupäivä", "iltapäivä", "iltapäivä", "iltapäivä", "iltapäivä", "iltapäivä", "iltapäivä", "ilta", "ilta", "ilta", "ilta", "ilta", "yö"},
        {"uralic", "hu", "Hungarian", "éjjel", "éjjel", "éjjel", "éjjel", "hajnal", "hajnal", "reggel", "reggel", "reggel", "délelőtt", "délelőtt", "délelőtt", "délután", "délután", "délután", "délután", "délután", "délután", "este", "este", "este", "éjjel", "éjjel", "éjjel"},
        {"tai", "th", "Thai", "กลางคืน", "กลางคืน", "กลางคืน", "กลางคืน", "กลางคืน", "กลางคืน", "เช้า", "เช้า", "เช้า", "เช้า", "เช้า", "เช้า", "เที่ยง", "บ่าย", "บ่าย", "บ่าย", "เย็น", "เย็น", "ค่ำ", "ค่ำ", "ค่ำ", "กลางคืน", "กลางคืน", "กลางคืน"},
        {"tai", "lo", "Lao", "​ກາງ​ຄືນ", "​ກາງ​ຄືນ", "​ກາງ​ຄືນ", "​ກາງ​ຄືນ", "​ກາງ​ຄືນ", "​ເຊົ້າ", "​ເຊົ້າ", "​ເຊົ້າ", "​ເຊົ້າ", "​ເຊົ້າ", "​ເຊົ້າ", "​ເຊົ້າ", "​ສວາຍ", "​ສວາຍ", "​ສວາຍ", "​ສວາຍ", "ແລງ", "​ແລງ", "​ແລງ", "​ແລງ", "​ຄ່ຳ", "​ຄ່ຳ", "​ຄ່ຳ", "​ຄ່ຳ"},
        {"semitic", "ar", "Arabic", " منتصف الليل", "ليلا", "ليلا", "فجرا", "فجرا", "فجرا", "صباحا", "صباحا", "صباحا", "صباحا", "صباحا", "صباحا", "ظهرا", "بعد الظهر", "بعد الظهر", "بعد الظهر", "بعد الظهر", "بعد الظهر", "مساء", "مساء", "مساء", "مساء", "مساء", "مساء"},
        {"semitic", "he", "Hebrew", "לילה", "לילה", "לילה", "לילה", "לילה", "בוקר", "בוקר", "בוקר", "בוקר", "בוקר", "בוקר", "צהריים", "צהריים", "צהריים", "צהריים", "אחר הצהריים", "אחר הצהריים", "אחר הצהריים", "ערב", "ערב", "ערב", "ערב", "לילה", "לילה"},
        {"malayic", "id", "Indonesian", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "siang", "siang", "siang", "siang", "siang", "sore", "sore", "sore", "malam", "malam", "malam", "malam", "malam", "malam"},
        {"malayic", "ms", "Malay", "tengah malam", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "pagi", "tengah hari", "tengah hari", "petang", "petang", "petang", "petang", "petang", "malam", "malam", "malam", "malam", "malam"},
        {"malayic", "fil", "Filipino", "madaling-araw", "madaling-araw", "madaling-araw", "madaling-araw", "madaling-araw", "madaling-araw", "umaga", "umaga", "umaga", "umaga", "umaga", "umaga", "tanghali", "tanghali", "tanghali", "tanghali", "hapon", "hapon", "gabi", "gabi", "gabi", "gabi", "gabi", "gabi"},
        {"austroasiatic", "vi", "Vietnamese", "đêm", "đêm", "đêm", "đêm", "sáng", "sáng", "sáng", "sáng", "sáng", "sáng", "sáng", "sáng", "chiều", "chiều", "chiều", "chiều", "chiều", "chiều", "tối", "tối", "tối", "đêm", "đêm", "đêm"},
        {"austroasiatic", "km", "Khmer", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "ព្រឹក", "រសៀល", "រសៀល", "រសៀល", "រសៀល", "រសៀល", "រសៀល", "ល្ងាច", "ល្ងាច", "ល្ងាច", "យប់", "យប់", "យប់"},
        {"other", "sw", "Swahili", "usiku", "usiku", "usiku", "usiku", "alfajiri", "alfajiri", "alfajiri", "asubuhi", "asubuhi", "asubuhi", "asubuhi", "asubuhi", "mchana", "mchana", "mchana", "mchana", "jioni", "jioni", "jioni", "usiku", "usiku", "usiku", "usiku", "usiku"},
        {"other", "zu", "Zulu", "ntathakusa", "ntathakusa", "ntathakusa", "ntathakusa", "ntathakusa", "ntathakusa", "ekuseni", "ekuseni", "ekuseni", "ekuseni", "emini", "emini", "emini", "ntambama", "ntambama", "ntambama", "ntambama", "ntambama", "ntambama", "ebusuku", "ebusuku", "ebusuku", "ebusuku", "ebusuku"},
        {"other", "am", "Amharic", "ሌሊት", "ሌሊት", "ሌሊት", "ሌሊት", "ሌሊት", "ሌሊት", "ጥዋት", "ጥዋት", "ጥዋት", "ጥዋት", "ጥዋት", "ጥዋት", "ከሰዓት በኋላ", "ከሰዓት በኋላ", "ከሰዓት በኋላ", "ከሰዓት በኋላ", "ከሰዓት በኋላ", "ከሰዓት በኋላ", "ማታ", "ማታ", "ማታ", "ማታ", "ማታ", "ማታ"},
        {"other", "eu", "Basque", "goizaldea", "goizaldea", "goizaldea", "goizaldea", "goizaldea", "goizaldea", "goiza", "goiza", "goiza", "goiza", "goiza", "goiza", "eguerdia", "eguerdia", "arratsaldea", "arratsaldea", "arratsaldea", "arratsaldea", "arratsaldea", "arratsaldea", "arratsaldea", "gaua", "gaua", "gaua"},
        {"other", "mn", "Mongolian", "шөнө", "шөнө", "шөнө", "шөнө", "шөнө", "шөнө", "өглөө", "өглөө", "өглөө", "өглөө", "өглөө", "өглөө", "өдөр", "өдөр", "өдөр", "өдөр", "өдөр", "өдөр", "орой", "орой", "орой", "шөнө", "шөнө", "шөнө"},
        {"other", "my", "Burmese", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နံနက်", "နေ့လည်", "နေ့လည်", "နေ့လည်", "နေ့လည်", "ညနေ", "ညနေ", "ညနေ", "ည", "ည", "ည", "ည", "ည"},    };

    public enum DayPeriod {EARLY_MORNING, MORNING, EARLY_AFTERNOON, AFTERNOON, EARLY_EVENING, EVENING, NIGHT, LATE_NIGHT}

    static final Map<ULocale,DayPeriod[]> DATA;
    static {
        Map<ULocale,DayPeriod[]> temp  = new LinkedHashMap<>();
        EnumSet<DayPeriod> missing = EnumSet.allOf(DayPeriod.class);
        for (String[] x : RAW_DATA) {
            String locale = (String) x[1];
            if (locale.contains("-") || locale.contains("_")) {
                continue;
            }
            Relation<String,Integer> raw = Relation.of(new TreeMap<String,Set<Integer>>(), TreeSet.class);
            Set<String> am = new LinkedHashSet<>();
            Set<String> pm = new LinkedHashSet<>();
            for (int i=0; i < x.length-3; ++i) {
                String value = x[i+3];
                raw.put(value, i);
                if (i < 12) {
                    am.add(value);
                } else {
                    pm.add(value);
                }
            }
            // handle overlaps
            am.removeAll(pm);
            if (am.size() > 3) {
                am.remove(x[0+3]);
                pm.add(x[0+3]);
            }
//            if (am.contains(x[12+3]) && pm.contains(x[12+3])) {
//                am.remove(x[12+3]); // keep afternoon in PM
//            }
//            if (am.contains(x[0+3]) && (pm.contains(x[0+3]) || am.size() > 3)) {
//                am.remove(x[0+3]); // keep night in PM
//            }
            ArrayList<String> amList = new ArrayList<>(am);
            ArrayList<String> pmList = new ArrayList<>(pm);
            Map<DayPeriod,String> result = new EnumMap<DayPeriod,String>(DayPeriod.class);
            switch(amList.size()) {
            case 1: 
                put(result,amList.get(0), DayPeriod.MORNING); 
                break;
            case 2: 
                put(result,amList.get(0), DayPeriod.EARLY_MORNING); 
                put(result,amList.get(1), DayPeriod.MORNING); 
                break;
            case 3: 
                put(result,amList.get(0), DayPeriod.LATE_NIGHT); 
                put(result,amList.get(1), DayPeriod.EARLY_MORNING); 
                put(result,amList.get(2), DayPeriod.MORNING); 
                break;
//            case 4: 
//                put(result,amList.get(0), DayPeriod.LATE_NIGHT); 
//                put(result,amList.get(1), DayPeriod.EARLY_MORNING); 
//                put(result,amList.get(2), DayPeriod.MORNING); 
//                break;
            default:
                throw new IllegalArgumentException(locale + " Too many items in am: " + amList);
            }
            switch(pmList.size()) {
            case 1: 
                put(result,pmList.get(0), DayPeriod.AFTERNOON); 
                break;
            case 2: 
                put(result,pmList.get(0), DayPeriod.AFTERNOON); 
                put(result,pmList.get(1), DayPeriod.EVENING); 
                break;
            case 3: 
                put(result,pmList.get(0), DayPeriod.AFTERNOON); 
                put(result,pmList.get(1), DayPeriod.EVENING); 
                put(result,pmList.get(2), DayPeriod.NIGHT); 
                break;
            case 4: 
                put(result,pmList.get(0), DayPeriod.EARLY_AFTERNOON); 
                put(result,pmList.get(1), DayPeriod.AFTERNOON); 
                put(result,pmList.get(2), DayPeriod.EVENING); 
                put(result,pmList.get(3), DayPeriod.NIGHT); 
                break;
            case 5: 
                put(result,pmList.get(0), DayPeriod.EARLY_AFTERNOON); 
                put(result,pmList.get(1), DayPeriod.AFTERNOON); 
                put(result,pmList.get(2), DayPeriod.EARLY_EVENING); 
                put(result,pmList.get(3), DayPeriod.EVENING); 
                put(result,pmList.get(4), DayPeriod.NIGHT); 
                break;
            default:
                throw new IllegalArgumentException(locale + "Too many items in pm: " + pmList);
            }
            fix(result, DayPeriod.LATE_NIGHT, DayPeriod.NIGHT);
            fix(result, DayPeriod.EARLY_MORNING, DayPeriod.NIGHT);
            fix(result, DayPeriod.EARLY_AFTERNOON, DayPeriod.AFTERNOON);
            fix(result, DayPeriod.EARLY_EVENING, DayPeriod.EVENING);
            Set<Integer> hoursSoFar = new TreeSet<Integer>();
            if (TO_CODE) {
                System.out.println("\t\tmake(\"" + locale + "\")");
                for (Entry<DayPeriod, String> entry : result.entrySet()) {
                    missing.remove(entry.getKey());
                    Set<Integer> hours = raw.get(entry.getValue());
                    System.out.println("\t\t.add(\"" + entry.getKey() 
                        + "\", \"" + entry.getValue()
                        + "\", " + CollectionUtilities.join(hours, ", ")
                        + ")"
                        );
                    hoursSoFar.addAll(hours);
                }
                if (hoursSoFar.size() != 24) {
                    System.out.println("Missing!");
                }
                System.out.println("\t\t.build();");
            } else {
                for (Entry<DayPeriod, String> entry : result.entrySet()) {
                    missing.remove(entry.getKey());
                    System.out.println(locale + "\t" + entry.getValue() + "\t" + entry.getKey() + "\t\t" + raw.get(entry.getValue()));
                }
            }
            System.out.println();
//            DayPeriod[] map = new DayPeriod[24];
//            temp.put(new ULocale(locale), map);
        }
        System.out.println("// Missing: " + missing);
        DATA = Collections.unmodifiableMap(temp);
    }
    public static void main(String[] args) {

    }
    private static void put(Map<DayPeriod, String> result, String string, DayPeriod earlyAfternoon) {
        result.put(earlyAfternoon, string);
    }
    private static void fix(Map<DayPeriod, String> result, DayPeriod lessDesirable, DayPeriod moreDesirable) {
        Set<DayPeriod> keySet = result.keySet();
        if (keySet.contains(lessDesirable) && !keySet.contains(moreDesirable)) {
            String oldValue = result.get(lessDesirable);
            result.remove(lessDesirable);
            result.put(moreDesirable, oldValue);
        }
    }
}
