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
    private static boolean DEBUG = false;
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
//        Map<String,String> inner = new HashMap<String,String>();
//        inner.put("one", "0 qwerty");
//        inner.put("other", "0 dvorak");
//        customData.put("1000", inner);

//        Map<String, String[][]> affixes = new HashMap<String, String[][]>();
//        Map<String, String[]> unitPrefixes = new HashMap<String, String[]>();
//
//        // String[] prefix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
//        // String[] suffix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
//        long[] divisor = new long[MINIMUM_ARRAY_LENGTH];
//        // get the pattern details from the locale
//        PluralInfo pluralInfo = sdi.getPlurals(PluralType.cardinal, locale.toString());
//
//        // fix low numbers
//        Set<String> canonicalKeywords = pluralInfo.getCanonicalKeywords();
//        for (String key : canonicalKeywords) {
//            String[][] affix = new String[MINIMUM_ARRAY_LENGTH][];
//            for (int i = 0; i < 3; ++i) {
//                affix[i] = new String[] { "", "" };
//                divisor[i] = 1;
//            }
//            affixes.put(key, affix);
//        }
//        Matcher patternMatcher = PATTERN.matcher("");
//        Matcher typeMatcher = TYPE.matcher("");
//        XPathParts parts = new XPathParts();
        // for (String path :
        // With.in(resolvedCldrFile.iterator("//ldml/numbers/decimalFormats/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/")))
        // {
        String prefix = currencyStyle == CurrencyStyle.PLAIN ? "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength"
            : "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength";

        Iterator<String> it = resolvedCldrFile.iterator(prefix);

        String styleString = style.toString().toLowerCase(Locale.ENGLISH);
        while (it.hasNext()) {
            String path = it.next();
            if (path.endsWith("/alias")) {
                continue;
            }
            // String sourceLocale = resolvedCldrFile.getSourceLocaleID(path, null);
            // if ("root".equals(sourceLocale)) {
            // continue;
            // }
            // if (!path.contains("decimalFormatLength")) {
            // continue;
            // }
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

//            if (DEBUG && path.contains("miliony")) {
//                System.out.println(key + ", " + path);
//            }
//            String[][] affix = affixes.get(key);
//
//            if (!typeMatcher.reset(type).matches()) {
//                debugCreationErrors.add("type (" + type +
//                    ") doesn't match expected " + TYPE.pattern());
//                continue;
//            }
//
//            String pattern = resolvedCldrFile.getStringValue(path);
//
//            int typeZeroCount = typeMatcher.end(1) - typeMatcher.start(1);
//            // for debugging
//            if (debugOriginals != null) {
//                debugOriginals[typeZeroCount] = pattern;
//            }
//
//            // special pattern for unused
//            if (pattern.equals("0")) {
//                affix[typeZeroCount] = new String[] { "", "" };
//                divisor[typeZeroCount] = 1;
//                continue;
//            }
//
//            if (!patternMatcher.reset(pattern).matches()) {
//                debugCreationErrors.add("pattern (" + pattern +
//                    ") doesn't match expected " + PATTERN.pattern());
//                continue;
//            }
//
//            // HACK '.' just in case.
//            affix[typeZeroCount] = new String[] { 
//                escape(patternMatcher.group(1).replace("'.'", ".")),
//                escape(patternMatcher.group(4).replace("'.'", "."))
//
////                patternMatcher.group(1).replace("'.'", "."),
////                patternMatcher.group(4).replace("'.'", ".")
//                };
//            if (DEBUG && key.equals("one")) {
//                System.out.println(key + ", " + typeZeroCount + ", " + Arrays.asList(affix[typeZeroCount]));
//            }
//            int zeroCount = patternMatcher.end(2) - patternMatcher.start(2) - 1;
//            divisor[typeZeroCount] = (long) Math.pow(10.0, typeZeroCount - zeroCount);
        }

        // DecimalFormat format = (DecimalFormat) (currencyStyle == CurrencyStyle.PLAIN
        // ? NumberFormat.getInstance(new ULocale(resolvedCldrFile.getLocaleID()))
        // : NumberFormat.getCurrencyInstance(new ULocale(resolvedCldrFile.getLocaleID())));

