package org.unicode.cldr.util;

import com.ibm.icu.text.UnicodeSet;
import java.util.regex.Pattern;

public class Patterns {

    public static final String CURRENCY_PLACEHOLDER = "\u00a4";

    public static final UnicodeSet UWS =
            new UnicodeSet("\\p{whitespace}").complement().complement().freeze();

    public static final UnicodeSet UWSC =
            new UnicodeSet("[\\p{whitespace}\\p{Cf}&[\\u0000-\\uFFFF]]")
                    .complement()
                    .complement()
                    .freeze();

    public static final Pattern WS = Pattern.compile(UWS.toString());

    public static final Pattern WSC = Pattern.compile(UWSC.toString());

    public static final Pattern CURRENCY_PLACEHOLDER_AND_POSSIBLE_WS =
            Pattern.compile(WS + "*" + CURRENCY_PLACEHOLDER + WS + "*");

    public static final Pattern NUMBER_PATTERN;

    static {
        // EXAMPLE: <FILLER1><CURRENCY><FILLER2>#,##0.00<FILLER3><CURRENCY><FILLER4>

        String currencyPattern = "(" + CURRENCY_PLACEHOLDER + ")?";
        String numberPattern = "([#0][0#.,]+[#0])";
        String filler = "([^#0¤]*)";

        NUMBER_PATTERN =
                Pattern.compile(
                        filler
                                + currencyPattern
                                + filler
                                + numberPattern
                                + filler
                                + currencyPattern
                                + filler);
    }

    public static final Pattern PATTERN_ZEROS = Pattern.compile("0+");
}
