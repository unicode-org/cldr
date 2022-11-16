package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;

import com.google.common.collect.ImmutableMultimap;
import com.ibm.icu.text.UnicodeSet;

public class CheckPersonNames extends CheckCLDR {

    static final String MISSING = "∅∅∅";
    /**
     * @internal, public for testing
     */
    public static final ImmutableMultimap<SampleType, String> REQUIRED = ImmutableMultimap.<SampleType, String> builder()
        .putAll(SampleType.nativeG, "given")
        .putAll(SampleType.nativeGS, "given", "surname")
        .putAll(SampleType.nativeGGS, "given", "given2", "surname")
        .putAll(SampleType.foreignFull, "prefix", "given", "given-informal", "given2", "surname", "surname2", "suffix")
        .putAll(SampleType.nativeG, "given")
        .putAll(SampleType.nativeGS, "given", "surname")
        .putAll(SampleType.nativeGGS, "given", "surname")
        .putAll(SampleType.nativeFull, "given", "surname")
        .putAll(SampleType.foreignG, "given")
        .putAll(SampleType.foreignGS, "given", "surname")
        .putAll(SampleType.foreignGGS, "given", "given2", "surname")
        .putAll(SampleType.foreignFull, "prefix", "given", "given-informal", "given2", "surname", "surname2", "suffix")
        .build();
    /**
     * @internal, public for testing
     */
    public static final ImmutableMultimap<SampleType, String> REQUIRED_EMPTY = ImmutableMultimap.<SampleType, String> builder()
        .putAll(SampleType.nativeG, "prefix", "given-informal", "given2", "surname", "surname-prefix", "surname-core", "surname2", "suffix")
        .putAll(SampleType.nativeGS, "prefix", "given2", "surname-prefix", "surname-core", "surname2", "suffix")
        .putAll(SampleType.nativeG, "prefix", "given-informal", "given2", "surname", "surname-prefix", "surname-core", "surname2", "suffix")
        .putAll(SampleType.nativeGS, "prefix", "given2", "surname-prefix", "surname-core", "surname2", "suffix")
        .putAll(SampleType.foreignG, "prefix", "given-informal", "given2", "surname", "surname-prefix", "surname-core", "surname2", "suffix")
        .putAll(SampleType.foreignGS, "prefix", "given2", "surname-prefix", "surname-core", "surname2", "suffix")
        .build();

    boolean isRoot = false;
    UnicodeSet allowedCharacters;

// commented out temporarily; will reenable later
//    static final UnicodeSet BASE_ALLOWED = new UnicodeSet("[\\p{sc=Common}\\p{sc=Inherited}]").freeze();
//    static final UnicodeSet HANI = new UnicodeSet("[\\p{sc=Hani}]").freeze();
//    static final UnicodeSet KORE = new UnicodeSet("[\\p{sc=Hang}]").addAll(HANI).freeze();
//    static final UnicodeSet JPAN = new UnicodeSet("[\\p{sc=Kana}\\p{sc=Hira}]").addAll(HANI).freeze();


    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        String localeId = cldrFileToCheck.getLocaleID();
        isRoot = localeId.equals("root");

        // other characters are caught by CheckForExemplars
//        String script = new LikelySubtags().getLikelyScript(localeId);
//        allowedCharacters = new UnicodeSet(BASE_ALLOWED).addAll(getUnicodeSetForScript(script))
//            .freeze();
//
        return super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
    }

//    public UnicodeSet getUnicodeSetForScript(String script) {
//        switch (script) {
//        case "Japn":
//            return JPAN;
//        case "Kore":
//            return KORE;
//        case "Hant":
//        case "Hans":
//            return HANI;
//        default:
//            return new UnicodeSet("[\\p{sc=" + script + "}]");
//        }
//    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null || isRoot || !path.startsWith("//ldml/personNames/")) {
            return this;
        }

        XPathParts parts = XPathParts.getFrozenInstance(path);
        String category = parts.getElement(2);
        if (category.equals("sampleName")) {
//            if (!allowedCharacters.containsAll(value)) {
//                UnicodeSet bad = new UnicodeSet().addAll(value).removeAll(allowedCharacters);
//                result.add(new CheckStatus().setCause(this)
//                    .setMainType(CheckStatus.errorType)
//                    .setSubtype(Subtype.badSamplePersonName)
//                    .setMessage("Illegal characters in sample name: " + bad.toPattern(false)));
//            } else
            {

                Type status = CheckStatus.warningType;
                String message = null;
                SampleType item = SampleType.valueOf(parts.getAttributeValue(2, "item"));
                String modifiedField = parts.getAttributeValue(3, "type");
                boolean isMissing = value.equals(MISSING);
                if (isMissing) {
                    if (REQUIRED.get(item).contains(modifiedField)) {
                        message = "This value must not be empty (" + MISSING + ")";
                    } else if (modifiedField.equals("surname")) {
                        String surname2 = getCldrFileToCheck().getStringValue(path.replace("surname", "surname2"));
                        if (!surname2.equals(MISSING)) {
                            message = "The value for 'surname' must not be empty (" + MISSING + ") unless 'surname2' is.";
                        }
                    }
                } else { // not missing, so...
                    status = CheckStatus.errorType;
                    if (REQUIRED_EMPTY.get(item).contains(modifiedField)) {
                        message = "This value must be empty (" + MISSING + ")";
                    } else if (modifiedField.equals("multiword") && !value.contains(" ")) {
                        message = "All multiword fields must have 2 (or more) words separated by spaces";
                    }
                }
                if (message != null) {
                    result.add(new CheckStatus().setCause(this)
                        .setMainType(status)
                        .setSubtype(Subtype.badSamplePersonName)
                        .setMessage(message));
                }
            }
        }
        return this;
    }
}