//        ICUServiceBuilder builder = new ICUServiceBuilder().setCldrFile(resolvedCldrFile);
////        final DecimalFormat format = builder.getNumberFormat(1);
//        switch (currencyStyle) {
//        case PLAIN:
//        default:
//            break;
//        case LONG_CURRENCY:
//            // if the long form, modify the patterns
//            CurrencyInfo names = new CurrencyInfo(resolvedCldrFile, canonicalKeywords,
//                currencyCodeOrUnit, parts);
//            if (!names.isEmpty()) {
//                for (String count : canonicalKeywords) {
//                    String unitPattern = names.getUnitPattern(count);
//                    int pos = unitPattern.indexOf("{0}");
//                    String prefixUnit = unitPattern.substring(0, pos);
//                    String suffixUnit = unitPattern.substring(pos + 3);
//                    String currencyName = names.getCurrencyName(count);
//                    addPrefixSuffixInfo(unitPrefixes, count, 
//                        MessageFormat.format(prefixUnit, null, currencyName),
//                        MessageFormat.format(suffixUnit, null, currencyName));
//                }
//                break;
//            }
//            // otherwise fallthru
//        case CURRENCY:
//            DecimalFormat format2 = builder.getCurrencyFormat(currencyCodeOrUnit);
//            String prefix1 = format2.getPositivePrefix();
//            String suffix2 = format2.getPositiveSuffix();
//            for (String count : canonicalKeywords) {
//                addPrefixSuffixInfo(unitPrefixes, count, prefix1, suffix2);
//            }
//            break;
//        case ISO_CURRENCY:
//            throw new IllegalArgumentException();
//        case UNIT:
//            String unit = currencyCodeOrUnit == null ? "mass-kilogram" : currencyCodeOrUnit;
//            String otherValue = getUnitString(resolvedCldrFile, unit, "other");
//            for (String count : canonicalKeywords) {
//                String value = getUnitString(resolvedCldrFile, unit, count);
//                if (value == null) {
//                    value = otherValue;
//                }
//                int pos = value.indexOf("{0}");
//                addPrefixSuffixInfo(unitPrefixes, count, value.substring(0, pos), value.substring(pos + 3));
//            }
//            break;
//        }

        // DecimalFormat currencyFormat = new
        // ICUServiceBuilder().setCldrFile(resolvedCldrFile).getCurrencyFormat("USD");
        // for (String s : With.in(resolvedCldrFile.iterator("//ldml/numbers/currencyFormats/"))) {
        // System.out.println(s + "\t" + resolvedCldrFile.getStringValue(s));
        // }
        // DecimalFormat currencyFormat = new DecimalFormat(pattern);
        // do this by hand, because the DecimalFormat pattern parser does too much.
        // String pattern =
        // resolvedCldrFile.getWinningValue("//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\""
        // +
        // "standard" +
        // "\"]");
        // String[] currencyAffixes = new String[CompactDecimalFormat.AFFIX_SIZE];
        // String[] patterns = pattern.split(";");
        // final Matcher matcher = CURRENCY_PATTERN.matcher(patterns[0]);
        // if (!matcher.matches()) {
        // throw new IllegalArgumentException("Can't match currency pattern");
        // }
        // currencyAffixes[CompactDecimalFormat.POSITIVE_PREFIX] = matcher.group(1);
        // currencyAffixes[CompactDecimalFormat.POSITIVE_SUFFIX] = matcher.group(2);
        //
//        if (DEBUG) {
//            for (Entry<String, String[][]> keyList : affixes.entrySet()) {
//                System.out.println("*\t" + keyList.getKey() + "\t" + CldrUtility.toString(keyList));
//            }
//        }

        // TODO fix to get right symbol for the count
        // String pattern = format.toPattern();

        // if (style == Style.LONG) {
        // pattern = pattern.replace("¤", "¤¤¤");
        // }

        // JCE 2017-03-28 - This constructor was removed in ICU 59.  Shane is working on a
        // workaround, but until one is done, we can't use it in its current state.
        // TODO: Put it back once the fix is in place, See Ticket #10166
        //        try {
