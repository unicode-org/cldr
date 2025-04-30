package org.unicode.cldr.util;

import com.google.common.base.Joiner;

public class Joiners {
    public static final Joiner VBAR = Joiner.on('|').useForNull("null");
}
