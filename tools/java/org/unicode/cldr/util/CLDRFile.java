/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.XPathParts.Comments;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import org.unicode.cldr.icu.CollectionUtilities;

import org.unicode.cldr.test.DateTimePatternGenerator;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;

//import javax.xml.parsers.*;

/**
 * This is a class that represents the contents of a CLDR file, as <key,value> pairs,
 * where the key is a "cleaned" xpath (with non-distinguishing attributes removed),
 * and the value is an object that contains the full
 * xpath plus a value, which is a string, or a node (the latter for atomic elements).
 * <p><b>WARNING: The API on this class is likely to change.</b> Having the full xpath on the value is clumsy;
 * I need to change it to having the key be an object that contains the full xpath, but then sorts as if
 * it were clean.
 * <p>Each instance also contains a set of associated comments for each xpath.
 * @author medavis
 */
/*
 Notes:
 http://xml.apache.org/xerces2-j/faq-grammars.html#faq-3
 http://developers.sun.com/dev/coolstuff/xml/readme.html
 http://lists.xml.org/archives/xml-dev/200007/msg00284.html
 http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/DTDHandler.html
 */
public class CLDRFile implements Freezable, Iterable<String> {
  public   static final Pattern ALT_PROPOSED_PATTERN = Pattern.compile(".*\\[@alt=\"[^\"]*proposed[^\"]*\"].*");

  private static boolean LOG_PROGRESS = false;
  
  public static boolean HACK_ORDER = false;
  private static boolean DEBUG_LOGGING = false;
  private static boolean SHOW_ALIAS_FIXES = false;
  
  public static final String SUPPLEMENTAL_NAME = "supplementalData";
  public static final String SUPPLEMENTAL_METADATA = "supplementalMetadata";
  public static final String SUPPLEMENTAL_PREFIX = "supplemental";
  public static final String GEN_VERSION = "1.7";
  
  private boolean locked;
  private XMLSource dataSource;
  private String dtdVersion;

  private File alternateSupplementalDirectory;
  
  public enum DraftStatus {unconfirmed, provisional, contributed, approved};
  
  public static class SimpleXMLSource extends XMLSource {
    private HashMap xpath_value = new HashMap(); // TODO change to HashMap, once comparator is gone
    private HashMap xpath_fullXPath = new HashMap();
    private Comments xpath_comments = new Comments(); // map from paths to comments.
    private Factory factory; // for now, fix later
    public DraftStatus madeWithMinimalDraftStatus = DraftStatus.unconfirmed;
    
