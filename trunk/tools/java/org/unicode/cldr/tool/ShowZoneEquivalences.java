package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.ArrayComparator;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Tabber;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.ZoneInflections;

import com.ibm.icu.util.TimeZone;

public class ShowZoneEquivalences {

    public static void main(String[] args) throws Exception {
        double deltaTime = System.currentTimeMillis();
        try {
            ShowZoneEquivalences.getZoneEquivalences();
        } finally {
            deltaTime = System.currentTimeMillis() - deltaTime;
            System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
            System.out.println("Done");
        }
    }

    public static void getZoneEquivalences() throws IOException, ParseException {
        // String tzid = "America/Argentina/ComodRivadavia";
        // TimeZone tz = TimeZone.getTimeZone(tzid);
        // int offset = tz.getOffset(new Date().getTime());
        // System.out.println(tzid + ":\t" + offset);
        // System.out.println("in available? " + Arrays.asList(TimeZone.getAvailableIDs()).contains(tzid));
        // System.out.println(new TreeSet(Arrays.asList(TimeZone.getAvailableIDs())));

        Set<String> needsTranslation = new TreeSet<String>(Arrays.asList(CountItems.needsTranslationString
            .split("[,]?\\s+")));
        Set<String> singleCountries = new TreeSet<String>(
            Arrays
                .asList(
                    "Africa/Bamako America/Godthab America/Santiago America/Guayaquil     Asia/Shanghai Asia/Tashkent Asia/Kuala_Lumpur Europe/Madrid Europe/Lisbon Europe/London Pacific/Auckland Pacific/Tahiti"
                        .split("\\s")));
        Set<String> defaultItems = new TreeSet<String>(
            Arrays
                .asList(
                    "Antarctica/McMurdo America/Buenos_Aires Australia/Sydney America/Sao_Paulo America/Toronto Africa/Kinshasa America/Santiago Asia/Shanghai America/Guayaquil Europe/Madrid Europe/London America/Godthab Asia/Jakarta Africa/Bamako America/Mexico_City Asia/Kuala_Lumpur Pacific/Auckland Europe/Lisbon Europe/Moscow Europe/Kiev America/New_York Asia/Tashkent Pacific/Tahiti Pacific/Kosrae Pacific/Tarawa Asia/Almaty Pacific/Majuro Asia/Ulaanbaatar Arctic/Longyearbyen Pacific/Midway"
                        .split("\\s")));

        StandardCodes sc = StandardCodes.make();
        Collection<String> codes = sc.getGoodAvailableCodes("tzid");
        TreeSet<String> extras = new TreeSet<String>();
        Map<String, Set<String>> m = sc.getZoneLinkNew_OldSet();
        for (String code : codes) {
            Collection<String> s = m.get(code);
            if (s == null)
                continue;
            extras.addAll(s);
        }
        extras.addAll(codes);
        Set<String> icu4jTZIDs = new TreeSet<String>(Arrays.asList(TimeZone.getAvailableIDs()));
        Set<String> diff2 = new TreeSet<String>(icu4jTZIDs);
        diff2.removeAll(extras);
        System.out.println("icu4jTZIDs - StandardCodes: " + diff2);
        diff2 = new TreeSet<String>(extras);
        diff2.removeAll(icu4jTZIDs);
        System.out.println("StandardCodes - icu4jTZIDs: " + diff2);
        ArrayComparator ac = new ArrayComparator(new Comparator[] {
            ArrayComparator.COMPARABLE, ArrayComparator.COMPARABLE,
            ArrayComparator.COMPARABLE });
        Map<String, String> zone_countries = sc.getZoneToCounty();

        TreeSet<Object[]> country_inflection_names = new TreeSet<Object[]>(ac);
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "inflections.txt");

        TreeMap<Integer, TreeSet<String>> minOffsetMap = new TreeMap<Integer, TreeSet<String>>();
        TreeMap<Integer, TreeSet<String>> maxOffsetMap = new TreeMap<Integer, TreeSet<String>>();

        for (Iterator<String> it = codes.iterator(); it.hasNext();) {
            String zoneID = it.next();
            String country = zone_countries.get(zoneID);
            TimeZone zone = TimeZone.getTimeZone(zoneID);
            ZoneInflections zip = new ZoneInflections(zone);
            out.println(zoneID + "\t" + zip);
            country_inflection_names.add(new Object[] { country, zip, zoneID });

            TreeSet<String> s = minOffsetMap.get(zip.getMinOffset());
            if (s == null)
                minOffsetMap.put(zip.getMinOffset(), s = new TreeSet<String>());
            s.add(zone.getID());

            s = maxOffsetMap.get(zip.getMaxOffset());
            if (s == null)
                maxOffsetMap.put(zip.getMaxOffset(), s = new TreeSet<String>());
            s.add(zone.getID());

        }
        System.out.println("Minimum Offset: " + minOffsetMap);
        System.out.println("Maximum Offset: " + maxOffsetMap);
        out.close();

