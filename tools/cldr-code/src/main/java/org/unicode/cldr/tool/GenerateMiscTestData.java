package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.Comparators;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NameGetter;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.ZoneParser;

/**
 * For now, this is code to investigate dealing with the CLDR-18847; the idea is to turn it into a
 * tool for generating the associated test data.
 */
public class GenerateMiscTestData {
    private static final Factory CLDR_FACTORY = CLDRConfig.getInstance().getCldrFactory();
    private static final NameGetter localeNamegetter =
            new NameGetter(CLDR_FACTORY.make("en", true));

    public static void main(String[] args) {
        checkAllTimezoneTransitions();
        // checkTimezoneTransitions();
        // generateGmt();
        // generateRational();
    }

    public static void generateGmt() {
        // https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
        /*
         * Samples are chosen to provide:
         * 1 Northern hemisphere daylight
         * 1 Southern hemisphere daylight
         * 1 no daylight time shift
         * 1 hour-only offset
         * 1 hour-minute offset
         */
        List<Row.R3<String, Number, Number>> samples =
                List.of(
                        Row.of("Toronto (CA)", -5, -4),
                        Row.of("Tokyo (JP)", 9, 9),
                        Row.of("Adelaide (AU)", 9.5, 10.5));

        String titles = samples.stream().map(x -> x.get0()).collect(Collectors.joining("\t"));
        System.out.println(
                Joiners.TAB.join(
                        "Locale",
                        "Code",
                        "NS",
                        "GMT pat",
                        "Hour pat",
                        titles,
                        "Combo pat",
                        "Combo pat*"));

        for (String locale : CLDR_FACTORY.getAvailableLanguages()) {
            Level level = StandardCodes.make().getLocaleCoverageLevel(Organization.cldr, locale);
            if (level.compareTo(Level.MODERATE) < 0) {
                continue;
            }
            CLDRFile cf = CLDR_FACTORY.make(locale, true);
            GmtData gmtData = new GmtData(cf);
            String guess = "{0}/{1}";
            // handle both latn and default (if different)
            LinkedHashSet<String> numSystems = new LinkedHashSet<>();
            numSystems.add("latn");
            numSystems.add(gmtData.defaultNumberingSystem);

            for (String numSystem : numSystems) {
                System.out.print(
                        Joiners.TAB.join(
                                localeNamegetter.getNameFromIdentifier(locale),
                                locale,
                                numSystem,
                                '«' + gmtData.gmtFormat + '»',
                                '«' + gmtData.gmtHourFormat + '»',
                                '«' + guess + '»',
                                ""));
                System.out.println(
                        samples.stream()
                                .map(x -> gmtData.format(x.get1(), x.get2(), guess, numSystem))
                                .collect(Collectors.joining("\t")));
            }
        }
    }

    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    //    static void checkTimezoneTransitions() {
    //        long jan15 = Instant.parse("2025-01-15T12:00:00Z").toEpochMilli();
    //        long jul15 = Instant.parse("2025-07-15T12:00:00Z").toEpochMilli();
    //        long jan15_next = Instant.parse("2026-01-15T12:00:00Z").toEpochMilli();
    //        DateFormat df = new SimpleDateFormat("y-MM-dd", Locale.ENGLISH);
    //
    //        /*
    //        * <key name="tz" description="Time zone key" alias="timezone">
    //            <type name="adalv" description="Andorra" alias="Europe/Andorra"/>
    //            <type name="ancur" description="Curaçao" alias="America/Curacao" region="CW"/>
    //            <type name="aukns" description="Currie, Australia" deprecated="true"
    // preferred="auhba"/>
    //        */
    //        Set<String> shortTzIds = SDI.bcp47Key2Subtypes.get("tz");
    //        Map<R2<String, String>, String> deprecated = SDI.getBcp47Deprecated();
    //        Relation<R2<String, String>, String> aliases = SDI.getBcp47Aliases();
    //
    //        Map<Row.R4<String, String, Duration, Duration>, Multimap<String, String>> data =
    //                new TreeMap<>();
    //        for (String shortTzId : shortTzIds) {
    //            R2<String, String> keySubtype = R2.of("tz", shortTzId);
    //            if (deprecated.get(keySubtype).equals("true")) {
    //                continue;
    //            }
    //            Set<String> a = aliases.get(keySubtype);
    //            String zone = a.iterator().next();
    //            TimeZone foo = TimeZone.getTimeZone(zone).freeze();
    //            Duration jan15Offset = Duration.ofMillis(foo.getOffset(jan15));
    //            Duration jul15Offset = Duration.ofMillis(foo.getOffset(jul15));
    //            if (jan15Offset.equals(jul15Offset)) {
    //                continue;
    //            }
    //            // find the transition dates
    //            long first = findTransition(foo, jan15, jul15);
    //            long second = findTransition(foo, jul15, jan15_next);
    //            R4<String, String, Duration, Duration> key =
    //                    Row.R4.of(df.format(first), df.format(second), jan15Offset, jul15Offset);
    //            String region = getRegionFromShortTzid(shortTzId);
    //            String[] parts = zone.split("/");
    //            String lastPart = parts[parts.length - 1];
    //            Multimap<String, String> map = data.get(key);
    //            if (map == null) {
    //                data.put(key, map = TreeMultimap.create());
    //            }
    //            map.put(region, shortTzId + ":" + lastPart);
    //        }
    //        data.entrySet().stream()
    //                .forEach(
    //                        x ->
    //                                println(
    //                                        "",
    //                                        x.getKey().get0(),
    //                                        "\t",
    //                                        x.getKey().get1(),
    //                                        "\t",
    //                                        formatDuration(x.getKey().get2()),
    //                                        "\t",
    //                                        formatDuration(x.getKey().get3()),
    //                                        "\t➡︎\t",
    //
    // Joiners.COMMA_SP.join(x.getValue().asMap().entrySet())));
    //    }

