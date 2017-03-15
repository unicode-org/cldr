package org.unicode.cldr.icu;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.icu.RegexManager.CldrArray;
import org.unicode.cldr.icu.RegexManager.Function;
import org.unicode.cldr.icu.RegexManager.PathValueInfo;
import org.unicode.cldr.icu.RegexManager.RegexResult;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;

/**
 * A mapper that converts supplemental LDML data from CLDR to the ICU data
 * structure.
 */
public class SupplementalMapper {
    private static final Pattern ARRAY_INDEX = PatternCache.get("(/[^\\[]++)(?:\\[(\\d++)\\])?$");
    private static final Map<String, String> enumMap = Builder.with(new HashMap<String, String>())
        .put("sun", "1").put("mon", "2").put("tues", "3").put("wed", "4")
        .put("thu", "5").put("fri", "6").put("sat", "7").get();
    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    static {
        numberFormat.setMinimumIntegerDigits(4);
    }

    private int fifoCounter;
    private String inputDir;
    private RegexManager regexMapper;
    private String debugXPath;

    private enum DateFieldType {
        from, to;

        public static DateFieldType toEnum(String value) {
            value = value.toLowerCase();
            if (value.equals("from") || value.equals("start")) {
                return from;
            } else if (value.equals("to") || value.equals("end")) {
                return to;
            } else {
                throw new IllegalArgumentException(value + " is not a valid date field type");
            }
        }
    };

    /**
     * Comparator for sorting LDML supplementalData xpaths.
     */
    private static Comparator<String> supplementalComparator = new Comparator<String>() {
        private final Pattern FROM_ATTRIBUTE = PatternCache.get("\\[@from=\"([^\"]++)\"]");
        private final Pattern WEEKDATA = PatternCache.get(
            "//supplementalData/weekData/(minDays|firstDay|weekendStart|weekendEnd).*");

        @Override
        public int compare(String arg0, String arg1) {
            Matcher[] matchers = new Matcher[2];
            String metazone = "//supplementalData/metaZones/metazoneInfo/timezone";
            if (arg0.startsWith(metazone) && arg1.startsWith(metazone)) {
                int startPos = metazone.length();
                boolean from0 = FROM_ATTRIBUTE.matcher(arg0).find(startPos);
                boolean from1 = FROM_ATTRIBUTE.matcher(arg1).find(startPos);
                if (from0 != from1) {
                    return from0 ? 1 : -1;
                } else {
                    // CLDRFile.ldmlComparator doesn't always order the from
                    // dates correctly, so use a regular string comparison.
                    return arg0.compareTo(arg1);
                }
            } else if (RegexManager.matches(WEEKDATA, arg0, arg1, matchers)) {
                // Sort weekData elements ourselves because ldmlComparator
                // sorts firstDay after minDays.
                String elem0 = matchers[0].group(1);
                String elem1 = matchers[1].group(1);
                int compareElem = elem0.compareTo(elem1);
                if (compareElem == 0) return compareElem;
                if (elem0.equals("weekendEnd")) {
                    return 1;
                } else if (elem1.equals("weekendEnd")) {
                    return -1;
                }
                return compareElem;
            }
            return CLDRFile.getComparator(DtdType.supplementalData).compare(arg0, arg1);
        }
    };

    /**
     * SupplementalMapper constructor.
     *
     * @param inputDir
     *            the directory that the input files are in
     */
    private SupplementalMapper(String inputDir) {
        this.inputDir = inputDir;
    }

    public static SupplementalMapper create(String inputDir) {
        SupplementalMapper mapper = new SupplementalMapper(inputDir);
        // Handlers for functions in regex file.
        RegexManager manager = new RegexManager("ldml2icu_supplemental.txt");
        manager.addFunction("date", new Function(2) {
            /**
             * args[0] = value
             * args[1] = type (i.e. from/to)
             */
            @Override
            protected String run(String... args) {
                DateFieldType dft = DateFieldType.toEnum(args[1].trim());
                return getSeconds(args[0], dft);
            }
        });
        manager.addFunction("algorithm", new Function(1) {
            @Override
            protected String run(String... args) {
                // Insert % into numberingSystems descriptions.
                String value = args[0];
                int percentPos = value.lastIndexOf('/') + 1;
                return value.substring(0, percentPos) + '%' + value.substring(percentPos);
            }
        });
        // Converts a number into a special integer that represents the number in
        // normalized scientific notation for ICU's RB parser.
        // Resultant integers are in the form -?xxyyyyyy, where xx is the exponent
        // offset by 50 and yyyyyy is the coefficient to 5 decimal places, e.g.
        // 14660000000000 -> 1.466E13 -> 63146600
        // 0.0001 -> 1E-4 -> 46100000
        // -123.456 -> -1.23456E-2 -> -48123456
        // args[0] = number to be converted
        // args[2] = an (optional) additional exponent offset,
        // e.g. -2 for converting percentages into fractions.
        manager.addFunction("exp", new Function(2) {
            @Override
            protected String run(String... args) {
                double value = Double.parseDouble(args[0]);
                if (value == 0) {
                    return "0";
                }
                int exponent = 50;
                if (args.length == 2) {
                    exponent += Integer.parseInt(args[1]);
                }
                String sign = value >= 0 ? "" : "-";
                value = Math.abs(value);
                while (value >= 10) {
                    value /= 10;
                    exponent++;
                }
                while (value < 1) {
                    value *= 10;
                    exponent--;
                }
                if (exponent < 0 || exponent > 99) {
                    throw new IllegalArgumentException("Exponent out of bounds: " + exponent);
                }
                return sign + exponent + Math.round(value * 100000);
            }
        });
        mapper.regexMapper = manager;
        return mapper;
    }

