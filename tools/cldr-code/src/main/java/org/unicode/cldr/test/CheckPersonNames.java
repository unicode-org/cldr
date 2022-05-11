package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Type;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;

import com.google.common.collect.ImmutableMultimap;

public class CheckPersonNames extends CheckCLDR {

    static final String MISSING = "∅∅∅";
    /**
     * @internal, public for testing
     */
    public static final ImmutableMultimap<SampleType, String> REQUIRED = ImmutableMultimap.<SampleType, String> builder()
        .putAll(SampleType.givenOnly, "given")
        .putAll(SampleType.givenSurnameOnly, "given", "surname")
        .putAll(SampleType.given12Surname, "given", "given2", "surname")
        .putAll(SampleType.full, "prefix", "given", "given-informal", "given2", "surname", "surname2", "suffix")
        .build();
    /**
     * @internal, public for testing
     */
    public static final ImmutableMultimap<SampleType, String> REQUIRED_EMPTY = ImmutableMultimap.<SampleType, String> builder()
        .putAll(SampleType.givenOnly, "prefix", "given-informal", "given2", "surname", "surname-prefix", "surname-core", "surname2", "suffix")
        .putAll(SampleType.givenSurnameOnly, "prefix", "given2", "surname-prefix", "surname-core", "surname2", "suffix")
        .build();

    boolean isRoot = false;

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options, List<CheckStatus> possibleErrors) {
        isRoot = cldrFileToCheck.getLocaleID().equals("root");
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
        return this;
    }
}
