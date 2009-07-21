package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.Relation;

public class Iso639Data {

  static Map<String, String> toAlpha3;

  static Map<String, String> fromAlpha3;

  static Map<String, String> toBiblio3;

  static Map<String, String> fromBiblio3;

  static Relation<String, String> toNames;

  static Map<String, Scope> toScope;

  static Map<String, Type> toType;

  static Map<String, String> suffix_prefix;

  static Relation<String, String> prefix_suffix;

  static Map<String, Source> toSource;

  private static String version;

  /**
   * <h3><a NAME="I">Individual</a> languages</h3>
   * <p>
   * Judgments regarding when two varieties are considered to be the same or
   * different languages are based on a number of factors, including linguistic
   * similarity, intelligibility, a common literature, the views of speakers
   * concerning the relationship between language and identity, and other
   * factors.
   * </p>
   * <h3><a NAME="M">Macrolanguages</a></h3>
   * <p>
   * In various parts of the world, there are clusters of closely-related
   * language varieties that, based on the criteria discussed above, can be
   * considered distinct individual languages, yet in certain usage contexts a
   * single language identity for all is needed.
   * </p>
   * <p>
   * Macrolanguages are distinguished from language collections in that the
   * individual languages that correspond to a macrolanguage must be very
   * closely related, and there must be some domain in which only a single
   * language identity is recognized.
   * </p>
   * 
   * <h3><a NAME="C">Collections</a> of languages</h3>
   * <p>
   * A collective language code element is an identifier that represents a group
   * of individual languages that are not deemed to be one language in any usage
   * context.
   * </p>
   * </p>
   * <h3><a NAME="R">Private Use</a></h3>
   * <p>
   * Identifiers <tt>qaa</tt> through <tt>qtz</tt> are reserved for local
   * use, to be used in cases in which there is no suitable existing code in ISO
   * 639. There are no constraints as to scope of denotation. These identifiers
   * may only be used locally, and may not be used in interchange without a
   * private agreement.
   * </p>
   * <h3><a NAME="S">Special situations</a></h3>
   * <p>
   * A few code elements are defined for other special situations.
   * </p>
   * For more information, see http://www.sil.org/iso639-3/scope.asp
   * <p>
   * Note that the casing on these enum values is chosen to match standard
   * usage.
   * </p>
   */
  public enum Scope {
    Individual, Macrolanguage, Special, Collection, PrivateUse, Unknown
  };

  /**
   * <h3><a NAME="L"></a>Living languages</h3>
   * <p>
   * A language is listed as <i>living</i> when there are people still living
   * who learned it as a first language.
   * </p>
   * <h3><a NAME="E"></a>Extinct languages</h3>
   * 
   * <p>
   * A language is listed as <i>extinct</i> if it has gone extinct in recent
   * times. (e.g. in the last few centuries).
   * </p>
   * <h3><a NAME="A"></a>Ancient languages</h3>
   * <p>
   * A language is listed as <i>ancient</i> if it went extinct in ancient times
   * (e.g. more than a millennium ago).
   * </p>
   * <h3><a NAME="H"></a>Historic languages</h3>
   * <p>
   * A language is listed as <i>historic</i> when it is considered to be
   * distinct from any modern languages that are descended from it; for
   * instance, Old English and Middle English.
   * </p>
   * 
   * <h3><a NAME="C"></a>Constructed languages</h3>
   * <p>
   * Artificial languages are those like Esperanto: it excludes programming
   * languages.
   * </p>
   * <p>
   * Note that the casing on these enum values is chosen to match standard
   * usage. <i>For more information, see http://www.sil.org/iso639-3/scope.asp</i>
   * </p>
   */
  public enum Type {
    Ancient, Constructed, Extinct, Historical, Living, Special, Collection, Unknown
  };

  /**
   * This indicates the source of the language subtag.
   * 
   * @author markdavis
   * 
   */
  public enum Source {
    ISO_639_1, ISO_639_2, ISO_639_3, BCP47, CLDR
  };

  public static String getVersion() {
    return version;
  }

