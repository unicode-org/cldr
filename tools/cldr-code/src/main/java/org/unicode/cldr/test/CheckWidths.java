package org.unicode.cldr.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.ApproximateWidth;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.Validity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;

public class CheckWidths extends CheckCLDR {
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*CheckWidths.*
    private static CoverageLevel2 coverageLevel;
    private Level requiredLevel;

    private static UnitWidthUtil UNIT_WIDTHS_UTIL = UnitWidthUtil.getInstance();

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
        NONE, QUOTES, PLACEHOLDERS, NUMBERSYMBOLS, NUMBERFORMAT, BARS, PLACEHOLDER_UNITS
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

        boolean hasProblem(String path, String value, List<CheckStatus> result, CheckCLDR cause, Boolean aliasedAndComprehensive) {
            double factor = 1d;
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
            case PLACEHOLDER_UNITS:
                factor = UNIT_WIDTHS_UTIL.getRoughComponentMax(path);
                // fall through ok
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
                if (valueMeasure < errorReference
                	&& cause.getPhase() != Phase.BUILD
                	&& !aliasedAndComprehensive) {
                    errorType = CheckStatus.errorType;
                }
                break;
            case MAXIMUM:
                if (valueMeasure <= warningReference * factor) {
                    return false;
                }
                if (valueMeasure > errorReference * factor
                	&& cause.getPhase() != Phase.BUILD
                	&& !aliasedAndComprehensive) {
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
// pattern[@type="100000000000000"]
        .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"100000000000000",
            new Limit[] {
                new Limit(4 * EM, 6 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NUMBERFORMAT)
        })
        .add("//ldml/numbers/decimalFormats[@numberSystem=%A]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=%A]/pattern[@type=\"1",
            new Limit[] {
                new Limit(4 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.NUMBERFORMAT)
        })

        // Short/Narrow units
        // Note that the EM values are adjusted for units according to the number of components in the units
        // See UnitWidthUtil for more information
        .add("//ldml/units/unitLength[@type=\"(short|narrow)\"]/unit[@type=%A]/unitPattern", new Limit[] {
            new Limit(3 * EM, 5 * EM, Measure.DISPLAY_WIDTH, LimitType.MAXIMUM, Special.PLACEHOLDER_UNITS)
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

// Quell noisy printout
//    static {
//        System.out.println("EMs: " + ApproximateWidth.getWidth("grinning cat face with smiling eyes"));
//    }

    Set<Limit> found = new LinkedHashSet<>();

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
            CLDRFile.Status status = new CLDRFile.Status();
            this.getCldrFileToCheck().getSourceLocaleID(path, status);
            // This was put in specifically to deal with the fact that we added a bunch of new units in CLDR 26
            // and didn't put the narrow forms of them into modern coverage.  If/when the narrow forms of all units
            // are modern coverage, then we can safely remove the aliasedAndComprehensive check.  Right now if an
            // item is aliased and coverage is comprehensive, then it can't generate anything worse than a warning.
            Boolean aliasedAndComprehensive = (coverageLevel.getLevel(path).compareTo(Level.COMPREHENSIVE) == 0)
            && (status.pathWhereFound.compareTo(path) != 0);
            for (Limit item : items) {
                if (item.hasProblem(path, value, result, this, aliasedAndComprehensive)) {
                    if (DEBUG && !found.contains(item)) {
                        found.add(item);
                    }
                    break; // only one error per item
                }
            }
        }
        return this;
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        final String localeID = cldrFileToCheck.getLocaleID();
        supplementalData = SupplementalDataInfo.getInstance(cldrFileToCheck.getSupplementalDirectory());
        coverageLevel = CoverageLevel2.getInstance(supplementalData, localeID);

        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        return this;
    }

    /**
     * Provide a rough measure of how many unit components there are for the purpose of establishing a maximum width, with an special factor for non-metric.
     */
    public static class UnitWidthUtil {
        static final Pattern UNIT_PREFIX = Pattern.compile("//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"([^\\\"]*)\"]");
        final UnitConverter CONVERTER = SupplementalDataInfo.getInstance().getUnitConverter();
        final Set<String> validLongUnitIDs = Validity.getInstance().getCodeToStatus(LstrType.unit).keySet();

        LoadingCache<String, Double> pathToUnitComponents = CacheBuilder.newBuilder().build(
            new CacheLoader<String, Double>() {
            @Override
            public Double load(String path) throws ExecutionException {
                final Matcher matcher = UNIT_PREFIX.matcher(path);
                if (matcher.lookingAt()) {
                    //String length = matcher.group(1);
                    String longUnitId = matcher.group(2);
                    return unitToComponents.get(longUnitId);
                } else {
                    throw new ICUException("Internal error");
                }
            }
        });

        LoadingCache<String, Double> unitToComponents = CacheBuilder.newBuilder().build(new CacheLoader<String, Double>() {
            @Override
            public Double load(String longUnitId) {
                double components = 0;
                String shortId = CONVERTER.getShortId(longUnitId);

                Set<String> systems = CONVERTER.getSystems(shortId);
                int widthFactor = systems.contains("metric") &&  !shortId.endsWith("-metric") ? 1 : 3;
                // NOTE: allow cup-metric and pint-metric to be longer, since they aren't standard metric

                // walk thorough the numerator and denominator to get the values
                UnitId unitId = CONVERTER.createUnitId(shortId);
                for (Entry<String, Integer> entry : unitId.numUnitsToPowers.entrySet()) {
                    components += getComponentCount(entry.getKey(), entry.getValue());
                }
                for (Entry<String, Integer> entry : unitId.denUnitsToPowers.entrySet()) {
                    components += getComponentCount(entry.getKey(), entry.getValue());
                }
                return widthFactor * components;
            }

            public double getComponentCount(String unit, Integer power) {
                int result = 1;
                if (power > 1) {
                    ++result; // add one component for a power
                }
                // hack for number
                if (unit.startsWith("100-")) {
                    ++result;
                    unit = unit.substring(4);
                }
                Output<Rational> deprefix = new Output<>();
                unit = UnitConverter.stripPrefix(unit, deprefix);
                if (!deprefix.value.equals(Rational.ONE)) {
                    ++result; // add 1 component for kilo, mega, etc.
                }
                for (int i = 0; i < unit.length(); ++i) {
                    if (unit.charAt(i) == '-') {
                        ++result; // add one component for -imperial, etc.
                    }
                }
                return result;
            }
        });

        private UnitWidthUtil() { }

        public static UnitWidthUtil getInstance() {
            return new UnitWidthUtil();
        }

        public double getRoughComponentMax(String path) {
            try {
                return pathToUnitComponents.get(path);
            } catch (ExecutionException e) {
                throw new ICUException(e);
            }
        }
    }
}
