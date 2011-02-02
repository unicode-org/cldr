/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.TimeZone;

public class CldrUtility {

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * Very simple class, used to replace variables in a string. For example<p>
	<pre>static VariableReplacer langTag = new VariableReplacer()
			.add("$alpha", "[a-zA-Z]")
			.add("$digit", "[0-9]")
			.add("$alphanum", "[a-zA-Z0-9]")
			.add("$x", "[xX]");
			...
			String langTagPattern = langTag.replace(...);
	</pre>
   */
  public static class VariableReplacer {
    // simple implementation for now
    private Map<String, String> m = new TreeMap<String, String>(Collections.reverseOrder());
    public VariableReplacer add(String variable, String value) {
      m.put(variable, value);
      return this;
    }
    public String replace(String source) {
      String oldSource;
      do {
        oldSource = source;
        for (Iterator<String> it = m.keySet().iterator(); it.hasNext();) {
          String variable = it.next();
          String value = m.get(variable);
          source = replaceAll(source, variable, value);
        }
      } while (!source.equals(oldSource));
      return source;
    }
    public String replaceAll(String source, String key, String value) {
      while (true) {
        int pos = source.indexOf(key);
        if (pos < 0) return source;
        source = source.substring(0,pos) + value + source.substring(pos+key.length());
      }
    }
  }

  static String getPath(String path, String filename) {
    if (path == null) {
      return null;
    }
    final File file = filename == null ? new File(path) 
    : new File(path, filename);
    try {
      return file.getCanonicalPath() + File.separatorChar;
    } catch (IOException e) {
      return file.getPath() + File.separatorChar;
    }
  }

  static String getPath(String path) {
    return getPath(path, null);
  }

  static final boolean DEBUG_SHOW_BAT = false;
  /** default working directory for Eclipse is . = ${workspace_loc:cldr}, which is <CLDR>/tools/java/ */
  // set the base directory with -Dcldrdata=<value>
  // if the main is different, use -Dcldrmain=<value>
  public static final String BASE_DIRECTORY = getPath(CldrUtility.getProperty("CLDR_DIR", null)); // new File(Utility.getProperty("CLDR_DIR", null)).getPath();	// get up to <CLDR>
  public static final String UTIL_DATA_DIR = getPath(BASE_DIRECTORY, "tools/java/org/unicode/cldr/util/data/");        // "C:/ICU4C/locale/tools/java/org/unicode/cldr/util/";
  public static final String UTIL_CLASS_DIR = "org.unicode.cldr.util";
  public static final String COMMON_DIRECTORY = getPath(BASE_DIRECTORY , "common/");
  public static final String MAIN_DIRECTORY = CldrUtility.getProperty("CLDR_MAIN", getPath(CldrUtility.COMMON_DIRECTORY,  "main"));
  public static final String COLLATION_DIRECTORY = getPath(COMMON_DIRECTORY,"collation/");
  public static final String GEN_DIRECTORY = getPath(CldrUtility.getProperty("CLDR_GEN_DIR", getPath(BASE_DIRECTORY , "../Generated/cldr/"))); 
  public static final String TMP_DIRECTORY = getPath(CldrUtility.getProperty("CLDR_TMP_DIR", getPath(BASE_DIRECTORY , "../cldr-tmp/"))); 

  /**
   * @deprecated please use XMLFile and CLDRFILE getSupplementalDirectory()
   * @see DEFAULT_SUPPLEMENTAL_DIRECTORY
   */
  public static final String SUPPLEMENTAL_DIRECTORY = getPath(COMMON_DIRECTORY , "supplemental/");
  /**
   * Only the default, if no other directory is specified.
   */
  public static final String DEFAULT_SUPPLEMENTAL_DIRECTORY = getPath(COMMON_DIRECTORY , "supplemental/");
  public static final String CHART_DIRECTORY = getPath(BASE_DIRECTORY,  "/../cldr-tmp/diff/");
  public static final String TEST_DIR = getPath(CldrUtility.BASE_DIRECTORY,  "test/");


  /** If the generated BAT files are to work, this needs to be set right */
  public static final String COMPARE_PROGRAM = "\"C:\\Program Files\\Compare It!\\wincmp3.exe\"";

  public static final List<String> MINIMUM_LANGUAGES = Arrays.asList(new String[] {"ar", "en", "de", "fr", "hi", "it", "es", "pt", "ru", "zh", "ja"}); // plus language itself
  public static final List<String> MINIMUM_TERRITORIES = Arrays.asList(new String[] {"US", "GB", "DE", "FR", "IT", "JP", "CN", "IN", "RU", "BR"});

  public interface LineComparer {
    static final int LINES_DIFFERENT = -1, LINES_SAME = 0, SKIP_FIRST = 1, SKIP_SECOND = 2;
    /**
     * Returns LINES_DIFFERENT, LINES_SAME, or if one of the lines is ignorable, SKIP_FIRST or SKIP_SECOND
     * @param line1
     * @param line2
     * @return
     */
    int compare(String line1, String line2);
  }

