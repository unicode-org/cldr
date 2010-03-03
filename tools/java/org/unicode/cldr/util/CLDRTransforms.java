/**
 * 
 */
package org.unicode.cldr.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeFilter;

public class CLDRTransforms {

  public static final String TRANSFORM_DIR = (CldrUtility.COMMON_DIRECTORY + "transforms/");

  static final CLDRTransforms SINGLETON = new CLDRTransforms();

  public static CLDRTransforms getInstance() {
    return SINGLETON;
  }

  public Appendable getShowProgress() {
    return showProgress;
  }

  public CLDRTransforms setShowProgress(Appendable showProgress) {
    this.showProgress = showProgress;
    return this;
  }

  final Set<String> overridden = new HashSet<String>();
  final DependencyOrder dependencyOrder = new DependencyOrder();

  /**
   * 
   * @param dir TODO
   * @param namesMatchingRegex TODO
   * @param showProgress null if no progress needed
   * @return
   */

  public static void registerCldrTransforms(String dir, String namesMatchingRegex, Appendable showProgress) {
    CLDRTransforms r = getInstance();
    if (dir == null) {
      dir = TRANSFORM_DIR;
    }
    // reorder to preload some
    r.showProgress = showProgress;
    Matcher filter = namesMatchingRegex == null ? null : Pattern.compile(namesMatchingRegex).matcher("");
    r.deregisterIcuTransliterators(filter);

    final List<String> files = Arrays.asList(new File(TRANSFORM_DIR).list());
    Set<String> ordered = r.dependencyOrder.getOrderedItems(files, filter, true);

    for (String cldrFileName : ordered) {
      r.registerTransliteratorsFromXML(dir, cldrFileName, files);
    }
    Transliterator.registerAny(); // do this last!
  }



  public Set<String> getOverriddenTransliterators() {
    return Collections.unmodifiableSet(overridden);
  }

  static Transliterator fixup = Transliterator.getInstance("[:Mn:]any-hex/java");

  class DependencyOrder {
    //String[] doFirst = {"Latin-ConjoiningJamo"};
    // the following are file names, not IDs, so the dependencies have to go both directions
    Relation<Matcher,String> dependsOn = new Relation(new LinkedHashMap(), LinkedHashSet.class);
    {
      addDependency("Latin-(Jamo|Hangul)(/.*)?", "Latin-ConjoiningJamo", "ConjoiningJamo-Latin");
      addDependency("(Jamo|Hangul)-Latin(/.*)?", "Latin-ConjoiningJamo", "ConjoiningJamo-Latin");
      addDependency("Latin-Han(/.*)", "Han-Spacedhan");
      addDependency(".*(Hiragana|Katakana|Han|han).*", "Fullwidth-Halfwidth", "Halfwidth-Fullwidth");
      addDependency(".*(Hiragana).*", "Latin-Katakana", "Katakana-Latin");
      addPivotDependency("Bengali", "InterIndic");
      addPivotDependency("Devanagari", "InterIndic");
      addPivotDependency("Gujarati", "InterIndic");
      addPivotDependency("Gurmukhi", "InterIndic");
      addPivotDependency("Kannada", "InterIndic");
      addPivotDependency("Malayalam", "InterIndic");
      addPivotDependency("Oriya", "InterIndic");
      addPivotDependency("Tamil", "InterIndic");
      addPivotDependency("Telugu", "InterIndic");
      addPivotDependency("Tamil", "InterIndic");
      addPivotDependency("Tamil", "InterIndic");
      addPivotDependency("Tamil", "InterIndic");
      addDependency(".*Digit.*", "NumericPinyin-Pinyin", "Pinyin-NumericPinyin");
      addDependency("Latin-NumericPinyin(/.*)?", "Tone-Digit", "Digit-Tone");
      addDependency("NumericPinyin-Latin(/.*)?", "Tone-Digit", "Digit-Tone");

      //Pinyin-NumericPinyin.xml
    }

    private void addPivotDependency(String script, String pivot) {
      addDependency(script + "-.*", "Bengali" + "-" + pivot, pivot + "-" + "Bengali");
      addDependency(".*-" + "Bengali" + "(/.*)?", pivot + "-" + "Bengali", pivot + "-" + "Bengali");
    }

