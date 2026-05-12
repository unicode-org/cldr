package org.unicode.cldr.util;

import com.google.common.base.Joiner;

public class SkeletonWeights {

    private static final int // numbers are chosen to express 'distance'
            DELTA = 0x10,
            NUMERIC = 0x100,
            NONE = 0,
            NARROW = -0x101,
            SHORTER = -0x102,
            SHORT = -0x103,
            LONG = -0x104,
            EXTRA_FIELD = 0x10000,
            MISSING_FIELD = 0x1000;

    enum Field {
        ERA,
        YEAR,
        QUARTER,
        MONTH,
        WEEK_OF_YEAR,
        WEEK_OF_MONTH,
        WEEKDAY,
        DAY,
        DAY_OF_YEAR,
        DAY_OF_WEEK_IN_MONTH,
        DAYPERIOD,
        HOUR,
        MINUTE,
        SECOND,
        FRACTIONAL_SECOND,
        ZONE
    }

    static {
        System.out.println("Missing Field " + MISSING_FIELD);
        System.out.println("Extra Field " + EXTRA_FIELD);

        // the order here makes a difference only when searching for single field.
        // format is:
        // pattern character, main type, weight, min length, weight
        addData("G", Field.ERA, SHORT, 1, 3);
        addData("G", Field.ERA, LONG, 4);
        addData("G", Field.ERA, NARROW, 5);
        addData("y", Field.YEAR, NUMERIC, 1, 20);
        addData("Y", Field.YEAR, NUMERIC + DELTA, 1, 20);
        addData("u", Field.YEAR, NUMERIC + 2 * DELTA, 1, 20);
        addData("r", Field.YEAR, NUMERIC + 3 * DELTA, 1, 20);
        addData("U", Field.YEAR, SHORT, 1, 3);
        addData("U", Field.YEAR, LONG, 4);
        addData("U", Field.YEAR, NARROW, 5);
        addData("Q", Field.QUARTER, NUMERIC, 1, 2);
        addData("Q", Field.QUARTER, SHORT, 3);
        addData("Q", Field.QUARTER, LONG, 4);
        addData("Q", Field.QUARTER, NARROW, 5);
        addData("q", Field.QUARTER, NUMERIC + DELTA, 1, 2);
        addData("q", Field.QUARTER, SHORT - DELTA, 3);
        addData("q", Field.QUARTER, LONG - DELTA, 4);
        addData("q", Field.QUARTER, NARROW - DELTA, 5);
        addData("M", Field.MONTH, NUMERIC, 1, 2);
        addData("M", Field.MONTH, SHORT, 3);
        addData("M", Field.MONTH, LONG, 4);
        addData("M", Field.MONTH, NARROW, 5);
        addData("L", Field.MONTH, NUMERIC + DELTA, 1, 2);
        addData("L", Field.MONTH, SHORT - DELTA, 3);
        addData("L", Field.MONTH, LONG - DELTA, 4);
        addData("L", Field.MONTH, NARROW - DELTA, 5);
        addData("l", Field.MONTH, NUMERIC + DELTA, 1, 1);
        addData("w", Field.WEEK_OF_YEAR, NUMERIC, 1, 2);
        addData("W", Field.WEEK_OF_MONTH, NUMERIC, 1);
        addData("E", Field.WEEKDAY, SHORT, 1, 3);
        addData("E", Field.WEEKDAY, LONG, 4);
        addData("E", Field.WEEKDAY, NARROW, 5);
        addData("E", Field.WEEKDAY, SHORTER, 6);
        addData("c", Field.WEEKDAY, NUMERIC + 2 * DELTA, 1, 2);
        addData("c", Field.WEEKDAY, SHORT - 2 * DELTA, 3);
        addData("c", Field.WEEKDAY, LONG - 2 * DELTA, 4);
        addData("c", Field.WEEKDAY, NARROW - 2 * DELTA, 5);
        addData("c", Field.WEEKDAY, SHORTER - 2 * DELTA, 6);
        addData(
                "e",
                Field.WEEKDAY,
                NUMERIC + DELTA,
                1,
                2); // "e" is currently not used in CLDR data, should not be canonical
        addData("e", Field.WEEKDAY, SHORT - DELTA, 3);
        addData("e", Field.WEEKDAY, LONG - DELTA, 4);
        addData("e", Field.WEEKDAY, NARROW - DELTA, 5);
        addData("e", Field.WEEKDAY, SHORTER - DELTA, 6);
        addData("d", Field.DAY, NUMERIC, 1, 2);
        addData("g", Field.DAY, NUMERIC + DELTA, 1, 20); // really internal use, so we don"t care
        addData("D", Field.DAY_OF_YEAR, NUMERIC, 1, 3);
        addData("F", Field.DAY_OF_WEEK_IN_MONTH, NUMERIC, 1);
        addData("a", Field.DAYPERIOD, SHORT, 1, 3);
        addData("a", Field.DAYPERIOD, LONG, 4);
        addData("a", Field.DAYPERIOD, NARROW, 5);
        addData("b", Field.DAYPERIOD, SHORT - DELTA, 1, 3);
        addData("b", Field.DAYPERIOD, LONG - DELTA, 4);
        addData("b", Field.DAYPERIOD, NARROW - DELTA, 5);
        // b needs to be closer to a than to B, so we make this 3*DELTA
        addData("B", Field.DAYPERIOD, SHORT - 3 * DELTA, 1, 3);
        addData("B", Field.DAYPERIOD, LONG - 3 * DELTA, 4);
        addData("B", Field.DAYPERIOD, NARROW - 3 * DELTA, 5);
        addData("H", Field.HOUR, NUMERIC + 10 * DELTA, 1, 2); // 24 hour
        addData("k", Field.HOUR, NUMERIC + 11 * DELTA, 1, 2);
        addData("h", Field.HOUR, NUMERIC, 1, 2); // 12 hour
        addData("K", Field.HOUR, NUMERIC + DELTA, 1, 2);
        addData("m", Field.MINUTE, NUMERIC, 1, 2);
        addData("s", Field.SECOND, NUMERIC, 1, 2);
        addData("A", Field.SECOND, NUMERIC + DELTA, 1, 1000);
        addData("S", Field.FRACTIONAL_SECOND, NUMERIC, 1, 1000);
        addData("v", Field.ZONE, SHORT - 2 * DELTA, 1);
        addData("v", Field.ZONE, LONG - 2 * DELTA, 4);
        addData("z", Field.ZONE, SHORT, 1, 3);
        addData("z", Field.ZONE, LONG, 4);
        addData("Z", Field.ZONE, NARROW - DELTA, 1, 3);
        addData("Z", Field.ZONE, LONG - DELTA, 4);
        addData("Z", Field.ZONE, SHORT - DELTA, 5);
        addData("O", Field.ZONE, SHORT - DELTA, 1);
        addData("O", Field.ZONE, LONG - DELTA, 4);
        addData("V", Field.ZONE, SHORT - DELTA, 1);
        addData("V", Field.ZONE, LONG - DELTA, 2);
        addData("V", Field.ZONE, LONG - 1 - DELTA, 3);
        addData("V", Field.ZONE, LONG - 2 - DELTA, 4);
        addData("X", Field.ZONE, NARROW - DELTA, 1);
        addData("X", Field.ZONE, SHORT - DELTA, 2);
        addData("X", Field.ZONE, LONG - DELTA, 4);
        addData("x", Field.ZONE, NARROW - DELTA, 1);
        addData("x", Field.ZONE, SHORT - DELTA, 2);
        addData("x", Field.ZONE, LONG - DELTA, 4);
    }

