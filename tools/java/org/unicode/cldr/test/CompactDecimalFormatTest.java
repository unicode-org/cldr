package org.unicode.cldr.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.test.BuildIcuCompactDecimalFormat.CurrencyStyle;
import org.unicode.cldr.test.CompactDecimalFormat.CurrencySymbolDisplay;
import org.unicode.cldr.test.CompactDecimalFormat.Style;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

public class CompactDecimalFormatTest {
    static class MyCurrencySymbolDisplay implements CurrencySymbolDisplay {
        CLDRFile cldrFile;

        public MyCurrencySymbolDisplay(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
        }

        @Override
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

    static final Pattern CURRENCY_PATTERN = Pattern.compile("([^#0-9]*).*?[#0-9]([^#0-9]*)");

    final static Options myOptions = new Options()
        .add("sourceDir", ".*", CldrUtility.MAIN_DIRECTORY,
            "The source directory for the compact decimal format information.")
        .add("organization", ".*", null, "The organization to use.")
        .add("locale", ".*", null, "The locales to use (comma-separated).")
        .add("generate", ".*", CldrUtility.BASE_DIRECTORY + "tools/java/org/unicode/cldr/test/",
            "Hard coded data file.")
        .add("use", null, null, "Use hard coded data file.")
        .add("display", null, null, "Display results on console (instead of generating)")
        .add("currency", "[A-Z]{3}", null, "Currency");

    /**
     * JUST FOR DEVELOPMENT
     * 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        myOptions.parse(args, true);

        // set up the CLDR Factories

        String sourceDir = myOptions.get("sourceDir").getValue();
        String organization = myOptions.get("organization").getValue();
        String localeList = myOptions.get("locale").getValue();
        boolean display = myOptions.get("display").doesOccur();
        String hardCodedFile = myOptions.get("generate").getValue();
        boolean useHard = myOptions.get("use").doesOccur();
        Currency currency = myOptions.get("currency").doesOccur() ? Currency.getInstance(myOptions.get("currency")
            .getValue()) : null;

        PrintWriter hardOut = display
            ? null :
            BagFormatter.openUTF8Writer(hardCodedFile, "CompactDecimalFormatData2.java");
        if (hardOut != null) {
            hardOut
                .println("package org.unicode.cldr.test;\npublic class CompactDecimalFormatData {\n\tstatic void load() {");
        }

        Factory cldrFactory = Factory.make(sourceDir, ".*");
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
        double base = 123456789012345.0d;
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
                snf = BuildIcuCompactDecimalFormat.build(file, errors, debugOriginals, Style.SHORT,
                    new ULocale(locale), CurrencyStyle.PLAIN);
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
                    formatted = currency == null
                        ? snf.format(test)
                        : snf.format(new CurrencyAmount(test, currency));
                    if (formatted.length() > 8 && currency == null) {
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
                hardOut.println("\t\t\tnew String[]{\"" + CollectionUtilities.join(snf.prefixes.entrySet(), "\", \"")
                    + "\"},");
                hardOut.println("\t\t\tnew String[]{\"" + CollectionUtilities.join(snf.suffixes.entrySet(), "\", \"")
                    + "\"});");
            }
        }

        if (hardOut != null) {
            hardOut.println("}}");
            hardOut.close();
            return;
        }

        // now do a transposed table
        for (int row = 0;; ++row) {
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
