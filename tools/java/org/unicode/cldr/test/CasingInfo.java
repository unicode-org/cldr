package org.unicode.cldr.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckConsistentCasing.CasingType;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
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
        .add("locales", ".*", "A regex of the locales to generate casing information for")
        .add("summary", null,
            "generates a summary of the casing for all locales that had casing generated for this run");

    private Map<String, Map<String, CasingType>> casing;
    private Map<String, Boolean> localeUsesCasing;
    private File casingDir;

    public CasingInfo(String dir) {
        this.casingDir = new File(dir);
        casing = CldrUtility.newConcurrentHashMap();
        localeUsesCasing = CldrUtility.newConcurrentHashMap();
    }

    /**
     * ONLY usable in command line tests.
     */
    public CasingInfo() {
        this(CldrUtility.COMMON_DIRECTORY + "/casing");
    }

    /**
     * Returns casing information to be used for a specified locale.
     * 
     * @param localeID
     * @return
     */
    public Map<String, CasingType> getLocaleCasing(String localeID) {
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
        File casingFile = new File(casingDir, localeID + ".xml");
        if (casingFile.isFile()) {
            CasingHandler handler = new CasingHandler();
            XMLFileReader xfr = new XMLFileReader().setHandler(handler);
            xfr.read(casingFile.toString(), -1, true);
            return handler;
        } // Fail silently if file not found.
        return null;
    }

    /**
     * Calculates casing information about all languages from the locale data.
     */
    private void generateCasingInformation(String localePattern) {
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = supplementalDataInfo.getDefaultContentLocales();
        String sourceDirectory = CldrUtility.checkValidDirectory(CldrUtility.MAIN_DIRECTORY);
        Factory cldrFactory = Factory.make(sourceDirectory, localePattern);
        Set<String> locales = new LinkedHashSet<String>(cldrFactory.getAvailable());
        locales.removeAll(defaultContentLocales); // Skip all default content locales
        UnicodeSet allCaps = new UnicodeSet("[:Lu:]");

        for (String localeID : locales) {
            if (CLDRFile.isSupplementalName(localeID)) continue;

            // We want country/script differences but not region differences
            // (unless it's pt_PT, which we do want).
            // Keep regional locales only if there isn't already a locale for its script,
            // e.g. keep zh_Hans_HK because zh_Hans is a default locale.
            int underscorePos = localeID.indexOf('_');
            if (underscorePos > 0) {
                int underscorePos2 = localeID.indexOf('_', underscorePos + 1);
                if (localeID.length() - underscorePos == 3 && !localeID.equals("pt_PT") ||
                    underscorePos2 > 0 && locales.contains(localeID.substring(0, underscorePos2))) {
                    System.out.println("Skipping regional locale " + localeID);
                    continue;
                }
            }

            // Save casing information about the locale.
            CLDRFile file = cldrFactory.make(localeID, true);
            UnicodeSet examplars = file.getExemplarSet("", WinningChoice.NORMAL);
            localeUsesCasing.put(localeID, examplars.containsSome(allCaps));
            casing.put(localeID, CheckConsistentCasing.getSamples(file));
        }
    }

    /**
     * Creates a CSV summary of casing information over all locales for verification.
     * 
     * @param outputFile
     */
    private void createCasingSummary(String outputFile) {
        PrintWriter out;
        try {
            out = new PrintWriter(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Header
        out.print(",");
        String[] typeNames = CheckConsistentCasing.typeNames;
        for (int i = 0; i < typeNames.length; i++) {
            out.print("," + typeNames[i]);
        }
        out.println();
        out.print("Locale ID,Case");
        for (int i = 0; i < CheckConsistentCasing.LIMIT_COUNT; i++) {
            out.print("," + i);
        }
        out.println();

        Set<String> locales = casing.keySet();
        for (String localeID : locales) {
            // Write casing information about the locale to file.
            out.print(localeID);
            out.print(",");
            out.print(localeUsesCasing.get(localeID) ? "Y" : "N");
            Map<String, CasingType> types = casing.get(localeID);
            for (int i = 0; i < typeNames.length; i++) {
                CasingType value = types.get(typeNames[i]);
                out.print("," + value == null ? null : value.toString().charAt(0));
            }
            out.println();
            out.flush();
        }
        out.close();
    }

    /**
     * Writes all casing information in memory to files in XML format.
     */
    private void createCasingXml() {
        File outputDir = casingDir;
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        Set<String> locales = casing.keySet();
        String[] typeNames = CheckConsistentCasing.typeNames;
        for (String localeID : locales) {
            Map<String, CasingType> localeCasing = casing.get(localeID);
            // Load any existing overrides over casing info.
            CasingHandler handler = loadFromXml(localeID);
            Map<String, CasingType> overrides = handler == null ?
                    new HashMap<String, CasingType>() : handler.getOverrides();
            localeCasing.putAll(overrides);

            XMLSource source = new SimpleXMLSource(localeID);
            for (int i = 0; i < typeNames.length; i++) {
                String typeName = typeNames[i];
                if (typeName.equals(CheckConsistentCasing.NOT_USED)) continue;
                CasingType type = localeCasing.get(typeName);
                if (overrides.containsKey(typeName)) {
                    String path = MessageFormat.format("//ldml/metadata/casingData/casingItem[@type=\"{0}\"][@override=\"true\"]", typeName);
                    source.putValueAtPath(path, type.toString());
                } else if (type != CasingType.other) {
                    String path = "//ldml/metadata/casingData/casingItem[@type=\"" + typeName + "\"]";
                    source.putValueAtPath(path, type.toString());
                }
            }
            CLDRFile cldrFile = new CLDRFile(source);
            File casingFile = new File(casingDir, localeID + ".xml");

            try {
                PrintWriter out = new PrintWriter(casingFile);
                cldrFile.write(out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        casingInfo.generateCasingInformation(options.get("locales").getValue());
        if (options.get("summary").doesOccur()) {
            casingInfo.createCasingSummary(args[0]);
        }
        // Create all casing files needed for CLDR checks.
        casingInfo.createCasingXml();
    }

    /**
     * XML handler for parsing casing files.
     */
    private class CasingHandler extends XMLFileReader.SimpleHandler {
        private Pattern localePattern = Pattern.compile("//ldml/identity/language\\[@type=\"(\\w+)\"\\]");
        private String localeID;
        private Map<String, CasingType> caseMap = new HashMap<String, CasingType>();
        private Map<String, CasingType> overrideMap = new HashMap<String, CasingType>();

        @Override
        public void handlePathValue(String path, String value) {
            // Parse casing info.
            if (path.contains("casingItem")) {
                XPathParts parts = new XPathParts().set(path);
                String type = parts.getAttributeValue(-1, "type");
                CasingType casingType = CasingType.valueOf(value);
                caseMap.put(type, casingType);
                if (Boolean.valueOf(parts.getAttributeValue(-1, "override"))) {
                    overrideMap.put(type, casingType);
                }
            } else {
                // Parse the locale that the casing is for.
                Matcher matcher = localePattern.matcher(path);
                if (matcher.matches()) {
                    localeID = matcher.group(1);
                }
            }
        }

        public void addParsedResult(Map<String, Map<String, CasingType>> map) {
            map.put(localeID, caseMap);
        }

        public Map<String, CasingType> getOverrides() {
            return overrideMap;
        }
    }
}
