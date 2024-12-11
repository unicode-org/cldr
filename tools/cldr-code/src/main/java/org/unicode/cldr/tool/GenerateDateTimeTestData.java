package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.NumberingSystem;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CalculatedCoverageLevels;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TempPrintWriter;

public class GenerateDateTimeTestData {

    private static final String OUTPUT_SUBDIR = "datetime";

    private static final String OUTPUT_FILENAME = "datetime.json";

    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            SupplementalDataInfo.getInstance();

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();

    private static final Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * The known set of values used to indicate the type of "glue pattern" aka the dateTimeFormat
     * type.
     *
     * <p>atTime = the word "at" is inserted between the date and time when formatting both date &
     * time together.
     *
     * <p>standard = do not insert the word "at". (ex: in `en`, there may or may not be a comma
     * instead to separate)
     */
    private static final Set<String> CLDR_DATE_TIME_FORMAT_TYPES =
            ImmutableSet.of("standard", "atTime");

    private static final ImmutableSet<String> NUMBERING_SYSTEMS =
            ImmutableSet.of("latn", "arab", "beng");

    // Use underscores for locale id b/c of CLDR historical reasons, even though dash is preferable
    // to underscore.
    private static final ImmutableSet<String> LOCALES =
            ImmutableSet.of(
                    "en_US", "en_GB", "zh_Hant_TW", "vi", "ar", "mt_MT", "bn", "zu"
                    // "root"
                    );

    private static final ImmutableMap.Builder<Object, Object> newMapStringToObjectBuilder() {
        return ImmutableMap.builder();
    }

    private static final ImmutableMap.Builder<Object, ImmutableMap<Object, Object>>
            newMapStringToMapBuilder() {
        return ImmutableMap.builder();
    }

