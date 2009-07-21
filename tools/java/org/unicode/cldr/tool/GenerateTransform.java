package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.icu.ConvertTransforms;
import org.unicode.cldr.tool.SearchXml.MyHandler;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.Row.R2;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Take a list of pairs, and generate a bidirectional transform.
 * 
 * @author markdavis
 * 
 */
// TODO handle casing
public class GenerateTransform {
  private static final String             TRANSFORM_DIRECTORY = CldrUtility.COMMON_DIRECTORY
                                                                      + "transforms"
                                                                      + File.separator;

  private final Set<Pair<String, String>> pairs               = new TreeSet<Pair<String, String>>(
                                                                      new MyComparator(ULocale.ROOT));
  private final Map<String, String>       source_target       = new LinkedHashMap<String, String>();
  private final Map<String, String>       target_source       = new LinkedHashMap<String, String>();
  private final UnicodeContext            addDotBetween       = new UnicodeContext();
  private final UnicodeContext            removeDot           = new UnicodeContext();
  private final Map<String, String>       target_retarget     = new LinkedHashMap<String, String>();

  private boolean sourceCased = false;
  private boolean targetCased = false;

  private static final String FALLOFFS = "̣̤̥̦̗̰̱";
  
  public void add(String sourceIn, String targetIn) {
    add(sourceIn, targetIn, -1);
    if (isCased(sourceIn)) {
      sourceCased  = true;
    }
    if (isCased(targetIn)) {
      targetCased   = true;
    }
  }
  
  private boolean isCased(String sourceIn) {
    if (UCharacter.toUpperCase(ULocale.ENGLISH, sourceIn).equals(sourceIn)) {
      return true;
    }
    if (UCharacter.toLowerCase(ULocale.ENGLISH, sourceIn).equals(sourceIn)) {
      return true;
    }
    return false;
  }

  public GenerateTransform add(String sourceIn, String targetIn, int falloff) {
    if (sourceIn.length() == 0 || targetIn.length() == 0) {
      throwException(sourceIn, targetIn, "both source and target must be nonempty");
    }
    final String source = Normalizer.decompose(sourceIn, false);
    final String target = falloff < 0 ? Normalizer.decompose(targetIn, false) 
            : Normalizer.decompose(targetIn + FALLOFFS.charAt(falloff), false);
    if (source_target.containsKey(source)) {
      throwException(sourceIn, targetIn, "source occurs twice");
    }
    if (target_source.containsKey(target)) {
      if (falloff >= FALLOFFS.length()) {
        throwException(sourceIn, targetIn, "target occurs twice");
      } else {
        return add(sourceIn, targetIn, falloff+1);
      }
    }
    source_target.put(source, target);
    target_source.put(target, source);
    pairs.add(Pair.of(source, target));
    return this;
  }

  private void throwException(final String source, final String target, final String title) {
    throw new IllegalArgumentException(title +
    		": " + source + " => " + target);
  }

  public String toRules(UnicodeSet sourceSet, UnicodeSet targetSet) {
    StringBuilder result = new StringBuilder();

    if (sourceCased && !targetCased) {
      result.append("::lowerCase ;" + CldrUtility.LINE_SEPARATOR);
    }
    UnicodeSet missingSource = decomposeAndRemoveMarks(sourceSet);
    UnicodeSet missingTarget = decomposeAndRemoveMarks(targetSet);
    // if we ever have a > XY, x -> X, and y -> Y
    // we have to add early
    // X {} y -> · ;
    // <- x {·} Y ;
    for (final String target : target_source.keySet()) {
      final String source = target_source.get(target);
      missingSource.remove(source);
      missingTarget.remove(target);
      for (String longerTarget : target_source.keySet()) {
        final int prefixLength = longerTarget.length() - target.length();
        if (prefixLength <= 0) {
          continue;
        }
        if (longerTarget.endsWith(target)) {
          String prefixTarget = longerTarget.substring(0, prefixLength);
          String prefixSource = target_source.get(prefixTarget);
          if (prefixSource != null) {
            addDotBetween.add(prefixSource, target);
            removeDot.add(prefixTarget, source);
          }
        }
      }
    }

    Set<UnicodeSet[]> items = addDotBetween.get();
    if (items.size() != 0) {
      result.append("# Sequences requiring insertion of hyphenation point for disambiguation" + CldrUtility.LINE_SEPARATOR);
      for (UnicodeSet[] pair : items) {
        // X {} y → · ;
        result.append(show(pair[0]) + " {} " + show(pair[1]) + " → ‧ ;" + CldrUtility.LINE_SEPARATOR);
      }
    }

    result.append("# Main rules" + CldrUtility.LINE_SEPARATOR);
    for (Pair<String, String> pair : pairs) {
      if (pair.getFirst().length() == 0) {
        continue;
      }
      result.append(pair.getFirst() + " ↔ " + pair.getSecond() + " ;" + CldrUtility.LINE_SEPARATOR);
    }
    
    items = removeDot.get();
    if (items.size() != 0) {
      result.append("# Removal of hyphenation point for disambiguation" + CldrUtility.LINE_SEPARATOR);
      for (UnicodeSet[] pair : items) {
        // ← x {·} Y ;
        result
        .append(" ← " + show(pair[0]) + " {‧} " + show(pair[1]) + " ;"
                + CldrUtility.LINE_SEPARATOR);
      }
    }

    if (target_retarget.size() != 0) {
      result.append("# Retargetting items for completeness" + CldrUtility.LINE_SEPARATOR);
      for (String target : target_retarget.keySet()) {
        // ← x {·} Y ;
        result.append("|" + target_retarget.get(target) + " ← " + target + " ;"
                + CldrUtility.LINE_SEPARATOR);
        missingTarget.remove(target);
      }
    }

    if (missingSource.size() != 0) {
      result.append("# Missing Source: " + missingSource.size() + " - "
              + missingSource.toPattern(false) + CldrUtility.LINE_SEPARATOR);
    }
    if (missingTarget.size() != 0) {
      result.append("# Missing Target: " + missingTarget.size() + " - "
              + missingTarget.toPattern(false) + CldrUtility.LINE_SEPARATOR);
    }
    
    if (!sourceCased && targetCased) {
      result.append("::(lowerCase) ;" + CldrUtility.LINE_SEPARATOR);
    }

    String rules = result.toString();
    if (false) {
    Transliterator forward = Transliterator.createFromRules("forward", rules,
            Transliterator.FORWARD);
    Transliterator reverse = Transliterator.createFromRules("reverse", rules,
            Transliterator.REVERSE);
    }
    return rules;
  }

