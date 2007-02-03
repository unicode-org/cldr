package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple test that loads each file in the cldr directory, thus verifying that
 * the DTD works, and also checks that the PrettyPaths work.
 * 
 * @author markdavis
 */
public class QuickCheck {
  public static void main(String[] args) {
    double deltaTime = System.currentTimeMillis();
    checkPaths();
    deltaTime = System.currentTimeMillis() - deltaTime;
    System.out.println("Elapsed: " + deltaTime/1000.0 + " seconds");
    System.out.println("Done");
  }

  static Matcher skipPaths = Pattern.compile("/identity" + "|/alias" + "|\\[@alt=\"proposed").matcher("");

  private static void checkPaths() {
    Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
    Relation<String, String> pathToLocale = new Relation(new TreeMap(CLDRFile.ldmlComparator), TreeSet.class, null);
    for (String locale : cldrFactory.getAvailable()) {
      if (locale.equals("root"))
        continue;
      CLDRFile file = cldrFactory.make(locale, false);
      if (file.isNonInheriting())
        continue;
      System.out.println(locale);
      for (Iterator<String> it = file.iterator(); it.hasNext();) {
        pathToLocale.put(it.next(), locale);
      }
    }
    PrettyPath prettyPath = new PrettyPath().setShowErrors(true);
    for (String path : pathToLocale.keySet()) {
      System.out.println(path + "\t" + prettyPath.getPrettyPath(path, false));
    }
    // now remove root
    CLDRFile root = cldrFactory.make("root", true);
    for (Iterator<String> it = root.iterator(); it.hasNext();) {
      pathToLocale.remove(it.next());
    }
    for (String path : pathToLocale.keySet()) {
      if (skipPaths.reset(path).find()) {
        continue;
      }
      System.out.println(path + "\t" + pathToLocale.getAll(path));
    }

  }

}