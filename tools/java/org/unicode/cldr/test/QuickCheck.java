package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Log;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
  private static final Set skipAttributes = new HashSet(Arrays.asList(new String[]{
      "alt", "draft", "references"}));
  
  private static String localeRegex;
  
  private static boolean showInfo = false;
  
  private static String commonDirectory;
  private static String mainDirectory;
  
  private static boolean showForceZoom;
  
  private static boolean resolved;
  
  private static Exception[] internalException = new Exception[1];

  private static boolean verbose;
  
  public static void main(String[] args) throws IOException {
    verbose = Utility.getProperty("verbose","false","true").matches("(?i)T|TRUE");
    localeRegex = Utility.getProperty("locale", ".*");
    
    showInfo = Utility.getProperty("showinfo","false","true").matches("(?i)T|TRUE");
    
    commonDirectory = Utility.COMMON_DIRECTORY; // Utility.getProperty("common", Utility.COMMON_DIRECTORY);
    //if (commonDirectory == null) commonDirectory = Utility.COMMON_DIRECTORY
    //System.out.println("Main Source Directory: " + commonDirectory + "\t\t(to change, use -DSOURCE=xxx, eg -DSOURCE=C:/cvsdata/unicode/cldr/incoming/proposed/main)");

    mainDirectory = Utility.getProperty("main", Utility.COMMON_DIRECTORY + "/main");
    //System.out.println("Main Source Directory: " + commonDirectory + "\t\t(to change, use -DSOURCE=xxx, eg -DSOURCE=C:/cvsdata/unicode/cldr/incoming/proposed/main)");

    showForceZoom = Utility.getProperty("forcezoom","false","true").matches("(?i)T|TRUE");
    
    resolved = Utility.getProperty("resolved","false","true").matches("(?i)T|TRUE");
    
    boolean paths = Utility.getProperty("paths", "true").matches("(?i)T|TRUE");
    
    pretty = Utility.getProperty("pretty", "true").matches("(?i)T|TRUE");
    
    double startTime = System.currentTimeMillis();
    checkDtds();
    double deltaTime = System.currentTimeMillis() - startTime;
    System.out.println("Elapsed: " + deltaTime/1000.0 + " seconds");
    
    if (paths) {
      System.out.println("Checking paths");
      checkPaths();
      deltaTime = System.currentTimeMillis() - startTime;
      System.out.println("Elapsed: " + deltaTime/1000.0 + " seconds");
      System.out.println("Basic Test Passes");
    }
  }
  
  private static void checkDtds() throws IOException {
    checkDtds(mainDirectory);
    checkDtds(commonDirectory + "supplemental");
    checkDtds(commonDirectory + "collation");
    checkDtds(commonDirectory + "segments");
    checkDtds(commonDirectory + "test");
    checkDtds(commonDirectory + "transforms");
  }
  
  private static void checkDtds(String directory) throws IOException {
    File directoryFile = new File(directory);
    File[] listFiles = directoryFile.listFiles();
    String canonicalPath = directoryFile.getCanonicalPath();
    if (listFiles == null) {
      throw new IllegalArgumentException("Empty directory: " + canonicalPath);
    }
    System.out.println("Checking files for DTD errors in: " + canonicalPath);
    for (File fileName : listFiles) {
      if (!fileName.toString().endsWith(".xml")) {
        continue;
      }
      check(fileName);
    }
  }
  
  static class MyErrorHandler implements ErrorHandler {
    public void error(SAXParseException exception) throws SAXException {
      System.out.println("\r\nerror: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
    public void fatalError(SAXParseException exception) throws SAXException {
      System.out.println("\r\nfatalError: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
    public void warning(SAXParseException exception) throws SAXException {
      System.out.println("\r\nwarning: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
  }
  
  public static void check(File systemID) {
    try {
      FileInputStream fis = new FileInputStream(systemID);
      XMLReader xmlReader = XMLFileReader.createXMLReader(true);
      xmlReader.setErrorHandler(new MyErrorHandler());
      InputSource is = new InputSource(fis);
      is.setSystemId(systemID.toString());
      xmlReader.parse(is);
      fis.close();
    } catch (SAXParseException e) {
      System.out.println("\t" + "Can't read " + systemID);
      System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
    } catch (SAXException e) {
      System.out.println("\t" + "Can't read " + systemID);
      System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
    } catch (IOException e) {
      System.out.println("\t" + "Can't read " + systemID);
      System.out.println("\t" + e.getClass() + "\t" + e.getMessage());
    }      
  }
  
  static Matcher skipPaths = Pattern.compile("/identity" + "|/alias" + "|\\[@alt=\"proposed").matcher("");
  
  private static boolean pretty;
  
  private static void checkPaths() {
    Relation<String,String> distinguishing = new Relation(new TreeMap(), TreeSet.class, null);
    Relation<String,String> nonDistinguishing = new Relation(new TreeMap(), TreeSet.class, null);
    XPathParts parts = new XPathParts();
    Factory cldrFactory = Factory.make(mainDirectory, localeRegex);
    CLDRFile english = cldrFactory.make("en", true);
    
    Relation<String, String> pathToLocale = new Relation(new TreeMap(CLDRFile.ldmlComparator), TreeSet.class, null);
    for (String locale : cldrFactory.getAvailable()) {
//      if (locale.equals("root") && !localeRegex.equals("root"))
//        continue;
      CLDRFile file;
      try {
        file = cldrFactory.make(locale, resolved);
      } catch (Exception e) {
        System.out.println("\r\nfatalError: " + e.getMessage());
        continue;
      }
      if (file.isNonInheriting())
        continue;
      DisplayAndInputProcessor displayAndInputProcessor = new DisplayAndInputProcessor(file);
      
      System.out.println(locale + "\t-\t" + english.getName(locale));
      
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
        if (false && !displayValue.equals(value)) {
          System.out.println("\t" + locale + "\tdisplayAndInputProcessor changes display value <" + value + ">\t=>\t<" + displayValue + ">\t\t" + path);
        }
        String inputValue = displayAndInputProcessor.processInput(path, value, internalException);
        if (internalException[0] != null) {
          System.out.println("\t" + locale + "\tdisplayAndInputProcessor internal error <" + value + ">\t=>\t<" + inputValue + ">\t\t" + path);
          internalException[0].printStackTrace(System.out);
        }
        if (verbose && !inputValue.equals(value)) {
          displayAndInputProcessor.processInput(path, value, internalException); // for debugging
          System.out.println("\t" + locale + "\tdisplayAndInputProcessor changes input value <" + value + ">\t=>\t<" + inputValue + ">\t\t" + path);
        }
        
        pathToLocale.put(path, locale);
        
        // also check for non-distinguishing attributes
        if (path.contains("/identity")) continue;
        
        // make sure we don't have problem alts
        if (path.contains("proposed")) {
          String sourceLocale = file.getSourceLocaleID(path, null);
          if (locale.equals(sourceLocale)) {
            String nonAltPath = file.getNondraftNonaltXPath(path);
            if (!path.equals(nonAltPath)) {
              String nonAltLocale = file.getSourceLocaleID(nonAltPath, null);
              String nonAltValue = file.getStringValue(nonAltPath);
              if (nonAltValue == null || !locale.equals(nonAltLocale)) {
                System.out.println("\t" + locale + "\tProblem alt=proposed <" + value + ">\t\t" + path);
              }
            }
          }
        }
        
        String fullPath = file.getFullXPath(path);
        parts.set(fullPath);
        for (int i = 0; i < parts.size(); ++i) {
          if (parts.getAttributeCount(i) == 0) continue;
          String element = parts.getElement(i);
          for (String attribute : parts.getAttributeKeys(i)) {
            if (skipAttributes.contains(attribute)) continue;
            if (CLDRFile.isDistinguishing(element, attribute)) {
              distinguishing.put(element, attribute);
            } else {
              nonDistinguishing.put(element, attribute);
            }
          }
        }
      }
    }
    System.out.println();
    
    System.out.format("Distinguishing Elements: %s" + Utility.LINE_SEPARATOR, distinguishing);
    System.out.format("Nondistinguishing Elements: %s" + Utility.LINE_SEPARATOR, nonDistinguishing);
    System.out.format("Skipped %s" + Utility.LINE_SEPARATOR, skipAttributes);
    
    if (verbose) {
      System.out.println(Utility.LINE_SEPARATOR + "Paths to skip in Survey Tool");
      for (String path : pathToLocale.keySet()) {
        if (CheckCLDR.skipShowingInSurvey.matcher(path).matches()) {
          System.out.println("Skipping: " + path);
        }
      }
      
      System.out.println(Utility.LINE_SEPARATOR + "Paths to force zoom in Survey Tool");
      for (String path : pathToLocale.keySet()) {
        if (CheckCLDR.FORCE_ZOOMED_EDIT.matcher(path).matches()) {
          System.out.println("Forced Zoom Edit: " + path);
        }
      }
    }
    
    if (pretty) {
      if (showInfo) {
        System.out.println(Utility.LINE_SEPARATOR + "Showing Path to PrettyPath mapping" + Utility.LINE_SEPARATOR);
      }
      PrettyPath prettyPath = new PrettyPath().setShowErrors(true);
      Set<String> badPaths = new TreeSet();
      for (String path : pathToLocale.keySet()) {
        String prettied = prettyPath.getPrettyPath(path, false);
        if (showInfo) System.out.println(prettied + "\t\t" + path);
        if (prettied.contains("%%") && !path.contains("/alias")) {
          badPaths.add(path);
        }
      }
      // now remove root
      
      if (showInfo) {
        System.out.println(Utility.LINE_SEPARATOR + "Showing Paths not in root" + Utility.LINE_SEPARATOR);
      }
      
      CLDRFile root = cldrFactory.make("root", true);
      for (Iterator<String> it = root.iterator(); it.hasNext();) {
        pathToLocale.removeAll(it.next());
      }
      if (showInfo) for (String path : pathToLocale.keySet()) {
        if (skipPaths.reset(path).find()) {
          continue;
        }
        System.out.println(path + "\t" + pathToLocale.getAll(path));
      }
      
      if (badPaths.size() != 0) {
        System.out.println("Error: " + badPaths.size() + " Paths were not prettied: use -DSHOW and look for ones with %% in them.");
      }
    }
  }
  
}