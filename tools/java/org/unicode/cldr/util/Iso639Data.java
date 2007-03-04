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
  
  public enum Scope {Individual, Macrolanguage, Special};
  public enum Type {Ancient, Constructed, Extinct, Historical, Living, Special};
  
  public static String toAlpha3(String languageSubtag) {
    if (toAlpha3 == null) {
      getData();
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
    return toScope.get(languageSubtag);
  }
  public static Type getType(String languageSubtag) {
    if (toAlpha3 == null) {
      getData();
    }
    return toType.get(languageSubtag);
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
      toNames = new Relation(new TreeMap(), LinkedHashSet.class, null);
      EnumSet allScope = EnumSet.allOf(Scope.class);
      EnumSet allType = EnumSet.allOf(Type.class);
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
      in.close();
      
      // make data unmodifiable, just to prevent mistakes
      
      toAlpha3 = Collections.unmodifiableMap(toAlpha3);
      fromAlpha3 = Collections.unmodifiableMap(fromAlpha3);
      toScope = Collections.unmodifiableMap(toScope);
      toType = Collections.unmodifiableMap(toType);
      toNames.freeze();
      
    } catch (IOException e) {
      throw (RuntimeException) new IllegalArgumentException("Cannot parse iso-fdis-639-3_20061114.tab").initCause(e);
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
    return toNames.keySet();
  }
}