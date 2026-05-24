package org.unicode.cldr.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.util.TimeZone;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("deprecation")
public class CldrIntervalFormat {
    public final String calendar;
    public final String firstPattern;
    public final List<VariableField> firstFields;
    public final String separator;
    public final String secondPattern;
    public final List<VariableField> secondFields;

    private CldrIntervalFormat(
            String calendar,
            String firstPattern,
            List<VariableField> firstFields,
            String separator,
            String secondPattern,
            List<VariableField> secondFields) {
        this.calendar = calendar;
        this.firstPattern = firstPattern;
        this.firstFields = ImmutableList.copyOf(firstFields);
        this.separator = separator;
        this.secondPattern = secondPattern;
        this.secondFields = ImmutableList.copyOf(secondFields);
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
        DateTimePatternGenerator.FormatParser formatParser = formatParser(pattern);
        BitSet variableFields = new BitSet();

        StringBuilder first = new StringBuilder();
        List<VariableField> firstFields = new ArrayList<>();
        StringBuilder second = new StringBuilder();
        List<VariableField> secondFields = new ArrayList<>();

        String sep = null;
        boolean doFirst = true;
        String literal = null;

        // We break at the first repeated date field: note that it is the type of field
        //
        for (Object item : formatParser.getItems()) {
            if (item instanceof DateTimePatternGenerator.VariableField) {
                DateTimePatternGenerator.VariableField vField =
                        (DateTimePatternGenerator.VariableField) item;
                int variableField = vField.getType();
                if (!variableFields.get(variableField)) {
                    variableFields.set(variableField);
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
                        first.append(literal);
                    }
                    first.append(item);
                    firstFields.add(vField);
                } else {
                    if (literal != null) {
                        second.append(literal);
                    }
                    second.append(item);
                    secondFields.add(vField);
                }
                literal = null; // we have absorbed the literal
            } else {
                // we will never get two literals in a row
                literal = (String) formatParser.quoteLiteral((String) item);
            }
        }
        if (second.length() == 0) {
            throw new IllegalArgumentException(
                    "Interval patterns must have two parts, with a separator between: «"
                            + pattern
                            + "»");
        }
        if (literal != null) {
            // append any trailing literal
            second.append(literal);
        }
        return new CldrIntervalFormat(
                calendar, first.toString(), firstFields, sep, second.toString(), secondFields);
    }

    private static DateTimePatternGenerator.FormatParser formatParser(String pattern) {
        DateTimePatternGenerator.FormatParser formatParser =
                new DateTimePatternGenerator.FormatParser();
        if (formatParser == null || pattern == null) {
            throw new IllegalArgumentException("Can't parse: «" + pattern + "»");
        }
        return formatParser.set(pattern);
    }

    public static String removeMissingFieldsFromSkeleton(
            String skeleton, Collection<VariableField> firstFields) {
        DateTimePatternGenerator.FormatParser formatParser = formatParser(skeleton);
        StringBuilder result = new StringBuilder();
        for (Object item : formatParser.getItems()) {
            VariableField vField = (DateTimePatternGenerator.VariableField) item;
            if (hasType(firstFields, vField.getType())) {
                result.append(vField.toString());
            }
        }
        return result.toString();
    }

    private static boolean hasType(Collection<VariableField> firstFields2, int type) {
        return firstFields2.stream().anyMatch(x -> x.getType() == type);
    }
}
