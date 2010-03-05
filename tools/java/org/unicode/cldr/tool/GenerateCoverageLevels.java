package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CoverageLevel;
import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transform;

public class GenerateCoverageLevels {
  private static boolean SKIP_UNCONFIRMED = true;
  private static int SHOW_EXAMPLES = 5;
  private static final String FILES = ".*";
  private static final String MAIN_DIRECTORY = CldrUtility.MAIN_DIRECTORY;//CldrUtility.SUPPLEMENTAL_DIRECTORY; //CldrUtility.MAIN_DIRECTORY;
  private static final String COLLATION_DIRECTORY = CldrUtility.COMMON_DIRECTORY + "/collation/";//CldrUtility.SUPPLEMENTAL_DIRECTORY; //CldrUtility.MAIN_DIRECTORY;
  private static final String RBNF_DIRECTORY = CldrUtility.COMMON_DIRECTORY + "/rbnf/";//CldrUtility.SUPPLEMENTAL_DIRECTORY; //CldrUtility.MAIN_DIRECTORY;
  private static final String OUT_DIRECTORY = CldrUtility.GEN_DIRECTORY + "/coverage/"; //CldrUtility.MAIN_DIRECTORY;
  private static final Factory cldrFactory = Factory.make(MAIN_DIRECTORY, FILES);
  private static final Comparator attributeComparator = CLDRFile.getAttributeComparator();
  private static final CLDRFile english = cldrFactory.make("en", true);
  private static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(english.getSupplementalDirectory());
  private static Set<String> defaultContents = supplementalData.getDefaultContentLocales();
  private static Map<String, List<String>> languageAliasInfo = supplementalData.getLocaleAliasInfo().get("language");
  private static LocaleFilter localeFilter = new LocaleFilter(true);
  private static LocaleFilter nonAliasLocaleFilter = new LocaleFilter(false);
  

  private static final CoverageLevel coverageLevel = new CoverageLevel();
  private static final long COLLATION_WEIGHT = 50;
  private static final Level COLLATION_LEVEL = Level.POSIX;
  private static final long PLURALS_WEIGHT = 20;
  private static final Level PLURALS_LEVEL = Level.MINIMAL;
  private static final long RBNF_WEIGHT = 20;
  private static final Level RBNF_LEVEL = Level.MODERATE;

  static {
    coverageLevel.setFile(english, new TreeMap(), null, new ArrayList());
  }
  static int totalCount = 0;

  enum Inheritance {actual, inherited}

  public static void main(String[] args) throws IOException {
    PrintWriter out = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "fullpaths.txt");
    showEnglish(out);
    out.close();

