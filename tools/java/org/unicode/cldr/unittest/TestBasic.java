package org.unicode.cldr.unittest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.test.TestFmwk;

public class TestBasic extends TestFmwk {
  static TestInfo testInfo = TestInfo.getInstance();

  /**
   * Simple test that loads each file in the cldr directory, thus verifying that
   * the DTD works, and also checks that the PrettyPaths work.
   * 
   * @author markdavis
   */

  public static void main(String[] args) {
    new TestBasic().run(args);
  }

  private static final Set<String> skipAttributes    = new HashSet<String>(Arrays.asList("alt", "draft",
                                                     "references"));
  private static final Matcher  skipPaths = Pattern.compile("/identity" + "|/alias" + "|\\[@alt=\"proposed")
  .matcher("");


  private final String           localeRegex = Utility.getProperty("locale", ".*");

  private final boolean          showInfo          = Utility.getProperty("showinfo", false);

  private final String           commonDirectory  = Utility.COMMON_DIRECTORY;

  private final String           mainDirectory = Utility.MAIN_DIRECTORY;

  //private final boolean          showForceZoom = Utility.getProperty("forcezoom", false);

  private final boolean          resolved = Utility.getProperty("resolved", false);

  private final Exception[]      internalException = new Exception[1];
  
  private boolean pretty = Utility.getProperty("pretty", true);

  public void TestDtds() throws IOException {
    checkDtds(mainDirectory);
    checkDtds(commonDirectory + "/supplemental");
    checkDtds(commonDirectory + "/collation");
    checkDtds(commonDirectory + "/segments");
    checkDtds(commonDirectory + "/test");
    checkDtds(commonDirectory + "/transforms");
  }

  private void checkDtds(String directory) throws IOException {
    File directoryFile = new File(directory);
    File[] listFiles = directoryFile.listFiles();
    String canonicalPath = directoryFile.getCanonicalPath();
    if (listFiles == null) {
      throw new IllegalArgumentException("Empty directory: " + canonicalPath);
    }
    logln("Checking files for DTD errors in: " + canonicalPath);
    for (File fileName : listFiles) {
      if (!fileName.toString().endsWith(".xml")) {
        continue;
      }
      check(fileName);
    }
  }

  class MyErrorHandler implements ErrorHandler {
    public void error(SAXParseException exception) throws SAXException {
      errln("error: " + XMLFileReader.showSAX(exception));
      throw exception;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
      errln("fatalError: " + XMLFileReader.showSAX(exception));
      throw exception;
    }

