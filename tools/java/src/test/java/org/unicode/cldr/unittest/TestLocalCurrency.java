package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;

import com.ibm.icu.dev.test.TestFmwk;

public class TestLocalCurrency extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestLocalCurrency().run(args);
    }

    private int maxLocalizedSymbols(String region) {
        final List<String> regionsWithTwoCurrencySymbols = Arrays.asList("AE", "AZ",
            "BA", "CN", "DZ", "ET", "IQ", "IR", "LK", "KM", "MA", "MR", "MK", "PK",
            "RS", "SD", "SY", "TN", "UZ");
        if (regionsWithTwoCurrencySymbols.contains(region)) {
            return 2;
        }
        return 1;
    }

    public void TestConsistency() {

        LanguageTagParser ltp = new LanguageTagParser();
        SupplementalDataInfo supplementalDataInfo = testInfo
            .getSupplementalDataInfo();
        Map<String, String> localeToLocalCurrencySymbol = new HashMap<String, String>();
        Map<String, Set<String>> localizedCurrencySymbols = new HashMap<String, Set<String>>();
        Map<String, Set<String>> regionToLocales = new HashMap<String, Set<String>>();

        List<String> nonLocalizedOK = Arrays.asList("AED", "AZN", "CHF", "CVE", "GEL",
            "HRK", "HUF", "IQD", "IRR", "ISK", "KPW", "LTL", "MAD", "MDL", "RON", "RSD",
            "SDG", "THB", "TMT");

        Factory factory = testInfo.getCldrFactory();
        Date now = new Date();
        for (String locale : factory.getAvailable()) {
            ltp.set(locale);
            String region = ltp.getRegion();
            if (region == null || region.isEmpty() || region.length() != 2) {
                continue;
            }
            CLDRFile localeData = testInfo.getCLDRFile(locale, true);
            String localCurrency = null;
            Set<CurrencyDateInfo> targetCurrencyInfo = supplementalDataInfo
                .getCurrencyDateInfo(region);

            for (CurrencyDateInfo cdi : targetCurrencyInfo) {
                if (cdi.getStart().before(now) && cdi.getEnd().after(now)
                    && cdi.isLegalTender()) {
                    localCurrency = cdi.getCurrency();
                    break;
                }
            }

            if (localCurrency == null) {
                errln("No current legal tender currency for locale: " + locale);
                continue;
            } else {
                logln("Testing currency: " + localCurrency + " for locale: "
                    + locale);
            }

            String checkPath = "//ldml/numbers/currencies/currency[@type=\""
                + localCurrency + "\"]/symbol";
            String localCurrencySymbol = localeData.getWinningValue(checkPath);
            localeToLocalCurrencySymbol.put(locale, localCurrencySymbol);

            Set<String> localSymbols = localizedCurrencySymbols.get(region);
            if (localSymbols == null) {
                localSymbols = new TreeSet<String>();
            }

            if (localCurrencySymbol.equals(localCurrency)
                && !nonLocalizedOK.contains(localCurrency)) {
                    errln("Currency symbol " + localCurrencySymbol + " for locale "
                        + locale + " is not localized.");
            }

            localSymbols.add(localCurrencySymbol);
            localizedCurrencySymbols.put(region, localSymbols);

            Set<String> regionLocales = regionToLocales.get(region);
            if (regionLocales == null) {
                regionLocales = new TreeSet<String>();
            }

            regionLocales.add(locale);
            regionToLocales.put(region, regionLocales);

        }

        for (String region : localizedCurrencySymbols.keySet()) {
            Set<String> symbols = localizedCurrencySymbols.get(region);
            if (symbols.size() > maxLocalizedSymbols(region)) {
                StringBuffer errmsg = new StringBuffer();
                errmsg.append("Too many localized currency symbols for region: "
                    + region + "\n");
                for (String locale : regionToLocales.get(region)) {
                    errmsg.append("\t\tLocale: " + locale);
                    errmsg.append(" Symbol: "
                        + localeToLocalCurrencySymbol.get(locale));
                    errmsg.append('\n');
                }
                errln(errmsg.toString());
            }
        }
    }
}
