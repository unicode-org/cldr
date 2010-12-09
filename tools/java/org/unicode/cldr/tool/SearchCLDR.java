package org.unicode.cldr.tool;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.tool.UOption;

public class SearchCLDR {
  private static final int
  HELP1 = 0,
  HELP2 = 1,
  SOURCEDIR = 2,
  MATCH_FILE = 3,
  MATCH_PATH = 4,
  MATCH_VALUE = 5,
  SHOW_PATH = 6,
  SHOW_PARENT_VALUE = 7,
  SHOW_ENGLISH_VALUE = 8
  ;
  private static final UOption[] options = {
    UOption.HELP_H(),
    UOption.HELP_QUESTION_MARK(),
    UOption.SOURCEDIR().setDefault(CldrUtility.MAIN_DIRECTORY),
    UOption.create("localematch", 'l', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("pathmatch", 'p', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("valuematch", 'v', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("showPath", 'z', UOption.NO_ARG),
    UOption.create("showParentValue", 'q', UOption.NO_ARG),
    UOption.create("showEnglishValue", 'e', UOption.NO_ARG),
  };
  static final String HELP_TEXT1 = "Use the following options" + XPathParts.NEWLINE
  + "-h or -?\t for this message" + XPathParts.NEWLINE
  + "-"+options[SOURCEDIR].shortName + "\t source directory. Default = -s" + CldrUtility.getCanonicalName(CldrUtility.MAIN_DIRECTORY) + XPathParts.NEWLINE
    + "\tExample:-sC:\\Unicode-CVS2\\cldr\\common\\gen\\source\\" + XPathParts.NEWLINE
    + "-l<regex>\t to restrict the locales to what matches <regex>" + XPathParts.NEWLINE
    + "-p<regex>\t to restrict the paths to what matches <regex>" + XPathParts.NEWLINE
    + "-v<regex>\t to restrict the values to what matches <regex>" + XPathParts.NEWLINE
    + "\t Remember to put .* on the front and back of any regex if you want to find any occurence."
    + "-s\t show path"
    + "-s\t show parent value"
    + "-s\t show English value"
;
  
  public static void main(String[] args) {
    System.out.println("Arguments: " + CollectionUtilities.join(args, " "));

    long startTime = System.currentTimeMillis();
    UOption.parseArgs(args, options);
    if (options[HELP1].doesOccur || options[HELP2].doesOccur) {
      System.out.println(HELP_TEXT1);
      return;
    }
    Factory cldrFactory = Factory.make(options[SOURCEDIR].value, ".*");
    Set<String> locales = new TreeSet(cldrFactory.getAvailable());
    
    Matcher pathMatch = null;
    Matcher valueMatch = null;
    if (!options[MATCH_FILE].value.equals(".*")) {
      System.out.println("-l File Matching: " + options[MATCH_FILE].value);
      new CldrUtility.MatcherFilter(options[MATCH_FILE].value).retainAll(locales);
    }
    if (!options[MATCH_PATH].value.equals(".*")) {
      System.out.println("-p Path Matching: " + options[MATCH_PATH].value);
      pathMatch = Pattern.compile(options[MATCH_PATH].value).matcher("");
    }
    if (!options[MATCH_VALUE].value.equals(".*")) {
      System.out.println("-v Value Matching: " + options[MATCH_VALUE].value);
      valueMatch = Pattern.compile(options[MATCH_VALUE].value).matcher("");
    }
    boolean showPath = options[SHOW_PATH].doesOccur;
    System.out.println("-z Show Path: " + showPath);

    boolean showParent = options[SHOW_PARENT_VALUE].doesOccur;
    System.out.println("-q Show Parent Value: " + showParent);

    boolean showEnglish = options[SHOW_ENGLISH_VALUE].doesOccur;
    System.out.println("-e Show English Value: " + showEnglish);

    
    CLDRFile english = null;
    if (showEnglish) {
      english = cldrFactory.make("en", true);
    }
    
    System.out.println("Searching...");
    System.out.println();
    System.out.flush();
    PrettyPath pretty = new PrettyPath();
    
    for (String locale : locales) {
      CLDRFile file = (CLDRFile) cldrFactory.make(locale, false);
      CLDRFile parent = null;
      boolean headerShown = false;
      
      //System.out.println("*Checking " + locale);
      int count = 0;
      for (Iterator<String> it = file.iterator("",CLDRFile.ldmlComparator); it.hasNext();) {
        String path = it.next();
        String fullPath = file.getFullXPath(path);
        String value = file.getStringValue(path);
        if (pathMatch != null && !pathMatch.reset(fullPath).find()) continue;
        if (valueMatch != null && !valueMatch.reset(value).find()) continue;
        
        // made it through the sieve
        if (!headerShown) {
          showLine(showPath, showParent, showEnglish, locale, "Path", "Full-Path", "Value", "ShortPath", "Parent-Value", "English-Value");
          headerShown = true;
        }
        if (showParent && parent == null) {
          String parentLocale = CLDRFile.getParent(locale);
          parent = cldrFactory.make(parentLocale, true);
        }
        String shortPath = pretty.getPrettyPath(path);
        String cleanShort = pretty.getOutputForm(shortPath);
        showLine(showPath, showParent, showEnglish, locale, path, fullPath, value, cleanShort, 
                parent == null ? null : parent.getStringValue(path), 
                        english == null ? null : english.getStringValue(path));
        count++;
      }
      if (count != 0) {
        System.out.println("*Found:\t" + count);
        System.out.println();
        System.out.flush();
      }
    }
    System.out.println("Done -- Elapsed time: " + ((System.currentTimeMillis() - startTime)/60000.0) + " minutes");
  }

  private static void showLine(boolean showPath, boolean showParent, boolean showEnglish,
          String locale, String path, String fullPath, String value, String shortPath, String parentValue, String englishValue) {
    System.out.println(locale + "\t{" + value + "}" 
            + (showParent ? "\t{" + parentValue + "}" : "")
            + (showEnglish ? "\t{" + englishValue + "}" : "")
            + "\t" + shortPath
            + (showPath ?"\t" + fullPath : "")
    );
  }
}