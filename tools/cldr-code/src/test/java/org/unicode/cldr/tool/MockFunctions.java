package org.unicode.cldr.tool;

import com.google.common.collect.Sets;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.tool.MockMessageFormat.FunctionFactory;
import org.unicode.cldr.tool.MockMessageFormat.FunctionVariable;
import org.unicode.cldr.tool.MockMessageFormat.MFContext;
import org.unicode.cldr.tool.MockMessageFormat.OptionsMap;

public class MockFunctions {

    /** FunctionFactory & FunctionVariable for :string */
    static class StringFactory implements FunctionFactory {

        @Override
        public String getName() {
            return ":string";
        }

        @Override
        public FunctionVariable fromVariable(
                String variableName, MFContext context, OptionsMap options) {
            FunctionVariable variable = context.get(variableName);
            validate(options);
            return new StringVariable(variable.format(context), options);
        }

        @Override
        public FunctionVariable fromLiteral(String literal, OptionsMap options) {
            return fromInput(literal, options);
        }

        static final Set<String> ALLOWED = Set.of("casing");

        private void validate(OptionsMap options) {
            // simple for now
            if (!ALLOWED.containsAll(options.keySet())) {
                throw new IllegalArgumentException(
                        "Invalid options: " + Sets.difference(options.keySet(), ALLOWED));
            }
        }

        @Override
        public FunctionVariable fromInput(Object input, OptionsMap options) {
            validate(options);
            return new StringVariable(input.toString(), options);
        }

        @Override
        public boolean canSelect() {
            return true;
        }

        @Override
        public boolean canFormat() {
            return true;
        }
    }

    static class StringVariable extends FunctionVariable {
        private String value;

        public StringVariable(String string, OptionsMap options) {
            value = string;
            setOptions(options);
        }

        @Override
        public void setOptions(OptionsMap options) {
            super.setOptions(options);
        }

        @Override
        public int match(MFContext contact, String matchKey) {
            return value.equals(matchKey)
                    ? FunctionVariable.EXACT_MATCH
                    : FunctionVariable.NO_MATCH;
        }

        @Override
        public String format(MFContext context) {
            Object casing = getOptions().get("casing");
            if (casing == null) {
                return value;
            }
            final String caseOption = casing.toString();
            switch (caseOption) {
                case "upper":
                    return value.toUpperCase(context.locale);
                case "lower":
                    return value.toLowerCase(context.locale);
                default:
                    throw new IllegalArgumentException("Illegal casing=" + caseOption);
            }
        }

        @Override
        public String toString() {
            return "value=" + value + ", options=" + getOptions().toString();
        }
    }

    /** FunctionFactory & FunctionVariable for :number */
    static class NumberFactory implements FunctionFactory {

        @Override
        public String getName() {
            return ":number";
        }

        @Override
        public NumberVariable fromVariable(
                String variableName, MFContext context, OptionsMap options) {
            FunctionVariable variable = context.get(variableName);
            // In this case, we always
            if (!(variable instanceof NumberVariable)) {
                throw new IllegalArgumentException(getName() + " requires numbers");
            }
            validate(options);

            // Determine if any mutate.
            // In that case, they could modify the value, and modify the options
            // Otherwise...
            return new NumberVariable(
                    ((NumberVariable) variable).value, variable.getOptions().merge(options));
        }

        @Override
        public NumberVariable fromLiteral(String literal, OptionsMap options) {
            // needs to use an implementation-neutral parse
            return fromInput(new BigDecimal(literal), options);
        }

        @Override
        public NumberVariable fromInput(Object input, OptionsMap options) {
            if (!(input instanceof Number)) {
                throw new IllegalArgumentException(getName() + " requires numbers");
            }
            validate(options);
            return new NumberVariable((Number) input, options);
        }

        @Override
        public boolean canSelect() {
            return true;
        }

        @Override
        public boolean canFormat() {
            return true;
        }

        static final Set<String> ALLOWED =
                Set.of("numberingSystem", "maxFractionDigits", "minFractionDigits", "signDisplay");

        public void validate(OptionsMap options) {
            // simple for now
            if (!ALLOWED.containsAll(options.keySet())) {
                throw new IllegalArgumentException(
                        "Invalid options: " + Sets.difference(options.keySet(), ALLOWED));
            }
        }
    }

    /** A variable of information that results from applying number function (factory). */
    static class NumberVariable extends FunctionVariable {
        Number value;
        UnlocalizedNumberFormatter nf = NumberFormatter.with();
        String pluralCategory = null;
        boolean optionsApplied = false;

        @Override
        public String toString() {
            return "number="
                    + value
                    + ", keyword="
                    + pluralCategory
                    + ", optionsApplied="
                    + optionsApplied
                    + ", options="
                    + getOptions();
        }

        private NumberVariable(Number operand, OptionsMap options) {
            value = operand;
            setOptions(options);
        }

        @Override
        public int match(MFContext context, String matchKey) {
            if (matchKey.charAt(0) < 'A') {
                // hack for now; should perform a better comparison that matches
                // irrespective of the type of number
                return value.toString().equals(matchKey)
                        ? FunctionVariable.EXACT_MATCH
                        : FunctionVariable.NO_MATCH;
            }
            // TODO, look at select option to pick cardinal vs ordinal vs none.
            if (pluralCategory == null) {
                // get the plural category
                applyOptions(context);
                PluralRules rules =
                        PluralRules.forLocale(context.locale, PluralRules.PluralType.CARDINAL);
                IFixedDecimal fixedDecimal =
                        nf.locale(context.locale).format(value).getFixedDecimal();
                pluralCategory = rules.select(fixedDecimal);
            }
            return pluralCategory.equals(matchKey) ? 100 : FunctionVariable.NO_MATCH;
        }

