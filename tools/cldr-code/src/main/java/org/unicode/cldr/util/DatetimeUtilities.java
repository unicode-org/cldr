package org.unicode.cldr.util;

import com.google.common.collect.Multimap;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.PatternInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.tool.CheckDatePatternOrder;

/** This is a set of utilities for dealing with different date/time data */
public class DatetimeUtilities extends TestFmwk {

    public enum SkeletonField {
        era(1, 4, "G"),
        year(1, 4, "y"),
        month(1, 4, "M"),
        day(1, 2, "d"),
        dow(1, 4, "E"),
        dp(1, 4, "bB"), // note: 'a' will be implicitly added to 'h' in *patterns*, if there is no
        // dayPeriod
        hour(1, 2, "hH"),
        minute(1, 2, "m"),
        second(1, 2, "s"),
        zone(1, 5, "vZOvV");

        private final List<String> letters;
        private static final List<String> dateCombos;

        public List<String> getLetters() {
            return letters;
        }

        private SkeletonField(int start, int end, String letterOptions) {
            List<String> result = new ArrayList<>();
            StringBuilder temp = new StringBuilder();
            letterOptions
                    .codePoints()
                    .forEach(
                            x -> {
                                temp.setLength(0);
                                for (int i = 1; i <= end; ++i) {
                                    temp.appendCodePoint(x);
                                    if (i >= start) {
                                        result.add(temp.toString());
                                    }
                                }
                            });
            letters = List.copyOf(result);
        }

        static {
            List<String> dateCombinations = new ArrayList<String>();

            // dates

            // year + day requires month
            // era requires year
            // dow requires day
            generateCombinations(dateCombinations, era, year, month, day, dow);
            generateCombinations(dateCombinations, era, year, month, day);
            generateCombinations(dateCombinations, era, year, month);
            generateCombinations(dateCombinations, era, year);
            generateCombinations(dateCombinations, year, month, day, dow);
            generateCombinations(dateCombinations, year, month, day);
            generateCombinations(dateCombinations, year, month);
            generateCombinations(dateCombinations, month, day, dow);
            generateCombinations(dateCombinations, month, day);
            generateCombinations(dateCombinations, day, dow);

            // isolates for completeness

            generateCombinations(dateCombinations, era);
            generateCombinations(dateCombinations, year);
            generateCombinations(dateCombinations, month);
            generateCombinations(dateCombinations, day);
            generateCombinations(dateCombinations, dow);

            // times

            // hour always required
            // second requires minute
            generateCombinations(dateCombinations, dp, hour, minute, second, zone);
            generateCombinations(dateCombinations, dp, hour, minute, zone);
            generateCombinations(dateCombinations, dp, hour, zone);
            generateCombinations(dateCombinations, dp, hour, minute, second);
            generateCombinations(dateCombinations, dp, hour, minute);
            generateCombinations(dateCombinations, hour, minute, second, zone);
            generateCombinations(dateCombinations, hour, minute, zone);
            generateCombinations(dateCombinations, hour, zone);
            generateCombinations(dateCombinations, hour, minute, second);
            generateCombinations(dateCombinations, hour, minute);

            // isolates for completeness

            generateCombinations(dateCombinations, dp);
            generateCombinations(dateCombinations, hour);
            generateCombinations(dateCombinations, minute);
            generateCombinations(dateCombinations, second);
            generateCombinations(dateCombinations, zone);

            dateCombos = List.copyOf(dateCombinations);
        }

        private static void generateCombinations(List<String> toAddTo, SkeletonField... fields) {
            generateCombinations(
                    toAddTo, 0, new LinkedHashSet<String>(Arrays.asList(new String())), fields);
        }

        private static void generateCombinations(
                List<String> toAddTo,
                int depth,
                LinkedHashSet<String> toExtend,
                SkeletonField... fields) {

            // Base case: if depth equals input size, we've generated all that we need
            if (depth >= fields.length) {
                toAddTo.addAll(toExtend);
                return;
            }

            // Iterate through all characters in the current sub-list

            for (String base : new ArrayList<>(toExtend)) { // avoid ConcurrentModificationException
                for (String s : fields[depth].getLetters()) {
                    toExtend.add(base + s);
                }
                toExtend.remove(base); // mark the base as one to discard
            }
            generateCombinations(toAddTo, depth + 1, toExtend, fields);
        }

