package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class Iso639Data {
  
  static Map<String, String> toAlpha3;
  static Map<String, String> fromAlpha3;
  static Relation<String, String> toNames;
  static Map<String, Scope> toScope;
  static Map<String, Type> toType;
  static Map<String,String> suffix_prefix;
  static Relation<String,String> prefix_suffix;
  static Map<String,Source> toSource;
  
  public enum Scope {Individual, Macrolanguage, Special, Collection, PrivateUse};
  public enum Type {Ancient, Constructed, Extinct, Historical, Living, Special};
  public enum Source {Iso639_1, Iso639_2, Iso639_3};
  
  public static Source getSource(String languageSubtag) {
    if (toAlpha3 == null) {
      getData();
    }
    if (!toNames.containsKey(languageSubtag)) {
      return null;
    }
    Source result = toSource.get(languageSubtag);
    if (result == null) return Source.Iso639_3;
    return result;
  }

  public static String toAlpha3(String languageSubtag) {
    if (toAlpha3 == null) {
      getData();
    }
    if (!toNames.containsKey(languageSubtag)) {
      return null;
    }
    return toAlpha3.get(languageSubtag);
  }
  
  public static String fromAlpha3(String alpha3) {
    if (fromAlpha3 == null) {
      getData();
    }
    String alpha2 = fromAlpha3.get(alpha3);
    if (alpha2 != null) {
      return alpha2;
    }
    // it only exists if it has a name
    if (toNames.containsKey(alpha3)) {
      return alpha3;
    }
    return null;
  }

  public static Set<String> getNames(String languageSubtag) {
    if (toNames == null) {
      getData();
    }
    return toNames.getAll(languageSubtag);
  }

  public static Scope getScope(String languageSubtag) {
    if (toScope == null) {
      getData();
    }
    if (!toNames.keySet().contains(languageSubtag)) return null;
    Scope result = toScope.get(languageSubtag);
    if (result != null) return result;
    return Scope.Individual;
  }
  
  public static Type getType(String languageSubtag) {
    if (toAlpha3 == null) {
      getData();
    }
    if (!toNames.keySet().contains(languageSubtag)) return null;
    Type result = toType.get(languageSubtag);
    if (result != null) return result;
    return Type.Living;
  }
  /**
   Id      char(3) NOT NULL,  -- The three-letter 639-3 identifier
   Part2B  char(3) NULL,      -- Equivalent 639-2 identifier of the bibliographic applications code set, if there is one
   Part2T  char(3) NULL,      -- Equivalent 639-2 identifier of the terminology applications code set, if there is one
   Part1   char(2) NULL,      -- Equivalent 639-1 identifier, if there is one    
   Scope   char(1) NOT NULL,  -- I(ndividual), M(acrolanguage), S(pecial)
   Type    char(1) NOT NULL,  -- A(ncient), C(onstructed),  
                              -- E(xtinct), H(istorical), L(iving), S(pecial)
   Ref_Name   varchar(150) NOT NULL)   -- Reference language name 
   * @throws IOException
   */
  enum IsoColumn {Id, Part2B, Part2T, Part1, Scope, Type, Ref_Name};
  /**
   Id             char(3)     NOT NULL,  -- The three-letter 639-3 identifier
   Print_Name     varchar(75) NOT NULL,  -- One of the names associated with this identifier 
   Inverted_Name  varchar(75) NOT NULL)  -- The inverted form of this Print_Name form   
   */
  enum IsoNamesColumn {Id, Print_Name, Inverted_Name};
  
  private static void getData() {
    try {
      BufferedReader in = Utility.getUTF8Data("iso-639-3_20070205.tab");
      Pattern tabs = Pattern.compile("\\t");
      toAlpha3 = new HashMap();
      fromAlpha3 = new HashMap();
      toScope = new HashMap();
      toType = new HashMap();
      toNames = new Relation(new TreeMap(), LinkedHashSet.class);
      prefix_suffix = new Relation(new TreeMap(), LinkedHashSet.class);
      suffix_prefix = new HashMap();
      toSource = new HashMap();
    
      while (true) {
        String line = in.readLine();
        if (line == null) break;
        if (line.startsWith("\uFEFF")) line = line.substring(1);
        String[] parts = tabs.split(line);
        String alpha3 = parts[IsoColumn.Id.ordinal()];
        if (alpha3.equals("Id")) continue;
        String languageSubtag = alpha3;
        if (parts[IsoColumn.Part1.ordinal()].length() != 0) { // parts.length > IsoColumn.Part1.ordinal() && 
          languageSubtag = parts[IsoColumn.Part1.ordinal()];
          toAlpha3.put(languageSubtag,alpha3);
          fromAlpha3.put(alpha3,languageSubtag);
        }
        toNames.put(languageSubtag, parts[IsoColumn.Ref_Name.ordinal()]);
        Scope scope = findMatchToPrefix(parts[IsoColumn.Scope.ordinal()], Scope.values());
        if (scope != Scope.Individual) toScope.put(languageSubtag, scope);
        Type type = findMatchToPrefix(parts[IsoColumn.Type.ordinal()], Type.values());
        if (type != Type.Living) toType.put(languageSubtag, type);
      }
      System.out.println("Size:\t" + toNames.size());
      in.close();
      
      // ﻿Id  Print_Name  Inverted_Name
      in = Utility.getUTF8Data("iso-639-3-macrolanguages_20070205.tab");
      while (true) {
        String line = in.readLine();
        if (line == null) break;
        if (line.startsWith("\uFEFF")) line = line.substring(1);
        String[] parts = tabs.split(line);
        String prefix = parts[0];
        if (prefix.equals("M_Id")) continue;
        prefix = fromAlpha3(prefix);
        String suffix = fromAlpha3(parts[1]);
        suffix_prefix.put(suffix, prefix);
        prefix_suffix.put(prefix, suffix);
        // skip inverted name for now
      }
      System.out.println("Size:\t" + toNames.size());
      in.close();

      
      // ﻿Id  Print_Name  Inverted_Name
      in = Utility.getUTF8Data("iso-639-3_Name_Index_20070205.tab");
      while (true) {
        String line = in.readLine();
        if (line == null) break;
        if (line.startsWith("\uFEFF")) line = line.substring(1);
        String[] parts = tabs.split(line);
        String alpha3 = parts[IsoColumn.Id.ordinal()];
        if (alpha3.equals("Id")) continue;
        String languageSubTag = fromAlpha3(alpha3);
        toNames.put(languageSubTag, parts[IsoNamesColumn.Print_Name.ordinal()]);
        // skip inverted name for now
      }
      System.out.println("Size:\t" + toNames.size());
      in.close();

      in = Utility.getUTF8Data("ISO-639-2_values_8bits.txt");
      // An alpha-3 (bibliographic) code, 
      // an alpha-3 (terminologic) code (when given), 
      // an alpha-2 code (when given), 
      // an English name, 
      // and a French name of a language are all separated by pipe (|) characters.
      int addCounter = 0;
      while (true) {
        String line = in.readLine();
        if (line == null) break;
        if (line.startsWith("\uFEFF")) line = line.substring(1);
        String[] parts = line.split("\\s*\\|\\s*");
        String alpha3 = parts[0];
        if (alpha3.equals("qaa-qtz")) {
          for (char second = 'a'; second <= 't'; ++second) {
            for (char third = 'a'; third <= 'z'; ++third) {
              String languageSubtag = (("q" + second) + third);
              toScope.put(languageSubtag, Scope.PrivateUse);
              toType.put(languageSubtag, Type.Special);
              toNames.put(languageSubtag, "private-use");
              toSource.put(languageSubtag, Source.Iso639_2);
            }
          }
          continue;
        }
        if (parts[1].length() != 0) alpha3 = parts[1];
        String languageSubtag = parts[2];
        if (languageSubtag.length() == 0) {
          languageSubtag = alpha3;
        }
        String[] english = parts[3].split(";");
        toSource.put(languageSubtag, languageSubtag.length() == 2 ? Source.Iso639_1 : Source.Iso639_2);
        if (!toNames.containsKey(languageSubtag)) {
          // we don't have it already,
          System.out.println("Adding2: " + alpha3 + "\t" + languageSubtag + "\t" + Arrays.asList(english));
          if (languageSubtag.length() == 2) {
            toAlpha3.put(languageSubtag,alpha3);
            fromAlpha3.put(alpha3,languageSubtag);
          }
          toScope.put(languageSubtag,Scope.Collection);
          toType.put(languageSubtag,Type.Special);
          toNames.putAll(languageSubtag,Arrays.asList(english));
        }
        // skip inverted name for now
      }
      in.close();
      System.out.println("Size:\t" + toNames.size());

      // make data unmodifiable, just to prevent mistakes
      
      toAlpha3 = Collections.unmodifiableMap(toAlpha3);
      fromAlpha3 = Collections.unmodifiableMap(fromAlpha3);
      toScope = Collections.unmodifiableMap(toScope);
      toType = Collections.unmodifiableMap(toType);
      toNames.freeze();
      prefix_suffix.freeze();
      
    } catch (IOException e) {
      throw (RuntimeException) new IllegalArgumentException("Cannot parse file").initCause(e);
    }    
  }

  public static <T> T findMatchToPrefix(String prefix, T[] values) {
   for (T x : values) {
      if (x.toString().startsWith(prefix)) {
        return x;
      }
    }
    throw new IllegalArgumentException("Prefix <" + prefix + "> not found in " + Arrays.asList(values));
  }

  public static Set<String> getAvailable() {
    if (toAlpha3 == null) {
      getData();
    }
    return toNames.keySet();
  }

  public static String getPrefix(String suffix) {
    String prefix = suffix_prefix.get(suffix);
    if (prefix != null) return prefix;
    if (suffix.equals("sgn")) return null;
    Set<String> names = toNames.getAll(suffix);
    if (names == null) return null;
    for (String name : names) {
      if (name.contains("Sign Language")) return "sgn";
    }
    return null;
  }
  
  public static Set<String> getSuffixes(String prefix) {
    return prefix_suffix.getAll(prefix);
  }
}