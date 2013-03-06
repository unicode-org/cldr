package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.text.Transform;

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

    static final String[][][] supplementalMap = {
        // {{"characters.xml"},{""}},
        // {{"coverageLevels.xml"},{""}},
        // {{"dayPeriods.xml"},{""}},
        { { "genderList" }, { "genderList" } },
        // {{"languageInfo.xml"},{""}},
        // {{"likelySubtags.xml"},{""}},
        // {{"metaZones.xml"},{""}},
        // {{"numberingSystems.xml"},{""}},
        { { "plurals", "ordinals" }, { "plurals" } },
        // {{"postalCodeData.xml"},{""}},
        { { "supplementalData" }, { "supplementalData" } },
        // {{"supplementalMetadata.xml"},{""}},
        // {{"telephoneCodeData.xml"},{""}},
        // {{"windowsZones.xml"},{""}},
    };

    /*
     * currencyNumericCodes.txt
     * dayPeriods.txt
     * genderList.txt
     * icudata.rc
     * icustd.txt
     * icuver.txt
     * keyTypeData.txt
     * likelySubtags.txt
     * metadata.txt
     * metaZones.txt
     * miscfiles.mk
     * numberingSystems.txt
     * plurals.txt
     * postalCodeData.txt
     * supplementalData.txt
     * timezoneTypes.txt
     * windowsZones.txt
     * zoneinfo64.txt
     */

    public static void main(String[] args) {
        // "/Users/markdavis/Documents/workspace/cldr-1.9.0/common/main/"
        factory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*", DraftStatus.contributed);
        new TestLdml2ICU().run(args);
    }

    enum ExclusionType {
        SKIP,
        WARNING,
        VALUE;
        public static Transform<String, Pair<ExclusionType, String>> TRANSFORM = new Transform<String, Pair<ExclusionType, String>>() {
            public Pair<ExclusionType, String> transform(String source) {
                String value = null;
                if (source.contains(";")) {
                    String[] split = source.split("\\s*;\\s*");
                    source = split[0];
                    value = split[1];
                }
                ExclusionType type = ExclusionType.valueOf(source.toUpperCase());
                return Pair.of(type, value);
            }
        };
    }

    static final RegexLookup<Pair<ExclusionType, String>> exclusions =
        RegexLookup.of(ExclusionType.TRANSFORM)
            .setPatternTransform(RegexLookup.RegexFinderTransformPath)
            .loadFromFile(TestLdml2ICU.class, "../util/data/testLdml2Icu.txt");

    public void TestEnglish() {
        checkLocale("en");
    }

    public void TestArabic() {
        checkLocale("ar");
    }

    public void TestRoot() {
        checkLocale("root");
    }

    public void TestSupplemental() {
        for (String[][] cldrVsIcu : supplementalMap) {
            Relation<String, String> cldrData = Relation.of(new TreeMap<String, Set<String>>(), LinkedHashSet.class);
            for (String cldr : cldrVsIcu[0]) {
                XMLFileReader.loadPathValues(CldrUtility.SUPPLEMENTAL_DIRECTORY + cldr + ".xml", cldrData);
            }
            IcuValues icu = new IcuValues("misc/", cldrVsIcu[1]);
            checkValues(cldrData, cldrData, icu);
        }
    }

    // static final Pattern SKIP = Pattern.compile("^//ldml/(identity|posix/messages)|/(default|alias|commonlyUsed)$");

    static final SupplementalDataInfo supp = info.getSupplementalDataInfo();

    public void checkLocale(String locale) {
        // /Users/markdavis/Documents/workspace/icu/source/data/locales/en.txt

        CLDRFile plain = factory.make(locale, false);
        Relation<String, String> plainData = putDataIntoMap(plain, plain.iterator("", CLDRFile.ldmlComparator),
            Relation.of(new TreeMap<String, Set<String>>(), LinkedHashSet.class));
        CLDRFile resolved = factory.make(locale, true);
        Relation<String, String> resolvedData = putDataIntoMap(plain, resolved.iterator(),
            Relation.of(new TreeMap<String, Set<String>>(), LinkedHashSet.class));
        IcuValues icu = new IcuValues(locale);

        checkValues(plainData, resolvedData, icu);
    }

    private void checkValues(Relation<String, String> plainData, Relation<String, String> resolvedData, IcuValues icu) {
        XPathParts parts = new XPathParts();

        // show(supp.getDeprecationInfo());
        // Matcher skipper = SKIP.matcher("");
        Set<String> seen = new HashSet<String>();

        for (Entry<String, String> entry : plainData.entrySet()) {
            String xpath = entry.getKey();
            String value = entry.getValue();
            // Pair<ExclusionType, String> exclusionInfo = exclusions.get(xpath);
            Output<Finder> matcher = new Output<Finder>();
            Pair<ExclusionType, String> exclusionInfo = exclusions.get(xpath, null, null, matcher, null);
            ExclusionType exclusionType = null;
            if (exclusionInfo != null) {
                exclusionType = exclusionInfo.getFirst();
                if (exclusionType == ExclusionType.SKIP) {
                    continue;
                } else if (exclusionType == ExclusionType.VALUE) {
                    String[] arguments = matcher.value.getInfo();
                    value = RegexLookup.replace(exclusionInfo.getSecond(), arguments);
                }
            }
            // if (skipper.reset(xpath).find()) {
            // continue;
            // }
            boolean inICU = value.isEmpty() || icu.contains(value);
            // if (supp.hasDeprecatedItem("ldml", parts.set(xpath))) {
            // warnln("CLDR has deprecated path, with value <" + value + "> for " + xpath);
            // } else
            if (!inICU) {
                if (exclusionType == ExclusionType.WARNING) {
                    warnln("ICU missing CLDR value <" + value + "> for " + xpath);
                } else {
                    errln("ICU missing CLDR value <" + value + "> for " + xpath);
                }
            }
            seen.add(value);
        }
        if (resolvedData != plainData) {
            seen.addAll(resolvedData.values());
        }
        Relation<String, String> notSeen = icu.getAllBut(seen);
        notSeen.removeAll(CURRENCY_CODES);
        notSeen.removeAll(TIMEZONE_CODES);

        for (Entry<String, Set<String>> s : notSeen.keyValuesSet()) {
            String icuValue = s.getKey();
            Set<String> icuLocation = s.getValue();
            if (icuValue.startsWith("meta:")
                || icuValue.startsWith("set") && icuLocation.contains("misc/plurals.txt")) {
                // TODO document why this is done
                continue;
            }
            warnln("CLDR missing ICU value <" + icuValue + "> in " + icuLocation);
        }
    }

    private Relation<String, String> putDataIntoMap(CLDRFile plain, Iterator<String> it,
        Relation<String, String> plainData) {
        for (; it.hasNext();) {
            String key = it.next();
            plainData.put(key, plain.getStringValue(key));
        }
        return plainData;
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
        // others: brkitr, misc, rbnf, translit,
        static final Pattern quotedPattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = quotedPattern.matcher("");

        final Relation<String, String> values;

        static final String[] dirs = { "locales/", "lang/", "curr/", "region/", "zone/" };

        public IcuValues(String locale) {
            Relation<String, String> valuesToSource = Relation.of(new HashMap<String, Set<String>>(),
                LinkedHashSet.class);
            for (String dir : dirs) {
                addData(dir, locale, valuesToSource);
            }
            values = (Relation<String, String>) valuesToSource.freeze();
        }

        public IcuValues(String dir, String... fileBases) {
            Relation<String, String> valuesToSource = Relation.of(new HashMap<String, Set<String>>(),
                LinkedHashSet.class);
            for (String fileBase : fileBases) {
                addData(dir, fileBase, valuesToSource);
            }
            values = (Relation<String, String>) valuesToSource.freeze();
        }

        private void addData(String dir, String fileBase, Relation<String, String> valuesToSourceToAddTo) {
            try {
                String filename = fileBase + ".txt";
                BufferedReader in = BagFormatter.openUTF8Reader(CldrUtility.ICU_DATA_DIR + dir, filename);
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    if (line.contains(":alias") || line.contains("Version{")) {
                        continue;
                    }
                    matcher.reset(line);
                    while (matcher.find()) {
                        valuesToSourceToAddTo.put(matcher.group(1), dir + filename);
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e); // damn'd checked exceptions
            }
        }

        public Relation<String, String> getAllBut(Set<String> resolved) {
            Relation<String, String> result = Relation.of(new TreeMap<String, Set<String>>(), LinkedHashSet.class);
            for (Entry<String, Set<String>> entry : values.keyValuesSet()) {
                if (resolved.contains(entry.getKey())) {
                    continue;
                }
                result.putAll(entry.getKey(), entry.getValue());
            }
            return result;
        }

        public boolean contains(String value) {
            return values.containsKey(value);
        }
    }
}