  public static class SimpleLineComparator implements LineComparer {
    public static final int TRIM = 1, SKIP_SPACES = 2, SKIP_EMPTY = 4, SKIP_CVS_TAGS = 8;
    StringIterator si1 = new StringIterator();
    StringIterator si2 = new StringIterator();
    int flags;
    public SimpleLineComparator(int flags) {
      this.flags = flags;
    }
    public int compare(String line1, String line2) {
      // first, see if we want to skip one or the other lines
      int skipper = 0;
      if (line1 == null) {
        skipper = SKIP_FIRST;
      } else {
        if ((flags & TRIM)!= 0) line1 = line1.trim();
        if ((flags & SKIP_EMPTY)!= 0 && line1.length() == 0) skipper = SKIP_FIRST;
      }
      if (line2 == null) {
        skipper = SKIP_SECOND;
      } else {
        if ((flags & TRIM)!= 0) line2 = line2.trim();
        if ((flags & SKIP_EMPTY)!= 0 && line2.length() == 0) skipper += SKIP_SECOND;
      }
      if (skipper != 0) {
        if (skipper == SKIP_FIRST + SKIP_SECOND) return LINES_SAME; // ok, don't skip both
        return skipper;
      }

      // check for null
      if (line1 == null) {
        if (line2 == null) return LINES_SAME;
        return LINES_DIFFERENT;          	
      }
      if (line2 == null) {
        return LINES_DIFFERENT;
      }

      // now check equality
      if (line1.equals(line2)) return LINES_SAME;

      // if not equal, see if we are skipping spaces
      if ((flags & SKIP_CVS_TAGS) != 0) {
        if (line1.indexOf('$') >= 0 && line2.indexOf('$') >= 0) {
          line1 = stripTags(line1);
          line2 = stripTags(line2);
          if (line1.equals(line2)) return LINES_SAME;
        } else if (line1.startsWith("<!DOCTYPE ldml SYSTEM \"../../common/dtd/")
                && line2.startsWith("<!DOCTYPE ldml SYSTEM \"../../common/dtd/")) {
          return LINES_SAME;
        }
      }
      if ((flags & SKIP_SPACES) != 0 && si1.set(line1).matches(si2.set(line2))) return LINES_SAME;
      return LINES_DIFFERENT;
    }

    //  private Matcher dtdMatcher = Pattern.compile(
    //  "\\Q<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/\\E.*\\Q/ldml.dtd\">\\E").matcher("");

    private String[] CVS_TAGS = {"Revision", "Date"};

    private String stripTags(String line) {
      //$Revision$
      //$Date$
      int pos = line.indexOf('$');
      if (pos < 0) return line;
      pos++;
      int endpos = line.indexOf('$', pos);
      if (endpos < 0) return line;
      for (int i = 0; i < CVS_TAGS.length; ++i) {
        if (!line.startsWith(CVS_TAGS[i], pos)) continue;
        line = line.substring(0,pos + CVS_TAGS[i].length()) + line.substring(endpos);
      }
      return line;
    }

  }

  /**
   * 
   * @param file1
   * @param file2
   * @param failureLines on input, String[2], on output, failing lines
   * @param lineComparer
   * @return
   * @throws IOException
   */
  public static boolean areFileIdentical(String file1, String file2, String[] failureLines, 
          LineComparer lineComparer) throws IOException {
    BufferedReader br1 = new BufferedReader(new FileReader(file1), 32*1024);
    try {
      BufferedReader br2 = new BufferedReader(new FileReader(file2), 32*1024);
      try {
        String line1 = "";
        String line2 = "";
        int skip = 0;

        for (int lineCount = 0; ; ++lineCount) {
          if ((skip & LineComparer.SKIP_FIRST) == 0) line1 = br1.readLine();
          if ((skip & LineComparer.SKIP_SECOND) == 0) line2 = br2.readLine();
          if (line1 == null && line2 == null) return true;
          if (line1 == null || line2 == null) {
            // System.out.println("debug");
          }
          skip = lineComparer.compare(line1, line2);
          if (skip == LineComparer.LINES_DIFFERENT) {
            break;
          }
        }
        failureLines[0] = line1 != null ? line1 : "<end of file>";
        failureLines[1] = line2 != null ? line2 : "<end of file>";
        return false;
      } finally {
        br2.close();
      }
    } finally {
      br1.close();
    }
  }

  public static void registerExtraTransliterators() {
    String tzadir = UTIL_DATA_DIR + File.separatorChar; // work around bad pattern (dir+filename)
    // HACK around lack of Armenian, Ethiopic				
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Latin-Armenian");
    //TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Latin-Ethiopic");
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Cyrillic-Latin");
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Arabic-Latin");	
    // needed
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Thaana-Latin");		
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Syriac-Latin");		
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Canadian_Aboriginal-Latin");
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Georgian-Latin");

    // do nothing, too complicated to do quickly
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Tibetan-Latin");
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Khmer-Latin");
    TransliteratorUtilities.registerTransliteratorFromFile(tzadir, "Lao-Latin");
  }