        out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY, "modernTimezoneEquivalents.html");
        out.println("<html>" + "<head>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>"
            + "<title>Modern Equivalent Timezones</title><style>");
        out.println("td.top,td.topr { background-color: #EEEEFF }");
        out.println("td.r,td.topr { text-align:right }");
        out
            .println("td.gap { font-weight:bold; border-top: 3px solid #0000FF; border-bottom: 3px solid #0000FF; background-color: #CCCCCC }");
        out
            .println("</style>"
                + "</head>"
                + "<body>"
                + "<table border='1' cellspacing='0' cellpadding='2' style='border-collapse: collapse'>");
        Tabber.HTMLTabber tabber1 = new Tabber.HTMLTabber();
        tabber1.setParameters(4, "class='r'");
        tabber1.setParameters(5, "class='r'");
        Tabber.HTMLTabber tabber2 = new Tabber.HTMLTabber();
        tabber2.setParameters(0, "class='top'");
        tabber2.setParameters(1, "class='top'");
        tabber2.setParameters(2, "class='top'");
        tabber2.setParameters(3, "class='top'");
        tabber2.setParameters(4, "class='topr'");
        tabber2.setParameters(5, "class='topr'");
        Tabber.HTMLTabber tabber3 = new Tabber.HTMLTabber();
        tabber3.setParameters(0, "class='gap'");
        tabber3.setParameters(1, "class='gap'");
        tabber3.setParameters(2, "class='gap'");
        tabber3.setParameters(3, "class='gap'");
        tabber3.setParameters(4, "class='gap'");
        tabber3.setParameters(5, "class='gap'");

        long minimumDate = ICUServiceBuilder.isoDateParse("2000-1-1T00:00:00Z")
            .getTime();
        out
            .println("<h1>Modern Equivalent Timezones: <a target='_blank' href='instructions.html'>Instructions</a></h1>");
        out.println(ShowData.dateFooter());
        out.println("<p>Zones identical after: "
            + ICUServiceBuilder.isoDateFormat(minimumDate) + "</p>");
        String lastCountry = "";
        ZoneInflections.OutputLong diff = new ZoneInflections.OutputLong(0);
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        TimezoneFormatter tzf = new TimezoneFormatter(cldrFactory, "en", true);
        Map<String, Set<String>> country_zoneSet = sc.getCountryToZoneSet();
        boolean shortList = true;
        boolean first = true;
        int category = 1;
        Tabber tabber = tabber1;
        for (Iterator<Object[]> it = country_inflection_names.iterator(); it.hasNext();) {
            Object[] row = it.next();
            String country = (String) row[0];
            if (country.equals("001"))
                continue;
            if (shortList && (country_zoneSet.get(country)).size() < 2)
                continue;
            ZoneInflections zip = (ZoneInflections) row[1];
            String zoneID = (String) row[2];

            if (!country.equals(lastCountry)) {
                if (first)
                    first = false;
                category = 1;
                CountItems.subheader(out, tabber3);
            } else if (diff.value >= minimumDate) {
                // out.println(tabber.process("\tDiffers at:\t" + ICUServiceBuilder.isoDateFormat(diff.value)));
                tabber = tabber == tabber1 ? tabber2 : tabber1;
                ++category;
            }
            String zoneIDShown = zoneID;
            if (needsTranslation.contains(zoneID)) {
                zoneIDShown = "<b>" + zoneIDShown + "\u00B9</b>";
            }
            if (singleCountries.contains(zoneID)) {
                zoneIDShown = "<i>" + zoneIDShown + "</i> \u00B2";
            }
            if (defaultItems.contains(zoneID)) {
                zoneIDShown = "<span style='background-color: #FFFF00'>" + zoneIDShown
                    + "</span> ?";
            }
            // if (country.equals(lastCountry) && diff.value >= minimumDate) System.out.print("X");
            String newCountry = country;
            String mapLink = CountItems.country_map.get(country);
            if (mapLink != null) {
                newCountry = "<a target='map' href='" + mapLink + "'>" + country
                    + "</a>";
            }
            String minOffset = ZoneInflections.formatHours(zip.getMinOffset(minimumDate));
            String maxOffset = ZoneInflections.formatHours(zip.getMaxOffset(minimumDate));
            if (!icu4jTZIDs.contains(zoneID)) {
                minOffset = maxOffset = "??";
            }

            out.println(tabber.process(newCountry + "\t" + "<b>" + category + "</b>"
                + "\t" + zoneIDShown + "\t"
                + tzf.getFormattedZone(zoneID, "vvvv", minimumDate, false) + "\t"
                + minOffset + "\t" + maxOffset));
            lastCountry = country;
        }
        CountItems.subheader(out, tabber3);
        out.println("</table>");
        out.println(CldrUtility.ANALYTICS);
        out.println("</body></html>");
        out.close();
    }

}
