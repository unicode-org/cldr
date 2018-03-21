package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

public class FallbackIteratorDataGenerator {
    static CLDRConfig testInfo = ToolConfig.getToolInstance();

    public static void main(String[] args) {
        final StandardCodes sc = testInfo.getStandardCodes();
        List<String> decanonicalizeList = new ArrayList<String>();
        System.out.println();
        System.out.println("\t\t\"canonicalize\",\t\t// mechanically generated");
        System.out.println();

        for (String type : sc.getAvailableTypes()) {
            final boolean isLanguage = type.equals("language");
            final boolean isGrandfathered = type.equals("grandfathered");
            final boolean isRegion = type.equals("territory");
            final String canonicalizationFormat = isGrandfathered ? "\t\t\"%s;%s\","
                : isLanguage ? "\t\t\"%s(-.*)?;%s$1\","
                    : isRegion ? "\t\t\"(.*-)%s(-.*)?;$1%s$2\","
                        : null;
            if (canonicalizationFormat == null) {
                continue;
            }
            System.out.println("\t\t// " + type);
            for (String code : sc.getAvailableCodes(type)) {
                List<String> data = sc.getFullData(type, code);
                String canonicalValue = data.get(2);

                String special = specialCanonicalValue.get(code);
                if (special != null) {
                    canonicalValue = special;
                }

                if (canonicalValue == null || canonicalValue.length() == 0) {
                    // System.out.println("\t\t\\\\ skipping " + code);
                    if (isGrandfathered) {
                        System.out.println("\t\t// Grandfathered code with no replacement " + code);
                        continue;
                    } else {
                        continue;
                    }
                }
                if (canonicalValue.startsWith("deprecated")) {
                    System.out.println("\t\t// skipping " + code + ", deprecated but no replacement");
                    continue;
                }
                System.out.format(canonicalizationFormat, code, canonicalValue);
                if (special != null) {
                    System.out.print("\t\t// Grandfathered code with special replacement: " + code);
                }
                System.out.println();
                if (!isGrandfathered) {
                    decanonicalizeList.add(String.format(canonicalizationFormat, canonicalValue, code));
                }
            }
        }
        // now look for languages with multiple scripts
        SupplementalDataInfo supplemental = testInfo.getSupplementalDataInfo();
        for (String lang : supplemental.getLanguagesForTerritoriesPopulationData()) {
            if (lang.contains("_")) {
                System.out.println(lang);
            }
        }
        System.out.println();
        System.out.println("\t\t\"decanonicalize\",\t\t// mechanically generated");
        System.out.println();
        for (String item : decanonicalizeList) {
            System.out.println(item);
        }
    }

    static Map<String, String> specialCanonicalValue = CldrUtility.asMap(new String[][] {
        { "cel-gaulish", "xcg" },
        { "en-GB-oed", "en-GB-x-oed" },
        { "i-default", "und" },
        { "i-enochian", "x-enochian" },
        { "i-mingo", "see" },
        { "zh-min", "nan" },
    });

}
