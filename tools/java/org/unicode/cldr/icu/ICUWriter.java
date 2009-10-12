// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Iterator;

class ICUWriter {
  private static final String LINESEP = System.getProperty("line.separator");
  private static final String BOM = "\uFEFF";
  private static final String CHARSET = "UTF-8";

  private final String dstDirName;
  private final ICULog log;

  ICUWriter(String dstDirName, ICULog log) {
    this.dstDirName = dstDirName;
    this.log = log;
  }

  public void writeResource(Resource res, String sourceInfo) {
    String outputFileName = dstDirName + "/" + res.name + ".txt";
    writeResource(res, sourceInfo, outputFileName);
  }

  public void writeResource(Resource set, String sourceInfo, String outputFileName) {
    try {
      log.log("Writing: " + outputFileName);
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

  public void writeSimpleLocaleAlias(
      String fileName, String fromLocale, String toLocale, String sourceInfo, String comment) {
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
    
    writeResource(set, sourceInfo, dstFilePath);
  }

  public static String fileIteratorToList(Iterator<File> files) {
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