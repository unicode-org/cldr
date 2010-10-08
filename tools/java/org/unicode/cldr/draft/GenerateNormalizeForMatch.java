/*
 **********************************************************************
 * Copyright (c) 2002-2004, Google Inc, Unicode Inc, and others.  All Rights Reserved.
 **********************************************************************
 */
package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.UnicodeMap;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.Normalizer.Mode;
import com.ibm.icu.util.VersionInfo;

public class GenerateNormalizeForMatch {
  static boolean TABLE = true;

  // Command choices
  enum ListStyle {ALL, SHOW_AGE, ONLY_OLD}
  private static ListStyle LIST_STYLE = ListStyle.ALL;

  // Useful objects
  private static final UnicodeSet U50 = (UnicodeSet) new UnicodeSet("[:age=5.0:]").freeze();
  private static final UnicodeSet ASSIGNED = (UnicodeSet) new UnicodeSet("[:ASSIGNED:]").freeze();
  private static final DateFormat ISO_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss v");
  private static final NumberFormat FORMAT = new DecimalFormat("#,##0");
  public static final Charset UTF8 = Charset.forName("utf-8");

  private static int DEBUG_CODE_POINT = 0x0041; // eg 0xFDFA

  private static final Matcher HEXFORM = Pattern.compile("[0-9A-Fa-f]{4,6}(\\s+[0-9A-Fa-f]{4,6})*").matcher("");

  private static final int DIFF_LIMIT = 10;

  // Fixes to match Jim's names
  private static UnicodeMap<String> JIM_NAMES = new UnicodeMap<String>();
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
    String oldMappingFile = null;
    String frequencyFile = null;
    String logFile = null;
    boolean fix = false;
    for (int i = 0; i < args.length; ++i) {
      String arg = args[i];
      if (arg.equals("-i")) {
        sourceFile = args[++i];
      } else if (arg.equals("-o")) {
        targetFile = args[++i];
      } else if (arg.equals("-d")) {
        oldMappingFile = args[++i];
      } else if (arg.equals("-l")) {
        logFile = args[++i];
        TABLE = logFile.contains(".htm");
      } else if (arg.equals("-f")) {
        frequencyFile = args[++i];
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
      if (TABLE) {
        LOG_WRITER.write("<html>\r\n" +
                "<head>\r\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>\r\n" +
                "<style type='text/css'>\r\n" +
                "table { border: 1px solid blue; border-collapse: collapse; }" +
                "td,th { border: 1px solid blue; vertical-align: top; }" +
                "</style>" +
                "</head>\r\n" +
                "<body>\r\n" +
        "<pre>\r\n");
      }
    }
    if (fix) {
      fixOld(sourceFile, targetFile);
    } else {
      frequencies = new FrequencyData(frequencyFile, true);
      generateMappings(sourceFile, targetFile, oldMappingFile, frequencyFile);
    }
    LOG_WRITER.println("# END");
    if (TABLE) {
      LOG_WRITER.println("</pre></body></html>");
    }
    LOG_WRITER.close();
    System.out.println("DONE");
  }


  private static void logDiffs(UnicodeMap<String> newMappings, String oldMappingFile, String frequencyFile) throws IOException {
    LOG_WRITER.println();
    LOG_WRITER.println("# *** Differences from " + new File(oldMappingFile).getName() + " ***");
    LOG_WRITER.println();
    UnicodeMap<String> diffMappings = new UnicodeMap<String>();
    loadMappings(oldMappingFile, diffMappings, false);


    UnicodeSet newSource = newMappings.keySet();
    UnicodeSet diffSourceSet = diffMappings.keySet();
    UnicodeSet union = new UnicodeSet(newSource).addAll(diffSourceSet);

    UnicodeSet isLetter = new UnicodeSet("[[:L:][:M:][:N:]\\u002B\\u005F\\uFF06\\uFF0B\\uFF3F\\u309B\\u309C\\u30a0]");
    showOrderedList("IsLetter", new UnicodeSet(union).retainAll(isLetter), diffMappings, newMappings, Integer.MAX_VALUE);
    showOrderedList("NOT IsLetter", new UnicodeSet(union).removeAll(isLetter), diffMappings, newMappings, Integer.MAX_VALUE);
  }

