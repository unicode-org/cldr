package org.unicode.cldr.unittest;

import com.google.common.base.Objects;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.test.BuildIcuCompactDecimalFormat;
import org.unicode.cldr.test.BuildIcuCompactDecimalFormat.CurrencyStyle;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.VerifyCompactNumbers;

public class TestCompactNumbers extends TestFmwkPlus {
    static final boolean DEBUG = false;
    private static final StandardCodes sc = StandardCodes.make();
    private static final CLDRConfig CLDRCONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SDI = CLDRCONFIG.getSupplementalDataInfo();
    private static final CLDRFile ENGLISH = CLDRCONFIG.getEnglish();
    private static final Factory factory2 = CLDRCONFIG.getCldrFactory();

    public static void main(String[] args) {
        new TestCompactNumbers().run(args);
    }

    public void TestVerify() {
        // Just verify no crashes
        CLDRFile cldrFile = factory2.make("it", true);
        Appendable out = new StringBuilder();
        VerifyCompactNumbers.showNumbers(cldrFile, true, "EUR", out, factory2);
        if (DEBUG) {
            System.out.println(out);
        }
    }

    public void TestInternals() {
        // Just verify no crashes
        String locale = "it";
        String currencyCode = "EUR";
        ULocale locale2 = ULocale.forLanguageTag(locale);
        CLDRFile cldrFile = factory2.make(locale, true);
        Set<String> debugCreationErrors = new LinkedHashSet<>();
        String[] debugOriginals = null;

        final ICUServiceBuilder builder = new ICUServiceBuilder(cldrFile);
        NumberFormat nf = builder.getNumberFormat(1);

        CompactDecimalFormat cdf =
                BuildIcuCompactDecimalFormat.build(
                        cldrFile,
                        debugCreationErrors,
                        debugOriginals,
                        CompactStyle.SHORT,
                        locale2,
                        CurrencyStyle.PLAIN,
                        currencyCode);

        Map<String, Map<String, String>> data =
                BuildIcuCompactDecimalFormat.buildCustomData(
                        cldrFile, CompactStyle.SHORT, CurrencyStyle.PLAIN);
        if (DEBUG) {
            for (Entry<String, Map<String, String>> entry : data.entrySet()) {
                System.out.println(entry);
            }
        }

        CompactDecimalFormat cdfs =
                BuildIcuCompactDecimalFormat.build(
                        cldrFile,
                        debugCreationErrors,
                        debugOriginals,
                        CompactStyle.LONG,
                        locale2,
                        CurrencyStyle.PLAIN,
                        currencyCode);
        CompactDecimalFormat cdfCurr =
                BuildIcuCompactDecimalFormat.build(
                        cldrFile,
                        debugCreationErrors,
                        debugOriginals,
                        CompactStyle.SHORT,
                        locale2,
                        CurrencyStyle.CURRENCY,
                        currencyCode);

        Set<Double> allSamples =
                VerifyCompactNumbers.collectSamplesAndSetFormats(
                        currencyCode, locale, SDI, cdf, cdfs, cdfCurr);

        for (double source : allSamples) {
            if (false && source == 22000000 && locale.equals("cs")) {
                System.out.println("**");
            }

            String formattedNumber = nf.format(source);
            String compactFormattedNumber = cdf == null ? "n/a" : cdf.format(source);
            String compactLongFormattedNumber = cdfs == null ? "n/a" : cdfs.format(source);
            String compactCurrFormattedNumber = cdfs == null ? "n/a" : cdfCurr.format(source);
            if (DEBUG)
                System.out.println(
                        source
                                + "\tnf:\t"
                                + formattedNumber
                                + "\tcnf:\t"
                                + compactFormattedNumber
                                + "\tclnf:\t"
                                + compactLongFormattedNumber
                                + "\tccnf:\t"
                                + compactCurrFormattedNumber);
        }
    }

