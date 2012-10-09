package org.unicode.cldr.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.BuildIcuCompactDecimalFormat;
import org.unicode.cldr.test.CompactDecimalFormat;
import org.unicode.cldr.test.CompactDecimalFormat.Style;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class VerifyCompactNumbers {
    
    final static Options myOptions = new Options();
    enum MyOptions {
        organization(".*", "Google", "organization"),
        filter(".*", ".*", "locale filter (regex)"),
        ;
        // boilerplate
        final Option option;
        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    // later, look at DateTimeFormats to set up as an HTML table

    private static final Set<String> USES_GROUPS_OF_4 = new HashSet<String>(Arrays.asList("ko", "ja", "zh", "zh_Hant"));

    /**
     * Produce a set of static tables from the vxml data. Only a stopgap until the above is integrated into ST.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.organization, args, true);
        
        String organization = MyOptions.organization.option.getValue();
        String filter = MyOptions.filter.option.getValue();
        
        Factory factory2 = Factory.make(CldrUtility.MAIN_DIRECTORY, filter);
        CLDRFile englishCldrFile = factory2.make("en", true);

        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = sdi.getDefaultContentLocales();
        NumberFormat enf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
        enf.setGroupingUsed(false);
        Set<String> debugCreationErrors = new LinkedHashSet<String>();
        Set<String> errors = new LinkedHashSet<String>();

        for (String locale : factory2.getAvailableLanguages()) {
            if (defaultContentLocales.contains(locale)) {
                continue;
            }
            Level level = StandardCodes.make().getLocaleCoverageLevel(organization, locale);
            if (Level.MODERN.compareTo(level) > 0) {
                continue;
            }
            // TODO: fix to ignore locales with no data.
            if (locale.equals("ne") || locale.equals("cy")) {
                continue;
            }
            // one path for group-3, one for group-4
            int factor = USES_GROUPS_OF_4.contains(locale) ? 10000 : 1000;
            
            ULocale locale2 = new ULocale(locale);
            NumberFormat nf = NumberFormat.getIntegerInstance(locale2);
            nf.setMaximumFractionDigits(0);
            CLDRFile cldrFile = factory2.make(locale, true, DraftStatus.contributed);
            PluralInfo pluralInfo = sdi.getPlurals(locale);
            Set<Double> samples = new TreeSet<Double>();
            for (Entry<Count, List<Double>> entry : pluralInfo.getCountToExamplesMap().entrySet()) {
                samples.add(entry.getValue().get(0));
            }
            String[] debugOriginals = null;
            CompactDecimalFormat cdf = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors, debugOriginals, Style.SHORT, locale2);
            captureErrors(debugCreationErrors, errors, locale, "short");
            CompactDecimalFormat cdfs = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors, debugOriginals, Style.LONG, locale2);
            captureErrors(debugCreationErrors, errors, locale, "long");

            Set<Double> samples2 = new TreeSet<Double>();
            for (int i = 10; i < factor; i *= 10) {
                for (Double sample : samples) {
                    samples2.add(sample*i);
                }
            }
            samples.addAll(samples2);
            samples.add(1.5d);
            System.out.println("———\t" + englishCldrFile.getName(locale) + "\t———");

            String column12 = (locale  + "\t" +  englishCldrFile.getName(locale));
            System.out.print(column12 + "\tNumeric\tCompact-Short\tCompact-Long\tFixed Numeric\tFixed Compact-Short\tFixed Compact-Long\n");

            try {
                // we print the __ so that it can be imported into a spreadsheet without problems.
                for (long i = factor; i <= 100000000000000L; i *= factor) {
                    for (Double sample : samples) {
                        double source = i * sample;
                        if (false && source == 22000000 && locale.equals("cs")) {
                            System.out.println("**");
                        }
                        System.out.print(locale + "\t__" + enf.format(source));
                        System.out.print("\t__" + nf.format(source));
                        String formatted = cdf.format(source);
                        System.out.print("\t__" + formatted);
                        formatted = cdfs.format(source);
                        System.out.println("\t__" + formatted);
                    }
                    System.out.println();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String s : errors) {
            System.out.println(s);
        }
    }

    private static void captureErrors(Set<String> debugCreationErrors, Set<String> errors, String locale, String length) {
        if (debugCreationErrors.size() != 0) {
            for (String s : debugCreationErrors) {
                errors.add(locale + "\t" + length + "\t" + s);
            }
            debugCreationErrors.clear();
        }
    }
}
