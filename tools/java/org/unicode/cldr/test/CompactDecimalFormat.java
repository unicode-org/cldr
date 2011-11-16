package org.unicode.cldr.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

/**
 * The CompactDecimalFormat produces abbreviated numbers, suitable for display in environments will limited real estate.
 * For example, 'Hits: 1.2B' instead of 'Hits: 1,200,000,000'.
 * The format will be appropriate for the given language, such as "1,2 Mrd." for German.
 * <p>For numbers under 1000 trillion (under 10^15, such as 123,456,789,012,345), the result will be short for supported languages.
 * However, the result may sometimes exceed 7 characters, such as when there are combining marks or thin characters.
 * In such cases, the visual width in fonts should still be short.
 * <p>By default, there are 2 significant digits.
 * After creation, if more than three significant digits are set (with setMaximumSignificantDigits), or
 * if a fixed number of digits are set (with setMaximumIntegerDigits or setMaximumFractionDigits), then result may be wider.
 * <p>At this time, negative numbers and parsing are not supported, and will produce an UnsupportedOperationException. 
 * Resetting the pattern prefixes or suffixes is not supported; the method calls are ignored. 
 * <p>Note that important methods, like setting the number of decimals, will be moved up from DecimalFormat to NumberFormat.
 * @author markdavis
 */
public class CompactDecimalFormat extends DecimalFormat {
    // TODO add serialization id
    /**
     * Style parameter for CompactDecimalFormat. Would actually be on NumberFormat.
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
    private final String[] prefix;
    private final String[] suffix;
    private final long[] divisor;

    private static final Pattern PATTERN = Pattern.compile("([^0,]*)([0]+)([.]0+)?([^0]*)");
    private static final Pattern TYPE = Pattern.compile("1([0]*)");

    /**
     * Create a CompactDecimalFormat appropriate for a locale (Mockup for what would be in NumberFormat). The result may be affected by the number system in the locale, such as ar-u-nu-latn.
     * @param locale
     */
    public static final NumberFormat NumberFormat_getCompactDecimalInstance(ULocale locale, Style style) {
        return new CompactDecimalFormat(locale, style);
    }

    /**
     * Create a CompactDecimalFormat appropriate for a locale (Mockup for what would be in NumberFormat). The result may be affected by the number system in the locale, such as ar-u-nu-latn.
     * @param locale
     */
    public static final NumberFormat NumberFormat_getCompactDecimalInstance(Locale locale, Style style) {
        return new CompactDecimalFormat(locale, style);
    }

