package org.unicode.cldr.util;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.TimeZone;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
                    sep = literal;
                    literal = null;
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
        equal,
        count,
        sep,
        type,
        num,
        len,
        lit,
        other;

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
            if (fields.size() != fields2.size()) {
                result.add(IntervalDiff.count);
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
                            result.add(IntervalDiff.len);
                        } else {
                            result.add(IntervalDiff.other);
                        }
                    }
                }
            }
        }
    }
}
