package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.BuildIcuCompactDecimalFormat;
import org.unicode.cldr.test.BuildIcuCompactDecimalFormat.CurrencyStyle;
import org.unicode.cldr.test.CompactDecimalFormat;
import org.unicode.cldr.test.CompactDecimalFormat.Style;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.TablePrinter;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class VerifyCompactNumbers {

    final static Options myOptions = new Options();

    enum MyOptions {
        organization(".*", "Google", "organization"),
        filter(".*", ".*", "locale filter (regex)"),
        currency(null, null, "show currency"), ;
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
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.organization, args, true);

        String organization = MyOptions.organization.option.getValue();
        String filter = MyOptions.filter.option.getValue();
        boolean showCurrency = MyOptions.currency.option.doesOccur();

        Factory factory2 = Factory.make(CldrUtility.MAIN_DIRECTORY, filter);
        CLDRFile englishCldrFile = factory2.make("en", true);

        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = sdi.getDefaultContentLocales();
        NumberFormat enf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
        enf.setGroupingUsed(false);
        Set<String> debugCreationErrors = new LinkedHashSet<String>();
        Set<String> errors = new LinkedHashSet<String>();

        TablePrinter tablePrinter = new TablePrinter() // .setCaption("Timezone Formats")
            .addColumn("Number").setHeaderCell(true)
            .addColumn("Numeric Format")
            .addColumn("Compact-Short")
            .addColumn("Compact-Long");
        if (showCurrency) {
            tablePrinter
                .addColumn("Compact-Short<br>+Currency")
                .addColumn("Compact-Long<br>+Currency")
                .addColumn("Number").setHeaderCell(true);
        }
        ;

        Set<String> availableLanguages = new TreeSet<String>(factory2.getAvailableLanguages());
        availableLanguages.add("pt_PT");

        for (String locale : availableLanguages) {
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

            tablePrinter.clearRows();

            ULocale locale2 = new ULocale(locale);
            NumberFormat nf = NumberFormat.getIntegerInstance(locale2);
            nf.setMaximumFractionDigits(0);
            CLDRFile cldrFile = factory2.make(locale, true, DraftStatus.contributed);
            PluralInfo pluralInfo = sdi.getPlurals(locale);
            String[] debugOriginals = null;
            CompactDecimalFormat cdf = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
                debugOriginals, Style.SHORT, locale2, CurrencyStyle.PLAIN);
            captureErrors(debugCreationErrors, errors, locale, "short");
            CompactDecimalFormat cdfs = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
                debugOriginals, Style.LONG, locale2, CurrencyStyle.PLAIN);
            captureErrors(debugCreationErrors, errors, locale, "long");

            CompactDecimalFormat cdfCurr = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
                debugOriginals, Style.SHORT, locale2, CurrencyStyle.CURRENCY);
            captureErrors(debugCreationErrors, errors, locale, "short");
            CompactDecimalFormat cdfsCurr = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
                debugOriginals, Style.LONG, locale2, CurrencyStyle.CURRENCY);
            captureErrors(debugCreationErrors, errors, locale, "long");

            // Collect samples for display
            // one path for group-3, one for group-4
            // TODO, fix for indic.
            int factor = USES_GROUPS_OF_4.contains(locale) ? 10000 : 1000;

            // we want to collect a sample of at least one sample for each plural category for each
            // power of ten
            Set<Double> samples = new TreeSet<Double>();
            samples.add(1.5d);
            collectItems(pluralInfo, 1, 10, samples);
            collectItems(pluralInfo, 10, 100, samples);
            collectItems(pluralInfo, 100, 1000, samples);
            if (factor > 1000) {
                collectItems(pluralInfo, 1000, 10000, samples);
            }

            // for (Entry<Count, List<Double>> entry : pluralInfo.getCountToExamplesMap().entrySet()) {
            // samples.add(entry.getValue().get(0));
            // }
            //
            // Set<Double> samples2 = new TreeSet<Double>();
            // for (int i = 10; i < factor; i *= 10) {
            // for (Double sample : samples) {
            // samples2.add(sample*i);
            // }
            // }
            // samples.addAll(samples2);

            Set<Double> allSamples = new TreeSet<Double>();
            for (long i = factor; i <= 100000000000000L; i *= factor) {
                for (Double sample : samples) {
                    double source = i * sample;
                    allSamples.add(source);
                }
            }

            try {
                for (double source : allSamples) {
                    if (false && source == 22000000 && locale.equals("cs")) {
                        System.out.println("**");
                    }

                    tablePrinter.addRow()
                        .addCell(enf.format(source))
                        .addCell(nf.format(source))
                        .addCell(cdf.format(source))
                        .addCell(cdfs.format(source));
                    if (showCurrency) {
                        tablePrinter.addCell(cdfCurr.format(source))
                            .addCell(cdfsCurr.format(source))
                            .addCell(enf.format(source));
                    }
                    tablePrinter
                        .finishRow();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.TMP_DIRECTORY + "verify/numbers/",
                locale +
                    ".html");
            String title = "Verify Number Formats: " + englishCldrFile.getName(locale);
            out.println("<html><head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "<title>" + title + "</title>\n" +
                "<link rel='stylesheet' type='text/css' href='index.css'>\n" +
                "</head><body><h1>" + title + "</h1>");

            out.println(tablePrinter.toString());
            out.println("</body></html>");
            out.close();
        }

        for (String s : errors) {
            System.out.println(s);
        }
    }

    private static Set<Double> collectItems(PluralInfo pluralInfo, double start, double limit,
        Set<Double> samples) {
        // TODO optimize once we have all the keywords
        Map<String, Double> ones = new TreeMap<String, Double>();
        for (double i = start; i < limit; ++i) {
            String cat = pluralInfo.getPluralRules().select(i);
            if (ones.containsKey(cat)) {
                continue;
            }
            ones.put(cat, i);
        }
        samples.addAll(ones.values());
        return samples;
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
