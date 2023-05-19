package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.Comparators;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckUnits;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GrammarDerivation;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.LocaleStringProvider;
import org.unicode.cldr.util.MapComparator;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.ContinuedFraction;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.Rational.RationalParser;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.SupplementalDataInfo.UnitIdComponentType;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.Continuation;
import org.unicode.cldr.util.UnitConverter.Continuation.UnitIterator;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.UnitConverter.UnitComplexity;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitConverter.UnitSystem;
import org.unicode.cldr.util.UnitParser;
import org.unicode.cldr.util.UnitPathType;
import org.unicode.cldr.util.UnitPreferences;
import org.unicode.cldr.util.UnitPreferences.UnitPreference;
import org.unicode.cldr.util.Units;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

public class TestUnits extends TestFmwk {

    /** Flags to emit debugging information */
    private static final boolean SHOW_UNIT_ORDER = getFlag("TestUnits:SHOW_UNIT_ORDER");

    private static final boolean SHOW_UNIT_CATEGORY = getFlag("TestUnits:SHOW_UNIT_CATEGORY");
    private static final boolean SHOW_COMPOSE = getFlag("TestUnits:SHOW_COMPOSE");
    private static final boolean SHOW_DATA = getFlag("TestUnits:SHOW_DATA");

    /** Flags for reformatting data file */
    private static final boolean SHOW_PREFS = getFlag("TestUnits:SHOW_PREFS");

    /** Flag for generating test: TODO move to separate file */
    private static final boolean GENERATE_TESTS = getFlag("TestUnits:GENERATE_TESTS");

    private static final Set<String> VALID_REGULAR_UNITS =
            Validity.getInstance().getStatusToCodes(LstrType.unit).get(Validity.Status.regular);
    private static final Set<String> DEPRECATED_REGULAR_UNITS =
            Validity.getInstance().getStatusToCodes(LstrType.unit).get(Validity.Status.deprecated);
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final Integer INTEGER_ONE = Integer.valueOf(1);

    public static boolean getFlag(String flag) {
        return CldrUtility.getProperty(flag, false);
    }

    private static final String TEST_SEP = ";\t";

    private static final ImmutableSet<String> WORLD_SET = ImmutableSet.of("001");
    private static final CLDRConfig info = CLDR_CONFIG;
    private static final SupplementalDataInfo SDI = info.getSupplementalDataInfo();

    static final UnitConverter converter = SDI.getUnitConverter();
    static final Set<String> VALID_SHORT_UNITS = converter.getShortIds(VALID_REGULAR_UNITS);
    static final Set<String> DEPRECATED_SHORT_UNITS =
            converter.getShortIds(DEPRECATED_REGULAR_UNITS);

    static final Splitter SPLIT_SEMI = Splitter.on(Pattern.compile("\\s*;\\s*")).trimResults();
    static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    static final Splitter SPLIT_AND = Splitter.on("-and-").trimResults().omitEmptyStrings();
    static final Splitter SPLIT_DASH = Splitter.on('-').trimResults().omitEmptyStrings();

    static final Rational R1000 = Rational.of(1000);

    static Map<String, String> normalizationCache = new TreeMap<>();

    public static void main(String[] args) {
        new TestUnits().run(args);
    }

    private Map<String, String> BASE_UNIT_TO_QUANTITY = converter.getBaseUnitToQuantity();

