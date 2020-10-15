package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.AttributeStatus;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.TreeMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;

/** Simple tool to help verify that CLDR data is getting transferred.
 *
 * @author markdavis
 *
 */
public class VerifyConverterResults {
    public static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    public static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    public static final PathStarrer PATH_STARRER = new PathStarrer().setSubstitutionPattern("*");

    enum SourceType {
        text,
        json,
        rb}

    public static void main(String[] args) {
        // TODO, make these arguments
        SourceType sourceType = SourceType.json;

        String dirBase = CLDRPaths.STAGING_DIRECTORY + "production/";
        String textSource = "/Users/markdavis/GitHub/macchiati/icu/icu4c/source/data";
        String jsonSource = "/Users/markdavis/Downloads/38JsonBetaAll";
        String locale = "de";
        boolean isVerbose = false;


        String source;
        Matcher fileMatcher;
        Matcher parentMatcher = null;
        switch(sourceType) {
        case text:
            source = textSource;
            fileMatcher = Pattern.compile(locale + ".txt").matcher("");
            break;
        case json:
            source = jsonSource;
            fileMatcher = Pattern.compile(".*\\.json").matcher("");
            parentMatcher = Pattern.compile(locale + "|cldr-core|supplemental").matcher("");
            break;
        default: throw new IllegalArgumentException("No code yet for " + sourceType);
        }

        Set<String> skipSupplementalFiles = ImmutableSet.of(
            // purposely excluded from clients
            "subdivisions.xml",

            // internal to CLDR, not applicable for clients
            "attributeValueValidity.xml", "coverageLevels.xml"

            // the format changes so dramatically we can't compare

            );

        Set<String> converted = getConvertedData(sourceType, new File(source), fileMatcher, parentMatcher, new TreeSet<>());
        Set<String> excludeDraftStatus = ImmutableSet.of("unconfirmed", "provisional");

        // Now check that the data values in CLDR are contained
        for (String dir : Iterables.concat(DtdType.ldml.directories, DtdType.supplementalData.directories)) {
            switch(dir) {

            case "annotationsDerived": case "annotations":
                if (sourceType != SourceType.text) {
                    break;
                }
                System.out.println(dir + "\t##SKIPPING\t" + "excluded from ICU");
                continue;

            case "casing":
                System.out.println(dir + "\t##SKIPPING\t" + "internal to CLDR, not applicable for conversion");
                continue;

            case "subdivisions":
                System.out.println(dir + "\t##SKIPPING\t" + "purposely excluded from conversion");
                continue;

            case "collation":
            case "rbnf":
            case "transforms":
            case "segments":
            case "validity":
                System.out.println(dir + "\t##SKIPPING\t" + "format changes so dramatically we can't compare yet");
                continue;
            }

            final boolean isSupplemental = DtdType.supplementalData.directories.contains(dir);
            DtdData supplementalDtdData = isSupplemental ? DtdData.getInstance(DtdType.supplementalData) : null;

            Matcher cldrFileMatcher = Pattern.compile(locale + ".xml").matcher("");

            FileData filedata = new FileData(converted);
            String current = dirBase + "common/" + dir;

            for (File child : new File(current).listFiles()) {
                final String name = child.getName();
                if (isSupplemental) {
                    if (skipSupplementalFiles.contains(name)) {
                        continue;
                    }
                } else {
                    if (!cldrFileMatcher.reset(name).matches()) {
                        continue;
                    }
                }
                filedata.clear();
                for (Pair<String, String> line : XMLFileReader.loadPathValues(child.toString(), new ArrayList<>(), false)) {
                    final String value = line.getSecond();
                    final String path = line.getFirst();
                    XPathParts parts = XPathParts.getFrozenInstance(path);
                    String draftStatus = parts.getAttributeValue(-1, "draft");
                    if (draftStatus != null && excludeDraftStatus.contains(draftStatus)) {
                        // doesn't need to be copied; up to client
                        continue;
                    } else if (path.startsWith("//supplementalData/metadata/suppress/attributes")
                        || path.startsWith("//supplementalData/metadata/serialElements")) {
                        // internal to CLDR
                        continue;
                    }
                    filedata.checkValue(dir, name, path, value);

                    // for supplemental data, also check the value attributes
                    if (isSupplemental) {
                        for (int elementIndex = 0; elementIndex < parts.size(); ++elementIndex) {
                            String element = parts.getElement(elementIndex);
                            for (Entry<String, String> attribute : parts.getAttributes(elementIndex).entrySet()) {
                                if (AttributeStatus.value == supplementalDtdData.getAttributeStatus(element, attribute.getKey())) {
                                    filedata.checkValue(dir, name, path, attribute.getValue());
                                }
                            }
                        }
                    }
                }
                filedata.print(isVerbose);
                System.out.println(dir + "\t##Missing Paths #:\t" + (filedata.filedata.size() == 0? "NONE"
                    : filedata.filedata.size()));
            }
        }
    }

