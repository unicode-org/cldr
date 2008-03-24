package org.unicode.cldr.tool;

import java.util.Date;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;

public class CheckEnglishCurrencyNames {
  static SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
  static StandardCodes sc = StandardCodes.make();
  static Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
  static CLDRFile english = cldrFactory.make("en", true);

  public static void main(String[] args) {
    Set<String> currencyCodes = sc.getGoodAvailableCodes("currency");
    Relation<String,String> currencyCodesWithDates = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,String> modernCurrencyCodes = new Relation(new TreeMap(), TreeSet.class);
    Set<String> territoriesWithoutModernCurrencies = new TreeSet(sc.getGoodAvailableCodes("territory"));
    
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
        final String currency = dateInfo.getCurrency();
        final Date start = dateInfo.getStart();
        final Date end = dateInfo.getEnd();
        if (end == null) {
          modernCurrencyCodes.put(currency, territory);
          territoriesWithoutModernCurrencies.remove(territory);
        } else {
          currencyCodesWithDates.put(currency, territory);          
        }
        System.out.println("\t" + currency + "\t" + start + "\t" + end);
      }
    }
    System.out.println("Modern Codes: " + modernCurrencyCodes);
    for (String currency : modernCurrencyCodes.keySet()) {
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
    Set remainder = new TreeSet();
    remainder.addAll(currencyCodes);
    remainder.removeAll(currencyCodesWithDates.keySet());
    System.out.println("Currencies without Territories: " + remainder);
    System.out.println("Territories without Modern Currencies: " + territoriesWithoutModernCurrencies);
  }
}
