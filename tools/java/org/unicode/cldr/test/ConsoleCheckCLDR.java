package org.unicode.cldr.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.icu.CollectionUtilities;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.FormatDemo;
import org.unicode.cldr.test.CheckCLDR.Phase;
import org.unicode.cldr.test.CheckCLDR.SimpleDemo;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.test.ExampleGenerator.Zoomed;
import org.unicode.cldr.tool.TablePrinter;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.Row;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CollectionUtilities.ComparableCollection;
import org.unicode.cldr.util.VoteResolver.CandidateInfo;
import org.unicode.cldr.util.VoteResolver.Organization;
import org.unicode.cldr.util.VoteResolver.UnknownVoterException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ElapsedTimer;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * Console test for CheckCLDR.
 * <br> Some common source directories:
 * 
 * <pre>
 *  -s C:/cvsdata/unicode/cldr/incoming/vetted/main
 *  -s C:/cvsdata/unicode/cldr/incoming/proposed/main
 *  -s C:/cvsdata/unicode/cldr/incoming/proposed/main
 *  -s C:/cvsdata/unicode/cldr/testdata/main
 * </pre>
 * @author markdavis
 *
 */
public class ConsoleCheckCLDR {
  public static boolean showStackTrace = false;
  public static boolean errorsOnly = false;
  static boolean SHOW_LOCALE = true;
  static Zoomed SHOW_EXAMPLES = null;
  static  PrettyPath prettyPathMaker = new PrettyPath();

  private static final int
  HELP1 = 0,
  HELP2 = 1,
  COVERAGE = 2,
  EXAMPLES = 3,
  FILE_FILTER = 4,
  TEST_FILTER = 5,
  DATE_FORMATS = 6,
  ORGANIZATION = 7,
  SHOWALL = 8,
  PATH_FILTER = 9,
  ERRORS_ONLY = 10,
  CHECK_ON_SUBMIT = 11,
  NO_ALIASES = 12,
  SOURCE_DIRECTORY = 13,
  USER = 14,
  PHASE = 15,
  GENERATE_HTML = 16,
  VOTE_RESOLVE = 17
  //VOTE_RESOLVE2 = 18
  ;

