package org.unicode.cldr.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CompactDecimalFormat.Style;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class CompactDecimalFormatTest {
    /** JUST FOR DEVELOPMENT */
    private static final CompactDecimalFormat build(CLDRFile resolvedCldrFile, Set<String> debugCreationErrors, String[] debugOriginals, Style style) {
        String[] prefix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
        String[] suffix = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
        long[] divisor = new long[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
        // get the pattern details from the locale

        // fix low numbers
        for (int i = 0; i < 3; ++i) {
            prefix[i] = suffix[i] = "";
            divisor[i] = 1;
        }
        Matcher patternMatcher = CompactDecimalFormatTest.PATTERN.matcher("");
        Matcher typeMatcher = CompactDecimalFormatTest.TYPE.matcher("");
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
        String hardCodedFile = myOptions.get("generate").getValue();
        boolean useHard = myOptions.get("use").doesOccur();
        
        PrintWriter hardOut = hardCodedFile == null ? null : BagFormatter.openUTF8Writer(hardCodedFile, "CompactDecimalFormatData.java");
        if (hardOut != null) {
            hardOut.println("package org.unicode.cldr.test;\npublic class CompactDecimalFormatData {\n\tstatic void load() {");
        }

        Factory cldrFactory  = Factory.make(sourceDir, ".*");
        StandardCodes sc = StandardCodes.make();
        NumberFormat enf = NumberFormat.getInstance(ULocale.ENGLISH);

        Collection<String> locales;

        if (organization != null) {
            locales = sc.getLocaleCoverageLocales(organization);
        } else if (localeList != null) {
            locales = Arrays.asList(localeList.split(","));
            organization = null;
        } else if (useHard) {
            locales = new TreeSet<String>();
            for (ULocale item : CompactDecimalFormat.localeToData.keySet()) {
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

            String[] debugOriginals = new String[CompactDecimalFormat.MINIMUM_ARRAY_LENGTH];
            CompactDecimalFormat snf;
            try {
                snf = useHard ? new CompactDecimalFormat(uLocale, Style.SHORT) 
                : build(file, errors, debugOriginals, Style.SHORT);
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

    static final Pattern PATTERN = Pattern.compile("([^0,]*)([0]+)([.]0+)?([^0]*)");
    static final Pattern TYPE = Pattern.compile("1([0]*)");
}
