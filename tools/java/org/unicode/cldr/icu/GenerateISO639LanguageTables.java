package org.unicode.cldr.icu;

import org.unicode.cldr.util.Iso639Data;

public class GenerateISO639LanguageTables {

    private static void generateLanguageTable(int size) {
        String tag = "LANGUAGES";
        System.out.println("/* Generated using org.unicode.cldr.icu.GenerateISO639LanguageTables */");
        System.out.println("/* ISO639 table version is " + Iso639Data.getVersion() + " */");
        if (size == 3) {
            tag = "LANGUAGES_3";
        }
        System.out.println("static const char * const " + tag + "[] = {");
        System.out.print("    ");
        String currentStartingLetter = "a";
        int languagesOnThisLine = 0;
        for (String language : Iso639Data.getAvailable()) {
            if (!language.startsWith(currentStartingLetter) || languagesOnThisLine > 7) {
                System.out.println();
                System.out.print("    ");
                currentStartingLetter = language.substring(0, 1);
                languagesOnThisLine = 0;
            }
            if (size == 2) {
                System.out.print("\"" + language + "\", ");
            } else {
                
                System.out.print("\"" + ( Iso639Data.toAlpha3(language) != null ? Iso639Data.toAlpha3(language) : language ) + "\", ");
            }
            if (language.length() == 2 && size == 2) {
                System.out.print(" ");
            }
            languagesOnThisLine++;
        }
    }
    private static void generateStructLocaleTxt() {
        System.out.println("    Languages{");
        for (String language : Iso639Data.getAvailable()) {
            System.out.println("        "+language+"{\"\"}");
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