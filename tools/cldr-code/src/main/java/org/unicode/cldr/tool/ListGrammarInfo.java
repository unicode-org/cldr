package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Joiner;

public class ListGrammarInfo {
    public static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    public static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    public static final CLDRFile english = CONFIG.getEnglish();
    public static void main(String[] args) {
        Set<String> locales = GrammarInfo.getGrammarLocales();
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> sortedGenderLocales = new TreeSet<>();
        Set<String> sortedCaseLocales = new TreeSet<>();
        Set<String> sortedBothLocales = new TreeSet<>();

        for (String locale : locales) {
            if (locale.equals("root")) {
                continue;
            }
            ltp.set(locale);
            String region = ltp.getRegion();
            if (!region.isEmpty()) {
                continue;
            }
            GrammarInfo grammarInfo = SDI.getGrammarInfo(locale, true);
            if (grammarInfo == null || !grammarInfo.hasInfo(GrammaticalTarget.nominal)) {
                continue;
            }
            //CLDRFile cldrFile = factory.make(locale, true);

            Collection<String> genders = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalGender, GrammaticalScope.units);
            Collection<String> rawCases = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units);

            boolean hasGender = genders != null && genders.size() > 1;
            boolean hasCase = rawCases != null && rawCases.size() > 1;

            if (hasGender) {
                if (hasCase) {
                    sortedBothLocales.add(format(locale, genders, rawCases));
                } else {
                    sortedGenderLocales.add(format(locale, genders));
                }
            } else if (hasCase) {
                sortedCaseLocales.add(format(locale, rawCases));
            }
        }
        System.out.println("Gender\t" + Joiner.on(", ").join(sortedGenderLocales));
        System.out.println("Case\t" + Joiner.on(", ").join(sortedCaseLocales));
        System.out.println("Gender & Case\t" + Joiner.on(", ").join(sortedBothLocales));
    }

    private static String format(String locale, Collection<String> genders, Collection<String> rawCases) {
        return english.getName(locale) + " (" + locale + "/" + genders.size() + "×" + rawCases.size() + ")";
    }

    public static String format(String locale, Collection<String> genders) {
        return english.getName(locale) + " (" + locale + "/" + genders.size() + ")";
    }
}

