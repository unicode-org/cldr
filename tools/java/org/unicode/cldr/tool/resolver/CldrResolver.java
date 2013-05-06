/*
 * Copyright (C) 2004-2011, Unicode, Inc., Google, Inc., and others.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 */
package org.unicode.cldr.tool.resolver;

import com.ibm.icu.dev.tool.UOption;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LruMap;
import org.unicode.cldr.util.SimpleXMLSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class designed for the resolution of CLDR XML Files (e.g., removing aliases
 * but leaving the inheritance structure intact).
 * 
 * Instances of this class are not thread-safe. Any attempts to use an object of
 * this class in multiple threads must be externally synchronized.
 * 
 * @author ryanmentley@google.com (Ryan Mentley), jchye@google.com (Jennifer Chye)
 * 
 */
public class CldrResolver {
  /**
   * 
   */
  public static final String UNDEFINED = "�UNDEFINED�";
  /**
   * The name of the code-fallback locale
   */
  public static final String CODE_FALLBACK = "code-fallback";

  /**
   * The name of the root locale
   */
  public static final String ROOT = "root";

  /* The command-line UOptions, along with a storage container. */
  private static final UOption LOCALE = UOption.create("locale", 'l', UOption.REQUIRES_ARG);
  private static final UOption DESTDIR = UOption.DESTDIR();
  private static final UOption SOURCEDIR = UOption.SOURCEDIR();
  private static final UOption RESOLUTION_TYPE = UOption.create("resolutiontype", 'r',
      UOption.REQUIRES_ARG);
  private static final UOption DRAFT_STATUS = UOption.create("mindraftstatus", 'm',
      UOption.REQUIRES_ARG);
  private static final UOption VERBOSITY = UOption.create("verbosity", 'v', UOption.REQUIRES_ARG);
  private static final UOption[] OPTIONS = {LOCALE, DESTDIR, SOURCEDIR, RESOLUTION_TYPE,
      DRAFT_STATUS, VERBOSITY};

  /* Private instance variables */
  private Factory cldrFactory;
  private ResolutionType resolutionType;
  // Cache for resolved CLDRFiles.
  // This is most useful for simple resolution, where the resolved locales are
  // required to resolve their children.
  private Map<String, CLDRFile> resolvedCache = new LruMap<String, CLDRFile>(5);

  public static void main(String[] args) {
    UOption.parseArgs(args, OPTIONS);

    // Defaults
    ResolutionType resolutionType = ResolutionType.SIMPLE;
    String localeRegex = ".*";
    String srcDir = CldrUtility.MAIN_DIRECTORY;
    File dest = new File(CldrUtility.GEN_DIRECTORY, "resolver");
    if (!dest.exists()) {
        dest.mkdir();
    }
    String destDir = dest.getAbsolutePath();
    
    // Parse the options
    if (RESOLUTION_TYPE.doesOccur) {
      try {
        resolutionType = ResolutionType.forString(RESOLUTION_TYPE.value);
      } catch (IllegalArgumentException e) {
        ResolverUtils.debugPrintln("Warning: " + e.getMessage(), 1);
        ResolverUtils.debugPrintln("Using default resolution type " + resolutionType.toString(), 1);
      }
    }

    if (LOCALE.doesOccur) {
      localeRegex = LOCALE.value;
    }

    if (SOURCEDIR.doesOccur) {
      srcDir = SOURCEDIR.value;
    }

    if (DESTDIR.doesOccur) {
      destDir = DESTDIR.value;
    }

    if (VERBOSITY.doesOccur) {
      int verbosityParsed;
      try {
        verbosityParsed = Integer.parseInt(VERBOSITY.value);
      } catch (NumberFormatException e) {
        ResolverUtils.debugPrintln("Warning: Error parsing verbosity value \"" + VERBOSITY.value
            + "\".  Using default value " + ResolverUtils.verbosity, 1);
        verbosityParsed = ResolverUtils.verbosity;
      }

      if (verbosityParsed < 0 || verbosityParsed > 5) {
        ResolverUtils.debugPrintln(
            "Warning: Verbosity must be between 0 and 5, inclusive.  Using default value "
                + ResolverUtils.verbosity, 1);
      } else {
        ResolverUtils.verbosity = verbosityParsed;
      }
    }

    if (srcDir == null) {
      ResolverUtils.debugPrintln(
          "Error: a source (CLDR common/main) directory must be specified via either"
              + " the -s command-line option or by the CLDR_DIR environment variable.", 1);
      System.exit(1);
    }

    DraftStatus minDraftStatus = DRAFT_STATUS.doesOccur ? DraftStatus.forString(DRAFT_STATUS.value) : DraftStatus.unconfirmed;
    CldrResolver resolver = new CldrResolver(srcDir, resolutionType, minDraftStatus);

    // Print out the options other than draft status (which has already been
    // printed)
    ResolverUtils.debugPrintln("Locale regular expression: \"" + localeRegex + "\"", 2);
    ResolverUtils.debugPrintln("Source (CLDR common/main) directory: \"" + srcDir + "\"", 2);
    ResolverUtils.debugPrintln("Destination (resolved output) directory: \"" + destDir + "\"", 2);
    ResolverUtils.debugPrintln("Resolution type: " + resolutionType.toString(), 2);
    ResolverUtils.debugPrintln("Verbosity: " + ResolverUtils.verbosity, 2);

    // Perform the resolution
    resolver.resolve(localeRegex, destDir);
    ResolverUtils.debugPrintln("Execution complete.", 3);
  }

