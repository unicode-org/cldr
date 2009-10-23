// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;
import org.unicode.cldr.icu.ResourceSplitter.ResultInfo;
import org.unicode.cldr.icu.ResourceSplitter.SplitInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class ICUWriter {
  private static final String LINESEP = System.getProperty("line.separator");
  private static final String BOM = "\uFEFF";
  private static final String CHARSET = "UTF-8";

  private final String dstDirName;
  private final ICULog log;
  private final ResourceSplitter splitter;

  ICUWriter(String dstDirName, ICULog log, ResourceSplitter splitter) {
    this.dstDirName = dstDirName;
    this.log = log;
    this.splitter = splitter;
  }

//  private static final ResourceSplitter debugSplitter;
//  static {
//    List<SplitInfo> splitInfos = new ArrayList<SplitInfo>();
//    splitInfos.add(new SplitInfo("/Languages", "lang"));
//    splitInfos.add(new SplitInfo("/LanguagesShort", "lang"));
//    splitInfos.add(new SplitInfo("/Scripts", "lang"));
//    splitInfos.add(new SplitInfo("/Types", "lang"));
//    splitInfos.add(new SplitInfo("/Variants", "lang"));
//    splitInfos.add(new SplitInfo("/codePatterns", "lang"));
//    splitInfos.add(new SplitInfo("/Countries", "terr", "/Territories"));
//    splitInfos.add(new SplitInfo("/Currencies", "curr"));
//    splitInfos.add(new SplitInfo("/CurrencyPlurals", "curr"));
//    splitInfos.add(new SplitInfo("/CurrencyUnitPatterns", "curr"));
//    splitInfos.add(new SplitInfo("/zoneStrings", "zone"));
//   
//    debugSplitter = new ResourceSplitter("/tmp/ldml", splitInfos);
//  }
  
  public void writeResource(Resource res, String sourceInfo) {
    if (splitter == null) {
      String outputFileName = dstDirName + "/" + res.name + ".txt";
      writeResource(res, sourceInfo, outputFileName);
    } else {
      File rootDir = new File(dstDirName);
      List<ResultInfo> result = splitter.split(rootDir, (ResourceTable) res);

      for (ResultInfo info : result) {
        res = info.root;
        String outputFileName = info.directory.getAbsolutePath() + "/" + res.name + ".txt";
        writeResource(res, sourceInfo, outputFileName);
      }
    }
  }

  private void writeResource(Resource set, String sourceInfo, String outputFileName) {
    try {
      log.log("Writing " + outputFileName);
      FileOutputStream file = new FileOutputStream(outputFileName);
      BufferedOutputStream writer = new BufferedOutputStream(file);
      writeHeader(writer, sourceInfo);

      for (Resource res = set; res != null; res = res.next) {
        res.sort();
      }

      for (Resource res = set; res != null; res = res.next) {
        res.write(writer, 0, false);
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
    } catch (Exception ie) {
      log.error("Could not write resource." + ie.toString(), ie);
      if (!new File(outputFileName).delete()) {
        log.error("Failed to delete file");
      }
      System.exit(1);
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
}