  /*
    static String getLineWithoutFluff(BufferedReader br1, boolean first, int flags) throws IOException {
        while (true) {
            String line1 = br1.readLine();
            if (line1 == null) return line1;
            if ((flags & TRIM)!= 0) line1 = line1.trim();
            if ((flags & SKIP_EMPTY)!= 0 && line1.length() == 0) continue;
            return line1;
        }
    }
   */

  public final static class StringIterator {
    String string;
    int position = 0;
    char next() {
      while (true) {
        if (position >= string.length()) return '\uFFFF';
        char ch = string.charAt(position++);
        if (ch != ' ' && ch != '\t') return ch;
      }
    }
    StringIterator reset() {
      position = 0;
      return this;
    }
    StringIterator set(String string) {
      this.string = string;
      position = 0;
      return this;
    }
    boolean matches(StringIterator other) {
      while (true) {
        char c1 = next();
        char c2 = other.next();
        if (c1 != c2) return false;
        if (c1 == '\uFFFF') return true;
      }
    }
    /**
     * @return Returns the position.
     */
    public int getPosition() {
      return position;
    }
  }

  static public void generateBat(String sourceDir, String sourceFile, String targetDir, String targetFile) {
    generateBat( sourceDir,  sourceFile,  targetDir,  targetFile, new CldrUtility.SimpleLineComparator(0)); 
  }

