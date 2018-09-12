package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.LocaleMatcher.LanguageMatcherData;

public class LocaleMatcherShim {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

    static final SupplementalDataInfo INFO = CONFIG.getSupplementalDataInfo();
    static final LanguageMatcherData LANGUAGE_MATCHER_DATA = new LanguageMatcherData();
    static {
        for (R4<String, String, Integer, Boolean> foo : INFO.getLanguageMatcherData("written")) {
            LANGUAGE_MATCHER_DATA.addDistance(foo.get0(), foo.get1(), foo.get2(), foo.get3());
        }
        LANGUAGE_MATCHER_DATA.freeze();
    }

    public static LanguageMatcherData load() {
        return LANGUAGE_MATCHER_DATA;
    }
}