  public static Source getSource(String languageSubtag) {
    if (toAlpha3 == null) {
      getData();
    }
    if (!toNames.containsKey(languageSubtag)) {
      return null;
    }
    Source result = toSource.get(languageSubtag);
    if (result == null)
      return Source.ISO_639_3;
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

   public static String fromBiblio3(String biblio3) {
     if (toNames == null) {
       getData();
     }
     String result = fromBiblio3.get(biblio3);
     if (result != null) {
       return result;
     }
     return fromAlpha3(biblio3);
   }

   public static String toBiblio3(String languageTag) {
     if (toNames == null) {
       getData();
     }
     String result = toBiblio3.get(languageTag);
     if (result != null) {
       return result;
     }
     return toAlpha3(languageTag);
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
    if (!toNames.keySet().contains(languageSubtag))
      return Scope.Unknown;
    Scope result = toScope.get(languageSubtag);
    if (result != null)
      return result;
    return Scope.Individual;
  }

  public static Type getType(String languageSubtag) {
    if (toAlpha3 == null) {
      getData();
    }
    if (!toNames.keySet().contains(languageSubtag))
      return Type.Unknown;
    Type result = toType.get(languageSubtag);
    if (result != null)
      return result;
    return Type.Living;
  }

  /**
   * Id char(3) NOT NULL, -- The three-letter 639-3 identifier Part2B char(3)
   * NULL, -- Equivalent 639-2 identifier of the bibliographic applications code
   * set, if there is one Part2T char(3) NULL, -- Equivalent 639-2 identifier of
   * the terminology applications code set, if there is one Part1 char(2) NULL, --
   * Equivalent 639-1 identifier, if there is one Scope char(1) NOT NULL, --
   * I(ndividual), M(acrolanguage), S(pecial) Type char(1) NOT NULL, --
   * A(ncient), C(onstructed), -- E(xtinct), H(istorical), L(iving), S(pecial)
   * Ref_Name varchar(150) NOT NULL) -- Reference language name
   * 
   * @throws IOException
   */
  enum IsoColumn {
    Id, Part2B, Part2T, Part1, Scope, Type, Ref_Name
  };

  /**
   * Id char(3) NOT NULL, -- The three-letter 639-3 identifier Print_Name
   * varchar(75) NOT NULL, -- One of the names associated with this identifier
   * Inverted_Name varchar(75) NOT NULL) -- The inverted form of this Print_Name
   * form
   */
  enum IsoNamesColumn {
    Id, Print_Name, Inverted_Name
  };

  private static void getData() {
    try {
      BufferedReader in = Utility.getUTF8Data("iso-639-3.tab");
      version = in.readLine().trim();
      in.close();
      
      in = Utility.getUTF8Data("iso-639-3.tab");
      Pattern tabs = Pattern.compile("\\t");
      toAlpha3 = new HashMap();
      fromAlpha3 = new HashMap();
      toBiblio3 = new HashMap();
      fromBiblio3 = new HashMap();
      toScope = new HashMap();
      toType = new HashMap();
      toNames = new Relation(new TreeMap(), LinkedHashSet.class);
      prefix_suffix = new Relation(new TreeMap(), LinkedHashSet.class);
      suffix_prefix = new HashMap();
      toSource = new HashMap();
      toSource.put("sh", Source.ISO_639_1); // add deprecated language

      while (true) {
        String line = in.readLine();
        if (line == null)
          break;
        if (line.startsWith("\uFEFF"))
          line = line.substring(1);
        String[] parts = tabs.split(line);
        String alpha3 = parts[IsoColumn.Id.ordinal()];
        if (alpha3.equals("Id"))
          continue;
        String languageSubtag = alpha3;
        if (parts[IsoColumn.Part1.ordinal()].length() != 0) { // parts.length >
          // IsoColumn.Part1.ordinal()
          // &&
          languageSubtag = parts[IsoColumn.Part1.ordinal()];
          toAlpha3.put(languageSubtag, alpha3);
          fromAlpha3.put(alpha3, languageSubtag);
        }
        
        if (parts[IsoColumn.Part2B.ordinal()].length() != 0) { // parts.length >
          // IsoColumn.Part1.ordinal()
          // &&
          String biblio = parts[IsoColumn.Part2B.ordinal()];
          if (!biblio.equals(alpha3)) {
            toBiblio3.put(languageSubtag, biblio);
            fromBiblio3.put(biblio, languageSubtag);
          }
        }
        
        toNames.put(languageSubtag, parts[IsoColumn.Ref_Name.ordinal()]);
        Scope scope = findMatchToPrefix(parts[IsoColumn.Scope.ordinal()], Scope.values());
        if (scope != Scope.Individual)
          toScope.put(languageSubtag, scope);
        Type type = findMatchToPrefix(parts[IsoColumn.Type.ordinal()], Type.values());
        if (type != Type.Living)
          toType.put(languageSubtag, type);
      }
      //System.out.println("Size:\t" + toNames.size());
      in.close();

      // Id Print_Name Inverted_Name
      in = Utility.getUTF8Data("iso-639-3-macrolanguages.tab");
      while (true) {
        String line = in.readLine();
        if (line == null)
          break;
        if (line.startsWith("\uFEFF"))
          line = line.substring(1);
        String[] parts = tabs.split(line);
        String prefix = parts[0];
        if (prefix.equals("M_Id"))
          continue;
        prefix = fromAlpha3(prefix);
        String suffix = fromAlpha3(parts[1]);
        suffix_prefix.put(suffix, prefix);
        prefix_suffix.put(prefix, suffix);
        // skip inverted name for now
      }
      //System.out.println("Size:\t" + toNames.size());
      in.close();

      // Id Print_Name Inverted_Name
      in = Utility.getUTF8Data("iso-639-3_Name_Index.tab");
      while (true) {
        String line = in.readLine();
        if (line == null)
          break;
        if (line.startsWith("\uFEFF"))
          line = line.substring(1);
        String[] parts = tabs.split(line);
        String alpha3 = parts[IsoColumn.Id.ordinal()];
        if (alpha3.equals("Id"))
          continue;
        String languageSubTag = fromAlpha3(alpha3);
        toNames.put(languageSubTag, parts[IsoNamesColumn.Print_Name.ordinal()]);
        // skip inverted name for now
      }
      //System.out.println("Size:\t" + toNames.size());
      in.close();

      in = Utility.getUTF8Data("ISO-639-2_values_8bits.txt");
      // An alpha-3 (bibliographic) code,
      // an alpha-3 (terminologic) code (when given),
      // an alpha-2 code (when given),
      // an English name,
      // and a French name of a language are all separated by pipe (|)
      // characters.
      int addCounter = 0;
      while (true) {
        String line = in.readLine();
        if (line == null)
          break;
        if (line.startsWith("\uFEFF"))
          line = line.substring(1);
        String[] parts = line.split("\\s*\\|\\s*");
        String alpha3 = parts[0];
        if (alpha3.equals("qaa-qtz")) {
          for (char second = 'a'; second <= 't'; ++second) {
            for (char third = 'a'; third <= 'z'; ++third) {
              String languageSubtag = (("q" + second) + third);
              toScope.put(languageSubtag, Scope.PrivateUse);
              toType.put(languageSubtag, Type.Special);
              toNames.put(languageSubtag, "private-use");
              toSource.put(languageSubtag, Source.ISO_639_2);
            }
          }
          continue;
        }
        if (parts[1].length() != 0)
          alpha3 = parts[1];
        String languageSubtag = parts[2];
        if (languageSubtag.length() == 0) {
          languageSubtag = alpha3;
        }
        String[] english = parts[3].split(";");
        toSource.put(languageSubtag, languageSubtag.length() == 2 ? Source.ISO_639_1 : Source.ISO_639_2);
        if (!toNames.containsKey(languageSubtag)) {
          // we don't have it already,
          //System.out.println("Adding2: " + alpha3 + "\t" + languageSubtag + "\t" + Arrays.asList(english));
          if (languageSubtag.length() == 2) {
            toAlpha3.put(languageSubtag, alpha3);
            fromAlpha3.put(alpha3, languageSubtag);
          }
          toScope.put(languageSubtag, Scope.Collection);
          toType.put(languageSubtag, Type.Special);
          toNames.putAll(languageSubtag, Arrays.asList(english));
        }
        // skip inverted name for now
      }
      in.close();
      //System.out.println("Size:\t" + toNames.size());

      // make data unmodifiable, just to prevent mistakes

      toAlpha3 = Collections.unmodifiableMap(toAlpha3);
      fromAlpha3 = Collections.unmodifiableMap(fromAlpha3);
      toBiblio3 = Collections.unmodifiableMap(toBiblio3);
      fromBiblio3 = Collections.unmodifiableMap(fromBiblio3);
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
    if (prefix != null)
      return prefix;
    if (suffix.equals("sgn"))
      return null;
    Set<String> names = toNames.getAll(suffix);
    if (names == null)
      return null;
    for (String name : names) {
      if (name.contains("Sign Language"))
        return "sgn";
    }
    return null;
  }

  public static Set<String> getSuffixes(String prefix) {
    return prefix_suffix.getAll(prefix);
  }
}