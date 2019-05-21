package org.unicode.cldr.tool;

import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DateTimeFormats;
import org.unicode.cldr.util.Factory;

import com.ibm.icu.text.ListFormat;
import com.ibm.icu.text.SimpleDateFormat;

public class GenerateSeedDurations {
    /*
     * <numericUnits>
     * <numericUnit type="ms">mm:ss</numericUnit> <!-- 33:59 -->
     * <numericUnit type="Hm">HH:mm</numericUnit> <!-- 33:59 -->
     * <numericUnit type="Hms">HH:mm:ss</numericUnit> <!-- 33:59:59 -->
     * </numericUnits>
     * <combinationUnits>
     * <combinationUnit type="ms">{0}, {1}<combinationUnit> <!-- 3 minutes, 15 seconds -->
     * <combinationUnit type="Hm">{0}, {1}<combinationUnit> <!-- 3 hours, 15 minutes -->
     * <combinationUnit type="Hms">{0}, {1}, and {2}<combinationUnit> <!-- 3 hours, 15 minutes, and 25 seconds -->
     * <combinationUnit type="dH">{0}, {1}<combinationUnit> <!-- 3 days, 15 hours -->
     * <combinationUnit type="dHm">{0}, {1}, and {2}<combinationUnit> <!-- 3 days, 15 hours, and 35 minutes -->
     * <combinationUnit type="wd">{0}, {1}<combinationUnit> <!-- 3 weeks, 5 days -->
     * <combinationUnit type="md">{0}, {1}<combinationUnit> <!-- 3 months, 5 days -->
     * <combinationUnit type="ym">{0}, {1}<combinationUnit> <!-- 3 years, 11 months -->
     * <combinationUnit type="ymd">{0}, {1}, and {2}<combinationUnit> <!-- 3 years, 11 months, and 30 days -->
     * </combinationUnits>
     */
    static CLDRConfig testInfo = ToolConfig.getToolInstance();
    static String[] numericUnits = {
        "ms", "Hm", "Hms"
    };
    static String[] combinationUnits = {
        "ms", "Hm", "Hms", "dH", "dHm", "wd", "md", "ym", "ymd"
    };

    /*
     * Seed these values using:
     * for numeric fields, the current time separators
     * for the others, the current list separators
     */
    public static void main(String[] args) {
        Factory cldrFactory = testInfo.getCldrFactory();
        String[] data = new String[4];
        Set<String> warnings = new LinkedHashSet<String>();
        for (String locale : cldrFactory.getAvailableLanguages()) {
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            String localeString = locale + "\t" + testInfo.getEnglish().getName(locale);
            System.out.println("\n" + localeString);

            DateTimeFormats formats = new DateTimeFormats().set(cldrFile, "gregorian");
            System.out.println("    <numericUnits>");
            for (String numericUnit : numericUnits) {
                SimpleDateFormat pattern = formats.getDateFormatFromSkeleton(numericUnit);
                String patternString = pattern.toPattern();
                if (numericUnit.contains("H")) {
                    if (!patternString.contains("H")) {
                        warnings.add(localeString + "\t" + patternString + "\t ***No 'H'");
                    }
                    patternString = patternString.replace("HH", "H");
                } else {
                    patternString = patternString.replace("mm", "m");
                }
                if (!patternString.contains(":")) {
                    warnings.add(localeString + "\t" + patternString + "\t ***No ':'");
                }
                System.out.println("        <numericUnit type=\"" + numericUnit + "\">" + patternString
                    + "<numericUnit>");
            }
            System.out.println("    </numericUnits>");

            for (int i = 0; i < ExtractListInfo.paths.length; ++i) {
                data[i] = cldrFile.getStringValue(ExtractListInfo.paths[i]);
            }
            ListFormat listFormat = new ListFormat(data[0], data[1], data[2], data[3]);

            System.out.println("    <combinationUnits>");
            for (String combinationUnit : combinationUnits) {
                String pattern = combinationUnit.length() == 2
                    ? listFormat.format("{0}", "{1}")
                    : listFormat.format("{0}", "{1}", "{2}");
                System.out.println("        <combinationUnit type=\"" + combinationUnit + "\">" + pattern
                    + "<combinationUnit>");
            }
            System.out.println("    </combinationUnits>");
        }
        for (String warning : warnings) {
            System.out.println(warning);
        }
    }
}
