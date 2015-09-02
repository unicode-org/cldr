package org.unicode.cldr.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.ChainedMap.M3;

import com.ibm.icu.util.ULocale;

public enum LanguageGroup {
    root("root"),
    germanic("gem"), celtic("cel"), romance("roa"), slavic("sla"), baltic("bat"), indic("inc"), other_indo("ine"), dravidian("dra"),
    uralic("urj"), cjk("und_Hani"), sino_tibetan("sit"), tai("tai"), austronesian("map"), turkic("trk"),
    afroasiatic("afa"), austroasiatic("aav"), niger_congo("nic"), east_sudanic("sdv"),
    songhay("son"), american("und_019"),
    art("art"), other("und");
    public final String iso;

    LanguageGroup(String iso) {
        this.iso = iso;
    }

    static final Map<ULocale, LanguageGroup> LANGUAGE_GROUP;
    static final M3<LanguageGroup, ULocale, Integer> GROUP_LANGUAGE = ChainedMap.of(new TreeMap<LanguageGroup, Object>(), new LinkedHashMap<ULocale, Object>(), Integer.class);

    private static void add(Map<ULocale, LanguageGroup> map, LanguageGroup group, String... baseLanguages) {
        int count = 0;
        for (String s : baseLanguages) {
            ULocale loc = new ULocale(s);
            if (map.put(loc, group) != null) {
                throw new IllegalArgumentException("duplicate: " + s + ", " + group);
            }
            ;
            GROUP_LANGUAGE.put(group, loc, count);
            ++count;
        }
    }

    static {
        LinkedHashMap<ULocale, LanguageGroup> temp = new LinkedHashMap<>();
        LANGUAGE_GROUP = Collections.unmodifiableMap(temp);
        add(temp, root, "root");
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
        add(temp, niger_congo, "agq", "ak", "asa", "bas", "bem", "bez", "bm", "cgg", "dua", "dyo", "ebu", "ee", "ewo", "guz", "jgo", "kam", "ki", "kkj", "ksb",
            "ksf", "lag", "lg", "ln", "lu", "luy", "mua", "nd", "nnh", "nr", "nyn", "rn", "rof", "rw", "sbp", "sg", "ss", "tn", "ts", "vai", "ve", "dav",
            "jmc", "kde", "mer", "mgh", "mgo", "nmg", "nso", "rwk", "seh", "vun", "xog", "yav");
        add(temp, romance, "fur", "kea", "mfe");
        add(temp, sino_tibetan, "bo", "brx", "dz", "ii");
        add(temp, slavic, "dsb", "hsb");
        add(temp, songhay, "dje", "khq", "ses", "twq");
        add(temp, turkic, "sah");
        //GROUP_LANGUAGE.freeze();
    }

    public static LanguageGroup get(ULocale locale) {
        return CldrUtility.ifNull(LANGUAGE_GROUP.get(locale), LanguageGroup.other);
    }

    public static Set<ULocale> getExplicit() {
        return Collections.unmodifiableSet(LANGUAGE_GROUP.keySet());
    }

    public static Set<ULocale> getLocales(LanguageGroup group) {
        return Collections.unmodifiableSet(GROUP_LANGUAGE.get(group).keySet());
    }
    
    /**
     * return position in group, or -1 if in no group
     * @param locale
     * @return
     */
    public static int rankInGroup(ULocale locale) {
        LanguageGroup group = LANGUAGE_GROUP.get(locale);
        if (group == null) {
            return Integer.MAX_VALUE;
        }
        return GROUP_LANGUAGE.get(group).get(locale);
    }

    public static Comparator<ULocale> COMPARATOR = new Comparator<ULocale>() {
        @Override
        public int compare(ULocale o1, ULocale o2) {
            LanguageGroup group1 = get(o1);
            LanguageGroup group2 = get(o2);
            int diff = group1.ordinal() - group2.ordinal();
            if (diff != 0) return diff;
            int r1 = rankInGroup(o1);
            int r2 = rankInGroup(o2);
            diff = r1 - r2;
            return diff != 0 ? diff : o1.compareTo(o2);
        }
    };
}