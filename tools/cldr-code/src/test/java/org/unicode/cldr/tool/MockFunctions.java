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
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.MockMessageFormat.Expression;
import org.unicode.cldr.tool.MockMessageFormat.MatchComparison;
import org.unicode.cldr.tool.MockMessageFormat.MfContext;
import org.unicode.cldr.tool.MockMessageFormat.MfFunction;
import org.unicode.cldr.tool.MockMessageFormat.MfResolvedVariable;
import org.unicode.cldr.tool.MockMessageFormat.OptionsMap;

public class MockFunctions {

    static final RegistryBuilder registry =
            new RegistryBuilder(new StringFunction(), new NumberFunction(), new MeasureFunction());

    static class RegistryBuilder {
        Map<String, MfFunction> lookup = new ConcurrentHashMap<>();

        public RegistryBuilder(MfFunction... factories) {
            for (MfFunction factory : factories) {
                lookup.put(factory.getName(), factory);
            }
        }
    }

    /*
     * To register additional functions
     */
    public void register(MfFunction... factories) {
        for (MfFunction factory : factories) {
            registry.lookup.put(factory.getName(), factory);
        }
    }

    /*
     * To access a function by name
     */
    static MfFunction get(String functionName) {
        return registry.lookup.get(functionName);
    }

    /** MfFunction & MfVariable for :string */
    static class StringFunction implements MfFunction {

        static class StringVariable extends MfResolvedVariable {
            private String baseValue;

            public StringVariable(String string, OptionsMap options) {
                baseValue = string;
                setOptions(options);
            }

            @Override
            public void setOptions(OptionsMap options) {
                super.setOptions(options);
            }

            @Override
            public MatchComparison match(MfContext context, String matchKey, String bestMatchKey) {
                return baseValue.equals(matchKey) ? MatchComparison.BEST : MatchComparison.FAIL;
            }

            @Override
            public String format(MfContext context) {
                for (Entry<String, Object> entry : getOptions().entrySet()) {
                    switch (entry.getKey()) {
                        case "u:casing":
                            final String caseOption = entry.getValue().toString();
                            switch (caseOption) {
                                case "upper":
                                    return baseValue.toUpperCase(context.getLocale());
                                case "lower":
                                    return baseValue.toLowerCase(context.getLocale());
                                default:
                                    throw new IllegalArgumentException(
                                            "Illegal casing=" + caseOption);
                            }
                        default:
                            throw new IllegalArgumentException(
                                    ":string doen't allow the option " + entry);
                    }
                }
                return baseValue;
            }

            @Override
            public String toString() {
                return "[baseValue="
                        + baseValue
                        + (getOptions().isEmpty() ? "" : ", options=" + getOptions().toString())
                        + "]";
            }
        }

        @Override
        public String getName() {
            return ":string";
        }

        static final Set<String> ALLOWED = Set.of("u:casing");

        private void validate(OptionsMap options) {
            // simple for now
            if (!ALLOWED.containsAll(options.keySet())) {
                throw new IllegalArgumentException(
                        "Invalid options: " + Sets.difference(options.keySet(), ALLOWED));
            }
        }

        @Override
        public boolean canSelect() {
            return true;
        }

        @Override
        public boolean canFormat() {
            return true;
        }

        @Override
        public MfResolvedVariable from(Expression expression, MfContext mfContext) {
            switch (expression.type) {
                case literal:
                    return new StringVariable(expression.operandId.toString(), expression.map);
                case input:
                    return new StringVariable(
                            mfContext.getInput(expression.operandId).toString(), expression.map);
                case variable:
                    MfResolvedVariable sourceVariable =
                            mfContext.boundVariables.get(expression.operandId);
                    if (expression.map.isEmpty()) {
                        return sourceVariable;
                    }
                    OptionsMap newMap = sourceVariable.getOptions().merge(expression.map);
                    if (newMap.equals(sourceVariable.getOptions())) {
                        return sourceVariable;
                    }
                    return new StringVariable(sourceVariable.format(mfContext), newMap);
            }
            return null;
        }
    }

    /** MfFunction & MfVariable for :number */
    static class NumberFunction implements MfFunction {

