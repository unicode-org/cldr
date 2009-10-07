// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.LDML2ICUConverter.LDMLServices;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class LDML2ICUInputLocale {
  private final LDMLServices services;
  private boolean notOnDisk;
  private String locale;
  private CLDRFile rawFile;
  private CLDRFile file;
  private CLDRFile specialsFile;
  private CLDRFile resolved;

  boolean isNotOnDisk() {
    return notOnDisk;
  }

  String getLocale() {
    return locale;
  }

  CLDRFile getFile() {
    return file;
  }

  CLDRFile getSpecialsFile() {
    return specialsFile;
  }

  @Override
  public String toString() {
    return "{"
      + "notOnDisk=" + isNotOnDisk()
      + " locale=" + getLocale()
      + " rawFile=" + abbreviated(rawFile)
      + " file=" + abbreviated(getFile())
      + " specialsFile=" + abbreviated(getSpecialsFile())
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
      // System.err.println("** spinning up resolved for " + locale);
      if (cldrFactory() != null) {
        resolved = cldrFactory().make(getLocale(), true, DraftStatus.contributed);
      } else {
        System.err.println("Error: cldrFactory is null in \"resolved()\"");
        System.err.flush();
        System.exit(1);
      }
    }
    return resolved;
  }

  LDML2ICUInputLocale(CLDRFile fromFile, LDMLServices services) {
    this.services = services;
    this.notOnDisk = true;
    this.rawFile = this.file = this.resolved = fromFile;
    this.locale = fromFile.getLocaleID();
  }

  LDML2ICUInputLocale(String locale, LDMLServices services) {
    this.services = services;
    this.locale = locale;
    this.rawFile = cldrFactory().make(locale, false);
    this.specialsFile = getSpecialsFile(locale);
    if (getSpecialsFile() != null) {
      this.file = (CLDRFile) rawFile.cloneAsThawed();
      this.file.putAll(getSpecialsFile(), CLDRFile.MERGE_REPLACE_MINE);
    } else {
      this.file = rawFile; // frozen
    }
  }

  private XPathParts xpp = new XPathParts(null, null);

  Set<String> getByType(String baseXpath, String element) {
    return getByType(baseXpath, element, LDMLConstants.TYPE);
  }

  Set<String> getByType(String baseXpath, String element, String attribute) {
    Set<String> typeList = new HashSet<String >();
    for (Iterator<String> iter = getFile().iterator(baseXpath); iter.hasNext();) {
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
    String fullPath = getFile().getFullXPath(xpath);
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
    String fullPath = getFile().getFullXPath(xpath);
    xpp.set(fullPath);
    for (int j = 1; j <= xpp.size(); j++) {
      String v = xpp.getAttributeValue(0 - j, attribute);
      if (v != null)
        return v;
    }
    return null;
  }

  String getResolvedString(String xpath) {
    String rv = getFile().getStringValue(xpath);
    if (rv == null) {
      rv = resolved().getStringValue(xpath);
      // System.err.println("Falling back:" + xpath + " -> " + rv);
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
    return isPathNotConvertible(getFile(), xpath);
  }

  boolean isPathNotConvertible(CLDRFile f, String xpath) {
    String alt = getBasicAttributeValue(f, xpath, "alt");
    if (alt != null) {
      return true;
    }
    return !xpathListContains(f.getFullXPath(xpath)) && f.isHere(xpath);
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
   */
  Node getTopNode(String topName) {
    StringBuilder xpath = new StringBuilder();
    xpath.append("//ldml");

    Node ldml = null;

    for (ldml = getDocument().getFirstChild(); ldml != null; ldml = ldml.getNextSibling()) {
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

  /**
   * Parse the current locale for DOM.
   */
  Document getDocument() {
    if (isNotOnDisk()) {
      throw new InternalError(
          "Error: this locale (" + getLocale() + ") isn't on disk, can't parse with DOM.");
    }

    if (doc == null) {
      doc = getDocument(getLocale());
    }
    return doc;
  }

  private CLDRFile.Factory cldrFactory() {
    return services.cldrFactory();
  }

  private Document getDocument(String locale) {
    return services.getDocument(locale);
  }

  private CLDRFile getSpecialsFile(String locale) {
    return services.getSpecialsFile(locale);
  }

  private boolean xpathListContains(String xpath) {
    return services.xpathListContains(xpath);
  }

  private void setLdmlVersion(String version) {
    services.setLdmlVersion(version);
  }
}