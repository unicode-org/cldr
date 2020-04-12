package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;

public class CheckForCopy extends FactoryCheckCLDR {

    private static final boolean DEBUG = CldrUtility.getProperty("DEBUG", false);

    public CheckForCopy(Factory factory) {
        super(factory);
    }

    private static final RegexLookup<Boolean> skip = new RegexLookup<Boolean>()
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

    private static final RegexLookup<Boolean> SKIP_CODE_CHECK = new RegexLookup<Boolean>()
        .add("^//ldml/characterLabels/characterLabel", true)
        .add("^//ldml/dates/fields/field\\[@type=\"(era|week|minute|quarter|second)\"]/displayName", true)
        .add("^//ldml/localeDisplayNames/scripts/script\\[@type=\"(Jamo|Thai|Ahom|Loma|Moon|Newa|Arab|Lisu|Bali|Cham|Modi)\"]", true)   
        .add("^//ldml/localeDisplayNames/languages/language\\[@type=\"(fon|gan|luo|tiv|yao|vai)\"]", true)   
        .add("^//ldml/dates/timeZoneNames/metazone\\[@type=\"GMT\"]", true)  
        .add("^//ldml/localeDisplayNames/territories/territory\\[@type=\"[^\"]*+\"]\\[@alt=\"short\"]", true)  
        .add("^//ldml/localeDisplayNames/measurementSystemNames/measurementSystemName", true)  
        .add("^//ldml/localeDisplayNames/types/type\\[@key=\"collation\"]\\[@type=\"standard\"]", true)  
        ;

    static UnicodeSet ASCII_LETTER = new UnicodeSet("[a-zA-Z]").freeze();

    enum Failure {
        ok, same_as_english, same_as_code
    }

    @SuppressWarnings("unused")
    public CheckCLDR handleCheck(String path, String fullPath, String value,
        Options options, List<CheckStatus> result) {

        if (fullPath == null || path == null || value == null) {
            return this; // skip root, and paths that we don't have
        }

        Status status = new Status();

        if (Boolean.TRUE == skip.get(path)) {
            return this;
        }

        Failure failure = Failure.ok;

        CLDRFile di = getDisplayInformation();
        if (di == null) {
            throw new InternalCldrException("CheckForCopy.handleCheck error: getDisplayInformation is null");
        }
        String english = di.getStringValue(path);
        if (value.equals(english)) {
            if (ASCII_LETTER.containsSome(english)) {
                failure = Failure.same_as_english;
            }
        }

        // Check for attributes.
        // May override English test
        if (Boolean.TRUE != SKIP_CODE_CHECK.get(path)) {
            String value2 = value;
            /*
             * TODO: clarify the purpose of using topStringValue and getConstructedValue here;
             * maybe related to getConstructedBaileyValue
             */
            String topStringValue = getCldrFileToCheck().getUnresolved().getStringValue(path);
            if (CldrUtility.INHERITANCE_MARKER.equals(topStringValue)) {
                value2 = getCldrFileToCheck().getConstructedValue(path);
                if (value2 == null) { // no special constructed value
                    value2 = value;
                }
            }

            XPathParts parts = XPathParts.getFrozenInstance(path);

            int elementCount = parts.size();
            for (int i = 2; i < elementCount; ++i) {
                Map<String, String> attributes = parts.getAttributes(i);
                for (Entry<String, String> attributeEntry : attributes.entrySet()) {
                    final String attributeValue = attributeEntry.getValue();
                    try {
                        if (value2.equals(attributeValue)) {
                            failure = Failure.same_as_code;
                            break;
                        }
                    } catch (NullPointerException e) {
                        throw new ICUException("Value: " + value + "\nattributeValue: " + attributeValue
                            + "\nPath: " + path, e);
                    }                
                }
            }
        }

        switch (failure) {
        case same_as_english:
            result
            .add(new CheckStatus()
                .setCause(this)
                .setMainType(CheckStatus.warningType)
                .setSubtype(Subtype.sameAsEnglish)
                .setCheckOnSubmit(false)
                .setMessage(
                    "The value is the same as in English: see <a target='CLDR-ST-DOCS' href='http://cldr.org/translation/fixing-errors'>Fixing Errors and Warnings</a>.",
                    new Object[] {}));
            break;
        case same_as_code:
            result
            .add(new CheckStatus()
                .setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.sameAsCode)
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

        setSkipTest(false);        
        if (lang.equals("en") || localeID.equals("root")) {// || exemplars != null && ASCII_LETTER.containsNone(exemplars)) {
            setSkipTest(true);
            if (DEBUG) {
                System.out.println("# CheckForCopy: Skipping: " + localeID);
            }
            return this;
        }

        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        return this;
    }

    public static boolean sameAsCode(String value, String path, CLDRFile cldrFile) {

        if (SKIP_CODE_CHECK.get(path)) {
            return false;
        }
        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
            value = cldrFile.getConstructedBaileyValue(path, null, null);
        }
        String value2 = value;
        /*
         * TODO: clarify the purpose of using topStringValue and getConstructedValue here;
         * maybe related to getConstructedBaileyValue
         */
        String topStringValue = cldrFile.getUnresolved().getStringValue(path);
        if (CldrUtility.INHERITANCE_MARKER.equals(topStringValue)) {
            value2 = cldrFile.getConstructedValue(path);
            if (value2 == null) { // no special constructed value
                value2 = value;
            }
        }

        XPathParts parts = XPathParts.getFrozenInstance(path);

        int elementCount = parts.size();
        for (int i = 2; i < elementCount; ++i) {
            Map<String, String> attributes = parts.getAttributes(i);
            for (Entry<String, String> attributeEntry : attributes.entrySet()) {
                final String attributeValue = attributeEntry.getValue();
                try {
                    if (value2.equals(attributeValue)) {
                        return true;
                    }
                } catch (NullPointerException e) {
                    throw new ICUException("Value: " + value + "\nattributeValue: " + attributeValue
                        + "\nPath: " + path, e);
                }
            }
        }
        return false;
    }
}