    public SimpleXMLSource(Factory factory, String localeID) {
      this.factory = factory;
      this.setLocaleID(localeID);
    }
    /** 
     * Create a shallow, locked copy of another XMLSource. 
     * @param copyAsLockedFrom
     */
    protected SimpleXMLSource(SimpleXMLSource copyAsLockedFrom) {
        this.xpath_value = copyAsLockedFrom.xpath_value;
        this.xpath_fullXPath = copyAsLockedFrom.xpath_fullXPath;
        this.xpath_comments = copyAsLockedFrom.xpath_comments;
        this.factory = copyAsLockedFrom.factory;
        this.madeWithMinimalDraftStatus = copyAsLockedFrom.madeWithMinimalDraftStatus;
        this.setLocaleID(copyAsLockedFrom.getLocaleID());
        locked=true;
    }
    public String getValueAtDPath(String xpath) {
      return (String)xpath_value.get(xpath);
    }
    public File getSupplementalDirectory() {
      final File result = new File(factory.getSourceDirectory(), "../supplemental/");
      if (result.isDirectory()) {
        return result;
      }
      return new File(Utility.DEFAULT_SUPPLEMENTAL_DIRECTORY);
    }
    public String getFullPathAtDPath(String xpath) {
      String result = (String) xpath_fullXPath.get(xpath);
      if (result != null) return result;
      if (xpath_value.get(xpath) != null) return xpath; // we don't store duplicates
      //System.err.println("WARNING: "+getLocaleID()+": path not present in data: " + xpath);
      //return xpath;
      return null; // throw new IllegalArgumentException("Path not present in data: " + xpath);
    }
    public Comments getXpathComments() {
      return xpath_comments;
    }
    public void setXpathComments(Comments xpath_comments) {
      this.xpath_comments = xpath_comments;
    }
//  public void putPathValue(String xpath, String value) {
//  if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
//  String distinguishingXPath = CLDRFile.getDistinguishingXPath(xpath, fixedPath);	
//  xpath_value.put(distinguishingXPath, value);
//  if (!fixedPath[0].equals(distinguishingXPath)) {
//  xpath_fullXPath.put(distinguishingXPath, fixedPath[0]);
//  }
//  }
    public void removeValueAtDPath(String distinguishingXPath) {
      xpath_value.remove(distinguishingXPath);
      xpath_fullXPath.remove(distinguishingXPath);
    }
    public Iterator<String> iterator() { // must be unmodifiable or locked
      return Collections.unmodifiableSet(xpath_value.keySet()).iterator();
    }
    public Object freeze() {
      locked = true;
      return this;
    }
    public Object cloneAsThawed() {
      SimpleXMLSource result = (SimpleXMLSource) super.cloneAsThawed();
      result.xpath_comments = (Comments) result.xpath_comments.clone();
      result.xpath_fullXPath = (HashMap) result.xpath_fullXPath.clone();
      result.xpath_value = (HashMap) result.xpath_value.clone();
      return result;
    }
    public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
      xpath_fullXPath.put(distinguishingXPath, fullxpath);
    }
    public void putValueAtDPath(String distinguishingXPath, String value) {
      xpath_value.put(distinguishingXPath, value);
    }
    public XMLSource make(String localeID) {
      if (localeID == null) return null;
      CLDRFile file = factory.make(localeID, false, madeWithMinimalDraftStatus);
      if (file == null) return null;
      return file.dataSource;
    }
    public Set getAvailableLocales() {
      return factory.getAvailable();
    }
  }
  
  
  
  public String toString() {
    return "{"
    + "locked=" + locked 
    + " locale=" + dataSource.getLocaleID() 
    + " dataSource=" + dataSource.toString()
    + "}";
  }
  
  public String toString(String regex) {
    return "{"
    + "locked=" + locked 
    + " locale=" + dataSource.getLocaleID() 
    + " regex=" + regex 
    + " dataSource=" + dataSource.toString(regex)
    + "}";
  }
  
  // for refactoring
  
  public CLDRFile setNonInheriting(boolean isSupplemental) {
    if (locked) {
      throw new UnsupportedOperationException("Attempt to modify locked object");
    }
    dataSource.setNonInheriting(isSupplemental);
    return this;
  }
  
  public boolean isNonInheriting() {
    return dataSource.isNonInheriting();
  }
  
  /**
   * Construct a new CLDRFile.  
   * @param dataSource if null, an empty SimpleXMLSource will be used.
   * @param resolved
   */
  public CLDRFile(XMLSource dataSource, boolean resolved){
    if (dataSource == null) dataSource = new SimpleXMLSource(null, null);
    if (resolved && !dataSource.isResolving()) {
      dataSource = dataSource.getResolving();
    }
    if (!resolved && dataSource.isResolving()) {
      throw new IllegalArgumentException("Can't create unresolved file from resolved one");
    }
    this.dataSource = dataSource;
    //source.xpath_value = isSupplemental ? new TreeMap() : new TreeMap(ldmlComparator);
  }
  
  /**
   * Create a CLDRFile for the given localename. (Normally a Factory is used to create CLDRFiles.)
   * SimpleXMLSource will be used as the source.
   * @param localeName
   */
  public static CLDRFile make(String localeName) {
    CLDRFile result = new CLDRFile(null, false);
    result.dataSource.setLocaleID(localeName);
    return result;
  }
  
  /**
   * Create a CLDRFile for the given localename. (Normally a Factory is used to create CLDRFiles.)
   * @param localeName
   */
  public static CLDRFile makeSupplemental(String localeName) {
    CLDRFile result = new CLDRFile(null, false);
    result.dataSource.setLocaleID(localeName);
    result.setNonInheriting(true);
    return result;
  }
  
  /**
   * Produce a CLDRFile from a localeName and filename, given a directory. (Normally a Factory is used to create CLDRFiles.)
   * @param localeName
   * @param dir directory 
   */
  public static CLDRFile make(String localeName, String dir, boolean includeDraft) {
    return make(localeName, dir, includeDraft ? DraftStatus.unconfirmed : DraftStatus.approved);
  }
  
  public static CLDRFile make(String localeName, String dir, DraftStatus minimalDraftStatus) {
    return makeFromFile(dir + File.separator + localeName + ".xml", localeName, minimalDraftStatus);
  }
  
  /**
   * Produce a CLDRFile from a localeName, given a directory. (Normally a Factory is used to create CLDRFiles.)
   * @param localeName
   * @param dir directory 
   */
  // TODO make the directory a URL  
  public static CLDRFile makeFromFile(String fullFileName, String localeName, DraftStatus minimalDraftStatus) {
    return make(localeName).loadFromFile(fullFileName, localeName, minimalDraftStatus);
  }
  /**
   * Produce a CLDRFile from a localeName, given a directory. (Normally a Factory is used to create CLDRFiles.)
   * @param localeName
   * @param dir directory 
   */
  public CLDRFile loadFromFile(File f, String localeName, DraftStatus minimalDraftStatus) {
    String fullFileName = f.getAbsolutePath();
    try {
      fullFileName = f.getCanonicalPath();
      if (DEBUG_LOGGING) {
        System.out.println("Parsing: " + fullFileName);
        Log.logln(LOG_PROGRESS, "Parsing: " + fullFileName);
      }
      FileInputStream fis = new FileInputStream(f);
      load(fullFileName, localeName, fis, minimalDraftStatus);
      fis.close();
      return this;
    } catch (Exception e) {
      //e.printStackTrace();
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + fullFileName).initCause(e);
    }
  }
  public CLDRFile loadFromFile(String fullFileName, String localeName, DraftStatus minimalDraftStatus) {
      return loadFromFile(new File(fullFileName), localeName, minimalDraftStatus);
  }
  
  /**
   * Produce a CLDRFile from a file input stream. (Normally a Factory is used to create CLDRFiles.)
   * @param localeName
   * @param fis
   */
  public static CLDRFile make(String fileName, String localeName, InputStream fis, DraftStatus minimalDraftStatus) {
      return make(localeName).load(fileName,localeName, fis, minimalDraftStatus);
  }
  /**
   * Load a CLDRFile from a file input stream.
   * @param localeName
   * @param fis
   */
  public CLDRFile load(String fileName, String localeName, InputStream fis, DraftStatus minimalDraftStatus) {
    try {
      fis = new StripUTF8BOMInputStream(fis);
      MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler(this, minimalDraftStatus);
      
      // now fill it.
      
      XMLReader xmlReader = createXMLReader(true);
      xmlReader.setContentHandler(DEFAULT_DECLHANDLER);
      xmlReader.setErrorHandler(DEFAULT_DECLHANDLER);
      xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", DEFAULT_DECLHANDLER);
      xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", DEFAULT_DECLHANDLER);
      InputSource is = new InputSource(fis);
      is.setSystemId(fileName);
      xmlReader.parse(is);
      if (DEFAULT_DECLHANDLER.isSupplemental < 0) {
        throw new IllegalArgumentException("root of file must be either ldml or supplementalData");
      }
      this.setNonInheriting(DEFAULT_DECLHANDLER.isSupplemental > 0);
      return this;
    } catch (SAXParseException e) {
      System.out.println(CLDRFile.showSAX(e));
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
    } catch (SAXException e) {
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
    } catch (IOException e) {
      throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
    }    	 
  }
  
  /**
   * Clone the object. Produces unlocked version (see Lockable).
   */
  public Object cloneAsThawed() {
    try {
      CLDRFile result = (CLDRFile) super.clone();
      result.locked = false;
      result.dataSource = (XMLSource)result.dataSource.cloneAsThawed();
      return result;
    } catch (CloneNotSupportedException e) {
      throw new InternalError("should never happen");
    }
  }
  
  /**
   * Prints the contents of the file (the xpaths/values) to the console.
   *
   */
  public CLDRFile show() {
    for (Iterator it2 = iterator(); it2.hasNext();) {
      String xpath = (String)it2.next();
      System.out.println(getFullXPath(xpath) + " =>\t" + getStringValue(xpath));
    }
    return this;
  }
  
  /**
   * Write the corresponding XML file out, with the normal formatting and indentation.
   * Will update the identity element, including generation, version, and other items.
   */
  public CLDRFile write(PrintWriter pw) {
    Set orderedSet = new TreeSet(ldmlComparator);
    CollectionUtilities.addAll(dataSource.iterator(), orderedSet);
    
    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    pw.println("<!DOCTYPE " 
        + (isNonInheriting() ? "supplementalData" : "ldml")  
        + " SYSTEM \"http://www.unicode.org/cldr/dtd/" + GEN_VERSION + "/ldml" 
        + (isNonInheriting() ? "Supplemental" : "")
        + ".dtd\">");
    /*
     <identity>
     <version number="1.2"/>
     <generation date="2004-08-27"/>
     <language type="en"/>
     */
    // if ldml has any attributes, get them.
    Set identitySet = new TreeSet(ldmlComparator);
    if (isNonInheriting()) {
      //identitySet.add("//supplementalData[@version=\"" + GEN_VERSION + "\"]/version[@number=\"$Revision$\"]");
      //identitySet.add("//supplementalData[@version=\"" + GEN_VERSION + "\"]/generation[@date=\"$Date$\"]");
    } else {
      String ldml_identity = "//ldml/identity";
      if (orderedSet.size() > 0) {
        String firstPath = (String) orderedSet.iterator().next();
        //Value firstValue = (Value) getXpath_value().get(firstPath);
        String firstFullPath = getFullXPath(firstPath);
        XPathParts parts = new XPathParts(null,null).set(firstFullPath);
        if (firstFullPath.indexOf("/identity") >= 0) {
          ldml_identity = parts.toString(2);
        } else {
          ldml_identity = parts.toString(1) + "/identity";			
        }
      }
      
      identitySet.add(ldml_identity + "/version[@number=\"$Revision$\"]");
      identitySet.add(ldml_identity + "/generation[@date=\"$Date$\"]");
      LocaleIDParser lip = new LocaleIDParser();
      lip.set(dataSource.getLocaleID());
      identitySet.add(ldml_identity + "/language[@type=\"" + lip.getLanguage() + "\"]");
      if (lip.getScript().length() != 0) {
        identitySet.add(ldml_identity + "/script[@type=\"" + lip.getScript() + "\"]");
      }
      if (lip.getRegion().length() != 0) {
        identitySet.add(ldml_identity + "/territory[@type=\"" + lip.getRegion() + "\"]");
      }
      String[] variants = lip.getVariants();
      for (int i = 0; i < variants.length; ++i) {
        identitySet.add(ldml_identity + "/variant[@type=\"" + variants[i] + "\"]");
      }
    }
    // now do the rest
    
    XPathParts.writeComment(pw, 0, dataSource.getXpathComments().getInitialComment(), false);
    
    XPathParts.Comments tempComments = (XPathParts.Comments) dataSource.getXpathComments().clone();
    
    MapComparator modAttComp = attributeOrdering;
    if (HACK_ORDER) modAttComp = new MapComparator()
    .add("alt").add("draft").add(modAttComp.getOrder());
    
    XPathParts last = new XPathParts(attributeOrdering, defaultSuppressionMap);
    XPathParts current = new XPathParts(attributeOrdering, defaultSuppressionMap);
    XPathParts lastFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
    XPathParts currentFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
    
    for (Iterator it2 = identitySet.iterator(); it2.hasNext();) {
      String xpath = (String)it2.next();
      currentFiltered.set(xpath);
      current.set(xpath);
      current.writeDifference(pw, currentFiltered, last, lastFiltered, "", tempComments);
      // exchange pairs of parts
      XPathParts temp = current;
      current = last;
      last = temp;
      temp = currentFiltered;
      currentFiltered = lastFiltered;
      lastFiltered = temp;
    }
    
    for (Iterator it2 = orderedSet.iterator(); it2.hasNext();) {
      String xpath = (String)it2.next();
      //Value v = (Value) getXpath_value().get(xpath);
      currentFiltered.set(xpath);
      if (currentFiltered.getElement(1).equals("identity")) continue;
      current.set(getFullXPath(xpath));
      current.writeDifference(pw, currentFiltered, last, lastFiltered, getStringValue(xpath), tempComments);
      // exchange pairs of parts
      XPathParts temp = current;
      current = last;
      last = temp;
      temp = currentFiltered;
      currentFiltered = lastFiltered;
      lastFiltered = temp;
    }
    current.clear().writeDifference(pw, null, last, lastFiltered, null, tempComments);
    String finalComment = dataSource.getXpathComments().getFinalComment();
    
    // write comments that no longer have a base
    List x = tempComments.extractCommentsWithoutBase();
    if (x.size() != 0) {
      String extras = "Comments without bases" + XPathParts.NEWLINE;
      for (Iterator it = x.iterator(); it.hasNext();) {
        String key = (String) it.next();
        //Log.logln("Writing extra comment: " + key);
        extras += XPathParts.NEWLINE + key;
      }
      finalComment += XPathParts.NEWLINE + extras;
    }
    XPathParts.writeComment(pw, 0, finalComment, true);
    return this;
  }
  
  /**
   * Get a string value from an xpath.
   */
  public String getStringValue(String xpath) {
    String result = dataSource.getValueAtPath(xpath);
    if (result == null && dataSource.isResolving()) {
      final String fallbackPath = getFallbackPath(xpath, false);
      if (fallbackPath != null) {
        result = dataSource.getValueAtPath(fallbackPath);
      }
    }
    return result;
  }

  /**
   * Only call if xpath doesn't exist in the current file.
   * <p>For now, just handle counts: see getCountPath
   * @param xpath
   * @param winning TODO
   * @return
   */
  private String getFallbackPath(String xpath, boolean winning) {
    //  || xpath.contains("/currency") && xpath.contains("/displayName")
    if (xpath.contains("[@count=")) {
      return getCountPathWithFallback(xpath, Count.other, winning);
    }
    return null;
  }
  
  /**
   * Get the full path from a distinguished path
   */
  public String getFullXPath(String xpath) {
    String result = dataSource.getFullPath(xpath);
    if (result == null && dataSource.isResolving()) {
      String fallback = getFallbackPath(xpath, true);
      if (fallback != null) {
        // TODO, add attributes from fallback into main
        result = xpath;
      }
    }
    return result;
  }
  
  /**
   * Find out where the value was found (for resolving locales). Returns code-fallback as the location if nothing is found
   * @param distinguishedXPath path (must be distinguished!)
   * @param status the distinguished path where the item was found. Pass in null if you don't care.
   */
  public String getSourceLocaleID(String distinguishedXPath, CLDRFile.Status status) {
    String result = dataSource.getSourceLocaleID(distinguishedXPath, status);
    if (result == XMLSource.CODE_FALLBACK_ID && dataSource.isResolving()) {
      final String fallbackPath = getFallbackPath(distinguishedXPath, false);
      if (fallbackPath != null && !fallbackPath.equals(distinguishedXPath)) {
        result = dataSource.getSourceLocaleID(fallbackPath, status);
//        if (status != null && status.pathWhereFound.equals(distinguishedXPath)) {
//          status.pathWhereFound = fallbackPath;
//        }
      }
    }
    return result;
  }
  
  /**
   * return true if the path in this file (without resolution)
   * @param path
   * @return
   */
  public boolean isHere(String path) {
    return dataSource.isHere(path);
  }
  
  /**
   * Add a new element to a CLDRFile.
   * @param currentFullXPath
   * @param value
   */
  public CLDRFile add(String currentFullXPath, String value) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    //StringValue v = new StringValue(value, currentFullXPath);
    Log.logln(LOG_PROGRESS, "ADDING: \t" + currentFullXPath + " \t" + value + "\t" + currentFullXPath);
    //xpath = xpath.intern();
    try {
      dataSource.putValueAtPath(currentFullXPath, value);
    } catch (RuntimeException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("failed adding " + currentFullXPath + ",\t" + value).initCause(e);
    }
    return this;
  }
  
  public CLDRFile addComment(String xpath, String comment, int type) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    // System.out.println("Adding comment: <" + xpath + "> '" + comment + "'");
    Log.logln(LOG_PROGRESS, "ADDING Comment: \t" + type + "\t" + xpath + " \t" + comment);
    if (xpath == null || xpath.length() == 0) {
      dataSource.getXpathComments().setFinalComment(
          Utility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(), XPathParts.NEWLINE, comment));
    } else {
      xpath = getDistinguishingXPath(xpath, null, false);
      dataSource.getXpathComments().addComment(type, xpath, comment);
    }
    return this;
  }
  
  static final public int 
  MERGE_KEEP_MINE = 0, 
  MERGE_REPLACE_MINE = 1, 
  MERGE_ADD_ALTERNATE = 2, 
  MERGE_REPLACE_MY_DRAFT = 3;
  /**
   * Merges elements from another CLDR file. Note: when both have the same xpath key, 
   * the keepMine determines whether "my" values are kept
   * or the other files values are kept.
   * @param other
   * @param keepMine if true, keep my values in case of conflict; otherwise keep the other's values.
   */
  public CLDRFile putAll(CLDRFile other, int conflict_resolution) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    XPathParts parts = new XPathParts(null, null);
    if (conflict_resolution == MERGE_KEEP_MINE) {
      Map temp = isNonInheriting() ? new TreeMap() : new TreeMap(ldmlComparator);
      dataSource.putAll(other.dataSource, MERGE_KEEP_MINE);
    } else if (conflict_resolution == MERGE_REPLACE_MINE) {
      dataSource.putAll(other.dataSource, MERGE_REPLACE_MINE);
    } else if (conflict_resolution == MERGE_REPLACE_MY_DRAFT) {
      // first find all my alt=..proposed items
      Set hasDraftVersion = new HashSet();
      for (Iterator it = dataSource.iterator(); it.hasNext();) {
        String cpath = (String) it.next();
        String fullpath = getFullXPath(cpath);
        if (fullpath.indexOf("[@draft") >= 0) {
          hasDraftVersion.add(getNondraftNonaltXPath(cpath)); // strips the alt and the draft
        }
      }
      // only replace draft items!
      // this is either an item with draft in the fullpath
      // or an item with draft and alt in the full path
      for (Iterator it = other.iterator(); it.hasNext();) {
        String cpath = (String) it.next();
        //Value otherValueOld = (Value) other.getXpath_value().get(cpath);
        // fix the data
        //cpath = Utility.replace(cpath, "[@type=\"ZZ\"]", "[@type=\"QO\"]"); // fix because tag meaning changed after beta
        cpath = getNondraftNonaltXPath(cpath);
        String newValue = other.getStringValue(cpath);
        String newFullPath = getNondraftNonaltXPath(other.getFullXPath(cpath));
        // newFullPath = Utility.replace(newFullPath, "[@type=\"ZZ\"]", "[@type=\"QO\"]");
        // another hack; need to add references back in
        newFullPath = addReferencesIfNeeded(newFullPath, getFullXPath(cpath));
        //Value otherValue = new StringValue(newValue, newFullPath);
        
        if (!hasDraftVersion.contains(cpath)) {
          if (cpath.startsWith("//ldml/identity/")) continue; // skip, since the error msg is not needed.
          String myVersion = getStringValue(cpath);
          if (myVersion == null || !newValue.equals(myVersion)) {
            Log.logln(getLocaleID() + "\tDenied attempt to replace non-draft" + Utility.LINE_SEPARATOR + "\tcurr: [" + cpath + ",\t"
                + myVersion + "]" + Utility.LINE_SEPARATOR + "\twith: [" + newValue + "]");
            continue;
          }
        }
        Log.logln(getLocaleID() + "\tVETTED: [" + newFullPath + ",\t" + newValue + "]");
        dataSource.putValueAtPath(newFullPath, newValue);
      }
    } else if (conflict_resolution == MERGE_ADD_ALTERNATE){
      for (Iterator it = other.iterator(); it.hasNext();) {
        String key = (String) it.next();
        String otherValue = other.getStringValue(key);
        String myValue = dataSource.getValueAtPath(key);
        if (myValue == null) {
          dataSource.putValueAtPath(other.getFullXPath(key), otherValue);
        } else if (!(myValue.equals(otherValue) 
            && equalsIgnoringDraft(getFullXPath(key), other.getFullXPath(key)))
            && !key.startsWith("//ldml/identity")){
          for (int i = 0; ; ++i) {
            String prop = "proposed" + (i == 0 ? "" : String.valueOf(i));
            String fullPath = parts.set(other.getFullXPath(key)).addAttribute("alt", prop).toString();
            String path = getDistinguishingXPath(fullPath, null, false);
            if (dataSource.getValueAtPath(path) != null) continue;
            dataSource.putValueAtPath(fullPath, otherValue);
            break;
          }
        }
      }
    } else throw new IllegalArgumentException("Illegal operand: " + conflict_resolution);
    
    dataSource.getXpathComments().setInitialComment(
        Utility.joinWithSeparation(dataSource.getXpathComments().getInitialComment(),
            XPathParts.NEWLINE, 
            other.dataSource.getXpathComments().getInitialComment()));
    dataSource.getXpathComments().setFinalComment(
        Utility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(), 
            XPathParts.NEWLINE, 
            other.dataSource.getXpathComments().getFinalComment()));
    dataSource.getXpathComments().joinAll(other.dataSource.getXpathComments());
    /*
     *     private Map xpath_value;
     private String initialComment = "";
     private String finalComment = "";
     private String key;
     private XPathParts.Comments xpath_comments = new XPathParts.Comments(); // map from paths to comments.
     private boolean isSupplemental;
     
     */
    return this;
  }
  
  /**
   * 
   */
  private String addReferencesIfNeeded(String newFullPath, String fullXPath) {
    if (fullXPath == null || fullXPath.indexOf("[@references=") < 0) return newFullPath;
    XPathParts parts = new XPathParts(null, null).set(fullXPath);
    String accummulatedReferences = null;
    for (int i = 0; i < parts.size(); ++i) {
      Map attributes = parts.getAttributes(i);
      String references = (String) attributes.get("references");
      if (references == null) continue;
      if (accummulatedReferences == null) accummulatedReferences = references;
      else accummulatedReferences += ", " + references; 
    }
    if (accummulatedReferences == null) return newFullPath;
    XPathParts newParts = new XPathParts(null, null).set(newFullPath);
    Map attributes = newParts.getAttributes(newParts.size()-1);
    String references = (String) attributes.get("references");
    if (references == null) references = accummulatedReferences;
    else references += ", " + accummulatedReferences;
    attributes.put("references", references);
    System.out.println("Changing " + newFullPath + " plus " + fullXPath + " to " + newParts.toString());
    return newParts.toString();
  }
  
  /**
   * Removes an element from a CLDRFile.
   */
  public CLDRFile remove(String xpath) {
    remove(xpath, false);
    return this;
  }
  
  /**
   * Removes an element from a CLDRFile.
   */
  public CLDRFile remove(String xpath, boolean butComment) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    if (butComment) {
      //CLDRFile.Value v = getValue(xpath);
      appendFinalComment(dataSource.getFullPath(xpath)+ "::<" + dataSource.getValueAtPath(xpath) + ">");
    }
    dataSource.removeValueAtPath(xpath);
    return this;
  }
  
  /**
   * Removes all xpaths from a CLDRFile.
   */
  public CLDRFile removeAll(Set xpaths, boolean butComment) {
    if (butComment) appendFinalComment("Illegal attributes removed:");
    for (Iterator it = xpaths.iterator(); it.hasNext();) {
      remove((String) it.next(), butComment);
    }
    return this;
  }
  
  /**
   * Code should explicitly include CODE_FALLBACK
   */
  public static final Pattern specialsToKeep = Pattern.compile(
      "/(" +
      "measurementSystemName" +
      "|codePattern" +
      "|calendar\\[\\@type\\=\"[^\"]*\"\\]/(?!dateTimeFormats/appendItems)" + // gregorian
      "|numbers/symbols/(decimal/group)" +
      "|timeZoneNames/(hourFormat|gmtFormat|regionFormat)" +
      "|pattern" +
      ")");

  static public final Pattern specialsToPushFromRoot = Pattern.compile(
      "/(" +
      "calendar\\[\\@type\\=\"gregorian\"\\]/" +
        "(?!fields)" +
        "(?!dateTimeFormats/appendItems)" +
        "(?!.*\\[@type=\"format\"].*\\[@type=\"narrow\"])" +
        "(?!.*\\[@type=\"stand-alone\"].*\\[@type=\"(abbreviated|wide)\"])" +
      "|numbers/symbols/(decimal/group)" +
      "|timeZoneNames/(hourFormat|gmtFormat|regionFormat)" +
      ")");

  private static final boolean MINIMIZE_ALT_PROPOSED = false;

  /**
   * Removes all items with same value
   * @param keepIfMatches TODO
   * @param keepList TODO
   */
  public CLDRFile removeDuplicates(CLDRFile other, boolean butComment, boolean dontRemoveSpecials, Predicate keepIfMatches) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    //Matcher specialPathMatcher = dontRemoveSpecials ? specialsToKeep.matcher("") : null;
    boolean first = true;
    List<String> toRemove = new ArrayList();
    for (Iterator it = iterator(); it.hasNext();) { // see what items we have that the other also has
      String xpath = (String)it.next();
      String currentValue = dataSource.getValueAtPath(xpath);
      //if (currentValue == null) continue;
      String otherXpath = xpath;
      String otherValue = other.dataSource.getValueAtPath(otherXpath);
      if (!currentValue.equals(otherValue)) {
        if (MINIMIZE_ALT_PROPOSED) {
          otherXpath = CLDRFile.getNondraftNonaltXPath(xpath);
          if (otherXpath.equals(xpath)) {
            continue;
          }
          otherValue = other.dataSource.getValueAtPath(otherXpath);
          if (!currentValue.equals(otherValue)) {
            continue;
          }
        } else {
          continue;
        }
      }
      if (dontRemoveSpecials) {
        String keepValue = (String) XMLSource.getPathsAllowingDuplicates().get(xpath);
        if (keepValue != null && keepValue.equals(currentValue)) {
          continue;
        }
        if (keepIfMatches.is(xpath)) { // skip certain xpaths
          continue;
        }
      }
      
      // we've now established that the values are the same for the 
      String currentFullXPath = dataSource.getFullPath(xpath);
      String otherFullXPath = other.dataSource.getFullPath(otherXpath);
      if (!equalsIgnoringDraft(currentFullXPath, otherFullXPath)) continue;
      if (first) {
        first = false;
        if (butComment) appendFinalComment("Duplicates removed:");
      }
      // we can't remove right away, since that disturbs the iterator.
      toRemove.add(xpath);
      //remove(xpath, butComment);
    }
    // now remove them safely
    for (String xpath : toRemove) {
      remove(xpath, butComment);
    }
    return this;
  }
  
  public CLDRFile putRoot(CLDRFile rootFile) {
    Matcher specialPathMatcher = specialsToPushFromRoot.matcher("");
    XPathParts parts = new XPathParts(attributeOrdering, defaultSuppressionMap);
    for (Iterator it = rootFile.iterator(); it.hasNext();) {
      String xpath = (String)it.next();
      
      // skip aliases, choices
      if (xpath.contains("/alias")) continue;
      if (xpath.contains("/default")) continue;
      
      // skip values we have
      String currentValue = dataSource.getValueAtPath(xpath);      
      if (currentValue != null) continue;
      
      // only copy specials
      if (!specialPathMatcher.reset(xpath).find()) { // skip certain xpaths
        continue;
      }
      // now add the value
      String otherValue = rootFile.dataSource.getValueAtPath(xpath);
      String otherFullXPath = rootFile.dataSource.getFullPath(xpath);
      if (!otherFullXPath.contains("[@draft")) {
        parts.set(otherFullXPath);
        Map attributes = parts.getAttributes(-1);
        attributes.put("draft","unconfirmed");
        otherFullXPath = parts.toString();
      }

      add(otherFullXPath,otherValue);
    }
    return this;
  }
  
  /**
   * @return Returns the finalComment.
   */
  public String getFinalComment() {
    return dataSource.getXpathComments().getFinalComment();
  }
  /**
   * @return Returns the finalComment.
   */
  public String getInitialComment() {
    return dataSource.getXpathComments().getInitialComment();
  }
  /**
   * @return Returns the xpath_comments. Cloned for safety.
   */
  public XPathParts.Comments getXpath_comments() {
    return (XPathParts.Comments) dataSource.getXpathComments().clone();
  }
  /**
   * @return Returns the locale ID. In the case of a supplemental data file, it is SUPPLEMENTAL_NAME.
   */
  public String getLocaleID() {
    return dataSource.getLocaleID();
  }
  
  /**
   * @see com.ibm.icu.util.Freezable#isFrozen()
   */
  public synchronized boolean isFrozen() {
    return locked;
  }
  
  /**
   * @see com.ibm.icu.util.Freezable#freeze()
   */
  public synchronized Object freeze() {
    locked = true;
    dataSource.freeze();
    return this;
  }
  
  public CLDRFile clearComments() {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    dataSource.setXpathComments(new XPathParts.Comments());
    return this;
  }
  /**
   * Sets a final comment, replacing everything that was there.
   */
  public CLDRFile setFinalComment(String comment) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    dataSource.getXpathComments().setFinalComment(comment);
    return this;
  }
  
  /**
   * Adds a comment to the final list of comments.
   */
  public CLDRFile appendFinalComment(String comment) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    dataSource.getXpathComments().setFinalComment(Utility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(), XPathParts.NEWLINE, comment));
    return this;
  }
  
  /**
   * Sets the initial comment, replacing everything that was there.
   */
  public CLDRFile setInitialComment(String comment) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    dataSource.getXpathComments().setInitialComment(comment);
    return this;
  }
  
  // ========== STATIC UTILITIES ==========
  
  /**
   * Utility to restrict to files matching a given regular expression. The expression does not contain ".xml".
   * Note that supplementalData is always skipped, and root is always included.
   */
  public static Set<String> getMatchingXMLFiles(String sourceDir, Matcher m) {
    Set<String> s = new TreeSet();
    File[] files = new File(sourceDir).listFiles();
    for (int i = 0; i < files.length; ++i) {
      String name = files[i].getName();
      if (!name.endsWith(".xml")) continue;
      //if (name.startsWith(SUPPLEMENTAL_NAME)) continue;
      String locale = name.substring(0,name.length()-4); // drop .xml
      if (!m.reset(locale).matches()) continue;
      s.add(locale);
    }
    return s;
  }
  
  /**
   * Returns a collection containing the keys for this file.
   */
