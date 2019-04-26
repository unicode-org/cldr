package org.unicode.cldr.test;

import java.util.List;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

public class CheckCasing extends CheckCLDR {
    public enum Case {
        mixed, lowercase_words, titlecase_words, titlecase_firstword, verbatim;
        public static Case forString(String input) {
            return valueOf(input.replace('-', '_'));
        }
    };

    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Currencies.*

    ULocale uLocale = null;
    BreakIterator breaker = null;

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        uLocale = new ULocale(cldrFileToCheck.getLocaleID());
        breaker = BreakIterator.getWordInstance(uLocale);
        return this;
    }

    // If you don't need any file initialization or postprocessing, you only need this one routine
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        // it helps performance to have a quick reject of most paths
        if (fullPath == null) return this; // skip paths that we don't have
        if (fullPath.indexOf("casing") < 0) return this;

        // pick up the casing attributes from the full path
        XPathParts parts = XPathParts.getTestInstance(fullPath); // frozen should be OK

        Case caseTest = Case.mixed;
        for (int i = 0; i < parts.size(); ++i) {
            String casingValue = parts.getAttributeValue(i, "casing");
            if (casingValue == null) {
                continue;
            }
            caseTest = Case.forString(casingValue);
            if (caseTest == Case.verbatim) {
                return this; // we're done
            }
        }

        String newValue = value;
        switch (caseTest) {
        case lowercase_words:
            newValue = UCharacter.toLowerCase(uLocale, value);
            break;
        case titlecase_words:
            newValue = UCharacter.toTitleCase(uLocale, value, null);
            break;
        case titlecase_firstword:
            newValue = TitleCaseFirst(uLocale, value);
            break;
        default:
            break;

        }
        if (!newValue.equals(value)) {
            // the following is how you signal an error or warning (or add a demo....)
            result.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.errorType)
                .setSubtype(Subtype.incorrectCasing)
                // typically warningType or errorType
                .setMessage("Casing incorrect: either should have casing=\"verbatim\" or be <{0}>",
                    new Object[] { newValue })); // the message; can be MessageFormat with arguments
        }
        return this;
    }

    // -f(bg|cs|da|el|et|is|it|lt|ro|ru|sl|uk) -t(.*casing.*)

    private String TitleCaseFirst(ULocale locale, String value) {
        if (value.length() == 0) {
            return value;
        }
        breaker.setText(value);
        breaker.first();
        int endOfFirstWord = breaker.next();
        return UCharacter.toTitleCase(uLocale, value.substring(0, endOfFirstWord), breaker)
            + value.substring(endOfFirstWord);
    }

}