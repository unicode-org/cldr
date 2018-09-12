package org.unicode.cldr.util;

import java.util.Locale;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class XChoiceFormat {
    /**
     * Enumeration representing different types of plurals used in various
     * languages. More may be added over time. The names are chosen to be short.
     */
    public enum Condition {
        /** nullar: used for zero items */
        N0,
        /** singular: used for 1 item */
        N1,
        /** dual: used for 2 items */
        N2,
        /** trial: used for 2 items */
        N3,
        /** paucal: used for 2-4 items */
        N234,
        /** slavic singular: used for 1, 21, 31, ... items */
        S1,
        /** slavic dual: used for 2, 22, 32, ... items */
        S2,
        /** slavic paucal: used for 2, 3, 4, 22, 23, ... items */
        S234,
        /** other: used for anything not mentioned above, such as plurals */
        N;

        public boolean matches(long num) {
            switch (this) {
            case N0:
                return num == 0;
            case N1:
                return num == 1;
            case N2:
                return num == 2;
            case N3:
                return num == 3;
            case N234:
                return num >= 3 && num <= 4;
            case S1:
            case S2:
            case S234:
                long digit = num % 10;
                if (((num / 10) % 10) == 1) {
                    return false;
                }
                switch (this) {
                case S1: {
                    return digit == 1;
                }
                case S2: {
                    return digit == 2;
                }
                default: {
                    return digit >= 2 && digit <= 4;
                }
                }
            default:
                return true; // CN
            }
        }
    };

    private Condition[] conditions;

    private Object[] results;

    ULocale locale = ULocale.getDefault();

    /**
     * "There {countOfBooks,C0:are no files|C1:is one file|Cn:are #0 files} in
     * {directory}."
     *
     * @param pattern
     */
    public void applyPattern(String pattern) {
        String[] items = pattern.split("[|]");
        conditions = new Condition[items.length];
        results = new Object[items.length];
        // conditions = new int
        int i = 0;
        for (String item : items) {
            int semiPosition = item.indexOf(':');
            if (semiPosition < 0) {
                throw new IllegalArgumentException("syntax error: no semicolon");
            }
            String tag = item.substring(0, semiPosition);
            conditions[i] = Condition.valueOf(tag.toUpperCase(Locale.ENGLISH));
            String numberPattern = item.substring(semiPosition + 1);
            results[i] = numberPattern.contains("#")
                ? new DecimalFormat(numberPattern, new DecimalFormatSymbols(locale))
                : numberPattern;
            ++i;
        }
        if (conditions[i - 1] != Condition.N) {
            throw new IllegalArgumentException("final condition must be CN");
        }
    }

    public String format(long num) {
        for (int i = 0; i < conditions.length; ++i) {
            if (conditions[i].matches(num)) {
                if (results[i] instanceof String) {
                    return (String) results[i];
                } else {
                    return ((NumberFormat) results[i]).format(num);
                }
            }
        }
        throw new IllegalArgumentException("final condition must be CN");
    }

    public static void main(String[] args) {
        Object[] tests = {
            new ULocale("en"),
            "There {N0:are no files|N1:is 1 file|N:are #,### files} in the directory.",
            new ULocale("fr"),
            "There {N0:is aucun file|N1:is 1 file|N:are #,### files} in the directory.",
            new ULocale("sl"),
            "There {N0:are no mest|S1:is #,### mesto|S2:are #,### mesti|S234:are #,### mesta|N:are #,### mest} in the directory.",
            new ULocale("ru"),
            "There {N0:are no zavody|S1:is #,### zavod|S234:are #,### zavoda|N:are #,### zavodov} in the directory.", };

        int[] testNumbers = { 0, 1, 2, 3, 5, 10, 11, 12, 15, 20, 21, 22, 25, 123450, 123451, 123452, 123456 };
        XChoiceFormat foo = new XChoiceFormat();
        for (Object test : tests) {
            if (test instanceof ULocale) {
                ULocale locale = (ULocale) test;
                System.out.println("Language:\t" + locale.getDisplayName());
                foo.setLocale(locale);
                continue;
            }
            System.out.println("Test pattern:\t\"" + test + "\"");
            String[] parts = ((String) test).split("[{}]");
            foo.applyPattern(parts[1]);
            for (int i : testNumbers) {
                System.out.println("\tSample:\t" + parts[0] + foo.format(i) + parts[2]);
            }
        }
    }

    /*
     * zeroHours oneHour twoHours threeToFourItems oneItemRussian
     * twoToFourItemsRussian otherHours
     *
     * Do the same for zeroMinutes, zeroSeconds, etc.
     *
     * For a given locale, for a given field type (hour, minute, second), the code
     * will fetch each of the corresponding strings, and do the following
     * pseudocode, given a number x. // special checks for certain slavic
     * languages int digit = x % 10; int tens = (x / 10) % 10; if (oneItemRussian !=
     * null && tens != 1 && digit == 1) return oneItemRussian; if
     * (twoToFourItemsRussian != null && tens != 1 && 2 <= digit && digit <= 4)
     * return twoToFourItemsRussian; // above might need fixing, this is off the
     * top of my head // more regular cases if (zeroItems != null && x == 0)
     * return zeroItems; if (oneItem != null && x == 1) return oneItem; if
     * (twoItems != null && x == 2) return twoItems; if (threeToFourItems != null &&
     * x == 2) return threeToFourItems; return otherItems; // must always be
     * populated // above can be optimized, again off the top of my head
     */

    private void setLocale(ULocale locale2) {
        locale = locale2;
    }
}
