package org.unicode.cldr.icu;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;

/**
 * A mapper that converts supplemental LDML data from CLDR to the ICU data
 * structure.
 */
public class SupplementalMapper extends LdmlMapper {
    private static final Map<String, String> enumMap = Builder.with(new HashMap<String, String>())
            .put("sun", "1").put("mon", "2").put("tues", "3").put("wed", "4")
            .put("thu", "5").put("fri", "6").put("sat", "7").get();
    private static final Pattern DATE_PATH = Pattern.compile("/CurrencyMap/.*/(from|to):intvector");
    private static final Pattern NUMBERING_SYSTEMS_DESC = Pattern.compile("/numberingSystems/\\w++/desc");
    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    static {
        numberFormat.setMinimumIntegerDigits(4);
    }

    private int fifoCounter;
    private String inputDir;
    private String cldrVersion;

    /**
     * Comparator for sorting LDML supplementalData xpaths.
     */
    private static Comparator<String> supplementalComparator = new Comparator<String>() {
        private final Pattern FROM_ATTRIBUTE = Pattern.compile("\\[@from=\"([^\"]++)\"]");
        private final Pattern WEEKDATA = Pattern.compile(
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
            } else if (matches(WEEKDATA, arg0, arg1, matchers)) {
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
            return CLDRFile.ldmlComparator.compare(arg0, arg1);
        }
    };

    /**
     * SupplementalMapper constructor.
     * @param inputDir the directory that the input files are in
     * @param cldrVersion the version of CLDR for output purposes. Only used
     * in supplementalData conversion.
     */
    public SupplementalMapper(String inputDir, String cldrVersion) {
        super("ldml2icu_supplemental.txt");
        this.inputDir = inputDir;
        this.cldrVersion = cldrVersion;
    }

    /**
     * Loads an IcuData object of the specified type.
     * @param outputName the type of data to be converted
     * @return an IcuData object
     */
    public IcuData fillFromCldr(String outputName) {
        Map<String,CldrArray> pathValueMap = new HashMap<String, CldrArray>();
        String category = outputName;
        if (outputName.equals("supplementalData")) {
            String[] categories = {"supplementalData", "telephoneCodeData", "languageInfo"};
            for (String cat : categories) {
                loadValues(cat, pathValueMap);
            }
        } else {
            if (outputName.equals("metadata")) category = "supplementalMetadata";
            loadValues(category, pathValueMap);
        }
        addFallbackValues(pathValueMap);
        IcuData icuData = new IcuData(category + ".xml", outputName, false, enumMap);
        for (String rbPath : pathValueMap.keySet()) {
            CldrArray values = pathValueMap.get(rbPath);
            icuData.addAll(rbPath, values.sortValues(supplementalComparator));
        }
        // Hack to add the CLDR version
        if (outputName.equals("supplementalData")) {
            icuData.add("/cldrVersion", cldrVersion);
        }
        return icuData;
    }

