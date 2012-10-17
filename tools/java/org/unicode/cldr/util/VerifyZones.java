package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.TablePrinter;
import org.unicode.cldr.util.TimezoneFormatter.Format;
import org.unicode.cldr.util.VerifyZones.ZoneFormats.Length;
import org.unicode.cldr.util.VerifyZones.ZoneFormats.Type;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;

public class VerifyZones {

    final static Options myOptions = new Options();

    enum MyOptions {
        organization(".*", "Google", "organization"),
        filter(".*", ".*", "locale filter (regex)"),
        timezoneFilter(".*", null, "timezone filter (regex)"), ;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    static class MetazoneRow extends R4<Integer, Integer, String, String> {
        public MetazoneRow(Integer arg0, Integer offset, String arg1, String arg2) {
            super(arg0, offset, arg1, arg2);
        }
    }

    public static class ZoneFormats {
        private String regionFormat;
        private String fallbackFormat;
        private String gmtFormat;
        private String hourFormat;
        private String[] hourFormatPlusMinus;
        private ICUServiceBuilder icuServiceBuilder = new ICUServiceBuilder();
        private CLDRFile cldrFile;

        public enum Length {
            LONG, SHORT;
            public String toString() {
                return name().toLowerCase(Locale.ENGLISH);
            }
        }

        public enum Type {
            generic, standard, daylight, genericOrStandard
        }

        public ZoneFormats set(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
            regionFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/regionFormat");
            fallbackFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/fallbackFormat");

            gmtFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/gmtFormat");
            hourFormat = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/hourFormat");
            hourFormatPlusMinus = hourFormat.split(";");
            icuServiceBuilder.setCldrFile(cldrFile);
            return this;
        }

        public String formatGMT(TimeZone currentZone) {
            int tzOffset = currentZone.getRawOffset();
            SimpleDateFormat dateFormat = icuServiceBuilder.getDateFormat("gregorian",
                hourFormatPlusMinus[tzOffset >= 0 ? 0 : 1]);
            String hoursMinutes = dateFormat.format(tzOffset >= 0 ? tzOffset : -tzOffset);
            return MessageFormat.format(gmtFormat, hoursMinutes);
        }

        public String getExemplarCity(String timezoneString) {
            String exemplarCity = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/zone[@type=\"" + timezoneString
                + "\"]/exemplarCity");
            if (exemplarCity == null) {
                exemplarCity = timezoneString.substring(timezoneString.lastIndexOf('/') + 1).replace('_', ' ');
            }
            return exemplarCity;
        }

        public String getMetazoneName(String metazone, Length length, Type typeIn) {
            Type type = typeIn == Type.genericOrStandard ? Type.generic : typeIn;
            String name = cldrFile.getWinningValue("//ldml/dates/timeZoneNames/metazone[@type=\""
                + metazone + "\"]/" + length + "/" + type);

            return name != null ? name : typeIn != Type.genericOrStandard ? "n/a" : getMetazoneName(metazone, length,
                Type.standard);
        }
    }

    private final static SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    private final static Map<String, Map<String, String>> metazoneToRegionToZone = sdi.getMetazoneToRegionToZone();
    private final static Set<MetazoneRow> rows = new TreeSet<MetazoneRow>();

    private final static List<Format> FORMAT_LIST = Arrays.asList(Format.VVVV, Format.vvvv, Format.v, Format.zzzz,
        Format.z, Format.zzzz, Format.z);

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
        Matcher timezoneFilter = timezoneFilterString == null ? null : Pattern.compile(timezoneFilterString)
            .matcher("");

        Factory factory2 = Factory.make(CldrUtility.MAIN_DIRECTORY, filter);
        CLDRFile englishCldrFile = factory2.make("en", true);
        ZoneFormats englishZoneFormats = new ZoneFormats().set(englishCldrFile);

        Set<String> metazones = sdi.getAllMetazones();

        for (String metazone : metazones) {
            String container = PathHeader.getMetazonePageTerritory(metazone);
            Map<String, String> regionToZone = metazoneToRegionToZone.get(metazone);
            String tz_string = regionToZone.get("001");
            TimeZone currentZone = TimeZone.getTimeZone(tz_string);
            int order = Containment.getOrder(container);
            MetazoneRow row = new MetazoneRow(order, currentZone.getRawOffset(), container, metazone);
            rows.add(row);
        }