        /** A variable of information that results from applying number function (factory). */
        static class NumberVariable extends MfResolvedVariable {
            Number value;
            Number offsettedValue;
            UnlocalizedNumberFormatter nf = NumberFormatter.with();
            String pluralCategory = null;
            int offset = 0;
            NumberingSystem numberingSystem = null;
            private boolean choice = false;

            @Override
            public String toString() {
                return "["
                        + "baseValue="
                        + value
                        + (offsettedValue.equals(value) ? "" : ", offsettedValue=" + offsettedValue)
                        + (pluralCategory == null ? "" : ", pluralCategory=" + pluralCategory)
                        + (offset == 0 ? "" : ", offset=" + offset)
                        + (getOptions().isEmpty() ? "" : ", options=" + getOptions())
                        + "]";
            }

            private NumberVariable(Number operand, OptionsMap options) {
                value = operand;
                offsettedValue = operand;
                setOptions(options);
                applyOptions();
            }

            @Override
            public MatchComparison match(MfContext context, String matchKey, String bestMatchKey) {
                if (Double.isNaN(value.doubleValue())) {
                    return MatchComparison.FAIL;
                }
                // numbers only have 4 results, so they are easy to compare
                MatchComparison result1 = matchKey(context, matchKey);
                if (result1 == MatchComparison.FAIL || result1 == MatchComparison.BEST) {
                    return result1;
                }
                if (bestMatchKey == null) {
                    return MatchComparison.BETTER;
                }
                // If the best key match were FAIL, it would be null instead, and handled at a
                // higher level
                MatchComparison bestResult = matchKey(context, bestMatchKey);
                int comparison = result1.compareTo(bestResult);
                return comparison < 0
                        ? MatchComparison.WORSE
                        : comparison > 0 ? MatchComparison.BETTER : MatchComparison.SAME;
            }

            static final Pattern HALFOPEN = Pattern.compile("[\\[(]([^,]*),([^)]*)[)\\]]");

            public MatchComparison matchKey(MfContext context, String matchKey) {
                char firstChar = matchKey.charAt(0);
                if (firstChar == '[' || firstChar == '(') {
                    Matcher matcher = HALFOPEN.matcher(matchKey);
                    if (!matcher.matches()) {
                        throw new IllegalArgumentException(
                                "Match key is not a valid half-open interval: " + matchKey);
                    }
                    final String lowerBound = matcher.group(1);
                    if (!lowerBound.isEmpty()) { // "" = -infinity
                        BigDecimal lowerBoundBG = new BigDecimal(lowerBound);
                        int lowerToValue = compare(lowerBoundBG, value);
                        if (lowerToValue > 0 || lowerToValue == 0 && firstChar == '(') {
                            return MatchComparison.FAIL;
                        }
                    }
                    final String upperBound = matcher.group(2);
                    if (!upperBound.isEmpty()) { // "" = infinity
                        char lastChar = matchKey.charAt(matchKey.length() - 1);
                        BigDecimal upperBoundBG = new BigDecimal(upperBound);
                        int upperToValue = compare(upperBoundBG, value);
                        if (upperToValue < 0 || upperToValue == 0 && lastChar == ')') {
                            return MatchComparison.FAIL;
                        }
                    }
                    return MatchComparison.BEST;
                } else if (firstChar < 'A') {
                    // TODO Fix to not worry about the number type.
                    // hack for now; should perform a better comparison that matches
                    // irrespective of the type of number
                    BigDecimal keyValue = new BigDecimal(matchKey);

                    return compare(keyValue, value) == 0
                            ? MatchComparison.BEST
                            : MatchComparison.FAIL;
                } else {
                    // TODO, look at select option to pick cardinal vs ordinal vs none.
                    if (pluralCategory == null) {
                        // get the plural category
                        PluralRules rules =
                                PluralRules.forLocale(
                                        context.getLocale(), PluralRules.PluralType.CARDINAL);
                        IFixedDecimal fixedDecimal =
                                nf.locale(context.getLocale())
                                        .format(offsettedValue)
                                        .getFixedDecimal();
                        pluralCategory = rules.select(fixedDecimal);
                    }
                    return pluralCategory.equals(matchKey)
                            ? MatchComparison.BETTER
                            : MatchComparison.FAIL;
                }
            }