//public Set keySet() {
//return (Set) CollectionUtilities.addAll(dataSource.iterator(), new HashSet());
//}
  
  public Iterator<String> iterator() {
    return dataSource.iterator();
  }
  
  public Iterator<String> iterator(String prefix) {
    return dataSource.iterator(prefix);
  }
  
  public Iterator<String> iterator(Matcher pathFilter) {
    return dataSource.iterator(pathFilter);
  }
  
  public Iterator iterator(String prefix, Comparator comparator) {
    Iterator it = (prefix == null || prefix.length() == 0) 
    ? dataSource.iterator() 
        : dataSource.iterator(prefix);
    if (comparator == null) return it;
    Set orderedSet = new TreeSet(comparator);
    CollectionUtilities.addAll(it, orderedSet);
    return orderedSet.iterator();
  }
  
  
  public static String getDistinguishingXPath(String xpath, String[] normalizedPath, boolean nonInheriting) {
    return distinguishedXPath.getDistinguishingXPath(xpath, normalizedPath, nonInheriting);
  }
  
  private static boolean equalsIgnoringDraft(String path1, String path2) {
    if (path1 == path2) {
      return true;
    }
    if (path1 == null || path2 == null) {
      return false;
    }
    // TODO: optimize
    if (path1.indexOf("[@draft=") < 0 && path2.indexOf("[@draft=") < 0) return path1.equals(path2);
    return getNondraftNonaltXPath(path1).equals(getNondraftNonaltXPath(path2));
  }
  
  static XPathParts nondraftParts = new XPathParts(null,null);
  
  public static String getNondraftNonaltXPath(String xpath) {
    if (xpath.indexOf("draft=\"") < 0 && xpath.indexOf("alt=\"") < 0 ) return xpath;
    synchronized (nondraftParts) {
      XPathParts parts = new XPathParts(null,null).set(xpath);
      String restore;
      for (int i = 0; i < parts.size(); ++i) {
        String element = parts.getElement(i);
        Map attributes = parts.getAttributes(i);
        restore = null;
        for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
          String attribute = (String) it.next();
          if (attribute.equals("draft")) it.remove();
          else if (attribute.equals("alt")) {
            String value = (String) attributes.get(attribute);		
            int proposedPos = value.indexOf("proposed");
            if (proposedPos >= 0) {
              it.remove();
              if (proposedPos > 0) {
                restore = value.substring(0, proposedPos-1); // is of form xxx-proposedyyy
              }
            }
          }
        }
        if (restore != null) attributes.put("alt", restore);
      }
      return parts.toString();
    }
  }
  