        public static List<String> getDatecombos() {
            return dateCombos;
        }
    }

    public static class DatePatternInfo {
        private final Map<String, String> stockSkeletonToPattern;
        private final Map<String, String> availableSkeletonToPattern;
        private final Map<String, String> appendItems;
        private final Map<String, String> appendDateAndTime;

        private final DateTimePatternGenerator generatorNoStock;

        private final DateTimePatternGenerator generatorWithStock;

        private DatePatternInfo(
                Map<String, String> stockSkeletonToPattern,
                Map<String, String> availableSkeletonToPattern,
                Map<String, String> appendItems,
                Map<String, String> appendDateAndTime,
                DateTimePatternGenerator generatorNoStock,
                DateTimePatternGenerator generatorWithStock) {
            this.stockSkeletonToPattern = stockSkeletonToPattern;
            this.availableSkeletonToPattern = availableSkeletonToPattern;
            this.appendItems = appendItems;
            this.appendDateAndTime = appendDateAndTime;
            this.generatorNoStock = generatorNoStock;
            this.generatorWithStock = generatorWithStock;
        }

        public DateTimePatternGenerator getGenerator(boolean withStock) {
            return withStock ? generatorWithStock : generatorNoStock;
        }

        public Map<String, String> getStockSkeletonToPattern() {
            return stockSkeletonToPattern;
        }

        public Map<String, String> getAvailableSkeletonToPattern() {
            return availableSkeletonToPattern;
        }

        public Map<String, String> getAppendItems() {
            return appendItems;
        }

        public Map<String, String> getAppendDateAndTime() {
            return appendDateAndTime;
        }

        @Override
        public String toString() {
            return stockSkeletonToPattern
                    + "\n"
                    + availableSkeletonToPattern
                    + "\n"
                    + appendItems
                    + "\n"
                    + appendDateAndTime;
        }

        public static final DatePatternInfo from(CLDRFile cldrFile, String calendar) {
            return fromInternal(cldrFile, calendar);
        }