    public void TestSpaceInNarrowUnits() {
        final CLDRFile english = CLDR_CONFIG.getEnglish();
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

    static final String PATH_UNIT_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"{1}\"]/unitPattern[@count=\"{2}\"]";

    static final String PATH_PREFIX_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/unitPrefixPattern";
    static final String PATH_SUFFIX_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/compoundUnitPattern1";

    static final String PATH_MILLI_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"10p-3\"]/unitPrefixPattern";
    static final String PATH_SQUARE_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1";

    static final String PATH_METER_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"length-meter\"]/unitPattern[@count=\"{1}\"]";
    static final String PATH_MILLIMETER_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"length-millimeter\"]/unitPattern[@count=\"{1}\"]";
    static final String PATH_SQUARE_METER_PATTERN =
            "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"area-square-meter\"]/unitPattern[@count=\"{1}\"]";

    public void TestAUnits() {
        if (isVerbose()) {
            System.out.println();
            Output<String> baseUnit = new Output<>();
            int count = 0;
            for (String simpleUnit : converter.getSimpleUnits()) {
                ConversionInfo conversion = converter.parseUnitId(simpleUnit, baseUnit, false);
                if (simpleUnit.equals(baseUnit)) {
                    continue;
                }
                System.out.println(
                        ++count
                                + ")\t"
                                + simpleUnit
                                + " → "
                                + baseUnit
                                + "; factor = "
                                + conversion.factor
                                + " = "
                                + conversion.factor.toString(FormatStyle.repeatingAll)
                                + (conversion.offset.equals(Rational.ZERO)
                                        ? ""
                                        : "; offset = " + conversion.offset));
            }
        }
    }

    public void TestCompoundUnit3() {
        Factory factory = CLDR_CONFIG.getCldrFactory();

        Map<String, String> prefixToType = new LinkedHashMap<>();
        for (String[] prefixRow : PREFIX_NAME_TYPE) {
            prefixToType.put(prefixRow[0], prefixRow[1]);
        }
        prefixToType = ImmutableMap.copyOf(prefixToType);

        Set<String> localesToTest = ImmutableSet.of("en"); // factory.getAvailableLanguages();
        int testCount = 0;
        for (String locale : localesToTest) {
            CLDRFile file = factory.make(locale, true);
            // ExampleGenerator exampleGenerator = getExampleGenerator(locale);
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
                    String prefixPath =
                            ExampleGenerator.format(
                                    isPrefix ? PATH_PREFIX_PATTERN : PATH_SUFFIX_PATTERN,
                                    len,
                                    prefixType);
                    String prefixValue = file.getStringValue(prefixPath);
                    boolean lowercaseIfSpaced = len.equals("long");

                    for (Count count : pluralInfo.getCounts()) {
                        final String countString = count.toString();
                        String targetUnitPath =
                                ExampleGenerator.format(
                                        PATH_UNIT_PATTERN, len, targetUnit, countString);
                        String targetUnitPattern = file.getStringValue(targetUnitPath);

                        String baseUnitPath =
                                ExampleGenerator.format(
                                        PATH_UNIT_PATTERN, len, baseUnit, countString);
                        String baseUnitPattern = file.getStringValue(baseUnitPath);

                        String composedTargetUnitPattern =
                                Units.combinePattern(
                                        baseUnitPattern, prefixValue, lowercaseIfSpaced);
                        if (isEnglish && !targetUnitPattern.equals(composedTargetUnitPattern)) {
                            if (allowEnglishException(
                                    targetUnitPattern, composedTargetUnitPattern)) {
                                continue;
                            }
                        }
                        if (!assertEquals2(
                                errMsg,
                                testCount++
                                        + ") "
                                        + locale
                                        + "/"
                                        + len
                                        + "/"
                                        + count
                                        + "/"
                                        + prefix
                                        + "+"
                                        + baseUnit
                                        + ": constructed pattern",
                                targetUnitPattern,
                                composedTargetUnitPattern)) {
                            Units.combinePattern(baseUnitPattern, prefixValue, lowercaseIfSpaced);
                            int debug = 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * Curated list of known exceptions. Usually because the short form of a unit is shorter when
     * combined with a prefix or suffix
     */
    static final Map<String, String> ALLOW_ENGLISH_EXCEPTION =
            ImmutableMap.<String, String>builder()
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

    private boolean allowEnglishException(
            String targetUnitPattern, String composedTargetUnitPattern) {
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
    private boolean assertEquals2(
            int TestERR, String title, String sqmeterPattern, String conSqmeterPattern) {
        if (!Objects.equals(sqmeterPattern, conSqmeterPattern)) {
            msg(
                    title + ", expected «" + sqmeterPattern + "», got «" + conSqmeterPattern + "»",
                    TestERR,
                    true,
                    true);
            return false;
        } else if (isVerbose()) {
            msg(
                    title + ", expected «" + sqmeterPattern + "», got «" + conSqmeterPattern + "»",
                    LOG,
                    true,
                    true);
        }
        return true;
    }

    static final boolean DEBUG = false;

    public void TestConversion() {
        String[][] tests = {
            {"foot", "12", "inch"},
            {"gallon", "4", "quart"},
            {"gallon", "16", "cup"},
        };
        for (String[] test : tests) {
            String sourceUnit = test[0];
            Rational factor = Rational.of(test[1]);
            String targetUnit = test[2];
            final Rational convert = converter.convertDirect(Rational.ONE, sourceUnit, targetUnit);
            assertEquals(sourceUnit + " to " + targetUnit, factor, convert);
        }

        // test conversions are disjoint
        Set<String> gotAlready = new HashSet<>();
        List<Set<String>> equivClasses = new ArrayList<>();
        Map<String, String> classToId = new TreeMap<>();
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
        //                    errln("Overlapping equivalence classes: " + eclass1 + " ~ " + eclass2
        // + "\n\tProbably bad chain requiring 3 steps.");
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
                assertTrue(
                        unit + ": " + piece + " in " + UnitConverter.BASE_UNIT_PARTS,
                        UnitConverter.BASE_UNIT_PARTS.contains(piece));
            }
        }
    }

    public void TestUnitId() {

        for (String simple : converter.getSimpleUnits()) {
            String canonicalUnit = converter.getBaseUnit(simple);
            UnitId unitId = converter.createUnitId(canonicalUnit);
            String output = unitId.toString();
            if (!assertEquals(
                    simple + ": targets should be in canonical form", output, canonicalUnit)) {
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
                if (!assertEquals(
                        quantity + ": targets should be in canonical form", output, baseUnit)) {
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
                converter.createUnitId(baseUnit); // for debugging
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
        Multimap<Pair<String, Double>, String> testPrintout = TreeMultimap.create();

        // checkUnitConvertability(converter, compoundBaseUnit, badUnits, "pint-metric-per-second");

        for (Entry<String, String> entry : TYPE_TO_CORE.entries()) {
            String type = entry.getKey();
            String unit = entry.getValue();
            if (NOT_CONVERTABLE.contains(unit)) {
                continue;
            }
            checkUnitConvertability(
                    converter, compoundBaseUnit, badUnits, noQuantity, type, unit, testPrintout);
        }
        if (GENERATE_TESTS) { // test data
            try (TempPrintWriter pw =
                    TempPrintWriter.openUTF8Writer(
                            CLDRPaths.TEST_DATA + "units", "unitsTest.txt")) {

                pw.println(
                        "# Test data for unit conversions\n"
                                + CldrUtility.getCopyrightString("#  ")
                                + "\n"
                                + "#\n"
                                + "# Format:\n"
                                + "#\tQuantity\t;\tx\t;\ty\t;\tconversion to y (rational)\t;\ttest: 1000 x ⟹ y\n"
                                + "#\n"
                                + "# Use: convert 1000 x units to the y unit; the result should match the final column,\n"
                                + "#   at the given precision. For example, when the last column is 159.1549,\n"
                                + "#   round to 4 decimal digits before comparing.\n"
                                + "# Note that certain conversions are approximate, such as degrees to radians\n"
                                + "#\n"
                                + "# Generation: Set GENERATE_TESTS in TestUnits.java, and look at TestParseUnit results.\n");
                for (Entry<Pair<String, Double>, String> entry : testPrintout.entries()) {
                    pw.println(entry.getValue());
                }
            }
        }
        assertEquals("Unconvertable units", Collections.emptySet(), badUnits);
        assertEquals("Units without Quantity", Collections.emptySet(), noQuantity);
    }

    static final Set<String> NOT_CONVERTABLE = ImmutableSet.of("generic", "beaufort");

    private void checkUnitConvertability(
            UnitConverter converter,
            Output<String> compoundBaseUnit,
            Set<String> badUnits,
            Set<String> noQuantity,
            String type,
            String unit,
            Multimap<Pair<String, Double>, String> testPrintout) {

        if (converter.isBaseUnit(unit)) {
            String quantity = converter.getQuantityFromBaseUnit(unit);
            if (quantity == null) {
                noQuantity.add(unit);
            }
            if (GENERATE_TESTS) {
                testPrintout.put(
                        new Pair<>(quantity, 1000d),
                        quantity + "\t;\t" + unit + "\t;\t" + unit + "\t;\t1 * x\t;\t1,000.00");
            }
        } else {
            ConversionInfo unitInfo = converter.getUnitInfo(unit, compoundBaseUnit);
            if (unitInfo == null) {
                unitInfo = converter.parseUnitId(unit, compoundBaseUnit, false);
            }
            if (unitInfo == null) {
                badUnits.add(unit);
            } else if (GENERATE_TESTS) {
                String quantity = converter.getQuantityFromBaseUnit(compoundBaseUnit.value);
                if (quantity == null) {
                    noQuantity.add(compoundBaseUnit.value);
                }
                final double testValue =
                        unitInfo.convert(R1000).toBigDecimal(MathContext.DECIMAL32).doubleValue();
                testPrintout.put(
                        new Pair<>(quantity, testValue),
                        quantity
                                + "\t;\t"
                                + unit
                                + "\t;\t"
                                + compoundBaseUnit
                                + "\t;\t"
                                + unitInfo
                                + "\t;\t"
                                + testValue
                        //                    + "\t" +
                        // unitInfo.factor.toBigDecimal(MathContext.DECIMAL32)
                        //                    + "\t" +
                        // unitInfo.factor.reciprocal().toBigDecimal(MathContext.DECIMAL32)
                        );
            }
        }
    }

    public void TestRational() {
        Rational a3_5 = Rational.of(3, 5);

        Rational a6_10 = Rational.of(6, 10);
        assertEquals("", a3_5, a6_10);

        Rational a5_3 = Rational.of(5, 3);
        assertEquals("", a3_5, a5_3.reciprocal());

        assertEquals("", Rational.ONE, a3_5.multiply(a3_5.reciprocal()));
        assertEquals("", Rational.ZERO, a3_5.add(a3_5.negate()));

        assertEquals("", Rational.INFINITY, Rational.ZERO.reciprocal());
        assertEquals("", Rational.NEGATIVE_INFINITY, Rational.INFINITY.negate());
        assertEquals("", Rational.NEGATIVE_ONE, Rational.ONE.negate());

        assertEquals("", Rational.NaN, Rational.ZERO.divide(Rational.ZERO));

        assertEquals("", BigDecimal.valueOf(2), Rational.of(2, 1).toBigDecimal());
        assertEquals("", BigDecimal.valueOf(0.5), Rational.of(1, 2).toBigDecimal());

        assertEquals("", BigDecimal.valueOf(100), Rational.of(100, 1).toBigDecimal());
        assertEquals("", BigDecimal.valueOf(0.01), Rational.of(1, 100).toBigDecimal());

        assertEquals("", Rational.of(12370, 1), Rational.of(BigDecimal.valueOf(12370)));
        assertEquals("", Rational.of(1237, 10), Rational.of(BigDecimal.valueOf(1237.0 / 10)));
        assertEquals("", Rational.of(1237, 10000), Rational.of(BigDecimal.valueOf(1237.0 / 10000)));

        ConversionInfo uinfo = new ConversionInfo(Rational.of(2), Rational.of(3));
        assertEquals("", Rational.of(3), uinfo.convert(Rational.ZERO));
        assertEquals("", Rational.of(7), uinfo.convert(Rational.of(2)));
    }

    public void TestRationalParse() {
        Rational.RationalParser parser = SDI.getRationalParser();

        Rational a3_5 = Rational.of(3, 5);

        assertEquals("", a3_5, parser.parse("6/10"));

        assertEquals("", a3_5, parser.parse("0.06/0.10"));

        assertEquals("", Rational.of(381, 1250), parser.parse("ft_to_m"));
        assertEquals(
                "", 6.02214076E+23d, parser.parse("6.02214076E+23").toBigDecimal().doubleValue());
        Rational temp = parser.parse("gal_to_m3");
        // System.out.println(" " + temp);
        assertEquals(
                "", 0.003785411784, temp.numerator.doubleValue() / temp.denominator.doubleValue());
    }

    static final Map<String, String> CORE_TO_TYPE;
    static final Multimap<String, String> TYPE_TO_CORE;

    static {
        Set<String> VALID_UNITS =
                Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular);

        Map<String, String> coreToType = new TreeMap<>();
        TreeMultimap<String, String> typeToCore = TreeMultimap.create();
        for (String s : VALID_UNITS) {
            int dashPos = s.indexOf('-');
            String unitType = s.substring(0, dashPos);
            String coreUnit = s.substring(dashPos + 1);
            coreUnit = converter.fixDenormalized(coreUnit);
            coreToType.put(coreUnit, unitType);
            typeToCore.put(unitType, coreUnit);
        }
        CORE_TO_TYPE = ImmutableMap.copyOf(coreToType);
        TYPE_TO_CORE = ImmutableMultimap.copyOf(typeToCore);
    }

    static final Map<String, String> quantityToCategory =
            ImmutableMap.<String, String>builder()
                    .put("acceleration", "acceleration")
                    .put("angle", "angle")
                    .put("area", "area")
                    .put("catalytic-activity", "concentr")
                    .put("concentration", "concentr")
                    .put("concentration-mass", "concentr")
                    .put("consumption", "consumption")
                    .put("consumption-inverse", "consumption")
                    .put("digital", "digital")
                    .put("duration", "duration")
                    .put("electric-capacitance", "electric")
                    .put("electric-charge", "electric")
                    .put("electric-conductance", "electric")
                    .put("electric-current", "electric")
                    .put("electric-inductance", "electric")
                    .put("electric-resistance", "electric")
                    .put("energy", "energy")
                    .put("force", "force")
                    .put("frequency", "frequency")
                    .put("graphics", "graphics")
                    .put("illuminance", "light")
                    .put("ionizing-radiation", "energy")
                    .put("length", "length")
                    .put("luminous-flux", "light")
                    .put("luminous-intensity", "light")
                    .put("magnetic-flux", "magnetic")
                    .put("magnetic-induction", "magnetic")
                    .put("mass", "mass")
                    .put("portion", "concentr")
                    .put("power", "power")
                    .put("pressure", "pressure")
                    .put("pressure-per-length", "pressure")
                    .put("radioactivity", "energy")
                    .put("resolution", "graphics")
                    .put("solid-angle", "angle")
                    .put("speed", "speed")
                    .put("substance-amount", "concentr")
                    .put("temperature", "temperature")
                    .put("typewidth", "graphics")
                    .put("voltage", "electric")
                    .put("volume", "volume")
                    .put("year-duration", "duration")
                    .build();

    // TODO Get rid of these exceptions.
    // Some of the qualities are 'split' over categories, which ideally shouldn't happen.
    static final Map<String, String> CATEGORY_EXCEPTIONS =
            ImmutableMap.<String, String>builder()
                    .put("dalton", "mass")
                    .put("newton-meter", "torque")
                    .put("pound-force-foot", "torque")
                    .put("solar-luminosity", "light")
                    .build();

    public void TestUnitCategory() {
        Map<String, Multimap<String, String>> bad = new TreeMap<>();
        for (Entry<String, String> entry : TYPE_TO_CORE.entries()) {
            final String coreUnit = entry.getValue();
            final String unitType = entry.getKey();
            if (NOT_CONVERTABLE.contains(coreUnit)) {
                continue;
            }
            String quantity = converter.getQuantityFromUnit(coreUnit, false);
            if (quantity == null) {
                converter.getQuantityFromUnit(coreUnit, true);
                errln("Null quantity " + coreUnit);
            } else {
                String exception = CATEGORY_EXCEPTIONS.get(coreUnit);
                if (unitType.equals(exception)) {
                    continue;
                }
                assertEquals(
                        "Category for «" + coreUnit + "» with quality «" + quantity + "»",
                        unitType,
                        quantityToCategory.get(quantity));
            }
        }
    }

    public void TestQuantities() {
        // put quantities in order
        Multimap<String, String> quantityToBaseUnits = LinkedHashMultimap.create();

        Multimaps.invertFrom(Multimaps.forMap(BASE_UNIT_TO_QUANTITY), quantityToBaseUnits);

        for (Entry<String, Collection<String>> entry : quantityToBaseUnits.asMap().entrySet()) {
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

        if (SHOW_UNIT_CATEGORY) {
            System.out.println();
            for (Entry<String, String> entry : BASE_UNIT_TO_QUANTITY.entrySet()) {
                String baseUnit = entry.getKey();
                String quantity = entry.getValue();
                System.out.println(
                        "        <unitQuantity"
                                + " baseUnit='"
                                + baseUnit
                                + "'"
                                + " quantity='"
                                + quantity
                                + "'"
                                + "/>");
            }
            System.out.println();
            System.out.println("Quantities");
            for (Entry<String, Collection<String>> entry :
                    quantityToConvertible.asMap().entrySet()) {
                String quantity = entry.getKey();
                Collection<String> convertible = entry.getValue();
                System.out.println(quantity + "\t" + convertible);
            }
        }
    }

    static final UnicodeSet ALLOWED_IN_COMPONENT = new UnicodeSet("[a-z0-9]").freeze();
    static final Set<String> STILL_RECOGNIZED_SIMPLES =
            ImmutableSet.of("em", "g-force", "therm-us", "british-thermal-unit-it", "calorie-it");

    public void TestOrder() {
        if (SHOW_UNIT_ORDER) System.out.println();
        for (String s : UnitConverter.BASE_UNITS) {
            String quantity = converter.getQuantityFromBaseUnit(s);
            if (SHOW_UNIT_ORDER) {
                System.out.println("\"" + quantity + "\",");
            }
        }
        for (String unit : CORE_TO_TYPE.keySet()) {
            if (!STILL_RECOGNIZED_SIMPLES.contains(unit)) {
                for (String part : unit.split("-")) {
                    assertTrue(unit + " has no parts < 2 in length", part.length() > 2);
                    assertTrue(
                            unit + " has only allowed characters",
                            ALLOWED_IN_COMPONENT.containsAll(part));
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
        Multimap<TargetInfo, String> sorted =
                TreeMultimap.create(converter.targetInfoComparator, Comparator.naturalOrder());
        Multimaps.invertFrom(Multimaps.forMap(data), sorted);

        String lastBase = "";

        // Test that sorted is in same order as the file.
        MapComparator<String> conversionOrder = new MapComparator<>(data.keySet());
        String lastUnit = null;
        for (Entry<TargetInfo, String> entry : sorted.entries()) {
            final TargetInfo tInfo = entry.getKey();
            final String unit = entry.getValue();
            if (lastUnit != null) {
                if (!(conversionOrder.compare(lastUnit, unit) < 0)) {
                    Output<String> metricUnit = new Output<>();
                    ConversionInfo lastInfo = converter.parseUnitId(lastUnit, metricUnit, false);
                    String lastMetric = metricUnit.value;
                    ConversionInfo info = converter.parseUnitId(unit, metricUnit, false);
                    String metric = metricUnit.value;
                    if (metric.equals(lastMetric)) {
                        warnln(
                                "Expected "
                                        + lastUnit
                                        + " < "
                                        + unit
                                        + "\t"
                                        + lastMetric
                                        + " "
                                        + lastInfo
                                        + " < "
                                        + metric
                                        + " "
                                        + info);
                    }
                }
            }
            lastUnit = unit;
            if (SHOW_UNIT_ORDER) {
                if (!lastBase.equals(tInfo.target)) {
                    lastBase = tInfo.target;
                    System.out.println(
                            "\n      <!-- " + converter.getQuantityFromBaseUnit(lastBase) + " -->");
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
        Continuation fluid_imperial =
                new Continuation(Arrays.asList("fluid", "imperial"), "fluid-ounce-imperial");
        final int fvfl = fluid.compareTo(fluid_imperial);
        assertTrue(fluid + " vs " + fluid_imperial, fvfl > 0);
        assertTrue(fluid_imperial + " vs " + fluid, fluid_imperial.compareTo(fluid) < 0);
    }

    private static final Pattern usSystemPattern =
            Pattern.compile(
                    "\\b(lb_to_kg|ft_to_m|ft2_to_m2|ft3_to_m3|in3_to_m3|gal_to_m3|cup_to_m3)\\b");
    private static final Pattern ukSystemPattern =
            Pattern.compile("\\b(lb_to_kg|ft_to_m|ft2_to_m2|ft3_to_m3|in3_to_m3|gal_imp_to_m3)\\b");

    static final Set<String> OK_BOTH =
            ImmutableSet.of(
                    "ounce-troy",
                    "nautical-mile",
                    "fahrenheit",
                    "inch-ofhg",
                    "british-thermal-unit",
                    "foodcalorie",
                    "knot");

    static final Set<String> OK_US = ImmutableSet.of("therm-us", "bushel");
    static final Set<String> NOT_US = ImmutableSet.of("stone");

    static final Set<String> OK_UK = ImmutableSet.of();
    static final Set<String> NOT_UK = ImmutableSet.of("therm-us", "bushel", "barrel");

    public static final Set<String> OTHER_SYSTEM =
            ImmutableSet.of(
                    "g-force",
                    "dalton",
                    "calorie",
                    "earth-radius",
                    "solar-radius",
                    "solar-radius",
                    "astronomical-unit",
                    "light-year",
                    "parsec",
                    "earth-mass",
                    "solar-mass",
                    "bit",
                    "byte",
                    "karat",
                    "solar-luminosity",
                    "ofhg",
                    "atmosphere",
                    "pixel",
                    "dot",
                    "permillion",
                    "permyriad",
                    "permille",
                    "percent",
                    "karat",
                    "portion",
                    "minute",
                    "hour",
                    "day",
                    "day-person",
                    "week",
                    "week-person",
                    "year",
                    "year-person",
                    "decade",
                    "month",
                    "month-person",
                    "century",
                    "quarter",
                    "arc-second",
                    "arc-minute",
                    "degree",
                    "radian",
                    "revolution",
                    "electronvolt",
                    // quasi-metric
                    "dunam",
                    "mile-scandinavian",
                    "carat",
                    "cup-metric",
                    "pint-metric");

    public void TestSystems() {
        final Logger logger = getLogger();
        //        Map<String, TargetInfo> data = converter.getInternalConversionData();
        Output<String> metricUnit = new Output<>();
        Multimap<Set<UnitSystem>, R3<String, ConversionInfo, String>> systemsToUnits =
                TreeMultimap.create(
                        Comparators.lexicographical(Ordering.natural()), Ordering.natural());
        for (String longUnit : VALID_REGULAR_UNITS) {
            String unit = Units.getShort(longUnit);
            if (NOT_CONVERTABLE.contains(unit)) {
                continue;
            }
            if (unit.contentEquals("centiliter")) {
                int debug = 0;
            }
            Set<UnitSystem> systems = converter.getSystemsEnum(unit);
            ConversionInfo parseInfo = converter.parseUnitId(unit, metricUnit, false);
            String mUnit = metricUnit.value;
            final R3<String, ConversionInfo, String> row = Row.of(mUnit, parseInfo, unit);
            systemsToUnits.put(systems, row);
            //            if (systems.isEmpty()) {
            //                Rational factor = parseInfo.factor;
            //                if (factor.isPowerOfTen()) {
            //                    log("System should be 'metric': " + unit);
            //                } else {
            //                    log("System should be ???: " + unit);
            //                }
            //            }
        }
        String std = converter.getStandardUnit("kilogram-meter-per-square-meter-square-second");
        logger.fine("");
        Output<Rational> outFactor = new Output<>();
        for (Entry<Set<UnitSystem>, Collection<R3<String, ConversionInfo, String>>>
                systemsAndUnits : systemsToUnits.asMap().entrySet()) {
            Set<UnitSystem> systems = systemsAndUnits.getKey();
            for (R3<String, ConversionInfo, String> unitInfo : systemsAndUnits.getValue()) {
                String unit = unitInfo.get2();
                switch (unit) {
                    case "gram":
                        continue;
                    case "kilogram":
                        break;
                    default:
                        String paredUnit = UnitConverter.stripPrefix(unit, outFactor);
                        if (!paredUnit.equals(unit)) {
                            continue;
                        }
                }
                final String metric = unitInfo.get0();
                String standard = converter.getStandardUnit(metric);
                final String quantity = converter.getQuantityFromUnit(unit, false);
                final Rational factor = unitInfo.get1().factor;
                // show non-metric relations
                String specialRef = "";
                String specialUnit = converter.getSpecialBaseUnit(quantity, systems);
                if (specialUnit != null) {
                    Rational specialFactor =
                            converter.convert(Rational.ONE, unit, specialUnit, false);
                    specialRef = "\t" + specialFactor + "\t" + specialUnit;
                }
                logger.fine(
                        systems
                                + "\t"
                                + quantity
                                + "\t"
                                + unit
                                + "\t"
                                + factor
                                + "\t"
                                + standard
                                + specialRef);
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
        lines.forEach(
                line -> {
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
                    double actual =
                            unitInfo.convert(R1000)
                                    .toBigDecimal(MathContext.DECIMAL32)
                                    .doubleValue();
                    assertEquals(Joiner.on(" ; ").join(fields), expected, actual);
                });
        lines.close();
    }

    public void TestSpecialCases() {
        String[][] tests = {
            {"1", "millimole-per-liter", "milligram-ofglucose-per-deciliter", "18.01557"},
            {"1", "millimole-per-liter", "item-per-cubic-meter", "602214076000000000000000"},
            {"50", "foot", "xxx", "0/0"},
            {"50", "xxx", "mile", "0/0"},
            {"50", "foot", "second", "0/0"},
            {"50", "foot-per-xxx", "mile-per-hour", "0/0"},
            {"50", "foot-per-minute", "mile", "0/0"},
            {"50", "foot-per-ampere", "mile-per-hour", "0/0"},
            {"50", "foot", "mile", "5 / 528"},
            {"50", "foot-per-minute", "mile-per-hour", "25 / 44"},
            {"50", "foot-per-minute", "hour-per-mile", "44 / 25"},
            {"50", "mile-per-gallon", "liter-per-100-kilometer", "112903 / 24000"},
            {"50", "celsius-per-second", "kelvin-per-second", "50"},
            {"50", "celsius-per-second", "fahrenheit-per-second", "90"},
            {
                "50",
                "pound-force",
                "kilogram-meter-per-square-second",
                "8896443230521 / 40000000000"
            },
            // Note: pound-foot-per-square-second is a pound-force divided by gravity
            {
                "50",
                "pound-foot-per-square-second",
                "kilogram-meter-per-square-second",
                "17281869297 / 2500000000"
            },
        };
        int count = 0;
        for (String[] test : tests) {
            final Rational sourceValue = Rational.of(test[0]);
            final String sourceUnit = test[1];
            final String targetUnit = test[2];
            final Rational expectedValue = Rational.of(test[3]);
            final Rational conversion =
                    converter.convert(sourceValue, sourceUnit, targetUnit, SHOW_DATA);
            if (!assertEquals(
                    count++ + ") " + sourceValue + " " + sourceUnit + " ⟹ " + targetUnit,
                    expectedValue,
                    conversion)) {
                converter.convert(sourceValue, sourceUnit, targetUnit, SHOW_DATA);
            }
        }
    }

    static Multimap<String, String> EXTRA_UNITS =
            ImmutableMultimap.<String, String>builder()
                    .putAll("area", "square-foot", "square-yard", "square-mile")
                    .putAll("volume", "cubic-inch", "cubic-foot", "cubic-yard")
                    .build();

    public void TestEnglishSystems() {
        Multimap<String, String> systemToUnits = TreeMultimap.create();
        for (String unit : converter.canConvert()) {
            Set<String> systems = converter.getSystems(unit);
            if (systems.isEmpty()) {
                systemToUnits.put("other", unit);
            } else
                for (String s : systems) {
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
        Multimap<String, String> quantityToUnits = TreeMultimap.create();
        boolean metric = system.equals("metric");
        for (String unit : units) {
            quantityToUnits.put(converter.getQuantityFromUnit(unit, false), unit);
        }
        for (Entry<String, Collection<String>> entry : quantityToUnits.asMap().entrySet()) {
            String quantity = entry.getKey();
            String baseUnit = converter.getBaseUnitToQuantity().inverse().get(quantity);
            Multimap<Rational, String> sorted = TreeMultimap.create();
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

            if (SHOW_DATA) {
                printUnits(system, quantity, comparableUnits);
            }
        }
    }

    private void addUnit(
            String baseUnit, String englishBaseUnit, Multimap<Rational, String> sorted) {
        Rational value = converter.convert(Rational.ONE, englishBaseUnit, baseUnit, false);
        sorted.put(value, englishBaseUnit);
    }

    private void printUnits(String system, String quantity, Set<String> comparableUnits) {
        System.out.print("\n" + system + "\t" + quantity);
        for (String targetUnit : comparableUnits) {
            System.out.print("\t" + targetUnit);
        }
        System.out.println();
        for (String sourceUnit : comparableUnits) {
            System.out.print("\t" + sourceUnit);
            for (String targetUnit : comparableUnits) {
                Rational rational = converter.convert(Rational.ONE, sourceUnit, targetUnit, false);
                System.out.print("\t" + rational.toBigDecimal(MathContext.DECIMAL64).doubleValue());
            }
            System.out.println();
        }
    }

    private String getEnglishBaseUnit(String baseUnit) {
        return baseUnit.replace("kilogram", "pound").replace("meter", "foot");
    }

    public void TestPI() {
        Rational PI = converter.getConstants().get("PI");
        double PID = PI.toBigDecimal(MathContext.DECIMAL128).doubleValue();
        final BigDecimal bigPi =
                new BigDecimal("3.141592653589793238462643383279502884197169399375105820974944");
        double bigPiD = bigPi.doubleValue();
        assertEquals("pi accurate enough", bigPiD, PID);

        // also test continued fractions used in deriving values

        Object[][] tests0 = {
            {
                new ContinuedFraction(0, 1, 5, 2, 2),
                Rational.of(27, 32),
                ImmutableList.of(
                        Rational.of(0), Rational.of(1), Rational.of(5, 6), Rational.of(11, 13))
            },
        };
        for (Object[] test : tests0) {
            ContinuedFraction source = (ContinuedFraction) test[0];
            Rational expected = (Rational) test[1];
            @SuppressWarnings("unchecked")
            List<Rational> expectedIntermediates = (List<Rational>) test[2];
            List<Rational> intermediates = new ArrayList<>();
            final Rational actual = source.toRational(intermediates);
            assertEquals("continued", expected, actual);
            assertEquals("continued", expectedIntermediates, intermediates);
        }
        Object[][] tests = {
            {Rational.of(3245, 1000), new ContinuedFraction(3, 4, 12, 4)},
            {Rational.of(39, 10), new ContinuedFraction(3, 1, 9)},
            {Rational.of(-3245, 1000), new ContinuedFraction(-4, 1, 3, 12, 4)},
        };
        for (Object[] test : tests) {
            Rational source = (Rational) test[0];
            ContinuedFraction expected = (ContinuedFraction) test[1];
            ContinuedFraction actual = new ContinuedFraction(source);
            assertEquals(source.toString(), expected, actual);
            assertEquals(actual.toString(), source, actual.toRational(null));
        }

        if (SHOW_DATA) {
            ContinuedFraction actual = new ContinuedFraction(Rational.of(bigPi));
            List<Rational> intermediates = new ArrayList<>();
            actual.toRational(intermediates);
            System.out.println("\nRational\tdec64\tdec128\tgood enough");
            System.out.println(
                    "Target\t"
                            + bigPi.round(MathContext.DECIMAL64)
                            + "x"
                            + "\t"
                            + bigPi.round(MathContext.DECIMAL128)
                            + "x"
                            + "\t"
                            + "delta");
            int goodCount = 0;
            for (Rational item : intermediates) {
                final BigDecimal dec64 = item.toBigDecimal(MathContext.DECIMAL64);
                final BigDecimal dec128 = item.toBigDecimal(MathContext.DECIMAL128);
                final boolean goodEnough =
                        bigPiD == item.toBigDecimal(MathContext.DECIMAL128).doubleValue();
                System.out.println(
                        item
                                + "\t"
                                + dec64
                                + "x\t"
                                + dec128
                                + "x\t"
                                + goodEnough
                                + "\t"
                                + item.toBigDecimal(MathContext.DECIMAL128).subtract(bigPi));
                if (goodEnough && goodCount++ > 6) {
                    break;
                }
            }
        }
    }

    public void TestUnitPreferenceSource() {
        XMLSource xmlSource = new SimpleXMLSource("units");
        xmlSource.setNonInheriting(true);
        CLDRFile foo = new CLDRFile(xmlSource);
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
        if (SHOW_PREFS) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
            foo.write(out);
            out.flush();
        } else {
            warnln("Use  -DTestUnits:SHOW_PREFS to get the reformatted source");
        }
    }

    static final Joiner JOIN_SPACE = Joiner.on(' ');

    private void checkUnitPreferences(UnitPreferences uprefs) {
        Set<String> usages = new LinkedHashSet<>();
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry1 :
                uprefs.getData().entrySet()) {
            String quantity = entry1.getKey();

            // Each of the quantities is valid.
            assertNotNull("quantity is convertible", converter.getBaseUnitFromQuantity(quantity));

            Map<String, Multimap<Set<String>, UnitPreference>> usageToRegionToUnitPreference =
                    entry1.getValue();

            // each of the quantities has a default usage
            assertTrue(
                    "Quantity " + quantity + " contains default usage",
                    usageToRegionToUnitPreference.containsKey("default"));

            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 :
                    usageToRegionToUnitPreference.entrySet()) {
                String usage = entry2.getKey();
                final String quantityPlusUsage = quantity + "/" + usage;
                Multimap<Set<String>, UnitPreference> regionsToUnitPreference = entry2.getValue();
                usages.add(usage);
                Set<Set<String>> regionSets = regionsToUnitPreference.keySet();

                // all quantity + usage pairs must contain 001 (one exception)
                assertTrue(
                        "For "
                                + quantityPlusUsage
                                + ", the set of sets of regions must contain 001",
                        regionSets.contains(WORLD_SET)
                                || quantityPlusUsage.contentEquals("concentration/blood-glucose"));

                // Check that regions don't overlap for same quantity/usage
                Multimap<String, Set<String>> checkOverlap = LinkedHashMultimap.create();
                for (Set<String> regionSet : regionsToUnitPreference.keySet()) {
                    for (String region : regionSet) {
                        checkOverlap.put(region, regionSet);
                    }
                }
                for (Entry<String, Collection<Set<String>>> entry :
                        checkOverlap.asMap().entrySet()) {
                    assertEquals(
                            quantityPlusUsage
                                    + ": regions must be in only one set: "
                                    + entry.getValue(),
                            1,
                            entry.getValue().size());
                }

                Set<String> systems = new TreeSet<>();
                for (Entry<Set<String>, Collection<UnitPreference>> entry :
                        regionsToUnitPreference.asMap().entrySet()) {
                    Collection<UnitPreference> uPrefs = entry.getValue();
                    Set<String> regions = entry.getKey();

                    // reset these for every new set of regions
                    Rational lastSize = null;
                    String lastUnit = null;
                    Rational lastgeq = null;
                    systems.clear();
                    Set<String> lastRegions = null;
                    String unitQuantity = null;

                    preferences:
                    for (UnitPreference up : uPrefs) {
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
                                    throw new IllegalArgumentException(
                                            "Bad mixed unit; biggest unit must be first: "
                                                    + up.unit);
                                }
                                if (!lastQuantity.contentEquals(quantity)) {
                                    throw new IllegalArgumentException(
                                            "Inconsistent quantities for mixed unit: " + up.unit);
                                }
                            }
                            lastValue = value;
                            lastQuantity = quantity;
                            systems.addAll(converter.getSystems(unit));
                        }
                        String baseUnit = converter.getBaseUnitFromQuantity(unitQuantity);
                        Rational size = converter.convert(up.geq, topUnit, baseUnit, false);
                        if (lastSize != null) { // ensure descending order
                            if (!assertTrue(
                                    "Successive items must be ≥ previous:\n\t"
                                            + quantityPlusUsage
                                            + "; unit: "
                                            + up.unit
                                            + "; size: "
                                            + size
                                            + "; regions: "
                                            + regions
                                            + "; lastUnit: "
                                            + lastUnit
                                            + "; lastSize: "
                                            + lastSize
                                            + "; lastRegions: "
                                            + lastRegions,
                                    size.compareTo(lastSize) <= 0)) {
                                int debug = 0;
                            }
                        }
                        lastSize = size;
                        lastUnit = up.unit;
                        lastgeq = geq;
                        lastRegions = regions;
                        if (SHOW_DATA)
                            System.out.println(
                                    quantity
                                            + "\t"
                                            + usage
                                            + "\t"
                                            + regions
                                            + "\t"
                                            + up.geq
                                            + "\t"
                                            + up.unit
                                            + "\t"
                                            + up.skeleton);
                    }
                    // Check that last geq is ONE.
                    assertEquals(
                            usage
                                    + " + "
                                    + regions
                                    + ": the least unit must have geq=1 (or equivalently, no geq)",
                            Rational.ONE,
                            lastgeq);

                    // Check that each set has a consistent system.
                    assertTrue(
                            usage
                                    + " + "
                                    + regions
                                    + " has mixed systems: "
                                    + systems
                                    + "\n\t"
                                    + uPrefs,
                            areConsistent(systems, unitQuantity));
                }
            }
        }
    }

    private boolean areConsistent(Set<String> systems, String unitQuantity) {
        return unitQuantity.equals("duration")
                || !(systems.contains("metric")
                        && (systems.contains("ussystem") || systems.contains("uksystem")));
    }

    public void TestBcp47() {
        checkBcp47("Quantity", converter.getQuantities(), lowercaseAZ, false);
        checkBcp47("Usage", SDI.getUnitPreferences().getUsages(), lowercaseAZ09, true);
        checkBcp47("Unit", converter.getSimpleUnits(), lowercaseAZ09, true);
    }

    private void checkBcp47(
            String identifierType,
            Set<String> identifiers,
            UnicodeSet allowed,
            boolean allowHyphens) {
        Output<Integer> counter = new Output<>(0);
        Multimap<String, String> truncatedToFullIdentifier = TreeMultimap.create();
        final Set<String> simpleUnits = identifiers;
        for (String unit : simpleUnits) {
            if (!allowHyphens && unit.contains("-")) {
                truncatedToFullIdentifier.put(unit, "-");
            }
            checkBcp47(counter, identifierType, unit, allowed, truncatedToFullIdentifier);
        }
        for (Entry<String, Collection<String>> entry :
                truncatedToFullIdentifier.asMap().entrySet()) {
            Set<String> identifierSet = ImmutableSet.copyOf(entry.getValue());
            assertEquals(
                    identifierType + ": truncated identifier " + entry.getKey() + " must be unique",
                    ImmutableSet.of(identifierSet.iterator().next()),
                    identifierSet);
        }
    }

    private static int MIN_SUBTAG_LENGTH = 3;
    private static int MAX_SUBTAG_LENGTH = 8;

    static final UnicodeSet lowercaseAZ = new UnicodeSet("[a-z]").freeze();
    static final UnicodeSet lowercaseAZ09 = new UnicodeSet("[a-z0-9]").freeze();

    private void checkBcp47(
            Output<Integer> counter,
            String title,
            String identifier,
            UnicodeSet allowed,
            Multimap<String, String> truncatedToFullIdentifier) {
        StringBuilder shortIdentifer = new StringBuilder();
        boolean fail = false;
        for (String subtag : identifier.split("-")) {
            assertTrue(
                    ++counter.value
                            + ") "
                            + title
                            + " identifier="
                            + identifier
                            + " subtag="
                            + subtag
                            + " has right characters",
                    allowed.containsAll(subtag));
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
        warnln(
                "If this fails, check the output of TestUnitPreferencesSource (with -DTestUnits:SHOW_DATA), fix as needed, then incorporate.");
        UnitPreferences prefs = SDI.getUnitPreferences();
        checkUnitPreferences(prefs);
        //        Map<String, Map<String, Map<String, UnitPreference>>> fastMap =
        // prefs.getFastMap(converter);
        //        for (Entry<String, Map<String, Map<String, UnitPreference>>> entry :
        // fastMap.entrySet()) {
        //            String quantity = entry.getKey();
        //            String baseUnit = converter.getBaseUnitFromQuantity(quantity);
        //            for (Entry<String, Map<String, UnitPreference>> entry2 :
        // entry.getValue().entrySet()) {
        //                String usage = entry2.getKey();
        //                for (Entry<String, UnitPreference> entry3 : entry2.getValue().entrySet())
        // {
        //                    String region = entry3.getKey();
        //                    UnitPreference pref = entry3.getValue();
        //                    System.out.println(quantity + "\t" + usage + "\t" + region + "\t" +
        // pref.toString(baseUnit));
        //                }
        //            }
        //        }
        prefs.getFastMap(converter); // call just to make sure we don't get an exception

        if (GENERATE_TESTS) {
            try (TempPrintWriter pw =
                    TempPrintWriter.openUTF8Writer(
                            CLDRPaths.TEST_DATA + "units", "unitPreferencesTest.txt")) {

                pw.println(
                        "\n# Test data for unit preferences\n"
                                + CldrUtility.getCopyrightString("#  ")
                                + "\n"
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
                                + "# Generation: Set GENERATE_TESTS in TestUnits.java, and look at TestUnitPreferences results.\n");
                Rational ONE_TENTH = Rational.of(1, 10);

                // Note that for production usage, precomputed data like the
                // prefs.getFastMap(converter) would be used instead of the raw data.

                for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry :
                        prefs.getData().entrySet()) {
                    String quantity = entry.getKey();
                    String baseUnit = converter.getBaseUnitFromQuantity(quantity);
                    for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 :
                            entry.getValue().entrySet()) {
                        String usage = entry2.getKey();

                        // collect samples of base units
                        for (Entry<Set<String>, Collection<UnitPreference>> entry3 :
                                entry2.getValue().asMap().entrySet()) {
                            boolean first = true;
                            Set<Rational> samples = new TreeSet<>(Comparator.reverseOrder());
                            for (UnitPreference pref : entry3.getValue()) {
                                final String topUnit =
                                        UnitPreferences.SPLIT_AND
                                                .split(pref.unit)
                                                .iterator()
                                                .next();
                                if (first) {
                                    samples.add(
                                            converter.convert(
                                                    pref.geq.add(ONE_TENTH),
                                                    topUnit,
                                                    baseUnit,
                                                    false));
                                    first = false;
                                }
                                samples.add(converter.convert(pref.geq, topUnit, baseUnit, false));
                                samples.add(
                                        converter.convert(
                                                pref.geq.subtract(ONE_TENTH),
                                                topUnit,
                                                baseUnit,
                                                false));
                            }
                            // show samples
                            Set<String> regions = entry3.getKey();
                            String sampleRegion = regions.iterator().next();
                            Collection<UnitPreference> uprefs = entry3.getValue();
                            for (Rational sample : samples) {
                                showSample(
                                        quantity,
                                        usage,
                                        sampleRegion,
                                        sample,
                                        baseUnit,
                                        uprefs,
                                        pw);
                            }
                            pw.println();
                        }
                    }
                }
            }
        }
    }

    private void showSample(
            String quantity,
            String usage,
            String sampleRegion,
            Rational sampleBaseValue,
            String baseUnit,
            Collection<UnitPreference> prefs,
            TempPrintWriter pw) {
        String lastUnit = null;
        boolean gotOne = false;
        for (UnitPreference pref : prefs) {
            final String topUnit = UnitPreferences.SPLIT_AND.split(pref.unit).iterator().next();
            Rational baseGeq = converter.convert(pref.geq, topUnit, baseUnit, false);
            if (sampleBaseValue.compareTo(baseGeq) >= 0) {
                showSample2(
                        quantity, usage, sampleRegion, sampleBaseValue, baseUnit, pref.unit, pw);
                gotOne = true;
                break;
            }
            lastUnit = pref.unit;
        }
        if (!gotOne) {
            showSample2(quantity, usage, sampleRegion, sampleBaseValue, baseUnit, lastUnit, pw);
        }
    }

    private void showSample2(
            String quantity,
            String usage,
            String sampleRegion,
            Rational sampleBaseValue,
            String baseUnit,
            String lastUnit,
            TempPrintWriter pw) {
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
                sampleBaseValue =
                        converter.convert(
                                sample.subtract(Rational.of(floor)), unit, baseUnit, false);
            } else {
                formattedUnit.append(sample + TEST_SEP + sample.doubleValue() + TEST_SEP + unit);
            }
        }
        pw.println(
                quantity
                        + TEST_SEP
                        + usage
                        + TEST_SEP
                        + sampleRegion
                        + TEST_SEP
                        + originalSampleBaseValue
                        + TEST_SEP
                        + originalSampleBaseValue.doubleValue()
                        + TEST_SEP
                        + baseUnit
                        + TEST_SEP
                        + formattedUnit);
    }

    public void TestWithExternalData() throws IOException {

        Multimap<String, ExternalUnitConversionData> seen = HashMultimap.create();
        Set<ExternalUnitConversionData> cantConvert = new LinkedHashSet<>();
        Map<ExternalUnitConversionData, Rational> convertDiff = new LinkedHashMap<>();
        Set<String> remainingCldrUnits =
                new LinkedHashSet<>(converter.getInternalConversionData().keySet());
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
                if (symmetricDiff.abs().compareTo(Rational.of(1, 1000000)) > 0) {
                    convertDiff.put(data, cldrResult);
                } else {
                    remainingCldrUnits.remove(data.source);
                    remainingCldrUnits.remove(data.target);
                    if (SHOW_DATA)
                        System.out.println(
                                "*Converted"
                                        + "\t"
                                        + cldrResult.doubleValue()
                                        + "\t"
                                        + externalResult.doubleValue()
                                        + "\t"
                                        + symmetricDiff.doubleValue()
                                        + "\t"
                                        + data);
                }
            }
        }

        // get additional data on derived units
        //        for (Entry<String, TargetInfo> e : NistUnits.derivedUnitToConversion.entrySet()) {
        //            String sourceUnit = e.getKey();
        //            TargetInfo targetInfo = e.getValue();
        //
        //            Rational conversion = converter.convert(Rational.ONE, sourceUnit,
        // targetInfo.target, false);
        //            if (conversion.equals(Rational.NaN)) {
        //                couldAdd.add(new ExternalUnitConversionData("", sourceUnit,
        // targetInfo.target, conversion, "?", null));
        //            }
        //        }
        if (SHOW_DATA) {
            for (Entry<String, Collection<String>> e :
                    NistUnits.unitToQuantity.asMap().entrySet()) {
                System.out.println("*Quantities:" + "\t" + e.getKey() + "\t" + e.getValue());
            }
        }

        // check for missing external data
        int unitsWithoutExternalCheck = 0;
        for (String remainingUnit : remainingCldrUnits) {
            final TargetInfo targetInfo = converter.getInternalConversionData().get(remainingUnit);
            if (!targetInfo.target.contentEquals(remainingUnit)) {
                if (SHOW_DATA) {
                    printlnIfZero(unitsWithoutExternalCheck);
                    System.out.println(
                            "Not tested against external data\t"
                                    + remainingUnit
                                    + "\t"
                                    + targetInfo);
                }
                unitsWithoutExternalCheck++;
            }
        }
        if (unitsWithoutExternalCheck != 0 && !SHOW_DATA) {
            warnln(
                    unitsWithoutExternalCheck
                            + " units without external data verification.  Use -DTestUnits:SHOW_DATA for details.");
        }

        boolean showDiagnostics = false;
        for (Entry<String, Collection<ExternalUnitConversionData>> entry :
                seen.asMap().entrySet()) {
            if (entry.getValue().size() != 1) {
                Multimap<ConversionInfo, ExternalUnitConversionData> factors =
                        HashMultimap.create();
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

                errln(
                        "*DIFF CONVERT:"
                                + "\t"
                                + external.source
                                + "\t⟹\t"
                                + external.target
                                + "\texpected\t"
                                + externalResult.doubleValue()
                                + "\tactual:\t"
                                + computed.doubleValue()
                                + "\tsdiff:\t"
                                + computed.symmetricDiff(externalResult).abs().doubleValue()
                                + "\txdata:\t"
                                + external);
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

            showDelta(
                    "fahrenheit-hour-square-foot-per-british-thermal-unit-inch",
                    "meter-kelvin-per-watt",
                    true);
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
                if (SHOW_DATA)
                    System.out.println(
                            "*CHANGES\t" + e.getKey() + "\t" + Joiner.on('\t').join(e.getValue()));
            }
        }

        if (showDiagnostics && cantConvert.size() > 0) {
            System.out.println();
            for (ExternalUnitConversionData e : cantConvert) {
                System.out.println("*CANT CONVERT-" + e);
            }
        }
        Output<String> baseUnit = new Output<>();
        for (ExternalUnitConversionData s : couldAdd) {
            String target = s.target;
            Rational endFactor = s.info.factor;
            String mark = "";
            TargetInfo baseUnit2 = NistUnits.derivedUnitToConversion.get(s.target);
            if (baseUnit2 != null) {
                target = baseUnit2.target;
                endFactor = baseUnit2.unitInfo.factor;
                mark = "¹";
            } else {
                ConversionInfo conversionInfo = converter.getUnitInfo(s.target, baseUnit);
                if (conversionInfo != null && !s.target.equals(baseUnit.value)) {
                    target = baseUnit.value;
                    endFactor = conversionInfo.convert(s.info.factor);
                    mark = "²";
                }
            }
            //            if (SHOW_DATA)
            //                System.out.println(
            //                    "Could add 10^X conversion from a"
            //                        + "\t"
            //                        + s.source
            //                        + "\tto"
            //                        + mark
            //                        + "\t"
            //                        + endFactor.toString(FormatStyle.simple)
            //                        + "\t"
            //                        + target);
        }
        warnln("Use GenerateNewUnits.java to show units we could add from NIST.");
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
        for (Entry<String, TargetInfo> unitAndInfo :
                converter.getInternalConversionData().entrySet()) {
            final TargetInfo targetInfo2 = unitAndInfo.getValue();
            ConversionInfo targetInfo = targetInfo2.unitInfo;
            checkFormat(targetInfo.factor, seen);
            if (SHOW_DATA) {
                String rFormat = targetInfo.factor.toString(FormatStyle.repeating);
                String sFormat = targetInfo.factor.toString(FormatStyle.simple);
                if (!rFormat.equals(sFormat)) {
                    System.out.println(
                            "\t\t"
                                    + unitAndInfo.getKey()
                                    + "\t"
                                    + targetInfo2.target
                                    + "\t"
                                    + sFormat
                                    + "\t"
                                    + rFormat
                                    + "\t"
                                    + targetInfo.factor.doubleValue());
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

    /** Verify that the items in the validity files match those in the units.xml files */
    public void TestValidityAgainstUnitFile() {
        Set<String> simpleUnits = converter.getSimpleUnits();
        final SetView<String> simpleUnitsRemoveAllValidity =
                Sets.difference(simpleUnits, VALID_SHORT_UNITS);
        if (!assertEquals(
                "Simple Units removeAll Validity",
                Collections.emptySet(),
                simpleUnitsRemoveAllValidity)) {
            for (String s : simpleUnitsRemoveAllValidity) {
                System.out.println(s);
            }
        }

        // aliased units
        Map<String, R2<List<String>, String>> aliasedUnits = SDI.getLocaleAliasInfo().get("unit");
        // TODO adjust
        //        final SetView<String> aliasedRemoveAllDeprecated =
        // Sets.difference(aliasedUnits.keySet(), DEPRECATED_SHORT_UNITS);
        //        if (!assertEquals("aliased Units removeAll deprecated", Collections.emptySet(),
        // aliasedRemoveAllDeprecated)) {
        //            for (String s : aliasedRemoveAllDeprecated) {
        //                System.out.println(converter.getLongId(s));
        //            }
        //        }
        assertEquals(
                "deprecated removeAll aliased Units",
                Collections.emptySet(),
                Sets.difference(DEPRECATED_SHORT_UNITS, aliasedUnits.keySet()));
    }

    /** Check that units to be translated are as expected. */
    public void testDistinguishedSetsOfUnits() {
        Set<String> comparatorUnitIds = new LinkedHashSet<>(DtdData.getUnitOrder().getOrder());
        Set<String> validLongUnitIds = VALID_REGULAR_UNITS;
        Set<String> validAndDeprecatedLongUnitIds =
                ImmutableSet.<String>builder()
                        .addAll(VALID_REGULAR_UNITS)
                        .addAll(DEPRECATED_REGULAR_UNITS)
                        .build();

        final BiMap<String, String> shortToLong = Units.LONG_TO_SHORT.inverse();
        assertSuperset(
                "converter short-long",
                "units short-long",
                converter.SHORT_TO_LONG_ID.entrySet(),
                shortToLong.entrySet());
        assertSuperset(
                "units short-long",
                "converter short-long",
                shortToLong.entrySet(),
                converter.SHORT_TO_LONG_ID.entrySet());

        Set<String> errors = new LinkedHashSet<>();
        Set<String> unitsConvertibleLongIds =
                converter.canConvert().stream()
                        .map(
                                x -> {
                                    String result = shortToLong.get(x);
                                    if (result == null) {
                                        errors.add("No short form of " + x);
                                    }
                                    return result;
                                })
                        .collect(Collectors.toSet());
        assertEquals("", Collections.emptySet(), errors);

        Set<String> simpleConvertibleLongIds =
                converter.canConvert().stream()
                        .filter(x -> converter.isSimple(x))
                        .map((String x) -> Units.LONG_TO_SHORT.inverse().get(x))
                        .collect(Collectors.toSet());
        CLDRFile root = CLDR_CONFIG.getCldrFactory().make("root", true);
        ImmutableSet<String> unitLongIdsRoot = ImmutableSet.copyOf(getUnits(root, new TreeSet<>()));
        ImmutableSet<String> unitLongIdsEnglish =
                ImmutableSet.copyOf(getUnits(CLDR_CONFIG.getEnglish(), new TreeSet<>()));

        final Set<String> longUntranslatedUnitIds =
                converter.getLongIds(UnitConverter.UNTRANSLATED_UNIT_NAMES);

        assertSameCollections(
                "root unit IDs",
                "English",
                unitLongIdsRoot,
                Sets.difference(unitLongIdsEnglish, longUntranslatedUnitIds));

        final Set<String> validRootUnitIdsMinusOddballs = unitLongIdsRoot;
        final Set<String> validLongUnitIdsMinusOddballs =
                minus(validLongUnitIds, longUntranslatedUnitIds);
        assertSuperset(
                "valid regular",
                "root unit IDs",
                validLongUnitIdsMinusOddballs,
                validRootUnitIdsMinusOddballs);

        assertSameCollections(
                "comparatorUnitIds (DtdData)",
                "valid regular&deprecated",
                comparatorUnitIds,
                validAndDeprecatedLongUnitIds);

        assertSuperset(
                "valid regular", "specials", validLongUnitIds, GrammarInfo.getUnitsToAddGrammar());

        assertSuperset(
                "root unit IDs", "specials", unitLongIdsRoot, GrammarInfo.getUnitsToAddGrammar());

        // assertSuperset("long convertible units", "valid regular", unitsConvertibleLongIds,
        // validLongUnitIds);
        Output<String> baseUnit = new Output<>();
        for (String longUnit : validLongUnitIds) {
            String shortUnit = Units.getShort(longUnit);
            if (NOT_CONVERTABLE.contains(shortUnit)) {
                continue;
            }
            ConversionInfo conversionInfo = converter.parseUnitId(shortUnit, baseUnit, false);
            if (!assertNotNull("Can convert " + longUnit, conversionInfo)) {
                converter.getUnitInfo(shortUnit, baseUnit);
                int debug = 0;
            }
        }

        assertSuperset(
                "valid regular",
                "simple convertible units",
                validLongUnitIds,
                simpleConvertibleLongIds);

        SupplementalDataInfo.getInstance().getUnitConverter();
    }

    public void assertSameCollections(
            String title1, String title2, Collection<String> c1, Collection<String> c2) {
        assertSuperset(title1, title2, c1, c2);
        assertSuperset(title2, title1, c2, c1);
    }

    public <V> void assertSuperset(
            String title1, String title2, Collection<V> c1, Collection<V> c2) {
        if (!assertEquals(title1 + " ⊇ " + title2, Collections.emptySet(), minus(c2, c1))) {
            int debug = 0;
        }
    }

    public <V> Set<V> minus(Collection<V> a, Collection<V> b) {
        Set<V> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }

    public <V> Set<V> minus(Collection<V> a, V... b) {
        Set<V> result = new LinkedHashSet<>(a);
        result.removeAll(Arrays.asList(b));
        return result;
    }

    public Set<String> getUnits(CLDRFile root, Set<String> unitLongIds) {
        for (String path : root) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            int item = parts.findElement("unit");
            if (item == -1) {
                continue;
            }
            String type = parts.getAttributeValue(item, "type");
            unitLongIds.add(type);
            // "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + unit + "\"]/gender"
        }
        return unitLongIds;
    }

    static final Pattern NORM_SPACES = Pattern.compile("[ \u00A0\u200E]");

    public void TestGender() {
        Output<String> source = new Output<>();
        Multimap<UnitPathType, String> partsUsed = TreeMultimap.create();
        Factory factory = CLDR_CONFIG.getFullCldrFactory();
        Set<String> available = factory.getAvailable();
        int bad = 0;

        for (String locale : SDI.hasGrammarInfo()) {
            // skip ones without gender info
            GrammarInfo gi = SDI.getGrammarInfo("fr");
            Collection<String> genderInfo =
                    gi.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalGender,
                            GrammaticalScope.general);
            if (genderInfo.isEmpty()) {
                continue;
            }
            if (CLDRConfig.SKIP_SEED && !available.contains(locale)) {
                continue;
            }
            // check others
            CLDRFile resolvedFile = factory.make(locale, true);
            for (Entry<String, String> entry : converter.SHORT_TO_LONG_ID.entrySet()) {
                final String shortUnitId = entry.getKey();
                final String longUnitId = entry.getValue();
                final UnitId unitId = converter.createUnitId(shortUnitId);
                partsUsed.clear();
                String rawGender =
                        UnitPathType.gender.getTrans(
                                resolvedFile, "long", shortUnitId, null, null, null, partsUsed);

                if (rawGender != null) {
                    String gender = unitId.getGender(resolvedFile, source, partsUsed);
                    if (gender != null && !shortUnitId.equals(source.value)) {
                        if (!Objects.equals(rawGender, gender)) {
                            if (SHOW_DATA) {
                                printlnIfZero(bad);
                                System.out.println(
                                        locale
                                                + ": computed gender = raw gender for\t"
                                                + shortUnitId
                                                + "\t"
                                                + Joiner.on("\n\t\t")
                                                        .join(partsUsed.asMap().entrySet()));
                            }
                            ++bad;
                        }
                    }
                }
            }
        }
        if (bad > 0) {
            warnln(
                    bad
                            + " units x locales with incorrect computed gender. Use -DTestUnits:SHOW_DATA for details.");
        }
    }

    public void TestFallbackNames() {
        String[][] sampleUnits = {
            {"fr", "square-meter", "one", "nominative", "{0} mètre carré"},
            {"fr", "square-meter", "other", "nominative", "{0} mètres carrés"},
            {"fr", "square-decimeter", "other", "nominative", "{0} décimètres carrés"},
            {"fr", "meter-per-square-second", "one", "nominative", "{0} mètre par seconde carrée"},
            {
                "fr",
                "meter-per-square-second",
                "other",
                "nominative",
                "{0} mètres par seconde carrée"
            },
            {"de", "square-meter", "other", "nominative", "{0} Quadratmeter"},
            {"de", "square-decimeter", "other", "nominative", "{0} Quadratdezimeter"}, // real fail
            {"de", "per-meter", "other", "nominative", "{0} pro Meter"},
            {"de", "per-square-meter", "other", "nominative", "{0} pro Quadratmeter"},
            {"de", "second-per-meter", "other", "nominative", "{0} Sekunden pro Meter"},
            {"de", "meter-per-second", "other", "nominative", "{0} Meter pro Sekunde"},
            {
                "de",
                "meter-per-square-second",
                "other",
                "nominative",
                "{0} Meter pro Quadratsekunde"
            },
            {
                "de",
                "gigasecond-per-decimeter",
                "other",
                "nominative",
                "{0} Gigasekunden pro Dezimeter"
            },
            {
                "de",
                "decimeter-per-gigasecond",
                "other",
                "nominative",
                "{0} Dezimeter pro Gigasekunde"
            }, // real fail
            {
                "de",
                "gigasecond-milligram-per-centimeter-decisecond",
                "other",
                "nominative",
                "{0} Milligramm⋅Gigasekunden pro Zentimeter⋅Dezisekunde"
            },
            {
                "de",
                "milligram-per-centimeter-decisecond",
                "other",
                "nominative",
                "{0} Milligramm pro Zentimeter⋅Dezisekunde"
            },
            {
                "de",
                "per-centimeter-decisecond",
                "other",
                "nominative",
                "{0} pro Zentimeter⋅Dezisekunde"
            },
            {
                "de",
                "gigasecond-milligram-per-centimeter",
                "other",
                "nominative",
                "{0} Milligramm⋅Gigasekunden pro Zentimeter"
            },
            {"de", "gigasecond-milligram", "other", "nominative", "{0} Milligramm⋅Gigasekunden"},
            {"de", "gigasecond-gram", "other", "nominative", "{0} Gramm⋅Gigasekunden"},
            {"de", "gigasecond-kilogram", "other", "nominative", "{0} Kilogramm⋅Gigasekunden"},
            {"de", "gigasecond-megagram", "other", "nominative", "{0} Megagramm⋅Gigasekunden"},
            {
                "de",
                "dessert-spoon-imperial-per-dessert-spoon-imperial",
                "one",
                "nominative",
                "{0} Imp. Dessertlöffel pro Imp. Dessertlöffel"
            },
            {
                "de",
                "dessert-spoon-imperial-per-dessert-spoon-imperial",
                "one",
                "accusative",
                "{0} Imp. Dessertlöffel pro Imp. Dessertlöffel"
            },
            {
                "de",
                "dessert-spoon-imperial-per-dessert-spoon-imperial",
                "other",
                "dative",
                "{0} Imp. Dessertlöffeln pro Imp. Dessertlöffel"
            },
            {
                "de",
                "dessert-spoon-imperial-per-dessert-spoon-imperial",
                "one",
                "genitive",
                "{0} Imp. Dessertlöffels pro Imp. Dessertlöffel"
            },

            // TODO: pick names (eg in Polish) that show differences in case.
            // {"de", "foebar-foobar-per-fiebar-faebar", "other", "genitive", null},

        };
        ImmutableMap<String, String> frOverrides =
                ImmutableMap.<String, String>builder() // insufficient data in French as yet
                        .put(
                                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                                "{0} carré") //
                        .put(
                                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                                "{0} carrés") //
                        .put(
                                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"][@gender=\"feminine\"]",
                                "{0} carrée") //
                        .put(
                                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"][@gender=\"feminine\"]",
                                "{0} carrées") //
                        .build();

        Multimap<UnitPathType, String> partsUsed = TreeMultimap.create();
        int count = 0;
        for (String[] row : sampleUnits) {
            ++count;
            final String locale = row[0];
            CLDRFile resolvedFileRaw = CLDR_CONFIG.getCLDRFile(locale, true);
            LocaleStringProvider resolvedFile;
            switch (locale) {
                case "fr":
                    resolvedFile = resolvedFileRaw.makeOverridingStringProvider(frOverrides);
                    break;
                default:
                    resolvedFile = resolvedFileRaw;
                    break;
            }

            String shortUnitId = row[1];
            String pluralCategory = row[2];
            String caseVariant = row[3];
            String expectedName = row[4];
            if (shortUnitId.equals("gigasecond-milligram")) {
                int debug = 0;
            }
            final UnitId unitId = converter.createUnitId(shortUnitId);
            final String actual =
                    unitId.toString(
                            resolvedFile, "long", pluralCategory, caseVariant, partsUsed, false);
            assertEquals(
                    count
                            + ") "
                            + Arrays.asList(row).toString()
                            + "\n\t"
                            + Joiner.on("\n\t").join(partsUsed.asMap().entrySet()),
                    fixSpaces(expectedName),
                    fixSpaces(actual));
        }
    }

    public void TestFileFallbackNames() {
        Multimap<UnitPathType, String> partsUsed = TreeMultimap.create();

        // first gather all the  examples
        Set<String> skippedUnits = new LinkedHashSet<>();
        Set<String> testSet = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr);
        Counter<String> localeToErrorCount = new Counter<>();
        main:
        for (String localeId : testSet) {
            if (localeId.contains("_")) {
                continue; // skip to make test shorter
            }
            CLDRFile resolvedFile = CLDR_CONFIG.getCLDRFile(localeId, true);
            PluralInfo pluralInfo = CLDR_CONFIG.getSupplementalDataInfo().getPlurals(localeId);
            PluralRules pluralRules = pluralInfo.getPluralRules();
            GrammarInfo grammarInfo =
                    CLDR_CONFIG.getSupplementalDataInfo().getGrammarInfo(localeId);
            Collection<String> caseVariants =
                    grammarInfo == null
                            ? null
                            : grammarInfo.get(
                                    GrammaticalTarget.nominal,
                                    GrammaticalFeature.grammaticalCase,
                                    GrammaticalScope.units);
            if (caseVariants == null || caseVariants.isEmpty()) {
                caseVariants = Collections.singleton("nominative");
            }

            for (Entry<String, String> entry : converter.SHORT_TO_LONG_ID.entrySet()) {
                final String shortUnitId = entry.getKey();
                if (converter.getComplexity(shortUnitId) == UnitComplexity.simple) {
                    continue;
                }
                if (UnitConverter.HACK_SKIP_UNIT_NAMES.contains(shortUnitId)) {
                    skippedUnits.add(shortUnitId);
                    continue;
                }
                final String longUnitId = entry.getValue();
                final UnitId unitId = converter.createUnitId(shortUnitId);
                for (String width : Arrays.asList("long")) { // , "short", "narrow"
                    for (String pluralCategory : pluralRules.getKeywords()) {
                        for (String caseVariant : caseVariants) {
                            String composedName;
                            try {
                                composedName =
                                        unitId.toString(
                                                resolvedFile,
                                                width,
                                                pluralCategory,
                                                caseVariant,
                                                partsUsed,
                                                false);
                            } catch (Exception e) {
                                composedName = "ERROR:" + e.getMessage();
                            }
                            if (composedName != null
                                    && (composedName.contains("′")
                                            || composedName.contains("″"))) { // skip special cases
                                continue;
                            }
                            partsUsed.clear();
                            String transName =
                                    UnitPathType.unit.getTrans(
                                            resolvedFile,
                                            width,
                                            shortUnitId,
                                            pluralCategory,
                                            caseVariant,
                                            null,
                                            isVerbose() ? partsUsed : null);

                            // HACK to fix different spaces around placeholder
                            if (!Objects.equals(fixSpaces(transName), fixSpaces(composedName))) {
                                logln(
                                        "\t"
                                                + localeId
                                                + "\t"
                                                + shortUnitId
                                                + "\t"
                                                + width
                                                + "\t"
                                                + pluralCategory
                                                + "\t"
                                                + caseVariant
                                                + "\texpected ≠ fallback\t«"
                                                + transName
                                                + "»\t≠\t«"
                                                + composedName
                                                + "»"
                                                + partsUsed);
                                localeToErrorCount.add(localeId, 1);
                                if (!SHOW_COMPOSE && localeToErrorCount.getTotal() > 50) {
                                    break main;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!localeToErrorCount.isEmpty()) {
            warnln(
                    "composed name ≠ translated name: ≥"
                            + localeToErrorCount.getTotal()
                            + ". Use -DTestUnits:SHOW_COMPOSE to see summary");
            if (SHOW_COMPOSE) {
                System.out.println();
                for (R2<Long, String> entry :
                        localeToErrorCount.getEntrySetSortedByCount(false, null)) {
                    System.out.println(
                            "composed name ≠ translated name: "
                                    + entry.get0()
                                    + "\t"
                                    + entry.get1());
                }
            }
        }

        if (!skippedUnits.isEmpty()) {
            warnln("Skipped unsupported units: " + skippedUnits);
        }
    }

    public String fixSpaces(String transName) {
        return transName == null ? null : NORM_SPACES.matcher(transName).replaceAll(" ");
    }

    public void TestCheckUnits() {
        CheckUnits checkUnits = new CheckUnits();
        PathHeader.Factory phf = PathHeader.getFactory();
        for (String locale : Arrays.asList("en", "fr", "de", "pl", "el")) {
            CLDRFile cldrFile = CLDR_CONFIG.getCldrFactory().make(locale, true);

            Options options = new Options();
            List<CheckStatus> possibleErrors = new ArrayList<>();
            checkUnits.setCldrFileToCheck(cldrFile, options, possibleErrors);

            for (String path :
                    StreamSupport.stream(cldrFile.spliterator(), false)
                            .sorted()
                            .collect(Collectors.toList())) {
                UnitPathType pathType =
                        UnitPathType.getPathType(XPathParts.getFrozenInstance(path));
                if (pathType == null || pathType == UnitPathType.unit) {
                    continue;
                }
                String value = cldrFile.getStringValue(path);
                checkUnits.check(path, path, value, options, possibleErrors);
                if (!possibleErrors.isEmpty()) {
                    PathHeader ph = phf.fromPath(path);
                    logln(locale + "\t" + ph.getCode() + "\t" + possibleErrors.toString());
                }
            }
        }
    }

    public void TestDerivedCase() {
        // needs further work
        if (logKnownIssue("CLDR-16395", "finish this as part of unit derivation work")) {
            return;
        }
        for (String locale : Arrays.asList("pl", "ru")) {
            CLDRFile cldrFile = CLDR_CONFIG.getCldrFactory().make(locale, true);
            GrammarInfo gi = SDI.getGrammarInfo(locale);
            Collection<String> rawCases =
                    gi.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalCase,
                            GrammaticalScope.units);

            PluralInfo plurals =
                    SupplementalDataInfo.getInstance().getPlurals(PluralType.cardinal, locale);
            Collection<Count> adjustedPlurals = plurals.getCounts();

            Output<String> sourceCase = new Output<>();
            Output<String> sourcePlural = new Output<>();

            M4<String, String, String, Boolean> myInfo =
                    ChainedMap.of(
                            new TreeMap<String, Object>(),
                            new TreeMap<String, Object>(),
                            new TreeMap<String, Object>(),
                            Boolean.class);

            int count = 0;
            for (String longUnit : GrammarInfo.getUnitsToAddGrammar()) {
                final String shortUnit = converter.getShortId(longUnit);
                String gender =
                        UnitPathType.gender.getTrans(
                                cldrFile, "long", shortUnit, null, null, null, null);

                for (String desiredCase : rawCases) {
                    // gather some general information
                    for (Count plural : adjustedPlurals) {
                        String value =
                                UnitPathType.unit.getTrans(
                                        cldrFile,
                                        "long",
                                        shortUnit,
                                        plural.toString(),
                                        desiredCase,
                                        gender,
                                        null);
                        myInfo.put(
                                gender,
                                shortUnit + "\t" + value,
                                plural.toString() + "+" + desiredCase,
                                true);
                    }

                    // do actual test
                    if (desiredCase.contentEquals("nominative")) {
                        continue;
                    }
                    for (String desiredPlural : Arrays.asList("few", "other")) {

                        String value =
                                UnitPathType.unit.getTrans(
                                        cldrFile,
                                        "long",
                                        shortUnit,
                                        desiredPlural,
                                        desiredCase,
                                        gender,
                                        null);
                        gi.getSourceCaseAndPlural(
                                locale,
                                gender,
                                value,
                                desiredCase,
                                desiredPlural,
                                sourceCase,
                                sourcePlural);
                        String sourceValue =
                                UnitPathType.unit.getTrans(
                                        cldrFile,
                                        "long",
                                        shortUnit,
                                        sourcePlural.value,
                                        sourceCase.value,
                                        gender,
                                        null);
                        assertEquals(
                                count++
                                        + ") "
                                        + locale
                                        + ",\tshort unit/gender: "
                                        + shortUnit
                                        + " / "
                                        + gender
                                        + ",\tdesired case/plural: "
                                        + desiredCase
                                        + " / "
                                        + desiredPlural
                                        + ",\tsource case/plural: "
                                        + sourceCase
                                        + " / "
                                        + sourcePlural,
                                value,
                                sourceValue);
                    }
                }
            }
            for (Entry<String, Map<String, Map<String, Boolean>>> m : myInfo) {
                for (Entry<String, Map<String, Boolean>> t : m.getValue().entrySet()) {
                    System.out.println(
                            m.getKey() + "\t" + t.getKey() + "\t" + t.getValue().keySet());
                }
            }
        }
    }

    public void TestGenderOfCompounds() {
        Set<String> skipUnits =
                ImmutableSet.of(
                        "kilocalorie",
                        "kilopascal",
                        "terabyte",
                        "gigabyte",
                        "kilobyte",
                        "gigabit",
                        "kilobit",
                        "megabit",
                        "megabyte",
                        "terabit");
        final ImmutableSet<String> keyValues =
                ImmutableSet.of("length", "mass", "duration", "power");
        int noGendersForLocales = 0;
        int localesWithNoGenders = 0;
        int localesWithSomeMissingGenders = 0;

        for (String localeID : GrammarInfo.getGrammarLocales()) {
            GrammarInfo grammarInfo = SDI.getGrammarInfo(localeID);
            if (grammarInfo == null) {
                logln("No grammar info for: " + localeID);
                continue;
            }
            UnitConverter converter = SDI.getUnitConverter();
            Collection<String> genderInfo =
                    grammarInfo.get(
                            GrammaticalTarget.nominal,
                            GrammaticalFeature.grammaticalGender,
                            GrammaticalScope.units);
            if (genderInfo.isEmpty()) {
                continue;
            }
            CLDRFile cldrFile = info.getCldrFactory().make(localeID, true);
            Map<String, String> shortUnitToGender = new TreeMap<>();
            Output<String> source = new Output<>();
            Multimap<UnitPathType, String> partsUsed = LinkedHashMultimap.create();

            Set<String> units = new HashSet<>();
            M4<String, String, String, Boolean> quantityToGenderToUnits =
                    ChainedMap.of(
                            new TreeMap<String, Object>(),
                            new TreeMap<String, Object>(),
                            new TreeMap<String, Object>(),
                            Boolean.class);
            M4<String, String, String, Boolean> genderToQuantityToUnits =
                    ChainedMap.of(
                            new TreeMap<String, Object>(),
                            new TreeMap<String, Object>(),
                            new TreeMap<String, Object>(),
                            Boolean.class);

            for (String path : cldrFile) {
                if (!path.startsWith("//ldml/units/unitLength[@type=\"long\"]/unit[@type=")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                final String shortId = converter.getShortId(parts.getAttributeValue(-2, "type"));
                if (NOT_CONVERTABLE.contains(shortId)) {
                    continue;
                }
                String quantity = null;
                try {
                    quantity = converter.getQuantityFromUnit(shortId, false);
                } catch (Exception e) {
                }

                if (quantity == null) {
                    throw new IllegalArgumentException("No quantity for " + shortId);
                }

                // ldml/units/unitLength[@type="long"]/unit[@type="duration-year"]/gender
                String gender = null;
                if (parts.size() == 5 && parts.getElement(-1).equals("gender")) {
                    gender = cldrFile.getStringValue(path);
                    if (true) {
                        quantityToGenderToUnits.put(quantity, gender, shortId, true);
                        genderToQuantityToUnits.put(quantity, gender, shortId, true);
                    }
                } else {
                    if (units.contains(shortId)) {
                        continue;
                    }
                    units.add(shortId);
                }
                UnitId unitId = converter.createUnitId(shortId);
                String constructedGender = unitId.getGender(cldrFile, source, partsUsed);
                boolean multiUnit =
                        unitId.denUnitsToPowers.size() + unitId.denUnitsToPowers.size() > 1;
                if (gender == null && (constructedGender == null || !multiUnit)) {
                    continue;
                }

                final boolean areEqual = Objects.equals(gender, constructedGender);
                if (SHOW_COMPOSE) {
                    final String printInfo =
                            localeID
                                    + "\t"
                                    + unitId
                                    + "\t"
                                    + gender
                                    + "\t"
                                    + multiUnit
                                    + "\t"
                                    + quantity
                                    + "\t"
                                    + constructedGender
                                    + "\t"
                                    + areEqual;
                    System.out.println(printInfo);
                }

                if (gender != null && !areEqual && !skipUnits.contains(shortId)) {
                    unitId.getGender(cldrFile, source, partsUsed);
                    shortUnitToGender.put(
                            shortId,
                            unitId
                                    + "\t actual gender: "
                                    + gender
                                    + "\t constructed gender:"
                                    + constructedGender);
                }
            }
            if (quantityToGenderToUnits.keySet().isEmpty()) {
                if (SHOW_COMPOSE) {
                    printlnIfZero(noGendersForLocales);
                    System.out.println("No genders for\t" + localeID);
                }
                localesWithNoGenders++;
                continue;
            }

            for (Entry<String, String> entry : shortUnitToGender.entrySet()) {
                if (SHOW_COMPOSE) {
                    printlnIfZero(noGendersForLocales);
                    System.out.println(localeID + "\t" + entry);
                }
                noGendersForLocales++;
            }

            Set<String> missing = new LinkedHashSet<>(genderInfo);
            for (String quantity : keyValues) {
                M3<String, String, Boolean> genderToUnits = quantityToGenderToUnits.get(quantity);
                showData(localeID, null, quantity, genderToUnits);
                missing.removeAll(genderToUnits.keySet());
            }
            for (String quantity : quantityToGenderToUnits.keySet()) {
                M3<String, String, Boolean> genderToUnits = quantityToGenderToUnits.get(quantity);
                showData(localeID, missing, quantity, genderToUnits);
            }
            for (String gender : missing) {
                if (SHOW_DATA) {
                    printlnIfZero(noGendersForLocales);
                    System.out.println(
                            "Missing values: " + localeID + "\t" + "?" + "\t" + gender + "\t?");
                }
                noGendersForLocales++;
            }
        }
        if (noGendersForLocales > 0) {
            warnln(
                    noGendersForLocales
                            + " units x locales with missing gender. Use -DTestUnits:SHOW_DATA for info, -DTestUnits:SHOW_COMPOSE for compositions");
        }
    }

    public void printlnIfZero(int noGendersForLocales) {
        if (noGendersForLocales == 0) {
            System.out.println();
        }
    }

    public void showData(
            String localeID,
            Set<String> genderFilter,
            String quantity,
            final M3<String, String, Boolean> genderToUnits) {
        for (Entry<String, Map<String, Boolean>> entry2 : genderToUnits) {
            String gender = entry2.getKey();
            if (genderFilter != null) {
                if (!genderFilter.contains(gender)) {
                    continue;
                }
                genderFilter.remove(gender);
            }
            for (String unit : entry2.getValue().keySet()) {
                logln(localeID + "\t" + quantity + "\t" + gender + "\t" + unit);
            }
        }
    }

    static final boolean DEBUG_DERIVATION = false;

    public void testDerivation() {
        int count = 0;
        for (String locale : SDI.hasGrammarDerivation()) {
            GrammarDerivation gd = SDI.getGrammarDerivation(locale);
            if (DEBUG_DERIVATION) System.out.println(locale + " => " + gd);
            ++count;
        }
        assertNotEquals("hasGrammarDerivation", 0, count);
    }

    static final boolean DEBUG_ORDER = false;

    public void TestUnitOrder() {
        if (DEBUG_ORDER) {
            System.out.println();
            for (Entry<String, Collection<Continuation>> entry :
                    converter.getContinuations().asMap().entrySet()) {
                System.out.println(entry);
            }
        }

        for (Entry<String, String> entry : converter.getBaseUnitToQuantity().entrySet()) {
            checkNormalization("base-quantity, " + entry.getValue(), entry.getKey());
        }

        // check root list
        // crucial that this is stable!!
        Set<String> shortUnitsFound =
                checkCldrFileUnits("root unit", CLDRConfig.getInstance().getRoot());
        final Set<String> shortValidRegularUnits = VALID_SHORT_UNITS;
        assertEquals(
                "root units - regular units",
                Collections.emptySet(),
                Sets.difference(shortUnitsFound, shortValidRegularUnits));
        // TODO — we don't want to just add to the exception list.
        //        assertEquals(
        //                "regular units - special_untranslated - root units",
        //                Collections.emptySet(),
        //                Sets.difference(
        //                        Sets.difference(
        //                                shortValidRegularUnits,
        // UnitConverter.UNTRANSLATED_UNIT_NAMES),
        //                        shortUnitsFound));

        // check English also
        checkCldrFileUnits("en unit", CLDRConfig.getInstance().getEnglish());

        for (String unit : converter.canConvert()) {
            checkNormalization("convertable", unit);
            String baseUnitId = converter.getBaseUnit(unit);
            checkNormalization("convertable base", baseUnitId);
        }

        checkNormalization("test case", "foot-acre", "acre-foot");
        checkNormalization("test case", "meter-newton", "newton-meter");

        checkNormalization("test case", "newton-meter");
        checkNormalization("test case", "acre-foot");

        String stdAcre = converter.getStandardUnit("acre");

        UnitOrdering unitOrdering = new UnitOrdering();
        List<String> simpleBaseUnits = new ArrayList<>();

        for (ExternalUnitConversionData data : NistUnits.externalConversionData) {
            // unitOrdering.add(data.source);
            final String source = data.source;
            final String target = data.target;
            unitOrdering.add(target);
            checkNormalization("nist core, " + source, target);
        }
        for (Entry<String, TargetInfo> data : NistUnits.derivedUnitToConversion.entrySet()) {
            if (DEBUG_ORDER) {
                System.out.println(data);
            }
            final String target = data.getValue().target;
            unitOrdering.add(target);
            simpleBaseUnits.add(data.getKey());
            checkNormalization("nist derived", target);
        }

        if (DEBUG_ORDER) {
            System.out.println("Pass 1\n" + unitOrdering.orderingData);
        }

        for (String baseUnit : converter.getBaseUnitToQuantity().keySet()) {
            unitOrdering.add(baseUnit);
            String status = converter.getBaseUnitToStatus().get(baseUnit);
            if ("simple".equals(status)) {
                simpleBaseUnits.add(baseUnit);
            }
        }
        if (DEBUG_ORDER) {
            System.out.println("Pass 2\n" + unitOrdering.orderingData);
        }

        if (DEBUG_ORDER)
            System.out.println(
                    "Extracted data\n"
                            + Joiner.on('\n').join(unitOrdering.orderingData.asMap().entrySet()));
        if (DEBUG_ORDER) System.out.println("Building data");

        // check the builder first
        TotalOrderBuilder<String> totalOrderBuilder = new TotalOrderBuilder<>();

        if (false) {
            totalOrderBuilder.add("meter", "second").add("kilogram", "meter");
            totalOrderBuilder.build();

            totalOrderBuilder
                    .add("meter", "second")
                    .add("kilogram", "meter")
                    .add("second", "kilogram");
            try {
                totalOrderBuilder.build();
            } catch (Exception e) {
                errln("Problem in TotalOrderBuilder");
            }
        }
        if (DEBUG_ORDER) System.out.println("Show ordering");
        // now all the units
        for (List<String> orderedUnits : unitOrdering.orderingData.asMap().keySet()) {
            List<String> baseUnits = new ArrayList<>();
            for (String orderedUnit : orderedUnits) {
                baseUnits.add(unitOrdering.getId(orderedUnit, unitOrdering.rejects));
            }
            if (DEBUG_ORDER) System.out.println(orderedUnits + "\t" + baseUnits);
            totalOrderBuilder.add(baseUnits);
        }
        for (String simpleBaseUnit : simpleBaseUnits) {
            totalOrderBuilder.add(Collections.singletonList(simpleBaseUnit));
        }
        if (DEBUG_ORDER) System.out.println(totalOrderBuilder);

        if (DEBUG_ORDER) System.out.println("Rejects: " + unitOrdering.rejects);
        if (DEBUG_ORDER) System.out.println("Ordering: " + totalOrderBuilder.build());

        //        for (Entry<String, Collection<String>> entry :
        // piecesToOccurences.asMap().entrySet()) {
        //            System.out.println(entry.getKey() + "\t" + entry.getValue());
        //        }
    }

    /**
     * Checks the normalization of units found in the file, and returns the set of shortUnitIds
     * found in the file
     */
    public Set<String> checkCldrFileUnits(String title, final CLDRFile cldrFile) {
        Set<String> shortUnitsFound = new TreeSet<>();
        for (String path : cldrFile) {
            if (!path.startsWith("//ldml/units/unitLength")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String longUnitId = parts.findAttributeValue("unit", "type");
            if (longUnitId == null) {
                continue;
            }
            String shortUnitId = converter.getShortId(longUnitId);
            shortUnitsFound.add(shortUnitId);
            checkNormalization(title, shortUnitId);
        }
        return ImmutableSet.copyOf(shortUnitsFound);
    }

    public void checkNormalization(String title, String source, String expected) {
        String oldExpected = normalizationCache.get(source);
        if (oldExpected != null) {
            if (!oldExpected.equals(expected)) {
                assertEquals(
                        title + ", consistent expected results for " + source,
                        oldExpected,
                        expected);
            }
            return;
        }
        normalizationCache.put(source, expected);
        UnitId unitId = converter.createUnitId(source);
        assertEquals(title + ", unit order", expected, unitId.toString());
    }

    public void checkNormalization(String title, String source) {
        checkNormalization(title, source, source);
    }

    static class UnitOrdering {
        boolean SKIP_POWERS = true;
        Set<String> SKIP_UNITS =
                ImmutableSet.of(
                        "kilogram-per-pascal-second-square-meter",
                        "kilogram-per-pascal-second-meter");

        final Set<String> SUFFIXES =
                ImmutableSet.of(
                        "0c",
                        "15c",
                        "20c",
                        "23c",
                        "32f",
                        "365",
                        "392f",
                        "39f",
                        "4c",
                        "59f",
                        "60f",
                        "survey",
                        "assay",
                        "imperial",
                        "long",
                        "of",
                        "capacitance",
                        "inductance",
                        "current",
                        "electric",
                        "potential",
                        "electric",
                        "inductance,",
                        "resistance",
                        "water",
                        "troy",
                        "tnt",
                        "sidereal",
                        "unitth",
                        "unitit",
                        "mean",
                        "nutrition",
                        "tropical",
                        "pole",
                        "boiler",
                        "mil",
                        "force",
                        "printer",
                        "refrigeration",
                        "register",
                        "technical",
                        "thermal",
                        "metric",
                        "dry");

        final Set<String> POWERS = ImmutableSet.of("square", "cubic", "pow4");
        // mil-inch, perm-inch

        Set<String> seen = new HashSet<>();
        Multimap<String, String> piecesToOccurences = TreeMultimap.create();
        Multimap<String, Continuation> continuations = converter.getContinuations();
        TreeMultimap<List<String>, String> orderingData =
                TreeMultimap.create(
                        Comparators.lexicographical(Ordering.natural()), Ordering.natural());
        TreeSet<String> rejects = new TreeSet<>();

        void add(String unitId) {
            if (!unitId.contains("-") || !seen.add(unitId) || SKIP_UNITS.contains(unitId)) {
                return;
            }
            if (unitId.contains("square-meter-kilogram")) {
                int debug = 0;
            }
            List<String> pieces = new ArrayList<>();
            ArrayList<String> orderedNumerator = new ArrayList<>();
            ArrayList<String> orderedDenominator = new ArrayList<>();
            ArrayList<String> current = orderedNumerator;
            for (UnitIterator it = Continuation.split(unitId, continuations).iterator();
                    it.hasNext(); ) {
                String unit = it.next();
                if (unit.equals("per")) {
                    if (current == orderedDenominator) {
                        throw new IllegalArgumentException();
                    }
                    handleOrdering(current, unitId);
                    current = orderedDenominator;
                    continue;
                }
                if (POWERS.contains(unit)) {
                    if (SKIP_POWERS) {
                        continue;
                    }
                    String nextUnit = it.next();
                    nextUnit = UnitConverter.stripPrefix(nextUnit, null);
                    unit += "-" + nextUnit; // should never overrun
                } else {
                    unit = UnitConverter.stripPrefix(unit, null);
                }
                String peek = it.peek();
                while (peek != null && SUFFIXES.contains(peek)) {
                    unit += "-" + peek;
                    it.next();
                    peek = it.peek();
                }
                current.add(unit);
                pieces.add(unit);
                piecesToOccurences.put(unit, unitId);
            }
            handleOrdering(current, unitId);
            // System.out.println(pieces + "\t=>\t" + data.target);
        }

        Map<String, String> EXTRA_BASES =
                ImmutableMap.<String, String>builder()
                        .put("british-thermal-unitit", "joule")
                        .put("british-thermal-unitth", "joule")
                        .put("centimeter", "meter")
                        .put("circular-mil", "meter")
                        // .put("dry", "???")
                        .put("dyne", "newton")
                        .put("foot-survey", "meter")
                        .put("inch-0c", "meter")
                        .put("inch-23c", "meter")
                        .put("kilogram-force", "newton")
                        .put("kilowatt", "watt")
                        // .put("mil", "???")
                        .put("millimeter", "meter")
                        .put("ofhg-0c", "ofhg")
                        .put("ofhg-32f", "ofhg")
                        .put("ofhg-60f", "ofhg")
                        .put("ounce-force", "newton")
                        .put("perm", "kilogram-per-second-per-square-meter-per-pascal")
                        .put("poundal", "newton")
                        .put("rankine", "celcius")
                        .build();

        public String getId(String orderedUnit, Set<String> rejects) {
            String result = converter.getStandardUnit(orderedUnit);
            if (result == null) {
                result = EXTRA_BASES.get(orderedUnit);
                if (result == null) {
                    rejects.add(orderedUnit);
                    return "???";
                }
            }
            return result;
        }

        private void handleOrdering(ArrayList<String> current, String source) {
            if (current.size() < 2) {
                return;
            }
            orderingData.put(current, source);
        }
    }

    public void TestElectricConsumption() {
        String inputUnit = "kilowatt-hour-per-100-kilometer";
        String outputUnit = "kilogram-meter-per-square-second";
        Rational result = converter.convert(Rational.ONE, inputUnit, outputUnit, DEBUG);
        assertEquals("kWh-per-100k", Rational.of(36), result);
    }

    public void TestEnglishDisplayNames() {
        CLDRFile en = CLDRConfig.getInstance().getEnglish();
        ImmutableSet<String> unitSkips = ImmutableSet.of("temperature-generic", "graphics-em");
        for (String path : en) {
            if (path.startsWith("//ldml/units/unitLength[@type=\"long\"]")
                    && path.endsWith("/displayName")) {
                if (path.contains("coordinateUnit")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                final String longUnitId = parts.getAttributeValue(3, "type");
                if (unitSkips.contains(longUnitId)) {
                    continue;
                }
                final String width = parts.getAttributeValue(2, "type");
                // ldml/units/unitLength[@type="long"]/unit[@type="duration-decade"]/displayName
                String displayName = en.getStringValue(path);

                // ldml/units/unitLength[@type="long"]/unit[@type="duration-decade"]/unitPattern[@count="other"]
                String pluralFormPath =
                        path.substring(0, path.length() - "/displayName".length())
                                + "/unitPattern[@count=\"other\"]";
                String pluralForm = en.getStringValue(pluralFormPath);
                if (pluralForm == null) {
                    errln("Have display name but no plural: " + pluralFormPath);
                } else {
                    String cleaned = pluralForm.replace("{0}", "").trim();
                    assertEquals(
                            "Unit display name should correspond to plural in English "
                                    + width
                                    + ", "
                                    + longUnitId,
                            cleaned,
                            displayName);
                }
            }
        }
    }

    enum TranslationStatus {
        has_grammar_M,
        has_grammar_X,
        add_grammar,
        skip_grammar,
        skip_trans
    }

    /**
     * Check which units are enabled for translation. If -v, then generates lines for spreadsheet
     * checks.
     */
    public void TestUnitsToTranslate() {
        Set<String> toTranslate = GrammarInfo.getUnitsToAddGrammar();
        final CLDRConfig config = CLDRConfig.getInstance();
        final UnitConverter converter = config.getSupplementalDataInfo().getUnitConverter();
        Map<String, TranslationStatus> shortUnitToTranslationStatus40 = new TreeMap<>();
        for (String longUnit :
                Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular)) {
            String shortUnit = converter.getShortId(longUnit);
            shortUnitToTranslationStatus40.put(shortUnit, TranslationStatus.skip_trans);
        }
        for (String path :
                With.in(
                        config.getRoot()
                                .iterator("//ldml/units/unitLength[@type=\"short\"]/unit"))) {
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String longUnit = parts.getAttributeValue(3, "type");
            // Add simple units
            String shortUnit = converter.getShortId(longUnit);
            Set<UnitSystem> systems = converter.getSystemsEnum(shortUnit);

            boolean siOrMetric = !Collections.disjoint(systems, UnitSystem.SiOrMetric);

            TranslationStatus status =
                    toTranslate.contains(longUnit)
                            ? (siOrMetric
                                    ? TranslationStatus.has_grammar_M
                                    : TranslationStatus.has_grammar_X)
                            : siOrMetric
                                    ? TranslationStatus.add_grammar
                                    : TranslationStatus.skip_grammar;
            shortUnitToTranslationStatus40.put(shortUnit, status);
        }
        for (Entry<String, TranslationStatus> entry : shortUnitToTranslationStatus40.entrySet()) {
            String shortUnit = entry.getKey();
            TranslationStatus status40 = entry.getValue();
            if (isVerbose())
                System.out.println(
                        shortUnit
                                + "\t"
                                + converter.getQuantityFromUnit(shortUnit, false)
                                + "\t"
                                + converter.getSystemsEnum(shortUnit)
                                + "\t"
                                + (converter.isSimple(shortUnit) ? "simple" : "complex")
                                + "\t"
                                + status40);
        }
    }

    static final String marker = "➗";

    public void TestValidUnitIdComponents() {
        for (String longUnit : VALID_REGULAR_UNITS) {
            String shortUnit = SDI.getUnitConverter().getShortId(longUnit);
            checkShortUnit(shortUnit);
        }
    }

    public void TestDeprecatedUnitIdComponents() {
        for (String longUnit : DEPRECATED_REGULAR_UNITS) {
            String shortUnit = SDI.getUnitConverter().getShortId(longUnit);
            checkShortUnit(shortUnit);
        }
    }

    public void TestSelectedUnitIdComponents() {
        checkShortUnit("curr-chf");
    }

    public void checkShortUnit(String shortUnit) {
        List<String> parts = SPLIT_DASH.splitToList(shortUnit);
        List<String> simpleUnit = new ArrayList<>();
        UnitIdComponentType lastType = null;
        // structure is (prefix* base* suffix*) per ((prefix* base* suffix*)

        for (String part : parts) {
            UnitIdComponentType type = getUnitIdComponentType(part);
            switch (type) {
                case prefix:
                    if (lastType != UnitIdComponentType.prefix && !simpleUnit.isEmpty()) {
                        simpleUnit.add(marker);
                    }
                    break;
                case base:
                    if (lastType != UnitIdComponentType.prefix && !simpleUnit.isEmpty()) {
                        simpleUnit.add(marker);
                    }
                    break;
                case suffix:
                    if (!(lastType == UnitIdComponentType.base
                            || lastType == UnitIdComponentType.suffix)) {
                        if ("metric".equals(part)) { // backward compatibility for metric ton; only
                            // needed if deprecated ids are allowed
                            lastType = UnitIdComponentType.prefix;
                        } else {
                            errln(
                                    simpleUnit
                                            + "/"
                                            + part
                                            + "; suffix only after base or suffix: "
                                            + false);
                        }
                    }
                    break;
                    // could add more conditions on these
                case and:
                    assertNotNull(simpleUnit + "/" + part + "; not at start", lastType);
                    // fall through
                case power:
                case per:
                    assertNotEquals(
                            simpleUnit + "/" + part + "; illegal after prefix",
                            UnitIdComponentType.prefix,
                            lastType);
                    if (!simpleUnit.isEmpty()) {
                        simpleUnit.add(marker);
                    }
                    break;
            }
            simpleUnit.add(part + "*" + type.toShortId());
            lastType = type;
        }
        assertTrue(
                simpleUnit + ": last item must be base or suffix",
                lastType == UnitIdComponentType.base || lastType == UnitIdComponentType.suffix);
        logln("\t" + shortUnit + "\t" + simpleUnit.toString());
    }

    public UnitIdComponentType getUnitIdComponentType(String part) {
        return SDI.getUnitIdComponentType(part);
    }

    public void TestMetricTon() {
        assertTrue(
                "metric-ton is deprecated", DEPRECATED_REGULAR_UNITS.contains("mass-metric-ton"));
        assertEquals(
                "metric-ton is deprecated",
                "tonne",
                SDI.getUnitConverter().fixDenormalized("metric-ton"));
        assertEquals(
                "to short", "metric-ton", SDI.getUnitConverter().getShortId("mass-metric-ton"));
        // assertEquals("to long", "mass-metric-ton",
        // SDI.getUnitConverter().getLongId("metric-ton"));
    }

    public void TestUnitParser() {
        UnitParser up = new UnitParser();
        for (String longUnit : VALID_REGULAR_UNITS) {
            String shortUnit = SDI.getUnitConverter().getShortId(longUnit);
            checkParse(up, shortUnit);
        }
    }

    private List<Pair<String, UnitIdComponentType>> checkParse(UnitParser up, String shortUnit) {
        up.set(shortUnit);
        List<Pair<String, UnitIdComponentType>> results = new ArrayList<>();
        Output<UnitIdComponentType> type = new Output<>();
        while (true) {
            String result = up.nextParse(type);
            if (result == null) {
                break;
            }
            results.add(new Pair<>(result, type.value));
        }
        logln(shortUnit + "\t" + results);
        return results;
    }

    public void TestUnitParserSelected() {
        UnitParser up = new UnitParser();
        String[][] tests = {
            // unit, exception, resultList
            {"british-force", "Unit suffix must follow base: british ❌ force"}, // prefix-suffix
            {"force", "Unit suffix must follow base: null ❌ force"}, // suffix
            {
                "british-and-french", "Unit prefix must be followed with base: british ❌ and"
            }, // prefix-and
            {"british", "Unit prefix must be followed with base: british ❌ null"}, // prefix
            {"g-force-light-year", null, "[(g-force,base), (light-year,base)]"}, // suffix
        };
        for (String[] test : tests) {
            String shortUnit = test[0];
            String expectedError = test[1];
            String expectedResult = test.length <= 2 ? null : test[2];

            String actualError = null;
            List<Pair<String, UnitIdComponentType>> actualResult = null;
            try {
                actualResult = checkParse(up, shortUnit);
            } catch (Exception e) {
                actualError = e.getMessage();
            }
            assertEquals(shortUnit + " exception", expectedError, actualError);
            assertEquals(
                    shortUnit + " result",
                    expectedResult,
                    actualResult == null ? null : actualResult.toString());
        }
    }

    public void TestUnitParserAgainstContinuations() {
        UnitParser up = new UnitParser();
        UnitConverter uc = SDI.getUnitConverter();
        Multimap<String, Continuation> continuations = uc.getContinuations();
        Output<UnitIdComponentType> type = new Output<>();
        for (String shortUnit : VALID_SHORT_UNITS) {
            if (shortUnit.contains("100")) {
                logKnownIssue("CLDR-15929", "Code doesn't handle 100");
                continue;
            }
            up.set(shortUnit);
            UnitIterator x = UnitConverter.Continuation.split(shortUnit, continuations);

            int count = 0;
            while (true) {
                String upSegment = up.nextParse(type);
                String continuationSegment = x.hasNext() ? x.next() : null;
                if (upSegment == null || continuationSegment == null) {
                    assertEquals(
                            count + ") " + shortUnit + " Same number of segments ",
                            continuationSegment == null,
                            upSegment == null);
                    break;
                }
                assertTrue(
                        "type is never suffix or prefix",
                        UnitIdComponentType.suffix != type.value
                                && UnitIdComponentType.prefix != type.value);
                ++count;
                if (!assertEquals(
                        count + ") " + shortUnit + " Continuation segment vs UnitParser ",
                        continuationSegment,
                        upSegment)) {
                    break; // stop at first difference
                }
            }
        }
    }
}
