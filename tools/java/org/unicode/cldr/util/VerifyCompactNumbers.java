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
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class VerifyCompactNumbers {
    // later, look at DateTimeFormats to set up as an HTML table

    private static final Set<String> USES_GROUPS_OF_4 = new HashSet<String>(Arrays.asList("ko", "ja", "zh", "zh_Hant"));

    /**
     * Produce a set of static tables from the vxml data. Only a stopgap until the above is integrated into ST.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String organization = args.length > 0 ? args[0] : "Google";
        Factory factory2 = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile englishCldrFile = factory2.make("en", true);

        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = sdi.getDefaultContentLocales();
        for (String locale : factory2.getAvailableLanguages()) {
            if (defaultContentLocales.contains(locale)) {
                continue;
            }
            Level level = StandardCodes.make().getLocaleCoverageLevel(organization, locale);
            if (Level.MODERN.compareTo(level) > 0) {
                continue;
            }
            
            ULocale locale2 = new ULocale(locale);
            NumberFormat nf = NumberFormat.getIntegerInstance(locale2);
            nf.setMaximumFractionDigits(0);
            CLDRFile cldrFile = factory2.make(locale, true);
            PluralInfo pluralInfo = sdi.getPlurals(locale);
            Set<Double> samples = new TreeSet<Double>();
            for (Entry<Count, List<Double>> entry : pluralInfo.getCountToExamplesMap().entrySet()) {
                samples.add(entry.getValue().get(0));
            }
            String[] debugOriginals = null;
            Set<String> debugCreationErrors = new LinkedHashSet<String>();
            CompactDecimalFormat cdf = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors, debugOriginals, Style.SHORT, locale2);
            CompactDecimalFormat cdfs = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors, debugOriginals, Style.LONG, locale2);
            // one path for group-3, one for group-4
            int factor = USES_GROUPS_OF_4.contains(locale) ? 10000 : 1000;
            
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
            try {
            for (long i = factor; i <= 100000000000000L; i *= factor) {
                System.out.print(column12 + "\tNumeric");
                for (Double sample : samples) {
                    double source = i * sample;
                    System.out.print("\t__" + nf.format(source));
                }
                System.out.println();
                System.out.print(column12 + "\tCompact-Short");
                for (Double sample : samples) {
                    double source = i * sample;
                    String formatted = cdf.format(source);
                    System.out.print("\t__" + formatted);
                }
                System.out.println();
                System.out.print(column12 + "\tCompact-Long");
                for (Double sample : samples) {
                    double source = i * sample;
                    String formatted = cdfs.format(source);
                    System.out.print("\t__" + formatted);
                }
                System.out.println("\n");
            }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
