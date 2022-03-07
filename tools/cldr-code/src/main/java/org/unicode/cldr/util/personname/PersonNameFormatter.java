package org.unicode.cldr.util.personname;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.ULocale;

/**
 * Rough sketch for now
 * TODO Make classes/methods private that don't need to be public
 * TODO Check for invalid parameters
 */

public class PersonNameFormatter {
    public enum Field {
        prefix,
        given,
        given2,
        surname,
        surname2,
        suffix;
    }

    public enum Length {
        // There is a slight complication because 'long' collides with a keyword.
        long_name,
        medium,
        narrow,
        monogram,
        monogram_narrow;

        /**
         * Use this instead of valueOf
         */
        static Length from(String item) {
            return item.equals("long") ? long_name : valueOf(item);
        }
        @Override
        public String toString() {
            return this == long_name ? "long" : name();
        }
    }

    public enum Style {
        formal,
        informal
    }

    public enum Usage {
        referring,
        addressing,
        sorting
    }

    public enum Order {
        surname_first,
        given_first
    }


    public enum Modifier {
        initial,
        all_caps,
        informal,
        core
    }

    public static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults();
    public static final Splitter SPLIT_DASH = Splitter.on('-').trimResults();
    public static final Splitter SPLIT_EQUALS = Splitter.on('=').trimResults();
    public static final Joiner JOIN_SPACE = Joiner.on(' ');
    public static final Joiner JOIN_DASH = Joiner.on('-');


    /**
     * A Field and its modifiers, corresponding to a string form like {given-initial}.
     * Immutable
     */
    public static class ModifiedField {
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
    }

