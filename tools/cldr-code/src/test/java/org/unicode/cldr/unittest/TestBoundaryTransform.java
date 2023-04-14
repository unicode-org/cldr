package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.BoundaryTransform;
import org.unicode.cldr.util.BoundaryTransform.BoundaryUsage;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitPathType;
import org.unicode.cldr.util.XListFormatter.ListTypeLength;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetSpanner;

public class TestBoundaryTransform  extends TestFmwk {
    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CLDR_CONFIG.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestBoundaryTransform().run(args);
    }

    public void TestBasicUnitPrefixes() {
        BoundaryTransform bt = null;

        String[][] tests = {
            // rule = contextBefore ⦅ replaceBefore ❙ replaceAfter ⦆ contextAfter → replaceBy

            {"rules", "⦅(?<before>.*)❙na ⦆→na ${before};"}, // move 'na ' to the front
            {"kilo", "na calorie", "na kilocalorie"},
            {"giga{0}", "{0} na watt", "{0} na gigawatt"},
            {"giga{0}", "{0} na bit", "{0} na gigabit"},

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
            final String prefixValue = row[0];
            final String baseUnitPattern = row[1];
            if (prefixValue.equals("rules")) {
                bt = BoundaryTransform.from(baseUnitPattern);
                System.out.println(bt);
                continue;
            }
            if (bt == null) {
                throw new IllegalArgumentException("Must have rules line first");
            }
            String expected = row[2];
            String actual;
            if (prefixValue.contains("{0}")) {
                //     «giga{0}»   «{0} na watt» «{0} na gigawatt»
                actual = combinePattern(bt, baseUnitPattern, prefixValue, true);
            } else {
                actual = bt.apply(prefixValue + baseUnitPattern, prefixValue.length());
            }
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

    public void TestLocaleUnitPrefixes() {
        Factory factory = CLDR_CONFIG.getCldrFactory();

        // inclusion options
        boolean exhaustive = getInclusion() > 6;
        boolean doCases = exhaustive;
        boolean doCounts = exhaustive;
        Set<String> localesToTest;
        switch (getInclusion()) {
        case 0: case 1:
            localesToTest = ImmutableSet.of("fil");
            break;
        case 2: case 3: case 4:
            localesToTest = ImmutableSet.of("pt", "fr", "el", "de", "cs", "pl");
            break;
        default:
            Set<String> temps = STANDARD_CODES.getLocaleCoverageLocales(Organization.cldr);
            localesToTest = new LinkedHashSet<>();
            for (String temp : temps) {
                if (STANDARD_CODES.getLocaleCoverageLevel(Organization.cldr, temp).compareTo(Level.MODERATE) >= 0) {
                    localesToTest.add(temp);
                }
            }
            break;
        }

        Map<String,String> prefixToType = new LinkedHashMap<>();
        for (String[] prefixRow : PREFIX_NAME_TYPE) {
            prefixToType.put(prefixRow[0], prefixRow[1]);
        }
        prefixToType = ImmutableMap.copyOf(prefixToType);
        List<String> summary = new ArrayList<>();

        for (String locale : localesToTest) {
            int okCount = 0;
            int badCount = 0;
            CLDRFile resolvedCldrFile = factory.make(locale, true);
            UnicodeSet main = resolvedCldrFile.getExemplarSet(ExemplarType.main, WinningChoice.WINNING);
            String script = "?";
            for (String s : main) {
                int scriptNo = UScript.getScript(s.codePointAt(0));
                switch (scriptNo) {
                case UScript.UNKNOWN: case UScript.COMMON: case UScript.INHERITED:
                    continue;
                default:
                    break;
                }
                script = UScript.getSampleString(scriptNo);
            }
            BoundaryTransform bt = BoundaryTransform.getTransform(locale, BoundaryUsage.unitPrefix);

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

                    final Set<Count> counts = doCounts ? pluralInfo.getCounts() : ImmutableSet.of(Count.other);

                    for (Count count : counts) {

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
                                warnln("\t" + locale
                                    + "\t" + script
                                    + "\t" + targetUnit
                                    + "\t" + width + "/" + count + "/" + gcase
                                    + "\t«" + targetUnitPattern + "»"
                                    + "\t«" + composedTargetUnitPattern + "»"
                                    + "\t«" + prefixValue + "»"
                                    + "\t«" + baseUnitPattern + "»");
                                ++badCount;
                            } else {
                                ++okCount;
                            }

                        }
                    }
                }
            }
            summary.add("\t" + locale
                + "\t" + script
                + "\t" + okCount
                + "\t" + badCount
                + "\t\t\t\t");
        }
        summary.forEach(x -> warnln(x));
    }

    private String normalizeSpaces(String targetUnitPattern) {
        return SPACE_SPANNER.replaceFrom(targetUnitPattern, " ");
    }

    public void TestListFormat() {
        String[][] tests = {
            // Locale, listType, item1, item2, expected12, expected21
            {"it", "AND_WIDE", "tigri", "elefanti", "tigri ed elefanti", "elefanti e tigri"},
            {"it", "OR_WIDE", "rose", "orchidee", "rose od orchidee", "orchidee o rose"},
            {"it", "OR_WIDE", "tigri", "rose", "tigri o rose", "rose o tigri"},

            {"es", "AND_WIDE", "tigri", "rose", "tigri y rose", "rose y tigri"},
            {"es", "AND_WIDE", "tigri", "hiyo", "tigri e hiyo", "hiyo y tigri"},
            {"es", "OR_WIDE", "tigri", "rose", "tigri o rose", "rose o tigri"},
            {"es", "OR_WIDE", "tigri", "8", "tigri u 8", "8 o tigri"},

            {"he", "AND_WIDE", "א", "ב", "א וב", "ב וא"},
            {"he", "AND_WIDE", "א", "a", "א ו-a", "a וא"},

            /*
             *         .put("es", BoundaryTransform.from(""
            + " ⦅y ❙⦆([iI]|[hH][iI]([^aAeE]|$)→e ;" // y
            + " ⦅o ❙⦆([uU8]|[hH][oU]|11($| )→u ;" // o
            ))
        .put("he", BoundaryTransform.from(""
            + " \\u05D5⦅❙⦆\\P{sc=Hebr}→-;" // y
            ))

             */
        };
        String locale = null;
        BoundaryTransform bt = null;
        CLDRFile cldrFile = null;
        int i = 0;
        for (String[] test : tests) {
            ++i;
            String newLocale = test[0];
            ListTypeLength listTypeLength = ListTypeLength.valueOf(test[1]);
            if (!newLocale.equals(locale)) {
                locale = newLocale;
                bt = BoundaryTransform.getTransform(locale, BoundaryUsage.general);
                cldrFile = CLDR_CONFIG.getCldrFactory().make(locale, true);
            }
            ListFormatterX lt = new ListFormatterX(cldrFile, listTypeLength, bt);
            String actual = lt.format(test[2], test[3]);
            String expected = test[4];
            assertEquals(i + ") x " + test[1] + " y", expected, actual);
            actual = lt.format(test[3], test[2]);
            expected = test[5];
            assertEquals(i + ") y " + test[1] + " x", expected, actual);
        }
    }

    /**
     * Text implementation. Rudimentary for now
     */
    class ListFormatterX  {
        String doublePattern;
        String startPattern;
        String middlePattern;
        String endPattern;
        BoundaryTransform boundaryTransform;

        public ListFormatterX (CLDRFile cldrFile, ListTypeLength listTypeLength, BoundaryTransform bt) {
            SimpleFormatter listPathFormat = SimpleFormatter.compile(listTypeLength.getPath());
            doublePattern = cldrFile.getWinningValue(listPathFormat.format("2"));
            startPattern = cldrFile.getWinningValue(listPathFormat.format("start"));
            middlePattern = cldrFile.getWinningValue(listPathFormat.format("middle"));
            endPattern = cldrFile.getWinningValue(listPathFormat.format("end"));
            boundaryTransform = bt;
        }

        public String format(String string0, String string1) {
            // TODO generalize
            // for testing, assume {0} < {1}
            int s1 = doublePattern.indexOf("{0}");
            String pattern2 = doublePattern.replace("{0}", string0);
            // for now, just apply the the initial position for both items. TODO end position also
            pattern2 = boundaryTransform.apply(pattern2, s1);
            int s2 = pattern2.indexOf("{1}");
            pattern2 = pattern2.replace("{1}", string1);
            pattern2 = boundaryTransform.apply(pattern2, s2);
            return pattern2;
        }
    }

    public void TestGeneralFormat() {
        String[][] tests = {
            // Locale, pattern1, item1, expected
            {"en", "Take a {0}", "book", "Take a book"},
            {"en", "Take a {0}", "apple", "Take an apple"},
            {"zh", "舘{0}", "《豈》", "舘《豈》"},
            {"zh", "舘{0}", "豈", "舘豈"},
            {"zh", "舘{0}", "a", "舘 a"},
            {"en", "Take a {0}", "apple", "Take an apple"},
        };
        String locale = null;
        BoundaryTransform bt = null;
        int i = 0;
        for (String[] test : tests) {
            ++i;
            String newLocale = test[0];
            if (!newLocale.equals(locale)) {
                locale = newLocale;
                bt = BoundaryTransform.getTransform(locale, BoundaryUsage.general);
            }
            String pattern = test[1];
            int pos = pattern.indexOf("{0}");
            String result = pattern.replace("{0}", test[2]);
            String expected = test[3];
            String actual = bt.apply(result, pos);
            assertEquals(i + ") " + pattern, expected, actual);
        }
    }
}
