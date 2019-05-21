package org.unicode.cldr.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormat.PropertySetter;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

@SuppressWarnings("deprecation")
public class BuildIcuCompactDecimalFormat {
    static SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    static final int MINIMUM_ARRAY_LENGTH = 15;
    static final Pattern PATTERN = PatternCache.get("([^0,]*)([0]+)([.]0+)?([^0]*)");
    static final Pattern TYPE = PatternCache.get("1([0]*)");

    public enum CurrencyStyle {
        PLAIN, CURRENCY, LONG_CURRENCY, ISO_CURRENCY, UNIT
    }

    /**
     * JUST FOR DEVELOPMENT
     *
     * @param currencyStyle
     * @param currencyCode
     */
    public static final CompactDecimalFormat build(CLDRFile resolvedCldrFile,
        Set<String> debugCreationErrors, String[] debugOriginals,
        CompactStyle style, ULocale locale, CurrencyStyle currencyStyle, String currencyCodeOrUnit) {

        final Map<String, Map<String, String>> customData = new HashMap<String, Map<String, String>>();

        String prefix = currencyStyle == CurrencyStyle.PLAIN ? "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength"
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
            String pattern = resolvedCldrFile.getStringValue(path);

            /*
                    <pattern type="1000" count="one">0K</pattern>
             */

            add(customData, type, key, pattern);
        }
        CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(locale, style);
        ICUServiceBuilder builder = new ICUServiceBuilder().setCldrFile(resolvedCldrFile);

        cdf.setDecimalFormatSymbols(builder.getDecimalFormatSymbols("latn"));
        cdf.setProperties(new PropertySetter() {
            @Override
            public void set(DecimalFormatProperties props) {
                props.setCompactCustomData(customData);
            }
        });
        return cdf;
    }

    private static <A, B, C> void add(Map<A, Map<B, C>> customData, A a, B b, C c) {
        Map<B, C> inner = customData.get(a);
        if (inner == null) {
            customData.put(a, inner = new HashMap<>());
        }
        inner.put(b, c);
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
    };

}