    private void addDependency(String pattern, String... whatItDependsOn) {
      dependsOn.putAll(Pattern.compile(pattern).matcher(""), Arrays.asList(whatItDependsOn));
    }

    public Set<String> getOrderedItems(Collection<String> input, Matcher filter, boolean hasXmlSuffix) {
      Set<String> ordered = new LinkedHashSet<String>();

      //      for (String other : doFirst) {
      //        ordered.add(hasXmlSuffix ? other + ".xml" : other);
      //      }

      for (String cldrFileName : input) {
        if (hasXmlSuffix && !cldrFileName.endsWith(".xml")) {
          continue;
        }

        if (filter != null && !filter.reset(cldrFileName).matches()) {
          append("Skipping " + cldrFileName + "\n");
          continue;
        }
        // add dependencies first
        addDependenciesRecursively(cldrFileName, ordered, hasXmlSuffix);
      }
      append("Adding: " + ordered + "\n");
      return ordered;
    }

    private void addDependenciesRecursively(String cldrFileName, Set<String> ordered, boolean hasXmlSuffix) {
      String item = hasXmlSuffix && cldrFileName.endsWith(".xml") ? cldrFileName.substring(0,cldrFileName.length()-4) : cldrFileName;
      for (Matcher m : dependsOn.keySet()) {
        if (m.reset(item).matches()) {
          for (String other : dependsOn.getAll(m)) {
            final String toAdd = hasXmlSuffix ? other + ".xml" : other;
            if (other.equals(item) || ordered.contains(toAdd)) {
              continue;
            }
            addDependenciesRecursively(toAdd, ordered, hasXmlSuffix);
            append("Dependency: Adding: " + toAdd + " before " + item + "\n");
          }
        }
      }
      ordered.add(item);
    }

  }

  public Transliterator getInstance(String id) {
    if (!overridden.contains(id)) {
      throw new IllegalArgumentException("No overriden transform for " + id);
    }
    return Transliterator.getInstance(id);
  }

  public static Pattern TRANSFORM_ID_PATTERN = Pattern.compile("(.+)-([^/]+)(/(.*))?");