  private static void showOrderedList(String title, UnicodeSet charsToShow,
          UnicodeMap<String> oldMappings, UnicodeMap<String> newMappings, int limit) {

    Set<Comparable<? extends Object>[]> ordered = new TreeSet(DOUBLE_STRING_COMP);
    for (UnicodeSetIterator it = new UnicodeSetIterator(charsToShow); it.next();) {
      String oldMapped = getRemapped(it.codepoint, oldMappings);
      String newMapped = getRemapped(it.codepoint, newMappings);
      if (!newMapped.equals(oldMapped)) {
        Long count = frequencies == null ? null : frequencies.getCount(it.codepoint);
        ordered.add(new Comparable[]{count == null ? 0L : count, it.codepoint, oldMapped, newMapped});
      }
    }
    showOrderedList2(title, ordered, limit);
  }
  
  private static String anchorize(String name) {
    return name.replace(' ', '_').toLowerCase();
  }

  private static void showOrderedList2(String title, Collection<Comparable<? extends Object>[]> ordered, int limit) {
    if (ordered.size() == 0) {
      String msg = "NO CHARACTERS CHANGED";
      if (TABLE) {
        msg = "</pre><b>" + msg + "</b><pre>";
      }
      LOG_WRITER.println(msg);
      return;
    }

    if (TABLE) {
      LOG_WRITER.println("</pre>");
    }

    if (title != null) {
      if (TABLE) {
        title = "<h2><a name='" + anchorize(title) + "'>" + title + "</a></h2>\r\n";
      }
      LOG_WRITER.println(title);
    }

    final boolean longEnough = ordered.size() > 1;
    if (longEnough) {
      String footer = "Characters total:\t" + FORMAT.format(ordered.size());
      if (ordered.size() > limit) {
        footer += "\tOmitted from sample below:\t" + FORMAT.format(ordered.size()-limit);
      }
      if (TABLE) {
        footer = "<table><tr><td>" + footer.replace("\t","</td><td>") + "</td></tr></table><br>";
      }
      LOG_WRITER.println(footer);
    }

    if (TABLE) {
      LOG_WRITER.println("<table>");
      if (longEnough) {
        LOG_WRITER.println(
                "<tr>\r\n" +
                "<th>Code</th>" +
                "<th>New Map</th>" +
                "<th>Freq.</th>" +
                "<th>Name</th>" +
                "<th>↛ Old Map Name</th>" +
        "<th>→ New Map Name</th></tr>");
      }
    }

    int counter = 0;
    for (Comparable[] items : ordered) {
      counter++;
      if (counter > limit) {
        break;
      }
      final long count = (Long)items[0];
      final int cp = (Integer)items[1];
      final String oldMapped = (String)items[2];
      final String newMapped = (String)items[3];
      final String countStr = count == 0 ? "0" : "1/" + FORMAT.format(frequencies.getTotal()/count);
      
      String line = com.ibm.icu.impl.Utility.hex(cp) + " ; " 
      + hex(newMapped," ") + " ; "
      + countStr + " ; # "
      + showChanged(UTF16.valueOf(cp), oldMapped, newMapped, Form.codeStringAndName);
      
      if (TABLE) {
        line = "<tr><td>" + line.replace("\t","</td><td>").replace(" ; ","</td><td>") + "</td></tr>";
      }
      LOG_WRITER.println(line);
    }
    if (TABLE) {
      LOG_WRITER.println("</table>\r\n");
      LOG_WRITER.println("<pre>\r\n");
    }
  }

  static Comparator<Comparable[]> DOUBLE_STRING_COMP = new Comparator<Comparable[]>() {
    // only handle the case where the lengths are equal
    public int compare(Comparable[] o1, Comparable[] o2) {
      for (int i = 0; i < o1.length; ++i) {
        int result = o1[i].compareTo(o2[i]);
        if (result != 0) {
          return -result;
        }
      }
      return 0;
    }
  };


