package org.unicode.cldr.util;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleFormatter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.DatetimeUtilities.FieldType;
import org.unicode.cldr.util.DatetimeUtilities.PatternElement;

@SuppressWarnings("deprecation")
public class CldrIntervalFormat {
    public final String calendar;
    public final List<PatternElement> firstFields;
    public final PatternElement separator;
    public final List<PatternElement> secondFields;
    public final String firstPattern;
    public final String separatorString;
    public final String secondPattern;

    private CldrIntervalFormat(
            String calendar,
            List<PatternElement> firstFields,
            PatternElement separator,
            List<PatternElement> secondFields) {
        this.calendar = calendar;
        this.firstFields = ImmutableList.copyOf(firstFields);
        this.separator = separator;
        this.secondFields = ImmutableList.copyOf(secondFields);
        this.firstPattern = PatternElement.listToPattern(firstFields);
        this.separatorString = separator.toString();
        this.secondPattern = PatternElement.listToPattern(secondFields);
    }

    public String format(
            Date earlier, Date later, ICUServiceBuilder icuServiceBuilder, TimeZone timezone) {
        if (earlier == null || later == null) {
            return null;
        }
        if (later.compareTo(earlier) < 0) {
            /*
             * Swap so earlier is earlier than later.
             * This is necessary for "G" (Era) given the current FIRST_INTERVAL, SECOND_INTERVAL
             */
            Date tmp = earlier;
            earlier = later;
            later = tmp;
        }
        DateFormat firstFormat = icuServiceBuilder.getDateFormat(calendar, firstPattern);
        firstFormat.setTimeZone(timezone);

        DateFormat secondFormat = icuServiceBuilder.getDateFormat(calendar, secondPattern);
        firstFormat.setTimeZone(timezone);

        return firstFormat.format(earlier) + separator + secondFormat.format(later);
    }

