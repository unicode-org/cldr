package org.unicode.cldr.test;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.test.DisplayAndInputProcessor.NumericType;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.NameType;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.Patterns;
import org.unicode.cldr.util.PluralRulesUtil;
import org.unicode.cldr.util.Splitters;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XPathParts;

public class CheckNumbers extends FactoryCheckCLDR {
    private static final Set<String> SKIP_TIME_SEPARATOR = ImmutableSet.of("nds", "fr_CA");

    private static final UnicodeSet FORBIDDEN_NUMERIC_PATTERN_CHARS = new UnicodeSet("[[:n:]-[0]]");

    private static final boolean EXTENDED_CURRENCY_CHECKS =
            System.getProperty("EXTENDED_CURRENCY_CHECKS") != null;

    /**
     * If you are going to use ICU services, then ICUServiceBuilder will allow you to create them
     * entirely from CLDR data, without using the ICU data.
     */
    private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();

    private Set<Count> pluralTypes;
    private Map<Count, Set<Double>> pluralExamples;
    private Set<String> validNumberingSystems;

    private String defaultNumberingSystem;
    private String defaultTimeSeparatorPath;
    private String patternForHm;

    /** A number formatter used to show the English format for comparison. */
    private static NumberFormat english = NumberFormat.getNumberInstance(ULocale.ENGLISH);

    static {
        english.setMaximumFractionDigits(5);
    }

    /** Providing random numbers for some of the tests */
    private static Random random = new Random();

    private static Pattern ALLOWED_INTEGER = PatternCache.get("1(0+)");
    private static Pattern COMMA_ABUSE = PatternCache.get(",[0#]([^0#]|$)");

    /**
     * A MessageFormat string. For display, anything variable that contains strings that might have
     * BIDI characters in them needs to be surrounded by \u200E.
     */
    static String SampleList = "{0} \u2192 \u201C\u200E{1}\u200E\u201D \u2192 {2}";

    /** Special flag for POSIX locale. */
    boolean isPOSIX;

    public CheckNumbers(Factory factory) {
        super(factory);
    }

    /**
     * Whenever your test needs initialization, override setCldrFileToCheck. It is called for each
     * new file needing testing. The first two lines will always be the same; checking for null, and
     * calling the super.
     */
    @Override
    public CheckCLDR handleSetCldrFileToCheck(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.handleSetCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        icuServiceBuilder.setCldrFile(getResolvedCldrFileToCheck());
        isPOSIX = cldrFileToCheck.getLocaleID().indexOf("POSIX") >= 0;
        SupplementalDataInfo supplementalData =
                SupplementalDataInfo.getInstance(getFactory().getSupplementalDirectory());
        PluralInfo pluralInfo =
                supplementalData.getPlurals(PluralType.cardinal, cldrFileToCheck.getLocaleID());
        pluralTypes = pluralInfo.getCounts();
        pluralExamples = pluralInfo.getCountToExamplesMap();
        validNumberingSystems = supplementalData.getNumberingSystems();

        CLDRFile resolvedFile = getResolvedCldrFileToCheck();
        defaultNumberingSystem =
                resolvedFile.getWinningValue("//ldml/numbers/defaultNumberingSystem");
        if (defaultNumberingSystem == null
                || !validNumberingSystems.contains(defaultNumberingSystem)) {
            defaultNumberingSystem = "latn";
        }
        defaultTimeSeparatorPath =
                "//ldml/numbers/symbols[@numberSystem=\""
                        + defaultNumberingSystem
                        + "\"]/timeSeparator";
        // Note for the above, an actual time separator path may add the following after the above:
        // [@alt='...'] and/or [@draft='...']
        // Ideally we would get the following for default calendar, here we just use gregorian;
        // probably OK
        patternForHm =
                resolvedFile.getWinningValue(
                        "//ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='Hm']");

        return this;
    }

    /**
     * This is the method that does the check. Notice that for performance, you should try to exit
     * as fast as possible except where the path is one that you are testing.
     */
    @Override
    public CheckCLDR handleCheck(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {

        if (fullPath == null || value == null) return this; // skip paths that we don't have

        // TODO: could exclude more paths
        if (!accept(result)) return this;

        // Do a quick check on the currencyMatch, to make sure that it is a proper UnicodeSet
        if (path.indexOf("/currencyMatch") >= 0) {
            try {
                new UnicodeSet(value);
            } catch (Exception e) {
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.invalidCurrencyMatchSet)
                                .setMessage(
                                        "Error in creating UnicodeSet {0}; {1}; {2}",
                                        new Object[] {value, e.getClass().getName(), e}));
            }
            return this;
        }