        @Override
        public String format(MFContext context) {
            applyOptions(context);
            return nf.locale(context.locale).format(value).toString();
        }

        public void applyOptions(MFContext context) {
            if (optionsApplied) {
                return;
            }
            // for the options matching those in ICU NumberFormatter, we could drop after processing
            for (Entry<String, Object> entry : getOptions().entrySet()) {
                switch (entry.getKey()) {
                    case "maxFractionDigits":
                        nf = nf.precision(Precision.maxFraction(((Integer) entry.getValue())));
                        break;
                    case "minFractionDigits":
                        nf = nf.precision(Precision.minFraction(((Integer) entry.getValue())));
                        break;
                    case "numberingSystem":
                        nf =
                                nf.symbols(
                                        DecimalFormatSymbols.forNumberingSystem(
                                                context.locale,
                                                NumberingSystem.getInstanceByName(
                                                        entry.getValue().toString())));
                        break;
                    case "signDisplay":
                        nf =
                                nf.sign(
                                        SignDisplay.valueOf(
                                                entry.getValue()
                                                        .toString()
                                                        .toUpperCase(Locale.ROOT)));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Number doen't allow the option " + entry);
                }
            }
            optionsApplied = true;
        }
    }

    /** FunctionFactory & FunctionVariable for :number */
    static class MeasureFactory implements FunctionFactory {
        @Override
        public String getName() {
            return ":u:measure";
        }

        @Override
        public MeasureVariable fromVariable(
                String variableName, MFContext context, OptionsMap options) {
            FunctionVariable variable = context.get(variableName);
            // In this case, we always
            if (!(variable instanceof MeasureVariable)) {
                throw new IllegalArgumentException(getName() + " requires measures");
            }
            validate(options);

            // Determine if any mutate.
            // In that case, they could modify the value, and modify the options
            // Otherwise...
            return new MeasureVariable(
                    ((MeasureVariable) variable).value, variable.getOptions().merge(options));
        }

        @Override
        public MeasureVariable fromLiteral(String literal, OptionsMap options) {
            String[] parts = literal.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        getName() + " requires a literal in the form number/unicode_unit_id");
            }
            MeasureUnit unit = MeasureUnit.forIdentifier(parts[1]);
            Measure measure = new Measure(new BigDecimal(parts[0]), unit);
            return fromInput(measure, options);
        }

        @Override
        public MeasureVariable fromInput(Object input, OptionsMap options) {
            if (!(input instanceof Measure)) {
                throw new IllegalArgumentException(getName() + " requires measures");
            }
            validate(options);
            return new MeasureVariable((Measure) input, options);
        }

        @Override
        public boolean canSelect() {
            return false;
        }

        @Override
        public boolean canFormat() {
            return true;
        }

        static final Set<String> ALLOWED =
                Set.of(
                        "numberingSystem",
                        "maxFractionDigits",
                        "minFractionDigits",
                        "signDisplay",
                        "usage",
                        "width");

        public void validate(OptionsMap options) {
            // simple for now
            if (!ALLOWED.containsAll(options.keySet())) {
                throw new IllegalArgumentException(
                        "Invalid options: " + Sets.difference(options.keySet(), ALLOWED));
            }
        }
    }

    /** A variable of information that results from applying number function (factory). */
    static class MeasureVariable extends FunctionVariable {
        Measure value;
        String formatted = null;

        @Override
        public String toString() {
            return "value=" + value + ", options=" + getOptions();
        }

        private MeasureVariable(Measure operand, OptionsMap options) {
            value = operand;
            setOptions(options);
        }

        @Override
        public int match(MFContext context, String matchKey) {
            throw new UnsupportedOperationException(":u:measure doesn't support selection");
        }

        @Override
        public String format(MFContext context) {
            if (formatted == null) {
                UnlocalizedNumberFormatter nf = NumberFormatter.with();
                for (Entry<String, Object> entry : getOptions().entrySet()) {
                    final Object value1 = entry.getValue();
                    switch (entry.getKey()) {
                        case "maxFractionDigits":
                            nf = nf.precision(Precision.maxFraction(((Integer) value1)));
                            break;
                        case "minFractionDigits":
                            nf = nf.precision(Precision.minFraction(((Integer) value1)));
                            break;
                        case "numberingSystem":
                            nf.symbols(
                                    DecimalFormatSymbols.forNumberingSystem(
                                            context.locale,
                                            NumberingSystem.getInstanceByName(value1.toString())));
                            break;
                        case "signDisplay":
                            nf = nf.sign(SignDisplay.valueOf(value1.toString()));
                            break;
                        case "usage":
                            nf = nf.usage(value1.toString());
                            break;
                        case "width":
                            String stringValue = value1.toString().toUpperCase();
                            nf =
                                    nf.unitWidth(
                                            stringValue.equals("FULL")
                                                    ? UnitWidth.FULL_NAME
                                                    : UnitWidth.valueOf(stringValue));
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Number doen't allow the option " + entry);
                    }
                }
                final LocalizedNumberFormatter localized = nf.locale(context.locale);
                formatted = localized.format(value).toString();
            }
            return formatted;
        }
    }

    static final RegistryBuilder registry =
            new RegistryBuilder(new StringFactory(), new NumberFactory(), new MeasureFactory());

    static class RegistryBuilder {
        Map<String, FunctionFactory> lookup = new HashMap<>();

        public RegistryBuilder(FunctionFactory... factories) {
            for (FunctionFactory factory : factories) {
                lookup.put(factory.getName(), factory);
            }
        }
    }

    static FunctionFactory get(String functionName) {
        return registry.lookup.get(functionName);
    }
}