    /**
     * Create a CompactDecimalFormat appropriate for a locale. The result may be affected by the number system in the locale, such as ar-u-nu-latn.
     * @param locale
     */
    public CompactDecimalFormat(ULocale locale, Style style) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(locale);
        Data data;
        while (true) {
            data = localeToData.get(locale);
            if (data != null) {
                break;
            }
            locale = locale.getFallback();
        }
        this.prefix = data.prefixes;
        this.suffix = data.suffixes;
        this.divisor = data.divisors;
        applyPattern(format.toPattern());
        setDecimalFormatSymbols(format.getDecimalFormatSymbols());
        setMaximumSignificantDigits(2); // default significant digits
        setSignificantDigitsUsed(true);
    }

    /**
     * Create a CompactDecimalFormat appropriate for a locale. The result may be affected by the number system in the locale, such as ar-u-nu-latn.
     * @param locale
     */
    public CompactDecimalFormat(Locale locale, Style style) {
        this(ULocale.forLocale(locale), style);
    }

    /** Create a short number "from scratch". Intended for internal use.
     * The prefix, suffix, and divisor arrays are parallel, and provide the information for each power of 10.
     * When formatting a value, the correct power of 10 is found, then the value is divided by the divisor, and the prefix and suffix are set (using setPositivePrefix/Suffix).
     * 
     * @param pattern A number format pattern. Note that the prefix and suffix are discarded, and the decimals are overridden by default.
     * @param formatSymbols Decimal format symbols, typically from a locale.
     * @param prefix An array of prefix values, one for each power of 10 from 0 to 14
     * @param suffix An array of prefix values, one for each power of 10 from 0 to 14
     * @param divisor An array of prefix values, one for each power of 10 from 0 to 14
     * @param debugCreationErrors A collection of strings for debugging.
     * If null on input, then any errors found will be added to that collection instead of throwing exceptions.
     * @internal
     */
    public CompactDecimalFormat(String pattern, DecimalFormatSymbols formatSymbols, String[] prefix, String[] suffix, long[] divisor, Collection<String> debugCreationErrors, Style style) {
        if (prefix.length < MINIMUM_ARRAY_LENGTH) {
            recordError(debugCreationErrors, "Must have at least " + MINIMUM_ARRAY_LENGTH + " prefix items.");
        }
        if (prefix.length != suffix.length || prefix.length != divisor.length) {
            recordError(debugCreationErrors, "Prefix, suffix, and divisor arrays must have the same length.");
        }
        long oldDivisor = 0;
        Map<String, Integer> seen = new HashMap<String, Integer>();        
        for (int i = 0; i < prefix.length; ++i) {
            if (prefix[i] == null || suffix[i] == null) {
                recordError(debugCreationErrors, "Prefix or suffix is null for " + i); 
            }

            // divisor must be a power of 10, and must be less than or equal to 10^i
            int log = (int) Math.log10(divisor[i]);
            if (log > i) {
                recordError(debugCreationErrors, "Divisor[" + i + "] must be less than or equal to 10^" + i + ", but is: " + divisor[i]); 
            }
            long roundTrip = (long) Math.pow(10.0d, log);
            if (roundTrip != divisor[i]) {
                recordError(debugCreationErrors, "Divisor[" + i + "] must be a power of 10, but is: " + divisor[i]); 
            }

            // we can't have two different indexes with the same display
            String key = prefix[i] + "\uFFFF" + suffix[i] + "\uFFFF" + (i - log);
            Integer old = seen.get(key);
            if (old != null) {
                recordError(debugCreationErrors, "Collision between values for " + i + " and " + old + " for [prefix/suffix/index-log(divisor)" + key.replace('\uFFFF', ';')); 
            } else {
                seen.put(key, i);
            }
            if (divisor[i] < oldDivisor) {
                recordError(debugCreationErrors, "Bad divisor, the divisor for 10E" + i + "(" + divisor[i] +
                        ") is less than the divisor for the divisor for 10E" + (i-1) + "(" + oldDivisor + 
                ")"); 
            }
            oldDivisor = divisor[i];
        }

        this.prefix = prefix.clone();
        this.suffix = suffix.clone();
        this.divisor = divisor.clone();
        applyPattern(pattern);
        setDecimalFormatSymbols(formatSymbols);
        setMaximumSignificantDigits(2); // default significant digits
        setSignificantDigitsUsed(true);
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number < 0.0d) {
            throw new UnsupportedOperationException("CompactDecimalFormat doesn't handle negative numbers yet.");
        }
        int integerCount = (int) Math.log10(number);
        int base = integerCount > 14 ? 14 : integerCount;
        number = number / divisor[base];
        setPositivePrefix(prefix[base]);
        setPositiveSuffix(suffix[base]);
        return super.format(number, toAppendTo, pos);
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
    
    private void recordError(Collection<String> creationErrors, String errorMessage) {
        if (creationErrors == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        creationErrors.add(errorMessage);
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

    /** JUST FOR DEVELOPMENT */
    private static final CompactDecimalFormat build(CLDRFile resolvedCldrFile, Set<String> debugCreationErrors, String[] debugOriginals, Style style) {
        String[] prefix = new String[MINIMUM_ARRAY_LENGTH];
        String[] suffix = new String[MINIMUM_ARRAY_LENGTH];
        long[] divisor = new long[MINIMUM_ARRAY_LENGTH];
        // get the pattern details from the locale

        // fix low numbers
        for (int i = 0; i < 3; ++i) {
            prefix[i] = suffix[i] = "";
            divisor[i] = 1;
        }
        Matcher patternMatcher = PATTERN.matcher("");
        Matcher typeMatcher = TYPE.matcher("");
        XPathParts parts = new XPathParts();
        //for (String path : With.in(resolvedCldrFile.iterator("//ldml/numbers/decimalFormats/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/"))) {
        Iterator<String> it = resolvedCldrFile.iterator("//ldml/numbers/decimalFormats/decimalFormatLength");

        while (it.hasNext()) {
            String path = it.next();
            //            if (!path.contains("decimalFormatLength")) {
            //                continue;
            //            }
            parts.set(path);
            String stype = parts.getAttributeValue(3, "type");
            if (!"short".equals(stype)) {
                continue;
            }
            String type = parts.getAttributeValue(-1, "type");

            if (!typeMatcher.reset(type).matches()) {
                debugCreationErrors.add("type (" + type +
                        ") doesn't match expected " + TYPE.pattern()); 
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
                        ") doesn't match expected " + PATTERN.pattern()); 
                continue;
            }

            // HACK '.' for now!!
            prefix[typeZeroCount] = patternMatcher.group(1).replace("'.'", ".");
            suffix[typeZeroCount] = patternMatcher.group(4).replace("'.'", ".");
            int zeroCount = patternMatcher.end(2) - patternMatcher.start(2) - 1;
            divisor[typeZeroCount] = (long) Math.pow(10.0, typeZeroCount - zeroCount);
        }
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(new ULocale(resolvedCldrFile.getLocaleID()));
        return new CompactDecimalFormat(format.toPattern(), format.getDecimalFormatSymbols(), prefix, suffix, divisor, debugCreationErrors, style);
    }

    final static Options myOptions = new Options()
    .add("sourceDir", ".*", CldrUtility.MAIN_DIRECTORY, "The source directory for the compact decimal format information.")
    .add("organization", ".*", null, "The organization to use.")
    .add("locale", ".*", null, "The locales to use (comma-separated).")
    .add("generate", ".*", CldrUtility.BASE_DIRECTORY + "tools/java/org/unicode/cldr/test/", "Hard coded data file.")
    .add("use", null, null, "Use hard coded data file.")
    ;

    /** JUST FOR DEVELOPMENT 
     * @throws IOException */
    public static void main(String[] args) throws IOException {
        myOptions.parse(args, true);

        // set up the CLDR Factories

        String sourceDir = myOptions.get("sourceDir").getValue();
        String organization = myOptions.get("organization").getValue();
        String localeList = myOptions.get("locale").getValue();
        String hardCodedFile = myOptions.get("generate").getUsingImplicitValue() ? null 
                : myOptions.get("generate").getValue();
        boolean useHard = myOptions.get("use").doesOccur();
        
        PrintWriter hardOut = hardCodedFile == null ? null : BagFormatter.openUTF8Writer(hardCodedFile, "CompactDecimalFormatData.java");
        if (hardOut != null) {
            hardOut.println("package org.unicode.cldr.test;\npublic class CompactDecimalFormatData {\n\tstatic void load() {");
        }

        CLDRFile.Factory cldrFactory  = CLDRFile.Factory.make(sourceDir, ".*");
        StandardCodes sc = StandardCodes.make();
        NumberFormat enf = NumberFormat.getInstance(ULocale.ENGLISH);

        Collection<String> locales;

        if (organization != null) {
            locales = sc.getLocaleCoverageLocales(organization);
        } else if (localeList != null) {
            locales = Arrays.asList(localeList.split(","));
            organization = null;
        } else if (useHard) {
            locales = new ArrayList<String>();
            for (ULocale item : localeToData.keySet()) {
                locales.add(item.toString());
            }
        } else {
            locales = cldrFactory.getAvailable();
        }

        // get test values
        List<Double> testValues = new ArrayList<Double>();
        double base       = 123456789012345.0d;
        for (long divisor = 100000000000000L; divisor >= 1; divisor /= 10) {
            double test = base / divisor;
            testValues.add(test);
        }
        List<List<String>> dataList = new ArrayList<List<String>>();

        Set<String> errors = new LinkedHashSet<String>();

        for (String locale : locales) {
            // skip all but the modern locales
            if (organization != null) {
                Level coverage = sc.getLocaleCoverageLevel(organization, locale);
                if (coverage.compareTo(Level.MODERN) < 0) {
                    continue;
                }
            }
            final ULocale uLocale = new ULocale(locale);
            // skip region locales
            if (!uLocale.getCountry().isEmpty()) {
                continue;
            }

            ArrayList<String> list = new ArrayList<String>();
            dataList.add(list);
            // now start
            errors.clear();
            CLDRFile file = cldrFactory.make(locale, true);
            System.out.println("\n" + uLocale.getDisplayName() + " (" + locale + ")");
            list.add(uLocale.getDisplayName() + " (" + locale + ")");

            String[] debugOriginals = new String[MINIMUM_ARRAY_LENGTH];
            CompactDecimalFormat snf;
            try {
                snf = useHard ? new CompactDecimalFormat(uLocale, Style.SHORT) 
                : CompactDecimalFormat.build(file, errors, debugOriginals, Style.SHORT);
            } catch (Exception e) {
                errors.add("Can't construct: " + e.getMessage());
                continue;
            }
            Map<String, Double> already = new HashMap<String, Double>();
            int count = 0;
            String tooLong = null;
            for (double test : testValues) {
                String original = debugOriginals[count++];
                String formatted;
                try {
                    formatted = snf.format(test);
                    if (formatted.length() > 8) {
                        tooLong = formatted + " for " + enf.format(test);
                    }
                    list.add(formatted); // original + "\t" + 
                    Double old = already.get(formatted);
                    if (old != null) {
                        errors.add("Collision: " + formatted + " for " + enf.format(test) + " & " + old);
                    } else {
                        already.put(formatted, test);
                    }
                } catch (Exception e) {
                    errors.add("Runtime with " + enf.format(test) + ", " + e.getMessage());
                }
            }
            if (tooLong != null) {
                errors.add("At least one string too long: " + tooLong);
            }
            for (String error : errors) {
                list.add("ERROR: " + error);
            }
            
            if (hardOut != null) {
                hardOut.println("\t\tCompactDecimalFormat.add(\"" + uLocale + "\", ");
                String[] intFormatted = new String[snf.divisor.length];
                int i = 0;
                for (long divisor : snf.divisor) {
                    intFormatted[i++] = String.valueOf(divisor);
                }
                hardOut.println("\t\t\tnew long[]{" + CollectionUtilities.join(intFormatted, "L, ") + "L},");
                hardOut.println("\t\t\tnew String[]{\"" + CollectionUtilities.join(snf.prefix, "\", \"") + "\"},");
                hardOut.println("\t\t\tnew String[]{\"" + CollectionUtilities.join(snf.suffix, "\", \"") + "\"});");
            }
        }

        if (hardOut != null) {
            hardOut.println("}}");
            hardOut.close();
            return;
        }

        // now do a transposed table
        for (int row = 0; ; ++row) {
            boolean moreToGo = false;
            int num = row - 1;
            if (num >= 0 && num < testValues.size()) {
                System.out.print(testValues.get(num));
            }
            for (List<String> list : dataList) {
                final boolean exists = row < list.size();
                moreToGo |= exists;
                String cell = exists ? list.get(row) : "";
                System.out.print("\t'" + cell);
            }
            System.out.println();
            if (!moreToGo) break;
        }

    }
}