    public static CldrIntervalFormat getInstance(String calendar, String pattern) {
        List<PatternElement> patternElements = DatetimeUtilities.getPatternElements(pattern);
        Set<FieldType> variableFields = EnumSet.noneOf(FieldType.class);

        List<PatternElement> firstFields = new ArrayList<>();
        List<PatternElement> secondFields = new ArrayList<>();

        boolean doFirst = true;

        PatternElement sep = null;
        PatternElement literal = null;

        // We break at the first repeated date field: note that it is the type of field
        //
        for (PatternElement item : patternElements) {
            FieldType type = item.getType();
            if (type != FieldType.LITERAL) {
                FieldType variableField = type;
                if (!variableFields.contains(variableField)) {
                    variableFields.add(variableField);
                } else if (doFirst) {
                    doFirst = false;
                    if (literal == null) {
                        throw new IllegalArgumentException(
                                "Missing literal between first and second formats in «"
                                        + pattern
                                        + "»");
                    }
                    Output<PatternElement> literalBefore = new Output<>();
                    Output<PatternElement> literalAfter = new Output<>();
                    sep = split(literal, literalBefore, literalAfter);
                    if (literalBefore.value != null) {
                        firstFields.add(literalBefore.value);
                    }
                    literal = literalAfter.value;
                }
                if (doFirst) {
                    if (literal != null) {
                        firstFields.add(literal);
                    }
                    firstFields.add(item);
                } else {
                    if (literal != null) {
                        secondFields.add(literal);
                    }
                    secondFields.add(item);
                }
                literal = null; // we have absorbed the literal
            } else {
                // we will never get two literals in a row
                literal = item;
            }
        }
        if (firstFields.isEmpty() || secondFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Interval patterns must have two parts, with a separator between: «"
                            + pattern
                            + "»");
        }
        if (literal != null) {
            // append any trailing literal
            secondFields.add(literal);
        }
        return new CldrIntervalFormat(calendar, firstFields, sep, secondFields);
    }

    @Override
    public String toString() {
        return firstPattern + separatorString + secondPattern;
    }

    /** These may need to be adjusted if new locales do something radically different. */
    public static final UnicodeSet SEPARATORS = new UnicodeSet("[-–—～~至/]").freeze();

    public static final UnicodeSet SEPARATOR_SPACINGS =
            new UnicodeSet("[\\u0020\\u00A0\\u202F\\u2009]").freeze();

    public static final UnicodeSet SEPARATORS_WITH_SPACES =
            new UnicodeSet(SEPARATORS).addAll(SEPARATOR_SPACINGS).freeze();

    static final Pattern normal =
            Pattern.compile(SEPARATOR_SPACINGS + "?" + SEPARATORS + SEPARATOR_SPACINGS + "?");

    /**
     * In some languages, there is a literal between the last field of the first part, and the first
     * field of the next. Examples: <br>
     * Gy年M月d日(E)～Gy年M月d日(E) <br>
     * E, dd. – E, dd.MM.y
     *
     * @param literal
     * @param literalRemainder
     * @return
     */
    private static PatternElement split(
            PatternElement literal,
            Output<PatternElement> literalBefore,
            Output<PatternElement> literalAfter) {
        String rawString = literal.rawString();
        Matcher matcher = normal.matcher(rawString);
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start != 0 || end != rawString.length()) {
                literalBefore.value =
                        start != 0 ? PatternElement.from(rawString.substring(0, start)) : null;
                literalAfter.value =
                        end != rawString.length()
                                ? PatternElement.from(rawString.substring(end))
                                : null;
                return PatternElement.from(rawString.substring(start, end));
            }
        }
        literalBefore.value = null;
        literalAfter.value = null;
        return literal;
    }

    public static String removeMissingFieldsFromSkeleton(
            String skeleton, Collection<PatternElement> firstFields) {
        List<PatternElement> patternElements = DatetimeUtilities.getPatternElements(skeleton);

        StringBuilder result = new StringBuilder();
        for (PatternElement item : patternElements) {
            if (hasType(firstFields, item.getType())) {
                result.append(item);
            }
        }
        return result.toString();
    }

    private static boolean hasType(Collection<PatternElement> firstFields2, FieldType type) {
        return firstFields2.stream().anyMatch(x -> x.getType() == type);
    }

    public enum IntervalDiff {
        fewer("for prefix or suffix, constructed has fewer fields"),
        more("for prefix or suffix, constructed has more fields"),
        sep("the two separators are different"),
        lit("two fields are different literals; eg ' de ' vs ' d’ '"),
        type(
                "two fields have different types, eg y vs M or y vs ' de '; can be caused by fields in different order"),
        num("two fields have the same type, but one is numeric and the other isn’t; eg MMM vs M"),
        width(
                "two fields have the same type & numeric status, but are of different width; eg MMMM vs MMM, or d vs dd"),
        other("two fields have the same type, numeric status, and length, eg L vs M");
        public final String description;

        private IntervalDiff(String description) {
            this.description = description;
        }

        public static Set<IntervalDiff> compare(
                String actualPattern,
                String constructedPattern,
                CldrIntervalFormat actualIF,
                CldrIntervalFormat constructedIF) {
            if (actualPattern.equals(constructedPattern)) {
                return Set.<IntervalDiff>of();
            }
            Set<IntervalDiff> result = EnumSet.noneOf(IntervalDiff.class);
            if (!actualIF.separator.equals(constructedIF.separator)) {
                result.add(IntervalDiff.sep);
            }
            addDiffs(actualIF.firstFields, constructedIF.firstFields, result);
            addDiffs(actualIF.secondFields, constructedIF.secondFields, result);
            return result;
        }

        private static void addDiffs(
                List<PatternElement> fields,
                List<PatternElement> fields2,
                Set<IntervalDiff> result) {
            if (fields.size() < fields2.size()) {
                result.add(IntervalDiff.more);
            }
            if (fields.size() > fields2.size()) {
                result.add(IntervalDiff.fewer);
            } else {
                for (int i = 0; i < fields.size(); ++i) {
                    PatternElement pe1 = fields.get(i);
                    PatternElement pe2 = fields2.get(i);
                    if (pe1.equals(pe2)) {
                        continue;
                    }
                    FieldType type1 = pe1.getType();
                    FieldType type2 = pe2.getType();
                    if (type1 != type2) {
                        result.add(IntervalDiff.type);
                        continue;
                    }

                    if (type1 == FieldType.LITERAL) {
                        result.add(IntervalDiff.lit);
                    } else {
                        if (pe1.isNumeric() != pe2.isNumeric()) {
                            result.add(IntervalDiff.num);
                        } else if (pe1.toString().length() != pe2.toString().length()) {
                            result.add(IntervalDiff.width);
                        } else {
                            result.add(IntervalDiff.other);
                        }
                    }
                }
            }
        }
    }

    static final Date sampleStartDate = Date.from(Instant.parse("2026-11-25T09:35:45Z"));
    static final Map<String, Date> endDates =
            Map.of(
                    "s", Date.from(Instant.parse("2026-11-25T09:35:50Z")),
                    "m", Date.from(Instant.parse("2026-11-25T09:40:50Z")),
                    "h", Date.from(Instant.parse("2026-11-25T10:40:50Z")),
                    "a", Date.from(Instant.parse("2026-11-25T21:35:50Z")),
                    "d", Date.from(Instant.parse("2026-11-26T21:35:50Z")),
                    "M", Date.from(Instant.parse("2026-12-26T21:35:50Z")),
                    "y", Date.from(Instant.parse("2027-12-26T21:35:50Z")),
                    "G", Date.from(Instant.parse("-2026-12-26T21:35:50Z")));

    public static Date getSampleStartDate() {
        return sampleStartDate;
    }

    public static Date getSampleEndDate(String greatestDifference) {
        return endDates.get(greatestDifference);
    }

    public static class IntervalPatternConstructor {
        private final CLDRFile cldrFile;
        private final String calendar;
        private final SimpleFormatter numericSeparator;
        private final SimpleFormatter nonNumericSeparator;
        private final SimpleFormatter mixedSeparator;
        private final SimpleFormatter fallbackSeparator;

        public IntervalPatternConstructor(CLDRFile cldrFile, String calendar) {
            if (!cldrFile.isResolved()) {
                throw new DatetimeException("CLDRFile must be resolved");
            }
            this.cldrFile = cldrFile;
            this.calendar = calendar;
            this.numericSeparator =
                    SimpleFormatter.compile(
                            cldrFile.getStringValue(
                                    CldrPathUtilities.intervalSeparator(calendar, "numeric")));
            this.nonNumericSeparator =
                    SimpleFormatter.compile(
                            cldrFile.getStringValue(
                                    CldrPathUtilities.intervalSeparator(calendar, "non-numeric")));
            this.mixedSeparator =
                    SimpleFormatter.compile(
                            cldrFile.getStringValue(
                                    CldrPathUtilities.intervalSeparator(calendar, "mixed")));
            this.fallbackSeparator =
                    SimpleFormatter.compile(
                            cldrFile.getStringValue(
                                    CldrPathUtilities.intervalFormatFallback(calendar)));
        }

        static class DatetimeException extends RuntimeException {
            private static final long serialVersionUID = 1L;

            public DatetimeException(String message) {
                super(message);
            }

            public DatetimeException(String message, Throwable cause) {
                super(message, cause);
            }
        }

        /**
         * Constructs an interval pattern from available formats
         *
         * @param fields
         * @param greatestDifference
         * @param available
         * @return
         */
        public String construct(
                String fields, String greatestDifference, Output<String> available) {
            // get the full format from the fields
            String fullFormat =
                    cldrFile.getStringValue(CldrPathUtilities.availableFormat(calendar, fields));
            if (fullFormat == null) {
                if (fields.equals("MMMM")) {
                    fields = "MMM";
                } else if (fields.equals("Hvvvv")) {
                    fields = "Hv";
                } else if (fields.equals("hvvvv")) {
                    fields = "hv";
                }
                fullFormat =
                        cldrFile.getStringValue(
                                CldrPathUtilities.availableFormat(calendar, fields));
            }
            available.value = fullFormat;
            if (fullFormat == null) {
                throw new DatetimeException(
                        "Missing available format for " + fields + " in " + calendar);
            }
            return constructInternal(fullFormat, greatestDifference);
        }

        public String constructInternal(String fullFormat, String greatestDifference) {
            List<DatetimeUtilities.PatternElement> diff =
                    DatetimeUtilities.getPatternElements(greatestDifference);
            if (diff.isEmpty()) {
                throw new DatetimeException("Ill-formed greatest difference" + greatestDifference);
            }
            DatetimeUtilities.PatternElement greatestIntervalElement = diff.get(0);

            DatetimeUtilities.FieldType greatestIntervalType = greatestIntervalElement.getType();

            // We are dividing up the elements into the following:
            //     prefix postPrefix-literal variable preSuffix-literal suffix
            // The prefix and suffix are constant, given the greatest difference
            // While the variable part changes.
            // Example: "G_MMMM_d,_y" (with _ standing for space)
            // prefix: G
            // postPrefix: _
            // variable: d
            // postSuffix: ,_
            // suffix: y
            //
            // The end result will be an interval of the form
            //     prefix fix(postPrefix-literal) variable separator variable fix(preSuffix-literal)
            // suffix
            //
            // A key difficulty is that either postPrefix or preSuffix could be
            //   * a field separator (d/y ==> /)
            //   * terminator (25日 ==> 日, or ,_ above)
            //   * or in theory, an initiator, or a combination of the three.
            //
            // We need to suppress the separators, and retain the terminators/initiators
            // so that's what the fix(...) stands for.

            List<DatetimeUtilities.PatternElement> patternElements =
                    DatetimeUtilities.getPatternElements(fullFormat);
            // find the first element that is larger (in type) than the greatestIntervalElement
            // because the pattern element types are ordered from largest to smallest, that means
            // less than.
            // also optionally fill in a 'beforeLeast' literal

            boolean allFieldsVariable;

            String suffix;
            PatternElement firstVariableElement;

            { // for clarity, use separate blocks
                PatternElement postSeparatorLiteral = null;
                int firstVariableIndex = patternElements.size();
                for (int i = 0; i < patternElements.size(); ++i) {
                    DatetimeUtilities.FieldType type = patternElements.get(i).getType();
                    if (type == DatetimeUtilities.FieldType.LITERAL
                            || type.largerThan(greatestIntervalType)) {
                        // constants
                    } else {
                        firstVariableIndex = i;
                        if (i > 0) {
                            PatternElement elementBefore = patternElements.get(i - 1);
                            if (elementBefore.getType() == DatetimeUtilities.FieldType.LITERAL) {
                                postSeparatorLiteral = fix(elementBefore, true);
                            }
                        }
                        break;
                    }
                }
                firstVariableElement = patternElements.get(firstVariableIndex);
                List<PatternElement> suffixElements =
                        patternElements.subList(firstVariableIndex, patternElements.size());
                suffix =
                        suffixElements.stream()
                                .map(x -> x.toString())
                                .collect(Collectors.joining());
                if (postSeparatorLiteral != null) {
                    suffix = postSeparatorLiteral + suffix;
                }
                allFieldsVariable = firstVariableIndex == patternElements.size();
            }

            // do the same for the highest element backwards from the end
            // that is larger (in type) than the greatestIntervalElement

            PatternElement greatestVariableElement;
            String prefix;

            { // for clarity, use separate blocks
                PatternElement preSeparatorLiteral = null;
                int lastVariableIndex = 0;
                int last = patternElements.size() - 1;
                for (int i = last; i > 0; --i) {
                    DatetimeUtilities.FieldType type = patternElements.get(i).getType();
                    if (type == DatetimeUtilities.FieldType.LITERAL
                            || type.largerThan(greatestIntervalType)) {
                        // constants
                    } else {
                        lastVariableIndex = i;
                        if (i < last) {
                            DatetimeUtilities.PatternElement elementAfter =
                                    patternElements.get(i + 1);
                            if (elementAfter.getType() == DatetimeUtilities.FieldType.LITERAL) {
                                preSeparatorLiteral = fix(elementAfter, false);
                            }
                        }
                        break;
                    }
                }
                List<PatternElement> prefixElements =
                        patternElements.subList(0, lastVariableIndex + 1);
                prefix =
                        prefixElements.stream()
                                .map(x -> x.toString())
                                .collect(Collectors.joining());
                if (preSeparatorLiteral != null) {
                    prefix = prefix + preSeparatorLiteral;
                }
                greatestVariableElement = patternElements.get(lastVariableIndex);
            }

            // The end result will be an interval of the form
            //     prefix variable fix(preSuffix-literal)
            //     separator
            //     fix(postPrefix-literal) variable suffix
            // where the prefix and suffix are constant, and variable1/2 vary between the two dates.

            // get the right separator, based on the adjacent fields
            SimpleFormatter separatorPattern = null;
            if (allFieldsVariable) { // if all fields are variable, use the fallback
                separatorPattern = fallbackSeparator;
            } else {
                // the elements on either side of the separator will be
                // greatest <SEP> least
                if (firstVariableElement.getType() == greatestVariableElement.getType()) {
                    separatorPattern =
                            firstVariableElement.isNumeric()
                                    ? numericSeparator
                                    : nonNumericSeparator;
                } else {
                    separatorPattern = mixedSeparator;
                }
            }

            return separatorPattern.format(prefix, suffix);
        }

        private PatternElement fix(PatternElement postPrefixOrPreSuffix, boolean isSuffix) {
            // we know that the literal is not empty
            String element = postPrefixOrPreSuffix.rawString();
            if (element.contains("\u202F") || element.contains("\u2009")) {
                int debug = 0;
            }
            String processed;
            if (isSuffix) {
                processed = stripLeading(element);
                if (!processed.isEmpty() && TRAILING.containsAll(processed)) {
                    processed = "";
                }
            } else {
                processed = striptrailing(element);
            }
            if (!processed.isEmpty() && COMMON_FIELD_SEPARATORS.containsAll(processed)) {
                processed = "";
            }

            return element.equals(processed)
                    ? postPrefixOrPreSuffix
                    : PatternElement.from(processed);
        }
    }

    static UnicodeSet COMMON_FIELD_SEPARATORS = new UnicodeSet("[-/,.:]").freeze();
    static final UnicodeSet TRAILING = new UnicodeSet("[年月日]");

    public static String striptrailing(String element) {
        // processed = element.stripTrailing(); fails
        int firstNonSpace = SEPARATOR_SPACINGS.spanBack(element, SpanCondition.SIMPLE);
        return firstNonSpace == element.length() ? element : element.substring(0, firstNonSpace);
    }

    public static String stripLeading(String element) {
        // processed = element.stripLeading(); fails
        int firstNonSpace = SEPARATOR_SPACINGS.span(element, SpanCondition.SIMPLE);
        return firstNonSpace == 0 ? element : element.substring(firstNonSpace);
    }
}
