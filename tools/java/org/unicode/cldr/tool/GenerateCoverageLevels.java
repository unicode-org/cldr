package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.NumberFormat;

public class GenerateCoverageLevels {
  private static int SHOW_EXAMPLES = 5;
  private static final String FILES = ".*";
  private static final String MAIN_DIRECTORY = CldrUtility.MAIN_DIRECTORY;//CldrUtility.SUPPLEMENTAL_DIRECTORY; //CldrUtility.MAIN_DIRECTORY;
  private static final String COLLATION_DIRECTORY = CldrUtility.BASE_DIRECTORY + "/collation/";//CldrUtility.SUPPLEMENTAL_DIRECTORY; //CldrUtility.MAIN_DIRECTORY;
  private static final String RBNF_DIRECTORY = CldrUtility.BASE_DIRECTORY + "/rbnf/";//CldrUtility.SUPPLEMENTAL_DIRECTORY; //CldrUtility.MAIN_DIRECTORY;
  private static final String OUT_DIRECTORY = CldrUtility.GEN_DIRECTORY + "/coverage/"; //CldrUtility.MAIN_DIRECTORY;
  private static final Factory cldrFactory = Factory.make(MAIN_DIRECTORY, FILES);
  private static final CLDRFile english = cldrFactory.make("en", true);
  private static final CoverageLevel coverageLevel = new CoverageLevel();
  static {
    coverageLevel.setFile(english, new TreeMap(), null, new ArrayList());
  }

  enum Inheritance {actual, inherited}
  
  public static void main(String[] args) throws IOException {
    System.out.println("*** TODO check collations, RBNF, Transforms (if non-Latin)");
    PrintWriter summary = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "summary.txt");
    PrintWriter samples = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "samples.txt");
    summarizeCoverage(summary, samples);
    summary.close();

    PrintWriter out = BagFormatter.openUTF8Writer(OUT_DIRECTORY, "fullpaths.txt");
    showEnglish(out);
    out.close();
  }

  private static void showEnglish(PrintWriter out) throws IOException {
    CLDRFile cldrFile = english;
    String locale = "en";
    Set<String> sorted = Builder.with(new TreeSet<String>()).addAll(cldrFile.iterator()).addAll(cldrFile.getExtraPaths()).get();
    Set<R3<Level,String,Inheritance>> items = new TreeSet<R3<Level,String,Inheritance>>();
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
    for (R3<Level,String,Inheritance> item : items) {
      Level level = item.get0();
      String path = item.get1();
      Inheritance inherited = item.get2();
      out.println(level.ordinal() + "\t" + level + "\t" + inherited + "\t" + cldrFile.getStringValue(path) + "\t" + path);
    }
  }

  private static void summarizeCoverage(PrintWriter summary, PrintWriter samples2) {
    final String subdirectory = new File(MAIN_DIRECTORY).getName();
    final Factory cldrFactory = Factory.make(MAIN_DIRECTORY, FILES);

    final Set<String> locales = new TreeSet<String>(cldrFactory.getAvailable());
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
    NumberFormat integer = NumberFormat.getIntegerInstance();

    for (String locale : locales) {
      ltp.set(locale);
      if (ltp.getRegion().length() != 0 || !ltp.getVariants().isEmpty()) {
        // skip country locales
        continue;
      }

      Counter<Level> missing = new Counter<Level>();
      Relation<Level,R2<String,String>> samples = new Relation(new TreeMap(), LinkedHashSet.class);
      Counter<Level> found = new Counter<Level>();
      CLDRFile cldrFile = cldrFactory.make(locale, true);
      Set<String> sorted = Builder.with(new TreeSet<String>()).addAll(cldrFile.iterator()).addAll(cldrFile.getExtraPaths()).get();
      for (String path : sorted) {
        if (path.endsWith("/alias")) {
          continue;
        }

        String fullPath = cldrFile.getFullXPath(path);
        String source = cldrFile.getSourceLocaleID(path, null);
        Inheritance inherited = !source.equals(locale) ? Inheritance.inherited : Inheritance.actual;

        Level level = coverageLevel.getCoverageLevel(fullPath, path);
        if (inherited == Inheritance.actual) {
          found.add(level, 1);
        } else {
          missing.add(level, 1);
          if (SHOW_EXAMPLES > 0) {
            Set<R2<String,String>> samplesAlready = samples.getAll(level);
            if (samplesAlready == null || samplesAlready.size() < SHOW_EXAMPLES) {
              samples.put(level, Row.of(path, cldrFile.getStringValue(path)));
            }
          }
        }
      }
      samples2.println();
      samples2.println(locale + "\t" + english.getName(locale));
      System.out.println(locale + "\t" + english.getName(locale));
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
}
