package org.unicode.cldr.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.ibm.icu.impl.PatternTokenizer;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.UnicodeSet;

public class DateTimeCanonicalizer {

    public enum DateTimePatternType {
        NA, STOCK, AVAILABLE, INTERVAL, GMT;

        public static final Set<DateTimePatternType> STOCK_AVAILABLE_INTERVAL_PATTERNS = Collections
            .unmodifiableSet(EnumSet.of(DateTimePatternType.STOCK, DateTimePatternType.AVAILABLE,
                DateTimePatternType.INTERVAL));

        public static DateTimePatternType fromPath(String path) {
            return !path.contains("/dates") ? DateTimePatternType.NA
                : path.contains("/pattern") && (path.contains("/dateFormats") || path.contains("/timeFormats") || path.contains("/dateTimeFormatLength"))
                    ? DateTimePatternType.STOCK
                    : path.contains("/dateFormatItem") ? DateTimePatternType.AVAILABLE
                        : path.contains("/intervalFormatItem") ? DateTimePatternType.INTERVAL
                            : path.contains("/timeZoneNames/hourFormat") ? DateTimePatternType.GMT
                                : DateTimePatternType.NA;
        }
    }

    private boolean fixYears = false; // true to fix the years to y

    private FormatParser formatDateParser = new FormatParser();

    // TODO make ICU's FormatParser.PatternTokenizer public (and clean up API)

    private transient PatternTokenizer tokenizer = new PatternTokenizer()
        .setSyntaxCharacters(new UnicodeSet("[a-zA-Z]"))
        .setExtraQuotingCharacters(new UnicodeSet("[[[:script=Latn:][:script=Cyrl:]]&[[:L:][:M:]]]"))
        // .setEscapeCharacters(new UnicodeSet("[^\\u0020-\\u007E]")) // WARNING: DateFormat doesn't accept \\uXXXX
        .setUsingQuote(true);

    public DateTimeCanonicalizer(boolean fixYears) {
        this.fixYears = fixYears;
    }

    public String getCanonicalDatePattern(String path, String value, DateTimePatternType datetimePatternType) {
        formatDateParser.set(value);

        // ensure that all y fields are single y, except for the stock short, which can be y or yy.
        String newValue;
        if (fixYears) {
            StringBuilder result = new StringBuilder();
            for (Object item : formatDateParser.getItems()) {
                String itemString = item.toString();
                if (item instanceof String) {
                    result.append(tokenizer.quoteLiteral(itemString));
                } else if (!itemString.startsWith("y")
                    || (datetimePatternType == DateTimePatternType.STOCK
                        && path.contains("short")
                        && itemString.equals("yy"))) {
                    result.append(itemString);
                } else {
                    result.append('y');
                }
            }
            newValue = result.toString();
        } else {
            newValue = formatDateParser.toString();
        }

        if (!value.equals(newValue)) {
            value = newValue;
        }
        return value;
    }
}