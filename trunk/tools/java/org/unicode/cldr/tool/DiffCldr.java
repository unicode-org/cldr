package org.unicode.cldr.tool;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
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
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.util.Output;

public class DiffCldr {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();

    // ADD OPTIONS LATER

    enum MyOptions {
        //organization(".*", "CLDR", "organization"),
        filter(".*", "en_001", "locale ancestor"),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = new Option(this, argumentPattern, defaultArgument, helpText);
        }

        static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) {
        MyOptions.parse(args, true);
        String localeBase = MyOptions.filter.option.getValue();

        String dirBase = CLDRPaths.COMMON_DIRECTORY;
        PathHeader.Factory phf = PathHeader.getFactory(CONFIG.getEnglish());

        // load data

        M3<PathHeader, String, String> data = ChainedMap.of(new TreeMap<PathHeader, Object>(), new TreeMap<String, Object>(), String.class);
        Counter<String> localeCounter = new Counter<>();
        Counter<PathHeader> pathHeaderCounter = new Counter<>();
        int total = 0;
        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();
//        Output<String> reformattedValue = new Output<String>();
//        Output<Boolean> hasReformattedValue = new Output<Boolean>();
        Multimap<String, String> extras = TreeMultimap.create();

        for (String dir : DtdType.ldml.directories) {
            Factory factory = SimpleFactory.make(dirBase + dir, ".*");
            Set<String> available = factory.getAvailable();
            Set<String> locales = new LinkedHashSet<>();
            if (!available.contains(localeBase)) {
                continue;
            }
            locales.add(localeBase);
            for (String locale : available) {
                if (hasAncestor(locale, localeBase)) {
                    locales.add(locale);
                }
            }
            for (String locale : locales) {
                if (locale.equals("en_WS")) {
                    int debug = 0;
                }
                boolean isBase = locale.equals(localeBase);
                CLDRFile cldrFile = factory.make(locale, isBase);
                DtdData dtdData = cldrFile.getDtdData();
                CLDRFile cldrFileResolved = factory.make(locale, true);
                for (String distinguishedPath : With.in(cldrFile.iterator())) {
                    String path = cldrFile.getFullXPath(distinguishedPath);

                    XPathParts pathPlain = XPathParts.getFrozenInstance(path);
                    if (dtdData.isMetadata(pathPlain)) {
                        continue;
                    }
                    if (pathPlain.getElement(1).equals("identity")) {
                        continue;
                    }
                    String value = cldrFile.getStringValue(distinguishedPath);
                    String bailey = cldrFileResolved.getBaileyValue(distinguishedPath, pathWhereFound, localeWhereFound);

                    // one of the attributes might be a value (ugg)
                    // so check for that, and extract the value

                    Set<String> pathForValues = dtdData.getRegularizedPaths(pathPlain, extras);
                    if (pathForValues != null && (isBase || !value.equals(bailey))) {
                        for (String pathForValue : pathForValues) {
                            PathHeader ph = phf.fromPath(pathForValue);
                            Splitter splitter = DtdData.getValueSplitter(pathPlain);
                            String cleanedValue = joinValues(pathPlain, splitter.splitToList(value));
                            total = addValue(data, locale, ph, cleanedValue, total, localeCounter, pathHeaderCounter);
                        }
                    }

                    // there are value attributes, so do them

                    for (Entry<String, Collection<String>> entry : extras.asMap().entrySet()) {
                        final String extraPath = entry.getKey();
                        final PathHeader ph = phf.fromPath(extraPath);
                        final Collection<String> extraValues = entry.getValue();
                        String cleanedValue = joinValues(pathPlain, extraValues);
                        total = addValue(data, locale, ph, cleanedValue, total, localeCounter, pathHeaderCounter);
                    }
                    if (pathForValues == null && !value.isEmpty()) {
                        System.err.println("Shouldn't happen");
                    }
                }
            }
        }
        Set<String> localeList = localeCounter.getKeysetSortedByCount(false);

        // now print differences
        Set<String> currentValues = new TreeSet<>();
        System.out.print("№\tSection\tPage\tHeader\tCode\tCount");
        for (String locale : localeList) {
            System.out.print("\t" + locale);
        }
        System.out.println();
        System.out.print("\t\t\t\tCount\t" + total);
        for (String locale : localeList) {
            System.out.print("\t" + localeCounter.get(locale));
        }
        System.out.println();

        int sort = 0;
        for (PathHeader ph : data.keySet()) {
            String firstValue = null;
            currentValues.clear();
            final Map<String, String> localeToValue = data.get(ph);
            currentValues.addAll(localeToValue.values());
            if (currentValues.size() <= 1) {
                continue;
            }

            // have difference, so print

            System.out.print(++sort + "\t" + ph + "\t" + pathHeaderCounter.get(ph));
            for (String locale : localeList) {
                System.out.print("\t" + CldrUtility.ifNull(localeToValue.get(locale), ""));
            }
            System.out.println();
        }
    }

    private static boolean hasAncestor(String locale, String localeBase) {
        while (true) {
            if (locale == null) {
                return false;
            } else if (locale.equals(localeBase)) {
                return true;
            }
            locale = LocaleIDParser.getParent(locale);
        }
    }

    /**
     * Add <ph,value) line, recording extra info.
     */
    private static int addValue(M3<PathHeader, String, String> data, String locale, PathHeader ph, String value,
        int total, Counter<String> localeCounter, Counter<PathHeader> pathHeaderCounter) {
        if (value.isEmpty()) {
            return 0;
        }
        String old = data.get(ph, locale);
        if (old != null) {
            return 0; // suppress duplicates
        }
        data.put(ph, locale, value);
        // add to counts
        ++total;
        localeCounter.add(locale, 1); // count of items in locale
        pathHeaderCounter.add(ph, 1); // count of items with same pathHeader, across locales
        return total;
    }

    /**
     * Fix values that are multiple lines or multiple items
     */
    private static String joinValues(XPathParts pathPlain, Collection<String> values) {
        Set<String> cleanedValues = new LinkedHashSet<>();
        for (String item : values) {
            if (!DtdData.isComment(pathPlain, item)) {
                cleanedValues.add(item);
            }
        }
        return CollectionUtilities.join(DtdData.CR_SPLITTER.split(CollectionUtilities.join(values, " ␍ ")), " ␍ ");
    }
}
