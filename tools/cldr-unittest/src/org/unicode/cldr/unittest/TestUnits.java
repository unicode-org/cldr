package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.ContinuedFraction;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.Rational.RationalParser;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.Continuation;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitPreferences;
import org.unicode.cldr.util.UnitPreferences.UnitPreference;
import org.unicode.cldr.util.Units;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.XMLSource;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;

public class TestUnits extends TestFmwk {
    private static final boolean SHOW_DATA = CldrUtility.getProperty("TestUnits:SHOW_DATA", false); // set for verbose debugging information
    private static final boolean GENERATE_TESTS = CldrUtility.getProperty("TestUnits:GENERATE_TESTS", false);

    private static final String TEST_SEP = ";\t";

    private static final ImmutableSet<String> WORLD_SET = ImmutableSet.of("001");
    private static final CLDRConfig info = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = info.getSupplementalDataInfo();

    static final UnitConverter converter = SDI.getUnitConverter();
    static final Splitter SPLIT_SEMI = Splitter.on(Pattern.compile("\\s*;\\s*")).trimResults();
    static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    static final Splitter SPLIT_AND = Splitter.on("-and-").trimResults().omitEmptyStrings();

    static final Rational R1000 = Rational.of(1000);

    public static void main(String[] args) {
        new TestUnits().run(args);
    }

    private Map<String, String> BASE_UNIT_TO_QUANTITY = converter.getBaseUnitToQuantity();

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
            PluralInfo pluralInfo = SDI.getPlurals(PluralType.cardinal, locale);
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

    static final boolean DEBUG = false;

    public void TestConversion() {
        UnitConverter converter = SDI.getUnitConverter();
        Object[][] tests = {
            {"foot", 12, "inch"},
            {"gallon", 4, "quart"},
            {"gallon", 16, "cup"},
        };
        for (Object[] test : tests) {
            String sourceUnit = test[0].toString();
            String targetUnit = test[2].toString();
            int numerator = (Integer) test[1];
            final Rational convert = converter.convertDirect(Rational.ONE, sourceUnit, targetUnit);
            assertEquals(sourceUnit + " to " + targetUnit, Rational.of(numerator, 1), convert);
        }

        // test conversions are disjoint
        Set<String> gotAlready = new HashSet<>();
        List<Set<String>> equivClasses = new ArrayList<>();
        Map<String,String> classToId = new TreeMap<>();
        for (String unit : converter.canConvert()) {
            if (gotAlready.contains(unit)) {
                continue;
            }
            Set<String> set = converter.canConvertBetween(unit);
            final String id = "ID" + equivClasses.size();
            equivClasses.add(set);
            gotAlready.addAll(set);
            for (String s : set) {
                classToId.put(s, id);
            }
        }

        // check not overlapping
        // now handled by TestParseUnit, but we might revive a modified version of this.
//        for (int i = 0; i < equivClasses.size(); ++i) {
//            Set<String> eclass1 = equivClasses.get(i);
//            for (int j = i+1; j < equivClasses.size(); ++j) {
//                Set<String> eclass2 = equivClasses.get(j);
//                if (!Collections.disjoint(eclass1, eclass2)) {
//                    errln("Overlapping equivalence classes: " + eclass1 + " ~ " + eclass2 + "\n\tProbably bad chain requiring 3 steps.");
//                }
//            }
//
//            // check that all elements of an equivalence class have the same type
//            Multimap<String,String> breakdown = TreeMultimap.create();
//            for (String item : eclass1) {
//                String type = CORE_TO_TYPE.get(item);
//                if (type == null) {
//                    type = "?";
//                }
//                breakdown.put(type, item);
//            }
//            if (DEBUG) System.out.println("type to item: " + breakdown);
//            if (breakdown.keySet().size() != 1) {
//                errln("mixed categories: " + breakdown);
//            }
//
//        }
//
//        // check that all units with the same type have the same equivalence class
//        for (Entry<String, Collection<String>> entry : TYPE_TO_CORE.asMap().entrySet()) {
//            Multimap<String,String> breakdown = TreeMultimap.create();
//            for (String item : entry.getValue()) {
//                String id = classToId.get(item);
//                if (id == null) {
//                    continue;
//                }
//                breakdown.put(id, item);
//            }
//            if (DEBUG) System.out.println(entry.getKey() + " id to item: " + breakdown);
//            if (breakdown.keySet().size() != 1) {
//                errln(entry.getKey() + " mixed categories: " + breakdown);
//            }
//        }
    }

    public void TestBaseUnits() {
        Splitter barSplitter = Splitter.on('-');
        for (String unit : converter.baseUnits()) {
            for (String piece : barSplitter.split(unit)) {
                assertTrue(unit + ": " + piece + " in " + UnitConverter.BASE_UNIT_PARTS, UnitConverter.BASE_UNIT_PARTS.contains(piece));
            }
        }
    }

    public void TestUnitId() {

        for (String simple : converter.getSimpleUnits()) {
            String canonicalUnit = converter.getBaseUnit(simple);
            UnitId unitId = converter.createUnitId(canonicalUnit);
            String output = unitId.toString();
            if (!assertEquals(simple + ": targets should be in canonical form", 
                output, canonicalUnit)) {
                // for debugging
                converter.createUnitId(canonicalUnit);
                unitId.toString();
            }
        }
        for (Entry<String, String> baseUnitToQuantity : BASE_UNIT_TO_QUANTITY.entrySet()) {
            String baseUnit = baseUnitToQuantity.getKey();
            String quantity = baseUnitToQuantity.getValue();
            try {
                UnitId unitId = converter.createUnitId(baseUnit);
                String output = unitId.toString();
                if (!assertEquals(quantity + ": targets should be in canonical form", 
                    output, baseUnit)) {
                    // for debugging
                    converter.createUnitId(baseUnit);
                    unitId.toString();
                }
            } catch (Exception e) {
                errln("Can't convert baseUnit: " + baseUnit);
            }
        }

        for (String baseUnit : CORE_TO_TYPE.keySet()) {
            try {
                UnitId unitId = converter.createUnitId(baseUnit);
                assertNotNull("Can't parse baseUnit: " + baseUnit, unitId);
            } catch (Exception e) {
                UnitId unitId = converter.createUnitId(baseUnit); // for debugging
                errln("Can't parse baseUnit: " + baseUnit);
            }
        }

    }

    public void TestParseUnit() {
        Output<String> compoundBaseUnit = new Output<>();
        String[][] tests = {
            {"kilometer-pound-per-hour", "kilogram-meter-per-second", "45359237/360000000"},
            {"kilometer-per-hour", "meter-per-second", "5/18"},
        };
        for (String[] test : tests) {
            String source = test[0];
            String expectedUnit = test[1];
            Rational expectedRational = new Rational.RationalParser().parse(test[2]);
            ConversionInfo unitInfo = converter.parseUnitId(source, compoundBaseUnit, false);
            assertEquals(source, expectedUnit, compoundBaseUnit.value);
            assertEquals(source, expectedRational, unitInfo.factor);
        }

        // check all 
        if (GENERATE_TESTS) System.out.println();
        Set<String> badUnits = new LinkedHashSet<>();
        Set<String> noQuantity = new LinkedHashSet<>();
        Multimap<Pair<String,Double>, String> testPrintout = TreeMultimap.create();

        // checkUnitConvertability(converter, compoundBaseUnit, badUnits, "pint-metric-per-second");

        for (Entry<String, String> entry : TYPE_TO_CORE.entries()) {
            String type = entry.getKey();
            String unit = entry.getValue();
            if (NOT_CONVERTABLE.contains(unit)) {
                continue;
            }
            checkUnitConvertability(converter, compoundBaseUnit, badUnits, noQuantity, type, unit, testPrintout);
        }
        if (GENERATE_TESTS) { // test data
            System.out.println(
                "# Test data for unit conversions\n" 
                    + CldrUtility.getCopyrightString("#  ") + "\n"
                    + "#\n" 
                    + "# Format:\n"
                    + "#\tQuantity\t;\tx\t;\ty\t;\tconversion to y (rational)\t;\ttest: 1000 x ⟹ y\n"
                    + "#\n"
                    + "# Use: convert 1000 x units to the y unit; the result should match the final column,\n"
                    + "#   at the given precision. For example, when the last column is 159.1549,\n"
                    + "#   round to 4 decimal digits before comparing.\n"
                    + "# Note that certain conversions are approximate, such as degrees to radians\n"
                    + "#\n"
                    + "# Generation: Set GENERATE_TESTS in TestUnits.java, and look at TestParseUnit results.\n"
                );
            for (Entry<Pair<String, Double>, String> entry : testPrintout.entries()) {
                System.out.println(entry.getValue());
            }
        }
        assertEquals("Unconvertable units", Collections.emptySet(), badUnits);
        assertEquals("Units without Quantity", Collections.emptySet(), noQuantity);
    }