            private int compare(BigDecimal keyValue, Number value2) {
                if (value instanceof BigDecimal) {
                    return keyValue.compareTo((BigDecimal) value);
                } else if (value instanceof BigInteger) {
                    return keyValue.compareTo(new BigDecimal((BigInteger) value));
                } else if (value instanceof Double || value instanceof Float) {
                    final double doubleValue = value.doubleValue();
                    return doubleValue == Double.POSITIVE_INFINITY
                            ? -1
                            : doubleValue == Double.NEGATIVE_INFINITY
                                    ? 1
                                    : keyValue.compareTo(BigDecimal.valueOf(doubleValue));
                } else {
                    return keyValue.compareTo(BigDecimal.valueOf(value.longValue()));
                }
            }

            private Number subtractOffset() {
                if (value instanceof BigDecimal) {
                    return ((BigDecimal) value).subtract(new BigDecimal(offset));
                } else if (value instanceof BigInteger) {
                    return ((BigInteger) value).subtract(BigInteger.valueOf(offset));
                } else if (value instanceof Double || value instanceof Float) {
                    return value.doubleValue() - offset;
                } else {
                    return value.longValue() - offset;
                }
            }

            @Override
            public String format(MfContext context) {
                Locale locale = context.getLocale();
                if (numberingSystem != null) {
                    nf =
                            nf.symbols(
                                    DecimalFormatSymbols.forNumberingSystem(
                                            locale, numberingSystem));
                }
                return nf.locale(locale).format(offsettedValue).toString();
            }

            public void applyOptions() {
                for (Entry<String, Object> entry : getOptions().entrySet()) {
                    final Object eValue = entry.getValue();
                    switch (entry.getKey()) {
                        case "maxFractionDigits":
                            nf =
                                    nf.precision(
                                            Precision.maxFraction(
                                                    Integer.parseInt(eValue.toString())));
                            break;
                        case "minFractionDigits":
                            nf =
                                    nf.precision(
                                            Precision.minFraction(
                                                    Integer.parseInt(eValue.toString())));
                            break;
                        case "numberingSystem":
                            numberingSystem = NumberingSystem.getInstanceByName(eValue.toString());
                            break;
                        case "signDisplay":
                            nf =
                                    nf.sign(
                                            SignDisplay.valueOf(
                                                    eValue.toString().toUpperCase(Locale.ROOT)));
                            break;
                        case "u:offset":
                            offset = Integer.parseInt(eValue.toString());
                            offsettedValue = subtractOffset();
                            break;
                        case "u:choice":
                            choice = Boolean.valueOf(eValue.toString());
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    ":number doen't allow the option " + entry);
                    }
                }
            }
        }

        @Override
        public String getName() {
            return ":number";
        }

        @Override
        public boolean canSelect() {
            return true;
        }

        @Override
        public boolean canFormat() {
            return true;
        }

