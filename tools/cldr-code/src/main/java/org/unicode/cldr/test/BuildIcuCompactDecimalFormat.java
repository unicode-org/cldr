package org.unicode.cldr.test;

import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormat.PropertySetter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

@SuppressWarnings("deprecation")
public class BuildIcuCompactDecimalFormat {
    static SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    static final int MINIMUM_ARRAY_LENGTH = 15;
    static final Pattern PATTERN = PatternCache.get("([^0,]*)([0]+)([.]0+)?([^0]*)");
    static final Pattern TYPE = PatternCache.get("1([0]*)");
    private static final boolean DEBUG = false;

    public enum CurrencyStyle {
        PLAIN,
        CURRENCY,
        LONG_CURRENCY,
        ISO_CURRENCY,
        UNIT
    }

    /**
     * JUST FOR DEVELOPMENT
     *
     * @param currencyStyle
     * @param currencyCode
     */
    public static final CompactDecimalFormat build(
            CLDRFile resolvedCldrFile,
            Set<String> debugCreationErrors,
            String[] debugOriginals,
            CompactStyle style,
            ULocale locale,
            CurrencyStyle currencyStyle,
            String currencyCodeOrUnit) {

        // get the custom data from CLDR for use with the special setCompactCustomData.
        // buildCustomData() needs the currencySymbol to pick the right pattern.
        // And if we get it here, we can also pass it to ICUServiceBuilder.getCurrencyFormat().
        String currencySymbol = null;
        if (currencyStyle == CurrencyStyle.CURRENCY) {
            String currencySymbolPath =
                    "//ldml/numbers/currencies/currency[@type=\""
                            + currencyCodeOrUnit
                            + "\"]/symbol";
            currencySymbol = resolvedCldrFile.getWinningValueWithBailey(currencySymbolPath);
        }

        final Map<String, Map<String, String>> customData =
                buildCustomData(resolvedCldrFile, style, currencyStyle, currencySymbol);
        if (DEBUG) {
            System.out.println("\nCustom Data:");
            customData.forEach((k, v) -> System.out.println("\t" + k + "\t" + v));
        }

        // get the common CLDR data used for number/date/time formats

        ICUServiceBuilder builder = new ICUServiceBuilder().setCldrFile(resolvedCldrFile);

        DecimalFormat decimalFormat =
                currencyStyle == CurrencyStyle.PLAIN
                        ? builder.getNumberFormat(1)
                        : builder.getCurrencyFormat(currencyCodeOrUnit, currencySymbol);
        final String pattern = decimalFormat.toPattern();
        if (DEBUG) {
            System.out.println("Pattern:\t" + pattern);
        }

        final PluralRules rules =
                SupplementalDataInfo.getInstance().getPlurals(locale.toString()).getPluralRules();

        // create a compact decimal format, and reset its data

        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(locale, style);
        cdf.setDecimalFormatSymbols(builder.getDecimalFormatSymbols("latn"));

        cdf.setProperties(
                new PropertySetter() {
                    @Override
                    public void set(DecimalFormatProperties props) {
                        props.setCompactCustomData(customData);
                        PatternStringParser.parseToExistingProperties(
                                pattern, props, PatternStringParser.IGNORE_ROUNDING_ALWAYS);
                        props.setPluralRules(rules);
                    }
                });

        if (DEBUG) {
            System.out.println("CompactDecimalFormat:\t" + cdf.toString().replace("}, ", "},\n\t"));
        }
        return cdf;
    }

    // This interface (without currencySymbol) is used by test code
    public static Map<String, Map<String, String>> buildCustomData(
            CLDRFile resolvedCldrFile, CompactStyle style, CurrencyStyle currencyStyle) {
        return buildCustomData(resolvedCldrFile, style, currencyStyle, null);
    }

    public static Map<String, Map<String, String>> buildCustomData(
            CLDRFile resolvedCldrFile,
            CompactStyle style,
            CurrencyStyle currencyStyle,
            String currencySymbol) {

        final Map<String, Map<String, String>> customData = new TreeMap<>();

        boolean currSymbolAlphaLeading = false;
        boolean currSymbolAlphaTrailing = false;
        if (currencySymbol != null && currencySymbol.length() > 0) {
            currSymbolAlphaLeading = UCharacter.isLetter(currencySymbol.codePointAt(0));
            currSymbolAlphaTrailing =
                    (currencySymbol.length() > 1)
                            ? UCharacter.isLetter(
                                    currencySymbol.codePointBefore(currencySymbol.length()))
                            : currSymbolAlphaLeading;
        }

        String prefix =
                currencyStyle == CurrencyStyle.PLAIN
                        ? "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength"
                        : "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength";

        Iterator<String> it = resolvedCldrFile.iterator(prefix);

        String styleString = style.toString().toLowerCase(Locale.ENGLISH);
        while (it.hasNext()) {
            String path = it.next();
            if (path.endsWith("/alias")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String stype = parts.getAttributeValue(3, "type");
            if (!styleString.equals(stype)) {
                continue;
            }
            String type = parts.getAttributeValue(-1, "type");
            String key = parts.getAttributeValue(-1, "count");
            String alt = parts.getAttributeValue(-1, "alt"); // may be null
            String pattern = resolvedCldrFile.getStringValue(path);
            if (pattern.contentEquals("0")) {
                continue;
            }

            /*
                   <pattern type="1000" count="one">0K</pattern>
            */
            // if we have alt=alphaNextToNumber and we meet the conditions for using the alt value,
            // then add the alt pattern, replacing any previous  value for this type and count.
            // Otherwise skip the alt value.
            if (alt != null && alt.equals("alphaNextToNumber")) {
                int currPos = pattern.indexOf('¤');
                if (currPos < 0) {
                    continue;
                }
                if (currPos == 0 && !currSymbolAlphaTrailing) {
                    continue;
                } else if (currPos == pattern.length() - 1 && !currSymbolAlphaLeading) {
                    continue;
                } else if (!currSymbolAlphaLeading && !currSymbolAlphaTrailing) {
                    continue;
                }
                add(customData, type, key, pattern, true);
            }

            // Otherwise add the standard value if there is not yet a value for this type and count.
            add(customData, type, key, pattern, false);
        }
        return customData;
    }

    private static <A, B, C> void add(
            Map<A, Map<B, C>> customData, A a, B b, C c, boolean replace) {
        Map<B, C> inner = customData.get(a);
        if (inner == null) {
            customData.put(a, inner = new HashMap<>());
        }
        if (replace) {
            inner.put(b, c);
        } else {
            inner.putIfAbsent(b, c);
        }
    }

    static class MyCurrencySymbolDisplay {
        CLDRFile cldrFile;

        public MyCurrencySymbolDisplay(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
        }

        public String getName(Currency currency, int count) {
            final String currencyCode = currency.getCurrencyCode();
            if (count > 1) {
                return currencyCode;
            }
            String prefix = "//ldml/numbers/currencies/currency[@type=\"" + currencyCode + "\"]/";
            String currencySymbol = cldrFile.getWinningValue(prefix + "symbol");
            return currencySymbol != null ? currencySymbol : currencyCode;
        }
    }
}