    static final DateFormat df = new SimpleDateFormat("y-MM-dd", Locale.ENGLISH);

    static final class TimesAndOffsets implements Comparable<TimesAndOffsets> {
        final List<Long> pointsInTime = new ArrayList<>();
        final List<Duration> offsets = new ArrayList<>();

        @Override
        public int compareTo(TimesAndOffsets other) {
            return ComparisonChain.start()
                    .compare(
                            pointsInTime,
                            other.pointsInTime,
                            Comparators.lexicographical(Comparator.<Long>naturalOrder()))
                    .compare(
                            offsets,
                            other.offsets,
                            Comparators.lexicographical(Comparator.<Duration>naturalOrder()))
                    .result();
        }

        public String format() {
            String pits =
                    pointsInTime.stream().map(x -> df.format(x)).collect(Collectors.joining("\t"));
            String durations =
                    offsets.stream().map(x -> formatDuration(x)).collect(Collectors.joining("\t"));
            return pits + "\t" + durations;
        }
    }

    static final Set<String> SKIP_RAM = Set.of("eheai", "macas");

    static void checkAllTimezoneTransitions() {
        long jan15 = Instant.parse("2025-01-15T12:00:00Z").toEpochMilli();
        long dec15 = Instant.parse("2025-12-15T12:00:00Z").toEpochMilli();
        ZoneParser parser = new ZoneParser();
        Map<String, List<String>> zoneData = parser.getZoneData();

        Set<String> shortTzIds = SDI.bcp47Key2Subtypes.get("tz");
        Map<R2<String, String>, String> deprecated = SDI.getBcp47Deprecated();
        Relation<R2<String, String>, String> aliases = SDI.getBcp47Aliases();

        Map<TimesAndOffsets, Multimap<String, String>> data = new TreeMap<>();
        for (String shortTzId : shortTzIds) {
            if (SKIP_RAM.contains(shortTzId)) {
                continue;
            }
            R2<String, String> keySubtype = R2.of("tz", shortTzId);
            if (deprecated.get(keySubtype).equals("true")) {
                continue;
            }
            Set<String> a = aliases.get(keySubtype);
            String zone = a.iterator().next();
            int latitude = 0;
            int longitude = 0;
            List<String> zdata = zoneData.get(zone);
            if (zdata != null) {
                latitude = (int) Math.round(Double.parseDouble(zdata.get(0)));
                longitude = (int) Math.round(Double.parseDouble(zdata.get(1)));
            }

            TimeZone foo = TimeZone.getTimeZone(zone).freeze();

            Duration last = null;
            TimesAndOffsets tao = new TimesAndOffsets();

            for (long pit = jan15; pit <= dec15; pit += 86400000) {
                Duration offset = Duration.ofMillis(foo.getOffset(pit));
                if (!offset.equals(last)) {
                    if (last != null) {
                        tao.pointsInTime.add(pit);
                    }
                    tao.offsets.add(offset);
                    last = offset;
                }
            }
            if (tao.offsets.size() < 2) {
                continue;
            }
            String region = getRegionFromShortTzid(shortTzId);
            String[] parts = zone.split("/");
            String lastPart = parts[parts.length - 1];
            Multimap<String, String> map = data.get(tao);
            if (map == null) {
                data.put(tao, map = TreeMultimap.create());
            }
            map.put(region, shortTzId + ":" + lastPart + "(" + latitude + "," + longitude + ")");
        }
        data.entrySet().stream()
                .forEach(
                        x ->
                                println(
                                        "",
                                        x.getKey().format(),
                                        "\t➡︎\t",
                                        Joiners.COMMA_SP.join(x.getValue().asMap().entrySet())));
    }

    private static String getRegionFromShortTzid(String shortTzId) {
        Map<R2<String, String>, String> toRegion = SDI.getBcp47Region();
        String region = toRegion.get(R2.of("tz", shortTzId));
        return region != null
                ? region //
                : shortTzId.substring(0, 2).toUpperCase(Locale.ENGLISH);
    }

    private static String formatDuration(Duration duration) {
        return String.valueOf(duration.toMinutes() / 60.0);
    }

