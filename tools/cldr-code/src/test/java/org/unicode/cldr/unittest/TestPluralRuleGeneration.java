package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.unicode.cldr.icu.text.FixedDecimal;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.PluralSamples.Visitor;
import org.unicode.cldr.util.PluralUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

public class TestPluralRuleGeneration extends TestFmwkPlus {
    boolean SHOW = System.getProperty("TestPluralRuleGeneration:show") != null;

    CLDRConfig testInfo = CLDRConfig.getInstance();
    SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestPluralRuleGeneration().run(args);
    }

    public void TestGeneration() {
        /*
         * <pluralRules locales="am bn fa gu hi kn mr zu"> <pluralRule
         * count="one">i = 0 or n = 1 @integer 0, 1 @decimal 0.0~1.0,
         * 0.00~0.04</pluralRule> <pluralRule count="other"> @integer 2~17, 100,
         * 1000, 10000, 100000, 1000000, … @decimal 1.1~2.6, 10.0, 100.0,
         * 1000.0, 10000.0, 100000.0, 1000000.0, …</pluralRule> </pluralRules>
         */
        PluralRules rules = PluralRules.createRules("one:i = 0 or n = 1");
        boolean i = rules.computeLimited("one", SampleType.INTEGER);
        boolean d = rules.computeLimited("one", SampleType.DECIMAL);
        if (SHOW) {
            System.out.println(i + ", " + d);
        }
    }

    public void TestFilipino() {
        /*
         * <pluralRules locales="am bn fa gu hi kn mr zu"> <pluralRule
         * count="one">i = 0 or n = 1 @integer 0, 1 @decimal 0.0~1.0,
         * 0.00~0.04</pluralRule> <pluralRule count="other"> @integer 2~17, 100,
         * 1000, 10000, 100000, 1000000, … @decimal 1.1~2.6, 10.0, 100.0,
         * 1000.0, 10000.0, 100000.0, 1000000.0, …</pluralRule> </pluralRules>
         */
        PluralRules rules =
                PluralRules.createRules(
                        "one:v = 0 and i = 1,2,3 or v = 0 and i % 10 != 4,6,9 or v != 0 and f % 10 != 4,6,9");
        Object[][] test = {
            {1, "one"},
            {3, "one"},
            {5, "one"},
            {15, "one"},
            {4.5, "one"},
            {4, "other"},
            {1.4, "other"},
        };
    }

    public void TestAtoms() {
        CLDRConfig testInfo = CLDRConfig.getInstance();
        SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();
        Set<PluralRules> seen = new HashSet();
        Set<String> atoms = new TreeSet();
        Set<String> limitedSet = new TreeSet();
        for (PluralType type : SupplementalDataInfo.PluralType.values()) {
            for (String locale : supp.getPluralLocales(type)) {
                PluralInfo info = supp.getPlurals(locale, false);
                if (info == null) {
                    continue;
                }
                PluralRules rules = info.getPluralRules();
                if (seen.contains(rules)) {
                    continue;
                }
                seen.add(rules);
                for (String keyword : rules.getKeywords()) {
                    String rulesString = rules.getRules(keyword);
                    if (rulesString.isEmpty()) {
                        continue;
                    }

                    for (String orPart : rulesString.split("\\s*or\\s*")) {
                        for (String andPart : orPart.split("\\s*and\\s*")) {
                            PluralRules rules2 = PluralRules.createRules("one: " + andPart);
                            boolean i = rules.computeLimited("one", SampleType.INTEGER);
                            boolean d = rules.computeLimited("one", SampleType.DECIMAL);
                            limitedSet.add(
                                    (i ? "in" : "i∞") + " " + (d ? "dn" : "d∞") + " : " + andPart);
                        }
                    }
                }
            }
        }
        if (SHOW) {
            for (String item : limitedSet) {
                System.out.println(item);
            }
        }
    }

    public void TestEFixedDecimal() {
        PluralRules test;
        try {
            test =
                    PluralRules.parseDescription(
                            "many: e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0 .. 5 @integer 1000000");
        } catch (ParseException e) {
            throw new ICUException(e);
        }
        Object[][] checkItems = {
            // compare each group of items against one another, where a group is bounded by {},
            // source,  f,      v,  e,  plural (, skipString — if toString doesn't roundtrip)?
            {"2300000", 2300000d, 0, 0, "other"},
            {"2.3e6", 2300000d, 0, 6, "many"},
            {},
            {"1.20050e3", 1200.5d, 2, 3, "other", "skipString"},
            {},
            {"1200.5", 1200.5d, 1, 0, "other"},
            {"1.2005e3", 1200.5d, 1, 3, "other"},
            {},
            {"1200000", 1200000d, 0, 0, "other"},
            {"1.2e6", 1200000d, 0, 6, "many"},
            {"1.20e6", 1200000d, 0, 6, "many", "skipString"},
            {},
            {"1234567.8", 1234567.8, 1, 0, "other"},
            {"123456.78e1", 1234567.8, 1, 1, "other"},
            {"12.345678e5", 1234567.8, 1, 5, "other"},
            {"1.2345678e6", 1234567.8, 1, 6, "many"},
        };
        String lastSource = null;
        FixedDecimal lastFromString = null;
        Map<Operand, Double> lastOperands = null;
        for (Object[] row : checkItems) {
            if (row.length == 0) {
                // reset to different group
                lastSource = null;
                lastFromString = null;
                lastOperands = null;
                continue;
            }
            String rowString = Arrays.asList(row).toString();

            final String source = (String) row[0];
            final double fExpected = (double) row[1];
            final int vExpected = (int) row[2];
            final int eExpected = (int) row[3];
            final String pluralExpected = (String) row[4];
            final boolean checkString = row.length < 6 || !"skipString".equals(row[5]);

            // basics

            FixedDecimal fromString = new FixedDecimal(source);

            final String fromToString = fromString.toString();
            if (checkString
                    && !assertEquals(rowString + " source vs fromString", source, fromToString)) {
                fromString.toString(); // for debugging
            }
            if (!assertEquals(
                    rowString + " double vs toString", fExpected, fromString.doubleValue())) {
                fromString.doubleValue(); // for debugging
            }
            String pluralActual = test.select(fromString);
            assertEquals(rowString + " plural category", pluralExpected, pluralActual);

            // previous in group

            Map<Operand, Double> operands = getOperandMap(fromString);
            if (lastFromString != null) {
                if (!assertTrue(
                        lastSource + (checkString ? " < " : " ≤ ") + source,
                        lastFromString.compareTo(fromString) < (checkString ? 0 : 1))) {
                    lastFromString.compareTo(fromString); // for debugging
                }
                assertEquals(
                        "double " + lastSource + " vs " + source,
                        lastFromString.doubleValue(),
                        lastFromString.doubleValue());
                assertEquals("operands " + lastSource + " vs " + source, lastOperands, operands);
            }

            // different constructor

            FixedDecimal fromDoubleAndExponent =
                    FixedDecimal.createWithExponent(fExpected, vExpected, eExpected);

            if (!assertEquals(
                    rowString + " fromString vs fromDoubleAndExponent",
                    fromString,
                    fromDoubleAndExponent)) {
                FixedDecimal.createWithExponent(fExpected, vExpected, eExpected);
            }
            assertEquals(
                    rowString + " double vs fromDoubleAndExponent",
                    fExpected,
                    fromDoubleAndExponent.doubleValue());

            assertEquals(
                    rowString + " exponent, fromString vs fromDoubleAndExponent",
                    fromString.getPluralOperand(Operand.e),
                    fromDoubleAndExponent.getPluralOperand(Operand.e));

            assertEquals(
                    rowString + " fromString vs fromDoubleAndExponent",
                    0,
                    fromString.compareTo(fromDoubleAndExponent));

            lastSource = source;
            lastFromString = fromString;
            lastOperands = operands;
        }
    }

    private Map<Operand, Double> getOperandMap(FixedDecimal fixedDecimal) {
        Map<Operand, Double> result = new LinkedHashMap<>();
        for (Operand op : Operand.values()) {
            switch (op) {
                case e:
                case j:
                    continue;
            }
            result.put(op, fixedDecimal.getPluralOperand(op));
        }
        return ImmutableMap.copyOf(result);
    }

    static final Set<Operand> orderedOperands =
            ImmutableSet.of(
                    Operand.n, Operand.i, Operand.v, Operand.w, Operand.f, Operand.t, Operand.e);

    public void TestEFixedDecimal2() {
        UnlocalizedNumberFormatter unfScientific =
                NumberFormatter.with()
                        .unit(MeasureUnit.KILOMETER)
                        .unitWidth(UnitWidth.FULL_NAME)
                        .notation(Notation.engineering());

        UnlocalizedNumberFormatter unfCompact = unfScientific.notation(Notation.compactLong());
        unfScientific = unfScientific.precision(Precision.minMaxSignificantDigits(1, 3));

        ImmutableSet<ULocale> locales = ImmutableSet.of(ULocale.ENGLISH, ULocale.FRENCH);
        ImmutableSet<String> tests =
                ImmutableSet.of(
                        "1",
                        "1.0",
                        "1.00",
                        "1.3",
                        "1.30",
                        "1.03",
                        "1.230",
                        "1200000",
                        "1.2e6",
                        "123e6",
                        "123e5",
                        "1200.50",
                        "1.20050e3");

        for (String test : tests) {
            final FixedDecimal source = new FixedDecimal(test);
            assertEquals("check toString", test, source.toString());
        }

        System.out.println();
        String ops = "";
        for (Operand op : orderedOperands) {
            ops += "\t" + op.toString();
        }
        if (SHOW) {

            System.out.println("locale\tsource" + ops + "\tpluralCat");
        }

        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        for (ULocale locale : locales) {
            PluralRules pr = sdi.getPluralRules(locale, PluralRules.PluralType.CARDINAL);

            for (String test : tests) {
                final FixedDecimal source = new FixedDecimal(test);
                if (SHOW) {

                    System.out.println(
                            locale
                                    + "\t"
                                    + FixedDecimal.toSampleString(source)
                                    + "\t"
                                    + pluralInfo(pr, source));
                }
            }
            if (SHOW) {
                System.out.println();
            }
        }
    }

    public void TestDecimalQuantity() {
        ImmutableSet<String> tests =
                ImmutableSet.of(
                        "1000.5",
                        "1.0005e3",
                        "1",
                        "1.0",
                        "1.00",
                        "1.3",
                        "1.30",
                        "1.03",
                        "1.230",
                        "1200000",
                        "1.2e6",
                        "123e6",
                        "123e5",
                        "1200.5",
                        "1.2005e3",
                        "2300000",
                        "2.3e6",
                        // "1.20050e3",
                        "1200.5",
                        "1.2005e3",
                        "1200000",
                        "1.2e6",
                        // "1.20e6",
                        "1234567.8",
                        "123456.78e1",
                        "12.345678e5",
                        "1.2345678e6",
                        "1.20050e3");

        boolean ok;
        for (int i = 0; i < 3; ++i) {
            for (String test : tests) {
                IFixedDecimal sourceFixedDecimal = new FixedDecimal(test);
                DecimalQuantity_DualStorageBCD sourceDecimalQuantity =
                        quantityFromSampleString(test);
                switch (i) {
                    case 0:
                        ok =
                                assertEquals(
                                        test + " — check FixedDecimal toString",
                                        test,
                                        sourceFixedDecimal.toString());
                        if (!ok) {
                            sourceFixedDecimal = new FixedDecimal(test); // for debugging
                            sourceFixedDecimal.toString();
                        }
                        break;
                    case 1:
                        ok =
                                assertEquals(
                                        test + " — check quantity toScientificString",
                                        test,
                                        FixedDecimal.toSampleString(sourceDecimalQuantity));
                        if (!ok) {
                            sourceFixedDecimal = quantityFromSampleString(test); // for debugging
                        }
                        break;
                    case 2:
                        ok =
                                assertEquals(
                                        test + " — check operands FixedDecimal vs DecimalQuantity",
                                        "",
                                        showOperandDifferences(
                                                "FD",
                                                sourceFixedDecimal,
                                                "DC",
                                                sourceDecimalQuantity));
                        break;
                }
            }
        }
    }

    public static String showOperandDifferences(
            String myTitle, IFixedDecimal me, String otherTitle, IFixedDecimal other) {
        StringBuilder result = new StringBuilder();
        for (Operand op : Operand.values()) {
            if (me.getPluralOperand(op) != other.getPluralOperand(op)) {
                if (result.length() != 0) {
                    result.append("; ");
                }
                result.append(op)
                        .append(": " + myTitle + "=")
                        .append(me.getPluralOperand(op))
                        .append(" " + otherTitle + "=")
                        .append(other.getPluralOperand(op));
            }
        }
        return result.toString();
    }

    public static DecimalQuantity_DualStorageBCD quantityFromSampleString(String num) {
        final DecimalQuantity_DualStorageBCD sourceQuantity;
        int exponent = 0;
        int ePos = num.indexOf('e');
        if (ePos >= 0) {
            String exponentStr = num.substring(ePos + 1);
            exponent = Integer.parseInt(exponentStr);
            num = num.substring(0, ePos);
        }

        int v = FixedDecimal.getVisibleFractionCount(num) + exponent;

        BigDecimal altBD = new BigDecimal(num);
        altBD = altBD.movePointRight(exponent);
        sourceQuantity = new DecimalQuantity_DualStorageBCD(altBD);
        sourceQuantity.setMinFraction(v - exponent); // "1.20050e3" v should be 2, is 4
        sourceQuantity.adjustMagnitude(-exponent);
        sourceQuantity.adjustExponent(exponent);

        return sourceQuantity;
    }

    static final DecimalFormat nf = new DecimalFormat("0.#####");

    public CharSequence pluralInfo(PluralRules pr, final FixedDecimal formatted) {
        StringBuilder buffer = new StringBuilder();
        for (Operand op : orderedOperands) {
            double opValue = formatted.getPluralOperand(op);
            buffer.append(nf.format(opValue) + "\t");
        }
        buffer.append(pr.select(formatted));
        return buffer;
    }

    public void testLatvian() {
        /*
         * <pluralRules locales="am bn fa gu hi kn mr zu"> <pluralRule
         * count="one">i = 0 or n = 1 @integer 0, 1 @decimal 0.0~1.0,
         * 0.00~0.04</pluralRule> <pluralRule count="other"> @integer 2~17, 100,
         * 1000, 10000, 100000, 1000000, … @decimal 1.1~2.6, 10.0, 100.0,
         * 1000.0, 10000.0, 100000.0, 1000000.0, …</pluralRule> </pluralRules>
         */

        for (String ruleString :
                List.of(
                        "i % 10 = 0",
                        "n % 10 = 0",
                        "i % 100 = 11..19",
                        "n % 100 = 11..19",
                        "v = 2 and f % 100 = 11..19")) {
            PluralRules rules = PluralRules.createRules("many:" + ruleString);

            Multimap<String, FixedDecimal> data = TreeMultimap.create();
            for (double number :
                    List.of(
                            0d, 0.1d, 1.1d, 1.11d, 2.2d, 10d, 10.1d, 10.11d, 11d, 11.1d, 311d,
                            311.1d, 311.11d)) {

                FixedDecimal plain = new FixedDecimal(number);
                data.put(rules.select(plain), plain);
                long integerPart = (long) number;
                if (integerPart != number) {
                    FixedDecimal plainInt = new FixedDecimal(integerPart);
                    data.put(rules.select(plainInt), plainInt);
                }
                FixedDecimal x11 = new FixedDecimal(number, 2);
                data.put(rules.select(x11), x11);

                FixedDecimal x3 = new FixedDecimal(number, 3);
                data.put(rules.select(x3), x3);
            }
            logln(ruleString);
            System.out.println();
            data.entries().stream()
                    .filter(x -> x.getKey().equals("many"))
                    .forEach(x -> logln("match   " + "\t" + x.getValue()));
            // data.entries().stream().filter(x -> x.getKey().equals("other")).forEach(x ->
            // System.out.println("no-match " + "\t" + x.getValue()));
        }
    }

    public void testRuleOverlap() {
        // make sure the test does find overlaps
        Map<Set<String>, Double> shouldFail =
                checkRules(
                        new ConcurrentHashMap(),
                        "xxx",
                        PluralRules.createRules("one: n = 1..2; two: n=2..3"));
        assertNotEquals("Check that overlapping rules are detected", "{}", shouldFail.toString());

        CLDRConfig testInfo = CLDRConfig.getInstance();
        SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();
        ConcurrentHashMap<Map<String, String>, Boolean> seenLocale = new ConcurrentHashMap();
        supp.getPluralLocales().parallelStream()
                .forEach(
                        locale -> {
                            PluralInfo pluralInfo = supp.getPlurals(locale);
                            PluralRules rules = pluralInfo.getPluralRules();
                            Map<Set<String>, Double> shouldSucceed =
                                    checkRules(seenLocale, locale, rules);
                            assertEquals(
                                    locale + "\tCLDR plural rules overlap",
                                    "{}",
                                    shouldSucceed.toString());
                        });
        if (isVerbose()) {
            seenLocale.entrySet().stream().forEach(x -> logln(x.getKey().keySet().toString()));
        }
    }

    static List<Integer> fractionDigitsToCheck = List.of(1, 2, 3);
    static List<List<Integer>> integerRangesToCheck =
            List.of(List.of(0, 10000), List.of(100000), List.of(1000000));

    private Map<Set<String>, Double> checkRules(
            ConcurrentHashMap<Map<String, String>, Boolean> seenLocale,
            String locale,
            PluralRules rules) {
        Set<String> categories = rules.getKeywords();
        Map<String, String> categoryToRule = new TreeMap<>();
        for (String category : categories) {
            if (category.equals("other")) {
                continue;
            }
            String rule = rules.getRules(category);
            categoryToRule.put(category, rule);
        }
        boolean unseen = seenLocale.computeIfAbsent(categoryToRule, x -> true);
        if (!unseen) {
            return Map.of();
        }

        // check overlap

        if (categoryToRule.keySet().size() < 2) { // not needed for single rule (plus other)
            return Map.of();
        }

        Map<String, PluralRules> categoryToPRule = new TreeMap<>();

        for (Entry<String, String> entry : categoryToRule.entrySet()) {
            PluralRules pRule = PluralRules.createRules("many: " + entry.getValue());
            categoryToPRule.put(entry.getKey(), pRule);
        }

        Map<Set<String>, Double> overlapSamples = new HashMap<>();
        for (List<Integer> range : integerRangesToCheck) {
            int start = range.get(0);
            int last = range.size() < 2 ? start : range.get(1);
            for (int i = start; i <= last; ++i) {
                Set<String> triggers = new TreeSet<>(DtdData.countValueOrder);
                checkRules2(categoryToPRule, i, null, triggers);

                for (int fractionDigits : fractionDigitsToCheck) { // up to 3 fractional digits
                    // Iterate first from X.0 .. X.9, then X.00 .. X.99, then X.000 .. X.999
                    // That's because trailing zeros make a difference
                    double limit = Math.pow(10, fractionDigits);
                    for (int fraction = 0; fraction < limit; ++fraction) {
                        FixedDecimal fd = new FixedDecimal(i + fraction / limit, fractionDigits);
                        checkRules2(categoryToPRule, i, fd, triggers);
                    }
                }
                if (triggers.size() > 1) {
                    if (!overlapSamples.containsKey(triggers)) {
                        overlapSamples.put(triggers, (double) i);
                    }
                }
            }
        }
        return overlapSamples;
    }

    private void checkRules2(
            Map<String, PluralRules> categoryToPRule,
            int i,
            FixedDecimal fd,
            Set<String> triggers) {
        for (Entry<String, PluralRules> entry : categoryToPRule.entrySet()) {
            PluralRules pRule = entry.getValue();
            String tempCat = fd == null ? pRule.select(i) : pRule.select(fd);
            if (tempCat.charAt(0) == 'm') {
                triggers.add(entry.getKey());
            }
        }
    }

    public void testRuleComponents() {
        CLDRConfig testInfo = CLDRConfig.getInstance();
        SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();
        ConcurrentHashMap<Map<String, String>, Boolean> seenLocale = new ConcurrentHashMap();
        final Multimap<String, String> components =
                TreeMultimap.create(PluralUtilities.PLURAL_RELATION, Ordering.natural());
        PluralUtilities.getRepresentativeToLocales().keySet().stream()
                .forEach(
                        locale -> {
                            PluralInfo pluralInfo = supp.getPlurals(locale);
                            PluralRules rules = pluralInfo.getPluralRules();
                            Set<String> categories = rules.getKeywords();
                            for (String category : categories) {
                                String rule = rules.getRules(category);
                                PluralUtilities.AND_OR.splitToList(rule).stream()
                                        .forEach(x -> components.put(x, locale));
                            }
                        });
        if (SHOW) {
            System.out.println();
            components.asMap().entrySet().stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            x.getKey() + "\t" + Joiners.SP.join(x.getValue())));
            System.out.println();
            for (Entry<String, String> entry :
                    PluralUtilities.getCategorySetToRepresentativeLocales().entries()) {
                System.out.println(
                        Joiners.TAB.join(
                                entry.getKey(),
                                entry.getValue(),
                                Joiners.SP.join(
                                        PluralUtilities.getRepresentativeToLocales()
                                                .get(entry.getValue()))));
            }
        }
    }

    static final boolean debugSample = false;

    /**
     * A value representing a limited range of decimal numbers, targeted at use with plurals. These
     * are similar to FixedDecimals, but have additional capabilities, useful for CLDR. The number
     * of digits is limited to 19, whereby some of those can be fractional digits (including
     * trailing zeros). So 1.0 ≠ 1
     */
    static class VisibleDecimal implements Comparable<VisibleDecimal> {
        public long getDigits() {
            return digits;
        }

        public int getFractionCount() {
            return visibleDecimalCount;
        }

        public VisibleDecimal(long digits, int fractionCount) {
            this.digits = digits;
            this.factor = pow10(fractionCount);
            this.visibleDecimalCount = fractionCount;
        }

        public VisibleDecimal(FixedDecimal fixedDecimal) {
            visibleDecimalCount = fixedDecimal.getVisibleDecimalDigitCount();
            factor = pow10(visibleDecimalCount);
            digits = fixedDecimal.getIntegerValue() * factor + fixedDecimal.getDecimalDigits();
        }

        public FixedDecimal toFixedDecimal() {
            return new FixedDecimal(digits, visibleDecimalCount);
        }

        private final long digits;
        private final int factor;
        private final int visibleDecimalCount;

        @Override
        public boolean equals(Object obj) {
            VisibleDecimal that = (VisibleDecimal) obj;
            return digits == that.digits && visibleDecimalCount == that.visibleDecimalCount;
        }

        @Override
        public int hashCode() {
            return (int) ((digits << 16) ^ visibleDecimalCount);
        }

        @Override
        public int compareTo(VisibleDecimal o) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String toString() {
            return format(digits, visibleDecimalCount);
        }
    }

    /** A range of VisibleDecimals that have the same factionDigitCount */
    static class MagnitudeRange {
        VisibleDecimal first;
        long last = 0;

        enum Status {
            added,
            makeNewRange
        }

        /**
         * Add an item. If the visible decimal count is different than the others, fail.
         *
         * @param additional
         * @return
         */
        Status add(FixedDecimal additional) {
            if (first == null) {
                first = new VisibleDecimal(additional);
                last = first.digits;
                return Status.added;
            } else {
                if (additional.getVisibleDecimalDigitCount() != first.visibleDecimalCount) {
                    return Status.makeNewRange;
                }

                // if it is right after last, then add it

                long current =
                        additional.getIntegerValue() * first.factor + additional.getDecimalDigits();
                if (current == last + 1) {
                    last = current;
                    return Status.added;
                }

                // otherwise signal that we need a new range

                return Status.makeNewRange;
            }
        }

        Status addRange(FixedDecimal first_, FixedDecimal last_) {
            if (first == null) {
                if (first_.getVisibleDecimalDigitCount() != last_.getVisibleDecimalDigitCount()) {
                    return Status.makeNewRange;
                }
                first = new VisibleDecimal(first_);
            } else { // first != null
                if (first_.getVisibleDecimalDigitCount() != first.visibleDecimalCount
                        || first_.getVisibleDecimalDigitCount() != first.visibleDecimalCount) {
                    return Status.makeNewRange;
                }
                add(first_);
            }
            this.last = last_.getIntegerValue() * first.factor + last_.getDecimalDigits();
            return Status.added;
        }

        @Override
        public String toString() {
            if (first.digits == last) {
                return first.toString();
            } else {
                return first.toString() + "~" + format(last, first.visibleDecimalCount);
            }
        }
    }

    /**
     * Formats a long scaled by visibleDigits. Doesn't use standard double etc formatting it is
     * easier to control the formatting this way.
     *
     * @param value
     * @return
     */
    public static String format(long value, int visibleDigits) {
        String result = String.valueOf(value);
        if (visibleDigits == 0) {
            return result;
        } else if (visibleDigits < result.length()) {
            return result.substring(0, result.length() - visibleDigits)
                    + "."
                    + result.substring(result.length() - visibleDigits);
        } else {
            return "0." + "0".repeat(visibleDigits - result.length()) + result;
        }
    }

    static class Ranges {
        private List<MagnitudeRange> ranges = new ArrayList<>();
        private MagnitudeRange inProcess = null;

        void add(FixedDecimal additional) {
            if (inProcess == null) {
                inProcess = new MagnitudeRange();
            }
            switch (inProcess.add(additional)) {
                case makeNewRange:
                    {
                        ranges.add(inProcess);
                        inProcess = new MagnitudeRange();
                        inProcess.add(additional);
                        break;
                    }
            }
        }

        private void finish() {
            if (inProcess != null) {
                ranges.add(inProcess);
                inProcess = null;
            }
        }

        public int size() {
            return ranges.size();
        }

        @Override
        public String toString() {
            finish();
            return Joiners.SP.join(ranges);
        }
    }

    static int pow10(int exponent) {
        switch (exponent) {
            case 0:
                return 1;
            case 1:
                return 10;
            case 2:
                return 100;
            case 3:
                return 1000;
            case 4:
                return 10000;
            default:
                return (int) Math.pow(10, exponent);
        }
    }

    String showKey(int combined) {
        int pow = combined / 100;
        int frac = combined % 100;
        return Joiner.on("")
                .join(
                        new FixedDecimal(pow == 0 ? 0 : pow10(pow), frac),
                        "-",
                        new FixedDecimal(pow10(1 + pow) - 1, frac));
    }

    public void checkSampleSpeed() {

        //        Ranges ranges = new Ranges();
        //        for (int f : List.of(0,1,2,4,5,7, 100, 101, 102)) {
        //            ranges.add(new FixedDecimal(f));
        //        }
        //        for (int f : List.of(0,1,2,4,5,7)) {
        //            ranges.add(new FixedDecimal(3+f/10.0,1));
        //        }
        //        System.out.println("\n" + ranges);
        //        if (true) return;

        for (Entry<String, Set<Count>> entry :
                PluralUtilities.getRepresentativeToCountSet().entrySet()) {
            String representative = entry.getKey();
            if (debugSample && !representative.equals("en")) continue;
            Collection<String> representedLocales =
                    PluralUtilities.getRepresentativeToLocales().get(representative);
            PluralRules rules =
                    PluralUtilities.getRepresentativeToPluralRules().get(representative);
            Set<Count> countSets = entry.getValue();

            Map<PluralInfo.Count, Multimap<Integer, FixedDecimal>> data = new TreeMap<>();
            TreeMultimap.create();
            Visitor visitor =
                    Visitor.create(
                            rules,
                            (FixedDecimal x, String y) -> {
                                Count count = PluralInfo.Count.valueOf(y);
                                Multimap<Integer, FixedDecimal> data2 = data.get(count);
                                if (data2 == null) {
                                    data.put(count, data2 = TreeMultimap.create());
                                }
                                long intValue = x.getIntegerValue();
                                int ddc = x.getVisibleDecimalDigitCount();
                                int key = ((int) Math.log10(intValue)) * 100 + ddc;
                                data2.put(key, x);
                                return Visitor.Action.proceed;
                            },
                            !debugSample
                                    ? null
                                    : (FixedDecimal x) ->
                                            x.getVisibleDecimalDigitCount() == 2
                                                    && x.getIntegerValue() == 1);

            for (List<Integer> range : integerRangesToCheck) {
                int start = range.get(0);
                int last = range.size() < 2 ? start : range.get(1);
                visitor.handle(start, last, 3);
            }

            System.out.println();
            System.out.println(Joiners.TAB.join(countSets, representative, representedLocales));
            data.entrySet().stream()
                    .forEach(
                            x -> { // Multimap<Integer, FixedDecimal>
                                List<Ranges> firstNElements = new ArrayList<>();
                                x.getValue().asMap().entrySet().stream()
                                        .forEach(
                                                y -> {
                                                    Integer digitCount = y.getKey();
                                                    Collection<FixedDecimal> set = y.getValue();
                                                    Ranges ranges = new Ranges();
                                                    for (FixedDecimal fd : set) {
                                                        ranges.add(fd);
                                                        if (ranges.size() > 5) {
                                                            break;
                                                        }
                                                    }
                                                    firstNElements.add(ranges);

                                                    System.out.println(
                                                            Joiners.TAB.join(
                                                                    x.getKey(),
                                                                    showKey(y.getKey()),
                                                                    ranges));
                                                });
                                System.out.println(
                                        Joiners.TAB.join(
                                                x.getKey(), x.getValue().size(), firstNElements));
                            });
        }
    }
}
