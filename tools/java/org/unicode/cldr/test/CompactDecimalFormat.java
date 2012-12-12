package org.unicode.cldr.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

/**
 * The CompactDecimalFormat produces abbreviated numbers, suitable for display in environments will limited real estate.
 * For example, 'Hits: 1.2B' instead of 'Hits: 1,200,000,000'.
 * The format will be appropriate for the given language, such as "1,2 Mrd." for German.
 * <p>
 * For numbers under 1000 trillion (under 10^15, such as 123,456,789,012,345), the result will be short for supported
 * languages. However, the result may sometimes exceed 7 characters, such as when there are combining marks or thin
 * characters. In such cases, the visual width in fonts should still be short.
 * <p>
 * By default, there are 2 significant digits. After creation, if more than three significant digits are set (with
 * setMaximumSignificantDigits), or if a fixed number of digits are set (with setMaximumIntegerDigits or
 * setMaximumFractionDigits), then result may be wider.
 * <p>
 * At this time, negative numbers and parsing are not supported, and will produce an UnsupportedOperationException.
 * Resetting the pattern prefixes or suffixes is not supported; the method calls are ignored.
 * <p>
 * Note that important methods, like setting the number of decimals, will be moved up from DecimalFormat to
 * NumberFormat.
 * 
 * @author markdavis
 */
public class CompactDecimalFormat extends DecimalFormat {

    public interface CurrencySymbolDisplay {
        String getName(Currency currency, int count);
    }

    // TODO add serialization id
    /**
     * Style parameter for CompactDecimalFormat. Would actually be on NumberFormat.
     * 
     * @author markdavis
     * 
     */
    public enum Style {
        /**
         * Short version, like "1.2T"
         */
        SHORT,
        /**
         * Longer version, like "1.2 trillion", if available. May return same result as SHORT if not.
         */
        LONG
    }

    static final int MINIMUM_ARRAY_LENGTH = 15;
    final Map<String, String[]> prefixes;
    final Map<String, String[]> suffixes;
    final long[] divisor;
    static final int
        POSITIVE_PREFIX = 0,
        POSITIVE_SUFFIX = 1,
        AFFIX_SIZE = 2;
    // final String[] currencyAffixes;
    final CurrencySymbolDisplay currencySymbolDisplay;
    final PluralRules pluralRules;
    private Map<String, String> unitPrefixes;
    private Map<String, String> unitSuffixes;

    // /**
    // * Create a CompactDecimalFormat appropriate for a locale (Mockup for what would be in NumberFormat). The result
    // may be affected by the number system in the locale, such as ar-u-nu-latn.
    // * @param locale
    // */
    // public static final NumberFormat NumberFormat_getCompactDecimalInstance(ULocale locale, Style style) {
    // return new CompactDecimalFormat(locale, style);
    // }

    // /**
    // * Create a CompactDecimalFormat appropriate for a locale (Mockup for what would be in NumberFormat). The result
    // may be affected by the number system in the locale, such as ar-u-nu-latn.
    // * @param locale
    // */
    // public static final NumberFormat NumberFormat_getCompactDecimalInstance(Locale locale, Style style) {
    // return new CompactDecimalFormat(locale, style);
    // }

    // /**
    // * Create a CompactDecimalFormat appropriate for a locale. The result may be affected by the number system in the
    // locale, such as ar-u-nu-latn.
    // * @param locale
    // */
    // public CompactDecimalFormat(ULocale locale, Style style) {
    // DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(locale);
    // Data data;
    // while (true) {
    // data = localeToData.get(locale);
    // if (data != null) {
    // break;
    // }
    // locale = locale.equals(zhTW) ? ULocale.TRADITIONAL_CHINESE : locale.getFallback();
    // }
    // this.prefix = data.prefixes;
    // this.suffix = data.suffixes;
    // this.divisor = data.divisors;
    // applyPattern(format.toPattern());
    // setDecimalFormatSymbols(format.getDecimalFormatSymbols());
    // setMaximumSignificantDigits(2); // default significant digits
    // setSignificantDigitsUsed(true);
    // setGroupingUsed(false);
    //
    // DecimalFormat currencyFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
    // currencyAffixes = new String[AFFIX_SIZE];
    // currencyAffixes[CompactDecimalFormat.POSITIVE_PREFIX] = currencyFormat.getPositivePrefix();
    // currencyAffixes[CompactDecimalFormat.POSITIVE_SUFFIX] = currencyFormat.getPositiveSuffix();
    // // TODO fix to get right symbol for the count
    // currencySymbolDisplay = new IcuCurrencySymbolDisplay(locale);
    // }

    static class IcuCurrencySymbolDisplay implements CurrencySymbolDisplay {
        final ULocale locale;

        public IcuCurrencySymbolDisplay(ULocale locale) {
            this.locale = locale;
        }

        @Override
        public String getName(Currency currency, int count) {
            return count > 1
                ? currency.getCurrencyCode()
                : currency.getSymbol(locale);
        }
    };

