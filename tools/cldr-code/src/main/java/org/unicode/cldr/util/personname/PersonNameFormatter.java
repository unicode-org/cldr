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
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.TreeMultiset;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Output;
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

        static final Set<Set<Modifier>> INCONSISTENT_SETS = ImmutableSet.of(
            ImmutableSet.of(Modifier.core, Modifier.prefix),
            ImmutableSet.of(Modifier.initial, Modifier.monogram),
            ImmutableSet.of(Modifier.allCaps, Modifier.initialCap)
            );

        /**
         * If the input modifiers are consistent, returns an ordered set; if not, returns null and sets an error message.
         */
        public static Set<Modifier> getCleanSet(Collection<Modifier> modifierList, Output<String> errorMessage) {
            if (modifierList.isEmpty()) {
                return ImmutableSet.of();
            }
            Set<Modifier> modifiers = EnumSet.copyOf(modifierList);
            String errorMessage1 = null;
            if (modifiers.size() != modifierList.size()) {
                Multiset<Modifier> dupCheck = TreeMultiset.create();
                dupCheck.addAll(modifierList);
                for (Modifier m : modifiers) {
                    dupCheck.remove(m);
                }
                errorMessage1 = "Duplicate modifiers: " + JOIN_COMMA.join(dupCheck);
            }
            String errorMessage2 = null;
            for (Set<Modifier> inconsistentSet : INCONSISTENT_SETS) {
                if (modifiers.containsAll(inconsistentSet)) {
                    if (errorMessage2 == null) {
                        errorMessage2 = "Inconsistent modifiers: ";
                    } else {
                        errorMessage2 += ", ";
                    }
                    errorMessage2 += inconsistentSet;
                }
            }
            errorMessage.value = errorMessage1 == null ? errorMessage2
                : errorMessage2 == null ? errorMessage1
                    : errorMessage1 + "; " + errorMessage1;
            return ImmutableSet.copyOf(modifiers);
        }

        /**
         * Verifies that the prefix, core, and plain values are consistent. Returns null if ok, otherwise error message.
         */
        public static String inconsistentPrefixCorePlainValues(String prefixValue, String coreValue, String plainValue) {
            String errorMessage2 = null;
            if (prefixValue != null) {
                if (coreValue != null) {
                    if (plainValue != null) { // prefix = X, core = Y, plain = Z
                        // ok: prefix = "van", core = "Berg", plain = "van Berg"
                        // bad: prefix = "van", core = "Berg", plain = "van Wolf"
                        if (!plainValue.replace(prefixValue, "").trim().equals(coreValue)) {
                            errorMessage2 = "-core value and -prefix value are inconsistent with plain value";
                        }
                    }
                    // otherwise prefix = "x", core = "y", plain = null, so OK
                } else { // prefix = X, core = null, plain = ?
                    errorMessage2 = "cannot have -prefix without -core";
                }
            } else if (coreValue != null && plainValue != null && !plainValue.equals(coreValue)) {
                errorMessage2 = "There is no -prefix, but there is a -core and plain that are unequal";
            }
            return errorMessage2;
        }
    }

    /**
     * Types of samples, only for use by CLDR
     * @internal
     */
    public enum SampleType {
        givenOnly,
        givenSurnameOnly,
        given12Surname,
        full,
        foreign;
        public static final Set<SampleType> ALL = ImmutableSet.of(givenOnly,
            givenSurnameOnly,
            given12Surname,
            full); // exclude foreign for now
    }

    /**
     * @internal (all of these)
     */
    public static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults();
    public static final Splitter SPLIT_DASH = Splitter.on('-').trimResults();
    public static final Splitter SPLIT_EQUALS = Splitter.on('=').trimResults();
    public static final Splitter SPLIT_COMMA = Splitter.on(',').trimResults();
    public static final Splitter SPLIT_SEMI = Splitter.on(';').trimResults();

    public static final Joiner JOIN_SPACE = Joiner.on(' ');
    public static final Joiner JOIN_DASH = Joiner.on('-');
    public static final Joiner JOIN_SEMI = Joiner.on("; ");
    public static final Joiner JOIN_COMMA = Joiner.on(", ");
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
            Output<String> errorMessage = new Output<>();
            this.modifiers = Modifier.getCleanSet(modifiers, errorMessage);
            if (errorMessage.value != null) {
                throw new IllegalArgumentException(errorMessage.value);
            }
        }

        /** convenience method for testing */
        public ModifiedField(Field field, Modifier... modifiers) {
            this(field, Arrays.asList(modifiers));
        }

        /** convenience method for testing */
        public static ModifiedField from(String string) {
            Field field = null;
            List<Modifier> modifiers = new ArrayList<>();
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
     * The literal is null IFF the modifiedField is not null
     * Immutable
     * @internal
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
     * NOTE: CLDR needs to be able to create from data.
     * @internal
     */
    public static class FallbackFormatter {
        final private ULocale formatterLocale;
        final private BreakIterator characterBreakIterator;
        final private MessageFormat initialFormatter;
        final private MessageFormat initialSequenceFormatter;
        final private String foreignSpaceReplacement;

        public String getForeignSpaceReplacement() {
            return foreignSpaceReplacement;
        }

        final private boolean uppercaseSurnameIfSurnameFirst;

        public FallbackFormatter(ULocale uLocale,
            String initialPattern,
            String initialSequencePattern,
            String foreignSpaceReplacement,
            boolean uppercaseSurnameIfSurnameFirst) {
            formatterLocale = uLocale;
            characterBreakIterator = BreakIterator.getCharacterInstance(uLocale);
            initialFormatter = new MessageFormat(initialPattern);
            initialSequenceFormatter = new MessageFormat(initialSequencePattern);
            this.foreignSpaceReplacement = foreignSpaceReplacement;
            this.uppercaseSurnameIfSurnameFirst = uppercaseSurnameIfSurnameFirst;
        }

        /**
         * Apply the fallbacks for modifiers that are not handled. Public for testing.
         * @internal
         */
        public String applyModifierFallbacks(FormatParameters nameFormatParameters, Set<Modifier> remainingModifers, String bestValue) {
            // apply default algorithms

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

        /**
         * Apply other modifications. Currently just the surname capitalization, but can be extended in the future.
         * @param modifiedField
         */
        public String tweak(ModifiedField modifiedField, String bestValue, FormatParameters nameFormatParameters) {
            if (uppercaseSurnameIfSurnameFirst
                && nameFormatParameters.matchesOrder(Order.surnameFirst)
                && (modifiedField.getField() == Field.surname || modifiedField.getField() == Field.surname2)) {
                bestValue = UCharacter.toUpperCase(formatterLocale, bestValue);
            }
            return bestValue;
        }
    }

    /**
     * A name pattern, corresponding to a string such as "{given-initial} {surname}"
     * Immutable
     * NOTE: CLDR needs to be able to create from data.
     * @internal
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
            if (fallbackInfo.foreignSpaceReplacement != null && !fallbackInfo.foreignSpaceReplacement.equals(" ")) {
                ULocale nameLocale = nameObject.getNameLocale();
                if (!sharesLanguageScript(nameLocale, fallbackInfo.formatterLocale)) {
                    return SPACES.matcher(result).replaceAll(fallbackInfo.foreignSpaceReplacement);
                }
            }
            return result.toString();
        }

        private boolean sharesLanguageScript(ULocale nameLocale, ULocale formatterLocale) {
            return Objects.equals(nameLocale, formatterLocale); // TODO, fix to check language and script (maximized)
        }

        static final Pattern SPACES = Pattern.compile("\\s+"); // TODO pick whitespace

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
            return fallbackInfo.tweak(modifiedField, bestValue, nameFormatParameters);
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

        /**
         * Get the number of elements (literals and modified fields) in the pattern.
         */
        public int getElementCount() {
            return elements.size();
        }

        /**
         * Get the nth literal (or null if the nth element is a field)
         */
        public String getLiteral(int index) {
            return elements.get(index).literal;
        }

        /**
         * Get the nth modified field (or null if the nth element is a literal)
         */
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
     * Passed in when formatting.
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

        /**
         * Get the order; null means "any order"
         */
        public Order getOrder() {
            return order;
        }

        /**
         * Get the length; null means "any length"
         */
        public Length getLength() {
            return length;
        }

        /**
         * Get the usage; null means "any usage"
         */
        public Usage getUsage() {
            return usage;
        }

        /**
         * Get the formality; null means "any formality"
         */
        public Formality getFormality() {
            return formality;
        }

        public boolean matches(FormatParameters other) {
            return matchesOrder(other.order)
                && matchesLength(other.length)
                && matchesUsage(other.usage)
                && matchesFormality(other.formality);
        }

        /**
         * Utility methods for matching, taking into account that null matches anything
         */
        public boolean matchesOrder(Order otherOrder) {
            return order == null || otherOrder == null || order == otherOrder;
        }
        public boolean matchesFormality(final Formality otherFormality) {
            return formality == null || otherFormality == null || formality == otherFormality;
        }
        public boolean matchesUsage(final Usage otherUsage) {
            return usage == null || otherUsage == null || usage == otherUsage;
        }
        private boolean matchesLength(final Length otherLength) {
            return length == null || otherLength == null || length == otherLength;
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

        public String abbreviated() {
            List<String> items = new ArrayList<>();
            if (order != null) {
                items.add(order.toString().substring(0,3));
            }
            if (length != null) {
                items.add(length.toString().substring(0,3));
            }
            if (usage != null) {
                items.add(usage.toString().substring(0,3));
            }
            if (formality != null) {
                items.add(formality.toString().substring(0,3));
            }
            return JOIN_DASH.join(items);
        }

        public String dashed() {
            List<String> items = new ArrayList<>();
            if (order != null) {
                items.add(order.toString());
            }
            if (length != null) {
                items.add(length.toString());
            }
            if (usage != null) {
                items.add(usage.toString());
            }
            if (formality != null) {
                items.add(formality.toString());
            }
            return JOIN_DASH.join(items);
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

        // for thread-safe lazy evaluation
        private static class LazyEval {
            private static ImmutableSet<FormatParameters> DATA;
            private static ImmutableSet<FormatParameters> CLDR_DATA;
            static {
                Set<FormatParameters> _data = new LinkedHashSet<>();
                Set<FormatParameters> _cldrdata = new LinkedHashSet<>();
                for (Order order : Order.values()) {
                    for (Length length : Length.values()) {
                        if (order == Order.sorting) {
                            _cldrdata.add(new FormatParameters(order, length, Usage.referring, Formality.formal));
                            _cldrdata.add(new FormatParameters(order, length, Usage.referring, Formality.informal));
                        }
                        for (Formality formality : Formality.values()) {
                            for (Usage usage : Usage.values()) {
                                _data.add(new FormatParameters(order, length, usage, formality));
                                if (order != Order.sorting) {
                                    _cldrdata.add(new FormatParameters(order, length, usage, formality));
                                }
                            }
                        }
                    }
                }
                DATA = ImmutableSet.copyOf(_data);
                CLDR_DATA = ImmutableSet.copyOf(_cldrdata);
            }
        }

        /**
         * Returns all possible combinations of fields.
         * @return
         */
        static public ImmutableSet<FormatParameters> all() {
            return LazyEval.DATA;
        }

        /**
         * Returns all possible combinations of fields supported by CLDR.
         * (the order=sorting combinations are abbreviated
         * @return
         */
        static public ImmutableSet<FormatParameters> allCldr() {
            return LazyEval.CLDR_DATA;
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

        /**
         * Only used to add missing CLDR fields.
         * If an item is missing, get the best replacements.
         * @return
         */
        public Iterable<FormatParameters> getFallbacks() {
            return ImmutableList.of(
                new FormatParameters(order, length, null, formality),
                new FormatParameters(order, length, usage, null),
                new FormatParameters(order, length, null, null),
                new FormatParameters(order, null, null, null),
                new FormatParameters(null, null, null, null)
                );
        }

        @Override
        public boolean equals(Object obj) {
            FormatParameters that = (FormatParameters) obj;
            return Objects.equals(order, that.order)
                && Objects.equals(length, that.length)
                && Objects.equals(usage, that.usage)
                && Objects.equals(formality, that.formality);
        }
        @Override
        public int hashCode() {
            return (length == null ? 0 : length.hashCode())
                ^ (formality == null ? 0 : formality.hashCode())
                ^ (usage == null ? 0 : usage.hashCode())
                ^ (order == null ? 0 : order.hashCode());
        }
    }


    /**
     * Returns a match for the nameFormatParameters, or null if the parameterMatcherToNamePattern has no match.
     */
    public static Collection<NamePattern> getBestMatchSet(
        ListMultimap<FormatParameters, NamePattern> parameterMatcherToNamePattern,
        FormatParameters nameFormatParameters) {
        for (Entry<FormatParameters, Collection<NamePattern>> parametersAndPatterns : parameterMatcherToNamePattern.asMap().entrySet()) {
            FormatParameters parameters = parametersAndPatterns.getKey();
            if (parameters.matches(nameFormatParameters)) {
                return parametersAndPatterns.getValue();
            }
        }
        return null; // This will only happen if the NamePatternData is incomplete
    }

    /**
     * Data that maps from NameFormatParameters and a NameObject to the best NamePattern.
     * It must be complete: that is, it must match every possible value.
     * Immutable
     * @internal
     * NOTE: CLDR needs access to this.
     */
    public static class NamePatternData {
        private final ImmutableMap<ULocale, Order> localeToOrder;
        private final ImmutableListMultimap<FormatParameters, NamePattern> parameterMatcherToNamePattern;

        public NamePattern getBestMatch(NameObject nameObject, FormatParameters nameFormatParameters) {
            if (nameFormatParameters.order == null) {
                final Order mappedOrder = localeToOrder.get(nameObject.getNameLocale());
                nameFormatParameters = nameFormatParameters.setOrder(mappedOrder == null ? Order.givenFirst : mappedOrder);
            }

            NamePattern result = null;

            Collection<NamePattern> namePatterns = getBestMatchSet(parameterMatcherToNamePattern, nameFormatParameters);
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

        /**
         * Build the name pattern data. In the formatParametersToNamePattern:
         * <ul>
         * <li>Every possible FormatParameters value must match at least one FormatParameters</li>
         * <li>No FormatParameters is superfluous; the ones before it must not mask it.</li>
         * </ul>
         * The multimap values must retain the order they are built with!
         */
        public NamePatternData(ImmutableMap<ULocale, Order> localeToOrder,
            ListMultimap<FormatParameters, NamePattern> formatParametersToNamePattern) {

            if (formatParametersToNamePattern == null || formatParametersToNamePattern.isEmpty()) {
                throw new IllegalArgumentException("formatParametersToNamePattern must be non-null, non-empty");
            }

            this.localeToOrder = localeToOrder == null ? ImmutableMap.of() : localeToOrder;

            FormatParameters lastKey = null;
            Set<FormatParameters> remaining = new LinkedHashSet<>(FormatParameters.all());

            // check that parameters are complete, and that nothing is masked by anything previous

            for (Entry<FormatParameters, Collection<NamePattern>> entry : formatParametersToNamePattern.asMap().entrySet()) {
                FormatParameters key = entry.getKey();
                Collection<NamePattern> values = entry.getValue();

                // TODO Mark No FormatParameters should be completely masked by any previous ones

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

                // Each entry in FormatParameters must have at least one NamePattern
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("key has no values: " + key);
                }
                lastKey = key;
            }
            if (!remaining.isEmpty()) {
                throw new IllegalArgumentException("values are not complete; they don't match:\n\t"
                    + JOIN_LFTB.join(remaining));
            }
            this.parameterMatcherToNamePattern = ImmutableListMultimap.copyOf(formatParametersToNamePattern);
        }

        public Map<ULocale, Order> getLocaleToOrder() {
            return localeToOrder;
        }

        /**
         * Build from strings for ease of testing
         */
        public NamePatternData(ImmutableMap<ULocale, Order> localeToOrder, String...formatParametersToNamePatterns ) {
            this(localeToOrder, parseFormatParametersToNamePatterns(formatParametersToNamePatterns));
        }

        private static ListMultimap<FormatParameters, NamePattern> parseFormatParametersToNamePatterns(String... formatParametersToNamePatterns) {
            int count = formatParametersToNamePatterns.length;
            if ((count % 2) != 0) {
                throw new IllegalArgumentException("Must have even number of strings, fields => pattern: " + Arrays.asList(formatParametersToNamePatterns));
            }
            ListMultimap<FormatParameters, NamePattern> _formatParametersToNamePatterns = LinkedListMultimap.create();
            int rank = 0;
            for (int i = 0; i < count; i += 2) {
                FormatParameters pm = FormatParameters.from(formatParametersToNamePatterns[i]);
                NamePattern np = NamePattern.from(rank++, formatParametersToNamePatterns[i+1]);
                _formatParametersToNamePatterns.put(pm, np);
            }
            addMissing(_formatParametersToNamePatterns);

            return _formatParametersToNamePatterns;
        }

        @Override
        public String toString() {
            return "{" + (localeToOrder.isEmpty() ? "" : "localeToOrder=" + localeToOrder + "\n\t\t")
                + show(parameterMatcherToNamePattern) + "}";
        }

        private String show(ImmutableListMultimap<FormatParameters, NamePattern> multimap) {
            String result = multimap.asMap().toString();
            return result.replace("], ", "],\n\t\t\t"); // for readability
        }

        /**
         * For testing
         * @internal
         */
        public ImmutableListMultimap<FormatParameters, NamePattern> getMatcherToPatterns() {
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
     * NOTE CLDR will need to have access to this creation method.
     * @internal
     */
    public PersonNameFormatter(NamePatternData namePatternMap, FallbackFormatter fallbackFormatter) {
        this.namePatternMap = namePatternMap;
        this.fallbackFormatter = fallbackFormatter;
    }

    /**
     * Create a formatter from a CLDR file.
     * @internal
     */
    public PersonNameFormatter(CLDRFile cldrFile) {
        ListMultimap<FormatParameters, NamePattern> formatParametersToNamePattern = LinkedListMultimap.create();
        Set<Pair<FormatParameters, NamePattern>> ordered = new TreeSet<>();
        String initialPattern = null;
        String initialSequencePattern = null;
        String foreignSpaceReplacement = null;
        Map<ULocale, Order> _localeToOrder = new TreeMap<>();

        // read out the data and order it properly
        for (String path : cldrFile) {
            if (path.startsWith("//ldml/personNames") && !path.endsWith("/alias")) {
                String value = cldrFile.getStringValue(path);
                //System.out.println(path + ",\t" + value);
                XPathParts parts = XPathParts.getFrozenInstance(path);
                switch(parts.getElement(2)) {
                case "personName":
                    Pair<FormatParameters, NamePattern> pair = fromPathValue(parts, value);
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
                        Order order = Order.valueOf(parts.getAttributeValue(-1, "order"));
                        _localeToOrder.put(new ULocale(locale), order);
                    }
                    break;
                case "foreignSpaceReplacement":
                    foreignSpaceReplacement = value;
                    break;
                case "sampleName":
                    // skip
                    break;
                default: throw new IllegalArgumentException("Unexpected path: " + path);
                }
            }
        }
        for (Pair<FormatParameters, NamePattern> entry : ordered) {
            formatParametersToNamePattern.put(entry.getFirst(), entry.getSecond());
        }
        addMissing(formatParametersToNamePattern);

        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.copyOf(_localeToOrder);
        this.namePatternMap = new NamePatternData(localeToOrder, formatParametersToNamePattern);
        this.fallbackFormatter = new FallbackFormatter(new ULocale(cldrFile.getLocaleID()),
            initialPattern, initialSequencePattern, foreignSpaceReplacement, false);
    }

    /**
     * Add items that are not in the pattern, using the fallbacks.
     * TODO: can generalize; if we have order=x ... formality=y,
     * and a later value that matches except with formality=null,
     * and nothing in between matches, can drop the first
     */
    private static void addMissing(ListMultimap<FormatParameters, NamePattern> formatParametersToNamePattern) {
        for (FormatParameters formatParameters : FormatParameters.all()) {
            Collection<NamePattern> namePatterns = getBestMatchSet(formatParametersToNamePattern, formatParameters);
            if (namePatterns == null) {
                for (FormatParameters fallback : formatParameters.getFallbacks()) {
                    namePatterns = getBestMatchSet(formatParametersToNamePattern, fallback);
                    if (namePatterns != null) {
                        formatParametersToNamePattern.putAll(fallback, namePatterns);
                        break;
                    }
                }
                if (namePatterns == null) {
                    throw new IllegalArgumentException("Missing fallback for " + formatParameters);
                }
            }
        }
    }

    /**
     * Main function for formatting names.
     * @param nameObject — A name object, which supplies data.
     * @param nameFormatParameters - The specification of which parameters are desired.
     * @return formatted string
     * TODO make most public methods be @internal (public but just for testing).
     *      The NameObject and FormatParameters are exceptions.
     * TODO decide how to allow clients to customize data in the name object. Options:
     *      a. Leave it to implementers (eg they can write a FilteredNameObject that changes some fields).
     *      b. Pass in explicit override parameters, like whether to uppercase the surname in surnameFirst.
     * TODO decide whether/how to allow clients to customize the built-in data (namePatternData, fallbackFormatter)
     *      a. CLDR will need to be be able to customize it completely.
     *      b. Clients may want to set the contextual uppercasing of surnames, the handling of which locales cause surnameFirst, etc.
     */
    public String format(NameObject nameObject, FormatParameters nameFormatParameters) {
        // look through the namePatternMap to find the best match for the set of modifiers and the available nameObject fields
        NamePattern bestPattern = namePatternMap.getBestMatch(nameObject, nameFormatParameters);
        // then format using it
        return bestPattern.format(nameObject, nameFormatParameters, fallbackFormatter);
    }

    /**
     * For testing
     * @internal
     */
    public Collection<NamePattern> getBestMatchSet(FormatParameters nameFormatParameters) {
        return getBestMatchSet(namePatternMap.parameterMatcherToNamePattern, nameFormatParameters);
    }

    /**
     * Utility for constructing data from path and value.
     * @internal
     */
    public static Pair<FormatParameters, NamePattern> fromPathValue(XPathParts parts, String value) {
        //ldml/personNames/personName[@length="long"][@usage="referring"][@order="sorting"]/namePattern[alt="2"]
        // value = {surname}, {given} {given2} {suffix}
        final String altValue = parts.getAttributeValue(-1, "alt");
        int rank = altValue == null ? 0 : Integer.parseInt(altValue);
        FormatParameters pm = new FormatParameters(
            Order.from(parts.getAttributeValue(-2, "order")),
            Length.from(parts.getAttributeValue(-2, "length")),
            Usage.from(parts.getAttributeValue(-2, "usage")),
            Formality.from(parts.getAttributeValue(-2, "formality"))
            );

        NamePattern np = NamePattern.from(rank, value);
        if (np.toString().isBlank()) {
            throw new IllegalArgumentException("No empty patterns allowed: " + pm);
        }
        return Pair.of(pm, np);
    }

    /**
     * Special data for vetters, to how what foreign names would format
     */
    private static final Map<String, SimpleNameObject> FOREIGN_NAME_FOR_NON_SPACING;
    static {
        // code     given   surname
        final String[][] specials = {
            {"th", "อัลเบิร์ต", "ไอน์สไตน์"},
            {"my", "အဲလ်ဘတ်", "အိုင်းစတိုင်း"},
            {"km", "អាល់បឺត", "អែងស្តែង"},
            {"ko", "알베르트", "아인슈타인"},
            {"ja", "アルベルト", "アインシュタイン"},
            {"zh", "阿尔伯特", "爱因斯坦"}
        };
        Map<String, SimpleNameObject> temp = Maps.newLinkedHashMap();
        for (String[] row : specials) {
            temp.put(row[0], specialNameOf(row));
        }
        FOREIGN_NAME_FOR_NON_SPACING = ImmutableMap.copyOf(temp);
    }

    private static SimpleNameObject specialNameOf(String[] row) {
        return new SimpleNameObject(new ULocale("de_CH"), ImmutableMap.of(
            ModifiedField.from("given"), row[1],
            ModifiedField.from("surname"), row[2]
            ));
    }

    /**
     * Utility for getting sample names. DOES NOT CACHE
     * @param cldrFile
     * @return
     * @internal
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

        // add special foreign name for non-spacing languages
        LanguageTagParser ltp = new LanguageTagParser();
        SimpleNameObject extraName = FOREIGN_NAME_FOR_NON_SPACING.get(ltp.set(cldrFile.getLocaleID()).getLanguageScript());
        if (extraName != null) {
            result.put(SampleType.foreign, extraName);
        }
        return ImmutableMap.copyOf(result);
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