    public void warning(SAXParseException exception) throws SAXException {
      errln("warning: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
  }

  public void check(File systemID) {
    try {
      FileInputStream fis = new FileInputStream(systemID);
      XMLReader xmlReader = XMLFileReader.createXMLReader(true);
      xmlReader.setErrorHandler(new MyErrorHandler());
      InputSource is = new InputSource(fis);
      is.setSystemId(systemID.toString());
      xmlReader.parse(is);
      fis.close();
    } catch (SAXParseException e) {
      errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" + e.getMessage());
    } catch (SAXException e) {
      errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" + e.getMessage());
    } catch (IOException e) {
      errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" + e.getMessage());
    }
  }


  public void TestPaths() {
    Relation<String, String> distinguishing = new Relation(new TreeMap(), TreeSet.class, null);
    Relation<String, String> nonDistinguishing = new Relation(new TreeMap(), TreeSet.class, null);
    XPathParts parts = new XPathParts();
    Factory cldrFactory = Factory.make(mainDirectory, localeRegex);
    CLDRFile english = cldrFactory.make("en", true);

    Relation<String, String> pathToLocale = new Relation(new TreeMap(CLDRFile.ldmlComparator),
            TreeSet.class, null);
    for (String locale : cldrFactory.getAvailable()) {
      // if (locale.equals("root") && !localeRegex.equals("root"))
      // continue;
      CLDRFile file = cldrFactory.make(locale, resolved);
      if (file.isNonInheriting())
        continue;
      DisplayAndInputProcessor displayAndInputProcessor = new DisplayAndInputProcessor(file);

      logln(locale + "\t-\t" + english.getName(locale));

      for (Iterator<String> it = file.iterator(); it.hasNext();) {
        String path = it.next();
        if (path.endsWith("/alias")) {
          continue;
        }
        String value = file.getStringValue(path);
        if (value == null) {
          throw new IllegalArgumentException(locale + "\tError: in null value at " + path);
        }
        String displayValue = displayAndInputProcessor.processForDisplay(path, value);
        if (!displayValue.equals(value)) {
          logln("\t" + locale + "\tdisplayAndInputProcessor changes display value <" + value
                  + ">\t=>\t<" + displayValue + ">\t\t" + path);
        }
        String inputValue = displayAndInputProcessor.processInput(path, value, internalException);
        if (internalException[0] != null) {
          errln("\t" + locale + "\tdisplayAndInputProcessor internal error <" + value + ">\t=>\t<"
                  + inputValue + ">\t\t" + path);
          internalException[0].printStackTrace(System.out);
        }
        if (isVerbose() && !inputValue.equals(value)) {
          displayAndInputProcessor.processInput(path, value, internalException); // for
                                                                                  // debugging
          logln("\t" + locale + "\tdisplayAndInputProcessor changes input value <" + value
                  + ">\t=>\t<" + inputValue + ">\t\t" + path);
        }

        pathToLocale.put(path, locale);

        // also check for non-distinguishing attributes
        if (path.contains("/identity"))
          continue;

        // make sure we don't have problem alts
        if (false && path.contains("proposed")) {
          String sourceLocale = file.getSourceLocaleID(path, null);
          if (locale.equals(sourceLocale)) {
            String nonAltPath = file.getNondraftNonaltXPath(path);
            if (!path.equals(nonAltPath)) {
              String nonAltLocale = file.getSourceLocaleID(nonAltPath, null);
              String nonAltValue = file.getStringValue(nonAltPath);
              if (nonAltValue == null || !locale.equals(nonAltLocale)) {
                errln("\t" + locale + "\tProblem alt=proposed <" + value + ">\t\t" + path);
              }
            }
          }
        }

        String fullPath = file.getFullXPath(path);
        parts.set(fullPath);
        for (int i = 0; i < parts.size(); ++i) {
          if (parts.getAttributeCount(i) == 0)
            continue;
          String element = parts.getElement(i);
          for (String attribute : parts.getAttributeKeys(i)) {
            if (skipAttributes.contains(attribute))
              continue;
            if (CLDRFile.isDistinguishing(element, attribute)) {
              distinguishing.put(element, attribute);
            } else {
              nonDistinguishing.put(element, attribute);
            }
          }
        }
      }
    }

    if (isVerbose()) {

      System.out.format("Distinguishing Elements: %s" + Utility.LINE_SEPARATOR, distinguishing);
      System.out.format("Nondistinguishing Elements: %s" + Utility.LINE_SEPARATOR, nonDistinguishing);
      System.out.format("Skipped %s" + Utility.LINE_SEPARATOR, skipAttributes);

      logln(Utility.LINE_SEPARATOR + "Paths to skip in Survey Tool");
      for (String path : pathToLocale.keySet()) {
        if (CheckCLDR.skipShowingInSurvey.matcher(path).matches()) {
          logln("Skipping: " + path);
        }
      }

      logln(Utility.LINE_SEPARATOR + "Paths to force zoom in Survey Tool");
      for (String path : pathToLocale.keySet()) {
        if (CheckCLDR.FORCE_ZOOMED_EDIT.matcher(path).matches()) {
          logln("Forced Zoom Edit: " + path);
        }
      }
    }

    if (pretty) {
      if (showInfo) {
        logln(Utility.LINE_SEPARATOR + "Showing Path to PrettyPath mapping" + Utility.LINE_SEPARATOR);
      }
      PrettyPath prettyPath = new PrettyPath().setShowErrors(true);
      Set<String> badPaths = new TreeSet();
      for (String path : pathToLocale.keySet()) {
        String prettied = prettyPath.getPrettyPath(path, false);
        if (showInfo)
          logln(prettied + "\t\t" + path);
        if (prettied.contains("%%") && !path.contains("/alias")) {
          badPaths.add(path);
        }
      }
      // now remove root

      if (showInfo) {
        logln(Utility.LINE_SEPARATOR + "Showing Paths not in root" + Utility.LINE_SEPARATOR);
      }

      CLDRFile root = cldrFactory.make("root", true);
      for (Iterator<String> it = root.iterator(); it.hasNext();) {
        pathToLocale.removeAll(it.next());
      }
      if (showInfo)
        for (String path : pathToLocale.keySet()) {
          if (skipPaths.reset(path).find()) {
            continue;
          }
          logln(path + "\t" + pathToLocale.getAll(path));
        }

      if (badPaths.size() != 0) {
        errln("Error: " + badPaths.size()
                + " Paths were not prettied: use -DSHOW and look for ones with %% in them.");
      }
    }
  }

  public void TestAPath() {
    // <month type="1">1</month>
    String path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]";
    CLDRFile root = testInfo.getRoot();
    logln("path: " + path);
    String fullpath = root.getFullXPath(path);
    logln("fullpath: " + fullpath);
    String value = root.getStringValue(path);
    logln("value: " + value);
    Status status = new Status();
    String source = root.getSourceLocaleID(path, status);
    logln("locale: " + source);
    logln("status: " + status);
  }
  
  public void TestDefaultContents() {
    Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();
    for (String locale : defaultContents) {
      CLDRFile cldrFile;
      try {
        cldrFile = testInfo.getCldrFactory().make(locale, false);
      } catch (RuntimeException e) {
        logln("Can't open default content file:\t" + locale);
        continue;
      }
      for (Iterator<String> it = cldrFile.iterator(); it.hasNext();) {
        String path = it.next();
        if (path.contains("/identity")) {
          continue;
        }
        errln("Default content file not empty:\t" + locale);
        showDifferences(locale);
        break;
      }
    }
  }

  private void showDifferences(String locale) {
    CLDRFile cldrFile = testInfo.getCldrFactory().make(locale, false);
    final String localeParent = cldrFile.getParent(locale);
    CLDRFile parentFile = testInfo.getCldrFactory().make(localeParent, true);
    int funnyCount = 0;
    for (Iterator<String> it = cldrFile.iterator("", CLDRFile.ldmlComparator); it.hasNext();) {
      String path = it.next();
      if (path.contains("/identity")) {
        continue;
      }
      final String fullXPath = cldrFile.getFullXPath(path);
      if (fullXPath.contains("[@draft=\"unconfirmed\"]") || fullXPath.contains("[@draft=\"provisional\"]")) {
        funnyCount++;
        continue;
      }
      logln("\tpath:\t" + path);
      logln("\t\t" + locale + " value:\t<" + cldrFile.getStringValue(path) + ">");
      final String parentFullPath = parentFile.getFullXPath(path);
      logln("\t\t" + localeParent + " value:\t<" + parentFile.getStringValue(path) + ">");
      logln("\t\t" + locale + " fullpath:\t" + fullXPath);
      logln("\t\t" + localeParent + " fullpath:\t" + parentFullPath);
        }
    logln("\tCount of non-approved:\t" + funnyCount);
  }
}
