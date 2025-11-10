package org.unicode.cldr.test;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterDirection;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ComparatorUtilities;
import org.unicode.cldr.util.ExemplarSets;
import org.unicode.cldr.util.ExemplarSets.ExemplarType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.XPathParts;

public class CheckExemplars extends FactoryCheckCLDR {
    private static final boolean SUPPRESS_AUX_EMPTY_CHECK = true;
    private static final String[] QUOTE_ELEMENTS = {
        "quotationStart", "quotationEnd",
        "alternateQuotationStart", "alternateQuotationEnd"
    };

    private boolean isRoot;
    private UnicodeSetPrettyPrinter rawFormatter;

    public CheckExemplars(Factory factory) {
        super(factory);
    }

    // Allowed[:script=common:][:script=inherited:][:alphabetic=false:]

    @Override
    public CheckCLDR handleSetCldrFileToCheck(
            CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.handleSetCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        String locale = cldrFileToCheck.getLocaleID();
        isRoot = cldrFileToCheck.getLocaleID().equals("root");
        Collator col = ComparatorUtilities.getIcuCollator(new ULocale(locale), Collator.IDENTICAL);
        Collator spaceCol =
                ComparatorUtilities.getIcuCollator(new ULocale(locale), Collator.PRIMARY);
        rawFormatter = UnicodeSetPrettyPrinter.from((Comparator) col, (Comparator) spaceCol);

        // check for auxiliary anyway
        if (!SUPPRESS_AUX_EMPTY_CHECK) {
            UnicodeSet auxiliarySet =
                    getResolvedCldrFileToCheck()
                            .getExemplarSet(ExemplarType.auxiliary, CLDRFile.WinningChoice.WINNING);

            if (auxiliarySet == null) {
                possibleErrors.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.warningType)
                                .setSubtype(Subtype.missingAuxiliaryExemplars)
                                .setMessage(
                                        "Most languages allow <i>some<i> auxiliary characters, so review this."));
            }
        }
        return this;
    }

    @Override
    public CheckCLDR handleCheck(
            String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (fullPath == null) {
            return this; // skip paths that we don't have
        }
        if (!path.contains("/exemplarCharacters")) {
            if (path.contains("parseLenient")) {
                checkParse(path, value, result);
            }
            return this;
        }
        if (!accept(result)) {
            return this;
        }
        XPathParts oparts = XPathParts.getFrozenInstance(path);
        final String exemplarString = oparts.findAttributeValue("exemplarCharacters", "type");
        ExemplarType type = ExemplarType.from(exemplarString);
        checkExemplar(value, result, type);

        // check relation to auxiliary set
        try {
            UnicodeSet mainSet =
                    getResolvedCldrFileToCheck()
                            .getExemplarSet(ExemplarType.main, CLDRFile.WinningChoice.WINNING);
            UnicodeSet currentSet = SimpleUnicodeSetFormatter.parseLenient(value);
            switch (type) {
                case auxiliary:
                    checkAuxiliary(currentSet, mainSet, result);
                    break;
                case punctuation:
                    checkPunctuation(currentSet, result);
                    break;
                case index:
                    checkIndex(currentSet, mainSet, result);
                    break;
                case main:
                case punctuation_auxiliary:
                case punctuation_person:
                case numbers:
                case numbers_auxiliary:
                    break;
                default:
                    throw new IllegalArgumentException("Case not handled: " + type);
            }

            // check for consistency with RTL

            boolean localeIsRTL = false;
            String charOrientation =
                    getResolvedCldrFileToCheck()
                            .getStringValue("//ldml/layout/orientation/characterOrder");
            if (charOrientation.equals("right-to-left")) {
                localeIsRTL = true;
            }

            UnicodeSetIterator mi = new UnicodeSetIterator(mainSet);
            while (mi.next()) {
                if (mi.codepoint != UnicodeSetIterator.IS_STRING
                        && (UCharacter.getDirection(mi.codepoint)
                                        == UCharacterDirection.RIGHT_TO_LEFT
                                || UCharacter.getDirection(mi.codepoint)
                                        == UCharacterDirection.RIGHT_TO_LEFT_ARABIC)
                        && !localeIsRTL) {
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.orientationDisagreesWithExemplars)
                                    .setMessage(
                                            "Main exemplar set contains RTL characters, but orientation of this locale is not RTL."));
                    break;
                }
            }

        } catch (Exception ignored) {
        } // if these didn't parse, checkExemplar will be called anyway at some point
        return this;
    }

    private void checkAuxiliary(
            UnicodeSet currentSet, UnicodeSet mainSet, List<CheckStatus> result) {
        UnicodeSet combined = new UnicodeSet(mainSet).addAll(currentSet);
        checkMixedScripts("main+auxiliary", combined, result);
        if (currentSet.containsSome(mainSet)) {
            UnicodeSet overlap =
                    new UnicodeSet(mainSet)
                            .retainAll(currentSet)
                            .removeAll(ExemplarSets.HangulSyllables);
            if (!overlap.isEmpty()) {
                String fixedExemplar1 = rawFormatter.format(overlap);
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.auxiliaryExemplarsOverlap)
                                .setMessage(
                                        "Auxiliary characters also exist in main: \u200E{0}\u200E",
                                        fixedExemplar1));
            }
        }
    }

    private void checkPunctuation(UnicodeSet currentSet, List<CheckStatus> result) {
        // Check that the punctuation exemplar characters include quotation marks.
        UnicodeSet quoteSet = new UnicodeSet();
        for (String element : QUOTE_ELEMENTS) {
            quoteSet.add(
                    getResolvedCldrFileToCheck().getWinningValue("//ldml/delimiters/" + element));
        }
        if (!currentSet.containsAll(quoteSet)) {
            quoteSet.removeAll(currentSet);
            // go ahead and list the characters separately, with space between, for
            // clarity.
            StringBuilder characters = new StringBuilder();
            for (String item : quoteSet) {
                if (characters.length() != 0) {
                    characters.append(" ");
                }
                characters.append(item);
            }
            CheckStatus message =
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.warningType)
                            .setSubtype(Subtype.missingPunctuationCharacters)
                            .setMessage(
                                    "Punctuation exemplar characters are missing quotation marks for this locale: {0}",
                                    characters);
            result.add(message);
        }
    }

    private void checkIndex(UnicodeSet currentSet, UnicodeSet mainSet, List<CheckStatus> result) {
        // Check that the index exemplar characters are in case-completed union of main
        // and auxiliary exemplars
        UnicodeSet auxiliarySet2 =
                getResolvedCldrFileToCheck()
                        .getExemplarSet(ExemplarType.auxiliary, CLDRFile.WinningChoice.WINNING);
        if (auxiliarySet2 == null) {
            auxiliarySet2 = new UnicodeSet();
        }
        UnicodeSet mainAndAuxAllCase =
                new UnicodeSet(mainSet)
                        .addAll(auxiliarySet2)
                        .closeOver(UnicodeSet.ADD_CASE_MAPPINGS);
        UnicodeSet indexBadChars = currentSet.removeAll(mainAndAuxAllCase);

        if (!indexBadChars.isEmpty()) {
            CheckStatus message =
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.warningType)
                            .setSubtype(Subtype.charactersNotInMainOrAuxiliaryExemplars)
                            .setMessage(
                                    "Index exemplars include characters not in main or auxiliary exemplars: {0}",
                                    indexBadChars.toPattern(false));
            result.add(message);
        }
    }

    private void checkParse(String path, String value, List<CheckStatus> result) {
        if (value == null) {
            CheckStatus message =
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.badParseLenient)
                            .setMessage("null value");
            result.add(message);
            return;
        }
        try {
            XPathParts oparts = XPathParts.getFrozenInstance(path);
            // only thing we do is make sure that the sample is in the value
            UnicodeSet us = SimpleUnicodeSetFormatter.parseLenient(value);
            String sampleValue = oparts.getAttributeValue(-1, "sample");
            if (!us.contains(sampleValue)) {
                CheckStatus message =
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.badParseLenient)
                                .setMessage(
                                        "ParseLenient sample not in value: {0} ∌ {1}",
                                        us, sampleValue);
                result.add(message);
            }
        } catch (IllegalArgumentException e) {
            /*
             * new UnicodeSet(value) throws IllegalArgumentException if, for example, value is null or value = "?".
             * This can happen during cldr-unittest TestAll.
             * path = //ldml/characters/parseLenients[@scope="general"][@level="lenient"]/parseLenient[@sample="’"]
             * or
             * path = //ldml/characters/parseLenients[@scope="date"][@level="lenient"]/parseLenient[@sample="-"]
             */
            CheckStatus message =
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.badParseLenient)
                            .setMessage(e + (e.getMessage() == null ? "" : ": " + e.getMessage()));
            result.add(message);
        }
    }

    private void checkMixedScripts(String title, UnicodeSet set, List<CheckStatus> result) {
        BitSet s = new BitSet();
        for (String item : set) {
            int script = UScript.getScript(item.codePointAt(0));
            if (script != UScript.COMMON && script != UScript.INHERITED) {
                s.set(script);
            }
        }
        final int cardinality = s.cardinality();
        if (cardinality < 2) {
            return;
        }
        if (cardinality == 2 && title.equals("currencySymbol") && s.get(UScript.LATIN)) {
            return; // allow 2 scripts in exemplars for currencies.
        }
        // allowable combinations
        if (s.equals(ExemplarSets.Japn) || s.equals(ExemplarSets.Kore)) {
            return;
        }
        StringBuilder scripts = new StringBuilder();
        for (int i = s.nextSetBit(0); i >= 0; i = s.nextSetBit(i + 1)) {
            if (scripts.length() != 0) {
                scripts.append(", ");
            }
            scripts.append(UScript.getName(i));
            UnicodeSet inSet =
                    new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, i).retainAll(set);
            int count = 0;
            scripts.append(" (");
            for (String cp : inSet) {
                if (count != 0) {
                    scripts.append(",");
                }
                scripts.append(cp);
                count++;
                if (count > 3) {
                    scripts.append('\u2026');
                    break;
                }
            }
            scripts.append(")");
        }
        result.add(
                new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.errorType)
                        .setSubtype(Subtype.illegalExemplarSet)
                        .setMessage("{0} exemplars contain multiple scripts: {1}", title, scripts));
    }

    private void checkExemplar(
            String v, List<CheckStatus> result, ExemplarSets.ExemplarType exemplarType) {
        if (v == null) return;
        final UnicodeSet exemplar1;
        try {
            exemplar1 = SimpleUnicodeSetFormatter.parseLenient(v).freeze();
        } catch (Exception e) {
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.illegalExemplarSet)
                            .setMessage(e.getMessage()));
            return;
        }

        // check for mixed scripts

        checkMixedScripts(exemplarType.toString(), exemplar1, result);

        // check that the formatting is correct

        String fixedExemplar1 = rawFormatter.format(exemplar1);
        UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar1);
        if (!doubleCheck.equals(exemplar1)) {
            result.add(
                    new CheckStatus()
                            .setCause(this)
                            .setMainType(CheckStatus.errorType)
                            .setSubtype(Subtype.internalUnicodeSetFormattingError)
                            .setMessage(
                                    "Internal Error: formatting not working for {0}", exemplar1));
        }
        // else if (!v.equals(fixedExemplar1)) {
        // result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType)
        // .setMessage("Better formatting would be \u200E{0}\u200E", new Object[]{fixedExemplar1}));
        // }

        // now check that only allowed characters are in the set

        if (!exemplarType.allowed.containsAll(exemplar1)) {
            UnicodeSet remainder0 = new UnicodeSet(exemplar1).removeAll(exemplarType.allowed);

            // we do allow for punctuation & combining marks in strings
            UnicodeSet remainder = new UnicodeSet();
            for (String s : remainder0) {
                if (Character.codePointCount(s, 0, s.length()) == 1) {
                    remainder.add(s);
                }
                // else just check normalization
            }

            // after a first check, we check again in case we flattened

            if (!remainder.isEmpty()) {
                result.add(
                        new CheckStatus()
                                .setCause(this)
                                .setMainType(CheckStatus.errorType)
                                .setSubtype(Subtype.illegalCharactersInExemplars)
                                .setMessage(
                                        "Should be limited to "
                                                + exemplarType.message
                                                + "; thus not contain: \u200E{0}\u200E",
                                        remainder));
            }
        }

        // now check for empty

        if (!isRoot && exemplar1.isEmpty()) {
            switch (exemplarType) {
                    //            case currencySymbol: // ok if empty
                    //                break;
                case auxiliary:
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.warningType)
                                    .setSubtype(Subtype.missingAuxiliaryExemplars)
                                    .setMessage(
                                            "Most languages allow <i>some<i> auxiliary characters, so review this."));
                    break;
                case index:
                case punctuation:
                case main:
                    result.add(
                            new CheckStatus()
                                    .setCause(this)
                                    .setMainType(CheckStatus.errorType)
                                    .setSubtype(Subtype.missingMainExemplars)
                                    .setMessage(
                                            "Exemplar set ("
                                                    + exemplarType
                                                    + ") must not be empty -- that would imply that this language uses no "
                                                    + (exemplarType
                                                                    == ExemplarSets.ExemplarType
                                                                            .punctuation
                                                            ? "punctuation"
                                                            : "letters")
                                                    + "!"));
                    break;
            }
        }
    }
}
