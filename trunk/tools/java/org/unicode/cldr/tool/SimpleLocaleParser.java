package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse Locales, extended to BCP 47 and CLDR. Also normalizes the case of the results.
 * Only does syntactic parse: does not replace deprecated elements; does not check for validity.
 * Will throw IllegalArgumentException for duplicate variants and extensions.
 * @author markdavis
 */
class SimpleLocaleParser {
  // mechanically generated regex -- don't worry about trying to read it!
  // if we want to allow multiple --, change [-_] into [-_]+
  private static final Pattern rootPattern = Pattern.compile(
          "(?:" +
          " (?: ( [a-z]{2,8} )" + // language
          "   (?: [-_] ( [a-z]{4} ) )?" + // script
          "   (?: [-_] ( [a-z]{2} | [0-9]{3} ) )?" + // region
          "   (?: [-_] ( (?: [a-z 0-9]{5,8} | [0-9] [a-z 0-9]{3} ) (?: [-_] (?: [a-z 0-9]{5,8} | [0-9] [a-z 0-9]{3} ) )* ) )?" + // variant(s)
          "   (?: [-_] ( [a-w y-z] (?: [-_] [a-z 0-9]{2,8} )+ (?: [-_] [a-w y-z] (?: [-_] [a-z 0-9]{2,8} )+ )* ) )?" + // extensions
          "   (?: [-_] ( x (?: [-_] [a-z 0-9]{1,8} )+ ) )? )" + // private use
          " | ( x (?: [-_] [a-z 0-9]{1,8} )+ )" + // private use
          " | ( en [-_] GB [-_] oed" + // grandfathered gorp
          "   | i [-_] (?: ami | bnn | default | enochian | hak | klingon | lux | mingo | navajo | pwn | tao | tay | tsu )" +
          "   | no [-_] (?: bok | nyn )" +
          "   | sgn [-_] (?: BE [-_] (?: fr | nl) | CH [-_] de )" +
          "   | zh [-_] (?: cmn (?: [-_] Hans | [-_] Hant )? | gan | min (?: [-_] nan)? | wuu | yue ) ) )" +
          " (?: \\@ ((?: [a-z 0-9]+ \\= [a-z 0-9]+) (?: \\; (?: [a-z 0-9]+ \\= [a-z 0-9]+))*))?" + // CLDR/ICU keywords
          "", Pattern.COMMENTS | Pattern.CASE_INSENSITIVE);  // TODO change above to be lowercase, since source is already when we compare
  // Other regex patterns for splitting apart lists of items detected above.
  private static final Pattern variantSeparatorPattern = Pattern.compile("[-_]");
  private static final Pattern extensionPattern = Pattern.compile("([a-z]) [-_] ( [a-z 0-9]{2,8} (?:[-_] [a-z 0-9]{2,8})* )", Pattern.COMMENTS);
  private static final Pattern privateUsePattern = Pattern.compile("(x) [-_] ( [a-z 0-9]{1,8} (?:[-_] [a-z 0-9]{1,8})* )", Pattern.COMMENTS);
  private static final Pattern keywordPattern = Pattern.compile("([a-z 0-9]+) \\= ([a-z 0-9]+)", Pattern.COMMENTS);
  
  /**
   * The fields set by set().
   */
  private String language;
  private String script;
  private String region;
  private List<String> variants;
  private Map<String,String> extensions;
  
