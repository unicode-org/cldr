package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.CLDRFile.Status;

import com.ibm.icu.dev.test.TestFmwk;

public class TestPaths extends TestFmwk {
  static TestInfo testInfo = TestInfo.getInstance();

  public static void main(String[] args) {
    new TestPaths().run(args);
  }
  
  public void TestGetFullPath() {
    Status status = new Status();
    
    for (String locale : getLocalesToTest()) {
      CLDRFile file = testInfo.getCldrFactory().make(locale, true);
      logln(locale);

      for (Iterator<String> it = file.iterator(); it.hasNext();) {
        String path = it.next();
        String fullPath = file.getFullXPath(path);
        String value = file.getStringValue(path);
        String source = file.getSourceLocaleID(path, status);
        if (fullPath == null) {
          errln("Locale: " + locale + ",\t FullPath: " + path);
        }
        if (value == null) {
          errln("Locale: " + locale + ",\t Value: " + path);
        }
        if (source == null) {
          errln("Locale: " + locale + ",\t Source: " + path);
        }
        if (status.pathWhereFound == null) {
          errln("Locale: " + locale + ",\t Found Path: " + path);
        }
      }
    }
  }

  public void TestPretty() {
    PrettyPath prettyPath = new PrettyPath().setShowErrors(true);
    Set<String> pathsSeen = new HashSet<String>();
    
    for (String locale : getLocalesToTest()) {
      CLDRFile file = testInfo.getCldrFactory().make(locale, true);
      logln(locale);

      for (Iterator<String> it = file.iterator(); it.hasNext();) {
        checkPaths(it.next(), pathsSeen, prettyPath, locale);
      }
      for (String path : file.getExtraPaths()) {
        checkPaths(path, pathsSeen, prettyPath, locale);
      }
    }
  }

  private void checkPaths(String path, Set<String> pathsSeen, PrettyPath prettyPath, String locale) {
    if (path.endsWith("/alias")) {
      return;
    }
    if (pathsSeen.contains(path)) {
      return;
    }
    pathsSeen.add(path);
    String prettied = prettyPath.getPrettyPath(path, true);
    String unprettied = prettyPath.getOriginal(prettied);
    if (prettied.contains("%%")) { //  && !path.contains("/alias")
      errln(locale + "\t" + prettied + "\t" + path);
    } else if (!path.equals(unprettied)) {
      errln("Pretty Path doesn't roundtrip:\t" + path + "\t" + prettied + "\t" + unprettied);
    } else {
      //logln(prettied + "\t" + path);
    }
  }

  private Collection<String> getLocalesToTest() {
    return params.inclusion  < 5 ? Arrays.asList("root", "en", "ja", "ar")
            : params.inclusion  < 10 ? testInfo.getCldrFactory().getAvailableLanguages()
                    : testInfo.getCldrFactory().getAvailable();
  }
}
