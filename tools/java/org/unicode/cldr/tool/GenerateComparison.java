package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.Utility;

import org.unicode.cldr.util.Row;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Row.R2;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;

public class GenerateComparison {

  public static void main(String[] args) throws IOException {

    // Setup
    Timer timer = new Timer();
    Timer totalTimer = new Timer();
    long totalPaths = 0;
    NumberFormat format = NumberFormat.getNumberInstance();
    format.setGroupingUsed(true);
    Collator collator = Collator.getInstance();
    Map<String,String> deleted = new TreeMap(collator);
    Map<String,String> added = new TreeMap(collator);
    Map<String,String> changed = new TreeMap(collator);
    Map<String,String> rejected = new TreeMap(collator);
    int totalRejected= 0;
    int totalDeleted = 0;
    int totalAdded = 0;
    int totalChanged = 0;

    // Get the args

    String oldDirectory = Utility.getProperty("oldDirectory", new File(Utility.BASE_DIRECTORY,
    "../../common-cldr1.6/main/").getCanonicalPath());
    String newDirectory = Utility.getProperty("newDirectory", new File(Utility.BASE_DIRECTORY,
    "incoming/proposed/main/").getCanonicalPath());
    String filter = Utility.getProperty("localeFilter", ".*");
    boolean SHOW_ALIASED = Utility.getProperty("showAliased", "false").toLowerCase().startsWith("t");

    // Create the factories

    Factory oldFactory = Factory.make(oldDirectory, filter);
    Factory newFactory = Factory.make(newDirectory, filter);
    CLDRFile english = newFactory.make("en", true);

    // Get the union of all the language locales, sorted by English name

    Set<String> oldList = oldFactory.getAvailableLanguages();
    Set<String> newList = newFactory.getAvailableLanguages();
    Set<String> unifiedList = new HashSet<String>(oldList);
    unifiedList.addAll(newList);
    Set<R2<String, String>> pairs = new TreeSet<R2<String, String>>();
    for (String code : unifiedList) {
      pairs.add(Row.make(english.getName(code), code));
    }

    PrettyPath prettyPathMaker = new PrettyPath();
    int totalDifferences = 0;
    int differences = 0;

    // iterate through those
    for (R2<String, String> pair : pairs) {
      timer.start();
      final String locale = pair.get1();
      differences = 0;
      System.out.println();

      // Create CLDR files for both; null if can't open

      CLDRFile oldFile = null;
      try {
        oldFile = oldFactory.make(locale, true, true);
      } catch (Exception e) {
      }
      CLDRFile newFile = null;
      try {
        newFile = newFactory.make(locale, true, true);
      } catch (Exception e) {
      }

      // Check for null cases

      final String localeName = pair.get0();
      if (oldFile == null) {
        System.out.println("*** NEW *** " + localeName + "\t" + locale);
        System.out.println();
        continue;
      } else if (newFile == null) {
        System.out.println("*** DELETED *** " + localeName + "\t" + locale);
        System.out.println();
        continue;
      }
      System.out.println("*** " + localeName + "\t" + locale);
      System.out.println();

      // Get the union of all the paths

      Set<String> paths;
      try {
        paths = new HashSet<String>();
        CollectionUtilities.addAll(oldFile.iterator(), paths);
        paths.addAll(oldFile.getExtraPaths());
        CollectionUtilities.addAll(newFile.iterator(), paths);
        paths.addAll(newFile.getExtraPaths());
      } catch (Exception e) {
        System.out.println("Error in Reading Files");
        continue;
      }

      // We now have the full set of all the paths for old and new files
      // TODO Sort by the pretty form
      // Set<R2<String,String>> pathPairs = new TreeSet();
      // for (String code : unifiedList) {
      // pairs.add(Row.make(code, english.getName(code)));
      // }
      
      // Initialize sets
      deleted.clear();
      added.clear();
      changed.clear();
      rejected.clear();
      
      for (String path : paths) {
        if (path.contains("/alias")) {
          continue;
        }
        String oldString = oldFile.getStringValue(path);
        String newString = newFile.getStringValue(path);

        // for debugging
        if (oldString != null && oldString.contains("{1} {0}")) {
          System.out.print("");
        }

        if (equals(newString, oldString)) {
          continue;
        }
        Status oldStatus = new Status();
        String oldLocale = oldFile.getSourceLocaleID(path, oldStatus);
        
        Status newStatus = new Status();
        String newLocale = newFile.getSourceLocaleID(path, newStatus);

        // At this point, we have two unequal values
        // TODO check for non-distinguishing attribute value differences

        if (!SHOW_ALIASED) {
          
          // Skip deletions of alt-proposed
          
          if (newString == null) {
            if (path.contains("@alt=\"proposed")) {
              continue;
            }
          }
          
          // Skip if both inherited from the same locale, since we should catch it
          // in that locale.
          
          if (!oldLocale.equals(locale) && newLocale.equals(oldLocale)) {
            continue;
          }

          // Now check aliases
          
          final boolean newIsAlias = !newStatus.pathWhereFound.equals(path);
          if (newIsAlias) { // new is alias
            // filter out cases of a new string that is found via alias
            if (oldString == null) {
              continue;
            }
            
            // we filter out cases where both alias to the same thing, and not
            // this path
            
            if (newStatus.pathWhereFound.equals(oldStatus.pathWhereFound)) {
              continue;
            }
          }
        }

        // We definitely have a difference worth recording, so do so
        
        String newFullPath = newFile.getFullXPath(path);
        final boolean reject = newFullPath != null && newFullPath.contains("@draft") && !newFullPath.contains("@draft=\"contributed\"");
        final String pretty = prettyPathMaker.getPrettyPath(path, false);
        final String line = locale + "\t" + (reject ? "X" : "-") +"\t\u200E<" + oldString + ">\u200E\t\u200E<" + newString + ">\u200E\t" + pretty;
        if (reject) {
          rejected.put(pretty, line);
          totalRejected++;
        } else if (newString == null) {
          deleted.put(pretty, line);
          totalDeleted++;
        } else if (oldString == null) {
          added.put(pretty, line);
          totalAdded++;
        } else {
          changed.put(pretty, line);
          totalChanged++;
        }

        totalDifferences++;
        differences++;
      }
      showSet(changed, locale, "Changed");
      showSet(added, locale, "Added");
      showSet(deleted, locale, "Deleted");
      showSet(rejected, locale, "!REJECTED!");
      
      System.out.println(locale + "\tDifferences:\t" + format.format(differences)
              + "\tPaths:\t" + format.format(paths.size())
              + "\tTime:\t" + timer.getDuration() + "ms");
      totalPaths += paths.size();
    }
    
    System.out.println();
    System.out.println("Total Changed:\t" + format.format(totalChanged));
    System.out.println("Total Added:\t" + format.format(totalAdded));
    System.out.println("Total Deleted:\t" + format.format(totalDeleted));
    System.out.println("Total Rejected:\t" + format.format(totalRejected));

    System.out.println("Total Differences:\t" + format.format(totalDifferences) 
            + "\tPaths:\t" + format.format(totalPaths)
            + "\tTotal Time:\t" + format.format(totalTimer.getDuration()) + "ms");
  }

  private static void showSet(Map<String, String> rejected, final String locale, String title) {
    if (rejected.size() != 0) {
      System.out.println();
      System.out.println(locale + "\t" + title + "\t" + rejected.size());
      for (String line : rejected.keySet()) {
        System.out.println(rejected.get(line));
      }
    }
  }

  private static boolean equals(String newString, String oldString) {
    if (newString == null) {
      return oldString == null;
    }
    return newString.equals(oldString);
  }
}