  /**
   * Set the object to the source.
   * <p>Example (artificially complicated):
   * <pre> myParser.set("zh-Hans-HK-SCOUSE-a-foobar-x-a-en@collation=phonebook;calendar=islamic");
   * String language = myParser.getLanguage();
   * </pre>
   * @param source
   * @return
   */
  public boolean set(String source) {
    source = source.toLowerCase(Locale.ENGLISH);
    Matcher root = rootPattern.matcher(source);
    if (!root.matches()) {
      return false;
    }
    language = root.group(1);
    if (language == null) {
      language = root.group(8); // grandfathered
      if (language == null) {
        language = "und"; // placeholder for completely private use
      }
    }
    script = root.group(2);
    if (script == null) {
      script = "";
    } else {
      script = script.substring(0,1).toUpperCase(Locale.ENGLISH) + script.substring(1);
    }
    region = root.group(3);
    if (region == null) {
      region = "";
    } else {
      region = region.toUpperCase(Locale.ENGLISH);
    }
    final String variantList = root.group(4);
    if (variantList == null) {
      variants = (List<String>)Collections.EMPTY_LIST;
    } else {
      // make uppercase for compatibility with CLDR.
      variants = Arrays.asList(variantSeparatorPattern.split(variantList.toUpperCase(Locale.ENGLISH)));
      // check for duplicate variants
      if (new HashSet(variants).size() != variants.size()) {
        throw new IllegalArgumentException("Duplicate variants");
      }
    }
    extensions = new LinkedHashMap<String,String>(); // group 5 are extensions, 6 is private use
    // extensions are a bit more complicated
    addExtensions(root.group(5), extensionPattern);
    addExtensions(root.group(6), privateUsePattern);
    addExtensions(root.group(7), privateUsePattern);
    addExtensions(root.group(9), keywordPattern);
    extensions = Collections.unmodifiableMap(extensions);
    return true;
  }

  private void addExtensions(String item, Pattern pattern) {
    if (item != null) {
      Matcher extension = pattern.matcher(item);
      while (extension.find()) {
        final String key = extension.group(1);
        // check for duplicate keys
        if (extensions.containsKey(key)) {
          throw new IllegalArgumentException("duplicate key: " + key);
        }
        extensions.put(key, extension.group(2));
      }
    }
  }
  /**
   * Return BCP 47 language subtag (may be ISO registered code).
   * If the language tag is irregular, then the entire tag is in the language field.
   * If the entire code is private use, then the language code is "und".
   * Examples:
   * <table style="border-width:1; border-style:collapse">
   * <tr><th>Input String</th><th>Parsed</th></tr>
   * <tr><td>zh-cmn-Hans</td><td>{language=zh-cmn-hans, script=, country=, variants=[], keywords={}}</td></tr>
   * <tr><td>i-default@abc=def</td><td>{language=i-default, script=, country=, variants=[], keywords={abc=def}}</td></tr>
   * <tr><td>x-foobar@abc=def</td><td>{language=und, script=, country=, variants=[], keywords={x=foobar, abc=def}}</td></tr>
   * </table>
   * @return language subtag, lowercased.
   */
  public String getLanguage() {
    return language;
  }
  /**
   * Return BCP 47 script subtag (may be ISO or UN)
   * @return script subtag, titlecased.
   */
  public String getScript() {
    return script;
  }
  /**
   * Return BCP 47 region subtag (may be ISO or UN)
   * @return country (region) subtag, uppercased.
   */
  public String getCountry() {
    return region;
  }
  /**
   * Return immutable list of BCP 47 variants 
   * @return list of uppercased variants.
   */
  public List<String> getVariants() {
    return variants;
  }
  
  /**
   * Return the first variant, for compatibility 
   * @return first (uppercased) variant
   */
  public String getVariant() {
    return variants.size() == 0 ? "" : variants.iterator().next();
  }
  /**
   * Return immutable map of key/value extensions. Includes BCP 47 extensions and private use, also locale keyword extensions. If the entire code is private use,
   * then the language is set to "und" for consistency.
   * <p>Example:
   * <table style="border-width:1; border-style:collapse">
   * <tr><th>Input String</th><th>Parsed</th></tr>
   * <tr><td>zh-Hans-HK-SCOUSE-a-foobar-x-a-en@collation=phonebook;calendar=islamic</td><td>{language=zh, script=Hans, country=HK, variants=[SCOUSE], keywords={a=foobar, x=a-en, collation=phonebook, calendar=islamic}}</td></tr>
   * </table>
   * @return map of key/value pairs, lowercased.
   */
  public Map<String,String> getExtensions() {
    return extensions;
  }
  
  public String toString() {
    return "{language=" + language 
    + ", script=" + script 
    + ", country=" + region
    + ", variants=" + variants
    + ", keywords=" + extensions
    + "}";
  }
}