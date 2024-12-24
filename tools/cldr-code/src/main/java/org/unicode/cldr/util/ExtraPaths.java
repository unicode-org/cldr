package org.unicode.cldr.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ExtraPaths {

    private static final Collection<String> languagePaths = new HashSet<>();

    static void appendLanguages(Collection<String> toAddTo) {
        if (languagePaths.isEmpty()) {
            populateLanguagePaths();
        }
        toAddTo.addAll(languagePaths);
    }

    private static void populateLanguagePaths() {
        Set<String> codes = new TreeSet<>();
        StandardCodes sc = StandardCodes.make();
        codes.addAll(sc.getGoodAvailableCodes(StandardCodes.CodeType.language));
        codes.remove(LocaleNames.ROOT);
        codes.addAll(
                List.of(
                        "ar_001", "de_AT", "de_CH", "en_AU", "en_CA", "en_GB", "en_US", "es_419",
                        "es_ES", "es_MX", "fa_AF", "fr_CA", "fr_CH", "frc", "hi_Latn", "lou",
                        "nds_NL", "nl_BE", "pt_BR", "pt_PT", "ro_MD", "sw_CD", "zh_Hans",
                        "zh_Hant"));
        for (String code : codes) {
            languagePaths.add(NameType.LANGUAGE.getKeyPath(code));
        }
        languagePaths.add(languageAltPath("en_GB", "short"));
        languagePaths.add(languageAltPath("en_US", "short"));
        languagePaths.add(languageAltPath("az", "short"));
        languagePaths.add(languageAltPath("ckb", "menu"));
        languagePaths.add(languageAltPath("ckb", "variant"));
        languagePaths.add(languageAltPath("hi_Latn", "variant"));
        languagePaths.add(languageAltPath("yue", "menu"));
        languagePaths.add(languageAltPath("zh", "menu"));
        languagePaths.add(languageAltPath("zh_Hans", "long"));
        languagePaths.add(languageAltPath("zh_Hant", "long"));
    }

    private static String languageAltPath(String code, String alt) {
        String fullpath = NameType.LANGUAGE.getKeyPath(code);
        // Insert the @alt= string after the last occurrence of "]"
        StringBuilder fullpathBuf = new StringBuilder(fullpath);
        return fullpathBuf
                .insert(fullpathBuf.lastIndexOf("]") + 1, "[@alt=\"" + alt + "\"]")
                .toString();
    }
}
