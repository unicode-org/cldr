package org.unicode.cldr.test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.text.UnicodeSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrIntervalFormat;
import org.unicode.cldr.util.XPathParts;

public class RelatedDatePathValues {
    public static final int calendarElement = 3;
    public static final int dateTypeElement = 5;
    public static final int idElement = 6;

    static final XPathParts available =
            XPathParts.getFrozenInstance(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"MMMd\"]");

    static final XPathParts interval =
            XPathParts.getFrozenInstance(
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"Bhm\"]/greatestDifference[@id=\"B\"]");

    // samples
    // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/intervalFormats
    //  /intervalFormatItem[@id="Bhm"]/greatestDifference[@id="B"]
    // ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/availableFormats
    //  /dateFormatItem[@id="MMMd"]
    // ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats
    // /dateFormatLength[@type="full"]/dateFormat[@type="standard"]/datetimeSkeleton[@type="standard"]
    // ldml/dates/calendars/calendar[@type="gregorian"]/timeFormats
    // /timeFormatLength[@type="full"]/timeFormat[@type="standard"]/datetimeSkeleton[@type="standard"]
    public static Set<String> getRelatedPathValues(CLDRFile cldrFile, XPathParts xparts) {
        if (xparts.size() <= idElement) {
            return Set.of();
        }
        switch (xparts.getElement(dateTypeElement)) {
            case "availableFormats":
                return forAvailable(cldrFile, xparts);
            case "intervalFormats":
                return forInterval(cldrFile, xparts);
            default:
                break;
        }
        return Set.of();
    }

    public enum SkeletonPathType {
        na,
        available,
        interval,
        datetime;

        public static SkeletonPathType fromParts(XPathParts xparts) {
            if (xparts.size() >= idElement) {
                switch (xparts.getElement(dateTypeElement)) {
                    case "availableFormats":
                        return xparts.getAttributeValue(idElement, "id") == null ? na : available;
                    case "intervalFormats":
                        return xparts.getAttributeValue(idElement, "id") == null ? na : interval;
                    case "dateFormatLength":
                    case "timeFormatLength":
                        return "datetimeSkeleton".equals(xparts.getElement(-1)) ? datetime : na;
                }
            }
            return na;
        }
    }

    private static Set<String> forAvailable(CLDRFile cldrFile, XPathParts xparts) {
        Set<String> skeletons = new LinkedHashSet<>();
        String skeleton = xparts.getAttributeValue(idElement, "id");
        addRelated(skeleton, "G", skeletons);
        addRelated(skeleton, "E", skeletons);
        addRelated(skeleton, "v", skeletons);
        if (skeletons.isEmpty()) {
            return Set.of();
        }
        XPathParts newPath = xparts.cloneAsThawed();
        Set<String> result = new LinkedHashSet<>();
        for (String item : skeletons) {
            addAvailable(cldrFile, newPath, item, result);
        }
        return result;
    }

    private static String addRelated(String skeleton, String letter, Set<String> skeletons) {
        if (skeleton.contains(letter)) {
            String newItem = skeleton.replace(letter, "");
            if (newItem.length() > 1 && !newItem.equals(skeleton)) {
                skeletons.add(newItem);
            }
        }
        return null;
    }

    private static Set<String> forInterval(CLDRFile cldrFile, XPathParts xparts) {
        XPathParts newAvailableParts = available.cloneAsThawed();
        String calendar = xparts.getAttributeValue(calendarElement, "type");
        newAvailableParts.putAttributeValue(calendarElement, "type", calendar);
        // get the related primary
        Set<String> result = new LinkedHashSet<>();

        // eg /intervalFormatItem[@id="Bhm"]
        String intervalId = xparts.getAttributeValue(idElement, "id");
        addAvailable(cldrFile, newAvailableParts, intervalId, result);

        // Now the trickier part. The goal is to get the available formats that are parts of a
        // whole.
        // Examples:
        // Dec 12-13: the first is the longest, and matches the id; the second is different
        // 12 - 1 pm: the second is the longest, and matches the id; the first is different
        // 12:30 - 45 PST: both are different, and neither matches the ID

        String intervalPattern = cldrFile.getStringValue(xparts.toString());
        if (intervalPattern != null) {
            CldrIntervalFormat intervalFormat =
                    CldrIntervalFormat.getInstance(calendar, intervalPattern);
            String intervalId2 =
                    CldrIntervalFormat.removeMissingFieldsFromSkeleton(
                            intervalId, intervalFormat.firstFields);
            addAvailable(cldrFile, newAvailableParts, intervalId2, result);
            String intervalId3 =
                    CldrIntervalFormat.removeMissingFieldsFromSkeleton(
                            intervalId, intervalFormat.secondFields);
            addAvailable(cldrFile, newAvailableParts, intervalId3, result);
        }
        return ImmutableSet.copyOf(result);
    }

