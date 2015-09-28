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
            // throw in some extreme cases with multiple variants
            "zh-TW-foobar-variant, zh-Hant-TW-foobar-variant, zh-Hant-TW-foobar, zh-Hant-TW, zh-Hant, zh",
            "zh-HK-foobar-variant, zh-Hant-HK-foobar-variant, zh-Hant-HK-foobar, zh-Hant-HK, zh-Hant, zh",
            "zh-HK, zh-Hant-HK, zh-Hant, zh",
            "zh-Hant-HK, zh-Hant, zh",
            "zh-Hans-HK, zh-Hans, zh",
            "zh-Hant-SG, zh-Hant, zh",
            "zh-Hans-SG, zh-Hans, zh",
            "zh-SG, zh-Hans-SG, zh-Hans, zh",
            "zh-US, zh-Hans-US, zh-Hans, zh",
            "zh-US-foobar, zh-Hans-US-foobar, zh-Hans-US, zh-Hans, zh",
            "zh-foobar-variant, zh-foobar, zh",
            "en-Latn-US-foobar, en-Latn-US, en-Latn, en",
            "no-NO, nb-NO, nb, no",
            "nb-NO, no-NO, nb, no",
            "no-bok, nb, no", // note, "nb-bok" is not legal, but doesn't
            // hurt
            "no-YU, nb-CS, no-CS, nb-YU, nb, no",
            "sh-CS, sh-YU, sr-Latn-CS, sr-Latn-YU, sr-Latn, sr",
            "sh-Cyrl-CS, sh-Cyrl-YU, sr-Cyrl-CS, sr-Cyrl-YU, sr-Cyrl, sr",
            "cmn, zh", "zh-cmn, zh",
            "zh-YU, zh-CS, zh-Hans-CS, zh-Hans-YU, zh-Hans, zh",
            "zh-Hant-YU, zh-Hant-CS, zh-Hant, zh",
            "zh-CN, zh-Hans-CN, zh-Hans, zh", "zh-Hans, zh",
            "zh-Hans-CN, zh-Hans, zh", "zh-TW, zh-Hant-TW, zh-Hant, zh",
            "zh-Hant, zh", "zh-Hant-TW, zh-Hant, zh",
            "zh-Hant-TW-foobar, zh-Hant-TW, zh-Hant, zh", };
        for (String testString : tests) {
            String[] test = testString.split(",\\s*");
            FallbackIterator it = new FallbackIterator(test[0]);
            // get the fallback list
            ArrayList<String> items = new ArrayList<String>();
            while (it.hasNext()) {
                items.add(it.next());
            }
            // expected is the whole list, since the first item is always the
            // same
            assertEquals("Fallback chain for " + test[0], Arrays.asList(test)
                .toString(), items.toString());
        }
    }
}
