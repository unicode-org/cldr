package org.unicode.cldr.util;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.util.TimeZone;
import java.util.BitSet;
import java.util.Date;

@SuppressWarnings("deprecation")
public class CldrIntervalFormat {
    public final String calendar;
    public final String firstPattern;
    public final String separator;
    public final String secondPattern;

    private CldrIntervalFormat(
            String calendar, String firstPattern, String separator, String secondPattern) {
        this.calendar = calendar;
        this.firstPattern = firstPattern;
        this.separator = separator;
        this.secondPattern = secondPattern;
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
        DateTimePatternGenerator.FormatParser formatParser =
                new DateTimePatternGenerator.FormatParser();
        if (formatParser == null || pattern == null) {
            throw new IllegalArgumentException("Can't parse: «" + pattern + "»");
        }
        formatParser.set(pattern);
        BitSet variableFields = new BitSet();

        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        String sep = null;
        boolean doFirst = true;
        String literal = null;

        // We break at the first repeated date field: note that it is the type of field
        //
        for (Object item : formatParser.getItems()) {
            if (item instanceof DateTimePatternGenerator.VariableField) {
                int variableField = ((DateTimePatternGenerator.VariableField) item).getType();
                if (!variableFields.get(variableField)) {
                    variableFields.set(variableField);
                } else {
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
                } else {
                    if (literal != null) {
                        second.append(literal);
                    }
                    second.append(item);
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
        return new CldrIntervalFormat(calendar, first.toString(), sep, second.toString());
    }
}
