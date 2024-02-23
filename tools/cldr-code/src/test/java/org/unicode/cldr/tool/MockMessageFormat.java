package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
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
    private static final boolean DEBUG = false;

    /**
     * A factory that represents a particular function. It is used to create a FunctionVariable from
     * either an input, or another variable (
     */
    public interface FunctionFactory {

        public String getName();
        /**
         * Get a value from a variable, such as $count. An error may be thrown if not convertible.
         */
        public FunctionVariable fromVariable(
                String variableName, MFContext context, OptionsMap options);

        /**
         * Get a value from an input datatype, such as a Double. so an error may be thrown if not
         * convertible.
         */
        public FunctionVariable fromInput(Object input, OptionsMap options);

        /** Use an implementation-neutral parse to get an internal value. */
        public FunctionVariable fromLiteral(String literal, OptionsMap options);

        /** Used in checking syntax */
        public boolean canSelect();

        /** Used in checking syntax */
        public boolean canFormat();
    }

    /**
     * An immutable variable containing information that results from applying a FunctionFactory
     * such as :number or :date. The FunctionVariable is specific to FunctionFactory.
     */
    abstract static class FunctionVariable {
        private OptionsMap options;
        // The subclasses will have a value as well

        /** Return the stored options (immutable) */
        public OptionsMap getOptions() {
            return options;
        }

        /** Set the options. Only usable in the subclasses */
        protected void setOptions(OptionsMap options) {
            this.options = options;
        }

        /**
         * Matches this value against the result produced by its FunctionFactory from the literal
         * matchKey. A match value of zero is "no match"; the maximum int value is an exact match;
         * anything between implementation defined
         */
        public static final int NO_MATCH = 0;

        public static final int EXACT_MATCH = Integer.MAX_VALUE;

        public abstract int match(MFContext contact, String matchKey);

        public abstract String format(MFContext contact);

        // abstract Parts formatToParts(); // not necessary for mock

        @Override
        public String toString() {
            return options.toString();
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

    /** Represents a set of variants with keys */
    private static class Variants {
        private Map<List<String>, String> map = new LinkedHashMap<>();

        String getBestMatch(MFContext context, List<FunctionVariable> selectors) {
            // It is just a dumb algorithm for matching since that isn't the point of this mock:
            // just the first list of keys where each element is either a loose match (eg *) or
            // exact match. It should, of course, sort according to the ordering set by the
            // FunctionVariable.
            for (Entry<List<String>, String> entry : map.entrySet()) {
                if (selectors.size() != entry.getKey().size()) {
                    throw new IllegalArgumentException();
                }
                int i = 0;
                for (FunctionVariable selector : selectors) {
                    final String matchKey = entry.getKey().get(i);
                    if (matchKey.equals("*")
                            || selector.match(context, matchKey) != FunctionVariable.NO_MATCH) {
                        return entry.getValue(); // return the variant submessage
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

    private final MFContext context;

    public MFContext getContext() {
        return context;
    }

    public MockMessageFormat(Locale locale) {
        context = new MFContext(locale);
    }

    /** Format a variant message, once it has been chosen. */
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

    /**
     * Run some simple examples
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(formatTest(Locale.forLanguageTag("en-US"), 1, "John", "188/meter"));
        System.out.println(formatTest(Locale.forLanguageTag("en"), 1, "Sarah", "1100/meter"));
        System.out.println(formatTest(Locale.forLanguageTag("de"), 3.456, "John", "188/meter"));
        System.out.println(formatTest(Locale.forLanguageTag("fr"), 0, "John", "188/meter"));
    }

    /**
     * This is a mockup of an example with MF2. It is just looking at the structure, so it doesn't
     * bother with actually parsing the message. Instead, it shows code that would be generated when
     * interpreting the MF2 string. There are no optimizations for speed or memory since it is a
     * mockup.
     *
     * @param locale
     * @param distance TODO
     * @param varInput
     * @return
     */
    public static String formatTest(
            Locale locale, Number inputCount, String inputName, String distance) {
        MockMessageFormat mf = new MockMessageFormat(locale);
        final MFContext context = mf.getContext();

        // .input {$var :number maxFractionDigits=2 minFractionDigits=1}
        FunctionFactory number = MockFunctions.get(":number"); // create at first need
        context.put(
                "$var",
                number.fromInput(
                        inputCount,
                        OptionsMap.put("maxFractionDigits", 3).put("minFractionDigits", 1).done()));

        // .input {$name :string}
        FunctionFactory string = MockFunctions.get(":string"); // create at first need
        context.put("$name", string.fromInput(inputName, OptionsMap.EMPTY));

        // .input {$amount :number}
        context.put("$amount", number.fromLiteral("3.2", OptionsMap.EMPTY));

        // .local {$var2 :number maxFractionDigits=3}
        context.put(
                "$var2",
                number.fromVariable(
                        "$var", context, OptionsMap.put("maxFractionDigits", 2).done()));

        // .local {$distance :u:measure maxFractionDigits=3 usage=road width=}
        FunctionFactory measure = MockFunctions.get(":u:measure"); // create at first need
        context.put(
                "$distance",
                measure.fromLiteral( // the literal structure is number/unitId
                        distance,
                        OptionsMap.put("maxFractionDigits", 2)
                                .put("usage", "road")
                                .put("width", "full")
                                .done()));
        debug("$distance", context);

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
                                "There are no books for {$name} within {$distance}.",
                                List.of("one", "John"),
                                "There is {$var2} book for {$name} within {$distance}.",
                                List.of("*", "*"),
                                "There are {$var2_b} books for {$name_b} within {$distance}."));
        String variant =
                variants.getBestMatch(
                        context, List.of(context.get("$var2_a"), context.get("$name")));

        // And finally, we format (only format to string is needed for this mockup

        String formatted = mf.format(variant);
        return locale + ", " + inputCount + ", " + inputName + " 🡆 " + formatted;
    }

    private static void debug(String variableName, MFContext context) {
        if (DEBUG) {
            final FunctionVariable functionVariable = context.get(variableName);
            final String formatted = functionVariable.format(context);
            System.out.println(
                    "# "
                            + variableName
                            + " 🡆 «"
                            + formatted
                            + "» "
                            + functionVariable.getOptions());
        }
    }
}