  /**
   * Constructs a CLDR partial resolver given the path to a directory of XML
   * files.
   * 
   * @param cldrDirectory the path to the common/main folder from the standard
   *        CLDR distribution. Note: this still requires the CLDR_DIR
   *        environment variable to be set.
   * @param resolutionType the resolution type of the resolver.
   */
  public CldrResolver(String cldrDirectory, ResolutionType resolutionType) {
    this(cldrDirectory, resolutionType, DraftStatus.unconfirmed);
  }

  public CldrResolver(String cldrDirectory, ResolutionType resolutionType, DraftStatus minimumDraftStatus) {
    /*
     * We don't do the regex filter here so that we can still resolve parent
     * files that don't match the regex
     */
    cldrFactory = Factory.make(cldrDirectory, ".*", minimumDraftStatus);
    this.resolutionType = resolutionType;
  }

  /**
   * Resolves all locales that match the given regular expression and outputs
   * their XML files to the given directory.
   * 
   * @param localeRegex a regular expression that will be matched against the
   *        names of locales
   * @param outputDir the directory to which to output the partially-resolved
   *        XML files
   * @param resolutionType the type of resolution to perform
   * @throws IllegalArgumentException if outputDir is not a directory
   */
  public void resolve(String localeRegex, File outputDir) {
    if (!outputDir.isDirectory()) {
      throw new IllegalArgumentException(outputDir.getPath() + " is not a directory");
    }

    // Iterate through all the locales
    for (String locale : getLocaleNames(localeRegex)) {
      // Resolve the file
      ResolverUtils.debugPrintln("Processing locale " + locale + "...", 2);
      CLDRFile resolved = resolveLocale(locale);

      // Output the file to disk
      printToFile(resolved, outputDir);
    }
  }

  /**
   * Returns the locale names from the resolver that match a given regular
   * expression.
   * 
   * @param localeRegex a regular expression to match against
   * @return all of the locales that will be resolved by a call to resolve()
   *         with the same localeRegex
   */
  public Set<String> getLocaleNames(String localeRegex) {
    ResolverUtils.debugPrint("Getting list of locales...", 3);
    Set<String> allLocales = cldrFactory.getAvailable();
    Set<String> locales = new TreeSet<String>();
    // Iterate through all the locales
    for (String locale : allLocales) {
      // Check if the locale name matches the regex
      if (locale.matches(localeRegex)) {
        locales.add(locale);
      } else {
        ResolverUtils.debugPrintln("Locale " + locale
            + "does not match the pattern.  Skipping...\n", 4);
      }

    }
    ResolverUtils.debugPrintln("done.\n", 3);
    return locales;
  }

  /**
   * Accessor method for the CLDR factory.  Used for testing.
   * 
   * @return the {@link org.unicode.cldr.util.CLDRFile.Factory} used to resolve the CLDR data 
   */
  public Factory getFactory() {
    return cldrFactory;
  }

  /**
   * Resolves a locale to a {@link CLDRFile} object
   * 
   * @param locale the name of the locale to resolve
   * @param resolutionType the type of resolution to perform
   * @return a {@link CLDRFile} containing the resolved data
   */
  public CLDRFile resolveLocale(String locale) {
    // Create CLDRFile for current (base) locale
    CLDRFile base = cldrFactory.make(locale, true);
    CLDRFile resolved = resolvedCache.get(locale);
    if (resolved != null) return resolved;
    
    ResolverUtils.debugPrintln("Processing " + locale + "...", 2);
    resolved = resolveLocaleInternal(base, resolutionType);
    resolvedCache.put(locale, resolved);
    return resolved;
  }

