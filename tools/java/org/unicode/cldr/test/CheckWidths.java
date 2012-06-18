package org.unicode.cldr.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.RegexLookup;

public class CheckWidths extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*CheckWidths.*

    private static final double EM = ApproximateWidth.getWidth("月");

    private static final boolean DEBUG = true;

    private enum Special {NONE, QUOTES}
    private static class Limits {
        public Limits(int minimumLength, int maximumLength, double minimumWidth, double maximumWidth,
            String errorType, Special special) {
            this.minimumLength = minimumLength;
            this.maximumLength = maximumLength;
            this.minimumWidth = minimumWidth;
            this.maximumWidth = maximumWidth;
            this.errorType = errorType;
            this.special = special;
        }
        final int minimumLength;
        final int maximumLength;
        final double minimumWidth;
        final double maximumWidth;
        final String errorType;
        final Special special;
        boolean hasProblem(String value, List<CheckStatus> result, CheckCLDR cause) {
            if (special == Special.QUOTES) {
                value = value.replace("'","");
            }
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
                double width = ApproximateWidth.getWidth(value) / EM;
                if (minimumWidth >= 0 
                    && width < minimumWidth) {
                    lengthProblem = true;
                    result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(Subtype.valueTooNarrow) 
                        .setMessage("The value is not wide enough: should have at least a width of {0} em, but was ~{1} em.", minimumWidth, width)); 
                } else if (maximumWidth >= 0 
                    && width > maximumWidth) {
                    lengthProblem = true;
                    result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(Subtype.valueTooWide) 
                        .setMessage("The value is too wide: should have at most a width of {0} em, but was ~{1} em.", maximumWidth, width)); 
                }
            }
            return lengthProblem;
        }
    }

    static RegexLookup<Limits> lookup = new RegexLookup<Limits>()
        .setPatternTransform(RegexLookup.RegexFinderTransformPath)
        .addVariable("%A", "\"[^\"]+\"")
        .add("//ldml/delimiters/(quotation|alternateQuotation)", new Limits(1, 1, -1, -1, CheckStatus.errorType, Special.NONE))

        // The following are rough measures, just to check strange cases

        .add("//ldml/characters/ellipsis[@type=\"final\"]", new Limits(-1, -1, -1, 2 * ApproximateWidth.getWidth("{0}…"), CheckStatus.warningType, Special.NONE))
        .add("//ldml/characters/ellipsis[@type=\"initial\"]", new Limits(-1, -1, -1, 2 * ApproximateWidth.getWidth("…{0}"), CheckStatus.warningType, Special.NONE))
        .add("//ldml/characters/ellipsis[@type=\"medial\"]", new Limits(-1, -1, -1, 2 * ApproximateWidth.getWidth("{0}…{1}"), CheckStatus.warningType, Special.NONE))

        // Narrow items should be no wider than 2.5 em

        .add("//ldml/dates/calendars/calendar.*[@type=\"narrow\"](?!/cyclic|/dayPeriod)", new Limits(-1, -1, -1, 2.5, CheckStatus.errorType, Special.NONE))
        // \"(?!am|pm)[^\"]+\"\\
        // Numeric items should be no more than a single character

        .add("//ldml/numbers/symbols[@numberSystem=%A]/(decimal|group|minus|percent|perMille|plus)", new Limits(1, 1, -1, -1, CheckStatus.errorType, Special.NONE))
        
        // Compact number formats must be no bigger than 4 EM

        .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1", new Limits(-1, -1, -1, 4.5, CheckStatus.errorType, Special.QUOTES))
        ;
    
    Set<Limits> found = new LinkedHashSet();

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        if (value == null) {
            return this; // skip
        }
//        Limits item0 = lookup.get("//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000000000\"][@count=\"other\"]");
//        item0.check("123456789", result, this);
        
        Limits item = lookup.get(path);
        if (item != null) {
            if (item.hasProblem(value, result, this)) {
                if (DEBUG && !found.contains(item)) {
                    found.add(item);
                }
            }
        }
        return this;
    }    
}