  /**
   * Generate new mapping file, based on exceptions file. See normalizeForMatchExceptions.txt for the format.
   * @param specialMappingsFile TODO
   * @param outputFile TODO
   * @param diffSource 
   * @param frequencyData 
   * @param frequencyFile 
   * @throws IOException
   */
  static void generateMappings(String specialMappingsFile, String outputFile, String diffSource, String frequencyFile) throws IOException {
    PrintWriter out = openUTF8Writer(outputFile);
    out.println("# Generated from: " + new File(specialMappingsFile).getName());
    out.println("# Date: " + ISO_DATE.format(new Date()));
    out.println("#");

    // special mappings
    UnicodeMap SPECIAL_MAPPINGS = new UnicodeMap();

    loadMappings(specialMappingsFile, SPECIAL_MAPPINGS, true);

    int linecount = 0;
    UnicodeMap mappings = new UnicodeMap();
    for (UnicodeSetIterator it = new UnicodeSetIterator(ASSIGNED); it.next();) {
      if (LIST_STYLE == ListStyle.ONLY_OLD && !U50.contains(it.codepoint)) {
        continue;
      }
      String str = it.getString();
      String other = getRemapped(it.codepoint, SPECIAL_MAPPINGS);
      if (other.equals(str)) continue;
      mappings.put(it.codepoint, other);
    }

    // Transitively close the mapping
    while (true) {
      UnicodeMap deltaMappings = new UnicodeMap();
      UnicodeSet done = new UnicodeSet();
      for (UnicodeSetIterator it = new UnicodeSetIterator(mappings.keySet()); it.next();) {
        String target = (String) mappings.getValue(it.codepoint);
        String recursed = replace(target, mappings);
        if (recursed != target) {
          deltaMappings.put(it.codepoint, recursed);
          done.add(it.codepoint);
        }
      }
      if (done.size() == 0) {
        break;
      }
      showOrderedList("Recursive Closure", done, mappings, deltaMappings, Integer.MAX_VALUE);
      mappings.putAllFiltered(deltaMappings, done);
    };

    // print them
    for (UnicodeSetIterator it = new UnicodeSetIterator(mappings.keySet()); it.next();) {
      writeMapping(out, it.getString(), (String) mappings.getValue(it.codepoint));
      linecount++;
    }
    out.close();

    if (diffSource != null) {
      logDiffs(mappings, diffSource, frequencyFile);
    }
  }

  private static String replace(String target, UnicodeMap mappings) {
    if (target.indexOf('\u2044') >= 0) {
      //System.out.println("?debug?");
    }
    StringBuilder result = new StringBuilder();
    boolean changed = false;
    int cp;
    for (int i = 0; i < target.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(target, i);
      String remapped = (String) mappings.getValue(cp);
      if (remapped != null) {
        result.append(remapped);
        changed = true;
      } else {
        result.appendCodePoint(cp);
      }
    }
    return changed ? result.toString() : target;
  }

  private static String getRemapped(int codepoint, UnicodeMap mapping) {
    return getRemapped(codepoint, (String) mapping.getValue(codepoint));
  }

