package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.BoundaryTransform;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitPathType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetSpanner;

public class TestBoundaryTransform  extends TestFmwk {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CLDR_CONFIG.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestBoundaryTransform().run(args);
    }

    public void TestBasic() {
        BoundaryTransform bt = null;

        String[][] tests = {
            // rule = contextBefore ⦅ replaceBefore ❙ replaceAfter ⦆ contextAfter → replaceBy

            {"rules", "ο-❙λίτρ→όλιτρ"},
            {"χιλιοστο-", "λίτρα", "χιλιοστόλιτρα"},

            {"rules", "[aáàâãeéêiíoóòôõuú]⦅❙⦆s→s"},
            {"nano", "segundo", "nanossegundo"},
            {"nanx", "segundo", "nanxsegundo"},
            {"nano", "meter", "nanometer"},
            {"quilô", "segundo", "quilôssegundo"},


            {"rules", "a⦅b❙c⦆d→x"},
            {".ab", "cd.", ".axd."},
            {".qb", "cd.", ".qbcd."},
            {".ab", "cq.", ".abcq."},

        };
        for (String[] row : tests) {
            if (row[0].equals("rules")) {
                bt = BoundaryTransform.from(row[1]);
                System.out.println(bt);
                continue;
            }
            if (bt == null) {
                throw new IllegalArgumentException("Must have rules line first");
            }
            String actual = bt.apply(row[0] + row[1], row[0].length());
            String expected = row[2];
            assertEquals("basics", expected, actual);
        }
    }

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

    static final String[][] COMPOUND_TESTS = {
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

        {"digital-gigabit", "giga", "digital-bit"},
        {"digital-gigabyte", "giga", "digital-byte"},
        {"digital-kilobit", "kilo", "digital-bit"},
        {"digital-kilobyte", "kilo", "digital-byte"},
        {"digital-megabit", "mega", "digital-bit"},
        {"digital-megabyte", "mega", "digital-byte"},
        {"digital-petabyte", "peta", "digital-byte"},
        {"digital-terabit", "tera", "digital-bit"},
        {"digital-terabyte", "tera", "digital-byte"},

        {"area-square-centimeter", "square", "length-centimeter"},
        {"area-square-foot", "square", "length-foot"},
        {"area-square-inch", "square", "length-inch"},
        {"area-square-kilometer", "square", "length-kilometer"},
        {"area-square-meter", "square", "length-meter"},
        {"area-square-mile", "square", "length-mile"},
        {"area-square-yard", "square", "length-yard"},

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

    static final String PATH_PREFIX_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/unitPrefixPattern";
    static final String PATH_GENDER_PATTERN = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"{0}\"]/gender";
    static final String PATH_SUFFIX_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/compoundUnitPattern1[@count=\"{2}\"]";
    static final String PATH_SUFFIX_GENDER_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/compoundUnit[@type=\"{1}\"]/compoundUnitPattern1[@count=\"{2}\"][@gender=\"{3}\"]";
    static final String PATH_UNIT_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"{1}\"]/unitPattern[@count=\"{2}\"]";
    static final String PATH_UNIT_CASE_PATTERN = "//ldml/units/unitLength[@type=\"{0}\"]/unit[@type=\"{1}\"]/unitPattern[@count=\"{2}\"][@case=\"{3}\"]";
    //ldml/units/unitLength[@type="long"]/unit[@type="length-foot"]/unitPattern[@count="one"]
    //ldml/units/unitLength[@type="long"]/unit[@type="length-foot"]/unitPattern[@count="one"][@case="accusative"]


    private static final UnicodeSet WHITESPACE = new UnicodeSet("[:whitespace:]").freeze();
    private static final UnicodeSetSpanner SPACE_SPANNER = new UnicodeSetSpanner(WHITESPACE);

    private static Pattern NO_SPACE_PREFIX = Pattern.compile("\\}" + ExampleGenerator.backgroundEndSymbol + "?\\p{L}|\\p{L}" + ExampleGenerator.backgroundStartSymbol + "?\\{");

    public static String combinePattern(BoundaryTransform bt, String unitFormat, String compoundPattern, boolean lowercaseUnitIfNoSpaceInCompound) {
        // meterFormat of the form {0} meters or {0} Meter
        // compoundPattern is of the form Z{0} or Zetta{0}

        // extract the unit
        String modUnit = (String) SPACE_SPANNER.trim(unitFormat.replace("{0}", ""));
        Object[] parameters = { modUnit };

        String combined = MessageFormat.format(compoundPattern, parameters);
        if (bt != null && compoundPattern.endsWith("{0}")) {
            combined = bt.apply(combined, compoundPattern.length()-3);
        }

        String modFormat = unitFormat.replace(modUnit, combined);

        if (modFormat.equals(unitFormat)) {
            // didn't work, so fall back
            Object[] parameters1 = { unitFormat };
            modFormat = MessageFormat.format(compoundPattern, parameters1);
        }

        // hack to fix casing
        if (lowercaseUnitIfNoSpaceInCompound
            && NO_SPACE_PREFIX.matcher(compoundPattern).find()) {
            modFormat = modFormat.replace(modUnit, modUnit.toLowerCase(Locale.ENGLISH));
        }

        return modFormat;
    }

    private static final ImmutableSet<String> casesNominativeOnly = ImmutableSet.of(GrammaticalFeature.grammaticalCase.getDefault(null));

    public void TestComposition() {
        Factory factory = CLDR_CONFIG.getCldrFactory();

        // options
        boolean doCases = false;
        Set<String> localesToTest = ImmutableSet.of("pt", "fr", "el", "de", "cs", "pl"); // factory.getAvailableLanguages();

        Map<String,String> prefixToType = new LinkedHashMap<>();
        for (String[] prefixRow : PREFIX_NAME_TYPE) {
            prefixToType.put(prefixRow[0], prefixRow[1]);
        }
        prefixToType = ImmutableMap.copyOf(prefixToType);

        int testCount = 0;
        for (String locale : localesToTest) {
            CLDRFile resolvedCldrFile = factory.make(locale, true);
            BoundaryTransform bt = BoundaryTransform.getTransform(locale);

            Collection<String> cases = casesNominativeOnly;
            if (doCases) {
                GrammarInfo grammarInfo = SDI.getGrammarInfo(locale, true);
                if (grammarInfo != null) {
                    if (grammarInfo.hasInfo(GrammaticalTarget.nominal)) {
                        Collection<String> rawCases = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units);
                        if (!rawCases.isEmpty()) {
                            cases = rawCases;
                        }
                    }
                }
            }

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
                String gender = UnitPathType.gender.getTrans(resolvedCldrFile, "long", baseUnit, null, null, null, null);

                for (String width : Arrays.asList("long" /*,"short", "narrow" */)) {
                    boolean lowercaseIfSpaced = width.equals("long");

                    for (Count count : pluralInfo.getCounts()) {

                        String pluralCategory = count.toString();

                        for (String gcase : cases) {

                            String prefixValue = null;
                            if (isPrefix) {
                                prefixValue = UnitPathType.prefix.getTrans(resolvedCldrFile, width, prefixType, pluralCategory, gcase, gender, null);
                            } else {
                                prefixValue = UnitPathType.power.getTrans(resolvedCldrFile, width, prefixType, pluralCategory, gcase, gender, null);
                            }

                            String targetUnitPattern = UnitPathType.unit.getTrans(resolvedCldrFile, width, targetUnit, count.toString(), gcase, null, null);

                            String baseUnitPattern = UnitPathType.unit.getTrans(resolvedCldrFile, width, baseUnit, count.toString(), gcase, null, null);

                            String composedTargetUnitPattern = combinePattern(bt, baseUnitPattern, prefixValue, lowercaseIfSpaced);

                            // TODO find out why there is sometimes a mismatch between the compound and baseUnit in which space is used

                            targetUnitPattern = normalizeSpaces(targetUnitPattern);
                            composedTargetUnitPattern = normalizeSpaces(composedTargetUnitPattern);

                            if (!targetUnitPattern.equals(composedTargetUnitPattern)) {
                                warnln(++testCount + ") " + locale + "/" + targetUnit + "/" + width + "/" + count + "/" + gcase + "/" + prefixValue + "/" + baseUnitPattern
                                    + "; expected «" + targetUnitPattern + "», actual  «" + composedTargetUnitPattern + "»");
                            }
                        }
                    }
                }
            }
        }
    }

    private String normalizeSpaces(String targetUnitPattern) {
        return SPACE_SPANNER.replaceFrom(targetUnitPattern, " ");
    }
}
