package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.Units;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;

public class TestUnits extends TestFmwk {
    CLDRConfig info = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestUnits().run(args);
    }

    public void TestSpaceInNarrowUnits() {
        final CLDRFile english = CLDRConfig.getInstance().getEnglish();
        final Matcher m = Pattern.compile("narrow.*unitPattern").matcher("");
        for (String path : english) {
            if (m.reset(path).find()) {
                String value = english.getStringValue(path);
                if (value.contains("} ")) {
                    errln(path + " fails, «" + value + "» contains } + space");
                }
            }
        }
    }

    static final String[][] COMPOUND_TESTS = {
        {"area-square-centimeter", "square", "length-centimeter"},
        {"area-square-foot", "square", "length-foot"},
        {"area-square-inch", "square", "length-inch"},
        {"area-square-kilometer", "square", "length-kilometer"},
        {"area-square-meter", "square", "length-meter"},
        {"area-square-mile", "square", "length-mile"},
        {"area-square-yard", "square", "length-yard"},
        {"digital-gigabit", "giga", "digital-bit"},
        {"digital-gigabyte", "giga", "digital-byte"},
        {"digital-kilobit", "kilo", "digital-bit"},
        {"digital-kilobyte", "kilo", "digital-byte"},
        {"digital-megabit", "mega", "digital-bit"},
        {"digital-megabyte", "mega", "digital-byte"},
        {"digital-petabyte", "peta", "digital-byte"},
        {"digital-terabit", "tera", "digital-bit"},
        {"digital-terabyte", "tera", "digital-byte"},
        {"duration-microsecond", "micro", "duration-second"},
        {"duration-millisecond", "milli", "duration-second"},
        {"duration-nanosecond", "nano", "duration-second"},
        {"electric-milliampere", "milli", "electric-ampere"},
        {"energy-kilocalorie", "kilo", "energy-calorie"},
        {"energy-kilojoule", "kilo", "energy-joule"},
        {"frequency-gigahertz", "giga", "frequency-hertz"},
        {"frequency-kilohertz", "kilo", "frequency-hertz"},
        {"frequency-megahertz", "mega", "frequency-hertz"},
        {"graphics-megapixel", "mega", "graphics-pixel"},
        {"length-centimeter", "centi", "length-meter"},
        {"length-decimeter", "deci", "length-meter"},
        {"length-kilometer", "kilo", "length-meter"},
        {"length-micrometer", "micro", "length-meter"},
        {"length-millimeter", "milli", "length-meter"},
        {"length-nanometer", "nano", "length-meter"},
        {"length-picometer", "pico", "length-meter"},
        {"mass-kilogram", "kilo", "mass-gram"},
        {"mass-microgram", "micro", "mass-gram"},
        {"mass-milligram", "milli", "mass-gram"},
        {"power-gigawatt", "giga", "power-watt"},
        {"power-kilowatt", "kilo", "power-watt"},
        {"power-megawatt", "mega", "power-watt"},
        {"power-milliwatt", "milli", "power-watt"},
        {"pressure-hectopascal", "hecto", "pressure-pascal"},
        {"pressure-millibar", "milli", "pressure-bar"},
        {"pressure-kilopascal", "kilo", "pressure-pascal"},
        {"pressure-megapascal", "mega", "pressure-pascal"},
        {"volume-centiliter", "centi", "volume-liter"},
        {"volume-cubic-centimeter", "cubic", "length-centimeter"},
        {"volume-cubic-foot", "cubic", "length-foot"},
        {"volume-cubic-inch", "cubic", "length-inch"},
        {"volume-cubic-kilometer", "cubic", "length-kilometer"},
        {"volume-cubic-meter", "cubic", "length-meter"},
        {"volume-cubic-mile", "cubic", "length-mile"},
        {"volume-cubic-yard", "cubic", "length-yard"},
        {"volume-deciliter", "deci", "volume-liter"},
        {"volume-hectoliter", "hecto", "volume-liter"},
        {"volume-megaliter", "mega", "volume-liter"},
        {"volume-milliliter", "milli", "volume-liter"},
    };

    static final String[][] PREFIX_NAME_TYPE = {
        {"deci", "10p-1"},
        {"centi", "10p-2"},
        {"milli", "10p-3"},
        {"micro", "10p-6"},
        {"nano", "10p-9"},
        {"pico", "10p-12"},
        {"femto", "10p-15"},
        {"atto", "10p-18"},
        {"zepto", "10p-21"},
        {"yocto", "10p-24"},
        {"deka", "10p1"},
        {"hecto", "10p2"},
        {"kilo", "10p3"},
        {"mega", "10p6"},
        {"giga", "10p9"},
        {"tera", "10p12"},
        {"peta", "10p15"},
        {"exa", "10p18"},
        {"zetta", "10p21"},
        {"yotta", "10p24"},
        {"square", "power2"},
        {"cubic", "power3"},
    };

    static final String PATH_UNIT_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"{1}\"]/unitPattern[@count=\"{2}\"]";

    static final String PATH_PREFIX_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/unitPrefixPattern";
    static final String PATH_SUFFIX_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/compoundUnitPattern1";

    static final String PATH_MILLI_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"10p-3\"]/unitPrefixPattern";
    static final String PATH_SQUARE_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1";


    static final String PATH_METER_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"{1}\"]";
    static final String PATH_MILLIMETER_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"length-millimeter\"]/unitPattern[@count=\"{1}\"]";
    static final String PATH_SQUARE_METER_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"area-square-meter\"]/unitPattern[@count=\"{1}\"]";    

    public void TestCompoundUnit3() {
        Factory factory = CLDRConfig.getInstance().getCldrFactory();

        Map<String,String> prefixToType = new LinkedHashMap<>();
        for (String[] prefixRow : PREFIX_NAME_TYPE) {
            prefixToType.put(prefixRow[0], prefixRow[1]);
        }
        prefixToType = ImmutableMap.copyOf(prefixToType);

        Set<String> localesToTest = ImmutableSet.of("en"); // factory.getAvailableLanguages();
        int testCount = 0;
        for (String locale : localesToTest) {
            CLDRFile file = factory.make(locale, true);
            //ExampleGenerator exampleGenerator = getExampleGenerator(locale);
            PluralInfo pluralInfo = CLDRConfig.getInstance().getSupplementalDataInfo().getPlurals(PluralType.cardinal, locale);
            final boolean isEnglish = locale.contentEquals("en");
            int errMsg = isEnglish ? ERR : WARN;

            for (String[] compoundTest : COMPOUND_TESTS) {
                String targetUnit = compoundTest[0];
                String prefix = compoundTest[1];
                String baseUnit = compoundTest[2];
                String prefixType = prefixToType.get(prefix); // will be null for square, cubic
                final boolean isPrefix = prefixType.startsWith("1");

                for (String len : Arrays.asList("long", "short", "narrow")) {
                    String prefixPath = ExampleGenerator.format(isPrefix ? PATH_PREFIX_PATTERN 
                        : PATH_SUFFIX_PATTERN,
                        len, prefixType);
                    String prefixValue = file.getStringValue(prefixPath);
                    boolean lowercaseIfSpaced = len.equals("long");

                    for (Count count : pluralInfo.getCounts()) {
                        final String countString = count.toString();
                        String targetUnitPath = ExampleGenerator.format(PATH_UNIT_PATTERN, len, targetUnit, countString);
                        String targetUnitPattern = file.getStringValue(targetUnitPath);

                        String baseUnitPath = ExampleGenerator.format(PATH_UNIT_PATTERN, len, baseUnit, countString);
                        String baseUnitPattern = file.getStringValue(baseUnitPath);

                        String composedTargetUnitPattern = Units.combinePattern(baseUnitPattern, prefixValue, lowercaseIfSpaced);
                        if (isEnglish && !targetUnitPattern.equals(composedTargetUnitPattern)) {
                            if (allowEnglishException(targetUnitPattern, composedTargetUnitPattern)) {
                                continue;
                            }
                        }
                        if (!assertEquals2(errMsg, testCount++ + ") " 
                            + locale + "/" + len + "/" + count + "/" + prefix + "+" + baseUnit 
                            + ": constructed pattern",
                            targetUnitPattern, 
                            composedTargetUnitPattern)) {
                            Units.combinePattern(baseUnitPattern, prefixValue, lowercaseIfSpaced);
                            int debug = 0;
                        };
                    }
                }
            }
        }
    }

    /**
     * Curated list of known exceptions. Usually because the short form of a unit is shorter when combined with a prefix or suffix
     */
    static final Map<String,String> ALLOW_ENGLISH_EXCEPTION = ImmutableMap.<String,String>builder()
        .put("sq ft", "ft²")
        .put("sq mi", "mi²")
        .put("ft", "′")
        .put("in", "″")
        .put("MP", "Mpx")
        .put("b", "bit")
        .put("mb", "mbar")
        .put("B", "byte")
        .put("s", "sec")
        .build();
    private boolean allowEnglishException(String targetUnitPattern, String composedTargetUnitPattern) {
        for (Entry<String, String> entry : ALLOW_ENGLISH_EXCEPTION.entrySet()) {
            String mod = targetUnitPattern.replace(entry.getKey(), entry.getValue());
            if (mod.contentEquals(composedTargetUnitPattern)) {
                return true;
            }
        }
        return false;
    }

    // TODO Work this into a generating and then maintaining a data table for the units
    /*
        CLDRFile english = factory.make("en", false);
        Set<String> prefixes = new TreeSet<>();
        for (String path : english) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String lastElement = parts.getElement(-1);
            if (lastElement.equals("unitPrefixPattern") || lastElement.equals("compoundUnitPattern1")) {
                if (!parts.getAttributeValue(2, "type").equals("long")) {
                    continue;
                }
                String value = english.getStringValue(path);
                prefixes.add(value.replace("{0}", "").trim());
            }
        }        
        Map<Status, Set<String>> unitValidity = Validity.getInstance().getStatusToCodes(LstrType.unit);
        Multimap<String, String> from = LinkedHashMultimap.create();
        for (String unit : unitValidity.get(Status.regular)) {
            String[] parts = unit.split("[-]");
            String main = parts[1];
            for (String prefix : prefixes) {
                if (main.startsWith(prefix)) {
                    if (main.length() == prefix.length()) { // square,...
                        from.put(unit, main);
                    } else { // milli
                        from.put(unit, main.substring(0,prefix.length()));
                        from.put(unit, main.substring(prefix.length()));
                    }
                    for (int i = 2; i < parts.length; ++i) {
                        from.put(unit, parts[i]);
                    }
                }
            }
        }
        for (Entry<String, Collection<String>> set : from.asMap().entrySet()) {
            System.out.println(set.getKey() + "\t" + CollectionUtilities.join(set.getValue(), "\t"));
        }
     */
    private boolean assertEquals2(int TestERR, String title, String sqmeterPattern, String conSqmeterPattern) {
        if (!Objects.equals(sqmeterPattern, conSqmeterPattern)) {
            msg(title + ", expected «" + sqmeterPattern + "», got «" + conSqmeterPattern + "»", TestERR, true, true);
            return false;
        } else if (isVerbose()) {
            msg(title + ", expected «" + sqmeterPattern + "», got «" + conSqmeterPattern + "»", LOG, true, true);
        }
        return true;
    }
}
