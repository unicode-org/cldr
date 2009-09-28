/*
 ******************************************************************************
 * Copyright (C) 2004-2009 International Business Machines Corporation and    *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.icu;

import com.ibm.icu.dev.test.util.ElapsedTimer;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.TimeZone;

import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.ant.CLDRConverterTool;
import org.unicode.cldr.icu.LDML2ICUBinaryWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import javax.xml.transform.TransformerException;

/**
 * Converts from LDML files (from the CLDR project) into ICU text or binary format.
 *
 * @author Ram Viswanadha
 * @author Brian Rower - Added Binary file writing and fixed memory leak - June 2008
 */
public class LDML2ICUConverter extends CLDRConverterTool {
  /**
   * These must be kept in sync with getOptions().
   */
  private static final int HELP1 = 0;
  private static final int HELP2 = 1;
  private static final int SOURCEDIR = 2;
  private static final int DESTDIR = 3;
  private static final int SPECIALSDIR = 4;
  private static final int WRITE_DEPRECATED = 5;
  private static final int WRITE_DRAFT = 6;
  private static final int SUPPLEMENTALDIR = 7;
  private static final int SUPPLEMENTALONLY = 8;
  private static final int METAZONE_ONLY = 9;
  private static final int LIKELYSUBTAGS_ONLY = 10;
  private static final int PLURALS_ONLY = 11;
  private static final int NUMBERS_ONLY = 12;
  private static final int WRITE_BINARY = 13;
  private static final int VERBOSE = 14;
  private static final int ASCII_NUMBERS = 15;

  private static final UOption[] options = new UOption[] {
    UOption.HELP_H(),
    UOption.HELP_QUESTION_MARK(),
    UOption.SOURCEDIR(),
    UOption.DESTDIR(),
    UOption.create("specialsdir", 'p', UOption.REQUIRES_ARG),
    UOption.create("write-deprecated", 'w', UOption.REQUIRES_ARG),
    UOption.create("write-draft", 'f', UOption.NO_ARG),
    UOption.create("supplementaldir", 'm', UOption.REQUIRES_ARG),
    UOption.create("supplemental-only", 'l', UOption.NO_ARG),
    UOption.create("metazone-only", 'z', UOption.NO_ARG),
    UOption.create("likely-only", 't', UOption.NO_ARG),
    UOption.create("plurals-only", 'r', UOption.NO_ARG),
    UOption.create("numbers-only", 'n', UOption.NO_ARG),
    UOption.create("write-binary", 'b', UOption.NO_ARG),
    UOption.VERBOSE(),
    UOption.create("ascii-numbers", 'a', UOption.NO_ARG),
  };

  private String sourceDir;
  private String fileName;
  private String destDir;
  private String specialsDir;
  private String supplementalDir;
  private boolean writeDeprecated;
  private boolean writeDraft;
  private boolean writeSupplemental;
  private boolean writeMetazone;
  private boolean writeLikelySubtags;
  private boolean writePlurals;
  private boolean writeNumberingSystems;
  private boolean writeBinary;
  private boolean verbose;
  private boolean asciiNumbers;

  private String factoryDir;
  /**
   * Add comments on the item to indicate where fallbacks came from. Good for
   * information, bad for diffs.
   */
  private static boolean verboseFallbackComments;

  private static final String LINESEP = System.getProperty("line.separator");
  private static final String BOM = "\uFEFF";
  private static final String CHARSET = "UTF-8";
  private static final String DEPRECATED_LIST = "icu-config.xml & build.xml";

  private Document fullyResolvedDoc;
  private Document specialsDoc;
  private String locName;
  private Document supplementalDoc;
  private Document metazoneDoc;
  private Document likelySubtagsDoc;
  private Document pluralsDoc;
  private Document numberingSystemsDoc;
  private static final boolean DEBUG = false;

  // TODO: hard-coded file names for now
  private static final String supplementalDataFile = "supplementalData.xml";
  private static final String metazoneInfoFile = "metazoneInfo.xml";
  private static final String likelySubtagsFile = "likelySubtags.xml";
  private static final String pluralsFile = "plurals.xml";
  private static final String numberingSystemsFile = "numberingSystems.xml";

  private List xpathList = new ArrayList();

  private CLDRFile.Factory cldrFactory;
  private CLDRFile.Factory specialsFactory;
  private SupplementalDataInfo supplementalDataInfo;
  // TreeMap overrideMap = new TreeMap(); // list of locales to take regardless of draft status.
  // Written by writeDeprecated

  public static void main(String[] args) {
    LDML2ICUConverter cnv = new LDML2ICUConverter();
    cnv.processArgs(args);
  }

  private void usage() {
    System.out.println(
        "\nUsage: LDML2ICUConverter [OPTIONS] [FILES]\nLDML2ICUConverter [OPTIONS] " +
                  "-w [DIRECTORY]\n" +
        "This program is used to convert LDML files to ICU ResourceBundle TXT files.\n" +
        "Please refer to the following options. Options are not case sensitive.\n" +
        "Options:\n" +
        "-s or --sourcedir          source directory for files followed by path, " +
                                   "default is current directory.\n" +
        "-d or --destdir            destination directory, followed by the path, "+
                                   "default is current directory.\n" +
        "-p or --specialsdir        source directory for files containing special data " +
                                   "followed by the path. None if not specified\n" +
        "-f or --write-draft        write data for LDML nodes marked draft.\n" +
        "-m or --suplementaldir     source directory for finding the supplemental data.\n" +
        "-l or --supplemental-only  read " + supplementalDataFile + " file from the given " +
                                   "directory and write appropriate files to destination " +
                                   "directory\n" +
        "-t or --likely-only        read " + likelySubtagsFile + " file from the given directory " +
                                   "and write appropriate files to destination directory\n" +
        "-r or --plurals-only       read " + pluralsFile + " file from the given directory and " +
                                   "write appropriate files to destination directory\n" +
        "-z or --metazone-only      read " + metazoneInfoFile + " file from the given directory " +
                                   "and write appropriate files to destination directory\n" +
        "-n or --numbers-only       read " + numberingSystemsFile + " file from the given " +
                                   "directory and write appropriate files to destination " +
                                   "directory\n" +
        "-w [dir] or --write-deprecated [dir]   write data for deprecated locales. 'dir' is a " +
                                   "directory of source xml files.\n" +
        "-b or --write-binary       write data in binary (.res) files rather than .txt\n" +
        "-h or -? or --help         this usage text.\n" +
        "-v or --verbose            print out verbose output.\n" +
        "-a or --ascii-numbers      do ASCII-only numbers.\n" +
        "example: org.unicode.cldr.icu.LDML2ICUConverter -s xxx -d yyy en.xml");
    System.exit(-1);
  }

  private void printInfo(String message) {
    if (verbose) {
      System.out.println("INFO : " + message);
    }
  }

  private void printXPathWarning(InputLocale loc, String xpath) {
    // int len = xpath.length();
    // getXPath(node, xpath);
    System.err.println("WARNING : Not producing resource for : "
            + xpath.toString());
    // xpath.setLength(len);
  }

  /**
   * @deprecated
   * @param node
   * @param xpath
   */
  private void printXPathWarning(Node node, StringBuffer xpath) {
    int len = xpath.length();
    getXPath(node, xpath);
    System.err.println("WARNING : Not producing resource for : " + xpath.toString());
    xpath.setLength(len);
  }

  private void printWarning(String fileName, String message) {
    System.err.println(fileName + ": WARNING : " + message);
  }

  private void printError(String fileName, String message) {
    System.err.println(fileName + ": ERROR : " + message);
  }

  /*
   * First method called from the main method. Will check all the args
   * and direct us from there.
   * If not doing anything special, just taking in XML files and writing
   * TXT or Binary files, then will call processFile()
   */
  public void processArgs(String[] args) {
    int remainingArgc = 0;
    // for some reason when
    // Class classDefinition = Class.forName(className);
    // object = classDefinition.newInstance();
    // is done then the options are not reset!!
    for(int i = 0; i <options.length; i++) {
      options[i].doesOccur = false;
    }
    try {
      remainingArgc = UOption.parseArgs(args, options);
    } catch (Exception e) {
      printError("","(parsing args): " + e.toString());
      e.printStackTrace();
      usage();
    }
    if (args.length == 0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
      usage();
    }

    if (options[SOURCEDIR].doesOccur) {
      sourceDir = options[SOURCEDIR].value;
    }
    if (options[DESTDIR].doesOccur) {
      destDir = options[DESTDIR].value;
    }
    if (options[SPECIALSDIR].doesOccur) {
      specialsDir = options[SPECIALSDIR].value;
    }
    if (options[WRITE_DRAFT].doesOccur) {
      writeDraft = true;
    }
    if (options[SUPPLEMENTALDIR].doesOccur) {
      supplementalDir = options[SUPPLEMENTALDIR].value;
    }
    if (options[SUPPLEMENTALONLY].doesOccur) {
      writeSupplemental = true;
    }
    if (options[METAZONE_ONLY].doesOccur) {
      writeMetazone = true;
    }
    if (options[LIKELYSUBTAGS_ONLY].doesOccur) {
      writeLikelySubtags = true;
    }
    if (options[PLURALS_ONLY].doesOccur) {
      writePlurals = true;
    }
    if (options[NUMBERS_ONLY].doesOccur) {
      writeNumberingSystems = true;
    }
    if (options[WRITE_BINARY].doesOccur) {
      writeBinary = true;
    }
    if (options[VERBOSE].doesOccur) {
      verbose = true;
    }
    if (options[ASCII_NUMBERS].doesOccur) {
      asciiNumbers = true;
    }
    if (destDir == null) {
      destDir = ".";
    }
    if (options[WRITE_DEPRECATED].doesOccur) {
      writeDeprecated = true;
      if (remainingArgc > 0) {
        printError("", "-w takes one argument, the directory, and no other XML files.\n");
        usage();
        return; // NOTREACHED
      }
      writeDeprecated();
      return;
    }
    //if ((writeDraft == false) && (specialsDir != null)) {
    // printInfo("Reading alias table searching for draft overrides");
    // writeDeprecated(); // actually just reads the alias
    //}
    if (remainingArgc == 0 && (getLocalesMap()== null || getLocalesMap().size()== 0)) {
      printError("", "No files specified for processing. Please check the arguments and try again");
      usage();
    }
    if (supplementalDir != null) {
      // supplementalFileName = LDMLUtilities.getFullPath(LDMLUtilities.XML, "supplementalData",
      // supplementalDir);
      supplementalDoc = createSupplementalDoc();
      supplementalDataInfo = SupplementalDataInfo.getInstance(supplementalDir);
      metazoneDoc = createMetazoneDoc();
      likelySubtagsDoc = createLikelySubtagsDoc();
      pluralsDoc = createPluralsDoc();
      numberingSystemsDoc = createNumberingSystemsDoc();
    }
    if (writeSupplemental == true) {
      makeXPathList(supplementalDoc);
      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      System.out.println("Processing: " + supplementalDataFile);
      ICUResourceWriter.Resource res = parseSupplemental(supplementalDoc, supplementalDataFile);

      if (res != null && ((ICUResourceWriter.ResourceTable)res).first != null) {
        // write out the bundle
        writeResource(res, supplementalDataFile);
      }
    } else if (writeMetazone == true) {
      makeXPathList(metazoneDoc);
      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      System.out.println("Processing: " + metazoneInfoFile);
      ICUResourceWriter.Resource res = parseMetazoneFile(metazoneDoc, metazoneInfoFile);

      if (res != null && ((ICUResourceWriter.ResourceTable)res).first != null) {
        writeResource(res, metazoneInfoFile);
      }
    } else if (writeLikelySubtags == true) {
      makeXPathList(likelySubtagsDoc);
      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      System.out.println("Processing: " + likelySubtagsFile);
      ICUResourceWriter.Resource res = parseLikelySubtagsFile(likelySubtagsDoc, likelySubtagsFile);

      if (res != null && ((ICUResourceWriter.ResourceTable)res).first != null) {
        writeResource(res, likelySubtagsFile);
      }
    } else if (writePlurals == true) {
      makeXPathList(pluralsDoc);
      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      System.out.println("Processing: " + pluralsFile);
      ICUResourceWriter.Resource res = parsePluralsFile(pluralsDoc, pluralsFile);

      if (res != null && ((ICUResourceWriter.ResourceTable)res).first != null) {
        writeResource(res, pluralsFile);
      }
    } else if (writeNumberingSystems == true) {
      makeXPathList(numberingSystemsDoc);
      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      System.out.println("Processing: " + numberingSystemsFile);
      ICUResourceWriter.Resource res = parseNumberingSystemsFile(
          numberingSystemsDoc, numberingSystemsFile);

      if (res != null && ((ICUResourceWriter.ResourceTable)res).first != null) {
        // write out the bundle
        writeResource(res, numberingSystemsFile);
      }
    } else {
      if (getLocalesMap() != null && getLocalesMap().size() > 0) {
        factoryDir = sourceDir;
        for(Iterator iter = getLocalesMap().keySet().iterator(); iter.hasNext();) {
          String fileName = (String) iter.next();
          String draft = (String) getLocalesMap().get(fileName);
          if (draft != null && !draft.equals("false")) {
            writeDraft = true;
          } else {
            writeDraft = false;
          }
          this.fileName = fileName; // Scoping.
          processFile(fileName);
        }
      } else if (remainingArgc > 0) {
        factoryDir = sourceDir;
        for (int i = 0; i < remainingArgc; i++) {
          if (args[i].equals("*")) {
            for (String file : new File(factoryDir).list()) {
              if (!file.endsWith(".xml")) {
                continue;
              }
              processFile(file);
            }
          } else {
            processFile(args[i]);
          }
        }
      } else {
        printError("", "No files specified !");
      }
    }
  }

  private class InputLocale {
    boolean notOnDisk;
    String locale;
    CLDRFile rawFile;
    CLDRFile file;
    CLDRFile specialsFile;
    private CLDRFile fResolved;

    public String toString() {
      return "{"
        + "notOnDisk=" + notOnDisk
        + " locale=" + locale
        + " rawFile=" + abbreviated(rawFile)
        + " file=" + abbreviated(file)
        + " specialsFile=" + abbreviated(specialsFile)
        + " fResolved=" + abbreviated(fResolved)
        + "}";
    }

    private String abbreviated(Object raw) {
      if (raw == null) {
        return null;
      }
      String result = raw.toString();
      if (result.length() <= 100) {
        return result;
      }
      return result.substring(0, 100) + "...";
    }

    public CLDRFile resolved() {
      if (fResolved == null) {
        // System.err.println("** spinning up resolved for " + locale);
        if (cldrFactory != null) {
          fResolved = cldrFactory.make(locale, true, DraftStatus.contributed);
        } else {
          System.err.println("Error: cldrFactory is null in \"resolved()\"");
          System.err.flush();
          System.exit(1);
        }
      }
      return fResolved;
    }

    InputLocale(CLDRFile fromFile) {
      specialsDoc = null; // reset
      notOnDisk = true;
      rawFile = file = fResolved = fromFile;
      locale = file.getLocaleID();
    }

    InputLocale(String locale) {
      specialsDoc = null; // reset
      this.locale = locale;
      rawFile = cldrFactory.make(locale, false);
      if (specialsFactory != null) {
        String icuSpecialFile = specialsDir + "/" + locale + ".xml";
        if (new File(icuSpecialFile).exists()) {
          printInfo("Parsing ICU specials from: " + icuSpecialFile);
          specialsFile = specialsFactory.make(locale, false);
          file = (CLDRFile) rawFile.cloneAsThawed();
          file.putAll(specialsFile, CLDRFile.MERGE_REPLACE_MINE);
        } else {
          file = rawFile;
        }
      } else {
        file = rawFile; // frozen
      }
    }

    private XPathParts xpp = new XPathParts(null, null);

    Set<String> getByType(String baseXpath, String element) {
      return getByType(baseXpath, element, LDMLConstants.TYPE);
    }

    Set<String> getByType(String baseXpath, String element, String attribute) {
      Set<String> typeList = new HashSet<String >();
      for (Iterator<String> iter = file.iterator(baseXpath); iter.hasNext();) {
        String somePath = iter.next();
        String type = getAttributeValue(somePath, element, attribute);
        if (type == null) {
          continue;
        } else {
          typeList.add(type);
        }
      }
      return typeList;
    }

    // convenience functions
    String getXpathName(String xpath) {
      xpp.set(xpath);
      return xpp.getElement(-1);
    }

    String getXpathName(String xpath, int pos) {
      xpp.set(xpath);
      return xpp.getElement(pos);
    }

    String getAttributeValue(String xpath, String element, String attribute) {
      xpp.set(xpath);
      int el = xpp.findElement(element);
      if (el == -1) {
        return null;
      }
      return xpp.getAttributeValue(el, attribute);
    }

    String getAttributeValue(String xpath, String attribute) {
      xpp.set(xpath);
      return xpp.getAttributeValue(-1, attribute);
    }

    String getBasicAttributeValue(String xpath, String attribute) {
      String fullPath = file.getFullXPath(xpath);
      if (fullPath == null) {
        // System.err.println("No full path for " + xpath);
        return null;
      } else {
        // System.err.println(" >>> " + fullPath);
      }
      return getAttributeValue(fullPath, attribute);
    }

    String getBasicAttributeValue(CLDRFile whichFile, String xpath, String attribute) {
      String fullPath = whichFile.getFullXPath(xpath);
      if (fullPath == null) {
        // System.err.println("No full path for " + xpath);
        return null;
      } else {
        // System.err.println(" >>> " + fullPath);
      }
      return getAttributeValue(fullPath, attribute);
    }

    String findAttributeValue(String xpath, String attribute) {
      String fullPath = file.getFullXPath(xpath);
      xpp.set(fullPath);
      for (int j = 1; j <= xpp.size(); j++) {
        String v = xpp.getAttributeValue(0 - j, attribute);
        if (v != null)
          return v;
      }
      return null;
    }

    String getResolvedString(String xpath) {
      String rv = file.getStringValue(xpath);
      if (rv == null) {
        rv = resolved().getStringValue(xpath);
        // System.err.println("Fallin back:" + xpath + " -> " + rv);
      }
      return rv;
    }

    Set<String> alreadyDone = new HashSet<String>();

    /**
     * Determine whether a particular section has been done
     *
     * @param where
     *            the name of the section, i.e. LDMLConstants.IDENTITY
     * @return true if this part has already been processed, otherwise
     *         false. If false, it will return true the next time called.
     */
    boolean beenHere(String where) {
      if (alreadyDone.contains(where)) {
        return true;
      }

      alreadyDone.add(where);
      return false;
    }

    boolean isPathNotConvertible(String xpath) {
      return isPathNotConvertible(file, xpath);
    }

    boolean isPathNotConvertible(CLDRFile f, String xpath) {
      String alt = getBasicAttributeValue(f, xpath, "alt");
      if (alt != null) {
        return true;
      }
      return !(xpathList.contains(f.getFullXPath(xpath))||!f.isHere(xpath));
    }

    // ====== DOM compatibility
    Document doc = null;

    /**
     * Parse the current locale for DOM, and fetch a specific node.
     */
    Node getNode(String xpath) {
      return LDMLUtilities.getNode(getDocument(), xpath);
    }

    /**
     * Get the node of the 'top' item named. Similar to DOM-based parseBundle()
     * @param name
     * @return
     */
    Node getTopNode(String topName) {
      StringBuffer xpath = new StringBuffer();
      xpath.append("//ldml");
      //            int savedLength = xpath.length();
      Node ldml = null;

      for (ldml = getDocument().getFirstChild(); ldml != null; ldml = ldml.getNextSibling()) {
        if (ldml.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        String name = ldml.getNodeName();
        if (name.equals(LDMLConstants.LDML)) {
          ldmlVersion = LDMLUtilities.getAttributeValue(ldml, LDMLConstants.VERSION);
          // if (LDMLUtilities.isLocaleDraft(ldml) && !isDraftStatusOverridable(locName) &&
          // writeDraft == false) {
          //     System.err.println("WARNING: The LDML file " + sourceDir+ "/" + locName +
          // ".xml is marked draft ! Not producing ICU file. ");
          //System.exit(-1);
          //     return null;
          // }
          break;
        }
      }

      if (ldml == null) {
        throw new RuntimeException("ERROR: no <ldml > node found in parseBundle()");
      }

      for (Node node = ldml.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node.getNodeType()!= Node.ELEMENT_NODE) {
          continue;
        }
        String name = node.getNodeName();
        if (topName.equals(name)) {
          return node;
        }
      }

      return null;
    }

    Document getSpecialsDoc() {
      if (specialsDoc == null) {
        if (specialsDir != null) {
          String icuSpecialFile = specialsDir + "/" + fileName;
          if (new File(icuSpecialFile).exists()) {
            specialsDoc = LDMLUtilities.parseAndResolveAliases(fileName, specialsDir, false, false);
            /*
            try {
                OutputStreamWriter writer = new
                OutputStreamWriter(
                    new FileOutputStream("./" + File.separator + fileName + "_debug.xml"),"UTF-8");
                LDMLUtilities.printDOMTree(fullyResolvedSpecials,new PrintWriter(writer));
                writer.flush();
            } catch(IOException e) {
                  //throw the exceptionaway .. this is for debugging
            }
             */
          } else {
            if (ULocale.getCountry(locName).length()== 0) {
              printWarning(
                  icuSpecialFile, "ICU special not found for language-locale \"" + locName + "\"");
              //System.exit(-1);
            } else {
              System.err.println("ICU special " + icuSpecialFile + " not found, continuing.");
            }
            specialsDoc = null;
          }
        }
      }

      return specialsDoc;
    }

    /**
     * Parse the current locale for DOM.
     * @return
     */
    Document getDocument() {
      if (notOnDisk) {
        throw new InternalError(
            "Error: this locale (" + locale + ") isn't on disk, can't parse with DOM.");
      }

      if (doc == null) {
        String xmlfileName = LDMLUtilities.getFullPath(LDMLUtilities.XML,locale + ".xml",sourceDir);
        String fileName = locale + ".xml";
        //printInfo("Parsing: " + xmlfileName);
        String icuSpecialFile ="";
        getSpecialsDoc();

        doc = LDMLUtilities.parse(xmlfileName, false);
        if (specialsDoc != null) {
          StringBuffer xpath = new StringBuffer();
          doc = (Document) LDMLUtilities.mergeLDMLDocuments(
              doc, specialsDoc, xpath, icuSpecialFile, specialsDir, false, true);
          /*
            try {
                OutputStreamWriter writer = new
                OutputStreamWriter(
                    new FileOutputStream("./" + File.separator + fileName + "_debug.xml"),"UTF-8");
                LDMLUtilities.printDOMTree(fullyResolvedDoc,new PrintWriter(writer), "","");
                writer.flush();
            } catch (IOException e) {
                  //throw the exceptionaway .. this is for debugging
            }
           */
        }

        /*
         * debugging code
         *
         * try {
         *      Document doc = LDMLUtilities.getFullyResolvedLDML(sourceDir,
         *      fileName, false);
         *      OutputStreamWriter writer = new
         *      OutputStreamWriter(new FileOutputStream(
         *          "./" + File.separator + fileName + "_debug.xml"),"UTF-8");
         *      LDMLUtilities.printDOMTree(doc,new PrintWriter(writer));
         *      writer.flush();
         * } catch(IOException e) {
         *      //throw the exception away .. this is for debugging
         * }
         */
        if (!LDMLUtilities.isLocaleAlias(doc)) {
          fullyResolvedDoc = LDMLUtilities.getFullyResolvedLDML(
              sourceDir, fileName, false, false, false, false);
        } else {
          fullyResolvedDoc = null;
        }
        if ((writeDraft == false) && (isDraftStatusOverridable(locale))) {
          printInfo("Overriding draft status, and including: " + locale);
          writeDraft = true;
          // TODO: save/restore writeDraft
        }
        makeXPathList(doc);
      }
      return doc;
    }
  }

  /*
   * Sets some stuff up and calls createResourceBundle
   */
  private void processFile(String fileName) {
    // add 1 below to skip past the separator
    int lastIndex = fileName.lastIndexOf(File.separator, fileName.length()) + 1;
    fileName = fileName.substring(lastIndex, fileName.length());
    String xmlfileName = LDMLUtilities.getFullPath(LDMLUtilities.XML, fileName, sourceDir);

    locName = fileName;
    int index = locName.indexOf(".xml");
    if (index > -1) {
      locName = locName.substring(0, index);
    }

    if (cldrFactory == null) {
      printInfo("* Spinning up CLDRFactory on " + sourceDir);
      cldrFactory = CLDRFile.Factory.make(factoryDir, ".*");
      if (specialsDir != null) {
        printInfo("* Spinning up specials CLDRFactory on " + specialsDir);
        specialsFactory = CLDRFile.Factory.make(specialsDir, ".*");
      }
    }

    // if (CLDRFile.isSupplementalName(localeID)) continue;
    // if
    // (supplementalDataInfo.getDefaultContentLocales().contains(localeID))
    // {
    // boolean isLanguageLocale =
    // localeID.equals(localeIDParser.set(localeID).getLanguageScript());

    System.out.println("Processing: " + xmlfileName);
    ElapsedTimer timer = new ElapsedTimer();
    InputLocale loc = new InputLocale(locName);

    // printInfo("Parsing: " + xmlfileName);
    String icuSpecialFile = "";
    if (specialsDir != null) {
      icuSpecialFile = specialsDir + "/" + fileName;
      if (!new File(icuSpecialFile).exists()) {
        if (ULocale.getCountry(locName).length() == 0) {
          printWarning(icuSpecialFile, "ICU special not found for language-locale \""
                       + locName + "\"");
          // System.exit(-1);
        } else {
          printInfo("ICU special " + icuSpecialFile + " not found, continuing.");
        }
        specialsDoc = null;
      }
    }

    if ((writeDraft == false) && (isDraftStatusOverridable(locName))) {
      printInfo("Overriding draft status, and including: " + locName);
      writeDraft = true;
      // TODO: save/restore writeDraft
    }
    // System.out.println("Creating the resource bundle.");
    createResourceBundle(loc);
    printInfo("Elapsed time: " + timer + "s");
  }

