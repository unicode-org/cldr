// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.LDML2ICUConverter.DocumentPair;
import org.unicode.cldr.icu.LDML2ICUConverter.LDMLServices;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class LDML2ICUInputLocale {
  private final LDMLServices services;
  private final boolean notOnDisk;
  private final String locale;
  private final CLDRFile rawFile;
  private final CLDRFile file;
  private final CLDRFile specialsFile;
  private CLDRFile resolved;

  /** Cache results of beenHere. See {@link #beenHere}. */
  private final Set<String> alreadyDone = new HashSet<String>();

  
  public boolean isNotOnDisk() {
    return notOnDisk;
  }

  public String getLocale() {
    return locale;
  }

  public CLDRFile getFile() {
    return file;
  }

  public CLDRFile getSpecialsFile() {
    return specialsFile;
  }

  @Override
  public String toString() {
    return "{"
      + "notOnDisk=" + notOnDisk
      + " locale=" + locale
      + " rawFile=" + abbreviated(rawFile)
      + " file=" + abbreviated(file)
      + " specialsFile=" + abbreviated(specialsFile)
      + " resolved=" + abbreviated(resolved)
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
    if (resolved == null) {
      if (services.cldrFactory() != null) {
        resolved = services.cldrFactory().make(locale, true, DraftStatus.contributed);
      } else {
        System.err.println("Error: cldrFactory is null in \"resolved()\"");
        System.err.flush();
        System.exit(1);
      }
    }
    return resolved;
  }

  public LDML2ICUInputLocale(CLDRFile fromFile, LDMLServices services) {
    this.services = services;
    this.notOnDisk = true;
    this.locale = fromFile.getLocaleID();
    this.rawFile = this.file = this.resolved = fromFile;
    this.specialsFile = null;
  }

  public LDML2ICUInputLocale(String locale, LDMLServices services) {
    this.services = services;
    this.notOnDisk = false;
    this.locale = locale;
    this.rawFile = services.cldrFactory().make(locale, false);
    this.specialsFile = services.getSpecialsFile(locale);
    if (specialsFile != null) {
      this.file = (CLDRFile) rawFile.cloneAsThawed();
      this.file.putAll(specialsFile, CLDRFile.MERGE_REPLACE_MINE);
    } else {
      this.file = rawFile; // frozen
    }
  }

  public Set<String> getByType(String baseXpath, String element) {
    return getByType(baseXpath, element, LDMLConstants.TYPE);
  }

  public Set<String> getByType(String baseXpath, String element, String attribute) {
    Set<String> typeList = new HashSet<String>();
    for (Iterator<String> iter = file.iterator(baseXpath); iter.hasNext();) {
      String somePath = iter.next();
      String type = XPPUtil.getAttributeValue(somePath, element, attribute);
      if (type == null) {
        continue;
      } else {
        typeList.add(type);
      }
    }
    return typeList;
  }

  public String getBasicAttributeValue(String xpath, String attribute) {
    return XPPUtil.getBasicAttributeValue(file, xpath, attribute);
  }
  
  public String findAttributeValue(String xpath, String attribute) {
    return XPPUtil.findAttributeValue(file, xpath, attribute);
  }

  public String getResolvedString(String xpath) {
    String rv = file.getStringValue(xpath);
    if (rv == null) {
      rv = resolved().getStringValue(xpath);
    }
    return rv;
  }

  /**
   * Determine whether a particular section has been done
   *
   * @param where
   *            the name of the section, i.e. LDMLConstants.IDENTITY
   * @return true if this part has already been processed, otherwise
   *         false. If false, it will return true the next time called.
   */
  public boolean beenHere(String where) {
    if (alreadyDone.contains(where)) {
      return true;
    }

    alreadyDone.add(where);
    return false;
  }

  public boolean isPathNotConvertible(String xpath) {
    return isPathNotConvertible(file, xpath);
  }

  public boolean isPathNotConvertible(CLDRFile f, String xpath) {
    String alt = XPPUtil.getBasicAttributeValue(f, xpath, "alt");
    if (alt != null) {
      return true;
    }
    return !services.xpathListContains(f.getFullXPath(xpath)) && f.isHere(xpath);
  }

  // ====== DOM compatibility
  // This cache is set/retrieved by LDML2ICUConverter.getDocumentPair(LDML2ICUInputLocale)
  
  private DocumentPair docPair = null;

  public DocumentPair getDocumentPair() {
    return docPair;
  }

  public void setDocumentPair(DocumentPair docPair) {
    this.docPair = docPair;
  }
}