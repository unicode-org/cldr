package org.unicode.cldr.util;

import com.google.common.base.Splitter;

public class Splitters {
    public static final Splitter VBAR = Splitter.on('|').trimResults();
    public static final Splitter COMMA_SP = Splitter.on(", ").trimResults();
    public static final Splitter SEMI = Splitter.on(';').trimResults();
    ;
}
