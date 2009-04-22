package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.translit.TransliteratorTest;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTransformsSimple extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TestTransformsSimple().run(args);
    }
  
  private static final boolean verbose = Utility.getProperty("verbose", false);
  private static PrintWriter out = verbose ? new PrintWriter(System.out, true) : null;
  static CLDRTransforms transforms;
  
  public void TestSimple() throws IOException {
    String filter = System.getProperty("filter");
//  Matcher matcher1 = CLDRTransforms.TRANSFORM_ID_PATTERN.matcher("abc-def/ghi");
//  boolean foo = matcher1.matches();
//  Matcher matcher2 = CLDRTransforms.TRANSFORM_ID_PATTERN.matcher("abc-def");
//  foo = matcher2.matches();
    CLDRTransforms.registerCldrTransforms(null, ".*(Tamil|Jamo).*", out);
    System.out.println("Start");
    checkTamil(filter);
    checkJamo(filter);
  }
  
  public void TestChinese() {
    CLDRTransforms.registerCldrTransforms(null, ".*(Han|Pinyin).*", out);
    Transliterator hanLatin = Transliterator.getInstance("Han-Latin");
    assertTransform("Transform", "zào Unicode", hanLatin, "造Unicode");
    assertTransform("Transform", "zài chuàng zào Unicode zhī qián", hanLatin, "在創造Unicode之前");
  }
  
  public void TestHangul() {
    CLDRTransforms.registerCldrTransforms(null, ".*(Hangul|Jamo).*", out);

    Transliterator lh = Transliterator.getInstance("Latin-Hangul");
    Transliterator hl = lh.getInverse();
    
    assertTransform("Transform", "알따", lh, hl, "altta", "al-tta");
    assertTransform("Transform", "알빠", lh, hl, "alppa", "al-ppa");

    assertTransform("Transform", "츠", lh, "ch");
   
    assertTransform("Transform", "아따", lh, hl, "atta", "a-tta");
    assertTransform("Transform", "아빠", lh, hl, "appa", "a-ppa");
    assertTransform("Transform", "아짜", lh, hl, "ajja", "a-jja");
    assertTransform("Transform", "아까", lh, hl, "akka", "a-kka");
    assertTransform("Transform", "아싸", lh, hl, "assa", "a-ssa");
    assertTransform("Transform", "아차", lh, hl, "acha", "a-cha");
    assertTransform("Transform", "악사", lh, hl, "agsa", "ag-sa");
    assertTransform("Transform", "안자", lh, hl, "anja", "an-ja");
    assertTransform("Transform", "안하", lh, hl, "anha", "an-ha");
    assertTransform("Transform", "알가", lh, hl, "alga", "al-ga");
    assertTransform("Transform", "알마", lh, hl, "alma", "al-ma");
    assertTransform("Transform", "알바", lh, hl, "alba", "al-ba");
    assertTransform("Transform", "알사", lh, hl, "alsa", "al-sa");
    assertTransform("Transform", "알타", lh, hl, "alta", "al-ta");
    assertTransform("Transform", "알파", lh, hl, "alpa", "al-pa");
    assertTransform("Transform", "알하", lh, hl, "alha", "al-ha");
    assertTransform("Transform", "압사", lh, hl, "absa", "ab-sa");
    assertTransform("Transform", "안가", lh, hl, "anga", "an-ga");
    assertTransform("Transform", "악싸", lh, hl, "agssa", "ag-ssa");
    assertTransform("Transform", "안짜", lh, hl, "anjja", "an-jja");
    assertTransform("Transform", "알싸", lh, hl, "alssa", "al-ssa");
    assertTransform("Transform", "알따", lh, hl, "altta", "al-tta");
    assertTransform("Transform", "알빠", lh, hl, "alppa", "al-ppa");
    assertTransform("Transform", "압싸", lh, hl, "abssa", "ab-ssa");
    assertTransform("Transform", "앆카", lh, hl, "akkka", "akk-ka");
    assertTransform("Transform", "았사", lh, hl, "asssa", "ass-sa");

//    1. Latin->Hangul transliterator maps 'ch' to '킇' (splitting the sequence
//            into
//            'c' and 'h' and inserting an implicit vowel 'ㅡ'). It'd be better to map a
//            *stand-alone* 'ch' to '츠'  
//
//            2.  As mentioned in http://www.unicode.org/cldr/transliteration_guidelines.html
//            (Korean section),  
//
//            - altta = alt-ta  앑타   should be ' al-tta 알따'
//
//            - alppa = alp-pa  앒파   : should be 'al-ppa  알빠'

  }

  private void assertTransform(String message, String expected, StringTransform t, String source) {
    assertEquals(message + " " + source, expected, t.transform(source));
  }


  private void assertTransform(String message, String expected, StringTransform t, StringTransform back, String... source) {
    for (String s : source) {
      assertEquals(message + " " + s, expected, t.transform(s));
    }
    assertEquals(message + " " + expected, source[0], back.transform(expected));
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