  private void makeXPathList(Node node, StringBuffer xpath) {
    if (xpath == null) {
      xpath = new StringBuffer("/");
    }
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = child.getNodeName();
      int savedLength = xpath.length();
      xpath.append("/");
      xpath.append(name);
      LDMLUtilities.appendXPathAttribute(child, xpath, false, false);
      if (name.equals("collation")) {
        // special case for collation: draft attribute is set on the top level element
        xpathList.add(xpath.toString());
      } else if (LDMLUtilities.areChildrenElementNodes(child)) {
        makeXPathList(child, xpath);
      } else{
        //reached leaf node add to list
        xpathList.add(xpath.toString());
      }
      xpath.delete(savedLength, xpath.length());
    }
  }

  private void makeXPathList(InputLocale loc) {
    xpathList.clear();
    for(Iterator<String> iter = loc.file.iterator(); iter.hasNext();) {
      xpathList.add(loc.file.getFullXPath(iter.next()));
    }

    makeXPathList((Node) supplementalDoc, null);
    Collections.sort(xpathList);
    if (DEBUG) {
      try {
        PrintWriter log1 = new PrintWriter(new FileOutputStream("log1.txt"));
        log1.println("BEGIN: Before computeConvertibleXPaths");
        for (int i = 0; i < xpathList.size(); i++) {
          log1.println((String) xpathList.get(i));
        }
        log1.println("END: Before computeConvertibleXPaths");
        log1.flush();
        log1.close();
      } catch (Exception ex) {
        // debugging, throw away.
      }
    }

    // Ok now figure out which XPaths should be converted
    xpathList = computeConvertibleXPaths(xpathList,
            exemplarsContainAZ(loc), locName, supplementalDir);
    if (DEBUG) {
      try {
        PrintWriter log2 = new PrintWriter(new FileOutputStream("log2.txt"));
        log2.println("BEGIN: After computeConvertibleXPaths");
        for (int i = 0; i < xpathList.size(); i++) {
          log2.println((String) xpathList.get(i));
        }
        log2.println("END: After computeConvertibleXPaths");
        log2.flush();
        log2.close();
      } catch (Exception ex) {
        // debugging, throw away.
      }
    }
  }

  private void makeXPathList(Document doc) {
    xpathList.clear();
    makeXPathList((Node)doc, null);
    makeXPathList((Node)supplementalDoc, null);
    Collections.sort(xpathList);
    if (DEBUG) {
      try {
        PrintWriter log1 = new PrintWriter(new FileOutputStream("log1.txt"));
        log1.println("BEGIN: Before computeConvertibleXPaths");
        for (int i = 0; i <xpathList.size(); i++) {
          log1.println((String)xpathList.get(i));
        }
        log1.println("END: Before computeConvertibleXPaths");
        log1.flush();
        log1.close();
      } catch(Exception ex) {
        // debugging throw away.
      }
    }

    // Ok now figure out which XPaths should be converted
    xpathList = computeConvertibleXPaths(xpathList, exemplarsContainAZ(fullyResolvedDoc), locName,
       supplementalDir);
    if (DEBUG) {
      try {
        PrintWriter log2 = new PrintWriter(new FileOutputStream("log2.txt"));
        log2.println("BEGIN: After computeConvertibleXPaths");
        for (int i = 0; i <xpathList.size(); i++) {
          log2.println((String)xpathList.get(i));
        }
        log2.println("END: After computeConvertibleXPaths");
        log2.flush();
        log2.close();
      } catch(Exception ex) {
        // debugging, throw away.
      }
    }
  }

  private boolean exemplarsContainAZ(Document fullyResolvedDoc) {
    if (fullyResolvedDoc == null) {
      return false;
    }

    Node node = LDMLUtilities.getNode(fullyResolvedDoc, "//ldml/characters/exemplarCharacters");
    if (node == null) {
      return false;
    }

    String ex = LDMLUtilities.getNodeValue(node);
    UnicodeSet set = new UnicodeSet(ex);
    return set.containsAll(new UnicodeSet("[A-Z a-z]"));
  }

  private boolean exemplarsContainAZ(InputLocale loc) {
    if (loc == null) {
      return false;
    }

    UnicodeSet set = loc.file.getExemplarSet("", CLDRFile.WinningChoice.WINNING);
    if (set == null) {
      set = loc.resolved().getExemplarSet("", CLDRFile.WinningChoice.WINNING);
      if (set == null) {
        return false;
      }
    }

    return set.containsAll(new UnicodeSet("[A-Z a-z]"));
  }

  private Document createSupplementalDoc() {
    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        if (name.matches(".*\\.xml") && !name.equals("characters.xml") &&
                !name.equals(metazoneInfoFile) && !name.equals(likelySubtagsFile)) {
          return true;
        }
        return false;
      }
    };

    File myDir = new File(supplementalDir);
    String[] files = myDir.list(filter);
    if (files == null) {
      String canonicalPath;
      try {
        canonicalPath = myDir.getCanonicalPath();
      } catch (IOException e) {
        canonicalPath = e.getMessage();
      }
      printError(fileName, "Supplemental files are missing " + canonicalPath);
      System.exit(-1);
    }

    Document doc = null;
    for (int i = 0; i < files.length; i++) {
      try {
        printInfo("Parsing document " + files[i]);
        String fileName = myDir.getAbsolutePath() + File.separator + files[i];
        Document child = LDMLUtilities.parse(fileName, false);
        if (doc == null) {
          doc = child;
          continue;
        }
        StringBuffer xpath = new StringBuffer();
        LDMLUtilities.mergeLDMLDocuments(doc, child, xpath, files[i], myDir.getAbsolutePath(), true,
           false);
      } catch (Throwable se) {
        printError(fileName , "Parsing: " + files[i] + " " + se.toString());
        se.printStackTrace();
        System.exit(1);
      }
    }

    return doc;
  }

  private Document createMetazoneDoc() {
    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        if (name.matches(metazoneInfoFile)) {
          return true;
        }
        return false;
      }
    };

    File myDir = new File(supplementalDir);
    String[] files = myDir.list(filter);
    Document doc = null;
    for (int i = 0; i <files.length; i++) {
      try {
        printInfo("Parsing document " + files[i]);
        String fileName = myDir.getAbsolutePath() + File.separator + files[i];
        Document child = LDMLUtilities.parse(fileName, false);
        if (doc == null) {
          doc = child;
          continue;
        }
        StringBuffer xpath = new StringBuffer();
        LDMLUtilities.mergeLDMLDocuments(doc, child, xpath, files[i], myDir.getAbsolutePath(), true,
            false);
      } catch (Throwable se) {
        printError(fileName , "Parsing: " + files[i] + " " + se.toString());
        se.printStackTrace();
        System.exit(1);
      }
    }

    return doc;
  }

  private Document createLikelySubtagsDoc() {
    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        if (name.matches(likelySubtagsFile)) {
          return true;
        }
        return false;
      }
    };

    File myDir = new File(supplementalDir);
    String[] files = myDir.list(filter);
    Document doc = null;
    for (int i = 0; i < files.length; i++) {
      try {
        printInfo("Parsing document " + files[i]);
        String fileName = myDir.getAbsolutePath() + File.separator + files[i];
        Document child = LDMLUtilities.parse(fileName, false);
        if (doc == null) {
          doc = child;
          continue;
        }
        StringBuffer xpath = new StringBuffer();
        LDMLUtilities.mergeLDMLDocuments(doc, child, xpath, files[i], myDir.getAbsolutePath(), true,
           false);
      } catch (Throwable se) {
        printError(fileName , "Parsing: " + files[i] + " " + se.toString());
        se.printStackTrace();
        System.exit(1);
      }
    }

    return doc;
  }

  private Document createPluralsDoc() {
    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        if (name.matches(pluralsFile)) {
          return true;
        }
        return false;
      }
    };

    File myDir = new File(supplementalDir);
    String[] files = myDir.list(filter);
    Document doc = null;
    for (int i = 0; i <files.length; i++) {
      try {
        printInfo("Parsing document " + files[i]);
        String fileName = myDir.getAbsolutePath() + File.separator + files[i];
        Document child = LDMLUtilities.parse(fileName, false);
        if (doc == null) {
          doc = child;
          continue;
        }
        StringBuffer xpath = new StringBuffer();
        LDMLUtilities.mergeLDMLDocuments(doc, child, xpath, files[i], myDir.getAbsolutePath(), true,
            false);
      } catch (Throwable se) {
        printError(fileName , "Parsing: " + files[i] + " " + se.toString());
        se.printStackTrace();
        System.exit(1);
      }
    }

    return doc;
  }

  private Document createNumberingSystemsDoc() {
    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        if (name.matches(numberingSystemsFile)) {
          return true;
        }
        return false;
      }
    };

    File myDir = new File(supplementalDir);
    String[] files = myDir.list(filter);
    Document doc = null;
    for (int i = 0; i <files.length; i++) {
      try {
        printInfo("Parsing document " + files[i]);
        String fileName = myDir.getAbsolutePath() + File.separator + files[i];
        Document child = LDMLUtilities.parse(fileName, false);
        if (doc == null) {
          doc = child;
          continue;
        }
        StringBuffer xpath = new StringBuffer();
        LDMLUtilities.mergeLDMLDocuments(doc, child, xpath, files[i], myDir.getAbsolutePath(), true,
            false);
      } catch (Throwable se) {
        printError(fileName , "Parsing: " + files[i] + " " + se.toString());
        se.printStackTrace();
        System.exit(1);
      }
    }

    return doc;
  }

  /*
   * Create the Resource tree, and then Call writeResource or LDML2ICUBinaryWriter.writeBinaryFile(),
   * whichever is appropriate
   */
  private void createResourceBundle(InputLocale loc) {
    try {
      // calculate the list of vettable xpaths.
      try {
        makeXPathList(loc);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Can't make XPathList for: " + loc).initCause(e);
      }

      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      ICUResourceWriter.Resource res = parseBundle(loc);
      if (res != null && ((ICUResourceWriter.ResourceTable) res).first != null) {
        if (loc.specialsFile != null) {
          String dir = specialsDir.replace('\\', '/');
          dir = "<path>" + dir.substring(dir.indexOf("/xml"), dir.length());
          if (res.comment == null) {
            res.comment = " ICU <specials > source: " + dir + "/" + locName + ".xml";
          } else {
            res.comment = res.comment + " ICU <specials > source: " + dir + "/" + locName + ".xml";
          }
        }
        // write out the bundle depending on if writing Binary or txt
        if (writeBinary) {
          specialsFactory = null;
          cldrFactory = null;
          LDML2ICUBinaryWriter.writeBinaryFile(res, destDir, loc.locale);
        } else {
          //allLocales = null;
          //specialsFactory = null;
          cldrFactory = null;
          String theFileName = sourceDir.replace('\\','/') + "/" + loc.locale + ".xml";
          writeResource(res, theFileName);
        }
      }
      // writeAliasedResource();
    } catch (Throwable se) {
      printError(loc.locale, "(parsing and writing) " + se.toString());
      se.printStackTrace();
      System.exit(1);
    }
  }

  /*
    private void createAliasedResource(Document doc, String xmlfileName, String icuSpecialFile) {
        if (locName == null || writeDeprecated == false) {
            return;
        }
        String lang = null; // REMOVE
        //String lang = (String) deprecatedMap.get(ULocale.getLanguage(locName));
        //System.out.println("In aliased resource");
        if (lang != null) {
            ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
            ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
            str.name = "\"%%ALIAS\"";
            if (lang.indexOf("_")<0) {
                table.name = lang;
                String c = ULocale.getCountry(locName);
                if (c != null && c.length()>0) {
                    table.name = lang + "_" + c;
                }
                str.val = locName;
            }else{
                table.name = lang;
                str.val = ULocale.getLanguage(locName);
            }
            table.first = str;
            writeResource(table, "");
        }
        //System.out.println("exiting aliased resource");
    }
   */

  private static final String LOCALE_SCRIPT = "LocaleScript";
  private static final String NUMBER_ELEMENTS = "NumberElements";
  private static final String NUMBER_PATTERNS = "NumberPatterns";
  private static final String AM_PM_MARKERS = "AmPmMarkers";
  private static final String DTP = "DateTimePatterns";
  public static final String DTE = "DateTimeElements";

  private static Map<String, String> keyNameMap = new TreeMap<String, String>();
  private static final Map<String, String> deprecatedTerritories = new TreeMap<String, String>();
  // TODO: should be a set?
  static {
    keyNameMap.put("days", "dayNames");
    keyNameMap.put("months", "monthNames");
    keyNameMap.put("territories", "Countries");
    keyNameMap.put("languages", "Languages");
    keyNameMap.put("languagesShort", "LanguagesShort");
    keyNameMap.put("currencies", "Currencies");
    keyNameMap.put("variants", "Variants");
    keyNameMap.put("scripts", "Scripts");
    keyNameMap.put("keys", "Keys");
    keyNameMap.put("types", "Types");
    keyNameMap.put("version", "Version");
    keyNameMap.put("exemplarCharacters", "ExemplarCharacters");
    keyNameMap.put("auxiliary", "AuxExemplarCharacters");
    keyNameMap.put("timeZoneNames", "zoneStrings");
    keyNameMap.put("localizedPatternChars", "localPatternChars");
    keyNameMap.put("paperSize", "PaperSize");
    keyNameMap.put("measurementSystem", "MeasurementSystem");
    keyNameMap.put("measurementSystemNames", "measurementSystemNames");
    keyNameMap.put("codePatterns", "codePatterns");
    keyNameMap.put("fractions", "CurrencyData");
    keyNameMap.put("quarters", "quarters");
    keyNameMap.put("displayName", "dn");
    keyNameMap.put("icu:breakDictionaryData", "BreakDictionaryData");
    deprecatedTerritories.put( "BQ" ,"");
    deprecatedTerritories.put( "CT" ,"");
    deprecatedTerritories.put( "DD" ,"");
    deprecatedTerritories.put( "FQ" ,"");
    deprecatedTerritories.put( "FX" ,"");
    deprecatedTerritories.put( "JT" ,"");
    deprecatedTerritories.put( "MI" ,"");
    deprecatedTerritories.put( "NQ" ,"");
    deprecatedTerritories.put( "NT" ,"");
    deprecatedTerritories.put( "PC" ,"");
    deprecatedTerritories.put( "PU" ,"");
    deprecatedTerritories.put( "PZ" ,"");
    deprecatedTerritories.put( "SU" ,"");
    deprecatedTerritories.put( "VD" ,"");
    deprecatedTerritories.put( "WK" ,"");
    deprecatedTerritories.put( "YD" ,"");
    //TODO: "FX",  "RO",  "TP",  "ZR",   /* obsolete country codes */
  }

  private static final String commentForCurrencyMeta =
      "Currency metadata.  Unlike the \"Currencies\" element, this is\n" +
      "NOT true locale data.  It exists only in root.  The two\n" +
      "integers are the fraction digits for each currency, and the\n" +
      "rounding increment.  The fraction digits must be an integer\n" +
      "from 0..9.  If there is no rounding, the rounding incrementis \n" +
      "zero.  Otherwise the rounding increment is given in units of\n" +
      "10^(-fraction_digits).  The special tag \"DEFAULT\" gives the\n" +
      "meta data for all currencies not otherwise listed.";

  private static final String commentForCurrencyMap =
      "Map from ISO 3166 country codes to ISO 4217 currency codes\n" +
      "NOTE: This is not true locale data; it exists only in ROOT";

  private static final String commentForTelephoneCodeData =
      "Map from territory codes to ITU telephone codes.\n" +
      "NOTE: This is not true locale data; it exists only in ROOT";

  private ICUResourceWriter.Resource parseMetazoneFile(Node root, String file) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    StringBuffer xpath = new StringBuffer();
    xpath.append("//");
    xpath.append(LDMLConstants.SUPPLEMENTAL_DATA);
    table.name = LDMLConstants.METAZONE_INFO;
    table.annotation = ICUResourceWriter.ResourceTable.NO_FALLBACK;
    int savedLength = xpath.length();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.SUPPLEMENTAL_DATA)) {
        node = node.getFirstChild();
        continue;
      } else if (name.equals(LDMLConstants.METAZONE_INFO)) {
        res = parseMetazoneInfo(node, xpath);
      } else {
        printError(file,"Encountered unknown element " + getXPath(node, xpath).toString());
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(savedLength,xpath.length());
    }

    return table;
  }

  private ICUResourceWriter.Resource parseLikelySubtagsFile(Node root, String file) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    StringBuffer xpath = new StringBuffer();
    xpath.append("//");
    xpath.append(LDMLConstants.SUPPLEMENTAL_DATA);
    table.name = LDMLConstants.LIKELY_SUBTAGS;
    table.annotation = ICUResourceWriter.ResourceTable.NO_FALLBACK;
    int savedLength = xpath.length();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.SUPPLEMENTAL_DATA)) {
        node = node.getFirstChild();
        continue;
      }
      if (name.equals(LDMLConstants.LIKELY_SUBTAGS)) {
        res = parseLikelySubtagsInfo(node, xpath);
      } else if (name.equals(LDMLConstants.VERSION) || name.equals(LDMLConstants.GENERATION)) {
        continue;
      } else {
        printError(file,"Encountered unknown element " + getXPath(node, xpath).toString());
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(savedLength,xpath.length());
    }

    return table;
  }

  private ICUResourceWriter.Resource parsePluralsFile(Node root, String file) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    StringBuffer xpath = new StringBuffer();
    xpath.append("//");
    xpath.append(LDMLConstants.SUPPLEMENTAL_DATA);
    table.name = LDMLConstants.PLURALS;
    table.annotation = ICUResourceWriter.ResourceTable.NO_FALLBACK;
    int savedLength = xpath.length();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.SUPPLEMENTAL_DATA)) {
        node = node.getFirstChild();
        continue;
      }
      if (name.equals(LDMLConstants.PLURALS)) {
        res = parsePluralsInfo(node, xpath);
      } else if (name.equals(LDMLConstants.VERSION) || name.equals(LDMLConstants.GENERATION)) {
        continue;
      } else {
        printError(file,"Encountered unknown element " + getXPath(node, xpath).toString());
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(savedLength,xpath.length());
    }

    return table;
  }

  private ICUResourceWriter.Resource parseNumberingSystemsFile(Node root, String file) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    StringBuffer xpath = new StringBuffer();
    xpath.append("//");
    xpath.append(LDMLConstants.SUPPLEMENTAL_DATA);
    table.name = LDMLConstants.NUMBERING_SYSTEMS;
    table.annotation = ICUResourceWriter.ResourceTable.NO_FALLBACK;
    int savedLength = xpath.length();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.SUPPLEMENTAL_DATA)) {
        node = node.getFirstChild();
        continue;
      }

      if (name.equals(LDMLConstants.NUMBERING_SYSTEMS)) {
        res = parseNumberingSystemsInfo(node, xpath);
      } else if (name.equals(LDMLConstants.VERSION) || name.equals(LDMLConstants.GENERATION)) {
        continue;
      } else {
        printError(file,"Encountered unknown element " + getXPath(node, xpath).toString());
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(savedLength,xpath.length());
    }

    return table;
  }

  private ICUResourceWriter.Resource parseSupplemental(Node root, String file) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    StringBuffer xpath = new StringBuffer();
    xpath.append("//");
    xpath.append(LDMLConstants.SUPPLEMENTAL_DATA);
    table.name = LDMLConstants.SUPPLEMENTAL_DATA;
    table.annotation = ICUResourceWriter.ResourceTable.NO_FALLBACK;
    int savedLength = xpath.length();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.SUPPLEMENTAL_DATA)) {
        // if (isNodeNotConvertible(node,xpath) && writeDraft == false) {
        //    printWarning(
        //        file, "The " + supplementalDataFile +
        //        " file is marked draft ! Not producing ICU file. ");
        //    System.exit(-1);
        //    return null;
        // }
        node = node.getFirstChild();
        continue;
      }
      if (name.equals(LDMLConstants.SPECIAL)) {
        /*
         * IGNORE SPECIALS
         * FOR NOW
         */
        node = node.getFirstChild();
        continue;
      } else if (name.equals(LDMLConstants.CURRENCY_DATA)) {
        res = parseCurrencyData(node, xpath);
      } else if (name.equals(LDMLConstants.TERRITORY_CONTAINMENT)) {
        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseTerritoryContainment(node, xpath);
      } else if (name.equals(LDMLConstants.LANGUAGE_DATA)) {
        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseLanguageData(node, xpath);
      } else if (name.equals(LDMLConstants.TERRITORY_DATA)) {
        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseTerritoryData(node, xpath);
      } else if (name.equals(LDMLConstants.META_DATA)) {
        //Ignore this
        //if (DEBUG)printXPathWarning(node, xpath);
      } else if (name.equals(LDMLConstants.TERRITORY_INFO)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.CODE_MAPPINGS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.REFERENCES)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.VERSION)) {
        //Ignore this
        //if (DEBUG)printXPathWarning(node, xpath);
      } else if (name.equals(LDMLConstants.GENERATION)) {
        //Ignore this
        //if (DEBUG)printXPathWarning(node, xpath);
      } else if (name.equals(LDMLConstants.CALENDAR_DATA)) {
        //Ignore this
        //res = parseCalendarData(node, xpath);
      } else if (name.equals(LDMLConstants.CALENDAR_PREFERENCE_DATA)) {
        res = parseCalendarPreferenceData(node, xpath);
      } else if (name.equals(LDMLConstants.TIMEZONE_DATA)) {
        res = parseTimeZoneData(node, xpath);
      } else if (name.equals(LDMLConstants.WEEK_DATA)) {
        //res = parseWeekData(node, xpath);
      } else if (name.equals(LDMLConstants.CHARACTERS)) {
        //continue .. these are required for posix
      } else if (name.equals(LDMLConstants.MEASUREMENT_DATA)) {
        //res = parseMeasurementData(node, xpath);
        if (DEBUG)printXPathWarning(node, getXPath(node, xpath));
      } else if (name.equals(LDMLConstants.LIKELY_SUBTAGS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.PLURALS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.NUMBERING_SYSTEMS)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.POSTAL_CODE_DATA)) {
        //Ignore this
      } else if (name.equals(LDMLConstants.CLDR_VERSION)) {
        res = parseCLDRVersion(node, xpath);
      } else if (name.equals(LDMLConstants.TELEPHONE_CODE_DATA)) {
        res = addTelephoneCodeData(); // uses SupplementalDataInfo, doesn't need node, xpath
      } else if (name.equals(LDMLConstants.BCP47_KEYWORD_MAPPINGS)) {
        res = parseBCP47MappingData(node, xpath);
      } else if (name.equals(LDMLConstants.LANGUAGE_MATCHING)) {
        // Ignore this
      } else {
        printError(file,"Encountered unknown element " + getXPath(node, xpath).toString());
        System.exit(-1);
      }
      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(savedLength,xpath.length());
    }

    return table;
  }

  private ICUResourceWriter.Resource parseMetazoneInfo(Node root, StringBuffer xpath) {
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.ResourceTable mzInfo = new ICUResourceWriter.ResourceTable();
    mzInfo.name = LDMLConstants.METAZONE_MAPPINGS;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.TIMEZONE)) {
        ICUResourceWriter.Resource current_mz = null;
        ICUResourceWriter.ResourceTable mzTable = new ICUResourceWriter.ResourceTable();
        mzTable.name = "\"" + LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE).
            replaceAll("/",":") + "\"";
        int mz_count = 0;

        for (Node node2 = node.getFirstChild(); node2 != null; node2 = node2.getNextSibling()) {
          if (node2.getNodeType()!= Node.ELEMENT_NODE) {
            continue;
          }

          String name2 = node2.getNodeName();
          if (name2.equals(LDMLConstants.USES_METAZONE)) {
            ICUResourceWriter.ResourceArray this_mz = new ICUResourceWriter.ResourceArray();
            ICUResourceWriter.ResourceString mzone = new ICUResourceWriter.ResourceString();
            ICUResourceWriter.ResourceString from = new ICUResourceWriter.ResourceString();
            ICUResourceWriter.ResourceString to = new ICUResourceWriter.ResourceString();

            this_mz.name = "mz" + String.valueOf(mz_count);
            this_mz.first = mzone;
            mzone.next = from;
            from.next = to;
            mz_count++;

            mzone.val = LDMLUtilities.getAttributeValue(node2, LDMLConstants.MZONE);
            String str = LDMLUtilities.getAttributeValue(node2, LDMLConstants.FROM);
            if (str != null) {
              from.val = str;
            } else {
              from.val = "1970-01-01 00:00";
            }

            str = LDMLUtilities.getAttributeValue(node2, LDMLConstants.TO);
            if (str != null) {
              to.val = str;
            } else {
              to.val = "9999-12-31 23:59";
            }

            if (current_mz == null) {
              mzTable.first = this_mz;
              current_mz = findLast(this_mz);
            } else {
              current_mz.next = this_mz;
              current_mz = findLast(this_mz);
            }
          }
        }

        if (current == null) {
          mzInfo.first = mzTable;
          current = findLast(mzTable);
        } else {
          current.next = mzTable;
          current = findLast(mzTable);
        }
      }
    }

    if (mzInfo.first != null) {
      return mzInfo;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseLikelySubtagsInfo(Node root, StringBuffer xpath) {
    ICUResourceWriter.Resource first = null;
    ICUResourceWriter.Resource current = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.LIKELY_SUBTAG)) {
        ICUResourceWriter.ResourceString subtagString = new ICUResourceWriter.ResourceString();
        subtagString.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.FROM);
        subtagString.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.TO);

        if (current == null) {
          first = current = subtagString;
        } else {
          current.next = subtagString;
        }
        current = subtagString;
      }
    }

    return first;
  }

  private ICUResourceWriter.Resource parsePluralsInfo(Node root, StringBuffer xpath) {
    int currentSetNumber = 1;
    ICUResourceWriter.ResourceTable localesTable = new ICUResourceWriter.ResourceTable();
    localesTable.name = LDMLConstants.LOCALES;

    ICUResourceWriter.ResourceTable ruleSetsTable = new ICUResourceWriter.ResourceTable();
    ruleSetsTable.name = LDMLConstants.RULES;

    // The ruleSetsTable is a sibling of the locales table.
    localesTable.next = ruleSetsTable;

    for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (!name.equals(LDMLConstants.PLURAL_RULES)) {
        System.err.println("Encountered element " + name + " processing plurals.");
        System.exit(-1);
      }

      ICUResourceWriter.ResourceTable currentSetTable = null;
      ICUResourceWriter.ResourceString currentRuleString = null;
      Node child = node.getFirstChild();

      String locales = LDMLUtilities.getAttributeValue(node, LDMLConstants.LOCALES);
      String [] localesArray = locales.split("\\s");

      if (child == null) {
        // Create empty resource strings with the locale as the ID.
        for (int i = 0; i < localesArray.length; ++i) {
          ICUResourceWriter.ResourceString localeString =
            new ICUResourceWriter.ResourceString(localesArray[i], "");
          localesTable.appendContents(localeString);
        }
      } else {
        do {
          if (child.getNodeType() == Node.ELEMENT_NODE) {
            String childName = child.getNodeName();
            if (!childName.equals(LDMLConstants.PLURAL_RULE)) {
              System.err.println("Encountered element " + childName + " processing plurals.");
              System.exit(-1);
            }

            // This creates a rule string for the current rule set
            ICUResourceWriter.ResourceString ruleString = new ICUResourceWriter.ResourceString();
            ruleString.name = LDMLUtilities.getAttributeValue(child, LDMLConstants.COUNT);
            ruleString.val = LDMLUtilities.getNodeValue(child);

            // Defer the creation of the table until the first
            // rule for the locale, since there are some locales
            // with no rules, and we don't want those in the
            // ICU resource file.
            if (currentSetTable != null) {
              currentRuleString.next = ruleString;
            } else {
              currentSetTable = new ICUResourceWriter.ResourceTable();
              String currentSetName = new String("set") + currentSetNumber;
              ++currentSetNumber;
              currentSetTable.name = currentSetName;
              currentSetTable.first = ruleString;
              ruleSetsTable.appendContents(currentSetTable);

              // Now that we've created a rule set table, we can put all of the
              // locales for this rule set into the locales table.
              for (int i = 0; i < localesArray.length; ++i) {
                ICUResourceWriter.ResourceString localeString =
                  new ICUResourceWriter.ResourceString(localesArray[i], currentSetName);
                localesTable.appendContents(localeString);
              }
            }
            currentRuleString = ruleString;
          }
          child = child.getNextSibling();
        }
        while(child != null);
      }
    }

    return localesTable.first == null ? null : localesTable;
  }

  private ICUResourceWriter.Resource parseNumberingSystemsInfo(Node root, StringBuffer xpath) {
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.ResourceTable ns = new ICUResourceWriter.ResourceTable();
    ns.name = LDMLConstants.NUMBERING_SYSTEMS;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.NUMBERING_SYSTEM)) {
        ICUResourceWriter.ResourceTable nsTable = new ICUResourceWriter.ResourceTable();
        nsTable.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.ID);
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);

        ICUResourceWriter.ResourceInt radix = new ICUResourceWriter.ResourceInt();
        ICUResourceWriter.ResourceInt algorithmic = new ICUResourceWriter.ResourceInt();
        ICUResourceWriter.ResourceString desc = new ICUResourceWriter.ResourceString();

        radix.name = LDMLConstants.RADIX;
        desc.name = LDMLConstants.DESC;
        algorithmic.name = LDMLConstants.ALGORITHMIC;

        String radixString = LDMLUtilities.getAttributeValue(node, LDMLConstants.RADIX);
        if (radixString != null) {
          radix.val = radixString;
        } else {
          radix.val = "10";
        }

        if (type.equals(LDMLConstants.ALGORITHMIC)) {
          String numSysRules = LDMLUtilities.getAttributeValue(node, LDMLConstants.RULES);
          int marker = numSysRules.lastIndexOf("/");
          if (marker > 0) {
            String prefix = numSysRules.substring(0,marker + 1);
            String suffix = numSysRules.substring(marker + 1);
            desc.val = prefix + "%" + suffix;
          } else {
            desc.val = "%" + numSysRules;
          }
          algorithmic.val = "1";
        } else {
          desc.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.DIGITS);
          algorithmic.val = "0";
        }

        nsTable.first = radix;
        radix.next = desc;
        desc.next = algorithmic;

        if (current == null) {
          ns.first = nsTable;
          current = findLast(nsTable);
        } else{
          current.next = nsTable;
          current = findLast(nsTable);
        }
      }
    }

    if (ns.first != null) {
      return ns;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseCLDRVersion(Node root, StringBuffer xpath) {
    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
    str.name = LDMLConstants.CLDR_VERSION;
    str.val = LDMLUtilities.getAttributeValue(root, LDMLConstants.VERSION);
    return str;
  }

  private ICUResourceWriter.Resource parseTerritoryContainment(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource res = null;
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLConstants.TERRITORY_CONTAINMENT;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }
      if (name.equals(LDMLConstants.GROUP)) {
        String cnt = LDMLUtilities.getAttributeValue(node, LDMLConstants.CONTAINS);
        String value = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        res = getResourceArray(cnt, value);
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  //    private ICUResourceWriter.Resource parseCalendarData(Node root, StringBuffer xpath) {
  //        int savedLength = xpath.length();
  //        getXPath(root, xpath);
  //        int oldLength = xpath.length();
  //        ICUResourceWriter.Resource current = null;
  //        ICUResourceWriter.Resource res = null;
  //
  //        if (isNodeNotConvertible(root, xpath)) {
  //            xpath.setLength(savedLength);
  //            return null;
  //        }
  //
  //        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
  //        table.name = LDMLConstants.CALENDAR_DATA;
  //
  //        for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
  //            if (node.getNodeType()!= Node.ELEMENT_NODE) {
  //                continue;
  //            }
  //            String name = node.getNodeName();
  //            getXPath(node, xpath);
  //            if (isNodeNotConvertible(node, xpath)) {
  //                xpath.setLength(oldLength);
  //                continue;
  //            }
  //            if (name.equals(LDMLConstants.CALENDAR)) {
  //                //Note: territories attribute in calendar element was deprecated.
  //                //      use calendarPreferences instead.
  //                //String cnt = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORIES);
  //                //res = getResourceArray(cnt, LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE));
  //            } else {
  //                System.err.println("Encountered unknown " + xpath.toString());
  //                System.exit(-1);
  //            }
  //            if (res != null) {
  //                if (current == null) {
  //                    table.first = res;
  //                    current = findLast(res);
  //                }else{
  //                    current.next = res;
  //                    current = findLast(res);
  //                }
  //                res = null;
  //            }
  //            xpath.delete(oldLength, xpath.length());
  //        }
  //        xpath.delete(savedLength, xpath.length());
  //        if (table.first != null) {
  //            return table;
  //        }
  //        return null;
  //    }

  private ICUResourceWriter.Resource parseCalendarPreferenceData(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource res = null;

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLConstants.CALENDAR_PREFERENCE_DATA;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }
      if (!name.equals(LDMLConstants.CALENDAR_PREFERENCE)) {
        System.err.println("Encountered unknown " + xpath.toString());
        System.exit(-1);
      }
      String tmp = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORIES);
      String order = LDMLUtilities.getAttributeValue(node, LDMLConstants.ORDERING);

      // expand territories and create separated ordering array for each
      String[] territories = tmp.split("\\s+");
      for (int i = 0; i < territories.length; i++) {
        res = getResourceArray(order, territories[i]);
        if (current == null) {
          table.first = res;
        } else {
          current.next = res;
        }
        current = findLast(res);
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseTerritoryData(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource res = null;

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLConstants.TERRITORY_DATA;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.TERRITORY)) {
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        if (type == null) {
          printError(fileName, "Could not get type attribute for xpath: " + xpath.toString());
        }
        str.name = type;
        str.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.MPTZ);
        res = str;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.ResourceArray getResourceArray(String str, String name) {
    if (str != null) {
      String[] strs = str.split("\\s+");
      ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
      arr.name = name;
      ICUResourceWriter.Resource curr = null;
      for (int i = 0; i <strs.length; i++) {
        ICUResourceWriter.ResourceString string = new ICUResourceWriter.ResourceString();
        string.val = strs[i];
        if (curr == null) {
          curr = arr.first = string;
        } else {
          curr.next = string;
          curr = curr.next;
        }
      }

      return arr;
    }
    return null;
  }

  private ICUResourceWriter.Resource parseLanguageData(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }
    Hashtable hash = new Hashtable();

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLConstants.LANGUAGE_DATA;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }
      if (name.equals(LDMLConstants.LANGUAGE)) {
        String key = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        if (key == null) {
          printError(
              fileName,"<language > element does not have type attribute ! " + xpath.toString());
          return null;
        }

        String scs = LDMLUtilities.getAttributeValue(node, LDMLConstants.SCRIPTS);
        String trs = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORIES);
        String mpt = LDMLUtilities.getAttributeValue(node, LDMLConstants.MPT);

        String alt = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALT);
        if (alt == null) {
          alt = LDMLConstants.PRIMARY;
        }
        ICUResourceWriter.ResourceTable tbl = new ICUResourceWriter.ResourceTable();
        tbl.name = alt;
        ICUResourceWriter.ResourceArray scripts = getResourceArray(scs, LDMLConstants.SCRIPTS);
        ICUResourceWriter.ResourceArray terrs = getResourceArray(trs, LDMLConstants.TERRITORIES);
        ICUResourceWriter.ResourceArray mpts = getResourceArray(mpt, LDMLConstants.MPT);
        if (scripts != null) {
          tbl.first = scripts;
        }
        if (terrs != null) {
          if (tbl.first != null) {
            findLast(tbl.first).next = terrs;
          } else {
            tbl.first = terrs;
          }
        }
        if (mpts != null) {
          if (tbl.first != null) {
            findLast(tbl.first).next = mpts;
          } else {
            tbl.first = terrs;
          }
        }
        // now find in the Hashtable
        ICUResourceWriter.ResourceTable main = (ICUResourceWriter.ResourceTable)hash.get(key);
        if (main == null) {
          main = new ICUResourceWriter.ResourceTable();
          main.name = key;
          hash.put(key, main);
        }
        if (main.first != null) {
          findLast(main.first).next = tbl;
        } else {
          main.first = tbl;
        }
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
      xpath.setLength(oldLength);
    }

    Enumeration iter = hash.keys();
    ICUResourceWriter.Resource current = null, res = null;
    while (iter.hasMoreElements()) {
      String key = (String)iter.nextElement();
      res = (ICUResourceWriter.Resource) hash.get(key);
      if (current == null) {
        current = table.first = res;
      } else {
        current.next = res;
        current = current.next;
      }
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseTimeZoneData(Node root, StringBuffer xpath) {
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource first = null;
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // dont write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable mapZones = new ICUResourceWriter.ResourceTable();
    mapZones.name = LDMLConstants.MAP_TIMEZONES;
    for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      } else if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.MAP_TIMEZONES)) {

        //if (DEBUG)printXPathWarning(node, xpath);
        res = parseMapTimezones(node, xpath);
        if (res != null) {
          if (mapZones.first == null) {
            mapZones.first = res;
          } else {
            findLast(mapZones.first).next = res;
          }
        }
        res = null;
      } else if (name.equals(LDMLConstants.ZONE_FORMATTING)) {
        res = parseZoneFormatting(node, xpath);
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    if (mapZones.first != null) {
      if (current == null) {
        first = current = mapZones;
      }else{
        current.next = mapZones;
        current = findLast(mapZones);
      }
    }

    xpath.delete(savedLength, xpath.length());
    if (first != null) {
      return first;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseMapTimezones(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource res = null;

    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLUtilities.getAttributeValue(root, LDMLConstants.TYPE);

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();

      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.MAP_ZONE)) {
        String type = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        String other = LDMLUtilities.getAttributeValue(node, LDMLConstants.OTHER);
        String territory = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORY);
        String result;
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        if (territory != null && territory.length() > 0) {
          result = "meta:" + other + "_" + territory;
          str.name = "\"" + result + "\"";
          str.val = type;
        } else {
          result = type;
          str.name = "\"" + other + "\"";
          str.val = result;
        }
        res = str;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.setLength(oldLength);
    }

    xpath.setLength(savedLength);
    if (table.first != null) {
      return table;
    }

    return null;
  }

  // Quick & dirty bare-bones version of this for now just to start writing something out.
  // Doesn't write from or to data (don't have any yet), and just writes alt as a comment.
  // Add more once we figure out what ICU needs. -pedberg
  private ICUResourceWriter.Resource addTelephoneCodeData() {
    // uses SupplementalDataInfo, doesn't need node, xpath
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLConstants.TELEPHONE_CODE_DATA;
    table.comment = commentForTelephoneCodeData;
    ICUResourceWriter.Resource currTerr = null;
    ICUResourceWriter.ResourceTable terrTable = null;

    for (String terr: supplementalDataInfo.getTerritoriesForTelephoneCodeInfo()) {
      terrTable = new ICUResourceWriter.ResourceTable();
      terrTable.name = terr;
      ICUResourceWriter.Resource currCode = null;
      for (SupplementalDataInfo.TelephoneCodeInfo telephoneCodeInfo :
               supplementalDataInfo.getTelephoneCodeInfoForTerritory(terr)) {
        ICUResourceWriter.ResourceTable codeData = new ICUResourceWriter.ResourceTable();
        codeData.name = "";
        ICUResourceWriter.ResourceString codeEntry = new ICUResourceWriter.ResourceString();
        codeEntry.name = "code";
        codeEntry.val = telephoneCodeInfo.getCode();
        codeData.first = codeEntry;
        String alt = telephoneCodeInfo.getAlt();
        if (alt.length() > 0) {
          ICUResourceWriter.ResourceString altEntry = new ICUResourceWriter.ResourceString();
          altEntry.name = "alt";
          altEntry.val = alt;
          codeEntry.next = altEntry;
        }
        if (currCode == null) {
          terrTable.first = currCode = codeData;
        } else {
          currCode.next = codeData;
          currCode = currCode.next;
        }
      }

      if (currTerr == null) {
        table.first = terrTable;
        currTerr = findLast(terrTable);
      } else {
        currTerr.next = terrTable;
        currTerr = findLast(terrTable);
      }
    }

    return table;
  }

  private ICUResourceWriter.Resource parseZoneFormatting(Node root, StringBuffer xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;

    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    //if the whole node is marked draft then
    //dont write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    table.name = "zoneFormatting";
    table.noSort = true;
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.ZONE_ITEM)) {
        ICUResourceWriter.ResourceTable zi = new ICUResourceWriter.ResourceTable();
        zi.name = "\"" + LDMLUtilities.getAttributeValue(
            node, LDMLConstants.TYPE).replaceAll("/",":") + "\"";

        String canonical = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        ICUResourceWriter.ResourceString canon = new ICUResourceWriter.ResourceString();
        canon.name = LDMLConstants.CANONICAL;
        canon.val = canonical;
        zi.first = canon;

        String territory = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORY);
        ICUResourceWriter.ResourceString ter = new ICUResourceWriter.ResourceString();
        ter.name = LDMLConstants.TERRITORY;
        ter.val = territory;
        canon.next = ter;

        String aliases = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALIASES);
        String icu_aliases = getICUAlias(LDMLUtilities.getAttributeValue(node,LDMLConstants.TYPE));
        String all_aliases = aliases;
        if (icu_aliases != null) {
          if (aliases == null) {
            all_aliases = icu_aliases;
          } else {
            all_aliases = aliases + " " + icu_aliases;
          }
        }

        if (all_aliases != null) {
          String[] arr = all_aliases.split("\\s+");
          ICUResourceWriter.ResourceArray als = new ICUResourceWriter.ResourceArray();
          als.name = LDMLConstants.ALIASES;
          ICUResourceWriter.Resource cur = null;
          for(int i = 0; i <arr.length; i++) {
            ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
            str.val = arr[i];
            if (cur == null) {
              als.first = cur = str;
            }else{
              cur.next = str;
              cur = cur.next;
            }
          }
          ter.next = als;
        }
        res = zi;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }
    xpath.delete(savedLength, xpath.length());

    // Now add the multi-zone list to the table
    String multizone = LDMLUtilities.getAttributeValue(root,LDMLConstants.MULTIZONE);
    ICUResourceWriter.ResourceArray mz;
    mz = getResourceArray(multizone, LDMLConstants.MULTIZONE);
    if (current == null) {
      table.first = mz;
      current = findLast(mz);
    } else {
      current.next = mz;
      current = findLast(mz);
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private String getICUAlias(String tzid) {
    // This function is used to return the compatibility aliases for ICU.
    //  It should match the ICUZONES file in ICU4C source/tools/tzcode/icuzones.
    //  Note that since we don't expect this to change AT ALL over time, it is
    // easier to just hard code the information here. We only include those
    // aliases that are NOT in CLDR.

    if (tzid.equals("Australia/Darwin")) return("ACT");
    if (tzid.equals("Australia/Sydney")) return("AET");
    if (tzid.equals("America/Argentina/Buenos_Aires")) return("AGT");
    if (tzid.equals("Africa/Cairo")) return("ART");
    if (tzid.equals("America/Anchorage")) return("AST");
    if (tzid.equals("America/Sao_Paulo")) return("BET");
    if (tzid.equals("Asia/Dhaka")) return("BST");
    if (tzid.equals("Africa/Harare")) return("CAT");
    if (tzid.equals("America/St_Johns")) return("CNT");
    if (tzid.equals("America/Chicago")) return("CST");
    if (tzid.equals("Asia/Shanghai")) return("CTT");
    if (tzid.equals("Africa/Addis_Ababa")) return("EAT");
    if (tzid.equals("Europe/Paris")) return("ECT");
    if (tzid.equals("America/Indianapolis")) return("IET");
    if (tzid.equals("Asia/Calcutta")) return("IST");
    if (tzid.equals("Asia/Tokyo")) return("JST");
    if (tzid.equals("Pacific/Apia")) return("MIT");
    if (tzid.equals("Asia/Yerevan")) return("NET");
    if (tzid.equals("Pacific/Auckland")) return("NST");
    if (tzid.equals("Asia/Karachi")) return("PLT");
    if (tzid.equals("America/Phoenix")) return("PNT");
    if (tzid.equals("America/Puerto_Rico")) return("PRT");
    if (tzid.equals("America/Los_Angeles")) return("PST");
    if (tzid.equals("Pacific/Guadalcanal")) return("SST");
    if (tzid.equals("Asia/Saigon")) return("VST");

    return null;
  }

  private ICUResourceWriter.Resource parseCurrencyFraction(Node root, StringBuffer xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;

    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    table.name = "CurrencyMeta";
    table.noSort = true;
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
      }

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.INFO)) {
        ICUResourceWriter.ResourceIntVector vector = new ICUResourceWriter.ResourceIntVector();
        vector.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.ISO_4217);
        ICUResourceWriter.ResourceInt zero = new ICUResourceWriter.ResourceInt();
        ICUResourceWriter.ResourceInt one = new ICUResourceWriter.ResourceInt();
        zero.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.DIGITS);
        one.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.ROUNDING);
        vector.first = zero;
        zero.next = one;
        res = vector;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        }else{
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private int countHyphens(String str) {
    int ret = 0;
    for (int i = 0; i <str.length(); i++) {
      if (str.charAt(i) == '-') {
        ret++;
      }
    }

    return ret;
  }

  private long getMilliSeconds(String dateStr) {
    try {
      if (dateStr != null) {
        int count = countHyphens(dateStr);
        SimpleDateFormat format = new SimpleDateFormat();
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = null;
        if (count == 2) {
          format.applyPattern("yyyy-mm-dd");
          date = format.parse(dateStr);
        } else if (count == 1) {
          format.applyPattern("yyyy-mm");
          date = format.parse(dateStr);
        } else {
          format.applyPattern("yyyy");
          date = format.parse(dateStr);
        }
        return date.getTime();
      }
    } catch(ParseException ex) {
      System.err.println("Could not parse date: " + dateStr);
      System.err.println(ex.getMessage());
      System.exit(-1);
    }
    return -1;
  }

  private ICUResourceWriter.ResourceIntVector getSeconds(String dateStr) {
    long millis = getMilliSeconds(dateStr);
    if (millis == -1) {
      return null;
    }

    int top =(int)((millis & 0xFFFFFFFF00000000L)>>>32);
    int bottom = (int)((millis & 0x00000000FFFFFFFFL));
    ICUResourceWriter.ResourceIntVector vector = new ICUResourceWriter.ResourceIntVector();
    ICUResourceWriter.ResourceInt int1 = new ICUResourceWriter.ResourceInt();
    ICUResourceWriter.ResourceInt int2 = new ICUResourceWriter.ResourceInt();
    int1.val = Integer.toString(top);
    int2.val = Integer.toString(bottom);
    vector.first = int1;
    int1.next = int2;
    vector.smallComment = dateStr; // + " " + millis + "L";
    if (DEBUG) {
      top = Integer.parseInt(int1.val);
      bottom = Integer.parseInt(int2.val);
      long bot = 0xffffffffL & bottom;
      long full = ((long)(top) << 32);
      full +=(long)bot;
      if (full != millis) {
        System.out.println("Did not get the value back.");
      }
    }

    return vector;
  }

  private ICUResourceWriter.Resource parseCurrencyRegion(Node root, StringBuffer xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;

    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    table.name =  LDMLUtilities.getAttributeValue(root, LDMLConstants.ISO_3166);
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      getXPath(node, xpath);
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.CURRENCY)) {
        //getXPath(node, xpath);
        if (isNodeNotConvertible(node, xpath)) {
          xpath.setLength(oldLength);
          continue;
        }
        ICUResourceWriter.ResourceTable curr = new ICUResourceWriter.ResourceTable();
        curr.name ="";
        ICUResourceWriter.ResourceString id = new ICUResourceWriter.ResourceString();
        id.name ="id";
        id.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.ISO_4217);

        String tender = LDMLUtilities.getAttributeValue(node, LDMLConstants.TENDER);

        ICUResourceWriter.ResourceIntVector fromRes = getSeconds(
            LDMLUtilities.getAttributeValue(node, LDMLConstants.FROM));
        ICUResourceWriter.ResourceIntVector toRes =  getSeconds(
            LDMLUtilities.getAttributeValue(node, LDMLConstants.TO));

        if (fromRes != null) {
          fromRes.name = LDMLConstants.FROM;
          curr.first = id;
          id.next = fromRes;
        }
        if (toRes != null) {
          toRes.name = LDMLConstants.TO;
          fromRes.next = toRes;
        }
        if (tender != null && tender.equals("false")) {
          res = null;
        } else {
          res = curr;
        }
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseCurrencyData(Node root, StringBuffer xpath) {
    ICUResourceWriter.Resource currencyMeta = null;
    ICUResourceWriter.ResourceTable currencyMap = new ICUResourceWriter.ResourceTable();
    currencyMap.name = "CurrencyMap";
    currencyMap.comment = commentForCurrencyMap;
    currencyMap.noSort = true;
    ICUResourceWriter.Resource currentMap = null;

    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole collation node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      //getXPath(node, xpath);
      if (name.equals(LDMLConstants.REGION)) {
        res = parseCurrencyRegion(node, xpath);
        if (res != null) {
          if (currentMap == null) {
            currencyMap.first = res;
            currentMap = findLast(res);
          } else {
            currentMap.next = res;
            currentMap = findLast(res);
          }
          res = null;
        }
      } else if (name.equals(LDMLConstants.FRACTIONS)) {
        currencyMeta = parseCurrencyFraction(node, xpath);
        currencyMeta.comment = commentForCurrencyMeta;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    currencyMeta.next = currencyMap;

    return currencyMeta;
  }

  private String ldmlVersion = null;

  private ICUResourceWriter.Resource parseBundle(InputLocale loc) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();

    ldmlVersion = "0.0";

    // OK. This is no longer a reactive, but a proactive program.
    ICUResourceWriter.Resource res = null;
    // * Fetch the identity
    if ((res = parseIdentity(table, loc, "//ldml/identity")) != null) {
      // table.appendContents(res);
      res = null;
    }
    // * Verify the locale's identity.
    if (loc.file.isHere("//ldml/alias")) {
      // locale is an alias
      res = ICUResourceWriter.createString("\"%%ALIAS\"", loc
              .getBasicAttributeValue("//ldml/alias",
                      LDMLConstants.SOURCE));
      table.replaceContents(res); // overwrite anything else set so far.
      return table;
    }

    // If this is a language + script locale and the script is not default content,
    // then add a "Parent is root" boolean resource in order to prevent cross-script
    // inheritance.

    String localeID = loc.file.getLocaleID();

    if (ULocale.getScript(localeID).length() > 0
        && ULocale.getCountry(localeID).length() == 0
        && !supplementalDataInfo.getDefaultContentLocales().contains(localeID)) {

      ICUResourceWriter.ResourceInt pr = new ICUResourceWriter.ResourceInt();
      pr.name = "%%ParentIsRoot";
      pr.val = "1";
      table.appendContents(pr);
    }

    // Now, loop over other stuff.
    String stuff[] = {
            // Don't do these:
            // LDMLConstants.ALIAS,
            // LDMLConstants.IDENTITY,

            // these are OK:
            LDMLConstants.SPECIAL, LDMLConstants.LDN,
            LDMLConstants.LAYOUT,
            LDMLConstants.FALLBACK, // noop
            LDMLConstants.CHARACTERS, LDMLConstants.DELIMITERS,
            LDMLConstants.DATES, LDMLConstants.NUMBERS,
            LDMLConstants.POSIX,
            // LDMLConstants.SEGMENTATIONS,
            LDMLConstants.REFERENCES,
            LDMLConstants.RBNF,
            LDMLConstants.COLLATIONS,
            LDMLConstants.UNITS,
            // Second time for the alt ="short" versions...
            LDMLConstants.UNITS};

    boolean processedUnits = false;

    for (int jj = 0; jj < stuff.length; jj++) {
      res = null;
      // String xpath = (String) it.next();
      // String name = parts.getElement(1);
      String name = stuff[jj];
      String xpath = "//ldml/" + stuff[jj];
      if (verbose) {
        System.out.println(name + " ");
      }

      if (name.equals(LDMLConstants.SPECIAL)) {
        res = parseSpecialElements(loc, xpath);
      } else if (name.equals(LDMLConstants.LDN)) {
        res = parseLocaleDisplayNames(loc);
      } else if (name.equals(LDMLConstants.LAYOUT)) {
        res = parseLayout(loc, xpath);
      } else if (name.equals(LDMLConstants.FALLBACK)) {
      } else if (name.equals(LDMLConstants.CHARACTERS)) {
        res = parseCharacters(loc, xpath);
      } else if (name.equals(LDMLConstants.DELIMITERS)) {
        res = parseDelimiters(loc, xpath);
      } else if (name.equals(LDMLConstants.DATES)) {
        res = parseDates(loc, xpath);
      } else if (name.equals(LDMLConstants.NUMBERS)) {
        res = parseNumbers(loc, xpath);
      } else if (name.equals(LDMLConstants.COLLATIONS)) {
        if (sourceDir.indexOf("coll") > 0) {
          res = parseCollations(loc, xpath);
        }
      } else if (name.equals(LDMLConstants.POSIX)) {
        res = parsePosix(loc, xpath);
      } else if (name.equals(LDMLConstants.RBNF)) {
        res = parseRBNF(loc, xpath);
      } else if (name.equals(LDMLConstants.SEGMENTATIONS)) {
        // TODO: FIX ME with parseSegmentations();
        if (DEBUG) {
          printXPathWarning(loc, xpath);
        }
      } else if (name.indexOf("icu:") > -1 || name.indexOf("openOffice:") > -1) {
        // TODO: these are specials .. ignore for now ... figure out
        // what to do later
      } else if (name.equals(LDMLConstants.REFERENCES)) {
        // TODO: This is special documentation... ignore for now
        if (DEBUG) {
          printXPathWarning(loc, xpath);
        }
      } else if (name.equals(LDMLConstants.UNITS)) {
        if (processedUnits == false) {
          res = parseUnits(loc, xpath, LDMLConstants.UNITS, null);
          processedUnits = true;
        } else {
          res = parseUnits(loc, xpath, LDMLConstants.UNITS_SHORT, LDMLConstants.SHORT);
        }
      } else {
        System.err.println("Encountered unknown <" + "//ldml" + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) { // have an item
        table.appendContents(res);
      }
    }

    if (sourceDir.indexOf("main") > 0 /* && !LDMLUtilities.isLocaleAlias(root)*/) {
      ICUResourceWriter.Resource temp = parseWeek();
      if (temp != null) {
        ICUResourceWriter.Resource greg = findResource(table, LDMLConstants.GREGORIAN);
        ICUResourceWriter.Resource cals = findResource(table, LDMLConstants.CALENDAR);
        if (greg != null) {
          findLast(greg.first).next = temp;
        } else if (cals != null) {
          greg = new ICUResourceWriter.ResourceTable();
          greg.name = LDMLConstants.GREGORIAN;
          greg.first = temp;
          findLast(cals.first).next = greg;
        } else {
          greg = new ICUResourceWriter.ResourceTable();
          greg.name = LDMLConstants.GREGORIAN;
          greg.first = temp;

          ICUResourceWriter.ResourceTable cal = new ICUResourceWriter.ResourceTable();
          cal.name = LDMLConstants.CALENDAR;
          cal.first = greg;

          table.appendContents(cal);
        }
      }
      temp = parseMeasurement();
      if (temp != null) {
        table.appendContents(temp);
      }
    }

    if (verbose) {
      System.out.println();
    }

    if (supplementalDoc != null) {
      /*
       * TODO: comment this out for now. We shall revisit when we have
       * information on how to present the script data with new API
       * ICUResourceWriter.Resource res =
       * parseLocaleScript(supplementalDoc); if (res != null) { if (current ==
       * null) { table.first = res; current = findLast(res); }else{
       * current.next = res; current = findLast(res); } res = null; }
       *
       * ICUResourceWriter.Resource res = parseMetaData(supplementalDoc);
       */
    }

    return table;
  }

  /**
   * return the end of a res chain
   *
   * @param res
   *            res to start with
   * @return the end
   * @deprecated - use ICUResourceWriter.Resource.end()
   */
  private ICUResourceWriter.Resource findLast(ICUResourceWriter.Resource res) {
    return res.end();
  }

  private ICUResourceWriter.Resource findResource(ICUResourceWriter.Resource res, String type) {
    ICUResourceWriter.Resource current = res;
    ICUResourceWriter.Resource ret = null;
    while (current != null) {
      if (current.name != null && current.name.equals(type)) {
        return current;
      }

      if (current.first != null) {
        ret = findResource(current.first, type);
      }
      if (ret != null) {
        break;
      }

      current = current.next;
    }

    return ret;
  }

  /**
   * Higher convenience level than parseAliasResource Check to see if there is
   * an alias at xpath+ "/alias", if so, create & return it.
   *
   * @param loc
   * @param xpath
   * @return
   */
  private ICUResourceWriter.Resource getAliasResource(InputLocale loc, String xpath) {
    String name = loc.getXpathName(xpath);
    String aliasPath = xpath + "/alias";
    ICUResourceWriter.Resource aRes = parseAliasResource(loc, aliasPath);
    if (aRes != null) {
      aRes.name = name;
    }

    return aRes;
  }

  private ICUResourceWriter.Resource parseAliasResource(InputLocale loc, String xpath) {
    String source = loc.getBasicAttributeValue(xpath, LDMLConstants.SOURCE);
    String path = loc.getBasicAttributeValue(xpath, LDMLConstants.PATH);
    if (source == null && path == null) {
      if (!loc.file.isHere(xpath)) {
        return null;
      }
    }
    try {
      // if (node != null && (!isNodeNotConvertible(node, xpath))) { ??
      // aliases always convertible
      ICUResourceWriter.ResourceAlias alias = new ICUResourceWriter.ResourceAlias();
      String basePath = xpath.replaceAll("/alias.*$", "");
      String fullPath = loc.file.getFullXPath(xpath).replaceAll("/alias.*$", "");
      if (path != null) {
        path = path.replaceAll("='", "=\"").replaceAll("']", "\"]");
      }

      String val = LDMLUtilities.convertXPath2ICU(source, path, basePath, fullPath);
      alias.val = val;
      alias.name = basePath;
      // System.err.println("BBase: " + basePath + ", FFull: " + fullPath +
      // " >>> " + val);
      return alias;
      // }
    } catch (TransformerException ex) {
      System.err.println("Could not compile XPATH for" + " source:  "
              + source + " path: " + path + " Node: " + xpath);
      ex.printStackTrace();
      System.exit(-1);
    }

    return null;
    // TODO update when XPATH is integrated into LDML
  }

  /**
   * @param node
   * @param xpath
   * @return
   */
  private ICUResourceWriter.Resource parseAliasResource(Node node, StringBuffer xpath) {
    return parseAliasResource(node,xpath,false);
  }

  private ICUResourceWriter.Resource parseAliasResource(
      Node node, StringBuffer xpath, boolean IsCollation) {
    int saveLength = xpath.length();
    getXPath(node, xpath);
    try {
      if (node != null && (IsCollation || !isNodeNotConvertible(node, xpath))) {
        ICUResourceWriter.ResourceAlias alias = new ICUResourceWriter.ResourceAlias();
        xpath.setLength(saveLength);
        String val = LDMLUtilities.convertXPath2ICU(node, null, xpath);
        alias.val = val;
        alias.name = node.getParentNode().getNodeName();
        xpath.setLength(saveLength);
        return alias;
      }
    } catch(TransformerException ex) {
      System.err.println(
          "Could not compile XPATH for" +
          " source:  " + LDMLUtilities.getAttributeValue(node, LDMLConstants.SOURCE) +
          " path: " + LDMLUtilities.getAttributeValue(node, LDMLConstants.PATH) +
          " Node: " + node.getParentNode().getNodeName());
      ex.printStackTrace();
      System.exit(-1);
    }

    xpath.setLength(saveLength);
    // TODO update when XPATH is integrated into LDML
    return null;
  }

  private StringBuffer getXPath(Node node, StringBuffer xpath) {
    xpath.append("/");
    xpath.append(node.getNodeName());
    LDMLUtilities.appendXPathAttribute(node,xpath);
    return xpath;
  }

  /*
    private StringBuffer getXPathAllAttributes(Node node, StringBuffer xpath) {
        xpath.append("/");
        xpath.append(node.getNodeName());
        LDMLUtilities.appendAllAttributes(node,xpath);
        return xpath;
    }
   */

  private ICUResourceWriter.Resource parseIdentity(
      ICUResourceWriter.ResourceTable table, InputLocale loc, String xpath) {

    // version #
    String verPath = "//ldml/" + LDMLConstants.IDENTITY + "/" + LDMLConstants.VERSION;
    String version = loc.getBasicAttributeValue(loc.file, verPath, LDMLConstants.NUMBER);
    if (loc.resolved() != null) {
      String version2 = loc.getBasicAttributeValue(loc.resolved(),verPath, LDMLConstants.NUMBER);
      String foundIn = loc.resolved().getSourceLocaleID(verPath, null);
      if (foundIn != null && foundIn.equals(loc.locale) && version2 != null) {
        // make sure it is in our 'original' locale.
        version = version2; // use version from 'resolved' -
      }
    }

    if (version == null) {
      // System.err.println("No version #??");
    } else {
      version = version.replaceAll(".*?Revision: (.*?) .*", "$1");

      ICUResourceWriter.Resource res = ICUResourceWriter.createString(
              keyNameMap.get(LDMLConstants.VERSION), version);
      // write the version string
      if (res != null) {
        table.appendContents(res);
      }
    }

    String localeID = loc.file.getLocaleID();
    table.name = localeID;

    // TODO: alias in the identity??
    // }else if (name.equals(LDMLConstants.ALIAS)) {
    // res = parseAliasResource(node, xpath);

    return table; // modified TL resource
  }

  private static final String[] registeredKeys = new String[] {
    "collation", "calendar", "currency"
  };

  private ICUResourceWriter.Resource parseLocaleDisplayNames(InputLocale loc) {
    ICUResourceWriter.Resource first = null;
    ICUResourceWriter.Resource current = null;

    ICUResourceWriter.Resource res = null;
    String stuff[] = {
      LDMLConstants.LANGUAGES,
      LDMLConstants.SCRIPTS,
      LDMLConstants.TERRITORIES,
      LDMLConstants.KEYS,
      LDMLConstants.VARIANTS,
      LDMLConstants.MSNS,
      LDMLConstants.TYPES,
      LDMLConstants.ALIAS,
      //LDMLConstants.MSNS,
      LDMLConstants.CODE_PATTERNS,
      LDMLConstants.LOCALEDISPLAYPATTERN,
      LDMLConstants.LANGUAGES_SHORT
    };

    for (String name : stuff) {
      if (name.equals(LDMLConstants.LANGUAGES)
              || name.equals(LDMLConstants.SCRIPTS)
              || name.equals(LDMLConstants.TERRITORIES)
              || name.equals(LDMLConstants.KEYS)
              || name.equals(LDMLConstants.VARIANTS)
              || name.equals(LDMLConstants.MSNS)
              || name.equals(LDMLConstants.CODE_PATTERNS)) {
        res = parseList(loc, name);
      } else if (name.equals(LDMLConstants.TYPES)) {
        res = parseDisplayTypes(loc, name);
      } else if (name.equals(LDMLConstants.LOCALEDISPLAYPATTERN)) {
        res = parseLocaleDisplayPattern(loc);
      } else if (name.equals(LDMLConstants.ALIAS)) {
        // res = parseAliasResource(loc, name);
        // TODO: parseAliasResource - these are different types in ICU, can't just alias them all
      } else if (name.equals(LDMLConstants.LANGUAGES_SHORT)) {
        res = parseListAlt(loc, LDMLConstants.LANGUAGES, name, LDMLConstants.SHORT);
      } else {
        System.err.println("Unknown element found: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    return first;
  }

  private ICUResourceWriter.Resource parseDisplayTypes(InputLocale loc, String name) {
    StringBuffer myXpath = new StringBuffer();
    myXpath.append("//ldml/localeDisplayNames/types");
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = (String) keyNameMap.get(LDMLConstants.TYPES);
    ICUResourceWriter.Resource alias = null;

    // if the whole thing is an alias
    if ((alias = getAliasResource(loc, myXpath.toString()))!= null) {
      alias.name = table.name;
      return alias;
    }

    for (int i = 0; i < registeredKeys.length; i++) {
      ICUResourceWriter.ResourceTable subTable = new ICUResourceWriter.ResourceTable();
      subTable.name = registeredKeys[i];
      for (Iterator <String > iter = loc.file.iterator(myXpath.toString()); iter.hasNext();) {
        String xpath = iter.next();
        String name2 = loc.getXpathName(xpath);
        if (!LDMLConstants.TYPE.equals(name2)) {
          printError(loc.locale,"Encountered unknown <" + xpath + "> subelement: " + name2 +
                     " while looking for " + LDMLConstants.TYPE);
          System.exit(-1);
        }

        String key = loc.getAttributeValue(xpath, LDMLConstants.KEY);
        if (!registeredKeys[i].equals(key)) {
          continue;
        }

        String type = loc.getAttributeValue(xpath, LDMLConstants.TYPE);
        if (loc.isPathNotConvertible(xpath)) {
          continue;
        }

        String val = loc.file.getStringValue(xpath);
        ICUResourceWriter.Resource string = ICUResourceWriter.createString(type, val);
        subTable.appendContents(string);
      }

      if (!subTable.isEmpty()) {
        table.appendContents(subTable);
      }
    }

    if (!table.isEmpty()) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseLocaleDisplayPattern(InputLocale loc) {
    StringBuffer myXpath = new StringBuffer();
    myXpath.append("//ldml/localeDisplayNames/");
    myXpath.append(LDMLConstants.LOCALEDISPLAYPATTERN);
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLConstants.LOCALEDISPLAYPATTERN;
    ICUResourceWriter.Resource alias = null;

    // if the whole thing is an alias
    if ((alias = getAliasResource(loc, myXpath.toString()))!= null) {
      alias.name = table.name;
      return alias;
    }

    for (Iterator <String > iter = loc.file.iterator(myXpath.toString()); iter.hasNext();) {
      String xpath = iter.next();
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      String element = loc.getXpathName(xpath);
      String name = null;
      if (LDMLConstants.LOCALE_PATTERN.equals(element)) {
        name = LDMLConstants.PATTERN;
      } else if (LDMLConstants.LOCALE_SEPARATOR.equals(element)) {
        name = LDMLConstants.SEPARATOR;
      } else {
        printError(loc.locale,"Encountered unknown <" + xpath
                + "> subelement: " + element + " while looking for " + LDMLConstants.TYPE);
        System.exit(-1);
      }

      String value = loc.file.getStringValue(xpath);
      ICUResourceWriter.Resource res = ICUResourceWriter.createString(name, value);
      table.appendContents(res);
    }

    if (!table.isEmpty()) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseList(InputLocale loc, String name) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    String rootNodeName = name;
    table.name = (String) keyNameMap.get(rootNodeName);
    ICUResourceWriter.Resource current = null;
    boolean uc = rootNodeName.equals(LDMLConstants.VARIANTS);
    boolean prohibit = rootNodeName.equals(LDMLConstants.TERRITORIES);
    String origXpath = "//ldml/localeDisplayNames/" + name;
    if ((current = getAliasResource(loc, origXpath)) != null) {
      current.name = table.name;
      return current;
    }

    for (Iterator <String > iter = loc.file.iterator(origXpath); iter.hasNext();) {
      String xpath = (String) iter.next();
      // a certain element of the list
      // is marked draft .. just dont
      // output that item
      // if (isNodeNotConvertible(node, xpath)) {
      // xpath.setLength(oldLength);
      // continue;
      // }
      if (loc.isPathNotConvertible(xpath)) {
        // System.err.println("PNC: " + xpath);
        continue;
      }

      ICUResourceWriter.ResourceString res = new ICUResourceWriter.ResourceString();
      res.name = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
      if (uc) {
        res.name = res.name.toUpperCase();
      }
      res.val = loc.file.getStringValue(xpath);

      if (res.name == null) {
        System.err.println(name + " - " + res.name + " = " + res.val);
      }

      if (prohibit == true && deprecatedTerritories.get(res.name) != null) {
        res = null;
      }
      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
      }
    }
    // xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseListAlt(
      InputLocale loc, String originalName, String name, String altValue) {

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    String rootNodeName = name;
    table.name = (String) keyNameMap.get(rootNodeName);
    ICUResourceWriter.Resource current = null;
    boolean uc = rootNodeName.equals(LDMLConstants.VARIANTS);
    boolean prohibit = rootNodeName.equals(LDMLConstants.TERRITORIES);
    String origXpath = "//ldml/localeDisplayNames/" + originalName;
    if ((current = getAliasResource(loc, origXpath)) != null) {
      current.name = table.name;
      return current;
    }

    for (Iterator <String > iter = loc.file.iterator(origXpath); iter.hasNext();) {
      String xpath = (String) iter.next();
      //if (loc.isPathNotConvertible(xpath)) {
      //    // System.err.println("PNC: " + xpath);
      //    continue;
      //}

      // Check for the "alt" attribute, and process it if requested.
      // Otherwise, skip it.
      String alt = loc.getBasicAttributeValue(xpath, LDMLConstants.ALT);
      if (alt == null || !alt.equals(altValue)) {
        continue;
      }

      ICUResourceWriter.ResourceString res = new ICUResourceWriter.ResourceString();
      res.name = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
      if (uc) {
        res.name = res.name.toUpperCase();
      }
      res.val = loc.file.getStringValue(xpath);

      if (res.name == null) {
        System.err.println(name + " - " + res.name + " = " + res.val);
      }

      if (prohibit == true && deprecatedTerritories.get(res.name) != null) {
        res = null;
      }
      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
      }
    }
    // xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseArray(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceArray array = new ICUResourceWriter.ResourceArray();
    String name = loc.getXpathName(xpath);
    array.name = keyNameMap.get(name);
    ICUResourceWriter.Resource current = null;
    // want them in sorted order (?)
    Set<String> xpaths = new TreeSet<String>();
    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      xpaths.add(xpath);
    }

    for(String apath : xpaths) {
      name = loc.getXpathName(apath);
      // if (loc.isPathNotConvertible(xpath)) continue;

      if (current == null) {
        current = array.first = new ICUResourceWriter.ResourceString();
      } else {
        current.next = new ICUResourceWriter.ResourceString();
        current = current.next;
      }
      // current.name = LDMLUtilities.getAttributeValue(node,
      // LDMLConstants.TYPE);

      ((ICUResourceWriter.ResourceString) current).val = loc.file.getStringValue(apath);
    }

    if (array.first != null) {
      return array;
    }

    return null;
  }

  /**
   * Parse a table (k/v pair) into an ICU table
   * @param loc locale
   * @param xpath base xpath of items
   * @param element the item to search for
   * @param attribute the attribute which will become the 'key' in icu
   * @return the table, or null
   */
  private ICUResourceWriter.Resource parseTable(
      InputLocale loc, String xpath, String element, String attribute) {

    ICUResourceWriter.ResourceTable array = new ICUResourceWriter.ResourceTable();
    String name = loc.getXpathName(xpath);
    array.name = keyNameMap.get(name); // attempt
    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      name = loc.getXpathName(xpath);
      if (!name.equals(element)) {
        System.err.println("Err: unknown item " + xpath + " / " + name + " - expected " + element);
        continue;
      }
      String type = loc.getBasicAttributeValue(xpath, attribute);
      String val =  loc.file.getStringValue(xpath);

      array.appendContents(ICUResourceWriter.createString(type, val));
    }

    if (array.first != null) {
      return array;
    }

    return null;
  }

  private static final String ICU_SCRIPT = "icu:script";

  private ICUResourceWriter.Resource parseCharacters(InputLocale loc, String xpath) {
    ICUResourceWriter.Resource first = null;
    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      if (loc.isPathNotConvertible(aPath)) {
        continue;
      }

      String name = loc.getXpathName(aPath);

      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.EXEMPLAR_CHARACTERS)) {
        String type = loc.getBasicAttributeValue(aPath, LDMLConstants.TYPE);
        res = parseStringResource(loc, aPath);
        if (type != null && type.equals(LDMLConstants.AUXILIARY)) {
          res.name = (String) keyNameMap.get(LDMLConstants.AUXILIARY);
        } else if (type != null && type.equals(LDMLConstants.CURRENCY_SYMBOL)) {
          res = null;
        } else if (type != null && type.equals(LDMLConstants.INDEX)) {
          res = null;
        } else {
          res.name = (String) keyNameMap.get(name);
        }
      } else if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, aPath);
      } else if (name.equals(LDMLConstants.MAPPING)) {
        // Currently we dont have a way to represent this data in ICU !
        // And we don't need to
        // if (DEBUG)printXPathWarning(node, xpath);
      } else if (aPath.indexOf("/" + LDMLConstants.SPECIAL) > 0) {
        res = parseSpecialElements(loc, aPath);
      } else {
        System.err.println("Unknown  character element found: " + aPath
                + " / " + name + " -> " + loc.file.getFullXPath(aPath));
        System.exit(-1);
      }
      if (res != null) {
        first = ICUResourceWriter.Resource.addAfter(first, res);
      }
    }

    return first;
  }

  /*
    private ICUResourceWriter.Resource parseStringResource(Node node) {
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.val = LDMLUtilities.getNodeValue(node);
        str.name = node.getNodeName();
        return str;
    }
   */
  private ICUResourceWriter.Resource parseStringResource(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
    str.val = loc.file.getStringValue(xpath);
    str.name = loc.getXpathName(xpath);
    return str;
  }

  private ICUResourceWriter.Resource parseDelimiters(InputLocale loc, String xpath) {
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = loc.getXpathName(xpath);

    ICUResourceWriter.Resource current = table.first;
    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = loc.getXpathName(xpath);
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.QS)
          || name.equals(LDMLConstants.QE)
          || name.equals(LDMLConstants.AQS)
          || name.equals(LDMLConstants.AQE)) {
        // getXPath(node, xpath);
        if (loc.isPathNotConvertible(xpath)) {
          continue;
        }
        res = parseStringResource(loc, xpath);
      } else if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
      } else {
        System.err.println("Unknown element found: " + xpath);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }


  private ICUResourceWriter.Resource parseMeasurement() {
    String country = ULocale.getCountry(locName);
    ICUResourceWriter.Resource ret = null;
    String variant = ULocale.getVariant(locName);
    // optimization
    if (variant.length() != 0) {
      return ret;
    }

    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource first = null;
    StringBuffer xpath = new StringBuffer("//supplementalData/measurementData");
    Node root = LDMLUtilities.getNode(supplementalDoc, xpath.toString());
    if (root == null) {
      throw new RuntimeException("Could not load: " + xpath.toString());
    }
    int savedLength = xpath.length();
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      return null;
    }

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.MS)) {
        getXPath(node, xpath);
        if (isNodeNotConvertible(node, xpath)) {
          xpath.setLength(oldLength);
          continue;
        }

        String terr = LDMLUtilities.getAttributeValue(node,LDMLConstants.TERRITORIES);
        if (terr != null && ((locName.equals("root")&& terr.equals("001")) ||
                (country.length() > 0 && terr.indexOf(country) >= 0))) {
          ICUResourceWriter.ResourceInt resint = new ICUResourceWriter.ResourceInt();
          String sys = LDMLUtilities.getAttributeValue(node,LDMLConstants.TYPE);
          if (sys.equals("US")) {
            resint.val = "1";
          } else {
            resint.val = "0";
          }
          resint.name = (String) keyNameMap.get(LDMLConstants.MS);
          res = resint;
        }
      } else if (name.equals(LDMLConstants.PAPER_SIZE)) {
        String terr = LDMLUtilities.getAttributeValue(node,LDMLConstants.TERRITORIES);
        if (terr != null && ((locName.equals("root")&& terr.equals("001")) ||
                (country.length() > 0 && terr.indexOf(country) >= 0))) {
          ICUResourceWriter.ResourceIntVector vector = new ICUResourceWriter.ResourceIntVector();
          vector.name = (String) keyNameMap.get(name);
          ICUResourceWriter.ResourceInt height = new ICUResourceWriter.ResourceInt();
          ICUResourceWriter.ResourceInt width = new ICUResourceWriter.ResourceInt();
          vector.first = height;
          height.next = width;
          String type = LDMLUtilities.getAttributeValue(node,LDMLConstants.TYPE);
          /*
           * For A4 size paper the height and width are 297 mm and 210 mm repectively,
           * and for US letter size the height and width are 279 mm and 216 mm respectively.
           */
          if (type.equals("A4")) {
            height.val = "297";
            width.val = "210";
          } else if (type.equals("US-Letter")) {
            height.val = "279";
            width.val = "216";
          } else {
            throw new RuntimeException("Unknown paper type: " + type);
          }
          res = vector;
        }
      } else {
        System.err.println("Unknown element found: " + name);
        System.exit(-1);
      }
      if (res != null) {
        if (current == null) {
          current = first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }
    xpath.delete(savedLength, xpath.length());

    return first;
  }

  private ICUResourceWriter.Resource parseLayout(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = loc.getXpathName(xpath);
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      String name = loc.getXpathName(aPath);

      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, aPath);
        return res;
      }

      if (name.equals(LDMLConstants.INLIST)) {
        ICUResourceWriter.ResourceString cs = null;
        if (!loc.isPathNotConvertible(aPath)) {
          String casing = loc.getBasicAttributeValue(xpath, LDMLConstants.CASING);
          if (casing != null) {
            cs = new ICUResourceWriter.ResourceString();
            cs.comment = "Used for figuring out the casing of characters in a list.";
            cs.name = LDMLConstants.CASING;
            cs.val = casing;
            res = cs;
          }
        }
      } else if (name.equals(LDMLConstants.ORIENTATION)) {
        ICUResourceWriter.ResourceString chs = null;
        ICUResourceWriter.ResourceString lns = null;
        if (!loc.isPathNotConvertible(aPath)) {
          String characters = loc.getBasicAttributeValue(aPath, LDMLConstants.CHARACTERS);
          String lines = loc.getBasicAttributeValue(aPath, LDMLConstants.LINES);
          if (characters != null) {
            chs = new ICUResourceWriter.ResourceString();
            chs.name = LDMLConstants.CHARACTERS;
            chs.val = characters;
          }
          if (lines != null) {
            lns = new ICUResourceWriter.ResourceString();
            lns.name = LDMLConstants.LINES;
            lns.val = lines;
          }
          if (chs != null) {
            res = chs;
            chs.next = lns;
          } else {
            res = lns;
          }
        }
      } else if (name.equals(LDMLConstants.INTEXT)) {
      } else {
        System.err.println("Unknown element found: " + xpath + " / " + name);
        System.exit(-1);
      }
      if (res != null) {
        table.appendContents(res);
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseDates(InputLocale loc, String xpath) {
    ICUResourceWriter.Resource first = null;
    ICUResourceWriter.Resource current = null;
    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      return current;
    }

    // if the whole node is marked draft then
    // don't write anything
    final String stuff[] = {
      LDMLConstants.DEFAULT,
      // LDMLConstants.LPC,
      LDMLConstants.CALENDARS,
      LDMLConstants.TZN,
      // LDMLConstants.DRP,
    };

    String origXpath = xpath;
    for (int jj = 0; jj < stuff.length; jj++) {
      String name = stuff[jj];
      xpath = origXpath + "/" + name;
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        // dont compute xpath
        // res = parseAliasResource(loc, xpath);
        // handled above
      } else if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, xpath, name);
      } else if (name.equals(LDMLConstants.LPC)) {
        // localized pattern chars are deprecated
      } else if (name.equals(LDMLConstants.CALENDARS)) {
        res = parseCalendars(loc, xpath);
      } else if (name.equals(LDMLConstants.TZN)) {
        res = parseTimeZoneNames(loc, xpath);
      } else if (name.equals(LDMLConstants.DRP)) {
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    return first;
  }

  private ICUResourceWriter.Resource parseCalendars(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    table.name = LDMLConstants.CALENDAR;
    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name;
      return current;
    }

    // if the whole node is marked draft then
    // don't write anything
    final String stuff[] = {
      // LDMLConstants.ALIAS,
      LDMLConstants.DEFAULT,
      LDMLConstants.CALENDAR,
    };

    String origXpath = xpath;
    for (int jj = 0; jj < stuff.length; jj++) {
      String name = stuff[jj];
      xpath = origXpath + "/" + name;
      if (!loc.notOnDisk &&  loc.isPathNotConvertible(xpath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, xpath, name);
      } else if (name.equals(LDMLConstants.CALENDAR)) {
        Set<String> cals = loc.getByType(xpath, LDMLConstants.CALENDAR);
        for (String cal : cals) {
          res = parseCalendar(loc, xpath + "[@type=\"" + cal + "\"]");
          if (res != null) {
            table.appendContents(res);
            res = null;
          }
        }
        // if there was an item , resync current.
        if (table.first != null) {
          current = table.first.end();
        }
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseTimeZoneNames(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    table.name = (String) keyNameMap.get(loc.getXpathName(xpath));

    Set<String> zones = new HashSet<String>();
    Set<String> metazones = new HashSet<String>();
    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      String apath = iter.next();
      String name = loc.getXpathName(apath, 3);
      if (loc.isPathNotConvertible(apath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, apath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, apath, name);
      } else if (name.equals(LDMLConstants.ZONE)) {
        String tzname = loc.getAttributeValue(apath, LDMLConstants.ZONE, LDMLConstants.TYPE);
        zones.add(tzname);
      } else if (name.equals(LDMLConstants.METAZONE)) {
        String mzname = loc.getAttributeValue(apath, LDMLConstants.METAZONE, LDMLConstants.TYPE);
        metazones.add(mzname);
      } else if (
          name.equals(LDMLConstants.HOUR_FORMAT)
          || name.equals(LDMLConstants.HOURS_FORMAT)
          || name.equals(LDMLConstants.GMT_FORMAT)
          || name.equals(LDMLConstants.GMT_ZERO_FORMAT)
          || name.equals(LDMLConstants.REGION_FORMAT)
          || name.equals(LDMLConstants.FALLBACK_FORMAT)) {
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = name;
        str.val = loc.file.getStringValue(apath);
        if (str.val != null) {
          res = str;
        }
      } else if (name.equals(LDMLConstants.ABBREVIATION_FALLBACK)) {
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = name;
        str.val = loc.getBasicAttributeValue(apath, LDMLConstants.TYPE);
        if (str.val != null) {
          res = str;
        }
      } else if (
          name.equals(LDMLConstants.PREFERENCE_ORDERING)
          || name.equals(LDMLConstants.SINGLE_COUNTRIES)) {
        ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
        arr.name = name;
        ICUResourceWriter.Resource c = null;
        String[] values = null;
        if (name.equals(LDMLConstants.SINGLE_COUNTRIES)) {
          values = loc.getBasicAttributeValue(apath, LDMLConstants.LIST).split(" ");
        } else {
          String temp = loc.getBasicAttributeValue(apath, LDMLConstants.CHOICE);
          if (temp == null) {
            temp = loc.getBasicAttributeValue(apath, LDMLConstants.TYPE);
            if (temp == null) {
              throw new IllegalArgumentException("Node: " + name +
                  " must have either type or choice attribute");
            }
          }
          values = temp.split("\\s+");
        }

        for (int i = 0; i < values.length; i++) {
          ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
          str.val = values[i];
          if (c == null) {
            arr.first = c = str;
          } else {
            c.next = str;
            c = c.next;
          }
        }
        if (arr.first != null) {
          res = arr;
        }
      } else {
        System.err.println("Encountered unknown <" + apath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    for (Iterator <String > iter = zones.iterator(); iter.hasNext();) {
      ICUResourceWriter.Resource res = null;
      String zonepath = "//ldml/dates/timeZoneNames/zone[@type=\"" + iter.next() + "\"]";
      res = parseZone(loc,zonepath);
      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    for (Iterator <String > iter = metazones.iterator(); iter.hasNext();) {
      ICUResourceWriter.Resource res = null;
      String zonepath = "//ldml/dates/timeZoneNames/metazone[@type=\"" + iter.next() + "\"]";
      res = parseMetazone(loc,zonepath);
      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  /*
    private ICUResourceWriter.Resource getStringResource(String name,
            Node node, ICUResourceWriter.Resource res) {
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = name;
        str.val = LDMLUtilities.getNodeValue(node);
        if (res == null) {
            res = str;
        } else {
            findLast(res).next = str;
        }
        if (str.val == null) {
            str.val = "";
        }
        return res;
    }
   */

  private ICUResourceWriter.ResourceString getDefaultResource(InputLocale loc, String xpath) {
    return getDefaultResource(loc, xpath, loc.getXpathName(xpath));
  }

  /**
   * @deprecated
   * @param node
   * @param xpath
   * @param name
   * @return
   */
  private ICUResourceWriter.ResourceString getDefaultResource(
      Node node, StringBuffer xpath, String name) {
    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
    String temp = LDMLUtilities.getAttributeValue(node, LDMLConstants.CHOICE);
    if (temp == null) {
      temp = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
      if (temp == null) {
        throw new IllegalArgumentException("Node: " + name
                + " must have either type or choice attribute");
      }
    }

    str.name = name;
    str.val = temp;
    return str;
  }

  private ICUResourceWriter.ResourceString getDefaultResource(
          InputLocale loc, String xpath, String name) {
    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
    String temp = loc.getBasicAttributeValue(xpath, LDMLConstants.CHOICE);
    if (temp == null) {
      temp = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
      if (temp == null) {
        if (!loc.file.isHere(xpath)) {
          return null;
        }

        throw new IllegalArgumentException("Node: " + xpath +
            " must have either type or choice attribute");
      }
    }

    str.name = name;
    str.val = temp;
    return str;
  }

  private ICUResourceWriter.ResourceString getDefaultResourceWithFallback(
          InputLocale loc, String xpath, String name) {
    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();

    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    // try to get from the specified locale
    String temp = loc.getBasicAttributeValue(xpath, LDMLConstants.CHOICE);
    if (temp == null) {
      temp = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
    }
    if (temp == null) {
      temp =  loc.getBasicAttributeValue(loc.resolved(), xpath, LDMLConstants.CHOICE);
    }
    if (temp == null) {
      temp = loc.getBasicAttributeValue(loc.resolved(), xpath, LDMLConstants.TYPE);
    }

    // check final results
    if (temp == null) {
      if (!loc.file.isHere(xpath)) {
        return null;
      }

      throw new IllegalArgumentException("Node: " + xpath
              + "  must have either type or choice attribute");
    }

    // return data if we get here
    str.name = name;
    str.val = temp;
    return str;
  }

  private ICUResourceWriter.Resource parseZone(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.ResourceTable uses_mz_table = new ICUResourceWriter.ResourceTable();

    boolean containsUM = false;
    int mz_count = 0;

    String id = loc.getAttributeValue(xpath, LDMLConstants.ZONE, LDMLConstants.TYPE);

    table.name = "\"" + id + "\"";
    table.name = table.name.replace('/', ':');
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource current_mz = null;
    uses_mz_table.name = "um";

    for (Iterator <String > iter = loc.file.iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      String name = loc.getXpathName(aPath);
      ICUResourceWriter.Resource res = null;
      // a ceratain element of the list
      // is marked draft .. just dont
      // output that item
      if (loc.isPathNotConvertible(aPath)) {
        continue;
      }

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, aPath);
        if (res != null) {
          res.name = table.name;
        }
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, aPath, name);
      } else if (
          name.equals(LDMLConstants.STANDARD)
          || name.equals(LDMLConstants.DAYLIGHT)
          || name.equals(LDMLConstants.GENERIC)) {
        String shortlong = loc.getXpathName(aPath, -2).substring(0,1);
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = shortlong + name.substring(0,1);
        str.val = loc.file.getStringValue(aPath);
        if (str.val != null) {
          res = str;
        }
      } else if (name.equals(LDMLConstants.COMMONLY_USED)) {
        ICUResourceWriter.ResourceInt resint = new ICUResourceWriter.ResourceInt();
        String used = loc.file.getStringValue(aPath);
        if (used.equals("true")) {
          resint.val = "1";
        } else {
          resint.val = "0";
        }
        resint.name = "cu";
        res = resint;
      } else if (name.equals(LDMLConstants.USES_METAZONE)) {
        ICUResourceWriter.ResourceArray this_mz = new ICUResourceWriter.ResourceArray();
        ICUResourceWriter.ResourceString mzone = new ICUResourceWriter.ResourceString();
        ICUResourceWriter.ResourceString from = new ICUResourceWriter.ResourceString();
        ICUResourceWriter.ResourceString to = new ICUResourceWriter.ResourceString();

        this_mz.name = "mz" + String.valueOf(mz_count);
        this_mz.first = mzone;
        mzone.next = from;
        from.next = to;
        mz_count++;

        mzone.val = loc.getBasicAttributeValue(aPath, LDMLConstants.MZONE);
        String str = loc.getBasicAttributeValue(aPath, LDMLConstants.FROM);
        if (str != null) {
          from.val = str;
        } else {
          from.val = "1970-01-01 00:00";
        }

        str = loc.getBasicAttributeValue(aPath, LDMLConstants.TO);
        if (str != null) {
          to.val = str;
        } else {
          to.val = "9999-12-31 23:59";
        }
        if (current_mz == null) {
          uses_mz_table.first = this_mz;
          current_mz = findLast(this_mz);
        } else {
          current_mz.next = this_mz;
          current_mz = findLast(this_mz);
        }
        containsUM = true;

        res = null;
      } else if (name.equals(LDMLConstants.EXEMPLAR_CITY)) {
        String ec = loc.file.getStringValue(aPath);
        if (ec != null) {
          ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
          str.name = "ec";
          str.val = ec;
          res = str;
        }
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
    }

    // Add the metazone mapping table if mz mappings were present
    if (containsUM) {
      ICUResourceWriter.Resource res = uses_mz_table;
      if (current == null) {
        table.first = res;
        current = findLast(res);
      } else {
        current.next = res;
        current = findLast(res);
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseMetazone(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    String id = loc.getAttributeValue(xpath, LDMLConstants.METAZONE, LDMLConstants.TYPE);
    table.name = "\"meta:" + id + "\"";
    table.name = table.name.replace('/', ':');
    ICUResourceWriter.Resource current = null;

    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      String name = loc.getXpathName(aPath);
      ICUResourceWriter.Resource res = null;
      // a ceratain element of the list
      // is marked draft .. just dont
      // output that item
      if (loc.isPathNotConvertible(aPath)) {
        continue;
      }

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, aPath);
        if (res != null) {
          res.name = table.name;
        }
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, aPath, name);
      } else if (name.equals(LDMLConstants.STANDARD)
              || name.equals(LDMLConstants.DAYLIGHT)
              || name.equals(LDMLConstants.GENERIC)) {
        String shortlong = loc.getXpathName(aPath, -2).substring(0,1);
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = shortlong + name.substring(0,1);
        str.val = loc.file.getStringValue(aPath);
        if (str.val != null) {
          res = str;
        }
      } else if (name.equals(LDMLConstants.COMMONLY_USED)) {
        ICUResourceWriter.ResourceInt resint = new ICUResourceWriter.ResourceInt();
        String used = loc.file.getStringValue(aPath);
        if (used.equals("true")) {
          resint.val = "1";
        } else {
          resint.val = "0";
        }
        resint.name = "cu";
        res = resint;
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private static final String ICU_IS_LEAP_MONTH = "icu:isLeapMonth";
  private static final String ICU_LEAP_SYMBOL = "icu:leapSymbol";
  private static final String ICU_NON_LEAP_SYMBOL = "icu:nonLeapSymbol";

  private static final String leapStrings[] = {
    ICU_IS_LEAP_MONTH + "/" + ICU_NON_LEAP_SYMBOL,
    ICU_IS_LEAP_MONTH + "/" + ICU_LEAP_SYMBOL,
  };

  private ICUResourceWriter.Resource parseLeapMonth(InputLocale loc, String xpath) {
    // So.
    String theArray[] = leapStrings;
    ICUResourceWriter.ResourceString strs[] = new ICUResourceWriter.ResourceString[theArray.length];
    GroupStatus status = parseGroupWithFallback(loc, xpath, theArray, strs, false);
    if (GroupStatus.EMPTY == status) {
      System.err.println("failure: Could not load " + xpath + " - " + theArray[0] + ", etc.");
      return null; // NO items were found - don't even bother.
    }

    if (GroupStatus.SPARSE == status) {
      System.err.println("failure: Could not load all of " + xpath + " - " + theArray[0] + ", etc.");
      return null; // NO items were found - don't even bother.
    }

    ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
    arr.name = "isLeapMonth";
    for(ICUResourceWriter.ResourceString str : strs) {
      arr.appendContents(str);
    }

    return arr;
  }

  private ICUResourceWriter.Resource parseIntervalFormats(InputLocale loc, String parentxpath) {
    String xpath = parentxpath + "/" + LDMLConstants.INTVL_FMTS;
    ICUResourceWriter.Resource formats;

    formats = parseAliasResource(loc, xpath + "/" + LDMLConstants.ALIAS);
    if (formats != null) {
      formats.name = LDMLConstants.INTVL_FMTS;
      String val = ((ICUResourceWriter.ResourceAlias)formats).val;
      ((ICUResourceWriter.ResourceAlias)formats).val = val.replace(DTP + "/", "");
      return formats;
    }

    formats = new ICUResourceWriter.ResourceTable();
    formats.name = LDMLConstants.INTVL_FMTS;
    Map<String, ICUResourceWriter.ResourceTable> tableMap =
      new HashMap<String, ICUResourceWriter.ResourceTable>();

    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      ICUResourceWriter.Resource newres = null;
      String localxpath = iter.next();
      if (loc.isPathNotConvertible(localxpath)) {
        continue;
      }

      String name = loc.getXpathName(localxpath);
      if (name.equals(LDMLConstants.SPECIAL)) {
        newres = parseSpecialElements(loc, xpath);
      } else if (name.equals(LDMLConstants.INTVL_FMT_FALL)) {
        newres = new ICUResourceWriter.ResourceString(
            LDMLConstants.FALLBACK, loc.file.getStringValue(localxpath));
      } else if (name.equals(LDMLConstants.GREATEST_DIFF)) {
        String parentName = loc.getXpathName(localxpath, -2);
        String tableName = loc.getAttributeValue(localxpath, parentName, LDMLConstants.ID);
        // See if we've already created a table for this particular
        // intervalFormatItem.
        ICUResourceWriter.ResourceTable table = tableMap.get(tableName);
        if (table == null) {
          // We haven't encountered this one yet, so
          // create a new table and put it into the map.
          table = new ICUResourceWriter.ResourceTable();
          table.name = tableName;
          tableMap.put(tableName, table);
          // Update newres to reflect the fact we've created a new
          // table.  This will link the table into the resource chain
          // for the enclosing table.
          newres = table;
        }

        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = loc.getAttributeValue(localxpath, name, LDMLConstants.ID);
        str.val = loc.file.getStringValue(localxpath);

        table.appendContents(str);
      } else {
        System.err.println("Err: unknown item " + localxpath);
      }

      if (newres != null) {
        formats.appendContents(newres);
      }
    }

    if (formats.first != null) {
      return formats;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseCalendar(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    // if the whole calendar node is marked draft then
    // don't write anything
    boolean writtenAmPm = false;
    boolean writtenDTF = false;
    table.name = loc.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
    String origXpath = xpath;
    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name;
      return current;
    }

    // if the whole node is marked draft then
    // don't write anything
    final String stuff[] = {
      LDMLConstants.DEFAULT,
      LDMLConstants.MONTHS,
      LDMLConstants.DAYS,
      // LDMLConstants.WEEK,
      LDMLConstants.AM,
      LDMLConstants.PM,
      LDMLConstants.ERAS,
      LDMLConstants.DATE_FORMATS,
      LDMLConstants.TIME_FORMATS,
      LDMLConstants.DATE_TIME_FORMATS,
      LDMLConstants.SPECIAL,
      LDMLConstants.FIELDS,
      LDMLConstants.QUARTERS,
    };

    for (int jj = 0; jj < stuff.length; jj++) {
      String name = stuff[jj];
      xpath = origXpath + "/" + name;
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        if (res != null) {
          res.name = table.name;
        }
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, xpath, name);
      } else if (name.equals(LDMLConstants.MONTHS)
              || name.equals(LDMLConstants.DAYS)) {
        res = parseMonthsAndDays(loc, xpath);
      } else if (name.equals(LDMLConstants.WEEK)) {
        // ICUResourceWriter.Resource temp = parseWeek(node, xpath);
        // if (temp != null) {
          // res = temp;
        // }
        // WEEK is deprecated in CLDR 1.4
        printInfo("<week > element is deprecated and the data should moved to " + supplementalDataFile);
      } else if (name.equals(LDMLConstants.AM)
              || name.equals(LDMLConstants.PM)) {
        // TODO: figure out the tricky parts .. basically get the
        // missing element from
        // fully resolved locale !
        if (writtenAmPm == false) {
          writtenAmPm = true;
          res = parseAmPm(loc, origXpath); // We feed ampm the
          // original xpath.
        }
      } else if (name.equals(LDMLConstants.ERAS)) {
        res = parseEras(loc, xpath);
      } else if (name.equals(LDMLConstants.DATE_FORMATS)
              || name.equals(LDMLConstants.TIME_FORMATS)
              || name.equals(LDMLConstants.DATE_TIME_FORMATS)) {
        // TODO what to do if a number of formats are present?
        if (writtenDTF == false) {
          res = parseDTF(loc, origXpath);
          writtenDTF = true;
        }
        if (name.equals(LDMLConstants.DATE_TIME_FORMATS)) {
          // handle flexi formats
          ICUResourceWriter.Resource temp;

          temp = parseAliasResource(loc,xpath + "/" + LDMLConstants.ALIAS);
          if (temp != null) {
            String dtpPath = ((ICUResourceWriter.ResourceAlias)temp).val;
            // need to replace "/DateTimePatterns" = DTP at end with desired type

            ICUResourceWriter.ResourceAlias afAlias = new ICUResourceWriter.ResourceAlias();
            afAlias.name = LDMLConstants.AVAIL_FMTS;
            afAlias.val = dtpPath.replace(DTP, LDMLConstants.AVAIL_FMTS);
            res = ICUResourceWriter.Resource.addAfter(res, afAlias);

            ICUResourceWriter.ResourceAlias aaAlias = new ICUResourceWriter.ResourceAlias();
            aaAlias.name = LDMLConstants.APPEND_ITEMS;
            aaAlias.val = dtpPath.replace(DTP, LDMLConstants.APPEND_ITEMS);
            res = ICUResourceWriter.Resource.addAfter(res, aaAlias);

            ICUResourceWriter.ResourceAlias ifAlias = new ICUResourceWriter.ResourceAlias();
            ifAlias.name = LDMLConstants.INTVL_FMTS;
            ifAlias.val = dtpPath.replace(DTP, LDMLConstants.INTVL_FMTS);
            res = ICUResourceWriter.Resource.addAfter(res, ifAlias);
          } else {
            temp = parseTable(loc,xpath + "/" + LDMLConstants.AVAIL_FMTS,
                              LDMLConstants.DATE_FMT_ITEM, LDMLConstants.ID);
            if (temp != null) {
              temp.name = LDMLConstants.AVAIL_FMTS;
              res = ICUResourceWriter.Resource.addAfter(res, temp);
            }

            temp = parseTable(loc,xpath + "/" + LDMLConstants.APPEND_ITEMS,
                              LDMLConstants.APPEND_ITEM, LDMLConstants.REQUEST);
            if (temp != null) {
              temp.name = LDMLConstants.APPEND_ITEMS;
              res = ICUResourceWriter.Resource.addAfter(res, temp);
            }

            temp = parseIntervalFormats(loc, xpath);
            if (temp != null) {
              res = ICUResourceWriter.Resource.addAfter(res, temp);
            }

          }
        }
      } else if (name.equals(LDMLConstants.SPECIAL)) {
        res = parseSpecialElements(loc, xpath);
      } else if (name.equals(LDMLConstants.FIELDS)) {
        // if the whole thing is an alias
        if ((res = getAliasResource(loc, xpath)) == null) {
          ICUResourceWriter.ResourceTable subTable = new ICUResourceWriter.ResourceTable();
          subTable.name = LDMLConstants.FIELDS;
          Set <String > fields = loc.getByType(xpath, LDMLConstants.FIELD);
          for (String field : fields) {
            res = parseField(loc, xpath + "/field[@type=\"" + field + "\"]", field);
            if (res != null) {
              subTable.appendContents(res);
            }
          }
          if (!subTable.isEmpty()) {
            res = subTable;
          }
        } else {
          res.name = LDMLConstants.FIELDS;
        }
      } else if (name.equals(LDMLConstants.QUARTERS)) {
        // if (DEBUG)printXPathWarning(node, xpath);
        res = parseMonthsAndDays(loc, xpath);
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseField(InputLocale loc, String xpath, String type) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    table.name = type;
    ICUResourceWriter.ResourceString dn = null;
    ICUResourceWriter.ResourceTable relative = new ICUResourceWriter.ResourceTable();
    relative.name = LDMLConstants.RELATIVE;

    // if the whole node is marked draft then
    // dont write anything
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name;
      return current;
    }

    for (Iterator <String > iter = loc.file.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = loc.getXpathName(xpath);
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.RELATIVE)) {
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = "\"" + loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE) + "\"";
        str.val = loc.file.getStringValue(xpath);
        res = str;
        if (res != null) {
          if (current == null) {
            current = relative.first = res;
          } else {
            current.next = res;
            current = current.next;
          }
          res = null;
        }
      } else if (name.equals(LDMLConstants.DISPLAY_NAME)) {
        dn = new ICUResourceWriter.ResourceString();
        dn.name = (String) keyNameMap.get(LDMLConstants.DISPLAY_NAME);
        dn.val = loc.file.getStringValue(xpath);
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }
    }

    if (dn != null) {
      table.first = dn;
    }

    if (relative.first != null) {
      if (table.first != null) {
        table.first.next = relative;
      } else {
        table.first = relative;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseMonthsAndDays(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    String name = loc.getXpathName(xpath);
    table.name = (String) keyNameMap.get(name);

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      // System.err.println(
      //    "AR: was " + current.name + " but we think it should be " + table.name + " - on " +
      //    xpath);
      current.name = table.name; // months -> monthNames
      return current;
    }

    // if the whole node is marked draft then
    // dont write anything

    final String stuff[] = {
      LDMLConstants.DEFAULT,
      LDMLConstants.MONTH_CONTEXT,
      LDMLConstants.DAY_CONTEXT,
      LDMLConstants.QUARTER_CONTEXT,
    };

    String origXpath = xpath;
    for (int jj = 0; jj < stuff.length; jj++) {
      name = stuff[jj];
      xpath = origXpath + "/" + name;
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        // System.err.println(
        //     "AR 2: was " + res.name + " but we think it should be " + table.name + " - on "
        //     + xpath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, xpath, name);
      } else if (name.equals(LDMLConstants.MONTH_CONTEXT)
              || name.equals(LDMLConstants.DAY_CONTEXT)
              || name.equals(LDMLConstants.QUARTER_CONTEXT)) {
        Set<String> ctxs = loc.getByType(xpath, name);
        for (String ctx : ctxs) {
          res = parseContext(loc, xpath + "[@type=\"" + ctx + "\"]");
          if (res != null) {
            table.appendContents(res);
            res = null;
          }
        }
        // if there was an item , resync current.
        if (table.first != null) {
          current = table.first.end();
        }
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseContext(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;

    // if the whole collation node is marked draft then
    // don't write anything
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    String myName = loc.getXpathName(xpath);
    String resName = myName.substring(0, myName.lastIndexOf("Context"));
    table.name = loc.getAttributeValue(xpath, myName, LDMLConstants.TYPE);
    if (table.name == null) {
      throw new InternalError("Can't get table name for " + xpath + " / "
              + resName + " / " + LDMLConstants.TYPE);
    }

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name;
      return current;
    }

    // if the whole node is marked draft then
    // don't write anything

    String stuff[] = {
      LDMLConstants.DEFAULT,
      resName + "Width",
    };

    String origXpath = xpath;
    for (int jj = 0; jj < stuff.length; jj++) {
      String name = stuff[jj];
      xpath = origXpath + "/" + name;
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        if (res != null) {
          res.name = table.name;
        }
        return res; // an alias if for the resource
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, xpath, name);
      } else if (name.equals(resName + "Width")) {
        Set<String> ctxs = loc.getByType(xpath, name);
        for (String ctx : ctxs) {
          res = parseWidth(loc, resName, xpath + "[@type=\"" + ctx + "\"]");
          if (res != null) {
            table.appendContents(res);
            res = null;
          }
        }
        // if there was an item , resync current.
        if (table.first != null) {
          current = table.first.end();
        }
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private String getDayNumberAsString(String type) {
    if (type.equals("sun")) {
      return "1";
    } else if (type.equals("mon")) {
      return "2";
    } else if (type.equals("tue")) {
      return "3";
    } else if (type.equals("wed")) {
      return "4";
    } else if (type.equals("thu")) {
      return "5";
    } else if (type.equals("fri")) {
      return "6";
    } else if (type.equals("sat")) {
      return "7";
    } else{
      throw new IllegalArgumentException("Unknown type: " + type);
    }
  }

  private ICUResourceWriter.Resource parseWidth(InputLocale loc, String resName, String xpath) {
    ICUResourceWriter.ResourceArray array = new ICUResourceWriter.ResourceArray();
    ICUResourceWriter.Resource current = null;
    array.name = loc.getAttributeValue(xpath, resName + "Width", LDMLConstants.TYPE);

    // if the whole node is marked draft then
    // don't write anything
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = array.name;
      return current;
    }

    Map<String, String> map = getElementsMap(loc, resName, xpath, false);
    if (map.size() == 0) {
      // System.err.println(" -- no vals, exitting " + xpath);
      return null; // no items here.
    }

    Map<String, String >defMap = null;
    if (loc.resolved() != null) {
      defMap = getElementsMap(loc, resName, xpath, true);
    }
    Set<String> allThings = new TreeSet<String>();
    allThings.addAll(map.keySet());
    if (defMap != null) {
      allThings.addAll(defMap.keySet());
    }
    // if (defMap != null && (map.size() != defMap.size())) {
    // map = defMap;
    if ((resName.equals(LDMLConstants.DAY) && allThings.size() < 7)
        || (resName.equals(LDMLConstants.MONTH) && allThings.size() < 12)) {
      printError(
          "", "Could not get full " + resName + " array. ["
          + xpath + "] Only found " + map.size()
          + " items  in target locale (" + allThings.size()
          + " including " + ((defMap != null) ? defMap.size() : 0)
          + " inherited). Skipping.");
      return null;
    }

    // }
    if (map.size() > 0) {
      for (int i = 0; i < allThings.size(); i++) {
        String key = Integer.toString(i);
        ICUResourceWriter.ResourceString res = new ICUResourceWriter.ResourceString();
        res.val = (String) map.get(key);
        if (res.val == null && defMap != null) {
          res.val = defMap.get(key);
          if (verboseFallbackComments && res.val != null) {
            res.smallComment = " fallback";
          }
        }
        if (res.val == null) {
          printError(
              loc.locale,
              "Could not get full " + resName
              + " array., in " + xpath + " -   Missing #" + key + ".  Only found "
              + map.size() + " items (" + allThings.size()
              + " including inherited). Skipping.");
          return null;
        }

        // array of unnamed strings
        if (res.val != null) {
          if (current == null) {
            current = array.first = res;
          } else {
            current.next = res;
            current = current.next;
          }
        }
      }
    }

    // parse the default node
    {
      ICUResourceWriter.ResourceString res = getDefaultResource(loc, xpath + "/default");
      if (res != null) {
        System.err.println("Found def for " + xpath + " - " + res.val);
        if (current == null) {
          current = array.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
      }
    }

    if (array.first != null) {
      return array;
    }

    return null;
  }

  /*
    private TreeMap getElementsMap(Node root, StringBuffer xpath) {
        return getElementsMap(root, xpath, false);
    }
   */
  private static Set<String> completion_day = null;
  private static Set<String> completion_month = null;
  private static Set<String> completion_era = null;
  private static Set<String> completion_q = null;
  private static Set<String> completion_era_j = null;

  private Set<String> createNumericStringArray(int max) {
    Set<String> set = new HashSet<String>();
    for (int i = 0; i <= max; i++) {
      set.add(new Integer(i).toString());
    }
    return set;
  }

  private Set<String> getSetCompletion(InputLocale loc, String element, String xpath) {
    if (element.equals(LDMLConstants.DAY)) {
      if (completion_day == null) {
        completion_day = new HashSet <String >();
        String days[] = {
          LDMLConstants.SUN,
          LDMLConstants.MON,
          LDMLConstants.TUE,
          LDMLConstants.WED,
          LDMLConstants.THU,
          LDMLConstants.FRI,
          LDMLConstants.SAT
        };
        for (String day : days) {
          completion_day.add(day);
        }
      }
      return completion_day;
    }

    if (element.equals(LDMLConstants.MONTH)) {
      if (completion_month == null) {
        completion_month = createNumericStringArray(13);
      }
      return completion_month;
    }

    if (element.equals(LDMLConstants.ERA)) {
      if (completion_era == null) {
        completion_era = createNumericStringArray(2);
        completion_era_j = createNumericStringArray(235);
      }
      String type = loc.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
      if (type != null && type.equals("japanese")) {
        return completion_era_j;
      }
      return completion_era;
    }

    if (element.equals(LDMLConstants.QUARTER)) {
      if (completion_q == null) {
        completion_q = createNumericStringArray(4);
      }
      return completion_q;
    }

    System.err.println("Warning: no known completion for " + element);
    return null;
  }

  /**
   * @param loc
   * @param xpath
   * @param isNodeFromRoot
   * @return
   */
  private Map<String, String> getElementsMap(
      InputLocale loc, String element, String xpath, boolean fromResolved) {
    Map<String, String> map = new TreeMap<String, String>();
    CLDRFile whichFile;
    if (fromResolved) {
      whichFile = loc.resolved();
    } else {
      whichFile = loc.file;
    }

    String origXpath = xpath;
    for (Iterator<String> iter = whichFile.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      if (loc.isPathNotConvertible(whichFile, xpath)) {
        continue;
      }

      String name = loc.getXpathName(xpath);
      String val = whichFile.getStringValue(xpath);
      String caltype = loc.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
      String type = loc.getAttributeValue(xpath, name, LDMLConstants.TYPE);
      String yeartype = loc.getAttributeValue(xpath, name, LDMLConstants.YEARTYPE);

      if (name.equals(LDMLConstants.DAY)) {
        map.put(LDMLUtilities.getDayIndexAsString(type), val);
      } else if (name.equals(LDMLConstants.MONTH)) {
        if (caltype.equals("hebrew")
            && type.equals("7")
            && yeartype != null
            && yeartype.equals("leap")) {
            type = "14"; // Extra month name for hebrew Adar II in leap years
        }
        map.put(LDMLUtilities.getMonthIndexAsString(type), val);
      } else if (name.equals(LDMLConstants.ERA)) {
        map.put(type, val);
      } else if (name.equals(LDMLConstants.QUARTER)) {
        map.put(LDMLUtilities.getMonthIndexAsString(type), val);
      } else if (name.equals(LDMLConstants.ALIAS)) {
        if (fromResolved) {
          continue; // OK - inherits .
        }

        System.err.println(
            "Encountered unknown alias <res:"
            + fromResolved + " - " + xpath + " / " + name
            + "> subelement: " + name);
        System.exit(-1);
      } else {
        System.err.println(
            "Encountered unknown <res:" + fromResolved + " - " + xpath + " / " + name
            + "> subelement: " + name);
        System.exit(-1);
      }
    }

    Set<String> completion = getSetCompletion(loc, element, xpath);
    if (completion != null) {
      for (String type : completion) {
        xpath = origXpath + "/" + element + "[@type=\"" + type + "\"]";
        if (loc.isPathNotConvertible(whichFile, xpath)) {
          continue;
        }
        String name = loc.getXpathName(xpath);
        String val = whichFile.getStringValue(xpath);
        if (val == null) {
          continue;
        }

        // String type = loc.getAttributeValue(xpath, name,
        // LDMLConstants.TYPE);
        if (name.equals(LDMLConstants.DAY)) {
          map.put(LDMLUtilities.getDayIndexAsString(type), val);
        } else if (name.equals(LDMLConstants.MONTH)) {
          map.put(LDMLUtilities.getMonthIndexAsString(type), val);
        } else if (name.equals(LDMLConstants.ERA)) {
          map.put(type, val);
        } else if (name.equals(LDMLConstants.QUARTER)) {
          map.put(LDMLUtilities.getMonthIndexAsString(type), val);
        } else {
          throw new InternalError("Unknown name " + name);
        }
      }
      // System.err.println("After completion on " + origXpath +":
      // " + mapSize +" -> " + map.size());
    }

    return map;
  }

  /*
    private TreeMap getElementsMap(Node root, StringBuffer xpath,
            boolean isNodeFromRoot) {
        TreeMap map = new TreeMap();
        int saveLength = xpath.length();
        for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType()!= Node.ELEMENT_NODE) {
                continue;
            }
            getXPath(node, xpath);
            if (isNodeNotConvertible(node, xpath)) {
                xpath.setLength(saveLength);
                continue;
            }

            String name = node.getNodeName();
            String val = LDMLUtilities.getNodeValue(node);
            String type = LDMLUtilities.getAttributeValue(node,LDMLConstants.TYPE);

            if (name.equals(LDMLConstants.DAY)) {
                map.put(LDMLUtilities.getDayIndexAsString(type), val);
            }else if (name.equals(LDMLConstants.MONTH)) {
                map.put(LDMLUtilities.getMonthIndexAsString(type), val);
            }else if (name.equals(LDMLConstants.ERA)) {
                map.put(type, val);
            }else if (name.equals(LDMLConstants.QUARTER)) {
                map.put(LDMLUtilities.getMonthIndexAsString(type), val);
            }else{
                System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: "
                    + name);
                System.exit(-1);
            }
            xpath.setLength(saveLength);
        }
        return map;
    }
   */

  private ICUResourceWriter.Resource parseWeek() {
    String country = ULocale.getCountry(locName);
    ICUResourceWriter.Resource ret = null;
    String variant = ULocale.getVariant(locName);
    // optimization
    if (variant.length() != 0) {
      return ret;
    }

    StringBuffer xpath = new StringBuffer("//supplementalData/weekData");
    Node root = LDMLUtilities.getNode(supplementalDoc, xpath.toString());
    if (root != null) {
      ICUResourceWriter.Resource week = parseWeekend(root, xpath, country);
      ICUResourceWriter.Resource dte = parseDTE(root, xpath, country);
      if (week != null) {
        week.next = dte;
        ret = week;
      } else {
        ret = dte;
      }
    }

    return ret;
  }

  private int getMillis(String time) {
    String[] strings = time.split(":"); // time is in hh:mm format
    int hours = Integer.parseInt(strings[0]);
    int minutes = Integer.parseInt(strings[1]);
    return (hours * 60  + minutes) * 60 * 1000;
  }

  private Node getVettedNode(
      Node ctx, String node, String attrb, String attrbVal, StringBuffer xpath) {

    int savedLength = xpath.length();
    NodeList list = LDMLUtilities.getNodeList(ctx, node, null, xpath.toString());
    Node ret = null;
    for (int i = 0; i <list.getLength(); i++) {
      Node item = list.item(i);
      String val = LDMLUtilities.getAttributeValue(item, attrb);
      getXPath(item, xpath);
      if (val.matches(".*\\b" + attrbVal + "\\b.*")) {
        if (!isNodeNotConvertible(item, xpath)) {
          ret = item;
        }
        break;
      }
      xpath.setLength(savedLength);
    }

    xpath.setLength(savedLength);
    return ret;
  }

  private ICUResourceWriter.Resource parseWeekend(Node root, StringBuffer xpath, String country) {
    Node wkendStart = null;
    Node wkendEnd = null;
    if (country.length()>0) {
      wkendStart = getVettedNode(
          root, LDMLConstants.WENDSTART, LDMLConstants.TERRITORIES, country, xpath);
      wkendEnd = getVettedNode(
          root, LDMLConstants.WENDEND, LDMLConstants.TERRITORIES, country, xpath);
    }

    if (wkendEnd != null || wkendStart != null || locName.equals("root")) {
      if (wkendStart == null) {
        wkendStart = getVettedNode(
            null, root, LDMLConstants.WENDSTART + "[@territories='001']", xpath, true);
        if (wkendStart == null) {
          printError("parseWeekend", "Could not find weekendStart resource.");
        }
      }
      if (wkendEnd == null) {
        wkendEnd = getVettedNode(
            null, root, LDMLConstants.WENDEND + "[@territories='001']", xpath, true);
        if (wkendEnd == null) {
          printError("parseWeekend", "Could not find weekendEnd resource.");
        }
      }
    }

    ICUResourceWriter.ResourceIntVector wkend = null;
    if (wkendStart != null && wkendEnd != null) {
      try {
        wkend = new ICUResourceWriter.ResourceIntVector();
        wkend.name = LDMLConstants.WEEKEND;
        ICUResourceWriter.ResourceInt startday = new ICUResourceWriter.ResourceInt();
        startday.val = getDayNumberAsString(
            LDMLUtilities.getAttributeValue(wkendStart, LDMLConstants.DAY));
        ICUResourceWriter.ResourceInt starttime = new ICUResourceWriter.ResourceInt();
        String time = LDMLUtilities.getAttributeValue(wkendStart, LDMLConstants.TIME);
        starttime.val = Integer.toString(getMillis(time == null?"00:00":time));
        ICUResourceWriter.ResourceInt endday = new ICUResourceWriter.ResourceInt();
        endday.val = getDayNumberAsString(
            LDMLUtilities.getAttributeValue(wkendEnd, LDMLConstants.DAY));
        ICUResourceWriter.ResourceInt endtime = new ICUResourceWriter.ResourceInt();

        time = LDMLUtilities.getAttributeValue(wkendEnd, LDMLConstants.TIME);
        endtime.val = Integer.toString(getMillis(time == null?"24:00":time));

        wkend.first = startday;
        startday.next = starttime;
        starttime.next = endday;
        endday.next = endtime;
      } catch(NullPointerException ex) {
        throw new RuntimeException(ex);
      }
    }

    return wkend;
  }

  private ICUResourceWriter.Resource parseDTE(Node root, StringBuffer xpath, String country) {
    Node minDays = null;
    Node firstDay = null;
    ICUResourceWriter.ResourceIntVector dte = null;

    if (country.length()>0) {
      minDays = getVettedNode(
          root, LDMLConstants.MINDAYS, LDMLConstants.TERRITORIES, country, xpath);
      firstDay = getVettedNode(
          root, LDMLConstants.FIRSTDAY, LDMLConstants.TERRITORIES, country, xpath);
    }

    if (minDays != null || firstDay != null || locName.equals("root")) {
      // fetch inherited to complete the resource..
      if (minDays == null) {
        minDays = getVettedNode(
            root, LDMLConstants.MINDAYS, LDMLConstants.TERRITORIES, "001", xpath);
        if (minDays == null) {
          printError("parseDTE", "Could not find minDays resource.");
        }
      }
      if (firstDay == null) {
        firstDay = getVettedNode(
            root, LDMLConstants.FIRSTDAY, LDMLConstants.TERRITORIES, "001", xpath);
        if (firstDay == null) {
          printError("parseDTE", "Could not find firstDay resource.");
        }
      }
    }

    if (minDays != null && firstDay != null) {
      dte = new ICUResourceWriter.ResourceIntVector();
      ICUResourceWriter.ResourceInt int1 = new ICUResourceWriter.ResourceInt();
      int1.val = getDayNumberAsString(LDMLUtilities.getAttributeValue(firstDay, LDMLConstants.DAY));
      ICUResourceWriter.ResourceInt int2 = new ICUResourceWriter.ResourceInt();
      int2.val = LDMLUtilities.getAttributeValue(minDays, LDMLConstants.COUNT);

      dte.name = DTE;
      dte.first = int1;
      int1.next = int2;
    }

    if ((minDays == null && firstDay != null) || (minDays != null && firstDay == null)) {
      System.err.println(
          "WARNING: Could not find minDays = " + minDays + " or firstDay = " + firstDay
          + " from fullyResolved locale. Not producing the resource. " + xpath.toString());
      return null;
    }

    return dte;
  }

  private ICUResourceWriter.Resource parseEras(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    table.name = LDMLConstants.ERAS;

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name;
      return current;
    }

    // if the whole node is marked draft then
    // don't write anything
    final String stuff[] = {
      LDMLConstants.DEFAULT,
      LDMLConstants.ERAABBR,
      LDMLConstants.ERANAMES,
      LDMLConstants.ERANARROW,
    };
    String origXpath = xpath;
    for (int jj = 0; jj < stuff.length; jj++) {
      String name = stuff[jj];
      xpath = origXpath + "/" + name;
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, xpath, name);
      } else if (name.equals(LDMLConstants.ERAABBR)) {
        res = parseEra(loc, xpath, LDMLConstants.ABBREVIATED);
      } else if (name.equals(LDMLConstants.ERANAMES)) {
        res = parseEra(loc, xpath, LDMLConstants.WIDE);
      } else if (name.equals(LDMLConstants.ERANARROW)) {
        res = parseEra(loc, xpath, LDMLConstants.NARROW);
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseEra(InputLocale loc, String xpath, String name) {
    ICUResourceWriter.ResourceArray array = new ICUResourceWriter.ResourceArray();
    ICUResourceWriter.Resource current = null;
    array.name = name;
    String resName = LDMLConstants.ERA;

    // if the whole node is marked draft then
    // don't write anything
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = array.name;
      return current;
      // } else {
      // System.err.println("NO alias in: " + xpath);
    }

    Map<String, String> map = getElementsMap(loc, resName, xpath, false);
    if (map.size() == 0) {
      // System.err.println(" -- no vals, exitting " + xpath);
      return null;
    }

    Map<String, String> defMap = null;
    if (loc.resolved() != null) {
      defMap = getElementsMap(loc, resName, xpath, true);
    }
    Set<String> allThings = new TreeSet<String>();
    allThings.addAll(map.keySet());
    if (defMap != null) {
      allThings.addAll(defMap.keySet());
    }

    // / "special" hack for japanese narrow. If so, we'll have an alternate
    // set on standby.
    Map<String, String> nonNarrow = null;
    boolean isJapaneseNarrow = xpath.equals(
        "//ldml/dates/calendars/calendar[@type=\"japanese\"]/eras/eraNarrow");
    if (isJapaneseNarrow) {
      nonNarrow = getElementsMap(loc, resName, xpath.replaceAll(
              "eraNarrow", "eraAbbr"), true); // will NOT fallback from
      // specials.
      // System.err.println("xpath: " + xpath + ", resName: " + resName +
      // " - needs japanese hack.");
      allThings.addAll(nonNarrow.keySet());
    }

    if (map.size() > 0) {
      for (int i = 0; i < allThings.size(); i++) {
        String key = Integer.toString(i);
        ICUResourceWriter.ResourceString res = new ICUResourceWriter.ResourceString();
        res.val = (String) map.get(key);
        if (res.val == null && defMap != null) {
          res.val = defMap.get(key);
          if (verboseFallbackComments && res.val != null) {
            res.smallComment = " fallback";
          }
        }
        if (res.val == null && nonNarrow != null) {
          res.val = nonNarrow.get(key);
          if (res.val != null) {
            res.smallComment = "(abbr.)";
          }
        }
        if (res.val == null) {
          printError(
              "",
              "Could not get full " + resName
              + " array at " + xpath + " -  Missing #" + key
              + ".  Only found " + map.size() + " items ("
              + allThings.size()
              + " including inherited). Fatal error exiting.");
          // NB: see workaround for Japanese-narrow above.
          throw new InternalError("data problem");
        }

        // array of unnamed strings
        if (res.val != null) {
          if (current == null) {
            current = array.first = res;
          } else {
            current.next = res;
            current = current.next;
          }
        }
      }
    }

    if (array.first != null) {
      return array;
    }

    return null;
  }

  private boolean isNodeNotConvertible(Node node, StringBuffer xpath) {
    return isNodeNotConvertible(node, xpath, false, false);
  }

  private boolean isNodeNotConvertible(
      Node node, StringBuffer xpath, boolean isCollation, boolean isNodeFromParent) {
    // only deal with leaf nodes !
    // Here we assume that the CLDR files are normalized
    // and that the draft attributes are only on leaf nodes
    if (LDMLUtilities.areChildrenElementNodes(node) && !isCollation) {
      return false;
    }

    if (isNodeFromParent) {
      return false;
    }

    return !xpathList.contains(xpath.toString());
  }

  /*
    private Node getVettedNode(Node parent, String childName, StringBuffer xpath) {
        return getVettedNode(fullyResolvedDoc, parent, childName, xpath, true);
    }
   */

  public Node getVettedNode(
      Document fullyResolvedDoc, Node parent, String childName, StringBuffer xpath,
      boolean ignoreDraft) {

    // NodeList list = LDMLUtilities.getNodeList(
    //     parent, childName, fullyResolvedDoc, xpath.toString());
    String ctx = "./" + childName;
    NodeList list = LDMLUtilities.getNodeList(parent, ctx);
    int saveLength = xpath.length();
    Node ret = null;
    if ((list == null || list.getLength() < 0)) {
      if (fullyResolvedDoc != null) {
        int oldLength = xpath.length();
        xpath.append("/");
        xpath.append(childName);
        // try from fully resolved
        list = LDMLUtilities.getNodeList(fullyResolvedDoc, xpath.toString());
        // we can't depend on isNodeNotConvertible to return the correct
        // data since xpathList will not contain xpaths of nodes from
        // parent so we just return the first one we encounter.
        // This has a side effect of ignoring the config specifiation!
        if (list != null && list.getLength() > 0) {
          ret = list.item(0);
        }
        xpath.setLength(oldLength);
      }
    } else {
      // getVettedNode adds the node name of final node to xpath.
      // chop off the final node name from xpath.
      int end = childName.lastIndexOf('/');
      if (end > 0) {
        xpath.append('/');
        xpath.append(childName.substring(0, end));
      }
      ret = getVettedNode(list, xpath, ignoreDraft);
    }

    xpath.setLength(saveLength);
    return ret;
  }

  private Node getVettedNode(NodeList list, StringBuffer xpath, boolean ignoreDraft) {
    Node node = null;
    int oldLength = xpath.length();
    for (int i = 0; i < list.getLength(); i++) {
      node = list.item(i);
      getXPath(node, xpath);
      if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        node = null;
        continue;
      }
      break;
    }

    xpath.setLength(oldLength);
    return node;
  }

  private ICUResourceWriter.Resource parseAmPm(InputLocale loc, String xpath) {
    String[] AMPM = {
      LDMLConstants.AM,
      LDMLConstants.PM
    };
    ICUResourceWriter.ResourceString[] strs = new ICUResourceWriter.ResourceString[AMPM.length];
    String[] paths = new String[AMPM.length];
    ICUResourceWriter.Resource first = null;
    int validCount = 0;
    for (int i = 0; i < AMPM.length; i++) {
      strs[i] = new ICUResourceWriter.ResourceString();
      first = ICUResourceWriter.ResourceString.addAfter(first, strs[i]);
      paths[i] = xpath + "/" + AMPM[i];
      if (!loc.isPathNotConvertible(paths[i])) {
        strs[i].val = loc.file.getStringValue(paths[i]);
        if (strs[i].val != null) {
          validCount++;
        }
      }
    }

    if (validCount == 0) {
      return null;
    }

    if (validCount < AMPM.length) {
      for (int i = 0; i < AMPM.length; i++) {
        if (strs[i].val == null && !loc.isPathNotConvertible(loc.resolved(), paths[i])) {
          strs[i].val = loc.resolved().getStringValue(paths[i]);
          if (strs[i].val != null) {
            if (verboseFallbackComments) {
              strs[i].smallComment = " fallback";
            }
            validCount++;
          }
        }
      }
    }

    if (validCount != AMPM.length) {
      throw new InternalError(
          "On " + xpath + " (AMPM) - need "
          + AMPM.length + " strings but only have " + validCount
          + " after inheritance.");
    }

    // ok, set up the res
    ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
    arr.name = AM_PM_MARKERS;
    arr.first = first;
    return arr;
  }

  /**
   * parse all DTF
   *
   * @param loc
   * @param xpath -
   *            the base xpath ending at calendar[@type=...]
   * @return
   */
  // TODO figure out what to do for alias, draft and alt elements
  private static final String STD_SUFFIX = "[@type=\"standard\"]/pattern[@type=\"standard\"]";

  private static final String[] dtf_paths = new String[] {
    "timeFormats/timeFormatLength[@type=\"full\"]/timeFormat" + STD_SUFFIX,
    "timeFormats/timeFormatLength[@type=\"long\"]/timeFormat" + STD_SUFFIX,
    "timeFormats/timeFormatLength[@type=\"medium\"]/timeFormat" + STD_SUFFIX,
    "timeFormats/timeFormatLength[@type=\"short\"]/timeFormat" + STD_SUFFIX,
    "dateFormats/dateFormatLength[@type=\"full\"]/dateFormat" + STD_SUFFIX,
    "dateFormats/dateFormatLength[@type=\"long\"]/dateFormat" + STD_SUFFIX,
    "dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat" + STD_SUFFIX,
    "dateFormats/dateFormatLength[@type=\"short\"]/dateFormat" + STD_SUFFIX,
    "dateTimeFormats/dateTimeFormatLength[@type=\"medium\"]/dateTimeFormat" + STD_SUFFIX,
    "dateTimeFormats/dateTimeFormatLength[@type=\"full\"]/dateTimeFormat" + STD_SUFFIX,
    "dateTimeFormats/dateTimeFormatLength[@type=\"long\"]/dateTimeFormat" + STD_SUFFIX,
    "dateTimeFormats/dateTimeFormatLength[@type=\"medium\"]/dateTimeFormat" + STD_SUFFIX,
    "dateTimeFormats/dateTimeFormatLength[@type=\"short\"]/dateTimeFormat" + STD_SUFFIX,
  };

  private static final String[] ValidNumberingSystems = 
      ("arab arabext armn armnlow beng deva ethi fullwide geor grek greklow gujr guru hans " +
      "hansfin hant hantfin hebr jpan jpanfin knda khmr laoo latn mlym mong mymr orya roman " +
      "romanlow taml telu thai tibt").split("\\s+");

  private static final Set<String> NUMBER_SYSTEMS = new HashSet<String>(Arrays.asList(ValidNumberingSystems));
  // TODO: update to get from supplemental data: <variable id="$numberSystem" type="choice">
  // "arab arabext armn armnlow beng deva ethi fullwide geor grek greklow gujr guru hans " +
  // "hansfin hant hantfin hebr jpan jpanfin knda khmr laoo latn mlym mong mymr orya roman " +
  // "romanlow taml telu thai tibt</variable>"

  private ICUResourceWriter.Resource parseDTF(InputLocale loc, String xpath) {
    // TODO change the ICU format to reflect LDML format
    /*
     * The prefered ICU format would be timeFormats{ default{} full{} long{}
     * medium{} short{} .... } dateFormats{ default{} full{} long{} medium{}
     * short{} ..... } dateTimeFormats{ standard{} .... }
     */

    ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
    String[] theArray = dtf_paths;
    arr.name = DTP;
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.ResourceString strs[] = new ICUResourceWriter.ResourceString[theArray.length];
    String nsov[] = new String[theArray.length];

    GroupStatus status = parseGroupWithFallback(loc, xpath, theArray, strs, false);
    if (GroupStatus.EMPTY == status) {
      // TODO: cldrbug #2188: What follows is a hack because of
      // mismatch between CLDR & ICU format, need to do something
      // better for next versions of CLDR (> 1.7) & ICU (> 4.2).  If
      // any dateFormats/dateFormatLength,
      // timeFormats/timeFormatLength, or
      // dateTimeFormats/dateTimeFormatLength items are present,
      // status != GroupStatus.EMPTY and we don't get here. If we do
      // get here, then dateFormats and timeFormats elements are empty
      // or aliased, and dateTimeFormats has at least no
      // dateTimeFormats items, and is probably aliased (currently in
      // this case it is always aliased, and this only happens in
      // root). We need to get an alias from one of these elements
      // (for ICU format we need to create a single alias covering all
      // three of these elements). However, parseAliasResource doesn't
      // know how to make an alias from the dateFormats or timeFormats
      // elements (since there is no direct match in ICU to either of
      // these alone). It does know how from the dateTimeFormats, so
      // we will try that. This would fail if dateTimeFormats were not
      // aliased when dateFormats and timeFormats both were, but that
      // does not happen currently.
      //
      ICUResourceWriter.Resource alias = parseAliasResource(
          loc, xpath + "/" + LDMLConstants.DATE_TIME_FORMATS + "/" + LDMLConstants.ALIAS);
      if (alias != null) {
        alias.name = DTP;
      }
      return alias;
    }

    if (GroupStatus.SPARSE == status) {
      // Now, we have a problem.
      String type = loc.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
      if (!type.equals("gregorian")) {
        System.err.println(
            loc.locale + " " + xpath
            + " - some items are missing, attempting fallback from gregorian");
        ICUResourceWriter.ResourceString gregstrs[] =
            new ICUResourceWriter.ResourceString[theArray.length];
        GroupStatus gregstatus = parseGroupWithFallback(
            loc, xpath.replaceAll("\"" + type + "\"", "\"gregorian\""), theArray, gregstrs, false);
        if ((gregstatus != GroupStatus.EMPTY) && (gregstatus != GroupStatus.SPARSE)) {
          // They have something, let's see if it is enough;
          for (int j = 0; j < theArray.length; j++) {
            if (strs[j].val == null && gregstrs[j].val != null) {
              strs[j].val = gregstrs[j].val;
              strs[j].smallComment = " fallback from 'gregorian' ";
            }
          }
        }
      }
    }

    // Now determine if any Numbering System Overrides are present
    for (int i = 0; i < theArray.length; i++) {
      XPathParts xpp = new XPathParts();
      String aPath = xpath + "/" + dtf_paths[i];
      if (loc.file.isHere(aPath)) {
        String fullPath = loc.file.getFullXPath(aPath);
        xpp.set(fullPath);
        String numbersOverride =
            xpp.findAttributeValue(LDMLConstants.PATTERN, LDMLConstants.NUMBERS);
        if (numbersOverride != null) {
          nsov[i] = numbersOverride;
        }
      }
    }

    // Glue pattern default
    ICUResourceWriter.ResourceString res = getDefaultResourceWithFallback(
        loc, xpath + "/dateTimeFormats/" + LDMLConstants.DEFAULT, LDMLConstants.DEFAULT);
    int glueIndex = 8;
    if (res != null && res.val.trim().equalsIgnoreCase("full")) {
      glueIndex += 1;
    }
    if (res != null && res.val.trim().equalsIgnoreCase("long")) {
      glueIndex += 2;
    }
    if (res != null && res.val.trim().equalsIgnoreCase("medium")) {
      glueIndex += 3;
    }
    if (res != null && res.val.trim().equalsIgnoreCase("short")) {
      glueIndex += 4;
    }
    strs[8].val = strs[glueIndex].val;

    // write out the data
    int n = 0;
    for (ICUResourceWriter.ResourceString str : strs) {
      if (str.val == null) {
        printError(loc.locale,xpath + " - null value at " + n);
        System.exit(-1);
      }

      if (nsov[n] != null) {
        // We have a numbering system override - output an array containing the override
        ICUResourceWriter.ResourceString nso = new ICUResourceWriter.ResourceString();
        nso.val = nsov[n];

        ICUResourceWriter.ResourceArray nso_array = new ICUResourceWriter.ResourceArray();
        nso_array.first = str;
        str.next = nso;

        if (current == null) {
          current = arr.first = nso_array;
        } else {
          current.next = nso_array;
          current = current.next;
        }
      } else {
        // Just a normal pattern - output a string
        if (current == null) {
          current = arr.first = str;
        } else {
          current.next = str;
          current = current.next;
        }
      }
      n++;
    }

    if (arr.first != null) {
      return arr;
    }

    return null;
  }

  //    private ICUResourceWriter.Resource parseFlexibleFormats(InputLocale loc,
  //            String xpath) {
  //        xpath = xpath + "/" + LDMLConstants.AVAIL_FMTS;
  //
  //
  //
  //
  //         throw new InternalError("not imp " + xpath);
  //         ICUResourceWriter.Resource current = null, first = null;
  //
  //         int savedLength = xpath.length();
  //         getXPath(root, xpath);
  //         int oldLength = xpath.length();
  //
  //         for (Node node = root.getFirstChild(); node != null; node = node
  //         .getNextSibling()) {
  //         if (node.getNodeType() != Node.ELEMENT_NODE) {
  //         continue;
  //         }
  //         String name = node.getNodeName();
  //         ICUResourceWriter.Resource res = null;
  //
  //         if (name.equals(LDMLConstants.ALIAS)) {
  //         // TODO: Do nothing for now, fix when ICU is updated to reflect
  //         // CLDR structure
  //         } else if (name.equals(LDMLConstants.AVAIL_FMTS)
  //         || name.equals(LDMLConstants.APPEND_ITEMS)) {
  //         res = parseItems(loc, xpath);
  //         } else if (name.equals(LDMLConstants.DTFL)) {
  //         // TODO: Do nothing for now, fix when ICU is updated to reflect
  //         // CLDR structure
  //         } else {
  //         System.err.println("Encountered unknown <" + root.getNodeName()
  //         + "> subelement: " + name);
  //         System.exit(-1);
  //         }
  //         if (res != null) {
  //         if (current == null) {
  //         first = res;
  //         current = findLast(res);
  //         } else {
  //         current.next = res;
  //         current = findLast(res);
  //         }
  //         res = null;
  //         }
  //         xpath.delete(oldLength, xpath.length());
  //         }
  //         xpath.delete(savedLength, xpath.length());
  //         if (first != null) {
  //         return first;
  //         }
  //         return null;
  //    }
  //
  //    private ICUResourceWriter.Resource parseItems(InputLocale loc, String xpath) {
  //        throw new InternalError("not imp " + xpath);
  //         ICUResourceWriter.Resource current = null;
  //         int savedLength = xpath.length();
  //         getXPath(root, xpath);
  //         int oldLength = xpath.length();
  //         ICUResourceWriter.ResourceTable table = new
  //         ICUResourceWriter.ResourceTable();
  //         table.name = root.getNodeName();
  //
  //         // if the whole node is marked draft then
  //         // dont write anything
  //         if (isNodeNotConvertible(root, xpath)) {
  //         xpath.setLength(savedLength);
  //         return null;
  //         }
  //
  //         for (Node node = root.getFirstChild(); node != null; node = node
  //         .getNextSibling()) {
  //         if (node.getNodeType() != Node.ELEMENT_NODE) {
  //         continue;
  //         }
  //         String name = node.getNodeName();
  //         ICUResourceWriter.Resource res = null;
  //
  //         if (name.equals(LDMLConstants.ALIAS)) {
  //         res = parseAliasResource(node, xpath);
  //         res.name = name;
  //         return res;
  //         } else if (name.equals(LDMLConstants.DATE_FMT_ITEM)) {
  //         getXPath(node, xpath);
  //         if (isNodeNotConvertible(node, xpath)) {
  //         xpath.setLength(oldLength);
  //         continue;
  //         }
  //         ICUResourceWriter.ResourceString str = new
  //         ICUResourceWriter.ResourceString();
  //         str.name = LDMLUtilities.getAttributeValue(node,
  //         LDMLConstants.ID);
  //         str.val = LDMLUtilities.getNodeValue(node);
  //         res = str;
  //         } else if (name.equals(LDMLConstants.APPEND_ITEM)) {
  //         getXPath(node, xpath);
  //         if (isNodeNotConvertible(node, xpath)) {
  //         xpath.setLength(oldLength);
  //         continue;
  //         }
  //         ICUResourceWriter.ResourceString str = new
  //         ICUResourceWriter.ResourceString();
  //         str.name = LDMLUtilities.getAttributeValue(node,
  //         LDMLConstants.REQUEST);
  //         str.val = LDMLUtilities.getNodeValue(node);
  //         res = str;
  //         } else if (name.equals(LDMLConstants.DTFL)) {
  //         // Already parsed this element in parseDTF
  //         continue;
  //         } else {
  //         System.err.println("Encountered unknown <" + root.getNodeName()
  //         + "> subelement: " + name);
  //         System.exit(-1);
  //         }
  //         if (res != null) {
  //         if (current == null) {
  //         table.first = res;
  //         current = findLast(res);
  //         } else {
  //         current.next = res;
  //         current = res;
  //         }
  //         res = null;
  //         }
  //         xpath.delete(oldLength, xpath.length());
  //         }
  //         xpath.delete(savedLength, xpath.length());
  //         if (table.first != null) {
  //         return table;
  //         }
  //         return null;
  //    }

  private ICUResourceWriter.Resource parseNumbers(InputLocale loc, String xpath) {
    ICUResourceWriter.Resource current = null, first = null;
    boolean writtenFormats = false;
    boolean writtenCurrencyFormatPlurals = false;
    boolean writtenCurrencies = false;
    boolean writtenCurrencyPlurals = false;
    boolean writtenCurrencySpacing = false;

    String origXpath = xpath;
    String names[] = {
      LDMLConstants.ALIAS,
      LDMLConstants.DEFAULT,
      LDMLConstants.SYMBOLS,
      LDMLConstants.DECIMAL_FORMATS,
      LDMLConstants.PERCENT_FORMATS,
      LDMLConstants.SCIENTIFIC_FORMATS,
      LDMLConstants.CURRENCY_FORMATS,
      LDMLConstants.CURRENCIES,
      // Currencies appears twice so we can handle the plurals.
      LDMLConstants.CURRENCIES,
      LDMLConstants.DEFAULT_NUMBERING_SYSTEM
    };
    for (String name : names) {
      xpath = origXpath + "/" + name;
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        if (!loc.file.isHere(xpath)) {
          continue;
        }
        res = parseAliasResource(loc, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        if (!loc.file.isHere(xpath)) {
          continue;
        }
        res = getDefaultResource(loc, xpath, name);
      } else if (name.equals(LDMLConstants.SYMBOLS)) {
        res = parseSymbols(loc, xpath);
      } else if (name.equals(LDMLConstants.DECIMAL_FORMATS)
              || name.equals(LDMLConstants.PERCENT_FORMATS)
              || name.equals(LDMLConstants.SCIENTIFIC_FORMATS)
              || name.equals(LDMLConstants.CURRENCY_FORMATS)) {
        if (writtenFormats == false) {
          res = parseNumberFormats(loc, xpath);
          writtenFormats = true;
        } else if (writtenCurrencyFormatPlurals == false) {
          res = parseCurrencyFormatPlurals(loc, origXpath);
          writtenCurrencyFormatPlurals = true;
        } else if (writtenCurrencySpacing == false) {
          res = parseCurrencySpacing(loc, origXpath);
          writtenCurrencySpacing = true;
        }
      } else if (name.equals(LDMLConstants.CURRENCIES)) {
        if (writtenCurrencies == false) {
          res = parseCurrencies(loc, xpath);
          writtenCurrencies = true;
        }
        else if (writtenCurrencyPlurals == false) {
          res = parseCurrencyPlurals(loc, origXpath);
          writtenCurrencyPlurals = true;
        }
      } else if (name.equals(LDMLConstants.DEFAULT_NUMBERING_SYSTEM)) {
        res = parseDefaultNumberingSystem(loc, xpath);
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
    }

    if (first != null) {
      return first;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseUnits(
      InputLocale loc, String xpath, String tableName, String altValue) {

    ICUResourceWriter.ResourceTable unitsTable = new ICUResourceWriter.ResourceTable();
    unitsTable.name = tableName;
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource first = null;

    String origXpath = xpath;
    String names[] = {
      LDMLConstants.ALIAS,
      LDMLConstants.UNIT,
      LDMLConstants.SPECIAL
    };
    for (String name : names) {
      xpath = origXpath + "/" + name;
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        if (!loc.file.isHere(xpath)) {
          continue;
        }
        res = parseAliasResource(loc, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.UNIT)) {
        res = parseUnit(loc, xpath, altValue);
      } else if (name.equals(LDMLConstants.SPECIAL)) {
        res = parseSpecialElements(loc, xpath);
      } else {
        System.err.println("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          first = res;
        } else {
          current.next = res;
        }
        current = res;
        res = null;
      }
    }

    if (first != null) {
      unitsTable.first = first;
      return unitsTable;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseUnit(InputLocale loc, String xpath, String altValue) {
    Map<String, ICUResourceWriter.ResourceTable> tableMap =
      new HashMap<String, ICUResourceWriter.ResourceTable>();
    ICUResourceWriter.Resource first = null;
    ICUResourceWriter.Resource last = null;

    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      String localxpath = iter.next();
      if (altValue == null && loc.isPathNotConvertible(localxpath)) {
        continue;
      }

      String name = loc.getXpathName(localxpath);
      if (name.equals(LDMLConstants.UNIT)) {
        System.err.println("Err: unknown item " + localxpath);
        continue;
      }

      if (name.equals(LDMLConstants.UNIT_PATTERN)) {
        String currentAltValue = loc.getAttributeValue(localxpath, LDMLConstants.ALT);
        if (altValue != null) {
          if (currentAltValue == null || !altValue.equals(currentAltValue)) {
            continue;
          }
          // OK
        } else if (altValue == null && currentAltValue != null) {
          continue;
        }

        String parentName = loc.getXpathName(localxpath, -2);
        String tableName = loc.getAttributeValue(localxpath, parentName, LDMLConstants.TYPE);
        ICUResourceWriter.ResourceTable current = tableMap.get(tableName);
        if (current == null) {
          current = new ICUResourceWriter.ResourceTable();
          current.name = tableName;
          tableMap.put(tableName, current);
          // Hook everything up, because we need to
          // return the sibling chain.
          if (last == null) {
            first = current;
          } else {
            last.next = current;
          }
          last = current;
        }

        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = loc.getAttributeValue(localxpath, name, LDMLConstants.COUNT);
        str.val = loc.file.getStringValue(localxpath);
        current.appendContents(str);
      } else {
        System.err.println("Err: unknown item " + localxpath);
        continue;
      }
    }

    return first;
  }

  enum GroupStatus {
    EMPTY, SPARSE, FALLBACK, COMPLETE
  };

  /**
   * Parse some things, with fallback behavior
   *
   * @param loc
   * @param xpathBase
   *            the base xpath
   * @param xpaths
   *            the xpath (with slash before) to append to the base
   * @param res
   *            output array to hold res strings. must be same size as xpaths
   * @param preferNativeNumberSymbols fix the number symbols
   * @return Will return the lowest possible value that applies to any item,
   *         or GROUP_EMPTY if no items could be filled in
   */
  private GroupStatus parseGroupWithFallback(
      InputLocale loc, String xpathBase, String xpaths[], ICUResourceWriter.ResourceString res[],
      boolean preferNativeNumberSymbols) {

    String[] values = new String[xpaths.length];
    XPathParts xpp = new XPathParts();
    boolean someNonDraft = false;
    boolean anyExtant = false;
    for (int i = 0; i < xpaths.length; i++) {
      String aPath = xpathBase + "/" + xpaths[i];
      if (preferNativeNumberSymbols) {
        aPath = reviseAPath(loc, xpp, aPath);
      }
      if (loc.file.isHere(aPath)) {
        anyExtant = true;
        if (!loc.isPathNotConvertible(aPath)) {
          values[i] = loc.file.getStringValue(aPath);
          if (values[i] != null) {
            someNonDraft = true;
          }
        }
      }
    }

    GroupStatus status = GroupStatus.EMPTY;
    if (!anyExtant && !someNonDraft) {
      // System.err.println("No " + xpathBase + " for " + loc.locale);
      return status;
    }

    if (someNonDraft == true) {
      status = GroupStatus.COMPLETE;
      for (int i = 0; i < xpaths.length; i++) {
        res[i] = new ICUResourceWriter.ResourceString();
        String temp = values[i];
        if (temp == null) {
          String aPath = xpathBase + "/" + xpaths[i];
          if (preferNativeNumberSymbols) {
            aPath = reviseAPath(loc, xpp, aPath);
          }
          temp = loc.resolved().getStringValue(aPath);
          if (temp != null) {
            CLDRFile.Status fileStatus = new CLDRFile.Status();
            String foundIn = loc.resolved().getSourceLocaleID(
                    aPath, fileStatus);
            if (verboseFallbackComments) {
              res[i].smallComment = " From " + foundIn;
            }
            if (status != GroupStatus.SPARSE) {
              status = GroupStatus.FALLBACK;
            }
            // System.err.println("Fallback from " + foundIn + " in "
            // + loc.locale + " / " + aPath);
          } else {
            printInfo(loc.locale + " Can't complete array for " + xpathBase + " at " + aPath);
            status = GroupStatus.SPARSE;
          }
        }
        res[i].val = temp;
      }
      return status;
    }

    //      System.err.println(xpathBase + " - no non draft?");
    return GroupStatus.EMPTY;
  }

  private String reviseAPath(InputLocale loc, XPathParts xpp, String aPath) {
    // This is a clumsy way to do it, but the code for the converter is so convoluted...
    Set<String> paths = loc.file.getPaths(aPath, null, null);
    // We have all the paths that match, now. We prefer ones that have an
    // alt value that is a valid number system
    for (String path : paths) {
      String distinguishing = loc.file.getDistinguishingXPath(path, null, false);
      if (distinguishing.contains("@numberSystem")) {
        String alt = xpp.set(distinguishing).getAttributeValue(-1, "numberSystem");
        if (NUMBER_SYSTEMS.contains(alt)) {
          aPath = distinguishing;
          break;
        } 
      }
    }

    return aPath;
  }

  private static final String[] sym_paths = new String[] {
    LDMLConstants.DECIMAL,
    LDMLConstants.GROUP,
    LDMLConstants.LIST,
    LDMLConstants.PERCENT_SIGN,
    LDMLConstants.NATIVE_ZERO_SIGN,
    LDMLConstants.PATTERN_DIGIT,
    LDMLConstants.MINUS_SIGN,
    LDMLConstants.EXPONENTIAL,
    LDMLConstants.PER_MILLE,
    LDMLConstants.INFINITY,
    LDMLConstants.NAN,
    LDMLConstants.PLUS_SIGN,
  };

  private ICUResourceWriter.Resource parseSymbols(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
    arr.name = NUMBER_ELEMENTS;
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.ResourceString strs[] = new ICUResourceWriter.ResourceString[sym_paths.length];
    GroupStatus status = parseGroupWithFallback(loc, xpath, sym_paths, strs, !asciiNumbers);
    if (GroupStatus.EMPTY == status || GroupStatus.SPARSE == status) {
      return null;
    }

    for (ICUResourceWriter.ResourceString str : strs) {
      if (current == null) {
        current = arr.first = str;
      } else {
        current.next = str;
        current = current.next;
      }
    }

    if (arr.first != null) {
      return arr;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseCurrencyPlurals(InputLocale loc, String xpath) {
    // This resource is a table of tables, with each subtable containing
    // the data for a particular currency.  The structure of the XML file
    // is as follows:
    //
    // <ldml>
    //   <numbers>
    //     <currencies>
    //       <currency type="ADP">
    //         <displayName>Andorran Peseta</displayName>
    //         <displayName count="one">Andorran peseta</displayName>
    //         <displayName count="other">Andorran pesetas</displayName>
    //       </currency>
    //       <currency type="AED">
    //         <displayName>United Arab Emirates Dirham</displayName>
    //         <displayName count="one">UAE dirham</displayName>
    //         <displayName count="other">UAE dirhams</displayName>
    //         <symbol>AED</symbol>
    // 	     </currency>
    //     </currencies>
    //   </numbers>
    // </ldml>
    //
    // For this resource, the only interesting parts are the "displayName"
    // elements that have a "count" attribute.
    //
    // The resulting resource for this particular example would look like this:
    //
    //     CurrencyPlurals{
    //        ADP{
    //            one{"Andorran peseta"}
    //            other{"Andorran pesetas"}
    //        }
    //        AED{
    //            one{"UAE dirham"}
    //            other{"UAE dirhams"}
    //        }

    ICUResourceWriter.ResourceTable parentTable = new ICUResourceWriter.ResourceTable();
    parentTable.name = LDMLConstants.CURRENCY_PLURALS;
    String xpathCurrency = xpath + "/currencies/currency";

    // Since the leaf elements are not grouped by currency in the InputLocale
    // instance, this map will hold the table for each currency.
    Map<String, ICUResourceWriter.ResourceTable> tableMap =
      new HashMap<String, ICUResourceWriter.ResourceTable>();

    ICUResourceWriter.Resource last = null;
    for (Iterator<String> iter = loc.file.iterator(xpathCurrency); iter.hasNext();) {
      String localxpath = iter.next();
      if (loc.isPathNotConvertible(localxpath)) {
        continue;
      }

      String name = loc.getXpathName(localxpath);
      if (!name.equals(LDMLConstants.DISPLAY_NAME)) {
        continue;
      }

      // We only care about the elements with a "count" attribute.
      String count = loc.getAttributeValue(localxpath, name, LDMLConstants.COUNT);
      if (count != null) {
        String parentName = loc.getXpathName(localxpath, -2);
        String tableName = loc.getAttributeValue(localxpath, parentName, LDMLConstants.TYPE);
        ICUResourceWriter.ResourceTable current = tableMap.get(tableName);
        if (current == null) {
          current = new ICUResourceWriter.ResourceTable();
          current.name = tableName;
          tableMap.put(tableName, current);
          // If this is the first table, then update the
          // start of the children in the parent table.
          if (last == null) {
            parentTable.first = current;
          }
          else {
            last.next = current;
          }
          last = current;
        }

        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = count;
        str.val = loc.file.getStringValue(localxpath);
        current.appendContents(str);
      }
    }

    if (parentTable.first != null) {
      return parentTable;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseCurrencyFormatPlurals(InputLocale loc, String xpath) {
    // This table contains formatting patterns for this locale's currency.
    // Each pattern is represented by a "unitPattern" element in the XML file.
    // Each pattern is represented as a string in the resource format, with
    // the name of the string being the value of the "count" attribute.

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = LDMLConstants.CURRENCY_UNIT_PATTERNS;
    String xpathUnitPattern = xpath + "/currencyFormats/unitPattern";
    for (Iterator<String> iter = loc.file.iterator(xpathUnitPattern); iter.hasNext();) {
      String localxpath = iter.next();
      if (loc.isPathNotConvertible(localxpath)) {
        continue;
      }
      String name = loc.getXpathName(localxpath);
      ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
      str.name = loc.getAttributeValue(localxpath, name, LDMLConstants.COUNT);
      str.val = loc.file.getStringValue(localxpath);
      table.appendContents(str);
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private static final String[] CurrencySections = new String[] {
    LDMLConstants.CURRENCY_SPC_BEFORE,
    LDMLConstants.CURRENCY_SPC_AFTER
  };

  private ICUResourceWriter.Resource parseCurrencySpacing(InputLocale loc, String xpath) {
    // This table contains formatting patterns for this locale's currency.
    // Syntax example in XML file:
    //  <currencyFormats>
    //    <currencySpacing>
    //      <beforeCurrency>
    //          <currencyMatch>[:letter:]</currencyMatch>
    //          <surroundingMatch>[:digit:]</surroundingMatch>
    //          <insertBetween> </insertBetween>
    //      </beforeCurrency>
    //      <afterCurrency>
    //          <currencyMatch>[:letter:]</currencyMatch>
    //          <surroundingMatch>[:digit:]</surroundingMatch>
    //          <insertBetween> </insertBetween>
    //      </afterCurrency>
    //    </currencySpacing>
    //  </currencyFormats>

    ICUResourceWriter.ResourceTable table = null;
    ICUResourceWriter.ResourceTable current = null;
    ICUResourceWriter.ResourceTable first = null;
    for (String section : CurrencySections) {
      String xpathUnitPattern = xpath + "/" + LDMLConstants.CURRENCY_FORMATS + "/"
          + LDMLConstants.CURRENCY_SPACING + "/" + section;
      int count = 0;
      for (Iterator<String> iter = loc.file.iterator(xpathUnitPattern); iter.hasNext();) {
        String localxpath = iter.next();
        if (loc.isPathNotConvertible(localxpath)) {
          continue;
        }

        if (table == null) {
          table = new ICUResourceWriter.ResourceTable();
          table.name = LDMLConstants.CURRENCY_SPACING;
        }

        if (count++ == 0) {
          current = new ICUResourceWriter.ResourceTable();
          current.name = section;
          if (first == null) {
            table.first = current;
            first = current;
          } else {
            first.next = current;
          }
        }

        String name = loc.getXpathName(localxpath);
        ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
        str.name = name;
        str.val = loc.file.getStringValue(localxpath);
        current.appendContents(str);
      }
    }

    if (table != null) {
      return table;
    }

    return null;
  }

  // TODO figure out what to do for alias, draft and alt elements
  private static final String[] num_paths = new String[] {
    "decimalFormats/decimalFormatLength/decimalFormat" + STD_SUFFIX,
    "currencyFormats/currencyFormatLength/currencyFormat" + STD_SUFFIX,
    "percentFormats/percentFormatLength/percentFormat" + STD_SUFFIX,
    "scientificFormats/scientificFormatLength/scientificFormat" + STD_SUFFIX
  };

  private ICUResourceWriter.Resource parseNumberFormats(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
    String[] theArray = num_paths;
    arr.name = NUMBER_PATTERNS;
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.ResourceString strs[] = new ICUResourceWriter.ResourceString[theArray.length];
    GroupStatus status = parseGroupWithFallback(loc, "//ldml/numbers", theArray, strs, false);
    if (GroupStatus.EMPTY == status) {
      return null; // NO items were found
    }

    for (ICUResourceWriter.ResourceString str : strs) {
      if (current == null) {
        current = arr.first = str;
      } else {
        current.next = str;
        current = current.next;
      }
    }

    if (arr.first != null) {
      return arr;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseCurrencies(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;

    // if the whole node is marked draft then
    // don't write anything
    String origXpath = xpath;

    // collect a list of all currencies, ensure no dups.
    Set<String> currs = new HashSet<String>();
    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = loc.getXpathName(xpath);
      // System.err.println("$: " + xpath + " // " + name);
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, xpath, name);
      } else /* if (name.equals(LDMLConstants.CURRENCY)) */ {
        String type = loc.findAttributeValue(xpath, LDMLConstants.TYPE);
        if ((type == null) || (type.equals("standard"))) {
          continue;
        }

        if (currs.contains(type)) {
          // System.err.println("$$$ dup " + type);
          continue; // dup
        }

        res = parseCurrency(loc, origXpath + "/currency[@type=\"" + type + "\"]", type);
        currs.add(type);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }
    }

    if (table.first != null) {
      // lookup only if needed
      table.name = (String) keyNameMap.get(loc.getXpathName(origXpath));
      return table;
    }

    return null;
  }

  static final String curr_syms[] = {
    LDMLConstants.SYMBOL, // 0
    LDMLConstants.DISPLAY_NAME, // 1
    LDMLConstants.PATTERN, // 2
    LDMLConstants.DECIMAL, // 3
    LDMLConstants.GROUP, // 4
  };

  private ICUResourceWriter.Resource parseCurrency(InputLocale loc, String xpath, String type) {
    /*
     * Node alias = LDMLUtilities.getNode(root, LDMLConstants.ALIAS,
     * fullyResolvedDoc, xpath.toString()); if (alias != null) {
     * ICUResourceWriter.Resource res = parseAliasResource(alias, xpath);
     * res.name = LDMLUtilities .getAttributeValue(root,
     * LDMLConstants.TYPE); xpath.delete(savedLength, xpath.length());
     * return res; }
     */

    ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
    arr.name = type;
    ICUResourceWriter.ResourceString strs[] = new ICUResourceWriter.ResourceString[curr_syms.length];
    GroupStatus status = parseGroupWithFallback(loc, xpath, curr_syms, strs, false);
    if (status == GroupStatus.EMPTY) {
      String full = loc.resolved().getFullXPath(xpath);
      String val = loc.resolved().getStringValue(xpath);
      System.err.println(
          "totally empty - Failed to parse: " + type
          + " at xpath " + xpath + " - full value " + full
          + " value " + val);
      return null;
    }

    // aliases - for sanity
    ICUResourceWriter.ResourceString symbol = strs[0];
    ICUResourceWriter.ResourceString displayName = strs[1];
    ICUResourceWriter.ResourceString pattern = strs[2];
    ICUResourceWriter.ResourceString decimal = strs[3];
    ICUResourceWriter.ResourceString group = strs[4];

    // 0 - symb
    if (symbol.val != null) {
      String choice = loc.getBasicAttributeValue(
          xpath + "/" + curr_syms[0], LDMLConstants.CHOICE);
      if (choice == null) {
        String fullPathInh = loc.resolved().getFullXPath(xpath + "/" + curr_syms[0]);
        if (fullPathInh != null) {
          choice = loc.getAttributeValue(fullPathInh, LDMLConstants.CHOICE);
        }
      }
      if (choice != null && choice.equals("true") && !loc.isPathNotConvertible(xpath + "/symbol")) {
        symbol.val = "=" + symbol.val.replace('\u2264', '#').replace("&lt;","<");
        if (true || verboseFallbackComments) {
          if (symbol.smallComment != null) {
            symbol.smallComment = symbol.smallComment + " - (choice)";
          } else {
            symbol.smallComment = "(choice)";
          }
        }
      }
    } else {
      symbol.val = type;
      if (true || verboseFallbackComments) {
        symbol.smallComment = "===";
      }
    }

    // 1 - disp
    if (displayName.val == null) {
      displayName.val = type;
      if (true || verboseFallbackComments) {
        symbol.smallComment = "===";
      }
    }

    arr.first = symbol;
    symbol.next = displayName;
    if (pattern.val != null || decimal.val != null || group.val != null) {
      boolean isPatternDup = false;
      boolean isDecimalDup = false;
      boolean isGroupDup = false;
      if (pattern.val == null) {
        pattern.val = loc.getResolvedString(
            "//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat" + STD_SUFFIX);
        isPatternDup = true;
        if (pattern.val == null) {
          throw new RuntimeException("Could not get pattern currency resource!!");
        }
      }

      XPathParts xpp = new XPathParts();
      if (decimal.val == null) {
        String aPath = "//ldml/numbers/symbols/decimal";
        if (!asciiNumbers) {
          aPath = reviseAPath(loc, xpp, aPath);
        }

        decimal.val = loc.getResolvedString(aPath);
        isDecimalDup = true;
        if (decimal.val == null) {
          throw new RuntimeException("Could not get decimal currency resource!!");
        }
      }

      if (group.val == null) {
        String aPath = "//ldml/numbers/symbols/group";
        if (!asciiNumbers) {
          aPath = reviseAPath(loc, xpp, aPath);
        }

        group.val = loc.getResolvedString(aPath);
        isGroupDup = true;
        if (group.val == null) {
          throw new RuntimeException("Could not get group currency resource!!");
        }
      }

      ICUResourceWriter.ResourceArray elementsArr = new ICUResourceWriter.ResourceArray();
      pattern.comment = isPatternDup ? "Duplicated from NumberPatterns resource" : null;
      decimal.comment = isDecimalDup ? "Duplicated from NumberElements resource" : null;
      group.comment = isGroupDup ? "Duplicated from NumberElements resource" : null;

      elementsArr.first = pattern;
      pattern.next = decimal;
      decimal.next = group;
      if (displayName.val != null) {
        displayName.next = elementsArr;
      } else {
        System.err.println(
            "WARNING: displayName and symbol not vetted/available for currency resource "
            + arr.name + " not generating the resource");
      }
    }

    if (arr.first != null) {
      return arr;
    }

    return null;
  }

  private ICUResourceWriter.Resource parsePosix(InputLocale loc, String xpath) {
    return null;
    // ICUResourceWriter.Resource first = null;
    // ICUResourceWriter.Resource current = null;
    //
    // int savedLength = xpath.length();
    // getXPath(root, xpath);
    // int oldLength = xpath.length();
    //
    // // if the whole node is marked draft then
    // // dont write anything
    // if (isNodeNotConvertible(root, xpath)) {
    // xpath.setLength(savedLength);
    // return null;
    // }
    // for (Node node = root.getFirstChild(); node != null; node = node
    // .getNextSibling()) {
    // if (node.getNodeType() != Node.ELEMENT_NODE) {
    // continue;
    // }
    //
    // String name = node.getNodeName();
    // ICUResourceWriter.Resource res = null;
    // if (name.equals(LDMLConstants.MESSAGES)) {
    // res = parseMessages(node, xpath);
    // } else if (name.equals(LDMLConstants.ALIAS)) {
    // res = parseAliasResource(node, xpath);
    // } else {
    // System.err.println("Unknown element found: " + xpath + " / " + name);
    // System.exit(-1);
    // }
    // if (res != null) {
    // if (current == null) {
    // current = first = res;
    // } else {
    // current.next = res;
    // current = current.next;
    // }
    // res = null;
    // }
    // xpath.delete(oldLength, xpath.length());
    // }
    // xpath.delete(savedLength, xpath.length());
    // return first;
  }

  /*
    private ICUResourceWriter.Resource parseMessages(InputLocale loc, String xpath) {
        ICUResourceWriter.ResourceTable table = new
        ICUResourceWriter.ResourceTable();
        ICUResourceWriter.Resource current = null;

        int savedLength = xpath.length();
        getXPath(root, xpath);
        int oldLength = xpath.length();
       	// if the whole node is marked draft then
        // dont write anything
        if (isNodeNotConvertible(root, xpath)) {
        xpath.setLength(savedLength);
        return null;
        }
        table.name = root.getNodeName();

        for (Node node = root.getFirstChild(); node != null; node = node
        .getNextSibling()) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
        }
        String name = node.getNodeName();
        ICUResourceWriter.Resource res = null;
        if (name.equals(LDMLConstants.YESSTR)
        || name.equals(LDMLConstants.YESEXPR)
        || name.equals(LDMLConstants.NOSTR)
        || name.equals(LDMLConstants.NOEXPR)) {
        getXPath(node, xpath);
        if (isNodeNotConvertible(node, xpath)) {
        xpath.setLength(oldLength);
        continue;
        }
        ICUResourceWriter.ResourceString str = new
        ICUResourceWriter.ResourceString();
        str.name = name;
        str.val = LDMLUtilities.getNodeValue(node);
        res = str;
        } else if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        } else {
        System.err.println("Unknown element found: " + xpath + " / " + name);
        System.exit(-1);
        }
        if (res != null) {
        if (current == null) {
        current = table.first = res;
        } else {
        current.next = res;
        current = current.next;
        }
        res = null;
        }
        xpath.delete(oldLength, xpath.length());
        }
        xpath.delete(savedLength, xpath.length());
        if (table.first != null) {
        return table;
        }
        return null;
    }
   */

  /**
   * Shim. Transitions us from CLDRFile based processing to DOM.
   *
   * @param loc
   * @param xpath
   * @return
   */
  public ICUResourceWriter.Resource parseCollations(InputLocale loc, String xpath) {
    if (loc.notOnDisk) {
      // attempt to parse a 'fake' locale.
      ICUResourceWriter.Resource first = getAliasResource(loc, xpath);
      if (first != null) {
        first.name =  LDMLConstants.COLLATIONS;
        return first;
      }

      // now, look for a default
      first = getDefaultResource(loc, xpath + "/" + LDMLConstants.DEFAULT, LDMLConstants.DEFAULT);
      if (first != null) {
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        table.name = LDMLConstants.COLLATIONS;
        table.first = first;
        return table;
      }

      // now, look for aliases
      Set<String> collTypes = loc.getByType(xpath, LDMLConstants.COLLATION, LDMLConstants.TYPE);
      for(String type : collTypes) {
        ICUResourceWriter.Resource res = getAliasResource(
            loc, xpath + "/" + LDMLConstants.COLLATION + "[@type=\"" + type + "\"]");
        if (res != null) {
          first = ICUResourceWriter.Resource.addAfter(first, res);
        } else {
          throw new InternalError(
              "FAIL: locale " + loc.locale + " not on disc, and non-alias collation " + type
              + " encountered.");
        }
      }

      return first; // could be null.
    }

    // parse using DOM-based code
    Node collations = loc.getTopNode(LDMLConstants.COLLATIONS);
    if (collations == null) {
      throw new InternalError("Can't get top level collations node");
    }

    ICUResourceWriter.Resource table = parseCollations(
        collations, new StringBuffer("//ldml"), true);
    if (table == null || (table.isEmpty() && table instanceof ICUResourceWriter.ResourceTable)) {
      printWarning(loc.locale, " warning: No collations found. Bundle will be empty.");
      return null;
    }

    return table;
  }

  public ICUResourceWriter.Resource parseCollations(
      Node root, StringBuffer xpath, boolean checkIfConvertible) {

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    table.name = root.getNodeName();
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole collatoin node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    current = table.first = parseValidSubLocales(root, xpath);
    for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.COLLATION)) {
        res = parseCollation(node, xpath, checkIfConvertible);
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseValidSubLocales(Node root, StringBuffer xpath) {
    return null;
    /*
        String loc = LDMLUtilities.getAttributeValue(root,LDMLConstants.VALID_SUBLOCALE);
        if (loc != null) {
            String[] locales = loc.split("\u0020");
            if (locales != null && locales.length >0) {
                ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
                ICUResourceWriter.Resource current = null;
                table.name = LDMLConstants.VALID_SUBLOCALE;
                for(int i = 0; i <locales.length; i++) {
                    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                    str.name = locales[i];
                    str.val = "";
                    if (current == null) {
                        current = table.first = str;
                    }else{
                        current.next = str;
                        current = str;
                    }
                }
                return table;
            }
        }
        return null;
     */
  }

  private ICUResourceWriter.Resource parseCollation(
      Node root, StringBuffer xpath, boolean checkIfConvertible) {

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    ICUResourceWriter.Resource current = null;
    table.name = LDMLUtilities.getAttributeValue(root, LDMLConstants.TYPE);
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole collation node is marked draft then
    // don't write anything
    if (checkIfConvertible && isNodeNotConvertible(root, xpath, true, false)) {
      xpath.setLength(savedLength);
      return null;
    }

    StringBuffer rules = new StringBuffer();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath, true);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.RULES)) {
        Node alias = LDMLUtilities.getNode(
            node, LDMLConstants.ALIAS , fullyResolvedDoc, xpath.toString());
        getXPath(node, xpath);
        if (alias != null) {
          res = parseAliasResource(alias, xpath);
        } else {
          rules.append(parseRules(node, xpath));
        }
      } else if (name.equals(LDMLConstants.SETTINGS)) {
        rules.append(parseSettings(node));
      } else if (name.equals(LDMLConstants.SUPPRESS_CONTRACTIONS)) {
        if (DEBUG) {
          System.out.println("");
        }
        int index = rules.length();
        rules.append("[suppressContractions ");
        rules.append(LDMLUtilities.getNodeValue(node));
        rules.append(" ]");
        if (DEBUG) {
          System.out.println(rules.substring(index));
        }
      } else if (name.equals(LDMLConstants.OPTIMIZE)) {
        rules.append("[optimize ");
        rules.append(LDMLUtilities.getNodeValue(node));
        rules.append(" ]");
      } else if (name.equals(LDMLConstants.BASE)) {
        // TODO Dont know what to do here
        // if (DEBUG)printXPathWarning(node, xpath);
        rules.append(parseBase(node, xpath, oldLength));
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          current = table.first = res;
        } else {
          current.next = res;
          current = current.next;
        }
        res = null;
      }
      xpath.delete(oldLength, xpath.length());
    }

    if (rules != null) {
      ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
      str.name = LDMLConstants.SEQUENCE;
      str.val = rules.toString();
      if (current == null) {
        current = table.first = str;
      } else {
        current.next = str;
        current = current.next;
      }
      str = new ICUResourceWriter.ResourceString();
      str.name = "Version";
      str.val = ldmlVersion; //"1.0";
      /*
       * Not needed anymore
        if (specialsDoc != null) {
            Node version = LDMLUtilities.getNode(specialsDoc, xpath.append("/special").toString());
            if (version != null) {
                str.val = LDMLUtilities.getAttributeValue(version, "icu:version");
            }
        }
       */
      current.next = str;
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private String parseBase(Node node, StringBuffer xpath, int oldLength) {
    String myxp = xpath.substring(0, oldLength);
    // backward compatibility
    String locale = LDMLUtilities.getNodeValue(node);
    if (locale == null) {
      locale = LDMLUtilities.getAttributeValue(node, LDMLConstants.LOCALE);
    }
    if (locale != null) {
      String fn =  locale + ".xml";
      Document colDoc =  LDMLUtilities.getFullyResolvedLDML(
          sourceDir, fn, false, false, false, true);
      Node col = LDMLUtilities.getNode(colDoc, myxp);
      if (col != null) {
        ICUResourceWriter.ResourceTable table = (ICUResourceWriter.ResourceTable) parseCollation(
            col, new StringBuffer(myxp), false);
        if (table != null) {
          ICUResourceWriter.Resource current = table.first;
          while(current != null) {
            if (current instanceof ICUResourceWriter.ResourceString) {
              ICUResourceWriter.ResourceString temp = (ICUResourceWriter.ResourceString)current;
              if (temp.name.equals(LDMLConstants.SEQUENCE)) {
                return temp.val;
              }
            }
            current = current.next;
          }
        } else {
          printWarning(fn, "Collation node could not be parsed for " + myxp);
        }
      } else {
        printWarning(fn, "Could not find col from xpath: " + myxp);
      }
    } else {
      printWarning(fileName, "Could not find locale from xpath: " + xpath.toString());
    }

    return "";
  }

  private StringBuffer parseSettings(Node node) {
    StringBuffer rules = new StringBuffer();

    String strength = LDMLUtilities.getAttributeValue(node, LDMLConstants.STRENGTH);
    if (strength != null) {
      rules.append(" [strength ");
      rules.append(getStrength(strength));
      rules.append(" ]");
    }

    String alternate = LDMLUtilities.getAttributeValue(node, LDMLConstants.ALTERNATE);
    if (alternate != null) {
      rules.append(" [alternate ");
      rules.append(alternate);
      rules.append(" ]");
    }

    String backwards = LDMLUtilities.getAttributeValue(node, LDMLConstants.BACKWARDS);
    if (backwards != null && backwards.equals("on")) {
      rules.append(" [backwards 2]");
    }

    String normalization = LDMLUtilities.getAttributeValue(node, LDMLConstants.NORMALIZATION);
    if (normalization != null) {
      rules.append(" [normalization ");
      rules.append(normalization);
      rules.append(" ]");
    }

    String caseLevel = LDMLUtilities.getAttributeValue(node, LDMLConstants.CASE_LEVEL);
    if (caseLevel != null) {
      rules.append(" [caseLevel ");
      rules.append(caseLevel);
      rules.append(" ]");
    }

    String caseFirst = LDMLUtilities.getAttributeValue(node, LDMLConstants.CASE_FIRST);
    if (caseFirst != null) {
      rules.append(" [caseFirst ");
      rules.append(caseFirst);
      rules.append(" ]");
    }

    String hiraganaQ = LDMLUtilities.getAttributeValue(node, LDMLConstants.HIRAGANA_Q);
    if (hiraganaQ == null) {
      // try the deprecated version
      hiraganaQ = LDMLUtilities.getAttributeValue(node, LDMLConstants.HIRAGANA_Q_DEP);
    }
    if (hiraganaQ != null) {
      rules.append(" [hiraganaQ ");
      rules.append(hiraganaQ);
      rules.append(" ]");
    }

    String numeric = LDMLUtilities.getAttributeValue(node, LDMLConstants.NUMERIC);
    if (numeric != null) {
      rules.append(" [numericOrdering ");
      rules.append(numeric);
      rules.append(" ]");
    }

    return rules;
  }

  private static final TreeMap collationMap = new TreeMap();
  static {
    collationMap.put("first_tertiary_ignorable", "[first tertiary ignorable ]");
    collationMap.put("last_tertiary_ignorable",  "[last tertiary ignorable ]");
    collationMap.put("first_secondary_ignorable","[first secondary ignorable ]");
    collationMap.put("last_secondary_ignorable", "[last secondary ignorable ]");
    collationMap.put("first_primary_ignorable",  "[first primary ignorable ]");
    collationMap.put("last_primary_ignorable", "[last primary ignorable ]");
    collationMap.put("first_variable", "[first variable ]");
    collationMap.put("last_variable", "[last variable ]");
    collationMap.put("first_non_ignorable", "[first regular]");
    collationMap.put("last_non_ignorable", "[last regular ]");
    //TODO check for implicit
    //collationMap.put("??",      "[first implicit]");
    //collationMap.put("??",       "[last implicit]");
    collationMap.put("first_trailing", "[first trailing ]");
    collationMap.put("last_trailing", "[last trailing ]");
  }

  private StringBuffer parseRules(Node root, StringBuffer xpath) {
    StringBuffer rules = new StringBuffer();

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.PC)
          || name.equals(LDMLConstants.SC)
          || name.equals(LDMLConstants.TC)
          || name.equals(LDMLConstants.QC)
          || name.equals(LDMLConstants.IC)) {
        Node lastVariable = LDMLUtilities.getNode(node, LDMLConstants.LAST_VARIABLE , null, null);
        if (lastVariable != null) {
          if (DEBUG) {
            rules.append(" ");
          }
          rules.append(collationMap.get(lastVariable.getNodeName()));
        } else {
          String data = getData(node,name);
          rules.append(data);
        }
      } else if (name.equals(LDMLConstants.P)
                 || name.equals(LDMLConstants.S)
                 || name.equals(LDMLConstants.T)
                 || name.equals(LDMLConstants.Q)
                 || name.equals(LDMLConstants.I)) {
        Node lastVariable = LDMLUtilities.getNode(node, LDMLConstants.LAST_VARIABLE , null, null);
        if (lastVariable != null) {
          if (DEBUG) {
            rules.append(" ");
          }
          rules.append(collationMap.get(lastVariable.getNodeName()));
        } else {
          String data = getData(node, name);
          rules.append(data);
        }
      } else if (name.equals(LDMLConstants.X)) {
        rules.append(parseExtension(node));
      } else if (name.equals(LDMLConstants.RESET)) {
        rules.append(parseReset(node));
      } else{
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
    }

    return rules;
  }

  private static final UnicodeSet needsQuoting = new UnicodeSet(
      "[[:whitespace:][:c:][:z:][[:ascii:]-[a-zA-Z0-9]]]");
  private static StringBuffer quoteOperandBuffer = new StringBuffer(); // faster

  private static final String quoteOperand(String s) {
    s = Normalizer.normalize(s, Normalizer.NFC);
    quoteOperandBuffer.setLength(0);
    boolean noQuotes = true;
    boolean inQuote = false;
    int cp;
    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(s, i);
      if (!needsQuoting.contains(cp)) {
        if (inQuote) {
          quoteOperandBuffer.append('\'');
          inQuote = false;
        }
        quoteOperandBuffer.append(UTF16.valueOf(cp));
      } else {
        noQuotes = false;
        if (cp == '\'') {
          quoteOperandBuffer.append("''");
        } else {
          if (!inQuote) {
            quoteOperandBuffer.append('\'');
            inQuote = true;
          }
          if (cp > 0xFFFF) {
            quoteOperandBuffer.append("\\U").append(Utility.hex(cp,8));
          } else if (cp <= 0x20 || cp > 0x7E) {
            quoteOperandBuffer.append("\\u").append(Utility.hex(cp));
          } else {
            quoteOperandBuffer.append(UTF16.valueOf(cp));
          }
        }
      }
    }
    if (inQuote) {
      quoteOperandBuffer.append('\'');
    }

    if (noQuotes) {
      return s; // faster
    }

    return quoteOperandBuffer.toString();
  }

  private String getData(Node root, String strength) {
    StringBuffer data = new StringBuffer();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        String name = node.getNodeName();
        if (name.equals(LDMLConstants.CP)) {
          String hex = LDMLUtilities.getAttributeValue(node, LDMLConstants.HEX);
          if (DEBUG) {
            data.append(" ");
          }
          data.append(getStrengthSymbol(strength));
          if (DEBUG) {
            data.append(" ");
          }
          String cp = UTF16.valueOf(Integer.parseInt(hex, 16));
          data.append(quoteOperand(cp));
        }
      }

      if (node.getNodeType() == Node.TEXT_NODE) {
        String val = node.getNodeValue();
        if (val != null) {
          if (strength.equals(LDMLConstants.PC)
              || strength.equals(LDMLConstants.SC)
              || strength.equals(LDMLConstants.TC)
              || strength.equals(LDMLConstants.QC)
              || strength.equals(LDMLConstants.IC)) {
            data.append(getExpandedRules(val, strength));
          } else {
            if (DEBUG) {
              data.append(" ");
            }
            data.append(getStrengthSymbol(strength));
            if (DEBUG) {
              data.append(" ");
            }
            data.append(quoteOperand(val));
          }
        }
      }
    }

    return data.toString();
  }

  private String getStrengthSymbol(String name) {
    if (name.equals(LDMLConstants.PC) || name.equals(LDMLConstants.P)) {
      return "<";
    } else if (name.equals(LDMLConstants.SC)||name.equals(LDMLConstants.S)) {
      return "<<";
    } else if (name.equals(LDMLConstants.TC)|| name.equals(LDMLConstants.T)) {
      return "<<<";
    } else if (name.equals(LDMLConstants.QC) || name.equals(LDMLConstants.Q)) {
      return "<<<<";
    } else if (name.equals(LDMLConstants.IC) || name.equals(LDMLConstants.I)) {
      return "=";
    } else {
      System.err.println("Encountered strength: " + name);
      System.exit(-1);
    }
    return null;
  }

  private String getStrength(String name) {
    if (name.equals(LDMLConstants.PRIMARY)) {
      return "1";
    } else if (name.equals(LDMLConstants.SECONDARY)) {
      return "2";
    } else if (name.equals(LDMLConstants.TERTIARY)) {
      return "3";
    } else if (name.equals(LDMLConstants.QUARTERNARY)) {
      return "4";
    } else if (name.equals(LDMLConstants.IDENTICAL)) {
      return "5";
    } else {
      System.err.println("Encountered strength: " + name);
      System.exit(-1);
    }
    return null;
  }

  private StringBuffer parseReset(Node root) {
    /* variableTop   at      & x= [last variable] <reset>x</reset><i><last_variable/></i>
     * after & x < [last variable] <reset>x</reset><p><last_variable/></p>
     * before & [before 1] x< [last variable] <reset before="primary">x</reset>
     * <p><last_variable/></p>
     */
    /*
     * & [first tertiary ignorable] << \u00e1 <reset><first_tertiary_ignorable/></reset><s>?</s>
     */
    StringBuffer ret = new StringBuffer();

    if (DEBUG) {
      ret.append(" ");
    }
    ret.append("&");
    if (DEBUG) {
      ret.append(" ");
    }

    String val = LDMLUtilities.getAttributeValue(root, LDMLConstants.BEFORE);
    if (val != null) {
      if (DEBUG) {
        ret.append(" ");
      }
      ret.append("[before ");
      ret.append(getStrength(val));
      ret.append("]");
    }

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      short type = node.getNodeType();
      if (type == Node.ELEMENT_NODE) {
        String key = node.getNodeName();
        if (DEBUG) {
          ret.append(" ");
        }
        ret.append(collationMap.get(key));
      }
      if (type == Node.TEXT_NODE) {
        ret.append(quoteOperand(node.getNodeValue()));
      }
    }

    return ret;
  }

  private StringBuffer getExpandedRules(String data, String name) {
    UCharacterIterator iter = UCharacterIterator.getInstance(data);
    StringBuffer ret = new StringBuffer();
    String strengthSymbol = getStrengthSymbol(name);
    int ch;
    while((ch = iter.nextCodePoint())!= UCharacterIterator.DONE) {
      if (DEBUG) {
        ret.append(" ");
      }
      ret.append(strengthSymbol);
      if (DEBUG) {
        ret.append(" ");
      }
      ret.append(quoteOperand(UTF16.valueOf(ch)));
    }

    return ret;
  }

  private StringBuffer parseExtension(Node root) {
    /*
     * strength context string extension
     * <strength>  <context> | <string> / <extension>
     * < a | [last variable]      <x><context>a</context><p><last_variable/></p></x>
     * < [last variable]    / a   <x><p><last_variable/></p><extend>a</extend></x>
     * << k / h                   <x><s>k</s> <extend>h</extend></x>
     * << d | a                   <x><context>d</context><s>a</s></x>
     * =  e | a                   <x><context>e</context><i>a</i></x>
     * =  f | a                   <x><context>f</context><i>a</i></x>
     */
    StringBuffer rules = new StringBuffer();
    Node contextNode = null;
    Node extendNode = null;
    Node strengthNode = null;
    String strength = null;
    String string = null;
    String context = null;
    String extend = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(LDMLConstants.CONTEXT)) {
        contextNode = node;
      } else if (name.equals(LDMLConstants.P)
                 || name.equals(LDMLConstants.S)
                 || name.equals(LDMLConstants.T)
                 || name.equals(LDMLConstants.I)) {
        strengthNode = node;
      } else if (name.equals(LDMLConstants.EXTEND)) {
        extendNode = node;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
    }

    if (contextNode != null) {
      context = LDMLUtilities.getNodeValue(contextNode);
    }

    if (strengthNode != null) {
      Node lastVariable = LDMLUtilities.getNode(
          strengthNode, LDMLConstants.LAST_VARIABLE , null, null);
      if (lastVariable != null) {
        string = (String) collationMap.get(lastVariable.getNodeName());
      } else {
        strength = getStrengthSymbol(strengthNode.getNodeName());
        string = LDMLUtilities.getNodeValue(strengthNode);
      }
    }

    if (extendNode != null) {
      extend = LDMLUtilities.getNodeValue(extendNode);
    }

    if (DEBUG) {
      rules.append(" ");
    }
    rules.append(strength);
    if (DEBUG) {
      rules.append(" ");
    }

    if (context != null) {
      rules.append(quoteOperand(context));
      if (DEBUG) {
        rules.append(" ");
      }
      rules.append("|");
      if (DEBUG) {
        rules.append(" ");
      }
    }
    rules.append(string);

    if (extend != null) {
      if (DEBUG) {
        rules.append(" ");
      }
      rules.append("/");
      if (DEBUG) {
        rules.append(" ");
      }
      rules.append(quoteOperand(extend));
    }

    return rules;
  }

  private static final String ICU_BRKITR_DATA = "icu:breakIteratorData";
  private static final String ICU_DICTIONARIES = "icu:dictionaries";
  private static final String ICU_BOUNDARIES = "icu:boundaries";
  private static final String ICU_GRAPHEME = "icu:grapheme";
  private static final String ICU_WORD = "icu:word";
  private static final String ICU_SENTENCE = "icu:sentence";
  private static final String ICU_LINE = "icu:line";
  private static final String ICU_XGC          = "icu:xgc";
  private static final String ICU_TITLE = "icu:title";
  private static final String ICU_DICTIONARY = "icu:dictionary";
  //private static final String ICU_CLASS        = "icu:class";
  //private static final String ICU_IMPORT       = "icu:import";
  //private static final String ICU_APPEND       = "icu:append";
  private static final String ICU_UCARULES = "icu:UCARules";
  private static final String ICU_UCA_RULES = "icu:uca_rules";
  private static final String ICU_DEPENDS = "icu:depends";
  private static final String ICU_DEPENDENCY = "icu:dependency";

  private ICUResourceWriter.Resource parseBoundaries(Node root, StringBuffer xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    int savedLength = xpath.length();
    getXPath(root,xpath);
    ICUResourceWriter.Resource current = null;
    String name = root.getNodeName();
    table.name = name.substring(name.indexOf(':') + 1, name.length());

    // we don't care if special elements are marked draft or not!

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(ICU_GRAPHEME)
          || name.equals(ICU_WORD)
          || name.equals(ICU_LINE)
          || name.equals(ICU_SENTENCE)
          || name.equals(ICU_TITLE)
          || name.equals(ICU_XGC)) {
        ICUResourceWriter.ResourceProcess str = new ICUResourceWriter.ResourceProcess();
        str.ext =  ICUResourceWriter.DEPENDENCY;
        str.name = name.substring(name.indexOf(':') + 1, name.length());
        str.val = LDMLUtilities.getAttributeValue(node, ICU_DEPENDENCY);
        if (str.val != null) {
          res = str;
        }
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        }else{
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }

      xpath.delete(savedLength,xpath.length());
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseDictionaries(Node root, StringBuffer xpath) {
    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    int savedLength = xpath.length();
    getXPath(root,xpath);
    ICUResourceWriter.Resource current = null;
    String name = root.getNodeName();
    table.name = name.substring(name.indexOf(':') + 1, name.length());
    // we don't care if special elements are marked draft or not

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(ICU_DICTIONARY)) {
        ICUResourceWriter.ResourceProcess str = new ICUResourceWriter.ResourceProcess();
        str.ext =  ICUResourceWriter.DEPENDENCY;
        str.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        str.val = LDMLUtilities.getAttributeValue(node, ICU_DEPENDENCY);
        if (str.val != null) {
          res = str;
        }
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }

      xpath.delete(savedLength,xpath.length());
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  /*
    private ICUResourceWriter.Resource parseMetaData(Node doc) {
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        table.name = "DeprecatedList";
        String xpath = "//supplementalData/metadata/alias";
        Node alias = LDMLUtilities.getNode(doc, xpath);
        if (alias == null) {
            printWarning("","Could not find : " + xpath + " in supplementalData");
        }
        for(Node node = alias.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType()!= Node.ELEMENT_NODE) {
                continue;
            }

//                DeprecatedList{
//                    in{ id{} }
//                    iw{ he{} }
//                    ji{ yi{} }
//                    BU{ MM{} }
//                    DY{ BJ{} }
//                    HV{ BF{} }
//                    NH{ VU{} }
//                    RH{ ZW{} }
//                    TP{ TL{} }
//                    YU{ CS{} }
//                }

            String name = node.getNodeName();
            ICUResourceWriter.Resource res = null;
            if (name.equals(LDMLConstants.LANGUAGE_ALIAS)
                || name.equals(LDMLConstants.TERRITORY_ALIAS)) {
                String deprecated = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
                String current = LDMLUtilities.getAttributeValue(node, LDMLConstants.REPLACEMENT);

                if (deprecated.indexOf('_')>= 0) {
                    //TODO: generate a locale that is aliased to the replacement
                    continue;
                }
                //TODO: Fix it after discussion with the team
            }else{

            }
        }
        if (table.first != null) {
            return table;
        }
        return null;
    }

    private ICUResourceWriter.Resource parseLocaleScript(Node node) {
        String language = ULocale.getLanguage(locName);
        String territory = ULocale.getCountry(locName);
        String scriptCode = ULocale.getScript(locName);

        String xpath = "//supplementalData/languageData/language[@type='" + language + "']";
        Node scriptNode = LDMLUtilities.getNode(node, xpath);
        if (scriptNode != null) {
            String scripts = LDMLUtilities.getAttributeValue(scriptNode, LDMLConstants.SCRIPTS);
            // verify that the territory of this locale is one of the territories
            if (territory.length()>0) {
                String territories = LDMLUtilities.getAttributeValue(
                    scriptNode, LDMLConstants.TERRITORIES);
                if (territories != null) {
                    String[] list = territories.split("\\s");
                    boolean exists = false;
                    for(int i = 0; i >list.length; i ++) {
                        if (list[i].equals(territory)) {
                            exists = true;
                        }
                    }
                    if (exists == false) {
                        System.err.println(
                            "WARNING: Script info does not exist for locale: " + locName);
                    }
                }
                return null;
            }else if (scriptCode.length()>0) {
                ICUResourceWriter.ResourceArray arr = new  ICUResourceWriter.ResourceArray();
                arr.name = LOCALE_SCRIPT;
                ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
                str.val = scriptCode;
                arr.first = str;
                return arr;
            }else{
                if (scripts != null) {
                    String[] list = scripts.split("\\s");
                    if (list.length >0) {
                        ICUResourceWriter.ResourceArray arr = new ICUResourceWriter.ResourceArray();
                        arr.name = LOCALE_SCRIPT;
                        ICUResourceWriter.Resource current = null;
                        for(int i = 0; i <list.length; i++) {
                            ICUResourceWriter.ResourceString str =
                                new ICUResourceWriter.ResourceString();
                            str.val = list[i];
                            if (current == null) {
                                arr.first = current = str;
                            }else{
                                current.next = str;
                                current = current.next;
                            }
                        }
                        return arr;
                    }
                }
            }
            System.err.println("Could not find script information for locale: " + locName);
        }
        return null;
    }
   */

  private ICUResourceWriter.Resource parseSpecialElements(InputLocale loc, String xpath) {
    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource first = null;
    String origXpath = xpath;

    for (Iterator<String> iter = loc.file.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = loc.getXpathName(xpath);

      // we don't care if special elements are marked draft or not

      ICUResourceWriter.Resource res = null;
      if (name.equals(ICU_SCRIPT)) {
        if (!loc.beenHere(ICU_SCRIPT)) {
          res = parseArray(loc, "//ldml/characters/special/icu:scripts");
          res.name = LOCALE_SCRIPT;
        }
      } else if (name.equals(ICU_UCARULES)) {
        ICUResourceWriter.ResourceProcess process = new ICUResourceWriter.ResourceProcess();
        process.name = "UCARules";
        process.ext = "uca_rules";
        process.val = loc.findAttributeValue(xpath,ICU_UCA_RULES);
        res = process;
      } else if (name.equals(ICU_DEPENDS)) {
        ICUResourceWriter.ResourceProcess process = new ICUResourceWriter.ResourceProcess();
        process.name = "depends";
        process.ext = "dependency";
        process.val = loc.findAttributeValue(xpath,ICU_DEPENDENCY);
        res = process;
      } else if (xpath.startsWith("//ldml/special/" + ICU_BRKITR_DATA)) {
        res = parseBrkItrData(loc, "//ldml/special/" + ICU_BRKITR_DATA);
      } else if (name.equals(ICU_IS_LEAP_MONTH)
                 || name.equals(ICU_LEAP_SYMBOL)
                 || name.equals(ICU_NON_LEAP_SYMBOL)) {
        if (!loc.beenHere(origXpath + ICU_IS_LEAP_MONTH)) {
          res = parseLeapMonth(loc, origXpath);
        }
      } else if (name.equals(LDMLConstants.SPECIAL)) {
        // just continue, already handled
      } else {
        System.err.println("Encountered unknown <" + xpath + "> special subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
    }

    return first;
  }

  private ICUResourceWriter.Resource parseBrkItrData(InputLocale loc, String xpath) {
    // "//ldml/special/"
    if (loc.beenHere(ICU_BRKITR_DATA)) {
      return null;
    }

    Node root = loc.getTopNode(LDMLConstants.SPECIAL);
    StringBuffer xpathBuffer = new StringBuffer();
    getXPath(root,xpathBuffer);
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(ICU_BRKITR_DATA)) {
        return parseBrkItrData(node, xpathBuffer);
      }
    }
    throw new InternalError("Could not find node for " + ICU_BRKITR_DATA);
  }

  private ICUResourceWriter.Resource parseBrkItrData(Node root, StringBuffer xpath) {
    ICUResourceWriter.Resource current = null, first = null;
    int savedLength = xpath.length();
    getXPath(root, xpath);

    // we don't care if special elements are marked draft or not!
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(ICU_BOUNDARIES)) {
        res = parseBoundaries(node, xpath);
      } else if (name.equals(ICU_DICTIONARIES)) {
        res = parseDictionaries(node, xpath);
      } else {
        System.err.println(
            "Encountered @ " + xpath + "  unknown <" + root.getNodeName() + "> subelement: "
            + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          first = res;
          current = findLast(res);
        } else {
          current.next = res;
          current = findLast(res);
        }
        res = null;
      }

      xpath.delete(savedLength, xpath.length());
    }

    return first;
  }

  private ICUResourceWriter.Resource parseDefaultNumberingSystem(InputLocale loc, String xpath) {
    ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
    str.name = LDMLConstants.DEFAULT_NUMBERING_SYSTEM;
    str.val = loc.file.getStringValue(xpath);
    if (str.val != null) {
      return str;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseRBNF(InputLocale loc, String xpath) {
    char LARROW = 0x2190;
    char RARROW = 0x2192;

    ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
    table.name = "RBNFRules";

    ICUResourceWriter.Resource current = null;
    ICUResourceWriter.Resource res = null;
    ICUResourceWriter.ResourceArray ruleset = new ICUResourceWriter.ResourceArray();

    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    String currentRulesetGrouping = "";
    String currentRulesetType = "";

    for (Iterator<String> iter = loc.file.iterator(xpath, CLDRFile.ldmlComparator);
         iter.hasNext();) {
      String aPath = iter.next();
      String fullPath = loc.file.getFullXPath(aPath);
      String name = loc.getXpathName(aPath);
      if (name.equals(LDMLConstants.RBNFRULE)) {
        XPathParts xpp = new XPathParts();
        xpp.set(fullPath);

        String rulesetGrouping = xpp.findAttributeValue(
            LDMLConstants.RULESETGROUPING, LDMLConstants.TYPE);
        String rulesetType = xpp.findAttributeValue(LDMLConstants.RULESET, LDMLConstants.TYPE);
        String rulesetAccess = xpp.findAttributeValue(LDMLConstants.RULESET, LDMLConstants.ACCESS);
        String ruleValue = xpp.findAttributeValue(LDMLConstants.RBNFRULE, LDMLConstants.VALUE);
        String ruleRadix = xpp.findAttributeValue(LDMLConstants.RBNFRULE, LDMLConstants.RADIX);
        String ruleDecExp = xpp.findAttributeValue(LDMLConstants.RBNFRULE, LDMLConstants.DECEXP);

        if (rulesetGrouping != null && !rulesetGrouping.equals(currentRulesetGrouping)) {
          if (!currentRulesetGrouping.equals("")) {
            res = ruleset;
            table.appendContents(res);
            res = null;
          }

          ruleset = new ICUResourceWriter.ResourceArray();
          ruleset.name = rulesetGrouping;
          currentRulesetGrouping = rulesetGrouping;
          currentRulesetType = "";
        }

        if (!rulesetType.equals(currentRulesetType)) {
          ICUResourceWriter.ResourceString rsname = new ICUResourceWriter.ResourceString();
          String rsNamePrefix = "%";
          if (rulesetAccess != null && rulesetAccess.equals("private")) {
            rsNamePrefix = "%%";
          }
          rsname.val = rsNamePrefix + rulesetType + ":";
          ruleset.appendContents(rsname);
          currentRulesetType = rulesetType;
        }

        String radixString = "";
        if (ruleRadix != null) {
          radixString = "/" + ruleRadix;
        }

        String decExpString = "";

        if (ruleDecExp != null) {
          int decExp = Integer.valueOf(ruleDecExp).intValue();
          while (decExp > 0) {
            decExpString = decExpString.concat(">");
            decExp--;
          }
        }

        ICUResourceWriter.ResourceString rs = new ICUResourceWriter.ResourceString();
        if (rulesetType.equals(LDMLConstants.LENIENT_PARSE)) {
          rs.val = Utility.escape(
              loc.file.getStringValue(aPath).replace(LARROW, '<').replace(RARROW, '>'));
        } else {
          rs.val = ruleValue + radixString + decExpString + ": " + Utility.escape(
              loc.file.getStringValue(aPath).replace(LARROW, '<').replace(RARROW, '>'));
        }
        ruleset.appendContents(rs);
      } else {
        System.err.println("Unknown element found: " + xpath + " / " + name);
        System.exit(-1);
      }
    }

    if (ruleset.first != null) {
      table.appendContents(ruleset);
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseBCP47MappingData(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable bcp47KeywordMappings = new ICUResourceWriter.ResourceTable();
    bcp47KeywordMappings.name = LDMLConstants.BCP47_KEYWORD_MAPPINGS;
    ICUResourceWriter.Resource current = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.MAP_KEYS)) {
        res = parseMapKeys(node, xpath);
      } else if (name.equals(LDMLConstants.MAP_TYPES)) {
        res = parseMapTypes(node, xpath);
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          bcp47KeywordMappings.first = res;
        }else{
          current.next = res;
        }
        current = res;
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (bcp47KeywordMappings.first != null) {
      return bcp47KeywordMappings;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseMapKeys(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable mapKeys = new ICUResourceWriter.ResourceTable();
    mapKeys.name = "key";
    ICUResourceWriter.Resource current = null;

    for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;
      if (name.equals(LDMLConstants.KEY_MAP)) {
        ICUResourceWriter.ResourceString keyMap = new ICUResourceWriter.ResourceString();
        keyMap.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE)
            .toLowerCase(Locale.ENGLISH);
        keyMap.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.BCP47)
            .toLowerCase(Locale.ENGLISH);
        res = keyMap;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          mapKeys.first = res;
        }else{
          current.next = res;
        }
        current = res;
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());
    if (mapKeys.first != null) {
      return mapKeys;
    }

    return null;
  }

  private ICUResourceWriter.Resource parseMapTypes(Node root, StringBuffer xpath) {
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    ICUResourceWriter.ResourceTable mapTypes = new ICUResourceWriter.ResourceTable();
    String ldmlKey = LDMLUtilities.getAttributeValue(root, LDMLConstants.TYPE)
        .toLowerCase(Locale.ENGLISH);
    mapTypes.name = ldmlKey;
    boolean isTimeZone = ldmlKey.equals("timezone");
    ICUResourceWriter.Resource current = null;

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      ICUResourceWriter.Resource res = null;

      if (name.equals(LDMLConstants.TYPE_MAP)) {
        ICUResourceWriter.ResourceString typeMap = new ICUResourceWriter.ResourceString();
        if (isTimeZone) {
          typeMap.name = "\""
              + LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE).replaceAll("/", ":")
              .toLowerCase(Locale.ENGLISH)
              + "\"";
        } else {
          typeMap.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE)
              .toLowerCase(Locale.ENGLISH);
        }
        typeMap.val = LDMLUtilities.getAttributeValue(node, LDMLConstants.BCP47)
            .toLowerCase(Locale.ENGLISH);
        res = typeMap;
      } else {
        System.err.println("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          mapTypes.first = res;
        }else{
          current.next = res;
        }
        current = res;
      }

      xpath.delete(oldLength, xpath.length());
    }

    xpath.delete(savedLength, xpath.length());

    if (mapTypes.first != null) {
      return mapTypes;
    }

    return null;
  }

  /*
   * coment this method out since the specials document is now merged with
   * fully resolved document and is not parsed separately
   *
   * private ICUResourceWriter.Resource parseSpecialsDocucment(Node root) {
   *
   * ICUResourceWriter.Resource current = null, first = null; StringBuffer
   * xpath = new StringBuffer(); xpath.append("//ldml"); int savedLength =
   * xpath.length(); Node ldml = null; for(ldml = root.getFirstChild();
   * ldml != null; ldml = ldml.getNextSibling()) {
   * if (ldml.getNodeType()!= Node.ELEMENT_NODE) { continue; } String name =
   * ldml.getNodeName(); if (name.equals(LDMLConstants.LDML)) {
   * if (LDMLUtilities.isLocaleDraft(ldml) && writeDraft == false) {
   * System.err.println("WARNING: The LDML file " + sourceDir + "/" + locName + ".xml
   * is marked draft ! Not producing ICU file. "); System.exit(-1); } break; } }
   *
   * if (ldml == null) { throw new RuntimeException("ERROR: no <ldml > node
   * found in parseBundle()"); }
   *
   * for(Node node = ldml.getFirstChild(); node != null;
   * node = node.getNextSibling()) { if (node.getNodeType()!= Node.ELEMENT_NODE) {
   * continue; } String name = node.getNodeName(); ICUResourceWriter.Resource
   * res = null; if (name.equals(LDMLConstants.IDENTITY)) { //TODO: add code to
   * check the identity of specials doc is equal to identity of // main
   * document
   *
   * continue; }else if (name.equals(LDMLConstants.SPECIAL)) { res =
   * parseSpecialElements(node,xpath); }else
   * if (name.equals(LDMLConstants.CHARACTERS)) { res = parseCharacters(node,
   * xpath); }else if (name.equals(LDMLConstants.COLLATIONS)) { //collations are
   * resolved in parseCollation continue; }else
   * if (name.equals(LDMLConstants.DATES)) { // will be handled by parseCalendar
   * res = parseDates(node, xpath); }else if (name.indexOf("icu:")>-1||
   * name.indexOf("openOffice:")>-1) { //TODO: these are specials .. ignore for
   * now ... figure out // what to do later }else{
   * System.err.println("Encountered unknown <" + root.getNodeName() + ">
   * subelement: " + name); System.exit(-1); } if (res != null) { if (current ==
   * null) { first = res; current = findLast(res); }else{ current.next = res;
   * current = findLast(res); } res = null; }
   * xpath.delete(savedLength,xpath.length()); } return first; }
   */
  private void writeResource(ICUResourceWriter.Resource set, String sourceFileName) {
    String outputFileName = null;
    outputFileName = destDir + "/" + set.name + ".txt";
    try {
      System.out.println("Writing " + outputFileName);
      FileOutputStream file = new FileOutputStream(outputFileName);
      BufferedOutputStream writer = new BufferedOutputStream(file);
      printInfo("Writing ICU: " + outputFileName);
      // TODO: fix me
      writeHeader(writer, sourceFileName);

      ICUResourceWriter.Resource current = set;
      while (current != null) {
        current.sort();
        current = current.next;
      }

      // Now start writing the resource.
      // ICUResourceWriter.Resourcek
      current = set;
      while (current != null) {
        current.write(writer, 0, false);
        current = current.next;
      }
      writer.flush();
      writer.close();
    } catch (ICUResourceWriter.Resource.MalformedResourceError mre) {
      String where = set.findResourcePath(mre.offendingResource);
      System.err.println(
          sourceFileName + ": ERROR (writing resource " + where + ") :" + mre.toString());
      mre.printStackTrace();
      if (new File(outputFileName).delete()) {
        System.err.println("## Deleted partial file: " + outputFileName);
      }
      System.exit(1);
      return; // NOTREACHED
    } catch (Exception ie) {
      System.err.println(sourceFileName + ": ERROR (writing resource) :" + ie.toString());
      ie.printStackTrace();
      if (new File(outputFileName).delete()) {
        System.err.println("## Deleted partial file: " + outputFileName);
      }
      System.exit(1);
      return; // NOTREACHED
    }
  }

  /*
    private boolean isType(Node node, String type) {
        NamedNodeMap attributes = node.getAttributes();
        Node attr = attributes.getNamedItem(LDMLConstants.TYPE);
        if (attr != null && attr.getNodeValue().equals(type)) {
            return true;
        }
        return false;
    }
   */

  private void writeLine(OutputStream writer, String line) {
    try {
      byte[] bytes = line.getBytes(CHARSET);
      writer.write(bytes, 0, bytes.length);
    } catch (Exception e) {
      System.err.println(e);
      System.exit(1);
    }
  }

  private void writeHeader(OutputStream writer, String fileName) {
    writeBOM(writer);
    Calendar c = Calendar.getInstance();
    StringBuffer buffer = new StringBuffer();
    buffer.append("// ***************************************************************************")
        .append(LINESEP)
        .append("// *")
        .append(LINESEP)
        .append("// * Copyright (C) ")
        .append(c.get(Calendar.YEAR))
        .append(" International Business Machines")
        .append(LINESEP)
        .append("// * Corporation and others.  All Rights Reserved.")
        .append(LINESEP)
        .append("// * Tool: com.ibm.icu.dev.tool.cldr.LDML2ICUConverter.java")
        .append(LINESEP);
    // buffer.append("// * Date & Time: ")
    // .append(c.get(Calendar.YEAR))
    // .append("/")
    // .append(c.get(Calendar.MONTH) + 1)
    // .append("/")
    // .append(c.get(Calendar.DAY_OF_MONTH))
    // .append(" ")
    // .append(c.get(Calendar.HOUR_OF_DAY))
    // .append(COLON)
    // .append(c.get(Calendar.MINUTE))
    // .append(LINESEP);
    //         String ver = LDMLUtilities.getCVSVersion(fileName);
    //         if (ver == null) {
    //             ver = "";
    //         } else {
    //             ver = " v" + ver;
    //         }

    String tempdir = fileName.replace('\\','/');
    //System.out.println(tempdir);
    int index = tempdir.indexOf("/common");
    if (index > -1) {
      tempdir = "<path>" + tempdir.substring(index, tempdir.length());
    } else {
      index = tempdir.indexOf("/xml");
      if (index > -1) {
        tempdir = "<path>" + tempdir.substring(index, tempdir.length());
      } else {
        tempdir = "<path>/" + tempdir;
      }
    }
    buffer.append("// * Source File:" + tempdir)
        .append(LINESEP)
        .append("// *")
        .append(LINESEP)
        .append("// ***************************************************************************")
        .append(LINESEP);
    writeLine(writer, buffer.toString());
  }

  private void writeBOM(OutputStream buffer) {
    try {
      byte[] bytes = BOM.getBytes(CHARSET);
      buffer.write(bytes, 0, bytes.length);
    } catch(Exception e) {
      System.err.println(e);
      System.exit(1);
    }
  }

  private void writeDeprecated() {
    String myTreeName = null;
    File depF = null;
    File destD = new File(destDir);
    final File[] destFiles = destD.listFiles();
    if (writeDeprecated == true) {
      depF = new File(options[WRITE_DEPRECATED].value);
      if (!depF.isDirectory()) {
        printError("LDML2ICUConverter" ,  options[WRITE_DEPRECATED].value + " isn't a directory.");
        usage();
        return; // NOTREACHED
      }
      myTreeName = depF.getName();
    }

    // parse for draft status?
    boolean parseDraft = !writeDraft;

    boolean parseSubLocale = sourceDir.indexOf("collation")> -1;

    // parse a bunch of locales?
    boolean parseThem = (parseDraft||parseSubLocale);

    // ex: "ji" -> "yi"
    TreeMap fromToMap = new TreeMap();

    // ex:  "th_TH_TRADITIONAL" -> "@some xpath.."
    TreeMap fromXpathMap = new TreeMap();

    // ex:  "mt.xml" -> File .  Ordinary XML source files
    Map fromFiles = new TreeMap();

    // ex:  "en_US.xml" -> File .  empty files generated by validSubLocales
    Map emptyFromFiles = new TreeMap();

    // ex:  th_TH_TRADITIONAL.xml -> File  Files generated directly from the alias list
    // (no XML actually exists).
    Map generatedAliasFiles = new TreeMap();

    // ex: zh_MO.xml -> File  Files which actually exist in LDML and contain aliases
    Map aliasFromFiles = new TreeMap();

    // en -> "en_US en_GB ..."
    TreeMap validSubMap = new TreeMap();

    // for in -> id where id is a synthetic alias
    TreeMap maybeValidAlias = new TreeMap();

    // 1. get the list of input XML files
    FileFilter myFilter = new FileFilter() {
      public boolean accept(File f) {
        String n = f.getName();
        return !f.isDirectory()
            && n.endsWith(".xml")
            && !n.startsWith("supplementalData") // not a locale
            /* &&!n.startsWith("root") */
            && isInDest(n); // root is implied, will be included elsewhere.
      }

      public boolean isInDest(String n) {
        String name = n.substring(0, n.indexOf('.') + 1);
        for (int i = 0; i < destFiles.length; i++) {
          String dest = destFiles[i].getName();
          if (dest.indexOf(name)== 0) {
            return true;
          }
        }

        return false;
      }
    };

    // File destFiles[] =
    File inFiles[] = depF.listFiles(myFilter);

    int nrInFiles = inFiles.length;
    if (parseThem) {
      System.out.println(
          "Parsing: " + nrInFiles + " LDML locale files to check " + (parseDraft ? "draft, " : "")
          + (parseSubLocale ? "valid-sub-locales, " : ""));
    }

    for (int i = 0; i < nrInFiles; i++) {
      if (i > 0 && (i % 60 == 0)) {
        System.out.println(" " + i);
        System.out.flush();
      }
      boolean thisOK = true;
      String localeName = inFiles[i].getName();
      localeName = localeName.substring(0, localeName.indexOf('.'));
      if (parseThem) {
        // System.out.print(" " + inFiles[i].getName() + ":");
        try {
          Document doc2 = LDMLUtilities.parse(inFiles[i].toString(), false);
          // TODO: figure out if this is really required
          if (parseDraft && LDMLUtilities.isLocaleDraft(doc2)) {
            thisOK = false;
          }
          if (thisOK && parseSubLocale) {
            Node collations = LDMLUtilities.getNode(doc2, "//ldml/collations");
            if (collations != null) {
              String vsl = LDMLUtilities.getAttributeValue(collations, "validSubLocales");
              if (vsl != null && vsl.length() > 0) {
                validSubMap.put(localeName, vsl);
                printInfo(localeName + " <- " + vsl);
              }
            }
          }
        } catch (Throwable t) {
          System.err.println("While parsing " + inFiles[i].toString() + " - ");
          System.err.println(t.toString());
          t.printStackTrace(System.err);
          System.exit(-1); // TODO: should be full 'parser error'stuff.
        }
      }

      if (!localeName.equals("root")) {
        // System.out.println("FN put " + inFiles[i].getName());
        if (thisOK) {
          System.out.print("."); // regular file
          fromFiles.put(inFiles[i].getName(), inFiles[i]); // add to hash
        } else {
          if (isDraftStatusOverridable(localeName)) {
            fromFiles.put(inFiles[i].getName(), inFiles[i]); // add to hash
            System.out.print("o"); // override
            // System.out.print("[o:" + localeName + "]");
          } else {
            System.out.print("d"); // draft
            // System.out.print("[d:" + localeName + "]");
          }
        }
      } else {
        System.out.print("_");
      }
    }

    if (parseThem == true) {
      // end the debugging line
      System.out.println("");
    }
    // End of parsing all XML files.

    if (emptyLocaleList != null && emptyLocaleList.size() > 0) {
      for (int i = 0; i < emptyLocaleList.size(); i++) {
        String loc = (String) emptyLocaleList.get(i);
        writeSimpleLocale(
            loc + ".txt", loc, null, null, "empty locale file for dependency checking");
        // we do not want these files to show up in installed locales list!
        generatedAliasFiles.put(loc + ".xml", new File(depF, loc + ".xml"));
      }
    }

    // interpret the deprecated locales list
    if (aliasMap != null && aliasMap.size() > 0) {
      for (Iterator i = aliasMap.keySet().iterator(); i.hasNext();) {
        String from = (String) i.next();
        Alias value = (Alias) aliasMap.get(from);
        String to = value.to;
        String xpath = value.xpath;
        if (to.indexOf('@') != -1 && xpath == null) {
          System.err.println(
              "Malformed alias - '@' but no xpath: from=\"" + from + "\" to=\"" + to + "\"");
          System.exit(-1);
          return; // NOTREACHED
        }

        if (from == null || to == null) {
          System.err.println(
              "Malformed alias - no 'from' or no 'to':from=\"" + from + "\" to=\"" + to + "\"");
          System.exit(-1);
          return; // NOTREACHED
        }

        String toFileName = to;
        if (xpath != null) {
          toFileName = to.substring(0, to.indexOf('@'));
        }
        if (fromFiles.containsKey(from + ".xml")) {
          throw new IllegalArgumentException(
              "Can't be both a synthetic alias locale and a real xml file - "
              + "consider using <aliasLocale locale=\"" + from + "\"/> instead. ");
        }
        ULocale fromLocale = new ULocale(from);
        if (!fromFiles.containsKey(toFileName + ".xml")) {
          maybeValidAlias.put(toFileName, from);
          // System.err.println("WARNING: Alias from \"" + from + "\"
          // not generated, because it would point to a nonexistent
          // LDML file " + toFileName + ".xml");
          // writeSimpleLocale(from + ".txt", fromLocale, new
          // ULocale(to), xpath,null);
        } else {
          // System.out.println("Had file " + toFileName + ".xml");
          generatedAliasFiles.put(from, new File(depF, from + ".xml"));
          fromToMap.put(fromLocale.toString(), to);
          if (xpath != null) {
            fromXpathMap.put(fromLocale.toString(), xpath);
          }

          // write an individual file
          writeSimpleLocale(from + ".txt", fromLocale, new ULocale(to), xpath, null);
        }
      }
    }

    if (aliasLocaleList != null && aliasLocaleList.size() > 0) {
      for (int i = 0; i < aliasLocaleList.size(); i++) {
        String source = (String) aliasLocaleList.get(i);
        if (!fromFiles.containsKey(source + ".xml")) {
          System.err.println(
              "WARNING: Alias file " + source
              + ".xml named in deprecates list but not present. Ignoring alias entry.");
        } else {
          aliasFromFiles.put(source + ".xml", new File(depF, source + ".xml"));
          fromFiles.remove(source + ".xml");
        }
      }
    }

    // Post process: calculate any 'valid sub locales' (empty locales
    // generated due to validSubLocales attribute)
    if (!validSubMap.isEmpty() && sourceDir.indexOf("collation") > -1) {
      printInfo("Writing valid sub locs for : " + validSubMap.toString());

      for (Iterator e = validSubMap.keySet().iterator(); e.hasNext();) {
        String actualLocale = (String) e.next();
        String list = (String) validSubMap.get(actualLocale);
        String validSubs[] = list.split(" ");
        // printInfo(actualLocale + " .. ");
        for (int i = 0; i < validSubs.length; i++) {
          String aSub = validSubs[i];
          String testSub;
          // printInfo(" " + aSub);

          for (testSub = aSub;
               testSub != null && !testSub.equals("root") && !testSub.equals(actualLocale);
               testSub = LDMLUtilities.getParent(testSub)) {

            // printInfo(" trying " + testSub);
            if (fromFiles.containsKey(testSub + ".xml")) {
              printWarning(
                  actualLocale + ".xml",
                  " validSubLocale=" + aSub + " overridden because  " + testSub + ".xml  exists.");
              testSub = null;
              break;
            }

            if (generatedAliasFiles.containsKey(testSub)) {
              printWarning(
                  actualLocale + ".xml",
                  " validSubLocale=" + aSub + " overridden because  an alias locale " + testSub
                  + ".xml  exists.");
              testSub = null;
              break;
            }
          }

          if (testSub != null) {
            emptyFromFiles.put(aSub + ".xml", new File(depF, aSub + ".xml"));
            // ULocale aSubL = new ULocale(aSub);
            if (maybeValidAlias.containsKey(aSub)) {
              String from = (String) maybeValidAlias.get(aSub);
              // writeSimpleLocale(from + ".txt", fromLocale, new
              // ULocale(to), xpath,null);
              writeSimpleLocale(from + ".txt", from, aSub, null, null);
              maybeValidAlias.remove(aSub);
              generatedAliasFiles.put(from, new File(depF, from + ".xml"));
            }
            writeSimpleLocale(
                aSub + ".txt", aSub, null, null, "validSubLocale of \"" + actualLocale + "\"");
          }
        }
      }
    }

    if (!maybeValidAlias.isEmpty()) {
      Set keys = maybeValidAlias.keySet();
      Iterator iter = keys.iterator();
      while (iter.hasNext()) {
        String to = (String) iter.next();
        String from = (String) maybeValidAlias.get(to);
        System.err.println(
            "WARNING: Alias from \"" + from
            + "\" not generated, because it would point to a nonexistent LDML file " + to + ".xml");
      }
    }

    // System.out.println("In Files: " + inFileText);
    String inFileText = fileMapToList(fromFiles);
    String emptyFileText = null;
    if (!emptyFromFiles.isEmpty()) {
      emptyFileText = fileMapToList(emptyFromFiles);
    }
    String aliasFilesList = fileMapToList(aliasFromFiles);
    String generatedAliasList = fileMapToList(generatedAliasFiles);

    // Now- write the actual items (resfiles.mk, etc)
    String[] brkArray = new String[2];
    if (myTreeName.equals("brkitr")) {
      getBrkCtdFilesList(options[WRITE_DEPRECATED].value, brkArray);
    }
    writeResourceMakefile(myTreeName, generatedAliasList, aliasFilesList,
            inFileText, emptyFileText, brkArray[0], brkArray[1]);
    if (writeDeprecated == false) {
      return; // just looking for overrideDraft
    }

    System.out.println("done.");
    // System.err.println("Error: did not find tree " + myTreeName + " in
    // the deprecated alias table.");
    // System.exit(0);
  }

  public String[] getBrkCtdFilesList(String dir, String[] brkArray) {
    // read all xml files in the directory and create ctd file list and brk file list
    FilenameFilter myFilter = new FilenameFilter() {
      public boolean accept(File f, String name) {
        return !f.isFile()
            && name.endsWith(".xml")
            && !name.startsWith("supplementalData"); // not a locale
        // root is implied, will be included elsewhere.
      }
    };

    File directory = new File(dir);
    String[] files = directory.list(myFilter);
    StringBuffer brkList = new StringBuffer();
    StringBuffer ctdList = new StringBuffer();

    // open each file and create the list of files for brk and ctd
    for (int i = 0; i <files.length; i++) {
      Document doc = LDMLUtilities.parse(dir + "/" + files[i], false);
      for(Node node = doc.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }

        String name = node.getNodeName();
        if (name.equals(LDMLConstants.LDML)) {
          node = node.getFirstChild();
          continue;
        }

        if (name.equals(LDMLConstants.IDENTITY)) {
          continue;
        }

        if (name.equals(LDMLConstants.SPECIAL)) {
          node = node.getFirstChild();
          continue;
        }

        if (name.equals(ICU_BRKITR_DATA)) {
          node = node.getFirstChild();
          continue;
        }

        if (name.equals(ICU_BOUNDARIES)) {
          for (Node cn = node.getFirstChild(); cn != null; cn = cn.getNextSibling()) {
            if (cn.getNodeType() != Node.ELEMENT_NODE) {
              continue;
            }
            String cnName = cn.getNodeName();

            if (cnName.equals(ICU_GRAPHEME)
                || cnName.equals(ICU_WORD)
                || cnName.equals(ICU_TITLE)
                || cnName.equals(ICU_SENTENCE)
                || cnName.equals(ICU_XGC)
                || cnName.equals(ICU_LINE)) {

              String val = LDMLUtilities.getAttributeValue(cn,ICU_DEPENDENCY);
              if (val != null) {
                brkList.append(val.substring(0, val.indexOf('.')));
                brkList.append(".txt ");
              }
            } else {
              System.err.println("Encountered unknown <" + name + "> subelement: " + cnName);
              System.exit(-1);
            }
          }
        } else if (name.equals(ICU_DICTIONARIES)) {
          for (Node cn = node.getFirstChild(); cn != null; cn = cn.getNextSibling()) {
            if (cn.getNodeType() != Node.ELEMENT_NODE) {
              continue;
            }
            String cnName = cn.getNodeName();

            if (cnName.equals(ICU_DICTIONARY)) {
              String val = LDMLUtilities.getAttributeValue(cn, ICU_DEPENDENCY);
              if (val != null) {
                ctdList.append(val.substring(0, val.indexOf('.')));
                ctdList.append(".txt ");
              }
            } else {
              System.err.println("Encountered unknown <" + name + "> subelement: " + cnName);
              System.exit(-1);
            }
          }
        } else {
          System.err.println("Encountered unknown <" + doc.getNodeName() + "> subelement: " + name);
          System.exit(-1);
        }
      }
    }

    if (brkList.length() > 0) {
      brkArray[0] = brkList.toString();
    }

    if (ctdList.length() > 0) {
      brkArray[1] = ctdList.toString();
    }

    return brkArray;
  }

  public boolean isDraftStatusOverridable(String locName) {
    if (getLocalesMap() != null && getLocalesMap().size() > 0) {
      String draft = (String) getLocalesMap().get(locName + ".xml");
      if (draft != null && (draft.equals("true") || locName.matches(draft))) {
        return true;
      }
      return false;
    }

    // check to see if the destination file already exists
    // maybe override draft was specified in the run that produced
    // the txt files
    File f = new File(destDir, locName + ".txt");
    return f.exists();
  }

  private static String fileIteratorToList(Iterator files) {
    String out = "";
    int i = 0;
    for(; files.hasNext();) {
      File f = (File) files.next();
      if ((++i % 5) == 0) {
        out = out + "\\" + LINESEP;
      }
      out = out + (i == 0 ? "" : " ") + f.getName().substring(0, f.getName().indexOf('.')) + ".txt";
    }
    return out;
  }

  private static String fileMapToList(Map files) {
    return fileIteratorToList(files.values().iterator());
  }

  private void writeSimpleLocale(
      String fileName, ULocale fromLocale, ULocale toLocale, String xpath, String comment) {

    writeSimpleLocale(
        fileName, fromLocale == null ? "" : fromLocale.toString(),
        toLocale == null ? "" : toLocale.toString(), xpath, comment);
  }

  private void writeSimpleLocale(
      String fileName, String fromLocale, String toLocale, String xpath, String comment) {

    if (xpath != null) {
      // with CLDRFile this is a piece of cake
      CLDRFile fakeFile = CLDRFile.make(fromLocale);
      fakeFile.add(xpath, "");
      fakeFile.freeze();
      // fakeFile.write(new PrintWriter(System.out));
      InputLocale fakeLocale = new InputLocale(fakeFile);

      locName = fromLocale.toString(); // Global!

      // Feed the bundle into our parser..
      ICUResourceWriter.Resource res = parseBundle(fakeLocale);

      res.name = fromLocale.toString();
      if (res != null && ((ICUResourceWriter.ResourceTable) res).first != null) {
        // write out the bundle
        writeResource(res, DEPRECATED_LIST);
      } else {
        // parse error?
        System.err.println(
            "Failed to write out alias bundle " + fromLocale.toString() + " from " + xpath
            + " - XML list follows:");
        fakeFile.write(new PrintWriter(System.out));
      }
    } else {
      // no xpath - simple locale-level alias.
      String outputFileName = destDir + "/" + fileName;
      ICUResourceWriter.Resource set = null;
      try {
        ICUResourceWriter.ResourceTable table = new ICUResourceWriter.ResourceTable();
        table.name = fromLocale.toString();
        if (toLocale != null && xpath == null) {
          ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
          str.name = "\"%%ALIAS\"";
          str.val = toLocale.toString();
          table.first = str;
        } else {
          ICUResourceWriter.ResourceString str = new ICUResourceWriter.ResourceString();
          str.name = "___";
          str.val = "";
          str.comment = "so genrb doesn't issue warnings";
          table.first = str;
        }
        set = table;
        if (comment != null) {
          set.comment = comment;
        }
      } catch (Throwable e) {
        printError(
            "", "building synthetic locale tree for " + outputFileName + ": " + e.toString());
        e.printStackTrace();
        System.exit(1);
      }

      try {
        String info;
        if (toLocale != null) {
          info = "(alias to " + toLocale.toString() + ")";
        } else {
          info = comment;
        }
        printInfo("Writing synthetic: " + outputFileName + " " + info);
        FileOutputStream file = new FileOutputStream(outputFileName);
        BufferedOutputStream writer = new BufferedOutputStream(file);
        writeHeader(writer, DEPRECATED_LIST);

        ICUResourceWriter.Resource current = set;
        while (current != null) {
          current.sort();
          current = current.next;
        }

        // Now start writing the resource;
        /* ICUResourceWriter.Resource */ current = set;
        while (current != null) {
          current.write(writer, 0, false);
          current = current.next;
        }
        writer.flush();
        writer.close();
      } catch (IOException e) {
        System.err.println(
            "ERROR: While writing synthetic locale " + outputFileName + ": " + e.toString());
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  private void writeResourceMakefile(
      String myTreeName, String generatedAliasList, String aliasFilesList, String inFileText,
      String emptyFileText, String brkFilesList, String ctdFilesList) {

    // Write resfiles.mk
    String stub = "UNKNOWN";
    String shortstub = "unk";

    if (myTreeName.equals("main")) {
      stub = "GENRB"; // GENRB_SOURCE, GENRB_ALIAS_SOURCE
      shortstub = "res"; // resfiles.mk
    } else if (myTreeName.equals("collation")) {
      stub = "COLLATION"; // COLLATION_ALIAS_SOURCE, COLLATION_SOURCE
      shortstub = "col"; // colfiles.mk
    } else if (myTreeName.equals("brkitr")) {
      stub = "BRK_RES"; // BRK_SOURCE, BRK_CTD_SOURCE BRK_RES_SOURCE
      shortstub = "brk"; // brkfiles.mk
    } else if (myTreeName.equals("rbnf")) {
      stub = "RBNF"; // RBNF_SOURCE, RBNF_ALIAS_SOURCE
      shortstub = "rbnf"; // brkfiles.mk
    } else {
      printError("", "Unknown tree name in writeResourceMakefile: " + myTreeName);
      System.exit(-1);
    }

    String resfiles_mk_name = destDir + "/" + shortstub + "files.mk";
    try {
      printInfo("Writing ICU build file: " + resfiles_mk_name);
      PrintStream resfiles_mk = new PrintStream(new FileOutputStream(resfiles_mk_name));
      Calendar c = Calendar.getInstance();
      resfiles_mk.println(
          "# *   Copyright (C) 1998-" + c.get(Calendar.YEAR) + ", International Business Machines");
      resfiles_mk.println("# *   Corporation and others.  All Rights Reserved.");
      resfiles_mk.println(stub + "_CLDR_VERSION = " + CLDRFile.GEN_VERSION);
      resfiles_mk.println("# A list of txt's to build");
      resfiles_mk.println("# Note: ");
      resfiles_mk.println("#");
      resfiles_mk.println("#   If you are thinking of modifying this file, READ THIS. ");
      resfiles_mk.println("#");
      resfiles_mk.println("# Instead of changing this file [unless you want to check it back in],");
      resfiles_mk.println(
          "# you should consider creating a '" + shortstub
          + "local.mk' file in this same directory.");
      resfiles_mk.println("# Then, you can have your local changes remain even if you upgrade or");
      resfiles_mk.println("# reconfigure ICU.");
      resfiles_mk.println("#");
      resfiles_mk.println("# Example '" + shortstub + "local.mk' files:");
      resfiles_mk.println("#");
      resfiles_mk .println("#  * To add an additional locale to the list: ");
      resfiles_mk .println("#    _____________________________________________________");
      resfiles_mk.println("#    |  " + stub + "_SOURCE_LOCAL =   myLocale.txt ...");
      resfiles_mk.println("#");
      resfiles_mk.println("#  * To REPLACE the default list and only build with a few");
      resfiles_mk.println("#     locale:");
      resfiles_mk.println("#    _____________________________________________________");
      resfiles_mk.println("#    |  " + stub + "_SOURCE = ar.txt ar_AE.txt en.txt de.txt zh.txt");
      resfiles_mk.println("#");
      resfiles_mk.println("#");
      resfiles_mk .println("# Generated by LDML2ICUConverter, from LDML source files. ");
      resfiles_mk.println("");
      resfiles_mk .println(
          "# Aliases which do not have a corresponding xx.xml file (see " + DEPRECATED_LIST + ")");
      resfiles_mk.println(
          stub + "_SYNTHETIC_ALIAS =" + generatedAliasList); // note: lists start with a space.
      resfiles_mk.println("");
      resfiles_mk.println("");
      resfiles_mk.println(
          "# All aliases (to not be included under 'installed'), but not including root.");
      resfiles_mk.println(stub + "_ALIAS_SOURCE = $(" + stub
              + "_SYNTHETIC_ALIAS)" + aliasFilesList);
      resfiles_mk.println("");
      resfiles_mk.println("");

      if (ctdFilesList != null) {
        resfiles_mk.println("# List of compact trie dictionary files (ctd).");
        resfiles_mk.println("BRK_CTD_SOURCE = " + ctdFilesList);
        resfiles_mk.println("");
        resfiles_mk.println("");
      }

      if (brkFilesList != null) {
        resfiles_mk.println("# List of break iterator files (brk).");
        resfiles_mk.println("BRK_SOURCE = " + brkFilesList);
        resfiles_mk.println("");
        resfiles_mk.println("");
      }

      if (emptyFileText != null) {
        resfiles_mk.println("# Empty locales, used for validSubLocale fallback.");
        // note: lists start with a space.
        resfiles_mk.println(stub + "_EMPTY_SOURCE =" + emptyFileText);
        resfiles_mk.println("");
        resfiles_mk.println("");
      }

      resfiles_mk.println("# Ordinary resources");
      if (emptyFileText == null) {
        resfiles_mk.print(stub + "_SOURCE =" + inFileText);
      } else {
        resfiles_mk.print(stub + "_SOURCE = $(" + stub + "_EMPTY_SOURCE)" + inFileText);
      }
      resfiles_mk.println("");
      resfiles_mk.println("");

      resfiles_mk.close();
    } catch(IOException e) {
      System.err.println("While writing " + resfiles_mk_name);
      e.printStackTrace();
      System.exit(1);
    }
  }
}
