package org.unicode.cldr.test;

import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Utility;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.translit.TransliteratorTest;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTransformsSimple extends TestFmwk {

  public static void main(String[] args) throws Exception {
    CLDRTransforms.registerCldrTransforms(null, null, out);
    new TestTransformsSimple().run(args);
  }

  private static final boolean verbose = Utility.getProperty("verbose", false);
  private static PrintWriter out = verbose ? new PrintWriter(System.out, true) : null;

  public void TestChinese() {
    //CLDRTransforms.registerCldrTransforms(null, ".*(Han|Pinyin).*", out);
    Transliterator hanLatin = Transliterator.getInstance("Han-Latin");
    assertTransform("Transform", "zào Unicode", hanLatin, "造Unicode");
    assertTransform("Transform", "zài chuàng zào Unicode zhī qián", hanLatin, "在創造Unicode之前");
  }

  public void TestHangul() {
    //CLDRTransforms.registerCldrTransforms(null, ".*(Hangul|Jamo).*", out);

    Transliterator lh = Transliterator.getInstance("Latin-Hangul");
    Transliterator hl = lh.getInverse();

    //assertRoundTripTransform("Transform", "\uAC0D\uD0C0", lh, hl);
    //assertRoundTripTransform("Transform", "\uAC0D\uB530", lh, hl);

    final UnicodeSet representativeHangul = getRepresentativeHangul();
    final boolean ok = representativeHangul.contains("갍따");
    for (UnicodeSetIterator it = new UnicodeSetIterator(representativeHangul); it.next();) {
      assertRoundTripTransform("Transform", it.getString(), lh, hl);
    }

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

  private void assertRoundTripTransform(String message, String source, Transliterator lh, Transliterator hl) {
    String to = hl.transform(source);
    String back = lh.transform(to);
    String to2 = hl.transform(source.replaceAll("(.)", "$1 ").trim());
    String to3 = hl.transform(back.replaceAll("(.)", "$1 ").trim());
    assertEquals(message + " " + source + " [" + to + "/"+ to2 + "/"+ to3 + "]", source, back);
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

  public void xTestTamil() throws IOException {
    {
      //CLDRTransforms.registerCldrTransforms(null, ".*(Tamil).*", out);
      String name = "Tamil-Devanagari";
      Transliterator tamil_devanagari = Transliterator.getInstance(name);
      Transliterator devanagari_tamil = Transliterator.getInstance(name, Transliterator.REVERSE);
      writeFile(name, new UnicodeSet("[[:block=tamil:]-[ௗ]]"), null, tamil_devanagari, devanagari_tamil, false, null, null);
    }
  }

  public void TestJamo() throws IOException {
    {
      //CLDRTransforms.registerCldrTransforms(null, ".*(Jamo).*", out);
      String name = "Latin-ConjoiningJamo";
      Transliterator fromLatin = Transliterator.getInstance(name);
      Transliterator toLatin = Transliterator.getInstance(name, Transliterator.REVERSE);
      UnicodeSet sourceSet = getRepresentativeHangul();
      logln(sourceSet.size() + "\t" + sourceSet.toPattern(false));

      Transliterator nfd = Transliterator.getInstance("nfd");

      UnicodeSet multiply = new UnicodeSet(sourceSet);
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

      UnicodeSet specials = null; // new UnicodeSet("[{ch}]");
      writeFile(name, multiply, nfd, toLatin, fromLatin, true, null, specials);
    }
  }

  private int writeFile(String title, UnicodeSet sourceSet, Transliterator nfd, Transliterator toLatin, 
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
      out.println("</table><p>Special failures:\t" + errorCount + "</p>");
      if (errorCount != 0) {
        errln("Special failures:\t" + errorCount);
        errorCount = 0;
      }
    }

    if (latinSpecials != null) {
      out.println("<h1>Specials</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
      showItems(out, true, "Latin", "ToNative", "BackToLatin", "BackToNative");
      for (UnicodeSetIterator it = new UnicodeSetIterator(latinSpecials); it.next();) {
        String item = it.getString();
        errorCount = checkString(out, item, nfd, toLatin, fromLatin, errorCount, null);
        //errorCount = checkString(out, item, nfd, fromLatin, toLatin, errorCount, "-");
      }
      out.println("</table><p>Special failures:\t" + errorCount + "</p>");
      if (errorCount != 0) {
        errln("Special failures:\t" + errorCount);
        errorCount = 0;
      }
    }

    if (doLatin) {
      out.println("<h1>Latin failures</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
      showItems(out, true, "Latin", "Target", "BackToLatin", "BackToTarget");
      errorCount = checkLatin(out, fromLatin, toLatin);
      out.println("</table><p>Latin failures:\t" + errorCount + "</p>");
      if (errorCount != 0) {
        errln("Latin failures:\t" + errorCount);
        errorCount = 0;
      }

    }

    out.println("<h1>Not Reversible</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
    errorCount = showMappings(out, sourceSet, null, nfd, fromLatin, toLatin);
    out.println("</table><p>Reversible failures:\t" + errorCount + "</p>");
    if (errorCount != 0) {
      errln("Reversible failures:\t" + errorCount);
      errorCount = 0;
    }


    out.println("<h1>Has Unneeded Separator</h1><table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse'>");
    errorCount = showMappings(out, sourceSet, "-", nfd, fromLatin, toLatin);
    out.println("</table><p>Separator failures:\t" + errorCount + "</p>");
    if (errorCount != 0) {
      errln("Separator failures:\t" + errorCount);
      errorCount = 0;
    }

    out.print("</body></html>");
    out.close();
    return errorCount;
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
        if (bad) {
          errorCount += 1;
        }
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
      //System.out.print("<" + source + ">\t");
      out.println((th ? "<th>" : "<td>") + pretty(source) + (th ? "</th>" : "</td>"));
    }
    //System.out.println();
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
    UnicodeSet sourceSet = new UnicodeSet("[{가가}{각아}{갂아}{갘카}{가까}{물엿}{굳이}{없었습}{무렷}{구디}{업섯씁}" +
            "{아따}{아빠}{아짜}{아까}{아싸}{아차}{악사}{안자}{안하}{알가}{알마}{알바}{알사}{알타}{알파}{알하}{압사}{안가}{악싸}{안짜}{알싸}{압싸}{앆카}{았사}{알따}{알빠}" +
    "{츠} {아따} {아빠} {아짜} {아까} {아싸} {아차} {악사} {안자} {안하} {알가} {알마} {알바} {알사} {알타} {알파} {알하} {압사} {안가} {악싸} {안짜} {알싸} {알따} {알빠} {압싸} {앆카} {았사}]");
    addRepresentativeHangul(sourceSet, 2, false);
    addRepresentativeHangul(sourceSet, 3, false);
    addRepresentativeHangul(sourceSet, 2, true);
    addRepresentativeHangul(sourceSet, 3, true);
    // add the boundary cases; we want an example of each case of V + L and one example of each case of T+L

    UnicodeSet more = getRepresentativeBoundaryHangul();
    sourceSet.addAll(more);
    return sourceSet;
  }

  //  public static UnicodeSet concatenate(UnicodeSet a, UnicodeSet b) {
  //    UnicodeSet c = new UnicodeSet();
  //    for (UnicodeSetIterator ai = new UnicodeSetIterator(a); ai.next();) {
  //      for (UnicodeSetIterator bi = new UnicodeSetIterator(b); bi.next();) {
  //        c.add(ai.getString() + bi.getString());
  //      }
  //    }
  //    return c;
  //  }
  //  
  //  static interface Process {
  //    public Process process(String s);
  //    public UnicodeSet getResults();
  //  }
  //  
  //  static class Processes implements Process {
  //    private Process[] processes;
  //    public Processes(Process[] processes) {
  //      this.processes = processes;
  //    }
  //    public Process process(String s) {
  //      for (int i = 0; i < processes.length; ++i) {
  //        processes[i].process(s);
  //      }
  //      return this;
  //    }
  //    public UnicodeSet getResults() {
  //      UnicodeSet result = new UnicodeSet();
  //      for (int i = 0; i < processes.length; ++i) {
  //        result.addAll(processes[i].getResults());
  //      }
  //      return result;
  //    }
  //  }
  //
  //  public static UnicodeSet ProcessDecomposed(Process process, UnicodeSet source) {
  //    for (UnicodeSetIterator ai = new UnicodeSetIterator(source); ai.next();) {
  //      String normalized;
  //      if (ai.codepoint == UnicodeSetIterator.IS_STRING) {
  //        normalized = Normalizer.normalize(ai.codepoint, Normalizer.NFD); 
  //      } else {
  //        normalized = Normalizer.normalize(ai.string, Normalizer.NFD); 
  //      }
  //      process.process(normalized);
  //    }
  //    return process.getResults();
  //  }

  private static UnicodeSet getRepresentativeBoundaryHangul() {
    UnicodeSet resultToAddTo = new UnicodeSet();
    // U+1100 ( ᄀ ) HANGUL CHOSEONG KIYEOK
    // U+1161 ( ᅡ ) HANGUL JUNGSEONG A
    UnicodeSet L = new UnicodeSet("[:hst=L:]");
    UnicodeSet V = new UnicodeSet("[:hst=V:]");
    UnicodeSet T = new UnicodeSet("[:hst=T:]");

    String prefixLV = "\u1100\u1161";
    String prefixL = "\u1100";
    String suffixV = "\u1161";
    String nullL = "\u110B";

    UnicodeSet L0 = new UnicodeSet("[\u1100\u110B]");

    // do all combinations of L0 + V + nullL + V

    for (UnicodeSetIterator iL0 = new UnicodeSetIterator(L0); iL0.next();) {
      for (UnicodeSetIterator iV = new UnicodeSetIterator(V); iV.next();) {
          for (UnicodeSetIterator iV2 = new UnicodeSetIterator(V); iV2.next();) {
            String sample = iL0.getString() + iV.getString() + nullL + iV2.getString();
            String trial = Normalizer.compose(sample, false);
            if (trial.length() == 2) {
              resultToAddTo.add(trial);
            }
        }
      }
    }

    for (UnicodeSetIterator iL = new UnicodeSetIterator(L); iL.next();) {
      // do all combinations of "g" + V + L + "a"
      final String suffix = iL.getString() + suffixV;
      for (UnicodeSetIterator iV = new UnicodeSetIterator(V); iV.next();) {
        String sample = prefixL + iV.getString() + suffix;
        String trial = Normalizer.compose(sample, false);
        if (trial.length() == 2) {
          resultToAddTo.add(trial);
        }
      }
      // do all combinations of "ga" + T + L + "a"
      for (UnicodeSetIterator iT = new UnicodeSetIterator(T); iT.next();) {
        String sample = prefixLV + iT.getString() + suffix;
        String trial = Normalizer.compose(sample, false);
        if (trial.length() == 2) {
          resultToAddTo.add(trial);
        }
      }
    }
    return resultToAddTo;
  }

  private static void addRepresentativeHangul(UnicodeSet resultToAddTo, int leng, boolean noFirstConsonant) {
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