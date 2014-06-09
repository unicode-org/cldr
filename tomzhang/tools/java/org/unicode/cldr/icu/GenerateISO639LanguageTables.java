package org.unicode.cldr.icu;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.SupplementalDataInfo;

public class GenerateISO639LanguageTables {

    private static SupplementalDataInfo sdi = SupplementalDataInfo
        .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

    private static void generateLanguageTable(int size) {
        String tag = "LANGUAGES";
        System.out.println("/* Generated using org.unicode.cldr.icu.GenerateISO639LanguageTables */");
        if (size == 3) {
            tag = "LANGUAGES_3";
        }
        System.out.println("static const char * const " + tag + "[] = {");
        System.out.print("    ");
        String currentStartingLetter = "a";
        int languagesOnThisLine = 0;
        for (String language : sdi.getCLDRLanguageCodes()) {
            if (!language.startsWith(currentStartingLetter) || languagesOnThisLine > 7) {
                System.out.println();
                System.out.print("    ");
                currentStartingLetter = language.substring(0, 1);
                languagesOnThisLine = 0;
            }
            if (size == 2) {
                System.out.print("\"" + language + "\", ");
            } else {

                System.out.print("\""
                    + (Iso639Data.toAlpha3(language) != null ? Iso639Data.toAlpha3(language) : language) + "\", ");
            }
            if (language.length() == 2 && size == 2) {
                System.out.print(" ");
            }
            languagesOnThisLine++;
        }
    }

    private static void generateStructLocaleTxt() {
        System.out.println("    Languages{");
        for (String language : sdi.getCLDRLanguageCodes()) {
            System.out.println("        " + language + "{\"\"}");
        }
        System.out.println("    }");
    }

    public static void main(String[] args) {
        generateLanguageTable(2);
        System.out.println();
        generateLanguageTable(3);
        System.out.println();
        generateStructLocaleTxt();
    }

}