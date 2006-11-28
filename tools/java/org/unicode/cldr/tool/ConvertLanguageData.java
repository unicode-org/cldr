package org.unicode.cldr.tool;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SpreadSheet;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConvertLanguageData {
  
  static final double populationFactor = 1;
  static final double gdpFactor = 1;
  static final int COUNTRY_CODE = 2, COUNTRY_POPULATION = 7, COUNTRY_GDP = 8, LANGUAGE_CODE = 5, LANGUAGE_POPULATION = 3;
  static final Map<String, Object[]> languageToMaxCountry = new TreeMap<String, Object[]>();
  static CLDRFile english;
  static Set locales;
  static Factory cldrFactory;

  
  public static void main(String[] args) throws IOException, ParseException {
    // load elements we care about
    Set<String> cldrParents = new TreeSet<String>();
    cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
    Set available = cldrFactory.getAvailable();
    LanguageTagParser ltp = new LanguageTagParser();
    for (String locale : (Set<String>) available) {
      int lastPos = locale.lastIndexOf('_');
      if (lastPos < 0) continue;
      locale = locale.substring(0,lastPos);
      cldrParents.add(locale);
      languageToMaxCountry.put(locale,null);
    }
    System.out.println("CLDR Parents: " + cldrParents);
    
    List<String> failures = new ArrayList();

    NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
    NumberFormat pf = NumberFormat.getPercentInstance(ULocale.ENGLISH);
    List<List<String>> input = SpreadSheet.convert("C:\\Documents and Settings\\markdavis\\My Documents\\" +
        "Excel Stuff\\country_language_population-1022md2.txt");
    String lastCountryCode = "";
    int count = 0;
    boolean first = true;
    for (List<String> row : input) {
      ++count;
      if (count == 1 || row.size() <= COUNTRY_GDP) {
        failures.add(join(row,"\t") + "\tShort row");
        continue;
      }
      try {
        String countryCode = row.get(COUNTRY_CODE);
        
        double countryPopulationRaw = nf.parse(row.get(COUNTRY_POPULATION)).doubleValue();
        long countryPopulation = Math.round(countryPopulationRaw/populationFactor);
        
        double countryGDPRaw = nf.parse(row.get(COUNTRY_GDP)).doubleValue();
        long countryGDP = Math.round(countryGDPRaw/gdpFactor);
        
        String languageCode = row.get(LANGUAGE_CODE);
        
        double languagePopulationRaw = nf.parse(row.get(LANGUAGE_POPULATION)).doubleValue();
        long languagePopulation = Math.round(languagePopulationRaw/populationFactor);
        
        int languagePopulationPercent = (int) Math.min(100, Math.max(0, 
                Math.round(languagePopulation*100 / (double)countryPopulation)));

        if (!countryCode.equals(lastCountryCode)) {
          if (first) {
            first = false;
          } else {
            System.out.println("\t\t</country>");
          }
          System.out.print("\t\t<country countryCode=\"" + countryCode + "\""
              + " gdp=\"" + countryGDP + "\""
              + " population=\"" + countryPopulation + "\">");
          lastCountryCode = countryCode;
          System.out.println("\t<!--" + ULocale.getDisplayCountry("und_" + countryCode, ULocale.ENGLISH) + "-->");
        }
        // add best case
        addBest(countryCode, languageCode, languagePopulationRaw);
        String baseLanguage = new ULocale(languageCode).getLanguage();
        if (!baseLanguage.equals(languageCode)) {
          addBest(countryCode, baseLanguage, languagePopulationRaw);
        }
          
        if (languagePopulationPercent != 0 && languageCode.length() != 0) {
          System.out.print("\t\t\t<language languageCode=\"" + languageCode + "\""
              + " functionallyLiterate=\"" + languagePopulationPercent + "%\"/>");
          System.out.println("\t<!--" + ULocale.getDisplayName(languageCode, ULocale.ENGLISH) + "-->");
        } else {
          failures.add(join(row,"\t") + "\tLess than 1% or no language code");
        }
        //if (first) {
          if (false) System.out.print(
              "countryCode: " + countryCode + "\t"
              + "countryPopulation: " + countryPopulation + "\t"
              + "countryGDP: " + countryGDP + "\t"
              + "languageCode: " + languageCode + "\t"
              + "languagePopulation: " + languagePopulation + "\r\n"
              );
        //}
      } catch (ParseException e) {
        failures.add(join(row,"\t") + "\t" + join(Arrays.asList(e.getStackTrace()),";\t"));
        //System.out.println(row);
        //e.printStackTrace(System.out);
      }
    }
    
    Set<String> defaultContent = new TreeSet<String>();
    System.out.println("\t\t</country>");
    for (String languageCode : languageToMaxCountry.keySet()) {
      Object[] best = languageToMaxCountry.get(languageCode);
      String countryCode = "ZZ";
      double rawLanguagePopulation = -1;
      if (best != null) {
        countryCode = (String)best[0];
        rawLanguagePopulation = (Double)best[1];
      }
      System.out.println(
          languageCode
          + "\t" + ULocale.getDisplayName(languageCode, ULocale.ENGLISH)
          + "\t" + countryCode
          + "\t" + ULocale.getDisplayCountry("und_" + countryCode, ULocale.ENGLISH)
          + "\t" + nf.format(rawLanguagePopulation)
          + (cldrParents.contains(languageCode) ? "\tCLDR" : "")
          );
      if (languageCode.length() == 0) continue;
      
      String localeID = languageCode;
      ltp.set(languageCode);
      if (ltp.getRegion().length() == 0) {
        localeID = languageCode + "_" + countryCode;
      }
      defaultContent.add(localeID);
    }
    
    System.out.println();
    System.out.println("CLDR Content");
    System.out.println();
    for (String locale : (Set<String>) available) {
      if (defaultContent.contains(locale)) System.out.print("*");
      System.out.println("\t" + locale + "\t" + ULocale.getDisplayName(locale, ULocale.ENGLISH));
    }

    System.out.println();
    System.out.println("Failures");
    System.out.println();
    
    for (String failure : failures) {
      System.out.println(failure);
    }
  }
  
  public static String join (Collection c, String separator) {
    StringBuffer result = new StringBuffer();
    boolean first = true;
    for (Object x : c) {
      if (first) first = false;
      else result.append(separator);
      result.append(x);
    }
    return result.toString();
  }

  private static void addBest(String countryCode, String languageCode, double languagePopulationRaw) {
    Object[] best = languageToMaxCountry.get(languageCode);
    if (best == null) {
      languageToMaxCountry.put(languageCode, new Object[]{countryCode, languagePopulationRaw});
    } else if ((Double)(best[1]) < languagePopulationRaw) {
      languageToMaxCountry.put(languageCode, new Object[]{countryCode, languagePopulationRaw});
    }
  }
}