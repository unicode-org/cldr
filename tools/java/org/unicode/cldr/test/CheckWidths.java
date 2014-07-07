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
        NONE, QUOTES, PLACEHOLDERS, NUMBERSYMBOLS
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
        final boolean debug;

        public Limit(double warningReference, double errorReference, Measure measure, LimitType limit, Special special, boolean debug) {
            this.debug = debug;
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

        public Limit(double d, double e, Measure displayWidth, LimitType maximum, Special placeholders) {
            this(d, e, displayWidth, maximum, placeholders, false);
        }

        boolean hasProblem(String value, List<CheckStatus> result, CheckCLDR cause) {
            switch (special) {
            case QUOTES:
                value = value.replace("'", "");
                break;
            case PLACEHOLDERS:
                value = PLACEHOLDER_PATTERN.matcher(value).replaceAll("");
                break;
            case NUMBERSYMBOLS:
                value = value.replaceAll("[\u200E\u200F]", ""); // don't include LRM/RLM when checking length of number symbols
                break;
            }
            double valueMeasure = measure == Measure.CODE_POINTS ? value.codePointCount(0, value.length()) : ApproximateWidth.getWidth(value);
            CheckStatus.Type errorType = CheckStatus.warningType;
            switch (limit) {
            case MINIMUM:
                if (valueMeasure >= warningReference) {
                    return false;
                }
                if (valueMeasure < errorReference && cause.getPhase() != Phase.BUILD) {
                    errorType = CheckStatus.errorType;
                }
                break;
            case MAXIMUM:
                if (valueMeasure <= warningReference) {
                    return false;
                }
                if (valueMeasure > errorReference && cause.getPhase() != Phase.BUILD) {
                    errorType = CheckStatus.errorType;
                }
                break;
            }
            // the 115 is so that we don't show small percentages
            // the /10 ...*10 is to round to multiples of 10% percent
            double percent = (int) (Math.abs(115 * valueMeasure / warningReference - 100.0d) / 10 + 0.49999d) * 10;
            result.add(new CheckStatus().setCause(cause)
                .setMainType(errorType)
                .setSubtype(subtype)
                .setMessage(message, warningReference, valueMeasure, percent));
            return true;
        }
    }

    // WARNING: errors must occur before warnings!!
    // we allow unusual units and English units to be a little longer
    static final String ALLOW_LONGER = "(area-acre" +
        "|area-square-foot" +
        "|area-square-mile" +
        "|length-foot" +
        "|length-inch" +
        "|length-mile" +
        "|length-light-year" +
        "|length-yard" +
        "|mass-ounce" +
        "|mass-pound" +
        "|power-horsepower" +
        "|pressure-inch-hg" +
        "|speed-mile-per-hour" +
        "|temperature-fahrenheit" +
        "|volume-cubic-mile" +
        "|acceleration-g-force" +
        "|speed-kilometer-per-hour" +
        "|speed-meter-per-second" +
        ")";

    static RegexLookup<Limit[]> lookup = new RegexLookup<Limit[]>()
        .setPatternTransform(RegexLookup.RegexFinderTransformPath)
        .addVariable("%A", "\"[^\"]+\"")
        .add("//ldml/delimiters/(quotation|alternateQuotation)", new Limit[] {
            new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NONE)
        })

        // Numeric items should be no more than a single character

        .add("//ldml/numbers/symbols[@numberSystem=%A]/(decimal|group|minus|percent|perMille|plus)", new Limit[] {
            new Limit(1, 1, Measure.CODE_POINTS, LimitType.MAXIMUM, Special.NUMBERSYMBOLS)
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

        .add("//ldml/dates/timeZoneNames/(regionFormat|hourFormat)", new Limit[] { // {0} Time,
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

        .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1",
            new Limit[] {
            new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.QUOTES)
            })
        // Catch -future/past Narrow units  and allow much wider values
        .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"[^\"]+-(future|past)\"]/unitPattern", new Limit[] {
            new Limit(10 * EM, 15 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })
        // Catch special units and allow a bit wider
        .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"" + ALLOW_LONGER + "\"]/unitPattern", new Limit[] {
            new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })
        // Narrow units
        .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=%A]/unitPattern", new Limit[] {
            new Limit(3 * EM, 4 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })
        // Short units
        .add("//ldml/units/unitLength[@type=\"short\"]/unit[@type=%A]/unitPattern", new Limit[] {
            new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })

        // Currency Symbols
        .add("//ldml/numbers/currencies/currency[@type=%A]/symbol", new Limit[] {
            new Limit(3 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        });

    Set<Limit> found = new LinkedHashSet<Limit>();

    @Override
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options, List<CheckStatus> result) {
        if (value == null) {
            return this; // skip
        }
        //        String testPrefix = "//ldml/units/unitLength[@type=\"narrow\"]";
        //        if (path.startsWith(testPrefix)) {
        //            int i = 0;
        //        }
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
