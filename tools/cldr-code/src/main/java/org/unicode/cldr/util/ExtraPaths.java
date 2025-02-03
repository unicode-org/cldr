package org.unicode.cldr.util;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ExtraPaths {

    public static void add(Collection<String> toAddTo) {
        toAddTo.addAll(SingletonHelper.INSTANCE.paths);
    }

    private static class SingletonHelper {
        private static final Singleton INSTANCE = new Singleton();
    }

    private static class Singleton {
        private final Collection<String> paths;
        private Collection<String> pathsTemp;

        Singleton() {
            pathsTemp = new TreeSet<>();
            addPaths(NameType.SCRIPT);
            addPaths(NameType.LANGUAGE);
            paths = ImmutableSet.copyOf(pathsTemp); // preserves order (Sets.copyOf doesn't)
            pathsTemp = null;
        }

        private void addPaths(NameType nameType) {
            StandardCodes.CodeType codeType = nameType.toCodeType();
            StandardCodes sc = StandardCodes.make();
            Set<String> codes = new TreeSet<>(sc.getGoodAvailableCodes(codeType));
            adjustCodeSet(codes, nameType);
            for (String code : codes) {
                pathsTemp.add(nameType.getKeyPath(code));
            }
            addAltPaths(nameType);
        }

        private void adjustCodeSet(Set<String> codes, NameType nameType) {
            if (nameType == NameType.LANGUAGE) {
                codes.remove(LocaleNames.ROOT);
                codes.addAll(
                        List.of(
                                "ar_001", "de_AT", "de_CH", "en_AU", "en_CA", "en_GB", "en_US",
                                "es_419", "es_ES", "es_MX", "fa_AF", "fr_CA", "fr_CH", "frc",
                                "hi_Latn", "lou", "nds_NL", "nl_BE", "pt_BR", "pt_PT", "ro_MD",
                                "sw_CD", "zh_Hans", "zh_Hant"));
            }
        }

        private void addAltPaths(NameType nameType) {
            switch (nameType) {
                case LANGUAGE:
                    addAltPath("en_GB", "short", nameType);
                    addAltPath("en_US", "short", nameType);
                    addAltPath("az", "short", nameType);
                    addAltPath("ckb", "menu", nameType);
                    addAltPath("ckb", "variant", nameType);
                    addAltPath("hi_Latn", "variant", nameType);
                    addAltPath("yue", "menu", nameType);
                    addAltPath("zh", "menu", nameType);
                    addAltPath("zh_Hans", "long", nameType);
                    addAltPath("zh_Hant", "long", nameType);
                    break;
                case SCRIPT:
                    addAltPath("Hans", "stand-alone", nameType);
                    addAltPath("Hant", "stand-alone", nameType);
            }
        }

        private void addAltPath(String code, String alt, NameType nameType) {
            String fullpath = nameType.getKeyPath(code);
            // Insert the @alt= string after the last occurrence of "]"
            StringBuilder fullpathBuf = new StringBuilder(fullpath);
            String altPath =
                    fullpathBuf
                            .insert(fullpathBuf.lastIndexOf("]") + 1, "[@alt=\"" + alt + "\"]")
                            .toString();
            pathsTemp.add(altPath);
        }
    }
}
