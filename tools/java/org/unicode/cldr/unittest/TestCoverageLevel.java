package org.unicode.cldr.unittest;

import org.unicode.cldr.test.CoverageLevel;
import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.util.CLDRFile;
import com.ibm.icu.dev.test.util.Relation;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.CLDRFile.Factory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCoverageLevel {
  
  private static CoverageLevel coverageLevel = new CoverageLevel();
  
  private static String fileMatcher;

  private static Matcher pathMatcher;

  private static int count = 0;

  public static void main(String[] args) throws IOException {

    fileMatcher = Utility.getProperty("FILE", ".*");
    //pathMatcher = Pattern.compile(getProperty("XMLPATH", ".*")).matcher("");

    double startTime = System.currentTimeMillis();
    Factory factory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, fileMatcher);
    Map options = new TreeMap();
    List possibleErrors = new ArrayList();
    Relation<Level, String> values = new Relation(new TreeMap(), TreeSet.class);
    for (String locale : factory.getAvailable()) {
      CLDRFile cldrFileToCheck = factory.make(locale,true);
      coverageLevel.setFile(cldrFileToCheck, options, null, possibleErrors);
      for (String path : cldrFileToCheck) {
        String fullPath = cldrFileToCheck.getFullXPath(path);
        Level level = coverageLevel.getCoverageLevel(fullPath);
        values.put(level, path);
      }
      System.out.println(values.keySet());
      for (Level level : values.keySet()) {
        System.out.println(level);
        for (String path : values.getAll(level)) {
          System.out.println("\t" + path);
        }
      }
    }

    double deltaTime = System.currentTimeMillis() - startTime;
    System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
    System.out.println("Instances found: " + count);
  }
}