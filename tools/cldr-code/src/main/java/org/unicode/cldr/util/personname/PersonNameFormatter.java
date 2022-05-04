package org.unicode.cldr.util.personname;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;

/**
 * Rough sketch for now
 * TODO Mark Make classes/methods private that don't need to be public
 * TODO Peter Check for invalid parameters
 */

public class PersonNameFormatter {

    public static final boolean DEBUG = System.getProperty("PersonNameFormatter.DEBUG") != null;

    public enum Field {
        prefix,
        given,
        given2,
        surname,
        surname2,
        suffix;
        public static final Comparator<Iterable<Field>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Field>naturalOrder());
        public static final Set<Field> ALL = ImmutableSet.copyOf(Field.values());
    }

    public enum Order {
        givenFirst,
        surnameFirst,
        sorting;
        public static final Comparator<Iterable<Order>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Order>naturalOrder());
        public static final Set<Order> ALL = ImmutableSet.copyOf(Order.values());
        /**
         * Use this instead of valueOf if value might be null
         */
        public static Order from(String item) {
            return item == null ? null : Order.valueOf(item);
        }
    }

    public enum Length {
        // There is a slight complication because 'long' collides with a keyword.
        long_name,
        medium,
        short_name;

        private static ImmutableBiMap<String,Length> exceptionNames = ImmutableBiMap.of(
            "long", long_name,
            "short", short_name);

        /**
         * Use this instead of valueOf
         */
        public static Length from(String item) {
            if (item == null) {
                return null;
            }
            Length result = exceptionNames.get(item);
            return result != null ? result : Length.valueOf(item);
        }
        @Override
        public String toString() {
            String result = exceptionNames.inverse().get(this);
            return result != null ? result : name();
        }

        public static final Comparator<Iterable<Length>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Length>naturalOrder());
        public static final Set<Length> ALL = ImmutableSet.copyOf(Length.values());
    }

    public enum Usage {
        referring,
        addressing,
        monogram;
        public static final Comparator<Iterable<Usage>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Usage>naturalOrder());
        public static final Set<Usage> ALL = ImmutableSet.copyOf(Usage.values());
        /**
         * Use this instead of valueOf if value might be null
         */
        public static Usage from(String item) {
            return item == null ? null : Usage.valueOf(item);
        }
    }

    public enum Formality {
        formal,
        informal;
        public static final Comparator<Iterable<Formality>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Formality>naturalOrder());
        public static final Set<Formality> ALL = ImmutableSet.copyOf(Formality.values());
        /**
         * Use this instead of valueOf if value might be null
         */
        public static Formality from(String item) {
            return item == null ? null : Formality.valueOf(item);
        }
    }

    public enum Modifier {
        informal,
        allCaps,
        initialCap,
        initial,
        monogram,
        prefix,
        core,
        ;
        public static final Comparator<Iterable<Modifier>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Modifier>naturalOrder());
        public static final Comparator<Collection<Modifier>> LONGEST_FIRST = new Comparator<>() {

            @Override
            public int compare(Collection<Modifier> o1, Collection<Modifier> o2) {
                return ComparisonChain.start()
                    .compare(o2.size(), o1.size()) // reversed order for longest first
                    .compare(o1,  o2, ITERABLE_COMPARE)
                    .result();
            }

        };
        public static final Set<Modifier> ALL = ImmutableSet.copyOf(Modifier.values());
        public static final Set<Modifier> EMPTY = ImmutableSet.of();
    }

    public enum SampleType {
        givenOnly,
        givenSurnameOnly,
        given12Surname,
        full;
        public static final Set<SampleType> ALL = ImmutableSet.copyOf(SampleType.values());
    }

    public static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults();
    public static final Splitter SPLIT_DASH = Splitter.on('-').trimResults();
    public static final Splitter SPLIT_EQUALS = Splitter.on('=').trimResults();
    public static final Splitter SPLIT_COMMA = Splitter.on(',').trimResults();
    public static final Splitter SPLIT_SEMI = Splitter.on(';').trimResults();

    public static final Joiner JOIN_SPACE = Joiner.on(' ');
    public static final Joiner JOIN_DASH = Joiner.on('-');
    public static final Joiner JOIN_SEMI = Joiner.on("; ");
    public static final Joiner JOIN_LFTB = Joiner.on("\n\t\t");



    /**
     * A Field and its modifiers, corresponding to a string form like {given-initial}.
     * Immutable
     */
    public static class ModifiedField implements Comparable<ModifiedField> {
        private final Field field;
        private final Set<Modifier> modifiers;

        public Field getField() {
            return field;
        }
        public Set<Modifier> getModifiers() {
            return modifiers;
        }

        public ModifiedField(Field field, Collection<Modifier> modifiers) {
            this.field = field;
            this.modifiers = ImmutableSet.copyOf(modifiers);
        }

        /** convenience method for testing */
        public ModifiedField(Field field, Modifier... modifiers) {
            this.field = field;
            this.modifiers = ImmutableSet.copyOf(modifiers);
        }

        /** convenience method for testing */
        public static ModifiedField from(String string) {
            Field field = null;
            Set<Modifier> modifiers = new TreeSet<>();
            for (String item : SPLIT_DASH.split(string)) {
                if (field == null) {
                    field = Field.valueOf(item);
                } else {
                    modifiers.add(Modifier.valueOf(item));
                }
            }
            return new ModifiedField(field, modifiers);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(field);
            if (!modifiers.isEmpty()) {
                result.append('-').append(JOIN_DASH.join(modifiers));
            }
            return result.toString();
        }
        @Override
        public boolean equals(Object obj) {
            ModifiedField that = (ModifiedField) obj;
            return field == that.field && modifiers.equals(that.modifiers);
        }
        @Override
        public int hashCode() {
            return field.hashCode() ^ modifiers.hashCode();
        }
        @Override
        public int compareTo(ModifiedField o) {
            return ComparisonChain.start()
                .compare(field, o.field)
                .compare(modifiers, o.modifiers, Modifier.ITERABLE_COMPARE)
                .result();
        }
    }

    /**
     * An element of a name pattern: either a literal string (like ", ") or a modified field (like {given-initial})
     * The literal is null IFF then modifiedField is not null
     * Immutable
     */
    public static class NamePatternElement implements Comparable<NamePatternElement> {
        private final String literal;
        private final ModifiedField modifiedField;


        public String getLiteral() {
            return literal;
        }
        public ModifiedField getModifiedField() {
            return modifiedField;
        }

        /**
         * @param literal
         * @param field
         * @param modifiers
         */
        public NamePatternElement(ModifiedField modifiedField) {
            this.literal = null;
            this.modifiedField = modifiedField;
        }

        public NamePatternElement(String literal) {
            this.literal = literal;
            this.modifiedField = null;
        }

        /** convenience method for testing */
        public static NamePatternElement from(Object element) {
            if (element instanceof ModifiedField) {
                return new NamePatternElement((ModifiedField) element);
            } else {
                String string = element.toString();
                if (string.startsWith("{") && string.endsWith("}")) {
                    return new NamePatternElement(ModifiedField.from(string.substring(1, string.length()-1)));
                } else {
                    return new NamePatternElement(string);
                }
            }
        }
        @Override
        public String toString() {
            return literal != null ? literal.replace("\\", "\\\\").replace("{", "\\{") : modifiedField.toString();
        }

        public static final Comparator<Iterable<NamePatternElement>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<NamePatternElement>naturalOrder());


        @Override
        public int compareTo(NamePatternElement o) {
            if (literal != null && o.literal != null) {
                return literal.compareTo(o.literal);
            } else if (modifiedField != null && o.modifiedField != null) {
                return modifiedField.compareTo(o.modifiedField);
            } else {
                return literal != null ? -1 : 1; // all literals are less than all modified fields
            }
        }
    }

    /**
     * Format fallback results, for when modifiers are not found
     */

    public static class FallbackFormatter {
        final private ULocale formatterLocale;
        final private BreakIterator characterBreakIterator;
        final private MessageFormat initialFormatter;
        final private MessageFormat initialSequenceFormatter;

        public FallbackFormatter(ULocale uLocale, String initialPattern, String initialSequencePattern) {
            formatterLocale = uLocale;
            characterBreakIterator = BreakIterator.getCharacterInstance(uLocale);
            initialFormatter = new MessageFormat(initialPattern);
            initialSequenceFormatter = new MessageFormat(initialSequencePattern);
        }

        /**
         * Apply the fallbacks for modifiers that are not handled. Public for testing.
         * @internal
         */
        public String applyModifierFallbacks(FormatParameters nameFormatParameters, Set<Modifier> remainingModifers, String bestValue) {
            // apply default algorithms
            // TODO ALL Decide if the order among modifiers is rights

            for (Modifier modifier : remainingModifers) {
                switch(modifier) {
                case initial:
                    bestValue = formatInitial(bestValue, nameFormatParameters);
                    break;
                case monogram:
                    bestValue = formatMonogram(bestValue, nameFormatParameters);
                    break;
                case initialCap:
                    bestValue = TO_TITLE_WHOLE_STRING_NO_LOWERCASE.apply(formatterLocale.toLocale(), null, bestValue);
                    break;
                case allCaps:
                    bestValue = UCharacter.toUpperCase(formatterLocale, bestValue);
                    break;
                case prefix:
                    bestValue = null;
                    // TODO Mark if there is no plain, but there is a prefix and core, use that; otherwise use core
                    break;
                case core:
                case informal:
                    // no option, just fall back
                    break;
                }
            }
            return bestValue;
        }

        public String formatInitial(String bestValue, FormatParameters nameFormatParameters) {
            // It is probably unusual to have multiple name fields, so this could be optimized for
            // the simpler case.

            // Employ both the initialFormatter and initialSequenceFormatter

            String result = null;
            for(String part : SPLIT_SPACE.split(bestValue)) {
                String partFirst = getFirstGrapheme(part);
                bestValue = initialFormatter.format(new String[] {partFirst});
                if (result == null) {
                    result = bestValue;
                } else {
                    result = initialSequenceFormatter.format(new String[] {result, bestValue});
                }
            }
            return result;
        }

        public String formatMonogram(String bestValue, FormatParameters nameFormatParameters) {
            // It is probably unusual to have multiple name fields, so this could be optimized for
            // the simpler case.

            // For the case of monograms, don't use the initialFormatter or initialSequenceFormatter
            // And just take the first grapheme.

            return getFirstGrapheme(bestValue);
        }


        private String getFirstGrapheme(String bestValue) {
            characterBreakIterator.setText(bestValue);
            bestValue = bestValue.substring(0, characterBreakIterator.next());
            return bestValue;
        }

        public String formatAllCaps(String bestValue) {
            return UCharacter.toUpperCase(formatterLocale, bestValue);
        }
    }

    /**
     * A name pattern, corresponding to a string such as "{given-initial} {surname}"
     * Immutable
     */
    public static class NamePattern implements Comparable<NamePattern> {
        private final int rank;
        private final List<NamePatternElement> elements;
        private final Set<Field> fields;

        public Set<Field> getFields() {
            return ImmutableSet.copyOf(fields);
        }

        public int getFieldsSize() {
            return fields.size();
        }

        /**
         * Return the rank order (0, 1, ...) in a list
         * @return
         */
        public int getRank() {
            return rank;
        }

        public String format(NameObject nameObject, FormatParameters nameFormatParameters, FallbackFormatter fallbackInfo) {
            StringBuilder result = new StringBuilder();
            boolean seenLeadingField = false;
            boolean seenEmptyLeadingField = false;
            boolean seenEmptyField = false;
            StringBuilder literalTextBefore = new StringBuilder();
            StringBuilder literalTextAfter = new StringBuilder();

            for (NamePatternElement element : elements) {
                final String literal = element.getLiteral();
                if (literal != null) {
                    if (seenEmptyLeadingField) {
                        // do nothing; throw away the literal text
                    } else if (seenEmptyField) {
                        literalTextAfter.append(literal);
                    } else {
                        literalTextBefore.append(literal);
                    }
                } else {
                    String bestValue = getBestValueForNameObject(nameObject, element, nameFormatParameters, fallbackInfo);
                    if (bestValue == null) {
                        if (!seenLeadingField) {
                            seenEmptyLeadingField = true;
                            literalTextBefore.setLength(0);
                        } else {
                            seenEmptyField = true;
                            literalTextAfter.setLength(0);
                        }
                    } else {
                        seenLeadingField = true;
                        seenEmptyLeadingField = false;
                        if (seenEmptyField) {
                            result.append(coalesceLiterals(literalTextBefore, literalTextAfter));
                            result.append(bestValue);
                            seenEmptyField = false;
                        } else {
                            result.append(literalTextBefore);
                            literalTextBefore.setLength(0);
                            result.append(bestValue);
                        }
                    }
                }
            }
            if (!seenEmptyField) {
                result.append(literalTextBefore);
            }
            return result.toString();
        }

        private String getBestValueForNameObject(NameObject nameObject, NamePatternElement element, FormatParameters nameFormatParameters, FallbackFormatter fallbackInfo) {
            Set<Modifier> remainingModifers = EnumSet.noneOf(Modifier.class);
            final ModifiedField modifiedField = element.getModifiedField();
            String bestValue = nameObject.getBestValue(modifiedField, remainingModifers);
            if (bestValue == null) {
                    return null;
            }
            if (!remainingModifers.isEmpty()) {
                bestValue = fallbackInfo.applyModifierFallbacks(nameFormatParameters, remainingModifers, bestValue);
            }
            return bestValue;
        }

        private String coalesceLiterals(StringBuilder l1, StringBuilder l2) {
            // get the range of nonwhitespace characters at the beginning of l1
            int p1 = 0;
            while (p1 < l1.length() && !Character.isWhitespace(l1.charAt(p1))) {
                ++p1;
            }

            // get the range of nonwhitespace characters at the end of l2
            int p2 = l2.length() - 1;
            while (p2 >= 0 && !Character.isWhitespace(l2.charAt(p2))) {
                --p2;
            }

            // also include one whitespace character from l1 or, if there aren't
            // any, one whitespace character from l2
            if (p1 < l1.length()) {
                ++p1;
            } else if (p2 >= 0) {
                --p2;
            }

            // concatenate those two ranges to get the coalesced literal text
            String result = l1.substring(0, p1) + l2.substring(p2 + 1);

            // clear out l1 and l2 (done here to improve readability in format() above))
            l1.setLength(0);
            l2.setLength(0);

            return result;
        }

        private void trimEnd(StringBuilder result) {
            int len = result.length();
            if (len > 0 && result.charAt(len-1) == ' ') {
                result.setLength(len-1);
            }
        }

        public NamePattern(int rank, List<NamePatternElement> elements) {
            this.rank = rank;
            this.elements = elements;
            Set<Field> result = EnumSet.noneOf(Field.class);
            for (NamePatternElement element : elements) {
                ModifiedField modifiedField = element.getModifiedField();
                if (modifiedField != null) {
                    result.add(modifiedField.getField());
                }
            }
            this.fields = ImmutableSet.copyOf(result);
        }

        /** convenience method for testing */
        public static NamePattern from(int rank, Object... elements) {
            return new NamePattern(rank, makeList(elements));
        }

        /** convenience method for testing */
        public static NamePattern from(int rank, String patternString) {
            return new NamePattern(rank, parse(patternString));
        }

        private static final Set<Character> ALLOWED_ESCAPED_CHARACTERS = new HashSet<>(Arrays.asList('\\', '{', '}'));

        private static List<NamePatternElement> parse(String patternString) {
            List<NamePatternElement> result = new ArrayList<>();

            String rawValue = "";
            Boolean curlyStarted = false;
            final int patternLength = patternString.length();
            int i = 0;
            while (i < patternLength) {
                final Character currentCharacter = patternString.charAt(i); // this is safe, since syntax is ASCII

                switch (currentCharacter) {
                case '\\':
                    if (i + 1 < patternLength) {
                        final Character nextCharacter = patternString.charAt(i + 1);
                        if (!ALLOWED_ESCAPED_CHARACTERS.contains(nextCharacter)) {
                            throwParseError(String.format("Escaping character '%c' is not supported", nextCharacter), patternString, i);
                        }

                        rawValue += nextCharacter;
                        i += 2;
                        continue;
                    } else {
                        throwParseError("Invalid character: ", patternString, i);
                    }

                case '{':
                    if (curlyStarted) {
                        throwParseError("Unexpected {: ", patternString, i);
                    }
                    curlyStarted = true;
                    if (!rawValue.isEmpty()) {
                        result.add(new NamePatternElement(rawValue));
                        rawValue = "";
                    }
                    break;

                case '}':
                    if (!curlyStarted) {
                        throwParseError("Unexpected }", patternString, i);
                    }
                    curlyStarted = false;
                    if (rawValue.isEmpty()) {
                        throwParseError("Empty field '{}' is not allowed ", patternString, i);
                    } else {
                        try {
                            result.add(new NamePatternElement(ModifiedField.from(rawValue)));
                        } catch (Exception e) {
                            throwParseError("Invalid field: ", rawValue, 0);
                        }
                        rawValue = "";
                    }
                    break;

                default:
                    rawValue += currentCharacter;
                    break;
                }

                i++;
            }

            if (curlyStarted) {
                throwParseError("Unmatched {", patternString, patternString.length());
            }
            if (!rawValue.isEmpty()) {
                result.add(new NamePatternElement(rawValue));
            }

            return result;
        }

        private static String BAD_POSITION = "❌";

        private static void throwParseError(String message, String patternString, int i) {
            throw new IllegalArgumentException(message + ": " +  "«" + patternString.substring(0,i) + BAD_POSITION + patternString.substring(i) + "»");
        }

        private static List<NamePatternElement> makeList(Object... elements2) {
            List<NamePatternElement> result = new ArrayList<>();
            for (Object element : elements2) {
                result.add(NamePatternElement.from(element));
            }
            return result;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("\"");
            for (NamePatternElement element : elements) {
                if (element.literal != null) {
                    for (final Character c : element.literal.toCharArray()) {
                        if (ALLOWED_ESCAPED_CHARACTERS.contains(c)) {
                            result.append('\\');
                        }
                        result.append(c);
                    }
                } else {
                    result.append('{').append(element).append('}');
                }
            }
            return result.append("\"").toString();
        }
        public static final Comparator<Iterable<NamePattern>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<NamePattern>naturalOrder());

        @Override
        /**
         * Compares first by fields, then by the string value (later case would be unusual)
         */
        public int compareTo(NamePattern o) {
            return ComparisonChain.start()
                .compare(rank, o.rank)
                .compare(fields, o.fields, Field.ITERABLE_COMPARE)
                .compare(elements, o.elements, NamePatternElement.ITERABLE_COMPARE)
                .result();
        }
        @Override
        public boolean equals(Object obj) {
            return compareTo((NamePattern)obj) == 0; // no need to optimize
        }
        @Override
        public int hashCode() {
            return Objects.hash(rank, fields, elements);
        }

        /**
         * Utility for testing validity
         * @return
         */
        public Multimap<Field, Integer> getFieldPositions() {
            Multimap<Field, Integer> result = TreeMultimap.create();
            int i = -1;
            for (NamePatternElement element : elements) {
                ++i;
                if (element.literal == null) {
                    result.put(element.modifiedField.field, i);
                }
            }
            return result;
        }

        public ModifiedField getModifiedField(int index) {
            return elements.get(index).modifiedField;
        }

        /**
         * @internal
         */
        public String firstLiteralContaining(String item) {
            for (NamePatternElement element : elements) {
                final String literal = element.literal;
                if (literal != null && literal.contains(item)) {
                    return literal;
                }
            }
            return null;
        }
    }

    /**
     * Input parameters, such as {length=long_name, formality=informal}. Unmentioned items are null, and match any value.
     */
    public static class FormatParameters implements Comparable<FormatParameters> {
        private final Order order;
        private final Length length;
        private final Usage usage;
        private final Formality formality;

        /**
         * Normally we don't often need to create one FormalParameters from another.
         * The one exception is the order, which comes from the NameObject.
         */
        public FormatParameters setOrder(Order order) {
            return new FormatParameters(order, length, usage, formality);
        }

        public Order getOrder() {
            return order;
        }

        public Length getLength() {
            return length;
        }

        public Usage getUsage() {
            return usage;
        }

        public Formality getFormality() {
            return formality;
        }

        public boolean matches(FormatParameters other) {
            return (order == null || other.order == null || order == other.order)
                && (length == null || other.length == null || length == other.length)
                && (usage == null || other.usage == null || usage == other.usage)
                && (formality == null || other.formality == null || formality == other.formality)
                ;
        }

        public FormatParameters(Order order, Length length, Usage usage, Formality formality) {
            this.order = order;
            this.length = length;
            this.usage = usage;
            this.formality = formality;
        }
        @Override
        public String toString() {
            List<String> items = new ArrayList<>();
            if (order != null) {
                items.add("order='" + order + "'");
            }
            if (length != null) {
                items.add("length='" + length + "'");
            }
            if (usage != null) {
                items.add("usage='" + usage + "'");
            }
            if (formality != null) {
                items.add("formality='" + formality + "'");
            }
            return JOIN_SPACE.join(items);
        }

        public static FormatParameters from(String string) {
            Order order = null;
            Length length = null;
            Usage usage = null;
            Formality formality = null;
            for (String part : SPLIT_SEMI.split(string)) {
                List<String> parts = SPLIT_EQUALS.splitToList(part);
                if (parts.size() != 2) {
                    throw new IllegalArgumentException("must be of form length=medium; formality=… : " + string);
                }
                final String key = parts.get(0);
                final String value = parts.get(1);
                switch(key) {
                case "order":
                    order = Order.valueOf(value);
                    break;
                case "length":
                    length = Length.from(value);
                    break;
                case "usage":
                    usage = Usage.valueOf(value);
                    break;
                case "formality":
                    formality = Formality.valueOf(value);
                    break;
                }
            }
            return new FormatParameters(order, length, usage, formality);
        }

        public static FormatParameters from(XPathParts parts) {
            FormatParameters formatParameters = new FormatParameters(
                PersonNameFormatter.Order.from(parts.getAttributeValue(2, "order")),
                PersonNameFormatter.Length.from(parts.getAttributeValue(2, "length")),
                PersonNameFormatter.Usage.from(parts.getAttributeValue(2, "usage")),
                PersonNameFormatter.Formality.from(parts.getAttributeValue(2, "formality")));
            return formatParameters;
        }

        // for thread-safe lazy evaluation
        private static class LazyEval {
            private static ImmutableSet<FormatParameters> DATA;
            static {
                Set<FormatParameters> _data = new LinkedHashSet<>();
                for (Order order : Order.values()) {
                    for (Length length : Length.values()) {
                        for (Usage usage : Usage.values()) {
                            for (Formality formality : Formality.values()) {
                                _data.add(new FormatParameters(order, length, usage, formality));
                            }
                        }
                    }
                }
                DATA = ImmutableSet.copyOf(_data);
            }
        }

        /**
         * Returns all possible combinations of fields.
         * @return
         */
        static public ImmutableSet<FormatParameters> all() {
            return LazyEval.DATA;
        }

        @Override
        public int compareTo(FormatParameters other) {
            return ComparisonChain.start()
                .compare(order, other.order)
                .compare(length, other.length)
                .compare(usage, other.usage)
                .compare(formality, other.formality)
                .result();
        }

        public String toLabel() {
            StringBuilder sb  = new StringBuilder();
            addToLabel(order, sb);
            addToLabel(length, sb);
            addToLabel(usage, sb);
            addToLabel(formality, sb);
            return sb.length() == 0 ? "any" : sb.toString();
        }

        private <T> void addToLabel(T item, StringBuilder sb) {
            if (item != null) {
                if (sb.length() != 0) {
                    sb.append('-');
                }
                sb.append(item.toString());
            }
        }
    }

    /**
     * Matching parameters, such as {lengths={long_name medium_name}, formalities={informal}}. Unmentioned items are empty, and match any value.
     */
    public static class ParameterMatcher implements Comparable<ParameterMatcher> {
        private final Set<Order> orders;
        private final Set<Length> lengths;
        private final Set<Usage> usages;
        private final Set<Formality> formalities;

        public Set<Order> getOrder() {
            return orders;
        }

        public Set<Length> getLength() {
            return lengths;
        }

        public Set<Usage> getUsage() {
            return usages;
        }

        public Set<Formality> getFormality() {
            return formalities;
        }

        public boolean matches(FormatParameters other) {
            return (orders.isEmpty() || other.order == null || orders.contains(other.order))
                && (lengths.isEmpty() || other.length == null || lengths.contains(other.length))
                && (usages.isEmpty() || other.usage == null || usages.contains(other.usage))
                && (formalities.isEmpty() || other.formality == null || formalities.contains(other.formality))
                ;
        }

        public ParameterMatcher(Set<Order> orders, Set<Length> lengths, Set<Usage> usages, Set<Formality> formalities) {
            this.orders = orders == null ? ImmutableSet.of() : ImmutableSet.copyOf(orders);
            this.lengths = lengths == null ? ImmutableSet.of() : ImmutableSet.copyOf(lengths);
            this.usages = usages == null ? ImmutableSet.of() : ImmutableSet.copyOf(usages);
            this.formalities = formalities == null ? ImmutableSet.of() : ImmutableSet.copyOf(formalities);
        }

        public ParameterMatcher(String orders, String lengths, String usages, String formalities) {
            this.orders = setFrom(orders, Order::valueOf);
            this.lengths = setFrom(lengths, Length::from);
            this.usages = setFrom(usages, Usage::valueOf);
            this.formalities = setFrom(formalities, Formality::valueOf);
        }

        public ParameterMatcher(FormatParameters item) {
            this(SingletonSetOrNull(item.getOrder()),
                SingletonSetOrNull(item.getLength()),
                SingletonSetOrNull(item.getUsage()),
                SingletonSetOrNull(item.getFormality()));
        }

        private static <T> ImmutableSet<T> SingletonSetOrNull(T item) {
            return item == null ? null : ImmutableSet.of(item);
        }

        private <T> Set<T> setFrom(String parameter, Function<String, T> func) {
            if (parameter == null || parameter.isBlank()) {
                return ImmutableSet.of();
            }
            Set<T> result = new TreeSet<>();
            for (String part : SPLIT_SPACE.split(parameter)) {
                result.add(func.apply(part));
            }
            return ImmutableSet.copyOf(result);
        }

        public static ParameterMatcher from(String string) {
            String order = null;
            String length = null;
            String usage = null;
            String formality = null;
            if (string.isBlank()) {
                throw new IllegalArgumentException("must have at least one of order, length, usage, or formality");
            }
            for (String part : SPLIT_SEMI.split(string)) {
                List<String> parts = SPLIT_EQUALS.splitToList(part);
                if (parts.size() != 2) {
                    throw new IllegalArgumentException("must be of form length=medium short; formality=… : " + string);
                }
                final String key = parts.get(0);
                final String value = parts.get(1);
                switch(key) {
                case "order":
                    order = value;
                    break;
                case "length":
                    length = value;
                    break;
                case "usage":
                    usage = value;
                    break;
                case "formality":
                    formality = value;
                    break;
                }
            }
            return new ParameterMatcher(order, length, usage, formality);
        }

        @Override
        public String toString() {
            List<String> items = new ArrayList<>();
            showAttributes("order", orders, items);
            showAttributes("length", lengths, items);
            showAttributes("usage", usages, items);
            showAttributes("formality", formalities, items);
            return items.isEmpty() ? "ANY" : JOIN_SPACE.join(items);
        }

        public String toLabel() {
            StringBuilder sb  = new StringBuilder();
            addToLabel(orders, sb);
            addToLabel(lengths, sb);
            addToLabel(usages, sb);
            addToLabel(formalities, sb);
            return sb.length() == 0 ? "any" : sb.toString();
        }

        private <T> void addToLabel(Set<T> set, StringBuilder sb) {
            for (T item : set) {
                if (sb.length() != 0) {
                    sb.append('-');
                }
                sb.append(item.toString());
            }
        }

        private <T> void showAttributes(String title, Set<T> set, List<String> toAddTo) {
            if (!set.isEmpty()) {
                toAddTo.add(title + "='" + JOIN_SPACE.join(set) + "'");
            }
        }
        //public static final ParameterMatcher MATCH_ALL = new ParameterMatcher((Set<Length>)null, null, null, null);

        public static final Comparator<Iterable<ParameterMatcher>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<ParameterMatcher>naturalOrder());

        @Override
        public boolean equals(Object obj) {
            ParameterMatcher that = (ParameterMatcher) obj;
            return orders.equals(that.orders)
                && lengths.equals(that.lengths)
                && usages.equals(that.usages)
                && formalities.equals(that.formalities);
        }
        @Override
        public int hashCode() {
            return lengths.hashCode() ^ formalities.hashCode() ^ usages.hashCode() ^ orders.hashCode();
        }

        @Override
        public int compareTo(ParameterMatcher other) {
            return ComparisonChain.start()
                .compare(orders, other.orders, Order.ITERABLE_COMPARE)
                .compare(lengths, other.lengths, Length.ITERABLE_COMPARE)
                .compare(usages, other.usages, Usage.ITERABLE_COMPARE)
                .compare(formalities, other.formalities, Formality.ITERABLE_COMPARE)
                .result();
        }

        public ParameterMatcher merge(ParameterMatcher that) {
            // if 3 of the 4 rows are equal, merge the other two
            if (lengths.equals(that.lengths)
                && usages.equals(that.usages)
                && formalities.equals(that.formalities)) {
                return new ParameterMatcher(Sets.union(orders, that.orders), lengths, usages, formalities);
            } else if (orders.equals(that.orders)
                && usages.equals(that.usages)
                && formalities.equals(that.formalities)) {
                return new ParameterMatcher(orders, Sets.union(lengths, that.lengths), usages, formalities);
            } else if (orders.equals(that.orders)
                && lengths.equals(that.lengths)
                && formalities.equals(that.formalities)) {
                return new ParameterMatcher(orders, lengths, Sets.union(usages, that.usages), formalities);
            } else if (orders.equals(that.orders)
                && lengths.equals(that.lengths)
                && usages.equals(that.usages)) {
                return new ParameterMatcher(orders, lengths, usages, Sets.union(formalities, that.formalities));
            }
            return null;
        }

        public ParameterMatcher slim() {
            return new ParameterMatcher(
                orders.equals(Length.ALL) ? ImmutableSet.of() : orders,
                    lengths.equals(Formality.ALL) ? ImmutableSet.of() : lengths,
                        usages.equals(Usage.ALL) ? ImmutableSet.of() : usages,
                            formalities.equals(Order.ALL) ? ImmutableSet.of() : formalities
                );
        }

        /**
         * Compact a set of FormatParameters into a (smaller) collection of ParameterMatchers
         */
        public static Set<ParameterMatcher> compact(Collection<FormatParameters> expanded) {
            Set<ParameterMatcher> result = new TreeSet<>();
            for (FormatParameters item : expanded) {
                result.add(new ParameterMatcher(item));
            }
            // try merging each pair
            // if we can merge, then start over from the top
            // look at optimizing later
            main:
                while (true) {
                    for (ParameterMatcher item1 : result) {
                        for (ParameterMatcher item2: result) {
                            if (item1 == item2) { // skip ourselves
                                continue;
                            }
                            ParameterMatcher item12 = item1.merge(item2); // merge if possible
                            if (item12 != null) {
                                result.remove(item1);
                                result.remove(item2);
                                result.add(item12);
                                continue main; // retry everything
                            }
                        }
                    }
                    break;
                }

            // now replace any "complete" items by empty.
            Set<ParameterMatcher> result2 = new TreeSet<>();
            for (ParameterMatcher item1 : result) {
                result2.add(item1.slim());
            }
            return ImmutableSet.copyOf(result2);
        }

    }

    /**
     * Data that maps from NameFormatParameters and a NameObject to the best NamePattern.
     * Immutable
     */
    public static class NamePatternData {
        private final ImmutableMap<ULocale, Order> localeToOrder;
        private final ImmutableListMultimap<ParameterMatcher, NamePattern> parameterMatcherToNamePattern;

        public NamePattern getBestMatch(NameObject nameObject, FormatParameters nameFormatParameters) {
            if (nameFormatParameters.order == null) {
                final Order mappedOrder = localeToOrder.get(nameObject.getNameLocale());
                nameFormatParameters = nameFormatParameters.setOrder(mappedOrder == null ? Order.givenFirst : mappedOrder);
            }

            NamePattern result = null;

            Collection<NamePattern> namePatterns = getBestMatchSet(nameFormatParameters);
            if (namePatterns == null) {
                // Internal error, should never happen with valid data
                throw new IllegalArgumentException("Can't find " + nameFormatParameters + " in " + parameterMatcherToNamePattern);
            }
            Set<Field> nameFields = nameObject.getAvailableFields();
            int bestMatchSize = -1;

            for (NamePattern pattern : namePatterns) {
                Set<Field> patternFields = pattern.getFields();

                int matchSize = getIntersectionSize(nameFields, patternFields);

                if ((matchSize > bestMatchSize) /* better match */
                    || (matchSize == bestMatchSize
                    && patternFields.size() < result.getFieldsSize()) /* equal match, but less "extra" fields */) {
                    bestMatchSize = matchSize;
                    result = pattern;
                }
            }

            return result;
        }

        public Collection<NamePattern> getBestMatchSet(FormatParameters nameFormatParameters) {

            for (Entry<ParameterMatcher, Collection<NamePattern>> parametersAndPatterns : parameterMatcherToNamePattern.asMap().entrySet()) {
                ParameterMatcher parameters = parametersAndPatterns.getKey();
                if (parameters.matches(nameFormatParameters)) {
                    return parametersAndPatterns.getValue();
                }
            }

            return null; // for now; this will only happen if the NamePatternData is invalid
        }


        /**
         * Build the name pattern data. In the formatParametersToNamePattern:
         * <ul>
         * <li>Every possible FormatParameters value must match at least one ParameterMatcher</li>
         * <li>No ParameterMatcher is superfluous; the ones before it must not mask it.</li>
         * </ul>
         * The multimap values must retain the order they are built with!
         */
        public NamePatternData(ImmutableMap<ULocale, Order> localeToOrder,
            ImmutableListMultimap<ParameterMatcher, NamePattern> formatParametersToNamePattern) {

            if (formatParametersToNamePattern == null || formatParametersToNamePattern.isEmpty()) {
                throw new IllegalArgumentException("formatParametersToNamePattern must be non-null, non-empty");
            }

            this.localeToOrder = localeToOrder == null ? ImmutableMap.of() : localeToOrder;
            this.parameterMatcherToNamePattern = formatParametersToNamePattern;

            ParameterMatcher lastKey = null;
            Set<FormatParameters> remaining = new LinkedHashSet<>(FormatParameters.all());

            for (Entry<ParameterMatcher, Collection<NamePattern>> entry : formatParametersToNamePattern.asMap().entrySet()) {
                ParameterMatcher key = entry.getKey();
                Collection<NamePattern> values = entry.getValue();

                // TODO Mark No ParameterMatcher should be completely masked by any previous ones
                // The following code starts with a list of all the items, and removes any that match
                int matchCount = 0;
                for (Iterator<FormatParameters> rest = remaining.iterator(); rest.hasNext(); ) {
                    FormatParameters item = rest.next();
                    if (key.matches(item)) {
                        rest.remove();
                        ++matchCount;
                        if (DEBUG) {
                            System.out.println(" * " + item + " matches " + key);
                        }
                    }
                }
                if (matchCount == 0) {
                    key.equals(lastKey);
                    throw new IllegalArgumentException("key is masked by previous values: " + key
                        + ",\n\t" + JOIN_LFTB.join(formatParametersToNamePattern.entries()));
                }

                // Each entry in ParameterMatcher must have at least one NamePattern
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("key has no values: " + key);
                }
                lastKey = key;
            }
        }

        /**
         * Build from strings for ease of testing
         */
        public NamePatternData(ImmutableMap<ULocale, Order> localeToOrder, String...formatParametersToNamePatterns ) {
            this(localeToOrder, parseFormatParametersToNamePatterns(formatParametersToNamePatterns));
        }

        private static ImmutableListMultimap<ParameterMatcher, NamePattern> parseFormatParametersToNamePatterns(String... formatParametersToNamePatterns) {
            int count = formatParametersToNamePatterns.length;
            if ((count % 2) != 0) {
                throw new IllegalArgumentException("Must have even number of strings, fields => pattern: " + Arrays.asList(formatParametersToNamePatterns));
            }
            ListMultimap<ParameterMatcher, NamePattern> _formatParametersToNamePatterns = LinkedListMultimap.create();
            int rank = 0;
            for (int i = 0; i < count; i += 2) {
                ParameterMatcher pm = ParameterMatcher.from(formatParametersToNamePatterns[i]);
                NamePattern np = NamePattern.from(rank++, formatParametersToNamePatterns[i+1]);
                _formatParametersToNamePatterns.put(pm, np);
            }
            return ImmutableListMultimap.copyOf(_formatParametersToNamePatterns);
        }

        @Override
        public String toString() {
            return "{" + (localeToOrder.isEmpty() ? "" : "localeToOrder=" + localeToOrder + "\n\t\t")
                + show(parameterMatcherToNamePattern) + "}";
        }

        private String show(ImmutableListMultimap<ParameterMatcher, NamePattern> multimap) {
            String result = multimap.asMap().toString();
            return result.replace("], ", "],\n\t\t\t"); // for readability
        }

        /**
         * For testing
         * @internal
         */
        public ImmutableListMultimap<ParameterMatcher, NamePattern> getMatcherToPatterns() {
            return parameterMatcherToNamePattern;
        }
    }

    /**
     * Interface used by the person name formatter to access name field values.
     * It provides access not only to values for modified fields directly supported by the NameObject,
     * but also to values that may be produced or modified by the Name Object.
     */
    public static interface NameObject {
        /**
         * Returns the locale of the name, or null if not available.
         * NOTE: this is not the same as the locale of the person name formatter.
         * @return
         */
        public ULocale getNameLocale();
        /**
         * Returns a mapping for the modified fields directly supported to their values.
         */
        public ImmutableMap<ModifiedField, String> getModifiedFieldToValue();
        /**
         * Returns the set of fields directly supported. Should be overridden for speed.
         * It returns the same value as getModifiedFieldToValue().keySet().stream().map(x -> x.field).collect(Collectors.toSet()),
         * but may be optimized.
         */
        public Set<Field> getAvailableFields();
        /**
         * Returns the best available value for the modified field, or null if nothing is available.
         * Null is returned in all and only those cases where !getAvailableFields().contains(modifiedField.field)
         * @param modifiedField the input modified field, for which the best value is fetched.
         * @param remainingModifers contains the set of modifiers that were not handled by this method.
         * The calling code may apply fallback algorithms based on these values.
         * @return
         */
        public String getBestValue(ModifiedField modifiedField, Set<Modifier> remainingModifers);
    }

    private final NamePatternData namePatternMap;
    private final FallbackFormatter fallbackFormatter;

    @Override
    public String toString() {
        return namePatternMap.toString();
    }

    /**
     * @internal
     */
    public final NamePatternData getNamePatternData() {
        return namePatternMap;
    }

    /**
     * Create a formatter directly from data.
     */
    public PersonNameFormatter(NamePatternData namePatternMap, FallbackFormatter fallbackFormatter) {
        this.namePatternMap = namePatternMap;
        this.fallbackFormatter = fallbackFormatter;
    }

    /**
     * Create a formatter from a cldr file.
     */
    public PersonNameFormatter(CLDRFile cldrFile) {
        ListMultimap<ParameterMatcher, NamePattern> formatParametersToNamePattern = LinkedListMultimap.create();
        Set<Pair<ParameterMatcher, NamePattern>> ordered = new TreeSet<>();
        String initialPattern = null;
        String initialSequencePattern = null;
        Map<ULocale, Order> _localeToOrder = new TreeMap<>();

        // read out the data and order it properly
        for (String path : cldrFile) {
            if (path.startsWith("//ldml/personNames") && !path.endsWith("/alias")) {
                String value = cldrFile.getStringValue(path);
                //System.out.println(path + ",\t" + value);
                XPathParts parts = XPathParts.getFrozenInstance(path);
                switch(parts.getElement(2)) {
                case "personName":
                    Pair<ParameterMatcher, NamePattern> pair = fromPathValue(parts, value);
                    boolean added = ordered.add(pair);
                    if (!added) {
                        throw new IllegalArgumentException("Duplicate path/value " + pair);
                    }
                    break;
                case "initialPattern":
                    //ldml/personNames/initialPattern[@type="initial"]
                    String type = parts.getAttributeValue(-1, "type");
                    switch(type) {
                    case "initial": initialPattern = value; break;
                    case "initialSequence": initialSequencePattern = value; break;
                    default: throw new IllegalArgumentException("Unexpected path: " + path);
                    }
                    break;
                case "nameOrderLocales":
                    //ldml/personNames/nameOrderLocales[@order="givenFirst"], value = list of locales
                    for (String locale : SPLIT_SPACE.split(value)) {
                        // TODO fix Order.valueOf(parts.getAttributeValue(-1, "order")) to work.
                        _localeToOrder.put(new ULocale(locale), Order.surnameFirst);
                    }
                    break;
                case "sampleName":
                    // skip
                    break;
                default: throw new IllegalArgumentException("Unexpected path: " + path);
                }
            }
        }
        for (Pair<ParameterMatcher, NamePattern> entry : ordered) {
            formatParametersToNamePattern.put(entry.getFirst(), entry.getSecond());
        }
        // TODO Peter Add locale map to en.xml so we can read it
        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.copyOf(_localeToOrder);

        this.namePatternMap = new NamePatternData(localeToOrder, ImmutableListMultimap.copyOf(formatParametersToNamePattern));
        this.fallbackFormatter = new FallbackFormatter(new ULocale(cldrFile.getLocaleID()), initialPattern, initialSequencePattern);
    }

    public String format(NameObject nameObject, FormatParameters nameFormatParameters) {
        // look through the namePatternMap to find the best match for the set of modifiers and the available nameObject fields
        NamePattern bestPattern = namePatternMap.getBestMatch(nameObject, nameFormatParameters);
        // then format using it
        return bestPattern.format(nameObject, nameFormatParameters, fallbackFormatter);
    }

    /**
     * For testing
     */
    public Collection<NamePattern> getBestMatchSet(FormatParameters nameFormatParameters) {
        return namePatternMap.getBestMatchSet(nameFormatParameters);
    }

    /**
     * Utility for constructing data from path and value.
     */
    public static Pair<ParameterMatcher, NamePattern> fromPathValue(XPathParts parts, String value) {

        //ldml/personNames/personName[@length="long"][@usage="referring"][@order="sorting"]/namePattern[alt="2"]
        // value = {surname}, {given} {given2} {suffix}
        final String altValue = parts.getAttributeValue(-1, "alt");
        int rank = altValue == null ? 0 : Integer.parseInt(altValue);
        ParameterMatcher pm = new ParameterMatcher(
            parts.getAttributeValue(-2, "order"),
            parts.getAttributeValue(-2, "length"),
            parts.getAttributeValue(-2, "usage"),
            parts.getAttributeValue(-2, "formality")
            );

        NamePattern np = NamePattern.from(rank, value);
        if (np.toString().isBlank()) {
            throw new IllegalArgumentException("No empty patterns allowed: " + pm);
        }
        return Pair.of(pm, np);
    }

    /**
     * Utility for getting sample names. DOES NOT CACHE
     * @param cldrFile
     * @return
     */
    public static Map<SampleType, SimpleNameObject> loadSampleNames(CLDRFile cldrFile) {
        M3<SampleType, ModifiedField, String> names = ChainedMap.of(new TreeMap<SampleType, Object>(), new TreeMap<ModifiedField, Object>(), String.class);
        for (String path : cldrFile) {
            if (path.startsWith("//ldml/personNames/sampleName")) {
                //ldml/personNames/sampleName[@item="full"]/nameField[@type="prefix"]
                String value = cldrFile.getStringValue(path);
                if (value != null && !value.equals("∅∅∅")) {
                    XPathParts parts = XPathParts.getFrozenInstance(path);
                    names.put(SampleType.valueOf(parts.getAttributeValue(-2, "item")), ModifiedField.from(parts.getAttributeValue(-1, "type")), value);
                }
            }
        }
        Map<SampleType, SimpleNameObject> result = new TreeMap<>();
        for (Entry<SampleType, Map<ModifiedField, String>> entry : names) {
            SimpleNameObject name = new SimpleNameObject(new ULocale(cldrFile.getLocaleID()), entry.getValue());
            result.put(entry.getKey(), name);
        }
        return ImmutableMap.copyOf(result);
    }

    /**
     * Expand the name pattern data into a mapping from all possible FormatParameters to NamePatterns. Retains the order of the NamePatterns
     * @return
     */
    public Multimap<FormatParameters, NamePattern> expand() {
        Multimap<FormatParameters, NamePattern> parametersToPatterns = LinkedHashMultimap.create();

        for (FormatParameters item : FormatParameters.all()) {
            parametersToPatterns.putAll(item, getBestMatchSet(item));
        }
        return parametersToPatterns;
    }

    /**
     * Group FormatParameters by lists of NamePatterns that have the same FormatParameters
     */
    public static TreeMultimap<Iterable<NamePattern>, FormatParameters> groupByNamePatterns(Multimap<FormatParameters, NamePattern> parametersToPatterns) {
        TreeMultimap<Iterable<NamePattern>, FormatParameters> patternsToParameters =
            TreeMultimap.create(NamePattern.ITERABLE_COMPARE, Comparator.<FormatParameters>naturalOrder());

        for (Entry<FormatParameters, Collection<NamePattern>> entry : parametersToPatterns.asMap().entrySet()) {
            patternsToParameters.put(entry.getValue(), entry.getKey());
        }
        return patternsToParameters;
    }

    public static Multimap<ParameterMatcher, NamePattern> compact(Multimap<Iterable<NamePattern>, FormatParameters> patternsToParameters) {
        Multimap<ParameterMatcher, NamePattern> result = LinkedListMultimap.create();

        // first find the Collection<FormatParameters> that is repeated the most often, so we can use that for the ANY value
        Counter<Iterable<NamePattern>> counter = new Counter<>();
        for (Entry<Iterable<NamePattern>, Collection<FormatParameters>> entry : patternsToParameters.asMap().entrySet()) {
            counter.add(entry.getKey(), 1);
        }
        Set<R2<Long, Iterable<NamePattern>>> sorted = counter.getEntrySetSortedByCount(false, NamePattern.ITERABLE_COMPARE);
        Iterable<NamePattern> optimalAny = sorted.iterator().next().get1();

        for (Entry<Iterable<NamePattern>, Collection<FormatParameters>> entry : patternsToParameters.asMap().entrySet()) {
            Iterable<NamePattern> patterns = entry.getKey();
            if (patterns.equals(optimalAny)) {
                continue; // will replace by ANY
            }
            Collection<FormatParameters> parameterSet = entry.getValue();
            Set<ParameterMatcher> compacted = ParameterMatcher.compact(parameterSet);
            for (ParameterMatcher matcher : compacted) {
                result.putAll(matcher, patterns);
            }
        }
        return result;
    }

    /**
     * General Utility
     * Avoids object creation in Sets.intersection(a,b).size()
     */
    public static <T> int getIntersectionSize(Set<T> set1, Set<T> set2) {
        int size = 0;
        for (T e : set1) {
            if (set2.contains(e)) {
                size++;
            }
        }
        return size;
    }

    private static final CaseMap.Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE =
        CaseMap.toTitle().wholeString().noLowercase();

    public FallbackFormatter getFallbackInfo() {
        return fallbackFormatter;
    }
}