//private static String getNondraftXPath(String xpath) {
//if (xpath.indexOf("draft=\"") < 0) return xpath;
//synchronized (nondraftParts) {
//XPathParts parts = new XPathParts(null,null).set(xpath);
//for (int i = 0; i < parts.size(); ++i) {
//Map attributes = parts.getAttributes(i);
//for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
//String attribute = (String) it.next();
//if (attribute.equals("draft")) it.remove();
//}
//}
//return parts.toString();
//}
//}
  
  /**
   * Determine if an attribute is a distinguishing attribute.
   * @param elementName
   * @param attribute
   * @return
   */
  public static boolean isDistinguishing(String elementName, String attribute) {
    boolean result =
      attribute.equals("key") 
      || attribute.equals("request") 
      || attribute.equals("count") 
      || attribute.equals("id") 
      || attribute.equals("_q") 
      || attribute.equals("registry") 
      || attribute.equals("alt")
      || attribute.equals("iso4217")
      || attribute.equals("iso3166")
      || attribute.equals("mzone")
      || attribute.equals("from")
      || attribute.equals("to")
      || attribute.equals("value")
      || (attribute.equals("type") 
          && !elementName.equals("default") 
          && !elementName.equals("measurementSystem") 
          && !elementName.equals("mapping")
          && !elementName.equals("abbreviationFallback")
          && !elementName.equals("preferenceOrdering"))
          || elementName.equals("deprecatedItems");
//  if (result != matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true)) {
//  matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true);
//  throw new IllegalArgumentException("Failed: " + elementName + ", " + attribute);
//  }
    return result;
  }
  
  /**
   * Utility to create a validating XML reader.
   */
  public static XMLReader createXMLReader(boolean validating) {
    String[] testList = {
        "org.apache.xerces.parsers.SAXParser",
        "org.apache.crimson.parser.XMLReaderImpl",
        "gnu.xml.aelfred2.XmlReader",
        "com.bluecast.xml.Piccolo",
        "oracle.xml.parser.v2.SAXParser",
        ""
    };
    XMLReader result = null;
    for (int i = 0; i < testList.length; ++i) {
      try {
        result = (testList[i].length() != 0) 
        ? XMLReaderFactory.createXMLReader(testList[i])
            : XMLReaderFactory.createXMLReader();
        result.setFeature("http://xml.org/sax/features/validation", validating);
        break;
      } catch (SAXException e1) {}
    }
    if (result == null) throw new NoClassDefFoundError("No SAX parser is available, or unable to set validation correctly");
    try {
      result.setEntityResolver(new CachingEntityResolver());
    } catch (Throwable e) {
      System.err
      .println("WARNING: Can't set caching entity resolver  -  error "
          + e.toString());
      e.printStackTrace();
    }
    return result;
  }
  
  /**
   * A factory is the normal method to produce a set of CLDRFiles from a directory of XML files.
   * See SimpleFactory for a concrete subclass.
   */
  public static abstract class Factory {
    
    private File alternateSupplementalDirectory = null;

    public abstract String getSourceDirectory();

    protected abstract CLDRFile handleMake(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus);
    
    public CLDRFile make(String localeID, boolean resolved, DraftStatus madeWithMinimalDraftStatus) {
      return handleMake(localeID, resolved, madeWithMinimalDraftStatus).setAlternateSupplementalDirectory(alternateSupplementalDirectory);
    }

    public CLDRFile make(String localeID, boolean resolved, boolean includeDraft) {
      return make(localeID, resolved, includeDraft ? DraftStatus.unconfirmed : DraftStatus.approved);
    }


    public CLDRFile make(String localeID, boolean resolved) {
        return make(localeID, resolved, getMinimalDraftStatus());
      }

    protected abstract DraftStatus getMinimalDraftStatus();

    /**
     * Convenience static
     * @param path
     * @param string
     * @return
     */
    public static Factory make(String path, String string) {
        return SimpleFactory.make(path,string);
    }

    /**
     * Convenience static
     * @param mainDirectory
     * @param string
     * @param approved
     * @return
     */
    public static Factory make(String mainDirectory, String string, DraftStatus approved) {
        return SimpleFactory.make(mainDirectory, string, approved);
    }

    /**
     * Get a set of the available locales for the factory.
     */
    public Set<String> getAvailable() {
      return Collections.unmodifiableSet(handleGetAvailable());
    }
    protected abstract Set<String> handleGetAvailable();

    /**
     * Get a set of the available language locales (according to isLanguage).
     */
    public Set<String> getAvailableLanguages() {
      Set<String> result = new TreeSet();
      for (Iterator<String> it = handleGetAvailable().iterator(); it.hasNext();) {
        String s = it.next();
        if (XPathParts.isLanguage(s)) result.add(s);
      }
      return result;
    }
    
    /**
     * Get a set of the locales that have the given parent (according to isSubLocale())
     * @param isProper if false, then parent itself will match
     */
    public Set getAvailableWithParent(String parent, boolean isProper) {
      Set result = new TreeSet();
      
      for (Iterator it = handleGetAvailable().iterator(); it.hasNext();) {
        String s = (String) it.next();
        int relation = XPathParts.isSubLocale(parent, s);
        if (relation >= 0 && !(isProper && relation == 0)) result.add(s);
      }
      return result;
    }

    public File getAlternateSupplementalDirectory() {
      return alternateSupplementalDirectory;
    }

    public Factory setAlternateSupplementalDirectory(File alternateSupplementalDirectory) {
      this.alternateSupplementalDirectory = alternateSupplementalDirectory;
      return this;
    }

  }
  
  public static class SimpleFactory extends Factory {
    private String sourceDirectory;
    private String matchString;
    private Set<String> localeList = new TreeSet<String>();
    private Map<String,CLDRFile>[] mainCache = new Map[DraftStatus.values().length];
    private Map<String,CLDRFile>[] resolvedCache = new Map[DraftStatus.values().length];
    {
      for (int i = 0; i < mainCache.length; ++i) {
        mainCache[i] = new TreeMap();
        resolvedCache[i] = new TreeMap();
      }
    }
    //private Map mainCacheNoDraft = new TreeMap();
    //private Map resolvedCacheNoDraft = new TreeMap();  
    private Map supplementalCache = new TreeMap();
    private DraftStatus minimalDraftStatus = DraftStatus.unconfirmed;
    private SimpleFactory() {}		
    
    protected DraftStatus getMinimalDraftStatus() {
        return minimalDraftStatus;
    }
    
    /**
     * Create a factory from a source directory, matchingString, and an optional log file.
     * For the matchString meaning, see {@link getMatchingXMLFiles}
     */
    public static Factory make(String sourceDirectory, String matchString) {
      return make(sourceDirectory, matchString, DraftStatus.unconfirmed);
    }
    
    public static Factory make(String sourceDirectory, String matchString, DraftStatus minimalDraftStatus) {
      SimpleFactory result = new SimpleFactory();
      result.sourceDirectory = sourceDirectory;
      result.matchString = matchString;
      result.minimalDraftStatus = minimalDraftStatus;
      Matcher m = Pattern.compile(matchString).matcher("");
      result.localeList = getMatchingXMLFiles(sourceDirectory, m);
//      try {
//        result.localeList.addAll(getMatchingXMLFiles(sourceDirectory + "/../supplemental/", m));
//      } catch(Throwable t) {
//        throw new Error("CLDRFile unable to load Supplemental data: couldn't getMatchingXMLFiles("+sourceDirectory + "/../supplemental"+")",t);
//      }
      return result;
    }
    
    
    protected Set<String> handleGetAvailable() {
        return localeList;
    }
    
    
    private boolean needToReadRoot = true;
    
    /**
     * Make a CLDR file. The result is a locked file, so that it can be cached. If you want to modify it,
     * use clone().
     */
    // TODO resolve aliases
    
    public CLDRFile handleMake(String localeName, boolean resolved, DraftStatus minimalDraftStatus) {
      // TODO fix hack: 
      // read root first so that we get the ordering right.
      /*			if (needToReadRoot) {
       if (!localeName.equals("root")) make("root", false);
       needToReadRoot = false;
       }
       */			// end of hack
      Map<String,CLDRFile> cache = resolved ? resolvedCache[minimalDraftStatus.ordinal()] : mainCache[minimalDraftStatus.ordinal()];

      CLDRFile result = cache.get(localeName);
      if (result == null) {
        final String dir = isSupplementalName(localeName) ? sourceDirectory.replace("incoming/vetted/","common/") + File.separator + "../supplemental/" : sourceDirectory;
        result = CLDRFile.make(localeName, dir, minimalDraftStatus);
        SimpleXMLSource mySource = (SimpleXMLSource)result.dataSource;
        mySource.factory = this;
        mySource.madeWithMinimalDraftStatus = minimalDraftStatus;
        if (resolved) {
          result.dataSource = result.dataSource.getResolving();
        } else {
          result.freeze();	    			
        }
        cache.put(localeName, result);
      }
      return result;
    }

    public String getSourceDirectory() {
      return sourceDirectory;
    }
    
  }
  
  public File getSupplementalDirectory() {
      return alternateSupplementalDirectory != null ? alternateSupplementalDirectory : dataSource.getSupplementalDirectory();
  }

  public File getAlternateSupplementalDirectory() {
    return alternateSupplementalDirectory;
  }

  public CLDRFile setAlternateSupplementalDirectory(File alternateSupplementalDirectory) {
    this.alternateSupplementalDirectory = alternateSupplementalDirectory;
    return this;
  }

  /**
   * Convenience function to return a list of XML files in the Supplemental directory.
   * @return all files ending in ".xml"
   * @see #getSupplementalDirectory()
   */
  public File[] getSupplementalXMLFiles() {
      return getSupplementalDirectory().listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
              return name.endsWith(".xml");
          }} );
  }
  
  /**
   * Convenience function to return a specific supplemental file
   * @param filename the file to return
   * @return the file (may not exist)
   * @see #getSupplementalDirectory()
   */
  public File getSupplementalFile(String filename) {
      return new File(getSupplementalDirectory(), filename);
  }
  
  public static boolean isSupplementalName(String localeName) {
    return localeName.startsWith(SUPPLEMENTAL_PREFIX) || localeName.equals("characters");
  }
  
