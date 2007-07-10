package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;

import java.util.Iterator;
import java.util.Set;

public class TestCLDRFile {
  public static void main(String[] args) {
    double deltaTime = System.currentTimeMillis();
    Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, ".*");
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