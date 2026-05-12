package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.ibm.icu.impl.CalType;
import com.ibm.icu.text.DateTimePatternGenerator;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.DatetimeUtilities;
import org.unicode.cldr.util.DatetimeUtilities.DatePatternInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Joiners;

@CLDRTool(alias = "unexpected", description = "Show unexpected date formats")
public class ShowUnexpectedDateFormats {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final CLDRFile ENGLISH = CLDR_CONFIG.getEnglish();
    private static final String[] formats = {
        "MMMEEEEd",
        "MMMMEEEEd",
        "yMMMEEEEd",
        "yMMMMEEEEd",
        "GyMMMEEEEd/d",
        "GyMMMEEEEd/G",
        "GyMMMEEEEd/M",
        "GyMMMEEEEd/y"
    };

    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            switch (arg) {
                case "unexpected":
                    checkUnexpected();
                    break;
                case "calendar":
                    showCalendarSkeletons();
                    break;
            }
        }
    }

    private static void checkUnexpected() {
        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> locales = new TreeSet<>(cldrFactory.getAvailable());
        System.out.println("Checking " + locales.size() + " locales");
        Map<String, Integer> count = new TreeMap<>();
        Set<String> localesWithUnexpectedPaths = new TreeSet<>();
        for (String loc : locales) {
            CLDRFile cldrFile = cldrFactory.make(loc, false);
            for (String format : formats) {
                for (CalType calType : CalType.values()) {
                    String path = makePathFromFormat(calType, format);
                    String value = cldrFile.getStringValue(path);
                    if (value != null) {
                        System.out.println(loc + "\t" + path + "\t" + value);
                        localesWithUnexpectedPaths.add(loc);
                        Integer c = count.get(format);
                        if (c == null) {
                            c = 0;
                        }
                        count.put(format, c + 1);
                    }
                }
            }
        }
        for (String format : formats) {
            Integer c = count.get(format);
            if (c == null) {
                c = 0;
            }
            System.out.println(format + "\t" + c);
        }
        System.out.println(
                localesWithUnexpectedPaths.size()
                        + " locales with unexpected paths: "
                        + Joiner.on(" ").join(localesWithUnexpectedPaths));
    }

    private static String makePathFromFormat(CalType calType, String format) {
        if (format.contains("/")) {
            String format1 = format.substring(0, format.length() - 2);
            String format2 = format.substring(format.length() - 1);
            // Note: this path differs from the one below not only in the addition of
            // "greatestDifference", but also in having "intervalFormats/intervalFormatItem" instead
            // of "availableFormats/dateFormatItem"
            return "//ldml/dates/calendars/calendar[@type=\""
                    + calType.getId()
                    + "\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\""
                    + format1
                    + "\"]/greatestDifference[@id=\""
                    + format2
                    + "\"]";
        } else {
            return "//ldml/dates/calendars/calendar[@type=\""
                    + calType.getId()
                    + "\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                    + format
                    + "\"]";
        }
    }

    static Set<String> SKIP_NON_GREGORIAN = Set.of("yyyy", "yyyyQQQ", "yyyyQQQQ");

    static void showCalendarSkeletons() {
        System.out.println(
                Joiners.TAB.join(
                        "locale",
                        "calen.",
                        "skel.",
                        "fixedSk",
                        "greg.B",
                        "gen.B",
                        "gen.M"));
        showCalendarSkeletons2("root", "generic");
        showCalendarSkeletons2("root", "chinese");
        showCalendarSkeletons2("en", "generic");
        showCalendarSkeletons2("en", "chinese");
    }

    private static void showCalendarSkeletons2(String locale, String calendar) {
        CLDRFile cldrFile = CLDR_CONFIG.getCldrFactory().make(locale, true);
        
        DatePatternInfo gregorian = DatetimeUtilities.DatePatternInfo.from(cldrFile, "gregorian");
        DatePatternInfo generic = DatetimeUtilities.DatePatternInfo.from(cldrFile, calendar);
        
        Set<String> gregorianSkeletons = gregorian.getAvailableSkeletonToPattern().keySet();
        Set<String> genericSkeletons = generic.getAvailableSkeletonToPattern().keySet();
        
        DateTimePatternGenerator gregorianGenerator = gregorian.getGenerator(false);
        DateTimePatternGenerator genericGenerator = generic.getGenerator(false);
        
        for (String skeleton : Sets.difference(genericSkeletons, gregorianSkeletons)) {
            if (SKIP_NON_GREGORIAN.contains(skeleton)) {
                continue;
            }
            String fixedSkeleton = skeleton;
            if (skeleton.contains("yyyy")) {
                fixedSkeleton = skeleton.replace("yyyy", "y");
            }
            if (fixedSkeleton.contains("y") && !fixedSkeleton.contains("G")) {
                fixedSkeleton = "G" + fixedSkeleton;
            }
           
            String gregorianBest = gregorianGenerator.getBestPattern(skeleton);
            String genericBest = genericGenerator.getBestPattern(skeleton);
            
            String genericMod = skeleton.equals(fixedSkeleton) ? "???" : genericGenerator.getBestPattern(fixedSkeleton);
            
            if (!genericBest.equals(genericMod)) {
                System.out.println(
                        Joiners.TAB.join(
                                locale,
                                calendar,
                                skeleton,
                                fixedSkeleton,
                                gregorianBest,
                                genericBest,
                                genericMod));
            }
        }
    }
}