  private CLDRFile resolveLocaleInternal(CLDRFile file, ResolutionType resolutionType) {
      String locale = file.getLocaleID();
      // Make parent files for simple resolution.
      List<CLDRFile> ancestors = new ArrayList<CLDRFile>();
      if (resolutionType == ResolutionType.SIMPLE && !locale.equals(ROOT)) {
          String parentLocale = locale;
          do {
              parentLocale = LocaleIDParser.getSimpleParent(parentLocale);
              ancestors.add(resolveLocale(parentLocale));
          } while (!parentLocale.equals(ROOT));
      }

      // Create empty file to hold (partially or fully) resolved data.
      CLDRFile resolved = new CLDRFile(new SimpleXMLSource(locale));

      // Go through the XPaths, filter out appropriate values based on the
      // inheritance model,
      // then copy to the new CLDRFile.
      Set<String> basePaths = ResolverUtils.getAllPaths(file);
      for (String distinguishedPath : basePaths) {
          ResolverUtils.debugPrintln("Distinguished path: " + distinguishedPath, 5);

          if (distinguishedPath.endsWith("/alias")) {
              // Ignore any aliases.
              ResolverUtils.debugPrintln("This path is an alias.  Dropping...", 5);
              continue;
          }

          /*
           * If we're fully resolving the locale (and, if code-fallback suppression
           * is enabled, if the value is not from code-fallback) or the values
           * aren't equal, add it to the resolved file.
           */
          if (resolutionType == ResolutionType.NO_CODE_FALLBACK && file.getSourceLocaleID(
                  distinguishedPath, null).equals(CODE_FALLBACK)) {
              continue;
          }

          // For simple resolution, don't add paths to child locales if the parent
          // locale contains the same path with the same value.
          String baseValue = file.getStringValue(distinguishedPath);
          if (resolutionType == ResolutionType.SIMPLE) {
              String parentValue = null;
              for (CLDRFile ancestor : ancestors) {
                  parentValue = ancestor.getStringValue(distinguishedPath);
                  if (parentValue != null) break;
              }
              ResolverUtils.debugPrintln(
                      "    Parent value : " + ResolverUtils.strRep(parentValue), 5);
              if (areEqual(parentValue, baseValue)) continue;
          }

          ResolverUtils.debugPrintln("  Adding to resolved file.", 5);
          // Suppress non-distinguishing attributes in simple inheritance
          String path = resolutionType == ResolutionType.SIMPLE ?
                  distinguishedPath : file.getFullXPath(distinguishedPath);
          ResolverUtils.debugPrintln("Path to be saved: " + path, 5);
          resolved.add(path, baseValue);
      }

      // Sanity check in simple resolution to make sure that all paths in the parent are also in the child.
      if (ancestors.size() > 0) {
          CLDRFile ancestor = ancestors.get(0);
          ResolverUtils.debugPrintln(
                  "Adding UNDEFINED values based on ancestor: " + ancestor.getLocaleID(), 3);
          for (String distinguishedPath : ResolverUtils.getAllPaths(ancestor)) {
              // Do the comparison with distinguished paths to prevent errors
              // resulting from duplicate full paths but the same distinguished path
              if (!basePaths.contains(distinguishedPath) &&
                      !ancestor.getStringValue(distinguishedPath).equals(UNDEFINED)) {
                  ResolverUtils.debugPrintln(
                          "Added UNDEFINED value for path: " + distinguishedPath, 4);
              }
          }
      }
      return resolved;
  }

  /**
   * Resolves all locales that match the given regular expression and outputs
   * their XML files to the given directory.
   * 
   * @param localeRegex a regular expression that will be matched against the
   *        names of locales
   * @param outputDir the directory to which to output the partially-resolved
   *        XML files
   * @param resolutionType the type of resolution to perform
   * @throws IllegalArgumentException if outputDir is not a directory
   */
  public void resolve(String localeRegex, String outputDir) {
    resolve(localeRegex, new File(outputDir));
  }

  /**
   * Writes out the given CLDRFile in XML form to the given directory
   * 
   * @param cldrFile the CLDRFile to print to XML
   * @param directory the directory to which to add the file
   */
  private static void printToFile(CLDRFile cldrFile, File directory) {
    ResolverUtils.debugPrint("Printing file...", 2);
    try {
      PrintWriter pw =
          new PrintWriter(new File(directory, cldrFile.getLocaleID() + ".xml"), "UTF-8");
      cldrFile.write(pw);
      pw.close();
      ResolverUtils.debugPrintln("done.\n", 2);
    } catch (FileNotFoundException e) {
      ResolverUtils.debugPrintln("\nFile not found: " + e.getMessage(), 1);
      System.exit(1);
      return;
    } catch (UnsupportedEncodingException e) {
      // This should never ever happen.
      ResolverUtils.debugPrintln("Your system does not support UTF-8 encoding: " + e.getMessage(),
          1);
      System.exit(1);
      return;
    }
  }

  /**
   * Convenience method to compare objects that works with nulls
   * 
   * @param o1 the first object
   * @param o2 the second object
   * @return true if objects o1 == o2 or o1.equals(o2); false otherwise
   */
  private static boolean areEqual(Object o1, Object o2) {
    if (o1 == o2) {
      return true;
    } else if (o1 == null) {
      return false;
    } else {
      return o1.equals(o2);
    }
  }
}
