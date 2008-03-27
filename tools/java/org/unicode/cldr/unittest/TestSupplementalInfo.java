package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;

import com.ibm.icu.dev.test.TestFmwk;

public class TestSupplementalInfo extends TestFmwk {
  static TestInfo testInfo = new TestInfo();

  public static void main(String[] args) {
    new TestSupplementalInfo().run(args);
  }
  
  public void TestCompleteness() {
    assertEquals("API doesn't support: " + testInfo.supplementalDataInfo.getSkippedElements(), 0, testInfo.supplementalDataInfo.getSkippedElements().size());
  }

  // these are settings for exceptional cases we want to allow
  private static final Set<String> EXCEPTION_CURRENCIES_WITH_NEW = new TreeSet<String>(Arrays.asList("NZD", "PGK"));
  
  // ok since there is no problem with confusion
  private static final Set<String> OK_TO_NOT_HAVE_OLD = new TreeSet<String>(Arrays.asList(
          "ADP", "ATS", "BEF", "CYP", "DEM", "ESP", "FIM", "FRF", "GRD", "IEP", "ITL", "LUF", "MTL", "MTP",
          "NLG", "PTE", "YUM", "ARA", "BAD", "BGL", "BOP", "BRC", "BRN", "BRR", "BUK", "CSK", "ECS", "GEK", "GNS",
          "GQE", "HRD", "ILP", "LTT", "LVR", "MGF", "MLF", "MZE", "NIC", "PEI", "PES", "SIT", "SRG", "SUR",
          "TJR", "TPE", "UAK", "YUD", "YUN", "ZRZ", "GWE"));
  
  private static final Date LIMIT_FOR_NEW_CURRENCY = new Date(new Date().getYear() - 5, 1, 1);
  private static final Date NOW = new Date();
  private Matcher oldMatcher = Pattern.compile("\\bold\\b|\\([0-9]{4}-[0-9]{4}\\)",Pattern.CASE_INSENSITIVE).matcher("");
  private Matcher newMatcher = Pattern.compile("\\bnew\\b",Pattern.CASE_INSENSITIVE).matcher("");
  