    private static void addAvailable(
            CLDRFile cldrFile, XPathParts newPath, String intervalId, Set<String> result) {
        newPath.putAttributeValue(idElement, "id", intervalId);
        String pattern = cldrFile.getStringValue(newPath.toString());
        if (pattern != null) {
            result.add(pattern);
        }
    }

    public static Set<String> getCores(String skeleton) {
        try {
            return skeletonToCores.get(skeleton);
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static LoadingCache<String, Set<String>> skeletonToCores =
            CacheBuilder.newBuilder()
                    .build(
                            new CacheLoader<String, Set<String>>() {
                                @Override
                                public Set<String> load(String skeleton) throws ExecutionException {
                                    Set<String> cores = new TreeSet<>(SKELETON_COMPARE);
                                    addCore(skeleton, "G", cores);
                                    addCore(skeleton, "E", cores);
                                    addCore(skeleton, "v", cores);
                                    return ImmutableSet.copyOf(cores);
                                }
                            });

    //    Above cache replaces
    // private static Map<String, Set<String>> skeletonToCores = new HashMap<>();
    //
    //    // # Root skeleton characters: [BEGHMQUWdhmsvwy]
    //    // # All skeleton characters:   [c Z]
    //
    //    private static Collection<String> getCores(String skeleton) {
    //        Collection<String> cores = skeletonToCores.get(skeleton);
    //        if (cores == null) {
    //            cores = new TreeSet<>(SKELETON_COMPARE);
    //            String newItem = addCore(skeleton, "G", cores);
    //            addCore(skeleton, "E", cores);
    //            addCore(skeleton, "v", cores);
    //        }
    //        skeletonToCores.put(skeleton, ImmutableSet.copyOf(cores));
    //        return cores;
    //    }

    private static String addCore(String skeleton, String letter, Collection<String> cores) {
        if (skeleton.contains(letter)) {
            String newItem = skeleton.replace(letter, "");
            if (!newItem.isEmpty() && !newItem.equals(skeleton) && cores.add(newItem)) {
                return newItem;
            }
        }
        return null;
    }

    public static Comparator<String> SKELETON_COMPARE =
            new Comparator<>() {
                final UnicodeSet ODD_DATE_FIELDS = new UnicodeSet("[UQWw]").freeze();
                final UnicodeSet DATE_FIELDS =
                        new UnicodeSet("[d G M Q U wW y]").freeze(); // E is shared
                final UnicodeSet YEAR_FIELDS = new UnicodeSet("[yU]").freeze();
                final UnicodeSet HOUR_FIELDS = new UnicodeSet("[Hh]").freeze();
                final UnicodeSet TIME_FIELDS = new UnicodeSet("[B hH m s v]").freeze();

                @Override
                public int compare(String o1, String o2) {
                    return ComparisonChain.start()
                            .compare(DATE_FIELDS.containsSome(o2), DATE_FIELDS.containsSome(o1))
                            .compare(
                                    ODD_DATE_FIELDS.containsSome(o1),
                                    ODD_DATE_FIELDS.containsSome(o2))
                            .compare(YEAR_FIELDS.containsSome(o2), YEAR_FIELDS.containsSome(o1))
                            .compare(o1.contains("U"), o2.contains("U"))
                            .compare(o1.contains("M"), o2.contains("M"))
                            .compare(o1.contains("d"), o2.contains("d"))
                            .compare(HOUR_FIELDS.containsSome(o2), HOUR_FIELDS.containsSome(o1))
                            .compare(o1.contains("m"), o2.contains("m"))
                            .compare(o1.contains("s"), o2.contains("s"))
                            .compare(o1.length(), o2.length())
                            .compare(o1, o2)
                            .result();
                }
            };
}
