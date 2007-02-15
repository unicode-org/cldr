package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.tool.UOption;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchCLDR {
  private static final int
  HELP1 = 0,
  HELP2 = 1,
  SOURCEDIR = 2,
  MATCH_FILE = 3,
  MATCH_PATH = 4,
  MATCH_VALUE = 5
  ;
  private static final UOption[] options = {
    UOption.HELP_H(),
    UOption.HELP_QUESTION_MARK(),
    UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY),
    UOption.create("localematch", 'l', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("pathmatch", 'p', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("valuematch", 'v', UOption.REQUIRES_ARG).setDefault(".*"),
  };
  static final String HELP_TEXT1 = "Use the following options" + XPathParts.NEWLINE
  + "-h or -?\t for this message" + XPathParts.NEWLINE
  + "-"+options[SOURCEDIR].shortName + "\t source directory. Default = -s" + Utility.getCanonicalName(Utility.MAIN_DIRECTORY) + XPathParts.NEWLINE
    + "\tExample:-sC:\\Unicode-CVS2\\cldr\\common\\gen\\source\\" + XPathParts.NEWLINE
    + "-l<regex>\t to restrict the locales to what matches <regex>" + XPathParts.NEWLINE
    + "-p<regex>\t to restrict the paths to what matches <regex>" + XPathParts.NEWLINE
    + "-v<regex>\t to restrict the values to what matches <regex>" + XPathParts.NEWLINE
    + "\t Remember to put .* on the front and back of any regex if you want to find any occurence."
;
  
  public static void main(String[] args) {
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
      System.out.println("File Matching: " + options[MATCH_FILE].value);
      new Utility.MatcherFilter(options[MATCH_FILE].value).retainAll(locales);
    }
    if (!options[MATCH_PATH].value.equals(".*")) {
      System.out.println("Path Matching: " + options[MATCH_PATH].value);
      pathMatch = Pattern.compile(options[MATCH_PATH].value).matcher("");
    }
    if (!options[MATCH_VALUE].value.equals(".*")) {
      System.out.println("Value Matching: " + options[MATCH_VALUE].value);
      valueMatch = Pattern.compile(options[MATCH_VALUE].value).matcher("");
    }
    System.out.println("Searching...");
    System.out.println();
    System.out.flush();
    PrettyPath pretty = new PrettyPath();
    
    for (String locale : locales) {
      CLDRFile file = (CLDRFile) cldrFactory.make(locale, false);
      //System.out.println("*Checking " + locale);
      int count = 0;
      for (Iterator<String> it = file.iterator("",CLDRFile.ldmlComparator); it.hasNext();) {
        String path = it.next();
        String fullPath = file.getFullXPath(path);
        String value = file.getStringValue(path);
        if (pathMatch != null && !pathMatch.reset(fullPath).matches()) continue;
        if (valueMatch != null && !valueMatch.reset(value).matches()) continue;
        String shortPath = pretty.getPrettyPath(path);
        System.out.println(locale + "\t" + shortPath + "\t{" + value + "}"  + "\t" + fullPath);
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
}