    System.out.println("*** TODO check collations, RBNF, Transforms (if non-Latin)");
    PrintWriter summary = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "summary.txt");
    PrintWriter samples = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "samples.txt");
    summarizeCoverage(summary, samples);
    summary.close();
    samples.close();
  }

  private static void showEnglish(PrintWriter out) throws IOException {
    CLDRFile cldrFile = english;
    String locale = "en";
    Set<String> sorted = Builder.with(new TreeSet<String>()).addAll(cldrFile.iterator()).addAll(cldrFile.getExtraPaths()).get();
    Set<R3<Level,String,Inheritance>> items = new TreeSet<R3<Level,String,Inheritance>>(new RowComparator());
    for (String path : sorted) {
      if (path.endsWith("/alias")) {
        continue;
      }
      String fullPath = cldrFile.getFullXPath(path);
      String source = cldrFile.getSourceLocaleID(path, null);
      Inheritance inherited = !source.equals(locale) ? Inheritance.inherited : Inheritance.actual;

      Level level = coverageLevel.getCoverageLevel(fullPath, path);

      items.add(Row.of(level, path, inherited));
    }

    PathStore store = new PathStore();
    for (R3<Level,String,Inheritance> item : items) {
      show(out, cldrFile, item, store);
    }
    show(out, cldrFile, null, store);
  }

  private static class RowComparator implements Comparator<R3<Level,String,Inheritance>> {

    public int compare(R3<Level, String, Inheritance> o1, R3<Level, String, Inheritance> o2) {
      int result = o1.get0().compareTo(o2.get0());
      if (result != 0) return result;
      result = CLDRFile.ldmlComparator.compare(o1.get1(), o2.get1());
      if (result != 0) return result;    
      result = o1.get2().compareTo(o2.get2());
      return result;    
    }
  }

  private static void show(PrintWriter out, CLDRFile cldrFile, R3<Level, String, Inheritance> next, PathStore store) {
    R5<Level, Inheritance, Integer, String, TreeMap<String, Relation<String, String>>> results = store.add(next);
    if (results != null) {
      Level lastLevel = results.get0();
      Inheritance lastInheritance = results.get1();
      int count = results.get2();
      String path = results.get3();
      totalCount += count;
      try {
        StringBuilder resultString = new StringBuilder();
        TreeMap<String, Relation<String, String>> types = results.get4();
        for (String key : types.keySet()) {
          Relation<String, String> attr_values = types.get(key);
          for (String attr : attr_values.keySet()) {
            resultString.append("\t").append(key + ":\u200b" + attr).append("=\u200b").append(attr_values.getAll(attr));
          }
        }
        out.println(lastLevel.ordinal() 
                + "\t" + lastLevel
                + "\t" + count 
                + "\t" + totalCount 
                //+ "\t" + lastInheritance + 
                //"\t" + cldrFile.getStringValue(path) + 
                + "\t" + path + resultString);
      } catch (RuntimeException e) {
        throw e;
      }
    }
  }

  static class PathStore {
    XPathParts lastParts = new XPathParts();
    XPathParts nextParts = new XPathParts();
    Level lastLevel;
    Inheritance lastInheritance;
    int count = 0;

    TreeMap<String,Relation<String,String>> differences = new TreeMap<String,Relation<String,String>>();

    R5<Level, Inheritance, Integer, String, TreeMap<String, Relation<String, String>>> add(R3<Level, String, Inheritance> next) {
      count ++;
      boolean wasNull = lastLevel == null;
      Level level = null;
      String path = null;
      Inheritance inherited = null;

      if (next != null) {
        level = next.get0();
        path = next.get1();
        inherited = next.get2();

        setParts(nextParts, path);
        if (sameElements()) {
          addDifferences();
          return null;
        }
      }
      // clear the values
      clean(lastParts,differences);
      R5<Level, Inheritance, Integer, String, TreeMap<String, Relation<String, String>>> results = Row.of(lastLevel, lastInheritance, count-1, lastParts.toString().replace("/", "\u200B/"), differences);
      lastParts = nextParts;
      differences = new TreeMap<String,Relation<String,String>>();
      nextParts = new XPathParts();
      lastLevel = level;
      lastInheritance = inherited;
      count = 1;
      if (wasNull) return null;
      return results;
    }

    private void clean(XPathParts lastParts2, TreeMap<String, Relation<String, String>> differences2) {
      for (int i = 0; i < lastParts.size(); ++i) {
        String element = lastParts.getElement(i);
        Relation<String, String> attr_values = differences2.get(element);
        if (attr_values == null) continue;
        for (String attr : attr_values.keySet()) {
          lastParts.putAttributeValue(i, attr, "*");
        }
      }      
    }

    private void setParts(XPathParts parts, String path) {
      parts.set(path);
      if (path.startsWith("//ldml/dates/timeZoneNames/metazone") || path.startsWith("//ldml/dates/timeZoneNames/zone")) {
        String element = nextParts.getElement(-1);
        nextParts.setElement(-1, "zoneChoice");
        nextParts.putAttributeValue(-1, "type", element);
        element = nextParts.getElement(-2);
        nextParts.setElement(-2, "zoneLength");
        nextParts.putAttributeValue(-2, "type", element);
      } else if (path.startsWith("//ldml/dates/calendars/calendar")) {
        if (!"gregorian".equals(parts.getAttributeValue(3, "type"))) {
          for (int i = parts.size()-1; i > 3; --i) {
            parts.removeElement(i);
          }
          parts.addElement("*");
        }
      }
    }

    private void addDifferences() {
      for (int i = 0; i < lastParts.size(); ++i) {
        Map<String, String> lastAttrs = lastParts.getAttributes(i);
        Map<String, String> nextAttrs = nextParts.getAttributes(i);
        if (!lastAttrs.equals(nextAttrs)) {
          String element = lastParts.getElement(i);
          Relation<String, String> old = differences.get(element);
          if (old == null) {
            differences.put(element, old = new Relation(new TreeMap(attributeComparator), TreeSet.class));
          }
          Set<String> union = Builder.with(new TreeSet<String>()).addAll(lastAttrs.keySet()).addAll(nextAttrs.keySet()).get();
          for (String key : union) {
            String lastValue = lastAttrs.get(key);
            String nextValue = nextAttrs.get(key);
            if (!Utility.objectEquals(lastValue,nextValue)) {
              if (lastValue != null) old.put(key, lastValue);
              if (nextValue != null) old.put(key, nextValue);
            }
          }
        }
      }
    }

    private boolean sameElements() {
      if (lastParts.size() != nextParts.size()) return false;
      for (int i = 0; i < lastParts.size(); ++i) {
        if (!lastParts.getElement(i).equals(nextParts.getElement(i))) return false;
      }
      return true;
    }

  }

  private static void summarizeCoverage(PrintWriter summary, PrintWriter samples2) {
    final String subdirectory = new File(MAIN_DIRECTORY).getName();
    final Factory cldrFactory = Factory.make(MAIN_DIRECTORY, FILES);
    final Factory collationFactory = Factory.make(COLLATION_DIRECTORY, FILES);
    final Factory rbnfFactory = Factory.make(RBNF_DIRECTORY, FILES);

    final XPathParts oldParts = new XPathParts();
    final XPathParts parts = new XPathParts();


    //    CLDRFile sd = CLDRFile.make(CLDRFile.SUPPLEMENTAL_NAME, CldrUtility.SUPPLEMENTAL_DIRECTORY, true);
    //    CLDRFile smd = CLDRFile.make(CLDRFile.SUPPLEMENTAL_METADATA, CldrUtility.SUPPLEMENTAL_DIRECTORY, true);
    //
    //    CoverageLevel.init(sd, smd);

    LanguageTagParser ltp = new LanguageTagParser();
    NumberFormat percent = NumberFormat.getPercentInstance();
    NumberFormat decimal = NumberFormat.getInstance();
    decimal.setGroupingUsed(true);
    decimal.setMaximumFractionDigits(2);
    percent.setMaximumFractionDigits(2);
    NumberFormat integer = NumberFormat.getIntegerInstance();
    Set<String> localesFound = new TreeSet<String>();
    
    // get list of locales
    LocaleLevelData mapLevelData = new LocaleLevelData();
    TreeSet<String> mainAvailableSource = new TreeSet<String>(cldrFactory.getAvailable());
    TreeSet<String> mainAvailable = new TreeSet();
    for (String locale : mainAvailableSource) {
      if (localeFilter.skipLocale(locale)) continue;
      mainAvailable.add(locale);
    }

    System.out.println("gathering rbnf data");
    Set<String> ordinals = new TreeSet<String>();
    Set<String> spellout = new TreeSet<String>();
    localesFound.clear();
    for (String locale : rbnfFactory.getAvailable()) {
      if (localeFilter.skipLocale(locale)) continue;
      System.out.println(locale + "\t" + english.getName(locale));
      getRBNFData(locale, rbnfFactory.make(locale, true), ordinals, spellout, localesFound);
    }
    markData("RBNF-Ordinals", ordinals, mapLevelData, mainAvailable, RBNF_LEVEL, RBNF_WEIGHT, Row.of("//ldml/rbnf/ordinals", "?"));
    markData("RBNF-Spellout", spellout, mapLevelData, mainAvailable, RBNF_LEVEL, RBNF_WEIGHT, Row.of("//ldml/rbnf/spellout", "?"));
    if (localesFound.size() != 0) {
      System.out.println("Other rbnf found:\t" + localesFound);
    }
    
    System.out.println("gathering plural data");
    PluralInfo rootPlurals = supplementalData.getPlurals("root");
    localesFound = new TreeSet(supplementalData.getPluralLocales());
    markData("Plurals", localesFound, mapLevelData, mainAvailable, PLURALS_LEVEL, PLURALS_WEIGHT, Row.of("//supplementalData/plurals", "UCA"));

    System.out.println("gathering collation data");
    localesFound.clear();
    for (String locale : collationFactory.getAvailable()) {
      if (localeFilter.skipLocale(locale)) continue;
      System.out.println(locale + "\t" + english.getName(locale));
      getCollationData(locale, collationFactory.make(locale, true), localesFound);
    }
    markData("Collation", localesFound, mapLevelData, mainAvailable, COLLATION_LEVEL, COLLATION_WEIGHT, Row.of("//ldml/collations", "UCA"));

    System.out.println("gathering main data");
    for (String locale : mainAvailable) {
      System.out.println(locale + "\t" + english.getName(locale));
      LevelData levelData = mapLevelData.get(locale);
      getMainData(locale, levelData, cldrFactory.make(locale, true));
    }

    System.out.println("printing data");
    for (String locale : mapLevelData.keySet()) {
      LevelData levelData = mapLevelData.get(locale);

      Counter<Level> missing = levelData.missing;
      Counter<Level> found = levelData.found;
      Relation<Level, R2<String, String>> samples = levelData.samples;

      // Now print the information
      samples2.println();
      samples2.println(locale + "\t" + english.getName(locale));
      double weightedFound = 0;
      double weightedMissing = 0;
      for (Level level : Level.values()) {
        if (level == Level.UNDETERMINED) {
          continue;
        }
        long missingCount = missing.get(level);
        long foundCount = found.get(level);
        samples2.println(level + "\tMissing:\t" + integer.format(missingCount) + "\tFound:\t" + integer.format(foundCount)
                + "\tScore:\t" + percent.format(foundCount/(double)(foundCount + missingCount))
                + "\tLevel-Value:\t" + level.getValue());
        Set<R2<String,String>> samplesAlready = samples.getAll(level);
        if (samplesAlready != null) {
          for (R2<String,String> row : samplesAlready) {
            samples2.println("\t" + row);
          }
          if (samplesAlready.size() >= SHOW_EXAMPLES) {
            samples2.println("\t...");
          }
        }
        weightedFound += foundCount * level.getValue();
        weightedMissing += missingCount * level.getValue(); 
      }
      int base = Level.POSIX.getValue();
      double foundCount = weightedFound/base;
      double missingCount = weightedMissing/base;        
      String summaryLine = "Weighted Missing:\t" + decimal.format(missingCount) + "\tFound:\t" + decimal.format(foundCount) + "\tScore:\t" + percent.format(foundCount/(double)(foundCount + missingCount));
      samples2.println(summaryLine);
      summary.println(locale + "\t" + english.getName(locale) + "\t" + summaryLine);
    }
  }

  private static void getRBNFData(String locale, CLDRFile cldrFile, Set<String> ordinals, Set<String> spellout, Set<String> others) {
    XPathParts parts = new XPathParts();
    for (String path : cldrFile) {
      if (path.endsWith("/alias")) {
        continue;
      }
      if (!path.contains("rulesetGrouping")) {
        continue;
      }
      if (SKIP_UNCONFIRMED && path.contains("unconfirmed")) {
        continue;
      }
      parts.set(path);
      String ruleSetGrouping = parts.getAttributeValue(2, "type");
      if (ruleSetGrouping.equals("SpelloutRules")) {
        spellout.add(locale);
      } else if (ruleSetGrouping.equals("OrdinalRules")) {
        ordinals.add(locale);
      } else {
        others.add(ruleSetGrouping);
      }  
    }
  }

  private static void markData(String title, Set<String> localesFound, LocaleLevelData mapLevelData, TreeSet<String> mainAvailable, Level level, long weight, R2<String, String> samples) {
    if (!mainAvailable.containsAll(localesFound)) {
      System.out.println(title + " Locales that are not in main: " + Builder.with(new TreeSet<String>())
              .addAll(localesFound).removeAll(mainAvailable)
              .filter(nonAliasLocaleFilter).get());
    }
    for (String locale : mainAvailable) {
      if (localesFound.contains(locale)) {
        mapLevelData.get(locale).found.add(level, weight);
      } else {
        System.out.println(locale + "\t" + english.getName(locale) + "\t" + "missing " + title);
        mapLevelData.get(locale).missing.add(level, weight);
        mapLevelData.get(locale).samples.put(level, samples);
      }
    }
  }

  private static class LocaleFilter implements Transform<String,Boolean> {
    LanguageTagParser ltp = new LanguageTagParser();
    final boolean checkAliases;
    
    public LocaleFilter(boolean checkAliases) {
      this.checkAliases = checkAliases;
    }
    private boolean skipLocale(String locale) {
      return !transform(locale);
    }
    public Boolean transform(String locale) {
      if (defaultContents.contains(locale)) return Boolean.FALSE;
      ltp.set(locale);
      if (ltp.getRegion().length() != 0 || !ltp.getVariants().isEmpty()) {
        // skip country locales
        return Boolean.FALSE;
      }
      if (checkAliases) {
        String language = ltp.getLanguage();
        if (languageAliasInfo.get(language) != null) {
          return Boolean.FALSE;
        }
      }
      return Boolean.TRUE;
    }
  }

  private static void getCollationData(String locale, CLDRFile cldrFile, Set<String> localesFound) {
    XPathParts parts = new XPathParts();
    for (String path : cldrFile) {
      if (path.endsWith("/alias")) {
        continue;
      }
      if (!path.contains("collations")) {
        continue;
      }
      if (SKIP_UNCONFIRMED && path.contains("unconfirmed")) {
        continue;
      }
      localesFound.add(locale);

      String fullPath = cldrFile.getFullXPath(path);
      if (fullPath == null) fullPath = path;
      try {
        parts.set(fullPath);
      } catch (RuntimeException e) {
        throw e;
      }
      String validSubLocales = parts.getAttributeValue(1, "validSubLocales");
      if (validSubLocales != null) {
        String[] sublocales = validSubLocales.split("\\s+");
        LanguageTagParser ltp = new LanguageTagParser();
        for (String sublocale : sublocales) {
          if (localeFilter.skipLocale(locale)) continue;
          localesFound.add(sublocale);
        }
      }
      break;
    }
  }

  private static void getMainData(String locale, LevelData levelData, CLDRFile cldrFile) {
    Status status = new Status();
    Set<String> sorted = Builder.with(new TreeSet<String>()).addAll(cldrFile.iterator()).addAll(cldrFile.getExtraPaths()).get();
    for (String path : sorted) {
      if (path.endsWith("/alias")) {
        continue;
      }

      String fullPath = cldrFile.getFullXPath(path);
      String source = cldrFile.getSourceLocaleID(path, status);
      Inheritance inherited = !source.equals(locale) || SKIP_UNCONFIRMED && path.contains("unconfirmed") ? Inheritance.inherited : Inheritance.actual;

      Level level = coverageLevel.getCoverageLevel(fullPath, path);
      if (inherited == Inheritance.actual) {
        levelData.found.add(level, 1);
      } else {
        levelData.missing.add(level, 1);
        if (SHOW_EXAMPLES > 0) {
          Set<R2<String,String>> samplesAlready = levelData.samples.getAll(level);
          if (samplesAlready == null || samplesAlready.size() < SHOW_EXAMPLES) {
            levelData.samples.put(level, Row.of(path, cldrFile.getStringValue(path)));
          }
        }
      }
    }
  }

  static class LevelData {
    Counter<Level> missing = new Counter<Level>();
    Relation<Level,R2<String,String>> samples = new Relation(new TreeMap(), LinkedHashSet.class);
    Counter<Level> found = new Counter<Level>();
  }

  static class LocaleLevelData {
    Map<String,LevelData> locale_levelData = new TreeMap<String,LevelData>();

    public LevelData get(String locale) {
      if (locale.equals("zh_Hans") || locale.equals("iw")) {
        throw new IllegalArgumentException();
      }
      LevelData result = locale_levelData.get(locale);
      if (result == null) {
        locale_levelData.put(locale, result = new LevelData());
      }
      return result;
    }

    public Set<String> keySet() {
      return locale_levelData.keySet();
    }
  }


}
