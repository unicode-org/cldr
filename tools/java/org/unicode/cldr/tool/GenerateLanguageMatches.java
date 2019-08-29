package org.unicode.cldr.tool;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.Transform;

public class GenerateLanguageMatches {
    private static final CLDRFile ENGLISH = CLDRConfig.getInstance().getEnglish();

    public static void main(String[] args) {

        SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();
//        StandardCodes sc = CLDRConfig.getInstance().getStandardCodes();
        Map<String, Map<LstrField, String>> lstr = StandardCodes.getLstregEnumRaw().get(LstrType.language);
        Set<String> locales = CLDRConfig.getInstance().getFullCldrFactory().getAvailableLanguages();
        List<R4<String, String, Integer, Boolean>> matchData = SDI.getLanguageMatcherData("written_new");
        Multimap<String,String> desiredToSupported = HashMultimap.create();
        for (R4<String, String, Integer, Boolean> item : matchData) {
            desiredToSupported.put(item.get0(), item.get1());
            if (!item.get3()) { // if not oneway
                desiredToSupported.put(item.get1(), item.get0());
            }
        }

        Multimap<String,String> macroToEncompassed = TreeMultimap.create();
        for (Entry<String, Map<LstrField, String>> localeInfo : lstr.entrySet()) {
            String locale = localeInfo.getKey();
            if (locale.contains("_")) {
                continue;
            }
            Map<LstrField, String> data = localeInfo.getValue();
            String macroLanguage = data.get(LstrField.Macrolanguage);

            if (macroLanguage != null && locales.contains(macroLanguage)) {
                // Filter out what is in LanguageInfo already
                if (desiredToSupported.containsEntry(locale, macroLanguage)) {
                    continue;
                }
                macroToEncompassed.put(macroLanguage, locale);
            }
        }
        String last = "";
        for ( Entry<String, String> entry : macroToEncompassed.entries()) {
            String macroLanguage = entry.getKey();
            if (!last.contentEquals(macroLanguage)) {
                System.out.println("<!-- " + getName(macroLanguage) + " -->");
            }
            String encompassed = entry.getValue();
            System.out.println("<languageMatch desired=\"" + encompassed
                + "\" supported=\"" + macroLanguage
                + "\" distance=\"10\" oneway=\"true\"/>\t"
                + "<!-- " + getName(encompassed) + " -->");
            last = macroLanguage;
        }
    }
    
    static final Transform<String, String> MENU = new Transform<String, String>() {
        public String transform(@SuppressWarnings("unused") String source) {
            return "menu";
        }
    };

    private static String getName(String lang) {
        return ENGLISH.getName(CLDRFile.LANGUAGE_NAME, lang, MENU);
    }

}
