package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility.SimpleLineComparator;

import com.ibm.icu.dev.test.util.BagFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PivotData {
  public static final boolean DEBUG = true;

  private static Matcher fileMatcher;
  
  public static void main(String[] args) throws IOException {
    System.out.println("WARNING: Must be done in 3 phases. -DPhase=1, then -DPhase=2, then -DPhase=3" + CldrUtility.LINE_SEPARATOR +
        "These are Lang+Script+Region, then Lang+Region, then Lang+Script" + CldrUtility.LINE_SEPARATOR +
    "Inspect and check-in after each phase");
    fileMatcher = Pattern.compile(CldrUtility.getProperty("FILE", ".*")).matcher("");
    int phase = Integer.parseInt(CldrUtility.getProperty("phase", null));
    Set<LocaleIDParser.Level> conditions = null;
    switch(phase) {
      case 1: conditions = EnumSet.of(LocaleIDParser.Level.Language, 
          LocaleIDParser.Level.Script,
          LocaleIDParser.Level.Region
      );
      break;
      case 2: conditions = EnumSet.of(LocaleIDParser.Level.Language, 
          LocaleIDParser.Level.Region
      );
      break;
      case 3: conditions = EnumSet.of(LocaleIDParser.Level.Language, 
          LocaleIDParser.Level.Script
      );
      break;
    }

    try {
      Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
      PivotData pd = new PivotData(cldrFactory, CldrUtility.MAIN_DIRECTORY, CldrUtility.GEN_DIRECTORY + "pivot/");
      pd.pivotGroup(cldrFactory, conditions);
    } finally {
      System.out.println("DONE");
    }
  }

  private void pivotGroup(Factory cldrFactory, Set<LocaleIDParser.Level> conditions) throws IOException {
    CLDRFile supplementalMetadata = cldrFactory.make("supplementalMetadata", false);
    if (false) for (Iterator it = supplementalMetadata.iterator(); it.hasNext();) {
      System.out.println(it.next());
    }
    String defaultContentList = supplementalMetadata.getFullXPath("//supplementalData/metadata/defaultContent", true);

    XPathParts parts = new XPathParts();
    parts.set(defaultContentList);
    String list = parts.getAttributeValue(-1, "locales");
    String[] items = list.split("\\s+");
    for (String item : items) {
      if (!fileMatcher.reset(item).matches()) {
        continue;
      }
      if (lidp.set(item).getLevels().equals(conditions)) {
        writePivot(item);
      }
    }
  }
  
  private LocaleIDParser lidp = new LocaleIDParser();
  
  private CLDRFile.Factory factory;
  private String outputDirectory;

  private String sourceDirectory;
  
  PivotData(CLDRFile.Factory factory, String sourceDirectory, String targetDirectory) {
    this.factory = factory;
    this.sourceDirectory = sourceDirectory;
    this.outputDirectory = targetDirectory;
  }
  
  /**
   * Move all of the contents in the localeID to the parent. Maintain, however,
   * the same nominal contents by moving all the replaced contents (inherited or
   * not) to the other children.
   * 
   * @param localeID
   *          to modify
   * @return Return list of locale IDs written out
   * @throws IOException 
   */
  public int writePivot(String localeID) throws IOException {
    int countChanges = 0;
    // first, find my parent and all of my siblings
    CLDRFile me = factory.make(localeID, false);
    if (me.getFullXPath("//ldml/alias",true) != null) {
      throw new IllegalArgumentException("File cannot be completely aliased: " + localeID);
    }
    String parentID = CLDRFile.getParent(localeID);
    
    if (DEBUG) System.out.format("LocaleID: %s, %s" + CldrUtility.LINE_SEPARATOR, localeID, parentID);
    
    // find all the unique paths that I have, where the value or fullpath is different from the parent.
    // AND the parent has the path
    
    Set<String> uniquePaths = new TreeSet<String>();
    CLDRFile resolvedParent = factory.make(parentID, true);
    if (resolvedParent.getFullXPath("//ldml/alias",true) != null) {
      throw new IllegalArgumentException("File cannot be completely aliased: " + localeID);
    }

    for (Iterator<String> it = me.iterator(); it.hasNext();) {
      String path = it.next();
      if (path.startsWith("//ldml/identity")) continue;
      String fullPath = me.getFullXPath(path);
      String value = me.getStringValue(path);
      String oldFullXPath = resolvedParent.getFullXPath(path);
      if (oldFullXPath == null) {
        uniquePaths.add(path);
        continue;
      }
      String oldValue = resolvedParent.getStringValue(path);
      Status status = new Status();
      if (!fullPath.equals(oldFullXPath) || !value.equals(oldValue)) {
        if (fullPath.contains("[@casing") != oldFullXPath.contains("[@casing")) {
          // only do if parent's value is "real"
          if (resolvedParent.getSourceLocaleID(path,status).equals(parentID)) {
            throw new IllegalArgumentException("Mismatched casing: " + localeID + ", " + parentID + " For:" + CldrUtility.LINE_SEPARATOR + fullPath + CldrUtility.LINE_SEPARATOR + oldFullXPath);
          }
        }
        uniquePaths.add(path);
      }
    }
    
    // if there are no unique paths our work here is done
    if (uniquePaths.size() == 0) {
      if (DEBUG) System.out.format("LocaleID: %s is EMPTY, no changes necessary" + CldrUtility.LINE_SEPARATOR, localeID);
      return countChanges;
    }
    
    // now find all the siblings. These are all the locales that have the same parent, and are one-level down
    Set<String> siblings = lidp.set(localeID).getSiblings(factory.getAvailable());
    siblings.remove(localeID); // remove myself
    
    if (DEBUG) System.out.format("Siblings: %s" + CldrUtility.LINE_SEPARATOR, siblings);
    
    // we now have a list of siblings. 
    // Create and write a new CLDRFile that is an empty me
    
    CLDRFile newFile = CLDRFile.make(localeID);
    writeFile(newFile);
    if (DEBUG) System.out.format("%s changes in: %s" + CldrUtility.LINE_SEPARATOR, uniquePaths.size(), localeID);
    
    // now add the different paths to the copy of the parent, and write out
    
    newFile = (CLDRFile) factory.make(parentID, false).cloneAsThawed();
    //System.out.println("parent " + size(CLDRFile.make(parentID).iterator()));
    //System.out.println("clone " + size(newFile.iterator()));
    int deltaChangeCount = addPathsAndValuesFrom(newFile, uniquePaths, me, true);
    countChanges += deltaChangeCount;
    if (DEBUG) System.out.format("%s changes in: %s" + CldrUtility.LINE_SEPARATOR, deltaChangeCount, parentID);
    writeFile(newFile);
    
    //  now add the parent's values for the paths to the siblings, and write out
    for (String id : siblings) {
      newFile = (CLDRFile) factory.make(id, false).cloneAsThawed();
      if (newFile.getFullXPath("//ldml/alias",true) != null) {
        System.out.println("Skipping completely aliased file: " + id);
        continue;
      }
      deltaChangeCount = addPathsAndValuesFrom(newFile, uniquePaths, resolvedParent, false);
      countChanges += deltaChangeCount;
      if (DEBUG) System.out.format("%s changes in: %s" + CldrUtility.LINE_SEPARATOR, deltaChangeCount, id);
      writeFile(newFile);
    }
    
    return countChanges;
  }


  private int size(Iterator name) {
    int count = 0;
    while (name.hasNext()) {
      Object x = name.next();
      count++;
    }
    return count;
  }

  private int addPathsAndValuesFrom(CLDRFile toModify, Set<String> uniquePaths, CLDRFile toAddFrom, boolean override) {
    int changeCount = 0;
    XPathParts parts = new XPathParts();
    for (String path : uniquePaths) {
      String fullPath = null, value = null, oldFullXPath = null, oldValue = null;
      fullPath = toAddFrom.getFullXPath(path);
      if (fullPath == null) continue;
      value = toAddFrom.getStringValue(path);
      oldFullXPath = toModify.getFullXPath(path);
      if (!override && oldFullXPath != null) {
        continue; // don't override unless specified
      }
      oldValue = toModify.getStringValue(path);
      try {
        if (!fullPath.equals(oldFullXPath) || !value.equals(oldValue)) {
          if (oldFullXPath != null && fullPath.contains("[@casing") != oldFullXPath.contains("[@casing")) {
            throw new IllegalArgumentException("Mismatched casing: " + toAddFrom.getLocaleID() + ", " + toModify.getLocaleID());
          }
          if (override && oldFullXPath != null) {
            Map<String,String> attributes = parts.set(oldFullXPath).getAttributes(-1);
            String alt = attributes.get("alt");
            if (alt == null) {
              attributes.put("alt", "proposed-x999");
            } else if (alt.contains("proposed")){
              attributes.put("alt", alt + "-x999");
            } else {
              attributes.put("alt", alt + "-proposed-x999");
            }
            oldFullXPath = parts.toString();
            toModify.add(oldFullXPath,oldValue);
          }
          toModify.add(fullPath,value);
          ++changeCount;
        }
      } catch (RuntimeException e) {
        throw e; // for debuggin
      }
    }
    return changeCount;
  }
  
  SimpleLineComparator lineComparer = new SimpleLineComparator(
      //SimpleLineComparator.SKIP_SPACES + 
      SimpleLineComparator.TRIM +
      SimpleLineComparator.SKIP_EMPTY +
      SimpleLineComparator.SKIP_CVS_TAGS);

  private void writeFile(CLDRFile newFile) throws IOException {
    String id = newFile.getLocaleID();
    PrintWriter out = BagFormatter.openUTF8Writer(outputDirectory, id + ".xml");
    newFile.write(out);
    out.println();
    out.close();
    CldrUtility.generateBat(sourceDirectory, id + ".xml", outputDirectory, id + ".xml", lineComparer);
  }

}