package org.unicode.cldr.util;

import com.ibm.icu.text.UnicodeSet;
import java.util.regex.Pattern;

public class Patterns {

    public static final Pattern WHITESPACE =
            Pattern.compile(new UnicodeSet("\\p{whitespace}").complement().complement().toString());
    public static final String CURRENCY_PLACEHOLDER = "\u00a4";
    public static final Pattern CURRENCY_PLACEHOLDER_AND_POSSIBLE_WS =
            Pattern.compile(WHITESPACE + "*" + CURRENCY_PLACEHOLDER + WHITESPACE + "*");
    public static final Pattern PATTERN_ZEROS = Pattern.compile("0+");
}
