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
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

import static org.unicode.cldr.icu.ICUID.*;

import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.ant.CLDRConverterTool;
import org.unicode.cldr.icu.LDML2ICUBinaryWriter;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceArray;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceInt;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceIntVector;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceAlias;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceProcess;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
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
  private String destDir;
  private String specialsDir;
  private String supplementalDir;
  private boolean writeDraft;
  private boolean writeBinary;
  private boolean asciiNumbers;

  /**
   * Add comments on the item to indicate where fallbacks came from. Good for
   * information, bad for diffs.
   */
  private static final boolean verboseFallbackComments = false;

  private Document supplementalDoc;
  private SupplementalDataInfo supplementalDataInfo;
  
  private static final boolean DEBUG = false;

  // TODO: hard-coded file names for now
  private static final String supplementalDataFile = "supplementalData.xml";
  private static final String metazoneInfoFile = "metazoneInfo.xml";
  private static final String likelySubtagsFile = "likelySubtags.xml";
  private static final String pluralsFile = "plurals.xml";
  private static final String numberingSystemsFile = "numberingSystems.xml";

  private List<String> _xpathList = new ArrayList<String>();

  private ICULog log;
  private ICUWriter writer;
  private CLDRFile.Factory cldrFactory;
  private CLDRFile.Factory specialsFactory;

  private final LDMLServices serviceAdapter = new LDMLServices() {
    public Factory cldrFactory() {
      return LDML2ICUConverter.this.cldrFactory;
    }

    public CLDRFile getSpecialsFile(String locale) {
      return LDML2ICUConverter.this.getSpecialsFile(locale);
    }

    public boolean xpathListContains(String xpath) {
      return LDML2ICUConverter.this.xpathListContains(xpath);
    }

    @Override
    public boolean isDraftStatusOverridable(String locName) {
      return LDML2ICUConverter.this.isDraftStatusOverridable(locName);
    }

    @Override
    public Resource parseBundle(CLDRFile file) {
      return LDML2ICUConverter.this.parseBundle(file);
    }

    @Override
    public SupplementalDataInfo getSupplementalDataInfo() {
      return LDML2ICUConverter.this.supplementalDataInfo;
    }
  };

  private Resource parseBundle(CLDRFile file) {
    LDML2ICUInputLocale fakeLocale = new LDML2ICUInputLocale(file, serviceAdapter);

    return parseBundle(fakeLocale);
  }

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

  /*
   * First method called from the main method. Will check all the args
   * and direct us from there.
   * If not doing anything special, just taking in XML files and writing
   * TXT or Binary files, then will call processFile()
   */
  @Override
  public void processArgs(String[] args) {
    int remainingArgc = 0;
    // Reset options (they're static).
    for (int i = 0; i < options.length; i++) {
      options[i].doesOccur = false;
    }
    
    try {
      remainingArgc = UOption.parseArgs(args, options);
    } catch (Exception e) {
      // log is not set up yet, so do this manually
      System.out.println("ERROR: parsing args '" + e.getMessage() + "'");
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
    } else {
      destDir = ".";
    }
    if (options[SPECIALSDIR].doesOccur) {
      specialsDir = options[SPECIALSDIR].value;
    }
    if (options[SUPPLEMENTALDIR].doesOccur) {
      supplementalDir = options[SUPPLEMENTALDIR].value;
    }
    if (options[WRITE_DRAFT].doesOccur) {
      writeDraft = true;
    }
    if (options[WRITE_BINARY].doesOccur) {
      writeBinary = true;
    }
    if (options[ASCII_NUMBERS].doesOccur) {
      asciiNumbers = true;
    }

    // Set up resource splitting, if we have it
    ResourceSplitter splitter = null;
    if (splitInfos != null) {
      splitter = new ResourceSplitter(destDir + "/..", splitInfos);
    }
    
    // Set up logging so we can use it here on out
    ICULog.Level level =
      DEBUG ? ICULog.Level.DEBUG : 
        options[VERBOSE].doesOccur ? ICULog.Level.INFO : ICULog.Level.LOG;
    log = new ICULogImpl(level);

    // Set up writer
    writer = new ICUWriter(destDir, log, splitter);

    if (options[WRITE_DEPRECATED].doesOccur) {
      if (remainingArgc > 0) {
        log.error("-w takes one argument, the directory, and no other XML files.\n");
        usage();
        return; // NOTREACHED
      }
      String depDirName = options[WRITE_DEPRECATED].value;
      File depDir = new File(depDirName);
      if (!depDir.isDirectory()) {
        log.error(depDirName + " isn't a directory.");
        usage();
        return; // NOTREACHED
      }
      // parse for draft status?
      File dstDir = new File(destDir);
      boolean parseDraft = !writeDraft;
      boolean parseSubLocale = sourceDir.indexOf("collation") > -1;
      new DeprecatedConverter(log, serviceAdapter, depDir, dstDir)
        .write(writer, aliasDeprecates, parseDraft, parseSubLocale);
      return;
    }
    
    if (supplementalDir != null) {
      supplementalDoc = createSupplementalDoc();
      supplementalDataInfo = SupplementalDataInfo.getInstance(supplementalDir);
    }

    if (options[SUPPLEMENTALONLY].doesOccur) {
      // TODO(dougfelt): this assumes there is no data in list before this point.  check.
      // addToXPathList(supplementalDoc);
      setXPathList(makeXPathList(supplementalDoc));
      
      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      log.log("Processing " + supplementalDataFile);
      Resource res = new SupplementalDataParser(log, serviceAdapter)
        .parse(supplementalDoc, supplementalDataFile);

      if (res != null && ((ResourceTable)res).first != null) {
        writer.writeResource(res, supplementalDataFile);
      }
    } else if (options[METAZONE_ONLY].doesOccur) {
      new MetazoneConverter(log, metazoneInfoFile, supplementalDir).convert(writer);
    } else if (options[LIKELYSUBTAGS_ONLY].doesOccur) {
      new LikelySubtagsConverter(log, likelySubtagsFile, supplementalDir).convert(writer);
    } else if (options[PLURALS_ONLY].doesOccur) {
      new PluralsConverter(log, pluralsFile, supplementalDir).convert(writer);
    } else if (options[NUMBERS_ONLY].doesOccur) {
      new NumberingSystemsConverter(log, numberingSystemsFile, supplementalDir).convert(writer);
    } else {
      spinUpFactories(sourceDir, specialsDir);
      if (getLocalesMap() != null && getLocalesMap().size() > 0) {
        for (Iterator<String> iter = getLocalesMap().keySet().iterator(); iter.hasNext();) {
          String fileName = iter.next();
          String draft = getLocalesMap().get(fileName);
          if (draft != null && !draft.equals("false")) {
            writeDraft = true;
          } else {
            writeDraft = false;
          }
          processFile(fileName);
        }
      } else if (remainingArgc > 0) {
        for (int i = 0; i < remainingArgc; i++) {
          if (args[i].equals("*")) {
            for (String file : new File(sourceDir).list()) {
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
        log.error("No files specified !");
      }
    }
  }

  /**
   * Serves to narrow the interface to InputLocale so that it can be
   * separated from LDML2ICUConverter.
   */
  static interface LDMLServices {
    /** Returns the cldr factory, or null */
    CLDRFile.Factory cldrFactory();
    /** Return a specials file for the locale */
    CLDRFile getSpecialsFile(String locale);
    /** Returns true if xpathlist contains the xpath */
    boolean xpathListContains(String xpath);

    // for DeprecatedConverter
    /** Returns true if draft status is overridable. */
    boolean isDraftStatusOverridable(String locName);
    /** Parses the CLDRFile, with the given status string */
    Resource parseBundle(CLDRFile file);
    
    // for SupplementalDataParser
    SupplementalDataInfo getSupplementalDataInfo();
  }
  
  private Document getSpecialsDoc(String locName) {
    if (specialsDir != null) {
      String locNameXml = locName + ".xml";
      String icuSpecialFile = specialsDir + "/" + locNameXml;
      if (new File(icuSpecialFile).exists()) {
        return LDMLUtilities.parseAndResolveAliases(locNameXml, specialsDir, false, false);
      }

      if (ULocale.getCountry(locName).length() == 0) {
        log.warning("ICU special not found for language-locale \"" + locName + "\"");
        // System.exit(-1);
      } else {
        log.warning("ICU special file not found, continuing.");
      }
    }

    return null;
  }

  private DocumentPair getDocumentPair(String locale) {
    String localeXml = locale + ".xml";
    String xmlfileName = LDMLUtilities.getFullPath(LDMLUtilities.XML, localeXml, sourceDir);

    Document doc = LDMLUtilities.parse(xmlfileName, false);
    Document specials = getSpecialsDoc(locale);
    if (specials != null) {
      StringBuilder xpath = new StringBuilder();
      doc = (Document) LDMLUtilities.mergeLDMLDocuments(
          doc, specials, xpath, null/* unused */, null /* unused */, false, true);
    }

    Document fullyResolvedDoc = null;
    if (!LDMLUtilities.isLocaleAlias(doc)) {
      fullyResolvedDoc = LDMLUtilities.getFullyResolvedLDML(
          sourceDir, localeXml, false, false, false, false);
    }

    if (writeDraft == false && isDraftStatusOverridable(locale)) {
      log.info("Overriding draft status, and including: " + locale);
      writeDraft = true;
      // TODO: save/restore writeDraft
    }
    
    setXPathList(makeXPathList(doc, fullyResolvedDoc, locale));

    return new DocumentPair(doc, fullyResolvedDoc);
  }

  private void setXPathList(List<String> xpathList) {
    _xpathList = xpathList;
  }

  private CLDRFile getSpecialsFile(String locale) {
    if (specialsFactory != null) {
      String icuSpecialFile = specialsDir + "/" + locale + ".xml";
      if (new File(icuSpecialFile).exists()) {
        log.info("Parsing ICU specials from: " + icuSpecialFile);
        return specialsFactory.make(locale, false);
      }
    }
    return null;
  }

  /*
   * Sets some stuff up and calls createResourceBundle
   */
  private void processFile(String fileName) {
    // add 1 below to skip past the separator
    int lastIndex = fileName.lastIndexOf(File.separator, fileName.length()) + 1;
    fileName = fileName.substring(lastIndex, fileName.length());
    String xmlfileName = LDMLUtilities.getFullPath(LDMLUtilities.XML, fileName, sourceDir);

    String locName = fileName;
    int index = locName.indexOf(".xml");
    if (index > -1) {
      locName = locName.substring(0, index);
    }

    log.setStatus(locName);
    log.log("Processing " + xmlfileName);
    ElapsedTimer timer = new ElapsedTimer();

    LDML2ICUInputLocale loc = new LDML2ICUInputLocale(locName, serviceAdapter);

    if (writeDraft == false && isDraftStatusOverridable(locName)) {
      log.info("Overriding draft status, and including: " + locName);
      writeDraft = true;
      // TODO: save/restore writeDraft
    }
    
    createResourceBundle(loc);
    
    log.info("Elapsed time: " + timer + "s");
  }

  private void spinUpFactories(String factoryDir, String specialsDir) {
    if (cldrFactory == null) {
      log.info("* Spinning up CLDRFactory on " + factoryDir);
      cldrFactory = CLDRFile.Factory.make(factoryDir, ".*");
      if (specialsDir != null) {
        log.info("* Spinning up specials CLDRFactory on " + specialsDir);
        specialsFactory = CLDRFile.Factory.make(specialsDir, ".*");
      }
    }
  }

  private static List<String> makeXPathList(Document doc) {
    List<String> xpathList = new ArrayList<String>();
    addToXPathList(xpathList, doc);
    return xpathList;
  }
  
  private static void addToXPathList(List<String> xpathList, Document doc) {
    addToXPathList(xpathList, doc, (StringBuilder)null);
  }
  
  private static void addToXPathList(List<String> xpathList, Node node, StringBuilder xpath) {
    if (xpath == null) {
      xpath = new StringBuilder("/");
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
        addToXPathList(xpathList, child, xpath);
      } else {
        xpathList.add(xpath.toString());
      }
      xpath.delete(savedLength, xpath.length());
    }
  }

  private List<String> makeXPathList(LDML2ICUInputLocale loc) {
    String locName = loc.getLocale();
    boolean exemplarsContainAZ = exemplarsContainAZ(loc);
    
    List<String> xpathList = new ArrayList<String>();
    for (Iterator<String> iter = loc.getFile().iterator(); iter.hasNext();) {
      xpathList.add(loc.getFile().getFullXPath(iter.next()));
    }
    addToXPathList(xpathList, supplementalDoc);
    Collections.sort(xpathList);
    
    return computeConvertibleXPathList(xpathList, exemplarsContainAZ, locName);
  }

  private List<String> computeConvertibleXPathList(List<String> xpathList,
      boolean exemplarsContainAZ, String locName) {
    dumpXPathList(xpathList, "Before computeConvertibleXPaths", "log1.txt");
    xpathList = computeConvertibleXPaths(xpathList, exemplarsContainAZ, locName, supplementalDir);
    dumpXPathList(xpathList, "After computeConvertibleXPaths", "log2.txt");
    return xpathList;
  }

  private static void dumpXPathList(List<String> xpathList, String msg, String fname) {
    if (DEBUG) {
      try {
        PrintWriter log = new PrintWriter(new FileOutputStream(fname));
        log.println("BEGIN: " + msg);
        for (String xpath : xpathList) {
          log.println(xpath);
        }
        log.println("END: " + msg);
        log.flush();
        log.close();
      } catch (Exception ex) {
        // debugging, throw away.
      }
    }
  }

  private List<String> makeXPathList(Document doc, Document fullyResolvedDoc, String locName) {
    boolean exemplarsContainAZ = exemplarsContainAZ(fullyResolvedDoc);

    List<String> xpathList = new ArrayList<String>();
    addToXPathList(xpathList, doc);
    addToXPathList(xpathList, supplementalDoc);
    Collections.sort(xpathList);
        
    return computeConvertibleXPathList(xpathList, exemplarsContainAZ, locName);
  }

  private static boolean exemplarsContainAZ(Document fullyResolvedDoc) {
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

  private static boolean exemplarsContainAZ(LDML2ICUInputLocale loc) {
    if (loc == null) {
      return false;
    }

    UnicodeSet set = loc.getFile().getExemplarSet("", CLDRFile.WinningChoice.WINNING);
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
      log.error("Supplemental files are missing " + canonicalPath);
      System.exit(-1);
    }

    Document doc = null;
    for (int i = 0; i < files.length; i++) {
      try {
        log.info("Parsing document " + files[i]);
        String fileName = myDir.getAbsolutePath() + File.separator + files[i];
        Document child = LDMLUtilities.parse(fileName, false);
        if (doc == null) {
          doc = child;
          continue;
        }
        StringBuilder xpath = new StringBuilder();
        LDMLUtilities.mergeLDMLDocuments(doc, child, xpath, files[i], myDir.getAbsolutePath(), true,
           false);
      } catch (Throwable se) {
        log.error("Parsing: " + files[i] + " " + se.toString(), se);
        System.exit(1);
      }
    }

    return doc;
  }

  /*
   * Create the Resource tree, and then Call writeResource or
   * LDML2ICUBinaryWriter.writeBinaryFile(), whichever is appropriate
   */
  private void createResourceBundle(LDML2ICUInputLocale loc) {
    try {
      // calculate the list of vettable xpaths.
      try {
        setXPathList(makeXPathList(loc));
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Can't make XPathList for: " + loc).initCause(e);
      }

      // Create the Resource linked list which will hold the
      // data after parsing
      // The assumption here is that the top
      // level resource is always a table in ICU
      Resource res = parseBundle(loc);
      if (res != null && ((ResourceTable) res).first != null) {
        if (loc.getSpecialsFile() != null) {
          String dir = specialsDir.replace('\\', '/');
          dir = "<path>" + dir.substring(dir.indexOf("/xml"), dir.length());
          String locName = loc.getLocale();
          if (res.comment == null) {
            res.comment = " ICU <specials> source: " + dir + "/" + locName + ".xml";
          } else {
            res.comment = res.comment + " ICU <specials> source: " + dir + "/" + locName + ".xml";
          }
        }
        // write out the bundle depending on if writing Binary or txt
        if (writeBinary) {
          LDML2ICUBinaryWriter.writeBinaryFile(res, destDir, loc.getLocale());
        } else {
          String sourceInfo = sourceDir.replace('\\','/') + "/" + loc.getLocale() + ".xml";
          writer.writeResource(res, sourceInfo);
        }
      }
      // writeAliasedResource();
    } catch (Throwable se) {
      log.error("Parsing and writing " + loc.getLocale() + " " + se.toString(), se);
      System.exit(1);
    }
  }

  private static final String LOCALE_SCRIPT = "LocaleScript";
  private static final String NUMBER_ELEMENTS = "NumberElements";
  private static final String NUMBER_PATTERNS = "NumberPatterns";
  private static final String AM_PM_MARKERS = "AmPmMarkers";
  private static final String DTP = "DateTimePatterns";
  private static final String DTE = "DateTimeElements";

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
    
    deprecatedTerritories.put("BQ", "");
    deprecatedTerritories.put("CT", "");
    deprecatedTerritories.put("DD", "");
    deprecatedTerritories.put("FQ", "");
    deprecatedTerritories.put("FX", "");
    deprecatedTerritories.put("JT", "");
    deprecatedTerritories.put("MI", "");
    deprecatedTerritories.put("NQ", "");
    deprecatedTerritories.put("NT", "");
    deprecatedTerritories.put("PC", "");
    deprecatedTerritories.put("PU", "");
    deprecatedTerritories.put("PZ", "");
    deprecatedTerritories.put("SU", "");
    deprecatedTerritories.put("VD", "");
    deprecatedTerritories.put("WK", "");
    deprecatedTerritories.put("YD", "");
    //TODO: "FX",  "RO",  "TP",  "ZR",   /* obsolete country codes */
  }


  public static ResourceArray getResourceArray(String str, String name) {
    if (str != null) {
      String[] strs = str.split("\\s+");
      ResourceArray arr = new ResourceArray();
      arr.name = name;
      Resource curr = null;
      for (int i = 0; i <strs.length; i++) {
        ResourceString string = new ResourceString();
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

  public static String getICUAlias(String tzid) {
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

  private String ldmlVersion_ = null;
  
  private String getLdmlVersion() {
    return ldmlVersion_;
  }
  
  private void setLdmlVersion(String version) {
    ldmlVersion_ = version;
  }

  private Resource parseBundle(LDML2ICUInputLocale loc) {
    final boolean SEPARATE_LDN = false;
    
    setLdmlVersion("0.0");

    String localeID = loc.getFile().getLocaleID();

    ResourceTable mainTable = new ResourceTable();
    mainTable.name = localeID;
    
    // handle identity
    Resource version = parseIdentity(loc);
    if (version != null) {
      mainTable.appendContents(version);
    }

    // handle alias, early exit
    if (loc.getFile().isHere("//ldml/alias")) {
      Resource res = ICUResourceWriter.createString("\"%%ALIAS\"", 
          loc.getBasicAttributeValue("//ldml/alias", LDMLConstants.SOURCE));
      mainTable.appendContents(res);
      return mainTable;
    }

    // If this is a language + script locale and the script is not default content,
    // then add a "Parent is root" boolean resource in order to prevent cross-script
    // inheritance.
    if (ULocale.getScript(localeID).length() > 0
        && ULocale.getCountry(localeID).length() == 0
        && !supplementalDataInfo.getDefaultContentLocales().contains(localeID)
        && sourceDir.indexOf("coll") < 0) {

      ResourceInt pr = new ResourceInt();
      pr.name = "%%ParentIsRoot";
      pr.val = "1";
      mainTable.appendContents(pr);
    }
    
    // Now, loop over other stuff.
    String stuff[] = {
        // Following two resources are handled above
        // LDMLConstants.ALIAS,
        // LDMLConstants.IDENTITY,

        LDMLConstants.SPECIAL,
        LDMLConstants.LDN,
        LDMLConstants.LAYOUT,
        // LDMLConstants.FALLBACK
        LDMLConstants.CHARACTERS,
        LDMLConstants.DELIMITERS,
        LDMLConstants.DATES,
        LDMLConstants.NUMBERS,
        // LDMLConstants.POSIX,
        // LDMLConstants.SEGMENTATIONS,
        LDMLConstants.REFERENCES,
        LDMLConstants.RBNF,
        LDMLConstants.COLLATIONS,
        LDMLConstants.UNITS,
        LDMLConstants.UNITS_SHORT
    };

    for (String name : stuff) {
      String xpath = "//ldml/" + name;
      log.info(name + " ");
      
      Resource res = null;
      if (name.equals(LDMLConstants.SPECIAL)) {
        res = parseSpecialElements(loc, xpath);
      } else if (!SEPARATE_LDN && name.equals(LDMLConstants.LDN)) {
        res = parseLocaleDisplayNames(loc);
      } else if (name.equals(LDMLConstants.LAYOUT)) {
        res = parseLayout(loc, xpath);
      } else if (name.equals(LDMLConstants.FALLBACK)) {
        // ignored
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
        // res = parsePosix(loc, xpath);
      } else if (name.equals(LDMLConstants.RBNF)) {
        res = parseRBNF(loc, xpath);
      } else if (name.equals(LDMLConstants.SEGMENTATIONS)) {
        // TODO: FIX ME with parseSegmentations();
        if (DEBUG) {
          log.warning("Not producing resource for " + xpath.toString());
        }
      } else if (name.indexOf("icu:") > -1 || name.indexOf("openOffice:") > -1) {
        // TODO: these are specials .. ignore for now ... figure out
        // what to do later
      } else if (name.equals(LDMLConstants.REFERENCES)) {
        // TODO: This is special documentation... ignore for now
        if (DEBUG) {
          log.warning("Not producing resource for " + xpath.toString());
        }
      } else if (name.equals(LDMLConstants.UNITS)) {
        res = parseUnits(loc, name, null);
      } else if (name.equals(LDMLConstants.UNITS_SHORT)) {
        res = parseUnits(loc, name, LDMLConstants.SHORT);
      } else {
        log.error("Encountered unknown <" + "//ldml" + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) { // have an item
        mainTable.appendContents(res);
      }
    }

    if (sourceDir.indexOf("main") > 0 /* && !LDMLUtilities.isLocaleAlias(root)*/) {
      String locName = loc.getLocale();
      String country = ULocale.getCountry(locName);
      String variant = ULocale.getVariant(locName);
      boolean isRoot = locName.equals("root");

      Resource temp = parseWeek(country, variant, isRoot);
      if (temp != null) {
        Resource greg = findResource(mainTable, LDMLConstants.GREGORIAN);
        Resource cals = findResource(mainTable, LDMLConstants.CALENDAR);
        if (greg != null) {
          greg.first.end().next = temp;
        } else if (cals != null) {
          greg = new ResourceTable();
          greg.name = LDMLConstants.GREGORIAN;
          greg.first = temp;
          cals.first.end().next = greg;
        } else {
          greg = new ResourceTable();
          greg.name = LDMLConstants.GREGORIAN;
          greg.first = temp;

          ResourceTable cal = new ResourceTable();
          cal.name = LDMLConstants.CALENDAR;
          cal.first = greg;

          mainTable.appendContents(cal);
        }
      }
      temp = parseMeasurement(country, variant, isRoot);
      if (temp != null) {
        mainTable.appendContents(temp);
      }
    }

    log.info("");

    if (supplementalDoc != null) {
      /*
       * TODO: comment this out for now. We shall revisit when we have
       * information on how to present the script data with new API
       * Resource res =
       * parseLocaleScript(supplementalDoc); if (res != null) { if (current ==
       * null) { table.first = res; current = findLast(res); }else{
       * current.next = res; current = findLast(res); } res = null; }
       *
       * Resource res = parseMetaData(supplementalDoc);
       */
    }

    return mainTable;
  }

  private static Resource findResource(Resource res, String type) {
    Resource current = res;
    Resource ret = null;
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
   * an alias at xpath + "/alias", if so, create & return it.
   */
  private Resource getAliasResource(LDML2ICUInputLocale loc, String xpath) {
    String name = XPPUtil.getXpathName(xpath);
    String aliasPath = xpath + "/alias";
    Resource aRes = parseAliasResource(loc, aliasPath);
    if (aRes != null) {
      aRes.name = name;
    }

    return aRes;
  }

  private Resource parseAliasResource(LDML2ICUInputLocale loc, String xpath) {
    String source = loc.getBasicAttributeValue(xpath, LDMLConstants.SOURCE);
    String path = loc.getBasicAttributeValue(xpath, LDMLConstants.PATH);
    if (source == null && path == null) {
      if (!loc.getFile().isHere(xpath)) {
        return null;
      }
    }
    
    try {
      ResourceAlias alias = new ResourceAlias();
      String basePath = xpath.replaceAll("/alias.*$", "");
      String fullPath = loc.getFile().getFullXPath(xpath).replaceAll("/alias.*$", "");
      if (path != null) {
        path = path.replaceAll("='", "=\"").replaceAll("']", "\"]");
      }

      String val = LDMLUtilities.convertXPath2ICU(source, path, basePath, fullPath);
      alias.val = val;
      alias.name = basePath;
      return alias;
    } catch (TransformerException ex) {
      log.error("Could not compile XPATH for source: "
              + source + " path: " + path + " Node: " + xpath, ex);
      System.exit(-1);
    }

    return null;
    // TODO update when XPATH is integrated into LDML
  }

  private Resource parseAliasResource(Node node, StringBuilder xpath) {
    return parseAliasResource(node, xpath, false);
  }

  private Resource parseAliasResource(
      Node node, StringBuilder xpath, boolean isCollation) {
    int saveLength = xpath.length();
    getXPath(node, xpath);
    try {
      if (node != null && (isCollation || !isNodeNotConvertible(node, xpath))) {
        ResourceAlias alias = new ResourceAlias();
        xpath.setLength(saveLength);
        String val = LDMLUtilities.convertXPath2ICU(node, null, xpath);
        alias.val = val;
        alias.name = node.getParentNode().getNodeName();
        xpath.setLength(saveLength);
        return alias;
      }
    } catch(TransformerException ex) {
      log.error(
          "Could not compile XPATH for" +
          " source:  " + LDMLUtilities.getAttributeValue(node, LDMLConstants.SOURCE) +
          " path: " + LDMLUtilities.getAttributeValue(node, LDMLConstants.PATH) +
          " Node: " + node.getParentNode().getNodeName(), ex);
      System.exit(-1);
    }

    xpath.setLength(saveLength);
    // TODO update when XPATH is integrated into LDML
    return null;
  }

  private Resource parseIdentity(LDML2ICUInputLocale loc) {
    // version #
    String verPath = "//ldml/" + LDMLConstants.IDENTITY + "/" + LDMLConstants.VERSION;
    String version = XPPUtil.getBasicAttributeValue(loc.getFile(), verPath, LDMLConstants.NUMBER);
    if (loc.resolved() != null) {
      String version2 = XPPUtil.getBasicAttributeValue(loc.resolved(), verPath, 
          LDMLConstants.NUMBER);
      String foundIn = loc.resolved().getSourceLocaleID(verPath, null);
      if (foundIn != null && foundIn.equals(loc.getLocale()) && version2 != null) {
        // make sure it is in our 'original' locale.
        version = version2; // use version from 'resolved' -
      }
    }

    if (version == null) {
      log.warning("No version #??");
      return null;
    }
    
    version = version.replaceAll(".*?Revision: (.*?) .*", "$1");

    int intversion;
    try {
      intversion = Integer.valueOf(version).intValue();
    } catch (NumberFormatException ex) {
      intversion = 1;
    }

    if (intversion > 1) { // This is a SVN changeset number
      int x = intversion / 10000;
      int y = (intversion - 10000 * x) / 100;
      int z = (intversion - 10000 * x) % 100;
      version = "2." +
      Integer.toString(x) + "." +
      Integer.toString(y) + "." +
      Integer.toString(z);
    }

    return ICUResourceWriter.createString(keyNameMap.get(LDMLConstants.VERSION), version);
  }

  private static final String[] registeredKeys = new String[] {
    "collation", "calendar", "currency"
  };

  private Resource parseLocaleDisplayNames(LDML2ICUInputLocale loc) {
    Resource first = null;
    Resource current = null;

    Resource res = null;
    String stuff[] = {
      LDMLConstants.LANGUAGES,
      LDMLConstants.SCRIPTS,
      LDMLConstants.TERRITORIES,
      LDMLConstants.KEYS,
      LDMLConstants.VARIANTS,
      LDMLConstants.MSNS,
      LDMLConstants.TYPES,
      LDMLConstants.ALIAS,
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
        log.error("Unknown element found: " + name);
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

  private Resource parseDisplayTypes(LDML2ICUInputLocale loc, String name) {
    log.setStatus(loc.getLocale());
    StringBuilder myXpath = new StringBuilder();
    myXpath.append("//ldml/localeDisplayNames/types");
    ResourceTable table = new ResourceTable();
    table.name = keyNameMap.get(LDMLConstants.TYPES);
    Resource alias = null;

    // if the whole thing is an alias
    if ((alias = getAliasResource(loc, myXpath.toString())) != null) {
      alias.name = table.name;
      return alias;
    }

    for (int i = 0; i < registeredKeys.length; i++) {
      ResourceTable subTable = new ResourceTable();
      subTable.name = registeredKeys[i];
      for (Iterator<String> iter = loc.getFile().iterator(myXpath.toString()); iter.hasNext();) {
        String xpath = iter.next();
        String name2 = XPPUtil.getXpathName(xpath);
        if (!LDMLConstants.TYPE.equals(name2)) {
          log.error("Encountered unknown <" + xpath + "> subelement: " + name2 +
                               " while looking for " + LDMLConstants.TYPE);
          System.exit(-1);
        }

        String key = XPPUtil.getAttributeValue(xpath, LDMLConstants.KEY);
        if (!registeredKeys[i].equals(key)) {
          continue;
        }

        String type = XPPUtil.getAttributeValue(xpath, LDMLConstants.TYPE);
        if (loc.isPathNotConvertible(xpath)) {
          continue;
        }

        String val = loc.getFile().getStringValue(xpath);
        Resource string = ICUResourceWriter.createString(type, val);
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

  private Resource parseLocaleDisplayPattern(LDML2ICUInputLocale loc) {
    log.setStatus(loc.getLocale());
    StringBuilder myXpath = new StringBuilder();
    myXpath.append("//ldml/localeDisplayNames/");
    myXpath.append(LDMLConstants.LOCALEDISPLAYPATTERN);
    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.LOCALEDISPLAYPATTERN;
    Resource alias = null;

    // if the whole thing is an alias
    if ((alias = getAliasResource(loc, myXpath.toString()))!= null) {
      alias.name = table.name;
      return alias;
    }

    for (Iterator <String > iter = loc.getFile().iterator(myXpath.toString()); iter.hasNext();) {
      String xpath = iter.next();
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      String element = XPPUtil.getXpathName(xpath);
      String name = null;
      if (LDMLConstants.LOCALE_PATTERN.equals(element)) {
        name = LDMLConstants.PATTERN;
      } else if (LDMLConstants.LOCALE_SEPARATOR.equals(element)) {
        name = LDMLConstants.SEPARATOR;
      } else {
        log.error(
            "Encountered unknown <" + xpath
            + "> subelement: " + element
            + " while looking for " + LDMLConstants.TYPE);
        System.exit(-1);
      }

      String value = loc.getFile().getStringValue(xpath);
      Resource res = ICUResourceWriter.createString(name, value);
      table.appendContents(res);
    }

    if (!table.isEmpty()) {
      return table;
    }

    return null;
  }

  private Resource parseList(LDML2ICUInputLocale loc, String name) {
    ResourceTable table = new ResourceTable();
    String rootNodeName = name;
    table.name = keyNameMap.get(rootNodeName);
    Resource current = null;
    boolean uc = rootNodeName.equals(LDMLConstants.VARIANTS);
    boolean prohibit = rootNodeName.equals(LDMLConstants.TERRITORIES);
    String origXpath = "//ldml/localeDisplayNames/" + name;
    if ((current = getAliasResource(loc, origXpath)) != null) {
      current.name = table.name;
      return current;
    }

    for (Iterator<String> iter = loc.getFile().iterator(origXpath); iter.hasNext();) {
      String xpath = iter.next();
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      ResourceString res = new ResourceString();
      res.name = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
      if (uc) {
        res.name = res.name.toUpperCase();
      }
      res.val = loc.getFile().getStringValue(xpath);

      if (res.name == null) {
        log.error(name + " - " + res.name + " = " + res.val);
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

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private Resource parseListAlt(
      LDML2ICUInputLocale loc, String originalName, String name, String altValue) {

    ResourceTable table = new ResourceTable();
    String rootNodeName = name;
    table.name = keyNameMap.get(rootNodeName);
    Resource current = null;
    boolean uc = rootNodeName.equals(LDMLConstants.VARIANTS);
    boolean prohibit = rootNodeName.equals(LDMLConstants.TERRITORIES);
    String origXpath = "//ldml/localeDisplayNames/" + originalName;
    if ((current = getAliasResource(loc, origXpath)) != null) {
      current.name = table.name;
      return current;
    }

    for (Iterator<String> iter = loc.getFile().iterator(origXpath); iter.hasNext();) {
      String xpath = iter.next();
      // Check for the "alt" attribute, and process it if requested.
      // Otherwise, skip it.
      String alt = loc.getBasicAttributeValue(xpath, LDMLConstants.ALT);
      if (alt == null || !alt.equals(altValue)) {
        continue;
      }

      ResourceString res = new ResourceString();
      res.name = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
      if (uc) {
        res.name = res.name.toUpperCase();
      }
      res.val = loc.getFile().getStringValue(xpath);

      if (res.name == null) {
        log.error(name + " - " + res.name + " = " + res.val);
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
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private Resource parseArray(LDML2ICUInputLocale loc, String xpath) {
    ResourceArray array = new ResourceArray();
    String name = XPPUtil.getXpathName(xpath);
    array.name = keyNameMap.get(name);
    Resource current = null;
    // want them in sorted order (?)
    Set<String> xpaths = new TreeSet<String>();
    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      xpaths.add(xpath);
    }

    for(String apath : xpaths) {
      name = XPPUtil.getXpathName(apath);

      if (current == null) {
        current = array.first = new ResourceString();
      } else {
        current.next = new ResourceString();
        current = current.next;
      }

      ((ResourceString) current).val = loc.getFile().getStringValue(apath);
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
  private Resource parseTable(
      LDML2ICUInputLocale loc, String xpath, String element, String attribute) {

    ResourceTable array = new ResourceTable();
    String name = XPPUtil.getXpathName(xpath);
    array.name = keyNameMap.get(name); // attempt
    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      name = XPPUtil.getXpathName(xpath);
      if (!name.equals(element)) {
        log.error("Err: unknown item " + xpath + " / " + name + " - expected " + element);
        continue;
      }
      String type = loc.getBasicAttributeValue(xpath, attribute);
      String val =  loc.getFile().getStringValue(xpath);

      array.appendContents(ICUResourceWriter.createString(type, val));
    }

    if (array.first != null) {
      return array;
    }

    return null;
  }

  private static final String ICU_SCRIPT = "icu:script";

  private Resource parseCharacters(LDML2ICUInputLocale loc, String xpath) {
    Resource first = null;
    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      if (loc.isPathNotConvertible(aPath)) {
        continue;
      }

      String name = XPPUtil.getXpathName(aPath);

      Resource res = null;
      if (name.equals(LDMLConstants.EXEMPLAR_CHARACTERS)) {
        String type = loc.getBasicAttributeValue(aPath, LDMLConstants.TYPE);
        res = parseStringResource(loc, aPath);
        if (type != null && type.equals(LDMLConstants.AUXILIARY)) {
          res.name = keyNameMap.get(LDMLConstants.AUXILIARY);
        } else if (type != null && type.equals(LDMLConstants.CURRENCY_SYMBOL)) {
          res = null;
        } else if (type != null && type.equals(LDMLConstants.INDEX)) {
          res = null;
        } else {
          res.name = keyNameMap.get(name);
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
        log.error("Unknown  character element found: " + aPath
                + " / " + name + " -> " + loc.getFile().getFullXPath(aPath));
        System.exit(-1);
      }
      if (res != null) {
        first = Resource.addAfter(first, res);
      }
    }

    return first;
  }

  private Resource parseStringResource(LDML2ICUInputLocale loc, String xpath) {
    ResourceString str = new ResourceString();
    str.val = loc.getFile().getStringValue(xpath);
    str.name = XPPUtil.getXpathName(xpath);
    return str;
  }

  private Resource parseDelimiters(LDML2ICUInputLocale loc, String xpath) {
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    ResourceTable table = new ResourceTable();
    table.name = XPPUtil.getXpathName(xpath);

    Resource current = table.first;
    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = XPPUtil.getXpathName(xpath);
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      Resource res = null;
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
        log.error("Unknown element found: " + xpath);
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

  private Resource parseMeasurement(String country, String variant, boolean isRoot) {
    Resource ret = null;
     // optimization
    if (variant.length() != 0) {
      return ret;
    }

    Resource current = null;
    Resource first = null;
    StringBuilder xpath = new StringBuilder("//supplementalData/measurementData");
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
      Resource res = null;
      if (name.equals(LDMLConstants.MS)) {
        getXPath(node, xpath);
        if (isNodeNotConvertible(node, xpath)) {
          xpath.setLength(oldLength);
          continue;
        }

        String terr = LDMLUtilities.getAttributeValue(node, LDMLConstants.TERRITORIES);
        if (terr != null && ((isRoot && terr.equals("001")) ||
                (country.length() > 0 && terr.indexOf(country) >= 0))) {
          ResourceInt resint = new ResourceInt();
          String sys = LDMLUtilities.getAttributeValue(node,LDMLConstants.TYPE);
          if (sys.equals("US")) {
            resint.val = "1";
          } else {
            resint.val = "0";
          }
          resint.name = keyNameMap.get(LDMLConstants.MS);
          res = resint;
        }
      } else if (name.equals(LDMLConstants.PAPER_SIZE)) {
        String terr = LDMLUtilities.getAttributeValue(node,LDMLConstants.TERRITORIES);
        if (terr != null && ((isRoot && terr.equals("001")) ||
                (country.length() > 0 && terr.indexOf(country) >= 0))) {
          ResourceIntVector vector = new ResourceIntVector();
          vector.name = keyNameMap.get(name);
          ResourceInt height = new ResourceInt();
          ResourceInt width = new ResourceInt();
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
        log.error("Unknown element found: " + name);
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

  private Resource parseLayout(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    table.name = XPPUtil.getXpathName(xpath);
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      String name = XPPUtil.getXpathName(aPath);

      Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, aPath);
        return res;
      }

      if (name.equals(LDMLConstants.INLIST)) {
        ResourceString cs = null;
        if (!loc.isPathNotConvertible(aPath)) {
          String casing = loc.getBasicAttributeValue(xpath, LDMLConstants.CASING);
          if (casing != null) {
            cs = new ResourceString();
            cs.comment = "Used for figuring out the casing of characters in a list.";
            cs.name = LDMLConstants.CASING;
            cs.val = casing;
            res = cs;
          }
        }
      } else if (name.equals(LDMLConstants.ORIENTATION)) {
        ResourceString chs = null;
        ResourceString lns = null;
        if (!loc.isPathNotConvertible(aPath)) {
          String characters = loc.getBasicAttributeValue(aPath, LDMLConstants.CHARACTERS);
          String lines = loc.getBasicAttributeValue(aPath, LDMLConstants.LINES);
          if (characters != null) {
            chs = new ResourceString();
            chs.name = LDMLConstants.CHARACTERS;
            chs.val = characters;
          }
          if (lines != null) {
            lns = new ResourceString();
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
        log.error("Unknown element found: " + xpath + " / " + name);
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

  private Resource parseDates(LDML2ICUInputLocale loc, String xpath) {
    Resource first = null;
    Resource current = null;
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
      Resource res = null;

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
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
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

  private Resource parseCalendars(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;
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
      if (!loc.isNotOnDisk() &&  loc.isPathNotConvertible(xpath)) {
        continue;
      }
      Resource res = null;

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
        // if there was an item, resync current.
        if (table.first != null) {
          current = table.first.end();
        }
      } else {
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
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

  private Resource parseTimeZoneNames(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;
    table.name = keyNameMap.get(XPPUtil.getXpathName(xpath));

    Set<String> zones = new HashSet<String>();
    Set<String> metazones = new HashSet<String>();
    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      String apath = iter.next();
      String name = XPPUtil.getXpathName(apath, 3);
      if (loc.isPathNotConvertible(apath)) {
        continue;
      }
      Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, apath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(loc, apath, name);
      } else if (name.equals(LDMLConstants.ZONE)) {
        String tzname = XPPUtil.getAttributeValue(apath, LDMLConstants.ZONE, LDMLConstants.TYPE);
        zones.add(tzname);
      } else if (name.equals(LDMLConstants.METAZONE)) {
        String mzname = XPPUtil.getAttributeValue(apath, LDMLConstants.METAZONE, LDMLConstants.TYPE);
        metazones.add(mzname);
      } else if (
          name.equals(LDMLConstants.HOUR_FORMAT)
          || name.equals(LDMLConstants.HOURS_FORMAT)
          || name.equals(LDMLConstants.GMT_FORMAT)
          || name.equals(LDMLConstants.GMT_ZERO_FORMAT)
          || name.equals(LDMLConstants.REGION_FORMAT)
          || name.equals(LDMLConstants.FALLBACK_FORMAT)) {
        ResourceString str = new ResourceString();
        str.name = name;
        str.val = loc.getFile().getStringValue(apath);
        if (str.val != null) {
          res = str;
        }
      } else if (name.equals(LDMLConstants.ABBREVIATION_FALLBACK)) {
        ResourceString str = new ResourceString();
        str.name = name;
        str.val = loc.getBasicAttributeValue(apath, LDMLConstants.TYPE);
        if (str.val != null) {
          res = str;
        }
      } else if (
          name.equals(LDMLConstants.PREFERENCE_ORDERING)
          || name.equals(LDMLConstants.SINGLE_COUNTRIES)) {
        ResourceArray arr = new ResourceArray();
        arr.name = name;
        Resource c = null;
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
          ResourceString str = new ResourceString();
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
        log.error("Encountered unknown <" + apath + "> subelement: " + name);
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
      Resource res = null;
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
      Resource res = null;
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

  private ResourceString getDefaultResource(LDML2ICUInputLocale loc, String xpath) {
    return getDefaultResource(loc, xpath, XPPUtil.getXpathName(xpath));
  }

  /**
   * @deprecated
   */
  @Deprecated
  public static ResourceString getDefaultResource(Node node, StringBuilder xpath, String name) {
    ResourceString str = new ResourceString();
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

  private static ResourceString getDefaultResource(LDML2ICUInputLocale loc, String xpath, String name) {
    ResourceString str = new ResourceString();
    String temp = loc.getBasicAttributeValue(xpath, LDMLConstants.CHOICE);
    if (temp == null) {
      temp = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
      if (temp == null) {
        if (!loc.getFile().isHere(xpath)) {
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

  private static ResourceString getDefaultResourceWithFallback(
      LDML2ICUInputLocale loc, String xpath, String name) {
    ResourceString str = new ResourceString();

    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    // try to get from the specified locale
    String temp = loc.getBasicAttributeValue(xpath, LDMLConstants.CHOICE);
    if (temp == null) {
      temp = loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE);
    }
    if (temp == null) {
      temp =  XPPUtil.getBasicAttributeValue(loc.resolved(), xpath, LDMLConstants.CHOICE);
    }
    if (temp == null) {
      temp = XPPUtil.getBasicAttributeValue(loc.resolved(), xpath, LDMLConstants.TYPE);
    }

    // check final results
    if (temp == null) {
      if (!loc.getFile().isHere(xpath)) {
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

  private Resource parseZone(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    ResourceTable uses_mz_table = new ResourceTable();

    boolean containsUM = false;
    int mz_count = 0;

    String id = XPPUtil.getAttributeValue(xpath, LDMLConstants.ZONE, LDMLConstants.TYPE);

    table.name = "\"" + id + "\"";
    table.name = table.name.replace('/', ':');
    Resource current = null;
    Resource current_mz = null;
    uses_mz_table.name = "um";

    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      String name = XPPUtil.getXpathName(aPath);
      Resource res = null;

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
        String shortlong = XPPUtil.getXpathName(aPath, -2).substring(0,1);
        ResourceString str = new ResourceString();
        str.name = shortlong + name.substring(0,1);
        str.val = loc.getFile().getStringValue(aPath);
        if (str.val != null) {
          res = str;
        }
      } else if (name.equals(LDMLConstants.COMMONLY_USED)) {
        ResourceInt resint = new ResourceInt();
        String used = loc.getFile().getStringValue(aPath);
        if (used.equals("true")) {
          resint.val = "1";
        } else {
          resint.val = "0";
        }
        resint.name = "cu";
        res = resint;
      } else if (name.equals(LDMLConstants.USES_METAZONE)) {
        ResourceArray this_mz = new ResourceArray();
        ResourceString mzone = new ResourceString();
        ResourceString from = new ResourceString();
        ResourceString to = new ResourceString();

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
          current_mz = this_mz.end();
        } else {
          current_mz.next = this_mz;
          current_mz = this_mz.end();
        }
        containsUM = true;

        res = null;
      } else if (name.equals(LDMLConstants.EXEMPLAR_CITY)) {
        String ec = loc.getFile().getStringValue(aPath);
        if (ec != null) {
          ResourceString str = new ResourceString();
          str.name = "ec";
          str.val = ec;
          res = str;
        }
      } else {
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
    }

    // Add the metazone mapping table if mz mappings were present
    if (containsUM) {
      Resource res = uses_mz_table;
      if (current == null) {
        table.first = res;
        current = res.end();
      } else {
        current.next = res;
        current = res.end();
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private Resource parseMetazone(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    String id = XPPUtil.getAttributeValue(xpath, LDMLConstants.METAZONE, LDMLConstants.TYPE);
    table.name = "\"meta:" + id + "\"";
    table.name = table.name.replace('/', ':');
    Resource current = null;

    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      String aPath = iter.next();
      String name = XPPUtil.getXpathName(aPath);
      Resource res = null;
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
        String shortlong = XPPUtil.getXpathName(aPath, -2).substring(0,1);
        ResourceString str = new ResourceString();
        str.name = shortlong + name.substring(0,1);
        str.val = loc.getFile().getStringValue(aPath);
        if (str.val != null) {
          res = str;
        }
      } else if (name.equals(LDMLConstants.COMMONLY_USED)) {
        ResourceInt resint = new ResourceInt();
        String used = loc.getFile().getStringValue(aPath);
        if (used.equals("true")) {
          resint.val = "1";
        } else {
          resint.val = "0";
        }
        resint.name = "cu";
        res = resint;
      } else {
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
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

  private Resource parseLeapMonth(LDML2ICUInputLocale loc, String xpath) {
    // So.
    String theArray[] = leapStrings;
    ResourceString strs[] = new ResourceString[theArray.length];
    GroupStatus status = parseGroupWithFallback(loc, xpath, theArray, strs, false);
    if (GroupStatus.EMPTY == status) {
      log.warning("Could not load " + xpath + " - " + theArray[0] + ", etc.");
      return null; // NO items were found - don't even bother.
    }

    if (GroupStatus.SPARSE == status) {
      log.warning("Could not load all of " + xpath + " - " + theArray[0] + ", etc.");
      return null; // NO items were found - don't even bother.
    }

    ResourceArray arr = new ResourceArray();
    arr.name = "isLeapMonth";
    for(ResourceString str : strs) {
      arr.appendContents(str);
    }

    return arr;
  }

  private Resource parseIntervalFormats(LDML2ICUInputLocale loc, String parentxpath) {
    String xpath = parentxpath + "/" + LDMLConstants.INTVL_FMTS;
    Resource formats;

    formats = parseAliasResource(loc, xpath + "/" + LDMLConstants.ALIAS);
    if (formats != null) {
      formats.name = LDMLConstants.INTVL_FMTS;
      String val = ((ResourceAlias)formats).val;
      ((ResourceAlias)formats).val = val.replace(DTP + "/", "");
      return formats;
    }

    formats = new ResourceTable();
    formats.name = LDMLConstants.INTVL_FMTS;
    Map<String, ResourceTable> tableMap = new HashMap<String, ResourceTable>();

    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      Resource newres = null;
      String localxpath = iter.next();
      if (loc.isPathNotConvertible(localxpath)) {
        continue;
      }

      String name = XPPUtil.getXpathName(localxpath);
      if (name.equals(LDMLConstants.SPECIAL)) {
        newres = parseSpecialElements(loc, xpath);
      } else if (name.equals(LDMLConstants.INTVL_FMT_FALL)) {
        newres = new ResourceString(LDMLConstants.FALLBACK, loc.getFile().getStringValue(localxpath));
      } else if (name.equals(LDMLConstants.GREATEST_DIFF)) {
        String parentName = XPPUtil.getXpathName(localxpath, -2);
        String tableName = XPPUtil.getAttributeValue(localxpath, parentName, LDMLConstants.ID);
        // See if we've already created a table for this particular
        // intervalFormatItem.
        ResourceTable table = tableMap.get(tableName);
        if (table == null) {
          // We haven't encountered this one yet, so
          // create a new table and put it into the map.
          table = new ResourceTable();
          table.name = tableName;
          tableMap.put(tableName, table);
          // Update newres to reflect the fact we've created a new
          // table.  This will link the table into the resource chain
          // for the enclosing table.
          newres = table;
        }

        ResourceString str = new ResourceString();
        str.name = XPPUtil.getAttributeValue(localxpath, name, LDMLConstants.ID);
        str.val = loc.getFile().getStringValue(localxpath);

        table.appendContents(str);
      } else {
        log.warning("Unknown item " + localxpath);
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

  private Resource parseCalendar(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;

    boolean writtenAmPm = false;
    boolean writtenDTF = false;
    table.name = XPPUtil.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
    String origXpath = xpath;
    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name;
      return current;
    }

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
      Resource res = null;

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
        log.info(
            "<week > element is deprecated and the data should moved to " + supplementalDataFile);
      } else if (name.equals(LDMLConstants.AM)
              || name.equals(LDMLConstants.PM)) {
        // TODO: figure out the tricky parts .. basically get the
        // missing element from
        // fully resolved locale !
        if (writtenAmPm == false) {
          writtenAmPm = true;
          res = parseAmPm(loc, origXpath); // We feed ampm the original xpath.
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
          Resource temp;

          temp = parseAliasResource(loc,xpath + "/" + LDMLConstants.ALIAS);
          if (temp != null) {
            String dtpPath = ((ResourceAlias)temp).val;
            // need to replace "/DateTimePatterns" = DTP at end with desired type

            ResourceAlias afAlias = new ResourceAlias();
            afAlias.name = LDMLConstants.AVAIL_FMTS;
            afAlias.val = dtpPath.replace(DTP, LDMLConstants.AVAIL_FMTS);
            res = Resource.addAfter(res, afAlias);

            ResourceAlias aaAlias = new ResourceAlias();
            aaAlias.name = LDMLConstants.APPEND_ITEMS;
            aaAlias.val = dtpPath.replace(DTP, LDMLConstants.APPEND_ITEMS);
            res = Resource.addAfter(res, aaAlias);

            ResourceAlias ifAlias = new ResourceAlias();
            ifAlias.name = LDMLConstants.INTVL_FMTS;
            ifAlias.val = dtpPath.replace(DTP, LDMLConstants.INTVL_FMTS);
            res = Resource.addAfter(res, ifAlias);
          } else {
            temp = parseTable(loc,xpath + "/" + LDMLConstants.AVAIL_FMTS,
                              LDMLConstants.DATE_FMT_ITEM, LDMLConstants.ID);
            if (temp != null) {
              temp.name = LDMLConstants.AVAIL_FMTS;
              res = Resource.addAfter(res, temp);
            }

            temp = parseTable(loc,xpath + "/" + LDMLConstants.APPEND_ITEMS,
                              LDMLConstants.APPEND_ITEM, LDMLConstants.REQUEST);
            if (temp != null) {
              temp.name = LDMLConstants.APPEND_ITEMS;
              res = Resource.addAfter(res, temp);
            }

            temp = parseIntervalFormats(loc, xpath);
            if (temp != null) {
              res = Resource.addAfter(res, temp);
            }

          }
        }
      } else if (name.equals(LDMLConstants.SPECIAL)) {
        res = parseSpecialElements(loc, xpath);
      } else if (name.equals(LDMLConstants.FIELDS)) {
        // if the whole thing is an alias
        if ((res = getAliasResource(loc, xpath)) == null) {
          ResourceTable subTable = new ResourceTable();
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
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private Resource parseField(LDML2ICUInputLocale loc, String xpath, String type) {
    ResourceTable table = new ResourceTable();
    Resource current = null;
    table.name = type;
    ResourceString dn = null;
    ResourceTable relative = new ResourceTable();
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

    for (Iterator <String > iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = XPPUtil.getXpathName(xpath);
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }
      Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.RELATIVE)) {
        ResourceString str = new ResourceString();
        str.name = "\"" + loc.getBasicAttributeValue(xpath, LDMLConstants.TYPE) + "\"";
        str.val = loc.getFile().getStringValue(xpath);
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
        dn = new ResourceString();
        dn.name = keyNameMap.get(LDMLConstants.DISPLAY_NAME);
        dn.val = loc.getFile().getStringValue(xpath);
      } else {
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
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

  private Resource parseMonthsAndDays(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;
    String name = XPPUtil.getXpathName(xpath);
    table.name = keyNameMap.get(name);

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name; // months -> monthNames
      return current;
    }

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
      Resource res = null;

      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(loc, xpath);
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
        // if there was an item, resync current.
        if (table.first != null) {
          current = table.first.end();
        }
      } else {
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
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

  private Resource parseContext(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;

    // if the whole collation node is marked draft then
    // don't write anything
    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    String myName = XPPUtil.getXpathName(xpath);
    String resName = myName.substring(0, myName.lastIndexOf("Context"));
    table.name = XPPUtil.getAttributeValue(xpath, myName, LDMLConstants.TYPE);
    if (table.name == null) {
      throw new InternalError("Can't get table name for " + xpath + " / "
              + resName + " / " + LDMLConstants.TYPE);
    }

    // if the whole thing is an alias
    if ((current = getAliasResource(loc, xpath)) != null) {
      current.name = table.name;
      return current;
    }

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
      Resource res = null;
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
        // if there was an item, resync current.
        if (table.first != null) {
          current = table.first.end();
        }
      } else {
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
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

  private Resource parseWidth(LDML2ICUInputLocale loc, String resName, String xpath) {
    log.setStatus(loc.getLocale());
    ResourceArray array = new ResourceArray();
    Resource current = null;
    array.name = XPPUtil.getAttributeValue(xpath, resName + "Width", LDMLConstants.TYPE);

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
      log.info("No vals, exiting " + xpath);
      return null;
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

    if ((resName.equals(LDMLConstants.DAY) && allThings.size() < 7)
        || (resName.equals(LDMLConstants.MONTH) && allThings.size() < 12)) {
      log.error(
          "Could not get full " + resName + " array. ["
          + xpath + "] Only found " + map.size()
          + " items  in target locale (" + allThings.size()
          + " including " + ((defMap != null) ? defMap.size() : 0)
          + " inherited). Skipping.");
      return null;
    }

    if (map.size() > 0) {
      for (int i = 0; i < allThings.size(); i++) {
        String key = Integer.toString(i);
        ResourceString res = new ResourceString();
        res.val = map.get(key);
        if (res.val == null && defMap != null) {
          res.val = defMap.get(key);
          if (verboseFallbackComments && res.val != null) {
            res.smallComment = " fallback";
          }
        }
        if (res.val == null) {
          log.error(
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
      ResourceString res = getDefaultResource(loc, xpath + "/default");
      if (res != null) {
        log.warning("Found def for " + xpath + " - " + res.val);
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

  private Set<String> getSetCompletion(LDML2ICUInputLocale loc, String element, String xpath) {
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
      String type = XPPUtil.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
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

    log.warning("No known completion for " + element);
    return null;
  }

  private Map<String, String> getElementsMap(
      LDML2ICUInputLocale loc, String element, String xpath, boolean fromResolved) {
    Map<String, String> map = new TreeMap<String, String>();
    CLDRFile whichFile;
    if (fromResolved) {
      whichFile = loc.resolved();
    } else {
      whichFile = loc.getFile();
    }

    String origXpath = xpath;
    for (Iterator<String> iter = whichFile.iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      if (loc.isPathNotConvertible(whichFile, xpath)) {
        continue;
      }

      String name = XPPUtil.getXpathName(xpath);
      String val = whichFile.getStringValue(xpath);
      String caltype = XPPUtil.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
      String type = XPPUtil.getAttributeValue(xpath, name, LDMLConstants.TYPE);
      String yeartype = XPPUtil.getAttributeValue(xpath, name, LDMLConstants.YEARTYPE);

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

        log.error(
            "Encountered unknown alias <res:"
            + fromResolved + " - " + xpath + " / " + name
            + "> subelement: " + name);
        System.exit(-1);
      } else {
        log.error(
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
        String name = XPPUtil.getXpathName(xpath);
        String val = whichFile.getStringValue(xpath);
        if (val == null) {
          continue;
        }

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
    }

    return map;
  }

  private Resource parseWeek(String country, String variant, boolean isRoot) {
    Resource ret = null;
    // optimization
    if (variant.length() != 0) {
      return ret;
    }

    StringBuilder xpath = new StringBuilder("//supplementalData/weekData");
    Node root = LDMLUtilities.getNode(supplementalDoc, xpath.toString());
    if (root != null) {
      Resource week = parseWeekend(root, xpath, country, isRoot);
      Resource dte = parseDTE(root, xpath, country, isRoot);
      if (week != null) {
        week.next = dte;
        ret = week;
      } else {
        ret = dte;
      }
    }

    return ret;
  }

  private static int getMillis(String time) {
    String[] strings = time.split(":"); // time is in hh:mm format
    int hours = Integer.parseInt(strings[0]);
    int minutes = Integer.parseInt(strings[1]);
    return (hours * 60  + minutes) * 60 * 1000;
  }

  private Node getVettedNode(
      Node ctx, String node, String attrb, String attrbVal, StringBuilder xpath) {

    int savedLength = xpath.length();
    NodeList list = LDMLUtilities.getNodeList(ctx, node, null, xpath.toString());
    Node ret = null;
    for (int i = 0; i < list.getLength(); i++) {
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

  private Resource parseWeekend(Node root, StringBuilder xpath, String country, boolean isRoot) {
    Node wkendStart = null;
    Node wkendEnd = null;
    if (country.length()>0) {
      wkendStart = getVettedNode(
          root, LDMLConstants.WENDSTART, LDMLConstants.TERRITORIES, country, xpath);
      wkendEnd = getVettedNode(
          root, LDMLConstants.WENDEND, LDMLConstants.TERRITORIES, country, xpath);
    }

    if (wkendEnd != null || wkendStart != null || isRoot) {
      if (wkendStart == null) {
        wkendStart = getVettedNode(
            null, root, LDMLConstants.WENDSTART + "[@territories='001']", xpath, true);
        if (wkendStart == null) {
          log.error("Could not find weekendStart resource.");
        }
      }
      if (wkendEnd == null) {
        wkendEnd = getVettedNode(
            null, root, LDMLConstants.WENDEND + "[@territories='001']", xpath, true);
        if (wkendEnd == null) {
          log.error("Could not find weekendEnd resource.");
        }
      }
    }

    ResourceIntVector wkend = null;
    if (wkendStart != null && wkendEnd != null) {
      try {
        wkend = new ResourceIntVector();
        wkend.name = LDMLConstants.WEEKEND;
        ResourceInt startday = new ResourceInt();
        startday.val = getDayNumberAsString(
            LDMLUtilities.getAttributeValue(wkendStart, LDMLConstants.DAY));
        ResourceInt starttime = new ResourceInt();
        String time = LDMLUtilities.getAttributeValue(wkendStart, LDMLConstants.TIME);
        starttime.val = Integer.toString(getMillis(time == null?"00:00":time));
        ResourceInt endday = new ResourceInt();
        endday.val = getDayNumberAsString(
            LDMLUtilities.getAttributeValue(wkendEnd, LDMLConstants.DAY));
        ResourceInt endtime = new ResourceInt();

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

  private Resource parseDTE(Node root, StringBuilder xpath, String country, boolean isRoot) {
    Node minDays = null;
    Node firstDay = null;
    ResourceIntVector dte = null;

    if (country.length()>0) {
      minDays = getVettedNode(
          root, LDMLConstants.MINDAYS, LDMLConstants.TERRITORIES, country, xpath);
      firstDay = getVettedNode(
          root, LDMLConstants.FIRSTDAY, LDMLConstants.TERRITORIES, country, xpath);
    }

    if (minDays != null || firstDay != null || isRoot) {
      // fetch inherited to complete the resource..
      if (minDays == null) {
        minDays = getVettedNode(
            root, LDMLConstants.MINDAYS, LDMLConstants.TERRITORIES, "001", xpath);
        if (minDays == null) {
          log.error("Could not find minDays resource.");
        }
      }
      if (firstDay == null) {
        firstDay = getVettedNode(
            root, LDMLConstants.FIRSTDAY, LDMLConstants.TERRITORIES, "001", xpath);
        if (firstDay == null) {
          log.error("Could not find firstDay resource.");
        }
      }
    }

    if (minDays != null && firstDay != null) {
      dte = new ResourceIntVector();
      ResourceInt int1 = new ResourceInt();
      int1.val = getDayNumberAsString(LDMLUtilities.getAttributeValue(firstDay, LDMLConstants.DAY));
      ResourceInt int2 = new ResourceInt();
      int2.val = LDMLUtilities.getAttributeValue(minDays, LDMLConstants.COUNT);

      dte.name = DTE;
      dte.first = int1;
      int1.next = int2;
    }

    if ((minDays == null && firstDay != null) || (minDays != null && firstDay == null)) {
      log.warning(
          "Could not find minDays = " + minDays + " or firstDay = " + firstDay
          + " from fullyResolved locale. Not producing the resource. " + xpath.toString());
      return null;
    }

    return dte;
  }

  private Resource parseEras(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;
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

      Resource res = null;
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
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
    }

    if (table.first != null) {
      return table;
    }

    return null;
  }

  private Resource parseEra(LDML2ICUInputLocale loc, String xpath, String name) {
    ResourceArray array = new ResourceArray();
    Resource current = null;
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
    }

    Map<String, String> map = getElementsMap(loc, resName, xpath, false);
    if (map.size() == 0) {
      log.info("No vals, exiting " + xpath);
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
      // log.info("xpath: " + xpath + ", resName: " + resName +
      // " - needs japanese hack.");
      allThings.addAll(nonNarrow.keySet());
    }

    if (map.size() > 0) {
      for (int i = 0; i < allThings.size(); i++) {
        String key = Integer.toString(i);
        ResourceString res = new ResourceString();
        res.val = map.get(key);
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
          log.error(
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

  private boolean isNodeNotConvertible(Node node, StringBuilder xpath) {
    return isNodeNotConvertible(node, xpath, false, false);
  }

  private boolean isNodeNotConvertible(
      Node node, StringBuilder xpath, boolean isCollation, boolean isNodeFromParent) {
    // only deal with leaf nodes!
    // Here we assume that the CLDR files are normalized
    // and that the draft attributes are only on leaf nodes
    if (LDMLUtilities.areChildrenElementNodes(node) && !isCollation) {
      return false;
    }

    if (isNodeFromParent) {
      return false;
    }

    return !xpathListContains(xpath.toString());
  }
  
  public boolean xpathListContains(String xpath) {
    return _xpathList.contains(xpath);
  }

  public Node getVettedNode(
      Document fullyResolvedDoc, Node parent, String childName, StringBuilder xpath,
      boolean ignoreDraft) {

    String ctx = "./" + childName;
    NodeList list = LDMLUtilities.getNodeList(parent, ctx);
    int saveLength = xpath.length();
    Node ret = null;
    if (list == null || list.getLength() < 0) {
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

  private Node getVettedNode(NodeList list, StringBuilder xpath, boolean ignoreDraft) {
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

  private Resource parseAmPm(LDML2ICUInputLocale loc, String xpath) {
    String[] AMPM = {
      LDMLConstants.AM,
      LDMLConstants.PM
    };
    ResourceString[] strs = new ResourceString[AMPM.length];
    String[] paths = new String[AMPM.length];
    Resource first = null;
    int validCount = 0;
    for (int i = 0; i < AMPM.length; i++) {
      strs[i] = new ResourceString();
      first = ResourceString.addAfter(first, strs[i]);
      paths[i] = xpath + "/" + AMPM[i];
      if (!loc.isPathNotConvertible(paths[i])) {
        strs[i].val = loc.getFile().getStringValue(paths[i]);
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
    ResourceArray arr = new ResourceArray();
    arr.name = AM_PM_MARKERS;
    arr.first = first;
    return arr;
  }

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

  private static final Set<String> NUMBER_SYSTEMS =
      new HashSet<String>(Arrays.asList(ValidNumberingSystems));
  // TODO: update to get from supplemental data: <variable id="$numberSystem" type="choice">
  // "arab arabext armn armnlow beng deva ethi fullwide geor grek greklow gujr guru hans " +
  // "hansfin hant hantfin hebr jpan jpanfin knda khmr laoo latn mlym mong mymr orya roman " +
  // "romanlow taml telu thai tibt</variable>"

  private Resource parseDTF(LDML2ICUInputLocale loc, String xpath) {
    log.setStatus(loc.getLocale());

    // TODO change the ICU format to reflect LDML format
    /*
     * The prefered ICU format would be timeFormats{ default{} full{} long{}
     * medium{} short{} .... } dateFormats{ default{} full{} long{} medium{}
     * short{} ..... } dateTimeFormats{ standard{} .... }
     */

    ResourceArray arr = new ResourceArray();
    String[] theArray = dtf_paths;
    arr.name = DTP;
    Resource current = null;
    ResourceString strs[] = new ResourceString[theArray.length];
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
      Resource alias = parseAliasResource(
          loc, xpath + "/" + LDMLConstants.DATE_TIME_FORMATS + "/" + LDMLConstants.ALIAS);
      if (alias != null) {
        alias.name = DTP;
      }
      return alias;
    }

    if (GroupStatus.SPARSE == status) {
      // Now, we have a problem.
      String type = XPPUtil.getAttributeValue(xpath, LDMLConstants.CALENDAR, LDMLConstants.TYPE);
      if (!type.equals("gregorian")) {
        log.info(
            loc.getLocale() + " " + xpath
            + " - some items are missing, attempting fallback from gregorian");
        ResourceString gregstrs[] =
            new ResourceString[theArray.length];
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
      if (loc.getFile().isHere(aPath)) {
        String fullPath = loc.getFile().getFullXPath(aPath);
        xpp.set(fullPath);
        String numbersOverride =
            xpp.findAttributeValue(LDMLConstants.PATTERN, LDMLConstants.NUMBERS);
        if (numbersOverride != null) {
          nsov[i] = numbersOverride;
        }
      }
    }

    // Glue pattern default
    ResourceString res = getDefaultResourceWithFallback(
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
    for (ResourceString str : strs) {
      if (str.val == null) {
        log.error(xpath + " - null value at " + n);
        System.exit(-1);
      }

      if (nsov[n] != null) {
        // We have a numbering system override - output an array containing the override
        ResourceString nso = new ResourceString();
        nso.val = nsov[n];

        ResourceArray nso_array = new ResourceArray();
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

  private Resource parseNumbers(LDML2ICUInputLocale loc, String xpath) {
    Resource current = null, first = null;
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

      Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        if (!loc.getFile().isHere(xpath)) {
          continue;
        }
        res = parseAliasResource(loc, xpath);
        res.name = name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        if (!loc.getFile().isHere(xpath)) {
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
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
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

    if (first != null) {
      return first;
    }

    return null;
  }

  private Resource parseUnits(LDML2ICUInputLocale loc, String tableName, String altValue) {
    String xpath = "//ldml/" + LDMLConstants.UNITS;
    
    ResourceTable unitsTable = new ResourceTable();
    unitsTable.name = tableName;
    Resource current = null;
    Resource first = null;

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

      Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        if (!loc.getFile().isHere(xpath)) {
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
        log.error("Encountered unknown <" + xpath + "> subelement: " + name);
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

  private Resource parseUnit(LDML2ICUInputLocale loc, String xpath, String altValue) {
    Map<String, ResourceTable> tableMap = new HashMap<String, ResourceTable>();
    Resource first = null;
    Resource last = null;

    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      String localxpath = iter.next();
      if (altValue == null && loc.isPathNotConvertible(localxpath)) {
        continue;
      }

      String name = XPPUtil.getXpathName(localxpath);
      if (name.equals(LDMLConstants.UNIT)) {
        log.error("Unknown item " + localxpath);
        continue;
      }

      if (name.equals(LDMLConstants.UNIT_PATTERN)) {
        String currentAltValue = XPPUtil.getAttributeValue(localxpath, LDMLConstants.ALT);
        if (altValue != null) {
          if (currentAltValue == null || !altValue.equals(currentAltValue)) {
            continue;
          }
          // OK
        } else if (altValue == null && currentAltValue != null) {
          continue;
        }

        String parentName = XPPUtil.getXpathName(localxpath, -2);
        String tableName = XPPUtil.getAttributeValue(localxpath, parentName, LDMLConstants.TYPE);
        ResourceTable current = tableMap.get(tableName);
        if (current == null) {
          current = new ResourceTable();
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

        ResourceString str = new ResourceString();
        str.name = XPPUtil.getAttributeValue(localxpath, name, LDMLConstants.COUNT);
        str.val = loc.getFile().getStringValue(localxpath);
        current.appendContents(str);
      } else {
        log.error("Unknown item " + localxpath);
        continue;
      }
    }

    return first;
  }

  enum GroupStatus {
    EMPTY, SPARSE, FALLBACK, COMPLETE
  }

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
      LDML2ICUInputLocale loc, String xpathBase, String xpaths[], ResourceString res[],
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
      if (loc.getFile().isHere(aPath)) {
        anyExtant = true;
        if (!loc.isPathNotConvertible(aPath)) {
          values[i] = loc.getFile().getStringValue(aPath);
          if (values[i] != null) {
            someNonDraft = true;
          }
        }
      }
    }

    GroupStatus status = GroupStatus.EMPTY;
    if (!anyExtant && !someNonDraft) {
      // log.warning("No " + xpathBase + " for " + loc.locale);
      return status;
    }

    if (someNonDraft == true) {
      status = GroupStatus.COMPLETE;
      for (int i = 0; i < xpaths.length; i++) {
        res[i] = new ResourceString();
        String temp = values[i];
        if (temp == null) {
          String aPath = xpathBase + "/" + xpaths[i];
          if (preferNativeNumberSymbols) {
            aPath = reviseAPath(loc, xpp, aPath);
          }
          temp = loc.resolved().getStringValue(aPath);
          if (temp != null) {
            CLDRFile.Status fileStatus = new CLDRFile.Status();
            String foundIn = loc.resolved().getSourceLocaleID(aPath, fileStatus);
            if (verboseFallbackComments) {
              res[i].smallComment = " From " + foundIn;
            }
            if (status != GroupStatus.SPARSE) {
              status = GroupStatus.FALLBACK;
            }
            // log.warning("Fallback from " + foundIn + " in "
            // + loc.locale + " / " + aPath);
          } else {
            log.info("Can't complete array for " + xpathBase + " at " + aPath);
            status = GroupStatus.SPARSE;
          }
        }
        res[i].val = temp;
      }
      return status;
    }

    return GroupStatus.EMPTY;
  }

  private String reviseAPath(LDML2ICUInputLocale loc, XPathParts xpp, String aPath) {
    // This is a clumsy way to do it, but the code for the converter is so convoluted...
    Set<String> paths = loc.getFile().getPaths(aPath, null, null);
    // We have all the paths that match, now. We prefer ones that have an
    // alt value that is a valid number system
    for (String path : paths) {
      String distinguishing = CLDRFile.getDistinguishingXPath(path, null, false);
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

  private Resource parseSymbols(LDML2ICUInputLocale loc, String xpath) {
    ResourceArray arr = new ResourceArray();
    arr.name = NUMBER_ELEMENTS;
    Resource current = null;
    ResourceString strs[] = new ResourceString[sym_paths.length];
    GroupStatus status = parseGroupWithFallback(loc, xpath, sym_paths, strs, !asciiNumbers);
    if (GroupStatus.EMPTY == status || GroupStatus.SPARSE == status) {
      return null;
    }

    for (ResourceString str : strs) {
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

  private Resource parseCurrencyPlurals(LDML2ICUInputLocale loc, String xpath) {
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

    ResourceTable parentTable = new ResourceTable();
    parentTable.name = LDMLConstants.CURRENCY_PLURALS;
    String xpathCurrency = xpath + "/currencies/currency";

    // Since the leaf elements are not grouped by currency in the InputLocale
    // instance, this map will hold the table for each currency.
    Map<String, ResourceTable> tableMap = new HashMap<String, ResourceTable>();

    Resource last = null;
    for (Iterator<String> iter = loc.getFile().iterator(xpathCurrency); iter.hasNext();) {
      String localxpath = iter.next();
      if (loc.isPathNotConvertible(localxpath)) {
        continue;
      }

      String name = XPPUtil.getXpathName(localxpath);
      if (!name.equals(LDMLConstants.DISPLAY_NAME)) {
        continue;
      }

      // We only care about the elements with a "count" attribute.
      String count = XPPUtil.getAttributeValue(localxpath, name, LDMLConstants.COUNT);
      if (count != null) {
        String parentName = XPPUtil.getXpathName(localxpath, -2);
        String tableName = XPPUtil.getAttributeValue(localxpath, parentName, LDMLConstants.TYPE);
        ResourceTable current = tableMap.get(tableName);
        if (current == null) {
          current = new ResourceTable();
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

        ResourceString str = new ResourceString();
        str.name = count;
        str.val = loc.getFile().getStringValue(localxpath);
        current.appendContents(str);
      }
    }

    if (parentTable.first != null) {
      return parentTable;
    }

    return null;
  }

  private Resource parseCurrencyFormatPlurals(LDML2ICUInputLocale loc, String xpath) {
    // This table contains formatting patterns for this locale's currency.
    // Each pattern is represented by a "unitPattern" element in the XML file.
    // Each pattern is represented as a string in the resource format, with
    // the name of the string being the value of the "count" attribute.

    ResourceTable table = new ResourceTable();
    table.name = LDMLConstants.CURRENCY_UNIT_PATTERNS;
    String xpathUnitPattern = xpath + "/currencyFormats/unitPattern";
    for (Iterator<String> iter = loc.getFile().iterator(xpathUnitPattern); iter.hasNext();) {
      String localxpath = iter.next();
      if (loc.isPathNotConvertible(localxpath)) {
        continue;
      }
      String name = XPPUtil.getXpathName(localxpath);
      ResourceString str = new ResourceString();
      str.name = XPPUtil.getAttributeValue(localxpath, name, LDMLConstants.COUNT);
      str.val = loc.getFile().getStringValue(localxpath);
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

  private Resource parseCurrencySpacing(LDML2ICUInputLocale loc, String xpath) {
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

    ResourceTable table = null;
    ResourceTable current = null;
    ResourceTable first = null;
    for (String section : CurrencySections) {
      String xpathUnitPattern = xpath + "/" + LDMLConstants.CURRENCY_FORMATS + "/"
          + LDMLConstants.CURRENCY_SPACING + "/" + section;
      int count = 0;
      for (Iterator<String> iter = loc.getFile().iterator(xpathUnitPattern); iter.hasNext();) {
        String localxpath = iter.next();
        if (loc.isPathNotConvertible(localxpath)) {
          continue;
        }

        if (table == null) {
          table = new ResourceTable();
          table.name = LDMLConstants.CURRENCY_SPACING;
        }

        if (count++ == 0) {
          current = new ResourceTable();
          current.name = section;
          if (first == null) {
            table.first = current;
            first = current;
          } else {
            first.next = current;
          }
        }

        String name = XPPUtil.getXpathName(localxpath);
        ResourceString str = new ResourceString();
        str.name = name;
        str.val = loc.getFile().getStringValue(localxpath);
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

  private Resource parseNumberFormats(LDML2ICUInputLocale loc, String xpath) {
    ResourceArray arr = new ResourceArray();
    String[] theArray = num_paths;
    arr.name = NUMBER_PATTERNS;
    Resource current = null;
    ResourceString strs[] = new ResourceString[theArray.length];
    GroupStatus status = parseGroupWithFallback(loc, "//ldml/numbers", theArray, strs, false);
    if (GroupStatus.EMPTY == status) {
      return null; // NO items were found
    }

    for (ResourceString str : strs) {
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

  private Resource parseCurrencies(LDML2ICUInputLocale loc, String xpath) {
    ResourceTable table = new ResourceTable();
    Resource current = null;

    // if the whole node is marked draft then
    // don't write anything
    String origXpath = xpath;

    // collect a list of all currencies, ensure no dups.
    Set<String> currs = new HashSet<String>();
    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = XPPUtil.getXpathName(xpath);
      if (loc.isPathNotConvertible(xpath)) {
        continue;
      }

      Resource res = null;
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
          // log.warning("$$$ dup " + type);
          continue; // dup
        }

        res = parseCurrency(loc, origXpath + "/currency[@type=\"" + type + "\"]", type);
        currs.add(type);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
        }
        res = null;
      }
    }

    if (table.first != null) {
      // lookup only if needed
      table.name = keyNameMap.get(XPPUtil.getXpathName(origXpath));
      return table;
    }

    return null;
  }

  private static final String curr_syms[] = {
    LDMLConstants.SYMBOL, // 0
    LDMLConstants.DISPLAY_NAME, // 1
    LDMLConstants.PATTERN, // 2
    LDMLConstants.DECIMAL, // 3
    LDMLConstants.GROUP, // 4
  };

  private Resource parseCurrency(LDML2ICUInputLocale loc, String xpath, String type) {
    ResourceArray arr = new ResourceArray();
    arr.name = type;
    ResourceString strs[] = new ResourceString[curr_syms.length];
    GroupStatus status = parseGroupWithFallback(loc, xpath, curr_syms, strs, false);
    if (status == GroupStatus.EMPTY) {
      String full = loc.resolved().getFullXPath(xpath);
      String val = loc.resolved().getStringValue(xpath);
      log.warning(
          "totally empty - Failed to parse: " + type
          + " at xpath " + xpath + " - full value " + full
          + " value " + val);
      return null;
    }

    // aliases - for sanity
    ResourceString symbol = strs[0];
    ResourceString displayName = strs[1];
    ResourceString pattern = strs[2];
    ResourceString decimal = strs[3];
    ResourceString group = strs[4];

    // 0 - symb
    if (symbol.val != null) {
      String choice = loc.getBasicAttributeValue(
          xpath + "/" + curr_syms[0], LDMLConstants.CHOICE);
      if (choice == null) {
        String fullPathInh = loc.resolved().getFullXPath(xpath + "/" + curr_syms[0]);
        if (fullPathInh != null) {
          choice = XPPUtil.getAttributeValue(fullPathInh, LDMLConstants.CHOICE);
        }
      }
      if (choice != null && choice.equals("true") && !loc.isPathNotConvertible(xpath + "/symbol")) {
        symbol.val = "=" + symbol.val.replace('\u2264', '#').replace("&lt;", "<");
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

      ResourceArray elementsArr = new ResourceArray();
      pattern.comment = isPatternDup ? "Duplicated from NumberPatterns resource" : null;
      decimal.comment = isDecimalDup ? "Duplicated from NumberElements resource" : null;
      group.comment = isGroupDup ? "Duplicated from NumberElements resource" : null;

      elementsArr.first = pattern;
      pattern.next = decimal;
      decimal.next = group;
      if (displayName.val != null) {
        displayName.next = elementsArr;
      } else {
        log.warning(
            "displayName and symbol not vetted/available for currency resource "
            + arr.name + " not generating the resource");
      }
    }

    if (arr.first != null) {
      return arr;
    }

    return null;
  }

  /**
   * Shim. Transitions us from CLDRFile based processing to DOM.
   */
  public Resource parseCollations(LDML2ICUInputLocale loc, String xpath) {
    log.setStatus(loc.getLocale());
    if (loc.isNotOnDisk()) {
      // attempt to parse a 'fake' locale.
      Resource first = getAliasResource(loc, xpath);
      if (first != null) {
        first.name =  LDMLConstants.COLLATIONS;
        return first;
      }

      // now, look for a default
      first = getDefaultResource(loc, xpath + "/" + LDMLConstants.DEFAULT, LDMLConstants.DEFAULT);
      if (first != null) {
        ResourceTable table = new ResourceTable();
        table.name = LDMLConstants.COLLATIONS;
        table.first = first;
        return table;
      }

      // now, look for aliases
      Set<String> collTypes = loc.getByType(xpath, LDMLConstants.COLLATION, LDMLConstants.TYPE);
      for(String type : collTypes) {
        Resource res = getAliasResource(
            loc, xpath + "/" + LDMLConstants.COLLATION + "[@type=\"" + type + "\"]");
        if (res != null) {
          first = Resource.addAfter(first, res);
        } else {
          throw new InternalError(
              "FAIL: locale " + loc.getLocale() + " not on disc, and non-alias collation " + type
              + " encountered.");
        }
      }

      return first; // could be null.
    }

    // parse using DOM-based code
    DocumentPair docPair = getDocumentPair(loc);
    Node collations = getTopNode(docPair.doc, LDMLConstants.COLLATIONS);
    if (collations == null) {
      throw new InternalError("Can't get top level collations node");
    }

    Resource table = parseCollations(collations, docPair.fullyResolvedDoc, 
        new StringBuilder("//ldml"), true);
    if (table == null || (table.isEmpty() && table instanceof ResourceTable)) {
      log.warning(" warning: No collations found. Bundle will be empty.");
      return null;
    }

    return table;
  }

  public Resource parseCollations(Node root, Document fullyResolvedDoc, StringBuilder xpath, 
      boolean checkIfConvertible) {
    ResourceTable table = new ResourceTable();
    Resource current = null;
    table.name = root.getNodeName();
    int savedLength = xpath.length();
    getXPath(root, xpath);
    int oldLength = xpath.length();

    // if the whole collation node is marked draft then
    // don't write anything
    if (isNodeNotConvertible(root, xpath)) {
      xpath.setLength(savedLength);
      return null;
    }

    current = table.first = null; // parseValidSubLocales(root, xpath);
    for(Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType()!= Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.DEFAULT)) {
        res = getDefaultResource(node, xpath, name);
      } else if (name.equals(LDMLConstants.COLLATION)) {
        res = parseCollation(node, fullyResolvedDoc, xpath, checkIfConvertible);
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
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

  private Resource parseCollation(Node root, Document fullyResolvedDoc, StringBuilder xpath, 
      boolean checkIfConvertible) {
    ResourceTable table = new ResourceTable();
    Resource current = null;
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

    StringBuilder rules = new StringBuilder();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      Resource res = null;
      if (name.equals(LDMLConstants.ALIAS)) {
        res = parseAliasResource(node, xpath, true);
        res.name = table.name;
        return res;
      }

      if (name.equals(LDMLConstants.RULES)) {
        Node alias = LDMLUtilities.getNode(
            node, LDMLConstants.ALIAS, fullyResolvedDoc, xpath.toString());
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
          log.debug("");
        }
        int index = rules.length();
        rules.append("[suppressContractions ");
        rules.append(LDMLUtilities.getNodeValue(node));
        rules.append(" ]");
        if (DEBUG) {
          log.debug(rules.substring(index));
        }
      } else if (name.equals(LDMLConstants.OPTIMIZE)) {
        rules.append("[optimize ");
        rules.append(LDMLUtilities.getNodeValue(node));
        rules.append(" ]");
      } else if (name.equals(LDMLConstants.BASE)) {
        // TODO Dont know what to do here
        // if (DEBUG)printXPathWarning(node, xpath);
        rules.append(parseBase(node, fullyResolvedDoc, xpath, oldLength));
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
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
      ResourceString str = new ResourceString();
      str.name = LDMLConstants.SEQUENCE;
      str.val = rules.toString();
      if (current == null) {
        current = table.first = str;
      } else {
        current.next = str;
        current = current.next;
      }
      str = new ResourceString();
      str.name = "Version";
      str.val = getLdmlVersion(); // "1.0"
      current.next = str;
    }

    xpath.delete(savedLength, xpath.length());
    if (table.first != null) {
      return table;
    }

    return null;
  }

  private String parseBase(Node node, Document fullyResolvedDoc, StringBuilder xpath, 
      int oldLength) {
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
        ResourceTable table = (ResourceTable) parseCollation(
            col, fullyResolvedDoc, new StringBuilder(myxp), false);
        if (table != null) {
          Resource current = table.first;
          while(current != null) {
            if (current instanceof ResourceString) {
              ResourceString temp = (ResourceString)current;
              if (temp.name.equals(LDMLConstants.SEQUENCE)) {
                return temp.val;
              }
            }
            current = current.next;
          }
        } else {
          log.warning("Locale (" + fn + ") Collation node could not be parsed for " + myxp);
        }
      } else {
        log.warning("Locale (" + fn + ") Could not find col from xpath: " + myxp);
      }
    } else {
      log.warning("Could not find locale from xpath: " + xpath.toString());
    }

    return "";
  }

  private StringBuilder parseSettings(Node node) {
    StringBuilder rules = new StringBuilder();

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

  private static final TreeMap<String, String> collationMap = new TreeMap<String, String>();
  static {
    collationMap.put("first_tertiary_ignorable", "[first tertiary ignorable ]");
    collationMap.put("last_tertiary_ignorable",  "[last tertiary ignorable ]");
    collationMap.put("first_secondary_ignorable", "[first secondary ignorable ]");
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

  private StringBuilder parseRules(Node root, StringBuilder xpath) {
    StringBuilder rules = new StringBuilder();

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
        Node lastVariable = LDMLUtilities.getNode(node, LDMLConstants.LAST_VARIABLE, null, null);
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
        Node lastVariable = LDMLUtilities.getNode(node, LDMLConstants.LAST_VARIABLE, null, null);
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
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
    }

    return rules;
  }

  private static final UnicodeSet needsQuoting = new UnicodeSet(
      "[[:whitespace:][:c:][:z:][[:ascii:]-[a-zA-Z0-9]]]");
  private static StringBuilder quoteOperandBuffer = new StringBuilder(); // faster

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
    StringBuilder data = new StringBuilder();
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
      log.error("Encountered strength: " + name);
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
      log.error("Encountered strength: " + name);
      System.exit(-1);
    }
    return null;
  }

  private StringBuilder parseReset(Node root) {
    /* variableTop   at      & x= [last variable] <reset>x</reset><i><last_variable/></i>
     * after & x < [last variable] <reset>x</reset><p><last_variable/></p>
     * before & [before 1] x< [last variable] <reset before="primary">x</reset>
     * <p><last_variable/></p>
     */
    /*
     * & [first tertiary ignorable] << \u00e1 <reset><first_tertiary_ignorable/></reset><s>?</s>
     */
    StringBuilder ret = new StringBuilder();

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

  private StringBuilder getExpandedRules(String data, String name) {
    UCharacterIterator iter = UCharacterIterator.getInstance(data);
    StringBuilder ret = new StringBuilder();
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

  private StringBuilder parseExtension(Node root) {
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
    StringBuilder rules = new StringBuilder();
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
      } else if (
          name.equals(LDMLConstants.P)
          || name.equals(LDMLConstants.S)
          || name.equals(LDMLConstants.T)
          || name.equals(LDMLConstants.I)) {
        strengthNode = node;
      } else if (name.equals(LDMLConstants.EXTEND)) {
        extendNode = node;
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }
    }

    if (contextNode != null) {
      context = LDMLUtilities.getNodeValue(contextNode);
    }

    if (strengthNode != null) {
      Node lastVariable = LDMLUtilities.getNode(
          strengthNode, LDMLConstants.LAST_VARIABLE, null, null);
      if (lastVariable != null) {
        string = collationMap.get(lastVariable.getNodeName());
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


  private Resource parseBoundaries(Node root, StringBuilder xpath) {
    ResourceTable table = new ResourceTable();
    int savedLength = xpath.length();
    getXPath(root,xpath);
    Resource current = null;
    String name = root.getNodeName();
    table.name = name.substring(name.indexOf(':') + 1, name.length());

    // we don't care if special elements are marked draft or not!

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      name = node.getNodeName();
      Resource res = null;
      if (name.equals(ICU_GRAPHEME)
          || name.equals(ICU_WORD)
          || name.equals(ICU_LINE)
          || name.equals(ICU_SENTENCE)
          || name.equals(ICU_TITLE)
          || name.equals(ICU_XGC)) {
        ResourceProcess str = new ResourceProcess();
        str.ext =  ICUResourceWriter.DEPENDENCY;
        str.name = name.substring(name.indexOf(':') + 1, name.length());
        str.val = LDMLUtilities.getAttributeValue(node, ICU_DEPENDENCY);
        if (str.val != null) {
          res = str;
        }
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        }else{
          current.next = res;
          current = res.end();
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

  private Resource parseDictionaries(Node root, StringBuilder xpath) {
    ResourceTable table = new ResourceTable();
    int savedLength = xpath.length();
    getXPath(root,xpath);
    Resource current = null;
    String name = root.getNodeName();
    table.name = name.substring(name.indexOf(':') + 1, name.length());
    // we don't care if special elements are marked draft or not

    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      name = node.getNodeName();
      Resource res = null;
      if (name.equals(ICU_DICTIONARY)) {
        ResourceProcess str = new ResourceProcess();
        str.ext =  ICUResourceWriter.DEPENDENCY;
        str.name = LDMLUtilities.getAttributeValue(node, LDMLConstants.TYPE);
        str.val = LDMLUtilities.getAttributeValue(node, ICU_DEPENDENCY);
        if (str.val != null) {
          res = str;
        }
      } else {
        log.error("Encountered unknown <" + root.getNodeName() + "> subelement: " + name);
        System.exit(-1);
      }

      if (res != null) {
        if (current == null) {
          table.first = res;
          current = res.end();
        } else {
          current.next = res;
          current = res.end();
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

  private Resource parseSpecialElements(LDML2ICUInputLocale loc, String xpath) {
    Resource current = null;
    Resource first = null;
    String origXpath = xpath;

    for (Iterator<String> iter = loc.getFile().iterator(xpath); iter.hasNext();) {
      xpath = iter.next();
      String name = XPPUtil.getXpathName(xpath);

      log.info("parseSpecial: " + name);
      // we don't care if special elements are marked draft or not

      Resource res = null;
      if (name.equals(ICU_SCRIPT)) {
        if (!loc.beenHere(ICU_SCRIPT)) {
          res = parseArray(loc, "//ldml/characters/special/icu:scripts");
          res.name = LOCALE_SCRIPT;
        }
      } else if (name.equals(ICU_UCARULES)) {
        ResourceProcess process = new ResourceProcess();
        process.name = "UCARules";
        process.ext = "uca_rules";
        process.val = loc.findAttributeValue(xpath,ICU_UCA_RULES);
        res = process;
      } else if (name.equals(ICU_DEPENDS)) {
        ResourceProcess process = new ResourceProcess();
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
        log.error("Encountered unknown <" + xpath + "> special subelement: " + name);
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

  public static class DocumentPair {
    public final Document doc;
    public final Document fullyResolvedDoc;
    DocumentPair(Document doc, Document fullyResolvedDoc) {
      this.doc = doc;
      this.fullyResolvedDoc = fullyResolvedDoc;
    }
  }
  
  /**
   * Use the input locale as a cache for the document pair.
   */
  private DocumentPair getDocumentPair(LDML2ICUInputLocale loc) {
    if (loc.isNotOnDisk()) {
      throw new InternalError(
          "Error: locale (" + loc.getLocale() + ") isn't on disk, can't parse with DOM.");
    }

    DocumentPair result = loc.getDocumentPair();
    if (result == null) {
      result = getDocumentPair(loc.getLocale());
      loc.setDocumentPair(result);
    }
    return result;
  }
  
  /**
   * Get the node of the 'top' item named. Similar to DOM-based parseBundle()
   */
  public Node getTopNode(Document doc, String topName) {
    StringBuilder xpath = new StringBuilder();
    xpath.append("//ldml");

    Node ldml = null;

    for (ldml = doc.getFirstChild(); ldml != null; ldml = ldml.getNextSibling()) {
      if (ldml.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      
      String name = ldml.getNodeName();
      if (name.equals(LDMLConstants.LDML)) {
        setLdmlVersion(LDMLUtilities.getAttributeValue(ldml, LDMLConstants.VERSION));
        break;
      }
    }

    if (ldml == null) {
      throw new RuntimeException("ERROR: no <ldml> node found in parseBundle()");
    }

    for (Node node = ldml.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      String name = node.getNodeName();
      if (topName.equals(name)) {
        return node;
      }
    }

    return null;
  }

  private Resource parseBrkItrData(LDML2ICUInputLocale loc, String xpath) {
    // "//ldml/special/"
    if (loc.beenHere(ICU_BRKITR_DATA)) {
      return null;
    }

    DocumentPair docPair = getDocumentPair(loc);
    Node root = getTopNode(docPair.doc, LDMLConstants.SPECIAL);
    StringBuilder xpathBuffer = new StringBuilder();
    getXPath(root,xpathBuffer);
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      if (name.equals(ICU_BRKITR_DATA)) {
        return parseBrkItrData(node, xpathBuffer);
      }
    }
    
    throw new InternalError("Could not find node for " + ICU_BRKITR_DATA);
  }

  private Resource parseBrkItrData(Node root, StringBuilder xpath) {
    Resource current = null, first = null;
    int savedLength = xpath.length();
    getXPath(root, xpath);

    // we don't care if special elements are marked draft or not!
    
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      String name = node.getNodeName();
      Resource res = null;
      if (name.equals(ICU_BOUNDARIES)) {
        res = parseBoundaries(node, xpath);
      } else if (name.equals(ICU_DICTIONARIES)) {
        res = parseDictionaries(node, xpath);
      } else {
        log.error(
            "Encountered @ " + xpath + "  unknown <" + root.getNodeName() + "> subelement: "
            + name);
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

      xpath.delete(savedLength, xpath.length());
    }

    return first;
  }

  private Resource parseDefaultNumberingSystem(LDML2ICUInputLocale loc, String xpath) {
    ResourceString str = new ResourceString();
    str.name = LDMLConstants.DEFAULT_NUMBERING_SYSTEM;
    str.val = loc.getFile().getStringValue(xpath);
    if (str.val != null) {
      return str;
    }

    return null;
  }

  private Resource parseRBNF(LDML2ICUInputLocale loc, String xpath) {
    char LARROW = 0x2190;
    char RARROW = 0x2192;

    ResourceTable table = new ResourceTable();
    table.name = "RBNFRules";

    Resource current = null;
    Resource res = null;
    ResourceArray ruleset = new ResourceArray();

    if (loc.isPathNotConvertible(xpath)) {
      return null;
    }

    String currentRulesetGrouping = "";
    String currentRulesetType = "";

    for (Iterator<String> iter = loc.getFile().iterator(xpath, CLDRFile.ldmlComparator);
         iter.hasNext();) {
      String aPath = iter.next();
      String fullPath = loc.getFile().getFullXPath(aPath);
      String name = XPPUtil.getXpathName(aPath);
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

          ruleset = new ResourceArray();
          ruleset.name = rulesetGrouping;
          currentRulesetGrouping = rulesetGrouping;
          currentRulesetType = "";
        }

        if (!rulesetType.equals(currentRulesetType)) {
          ResourceString rsname = new ResourceString();
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

        ResourceString rs = new ResourceString();
        if (rulesetType.equals(LDMLConstants.LENIENT_PARSE)) {
          rs.val = Utility.escape(
              loc.getFile().getStringValue(aPath).replace(LARROW, '<').replace(RARROW, '>'));
        } else {
          rs.val = ruleValue + radixString + decExpString + ": " + Utility.escape(
              loc.getFile().getStringValue(aPath).replace(LARROW, '<').replace(RARROW, '>'));
        }
        ruleset.appendContents(rs);
      } else {
        log.error("Unknown element found: " + xpath + " / " + name);
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

  public boolean isDraftStatusOverridable(String locName) {
    if (getLocalesMap() != null && getLocalesMap().size() > 0) {
      String draft = getLocalesMap().get(locName + ".xml");
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
  
  // utility
  public static StringBuilder getXPath(Node node, StringBuilder xpath) {
    xpath.append("/");
    xpath.append(node.getNodeName());
    LDMLUtilities.appendXPathAttribute(node, xpath);
    return xpath;
  }
}
