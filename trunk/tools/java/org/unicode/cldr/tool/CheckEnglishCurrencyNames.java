package org.unicode.cldr.tool;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.impl.Relation;

public class CheckEnglishCurrencyNames {
    static SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo
        .getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
    static StandardCodes sc = StandardCodes.make();
    static Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
    static CLDRFile english = cldrFactory.make("en", true);

    public static void main(String[] args) {
        Date now = new Date();
        Set<String> currencyCodes = sc.getGoodAvailableCodes("currency");
        Relation<String, String> currencyCodesWithDates = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Relation<String, String> modernCurrencyCodes2territory = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Set<String> territoriesWithoutModernCurrencies = new TreeSet<String>(sc.getGoodAvailableCodes("territory"));

        for (String territory : sc.getGoodAvailableCodes("territory")) {
            if (supplementalDataInfo.getContained(territory) != null) {
                territoriesWithoutModernCurrencies.remove(territory);
                continue;
            }
            System.out.println(territory);
            Set<CurrencyDateInfo> currencyInfo = supplementalDataInfo.getCurrencyDateInfo(territory);
            if (currencyInfo == null) {
                System.out.println("\tNONE");
                continue;
            }
            for (CurrencyDateInfo dateInfo : currencyInfo) {
                if (!dateInfo.isLegalTender()) {
                    continue;
                }
                final String currency = dateInfo.getCurrency();
                final Date start = dateInfo.getStart();
                final Date end = dateInfo.getEnd();
                if (end.compareTo(now) >= 0) {
                    modernCurrencyCodes2territory.put(currency, territory);
                    territoriesWithoutModernCurrencies.remove(territory);
                } else {
                    currencyCodesWithDates.put(currency, territory);
                }
                System.out.println("\t" + currency + "\t" + start + "\t" + end);
            }
        }
        System.out.println("Modern Codes: " + modernCurrencyCodes2territory);
        for (String currency : modernCurrencyCodes2territory.keySet()) {
            final String name = english.getName(CLDRFile.CURRENCY_NAME, currency).toLowerCase();
            if (name.contains("new") || name.contains("old")) {
                System.out.println(currency + "\t" + name);
            }
        }
        System.out.println("Non-Modern Codes (with dates): " + currencyCodesWithDates);
        for (String currency : currencyCodesWithDates.keySet()) {
            final String name = english.getName(CLDRFile.CURRENCY_NAME, currency).toLowerCase();
            if (name.contains("new") || name.contains("old")) {
                System.out.println(currency + "\t" + name);
            }
        }
        Set<String> remainder = new TreeSet<String>();
        remainder.addAll(currencyCodes);
        remainder.removeAll(currencyCodesWithDates.keySet());
        System.out.println("Currencies without Territories: " + remainder);
        System.out.println("Territories without Modern Currencies: " + territoriesWithoutModernCurrencies);

        Relation<String, String> territory2official = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);

        // finding official languages
        for (String language : supplementalDataInfo.getLanguagesForTerritoriesPopulationData()) {
            for (String territory : supplementalDataInfo.getTerritoriesForPopulationData(language)) {
                PopulationData populationData = supplementalDataInfo.getLanguageAndTerritoryPopulationData(language,
                    territory);
                OfficialStatus status = populationData.getOfficialStatus();
                switch (status) {
                case official:
                case de_facto_official:
                case recognized:
                    territory2official.put(territory, language);
                }
            }
        }
        Relation<String, String> currency2symbols = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Map<String, Relation<String, String>> currency2symbol2locales = new TreeMap<String, Relation<String, String>>(); //
        System.out.format("Raw usage data\n");
        Set<String> noOfficialLanguages = new TreeSet<String>();

        for (Entry<String, Set<String>> currencyAndTerritories : modernCurrencyCodes2territory.keyValuesSet()) {
            String currency = currencyAndTerritories.getKey();
            Set<String> territories = currencyAndTerritories.getValue();
            for (String territory : territories) {
                final Set<String> languages = territory2official.get(territory);
                if (languages == null) {
                    noOfficialLanguages.add(territory);
                    continue;
                }
                for (String language : languages) {
                    CLDRFile nativeLanguage = null;
                    String locale = language + "_" + territory;
                    try {
                        nativeLanguage = cldrFactory.make(locale, true);
                    } catch (Exception e) {
                        try {
                            nativeLanguage = cldrFactory.make(language, true);
                        } catch (Exception e1) {
                        }
                    }
                    String symbol = nativeLanguage == null ? "N/A" : nativeLanguage.getName(CLDRFile.CURRENCY_SYMBOL,
                        currency);
                    System.out.println(
                        currency + "\t" + english.getName(CLDRFile.CURRENCY_NAME, currency)
                            + "\t" + territory + "\t" + english.getName(CLDRFile.TERRITORY_NAME, territory)
                            + "\t" + language + "\t" + english.getName(language)
                            + "\t" + symbol);
                    // TODO add script
                    if (nativeLanguage != null) {
                        currency2symbols.put(currency, symbol);
                        Relation<String, String> rel = currency2symbol2locales.get(currency);
                        if (rel == null) {
                            currency2symbol2locales.put(currency,
                                rel = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
                        }
                        rel.put(symbol, locale);
                    }
                }
            }
        }
        System.out.format("No official languages\n");
        for (String territory : noOfficialLanguages) {
            System.out.println(territory + "\t" + english.getName(CLDRFile.TERRITORY_NAME, territory));
        }
        System.out.format("Collected usage data\n");
        for (Entry<String, Set<String>> currencyAndSymbols : currency2symbols.keyValuesSet()) {
            String currency = currencyAndSymbols.getKey();
            Set<String> symbols = currencyAndSymbols.getValue();
            System.out.println(currency + "\t" + english.getName(CLDRFile.CURRENCY_NAME, currency)
                + "\t" + symbols.size()
                + "\t" + symbols
                + "\t" + currency2symbol2locales.get(currency));
        }

    }
}
