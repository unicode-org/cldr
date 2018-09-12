package org.unicode.cldr.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.ChainedMap.M3;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.util.ULocale;

public enum LanguageGroup {
    root("und"), germanic("gem"), celtic("cel"), romance("roa"), slavic("sla"), baltic("bat"), indic("inc"), other_indo("ine_001"), dravidian("dra"), uralic(
        "urj"), cjk("und_Hani"), sino_tibetan("sit"), tai("tai"), austronesian("map"), turkic("trk"), afroasiatic(
            "afa"), austroasiatic("aav"), niger_congo("nic"), east_sudanic("sdv"), songhay("son"), american("und_019"), art("art"), other("und_001");

    public final String iso;

    LanguageGroup(String iso) {
        this.iso = iso;
    }

    static final Map<ULocale, LanguageGroup> LANGUAGE_GROUP;
    static final M3<LanguageGroup, ULocale, Integer> GROUP_LANGUAGE = ChainedMap.of(new TreeMap<LanguageGroup, Object>(), new LinkedHashMap<ULocale, Object>(),
        Integer.class);

    private static void add(Map<ULocale, LanguageGroup> map, LanguageGroup group, String... baseLanguages) {
        Map<ULocale, Integer> soFar = GROUP_LANGUAGE.get(group);
        int count = soFar == null ? 0 : soFar.size();
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
        add(temp, germanic, "en", "fy", "nl", "af", "de", "gsw", "wae", "ksh", "lb", "sv", "da", "nb", "nn", "fo", "is", "yi");
        add(temp, celtic, "ga", "gd", "cy", "gv", "kw", "br");
        add(temp, romance, "fr", "pt", "gl", "es", "ca", "ast", "it", "rm", "ro");
        add(temp, slavic, "pl", "cs", "sk", "sl", "hr", "bs", "mk", "sr", "bg", "ru", "be", "uk");
        add(temp, baltic, "lt", "lv");
        add(temp, other_indo, "el", "hy", "sq", "fa", "ps", "os");
        add(temp, indic, "ur", "hi", "gu", "sd", "bn", "as", "ccp", "or", "mr", "ne", "pa", "si");
        add(temp, dravidian, "ta", "te", "ml", "kn");
        add(temp, cjk, "zh", "yue", "ja", "ko");
        add(temp, turkic, "tr", "az", "tk", "kk", "ky", "uz", "ug");
        add(temp, uralic, "hu", "fi", "et", "se", "smn");
        add(temp, afroasiatic, "ar", "mt", "he", "om", "so", "ha", "am", "tzm", "zgh");
        add(temp, tai, "th", "lo");
        add(temp, austronesian, "id", "ms", "jv", "fil", "haw");
        add(temp, austroasiatic, "vi", "km");
        add(temp, niger_congo, "sw", "swc", "yo", "ig", "ff", "sn", "zu");
        add(temp, other, "ka", "eu", "mn", "naq");
        add(temp, sino_tibetan, "my");
        add(temp, afroasiatic, "aa", "kab", "shi", "ssy", "ti");
        add(temp, american, "chr", "kl", "lkt", "qu");
        add(temp, art, "eo", "vo", "ia");
        add(temp, austronesian, "mg", "to");
        add(temp, east_sudanic, "luo", "mas", "nus", "saq", "teo", "kln");
        add(temp, indic, "kok", "ks");
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
        return CldrUtility.ifNull(LANGUAGE_GROUP.get(new ULocale(locale.getLanguage())), LanguageGroup.other);
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
        locale = new ULocale(locale.getLanguage());
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

    public static void main(String[] args) {
        CLDRFile english = CLDRConfig.getInstance().getEnglish();
        System.out.print("<supplementalData>\n"
            + "\t<version number=\"$Revision:$\"/>\n"
            + "\t<languageGroups>\n");
        for (LanguageGroup languageGroup : LanguageGroup.values()) {
            Set<ULocale> locales = LanguageGroup.getLocales(languageGroup);
            String englishName = languageGroup.getName(english);
            System.out.print("\t\t<languageGroup id=\"" + languageGroup.iso
                + "\" code=\"" + CollectionUtilities.join(locales, ", ")
                + "\"/>\t<!-- " + englishName + " -->\n");
        }
        System.out.print("\t</languageGroups>"
            + "\n<supplementalData>\n");
    }

    public String getName(CLDRFile cldrFile) {
        String prefix = "";
        LanguageTagParser ltp = new LanguageTagParser().set(iso);
        switch (ltp.getRegion()) {
        case "001":
            if (ltp.getLanguage().equals("und")) {
                return "Other";
            }
            prefix = "Other ";
            break;
        case "":
            break;
        default:
            return cldrFile.getName(CLDRFile.TERRITORY_NAME, ltp.getRegion());
        }
        switch (ltp.getScript()) {
        case "Hani":
            return "CJK";
        case "":
            break;
        default:
            throw new IllegalArgumentException("Need to fix code: " + ltp.getScript());
        }
        return prefix + cldrFile.getName(ltp.getLanguage()).replace(" [Other]", "").replace(" languages", "");
    }

    @Override
    public String toString() {
        return getName(CLDRConfig.getInstance().getEnglish());
    }
}