    /**
     * Test case for VerifyCompactNumbers https://unicode-org.atlassian.net/browse/CLDR-15737
     * https://unicode-org.atlassian.net/browse/CLDR-15762
     */
    public void TestVerifyCompactNumbers() {
        Set<String> debugCreationErrors = new LinkedHashSet<>();
        String[] debugOriginals = null;
        String oldLocale = "";
        String oldCurrencyCode = "";
        CompactDecimalFormat cdfCurr = null;
        CompactStyle compactStyle = null;
        CurrencyStyle currencyStyle = null;
        CLDRFile cldrFile = null;

        Object[][] tests = {
            {"cs", null, 1100000d, "1,1 milionu"}, // should be 'many', not 1,1 milionů == 'other'
            /* Background for cs
             * <pattern type="1000000" count="one">0 milion</pattern>
             * <pattern type="1000000" count="few">0 miliony</pattern>
             * <pattern type="1000000" count="many">0 milionu</pattern>
             * <pattern type="1000000" count="other">0 milionů</pattern>
             * https://unicode-org.github.io/cldr-staging/charts/41/supplemental/language_plural_rules.html#cs
             */
            {"fr", "EUR", 100d, "100 €"},
            {"fr", "EUR", 1000d, "1 k €"},
        };

        for (Object[] row : tests) {
            String locale = row[0].toString();
            String currencyCode = row[1] == null ? null : row[1].toString();
            double value = (double) row[2];
            String expected = row[3].toString();

            if (!locale.equals(oldLocale) || Objects.equal(currencyCode, oldCurrencyCode)) {
                oldLocale = locale;
                oldCurrencyCode = currencyCode;
                cldrFile = factory2.make(locale, true);

                debugCreationErrors.clear();
                if (currencyCode == null) {
                    compactStyle = CompactStyle.LONG;
                    currencyStyle = CurrencyStyle.PLAIN;

                    cdfCurr =
                            BuildIcuCompactDecimalFormat.build(
                                    cldrFile,
                                    debugCreationErrors,
                                    debugOriginals,
                                    compactStyle,
                                    ULocale.forLanguageTag(locale),
                                    currencyStyle,
                                    null);

                    // note: custom data looks good:
                    // 1000000={other=0 milionů, one=0 milion, few=0 miliony, many=0 milionu}

                } else {
                    compactStyle = CompactStyle.SHORT;
                    currencyStyle = CurrencyStyle.CURRENCY;

                    cdfCurr =
                            BuildIcuCompactDecimalFormat.build(
                                    cldrFile,
                                    debugCreationErrors,
                                    debugOriginals,
                                    compactStyle,
                                    ULocale.forLanguageTag(locale),
                                    currencyStyle,
                                    currencyCode);

                    cdfCurr.setCurrency(Currency.getInstance(currencyCode));
                    int sigDigits = 3;
                    cdfCurr.setMaximumSignificantDigits(sigDigits);
                }
            }
            String actual = cdfCurr.format(value);
            if (!assertEquals("Formatted " + value, expected, actual)) {
                if (DEBUG) {
                    PluralInfo rules = SupplementalDataInfo.getInstance().getPlurals(locale);
                    int v, f, e;
                    DecimalQuantity dq = DecimalQuantity_DualStorageBCD.fromExponentString("1.1");
                    Count count = rules.getCount(dq);
                    System.out.println("Locale: " + locale);
                    final ICUServiceBuilder builder = new ICUServiceBuilder(cldrFile);

                    DecimalFormat decimalFormat =
                            currencyStyle == CurrencyStyle.PLAIN
                                    ? builder.getNumberFormat(1)
                                    : builder.getCurrencyFormat(currencyCode);
                    final String pattern = decimalFormat.toPattern();

                    System.out.println("Fallback pattern: " + pattern);
                    System.out.println("Plural Rules: ");
                    semiSplit
                            .split(rules.getRules())
                            .forEach(x -> System.out.println("\t" + x + ";"));
                    System.out.println("Plural sample result: " + dq + " => " + count);

                    Map<String, Map<String, String>> customData =
                            BuildIcuCompactDecimalFormat.buildCustomData(
                                    cldrFile, compactStyle, currencyStyle);
                    customData.forEach((k, vv) -> System.out.println("\t" + k + "\t" + vv));
                }
            }
        }
    }

    static final Splitter semiSplit = Splitter.on(';').trimResults();
}
