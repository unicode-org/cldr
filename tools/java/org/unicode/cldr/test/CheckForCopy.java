package org.unicode.cldr.test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

public class CheckForCopy extends FactoryCheckCLDR {

    private static final boolean DEBUG = CldrUtility.getProperty("DEBUG", false);

    public CheckForCopy(Factory factory) {
        super(factory);
    }

    static RegexLookup<Boolean> skip = new RegexLookup<Boolean>()
        .add("/(availableFormats" +
            "|exponential" +
            "|nan" +
            "|availableFormats" +
            "|intervalFormatItem" +
            "|exemplarCharacters\\[@type=\"(currencySymbol|index)\"]" +
            "|scientificFormat" +
            "|timeZoneNames/(hourFormat|gmtFormat|gmtZeroFormat)" +
            "|dayPeriod" +
            "|(monthWidth|dayWidth|quarterWidth)\\[@type=\"(narrow|abbreviated)\"]" +
            "|exemplarCity" +
            "|localeDisplayNames/(scripts|territories)" +
            "|currency\\[@type=\"[A-Z]+\"]/symbol" +
            "|pattern" +
            "|field\\[@type=\"dayperiod\"]" +
            "|defaultNumberingSystem" +
            "|otherNumberingSystems" +
            "|exemplarCharacters" +
            "|durationUnitPattern" +
            "|coordinateUnitPattern" +
            "|unitLength\\[@type=\"(short|narrow)\"\\]/unit\\[@type=\"[^\"]++\"\\]/unitPattern\\[@count=\"[^\"]++\"\\]" +
            "|unitLength\\[@type=\"(short|narrow)\"\\]/unit\\[@type=\"[^\"]++\"\\]/perUnitPattern" +
            ")", true)
        .add("^//ldml/dates/calendars/calendar\\[@type=\"gregorian\"]", false)
        .add("^//ldml/dates/calendars/calendar", true);

    static Set<String> SKIP_TYPES = Builder
        .with(new HashSet<String>())
        .addAll(
            "CHF", "EUR", "XPD",
            "Vaii", "Yiii", "Thai",
            "SAAHO", "BOONT", "SCOUSE",
            "fon", "ijo", "luo", "tiv", "yao", "zu", "zza", "tw", "ur", "vo", "ha", "hi", "ig", "yo", "ak", "vai",
            "eo", "af",
            "Cuba",
            // languages that are the same in English as in themselves
            // and countries that have the same name as English in one of their official languages.
            "af", // Afrikaans
            "ak", // Akan
            "AD", // Andorra
            "LI", // Liechtenstein
            "NA", // Namibia
            "AR", // Argentina
            "CO", // Colombia
            "VE", // Venezuela
            "CL", // Chile
            "CU", // Cuba
            "EC", // Ecuador
            "GT", // Guatemala
            "BO", // Bolivia
            "HN", // Honduras
            "SV", // El Salvador
            "CR", // Costa Rica
            "PR", // Puerto Rico
            "NI", // Nicaragua
            "UY", // Uruguay
            "PY", // Paraguay
            "fil", // Filipino
            "FR", // France
            "MG", // Madagascar
            "CA", // Canada
            "CI", // Côte d’Ivoire
            "BI", // Burundi
            "ML", // Mali
            "TG", // Togo
            "NE", // Niger
            "BF", // Burkina Faso
            "RE", // Réunion
            "GA", // Gabon
            "LU", // Luxembourg
            "MQ", // Martinique
            "GP", // Guadeloupe
            "YT", // Mayotte
            "VU", // Vanuatu
            "SC", // Seychelles
            "MC", // Monaco
            "DJ", // Djibouti
            "RW", // Rwanda
            "ha", // Hausa
            "ID", // Indonesia
            "ig", // Igbo
            "NG", // Nigeria
            "SM", // San Marino
            "kln", // Kalenjin
            "mg", // Malagasy
            "MY", // Malaysia
            "BN", // Brunei
            "MT", // Malta
            "ZW", // Zimbabwe
            "SR", // Suriname
            "AW", // Aruba
            "PT", // Portugal
            "AO", // Angola
            "TL", // Timor-Leste
            "RS", // Serbia
            "rw", // Kinyarwanda
            "RW", // Rwanda
            "ZW", // Zimbabwe
            "FI", // Finland
            "TZ", // Tanzania
            "KE", // Kenya
            "UG", // Uganda
            "TO", // Tonga
            "wae", // Walser
            "metric")
        .freeze();

    static UnicodeSet ASCII_LETTER = new UnicodeSet("[a-zA-Z]");

    enum Failure {
        ok, same_as_english, same_as_code
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value,
        Options options, List<CheckStatus> result) {

        if (fullPath == null || value == null) return this; // skip paths that we don't have

        Status status = new Status();

        String loc = getCldrFileToCheck().getSourceLocaleID(path, status);
        if (!getCldrFileToCheck().getLocaleID().equals(loc) || !path.equals(status.pathWhereFound)) {
            return this;
        }

        if (Boolean.TRUE == skip.get(path)) {
            return this;
        }

        Failure failure = Failure.ok;

        String english = getDisplayInformation().getStringValue(path);
        if (CharSequences.equals(english, value)) {
            if (ASCII_LETTER.containsSome(english)) {
                failure = Failure.same_as_english;
            }
        }

        // Check for attributes.
        // May override English test
        XPathParts parts = XPathParts.getTestInstance(path);
        int elementCount = parts.size();
        for (int i = 2; i < elementCount; ++i) {
            Map<String, String> attributes = parts.getAttributes(i);
            for (Entry<String, String> attributeEntry : attributes.entrySet()) {
                final String attributeValue = attributeEntry.getValue();
                if (SKIP_TYPES.contains(attributeValue)) {
                    failure = Failure.ok; // override English test
                    break;
                }
                try {
                    if (value.equals(attributeValue)) {
                        failure = Failure.same_as_code;
                        break;
                    }
                } catch (NullPointerException e) {
                    throw new ICUException("Value: " + value + "\nattributeValue: " + attributeValue
                        + "\nPath: " + path, e);
                }
            }
        }

        switch (failure) {
        case same_as_english:
            result
                .add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.sameAsEnglishOrCode)
                    .setCheckOnSubmit(false)
                    .setMessage(
                        "The value is the same as in English: see <a target='CLDR-ST-DOCS' href='http://cldr.org/translation/fixing-errors'>Fixing Errors and Warnings</a>.",
                        new Object[] {}));
            break;
        case same_as_code:
            result
                .add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.sameAsEnglishOrCode)
                    .setCheckOnSubmit(false)
                    .setMessage(
                        "The value is the same as the 'code': see <a target='CLDR-ST-DOCS' href='http://cldr.org/translation/fixing-errors'>Fixing Errors and Warnings</a>.",
                        new Object[] {}));
            break;
        default:
        }
        return this;
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;

        final String localeID = cldrFileToCheck.getLocaleID();
        LanguageTagParser ltp = new LanguageTagParser().set(localeID);
        String lang = ltp.getLanguage();
        UnicodeSet exemplars = cldrFileToCheck.getExemplarSet("main", CLDRFile.WinningChoice.WINNING);
        /*
         * skip non-Latin, because the exemplar set will check
         */
        if (lang.equals("en") || lang.equals("root") || exemplars != null && ASCII_LETTER.containsNone(exemplars)) {
            setSkipTest(true);
            if (DEBUG) {
                System.out.println("CheckForCopy: Skipping: " + localeID);
            }
            return this;
        }

        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        return this;
    }
}
