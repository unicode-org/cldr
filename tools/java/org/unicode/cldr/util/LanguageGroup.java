package org.unicode.cldr.util;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.util.ULocale;

public enum LanguageGroup {
    germanic("gem"), celtic("cel"), romance("roa"), slavic("sla"), baltic("bat"), indic("inc"), other_indo("ine"), dravidian("dra"), 
    uralic("urj"), cjk("und_Hani"), sino_tibetan("sit"), tai("tai"), austronesian("map"), turkic("trk"), 
    afroasiatic("afa"), austroasiatic("aav"), niger_congo("nic"), east_sudanic("sdv"), 
    songhay("son"), american("und_019"),
    art("art"), other("und");
    public final String iso;
    LanguageGroup(String iso) {
        this.iso = iso;
    }
    static final Map<ULocale,LanguageGroup> LANGUAGE_GROUP;
    static final Relation<LanguageGroup,ULocale> GROUP_LANGUAGE = Relation.of(new EnumMap<LanguageGroup,Set<ULocale>>(LanguageGroup.class), LinkedHashSet.class);
    
    private static void add(Map<ULocale, LanguageGroup> map, LanguageGroup group, String... baseLanguages) {
        for (String s : baseLanguages) {
            ULocale loc = new ULocale(s);
            if (map.put(loc, group) != null) {
                throw new IllegalArgumentException("duplicate: " + s + ", " + group);
            };
            GROUP_LANGUAGE.put(group, loc);
        }
    }
    static {
        LinkedHashMap<ULocale, LanguageGroup> temp = new LinkedHashMap<>();
        LANGUAGE_GROUP = Collections.unmodifiableMap(temp);
        add(temp, germanic, "en", "fy", "af", "nl", "de", "gsw", "wae", "ksh", "lb", "fo", "da", "nb", "nn", "sv", "is", "yi");
        add(temp, celtic, "gd", "ga", "cy", "gv", "kw", "br");
        add(temp, romance, "pt", "gl", "ast", "es", "ca", "it", "rm", "ro", "fr");
        add(temp, slavic, "ru", "be", "uk", "bg", "mk", "sr", "hr", "bs", "sl", "cs", "sk", "pl");
        add(temp, baltic, "lt", "lv");
        add(temp, other_indo, "el", "sq", "hy", "fa", "ps");
        add(temp, indic, "ur", "hi", "bn", "as", "gu", "or", "mr", "ne", "pa", "si");
        add(temp, dravidian, "ta", "te", "ml", "kn");
        add(temp, cjk, "zh", "ja", "ko");
        add(temp, turkic, "tr", "az", "kk", "ky", "uz", "ug");
        add(temp, uralic, "fi", "et", "se", "smn", "hu");
        add(temp, afroasiatic, "ar", "mt", "he", "om", "so", "ha", "am", "tzm", "zgh");
        add(temp, tai, "th", "lo");
        add(temp, austronesian, "id", "ms", "fil", "haw");
        add(temp, austroasiatic, "vi", "km");
        add(temp, niger_congo, "sw", "swc", "yo", "ig", "ff", "sn", "zu");
        add(temp, other, "ka", "eu", "mn", "naq");
        add(temp, sino_tibetan, "my");
        add(temp, afroasiatic, "aa", "kab", "shi", "ssy", "ti");
        add(temp, american, "chr", "kl", "lkt", "qu");
        add(temp, art, "eo", "vo", "ia");
        add(temp, austronesian, "mg", "to");
        add(temp, east_sudanic, "luo", "mas", "nus", "saq", "teo", "kln");
        add(temp, indic, "kok", "ks", "os");
        add(temp, niger_congo, "agq", "ak", "asa", "bas", "bem", "bez", "bm", "cgg", "dua", "dyo", "ebu", "ee", "ewo", "guz", "jgo", "kam", "ki", "kkj", "ksb", "ksf", "lag", "lg", "ln", "lu", "luy", "mua", "nd", "nnh", "nr", "nyn", "rn", "rof", "rw", "sbp", "sg", "ss", "tn", "ts", "vai", "ve", "dav", "jmc", "kde", "mer", "mgh", "mgo", "nmg", "nso", "rwk", "seh", "vun", "xog", "yav");
        add(temp, romance, "fur", "kea", "mfe");
        add(temp, sino_tibetan, "bo", "brx", "dz", "ii");
        add(temp, slavic, "dsb", "hsb");
        add(temp, songhay, "dje", "khq", "ses", "twq");
        add(temp, turkic, "sah");
        GROUP_LANGUAGE.freeze();
    }
    public static LanguageGroup get(ULocale locale) {
        LanguageGroup result = LANGUAGE_GROUP.get(locale);
        return result == null ? LanguageGroup.other : result;
    }
    public static Set<ULocale> getExplicit() {
        return LANGUAGE_GROUP.keySet();
    }
    public static Set<ULocale> getLocales(LanguageGroup group) {
        return GROUP_LANGUAGE.get(group);
    }
}