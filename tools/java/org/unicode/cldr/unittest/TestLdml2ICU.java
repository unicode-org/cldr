package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.Relation;

public class TestLdml2ICU extends TestFmwk {
    static final TestAll.TestInfo info = TestInfo.getInstance();
    static Factory factory;
    static final Set<String> CURRENCY_CODES = info.getStandardCodes().getAvailableCodes("currency");
    static final Set<String> TIMEZONE_CODES;
    static {
        Set<String> temp2 = new HashSet<String>();
        Set<String> temp = info.getStandardCodes().getAvailableCodes("tzid");
        for (String zone : temp) {
            temp2.add(zone.replace("/", ":"));
        }
        TIMEZONE_CODES = Collections.unmodifiableSet(temp2);
    }

    public static void main(String[] args) {
        // "/Users/markdavis/Documents/workspace/cldr-1.9.0/common/main/"
        factory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*", DraftStatus.contributed);
        new TestLdml2ICU().run(args);
    }

    public void TestEnglish() {
        checkLocale("en");
    }

    public void TestArabic() {
        checkLocale("ar");
    }

    static final Pattern SKIP = Pattern.compile("^//ldml/(identity|posix/messages)|/(default|alias|commonlyUsed)$");

    public void checkLocale(String locale) {
        // /Users/markdavis/Documents/workspace/icu/source/data/locales/en.txt

        CLDRFile plain = factory.make(locale, false);
        IcuValues icu = IcuValues.getInstance(locale);
        SupplementalDataInfo supp = info.getSupplementalDataInfo();
        XPathParts parts = new XPathParts();

        // show(supp.getDeprecationInfo());
        Matcher skipper = SKIP.matcher("");
        Set<String> seen = new HashSet<String>();

        for (Iterator<String> it = plain.iterator("", CLDRFile.ldmlComparator); it.hasNext();) {
            String xpath = it.next();
            if (skipper.reset(xpath).find()) {
                continue;
            }
            String value = plain.getStringValue(xpath);
            boolean inICU = value.isEmpty() || icu.contains(value);
            if (supp.hasDeprecatedItem("ldml", parts.set(xpath))) {
                warnln("CLDR has deprecated path, with value <" + value + "> for " + xpath);
            } else if (!inICU) {
                errln("ICU missing value <" + value + "> for " + xpath);
            }
            seen.add(value);
        }
        CLDRFile resolved = factory.make(locale, true);
        Set<String> valuesInResolved = new HashSet<String>();
        for (String xpath : resolved) {
            valuesInResolved.add(resolved.getStringValue(xpath));
        }

        Set<String> notSeen = icu.getAllBut(valuesInResolved.iterator());
        notSeen.removeAll(CURRENCY_CODES);
        notSeen.removeAll(TIMEZONE_CODES);

        for (String s : notSeen) {
            if (s.startsWith("meta:")) continue;
            warnln("ICU Values not in CLDR: " + s);
        }
    }

    private void show(Map<String, Map<String, Relation<String, String>>> deprecationInfo) {
        for (Entry<String, Map<String, Relation<String, String>>> entry0 : deprecationInfo.entrySet()) {
            String type = entry0.getKey();
            for (Entry<String, Relation<String, String>> entry1 : entry0.getValue().entrySet()) {
                String element = entry1.getKey();
                for (Entry<String, String> entry2 : entry1.getValue().entrySet()) {
                    String attribute = entry2.getKey();
                    String value = entry2.getValue();
                    System.out.println(type + "\t" + element + "\t" + attribute + "\t" + value);
                }
            }
        }
    }

    static class IcuValues {
        static final String IcuBaseDir = "/Users/markdavis/Documents/workspace/icu/source/data/";
        static final String[] dirs = { "locales/", "lang/", "curr/", "region/", "zone/" };
        static final Pattern quotedPattern = Pattern.compile("\"([^\"]*)\"");
        static final Map<String, IcuValues> cache = new HashMap();

        final Set<String> values;

        public IcuValues(String locale) {
            HashSet<String> tempValues = new HashSet<String>();
            Matcher matcher = quotedPattern.matcher("");
            for (String dir : dirs) {
                try {
                    BufferedReader in = BagFormatter.openUTF8Reader(IcuBaseDir + dir, locale + ".txt");
                    while (true) {
                        String line = in.readLine();
                        if (line == null) break;
                        matcher.reset(line);
                        while (matcher.find()) {
                            tempValues.add(matcher.group(1));
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(e); // damn'd checked exceptions
                }
            }
            values = Collections.unmodifiableSet(tempValues);
        }

        public Set<String> getAllBut(Iterator<String> resolved) {
            Set<String> result = Builder.with(new TreeSet<String>()).addAll(values).removeAll(resolved).get();
            return result;
        }

        public boolean contains(String value) {
            return values.contains(value);
        }

        static IcuValues getInstance(String locale) {
            synchronized (IcuValues.class) {
                IcuValues result = cache.get(locale);
                if (result != null) {
                    return result;
                }
                result = new IcuValues(locale);
                cache.put(locale, result);
                return result;
            }
        }
    }
}
