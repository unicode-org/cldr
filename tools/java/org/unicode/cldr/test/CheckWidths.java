package org.unicode.cldr.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.SupplementalDataInfo;

public class CheckWidths extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*CheckWidths.*
    private static CoverageLevel2 coverageLevel;
    private Level requiredLevel;

    /**
     * Controls for the warning about too many components, and for when to cause error. 
     */
    public static final int WARN_COMPONENTS_PER_ANNOTATION = 7;
    public static final int MAX_COMPONENTS_PER_ANNOTATION = 16;
    
    SupplementalDataInfo supplementalData;

    private static final double EM = ApproximateWidth.getWidth("月");

    private static final boolean DEBUG = true;

    private enum Measure {
        CODE_POINTS, DISPLAY_WIDTH, SET_ELEMENTS
    }

    private enum LimitType {
        MINIMUM, MAXIMUM
    }

    private enum Special {
        NONE, QUOTES, PLACEHOLDERS, NUMBERSYMBOLS, NUMBERFORMAT, BARS
    }

    private static final Pattern PLACEHOLDER_PATTERN = PatternCache.get("\\{\\d\\}");

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
                this.subtype = Subtype.valueTooNarrow;
                switch (measure) {
                case CODE_POINTS:
                    this.message = "Expected no fewer than {0} character(s), but was {1}.";
                    break;
                case DISPLAY_WIDTH:
                    this.message = "Too narrow by about {2}% (with common fonts).";
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                break;
            case MAXIMUM:
                switch (measure) {
                case CODE_POINTS:
                    this.message = "Expected no more than {0} character(s), but was {1}.";
                    this.subtype = Subtype.valueTooWide;
                    break;
                case DISPLAY_WIDTH:
                    this.message = "Too wide by about {2}% (with common fonts).";
                    this.subtype = Subtype.valueTooWide;
                    break;
                case SET_ELEMENTS: 
                    this.message = "Expected no more than {0} items(s), but was {1}.";
                    this.subtype = Subtype.tooManyValues;
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                break;
            default:
                throw new IllegalArgumentException();
            }
        }

        public Limit(double d, double e, Measure displayWidth, LimitType maximum, Special placeholders) {
            this(d, e, displayWidth, maximum, placeholders, false);
        }

        boolean hasProblem(String value, List<CheckStatus> result, CheckCLDR cause, Boolean aliasedAndComprenehsive) {
            switch (special) {
            case NUMBERFORMAT:
                String[] values = value.split(";", 2);
                // If it's a number format with positive and negative subpatterns, just check the longer one.
                value = (values.length == 2 && values[1].length() > values[0].length()) ? values[1] : values[0];
                value = value.replace("'", "");
                break;
            case QUOTES:
                value = value.replace("'", "");
                break;
            case PLACEHOLDERS:
                value = PLACEHOLDER_PATTERN.matcher(value).replaceAll("");
                break;
            case NUMBERSYMBOLS:
                value = value.replaceAll("[\u200E\u200F\u061C]", ""); // don't include LRM/RLM/ALM when checking length of number symbols
                break;
            case BARS:
                value = value.replaceAll("[^|]", "")+"|"; // Check the number of items by counting separators. Bit of a hack...
                break;
            default:
            }
            double valueMeasure = measure == Measure.DISPLAY_WIDTH ? ApproximateWidth.getWidth(value)
                : value.codePointCount(0, value.length()) ;
            CheckStatus.Type errorType = CheckStatus.warningType;
            switch (limit) {
            case MINIMUM:
                if (valueMeasure >= warningReference) {
                    return false;
                }
                if (valueMeasure < errorReference && cause.getPhase() != Phase.BUILD && !aliasedAndComprenehsive) {
                    errorType = CheckStatus.errorType;
                }
                break;
            case MAXIMUM:
                if (valueMeasure <= warningReference) {
                    return false;
                }
                if (valueMeasure > errorReference && cause.getPhase() != Phase.BUILD && !aliasedAndComprenehsive) {
                    // Workaround for ST submission phase only per TC discussion 2018-05-30
                    // Make too many keywords be only a warning until we decide policy (JCE)
                    if (cause.getPhase() == Phase.SUBMISSION && measure.equals(Measure.SET_ELEMENTS)) {
                        errorType = CheckStatus.warningType;
                    } else {
                        errorType = CheckStatus.errorType;
                    }
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
        "|pressure-millimeter-of-mercury" +
        "|speed-mile-per-hour" +
        "|temperature-fahrenheit" +
        "|volume-cubic-mile" +
        "|acceleration-g-force" +
        "|speed-kilometer-per-hour" +
        "|speed-meter-per-second" +
        "|pressure-pound-per-square-inch" +
        ")";

    static final String ALLOW_LONGEST = "consumption-liter-per-100kilometers";

    static RegexLookup<Limit[]> lookup = new RegexLookup<Limit[]>()
        .setPatternTransform(RegexLookup.RegexFinderTransformPath)
        .addVariable("%A", "\"[^\"]+\"")
        .addVariable("%P", "\"[ap]m\"")
        .addVariable("%Q", "[^ap].*|[ap][^m].*") // Anything but am or pm
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

        // Era Abbreviations

        // Allow longer for Japanese calendar eras
        .add("//ldml/dates/calendars/calendar[@type=\"japanese\"]/.*/eraAbbr/era[@type=%A]", new Limit[] {
            new Limit(12 * EM, 16 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })
        // Allow longer for ROC calendar eras
        .add("//ldml/dates/calendars/calendar[@type=\"roc\"]/.*/eraAbbr/era[@type=%A]", new Limit[] {
            new Limit(4 * EM, 8 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })
        .add("//ldml/dates/calendars/calendar.*/eraAbbr/era[@type=%A]", new Limit[] {
            new Limit(3 * EM, 6 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })

        // am/pm abbreviated
        .add("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/.*/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=%P]", new Limit[] {
            new Limit(4 * EM, 6 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })
        // other day periods abbreviated
        .add("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/.*/dayPeriodWidth[@type=\"abbreviated\"]/dayPeriod[@type=%Q]", new Limit[] {
            new Limit(8 * EM, 12 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })
        // am/pm wide
        .add("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/.*/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=%P]", new Limit[] {
            new Limit(5 * EM, 10 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })
        // other day periods wide
        .add("//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/.*/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=%Q]", new Limit[] {
            new Limit(10 * EM, 20 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })

        // Narrow items

        .add("//ldml/dates/calendars/calendar.*[@type=\"narrow\"](?!/cyclic|/dayPeriod|/monthPattern)", new Limit[] {
            new Limit(1.5 * EM, 2.25 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE)
        })
        // \"(?!am|pm)[^\"]+\"\\

        // Compact number formats

        .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1",
            new Limit[] {
                new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NUMBERFORMAT)
        })
        // Catch -future/past Narrow units  and allow much wider values
        .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"[^\"]+-(future|past)\"]/unitPattern", new Limit[] {
            new Limit(10 * EM, 15 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
        })
        // Catch widest units and allow a bit wider
        .add("//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"" + ALLOW_LONGEST + "\"]/unitPattern", new Limit[] {
            new Limit(5 * EM, 6 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDERS)
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
        })

        // "grinning cat face with smiling eyes" should be normal max ~= 160 em
        // emoji names (not keywords)
        .add("//ldml/annotations/annotation[@cp=%A][@type=%A]", new Limit[] {
            new Limit(20 * EM, 100 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NONE),
        })
        .add("//ldml/annotations/annotation[@cp=%A]", new Limit[] {
            new Limit(WARN_COMPONENTS_PER_ANNOTATION, MAX_COMPONENTS_PER_ANNOTATION, Measure.SET_ELEMENTS, LimitType.MAXIMUM, Special.BARS) // Allow up to 5 with no warning, up to 7 with no error.
        })
        ;

    static {
        System.out.println("EMs: " + ApproximateWidth.getWidth("grinning cat face with smiling eyes"));
    }

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
        CLDRFile.Status status = new CLDRFile.Status();
        this.getCldrFileToCheck().getSourceLocaleID(path, status);
        // This was put in specifically to deal with the fact that we added a bunch of new units in CLDR 26
        // and didn't put the narrow forms of them into modern coverage.  If/when the narrow forms of all units
        // are modern coverage, then we can safely remove the aliasedAndComprehensive check.  Right now if an
        // item is aliased and coverage is comprehensive, then it can't generate anything worse than a warning.
        Boolean aliasedAndComprenehsive = (coverageLevel.getLevel(path).compareTo(Level.COMPREHENSIVE) == 0)
            && (status.pathWhereFound.compareTo(path) != 0);
        if (items != null) {
            for (Limit item : items) {
                if (item.hasProblem(value, result, this, aliasedAndComprenehsive)) {
                    if (DEBUG && !found.contains(item)) {
                        found.add(item);
                    }
                    break; // only one error per item
                }
            }
        }
        return this;
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        final String localeID = cldrFileToCheck.getLocaleID();
        supplementalData = SupplementalDataInfo.getInstance(cldrFileToCheck.getSupplementalDirectory());
        coverageLevel = CoverageLevel2.getInstance(supplementalData, localeID);

        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        return this;
    }
}
