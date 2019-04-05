package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.TablePrinter;
import org.unicode.cldr.util.SupplementalDataInfo.DateRange;
import org.unicode.cldr.util.SupplementalDataInfo.MetaZoneRange;
import org.unicode.cldr.util.TimezoneFormatter.Format;

import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class VerifyZones {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();

    private static final String DIR = CLDRPaths.CHART_DIRECTORY + "verify/zones/";

    private static final boolean DEBUG = false;

    final static Options myOptions = new Options();

    enum MyOptions {
        organization(".*", "CLDR", "organization"), filter(".*", ".*", "locale filter (regex)"), timezoneFilter(".*", null, "timezone filter (regex)"),;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static long date = new Date(new Date().getYear(), 0, 15, 0, 0, 0).getTime();
    static long date6 = date + 182L * 24 * 60 * 60 * 1000;

    static class MetazoneRow extends R5<Long, String, String, Integer, String> {
        public MetazoneRow(Integer order, Integer rawOffset, String container, int orderInMetazone, String metazone, String zone) {
            super(((long) order << 32) + rawOffset, container, metazone, orderInMetazone, zone);
        }

        public String getContainer() {
            return get1();
        }

        public String getMetazone() {
            return get2();
        }

        public String getZone() {
            return get4();
        }
    }

    private final static SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    private final static Map<String, Map<String, String>> metazoneToRegionToZone = sdi.getMetazoneToRegionToZone();
    private final static Set<MetazoneRow> rows = new TreeSet<MetazoneRow>();
    private final static Set<String> goldenZones = new HashSet<String>();
    private final static Map<String, Integer> countryToOrder = new HashMap<String, Integer>();

    private final static List<Format> FORMAT_LIST = Arrays.asList(Format.VVVV, Format.vvvv, Format.v, Format.zzzz,
        Format.z, Format.zzzz, Format.z);
    static {

        // find out which canonical zones are not in a metazone
        Map<String, String> nameToCountry = new TreeMap<String, String>();
        String[] zones = TimeZone.getAvailableIDs();
        Set<String> zoneSet = new LinkedHashSet<String>();
        Set<String> noncanonical = new LinkedHashSet<String>();
        for (String zone : zones) {
            String countryCode = TimeZone.getRegion(zone);
            String englishTerritory = ULocale.getDisplayCountry("und-" + countryCode, ULocale.ENGLISH);
            nameToCountry.put(englishTerritory, countryCode);
            String canon = TimeZone.getCanonicalID(zone);
            if (canon.equals(zone)) {
                zoneSet.add(canon);
            } else {
                noncanonical.add(zone);
            }
        }

        // get mapping of country names to ints
        int i = 0;
        for (Entry<String, String> entry : nameToCountry.entrySet()) {
            countryToOrder.put(entry.getValue(), i++);
        }

        //System.out.println("Canonical zones:\t" + zoneSet.size() + "\t" + zoneSet);
        //System.out.println("Non-canonical zones:\t" + noncanonical.size() + "\t" + noncanonical);

        Set<String> metazones = sdi.getAllMetazones();
        if (DEBUG && !metazones.equals(metazoneToRegionToZone.keySet())) {
            System.out.println("Mismatch between metazones");
            showVennSets(metazones, metazoneToRegionToZone.keySet());
        }

        Set<String> zonesInMetazones = new LinkedHashSet<String>();
        for (String metazone : metazones) {
            //String container = PathHeader.getMetazonePageTerritory(metazone);
            Map<String, String> regionToZone = metazoneToRegionToZone.get(metazone);
            String zone = regionToZone.get("001");
            goldenZones.add(zone);
            zonesInMetazones.add(zone);
            //            TimeZone currentZone = TimeZone.getTimeZone(tz_string);
            //            int order = Containment.getOrder(container);
            //            int offsetOrder = currentZone.getRawOffset();
            //            MetazoneRow row = new MetazoneRow(order, offsetOrder, container, 0, metazone, tz_string);
            //            rows.add(row);
            addRow(metazone, zone, 0);
        }
        //System.out.println("Zones. A = canonical zones, B = zones in metazonesToRegionToZone");
        //showVennSets(zoneSet, zonesInMetazones);
        vennSets(zoneSet, zonesInMetazones);
        Set<String> found = new LinkedHashSet<String>();
        for (String zone : zoneSet) {
            Set<MetaZoneRange> metaZoneRanges = sdi.getMetaZoneRanges(zone);
            if (metaZoneRanges == null) {
                continue;
            }
            for (MetaZoneRange metaZoneRange : metaZoneRanges) {
                if (metaZoneRange.dateRange.getTo() == DateRange.END_OF_TIME) {
                    found.add(zone);
                    addRow(metaZoneRange.metazone, zone, 1);
                    break;
                }
            }
        }
        //        zoneSet.removeAll(found);
        //        for (String zone : zoneSet) {
        //            found.add(zone);
        //            //            TimeZone currentZone = TimeZone.getTimeZone(tz_string);
        //            //            int offsetOrder = currentZone.getRawOffset();
        //            //            MetazoneRow row = new MetazoneRow(Integer.MAX_VALUE, offsetOrder, "001", 1, "None", tz_string);
        //            //            rows.add(row);
        //            addRow("None", zone, 1);
        //        }
        if (DEBUG) System.out.println("\nSorted");
        for (MetazoneRow row : rows) {
            if (row.getMetazone().equals("Europe_Central")) {
                if (DEBUG) System.out.println(row);
            }
        }
    }

    private static void addRow(String metaZone, String tz_string, int orderInMetazone) {
        TimeZone currentZone = TimeZone.getTimeZone(tz_string);
        String container = PathHeader.getMetazonePageTerritory(metaZone);
        if (container == null) {
            return; // skip
        }
        int order = Containment.getOrder(container);
        int offsetOrder = currentZone.getRawOffset();
        orderInMetazone = (orderInMetazone << 16)
            | (hasDaylight(currentZone) ? 0 : 1)
            | countryToOrder.get(TimeZone.getRegion(tz_string));
        MetazoneRow row = new MetazoneRow(order, offsetOrder, container,
            orderInMetazone, metaZone, tz_string);
        if (metaZone.equals("Europe_Central")) {
            if (DEBUG) System.out.println(row);
        }
        rows.add(row);
    }

    private static void showVennSets(Set<String> zoneSet, Set<String> zonesInMetazones) {
        Set<String> common = new LinkedHashSet<String>();
        Set<String> firstMinusSecond = new LinkedHashSet<String>();
        Set<String> secondMinusFirst = new LinkedHashSet<String>();
        vennSets(zoneSet, zonesInMetazones, common, firstMinusSecond, secondMinusFirst);
        if (!common.isEmpty()) System.out.println("A & B:\t" + common.size() + "\t" + common);
        if (!firstMinusSecond.isEmpty()) System.out.println("A - B:\t" + firstMinusSecond.size() + "\t" + firstMinusSecond);
        if (!secondMinusFirst.isEmpty()) System.out.println("B - A:\t" + secondMinusFirst.size() + "\t" + secondMinusFirst);
    }

    private static <T> void vennSets(Set<T> first, Set<T> second,
        Set<T> common, Set<T> firstMinusSecond, Set<T> secondMinusFirst) {
        common.clear();
        common.addAll(first);
        common.retainAll(second);
        firstMinusSecond.clear();
        firstMinusSecond.addAll(first);
        firstMinusSecond.removeAll(common);
        secondMinusFirst.clear();
        secondMinusFirst.addAll(second);
        secondMinusFirst.removeAll(common);
    }

    @SuppressWarnings("unused")
    private static <T> void vennSets(Set<T> first, Set<T> second, Set<T> common) {
        common.clear();
        common.addAll(first);
        common.retainAll(second);
        first.removeAll(common);
        second.removeAll(common);
    }

    private static <T> void vennSets(Set<T> first, Set<T> second) {
        first.removeAll(second);
        second.removeAll(first);
    }

    /**
     * Produce a set of static tables from the vxml data. Only a stopgap until the above is integrated into ST.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        myOptions.parse(MyOptions.organization, args, true);

        String organization = MyOptions.organization.option.getValue();
        String filter = MyOptions.filter.option.getValue();
        String timezoneFilterString = MyOptions.timezoneFilter.option.getValue();
        Matcher timezoneFilter = timezoneFilterString == null ? null : PatternCache.get(timezoneFilterString)
            .matcher("");

        Factory factory2 = Factory.make(CLDRPaths.MAIN_DIRECTORY, filter);
        CLDRFile englishCldrFile = factory2.make("en", true);
        DateTimeFormats.writeCss(DIR);
        final CLDRFile english = CLDR_CONFIG.getEnglish();

        Map<String, String> indexMap = new TreeMap<>(CLDR_CONFIG.getCollator());

        for (String localeID : factory2.getAvailableLanguages()) {
            Level level = StandardCodes.make().getLocaleCoverageLevel(organization, localeID);
            if (Level.MODERN.compareTo(level) > 0) {
                continue;
            }
            CLDRFile cldrFile = factory2.make(localeID, true);
            PrintWriter out = FileUtilities.openUTF8Writer(DIR, localeID + ".html");
            String title = "Verify Time Zones: " + englishCldrFile.getName(localeID);
            out.println("<!doctype HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN'><html><head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "<title>" + title + "</title>\n" +
                "<link rel='stylesheet' type='text/css' href='index.css'>\n" +
                "</head><body><h1>" + title + "</h1>\n"
                + "<p><a href='index.html'>Index</a></p>\n");

            showZones(timezoneFilter, englishCldrFile, cldrFile, out);

            out.println("</body></html>");
            out.close();

            indexMap.put(english.getName(localeID), localeID + ".html");
        }
        try (PrintWriter index = DateTimeFormats.openIndex(DIR, "Time Zones")) {
            DateTimeFormats.writeIndexMap(indexMap, index);
        }

        // Look at DateTimeFormats.java

        if (true) return;

        // Set<String> defaultContentLocales = sdi.getDefaultContentLocales();
        // NumberFormat enf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
        // enf.setGroupingUsed(false);
        // Set<String> debugCreationErrors = new LinkedHashSet<String>();
        // Set<String> errors = new LinkedHashSet<String>();
        //
        // for (String locale : factory2.getAvailableLanguages()) {
        // if (defaultContentLocales.contains(locale)) {
        // continue;
        // }
        // Level level = StandardCodes.make().getLocaleCoverageLevel(organization, locale);
        // if (Level.MODERN.compareTo(level) > 0) {
        // continue;
        // }
        //
        // // one path for group-3, one for group-4
        // int factor = USES_GROUPS_OF_4.contains(locale) ? 10000 : 1000;
        //
        // ULocale locale2 = new ULocale(locale);
        // NumberFormat nf = NumberFormat.getIntegerInstance(locale2);
        // nf.setMaximumFractionDigits(0);
        // CLDRFile cldrFile = factory2.make(locale, true, DraftStatus.contributed);
        // PluralInfo pluralInfo = sdi.getPlurals(locale);
        // Set<Double> samples = new TreeSet<Double>();
        // for (Entry<Count, List<Double>> entry : pluralInfo.getCountToExamplesMap().entrySet()) {
        // samples.add(entry.getValue().get(0));
        // }
        // String[] debugOriginals = null;
        // CompactDecimalFormat cdf = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors, debugOriginals,
        // Style.SHORT, locale2);
        // captureErrors(debugCreationErrors, errors, locale, "short");
        // CompactDecimalFormat cdfs = BuildIcuCompactDecimalFormat.build(cldrFile, debugCreationErrors, debugOriginals,
        // Style.LONG, locale2);
        // captureErrors(debugCreationErrors, errors, locale, "long");
        //
        // Set<Double> samples2 = new TreeSet<Double>();
        // for (int i = 10; i < factor; i *= 10) {
        // for (Double sample : samples) {
        // samples2.add(sample*i);
        // }
        // }
        // samples.addAll(samples2);
        // samples.add(1.5d);
        // System.out.println("———\t" + englishCldrFile.getName(locale) + "\t———");
        //
        // String column12 = (locale + "\t" + englishCldrFile.getName(locale));
        // System.out.print(column12 +
        // "\tNumeric\tCompact-Short\tCompact-Long\tFixed Numeric\tFixed Compact-Short\tFixed Compact-Long\n");
        //
        // try {
        // // we print the __ so that it can be imported into a spreadsheet without problems.
        // for (long i = factor; i <= 100000000000000L; i *= factor) {
        // for (Double sample : samples) {
        // double source = i * sample;
        // if (false && source == 22000000 && locale.equals("cs")) {
        // System.out.println("**");
        // }
        // System.out.print(locale + "\t__" + enf.format(source));
        // System.out.print("\t__" + nf.format(source));
        // String formatted = cdf.format(source);
        // System.out.print("\t__" + formatted);
        // formatted = cdfs.format(source);
        // System.out.println("\t__" + formatted);
        // }
        // System.out.println();
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // }
        // for (String s : errors) {
        // System.out.println(s);
        // }
    }

    public static void showZones(Matcher timezoneFilter,
        CLDRFile englishCldrFile, CLDRFile nativeCdrFile,
        Appendable out) throws IOException {
        TablePrinter tablePrinter = new TablePrinter() // .setCaption("Timezone Formats")
            .setTableAttributes("class='dtf-table'")
            .addColumn("Metazone").setHeaderCell(true).setSpanRows(true)
            .setHeaderAttributes("class='dtf-th'").setCellAttributes("class='dtf-s'")
            .addColumn("Region: TZID").setHeaderCell(true).setSpanRows(true)
            .setHeaderAttributes("class='dtf-th'").setCellAttributes("class='dtf-s'")
        //.setCellPattern(CldrUtility.getDoubleLinkMsg())
        // HACK because anchors don't work any more
        // .addColumn("Region: City").setHeaderCell(true).setSpanRows(true)
        // .addColumn("Region/City").setSpanRows(true)
        ;
        //         .addColumn("Code", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true).setSpanRows(true)

        boolean daylight = false;
        for (Format s : FORMAT_LIST) {
            tablePrinter.addColumn(s.toString()
                + "<br>" + s.type.toString(daylight)
                + "<br>" + s.location
                + "<br>" + s.length).setSpanRows(true).setHeaderAttributes("class='dtf-th'")
                .setCellAttributes("class='dtf-s'");
            if (s == Format.z) {
                daylight = true; // reset for final 2 items
            }
        }
        tablePrinter.addColumn("View").setHeaderCell(true).setHeaderAttributes("class='dtf-th'").setCellAttributes("class='dtf-s'");
        ZoneFormats englishZoneFormats = new ZoneFormats().set(englishCldrFile);
        addZones(englishZoneFormats, nativeCdrFile, timezoneFilter, tablePrinter);

        out.append(tablePrinter.toString() + "\n");
    }

    private static void addZones(ZoneFormats englishZoneFormats, CLDRFile cldrFile, Matcher timezoneFilter,
        TablePrinter output) throws IOException {
        CLDRFile englishCldrFile = englishZoneFormats.cldrFile;
        //ZoneFormats nativeZoneFormats = new ZoneFormats().set(cldrFile);
        TimezoneFormatter tzformatter = new TimezoneFormatter(cldrFile);

        for (MetazoneRow row : rows) {
            String grouping = row.getContainer();
            String metazone = row.getMetazone();
            String tzid = row.getZone();
            TimeZone currentZone = TimeZone.getTimeZone(tzid);
            TimeZone tz = currentZone;

            String englishGrouping = englishCldrFile.getName(CLDRFile.TERRITORY_NAME, grouping);

            String metazoneInfo = englishGrouping
                + "<br>" + englishZoneFormats.formatGMT(currentZone)
                + "<br>" + "MZ: " + metazone;

            boolean isGolden = goldenZones.contains(tzid);
            String countryCode2 = TimeZone.getRegion(tzid);
            if (countryCode2.equals("001")) {
                continue;
            }
            String englishTerritory = englishCldrFile.getName(CLDRFile.TERRITORY_NAME, countryCode2);
            output.addRow()
                .addCell(metazoneInfo)
                .addCell(englishTerritory + ": " + tzid.replace("/", "/\u200B"));
            long date2 = getStandardDate(tz);
            for (Format pattern : FORMAT_LIST) {
                String formattedZone = tzformatter.getFormattedZone(tzid, pattern.toString(), date2);
                if (isGolden) {
                    formattedZone = "<b>" + formattedZone + "</b>";
                }
                output.addCell(formattedZone);
                if (pattern == Format.z) {
                    if (!hasDaylight(tz)) {
                        output.addCell("<i>n/a</i>");
                        output.addCell("<i>n/a</i>");
                        break;
                    }
                    date2 = date2 == date ? date6 : date; // reverse for final 2 items
                }
            }
            String view = PathHeader.getLinkedView(surveyUrl, cldrFile, METAZONE_PREFIX + metazone + METAZONE_SUFFIX);
            if (view == null) {
                view = PathHeader.getLinkedView(surveyUrl, cldrFile, METAZONE_PREFIX + metazone + METAZONE_SUFFIX2);
            }

            output.addCell(view == null
                ? ""
                : view);
            output.finishRow();
        }
    }

    private static String surveyUrl = CLDR_CONFIG.getProperty("CLDR_SURVEY_URL",
        "http://st.unicode.org/cldr-apps/survey");

    static private String METAZONE_PREFIX = "//ldml/dates/timeZoneNames/metazone[@type=\"";
    static private String METAZONE_SUFFIX = "\"]/long/generic";
    static private String METAZONE_SUFFIX2 = "\"]/long/standard";

    private static boolean hasDaylight(TimeZone tz) {
        int dateOffset = tz.getOffset(date);
        return dateOffset != tz.getRawOffset() || dateOffset != tz.getOffset(date6);
    }

    private static long getStandardDate(TimeZone tz) {
        return tz.getOffset(date) == tz.getRawOffset() ? date : date6;
    }

    private static long getDaylightDate(TimeZone tz) {
        return tz.getOffset(date) == tz.getRawOffset() ? date6 : date;
    }

    private static void captureErrors(Set<String> debugCreationErrors, Set<String> errors, String locale, String length) {
        if (debugCreationErrors.size() != 0) {
            for (String s : debugCreationErrors) {
                errors.add(locale + "\t" + length + "\t" + s);
            }
            debugCreationErrors.clear();
        }
    }
}
