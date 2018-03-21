/*
 ******************************************************************************
 * Copyright (C) 2005-2012, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckForExemplars extends FactoryCheckCLDR {
    private static final UnicodeSet RTL_CONTROLS = new UnicodeSet("[\\u061C\\u200E\\u200F\\u202A-\\u202D\\u2066-\\u2069]");

    private static final UnicodeSet RTL = new UnicodeSet("[[:bc=AL:][:bc=R:]]");

    private static final String STAND_IN = "#";

    // private final UnicodeSet commonAndInherited = new UnicodeSet(CheckExemplars.Allowed).complement();
    // "[[:script=common:][:script=inherited:][:alphabetic=false:]]");
    static String[] EXEMPLAR_SKIPS = {
        "/currencySpacing",
        "/exemplarCharacters",
        // "/pattern",
        "/localizedPatternChars",
        "/segmentations",
        "/references",
        "/localeDisplayNames/variants/",
        "/commonlyUsed",
        "/defaultNumberingSystem",
        "/otherNumberingSystems",
        "/exponential",
        "/nan",
        "/scientificFormats",
        "/inText",
        "/orientation",
        "/symbol[@alt=\"narrow\"]",
        "/characters/parseLenients"
    };

    static String[] DATE_PARTS = {
        "/hourFormat",
        "/dateFormatItem",
        "/intervalFormatItem",
        "/dateFormatLength",
        "timeFormatLength"
    };

    static final UnicodeSet START_PAREN = new UnicodeSet("[[:Ps:]]").freeze();
    static final UnicodeSet END_PAREN = new UnicodeSet("[[:Pe:]]").freeze();
    static final UnicodeSet ALL_CURRENCY_SYMBOLS = new UnicodeSet("[[:Sc:]]").freeze();
    static final UnicodeSet LETTER = new UnicodeSet("[[A-Za-z]]").freeze();
    static final UnicodeSet NUMBERS = new UnicodeSet("[[:N:]]").freeze();
    static final UnicodeSet DISALLOWED_HOUR_FORMAT = new UnicodeSet("[[:letter:]]").remove('H').remove('m').freeze();
    static final UnicodeSet DISALLOWED_IN_RANGE = new UnicodeSet("[:L:]").freeze();

    private UnicodeSet exemplars;
    private UnicodeSet exemplarsPlusAscii;
    //private static final UnicodeSet DISALLOWED_IN_scriptRegionExemplars = new UnicodeSet("[()（）;,；，]").freeze();
    //private static final UnicodeSet DISALLOWED_IN_scriptRegionExemplarsWithParens = new UnicodeSet("[;,；，]").freeze();

    // Hack until cldrbug 6566 is fixed. TODO
    private static final Pattern IGNORE_PLACEHOLDER_PARENTHESES = PatternCache.get("\\p{Ps}#\\p{Pe}");

    // private UnicodeSet currencySymbolExemplars;
    private boolean skip;
    private Collator col;
    private Collator spaceCol;
    UnicodeSetPrettyPrinter prettyPrint;
    private Status otherPathStatus = new Status();
    private Matcher patternMatcher = ExampleGenerator.PARAMETER.matcher("");
    private boolean errorDefaultOption;

    // for extracting date pattern text
    private DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
    StringBuilder justText = new StringBuilder();

    // public static final Pattern SUPPOSED_TO_BE_MESSAGE_FORMAT_PATTERN = PatternCache.get("/(" +
    // "codePattern" +
    // "|dateRangePattern" +
    // "|dateTimeFormat[^/]*?/pattern" +
    // "|appendItem" +
    // "|intervalFormatFallback" +
    // "|hoursFormat" +
    // "|gmtFormat" +
    // "|regionFormat" +
    // "|fallbackFormat" +
    // "|unitPattern.*@count=\"(zero|one|two|few|many|other)\"" +
    // "|localePattern" +
    // "|localeKeyTypePattern" +
    // "|listPatternPart" +
    // "|ellipsis" +
    // "|monthPattern" +
    // ")");
    // private Matcher supposedToBeMessageFormat = SUPPOSED_TO_BE_MESSAGE_FORMAT_PATTERN.matcher("");

    public static final Pattern LEAD_OR_TRAIL_WHITESPACE_OK = PatternCache.get("/(" +
        "references/reference" +
        "|insertBetween" +
        ")");
    private Matcher leadOrTrailWhitespaceOk = LEAD_OR_TRAIL_WHITESPACE_OK.matcher("");

    private static UnicodeSet ASCII = (UnicodeSet) new UnicodeSet("[\\u0020-\\u007F]").freeze();

    private PatternPlaceholders patternPlaceholders = PatternPlaceholders.getInstance();
    private SupplementalDataInfo sdi;
    private Relation scriptToCurrencies;

    public CheckForExemplars(Factory factory) {
        super(factory);
        // patternPlaceholders = RegexLookup.of(new PlaceholderTransform())
        // .loadFromFile(PatternPlaceholders.class, "data/Placeholders.txt");
        sdi = SupplementalDataInfo.getInstance();
    }

    /**
     * Adapted from GenerateXMB.MapTransform
     *
     * @author jchye
     *
     */
    static class PlaceholderTransform implements Transform<String, Set<String>> {
        @Override
        public Set<String> transform(String source) {
            Set<String> placeholders = new LinkedHashSet<String>();
            String[] parts = source.split(";\\s+");
            for (String part : parts) {
                int equalsPos = part.indexOf('=');
                String placeholder = part.substring(0, equalsPos).trim();
                placeholders.add(placeholder);
            }
            return placeholders;
        }
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFile, Options options, List<CheckStatus> possibleErrors) {
        if (cldrFile == null) return this;
        skip = true;
        super.setCldrFileToCheck(cldrFile, options, possibleErrors);
        if (cldrFile.getLocaleID().equals("root")) {
            return this;
        }

        errorDefaultOption = options.get(Options.Option.exemplarErrors) != null;

        String locale = cldrFile.getLocaleID();
        col = Collator.getInstance(new ULocale(locale));
        spaceCol = Collator.getInstance(new ULocale(locale));
        spaceCol.setStrength(Collator.PRIMARY);

        CLDRFile resolvedFile = getResolvedCldrFileToCheck();
        boolean[] ok = new boolean[1];
        exemplars = safeGetExemplars("", possibleErrors, resolvedFile, ok);

        if (exemplars == null) {
            CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.noExemplarCharacters)
                .setMessage("No Exemplar Characters: {0}", new Object[] { this.getClass().getName() });
            possibleErrors.add(item);
            return this;
        } else if (!ok[0]) {
            exemplars = new UnicodeSet();
        } else {
            exemplars = new UnicodeSet(exemplars); // modifiable copy
        }

        boolean isRTL = RTL.containsSome(exemplars);
        if (isRTL) {
            exemplars.addAll(RTL_CONTROLS);
        }
        // UnicodeSet temp = resolvedFile.getExemplarSet("standard");
        // if (temp != null) exemplars.addAll(temp);
        UnicodeSet auxiliary = safeGetExemplars("auxiliary", possibleErrors, resolvedFile, ok); // resolvedFile.getExemplarSet("auxiliary",
        // CLDRFile.WinningChoice.WINNING);
        if (auxiliary != null) {
            exemplars.addAll(auxiliary);
        }

        if (CheckExemplars.USE_PUNCTUATION) {
            UnicodeSet punctuation = safeGetExemplars("punctuation", possibleErrors, resolvedFile, ok); // resolvedFile.getExemplarSet("auxiliary",
            if (punctuation != null) {
                exemplars.addAll(punctuation);
            }

            UnicodeSet numbers = getNumberSystemExemplars();
            exemplars.addAll(numbers);

            // TODO fix replacement character
            exemplars.add(STAND_IN);
        }

        exemplars.addAll(CheckExemplars.AlwaysOK).freeze();
        exemplarsPlusAscii = new UnicodeSet(exemplars).addAll(ASCII).freeze();

        skip = false;
        prettyPrint = new UnicodeSetPrettyPrinter()
            .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
            .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                .setStrength2(Collator.PRIMARY))
            .setCompressRanges(true);
        return this;
    }

    private UnicodeSet getNumberSystemExemplars() {
        String numberSystem = getCldrFileToCheck().getStringValue("//ldml/numbers/defaultNumberingSystem");
        String digits = sdi.getDigits(numberSystem);
        return new UnicodeSet().addAll(digits);
    }

    private UnicodeSet safeGetExemplars(String type, List<CheckStatus> possibleErrors, CLDRFile resolvedFile,
        boolean[] ok) {
        UnicodeSet result = null;
        try {
            result = resolvedFile.getExemplarSet(type, CLDRFile.WinningChoice.WINNING);
            ok[0] = true;
        } catch (IllegalArgumentException iae) {
            possibleErrors.add(new CheckStatus()
                .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.couldNotAccessExemplars)
                .setMessage("Could not get exemplar set: " + iae.toString()));
            ok[0] = false;
        }
        return result;
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value,
        Options options, List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have
        if (value == null) return this; // skip values that we don't have ?
        if (skip) return this;
        if (path == null) {
            throw new InternalCldrException("Empty path!");
        } else if (getCldrFileToCheck() == null) {
            throw new InternalCldrException("no file to check!");
        }
        String sourceLocale = getResolvedCldrFileToCheck().getSourceLocaleID(path, otherPathStatus);

        // if we are an alias to another path, then skip
        // if (!path.equals(otherPathStatus.pathWhereFound)) {
        // return this;
        // }

        // now check locale source
        if (XMLSource.CODE_FALLBACK_ID.equals(sourceLocale)) {
            return this;
            // } else if ("root".equals(sourceLocale)) {
            // // skip eras for non-gregorian
            // if (true) return this;
            // if (path.indexOf("/calendar") >= 0 && path.indexOf("gregorian") <= 0) return this;
        }

        if (containsPart(path, EXEMPLAR_SKIPS)) {
            return this;
        }

        CheckStatus.Type errorOption = errorDefaultOption & sourceLocale.equals(getResolvedCldrFileToCheck().getLocaleID())
            ? CheckStatus.errorType : CheckStatus.warningType;

        value = checkAndReplacePlaceholders(path, value, result);
        if (path.startsWith("//ldml/numbers/miscPatterns") && path.contains("[@type=\"range\"]")) {
            if (DISALLOWED_IN_RANGE.containsSome(value)) {
                result
                    .add(new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.illegalCharactersInPattern)
                        .setMessage(
                            "Range patterns should not have letters.",
                            new Object[] {}));
            }
        }
        // Now handle date patterns.
        if (containsPart(path, DATE_PARTS)) {
            if (!extractDatePatternText(value, STAND_IN, justText)) {
                return this; // we are done, no text.
            }
            value = justText.toString();
            if (NUMBERS.containsSome(value)) {
                UnicodeSet disallowed = new UnicodeSet().addAll(value).retainAll(NUMBERS);
                addMissingMessage(disallowed, CheckStatus.errorType,
                    Subtype.patternCannotContainDigits,
                    Subtype.patternCannotContainDigits,
                    "cannot occur in date or time patterns", result);
            }
            if (path.endsWith("/hourFormat")) {
                UnicodeSet disallowed = new UnicodeSet().addAll(value)
                    .retainAll(DISALLOWED_HOUR_FORMAT);
                if (!disallowed.isEmpty()) {
                    addMissingMessage(disallowed, CheckStatus.errorType,
                        Subtype.patternContainsInvalidCharacters,
                        Subtype.patternContainsInvalidCharacters,
                        "cannot occur in the hour format", result);
                }
            }
        }

        if (path.startsWith("//ldml/posix/messages")) return this;

        UnicodeSet disallowed;

        if (path.contains("/currency") && path.contains("/symbol")) {
            if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
                disallowed.removeAll(ALL_CURRENCY_SYMBOLS);
                disallowed.removeAll(LETTER); // Allow ASCII A-Z in currency symbols
                // String currency = new XPathParts().set(path).getAttributeValue(-2, "type");
                if (disallowed.size() > 0) {
                    // && asciiNotAllowed(getCldrFileToCheck().getLocaleID(), currency)) {
                    addMissingMessage(disallowed, errorOption,
                        Subtype.charactersNotInMainOrAuxiliaryExemplars,
                        Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters",
                        result);
                }
            }
        } else if (path.contains("/gmtFormat") || path.contains("/gmtZeroFormat")) {
            if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
                disallowed.removeAll(LETTER); // Allow ASCII A-Z in gmtFormat and gmtZeroFormat
                if (disallowed.size() > 0) {
                    addMissingMessage(disallowed, errorOption,
                        Subtype.charactersNotInMainOrAuxiliaryExemplars,
                        Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters",
                        result);
                }
            }
        } else if (path.contains("/months") || path.contains("/quarters")) {
            if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
                disallowed.removeAll("IVXivx"); // Allow Roman-numeral letters in month or quarter names
                if (path.contains("/calendar[@type=\"generic\"]/months")) {
                    disallowed.removeAll("M"); // Generic-calendar month names contain 'M' and do not get modified
                }
                if (disallowed.size() > 0) {
                    addMissingMessage(disallowed, errorOption,
                        Subtype.charactersNotInMainOrAuxiliaryExemplars,
                        Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters",
                        result);
                }
            }
        } else if (path.contains("/localeDisplayNames") && !path.contains("/localeDisplayPattern")) {
            // test first for outside of the set.
            if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
                if (path.contains("[@type=\"iso8601\"]")) {
                    disallowed.removeAll("ISO"); // Name of ISO8601 calendar may contain "ISO" regardless of native script
                }
                if (disallowed.size() > 0) {
                    addMissingMessage(disallowed, errorOption, Subtype.charactersNotInMainOrAuxiliaryExemplars,
                        Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters", result);
                }
            }
            if (path.contains("/codePatterns")) {
                disallowed = new UnicodeSet().addAll(value).retainAll(NUMBERS);
                if (!disallowed.isEmpty()) {
                    addMissingMessage(disallowed, CheckStatus.errorType,
                        Subtype.patternCannotContainDigits,
                        Subtype.patternCannotContainDigits,
                        "cannot occur in locale fields", result);
                }
            }
        } else if (path.contains("/units")) {
            String noValidParentheses = IGNORE_PLACEHOLDER_PARENTHESES.matcher(value).replaceAll("");
            disallowed = new UnicodeSet().addAll(START_PAREN).addAll(END_PAREN)
                .retainAll(noValidParentheses);
            if (!disallowed.isEmpty()) {
                addMissingMessage(disallowed, CheckStatus.errorType,
                    Subtype.parenthesesNotAllowed,
                    Subtype.parenthesesNotAllowed,
                    "cannot occur in units", result);
            }
        } else if (path.endsWith("/exemplarCity")) {
            disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value);
            if (disallowed != null) {
                if ("root".equals(sourceLocale)) {
                    return this;
                }
                // Get script of locale.
                LocaleIDParser parser = new LocaleIDParser().set(sourceLocale);
                String script = parser.getScript();
                if (script.length() == 0) {
                    String localeID = sdi.getLikelySubtags().get(sourceLocale);
                    if (localeID == null) {
                        localeID = sdi.getLikelySubtags().get(parser.getLanguage());
                        if (localeID == null) {
                            throw new IllegalArgumentException(
                                "A likely subtag for " + parser.getLanguage() +
                                    " is required to get its script.");
                        }
                    }
                    script = parser.set(localeID).getScript();
                }
                int myscript = UScript.getCodeFromName(script);
                UnicodeSet toRemove = new UnicodeSet();
                for (int i = 0; i < disallowed.size(); i++) {
                    int c = disallowed.charAt(i);
                    if (UScript.getScript(c) == myscript) {
                        toRemove.add(c);
                    }
                }
                disallowed.removeAll(toRemove);
                if (disallowed.size() > 0) {
                    addMissingMessage(disallowed, errorOption, Subtype.charactersNotInMainOrAuxiliaryExemplars,
                        Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters", result);
                }
            }
        } else if (path.contains("/annotations") && !path.contains("[@type")) {
            if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
                addMissingMessage(disallowed, CheckStatus.warningType, Subtype.charactersNotInMainOrAuxiliaryExemplars,
                    Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters", result);
            }
        } else {
            if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
                addMissingMessage(disallowed, errorOption, Subtype.charactersNotInMainOrAuxiliaryExemplars,
                    Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters", result);
            }
        }

        // check for spaces

        if (!value.equals(value.trim())) {
            if (!leadOrTrailWhitespaceOk.reset(path).find()) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.mustNotStartOrEndWithSpace)
                    .setMessage("This item must not start or end with whitespace, or be empty."));
            }
        }
        // if (value.contains("  ")) {
        // result.add(new
        // CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.mustNotStartOrEndWithSpace)
        // .setMessage("This item must not contain two space characters in a row."));
        // }
        return this;
    }

    private String checkAndReplacePlaceholders(String path, String value, List<CheckStatus> result) {
        // add checks for patterns. Make sure that all and only the message format patterns have {n}
        Matcher matcher = patternMatcher.reset(value);
        Set<String> matchList = new HashSet<String>();
        StringBuffer placeholderBuffer = new StringBuffer();
        while (matcher.find()) {
            // Look for duplicate values.
            if (!matchList.add(matcher.group())) {
                placeholderBuffer.append(", ").append(matcher.group());
            }
        }
        Set<String> placeholders = null;
        PlaceholderStatus placeholderStatus = patternPlaceholders.getStatus(path);
        if (placeholderStatus != PlaceholderStatus.DISALLOWED) {
            placeholders = patternPlaceholders.get(path).keySet();
        }

        boolean supposedToHaveMessageFormatFields =
            // supposedToBeMessageFormat.reset(path).find()
            placeholders != null;

        if (supposedToHaveMessageFormatFields) {
            if (placeholderBuffer.length() > 0) {
                if (placeholderStatus != PlaceholderStatus.MULTIPLE) {
                    result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.extraPlaceholders)
                        .setMessage("Remove duplicates of{0}",
                            new Object[] { placeholderBuffer.substring(1) }));
                }
            }
            placeholderBuffer.setLength(0);
            // Check that the needed placeholders are there.
            if (placeholders == null) placeholders = new HashSet<String>();
            for (String placeholder : placeholders) {
                if (!matchList.contains(placeholder)) {
                    placeholderBuffer.append(", ").append(placeholder);
                }
            }
            boolean placeholdersMissing = false;
            if (placeholderBuffer.length() > 0) {
                // Check
                if (placeholderStatus == PlaceholderStatus.LOCALE_DEPENDENT && (path.contains("[@count=") || path.contains("[@ordinal="))) {
                    PluralRules rules = PluralRules.forLocale(new ULocale(getCldrFileToCheck().getLocaleID()));
                    XPathParts parts = XPathParts.getFrozenInstance(path);
                    String keyword = parts.getAttributeValue(-1, "count");
                    if (keyword == null) {
                        keyword = parts.getAttributeValue(-1, "ordinal");
                    }
                    placeholdersMissing = rules.getUniqueKeywordValue(keyword) == PluralRules.NO_UNIQUE_VALUE;
                } else {
                    placeholdersMissing = placeholderStatus == PlaceholderStatus.REQUIRED;
                }
            }
            if (placeholdersMissing) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.missingPlaceholders)
                    .setMessage("This message pattern is missing placeholder(s){0}. See the English for an example.",
                        new Object[] { placeholderBuffer.substring(1) }));
            }
            // Check for extra placeholders.
            matchList.removeAll(placeholders);
            if (matchList.size() > 0) {
                String list = matchList.toString();
                list = list.substring(1, list.length() - 1);
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.extraPlaceholders)
                    .setMessage("Extra placeholders {0} should be removed.",
                        new Object[] { list }));
            }
            // check the other characters in the message format patterns
            value = patternMatcher.replaceAll(STAND_IN);
        } else if (matchList.size() > 0 && placeholderStatus == PlaceholderStatus.DISALLOWED) { // non-message field has
            // placeholder values
            result.add(new CheckStatus()
                .setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.shouldntHavePlaceholders)
                .setMessage(
                    "This field is not a message pattern, and should not have '{0}, {1},' etc. See the English for an example.",
                    new Object[] {}));
            // end checks for patterns
        }
        return value;
    }

    /**
     * Checks if ASCII characters are allowed in a currency symbol in the specified locale.
     * @param localeID the locale ID that the currency is in
     * @param currency the currency to be checked
     * @return true if ASCII is not allowed
     */
    private boolean asciiNotAllowed(String localeID, String currency) {
        // Don't allow ascii at all for bidi scripts.
        String charOrientation = getResolvedCldrFileToCheck().getStringValue(
            "//ldml/layout/orientation/characterOrder");
        if (charOrientation.equals("right-to-left")) {
            return true;
        }

        // Get script of locale. if Latn, quit.
        LocaleIDParser parser = new LocaleIDParser().set(localeID);
        String script = parser.getScript();
        if (script.length() == 0) {
            localeID = sdi.getLikelySubtags().get(localeID);
            if (localeID == null) {
                localeID = sdi.getLikelySubtags().get(parser.getLanguage());
                if (localeID == null) {
                    throw new IllegalArgumentException(
                        "A likely subtag for " + parser.getLanguage() +
                            " is required to get its script.");
                }
            }
            script = parser.set(localeID).getScript();
        }
        if (script.equals("Latn")) {
            return false;
        }

        // Enforce checking of for other non-Latin scripts, for all currencies
        // whose countries use that script, e.g. Russian should have Cyrillic
        // currency symbols for modern currencies of countries with official
        // languages whose script is Cyrillic (Bulgaria, Serbia, ...).
        Set<String> currencies = getCurrenciesForScript(script);
        return currencies != null && currencies.contains(currency);
    }

    private Set<String> getCurrenciesForScript(String script) {
        if (scriptToCurrencies != null) return scriptToCurrencies.get(script);

        // Get mapping of scripts to the territories that use that script in
        // any of their primary languages.
        Relation scriptToTerritories = new Relation(new HashMap<String, Set<String>>(), HashSet.class);
        for (String lang : sdi.getBasicLanguageDataLanguages()) {
            BasicLanguageData langData = sdi.getBasicLanguageDataMap(lang).get(Type.primary);
            if (langData == null) {
                continue;
            }
            for (String curScript : langData.getScripts()) {
                scriptToTerritories.putAll(curScript, langData.getTerritories());
            }
        }

        // For each territory, get all of its legal tender currencies.
        Date now = new Date(System.currentTimeMillis());
        scriptToCurrencies = new Relation(new HashMap<String, Set<String>>(), HashSet.class);
        for (Object curScript : scriptToTerritories.keySet()) {
            Set<String> territories = scriptToTerritories.get(curScript);
            Set<String> currencies = new HashSet<String>();
            for (String territory : territories) {
                Set<CurrencyDateInfo> currencyInfo = sdi.getCurrencyDateInfo(territory);
                for (CurrencyDateInfo info : currencyInfo) {
                    if (info.isLegalTender() && info.getEnd().compareTo(now) > 0) {
                        currencies.add(info.getCurrency());
                    }
                }
            }
            scriptToCurrencies.putAll(curScript, currencies);
        }
        return scriptToCurrencies.get(script);
    }

    /**
     * Extracts just the text from a date field, replacing all the variable fields by variableReplacement. Return null
     * if
     * there is an error (a different test will find that error).
     */
    public boolean extractDatePatternText(String value, String variableReplacement, StringBuilder justText) {
        boolean haveText = false;
        try {
            formatParser.set(value);
        } catch (Exception e) {
            return false; // give up, it is illegal
        }
        boolean doReplacement = variableReplacement != null && variableReplacement.length() > 0;
        justText.setLength(0);
        for (Object item : formatParser.getItems()) {
            if (item instanceof String) {
                justText.append(item);
                haveText = true;
            } else {
                if (doReplacement) {
                    justText.append(variableReplacement);
                }
            }
        }
        return haveText;
    }

    public boolean containsPart(String source, String... segments) {
        for (int i = 0; i < segments.length; ++i) {
            if (source.indexOf(segments[i]) > 0) {
                return true;
            }
        }
        return false;
    }

    static final String TEST = "؉";

    private void addMissingMessage(UnicodeSet missing, CheckStatus.Type warningVsError, Subtype subtype,
        Subtype subtypeAscii,
        String qualifier, List<CheckStatus> result) {
        String fixedMissing = prettyPrint.format(missing);
        BitSet scripts = new BitSet();
        for (String s : missing) {
            final int script = UScript.getScript(s.codePointAt(0));
            if (script == UScript.INHERITED || script == UScript.COMMON) {
                continue;
            }
            scripts.set(script);
        }
        StringBuilder scriptString = new StringBuilder();
        if (!scripts.isEmpty()) {
            scriptString.append("{");
            for (int i = scripts.nextSetBit(0); i >= 0; i = scripts.nextSetBit(i + 1)) {
                if (scriptString.length() > 1) {
                    scriptString.append(", ");
                }
                scriptString.append(UScript.getName(i));
            }
            scriptString.append("}");
        }
        result
            .add(new CheckStatus()
                .setCause(this)
                .setMainType(warningVsError)
                .setSubtype(ASCII.containsAll(missing) ? subtypeAscii : subtype)
                .setMessage(
                    "The characters \u200E{0}\u200E {1} {2}. "
                        +
                        "For what to do, see <i>Handling Warnings</i> in <a target='CLDR-ST-DOCS' href='http://cldr.org/translation/characters#TOC-Handing-Warnings'>Characters</a>.",
                    new Object[] { fixedMissing, scriptString, qualifier }));
    }

    static final Normalizer2 NFC = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.COMPOSE);

    /**
     * Return null if ok, otherwise UnicodeSet of bad characters
     *
     * @param exemplarSet
     * @param value
     * @return
     */
    private UnicodeSet containsAllCountingParens(UnicodeSet exemplarSet, UnicodeSet exemplarSetPlusASCII, String value) {
        UnicodeSet result = null;
        if (exemplarSet.containsAll(value)) {
            return result;
        }

        // Normalize
        value = NFC.normalize(value);

        // if we failed, then check that everything outside of () is ok.
        // and everything inside parens is either ASCII or in the set
        int lastPos = 0;
        while (true) {
            int start = START_PAREN.findIn(value, lastPos, false);
            String outside = value.substring(lastPos, start);
            result = addDisallowedItems(exemplarSet, outside, result);
            if (start == value.length()) {
                break; // all done
            }
            ++start;
            int end = END_PAREN.findIn(value, start, false);
            // don't worry about mixed brackets
            String inside = value.substring(start, end);
            result = addDisallowedItems(exemplarSetPlusASCII, inside, result);
            if (end == value.length()) {
                break; // all done
            }
            lastPos = end + 1;
        }
        return result;
    }

    private UnicodeSet addDisallowedItems(UnicodeSet exemplarSet, String outside, UnicodeSet result) {
        if (!exemplarSet.containsAll(outside)) {
            if (result == null) {
                result = new UnicodeSet();
            }
            result.addAll(new UnicodeSet().addAll(outside).removeAll(exemplarSet));
        }
        return result;
    }
}