    private static final ImmutableSet<ImmutableMap<Object, Object>> FIELD_STYLE_COMBINATIONS =
            ImmutableSet.of(
                    newMapStringToObjectBuilder()
                            .put("dateLength", "short")
                            .put("timeLength", "short")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("dateLength", "medium")
                            .put("timeLength", "medium")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("dateLength", "long")
                            .put("timeLength", "long")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("dateLength", "full")
                            .put("timeLength", "full")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("dateLength", "full")
                            .put("timeLength", "short")
                            .build(),
                    newMapStringToObjectBuilder().put("dateLength", "long").build(),
                    newMapStringToObjectBuilder().put("timeLength", "long").build(),
                    newMapStringToObjectBuilder().put("hour", "numeric").build(),
                    newMapStringToObjectBuilder()
                            .put("hour", "numeric")
                            .put("minute", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("hour", "numeric")
                            .put("minute", "numeric")
                            .put("second", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("hour", "numeric")
                            .put("minute", "numeric")
                            .put("second", "numeric")
                            .put("fractionalSecondDigits", 1)
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("hour", "numeric")
                            .put("minute", "numeric")
                            .put("second", "numeric")
                            .put("fractionalSecondDigits", 2)
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("hour", "numeric")
                            .put("minute", "numeric")
                            .put("second", "numeric")
                            .put("fractionalSecondDigits", 3)
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("hour", "numeric")
                            .put("minute", "numeric")
                            .put("second", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("month", "numeric")
                            .put("weekday", "long")
                            .put("day", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("month", "2-digit")
                            .put("weekday", "long")
                            .put("day", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("month", "long")
                            .put("weekday", "long")
                            .put("day", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("month", "short")
                            .put("weekday", "long")
                            .put("day", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("month", "narrow")
                            .put("weekday", "long")
                            .put("day", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("month", "short")
                            .put("weekday", "short")
                            .put("day", "numeric")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("month", "short")
                            .put("weekday", "narrow")
                            .put("day", "numeric")
                            .build(),

                    // TODO: remove non-semantic skeletons
                    newMapStringToObjectBuilder().put("era", "long").build(),
                    newMapStringToObjectBuilder().put("era", "short").build(),
                    newMapStringToObjectBuilder().put("era", "narrow").build(),
                    newMapStringToObjectBuilder().put("timeZoneName", "long").build(),
                    newMapStringToObjectBuilder().put("timeZoneName", "short").build(),
                    newMapStringToObjectBuilder().put("timeZoneName", "shortOffset").build(),
                    newMapStringToObjectBuilder().put("timeZoneName", "longOffset").build(),
                    newMapStringToObjectBuilder().put("timeZoneName", "shortGeneric").build(),
                    newMapStringToObjectBuilder().put("timeZoneName", "longGeneric").build());

    // Note: CLDR uses the identifier "gregorian", although BCP-47 which came later specifies
    // "gregory" as the calendar identifier.
    private static final ImmutableSet<String> CALENDARS =
            ImmutableSet.of(
                    "gregorian",
                    "buddhist",
                    "hebrew",
                    "chinese",
                    "roc",
                    "japanese",
                    "islamic",
                    "islamic-umalqura",
                    "persian");

    private static final ImmutableSet<String> TIME_ZONES =
            ImmutableSet.of(
                    "America/Los_Angeles",
                    "Africa/Luanda",
                    "Asia/Tehran",
                    "Europe/Kiev",
                    "Australia/Brisbane",
                    "Pacific/Palau",
                    "America/Montevideo");

    private static final ImmutableSet<ZonedDateTime> JAVA_TIME_ZONED_DATE_TIMES =
            ImmutableSet.of(
                    LocalDate.parse("2024-03-17")
                            .atStartOfDay(ZoneId.of(ZoneOffset.UTC.getId())), // "Mar 17, 2024"
                    Instant.parse("2001-07-02T13:14:15.00Z")
                            .atZone(ZoneOffset.UTC), // "02-Jul-2001, 13:14:15"
                    Instant.parse("1984-05-29T07:53:00.00Z")
                            .atZone(ZoneOffset.UTC), // "May 29, 1984, 7:53"
                    Instant.parse("2050-05-29T16:47:00.00Z")
                            .atZone(ZoneOffset.UTC), // "2050, May 29, 16:47"
                    LocalDate.parse("1969-07-16")
                            .atStartOfDay(ZoneId.of(ZoneOffset.UTC.getId())), // "1969, July 16"
                    Instant.ofEpochMilli(1_000_000_000L).atZone(ZoneOffset.UTC), // 1e9
                    Instant.ofEpochMilli(1_000_000_000_000L).atZone(ZoneOffset.UTC) // 1e12
                    );

    private static final ImmutableSet<ImmutableMap<Object, Object>> TEMPORAL_DATES =
            ImmutableSet.of(
                    newMapStringToObjectBuilder()
                            .put("timeZone", "America/Los_Angeles")
                            .put("year", 2024)
                            .put("month", 3)
                            .put("day", 7)
                            .put("hour", 0)
                            .put("minute", 0)
                            .put("second", 1)
                            .put("millisecond", 0)
                            .put("microsecond", 0)
                            .put("nanosecond", 0)
                            .put("calendar", "gregory")
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("timeZone", "America/Los_Angeles")
                            .put("year", 2001)
                            .put("month", 7)
                            .put("day", 2)
                            .put("hour", 13)
                            .put("minute", 14)
                            .put("second", 15)
                            .put("millisecond", 0)
                            .put("microsecond", 0)
                            .put("nanosecond", 0)
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("timeZone", "America/Los_Angeles")
                            .put("year", 1984)
                            .put("month", 5)
                            .put("day", 29)
                            .put("hour", 7)
                            .put("minute", 53)
                            .put("second", 0)
                            .put("millisecond", 0)
                            .put("microsecond", 0)
                            .put("nanosecond", 0)
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("timeZone", "America/Los_Angeles")
                            .put("year", 2030)
                            .put("month", 5)
                            .put("day", 29)
                            .put("hour", 16)
                            .put("minute", 47)
                            .put("second", 0)
                            .put("millisecond", 0)
                            .put("microsecond", 0)
                            .put("nanosecond", 0)
                            .build(),
                    newMapStringToObjectBuilder()
                            .put("timeZone", "America/Los_Angeles")
                            .put("year", 1969)
                            .put("month", 7)
                            .put("day", 16)
                            .put("hour", 0)
                            .put("minute", 0)
                            .put("second", 0)
                            .put("millisecond", 0)
                            .put("microsecond", 0)
                            .put("nanosecond", 0)
                            .build(),
                    newMapStringToObjectBuilder() // 1e6
                            .put("timeZone", "America/Los_Angeles")
                            .put("year", 1970)
                            .put("month", 1)
                            .put("day", 12)
                            .put("hour", 13)
                            .put("minute", 46)
                            .put("second", 40)
                            .put("millisecond", 0)
                            .put("microsecond", 0)
                            .put("nanosecond", 0)
                            .build(),
                    newMapStringToObjectBuilder() // 1e9
                            .put("timeZone", "America/Los_Angeles")
                            .put("year", 2001)
                            .put("month", 9)
                            .put("day", 9)
                            .put("hour", 1)
                            .put("minute", 46)
                            .put("second", 40)
                            .put("millisecond", 0)
                            .put("microsecond", 0)
                            .put("nanosecond", 0)
                            .build());

    private static final ImmutableMap<Object, ImmutableMap<Object, Object>>
            FIELD_STYLE_TO_SKELETON =
                    newMapStringToMapBuilder()
                            .put(
                                    "era",
                                    newMapStringToObjectBuilder()
                                            .put("long", "GGGG")
                                            .put("short", "GG")
                                            .put("narrow", "GGGGG")
                                            .build())
                            .put(
                                    "year",
                                    newMapStringToObjectBuilder()
                                            .put("numeric", "y")
                                            .put("2-digit", "yy")
                                            .build())
                            .put(
                                    "quarter",
                                    newMapStringToObjectBuilder().put("numeric", "q").build())
                            .put(
                                    "month",
                                    newMapStringToObjectBuilder()
                                            .put("numeric", "M")
                                            .put("2-digit", "MM")
                                            .put("long", "MMMM")
                                            .put("short", "MMM")
                                            .put("narrow", "MMMMM")
                                            .build())
                            .put(
                                    "weekday",
                                    newMapStringToObjectBuilder()
                                            .put("long", "EEEE")
                                            .put("short", "E")
                                            .put("narrow", "EEEEE")
                                            .build())
                            .put(
                                    "day",
                                    newMapStringToObjectBuilder()
                                            .put("numeric", "d")
                                            .put("2-digit", "dd")
                                            .build())
                            .put("hour", newMapStringToObjectBuilder().put("numeric", "j").build())
                            .put(
                                    "minute",
                                    newMapStringToObjectBuilder().put("numeric", "m").build())
                            .put(
                                    "second",
                                    newMapStringToObjectBuilder().put("numeric", "s").build())
                            .put(
                                    "fractionalSecondDigits",
                                    newMapStringToObjectBuilder()
                                            .put(1, "S")
                                            .put(2, "SS")
                                            .put(3, "SSS")
                                            .build())
                            .put(
                                    "timeZoneName",
                                    newMapStringToObjectBuilder()
                                            .put("short", "z")
                                            .put("long", "zzzz")
                                            .put("shortOffset", "O")
                                            .put("longOffset", "OOOO")
                                            .put("shortGeneric", "v")
                                            .put("longGeneric", "vvvv")
                                            .build())
                            .build();

    private static String fieldStyleCombinationToSkeleton(Map<Object, Object> styleCombination) {
        List<String> skeletonArray = new ArrayList<>();

        for (Object dtField : styleCombination.keySet()) {
            if (dtField.equals("dateLength") || dtField.equals("timeLength")) {
                continue;
            }
            if (FIELD_STYLE_TO_SKELETON.containsKey(dtField)) {
                String styleValue = (String) styleCombination.get(dtField);
                Map<Object, Object> styleToSkeleton = FIELD_STYLE_TO_SKELETON.get(dtField);
                if (styleToSkeleton.containsKey(styleValue)) {
                    skeletonArray.add(styleValue);
                }
            }
        }

        return String.join("", skeletonArray);
    }

    private static Set<String> getLocaleNumberingSystems(CLDRFile localeFile) {
        Set<String> result = new HashSet<>();
        for (NumberingSystem system : NumberingSystem.values()) {
            String numberingSystem =
                    system.path == null ? "latn" : localeFile.getStringValue(system.path);
            if (numberingSystem != null) {
                result.add(numberingSystem);
            }
        }

        return result;
    }

    private static ZonedDateTime getZonedDateTimeFromTemporalDateInput(Map<Object, Object> input) {
        if (!input.containsKey("year")) {
            return null;
        }
        if (!input.containsKey("month")) {
            return null;
        }
        if (!input.containsKey("day")) {
            return null;
        }

        // TODO: Use ICU Calendar object to get a date time instant with a calendar
        if (input.containsKey("calendar") && !((String) input.get("calendar")).equals("gregory")) {
            return null;
        }

        int year = (int) input.get("year");
        int monthInt = (int) input.get("month");
        int day = (int) input.get("day");
        Month month = Month.of(monthInt);
        int hour = (int) input.getOrDefault("hour", 0);
        int minute = (int) input.getOrDefault("minute", 0);
        int second = (int) input.getOrDefault("second", 0);
        int millisecond = (int) input.getOrDefault("millisecond", 0);
        int microsecond = (int) input.getOrDefault("microsecond", 0);
        int nanosecondOrig = (int) input.getOrDefault("nanosecond", 0);
        int nanosecond = ((millisecond * 1000) + microsecond) * 1000 + nanosecondOrig;
        String timeZoneIdStr = (String) input.getOrDefault("timeZone", "UTC");
        ZoneId timeZoneId = ZoneId.of(timeZoneIdStr);

        LocalDateTime localDt =
                LocalDateTime.of(year, month, day, hour, minute, second, nanosecond);
        return ZonedDateTime.of(localDt, timeZoneId);
    }

    private static final Optional<CLDRFile> getCLDRFile(String locale) {
        CLDRFile cldrFile =
                CLDR_FACTORY.make(
                        locale, true, DraftStatus.contributed); // don't include provisional data

        // This is the CLDR "effective coverage level"
        Level coverageLevel =
                CalculatedCoverageLevels.getInstance().getEffectiveCoverageLevel(locale);

        if (coverageLevel == null || !coverageLevel.isAtLeast(Level.MODERN)) {
            return Optional.empty();
        } else {
            return Optional.of(cldrFile);
        }
    }

    private static String getDateLength(Map<Object, Object> optionsMap) {
        String length = null;
        if (optionsMap.containsKey("dateLength")) {
            length = (String) optionsMap.get("dateLength");
        }

        return length;
    }

    private static String getTimeLength(Map<Object, Object> optionsMap) {
        String length = null;
        if (optionsMap.containsKey("timeLength")) {
            length = (String) optionsMap.get("timeLength");
        }

        return length;
    }

    /**
     * Take a map of test case inputs, compute the formatted datetime, and return a map that
     * includes the formatted datetime as well as all of the test case inputs.
     *
     * @param icuServiceBuilder
     * @param localeCldrFile
     * @param options
     * @param optionsBuilder
     * @param zdt
     * @param calendar
     * @param dateFormatter
     * @param timeFormatter
     * @param dateLength
     * @return
     */
    private static List<ImmutableMap<Object, Object>> getTestCasesForZonedDateTime(
            ICUServiceBuilder icuServiceBuilder,
            CLDRFile localeCldrFile,
            ImmutableMap<Object, Object> options,
            ImmutableMap.Builder<Object, Object> optionsBuilder,
            ZonedDateTime zdt,
            String calendar,
            SimpleDateFormat dateFormatter,
            SimpleDateFormat timeFormatter,
            String dateLength) {
        String formattedDateTime;

        // "input" = the ISO 18601 UTC time zone formatted string of the zoned date time
        optionsBuilder.put("input", zdt.toString());

        // After all the options configuration, finally construct the formatted DateTime
        if (dateFormatter == null) {
            formattedDateTime = timeFormatter.format(zdt);
            // Reuse and update the optionsBuilder to insert the expected value according to
            // the result of the CLDR formatter
            optionsBuilder.put("expected", formattedDateTime);
            return ImmutableList.of(optionsBuilder.buildKeepingLast());
        } else if (timeFormatter == null) {
            formattedDateTime = dateFormatter.format(zdt);
            // Reuse and update the optionsBuilder to insert the expected value according to
            // the result of the CLDR formatter
            optionsBuilder.put("expected", formattedDateTime);
            return ImmutableList.of(optionsBuilder.buildKeepingLast());
        } else {
            String formattedDate = dateFormatter.format(zdt);
            String formattedTime = timeFormatter.format(zdt);

            ImmutableList.Builder resultBuilder = ImmutableList.builder();

            // when we have date and time formatting information, then we also need to use
            // the date time "glue pattern" aka dateTimeFormatType, which can vary over
            // different values (ex: "standard", "atTime"), that indicates the style of pattern
            // that is used to combine the date and time formatted values together.
            for (String dateTimeGluePatternFormatType : CLDR_DATE_TIME_FORMAT_TYPES) {
                formattedDateTime =
                        localeCldrFile.glueDateTimeFormat(
                                formattedDate,
                                formattedTime,
                                calendar,
                                dateLength,
                                dateTimeGluePatternFormatType,
                                icuServiceBuilder);
                optionsBuilder.put("dateTimeFormatType", dateTimeGluePatternFormatType);
                // Reuse and update the optionsBuilder to insert the expected value according to
                // the result of the CLDR formatter
                optionsBuilder.put("expected", formattedDateTime);
                resultBuilder.add(optionsBuilder.buildKeepingLast());
            }

            return resultBuilder.build();
        }
    }

    private static Collection<Map<Object, Object>> generateAllTestCases() {

        List<Map<Object, Object>> result = new LinkedList<>();

        // locale iteration

        for (String localeStr : LOCALES) {
            ULocale locale =
                    new ULocale(localeStr); // constructor that uses underscores for locale id
            CLDRFile localeCldrFile = getCLDRFile(localeStr).orElse(null);

            if (localeCldrFile == null) {
                continue;
            }

            ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
            icuServiceBuilder.clearCache();
            icuServiceBuilder.setCldrFile(localeCldrFile);

            // calendar iteration

            ULocale maximizedLoc = ULocale.addLikelySubtags(locale);
            String region = maximizedLoc.getCountry();
            List<String> localePreferredCalendars = SUPPLEMENTAL_DATA_INFO.getCalendars(region);

            if (localePreferredCalendars == null) {
                localePreferredCalendars = SUPPLEMENTAL_DATA_INFO.getCalendars("001");
            }

            for (String calendar : CALENDARS) {
                if (!localePreferredCalendars.contains(calendar)) {
                    continue;
                }

                // field style combinations iterations

                for (Map<Object, Object> fieldStyleCombo : FIELD_STYLE_COMBINATIONS) {
                    // Chinese calendar doesn't have era.
                    if (fieldStyleCombo.containsKey("era") && calendar.equals("chinese")) {
                        continue;
                    }
                    // TODO: support test cases with skeletons or semantic skeletons
                    if (!fieldStyleCombo.containsKey("dateLength")
                            && !fieldStyleCombo.containsKey("timeLength")) {
                        continue;
                    }

                    // time zone iteration

                    for (String timeZone : TIME_ZONES) {

                        String skeleton = fieldStyleCombinationToSkeleton(fieldStyleCombo);

                        // construct the final set of formatter options

                        ImmutableMap.Builder<Object, Object> optionsBuilder =
                                ImmutableMap.builder().putAll(fieldStyleCombo);
                        if (!calendar.isEmpty()) {
                            optionsBuilder.put("calendar", calendar);
                        }
                        optionsBuilder.put("locale", locale.toLanguageTag());
                        ImmutableMap<Object, Object> options = optionsBuilder.build();

                        TimeZone icuTimeZone = TimeZone.getTimeZone(timeZone);

                        String dateLength = getDateLength(options);
                        SimpleDateFormat dateFormatter =
                                localeCldrFile.getDateFormat(
                                        calendar, dateLength, icuServiceBuilder);
                        if (dateFormatter != null) {
                            dateFormatter.setTimeZone(icuTimeZone);
                        }

                        String timeLength = getTimeLength(options);
                        SimpleDateFormat timeFormatter =
                                localeCldrFile.getTimeFormat(
                                        calendar, timeLength, icuServiceBuilder);
                        if (timeFormatter != null) {
                            timeFormatter.setTimeZone(icuTimeZone);
                        }

                        // iterate over all dates and format the date time

                        ZoneId zoneId = ZoneId.of(timeZone);

                        for (ZonedDateTime zdt : JAVA_TIME_ZONED_DATE_TIMES) {
                            ZonedDateTime zdtNewTz = zdt.withZoneSameInstant(zoneId);
                            List<ImmutableMap<Object, Object>> testCases =
                                    getTestCasesForZonedDateTime(
                                            icuServiceBuilder,
                                            localeCldrFile,
                                            options,
                                            optionsBuilder,
                                            zdtNewTz,
                                            calendar,
                                            dateFormatter,
                                            timeFormatter,
                                            dateLength);
                            result.addAll(testCases);
                        }

                        for (Map<Object, Object> temporalDateInfo : TEMPORAL_DATES) {
                            ZonedDateTime zdt =
                                    getZonedDateTimeFromTemporalDateInput(temporalDateInfo);
                            ZonedDateTime zdtNewTz = zdt.withZoneSameInstant(zoneId);
                            List<ImmutableMap<Object, Object>> testCases =
                                    getTestCasesForZonedDateTime(
                                            icuServiceBuilder,
                                            localeCldrFile,
                                            options,
                                            optionsBuilder,
                                            zdtNewTz,
                                            calendar,
                                            dateFormatter,
                                            timeFormatter,
                                            dateLength);
                            result.addAll(testCases);
                        }
                    }
                }
            }
        }

        return result;
    }


    enum DateStyle {
        SHORT,
        MEDIUM,
        LONG,
        FULL
    }

    enum TimeStyle {
        SHORT,
        MEDIUM,
        LONG,
        FULL
    }

    enum SemanticSkeletonLength {
        SHORT,
        MEDIUM,
        LONG
    }

    enum SemanticSkeleton {
        YMDE,
        MDTZ,
        M,
        T,
        Z
    }

    enum HourCycle {
        H12,
        H23
    }

    enum YearStyle {
        AUTO,
        WITH_ERA,
    }

    enum ZoneStyle {
        SPECIFIC,
        GENERIC,
        LOCATION,
        OFFSET,
    }


    /**
     * A struct to contain combinations of datetime fields & styles, mainly to allow an enumeration
     * of combinations of values for these fields that yields a similarly thorough coverage of
     * the test space without having to compute the full Cartesian product of all values of all
     * dimensions possible.
     */
    static class FieldStyleCombo {
        SemanticSkeleton semanticSkeleton;
        SemanticSkeletonLength semanticSkeletonLength;
        DateStyle dateStyle;
        TimeStyle timeStyle;
        HourCycle hourCycle;
        ZoneStyle zoneStyle;
        YearStyle yearStyle;
    }

    /**
     * A struct to contain the data to be used to generate combinations of datetime fields & styles
     * and whether to combine (obtain the Cartesian product of) them by other dimensions
     */
    static class FieldStyleComboInput {
        FieldStyleCombo fieldStyleCombo;
        boolean shouldMultiplyByTimeZone;
        boolean shouldMultiplyByDateTime;
    }

    private static ImmutableSet<FieldStyleComboInput> getFieldStyleComboInputs() {
        ImmutableSet.Builder<FieldStyleComboInput> builder = ImmutableSet.builder();

        FieldStyleComboInput elem;

        // 1 (Row 2)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo.timeStyle = TimeStyle.SHORT;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 2 (Row 3)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.dateStyle = DateStyle.MEDIUM;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 3
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.dateStyle = DateStyle.FULL;
        elem.fieldStyleCombo.timeStyle = TimeStyle.SHORT;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 4
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.dateStyle = DateStyle.SHORT;
        elem.fieldStyleCombo.timeStyle = TimeStyle.FULL;
        elem.shouldMultiplyByTimeZone = true;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 5 (Row 6)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.YMDE;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.YMDE;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.MEDIUM;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.YMDE;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.YMDE;
        elem.fieldStyleCombo.yearStyle = YearStyle.WITH_ERA;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.YMDE;
        elem.fieldStyleCombo.yearStyle = YearStyle.WITH_ERA;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.MEDIUM;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 10 (Row 11)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.YMDE;
        elem.fieldStyleCombo.yearStyle = YearStyle.WITH_ERA;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.SPECIFIC;
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.Z;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.LOCATION;
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.Z;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.GENERIC;
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.Z;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.OFFSET;
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.Z;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        // 15 (Row 16)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.SPECIFIC;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.SPECIFIC;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.LOCATION;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.LOCATION;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.GENERIC;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        // 20 (Row 21)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.GENERIC;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.OFFSET;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.MDTZ;
        elem.fieldStyleCombo.zoneStyle = ZoneStyle.OFFSET;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByTimeZone = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.M;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.T;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 25 (Row 26)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.T;
        elem.fieldStyleCombo.hourCycle = HourCycle.H12;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.T;
        elem.fieldStyleCombo.hourCycle = HourCycle.H12;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.MEDIUM;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.T;
        elem.fieldStyleCombo.hourCycle = HourCycle.H12;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 28 (Row 29)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.T;
        elem.fieldStyleCombo.hourCycle = HourCycle.H23;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.LONG;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        return builder.build();
    }

    static class FieldStyleComboWithTZAndDateTime {
        FieldStyleCombo fieldStyleCombo;
        LocalDateTime dateTime;
        TimeZone timeZone;
    }

    static class TestCase {
        ULocale locale;
        Calendar calendar;
        FieldStyleComboWithTZAndDateTime fieldStyleComboWithTZAndDateTime;
    }

    public static ImmutableSet<TestCase> getKernelTestCases() {

        // more manually defined inputs

        List<Pair<ULocale, Calendar>> LOCALE_CALENDAR_PAIRS = List.of(
            Pair.of(ULocale.ENGLISH, GregorianCalendar.getInstance()),
            Pair.of(ULocale.forLanguageTag("ar-SA"), IslamicCalendar.getInstance()),
            Pair.of(ULocale.forLanguageTag("th-TH"), BuddhistCalendar.getInstance()),
            Pair.of(ULocale.forLanguageTag("ja-JP"), JapaneseCalendar.getInstance())
        );

        List<LocalDateTime> DATE_TIMES = List.of(
            LocalDateTime.of(2024, 7, 1, 8, 50, 7),
            LocalDateTime.of(2000, 1, 1, 0, 0, 0),
            LocalDateTime.of(2014, 7, 15, 12, 0, 0)
        );

        // An extra time zone that should be added dynamically is the default for the locale
        // visited during the iteration over locales
        List<TimeZone> STATIC_TIME_ZONES = List.of(
            TimeZone.GMT_ZONE,
            TimeZone.getTimeZone("Australia/Adelaide")
        );

        // setup of return value

        ImmutableSet.Builder<TestCase> builder = ImmutableSet.builder();

        // iteration to add to return value

        for (Pair<ULocale,Calendar> localeCalendarPair : LOCALE_CALENDAR_PAIRS) {
            ULocale locale = localeCalendarPair.first;
            Calendar calendar = localeCalendarPair.second;

            for (FieldStyleComboInput input : getFieldStyleComboInputs()) {
                assert input.shouldMultiplyByDateTime || input.shouldMultiplyByTimeZone;

                FieldStyleCombo fieldStyleCombo = input.fieldStyleCombo;

                List<LocalDateTime> dateTimeIterationColl = List.of();
                if (input.shouldMultiplyByDateTime) {
                    dateTimeIterationColl = DATE_TIMES;
                }

                List<TimeZone> timeZoneIterationColl = List.of();
                if (input.shouldMultiplyByTimeZone) {
                    timeZoneIterationColl = STATIC_TIME_ZONES;
                    // TODO: add a TimeZone from the region of the locale to the list of time zones to iterate over
                }

                for (LocalDateTime localDateTime : dateTimeIterationColl) {
                    for (TimeZone timeZone : timeZoneIterationColl) {
                        FieldStyleComboWithTZAndDateTime fieldStyleComboWithTZAndDateTime = new FieldStyleComboWithTZAndDateTime();
                        fieldStyleComboWithTZAndDateTime.fieldStyleCombo = fieldStyleCombo;
                        fieldStyleComboWithTZAndDateTime.dateTime = localDateTime;
                        fieldStyleComboWithTZAndDateTime.timeZone = timeZone;

                        TestCase testCase = new TestCase();
                        testCase.locale = locale;
                        testCase.calendar = calendar;
                        testCase.fieldStyleComboWithTZAndDateTime = fieldStyleComboWithTZAndDateTime;

                        builder.add(testCase);
                    }
                }
            }
        }

        return builder.build();
    }

    public static void main(String[] args) throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(
                        CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, OUTPUT_FILENAME)) {

            Collection<Map<Object, Object>> testCases = generateAllTestCases();
            pw.println(GSON.toJson(testCases));
        }
    }
}
