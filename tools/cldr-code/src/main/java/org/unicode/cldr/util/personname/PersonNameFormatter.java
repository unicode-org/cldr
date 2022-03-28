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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.unicode.cldr.util.CLDRFile;
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

    public enum Length {
        // There is a slight complication because 'long' collides with a keyword.
        long_name,
        medium,
        short_name,
        monogram,
        monogramNarrow;

        private static ImmutableBiMap<String,Length> exceptionNames = ImmutableBiMap.of(
            "long", long_name,
            "short", short_name);

        /**
         * Use this instead of valueOf
         */
        static Length from(String item) {
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

    public enum Style {
        formal,
        informal;
        public static final Comparator<Iterable<Style>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Style>naturalOrder());
        public static final Set<Style> ALL = ImmutableSet.copyOf(Style.values());
    }

    public enum Usage {
        referring,
        addressing,
        sorting;
        public static final Comparator<Iterable<Usage>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Usage>naturalOrder());
        public static final Set<Usage> ALL = ImmutableSet.copyOf(Usage.values());
    }

    public enum Order {
        surnameFirst,
        givenFirst;
        public static final Comparator<Iterable<Order>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Order>naturalOrder());
        public static final Set<Order> ALL = ImmutableSet.copyOf(Order.values());
    }


    public enum Modifier {
        informal,
        allCaps,
        initial,
        prefix,
        core
        ;
        public static final Comparator<Iterable<Modifier>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<Modifier>naturalOrder());
        public static final Comparator<Collection<Modifier>> LONGEST_FIRST = new Comparator<Collection<Modifier>>() {

            @Override
            public int compare(Collection<Modifier> o1, Collection<Modifier> o2) {
                return ComparisonChain.start()
                    .compare(o2.size(), o1.size()) // reversed order for longest first
                    .compare(o1,  o2, ITERABLE_COMPARE)
                    .result();
            }

        };
        public static final Set<Modifier> ALL = ImmutableSet.copyOf(Modifier.values());
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
            // TODO Rich escape \ and { in literals
            return literal != null ? literal : modifiedField.toString();
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
        private ULocale formatterLocale;
        private BreakIterator characterBreakIterator;
        private MessageFormat initialFormatter;

        public FallbackFormatter(ULocale uLocale, String formatterPattern) {
            formatterLocale = uLocale;
            characterBreakIterator = BreakIterator.getCharacterInstance(uLocale);
            initialFormatter = new MessageFormat(formatterPattern);
        }

        public String formatInitial(String bestValue) {
            // TODO Alex if multiword, apply the algorithm to each part
            BreakIterator breakIterator = characterBreakIterator;
            breakIterator.setText(bestValue);
            bestValue = bestValue.substring(0, breakIterator.next());
            bestValue = initialFormatter.format(new String[] {bestValue});
            // TODO Alex use initial pattern after defined
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

        public String format(NameObject nameObject, FallbackFormatter fallbackInfo) {
            StringBuilder result = new StringBuilder();
            Set<Modifier> remainingModifers = EnumSet.noneOf(Modifier.class);

            NamePatternElement lastAppendedElement = null;
            for (NamePatternElement element : elements) {
                final String literal = element.getLiteral();
                if (literal != null) {
                    // TODO Rich to handle collapsing whitespace / literals
                    // can look at lastAppendedElement and this element
                    result.append(literal);
                } else {
                    String bestValue = nameObject.getBestValue(element.getModifiedField(), remainingModifers);
                    if (bestValue == null) {
                        continue;
                    } if (!remainingModifers.isEmpty()) {
                        // apply default algorithms
                        // TODO specify order among modifiers

                        for (Modifier modifier : remainingModifers) {
                            switch(modifier) {
                            case initial:
                                bestValue = fallbackInfo.formatInitial(bestValue);
                                break;
                            case allCaps:
                                bestValue = UCharacter.toUpperCase(fallbackInfo.formatterLocale, bestValue);
                                break;
                            case prefix:
                                // TODO unhandled prefix is special; treat as if null
                                // TODO if there is no plain, but there is a prefix and core, use that; otherwise use core
                                break;
                            case core:
                            case informal:
                                // no option, just fall back
                                break;
                            }
                        }

                        // then clear the results for the next placeholder
                        remainingModifers.clear();
                    }
                    result.append(bestValue);
                }
                lastAppendedElement = element;
            }
            return result.toString();
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
                final Character currentCharacter = patternString.charAt(i);

                switch (currentCharacter) {
                case '\\':
                    if (i + 1 < patternLength) {
                        final Character nextCharacter = patternString.charAt(i + 1);
                        if (!ALLOWED_ESCAPED_CHARACTERS.contains(nextCharacter)) {
                            throw new IllegalArgumentException(String.format("Escaping character '%c' is not supported", nextCharacter));
                        }

                        rawValue += nextCharacter;
                        i += 2;
                        continue;
                    } else {
                        throw new IllegalArgumentException("Invalid escape sequence");
                    }

                case '{':
                    if (curlyStarted) {
                        throw new IllegalArgumentException("Unexpected {");
                    }
                    curlyStarted = true;
                    if (!rawValue.isEmpty()) {
                        result.add(new NamePatternElement(rawValue));
                        rawValue = "";
                    }
                    break;

                case '}':
                    if (!curlyStarted) {
                        throw new IllegalArgumentException("Unexpected }");
                    }
                    curlyStarted = false;
                    if (rawValue.isEmpty()) {
                        throw new IllegalArgumentException("Empty field is not allowed");
                    } else {
                        result.add(new NamePatternElement(ModifiedField.from(rawValue)));
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
                throw new IllegalArgumentException("Unmatched {");
            }
            if (!rawValue.isEmpty()) {
                result.add(new NamePatternElement(rawValue));
            }

            return result;
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
    }

    /**
     * Input parameters, such as {length=long_name, style=informal}. Unmentioned items are null, and match any value.
     */
    public static class FormatParameters implements Comparable<FormatParameters> {
        private final Length length;
        private final Style style;
        private final Usage usage;
        private final Order order;

        /**
         * Normally we don't often need to create one FormalParameters from another.
         * The one exception is the order, which comes from the NameObject.
         */
        public FormatParameters setOrder(Order order) {
            return new FormatParameters(length, style, usage, order);
        }

        public Length getLength() {
            return length;
        }

        public Style getStyle() {
            return style;
        }

        public Usage getUsage() {
            return usage;
        }

        public Order getOrder() {
            return order;
        }

        public boolean matches(FormatParameters other) {
            return (length == null || other.length == null || length == other.length)
                && (style == null || other.style == null || style == other.style)
                && (usage == null || other.usage == null || usage == other.usage)
                && (order == null || other.order == null || order == other.order)
                ;
        }

        public FormatParameters(Length length, Style style, Usage usage, Order order) {
            this.length = length;
            this.style = style;
            this.usage = usage;
            this.order = order;
        }
        @Override
        public String toString() {
            List<String> items = new ArrayList<>();
            if (length != null) {
                items.add("length='" + length + "'");
            }
            if (usage != null) {
                items.add("usage='" + usage + "'");
            }
            if (style != null) {
                items.add("style='" + style + "'");
            }
            if (order != null) {
                items.add("order='" + order + "'");
            }
            return JOIN_SPACE.join(items);
        }

        public static FormatParameters from(String string) {
            Length length = null;
            Style style = null;
            Usage usage = null;
            Order order = null;
            for (String part : SPLIT_SEMI.split(string)) {
                List<String> parts = SPLIT_EQUALS.splitToList(part);
                if (parts.size() != 2) {
                    throw new IllegalArgumentException("must be of form length=medium; style=… : " + string);
                }
                final String key = parts.get(0);
                final String value = parts.get(1);
                switch(key) {
                case "length":
                    length = Length.from(value);
                    break;
                case "style":
                    style = Style.valueOf(value);
                    break;
                case "usage":
                    usage = Usage.valueOf(value);
                    break;
                case "order":
                    order = Order.valueOf(value);
                    break;
                }
            }
            return new FormatParameters(length, style, usage, order);
        }

        // for thread-safe lazy evaluation
        private static class LazyEval {
            private static ImmutableSet<FormatParameters> DATA;
            static {
                Set<FormatParameters> _data = new LinkedHashSet<>();
                for (Length length : Length.values()) {
                    for (Usage usage : Usage.values()) {
                        for (Style style : Style.values()) {
                            for (Order order : Order.values()) {
                                _data.add(new FormatParameters(length, style, usage, order));
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
                .compare(length, other.length)
                .compare(usage, other.usage)
                .compare(style, other.style)
                .compare(order, other.order)
                .result();
        }

        public String toLabel() {
            StringBuilder sb  = new StringBuilder();
            addToLabel(length, sb);
            addToLabel(usage, sb);
            addToLabel(style, sb);
            addToLabel(order, sb);
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
     * Matching parameters, such as {lengths={long_name medium_name}, styles={informal}}. Unmentioned items are empty, and match any value.
     */
    public static class ParameterMatcher implements Comparable<ParameterMatcher> {
        private final Set<Length> lengths;
        private final Set<Style> styles;
        private final Set<Usage> usages;
        private final Set<Order> orders;

        public Set<Length> getLength() {
            return lengths;
        }

        public Set<Style> getStyle() {
            return styles;
        }

        public Set<Usage> getUsage() {
            return usages;
        }

        public Set<Order> getOrder() {
            return orders;
        }

        public boolean matches(FormatParameters other) {
            return (lengths.isEmpty() || other.length == null || lengths.contains(other.length))
                && (styles.isEmpty() || other.style == null || styles.contains(other.style))
                && (usages.isEmpty() || other.usage == null || usages.contains(other.usage))
                && (orders.isEmpty() || other.order == null || orders.contains(other.order))
                ;
        }

        public ParameterMatcher(Set<Length> lengths, Set<Style> styles, Set<Usage> usages, Set<Order> orders) {
            this.lengths = lengths == null ? ImmutableSet.of() : ImmutableSet.copyOf(lengths);
            this.styles = styles == null ? ImmutableSet.of() : ImmutableSet.copyOf(styles);
            this.usages = usages == null ? ImmutableSet.of() : ImmutableSet.copyOf(usages);
            this.orders = orders == null ? ImmutableSet.of() : ImmutableSet.copyOf(orders);
        }

        public ParameterMatcher(String lengths, String styles, String usages, String orders) {
            this.lengths = setFrom(lengths, Length::from);
            this.styles = setFrom(styles, Style::valueOf);
            this.usages = setFrom(usages, Usage::valueOf);
            this.orders = setFrom(orders, Order::valueOf);
        }

        public ParameterMatcher(FormatParameters item) {
            this(SingletonSetOrNull(item.getLength()),
                SingletonSetOrNull(item.getStyle()),
                SingletonSetOrNull(item.getUsage()),
                SingletonSetOrNull(item.getOrder()));
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
            String length = null;
            String style = null;
            String usage = null;
            String order = null;
            if (string.isBlank()) {
                return MATCH_ALL;
            }
            for (String part : SPLIT_SEMI.split(string)) {
                List<String> parts = SPLIT_EQUALS.splitToList(part);
                if (parts.size() != 2) {
                    throw new IllegalArgumentException("must be of form length=medium short; style=… : " + string);
                }
                final String key = parts.get(0);
                final String value = parts.get(1);
                switch(key) {
                case "length":
                    length = value;
                    break;
                case "style":
                    style = value;
                    break;
                case "usage":
                    usage = value;
                    break;
                case "order":
                    order = value;
                    break;
                }
            }
            return new ParameterMatcher(length, style, usage, order);
        }

        @Override
        public String toString() {
            List<String> items = new ArrayList<>();
            showAttributes("length", lengths, items);
            showAttributes("usage", usages, items);
            showAttributes("style", styles, items);
            showAttributes("order", orders, items);
            return items.isEmpty() ? "ANY" : JOIN_SPACE.join(items);
        }

        public String toLabel() {
            StringBuilder sb  = new StringBuilder();
            addToLabel(lengths, sb);
            addToLabel(usages, sb);
            addToLabel(styles, sb);
            addToLabel(orders, sb);
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
        public static final ParameterMatcher MATCH_ALL = new ParameterMatcher((Set<Length>)null, null, null, null);

        public static final Comparator<Iterable<ParameterMatcher>> ITERABLE_COMPARE = Comparators.lexicographical(Comparator.<ParameterMatcher>naturalOrder());

        @Override
        public boolean equals(Object obj) {
            ParameterMatcher that = (ParameterMatcher) obj;
            return lengths.equals(that.lengths)
                && styles.equals(that.styles)
                && usages.equals(that.usages)
                && orders.equals(that.orders);
        }
        @Override
        public int hashCode() {
            return lengths.hashCode() ^ styles.hashCode() ^ usages.hashCode() ^ orders.hashCode();
        }

        @Override
        public int compareTo(ParameterMatcher other) {
            // special case MATCH_ALL to put at end
            if (this.equals(MATCH_ALL)) {
                return other.equals(MATCH_ALL) ? 0 : 1;
            } else if (other.equals(MATCH_ALL)) {
                return -1;
            }
            return ComparisonChain.start()
                .compare(lengths, other.lengths, Length.ITERABLE_COMPARE)
                .compare(usages, other.usages, Usage.ITERABLE_COMPARE)
                .compare(styles, other.styles, Style.ITERABLE_COMPARE)
                .compare(orders, other.orders, Order.ITERABLE_COMPARE)
                .result();
        }

        public ParameterMatcher merge(ParameterMatcher that) {
            // if 3 of the 4 rows are equal, merge the other two
            if (lengths.equals(that.lengths)
                && styles.equals(that.styles)
                && usages.equals(that.usages)) {
                return new ParameterMatcher(lengths, styles, usages, Sets.union(orders, that.orders));
            } else if (lengths.equals(that.lengths)
                && styles.equals(that.styles)
                && orders.equals(that.orders)) {
                return new ParameterMatcher(lengths, styles, Sets.union(usages, that.usages), orders);
            } else if (lengths.equals(that.lengths)
                && usages.equals(that.usages)
                && orders.equals(that.orders)) {
                return new ParameterMatcher(lengths, Sets.union(styles, that.styles), usages, orders);
            } else if (styles.equals(that.styles)
                && usages.equals(that.usages)
                && orders.equals(that.orders)) {
                return new ParameterMatcher(Sets.union(lengths, that.lengths), styles, usages, orders);
            }
            return null;
        }

        public ParameterMatcher slim() {
            return new ParameterMatcher(
                lengths.equals(Length.ALL) ? ImmutableSet.of() : lengths,
                    styles.equals(Style.ALL) ? ImmutableSet.of() : styles,
                        usages.equals(Usage.ALL) ? ImmutableSet.of() : usages,
                            orders.equals(Order.ALL) ? ImmutableSet.of() : orders
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
         * <li>The final ParameterMatcher must be MATCH_ALL.</li>
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
                // Except for the final MATCH_ALL
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
                if (matchCount == 0 && !key.equals(ParameterMatcher.MATCH_ALL)) {
                    throw new IllegalArgumentException("key is masked by previous values: " + key
                        + ",\n\t" + JOIN_LFTB.join(formatParametersToNamePattern.entries()));
                }

                // Each entry in ParameterMatcher must have at least one NamePattern
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("key has no values: " + key);
                }
                lastKey = key;
            }

            // The final entry must have MATCH_ALL as the key
            if (!lastKey.equals(ParameterMatcher.MATCH_ALL)) {
                throw new IllegalArgumentException("last key is not MATCH_ALL" + lastKey);
            }
        }

        /**
         * Build from strings for ease of testing
         * TODO Mark make the localeToOrder from strings also
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
            String result = parameterMatcherToNamePattern.asMap().toString();
            return result.replace("], ", "],\n\t\t\t"); // for readability
        }
    }

    public static interface NameObject {
        public String getBestValue(ModifiedField modifiedField, Set<Modifier> remainingModifers);
        public ULocale getNameLocale();
        public Set<Field> getAvailableFields();
    }

    private final NamePatternData namePatternMap;
    private final FallbackFormatter fallbackFormatter;

    @Override
    public String toString() {
        return namePatternMap.toString();
    }

    /**
     * Create a formatter directly from data.
     * An alternative method would be to create from the viewer's locale, using a resource bundle or CLDR data
     * @param patternLocale TODO
     */
    public PersonNameFormatter(NamePatternData namePatternMap, FallbackFormatter fallbackFormatter) {
        this.namePatternMap = namePatternMap;
        this.fallbackFormatter = fallbackFormatter;
    }

    public String format(NameObject nameObject, FormatParameters nameFormatParameters) {
        // look through the namePatternMap to find the best match for the set of modifiers and the available nameObject fields
        NamePattern bestPattern = namePatternMap.getBestMatch(nameObject, nameFormatParameters);
        // then format using it
        return bestPattern.format(nameObject, fallbackFormatter);
    }

    /**
     * For testing
     */
    public Collection<NamePattern> getBestMatchSet(FormatParameters nameFormatParameters) {
        return namePatternMap.getBestMatchSet(nameFormatParameters);
    }

    public PersonNameFormatter(CLDRFile cldrFile) {
        ListMultimap<ParameterMatcher, NamePattern> formatParametersToNamePattern = LinkedListMultimap.create();
        Set<Pair<ParameterMatcher, NamePattern>> ordered = new TreeSet<>();
        // read out the data and order it properly
        for (String path : cldrFile) {
            if (path.startsWith("//ldml/personNames")) {
                // eg //ldml/personNames/personName[@length="long"][@usage="sorting"]/namePattern[alt="7"]
                // value = {surname}, {given} {given2} {suffix}
                String value = cldrFile.getStringValue(path);
                XPathParts parts = XPathParts.getFrozenInstance(path);
                final String altValue = parts.getAttributeValue(-1, "alt");
                int rank = altValue == null ? 0 : Integer.parseInt(altValue);
                ParameterMatcher pm = new ParameterMatcher(
                    hackFix(parts.getAttributeValue(-2, "length")),
                    parts.getAttributeValue(-2, "style"),
                    parts.getAttributeValue(-2, "usage"),
                    parts.getAttributeValue(-2, "order")
                    );
                // TODO change en.xml to not have periods after initials
                value = value.replace(".", "");
                NamePattern np = NamePattern.from(rank, value);
                if (np.toString().isBlank()) {
                    throw new IllegalArgumentException("No empty patterns allowed: " + pm);
                }
                boolean added = ordered.add(Pair.of(pm, np));
                if (!added) {
                    throw new IllegalArgumentException("Duplicate name pattern " + np + ", for " + pm);
                }
            }
        }
        for (Pair<ParameterMatcher, NamePattern> entry : ordered) {
            formatParametersToNamePattern.put(entry.getFirst(), entry.getSecond());
        }
        // TODO Peter Add locale map to en.xml so we can read it
        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.of();

        this.namePatternMap = new NamePatternData(localeToOrder, ImmutableListMultimap.copyOf(formatParametersToNamePattern));
        String HACK_INITIAL_FORMATTER = "{0}॰"; // use "unusual" period to mark that we aren't done
        this.fallbackFormatter = new FallbackFormatter(new ULocale(cldrFile.getLocaleID()), HACK_INITIAL_FORMATTER);
    }

    /**
     * Remove once the DTD and en.xml are fixed
     * @param attributeValue
     * @return
     */
    private String hackFix(String attributeValue) {
        if (attributeValue == null) {
            return null;
        }
        else {
            return attributeValue.replace("monogram-narrow", "monogramNarrow");
        }
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
        result.putAll(ParameterMatcher.MATCH_ALL, optimalAny);
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
}
