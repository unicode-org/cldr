package org.unicode.cldr.unittest;

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
import org.unicode.cldr.util.VerifyCompactNumbers;

import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class TestCompactNumbers  extends TestFmwkPlus {
    static final boolean DEBUG = false;
    private static StandardCodes sc = StandardCodes.make();
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

        ICUServiceBuilder builder = new ICUServiceBuilder().setCldrFile(cldrFile);
        NumberFormat nf = builder.getNumberFormat(1);

        CompactDecimalFormat cdf = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
            debugOriginals, CompactStyle.SHORT, locale2, CurrencyStyle.PLAIN, currencyCode);

        Map<String, Map<String, String>> data = BuildIcuCompactDecimalFormat.buildCustomData(cldrFile, CompactStyle.SHORT, CurrencyStyle.PLAIN);
        if (DEBUG) {
            for (Entry<String, Map<String, String>> entry : data.entrySet()) {
                System.out.println(entry);
            }
        }

        CompactDecimalFormat cdfs = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
            debugOriginals, CompactStyle.LONG, locale2, CurrencyStyle.PLAIN, currencyCode);
        CompactDecimalFormat cdfCurr = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors,
            debugOriginals, CompactStyle.SHORT, locale2, CurrencyStyle.CURRENCY, currencyCode);

        Set<Double> allSamples = VerifyCompactNumbers.collectSamplesAndSetFormats(currencyCode, locale, SDI, cdf, cdfs, cdfCurr);

        for (double source : allSamples) {
            if (false && source == 22000000 && locale.equals("cs")) {
                System.out.println("**");
            }

            String formattedNumber = nf.format(source);
            String compactFormattedNumber = cdf == null ? "n/a" : cdf.format(source);
            String compactLongFormattedNumber = cdfs == null ? "n/a" : cdfs.format(source);
            String compactCurrFormattedNumber = cdfs == null ? "n/a" : cdfCurr.format(source);
            if (DEBUG) System.out.println(source
                + "\tnf:\t" + formattedNumber
                + "\tcnf:\t" + compactFormattedNumber
                + "\tclnf:\t" + compactLongFormattedNumber
                + "\tccnf:\t" + compactCurrFormattedNumber
                );
        }
    }
}
