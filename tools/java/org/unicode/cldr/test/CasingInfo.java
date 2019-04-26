package org.unicode.cldr.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckConsistentCasing.CasingType;
import org.unicode.cldr.test.CheckConsistentCasing.CasingTypeAndErrFlag;
import org.unicode.cldr.test.CheckConsistentCasing.Category;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.UnicodeSet;

/**
 * Calculates, reads, writes and returns casing information about locales for
 * CheckConsistentCasing.
 * Run main() to generate the casing information files which will be stored in common/casing.
 *
 * @author jchye
 */
public class CasingInfo {
    private static final Options options = new Options(
        "This program is used to generate casing files for locales.")
            .add("locales", ".*", ".*", "A regex of the locales to generate casing information for")
            .add("summary", null,
                "generates a summary of the casing for all locales that had casing generated for this run");
    private Map<String, Map<Category, CasingTypeAndErrFlag>> casing;
    private List<File> casingDirs;

    public CasingInfo(Factory factory) {
        casingDirs = new ArrayList<File>();
        for (File f : factory.getSourceDirectories()) {
            this.casingDirs.add(new File(f.getAbsolutePath() + "/../casing"));
        }
        casing = CldrUtility.newConcurrentHashMap();
    }

    /**
     * ONLY usable in command line tests.
     */
    public CasingInfo() {
        casingDirs = new ArrayList<File>();
        this.casingDirs.add(new File(CLDRPaths.CASING_DIRECTORY));
        casing = CldrUtility.newConcurrentHashMap();
    }

    /**
     * Returns casing information to be used for a specified locale.
     *
     * @param localeID
     * @return
     */
    public Map<Category, CasingTypeAndErrFlag> getLocaleCasing(String localeID) {
        // Check if the localeID contains casing first.
        // If there isn't a casing file available for the locale,
        // recurse over the locale's parents until something is found.
        if (!casing.containsKey(localeID)) {
            // Synchronize writes to casing map in an attempt to avoid NPEs (cldrbug 5051).
            synchronized (casing) {
                CasingHandler handler = loadFromXml(localeID);
                if (handler != null) {
                    handler.addParsedResult(casing);
                }
                if (!casing.containsKey(localeID)) {
                    String parentID = LocaleIDParser.getSimpleParent(localeID);
                    if (!parentID.equals("root")) {
                        casing.put(localeID, getLocaleCasing(parentID));
                    }
                }
            }
        }

        return casing.get(localeID);
    }

    /**
     * Loads casing information about a specified locale from the casing XML,
     * if it exists.
     *
     * @param localeID
     */
    private CasingHandler loadFromXml(String localeID) {
        for (File casingDir : casingDirs) {
            File casingFile = new File(casingDir, localeID + ".xml");
            if (casingFile.isFile()) {
                CasingHandler handler = new CasingHandler();
                XMLFileReader xfr = new XMLFileReader().setHandler(handler);
                xfr.read(casingFile.toString(), -1, true);
                return handler;
            }
        } // Fail silently if file not found.
        return null;
    }

    /**
     * Calculates casing information about all languages from the locale data.
     */
    private Map<String, Boolean> generateCasingInformation(String localePattern) {
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = supplementalDataInfo.getDefaultContentLocales();
        String sourceDirectory = CldrUtility.checkValidDirectory(CLDRPaths.MAIN_DIRECTORY);
        Factory cldrFactory = Factory.make(sourceDirectory, localePattern);
        Set<String> locales = new LinkedHashSet<String>(cldrFactory.getAvailable());
        locales.removeAll(defaultContentLocales); // Skip all default content locales
        UnicodeSet allCaps = new UnicodeSet("[:Lu:]");
        Map<String, Boolean> localeUsesCasing = new HashMap<String, Boolean>();
        LocaleIDParser parser = new LocaleIDParser();

        for (String localeID : locales) {
            if (CLDRFile.isSupplementalName(localeID)) continue;

            // We want country/script differences but not region differences
            // (unless it's pt_PT, which we do want).
            // Keep regional locales only if there isn't already a locale for its script,
            // e.g. keep zh_Hans_HK because zh_Hans is a default locale.
            parser.set(localeID);
            if (parser.getRegion().length() > 0 && !localeID.equals("pt_PT")) {
                System.out.println("Skipping regional locale " + localeID);
                continue;
            }

            // Save casing information about the locale.
            CLDRFile file = cldrFactory.make(localeID, true);
            UnicodeSet examplars = file.getExemplarSet("", WinningChoice.NORMAL);
            localeUsesCasing.put(localeID, examplars.containsSome(allCaps));
            createCasingXml(localeID, CheckConsistentCasing.getSamples(file));
        }
        return localeUsesCasing;
    }

