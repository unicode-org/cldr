package org.unicode.cldr.tool;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.With;

public class DiffCldr {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

    // ADD OPTIONS LATER
    final static Options myOptions = new Options()
    .add("source", ".*", CLDRPaths.MAIN_DIRECTORY, "source directory")
//    .add("file", ".*", ".*", "regex to filter files/locales.")
//    .add("path", ".*", null, "regex to filter paths. ! in front selects items that don't match. example: -p relative.*@type=\\\"-?3\\\"")
//    .add("value", ".*", null, "regex to filter values. ! in front selects items that don't match")
//    .add("level", ".*", null, "regex to filter levels. ! in front selects items that don't match")
//    .add("count", null, null, "only count items")
//    .add("organization", ".*", null, "show level for organization")
//    .add("z-showPath", null, null, "show paths")
//    .add("resolved", null, null, "use resolved locales")
//    .add("q-showParent", null, null, "show parent value")
//    .add("english", null, null, "show english value")
//    .add("Verbose", null, null, "verbose output")
//    .add("PathHeader", null, null, "show path header and string ID")
    ;

//    private static String fileMatcher;
//    private static Matcher pathMatcher;
//    private static boolean countOnly;
//    private static boolean showPath;
    private static PathHeader.Factory PATH_HEADER_FACTORY = null;

    private static String organization;

    public static void main(String[] args) {
        myOptions.parse(args, true);
        String dirBase = CLDRPaths.COMMON_DIRECTORY;
        PathHeader.Factory phf = PathHeader.getFactory(CONFIG.getEnglish());
        String localeBase = "en";

        // load data
        
        M3<PathHeader, String, String> data = ChainedMap.of(new TreeMap<PathHeader,Object>(), new TreeMap<String,Object>(), String.class);
        Counter<String> locales = new Counter<>();
        for (String dir :DtdType.ldml.directories) {
            Factory factory = SimpleFactory.make(dirBase + dir, localeBase + "(_.*)?");
            for (String locale : factory.getAvailable()) {
                CLDRFile cldrFile = factory.make(locale, false);
                for (String path : With.in(cldrFile.iterator())) {
                    PathHeader ph = phf.fromPath(path);
                    String value = cldrFile.getStringValue(path);
                    data.put(ph, locale, value);
                    locales.add(locale, 1); // count of items in locale
                }
            }
        }
        Set<String> localeList = locales.getKeysetSortedByCount(false);

        // now print differences
        Set<String> currentValues = new TreeSet<>();
        System.out.print("Section\tPage\tHeader\tCode");
        for (String locale : localeList) {
            System.out.print("\t" + locale);
        }
        System.out.println();

        for (PathHeader ph : data.keySet()) {
            String firstValue = null;
            currentValues.clear();
            final Map<String, String> localeToValue = data.get(ph);
            currentValues.addAll(localeToValue.values());
            if (currentValues.size() <= 1) {
                continue;            
            }
            
            // have difference, so print
            
            System.out.print(ph);
            for (String locale : localeList) {
                System.out.print("\t" + CldrUtility.ifNull(localeToValue.get(locale),""));
            }
            System.out.println();

        
        }
    }
}
