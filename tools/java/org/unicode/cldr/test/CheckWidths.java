package org.unicode.cldr.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.RegexLookup;

public class CheckWidths extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*CheckWidths.*

    private static final double EM = ApproximateWidth.getWidth("月");

    private static final boolean DEBUG = true;

    private enum Measure {
        CODE_POINTS, DISPLAY_WIDTH
    }

    private enum LimitType {
        MINIMUM, MAXIMUM
    }

    private enum Special {
        NONE, QUOTES, PLACEHOLDERS
    }

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\d\\}");

    private static class Limit {
        final double warningReference;
        final double errorReference;
        final LimitType limit;
        final Measure measure;
        final Special special;
        final String message;
        final Subtype subtype;

        public Limit(double warningReference, double errorReference, Measure measure, LimitType limit, Special special) {
            this.warningReference = warningReference;
            this.errorReference = errorReference;
            this.limit = limit;
            this.measure = measure;
            this.special = special;
            switch (limit) {
            case MINIMUM:
                this.message = measure == Measure.CODE_POINTS
                    ? "Expected no fewer than {0} character(s), but was {1}."
                    : "Too narrow by about {2}% (with common fonts).";
                this.subtype = Subtype.valueTooNarrow;
                break;
            case MAXIMUM:
                this.message = measure == Measure.CODE_POINTS
                    ? "Expected no more than {0} character(s), but was {1}."
                    : "Too wide by about {2}% (with common fonts).";
                this.subtype = Subtype.valueTooWide;
                break;
            default:
                throw new IllegalArgumentException();
            }
        }

        boolean hasProblem(String value, List<CheckStatus> result, CheckCLDR cause) {
            switch (special) {
            case QUOTES:
                value = value.replace("'", "");
                break;
            case PLACEHOLDERS:
                value = PLACEHOLDER_PATTERN.matcher(value).replaceAll("");
                break;
            }
            double valueMeasure = measure == Measure.CODE_POINTS
                ? value.codePointCount(0, value.length())
                : ApproximateWidth.getWidth(value);
            String errorType = CheckStatus.warningType;
            switch (limit) {
            case MINIMUM:
                if (valueMeasure >= warningReference) {
                    return false;
                }
                if (valueMeasure < errorReference) {
                    errorType = CheckStatus.errorType;
                }
                break;
            case MAXIMUM:
                if (valueMeasure <= warningReference) {
                    return false;
                }
                if (valueMeasure > errorReference) {
                    errorType = CheckStatus.errorType;
                }
                break;
            }
            double percent = (int) (Math.abs(100 * ApproximateWidth.getWidth(value) / warningReference - 100.0d) + 0.49999d);
            result.add(new CheckStatus().setCause(cause).setMainType(errorType).setSubtype(subtype)
                .setMessage(message, warningReference, valueMeasure, percent));
            return true;
        }
    }

    // WARNING: errors must occur before warnings!!

    static RegexLookup<Limit[]> lookup = new RegexLookup<Limit[]>()
        .setPatternTransform(RegexLookup.RegexFinderTransformPath)
        .addVariable("%A", "\"[^\"]+\"")
        .add("//ldml/delimiters/(quotation|alternateQuotation)", new Limit[] {
            new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NONE)
        })

        // Numeric items should be no more than a single character

        .add("//ldml/numbers/symbols[@numberSystem=%A]/(decimal|group|minus|percent|perMille|plus)", new Limit[] {
            new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NONE)
        })

        // Now widths
        // The following are rough measures, just to check strange cases

        .add("//ldml/characters/ellipsis[@type=\"(final|initial|medial)\"]", new Limit[] {
            new Limit(2 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })

        .add("//ldml/localeDisplayNames/localeDisplayPattern/", new Limit[] { // {0}: {1}, {0} ({1}), ,
            new Limit(2 * EM, 3 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })

        .add("//ldml/listPatterns/listPattern/listPatternPart[@type=%A]", new Limit[] { // {0} and {1}
            new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })

        .add("//ldml/dates/timeZoneNames/fallbackFormat", new Limit[] { // {1} ({0})
            new Limit(2 * EM, 3 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })

        .add("//ldml/dates/timeZoneNames/(fallbackRegionFormat|regionFormat|hourFormat)", new Limit[] { // {1} Time
                                                                                                        // ({0}), {0}
                                                                                                        // Time,
                                                                                                        // +HH:mm;-HH:mm
            new Limit(10 * EM, 20 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })

        .add("//ldml/dates/timeZoneNames/(gmtFormat|gmtZeroFormat)", new Limit[] { // GMT{0}, GMT
            new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })

        // Narrow items

        .add("//ldml/dates/calendars/calendar.*[@type=\"narrow\"](?!/cyclic|/dayPeriod|/monthPattern)", new Limit[] {
            new Limit(1.5 * EM, 2.25 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })
        // \"(?!am|pm)[^\"]+\"\\

        // Compact number formats

        .add(
            "//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1",
            new Limit[] {
            new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.QUOTES)
            });

    Set<Limit> found = new LinkedHashSet<Limit>();

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
        List<CheckStatus> result) {
        if (value == null) {
            return this; // skip
        }
        // Limits item0 =
        // lookup.get("//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000000000\"][@count=\"other\"]");
        // item0.check("123456789", result, this);

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
