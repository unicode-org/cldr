package org.unicode.cldr.tool;

import org.unicode.cldr.util.Iso639Data;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.Iso639Data.Source;
import org.unicode.cldr.util.Iso639Data.Type;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class GenerateLanguageData {
  //static StandardCodes sc = StandardCodes.make();
  static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
  static Iso639Data iso639Data = new Iso639Data();

  public static void main(String[] args) {
    //Set<String> languageRegistryCodes = sc.getAvailableCodes("language");
    Set<String> languageCodes = new TreeSet(iso639Data.getAvailable());
    
    System.out.println("Macrolanguages");
    Set<String> bcp47languages = StandardCodes.make().getAvailableCodes("language");
    for (String languageCode : languageCodes) {
      Set<String> suffixes = iso639Data.getEncompassedForMacro(languageCode);
      if (suffixes == null) continue;
//    System.out.println(
//    languageCode 
//    + "\t"
//    //+ "\t" + iso639Data.getSource(languageCode)
//    + "\t" + (bcp47languages.contains(languageCode) ? "" : "new")
//    //+ "\t" + iso639Data.getScope(languageCode)
//    //+ "\t" + iso639Data.getType(languageCode)
//    + "\t" + Utility.join(iso639Data.getNames(languageCode),"; ")
//    );
      for (String suffix : new TreeSet<String>(suffixes)) {
        System.out.println(
            languageCode 
            + "\t" + (bcp47languages.contains(languageCode) ? "4646" : "new")
            + "\t" + iso639Data.getNames(languageCode).iterator().next() // Utility.join(iso639Data.getNames(languageCode),"; ")
            + "\t" + suffix 
            //+ "\t" + iso639Data.getSource(suffix)
            + "\t" + (bcp47languages.contains(suffix) ? "4646" : "new")
            //+ "\t" + iso639Data.getScope(suffix)
            //+ "\t" + iso639Data.getType(suffix)
            + "\t" + iso639Data.getNames(suffix).iterator().next()
        );
      }
    }
    System.out.println("All");
    //languageCodes.addAll(languageRegistryCodes);
    Relation<String,String> type_codes = new Relation(new TreeMap(), TreeSet.class);
    for (String languageCode : languageCodes) {
      Scope scope = iso639Data.getScope(languageCode);
      Type type = iso639Data.getType(languageCode);
      Set<String> names = iso639Data.getNames(languageCode);
      Source source = iso639Data.getSource(languageCode);
      String prefix = iso639Data.getMacroForEncompassed(languageCode);
      Set<String> prefixNames = prefix == null ? null : iso639Data.getNames(prefix);
      String prefixName = prefixNames == null || prefixNames.size() == 0 ? "" : prefixNames.iterator().next() + "::\t";
      String fullCode = (prefix != null ? prefix + "-" : "") + languageCode;
      String scopeString = String.valueOf(scope);
      if (iso639Data.getEncompassedForMacro(languageCode) != null) {
        scopeString += "*";
      }
      System.out.println(
          fullCode 
          + "\t" + source
          + "\t" + scopeString
          + "\t" + type
          + "\t" + prefixName + CldrUtility.join(names,"\t")
          );
      type_codes.put(source + "\t" + scopeString + "\t" + type, fullCode);
    }
    for (String type : type_codes.keySet()) {
      Set<String> codes = type_codes.getAll(type);
      System.out.println(codes.size() + "\t" + type + "\t" + truncate(codes));
    }
   }

  private static String truncate(Object codes) {
    String result = codes.toString();
    if (result.length() < 100) return result;
    return result.substring(0,99) + '\u2026';
  }
}