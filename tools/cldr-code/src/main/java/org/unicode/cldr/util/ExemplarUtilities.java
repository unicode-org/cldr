package org.unicode.cldr.util;

import com.ibm.icu.text.UnicodeSet;
import java.util.Set;
import org.unicode.cldr.tool.LikelySubtags;

/** Utilities for use in handling exemplars, both in checks and in tests. */
public class ExemplarUtilities {
    static LikelySubtags ls = new LikelySubtags();
    static LanguageTagParser ltp = new LanguageTagParser();

    public static synchronized String getLikelyScript(String locale) {
        String max = ls.maximize(locale);
        return ltp.set(max).getScript();
    }

    public static boolean nonNativeCharacterAllowed(String path, int cp) {
        if (CHARS_OK.contains(cp)) {
            return true;
        }
        Set<String> pathCheckSet = pathSegmentsOk.get(cp);
        if (pathCheckSet == null) {
            return false;
        }
        for (String pathCheck : pathCheckSet) {
            if (path.contains(pathCheck)) {
                return true;
            }
        }
        return false;
    }

    static UnicodeRelation<String> pathSegmentsOk = new UnicodeRelation<>();

    static {
        // need to add exceptions to CheckForExemplars
        pathSegmentsOk.add('\u03A9', "/unit[@type=\"electric-ohm\"]");
        pathSegmentsOk.add('\u03BC', "/unit[@type=\"length-micrometer\"]");
        pathSegmentsOk.add('\u03BC', "/unit[@type=\"mass-microgram\"]");
        pathSegmentsOk.add('\u03BC', "/unit[@type=\"duration-microsecond\"]");
        pathSegmentsOk.add('\u03BC', "//ldml/annotations/annotation[@cp=\"µ\"]");
        pathSegmentsOk.add('\u03BC', "/compoundUnit[@type=\"10p-6\"]/unitPrefixPattern");
        pathSegmentsOk.add('\u03C0', "/unit[@type=\"angle-radian\"]");
        pathSegmentsOk.add('\u03C9', "/compoundUnit[@type=\"10p-6\"]/unitPrefixPattern");
        pathSegmentsOk.add('\u0440', "/currency[@type=\"BYN\"]");
        pathSegmentsOk.add('\u0440', "/currency[@type=\"RUR\"]");
        pathSegmentsOk.add('\u10DA', "/currency[@type=\"GEL\"]");
        pathSegmentsOk.addAll(
                new UnicodeSet("[؉ ٪ ٫ ۰ ۱ ؉ا س ا س ٬ ٬ ؜ ؛  ]"),
                "//ldml/numbers/symbols[@numberSystem=\"arab");

        // need to fix data in locale files
        pathSegmentsOk.addAll(
                new UnicodeSet("[コサ割可合営得指月有満無申祝禁秘空割祝秘]"), "//ldml/annotations/annotation");
        pathSegmentsOk.addAll(
                new UnicodeSet("[ا ر ل ی]"), "//ldml/annotations/annotation[@cp=\"﷼\"]");
        pathSegmentsOk.addAll(
                new UnicodeSet("[Р а в д е з л о п р т у ы ь]"),
                "//ldml/annotations/annotation[@cp=\"🪬\"]");
        // ω Grek    lo; Laoo;
        // //ldml/units/unitLength[@type="short"]/unit[@type="electric-ohm"]/unitPattern[@count="other"];
        pathSegmentsOk.freeze();
    }

    static final UnicodeSet CHARS_OK = new UnicodeSet("[\u061C \u202F \\p{Sc}]").freeze();
}