    /**
     * An element of a name pattern: either a literal string (like ", ") or a modified field (like {given-initial})
     * The literal is null IFF then modifiedField is not null
     * Immutable
     */
    public static class NamePatternElement {
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
            // TODO: escape \ and { in literals
            return literal != null ? literal : modifiedField.toString();
        }
    }

    /**
     * A name pattern, corresponding to a string such as "{given-initial} {surname}"
     * Immutable
     */
    public static class NamePattern {
        private final List<NamePatternElement> elements;
        private final Set<Field> fields;

        public Set<Field> getFields() {
            return fields;
        }

        public String format(NameObject nameObject) {
            StringBuilder result = new StringBuilder();
            Set<Modifier> unhandledModifiers = EnumSet.noneOf(Modifier.class);

            for (NamePatternElement element : elements) {
                final String literal = element.getLiteral();
                if (literal != null) {
                    result.append(literal);
                } else {
                    final String bestValue = nameObject.getBestValue(element.getModifiedField(), unhandledModifiers);
                    if (!unhandledModifiers.isEmpty()) {
                        // TODO Apply unhandled modifiers algorithmically where possible

                        // then clear the results for the next placeholder
                        unhandledModifiers.clear();
                    }
                    result.append(bestValue);
                }
            }
            return result.toString();
        }

        /**
         * TODO Replace by builder
         */
        public NamePattern(List<NamePatternElement> elements) {
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

        // TODO: make convenience method that parses a string, correctly handling {...} but also \{ and \\

        /** convenience method for testing */
        public static NamePattern from(Object... elements) {
            return new NamePattern(makeList(elements));
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
                    result.append(element);
                } else {
                    result.append('{').append(element).append('}');
                }
            }
            return result.append("\"").toString();
        }
    }

    /**
     * Input parameters, such as {length=long_name, style=informal}. Unmentioned items are null, and match any value.
     */
    public static class FormatParameters {
        private final Length length;
        private final Style style;
        private final Usage usage;
        private final Order order;

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
                items.add("length=" + length);
            }
            if (style != null) {
                items.add("style=" + style);
            }
            if (usage != null) {
                items.add("usage=" + usage);
            }
            if (order != null) {
                items.add("order=" + order);
            }
            return "{" + JOIN_SPACE.join(items) + "}";
        }

        public static FormatParameters from(String string) {
            Length length = null;
            Style style = null;
            Usage usage = null;
            Order order = null;
            for (String part : SPLIT_SPACE.split(string)) {
                List<String> parts = SPLIT_EQUALS.splitToList(part);
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
    }

    /**
     * Matching parameters, such as {lengths={long_name medium_name}, styles={informal}}. Unmentioned items are empty, and match any value.
     */
    public static class ParameterMatcher {
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
        @Override
        public String toString() {
            List<String> items = new ArrayList<>();
            if (!lengths.isEmpty()) {
                items.add("length=" + lengths);
            }
            if (!styles.isEmpty()) {
                items.add("style=" + styles);
            }
            if (!usages.isEmpty()) {
                items.add("usage=" + usages);
            }
            if (!orders.isEmpty()) {
                items.add("order=" + orders);
            }
            return items.isEmpty() ? "ANY" : "{" + Joiner.on(" ").join(items) + "}";
        }
        public static final ParameterMatcher MATCH_ALL = new ParameterMatcher(null, null, null, null);
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
                nameFormatParameters = nameFormatParameters.setOrder(localeToOrder.get(nameObject.getNameLocale()));
            }

            // Sift through the options to get the first one that best matches nameObjectFields

            for (Entry<ParameterMatcher, Collection<NamePattern>> parametersAndPatterns : parameterMatcherToNamePattern.asMap().entrySet()) {
                ParameterMatcher parameters = parametersAndPatterns.getKey();
                if (!parameters.matches(nameFormatParameters)) {
                    continue;
                }

                Collection<NamePattern> namePatterns = parametersAndPatterns.getValue();

                // TODO pick the NamePattern that best matches the fields in the nameObject
                // for now, just return the first

                return namePatterns.iterator().next();
            }
            return null; // for now; this will only happen if the NamePatternData is invalid
        }

        /**
         * TODO Replace by builder. Eventually will be built from CLDR XML data.
         * The multimap values must retain the order they are built with!
         */
        public NamePatternData(ImmutableMap<ULocale, Order> localeToOrder, ImmutableListMultimap<ParameterMatcher, NamePattern> formatParametersToNamePattern) {
            this.localeToOrder = localeToOrder == null ? ImmutableMap.of() : localeToOrder;
            this.parameterMatcherToNamePattern = formatParametersToNamePattern;
            // TODO: check formatParametersToNamePattern for validity
            // * no null values
            // * no ParameterMatcher should be completely masked by previous ones
            // * each entry in ParameterMatcher must have at least one NamePattern
            // * the final entry must have MATCH_ALL as the key
        }

        // TODO: add method that takes a string instead of a NamePatten

        @Override
        public String toString() {
            return "{" + (localeToOrder.isEmpty() ? "" : "localeToOrder=" + localeToOrder + "\n\t\t")
                + show(parameterMatcherToNamePattern) + "}";
        }

        private String show(ImmutableListMultimap<ParameterMatcher, NamePattern> multimap) {
//            StringBuilder result = new StringBuilder();
//            for (Entry<ParameterMatcher, Collection<NamePattern>> entry : multimap.asMap().entrySet()) {
//                ParameterMatcher key = entry.getKey();
//                if
//            }
            return parameterMatcherToNamePattern.asMap().toString();
        }
    }

    public static interface NameObject {
        public String getBestValue(ModifiedField modifiedField, Set<Modifier> outputUnhandledModifiers);
        public ULocale getNameLocale();
        public Set<Field> getAvailableFields();
    }

    private NamePatternData namePatternMap;

    @Override
    public String toString() {
        return namePatternMap.toString();
    }

    /**
     * Create a formatter directly from data.
     * An alternative method would be to create from the viewer's locale, using a resource bundle or CLDR data
     */
    public PersonNameFormatter(NamePatternData namePatternMap) {
        this.namePatternMap = namePatternMap;
    }

    public String format(NameObject nameObject, FormatParameters nameFormatParameters) {
        // look through the namePatternMap to find the best match for the set of modifiers and the available nameObject fields
        NamePattern bestPattern = namePatternMap.getBestMatch(nameObject, nameFormatParameters);
        // then format using it
        return bestPattern.format(nameObject);
    }
}
