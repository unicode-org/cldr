/**
 * 
 */
package org.unicode.cldr.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeFilter;

public class CLDRTransforms {

  private static Map<String, CLDRTransforms> cache = new HashMap();

  /**
   * 
   * @param showProgress null if no progress needed
   * @param filter TODO
   * @return
   */

  public static CLDRTransforms getinstance(Appendable showProgress, String filter) {
    synchronized (cache) {
      CLDRTransforms instance = cache.get(filter);
      if (instance == null) {
        instance = new CLDRTransforms(showProgress, filter);
        if (instance != null) {
          cache.put(filter, instance);
        }
      }
      return instance;
    }
  }


  Map id_instance = new HashMap();

  static Transliterator fixup = Transliterator
  .getInstance("[:Mn:]any-hex/java");

  Set available = new HashSet();
  String[] doFirst = {"Latin-ConjoiningJamo"};
  Relation<Matcher,String> dependsOn = new Relation(new LinkedHashMap(), LinkedHashSet.class);
  {
    addDependency(".*(Jamo|Hangul).*", "Latin-ConjoiningJamo");
    addDependency(".*Bengali.*", "Bengali-InterIndic", "InterIndic-Bengali");
    addDependency(".*Devanagari.*", "Devanagari-InterIndic", "Devanagari-Tamil");
    addDependency(".*Gujarati.*", "Gujarati-InterIndic", "Gujarati-Tamil");
    addDependency(".*Gurmukhi.*", "Gurmukhi-InterIndic", "Gurmukhi-Tamil");
    addDependency(".*Kannada.*", "Kannada-InterIndic", "Kannada-Tamil");
    addDependency(".*Malayalam.*", "Malayalam-InterIndic", "Malayalam-Tamil");
    addDependency(".*Oriya.*", "Oriya-InterIndic", "Oriya-Tamil");
    addDependency(".*Tamil.*", "Tamil-InterIndic", "InterIndic-Tamil");
    addDependency(".*Telugu.*", "Telugu-InterIndic", "Telugu-Tamil");
    addDependency(".*Tamil.*", "Tamil-InterIndic", "InterIndic-Tamil");
  }

  Factory cldrFactory = CLDRFile.Factory.make(
          Utility.COMMON_DIRECTORY + File.separatorChar +  "transforms/", ".*");

  private CLDRTransforms(Appendable showProgress, String filterString)  {        
    // reorder to preload some
    this.showProgress = showProgress;
    Set<String> ordered = new LinkedHashSet<String>();

    Matcher filter = filterString == null ? null : Pattern.compile(filterString).matcher("");

    //ordered.addAll(Arrays.asList(doFirst));
    for (String item : cldrFactory.getAvailable()) {
      if (filter != null && !filter.reset(item).matches()) {
        System.out.println("Skipping " + item);
        continue;
      }
      // add dependencies first
      for (Matcher m : dependsOn.keySet()) {
        if (m.reset(item).matches()) {
          ordered.addAll(dependsOn.getAll(m));
        }
      }
      ordered.add(item);
    }
    append("Adding: " + ordered + "\n");

    for (Iterator it = ordered.iterator(); it.hasNext();) {
      String cldrFileName = (String) it.next();
      //      if (cldrFileName.contains("Ethiopic") || cldrFileName.contains("Aboriginal")) {
      //        System.out.println("Skipping Ethiopic");
      //        //Transliterator.DEBUG = true;
      //        //continue;
      //      } else {
      //        //Transliterator.DEBUG = false;
      //      }

      CLDRFile file = cldrFactory.make(cldrFileName, false);
      cache(file);
    }
    available = Collections.unmodifiableSet(id_instance.keySet());
  }

  private void addDependency(String pattern, String... whatItDependsOn) {
    dependsOn.putAll(Pattern.compile(pattern).matcher(""), Arrays.asList(whatItDependsOn));
  }

  public  Set getAvailableTransforms() {
    return available;
  }

  public  Transliterator getInstance(String id) {
    Transliterator result = (Transliterator) id_instance.get(id);
    if (result == null) {
      throw new IllegalArgumentException("No transform for " + id);
    }
    return result;
  }

  public static Pattern TRANSFORM_ID_PATTERN = Pattern.compile("(.+)-([^/]+)(/(.*))?");