    // private static ULocale zhTW = new ULocale("zh_TW");

    // /**
    // * Create a CompactDecimalFormat appropriate for a locale. The result may be affected by the number system in the
    // locale, such as ar-u-nu-latn.
    // * @param locale
    // */
    // public CompactDecimalFormat(Locale locale, Style style) {
    // this(ULocale.forLocale(locale), style);
    // }

    /**
     * Create a short number "from scratch". Intended for internal use.
     * The prefix, suffix, and divisor arrays are parallel, and provide the information for each power of 10.
     * When formatting a value, the correct power of 10 is found, then the value is divided by the divisor, and the
     * prefix and suffix are set (using setPositivePrefix/Suffix).
     * 
     * @param pattern
     *            A number format pattern. Note that the prefix and suffix are discarded, and the decimals are
     *            overridden by default.
     * @param formatSymbols
     *            Decimal format symbols, typically from a locale.
     * @param prefix
     *            An array of prefix values, one for each power of 10 from 0 to 14
     * @param suffix
     *            An array of prefix values, one for each power of 10 from 0 to 14
     * @param divisor
     *            An array of prefix values, one for each power of 10 from 0 to 14
     * @param unitSuffixes
     * @param unitPrefixes
     * @param debugCreationErrors
     *            A collection of strings for debugging.
     *            If null on input, then any errors found will be added to that collection instead of throwing
     *            exceptions.
     * @internal
     */
    public CompactDecimalFormat(String pattern, DecimalFormatSymbols formatSymbols,
        Map<String, String[]> prefixesInput, Map<String, String[]> suffixesInput, long[] divisor,
        Map<String, String> unitPrefixes, Map<String, String> unitSuffixes,
        Collection<String> debugCreationErrors, Style style,
        String[] currencyAffixes, CurrencySymbolDisplay currencySymbolDisplay, PluralRules pluralRules) {

        super(pattern, formatSymbols);
        this.pluralRules = pluralRules;
        this.unitPrefixes = unitPrefixes;
        this.unitSuffixes = unitSuffixes;

        for (String key : prefixesInput.keySet()) {
            String[] prefix = prefixesInput.get(key);
            String[] suffix = prefixesInput.get(key);
            if (prefix.length < MINIMUM_ARRAY_LENGTH) {
                recordError(debugCreationErrors, key, "Must have at least " + MINIMUM_ARRAY_LENGTH + " prefix items.");
            }
            if (prefix.length != suffix.length || prefix.length != divisor.length) {
                recordError(debugCreationErrors, key, "Prefix, suffix, and divisor arrays must have the same length.");
            }
            long oldDivisor = 0;
            Map<String, Integer> seen = new HashMap<String, Integer>();
            for (int i = 0; i < prefix.length; ++i) {
                boolean nullValues = prefix[i] == null || suffix[i] == null;
                if (nullValues) {
                    recordError(debugCreationErrors, key, "Prefix or suffix is null for " + i);
                }

                // divisor must be a power of 10, and must be less than or equal to 10^i
                int log = (int) Math.log10(divisor[i]);
                if (log > i) {
                    recordError(debugCreationErrors, key, "Divisor[" + i + "] must be less than or equal to 10^" + i
                        + ", but is: " + divisor[i]);
                }
                long roundTrip = (long) Math.pow(10.0d, log);
                if (roundTrip != divisor[i]) {
                    recordError(debugCreationErrors, key, "Divisor[" + i + "] must be a power of 10, but is: "
                        + divisor[i]);
                }

                // we can't have two different indexes with the same display, except for 0.
                if (!nullValues && !prefix[i].isEmpty() && !suffix[i].isEmpty()) {
                    String key2 = prefix[i] + "\uFFFF" + suffix[i] + "\uFFFF" + (i - log);
                    Integer old = seen.get(key2);
                    if (old != null) {
                        recordError(debugCreationErrors, key, "Collision between values for " + i + " and " + old
                            + " for [prefix/suffix/index-log(divisor)" + key2.replace('\uFFFF', ';'));
                    } else {
                        seen.put(key2, i);
                    }
                }
                if (divisor[i] < oldDivisor) {
                    recordError(debugCreationErrors, key, "Bad divisor, the divisor for 10E" + i + "(" + divisor[i] +
                        ") is less than the divisor for the divisor for 10E" + (i - 1) + "(" + oldDivisor +
                        ")");
                }
                oldDivisor = divisor[i];
            }
        }

        // HACK; should clone
        this.prefixes = prefixesInput;
        this.suffixes = suffixesInput;
        this.divisor = divisor.clone();
        // applyPattern(pattern);
        // setDecimalFormatSymbols(formatSymbols);
        setMaximumSignificantDigits(2); // default significant digits
        setSignificantDigitsUsed(true);
        setGroupingUsed(false);
        // this.currencyAffixes = currencyAffixes.clone();
        this.currencySymbolDisplay = currencySymbolDisplay;
    }

