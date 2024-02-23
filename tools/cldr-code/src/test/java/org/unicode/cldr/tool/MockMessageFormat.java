package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.SignDisplay;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A mock implementation of a possible internal organization for MF2. Each function is represented
 * by a FunctionFactory. That factory can take an input value or FunctionVariable, plus options, and
 * produce a FunctionVariable. For example, a NumberFactory represents the :number function. It can
 * take an input value (say a Double) and some options, and produce a NumberVariable. <br>
 * A function factory may produce a different type. For example, a StringFunction (aka :string)
 * could take a NumberVariable, and produce a StringVariable. <br>
 * This is a mockup, and no attempt has been made to produce optimal coden, nor is everything
 * cleanly encapsulated with getters/setters, nor marked with private/public as would be needed for
 * a real API, nor with good error checking.
 */
public class MockMessageFormat {

    private final MFContext context;

    public MFContext getContext() {
        return context;
    }

    public MockMessageFormat(Locale locale) {
        context = new MFContext(locale);
    }

    /** Format a variant, once it has been chosen. */
    private String format(String variant) {
        StringBuilder result = new StringBuilder();
        int lastPosition = 0;
        while (true) {
            int start = variant.indexOf('{', lastPosition);
            if (start < 0) { // failed
                result.append(variant.substring(lastPosition));
                return result.toString();
            }
            result.append(variant.substring(lastPosition, start));
            int end = variant.indexOf('}', start);
            if (end < 0) {
                throw new IllegalArgumentException();
            }
            // the variant messages are pre-parsed to not have any options
            String varString = variant.substring(start + 1, end);
            FunctionVariable variable = context.namedVariables.get(varString);
            if (variable == null) {
                throw new IllegalArgumentException("No variable named " + varString);
            }
            result.append(variable.format(context));
            lastPosition = end + 1;
        }
    }

    /** Represents a set of variants with keys */
    private static class Variants {
        private Map<List<String>, String> map = new LinkedHashMap<>();

        String getBestMatch(MFContext context, List<FunctionVariable> selectors) {
            // dumb algorithm for now, just first match
            for (Entry<List<String>, String> entry : map.entrySet()) {
                if (selectors.size() != entry.getKey().size()) {
                    throw new IllegalArgumentException();
                }
                int i = 0;
                for (FunctionVariable selector : selectors) {
                    if (selector.match(context, entry.getKey().get(i))) {
                        return entry.getValue();
                    }
                }
                ++i;
            }
            throw new IllegalArgumentException();
        }

