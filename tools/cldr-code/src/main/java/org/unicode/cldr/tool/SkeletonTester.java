package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DatetimeUtilities;
import org.unicode.cldr.util.DatetimeUtilities.DatePatternInfo;
import org.unicode.cldr.util.DatetimeUtilities.FieldType;
import org.unicode.cldr.util.DatetimeUtilities.PatternElement;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.UnicodeSet;

public class SkeletonTester {
    private static final Factory CLDR_FACTORY = CLDRConfig.getInstance().getCldrFactory();

    public static void main(String[] args) {
        String locale = "de";
        CLDRFile cldrFile = CLDR_FACTORY.make(locale, true);
        CLDRFile cldrFileUnresolved = cldrFile.getUnresolved();
        Map<String, DatePatternInfo> calendarToDPI =
                DatetimeUtilities.calendarToDatePatternInfo(cldrFile);
        Map<String, DatePatternInfo> calendarToDPIUnresolved =
                DatetimeUtilities.calendarToDatePatternInfo(cldrFileUnresolved);
        
        Multimap<String, String> skeletonToCalendars = TreeMultimap.create();
        Multimap<String, String> nSkeletonToCalendars = TreeMultimap.create();
        UnicodeSet symbols = new UnicodeSet();
        Set<String> fullSymbols = new TreeSet<>();
        Set<String> fullNSymbols = new TreeSet<>();
        
        for (String calendar : calendarToDPIUnresolved.keySet()) {
            DatePatternInfo dpi = calendarToDPI.get(calendar);
            //        DateTimePatternGenerator gen1 = dpi.getGenerator(false);
            Map<String, String> skeletonToPattern = dpi.getAvailableSkeletonToPattern();
            skeletonToPattern
                    .keySet()
                    .forEach(
                            key -> {
                                symbols.addAll(key);
                                DatetimeUtilities.getPatternElements(key).forEach(x -> fullSymbols.add(x.rawString()));
                                skeletonToCalendars.put(key, calendar);
                                
                                String norm = normalizeSkeleton(key);
                                nSkeletonToCalendars.put(norm, calendar);
                                DatetimeUtilities.getPatternElements(norm).forEach(x -> fullNSymbols.add(x.rawString()));

                                if (!norm.equals(key)) {
                                    System.out.println(
                                            Joiners.TAB.join(
                                                    locale,
                                                    calendar,
                                                    key,
                                                    norm,
                                                    skeletonToPattern.containsKey(norm)
                                                            ? "COLLIDES"
                                                            : ""));
                                }
                            });

            
            //        Map<String, String> targetMap = skeletonToPattern.entrySet().stream()
            //            .collect(Collectors.toMap(
            //                entry2 -> {
            //                    String key = entry2.getKey();
            //                    String norm = normalizeSkeleton(key);
            //                    return norm;
            //                    }),
            //                Map.Entry::getValue
            //            ));
        }
        System.out.println("Symbol Characters\t" + symbols);
        System.out.println("Full Symbols\t" + fullSymbols);
        System.out.println("Full Norm. Symbols\t" + fullNSymbols);
        showItems("Calendar to Skeletons", skeletonToCalendars);
        showItems("Calendar to Norm. Skeletons", nSkeletonToCalendars);
    }

    private static void showItems(String title, Multimap<String, String> skelsToCalendars) {
        System.out.println("\n" + title);
        TreeSet<String> sortedValues = new TreeSet<>();
        sortedValues.addAll(skelsToCalendars.values());
        System.out.println(Joiners.TAB.join("Pat", Joiners.TAB.join(sortedValues)));
        skelsToCalendars.asMap().entrySet().stream().forEach(x -> System.out.println(Joiners.TAB.join(x.getKey(), show(sortedValues, x.getValue()))));
    }

    private static Object show(TreeSet<String> sorted, Collection<String> value) {
        return sorted.stream().map(x -> value.contains(x) ? x : "").collect(Collectors.joining("\t"));
    }

    public static String normalizeSkeleton(String pattern) {
        List<PatternElement> elements = DatetimeUtilities.getPatternElements(pattern);
        StringBuilder sb = new StringBuilder();
        for (PatternElement element : elements) {
            if (element.getType() == FieldType.LITERAL) {
                throw new IllegalArgumentException("No literals allowed in Skeleton");
            }
            String symbol = normalize(element);
            sb.append(symbol);
        }
        return sb.toString();
    }

    private static String normalize(PatternElement element) {
        String string = element.rawString();
        switch (element.getType()) {
            case ERA:
                return "G";
            case YEAR:
                switch(string.charAt(0)) {
                case 'y': return "y";
                case 'U': return string.length() < 3 ? "U" : string.length() == 3 ? "UUU" : "UUUU";
                }
                break;
            case QUARTER:
                return string.length() < 4 ? "QQQ" : "QQQQ";
            case MONTH:
                return string.length() < 3 ? "M" : string.length() == 3 ? "MMM" : "MMMM";
            case DAY_OF_MONTH:
                return string.length() < 3 ? "d" : "ddd";
            case WEEKDAY:
                return string.length() < 4 ? "E" : "EEEE";

            case DAYPERIOD:
                return "B";

            case HOUR:
                return string.startsWith("H") ? "H" : "h";
            case MINUTE:
                return "m";
            case SECOND:
                return "s";

            case ZONE:
                return "v";
        }
        throw new IllegalArgumentException("Unexpected pattern element: " + element);
    }
}
