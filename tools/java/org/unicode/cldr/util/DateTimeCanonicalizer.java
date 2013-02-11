package org.unicode.cldr.util;

import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.DisplayAndInputProcessor.DateTimePatternType;

import com.ibm.icu.impl.PatternTokenizer;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;

public class DateTimeCanonicalizer {
    private static final boolean FIX_YEARS = false; // true to fix the years to y

    private FormatParser formatDateParser = new FormatParser();
    
    // TODO make ICU's FormatParser.PatternTokenizer public (and clean up API)
    
    private transient PatternTokenizer tokenizer = new PatternTokenizer()
    .setSyntaxCharacters(new UnicodeSet("[a-zA-Z]"))
    .setExtraQuotingCharacters(new UnicodeSet("[[[:script=Latn:][:script=Cyrl:]]&[[:L:][:M:]]]"))
    //.setEscapeCharacters(new UnicodeSet("[^\\u0020-\\u007E]")) // WARNING: DateFormat doesn't accept \\uXXXX
    .setUsingQuote(true);

    public String getCanonicalDatePattern(String path, String value, DateTimePatternType datetimePatternType) {
        formatDateParser.set(value);

        // ensure that all y fields are single y, except for the stock short, which can be y or yy.
        String newValue;
        if (FIX_YEARS) {
            StringBuilder result = new StringBuilder();
            for (Object item : formatDateParser.getItems()) {
                String itemString = item.toString();
                if (item instanceof String) {
                    result.append(tokenizer.quoteLiteral(itemString));
                } else {
                    VariableField vf = (VariableField) item;
                    if (vf.getType() != DateTimePatternGenerator.YEAR
                        || (
                            datetimePatternType == DateTimePatternType.STOCK 
                            && path.contains("short") 
                            && itemString.equals("yy"))) {
                        result.append(itemString);
                    } else {
                        result.append('y');
                    }
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