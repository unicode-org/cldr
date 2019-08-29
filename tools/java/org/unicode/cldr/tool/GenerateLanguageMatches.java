package org.unicode.cldr.tool;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class GenerateLanguageMatches {
    public static void main(String[] args) {
        
//        SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();
//        StandardCodes sc = CLDRConfig.getInstance().getStandardCodes();
        Map<String, Map<LstrField, String>> lstr = StandardCodes.getLstregEnumRaw().get(LstrType.language);
        
        Set<String> locales = CLDRConfig.getInstance().getFullCldrFactory().getAvailableLanguages();
        
        Multimap<String,String> macroToEncompassed = TreeMultimap.create();
        for (Entry<String, Map<LstrField, String>> localeInfo : lstr.entrySet()) {
            String locale = localeInfo.getKey();
            Map<LstrField, String> data = localeInfo.getValue();
            String macroLanguage = data.get(LstrField.Macrolanguage);
            
            if (macroLanguage != null && locales.contains(macroLanguage)) {
                // TODO filter out what is in LanguageInfo already
                macroToEncompassed.put(macroLanguage, locale);
            }
        }
        String last = "";
        for ( Entry<String, String> entry : macroToEncompassed.entries()) {
            String macroLanguage = entry.getKey();
            if (!last.contentEquals(macroLanguage)) {
                System.out.println("<!-- " + CLDRConfig.getInstance().getEnglish().getName(macroLanguage) + " -->");
            }
            String encompassed = entry.getValue();
            System.out.println("<languageMatch desired=\"" + encompassed
                + "\" supported=\"" + macroLanguage
                + "\" distance=\"10\" oneway=\"true\"/>\t"
                + "<!-- " + CLDRConfig.getInstance().getEnglish().getName(encompassed) + " -->");
            last = macroLanguage;
        }
    }

}