  public  Transliterator getReverseInstance(String id) {
    Matcher matcher = TRANSFORM_ID_PATTERN.matcher(id);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("**No transform for " + id);
    }
    return getInstance(matcher.group(2) + "-" + matcher.group(1) + (matcher.group(4) == null ? "" : "/" + matcher.group(4)));
  }

  public void registerTransliteratorsFromXML(String dir, String cldrFileName, List<String> cantSkip) {
    ParsedTransformID directionInfo = new ParsedTransformID();
    String ruleString;
    final String cldrFileName2 = cldrFileName + ".xml";
    try {
      ruleString = getIcuRulesFromXmlFile(dir, cldrFileName2, directionInfo);
    } catch (RuntimeException e) {
      if (!cantSkip.contains(cldrFileName2)) {
        return;
      }
      throw e;
    }
    if (directionInfo.getDirection() == Direction.both || directionInfo.getDirection() == Direction.forward) {
      internalRegister(directionInfo.getId(), ruleString, Transliterator.FORWARD);
    }
    if (directionInfo.getDirection() == Direction.both || directionInfo.getDirection() == Direction.backward) {
      internalRegister(directionInfo.getId(), ruleString, Transliterator.REVERSE);
    }
  }

  /**
   * Return Icu rules, and the direction info
   * @param dir TODO
   * @param cldrFileName
   * @param directionInfo
   * @return
   */
  public String getIcuRulesFromXmlFile(String dir, String cldrFileName, ParsedTransformID directionInfo) {
    final MyHandler myHandler = new MyHandler(cldrFileName, directionInfo);
    XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
    xfr.read(dir + cldrFileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, true);
    return myHandler.getRules();
  }

  private  void internalRegister(String id, String ruleString, int direction) {
    if (direction == Transliterator.REVERSE) {
      id = ParsedTransformID.reverse(id);
    }
    internalRegisterNoReverseId(id, ruleString, direction);
  }

  private  void internalRegisterNoReverseId(String id, String ruleString, int direction) {
    try {
      Transliterator t = Transliterator.createFromRules(id, ruleString, direction);
      overridden.add(id);
      Transliterator.unregister(id);
      Transliterator.registerInstance(t);
      if (false) { // for paranoid testing
        Transliterator t1 = Transliterator.createFromRules(id, ruleString, direction);
        String r1 = t1.toRules(false);
        Transliterator t2 = Transliterator.getInstance(id);
        String r2 = t2.toRules(false);
        if (!r1.equals(r2)) {
          throw (IllegalArgumentException) new IllegalArgumentException("Rules unequal" + ruleString + "$$$\n$$$" + r2);
        }
      }
      //verifyNullFilter("halfwidth-fullwidth");
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
      if (showProgress == null) {
        return;
      }
      showProgress.append(string);
      if (showProgress instanceof Writer) {
        ((Writer) showProgress).flush();
      }
    } catch (IOException e) {
      throw (RuntimeException) new IllegalArgumentException().initCause(e);
    }
  }
  
  private void appendln(String s) {
    append(s + "\n");
  }

  // ===================================

  @SuppressWarnings("deprecation")
  public void registerFromIcuFormatFiles(String directory) throws IOException {

    deregisterIcuTransliterators((Matcher)null);

    Matcher getId = Pattern.compile("\\s*(\\S*)\\s*\\{\\s*").matcher("");
    Matcher getSource = Pattern.compile("\\s*(\\S*)\\s*\\{\\s*\\\"(.*)\\\".*").matcher("");
    Matcher translitID = Pattern.compile("([^-]+)-([^/]+)+(?:[/](.+))?").matcher("");

    Map<String, String> fixedIDs = new TreeMap<String, String>();
    Set<String> oddIDs = new TreeSet<String>();

    File dir = new File(directory);
    // get the list of files to take, and their directions
    BufferedReader input = BagFormatter.openUTF8Reader(directory, "root.txt");
    String id = null;
    String filename = null;
    Map<String, String> aliasMap = new LinkedHashMap<String, String>();

    //    deregisterIcuTransliterators();

    // do first, since others depend on theseregisterFromIcuFile
    /**
     * Special aliases. 
     * Tone-Digit {
            alias {"Pinyin-NumericPinyin"}
        }
        Digit-Tone {
            alias {"NumericPinyin-Pinyin"}
        }
     */
    //    registerFromIcuFile("Latin-ConjoiningJamo", directory, null);
    //    registerFromIcuFile("Pinyin-NumericPinyin", directory, null);
    //    Transliterator.registerAlias("Tone-Digit", "Pinyin-NumericPinyin");
    //    Transliterator.registerAlias("Digit-Tone", "NumericPinyin-Pinyin");
    //    registerFromIcuFile("Fullwidth-Halfwidth", directory, null);
    //    registerFromIcuFile("Hiragana-Katakana", directory, null);
    //    registerFromIcuFile("Latin-Katakana", directory, null);
    //    registerFromIcuFile("Hiragana-Latin", directory, null);

    while (true) {
      String line = input.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.startsWith("\uFEFF")) {
        line = line.substring(1);
      }
      if (line.startsWith("TransliteratorNamePattern")) break; // done
      //			if (line.indexOf("Ethiopic") >= 0) {
      //				appendln("Skipping Ethiopic");
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
          id = null;
        } else if (operation.equals("resource:process(transliterator)")) {
          filename = source;
        } else if (operation.equals("direction")) {
          try {
            if (id == null || filename == null) {
              //appendln("skipping: " + line);
              continue;
            }
            if (filename.indexOf("InterIndic") >= 0 && filename.indexOf("Latin") >= 0) {
              //append("**" + id);
            }
            checkIdFix(id, fixedIDs, oddIDs, translitID);

            final int direction = source.equals("FORWARD") ? Transliterator.FORWARD : Transliterator.REVERSE;
            registerFromIcuFile(id, directory, filename, direction);

            verifyNullFilter("halfwidth-fullwidth");

            id = null;
            filename = null;
          } catch (RuntimeException e) {
            throw (RuntimeException) new IllegalArgumentException("Failed with " + filename + ", " + source).initCause(e);
          }
        } else {
          append(dir + "root.txt unhandled line:" + line);
        }
        continue;
      }
      String trimmed = line.trim();
      if (trimmed.equals("")) continue;
      if (trimmed.equals("}")) continue;
      if (trimmed.startsWith("//")) continue;
      throw new IllegalArgumentException("Unhandled:" + line);
    }

    final Set<String> rawIds = idToRules.keySet();
    Set<String> ordered = dependencyOrder.getOrderedItems(rawIds, null, false);
    ordered.retainAll(rawIds); // since we are in ID space, kick out anything that isn't

    for (String id2 : ordered) {
      RuleDirection stuff = idToRules.get(id2);
      internalRegisterNoReverseId(id2, stuff.ruleString, stuff.direction);
      verifyNullFilter("halfwidth-fullwidth"); // TESTING
    }

    for (Iterator<String> it = aliasMap.keySet().iterator(); it.hasNext();) {
      id = (String)it.next();
      String source = (String) aliasMap.get(id);
      Transliterator.unregister(id);
      Transliterator t = Transliterator.createFromRules(id, "::" + source + ";", Transliterator.FORWARD);
      Transliterator.registerInstance(t);
      //verifyNullFilter("halfwidth-fullwidth");
      appendln("Registered new Transliterator Alias: " + id);

    }
    appendln("Fixed IDs");
    for (Iterator<String> it = fixedIDs.keySet().iterator(); it.hasNext();) {
      String id2 = (String) it.next();
      appendln("\t" + id2 + "\t" + fixedIDs.get(id2));
    }
    appendln("Odd IDs");
    for (Iterator<String> it = oddIDs.iterator(); it.hasNext();) {
      String id2 = (String) it.next();
      appendln("\t" + id2);
    }
    Transliterator.registerAny(); // do this last!
  }

  Map<String,RuleDirection> idToRules = new TreeMap<String, RuleDirection>();

  private class RuleDirection {
    String ruleString;
    int direction;
    public RuleDirection(String ruleString, int direction) {
      super();
      this.ruleString = ruleString;
      this.direction = direction;
    }
  }

  private void registerFromIcuFile(String id, String directory, String filename, int direction) {
    if (filename == null) {
      filename = id.replace("-", "_").replace("/", "_") + ".txt";
    }
    String ruleString = CldrUtility.getText(directory, filename);
    idToRules.put(id, new RuleDirection(ruleString, direction));
  }

  //  private void registerFromIcuFile(String id, String dir, String filename) {
  //    registerFromIcuFile(id, dir, filename, Transliterator.FORWARD);
  //    registerFromIcuFile(id, dir, filename, Transliterator.REVERSE);
  //  }

  public void checkIdFix(String id, Map<String,String> fixedIDs, Set<String> oddIDs, Matcher translitID) {
    if (fixedIDs.containsKey(id)) return;
    if (!translitID.reset(id).matches()) {
      appendln("Can't fix: " + id);
      fixedIDs.put(id, "?"+id);
      return;
    }
    String source1 = translitID.group(1);
    String target1 = translitID.group(2);
    String variant = translitID.group(3);
    String source = fixID(source1);
    String target = fixID(target1);
    if (!source1.equals(source)) {
      fixedIDs.put(source1, source);
    }
    if (!target1.equals(target)) {
      fixedIDs.put(target1, target);
    }
    if (variant != null) {
      oddIDs.add("variant: " + variant);
    }
  }

  static String fixID(String source) {
    return source; // for now
  }

  public void deregisterIcuTransliterators(Matcher filter) {
    // Remove all of the current registrations
    // first load into array, so we don't get sync problems.
    List<String> rawAvailable = new ArrayList<String>();
    for (Enumeration en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
      final String id = (String)en.nextElement();
      if (filter != null && !filter.reset(id).matches()) {
        continue;
      }
      rawAvailable.add(id);
    }

    //deregisterIcuTransliterators(rawAvailable);

    Set<String> available = dependencyOrder.getOrderedItems(rawAvailable, filter, false);
    List reversed = new LinkedList();
    for (String item : available) {
      reversed.add(0, item);
    }
    //      available.retainAll(rawAvailable); // remove the items we won't touch anyway
    //      rawAvailable.removeAll(available); // now the ones whose order doesn't matter
    //      deregisterIcuTransliterators(rawAvailable);
    deregisterIcuTransliterators(reversed);

    for (Enumeration en = Transliterator.getAvailableIDs(); en.hasMoreElements();) {
      String oldId = (String)en.nextElement();
      append("Retaining: " + oldId);
    }
  }

  public void deregisterIcuTransliterators(Collection<String> available) {
    for (String oldId : available) {
      Transliterator t;
      try {
        t = Transliterator.getInstance(oldId);
      } catch (RuntimeException e) {
        append("Failure with: " + oldId);
        t = Transliterator.getInstance(oldId);
        throw e;
      }
      String className = t.getClass().getName();
      if (className.endsWith(".CompoundTransliterator")
              || className.endsWith(".RuleBasedTransliterator")
              || className.endsWith(".AnyTransliterator")) {
        appendln("REMOVING: " + oldId);
        Transliterator.unregister(oldId);
      } else {
        appendln("Retaining: " + oldId + "\t\t" + className);
      }
    }
  }

  public enum Direction {backward, both, forward}

  public static class ParsedTransformID {
    public String source = "Any";
    public String target = "Any";
    public String variant;
    protected Direction direction = null;

    public String getId() {
      return getSource() + "-" + getTarget() + (getVariant() == null ? "" : "/" + getVariant());
    }
    public String getBackwardId() {
      return getTarget() + "-" + getSource() + (getVariant() == null ? "" : "/" + getVariant());
    }

    public ParsedTransformID() {}

    public ParsedTransformID set(String source, String target, String variant, Direction direction) {
      this.source = source;
      this.target = target;
      this.variant = variant;
      this.direction = direction;
      return this;
    }

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
    protected void setDirection(Direction direction) {
      this.direction = direction;
    }

    protected Direction getDirection() {
      return direction;
    }

    protected void setVariant(String variant) {
      this.variant = variant;
    }

    protected String getVariant() {
      return variant;
    }

    protected void setTarget(String target) {
      this.target = target;
    }

    protected String getTarget() {
      return target;
    }

    protected void setSource(String source) {
      this.source = source;
    }

    protected String getSource() {
      return source;
    }

    public String toString() {
      return source + "-" + getTargetVariant();
    }
    public static String getId(String source, String target, String variant) {
      String id = source + '-' + target;
      if (variant != null) id += "/" + variant;
      return id;
    }
    public static String reverse(String id) {
      return new ParsedTransformID().set(id).getBackwardId();
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

  static class MyHandler extends XMLFileReader.SimpleHandler {
    boolean first = true;
    ParsedTransformID directionInfo;
    XPathParts parts = new XPathParts();
    String cldrFileName;
    StringBuilder rules = new StringBuilder();

    public String getRules() {
      return rules.toString();
    }

    public MyHandler(String cldrFileName, ParsedTransformID directionInfo) {
      super();
      this.cldrFileName = cldrFileName;
      this.directionInfo = directionInfo;
    }

    public void handlePathValue(String path, String value) {
      //  private boolean handlePath(String cldrFileName, ParsedTransformID directionInfo, boolean first,
      //          StringBuffer rules, XPathParts parts, String path, String value) {
      if (first) {
        if (path.startsWith("//supplementalData/version")) {
          return;
        } else if (path.startsWith("//supplementalData/generation")) {
          return;
        }
        parts.set(path);
        Map<String,String> attributes = parts.findAttributes("transform");
        if (attributes == null) {
          throw new IllegalArgumentException("Not an XML transform file: " + cldrFileName + "\t" + path);
        }
        directionInfo.setSource((String) attributes.get("source"));
        directionInfo.setTarget((String) attributes.get("target"));
        directionInfo.setVariant((String) attributes.get("variant"));
        directionInfo.setDirection(Direction.valueOf(attributes.get("direction").toLowerCase(Locale.ENGLISH)));
        first = false;
      }
      if (path.indexOf("/comment") >= 0) {
        // skip
      } else if (path.indexOf("/tRule") >= 0) {
        value = fixup.transliterate(value);
        rules.append(value).append(CldrUtility.LINE_SEPARATOR);
      } else {
        throw new IllegalArgumentException("Unknown element: " + path + "\t " + value);
      }
    }
  }
}

