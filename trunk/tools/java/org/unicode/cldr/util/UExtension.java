package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.Relation;

public class UExtension {
  static SupplementalDataInfo data = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);

  static Pattern SEP = Pattern.compile("[-_]");
  static Pattern SPACE = Pattern.compile("\\s");
  static Pattern ALPHANUM = Pattern.compile("[0-9A-Za-z]{2,8}");
  static Pattern CODEPOINTS = Pattern.compile("(10|[0-9A-Fa-f])?[0-9A-Fa-f]{4}(\\s(10|[0-9A-Fa-f])?[0-9A-Fa-f]{4})*");
  static Relation<String,String> validKeyTypes = data.getBcp47Keys();

  private boolean validating = false;
  private SortedMap<String,List<String>> keyTypes = new TreeMap<String,List<String>>();
  private Set<String> attributes = new TreeSet<String>();

  public Set<String> getKeys() {
    return keyTypes.keySet();
  }

  public List<String> getTypes(String key) {
    return keyTypes.get(key);
  }

  public Set<String> getAttributes() {
    return attributes;
  }

  public boolean isValidating() {
    return validating;
  }

  public UExtension setValidating(boolean validating) {
    this.validating = validating;
    return this;
  }

  /**
   * Parses the subtags after the -u-
   * @param source
   * @return
   */
  public UExtension parse(String source) {
    // the subtags that are up to the first two letter are attributes
    String key = null;
    List<String> list = null;
    Set<String> validSubtypes = null;
    Matcher alphanum = ALPHANUM.matcher("");

    for (String subtag : SEP.split(source)) {
      if (!alphanum.reset(subtag).matches()) {
        throw new IllegalArgumentException("Invalid subtag contents, must be [0-9 A-Z a-z]{2,8}: " + subtag);
      }
      subtag = subtag.toLowerCase(Locale.ENGLISH); // normalize
      if (subtag.length() == 2) { // key
        if (list != null) { // check size of previous list 
          if (list.size() == 0  || !key.equals("vt") && list.size() > 1) {
            throw new IllegalArgumentException("Illegal number of subtypes for: " + key + "\t" + list); 
          }
        }
        key = subtag;
        if (validating) {
          validSubtypes = validKeyTypes.getAll(key);
          if (validSubtypes == null) {
            throw new IllegalArgumentException("Invalid key: " + key); 
          }
        }
        list = keyTypes.get(key);
        if (list != null) {
          throw new IllegalArgumentException("Multiple keys with same value: " + subtag);
        }
        list = new ArrayList<String>();
        keyTypes.put(key, list);
      } else { // add subtype
        if (key == null) {
          if (validating) {
            throw new IllegalArgumentException("No attributes currently valid: " + subtag);
          }
          attributes.add(subtag);
          break;
        }
        if (validating) {
          if (key.equals("vt")) {
            if (!CODEPOINTS.matcher(subtag).matches()) {
              throw new IllegalArgumentException("Illegal subtypes: " + key + "-" + subtag); 
            }
          } else if (!validSubtypes.contains(subtag)) {
            throw new IllegalArgumentException("Illegal subtypes: " + key + "-" + subtag); 
          }
        }
        list.add(subtag);
      }
    }
    // protect
    attributes = Collections.unmodifiableSet(attributes);
    for (String key2 : keyTypes.keySet()) {
      list = keyTypes.get(key2);
      keyTypes.put(key2, Collections.unmodifiableList(list));
    }
    keyTypes = Collections.unmodifiableSortedMap(keyTypes);
    return this;
  }
  
  public String toString() {
    return "{attributes=" + attributes + ", keyTypes=" + keyTypes + "}";
  }
}