  static public void generateBat(String sourceDir, String sourceFile, String targetDir, String targetFile, LineComparer lineComparer) {
    try {
      String batDir = targetDir + "diff" + File.separator;
      String batName = targetFile + ".bat";
      String[] failureLines = new String[2];

      String fullSource = sourceDir + File.separator + sourceFile;
      String fullTarget = targetDir + File.separator + targetFile;

      if (!new File(sourceDir, sourceFile).exists()) {
        File f = new File(batDir, batName);
        if (f.exists()) {
          if (DEBUG_SHOW_BAT) System.out.println("*Deleting old " + f.getCanonicalPath());
          f.delete();
        }
      } else if (!areFileIdentical(fullSource, fullTarget, failureLines, lineComparer)) {
        PrintWriter bat = BagFormatter.openUTF8Writer(batDir, batName);
        try {
          bat.println(COMPARE_PROGRAM + " " +
                  new File(fullSource).getCanonicalPath() + " " +
                  new File(fullTarget).getCanonicalPath());               
        } finally {
          bat.close();
        }
      } else {
        File f = new File(batDir, batName);
        if (f.exists()) {
          if (DEBUG_SHOW_BAT) System.out.println("*Deleting old:\t" + f.getCanonicalPath());
          f.delete();
        }
        f = new File(fullTarget);
        if (BagFormatter.SHOW_FILES) System.out.println("*Deleting old:\t" + f.getCanonicalPath());
        f.delete();
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static String[] splitArray(String source, char separator) {
    return splitArray(source, separator, false);
  }

  public static String[] splitArray(String source, char separator, boolean trim) {
    List<String> piecesList = splitList(source, separator, trim);
    String[] pieces = new String[piecesList.size()];
    piecesList.toArray(pieces);
    return pieces;
  }

  public static List<String> splitList(String source, char separator) {
    return splitList(source, separator, false, null);
  }

  public static List<String> splitList(String source, char separator, boolean trim) {
    return splitList(source, separator, trim, null);
  }

  public static List<String> splitList(String source, char separator, boolean trim, List<String> output) {
    if (output == null) output = new ArrayList<String>();
    if (source.length() == 0) return output;
    int pos = 0;
    do {
      int npos = source.indexOf(separator, pos);
      if (npos < 0) npos = source.length();
      String piece = source.substring(pos, npos);
      if (trim) piece = piece.trim();
      output.add(piece);
      pos = npos+1;
    } while (pos < source.length());
    return output;
  }

  /**
   * Protect a collection (as much as Java lets us!) from modification.
   * Really, really ugly code, since Java doesn't let us do better.
   */
  public static <T> T protectCollection(T source) {
    // TODO - exclude UnmodifiableMap, Set, ...
    if (source instanceof Map) {
      Map sourceMap = (Map) source;
      Map resultMap = clone(sourceMap);
      if (resultMap == null) return (T) sourceMap; // failed
      resultMap.clear();
      for (Object key : sourceMap.keySet()) {
        resultMap.put(protectCollection(key), protectCollection(sourceMap.get(key)));
      }
      return resultMap instanceof SortedMap ? (T) Collections.unmodifiableSortedMap((SortedMap)resultMap)
              : (T) Collections.unmodifiableMap(resultMap);
    } else if (source instanceof Collection) {
      Collection sourceCollection = (Collection) source;
      Collection<Object> resultCollection = clone(sourceCollection);
      if (resultCollection == null) return (T) sourceCollection; // failed
      resultCollection.clear();

      for (Object item : sourceCollection) {
        resultCollection.add(protectCollection(item));
      }

      return sourceCollection instanceof List ? (T)Collections.unmodifiableList((List)sourceCollection)
              : sourceCollection instanceof SortedSet ? (T)Collections.unmodifiableSortedSet((SortedSet)sourceCollection)
                      : sourceCollection instanceof Set ? (T)Collections.unmodifiableSet((Set)sourceCollection)
                              : (T)Collections.unmodifiableCollection(sourceCollection);
    } else if (source instanceof Freezable) {
      Freezable freezableSource = (Freezable) source;
      if (freezableSource.isFrozen()) return source;
      return (T)((Freezable)(freezableSource.cloneAsThawed())).freeze();
    } else {
      return source; // can't protect
    }
  }

  /**
   * Clones T if we can; otherwise returns null.
   * @param <T>
   * @param source
   * @return
   */
  public static <T> T clone(T source) {
    try {
      final Class<? extends Object> class1 = source.getClass();
      final Method declaredMethod = class1.getDeclaredMethod("clone", (Class)null);
      return (T) declaredMethod.invoke(source, (Object)null);
    } catch (Exception e) {
      return null; // uncloneable
    }
  }

  /** Appends two strings, inserting separator if either is empty
   */
  public static String joinWithSeparation(String a, String separator, String b) {
    if (a.length() == 0) return b;
    if (b.length() == 0) return a;
    return a + separator + b;
  }

  /** Appends two strings, inserting separator if either is empty. Modifies first map
   */
  public static Map<Object, String> joinWithSeparation(Map<Object, String> a, String separator, Map b) {
    for (Iterator it = b.keySet().iterator(); it.hasNext();) {
      Object key = it.next();
      String bvalue = (String) b.get(key);
      String avalue = a.get(key);
      if (avalue != null) {
        if (avalue.trim().equals(bvalue.trim())) continue;
        bvalue = joinWithSeparation(avalue, separator, bvalue);
      }
      a.put(key, bvalue);
    }
    return a;
  }

  public static String join(Collection c, String separator) {
    StringBuffer output = new StringBuffer();
    boolean isFirst = true;
    for (Object item : c) {
      if (isFirst) {
        isFirst = false;
      } else {
        output.append(separator);
      }
      output.append(item == null ? item : item.toString());
    }
    return output.toString();
  }

  public static String join(Object[] c, String separator) {
    StringBuffer output = new StringBuffer();
    boolean isFirst = true;
    for (Object item : c) {
      if (isFirst) {
        isFirst = false;
      } else {
        output.append(separator);
      }
      output.append(item == null ? item : item.toString());
    }
    return output.toString();
  }


  /**
   * Utility like Arrays.asList()
   */
  public static Map asMap(Object[][] source, Map target, boolean reverse) {
    int from = 0, to = 1;
    if (reverse) {
      from = 1; to = 0;
    }
    for (int i = 0; i < source.length; ++i) {
      if (source[i].length != 2) {
        throw new IllegalArgumentException("Source must be array of pairs of strings: " + Arrays.asList(source[i]));
      }
      target.put(source[i][from], source[i][to]);
    }
    return target;
  }

  public static Map asMap(Object[][] source) {
    return asMap(source, new HashMap(), false);
  }

  /**
   * Utility that ought to be on Map
   */
  public static Map removeAll(Map m, Collection itemsToRemove) {
    for (Iterator it = itemsToRemove.iterator(); it.hasNext();) {
      Object item = it.next();
      m.remove(item);
    }
    return m;
  }

  /**
   * Returns the canonical name for a file.
   */
  public static String getCanonicalName(String file) {
    try {
      return new File(file).getCanonicalPath();
    } catch (Exception e) {
      return file;
    }
  }

  /**
   * Convert a UnicodeSet into a string that can be embedded into a Regex. Handles strings that are in the UnicodeSet, Supplementary ranges, and escaping
   * @param source  The source set
   * @param escaper A transliterator that is used to escape the characters according to the requirements of the regex.
   * @return
   */
  public static String toRegex(UnicodeSet source) {
    return toRegex(source, null, false);
  }

  private static final  Transliterator DEFAULT_REGEX_ESCAPER = Transliterator.createFromRules(
          "foo", 
          "([ \\- \\\\ \\[ \\] ]) > '\\' $1 ;"
          //        + " ([:c:]) > &hex($1);"
          + " ([[:control:][[:z:]&[:ascii:]]]) > &hex($1);",
          Transliterator.FORWARD);

  /**
   * Convert a UnicodeSet into a string that can be embedded into a Regex.
   * Handles strings that are in the UnicodeSet, Supplementary ranges, and
   * escaping
   * 
   * @param source
   *          The source set
   * @param escaper
   *          A transliterator that is used to escape the characters according
   *          to the requirements of the regex. The default puts a \\ before [, -,
   *          \, and ], and converts controls and Ascii whitespace to hex.
   *          Alternatives can be supplied. Note that some Regex engines,
   *          including Java 1.5, don't really deal with escaped supplementaries
   *          well.
   * @param onlyBmp
   *          Set to true if the Regex only accepts BMP characters. In that
   *          case, ranges of supplementary characters are converted to lists of
   *          ranges. For example, [\uFFF0-\U0010000F \U0010100F-\U0010300F]
   *          converts into:
   *          <pre>
   *          [\uD800][\uDC00-\uDFFF]
   *          [\uD801-\uDBBF][\uDC00-\uDFFF]
   *          [\uDBC0][\uDC00-\uDC0F]</pre>
   *          and<pre>
   *          [\uDBC4][\uDC0F-\uDFFF]
   *          [\uDBC5-\uDBCB][\uDC00-\uDFFF]
   *          [\uDBCC][\uDC00-\uDC0F]
   *          </pre>
   *          These are then coalesced into a list of alternatives by sharing
   *          parts where feasible. For example, the above turns into 3 pairs of ranges:
   *          <pre>
   *          [\uDBC0\uDBCC][\uDC00-\uDC0F]|\uDBC4[\uDC0F-\uDFFF]|[\uD800-\uDBBF\uDBC5-\uDBCB][\uDC00-\uDFFF]
   *          </pre>
   * 
   * @return escaped string. Something like [a-z] or (?:[a-m]|{zh}) if there is
   *         a string zh in the set, or a more complicated case for
   *         supplementaries. <br>
   *         Special cases: [] returns "", single item returns a string
   *         (escaped), like [a] => "a", or [{abc}] => "abc"<br>
   *         Supplementaries are handled specially, as described under onlyBmp.
   */
  public static String toRegex(UnicodeSet source, Transliterator escaper, boolean onlyBmp) {
    if (escaper == null) {
      escaper = DEFAULT_REGEX_ESCAPER;
    }
    UnicodeSetIterator it = new UnicodeSetIterator(source);
    // if there is only one item, return it
    if (source.size() == 0) {
      return "";
    }
    if (source.size() == 1) {
      it.next();
      return escaper.transliterate(it.getString());
    }
    // otherwise, we figure out what is in the set, and will return 
    StringBuilder base = new StringBuilder("[");
    StringBuilder alternates = new StringBuilder();
    Map<UnicodeSet,UnicodeSet> lastToFirst = new TreeMap<UnicodeSet,UnicodeSet>(new UnicodeSetComparator());
    int alternateCount = 0;
    while(it.nextRange()) {
      if (it.codepoint == it.IS_STRING) {
        ++alternateCount;
        alternates.append('|').append(escaper.transliterate(it.string));
      } else if (!onlyBmp || it.codepointEnd <= 0xFFFF) { // BMP
        addBmpRange(it.codepoint, it.codepointEnd, escaper, base);
      } else { // supplementary
        if (it.codepoint <= 0xFFFF) {
          addBmpRange(it.codepoint, 0xFFFF, escaper, base);
          it.codepoint = 0x10000; // reset the range
        }
        // this gets a bit ugly; we are trying to minimize the extra ranges for supplementaries
        // we do this by breaking up X-Y based on the Lead and Trail values for X and Y
        // Lx [Tx - Ty])         (if Lx == Ly)
        // Lx [Tx - DFFF] | Ly [DC00-Ty]        (if Lx == Ly - 1)
        // Lx [Tx - DFFF] | [Lx+1 - Ly-1][DC00-DFFF] | Ly [DC00-Ty] (otherwise)
        int leadX = UTF16.getLeadSurrogate(it.codepoint);
        int trailX = UTF16.getTrailSurrogate(it.codepoint);
        int leadY = UTF16.getLeadSurrogate(it.codepointEnd);
        int trailY = UTF16.getTrailSurrogate(it.codepointEnd);
        if (leadX == leadY) {
          addSupplementalRange(leadX, leadX, trailX, trailY, escaper, lastToFirst);
        } else {
          addSupplementalRange(leadX, leadX, trailX, 0xDFFF, escaper, lastToFirst);
          if (leadX != leadY - 1) {
            addSupplementalRange(leadX+1, leadY-1, 0xDC00, 0xDFFF, escaper, lastToFirst);
          }
          addSupplementalRange(leadY, leadY, 0xDC00, trailY, escaper, lastToFirst);  
        }
      }
    }
    // add in the supplementary ranges 
    if (lastToFirst.size() != 0) {
      for (UnicodeSet last : lastToFirst.keySet()) {
        ++alternateCount;
        alternates.append('|').append(toRegex(lastToFirst.get(last), escaper, onlyBmp)).append(toRegex(last, escaper, onlyBmp));
      }
    }
    // Return the output. We separate cases in order to get the minimal extra apparatus
    base.append("]");
    if (alternateCount == 0) {
      return base.toString();
    } else if (base.length() > 2) {
      return "(?:" + base + "|" + alternates.substring(1) + ")";
    } else if (alternateCount == 1) {
      return alternates.substring(1);
    }else {
      return "(?:" + alternates.substring(1) + ")";
    }
  }

  private static void addSupplementalRange(int leadX, int leadY, int trailX, int trailY, Transliterator escaper, Map<UnicodeSet, UnicodeSet> lastToFirst) {
    System.out.println("\tadding: " + new UnicodeSet(leadX, leadY) + "\t" + new UnicodeSet(trailX, trailY) );
    UnicodeSet last = new UnicodeSet(trailX, trailY);
    UnicodeSet first = lastToFirst.get(last);
    if (first == null) {
      lastToFirst.put(last, first = new UnicodeSet());
    }
    first.add(leadX, leadY);
  }

  private static void addBmpRange(int start, int limit, Transliterator escaper, StringBuilder base) {
    base.append(escaper.transliterate(UTF16.valueOf(start)));
    if (start != limit) {
      base.append("-").append(escaper.transliterate(UTF16.valueOf(limit)));
    }
  }

  public static class UnicodeSetComparator implements Comparator<UnicodeSet> {
    public int compare(UnicodeSet o1, UnicodeSet o2) {
      return o1.compareTo(o2);
    }
  }

  public static class CollectionComparator<T extends Comparable<T>> implements Comparator<Collection<T>> {
    public int compare(Collection<T> o1, Collection<T> o2) {
      return UnicodeSet.compare(o1, o2, UnicodeSet.ComparisonStyle.SHORTER_FIRST);
    }
  }

  public static class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {
    public int compare(T arg0, T arg1) {
      return Utility.checkCompare(arg0, arg1);
    }
  }

  public static void addTreeMapChain(Map coverageData, Object... objects) {
    Map<Object, Object> base = coverageData;
    for (int i = 0; i < objects.length-2; ++i) {
      Map<Object, Object> nextOne = (Map<Object, Object>) base.get(objects[i]);
      if (nextOne == null) base.put(objects[i], nextOne = new TreeMap<Object, Object>());
      base = nextOne;
    }
    base.put(objects[objects.length-2], objects[objects.length-1]);
  }

  public static abstract class Transform {
    public abstract Object transform(Object source);
    public Collection<Object> transform(Collection input, Collection<Object> output) {
      for (Iterator it = input.iterator(); it.hasNext();) {
        Object result = transform(it.next());
        if (result != null) output.add(result);
      }
      return output;
    }
    public Collection<Object> transform(Collection input) {
      return transform(input, new ArrayList<Object>());
    }
  }

  public static abstract class Apply<T> {
    public abstract void apply(T item);
    public <U extends Collection<T>> void applyTo(U collection) {
      for (T item : collection) {
        apply(item);
      }
    }
  }

  public static abstract class Filter<T> {

    public abstract boolean contains(T item);

    public <U extends Collection<T>> U retainAll(U c) {
      for (Iterator<T> it = c.iterator(); it.hasNext();) {
        if (!contains(it.next())) it.remove();
      }
      return c;
    }

    public <U extends Collection<T>> U extractMatches(U c, U target) {
      for (Iterator<T> it = c.iterator(); it.hasNext();) {
        T item = it.next();
        if (contains(item)) {
          target.add(item);
        }
      }
      return target;
    }

    public <U extends Collection<T>> U removeAll(U c) {
      for (Iterator<T> it = c.iterator(); it.hasNext();) {
        if (contains(it.next())) it.remove();
      }
      return c;
    }

    public <U extends Collection<T>> U extractNonMatches(U c, U target) {
      for (Iterator<T> it = c.iterator(); it.hasNext();) {
        T item = it.next();
        if (!contains(item)) {
          target.add(item);
        }
      }
      return target;
    }
  }

  public static class MatcherFilter<T> extends Filter<T> {
    private Matcher matcher;
    public MatcherFilter(String pattern) {
      this.matcher = Pattern.compile(pattern).matcher("");
    }
    public MatcherFilter(Matcher matcher) {
      this.matcher = matcher;
    }
    public MatcherFilter<T> set(Matcher matcher) {
      this.matcher = matcher;
      return this;
    }
    public MatcherFilter<T> set(String pattern) {
      this.matcher = Pattern.compile(pattern).matcher("");
      return this;
    }
    public boolean contains(T o) {
      return matcher.reset(o.toString()).matches();
    }		
  }

  /**
 * Simple struct-like class for output parameters.
 * @param <T> The type of the parameter.
 */
public static final class Output<T> {
    public T value;
    public String toString() {
        return value == null ? "null" : value.toString();
    }
}
//    static final class HandlingTransform implements Transform<String, Handling> {
//        @Override
//        public Handling transform(String source) {
//            return Handling.valueOf(source);
//        }
//    }

/**
   * Fetch data from jar
   * @param name name of thing to load (org.unicode.cldr.util.name)
   */
  static public BufferedReader getUTF8Data(String name) throws java.io.IOException {
    java.io.InputStream is = null;
    try {
      is = 
        com.ibm.icu.impl.ICUData.getRequiredStream(Class.forName(CldrUtility.UTIL_CLASS_DIR+".CldrUtility"), "data/" + name);
    } catch (ClassNotFoundException cnf) { 

      throw new FileNotFoundException("Couldn't load " + CldrUtility.UTIL_CLASS_DIR + "." + name + " - ClassNotFoundException." + cnf.toString());
      //    .initCause(cnf);
    } catch (java.util.MissingResourceException mre) {
      // try file
      return BagFormatter.openUTF8Reader(CldrUtility.UTIL_DATA_DIR + File.separator, name);
    }
    return new java.io.BufferedReader(   new java.io.InputStreamReader(is,"UTF-8") );
  }

  /**
   * Takes a Map that goes from Object to Set, and fills in the transpose
   * @param source_key_valueSet
   * @param output_value_key
   */
  public static void putAllTransposed(Map source_key_valueSet, Map output_value_key) {
    for (Iterator it = source_key_valueSet.keySet().iterator(); it.hasNext();) {
      Object key = it.next();
      Set values = (Set) source_key_valueSet.get(key);
      for (Iterator it2 = values.iterator(); it2.hasNext();) {
        Object value = it2.next();
        output_value_key.put(value, key);
      }
    }
  }

  public static int countInstances(String source, String substring) {
    int count = 0;
    int pos = 0;
    while (true) {
      pos = source.indexOf(substring, pos) + 1;
      if (pos <= 0) break;
      count++;
    }
    return count;
  }

  public static void registerTransliteratorFromFile(String id, String dir, String filename) {
    registerTransliteratorFromFile(id, dir, filename, Transliterator.FORWARD, true);
    registerTransliteratorFromFile(id, dir, filename, Transliterator.REVERSE, true);
  }

  public static void registerTransliteratorFromFile(String id, String dir, String filename, int direction, boolean reverseID) {
    if (filename == null) {
      filename = id.replace('-', '_');
      filename = filename.replace('/', '_');
      filename += ".txt";
    }
    String rules = getText(dir, filename);
    Transliterator t;
    int pos = id.indexOf('-');
    String rid;
    if (pos < 0) {
      rid = id + "-Any";
      id = "Any-" + id;
    } else {
      rid = id.substring(pos+1) + "-" + id.substring(0, pos);
    }
    if (!reverseID) rid = id;

    if (direction == Transliterator.FORWARD) {
      Transliterator.unregister(id);
      t = Transliterator.createFromRules(id, rules, Transliterator.FORWARD);
      Transliterator.registerInstance(t);
      System.out.println("Registered new Transliterator: " + id);
    }

    /*String test = "\u049A\u0430\u0437\u0430\u049B";
			System.out.println(t.transliterate(test));
			t = Transliterator.getInstance(id);
			System.out.println(t.transliterate(test));
     */

    if (direction == Transliterator.REVERSE) {
      Transliterator.unregister(rid);
      t = Transliterator.createFromRules(rid, rules, Transliterator.REVERSE);
      Transliterator.registerInstance(t);
      System.out.println("Registered new Transliterator: " + rid);
    }
  }

  public static String getText(String dir, String filename) {
    try {
      BufferedReader br = BagFormatter.openUTF8Reader(dir, filename);
      StringBuffer buffer = new StringBuffer();
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if (line.length() > 0 && line.charAt(0) == '\uFEFF') line = line.substring(1);
        if (line.startsWith("//")) continue;
        buffer.append(line).append(CldrUtility.LINE_SEPARATOR);
      }
      br.close();
      String rules = buffer.toString();
      return rules;
    } catch (IOException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("Can't open " + dir + ", " + filename).initCause(e);
    }
  }

  public static void callMethod(String methodNames, Class cls) {
    for (String methodName : methodNames.split(",")) {
      try {
        Method method;
        try {
          method = cls.getMethod(methodName, (Class[]) null);
          try {
            method.invoke(null, (Object[]) null);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } catch (Exception e) {
          System.out.println("No such method: " + methodName);
          showMethods(cls);
        }
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public static void showMethods(Class cls) throws ClassNotFoundException {
    System.out.println("Possible methods of " + cls.getCanonicalName() + " are: ");
    Method[] methods = cls.getMethods();
    Set<String> names = new TreeSet<String>();
    for (int i = 0; i < methods.length; ++i) {
      if (methods[i].getGenericParameterTypes().length != 0) continue;
      int mods = methods[i].getModifiers();
      //if (!Modifier.isStatic(mods)) continue;
      String name = methods[i].getName();
      names.add(name);
    }
    for (Iterator<String> it = names.iterator(); it.hasNext();) {
      System.out.println("\t" + it.next());
    }
  }

  /**
   * Breaks lines if they are too long, or if matcher.group(1) != last. Only breaks just before matcher.
   * @param input
   * @param separator
   * @param matcher must match each possible item. The first group is significant; if different, will cause break
   * @return
   */
  static public String breakLines(CharSequence input, String separator, Matcher matcher, int width) {
    StringBuffer output = new StringBuffer();
    String lastPrefix = "";
    int lastEnd = 0;
    int lastBreakPos = 0;
    matcher.reset(input);
    while (true) {
      boolean match = matcher.find();
      if (!match) {
        output.append(input.subSequence(lastEnd, input.length()));
        break;
      }
      String prefix = matcher.group(1);
      if (!prefix.equalsIgnoreCase(lastPrefix) || matcher.end() - lastBreakPos > width) { // break before?
        output.append(separator);
        lastBreakPos = lastEnd;
      } else if (lastEnd != 0){
        output.append(' ');
      }
      output.append(input.subSequence(lastEnd, matcher.end()).toString().trim());
      lastEnd = matcher.end();
      lastPrefix = prefix;
    }
    return output.toString();
  }

  public static void showOptions(String[] args) {
    //Properties props = System.getProperties();
    System.out.println("Arguments: " + join(args," ")); //  + (props == null ? "" : " " + props));
  }

  public static double roundToDecimals(double input, int places) {
    double log10 = Math.log10(input); // 15000 => 4.xxx
    double intLog10 = Math.floor(log10);
    double scale = Math.pow(10, intLog10 - places + 1);
    double factored = Math.round(input / scale) * scale;
    //System.out.println("###\t" +input + "\t" + factored);
    return factored;
  }

  /**
   * Get a property value, returning the value if there is one (eg -Dkey=value),
   * otherwise the default value (for either empty or null).
   * 
   * @param key
   * @param valueIfNull
   * @param valueIfEmpty
   * @return
   */
  public static String getProperty(String key, String defaultValue) {
    return getProperty(key, defaultValue, defaultValue);
  }

  /**
   * Get a property value, returning the value if there is one, otherwise null.
   */
  public static String getProperty(String key) {
    return getProperty(key, null, null);
  }

  /**
   * Get a property value, returning the value if there is one (eg -Dkey=value),
   * the valueIfEmpty if there is one with no value (eg -Dkey) and the valueIfNull
   * if there is no property.
   * 
   * @param key
   * @param valueIfNull
   * @param valueIfEmpty
   * @return
   */
  public static String getProperty(String key, String valueIfNull, String valueIfEmpty) {
    String result = System.getProperty(key);
    if (result == null) {
      result = System.getProperty(key.toUpperCase(Locale.ENGLISH));
    }
    if (result == null) {
      result = System.getProperty(key.toLowerCase(Locale.ENGLISH));
    }
    if (result == null) {
      result = System.getenv(key);
    }
    if (result == null) {
      result = valueIfNull;
    } else if (result.length() == 0) {
      result = valueIfEmpty;
    }
    System.out.println("-D" + key + "=" + result);
    return result;
  }

  public static String hex(byte[] bytes, int start, int end, String separator) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < end; ++i) {
      if (result.length() != 0) {
        result.append(separator);
      }
      result.append(Utility.hex(bytes[i]&0xFF,2));
    }
    return result.toString();
  }

  public static boolean getProperty(String string, boolean b) {
    return getProperty(string, b ? "true" : "false", "true").matches("(?i)T|TRUE");
  }

  public static String checkValidDirectory(String sourceDirectory) {
    return checkValidFile(sourceDirectory, true, null);
  }

  public static String checkValidDirectory(String sourceDirectory, String correction) {
    return checkValidFile(sourceDirectory, true, correction);
  }

  public static String checkValidFile(String sourceDirectory, boolean checkForDirectory, String correction) {
    File file = null;
    String canonicalPath = null;
    try {
      file = new File(sourceDirectory);
      canonicalPath = file.getCanonicalPath() + File.separatorChar;
    } catch (Exception e) {
    }
    if (file == null || canonicalPath == null || checkForDirectory && !file.isDirectory()) {
      throw new RuntimeException("Directory not found: " + sourceDirectory + (canonicalPath == null ? "" : " => " + canonicalPath) 
              + (correction == null ? "" : CldrUtility.LINE_SEPARATOR + correction));
    }
    return canonicalPath;
  }

  /**
   * Copy up to matching line (not included). If output is null, then just skip until.
   * @param oldFile
   * @param readUntilPattern
   * @param output
   * @throws IOException
   */
  public static void copyUpTo(BufferedReader oldFile, final Pattern readUntilPattern,
          final PrintWriter output, boolean includeMatchingLine) throws IOException {
    Matcher readUntil = readUntilPattern == null ? null : readUntilPattern.matcher("");
    while (true) {
      String line = oldFile.readLine();
      if (line == null) {
        break;
      }
      if (line.startsWith("\uFEFF")) {
        line = line.substring(1);
      }
      if (readUntil != null && readUntil.reset(line).matches()) {
        if (includeMatchingLine && output != null) {
          output.println(line);
        }
        break;
      }
      if (output != null) {
        output.println(line);
      }
    }
  }

  private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");
  static {
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public static String isoFormat(Date date) {
    synchronized(df) {
      return df.format(date);
    }
  }
}
