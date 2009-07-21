package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Pair;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Takes a list of mappings (tab delimited) from source to target and produces a
 * transliterator
 * 
 * @author markdavis
 * http://en.wikipedia.org/wiki/English_phonology
 */
public class MakeTransliterator {
  // DEBUGGING
  private static final String separateSuffix = ""; // "}$x";
  static int forceSeparateIfShorter = 4; // 4
  
  private static final String CHECK_BASE = null; // "vessel";
  private static final String CHECK_BUILT =  null; // "vessel";


  private static final String TEST_STRING = "territories";
  private static final boolean SHOW_OVERRIDES = true;
  
  private static final int MINIMUM_FREQUENCY = 9999;
  
  static boolean isIPA = true;
  static boolean onlyToTarget = true;
  
  // others
  
  static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
  
  static Collator col = Collator.getInstance(ULocale.ROOT);
  
  static String cldrDataDir = "C:\\cvsdata\\unicode\\cldr\\tools\\java\\org\\unicode\\cldr\\util\\data\\transforms\\";
  
  public static void main(String[] args) throws IOException {
    setTranslitDebug(true);
    
    Locale fil = new Locale("fil");
    System.out.println(fil);
    fil = new Locale("fil","US");
    System.out.println(fil);


    String sourceFile = cldrDataDir + "internal_raw_IPA.txt";
    String targetFile = cldrDataDir + "en-IPA.txt";
    String targetCountFile = cldrDataDir + "en-IPA_count.txt";
    String skippedLinesFile = "C:\\DATA\\GEN\\SkippedIPA.txt";
    
    PrintWriter skippedOut = BagFormatter.openUTF8Writer("", skippedLinesFile);
    
    //String coreRules = getCoreTransliterator();
    String fixBadIpaRules = createFromFile(cldrDataDir + "internal_fixBadIpa.txt", null, null);
    fixBadIpa = Transliterator.createFromRules("foo", fixBadIpaRules, Transliterator.FORWARD);
    
    Map<String, String> overrides = getOverrides();
    
    String coreForeRules = createFromFile(cldrDataDir + "internal_baseEnglishToIpa.txt", null, null);
    coreBase = Transliterator.createFromRules("foo", coreForeRules, Transliterator.FORWARD);
    if (CHECK_BASE != null) {
      setTranslitDebug(true);
      System.out.println(coreBase.transliterate(CHECK_BASE));
      return;
    }
    
    if (CHECK_BUILT != null) {
      String foo = createFromFile(cldrDataDir + "en-IPA.txt", null, null);
      Transliterator fooTrans = Transliterator.createFromRules("foo", foo, Transliterator.FORWARD);

      setTranslitDebug(true);
      System.out.println(fooTrans.transliterate(CHECK_BUILT));
      return;
    }
    
    String coreBackRules = createFromFile(cldrDataDir + "internal_English-IPA-backwards.txt", null, null);
    checkCoreReversibility(skippedOut, coreForeRules, coreBackRules);
    String coreRules = coreForeRules + coreBackRules;
    System.out.println(coreRules);
    
    // C:\DATA\GEN\mergedIPA2.txt
    // we have to have items in order. Longest forms need to come first, on both
    // sides.
    Relation<String, Pair<String,Long>> store = new Relation(new TreeMap(MyComparator),
        TreeSet.class);
    
    targetCharacters = new UnicodeSet();
    sourceCharacters = new UnicodeSet();
    allowedSourceCharacters = (UnicodeSet) new UnicodeSet(
    "[[:Letter:]\u2019]").freeze();
    allowedTargetCharacters = (UnicodeSet) new UnicodeSet(
    "[\u00E6 \u0251 b d\u00F0 e \u0259 \u025B f-i \u026A j-n \u014B o p r s \u0283 t u \u028A v w z \u0292 \u03B8]")
    .freeze();
    countSkipped = 0;
    totalFrequency = 0;
    skippedFrequency = 0;
    int targetField = isIPA ? 2 : 1;
    
    BufferedReader in = BagFormatter.openUTF8Reader("", sourceFile);
    while (true) {
      String line = in.readLine();
      if (line == null)
        break;
      if (line.startsWith("\uFEFF")) {
        line = line.substring(1);
      }
      String originalLine = line;
      int commentCharPosition = line.indexOf('#');
      if (commentCharPosition >= 0) {
        line = line.substring(0, commentCharPosition);
      }
      line = line.trim();
      frequency = -1;
      String[] pieces = line.split(" *[\\t,] *");
      if (pieces.length <= targetField) {
        // skippedOut.println(originalLine + "\tno phonetics");
        // countSkipped++;
        continue; // no phonetics
      }
      String source = pieces[0];
      if (TEST_STRING != null && source.equals(TEST_STRING)) {
        System.out.println(line); // for debugging
      }
      
      // Fix Source
      source = source.replace("'", "’");
      source = UCharacter.toLowerCase(ULocale.ENGLISH, source);
      if (source.endsWith(".")) {
        source = source.substring(0, source.length() - 1);
      }
      if (source.contains(" ") || source.contains("-")) {
        skippedOut.println(originalLine + "\tspace or hyphen");
        countSkipped++;
        skippedFrequency += frequency;
        continue;
      }
      
      String bestTarget = null;
      
      String override = overrides.get(source);
      String spelling = spellout.transliterate(source);
      
      for (int i = 1; i < pieces.length; ++i) {
        String target = pieces[i];
        if (target.startsWith("%")) {
          frequency = Long.parseLong(target.substring(1));
          continue;
        }
        
        if (override != null) {
          if (SHOW_OVERRIDES) System.out.println("Overriding\t" + source + " → ! " + target + " → " + override);
          if (override.length() != 0) {
            if (TEST_STRING != null && source.equals(TEST_STRING)) {
              setTranslitDebug(true);
            }
            target = fixBadIpa.transliterate(override);
            setTranslitDebug(false);
            addSourceTarget(skippedOut, source, target, originalLine, store);      
          }
          break;
        } 
  
        if (frequency < MINIMUM_FREQUENCY) {
          //skippedOut.println(originalLine + "\tno frequency");
          countSkipped++;
          continue;          
        }
        
        target = UCharacter.toLowerCase(ULocale.ENGLISH, target);
        target = target.replace(" ", ""); // remove extra spaces
        
        if (target.startsWith("-") || target.endsWith("-")) {
          continue;
        }
        
        String oldTarget = target;
        target = fixBadIpa.transliterate(target);

        if (target.equals(spelling)) {
          skippedOut.println(originalLine
              + "\tspellout");
          countSkipped++;
          continue;
        }
        
        if (!target.equals(oldTarget)) {
          skippedOut.println("\t### fixed IPA:\t" + source + "\t" + target
              + "\twas: " + oldTarget);
        }
        
        addSourceTarget(skippedOut, source, target, originalLine, store);
      }
    }
    
    // add the overrides that are not in.
    
    for (String word : overrides.keySet()) {
      if (!store.containsKey(word)) {
        String target = overrides.get(word);
        if (target.length() != 0) {
          if (SHOW_OVERRIDES) System.out.println("New overrides:\t" + word + " → " + target);
          addSourceTarget(skippedOut, word, target, "overrides", store);
        }
      }
    }
    in.close();
    System.out.println("total count: " + nf.format(store.size()));
    System.out.println("skipped count: " + nf.format(countSkipped));
    
    System.out.println("total frequency-weighted: " + nf.format(totalFrequency));
    System.out.println("skipped frequency-weighted: " + nf.format(skippedFrequency));
    
    if (false) {
      System.out.println(CldrUtility.LINE_SEPARATOR + "Source Characters ");
      showSet(sourceCharacters);
      System.out.println(CldrUtility.LINE_SEPARATOR + "Target Characters ");
      showSet(targetCharacters);
    }
    
    //Set<String> seenSource = new HashSet<String>();
    //Set<String> seenTarget = new HashSet<String>();
    
    int countAdded = 0;
    int countTotal = 0;
    long frequencyAdded = 0;
    long frequencySkipped = 0;
    
    Transliterator base = Transliterator.createFromRules("foo", coreRules, Transliterator.FORWARD);
    // build up the transliterator one length at a time.
    List<String> newRules = new ArrayList<String>();
    StringBuilder buffer = new StringBuilder();
    
    int lastSourceLength = 1;
    
    Relation<Long, String> count_failures = new Relation(new TreeMap(), TreeSet.class);
    
    sourceLoop:
      for (String source : store.keySet()) {
        if (TEST_STRING != null && source.equals(TEST_STRING)) {
          System.out.println(source + "\t" + store.getAll(source));
        }
        countTotal++;
        // whenever the source changes in length, rebuild the transliterator
        if (source.length() != lastSourceLength && source.length() >= forceSeparateIfShorter) {
          System.out.println("Building transliterator for length " + lastSourceLength + " : " + newRules.size());
          System.out.flush();
          skippedOut.flush();
          String rules = buildRules(coreRules, newRules, buffer);
          //System.out.println(rules);
          base = Transliterator.createFromRules("foo", rules, Transliterator.FORWARD);
          
          lastSourceLength = source.length();
        }
        Set<Pair<String, Long>> targetSet = store.getAll(source);
        // see if any of the mappings fall out
        String targetUsingCore = base.transliterate(source);
        
        String bestTarget = null;
        int bestDistance = 999;
        long frequency = 0;
        for (Pair<String, Long> targetPair : targetSet) {
          String target = targetPair.getFirst();
          if (target.length() == 0) {
            throw new IllegalArgumentException(source + " → " + target);
          }
          frequency = targetPair.getSecond();
          
          if (targetUsingCore.equals(target)) {
            // we have a match! skip this source
            skippedOut.println("# skipping " + source + " → " + target + " ;");
            frequencySkipped += frequency;
            continue sourceLoop;
          }
          if (mostlyEqual(source, target, targetUsingCore)) {
            // we have a match! skip this source
            skippedOut.println("# skipping " + source + " → " + target + " ; # close enough to " + targetUsingCore);
            frequencySkipped += frequency;
            continue sourceLoop;
          }
          int distance = distance(source, target, targetUsingCore);
          if (bestDistance > distance) {
            bestTarget = target;
            bestDistance = distance;
          }
        }
        // if we get to here, we have a new rule.
        if (bestTarget != null) {
          boolean forceSeparate = false;
          if (source.length() < forceSeparateIfShorter || bestTarget.length() * 2 > source.length() * 3) {
            forceSeparate = true;
          } else {
            String spelling = spellout.transliterate(source);
            if (bestTarget.equals(spelling)) {
              forceSeparate = true;
            } else {
              // if it is likely that the word can have an extra letter added that changes the pronunciation
              // force it to be separate
              if (source.endsWith("e")) {
                forceSeparate = true;
              }
            }
          }
          String targetUsingBaseCore = coreBase.transliterate(source);
          
          if (forceSeparate) {
            source = "$x{" + source + "}$x";
          } else {
            source = "$x{" + source;
          }
          // strange hack
          String hackSource = source.startsWith("use") ? "'" + source + "'" : source;
          newRules.add(hackSource + " → " + bestTarget + " ; # " + targetUsingCore + (targetUsingBaseCore.equals(targetUsingCore) ? "" : "\t\t" + targetUsingBaseCore) + CldrUtility.LINE_SEPARATOR);
          skippedOut.println("# couldn't replace  " + source + " → " + bestTarget + " ; # " + targetUsingCore );
          count_failures.put(-frequency, source + " → " + bestTarget + " ; # " + targetUsingCore);
          countAdded++;
          frequencyAdded += frequency;
        }
      }
    
    String rules = buildRules(coreRules, newRules, buffer);
    base = Transliterator.createFromRules("foo", rules, Transliterator.FORWARD); // verify that it builds
    
    PrintWriter out = BagFormatter.openUTF8Writer("", targetFile);
    out.println(rules);
    out.close();
    
    out = BagFormatter.openUTF8Writer("", targetCountFile);
    for (long count : count_failures.keySet()) {
      for (String line : count_failures.getAll(count)) {
        out.println(count + "\t" + line);
      }
    }
    out.close();
    
    
//  if (false) {
//  
//  // now write out the transliterator file
//  PrintWriter out = BagFormatter.openUTF8Writer("", targetFile);
//  for (String source : store.keySet()) {
//  Set<String> targetSet = store.getAll(source);
//  for (String target : targetSet) {
//  if (seenSource.contains(source)) {
//  if (onlyToTarget) {
//  // nothing
//  } else if (seenTarget.contains(target)) {
//  skippedOut.println("# " + source + " → " + target + " ;");
//  countSkipped++;
//  } else {
//  out.println(source + " ← " + target + " ;");
//  countSourceFromTarget++;
//  }
//  } else if (onlyToTarget || seenTarget.contains(target)) {
//  out.println(source + " → " + target + " ;");
//  countSourceToTarget++;
//  } else {
//  out.println(source + " ↔ " + target + " ;");
//  countSourceAndTarget++;
//  }
//  seenSource.add(source);
//  seenTarget.add(target);
//  }
//  }
//  out.close();
//  }
    skippedOut.close();
    System.out.println("countTotal: " +nf.format( countTotal));
    System.out.println("countAdded: " + nf.format(countAdded));
    System.out.println("countSkipped: " + nf.format(countTotal - countAdded));
    System.out.println("frequencyTotal: " + nf.format(frequencyAdded + frequencySkipped));
    System.out.println("frequencyAdded: " + nf.format(frequencyAdded));
    System.out.println("frequencySkipped: " + nf.format(frequencySkipped));
  }