        @Override
        public MfResolvedVariable from(Expression expression, MfContext mfContext) {
            switch (expression.type) {
                case literal:
                    // needs to use an implementation-neutral parse
                    new NumberVariable(new BigDecimal(expression.operandId), expression.map);
                case input:
                    Object input1 = mfContext.getInput(expression.operandId);
                    if (!(input1 instanceof Number)) {
                        throw new IllegalArgumentException(getName() + " requires numbers");
                    }
                    return new NumberVariable((Number) input1, expression.map);
                case variable:
                    MfResolvedVariable sourceVariable =
                            mfContext.boundVariables.get(expression.operandId);
                    // In this case, we only take numbers
                    if (!(sourceVariable instanceof NumberVariable)) {
                        throw new IllegalArgumentException(getName() + " requires numbers");
                    }
                    if (expression.map.isEmpty()) {
                        return sourceVariable;
                    }
                    OptionsMap newMap = sourceVariable.getOptions().merge(expression.map);
                    if (newMap.equals(sourceVariable.getOptions())) {
                        return sourceVariable;
                    }
                    // Determine if anything mutates.
                    // In that case, they could modify the value, and modify the options
                    // Otherwise...
                    return new NumberVariable(((NumberVariable) sourceVariable).value, newMap);
            }
            return null;
        }
    }

    /** MfFunction & MfVariable for :number */
    static class MeasureFunction implements MfFunction {
        /** A variable of information that results from applying measure function (factory). */
        static class MeasureVariable extends MfResolvedVariable {
            Measure value;
            String formatted = null;

            @Override
            public String toString() {
                return "[value=" + value + ", options=" + getOptions() + "]";
            }

            private MeasureVariable(Measure operand, OptionsMap options) {
                value = operand;
                setOptions(options);
            }

            @Override
            public MatchComparison match(MfContext context, String matchKey, String bestMatchKey) {
                throw new UnsupportedOperationException(":u:measure doesn't support selection");
            }

            @Override
            public String format(MfContext context) {
                if (formatted == null) {
                    UnlocalizedNumberFormatter nf = NumberFormatter.with();
                    for (Entry<String, Object> entry : getOptions().entrySet()) {
                        final Object eValue = entry.getValue();
                        switch (entry.getKey()) {
                            case "maxFractionDigits":
                                nf =
                                        nf.precision(
                                                Precision.maxFraction(
                                                        Integer.parseInt(eValue.toString())));
                                break;
                            case "minFractionDigits":
                                nf =
                                        nf.precision(
                                                Precision.minFraction(
                                                        Integer.parseInt(eValue.toString())));
                                break;
                            case "numberingSystem":
                                final NumberingSystem numberingSystem =
                                        NumberingSystem.getInstanceByName(eValue.toString());
                                nf.symbols(
                                        DecimalFormatSymbols.forNumberingSystem(
                                                context.getLocale(), numberingSystem));
                                break;
                            case "signDisplay":
                                nf = nf.sign(SignDisplay.valueOf(eValue.toString()));
                                break;
                            case "usage":
                                nf = nf.usage(eValue.toString());
                                break;
                            case "width":
                                String stringValue = eValue.toString().toUpperCase();
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
                    final LocalizedNumberFormatter localized = nf.locale(context.getLocale());
                    formatted = localized.format(value).toString();
                }
                return formatted;
            }
        }

        @Override
        public String getName() {
            return ":u:measure";
        }

        public MeasureVariable fromVariable(
                MfResolvedVariable variable, MfContext context, OptionsMap options) {
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

        public MeasureVariable fromLiteral(String literal, OptionsMap options) {
            List<String> parts = MockMessageFormat.SPACE_SPLITTER.splitToList(literal);
            if (parts.size() != 2) {
                throw new IllegalArgumentException(
                        getName() + " requires a literal in the form number/unicode_unit_id");
            }
            MeasureUnit unit = MeasureUnit.forIdentifier(parts.get(1));
            Measure measure = new Measure(new BigDecimal(parts.get(0)), unit);
            return fromInput(measure, options);
        }

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

        @Override
        public MfResolvedVariable from(Expression expression, MfContext mfContext) {
            switch (expression.type) {
                case literal:
                    List<String> parts =
                            MockMessageFormat.SPACE_SPLITTER.splitToList(expression.operandId);
                    if (parts.size() != 2) {
                        throw new IllegalArgumentException(
                                getName()
                                        + " requires a literal in the form number/unicode_unit_id");
                    }
                    MeasureUnit unit = MeasureUnit.forIdentifier(parts.get(1));
                    Measure measure = new Measure(new BigDecimal(parts.get(0)), unit);
                    OptionsMap options2 = expression.map;
                    if (!(measure instanceof Measure)) {
                        throw new IllegalArgumentException(getName() + " requires measures");
                    }
                    validate(options2);
                    return new MeasureVariable(measure, options2);
                case input:
                    Object input1 = mfContext.getInput(expression.operandId);
                    OptionsMap options = expression.map;
                    if (!(input1 instanceof Measure)) {
                        throw new IllegalArgumentException(getName() + " requires measures");
                    }
                    validate(options);
                    return new MeasureVariable((Measure) input1, options);
                case variable:
                    MfResolvedVariable sourceVariable =
                            mfContext.boundVariables.get(expression.operandId);
                    if (!(sourceVariable instanceof MeasureVariable)) {
                        throw new IllegalArgumentException(getName() + " requires measures");
                    }
                    validate(expression.map);
                    if (expression.map.isEmpty()) {
                        return sourceVariable;
                    }
                    OptionsMap newMap = sourceVariable.getOptions().merge(expression.map);
                    if (newMap.equals(sourceVariable.getOptions())) {
                        return sourceVariable;
                    }

                    // Determine if any mutate.
                    // In that case, they could modify the value, and modify the options
                    // Otherwise...
                    return new MeasureVariable(((MeasureVariable) sourceVariable).value, newMap);
            }
            return null;
        }
    }
}
