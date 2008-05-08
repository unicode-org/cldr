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
            "cmn, zh",
            "zh-cmn, zh, cmn",
            "zh-YU, zh-CS, cmn-CS, cmn-YU, zh, cmn",
            "zh-CN, cmn-CN, zh-Hans-CN, cmn-Hans-CN, zh-Hans, cmn-Hans, zh, cmn",
            "zh-Hans, cmn-Hans, zh-CN, cmn-CN, zh-Hans-CN, cmn-Hans-CN, zh, cmn",
            "zh-Hans-CN, cmn-Hans-CN, zh-Hans, cmn-Hans, zh-CN, cmn-CN, zh, cmn",
            "zh-TW, cmn-TW, zh-Hant-TW, cmn-Hant-TW, zh-Hant, cmn-Hant, zh, cmn",
            "zh-Hant, cmn-Hant, zh-TW, cmn-TW, zh-Hant-TW, cmn-Hant-TW, zh, cmn",
            "zh-Hant-TW, cmn-Hant-TW, zh-Hant, cmn-Hant, zh-TW, cmn-TW, zh, cmn",
            "zh-Hant-TW-foobar, cmn-Hant-TW-foobar, zh-Hant-TW, cmn-Hant-TW, zh-Hant, cmn-Hant, zh-TW, cmn-TW, zh, cmn",
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
