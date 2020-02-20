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
    private static final ImmutableSet<String> WORLD_SET = ImmutableSet.of("001");

    private static final boolean SHOW_DATA = CldrUtility.getProperty("TestUnits:SHOW_DATA", false);

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
        System.out.println();
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
        assertEquals("Unconvertable units", Collections.emptySet(), badUnits);
        assertEquals("Units without Quantity", Collections.emptySet(), noQuantity);
        if (SHOW_DATA) { // test data
            System.out.println(
                "# Test data for unit conversions\n" 
                    + "# Format:\n"
                    + "#\tQuantity\t;\tx\t;\ty\t;\tconversion to y (rational)\t;\ttest: 1000 x ⟹ y\n"
                    + "#\n"
                    + "# Use: convert 1000 x units to the y unit; the result should match the final column,\n"
                    + "#   at the given precision. For example, when the last column is 159.1549,\n"
                    + "#   round to 4 decimal digits before comparing.\n"
                    + "# Note that certain conversions are approximate, such as degrees to radians\n"
                    + "#\n"
                    + "# Generation: Set SHOW_DATA in TestUnits.java, and look at TestParseUnit results.\n"
                );
            for (Entry<Pair<String, Double>, String> entry : testPrintout.entries()) {
                System.out.println(entry.getValue());
            }
        }
    }

    static final Set<String> NOT_CONVERTABLE = ImmutableSet.of("generic");

    private void checkUnitConvertability(UnitConverter converter, Output<String> compoundBaseUnit, 
        Set<String> badUnits, Set<String> noQuantity, String type, String unit, 
        Multimap<Pair<String, Double>, String> testPrintout) {

        if (unit.equals("liter-per-100kilometers")) {
            int debug = 0;
        }
        if (converter.isBaseUnit(unit)) {
            String quantity = converter.getQuantityFromBaseUnit(unit);
            if (quantity == null) {
                noQuantity.add(unit);
            }
            if (SHOW_DATA) {
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
            } else if (SHOW_DATA){
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
                    case "luminous-flux": case "power": continue;
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
    static final Set<String> GRANDFATHERED_SIMPLES = ImmutableSet.of("em", "g-force", "inch-hg", "liter-per-100-kilometer", "millimeter-of-mercury", "therm-us");

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
        checkFactorForPowerOfTen(100, 1);
        checkFactorForPowerOfTen(2, 5);
        checkFactorForPowerOfTen(4, 25);
        checkFactorForPowerOfTen(5, 2);
        checkFactorForPowerOfTen(25, 4);
        checkFactorForPowerOfTen(3, null);
        checkFactorForPowerOfTen(1, 1);
        checkFactorForPowerOfTen(0, null);
    }

    private void checkFactorForPowerOfTen(int source, Integer expected) {
        assertEquals(source+"", 
            expected == null ? null : BigInteger.valueOf(expected), 
                Rational.factorForPowerOfTen(BigInteger.valueOf(source)));
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
        "ounce-troy", "nautical-mile", "fahrenheit", "inch-hg", 
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
    static Set<String> OTHER_SYSTEM = ImmutableSet.of(
        "g-force", "dalton", "calorie", "earth-radius", 
        "solar-radius", "solar-radius", "astronomical-unit", "light-year", "parsec", "earth-mass", 
        "solar-mass", "bit", "byte", "karat", "solar-luminosity", "millimeter-of-mercury", "atmosphere", 
        "pixel", "dot", "part-per-million", "permyriad", "permille", "percent", "karat", "portion",
        "minute", "hour", "day", "day-person", "week", "week-person",
        "year", "year-person", "decade", "month", "month-person", "century",
        "arc-second", "arc-minute", "degree", "radian", "revolution",
        "electronvolt");

    public void TestEnglishSystems() {
        Multimap<String, String> toSystems = converter.getSourceToSystems();
        Multimap<String, String> systemToUnit = Multimaps.invertFrom(toSystems, TreeMultimap.create());
        Set<String> metric = new HashSet<>(converter.canConvert());
        for (Entry<String, Collection<String>> systemAndUnits : systemToUnit.asMap().entrySet()) {
            String system = systemAndUnits.getKey();
            final Collection<String> units = systemAndUnits.getValue();
            printSystemUnits(metric, system, units);
        }
        printSystemUnits(metric, "other", OTHER_SYSTEM);
        printSystemUnits(null, "metric", metric);
    }

    private void printSystemUnits(Set<String> metric, String system, Collection<String> units) {
        Multimap<String,String> quantityToUnits = TreeMultimap.create();
        for (String unit : units) {
            quantityToUnits.put(converter.getQuantityFromUnit(unit, false), unit);
        }
        for (Entry<String, Collection<String>> entry : quantityToUnits.asMap().entrySet()) {
            String quantity = entry.getKey();
            String baseUnit = converter.getBaseUnitToQuantity().inverse().get(quantity);
            Multimap<Rational,String> sorted = TreeMultimap.create();
            sorted.put(Rational.ONE, baseUnit);
            if (metric != null) {
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
                if (metric != null) {
                    metric.remove(unit);
                }
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
            System.out.println("Target\t" + bigPi.round(MathContext.DECIMAL64)+"x" + "\t" + bigPi.round(MathContext.DECIMAL128)+"x");
            int goodCount = 0;
            for (Rational item : intermediates) {
                final BigDecimal dec64 = item.toBigDecimal(MathContext.DECIMAL64);
                final BigDecimal dec128 = item.toBigDecimal(MathContext.DECIMAL128);
                final boolean goodEnough = bigPiD == item.toBigDecimal(MathContext.DECIMAL128).doubleValue();
                System.out.println(item + "\t" + dec64 + "x\t" + dec128 + "x\t" + goodEnough);
                if (goodEnough && goodCount++ > 1) {
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
                double geq = geqStr.isEmpty() ? 1d : Double.parseDouble(geqStr);
                String skeleton = items.get(4);
                String unit = items.get(5);
                uprefs.add(quantity, usage, regionsStr, geqStr, skeleton, unit);
                String path = uprefs.getPath(order++, quantity, usage, regions, geq, skeleton);
                xmlSource.putValueAtPath(path, unit);
            } catch (Exception e) {
                errln("Failure on line: " + line + "; " + e.getMessage());
            }
        }
        checkUnitPreferences(uprefs);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
        foo.write(out);
        out.flush();
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
                
                for (Entry<Set<String>, Collection<UnitPreference>> entry : regionsToUnitPreference.asMap().entrySet()) {
                    Collection<UnitPreference> uPrefs = entry.getValue();
                    Set<String> regions = entry.getKey();
                    
                    
                    // reset these for every new set of regions
                    Rational lastSize = null;
                    String lastUnit = null;
                    Set<String> lastRegions = null;
                    preferences:
                        for (UnitPreference up : uPrefs) {
                            String unitQuantity = null;
                            String topUnit = null;
                            if ("minute:second".equals(up.unit)) {
                                int debug = 0;
                            }
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
                                if (!assertEquals(up.unit, quantity, unitQuantity)) {
                                    int debug = 0;
                                }
                            }
                            String baseUnit = converter.getBaseUnitFromQuantity(unitQuantity);
                            Rational rationalGeq = Rational.of(BigDecimal.valueOf(up.geq));
                            Rational size = converter.convert(rationalGeq, topUnit, baseUnit, false);
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
                            lastRegions = regions;
                            System.out.println(quantity + "\t" + usage + "\t" + regions + "\t" + up.geq + "\t" + up.unit + "\t" + up.skeleton);
                        }
                }
            }
        }
        final UnicodeSet lowercaseAZ = new UnicodeSet("[a-z]").freeze();
        for (String usage : usages) {
            checkBcp47(usage, lowercaseAZ);
        }
    }

    private void checkBcp47(String usage, UnicodeSet allowed) {
        for (String subtag : usage.split("-")) {
            assertTrue(subtag, allowed.containsAll(subtag) && subtag.length() > 2 && subtag.length() <= 8);
        }
    }

    public void TestUnitPreferences() {
        System.out.println("\t If this fails, check the output of TestUnitPreferencesSource, fix as needed, then incorporate");
        UnitPreferences prefs = SDI.getUnitPreferences();
        checkUnitPreferences(prefs);
    }
}
