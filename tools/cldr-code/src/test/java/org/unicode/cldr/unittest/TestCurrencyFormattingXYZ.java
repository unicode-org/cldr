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
 * Demonstrates the behavior and differences between {@link UnitWidth#SHORT}, {@link
 * UnitWidth#ISO_CODE}, and {@link UnitWidth#FULL_NAME} when formatting a currency without localized
 * symbol/display names (e.g. "XYZ") in the "en" (English) locale.
 *
 * <p>Context: <a href="https://unicode-org.atlassian.net/browse/CLDR-19649">CLDR-19649</a>
 *
 * <ul>
 *   <li><b>Symbol fallback ({@link UnitWidth#SHORT}):</b> Uses the 3-letter currency code as the
 *       currency symbol in the standard currency pattern (prefix in English with non-breaking
 *       space): {@code "XYZ 1,234.57"}.
 *   <li><b>ISO code ({@link UnitWidth#ISO_CODE}):</b> Explicitly uses the 3-letter ISO code in the
 *       currency pattern: {@code "XYZ 1,234.57"}. For currencies without a custom symbol (like
 *       "XYZ"), this produces the exact same result as {@link UnitWidth#SHORT}.
 *   <li><b>Full name fallback ({@link UnitWidth#FULL_NAME}):</b> Uses the 3-letter currency code as
 *       the unit display name in the unit/plural pattern: {@code "1,234.57 XYZ"}.
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

        // 2. ISO code (UnitWidth.ISO_CODE)
        String isoFormat =
                baseFormatter.unitWidth(UnitWidth.ISO_CODE).format(TEST_VALUE).toString();

        // 3. Full name fallback (UnitWidth.FULL_NAME)
        String longFormat =
                baseFormatter.unitWidth(UnitWidth.FULL_NAME).format(TEST_VALUE).toString();

        // Optional variants for completeness
        String narrowFormat =
                baseFormatter.unitWidth(UnitWidth.NARROW).format(TEST_VALUE).toString();
        String formalFormat =
                baseFormatter.unitWidth(UnitWidth.FORMAL).format(TEST_VALUE).toString();

        System.out.println(
                "=== NumberFormatter Comparison: SHORT vs ISO_CODE vs FULL_NAME (CLDR-19649) ===");
        System.out.println("Locale:   " + LOCALE_EN);
        System.out.println("Currency: " + CURRENCY_CODE);
        System.out.println("Value:    " + TEST_VALUE);
        System.out.println("Symbol / Short (UnitWidth.SHORT):       \"" + shortFormat + "\"");
        System.out.println("ISO Code       (UnitWidth.ISO_CODE):     \"" + isoFormat + "\"");
        System.out.println("Full Name / Long (UnitWidth.FULL_NAME): \"" + longFormat + "\"");
        System.out.println("Narrow Symbol  (UnitWidth.NARROW):        \"" + narrowFormat + "\"");
        System.out.println("Formal Symbol  (UnitWidth.FORMAL):        \"" + formalFormat + "\"");

        assertNotNull(shortFormat, "Short formatted output should not be null");
        assertNotNull(isoFormat, "ISO formatted output should not be null");
        assertNotNull(longFormat, "Long formatted output should not be null");

        // Assert exact formatted outputs:
        // - SHORT (symbol fallback): uses ISO code as prefix with non-breaking space
        assertEquals("XYZ 1,234.57", shortFormat);
        // - ISO_CODE: explicitly uses ISO code with non-breaking space (matches SHORT fallback for
        // XYZ)
        assertEquals("XYZ 1,234.57", isoFormat);
        assertEquals(shortFormat, isoFormat, "For XYZ, SHORT fallback matches ISO_CODE formatting");
        // - FULL_NAME (long fallback): uses ISO code as unit name after number with standard space
        assertEquals("1,234.57 XYZ", longFormat);
    }
}