  private UnicodeSet decomposeAndRemoveMarks(UnicodeSet sourceSet) {
    UnicodeSet result = new UnicodeSet();
    for (UnicodeSetIterator it = new UnicodeSetIterator(sourceSet); it.next();) {
      String decompose = Normalizer.decompose(it.getString(), true);
      decompose = UCharacter.foldCase(decompose, true);
      result.addAll(Normalizer.decompose(decompose, true));
    }
    return result;
  }

  private String show(UnicodeSet unicodeSet) {
    if (unicodeSet.size() == 1) {
      UnicodeSetIterator it = new UnicodeSetIterator(unicodeSet);
      it.next();
      return it.getString();
    }
    return unicodeSet.toPattern(false);
  }

  static class MyComparator implements Comparator<Pair<String, String>> {
    private Collator collator;

    public MyComparator(ULocale locale) {
      collator = Collator.getInstance(locale);
      collator.setStrength(collator.IDENTICAL);
    }

    public int compare(Pair<String, String> arg0, Pair<String, String> arg1) {
      int result = arg0.getFirst().length()
      - arg1.getFirst().length();
      if (result != 0) {
        return -result;
      }
      result = arg0.getSecond().length()
      - arg1.getSecond().length();
      if (result != 0) {
        return -result;
      }
      result = collator.compare(arg0.getFirst(), arg1.getFirst());
      return result;
    }
  }

  static class UnicodeContext {
    Map<String, UnicodeSet> first_second = new LinkedHashMap();

    void add(String a, String b) {
      UnicodeSet second = first_second.get(a);
      if (second == null) {
        first_second.put(a, second = new UnicodeSet());
      }
      second.add(b);
    }

    Set<UnicodeSet[]> get() {
      Map<UnicodeSet, UnicodeSet> second_first = new LinkedHashMap();
      for (String first : first_second.keySet()) {
        UnicodeSet second = first_second.get(first);
        UnicodeSet firstSet = second_first.get(second);
        if (firstSet == null) {
          second_first.put(second, firstSet = new UnicodeSet());
        }
        firstSet.add(first);
      }
      Set<UnicodeSet[]> result = new LinkedHashSet();
      for (UnicodeSet second : second_first.keySet()) {
        UnicodeSet first = second_first.get(second);
        result.add(new UnicodeSet[] { first, second });
      }
      return result;
    }
  }

  public void addFallback(String target, String retarget) {
    target_retarget.put(target, retarget);
  }

  public List<String> getCldrTransformNames() {
    return Arrays.asList(new File(TRANSFORM_DIRECTORY).list());
  }

  public GenerateTransform addFromCldrFile(String transformName, int dir) {
    String rules = transformFromCldrFile(transformName);
    Transliterator trans = Transliterator.createFromRules(transformName, rules, dir);
    UnicodeSet sourceSet = trans.getSourceSet();
    for (UnicodeSetIterator it = new UnicodeSetIterator(sourceSet); it.next();) {
      String source = it.getString();
      add(source, trans.transform(source));
    }
    return this;
  }

  public String transformFromCldrFile(String transformName) {
    MyHandler myHandler = new MyHandler();
    XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
    xfr.read(TRANSFORM_DIRECTORY + transformName + ".xml", XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
    return myHandler.toString();
  }

  static final Transliterator fixup = Transliterator.getInstance("[:Mn:]any-hex/java");

  static class MyHandler extends XMLFileReader.SimpleHandler {
    private StringBuilder output = new StringBuilder();

    public void clear() {
      output.setLength(0);
    }

    public String toString() {
      return output.toString();
    }

    public void handlePathValue(String path, String value) {
      if (path.indexOf("/comment") >= 0) {
        if (!value.trim().startsWith("#"))
          value = value + "# ";
        output.append(value).append(CldrUtility.LINE_SEPARATOR);
      } else if (path.indexOf("/tRule") >= 0) {
        // value = replaceUnquoted(value,"\u00A7", "&");
        // value = ConvertTransforms.replaceUnquoted(value, "\u2192", ">");
        //value = ConvertTransforms.replaceUnquoted(value, "\u2190", "<");
        //value = ConvertTransforms.replaceUnquoted(value, "\u2194", "<>");
        value = fixup.transliterate(value);
        output.append(value).append(CldrUtility.LINE_SEPARATOR);
      } else {
        throw new IllegalArgumentException("Unknown element: " + path + "\t " + value);
      }
    }
  }
}
