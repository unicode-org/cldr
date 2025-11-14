package org.unicode.cldr.tool;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.NumberingSystem;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CalculatedCoverageLevels;
import org.unicode.cldr.util.DateTimeFormats;
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
     * type. The default value should be assumed to be AT_TIME if a variable of type
     * DateTimeFormatType is not set.
     *
     * <p>atTime = the word "at" is inserted between the date and time when formatting both date &
     * time together, at least for long and full dates.
     *
     * <p>relative = the word "at" may be inserted between the date and time when formatting both
     * date & time together for long and full dates, depending on the grammatical requirements onf
     * the language.
     *
     * <p>standard = do not insert the word "at". (ex: in `en`, there may or may not be a comma
     * instead to separate)
     */
    enum DateTimeFormatType {
        STANDARD("standard"),
        AT_TIME("atTime"),
        RELATIVE("relative");

        public final String label;

        String getLabel() {
            return this.label;
        }

        DateTimeFormatType(String label) {
            this.label = label;
        }
    }

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

    public static final Optional<CLDRFile> getCLDRFile(String locale) {
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

    private static String getExpectedStringForTestCase(
            ICUServiceBuilder icuServiceBuilder,
            CLDRFile localeCldrFile,
            String calendar,
            TimeZone icuTimeZone,
            ZonedDateTime zdt,
            String timeLength,
            String dateLength,
            String dateTimeGluePatternFormatType) {
        String formattedDateTime;
        SimpleDateFormat timeFormatter = null;
        SimpleDateFormat dateFormatter = null;

        // properly initialize the time formatter

        if (timeLength != null) {
            timeFormatter = localeCldrFile.getTimeFormat(calendar, timeLength, icuServiceBuilder);
            assert timeFormatter != null;
            timeFormatter.setTimeZone(icuTimeZone);
        }

        // properly initialize the date formatter

        if (dateLength != null) {
            dateFormatter = localeCldrFile.getDateFormat(calendar, dateLength, icuServiceBuilder);
            assert dateFormatter != null;
            dateFormatter.setTimeZone(icuTimeZone);
        }

        // compute the formatted date time string

        if (dateLength == null) {
            formattedDateTime = timeFormatter.format(zdt);
        } else if (timeLength == null) {
            formattedDateTime = dateFormatter.format(zdt);
        } else {
            String formattedDate = dateFormatter.format(zdt);
            String formattedTime = timeFormatter.format(zdt);

            assert dateTimeGluePatternFormatType != null;

            formattedDateTime =
                    localeCldrFile.glueDateTimeFormat(
                            formattedDate,
                            formattedTime,
                            calendar,
                            dateLength,
                            dateTimeGluePatternFormatType,
                            icuServiceBuilder);
        }

        return formattedDateTime;
    }

    /* TODO: Expand the kernel over all locale-preferred calendars:

           List<String> localePreferredCalendars = SUPPLEMENTAL_DATA_INFO.getCalendars(region);
           if (localePreferredCalendars == null) {
               localePreferredCalendars = SUPPLEMENTAL_DATA_INFO.getCalendars("001");
           }
    */

    enum DateStyle {
        SHORT("short"),
        MEDIUM("medium"),
        LONG("long"),
        FULL("full");

        public final String label;

        String getLabel() {
            return this.label;
        }

        DateStyle(String label) {
            this.label = label;
        }
    }

    enum TimeStyle {
        SHORT("short"),
        MEDIUM("medium"),
        LONG("long"),
        FULL("full");

        public final String label;

        String getLabel() {
            return this.label;
        }

        TimeStyle(String label) {
            this.label = label;
        }
    }

    enum SemanticSkeletonLength {
        SHORT("short"),
        MEDIUM("medium"),
        LONG("long");

        public final String label;

        String getLabel() {
            return this.label;
        }

        SemanticSkeletonLength(String label) {
            this.label = label;
        }
    }

    /**
     * Semantic Skeletons and related DateTime fields, etc. are defined in LDML / UTS 35. Ex: See:
     * https://www.unicode.org/reports/tr35/tr35-dates.html#Generating_Patterns_for_Semantic_Skeletons
     */
    enum SemanticSkeleton {
        YMDE("YMDE"),
        MDTZ("MDTZ"),
        M("M"),
        T("T"),
        Z("Z");

        public final String label;

        String getLabel() {
            return this.label;
        }

        SemanticSkeleton(String label) {
            this.label = label;
        }

        public boolean hasYear() {
            return this == SemanticSkeleton.YMDE;
        }

        public boolean hasMonth() {
            return (this == SemanticSkeleton.YMDE
                    || this == SemanticSkeleton.MDTZ
                    || this == SemanticSkeleton.M);
        }

        public boolean hasDay() {
            return (this == SemanticSkeleton.YMDE || this == SemanticSkeleton.MDTZ);
        }

        public boolean hasWeekday() {
            return this == SemanticSkeleton.YMDE;
        }

        public boolean hasTime() {
            return (this == SemanticSkeleton.MDTZ || this == SemanticSkeleton.T);
        }

        public boolean hasZone() {
            return (this == SemanticSkeleton.MDTZ || this == SemanticSkeleton.Z);
        }

        public boolean isStandalone() {
            return (this == SemanticSkeleton.M
                    || this == SemanticSkeleton.T
                    || this == SemanticSkeleton.Z);
        }
    }

    enum HourCycle {
        H12("H12"),
        H23("H23");

        public final String label;

        String getLabel() {
            return this.label;
        }

        HourCycle(String label) {
            this.label = label;
        }
    }

    enum YearStyle {
        AUTO("auto"),
        FULL("full"),
        WITH_ERA("with_era");

        public final String label;

        String getLabel() {
            return this.label;
        }

        YearStyle(String label) {
            this.label = label;
        }
    }

    enum ZoneStyle {
        SPECIFIC("specific"),
        GENERIC("generic"),
        LOCATION("location"),
        OFFSET("offset");

        public final String label;

        String getLabel() {
            return this.label;
        }

        ZoneStyle(String label) {
            this.label = label;
        }
    }

    // private class SemanticSkeletonFieldSet()

    /**
     * A struct to contain combinations of datetime fields & styles, mainly to allow an enumeration
     * of combinations of values for these fields that yields a similarly thorough coverage of the
     * test space without having to compute the full Cartesian product of all values of all
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
        DateTimeFormatType dateTimeFormatType;
    }

    /**
     * A struct to contain the data to be used to generate combinations of datetime fields & styles
     * and whether to combine (obtain the Cartesian product of) them by other dimensions
     */
    static class FieldStyleComboInput {
        FieldStyleCombo fieldStyleCombo = new FieldStyleCombo();
        boolean shouldMultiplyByTimeZone;
        boolean shouldMultiplyByDateTime;
    }

    /**
     * @return The manually created collection of field input value combinations that characterize
     *     the test cases of the kernel.
     */
    private static ImmutableSet<FieldStyleComboInput> getFieldStyleComboInputs() {
        ImmutableSet.Builder<FieldStyleComboInput> builder = ImmutableSet.builder();

        FieldStyleComboInput elem;

        // TODO: Add to the kernel:
        //  - fractional second digits
        //  - column alignment
        //  - time precision

        // TODO: For semantic skeleton test cases,
        //     add DateTimeFormatType=STANDARD to test cases
        //     once CLDR DateTimeFormats constructor can use CLDRFile to get the dateTimeFormat glue
        //     pattern, since we are currently using ICU to get the dateTimeFormat pattern,
        //     which defaults to the behavior of DateTimeFormatType.AT_TIME

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
        elem.fieldStyleCombo.dateTimeFormatType = DateTimeFormatType.AT_TIME;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 3a
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.dateStyle = DateStyle.FULL;
        elem.fieldStyleCombo.timeStyle = TimeStyle.SHORT;
        elem.fieldStyleCombo.dateTimeFormatType = DateTimeFormatType.STANDARD;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 4
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.dateStyle = DateStyle.SHORT;
        elem.fieldStyleCombo.timeStyle = TimeStyle.FULL;
        elem.fieldStyleCombo.dateTimeFormatType = DateTimeFormatType.AT_TIME;
        elem.shouldMultiplyByTimeZone = true;
        elem.shouldMultiplyByDateTime = true;
        builder.add(elem);

        // 5 (Row 6)
        elem = new FieldStyleComboInput();
        elem.fieldStyleCombo = new FieldStyleCombo();
        elem.fieldStyleCombo.semanticSkeleton = SemanticSkeleton.YMDE;
        elem.fieldStyleCombo.semanticSkeletonLength = SemanticSkeletonLength.SHORT;
        elem.shouldMultiplyByDateTime = true;
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

    /**
     * A struct used to represent and store the manually created test case inputs that constitute
     * the "kernel" of test cases. The kernel of test cases exist to create a minimal set of
     * combinations / variations of values for the date time fields that exercise a fairly thorough
     * coverage of functionality & corner cases for date time formatting.
     */
    static class TestCaseInput {
        FieldStyleCombo fieldStyleCombo;
        LocalDateTime dateTime;
        TimeZone timeZone;
        ULocale locale;
        Calendar calendar;
    }

    static class TestCase {
        TestCaseInput testCaseInput;
        String classicalSkeleton; // Should only be non-null when semantic skeleton non-null.
        // This exists to allow existing ICU & other i18n impls to format
        // until they add support for formatting using semantic skeletons.
        String expected;
    }

    private static final Pattern SKELETON_YEAR_FIELD_PATTERN = Pattern.compile("y+");

    private static final Pattern SKELETON_ERA_FIELD_PATTERN = Pattern.compile("G+");

    private static final Pattern SKELETON_MONTH_FIELD_PATTERN = Pattern.compile("M+");

    private static final Pattern SKELETON_DAY_FIELD_PATTERN = Pattern.compile("d+");

    /**
     * Get the locale default date time skeleton for the given semantic skeleton length, and then
     * return the skeleton substring that represents a particular date time field according to
     * {@code fieldPattern}.
     *
     * @param localeCldrFile
     * @param fieldStyleCombo
     * @param calendarStr
     * @param fieldPattern
     * @return
     */
    private static String getFieldFromDateTimeSkeleton(
            CLDRFile localeCldrFile,
            FieldStyleCombo fieldStyleCombo,
            String calendarStr,
            Pattern fieldPattern) {
        String skeletonLength = fieldStyleCombo.semanticSkeletonLength.getLabel();
        String dateTimeSkeleton = localeCldrFile.getDateSkeleton(calendarStr, skeletonLength);
        Matcher fieldMatchResult = fieldPattern.matcher(dateTimeSkeleton);

        if (fieldMatchResult.find()) {
            // take the first result, which assumes that there is only one unique place where a
            // match can occur
            // otherwise, we would need to get the group count and iterate over each match group
            String field = fieldMatchResult.group();
            return field;
        } else {
            return "";
        }
    }

    public static String computeSkeletonFromSemanticSkeleton(
            ICUServiceBuilder icuServiceBuilder,
            CLDRFile localeCldrFile,
            FieldStyleCombo fieldStyleCombo,
            String calendarStr) {
        SemanticSkeleton skeleton = fieldStyleCombo.semanticSkeleton;
        StringBuilder sb = new StringBuilder();

        // Year
        if (skeleton.hasYear()) {
            final String year =
                    getFieldFromDateTimeSkeleton(
                            localeCldrFile,
                            fieldStyleCombo,
                            calendarStr,
                            SKELETON_YEAR_FIELD_PATTERN);

            final String era =
                    getFieldFromDateTimeSkeleton(
                            localeCldrFile,
                            fieldStyleCombo,
                            calendarStr,
                            SKELETON_ERA_FIELD_PATTERN);

            // Compute the final form of the year field according to the year style
            String updatedYear = year;
            String updatedEra = era;
            YearStyle yearStyle = fieldStyleCombo.yearStyle;
            if (yearStyle != null) {
                switch (yearStyle) {
                    case AUTO:
                        updatedYear = year;
                        updatedEra = era;
                        break;
                    case FULL:
                        updatedYear = year.replace("yy", "y");
                        updatedEra = era;
                        break;
                    case WITH_ERA:
                        updatedYear = year.replace("yy", "y");
                        if (era.isEmpty()) {
                            updatedEra = "G";
                        } else {
                            updatedEra = era;
                        }
                        break;
                }
            }
            String updatedYearEra = updatedEra + updatedYear;

            sb.append(updatedYearEra);
        }

        // Month
        if (skeleton.hasMonth()) {
            if (skeleton.isStandalone()) {
                switch (fieldStyleCombo.semanticSkeletonLength) {
                    case LONG:
                        sb.append("LLLL");
                        break;
                    case MEDIUM:
                        sb.append("LLL");
                        break;
                    case SHORT:
                        sb.append("L");
                        break;
                    default:
                        break;
                }
            } else {
                sb.append(
                        getFieldFromDateTimeSkeleton(
                                localeCldrFile,
                                fieldStyleCombo,
                                calendarStr,
                                SKELETON_MONTH_FIELD_PATTERN));
            }
        }

        // Day
        if (skeleton.hasDay()) {
            sb.append(
                    getFieldFromDateTimeSkeleton(
                            localeCldrFile,
                            fieldStyleCombo,
                            calendarStr,
                            SKELETON_DAY_FIELD_PATTERN));
        }

        // Weekday
        if (skeleton.hasWeekday()) {
            switch (fieldStyleCombo.semanticSkeletonLength) {
                case LONG:
                    sb.append("EEEE");
                    break;
                case MEDIUM:
                    sb.append("EEE");
                    break;
                case SHORT:
                    if (skeleton.isStandalone()) {
                        sb.append("EEEEE");
                    } else {
                        sb.append("EEE");
                    }
                    break;
                default:
                    break;
            }
        }

        // Time
        if (skeleton.hasTime()) {
            if (fieldStyleCombo.hourCycle == null) {
                // TODO: use "C" instead of "j" by fixing NullPointerException in ICU4J
                //  DateTimePatternGenerator.mapSkeletonMetacharacters
                sb.append("j");
            } else {
                // H M S
                switch (fieldStyleCombo.hourCycle) {
                    case H12:
                        sb.append("h");
                        break;
                    case H23:
                        sb.append("H");
                        break;
                }
            }
            sb.append("m");
            sb.append("s");
        }

        // Time Zone
        if (skeleton.hasZone()) {
            switch (fieldStyleCombo.zoneStyle) {
                case GENERIC:
                    if (skeleton.isStandalone()) {
                        if (fieldStyleCombo.semanticSkeletonLength
                                == SemanticSkeletonLength.SHORT) {
                            sb.append("v");
                        } else {
                            sb.append("vvvv");
                        }
                    } else {
                        sb.append("v");
                    }
                    break;
                case SPECIFIC:
                    if (skeleton.isStandalone()) {
                        if (fieldStyleCombo.semanticSkeletonLength
                                == SemanticSkeletonLength.SHORT) {
                            sb.append("z");
                        } else {
                            sb.append("zzzz");
                        }
                    } else {
                        sb.append("z");
                    }
                    break;
                case LOCATION:
                    sb.append("VVVV");
                    break;
                case OFFSET:
                    sb.append("O");
                    break;
                default:
                    break;
            }
        }

        return sb.toString();
    }

    private static TestCase convertStylesTestCaseInputToTestCase(
            ICUServiceBuilder icuServiceBuilder,
            CLDRFile localeCldrFile,
            TestCaseInput testCaseInput) {
        String calendarStr = testCaseInput.calendar.getType();
        String dateLength =
                testCaseInput.fieldStyleCombo.dateStyle == null
                        ? null
                        : testCaseInput.fieldStyleCombo.dateStyle.getLabel();
        String timeLength =
                testCaseInput.fieldStyleCombo.timeStyle == null
                        ? null
                        : testCaseInput.fieldStyleCombo.timeStyle.getLabel();
        LocalDateTime localDt = testCaseInput.dateTime;
        TimeZone icuTimeZone = testCaseInput.timeZone;
        ZoneId zoneId = ZoneId.of(icuTimeZone.getID());
        ZonedDateTime zdt = ZonedDateTime.of(localDt, zoneId);
        String dateTimeGluePatternFormatType =
                testCaseInput.fieldStyleCombo.dateTimeFormatType == null
                        ? null
                        : testCaseInput.fieldStyleCombo.dateTimeFormatType.getLabel();

        String expected =
                getExpectedStringForTestCase(
                        icuServiceBuilder,
                        localeCldrFile,
                        calendarStr,
                        icuTimeZone,
                        zdt,
                        timeLength,
                        dateLength,
                        dateTimeGluePatternFormatType);

        TestCase result = new TestCase();
        result.testCaseInput = testCaseInput;
        result.expected = expected;

        return result;
    }

    private static TestCase computeTestCase(
            ICUServiceBuilder icuServiceBuilder,
            CLDRFile localeCldrFile,
            TestCaseInput testCaseInput) {

        if (testCaseInput.fieldStyleCombo.semanticSkeleton == null) {
            return convertStylesTestCaseInputToTestCase(
                    icuServiceBuilder, localeCldrFile, testCaseInput);
        } else {
            String calendarStr = testCaseInput.calendar.getType();
            String skeleton =
                    computeSkeletonFromSemanticSkeleton(
                            icuServiceBuilder,
                            localeCldrFile,
                            testCaseInput.fieldStyleCombo,
                            calendarStr);
            // compute the expected
            // TODO: fix CLDR DateTimeFormats constructor to use CLDRFile to get the dateTimeFormat
            //   glue pattern rather than use ICU to get it
            DateTimeFormats formats = new DateTimeFormats(localeCldrFile, calendarStr, false);
            SimpleDateFormat formatterForSkeleton = formats.getDateFormatFromSkeleton(skeleton);
            formatterForSkeleton.setCalendar(testCaseInput.calendar);
            formatterForSkeleton.setTimeZone(testCaseInput.timeZone);
            String timeZoneIdStr = testCaseInput.timeZone.getID();
            ZoneId timeZoneId = ZoneId.of(timeZoneIdStr);
            ZonedDateTime zdt = ZonedDateTime.of(testCaseInput.dateTime, timeZoneId);
            String formattedDateTime = formatterForSkeleton.format(zdt);

            TestCase result = new TestCase();
            result.testCaseInput = testCaseInput;
            result.classicalSkeleton = skeleton;
            result.expected = formattedDateTime;

            return result;
        }
    }

    public static ImmutableSet<TestCase> getKernelTestCases() {

        // more manually defined inputs

        List<Pair<ULocale, Calendar>> LOCALE_CALENDAR_PAIRS =
                List.of(
                        Pair.of(ULocale.ENGLISH, new GregorianCalendar()),
                        Pair.of(ULocale.forLanguageTag("ar-SA"), new IslamicCalendar()),
                        Pair.of(ULocale.forLanguageTag("th-TH"), new BuddhistCalendar()),
                        Pair.of(ULocale.forLanguageTag("ja-JP"), new JapaneseCalendar()));

        List<LocalDateTime> DATE_TIMES =
                List.of(
                        LocalDateTime.of(2000, 1, 1, 0, 0, 0),
                        LocalDateTime.of(2024, 7, 1, 8, 50, 7),
                        // Ramadan in Summer at 12:00 noon in the year 2014
                        LocalDateTime.of(2014, 7, 15, 12, 0, 0));

        List<LocalDateTime> DATE_TIME_ONE_ONLY = List.of(DATE_TIMES.get(0));

        // TODO: add a 3rd time zone dynamically, which is the default time zone for the current
        //    locale in question when iterating over all locales
        List<TimeZone> STATIC_TIME_ZONES =
                List.of(TimeZone.GMT_ZONE, TimeZone.getTimeZone("Australia/Adelaide"));

        List<TimeZone> STATIC_TIME_ZONE_ONE_ONLY = List.of(STATIC_TIME_ZONES.get(0));

        // setup of return value

        ImmutableSet.Builder<TestCase> builder = ImmutableSet.builder();

        // iteration to add to return value

        for (Pair<ULocale, Calendar> localeCalendarPair : LOCALE_CALENDAR_PAIRS) {
            ULocale locale = localeCalendarPair.first;
            Calendar calendar = localeCalendarPair.second;

            // get the formatted version of the locale with underscores instead of BCP 47 dashes
            // since this is what the CLDRFile constructor expects
            String localeStr = locale.getName();

            CLDRFile localeCldrFile = getCLDRFile(localeStr).orElse(null);

            if (localeCldrFile == null) {
                continue;
            }

            ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder(localeCldrFile);

            for (FieldStyleComboInput input : getFieldStyleComboInputs()) {
                assert input.shouldMultiplyByDateTime || input.shouldMultiplyByTimeZone;

                FieldStyleCombo fieldStyleCombo = input.fieldStyleCombo;

                List<LocalDateTime> dateTimeIterationColl = DATE_TIME_ONE_ONLY;
                if (input.shouldMultiplyByDateTime) {
                    dateTimeIterationColl = DATE_TIMES;
                }

                List<TimeZone> timeZoneIterationColl = STATIC_TIME_ZONE_ONE_ONLY;
                if (input.shouldMultiplyByTimeZone) {
                    timeZoneIterationColl = STATIC_TIME_ZONES;
                    // TODO: add a TimeZone from the region of the locale to the list of time zones
                    // to iterate over
                }

                for (LocalDateTime localDateTime : dateTimeIterationColl) {
                    for (TimeZone timeZone : timeZoneIterationColl) {
                        TestCaseInput testCaseInput = new TestCaseInput();
                        testCaseInput.fieldStyleCombo = fieldStyleCombo;
                        testCaseInput.dateTime = localDateTime;
                        testCaseInput.timeZone = timeZone;
                        testCaseInput.locale = locale;
                        testCaseInput.calendar = calendar;

                        TestCase testCase =
                                computeTestCase(icuServiceBuilder, localeCldrFile, testCaseInput);

                        builder.add(testCase);
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * This struct class exists specifically to convert from the structured {@code TestCase} struct
     * class to one that is appropriate for de-/serializing (formatting & parsing), ex: to give it a
     * flat structure that makes it easier for downstream consumers of the formatted output.
     */
    static class TestCaseSerde {
        String dateLength = null;
        String timeLength = null;
        String semanticSkeleton = null;
        String semanticSkeletonLength = null;
        String classicalSkeleton = null;
        String dateTimeFormatType = null;
        String hourCycle = null;
        String zoneStyle = null;
        String yearStyle = null;
        String calendar = null;
        String locale = null;
        String input = null;
        String expected = null;
    }

    private static TestCaseSerde convertTestCaseToSerialize(TestCase testCase) {
        TestCaseSerde result = new TestCaseSerde();

        result.dateLength =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.dateStyle)
                        .map(DateStyle::getLabel)
                        .orElse(null);
        result.timeLength =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.timeStyle)
                        .map(TimeStyle::getLabel)
                        .orElse(null);
        result.semanticSkeleton =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.semanticSkeleton)
                        .map(SemanticSkeleton::getLabel)
                        .orElse(null);
        result.semanticSkeletonLength =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.semanticSkeletonLength)
                        .map(SemanticSkeletonLength::getLabel)
                        .orElse(null);
        result.classicalSkeleton = testCase.classicalSkeleton;
        result.dateTimeFormatType =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.dateTimeFormatType)
                        // because AT_TIME is the default, we do not serialize it to the output
                        .filter(dtft -> dtft != DateTimeFormatType.AT_TIME)
                        .map(DateTimeFormatType::getLabel)
                        .orElse(null);
        result.hourCycle =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.hourCycle)
                        .map(HourCycle::getLabel)
                        .orElse(null);
        result.zoneStyle =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.zoneStyle)
                        .map(ZoneStyle::getLabel)
                        .orElse(null);
        result.yearStyle =
                Optional.ofNullable(testCase.testCaseInput.fieldStyleCombo.yearStyle)
                        .map(YearStyle::getLabel)
                        .orElse(null);
        result.calendar = testCase.testCaseInput.calendar.getType();
        result.locale = testCase.testCaseInput.locale.toLanguageTag();

        LocalDateTime localDt = testCase.testCaseInput.dateTime;
        TimeZone icuTimeZone = testCase.testCaseInput.timeZone;
        ZoneId zoneId = ZoneId.of(icuTimeZone.getID());
        ZonedDateTime zdt = ZonedDateTime.of(localDt, zoneId);
        result.input = zdt.toString();

        result.expected = testCase.expected;

        return result;
    }

    public static void main(String[] args) throws IOException {
        try (TempPrintWriter pw =
                TempPrintWriter.openUTF8Writer(
                        CLDRPaths.TEST_DATA + OUTPUT_SUBDIR, OUTPUT_FILENAME)) {
            ImmutableSet<TestCase> testCases = getKernelTestCases();
            List<TestCaseSerde> testCaseSerdes =
                    testCases.stream()
                            .map(GenerateDateTimeTestData::convertTestCaseToSerialize)
                            .collect(Collectors.toList());
            pw.println(GSON.toJson(testCaseSerdes));
        }
    }
}