    /**
     * Creates a CSV summary of casing information over all locales for verification.
     *
     * @param outputFile
     */
    private void createCasingSummary(String outputFile, Map<String, Boolean> localeUsesCasing) {
        PrintWriter out;
        try {
            out = new PrintWriter(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Header
        out.print(",");
        for (Category category : Category.values()) {
            out.print("," + category.toString().replace('_', '-'));
        }
        out.println();
        out.print("Locale ID,Case");
        for (int i = 0; i < Category.values().length; i++) {
            out.print("," + i);
        }
        out.println();

        Set<String> locales = casing.keySet();
        for (String localeID : locales) {
            // Write casing information about the locale to file.
            out.print(localeID);
            out.print(",");
            out.print(localeUsesCasing.get(localeID) ? "Y" : "N");
            Map<Category, CasingTypeAndErrFlag> types = casing.get(localeID);
            for (Category category : Category.values()) {
                CasingTypeAndErrFlag value = types.get(category);
                out.print("," + value == null ? null : value.type().toString().charAt(0));
            }
            out.println();
            out.flush();
        }
        out.close();
    }

    /**
     * Writes casing information for the specified locale to XML format.
     */
    private void createCasingXml(String localeID, Map<Category, CasingType> localeCasing) {
        // Load any existing overrides over casing info.
        CasingHandler handler = loadFromXml(localeID);
        Map<Category, CasingType> overrides = handler == null ? new EnumMap<Category, CasingType>(Category.class) : handler.getOverrides();
        localeCasing.putAll(overrides);

        XMLSource source = new SimpleXMLSource(localeID);
        for (Category category : Category.values()) {
            if (category == Category.NOT_USED) continue;
            CasingType type = localeCasing.get(category);
            if (overrides.containsKey(category)) {
                String path = MessageFormat.format("//ldml/metadata/casingData/casingItem[@type=\"{0}\"][@override=\"true\"]", category);
                source.putValueAtPath(path, type.toString());
            } else if (type != CasingType.other) {
                String path = "//ldml/metadata/casingData/casingItem[@type=\"" + category + "\"]";
                source.putValueAtPath(path, type.toString());
            }
        }
        CLDRFile cldrFile = new CLDRFile(source);
        File casingFile = new File(CLDRPaths.GEN_DIRECTORY + "/casing", localeID + ".xml");

        try {
            PrintWriter out = new PrintWriter(casingFile);
            cldrFile.write(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates all the casing information and writes it to XML.
     * A CSV summary of casing information is written to file if a filename argument is provided.
     *
     * @param args
     */
    public static void main(String[] args) {
        CasingInfo casingInfo = new CasingInfo();
        options.parse(args, true);
        Map<String, Boolean> localeUsesCasing = casingInfo.generateCasingInformation(options.get("locales").getValue());
        if (options.get("summary").doesOccur()) {
            casingInfo.createCasingSummary(args[0], localeUsesCasing);
        }
    }

    /**
     * XML handler for parsing casing files.
     */
    private class CasingHandler extends XMLFileReader.SimpleHandler {
        private Pattern localePattern = PatternCache.get("//ldml/identity/language\\[@type=\"(\\w+)\"\\]");
        private String localeID;
        private Map<Category, CasingTypeAndErrFlag> caseMap = new EnumMap<Category, CasingTypeAndErrFlag>(Category.class);
        private Map<Category, CasingType> overrideMap = new EnumMap<Category, CasingType>(Category.class);

        @Override
        public void handlePathValue(String path, String value) {
            // Parse casing info.
            if (path.contains("casingItem")) {
                XPathParts parts = XPathParts.getTestInstance(path); // frozen should be OK
                Category category = Category.valueOf(parts.getAttributeValue(-1, "type").replace('-', '_'));
                CasingType casingType = CasingType.valueOf(value);
                boolean errFlag = Boolean.parseBoolean(parts.getAttributeValue(-1, "forceError"));
                for (CasingTypeAndErrFlag typeAndFlag : CasingTypeAndErrFlag.values()) {
                    if (casingType == typeAndFlag.type() && errFlag == typeAndFlag.flag()) {
                        caseMap.put(category, typeAndFlag);
                        break;
                    }
                }
                if (Boolean.valueOf(parts.getAttributeValue(-1, "override"))) {
                    overrideMap.put(category, casingType);
                }
            } else {
                // Parse the locale that the casing is for.
                Matcher matcher = localePattern.matcher(path);
                if (matcher.matches()) {
                    localeID = matcher.group(1);
                }
            }
        }

        public void addParsedResult(Map<String, Map<Category, CasingTypeAndErrFlag>> map) {
            map.put(localeID, caseMap);
        }

        public Map<Category, CasingType> getOverrides() {
            return overrideMap;
        }
    }
}