        TablePrinter tablePrinter = new TablePrinter() // .setCaption("Timezone Formats")
            .addColumn("Metazone").setHeaderCell(true).setSpanRows(true)
            .addColumn("Region: TZID").setHeaderCell(true).setSpanRows(true)
        // .addColumn("Region: City").setHeaderCell(true).setSpanRows(true)
        // .addColumn("Region/City").setSpanRows(true)
        ;
        boolean daylight = false;
        for (Format s : FORMAT_LIST) {
            tablePrinter.addColumn(s.toString()
                + "<br>" + s.type.toString(daylight)
                + "<br>" + s.location
                + "<br>" + s.length).setSpanRows(true);
            if (s == Format.z) {
                daylight = true; // reset for final 2 items
            }
        }

        for (String localeID : factory2.getAvailableLanguages()) {
            CLDRFile cldrFile = factory2.make(localeID, true);
            tablePrinter.clearRows();
            addZones(englishZoneFormats, cldrFile, timezoneFilter, tablePrinter);

            PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.TMP_DIRECTORY + "verify/zones/", localeID +
                ".html");
            String title = "Verify Time Zones: " + englishCldrFile.getName(localeID);
            out.println("<html><head>\n" +
                "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n" +
                "<title>" + title + "</title>\n" +
                "<link rel='stylesheet' type='text/css' href='index.css'>\n" +
                "</head><body><h1>" + title + "</h1>");

            out.println(tablePrinter.toString());
            out.println("</body></html>");
            out.close();
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

    static long date = new Date(new Date().getYear(), 0, 15, 0, 0, 0).getTime();
    static long date6 = date + 182L * 24 * 60 * 60 * 1000;

    private static void addZones(ZoneFormats englishZoneFormats, CLDRFile cldrFile, Matcher timezoneFilter,
        TablePrinter output) throws IOException {
        CLDRFile englishCldrFile = englishZoneFormats.cldrFile;
        ZoneFormats nativeZoneFormats = new ZoneFormats().set(cldrFile);
        TimezoneFormatter tzformatter = new TimezoneFormatter(cldrFile);

        for (MetazoneRow row : rows) {
            String grouping = row.get2();
            String metazone = row.get3();
            Map<String, String> regionToZone = metazoneToRegionToZone.get(metazone);
            String tz_string = regionToZone.get("001");
            TimeZone currentZone = TimeZone.getTimeZone(tz_string);
            int gmtOffset = currentZone.getRawOffset();
            boolean observesDaylight = currentZone.observesDaylightTime();

            String englishGrouping = englishCldrFile.getName(CLDRFile.TERRITORY_NAME, grouping);
            // String nativeGrouping = nativeCldrFile.getName(CLDRFile.TERRITORY_NAME, grouping);

            String longGenericOrStandard = englishZoneFormats.getMetazoneName(metazone, Length.LONG,
                Type.genericOrStandard);
            String metazoneInfo = englishGrouping
                + "<br>" + englishZoneFormats.formatGMT(currentZone)
                // + "\t" + nativeZoneFormats.formatGMT(currentZone)
                + "<br>" + metazone;

            for (Entry<String, String> entity : regionToZone.entrySet()) {
                String countryCode = entity.getKey();
                String tzid = entity.getValue();
                TimeZone tz = TimeZone.getTimeZone(tzid);

                if (timezoneFilter != null && !timezoneFilter.reset(tzid).find()) {
                    continue;
                }

                String marker = "";
                String countryCode2 = countryCode;
                boolean defaultCountry = "001".equals(countryCode);
                if (defaultCountry) {
                    marker = "*";
                    countryCode2 = TimeZone.getRegion(tzid);
                }
                String englishTerritory = englishCldrFile.getName(CLDRFile.TERRITORY_NAME, countryCode2);
                String nativeTerritory = cldrFile.getName(CLDRFile.TERRITORY_NAME, countryCode2);
                String nativeCity = nativeZoneFormats.getExemplarCity(tzid);
                output.addRow()
                    .addCell(metazoneInfo)
                    .addCell(marker + englishTerritory + ": " + tzid.replace("/", "/\u200B"))
                // .addCell(englishTerritory + ": " + englishZoneFormats.getExemplarCity(tzid))
                // .addCell(nativeTerritory + "<br><br>" + nativeCity)
                ;
                long date2 = tz.getOffset(date) == tz.getRawOffset() ? date : date6;
                for (Format pattern : FORMAT_LIST) {
                    String formattedZone = tzformatter.getFormattedZone(tzid, pattern.toString(), date2);
                    output.addCell(formattedZone);
                    if (pattern == Format.z) {
                        date2 = date2 == date ? date6 : date; // reverse for final 2 items
                        if (tz.getOffset(date) == tz.getOffset(date6)) {
                            output.addCell("");
                            output.addCell("");
                            break;
                        }
                    }
                }
                output.finishRow();
            }
        }
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
