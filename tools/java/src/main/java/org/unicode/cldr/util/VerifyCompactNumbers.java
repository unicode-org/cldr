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
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.BuildIcuCompactDecimalFormat;
import org.unicode.cldr.test.BuildIcuCompactDecimalFormat.CurrencyStyle;
import org.unicode.cldr.tool.FormattedFileWriter;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.ShowData;
import org.unicode.cldr.tool.ShowPlurals;
import org.unicode.cldr.tool.TablePrinter;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;

import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

public class VerifyCompactNumbers {

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final String DIR = CLDRPaths.CHART_DIRECTORY + "verify/numbers/";
    final static Options myOptions = new Options();

    enum MyOptions {
        organization(".*", "CLDR", "organization"), filter(".*", ".*", "locale filter (regex)"), currency(".*", "EUR", "show currency"),;
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
        FileCopier.copy(ShowData.class, "verify-index.html", DIR, "index.html");
        FileCopier.copy(ShowData.class, "index.css", DIR, "index.css");
        FormattedFileWriter.copyIncludeHtmls(DIR);

        String organization = MyOptions.organization.option.getValue();
        String filter = MyOptions.filter.option.getValue();
        boolean showCurrency = true; // MyOptions.currency.option.doesOccur();
        String currencyCode = MyOptions.currency.option.getValue();

        Factory factory2 = Factory.make(CLDRPaths.MAIN_DIRECTORY, filter);
        CLDRFile englishCldrFile = factory2.make("en", true);

        SupplementalDataInfo sdi = CLDR_CONFIG.getSupplementalDataInfo();
        Set<String> defaultContentLocales = sdi.getDefaultContentLocales();
        NumberFormat enf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
        enf.setGroupingUsed(false);

        Set<String> availableLanguages = new TreeSet<String>(factory2.getAvailableLanguages());
        if (Pattern.matches(filter, "pt_PT")) {
            availableLanguages.add("pt_PT");
        }

        PrintWriter plainText = FileUtilities.openUTF8Writer(DIR, "compactTestFile.txt");
        DateTimeFormats.writeCss(DIR);
        final CLDRFile english = CLDR_CONFIG.getEnglish();

        Map<String, String> indexMap = new TreeMap<>(CLDR_CONFIG.getCollator());

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

            PrintWriter out = FileUtilities.openUTF8Writer(DIR, locale + ".html");
            String title = "Verify Number Formats: " + englishCldrFile.getName(locale);
            out.println("<!doctype HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN'><html><head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "<title>" + title + "</title>\n" +
                "<link rel='stylesheet' type='text/css' href='index.css'>\n" +
                "</head><body><h1>" + title + "</h1>\n"
                + "<p><a href='index.html'>Index</a></p>\n");

            CLDRFile cldrFile = factory2.make(locale, true, DraftStatus.contributed);

            showNumbers(cldrFile, showCurrency, currencyCode, out, factory2);

            out.println("</body></html>");
            out.close();
            indexMap.put(english.getName(locale), locale + ".html");

        }
        try (PrintWriter index = DateTimeFormats.openIndex(DIR, "Numbers")) {
            DateTimeFormats.writeIndexMap(indexMap, index);
        }

        plainText.close();

    }

    public static void showNumbers(CLDRFile cldrFile, boolean showCurrency,
        String currencyCode, Appendable out, Factory factory) {
        try {
            Set<String> debugCreationErrors = new LinkedHashSet<String>();
            Set<String> errors = new LinkedHashSet<String>();
            String locale = cldrFile.getLocaleID();

            TablePrinter tablePrinter1 = new TablePrinter()
                // .setCaption("Timezone Formats")
                .setTableAttributes("class='dtf-table'")
                .addColumn("Numeric Format").setHeaderCell(true).setHeaderAttributes("class='dtf-th'")
                .setCellAttributes("class='dtf-s'")
                .addColumn("Compact-Short").setHeaderAttributes("class='dtf-th'").setCellAttributes("class='dtf-s'")
                .addColumn("Compact-Long").setHeaderAttributes("class='dtf-th'").setCellAttributes("class='dtf-s'");
            if (showCurrency) {
                tablePrinter1
                    .addColumn("Compact-Short<br>+Currency")
                    .setHeaderAttributes("class='dtf-th'")
                    .setCellAttributes("class='dtf-s'")
//                    .addColumn("Compact-Short<br>+Unit")
//                    .setHeaderAttributes("class='dtf-th'")
//                    .setCellAttributes("class='dtf-s'")
                // .addColumn("Compact-Long<br>+Currency")
                // .addColumn("Compact-Long<br>+Currency-Long")
//                    .addColumn("Numeric Format").setHeaderCell(true).setHeaderAttributes("class='dtf-th'")
//                      .setCellAttributes("class='dtf-s'")
                ;
            }
            //            tablePrinter1.addColumn("View").setHeaderCell(true).setHeaderAttributes("class='dtf-th'").setCellAttributes("class='dtf-s'");

            ;

            ULocale locale2 = new ULocale(locale);
            NumberFormat nf = NumberFormat.getInstance(locale2);
            // nf.setMaximumFractionDigits(0);
            SupplementalDataInfo sdi = CLDR_CONFIG.getSupplementalDataInfo();
            PluralInfo pluralInfo = sdi.getPlurals(locale);
            String[] debugOriginals = null;
            CompactDecimalFormat cdf = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
                debugOriginals, CompactStyle.SHORT, locale2, CurrencyStyle.PLAIN, currencyCode);
            captureErrors(debugCreationErrors, errors, locale, "short");
            CompactDecimalFormat cdfs = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
                debugOriginals, CompactStyle.LONG, locale2, CurrencyStyle.PLAIN, currencyCode);
            captureErrors(debugCreationErrors, errors, locale, "long");

            CompactDecimalFormat cdfCurr = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
                debugOriginals, CompactStyle.SHORT, locale2, CurrencyStyle.CURRENCY, currencyCode);
            captureErrors(debugCreationErrors, errors, locale, "short-curr");
