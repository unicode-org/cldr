package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class IsoCurrencyParser {
  static Map<String,String> iso4217CountryToCountryCode = new TreeMap();
  static {
    StandardCodes sc = StandardCodes.make();
    Set<String> countries = sc.getAvailableCodes("territory");
    for (String country : countries) {
      String name = sc.getData("territory", country);
      iso4217CountryToCountryCode.put(name.toUpperCase(Locale.ENGLISH), country);
    }
    // add bogus names
    String[][] extras = {
        {"BOSNIA & HERZEGOVINA", "BA"},
        {"CONGO, THE DEMOCRATIC REPUBLIC OF", "CD"},
        {"C\u00D4TE D'IVOIRE", "CI"},
        {"HEARD ISLAND AND McDONALD ISLANDS", "HM"},
        {"INTERNATIONAL MONETARY FUND (I.M.F)", "ZZ"},
        {"IRAN (ISLAMIC REPUBLIC OF)", "IR"},
        {"MICRONESIA (FEDERATED STATES OF)", "FM"},
        {"R\u00C9UNION", "RE"},
        {"S\u00C3O TOME AND PRINCIPE", "ST"},
        {"SERBIA AND MONTENEGRO *", "CS"},
        {"VIRGIN ISLANDS (BRITISH)", "VG"},
        {"VIRGIN ISLANDS (US)", "VI"},
        {"ZZ", "ZZ"},
    };
    for (String[] pair : extras) {
      iso4217CountryToCountryCode.put(pair[0], pair[1]);
    }
  }
  
  private Relation<String,Data> codeList = new Relation(new TreeMap(), TreeSet.class, null);
  private String version;
  
  public static class Data implements Comparable {
    private String name;
    private String countryCode;
    private int numericCode;
    
    public Data(String countryCode, String name, String numericCode) {
      this.countryCode = getCountryCode(countryCode);
      this.name = name;
      this.numericCode = numericCode.equals("Nil") ? -1 : Integer.parseInt(numericCode);
    }
    
    String getCountryCode(String iso4217Country) {
      iso4217Country = iso4217Country.trim();
      if (iso4217Country.startsWith("\"")) {
        iso4217Country = iso4217Country.substring(1,iso4217Country.length()-1);
      }
      String name = iso4217CountryToCountryCode.get(iso4217Country);
      if (name != null) return name;
      System.out.format("\t\t{\"%s\", \"XXX\"},\r\n", iso4217Country);
      return "???" + iso4217Country;
    }

    public String getCountryCode() {
      return countryCode;
    }
    public String getName() {
      return name;
    }
    public int getNumericCode() {
      return numericCode;
    }
    public String toString() {
      return String.format("[%s,\t%s [%s],\t%d]", name, countryCode, StandardCodes.make().getData("territory", countryCode), numericCode);
    }
    
    public int compareTo(Object o) {
      Data other = (Data)o;
      int result;
      if (0 != (result = countryCode.compareTo(other.countryCode))) return result;
      if (0 != (result = name.compareTo(other.name))) return result;
      return numericCode - other.numericCode;
    }
  }
  
  private static IsoCurrencyParser INSTANCE = new IsoCurrencyParser();
  
  public String getVersion() {
    return version;
  }
  
  public static IsoCurrencyParser getInstance() {
    return INSTANCE;
  }
  
  public Relation<String, Data> getCodeList() {
    return codeList;
  }
  
  private IsoCurrencyParser() {
    String line = null;
    String[] parts = new String[0];
    String lastCountry = "";
    Set<String> currencies = new TreeSet();
    try {
      BufferedReader in = Utility.getUTF8Data("currencycodeslist.txt");
      while (true) {
        line = in.readLine();
        if (line == null) break;
        if (line.startsWith("Last modified")) {
          version = line.substring(14).trim();
          continue;
        }
        parts = line.split("\t");
        if (parts.length == 0) continue;
        if (parts.length < 4 || parts[3].equals("Numeric") || parts[3].equals("")) {
          //System.out.format("Skipping %s\r\n", Arrays.asList(parts));
          continue;
        }
        // AFGHANISTAN  Afghani AFN 971
        String countryCode = parts[0].length() == 0 ? lastCountry : parts[0];
        Data data = new Data(countryCode, parts[1], parts[3]);
        codeList.put(parts[2], data);
        lastCountry = countryCode;
      }
      in.close();
      // codeList = (Map<String, Data>) Utility.protectCollection(codeList);
      oldValues.removeAll(codeList.keySet());
      StandardCodes sc = StandardCodes.make();
      for (String code : oldValues) {
        String name = sc.getData("currency", code);
        Data data = new Data("ZZ", name, "-1");
        codeList.put(code, data);
      }
//      Set<String> remainder = new TreeSet(codeList.keySet());
//      System.out.format("MISSING: %s\r\n", Utility.join(oldValues," "));
//      remainder.removeAll(StandardCodes.make().getAvailableCodes("currency"));
//      if (remainder.size() != 0) {
//        throw new IllegalArgumentException("Missing value; update internal list");
//      }
    } catch (Exception e) {
      throw (RuntimeException) new IllegalArgumentException(Arrays.asList(parts).toString()).initCause(e);
    }
  }

  Set<String> oldValues = new TreeSet(Arrays.asList(new String[]{
      "ADP", "AFA", "AOK", "AON", "AOR", "ARA", "ARP", "ATS", "AZM", 
      "BAD", "BEC", "BEF", "BEL", "BGL", "BOP", "BRB", "BRC", "BRE", "BRN", "BRR", "BUK", "BYB",
      "CSK",
      "DDM", "DEM",
      "ECS", "ECV", "EQE", "ESA", "ESB", "ESP",
      "FIM", "FRF",
      "GEK", "GNS", "GQE", "GRD", "GWE",
      "HRD",
      "IEP", "ILP", "ITL",
      "LSM", "LTT", "LUC", "LUF", "LUL", "LVR",
      "MAF", "MGF", "MLF", "MTP", "MXP", "MZE", "MZM",
      "NIC", "NLG",
      "PEI", "PES", "PLZ", "PTE",
      "RHD", "RSD", "RUR",
      "SDP", "SRG", "SUR",
      "TJR", "TPE", "TRL",
      "UAK", "UGS", "UYP",
      "XEU", "XRE",
      "YDD", "YUD", "YUM", "YUN",
      "ZAL", "ZRN", "ZRZ"
  }));
}