    //    int getDistance(DateTimeMatcher other, int includeMask, DistanceInfo distanceInfo) {
    //        int result = 0;
    //        distanceInfo.clear();
    //        for (Field i : Field.values()) {
    //            int myType = (includeMask & (1 << i)) == 0 ? 0 : type[i];
    //            int otherType = other.type[i];
    //            if (myType == otherType) continue; // identical (maybe both zero) add 0
    //            if (myType == 0) { // and other is not
    //                result += EXTRA_FIELD;
    //                distanceInfo.addExtra(i);
    //            } else if (otherType == 0) { // and mine is not
    //                result += MISSING_FIELD;
    //                distanceInfo.addMissing(i);
    //            } else {
    //                result += Math.abs(myType - otherType); // square of mismatch
    //            }
    //        }
    //        return result;
    //    }

    private static void addData(String c, Field field, int weight, int minLength) {
        addData(c, field, weight, minLength, minLength);
    }

    // character, main type, weight, min length, weight
    private static void addData(String c, Field field, int weight, int minLength, int maxLength) {
        String min = c.repeat(minLength);
        String max = c.repeat(Math.min(12, maxLength));
        System.out.println(
                Joiner.on('\t').skipNulls().join(field, weight, min, min.equals(max) ? null : max));
    }

    public static void main(String[] args) {}
}
