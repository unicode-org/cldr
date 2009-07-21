package org.unicode.cldr.unittest;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.text.UTF16;

public class TestCLDRFile {
  public static void main(String[] args) {
    testExtraPaths();
    TestTimeZonePath();
    testDraftFilter();
    simpleTest();
  }

  private static void testExtraPaths() {
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*", DraftStatus.approved);
    for (String locale : new String[]{"en", "ar", "ja"}) {
      CLDRFile cldrFile = cldrFactory.make(locale, true);
      Set<String> s = (Set<String>) cldrFile.getExtraPaths(new TreeSet<String>());
      System.out.println("Extras for " + locale);
      for (String path : s) {
        System.out.println(path + " => " + cldrFile.getStringValue(path));
      }
      System.out.println("Already in " + locale);
      for (Iterator<String> it = cldrFile.iterator(Pattern.compile(".*\\[@count=.*").matcher("")); it.hasNext();) {
        String path = it.next();
        System.out.println(path + " => " + cldrFile.getStringValue(path));
      }
    }
  }

  private static void testDraftFilter() {
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*", DraftStatus.approved);
    checkLocale(cldrFactory.make("root", true));
    checkLocale(cldrFactory.make("ee", true));
  }

  public static void checkLocale(CLDRFile cldr) {
    Matcher m = Pattern.compile("gregorian.*eras").matcher("");
    for (Iterator<String> it = cldr.iterator("", new UTF16.StringComparator()); it.hasNext();) {
      String path = it.next();
      if (m.reset(path).find() && !path.contains("alias")) {
        System.out.println(cldr.getLocaleID() + "\t" + cldr.getStringValue(path) + "\t" + cldr.getFullXPath(path));
      }
      if (path == null) {
        throw new IllegalArgumentException("Null path");
      }
      String fullPath = cldr.getFullXPath(path);
      if (fullPath.contains("@draft")) {
        throw new IllegalArgumentException("File can't contain draft elements");
      }
    }
  }
  
  public static void TestTimeZonePath() {
    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    String tz = "Pacific/Midway";
    CLDRFile cldrFile = cldrFactory.make("lv",true);
    String retVal = cldrFile.getStringValue(
        "//ldml/dates/timeZoneNames/zone[@type=\"" + tz + "\"]/exemplarCity"
        , true).trim();
    System.out.println(retVal);
  }

  private static void simpleTest() {
    double deltaTime = System.currentTimeMillis();    Factory cldrFactory = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    CLDRFile english = cldrFactory.make("en", true);
    deltaTime = System.currentTimeMillis() - deltaTime;
    System.out.println("Creation: Elapsed: " + deltaTime/1000.0 + " seconds");
    
    deltaTime = System.currentTimeMillis();
    english.getStringValue("//ldml");    
    deltaTime = System.currentTimeMillis() - deltaTime;
    System.out.println("Creation: Elapsed: " + deltaTime/1000.0 + " seconds");
    
    deltaTime = System.currentTimeMillis();
    english.getStringValue("//ldml");
    deltaTime = System.currentTimeMillis() - deltaTime;
    System.out.println("Caching: Elapsed: " + deltaTime/1000.0 + " seconds");
    
    deltaTime = System.currentTimeMillis();
    for (int j = 0; j < 2; ++j) {
      for (Iterator<String> it = english.iterator(); it.hasNext();) {
        String dpath = it.next();
        String value = english.getStringValue(dpath);
        Set<String> paths = english.getPathsWithValue(value, null, null, null);
        if (!paths.contains(dpath)) {
          throw new IllegalArgumentException(paths + " don't contain <" + value + ">.");
        }
        if (false && paths.size() > 1) {
          System.out.println("Value: " + value + "\t\tPaths: " + paths);
        }
      }
    }
    deltaTime = System.currentTimeMillis() - deltaTime;
    System.out.println("Elapsed: " + deltaTime/1000.0 + " seconds");
  }
}