  /**
   * Test that access to currency info in supplemental data is ok. At this point just a simple test.
   * @param args
   */
  public void TestCurrency() {
    Set<String> currencyCodes = testInfo.sc.getGoodAvailableCodes("currency");
    Relation<String,Pair<String, CurrencyDateInfo>> nonModernCurrencyCodes = new Relation(new TreeMap(), TreeSet.class);
    Relation<String,Pair<String, CurrencyDateInfo>> modernCurrencyCodes = new Relation(new TreeMap(), TreeSet.class);
    Set<String> territoriesWithoutModernCurrencies = new TreeSet(testInfo.sc.getGoodAvailableCodes("territory"));
    Map<String,Date> currencyFirstValid = new TreeMap();
    Map<String,Date> currencyLastValid = new TreeMap();
    territoriesWithoutModernCurrencies.remove("ZZ");
    
    for (String territory : testInfo.sc.getGoodAvailableCodes("territory")) {
      if (testInfo.supplementalDataInfo.getContained(territory) != null) {
        territoriesWithoutModernCurrencies.remove(territory);
        continue;
      }
      Set<CurrencyDateInfo> currencyInfo = testInfo.supplementalDataInfo.getCurrencyDateInfo(territory);
      if (currencyInfo == null) {
        continue; // error, but will pick up below.
      }
      for (CurrencyDateInfo dateInfo : currencyInfo) {
        final String currency = dateInfo.getCurrency();
        final Date start = dateInfo.getStart();
        final Date end = dateInfo.getEnd();
        if (dateInfo.getErrors().length() != 0) {
          errln("parsing " + territory + "\t" + dateInfo.toString() + "\t" + dateInfo.getErrors());
        }
        Date firstValue = currencyFirstValid.get(currency);
        if (firstValue == null || firstValue.compareTo(start) < 0) {
          currencyFirstValid.put(currency, start);
        } 
        Date lastValue = currencyLastValid.get(currency);
        if (lastValue == null || lastValue.compareTo(end) > 0) {
          currencyLastValid.put(currency, end);
        }         
        if (end.compareTo(NOW) >= 0) {
          modernCurrencyCodes.put(currency, new Pair<String, CurrencyDateInfo>(territory, dateInfo));
          territoriesWithoutModernCurrencies.remove(territory);
        } else {
          nonModernCurrencyCodes.put(currency, new Pair<String, CurrencyDateInfo>(territory, dateInfo));          
        }
        logln(territory + "\t" + dateInfo.toString() + "\t" + testInfo.english.getName(CLDRFile.CURRENCY_NAME, currency));
      }
    }
    // fix up 
    nonModernCurrencyCodes.removeAll(modernCurrencyCodes.keySet());
    // now print error messages
    logln("Modern Codes: " + modernCurrencyCodes);
    for (String currency : modernCurrencyCodes.keySet()) {
      final String name = testInfo.english.getName(CLDRFile.CURRENCY_NAME, currency);
      if (oldMatcher.reset(name).find()) {
        errln("Has 'old' in name but still used " + "\t" + currency + "\t" + name + "\t" + modernCurrencyCodes.getAll(currency));
      }
      if (newMatcher.reset(name).find() && !EXCEPTION_CURRENCIES_WITH_NEW.contains(currency)) {
        // find the first use. If older than 5 years, flag as error
        if (currencyFirstValid.get(currency).compareTo(LIMIT_FOR_NEW_CURRENCY) < 0) {   
          errln("Has 'new' in name but used since " + CurrencyDateInfo.formatDate(currencyFirstValid.get(currency)) + "\t" + currency + "\t" + name + "\t" + modernCurrencyCodes.getAll(currency));
        } else {
          logln("Has 'new' in name but used since " + CurrencyDateInfo.formatDate(currencyFirstValid.get(currency)) + "\t" + currency + "\t" + name + "\t" + modernCurrencyCodes.getAll(currency));
        }
      }
    }
    logln("Non-Modern Codes (with dates): " + nonModernCurrencyCodes);
    for (String currency : nonModernCurrencyCodes.keySet()) {
      final String name = testInfo.english.getName(CLDRFile.CURRENCY_NAME, currency);
      if (newMatcher.reset(name).find() && !EXCEPTION_CURRENCIES_WITH_NEW.contains(currency)) {
        logln("Has 'new' in name but NOT used since " + CurrencyDateInfo.formatDate(currencyLastValid.get(currency)) + "\t" + currency + "\t" + name + "\t" + nonModernCurrencyCodes.getAll(currency));
      } else if (!oldMatcher.reset(name).find() && !OK_TO_NOT_HAVE_OLD.contains(currency)){
        logln("Doesn't have 'old' or date range in name but NOT used since " + CurrencyDateInfo.formatDate(currencyLastValid.get(currency))
                + "\t" + currency + "\t" + name + "\t" + nonModernCurrencyCodes.getAll(currency));
        for (Pair<String, CurrencyDateInfo> pair : nonModernCurrencyCodes.getAll(currency)) {
          final String territory = pair.getFirst();
          Set<CurrencyDateInfo> currencyInfo = testInfo.supplementalDataInfo.getCurrencyDateInfo(territory);
          for (CurrencyDateInfo dateInfo : currencyInfo) {
            if (dateInfo.getEnd().compareTo(NOW) < 0) {
              continue;
            }
            logln("\tCurrencies used instead: " + territory + "\t" + dateInfo
                    + "\t" + testInfo.english.getName(CLDRFile.CURRENCY_NAME, dateInfo.getCurrency()));
           
          }
        }
        
      }
    }
    Set remainder = new TreeSet();
    remainder.addAll(currencyCodes);
    remainder.removeAll(nonModernCurrencyCodes.keySet());
    // TODO make this an error, except for allowed exceptions.
    logln("Currencies without Territories: " + remainder);
    assertEquals("Modern territory missing currency: " + territoriesWithoutModernCurrencies, 
            0, territoriesWithoutModernCurrencies.size());
  }

}