  public  Transliterator getReverseInstance(String id) {
    Matcher matcher = TRANSFORM_ID_PATTERN.matcher(id);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("**No transform for " + id);
    }
    return getInstance(matcher.group(2) + "-" + matcher.group(1) + (matcher.group(4) == null ? "" : "/" + matcher.group(4)));
  }

  private  void cache(CLDRFile cldrFile) {
    boolean first = true;
    StringBuffer rules = new StringBuffer();
    XPathParts parts = new XPathParts();
    String source = null;
    String target = null;
    String variant = null;
    String direction = null;

    for (Iterator it = cldrFile.iterator("", CLDRFile.ldmlComparator); it
    .hasNext();) {
      String path = (String) it.next();
      String value = cldrFile.getStringValue(path);
      if (first) {
        parts.set(path);
        Map attributes = parts.findAttributes("transform");
        if (attributes == null)
          return; // error, not a transform file
        source = (String) attributes.get("source");
        target = (String) attributes.get("target");
        variant = (String) attributes.get("variant");
        direction = (String) attributes.get("direction");
        first = false;
      }
      if (path.indexOf("/comment") >= 0) {
        // skip
      } else if (path.indexOf("/tRule") >= 0) {
        // value = replaceUnquoted(value,"\u00A7", "&");
        // no longer need to replace arrows, ICU now handles the 2190/2192/2194 arrows
        //value = value.replace('\u2192', '>');
        //value = value.replace('\u2190', '<');
        //value = value.replaceAll("\u2194", "<>");
        value = fixup.transliterate(value);
        rules.append(value).append(Utility.LINE_SEPARATOR);
      } else {
        throw new IllegalArgumentException("Unknown element: "
                + path + "\t " + value);
      }
    }
    String ruleString = rules.toString();
    if (direction.equals("both") || direction.equals("forward")) {
      String id = source + "-" + target + (variant == null ? "" : "/" + variant);
      internalRegister(id, ruleString, Transliterator.FORWARD);
    }
    if (direction.equals("both") || direction.equals("backward")) {
      String id = target + "-" + source + (variant == null ? "" : "/" + variant);
      internalRegister(id, ruleString, Transliterator.REVERSE);
    }
    if (source.contains("alf") || target.contains("alf")) { // debugging
        CLDRTransforms.verifyNullFilter("halfwidth-fullwidth");
    }
  }

  private  void internalRegister(String id, String ruleString, int direction) {
    try {
      Transliterator t = Transliterator.createFromRules(id, ruleString, direction);
      id_instance.put(id, t);
      Transliterator.unregister(id);
      Transliterator.registerInstance(t);
      verifyNullFilter("halfwidth-fullwidth");
      if (showProgress != null) {
        append("Registered new Transliterator: " + id + '\n');
      }
    } catch (RuntimeException e) {
      if (showProgress != null) {
        e.printStackTrace();
        append("Couldn't register new Transliterator: " + id + "\t" + e.getMessage() + '\n');
      } else {
        throw (IllegalArgumentException) new IllegalArgumentException("Couldn't register new Transliterator: " + id).initCause(e);
      }
    }
  }

  Appendable showProgress;

  private void append(String string) {
    try {
      showProgress.append(string);
    } catch (IOException e) {
      throw (RuntimeException) new IllegalArgumentException().initCause(e);
    }
  }

  public static void registerFromIcuFormatFiles(String target) throws IOException {
    Matcher getId = Pattern.compile("\\s*(\\S*)\\s*\\{\\s*").matcher("");
    Matcher getSource = Pattern.compile("\\s*(\\S*)\\s*\\{\\s*\\\"(.*)\\\".*").matcher("");
    Matcher translitID = Pattern.compile("([^-]+)-([^/]+)+(?:[/](.+))?").matcher("");

    Map fixedIDs = new TreeMap();
    Set oddIDs = new TreeSet();

    File dir = new File(target);
    // get the list of files to take, and their directions
    BufferedReader input = BagFormatter.openUTF8Reader(target, "root.txt");
    String id = null;
    String filename = null;
    String lastId = null;
    String lastFilename = null;
    Map aliasMap = new LinkedHashMap();

    deregisterIcuTransliterators();

    // do first, since others depend on these
    /**
     * Special aliases. 
     * Tone-Digit {
            alias {"Pinyin-NumericPinyin"}
        }
        Digit-Tone {
            alias {"NumericPinyin-Pinyin"}
        }
     */
    registerTransliteratorFromFile("Latin-ConjoiningJamo", target, null);
    registerTransliteratorFromFile("Pinyin-NumericPinyin", target, null);
    Transliterator.registerAlias("Tone-Digit", "Pinyin-NumericPinyin");
    Transliterator.registerAlias("Digit-Tone", "NumericPinyin-Pinyin");
    registerTransliteratorFromFile("Fullwidth-Halfwidth", target, null);
    registerTransliteratorFromFile("Hiragana_Katakana", target, null);
    registerTransliteratorFromFile("Latin-Katakana", target, null);

    String fileMatcherString = Utility.getProperty("file", ".*");
    Matcher fileMatcher = Pattern.compile(fileMatcherString).matcher("");

    while (true) {
      String line = input.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.startsWith("\uFEFF")) {
        line = line.substring(1);
      }
      if (line.startsWith("TransliteratorNamePattern")) break; // done
      //			if (line.indexOf("Ethiopic") >= 0) {
      //				System.out.println("Skipping Ethiopic");
      //				continue;
      //			}
      if (getId.reset(line).matches()) {
        String temp = getId.group(1);
        if (!temp.equals("file") && !temp.equals("internal")) id = temp;
        continue;
      }
      if (getSource.reset(line).matches()) {
        String operation = getSource.group(1);
        String source = getSource.group(2);
        if (operation.equals("alias")) {
          aliasMap.put(id, source);
          checkIdFix(id, fixedIDs, oddIDs, translitID);
          lastId = id;
          id = null;
        } else if (operation.equals("resource:process(transliterator)")) {
          filename = source;
        } else if (operation.equals("direction")) {
          try {
            if (id == null || filename == null) {
              System.out.println("skipping: " + line);
              continue;
            }
            if (filename.indexOf("InterIndic") >= 0 && filename.indexOf("Latin") >= 0) {
              System.out.print("**" + id);
            }
            checkIdFix(id, fixedIDs, oddIDs, translitID);
            if (source.equals("FORWARD")) {
              Utility.registerTransliteratorFromFile(id, target, filename, Transliterator.FORWARD, false);
            } else {
              Utility.registerTransliteratorFromFile(id, target, filename, Transliterator.REVERSE, false);
            }
            verifyNullFilter("halfwidth-fullwidth");

            lastId = id;
            id = null;
            lastFilename = filename;
            filename = null;
          } catch (RuntimeException e) {
            throw (RuntimeException) new IllegalArgumentException("Failed with " + filename + ", " + source).initCause(e);
          }
        } else {
          System.out.println(dir + "root.txt unhandled line:" + line);
        }
        continue;
      }
      String trimmed = line.trim();
      if (trimmed.equals("")) continue;
      if (trimmed.equals("}")) continue;
      if (trimmed.startsWith("//")) continue;
      System.out.println("Unhandled:" + line);
    }
    for (java.util.Iterator it = aliasMap.keySet().iterator(); it.hasNext();) {
      id = (String)it.next();
      String source = (String) aliasMap.get(id);
      Transliterator.unregister(id);
      Transliterator t = Transliterator.createFromRules(id, "::" + source + ";", Transliterator.FORWARD);
      Transliterator.registerInstance(t);
      verifyNullFilter("halfwidth-fullwidth");
      System.out.println("Registered new Transliterator Alias: " + id);

    }
    System.out.println("Fixed IDs");
    for (Iterator it = fixedIDs.keySet().iterator(); it.hasNext();) {
      String id2 = (String) it.next();
      System.out.println("\t" + id2 + "\t" + fixedIDs.get(id2));
    }
    System.out.println("Odd IDs");
    for (Iterator it = oddIDs.iterator(); it.hasNext();) {
      String id2 = (String) it.next();
      System.out.println("\t" + id2);
    }
    Transliterator.registerAny(); // do this last!
  }

  private static void registerTransliteratorFromFile(String string, String target, String object) {
    Utility.registerTransliteratorFromFile(string, target, object, Transliterator.FORWARD, true);
    verifyNullFilter("halfwidth-fullwidth");
    Utility.registerTransliteratorFromFile(string, target, object, Transliterator.REVERSE, true);
    verifyNullFilter("halfwidth-fullwidth");
  }

  public static void checkIdFix(String id, Map fixedIDs, Set oddIDs, Matcher translitID) {
    if (fixedIDs.containsKey(id)) return;
    if (!translitID.reset(id).matches()) {
      System.out.println("Can't fix: " + id);
      fixedIDs.put(id, "?"+id);
      return;
    }
    String source1 = translitID.group(1);
    String target1 = translitID.group(2);
    String variant = translitID.group(3);
    String source = fixID(source1);
    String target = fixID(target1);
    fixedIDs.put(source1, source);
    fixedIDs.put(target1, target);
    if (variant != null) oddIDs.add("variant: " + variant);
  }

  static String fixID(String source) {
    return source; // for now
  }

  public static void deregisterIcuTransliterators() {
    // Remove all of the current registrations
    // first load into array, so we don't get sync problems.
    List<String> rawAvailable = new ArrayList<String>();
    for (Enumeration en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
      rawAvailable.add((String)en.nextElement());
    }

    List<String> available = getDependentOrder(rawAvailable);
    available.retainAll(rawAvailable); // remove the items we won't touch anyway
    rawAvailable.removeAll(available); // now the ones whose order doesn't matter
    removeTransliterators(rawAvailable);
    removeTransliterators(available);

    for (Enumeration en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
      String oldId = (String)en.nextElement();
      System.out.println("Retaining: " + oldId);
    }
  }


  public static List<String> getDependentOrder(Collection<String> available) {
    MergeLists<String> mergeLists = new MergeLists<String>(new TreeSet(new UTF16.StringComparator(true, false, 0)));
    // We can't determine these from looking at the dependency lists, since they are used in the rules.
    mergeLists.add("Latin-NumericPinyin", "Tone-Digit", "Pinyin-NumericPinyin");
    mergeLists.add("NumericPinyin-Latin", "Digit-Tone", "NumericPinyin-Pinyin");
    mergeLists.add("Han-Latin", "Fullwidth-Halfwidth");
    mergeLists.add("Hiragana-Latin", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    mergeLists.add("Katakana-Latin", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    mergeLists.add("Latin-Hiragana", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    mergeLists.add("Latin-Katakana", "Halfwidth-Fullwidth", "Fullwidth-Halfwidth");
    for (String oldId : available) {
      Transliterator t = Transliterator.getInstance(oldId);
      addDependingOn(mergeLists, oldId, t);
    }
    return mergeLists.merge();
  }

  private static Set<String> SKIP_DEPENDENCIES = new HashSet<String>();
  static {
    SKIP_DEPENDENCIES.add("%Pass1");
    SKIP_DEPENDENCIES.add("NFC(NFD)");
    SKIP_DEPENDENCIES.add("NFD(NFC)");
    SKIP_DEPENDENCIES.add("NFD");
    SKIP_DEPENDENCIES.add("NFC");
  }
  private static void addDependingOn(MergeLists<String> mergeLists, String oldId, Transliterator t) {
    Transliterator[] elements = t.getElements();
    for (Transliterator s : elements) {
      final String id = s.getID();
      if (id.equals(oldId) || SKIP_DEPENDENCIES.contains(id)) {
        continue;
      }
      mergeLists.add(oldId, id);
      addDependingOn(mergeLists, id, s);
    }
  }

  public static void removeTransliterators(Collection<String> available) {
    for (String oldId : available) {
      Transliterator t;
      try {
        t = Transliterator.getInstance(oldId);
      } catch (Exception e) {
        System.out.println("Skipping: " + oldId);
        t = Transliterator.getInstance(oldId);
        continue;
      }
      String className = t.getClass().getName();
      if (className.endsWith(".CompoundTransliterator")
              || className.endsWith(".RuleBasedTransliterator")
              || className.endsWith(".AnyTransliterator")) {
        System.out.println("REMOVING: " + oldId);
        Transliterator.unregister(oldId);
      } else {
        System.out.println("Retaining: " + oldId + "\t\t" + className);
      }
    }
  }

  public static class ParsedTransformID {
    public String source;
    public String target;
    public String variant;
    public ParsedTransformID set(String id) {
      variant = null;
      int pos = id.indexOf('-');
      if (pos < 0) {
        source = "Any";
        target = id;
        return this;
      }
      source = id.substring(0,pos);
      int pos2 = id.indexOf('/', pos);
      if (pos2 < 0) {
        target = id.substring(pos+1);
        return this;
      }
      target = id.substring(pos+1, pos2);
      variant = id.substring(pos2+1);
      return this;
    }
    public ParsedTransformID reverse() {
      String temp = source;
      source = target;
      target = temp;
      return this;
    }
    public String getTargetVariant() {
      return target + (variant == null ? "" : "/" + variant);
    }
    public String getSourceVariant() {
      return source + (variant == null ? "" : "/" + variant);
    }
    public String toString() {
      return source + "-" + getTargetVariant();
    }
    public static String getId(String source, String target, String variant) {
      String id = source + '-' + target;
      if (variant != null) id += "/" + variant;
      return id;
    }
  }

  /**
   * Verify that if the transliterator exists, it has a null filter
   * @param id
   */
  public static void verifyNullFilter(String id) {
    Transliterator widen;
    try {
      widen = Transliterator.getInstance(id);
    } catch (Exception e) {
      return;
    }
    UnicodeFilter filter = widen.getFilter();
    if (filter != null) {
      throw new IllegalArgumentException(id + " has non-empty filter: " + filter);
    }
  }
}
