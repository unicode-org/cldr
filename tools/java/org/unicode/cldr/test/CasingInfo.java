package org.unicode.cldr.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckConsistentCasing.CasingType;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.text.UnicodeSet;

/**
 * Calculates, reads, writes and returns casing information about locales for
 * CheckConsistentCasing.
 * Run main() to generate the casing information files which will be stored in common/casing.
 * 
 * @author jchye
 */
public class CasingInfo {
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
        // If there isn a casing file available for the locale,
        // recurse over the locale's parents until something is found.
        if (!casing.containsKey(localeID)) {
            loadFromXml(localeID);
            if (!casing.containsKey(localeID)) {
                String parentID = LocaleIDParser.getSimpleParent(localeID);
                if (!parentID.equals("root")) {
                    casing.put(localeID, getLocaleCasing(parentID));
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
    private void loadFromXml(String localeID) {
        File casingFile = new File(casingDir, localeID + ".xml");
        if (casingFile.isFile()) {
            CasingHandler handler = new CasingHandler();
            XMLFileReader xfr = new XMLFileReader().setHandler(handler);
            xfr.read(casingFile.toString(), -1, true);
            handler.addParsedResult(casing);
        } // Fail silently if file not found.
    }

    /**
     * Calculates casing information about all languages from the locale data.
     */
    private void generateCasingInformation() {
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance();
        Set<String> defaultContentLocales = supplementalDataInfo.getDefaultContentLocales();
        String sourceDirectory = CldrUtility.checkValidDirectory(CldrUtility.MAIN_DIRECTORY);
        Factory cldrFactory = Factory.make(sourceDirectory, ".*");
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
            CasingSource source = new CasingSource(localeID);
            for (int i = 0; i < typeNames.length; i++) {
                String typeName = typeNames[i];
                if (typeName.equals(CheckConsistentCasing.NOT_USED)) continue;
                CasingType type = localeCasing.get(typeName);
                if (type != CasingType.other) {
                    source.putValueAtDPath("//ldml/metadata/casingData/casingItem[@type=\"" + typeName + "\"]",
                        type.toString());
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
        casingInfo.generateCasingInformation();
        if (args.length > 0) {
            casingInfo.createCasingSummary(args[0]);
        }
        // Create all casing files needed for CLDR checks.
        casingInfo.createCasingXml();
    }

    /**
     * XML handler for parsing casing files.
     */
    private class CasingHandler extends XMLFileReader.SimpleHandler {
        private Pattern casingPattern = Pattern
            .compile("//ldml/metadata/casingData/casingItem\\[@type=\"([/\\-\\w]+)\"\\]");
        private Pattern localePattern = Pattern.compile("//ldml/identity/language\\[@type=\"(\\w+)\"\\]");
        private String localeID;
        private Map<String, CasingType> caseMap;

        public CasingHandler() {
            caseMap = new HashMap<String, CasingType>();
        }

        @Override
        public void handlePathValue(String path, String value) {
            Matcher matcher = casingPattern.matcher(path);
            // Parse casing info.
            if (matcher.matches()) {
                caseMap.put(matcher.group(1), CasingType.valueOf(value));
            } else {
                // Parse the locale that the casing is for.
                matcher = localePattern.matcher(path);
                if (matcher.matches()) {
                    localeID = matcher.group(1);
                }
            }
        }

        public void addParsedResult(Map<String, Map<String, CasingType>> map) {
            map.put(localeID, caseMap);
        }
    }

    /**
     * Wrapper XMLSource for storing casing information to be written to disk.
     */
    private class CasingSource extends XMLSource {
        Map<String, String> pathMap;
        Comments comments;

        public CasingSource(String localeID) {
            super.setLocaleID(localeID);
            pathMap = new HashMap<String, String>();
            comments = new Comments();
        }

        @Override
        public Object freeze() {
            return null;
        }

        @Override
        public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
        }

        @Override
        public void putValueAtDPath(String distinguishingXPath, String value) {
            pathMap.put(distinguishingXPath, value);
        }

        @Override
        public void removeValueAtDPath(String distinguishingXPath) {
            pathMap.remove(distinguishingXPath);
        }

        @Override
        public String getValueAtDPath(String path) {
            return pathMap.get(path);
        }

        @Override
        public String getFullPathAtDPath(String path) {
            return path;
        }

        @Override
        public Comments getXpathComments() {
            return comments;
        }

        @Override
        public void setXpathComments(Comments comments) {
        }

        @Override
        public Iterator<String> iterator() {
            return pathMap.keySet().iterator();
        }

        @Override
        public void getPathsWithValue(String valueToMatch, String pathPrefix, Set<String> result) {
            // do nothing
        }
    }
}
