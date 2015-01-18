package org.unicode.cldr.unittest;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.Annotations;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMap.EntryRange;
import com.ibm.icu.text.UnicodeSet;

public class TestAnnotations extends TestFmwk {
    public static void main(String[] args) {
        new TestAnnotations().run(args);
    }
    
    enum Containment {contains, empty, not_contains}
    
    public void TestBasic() {
        String[][] tests = {
                {"en", "[\u2650]", "contains", "sagitarius", "zodiac"},
                {"en", "[\u0020]", "empty"},
                {"en", "[\u2651]", "not_contains", "foobar"},
        };
        for (String[] test : tests) {
            UnicodeMap<Set<String>> data = Annotations.getData(test[0]);
            data = Annotations.getData(test[0]);
            UnicodeSet us = new UnicodeSet(test[1]);
            Set<String> annotations = new LinkedHashSet<>();
            Containment contains = Containment.valueOf(test[2]);
            for (int i = 3; i < test.length; ++i) {
                annotations.add(test[i]);
            }
            for (String s : us) {
                Set<String> set = data.get(s);
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
                for (EntryRange<Set<String>> s : Annotations.getData(locale).entryRanges()) {
                    logln(s.toString());
                }
            }
        }
    }
}
