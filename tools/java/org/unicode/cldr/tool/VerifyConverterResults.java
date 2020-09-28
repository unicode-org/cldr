package org.unicode.cldr.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;

import com.google.common.collect.TreeMultimap;

/** Simple tool to help verify that CLDR data is getting transferred.
 *
 * @author markdavis
 *
 */
public class VerifyConverterResults {
    public static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    public static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();

    public static void main(String[] args) {
        // TODO, make these arguments
        String dirBase = CLDRPaths.STAGING_DIRECTORY + "production/";
        String target = "/Users/markdavis/GitHub/macchiati/icu/icu4c/source/data";
        Matcher m = Pattern.compile("de.xml").matcher("");
        Matcher mtxt = Pattern.compile("de.txt").matcher("");

        Set<String> converted = getConvertedData(new File(target), mtxt, new TreeSet<>());

        for (String dir : DtdType.ldml.directories) {
            switch(dir) {
            case "annotationsDerived": case "annotations":
            case "casing": case "collation":
            case "rbnf":
            case "segments": case "subdivisions":
                System.out.println("##Skipping:\t" + dir);
                continue;
            }
            String current = dirBase + "common/" + dir;
            TreeMultimap<String, String> filedata = TreeMultimap.create();
            for (File child : new File(current).listFiles()) {
                final String name = child.getName();
                if (!m.reset(name).matches()) {
                    continue;
                }
                filedata.clear();
                for (Pair<String, String> line : XMLFileReader.loadPathValues(child.toString(), new ArrayList<>(), false)) {
                    final String value = line.getSecond();
                    if (value.isEmpty()) {
                        continue;
                    }
                    if (converted.contains(value)) {
                        continue;
                    }
                    final String path = line.getFirst();
                    if (path.contains("[@draft=\"unconfirmed\"]")) {
                        continue;
                    }
                    String fullPath = dir + "\t" + name + "\t" + path;
                    filedata.put(fullPath, value);
                }
                for (Entry<String, String> entry : filedata.entries()) {
                    System.out.println(entry.getValue() + "\t" + entry.getKey());
                }
            }
        }
    }

    private static Set<String> getConvertedData(File target, Matcher m, Set<String> accummulatedValues) {
        Matcher quoteMatcher = Pattern.compile("\"([^\"]*)\"").matcher("");
        if (target.isDirectory()) {
            for (File child : target.listFiles()) {
                getConvertedData(child, m, accummulatedValues);
            }
        } else if (!m.reset(target.getName()).matches()) {
            int debug = 0;
        } else {
            for (String line : FileUtilities.in(target)) {
                if (line.startsWith("//")) {
                    continue;
                }
                quoteMatcher.reset(line);
                while (quoteMatcher.find()) {
                    final String value = quoteMatcher.group(1);
                    accummulatedValues.add(value);
                }
            }
        }
        return accummulatedValues;
    }
    /*
    public static void main(String[] args) throws UnsupportedEncodingException {
        for (final String baseName : getBaseNames()) {
            gatherData(baseName);
        }
    }

    // Ugly hack to get base names
    static Collection<String> getBaseNames() {
        return new LinkedHashSet<String>(Arrays.asList(new String[] {
                ICUData.ICU_BASE_NAME,
                ICUData.ICU_BRKITR_BASE_NAME,
                ICUData.ICU_COLLATION_BASE_NAME,
                ICUData.ICU_RBNF_BASE_NAME,
                ICUData.ICU_TRANSLIT_BASE_NAME
        }));
    }


    private static void gatherData(String baseName) {
        ULocale[] availableULocales;
        try {
            availableULocales = ICUResourceBundle.getAvailableULocales(baseName, ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        } catch (final Exception e) {
            e.printStackTrace();
            System.out.println("*** Unable to load " + baseName);
            return;
        }
        System.out.println("Gathering data for: " + baseName);
        for (final ULocale locale : availableULocales) {
            final UResourceBundle rs = UResourceBundle.getBundleInstance(baseName, locale);
            addStrings(rs);
        }
    }

    private static void addStrings(UResourceBundle rs) {
        final String key = rs.getKey();
        if (key != null) {
            keyCounter.add(key, 1);
        }
        switch (rs.getType()) {
        case UResourceBundle.STRING:
            counter.add(rs.getString(), 1);
            break;
        case UResourceBundle.ARRAY:
        case UResourceBundle.TABLE:
            for (int i = 0; i < rs.getSize(); ++i) {
                final UResourceBundle rs2 = rs.get(i);
                addStrings(rs2);
            }
            break;
        case UResourceBundle.BINARY:
        case UResourceBundle.INT:
        case UResourceBundle.INT_VECTOR: // skip
            break;
        default:
            throw new IllegalArgumentException("Unknown Option: " + rs.getType());
        }
    }

     */
}