    /**
     * Loads values for the specified category from CLDR.
     * @param category
     * @param pathValueMap the output map
     */
    private void loadValues(String category, Map<String,CldrArray> pathValueMap) {
        String inputFile = category + ".xml";
        XMLSource source = new LinkedXMLSource();
        CLDRFile cldrFile = CLDRFile.loadFromFile(new File(inputDir, inputFile),
                category, DraftStatus.contributed, source);
        RegexLookup<RegexResult> pathConverter = getPathConverter();
        fifoCounter = 0; // Helps to keep unsorted rb paths in order.
        for (String xpath : cldrFile) {
            Output<Finder> matcher = new Output<Finder>();
            String fullPath = cldrFile.getFullXPath(xpath);
            RegexResult regexResult = pathConverter.get(fullPath, null, null, matcher, null);
            if (regexResult == null) continue;
            String[] arguments = matcher.value.getInfo();
            for (PathValueInfo info : regexResult) {
                List<String> values = info.processValues(arguments, cldrFile, xpath);
                // Check if there are any arguments that need splitting for the rbPath.
                String groupKey = info.processGroupKey(arguments);
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
                            processValues(fullPath, rbPath, values, groupKey, pathValueMap);
                        }
                        splitNeeded = true;
                    }
                }
                // No splitting required, process as per normal.
                if (!splitNeeded) {
                    String rbPath = info.processRbPath(arguments);
                    processValues(fullPath, rbPath, values, groupKey, pathValueMap);
                }
            }
            fifoCounter++;
        }
    }

    /**
     * Processes values to be added to the ICU data structure
     * @param xpath the CLDR path that the values came from
     * @param rbPath the rbPath that the values belong to
     * @param values the values
     * @param groupKey the key that the values should be grouped by
     * @param pathValueMap the output map
     */
    private void processValues(String xpath, String rbPath, List<String> values,
            String groupKey, Map<String,CldrArray> pathValueMap) {
        List<String> processedValues = new ArrayList<String>();
        // The fifo counter needs to be formatted with leading zeros for sorting.
        if (rbPath.contains("<FIFO>")) {
            rbPath = rbPath.replace("<FIFO>", '<' + numberFormat.format(fifoCounter) + '>');
        }
        if (NUMBERING_SYSTEMS_DESC.matcher(rbPath).matches()
                && xpath.contains("algorithmic")) {
            // Hack to insert % into numberingSystems descriptions.
            String value = values.get(0);
            int percentPos = value.lastIndexOf('/') + 1;
            value = value.substring(0, percentPos) + '%' + value.substring(percentPos);
            processedValues.add(value);
        } else if (isDatePath(rbPath)){
            String[] dateValues = getSeconds(values.get(0));
            processedValues.add(dateValues[0]);
            processedValues.add(dateValues[1]);
        } else {
            processedValues = values;
        }
        CldrArray cldrArray = getCldrArray(rbPath, pathValueMap);
        cldrArray.add(xpath, processedValues, groupKey);
    }

    /**
     * Checks if the given path should be treated as a date path.
     */
    private static boolean isDatePath(String rbPath) {
        return DATE_PATH.matcher(rbPath).matches();
    }

    /**
     * Converts a date string to a pair of millisecond values.
     * @param dateStr
     * @return
     */
    private String[] getSeconds(String dateStr) {
        long millis = getMilliSeconds(dateStr);
        if (millis == -1) {
            return null;
        }

        int top =(int)((millis & 0xFFFFFFFF00000000L)>>>32); // top
        int bottom = (int)((millis & 0x00000000FFFFFFFFL)); // bottom
        String[] result = { top + "", bottom + ""};

        if (NewLdml2IcuConverter.DEBUG) {
            long bot = 0xffffffffL & bottom;
            long full = ((long)(top) << 32);
            full += bot;
            if (full != millis) {
                System.err.println("Error when converting " + millis + ": " +
                    top + ", " + bottom + " was converted back into " + full);
            }
        }

        return result;
    }

    private long getMilliSeconds(String dateStr) {
        try {
            if (dateStr != null) {
                int count = countHyphens(dateStr);
                SimpleDateFormat format = new SimpleDateFormat();
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                if (count == 2) {
                    format.applyPattern("yyyy-mm-dd");
                } else if (count == 1) {
                    format.applyPattern("yyyy-mm");
                } else {
                    format.applyPattern("yyyy");
                }
                return format.parse(dateStr).getTime();
            }
        } catch(ParseException ex) {
            System.err.println("Could not parse date: " + dateStr);
        }
        return -1;
    }

    /**
     * Counts the number of hyphens in a string.
     * @param str
     * @return
     */
    private static int countHyphens(String str) {
        int lastPos = 0;
        int numHyphens = 0;
        while ((lastPos = str.indexOf('-', lastPos + 1))  > -1) {
            numHyphens++;
        }
        return numHyphens;
    }
    
    /**
     * Iterating through this XMLSource will return the xpaths in the order
     * that they were parsed from the XML file.
     */
    private class LinkedXMLSource extends XMLSource {
        private Map<String, String> xpath_value;
        private Map<String, String> xpath_fullXPath;
        private Comments comments;

        public LinkedXMLSource() {
            xpath_value = new LinkedHashMap<String, String>();
            xpath_fullXPath = new HashMap<String, String>();
            comments = new Comments();
        }

        @Override
        public Object freeze() {
            locked = true;
            return this;
        }
        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
            xpath_fullXPath.put(distinguishingXPath, fullxpath);
        }

        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            xpath_value.put(distinguishingXPath, value);
        }

        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            xpath_value.remove(distinguishingXPath);
        }

        @Override
        public String getValueAtDPath(String path) {
            return xpath_value.get(path);
        }
        @Override
        public String getFullPathAtDPath(String xpath) {
            String result = (String) xpath_fullXPath.get(xpath);
            if (result != null) return result;
            if (xpath_value.get(xpath) != null) return xpath; // we don't store duplicates
            return null;
        }
        @Override
        public Comments getXpathComments() {
            return comments;
        }
        @Override
        public void setXpathComments(Comments comments) {
            this.comments = comments;
        }

        @Override
        public Iterator<String> iterator() {
            return Collections.unmodifiableSet(xpath_value.keySet()).iterator();
        }

        @Override
        public void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result) {
            throw new UnsupportedOperationException();
        }
    }
}