    static class FileData {
        Set<String> converted;
        TreeMultimap<String, String> filedata =  TreeMultimap.create();
        TreeMap<String, String> starredData = new TreeMap<>();

        public FileData(Set<String> converted) {
            this.converted = converted;
        }

        public void clear() {
            filedata.clear();
            starredData.clear();
        }

        public void checkValue(final String dir, final String name, final String path, String value) {
            if (value.isEmpty()) {
                return;
            }
            value = value.replace('\n', ' ');
            if (converted.contains(value)) {
                return;
            }
            filedata.put(dir + "\t" + name + "\t" + path, value);
            starredData.put(dir + "\t" + name + "\t" + PATH_STARRER.set(path), value);
        }

        void print(boolean isVerbose) {
            final Set<Entry<String, String>> items = isVerbose ? filedata.entries() : starredData.entrySet();
            for (Entry<String, String> entry : items) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
        }
    }

    private static Set<String> getConvertedData(SourceType sourceType, File target, Matcher fileMatcher, Matcher parentMatcher, Set<String> accummulatedValues) {
        if (target.isDirectory()) {
            for (File child : target.listFiles()) {
                getConvertedData(sourceType, child, fileMatcher, parentMatcher, accummulatedValues);
            }
        } else {
            if (target.toString().contains("/de/")) {
                int debug = 0;
            }
            boolean ok = true;
            if (parentMatcher != null) {
                String parentName = target.getParentFile().getName();
                ok = parentMatcher.reset(parentName).matches();
            }
            if (ok) {
                ok = fileMatcher.reset(target.getName()).matches();
            }
            if (ok) { // not directory, matches
                int startCount = accummulatedValues.size();
                switch (sourceType) {
                case text:
                    processText(target, accummulatedValues);
                    break;
                case json:
                    processJson(target, accummulatedValues);
                    break;
                default:
                    throw new IllegalArgumentException("No code yet for " + sourceType);
                }
                int endCount = accummulatedValues.size();
                System.out.println("Processed Converted" + target + "; "
                    + (startCount == endCount ? "NO CHANGE" : startCount + " => " + endCount));
            }
        }
        return accummulatedValues;
    }


    private static void processJson(File target, Set<String> accummulatedValues) {
        try (Reader reader = FileUtilities.openFile(target, Charset.forName("utf8"))) {
            JsonStreamParser gsonParser = new JsonStreamParser(reader);
            gsonParser.forEachRemaining((JsonElement x) -> process(x, accummulatedValues));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void process(JsonElement x, Set<String> accummulatedValues) {
        if (x.isJsonPrimitive()) {
            accummulatedValues.add(x.getAsString());
        } else if (x.isJsonArray()) {
            for (JsonElement y : x.getAsJsonArray()) {
                process(y, accummulatedValues);
            }
        } else if (x.isJsonObject()) {
            for (Entry<String, JsonElement> y : x.getAsJsonObject().entrySet()) {
                process(y.getValue(), accummulatedValues);
            }
        } else {
            throw new IllegalArgumentException("No code yet for ");
        }
    }

    public static void processText(File target, Set<String> accummulatedValues) {
        Matcher quoteMatcher = Pattern.compile("\"([^\"]*)\"").matcher("");
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
