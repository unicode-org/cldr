package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DayPeriodInfo;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.DayPeriodInfo.Span;
import org.unicode.cldr.util.DayPeriodInfo.Type;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

public class GenerateDayPeriods {
    static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final int MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

    public static void main(String[] args) throws IOException {
        try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY + "/supplemental", "dayPeriods.xml")) {
            out.println(DtdType.supplementalData.header(null)
                + "\t<version number=\"$Revision" /* bypass SVN */ + "$\"/>");
            Factory factory = CLDRConfig.getInstance().getCldrFactory();
            for (Type type : Type.values()) {
                out.println("\t<dayPeriodRuleSet"
                    + (type == Type.format ? "" : " type=\"" + type + "\"")
                    + ">");
                for (String locale : SDI.getDayPeriodLocales(type)) {
                    DayPeriodInfo dayPeriodInfo = SDI.getDayPeriods(type, locale);
                    out.println("\t\t<dayPeriodRules locales=\"" + locale + "\">");
                    Map<DayPeriod, String> localizations = getLocalizations(factory.make(locale, true));
                    for (DayPeriod dayPeriod : DayPeriod.values()) {
                        if ((dayPeriod == DayPeriod.am || dayPeriod == DayPeriod.pm) && !locale.equals("root")) {
                            continue;
                        }
                        Set<Span> spanSet = dayPeriodInfo.getDayPeriodSpans(dayPeriod);
                        if (spanSet != null) {
                            Span span = combineSpans(spanSet);
                            final String localization = localizations.get(dayPeriod);
                            out.println("\t\t\t<dayPeriodRule type=\""
                                + span.dayPeriod
                                + (span.start == span.end
                                    ? "\" at=\"" + format(span.start, false) + "\""
                                    : "\" from=\"" + format(span.start, false) + "\" before=\"" + format(span.end, true) + "\"")
                                + "/>"
                                + (localization == null ? "" : "\t<!-- " + localization + " -->"));
                        }
                    }
                    out.println("\t\t</dayPeriodRules>");
                }
                out.println("\t</dayPeriodRuleSet>");
            }
            out.println("</supplementalData>");
        }
    }

    private static String format(int start, boolean to) {
        return DayPeriodInfo.formatTime(to == true && start == 0 ? start + MILLIS_PER_DAY : start);
    }

    static Map<DayPeriod, String> getLocalizations(CLDRFile localeFile) {
        Map<DayPeriod, String> result = new TreeMap<>();
        final String dayPeriodLocalization = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"stand-alone\"]/dayPeriodWidth[@type=\"wide\"]"; // [@type=\"stand-alone\"]/dayPeriodWidth[@type=\"wide\"]"
        for (String path2 : With.in(localeFile.iterator())) {
            if (!path2.startsWith(dayPeriodLocalization) || path2.endsWith("/alias")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path2);
            String type = parts.getAttributeValue(-1, "type");
            result.put(DayPeriod.valueOf(type), localeFile.getStringValue(path2));
        }
        return result;
    }

    private static Span combineSpans(Set<Span> info) {
        if (info.size() == 1) {
            return info.iterator().next();
        }
        int start = -1;
        int end = -1;
        DayPeriod dayPeriod = null;
        for (Span span : info) {
            if (start < 0) {
                start = span.start;
                end = span.end;
                dayPeriod = span.dayPeriod;
                continue;
            }
            if (same(end, span.start)) {
                end = span.end;
            } else if (same(start, span.end)) {
                start = span.start;
            } else {
                throw new IllegalArgumentException("Failed to combine: " + info);
            }
        }
        return new Span(start, end, dayPeriod);
    }

    private static boolean same(int a, int b) {
        return (a % MILLIS_PER_DAY) == (b % MILLIS_PER_DAY);
    }
}
