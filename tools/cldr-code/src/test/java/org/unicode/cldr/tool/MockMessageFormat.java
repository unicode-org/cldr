package org.unicode.cldr.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

public class MockMessageFormat {

    private final MFContext context;

    public MockMessageFormat(Locale locale) {
        context = new MFContext(locale);
    }

    /**
     * Format a variant, once it has been chosen.
     */
    private String format(String variant) {
        // dumb implementation
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
            FunctionBundle bundle = context.namedBundles.get(varString);
            if (bundle == null) {
                throw new IllegalArgumentException("No variable named " + varString);
            }
            result.append(bundle.format(context));
            lastPosition = end + 1;
        }
    }

    /**
     * Represents a set of variants with keys
     */
    private static class Variants {
        private Map<List<String>, String> map = new LinkedHashMap<>();

        String getBestMatch(MFContext context, List<FunctionBundle> selectors) {
            // dumb algorithm for now, just first match
            for (Entry<List<String>, String> entry : map.entrySet()) {
                if (selectors.size() != entry.getKey().size()) {
                    throw new IllegalArgumentException();
                }
                int i = 0;
                for (FunctionBundle selector : selectors) {
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
     * A factory that represents a particular function. It is used to create a FunctionBundle from
     * either an input, or another variable (
     */
   public interface FunctionFactory {
        public FunctionBundle fromBundle(FunctionBundle bundle, OptionsMap options);

        public FunctionBundle fromInput(Object input, OptionsMap options);

        public boolean canSelect();

        public boolean canFormat();
    }

    /**
     * A bundle of information that results from applying a function (factory).
     */
    abstract static class FunctionBundle {
        OptionsMap options;
        // The subclasses will have a value as well

        public OptionsMap getOptions() {
            return options;
        }

        public abstract void setOptions(OptionsMap options);

        public abstract boolean match(MFContext contact, String matchKey);

        public abstract String format(MFContext contact);

        // abstract Parts formatToParts(); // not necessary for mock

        @Override
        public String toString() {
            return options.toString();
        }
    }

    /**
     * A factory that represents a particular function (in this case :number).
     */
    static class NumberFactory implements FunctionFactory {
        @Override
        public NumberBundle fromBundle(FunctionBundle bundle, OptionsMap options) {
            // In this case, we always
            if (!(bundle instanceof NumberBundle)) {
                throw new IllegalArgumentException("Number requires numbers");
            }
            validate(options);

            // Determine if any mutate.
            // In that case, they could modify the value, and modify the options
            // Otherwise...
            return new NumberBundle(
                    ((NumberBundle) bundle).value, bundle.getOptions().merge(options));
        }

        @Override
        public NumberBundle fromInput(Object input, OptionsMap options) {
            if (!(input instanceof Number)) {
                throw new IllegalArgumentException("Number requires numbers");
            }
            validate(options);
            return new NumberBundle((Number) input, options);
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
            if (!ALLOWED.containsAll(options.map.keySet())) {
                throw new IllegalArgumentException(
                        "Invalid options: " + Sets.difference(options.map.keySet(), ALLOWED));
            }
        }
    }

    /**
     * A bundle of information that results from applying number function (factory).
     */
    static class NumberBundle extends FunctionBundle {
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
                    + optionsApplied;
        }

        @Override
        public void setOptions(OptionsMap options) {
            this.options = options;
        }

        private NumberBundle(Number operand, OptionsMap options) {
            value = operand;
            this.options = options;
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
            for (Entry<String, Object> entry : options.entrySet()) {
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

    /**
     * A container for context that the particular bundles will need.
     */
    static class MFContext {
        public final Map<String, FunctionBundle> namedBundles = new LinkedHashMap<>();
        public final Locale locale;

        public MFContext(Locale locale) {
            this.locale = locale;
        }

        public FunctionBundle get(String name) {
            return namedBundles.get(name);
        }

        public void put(String name, FunctionBundle numberBundle) {
            if (namedBundles.containsKey(name)) {
                throw new IllegalArgumentException("Can't reassign variable");
            }
            namedBundles.put(name, numberBundle);
        }
    }

    /**
     * A container for an options map.
     */
    static class OptionsMap {
        final Map<String, Object> map;

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
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(formatTest(Locale.forLanguageTag("ar"), 3.456));
        System.out.println(formatTest(Locale.forLanguageTag("fr"), 0));
        System.out.println(formatTest(Locale.forLanguageTag("en"), 1));
    }

    /**
     * This is a mockup of an example with MF2. It is just looking at the structure,
     * so it doesn't bother with actually parsing the message.
     * Instead, it shows code that would be generated when interpreting the MF2 string.
     * There are no optimizations for speed or memory since it is a mockup.
     * @param locale
     * @param varInput
     * @return
     */
    public static String formatTest(Locale locale, Number varInput) {
        MockMessageFormat mf = new MockMessageFormat(locale);

        // .input {$var :number maxFractionDigits=2 minFractionDigits=1}
        NumberFactory number = new NumberFactory(); // create at first need
        mf.context.put(
                "$var",
                number.fromInput(
                        varInput,
                        OptionsMap.put("maxFractionDigits", 3).put("minFractionDigits", 1).done()));

        // .local {$var2 :number maxFractionDigits=3}
        mf.context.put(
                "$var2",
                number.fromBundle(mf.context.get("$var"), OptionsMap.put("maxFractionDigits", 2).done()));

        // .match {$var2 :number numberingSystem=arab}
        // 0 {{The selector can apply a different annotation to {$var2} for the purposes of
        // selection}}
        // one {{Matches ‘one’ with {$var2}}
        // * {{A placeholder in a pattern can apply a different annotation to {$var2 :number
        // signDisplay=always}}}

        // For simplicity in this mockup, we pull all the additional function values out ahead of time, giving them
        // non-colliding identifiers.
        mf.context.put(
                "$var2_a",
                number.fromBundle(mf.context.get("$var2"), OptionsMap.put("numberingSystem", "arab").done()));
        mf.context.put(
                "$var2_b",
                number.fromBundle(mf.context.get("$var2"), OptionsMap.put("signDisplay", "always").done()));

        // We then build the variants and match them

        Variants variants =
                new Variants(
                        ImmutableMap.of(
                                List.of("0"),
                                "The selector can apply a different annotation to {$var} for the purposes of selection",
                                List.of("one"),
                                "Matches ‘one’ with {$var2_a}",
                                List.of("*"),
                                "A placeholder in a pattern can apply a different annotation to {$var2_b}."));
        String variant = variants.getBestMatch(mf.context, List.of(mf.context.get("$var2_a")));

        // And finally, we format (only format to string is needed for this mockup

        String formatted = mf.format(variant);
        return locale + "/" + varInput + " 🡆 " + formatted;
    }
}
