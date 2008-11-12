/*
 **********************************************************************
 * Copyright (c) 2002-2004, Google Inc, Unicode Inc, and others.  All Rights Reserved.
 **********************************************************************
 */
package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;

import org.unicode.cldr.util.UnicodeMap;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer.Mode;
import com.ibm.icu.util.VersionInfo;

public class GenerateNormalizeForMatch {

  private static final boolean DO_NFC = false; // always include NFC mappings, if nothing else?
  
  // Command choices
  enum ListStyle {ALL, SHOW_AGE, ONLY_OLD}
  private static ListStyle LIST_STYLE = ListStyle.ALL;

  // Useful objects
  private static final UnicodeSet U50 = (UnicodeSet) new UnicodeSet("[:age=5.0:]").freeze();
  private static final UnicodeSet ASSIGNED = (UnicodeSet) new UnicodeSet("[:ASSIGNED:]").freeze();
  private static final DateFormat ISO_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss v");
  private static final Charset UTF8 = Charset.forName("utf-8");

  private static final int DEBUG_CODE_POINT = -1; // eg 0xFDFA
  
  // Special mappings
  private static UnicodeMap SPECIAL_MAPPINGS = new UnicodeMap();

  // Fixes to match Jim's names
  private static UnicodeMap JIM_NAMES = new UnicodeMap();
  static {
    JIM_NAMES.putAll(new UnicodeSet("[:block=CJK Unified Ideographs:]"), "<CJK Ideograph>");
    JIM_NAMES.putAll(new UnicodeSet("[:block=CJK Unified Ideographs Extension A:]"), "<CJK Ideograph Extension A>");
    JIM_NAMES.putAll(new UnicodeSet("[:block=CJK Unified Ideographs Extension B:]"), "<CJK Ideograph Extension B>");
    JIM_NAMES.freeze();
  }
  
  static PrintWriter LOG_WRITER = null;
  
  /**
   * Generate new files or reformat old ones, depending on options
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    String sourceFile = null;
    String targetFile = null;
    String logFile = null;
    boolean fix = false;
    for (int i = 0; i < args.length; ++i) {
      String arg = args[i];
      if (arg.equals("-i")) {
        sourceFile = args[++i];
      } else if (arg.equals("-o")) {
        targetFile = args[++i];
      } else if (arg.equals("-l")) {
        logFile = args[++i];
      } else if (arg.equals("fix")) {
        fix = true;
      } else {
        throw new IllegalArgumentException("Unknown option: " + arg + "\r\n" +
        		"-o <targetFile>\r\n" +
        		"-i <inputFile>\r\n" +
        		"-l <logFile>");
      }
    }
    if (logFile == null) {
      LOG_WRITER = new PrintWriter(System.out);
    } else {
      LOG_WRITER = openUTF8Writer(logFile);
      LOG_WRITER.write(0xFEFF);
    }
    if (fix) {
      fixOld(sourceFile, targetFile);
    } else {
      generateMappings(sourceFile, targetFile);
    }
    System.out.println("DONE");
  }
  

  /**
   * Generate new mapping file, based on exceptions file. See normalizeForMatchExceptions.txt for the format.
   * @param specialMappingsFile TODO
   * @param outputFile TODO
   * @throws IOException
   */
  static void generateMappings(String specialMappingsFile, String outputFile) throws IOException {
    PrintWriter out = openUTF8Writer(outputFile);
    out.println("# Generated from: " + new File(specialMappingsFile).getName());
    out.println("# Date: " + ISO_DATE.format(new Date()));
    out.println("#");

    // special mappings
    getSpecialMappings(specialMappingsFile);

    int linecount = 0;
    UnicodeMap mappings = new UnicodeMap();
    for (UnicodeSetIterator it = new UnicodeSetIterator(ASSIGNED); it.next();) {
      if (LIST_STYLE == ListStyle.ONLY_OLD && !U50.contains(it.codepoint)) {
        continue;
      }
      String str = it.getString();
      String special = (String) SPECIAL_MAPPINGS.getValue(it.codepoint);
      String other = getRemapped(it.codepoint, special);
      if (other.equals(str)) continue;
      mappings.put(it.codepoint, other);
    }

    // print them
    for (UnicodeSetIterator it = new UnicodeSetIterator(mappings.keySet()); it.next();) {
      writeMapping(out, it.getString(), (String) mappings.getValue(it.codepoint));
      linecount++;
    }
    out.close();
  }

