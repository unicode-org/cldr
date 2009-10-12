// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import com.ibm.icu.util.ULocale;

import static org.unicode.cldr.icu.ICUID.*;

import org.unicode.cldr.ant.CLDRConverterTool.Alias;
import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.icu.LDML2ICUConverter.LDMLServices;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class ICUWriter {
  private static final String LINESEP = System.getProperty("line.separator");
  private static final String BOM = "\uFEFF";
  private static final String CHARSET = "UTF-8";

  private static final String DEPRECATED_LIST = "icu-config.xml & build.xml";

  private final String dstDirName;
  private final ICULog log;

  ICUWriter(String dstDirName, ICULog log) {
    this.dstDirName = dstDirName;
    this.log = log;
  }

  public void writeResource(Resource res, String sourceFileName) {
    String outputFileName = dstDirName + "/" + res.name + ".txt";
    writeResource(res, sourceFileName, outputFileName);
  }

  public void writeResource(Resource set, String sourceFileName, String outputFileName) {
    try {
      log.log("Writing: " + outputFileName);
      FileOutputStream file = new FileOutputStream(outputFileName);
      BufferedOutputStream writer = new BufferedOutputStream(file);
      writeHeader(writer, sourceFileName);

      Resource current = set;
      while (current != null) {
        current.sort();
        current = current.next;
      }

      current = set;
      while (current != null) {
        current.write(writer, 0, false);
        current = current.next;
      }
      writer.flush();
      writer.close();
    } catch (Resource.MalformedResourceError mre) {
      String where = set.findResourcePath(mre.offendingResource);
      log.error("Could not write resource " + where + ". " + mre.toString(), mre);
      if (!new File(outputFileName).delete()) {
        log.error("Failed to delete file");
      }
      System.exit(1);
      return; // NOTREACHED
    } catch (Exception ie) {
      log.error("Could not write resource." + ie.toString(), ie);
      if (!new File(outputFileName).delete()) {
        log.error("Failed to delete file");
      }
      System.exit(1);
      return; // NOTREACHED
    }
  }

  private void writeLine(OutputStream writer, String line) {
    try {
      byte[] bytes = line.getBytes(CHARSET);
      writer.write(bytes, 0, bytes.length);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  private void writeHeader(OutputStream writer, String fileName) {
    writeBOM(writer);
    Calendar c = Calendar.getInstance();
    StringBuilder buffer = new StringBuilder();
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
      log.error(e.getMessage(), e);
    }
  }

  private void writeSimpleLocaleAlias(
      String fileName, String fromLocale, String toLocale, String comment) {
    String dstFilePath = dstDirName + "/" + fileName;
    Resource set = null;
    try {
      ResourceTable table = new ResourceTable();
      table.name = fromLocale;
      if (toLocale != null) {
        ResourceString str = new ResourceString();
        str.name = "\"%%ALIAS\"";
        str.val = toLocale;
        table.first = str;
      } else {
        ResourceString str = new ResourceString();
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
      log.error("building synthetic locale tree for " + dstFilePath, e);
      System.exit(1);
    }

    String info;
    if (toLocale != null) {
      info = "(alias to " + toLocale.toString() + ")";
    } else {
      info = comment;
    }
    log.info("Writing synthetic: " + dstFilePath + " " + info);
    
    writeResource(set, DEPRECATED_LIST, dstFilePath);
  }

  public void writeDeprecated(LDMLServices services, File depDir, File dstDir, List<String> emptyLocaleList,
      Map<String, Alias> aliasMap, List<String> aliasLocaleList, boolean parseDraft,
      boolean parseSubLocale) {
    new DeprecatedConverter(log, services, depDir, dstDir).write(this, emptyLocaleList, aliasMap, aliasLocaleList, parseDraft, parseSubLocale);
  }

  public static class DeprecatedConverter {
    private final ICULog log;
    private final LDMLServices services;
    private final File depDir;
    private final File dstDir;
    
    public DeprecatedConverter(ICULog log, LDMLServices services, File depDir, File dstDir) {
      this.log = log;
      this.services = services;
      this.depDir = depDir;
      this.dstDir = dstDir;
    }
    
    public void write(ICUWriter writer, List<String> emptyLocaleList, Map<String, Alias> aliasMap,
        List<String> aliasLocaleList, boolean parseDraft, boolean parseSubLocale) {
    String myTreeName = depDir.getName();
    final File[] destFiles = dstDir.listFiles();

    // parse a bunch of locales?
    boolean parseThem = parseDraft || parseSubLocale;

    // ex: "ji" -> "yi"
    TreeMap<String, String> fromToMap = new TreeMap<String, String>();

    // ex:  "th_TH_TRADITIONAL" -> "@some xpath.."
    TreeMap<String, String> fromXpathMap = new TreeMap<String, String>();

    // ex:  "mt.xml" -> File .  Ordinary XML source files
    Map<String, File> fromFiles = new TreeMap<String, File>();

    // ex:  "en_US.xml" -> File .  empty files generated by validSubLocales
    Map<String, File> emptyFromFiles = new TreeMap<String, File>();

    // ex:  th_TH_TRADITIONAL.xml -> File  Files generated directly from the alias list
    // (no XML actually exists).
    Map<String, File> generatedAliasFiles = new TreeMap<String, File>();

    // ex: zh_MO.xml -> File  Files which actually exist in LDML and contain aliases
    Map<String, File> aliasFromFiles = new TreeMap<String, File>();

    // en -> "en_US en_GB ..."
    TreeMap<String, String> validSubMap = new TreeMap<String, String>();

    // for in -> id where id is a synthetic alias
    TreeMap<String, String> maybeValidAlias = new TreeMap<String, String>();

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
          if (dest.indexOf(name) == 0) {
            return true;
          }
        }

        return false;
      }
    };

    File inFiles[] = depDir.listFiles(myFilter);

    int nrInFiles = inFiles.length;
    if (parseThem) {
      log.setStatus(null);
      log.log("Parsing: " + nrInFiles + " LDML locale files to check "
          + (parseDraft ? "draft, " : "")
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
      log.setStatus(localeName);
      if (parseThem) {
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
                log.info(localeName + " <- " + vsl);
              }
            }
          }
        } catch (Throwable t) {
          log.error(t.getMessage(), t);
          System.exit(-1); // TODO: should be full 'parser error'stuff.
        }
      }

      if (!localeName.equals("root")) {
        if (thisOK) {
          System.out.print("."); // regular file
          fromFiles.put(inFiles[i].getName(), inFiles[i]); // add to hash
        } else {
          if (services.isDraftStatusOverridable(localeName)) {
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
      log.setStatus(null);
    }

    if (parseThem == true) {
      // end the debugging line
      System.out.println();
    }
    
    // End of parsing all XML files.
    if (emptyLocaleList != null && emptyLocaleList.size() > 0) {
      for (int i = 0; i < emptyLocaleList.size(); i++) {
        String loc = emptyLocaleList.get(i);
        writer.writeSimpleLocaleAlias(
            loc + ".txt", loc, null, "empty locale file for dependency checking");
        // we do not want these files to show up in installed locales list!
        generatedAliasFiles.put(loc + ".xml", new File(depDir, loc + ".xml"));
      }
    }

    // interpret the deprecated locales list
    if (aliasMap != null && aliasMap.size() > 0) {
      for (Iterator<String> i = aliasMap.keySet().iterator(); i.hasNext();) {
        String from = i.next();
        log.setStatus(String.valueOf(from));
        Alias value = aliasMap.get(from);
        String to = value.to;
        String xpath = value.xpath;
        if (to.indexOf('@') != -1 && xpath == null) {
          log.error("Malformed alias - '@' but no xpath: from=\"" + from + "\" to=\"" + to + "\"");
          System.exit(-1);
          return; // NOTREACHED
        }

        if (from == null || to == null) {
          log.error("Malformed alias - no 'from' or 'to': from=\"" + from + "\" to=\"" + to + "\"");
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
        String fromLocaleName = fromLocale.toString();
        if (!fromFiles.containsKey(toFileName + ".xml")) {
          maybeValidAlias.put(toFileName, from);
        } else {
          generatedAliasFiles.put(from, new File(depDir, from + ".xml"));
          fromToMap.put(fromLocale.toString(), to);
          if (xpath != null) {
            fromXpathMap.put(fromLocale.toString(), xpath);
            
            CLDRFile fakeFile = CLDRFile.make(fromLocaleName);
            fakeFile.add(xpath, "");
            fakeFile.freeze();
            Resource res = services.parseBundle(fakeFile);
            
            if (res != null && ((ResourceTable) res).first != null) {
              res.name = fromLocaleName;
              writer.writeResource(res, DEPRECATED_LIST);
            } else {
              // parse error?
              log.error("Failed to write out alias bundle " + fromLocaleName + " from " + xpath
                  + " - XML list follows:");
              fakeFile.write(new PrintWriter(System.out));
            }

          } else {
            String toLocaleName = new ULocale(to).toString();
            writer.writeSimpleLocaleAlias(from + ".txt", fromLocaleName, toLocaleName, null);
          }
        }
      }
      log.setStatus(null);
    }

    if (aliasLocaleList != null && aliasLocaleList.size() > 0) {
      for (int i = 0; i < aliasLocaleList.size(); i++) {
        String source = aliasLocaleList.get(i) + ".xml";
        log.setStatus(source);
        if (!fromFiles.containsKey(source)) {
          log.warning("Alias file named in deprecates list but not present. Ignoring alias entry.");
        } else {
          aliasFromFiles.put(source, new File(depDir, source));
          fromFiles.remove(source);
        }
      }
      log.setStatus(null);
    }

    // Post process: calculate any 'valid sub locales' (empty locales
    // generated due to validSubLocales attribute)
    if (!validSubMap.isEmpty() && parseSubLocale) {
      log.info("Writing valid sub locs for: " + validSubMap.toString());

      for (Iterator<String> e = validSubMap.keySet().iterator(); e.hasNext();) {
        String actualLocale = e.next();
        log.setStatus(actualLocale + ".xml");
        String list = validSubMap.get(actualLocale);
        String validSubs[] = list.split(" ");
        for (int i = 0; i < validSubs.length; i++) {
          String aSub = validSubs[i];
          String testSub;

          for (testSub = aSub;
               testSub != null && !testSub.equals("root") && !testSub.equals(actualLocale);
               testSub = LDMLUtilities.getParent(testSub)) {

            if (fromFiles.containsKey(testSub + ".xml")) {
              log.warning(
                  "validSubLocale=" + aSub + " overridden because  " + testSub + ".xml  exists.");
              testSub = null;
              break;
            }

            if (generatedAliasFiles.containsKey(testSub)) {
              log.warning(
                  "validSubLocale=" + aSub + " overridden because an alias locale " + testSub
                  + ".xml  exists.");
              testSub = null;
              break;
            }
          }

          if (testSub != null) {
            emptyFromFiles.put(aSub + ".xml", new File(depDir, aSub + ".xml"));
            if (maybeValidAlias.containsKey(aSub)) {
              String from = maybeValidAlias.get(aSub);
              writer.writeSimpleLocaleAlias(from + ".txt", from, aSub, null);
              maybeValidAlias.remove(aSub);
              generatedAliasFiles.put(from, new File(depDir, from + ".xml"));
            }
            writer.writeSimpleLocaleAlias(
                aSub + ".txt", aSub, null, "validSubLocale of \"" + actualLocale + "\"");
          }
        }
      }
      log.setStatus(null);
    }

    if (!maybeValidAlias.isEmpty()) {
      Set<String> keys = maybeValidAlias.keySet();
      Iterator<String> iter = keys.iterator();
      while (iter.hasNext()) {
        String to = iter.next();
        String from = maybeValidAlias.get(to);
        log.warning("Alias from \"" + from
            + "\" not generated, because it would point to a nonexistent LDML file " + to + ".xml");
      }
    }
    
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
      getBrkCtdFilesList(depDir, brkArray);
    }
    
    writeResourceMakefile(myTreeName, generatedAliasList, aliasFilesList,
            inFileText, emptyFileText, brkArray[0], brkArray[1]);

    log.setStatus(null);
    log.log("WriteDeprecated done.");
  }

  private String fileMapToList(Map<String, File> files) {
    return fileIteratorToList(files.values().iterator());
  }

  private String[] getBrkCtdFilesList(File directory, String[] brkArray) {
    // read all xml files in the directory and create ctd file list and brk file list
    FilenameFilter myFilter = new FilenameFilter() {
      public boolean accept(File f, String name) {
        return !f.isFile()
            && name.endsWith(".xml")
            && !name.startsWith("supplementalData"); // not a locale
        // root is implied, will be included elsewhere.
      }
    };

    File[] files = directory.listFiles(myFilter);
    StringBuilder brkList = new StringBuilder();
    StringBuilder ctdList = new StringBuilder();

    // open each file and create the list of files for brk and ctd
    for (File file : files) {
      String fileName = file.getName();
      String filePath = file.getAbsolutePath();
      Document doc = LDMLUtilities.parse(filePath, false);
      log.setStatus(fileName);
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

              String val = LDMLUtilities.getAttributeValue(cn, ICU_DEPENDENCY);
              if (val != null) {
                brkList.append(val.substring(0, val.indexOf('.')));
                brkList.append(".txt ");
              }
            } else {
              log.error("Encountered unknown <" + name + "> subelement: " + cnName);
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
              log.error("Encountered unknown <" + name + "> subelement: " + cnName);
              System.exit(-1);
            }
          }
        } else {
          log.error("Encountered unknown <" + doc.getNodeName() + "> subelement: " + name);
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

  private void writeResourceMakefile(
      String myTreeName, String generatedAliasList, String aliasFilesList, String inFileText,
      String emptyFileText, String brkFilesList, String ctdFilesList) {

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
      log.error("Unknown tree name in writeResourceMakefile: " + myTreeName);
      System.exit(-1);
    }

    String resfiles_mk_name = dstDir + "/" + shortstub + "files.mk";
    try {
      log.info("Writing ICU build file: " + resfiles_mk_name);
      PrintStream resfiles_mk = new PrintStream(new FileOutputStream(resfiles_mk_name));
      Calendar c = Calendar.getInstance();
      resfiles_mk.println(
          "# *   Copyright (C) 1998-" + c.get(Calendar.YEAR) + ", International Business Machines");
      resfiles_mk.println("# *   Corporation and others.  All Rights Reserved.");
      resfiles_mk.println(stub + "_CLDR_VERSION = " + CLDRFile.GEN_VERSION);
      resfiles_mk.println("# A list of txt's to build");
      resfiles_mk.println("# Note: ");
      resfiles_mk.println("#");
      resfiles_mk.println("#   If you are thinking of modifying this file, READ THIS.");
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
      log.error("While writing " + resfiles_mk_name, e);
      System.exit(1);
    }
  }

  private String fileIteratorToList(Iterator<File> files) {
    String out = "";
    int i = 0;
    while (files.hasNext()) {
      File f = files.next();
      if ((++i % 5) == 0) {
        out = out + "\\" + LINESEP;
      }
      out = out + (i == 0 ? " " : " ") + f.getName().substring(0, f.getName().indexOf('.')) + ".txt";
    }
    return out;
  }
  }
}