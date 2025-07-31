package org.unicode.cldr.util;

import com.google.common.base.Joiner;

public class Joiners {
    public static final Joiner VBAR = Joiner.on('|').useForNull("null");
    public static final Joiner TAB = Joiner.on('\t').useForNull("null");
    public static final Joiner COMMA_SP = Joiner.on(", ").useForNull("null");
    public static final Joiner N = Joiner.on("\n").useForNull("null");
    public static final Joiner SP = Joiner.on(' ').useForNull("null");
    public static final Joiner VBAR_SP = Joiner.on(" | ").skipNulls();
    public static final Joiner ES = Joiner.on("").useForNull("null");
    public static final Joiner ES_BLANK_NULLS = Joiner.on("").useForNull("");
    public static final Joiner SEMI = Joiner.on(';').useForNull("null");
}
