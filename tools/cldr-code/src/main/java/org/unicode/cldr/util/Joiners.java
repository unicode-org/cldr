package org.unicode.cldr.util;

import com.google.common.base.Joiner;

public class Joiners {
    public static final Joiner VBAR = Joiner.on('|').useForNull("null");
    public static final Joiner TAB = Joiner.on('\t').useForNull("null");
    public static final Joiner COMMA_SP = Joiner.on(", ").useForNull("null");
}
