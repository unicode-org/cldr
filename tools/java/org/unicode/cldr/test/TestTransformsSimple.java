package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTransformsSimple {
  
  private static final boolean verbose = Utility.getProperty("verbose", false);
  static CLDRTransforms transforms;
  
  public static void main(String[] args) throws IOException {
    String filter = System.getProperty("filter");
//  Matcher matcher1 = CLDRTransforms.TRANSFORM_ID_PATTERN.matcher("abc-def/ghi");
//  boolean foo = matcher1.matches();
//  Matcher matcher2 = CLDRTransforms.TRANSFORM_ID_PATTERN.matcher("abc-def");
//  foo = matcher2.matches();
    transforms = CLDRTransforms.getinstance(null, ".*(Tamil|Jamo).*");
    System.out.println("Start");
    checkTamil(filter);
    checkJamo(filter);
  }
  
  private static void checkTamil(String filter) throws IOException {
    {
      String name = "Tamil-Devanagari";
      Transliterator tamil_devanagari = transforms.getInstance(name);
      Transliterator devanagari_tamil = transforms.getReverseInstance(name);
      writeFile(name, new UnicodeSet("[[:block=tamil:]-[ௗ]]"), null, tamil_devanagari, devanagari_tamil, false, null, null);
    }
  }
  
  private static void checkJamo(String filter) throws IOException {
    {
      String name = "Latin-ConjoiningJamo";
      Transliterator fromLatin = transforms.getInstance(name);
      Transliterator toLatin = transforms.getReverseInstance(name);
      UnicodeSet sourceSet = getRepresentativeHangul();
      System.out.println(sourceSet.size() + "\t" + sourceSet.toPattern(false));
      
      UnicodeSet multiply = new UnicodeSet();
      for (UnicodeSetIterator it = new UnicodeSetIterator(sourceSet); it.next();) {
        for (UnicodeSetIterator it2 = new UnicodeSetIterator(sourceSet); it2.next();) {
          String source1 = it.getString() + it2.getString(); // try all combinations.
          multiply.add(source1);
        }
      }
      
//    latin.addAll(toTarget.getSourceSet())
//    .addAll(toTarget.getTargetSet())
//    .addAll(fromTarget.getSourceSet())
//    .addAll(fromTarget.getTargetSet());
//    latin.retainAll(new UnicodeSet("[[:latin:][:common:][:inherited:]]"));
      
      //Transliterator.DEBUG = true;  
      Transliterator nfd = Transliterator.getInstance("nfd");
      
      UnicodeSet specials = null; // new UnicodeSet("[{ch}]");
      writeFile(name, multiply, nfd, toLatin, fromLatin, true, null, specials);
    }
  }

  private static void writeFile(String title, UnicodeSet sourceSet, Transliterator nfd, Transliterator toLatin, 
          Transliterator fromLatin, boolean doLatin, UnicodeSet nativeSpecials, UnicodeSet latinSpecials) throws IOException {
    int errorCount = 0;
    PrintWriter out = BagFormatter.openUTF8Writer(org.unicode.cldr.util.Utility.GEN_DIRECTORY + "transTest/", title + ".html");
    out.println("<html><head>");
    out.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'></head><body>");
    if (nativeSpecials != null) {
      out.println("<h1>Specials</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
      showItems(out, true, "Source", "ToLatin", "FromLatin", "BackToLatin");
      for (UnicodeSetIterator it = new UnicodeSetIterator(nativeSpecials); it.next();) {
        String item = it.getString();
        errorCount = checkString(out, item, nfd, fromLatin, toLatin, errorCount, null);
        errorCount = checkString(out, item, nfd, fromLatin, toLatin, errorCount, "-");
      }
      System.out.println("Special failures:\t" + errorCount);
      out.println("</table><p>Special failures:\t" + errorCount + "</p>");
    }
    
    if (latinSpecials != null) {
      out.println("<h1>Specials</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
      showItems(out, true, "Latin", "ToNative", "BackToLatin", "BackToNative");
      for (UnicodeSetIterator it = new UnicodeSetIterator(latinSpecials); it.next();) {
        String item = it.getString();
        errorCount = checkString(out, item, nfd, toLatin, fromLatin, errorCount, null);
        //errorCount = checkString(out, item, nfd, fromLatin, toLatin, errorCount, "-");
      }
      System.out.println("Special failures:\t" + errorCount);
      out.println("</table><p>Special failures:\t" + errorCount + "</p>");
    }
    
    if (doLatin) {
      out.println("<h1>Latin failures</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
      showItems(out, true, "Latin", "Target", "BackToLatin", "BackToTarget");
      errorCount = checkLatin(out, fromLatin, toLatin);
      System.out.println("Latin failures:\t" + errorCount);
      out.println("</table><p>Latin failures:\t" + errorCount + "</p>");
    }
    
    out.println("<h1>Not Reversible</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
    errorCount = showMappings(out, sourceSet, null, nfd, fromLatin, toLatin);
    System.out.println("Reversible failures:\t" + errorCount);
    out.println("</table><p>Reversible failures:\t" + errorCount + "</p>");
    
    out.println("<h1>Has Unneeded Separator</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
    int separators = showMappings(out, sourceSet, "-", nfd, fromLatin, toLatin);
    System.out.println("Separator failures:\t" + separators);
    out.println("</table><p>Separator failures:\t" + errorCount + "</p>");
    
    out.print("</body></html>");
    out.close();
  }
  
  static UnicodeSet latin = new UnicodeSet("[a-z]");
  
  private static int checkLatin(PrintWriter out, Transliterator fromLatin, Transliterator toLatin) {
    int errorCount = 0;
    for (UnicodeSetIterator it = new UnicodeSetIterator(latin); it.next();) {
      String source = it.getString();
      String to = fromLatin.transliterate(source);
      if (latin.containsSome(to)) {
        String from = toLatin.transliterate(to);
        String backto = toLatin.transliterate(from);
        errorCount += showItems(out, false, source, to, from, backto);
      }
    }
    return errorCount;
  }
  
  private static int showMappings(PrintWriter out, UnicodeSet testSet, String separator, Transliterator nfd, Transliterator fromLatin, Transliterator toLatin) {
    int errorCount = 0;
    if (separator == null) {
      showItems(out, true, "Source", "ToLatin", "FromLatin", "BackToLatin");
    } else {
      showItems(out, true, "Source", "ToLatin", "FromLatin", "BackToLatin", "WithoutSeparator");
    }
    for (UnicodeSetIterator it = new UnicodeSetIterator(testSet); it.next();) {
      errorCount = checkString(out, it.getString(), nfd, fromLatin, toLatin, errorCount, separator);
    }
    return errorCount;
  }
  
  private static int checkString(PrintWriter out, String source1, Transliterator nfd, Transliterator fromLatin, Transliterator toLatin, int errorCount, String separator) {
    String source = nfd == null ? source1 : nfd.transliterate(source1);
    String to = toLatin.transliterate(source);
    String from = fromLatin.transliterate(to);
    if (separator == null) {
      final boolean bad = !source.equals(from);
      if (bad || verbose) {
        String backto = toLatin.transliterate(from);
        errorCount += 1;
        showItems(out, false, source, to, from, backto, bad ? "FAIL" : null);
      } else {
        //showItems(out, source, to, from, "OK");
      }
    } else {
      if (to.contains(separator)) { // check separators, only put in when needed
        String otherTo = to.replace("-","");
        String otherFrom =  fromLatin.transliterate(otherTo);
        final boolean bad = otherFrom.equals(from);
        if (bad) {
          //String backto = toLatin.transliterate(from);
          errorCount += 1;
          showItems(out, false, source, to, from, otherTo, otherFrom, bad ? "FAIL" : null);
        }
      }
    }
    return errorCount;
  }
  
  private static int showItems(PrintWriter out, boolean th, String... sourceList) {
    out.println("<tr>");
    for (String source : sourceList) {
      if (source == null) continue;
      System.out.print("<" + source + ">\t");
      out.println((th ? "<th>" : "<td>") + pretty(source) + (th ? "</th>" : "</td>"));
    }
    System.out.println();
    out.println("</tr>");
    out.flush();
    return 1;
  }
  
  static UnicodeSet lead = new UnicodeSet("[:Hangul_Syllable_Type=Leading_Jamo:]");
  static UnicodeSet vowel = new UnicodeSet("[:Hangul_Syllable_Type=Vowel_Jamo:]");
  static UnicodeSet trail = new UnicodeSet("[:Hangul_Syllable_Type=Trailing_Jamo:]");
  
  private static String pretty(String source) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < source.length(); ++i) {
      char c = source.charAt(i);
      String color = lead.contains(c) ? "FFcccc" 
          : vowel.contains(c) ? "ccFFcc" 
              : trail.contains(c) ? "ccccFF" : "FFFFFF";
      result.append("<span style='background-color: #"+ color + "'>"  + c + "</span>");
    }
    return result.toString();
  }
  
  public static UnicodeSet getRepresentativeHangul() {
    UnicodeSet sourceSet = new UnicodeSet();
    getRepresentativeHangul(2, sourceSet, false);
    getRepresentativeHangul(3, sourceSet, false);
    getRepresentativeHangul(2, sourceSet, true);
    getRepresentativeHangul(3, sourceSet, true);
    return sourceSet;
  }
  
  private static void getRepresentativeHangul(int leng, UnicodeSet resultToAddTo, boolean noFirstConsonant) {
    UnicodeSet notYetSeen = new UnicodeSet();
    for (char c = '\uAC00'; c <  '\uD7AF'; ++c) {
      String charStr = String.valueOf(c);
      String decomp = Normalizer.decompose(charStr, false);
      if (decomp.length() != leng) {
        continue; // only take one length at a time
      }
      if (decomp.startsWith("ᄋ") != noFirstConsonant) {
        continue;
      }
      if (!notYetSeen.containsAll(decomp)) {
        resultToAddTo.add(c);
        notYetSeen.addAll(decomp);
      }
    }
  }
}