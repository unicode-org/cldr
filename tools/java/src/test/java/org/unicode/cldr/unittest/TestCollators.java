package org.unicode.cldr.unittest;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PatternCache;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

public class TestCollators extends TestFmwk {
    public static void main(String[] args) {
        new TestCollators().run(args);
    }
    
    public void TestAccessByCldrConfig() {
        Collator col = CLDRConfig.getInstance().getCollatorRoot();
        String[] testSequence = {
            "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊", "😋", 
            "😎", "😍", "😘", "🥰", "ℹ", "a", "A", "b", 
            null,
            "🏻", "🏼", "🏽", "🏾", "🏿", "🦰", "🦱", "🦳", "🦲", "🏻a"};
        String last = null;
        for (String test : testSequence) {
            if (last != null && test != null) {
                int comp = col.compare(last, test);
                assertEquals(last + " < " + test + " — " + Utility.hex(last) + " < " + Utility.hex(test), -1, comp);
            }
            last = test;
        }
    }
    public void TestBuildable() {
        for (String locale : CollatorSource.getAvailableLocales()) {
            logln(locale);
            for (String type : CollatorSource.getAvailableTypes(locale)) {
                logln("\t" + type);
                try {
                    RuleBasedCollator collator = CollatorSource.build(locale, type);
                    collator.compare("a", "b");
                } catch (Exception e) {
                    errln(e.toString());
                }
            }
        }
    }

    static class CollatorSource {
        static Factory cldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "collation/", ".*");

        static M3<String, String, String> localeToTypeToPath = ChainedMap.of(
            new LinkedHashMap<String, Object>(),
            new LinkedHashMap<String, Object>(),
            String.class);

        static RuleBasedCollator build(String locale, String type) {
            CLDRFile cldrFile = cldrFactory.make(locale, false); // don't need resolved
            String path = localeToTypeToPath.get(locale, type);
            String rules = cldrFile.getStringValue(path);
            try {
                return new RuleBasedCollator(rules);
            } catch (Exception e) {
                throw new IllegalArgumentException(locale + ", " + type + ", " + e);
            }
        }

        static Set<String> getAvailableLocales() {
            return cldrFactory.getAvailable();
        }

        static final Pattern TYPE = PatternCache.get("//ldml/collations/collation\\[@type=\"([^\"]+)\"\\].*/cr");

        static public Set<String> getAvailableTypes(String locale) {
            CLDRFile cldrFile = cldrFactory.make(locale, false); // don't need resolved
            Set<String> results = new LinkedHashSet<>();
            Matcher m = TYPE.matcher("");
            for (String path : cldrFile) {
                if (m.reset(path).matches()) {
                    String type = m.group(1);
                    boolean newOne = results.add(type);
                    if (newOne) {
                        localeToTypeToPath.put(locale, type, path); // hack to get around the fact that some attributes should be nondistinguishing
                    }
                }
            }
            return results;
        }
    }
}