        Variants(Map<List<String>, String> map) {
            this.map = ImmutableMap.copyOf(map);
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    /**
     * A factory that represents a particular function. It is used to create a FunctionVariable from
     * either an input, or another variable (
     */
    public interface FunctionFactory {
        public FunctionVariable fromVariable(
                String variableName, MFContext context, OptionsMap options);

        public FunctionVariable fromInput(Object input, OptionsMap options);

        public boolean canSelect();

        public boolean canFormat();
    }

    /** A variable of information that results from applying a function (factory). */
    abstract static class FunctionVariable {
        private OptionsMap options;
        // The subclasses will have a value as well

        public OptionsMap getOptions() {
            return options;
        }

        public void setOptions(OptionsMap options) {
            this.options = options;
        }

        public abstract boolean match(MFContext contact, String matchKey);

        public abstract String format(MFContext contact);

        // abstract Parts formatToParts(); // not necessary for mock

        @Override
        public String toString() {
            return options.toString();
        }
    }

    static class StringFactory implements FunctionFactory {

        @Override
        public FunctionVariable fromVariable(
                String variableName, MFContext context, OptionsMap options) {
            FunctionVariable variable = context.get(variableName);
            validate(options);
            return new StringVariable(variable.format(context), options);
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
            super.options = options;
        }

        @Override
        public boolean match(MFContext contact, String matchKey) {
            return value.equals(matchKey);
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

    /** A factory that represents a particular function (in this case :number). */
    static class NumberFactory implements FunctionFactory {
        @Override
        public NumberVariable fromVariable(
                String variableName, MFContext context, OptionsMap options) {
            FunctionVariable variable = context.get(variableName);
            // In this case, we always
            if (!(variable instanceof NumberVariable)) {
                throw new IllegalArgumentException("Number requires numbers");
            }
            validate(options);

            // Determine if any mutate.
            // In that case, they could modify the value, and modify the options
            // Otherwise...
            return new NumberVariable(
                    ((NumberVariable) variable).value, variable.getOptions().merge(options));
        }

        @Override
        public NumberVariable fromInput(Object input, OptionsMap options) {
            if (!(input instanceof Number)) {
                throw new IllegalArgumentException("Number requires numbers");
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
        String keyword = null;
        boolean optionsApplied = false;

        @Override
        public String toString() {
            return "number="
                    + value
                    + ", keyword="
                    + keyword
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
        public boolean match(MFContext context, String matchKey) {
            if (matchKey.equals("*")) {
                return true;
            } else if (matchKey.charAt(0) < 'A') { // hack for now
                return value.toString().equals(matchKey);
            }
            if (keyword == null) {
                applyOptions(context);
                PluralRules rules =
                        PluralRules.forLocale(context.locale, PluralRules.PluralType.CARDINAL);
                IFixedDecimal fixedDecimal =
                        nf.locale(context.locale).format(value).getFixedDecimal();
                keyword = rules.select(fixedDecimal);
            }
            return keyword.equals(matchKey);
        }

        @Override
        public String format(MFContext context) {
            applyOptions(context);
            return nf.locale(context.locale).format(value).toString();
        }

        public void applyOptions(MFContext context) {
            if (!optionsApplied) {
                return;
            }
            // for the options matching those in ICU NumberFormatter, we could drop after processing
            for (Entry<String, Object> entry : getOptions().entrySet()) {
                switch (entry.getKey()) {
                    case "maxFractionDigits":
                        nf.precision(Precision.maxFraction(((Integer) entry.getValue())));
                        break;
                    case "minFractionDigits":
                        nf.precision(Precision.minFraction(((Integer) entry.getValue())));
                        break;
                    case "numberingSystem":
                        nf.symbols(
                                DecimalFormatSymbols.forNumberingSystem(
                                        context.locale,
                                        NumberingSystem.getInstanceByName(
                                                entry.getValue().toString())));
                        break;
                    case "signDisplay":
                        nf.sign(SignDisplay.valueOf(entry.getValue().toString()));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Number doen't allow the option " + entry);
                }
            }
            optionsApplied = true;
        }
    }

    /** A container for context that the particular variables will need. */
    static class MFContext {
        public final Map<String, FunctionVariable> namedVariables = new LinkedHashMap<>();
        public final Locale locale;

        public MFContext(Locale locale) {
            this.locale = locale;
        }

        public FunctionVariable get(String name) {
            return namedVariables.get(name);
        }

        public void put(String name, FunctionVariable numberVariable) {
            if (namedVariables.containsKey(name)) {
                throw new IllegalArgumentException("Can't reassign variable");
            }
            namedVariables.put(name, numberVariable);
        }
    }

    /** A container for an options map. <b> Could have enum keys instead of Strings. */
    static class OptionsMap {
        final Map<String, Object> map;

        static final OptionsMap EMPTY = new OptionsMap(ImmutableMap.of());

        static class OptionBuilder {
            Map<String, Object> map = new LinkedHashMap<>();

            OptionBuilder put(String key, Object value) {
                map.put(key, value);
                return this;
            }

            OptionsMap done() {
                return new OptionsMap(map);
            }
        }

        private OptionsMap(Map<String, Object> rawMap) {
            map = ImmutableMap.copyOf(rawMap);
        }

        public Set<Entry<String, Object>> entrySet() {
            return map.entrySet();
        }

        public Set<String> keySet() {
            return map.keySet();
        }

        public Object get(String option) {
            return map.get(option);
        }

        public OptionsMap merge(OptionsMap options) {
            Map<String, Object> temp = new LinkedHashMap<>();
            temp.putAll(options.map);
            return new OptionsMap(temp);
        }

        static OptionBuilder put(String key, Object value) {
            return new OptionBuilder().put(key, value);
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    /**
     * Run some simple examples
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(formatTest(Locale.forLanguageTag("ar"), 3.456, "John"));
        System.out.println(formatTest(Locale.forLanguageTag("fr"), 0, "John"));
        System.out.println(formatTest(Locale.forLanguageTag("en"), 1, "John"));
        System.out.println(formatTest(Locale.forLanguageTag("en"), 1, "Sarah"));
    }

    /**
     * This is a mockup of an example with MF2. It is just looking at the structure, so it doesn't
     * bother with actually parsing the message. Instead, it shows code that would be generated when
     * interpreting the MF2 string. There are no optimizations for speed or memory since it is a
     * mockup.
     *
     * @param locale
     * @param varInput
     * @return
     */
    public static String formatTest(Locale locale, Number inputCount, String inputName) {
        MockMessageFormat mf = new MockMessageFormat(locale);
        final MFContext context = mf.getContext();

        // .input {$var :number maxFractionDigits=2 minFractionDigits=1}
        NumberFactory number = new NumberFactory(); // create at first need
        context.put(
                "$var",
                number.fromInput(
                        inputCount,
                        OptionsMap.put("maxFractionDigits", 3).put("minFractionDigits", 1).done()));

        // .input {$name :string}
        StringFactory string = new StringFactory(); // create at first need
        context.put("$name", string.fromInput(inputName, OptionsMap.EMPTY));

        // .local {$var2 :number maxFractionDigits=3}
        context.put(
                "$var2",
                number.fromVariable(
                        "$var", context, OptionsMap.put("maxFractionDigits", 2).done()));

        // .match {$var2 :number numberingSystem=arab} {$name}
        // 0 hi {{There are no books for the {$name}.}
        // one hi{{There is {$var2} book for {$name}.}
        // * * {{There are {$var2 :number signDisplay=always} books for {$name option="upper"}.}}

        // For simplicity in this mockup, we pull all the additional function values out ahead of
        // time, giving them
        // non-colliding identifiers.
        context.put(
                "$var2_a",
                number.fromVariable(
                        "$var2", context, OptionsMap.put("numberingSystem", "arab").done()));
        context.put(
                "$var2_b",
                number.fromVariable(
                        "$var2", context, OptionsMap.put("signDisplay", "always").done()));
        context.put(
                "$name_b",
                string.fromVariable("$name", context, OptionsMap.put("casing", "upper").done()));

        // We then build the variants and match them

        Variants variants =
                new Variants(
                        ImmutableMap.of(
                                List.of("0", "John"),
                                "There are no books for the {$name}.",
                                List.of("one", "John"),
                                "There is {$var2} book for {$name}.",
                                List.of("*", "*"),
                                "There are {$var2_b} books for {$name_b}."));
        String variant =
                variants.getBestMatch(
                        context, List.of(context.get("$var2_a"), context.get("$name")));

        // And finally, we format (only format to string is needed for this mockup

        String formatted = mf.format(variant);
        return locale + ", " + inputCount + ", " + inputName + " 🡆 " + formatted;
    }
}