//            CompactDecimalFormat cdfU = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
//                debugOriginals, CompactStyle.SHORT, locale2, CurrencyStyle.UNIT, "EUR");
//            captureErrors(debugCreationErrors, errors, locale, "short-kg");
//             CompactDecimalFormat cdfsCurr = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
//             debugOriginals, CompactStyle.SHORT, locale2, CurrencyStyle.CURRENCY, currencyCode);
//             CompactDecimalFormat cdfsCurrISO = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
//             debugOriginals, CompactStyle.LONG, locale2, CurrencyStyle.ISO_CURRENCY, "EUR");

            // Collect samples for display
            // one path for group-3, one for group-4
            // TODO, fix for indic.
            int factor = USES_GROUPS_OF_4.contains(locale) ? 10000 : 1000;

            // we want to collect a sample of at least one sample for each plural category for each
            // power of ten
            Set<Double> samples = new TreeSet<Double>();
            samples.add(1.1d);
            samples.add(1.5d);
            samples.add(1100d);
            collectItems(pluralInfo, 1, 10, samples);
            collectItems(pluralInfo, 10, 100, samples);
            collectItems(pluralInfo, 100, 1000, samples);
            int sigDigits = 3;
            if (factor > 1000) {
                collectItems(pluralInfo, 1000, 10000, samples);
                sigDigits = 4;
            }
            if (cdf != null) {
                cdf.setMaximumSignificantDigits(sigDigits);
            }
            if (cdfs != null) {
                cdfs.setMaximumSignificantDigits(sigDigits);
            }
            if (cdfCurr != null) {
                cdfCurr.setCurrency(Currency.getInstance(currencyCode));
                cdfCurr.setMaximumSignificantDigits(sigDigits);
            }
//            cdfU.setMaximumSignificantDigits(sigDigits);

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
            for (long i = 1; i <= 100000000000000L; i *= factor) {
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

                    String formattedNumber = nf.format(source);
                    String compactFormattedNumber = cdf == null ? "n/a" : cdf.format(source);
                    String compactLongFormattedNumber = cdfs == null ? "n/a" : cdfs.format(source);
                    // plainText.println(locale
                    // + "\t__" + source
                    // + "\t__" + compactFormattedNumber
                    // + "\t__" + compactLongFormattedNumber
                    // );
                    tablePrinter1.addRow()
                        .addCell(formattedNumber)
                        .addCell(compactFormattedNumber)
                        .addCell(compactLongFormattedNumber);
                    if (showCurrency) {
                        tablePrinter1
                            .addCell(cdfCurr == null ? "n/a" : cdfCurr.format(source))
//                            .addCell(cdfU.format(source))
//                             .addCell(cdfsCurr.format(source))
                        // .addCell(cdfsCurrLong.format(source))
                        // .addCell(cdfsCurrLong.format(source))
                        //.addCell(formattedNumber)
                        ;
                    }
                    //                    String view = PathHeader.getLinkedView(surveyUrl, cldrFile, METAZONE_PREFIX + metazone + METAZONE_SUFFIX);
                    //                    tablePrinter1.addCell(view == null
                    //                            ? ""
                    //                                    : view);
                    tablePrinter1
                        .finishRow();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            out.append("<p>To correct problems in compact numbers below, please go to "
                + PathHeader.SECTION_LINK
                + CLDR_CONFIG.urls().forPage(cldrFile.getLocaleID(), PageId.Compact_Decimal_Formatting)
                + "'><em>" + PageId.Compact_Decimal_Formatting
                + "</em></a>.</p>");
            out.append(tablePrinter1.toString() + "\n");
            out.append("<h3>Plural Rules</h3>");
            out.append("<p>Look over the Minimal Pairs to make sure they are ok. "
                + "Then review the examples in the cell to the left. "
                + "All of those you should be able to substitute for the numbers in the Minimal Pairs, "
                + "with an acceptable result. "
                + "If any would be incorrect, please "
                + "<a target='ticket' href='https://unicode.org/cldr/trac/newticket'>file a ticket</a>.</p>"
                + "<p>For more details, see " +
                "<a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/index/cldr-spec/plural-rules'>Plural Rules</a>.</p>");
            ShowPlurals showPlurals = new ShowPlurals(CLDR_CONFIG.getSupplementalDataInfo());
            showPlurals.printPluralTable(cldrFile, locale, out, factory);
            showPlurals.appendBlanksForScrolling(out);
            showErrors(errors, out);
            showErrors(debugCreationErrors, out);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static String surveyUrl = CLDR_CONFIG.getProperty("CLDR_SURVEY_URL",
        "http://st.unicode.org/cldr-apps/survey");

    private static void showErrors(Set<String> errors, Appendable out) throws IOException {
        if (errors.size() != 0) {
            out.append("<h2>" + "Errors" + "</h2>\n");
            for (String s : errors) {
                out.append("<p>" + s + "</p>\n");
            }
            errors.clear();
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
