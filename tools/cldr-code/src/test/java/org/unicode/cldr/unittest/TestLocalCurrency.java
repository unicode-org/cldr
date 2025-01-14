package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;

public class TestLocalCurrency extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestLocalCurrency().run(args);
    }

    static final Set<String> regionsWithTwoCurrencySymbols =
            ImmutableSet.of(
                    "AE", "AZ", "BA", "CA", "CN", "DZ", "ET", "IQ", "IR", "LK", "KM", "MA", "MR",
                    "MK", "PK", "NE", "GW", "CM", "SN",
                    "BF", // << ff_Adlm regions are multiscript, Latn+Adlm
                    "RS", "SD", "SY", "TN", "UZ");

    private int maxLocalizedSymbols(String region) {
        if (regionsWithTwoCurrencySymbols.contains(region)) {
            return 2;
        }
        return 1;
    }

    public void TestConsistency() {

        LanguageTagParser ltp = new LanguageTagParser();
        SupplementalDataInfo supplementalDataInfo = testInfo.getSupplementalDataInfo();
        Map<String, String> localeToLocalCurrencySymbol = new HashMap<>();
        Map<String, Set<String>> localizedCurrencySymbols = new HashMap<>();
        Map<String, Set<String>> regionToLocales = new HashMap<>();

        List<String> nonLocalizedOK =
                Arrays.asList(
                        "AED", "AZN", "CHF", "CVE", "GEL", "HRK", "HUF", "IQD", "IRR", "ISK", "KPW",
                        "LTL", "MAD", "MDL", "RON", "RSD", "SDG", "THB", "TMT");

        Factory factory = testInfo.getCldrFactory();
        Date now = new Date();
        for (String locale : factory.getAvailable()) {
            if (!StandardCodes.isLocaleAtLeastBasic(locale)) {
                continue;
            }
            ltp.set(locale);
            String region = ltp.getRegion();
            if (region == null || region.isEmpty() || region.length() != 2) {
                continue;
            }
            CLDRFile localeData = testInfo.getCLDRFile(locale, true);
            String localCurrency = null;
            Set<CurrencyDateInfo> targetCurrencyInfo =
                    supplementalDataInfo.getCurrencyDateInfo(region);

            for (CurrencyDateInfo cdi : targetCurrencyInfo) {
                if (cdi.getStart().before(now) && cdi.getEnd().after(now) && cdi.isLegalTender()) {
                    localCurrency = cdi.getCurrency();
                    break;
                }
            }

            if (localCurrency == null) {
                errln("No current legal tender currency for locale: " + locale);
                continue;
            } else {
                logln("Testing currency: " + localCurrency + " for locale: " + locale);
            }

            String checkPath =
                    "//ldml/numbers/currencies/currency[@type=\"" + localCurrency + "\"]/symbol";
            String localCurrencySymbol = localeData.getWinningValue(checkPath);
            localeToLocalCurrencySymbol.put(locale, localCurrencySymbol);

            Set<String> localSymbols = localizedCurrencySymbols.get(region);
            if (localSymbols == null) {
                localSymbols = new TreeSet<>();
            }

            if (localCurrencySymbol.equals(localCurrency)
                    && !nonLocalizedOK.contains(localCurrency)) {
                errln(
                        "Currency symbol "
                                + localCurrencySymbol
                                + " for locale "
                                + locale
                                + " is not localized.");
            }

            localSymbols.add(localCurrencySymbol);
            localizedCurrencySymbols.put(region, localSymbols);

            Set<String> regionLocales = regionToLocales.get(region);
            if (regionLocales == null) {
                regionLocales = new TreeSet<>();
            }

            regionLocales.add(locale);
            regionToLocales.put(region, regionLocales);
        }

        for (String region : localizedCurrencySymbols.keySet()) {
            Set<String> symbols = localizedCurrencySymbols.get(region);
            if (symbols.size() > maxLocalizedSymbols(region)) {
                StringBuffer errmsg = new StringBuffer();
                errmsg.append("Too many localized currency symbols for region: " + region + "\n");
                for (String locale : regionToLocales.get(region)) {
                    errmsg.append("\t\tLocale: " + locale);
                    errmsg.append(" Symbol: " + localeToLocalCurrencySymbol.get(locale));
                    errmsg.append('\n');
                }
                errln(errmsg.toString()); // if this fails, see if it warrants changing
                // regionsWithTwoCurrencySymbols.
            }
        }
    }
}