  /**
   * Remap a string based on a special flag (usually gotten from the special_mappings, but
   * broken out so that we can see the effects of new rules).
   * @param codepoint
   * @param special either "exclude", or "caseonly", or null, or actual result.
   * @return
   */
  private static String getRemapped(int codepoint, String special) {
    String other = UTF16.valueOf(codepoint);
    if (codepoint == DEBUG_CODE_POINT) {
      System.out.println("?debug?");
    }
    if (special == null) {
      other = normalizeAndCaseFold(other, Normalizer.NFKC);
    } else if (special.equalsIgnoreCase("exclude")) {
      if (DO_NFC) {
        other = Normalizer.normalize(other, Normalizer.NFC, 0);
      }
    } else if (special.equalsIgnoreCase("caseonly")) {
      other = normalizeAndCaseFold(other, Normalizer.NFC);
    } else {
      other = special;
    }
    return other;
  }

  /**
   * Do the standard normalization & casefold
   * @param other
   * @return
   */
  private static String normalizeAndCaseFold(String other, Mode normalizerType) {
    String defaultChange = Normalizer.normalize(other, normalizerType, 0);
    defaultChange = UCharacter.foldCase(defaultChange, true);
    defaultChange = Normalizer.normalize(defaultChange, normalizerType, 0);
    return defaultChange;
  }
  
  /**
   * Write out a mapping line
   * @param out
   * @param source
   * @param target
   */
  private static void writeMapping(PrintWriter out, String source, String target) {
    String otherName = jimName(target);
    String age = (LIST_STYLE == ListStyle.SHOW_AGE) && (!U50.containsAll(source) || !U50.containsAll(target)) 
      ? showVersion(getNewest(source + target)) + " " : "";
    out.println(hex(source," ")
            + " ; " + hex(target," ").replace(",", " ")
            + " ; # " + age + UCharacter.getName(source, " + ")
            + " => " + otherName
    );
  }

  /**
   * Show the version in a nice format
   * @param newest
   * @return
   */
  private static String showVersion(VersionInfo newest) {
    return "[" + newest.getMajor() + "." + newest.getMinor() 
    + (newest.getMilli() == 0 ? "" : "." + newest.getMilli())
    + "]";
  }


  /**
   * Get the age of the newest character in the string.
   * @param string
   * @return
   */
  private static VersionInfo getNewest(String string) {
    int cp;
    VersionInfo oldest = null;
    for (int i = 0; i < string.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(string, i);
      VersionInfo age = UCharacter.getAge(cp);
      if (oldest == null || oldest.compareTo(age) < 0) {
        oldest = age;
      }
    }
    return oldest;
  }