    /**
     * Loads an IcuData object of the specified type.
     *
     * @param outputName
     *            the type of data to be converted
     * @return an IcuData object
     */
    public IcuData fillFromCldr(String outputName) {
        Map<String, CldrArray> pathValueMap = new HashMap<String, CldrArray>();
        String category = outputName;
        if (outputName.equals("supplementalData")) {
            String[] categories = {
                //                "characters", explicitly skipped
//                "coverageLevels", explicitly skipped
//                "dayPeriods", done in processSupplemental
//                "genderList", done elsewhere??
                "languageInfo",
                //                "likelySubtags", done elsewhere??
//                "metaZones", done elsewhere??
//                "numberingSystems", done elsewhere??
//                "ordinals", done in processSupplemental
//                "pluralRanges", done in processSupplemental
//                "plurals", done in processSupplemental
//                "postalCodeData", deprecated
                "supplementalData",
                "subdivisions",
                "telephoneCodeData",
                "/../validity/"
                //                "windowsZones", done elsewhere??
            };
            for (String cat : categories) {
                loadValues(cat, pathValueMap);
            }
        } else {
            if (outputName.equals("metadata")) category = "supplementalMetadata";
            loadValues(category, pathValueMap);
        }
        regexMapper.addFallbackValues(pathValueMap);
        IcuData icuData = new IcuData(category + ".xml", outputName, false, enumMap);
        for (String rbPath : pathValueMap.keySet()) {
            CldrArray values = pathValueMap.get(rbPath);
            icuData.addAll(rbPath, values.sortValues(supplementalComparator));
        }
        // Final pass through IcuData object to clean up any fallback rbpaths
        // in the values.
        // Assume one value per fallback path.
        for (String rbPath : icuData) {
            List<String[]> values = icuData.get(rbPath);
            for (int i = 0, len = values.size(); i < len; i++) {
                String[] valueArray = values.get(i);
                if (valueArray.length != 1) continue;
                String value = valueArray[0];
                Matcher matcher = ARRAY_INDEX.matcher(value);
                if (!matcher.matches()) continue;
                String replacePath = matcher.group(1);
                List<String[]> replaceValues = icuData.get(replacePath);
                if (replaceValues == null) {
                    throw new RuntimeException(replacePath + " is missing from IcuData object.");
                }
                int replaceIndex = matcher.groupCount() > 1 ? Integer.valueOf(matcher.group(2)) : 0;
                if (replaceIndex >= replaceValues.size()) {
                    throw new RuntimeException(replaceIndex + " out of range of values in " + replacePath);
                }
                values.set(i, replaceValues.get(replaceIndex));
            }
        }
        // Hack to add the CLDR version
        if (outputName.equals("supplementalData")) {
            icuData.add("/cldrVersion", CLDRFile.GEN_VERSION);
        }
        return icuData;
    }

