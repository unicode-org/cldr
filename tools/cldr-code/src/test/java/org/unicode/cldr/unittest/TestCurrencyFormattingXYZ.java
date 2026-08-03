package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.NumberFormatter.UnitWidth;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates the difference in fallback behavior between symbol (UnitWidth.SHORT) and full name
 * (UnitWidth.FULL_NAME) when formatting a currency without localized symbol/display names (e.g.
 * "XYZ") in the "en" locale.
 *
 * <p>Context: <a href="https://unicode-org.atlassian.net/browse/CLDR-19649">CLDR-19649</a>
 *
 * <ul>
 *   <li><b>Symbol fallback (UnitWidth.SHORT):</b> Uses the 3-letter currency code as the currency
 *       symbol in the standard currency pattern (prefix in English with non-breaking space): {@code
 *       "XYZ 1,234.57"}.
 *   <li><b>Full name fallback (UnitWidth.FULL_NAME):</b> Uses the 3-letter currency code as the
 *       unit display name in the unit/plural pattern: {@code "1,234.57 XYZ"}.
 * </ul>
 */
public class TestCurrencyFormattingXYZ {

    private static final ULocale LOCALE_EN = ULocale.ENGLISH;
    private static final String CURRENCY_CODE = "XYZ";
    private static final Currency CURRENCY_XYZ = Currency.getInstance(CURRENCY_CODE);
    private static final double TEST_VALUE = 1234.5678;

    public static void main(String[] args) {
        new TestCurrencyFormattingXYZ().testNumberFormatterUnitWidth();
    }

    @Test
    void testNumberFormatterUnitWidth() {
        LocalizedNumberFormatter baseFormatter =
                NumberFormatter.withLocale(LOCALE_EN).unit(CURRENCY_XYZ);

        // 1. Symbol fallback (UnitWidth.SHORT)
        String shortFormat = baseFormatter.unitWidth(UnitWidth.SHORT).format(TEST_VALUE).toString();

        // 2. Full name fallback (UnitWidth.FULL_NAME)
        String longFormat =
                baseFormatter.unitWidth(UnitWidth.FULL_NAME).format(TEST_VALUE).toString();

        // Optional variants for completeness
        String isoFormat =
                baseFormatter.unitWidth(UnitWidth.ISO_CODE).format(TEST_VALUE).toString();
        String narrowFormat =
                baseFormatter.unitWidth(UnitWidth.NARROW).format(TEST_VALUE).toString();
        String formalFormat =
                baseFormatter.unitWidth(UnitWidth.FORMAL).format(TEST_VALUE).toString();

        System.out.println("=== NumberFormatter Fallback: Symbol vs Full Name (CLDR-19649) ===");
        System.out.println("Locale:   " + LOCALE_EN);
        System.out.println("Currency: " + CURRENCY_CODE);
        System.out.println("Value:    " + TEST_VALUE);
        System.out.println("Symbol / Short (UnitWidth.SHORT):       \"" + shortFormat + "\"");
        System.out.println("Full Name / Long (UnitWidth.FULL_NAME): \"" + longFormat + "\"");
        System.out.println("ISO Code (UnitWidth.ISO_CODE):           \"" + isoFormat + "\"");
        System.out.println("Narrow Symbol (UnitWidth.NARROW):        \"" + narrowFormat + "\"");
        System.out.println("Formal Symbol (UnitWidth.FORMAL):        \"" + formalFormat + "\"");

        assertNotNull(shortFormat, "Short formatted output should not be null");
        assertNotNull(longFormat, "Long formatted output should not be null");

        // Assert exact formatted outputs showing fallback pattern difference:
        // - Symbol fallback puts symbol before number with non-breaking space:
        assertEquals("XYZ 1,234.57", shortFormat);
        // - Full name fallback puts currency name after number with standard space:
        assertEquals("1,234.57 XYZ", longFormat);
    }
}