  private static final UOption[] options = {
    UOption.HELP_H(),
    UOption.HELP_QUESTION_MARK(),
    UOption.create("coverage", 'c', UOption.REQUIRES_ARG),
    UOption.create("examples", 'x', UOption.OPTIONAL_ARG),
    UOption.create("file_filter", 'f', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("test_filter", 't', UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("date_formats", 'd', UOption.NO_ARG),
    UOption.create("organization", 'o', UOption.REQUIRES_ARG),
    UOption.create("showall", 'a', UOption.NO_ARG),
    UOption.create("path_filter", 'p',  UOption.REQUIRES_ARG).setDefault(".*"),
    UOption.create("errors_only", 'e', UOption.NO_ARG),
    UOption.create("check-on-submit", 'k', UOption.NO_ARG),
    UOption.create("noaliases", 'n', UOption.NO_ARG),
    UOption.create("source_directory", 's',  UOption.REQUIRES_ARG).setDefault(Utility.MAIN_DIRECTORY),
    UOption.create("user", 'u',  UOption.REQUIRES_ARG),
    UOption.create("phase", 'z',  UOption.REQUIRES_ARG),
    UOption.create("generate_html", 'g',  UOption.OPTIONAL_ARG).setDefault(Utility.CHART_DIRECTORY + "/errors/"),
    UOption.create("vote resolution", 'v',  UOption.NO_ARG),
    // UOption.create("vote resolution2", 'w',  UOption.OPTIONAL_ARG).setDefault(Utility.BASE_DIRECTORY + "incoming/vetted/main/votes/"),
    //-v /Users/markdavis/Documents/workspace/cldr/src/incoming/vetted/main/usersa.xml
    //-w /Users/markdavis/Documents/workspace/cldr/src/incoming/vetted/main/votes/
  };
  private static final Comparator<String> baseFirstCollator = new Comparator<String>() {
    LanguageTagParser languageTagParser1 = new LanguageTagParser();
    LanguageTagParser languageTagParser2 = new LanguageTagParser();

    public int compare(String o1, String o2) {
      String ls1 = languageTagParser1.set(o1).getLanguageScript();
      String ls2 = languageTagParser2.set(o2).getLanguageScript();
      int result = ls1.compareTo(ls2);
      if (result != 0) return result;
      return o1.compareTo(o2);
    }
  };
  private static final boolean PATH_IN_COUNT = false;

  private static String[] HelpMessage = {
    "-h \t This message",
    "-s \t Source directory, default = " + Utility.MAIN_DIRECTORY,
    "-fxxx \t Pick the locales (files) to check: xxx is a regular expression, eg -f fr, or -f fr.*, or -f (fr|en-.*)",
    "-pxxx \t Pick the paths to check, eg -p(.*languages.*)",
    "-cxxx \t Set the coverage: eg -c comprehensive or -c modern or -c moderate or -c basic",
    "-txxx \t Filter the Checks: xxx is a regular expression, eg -t.*number.*. To check all BUT a given test, use the style -t ((?!.*CheckZones).*)",
    "-oxxx \t Organization: ibm, google, ....; filters locales and uses Locales.txt for coverage tests",
    "-x \t Turn on examples (actually a summary of the demo). Takes optional parameter: in -- for the zoomed in view.",
    "-d \t Turn on special date format checks",
    "-a \t Show all paths",
    "-e \t Show errors only (with -ef, only final processing errors)",
    "-n \t No aliases",
    "-u \t User, eg -uu148",
  };

  static Counter<ErrorType> subtotalCount = new Counter<ErrorType>(true); // new ErrorCount();
  static Counter<ErrorType> totalCount = new Counter<ErrorType>(true);

  /**
   * This will be the test framework way of using these tests. It is preliminary for now.
   * The Survey Tool will call setDisplayInformation, and getCheckAll.
   * For each cldrfile, it will set the cldrFile.
   * Then on each path in the file it will call check.
   * Right now it doesn't work with resolved files, so just use unresolved ones.
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    ElapsedTimer totalTimer = new ElapsedTimer();
    Utility.showOptions(args);
    UOption.parseArgs(args, options);
    if (options[HELP1].doesOccur || options[HELP2].doesOccur) {
      for (int i = 0; i < HelpMessage.length; ++i) {
        System.out.println(HelpMessage[i]);
      }
      return;
    }
    String factoryFilter = options[FILE_FILTER].value; 
    String checkFilter = options[TEST_FILTER].value;
    errorsOnly = options[ERRORS_ONLY].doesOccur;
//  if ("f".equals(options[ERRORS_ONLY].value)) {
//  CheckCLDR.finalErrorType = CheckStatus.warningType;
//  }

    SHOW_EXAMPLES = !options[EXAMPLES].doesOccur ? null
            : options[EXAMPLES].value == null ? Zoomed.OUT
                    : Zoomed.valueOf(options[EXAMPLES].value.toUpperCase());
    boolean showAll = options[SHOWALL].doesOccur; 
    boolean checkFlexibleDates = options[DATE_FORMATS].doesOccur; 
    String pathFilterString = options[PATH_FILTER].value;
    Matcher pathFilter = null;
    if (!pathFilterString.equals(".*")) {
      pathFilter = Pattern.compile(pathFilterString).matcher("");
    }
    boolean checkOnSubmit = options[CHECK_ON_SUBMIT].doesOccur; 
    boolean noaliases = options[NO_ALIASES].doesOccur; 

    Level coverageLevel = null;
    String coverageLevelInput = options[COVERAGE].value;
    if (coverageLevelInput != null) {
      coverageLevel = Level.get(coverageLevelInput);
      if (coverageLevel == Level.UNDETERMINED) {
        throw new IllegalArgumentException("-c" + coverageLevelInput + "\t is invalid: must be one of: " + "basic,moderate,...");
      }
    }

    String organization = options[ORGANIZATION].value;
    if (organization != null) {
      Set<String> organizations = StandardCodes.make().getLocaleCoverageOrganizations();
      if (!organizations.contains(organization)) {
        throw new IllegalArgumentException("-o" + organization + "\t is invalid: must be one of: " + organizations);
      }
    }

    Phase phase = Phase.SUBMISSION;
    if (options[PHASE].doesOccur) {
      try {
        phase = Phase.valueOf(options[PHASE].value.toUpperCase());
      } catch (RuntimeException e) {
        throw (IllegalArgumentException) new IllegalArgumentException("Incorrect Phase value: should be one of " + Arrays.asList(Phase.values())).initCause(e);
      }
    }

    String sourceDirectory = Utility.checkValidDirectory(options[SOURCE_DIRECTORY].value, "Fix with -s. Use -h for help.");

    if (options[GENERATE_HTML].doesOccur) {
      coverageLevel = Level.MODERN; // reset
      ErrorFile.generated_html_directory = options[GENERATE_HTML].value;
      ErrorFile.generated_html_count = BagFormatter.openUTF8Writer(ErrorFile.generated_html_directory, "count.txt");
//    try {
//    ErrorFile.voteFactory = CLDRFile.Factory.make(sourceDirectory + "../../proposed/main/", ".*");
//    } catch (RuntimeException e) {
//    ErrorFile.voteFactory = null;
//    }
      //PrintWriter cssFile = BagFormatter.openUTF8Writer(generated_html_directory, "index.css");
      //Utility;
    }

    if (options[VOTE_RESOLVE].doesOccur) {
      resolveVotesDirectory = Utility.checkValidFile(Utility.BASE_DIRECTORY + "incoming/vetted/votes/", true, null);
      VoteResolver.setVoterToInfo(Utility.checkValidFile(Utility.BASE_DIRECTORY + "incoming/vetted/usersa/usersa.xml", false, null));
      voteResolver = new VoteResolver<String>();
    }

    // check stuff
//  Comparator cc = StandardCodes.make().getTZIDComparator();
//  System.out.println(cc.compare("Antarctica/Rothera", "America/Cordoba"));
//  System.out.println(cc.compare("Antarctica/Rothera", "America/Indianapolis"));



    String user = options[USER].value;

    System.out.println("source directory: " + sourceDirectory + "\t(" + new File(sourceDirectory).getCanonicalPath() + ")");
    System.out.println("factoryFilter: " + factoryFilter);
    System.out.println("test filter: " + checkFilter);
    System.out.println("organization: " + organization);
    System.out.println("show examples: " + SHOW_EXAMPLES);
    System.out.println("phase: " + phase);
    System.out.println("coverage level: " + coverageLevel);
    System.out.println("checking dates: " + checkFlexibleDates);
    System.out.println("only check-on-submit: " + checkOnSubmit);
    System.out.println("show all: " + showAll);
    System.out.println("errors only?: " + errorsOnly);
    System.out.println("generate error counts: " + ErrorFile.generated_html_directory);
    //System.out.println("vote directory: " + (ErrorFile.voteFactory == null ? null : ErrorFile.voteFactory.getSourceDirectory()));
    System.out.println("resolve votes: " + resolveVotesDirectory);

    // set up the test
    Factory cldrFactory = CLDRFile.Factory.make(sourceDirectory, factoryFilter)
      .setAlternateSupplementalDirectory(new File(Utility.SUPPLEMENTAL_DIRECTORY));
    CheckCLDR checkCldr = CheckCLDR.getCheckAll(checkFilter);
    CLDRFile english;
    try {
      english = cldrFactory.make("en", true);
    } catch (Exception e1) {
      Factory backCldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, factoryFilter)
      .setAlternateSupplementalDirectory(new File(Utility.SUPPLEMENTAL_DIRECTORY));
      english = backCldrFactory.make("en", true);
    }
    checkCldr.setDisplayInformation(english);
    PathShower pathShower = new PathShower();

    // call on the files
    Set locales = new TreeSet(baseFirstCollator);
    locales.addAll(cldrFactory.getAvailable());

    List<CheckStatus> result = new ArrayList<CheckStatus>();
    Set<String> paths = new TreeSet<String>(); // CLDRFile.ldmlComparator);
    Map m = new TreeMap();
    //double testNumber = 0;
    Map<String,String> options = new HashMap<String,String>();
    FlexibleDateFromCLDR fset = new FlexibleDateFromCLDR();
    Set<String> englishPaths = null;


    Set<String> fatalErrors = new TreeSet<String>();

    showHeaderLine();

    SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);

    LocaleIDParser localeIDParser = new LocaleIDParser();
    String lastBaseLanguage = "";

    for (Iterator it = locales.iterator(); it.hasNext();) {
      String localeID = (String) it.next();
      if (CLDRFile.isSupplementalName(localeID)) continue;
      if (supplementalDataInfo.getDefaultContentLocales().contains(localeID)) {
        System.out.println("Skipping default content locale: " + localeID);
        continue;
      }

      boolean isLanguageLocale = localeID.equals(localeIDParser.set(localeID).getLanguageScript());
      options.clear();

      // if the organization is set, skip any locale that doesn't have a value in Locales.txt
      Level level = coverageLevel;
      if (level == null) {
        level = Level.BASIC;
      }
      if (organization != null) {
        Map<String,Level> locale_status = StandardCodes.make().getLocaleTypes().get(organization);
        if (locale_status == null) continue;
        level = locale_status.get(localeID);
        if (level == null) continue;
        if (level.compareTo(Level.BASIC) <= 0) continue;
      } else if (!isLanguageLocale) {
        // otherwise, skip all language locales
        options.put("CheckCoverage.skip","true");
      }

      if (coverageLevel != null) options.put("CoverageLevel.requiredLevel", coverageLevel.toString());
      if (organization != null) options.put("CoverageLevel.localeType", organization);
      options.put("phase", phase.toString());
      //options.put("SHOW_TIMES", "");

      if (SHOW_LOCALE) System.out.println();

      //options.put("CheckCoverage.requiredLevel","comprehensive");

      CLDRFile file;
      CLDRFile parent = null;

      ElapsedTimer timer = new ElapsedTimer();
      try {
        file = cldrFactory.make(localeID, true);
        if (ErrorFile.voteFactory != null) {
          ErrorFile.voteFile = ErrorFile.voteFactory.make(localeID, true);
        }
        final String parentID = file.getParent(localeID);
        if (parentID != null) {
          parent = cldrFactory.make(parentID, true);
        }
      } catch (RuntimeException e) {
        fatalErrors.add(localeID);
        System.out.println("FATAL ERROR: " + localeID);
        e.printStackTrace(System.out);
        continue;
      }

      // generate HTML if asked for
      if (ErrorFile.generated_html_directory != null) {
        String baseLanguage = localeIDParser.set(localeID).getLanguageScript();

        if (!baseLanguage.equals(lastBaseLanguage)) {
          lastBaseLanguage = baseLanguage;
          ErrorFile.openErrorFile(localeID, baseLanguage);
        }

      }

      if (user != null) {
        file = new CLDRFile.TestUser(file, user, isLanguageLocale);
        if (parent != null) {
          parent = new CLDRFile.TestUser(parent, user, isLanguageLocale);
        }
      }
      checkCldr.setCldrFileToCheck(file, options, result);

      subtotalCount.clear();

      for (Iterator<CheckStatus> it3 = result.iterator(); it3.hasNext();) {
        CheckStatus status = it3.next();
        String statusString = status.toString(); // com.ibm.icu.impl.Utility.escape(
        String statusType = status.getType();

        if (errorsOnly) {
          if (!statusType.equals(status.errorType)) continue;
        }
        if (checkOnSubmit) {
          if (!status.isCheckOnSubmit() || !statusType.equals(status.errorType)) continue;
        }
        showValue(file, null, localeID, null, null, null, null, statusString, status.getSubtype(), null);
        // showSummary(checkCldr, localeID, level, statusString);
      }
      paths.clear();
      //CollectionUtilities.addAll(file.iterator(pathFilter), paths);
      addPrettyPaths(file, pathFilter, prettyPathMaker, noaliases, false, paths);
      addPrettyPaths(file, file.getExtraPaths(), pathFilter, prettyPathMaker, noaliases, false, paths);

      // also add the English paths
      //CollectionUtilities.addAll(checkCldr.getDisplayInformation().iterator(pathFilter), paths);
      // initialize the first time in.
      if (englishPaths == null) {
        englishPaths = new HashSet<String>();
        final CLDRFile displayFile = checkCldr.getDisplayInformation();
        addPrettyPaths(displayFile, pathFilter, prettyPathMaker, noaliases, true, englishPaths);
        addPrettyPaths(displayFile, displayFile.getExtraPaths(), pathFilter, prettyPathMaker, noaliases, true, englishPaths);
        englishPaths = Collections.unmodifiableSet(englishPaths); // for robustness
      }
      // paths.addAll(englishPaths);

      UnicodeSet missingExemplars = new UnicodeSet();
      UnicodeSet missingCurrencyExemplars = new UnicodeSet();
      if (checkFlexibleDates) {
        fset.set(file);
      }
      pathShower.set(localeID);

      // only create if we are going to use
      ExampleGenerator exampleGenerator = SHOW_EXAMPLES != null ? new ExampleGenerator(file, Utility.SUPPLEMENTAL_DIRECTORY) : null;
      ExampleContext exampleContext = new ExampleContext();

      //Status pathStatus = new Status();
      int pathCount = 0;
      Status otherPath = new Status();

      for (Iterator<String> it2 = paths.iterator(); it2.hasNext();) {
        pathCount++;
        String prettyPath = it2.next();
        String path = prettyPathMaker.getOriginal(prettyPath);
        if (path == null) {
          prettyPathMaker.getOriginal(prettyPath);
        }

        if (!showAll && !file.isWinningPath(path)) {
          continue;
        }
        final String sourceLocaleID = file.getSourceLocaleID(path, otherPath);
        if (!isLanguageLocale) {
          if (!localeID.equals(sourceLocaleID)) {
            continue;
          }
          // also skip aliases
          if (!path.equals(otherPath.pathWhereFound)) {
            continue;
          }
        }

        if (path.contains("@alt")) {
          if (path.contains("proposed")) continue;
        }
        String value = file.getStringValue(path);
        String fullPath = file.getFullXPath(path);


        String example = "";

        if (SHOW_EXAMPLES != null) {
          example = exampleGenerator.getExampleHtml(path, value, ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
          showExamples(checkCldr, prettyPath, localeID, exampleGenerator, path, value, fullPath, example, exampleContext);
          //continue; // don't show problems
        }

        if (checkFlexibleDates) {
          fset.checkFlexibles(path, value, fullPath);
        }

        int limit = 1;
        if (SHOW_EXAMPLES == Zoomed.IN) limit = 2;
        for (int jj = 0; jj < limit; ++jj) {
          if (jj == 0) {
            checkCldr.check(path, fullPath, value, options, result);
          } else {
            checkCldr.getExamples(path, fullPath, value, options, result);
          }

          boolean showedOne = false;
          for (Iterator<CheckStatus> it3 = result.iterator(); it3.hasNext();) {
            CheckStatus status = it3.next();
            String statusString = status.toString(); // com.ibm.icu.impl.Utility.escape(
            String statusType = status.getType();
            if (errorsOnly && !statusType.equals(status.errorType)) continue;
            if (checkOnSubmit) {
              if (!status.isCheckOnSubmit() || !statusType.equals(status.errorType)) continue;
            }
            //pathShower.showHeader(path, value);


            //System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
            if (statusType.equals(CheckStatus.demoType)) {
              SimpleDemo d = status.getDemo();
              if (d != null && d instanceof FormatDemo) {
                FormatDemo fd = (FormatDemo)d;
                m.clear();
                //m.put("pattern", fd.getPattern());
                //m.put("input", fd.getRandomInput());
                if (d.processPost(m)) System.out.println("\tDemo:\t" + fd.getPlainText(m));
              }
              continue;
            }
            showValue(file, prettyPath, localeID, example, path, value, fullPath, statusString, status.getSubtype(), exampleContext);
            showedOne = true;

            Object[] parameters = status.getParameters();
            if (parameters != null) {
              if (parameters.length >= 1 && status.getCause().getClass() == CheckForExemplars.class) {
                try {
                  UnicodeSet set = new UnicodeSet(parameters[0].toString());
                  if (status.getMessage().contains("currency")) {
                    missingCurrencyExemplars.addAll(set);
                  } else {
                    missingExemplars.addAll(set);
                  }
                } catch (RuntimeException e) {} // skip if not parseable as set
              }
              for (int i = 0; i < parameters.length; ++i) {
                if (showStackTrace && parameters[i] instanceof Throwable) {
                  ((Throwable) parameters[i]).printStackTrace();
                }
              }
            }
            // survey tool will use: if (status.hasHTMLMessage()) System.out.println(status.getHTMLMessage());
          }
          if (!showedOne && phase != Phase.FINAL_TESTING) {
            if (fullPath != null && draftStatusMatcher.reset(fullPath).find() && localeID.equals(sourceLocaleID) && path.equals(otherPath.pathWhereFound)) {
              final String draftStatus = draftStatusMatcher.group(1);
              // see if value is same as parents, then skip
              String parentValue = parent == null ? null : parent.getStringValue(path);
              if (parentValue == null || !parentValue.equals(value)) {
                showValue(file, prettyPath, localeID, example, path, value, fullPath, draftStatus, Subtype.none, exampleContext);
                showedOne = true;
              }
            }
            if (!showedOne && showAll) {
              showValue(file, prettyPath, localeID, example, path, value, fullPath, "ok", Subtype.none, exampleContext);
              showedOne = true;
              //pathShower.showHeader(path, value);
            }
          }

        }
      }

      if (resolveVotesDirectory != null) {
        LocaleVotingData.resolveErrors(localeID);
      }

      showSummary(checkCldr, localeID, level, "Items (including inherited):\t" + pathCount);
      if (missingExemplars.size() != 0) {
        Collator col = Collator.getInstance(new ULocale(localeID));
        showSummary(checkCldr, localeID, level, "Total missing from general exemplars:\t" + CollectionUtilities.prettyPrint(missingExemplars, true, null, null, col, col));
      }
      if (missingCurrencyExemplars.size() != 0) {
        Collator col = Collator.getInstance(new ULocale(localeID));
        showSummary(checkCldr, localeID, level, "Total missing from currency exemplars:\t" + CollectionUtilities.prettyPrint(missingCurrencyExemplars, true, null, null, col, col));
      }
      for (ErrorType type : subtotalCount.keySet()) {
        showSummary(checkCldr, localeID, level, "Subtotal " + type + ":\t" + subtotalCount.getCount(type));
      }
      if (checkFlexibleDates) {
        fset.showFlexibles();
      }
      if (SHOW_EXAMPLES != null) {
//      ldml/dates/timeZoneNames/zone[@type="America/Argentina/San_Juan"]/exemplarCity
        for (String zone : StandardCodes.make().getGoodAvailableCodes("tzid")) {
          String path = "//ldml/dates/timeZoneNames/zone[@type=\"" + zone + "\"]/exemplarCity";
          String prettyPath = prettyPathMaker.getPrettyPath(path, false);
          if (pathFilter != null && !pathFilter.reset(path).matches()) continue;
          String fullPath = file.getStringValue(path);
          if (fullPath != null) continue;
          String example = exampleGenerator.getExampleHtml(path, null, ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.NATIVE);
          showExamples(checkCldr, prettyPath, localeID, exampleGenerator, path, null, fullPath, example, exampleContext);
        }
      }
      System.out.println("Elapsed time: " + timer);
      System.out.flush();
    }

    if (ErrorFile.errorFileWriter != null) {
      ErrorFile.closeErrorFile();
    }

    if (ErrorFile.generated_html_directory != null) {
      ErrorFile.writeErrorCountsText();
      ErrorFile.writeErrorFileIndex();
    }
    for (ErrorType type : totalCount.keySet()) {
      System.out.println("Total " + type + ":\t" + totalCount.getCount(type));
    }

    System.out.println("Total Elapsed: " + totalTimer);
    if (fatalErrors.size() != 0) {
      System.out.println("FATAL ERRORS:" );
    }
  }

  static class LocaleVotingData {
    private int disputedCount = 0;
    Counter<Organization> missingOrganizationCounter = new Counter<Organization>(true);
    Counter<Organization> goodOrganizationCounter = new Counter<Organization>(true);
    Counter<Organization> conflictedOrganizations = new Counter<Organization>(true);
    Counter<VoteResolver.Status> winningStatusCounter = new Counter<VoteResolver.Status>(true);

    static Map<String,LocaleVotingData> localeToErrors = new HashMap<String, LocaleVotingData>();
    private static Map<Integer,String> idToPath;

    public static void resolveErrors(String locale) {
      localeToErrors.put(locale, new LocaleVotingData(locale));
    }

    public LocaleVotingData(String locale) {

      Map<Organization, VoteResolver.Level> orgToMaxVote = VoteResolver.getOrganizationToMaxVote(locale);

      Map<Integer, Map<Integer, CandidateInfo>> info = VoteResolver.getBaseToAlternateToInfo(resolveVotesDirectory + locale + ".xml");

      Map<String,Integer> valueToItem = new HashMap<String,Integer>();

      for (int basePath : info.keySet()) {
        final Map<Integer, CandidateInfo> itemInfo = info.get(basePath);

        // find the last release status and value
        voteResolver.clear();
        valueToItem.clear();

        for (int item : itemInfo.keySet()) {
          String itemValue = getValue(item);
          valueToItem.put(itemValue, item);

          CandidateInfo candidateInfo = itemInfo.get(item);
          if (candidateInfo.oldStatus != null) {
            voteResolver.setLastRelease(itemValue, candidateInfo.oldStatus);
          }
          voteResolver.add(itemValue);
          for (int voter : candidateInfo.voters) {
            try {
              voteResolver.add(itemValue, voter);
            } catch (UnknownVoterException e) {
              // skip
            }
          }
        }

        EnumSet<Organization> basePathConflictedOrganizations = voteResolver.getConflictedOrganizations();
        conflictedOrganizations.addAll(basePathConflictedOrganizations, 1);

        VoteResolver.Status winningStatus = voteResolver.getWinningStatus();
        String winningValue = voteResolver.getWinningValue();

        winningStatusCounter.add(winningStatus,1);

        if (winningStatus == VoteResolver.Status.approved) {
          continue;
        } 

        CandidateInfo candidateInfo = itemInfo.get(valueToItem.get(winningValue));
        Map<Organization, VoteResolver.Level> orgToMaxVoteHere = VoteResolver.getOrganizationToMaxVote(candidateInfo.voters);

        // if the winning item is less than contributed, record the organizations that haven't given their maximum vote to the winning item.
        if (winningStatus.compareTo(VoteResolver.Status.contributed) < 0) {
          //       showPaths(basePath, itemInfo);
          for (Organization org : orgToMaxVote.keySet()) {
            VoteResolver.Level maxVote = orgToMaxVote.get(org);
            VoteResolver.Level maxVoteHere = orgToMaxVoteHere.get(org);
            if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
              missingOrganizationCounter.add(org,1);
            }
          }
          if (voteResolver.isDisputed()) {
            disputedCount++;
            String path = getIdToPath(basePath);
            ErrorFile.addDataToErrorFile(locale, path, null, ErrorType.disputed, Subtype.none);
          }
        } else {
          for (Organization org : orgToMaxVote.keySet()) {
            VoteResolver.Level maxVote = orgToMaxVote.get(org);
            VoteResolver.Level maxVoteHere = orgToMaxVoteHere.get(org);
            if (maxVoteHere == null || maxVoteHere.compareTo(maxVote) < 0) {
            } else {
              goodOrganizationCounter.add(org,1);
            }
          }
        }
      }
      System.out.println(getLocaleAndName(locale) + "\tEnabled Organizations:\t" + orgToMaxVote);
      if (disputedCount != 0) {
        System.out.println(getLocaleAndName(locale) + "\tDisputed Items:\t" + disputedCount);    
      }

      if (missingOrganizationCounter.size() > 0) {
        System.out.println(getLocaleAndName(locale) + "\tMIA organizations:\t" + missingOrganizationCounter);
        System.out.println(getLocaleAndName(locale) + "\tConflicted organizations:\t" + conflictedOrganizations);
        System.out.println(getLocaleAndName(locale) + "\tCool organizations!:\t" + goodOrganizationCounter);
      }
      System.out.println(getLocaleAndName(locale) + "\tOptimal Status:\t" + winningStatusCounter);
    }

    
    private static String getIdToPath(int basePath) {
      if (idToPath == null) {
        idToPath = VoteResolver.getIdToPath(resolveVotesDirectory + "xpathTable.xml");
      }
      return idToPath.get(basePath);
    }

    public static LocaleVotingData get(String locale) {
      return localeToErrors.get(locale);
    }

    int getDisputedCount() {
      return disputedCount;
    }
    
    String getConflictedHTML() {
      String result = conflictedOrganizations.toString();
      if (result.length() == 0) {
        return "";
      }
      result = result.substring(1,result.length()-1);
      result = result.replace(", ", "<br>");
      return result;
    }
  }

  private static String getValue(int item) {
    return String.valueOf(item);
  }


  static Matcher draftStatusMatcher = Pattern.compile("\\[@draft=\"(provisional|unconfirmed)\"]").matcher("");

  enum ErrorType {
    ok,
    error,
    disputed,
    warning,
    posix, minimal, basic, moderate, modern, comprehensive, optional,
    contributed, provisional, unconfirmed, 
    unknown;
    static EnumSet<ErrorType> unapproved = EnumSet.range(ErrorType.contributed, ErrorType.unconfirmed);
    static EnumSet<ErrorType> coverage = EnumSet.range(ErrorType.posix, ErrorType.optional);
    static EnumSet<ErrorType> showInSummary = EnumSet.of(
            ErrorType.error, ErrorType.warning, ErrorType.posix, ErrorType.minimal, ErrorType.basic
    );

    static ErrorType fromStatusString(String statusString) {
      ErrorType shortStatus = statusString.equals("ok") ? ErrorType.ok 
              : statusString.startsWith("Error") ? ErrorType.error 
                      : statusString.equals("disputed") ? ErrorType.disputed 
                              : statusString.startsWith("Warning") ? ErrorType.warning 
                                      : statusString.equals("contributed") ? ErrorType.contributed 
                                              : statusString.equals("provisional") ? ErrorType.provisional 
                                                      : statusString.equals("unconfirmed") ? ErrorType.unconfirmed 
                                                              : ErrorType.unknown ;
      if (shortStatus == ErrorType.unknown) {
        throw new IllegalArgumentException("Unknown error type: " + statusString);
      } else if (shortStatus == ErrorType.warning) {
        if (coverageMatcher.reset(statusString).find()) {
          shortStatus = ErrorType.valueOf(coverageMatcher.group(1));
        }
      }
      return shortStatus;
    }
  };

  /*
  static class ErrorCount implements Comparable<ErrorCount> {
    private Counter<ErrorType> counter = new Counter<ErrorType>();

    public int compareTo(ErrorCount o) {
      // we don't really need a good comparison - aren't going to be sorting
      return total() < o.total() ? -1 : total() > o.total() ? 1 : 0;
    }
    public long total() {
      return counter.getTotal();
    }
    public void clear() {
      counter.clear();
    }
    public Set<ErrorType> keySet() {
      return counter.getKeysetSortedByKey();
    }
    public long getCount(ErrorType input) {
      return counter.getCount(input);
    }
    public void increment(ErrorType errorType) {
      counter.add(errorType, 1);
    }
  }
   */

  static class ErrorFile {

    private static final boolean SHOW_VOTING_INFO = false;
    public static CLDRFile voteFile;
    public static Factory voteFactory;

    private static void openErrorFile(String localeID, String baseLanguage) throws IOException {
      htmlOpenedFileLocale = localeID;
      if (ErrorFile.errorFileWriter != null) {
        ErrorFile.closeErrorFile();
      }
      ErrorFile.errorFileWriter = BagFormatter.openUTF8Writer(ErrorFile.generated_html_directory, baseLanguage + ".html");
      ErrorFile.errorFileTable = new TablePrinter();
      errorFileCounter.clear();
      ErrorFile.errorFileTable.setCaption("Problem Details")
      .addColumn("Problem").setCellAttributes("align=\"left\" class=\"{0}\"").setSortPriority(0).setSpanRows(true)
        .setBreakSpans(true).setRepeatHeader(true).setHeaderCell(true)
      .addColumn("Subtype").setCellAttributes("align=\"left\" class=\"{1}\"").setSortPriority(1).setSpanRows(true)
        .setBreakSpans(true).setRepeatHeader(true).setHeaderCell(true)
      .addColumn("Locale").setCellAttributes("class=\"{1}\"").setCellPattern("<a href=\"http://unicode.org/cldr/apps/survey?_={0}\">{0}</a>").setSortPriority(2).setSpanRows(true).setBreakSpans(true).setRepeatDivider(true)
      .addColumn("Name").setCellAttributes("class=\"{1}\"").setSpanRows(true).setBreakSpans(true)
      //.addColumn("HIDDEN").setSortPriority(2).setHidden(true)
      .addColumn("Section").setCellAttributes("class=\"{1}\"").setSortPriority(3).setCellPattern("<a href=\"http://unicode.org/cldr/apps/survey?_={3}&x={0}\">{0}</a>").setSpanRows(true)
      .addColumn("Count").setCellAttributes("class=\"{1}\" align=\"right\"");
      //showLineHeaders(generated_html_table);
      //"<a href='http://unicode.org/cldr/apps/survey?_=" + locale + "'>" + locale + "</a>";

      ErrorFile.htmlOpenedFileLanguage = baseLanguage;
      showIndexHead("", localeID, ErrorFile.errorFileWriter);
    }


    static TablePrinter errorFileTable = new TablePrinter();
    static Counter<Row.R4<String, String, ErrorType, Subtype>> errorFileCounter
      = new Counter<Row.R4<String, String, ErrorType, Subtype>>(true);

    private static void addDataToErrorFile(String localeID, String path, String value, ErrorType shortStatus, Subtype subType) {
      String section = path == null 
              ? null 
              : org.unicode.cldr.util.PathUtilities.xpathToMenu(path);
      if (section == null) {
        section = "general";
      }
      if (voteFile != null) {
        String fullVotePath = voteFile.getFullXPath(path);
        String users = "";
      }
      errorFileCounter.add(
              new Row.R4<String, String, ErrorType, Subtype>(localeID, section, shortStatus, subType), 1);
      ErrorFile.sectionToProblemsToLocaleToCount.add(
              new Row.R4<String, ErrorType, Subtype, String>(section, shortStatus, subType, localeID), 1);
    }

    private static void closeErrorFile() {
      Set<String> locales = new TreeSet();
      for (Row.R4<String, String, ErrorType, Subtype> item : errorFileCounter.keySet()) {
        String localeID = item.get0();
        locales.add(localeID);
        String section = item.get1();
        ErrorType shortStatus = item.get2();
        Subtype subtype = item.get3();
        //final String prettyPath = path == null ? "general" : prettyPathMaker.getPrettyPath(path, true);
        //final String outputForm = path == null ? "general" : prettyPathMaker.getOutputForm(prettyPath);
        errorFileTable.addRow()
        .addCell(shortStatus)
        .addCell(subtype)
        .addCell(localeID)
        .addCell(ConsoleCheckCLDR.getLocaleName(localeID))
        //.addCell(prettyPath) // menuPath == null ? "" : "<a href='" + link + "'>" + menuPath + "</a>"
        .addCell(section) // menuPath == null ? "" : "<a href='" + link + "'>" + menuPath + "</a>"
        .addCell(errorFileCounter.getCount(item))
        //.addCell(ConsoleCheckCLDR.safeForHtml(path == null ? null : ConsoleCheckCLDR.getEnglishPathValue(path)))
        //.addCell(ConsoleCheckCLDR.safeForHtml(value))
        .finishRow();
      }

      if (SHOW_VOTING_INFO) {
        TablePrinter data = new TablePrinter().setCaption("Voting Information")
        .addColumn("Locale").setHeaderCell(true)
        .addColumn("Name").setHeaderCell(true)
        .addColumn("Organization")
        .addColumn("Missing")
        .addColumn("Conflicted")
        //.addColumn("Good")
        ;
        for (String localeID : locales) {
          // now the voting info
          LocaleVotingData localeVotingData = LocaleVotingData.localeToErrors.get(localeID);
          if (localeVotingData != null) {
            // find all the orgs with data
            EnumSet<Organization> orgs = EnumSet.noneOf(Organization.class);
            orgs.addAll(localeVotingData.missingOrganizationCounter.keySet());
            orgs.addAll(localeVotingData.conflictedOrganizations.keySet());
            orgs.addAll(localeVotingData.goodOrganizationCounter.keySet());
            for (Organization org : orgs) {
              data.addRow()
              .addCell(ConsoleCheckCLDR.getLinkedLocale(localeID))
              .addCell(ConsoleCheckCLDR.getLocaleName(localeID))
              .addCell(org)
              .addCell(localeVotingData.missingOrganizationCounter.getCount(org))
              .addCell(localeVotingData.conflictedOrganizations.getCount(org))
              //.addCell(localeVotingData.goodOrganizationCounter.getCount(org))
              .finishRow();
            }
          }
        }
        ErrorFile.errorFileWriter.println(data.toTable());
        ErrorFile.errorFileWriter.println("<p></p>");
      }

      //generated_html.println("<table border='1' style='border-collapse: collapse' bordercolor='#CCCCFF'>");
      // Locale Group Error Warning Missing Votes: Contributed  Missing Votes: Provisional  Missing Votes: Unconfirmed  Missing Coverage: Posix Missing Coverage: Minimal Missing Coverage: Basic Missing Coverage: Moderate  Missing Coverage: Modern
      ErrorFile.errorFileWriter.println(ErrorFile.errorFileTable.toTable());
      ErrorFile.errorFileWriter.println("</body></html>");
      ErrorFile.errorFileWriter.close();
      ErrorFile.errorFileTable = null;
    }

    // ================  Index File ===================

    static void showErrorFileIndex(PrintWriter generated_html_index) {

      // get organizations
      Relation<String, String> orgToLocales = getOrgToLocales();

      TablePrinter indexTablePrinter = new TablePrinter().setCaption("Problem Summary")
      .setTableAttributes("border='1' style='border-collapse: collapse' bordercolor='blue'")
      .addColumn("BASE").setHidden(true).setRepeatDivider(true)
      .addColumn("Locale").setCellPattern("<a name=\"{0}\" href=\"{1}.html\">{0}</a>") // link to base, anchor with full
      .addColumn("Name");
      if (SHOW_VOTING_INFO) {
        indexTablePrinter.addColumn("Summary")
        .addColumn("Missing");
      }
      for (String org : orgToLocales.keySet()) {
        indexTablePrinter.addColumn(org.substring(0,2));
      }
      indexTablePrinter
      .addColumn("Disputed").setHeaderAttributes("class='disputed'").setCellAttributes("class='disputed'")
      .addColumn("Conflicted").setHeaderAttributes("class='conflicted'").setCellAttributes("class='conflicted'")
      ;
      
      for (ConsoleCheckCLDR.ErrorType type : ConsoleCheckCLDR.ErrorType.showInSummary) {
        String columnTitle = UCharacter.toTitleCase(type.toString(), null);
        final boolean coverage = ConsoleCheckCLDR.ErrorType.coverage.contains(type);
        if (coverage) {
          columnTitle = "MC: " + columnTitle;
        } else if (ConsoleCheckCLDR.ErrorType.unapproved.contains(type)){
          columnTitle = "MV: " + columnTitle;
        }
        indexTablePrinter.addColumn(columnTitle).setHeaderAttributes("class='" + type + "'").setCellAttributes("class='" + type + "'");
      }

      // now fill in the data
      LanguageTagParser ltp = new LanguageTagParser();
      for (String key : ErrorFile.errorFileIndexData.keySet()) {
        Pair<String, Counter<ErrorType>>  pair = ErrorFile.errorFileIndexData.get(key);
        String htmlOpenedFileLanguage = pair.getFirst();
        Counter<ErrorType> counts = pair.getSecond();
        LocaleVotingData votingData = LocaleVotingData.get(htmlOpenedFileLanguage);
        if (counts.getTotal() == 0) {
          continue;
        }
        final String baseLanguage = ltp.set(htmlOpenedFileLanguage).getLanguage();
        indexTablePrinter.addRow()
        .addCell(baseLanguage)
        .addCell(htmlOpenedFileLanguage)
        .addCell(ConsoleCheckCLDR.getLocaleName(htmlOpenedFileLanguage));
        if (SHOW_VOTING_INFO) {
          indexTablePrinter.addCell(votingData == null ? "" : votingData.winningStatusCounter.toString())
          .addCell(votingData == null ? "" : votingData.missingOrganizationCounter.toString());
        }
        for (String org : orgToLocales.keySet()) {
          indexTablePrinter.addCell(orgToLocales.getAll(org).contains(htmlOpenedFileLanguage) ? org.substring(0,2) : "");
        }
        indexTablePrinter
        .addCell(votingData == null ? "" : formatSkippingZero(votingData.getDisputedCount()))
        .addCell(votingData == null ? "" : votingData.getConflictedHTML())
        ;
        for (ConsoleCheckCLDR.ErrorType type : ConsoleCheckCLDR.ErrorType.showInSummary) {
          indexTablePrinter.addCell(formatSkippingZero(counts.getCount(type)));
        }
        indexTablePrinter.finishRow();
      }      
      generated_html_index.println(indexTablePrinter.toTable());
      generated_html_index.println("</html>");
    }

    static Relation<String,String> orgToLocales;
    
    private static Relation<String, String> getOrgToLocales() {
      if (orgToLocales == null) {
        orgToLocales = new Relation(new TreeMap(), TreeSet.class);
        StandardCodes sc = StandardCodes.make();
        for (String org : sc.getLocaleCoverageOrganizations()) {
          for (String locale : sc.getLocaleCoverageLocales(org)) {
            Level x = sc.getLocaleCoverageLevel(org, locale);
            if (x.compareTo(Level.BASIC) > 0) {
              orgToLocales.put(org, locale);
            }
          }
        }
      }
      return orgToLocales;
    }
    
    static void showSections() throws IOException {
      Relation<String, String> orgToLocales = getOrgToLocales();
      TablePrinter indexTablePrinter = new TablePrinter().setCaption("Problem Summary")
      .setTableAttributes("border='1' style='border-collapse: collapse' bordercolor='blue'")
      .addColumn("Section").setSpanRows(true).setBreakSpans(true).setRepeatDivider(true)
      .addColumn("Problems").setCellAttributes("style=\"text-align:left\" class=\"{2}\"").setSpanRows(true)
      .addColumn("Subtype").setCellAttributes("style=\"text-align:left\" class=\"{2}\"").setSpanRows(true)
      .addColumn("Locale").setCellAttributes("class=\"{2}\"")
      .addColumn("Code").setCellAttributes("class=\"{2}\"").setCellPattern("<a href=\"http://unicode.org/cldr/apps/survey?_={0}&x={1}\">{0}</a>")
      .addColumn("Count").setCellAttributes("class=\"{2}\"")
      ;
      for (String org : orgToLocales.keySet()) {
        indexTablePrinter.addColumn(org.substring(0,2));
      }

      for (Row.R4<String, ErrorType, Subtype, String> sectionAndProblemsAndLocale 
              : ErrorFile.sectionToProblemsToLocaleToCount.getKeysetSortedByKey()) {
        final ErrorType problem = sectionAndProblemsAndLocale.get1();
        final Subtype subtype = sectionAndProblemsAndLocale.get2();
        if (!ConsoleCheckCLDR.ErrorType.showInSummary.contains(problem)) {
          continue;
        }
        final String locale = sectionAndProblemsAndLocale.get3();
        if (problem != ErrorType.error && problem != ErrorType.disputed && !orgToLocales.containsValue(locale)) {
          continue;
        }
        long count = ErrorFile.sectionToProblemsToLocaleToCount.getCount(sectionAndProblemsAndLocale);
        final String section = sectionAndProblemsAndLocale.get0();
        indexTablePrinter.addRow()
        .addCell(section)
        .addCell(problem)
        .addCell(subtype)
        .addCell(ConsoleCheckCLDR.getLocaleName(locale))
        .addCell(locale)
        .addCell(count);
        for (String org : orgToLocales.keySet()) {
          indexTablePrinter.addCell(orgToLocales.getAll(org).contains(locale) ? org.substring(0,2) : "");
        }
        indexTablePrinter.finishRow();
      }
      PrintWriter generated_html_index = BagFormatter.openUTF8Writer(ErrorFile.generated_html_directory, "sections.html");
      ConsoleCheckCLDR.ErrorFile.showIndexHead("Error Report Index by Section", "", generated_html_index);
      generated_html_index.println(indexTablePrinter.toTable());
      generated_html_index.println("</html>");
      generated_html_index.close();
    }
    
    static String formatSkippingZero(long count) {
      if (count == 0) {
        return "";
      }
      return String.valueOf(count);
    }

    static void showIndexHead(String title, String localeID, PrintWriter generated_html_index) {
      final boolean notLocaleSpecific = localeID.length() == 0;
      if ((!notLocaleSpecific)) {
        title="Errors in " + ConsoleCheckCLDR.getNameAndLocale(localeID, false);
      }
      generated_html_index.println("<html>" +
              "<head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>" + Utility.LINE_SEPARATOR +
              "<title>" + title + "</title>" + Utility.LINE_SEPARATOR +
              "<link rel='stylesheet' href='errors.css' type='text/css'>" + Utility.LINE_SEPARATOR +
              "<base target='_blank'>" + Utility.LINE_SEPARATOR +
              "</head><body>" + Utility.LINE_SEPARATOR +
              "<h1>" + title + "</h1>" + Utility.LINE_SEPARATOR +
              "<p>" +
              "<a href='index.html" + (notLocaleSpecific ? "" : "#" + localeID) + "'>Index</a>" +
              " | " +
              "<a href='sections.html" + (notLocaleSpecific ? "" : "#" + localeID) + "'>Index by Section</a>" +
              " | " +
              "<a href='http://unicode.org/cldr/data/docs/survey/vetting.html'><b style='background-color: yellow;'><i>Help: How to Vet</i></b></a>" +
              		"</p>" +
              "<p>The following errors have been detected in the locale" +
              (notLocaleSpecific
                      ? "s. " + org.unicode.cldr.tool.ShowLanguages.getHelpHtml("error_index_header")
                      : " " + ConsoleCheckCLDR.getNameAndLocale(localeID, false) + ". " + ErrorFile.ERROR_CHART_HEADER
              ));
    }
    
    private static void writeErrorFileIndex() throws IOException {
      PrintWriter generated_html_index = BagFormatter.openUTF8Writer(ErrorFile.generated_html_directory, "index.html");
      ConsoleCheckCLDR.ErrorFile.showIndexHead("Error Report Index", "", generated_html_index);
      ConsoleCheckCLDR.ErrorFile.showErrorFileIndex(generated_html_index);
      generated_html_index.close();
      showSections();
    }

    private static void writeErrorCountsText() {
      //if (ErrorFile.htmlErrorsPerLocale.total() != 0) {

      // do the plain text file
      ErrorFile.generated_html_count.print(ConsoleCheckCLDR.lastHtmlLocaleID + ";\tcounts");
      for (ConsoleCheckCLDR.ErrorType type : ConsoleCheckCLDR.ErrorType.showInSummary) {
        ErrorFile.generated_html_count.print(";\t" + type + "=" + ErrorFile.htmlErrorsPerLocale.getCount(type));
      }
      ErrorFile.generated_html_count.println();
      ErrorFile.generated_html_count.flush();

      // now store the data for the index
      ErrorFile.errorFileIndexData.put(ConsoleCheckCLDR.lastHtmlLocaleID, 
              new Pair<String, Counter<ErrorType>>(ConsoleCheckCLDR.lastHtmlLocaleID, ErrorFile.htmlErrorsPerLocale));
      ErrorFile.htmlErrorsPerLocale = new Counter<ErrorType>();
      //}
    }
    /*
     *   static Counter<Organization> missingOrganizationCounter = new Counter<Organization>(true);
  static Counter<Organization> goodOrganizationCounter = new Counter<Organization>(true);
  static Counter<Organization> conflictedOrganizations = new Counter<Organization>(true);
  static Counter<VoteResolver.Status> winningStatusCounter = new Counter<VoteResolver.Status>(true);

     */

    static Counter<ErrorType> htmlErrorsPerLocale = new Counter<ErrorType>(); // ConsoleCheckCLDR.ErrorCount();
    static PrintWriter generated_html_count = null;
    private static TreeMap<String, Pair<String, Counter<ErrorType>>> errorFileIndexData = new TreeMap<String, Pair<String, Counter<ErrorType>>>();

    //private static ConsoleCheckCLDR.ErrorCount htmlErrorsPerBaseLanguage = new ConsoleCheckCLDR.ErrorCount();
    static PrintWriter errorFileWriter = null;
    private static String htmlOpenedFileLanguage = null;
    private static String htmlOpenedFileLocale = null;
    private static final String ERROR_CHART_HEADER = org.unicode.cldr.tool.ShowLanguages.getHelpHtml("error_locale_header");
//  "Please review and correct them. " +
//  "Note that errors in <i>sublocales</i> are often fixed by fixing the main locale.</p>" + Utility.LINE_SEPARATOR +
//  "<p><i>This list is only generated daily, and so may not reflect fixes you have made until tomorrow. " +
//  "(There were production problems in integrating it fully into the Survey tool. " +
//  "However, it should let you see the problems and make sure that they get taken care of.)</i></p>" +
//  "<p>Coverage depends on your organizations goals: the highest tier languages should include up through all Modern values.</p>" + Utility.LINE_SEPARATOR;
    static  String generated_html_directory = null;
    public static Counter<Row.R4<String, ErrorType, Subtype, String>> sectionToProblemsToLocaleToCount
    = new Counter<Row.R4<String, ErrorType, Subtype, String>>();
  }

  private static void showSummary(CheckCLDR checkCldr, String localeID, Level level, String value) {
    String line = getLocaleAndName(localeID) + "\tSummary\t" + level + "\t" + value;
    System.out.println(line);
//  if (generated_html != null) {
//  line = TransliteratorUtilities.toHTML.transform(line);
//  line = line.replace("\t", "</td><td>");
//  generated_html.println("<table><tr><td>" + line + "</td></tr></table>");
//  }
  }

  private static void showExamples(CheckCLDR checkCldr, String prettyPath, String localeID, ExampleGenerator exampleGenerator, String path, String value, String fullPath, String example, ExampleContext exampleContext) {
//  if (example != null) {
//  showValue(prettyPath, localeID, example, path, value, fullPath, "ok", exampleContext);
//  }
    if (SHOW_EXAMPLES == Zoomed.IN) {
      String longExample = exampleGenerator.getExampleHtml(path, value, ExampleGenerator.Zoomed.IN, exampleContext, ExampleType.NATIVE);
      if (longExample != null && !longExample.equals(example)) {
        showValue(checkCldr.getCldrFileToCheck(), prettyPath, localeID, longExample, path, value, fullPath, "ok", Subtype.none, exampleContext);
      }
      String help = exampleGenerator.getHelpHtml(path, value);
      if (help != null) {
        showValue(checkCldr.getCldrFileToCheck(), prettyPath, localeID, help, path, value, fullPath, "ok", Subtype.none, exampleContext);
      }
    }
  }

  private static void addPrettyPaths(CLDRFile file, Matcher pathFilter, PrettyPath prettyPathMaker, boolean noaliases, boolean filterDraft, Collection<String> target) {
//  Status pathStatus = new Status();
    for (Iterator<String> pit = file.iterator(pathFilter); pit.hasNext();) {
      String path = pit.next();
      if (file.isPathExcludedForSurvey(path)) {
        continue;
      }
      addPrettyPath(file, prettyPathMaker, noaliases, filterDraft, target, path);
    }
  }

  private static void addPrettyPaths(CLDRFile file, Collection<String> paths, Matcher pathFilter, PrettyPath prettyPathMaker, boolean noaliases, boolean filterDraft, Collection<String> target) {
//  Status pathStatus = new Status();
    for (String path : paths) {
      if (pathFilter != null && !pathFilter.reset(path).matches()) continue;
      addPrettyPath(file, prettyPathMaker, noaliases, filterDraft, target, path);
    }
  }

  private static void addPrettyPath(CLDRFile file, PrettyPath prettyPathMaker, boolean noaliases,
          boolean filterDraft, Collection<String> target, String path) {
    if (noaliases && XMLSource.Alias.isAliasPath(path)) { // this is just for console testing, the survey tool shouldn't do it.
      return;
//    file.getSourceLocaleID(path, pathStatus);
//    if (!path.equals(pathStatus.pathWhereFound)) {
//    continue;
//    }
    }
    if (filterDraft) {
      String newPath = CLDRFile.getNondraftNonaltXPath(path);
      if (!newPath.equals(path)) {
        String value = file.getStringValue(newPath);
        if (value != null) {
          return;
        }
      }
    }
    String prettyPath = prettyPathMaker.getPrettyPath(path, true); // get sortable version
    target.add(prettyPath);
  }
  public static synchronized void setDisplayInformation(CLDRFile inputDisplayInformation, ExampleGenerator inputExampleGenerator) {
    CheckCLDR.setDisplayInformation(inputDisplayInformation);
    englishExampleGenerator = inputExampleGenerator;
  }
  public static synchronized void setExampleGenerator(ExampleGenerator inputExampleGenerator) {
    englishExampleGenerator = inputExampleGenerator;
  }
  public static synchronized ExampleGenerator getExampleGenerator() {
    return englishExampleGenerator;
  }

  private static ExampleGenerator englishExampleGenerator;
  private static Object lastLocaleID = null;

  static Matcher coverageMatcher = Pattern.compile("meet ([a-z]*) coverage").matcher(""); // HACK TODO fix


  private static void showHeaderLine() {
    if (SHOW_LOCALE) System.out.println("Locale\tStatus\tPPath\t〈Eng.Value〉\t【Eng.Ex.】\t〈Loc.Value〉\t«fill-in»\t【Loc.Ex】\tError/Warning Msg\tFull Path\tAliasedSource/Path?");
  }

  private static void showValue(CLDRFile cldrFile, String prettyPath, String localeID, String example, 
          String path, String value, String fullPath, String statusString,
          Subtype subType, ExampleContext exampleContext) {
    ErrorType shortStatus = ErrorType.fromStatusString(statusString);
    subtotalCount.add(shortStatus, 1);
    totalCount.add(shortStatus, 1);
    if (subType == null) {
      subType = Subtype.none;
    }

    if (ErrorFile.errorFileWriter == null) {
      example = example == null ? "" : example;
      String englishExample = null;
      final String englishPathValue = path == null ? null : getEnglishPathValue(path);
      if (SHOW_EXAMPLES != null && path != null) {
        if (getExampleGenerator() == null) {
          setExampleGenerator(new ExampleGenerator(CheckCLDR.getDisplayInformation(), Utility.SUPPLEMENTAL_DIRECTORY));
        }
        englishExample = getExampleGenerator().getExampleHtml(path, englishPathValue, ExampleGenerator.Zoomed.OUT, exampleContext, ExampleType.ENGLISH);
      }
      englishExample = englishExample == null ? "" : englishExample;
      String cleanPrettyPath = path == null ? null : prettyPathMaker.getOutputForm(prettyPath);
      Status status = new Status();
      String sourceLocaleID = path == null ? null : cldrFile.getSourceLocaleID(path, status);
      String fillinValue = path == null ? null : cldrFile.getFillInValue(path);
      fillinValue = fillinValue == null ? "" : fillinValue.equals(value) ? "=" : fillinValue;

      final String otherSource = path == null ? null 
              : (sourceLocaleID.equals(localeID) ? "" 
                      : "\t" + sourceLocaleID);
      final String otherPath = path == null ? null 
              : (status.pathWhereFound.equals(path) ? "" 
                      : "\t" + status.pathWhereFound);
      System.out.println(
              getLocaleAndName(localeID)
              //+ "\t" + subtotalCount.getCount(shortStatus)
              + "\t" + shortStatus
              + "\t" + cleanPrettyPath
              + "\t〈" + englishPathValue + "〉"
              + "\t【" + englishExample + "】"
              + "\t〈" + value + "〉"
              + "\t«" + fillinValue + "»"
              + "\t【" + example + "】"
              + "\t" + statusString
              + "\t" + fullPath
              + otherSource
              + otherPath
      );
    } else if (ErrorFile.errorFileWriter != null) {
      if (shortStatus == ErrorType.contributed) {
        return;
      }
      if (shortStatus == ErrorType.posix) {
        shortStatus = ErrorType.minimal;
      }
      if (!localeID.equals(lastHtmlLocaleID)) {
        ErrorFile.writeErrorCountsText();
        //startGeneratedTable(generated_html, generated_html_table);
        lastHtmlLocaleID = localeID;
      }
      addError(localeID, path, shortStatus);
      //ErrorFile.htmlErrorsPerBaseLanguage.increment(shortStatus);

      //String menuPath = path == null ? null : PathUtilities.xpathToMenu(path);
      //String link = path == null ? null : "http://unicode.org/cldr/apps/survey?_=" + localeID + "&x=" + menuPath;
      ErrorFile.addDataToErrorFile(localeID, path, value, shortStatus, subType);
    }
    if (PATH_IN_COUNT && ErrorFile.generated_html_count != null) {
      ErrorFile.generated_html_count.println(lastHtmlLocaleID + ";\tpath:\t" + path);
    }
  }

  private static void addError(String localeID, String path, ErrorType shortStatus) {
    if (ErrorType.showInSummary.contains(shortStatus)) {
      ErrorFile.htmlErrorsPerLocale.increment(shortStatus);
    }
  }

  static String lastHtmlLocaleID = "";
  private static VoteResolver<String> voteResolver;
  private static String resolveVotesDirectory;

  private static String safeForHtml(String value) {
    return value == null ? "" : TransliteratorUtilities.toHTML.transliterate(value);
  }

  public static class PathShower {
    String localeID;
    boolean newLocale = true;
    String lastPath;
    String[] lastSplitPath;
    boolean showEnglish;
    String splitChar = "/";

    static final String lead = "****************************************";

    public void set(String localeID) {
      this.localeID = localeID;
      newLocale = true;
      LocaleIDParser localeIDParser = new LocaleIDParser();
      showEnglish = !localeIDParser.set(localeID).getLanguageScript().equals("en");
      //localeID.equals(CheckCLDR.displayInformation.getLocaleID());
      lastPath = null;
      lastSplitPath = null;
    }

    public void setDisplayInformation(CLDRFile displayInformation) {
      setDisplayInformation(displayInformation); 
    }

    public void showHeader(String path, String value) {
      if (newLocale) {
        System.out.println("Locale:\t" + getLocaleAndName(localeID));
        newLocale = false;
      }
      if (path.equals(lastPath)) return;

//    This logic keeps us from splitting on an attribute value that contains a /
//    such as time zone names.

      StringBuffer newPath = new StringBuffer();
      boolean inQuotes = false;
      for ( int i = 0 ; i < path.length() ; i++ ) {
        if ( (path.charAt(i) == '/') && !inQuotes )
          newPath.append('%');
        else
          newPath.append(path.charAt(i));

        if ( path.charAt(i) == '\"' )
          inQuotes = !inQuotes;
      }

      String[] splitPath = newPath.toString().split("%");

      for (int i = 0; i < splitPath.length; ++i) {
        if (lastSplitPath != null && i < lastSplitPath.length && splitPath[i].equals(lastSplitPath[i])) {
          continue;
        }
        lastSplitPath = null; // mark so we continue printing now
        System.out.print(lead.substring(0,i));
        System.out.print(splitPath[i]);
        if (i == splitPath.length - 1) {
          showValue(path, value, showEnglish, localeID);        
        } else {
          System.out.print(":");
        }
        System.out.println();       
      }
//    String prettierPath = path;
//    if (false) {
//    prettierPath = prettyPath.transliterate(path);
//    }

      lastPath = path;
      lastSplitPath = splitPath;
    }

    public String getSplitChar() {
      return splitChar;
    }

    public PathShower setSplitChar(String splitChar) {
      this.splitChar = splitChar;
      return this;
    }
  }
  private static void showValue(String path, String value, boolean showEnglish, String localeID) {
    System.out.println( "\tValue:\t" + value + (showEnglish ? "\t" + getEnglishPathValue(path) : "") + "\tLocale:\t" + localeID);
  }

  private static String getEnglishPathValue(String path) {
    String englishValue = CheckCLDR.getDisplayInformation().getWinningValue(path);
    if (englishValue == null) {
      String path2 = CLDRFile.getNondraftNonaltXPath(path);
      englishValue = CheckCLDR.getDisplayInformation().getWinningValue(path2);
    }
    return englishValue;
  }

  /**
   * Utility for getting information.
   * @param locale
   * @return
   */
  public static String getLocaleAndName(String locale) {
    String localizedName = CheckCLDR.getDisplayInformation().getName(locale);
    if (localizedName == null || localizedName.equals(locale)) return locale;
    return locale + " [" + localizedName + "]";
  }

  /**
   * Utility for getting information.
   * @param locale
   * @param linkToXml TODO
   * @return
   */
  public static String getNameAndLocale(String locale, boolean linkToXml) {
    String localizedName = CheckCLDR.getDisplayInformation().getName(locale);
    if (localizedName == null || localizedName.equals(locale)) return locale;
    if (linkToXml) {
      locale = "<a href='http://unicode.org/cldr/data/common/main/" + locale + ".xml'>" + locale + "</a>";
    }
    return localizedName  + " [" + locale + "]";
  }

  public static String getLocaleName(String locale) {
    String localizedName = CheckCLDR.getDisplayInformation().getName(locale);
    if (localizedName == null || localizedName.equals(locale)) return locale;
    return localizedName;
  }
  public static String getLinkedLocale(String locale) {
    return "<a href='http://unicode.org/cldr/apps/survey?_=" + locale + "'>" + locale + "</a>";
  }
}