    private static void println(String joinWith, Object... toJoin) {
        System.out.println(Joiner.on(joinWith).join(toJoin));
    }

    private static long findTransition(TimeZone foo, long start, long end) {
        int startOffset = foo.getOffset(start);
        int endOffset = foo.getOffset(end);
        while (true) {
            long mid = (start + end) / 2;
            if (end - start < 86400) {
                return mid;
            }
            int midOffset = foo.getOffset(mid);
            if (midOffset == startOffset) {
                start = mid;
            } else if (midOffset == endOffset) {
                end = mid;
            } else {
                throw new IllegalArgumentException("Bizarre behavior");
            }
        }
    }

    private static class GmtData {
        final String gmtFormat;
        final String gmtHourFormat;
        final ICUServiceBuilder isb;
        String defaultNumberingSystem;

        /*
        <timeZoneNames>
          <hourFormat>+HH:mm;-HH:mm</hourFormat>
          <gmtFormat>GMT{0}</gmtFormat>
         */
        public GmtData(CLDRFile cf) {
            String gmtBasePath = "//ldml/dates/timeZoneNames";
            String gmtHourFormatPath = gmtBasePath + "/hourFormat";
            String gmtFormatPath = gmtBasePath + "/gmtFormat";
            defaultNumberingSystem = cf.getStringValue("//ldml/numbers/defaultNumberingSystem");
            gmtFormat = cf.getStringValue(gmtFormatPath);
            gmtHourFormat = cf.getStringValue(gmtHourFormatPath);
            isb = new ICUServiceBuilder();
            isb.setCldrFile(cf);
        }

        private String format(Number jan15, Number jul15, String guess, String numSystem) {
            String[] gmtHourFormats = gmtHourFormat.split(";");
            // result.add("jan15=" + jan15 + "; jul15=" + jul15);
            CharSequence formattedHourJan15 =
                    formatHour(gmtHourFormats, jan15.doubleValue(), isb, numSystem);
            CharSequence formattedHourJul15 =
                    formatHour(gmtHourFormats, jul15.doubleValue(), isb, numSystem);
            if (formattedHourJan15.equals(formattedHourJul15)) {
                return gmtFormat.replace("{0}", formattedHourJan15);
            }
            String formattedTime =
                    guess.replace("{0}", formattedHourJan15).replace("{1}", formattedHourJul15);
            return gmtFormat.replace("{0}", formattedTime);
        }
    }

    private static CharSequence formatHour(
            String[] gmtHourFormats, double offset, ICUServiceBuilder isb, String numberSystem) {
        return formatHour(gmtHourFormats[offset >= 0 ? 0 : 1], Math.abs(offset), isb, numberSystem);
    }

    // hack for now
    private static CharSequence formatHour(
            String patternhhmm, double offset, ICUServiceBuilder isb, String numberSystem) {
        int h = patternhhmm.lastIndexOf("H");
        String hpat = patternhhmm.replace("H", "#").substring(0, h + 1);
        double hour = Math.floor(offset);
        double minute = (offset - hour) * 60;
        DecimalFormat nf = isb.getNumberFormat(hpat, numberSystem);
        if (minute == 0) {
            return nf.format(hour);
        }
        String mpat = patternhhmm.replace("m", "0").substring(h + 1);
        DecimalFormat nf2 = isb.getNumberFormat(mpat, numberSystem);
        return nf.format(hour) + nf2.format(minute);
    }

    static void generateRational() {
        // TBD
        /*
         <rationalFormats numberSystem="latn">
           <rationalPattern>{0}⁄{1}</rationalPattern>
           <integerAndRationalPattern>{0} {1}</integerAndRationalPattern>
           <integerAndRationalPattern alt="superSub">{0}⁠{1}</integerAndRationalPattern>
           <rationalUsage>sometimes</rationalUsage>
         </rationalFormats>
        */
        final String base = "//ldml/numbers/rationalFormats[@numberSystem=\"latn\"]";
        final String rationalPatternPath = base + "/rationalPattern";
        final String integerAndRationalPatternPath = base + "/integerAndRationalPattern";
        final String integerAndRationalPatternAltPath =
                base + "/integerAndRationalPattern[@alt=\"superSub\"]";
        for (String locale : CLDR_FACTORY.getAvailableLanguages()) {
            Level level = StandardCodes.make().getLocaleCoverageLevel(Organization.cldr, locale);
            if (level.compareTo(Level.MODERATE) < 0) {
                continue;
            }
            CLDRFile cf = CLDR_FACTORY.make(locale, true);
            final String rationalPattern = cf.getStringValue(rationalPatternPath);
            final String integerAndRationalPattern =
                    cf.getStringValue(integerAndRationalPatternPath);
            final String integerAndRationalPatternAlt =
                    cf.getStringValue(integerAndRationalPatternAltPath);
            println(
                    "\t",
                    locale,
                    localeNamegetter.getNameFromIdentifier(locale),
                    rationalPattern,
                    integerAndRationalPattern,
                    integerAndRationalPatternAlt);
        }
    }
}
