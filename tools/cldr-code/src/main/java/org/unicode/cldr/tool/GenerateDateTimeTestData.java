package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.icu.text.SimpleDateFormat;
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
        } else if (optionsMap.containsKey("timeLength")) {
            length = (String) optionsMap.get("timeLength");
        }

        return length;
    }

    private static String getTimeLength(Map<Object, Object> optionsMap) {
        String length = null;
        if (optionsMap.containsKey("timeLength")) {
            length = (String) optionsMap.get("timeLength");
        } else if (optionsMap.containsKey("dateLength")) {
            length = (String) optionsMap.get("dateLength");
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
    private static ImmutableMap<Object, Object> getTestCaseForZonedDateTime(
            ICUServiceBuilder icuServiceBuilder,
            CLDRFile localeCldrFile,
            ImmutableMap<Object, Object> options,
            ImmutableMap.Builder<Object, Object> optionsBuilder,
            ZonedDateTime zdt,
            String calendar,
            SimpleDateFormat dateFormatter,
            SimpleDateFormat timeFormatter,
            String dateLength) {

        // After all the options configuration, finally construct the formatted DateTime
        String formattedDate = dateFormatter.format(zdt);
        String formattedTime = timeFormatter.format(zdt);
        String dateTimeGluePatternFormatType = "standard";
        String formattedDateTime =
                localeCldrFile.glueDateTimeFormat(
                        formattedDate,
                        formattedTime,
                        calendar,
                        dateLength,
                        dateTimeGluePatternFormatType,
                        icuServiceBuilder);

        // "input" = the ISO 18601 UTC time zone formatted string of the zoned date time
        optionsBuilder.put("input", zdt.toString());
        // Reuse and update the optionsBuilder to insert the expected value according to
        // the result of the CLDR formatter
        optionsBuilder.put("expected", formattedDateTime);

        return optionsBuilder.buildKeepingLast();
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
                        dateFormatter.setTimeZone(icuTimeZone);

                        String timeLength = getTimeLength(options);
                        SimpleDateFormat timeFormatter =
                                localeCldrFile.getTimeFormat(
                                        calendar, timeLength, icuServiceBuilder);
                        timeFormatter.setTimeZone(icuTimeZone);

                        // iterate over all dates and format the date time

                        ZoneId zoneId = ZoneId.of(timeZone);

                        for (ZonedDateTime zdt : JAVA_TIME_ZONED_DATE_TIMES) {
                            ZonedDateTime zdtNewTz = zdt.withZoneSameInstant(zoneId);
                            ImmutableMap<Object, Object> testCase =
                                    getTestCaseForZonedDateTime(
                                            icuServiceBuilder,
                                            localeCldrFile,
                                            options,
                                            optionsBuilder,
                                            zdtNewTz,
                                            calendar,
                                            dateFormatter,
                                            timeFormatter,
                                            dateLength);
                            result.add(testCase);
                        }

                        for (Map<Object, Object> temporalDateInfo : TEMPORAL_DATES) {
                            ZonedDateTime zdt =
                                    getZonedDateTimeFromTemporalDateInput(temporalDateInfo);
                            ZonedDateTime zdtNewTz = zdt.withZoneSameInstant(zoneId);
                            ImmutableMap<Object, Object> testCase =
                                    getTestCaseForZonedDateTime(
                                            icuServiceBuilder,
                                            localeCldrFile,
                                            options,
                                            optionsBuilder,
                                            zdtNewTz,
                                            calendar,
                                            dateFormatter,
                                            timeFormatter,
                                            dateLength);
                            result.add(testCase);
                        }
                    }
                }
            }
        }

        return result;
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
