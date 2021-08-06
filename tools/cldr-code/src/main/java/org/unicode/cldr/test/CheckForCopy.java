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
        .add("^//ldml/localeDisplayNames/scripts/script\\[@type=\"(Jamo|Thai|Ahom|Loma|Moon|Newa|Arab|Lisu|Bali|Cham|Modi|Toto)\"]", true)
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

    @Override
    @SuppressWarnings("unused")
    public CheckCLDR handleCheck(String path, String fullPath, String value,
        Options options, List<CheckStatus> result) {

        if (fullPath == null || path == null || value == null) {
            return this; // skip root, and paths that we don't have
        }
        Failure failure = sameAsCodeOrEnglish(value, path, getCldrFileToCheck(), false);
        addFailure(result, failure);
        return this;
    }

    /**
     * Check the given path and value, and return true if it has a same_as_code failure

     * @param value the value
     * @param path the path
     * @param cldrFile the CLDRFile
     * @return true or false
     */
    public static boolean sameAsCode(String value, String path, CLDRFile cldrFile) {
        return sameAsCodeOrEnglish(value, path, cldrFile, true) == Failure.same_as_code;
    }

    /**
     * Check the given path and value for same_as_code and same_as_english failures
     *
     * @param value the value
     * @param path the path
     * @param cldrFile the CLDRFile
     * @param contextIsVoteSubmission true when a new or imported vote is in question, else false
     * @return the Failure object
     */
    private static Failure sameAsCodeOrEnglish(String value, String path, CLDRFile cldrFile, boolean contextIsVoteSubmission) {

        Status status = new Status();

        /*
         * Don't check inherited values unless they are from ^^^
         *
         * In the context of vote submission, we must check inherited values,
         * otherwise nothing prevents voting to inherit the code value.
         *
         * TODO: clarify the purpose of using topStringValue and getConstructedValue here;
         * cf. getConstructedBaileyValue. This code is confusing and warrants explanation.
         * The meaning of "explicit" here seems to be the opposite of its meaning elsewhere.
         */
        String topStringValue = cldrFile.getUnresolved().getStringValue(path);
        final boolean isExplicitBailey = CldrUtility.INHERITANCE_MARKER.equals(topStringValue);
        String loc = cldrFile.getSourceLocaleID(path, status);
        if (!contextIsVoteSubmission && !isExplicitBailey) {
            if (!cldrFile.getLocaleID().equals(loc)
                || !path.equals(status.pathWhereFound)) {
                return Failure.ok;
            }
        }

        /*
         * Since get() may return null here, comparison with Boolean.TRUE prevents NullPointerException.
         */
        if (Boolean.TRUE == skip.get(path)) {
            return Failure.ok;
        }

        Failure failure = Failure.ok;

        CLDRFile di = getDisplayInformation();
        if (di == null) {
            throw new InternalCldrException("CheckForCopy.sameAsCodeOrEnglish error: getDisplayInformation is null");
        }
        String english = di.getStringValue(path);
        if (value.equals(english)) {
            if (ASCII_LETTER.containsSome(english)) {
                failure = Failure.same_as_english;
            }
        }

        /*
         * Check for attributes. May override English test.
         * Since get() may return null here, comparison with Boolean.TRUE prevents NullPointerException.
         */
        if (Boolean.TRUE == SKIP_CODE_CHECK.get(path)) {
            return Failure.ok;
        }
        if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
            value = cldrFile.getConstructedBaileyValue(path, null, null);
            if (value == null) {
                return Failure.ok;
            }
        }
        if (sameAsEnglishOK(loc, path, value)) {
            return Failure.ok;
        }
        String value2 = value;
        if (isExplicitBailey) {
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
                        failure = Failure.same_as_code;
                        break;
                    }
                } catch (NullPointerException e) {
                    throw new ICUException("Value: " + value + "\nattributeValue: " + attributeValue
                        + "\nPath: " + path, e);
                }
            }
        }
        return failure;
    }

    private static boolean sameAsEnglishOK(String loc, String path, String value) {
        if (path.startsWith("//ldml/units/unitLength")
            || path.startsWith("//ldml/characters/parseLenients")) {
            return true;
        }
        if ("en".equals(loc) || loc.startsWith("en_")) {
            if ("year".equals(value) || "month".equals(value) || "day".equals(value) || "hour".equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * If there is a failure, add it to the list
     *
     * @param result the list of CheckStatus objects
     * @param failure the Failure object
     */
    private void addFailure(List<CheckStatus> result, Failure failure) {
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
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {

        if (cldrFileToCheck == null) {
            return this;
        }

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
}
