// New implementation based on compile errors from CLDR
// @author Steven R. Loomis
// SPDX-License-Identifier: Unicode-3.0
// Date: 7 May 2024
// Copyright © 2024 Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode,
// Inc. in the United States and other countries.

package com.ibm.icu.dev.util;

import com.google.common.base.Stopwatch;
import org.unicode.cldr.util.TimeDiff;

/**
 * simple class to calculate elapsed times.
 *
 * @deprecated use {@link com.google.common.base.Stopwatch} instead
 */
@Deprecated
public class ElapsedTimer {
    final Stopwatch s;
    final String str;

    public ElapsedTimer() {
        s = Stopwatch.createStarted();
        str = "";
    }

    public ElapsedTimer(String string) {
        str = string;
        s = Stopwatch.createStarted();
    }

    @Override
    public String toString() {
        return str + ": " + s.toString();
    }

    /**
     * @deprecated simplistic implementation
     */
    @Deprecated
    public static String elapsedTime(long startMs) {
        return elapsedTime(startMs, System.currentTimeMillis());
    }

    /**
     * @deprecated simplistic implementation
     */
    @Deprecated
    public static String elapsedTime(long startMs, long endMs) {
        return TimeDiff.timeDiff(startMs, endMs);
    }
}
