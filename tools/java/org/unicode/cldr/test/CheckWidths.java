package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.RegexLookup;

public class CheckWidths extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*CheckWidths.*

    private static final int EM = ApproximateWidth.getWidth("月");

    private static class Limits {
        public Limits(int minimumLength, int maximumLength, int minimumWidth, int maximumWidth,
            String errorType) {
            this.minimumLength = minimumLength;
            this.maximumLength = maximumLength;
            this.minimumWidth = minimumWidth;
            this.maximumWidth = maximumWidth;
            this.errorType = errorType;
        }
        final int minimumLength;
        final int maximumLength;
        final int minimumWidth;
        final int maximumWidth;
        final String errorType;
        void check(String path, String value, List<CheckStatus> result, CheckCLDR cause) {
            boolean lengthProblem = false;
            if (minimumLength >= 0 
                && value.codePointCount(0, value.length()) < minimumLength) {
                lengthProblem = true;
                result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(Subtype.valueTooNarrow) 
                    .setMessage("There are too few characters: should be at least {0} character long.", minimumLength)); 
            } else if (maximumLength >= 0 
                && value.codePointCount(0, value.length()) > maximumLength) {
                lengthProblem = true;
                result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(Subtype.valueTooWide) 
                    .setMessage("There are too many characters: should be at most {0} character long.", maximumLength)); 
            }
            if (!lengthProblem && (minimumWidth >= 0 || maximumWidth >= 0)) {
                int width = ApproximateWidth.getWidth(value) / EM;
                if (minimumWidth >= 0 
                    && width < minimumWidth) {
                    result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(Subtype.valueTooNarrow) 
                        .setMessage("The value is not wide enough: should have at least a width of {0} em, but was ~{1} em.", minimumWidth, width)); 
                } else if (maximumWidth >= 0 
                    && width > maximumWidth) {
                    result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(Subtype.valueTooWide) 
                        .setMessage("The value is too wide: should have at most a width of {0} em, but was ~{1} em.", maximumWidth, width)); 
                }
            }
        }
    }

    RegexLookup<Limits> lookup = new RegexLookup<Limits>()
        .add("^//ldml/delimiters/quotation", 
            new Limits(1, 1, -1, -1, CheckStatus.errorType))
            .add("^//ldml/delimiters/alternateQuotation", 
                new Limits(1, 1, -1, -1, CheckStatus.errorType))

                // The following are rough measures, just to check strange cases

                .add("^//ldml/characters/ellipsis[@type=\"final\"]", 
                    new Limits(-1, -1, -1, 2 * ApproximateWidth.getWidth("{0}…"), CheckStatus.errorType))
                    .add("^//ldml/characters/ellipsis[@type=\"initial\"]", 
                        new Limits(-1, -1, -1, 2 * ApproximateWidth.getWidth("…{0}"), CheckStatus.errorType))
                        .add("^//ldml/characters/ellipsis[@type=\"medial\"]", 
                            new Limits(-1, -1, -1, 2 * ApproximateWidth.getWidth("{0}…{1}"), CheckStatus.errorType))

                            // Narrow items should be no wider that 2 em

                            .add("^//ldml/dates/calendars/calendar.*\\[@type=\"narrow\"\\](?!/cyclic|/dayPeriod)", 
                                new Limits(-1, -1, -1, 2, CheckStatus.errorType))

                                // Numeric items should be no more than a single character

                                .add("^//ldml/numbers/symbols\\[@numberSystem=\"(?!am|pm)[^\"]\"\\]/(decimal|group|minus|percent|perMille|plus)", 
                                    new Limits(1, 1, -1, -1, CheckStatus.errorType))
                                    ;

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        if (value == null) {
            return this; // skip
        }
        Limits item = lookup.get(path);
        if (item != null) {
            item.check(path, value, result, this);
        }
        return this;
    }    
}