  /**
   * Load the special mappings from a file.
   * @param filename TODO
   * @throws IOException
   */
  private static void getSpecialMappings(String filename) throws IOException {
    SPECIAL_MAPPINGS.putAll(0,0x10FFFF, "exclude");
    BufferedReader in = openUTF8Reader(filename);
    int lineNumber = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      getMappingFromSemiLine(lineNumber++, line, true);
    }
    in.close();
    SPECIAL_MAPPINGS.freeze();
  }

  /**
   * Process each special mapping line from a file
   * @param lineNumber
   * @param line
   * @param mappings
   * @param skipIfIdentical
   */
  private static void getMappingFromSemiLine(int lineNumber, String line, boolean skipIfIdentical) {
    line = line.trim();
    if (line.startsWith("\uFEFF")) {
      line = line.substring(1);
    }
    LOG_WRITER.println(lineNumber + ":\t" + line);
    int commentPos = line.indexOf("#");
    if (commentPos >= 0) {
      line = line.substring(0,commentPos);
    }
    line = line.trim();
    if (line.length() == 0) return;
    if (line.startsWith("@")) {
      line = line.toUpperCase().replace(" ", "").replace("\t", ""); // remove spaces, tabs
      if (line.startsWith("@LIST=")) {
        LIST_STYLE = ListStyle.valueOf(line.substring(6).toUpperCase());
      } else {
        throw new IllegalArgumentException("Illegal command: " + line); 
      }
      return;
    }

    String[] pieces = line.split("\\s*;\\s*");
    if (pieces.length == 1) {
      throw new IllegalArgumentException("Line without target: " + line);
    }
    UnicodeSet source = new UnicodeSet();
    if (UnicodeSet.resemblesPattern(pieces[0], 0)) {
      source.applyPattern(pieces[0]);
    } else {
      String[] starts = pieces[0].split("\\s*-\\s*");
      int start = Integer.parseInt(starts[0], 16);
      int end = starts.length == 1 ? start : Integer.parseInt(starts[1], 16);
      source.add(start,end);
    }
    
    final String target = pieces[1]; // pieces.length == 1 ? "" : pieces[1];
    UnicodeSet targetFilter = pieces.length < 3 || pieces[2].length() == 0 ? new UnicodeSet(ASSIGNED) : new UnicodeSet(pieces[2]);
    addExceptions(source, target, targetFilter);
  }

  /**
   * Add exceptions based on a line from the special mapping files
   * @param mappings
   * @param source
   * @param target
   * @param targetFilter
   */
  private static void addExceptions(UnicodeSet source, String target, UnicodeSet targetFilter) {
    String oldTarget = target;
    // remap options
    
    if (target.equalsIgnoreCase("exclude") || target.equalsIgnoreCase("caseonly")) {
      // nothing
    } else if (target.equalsIgnoreCase("delete")) {
      target = "";
    } else if (target.equalsIgnoreCase("ok")) {
      target = null;
    } else {
      target = fromHex(target);
    }
    
    // show a sample of what changed
    // and absorb new mappings, where they make a difference
    UnicodeSet remainder = new UnicodeSet();
    int count = 0;
    UnicodeSet affected = new UnicodeSet(source).retainAll(ASSIGNED);
    
    for (UnicodeSetIterator it = new UnicodeSetIterator(affected); it.next();) {
      String whatWedGet = getRemapped(it.codepoint, target);
      if (!targetFilter.containsAll(whatWedGet)) {
        continue;
      }
      if (it.codepoint == DEBUG_CODE_POINT) {
        System.out.println("?debug?");
      }
      
      String special = (String) SPECIAL_MAPPINGS.getValue(it.codepoint);
      String ifWeDidntHaveTarget = getRemapped(it.codepoint, special);
      
      if (whatWedGet.equals(ifWeDidntHaveTarget)) {
        // skip cases where we already covered this mapping
        continue;
      }
      SPECIAL_MAPPINGS.put(it.codepoint, target);
      
      // show what we are doing
      
      if (++count > 6) {
        remainder.add(it.codepoint);
        continue;
      }
      final String string = it.getString();
      //final String normalizedAndCaseFolded = normalizeAndCaseFold(string);
      //final String caseFolded = UCharacter.foldCase(string, true);

      LOG_WRITER.println("\t" + codeAndName(string)
              + "\t;\t" + oldTarget 
              + "\t; #\t" + ifWeDidntHaveTarget
              + "\t=>\t" + whatWedGet 
              );
    }
    if (count == 0) {
      LOG_WRITER.println("\tPOINTLESS RULE");
    } else if (remainder.size() != 0) {
      LOG_WRITER.println("\t..." + remainder.toPattern(false) + "\t;\t" + oldTarget + "\t;");
      LOG_WRITER.println("\tCharacters affected: " + count);
    }
  }
  
  /**
   * Printing convenience function
   * @param defaultChange
   * @return
   */
  private static String codeAndName(String defaultChange) {
    return hex(defaultChange," ") + " - " + UCharacter.getName(defaultChange, " ");
  }

  /**
   * Printing convenience function
   * @param spaceDelimitedHex
   * @return
   */
  private static String fromHex(String spaceDelimitedHex) {
    spaceDelimitedHex = spaceDelimitedHex.trim();
    if (spaceDelimitedHex.length() == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    String[] pieces = spaceDelimitedHex.split("\\s+");
    for (String piece : pieces) {
      result.append(UTF16.valueOf(Integer.parseInt(piece, 16)));
    }
    return result.toString();
  }

  /**
   * Printing convenience function
   * @param other
   * @return
   */
  private static String jimName(String other) {
    String otherName = UTF16.countCodePoint(other) != 1 ? null 
            : (String) JIM_NAMES.getValue(UTF16.charAt(other, 0));
    if (otherName == null) {
      otherName = UCharacter.getName(other, " + ");
    }
    return otherName;
  }

  /**
   * Printing convenience function
   * @param s
   * @param separator
   * @return
   */
  public static String hex(String s, String separator) {
    StringBuffer result = new StringBuffer();
    int cp = 0;
    for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
      if (i != 0) result.append(separator);
      cp = UTF16.charAt(s, i);
      result.append(com.ibm.icu.impl.Utility.hex(cp, 4));
    }
    return result.toString();
  }
  

  private static PrintWriter openUTF8Writer(String filename) throws FileNotFoundException {
    return new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), UTF8));
  }
  
  private static BufferedReader openUTF8Reader(String filename) throws FileNotFoundException {
    return new BufferedReader(new InputStreamReader(new FileInputStream(filename), UTF8));
  }
  
  /**
   * Used to reformat files into consistent form.
   * @param sourceFile
   * @param targetFile
   * @throws IOException
   */
  private static void fixOld(String sourceFile, String targetFile) throws IOException {
    UnicodeMap oldMap = new UnicodeMap();
    BufferedReader in = openUTF8Reader(sourceFile);
    int lineNumber = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      getMappingFromSemiLine(lineNumber++, line, false);
    }
    in.close();
    PrintWriter out = openUTF8Writer(targetFile);
    for (UnicodeSetIterator it = new UnicodeSetIterator(oldMap.keySet()); it.next();) {
      String str = it.getString();
      String other = (String) oldMap.getValue(it.codepoint);
      writeMapping(out, str, other);
    }
    out.close();
  }
}
