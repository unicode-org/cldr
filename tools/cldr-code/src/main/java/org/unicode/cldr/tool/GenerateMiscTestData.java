package org.unicode.cldr.tool;

import com.ibm.icu.impl.Row;
import com.ibm.icu.text.DecimalFormat;
import java.util.LinkedHashSet;
import java.util.List;
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

public class GenerateMiscTestData {
    private static final Factory CLDR_FACTORY = CLDRConfig.getInstance().getCldrFactory();

    public static void main(String[] args) {
        generateGmt();
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

        NameGetter localeNamegetter = new NameGetter(CLDR_FACTORY.make("en", true));

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
        /* <rationalFormats numberSystem="latn">
           <rationalPattern>{0}⁄{1}</rationalPattern>
           <integerAndRationalPattern>{0} {1}</integerAndRationalPattern>
           <integerAndRationalPattern alt="superSub">{0}⁠{1}</integerAndRationalPattern>
           <rationalUsage>sometimes</rationalUsage>
         </rationalFormats>
        */
        String base = "//ldml/numbers/rationalFormats[@numberSystem=\"latn\"]";
        String rationalPatternPath = base + "/rationalPattern";
    }
}
