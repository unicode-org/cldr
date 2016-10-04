package org.unicode.cldr.unittest;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UnicodeSet;

public class TestAnnotations extends TestFmwk {
    public static void main(String[] args) {
        new TestAnnotations().run(args);
    }

    enum Containment {
        contains, empty, not_contains
    }

    public void TestBasic() {
        String[][] tests = {
            { "en", "[\u2650]", "contains", "sagitarius", "zodiac" },
            { "en", "[\u0020]", "empty" },
            { "en", "[\u2651]", "not_contains", "foobar" },
        };
        for (String[] test : tests) {
            UnicodeMap<Annotations> data = Annotations.getData(test[0]);
            UnicodeSet us = new UnicodeSet(test[1]);
            Set<String> annotations = new LinkedHashSet<>();
            Containment contains = Containment.valueOf(test[2]);
            for (int i = 3; i < test.length; ++i) {
                annotations.add(test[i]);
            }
            for (String s : us) {
                Set<String> set = data.get(s).getKeywords();
                if (set == null) {
                    set = Collections.emptySet();
                }
                switch (contains) {
                case contains:
                    if (Collections.disjoint(set, annotations)) {
                        LinkedHashSet<String> temp = new LinkedHashSet<>(annotations);
                        temp.removeAll(set);
                        assertEquals("Missing items", Collections.EMPTY_SET, temp);
                    }
                    break;
                case not_contains:
                    if (!Collections.disjoint(set, annotations)) {
                        LinkedHashSet<String> temp = new LinkedHashSet<>(annotations);
                        temp.retainAll(set);
                        assertEquals("Extra items", Collections.EMPTY_SET, temp);
                    }
                    break;
                case empty:
                    assertEquals("mismatch", Collections.emptySet(), set);
                    break;
                }
            }
        }
    }

    public void TestList() {
        if (isVerbose()) {
            for (String locale : Annotations.getAvailable()) {
                for (EntryRange<Annotations> s : Annotations.getData(locale).entryRanges()) {
                    logln(s.toString());
                }
            }
        }
    }

    public void TestNames() {
        AnnotationSet eng = Annotations.getDataSet("en");
        String[][] tests = {
            {"🇪🇺","European Union","flag"},
            {"#️⃣","keycap: #","keycap"},
            {"9️⃣","keycap: 9","keycap"},

            {"💏","kiss","couple"},
            {"👩‍❤️‍💋‍👩","kiss: woman, woman","couple|woman"},
            {"💑","couple with heart","couple|love"},
            {"👩‍❤️‍👩","couple with heart: woman, woman","couple|love|woman"},
            {"👪","family","family"},
            {"👩‍👩‍👧","family: woman, woman, girl","family|woman|girl"},
            {"👦🏻","boy: light skin tone","boy|young|light skin tone"},
            {"👩🏿","woman: dark skin tone","woman|dark skin tone"},
            {"👨‍⚖","man judge","justice|man|scales"},
            {"👨🏿‍⚖","man judge: dark skin tone","justice|man|scales|dark skin tone"},
            {"👩‍⚖","woman judge","judge|scales|woman"},
            {"👩🏼‍⚖","woman judge: medium-light skin tone","judge|scales|woman|medium-light skin tone"},
            {"👮","police officer","cop|officer|police"},
            {"👮🏿","police officer: dark skin tone","cop|officer|police|dark skin tone"},
            {"👮‍♂️","man police officer","cop|man|officer|police"},
            {"👮🏼‍♂️","man police officer: medium-light skin tone","cop|officer|police|man|medium-light skin tone"},
            {"👮‍♀️","woman police officer","cop|officer|police|woman"},
            {"👮🏿‍♀️","woman police officer: dark skin tone","cop|officer|police|woman|dark skin tone"},
            {"🚴","person biking","bicycle|biking|cyclist"},
            {"🚴🏿","person biking: dark skin tone","bicycle|biking|cyclist|dark skin tone"},
            {"🚴‍♂️","man biking","bicycle|biking|cyclist|man"},
            {"🚴🏿‍♂️","man biking: dark skin tone","bicycle|biking|cyclist|man|dark skin tone"},
            {"🚴‍♀️","woman biking","bicycle|biking|cyclist|woman"},
            {"🚴🏿‍♀️","woman biking: dark skin tone","bicycle|biking|cyclist|woman|dark skin tone"},
        };

        Splitter BAR = Splitter.on('|').trimResults();
        for (String[] test : tests) {
            String emoji = test[0];
            String expectedName = test[1];
            Set<String> expectedKeywords = new HashSet<>(BAR.splitToList(test[2]));
            final String shortName = eng.getShortName(emoji);
            final Set<String> keywords = eng.getKeywords(emoji);
            assertEquals("short name for " + emoji, expectedName, shortName);
            assertEquals("keywords for " + emoji, expectedKeywords, keywords);
        }
    }
}