//            return new CompactDecimalFormat(
//                pattern, format.getDecimalFormatSymbols(),
//                style, pluralInfo.getPluralRules(),
//                divisor, affixes, unitPrefixes,
//                debugCreationErrors);
//        } catch (Exception e) {
//            debugCreationErrors.add(e.getMessage());
//            return null;

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

//        debugCreationErrors.add("Can't create due to lack of 'from scratch' constructor for CompactDecimalFormat");
//        return null;
//        }
        /*
         *                 divisor, prefixes, suffixes,
            unitPrefixes, unitSuffixes,
            currencyAffixes, new CompactDecimalFormatTest.MyCurrencySymbolDisplay(resolvedCldrFile),
            debugCreationErrors
        
         */
        //        CompactDecimalFormat cdf = new CompactDecimalFormat(
        //                "#,###.00",
        //                DecimalFormatSymbols.getInstance(new ULocale("fr")),
        //                CompactStyle.SHORT, PluralRules.createRules("one: j is 1 or f is 1"),
        //                divisors, affixes, null,
        //                debugCreationErrors
        //                );

    }

    private static <A, B, C> void add(Map<A, Map<B, C>> customData, A a, B b, C c) {
        Map<B, C> inner = customData.get(a);
        if (inner == null) {
            customData.put(a, inner = new HashMap<>());
        }
        inner.put(b, c);
    }

    private static String[] addPrefixSuffixInfo(Map<String, String[]> unitPrefixes, String count,
        final String prefix, final String suffix) {
        return unitPrefixes.put(count, new String[] { escape(prefix), escape(suffix) });
    }

    private static String escape(String prefix) {
        return prefix.isEmpty() ? prefix : "'" + prefix.replace("'", "''") + "'";
    }

    private static String getUnitString(CLDRFile resolvedCldrFile, String unit, String count) {
        return resolvedCldrFile.getStringValue("//ldml/units/unitLength[@type=\"short\"]/unit[@type=\""
            + unit + "\"]/unitPattern[@count=\"" +
            count +
            "\"]");
    }

    private static class CurrencyInfo {
        private HashMap<String, String> currencyNames = new HashMap<String, String>();
        private HashMap<String, String> unitPatterns = new HashMap<String, String>();

        CurrencyInfo(CLDRFile resolvedCldrFile, Set<String> canonicalKeywords, String currencyCode, XPathParts parts) {
            Iterator<String> it;
            it = resolvedCldrFile.iterator(
                "//ldml/numbers/currencies/currency[@type=\"" +
                    currencyCode +
                    "\"]/displayName");
            // //ldml/numbers/currencies/currency[@type="SRD"]/symbol
            while (it.hasNext()) {
                String path = it.next();
                parts.set(path);
                String key = parts.getAttributeValue(-1, "count");
                if (key == null) {
                    key = "default";
                }
                currencyNames.put(key, resolvedCldrFile.getStringValue(path));
            }

            it = resolvedCldrFile.iterator(
                "//ldml/numbers/currencyFormats/unitPattern");
            while (it.hasNext()) {
                String path = it.next();
                parts.set(path);
                String key = parts.getAttributeValue(-1, "count");
                unitPatterns.put(key, resolvedCldrFile.getStringValue(path));
            }
            // <displayName count="one" draft="contributed">evro</displayName>
            // flesh out missing
        }

        public boolean isEmpty() {
            return currencyNames.isEmpty();
        }

        String getUnitPattern(String count) {
            String result = unitPatterns.get(count);
            if (result == null) {
                result = unitPatterns.get("other");
            }
            return result;
        }

        String getCurrencyName(String count) {
            String result = currencyNames.get(count);
            if (result != null) {
                return result;
            }
            if (!count.equals("other")) {
                result = currencyNames.get("other");
                if (result != null) {
                    return result;
                }
            }
            result = currencyNames.get("default");
            return result;
        }
    }

    // <currencyFormats numberSystem="latn">
    // <currencyFormatLength>
    // <currencyFormat>
    // <pattern>¤#,##0.00;(¤#,##0.00)</pattern>
    // </currencyFormat>
    // </currencyFormatLength>
    // <unitPattern count="one">{0} {1}</unitPattern>
    // <unitPattern count="other">{0} {1}</unitPattern>
    // </currencyFormats>

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
