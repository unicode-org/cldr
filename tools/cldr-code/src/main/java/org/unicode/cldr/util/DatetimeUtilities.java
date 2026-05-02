package org.unicode.cldr.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.PatternInfo;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.tool.CheckDatePatternOrder;
import org.unicode.cldr.util.NestedMap.Multimap2;

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
        private final Map<String, String> lengthToPattern;
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
                DateTimePatternGenerator generatorWithStock,
                Map<String, String> lengthToPattern) {
            this.stockSkeletonToPattern = CldrUtility.protectCollection(stockSkeletonToPattern);
            this.lengthToPattern = CldrUtility.protectCollection(lengthToPattern);
            this.availableSkeletonToPattern =
                    CldrUtility.protectCollection(availableSkeletonToPattern);
            this.appendItems = CldrUtility.protectCollection(appendItems);
            this.appendDateAndTime = CldrUtility.protectCollection(appendDateAndTime);
            this.generatorNoStock = generatorNoStock;
            this.generatorWithStock = generatorWithStock;
        }

        public Map<String, String> getLengthToPattern() {
            return lengthToPattern;
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
                    generatorWithStock,
                    null);
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
                    generatorWithStock,
                    null);
        }
    }

    private static class Builder {
        Map<String, String> lengthToSkeleton = new TreeMap<>();
        Map<String, String> lengthToPattern = new TreeMap<>();
        Map<String, String> availableSkeletonToPattern = new TreeMap<>();
        Map<String, String> stockSkeletonToPattern = new TreeMap<>();
        Map<String, String> appendItems = new TreeMap<>();
        Map<String, String> appendDateAndTime = new TreeMap<>();
        Map<String, Pair<String, String>> paths = new TreeMap<>();
    }

    private enum StockSkeletonType {
        date_full,
        date_long,
        date_medium,
        date_short,
        time_full,
        time_long,
        time_medium,
        time_short;
    }

    public static Multimap2<String, String, String> missingSkeletonsForLengths =
            Multimap2.create(TreeMap::new);

    public static Map<String, DatePatternInfo> calendarToDatePatternInfo(CLDRFile cldrFile) {
        Map<String, Builder> result = new TreeMap<>();

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
            if (parts.size() < 5
                    || !"dates".equals(parts.getElement(1))
                    || "alias".equals(lastElement)
                    || parts.getAttributeValue(-1, "alt") != null
                    || parts.getAttributeValue(-1, "count") != null) {
                continue;
            }
            String calendarElement = parts.getElement(3);
            if (!"calendar".equals(calendarElement)) {
                continue;
            }
            String calendarAttribute = parts.getAttributeValue(3, "type");
            if (calendarAttribute == null) {
                continue;
            }
            Builder builder = result.computeIfAbsent(calendarAttribute, x -> new Builder());

            String value = cldrFile.getStringValue(path);
            String key;
            Map<String, String> map;

            switch (parts.getElement(4)) {
                case "dateFormats":
                    key = "date_" + parts.getAttributeValue(5, "type");
                    map =
                            lastElement.equals("pattern")
                                    ? builder.lengthToPattern
                                    : builder.lengthToSkeleton;
                    break;
                case "timeFormats":
                    key = "time_" + parts.getAttributeValue(5, "type");
                    map =
                            lastElement.equals("pattern")
                                    ? builder.lengthToPattern
                                    : builder.lengthToSkeleton;
                    break;
                case "dateTimeFormats":
                    switch (parts.getElement(5)) {
                        case "appendItems":
                            key = parts.getAttributeValue(6, "request");
                            map = builder.appendItems;
                            break;
                        case "availableFormats":
                            key = parts.getAttributeValue(6, "id");
                            map = builder.availableSkeletonToPattern;
                            break;
                        case "dateTimeFormatLength":
                            key = parts.getAttributeValue(5, "type");
                            map = builder.appendDateAndTime;
                            break;
                        default:
                            continue; // SKIP path
                    }
                    break;
                default:
                    continue; // SKIP path
            }
            builder.paths.put(PathStarrer.get(path).toString(), Pair.of(path, value));
            map.put(key, value);
        }

        Map<String, DatePatternInfo> realResult = new TreeMap<>();

        for (Entry<String, Builder> entry : result.entrySet()) {
            String calendar = entry.getKey();
            Builder builder = entry.getValue();

            for (String length : builder.lengthToPattern.keySet()) {
                String skeletonForLength = builder.lengthToSkeleton.get(length);
                if (skeletonForLength == null) {
                    missingSkeletonsForLengths.put(calendar, length, cldrFile.getLocaleID());
                    continue;
                } else if (skeletonForLength.equals("↑↑↑")) {
                    continue;
                }
                builder.stockSkeletonToPattern.put(
                        skeletonForLength, builder.lengthToPattern.get(length));
            }

            DatePatternInfo value =
                    new DatePatternInfo(
                            builder.stockSkeletonToPattern,
                            builder.availableSkeletonToPattern,
                            builder.appendItems,
                            builder.appendDateAndTime,
                            null,
                            null,
                            builder.lengthToPattern);
            realResult.put(calendar, value);
        }
        return CldrUtility.protectCollection(realResult);
    }

    static final UnicodeSet legalChars = new UnicodeSet("[GUyQMdEBhHmsv]");
    static final Pattern illegalWidths = Pattern.compile("GGG?|EEE?|BBB?|vvv?");
    static final Pattern legalWidths = Pattern.compile("GGGG|EEEE|BBBB|vvvv");

    public static String idStatus(String string) {
        return string.equals(normalizePattern(string, null)) ? null : "Bad";
        //        if (!legalChars.containsAll(string)) {
        //            return "Bad Characters: " + new
        // UnicodeSet().addAll(string).removeAll(legalChars);
        //        }
        //        Matcher matcher = illegalWidths.matcher(string);
        //        if (matcher.find()) {
        //            Matcher matcher2 = legalWidths.matcher(string);
        //            if (!matcher2.find()) {
        //                return "Bad Lengths: " + matcher.group();
        //            }
        //        }
        //        return null;
    }

    static final Map<String, String> replacements =
            ImmutableMap.<String, String>builder()
                    .put("Y", "y")
                    .put("u", "y")
                    .put("r", "")
                    .put("L", "M")
                    .put("c", "E")
                    .put("e", "E")
                    .put("a", "")
                    .put("b", "")
                    .put("K", "h")
                    .put("k", "H")
                    .put("S", "")
                    .put("z", "v")
                    .put("Z", "v")
                    .put("O", "v")
                    .put("V", "v")
                    .build();

    public static String normalizePattern(String string, List<String> causes) {
        // fix illegal characters.
        Output<String> result = new Output<>(string);
        replacements.entrySet().stream()
                .forEach(
                        x -> {
                            String newString = result.value.replace(x.getKey(), x.getValue());
                            if (!newString.equals(result.value)) {
                                result.value = newString;
                                if (causes != null) {
                                    if (causes != null) {
                                        causes.add(x.getKey() + "→" + x.getValue());
                                    }
                                }
                            }
                        });

        // now fix widths.

        List<String> fields = getPatternFields(result.value);
        StringBuilder b = new StringBuilder();
        for (String field : fields) {
            String newField = null;
            switch (field) {
                case "GG":
                case "GGG":
                case "GGGGG":
                    newField = "G";
                    break;
                case "EE":
                case "EEE":
                    newField = "E";
                    break;
                case "BB":
                case "BBB":
                    newField = "B";
                    break;
                case "vv":
                case "vvv":
                case "vvvv":
                    newField = "v";
                    break; // we shouldn't need different patterns for different v widths
                // numeric time fields should all be singletons
                case "hh":
                    newField = "h";
                    break;
                case "HH":
                    newField = "H";
                    break;
                case "mm":
                    newField = "m";
                    break;
                case "ss":
                    newField = "s";
                    break;
                // as should the day of month and numeric month
                case "dd":
                    newField = "d";
                    break;
                case "MM":
                    newField = "M";
                    break;
                    // We need to check on y; whether we need to distinguish yyyy, yy and y or not
                    // That is, whether the same results would obtain after adjustments
            }
            if (newField == null) {
                b.append(field);
            } else {
                if (causes != null) {
                    causes.add(field + "🠞" + newField);
                }
                b.append(newField);
            }
        }
        return b.toString();
    }

    static final Pattern identicalSequence = Pattern.compile("(.)\\1*");

    public static List<String> getPatternFields(String pattern) {
        List<String> result = new ArrayList<>();
        Matcher matcher = identicalSequence.matcher(pattern);
        while (matcher.find()) {
            result.add(matcher.group(0));
        }
        return result;
    }

    /** Clean replacement for VariableField */
    public static final class PatternElement implements Comparable<PatternElement> {
        private final String element;
        private final int type;
        private final boolean numeric;

        private PatternElement(String element, int type, boolean numeric) {
            this.element = element;
            this.type = type; // determined by the element
            this.numeric = numeric; // determined by the element
        }

        @SuppressWarnings("deprecation")
        public static PatternElement from(VariableField vf) {
            return new PatternElement(vf.toString(), vf.getType(), vf.isNumeric());
        }

        @SuppressWarnings("deprecation")
        public VariableField toVariableField(VariableField vf) {
            return new VariableField(element);
        }

        @Override
        public int compareTo(PatternElement other) {
            return Comparator //
                    .comparing((PatternElement x) -> x.type)
                    .thenComparing(x -> x.numeric)
                    .thenComparing(x -> x.element)
                    .compare(this, other);
        }

        @Override
        public int hashCode() {
            return element.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return element.equals(((PatternElement) obj).element);
        }

        @Override
        public String toString() {
            return element;
        }
    }

    private static final ConcurrentHashMap<String, PatternSortKey> PatternSortKeyCache =
            new ConcurrentHashMap<>();

    public static class PatternSortKey implements Comparable<PatternSortKey> {
        byte[] elements;

        public PatternSortKey(List<Integer> sortKey) {
            elements = new byte[sortKey.size()];
            for (int i = 0; i < sortKey.size(); ++i) {
                elements[i] = sortKey.get(i).byteValue();
            }
        }

        static PatternSortKey getSortKey(String pattern) {
            return PatternSortKeyCache.computeIfAbsent(
                    pattern,
                    y -> {
                        Set<PatternElement> patternElements = getPatternElements(y);
                        List<Integer> sortKey = new ArrayList<>();
                        // We ensure that all of the integers are between 1..127 inclusive
                        patternElements.stream().forEach(x -> sortKey.add(x.type + 1));
                        sortKey.add(0);
                        patternElements.stream().forEach(x -> sortKey.add(x.numeric ? 1 : 2));
                        sortKey.add(0);
                        patternElements.stream()
                                .forEach(x -> sortKey.add(x.element.length())); // length, longest
                        // first
                        sortKey.add(0);
                        patternElements.stream()
                                .forEach(
                                        x ->
                                                sortKey.add(
                                                        (int)
                                                                x.element.charAt(
                                                                        0))); // pattern character
                        sortKey.add(0);
                        return new PatternSortKey(sortKey);
                    });
        }

        @Override
        public int compareTo(PatternSortKey other) {
            int minLen = Math.min(this.elements.length, other.elements.length);
            for (int i = 0; i < minLen; ++i) {
                int diff = elements[i] - other.elements[i];
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }

        public static int compare(String pattern1, String pattern2) {
            return getSortKey(pattern1).compareTo(getSortKey(pattern2));
        }

        @Override
        public String toString() {
            return Arrays.asList(elements).toString();
        }

        @Override
        public boolean equals(Object other) {
            return Arrays.equals(elements, ((PatternSortKey) other).elements);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(elements);
        }
    }

    public static final Comparator<String> PATTERN_COMPARATOR =
            (a, b) -> PatternSortKey.compare(a, b);

    private static final ConcurrentHashMap<String, Set<PatternElement>> PatternVariableFieldCache =
            new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    public static final Set<PatternElement> getPatternElements(String pattern) {
        return PatternVariableFieldCache.computeIfAbsent(
                pattern,
                y -> {
                    FormatParser fp = new DateTimePatternGenerator.FormatParser();
                    fp.set(y);
                    return fp.getItems().stream()
                            .map(x -> PatternElement.from((VariableField) x))
                            .collect(
                                    ImmutableSortedSet.toImmutableSortedSet(
                                            Comparator.naturalOrder()));
                });
    }

    // temporary for quick testing; needs to be removed and a test added

    public static void main(String[] args) {
        Set<PatternElement> pe1 = getPatternElements("Gy");
        Set<PatternElement> pe2 = getPatternElements("yG");
        Set<PatternElement> pe3 = getPatternElements("y");
        Set<PatternElement> pe4 = getPatternElements("G");
        System.out.println(PATTERN_COMPARATOR.compare("Gy", "y"));

        System.out.println(
                SkeletonField.getDatecombos().size()
                        + "\t"
                        + Joiners.N.join(SkeletonField.getDatecombos()));
    }
}