    @Override
    public StringBuffer format(double numberInput, StringBuffer toAppendTo, FieldPosition pos) {
        double number;
        if (numberInput < 0.0d) {
            throw new UnsupportedOperationException("CompactDecimalFormat doesn't handle negative numbers yet.");
        }

        int integerCount = Integer.MIN_VALUE;
        int base = Integer.MIN_VALUE;
        int check = Integer.MIN_VALUE;
        try {
            integerCount = numberInput <= 0.0d ? 0 : (int) Math.log10(numberInput);
            base = integerCount > 14 ? 14 : integerCount;
            number = numberInput / divisor[base];
            String key = pluralRules.select(number);
            String prefixString = prefixes.get(key)[base];
            String suffixString = suffixes.get(key)[base];
            check = prefixString.length() + suffixString.length();
            if (!unitSuffixes.isEmpty()) {
                String key2 = pluralRules.select(numberInput * divisor[base]);
                toAppendTo.append(unitPrefixes.get(key2));
                suffixString += unitSuffixes.get(key2);
            }
            toAppendTo.append(prefixString);

            super.format(number, toAppendTo, pos);

            // decide where to put suffixString
            StringBuffer temp = new StringBuffer();
            FieldPosition pos2 = new FieldPosition(Field.FRACTION);
            super.format(number, temp, pos2);
            int fractionEndIndex = pos2.getEndIndex();
            int offsetFromEnd = temp.length() - fractionEndIndex;
            if (offsetFromEnd == 0) {
                toAppendTo.append(suffixString);
            } else {
                toAppendTo.insert(toAppendTo.length() - offsetFromEnd, suffixString);
            }

            return toAppendTo;
        } catch (Exception e) {
            toAppendTo.append("ERROR:");
            return super.format(numberInput, toAppendTo, pos);
        }
    }

    // @Override
    // public StringBuffer format(CurrencyAmount currencyAmount, StringBuffer toAppendTo, FieldPosition pos) {
    // double number = currencyAmount.getNumber().doubleValue();
    // if (number < 0.0d) {
    // throw new UnsupportedOperationException("CompactDecimalFormat doesn't handle negative numbers yet.");
    // }
    // int integerCount = (int) Math.log10(number);
    // int base = integerCount > 14 ? 14 : integerCount;
    // number = number / divisor[base];
    //
    // final Currency currency = currencyAmount.getCurrency();
    // // TODO sometimes the affixes depend on the currency, so this needs to be fixed for that
    // setPositivePrefix(replaceCurrencySymbol(currencyAffixes[POSITIVE_PREFIX], currency) + prefix[base]);
    // setPositiveSuffix(suffix[base] + replaceCurrencySymbol(currencyAffixes[POSITIVE_SUFFIX], currency));
    // setCurrency(currency);
    // setMaximumFractionDigits(0); // reset to no fractions
    // return super.format(number, toAppendTo, pos);
    // }

    /**
     * Duplicate what is in Decimal Format (but which is inaccessible). Doesn't handle quoted currency signs correctly.
     * 
     * @param string
     * @param currency
     * @return
     */
    private String replaceCurrencySymbol(String string, Currency currency) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (int i = 0; i < string.length(); ++i) {
            final char ch = string.charAt(i);
            if (ch == '\u00A4') {
                ++count;
            } else {
                if (count > 0) {
                    result.append(currencySymbolDisplay.getName(currency, count));
                    count = 0;
                }
                result.append(ch);
            }
        }
        if (count > 0) {
            result.append(currencySymbolDisplay.getName(currency, count));
        }
        return result.toString();
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return format((double) number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    @Override
    public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    @Override
    public StringBuffer format(com.ibm.icu.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return format(number.doubleValue(), toAppendTo, pos);
    }

    /**
     * Parsing is currently unsupported, and throws an UnsupportedOperationException.
     */
    @Override
    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    /* INTERNALS */

    private void recordError(Collection<String> creationErrors, String key, String errorMessage) {
        if (creationErrors == null) {
            throw new IllegalArgumentException(key + ":" + errorMessage);
        }
        creationErrors.add(key + ":" + errorMessage);
    }

    /** JUST FOR DEVELOPMENT */
    // For use with the hard-coded data
    static class Data {
        public Data(long[] divisors, String[] prefixes, String[] suffixes) {
            this.divisors = divisors;
            this.prefixes = prefixes;
            this.suffixes = suffixes;
        }

        long[] divisors;
        String[] prefixes;
        String[] suffixes;
    }

    static Map<ULocale, Data> localeToData = new HashMap<ULocale, Data>();

    static void add(String locale, long[] ls, String[] prefixes, String[] suffixes) {
        localeToData.put(new ULocale(locale), new Data(ls, prefixes, suffixes));
    }

    static {
        CompactDecimalFormatData.load();
    }

}
