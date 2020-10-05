package org.unicode.cldr.unittest;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.FixedDecimal;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class TestPluralRuleGeneration extends TestFmwkPlus {
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
        System.out.println(i + ", " + d);
    }

    public void TestFilipino() {
        /*
         * <pluralRules locales="am bn fa gu hi kn mr zu"> <pluralRule
         * count="one">i = 0 or n = 1 @integer 0, 1 @decimal 0.0~1.0,
         * 0.00~0.04</pluralRule> <pluralRule count="other"> @integer 2~17, 100,
         * 1000, 10000, 100000, 1000000, … @decimal 1.1~2.6, 10.0, 100.0,
         * 1000.0, 10000.0, 100000.0, 1000000.0, …</pluralRule> </pluralRules>
         */
        PluralRules rules = PluralRules
            .createRules("one:v = 0 and i = 1,2,3 or v = 0 and i % 10 != 4,6,9 or v != 0 and f % 10 != 4,6,9");
        Object[][] test = { { 1, "one" }, { 3, "one" }, { 5, "one" },
            { 15, "one" }, { 4.5, "one" }, { 4, "other" },
            { 1.4, "other" }, };
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
                            PluralRules rules2 = PluralRules
                                .createRules("one: " + andPart);
                            boolean i = rules.computeLimited("one",
                                SampleType.INTEGER);
                            boolean d = rules.computeLimited("one",
                                SampleType.DECIMAL);
                            limitedSet.add((i ? "in" : "i∞") + " "
                                + (d ? "dn" : "d∞") + " : " + andPart);
                        }
                    }
                }
            }
        }
        for (String item : limitedSet) {
            System.out.println(item);
        }

    }

    public void TestEFixedDecimal() {
        PluralRules test;
        try {
            test = PluralRules.parseDescription("many: e = 0 and i != 0 and i % 1000000 = 0 and v = 0 or e != 0 .. 5 @integer 1000000");
        } catch (ParseException e) {
            throw new ICUException(e);
        }
        Object[][] checkItems = {
            // compare each group of items against one another, where a group is bounded by {},
            // source,  f,      v,  e,  plural (, skipString — if toString doesn't roundtrip)?
            {"2300000", 2300000d, 0, 0, "other"},
            {"2.3e6", 2300000d, 0, 6, "many"},
            {},
            {"1.20050e3",1200.5d, 2, 3, "other", "skipString"},
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

            final String source = (String)row[0];
            final double fExpected = (double)row[1];
            final int vExpected = (int)row[2];
            final int eExpected = (int)row[3];
            final String pluralExpected = (String)row[4];
            final boolean checkString = row.length < 6 || !"skipString".equals(row[5]);

            // basics

            FixedDecimal fromString = new FixedDecimal(source);

            final String fromToString = fromString.toString();
            if (checkString && !assertEquals(rowString + " source vs fromString", source, fromToString)) {
                fromString.toString(); // for debugging
            }
            if (!assertEquals(rowString + " double vs toString", fExpected, fromString.doubleValue())) {
                fromString.doubleValue(); // for debugging
            }
            String pluralActual = test.select(fromString);
            assertEquals(rowString + " plural category", pluralExpected, pluralActual);

            // previous in group

            Map<Operand, Double> operands = getOperandMap(fromString);
            if (lastFromString != null) {
                if (!assertTrue(lastSource + (checkString ? " < " :  " ≤ ") + source, lastFromString.compareTo(fromString) < (checkString ? 0 : 1))) {
                    lastFromString.compareTo(fromString); // for debugging
                }
                assertEquals("double " + lastSource + " vs " + source, lastFromString.doubleValue(), lastFromString.doubleValue());
                assertEquals("operands " + lastSource + " vs " + source, lastOperands, operands);
            }

            // different constructor

            FixedDecimal fromDoubleAndExponent = FixedDecimal.createWithExponent(fExpected, vExpected, eExpected);

            if (!assertEquals(rowString + " fromString vs fromDoubleAndExponent", fromString, fromDoubleAndExponent)) {
                FixedDecimal.createWithExponent(fExpected, vExpected, eExpected);
            }
            assertEquals(rowString + " double vs fromDoubleAndExponent", fExpected, fromDoubleAndExponent.doubleValue());

            assertEquals(rowString + " exponent, fromString vs fromDoubleAndExponent", fromString.getPluralOperand(Operand.e), fromDoubleAndExponent.getPluralOperand(Operand.e));

            assertEquals(rowString + " fromString vs fromDoubleAndExponent", 0, fromString.compareTo(fromDoubleAndExponent));


            lastSource = source;
            lastFromString = fromString;
            lastOperands = operands;
        }
    }

    private Map<Operand,Double> getOperandMap(FixedDecimal fixedDecimal) {
        Map<Operand,Double> result = new LinkedHashMap<>();
        for (Operand op : Operand.values()) {
            switch (op) {
            case e: case j:
                continue;
            }
            result.put(op, fixedDecimal.getPluralOperand(op));
        }
        return ImmutableMap.copyOf(result);
    }

    static final Set<Operand> orderedOperands = ImmutableSet.of(Operand.n, Operand.i, Operand.v, Operand.w, Operand.f, Operand.t, Operand.e);

    public void TestEFixedDecimal2() {
        UnlocalizedNumberFormatter unfScientific = NumberFormatter.with()
            .unit(MeasureUnit.KILOMETER)
            .unitWidth(UnitWidth.FULL_NAME)
            .notation(Notation.engineering());

        UnlocalizedNumberFormatter unfCompact = unfScientific.notation(Notation.compactLong());
        unfScientific = unfScientific.precision(Precision.minMaxSignificantDigits(1, 3));

        ImmutableSet<ULocale> locales = ImmutableSet.of(ULocale.ENGLISH, ULocale.FRENCH);
        ImmutableSet<String> tests = ImmutableSet.of(
            "1", "1.0", "1.00",
            "1.3", "1.30", "1.03", "1.230",
            "1200000", "1.2e6",
            "123e6",
            "123e5",
            "1200.50",
            "1.20050e3"
            );

        for (String test : tests) {
            final FixedDecimal source = new FixedDecimal(test);
            assertEquals("check toString", test, source.toString());
        }

        System.out.println();
        String ops = "";
        for (Operand op : orderedOperands) {
            ops += "\t" + op.toString();
        }
        System.out.println("locale\tsource"
            + ops
            + "\tpluralCat"
            );
        for (ULocale locale : locales) {
            PluralRules pr = PluralRules.forLocale(locale);

            for (String test : tests) {
                final FixedDecimal source = new FixedDecimal(test);
                System.out.println(locale
                    + "\t" + FixedDecimal.toSampleString(source)
                    + "\t" + pluralInfo(pr, source)
                    );
            }
            System.out.println();

        }
    }

    public void TestDecimalQuantity() {
        ImmutableSet<String> tests = ImmutableSet.of(
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
            //"1.20050e3",
            "1200.5",
            "1.2005e3",
            "1200000",
            "1.2e6",
            //"1.20e6",
            "1234567.8",
            "123456.78e1",
            "12.345678e5",
            "1.2345678e6",
            "1.20050e3"
            );

        boolean ok;
        for (int i = 0; i < 3; ++i) {
            for (String test : tests) {
                IFixedDecimal sourceFixedDecimal = new FixedDecimal(test);
                DecimalQuantity_DualStorageBCD sourceDecimalQuantity = quantityFromSampleString(test);
                switch(i) {
                case 0:
                    ok = assertEquals(test + " — check FixedDecimal toString", test, sourceFixedDecimal.toString());
                    if (!ok) {
                        sourceFixedDecimal = new FixedDecimal(test); // for debugging
                        sourceFixedDecimal.toString();
                    }
                    break;
                case 1:
                    ok = assertEquals(test + " — check quantity toScientificString",
                        test, FixedDecimal.toSampleString(sourceDecimalQuantity));
                    if (!ok) {
                        sourceFixedDecimal = quantityFromSampleString(test); // for debugging
                    }
                    break;
                case 2:
                    ok = assertEquals(test + " — check operands FixedDecimal vs DecimalQuantity", "",
                        showOperandDifferences("FD", sourceFixedDecimal, "DC", sourceDecimalQuantity));
                    break;
                }
            }
        }
    }

    public static String showOperandDifferences(String myTitle, IFixedDecimal me, String otherTitle, IFixedDecimal other) {
        StringBuilder result = new StringBuilder();
        for (Operand op : Operand.values()) {
            if (me.getPluralOperand(op) != other.getPluralOperand(op)) {
                if (result.length() != 0) {
                    result.append("; ");
                }
                result.append(op)
                .append(": "
                    + myTitle
                    + "=").append(me.getPluralOperand(op))
                .append(" "
                    + otherTitle
                    + "=").append(other.getPluralOperand(op))
                ;
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
        sourceQuantity.setMinFraction(v-exponent); //"1.20050e3" v should be 2, is 4
        sourceQuantity.adjustMagnitude(-exponent);
        sourceQuantity.adjustExponent(exponent);

        return sourceQuantity;
    }

    static final DecimalFormat nf = new DecimalFormat("0.#####");

    public CharSequence pluralInfo(PluralRules pr, final FixedDecimal formatted) {
        StringBuilder buffer = new StringBuilder();
        for (Operand op :orderedOperands) {
            double opValue = formatted.getPluralOperand(op);
            buffer.append(nf.format(opValue) + "\t");
        }
        buffer.append(pr.select(formatted));
        return buffer;
    }
}