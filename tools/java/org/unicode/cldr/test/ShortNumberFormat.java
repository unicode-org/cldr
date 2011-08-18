package org.unicode.cldr.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class ShortNumberFormat extends DecimalFormat {
    static final int LIMIT_COUNT = 15;
    private final String[] prefix = new String[LIMIT_COUNT];
    private final String[] suffix = new String[LIMIT_COUNT];
    private final long[] divisor = new long[LIMIT_COUNT];
    private final String[] debugOriginals = new String[LIMIT_COUNT];
    private final Set<String> creationErrors = new LinkedHashSet<String>();

    private static final Pattern PATTERN = Pattern.compile("([^0,]*)([0]+)([.]0+)?([^0]*)");
    private static final Pattern TYPE = Pattern.compile("1([0]*)");

    ShortNumberFormat(CLDRFile resolvedCldrFile) {
        // get the pattern details from the locale
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(new ULocale(resolvedCldrFile.getLocaleID()));
        setDecimalFormatSymbols(format.getDecimalFormatSymbols());
        applyPattern(format.toPattern());
        setMaximumSignificantDigits(2); // default significant digits
        setSignificantDigitsUsed(true);

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
                creationErrors.add("type (" + type +
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
                creationErrors.add("pattern (" + pattern +
                        ") doesn't match expected " + PATTERN.pattern()); 
                continue;
            }

            // HACK '.' for now!!
            prefix[typeZeroCount] = patternMatcher.group(1).replace("'.'", ".");
            suffix[typeZeroCount] = patternMatcher.group(4).replace("'.'", ".");
            int zeroCount = patternMatcher.end(2) - patternMatcher.start(2) - 1;
            divisor[typeZeroCount] = (long) Math.pow(10.0, typeZeroCount - zeroCount);
        }
        // do basic validation
        long oldDivisor = 0;
        for (int i = 0; i < prefix.length; ++i) {
            if (prefix[i] == null || suffix[i] == null) {
                creationErrors.add("prefix/suffix null for " + i); 

            }
            if (divisor[i] < oldDivisor) {
                creationErrors.add("Bad divisor, the divisor for 10E" + i + "(" + divisor[i] +
                        ") is less than the divisor for the divisor for 10E" + (i-1) + "(" + oldDivisor + 
                ")"); 
            }
            oldDivisor = divisor[i];
        }
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        if (number < 0.0d) {
            throw new UnsupportedOperationException("ShortNumberFormat doesn't handle negative numbers yet.");
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
        throw new UnsupportedOperationException();
    }

    @Override
    public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringBuffer format(com.ibm.icu.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number parse(String text, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    public String[] getDebugOriginals() {
        return debugOriginals;
    }

    public Set<String> getDebugCreationErrors() {
        return creationErrors;
    }

    public static void main(String[] args) {
        CLDRFile.Factory cldrFactory  = CLDRFile.Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        StandardCodes sc = StandardCodes.make();
        NumberFormat enf = NumberFormat.getInstance(ULocale.ENGLISH);

        Collection<String> locales;

        String organization = args[0];
        if (args.length == 1 && organization.length() > 3) {
            locales = sc.getLocaleCoverageLocales(organization);
        } else {
            locales = Arrays.asList(args);
            organization = null;
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
            Level coverage = sc.getLocaleCoverageLevel(organization, locale);
            if (coverage.compareTo(Level.MODERN) < 0) {
                continue;
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

            ShortNumberFormat snf;
            try {
                snf = new ShortNumberFormat(file);
                errors.addAll(snf.getDebugCreationErrors());
            } catch (Exception e) {
                errors.add("Can't construct: " + e.getMessage());
                continue;
            }
            Map<String, Double> already = new HashMap<String, Double>();
            int count = 0;
            String tooLong = null;
            for (double test : testValues) {
                String original = snf.getDebugOriginals()[count++];
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
