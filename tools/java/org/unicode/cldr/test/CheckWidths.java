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

    private enum Measure {CODE_POINTS, DISPLAY_WIDTH}
    private enum LimitType {MINIMUM, MAXIMUM}
    private enum Special {NONE, QUOTES}
    
    private static class Limit {
        final double reference;
        final LimitType limit;
        final Measure measure;
        final String errorType;
        final Special special;
        final String message;
        final Subtype subtype;

        public Limit(double reference, Measure measure, LimitType limit, String errorType, Special special) {
            this.reference = reference;
            this.limit = limit;
            this.measure = measure;
            this.errorType = errorType;
            this.special = special;
            switch (limit) {
            case MINIMUM:
                this.message = measure == Measure.CODE_POINTS 
                ? "Expected length of ≥{0} character(s), but was {1}" 
                    : "Expected width of ≥{0} em, but was ~{1} em." ;
                this.subtype = Subtype.valueTooNarrow;
                break;
            case MAXIMUM:
                this.message = measure == Measure.CODE_POINTS 
                ? "Expected length of ≤{0} character(s), but was {1}."
                    : "Expected width of ≤{0} em, but was ~{1} em.";
                this.subtype = Subtype.valueTooWide;
                break;
            default: throw new IllegalArgumentException();
            }
        }

        boolean hasProblem(String value, List<CheckStatus> result, CheckCLDR cause) {
            if (special == Special.QUOTES) {
                value = value.replace("'","");
            }
            double valueMeasure = measure == Measure.CODE_POINTS 
                ? value.codePointCount(0, value.length()) 
                    : ApproximateWidth.getWidth(value) / EM;
            switch (limit) {
            case MINIMUM: 
                if (valueMeasure >= reference) {
                    return false;
                }
                break;
            case MAXIMUM: 
                if (valueMeasure <= reference) {
                    return false;
                }
                break;
            }
            result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(subtype) 
                .setMessage(message, reference, valueMeasure)); 
            return true;
        }
    }

    // WARNING: errors must occur before warnings!!
    
    static RegexLookup<Limit[]> lookup = new RegexLookup<Limit[]>()
        .setPatternTransform(RegexLookup.RegexFinderTransformPath)
        .addVariable("%A", "\"[^\"]+\"")
        .add("//ldml/delimiters/(quotation|alternateQuotation)", new Limit[] {
            new Limit(1, Measure.CODE_POINTS, LimitType.MAXIMUM, CheckStatus.errorType, Special.NONE)
            })

        // Numeric items should be no more than a single character

        .add("//ldml/numbers/symbols[@numberSystem=%A]/(decimal|group|minus|percent|perMille|plus)", new Limit[] {
            new Limit(1, Measure.CODE_POINTS, LimitType.MAXIMUM, CheckStatus.errorType, Special.NONE)
            })

        // Now widths
        // The following are rough measures, just to check strange cases

        .add("//ldml/characters/ellipsis[@type=\"final\"]", new Limit[] {
            new Limit(1.5 * ApproximateWidth.getWidth("{0}…"), Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, CheckStatus.warningType, Special.NONE)
            })
        .add("//ldml/characters/ellipsis[@type=\"initial\"]", new Limit[] {
            new Limit(1.5 * ApproximateWidth.getWidth("…{0}"), Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, CheckStatus.warningType, Special.NONE)
            })
        .add("//ldml/characters/ellipsis[@type=\"medial\"]", new Limit[] {
            new Limit(1.5 * ApproximateWidth.getWidth("{0}…{1}"), Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, CheckStatus.warningType, Special.NONE)
            })

        // Narrow items

        .add("//ldml/dates/calendars/calendar.*[@type=\"narrow\"](?!/cyclic|/dayPeriod)", new Limit[] {
            new Limit(2.25, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, CheckStatus.errorType, Special.NONE),
            new Limit(1.5, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, CheckStatus.warningType, Special.NONE)
            })
        // \"(?!am|pm)[^\"]+\"\\

        // Compact number formats

        .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1", 
            new Limit[] {
            new Limit(4.5, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, CheckStatus.errorType, Special.QUOTES),
            new Limit(4, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, CheckStatus.warningType, Special.QUOTES)
            })
            ;

    Set<Limit> found = new LinkedHashSet();

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        if (value == null) {
            return this; // skip
        }
        //        Limits item0 = lookup.get("//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000000000\"][@count=\"other\"]");
        //        item0.check("123456789", result, this);

        Limit[] items = lookup.get(path);
        if (items != null) {
            for (Limit item : items) {
                if (item.hasProblem(value, result, this)) {
                    if (DEBUG && !found.contains(item)) {
                        found.add(item);
                    }
                    break; // only one error per item
                }
            }
        }
        return this;
    }    
}
