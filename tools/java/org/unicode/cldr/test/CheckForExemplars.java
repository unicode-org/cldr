/*
 ******************************************************************************
 * Copyright (C) 2005-2012, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.dev.util.PrettyPrinter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckForExemplars extends FactoryCheckCLDR {
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
        "/inText"
    };

    static String[] DATE_PARTS = {
        "/hourFormat",
        "/dateFormatItem",
        "/intervalFormatItem",
        "/dateFormatLength",
        "timeFormatLength"
    };

    static final UnicodeSet START_PAREN = new UnicodeSet("[(\\[（［]").freeze();
    static final UnicodeSet END_PAREN = new UnicodeSet("[)\\]］）]").freeze();
    static final UnicodeSet ALL_CURRENCY_SYMBOLS = new UnicodeSet("[[:Sc:]]").freeze();
    static final UnicodeSet NUMBERS = new UnicodeSet("[[:N:]]").freeze();
    static final UnicodeSet DISALLOWED_HOUR_FORMAT = new UnicodeSet("[[:letter:]]").remove('H').remove('m').freeze();

    private UnicodeSet exemplars;
    private UnicodeSet exemplarsPlusAscii;
    private static final UnicodeSet DISALLOWED_IN_scriptRegionExemplars = new UnicodeSet("[()（）;,；，]").freeze();
    private static final UnicodeSet DISALLOWED_IN_scriptRegionExemplarsWithParens = new UnicodeSet("[;,；，]").freeze();
    // private UnicodeSet currencySymbolExemplars;
    private boolean skip;
    private Collator col;
    private Collator spaceCol;
    private String informationMessage;
    PrettyPrinter prettyPrint;
    private Status otherPathStatus = new Status();
    private Matcher patternMatcher = ExampleGenerator.PARAMETER.matcher("");

    // for extracting date pattern text
    private DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();
    StringBuilder justText = new StringBuilder();

    // public static final Pattern SUPPOSED_TO_BE_MESSAGE_FORMAT_PATTERN = Pattern.compile("/(" +
    // "codePattern" +
    // "|dateRangePattern" +
    // "|dateTimeFormat[^/]*?/pattern" +
    // "|appendItem" +
    // "|intervalFormatFallback" +
    // "|hoursFormat" +
    // "|gmtFormat" +
    // "|regionFormat" +
    // "|fallbackRegionFormat" +
    // "|fallbackFormat" +
    // "|unitPattern.*@count=\"(zero|one|two|few|many|other)\"" +
    // "|localePattern" +
    // "|localeKeyTypePattern" +
    // "|listPatternPart" +
    // "|ellipsis" +
    // "|monthPattern" +
    // ")");
    // private Matcher supposedToBeMessageFormat = SUPPOSED_TO_BE_MESSAGE_FORMAT_PATTERN.matcher("");

    public static final Pattern LEAD_OR_TRAIL_WHITESPACE_OK = Pattern.compile("/(" +
        "localeSeparator" +
        "|references/reference" +
        "|insertBetween" +
        ")");
    private Matcher leadOrTrailWhitespaceOk = LEAD_OR_TRAIL_WHITESPACE_OK.matcher("");

    private static UnicodeSet ASCII_UPPERCASE = (UnicodeSet) new UnicodeSet("[A-Z]").freeze();
    private static UnicodeSet ASCII = (UnicodeSet) new UnicodeSet("[\\u0020-\\u007F]").freeze();

    static final Pattern IS_COUNT_ZERO_ONE_TWO = Pattern.compile("/units.*\\[@count=\"(zero|one|two)\"");
    private Matcher isCountZeroOneTwo = IS_COUNT_ZERO_ONE_TWO.matcher("");
    private boolean hasSpecialPlurals;
    private PatternPlaceholders patternPlaceholders = PatternPlaceholders.getInstance();

    public CheckForExemplars(Factory factory) {
        super(factory);
        // patternPlaceholders = RegexLookup.of(new PlaceholderTransform())
        // .loadFromFile(PatternPlaceholders.class, "data/Placeholders.txt");
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

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFile, Map<String, String> options, List<CheckStatus> possibleErrors) {
        if (cldrFile == null) return this;
        skip = true;
        super.setCldrFileToCheck(cldrFile, options, possibleErrors);
        if (cldrFile.getLocaleID().equals("root")) {
            return this;
        }
        String locale = cldrFile.getLocaleID();
        hasSpecialPlurals = locale.equals("ar") || locale.startsWith("ar_");
        informationMessage = "<a href='http://unicode.org/cldr/apps/survey?_=" + locale
            + "&x=characters'>characters</a>";
        col = Collator.getInstance(new ULocale(locale));
        spaceCol = Collator.getInstance(new ULocale(locale));
        spaceCol.setStrength(Collator.PRIMARY);

        CLDRFile resolvedFile = getResolvedCldrFileToCheck();
        boolean[] ok = new boolean[1];
        exemplars = safeGetExemplars("", possibleErrors, resolvedFile, ok);
        if (!ok[0]) exemplars = new UnicodeSet();

        if (exemplars == null) {
            CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.noExemplarCharacters)
                .setMessage("No Exemplar Characters: {0}", new Object[] { this.getClass().getName() });
            possibleErrors.add(item);
            return this;
        }
        // UnicodeSet temp = resolvedFile.getExemplarSet("standard");
        // if (temp != null) exemplars.addAll(temp);
        UnicodeSet auxiliary = safeGetExemplars("auxiliary", possibleErrors, resolvedFile, ok); // resolvedFile.getExemplarSet("auxiliary",
                                                                                                // CLDRFile.WinningChoice.WINNING);
        if (auxiliary != null) exemplars.addAll(auxiliary);
        exemplars.addAll(CheckExemplars.AlwaysOK).freeze();
        exemplarsPlusAscii = new UnicodeSet(exemplars).addAll(ASCII).freeze();

        skip = false;
        prettyPrint = new PrettyPrinter()
            .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
            .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                .setStrength2(Collator.PRIMARY))
            .setCompressRanges(true);
        return this;
    }

    private UnicodeSet safeGetExemplars(String type, List possibleErrors, CLDRFile resolvedFile, boolean[] ok) {
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
        Map<String, String> options, List<CheckStatus> result) {
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
            placeholders != null
                && !(hasSpecialPlurals
                && isCountZeroOneTwo.reset(path).find());

        if (supposedToHaveMessageFormatFields) {
            if (placeholderBuffer.length() > 0) {
                result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.extraPlaceholders)
                    .setMessage("Remove duplicates of{0}",
                        new Object[] { placeholderBuffer.substring(1) }));
            }
            placeholderBuffer.setLength(0);
            // Check that the needed placeholders are there.
            if (placeholders == null) placeholders = new HashSet<String>();
            for (String placeholder : placeholders) {
                if (!matchList.contains(placeholder)) {
                    placeholderBuffer.append(", ").append(placeholder);
                }
            }
            if (placeholderBuffer.length() > 0 && placeholderStatus == PlaceholderStatus.REQUIRED) {
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
            result
                .add(new CheckStatus()
                    .setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.shouldntHavePlaceholders)
                    .setMessage(
                        "This field is not a message pattern, and should not have '{0}, {1},' etc. See the English for an example.",
                        new Object[] {}));
            // end checks for patterns
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

        if (path.contains("/currency") && path.endsWith("/symbol")) {
            if (null != (disallowed = containsAllCountingParens(exemplarsPlusAscii, exemplarsPlusAscii, value))) {
                disallowed.removeAll(ALL_CURRENCY_SYMBOLS);
                if (disallowed.size() > 0) {
                    addMissingMessage(disallowed, CheckStatus.warningType,
                        Subtype.charactersNotInMainOrAuxiliaryExemplars,
                        Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters",
                        result);
                }
            }
        } else if (path.contains("/localeDisplayNames") && !path.contains("/localeDisplayPattern")) {
            // test first for outside of the set.
            if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
                addMissingMessage(disallowed, CheckStatus.warningType, Subtype.charactersNotInMainOrAuxiliaryExemplars,
                    Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters", result);
            }
            UnicodeSet disallowedInExemplars = path.contains("_") ? DISALLOWED_IN_scriptRegionExemplarsWithParens
                : DISALLOWED_IN_scriptRegionExemplars;
            if (disallowedInExemplars.containsSome(value)) {
                disallowed = new UnicodeSet().addAll(value).retainAll(disallowedInExemplars);
                addMissingMessage(disallowed, CheckStatus.warningType, Subtype.discouragedCharactersInTranslation,
                    Subtype.discouragedCharactersInTranslation, "should not be used in this context", result);
                //
                // String fixedMissing = prettyPrint
                // .setToQuote(null)
                // .setQuoter(null).format(missing);
                // result.add(new
                // CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.discouragedCharactersInTranslation)
                // .setMessage("The characters \u200E{1}\u200E are discouraged in display names. Please avoid these characters.",
                // new Object[]{null,fixedMissing}));
                // // note: we are using {1} so that we don't include these in the console summary of bad characters.
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
        } else if (null != (disallowed = containsAllCountingParens(exemplars, exemplarsPlusAscii, value))) {
            addMissingMessage(disallowed, CheckStatus.warningType, Subtype.charactersNotInMainOrAuxiliaryExemplars,
                Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars, "are not in the exemplar characters", result);
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

    private void addMissingMessage(UnicodeSet missing, String warningVsError, Subtype subtype, Subtype subtypeAscii,
        String qualifier, List<CheckStatus> result) {
        if (missing.containsAll(TEST)) {
            int x = 1;
        }
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
