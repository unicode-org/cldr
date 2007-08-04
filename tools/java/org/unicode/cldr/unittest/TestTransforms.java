package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTransforms;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;

import java.util.Locale;

public class TestTransforms extends TestFmwk {

  public static void main(String[] args) throws Exception {
    new TestTransforms().run(args);
  }

  enum Options {transliterator, roundtrip};
  
  public void test1461() {
    CLDRTransforms transforms = null;
    System.out.println("hi");

    String[][] tests = {
        { "transliterator=", "Katakana-Latin"},
        { "\u30CF \u30CF\uFF70 \u30CF\uFF9E \u30CF\uFF9F", "ha hā ba pa" },
        { "transliterator=", "Hangul-Latin"},
        { "roundtrip=", "true"},
        { "갗", "gach"},
        { "느", "neu"},
        };

    Transliterator transform = null;
    Transliterator inverse = null;
    String id = null;
    boolean roundtrip = false;
    for (String[] items : tests) {
      String source = items[0];
      String target = items[1];
      if (source.endsWith("=")) {
        switch (Options.valueOf(source.substring(0,source.length()-1).toLowerCase(Locale.ENGLISH))) {
          case transliterator:
            if (transforms == null) {
              transforms = CLDRTransforms.getinstance(isVerbose() ? getLogPrintWriter() : null, null);
            }
            id = target;
            transform = transforms.getInstance(id);
            inverse = transforms.getReverseInstance(target);
            break;
          case roundtrip:
            roundtrip = target.toLowerCase(Locale.ENGLISH).charAt(0) == 't';
            break;
        }
        continue;
      }
      String result = transform.transliterate(source);
      assertEquals(id + ":from " + source, target, result);
      if (roundtrip) {
        String result2 = inverse.transliterate(target);
        assertEquals(id + " (inv): from " + target, source, result2);
      }
    }
  }
}