package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.util.ULocale;

public class FilterFactory extends Factory {
    private static final Pattern SPECIAL_ALT_PATHS = Pattern.compile(
        "//ldml/localeDisplayNames/territories/territory\\[@type=\"(?:HK|MO|MK)\"](\\[@alt=\"(short|variant)\"])?");

    private Factory rawFactory;
    private String organization;
    private SupplementalDataInfo supplementalData;

    /**
     * Creates a new Factory for filtering CLDRFiles.
     * @param rawFactory the factory to be filtered
     * @param organization the organization that the filtering is catered towards
     */
    public FilterFactory(Factory rawFactory, String organization) {
        this.rawFactory = rawFactory;
        this.organization = organization;
        supplementalData = SupplementalDataInfo.getInstance();
        setSupplementalDirectory(rawFactory.getSupplementalDirectory());
    }

    @Override
    public File[] getSourceDirectories() {
        return rawFactory.getSourceDirectories();
    }

    @Override
    public File getSourceDirectoryForLocale(String localeID) {
        return rawFactory.getSourceDirectoryForLocale(localeID);
    }

    @Override
    protected CLDRFile handleMake(String localeID, boolean resolved, DraftStatus minimalDraftStatus) {
        if (resolved) {
            return new CLDRFile(makeResolvingSource(localeID, minimalDraftStatus));
        } else {
            return new CLDRFile(filterCldrFile(localeID, minimalDraftStatus));
        }
    }

    private XMLSource filterCldrFile(String localeID, DraftStatus minimalDraftStatus) {
        XMLSource filteredSource = new SimpleXMLSource(localeID);
        int minLevel = StandardCodes.make()
            .getLocaleCoverageLevel(organization, localeID)
            .getLevel();
        ULocale locale = new ULocale(localeID);
        CLDRFile rawFile = rawFactory.make(localeID, false, minimalDraftStatus);
        for (String xpath : rawFile) {
            String fullPath = rawFile.getFullXPath(xpath);
            int level = supplementalData.getCoverageValue(xpath, locale);
            if (level > minLevel) continue;
            // For certain alternate values, use them as the main values.
            String value = rawFile.getStringValue(xpath);
            Matcher matcher = SPECIAL_ALT_PATHS.matcher(fullPath);
            if (matcher.matches()) {
                // Don't override any prefilled alt values with the non-alt value.
                if (matcher.groupCount() == 0 && filteredSource.hasValueAtDPath(xpath)) {
                    continue;
                }
                // Strip the alt attribute from the end.
                fullPath = fullPath.substring(0, fullPath.lastIndexOf('['));
            }
            filteredSource.putValueAtPath(fullPath, value);
        }
        return filteredSource;
    }

    @Override
    public DraftStatus getMinimalDraftStatus() {
        return rawFactory.getMinimalDraftStatus();
    }

    @Override
    protected Set<String> handleGetAvailable() {
        return rawFactory.getAvailable();
    }

    /**
     * Run FilterFactory for a specfic organization.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Factory rawFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        // TODO: generalize this for other organizations.
        Factory filterFactory = new FilterFactory(rawFactory, "google");
        String outputDir = CldrUtility.GEN_DIRECTORY + "/filter";
        for (String locale : rawFactory.getAvailable()) {
            PrintWriter out = BagFormatter.openUTF8Writer(outputDir, locale + ".xml");
            filterFactory.make(locale, false).write(out);
            out.close();
        }
    }
}