        public static final DatePatternInfo fromInternal(CLDRFile cldrFile, String calendar) {
            Map<String, String> lengthToSkeleton = new TreeMap<>();
            Map<String, String> lengthToPattern = new TreeMap<>();
            Map<String, String> availableSkeletonToPattern = new TreeMap<>();
            Map<String, String> stockSkeletonToPattern = new TreeMap<>();
            Map<String, String> appendItems = new TreeMap<>();
            Map<String, String> appendDateAndTime = new TreeMap<>();
            Map<String, Pair<String, String>> paths = new TreeMap<>();

            for (String path : cldrFile) {
                // spotless:off
                // only need to look at the following paths (examples)
                
                //ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type="medium"]/dateFormat[@type="standard"]/datetimeSkeleton   yMMMd
                //ldml/dates/calendars/calendar[@type="gregorian"]/dateFormats/dateFormatLength[@type="long"]/dateFormat[@type="standard"]/pattern[@type="standard"]    MMMM d, y
                //ldml/dates/calendars/calendar[@type="gregorian"]/timeFormats/timeFormatLength[@type="medium"]/timeFormat[@type="standard"]/datetimeSkeleton   ahmmss
                //ldml/dates/calendars/calendar[@type="gregorian"]/timeFormats/timeFormatLength[@type="medium"]/timeFormat[@type="standard"]/pattern[@type="standard"]  h:mm:ss a
                
                //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/appendItems/appendItem[@request="Hour"]  {0} ({2}: {1})
                //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/availableFormats/dateFormatItem[@id="hms"]   h:mm:ss a
                //ldml/dates/calendars/calendar[@type="gregorian"]/dateTimeFormats/dateTimeFormatLength[@type="full"]/dateTimeFormat[@type="relative"]/pattern[@type="standard"]    {1} 'at' {0}
                // skipping intervals
                
                // NOTE: the skeletons are not attributes for the stock formats; need to look at pairs, so we first gather into separate maps
                //ldml/dates/calendars/calendar[@type="([^"]*+)"]/dateFormats/dateFormatLength[@type="([^"]*+)"]/dateFormat[@type="([^"]*+)"]/datetimeSkeleton=rMd
                //                          <pattern>EEEE, MMMM d, r(U)</pattern>
                //                          <datetimeSkeleton>rMMMMEEEEd</datetimeSkeleton>
                // spotless:on
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String lastElement = parts.getElement(-1);
                if (!"dates".equals(parts.getElement(1))
                        || !calendar.equals(parts.getAttributeValue(3, "type"))
                        || "alias".equals(lastElement)
                        || parts.getAttributeValue(-1, "alt") != null
                        || parts.getAttributeValue(-1, "count") != null) {
                    continue;
                }
                String value = cldrFile.getStringValue(path);
                String key;
                Map<String, String> map;

                switch (parts.getElement(4)) {
                    case "dateFormats":
                        key = "date" + parts.getAttributeValue(5, "type");
                        map = lastElement.equals("pattern") ? lengthToPattern : lengthToSkeleton;
                        break;
                    case "timeFormats":
                        key = "time" + parts.getAttributeValue(5, "type");
                        map = lastElement.equals("pattern") ? lengthToPattern : lengthToSkeleton;
                        break;
                    case "dateTimeFormats":
                        switch (parts.getElement(5)) {
                            case "appendItems":
                                key = parts.getAttributeValue(6, "request");
                                map = appendItems;
                                break;
                            case "availableFormats":
                                key = parts.getAttributeValue(6, "id");
                                map = availableSkeletonToPattern;
                                break;
                            case "dateTimeFormatLength":
                                key = parts.getAttributeValue(5, "type");
                                map = appendDateAndTime;
                                break;
                            default:
                                continue; // SKIP path
                        }
                        break;
                    default:
                        continue; // SKIP path
                }
                paths.put(PathStarrer.get(path).toString(), Pair.of(path, value));
                map.put(key, value);
            }
            for (String length : lengthToPattern.keySet()) {
                stockSkeletonToPattern.put(
                        lengthToSkeleton.get(length), lengthToPattern.get(length));
            }

            DateTimePatternGenerator generatorNoStock =
                    getGenerator(null, availableSkeletonToPattern, appendItems, appendDateAndTime);
            DateTimePatternGenerator generatorWithStock =
                    getGenerator(
                            stockSkeletonToPattern,
                            availableSkeletonToPattern,
                            appendItems,
                            appendDateAndTime);

            return new DatePatternInfo(
                    stockSkeletonToPattern,
                    availableSkeletonToPattern,
                    appendItems,
                    appendDateAndTime,
                    generatorNoStock,
                    generatorWithStock);
        }

        public static DateTimePatternGenerator getGenerator(
                Map<String, String> stockSkeletonToPattern,
                Map<String, String> availableSkeletonToPattern,
                Map<String, String> appendItems,
                Map<String, String> appendDateAndTime) {
            PatternInfo returnInfo = new PatternInfo();
            DateTimePatternGenerator generator = DateTimePatternGenerator.getEmptyInstance();
            if (stockSkeletonToPattern != null) {
                stockSkeletonToPattern.entrySet().stream()
                        .forEach(
                                x ->
                                        generator.addPatternWithSkeleton(
                                                x.getKey(), x.getValue(), false, returnInfo));
            }
            availableSkeletonToPattern.entrySet().stream()
                    .forEach(
                            x ->
                                    generator.addPatternWithSkeleton(
                                            x.getValue(), x.getKey(), false, returnInfo));
            appendItems.entrySet().stream()
                    .forEach(
                            x ->
                                    generator.setAppendItemFormat(
                                            fieldToInt(x.getKey()), x.getValue()));
            appendDateAndTime.entrySet().stream()
                    .forEach(
                            x -> generator.setDateTimeFormat(widthToInt(x.getKey()), x.getValue()));
            generator.freeze();
            return generator;
        }

