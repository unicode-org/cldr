package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;

import com.google.common.collect.ImmutableMultimap;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
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
    UnicodeSet badScriptSet;

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        String localeId = cldrFileToCheck.getLocaleID();
        isRoot = localeId.equals("root");
        String script = new LikelySubtags().getLikelyScript(localeId);
        badScriptSet = new UnicodeSet()
            .applyIntPropertyValue(UProperty.SCRIPT, UScript.getCodeFromName(script))
            .applyIntPropertyValue(UProperty.SCRIPT, UScript.INHERITED)
            .applyIntPropertyValue(UProperty.SCRIPT, UScript.COMMON)
            .complement()
            .freeze();


        return super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
    }

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (value == null || isRoot || !path.startsWith("//ldml/personNames/")) {
            return this;
        }

        XPathParts parts = XPathParts.getFrozenInstance(path);
        String category = parts.getElement(2);
        if (category.equals("sampleName")) {
            if (badScriptSet.containsSome(value)) {
                UnicodeSet bad = new UnicodeSet().addAll(value).retainAll(badScriptSet);
                result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.errorType)
                    .setSubtype(Subtype.badSamplePersonName)
                    .setMessage("Illegal characters in sample name: " + bad.toPattern(false)));
            } else {

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