    /**
     * Loads values for the specified category from CLDR.
     *
     * @param category
     * @param pathValueMap
     *            the output map
     */
    private void loadValues(String category, Map<String, CldrArray> pathValueMap) {
        if (category.endsWith("/")) {
            File dir = new File(inputDir + category);
            for (File subfile : dir.listFiles()) {
                String name = subfile.getName();
                if (name.endsWith(".xml")) {
                    name = name.substring(0, name.length() - 4);
                    loadValues(category + name, pathValueMap);
                }
            }
            return;
        }
        String inputFile = new File(inputDir, category + ".xml").getAbsolutePath();
        List<Pair<String, String>> contents = new ArrayList<Pair<String, String>>();
        XMLFileReader.loadPathValues(inputFile, contents, true);
        RegexLookup<RegexResult> pathConverter = regexMapper.getPathConverter();
        fifoCounter = 0; // Helps to keep unsorted rb paths in order.
        XPathParts parts = new XPathParts();
        for (Pair<String, String> pair : contents) {
            Output<Finder> matcher = new Output<Finder>();
            String fullPath = parts.set(pair.getFirst()).toString();
            // Only convert contributed or higher data
            if (parts.containsAttributeValue("draft", "provisional") ||
                parts.containsAttributeValue("draft", "unconfirmed")) {
                continue;
            }
            List<String> debugResults = isDebugXPath(fullPath) ? new ArrayList<String>() : null;
            Output<String[]> argInfo = new Output<>();
            RegexResult regexResult = pathConverter.get(fullPath, null, argInfo, matcher, debugResults);
            if (regexResult == null) {
                RegexManager.printLookupResults(fullPath, debugResults);
                continue;
            }
            if (debugResults != null) {
                System.out.println(fullPath + " successfully matched");
            }
            // String[] arguments = matcher.value.getInfo();
            String[] arguments = argInfo.value;
            String cldrValue = pair.getSecond();
            for (PathValueInfo info : regexResult) {
                List<String> values = info.processValues(arguments, cldrValue);
                // Check if there are any arguments that need splitting for the rbPath.
                String groupKey = info.processGroupKey(arguments);
                String baseXPath = info.processXPath(arguments, fullPath);
                boolean splitNeeded = false;
                int argIndex = info.getSplitRbPathArg();
                if (argIndex != -1) {
                    String[] splitArgs = arguments[argIndex].split("\\s++");
                    // Only split the first splittable argument needed for each rbPath.
                    if (splitArgs.length > 1) {
                        String[] newArgs = arguments.clone();
                        for (String splitArg : splitArgs) {
                            newArgs[argIndex] = splitArg;
                            String rbPath = info.processRbPath(newArgs);
                            processValues(baseXPath, rbPath, values, groupKey, pathValueMap);
                        }
                        splitNeeded = true;
                    }
                }
                // No splitting required, process as per normal.
                if (!splitNeeded) {
                    String rbPath = info.processRbPath(arguments);
                    processValues(baseXPath, rbPath, values, groupKey, pathValueMap);
                }
            }
            fifoCounter++;
        }
    }

    /**
     * Processes values to be added to the ICU data structure
     *
     * @param xpath
     *            the CLDR path that the values came from
     * @param rbPath
     *            the rbPath that the values belong to
     * @param values
     *            the values
     * @param groupKey
     *            the key that the values should be grouped by
     * @param pathValueMap
     *            the output map
     */
    private void processValues(String xpath, String rbPath, List<String> values,
        String groupKey, Map<String, CldrArray> pathValueMap) {
        // The fifo counter needs to be formatted with leading zeros for sorting.
        if (rbPath.contains("<FIFO>")) {
            rbPath = rbPath.replace("<FIFO>", '<' + numberFormat.format(fifoCounter) + '>');
        }
        CldrArray cldrArray = RegexManager.getCldrArray(rbPath, pathValueMap);
        cldrArray.put(xpath, values, groupKey);
    }

    /**
     * Converts a date string to a pair of millisecond values.
     *
     * @param dateStr
     * @return
     */
    private static String getSeconds(String dateStr, DateFieldType type) {
        long millis;
        try {
            millis = getMilliSeconds(dateStr, type);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Could not parse date: " + dateStr, ex);
        }

        int top = (int) ((millis & 0xFFFFFFFF00000000L) >>> 32); // top
        int bottom = (int) ((millis & 0x00000000FFFFFFFFL)); // bottom

        if (NewLdml2IcuConverter.DEBUG) {
            long bot = 0xffffffffL & bottom;
            long full = ((long) (top) << 32);
            full += bot;
            if (full != millis) {
                System.err.println("Error when converting " + millis + ": " +
                    top + ", " + bottom + " was converted back into " + full);
            }
        }

        return top + " " + bottom;
    }

    /**
     * Parses a string date and normalizes it depending on what type of date it
     * is.
     *
     * @param dateStr
     * @param type
     *            whether the date is a from or a to
     * @return
     * @throws ParseException
     */
    private static long getMilliSeconds(String dateStr, DateFieldType type)
        throws ParseException {
        int count = countHyphens(dateStr);
        SimpleDateFormat format = new SimpleDateFormat();
        if (count == 2) {
            format.applyPattern("yyyy-MM-dd");
        } else {
            throw new RuntimeException("Tried to parse invalid date: " + dateStr);
        }
        TimeZone timezone = TimeZone.getTimeZone("GMT");
        format.setTimeZone(timezone);
        Date date = format.parse(dateStr);
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeZone(timezone);
        calendar.setTime(date);
        switch (type) {
        case from: {
            // Set the times for to fields to the beginning of the day.
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            break;
        }
        case to: {
            // Set the times for to fields to the end of the day.
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            break;
        }
        }
        return calendar.getTimeInMillis();
    }

    /**
     * Counts the number of hyphens in a string.
     *
     * @param str
     * @return
     */
    private static int countHyphens(String str) {
        // Hyphens in front are actually minus signs.
        int lastPos = 0;
        int numHyphens = 0;
        while ((lastPos = str.indexOf('-', lastPos + 1)) > -1) {
            numHyphens++;
        }
        return numHyphens;
    }

    /**
     * Sets xpath to monitor for debugging purposes.
     * @param debugXPath
     */
    public void setDebugXPath(String debugXPath) {
        this.debugXPath = debugXPath;
    }

    /**
     * @param xpath
     * @return true if the xpath is to be debugged
     */
    boolean isDebugXPath(String xpath) {
        return debugXPath == null ? false : xpath.startsWith(debugXPath);
    }
}