  enum RuleMappings {__ok, __nfc, __caseonly, __bracket, __bracket_up, __bracket_circle, __bracket_down, __delete, __exclude}

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
      // no change
    } else if (!special.startsWith("__")) {
      other = special;
    } else {
      switch (RuleMappings.valueOf(special)) {
        case __ok: 
          other = normalizeAndCaseFold(other, Normalizer.NFKC);
          break;
        case __nfc:
          other = Normalizer.normalize(other, Normalizer.NFC, 0);
          break;
        case __caseonly:
          other = normalizeAndCaseFold(other, Normalizer.NFC);
          break;
        case __bracket:
          other = " " + normalizeAndCaseFold(other, Normalizer.NFKC) + " ";
          break;
        case __bracket_down:
          other = "⌜" + normalizeAndCaseFold(other, Normalizer.NFKC) + "⌝";
          break;
        case __bracket_up:
          other = "⌞" + normalizeAndCaseFold(other, Normalizer.NFKC) + "⌟";
          break;
        case __bracket_circle:
          other = "(" + normalizeAndCaseFold(other, Normalizer.NFKC) + ")";
          break;
        default:
          throw new IllegalArgumentException("Missing rule");
      }
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
   * @param resultMappings 
   * @param printWriter TODO
   * @throws IOException
   */
  private static void loadMappings(String filename, UnicodeMap resultMappings, boolean printWriter) throws IOException {
    //SPECIAL_MAPPINGS.putAll(0,0x10FFFF, "exclude");
    BufferedReader in = openUTF8Reader(filename);
    int lineNumber = 0;
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      getMappingFromSemiLine(lineNumber++, line, true, resultMappings, printWriter);
    }
    in.close();
    resultMappings.freeze();
  }

  /**
   * Process each special mapping line from a file
   * @param lineNumber
   * @param line
   * @param skipIfIdentical
   * @param resultMappings 
   * @param printWriter TODO
   * @param mappings
   */
  private static void getMappingFromSemiLine(int lineNumber, String line, boolean skipIfIdentical, UnicodeMap resultMappings, boolean printWriter) {
    line = line.trim();
    if (line.startsWith("\uFEFF")) {
      line = line.substring(1);
    }
    if (printWriter) {
      LOG_WRITER.println(lineNumber + ":\t" + line);
    }
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
    final String sourcePiece = pieces[0];
    final String target = pieces.length < 2 ? "" : pieces[1];
    UnicodeSet source = new UnicodeSet();
    if (UnicodeSet.resemblesPattern(sourcePiece, 0)) {
      source.applyPattern(sourcePiece);
    } else if (UTF16.countCodePoint(sourcePiece) == 1) {
      source.add(UTF16.charAt(sourcePiece, 0));
    } else {
      String[] starts = sourcePiece.split("\\s*-\\s*");
      int start = Integer.parseInt(starts[0], 16);
      int end = starts.length == 1 ? start : Integer.parseInt(starts[1], 16);
      source.add(start,end);
    }

    UnicodeSet targetFilter = pieces.length < 3 || pieces[2].length() == 0 ? new UnicodeSet(ASSIGNED) : new UnicodeSet(pieces[2]);
    addMappings(source, target, targetFilter, resultMappings, printWriter);
  }

  /**
   * Add exceptions based on a line from the special mapping files
   * @param source
   * @param target
   * @param targetFilter
   * @param resultMappings 
   * @param printWriter TODO
   * @param mappings
   */
  private static void addMappings(UnicodeSet source, String target, UnicodeSet targetFilter, UnicodeMap resultMappings, boolean printWriter) {
    String oldTarget = target;
    // remap options

    if (target.equalsIgnoreCase("delete")) {
      target = "";
    } else if (target.equalsIgnoreCase("exclude")) {
      target = null;
    } else {
      try {
        RuleMappings x = RuleMappings.valueOf("__" + target.toLowerCase());
        target = x.toString();
      } catch (IllegalArgumentException e) {
        if (HEXFORM.reset(target).matches()) {
          target = fromHex(target);
        } else {
          target = target;
        }
      }
    }

    // show a sample of what changed
    // and absorb new mappings, where they make a difference
    int count = 0;
    UnicodeSet affected = new UnicodeSet(source).retainAll(ASSIGNED);
    UnicodeMap deltaMappings = new UnicodeMap();
    UnicodeSet done = new UnicodeSet();

    for (UnicodeSetIterator it = new UnicodeSetIterator(affected); it.next();) {
      String willGet = getRemapped(it.codepoint, target);
      if (!targetFilter.containsAll(willGet)) {
        continue;
      }
      if (it.codepoint == DEBUG_CODE_POINT) {
        System.out.println("?debug?");
      }

      String didGet = getRemapped(it.codepoint, resultMappings);

      if (willGet.equals(didGet)) {
        // skip cases where we already covered this mapping
        continue;
      }
      deltaMappings.put(it.codepoint, willGet);
      done.add(it.codepoint);
      count++;

      //      // show what we are doing
      //      
      //      if (++count > 6) {
      //        remainder.add(it.codepoint);
      //        continue;
      //      }
      //      final String string = it.getString();
      //      //final String normalizedAndCaseFolded = normalizeAndCaseFold(string);
      //      //final String caseFolded = UCharacter.foldCase(string, true);
      //
      //      if (printWriter != null) {
      //        printWriter.println("\t" + codeAndName(string, Form.codeStringAndName) + "\t;\t" + oldTarget
      //                + "\t; #\t" + showChanged(string, didGet, willGet, Form.string));
      //      }
    }

    if (printWriter) {
      showOrderedList(null, done, resultMappings, deltaMappings, DIFF_LIMIT);
    }
    resultMappings.putAllFiltered(deltaMappings, done);
  }


  enum Form {string, codeStringAndName}

  private static String showChanged(final String string, String didGet, String willGet, Form form) {
    final String didGetStr = string.equals(didGet) ? "[unchanged]" : codeAndName(didGet, form);
    return codeAndName(string, form)
    + "\t ↛ " + didGetStr
    + "\t → " + codeAndName(willGet, form);
  }

  static final UnicodeSet BIDI = new UnicodeSet("[[:bidiclass=R:][:bidiclass=AL:]]");
  static final UnicodeSet DI = new UnicodeSet("[[:C:][:Default_Ignorable_Code_Point:]]");
  static final UnicodeSet FISHY = new UnicodeSet("[\\<\\&\\>\\\"[:C:][:Z:][:whitespace:][:Default_Ignorable_Code_Point:]]");

  private static final String RULES =
    "'<' > '&lt;' ;" +
    "'&' > '&amp;' ;" +
    "'>' > '&gt;' ;" +
    "'\"' > '&quot;' ; " + 
    ":: [[:C:][:Default_Ignorable_Code_Point:]-[\\u0020\\u0009\\u000A\\u000D]] hex/java ; ";

  public static final Transliterator toHTMLControl = Transliterator.createFromRules(
          "any-html", RULES, Transliterator.FORWARD);

  private static FrequencyData frequencies;


  private static String quote(String input) {
    String source = input;
    if (FISHY.containsSome(source)) {
      if (false && source.length() > 20) {
        System.out.println("?debug?");
      }
      source = toHTMLControl.transform(source);
      if (false) {
        System.out.println(input + "=>" + source);
      }
      System.out.flush();
    }
    if (BIDI.containsSome(source)) {
      source = "\u200E" + source + "\u200E";
    }
    return source;
  }

  /**
   * Printing convenience function
   * @param defaultChange
   * @return
   */
  private static String codeAndName(String defaultChange, Form form) {
    final String quotedChar = DI.containsAll(defaultChange) ? "«»" : "«" + quote(defaultChange) + "»";
    switch (form) {
      default:
        return quotedChar;
      case codeStringAndName:
        return hex(defaultChange," ") + " " + quotedChar + " " + UCharacter.getName(defaultChange, " + ");
    }
  }

  /**
   * Printing convenience function
   * @param spaceDelimitedHex
   * @return
   */
  static String fromHex(String spaceDelimitedHex) {
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


  private static PrintWriter openUTF8Writer(String filename) throws IOException {
    File file = new File(filename);
    System.out.println("Writing:\t" + file.getCanonicalPath());
    return new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), UTF8));
  }

  static BufferedReader openUTF8Reader(String filename) throws IOException {
    File file = new File(filename);
    System.out.println("Reading:\t" + file.getCanonicalPath());
    return new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF8),1024*64);
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
      getMappingFromSemiLine(lineNumber++, line, false, oldMap, false);
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