    static final Set<String> NOT_CONVERTABLE = ImmutableSet.of("generic");

    private void checkUnitConvertability(UnitConverter converter, Output<String> compoundBaseUnit, 
        Set<String> badUnits, Set<String> noQuantity, String type, String unit, 
        Multimap<Pair<String, Double>, String> testPrintout) {

        if (converter.isBaseUnit(unit)) {
            String quantity = converter.getQuantityFromBaseUnit(unit);
            if (quantity == null) {
                noQuantity.add(unit);
            }
            if (GENERATE_TESTS) {
                testPrintout.put(
                    new Pair<>(quantity, 1000d),
                    quantity
                    + "\t;\t" + unit
                    + "\t;\t" + unit
                    + "\t;\t1 * x\t;\t1,000.00");
            }
        } else {
            ConversionInfo unitInfo = converter.getUnitInfo(unit, compoundBaseUnit);
            if (unitInfo == null) {
                unitInfo = converter.parseUnitId(unit, compoundBaseUnit, false);
            }
            if (unitInfo == null) {
                badUnits.add(unit);
            } else if (GENERATE_TESTS){
                String quantity = converter.getQuantityFromBaseUnit(compoundBaseUnit.value);
                if (quantity == null) {
                    noQuantity.add(compoundBaseUnit.value);
                }
                final double testValue = unitInfo.convert(R1000).toBigDecimal(MathContext.DECIMAL32).doubleValue();
                testPrintout.put(
                    new Pair<>(quantity, testValue),
                    quantity
                    + "\t;\t" + unit
                    + "\t;\t" + compoundBaseUnit
                    + "\t;\t" + unitInfo
                    + "\t;\t" + testValue
//                    + "\t" + unitInfo.factor.toBigDecimal(MathContext.DECIMAL32)
//                    + "\t" + unitInfo.factor.reciprocal().toBigDecimal(MathContext.DECIMAL32)
                    );
            }
        }
    }

    public void TestRational() {
        Rational a3_5 = Rational.of(3,5);

        Rational a6_10 = Rational.of(6,10);
        assertEquals("", a3_5, a6_10);

        Rational a5_3 = Rational.of(5,3);
        assertEquals("", a3_5, a5_3.reciprocal());

        assertEquals("", Rational.ONE, a3_5.multiply(a3_5.reciprocal()));
        assertEquals("", Rational.ZERO, a3_5.add(a3_5.negate()));

        assertEquals("", Rational.INFINITY, Rational.ZERO.reciprocal());
        assertEquals("", Rational.NEGATIVE_INFINITY, Rational.INFINITY.negate());
        assertEquals("", Rational.NEGATIVE_ONE, Rational.ONE.negate());

        assertEquals("", Rational.NaN, Rational.ZERO.divide(Rational.ZERO));

        assertEquals("", BigDecimal.valueOf(2), Rational.of(2,1).toBigDecimal());
        assertEquals("", BigDecimal.valueOf(0.5), Rational.of(1,2).toBigDecimal());

        assertEquals("", BigDecimal.valueOf(100), Rational.of(100,1).toBigDecimal());
        assertEquals("", BigDecimal.valueOf(0.01), Rational.of(1,100).toBigDecimal());

        assertEquals("", Rational.of(12370,1), Rational.of(BigDecimal.valueOf(12370)));
        assertEquals("", Rational.of(1237,10), Rational.of(BigDecimal.valueOf(1237.0/10)));
        assertEquals("", Rational.of(1237,10000), Rational.of(BigDecimal.valueOf(1237.0/10000)));

        ConversionInfo uinfo = new ConversionInfo(Rational.of(2), Rational.of(3));
        assertEquals("", Rational.of(3), uinfo.convert(Rational.ZERO));
        assertEquals("", Rational.of(7), uinfo.convert(Rational.of(2)));
    }

    public void TestRationalParse() {
        Rational.RationalParser parser = SDI.getRationalParser();

        Rational a3_5 = Rational.of(3,5);

        assertEquals("", a3_5, parser.parse("6/10"));

        assertEquals("", a3_5, parser.parse("0.06/0.10"));

        assertEquals("", Rational.of(381, 1250), parser.parse("ft_to_m"));
        assertEquals("", 6.02214076E+23d, parser.parse("6.02214076E+23").toBigDecimal().doubleValue());
        Rational temp = parser.parse("gal_to_m3");
        //System.out.println(" " + temp);
        assertEquals("", 0.003785411784, temp.numerator.doubleValue()/temp.denominator.doubleValue());        
    }


    static final Map<String,String> CORE_TO_TYPE;
    static final Multimap<String,String> TYPE_TO_CORE;
    static {
        Set<String> VALID_UNITS = Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular);

