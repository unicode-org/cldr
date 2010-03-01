package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.impl.Row;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class AddPopulationData {
  static boolean SHOW_SKIP = CldrUtility.getProperty("SHOW_SKIP", false);
  static boolean SHOW_ALTERNATE_NAMES = CldrUtility.getProperty("SHOW_ALTERNATE_NAMES", false);

  enum WBLine {
    Country_Name, Country_Code, Series_Name, Series_Code, YR2000, YR2001, YR2002, YR2003, YR2004, YR2005, YR2006, YR2007, YR2008;
    String get(String[] pieces) {
      return pieces[ordinal()];
    }
  }

  enum FBLine {
    Rank, Country, Value, Year;
    String get(String[] pieces) {
      return pieces[ordinal()];
    }
  }

  private static final String        GCP                   = "NY.GNP.MKTP.PP.CD";
  private static final String        POP                   = "SP.POP.TOTL";
  private static final String        EMPTY                 = "..";
  private static Counter2<String> worldbank_gdp        = new Counter2<String>();
  private static Counter2<String> worldbank_population = new Counter2<String>();
  private static Counter2<String> un_literacy   = new Counter2<String>();

  private static Counter2<String> factbook_gdp          = new Counter2<String>();
  private static Counter2<String> factbook_population   = new Counter2<String>();
  private static Counter2<String> factbook_literacy   = new Counter2<String>();

  private static CountryData other = new CountryData();

  private static Map<String, String> nameToCountryCode     = new TreeMap(new UTF16.StringComparator(true, true, 0));

  static class CountryData {
    private static Counter2<String> population   = new Counter2<String>();
    private static Counter2<String> gdp   = new Counter2<String>();
    private static Counter2<String> literacy   = new Counter2<String>();
  }

  public static void main(String[] args) throws IOException {


    System.out.println("Code"
            + "\t" + "Name"
            + "\t" + "Pop"
            + "\t" + "GDP-PPP"
            + "\t" + "UN Literacy"
    );

    for (String country : ULocale.getISOCountries()) {
      showCountryData(country);
    }
    Set<String> outliers = new TreeSet<String>();
    outliers.addAll(factbook_population.keySet());
    outliers.addAll(worldbank_population.keySet());
    outliers.addAll(factbook_gdp.keySet());
    outliers.addAll(worldbank_gdp.keySet());
    outliers.addAll(un_literacy.keySet());
    outliers.removeAll(Arrays.asList(ULocale.getISOCountries()));
    System.out.println("Probable Mistakes");
    for (String country : outliers) {
      showCountryData(country);
    }
    Set<String> altNames = new TreeSet<String>();
    String oldCode = "";
    int alt = 0;
    for (String display : nameToCountryCode.keySet()) {
      String code = nameToCountryCode.get(display);
      String icu = ULocale.getDisplayCountry("und-" + code, "en");
      if (!display.equalsIgnoreCase(icu)) {       
        altNames.add(code + "\t" + display + "\t" + icu);
      }
    }
    oldCode = "";
    if (SHOW_ALTERNATE_NAMES) {
      for (String altName : altNames) {
        String[] pieces = altName.split("\t");
        String code = pieces[0];
        if (code.equals("ZZ")) continue;
        if (!code.equals(oldCode)) {
          alt = 0;
          oldCode = code;
          System.out.println();
        }
        System.out.println(code + "; " + pieces[2] + "; " + pieces[1]);
        //System.out.println("<territory type=\"" + code + "\" alt=\"v" + (++alt) + "\">" + pieces[1] + "</territory> <!-- " + pieces[2] + " -->");
      }
    }
  }

  private static void showCountryData(String country) {
    System.out.println(country
            + "\t" + ULocale.getDisplayCountry("und-" + country, "en")
            + "\t" + getPopulation(country)
            + "\t" + getGdp(country)
            + "\t" + getLiteracy(country)
    );
  }

  public static Double getLiteracy(String country) {
    return firstNonZero(factbook_literacy.getCount(country),
            un_literacy.getCount(country),
            other.literacy.getCount(country));
  }

  public static Double getGdp(String country) {
    return firstNonZero(factbook_gdp.getCount(country),
            worldbank_gdp.getCount(country),
            other.gdp.getCount(country));
  }

  public static Double getPopulation(String country) {
    return firstNonZero(factbook_population.getCount(country),
            worldbank_population.getCount(country),
            other.population.getCount(country));
  }

  private static Double firstNonZero(Double...items) {
    for (Double item : items) {
      if (item.doubleValue() != 0) {
        return item;
      }
    }
    return 0.0;
  }

  private interface LineHandler {
    /**
     * Return false if line was skipped
     * @param line
     * @return
     */
    boolean handle(String line) throws Exception;
  }

  private static void handleFile(String filename, LineHandler handler) throws IOException {
    BufferedReader in = CldrUtility.getUTF8Data(filename);
    while (true) {
      String line = in.readLine();
      if (line == null) {
        break;
      }
      try {
        if (!handler.handle(line)) {
          if (SHOW_SKIP) System.out.println("Skipping line: " + line);
        }
      } catch (Exception e) {
        throw (RuntimeException) new IllegalArgumentException("Problem with line: " + line)
        .initCause(e);
      }
    }
    in.close();
  }

  private static void loadNames() throws IOException {
    for (String country : ULocale.getISOCountries()) {
      addName(ULocale.getDisplayCountry("und-" + country, "en"), country);
    }
    StandardCodes sc = StandardCodes.make();
    for (String country : sc.getGoodAvailableCodes("territory")) {
      String description = (String) sc.getFullData("territory", country).get(0);
      if (country.equals("057")) continue;
      addName(description, country);
    }
    handleFile("external/alternate_country_names.txt", new LineHandler() {
      public boolean handle(String line) {    
        if (line.trim().length() == 0) {
          return true; // don't show skips
        }
        String[] pieces = line.split(";");
        addName(pieces[2].trim(), pieces[0].trim());
        return true;
      }
    });
    //    if (false) {
    //    //addName("World", "ZZ");
    //    //addName("European Union", "ZZ");
    //    addName("Hong Kong", "HK");
    //    addName("Burma", "MM");
    //    addName("Cote d'Ivoire", "CI");
    //    addName("Congo, Democratic Republic of the", "CD");
    //    addName("Congo, Republic of the", "CG");
    //    addName("Macau", "MO");
    //    addName("Bahamas, The", "BS");
    //    addName("Gaza Strip", "PS");
    //    addName("West Bank", "PS");
    //    addName("Kosovo", "RS"); // no country code
    //    addName("Timor-Leste", "TL");
    //    addName("Gambia, The", "GM");
    //    addName("Virgin Islands", "VI"); // American
    //    addName("Micronesia, Federated States of", "FM");
    //    addName("Falkland Islands (Islas Malvinas)", "FK");
    //
    //    addName("Akrotiri", "CY"); // part
    //    addName("Dhekelia", "CY"); // part
    //    addName("Saint Barthelemy", "BL");
    //    addName("Svalbard", "SJ"); // part
    //    addName("Holy See (Vatican City)", "VA");
    //    addName("Cocos (Keeling) Islands", "CC");
    //    addName("Pitcairn Islands", "PN");
    //
    //    addName("Brunei Darussalam", "BN");
    //    addName("Channel Islands", "GG"); // should be GG + JE
    //    addName("Congo, Dem. Rep.", "CD");
    //    addName("Congo, Rep.", "CG");
    //    //addName("East Asia & Pacific", "ZZ");
    //    addName("Egypt, Arab Rep.", "EG");
    //    //addName("Europe & Central Asia", "ZZ");
    //    //addName("Euro area", "ZZ");
    //    addName("Faeroe Islands", "FO");
    //    //addName("Heavily indebted poor countries (HIPC)", "ZZ");
    //    //addName("High income", "ZZ");
    //    //addName("High income: nonOECD", "ZZ");
    //    //addName("High income: OECD", "ZZ");
    //    addName("Hong Kong, China", "HK");
    //    addName("Iran, Islamic Rep.", "IR");
    //    addName("Korea, Dem. Rep.", "KP");
    //    addName("Korea, Rep.", "KR");
    //    addName("Kyrgyz Republic", "KG");
    //    addName("Lao PDR", "LA");
    //    //addName("Latin America & Caribbean", "ZZ");
    //    //addName("Least developed countries: UN classification", "ZZ");
    //    //addName("Low & middle income", "ZZ");
    //    //addName("Low income", "ZZ");
    //    //addName("Lower middle income", "ZZ");
    //    addName("Macao, China", "MO");
    //    addName("Macedonia, FYR", "MK");
    //    addName("The Former Yugoslav Rep. of Macedonia", "MK");
    //    addName("Micronesia, Fed. Sts.", "FM");
    //    //addName("Middle East & North Africa", "ZZ");
    //    //addName("Middle income", "ZZ");
    //    addName("Russian Federation", "RU");
    //    addName("Slovak Republic", "SK");
    //    //addName("South Asia", "ZZ");
    //    addName("St. Kitts and Nevis", "KN");
    //    addName("St. Lucia", "LC");
    //    addName("St. Vincent and the Grenadines", "VC");
    //    //addName("Sub-Saharan Africa", "ZZ");
    //    addName("Syrian Arab Republic", "SY");
    //    //addName("Upper middle income", "ZZ");
    //    addName("Venezuela, RB", "VE");
    //    addName("Virgin Islands (U.S.)", "VI");
    //    addName("West Bank and Gaza", "PS");
    //    addName("Palestinian Autonomous Territories", "PS");
    //    addName("Yemen, Rep.", "YE");
    //    addName("C�te d'Ivoire", "CI");
    //    }
  }

  static String[] splitCommaSeparated(String line) {
    // items are separated by ','
    // each item is of the form abc...
    // or "..." (required if a comma or quote is contained)
    // " in a field is represented by ""
    List<String> result = new ArrayList<String>();
    StringBuilder item = new StringBuilder();
    boolean inQuote = false;
    for (int i = 0; i < line.length(); ++i) {
      char ch = line.charAt(i); // don't worry about supplementaries
      switch(ch) {
      case '"': 
        inQuote = !inQuote;
        // at start or end, that's enough
        // if get a quote when we are not in a quote, and not at start, then add it and return to inQuote
        if (inQuote && item.length() != 0) {
          item.append('"');
          inQuote = true;
        }
        break;
      case ',':
        if (!inQuote) {
          result.add(item.toString());
          item.setLength(0);
        } else {
          item.append(ch);
        }
        break;
      default:
        item.append(ch);
        break;
      }
    }
    result.add(item.toString());
    return result.toArray(new String[result.size()]);
  }

  private static void addName(String key, String code) {
    addName2(key, code);
    String trial = reverseComma(key);
    if (trial != null) {
      addName2(trial, code);
    }
  }

  private static void addName2(String key, String code) {
    String old = nameToCountryCode.get(key);
    if (old != null && !code.equals(old)) {
      System.out.println("Conflict!!" + key + "\t" + old + "\t" + code);
      return;
    }
    nameToCountryCode.put(key, code);
  }

  private static String countryToCode(String display) {
    String trial = display.trim();
    String result = nameToCountryCode.get(trial);
    if (result == null) {
      trial = reverseComma(display);
      if (trial != null) {
        result = nameToCountryCode.get(trial);
        if (result != null) {
          addName(trial, result);
        }
      }
    }
    if (SHOW_SKIP && result == null) {
      System.out.println("Missing code for: " + display);
    }
    return result;
  }

  private static String reverseComma(String display) {
    String trial;
    trial = null;
    int comma = display.indexOf(',');
    if (comma >= 0) {
      trial = display.substring(comma + 1).trim() + " " + display.substring(0, comma).trim();
    }
    return trial;
  }

  private static void loadFactbookInfo(String filename, final Counter2<String> factbookGdp) throws IOException {
    handleFile(filename, new LineHandler() {
      public boolean handle(String line) {
        if (line.length() == 0 || line.startsWith("This tab") || line.startsWith("Rank")
                || line.startsWith(" This file")) {
          return false;
        }
        String[] pieces = line.split("\t");
        String code = countryToCode(FBLine.Country.get(pieces));
        if (code == null) {
          return false;
        }
        String valueString = FBLine.Value.get(pieces).trim();
        if (valueString.startsWith("$")) {
          valueString = valueString.substring(1);
        }
        valueString = valueString.replace(",", "");
        double value = Double.parseDouble(valueString.trim());
        factbookGdp.add(code, value);
        // System.out.println(Arrays.asList(pieces));
        return true;
      }
    });
  }

  static final NumberFormat dollars = NumberFormat.getCurrencyInstance(ULocale.US);
  static final NumberFormat number = NumberFormat.getNumberInstance(ULocale.US);

  static class MyLineHandler implements LineHandler {
    CountryData countryData;

    public MyLineHandler(CountryData countryData) {
      super();
      this.countryData = countryData;
    }
    public boolean handle(String line) throws ParseException {
      if (line.startsWith("#")) return true;
      if (line.length() == 0) {
        return true;
      }
      String[] pieces = line.split(";");
      final String code = pieces[0].trim();
      if (code.equals("Code")) {
        return false;
      }
      // Code;Name;Type;Data;Source
      final String typeString = pieces[2].trim();
      final String data = pieces[3].trim();
      if (typeString.equals("gdp-ppp")) {
        if (StandardCodes.isCountry(data)) {
          Double otherPop = getPopulation(data);
          Double otherGdp = getPopulation(data);
          Double myPop = getPopulation(code);
          if (myPop.doubleValue() == 0 || otherPop.doubleValue() == 0 || otherGdp.doubleValue() == 0) {
            otherPop = getPopulation(data);
            otherGdp = getPopulation(data);
            myPop = getPopulation(code);
            throw new IllegalArgumentException("Zero population");
          }
          countryData.gdp.add(code, otherGdp * myPop / otherPop);
        } else {
          countryData.gdp.add(code, dollars.parse(data).doubleValue());
        }
      } else if (typeString.equals("population")) {
        if (StandardCodes.isCountry(data)) {
          throw new IllegalArgumentException("Population can't use other country's");
        }
        countryData.population.add(code, number.parse(data).doubleValue());
      } else if (typeString.equals("literacy")) {
        if (StandardCodes.isCountry(data)) {
          Double otherPop = getLiteracy(data);
          countryData.literacy.add(code, otherPop);
        } else {
          countryData.literacy.add(code, number.parse(data).doubleValue());
        }
      } else {
        throw new IllegalArgumentException("Illegal type");
      }
      return true;
    }
  }

  static final UnicodeSet DIGITS = (UnicodeSet) new UnicodeSet("[:Nd:]").freeze();

  private static void loadFactbookLiteracy() throws IOException {
    final String filename = "external/factbook_literacy.html";
    handleFile(filename, new LineHandler() {
      Matcher m = Pattern.compile("<i>total population:</i>\\s*(?:above\\s*)?(?:[0-9]+-)?([0-9]*\\.?[0-9]*)%.*").matcher("");
      Matcher codeMatcher = Pattern.compile("<a href=\"../geos/[^\\.]+.html\" class=\"CountryLink\">([^<]+)</a>").matcher("");
      String code = "ZZ";
      public boolean handle(String line) throws ParseException {
        // <i>total population:</i> 43.1% 
        line = line.trim();
        if (line.contains("CountryLink")) {
          if (!codeMatcher.reset(line).matches()) {
            throw new IllegalArgumentException("mismatched line: " + code);
          }       
          code = countryToCode(codeMatcher.group(1));
          if (code == null) {
            throw new IllegalArgumentException("bad country");
          }
          return true;
        }
        if (!line.contains("total population")) {
          return true;
        }
        if (!m.reset(line).matches()) {
          throw new IllegalArgumentException("mismatched line: " + code);
        }
        // <a href="../geos/al.html" class="CountryLink">Albania</a>
        // AX Aland Islands www.aland.ax  26,200  $929,773,254
        final String percentString = m.group(1);
        final double percent = number.parse(percentString).doubleValue();
        if (factbook_literacy.getCount(code) != 0) {
          System.out.println("Duplicate literacy in FactBook: " + code);
          return false;
        }
        factbook_literacy.add(code, percent);
        return true;
      }
    });
  }


  private static void loadWorldBankInfo() throws IOException {
    final String filename = "external/world_bank_data.csv";
    handleFile(filename, new LineHandler() {
      public boolean handle(String line) {
        if (line.contains("Series Code")) {
          return false;
        }
        String[] pieces = line.substring(1, line.length() - 2).split("\"\t\"");
        final String seriesCode = WBLine.Series_Code.get(pieces);

        String last = null;
        for (WBLine i : WBLine.values()) {
          if (i.compareTo(WBLine.YR2000) >= 0) {
            String current = i.get(pieces);
            if (!current.equals(EMPTY)) {
              last = current;
            }
          }
        }
        if (last == null) {
          return false;
        }
        String country = countryToCode(WBLine.Country_Name.get(pieces));
        if (country == null) {
          return false;
        }
        double value = Double.parseDouble(last);
        if (seriesCode.equals(GCP)) {
          worldbank_gdp.add(country, value);
        } else if (seriesCode.equals(POP)) {
          worldbank_population.add(country, value);
        } else {
          throw new IllegalArgumentException();
        }
        return true;
      }
    });
  }


  private static void loadUnLiteracy() throws IOException {
    handleFile("external/un_literacy.csv", new LineHandler() {
      public boolean handle(String line) {
        // Afghanistan,2000, ,28,43,13,,34,51,18
        String[] pieces = splitCommaSeparated(line);
        if (pieces.length != 10 || !DIGITS.containsAll(pieces[1])) {
          return false;
        }
        String code = countryToCode(pieces[0]);
        if (code == null) {
          return false;
        }
        double percent = Double.parseDouble(pieces[3]);
        un_literacy.add(code, percent);
        return true;
      }
    });
  }

  static {
    try {
      loadNames();
      loadFactbookLiteracy();
      loadUnLiteracy();

      loadFactbookInfo("external/factbook_gdp_ppp.txt", factbook_gdp);
      loadFactbookInfo("external/factbook_population.txt", factbook_population);
      handleFile("external/other_country_data.txt", new MyLineHandler(other));

      loadWorldBankInfo();
      StandardCodes sc = StandardCodes.make();
      StringBuilder myErrors = new StringBuilder();
      for (String territory : sc.getGoodAvailableCodes("territory")) {
        if (!sc.isCountry(territory)) {
          continue;
        }
        double gdp = getGdp(territory);
        double literacy = getLiteracy(territory);
        double population = getPopulation(territory);
        if (gdp == 0) {
          // AX;Aland Islands;population;26,200;www.aland.ax
          myErrors.append("\n" + territory + ";" + sc.getData("territory", territory) + ";gdp-ppp;0;reason");
        }
        if (literacy == 0) {
          myErrors.append("\n" + territory + ";" + sc.getData("territory", territory) + ";literacy;0;reason");
        }
        if (population == 0) {
          myErrors.append("\n" + territory + ";" + sc.getData("territory", territory) + ";population;0;reason");
        }
      }
      if (myErrors.length() != 0) {
        throw new IllegalArgumentException("Missing Country values, edit external/other_country_data to fix:" + myErrors);
      }
    } catch (IOException e) {
    }
  }
}