        if (path.indexOf("/minimumGroupingDigits") >= 0) {
            try {
                int mgd = Integer.parseInt(value);
                if (!CldrUtility.DIGITS.contains(value)) {
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.badMinimumGroupingDigits)
                                    .setMessage(
                                            "Minimum grouping digits can only contain Western digits [0-9]."));
                } else {
                    if (mgd > 4) {
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(CheckStatus.errorType)
                                        .setSubtype(Subtype.badMinimumGroupingDigits)
                                        .setMessage(
                                                "Minimum grouping digits cannot be greater than 4."));

                    } else if (mgd < 1) {
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(CheckStatus.errorType)
                                        .setSubtype(Subtype.badMinimumGroupingDigits)
                                        .setMessage(
                                                "Minimum grouping digits cannot be less than 1."));

                    } else if (mgd > 2) {
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(CheckStatus.warningType)
                                        .setSubtype(Subtype.badMinimumGroupingDigits)
                                        .setMessage(
                                                "Minimum grouping digits > 2 is rare. Please double check this."));
                    }
                }
            } catch (NumberFormatException e) {
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.badMinimumGroupingDigits)
                                .setMessage("Minimum grouping digits must be a numeric value."));
            }
            return this;
        }

        if (path.indexOf("defaultNumberingSystem") >= 0
                || path.indexOf("otherNumberingSystems") >= 0) {
            if (!validNumberingSystems.contains(value)) {
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.illegalNumberingSystem)
                                .setMessage("Invalid numbering system: " + value));
            }
        }

        if (path.contains(defaultTimeSeparatorPath) && !path.contains("[@alt=") && value != null) {
            // timeSeparator for default numbering system should be in availableFormats Hm item
            if (patternForHm != null && !patternForHm.contains(value)) {
                // Should be fixed to not require hack, see #11833
                if (!SKIP_TIME_SEPARATOR.contains(getCldrFileToCheck().getLocaleID())) {
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.invalidSymbol)
                                    .setMessage(
                                            "Invalid timeSeparator: "
                                                    + value
                                                    + "; must match what is used in Hm time pattern: "
                                                    + patternForHm));
                }
            }
        }

        // Check that symbols are for an explicit number system;
        // note that symbols are skipped for checks after this point.
        if (path.indexOf("/symbols") >= 0) {
            XPathParts symbolParts = XPathParts.getFrozenInstance(path);
            if (symbolParts.getAttributeValue(2, "numberSystem") == null) {
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.missingNumberingSystem)
                                .setMessage(
                                        "Symbols must have an explicit numberSystem attribute."));
            }
        }

        // quick bail from all other cases (including symbols)
        NumericType type = NumericType.getNumericType(path);
        if (type == NumericType.NOT_NUMERIC || type == NumericType.RATIONAL) {
            return this; // skip
        }
        XPathParts parts = XPathParts.getFrozenInstance(path);

        // TODO: re-enable this commented-out check
        // Reference: https://unicode-org.atlassian.net/browse/CLDR-18722
        /*
        // Check that number formats are for an explicit number system.
        String numberSystem = parts.getAttributeValue(2, "numberSystem");
        if (numberSystem == null) {
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.missingNumberingSystem)
                            .setMessage(
                                    "Number formats must have an explicit numberSystem attribute."));
        }
         */

        // Do tests that need to split the values

        boolean isPositive = true;
        List<String> splitValue = Splitters.SEMI.splitToList(value);
        for (String patternPart : splitValue) {
            if (!isPositive && !"accounting".equals(parts.getAttributeValue(-2, "type"))) {
                // must contain the minus sign if not accounting.
                // String numberSystem = parts.getAttributeValue(2, "numberSystem");
                // String minusSign = "-"; // icuServiceBuilder.getMinusSign(numberSystem == null ?
                // "latn" : numberSystem);
                if (patternPart.indexOf('-') < 0)
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.missingMinusSign)
                                    .setMessage(
                                            "Negative format must contain ASCII minus sign (-)."));
            }
            // Make sure currency patterns contain a currency symbol
            if (type == NumericType.CURRENCY
                    || type == NumericType.CURRENCY_ABBREVIATED && !value.equals("0")) {
                if (path.contains("noCurrency")) {
                    if (patternPart.indexOf("\u00a4") >= 0) {
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(CheckStatus.errorType)
                                        .setSubtype(Subtype.currencyPatternUnexpectedCurrencySymbol)
                                        .setMessage(
                                                "noCurrency formatting pattern must not contain the currency placeholder (\u00a4)."));
                    }
                } else if (patternPart.indexOf("\u00a4") < 0) {
                    // check for compact format
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.currencyPatternMissingCurrencySymbol)
                                    .setMessage(
                                            "Currency formatting pattern must contain a currency symbol."));
                }
            }

            // Make sure percent formatting patterns contain a percent symbol, in each part
            if (type == NumericType.PERCENT) {
                if (patternPart.indexOf("%") < 0)
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.percentPatternMissingPercentSymbol)
                                    .setMessage(
                                            "Percentage formatting pattern must contain a % symbol."));
            }
            isPositive = false;
        }

        // check for compatible patterns
        if (EXTENDED_CURRENCY_CHECKS) {

            if (splitValue.size() > 1
                    && (type == NumericType.CURRENCY_ABBREVIATED
                            || type == NumericType.DECIMAL_ABBREVIATED)) {
                Matcher matcher1 = Patterns.PATTERN_ZEROS.matcher(splitValue.get(0));
                Matcher matcher2 = Patterns.PATTERN_ZEROS.matcher(splitValue.get(1));
                String zeros1 = matcher1.find() ? matcher1.group() : "";
                String zeros2 = matcher2.find() ? matcher2.group() : "";
                if (!zeros1.equals(zeros2)) {
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.inconsistentPositiveAndNegativePatterns)
                                    .setMessage(
                                            "The positive pattern has {0} while the negative has {1}",
                                            zeros1, zeros2));
                }
            }

            if (type == NumericType.CURRENCY
                    || type == NumericType.CURRENCY_ABBREVIATED && !value.equals("0")) {
                String altValue = parts.getAttributeValue(-1, "alt");
                if (altValue != null) {
                    String plainPath = parts.cloneAsThawed().removeAttribute(-1, "alt").toString();
                    // Check to make sure at least one of the paths has a "real" value.
                    // That avoids errors in inherited number patterns (eg in Adlam digits)
                    if (getResolvedCldrFileToCheck().isHere(plainPath)
                            || getResolvedCldrFileToCheck().isHere(path)) {
                        String plainValue = getResolvedCldrFileToCheck().getStringValue(plainPath);
                        switch (altValue) {
                            case "noCurrency":
                                // remove \u00a4 and spaces around it
                                String noCurrency =
                                        removeCurrencyPlaceholderAndWhitespace(plainValue);
                                String normalizedNoCurrency = normalizeNumberPattern(noCurrency);
                                if (!normalizedNoCurrency.equals(value)
                                        && !noCurrency.equals(value)) {
                                    PathHeader ph = getPathHeaderFactory().fromPath(plainPath);
                                    String linked = getLinkedCode(ph);
                                    result.add(
                                            new CheckStatus()
                                                    .setCause(this)
                                                    .setMainType(CheckStatus.warningType)
                                                    .setSubtype(Subtype.inconsistentCurrencyPattern)
                                                    .setMessage(
                                                            "«{3}» instead would be consistent with {0} «{2}». Fix one or the other.",
                                                            linked,
                                                            ph.getCode(),
                                                            plainValue,
                                                            normalizedNoCurrency));
                                }
                                break;
                            case "alphaNextToNumber":
                                // It should only add spaces to the plain value
                                if (!value.equals(plainValue)
                                        && !removeWhitespaceAroundCurrencyPlaceholder(value)
                                                .equals(plainValue)) {
                                    PathHeader ph = PathHeader.getFactory().fromPath(plainPath);
                                    String linked = getLinkedCode(ph);

                                    // a common case is currency placeholder on the wrong side
                                    int valueCurrencyPosition =
                                            value.indexOf(Patterns.CURRENCY_PLACEHOLDER);
                                    int plainValueCurrencyPosition =
                                            plainValue.indexOf(Patterns.CURRENCY_PLACEHOLDER);
                                    if ((valueCurrencyPosition == 0)
                                            != (plainValueCurrencyPosition == 0)) {
                                        result.add(
                                                new CheckStatus()
                                                        .setCause(this)
                                                        .setMainType(CheckStatus.warningType)
                                                        .setSubtype(
                                                                Subtype.inconsistentCurrencyPattern)
                                                        .setMessage(
                                                                "This has the currency on the wrong side, compared to {0} «{1}».",
                                                                linked, plainValue));

                                    } else {
                                        result.add(
                                                new CheckStatus()
                                                        .setCause(this)
                                                        .setMainType(
                                                                CheckStatus
                                                                        .warningType) // errorButWarningIfJustWhitespace(value, plainValue)
                                                        .setSubtype(
                                                                Subtype.inconsistentCurrencyPattern)
                                                        .setMessage(
                                                                "This should only add spacing to {0} «{1}» or be identical.",
                                                                linked, plainValue));
                                    }
                                }
                                break;
                        }
                    }
                }
            }

            if (type == NumericType.CURRENCY_ABBREVIATED) {
                // take
                // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="1000"][@count="one"]
                // and compare to:
                // ldml/numbers/decimalFormats[@numberSystem="latn"]/decimalFormatLength[@type="short"]/decimalFormat[@type="standard"]/pattern[@type="1000"][@count="one"]

                // ldml/numbers/decimalFormats[@numberSystem="latn"]/decimalFormatLength[@type="short"]/decimalFormat[@type="standard"]/pattern[@type="1000"][@count="one"]

                String plainPath =
                        parts.cloneAsThawed()
                                .setElement(2, "decimalFormats")
                                .setElement(3, "decimalFormatLength")
                                .setElement(4, "decimalFormat")
                                .toString();
                // Check to make sure at least one of the paths has a "real" value.
                // That avoids errors in inherited number patterns (eg in Adlam digits)
                if (getResolvedCldrFileToCheck().isHere(plainPath)
                        || getResolvedCldrFileToCheck().isHere(path)) {
                    String plainValue = getResolvedCldrFileToCheck().getStringValue(plainPath);
                    // remove \u00a4 and spaces around it
                    String noCurrency = removeCurrencyPlaceholderAndWhitespace(value);
                    String normalizedNoCurrency = normalizeNumberPattern(noCurrency);
                    if (!normalizedNoCurrency.equals(plainValue)
                            && !noCurrency.equals(plainValue)) {
                        PathHeader ph = getPathHeaderFactory().fromPath(plainPath);
                        String linked = getLinkedCode(ph);
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(CheckStatus.warningType)
                                        .setSubtype(Subtype.inconsistentCurrencyPattern)
                                        .setMessage(
                                                "«{3}» instead would be consistent with {0} «{2}». Fix one or the other.",
                                                linked,
                                                ph.getCode(),
                                                plainValue,
                                                normalizedNoCurrency));
                    }
                }
            }

            // Check that formats are consistent within the same groupings, eg 0T 00T 000T
            // The groupings are different according to the type of language: #,##0 (eg en),
            // #,##,##0
            // (eg hi), and #,###0 (eg ja)
            if (type == NumericType.CURRENCY_ABBREVIATED
                    || type == NumericType.DECIMAL_ABBREVIATED && !value.equals("0")) {
                String pluralCategory = parts.getAttributeValue(-1, "count");
                String length = parts.getAttributeValue(3, "type");
                boolean isLong = "long".equals(length);

                // we look at the value, and if there is more than one 0, we compare to the value
                // for
                // the path with one fewer count.
                // example:
                // ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="10000000"][@count="other"]
                // ldml/numbers/decimalFormats[@numberSystem="latn"]/decimalFormatLength[@type="long"]/decimalFormat[@type="standard"]/pattern[@type="1000"][@count="one"]
                // ldml/numbers/decimalFormats[@numberSystem="latn"]/decimalFormatLength[@type="short"]/decimalFormat[@type="standard"]/pattern[@type="1000"][@count="one"]
                // value: ¤0000万

                Matcher matcher = Patterns.PATTERN_ZEROS.matcher(value);
                String group;
                if (matcher.find() // only if found, and
                        && (group = matcher.group()).length() > 1) {
                    String count = parts.getAttributeValue(-1, "type");
                    String lower = count.substring(0, count.length() - 1);
                    XPathParts lowerParts = parts.cloneAsThawed();
                    lowerParts.putAttributeValue(-1, "type", lower);
                    String lowerPath = lowerParts.toString();
                    String lowerValue = getResolvedCldrFileToCheck().getStringValue(lowerPath);
                    String lowerGroup = group.substring(0, group.length() - 1);
                    String expectedLowerValue = matcher.replaceAll(lowerGroup);
                    if (!lowerValue.equals(expectedLowerValue)) {
                        String expectedValue =
                                Patterns.PATTERN_ZEROS
                                        .matcher(lowerValue)
                                        .replaceAll(lowerGroup + "0");
                        PathHeader ph = getPathHeaderFactory().fromPath(lowerPath);
                        String linkedLowerCode = getLinkedCode(ph);
                        // for now, long ones just get warnings
                        Type mainType =
                                isLong
                                        ? Type.Warning
                                        : errorButWarningIfJustWhitespace(
                                                value, expectedLowerValue);
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(mainType)
                                        .setSubtype(Subtype.inconsistentCompactPattern)
                                        .setMessage(
                                                "«{0}» instead would be consistent with {1} «{2}». Fix this OR fix {3} to be «{4}».",
                                                expectedValue,
                                                linkedLowerCode,
                                                lowerValue,
                                                ph.getCode(),
                                                expectedLowerValue));
                    }
                }

                // now compare the count values
                if (!isLong && !"other".equals(pluralCategory)) {
                    XPathParts otherParts = parts.cloneAsThawed();
                    otherParts.putAttributeValue(-1, "count", "other");
                    String otherPath = otherParts.toString();
                    String otherValue = getResolvedCldrFileToCheck().getStringValue(otherPath);
                    if (!value.equals(otherValue)) {
                        PathHeader ph = getPathHeaderFactory().fromPath(otherPath);
                        String otherCode = getLinkedCode(ph);
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(
                                                errorButWarningIfJustWhitespace(value, otherValue))
                                        .setSubtype(Subtype.inconsistentCompactPattern)
                                        .setMessage(
                                                "«{0}» isn’t the same as «{2}» for {1}.",
                                                value, otherCode, otherValue));
                    }
                }
            }

            // check all
            if (FORBIDDEN_NUMERIC_PATTERN_CHARS.containsSome(value)) {
                UnicodeSet chars = new UnicodeSet().addAll(value);
                chars.retainAll(FORBIDDEN_NUMERIC_PATTERN_CHARS);
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.illegalCharactersInNumberPattern)
                                .setMessage(
                                        "Pattern contains forbidden characters: \u200E{0}\u200E",
                                        new Object[] {chars.toPattern(false)}));
            }

            // get the final type
            String lastType = parts.getAttributeValue(-1, "type");
            int zeroCount = 0;
            // it can only be null or an integer of the form 10+
            if (lastType != null && !lastType.equals("standard")) {
                Matcher matcher = ALLOWED_INTEGER.matcher(lastType);
                if (matcher.matches()) {
                    zeroCount = matcher.end(1) - matcher.start(1); // number of ascii zeros
                } else {
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.badNumericType)
                                    .setMessage(
                                            "The type of a numeric pattern must be missing or of the form 10...."));
                }
            }

            // Check the validity of the pattern. If this check fails, all other checks
            // after it will fail, so exit early.
            UnicodeSet illegalChars = findUnquotedChars(type, value);
            if (illegalChars != null) {
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.illegalCharactersInNumberPattern)
                                .setMessage(
                                        "Pattern contains characters that must be escaped or removed: {0}",
                                        new Object[] {illegalChars}));
                return this;
            }

            // Tests that assume that the value is a valid number pattern.
            // Notice that we pick up any exceptions, so that we can
            // give a reasonable error message.
            parts = parts.cloneAsThawed();
            try {
                if (type == NumericType.DECIMAL_ABBREVIATED
                        || type == NumericType.CURRENCY_ABBREVIATED) {
                    // Check for consistency in short/long decimal formats.
                    checkDecimalFormatConsistency(parts, path, value, result, type);
                } else {
                    checkPattern(path, fullPath, value, result, false);
                }

                // Check for sane usage of grouping separators.
                if (COMMA_ABUSE.matcher(value).find()) {
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.tooManyGroupingSeparators)
                                    .setMessage(
                                            "Grouping separator (,) should not be used to group tens. Check if a decimal symbol (.) should have been used instead."));
                } else {
                    // check that we have a canonical pattern
                    String pattern = getCanonicalPattern(value, type, zeroCount, isPOSIX);
                    if (!pattern.equals(value)) {
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(CheckStatus.errorType)
                                        .setSubtype(Subtype.numberPatternNotCanonical)
                                        .setMessage(
                                                "Value should be \u200E{0}\u200E",
                                                new Object[] {pattern}));
                    }
                }

            } catch (Exception e) {
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.illegalNumberFormat)
                                .setMessage(
                                        e.getMessage() == null ? e.toString() : e.getMessage()));
            }
        }
        return this;
    }

    private Type errorButWarningIfJustWhitespace(String value, String otherValue) {
        return equalsExceptWhitespace(value, otherValue)
                ? CheckStatus.warningType
                : CheckStatus.errorType;
    }

    private boolean equalsExceptWhitespace(String value, String otherValue) {
        return removeWhitespace(value).equals(removeWhitespace(otherValue));
    }

    private String getLinkedCode(PathHeader ph) {
        String link = CLDRConfig.getInstance().urls().forXpath(getLocaleID(), ph.getOriginalPath());
        String linked =
                link == null ? ph.getCode() : "<a href='" + link + "'>" + ph.getCode() + "</a>";
        return linked;
    }

    private String normalizeNumberPattern(String expectedValue) {
        if (expectedValue.contains(";")) {
            List<String> forms = Splitters.SEMI.splitToList(expectedValue);
            String positive = forms.get(0);
            String negative = forms.get(1);
            if (negative.equals("-" + positive)) {
                expectedValue = positive;
            }
        }
        return expectedValue;
    }

    public static String removeCurrencyPlaceholderAndWhitespace(String plainValue) {
        return Patterns.CURRENCY_PLACEHOLDER_AND_POSSIBLE_WS.matcher(plainValue).replaceAll("");
    }

    public static String removeWhitespace(String plainValue) {
        return Patterns.WS.matcher(plainValue).replaceAll("");
    }

    public static String removeWhitespaceAroundCurrencyPlaceholder(String plainValue) {
        return Patterns.CURRENCY_PLACEHOLDER_AND_POSSIBLE_WS
                .matcher(plainValue)
                .replaceAll(Patterns.CURRENCY_PLACEHOLDER);
    }

    /**
     * Looks for any unquoted non-pattern characters in the specified string which would make the
     * pattern invalid.
     *
     * @param type the type of the pattern
     * @param value the string containing the number pattern
     * @return the set of unquoted chars in the pattern
     */
    private static UnicodeSet findUnquotedChars(NumericType type, String value) {
        UnicodeSet chars = new UnicodeSet();
        UnicodeSet allowedChars = null;
        // Allow the digits 1-9 here because they're already checked in another test.
        if (type == NumericType.DECIMAL_ABBREVIATED) {
            allowedChars = new UnicodeSet("[0-9]");
        } else {
            allowedChars = new UnicodeSet("[0-9#@.,E+]");
        }
        for (String subPattern : value.split(";")) {
            // Any unquoted non-special chars are allowed in front of or behind the numerical
            // symbols, but not in between, e.g. " 0000" is okay but "0 000" is not.
            int firstIdx = -1;
            for (int i = 0, len = subPattern.length(); i < len; i++) {
                char c = subPattern.charAt(i);
                if (c == '0' || c == '#') {
                    firstIdx = i;
                    break;
                }
            }
            if (firstIdx == -1) {
                continue;
            }
            int lastIdx = Math.max(subPattern.lastIndexOf("0"), subPattern.lastIndexOf('#'));
            chars.addAll(subPattern.substring(firstIdx, lastIdx));
        }
        chars.removeAll(allowedChars);
        return chars.size() > 0 ? chars : null;
    }

    /**
     * Override this method if you are going to provide examples of usage. Only needed for more
     * complicated cases, like number patterns.
     */
    @Override
    public CheckCLDR handleGetExamples(
            String path, String fullPath, String value, Options options, List result) {
        if (path.indexOf("/numbers") < 0) return this;
        try {
            if (path.indexOf("/pattern") >= 0 && path.indexOf("/patternDigit") < 0) {
                checkPattern(path, fullPath, value, result, true);
            }
            if (path.indexOf("/currencies") >= 0 && path.endsWith("/symbol")) {
                checkCurrencyFormats(path, fullPath, value, result, true);
            }
        } catch (Exception e) {
            // don't worry about errors here, they'll be caught above.
        }
        return this;
    }

    /**
     * Only called when we are looking at compact decimals. Make sure that we have a consistent
     * number of 0's at each level, and check for missing 0's. (The latter are only allowed for
     * "singular" plural forms).
     */
    private void checkDecimalFormatConsistency(
            XPathParts parts,
            String path,
            String value,
            List<CheckStatus> result,
            NumericType type) {
        // Look for duplicates of decimal formats with the same number
        // system and type.
        // Decimal formats of the same type should have the same number
        // of integer digits in all the available plural forms.
        DecimalFormat format = new DecimalFormat(value);
        int numIntegerDigits = format.getMinimumIntegerDigits();
        String countString = parts.getAttributeValue(-1, "count");
        Count thisCount = null;
        try {
            thisCount = Count.valueOf(countString);
        } catch (Exception e) {
            // can happen if count is numeric literal, like "1"
        }
        CLDRFile resolvedFile = getResolvedCldrFileToCheck();
        Set<String> inconsistentItems = new TreeSet<>();
        Set<Count> otherCounts = new HashSet<>(pluralTypes);
        if (thisCount != null) {
            Set<Double> pe = pluralExamples.get(thisCount);
            if (pe == null) {
                /*
                 * This can happen for unknown reasons when path =
                 * //ldml/numbers/currencyFormats[@numberSystem="latn"]/currencyFormatLength[@type="short"]/currencyFormat[@type="standard"]/pattern[@type="1000"][@count="one"]
                 * TODO: something? At least don't throw NullPointerException, as happened when the code
                 * was "... pluralExamples.get(thisCount).size() ..."; never assume get() returns non-null
                 */
                return;
            }
            if (!value.contains("0")) {
                switch (pe.size()) {
                    case 0: // do nothing, shouldn't ever happen
                        break;
                    case 1:
                        // If a plural case corresponds to a single double value, the format is
                        // allowed to not include a numeric value and in this way be inconsistent
                        // with the numeric formats used for other plural cases.
                        return;
                    default: // we have too many digits
                        result.add(
                                new CheckStatus()
                                        .setCause(this)
                                        .setMainType(CheckStatus.errorType)
                                        .setSubtype(Subtype.missingZeros)
                                        .setMessage(
                                                "Values without a zero must only be used where there is only one possible numeric form, but this has multiple: {0} ",
                                                pe.toString()));
                }
            }
            otherCounts.remove(thisCount);
        }
        for (Count count : otherCounts) {
            // System.out.println("## double examples for count " + count + ": " +
            // pluralExamples.get(count));
            parts.setAttribute("pattern", "count", count.toString());
            String otherPattern = resolvedFile.getWinningValue(parts.toString());
            // Ignore the type="other" pattern if not present or invalid.
            if (otherPattern == null || findUnquotedChars(type, otherPattern) != null) continue;
            format = new DecimalFormat(otherPattern);
            int numIntegerDigitsOther = format.getMinimumIntegerDigits();
            if (pluralExamples.get(count).size() == 1 && numIntegerDigitsOther <= 0) {
                // If a plural case corresponds to a single double value, the format is
                // allowed to not include a numeric value and in this way be inconsistent
                // with the numeric formats used for other plural cases.
                continue;
            }
            // skip special cases where the count=many is optional
            if (count == Count.many
                    && PluralRulesUtil.LOCALES_WITH_OPTIONAL_MANY.contains(
                            LocaleIDParser.getSimpleBaseLanguage(resolvedFile.getLocaleID()))) {
                continue;
            }
            if (numIntegerDigitsOther != numIntegerDigits) {
                PathHeader pathHeader = getPathHeaderFactory().fromPath(parts.toString());
                inconsistentItems.add(pathHeader.getHeaderCode());
            }
        }
        if (inconsistentItems.size() > 0) {
            // Get label for items of this type by removing the count.
            PathHeader pathHeader =
                    getPathHeaderFactory().fromPath(path.substring(0, path.lastIndexOf('[')));
            String groupHeaderString = pathHeader.getHeaderCode();
            boolean isWinningValue = resolvedFile.getWinningValue(path).equals(value);
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(
                                    isWinningValue
                                            ? CheckStatus.errorType
                                            : CheckStatus.warningType)
                            .setSubtype(Subtype.inconsistentPluralFormat)
                            .setMessage(
                                    "All values for {0} must have the same number of digits. "
                                            + "The number of zeros in this pattern is inconsistent with the following: {1}.",
                                    groupHeaderString, inconsistentItems.toString()));
        }
    }

    /**
     * This method builds a decimal format (based on whether the pattern is for currencies or not)
     * and tests samples.
     */
    private void checkPattern(
            String path, String fullPath, String value, List result, boolean generateExamples)
            throws ParseException {
        if (value.indexOf('\u00a4') >= 0) { // currency pattern
            DecimalFormat x = icuServiceBuilder.getCurrencyFormat("XXX");
            addOrTestSamples(x, x.toPattern(), value, result, generateExamples);
        } else {
            DecimalFormat x = icuServiceBuilder.getNumberFormat(value);
            addOrTestSamples(x, value, "", result, generateExamples);
        }
    }

    /** Check some currency patterns. */
    private void checkCurrencyFormats(
            String path, String fullPath, String value, List result, boolean generateExamples)
            throws ParseException {
        DecimalFormat x = icuServiceBuilder.getCurrencyFormat(NameType.getCode(path));
        addOrTestSamples(x, x.toPattern(), value, result, generateExamples);
    }

    /**
     * Generates some samples. If we are producing examples, these are used for that; otherwise they
     * are just tested.
     */
    private void addOrTestSamples(
            DecimalFormat x, String pattern, String context, List result, boolean generateExamples)
            throws ParseException {
        // Object[] arguments = new Object[3];
        //
        // double sample = getRandomNumber();
        // arguments[0] = String.valueOf(sample);
        // String formatted = x.format(sample);
        // arguments[1] = formatted;
        // boolean gotFailure = false;
        // try {
        // parsePosition.setIndex(0);
        // double parsed = x.parse(formatted, parsePosition).doubleValue();
        // if (parsePosition.getIndex() != formatted.length()) {
        // arguments[2] = "Couldn't parse past: " + "\u200E" +
        // formatted.substring(0,parsePosition.getIndex()) +
        // "\u200E";
        // gotFailure = true;
        // } else {
        // arguments[2] = String.valueOf(parsed);
        // }
        // } catch (Exception e) {
        // arguments[2] = e.getMessage();
        // gotFailure = true;
        // }
        // htmlMessage.append(pattern1)
        // .append(TransliteratorUtilities.toXML.transliterate(String.valueOf(sample)))
        // .append(pattern2)
        // .append(TransliteratorUtilities.toXML.transliterate(formatted))
        // .append(pattern3)
        // .append(TransliteratorUtilities.toXML.transliterate(String.valueOf(parsed)))
        // .append(pattern4);
        // if (generateExamples || gotFailure) {
        // result.add(new CheckStatus()
        // .setCause(this).setType(CheckStatus.exampleType)
        // .setMessage(SampleList, arguments));
        // }
        if (generateExamples) {
            result.add(
                    new MyCheckStatus()
                            .setFormat(x, context)
                            .setCause(this)
                            .setMainType(CheckStatus.demoType));
        }
    }

    /**
     * Generate a randome number for testing, with a certain number of decimal places, and half the
     * time negative
     */
    private static double getRandomNumber() {
        // min = 12345.678
        double rand = random.nextDouble();
        // System.out.println(rand);
        double sample = Math.round(rand * 100000.0 * 1000.0) / 1000.0 + 10000.0;
        if (random.nextBoolean()) sample = -sample;
        return sample;
    }

    /*
     * static String pattern1 =
     * "<table border='1' cellpadding='2' cellspacing='0' style='border-collapse: collapse' style='width: 100%'>"
     * + "<tr>"
     * + "<td nowrap width='1%'>Input:</td>"
     * + "<td><input type='text' name='T1' size='50' style='width: 100%' value='";
     * static String pattern2 = "'></td>"
     * + "<td nowrap width='1%'><input type='submit' value='Test' name='B1'></td>"
     * + "<td nowrap width='1%'>Formatted:</td>"
     * + "<td><input type='text' name='T2' size='50' style='width: 100%' value='";
     * static String pattern3 = "'></td>"
     * + "<td nowrap width='1%'>Parsed:</td>"
     * + "<td><input type='text' name='T3' size='50' style='width: 100%' value='";
     * static String pattern4 = "'></td>"
     * + "</tr>"
     * + "</table>";
     */

    /**
     * Produce a canonical pattern, which will vary according to type and whether it is posix or
     * not.
     *
     * @param count
     * @param path
     */
    public static String getCanonicalPattern(
            String inpattern, NumericType type, int zeroCount, boolean isPOSIX) {
        // TODO fix later to properly handle quoted ;
        DecimalFormat df = new DecimalFormat(inpattern);
        String pattern;

        if (zeroCount == 0) {
            int[] digits = isPOSIX ? type.getPosixDigitCount() : type.getDigitCount();
            df.setMinimumIntegerDigits(digits[0]);
            df.setMinimumFractionDigits(digits[1]);
            df.setMaximumFractionDigits(digits[2]);
            pattern = df.toPattern();
        } else { // of form 1000. Result must be 0+(.0+)?
            if (type == NumericType.CURRENCY_ABBREVIATED
                    || type == NumericType.DECIMAL_ABBREVIATED) {
                if (!inpattern.contains("0")) {
                    return inpattern; // we check in checkDecimalFormatConsistency to make sure that
                    // the "no number" case is allowed.
                }
                if (!inpattern.contains("0.0")) {
                    df.setMinimumFractionDigits(0); // correct the current rewrite
                }
            }
            df.setMaximumFractionDigits(df.getMinimumFractionDigits());
            int minimumIntegerDigits = df.getMinimumIntegerDigits();
            if (minimumIntegerDigits < 1) minimumIntegerDigits = 1;
            df.setMaximumIntegerDigits(minimumIntegerDigits);
            pattern = df.toPattern();
        }

        // int pos = pattern.indexOf(';');
        // if (pos < 0) return pattern + ";-" + pattern;
        return pattern;
    }

    /** You don't normally need this, unless you are doing a demo also. */
    public static class MyCheckStatus extends CheckStatus {
        private DecimalFormat df;
        String context;

        public MyCheckStatus setFormat(DecimalFormat df, String context) {
            this.df = df;
            this.context = context;
            return this;
        }

        @Override
        public SimpleDemo getDemo() {
            return new MyDemo().setFormat(df);
        }
    }

    /**
     * Here is how to do a demo. You provide the function getArguments that takes in-and-out
     * parameters.
     */
    static class MyDemo extends FormatDemo {
        private DecimalFormat df;

        @Override
        protected String getPattern() {
            return df.toPattern();
        }

        @Override
        protected String getSampleInput() {
            return String.valueOf(ExampleGenerator.NUMBER_SAMPLE);
        }

        public MyDemo setFormat(DecimalFormat df) {
            this.df = df;
            return this;
        }

        @Override
        protected void getArguments(Map<String, String> inout) {
            currentPattern = currentInput = currentFormatted = currentReparsed = "?";
            double d;
            try {
                currentPattern = inout.get("pattern");
                if (currentPattern != null) df.applyPattern(currentPattern);
                else currentPattern = getPattern();
            } catch (Exception e) {
                currentPattern = "Use format like: ##,###.##";
                return;
            }
            try {
                currentInput = inout.get("input");
                if (currentInput == null) {
                    currentInput = getSampleInput();
                }
                d = Double.parseDouble(currentInput);
            } catch (Exception e) {
                currentInput = "Use English format: 1234.56";
                return;
            }
            try {
                currentFormatted = df.format(d);
            } catch (Exception e) {
                currentFormatted = "Can't format: " + e.getMessage();
                return;
            }
            try {
                parsePosition.setIndex(0);
                Number n = df.parse(currentFormatted, parsePosition);
                if (parsePosition.getIndex() != currentFormatted.length()) {
                    currentReparsed =
                            "Couldn't parse past: \u200E"
                                    + currentFormatted.substring(0, parsePosition.getIndex())
                                    + "\u200E";
                } else {
                    currentReparsed = n.toString();
                }
            } catch (Exception e) {
                currentReparsed = "Can't parse: " + e.getMessage();
            }
        }
    }
}