        private static int widthToInt(String key) {
            switch (key) {
                case "full":
                    return 0;
                case "long":
                    return 1;
                case "medium":
                    return 2;
                case "short":
                    return 3;
                default:
                    throw new IllegalArgumentException();
            }
        }

        private static int fieldToInt(String key) {
            return DateTimePatternGenerator.getAppendFormatNumber(key);
        }

        public String getBestPattern(String skeleton, boolean withStock) {
            return getGenerator(withStock).getBestPattern(skeleton);
        }

        public String getSkeletonFromPattern(String pattern, boolean withStock) {
            return getGenerator(withStock).getSkeleton(pattern);
        }

        public boolean stockMatchesGenerated() {
            return stockSkeletonToPattern.entrySet().stream()
                    .map(
                            x ->
                                    CheckDatePatternOrder.mappedEqual(
                                            x.getValue(),
                                            getBestPattern(x.getKey(), false),
                                            CheckDatePatternOrder.FIX_SPACE))
                    .reduce(true, (a, b) -> a && b);
        }

        public static String header(String prefix) {
            return Joiners.TAB.join(
                    prefix,
                    "actual skeleton",
                    "actual pattern",
                    "gen pattern",
                    "imputed skeleton",
                    "imputed pattern");
        }

        public String getStockDelta(String prefix) {
            Function<Entry<String, String>, String> foo =
                    x -> {
                        String imputedSkeleton = getSkeletonFromPattern(x.getValue(), false);
                        String best = getBestPattern(x.getKey(), false);
                        return Joiners.TAB.join(
                                prefix,
                                x.getKey(),
                                CheckDatePatternOrder.FIX_SPACE.apply(x.getValue()),
                                CheckDatePatternOrder.FIX_SPACE.apply(best),
                                imputedSkeleton,
                                CheckDatePatternOrder.FIX_SPACE.apply(
                                        getBestPattern(imputedSkeleton, false)));
                    };
            return stockSkeletonToPattern.entrySet().stream()
                    .map(foo)
                    .collect(Collectors.joining("\n"));

            //
        }

        public DatePatternInfo getWithAvailableReplaced(
                String key, Multimap<String, List<String>> replacements) {
            Map<String, String> availableSkeletonToPatternNew =
                    new TreeMap<>(availableSkeletonToPattern);
            stockSkeletonToPattern.entrySet().stream()
                    .forEach(
                            x -> {
                                // String skeleton = x.getKey();
                                String pattern = x.getValue();
                                String baseSkeleton = getGenerator(false).getBaseSkeleton(pattern);
                                String patternForBase =
                                        availableSkeletonToPattern.get(baseSkeleton);
                                if (CheckDatePatternOrder.equalIgnoringSpaceVariants(
                                        pattern, patternForBase)) {
                                    return;
                                }
                                String imputedSkeleton = getGenerator(false).getSkeleton(pattern);
                                String patternForImputed =
                                        availableSkeletonToPattern.get(imputedSkeleton);
                                if (CheckDatePatternOrder.equalIgnoringSpaceVariants(
                                        pattern, patternForImputed)) {
                                    return;
                                }
                                // neither method worked
                                String previousPattern =
                                        availableSkeletonToPatternNew.put(baseSkeleton, pattern);
                                if (previousPattern != null) {
                                    replacements.put(
                                            key, List.of(baseSkeleton, pattern, previousPattern));
                                }
                            });

            DateTimePatternGenerator generatorNoStock =
                    getGenerator(
                            null, availableSkeletonToPatternNew, appendItems, appendDateAndTime);
            DateTimePatternGenerator generatorWithStock =
                    getGenerator(
                            stockSkeletonToPattern,
                            availableSkeletonToPatternNew,
                            appendItems,
                            appendDateAndTime);

            return new DatePatternInfo(
                    stockSkeletonToPattern,
                    availableSkeletonToPatternNew,
                    appendItems,
                    appendDateAndTime,
                    generatorNoStock,
                    generatorWithStock);
        }
    }

    // temporary for testing

    public static void main(String[] args) {
        System.out.println(
                SkeletonField.getDatecombos().size()
                        + "\t"
                        + Joiners.N.join(SkeletonField.getDatecombos()));
    }
}