        Map<String, String> coreToType = new TreeMap<>();
        TreeMultimap<String, String> typeToCore = TreeMultimap.create();
        for (String s : VALID_UNITS) {
            int dashPos = s.indexOf('-');
            String unitType = s.substring(0,dashPos);
            String coreUnit = s.substring(dashPos+1);
            coreUnit = converter.fixDenormalized(coreUnit);
            coreToType.put(coreUnit, unitType);
            typeToCore.put(unitType, coreUnit);
        }
        CORE_TO_TYPE = ImmutableMap.copyOf(coreToType);
        TYPE_TO_CORE = ImmutableMultimap.copyOf(typeToCore);
    }

    public void TestUnitCategory() {
        if (SHOW_DATA) System.out.println();

        Map<String,Multimap<String,String>> bad = new TreeMap<>();
        for (Entry<String, String> entry : TYPE_TO_CORE.entries()) {
            final String coreUnit = entry.getValue();
            final String unitType = entry.getKey();
            if (coreUnit.equals("generic")) {
                continue;
            }
            String quantity = converter.getQuantityFromUnit(coreUnit, false);
            if (SHOW_DATA) {
                System.out.format("%s\t%s\t%s\n", coreUnit, quantity, unitType);
            }
            if (quantity == null) {
                converter.getQuantityFromUnit(coreUnit, true);
                errln("Null quantity " + coreUnit);
            } else if (!unitType.equals(quantity)) {
                switch (unitType) {
                case "concentr": 
                    switch (quantity) {
                    case "portion": case "mass-density": case "concentration": case "substance-amount": continue;
                    }
                    break;
                case "consumption":
                    switch (quantity) {
                    case "consumption-inverse": continue;
                    }
                    break;
                case "duration": 
                    switch (quantity) {
                    case "year-duration": continue;
                    }
                    break;
                case "electric":
                    switch (quantity) {
                    case "electric-current": case "electric-resistance": case "voltage": continue;
                    }
                    break;
                case "graphics":
                    switch (quantity) {
                    case "resolution": case "typewidth": continue;
                    }
                    break;
                case "light":
                    switch (quantity) {
                    case "luminous-flux": case "power": case "luminous-intensity": continue;
                    }
                    break;
                case "mass":
                    switch (quantity) {
                    case "energy": continue;
                    }
                    break;
                case "torque":
                    switch (quantity) {
                    case "energy": continue;
                    }
                    break;
                case "pressure":
                    switch (quantity) {
                    case "pressure-per-length": continue;
                    }
                    break;
                }
                Multimap<String, String> badMap = bad.get(unitType);
                if (badMap == null) {
                    bad.put(unitType, badMap = TreeMultimap.create());
                }
                badMap.put(quantity, coreUnit);
            }
        }
        for (Entry<String, Multimap<String, String>> entry : bad.entrySet()) {
            assertNull("UnitType != quantity: " + entry.getKey(), '"' + Joiner.on("\", \"").join(entry.getValue().asMap().entrySet()) + '"');
        }
    }

    public void TestQuantities() {
        // put quantities in order
        Multimap<String,String> quantityToBaseUnits = LinkedHashMultimap.create();

        Multimaps.invertFrom(Multimaps.forMap(BASE_UNIT_TO_QUANTITY), quantityToBaseUnits);

        for ( Entry<String, Collection<String>> entry : quantityToBaseUnits.asMap().entrySet()) {
            assertEquals(entry.toString(), 1, entry.getValue().size());
        }

        TreeMultimap<String, String> quantityToConvertible = TreeMultimap.create();
        Set<String> missing = new TreeSet<>(CORE_TO_TYPE.keySet());
        missing.removeAll(NOT_CONVERTABLE);

        for (Entry<String, String> entry : BASE_UNIT_TO_QUANTITY.entrySet()) {
            String baseUnit = entry.getKey();
            String quantity = entry.getValue();
            Set<String> convertible = converter.canConvertBetween(baseUnit);
            missing.removeAll(convertible);
            quantityToConvertible.putAll(quantity, convertible);
        }

        // handle missing
        for (String missingUnit : ImmutableSet.copyOf(missing)) {
            if (missingUnit.equals("mile-per-gallon")) {
                int debug = 0;
            }
            String quantity = converter.getQuantityFromUnit(missingUnit, false);
            if (quantity != null) {
                quantityToConvertible.put(quantity, missingUnit);
                missing.remove(missingUnit);
            } else {
                quantity = converter.getQuantityFromUnit(missingUnit, true); // for debugging
            }
        }
        assertEquals("all units have quantity", Collections.emptySet(), missing);

        if (SHOW_DATA) {
            System.out.println();
            for (Entry<String, String> entry : BASE_UNIT_TO_QUANTITY.entrySet()) {
                String baseUnit = entry.getKey();
                String quantity = entry.getValue();
                System.out.println("        <unitQuantity"
                    + " baseUnit='" + baseUnit + "'"
                    + " quantity='" + quantity + "'"
                    + "/>");
            }
            System.out.println();
            System.out.println("Quantities");
            for (Entry<String, Collection<String>> entry : quantityToConvertible.asMap().entrySet()) {
                String quantity = entry.getKey();
                Collection<String> convertible = entry.getValue();
                System.out.println(quantity + "\t" + convertible);
            }
        }
    }

    static final UnicodeSet ALLOWED_IN_COMPONENT = new UnicodeSet("[a-z0-9]").freeze();
    static final Set<String> GRANDFATHERED_SIMPLES = ImmutableSet.of("em", "g-force", "therm-us");

    public void TestOrder() {
        if (SHOW_DATA) System.out.println();
        for (String s : UnitConverter.BASE_UNITS) {
            String quantity = converter.getQuantityFromBaseUnit(s);
            if (SHOW_DATA) {
                System.out.println("\"" + quantity + "\",");
            }
        }
        for (String unit : CORE_TO_TYPE.keySet()) {
            if (!GRANDFATHERED_SIMPLES.contains(unit)) {
                for (String part : unit.split("-")) {
                    assertTrue(unit + " has no parts < 2 in length", part.length() > 2);
                    assertTrue(unit + " has only allowed characters", ALLOWED_IN_COMPONENT.containsAll(part));
                }
            }
            if (unit.equals("generic")) {
                continue;
            }
            String quantity = converter.getQuantityFromUnit(unit, false); // make sure doesn't crash
        }
    }

    public void TestConversionLineOrder() {
        Map<String, TargetInfo> data = converter.getInternalConversionData();
        Multimap<TargetInfo, String> sorted = TreeMultimap.create(converter.targetInfoComparator, 
            Comparator.naturalOrder());
        Multimaps.invertFrom(Multimaps.forMap(data), sorted);

        String lastBase = "";

        // Test that sorted is in same order as the file.
        MapComparator<String> conversionOrder = new MapComparator<>(data.keySet());
        String lastUnit = null;
        for (Entry<TargetInfo, String> entry : sorted.entries()) {
            final TargetInfo tInfo = entry.getKey();
            final String unit = entry.getValue();
            if (lastUnit != null) {
                assertTrue(lastUnit + " < " + unit, conversionOrder.compare(lastUnit, unit) < 0);
            }
            lastUnit = unit;
            if (SHOW_DATA) {
                if (!lastBase.equals(tInfo.target)) {
                    lastBase = tInfo.target;
                    System.out.println("\n      <!-- " + converter.getQuantityFromBaseUnit(lastBase) + " -->");
                }
                //  <convertUnit source='week-person' target='second' factor='604800'/>
                System.out.println("        " + tInfo.formatOriginalSource(entry.getValue()));
            }
        }
    }

    public final void TestSimplify() {
        Set<Rational> seen = new HashSet<>();
        checkSimplify("ZERO", Rational.ZERO, seen);
        checkSimplify("ONE", Rational.ONE, seen);
        checkSimplify("NEGATIVE_ONE", Rational.NEGATIVE_ONE, seen);
        checkSimplify("INFINITY", Rational.INFINITY, seen);
        checkSimplify("NEGATIVE_INFINITY", Rational.NEGATIVE_INFINITY, seen);
        checkSimplify("NaN", Rational.NaN, seen);

        checkSimplify("Simplify", Rational.of(25, 300), seen);
        checkSimplify("Simplify", Rational.of(100, 1), seen);
        checkSimplify("Simplify", Rational.of(2, 5), seen);
        checkSimplify("Simplify", Rational.of(4, 25), seen);
        checkSimplify("Simplify", Rational.of(5, 2), seen);
        checkSimplify("Simplify", Rational.of(25, 4), seen);

        for (Entry<String, TargetInfo> entry : converter.getInternalConversionData().entrySet()) {
            final Rational factor = entry.getValue().unitInfo.factor;
            checkSimplify(entry.getKey(), factor, seen);
            if (!factor.equals(Rational.ONE)) {
                checkSimplify(entry.getKey(), factor, seen);
            }
            final Rational offset = entry.getValue().unitInfo.offset;
            if (!offset.equals(Rational.ZERO)) {
                checkSimplify(entry.getKey(), offset, seen);
            }
        }
    }

    private void checkSimplify(String title, Rational expected, Set<Rational> seen) {
        if (!seen.contains(expected)) {
            seen.add(expected);
            String simpleStr = expected.toString(FormatStyle.simple);
            if (SHOW_DATA) System.out.println(title + ": " + expected + " => " + simpleStr);
            Rational actual = RationalParser.BASIC.parse(simpleStr);
            assertEquals("simplify", expected, actual);
        }
    }

    public void TestContinuationOrder() {
        Continuation fluid = new Continuation(Arrays.asList("fluid"), "fluid-ounce");
        Continuation fluid_imperial = new Continuation(Arrays.asList("fluid", "imperial"), "fluid-ounce-imperial");
        final int fvfl = fluid.compareTo(fluid_imperial);
        assertTrue(fluid + " vs " + fluid_imperial, fvfl > 0);
        assertTrue(fluid_imperial + " vs " + fluid, fluid_imperial.compareTo(fluid) < 0);
    }

    static final Pattern usSystemPattern = Pattern.compile("\\b(lb_to_kg|ft_to_m|ft2_to_m2|ft3_to_m3|in3_to_m3|gal_to_m3|cup_to_m3)\\b");
    static final Pattern ukSystemPattern = Pattern.compile("\\b(lb_to_kg|ft_to_m|ft2_to_m2|ft3_to_m3|in3_to_m3|gal_imp_to_m3)\\b");

    static final Set<String> OK_BOTH = ImmutableSet.of(
        "ounce-troy", "nautical-mile", "fahrenheit", "inch-ofhg", 
        "british-thermal-unit", "foodcalorie", "knot");

    static final Set<String> OK_US = ImmutableSet.of(
        "therm-us", "bushel");
    static final Set<String> NOT_US = ImmutableSet.of(
        "stone");

    static final Set<String> OK_UK = ImmutableSet.of();
    static final Set<String> NOT_UK = ImmutableSet.of(
        "therm-us", "bushel", "barrel");

    public void TestSystems() {
        Multimap<String, String> toSystems = converter.getSourceToSystems();

        Map<String, TargetInfo> data = converter.getInternalConversionData();
        for (Entry<String, TargetInfo> entry : data.entrySet()) {
            String unit = entry.getKey();
            TargetInfo value = entry.getValue();
            String inputFactor = value.inputParameters.get("factor");
            if (inputFactor == null) {
                inputFactor = "";
            }
            boolean usSystem = !NOT_US.contains(unit) && 
                (OK_BOTH.contains(unit)
                    || OK_US.contains(unit) 
                    || usSystemPattern.matcher(inputFactor).find());

            boolean ukSystem = !NOT_UK.contains(unit) && 
                (OK_BOTH.contains(unit)
                    || OK_UK.contains(unit) 
                    || ukSystemPattern.matcher(inputFactor).find());

            Collection<String> systems = toSystems.get(unit);
            if (systems == null) {
                systems = Collections.emptySet();
            }
            if (!assertEquals(unit + ": US? (" + inputFactor + ")", usSystem, systems.contains("ussystem"))) {
                int debug = 0;
            }
            if (!assertEquals(unit + ": UK? (" + inputFactor + ")", ukSystem, systems.contains("uksystem"))) {
                int debug = 0;
            }
        }
    }

    public void TestTestFile() {
        File base = info.getCldrBaseDirectory();
        File testFile = new File(base, "common/testData/units/unitsTest.txt");
        Output<String> metricUnit = new Output<>();
        Stream<String> lines;
        try {
            lines = Files.lines(testFile.toPath()); 
        } catch (IOException e) {
            throw new ICUUncheckedIOException("Couldn't process " + testFile);
        }
        lines.forEach(line -> {
            // angle   ;   arc-second  ;   revolution  ;   1 / 1296000 * x ;   7.716049E-4
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '#') {
                return;
            }
            List<String> fields = SPLIT_SEMI.splitToList(line);
            ConversionInfo unitInfo;
            try {
                unitInfo = converter.parseUnitId(fields.get(1), metricUnit, false);
            } catch (Exception e1) {
                throw new IllegalArgumentException("Couldn't access fields on " + line);
            }
            if (unitInfo == null) {
                throw new IllegalArgumentException("Couldn't get unitInfo on " + line);
            }
            double expected;
            try {
                expected = Double.parseDouble(fields.get(4).replace(",", ""));
            } catch (NumberFormatException e) {
                errln("Can't parse double in: " + line);
                return;
            }
            double actual = unitInfo.convert(R1000).toBigDecimal(MathContext.DECIMAL32).doubleValue();
            assertEquals(Joiner.on(" ; ").join(fields), expected, actual);
        });
        lines.close();
    }

    public void TestSpecialCases() {
        String [][] tests = {
            {"50",  "foot", "xxx", "0/0"},
            {"50",  "xxx", "mile", "0/0"},
            {"50",  "foot", "second", "0/0"},
            {"50",  "foot-per-xxx", "mile-per-hour", "0/0"},
            {"50",  "foot-per-minute", "mile", "0/0"},
            {"50",  "foot-per-ampere", "mile-per-hour", "0/0"},

            {"50",  "foot", "mile", "5 / 528"},
            {"50",  "foot-per-minute", "mile-per-hour", "25 / 44"},
            {"50",  "foot-per-minute", "hour-per-mile", "44 / 25"},
            {"50",  "mile-per-gallon", "liter-per-100-kilometer", "112903 / 24000"},
            {"50",  "celsius-per-second", "kelvin-per-second", "50"},
            {"50",  "celsius-per-second", "fahrenheit-per-second", "90"},
            {"50",  "pound-force", "kilogram-meter-per-square-second", "8896443230521 / 40000000000"},
            // Note: pound-foot-per-square-second is a pound-force divided by gravity
            {"50",  "pound-foot-per-square-second", "kilogram-meter-per-square-second", "17281869297 / 2500000000"},
        };
        int count = 0;
        for (String[] test : tests) {
            final Rational sourceValue = Rational.of(test[0]);
            final String sourceUnit = test[1]; 
            final String targetUnit = test[2];
            final Rational expectedValue = Rational.of(test[3]);
            final Rational conversion = converter.convert(sourceValue, sourceUnit, targetUnit, SHOW_DATA);
            if (!assertEquals(count++ + ") " + sourceValue + " " + sourceUnit + " ⟹ " + targetUnit, expectedValue, conversion)) {
                converter.convert(sourceValue, sourceUnit, targetUnit, SHOW_DATA);
            }
        }
    }

    static Multimap<String,String> EXTRA_UNITS = ImmutableMultimap.<String,String>builder()
        .putAll("area", "square-foot", "square-yard", "square-mile")
        .putAll("volume", "cubic-inch", "cubic-foot", "cubic-yard")
        .build();

    public void TestEnglishSystems() {
        Multimap<String, String> systemToUnits = TreeMultimap.create();
        for (String unit : converter.canConvert()) {
            Set<String> systems = converter.getSystems(unit);
            if (systems.isEmpty()) {
                systemToUnits.put("other", unit);
            } else for (String s : systems) {
                systemToUnits.put(s, unit);
            }
        }
        for (Entry<String, Collection<String>> systemAndUnits : systemToUnits.asMap().entrySet()) {
            String system = systemAndUnits.getKey();
            final Collection<String> units = systemAndUnits.getValue();
            printSystemUnits(system, units);
        }
    }

    private void printSystemUnits(String system, Collection<String> units) {
        Multimap<String,String> quantityToUnits = TreeMultimap.create();
        boolean metric = system.equals("metric");
        for (String unit : units) {
            quantityToUnits.put(converter.getQuantityFromUnit(unit, false), unit);
        }
        for (Entry<String, Collection<String>> entry : quantityToUnits.asMap().entrySet()) {
            String quantity = entry.getKey();
            String baseUnit = converter.getBaseUnitToQuantity().inverse().get(quantity);
            Multimap<Rational,String> sorted = TreeMultimap.create();
            sorted.put(Rational.ONE, baseUnit);
            if (!metric) {
                String englishBaseUnit = getEnglishBaseUnit(baseUnit);
                addUnit(baseUnit, englishBaseUnit, sorted);
                Collection<String> extras = EXTRA_UNITS.get(quantity);
                if (extras != null) {
                    for (String unit2 : extras) {
                        addUnit(baseUnit, unit2, sorted);
                    }
                }
            }
            for (String unit : entry.getValue()) {
                addUnit(baseUnit, unit, sorted);
            }
            Set<String> comparableUnits = ImmutableSet.copyOf(sorted.values());

            printUnits(system, quantity, comparableUnits);
        }
    }

    private void addUnit(String baseUnit, String englishBaseUnit, Multimap<Rational, String> sorted) {
        Rational value = converter.convert(Rational.ONE, englishBaseUnit, baseUnit, false);
        sorted.put(value, englishBaseUnit);
    }

    private void printUnits(String system, String quantity, Set<String> comparableUnits) {
        if (SHOW_DATA) System.out.print("\n"+ system + "\t" + quantity);
        for (String targetUnit : comparableUnits) {
            if (SHOW_DATA) System.out.print("\t" + targetUnit);
        }
        if (SHOW_DATA) System.out.println();
        for (String sourceUnit : comparableUnits) {
            if (SHOW_DATA) System.out.print("\t" + sourceUnit);
            for (String targetUnit : comparableUnits) {
                Rational rational = converter.convert(Rational.ONE, sourceUnit, targetUnit, false);
                if (SHOW_DATA) System.out.print("\t" + rational.toBigDecimal(MathContext.DECIMAL64).doubleValue());
            }               
            if (SHOW_DATA) System.out.println();
        }
    }

    private String getEnglishBaseUnit(String baseUnit) {
        return baseUnit.replace("kilogram", "pound").replace("meter", "foot");
    }

    public void TestPI() {
        Rational PI = converter.getConstants().get("PI");
        double PID = PI.toBigDecimal(MathContext.DECIMAL128).doubleValue();
        final BigDecimal bigPi = new BigDecimal("3.141592653589793238462643383279502884197169399375105820974944");
        double bigPiD = bigPi.doubleValue();
        assertEquals("pi accurate enough", bigPiD, PID);

        // also test continued fractions used in deriving values

        Object[][] tests0 = {
            {new ContinuedFraction(0, 1, 5, 2, 2), Rational.of(27, 32), ImmutableList.of(Rational.of(0), Rational.of(1), Rational.of(5,6), Rational.of(11, 13))},
        };
        for (Object[] test : tests0) {
            ContinuedFraction source = (ContinuedFraction) test[0];
            Rational expected = (Rational) test[1];
            List<Rational> expectedIntermediates = (List<Rational>) test[2];
            List<Rational> intermediates = new ArrayList<>();
            final Rational actual = source.toRational(intermediates);            
            assertEquals("continued", expected, actual);
            assertEquals("continued", expectedIntermediates, intermediates);
        }
        Object[][] tests = {
            {Rational.of(3245,1000), new ContinuedFraction(3, 4, 12, 4)},
            {Rational.of(39,10), new ContinuedFraction(3, 1, 9)},
            {Rational.of(-3245,1000), new ContinuedFraction(-4, 1, 3, 12, 4)},
        };
        for (Object[] test : tests) {
            Rational source = (Rational) test[0];
            ContinuedFraction expected =(ContinuedFraction) test[1];
            ContinuedFraction actual = new ContinuedFraction(source);
            assertEquals(source.toString(), expected, actual);
            assertEquals(actual.toString(), source, actual.toRational(null));
        }


        if (SHOW_DATA) {
            ContinuedFraction actual = new ContinuedFraction(Rational.of(bigPi));
            List<Rational> intermediates = new ArrayList<>();
            actual.toRational(intermediates);
            System.out.println("\nRational\tdec64\tdec128\tgood enough");
            System.out.println("Target\t"
                + bigPi.round(MathContext.DECIMAL64)+"x"
                + "\t" + bigPi.round(MathContext.DECIMAL128)+"x"
                + "\t" + "delta");
            int goodCount = 0;
            for (Rational item : intermediates) {
                final BigDecimal dec64 = item.toBigDecimal(MathContext.DECIMAL64);
                final BigDecimal dec128 = item.toBigDecimal(MathContext.DECIMAL128);
                final boolean goodEnough = bigPiD == item.toBigDecimal(MathContext.DECIMAL128).doubleValue();
                System.out.println(item 
                    + "\t" + dec64
                    + "x\t" + dec128 
                    + "x\t" + goodEnough
                    + "\t" + item.toBigDecimal(MathContext.DECIMAL128).subtract(bigPi));
                if (goodEnough && goodCount++ > 6) {
                    break;
                }
            }
        }
    }
    public void TestUnitPreferenceSource() {
        XMLSource xmlSource = new SimpleXMLSource("units");
        xmlSource.setNonInheriting(true);
        CLDRFile foo = new CLDRFile(xmlSource );
        foo.setDtdType(DtdType.supplementalData);
        UnitPreferences uprefs = new UnitPreferences();
        int order = 0;
        for (String line : FileUtilities.in(TestUnits.class, "UnitPreferenceSource.txt")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            List<String> items = SPLIT_SEMI.splitToList(line);
            try {
                String quantity = items.get(0);
                String usage = items.get(1);
                String regionsStr = items.get(2);
                List<String> regions = SPLIT_SPACE.splitToList(items.get(2));
                String geqStr = items.get(3);
                Rational geq = geqStr.isEmpty() ? Rational.ONE : Rational.of(geqStr);
                String skeleton = items.get(4);
                String unit = items.get(5);
                uprefs.add(quantity, usage, regionsStr, geqStr, skeleton, unit);
                String path = uprefs.getPath(order++, quantity, usage, regions, geq, skeleton);
                xmlSource.putValueAtPath(path, unit);
            } catch (Exception e) {
                errln("Failure on line: " + line + "; " + e.getMessage());
            }
        }
        if (SHOW_DATA) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
            foo.write(out);
            out.flush();
        }
    }

    static final Joiner JOIN_SPACE = Joiner.on(' ');

    private void checkUnitPreferences(UnitPreferences uprefs) {
        Set<String> usages = new LinkedHashSet<>();
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry1 : uprefs.getData().entrySet()) {
            String quantity = entry1.getKey();

            // Each of the quantities is valid.
            assertNotNull("quantity is convertible", converter.getBaseUnitFromQuantity(quantity));

            Map<String, Multimap<Set<String>, UnitPreference>> usageToRegionToUnitPreference = entry1.getValue();

            // each of the quantities has a default usage
            assertTrue("Quantity " + quantity + " contains default usage", usageToRegionToUnitPreference.containsKey("default"));

            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 : usageToRegionToUnitPreference.entrySet()) {
                String usage = entry2.getKey();
                final String quantityPlusUsage = quantity + "/" + usage;
                Multimap<Set<String>, UnitPreference> regionsToUnitPreference = entry2.getValue();
                usages.add(usage);
                Set<Set<String>> regionSets = regionsToUnitPreference.keySet();

                // all quantity + usage pairs must contain 001 (one exception)
                assertTrue("For " + quantityPlusUsage + ", the set of sets of regions must contain 001", regionSets.contains(WORLD_SET) 
                    || quantityPlusUsage.contentEquals("concentration/blood-glucose"));

                // Check that regions don't overlap for same quantity/usage
                Multimap<String, Set<String>> checkOverlap = LinkedHashMultimap.create();
                for (Set<String> regionSet : regionsToUnitPreference.keySet()) {
                    for (String region : regionSet) {
                        checkOverlap.put(region, regionSet);
                    }
                }
                for (Entry<String, Collection<Set<String>>> entry : checkOverlap.asMap().entrySet()) {
                    assertEquals(quantityPlusUsage + ": regions must be in only one set: " + entry.getValue(), 1, entry.getValue().size());
                }

                Set<String> systems = new TreeSet<>();
                for (Entry<Set<String>, Collection<UnitPreference>> entry : regionsToUnitPreference.asMap().entrySet()) {
                    Collection<UnitPreference> uPrefs = entry.getValue();
                    Set<String> regions = entry.getKey();


                    // reset these for every new set of regions
                    Rational lastSize = null;
                    String lastUnit = null;
                    Rational lastgeq = null;
                    systems.clear();
                    Set<String> lastRegions = null;
                    preferences:
                        for (UnitPreference up : uPrefs) {
                            String unitQuantity = null;
                            String topUnit = null;
                            if ("minute:second".equals(up.unit)) {
                                int debug = 0;
                            }
                            String lastQuantity = null;
                            Rational lastValue = null;
                            Rational geq = converter.parseRational(String.valueOf(up.geq));

                            // where we have an 'and' unit, get its information
                            for (String unit : SPLIT_AND.split(up.unit)) {
                                try {
                                    if (topUnit == null) {
                                        topUnit = unit;
                                    }
                                    unitQuantity = converter.getQuantityFromUnit(unit, false);
                                } catch (Exception e) {
                                    errln("Unit is not covertible: " + up.unit);
                                    continue preferences;
                                }
                                String baseUnit = converter.getBaseUnitFromQuantity(unitQuantity);
                                if (geq.compareTo(Rational.ZERO) < 0) {
                                    throw new IllegalArgumentException("geq must be > 0" + geq);
                                }
                                Rational value = converter.convert(Rational.ONE, unit, baseUnit, false);
                                if (lastQuantity != null) {
                                    int diff = value.compareTo(lastValue);
                                    if (diff >= 0) {
                                        throw new IllegalArgumentException("Bad mixed unit; biggest unit must be first: " + up.unit);
                                    }
                                    if (!lastQuantity.contentEquals(quantity)) {
                                        throw new IllegalArgumentException("Inconsistent quantities for mixed unit: " + up.unit);
                                    }
                                }
                                lastValue = value;
                                lastQuantity = quantity;
                                systems.addAll(converter.getSystems(unit));
                            }
                            String baseUnit = converter.getBaseUnitFromQuantity(unitQuantity);
                            Rational size = converter.convert(up.geq, topUnit, baseUnit, false);
                            if (lastSize != null) { // ensure descending order
                                if (!assertTrue("Successive items must be ≥ previous:\n\t" + quantityPlusUsage 
                                    + "; unit: " + up.unit
                                    + "; size: " + size
                                    + "; regions: " + regions
                                    + "; lastUnit: " + lastUnit
                                    + "; lastSize: " + lastSize
                                    + "; lastRegions: " + lastRegions
                                    , size.compareTo(lastSize) <= 0)) {
                                    int debug = 0;
                                }
                            }
                            lastSize = size;
                            lastUnit = up.unit;
                            lastgeq = geq;
                            lastRegions = regions;
                            if (SHOW_DATA) System.out.println(quantity + "\t" + usage + "\t" + regions + "\t" + up.geq + "\t" + up.unit + "\t" + up.skeleton);
                        }
                    // Check that last geq is ONE.
                    assertEquals(usage + " + " + regions + ": the least unit must have geq=1 (or equivalently, no geq)", Rational.ONE, lastgeq);

                    // Check that each set has a consistent system.
                    assertTrue(usage + " + " + regions + " has mixed systems: " + systems + "\n\t" + uPrefs, areConsistent(systems));
                }
            }
        }
    }

    private boolean areConsistent(Set<String> systems) {
        return !(systems.contains("metric") && (systems.contains("ussystem") || systems.contains("uksystem")));
    }

    public void TestBcp47() {
        checkBcp47("Quantity", converter.getQuantities(), lowercaseAZ, false);
        checkBcp47("Usage", SDI.getUnitPreferences().getUsages(), lowercaseAZ09, true);
        checkBcp47("Unit", converter.getSimpleUnits(), lowercaseAZ09, true);
    }

    private void checkBcp47(String identifierType, Set<String> identifiers, UnicodeSet allowed, boolean allowHyphens) {
        Output<Integer> counter = new Output<>(0);
        Multimap<String,String> truncatedToFullIdentifier = TreeMultimap.create();
        final Set<String> simpleUnits = identifiers;
        for (String unit : simpleUnits) {
            if (!allowHyphens && unit.contains("-")) {
                truncatedToFullIdentifier.put(unit, "-");
            }
            checkBcp47(counter, identifierType, unit, allowed, truncatedToFullIdentifier);
        }
        for (Entry<String, Collection<String>> entry : truncatedToFullIdentifier.asMap().entrySet()) {
            Set<String> identifierSet = ImmutableSet.copyOf(entry.getValue());
            assertEquals(identifierType + ": truncated identifier " + entry.getKey() + " must be unique", ImmutableSet.of(identifierSet.iterator().next()), identifierSet);
        }
    }

    private static int MIN_SUBTAG_LENGTH = 3;
    private static int MAX_SUBTAG_LENGTH = 8;

    static final UnicodeSet lowercaseAZ = new UnicodeSet("[a-z]").freeze();
    static final UnicodeSet lowercaseAZ09 = new UnicodeSet("[a-z0-9]").freeze();

    private void checkBcp47(Output<Integer> counter, String title, String identifier, UnicodeSet allowed, Multimap<String,String> truncatedToFullIdentifier) {
        StringBuilder shortIdentifer = new StringBuilder();
        boolean fail = false;
        for (String subtag : identifier.split("-")) {
            assertTrue(++counter.value + ") " + title + " identifier=" + identifier + " subtag=" + subtag + " has right characters", allowed.containsAll(subtag));
            if (!(subtag.length() >= MIN_SUBTAG_LENGTH && subtag.length() <= MAX_SUBTAG_LENGTH)) {
                for (Entry<String, Rational> entry : UnitConverter.PREFIXES.entrySet()) {
                    String prefix = entry.getKey();
                    if (subtag.startsWith(prefix)) {
                        subtag = subtag.substring(prefix.length());
                        break;
                    }
                }
            }
            if (shortIdentifer.length() != 0) {
                shortIdentifer.append('-');
            }
            if (subtag.length() > MAX_SUBTAG_LENGTH) {
                shortIdentifer.append(subtag.substring(0, MAX_SUBTAG_LENGTH));
                fail = true;
            } else {
                shortIdentifer.append(subtag);
            }
        }
        if (fail) {
            String shortIdentiferStr = shortIdentifer.toString();
            truncatedToFullIdentifier.put(shortIdentiferStr, identifier);
        }
    }

    public void TestUnitPreferences() {
        System.out.println("\n\t\t If this fails, check the output of TestUnitPreferencesSource (with -DTestUnits:SHOW_DATA), fix as needed, then incorporate.");
        UnitPreferences prefs = SDI.getUnitPreferences();
        checkUnitPreferences(prefs);
//        Map<String, Map<String, Map<String, UnitPreference>>> fastMap = prefs.getFastMap(converter);
//        for (Entry<String, Map<String, Map<String, UnitPreference>>> entry : fastMap.entrySet()) {
//            String quantity = entry.getKey();
//            String baseUnit = converter.getBaseUnitFromQuantity(quantity);
//            for (Entry<String, Map<String, UnitPreference>> entry2 : entry.getValue().entrySet()) {
//                String usage = entry2.getKey();
//                for (Entry<String, UnitPreference> entry3 : entry2.getValue().entrySet()) {
//                    String region = entry3.getKey();
//                    UnitPreference pref = entry3.getValue();
//                    System.out.println(quantity + "\t" + usage + "\t" + region + "\t" + pref.toString(baseUnit));
//                }
//            }
//        }
        prefs.getFastMap(converter); // call just to make sure we don't get an exception

        if (GENERATE_TESTS) {
            System.out.println(
                "\n# Test data for unit preferences\n" 
                    + CldrUtility.getCopyrightString("#  ") + "\n"
                    + "#\n" 
                    + "# Format:\n"
                    + "#\tQuantity;\tUsage;\tRegion;\tInput (r);\tInput (d);\tInput Unit;\tOutput (r);\tOutput (d);\tOutput Unit\n"
                    + "#\n"
                    + "# Use: Convert the Input amount & unit according to the Usage and Region.\n"
                    + "#\t The result should match the Output amount and unit.\n"
                    + "#\t Both rational (r) and double64 (d) forms of the input and output amounts are supplied so that implementations\n"
                    + "#\t have two options for testing based on the precision in their implementations. For example:\n"
                    + "#\t   3429 / 12500; 0.27432; meter;\n"
                    + "#\t The Output amount and Unit are repeated for mixed units. In such a case, only the smallest unit will have\n"
                    + "#\t both a rational and decimal amount; the others will have a single integer value, such as:\n"
                    + "#\t   length; person-height; CA; 3429 / 12500; 0.27432; meter; 2; foot; 54 / 5; 10.8; inch\n"
                    + "#\t The input and output units are unit identifers; in particular, the output does not have further processing:\n"
                    + "#\t\t • no localization\n"
                    + "#\t\t • no adjustment for pluralization\n"
                    + "#\t\t • no formatted with the skeleton\n"
                    + "#\t\t • no suppression of zero values (for secondary -and- units such as pound in stone-and-pound)\n"
                    + "#\n"
                    + "# Generation: Set GENERATE_TESTS in TestUnits.java, and look at TestUnitPreferences results.\n"
                );
            Rational ONE_TENTH = Rational.of(1,10);

            // Note that for production usage, precomputed data like the prefs.getFastMap(converter) would be used instead of the raw data.

            for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry : prefs.getData().entrySet()) {
                String quantity = entry.getKey();
                String baseUnit = converter.getBaseUnitFromQuantity(quantity);
                for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 : entry.getValue().entrySet()) {
                    String usage = entry2.getKey();

                    // collect samples of base units
                    for (Entry<Set<String>, Collection<UnitPreference>> entry3 : entry2.getValue().asMap().entrySet()) {
                        boolean first = true;
                        Set<Rational> samples = new TreeSet<>(Comparator.reverseOrder());
                        for (UnitPreference pref : entry3.getValue()) {
                            final String topUnit = UnitPreferences.SPLIT_AND.split(pref.unit).iterator().next();
                            if (first) {
                                samples.add(converter.convert(pref.geq.add(ONE_TENTH), topUnit, baseUnit, false));
                                first = false;
                            }
                            samples.add(converter.convert(pref.geq, topUnit, baseUnit, false));
                            samples.add(converter.convert(pref.geq.subtract(ONE_TENTH), topUnit, baseUnit, false));
                        }
                        // show samples
                        Set<String> regions = entry3.getKey();
                        String sampleRegion = regions.iterator().next();
                        if (usage.equals("person-age")) {
                            int debug = 0;
                        }
                        Collection<UnitPreference> uprefs = entry3.getValue();
                        for (Rational sample : samples) {
                            showSample(quantity, usage, sampleRegion, sample, baseUnit, uprefs);
                        }
                        System.out.println();
                    }
                }
            }
        }
    }

    private void showSample(String quantity, String usage, String sampleRegion, Rational sampleBaseValue, String baseUnit, Collection<UnitPreference> prefs) {
        String lastUnit = null;
        boolean gotOne = false;
        for (UnitPreference pref : prefs) {
            final String topUnit = UnitPreferences.SPLIT_AND.split(pref.unit).iterator().next();
            Rational baseGeq = converter.convert(pref.geq, topUnit, baseUnit, false);
            if (sampleBaseValue.compareTo(baseGeq) >= 0) {
                showSample2(quantity, usage, sampleRegion, sampleBaseValue, baseUnit, pref.unit);
                gotOne = true;
                break;
            }
            lastUnit = pref.unit;
        }
        if (!gotOne) {
            showSample2(quantity, usage, sampleRegion, sampleBaseValue, baseUnit, lastUnit);
        }
    }

    private void showSample2(String quantity, String usage, String sampleRegion, Rational sampleBaseValue, String baseUnit, String lastUnit) {
        Rational originalSampleBaseValue = sampleBaseValue;
        // Known slow algorithm for mixed values, but for generating tests we don't care.
        final List<String> units = UnitPreferences.SPLIT_AND.splitToList(lastUnit);
        StringBuilder formattedUnit = new StringBuilder();
        int remaining = units.size();
        for (String unit : units) {
            --remaining;
            Rational sample = converter.convert(sampleBaseValue, baseUnit, unit, false);
            if (formattedUnit.length() != 0) {
                formattedUnit.append(TEST_SEP);
            }
            if (remaining != 0) {
                BigInteger floor = sample.floor();
                formattedUnit.append(floor + TEST_SEP + unit);
                // convert back to base unit
                sampleBaseValue = converter.convert(sample.subtract(Rational.of(floor)), unit, baseUnit, false);
            } else {
                formattedUnit.append(sample + TEST_SEP + sample.doubleValue() + TEST_SEP + unit);
            }
        }
        System.out.println(quantity + TEST_SEP + usage + TEST_SEP + sampleRegion 
            + TEST_SEP + originalSampleBaseValue + TEST_SEP + originalSampleBaseValue.doubleValue() + TEST_SEP + baseUnit
            + TEST_SEP + formattedUnit);
    }

    public void TestWithExternalData() throws IOException {

        Multimap<String, ExternalUnitConversionData> seen = HashMultimap.create();
        Set<ExternalUnitConversionData> cantConvert = new LinkedHashSet<>();
        Map<ExternalUnitConversionData, Rational> convertDiff = new LinkedHashMap<>();
        Set<String> remainingCldrUnits = new LinkedHashSet<>(converter.getInternalConversionData().keySet());
        Set<ExternalUnitConversionData> couldAdd = new LinkedHashSet<>();

        if (SHOW_DATA) {
            System.out.println();
        }
        for (ExternalUnitConversionData data : NistUnits.externalConversionData) {
            Rational externalResult = data.info.convert(Rational.ONE);
            Rational cldrResult = converter.convert(Rational.ONE, data.source, data.target, false);
            seen.put(data.source + "⟹" + data.target, data);

            if (externalResult.isPowerOfTen()) {
                couldAdd.add(data);
            }

            if (cldrResult.equals(Rational.NaN)) {
                cantConvert.add(data);
            } else {
                final Rational symmetricDiff = externalResult.symmetricDiff(cldrResult);
                if (symmetricDiff.abs().compareTo(Rational.of(1, 1000000)) > 0){
                    convertDiff.put(data, cldrResult);
                } else {
                    remainingCldrUnits.remove(data.source);
                    remainingCldrUnits.remove(data.target);
                    if (SHOW_DATA) System.out.println("*Converted"
                        + "\t" + cldrResult.doubleValue() 
                        + "\t" + externalResult.doubleValue() 
                        + "\t" + symmetricDiff.doubleValue() 
                        + "\t" + data);
                }
            }
        }

        // get additional data on derived units
//        for (Entry<String, TargetInfo> e : NistUnits.derivedUnitToConversion.entrySet()) {
//            String sourceUnit = e.getKey();
//            TargetInfo targetInfo = e.getValue();
//
//            Rational conversion = converter.convert(Rational.ONE, sourceUnit, targetInfo.target, false);
//            if (conversion.equals(Rational.NaN)) {
//                couldAdd.add(new ExternalUnitConversionData("", sourceUnit, targetInfo.target, conversion, "?", null));
//            }
//        }
        if (SHOW_DATA) {
            for (Entry<String, Collection<String>> e : NistUnits.unitToQuantity.asMap().entrySet()) {
                System.out.println("*Quantities:"  + "\t" +  e.getKey()  + "\t" +  e.getValue());
            }
        }

        for (String remainingUnit : remainingCldrUnits) {
            final TargetInfo targetInfo = converter.getInternalConversionData().get(remainingUnit);
            if (!targetInfo.target.contentEquals(remainingUnit)) {
                warnln("Not tested against external data\t" + remainingUnit + "\t" + targetInfo);
            }
        }

        boolean showDiagnostics = false;
        for (Entry<String, Collection<ExternalUnitConversionData>> entry : seen.asMap().entrySet()) {
            if (entry.getValue().size() != 1) {
                Multimap<ConversionInfo, ExternalUnitConversionData> factors = HashMultimap.create();
                for (ExternalUnitConversionData s : entry.getValue()) {
                    factors.put(s.info, s);
                }
                if (factors.keySet().size() > 1) {
                    for (ExternalUnitConversionData s : entry.getValue()) {
                        errln("*DUP-" + s);
                        showDiagnostics = true;
                    }
                }
            }
        }

        if (convertDiff.size() > 0) {
            for (Entry<ExternalUnitConversionData, Rational> e : convertDiff.entrySet()) {
                final Rational computed = e.getValue();
                final ExternalUnitConversionData external = e.getKey();
                Rational externalResult = external.info.convert(Rational.ONE);
                showDiagnostics = true;
                // for debugging
                converter.convert(Rational.ONE, external.source, external.target, true);

                errln("*DIFF CONVERT:"
                    + "\t" + external.source
                    + "\t⟹\t" + external.target
                    + "\texpected\t" + externalResult.doubleValue()
                    + "\tactual:\t" + computed.doubleValue()
                    + "\tsdiff:\t" + computed.symmetricDiff(externalResult).abs().doubleValue()
                    + "\txdata:\t" + external);
            }
        }

        // temporary: show the items that didn't covert correctly
        if (showDiagnostics) {
            System.out.println();
            Rational x = showDelta("pound-fahrenheit", "gram-celsius", false);
            Rational y = showDelta("calorie", "joule", false);
            showDelta("product\t", x.multiply(y));
            showDelta("british-thermal-unit", "calorie", false);
            showDelta("inch-ofhg", "pascal", false);
            showDelta("millimeter-ofhg", "pascal", false);
            showDelta("ofhg", "kilogram-per-square-meter-square-second", false);
            showDelta("13595.1*gravity", Rational.of("9.80665*13595.1"));

            showDelta("fahrenheit-hour-square-foot-per-british-thermal-unit-inch", "meter-kelvin-per-watt", true);
        }

        if (showDiagnostics && NistUnits.skipping.size() > 0) {
            System.out.println();
            for (String s : NistUnits.skipping) {
                System.out.println("*SKIPPING " + s);
            }
        }
        if (showDiagnostics && NistUnits.idChanges.size() > 0) {
            System.out.println();
            for (Entry<String, Collection<String>> e : NistUnits.idChanges.asMap().entrySet()) {
                if (SHOW_DATA) System.out.println("*CHANGES\t" + e.getKey() + "\t" + Joiner.on('\t').join(e.getValue()));
            }
        }

        if (showDiagnostics && cantConvert.size() > 0) {
            System.out.println();
            for (ExternalUnitConversionData e : cantConvert) {
                System.out.println("*CANT CONVERT-" + e);
            }
        }
        Output<String> baseUnit = new Output<String>();
        for (ExternalUnitConversionData s : couldAdd) {
            String target = s.target;
            Rational endFactor = s.info.factor;
            String mark = "";
            TargetInfo baseUnit2 = NistUnits.derivedUnitToConversion.get(s.target);
            if (baseUnit2 != null) {
                target = baseUnit2.target;
                endFactor = baseUnit2.unitInfo.factor;
                mark="¹";
            } else {
                ConversionInfo conversionInfo = converter.getUnitInfo(s.target, baseUnit);
                if (conversionInfo != null && !s.target.equals(baseUnit.value)) {
                    target = baseUnit.value;
                    endFactor = conversionInfo.convert(s.info.factor);
                    mark="²";
                }
            }
            System.out.println("Could add 10^X conversion from a"
                + "\t" +  s.source 
                + "\tto" + mark
                + "\t" + endFactor.toString(FormatStyle.simple)  
                + "\t" + target);
        }
    }

    private Rational showDelta(String firstUnit, String secondUnit, boolean showYourWork) {
        Rational x = converter.convert(Rational.ONE, firstUnit, secondUnit, showYourWork);
        return showDelta(firstUnit + "\t" + secondUnit, x);
    }

    private Rational showDelta(final String title, Rational rational) {
        System.out.print("*CONST\t" + title);
        System.out.print("\t" + rational.toString(FormatStyle.simple));
        System.out.println("\t" + rational.doubleValue());
        return rational;
    }

    public void TestRepeating() {
        Set<Rational> seen = new HashSet<>();
        String[][] tests = {
            {"0/0", "NaN"},
            {"1/0", "INF"},
            {"-1/0", "-INF"},
            {"0/1", "0"},
            {"1/1", "1"},
            {"1/2", "0.5"},
            {"1/3", "0.˙3"},
            {"1/4", "0.25"},
            {"1/5", "0.2"},
            {"1/6", "0.1˙6"},
            {"1/7", "0.˙142857"},
            {"1/8", "0.125"},
            {"1/9", "0.˙1"},
            {"1/10", "0.1"},
            {"1/11", "0.˙09"},
            {"1/12", "0.08˙3"},
            {"1/13", "0.˙076923"},
            {"1/14", "0.0˙714285"},
            {"1/15", "0.0˙6"},
            {"1/16", "0.0625"},
        };
        for (String[] test : tests) {
            Rational source = Rational.of(test[0]);
            seen.add(source);
            String expected = test[1];
            String actual = source.toString(FormatStyle.repeating);
            assertEquals(test[0], expected, actual);
            Rational roundtrip = Rational.of(expected);
            assertEquals(expected, source, roundtrip);
        }
        for (int i = -50; i < 200; ++i) {
            for (int j = 0; j < 50; ++j) {
                checkFormat(Rational.of(i, j), seen);
            }
        }
        for (Entry<String, TargetInfo> unitAndInfo : converter.getInternalConversionData().entrySet()) {
            final TargetInfo targetInfo2 = unitAndInfo.getValue();
            ConversionInfo targetInfo = targetInfo2.unitInfo;
            checkFormat(targetInfo.factor, seen);
            if (SHOW_DATA) {
                String rFormat = targetInfo.factor.toString(FormatStyle.repeating);
                String sFormat = targetInfo.factor.toString(FormatStyle.simple);
                if (!rFormat.equals(sFormat)) {
                    System.out.println("\t\t" + unitAndInfo.getKey() + "\t" + targetInfo2.target + "\t" + sFormat + "\t" + rFormat + "\t" + targetInfo.factor.doubleValue());
                }
            }
        }
    }

    private void checkFormat(Rational source, Set<Rational> seen) {
        if (seen.contains(source)) {
            return;
        }
        seen.add(source);
        String formatted = source.toString(FormatStyle.repeating);
        Rational roundtrip = Rational.of(formatted);
        assertEquals("roundtrip " + formatted, source, roundtrip);
    }
}
