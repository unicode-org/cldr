package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.unicode.cldr.test.CompactDecimalFormat.Style;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

public class BuildIcuCompactDecimalFormat {
    static SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();

    /** JUST FOR DEVELOPMENT */
    public static final CompactDecimalFormat build(CLDRFile resolvedCldrFile, Set<String> debugCreationErrors, String[] debugOriginals, Style style, ULocale locale) {
        Map<String, String[]> prefixes = new HashMap();
        Map<String, String[]> suffixes = new HashMap();
        //String[] prefix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
        //String[] suffix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
        long[] divisor = new long[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
        // get the pattern details from the locale
        PluralInfo pluralInfo = sdi.getPlurals(locale.toString());

        // fix low numbers
        for (String key : pluralInfo.getCanonicalKeywords()) {
            String[] prefix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
            String[] suffix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
            for (int i = 0; i < 3; ++i) {
                prefix[i] = suffix[i] = "";
                divisor[i] = 1;
            }
            prefixes.put(key, prefix);
            suffixes.put(key, suffix);
        }
        Matcher patternMatcher = CompactDecimalFormatTest.PATTERN.matcher("");
        Matcher typeMatcher = CompactDecimalFormatTest.TYPE.matcher("");
        XPathParts parts = new XPathParts();
        //for (String path : With.in(resolvedCldrFile.iterator("//ldml/numbers/decimalFormats/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/"))) {
        Iterator<String> it = resolvedCldrFile.iterator("//ldml/numbers/decimalFormats/decimalFormatLength");

        String styleString = style.toString().toLowerCase(Locale.ENGLISH);
        while (it.hasNext()) {
            String path = it.next();
            if (path.endsWith("/alias")) {
                continue;
            }
            //            if (!path.contains("decimalFormatLength")) {
            //                continue;
            //            }
            parts.set(path);
            String stype = parts.getAttributeValue(3, "type");
            if (!styleString.equals(stype)) {
                continue;
            }
            String type = parts.getAttributeValue(-1, "type");

            String key = parts.getAttributeValue(-1, "count");
            String[] prefix = prefixes.get(key);
            String[] suffix = suffixes.get(key);

            if (!typeMatcher.reset(type).matches()) {
                debugCreationErrors.add("type (" + type +
                    ") doesn't match expected " + CompactDecimalFormatTest.TYPE.pattern()); 
                continue;
            }

            String pattern = resolvedCldrFile.getStringValue(path);

            int typeZeroCount = typeMatcher.end(1) - typeMatcher.start(1);
            // for debugging
            if (debugOriginals != null) {
                debugOriginals[typeZeroCount] = pattern;
            }

            // special pattern for unused
            if (pattern.equals("0")) {
                prefix[typeZeroCount] = suffix[typeZeroCount] = "";
                divisor[typeZeroCount] = 1;
                continue;
            }

            if (!patternMatcher.reset(pattern).matches()) {
                debugCreationErrors.add("pattern (" + pattern +
                    ") doesn't match expected " + CompactDecimalFormatTest.PATTERN.pattern()); 
                continue;
            }

            // HACK '.' for now!!
            prefix[typeZeroCount] = patternMatcher.group(1).replace("'.'", ".");
            suffix[typeZeroCount] = patternMatcher.group(4).replace("'.'", ".");
            int zeroCount = patternMatcher.end(2) - patternMatcher.start(2) - 1;
            divisor[typeZeroCount] = (long) Math.pow(10.0, typeZeroCount - zeroCount);
        }
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(new ULocale(resolvedCldrFile.getLocaleID()));

        //DecimalFormat currencyFormat = new ICUServiceBuilder().setCldrFile(resolvedCldrFile).getCurrencyFormat("USD");
        //        for (String s : With.in(resolvedCldrFile.iterator("//ldml/numbers/currencyFormats/"))) {
        //            System.out.println(s + "\t" + resolvedCldrFile.getStringValue(s));
        //        }
        //DecimalFormat currencyFormat = new DecimalFormat(pattern);
        // do this by hand, because the DecimalFormat pattern parser does too much.
        String pattern = resolvedCldrFile.getWinningValue("//ldml/numbers/currencyFormats/currencyFormatLength/currencyFormat[@type=\"standard\"]/pattern[@type=\"standard\"]");
        String[] currencyAffixes = new String[CompactDecimalFormat.AFFIX_SIZE];
        String[] patterns = pattern.split(";");
        final Matcher matcher = CompactDecimalFormatTest.CURRENCY_PATTERN.matcher(patterns[0]);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Can't match currency pattern");
        }
        currencyAffixes[CompactDecimalFormat.POSITIVE_PREFIX] = matcher.group(1);
        currencyAffixes[CompactDecimalFormat.POSITIVE_SUFFIX] = matcher.group(2);

        if (false) {
            for (Entry<String, String[]> keyList : suffixes.entrySet()) {
                System.out.println("*\t" + keyList.getKey() + "\t" + Arrays.asList(keyList.getValue()));
            }
        }

        // TODO fix to get right symbol for the count
        return new CompactDecimalFormat(format.toPattern(), format.getDecimalFormatSymbols(), prefixes, suffixes, divisor, debugCreationErrors, style, 
            currencyAffixes, new CompactDecimalFormatTest.MyCurrencySymbolDisplay(resolvedCldrFile), pluralInfo.getPluralRules());
    }

}
