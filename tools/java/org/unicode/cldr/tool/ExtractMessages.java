package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Pair;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.WinningChoice;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ExtractMessages {
  public static final UnicodeSet LATIN_SCRIPT = (UnicodeSet) new UnicodeSet("[:script=latin:]").freeze();
  
  private static Matcher fileMatcher;
  
  public static PrintWriter output;
  
  public static boolean SKIPEQUALS = true;
  public static boolean SKIPIFCLDR = true;
  public static String DIR = CldrUtility.GEN_DIRECTORY + "/../additions/";
  
  public static void main(String[] args) throws IOException {
    double startTime = System.currentTimeMillis();
    output = BagFormatter.openUTF8Writer(DIR, "additions.txt");
    int totalCount = 0;
    Set<String> skipped = new TreeSet();

    try {
      String sourceDirectory = getProperty("SOURCE", null);
      if (sourceDirectory == null) {
        System.out.println("Need Source Directory! ");
        return;
      }
      fileMatcher = Pattern.compile(getProperty("FILE", ".*")).matcher("");
      
      SKIPIFCLDR = getProperty("SKIPIFCLDR", null) != null;
      
      boolean showMissing = true;
      
      File src = new File(sourceDirectory);
      
      XMLFileReader xfr = new XMLFileReader().setHandler(new EnglishHandler());
      xfr.read(src+"/en.xmb", XMLFileReader.CONTENT_HANDLER
          | XMLFileReader.ERROR_HANDLER, false);
      

      for (File file : src.listFiles()) {
        if (file.isDirectory())
          continue;
        if (file.length() == 0)
          continue;
        String canonicalFile = file.getCanonicalPath();
        if (!canonicalFile.endsWith(".xtb")) {
          continue;
        }

        String name = file.getName();
        name = name.substring(0,name.length()-4);

        if (!fileMatcher.reset(name).matches()) {
          continue;
        }
        System.out.println("* " + canonicalFile);
        
        try {
          otherHandler.setLocale(name);
        } catch (RuntimeException e1) {
          System.out.println("Skipping, no CLDR locale file: " + name + "\t" + english.getName(name) + "\t" + e1.getClass().getName() + "\t" + e1.getMessage() );
          skipped.add(name);
          continue;
        }
        
        xfr = new XMLFileReader().setHandler(otherHandler);
        try {
          xfr.read(canonicalFile, XMLFileReader.CONTENT_HANDLER
              | XMLFileReader.ERROR_HANDLER, false);
        } catch (RuntimeException e) {
          System.out.println(e.getMessage());
          continue;
        }
        
        // now write it out
        CLDRFile newFile = CLDRFile.make(otherHandler.getLocale());
        int itemCount = 0;
        for (DataHandler dataHandler : dataHandlers) {
          if (showMissing) {
            System.out.println("case " + dataHandler.type + ":");
            for (String value : dataHandler.missing) {
              System.out.println("addName(\"" + value + "\", \"XXX\", true);");
            }
          }
          
          for (String id : dataHandler.id_to_value.keySet()) {
            Set<String> otherValue = dataHandler.id_to_value.getAll(id);
            if (otherValue == null || otherValue.size() == 0) continue;
            String cldrValue = dataHandler.id_to_cldrValue.get(id);
            int count = 0;
            for (String oValue : otherValue) {
              itemCount++;
              output.println(otherHandler.getLocale()
                  + "\t" + dataHandler.type
                  + "\t" + id
                  + "\t" + oValue
                  + (cldrValue == null ? "" : "\tcldr:\t" + cldrValue)
                  + (count == 0 ? "" : "\talt:\t" + String.valueOf(count)));
              newFile.add(dataHandler.getPath(id, count), oValue);
            }
          }
        }
        PrintWriter cldrOut = BagFormatter.openUTF8Writer(DIR, otherHandler.getLocale() + ".xml");
        newFile.write(cldrOut);
        cldrOut.close();
        
        output.println();
        showMissing = false;
        output.flush();
        System.out.println("\titems: " + itemCount);
        totalCount += itemCount;
      }
      
      for (String name : skipped) {
        System.out.println("\tSkipping, no CLDR locale file: " + name + "\t" + english.getName(name));
      }
      double deltaTime = System.currentTimeMillis() - startTime;
      System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
      System.out.println("\ttotal items: " + totalCount);
    } finally {
      output.close();
    }
  }
  
  private static String getProperty(String key, String defaultValue) {
    String fileRegex = System.getProperty(key);
    if (fileRegex == null)
      fileRegex = defaultValue;
    System.out.println("-D" + key + "=" + fileRegex);
    return fileRegex;
  }
  
  private static Map<String,Pair<String,DataHandler>> numericId_Id = new TreeMap();
  private static Matcher numericIdMatcher = Pattern.compile("\\[@id=\"([^\"]+)\"\\]").matcher("");
  private static Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
  private static CLDRFile english = cldrFactory.make("en", true);
  
  private static class EnglishHandler extends XMLFileReader.SimpleHandler {
    
    public void handlePathValue(String path, String value) {
      for (DataHandler handler : dataHandlers) {
        if (handler.matches(path)) {
          // //messagebundle/msg[@id="1907015897505457162"][@seq="71982"][@desc="Andorra is a display name for a timezone"][@xml:space="default"]
          numericIdMatcher.reset(path).find();
          String id = numericIdMatcher.group(1);
          value = value.trim();
          if (value.length() == 0) return; // skip empties
          value = TransliteratorUtilities.fromXML.transliterate(value);
          String realID = handler.getCode(value);
          if (realID == null) {
            handler.missing.add(value);
            return;
          }
          numericId_Id.put(id, new Pair(realID, handler));
          //System.out.println(id + "\t" + path + "\t" + value);
        }
      }
    }
  }
  
  public static Collator col = Collator.getInstance(ULocale.ENGLISH);
  static {
    col.setStrength(Collator.SECONDARY);
  }
  
  private static OtherHandler otherHandler = new OtherHandler();
  
  private static class OtherHandler extends XMLFileReader.SimpleHandler {
    private String locale;
    private ULocale uLocale;
    CLDRFile cldrFile;
    boolean usesLatin;
    
    public void handlePathValue(String path, String value) {
      // //messagebundle/msg[@id="1907015897505457162"][@seq="71982"][@desc="Andorra is a display name for a timezone"][@xml:space="default"]
      value = value.trim();
      if (value.length() == 0) return; // skip empties
      
      numericIdMatcher.reset(path).find();
      String numericId = numericIdMatcher.group(1);
      Pair<String,DataHandler> id_handler = numericId_Id.get(numericId);
      if (id_handler == null) return;
      String id = id_handler.getFirst();
      DataHandler dataHandler = id_handler.getSecond();
      
      if (!usesLatin && LATIN_SCRIPT.containsSome(value)) {
        // output.println(locale + "\tSkipping item with latin characters\t" + id + "\t" + value);
        return;
      }
      
      // this should be reorganized to put more in the DataHandler, but for now...
      
      value = dataHandler.fixValue(uLocale, value);
      
      String cldrValue = dataHandler.getCldrValue(cldrFile, id);
      if (cldrValue != null) {
        if (col.compare(cldrValue, value) == 0) {
          //System.out.println("Duplicate for " + id + "\t" + value);
          if (SKIPEQUALS) return;
        } else {
          if (SKIPIFCLDR) return;
          //output.println(locale + "\tDifferent value for\t" + id + "\t" + value + "\tcldr:\t" + cldrValue);
        }
      }
      dataHandler.addValues(id, value, cldrValue);
    }
    
    public void setLocale(String locale) {
      
      // skip en, fr_CA
      // as, sa bad
      // ku cldr-latin, g-arabic
      // ml, my, pa, te has mixed english
      // TODO move this into datahandler eventually
      locale = fixLocale(locale);
      this.locale = locale;
      this.uLocale = new ULocale(locale);
      String lang = uLocale.getLanguage();
      if (locale.equals("fr_CA") || lang.equals("en")) {
        throw new RuntimeException("Skipping " + locale);
      }
      cldrFile = cldrFactory.make(locale, false);
      UnicodeSet exemplars = cldrFile.getExemplarSet("",WinningChoice.WINNING);
      usesLatin = exemplars != null && exemplars.containsSome(LATIN_SCRIPT);
      for (DataHandler dataHandler : dataHandlers) {
        dataHandler.reset(cldrFile);
      }
    }
    
    
    public String getLocale() {
      return locale;
    }
  }
  
  static Map<String,String> fixLocaleMap = CldrUtility.asMap(new String[][]{
      {"zh_CN", "zh"},
      {"zh_TW", "zh_Hans"},
      {"pt_BR", "pt"},
      {"in", "id"},
      {"iw", "he"},
      {"jw", "jv"},
      {"no", "nb"},
      {"ku", "ku_Arab"},
  });
  
  static private String fixLocale(String locale) {
    locale = locale.replace('-', '_');
    String newLocale = fixLocaleMap.get(locale);
    if (newLocale != null) {
      locale = newLocale;
    }
    return locale;
  }
  
  /*
   Language
   -DXMLPATH=".*form of language.*"
   
   Country
   -DXMLPATH=".*the country or region.*"
   
   Currency
   -DXMLPATH=".*currency name.*"
   
   Month Long/Short
   -DXMLPATH=".*Name of the month of .*"
   -DXMLPATH=".*3 letter abbreviation for name of Month.*"
   
   Week Long/Short
   -DXMLPATH=".*day in week.*"
   -DXMLPATH=".*Short Version of .*"
   
   Timezone
   DXMLPATH=".*is a display name for a timezone.*"
   */
  
  enum Type {LANGUAGE, REGION, CURRENCY, MONTH, MONTHSHORT, DAY, DAYSHORT, TIMEZONE};
  static StandardCodes sc = StandardCodes.make();
  static DateFormatSymbols dfs = new DateFormatSymbols(ULocale.ENGLISH);
  
  static DataHandler[] dataHandlers = {
    new DataHandler(Type.LANGUAGE, ".*form of language.*"),
    new DataHandler(Type.REGION, ".*the country or region.*"),
    new DataHandler(Type.CURRENCY, ".*currency name.*"),
    new DataHandler(Type.MONTH, ".*Name of the month of .*"),
    new DataHandler(Type.MONTHSHORT, ".*3 letter abbreviation for name of Month.*"),
    new DataHandler(Type.DAY, ".*day in week.*"),
    new DataHandler(Type.DAYSHORT, ".*Short Version of .*"),
    new DataHandler(Type.TIMEZONE, ".*is a display name for a timezone.*"),
  };
  
  enum CasingAction {NONE, FORCE_TITLE, FORCE_LOWER}

  static class DataHandler implements Comparable {
    // mostly stable
    private Matcher matcher;
    private Type type;
    private Map<String,String> name_code = new TreeMap<String,String>();
    //private Map<String,String> code_name = new TreeMap();
    private Set<String> missing = new TreeSet<String>();
    
    // changes with each locale, must call reset
    private Relation<String,String> id_to_value = new Relation(new TreeMap<String,String>(), TreeSet.class);
    private Map<String,String> id_to_cldrValue = new TreeMap<String,String>();
    private CasingAction forceCasing = CasingAction.NONE;
    
    public void reset(CLDRFile cldrFile) {
      id_to_value.clear();
      id_to_cldrValue.clear();
      forceCasing = CasingAction.NONE;
      String key = null;
      switch (type) {
        case LANGUAGE: key = "en"; break;
        case REGION: key = "FR"; break;
        case CURRENCY: key = "GBP"; break;
        case MONTH: case MONTHSHORT: key = "1"; break;
        case DAY: case DAYSHORT: key = "mon"; break;
        case TIMEZONE: key = "America/New_York"; break;
      }
      String sample = getCldrValue(cldrFile, key);
      if (sample != null) {
        if (UCharacter.isLowerCase(sample.charAt(0))) {
          forceCasing = CasingAction.FORCE_LOWER;
        } else if (UCharacter.isUpperCase(sample.charAt(0))) {
          forceCasing = CasingAction.FORCE_TITLE;
        }
      }
    }
    
    public String fixValue(ULocale uLocale, String value) {
      value = TransliteratorUtilities.fromXML.transliterate(value);
      
      if (forceCasing == CasingAction.FORCE_LOWER) {
        if (!UCharacter.isLowerCase(value.charAt(0))) {
          value = UCharacter.toLowerCase(value);
        }
      } else if (forceCasing == CasingAction.FORCE_TITLE) {
        if (!UCharacter.isUpperCase(value.charAt(0))) {
          value = UCharacter.toTitleCase(uLocale, value, null);
        }
      }
      
      return value;
    }
    
    public void addValues(String id, String value, String cldrValue) {
      id_to_value.put(id, value);
      if (cldrValue != null) {
        id_to_cldrValue.put(id, cldrValue);
      }
    }
    
    public void addName(String name, String code, boolean skipMessage) {
      //String old = code_name.get(code);
//    if (old != null) {
//    if (!skipMessage) {
//    System.out.println("Name collision:\t" + code + "\tnew: " + name + "\tkeeping: " + old);
//    }
//    } else {
//    }
      //code_name.put(code, name);
      name_code.put(name, code);
    }
    
    DataHandler(Type type, String pattern) {
      this.type = type;
      matcher = Pattern.compile(pattern).matcher("");
      switch (type) {
        case LANGUAGE:
          for (String code : sc.getAvailableCodes("language")) {
            String name = english.getName("language",code);
            if (name == null) {
              //System.out.println("Missing name for: " + code);
              continue;
            }
            addName(name, code.replace("-","_"), false);
          }
          // add irregular names
          addName("English (US)", "en_US", true);
          addName("English (UK)", "en_GB", true);
          //addName("English (AU)", "en_AU/short");
          //addName("Portuguese (PT)", "pt_PT/short");
          //addName("Portuguese (BR)", "pt_BR/short");
          addName("Chinese (Simplified)", "zh_Hans", true);
          addName("Chinese (Traditional)", "zh_Hant", true);
          addName("Norwegian (Nynorsk)", "nn", true);
          addName("Portuguese (Portugal)", "pt_PT", true);
          addName("Portuguese (Brazil)", "pt_BR", true);
          addName("English (Australia)", "en_AU", true);
          addName("Scots Gaelic", "gd", true);
          addName("Frisian", "fy", true);
          addName("Sesotho", "st", true);
          addName("Kyrgyz", "ky", true);
          addName("Laothian", "lo", true);
          addName("Cambodian", "km", true);
          addName("Greenlandic", "kl", true);
          addName("Inupiak", "ik", true);
          addName("Volapuk", "vo", true);
          addName("Byelorussian", "be", true);
          addName("Faeroese", "fo", true);
          addName("Singhalese", "si", true);
          addName("Gaelic", "ga", true); // IRISH
          addName("Bhutani", "dz", true);
          addName("Setswana", "tn", true);
          addName("Siswati", "ss", true);
          addName("Sangro", "sg", true);
          //addName("Kirundi", "XXX"); // no ISO2 code
          //addName("Sudanese", "XXX"); // ???
          break;
        case REGION:
          for (String code : sc.getAvailableCodes("territory")) {
            String name = english.getName("territory",code);
            if (name == null) {
              //System.out.println("Missing name for: " + code);
              continue;
            }
            addName(name, code, false);
          }
          // add irregular names
          addName("Bosnia and Herzegowina", "BA", true);
          addName("Congo", "CG", true);
          addName("Congo, Democratic Republic of the", "CD", true);
          addName("Congo, The Democratic Republic of the", "CD", true);
          addName("Cote D'ivoire", "CI", true);
          addName("Côte d'Ivoire", "CI", true);
          addName("Equitorial Guinea", "GQ", true);
          addName("French Quiana", "GF", true);
          addName("Heard and Mc Donald Islands", "HM", true);
          addName("Holy See (Vatican City State)", "VA", true);
          addName("Iran (Islamic Republic of)", "IR", true);
          addName("Korea, Democratic People's Republic of", "KP", true);
          addName("Korea, Republic of", "KR", true);
          addName("Libyan Arab Jamahiriya", "LY", true);
          addName("Lichtenstein", "LI", true);
          addName("Macao", "MO", true);
          addName("Micronesia, Federated States of", "FM", true);
          addName("Palestine", "PS", true);
          addName("Serbia and Montenegro", "CS", true);
          addName("Slovakia (Slovak Republic)", "SK", true);
          addName("São Tomé and Príncipe", "ST", true);
          addName("The Former Yugoslav Republic of Macedonia", "MK", true);
          addName("United States minor outlying islands", "UM", true);
          addName("Vatican City", "VA", true);
          addName("Virgin Islands, British", "VG", true);
          addName("Virgin Islands, U.S.", "VI", true);
          addName("Zaire", "CD", true);
          addName("Åland Islands", "AX", true);
          break;
        case CURRENCY:
          for (String code : sc.getAvailableCodes("currency")) {
            String name = english.getName("currency",code);
            if (name == null) {
              //System.out.println("Missing name for: " + code);
              continue;
            }
            addName(name, code, false);
          }
          // add irregular names
          addName("Australian Dollars", "AUD", true);
          addName("Bolivian Boliviano", "BOB", true);
          addName("British Pounds Sterling", "GBP", true);
          addName("Bulgarian Lev", "BGN", true);
          addName("Canadian Dollars", "CAD", true);
          addName("Czech Koruna", "CZK", true);
          addName("Danish Kroner", "DKK", true);
          addName("Denmark Kroner", "DKK", true);
          addName("Deutsche Marks", "DEM", true);
          addName("Euros", "EUR", true);
          addName("French Franks", "FRF", true);
          addName("Hong Kong Dollars", "HKD", true);
          addName("Israeli Shekel", "ILS", true);
          addName("Lithuanian Litas", "LTL", true);
          addName("Mexico Peso", "MXN", true);
          addName("New Romanian Leu", "RON", true);
          addName("New Taiwan Dollar", "TWD", true);
          addName("New Zealand Dollars", "NZD", true);
          addName("Norway Kroner", "NOK", true);
          addName("Norwegian Kroner", "NOK", true);
          addName("Peruvian Nuevo Sol", "PEN", true);
          addName("Polish New Zloty", "PLN", true);
          addName("Polish NewZloty", "PLN", true);
          addName("Russian Rouble", "RUB", true);
          addName("Singapore Dollars", "SGD", true);
          addName("Slovenian Tolar", "SIT", true);
          addName("Sweden Kronor", "SEK", true);
          addName("Swedish Kronor", "SEK", true);
          addName("Swiss Francs", "CHF", true);
          addName("US Dollars", "USD", true);
          addName("United Arab EmiratesD irham", "AED", true);
          addName("Venezuela Bolivar", "VEB", true);
          addName("Yuan Renminbi", "CNY", true);
          break;
        case TIMEZONE:
          for (String code : sc.getAvailableCodes("tzid")) {
            String[] parts = code.split("/");
            addName(parts[parts.length-1].replace("_"," "), code, false);
          }
          // add irregular names
          addName("Alaska Time", "America/Anchorage", true);
          //addName("Atlantic Time", "XXX", true);
          //addName("Atlantic Time - Halifax", "America/Halifax", true);
          addName("Canary Islands", "Atlantic/Canary", true);
          //addName("Central European Time", "XXX", true);
          //addName("Central European Time - Madrid", "Europe/Madrid", true);
          //addName("Central Time", "America/Chicago", true);
          //addName("Central Time - Adelaide", "Australia/Adelaide", true);
          //addName("Central Time - Darwin", "Australia/Darwin", true);
          //addName("Central Time - Mexico City", "America/Mexico_City", true);
          //addName("Central Time - Mexico City, Monterrey", "America/Monterrey", true);
          //addName("Central Time - Regina", "America/Regina", true);
          //addName("Central Time - Sasketchewan", "XXX", true);
          //addName("Central Time - Winnipeg", "America/Winnipeg", true);
          //addName("China Time - Beijing", "XXX", true);
          addName("Dumont D'Urville", "Antarctica/DumontDUrville", true);
          addName("Easter Island", "Pacific/Easter", true);
          //addName("Eastern European Time", "XXX", true);
          //addName("Eastern Standard Time", "XXX", true);
          //addName("Eastern Time", "XXX", true);
          //addName("Eastern Time - Brisbane", "Australia/Brisbane", true);
          //addName("Eastern Time - Hobart", "Australia/Hobart", true);
          //addName("Eastern Time - Iqaluit", "America/Iqaluit", true);
          //addName("Eastern Time - Melbourne, Sydney", "XXX", true);
//        addName("Eastern Time - Montreal", "XXX", true);
//        addName("Eastern Time - Toronto", "XXX", true);
//        addName("GMT (no daylight saving)", "XXX", true);
//        addName("Greenwich Mean Time", "XXX", true);
          //addName("Hanoi", "XXX", true);
//        addName("Hawaii Time", "XXX", true);
//        addName("India Standard Time", "XXX", true);
//        addName("International Date Line West", "XXX", true);
//        addName("Japan Time", "XXX", true);
//        addName("Moscow+00", "XXX", true);
//        addName("Moscow+01 - Samara", "XXX", true);
//        addName("Moscow+02 - Yekaterinburg", "XXX", true);
//        addName("Moscow+03 - Omsk, Novosibirsk", "XXX", true);
//        addName("Moscow+04 - Krasnoyarsk", "XXX", true);
//        addName("Moscow+05 - Irkutsk", "XXX", true);
//        addName("Moscow+06 - Yakutsk", "XXX", true);
//        addName("Moscow+07 - Vladivostok, Sakhalin", "XXX", true);
//        addName("Moscow+07 - Yuzhno-Sakhalinsk", "XXX", true);
//        addName("Moscow+08 - Magadan", "XXX", true);
//        addName("Moscow+09 - Kamchatka, Anadyr", "XXX", true);
//        addName("Moscow+09 - Petropavlovsk-Kamchatskiy", "XXX", true);
//        addName("Moscow-01 - Kaliningrad", "XXX", true);
//        addName("Mountain Time", "XXX", true);
//        addName("Mountain Time - Arizona", "XXX", true);
//        addName("Mountain Time - Chihuahua, Mazatlan", "XXX", true);
//        addName("Mountain Time - Dawson Creek", "XXX", true);
//        addName("Mountain Time - Edmonton", "XXX", true);
//        addName("Mountain Time - Hermosillo", "XXX", true);
//        addName("Mountain Time - Yellowknife", "XXX", true);
//        addName("Newfoundland Time - St. Johns", "XXX", true);
//        addName("Pacific Time", "XXX", true);
//        addName("Pacific Time - Tijuana", "XXX", true);
//        addName("Pacific Time - Vancouver", "XXX", true);
//        addName("Pacific Time - Whitehorse", "XXX", true);
          addName("Salvador", "America/El_Salvador", true);
          addName("St. Kitts", "America/St_Kitts", true);
          addName("St. Lucia", "America/St_Lucia", true);
          addName("St. Thomas", "America/St_Thomas", true);
          addName("St. Vincent", "America/St_Vincent", true);
          //addName("Tel Aviv", "XXX", true);
//        addName("Western European Time", "XXX", true);
//        addName("Western European Time - Canary Islands", "XXX", true);
//        addName("Western European Time - Ceuta", "XXX", true);
//        addName("Western Time - Perth", "XXX", true);
          break;
        case MONTH:
        case MONTHSHORT:          
          String[] names = type == Type.MONTH ? dfs.getMonths() : dfs.getShortMonths();
          for (int i = 0; i < names.length; ++i) {
            addName(names[i], String.valueOf(i+1), true);
          }
          break;
        case DAY:
        case DAYSHORT:
          String[] names2 = type == Type.DAY ? dfs.getWeekdays() : dfs.getShortWeekdays();
          for (int i = 1; i < names2.length; ++i) {
            addName(names2[i], names2[i].substring(0,3).toLowerCase(Locale.ENGLISH), true);
          }
          break;
        default: 
          //throw new IllegalArgumentException();
          break;
      }
    }
    public String getCldrValue(CLDRFile cldrFile, String id) {
      String result = cldrFile.getStringValue(getPath(id));
      // cldrFile.getName(CLDRFile.LANGUAGE_NAME, id, false);
      if (result == null && type == Type.TIMEZONE) {
        String[] parts = id.split("/");
        result = parts[parts.length-1].replace("_"," ");
      }
      return result;
    }
    boolean matches(String text) {
      return matcher.reset(text).matches();
    }
    String getCode(String value) {
      return name_code.get(value);
    }
    public int compareTo(Object o) {
      throw new IllegalArgumentException();
    }
    String getPath(String id, int count) {
      String result = getPath(id);
      count += 650;
      result += "[@alt=\"proposed-x" + count + "\"]";
      result += "[@draft=\"provisional\"]";
      return result;
    }
    String getPath(String id) {
      switch (type) {
        case LANGUAGE: return CLDRFile.getKey(CLDRFile.LANGUAGE_NAME, id);
        case REGION: return CLDRFile.getKey(CLDRFile.TERRITORY_NAME, id);
        case CURRENCY: return CLDRFile.getKey(CLDRFile.CURRENCY_NAME, id);
        case TIMEZONE: return "//ldml/dates/timeZoneNames/zone[@type=\"$1\"]/exemplarCity".replace("$1",id);
        case MONTH: return "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"$1\"]".replace("$1",id);
        case MONTHSHORT: return "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"$1\"]".replace("$1",id);
        case DAY: return "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"$1\"]".replace("$1",id);
        case DAYSHORT: return "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"abbreviated\"]/day[@type=\"$1\"]".replace("$1",id);
      }
      return null;
      // 
    }
  }
}