  private static void setTranslitDebug(boolean newSetting) {
    //Transliterator.DEBUG = newSetting;
    try {
      Field debug = Transliterator.class.getField("DEBUG");
      debug.setBoolean(Transliterator.class, newSetting);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private static void addSourceTarget(PrintWriter skippedOut, String source, String target, String originalLine, Relation<String, Pair<String, Long>> store) {
    if (source.equals("teh")) {
      System.out.println("debug");
    }
    if (!allowedSourceCharacters.containsAll(source)) {
      skippedOut.println(originalLine
          + "\t# Strange source values:\t"
          + source
          + "\t"
          + new UnicodeSet().addAll(source)
          .removeAll(allowedSourceCharacters).toPattern(false));
      countSkipped++;
      skippedFrequency += frequency;
      return;
    }
    if (!allowedTargetCharacters.containsAll(target)) {
      System.out.println(originalLine
          + "\t# Strange target values:\t"
          + target
          + "\t"
          + new UnicodeSet().addAll(target)
          .removeAll(allowedTargetCharacters).toPattern(false));
      countSkipped++;
      skippedFrequency += frequency;
      return;
    }
    
    sourceCharacters.addAll(source);
    targetCharacters.addAll(target);
    store.put(source, new Pair<String,Long>(target, frequency));
    totalFrequency += frequency;
    
  }
  
  private static void checkCoreReversibility(PrintWriter skippedOut, String coreRules, String coreBackRules) {
    Transliterator base = Transliterator.createFromRules("foo", coreRules, Transliterator.FORWARD);
    Transliterator back = Transliterator.createFromRules("foo2", coreBackRules, Transliterator.REVERSE);
    String[] tests = "bat bait bet beet bit bite bot boat but bute bout boot book boy pat bat vat fat mat tat dat thew father nat sat zoo ash asia gate cat late rate hate yet rang chat jet".split("\\s");
    for (String test : tests) {
      String test2 = base.transliterate(test);
      String test3 = back.transliterate(test2);
      skippedOut.println(test + "\t " + test2 + "\t " + test3);
    }
    skippedOut.flush();
  }
  
  private static String buildRules(String coreRules, List newRules, StringBuilder buffer) {
    Transliterator base;
    // build backwards!!
    buffer.setLength(0);
    buffer.append(
        "# Author: M Davis" + CldrUtility.LINE_SEPARATOR +
        "# Email: mark.davis@icu-project.org" + CldrUtility.LINE_SEPARATOR +
        "# Description: English to IPA" + CldrUtility.LINE_SEPARATOR +
        //"$nletter {([A-Z]+)} $nletter > &en-IPA/spellout($1) ; " + Utility.LINE_SEPARATOR +
        ":: lower(); " + CldrUtility.LINE_SEPARATOR +
    "$x = [:^letter:] ;" + CldrUtility.LINE_SEPARATOR);
    for (int i = newRules.size() - 1; i >= 0; --i) {
      buffer.append(newRules.get(i));
    }
    buffer.append(coreRules);
    // System.out.println(buffer);
    String result = buffer.toString();
    // ensure it builds
    return result;
  }
  
  private static void showSet(UnicodeSet sourceCharacters) {
    for (UnicodeSetIterator it = new UnicodeSetIterator(sourceCharacters); it
    .next();) {
      System.out.println(com.ibm.icu.impl.Utility.hex(it.codepoint) + "\t("
          + UTF16.valueOf(it.codepoint) + ")\t"
          + UCharacter.getName(it.codepoint));
    }
  }
  
  public static UnicodeSet vowels = (UnicodeSet) new UnicodeSet("[aeiou æ ɑ ə ɛ ɪ ʊ â î ô]").freeze();
  public static UnicodeSet short_vowels = (UnicodeSet) new UnicodeSet("[ɑ æ ə ɛ ɪ ʊ]").freeze();
  /**
   * Return true if the strings are essentially the same.
   * Differences between schwas and short vowels are counted in certain cases
   * @param target
   * @param targetUsingCore
   * @param targetUsingCore2 
   * @return
   */
  static UnicodeSet targetChars = new UnicodeSet();
  static UnicodeSet targetCoreChars = new UnicodeSet();
  static UnicodeSet tempDiff = new UnicodeSet();
  static Transliterator distinguishLongVowels = Transliterator.createFromRules("faa",
      "ɑʊ > â ;" +
      "ɑɪ > î ;" +
      "oɪ > ô ;",
      Transliterator.FORWARD);
  
  private static int distance(String source, String target, String targetUsingCore) {
    if (target.equals(targetUsingCore)) return 0;
    if (mostlyEqual(source, target, targetUsingCore)) return 1;
    // first compare the consonants. Count each difference as 3
    String zappedTarget = distinguishLongVowels.transliterate(target);
    String zappedCoreTarget = distinguishLongVowels.transliterate(targetUsingCore);
    
    targetChars.clear().addAll(zappedTarget); // 
    targetCoreChars.clear().addAll(zappedCoreTarget);
    if (targetChars.equals(targetCoreChars)) {
      return 3;
    }
    targetChars.removeAll(short_vowels);
    targetCoreChars.removeAll(short_vowels);
    if (targetChars.equals(targetCoreChars)) {
      return 5;
    }
    
    targetChars.removeAll(vowels);
    targetCoreChars.removeAll(vowels);
    if (targetChars.equals(targetCoreChars)) {
      return 5;
    }
    
    tempDiff.clear().addAll(targetChars).removeAll(targetCoreChars);
    int result = 7 + tempDiff.size();
    tempDiff.clear().addAll(targetCoreChars).removeAll(targetChars);
    result += tempDiff.size();
    return result;
  }
  
  static final Transliterator skeletonize  = Transliterator.createFromRules("faa",
      "ɑʊ > âʊ ;" +
      "ɑɪ > âi ;" +
      "oɪ > oi ;" +
      "ɑr > âr ;" +
      "ær > er ;"+
      "ɛr > er ;" +
      "ɪr > ir ;" +
      "ʊr > ur ;",
      Transliterator.FORWARD);
  
  private static boolean mostlyEqual(String inSource, String inTarget, String inTargetUsingCore) {
    
    if (inTarget.length() != inTargetUsingCore.length()) return false;

    // transform these -- simplest that way
    String target = skeletonize.transliterate(inTarget);
    String targetUsingCore = skeletonize.transliterate(inTargetUsingCore);
    
    int vowelCount = 0;
    int diffCount = 0;
    for (int i = 0; i < target.length(); ++i) {
      char ca = target.charAt(i);
      char cb = targetUsingCore.charAt(i);
      if (vowels.contains(ca)) {
        vowelCount++;
      }
      if (ca != cb) {
        // disregard differences with short vowels
        if (ca == 'ə' && short_vowels.contains(cb) || short_vowels.contains(ca) && cb == 'ə') {
          diffCount++;
          continue;
        }
        //ɛ")  && a.startsWith("ɪ")
        if (ca == 'ɪ' && cb == 'ɛ'  || ca == 'ɪ' && cb == 'ɛ') {
          diffCount++;
          continue;
        }
        return false;
      }
    }
    return true; // return diffCount == 0 ? true : diffCount < vowelCount;
  }
  
  static Transliterator spellout = Transliterator.createFromRules("foo",
      "a > e ;"
      + "b > bi ;"
      + "c > si ;" 
      + "d > di ;"
      + "e > i ;"
      + "f > ɛf ;" 
      + "g > dʒi ;" 
      + "h > etʃ ;" 
      + "i > ɑɪ ;"
      + "j > dʒe ;"
      + "k > ke ;" 
      + "l > ɛl ;" 
      + "m > ɛm ;" 
      + "n > ɛn ;" 
      + "o > o ;"
      + "p > pi ;"
      + "q > kwu ;"
      + "r > ɑr ;"
      + "s > ɛs ;"
      + "t > ti ;"
      + "u > ju ;"
      + "v > vi ;"
      + "w > dəbjə ;"
      + "x > ɛks ;"
      + "y > wɑɪ ;"
      + "z > zi ;", 
      Transliterator.FORWARD);
  
  /**
   * Returns items sorted alphabetically, shortest first
   */
  static Comparator MyComparator = new Comparator() {
    
    public int compare(Object a, Object b) {
      String as = (String) a;
      String bs = (String) b;
      if (as.length() < bs.length())
        return -1;
      if (as.length() > bs.length())
        return 1;
      int result = col.compare(as, bs);
      if (result != 0) {
        return result;
      }
      return as.compareTo(bs);
    }
    
  };
//static String dataDir = "C:\\cvsdata\\unicode\\ucd\\unicodetools\\dictionary\\Data\\";
//private static String getCoreTransliterator() throws IOException {
//
//String accentRules = createFromFile(dataDir + "accentRules.txt", null, null);
//
//Transliterator doAccentRules = Transliterator.createFromRules("foo", accentRules, Transliterator.FORWARD);
//
//String markedToIpa = createFromFile(dataDir + "IPARules.txt", doAccentRules, null);
//System.out.println(markedToIpa);
//Transliterator doMarkedToIpa = Transliterator.createFromRules("foo", markedToIpa, Transliterator.FORWARD);
//
//String trial = "ạ>æ";
//String result = doMarkedToIpa.transliterate(trial);
//System.out.println("****" + result);
//
//String englishToIpaBase = createFromFile(dataDir + "reduceRules.txt", doAccentRules, doMarkedToIpa);
//
//System.out.println(englishToIpaBase);
//
////Transform file name into id
//
//return englishToIpaBase;
//}
  
  public static String createFromFile(String fileName, Transliterator pretrans, Transliterator pretrans2) throws IOException {
    StringBuilder buffer = new StringBuilder();
    BufferedReader fli = BagFormatter.openUTF8Reader("", fileName);
    while (true) {
      String line = fli.readLine();
      if (line == null) break;
      if (line.startsWith("\uFEFF")) line = line.substring(1);
      if (pretrans != null) {
        line = pretrans.transliterate(line);
      }
      if (pretrans2 != null) {
        line = pretrans2.transliterate(line);
      }
      
      buffer.append(line);
      buffer.append(CldrUtility.LINE_SEPARATOR); // separate with whitespace
    }
    fli.close();
    return buffer.toString();
  }
  
  static int LIMIT = Integer.MAX_VALUE;
  private static Transliterator fixBadIpa;
  private static UnicodeSet targetCharacters;
  private static UnicodeSet sourceCharacters;
  private static UnicodeSet allowedSourceCharacters;
  private static UnicodeSet allowedTargetCharacters;
  private static int countSkipped;
  private static long skippedFrequency;
  private static long frequency;
  private static long totalFrequency;
  private static Transliterator coreBase;
  
  public static Map<String,String> getOverrides() throws IOException {
    Map<String,String> result = new TreeMap<String,String>();
    BufferedReader br = BagFormatter.openUTF8Reader(cldrDataDir, "internal_overrides.txt");
    try {
      int counter = 0;
      while (counter < LIMIT) {
        String line = br.readLine();
        if (line == null) break;
        line = line.trim();
        if (line.length() == 0) continue;
        
        String[] iLine = line.split("\\s*→\\s*");
        String word = iLine[0].trim();
        if (result.containsKey(word)) {
          System.out.println("Overrides already contain: " + word);
          continue;
        }
        if (iLine.length < 2) {
          result.put(word, "");
        } else {
          String ipa = fixBadIpa.transliterate(iLine[1].trim());
          result.put(word, ipa);
        }
      }
    } finally {
      br.close();
    }
    return result;
  }
  
  
}