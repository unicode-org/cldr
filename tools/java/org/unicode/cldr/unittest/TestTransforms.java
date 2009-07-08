package org.unicode.cldr.unittest;

import java.util.Locale;

import org.unicode.cldr.util.CLDRTransforms;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transliterator;

public class TestTransforms extends TestFmwk {

  public static void main(String[] args) throws Exception {
    new TestTransforms().run(args);
  }

  enum Options {transliterator, roundtrip};
  
  public void test1461() {
    CLDRTransforms.registerCldrTransforms(CLDRTransforms.TRANSFORM_DIR, null, isVerbose() ? getLogPrintWriter() : null);
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
            id = target;
            transform = Transliterator.getInstance(id);
            inverse = Transliterator.getInstance(id, Transliterator.REVERSE);
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