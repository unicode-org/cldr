package org.unicode.cldr.draft;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.LdmlMapper.RegexResult;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CldrUtility.Output;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexLookup;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Row.R2;

public class SupplementalMapper extends LdmlMapper {
    private static final Map<String, String> enumMap = Builder.with(new HashMap<String, String>())
            .put("sun", "1").put("mon", "2").put("tues", "3").put("wed", "4")
            .put("thu", "5").put("fri", "6").put("sat", "7").get();

    private String inputDir;

    private static Comparator<CldrValue> supplementalComparator = new Comparator<CldrValue>() {
        private final Pattern FROM_ATTRIBUTE = Pattern.compile("\\[@from=\"([^\"]++)\"]");
        private final Pattern WEEKDATA = Pattern.compile(
                "//supplementalData/weekData/(minDays|firstDay|weekendStart|weekendEnd).*");

        @Override
        public int compare(CldrValue value0, CldrValue value1) {
            String arg0 = value0.getXpath();
            String arg1 = value1.getXpath();
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

    public SupplementalMapper(String inputDir) {
        super("ldml2icu_supplemental.txt");
        this.inputDir = inputDir;
    }

    public IcuData fillFromCldr(String outputName) {
        Map<String,List<CldrValue>> pathValueMap = new HashMap<String, List<CldrValue>>();
        String category = outputName;
        if (outputName.equals("supplementalData")) {
            // TODO: move /CurrencyMap and /CurrencyMeta to curr folder!
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
            List<CldrValue> values = pathValueMap.get(rbPath);
            if (values.size() > 0) {
                Collections.sort(values, supplementalComparator);
                List<String> sortedValues = new ArrayList<String>();
                for (CldrValue value : values) {
                    sortedValues.add(value.getValue());
                }
                String[] valueArray = new String[sortedValues.size()];
                sortedValues.toArray(valueArray);
                icuData.add(rbPath, valueArray);
            }
        }
        
        return icuData;
    }

    private void loadValues(String category, Map<String,List<CldrValue>> pathValueMap) {
        String inputFile = category + ".xml";
        CLDRFile cldrFile = CLDRFile.loadFromFile(new File(inputDir, inputFile), category,
                DraftStatus.unconfirmed);
        RegexLookup<RegexResult> pathConverter = getPathConverter();
        for (String xpath : cldrFile) {
            Output<Finder> matcher = new Output<Finder>();
            String fullPath = cldrFile.getFullXPath(xpath);
            RegexResult regexResult = pathConverter.get(fullPath, null, null, matcher, null);
            if (regexResult == null) continue;
            String[] arguments = matcher.value.getInfo();
            List<PathValuePair> pairs = regexResult.processResult(cldrFile, fullPath, arguments);
            for (PathValuePair pair : pairs) {
                String[] values = pair.values;
                String rbPath = pair.path;
                if (rbPath.matches("/numberingSystems/\\w++/desc")
                        && xpath.contains("algorithmic")) {
                    // Hack to insert % into numberingSystems descriptions.
                    String value = values[0];
                    int percentPos = value.lastIndexOf('/') + 1;
                    value = value.substring(0, percentPos) + '%' + value.substring(percentPos);
                    values[0] = value;
                }
                List<CldrValue> cldrValues = pathValueMap.get(rbPath);
                if (cldrValues == null) cldrValues = new ArrayList<CldrValue>();
                pathValueMap.put(rbPath, cldrValues);
                for (String value : values) {
                    cldrValues.add(new CldrValue(fullPath, value, pair.isArray));
                }
            }
        }
    }

    
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        SupplementalMapper mapper = new SupplementalMapper("/Users/jchye/tweaks/common/supplemental");
        IcuData icuData = mapper.fillFromCldr("supplementalData");
        IcuTextWriter.writeToFile(icuData, "/Users/jchye/tweaks/newspecial/misc");
    }
}
