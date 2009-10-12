package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public abstract class SimpleLDMLConverter {
  protected final ICULog log;
  protected final String fileName;
  protected final String supplementalDir;
  protected final String tableName;

  public SimpleLDMLConverter(ICULog log, String fileName, String supplementalDir,
      String tableName) {
    this.log = log;
    this.fileName = fileName;
    this.supplementalDir = supplementalDir;
    this.tableName = tableName;
  }

  public void convert(ICUWriter writer) {
    Document document = createDocument();

    log.log("Processing " + fileName);
    Resource res = parseDocument(document, fileName);
  
    if (res != null && ((ResourceTable)res).first != null) {
      writer.writeResource(res, fileName);
    }
  }

  public Document createDocument() {
    FilenameFilter filter = createDocumentFilter();
  
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

    String dirPath = myDir.getAbsolutePath();
    Document doc = null;
    for (String fileName : files) {
      try {
        log.info("Parsing document " + fileName);
        String filePath = dirPath + File.separator + fileName;
        Document child = LDMLUtilities.parse(filePath, false);
        if (doc == null) {
          doc = child;
          continue;
        }
        StringBuilder xpath = new StringBuilder();
        LDMLUtilities.mergeLDMLDocuments(doc, child, xpath, fileName, dirPath, true, false);
      } catch (Throwable se) {
        log.error("Parsing: " + fileName + " " + se.toString(), se);
        System.exit(1);
      }
    }
  
    return doc;
  }

  protected FilenameFilter createDocumentFilter() {
    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        if (name.matches(fileName)) {
          return true;
        }
        return false;
      }
    };
    return filter;
  }
  
  protected Resource parseDocument(Node root, String sourceInfo) {
    log.setStatus(sourceInfo);
    ResourceTable table = new ResourceTable();
    Resource current = null;
    StringBuilder xpath = new StringBuilder();
    xpath.append("//");
    xpath.append(LDMLConstants.SUPPLEMENTAL_DATA);
    table.name = tableName;
    table.annotation = ResourceTable.NO_FALLBACK;
    int savedLength = xpath.length();
    for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      if (node.getNodeName().equals(LDMLConstants.SUPPLEMENTAL_DATA)) {
        // Stop iterating over top-level elements, restart iterating over elements
        // under supplementalData.
        node = node.getFirstChild();
        continue;
      }
      
      Resource res = parseElement(node, xpath);
  
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
      xpath.delete(savedLength, xpath.length());
    }
  
    return table;
  }
  
  protected Resource parseElement(Node node, StringBuilder xpath) {
    String name = node.getNodeName();
    Resource res = null;
    if (name.equals(tableName)) {
      res = parseInfo(node, xpath);
    } else if (name.equals(LDMLConstants.VERSION) || name.equals(LDMLConstants.GENERATION)) {
      // ignore
    } else {
      log.error("Unknown element " + LDML2ICUConverter.getXPath(node, xpath).toString());
      System.exit(-1);
    }
    return res;
  }

  protected abstract Resource parseInfo(Node root, StringBuilder xpath);
}
