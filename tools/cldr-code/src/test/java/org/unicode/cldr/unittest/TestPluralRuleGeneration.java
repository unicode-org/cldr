package org.unicode.cldr.unittest;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
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
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PluralUtilities;
import org.unicode.cldr.util.PluralUtilities.KeySampleRanges;
import org.unicode.cldr.util.PluralUtilities.SampleRange;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

public class TestPluralRuleGeneration extends TestFmwkPlus {

    static final boolean debugSample =
            System.getProperty("TestPluralRuleGeneration:debugSample") != null;
    static final boolean SHOW = System.getProperty("TestPluralRuleGeneration:show") != null;

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
            CldrUtility.showIf(SHOW, i, ", ", d);
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
                CldrUtility.showIf(SHOW, item);
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

        CldrUtility.showIf(SHOW);
        String ops = "";
        for (Operand op : orderedOperands) {
            ops += "\t" + op.toString();
        }
        if (SHOW) {

            CldrUtility.showIf(SHOW, "locale\tsource", ops, "\tpluralCat");
        }

        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        for (ULocale locale : locales) {
            PluralRules pr = sdi.getPluralRules(locale, PluralRules.PluralType.CARDINAL);

            for (String test : tests) {
                final FixedDecimal source = new FixedDecimal(test);
                if (SHOW) {

                    CldrUtility.showIf(
                            SHOW,
                            locale
                                    + "\t"
                                    + FixedDecimal.toSampleString(source)
                                    + "\t"
                                    + pluralInfo(pr, source));
                }
            }
            if (SHOW) {
                CldrUtility.showIf(SHOW);
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
                DecimalQuantity sourceDecimalQuantity =
                        DecimalQuantity_DualStorageBCD.fromExponentString(test);
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
                            sourceFixedDecimal =
                                    DecimalQuantity_DualStorageBCD.fromExponentString(
                                            test); // for debugging
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

    private String showOperandDifferences(
            String myTitle, IFixedDecimal me, String otherTitle, IFixedDecimal other) {
        StringBuilder result = new StringBuilder();
        for (Operand op : Operand.values()) {
            if (op == op.c) {
                logKnownIssue("23146", "FixedDecimal doesn't implement the 'c' operand");
                continue;
            }
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
            CldrUtility.showIf(SHOW);
            data.entries().stream()
                    .filter(x -> x.getKey().equals("many"))
                    .forEach(x -> logln("match   " + "\t" + x.getValue()));
            // data.entries().stream().filter(x -> x.getKey().equals("other")).forEach(x ->
            // CldrUtility.show("no-match " + "\t" + x.getValue()));
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

        for (final PluralType pluralType : PluralType.values()) {

            ConcurrentHashMap<Map<String, String>, Boolean> seenLocale = new ConcurrentHashMap();

            supp.getPluralLocales().parallelStream()
                    .forEach(
                            locale -> {
                                PluralInfo pluralInfo = supp.getPlurals(pluralType, locale);
                                PluralRules rules = pluralInfo.getPluralRules();
                                Map<Set<String>, Double> shouldSucceed =
                                        checkRules(seenLocale, locale, rules);
                                String message = locale + "\tCLDR " + pluralType + " rules overlap";
                                String actual = shouldSucceed.toString();
                                if (!"{}".equals(actual)) {
                                    warnln(message + ": " + actual);
                                }
                            });
            if (isVerbose()) {
                seenLocale.entrySet().stream().forEach(x -> logln(x.getKey().keySet().toString()));
            }
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
        for (PluralType pluralType : PluralType.values()) {
            CldrUtility.showIf(SHOW, "\nPLURAL TYPE: ", pluralType);
            PluralUtilities.getRepresentativeToLocales(pluralType).keySet().stream()
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
                CldrUtility.showIf(SHOW);
                components.asMap().entrySet().stream()
                        .forEach(
                                x ->
                                        CldrUtility.showIf(
                                                SHOW,
                                                x.getKey(),
                                                "\t",
                                                Joiners.SP.join(x.getValue())));
                CldrUtility.showIf(SHOW);
                for (Entry<Set<Count>, String> entry :
                        PluralUtilities.getCountSetToRepresentative(pluralType).entries()) {
                    CldrUtility.showIf(
                            SHOW,
                            Joiners.TAB.join(
                                    entry.getKey(),
                                    entry.getValue(),
                                    Joiners.SP.join(
                                            PluralUtilities.getRepresentativeToLocales(pluralType)
                                                    .get(entry.getValue()))));
                }
            }
        }
    }

    //    public void testRanges() {
    //        PluralUtilities.Ranges ranges = new PluralUtilities.Ranges();
    //        for (int f : List.of(0, 1, 2, 4, 5, 7, 100, 101, 102)) {
    //            ranges.add(new FixedDecimal(f));
    //        }
    //        for (int f : List.of(0, 1, 2, 4, 5, 7)) {
    //            ranges.add(new FixedDecimal(3 + f / 10.0, 1));
    //        }
    //        assertEquals("ranges", ranges.toString(), "0~2 4~5 7 100~102 3.0~3.2 3.4~3.5 3.7");
    //
    //        List<VisibleDecimal> testOrder = List.of(new PluralUtilities.VisibleDecimal(11, 0),
    // new PluralUtilities.VisibleDecimal(12, 0), new PluralUtilities.VisibleDecimal(111, 1));
    //        VisibleDecimal lastItem = null;
    //        for (VisibleDecimal item : testOrder) {
    //            if (lastItem != null) {
    //                assertEquals("", 1, item.compareTo(lastItem));
    //            }
    //            lastItem = item;
    //        }
    //    }

    public void checkSampleSpeed() {
        for (PluralType pluralType : PluralType.values()) {
            CldrUtility.showIf(SHOW, "\nPLURAL TYPE: ", pluralType);
            for (Entry<Set<Count>, String> entry :
                    PluralUtilities.getCountSetToRepresentative(pluralType).entries()) {
                String locale = entry.getValue();

                PluralRules pluralRules =
                        PluralUtilities.getRepresentativeToPluralRules(pluralType).get(locale);
                CldrUtility.showIf(SHOW);
                CldrUtility.showIf(
                        SHOW,
                        locale
                                + "\t"
                                + PluralUtilities.getRepresentativeToLocales(pluralType)
                                        .get(locale));
                Set<Count> counts =
                        PluralUtilities.getRepresentativeToCountSet(pluralType).get(locale);
                for (Count count : counts) {
                    if (count == Count.other) {
                        continue;
                    }
                    CldrUtility.showIf(SHOW, count, ":\t", pluralRules.getRules(count.toString()));
                }
                Map<Count, KeySampleRanges> values =
                        PluralUtilities.getSamplesFromPluralRules(pluralType, pluralRules);
                for (Entry<Count, KeySampleRanges> entry2 : values.entrySet()) {
                    CldrUtility.showIf(SHOW, entry2.getKey(), ":\t", entry2.getValue());
                }
            }
        }
    }

    /** See if any rules for representatives give identical results: */
    public void testIdenticalResults() {
        for (PluralType pluralType : PluralType.values()) {
            CldrUtility.showIf(SHOW, "\nPLURAL TYPE: ", pluralType);
            // make a reverse mapping
            Multimap<Map<Count, KeySampleRanges>, String> reverse = HashMultimap.create();
            for (Entry<String, PluralRules> entry :
                    PluralUtilities.getRepresentativeToPluralRules(pluralType).entrySet()) {
                Map<Count, KeySampleRanges> pluralSamples =
                        PluralUtilities.getSamplesFromPluralRules(pluralType, entry.getValue());
                reverse.put(pluralSamples, entry.getKey());
            }
            for (Entry<Map<Count, KeySampleRanges>, Collection<String>> entry :
                    reverse.asMap().entrySet()) {
                if (!assertEquals(entry.getValue().toString(), 1, entry.getValue().size())) {
                    warnln(
                            "\nCould be that the samples in PluralRule utilities needs extending, "
                                    + "or that there are gratuitious differences between rule formulations (like the order of AND or OR clauses.");
                    boolean isFirst = true;
                    for (String locale : entry.getValue()) {
                        if (isFirst) {
                            CldrUtility.showIf(
                                    SHOW,
                                    PluralUtilities.format(
                                            pluralType,
                                            PluralUtilities.getSamplesForLocale(
                                                    pluralType, locale)));
                            isFirst = false;
                        }
                        CldrUtility.showIf(SHOW, locale);
                        CldrUtility.showIf(
                                SHOW,
                                PluralUtilities.format(
                                        PluralUtilities.getRepresentativeToPluralRules(pluralType)
                                                .get(locale)));
                    }
                }
            }
        }
    }

    public void testSingles() {
        for (final PluralType pluralType : PluralType.values()) {
            // CldrUtility.show(PluralUtilities.getSamplesForLocale(pluralType, "zh"));
            Multimap<String, String> visibleSamplesMultimap = TreeMultimap.create();
            for (Entry<Set<Count>, String> entry :
                    PluralUtilities.getCountSetToRepresentative(pluralType).entries()) {
                Set<Count> countSet = entry.getKey();
                String representativeLocale = entry.getValue();
                CldrUtility.showIf(SHOW, "\n", pluralType, "\t", countSet);
                CldrUtility.showIf(
                        SHOW,
                        "LOCALES:\t["
                                + Joiners.SP.join(
                                        PluralUtilities.getRepresentativeToLocales(pluralType)
                                                .get(representativeLocale))
                                + "]");
                String formattedRules =
                        PluralUtilities.format(
                                PluralUtilities.getRepresentativeToPluralRules(pluralType)
                                        .get(representativeLocale));
                CldrUtility.showIf(
                        SHOW,
                        formattedRules.isBlank() ? "RULES:\tnone" : "RULES:\n" + formattedRules);
                CldrUtility.showIf(SHOW, "SAMPLES:");
                Map<Count, KeySampleRanges> pluralSamples =
                        PluralUtilities.getSamplesForLocale(pluralType, representativeLocale);
                List<String> samplesPerCount = new ArrayList<>();
                for (Entry<Count, KeySampleRanges> entry2 : pluralSamples.entrySet()) {
                    Count count = entry2.getKey();

                    List<String> samplesPerKey = new ArrayList<>();
                    int keyLimit = 99;
                    for (Entry<Integer, Collection<SampleRange>> pair :
                            entry2.getValue().setIterable()) {
                        if (--keyLimit < 0) {
                            if ("…".equals(samplesPerKey.get(samplesPerKey.size() - 1))) {
                                samplesPerKey.add("…");
                            }
                            break;
                        }
                        // int key = pair.getKey(); // unneeded

                        int rangePerKeyLimit = 3;
                        for (SampleRange sr : pair.getValue()) {
                            if (--rangePerKeyLimit < 0) {
                                samplesPerKey.add("…");
                                break;
                            }
                            samplesPerKey.add(sr.toString());
                        }
                        samplesPerKey.add(";");
                    }
                    samplesPerCount.add(count + "\t" + Joiners.SP.join(samplesPerKey));
                }
                String joinedSamples = Joiners.N.join(samplesPerCount);
                CldrUtility.showIf(SHOW, joinedSamples);
                visibleSamplesMultimap.put(joinedSamples, representativeLocale);
            }
            CldrUtility.showIf(SHOW, "\nDuplicates\n");
            for (Entry<String, Collection<String>> entry :
                    visibleSamplesMultimap.asMap().entrySet()) {
                if (entry.getValue().size() <= 1) {
                    continue;
                }
                errln(
                        "Duplicate rules for representatives: "
                                + entry.getValue()
                                + "\n"
                                + entry.getKey());
                List<String> list = List.copyOf(entry.getValue());
                Pair<String, String> problem =
                        PluralUtilities.findDifference(pluralType, list.get(0), list.get(1));
                CldrUtility.showIf(SHOW, "Problem: ", problem);
                CldrUtility.showIf(
                        SHOW, PluralUtilities.getSamplesForLocale(pluralType, list.get(0)));
                CldrUtility.showIf(
                        SHOW, PluralUtilities.getSamplesForLocale(pluralType, list.get(1)));
                CldrUtility.showIf(SHOW);
            }
        }
    }
}
