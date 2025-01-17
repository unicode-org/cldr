package org.unicode.cldr.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ExtraPaths {

    private static final Map<NameType, ExtraPaths> instances = new HashMap<>();

    public static ExtraPaths getInstance(NameType nameType) {
        return instances.computeIfAbsent(nameType, ExtraPaths::new);
    }

    private final NameType nameType;
    private final Collection<String> paths;

    private ExtraPaths(NameType nameType) {
        this.nameType = nameType;
        paths = new HashSet<>();
    }

    void append(Collection<String> toAddTo) {
        if (paths.isEmpty()) {
            populatePaths();
        }
        toAddTo.addAll(paths);
    }

    private void populatePaths() {
        // TODO: https://unicode-org.atlassian.net/browse/CLDR-17014
        // StandardCodes.CodeType codeType = StandardCodes.CodeType.fromNameType(nameType);
        // See https://github.com/unicode-org/cldr/pull/4287
        StandardCodes.CodeType codeType;
        switch (nameType) {
            case LANGUAGE:
                codeType = StandardCodes.CodeType.language;
                break;
            case SCRIPT:
                codeType = StandardCodes.CodeType.script;
                break;
            default:
                throw new IllegalArgumentException("TODO: CodeType.fromNameType");
        }
        StandardCodes sc = StandardCodes.make();
        Set<String> codes = new TreeSet<>(sc.getGoodAvailableCodes(codeType));
        adjustCodeSet(codes);
        for (String code : codes) {
            paths.add(nameType.getKeyPath(code));
        }
        addAltPaths();
    }

    private void adjustCodeSet(Set<String> codes) {
        if (nameType == NameType.LANGUAGE) {
            codes.remove(LocaleNames.ROOT);
            codes.addAll(
                    List.of(
                            "ar_001", "de_AT", "de_CH", "en_AU", "en_CA", "en_GB", "en_US",
                            "es_419", "es_ES", "es_MX", "fa_AF", "fr_CA", "fr_CH", "frc", "hi_Latn",
                            "lou", "nds_NL", "nl_BE", "pt_BR", "pt_PT", "ro_MD", "sw_CD", "zh_Hans",
                            "zh_Hant"));
        }
    }

    private void addAltPaths() {
        switch (nameType) {
            case LANGUAGE:
                addAltPath("en_GB", "short");
                addAltPath("en_US", "short");
                addAltPath("az", "short");
                addAltPath("ckb", "menu");
                addAltPath("ckb", "variant");
                addAltPath("hi_Latn", "variant");
                addAltPath("yue", "menu");
                addAltPath("zh", "menu");
                addAltPath("zh_Hans", "long");
                addAltPath("zh_Hant", "long");
                break;
            case SCRIPT:
                addAltPath("Hans", "stand-alone");
                addAltPath("Hant", "stand-alone");
        }
    }

    private void addAltPath(String code, String alt) {
        String fullpath = nameType.getKeyPath(code);
        // Insert the @alt= string after the last occurrence of "]"
        StringBuilder fullpathBuf = new StringBuilder(fullpath);
        String altPath =
                fullpathBuf
                        .insert(fullpathBuf.lastIndexOf("]") + 1, "[@alt=\"" + alt + "\"]")
                        .toString();
        paths.add(altPath);
    }
}
