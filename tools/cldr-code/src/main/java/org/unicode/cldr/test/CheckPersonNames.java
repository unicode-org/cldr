package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter.Optionality;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;

import com.ibm.icu.text.UnicodeSet;

public class CheckPersonNames extends CheckCLDR {

    static final String MISSING = CldrUtility.NO_INHERITANCE_MARKER;
    boolean isRoot = false;
    boolean hasRootParent = false;

    UnicodeSet allowedCharacters;

    static final UnicodeSet BASE_ALLOWED = new UnicodeSet("[\\p{sc=Common}\\p{sc=Inherited}-\\p{N}-[∅]]").freeze();
    static final UnicodeSet HANI = new UnicodeSet("[\\p{sc=Hani}]").freeze();
    static final UnicodeSet KORE = new UnicodeSet("[\\p{sc=Hang}]").addAll(HANI).freeze();
    static final UnicodeSet JPAN = new UnicodeSet("[\\p{sc=Kana}\\p{sc=Hira}]").addAll(HANI).freeze();


    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        String localeId = cldrFileToCheck.getLocaleID();
        isRoot = localeId.equals("root");
        hasRootParent = "root".equals(LocaleIDParser.getParent(localeId));

        // other characters are caught by CheckForExemplars
        String script = new LikelySubtags().getLikelyScript(localeId);
        allowedCharacters = new UnicodeSet(BASE_ALLOWED).addAll(getUnicodeSetForScript(script))
            .freeze();
//
        return super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
    }

    public UnicodeSet getUnicodeSetForScript(String script) {
        switch (script) {
        case "Jpan":
            return JPAN;
        case "Kore":
            return KORE;
        case "Hant":
        case "Hans":
            return HANI;
        default:
            return new UnicodeSet("[\\p{sc=" + script + "}]");
        }
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null
            || isRoot
            || !path.startsWith("//ldml/personNames/")) {
            return this;
        }

        XPathParts parts = XPathParts.getFrozenInstance(path);
        String category = parts.getElement(2);

        if (category.equals("sampleName")) {
            if (!allowedCharacters.containsAll(value)) {
                UnicodeSet bad = new UnicodeSet().addAll(value).removeAll(allowedCharacters);
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.badSamplePersonName)
                    .setMessage("Illegal characters in sample name: " + bad.toPattern(false)));
            } else if (getCldrFileToCheck().getUnresolved().getStringValue(path) != null) {

                // (Above) We only check for an error if there is a value in the UNresolved file.
                // That is, we don't want MISSING that is inherited from root to cause an error, unless it is explicitly inherited

                // We also check that
                // - if there is a surname2, there must be either a surname or surname.core

                String message = null;
                SampleType sampleType = SampleType.valueOf(parts.getAttributeValue(2, "item"));
                String modifiedField = parts.getAttributeValue(3, "type");
                boolean isMissingInUnresolved = value.equals(MISSING)
                    || value.equals(CldrUtility.INHERITANCE_MARKER);

                final Optionality optionality = sampleType.getOptionality(modifiedField);
                if (isMissingInUnresolved) {
                    if (optionality == Optionality.required) {
                        message = "This value must not be empty (" + MISSING + ")";
                    }
                } else { // not missing, so...
                    if (optionality == Optionality.disallowed) {
                        message = "This value must be empty (" + MISSING + ")";
                    } else if (modifiedField.equals("surname2")) {
                        String surname = getCldrFileToCheck().getStringValue(path).replace("surname2", "surname");
                        String surnameCore = getCldrFileToCheck().getStringValue(path).replace("surname2", "surname-core");
                        if (surname.equals(MISSING) && surnameCore.equals(MISSING)) {
                            message = "The value for '" + modifiedField + "' must not be empty (" + MISSING + ") unless 'surname2' is.";
                        }
                    }
                }
                if (message != null) {
                    getPhase();
                    final Type mainType = getPhase() != Phase.BUILD ? CheckStatus.errorType : CheckStatus.warningType; // we need to be able to check this in without error
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(mainType)
                        .setSubtype(Subtype.badSamplePersonName)
                        .setMessage(message));
                }
            }
        }
        return this;
    }
}