//static String[] keys = {"calendar", "collation", "currency"};
//
//static String[] calendar_keys = {"buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil", "japanese"};
//static String[] collation_keys = {"phonebook", "traditional", "direct", "pinyin", "stroke", "posix", "big5han", "gb2312han"};
  
  
  /*    *//**
   * Value that contains a node. WARNING: this is not done yet, and may change.
   * In particular, we don't want to return a Node, since that is mutable, and makes caching unsafe!!
   *//*
   static public class NodeValue extends Value {
   private Node nodeValue;
   *//**
   * Creation. WARNING, may change.
   * @param value
   * @param currentFullXPath
   *//*
   public NodeValue(Node value, String currentFullXPath) {
   super(currentFullXPath);
   this.nodeValue = value;
   }
   *//**
   * boilerplate
   *//*
   public boolean hasSameValue(Object other) {
   if (super.hasSameValue(other)) return false;
   return nodeValue.equals(((NodeValue)other).nodeValue);
   }
   *//**
   * boilerplate
   *//*
   public String getStringValue() {
   return nodeValue.toString();
   }
   (non-Javadoc)
   * @see org.unicode.cldr.util.CLDRFile.Value#changePath(java.lang.String)
   
   public Value changePath(String string) {
   return new NodeValue(nodeValue, string);
   }
   }*/
  
  private static class MyDeclHandler implements DeclHandler, ContentHandler, LexicalHandler, ErrorHandler {
    private static UnicodeSet whitespace = new UnicodeSet("[:whitespace:]");
    private DraftStatus minimalDraftStatus;
    private static final boolean SHOW_START_END = false;
    private int commentStack;
    private boolean justPopped = false;
    private String lastChars = "";
    //private String currentXPath = "/";
    private String currentFullXPath = "/";
    private String comment = null;
    private Map attributeOrder = new TreeMap(attributeOrdering);
    private CLDRFile target;
    private String lastActiveLeafNode;
    private String lastLeafNode;
    private int  isSupplemental = -1;
    private int orderedCounter;
    
    MyDeclHandler(CLDRFile target, DraftStatus minimalDraftStatus) {
      this.target = target;
      this.minimalDraftStatus = minimalDraftStatus;
      //attributeOrder = new TreeMap(attributeOrdering);
    }
    
    private String show(Attributes attributes) {
      if (attributes == null) return "null";
      String result = "";
      for (int i = 0; i < attributes.getLength(); ++i) {    			
        String attribute = attributes.getQName(i);
        String value = attributes.getValue(i);
        result += "[@" + attribute + "=\"" + value + "\"]"; // TODO quote the value??
      }
      return result;
    }
    
    private void push(String qName, Attributes attributes) {
      //SHOW_ALL && 
      Log.logln(LOG_PROGRESS, "push\t" + qName + "\t" + show(attributes));
      if (lastChars.length() != 0) {
        if (whitespace.containsAll(lastChars)) lastChars = "";
        else throw new IllegalArgumentException("Must not have mixed content: " + qName + ", " + show(attributes) + ", Content: " + lastChars);
      }
      //currentXPath += "/" + qName;
      currentFullXPath += "/" + qName;
      //if (!isSupplemental) ldmlComparator.addElement(qName);
      if (orderedElements.contains(qName)) {
        currentFullXPath += "[@_q=\"" + (orderedCounter++) + "\"]";
      }
      if (attributes.getLength() > 0) {
        attributeOrder.clear();
        for (int i = 0; i < attributes.getLength(); ++i) {    			
          String attribute = attributes.getQName(i);
          String value = attributes.getValue(i);
          
          //if (!isSupplemental) ldmlComparator.addAttribute(attribute); // must do BEFORE put
          //ldmlComparator.addValue(value);
          // special fix to remove version
          if (attribute.equals("version") && (qName.equals("ldml") || qName.equals("supplementalData"))) {
            target.dtdVersion = value;
          } else {
            putAndFixDeprecatedAttribute(qName, attribute, value);
          }
        }
        for (Iterator it = attributeOrder.keySet().iterator(); it.hasNext();) {
          String attribute = (String)it.next();
          String value = (String)attributeOrder.get(attribute);
          String both = "[@" + attribute + "=\"" + value + "\"]"; // TODO quote the value??
          currentFullXPath += both;
          // distinguishing = key, registry, alt, and type (except for the type attribute on the elements default and mapping).
          //if (isDistinguishing(qName, attribute)) {
          //	currentXPath += both;
          //}
        }
      }
      if (comment != null) {
        target.addComment(currentFullXPath, comment, XPathParts.Comments.PREBLOCK);
        comment = null;
      }
      justPopped = false;
      lastActiveLeafNode = null;
      Log.logln(LOG_PROGRESS, "currentFullXPath\t" + currentFullXPath);
    }
    
    private void putAndFixDeprecatedAttribute(String element, String attribute, String value) {
      if (attribute.equals("draft")) {
        if (value.equals("true")) value = "approved";
        else if (value.equals("false")) value = "unconfirmed";
      } else if (attribute.equals("type")) {
        if (changedTypes.contains(element)&&isSupplemental<1) {  // measurementSystem for example did not change from 'type' to 'choice'.
          attribute = "choice";
        }
      } 
//      else if (element.equals("dateFormatItem")) {
//        if (attribute.equals("id")) {
//          String newValue = dateGenerator.getBaseSkeleton(value);
//          if (!fixedSkeletons.contains(newValue)) {
//            fixedSkeletons.add(newValue);
//            if (!value.equals(newValue)) {
//              System.out.println(value + " => " + newValue);
//            }
//            value = newValue;
//          }
//        }
//      }
      attributeOrder.put(attribute, value);
    }
    
    private Set<String> fixedSkeletons = new HashSet();
    
    private DateTimePatternGenerator dateGenerator = DateTimePatternGenerator.getEmptyInstance();
    
    /**
     * Types which changed from 'type' to 'choice', but not in supplemental data.
     */
    private static Set changedTypes = new HashSet(Arrays.asList(new String[]{
        "abbreviationFallback",
        "default", "mapping", "measurementSystem", "preferenceOrdering"}));
    
    static final Pattern draftPattern = Pattern.compile("\\[@draft=\"([^\"]*)\"\\]");
    Matcher draftMatcher = draftPattern.matcher("");
    
    private void pop(String qName) {
      Log.logln(LOG_PROGRESS, "pop\t" + qName);
      if (lastChars.length() != 0 || justPopped == false) {
        boolean acceptItem = minimalDraftStatus == DraftStatus.unconfirmed;
        if (!acceptItem) {
          if (draftMatcher.reset(currentFullXPath).find()) {
            DraftStatus foundStatus = DraftStatus.valueOf(draftMatcher.group(1));
            if (minimalDraftStatus.compareTo(foundStatus) <= 0) {
              // what we found is greater than or equal to our status
              acceptItem = true;
            }
          } else {
            acceptItem = true; // if not found, then the draft status is approved, so it is always ok
          }
        }
        if (acceptItem) {
          if (false && currentFullXPath.indexOf("i-klingon") >= 0) {
            System.out.println(currentFullXPath);
          }
          String former = target.getStringValue(currentFullXPath);
          if (former != null) {
            String formerPath = target.getFullXPath(currentFullXPath);
            if (!former.equals(lastChars) || !currentFullXPath.equals(formerPath)) {
              warnOnOverride(former, formerPath);
            }
          }
          target.add(currentFullXPath, lastChars);
          lastLeafNode = lastActiveLeafNode = currentFullXPath;
        }
        lastChars = "";
      } else {
        Log.logln(LOG_PROGRESS && lastActiveLeafNode != null, "pop: zeroing last leafNode: " + lastActiveLeafNode);
        lastActiveLeafNode = null;
        if (comment != null) {
          target.addComment(lastLeafNode, comment, XPathParts.Comments.POSTBLOCK);
          comment = null;
        }
      }
      //currentXPath = stripAfter(currentXPath, qName);
      currentFullXPath = stripAfter(currentFullXPath, qName);    
      justPopped = true;
    }

    private void warnOnOverride(String former, String formerPath) {
      System.out.println("\tWARNING: overriding " + target.getLocaleID() + "\t<" + former + ">\t\t" + formerPath + 
          Utility.LINE_SEPARATOR + "\twith " + target.getLocaleID() + "\t<" + lastChars + ">\t" + (currentFullXPath.equals(formerPath) ? "" : currentFullXPath));
    }
    
    private static String stripAfter(String input, String qName) {
      int pos = findLastSlash(input);
      if (qName != null) {
        //assert input.substring(pos+1).startsWith(qName);
        if (!input.substring(pos+1).startsWith(qName)) {
          throw new IllegalArgumentException("Internal Error: should never get here.");
        }
      }
      return input.substring(0,pos);
    }
    
    private static int findLastSlash(String input) {
      int braceStack = 0;
      for (int i = input.length()-1; i >= 0; --i) {
        char ch = input.charAt(i);
        switch(ch) {
          case '/': if (braceStack == 0) return i; break;
          case '[': --braceStack; break;
          case ']': ++braceStack; break;
        }
      }
      return -1;
    }
    
    // SAX items we need to catch
    
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes)
    throws SAXException {
      Log.logln(LOG_PROGRESS || SHOW_START_END, "startElement uri\t" + uri
          + "\tlocalName " + localName
          + "\tqName " + qName
          + "\tattributes " + show(attributes)
      );
      try {
        if (isSupplemental < 0) { // set by first element
          if (qName.equals("ldml")) isSupplemental = 0;
          else if (qName.equals("supplementalData")) isSupplemental = 1;
          else throw new IllegalArgumentException("File is neither ldml or supplementalData!");
        }
        push(qName, attributes);                    
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }
    public void endElement(String uri, String localName, String qName)
    throws SAXException {
      Log.logln(LOG_PROGRESS || SHOW_START_END, "endElement uri\t" + uri + "\tlocalName " + localName
          + "\tqName " + qName);
      try {
        pop(qName);
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }
    
    static final char XML_LINESEPARATOR = (char)0xA;
    static final String XML_LINESEPARATOR_STRING = String.valueOf(XML_LINESEPARATOR);
    
    public void characters(char[] ch, int start, int length)
    throws SAXException {
      try {
        String value = new String(ch,start,length);
        Log.logln(LOG_PROGRESS, "characters:\t" + value);
        while (value.startsWith(XML_LINESEPARATOR_STRING) && lastChars.length() == 0) {
          value = value.substring(1);
        }
        if (value.indexOf(XML_LINESEPARATOR) >= 0) value = value.replace(XML_LINESEPARATOR, '\u0020');
        lastChars += value;
        justPopped = false;
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }
    
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
      Log.logln(LOG_PROGRESS, "startDTD name: " + name
          + ", publicId: " + publicId
          + ", systemId: " + systemId
      );
      commentStack++;
    }
    public void endDTD() throws SAXException {
      Log.logln(LOG_PROGRESS, "endDTD");
      commentStack--;
    }
    
    public void comment(char[] ch, int start, int length) throws SAXException {
      Log.logln(LOG_PROGRESS, commentStack + " comment " + new String(ch, start,length));
      try {
        if (commentStack != 0) return;
        String comment0 = new String(ch, start,length);
        if (lastActiveLeafNode != null) {
          target.addComment(lastActiveLeafNode, comment0, XPathParts.Comments.LINE);
        } else {
          comment = (comment == null ? comment0 : comment + XPathParts.NEWLINE + comment0);
        }
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }
    
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      Log.logln(LOG_PROGRESS, "ignorableWhitespace length: " + length);
      for (int i = 0; i < ch.length; ++i) {
        if (ch[i] == '\n') {
          Log.logln(LOG_PROGRESS && lastActiveLeafNode != null, "\\n: zeroing last leafNode: " + lastActiveLeafNode);
          lastActiveLeafNode = null;
        }
      }
    }
    public void startDocument() throws SAXException {
      Log.logln(LOG_PROGRESS, "startDocument");
      commentStack = 0; // initialize
    }
    
    public void endDocument() throws SAXException {
      Log.logln(LOG_PROGRESS, "endDocument");
      try {
        if (comment != null) target.addComment(null, comment, XPathParts.Comments.LINE);
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }
    
    // ==== The following are just for debuggin =====
    
    public void elementDecl(String name, String model) throws SAXException {
      Log.logln(LOG_PROGRESS, "Attribute\t" + name + "\t" + model);
    }
    public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
      Log.logln(LOG_PROGRESS, "Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
    }
    public void internalEntityDecl(String name, String value) throws SAXException {
      Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + value);
    }
    public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
      Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
    }
    
    public void notationDecl (String name, String publicId, String systemId){
      Log.logln(LOG_PROGRESS, "notationDecl: " + name
          + ", " + publicId
          + ", " + systemId
      );
    }
    
    public void processingInstruction (String target, String data)
    throws SAXException {
      Log.logln(LOG_PROGRESS, "processingInstruction: " + target + ", " + data);
    }
    
    public void skippedEntity (String name)
    throws SAXException {
      Log.logln(LOG_PROGRESS, "skippedEntity: " + name);
    }
    
    public void unparsedEntityDecl (String name, String publicId,
        String systemId, String notationName) {
      Log.logln(LOG_PROGRESS, "unparsedEntityDecl: " + name
          + ", " + publicId
          + ", " + systemId
          + ", " + notationName
      );
    }
    
    public void setDocumentLocator(Locator locator) {
      Log.logln(LOG_PROGRESS, "setDocumentLocator Locator " + locator);
    }
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
      Log.logln(LOG_PROGRESS, "startPrefixMapping prefix: " + prefix +
          ", uri: " + uri);
    }
    public void endPrefixMapping(String prefix) throws SAXException {
      Log.logln(LOG_PROGRESS, "endPrefixMapping prefix: " + prefix);
    }
    public void startEntity(String name) throws SAXException {
      Log.logln(LOG_PROGRESS, "startEntity name: " + name);
    }
    public void endEntity(String name) throws SAXException {
      Log.logln(LOG_PROGRESS, "endEntity name: " + name);
    }
    public void startCDATA() throws SAXException {
      Log.logln(LOG_PROGRESS, "startCDATA");
    }
    public void endCDATA() throws SAXException {
      Log.logln(LOG_PROGRESS, "endCDATA");
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException exception) throws SAXException {
      Log.logln(LOG_PROGRESS || true, "error: " + showSAX(exception));
      throw exception;
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    public void fatalError(SAXParseException exception) throws SAXException {
      Log.logln(LOG_PROGRESS, "fatalError: " + showSAX(exception));
      throw exception;
    }
    
    /* (non-Javadoc)
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException exception) throws SAXException {
      Log.logln(LOG_PROGRESS, "warning: " + showSAX(exception));
      throw exception;
    }
  }
  
  /**
   * Show a SAX exception in a readable form.
   */
  public static String showSAX(SAXParseException exception) {
    return exception.getMessage() 
    + ";\t SystemID: " + exception.getSystemId() 
    + ";\t PublicID: " + exception.getPublicId() 
    + ";\t LineNumber: " + exception.getLineNumber() 
    + ";\t ColumnNumber: " + exception.getColumnNumber() 
    ;
  }
  
  /**
   * Only gets called on (mostly) resolved stuff
   */
  private CLDRFile fixAliases(Factory factory, boolean includeDraft) {
    // walk through the entire tree. If we ever find an alias, 
    // remove every peer of that alias,
    // then add everything from the resolved source of the alias.
    List aliases = new ArrayList();
    for (Iterator it = iterator(); it.hasNext();) {
      String xpath = (String) it.next();
      if (xpath.indexOf("/alias") >= 0) { // quick check; have more rigorous one later.
        aliases.add(xpath);
      }
    }
    if (aliases.size() == 0) return this;
    XPathParts parts = new XPathParts(attributeOrdering, defaultSuppressionMap);
    XPathParts fullParts = new XPathParts(attributeOrdering, defaultSuppressionMap);
    XPathParts otherParts = new XPathParts(attributeOrdering, defaultSuppressionMap);
    for (Iterator it = aliases.iterator(); it.hasNext();) {
      String xpathKey = (String) it.next();
      if (SHOW_ALIAS_FIXES) System.out.println("Doing Alias for: " + xpathKey);
      //Value v = (Value) getXpath_value().get(xpathKey);
      parts.set(xpathKey);
      int index = parts.findElement("alias"); // can have no children
      if (index < 0) continue;
      parts.trimLast();
      fullParts.set(dataSource.getFullPath(xpathKey));
      Map attributes = fullParts.getAttributes(index);
      fullParts.trimLast();
      // <alias source="<locale_ID>" path="..."/>
      String source = (String) attributes.get("source");
      if (source == null || source.equals("locale")) source = dataSource.getLocaleID();
      otherParts.set(parts);
      String otherPath = (String) attributes.get("path");
      if (otherPath != null) {
        otherParts.addRelative(otherPath);
      }
      //removeChildren(parts);  WARNING: leave the alias in the resolved one, for now.
      CLDRFile other;
      if (source.equals(dataSource.getLocaleID())) {
        other = this; 
      } else {				
        try {
          other = factory.make(source, true, includeDraft);
        } catch (RuntimeException e) {
          System.err.println("Bad alias");
          e.printStackTrace();
          throw e;
        }
      }
      addChildren(parts, fullParts, other, otherParts);
    }		
    return this;
  }
  
  /**
   * Search through the other CLDRFile, and add anything that starts with otherParts.
   * The new path will be fullParts + 
   * @param parts
   * @param other
   * @param otherParts
   */
  private CLDRFile addChildren(XPathParts parts, XPathParts fullParts, CLDRFile other, XPathParts otherParts) {
    String otherPath = otherParts + "/";
    XPathParts temp = new XPathParts(attributeOrdering, defaultSuppressionMap);
    XPathParts fullTemp = new XPathParts(attributeOrdering, defaultSuppressionMap);
    Map stuffToAdd = new HashMap();
    for (Iterator it = other.iterator(); it.hasNext();) {
      String path = (String)it.next();
      if (path.startsWith(otherPath)) {
        //Value value = (Value) other.getXpath_value().get(path);
        //temp.set(path);
        //temp.replace(otherParts.size(), parts);
        fullTemp.set(other.dataSource.getFullPath(path));
        fullTemp.replace(otherParts.size(), fullParts);
        String newPath = fullTemp.toString();
        String value = dataSource.getValueAtPath(path);
        //value = value.changePath(fullTemp.toString());
        if (SHOW_ALIAS_FIXES) System.out.println("Adding*: " + path + ";" + Utility.LINE_SEPARATOR + "\t" + newPath + ";" + Utility.LINE_SEPARATOR + "\t" 
            + dataSource.getValueAtPath(path));
        stuffToAdd.put(newPath, value);
        // to do, fix path
      }
    }
    dataSource.putAll(stuffToAdd, MERGE_REPLACE_MINE);
    return this;
  }
  
  /**
   * @param parts
   */
  private CLDRFile removeChildren(XPathParts parts) {
    String mypath = parts + "/";
    Set temp = new HashSet();
    for (Iterator it = iterator(); it.hasNext();) {
      String path = (String)it.next();
      if (path.startsWith(mypath)) {
        //if (false) System.out.println("Removing: " + getXpath_value().get(path));
        temp.add(path);
      }
    }
    dataSource.removeAll(temp);
    return this;
  }
  
  /**
   * Says whether the whole file is draft
   */
  public boolean isDraft() {
    String item = (String) iterator().next();
    return item.startsWith("//ldml[@draft=\"unconfirmed\"]");
  }
  
//public Collection keySet(Matcher regexMatcher, Collection output) {
//if (output == null) output = new ArrayList(0);
//for (Iterator it = keySet().iterator(); it.hasNext();) {
//String path = (String)it.next();
//if (regexMatcher.reset(path).matches()) {
//output.add(path);
//}
//}
//return output;
//}
  
//public Collection keySet(String regexPattern, Collection output) {
//return keySet(Pattern.compile(regexPattern).matcher(""), output);
//}
  
  /**
   * Gets the type of a given xpath, eg script, territory, ...
   * TODO move to separate class
   * @param xpath
   * @return
   */
  public static int getNameType(String xpath) {
    for (int i = 0; i < NameTable.length; ++i) {
      if (!xpath.startsWith(NameTable[i][0])) continue;
      if (xpath.indexOf(NameTable[i][1], NameTable[i][0].length()) >= 0) return i;
    }
    return -1;
  }
  
  /**
   * Gets the display name for a type
   */
  public static String getNameTypeName(int index) {
    try {
      return getNameName(index);
    } catch (Exception e) {
      return "Illegal Type Name: " + index;
    }
  }
  
  public static final int NO_NAME = -1, LANGUAGE_NAME = 0, SCRIPT_NAME = 1, TERRITORY_NAME = 2, VARIANT_NAME = 3,
  CURRENCY_NAME = 4, CURRENCY_SYMBOL = 5, 
  TZ_EXEMPLAR = 6, TZ_START = TZ_EXEMPLAR,
  TZ_GENERIC_LONG = 7, TZ_GENERIC_SHORT = 8,
  TZ_STANDARD_LONG = 9, TZ_STANDARD_SHORT = 10,
  TZ_DAYLIGHT_LONG = 11, TZ_DAYLIGHT_SHORT = 12,
  TZ_LIMIT = 13,
  LIMIT_TYPES = 13;
  
  private static final String[][] NameTable = {
    {"//ldml/localeDisplayNames/languages/language[@type=\"", "\"]", "language"},
    {"//ldml/localeDisplayNames/scripts/script[@type=\"", "\"]", "script"},
    {"//ldml/localeDisplayNames/territories/territory[@type=\"", "\"]", "territory"},
    {"//ldml/localeDisplayNames/variants/variant[@type=\"", "\"]", "variant"},
    {"//ldml/numbers/currencies/currency[@type=\"", "\"]/displayName", "currency"},
    {"//ldml/numbers/currencies/currency[@type=\"", "\"]/symbol", "currency-symbol"},
    {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/exemplarCity", "exemplar-city"},
    {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/generic", "tz-generic-long"},
    {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/generic", "tz-generic-short"},
    {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/standard", "tz-standard-long"},
    {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/standard", "tz-standard-short"},
    {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/daylight", "tz-daylight-long"},
    {"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/daylight", "tz-daylight-short"},
    
    /**
     * <long>
     <generic>Newfoundland Time</generic>
     <standard>Newfoundland Standard Time</standard>
     <daylight>Newfoundland Daylight Time</daylight>
     </long>
     -
     <short>
     <generic>NT</generic>
     <standard>NST</standard>
     <daylight>NDT</daylight>
     </short>
     */
  };
  
//private static final String[] TYPE_NAME = {"language", "script", "territory", "variant", "currency", "currency-symbol",
//"tz-exemplar",
//"tz-generic-long", "tz-generic-short"};
  
  /**
   * @return the key used to access  data of a given type
   */
  public static String getKey(int type, String code) {
    return NameTable[type][0] + code + NameTable[type][1];
  }
  /**
   * @return the code used to access  data of a given type from the path. Null if not found.
   */
  public static String getCode(String path) {
    int type = getNameType(path);
    if (type < 0) {
      throw new IllegalArgumentException("Illegal type in path: " + path);
    }
    int start = NameTable[type][0].length();
    int end = path.indexOf(NameTable[type][1], start);
    return path.substring(start, end);
  }
  /**
   * Utility for getting the name, given a code.
   * @param type
   * @param code
   * @return
   */
  public String getName(int type, String code) {
    String path = getKey(type, code);
    String result = getStringValue(path);
    if (result == null && getLocaleID().equals("en")) {
      if (type == LANGUAGE_NAME) {
        Set<String> set = Iso639Data.getNames(code);
        if (set != null) {
          return set.iterator().next();
        }
      }
    }
    return result;
  }
  
  /**
   * Utility for getting a name, given a type and code.
   */
  public String getName(String type, String code) {
    return getName(typeNameToCode(type), code);
  }
  
  /**
   * @param type
   * @return
   */
  public static int typeNameToCode(String type) {
    for (int i = 0; i < LIMIT_TYPES; ++i) {
      if (type.equalsIgnoreCase(getNameName(i))) return i;
    }
    return -1;
  }
  
  transient LanguageTagParser lparser = new LanguageTagParser();
  
  public synchronized String getName(String localeOrTZID) {
    return getName(localeOrTZID, false);
  }
  
  public synchronized String getName(String localeOrTZID, boolean onlyConstructCompound) {
    boolean isCompound = localeOrTZID.contains("_");
    String name = isCompound && onlyConstructCompound ? null : getName(LANGUAGE_NAME, localeOrTZID);
    // TODO - handle arbitrary combinations
    if (name != null && !name.contains("_")) {
      return name;
    }
    lparser.set(localeOrTZID);
    String original;

    // we need to check for prefixes, for lang+script or lang+country
    boolean haveScript = false;
    boolean haveRegion = false;
    // try lang+script
    if ((isCompound && onlyConstructCompound)) {
      name = getName(LANGUAGE_NAME, original = lparser.getLanguage());
      if (name == null) name = original;
    } else {
      name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT_REGION));
      if (name != null) {
        haveScript = haveRegion = true;
      } else {
        name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_SCRIPT));
        if (name != null) {
          haveScript = true;
        } else {
          name = getName(LANGUAGE_NAME, lparser.toString(LanguageTagParser.LANGUAGE_REGION));
          if (name != null) {
            haveRegion = true;
          } else {
            name = getName(LANGUAGE_NAME, original = lparser.getLanguage());
            if (name == null) name = original;
          }
        }
      }
    }

    String nameSeparator = getWinningValue("//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator");
    String sname;
    String extras = "";
    if (!haveScript) {
      sname = original = lparser.getScript();
      if (sname.length() != 0) {
        if (extras.length() != 0) extras += nameSeparator;
        sname = getName(SCRIPT_NAME, sname);
        extras += (sname == null ? original : sname);
      }
    }
    if (!haveRegion) {
      original = sname = lparser.getRegion();
      if (sname.length() != 0) {
        if (extras.length() != 0) extras += nameSeparator;
        sname = getName(TERRITORY_NAME, sname);
        extras += (sname == null ? original : sname);
      }
    }
    List variants = lparser.getVariants();
    for (int i = 0; i < variants.size(); ++i) {
      if (extras.length() != 0) extras += nameSeparator;
      sname = getName(VARIANT_NAME, original = (String)variants.get(i));
      extras += (sname == null ? original : sname);
    }
    // fix this -- shouldn't be hardcoded!
    if (extras.length() == 0) {
      return name;
    }
    String namePattern = getWinningValue("//ldml/localeDisplayNames/localeDisplayPattern/localePattern");
    return MessageFormat.format(namePattern, new Object[]{name, extras});
  }
  
  /**
   * Returns the name of a type.
   */
  public static String getNameName(int choice) {
    return NameTable[choice][2];
  }
  
  /**
   * Get standard ordering for elements.
   * @return ordered collection with items.
   */
  public static List<String> getElementOrder() {
    return elementOrdering.getOrder(); // already unmodifiable
  }
  
  /**
   * Get standard ordering for attributes.
   * @return ordered collection with items.
   */
  public static List<String> getAttributeOrder() {
    return attributeOrdering.getOrder(); // already unmodifiable
  }
  
  /**
   * Get standard ordering for attributes.
   * @return ordered collection with items.
   */
  public static Comparator getAttributeComparator() {
    return attributeOrdering; // already unmodifiable
  }
  
  
  /**
   * Get standard ordering for attribute values.
   * @return ordered collection with items.
   */
  public static Collection getValueOrder() {
    return valueOrdering.getOrder(); // already unmodifiable
  }
  
  /**
   * Utility to get the parent of a locale. If the input is "root", then the output is null.
   */
  public static String getParent(String localeName) {
    int pos = localeName.lastIndexOf('_');
    if (pos >= 0) {
      return localeName.substring(0,pos);
    }
    if (localeName.equals("root") || localeName.equals("supplementalData")) return null;
    return "root";
  }
  
  // note: run FindDTDOrder to get this list
  // TODO, convert to use SupplementalInfo
  
  static MapComparator elementOrdering = (MapComparator) new MapComparator()
  .add(
          "ldml alternate attributeOrder attributes blockingItems calendarSystem character character-fallback codePattern codesByTerritory comment context cp deprecatedItems distinguishingItems elementOrder first_variable fractions identity info languageAlias languageCodes languageCoverage languagePopulation last_variable first_tertiary_ignorable last_tertiary_ignorable first_secondary_ignorable last_secondary_ignorable first_primary_ignorable last_primary_ignorable first_non_ignorable last_non_ignorable first_trailing last_trailing likelySubtag mapTimezones mapZone numberingSystem pluralRule pluralRules reference region scriptAlias scriptCoverage serialElements substitute suppress tRule telephoneCountryCode territoryAlias territoryCodes territoryCoverage currencyCoverage timezone timezoneCoverage transform usesMetazone validity alias appendItem base beforeCurrency afterCurrency currencyMatch dateFormatItem day defaultNumberingSystem deprecated distinguishing blocking coverageAdditions era eraNames eraAbbr eraNarrow exemplarCharacters fallback field generic greatestDifference height hourFormat hoursFormat gmtFormat gmtZeroFormat intervalFormatFallback intervalFormatItem key localeDisplayNames layout localeDisplayPattern languages localePattern localeSeparator localizedPatternChars dateRangePattern calendars long mapping measurementSystem measurementSystemName messages minDays firstDay month months monthNames monthAbbr days dayNames dayAbbr orientation inList inText paperSize pattern displayName quarter quarters quotationStart quotationEnd alternateQuotationStart alternateQuotationEnd rbnfrule regionFormat fallbackFormat abbreviationFallback preferenceOrdering relative reset p pc rule ruleset rulesetGrouping s sc scripts segmentation settings short commonlyUsed exemplarCity singleCountries default calendar calendarPreference collation currency currencyFormat currencySpacing currencyFormatLength dateFormat dateFormatLength dateTimeFormat dateTimeFormatLength availableFormats appendItems dayContext dayWidth decimalFormat decimalFormatLength intervalFormats monthContext monthWidth percentFormat percentFormatLength quarterContext quarterWidth scientificFormat scientificFormatLength skipDefaultLocale defaultContent standard daylight suppress_contractions optimize rules surroundingMatch insertBetween symbol decimal group list percentSign nativeZeroDigit patternDigit plusSign minusSign exponential perMille infinity nan currencyDecimal currencyGroup symbols decimalFormats scientificFormats percentFormats currencyFormats currencies t tc q qc i ic extend territories timeFormat timeFormatLength timeZoneNames type unit unitPattern variable attributeValues variables segmentRules variantAlias variants keys types measurementSystemNames codePatterns version generation cldrVersion currencyData language script territory territoryContainment languageData territoryInfo calendarData variant week am pm eras dateFormats timeFormats dateTimeFormats fields weekData measurementData timezoneData characters delimiters measurement dates numbers transforms metadata codeMappings likelySubtags metazoneInfo plurals telephoneCodeData numberingSystems units collations posix segmentations rbnf references weekendStart weekendEnd width x yesstr nostr yesexpr noexpr zone metazone special zoneAlias zoneFormatting zoneItem supplementalData"
          .split("\\s+"))
          .setErrorOnMissing(false)
          .freeze();

  static MapComparator attributeOrdering = (MapComparator) new MapComparator()
  .add(
          "_q type id choice key registry source target path day date version count lines characters iso4217 before from to mzone number time casing list uri digits rounding iso3166 hex request direction alternate backwards caseFirst caseLevel hiraganaQuarternary hiraganaQuaternary variableTop normalization numeric strength elements element attributes attribute aliases attributeValue contains multizone order other replacement scripts services territories territory tzidVersion value values variant variants visibility alpha3 code end exclude fips10 gdp internet literacyPercent locales officialStatus population populationPercent start used writingPercent validSubLocales standard references alt draft"            .split("\\s+"))
          .setErrorOnMissing(false)
          .freeze();
  static MapComparator valueOrdering = (MapComparator) new MapComparator().setErrorOnMissing(false).freeze();
  /*
   
   //RuleBasedCollator valueOrdering = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
    static {
    
    
    // others are alphabetical
     String[] valueOrder = {
     "full", "long", "medium", "short",
     "abbreviated", "narrow", "wide",
     //"collation", "calendar", "currency",
      "buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil", "japanese", "direct",				
      //"japanese", "buddhist", "islamic", "islamic-civil", "hebrew", "chinese", "gregorian", "phonebook", "traditional", "direct",
       
       "sun", "mon", "tue", "wed", "thu", "fri", "sat", // removed, since it is a language tag
       "America/Vancouver",
       "America/Los_Angeles",
       "America/Edmonton",
       "America/Denver",
       "America/Phoenix",
       "America/Winnipeg",
       "America/Chicago",
       "America/Montreal",
       "America/New_York",
       "America/Indianapolis",
       "Pacific/Honolulu",
       "America/Anchorage",
       "America/Halifax",
       "America/St_Johns",
       "Europe/Paris",
       "Europe/Belfast",
       "Europe/Dublin",
       "Etc/GMT",
       "Africa/Casablanca",
       "Asia/Jerusalem",
       "Asia/Tokyo",
       "Europe/Bucharest",
       "Asia/Shanghai",
       };       	
       valueOrdering.add(valueOrder).lock();
       //StandardCodes sc = StandardCodes.make();
        }
        */
  static MapComparator dayValueOrder = (MapComparator) new MapComparator().add(new String[] {
      "sun", "mon", "tue", "wed", "thu", "fri", "sat"}).freeze();
  static MapComparator widthOrder = (MapComparator) new MapComparator().add(new String[] {
      "abbreviated", "narrow", "wide"}).freeze();
  static MapComparator lengthOrder = (MapComparator) new MapComparator().add(new String[] {
      "full", "long", "medium", "short"}).freeze();
  static MapComparator dateFieldOrder = (MapComparator) new MapComparator().add(new String[] {
      "era", "year", "month", "week", "day", "weekday", "dayperiod",
      "hour", "minute", "second", "zone"}).freeze();
  static Comparator zoneOrder = StandardCodes.make().getTZIDComparator();
  
  static Set orderedElements = Collections.unmodifiableSet(new HashSet(java.util.Arrays
      .asList(new String[] {
          "variable", "comment", "tRule", "attributeValues", 
          // collation
          "base", "settings", "suppress_contractions", "optimize", "rules",
          //"dateFormatItem",
          // collation
          "reset", "p", "pc", "s", "sc", "t", "tc", "q", "qc", "i",
          "ic", "x", "extend", "first_variable", "last_variable",
          "first_tertiary_ignorable", "last_tertiary_ignorable",
          "first_secondary_ignorable", "last_secondary_ignorable",
          "first_primary_ignorable", "last_primary_ignorable",
          "first_non_ignorable", "last_non_ignorable",
          "first_trailing", "last_trailing",
          // rbnf
          // supplemental
      "languagePopulation"})));
  /**
   * 
   */
  public static Comparator getAttributeValueComparator(String element, String attribute) {
    Comparator comp = valueOrdering;
    if (attribute.equals("day")) { //  && (element.startsWith("weekend")
      comp = dayValueOrder;
    } else if (attribute.equals("type")) {
      if (element.endsWith("FormatLength")) comp = lengthOrder;
      else if (element.endsWith("Width")) comp = widthOrder;
      else if (element.equals("day")) comp = dayValueOrder;
      else if (element.equals("field")) comp = dateFieldOrder;
      else if (element.equals("zone")) comp = zoneOrder;
    }
    return comp;
  }		
  
  /**
   * Comparator for attributes in CLDR files
   */
  public static Comparator ldmlComparator = new LDMLComparator();
  
  static class LDMLComparator implements Comparator {
    
    transient XPathParts a = new XPathParts(attributeOrdering, null);
    transient XPathParts b = new XPathParts(attributeOrdering, null);
    transient Set attSet1 = new TreeSet(attributeOrdering);
    transient Set attSet2 = new TreeSet(attributeOrdering);
    
    public void addElement(String a) {
      //elementOrdering.add(a);
    }
    public void addAttribute(String a) {
      if ( false && (a.equals("buddhist") ||
          a.equals("gregorian"))) {
        System.out.println("here2");
      }
      //attributeOrdering.add(a);
    }
    public void addValue(String a) {
      //valueOrdering.add(a);
    }
    public int compare(Object o1, Object o2) {
      if (o1 == o2) return 0; // quick test for common case
      int result;
      if (false && (o1.toString().indexOf("alt") >= 0 ||
          o2.toString().indexOf("alt") >= 0)) {
        System.out.println("here");
      }
      a.set((String)o1);
      b.set((String)o2);
      int minSize = a.size();
      if (b.size() < minSize) minSize = b.size();
      for (int i = 0; i < minSize; ++i) {
        String aname = a.getElement(i);
        String bname = b.getElement(i);
        if (0 != (result = elementOrdering.compare(aname, bname))) {
          // if they are different, then 
          // all ordered items are equal, and > than all unordered
          boolean aOrdered = orderedElements.contains(aname);
          boolean bOrdered = orderedElements.contains(bname);
          // if both ordered, continue, return result
          if (aOrdered && bOrdered) {
            // continue with comparison
          } else {
            if (aOrdered == bOrdered) return result; // both off
            return aOrdered ? 1 : 0;
          }
        }
        Map am = a.getAttributes(i);
        Map bm = b.getAttributes(i);
        int minMapSize = am.size();
        if (bm.size() < minMapSize) minMapSize = bm.size();
        if (minMapSize != 0) {
          Iterator ait = am.keySet().iterator();
          Iterator bit = bm.keySet().iterator();
          for (int j = 0; j < minMapSize; ++j) {
            String akey = (String) ait.next();
            String bkey = (String) bit.next();
            if (0 != (result = attributeOrdering.compare(akey, bkey))) return result;
            String avalue = (String) am.get(akey);
            String bvalue = (String) bm.get(bkey);
            Comparator comp = getAttributeValueComparator(aname, akey);
            if (0 != (result = comp.compare(avalue, bvalue))) return result;
          }
        }
        if (am.size() < bm.size()) return -1;
        if (am.size() > bm.size()) return 1;				
      }
      if (a.size() < b.size()) return -1;
      if (a.size() > b.size()) return 1;
      return 0;
    }
  }
  
  static String[][] distinguishingData = {
    {"*", "key"},
    {"*", "id"},
    {"*", "_q"},
    {"*", "alt"},
    {"*", "iso4217"},
    {"*", "iso3166"},
    {"default", "type"},
    {"measurementSystem", "type"},
    {"mapping", "type"},
    {"abbreviationFallback", "type"},
    {"preferenceOrdering", "type"},
    {"deprecatedItems", "iso3166"},
    {"ruleset", "type"},
    {"rbnfrule", "value"},
  };
  
  private final static Map distinguishingAttributeMap = asMap(distinguishingData, true); 
  
  private final static Map defaultSuppressionMap; 
  static {
    String[][] data = {
        {"ldml", "version", GEN_VERSION},
        {"orientation", "characters", "left-to-right"},
        {"orientation", "lines", "top-to-bottom"},
        {"weekendStart", "time", "00:00"},
        {"weekendEnd", "time", "24:00"},
        {"dateFormat", "type", "standard"},
        {"timeFormat", "type", "standard"},
        {"dateTimeFormat", "type", "standard"},
        {"decimalFormat", "type", "standard"},
        {"scientificFormat", "type", "standard"},
        {"percentFormat", "type", "standard"},
        {"currencyFormat", "type", "standard"},
        {"pattern", "type", "standard"},
        {"currency", "type", "standard"},
        {"collation", "type", "standard"},
        {"transform", "visibility", "external"},
        {"*", "_q", "*"},
    };
    Map tempmain = asMap(data, true);
    defaultSuppressionMap = Collections.unmodifiableMap(tempmain);
  }
  
  public static Map getDefaultSuppressionMap() {
    return defaultSuppressionMap;
  }
  
  private static boolean matches(Map map, Object[] items, boolean doStar) {
    for (int i = 0; i < items.length - 2; ++i) {
      Map tempMap = (Map) map.get(items[i]);
      if (doStar && map == null) map = (Map) map.get("*");
      if (map == null) return false;
      map = tempMap;
    }
    return map.get(items[items.length-2]) == items[items.length-1];
  }
  
  private static Map asMap(String[][] data, boolean tree) {
    Map tempmain = tree ? (Map) new TreeMap() : new HashMap();
    int len = data[0].length; // must be same for all elements
    for (int i = 0; i < data.length; ++i) {
      Map temp = tempmain;
      if (len != data[i].length) {
        throw new IllegalArgumentException("Must be square array: fails row " + i);
      }
      for (int j = 0; j < len - 2; ++j) {
        Map newTemp = (Map) temp.get(data[i][j]);
        if (newTemp == null) temp.put(data[i][j], newTemp = tree ? (Map) new TreeMap() : new HashMap());
        temp = newTemp;
      }
      temp.put(data[i][len-2], data[i][len-1]);
    }
    return tempmain;
  }
  /**
   * Removes a comment.
   */
  public CLDRFile removeComment(String string) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    // TODO Auto-generated method stub
    dataSource.getXpathComments().removeComment(string);
    return this;
  }
  
  /**
   * 
   */
  public CLDRFile makeDraft() {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    XPathParts parts = new XPathParts(null,null);
    for (Iterator it = dataSource.iterator(); it.hasNext();) {
      String path = (String) it.next();
      //Value v = (Value) getXpath_value().get(path);
      //if (!(v instanceof StringValue)) continue;
      parts.set(dataSource.getFullPath(path)).addAttribute("draft", "unconfirmed");
      dataSource.putValueAtPath(parts.toString(), dataSource.getValueAtPath(path));
    }
    return this;
  }
  
  public UnicodeSet getExemplarSet(String type, WinningChoice winningChoice) {
    if (type.length() != 0) type = "[@type=\"" + type + "\"]";
    String path = "//ldml/characters/exemplarCharacters" + type;
    if (winningChoice == WinningChoice.WINNING) {
      path = getWinningPath(path);
    }
    String v = getStringValue(path);
    if (v == null) return null;
    UnicodeSet result = new UnicodeSet(v, UnicodeSet.CASE);
    result.remove(0x20);
    return result;
  }
  
  public String getCurrentMetazone(String zone) {
    for (Iterator it2 = iterator(); it2.hasNext();) {
      String xpath = (String)it2.next();
      if ( xpath.startsWith("//ldml/dates/timeZoneNames/zone[@type=\""+zone+"\"]/usesMetazone")) {
        XPathParts parts = new XPathParts(null,null);
        parts.set(xpath);
        if (!parts.containsAttribute("to")) {
           String mz = parts.getAttributeValue(4,"mzone");
           return mz;
        }
      }
    }
    return null;
  }

  transient CLDRFile resolvedVersion;
  
  public CLDRFile getResolved() {
    if (dataSource.isResolving()) return this;
    if (resolvedVersion == null) {
      resolvedVersion = new CLDRFile(dataSource, true);
    }
    return resolvedVersion;
  }
  
  public Set getAvailableLocales() {
    return dataSource.getAvailableLocales();
  }
  
  public CLDRFile make(String locale, boolean resolved) {
    if (dataSource == null) throw new UnsupportedOperationException("Make not supported");
    return new CLDRFile(dataSource.make(locale), resolved);
  }
  
  // WARNING: this must go AFTER attributeOrdering is set; otherwise it uses a null comparator!!
  private static final DistinguishedXPath distinguishedXPath = new DistinguishedXPath();

  //private static Set atomicElements = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[]{"collation", "segmentation"})));
  
  public static final String distinguishedXPathStats() {
    return distinguishedXPath.stats();
  }
  
  private static class DistinguishedXPath {
    public static final String stats() {
      return "distinguishingMap:" + distinguishingMap.size() + " " +
      "normalizedPathMap:" + normalizedPathMap.size();
    }
    private static Map distinguishingMap = new HashMap();
    private static Map normalizedPathMap = new HashMap();
    private static XPathParts distinguishingParts = new XPathParts(attributeOrdering, null);
    
    public static String getDistinguishingXPath(String xpath, String[] normalizedPath, boolean nonInheriting) {
      synchronized (distinguishingMap) {
        String result = (String) distinguishingMap.get(xpath);
        if (result == null) {
          distinguishingParts.set(xpath);
          boolean inheriting = distinguishingParts.getElement(0).equals("ldml");
          
          // first clean up draft and alt
          
          String draft = null;
          String alt = null;
          String references = "";
          // note: we only need to clean up items that are NOT on the last element,
          // so we go up to size() - 1.
          
          
          // note: each successive item overrides the previous one. That's intended
          
          for (int i = 0; i < distinguishingParts.size() - 1; ++i) {
            // String element = distinguishingParts.getElement(i);
            //if (atomicElements.contains(element)) break;
            Map attributes = distinguishingParts.getAttributes(i);
            for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
              String attribute = (String) it.next();
              if (attribute.equals("draft")) {
                draft = (String) attributes.get(attribute);
                it.remove();
              } else if (attribute.equals("alt")) {
                alt = (String) attributes.get(attribute);
                it.remove();
              } else if (attribute.equals("references")) {
                if (references.length() != 0) references += " ";
                references += (String) attributes.get("references");
                it.remove();
              }
            }
          }
          if (draft != null || alt != null || references.length() != 0) {
            // get the last element that is not ordered.
            int placementIndex = distinguishingParts.size() - 1;
            while (true) {
              String element = distinguishingParts.getElement(placementIndex);
              if (!orderedElements.contains(element)) break;
              --placementIndex;
            }
            Map attributes = distinguishingParts.getAttributes(placementIndex);
            if (draft != null) attributes.put("draft", draft);
            if (alt != null) attributes.put("alt", alt);
            if (references.length() != 0) attributes.put("references", references);
            String newXPath = distinguishingParts.toString();
            if (!newXPath.equals(xpath)) {
              normalizedPathMap.put(xpath, newXPath); // store differences
              //System.err.println("fixing " + xpath + " => " + newXPath);
            }
          }
          
          // now remove non-distinguishing attributes (if non-inheriting)
          
          if (inheriting) {
            for (int i = 0; i < distinguishingParts.size(); ++i) {
              String element = distinguishingParts.getElement(i);
              Map attributes = distinguishingParts.getAttributes(i);
              for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
                String attribute = (String) it.next();
                if (!isDistinguishing(element, attribute)) {
                  it.remove();
                }
              }
            }
          }
          
          result = distinguishingParts.toString();
          if (result.equals(xpath)) { // don't save the copy if we don't have to.
            result = xpath;
          }
          distinguishingMap.put(xpath, result);
        }
        if (normalizedPath != null) {
          normalizedPath[0] = (String) normalizedPathMap.get(xpath);
          if (normalizedPath[0] == null) normalizedPath[0] = xpath;
        }
        return result;
      }
    }
    
    public Map getNonDistinguishingAttributes(String fullPath, Map result, Set skipList) {
      if (result == null) result = new LinkedHashMap();
      else result.clear();
      synchronized (distinguishingMap) {
        distinguishingParts.set(fullPath);
        for (int i = 0; i < distinguishingParts.size(); ++i) {
          String element = distinguishingParts.getElement(i);
          //if (atomicElements.contains(element)) break;
          Map attributes = distinguishingParts.getAttributes(i);
          for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
            String attribute = (String) it.next();
            if (!isDistinguishing(element,attribute) && !skipList.contains(attribute)) {
              result.put(attribute, attributes.get(attribute));
            }
          }
        }
      }
      return result;
    }
  }
  
  public static class Status {
    public String pathWhereFound;
    public String toString() {
      return pathWhereFound;
    }
  }
  
  public static boolean isLOG_PROGRESS() {
    return LOG_PROGRESS;
  }
  
  public static void setLOG_PROGRESS(boolean log_progress) {
    LOG_PROGRESS = log_progress;
  }
  
  public boolean isEmpty() {
    return !dataSource.iterator().hasNext();
  }
  
  public Map getNonDistinguishingAttributes(String fullPath, Map result, Set skipList) {
    return distinguishedXPath.getNonDistinguishingAttributes(fullPath, result, skipList);
  }
  
  public String getDtdVersion() {
    return dtdVersion;
  }
  
  public String getStringValue(String path, boolean ignoreOtherLeafAttributes) {
    String result = getStringValue(path);
    if (result != null) return result;
    XPathParts parts = new XPathParts().set(path);
    Map lastAttributes = parts.getAttributes(parts.size()-1);
    XPathParts other = new XPathParts();
    String base = parts.toString(parts.size()-1) + "/" + parts.getElement(parts.size()-1); // trim final element
    for (Iterator it = iterator(base); it.hasNext();) {
      String otherPath = (String)it.next();
      other.set(otherPath);
      if (other.size() != parts.size()) continue;
      Map lastOtherAttributes =  other.getAttributes(other.size()-1);
      if (!contains(lastOtherAttributes, lastAttributes)) continue;
      if (result == null) {
        result = getStringValue(otherPath);
      } else {
        throw new IllegalArgumentException("Multiple values for path: " + path);
      }
    }
    return result;
  }
  
  private boolean contains(Map a, Map b) {
    for (Iterator it = b.keySet().iterator(); it.hasNext();) {
      Object key = it.next();
      Object otherValue = a.get(key);
      if (otherValue == null) return false;
      Object value = b.get(key);
      if (!otherValue.equals(value)) return false;
    }
    return true;
  }
  
  public String getFullXPath(String path, boolean ignoreOtherLeafAttributes) {
    String result = getFullXPath(path);
    if (result != null) return result;
    XPathParts parts = new XPathParts().set(path);
    Map lastAttributes = parts.getAttributes(parts.size()-1);
    XPathParts other = new XPathParts();
    String base = parts.toString(parts.size()-1) + "/" + parts.getElement(parts.size()-1); // trim final element
    for (Iterator it = iterator(base); it.hasNext();) {
      String otherPath = (String)it.next();
      other.set(otherPath);
      if (other.size() != parts.size()) continue;
      Map lastOtherAttributes =  other.getAttributes(other.size()-1);
      if (!contains(lastOtherAttributes, lastAttributes)) continue;
      if (result == null) {
        result = getFullXPath(otherPath);
      } else {
        throw new IllegalArgumentException("Multiple values for path: " + path);
      }
    }
    return result;
  }
  
  /**
   * Return true if this item is the "winner" in the survey tool
   * @param path
   * @return
   */
  public boolean isWinningPath(String path) {
    return dataSource.isWinningPath(path);
  }
  
  /**
   * Returns the "winning" path, for use in the survey tool tests, out of all
   * those paths that only differ by having "alt proposed". The exact meaning
   * may be tweaked over time, but the user's choice (vote) has precedence, then
   * any undisputed choice, then the "best" choice of the remainders. A value is
   * always returned if there is a valid path, and the returned value is always
   * a valid path <i>in the resolved file</i>; that is, it may be valid in the
   * parent, or valid because of aliasing.
   * 
   * @param path
   * @return path, perhaps with an alt proposed added.
   */
  public String getWinningPath(String path) {
    return dataSource.getWinningPath(path);
  }
  
  /**
   * Shortcut for getting the string value for the winning path
   * @param path
   * @return
   */
  public String getWinningValue(String path) {
    final String winningPath = getWinningPath(path);
    return winningPath == null ? null : getStringValue(winningPath);
  }
  
  /**
   * Return the distinguished paths that have the specified value. The pathPrefix and pathMatcher
   * can be used to restrict the returned paths to those matching.
   * The pathMatcher can be null (equals .*).
   * @param valueToMatch
   * @param pathPrefix
   * @return
   */
  public Set<String> getPathsWithValue(String valueToMatch, String pathPrefix, Matcher pathMatcher, Set<String> result) {
    if (result == null) {
      result = new HashSet();
    }
    dataSource.getPathsWithValue(valueToMatch, pathPrefix, result);
    if (pathMatcher == null) {
      return result;
    }
    for (Iterator<String> it = result.iterator(); it.hasNext();) {
      String path = it.next();
      if (!pathMatcher.reset(path).matches()) {
        it.remove();
      }
    }
    return result;
  }
  
  public enum WinningChoice {NORMAL, WINNING};
  
  // TODO This stuff needs some rethinking, but just to get it going for now...
  
  public CLDRFile getSupplementalData() {
    try {
      return make("supplementalData", false);
    } catch (RuntimeException e) {
      return Factory.make(getSupplementalDirectory().getPath(), ".*").make("supplementalData", false);
    }
  }
  
  public CLDRFile getSupplementalMetadata() {
    try {
      return make("supplementalMetadata", false);
    } catch (RuntimeException e) {
      return Factory.make(getSupplementalDirectory().getPath(), ".*").make("supplementalMetadata", false);
    }
  }
  
  /**
   * Used in TestUser to get the "winning" path. Simple implementation just for testing.
   * @author markdavis
   *
   */
  static class WinningComparator implements Comparator {
    String user;
    public WinningComparator(String user) {
      this.user = user;
    }
    /**
     * if it contains the user, sort first. Otherwise use normal string sorting. A better implementation would look at
     * the number of votes next, and whither there was an approved or provisional path.
     */
    public int compare(Object oo1, Object oo2) {
      String o1 = (String) oo1;
      String o2 = (String) oo2;
      if (o1.contains(user)) {
        if (!o2.contains(user)) {
          return -1; // if it contains user
        }
      } else if (o2.contains(user)) {
        return 1; // if it contains user
      }
      return o1.compareTo(o2);
    }
  }
  
  /**
   * This is a test class used to simulate what the survey tool would do.
   * @author markdavis
   *
   */
  public static class TestUser extends CLDRFile {

    private CLDRFile baseFile;
    Map<String,String> userOverrides = new HashMap();

    public TestUser(CLDRFile baseFile, String user, boolean resolved) {
      super(baseFile.dataSource, resolved);
      Relation<String,String> pathMap = new Relation(new HashMap(), TreeSet.class, new WinningComparator(user));
      this.baseFile = baseFile;
      for (String path : baseFile) {
        String newPath = getNondraftNonaltXPath(path);
        pathMap.put(newPath, path);
      }
      // now reduce the storage by just getting the winning ones
      // so map everything but the first path to the first path
      for (String path : pathMap.keySet()) {
        String winner = null;
        for (String rowPath : pathMap.getAll(path)) {
          if (winner == null) {
            winner = rowPath;
            continue;
          }
          userOverrides.put(rowPath, winner);
        }
      }
    }

    @Override
    public String getWinningPath(String path) {
      String trial = userOverrides.get(path);
      if (trial != null) {
        return trial;
      }
      return path;
    }
  }
  
  public Collection<String> getExtraPaths() {
    return getExtraPaths(new HashSet<String>());
  }
  
  public Collection<String> getExtraPaths(Collection<String> toAddTo) {
    SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(getSupplementalDirectory());
    Set<String> codes = StandardCodes.make().getAvailableCodes("currency");
    // units
    final Set<Count> pluralCounts = supplementalData.getPlurals(getLocaleID()).getCountToExamplesMap().keySet();
    if (pluralCounts.size() != 1) {
      for (Count count : pluralCounts) {
        //if (count.equals(Count.one)) continue;
        //final String anyPatternPath = "//ldml/units/unit[@type=\"any\"]/unitPattern[@count=\"" + count + "\"]";
        //toAddTo.add(anyPatternPath);
        for (String unit : new String[]{"year", "month", "week", "day", "hour", "minute", "second"}) {
          final String unitPattern = "//ldml/units/unit[@type=\"" + unit + "\"]/unitPattern[@count=\"" + count + "\"]";
          String value = getWinningValue(unitPattern);
          if (value != null && value.length() == 0) {
            continue;
          }
          toAddTo.add(unitPattern);
          //toAddTo.add("//ldml/units/unit[@type=\"" + unit + "\"]/unitName[@count=\"" + count + "\"]");
        }

        // do units now, but only if pattern is not empty

        final String currencyPattern = "//ldml/numbers/currencyFormats/unitPattern[@count=\"" + count + "\"]";
        String value = getWinningValue(currencyPattern);
        if (value != null && value.length() == 0) {
          continue;
        }
        toAddTo.add(currencyPattern);
        for (String unit : codes) {
          toAddTo.add("//ldml/numbers/currencies/currency[@type=\"" + unit + "\"]/displayName[@count=\"" + count + "\"]");
        }
      }
    }
    return toAddTo;
  }
  
  public Collection<String> getExtraPaths(String prefix, Collection<String> toAddTo) {
    for (String item : getExtraPaths()) {
      if (item.startsWith(prefix)) {
        toAddTo.add(item);
      }
    }
    return toAddTo;
  }
  
  private Matcher typeValueMatcher = Pattern.compile("\\[@type=\"([^\"]*)\"\\]").matcher("");
  
  public boolean isPathExcludedForSurvey(String distinguishedPath) {
    // for now, just zones
    if (distinguishedPath.contains("/exemplarCity")) {
      excludedZones = getExcludedZones();
      typeValueMatcher.reset(distinguishedPath).find();
      if (excludedZones.contains(typeValueMatcher.group(1))) {
        return true;
      }
    }
    return false;
  }
  
  private Set<String> excludedZones;
  
  public Set<String> getExcludedZones() {
    synchronized (this) {
      if (excludedZones == null) {
        SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(getSupplementalDirectory());
        excludedZones = new HashSet<String>(supplementalData.getSingleRegionZones());
        List<String> singleCountries = Arrays.asList(
                new XPathParts()
                .set(getFullXPath("//ldml/dates/timeZoneNames/singleCountries"))
                .getAttributeValue(-1, "list")
                .split("\\s+"));
        excludedZones.addAll(singleCountries);
        excludedZones = Collections.unmodifiableSet(excludedZones); // protect
      }
      return excludedZones;
    }
  }

  /**
   * Get the path with the given count. 
   * It acts like there is an alias in root from count=n to count=one, 
   * then for currency display names from count=one to no count
   * <br>For unitPatterns, falls back to Count.one.
   * <br>For others, falls back to Count.one, then no count.
   * <p>The fallback acts like an alias in root.
   * @param xpath
   * @param count Count may be null. Returns null if nothing is found.
   * @param winning TODO
   * @return
   */
  public String getCountPathWithFallback(String xpath, Count count, boolean winning) {
    String result;
    XPathParts parts = new XPathParts().set(xpath);
    boolean isDisplayName = parts.contains("displayName");
    
    // try the given count first
    result = getCountPathWithFallback2(parts, xpath, count, winning);
    if (result != null && isNotRoot(result)) {
      return result;
    }
    // now try fallback
    if (count != Count.other) {
      result = getCountPathWithFallback2(parts, xpath, Count.other, winning);
      if (result != null && isNotRoot(result)) {
        return result;
      }
    }
    // now try deletion (for currency)
    if (isDisplayName) {
      result = getCountPathWithFallback2(parts, xpath, null, winning);
    }
    return result;
  }
  
  private String getCountPathWithFallback2(XPathParts parts, String xpathWithNoCount,
          Count count, boolean winning) {
    parts.addAttribute("count", count == null ? null : count.toString());
    String newPath = parts.toString();
    if (!newPath.equals(xpathWithNoCount)) {
      if (winning) {
        String temp = getWinningPath(newPath);
        if (temp != null) {
          newPath = temp;
        }
      }
      if (dataSource.getValueAtPath(newPath) != null) {
        return newPath;
      }
      //return getWinningPath(newPath);
    }
    return null;
  }

  /**
   * Returns a value to be used for "filling in" a "Change" value in the survey
   * tool. Currently returns the following.
   * <ul>
   * <li>The "winning" value (if not inherited). Example: if "Donnerstag" has the most votes for
   * 'thursday', then clicking on the empty field will fill in "Donnerstag"
   * <li>The singular form. Example: if the value for 'hour' is "heure", then
   * clicking on the entry field for 'hours' will insert "heure".
   * <li>The parent's value. Example: if I'm
   * in [de_CH] and there are no proposals for 'thursday', then clicking on the
   * empty field will fill in "Donnerstag" from [de].
   * <li>Otherwise don't fill in anything, and return null.
   * </ul>
   * 
   * @return
   */
  public String getFillInValue(String distinguishedPath) {
    String winningPath = getWinningPath(distinguishedPath);
    if (isNotRoot(winningPath)) {
      return getStringValue(winningPath);
    }
    String fallbackPath = getFallbackPath(winningPath, true);
    if (fallbackPath != null) {
      String value = getWinningValue(fallbackPath);
      if (value != null) {
        return value;
      }
    }
    return getStringValue(winningPath);
  }
  
  /**
   * returns true if the source of the path exists, and is neither root nor code-fallback
   * @param distinguishedPath
   * @return
   */
  public boolean isNotRoot(String distinguishedPath) {
    String source = getSourceLocaleID(distinguishedPath, null);
    return source != null && !source.equals("root") && !source.equals(XMLSource.CODE_FALLBACK_ID);
  }

  public static List<String> getSerialElements() {
    // TODO Auto-generated method stub
    return new ArrayList(orderedElements);
  }
}
