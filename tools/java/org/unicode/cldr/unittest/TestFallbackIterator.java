package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;

import org.unicode.cldr.tool.FallbackIterator;

import com.ibm.icu.dev.test.TestFmwk;

public class TestFallbackIterator extends TestFmwk {
  public static void main(String[] args) {
    new TestFallbackIterator().run(args);
  }
  public void TestSimpleFallbacks() {
    String[] tests = {
            "en-Latn-US-foobar, en-Latn-US, en-Latn, en",
            "no-NO, nb-NO, nb, no",
            "nb-NO, no-NO, nb, no",
            "no-YU, nb-CS, no-CS, nb-YU, nb, no",
            "cmn, zh",
            "zh-cmn, zh",
            "zh-YU, zh-CS, zh",
            "zh-Hant-YU, zh-Hant-CS, zh-Hant, zh-TW, zh-Hant-TW, zh",
            "zh-CN, zh-Hans-CN, zh-Hans, zh",
            "zh-Hans, zh-CN, zh-Hans-CN, zh",
            "zh-Hans-CN, zh-Hans, zh-CN, zh",
            "zh-TW, zh-Hant-TW, zh-Hant, zh",
            "zh-Hant, zh-TW, zh-Hant-TW, zh",
            "zh-Hant-TW, zh-Hant, zh-TW, zh",
            "zh-Hant-TW-foobar, zh-Hant-TW, zh-Hant, zh-TW, zh",
    };
    for (String testString : tests) {
      String[] test = testString.split(",\\s*");
      FallbackIterator it = new FallbackIterator(test[0]);
      // get the fallback list
      ArrayList<String> items = new ArrayList<String>();
      while (it.hasNext())  {
        items.add(it.next());
      }
      // expected is the whole list, since the first item is always the same
      assertEquals("Fallback chain", Arrays.asList(test).toString(), items.toString());
    }
  }
}
